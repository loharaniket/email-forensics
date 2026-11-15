package com.emailforensics.backend.service;

import com.emailforensics.backend.common.ResourceNotFoundException;
import com.emailforensics.backend.dto.EmailDetailDto;
import com.emailforensics.backend.dto.IndicatorDto;
import com.emailforensics.backend.dto.ParsedHeaderResult;
import com.emailforensics.backend.dto.UploadResponse;
import com.emailforensics.backend.entity.Email;
import com.emailforensics.backend.entity.Indicator;
import com.emailforensics.backend.exception.InvalidEmailException;
import com.emailforensics.backend.repository.EmailRepository;
import com.emailforensics.backend.repository.IndicatorRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L;

    private final EmailRepository emailRepository;
    private final EmailStorageService emailStorageService;
    private final EmailParsingService emailParsingService;
    private final MailAuthService mailAuthService;
    private final IndicatorExtractionService indicatorExtractionService;
    private final IndicatorRepository indicatorRepository;
    private final ThreatIntelligenceService threatIntelligenceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EmailService(EmailRepository emailRepository,
                        EmailStorageService emailStorageService,
                        EmailParsingService emailParsingService,
                        MailAuthService mailAuthService,
                        IndicatorExtractionService indicatorExtractionService,
                        IndicatorRepository indicatorRepository,
                        ThreatIntelligenceService threatIntelligenceService) {
        this.emailRepository = emailRepository;
        this.emailStorageService = emailStorageService;
        this.emailParsingService = emailParsingService;
        this.mailAuthService = mailAuthService;
        this.indicatorExtractionService = indicatorExtractionService;
        this.indicatorRepository = indicatorRepository;
        this.threatIntelligenceService = threatIntelligenceService;
    }

    @Transactional
    public UploadResponse processUpload(MultipartFile file) {
        validateFile(file);

        byte[] fileBytes = extractBytes(file);
        String sha256 = emailStorageService.computeSha256(fileBytes);
        String relativeFilePath = emailStorageService.saveOriginalEmail(fileBytes);
        String sanitizedName = emailStorageService.sanitizeFileName(file.getOriginalFilename());

        ParsedHeaderResult parsed = emailParsingService.parseHeaders(new ByteArrayInputStream(fileBytes));
        Path absolutePath = emailStorageService.getRootPath().resolve(relativeFilePath);
        String mailAuthJson = mailAuthService.authenticateEmail(absolutePath);
        String combinedAuthData = mergeAuthData(mailAuthJson, parsed);

        Email email = new Email();
        email.setFileName(sanitizedName);
        email.setFilePath(relativeFilePath);
        email.setSha256(sha256);
        email.setSender(parsed.getSender());
        email.setReplyTo(parsed.getReplyTo());
        email.setSubject(parsed.getSubject());
        email.setSentAt(parsed.getSentAt());
        email.setAuthData(combinedAuthData);
        email.setStatus("RECEIVED");
        email.setStage("AUTHENTICATED");
        email.setCreatedAt(Instant.now());

        Email savedEmail = emailRepository.save(email);

        List<Indicator> indicators = indicatorExtractionService.extractIndicators(savedEmail.getId(), parsed);
        if (!indicators.isEmpty()) {
            indicatorRepository.saveAll(indicators);
            threatIntelligenceService.checkEmailIndicators(savedEmail.getId());
        }
        savedEmail.setStage("THREAT_INTEL_CHECKED");
        savedEmail = emailRepository.save(savedEmail);

        return new UploadResponse(
                savedEmail.getId(),
                savedEmail.getFileName(),
                savedEmail.getSha256(),
                savedEmail.getStatus(),
                savedEmail.getStage(),
                "Email evidence preserved, parsed, and threat intelligence evaluated successfully"
        );
    }

    private String mergeAuthData(String mailAuthJson, ParsedHeaderResult parsed) {
        try {
            Map<String, Object> map;
            if (mailAuthJson != null && mailAuthJson.trim().startsWith("{")) {
                map = objectMapper.readValue(mailAuthJson, new TypeReference<Map<String, Object>>() {});
            } else {
                map = new LinkedHashMap<>();
            }

            Map<String, Object> senderAnalysis = new LinkedHashMap<>();
            senderAnalysis.put("from", parsed.getSender());
            senderAnalysis.put("fromDomain", parsed.getFromDomain());
            senderAnalysis.put("replyTo", parsed.getReplyTo());
            senderAnalysis.put("replyToDomain", parsed.getReplyToDomain());
            senderAnalysis.put("returnPath", parsed.getReturnPath());
            senderAnalysis.put("returnPathDomain", parsed.getReturnPathDomain());
            senderAnalysis.put("replyToMismatch", parsed.isReplyToMismatch());
            senderAnalysis.put("returnPathMismatch", parsed.isReturnPathMismatch());

            map.put("senderAnalysis", senderAnalysis);
            if (parsed.getReceivedHops() != null && !parsed.getReceivedHops().isEmpty()) {
                map.put("parsedHops", parsed.getReceivedHops());
            }

            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return mailAuthJson != null ? mailAuthJson : "{}";
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidEmailException("File is required and cannot be empty");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".eml")) {
            throw new InvalidEmailException("Only .eml files are supported");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new InvalidEmailException("File size exceeds maximum allowed limit of 10 MB");
        }
    }

    private byte[] extractBytes(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length == 0) {
                throw new InvalidEmailException("File contains zero bytes");
            }
            return bytes;
        } catch (IOException e) {
            throw new InvalidEmailException("Failed to read file content: " + e.getMessage());
        }
    }

    public EmailDetailDto getEmailById(Long id) {
        Email email = emailRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Email record not found with id: " + id));
        return toDetailDto(email);
    }

    public List<EmailDetailDto> getRecentEmails() {
        List<Email> emails = emailRepository.findAllByOrderByCreatedAtDesc();
        List<EmailDetailDto> dtos = new ArrayList<>();
        for (Email email : emails) {
            dtos.add(toDetailDto(email));
        }
        return dtos;
    }

    public Resource loadOriginalFile(Long id) {
        Email email = emailRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Email record not found with id: " + id));
        return emailStorageService.loadOriginalEmail(email.getFilePath());
    }

    public String getOriginalFilename(Long id) {
        Email email = emailRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Email record not found with id: " + id));
        return email.getFileName();
    }

    public List<IndicatorDto> getIndicatorsByEmailId(Long emailId) {
        List<Indicator> indicators = indicatorRepository.findByEmailId(emailId);
        List<IndicatorDto> dtos = new ArrayList<>();
        for (Indicator indicator : indicators) {
            dtos.add(IndicatorDto.fromEntity(indicator));
        }
        return dtos;
    }

    public List<IndicatorDto> checkThreatIntelligence(Long emailId) {
        return threatIntelligenceService.checkEmailIndicators(emailId);
    }

    private EmailDetailDto toDetailDto(Email email) {
        EmailDetailDto dto = new EmailDetailDto();
        dto.setId(email.getId());
        dto.setFileName(email.getFileName());
        dto.setFilePath(email.getFilePath());
        dto.setSha256(email.getSha256());
        dto.setSender(email.getSender());
        dto.setReplyTo(email.getReplyTo());
        dto.setSubject(email.getSubject());
        dto.setSentAt(email.getSentAt());
        dto.setAuthData(email.getAuthData());
        dto.setStatus(email.getStatus());
        dto.setStage(email.getStage());
        dto.setRiskScore(email.getRiskScore());
        dto.setRiskLevel(email.getRiskLevel());
        dto.setVerdict(email.getVerdict());
        dto.setCreatedAt(email.getCreatedAt());
        dto.setCompletedAt(email.getCompletedAt());
        return dto;
    }
}

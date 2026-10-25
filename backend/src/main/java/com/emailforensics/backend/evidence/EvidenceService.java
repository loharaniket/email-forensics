package com.emailforensics.backend.evidence;

import com.emailforensics.backend.casefile.CaseFile;
import com.emailforensics.backend.casefile.CaseRepository;
import com.emailforensics.backend.common.BadRequestException;
import com.emailforensics.backend.common.ResourceNotFoundException;
import com.emailforensics.backend.dto.common.PageResponse;
import com.emailforensics.backend.dto.evidence.EvidenceIntegrityResponse;
import com.emailforensics.backend.dto.evidence.EvidenceOverviewDto;
import com.emailforensics.backend.dto.evidence.EvidenceStatusResponse;
import com.emailforensics.backend.dto.evidence.EvidenceSummaryDto;
import com.emailforensics.backend.dto.evidence.EvidenceUploadResponse;
import com.emailforensics.backend.dto.email.AttachmentMetaDto;
import com.emailforensics.backend.dto.email.InvestigationDetailDto;
import com.emailforensics.backend.dto.email.ParsedEmailDto;
import com.emailforensics.backend.email.EmailParserService;
import com.emailforensics.backend.security.UserPrincipal;
import com.emailforensics.backend.user.User;
import com.emailforensics.backend.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EvidenceService {

    private final EmailEvidenceRepository evidenceRepository;
    private final AnalysisRunRepository analysisRunRepository;
    private final CaseRepository caseRepository;
    private final UserRepository userRepository;
    private final EvidenceStorageService storageService;
    private final EmailParserService emailParserService;
    private final com.emailforensics.backend.risk.RiskAssessmentRepository riskAssessmentRepository;
    private final String pipelineVersion;

    public EvidenceService(EmailEvidenceRepository evidenceRepository,
                           AnalysisRunRepository analysisRunRepository,
                           CaseRepository caseRepository,
                           UserRepository userRepository,
                           EvidenceStorageService storageService,
                           EmailParserService emailParserService,
                           com.emailforensics.backend.risk.RiskAssessmentRepository riskAssessmentRepository,
                           @Value("${emailforensics.pipeline.version:1.0.0}") String pipelineVersion) {
        this.evidenceRepository = evidenceRepository;
        this.analysisRunRepository = analysisRunRepository;
        this.caseRepository = caseRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.emailParserService = emailParserService;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.pipelineVersion = pipelineVersion;
    }

    @Transactional
    public EvidenceUploadResponse uploadEvidence(UUID caseId, MultipartFile file, UserPrincipal currentUser) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Evidence file is required and cannot be empty");
        }

        String rawFilename = file.getOriginalFilename();
        if (rawFilename == null || rawFilename.trim().isEmpty()) {
            throw new BadRequestException("Original filename cannot be empty");
        }

        String sanitizedFilename = storageService.sanitizeFilename(rawFilename);
        if (!sanitizedFilename.toLowerCase().endsWith(".eml")) {
            throw new BadRequestException("Only .eml files are supported as email evidence");
        }

        CaseFile caseFile = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found with ID: " + caseId));

        User uploader = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + currentUser.getId()));

        StoredEvidenceFile stored;
        try (InputStream inputStream = file.getInputStream()) {
            stored = storageService.storeEvidence(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded file stream", e);
        }

        EmailEvidence evidence = new EmailEvidence(
                UUID.randomUUID(),
                caseFile,
                sanitizedFilename,
                stored.getStorageKey(),
                stored.getFileSizeBytes(),
                stored.getSha256Hash(),
                uploader
        );

        try (InputStream streamForParse = storageService.loadAsResource(stored.getStorageKey()).getInputStream()) {
            ParsedEmailDto parsed = emailParserService.parse(streamForParse);
            if (parsed.getSubject() != null) evidence.setSubject(parsed.getSubject());
            if (parsed.getMessageId() != null) evidence.setMessageId(parsed.getMessageId());
            if (parsed.getSentDate() != null) evidence.setSentDate(parsed.getSentDate());
        } catch (Exception ignored) {
        }

        evidence = evidenceRepository.save(evidence);

        AnalysisRun run = new AnalysisRun(
                UUID.randomUUID(),
                evidence,
                pipelineVersion,
                AnalysisStatus.QUEUED,
                "QUEUED"
        );
        run = analysisRunRepository.save(run);

        String statusUrl = "/api/v1/evidence/" + evidence.getId() + "/status";

        return new EvidenceUploadResponse(
                evidence.getId(),
                run.getId(),
                run.getStatus().name(),
                statusUrl,
                evidence.getSha256Hash(),
                evidence.getOriginalFilename(),
                evidence.getFileSizeBytes()
        );
    }

    @Transactional(readOnly = true)
    public EvidenceStatusResponse getEvidenceStatus(UUID emailId) {
        if (!evidenceRepository.existsById(emailId)) {
            throw new ResourceNotFoundException("Email evidence not found with ID: " + emailId);
        }

        AnalysisRun run = analysisRunRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("No analysis run found for evidence: " + emailId));

        return new EvidenceStatusResponse(
                emailId,
                run.getId(),
                run.getStatus(),
                run.getCurrentStage(),
                run.getPipelineVersion(),
                run.getAttemptNumber(),
                run.getStartedAt(),
                run.getCompletedAt(),
                run.getErrorCode(),
                run.getSafeErrorMessage()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<EvidenceSummaryDto> getCaseEvidence(UUID caseId, Pageable pageable) {
        if (!caseRepository.existsById(caseId)) {
            throw new ResourceNotFoundException("Case not found with ID: " + caseId);
        }

        Page<EmailEvidence> page = evidenceRepository.findByCaseFileIdOrderByUploadedAtDesc(caseId, pageable);
        List<EvidenceSummaryDto> content = new ArrayList<>();

        for (EmailEvidence item : page.getContent()) {
            Optional<AnalysisRun> latestRun = analysisRunRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(item.getId());
            AnalysisStatus status = latestRun.map(AnalysisRun::getStatus).orElse(AnalysisStatus.QUEUED);
            String currentStage = latestRun.map(AnalysisRun::getCurrentStage).orElse("QUEUED");

            EvidenceSummaryDto dto = new EvidenceSummaryDto(
                    item.getId(),
                    item.getCaseFile().getId(),
                    item.getOriginalFilename(),
                    item.getFileSizeBytes(),
                    item.getSha256Hash(),
                    item.getSubject(),
                    item.getMessageId(),
                    item.getSentDate(),
                    item.getUploadedBy().getUsername(),
                    item.getUploadedAt(),
                    status,
                    currentStage
            );
            content.add(dto);
        }

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    @Transactional(readOnly = true)
    public EvidenceOverviewDto getEvidenceOverview(UUID emailId) {
        EmailEvidence evidence = evidenceRepository.findById(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("Email evidence not found with ID: " + emailId));

        Optional<AnalysisRun> latestRun = analysisRunRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId);
        AnalysisStatus status = latestRun.map(AnalysisRun::getStatus).orElse(AnalysisStatus.QUEUED);
        String currentStage = latestRun.map(AnalysisRun::getCurrentStage).orElse("QUEUED");

        Integer riskScore = null;
        String riskLevel = null;
        Integer confidenceScore = null;
        if (latestRun.isPresent()) {
            Optional<com.emailforensics.backend.risk.RiskAssessment> assessmentOpt =
                    riskAssessmentRepository.findByAnalysisRunId(latestRun.get().getId());
            if (assessmentOpt.isPresent()) {
                com.emailforensics.backend.risk.RiskAssessment ra = assessmentOpt.get();
                riskScore = ra.getDeterministicRiskScore();
                riskLevel = ra.getRiskLevel();
                confidenceScore = ra.getEvidenceConfidenceScore();
            }
        }

        return new EvidenceOverviewDto(
                evidence.getId(),
                evidence.getCaseFile().getId(),
                evidence.getCaseFile().getCaseNumber(),
                evidence.getOriginalFilename(),
                evidence.getFileSizeBytes(),
                evidence.getSha256Hash(),
                evidence.getSubject(),
                evidence.getMessageId(),
                evidence.getSentDate(),
                evidence.getUploadedAt(),
                evidence.getUploadedBy().getUsername(),
                status,
                currentStage,
                riskScore,
                riskLevel,
                confidenceScore
        );
    }

    @Transactional(readOnly = true)
    public EvidenceIntegrityResponse checkIntegrity(UUID emailId) {
        EmailEvidence evidence = evidenceRepository.findById(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("Email evidence not found with ID: " + emailId));

        String recalculatedHash = storageService.computeFileHash(evidence.getStorageKey());
        boolean intact = evidence.getSha256Hash().equalsIgnoreCase(recalculatedHash);

        return new EvidenceIntegrityResponse(
                evidence.getId(),
                evidence.getOriginalFilename(),
                evidence.getSha256Hash(),
                recalculatedHash,
                intact,
                evidence.getFileSizeBytes(),
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public Resource loadOriginalFileResource(UUID emailId) {
        EmailEvidence evidence = evidenceRepository.findById(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("Email evidence not found with ID: " + emailId));
        return storageService.loadAsResource(evidence.getStorageKey());
    }

    @Transactional(readOnly = true)
    public String getOriginalFilename(UUID emailId) {
        EmailEvidence evidence = evidenceRepository.findById(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("Email evidence not found with ID: " + emailId));
        return evidence.getOriginalFilename();
    }

    @Transactional
    public InvestigationDetailDto getInvestigationDetail(UUID emailId) {
        EmailEvidence evidence = evidenceRepository.findById(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("Email evidence not found with ID: " + emailId));

        AnalysisRun run = analysisRunRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)
                .orElse(null);

        AnalysisStatus status = run != null ? run.getStatus() : AnalysisStatus.QUEUED;
        String currentStage = run != null ? run.getCurrentStage() : "QUEUED";

        ParsedEmailDto parsed;
        try (InputStream inputStream = storageService.loadAsResource(evidence.getStorageKey()).getInputStream()) {
            parsed = emailParserService.parse(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read evidence stream for parsing", e);
        }

        boolean updated = false;
        if (evidence.getSubject() == null && parsed.getSubject() != null) {
            evidence.setSubject(parsed.getSubject());
            updated = true;
        }
        if (evidence.getMessageId() == null && parsed.getMessageId() != null) {
            evidence.setMessageId(parsed.getMessageId());
            updated = true;
        }
        if (evidence.getSentDate() == null && parsed.getSentDate() != null) {
            evidence.setSentDate(parsed.getSentDate());
            updated = true;
        }
        if (updated) {
            evidenceRepository.save(evidence);
        }

        if (run != null && (run.getStatus() == AnalysisStatus.QUEUED || run.getStatus() == AnalysisStatus.RUNNING)) {
            run.setStatus(AnalysisStatus.COMPLETED);
            run.setCurrentStage("MIME_PARSED");
            if (run.getStartedAt() == null) {
                run.setStartedAt(Instant.now());
            }
            run.setCompletedAt(Instant.now());
            analysisRunRepository.save(run);
            status = run.getStatus();
            currentStage = run.getCurrentStage();
        }

        return new InvestigationDetailDto(
                evidence.getId(),
                evidence.getCaseFile().getId(),
                evidence.getCaseFile().getCaseNumber(),
                evidence.getCaseFile().getTitle(),
                evidence.getOriginalFilename(),
                evidence.getFileSizeBytes(),
                evidence.getSha256Hash(),
                evidence.getUploadedAt(),
                evidence.getUploadedBy().getUsername(),
                status,
                currentStage,
                parsed
        );
    }

    @Transactional(readOnly = true)
    public List<AttachmentMetaDto> getEvidenceAttachments(UUID emailId) {
        EmailEvidence evidence = evidenceRepository.findById(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("Email evidence not found with ID: " + emailId));

        try (InputStream inputStream = storageService.loadAsResource(evidence.getStorageKey()).getInputStream()) {
            ParsedEmailDto parsed = emailParserService.parse(inputStream);
            return parsed.getAttachments();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read evidence stream for attachments", e);
        }
    }
}

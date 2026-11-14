package com.emailforensics.backend.headers;

import com.emailforensics.backend.analysis.AnalysisResult;
import com.emailforensics.backend.analysis.AnalysisResultRepository;
import com.emailforensics.backend.dto.headers.HeaderAnalysisDto;
import com.emailforensics.backend.dto.headers.ReceivedHopDto;
import com.emailforensics.backend.evidence.AnalysisRun;
import com.emailforensics.backend.evidence.AnalysisRunRepository;
import com.emailforensics.backend.evidence.EmailEvidence;
import com.emailforensics.backend.evidence.EmailEvidenceRepository;
import com.emailforensics.backend.evidence.EvidenceStorageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Header;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MailDateFormat;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HeaderAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(HeaderAnalysisService.class);

    private final AnalysisResultRepository analysisResultRepository;
    private final ReceivedHopRepository receivedHopRepository;
    private final EmailEvidenceRepository evidenceRepository;
    private final AnalysisRunRepository analysisRunRepository;
    private final EvidenceStorageService storageService;
    private final EmailHeaderService emailHeaderService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern IP_PATTERN = Pattern.compile("(?:\\[|\\()([0-9]{1,3}(?:\\.[0-9]{1,3}){3}|[a-fA-F0-9:]+)(?:\\]|\\))");
    private static final Pattern FROM_HOST_PATTERN = Pattern.compile("(?i)\\bfrom\\s+([^\\s;]+)");
    private static final Pattern BY_HOST_PATTERN = Pattern.compile("(?i)\\bby\\s+([^\\s;]+)");
    private static final Pattern PROTOCOL_PATTERN = Pattern.compile("(?i)\\bwith\\s+([^\\s;]+)");
    private static final Pattern EMAIL_IN_TEXT_PATTERN = Pattern.compile("([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");

    @Autowired
    public HeaderAnalysisService(AnalysisResultRepository analysisResultRepository,
                                 ReceivedHopRepository receivedHopRepository,
                                 EmailEvidenceRepository evidenceRepository,
                                 AnalysisRunRepository analysisRunRepository,
                                 EvidenceStorageService storageService,
                                 EmailHeaderService emailHeaderService) {
        this.analysisResultRepository = analysisResultRepository;
        this.receivedHopRepository = receivedHopRepository;
        this.evidenceRepository = evidenceRepository;
        this.analysisRunRepository = analysisRunRepository;
        this.storageService = storageService;
        this.emailHeaderService = emailHeaderService;
    }

    @Transactional
    public HeaderAnalysisDto analyzeAndSaveHeaders(UUID emailId) {
        synchronized (emailId.toString().intern()) {
            EmailEvidence evidence = evidenceRepository.findById(emailId)
                    .orElseThrow(() -> new IllegalArgumentException("Evidence not found: " + emailId));

            AnalysisRun run = analysisRunRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)
                    .orElseThrow(() -> new IllegalArgumentException("Analysis run not found for evidence: " + emailId));

            byte[] emlBytes;
            try (InputStream in = storageService.loadAsResource(evidence.getStorageKey()).getInputStream()) {
                emlBytes = in.readAllBytes();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load email resource for header analysis: " + emailId, e);
            }

            RawHeaderData rawData = extractRawHeaderData(emlBytes);

            HeaderAuthResult authResult;
            if (emailHeaderService != null) {
                authResult = emailHeaderService.analyzeHeader(rawData.rawHeadersText);
            } else {
                authResult = HeaderAuthResult.allUnavailable("EmailHeaderService is not configured.");
            }

            AnalysisResult existingEntity = analysisResultRepository.findByAnalysisRunId(run.getId()).orElse(null);
            AnalysisResult entity = existingEntity != null ? existingEntity : new AnalysisResult(run);
            if (entity.getHeaderResultId() == null) {
                entity.setHeaderResultId(UUID.randomUUID());
            }
            if (entity.getHeaderCreatedAt() == null) {
                entity.setHeaderCreatedAt(Instant.now());
            }

            entity.setAnalysisRun(run);
            entity.setFromAddress(rawData.fromAddress);
            entity.setFromDomain(rawData.fromDomain);
            entity.setReplyTo(rawData.replyToAddress);
            entity.setReturnPath(rawData.returnPathAddress);
            entity.setDkimDomain(authResult.getDkimDomain());
            entity.setIdentityMismatchFlags(formatFlagsJson(rawData.mismatchFlags));
            entity.setRawHeadersJson(formatRawHeadersJson(rawData.rawHeaders));

            if (emailHeaderService != null) {
                emailHeaderService.saveResult(entity, authResult);
            } else {
                entity.setAuthApiProvider(authResult.getProviderName());
                entity.setAuthApiStatus(authResult.getStatus());
                entity.setReportedSpf(authResult.getSpfResult());
                entity.setReportedDkim(authResult.getDkimResult());
                entity.setReportedDmarc(authResult.getDmarcResult());
                entity.setReportedArc(authResult.getArcResult());
                entity.setIndependentVerification(authResult.getSummary());
            }

            entity = analysisResultRepository.saveAndFlush(entity);

            List<ReceivedHopDto> hopDtos = new ArrayList<>();
            List<ReceivedHop> existingHops = receivedHopRepository.findByAnalysisRunIdOrderByHopOrderAsc(run.getId());

            if (existingHops.isEmpty()) {
                List<ReceivedHopDto> hopsToSave;
                if (authResult.getHops() != null && !authResult.getHops().isEmpty()) {
                    hopsToSave = authResult.getHops();
                } else {
                    hopsToSave = extractHopsFromStream(new ByteArrayInputStream(emlBytes));
                }

                for (ReceivedHopDto hopDto : hopsToSave) {
                    ReceivedHop hopEntity = new ReceivedHop(
                            UUID.randomUUID(),
                            run,
                            hopDto.getHopOrder(),
                            hopDto.getFromHost(),
                            hopDto.getByHost(),
                            hopDto.getProtocol(),
                            hopDto.getHopIp(),
                            hopDto.getHopTimestamp(),
                            hopDto.getDelaySeconds(),
                            hopDto.getRawHopLine(),
                            hopDto.getParsingWarning()
                    );
                    try {
                        receivedHopRepository.save(hopEntity);
                    } catch (org.springframework.dao.DataIntegrityViolationException ignored) {
                    }
                }
                try {
                    receivedHopRepository.flush();
                } catch (org.springframework.dao.DataIntegrityViolationException ignored) {
                }
                hopDtos = hopsToSave;
            } else {
                for (ReceivedHop h : existingHops) {
                    hopDtos.add(new ReceivedHopDto(
                            h.getId(),
                            h.getHopOrder(),
                            h.getFromHost(),
                            h.getByHost(),
                            h.getProtocol(),
                            h.getHopIp(),
                            h.getHopTimestamp(),
                            h.getDelaySeconds(),
                            h.getRawHopLine(),
                            h.getParsingWarning()
                    ));
                }
            }

            String verificationNote = "Authentication results validated via " + authResult.getProviderName() + ".";
            HeaderAnalysisDto dto = new HeaderAnalysisDto(
                    run.getId(),
                    rawData.fromAddress,
                    rawData.fromDomain,
                    rawData.replyToAddress,
                    rawData.returnPathAddress,
                    authResult.getDkimDomain(),
                    authResult.getSpfResult(),
                    authResult.getDkimResult(),
                    authResult.getDmarcResult(),
                    authResult.getArcResult(),
                    rawData.mismatchFlags,
                    verificationNote,
                    rawData.rawHeaders,
                    hopDtos.size()
            );

            dto.setSpfResult(authResult.getSpfResult());
            dto.setDkimResult(authResult.getDkimResult());
            dto.setDmarcResult(authResult.getDmarcResult());
            dto.setArcResult(authResult.getArcResult());
            dto.setReportedSpf(authResult.getSpfResult());
            dto.setReportedDkim(authResult.getDkimResult());
            dto.setReportedDmarc(authResult.getDmarcResult());
            dto.setReportedArc(authResult.getArcResult());
            dto.setDataMode("EXTERNAL_API");
            dto.setAuthApiProvider(authResult.getProviderName());
            dto.setAuthApiStatus(authResult.getStatus());

            return dto;
        }
    }

    @Transactional(readOnly = true)
    public HeaderAnalysisDto getHeaderAnalysis(UUID emailId) {
        AnalysisRun run = analysisRunRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)
                .orElse(null);
        if (run == null) {
            return null;
        }

        AnalysisResult entity = analysisResultRepository.findByAnalysisRunId(run.getId()).orElse(null);
        if (entity == null || !entity.hasHeaderAnalysis()) {
            return null;
        }

        List<ReceivedHop> hops = receivedHopRepository.findByAnalysisRunIdOrderByHopOrderAsc(run.getId());
        List<String> flags = parseFlagsJson(entity.getIdentityMismatchFlags());
        Map<String, List<String>> rawMap = parseRawHeadersJson(entity.getRawHeadersJson());

        String providerLabel = entity.getAuthApiProvider() != null ? entity.getAuthApiProvider() : "External API";
        String verificationNote = "Authentication results validated via " + providerLabel + ".";

        HeaderAnalysisDto dto = new HeaderAnalysisDto(
                run.getId(),
                entity.getFromAddress(),
                entity.getFromDomain(),
                entity.getReplyTo(),
                entity.getReturnPath(),
                entity.getDkimDomain(),
                entity.getReportedSpf(),
                entity.getReportedDkim(),
                entity.getReportedDmarc(),
                entity.getReportedArc(),
                flags,
                verificationNote,
                rawMap,
                hops.size()
        );

        dto.setSpfResult(entity.getReportedSpf());
        dto.setDkimResult(entity.getReportedDkim());
        dto.setDmarcResult(entity.getReportedDmarc());
        dto.setArcResult(entity.getReportedArc());
        dto.setReportedSpf(entity.getReportedSpf());
        dto.setReportedDkim(entity.getReportedDkim());
        dto.setReportedDmarc(entity.getReportedDmarc());
        dto.setReportedArc(entity.getReportedArc());
        dto.setDataMode("EXTERNAL_API");
        dto.setAuthApiProvider(entity.getAuthApiProvider());
        dto.setAuthApiStatus(entity.getAuthApiStatus());

        return dto;
    }

    @Transactional(readOnly = true)
    public List<ReceivedHopDto> getReceivedHops(UUID emailId) {
        AnalysisRun run = analysisRunRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)
                .orElse(null);
        if (run == null) {
            return Collections.emptyList();
        }

        List<ReceivedHop> hops = receivedHopRepository.findByAnalysisRunIdOrderByHopOrderAsc(run.getId());
        List<ReceivedHopDto> dtos = new ArrayList<>();
        for (ReceivedHop hop : hops) {
            dtos.add(new ReceivedHopDto(
                    hop.getId(),
                    hop.getHopOrder(),
                    hop.getFromHost(),
                    hop.getByHost(),
                    hop.getProtocol(),
                    hop.getHopIp(),
                    hop.getHopTimestamp(),
                    hop.getDelaySeconds(),
                    hop.getRawHopLine(),
                    hop.getParsingWarning()
            ));
        }
        return dtos;
    }

    public HeaderAnalysisDto analyzeStream(InputStream in) {
        try {
            byte[] emlBytes = in.readAllBytes();
            RawHeaderData rawData = extractRawHeaderData(emlBytes);

            HeaderAuthResult authResult;
            if (emailHeaderService != null) {
                authResult = emailHeaderService.analyzeHeader(rawData.rawHeadersText);
            } else {
                authResult = HeaderAuthResult.allUnavailable("EmailHeaderService is not configured.");
            }

            HeaderAnalysisDto dto = new HeaderAnalysisDto(
                    null,
                    rawData.fromAddress,
                    rawData.fromDomain,
                    rawData.replyToAddress,
                    rawData.returnPathAddress,
                    authResult.getDkimDomain(),
                    authResult.getSpfResult(),
                    authResult.getDkimResult(),
                    authResult.getDmarcResult(),
                    authResult.getArcResult(),
                    rawData.mismatchFlags,
                    "Authentication results validated via " + authResult.getProviderName() + ".",
                    rawData.rawHeaders,
                    0
            );

            dto.setSpfResult(authResult.getSpfResult());
            dto.setDkimResult(authResult.getDkimResult());
            dto.setDmarcResult(authResult.getDmarcResult());
            dto.setArcResult(authResult.getArcResult());
            dto.setReportedSpf(authResult.getSpfResult());
            dto.setReportedDkim(authResult.getDkimResult());
            dto.setReportedDmarc(authResult.getDmarcResult());
            dto.setReportedArc(authResult.getArcResult());
            dto.setDataMode("EXTERNAL_API");
            dto.setAuthApiProvider(authResult.getProviderName());
            dto.setAuthApiStatus(authResult.getStatus());

            return dto;

        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse email headers", e);
        }
    }

    public List<ReceivedHopDto> extractHopsFromStream(InputStream in) {
        List<ReceivedHopDto> hops = new ArrayList<>();
        try {
            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);
            MimeMessage message = new MimeMessage(session, in);

            String[] rawReceived = message.getHeader("Received");
            if (rawReceived == null || rawReceived.length == 0) {
                return hops;
            }

            List<String> chronologicalLines = new ArrayList<>(Arrays.asList(rawReceived));
            Collections.reverse(chronologicalLines);

            Instant previousTimestamp = null;
            MailDateFormat mailDateFormat = new MailDateFormat();

            for (int i = 0; i < chronologicalLines.size(); i++) {
                String line = chronologicalLines.get(i).replaceAll("[\\r\\n\\t]+", " ").replaceAll("\\s+", " ").trim();
                int order = i + 1;

                String fromHost = null;
                Matcher fromMatcher = FROM_HOST_PATTERN.matcher(line);
                if (fromMatcher.find()) {
                    fromHost = fromMatcher.group(1);
                }

                String byHost = null;
                Matcher byMatcher = BY_HOST_PATTERN.matcher(line);
                if (byMatcher.find()) {
                    byHost = byMatcher.group(1);
                }

                String protocol = null;
                Matcher protoMatcher = PROTOCOL_PATTERN.matcher(line);
                if (protoMatcher.find()) {
                    protocol = protoMatcher.group(1);
                }

                String hopIp = null;
                Matcher ipMatcher = IP_PATTERN.matcher(line);
                if (ipMatcher.find()) {
                    hopIp = ipMatcher.group(1);
                }

                Instant hopTimestamp = null;
                String warning = null;
                int semiIndex = line.lastIndexOf(';');
                if (semiIndex >= 0 && semiIndex < line.length() - 1) {
                    String datePart = line.substring(semiIndex + 1).trim();
                    try {
                        Date parsedDate = mailDateFormat.parse(datePart);
                        if (parsedDate != null) {
                            hopTimestamp = parsedDate.toInstant();
                        }
                    } catch (Exception e) {
                        warning = "Could not parse date: " + datePart;
                    }
                }

                Long delaySeconds = null;
                if (previousTimestamp != null && hopTimestamp != null) {
                    long diff = Duration.between(previousTimestamp, hopTimestamp).getSeconds();
                    if (diff >= 0) {
                        delaySeconds = diff;
                    }
                }

                if (hopTimestamp != null) {
                    previousTimestamp = hopTimestamp;
                }

                hops.add(new ReceivedHopDto(
                        null,
                        order,
                        fromHost,
                        byHost,
                        protocol,
                        hopIp,
                        hopTimestamp,
                        delaySeconds,
                        line,
                        warning
                ));
            }

        } catch (Exception e) {
            throw new IllegalStateException("Failed to extract received hops", e);
        }
        return hops;
    }

    private static class RawHeaderData {
        String fromAddress;
        String fromDomain;
        String replyToAddress;
        String returnPathAddress;
        String rawHeadersText;
        Map<String, List<String>> rawHeaders = new LinkedHashMap<>();
        List<String> mismatchFlags = new ArrayList<>();
    }

    private RawHeaderData extractRawHeaderData(byte[] emlBytes) {
        RawHeaderData data = new RawHeaderData();
        try {
            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);
            MimeMessage message = new MimeMessage(session, new ByteArrayInputStream(emlBytes));

            StringBuilder sb = new StringBuilder();
            Enumeration<Header> allHeaders = message.getAllHeaders();
            while (allHeaders.hasMoreElements()) {
                Header h = allHeaders.nextElement();
                data.rawHeaders.computeIfAbsent(h.getName(), k -> new ArrayList<>()).add(h.getValue());
                sb.append(h.getName()).append(": ").append(h.getValue()).append("\r\n");
            }
            data.rawHeadersText = sb.toString();

            String fromRaw = getFirstHeader(message, "From");
            String replyToRaw = getFirstHeader(message, "Reply-To");
            String returnPathRaw = getFirstHeader(message, "Return-Path");

            data.fromAddress = extractEmailAddress(fromRaw);
            data.fromDomain = extractDomain(data.fromAddress);
            data.replyToAddress = extractEmailAddress(replyToRaw);
            data.returnPathAddress = extractEmailAddress(returnPathRaw);

            if (data.replyToAddress != null && data.fromDomain != null) {
                String replyToDomain = extractDomain(data.replyToAddress);
                if (replyToDomain != null && !replyToDomain.equalsIgnoreCase(data.fromDomain)) {
                    data.mismatchFlags.add("REPLY_TO_DOMAIN_MISMATCH: Reply-To domain (@" + replyToDomain + ") differs from From domain (@" + data.fromDomain + ")");
                }
            }

            if (data.returnPathAddress != null && data.fromDomain != null) {
                String returnPathDomain = extractDomain(data.returnPathAddress);
                if (returnPathDomain != null && !returnPathDomain.equalsIgnoreCase(data.fromDomain)) {
                    data.mismatchFlags.add("RETURN_PATH_MISMATCH: Return-Path envelope (@" + returnPathDomain + ") differs from From domain (@" + data.fromDomain + ")");
                }
            }

            if (fromRaw != null) {
                String displayName = null;
                try {
                    InternetAddress[] fromAddresses = InternetAddress.parse(fromRaw);
                    if (fromAddresses.length > 0 && fromAddresses[0].getPersonal() != null) {
                        displayName = fromAddresses[0].getPersonal();
                    }
                } catch (Exception ignored) {
                }
                if (displayName == null) {
                    int lastAngle = fromRaw.lastIndexOf('<');
                    if (lastAngle > 0) {
                        displayName = fromRaw.substring(0, lastAngle).trim();
                    }
                }
                if (displayName != null && displayName.contains("@")) {
                    Matcher nameMatcher = EMAIL_IN_TEXT_PATTERN.matcher(displayName);
                    if (nameMatcher.find()) {
                        String nameEmail = nameMatcher.group(1).toLowerCase(Locale.ROOT);
                        if (data.fromAddress != null && !nameEmail.equalsIgnoreCase(data.fromAddress)) {
                            data.mismatchFlags.add("SUSPICIOUS_DISPLAY_NAME: Display name mimics email address (" + nameEmail + ") while true sender is (" + data.fromAddress + ")");
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.warn("Error extracting raw header data: {}", e.getMessage());
        }
        return data;
    }

    private String getFirstHeader(MimeMessage message, String name) {
        try {
            String[] values = message.getHeader(name);
            return (values != null && values.length > 0) ? values[0] : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractEmailAddress(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            InternetAddress[] addresses = InternetAddress.parse(raw);
            if (addresses.length > 0 && addresses[0].getAddress() != null) {
                return addresses[0].getAddress().trim().toLowerCase(Locale.ROOT);
            }
        } catch (Exception ignored) {
        }
        int start = raw.indexOf('<');
        int end = raw.indexOf('>');
        if (start >= 0 && end > start) {
            return raw.substring(start + 1, end).trim().toLowerCase(Locale.ROOT);
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private String extractDomain(String emailAddress) {
        if (emailAddress == null) return null;
        int atIndex = emailAddress.lastIndexOf('@');
        if (atIndex >= 0 && atIndex < emailAddress.length() - 1) {
            return emailAddress.substring(atIndex + 1).trim().toLowerCase(Locale.ROOT);
        }
        return null;
    }

    private String formatFlagsJson(List<String> flags) {
        if (flags == null || flags.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(flags);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> parseFlagsJson(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String formatRawHeadersJson(Map<String, List<String>> map) {
        if (map == null || map.isEmpty()) return "{}";
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, List<String>> parseRawHeadersJson(String json) {
        if (json == null || json.isBlank() || json.equals("{}")) return new LinkedHashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, List<String>>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }
}

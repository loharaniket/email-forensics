package com.emailforensics.backend.headers;

import com.emailforensics.backend.dto.headers.HeaderAnalysisDto;
import com.emailforensics.backend.dto.headers.ReceivedHopDto;
import com.emailforensics.backend.evidence.AnalysisRun;
import com.emailforensics.backend.evidence.AnalysisRunRepository;
import com.emailforensics.backend.evidence.AnalysisStatus;
import com.emailforensics.backend.evidence.EmailEvidence;
import com.emailforensics.backend.evidence.EmailEvidenceRepository;
import com.emailforensics.backend.evidence.EvidenceStorageService;
import jakarta.mail.Header;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MailDateFormat;
import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class HeaderForensicService {

    private final HeaderAnalysisRepository headerAnalysisRepository;
    private final ReceivedHopRepository receivedHopRepository;
    private final EmailEvidenceRepository evidenceRepository;
    private final AnalysisRunRepository analysisRunRepository;
    private final EvidenceStorageService storageService;
    private final ActiveEmailAuthService activeEmailAuthService;

    private static final Pattern IP_PATTERN = Pattern.compile("(?:\\[|\\()([0-9]{1,3}(?:\\.[0-9]{1,3}){3}|[a-fA-F0-9:]+)(?:\\]|\\))");
    private static final Pattern FROM_HOST_PATTERN = Pattern.compile("(?i)\\bfrom\\s+([^\\s;]+)");
    private static final Pattern BY_HOST_PATTERN = Pattern.compile("(?i)\\bby\\s+([^\\s;]+)");
    private static final Pattern PROTOCOL_PATTERN = Pattern.compile("(?i)\\bwith\\s+([^\\s;]+)");
    private static final Pattern DKIM_DOMAIN_PATTERN = Pattern.compile("(?i)\\bd=([a-zA-Z0-9.-]+)");
    private static final Pattern AUTH_SPF_PATTERN = Pattern.compile("(?i)\\bspf=([a-zA-Z]+)");
    private static final Pattern AUTH_DKIM_PATTERN = Pattern.compile("(?i)\\bdkim=([a-zA-Z]+)");
    private static final Pattern AUTH_DMARC_PATTERN = Pattern.compile("(?i)\\bdmarc=([a-zA-Z]+)");
    private static final Pattern AUTH_ARC_PATTERN = Pattern.compile("(?i)\\barc=([a-zA-Z]+)");
    private static final Pattern EMAIL_IN_TEXT_PATTERN = Pattern.compile("([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");

    @org.springframework.beans.factory.annotation.Autowired
    public HeaderForensicService(HeaderAnalysisRepository headerAnalysisRepository,
                                 ReceivedHopRepository receivedHopRepository,
                                 EmailEvidenceRepository evidenceRepository,
                                 AnalysisRunRepository analysisRunRepository,
                                 EvidenceStorageService storageService,
                                 ActiveEmailAuthService activeEmailAuthService) {
        this.headerAnalysisRepository = headerAnalysisRepository;
        this.receivedHopRepository = receivedHopRepository;
        this.evidenceRepository = evidenceRepository;
        this.analysisRunRepository = analysisRunRepository;
        this.storageService = storageService;
        this.activeEmailAuthService = activeEmailAuthService;
    }

    public HeaderForensicService(HeaderAnalysisRepository headerAnalysisRepository,
                                 ReceivedHopRepository receivedHopRepository,
                                 EmailEvidenceRepository evidenceRepository,
                                 AnalysisRunRepository analysisRunRepository,
                                 EvidenceStorageService storageService) {
        this(headerAnalysisRepository, receivedHopRepository, evidenceRepository, analysisRunRepository, storageService, null);
    }

    @Transactional
    public HeaderAnalysisDto analyzeAndSaveHeaders(UUID emailId) {
        EmailEvidence evidence = evidenceRepository.findById(emailId)
                .orElseThrow(() -> new IllegalArgumentException("Evidence not found: " + emailId));

        AnalysisRun run = analysisRunRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)
                .orElseThrow(() -> new IllegalArgumentException("Analysis run not found for evidence: " + emailId));

        HeaderAnalysisDto dto = analyzeStream(evidence.getStorageKey());
        dto.setAnalysisRunId(run.getId());

        UUID runId = run.getId();
        HeaderAnalysis entity;
        synchronized (runId.toString().intern()) {
            entity = headerAnalysisRepository.findByAnalysisRunId(runId).orElse(null);
            if (entity == null) {
                entity = new HeaderAnalysis();
                entity.setId(UUID.randomUUID());
            }
            entity.setAnalysisRun(run);
            entity.setFromAddress(dto.getFromAddress());
            entity.setFromDomain(dto.getFromDomain());
            entity.setReplyTo(dto.getReplyTo());
            entity.setReturnPath(dto.getReturnPath());
            entity.setDkimDomain(dto.getDkimDomain());
            entity.setReportedSpf(dto.getReportedSpf());
            entity.setReportedDkim(dto.getReportedDkim());
            entity.setReportedDmarc(dto.getReportedDmarc());
            entity.setReportedArc(dto.getReportedArc());
            entity.setIdentityMismatchFlags(formatFlagsJson(dto.getIdentityMismatchFlags()));
            entity.setRawHeadersJson(formatRawHeadersJson(dto.getRawHeaders()));
            try {
                entity = headerAnalysisRepository.saveAndFlush(entity);
            } catch (Exception ex) {
                entity = headerAnalysisRepository.findByAnalysisRunId(runId).orElse(entity);
            }
        }

        List<ReceivedHopDto> hopDtos = new ArrayList<>();
        List<ReceivedHop> existingHops = receivedHopRepository.findByAnalysisRunIdOrderByHopOrderAsc(run.getId());
        if (existingHops.isEmpty()) {
            List<ReceivedHopDto> hops = extractHopsFromStream(evidence.getStorageKey());
            for (ReceivedHopDto hopDto : hops) {
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
            dto.setTotalHopsCount(hops.size());
            hopDtos = hops;
        } else {
            dto.setTotalHopsCount(existingHops.size());
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

        if (activeEmailAuthService != null) {
            String dkimHeader = null;
            if (dto.getRawHeaders() != null && dto.getRawHeaders().containsKey("DKIM-Signature")) {
                List<String> dkimVals = dto.getRawHeaders().get("DKIM-Signature");
                if (dkimVals != null && !dkimVals.isEmpty()) {
                    dkimHeader = dkimVals.get(0);
                }
            }
            String returnPathDomain = extractDomain(dto.getReturnPath());
            ActiveEmailAuthService.ActiveAuthResult activeResult = activeEmailAuthService.performActiveAuthentication(
                    dto.getFromDomain(), returnPathDomain, dkimHeader, hopDtos
            );
            dto.setActiveSpfResult(activeResult.getActiveSpfResult());
            dto.setActiveSpfRecord(activeResult.getActiveSpfRecord());
            dto.setActiveSpfRelayIp(activeResult.getActiveSpfRelayIp());
            dto.setActiveDkimStatus(activeResult.getActiveDkimStatus());
            dto.setActiveDkimRecord(activeResult.getActiveDkimRecord());
            dto.setActiveDkimSelector(activeResult.getActiveDkimSelector());
            dto.setActiveDmarcStatus(activeResult.getActiveDmarcStatus());
            dto.setActiveDmarcPolicy(activeResult.getActiveDmarcPolicy());
            dto.setActiveVerificationSummary(activeResult.getSummary());

            entity.setIndependentVerification(activeResult.getSummary());
            entity.setCurrentDnsObservation(activeResult.getDnsObservationJson());
            try {
                headerAnalysisRepository.save(entity);
                headerAnalysisRepository.flush();
            } catch (Exception ignored) {
            }
        }

        run.setCurrentStage("HEADERS_ANALYZED");
        if (run.getStatus() == AnalysisStatus.QUEUED) {
            run.setStatus(AnalysisStatus.COMPLETED);
        }
        analysisRunRepository.save(run);

        return dto;
    }

    @Transactional
    public HeaderAnalysisDto getHeaderAnalysis(UUID emailId) {
        EmailEvidence evidence = evidenceRepository.findById(emailId)
                .orElseThrow(() -> new IllegalArgumentException("Evidence not found: " + emailId));

        AnalysisRun run = analysisRunRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)
                .orElseThrow(() -> new IllegalArgumentException("Analysis run not found for evidence: " + emailId));

        HeaderAnalysis entity = headerAnalysisRepository.findByAnalysisRunId(run.getId()).orElse(null);
        if (entity == null) {
            UUID runId = run.getId();
            synchronized (runId.toString().intern()) {
                entity = headerAnalysisRepository.findByAnalysisRunId(runId).orElse(null);
                if (entity == null) {
                    return analyzeAndSaveHeaders(emailId);
                }
            }
        }

        List<ReceivedHop> hops = receivedHopRepository.findByAnalysisRunIdOrderByHopOrderAsc(run.getId());
        List<String> flags = parseFlagsJson(entity.getIdentityMismatchFlags());
        Map<String, List<String>> rawMap = parseRawHeadersJson(entity.getRawHeadersJson());

        String verificationNote = "Authentication results (SPF, DKIM, DMARC, ARC) are extracted as reported by receiving server headers in the original message. They are not independently verified in offline state.";

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

        if (entity.getCurrentDnsObservation() != null && !entity.getCurrentDnsObservation().isBlank()) {
            populateActiveAuthFromObservation(dto, entity.getCurrentDnsObservation(), entity.getIndependentVerification());
        } else if (activeEmailAuthService != null) {
            List<ReceivedHopDto> hopDtos = new ArrayList<>();
            for (ReceivedHop h : hops) {
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
            String dkimHeader = null;
            if (rawMap != null && rawMap.containsKey("DKIM-Signature")) {
                List<String> dkimVals = rawMap.get("DKIM-Signature");
                if (dkimVals != null && !dkimVals.isEmpty()) {
                    dkimHeader = dkimVals.get(0);
                }
            }
            String returnPathDomain = extractDomain(entity.getReturnPath());
            ActiveEmailAuthService.ActiveAuthResult activeResult = activeEmailAuthService.performActiveAuthentication(
                    entity.getFromDomain(), returnPathDomain, dkimHeader, hopDtos
            );
            dto.setActiveSpfResult(activeResult.getActiveSpfResult());
            dto.setActiveSpfRecord(activeResult.getActiveSpfRecord());
            dto.setActiveSpfRelayIp(activeResult.getActiveSpfRelayIp());
            dto.setActiveDkimStatus(activeResult.getActiveDkimStatus());
            dto.setActiveDkimRecord(activeResult.getActiveDkimRecord());
            dto.setActiveDkimSelector(activeResult.getActiveDkimSelector());
            dto.setActiveDmarcStatus(activeResult.getActiveDmarcStatus());
            dto.setActiveDmarcPolicy(activeResult.getActiveDmarcPolicy());
            dto.setActiveVerificationSummary(activeResult.getSummary());

            entity.setIndependentVerification(activeResult.getSummary());
            entity.setCurrentDnsObservation(activeResult.getDnsObservationJson());
            try {
                headerAnalysisRepository.save(entity);
                headerAnalysisRepository.flush();
            } catch (Exception ignored) {
            }
        }

        return dto;
    }

    @Transactional
    public List<ReceivedHopDto> getReceivedHops(UUID emailId) {
        EmailEvidence evidence = evidenceRepository.findById(emailId)
                .orElseThrow(() -> new IllegalArgumentException("Evidence not found: " + emailId));

        AnalysisRun run = analysisRunRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)
                .orElseThrow(() -> new IllegalArgumentException("Analysis run not found for evidence: " + emailId));

        List<ReceivedHop> hops = receivedHopRepository.findByAnalysisRunIdOrderByHopOrderAsc(run.getId());
        if (hops.isEmpty()) {
            try {
                analyzeAndSaveHeaders(emailId);
            } catch (org.springframework.dao.DataIntegrityViolationException ignored) {
            }
            hops = receivedHopRepository.findByAnalysisRunIdOrderByHopOrderAsc(run.getId());
        }

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

    public HeaderAnalysisDto analyzeStream(String storageKey) {
        try (InputStream in = storageService.loadAsResource(storageKey).getInputStream()) {
            return analyzeStream(in);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse email headers", e);
        }
    }

    public HeaderAnalysisDto analyzeStream(InputStream in) {
        try {
            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);
            MimeMessage message = new MimeMessage(session, in);

            Map<String, List<String>> rawHeaders = new LinkedHashMap<>();
            Enumeration<Header> allHeaders = message.getAllHeaders();
            while (allHeaders.hasMoreElements()) {
                Header h = allHeaders.nextElement();
                rawHeaders.computeIfAbsent(h.getName(), k -> new ArrayList<>()).add(h.getValue());
            }

            String fromRaw = getFirstHeader(message, "From");
            String replyToRaw = getFirstHeader(message, "Reply-To");
            String returnPathRaw = getFirstHeader(message, "Return-Path");
            String dkimSigRaw = getFirstHeader(message, "DKIM-Signature");
            String authResultsRaw = getFirstHeader(message, "Authentication-Results");

            String fromAddress = extractEmailAddress(fromRaw);
            String fromDomain = extractDomain(fromAddress);
            String replyToAddress = extractEmailAddress(replyToRaw);
            String returnPathAddress = extractEmailAddress(returnPathRaw);

            String dkimDomain = null;
            if (dkimSigRaw != null) {
                Matcher dkimMatcher = DKIM_DOMAIN_PATTERN.matcher(dkimSigRaw);
                if (dkimMatcher.find()) {
                    dkimDomain = dkimMatcher.group(1).toLowerCase(Locale.ROOT);
                }
            }

            String reportedSpf = extractAuthResult(authResultsRaw, AUTH_SPF_PATTERN);
            String reportedDkim = extractAuthResult(authResultsRaw, AUTH_DKIM_PATTERN);
            String reportedDmarc = extractAuthResult(authResultsRaw, AUTH_DMARC_PATTERN);
            String reportedArc = extractAuthResult(authResultsRaw, AUTH_ARC_PATTERN);

            List<String> mismatchFlags = new ArrayList<>();

            if (replyToAddress != null && fromDomain != null) {
                String replyToDomain = extractDomain(replyToAddress);
                if (replyToDomain != null && !replyToDomain.equalsIgnoreCase(fromDomain)) {
                    mismatchFlags.add("REPLY_TO_DOMAIN_MISMATCH: Reply-To domain (@" + replyToDomain + ") differs from From domain (@" + fromDomain + ")");
                }
            }

            if (returnPathAddress != null && fromDomain != null) {
                String returnPathDomain = extractDomain(returnPathAddress);
                if (returnPathDomain != null && !returnPathDomain.equalsIgnoreCase(fromDomain)) {
                    mismatchFlags.add("RETURN_PATH_MISMATCH: Return-Path envelope (@" + returnPathDomain + ") differs from From domain (@" + fromDomain + ")");
                }
            }

            if (dkimDomain != null && fromDomain != null) {
                if (!fromDomain.equalsIgnoreCase(dkimDomain) && !fromDomain.endsWith("." + dkimDomain)) {
                    mismatchFlags.add("DKIM_DOMAIN_MISMATCH: DKIM signing domain (" + dkimDomain + ") does not match From domain (" + fromDomain + ")");
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
                        if (fromAddress != null && !nameEmail.equalsIgnoreCase(fromAddress)) {
                            mismatchFlags.add("SUSPICIOUS_DISPLAY_NAME: Display name mimics email address (" + nameEmail + ") while true sender is (" + fromAddress + ")");
                        }
                    }
                }
            }

            String verificationNote = "Authentication results (SPF, DKIM, DMARC, ARC) are extracted as reported by receiving server headers in the original message. They are not independently verified in offline state.";

            HeaderAnalysisDto dto = new HeaderAnalysisDto(
                    null,
                    fromAddress,
                    fromDomain,
                    replyToAddress,
                    returnPathAddress,
                    dkimDomain,
                    reportedSpf,
                    reportedDkim,
                    reportedDmarc,
                    reportedArc,
                    mismatchFlags,
                    verificationNote,
                    rawHeaders,
                    0
            );

            if (activeEmailAuthService != null) {
                String returnPathDomain = extractDomain(returnPathAddress);
                ActiveEmailAuthService.ActiveAuthResult activeRes = activeEmailAuthService.performActiveAuthentication(
                        fromDomain, returnPathDomain, dkimSigRaw, Collections.emptyList()
                );
                dto.setActiveSpfResult(activeRes.getActiveSpfResult());
                dto.setActiveSpfRecord(activeRes.getActiveSpfRecord());
                dto.setActiveSpfRelayIp(activeRes.getActiveSpfRelayIp());
                dto.setActiveDkimStatus(activeRes.getActiveDkimStatus());
                dto.setActiveDkimRecord(activeRes.getActiveDkimRecord());
                dto.setActiveDkimSelector(activeRes.getActiveDkimSelector());
                dto.setActiveDmarcStatus(activeRes.getActiveDmarcStatus());
                dto.setActiveDmarcPolicy(activeRes.getActiveDmarcPolicy());
                dto.setActiveVerificationSummary(activeRes.getSummary());
            }

            return dto;

        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse email headers", e);
        }
    }

    public List<ReceivedHopDto> extractHopsFromStream(String storageKey) {
        try (InputStream in = storageService.loadAsResource(storageKey).getInputStream()) {
            return extractHopsFromStream(in);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to extract received hops", e);
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

    private String extractAuthResult(String authResultsRaw, Pattern pattern) {
        if (authResultsRaw == null) return null;
        Matcher m = pattern.matcher(authResultsRaw);
        if (m.find()) {
            return m.group(1).toLowerCase(Locale.ROOT);
        }
        return null;
    }

    private static final Pattern JSON_STRING_PATTERN = Pattern.compile("\"((?:\\\\\"|[^\"])*)\"");
    private static final Pattern HEADER_ENTRY_PATTERN = Pattern.compile("\"((?:\\\\\"|[^\"])*)\"\\s*:\\s*\\[(.*?)\\]");

    private String formatFlagsJson(List<String> flags) {
        if (flags == null || flags.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < flags.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(flags.get(i))).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private List<String> parseFlagsJson(String json) {
        List<String> list = new ArrayList<>();
        if (json == null || json.trim().isEmpty() || json.equals("[]")) return list;
        String content = json.trim();
        if (content.startsWith("[") && content.endsWith("]")) {
            content = content.substring(1, content.length() - 1).trim();
        }
        if (content.isEmpty()) return list;
        Matcher m = JSON_STRING_PATTERN.matcher(content);
        while (m.find()) {
            list.add(unescapeJson(m.group(1)));
        }
        return list;
    }

    private String formatRawHeadersJson(Map<String, List<String>> map) {
        if (map == null || map.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean firstKey = true;
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            if (!firstKey) sb.append(",");
            firstKey = false;
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":[");
            List<String> vals = entry.getValue();
            for (int i = 0; i < vals.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(escapeJson(vals.get(i))).append("\"");
            }
            sb.append("]");
        }
        sb.append("}");
        return sb.toString();
    }

    private Map<String, List<String>> parseRawHeadersJson(String json) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        if (json == null || json.trim().isEmpty() || json.equals("{}")) return map;
        Matcher m = HEADER_ENTRY_PATTERN.matcher(json);
        while (m.find()) {
            String key = unescapeJson(m.group(1));
            String arrayContent = m.group(2);
            List<String> values = new ArrayList<>();
            Matcher vm = JSON_STRING_PATTERN.matcher(arrayContent);
            while (vm.find()) {
                values.add(unescapeJson(vm.group(1)));
            }
            map.put(key, values);
        }
        return map;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\\", "\\");
    }

    private void populateActiveAuthFromObservation(HeaderAnalysisDto dto, String obsJson, String summary) {
        dto.setActiveSpfResult(extractJsonField(obsJson, "activeSpf", "status"));
        dto.setActiveSpfRecord(extractJsonField(obsJson, "activeSpf", "record"));
        dto.setActiveSpfRelayIp(extractJsonField(obsJson, "activeSpf", "evaluatedIp"));
        dto.setActiveDkimStatus(extractJsonField(obsJson, "activeDkim", "status"));
        dto.setActiveDkimRecord(extractJsonField(obsJson, "activeDkim", "record"));
        dto.setActiveDkimSelector(extractJsonField(obsJson, "activeDkim", "selector"));
        dto.setActiveDmarcStatus(extractJsonField(obsJson, "activeDmarc", "status"));
        dto.setActiveDmarcPolicy(extractJsonField(obsJson, "activeDmarc", "record"));
        dto.setActiveVerificationSummary(summary != null ? summary : "Active DNS authentication observations persistent.");
    }

    private String extractJsonField(String json, String parentKey, String childKey) {
        if (json == null || parentKey == null || childKey == null) {
            return null;
        }
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(parentKey) + "\"\\s*:\\s*\\{[^}]*?\"" + Pattern.quote(childKey) + "\"\\s*:\\s*(?:\"([^\"]*)\"|(null))");
        Matcher m = pattern.matcher(json);
        if (m.find()) {
            if ("null".equals(m.group(2))) {
                return null;
            }
            return m.group(1);
        }
        return null;
    }
}

package com.emailforensics.backend.risk;

import com.emailforensics.backend.analysis.AnalysisResult;
import com.emailforensics.backend.analysis.AnalysisResultRepository;
import com.emailforensics.backend.common.ResourceNotFoundException;
import com.emailforensics.backend.content.ContentAnalysisService;
import com.emailforensics.backend.dto.content.ContentAnalysisDto;
import com.emailforensics.backend.dto.headers.HeaderAnalysisDto;
import com.emailforensics.backend.dto.headers.ReceivedHopDto;
import com.emailforensics.backend.dto.infrastructure.IpArtifactDto;
import com.emailforensics.backend.dto.risk.ChannelStatusDto;
import com.emailforensics.backend.dto.risk.RiskBreakdownDto;
import com.emailforensics.backend.dto.risk.RiskCategoryScoreDto;
import com.emailforensics.backend.dto.risk.RiskFactorDto;
import com.emailforensics.backend.dto.urlintel.ExtractedUrlDto;
import com.emailforensics.backend.dto.visual.QrArtifactDto;
import com.emailforensics.backend.dto.visual.VisualArtifactDto;
import com.emailforensics.backend.evidence.AnalysisRun;
import com.emailforensics.backend.evidence.AnalysisRunRepository;
import com.emailforensics.backend.evidence.EmailEvidence;
import com.emailforensics.backend.evidence.EmailEvidenceRepository;
import com.emailforensics.backend.headers.HeaderAnalysisService;
import com.emailforensics.backend.infrastructure.IpArtifactService;
import com.emailforensics.backend.urlintel.UrlAnalysisService;
import com.emailforensics.backend.visual.VisualAnalysisService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class RiskScoringService {

    private static final Logger log = LoggerFactory.getLogger(RiskScoringService.class);

    public static final String CAT_IDENTITY = "IDENTITY";
    public static final String CAT_AUTHENTICATION = "AUTHENTICATION";
    public static final String CAT_URL = "URL";
    public static final String CAT_INFRASTRUCTURE = "INFRASTRUCTURE";
    public static final String CAT_VISUAL = "VISUAL";
    public static final String CAT_CONTENT = "CONTENT";
    public static final String CAT_MACHINE_LEARNING = "MACHINE_LEARNING";

    public static final String ENGINE_VERSION = "v1.0.0";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RiskFactorRepository riskFactorRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final EmailEvidenceRepository evidenceRepository;
    private final AnalysisRunRepository analysisRunRepository;
    private final HeaderAnalysisService headerAnalysisService;
    private final UrlAnalysisService urlAnalysisService;
    private final IpArtifactService ipArtifactService;
    private final VisualAnalysisService visualAnalysisService;
    private final ContentAnalysisService contentAnalysisService;

    public RiskScoringService(RiskFactorRepository riskFactorRepository,
                              AnalysisResultRepository analysisResultRepository,
                              EmailEvidenceRepository evidenceRepository,
                              AnalysisRunRepository analysisRunRepository,
                              HeaderAnalysisService headerAnalysisService,
                              UrlAnalysisService urlAnalysisService,
                              IpArtifactService ipArtifactService,
                              VisualAnalysisService visualAnalysisService,
                              ContentAnalysisService contentAnalysisService) {
        this.riskFactorRepository = riskFactorRepository;
        this.analysisResultRepository = analysisResultRepository;
        this.evidenceRepository = evidenceRepository;
        this.analysisRunRepository = analysisRunRepository;
        this.headerAnalysisService = headerAnalysisService;
        this.urlAnalysisService = urlAnalysisService;
        this.ipArtifactService = ipArtifactService;
        this.visualAnalysisService = visualAnalysisService;
        this.contentAnalysisService = contentAnalysisService;
    }

    @Transactional
    public RiskBreakdownDto evaluateAndSaveRisk(UUID emailId) {
        EmailEvidence evidence = evidenceRepository.findById(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence not found: " + emailId));

        AnalysisRun run = analysisRunRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("No analysis run found for evidence: " + emailId));

        HeaderAnalysisDto headerAnalysis = null;
        try {
            headerAnalysis = headerAnalysisService.getHeaderAnalysis(emailId);
        } catch (Exception e) {
            log.warn("Could not retrieve header analysis for risk scoring: {}", e.getMessage());
        }

        List<ReceivedHopDto> hops = Collections.emptyList();
        try {
            hops = headerAnalysisService.getReceivedHops(emailId);
        } catch (Exception e) {
            log.warn("Could not retrieve received hops for risk scoring: {}", e.getMessage());
        }

        List<ExtractedUrlDto> urls = Collections.emptyList();
        try {
            urls = urlAnalysisService.getExtractedUrls(emailId);
        } catch (Exception e) {
            log.warn("Could not retrieve extracted URLs for risk scoring: {}", e.getMessage());
        }

        List<IpArtifactDto> ips = Collections.emptyList();
        try {
            ips = ipArtifactService.getInfrastructure(emailId);
        } catch (Exception e) {
            log.warn("Could not retrieve infrastructure IPs for risk scoring: {}", e.getMessage());
        }

        List<VisualArtifactDto> visuals = Collections.emptyList();
        try {
            visuals = visualAnalysisService.getVisualArtifacts(emailId);
        } catch (Exception e) {
            log.warn("Could not retrieve visual artifacts for risk scoring: {}", e.getMessage());
        }

        Optional<ContentAnalysisDto> contentOpt = Optional.empty();
        try {
            contentOpt = contentAnalysisService.getContentAnalysis(emailId);
        } catch (Exception e) {
            log.warn("Could not retrieve content analysis for risk scoring: {}", e.getMessage());
        }

        List<RiskFactor> rawFactors = new ArrayList<>();
        Set<String> deduplicationKeys = new HashSet<>();

        if (headerAnalysis != null) {
            collectIdentityFactors(headerAnalysis, run, rawFactors, deduplicationKeys);
            collectAuthenticationFactors(headerAnalysis, run, rawFactors, deduplicationKeys);
        }

        collectInfrastructureFactors(hops, ips, run, rawFactors, deduplicationKeys);
        collectUrlFactors(urls, run, rawFactors, deduplicationKeys);
        collectVisualFactors(visuals, run, rawFactors, deduplicationKeys);

        if (contentOpt.isPresent()) {
            collectContentFactors(contentOpt.get(), run, rawFactors, deduplicationKeys);
            collectMachineLearningFactors(contentOpt.get(), run, rawFactors, deduplicationKeys);
        }

        int totalUncapped = 0;
        Map<String, Integer> categoryScores = new LinkedHashMap<>();
        Map<String, Integer> categoryCounts = new LinkedHashMap<>();

        initializeCategoryMaps(categoryScores, categoryCounts);

        for (RiskFactor factor : rawFactors) {
            totalUncapped += factor.getWeightPoints();
            String cat = factor.getCategory();
            categoryScores.put(cat, categoryScores.getOrDefault(cat, 0) + factor.getWeightPoints());
            categoryCounts.put(cat, categoryCounts.getOrDefault(cat, 0) + 1);
        }

        int deterministicScore = Math.min(100, Math.max(0, totalUncapped));
        String riskLevel = computeRiskLevel(deterministicScore);

        List<ChannelStatusDto> channelStatuses = evaluateChannelStatuses(headerAnalysis, hops, urls, visuals, contentOpt);
        List<String> evaluatedChannels = extractEvaluatedChannels(channelStatuses);
        int completeness = calculateCompleteness(evaluatedChannels);
        int multiCategorySupport = calculateMultiCategorySupport(categoryScores);
        int confidenceScore = Math.min(100, Math.max(0, completeness + multiCategorySupport));
        String confidenceLevel = computeConfidenceLevel(confidenceScore);
        String confidenceExplanation = buildConfidenceExplanation(confidenceScore, confidenceLevel, completeness, multiCategorySupport, categoryScores);

        String categoryScoresJson = formatCategoryScoresJson(categoryScores, categoryCounts);

        riskFactorRepository.deleteByAnalysisRunId(run.getId());
        List<RiskFactor> savedFactors = riskFactorRepository.saveAll(rawFactors);

        AnalysisResult assessment = analysisResultRepository.findByAnalysisRunId(run.getId())
                .orElseGet(() -> new AnalysisResult(run));

        if (assessment.getRiskResultId() == null) {
            assessment.setRiskResultId(UUID.randomUUID());
        }
        if (assessment.getRiskCreatedAt() == null) {
            assessment.setRiskCreatedAt(Instant.now());
        }
        assessment.setAnalysisRun(run);
        assessment.setDeterministicRiskScore(deterministicScore);
        assessment.setRiskLevel(riskLevel);
        assessment.setEvidenceConfidenceScore(confidenceScore);
        assessment.setConfidenceLevel(confidenceLevel);
        assessment.setConfidenceExplanation(confidenceExplanation);
        assessment.setCategoryScoresJson(categoryScoresJson);
        assessment.setTotalUncappedScore(totalUncapped);
        assessment.setEngineVersion(ENGINE_VERSION);
        assessment.setEvaluatedAt(Instant.now());

        analysisResultRepository.save(assessment);

        List<RiskCategoryScoreDto> categoryScoreDtos = buildCategoryScoreDtos(categoryScores, categoryCounts);
        List<RiskFactorDto> factorDtos = toFactorDtos(savedFactors);

        RiskBreakdownDto dto = new RiskBreakdownDto(
                run.getId(),
                deterministicScore,
                riskLevel,
                confidenceScore,
                confidenceLevel,
                confidenceExplanation,
                totalUncapped,
                ENGINE_VERSION,
                assessment.getEvaluatedAt(),
                categoryScoreDtos,
                factorDtos
        );
        List<String> triggeredCategories = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : categoryScores.entrySet()) {
            if (entry.getValue() > 0) {
                triggeredCategories.add(entry.getKey());
            }
        }

        dto.setEvaluatedChannels(evaluatedChannels);
        dto.setChannelStatuses(channelStatuses);
        dto.setTriggeredCategories(triggeredCategories);
        return dto;
    }

    @Transactional(readOnly = true)
    public RiskBreakdownDto getRiskBreakdown(UUID emailId) {
        AnalysisRun run = analysisRunRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)
                .orElse(null);
        if (run == null) {
            return null;
        }

        Optional<AnalysisResult> assessmentOpt = analysisResultRepository.findByAnalysisRunId(run.getId());
        if (assessmentOpt.isEmpty() || !assessmentOpt.get().hasRiskAssessment()) {
            return null;
        }

        AnalysisResult assessment = assessmentOpt.get();
        List<RiskFactor> factors = riskFactorRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run.getId());
        List<RiskCategoryScoreDto> categoryScores = parseCategoryScoresJson(assessment.getCategoryScoresJson());
        List<RiskFactorDto> factorDtos = toFactorDtos(factors);

        RiskBreakdownDto dto = new RiskBreakdownDto(
                run.getId(),
                assessment.getDeterministicRiskScore(),
                assessment.getRiskLevel(),
                assessment.getEvidenceConfidenceScore(),
                assessment.getConfidenceLevel(),
                assessment.getConfidenceExplanation(),
                assessment.getTotalUncappedScore(),
                assessment.getEngineVersion(),
                assessment.getEvaluatedAt(),
                categoryScores,
                factorDtos
        );
        List<String> triggeredCategories = new ArrayList<>();
        if (categoryScores != null) {
            for (RiskCategoryScoreDto cat : categoryScores) {
                if (cat.getScore() != null && cat.getScore() > 0) {
                    triggeredCategories.add(cat.getCategory());
                }
            }
        }
        dto.setTriggeredCategories(triggeredCategories);

        try {
            HeaderAnalysisDto headerAnalysis = headerAnalysisService.getHeaderAnalysis(emailId);
            List<ReceivedHopDto> hops = headerAnalysisService.getReceivedHops(emailId);
            List<ExtractedUrlDto> urls = urlAnalysisService.getExtractedUrls(emailId);
            List<VisualArtifactDto> visuals = visualAnalysisService.getVisualArtifacts(emailId);
            Optional<ContentAnalysisDto> contentOpt = contentAnalysisService.getContentAnalysis(emailId);
            List<ChannelStatusDto> channelStatuses = evaluateChannelStatuses(headerAnalysis, hops, urls, visuals, contentOpt);
            dto.setChannelStatuses(channelStatuses);
            dto.setEvaluatedChannels(extractEvaluatedChannels(channelStatuses));
        } catch (Exception ignored) {
        }
        return dto;
    }

    private void collectIdentityFactors(HeaderAnalysisDto h, AnalysisRun run, List<RiskFactor> factors, Set<String> deduplicationKeys) {
        List<String> flags = h.getIdentityMismatchFlags();
        if (flags != null) {
            for (String flag : flags) {
                if (flag == null) continue;
                String normalized = flag.trim().toUpperCase(Locale.ROOT);
                if (normalized.equals("FROM_REPLYTO_MISMATCH")
                        || normalized.startsWith("REPLY_TO_DOMAIN_MISMATCH")
                        || normalized.contains("REPLY_TO")
                        || normalized.contains("REPLYTO")) {
                    addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                            UUID.randomUUID(), run, CAT_IDENTITY, "IDENTITY_REPLY_TO_MISMATCH", "MEDIUM", 15,
                            "Reply-To address uses a different domain than the From header address.",
                            "From: " + h.getFromAddress() + " vs Reply-To: " + h.getReplyTo()
                    ), "REPLY_TO");
                } else if (normalized.equals("FROM_RETURNPATH_MISMATCH")
                        || normalized.startsWith("RETURN_PATH_MISMATCH")
                        || normalized.contains("RETURN_PATH")
                        || normalized.contains("RETURNPATH")) {
                    addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                            UUID.randomUUID(), run, CAT_IDENTITY, "IDENTITY_RETURN_PATH_MISMATCH", "LOW", 10,
                            "Envelope Return-Path domain differs from the message From header domain.",
                            "From: " + h.getFromAddress() + " vs Return-Path: " + h.getReturnPath()
                    ), "RETURN_PATH");
                } else if (normalized.equals("DISPLAY_NAME_EMAIL_MISMATCH")
                        || normalized.equals("DISPLAY_NAME_SPOOFING")
                        || normalized.startsWith("SUSPICIOUS_DISPLAY_NAME")
                        || normalized.contains("DISPLAY_NAME")) {
                    addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                            UUID.randomUUID(), run, CAT_IDENTITY, "IDENTITY_DISPLAY_NAME_SPOOFING", "HIGH", 20,
                            "From display name contains an embedded email address or deceptive brand mismatch.",
                            "From: " + h.getFromAddress()
                    ), "DISPLAY_NAME");
                } else if (normalized.startsWith("DKIM_DOMAIN_MISMATCH") || normalized.contains("DKIM_DOMAIN")) {
                    addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                            UUID.randomUUID(), run, CAT_IDENTITY, "IDENTITY_DKIM_DOMAIN_MISMATCH", "LOW", 10,
                            "DKIM signature domain does not match From header domain.",
                            "From: " + h.getFromAddress() + " vs DKIM: " + h.getDkimDomain()
                    ), "DKIM_MISMATCH");
                }
            }
        }

        // Direct check fallback: Compare Reply-To and From domain directly
        if (h.getReplyTo() != null && (h.getFromDomain() != null || h.getFromAddress() != null)) {
            String fromDomain = h.getFromDomain() != null ? h.getFromDomain().trim().toLowerCase(Locale.ROOT) : extractDomain(h.getFromAddress());
            String replyToDomain = extractDomain(h.getReplyTo());
            if (fromDomain != null && replyToDomain != null && !fromDomain.equalsIgnoreCase(replyToDomain)) {
                addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                        UUID.randomUUID(), run, CAT_IDENTITY, "IDENTITY_REPLY_TO_MISMATCH", "MEDIUM", 15,
                        "Reply-To address uses a different domain than the From header address.",
                        "From: " + h.getFromAddress() + " vs Reply-To: " + h.getReplyTo()
                ), "REPLY_TO");
            }
        }

        // Direct check fallback: Compare Return-Path and From domain directly
        if (h.getReturnPath() != null && (h.getFromDomain() != null || h.getFromAddress() != null)) {
            String fromDomain = h.getFromDomain() != null ? h.getFromDomain().trim().toLowerCase(Locale.ROOT) : extractDomain(h.getFromAddress());
            String returnPathDomain = extractDomain(h.getReturnPath());
            if (fromDomain != null && returnPathDomain != null && !fromDomain.equalsIgnoreCase(returnPathDomain)) {
                addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                        UUID.randomUUID(), run, CAT_IDENTITY, "IDENTITY_RETURN_PATH_MISMATCH", "LOW", 10,
                        "Envelope Return-Path domain differs from the message From header domain.",
                        "From: " + h.getFromAddress() + " vs Return-Path: " + h.getReturnPath()
                ), "RETURN_PATH");
            }
        }
    }

    private String extractDomain(String emailAddress) {
        if (emailAddress == null) return null;
        int atIndex = emailAddress.lastIndexOf('@');
        if (atIndex >= 0 && atIndex < emailAddress.length() - 1) {
            String domain = emailAddress.substring(atIndex + 1).trim().toLowerCase(Locale.ROOT);
            if (domain.endsWith(">")) {
                domain = domain.substring(0, domain.length() - 1).trim();
            }
            return domain;
        }
        return null;
    }

    private void collectAuthenticationFactors(HeaderAnalysisDto h, AnalysisRun run, List<RiskFactor> factors, Set<String> deduplicationKeys) {
        if ("fail".equalsIgnoreCase(h.getReportedDmarc())) {
            addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                    UUID.randomUUID(), run, CAT_AUTHENTICATION, "AUTH_DMARC_FAIL", "HIGH", 25,
                    "Reported DMARC policy check failed.",
                    "dmarc=fail reported in receiving server headers"
            ), "DMARC_FAIL");
        }

        if ("fail".equalsIgnoreCase(h.getReportedSpf())) {
            addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                    UUID.randomUUID(), run, CAT_AUTHENTICATION, "AUTH_SPF_FAIL", "MEDIUM", 15,
                    "Reported SPF envelope verification failed.",
                    "spf=fail reported in receiving server headers"
            ), "SPF_FAIL");
        } else if ("softfail".equalsIgnoreCase(h.getReportedSpf())) {
            addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                    UUID.randomUUID(), run, CAT_AUTHENTICATION, "AUTH_SPF_SOFTFAIL", "LOW", 10,
                    "Reported SPF envelope verification softfailed.",
                    "spf=softfail reported in receiving server headers"
            ), "SPF_SOFTFAIL");
        }

        if ("fail".equalsIgnoreCase(h.getReportedDkim())) {
            addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                    UUID.randomUUID(), run, CAT_AUTHENTICATION, "AUTH_DKIM_FAIL", "MEDIUM", 15,
                    "Reported DKIM signature verification failed.",
                    "dkim=fail reported in receiving server headers"
            ), "DKIM_FAIL");
        }
    }

    private void collectInfrastructureFactors(List<ReceivedHopDto> hops, List<IpArtifactDto> ips,
                                             AnalysisRun run, List<RiskFactor> factors, Set<String> deduplicationKeys) {
        for (ReceivedHopDto hop : hops) {
            if (hop.getDelaySeconds() != null && hop.getDelaySeconds() > 3600) {
                addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                        UUID.randomUUID(), run, CAT_INFRASTRUCTURE, "INFRA_HOP_DELAY", "LOW", 10,
                        "Excessive relay delivery delay (> 1 hour) observed at intermediate hop.",
                        "Hop #" + hop.getHopOrder() + " delay=" + hop.getDelaySeconds() + "s"
                ), "HOP_DELAY");
                break;
            }
        }

        for (ReceivedHopDto hop : hops) {
            if (hop.getParsingWarning() != null && !hop.getParsingWarning().isBlank()) {
                addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                        UUID.randomUUID(), run, CAT_INFRASTRUCTURE, "INFRA_HOP_MALFORMED", "LOW", 10,
                        "Received relay hop line contains malformed syntax or parsing anomalies.",
                        "Hop #" + hop.getHopOrder() + ": " + hop.getParsingWarning()
                ), "HOP_MALFORMED");
                break;
            }
        }

        for (IpArtifactDto ip : ips) {
            if (Boolean.FALSE.equals(ip.getIsPrivate()) && "HIGH_RISK".equalsIgnoreCase(ip.getLookupStatus())) {
                addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                        UUID.randomUUID(), run, CAT_INFRASTRUCTURE, "INFRA_SUSPICIOUS_IP", "MEDIUM", 15,
                        "Public relay IP flagged as high-risk or anomalous external address.",
                        "IP: " + ip.getIpAddress()
                ), "IP:" + ip.getIpAddress());
            }
        }
    }

    private void collectUrlFactors(List<ExtractedUrlDto> urls, AnalysisRun run,
                                   List<RiskFactor> factors, Set<String> deduplicationKeys) {
        for (ExtractedUrlDto url : urls) {
            String domain = url.getDomain() != null ? url.getDomain().toLowerCase(Locale.ROOT) : "";
            String dedupeKey = domain.isEmpty() ? url.getNormalizedUrl() : domain;

            if (Boolean.TRUE.equals(url.getIsIpUrl())) {
                addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                        UUID.randomUUID(), run, CAT_URL, "URL_IP_LITERAL", "HIGH", 20,
                        "URL host uses a raw numeric IP address instead of a domain name.",
                        "URL: " + url.getNormalizedUrl()
                ), "IP_URL:" + dedupeKey);
            }

            if (Boolean.TRUE.equals(url.getIsShortened())) {
                addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                        UUID.randomUUID(), run, CAT_URL, "URL_SHORTENER", "MEDIUM", 15,
                        "URL utilizes a public URL shortening service that conceals destination.",
                        "Domain: " + domain
                ), "SHORTENER:" + dedupeKey);
            }

            if (Boolean.TRUE.equals(url.getAnchorMismatch())) {
                addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                        UUID.randomUUID(), run, CAT_URL, "URL_ANCHOR_MISMATCH", "HIGH", 25,
                        "Hyperlink anchor text appears to represent a different domain than actual destination.",
                        "Anchor: \"" + url.getAnchorText() + "\" -> " + domain
                ), "ANCHOR_MISMATCH:" + dedupeKey);
            }

            if (Boolean.TRUE.equals(url.getUnusualPort())) {
                addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                        UUID.randomUUID(), run, CAT_URL, "URL_UNUSUAL_PORT", "MEDIUM", 15,
                        "URL destination connects to an uncommon or non-standard network port.",
                        "URL: " + url.getNormalizedUrl()
                ), "UNUSUAL_PORT:" + dedupeKey);
            }

            if (Boolean.TRUE.equals(url.getLookalike())) {
                addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                        UUID.randomUUID(), run, CAT_URL, "URL_LOOKALIKE", "HIGH", 25,
                        "Domain employs Punycode, homoglyphs, or lookalike typosquatting characters.",
                        "Domain: " + domain
                ), "LOOKALIKE:" + dedupeKey);
            }

            if (Boolean.TRUE.equals(url.getSubdomainExcess())) {
                addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                        UUID.randomUUID(), run, CAT_URL, "URL_EXCESSIVE_SUBDOMAINS", "LOW", 10,
                        "Domain exhibits excessive subdomain nesting commonly used for evasion.",
                        "Domain: " + domain
                ), "SUBDOMAIN_EXCESS:" + dedupeKey);
            }

            if ("NXDOMAIN".equalsIgnoreCase(url.getDnsStatus()) || "FAILED".equalsIgnoreCase(url.getDnsStatus())) {
                addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                        UUID.randomUUID(), run, CAT_URL, "URL_DNS_NXDOMAIN", "HIGH", 20,
                        "Domain failed DNS resolution (NXDOMAIN or unresolvable).",
                        "Domain: " + domain
                ), "DNS_NXDOMAIN:" + dedupeKey);
            }
        }
    }

    private void collectVisualFactors(List<VisualArtifactDto> visuals, AnalysisRun run,
                                      List<RiskFactor> factors, Set<String> deduplicationKeys) {
        for (VisualArtifactDto visual : visuals) {
            List<QrArtifactDto> qrs = visual.getQrArtifacts();
            if (qrs != null && !qrs.isEmpty()) {
                for (QrArtifactDto qr : qrs) {
                    addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                            UUID.randomUUID(), run, CAT_VISUAL, "VISUAL_QR_CODE_FOUND", "MEDIUM", 15,
                            "Embedded QR code detected presenting a potential Quishing evasion vector.",
                            "Image: " + visual.getFilename()
                    ), "QR_FOUND");

                    if (qr.getNormalizedUrl() != null && !qr.getNormalizedUrl().isBlank()) {
                        String lowerUrl = qr.getNormalizedUrl().toLowerCase(Locale.ROOT);
                        if (lowerUrl.contains("login") || lowerUrl.contains("signin") || lowerUrl.contains("auth") || lowerUrl.contains("account")) {
                            addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                                    UUID.randomUUID(), run, CAT_VISUAL, "VISUAL_QR_MALICIOUS_URL", "HIGH", 25,
                                    "Decoded QR code directs to a sensitive authentication or credential destination.",
                                    "QR Destination: " + qr.getNormalizedUrl()
                            ), "QR_URL:" + qr.getNormalizedUrl());
                        }
                    }
                }
            }
        }
    }

    private void collectContentFactors(ContentAnalysisDto c, AnalysisRun run,
                                       List<RiskFactor> factors, Set<String> deduplicationKeys) {
        if (c.isPromptInjectionDetected()) {
            addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                    UUID.randomUUID(), run, CAT_CONTENT, "CONTENT_PROMPT_INJECTION", "CRITICAL", 30,
                    "Adversarial prompt injection pattern detected in message attempting to override AI instructions.",
                    "Prompt injection matched across message content"
            ), "PROMPT_INJECTION");
        }

        if (c.isCredentialRequest()) {
            addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                    UUID.randomUUID(), run, CAT_CONTENT, "CONTENT_CREDENTIAL_HARVESTING", "HIGH", 25,
                    "Social engineering indicator requesting user passwords, credentials, or login action.",
                    "social_engineering=CREDENTIAL_REQUEST"
            ), "CREDENTIAL_HARVESTING");
        }

        if (c.isPaymentInstruction()) {
            addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                    UUID.randomUUID(), run, CAT_CONTENT, "CONTENT_PAYMENT_INSTRUCTION", "HIGH", 20,
                    "Social engineering indicator containing wire transfer, invoice, or cryptocurrency demands.",
                    "social_engineering=PAYMENT_INSTRUCTION"
            ), "PAYMENT_INSTRUCTION");
        }

        if (c.isImpersonation()) {
            addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                    UUID.randomUUID(), run, CAT_CONTENT, "CONTENT_IMPERSONATION", "MEDIUM", 15,
                    "Social engineering indicator impersonating executive, IT support, or authority.",
                    "social_engineering=IMPERSONATION"
            ), "IMPERSONATION");
        }

        if (c.isUrgency()) {
            addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                    UUID.randomUUID(), run, CAT_CONTENT, "CONTENT_URGENCY", "LOW", 10,
                    "Social engineering indicator utilizing artificial urgency, deadlines, or negative consequence threats.",
                    "social_engineering=URGENCY"
            ), "URGENCY");
        }

        if (c.isSecrecy()) {
            addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                    UUID.randomUUID(), run, CAT_CONTENT, "CONTENT_SECRECY", "LOW", 10,
                    "Social engineering indicator urging confidentiality or bypassing normal procedures.",
                    "social_engineering=SECRECY"
            ), "SECRECY");
        }
    }

    private void collectMachineLearningFactors(ContentAnalysisDto c, AnalysisRun run,
                                               List<RiskFactor> factors, Set<String> deduplicationKeys) {
        if ("PHISHING".equalsIgnoreCase(c.getPredictedLabel()) && c.getProbability() != null) {
            double prob = c.getProbability();
            if (prob >= 0.80) {
                addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                        UUID.randomUUID(), run, CAT_MACHINE_LEARNING, "ML_PHISHING_HIGH_CONFIDENCE", "HIGH", 25,
                        "Tribuo ML baseline classifier predicted message is phishing with high confidence (>= 0.80).",
                        "Tribuo probability: " + String.format(Locale.ROOT, "%.2f", prob)
                ), "ML_PREDICTION");
            } else if (prob >= 0.50) {
                addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                        UUID.randomUUID(), run, CAT_MACHINE_LEARNING, "ML_PHISHING_SUSPICIOUS", "MEDIUM", 15,
                        "Tribuo ML baseline classifier predicted message is suspicious phishing (probability 0.50 - 0.79).",
                        "Tribuo probability: " + String.format(Locale.ROOT, "%.2f", prob)
                ), "ML_PREDICTION");
            }
        }
    }

    private void addFactorIfUnique(List<RiskFactor> factors, Set<String> deduplicationKeys, RiskFactor factor, String dedupeSuffix) {
        String key = factor.getCategory() + ":" + factor.getFactorCode() + ":" + dedupeSuffix;
        if (deduplicationKeys.add(key)) {
            factors.add(factor);
        }
    }

    private void initializeCategoryMaps(Map<String, Integer> scores, Map<String, Integer> counts) {
        scores.put(CAT_IDENTITY, 0);
        scores.put(CAT_AUTHENTICATION, 0);
        scores.put(CAT_URL, 0);
        scores.put(CAT_INFRASTRUCTURE, 0);
        scores.put(CAT_VISUAL, 0);
        scores.put(CAT_CONTENT, 0);
        scores.put(CAT_MACHINE_LEARNING, 0);

        counts.put(CAT_IDENTITY, 0);
        counts.put(CAT_AUTHENTICATION, 0);
        counts.put(CAT_URL, 0);
        counts.put(CAT_INFRASTRUCTURE, 0);
        counts.put(CAT_VISUAL, 0);
        counts.put(CAT_CONTENT, 0);
        counts.put(CAT_MACHINE_LEARNING, 0);
    }

    private String computeRiskLevel(int score) {
        if (score >= 75) {
            return "CRITICAL";
        } else if (score >= 50) {
            return "HIGH";
        } else if (score >= 25) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    /**
     * Single source of truth for evaluating all 6 forensic channels.
     * Evaluates whether analysis was executed and whether actual evidence/artifacts were found.
     * Non-null but empty URL or VISUAL lists do NOT count as evidence present.
     */
    public List<ChannelStatusDto> evaluateChannelStatuses(HeaderAnalysisDto headerAnalysis,
                                                          List<ReceivedHopDto> hops,
                                                          List<ExtractedUrlDto> urls,
                                                          List<VisualArtifactDto> visuals,
                                                          Optional<ContentAnalysisDto> contentOpt) {
        List<ChannelStatusDto> statuses = new ArrayList<>();

        // 1. HEADER: Verified from email headers
        boolean headerExecuted = headerAnalysis != null;
        boolean headerFound = headerAnalysis != null;
        int headerCount = headerFound ? 1 : 0;
        statuses.add(new ChannelStatusDto(
                "HEADER",
                headerFound ? "EVALUATED" : "UNAVAILABLE",
                headerFound,
                headerExecuted,
                headerFound,
                headerCount,
                headerFound ? "Header metadata verified" : "Header metadata unavailable"
        ));

        // 2. AUTH: SPF, DKIM, and DMARC verification
        boolean authExecuted = headerAnalysis != null;
        int authCount = 0;
        if (headerAnalysis != null) {
            if (headerAnalysis.getReportedSpf() != null || headerAnalysis.getActiveSpfResult() != null || headerAnalysis.getSpfResult() != null) {
                authCount++;
            }
            if (headerAnalysis.getReportedDkim() != null || headerAnalysis.getActiveDkimStatus() != null || headerAnalysis.getDkimResult() != null) {
                authCount++;
            }
            if (headerAnalysis.getReportedDmarc() != null || headerAnalysis.getActiveDmarcStatus() != null || headerAnalysis.getDmarcResult() != null) {
                authCount++;
            }
        }
        boolean authFound = authCount > 0;
        statuses.add(new ChannelStatusDto(
                "AUTH",
                authFound ? "EVALUATED" : "UNAVAILABLE",
                authFound,
                authExecuted,
                authFound,
                authCount,
                authFound ? authCount + " authentication checks verified" : "No authentication records found"
        ));

        // 3. ROUTE: Server transmission hops
        boolean routeExecuted = hops != null;
        int hopCount = (hops != null) ? hops.size() : 0;
        boolean routeFound = hopCount > 0;
        statuses.add(new ChannelStatusDto(
                "ROUTE",
                routeFound ? "EVALUATED" : "UNAVAILABLE",
                routeFound,
                routeExecuted,
                routeFound,
                hopCount,
                routeFound ? hopCount + " routing hops analyzed" : "No routing hops found"
        ));

        // 4. URL: Extracted links from plain/html body (empty list != evidence present)
        boolean urlExecuted = urls != null;
        int urlCount = (urls != null) ? urls.size() : 0;
        boolean urlFound = urlCount > 0;
        statuses.add(new ChannelStatusDto(
                "URL",
                urlFound ? "EVALUATED" : "UNAVAILABLE",
                urlFound,
                urlExecuted,
                urlFound,
                urlCount,
                urlFound ? urlCount + " extracted URLs analyzed" : "No URLs found in message"
        ));

        // 5. CONTENT: NLP / ML / Social engineering / Prompt injection
        boolean contentExecuted = contentOpt != null && contentOpt.isPresent();
        boolean contentFound = contentExecuted;
        int contentCount = contentFound ? 1 : 0;
        statuses.add(new ChannelStatusDto(
                "CONTENT",
                contentFound ? "EVALUATED" : "UNAVAILABLE",
                contentFound,
                contentExecuted,
                contentFound,
                contentCount,
                contentFound ? "Message body content analyzed" : "Content analysis unavailable"
        ));

        // 6. VISUAL: Images, attachments, and QR codes (empty list != evidence present)
        boolean visualExecuted = visuals != null;
        int visualCount = (visuals != null) ? visuals.size() : 0;
        boolean visualFound = visualCount > 0;
        statuses.add(new ChannelStatusDto(
                "VISUAL",
                visualFound ? "EVALUATED" : "UNAVAILABLE",
                visualFound,
                visualExecuted,
                visualFound,
                visualCount,
                visualFound ? visualCount + " visual artifacts analyzed" : "No visual artifacts found"
        ));

        return statuses;
    }

    /**
     * Extracts channel names for channels that have status EVALUATED (evidence present and evaluated).
     */
    public List<String> extractEvaluatedChannels(List<ChannelStatusDto> statuses) {
        List<String> list = new ArrayList<>();
        if (statuses != null) {
            for (ChannelStatusDto s : statuses) {
                if (s.isEvaluated()) {
                    list.add(s.getChannel());
                }
            }
        }
        return list;
    }

    /**
     * Calculates completeness points: exactly 10 points per successfully evaluated channel (up to 60).
     */
    public int calculateCompleteness(List<String> evaluatedChannels) {
        int count = (evaluatedChannels != null) ? evaluatedChannels.size() : 0;
        return Math.min(60, count * 10);
    }

    /**
     * Overload for backward compatibility.
     */
    public int calculateCompleteness(HeaderAnalysisDto headerAnalysis, List<ReceivedHopDto> hops,
                                     List<ExtractedUrlDto> urls, List<VisualArtifactDto> visuals,
                                     Optional<ContentAnalysisDto> contentOpt) {
        List<ChannelStatusDto> statuses = evaluateChannelStatuses(headerAnalysis, hops, urls, visuals, contentOpt);
        return calculateCompleteness(extractEvaluatedChannels(statuses));
    }

    /**
     * Overload for backward compatibility.
     */
    public List<String> determineEvaluatedChannels(HeaderAnalysisDto headerAnalysis, List<ReceivedHopDto> hops,
                                                   List<ExtractedUrlDto> urls, List<VisualArtifactDto> visuals,
                                                   Optional<ContentAnalysisDto> contentOpt) {
        return extractEvaluatedChannels(evaluateChannelStatuses(headerAnalysis, hops, urls, visuals, contentOpt));
    }

    // Multi-Category Support calculation (renamed from calculateCorroboration)
    private int calculateMultiCategorySupport(Map<String, Integer> categoryScores) {
        int triggeredCategories = 0;
        for (Map.Entry<String, Integer> entry : categoryScores.entrySet()) {
            if (entry.getValue() > 0) {
                triggeredCategories++;
            }
        }

        if (triggeredCategories == 0) {
            return 40;
        } else if (triggeredCategories >= 3) {
            return 40;
        } else if (triggeredCategories == 2) {
            return 25;
        } else {
            return 10;
        }
    }

    // Retained for backward compatibility
    private int calculateCorroboration(Map<String, Integer> categoryScores) {
        return calculateMultiCategorySupport(categoryScores);
    }

    private String computeConfidenceLevel(int score) {
        if (score >= 75) {
            return "HIGH";
        } else if (score >= 50) {
            return "MODERATE";
        } else {
            return "LOW";
        }
    }

    private String buildConfidenceExplanation(int confidenceScore, String confidenceLevel,
                                             int completeness, int multiCategorySupport,
                                             Map<String, Integer> categoryScores) {
        int channelsEvaluated = completeness / 10;
        List<String> activeCats = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : categoryScores.entrySet()) {
            if (entry.getValue() > 0) {
                activeCats.add(entry.getKey());
            }
        }

        if (activeCats.isEmpty()) {
            return "Evidence confidence is " + confidenceScore + "/100 (" + confidenceLevel + ") based on "
                    + channelsEvaluated + " forensic channels evaluated with complete multi-channel agreement on benign state.";
        }

        return "Evidence confidence is " + confidenceScore + "/100 (" + confidenceLevel + ") based on "
                + channelsEvaluated + " forensic channels evaluated (" + completeness + " pts) and multi-category support ("
                + multiCategorySupport + " pts) across " + activeCats.size() + " triggered categories (" + String.join(", ", activeCats) + ").";
    }

    private List<RiskCategoryScoreDto> buildCategoryScoreDtos(Map<String, Integer> scores, Map<String, Integer> counts) {
        List<RiskCategoryScoreDto> list = new ArrayList<>();
        Map<String, String> displayNames = getCategoryDisplayNames();

        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            String cat = entry.getKey();
            list.add(new RiskCategoryScoreDto(
                    cat,
                    displayNames.getOrDefault(cat, cat),
                    entry.getValue(),
                    counts.getOrDefault(cat, 0)
            ));
        }
        return list;
    }

    private Map<String, String> getCategoryDisplayNames() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(CAT_IDENTITY, "Identity & Spoofing");
        map.put(CAT_AUTHENTICATION, "Authentication Protocols");
        map.put(CAT_URL, "URL & Link Intelligence");
        map.put(CAT_INFRASTRUCTURE, "Infrastructure & Routing");
        map.put(CAT_VISUAL, "Visual & QR Forensics");
        map.put(CAT_CONTENT, "Content & Social Engineering");
        map.put(CAT_MACHINE_LEARNING, "Machine Learning Baseline");
        return map;
    }

    private List<RiskFactorDto> toFactorDtos(List<RiskFactor> factors) {
        List<RiskFactorDto> dtos = new ArrayList<>();
        for (RiskFactor f : factors) {
            dtos.add(new RiskFactorDto(
                    f.getId(),
                    f.getCategory(),
                    f.getFactorCode(),
                    f.getSeverity(),
                    f.getWeightPoints(),
                    f.getExplanation(),
                    f.getEvidenceReference(),
                    f.getCreatedAt()
            ));
        }
        return dtos;
    }

    private String formatCategoryScoresJson(Map<String, Integer> scores, Map<String, Integer> counts) {
        if (scores == null || scores.isEmpty()) return "[]";
        List<RiskCategoryScoreDto> list = buildCategoryScoreDtos(scores, counts);
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<RiskCategoryScoreDto> parseCategoryScoresJson(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<RiskCategoryScoreDto>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}

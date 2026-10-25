package com.emailforensics.backend.risk;

import com.emailforensics.backend.common.ResourceNotFoundException;
import com.emailforensics.backend.content.ContentAnalysisService;
import com.emailforensics.backend.dto.content.ContentAnalysisDto;
import com.emailforensics.backend.dto.headers.HeaderAnalysisDto;
import com.emailforensics.backend.dto.headers.ReceivedHopDto;
import com.emailforensics.backend.dto.infrastructure.IpArtifactDto;
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
import com.emailforensics.backend.headers.HeaderForensicService;
import com.emailforensics.backend.infrastructure.InfrastructureService;
import com.emailforensics.backend.urlintel.UrlIntelService;
import com.emailforensics.backend.visual.VisualAnalysisService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RiskScoringService {

    public static final String CAT_IDENTITY = "IDENTITY";
    public static final String CAT_AUTHENTICATION = "AUTHENTICATION";
    public static final String CAT_URL = "URL";
    public static final String CAT_INFRASTRUCTURE = "INFRASTRUCTURE";
    public static final String CAT_VISUAL = "VISUAL";
    public static final String CAT_CONTENT = "CONTENT";
    public static final String CAT_MACHINE_LEARNING = "MACHINE_LEARNING";

    public static final String ENGINE_VERSION = "v1.0.0";

    private static final Pattern OBJECT_PATTERN = Pattern.compile("\\{([^{}]+)\\}");
    private static final Pattern FIELD_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(?:\"((?:\\\\\"|[^\"])*)\"|([0-9]+))");

    private final RiskFactorRepository riskFactorRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final EmailEvidenceRepository evidenceRepository;
    private final AnalysisRunRepository analysisRunRepository;
    private final HeaderForensicService headerForensicService;
    private final UrlIntelService urlIntelService;
    private final InfrastructureService infrastructureService;
    private final VisualAnalysisService visualAnalysisService;
    private final ContentAnalysisService contentAnalysisService;

    public RiskScoringService(RiskFactorRepository riskFactorRepository,
                              RiskAssessmentRepository riskAssessmentRepository,
                              EmailEvidenceRepository evidenceRepository,
                              AnalysisRunRepository analysisRunRepository,
                              HeaderForensicService headerForensicService,
                              UrlIntelService urlIntelService,
                              InfrastructureService infrastructureService,
                              VisualAnalysisService visualAnalysisService,
                              ContentAnalysisService contentAnalysisService) {
        this.riskFactorRepository = riskFactorRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.evidenceRepository = evidenceRepository;
        this.analysisRunRepository = analysisRunRepository;
        this.headerForensicService = headerForensicService;
        this.urlIntelService = urlIntelService;
        this.infrastructureService = infrastructureService;
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
            headerAnalysis = headerForensicService.getHeaderAnalysis(emailId);
        } catch (Exception ignored) {
        }

        List<ReceivedHopDto> hops = Collections.emptyList();
        try {
            hops = headerForensicService.getReceivedHops(emailId);
        } catch (Exception ignored) {
        }

        List<ExtractedUrlDto> urls = Collections.emptyList();
        try {
            urls = urlIntelService.getExtractedUrls(emailId);
        } catch (Exception ignored) {
        }

        List<IpArtifactDto> ips = Collections.emptyList();
        try {
            ips = infrastructureService.getInfrastructure(emailId);
        } catch (Exception ignored) {
        }

        List<VisualArtifactDto> visuals = Collections.emptyList();
        try {
            visuals = visualAnalysisService.getVisualArtifacts(emailId);
        } catch (Exception ignored) {
        }

        Optional<ContentAnalysisDto> contentOpt = Optional.empty();
        try {
            contentOpt = contentAnalysisService.getContentAnalysis(emailId);
        } catch (Exception ignored) {
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

        int completeness = calculateCompleteness(headerAnalysis, hops, urls, visuals, contentOpt);
        int corroboration = calculateCorroboration(categoryScores);
        int confidenceScore = Math.min(100, Math.max(0, completeness + corroboration));
        String confidenceLevel = computeConfidenceLevel(confidenceScore);
        String confidenceExplanation = buildConfidenceExplanation(confidenceScore, confidenceLevel, completeness, corroboration, categoryScores);

        String categoryScoresJson = formatCategoryScoresJson(categoryScores, categoryCounts);

        riskFactorRepository.deleteByAnalysisRunId(run.getId());
        List<RiskFactor> savedFactors = riskFactorRepository.saveAll(rawFactors);

        RiskAssessment assessment = riskAssessmentRepository.findByAnalysisRunId(run.getId())
                .orElseGet(RiskAssessment::new);

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

        riskAssessmentRepository.save(assessment);

        run.setCurrentStage("RISK_EVALUATED");
        analysisRunRepository.save(run);

        List<RiskCategoryScoreDto> categoryScoreDtos = buildCategoryScoreDtos(categoryScores, categoryCounts);
        List<RiskFactorDto> factorDtos = toFactorDtos(savedFactors);

        return new RiskBreakdownDto(
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
    }

    @Transactional
    public RiskBreakdownDto getRiskBreakdown(UUID emailId) {
        EmailEvidence evidence = evidenceRepository.findById(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence not found: " + emailId));

        AnalysisRun run = analysisRunRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("No analysis run found for evidence: " + emailId));

        Optional<RiskAssessment> assessmentOpt = riskAssessmentRepository.findByAnalysisRunId(run.getId());
        if (assessmentOpt.isEmpty()) {
            return evaluateAndSaveRisk(emailId);
        }

        RiskAssessment assessment = assessmentOpt.get();
        List<RiskFactor> factors = riskFactorRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run.getId());
        List<RiskCategoryScoreDto> categoryScores = parseCategoryScoresJson(assessment.getCategoryScoresJson());
        List<RiskFactorDto> factorDtos = toFactorDtos(factors);

        return new RiskBreakdownDto(
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
    }

    private void collectIdentityFactors(HeaderAnalysisDto h, AnalysisRun run, List<RiskFactor> factors, Set<String> deduplicationKeys) {
        List<String> flags = h.getIdentityMismatchFlags();
        if (flags != null) {
            for (String flag : flags) {
                if ("FROM_REPLYTO_MISMATCH".equalsIgnoreCase(flag)) {
                    addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                            UUID.randomUUID(), run, CAT_IDENTITY, "IDENTITY_REPLY_TO_MISMATCH", "MEDIUM", 15,
                            "Reply-To address uses a different domain than the From header address.",
                            "From: " + h.getFromAddress() + " vs Reply-To: " + h.getReplyTo()
                    ), "REPLY_TO");
                } else if ("FROM_RETURNPATH_MISMATCH".equalsIgnoreCase(flag)) {
                    addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                            UUID.randomUUID(), run, CAT_IDENTITY, "IDENTITY_RETURN_PATH_MISMATCH", "LOW", 10,
                            "Envelope Return-Path domain differs from the message From header domain.",
                            "From: " + h.getFromAddress() + " vs Return-Path: " + h.getReturnPath()
                    ), "RETURN_PATH");
                } else if ("DISPLAY_NAME_EMAIL_MISMATCH".equalsIgnoreCase(flag) || "DISPLAY_NAME_SPOOFING".equalsIgnoreCase(flag)) {
                    addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                            UUID.randomUUID(), run, CAT_IDENTITY, "IDENTITY_DISPLAY_NAME_SPOOFING", "HIGH", 20,
                            "From display name contains an embedded email address or deceptive brand mismatch.",
                            "From: " + h.getFromAddress()
                    ), "DISPLAY_NAME");
                }
            }
        }
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
            String ocrText = visual.getOcrText();
            if (ocrText != null && !ocrText.isBlank()) {
                String lower = ocrText.toLowerCase(Locale.ROOT);
                if (lower.contains("password") || lower.contains("login") || lower.contains("verify account")
                        || lower.contains("wire transfer") || lower.contains("bank account") || lower.contains("urgent payment")) {
                    addFactorIfUnique(factors, deduplicationKeys, new RiskFactor(
                            UUID.randomUUID(), run, CAT_VISUAL, "VISUAL_OCR_SENSITIVE_TEXT", "MEDIUM", 15,
                            "OCR extracted image text containing credential or financial phishing lures.",
                            "Image: " + visual.getFilename()
                    ), "OCR_SENSITIVE");
                }
            }

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

    private int calculateCompleteness(HeaderAnalysisDto headerAnalysis, List<ReceivedHopDto> hops,
                                     List<ExtractedUrlDto> urls, List<VisualArtifactDto> visuals,
                                     Optional<ContentAnalysisDto> contentOpt) {
        int completeness = 0;
        if (headerAnalysis != null) {
            completeness += 10;
        }
        if (headerAnalysis != null && (headerAnalysis.getReportedSpf() != null || headerAnalysis.getReportedDkim() != null || headerAnalysis.getReportedDmarc() != null)) {
            completeness += 10;
        }
        if (hops != null && !hops.isEmpty()) {
            completeness += 10;
        }
        if (urls != null) {
            completeness += 10;
        }
        if (contentOpt.isPresent()) {
            completeness += 10;
        }
        if (visuals != null) {
            completeness += 10;
        }
        return Math.min(60, completeness);
    }

    private int calculateCorroboration(Map<String, Integer> categoryScores) {
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
                                             int completeness, int corroboration,
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
                + channelsEvaluated + " forensic channels evaluated (" + completeness + " pts) and cross-channel corroboration ("
                + corroboration + " pts) across " + activeCats.size() + " triggered categories (" + String.join(", ", activeCats) + ").";
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
        StringBuilder sb = new StringBuilder("[");
        Map<String, String> displayNames = getCategoryDisplayNames();
        int idx = 0;
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            if (idx > 0) {
                sb.append(",");
            }
            String cat = entry.getKey();
            sb.append("{")
                    .append("\"category\":\"").append(cat).append("\",")
                    .append("\"displayName\":\"").append(displayNames.getOrDefault(cat, cat)).append("\",")
                    .append("\"score\":").append(entry.getValue()).append(",")
                    .append("\"factorCount\":").append(counts.getOrDefault(cat, 0))
                    .append("}");
            idx++;
        }
        sb.append("]");
        return sb.toString();
    }

    private List<RiskCategoryScoreDto> parseCategoryScoresJson(String json) {
        List<RiskCategoryScoreDto> list = new ArrayList<>();
        if (json == null || json.trim().isEmpty() || json.equals("[]")) {
            return list;
        }

        Matcher om = OBJECT_PATTERN.matcher(json);
        while (om.find()) {
            String obj = om.group(1);
            Map<String, String> map = new HashMap<>();
            Matcher fm = FIELD_PATTERN.matcher(obj);
            while (fm.find()) {
                String key = fm.group(1);
                String val = fm.group(2) != null ? fm.group(2) : fm.group(3);
                map.put(key, val);
            }
            list.add(new RiskCategoryScoreDto(
                    map.get("category"),
                    map.get("displayName"),
                    parseIntSafe(map.get("score")),
                    parseIntSafe(map.get("factorCount"))
            ));
        }
        return list;
    }

    private int parseIntSafe(String val) {
        if (val == null) return 0;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

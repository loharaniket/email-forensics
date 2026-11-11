package com.emailforensics.backend.report;

import com.emailforensics.backend.aiassessment.AiGroundedAssessment;
import com.emailforensics.backend.aiassessment.AiGroundedAssessmentRepository;
import com.emailforensics.backend.analysis.AnalysisResult;
import com.emailforensics.backend.analysis.AnalysisResultRepository;
import com.emailforensics.backend.common.ResourceNotFoundException;
import com.emailforensics.backend.correlation.HistoricalMatch;
import com.emailforensics.backend.correlation.HistoricalMatchRepository;
import com.emailforensics.backend.dto.email.AttachmentMetaDto;
import com.emailforensics.backend.dto.email.ParsedEmailDto;
import com.emailforensics.backend.dto.report.ForensicReportDto;
import com.emailforensics.backend.email.EmailParserService;
import com.emailforensics.backend.evidence.AnalysisRun;
import com.emailforensics.backend.evidence.AnalysisRunRepository;
import com.emailforensics.backend.evidence.EmailEvidence;
import com.emailforensics.backend.evidence.EmailEvidenceRepository;
import com.emailforensics.backend.evidence.EvidenceStorageService;
import com.emailforensics.backend.headers.ReceivedHop;
import com.emailforensics.backend.headers.ReceivedHopRepository;
import com.emailforensics.backend.infrastructure.IpArtifact;
import com.emailforensics.backend.infrastructure.IpArtifactRepository;
import com.emailforensics.backend.risk.RiskFactor;
import com.emailforensics.backend.risk.RiskFactorRepository;
import com.emailforensics.backend.urlintel.ExtractedUrl;
import com.emailforensics.backend.urlintel.ExtractedUrlRepository;
import com.emailforensics.backend.visual.QrArtifact;
import com.emailforensics.backend.visual.QrArtifactRepository;
import com.emailforensics.backend.visual.VisualArtifact;
import com.emailforensics.backend.visual.VisualArtifactRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ReportService {

    private final EmailEvidenceRepository evidenceRepository;
    private final AnalysisRunRepository runRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final ReceivedHopRepository hopRepository;
    private final ExtractedUrlRepository urlRepository;
    private final IpArtifactRepository ipRepository;
    private final VisualArtifactRepository visualRepository;
    private final QrArtifactRepository qrRepository;
    private final RiskFactorRepository riskFactorRepository;
    private final HistoricalMatchRepository matchRepository;
    private final AiGroundedAssessmentRepository aiRepository;
    private final EvidenceStorageService storageService;
    private final EmailParserService emailParserService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReportService(EmailEvidenceRepository evidenceRepository,
                                 AnalysisRunRepository runRepository,
                                 AnalysisResultRepository analysisResultRepository,
                                 ReceivedHopRepository hopRepository,
                                 ExtractedUrlRepository urlRepository,
                                 IpArtifactRepository ipRepository,
                                 VisualArtifactRepository visualRepository,
                                 QrArtifactRepository qrRepository,
                                 RiskFactorRepository riskFactorRepository,
                                 HistoricalMatchRepository matchRepository,
                                 AiGroundedAssessmentRepository aiRepository,
                                 EvidenceStorageService storageService,
                                 EmailParserService emailParserService) {
        this.evidenceRepository = evidenceRepository;
        this.runRepository = runRepository;
        this.analysisResultRepository = analysisResultRepository;
        this.hopRepository = hopRepository;
        this.urlRepository = urlRepository;
        this.ipRepository = ipRepository;
        this.visualRepository = visualRepository;
        this.qrRepository = qrRepository;
        this.riskFactorRepository = riskFactorRepository;
        this.matchRepository = matchRepository;
        this.aiRepository = aiRepository;
        this.storageService = storageService;
        this.emailParserService = emailParserService;
    }

    @Transactional(readOnly = true)
    public ForensicReportDto generateReport(UUID emailId) {
        EmailEvidence evidence = evidenceRepository.findById(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence not found with ID: " + emailId));

        AnalysisRun run = runRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis run not found for evidence: " + emailId));

        return buildForensicReport(evidence, run, "Automated Forensic Engine");
    }

    public ForensicReportDto generateReport(UUID emailId, Object unused) {
        return generateReport(emailId);
    }

    public String exportReportJson(ForensicReportDto report) {
        if (report == null) return "{}";
        try {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("reportId", report.getReportId() != null ? report.getReportId() : "");
            root.put("generatedAt", report.getGeneratedAt() != null ? report.getGeneratedAt().toString() : "");
            root.put("reportVersion", report.getReportVersion() != null ? report.getReportVersion() : "");
            root.put("generatedBy", report.getGeneratedBy() != null ? report.getGeneratedBy() : "");
            root.put("classificationLevel", report.getClassificationLevel() != null ? report.getClassificationLevel() : "");

            if (report.getCaseContext() != null) {
                ForensicReportDto.CaseContextDto c = report.getCaseContext();
                Map<String, Object> caseContext = new LinkedHashMap<>();
                caseContext.put("caseId", c.getCaseId() != null ? c.getCaseId().toString() : "");
                caseContext.put("caseNumber", c.getCaseNumber() != null ? c.getCaseNumber() : "");
                caseContext.put("title", c.getTitle() != null ? c.getTitle() : "");
                caseContext.put("status", c.getStatus() != null ? c.getStatus() : "");
                root.put("caseContext", caseContext);
            }

            if (report.getEvidenceIntegrity() != null) {
                ForensicReportDto.EvidenceIntegrityDto i = report.getEvidenceIntegrity();
                Map<String, Object> evidenceIntegrity = new LinkedHashMap<>();
                evidenceIntegrity.put("emailId", i.getEmailId() != null ? i.getEmailId().toString() : "");
                evidenceIntegrity.put("originalFilename", i.getOriginalFilename() != null ? i.getOriginalFilename() : "");
                evidenceIntegrity.put("sha256Hash", i.getSha256Hash() != null ? i.getSha256Hash() : "");
                evidenceIntegrity.put("recalculatedHash", i.getRecalculatedHash() != null ? i.getRecalculatedHash() : "");
                evidenceIntegrity.put("fileSizeBytes", i.getFileSizeBytes() != null ? i.getFileSizeBytes() : 0);
                evidenceIntegrity.put("integrityVerified", i.isIntegrityVerified());
                evidenceIntegrity.put("integrityStatus", i.getIntegrityStatus() != null ? i.getIntegrityStatus() : "");
                evidenceIntegrity.put("integrityCheckTimestamp", i.getIntegrityCheckTimestamp() != null ? i.getIntegrityCheckTimestamp().toString() : "");
                root.put("evidenceIntegrity", evidenceIntegrity);
            }

            if (report.getMessageSummary() != null) {
                ForensicReportDto.MessageSummaryDto m = report.getMessageSummary();
                Map<String, Object> messageSummary = new LinkedHashMap<>();
                messageSummary.put("subject", m.getSubject() != null ? m.getSubject() : "");
                messageSummary.put("fromAddress", m.getFromAddress() != null ? m.getFromAddress() : "");
                messageSummary.put("toAddress", m.getToAddress() != null ? m.getToAddress() : "");
                messageSummary.put("sentDate", m.getSentDate() != null ? m.getSentDate().toString() : "");
                messageSummary.put("attachmentsCount", m.getAttachmentsCount());
                root.put("messageSummary", messageSummary);
            }

            if (report.getRiskSummary() != null) {
                ForensicReportDto.DeterministicRiskSummaryDto r = report.getRiskSummary();
                Map<String, Object> riskSummary = new LinkedHashMap<>();
                riskSummary.put("deterministicRiskScore", r.getDeterministicRiskScore());
                riskSummary.put("riskLevel", r.getRiskLevel() != null ? r.getRiskLevel() : "");
                riskSummary.put("evidenceConfidenceScore", r.getEvidenceConfidenceScore());
                riskSummary.put("confidenceLevel", r.getConfidenceLevel() != null ? r.getConfidenceLevel() : "");
                riskSummary.put("contributingFactorsCount", r.getContributingFactorsCount());
                root.put("riskSummary", riskSummary);
            }

            if (report.getAiAssessment() != null) {
                ForensicReportDto.AiAssessmentSummaryDto a = report.getAiAssessment();
                Map<String, Object> aiAssessment = new LinkedHashMap<>();
                aiAssessment.put("assessmentPresent", a.isAssessmentPresent());
                aiAssessment.put("status", a.getStatus() != null ? a.getStatus() : "");
                aiAssessment.put("provider", a.getProvider() != null ? a.getProvider() : "");
                aiAssessment.put("modelName", a.getModelName() != null ? a.getModelName() : "");
                aiAssessment.put("threatClassification", a.getThreatClassification() != null ? a.getThreatClassification() : "");
                aiAssessment.put("confidenceAssessment", a.getConfidenceAssessment() != null ? a.getConfidenceAssessment() : "");
                aiAssessment.put("tokensUsed", a.getTokensUsed() != null ? a.getTokensUsed() : 0);
                aiAssessment.put("generationTimeMs", a.getGenerationTimeMs() != null ? a.getGenerationTimeMs() : 0);
                aiAssessment.put("summaryExplanation", a.getSummaryExplanation() != null ? a.getSummaryExplanation() : "");
                aiAssessment.put("promptInjectionDetected", a.isPromptInjectionDetected());
                root.put("aiAssessment", aiAssessment);
            }

            if (report.getAnalystSignoff() != null) {
                ForensicReportDto.AnalystSignoffDto s = report.getAnalystSignoff();
                Map<String, Object> analystSignoff = new LinkedHashMap<>();
                analystSignoff.put("certifyingAnalyst", s.getCertifyingAnalyst() != null ? s.getCertifyingAnalyst() : "");
                analystSignoff.put("signoffTimestamp", s.getSignoffTimestamp() != null ? s.getSignoffTimestamp().toString() : "");
                analystSignoff.put("certificationStatement", s.getCertificationStatement() != null ? s.getCertificationStatement() : "");
                root.put("analystSignoff", analystSignoff);
            } else {
                root.put("analystSignoff", null);
            }

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("Failed to export forensic report as JSON", e);
        }
    }

    private ForensicReportDto buildForensicReport(EmailEvidence evidence, AnalysisRun run, String certifyingUser) {
        ForensicReportDto report = new ForensicReportDto();

        report.setReportId("FR-" + evidence.getId().toString().substring(0, 8).toUpperCase());
        report.setGeneratedAt(Instant.now());
        report.setReportVersion("v1.0.0");
        report.setGeneratedBy(certifyingUser);
        report.setClassificationLevel("CONFIDENTIAL - FORENSIC REPORT");

        report.setCaseContext(null);

        ForensicReportDto.EvidenceIntegrityDto integrity = new ForensicReportDto.EvidenceIntegrityDto();
        integrity.setEmailId(evidence.getId());
        integrity.setOriginalFilename(evidence.getOriginalFilename());
        integrity.setSha256Hash(evidence.getSha256Hash());
        integrity.setFileSizeBytes(evidence.getFileSizeBytes());
        integrity.setStorageKey(evidence.getStorageKey());
        integrity.setUploadedAt(evidence.getUploadedAt());
        integrity.setUploaderUsername("system");

        String recalculatedHash = null;
        String integrityStatus = "NOT_VERIFIED";
        boolean verified = false;
        try {
            recalculatedHash = storageService.computeFileHash(evidence.getStorageKey());
            if (recalculatedHash != null && evidence.getSha256Hash() != null) {
                if (recalculatedHash.equalsIgnoreCase(evidence.getSha256Hash())) {
                    verified = true;
                    integrityStatus = "VERIFIED";
                } else {
                    verified = false;
                    integrityStatus = "MISMATCH";
                }
            }
        } catch (Exception ignored) {
            integrityStatus = "NOT_VERIFIED";
        }
        integrity.setIntegrityVerified(verified);
        integrity.setRecalculatedHash(recalculatedHash);
        integrity.setIntegrityStatus(integrityStatus);
        integrity.setIntegrityCheckTimestamp(Instant.now());
        report.setEvidenceIntegrity(integrity);

        ParsedEmailDto parsedEmail = null;
        try (InputStream in = storageService.loadAsResource(evidence.getStorageKey()).getInputStream()) {
            parsedEmail = emailParserService.parse(in);
        } catch (Exception ignored) {
        }

        ForensicReportDto.MessageSummaryDto msg = new ForensicReportDto.MessageSummaryDto();
        if (parsedEmail != null) {
            msg.setSubject(parsedEmail.getSubject());
            msg.setFromAddress(parsedEmail.getFrom() != null ? parsedEmail.getFrom() : "Unknown");
            msg.setToAddress(parsedEmail.getTo() != null && !parsedEmail.getTo().isEmpty() ? String.join(", ", parsedEmail.getTo()) : "Undisclosed");
            msg.setCcAddress(parsedEmail.getCc() != null && !parsedEmail.getCc().isEmpty() ? String.join(", ", parsedEmail.getCc()) : null);
            msg.setReplyTo(parsedEmail.getReplyTo() != null && !parsedEmail.getReplyTo().isEmpty() ? String.join(", ", parsedEmail.getReplyTo()) : null);
            msg.setSentDate(parsedEmail.getSentDate());
            msg.setMessageId(parsedEmail.getMessageId());
            msg.setAttachmentsCount(parsedEmail.getAttachments() != null ? parsedEmail.getAttachments().size() : 0);

            String bodySnippet = parsedEmail.getPlainTextBody();
            String bodyType = "PLAIN_TEXT";
            if (bodySnippet == null || bodySnippet.isBlank()) {
                bodySnippet = parsedEmail.getHtmlBody();
                bodyType = "HTML";
            }
            if (bodySnippet != null) {
                bodySnippet = bodySnippet.replaceAll("[\\r\\n]+", " ").trim();
                if (bodySnippet.length() > 600) {
                    bodySnippet = bodySnippet.substring(0, 600) + "... [truncated for report preview]";
                }
            }
            msg.setBodySnippet(bodySnippet);
            msg.setBodyType(bodyType);
        } else {
            msg.setSubject(evidence.getSubject());
            msg.setMessageId(evidence.getMessageId());
            msg.setSentDate(evidence.getSentDate());
        }
        report.setMessageSummary(msg);

        ForensicReportDto.ExecutionStatusDto exec = new ForensicReportDto.ExecutionStatusDto();
        exec.setAnalysisRunId(run.getId());
        exec.setPipelineVersion(run.getPipelineVersion());
        exec.setCurrentStage(run.getCurrentStage());
        exec.setStatus(run.getStatus() != null ? run.getStatus().name() : "COMPLETED");
        exec.setStartedAt(run.getStartedAt());
        exec.setCompletedAt(run.getCompletedAt());
        exec.setErrorCode(run.getErrorCode());
        exec.setErrorMessage(run.getSafeErrorMessage());
        report.setExecutionStatus(exec);

        ForensicReportDto.HeaderForensicsSummaryDto headers = new ForensicReportDto.HeaderForensicsSummaryDto();
        Optional<AnalysisResult> resOpt = analysisResultRepository.findByAnalysisRunId(run.getId());
        if (resOpt.isPresent() && resOpt.get().hasHeaderAnalysis()) {
            AnalysisResult h = resOpt.get();
            headers.setFromAddress(h.getFromAddress());
            headers.setFromDomain(h.getFromDomain());
            headers.setReturnPath(h.getReturnPath());
            headers.setReplyTo(h.getReplyTo());
            headers.setSpfResult(h.getReportedSpf());
            headers.setDkimResult(h.getReportedDkim());
            headers.setDmarcResult(h.getReportedDmarc());
            headers.setArcResult(h.getReportedArc());
            headers.setIdentityMismatchFlags(parseStringList(h.getIdentityMismatchFlags()));
            if (h.getCurrentDnsObservation() != null && !h.getCurrentDnsObservation().isBlank()) {
                headers.setActiveSpfResult(extractSimpleJson(h.getCurrentDnsObservation(), "activeSpf", "status"));
                headers.setActiveDkimResult(extractSimpleJson(h.getCurrentDnsObservation(), "activeDkim", "status"));
                headers.setActiveDmarcResult(extractSimpleJson(h.getCurrentDnsObservation(), "activeDmarc", "status"));
            }
            headers.setActiveVerificationSummary(h.getIndependentVerification());
        }

        List<ReceivedHop> hopsList = hopRepository.findByAnalysisRunIdOrderByHopOrderAsc(run.getId());
        headers.setHopsCount(hopsList.size());
        if (!hopsList.isEmpty()) {
            headers.setFirstOriginatingIp(hopsList.get(0).getHopIp());
            headers.setFinalDestinationHost(hopsList.get(hopsList.size() - 1).getByHost());
            List<ForensicReportDto.HopSummaryDto> hopSummaries = new ArrayList<>();
            for (ReceivedHop hop : hopsList) {
                hopSummaries.add(new ForensicReportDto.HopSummaryDto(
                        hop.getHopOrder(),
                        hop.getFromHost(),
                        hop.getByHost(),
                        hop.getHopIp(),
                        hop.getDelaySeconds()
                ));
            }
            headers.setHopsSummary(hopSummaries);
        }
        report.setHeaderForensics(headers);

        ForensicReportDto.NetworkAndUrlSummaryDto net = new ForensicReportDto.NetworkAndUrlSummaryDto();
        List<ExtractedUrl> urlList = urlRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run.getId());
        net.setTotalUrlsCount(urlList.size());
        int suspCount = 0;
        int malCount = 0;
        List<ForensicReportDto.ReportUrlDto> rUrls = new ArrayList<>();
        for (ExtractedUrl u : urlList) {
            ForensicReportDto.ReportUrlDto rUrl = new ForensicReportDto.ReportUrlDto();
            rUrl.setOriginalUrl(u.getNormalizedUrl() != null ? u.getNormalizedUrl() : u.getRawUrl());
            rUrl.setDomain(u.getDomain());
            boolean suspicious = Boolean.TRUE.equals(u.getAnchorMismatch()) ||
                    Boolean.TRUE.equals(u.getIsIpUrl()) ||
                    Boolean.TRUE.equals(u.getIsShortened()) ||
                    Boolean.TRUE.equals(u.getLookalike());
            rUrl.setSuspicious(suspicious);
            rUrl.setIpLiteral(Boolean.TRUE.equals(u.getIsIpUrl()));
            rUrl.setShortener(Boolean.TRUE.equals(u.getIsShortened()));
            rUrl.setAnchorMismatch(Boolean.TRUE.equals(u.getAnchorMismatch()));
            rUrl.setPunycode(Boolean.TRUE.equals(u.getLookalike()));
            rUrl.setThreatCategory(suspicious ? "SUSPICIOUS_HEURISTIC" : "BENIGN");
            if (suspicious) {
                suspCount++;
                if (Boolean.TRUE.equals(u.getIsIpUrl()) || Boolean.TRUE.equals(u.getAnchorMismatch())) {
                    malCount++;
                }
            }
            rUrls.add(rUrl);
        }
        net.setSuspiciousUrlsCount(suspCount);
        net.setMaliciousUrlsCount(malCount);
        net.setUrls(rUrls);

        List<IpArtifact> ipList = ipRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run.getId());
        net.setTotalIpsCount(ipList.size());
        int bogonCount = 0;
        List<ForensicReportDto.ReportIpDto> rIps = new ArrayList<>();
        for (IpArtifact ip : ipList) {
            ForensicReportDto.ReportIpDto rIp = new ForensicReportDto.ReportIpDto();
            rIp.setIpAddress(ip.getIpAddress());
            rIp.setSourceCategory(ip.getSourceContext() != null ? ip.getSourceContext() : "NETWORK");
            rIp.setPrivateBogon(Boolean.TRUE.equals(ip.getIsPrivate()));
            if (Boolean.TRUE.equals(ip.getIsPrivate())) bogonCount++;
            rIps.add(rIp);
        }
        net.setPrivateBogonIpsCount(bogonCount);
        net.setIps(rIps);
        report.setNetworkAndUrls(net);

        ForensicReportDto.VisualAndArtifactSummaryDto vis = new ForensicReportDto.VisualAndArtifactSummaryDto();
        if (parsedEmail != null && parsedEmail.getAttachments() != null) {
            vis.setTotalAttachmentsCount(parsedEmail.getAttachments().size());
            List<ForensicReportDto.ReportAttachmentDto> rAtts = new ArrayList<>();
            for (AttachmentMetaDto att : parsedEmail.getAttachments()) {
                rAtts.add(new ForensicReportDto.ReportAttachmentDto(
                        att.getFilename(),
                        att.getMimeType(),
                        att.getSizeBytes(),
                        att.getSha256Hash()
                ));
            }
            vis.setAttachments(rAtts);
        }

        List<VisualArtifact> visList = visualRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run.getId());
        vis.setVisualArtifactsCount(visList.size());
        List<String> qrPayloads = new ArrayList<>();
        for (VisualArtifact v : visList) {
            if (v.getQrArtifacts() != null) {
                for (QrArtifact q : v.getQrArtifacts()) {
                    if (q.getDecodedPayload() != null) {
                        qrPayloads.add(q.getDecodedPayload());
                    }
                }
            }
        }
        vis.setQrDecodedPayloads(qrPayloads);
        report.setVisualsAndArtifacts(vis);

        ForensicReportDto.ContentAnalysisSummaryDto cont = new ForensicReportDto.ContentAnalysisSummaryDto();
        if (resOpt.isPresent() && resOpt.get().hasContentAnalysis()) {
            AnalysisResult ca = resOpt.get();
            cont.setUrgency(ca.isUrgency());
            cont.setCredentialRequest(ca.isCredentialRequest());
            cont.setPaymentInstruction(ca.isPaymentInstruction());
            cont.setImpersonation(ca.isImpersonation());
            cont.setSecrecy(ca.isSecrecy());
            cont.setPromptInjectionDetected(ca.isPromptInjectionDetected());
            cont.setTribuoPredictedLabel(ca.getPredictedLabel());
            cont.setTribuoProbability(ca.getProbability());
        }
        report.setContentAnalysis(cont);

        ForensicReportDto.DeterministicRiskSummaryDto risk = new ForensicReportDto.DeterministicRiskSummaryDto();
        if (resOpt.isPresent() && resOpt.get().hasRiskAssessment()) {
            AnalysisResult ra = resOpt.get();
            risk.setDeterministicRiskScore(ra.getDeterministicRiskScore());
            risk.setRiskLevel(ra.getRiskLevel() != null ? ra.getRiskLevel() : "LOW");
            risk.setEvidenceConfidenceScore(ra.getEvidenceConfidenceScore());
            risk.setConfidenceLevel(ra.getConfidenceLevel() != null ? ra.getConfidenceLevel() : "LOW");
        } else {
            risk.setDeterministicRiskScore(0);
            risk.setRiskLevel("LOW");
            risk.setEvidenceConfidenceScore(0);
            risk.setConfidenceLevel("LOW");
        }

        List<RiskFactor> factors = riskFactorRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run.getId());
        risk.setContributingFactorsCount(factors.size());
        List<ForensicReportDto.ReportRiskFactorDto> rFactors = new ArrayList<>();
        for (RiskFactor rf : factors) {
            rFactors.add(new ForensicReportDto.ReportRiskFactorDto(
                    rf.getFactorCode(),
                    rf.getSeverity() != null ? rf.getSeverity() : "LOW",
                    rf.getWeightPoints(),
                    rf.getCategory() != null ? rf.getCategory() : "GENERAL",
                    rf.getExplanation()
            ));
        }
        risk.setContributingFactors(rFactors);
        report.setRiskSummary(risk);

        ForensicReportDto.CorrelationAndCampaignSummaryDto corr = new ForensicReportDto.CorrelationAndCampaignSummaryDto();
        List<HistoricalMatch> matches = matchRepository.findBySourceRunId(run.getId());
        corr.setHistoricalMatchesCount(matches.size());
        List<ForensicReportDto.ReportCorrelationMatchDto> rMatches = new ArrayList<>();
        for (HistoricalMatch m : matches) {
            String targetEvidence = m.getMatchedEmail() != null
                    ? (m.getMatchedEmail().getOriginalFilename() != null ? m.getMatchedEmail().getOriginalFilename() : m.getMatchedEmail().getId().toString().substring(0, 8))
                    : "PREVIOUS_EMAIL";
            rMatches.add(new ForensicReportDto.ReportCorrelationMatchDto(
                    m.getIndicatorType() != null ? m.getIndicatorType() : "INDICATOR",
                    m.getNormalizedIndicatorValue(),
                    m.getMatchStrength() != null ? m.getMatchStrength() : "MEDIUM",
                    targetEvidence
            ));
        }
        corr.setMatchedIndicators(rMatches);

        report.setCorrelationsAndCampaigns(corr);

        ForensicReportDto.AiAssessmentSummaryDto ai = new ForensicReportDto.AiAssessmentSummaryDto();
        Optional<AiGroundedAssessment> aiOpt = aiRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(evidence.getId());
        if (aiOpt.isPresent()) {
            AiGroundedAssessment a = aiOpt.get();
            ai.setAssessmentPresent(true);
            ai.setStatus(a.getStatus());
            ai.setThreatClassification(a.getThreatClassification());
            ai.setConfidenceAssessment(a.getConfidenceAssessment());
            ai.setSummaryExplanation(a.getSummaryExplanation());
            ai.setKeyIndicators(parseStringList(a.getKeyIndicatorsJson()));
            ai.setEvidenceReferences(parseStringList(a.getEvidenceReferencesJson()));
            ai.setUnsupportedReferences(parseStringList(a.getUnsupportedReferencesJson()));
            ai.setLimitationsAndCaveats(a.getLimitationsAndCaveats());
            ai.setPromptInjectionDetected(a.isUntrustedInjectionsDetected());
            ai.setProvider(a.getProvider());
            ai.setModelName(a.getModelName());
            ai.setTokensUsed(a.getTokensUsed());
            ai.setGenerationTimeMs(a.getGenerationTimeMs());
        } else {
            ai.setAssessmentPresent(false);
        }
        report.setAiAssessment(ai);

        report.setAnalystSignoff(null);

        return report;
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String extractSimpleJson(String json, String parentKey, String childKey) {
        if (json == null || parentKey == null || childKey == null) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode parent = root.get(parentKey);
            if (parent == null || parent.isNull()) return null;
            JsonNode child = parent.get(childKey);
            if (child == null || child.isNull()) return null;
            return child.asText();
        } catch (Exception e) {
            return null;
        }
    }
}

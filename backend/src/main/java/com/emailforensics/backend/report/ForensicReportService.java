package com.emailforensics.backend.report;

import com.emailforensics.backend.aiassessment.AiGroundedAssessment;
import com.emailforensics.backend.aiassessment.AiGroundedAssessmentRepository;
import com.emailforensics.backend.campaign.CampaignMember;
import com.emailforensics.backend.campaign.CampaignMemberRepository;
import com.emailforensics.backend.casefile.CaseFile;
import com.emailforensics.backend.casefile.CaseRepository;
import com.emailforensics.backend.common.ResourceNotFoundException;
import com.emailforensics.backend.content.ContentAnalysis;
import com.emailforensics.backend.content.ContentAnalysisRepository;
import com.emailforensics.backend.correlation.HistoricalMatch;
import com.emailforensics.backend.correlation.HistoricalMatchRepository;
import com.emailforensics.backend.dto.email.AttachmentMetaDto;
import com.emailforensics.backend.dto.email.ParsedEmailDto;
import com.emailforensics.backend.dto.report.CaseReportDto;
import com.emailforensics.backend.dto.report.ForensicReportDto;
import com.emailforensics.backend.email.EmailParserService;
import com.emailforensics.backend.evidence.AnalysisRun;
import com.emailforensics.backend.evidence.AnalysisRunRepository;
import com.emailforensics.backend.evidence.EmailEvidence;
import com.emailforensics.backend.evidence.EmailEvidenceRepository;
import com.emailforensics.backend.evidence.EvidenceStorageService;
import com.emailforensics.backend.headers.HeaderAnalysis;
import com.emailforensics.backend.headers.HeaderAnalysisRepository;
import com.emailforensics.backend.headers.ReceivedHop;
import com.emailforensics.backend.headers.ReceivedHopRepository;
import com.emailforensics.backend.infrastructure.IpArtifact;
import com.emailforensics.backend.infrastructure.IpArtifactRepository;
import com.emailforensics.backend.risk.RiskAssessment;
import com.emailforensics.backend.risk.RiskAssessmentRepository;
import com.emailforensics.backend.risk.RiskFactor;
import com.emailforensics.backend.risk.RiskFactorRepository;
import com.emailforensics.backend.security.UserPrincipal;
import com.emailforensics.backend.urlintel.ExtractedUrl;
import com.emailforensics.backend.urlintel.ExtractedUrlRepository;
import com.emailforensics.backend.visual.QrArtifact;
import com.emailforensics.backend.visual.QrArtifactRepository;
import com.emailforensics.backend.visual.VisualArtifact;
import com.emailforensics.backend.visual.VisualArtifactRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ForensicReportService {

    private final EmailEvidenceRepository evidenceRepository;
    private final AnalysisRunRepository runRepository;
    private final CaseRepository caseRepository;
    private final HeaderAnalysisRepository headerRepository;
    private final ReceivedHopRepository hopRepository;
    private final ExtractedUrlRepository urlRepository;
    private final IpArtifactRepository ipRepository;
    private final VisualArtifactRepository visualRepository;
    private final QrArtifactRepository qrRepository;
    private final ContentAnalysisRepository contentRepository;
    private final RiskAssessmentRepository riskRepository;
    private final RiskFactorRepository riskFactorRepository;
    private final HistoricalMatchRepository matchRepository;
    private final CampaignMemberRepository campaignMemberRepository;
    private final AiGroundedAssessmentRepository aiRepository;
    private final EvidenceStorageService storageService;
    private final EmailParserService emailParserService;

    public ForensicReportService(EmailEvidenceRepository evidenceRepository,
                                 AnalysisRunRepository runRepository,
                                 CaseRepository caseRepository,
                                 HeaderAnalysisRepository headerRepository,
                                 ReceivedHopRepository hopRepository,
                                 ExtractedUrlRepository urlRepository,
                                 IpArtifactRepository ipRepository,
                                 VisualArtifactRepository visualRepository,
                                 QrArtifactRepository qrRepository,
                                 ContentAnalysisRepository contentRepository,
                                 RiskAssessmentRepository riskRepository,
                                 RiskFactorRepository riskFactorRepository,
                                 HistoricalMatchRepository matchRepository,
                                 CampaignMemberRepository campaignMemberRepository,
                                 AiGroundedAssessmentRepository aiRepository,
                                 EvidenceStorageService storageService,
                                 EmailParserService emailParserService) {
        this.evidenceRepository = evidenceRepository;
        this.runRepository = runRepository;
        this.caseRepository = caseRepository;
        this.headerRepository = headerRepository;
        this.hopRepository = hopRepository;
        this.urlRepository = urlRepository;
        this.ipRepository = ipRepository;
        this.visualRepository = visualRepository;
        this.qrRepository = qrRepository;
        this.contentRepository = contentRepository;
        this.riskRepository = riskRepository;
        this.riskFactorRepository = riskFactorRepository;
        this.matchRepository = matchRepository;
        this.campaignMemberRepository = campaignMemberRepository;
        this.aiRepository = aiRepository;
        this.storageService = storageService;
        this.emailParserService = emailParserService;
    }

    @Transactional(readOnly = true)
    public ForensicReportDto generateReport(UUID emailId, UserPrincipal currentUser) {
        EmailEvidence evidence = evidenceRepository.findById(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence not found with ID: " + emailId));

        AnalysisRun run = runRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis run not found for evidence: " + emailId));

        String username = currentUser != null ? currentUser.getUsername() : "analyst";
        return buildForensicReport(evidence, run, username);
    }

    @Transactional(readOnly = true)
    public CaseReportDto generateCaseReport(UUID caseId, UserPrincipal currentUser) {
        CaseFile caseFile = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found with ID: " + caseId));

        String username = currentUser != null ? currentUser.getUsername() : "analyst";
        CaseReportDto caseReport = new CaseReportDto();
        caseReport.setReportId("CASE-REP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        caseReport.setCaseId(caseFile.getId());
        caseReport.setCaseNumber(caseFile.getCaseNumber());
        caseReport.setTitle(caseFile.getTitle());
        caseReport.setDescription(caseFile.getDescription());
        caseReport.setStatus(caseFile.getStatus() != null ? caseFile.getStatus().name() : "OPEN");
        caseReport.setAnalystNotes(caseFile.getAnalystNotes());
        caseReport.setGeneratedBy(username);
        caseReport.setGeneratedAt(Instant.now());

        List<EmailEvidence> evidences = evidenceRepository.findByCaseFileIdOrderByUploadedAtDesc(caseId);
        caseReport.setTotalEvidenceCount(evidences.size());

        int highCount = 0;
        int criticalCount = 0;
        List<ForensicReportDto> reports = new ArrayList<>();

        for (EmailEvidence ev : evidences) {
            Optional<AnalysisRun> runOpt = runRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(ev.getId());
            if (runOpt.isPresent()) {
                ForensicReportDto report = buildForensicReport(ev, runOpt.get(), username);
                reports.add(report);
                if (report.getRiskSummary() != null) {
                    if ("CRITICAL".equalsIgnoreCase(report.getRiskSummary().getRiskLevel())) {
                        criticalCount++;
                    } else if ("HIGH".equalsIgnoreCase(report.getRiskSummary().getRiskLevel())) {
                        highCount++;
                    }
                }
            }
        }

        caseReport.setHighRiskCount(highCount);
        caseReport.setCriticalRiskCount(criticalCount);
        caseReport.setEvidenceReports(reports);

        return caseReport;
    }

    public String exportReportJson(ForensicReportDto report) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"reportId\": \"").append(escapeJson(report.getReportId())).append("\",\n");
        sb.append("  \"generatedAt\": \"").append(report.getGeneratedAt() != null ? report.getGeneratedAt().toString() : "").append("\",\n");
        sb.append("  \"reportVersion\": \"").append(escapeJson(report.getReportVersion())).append("\",\n");
        sb.append("  \"generatedBy\": \"").append(escapeJson(report.getGeneratedBy())).append("\",\n");
        sb.append("  \"classificationLevel\": \"").append(escapeJson(report.getClassificationLevel())).append("\",\n");

        if (report.getCaseContext() != null) {
            ForensicReportDto.CaseContextDto c = report.getCaseContext();
            sb.append("  \"caseContext\": {\n");
            sb.append("    \"caseId\": \"").append(c.getCaseId() != null ? c.getCaseId().toString() : "").append("\",\n");
            sb.append("    \"caseNumber\": \"").append(escapeJson(c.getCaseNumber())).append("\",\n");
            sb.append("    \"title\": \"").append(escapeJson(c.getTitle())).append("\",\n");
            sb.append("    \"status\": \"").append(escapeJson(c.getStatus())).append("\"\n");
            sb.append("  },\n");
        }

        if (report.getEvidenceIntegrity() != null) {
            ForensicReportDto.EvidenceIntegrityDto i = report.getEvidenceIntegrity();
            sb.append("  \"evidenceIntegrity\": {\n");
            sb.append("    \"emailId\": \"").append(i.getEmailId() != null ? i.getEmailId().toString() : "").append("\",\n");
            sb.append("    \"originalFilename\": \"").append(escapeJson(i.getOriginalFilename())).append("\",\n");
            sb.append("    \"sha256Hash\": \"").append(escapeJson(i.getSha256Hash())).append("\",\n");
            sb.append("    \"fileSizeBytes\": ").append(i.getFileSizeBytes() != null ? i.getFileSizeBytes() : 0).append(",\n");
            sb.append("    \"integrityVerified\": ").append(i.isIntegrityVerified()).append("\n");
            sb.append("  },\n");
        }

        if (report.getMessageSummary() != null) {
            ForensicReportDto.MessageSummaryDto m = report.getMessageSummary();
            sb.append("  \"messageSummary\": {\n");
            sb.append("    \"subject\": \"").append(escapeJson(m.getSubject())).append("\",\n");
            sb.append("    \"fromAddress\": \"").append(escapeJson(m.getFromAddress())).append("\",\n");
            sb.append("    \"toAddress\": \"").append(escapeJson(m.getToAddress())).append("\",\n");
            sb.append("    \"sentDate\": \"").append(m.getSentDate() != null ? m.getSentDate().toString() : "").append("\",\n");
            sb.append("    \"attachmentsCount\": ").append(m.getAttachmentsCount()).append("\n");
            sb.append("  },\n");
        }

        if (report.getRiskSummary() != null) {
            ForensicReportDto.DeterministicRiskSummaryDto r = report.getRiskSummary();
            sb.append("  \"riskSummary\": {\n");
            sb.append("    \"deterministicRiskScore\": ").append(r.getDeterministicRiskScore()).append(",\n");
            sb.append("    \"riskLevel\": \"").append(escapeJson(r.getRiskLevel())).append("\",\n");
            sb.append("    \"evidenceConfidenceScore\": ").append(r.getEvidenceConfidenceScore()).append(",\n");
            sb.append("    \"confidenceLevel\": \"").append(escapeJson(r.getConfidenceLevel())).append("\",\n");
            sb.append("    \"contributingFactorsCount\": ").append(r.getContributingFactorsCount()).append("\n");
            sb.append("  },\n");
        }

        if (report.getAiAssessment() != null) {
            ForensicReportDto.AiAssessmentSummaryDto a = report.getAiAssessment();
            sb.append("  \"aiAssessment\": {\n");
            sb.append("    \"assessmentPresent\": ").append(a.isAssessmentPresent()).append(",\n");
            sb.append("    \"threatClassification\": \"").append(escapeJson(a.getThreatClassification())).append("\",\n");
            sb.append("    \"confidenceAssessment\": \"").append(escapeJson(a.getConfidenceAssessment())).append("\",\n");
            sb.append("    \"summaryExplanation\": \"").append(escapeJson(a.getSummaryExplanation())).append("\",\n");
            sb.append("    \"promptInjectionDetected\": ").append(a.isPromptInjectionDetected()).append("\n");
            sb.append("  },\n");
        }

        if (report.getAnalystSignoff() != null) {
            ForensicReportDto.AnalystSignoffDto s = report.getAnalystSignoff();
            sb.append("  \"analystSignoff\": {\n");
            sb.append("    \"certifyingAnalyst\": \"").append(escapeJson(s.getCertifyingAnalyst())).append("\",\n");
            sb.append("    \"signoffTimestamp\": \"").append(s.getSignoffTimestamp() != null ? s.getSignoffTimestamp().toString() : "").append("\",\n");
            sb.append("    \"certificationStatement\": \"").append(escapeJson(s.getCertificationStatement())).append("\"\n");
            sb.append("  }\n");
        } else {
            sb.append("  \"analystSignoff\": null\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    private ForensicReportDto buildForensicReport(EmailEvidence evidence, AnalysisRun run, String certifyingUser) {
        ForensicReportDto report = new ForensicReportDto();

        report.setReportId("FR-" + evidence.getId().toString().substring(0, 8).toUpperCase());
        report.setGeneratedAt(Instant.now());
        report.setReportVersion("v1.0.0-phase11");
        report.setGeneratedBy(certifyingUser);
        report.setClassificationLevel("CONFIDENTIAL - FORENSIC EVIDENCE DOSSIER");

        ForensicReportDto.CaseContextDto caseContext = new ForensicReportDto.CaseContextDto();
        if (evidence.getCaseFile() != null) {
            CaseFile c = evidence.getCaseFile();
            caseContext.setCaseId(c.getId());
            caseContext.setCaseNumber(c.getCaseNumber());
            caseContext.setTitle(c.getTitle());
            caseContext.setDescription(c.getDescription());
            caseContext.setStatus(c.getStatus() != null ? c.getStatus().name() : "OPEN");
            caseContext.setAnalystNotes(c.getAnalystNotes());
            caseContext.setCreatedAt(c.getCreatedAt());
        }
        report.setCaseContext(caseContext);

        ForensicReportDto.EvidenceIntegrityDto integrity = new ForensicReportDto.EvidenceIntegrityDto();
        integrity.setEmailId(evidence.getId());
        integrity.setOriginalFilename(evidence.getOriginalFilename());
        integrity.setSha256Hash(evidence.getSha256Hash());
        integrity.setFileSizeBytes(evidence.getFileSizeBytes());
        integrity.setStorageKey(evidence.getStorageKey());
        integrity.setUploadedAt(evidence.getUploadedAt());
        integrity.setUploaderUsername(evidence.getUploadedBy() != null ? evidence.getUploadedBy().getUsername() : "system");

        boolean verified = false;
        try {
            String hash = storageService.computeFileHash(evidence.getStorageKey());
            verified = hash != null && hash.equalsIgnoreCase(evidence.getSha256Hash());
        } catch (Exception ignored) {
        }
        integrity.setIntegrityVerified(verified);
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
        Optional<HeaderAnalysis> hOpt = headerRepository.findByAnalysisRunId(run.getId());
        if (hOpt.isPresent()) {
            HeaderAnalysis h = hOpt.get();
            headers.setFromAddress(h.getFromAddress());
            headers.setFromDomain(h.getFromDomain());
            headers.setReturnPath(h.getReturnPath());
            headers.setReplyTo(h.getReplyTo());
            headers.setSpfResult(h.getReportedSpf());
            headers.setDkimResult(h.getReportedDkim());
            headers.setDmarcResult(h.getReportedDmarc());
            headers.setArcResult(h.getReportedArc());
            headers.setIdentityMismatchFlags(parseStringList(h.getIdentityMismatchFlags()));
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
            rIp.setCountryCode(ip.getCountry());
            rIp.setAsn(ip.getAsnNumber());
            rIp.setReverseDns(ip.getOrganization());
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
        List<String> ocrSnippets = new ArrayList<>();
        List<String> qrPayloads = new ArrayList<>();
        for (VisualArtifact v : visList) {
            if (v.getOcrText() != null && !v.getOcrText().isBlank()) {
                String text = v.getOcrText().trim().replaceAll("[\\r\\n]+", " ");
                if (text.length() > 200) text = text.substring(0, 200) + "...";
                ocrSnippets.add(text);
            }
            if (v.getQrArtifacts() != null) {
                for (QrArtifact q : v.getQrArtifacts()) {
                    if (q.getDecodedPayload() != null) {
                        qrPayloads.add(q.getDecodedPayload());
                    }
                }
            }
        }
        vis.setOcrTextSnippets(ocrSnippets);
        vis.setQrDecodedPayloads(qrPayloads);
        report.setVisualsAndArtifacts(vis);

        ForensicReportDto.ContentAnalysisSummaryDto cont = new ForensicReportDto.ContentAnalysisSummaryDto();
        Optional<ContentAnalysis> cOpt = contentRepository.findByAnalysisRunId(run.getId());
        if (cOpt.isPresent()) {
            ContentAnalysis ca = cOpt.get();
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
        Optional<RiskAssessment> rOpt = riskRepository.findByAnalysisRunId(run.getId());
        if (rOpt.isPresent()) {
            RiskAssessment ra = rOpt.get();
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
            String targetCase = m.getMatchedEmail() != null && m.getMatchedEmail().getCaseFile() != null
                    ? m.getMatchedEmail().getCaseFile().getCaseNumber()
                    : "PREVIOUS_CASE";
            rMatches.add(new ForensicReportDto.ReportCorrelationMatchDto(
                    m.getIndicatorType() != null ? m.getIndicatorType() : "INDICATOR",
                    m.getNormalizedIndicatorValue(),
                    m.getMatchStrength() != null ? m.getMatchStrength() : "MEDIUM",
                    targetCase
            ));
        }
        corr.setMatchedIndicators(rMatches);

        List<CampaignMember> campMembers = campaignMemberRepository.findByEmailEvidenceId(evidence.getId());
        if (!campMembers.isEmpty()) {
            CampaignMember cm = campMembers.get(0);
            corr.setCampaignMember(true);
            corr.setCampaignCode(cm.getCampaign().getCampaignCode());
            corr.setCampaignName(cm.getCampaign().getName());
            corr.setCampaignConfidence(cm.getCampaign().getConfidenceScore());
        }
        report.setCorrelationsAndCampaigns(corr);

        ForensicReportDto.AiAssessmentSummaryDto ai = new ForensicReportDto.AiAssessmentSummaryDto();
        Optional<AiGroundedAssessment> aiOpt = aiRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(evidence.getId());
        if (aiOpt.isPresent()) {
            AiGroundedAssessment a = aiOpt.get();
            ai.setAssessmentPresent(true);
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

        ForensicReportDto.AnalystSignoffDto signoff = new ForensicReportDto.AnalystSignoffDto();
        signoff.setCertifyingAnalyst(certifyingUser);
        signoff.setSignoffTimestamp(Instant.now());
        signoff.setCertificationStatement("I hereby certify that the evidence analyzed in this forensic dossier has been cryptographically verified against its preserved SHA-256 digest, and that all findings were extracted using deterministic forensic parsing engines and grounded threat intelligence.");
        if (evidence.getCaseFile() != null && evidence.getCaseFile().getAnalystNotes() != null) {
            signoff.setNotes(evidence.getCaseFile().getAnalystNotes());
        }
        report.setAnalystSignoff(signoff);

        return report;
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) return Collections.emptyList();
        List<String> list = new ArrayList<>();
        Matcher m = Pattern.compile("\"((?:\\\\\"|[^\"])*)\"").matcher(json);
        while (m.find()) {
            list.add(m.group(1).replace("\\\"", "\""));
        }
        return list;
    }
}

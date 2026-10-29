package com.emailforensics.backend.report;

import com.emailforensics.backend.aiassessment.AiGroundedAssessment;
import com.emailforensics.backend.aiassessment.AiGroundedAssessmentRepository;
import com.emailforensics.backend.campaign.Campaign;
import com.emailforensics.backend.campaign.CampaignMember;
import com.emailforensics.backend.campaign.CampaignMemberRepository;
import com.emailforensics.backend.casefile.CaseFile;
import com.emailforensics.backend.casefile.CaseRepository;
import com.emailforensics.backend.casefile.CaseStatus;
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
import com.emailforensics.backend.evidence.AnalysisStatus;
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
import com.emailforensics.backend.user.Role;
import com.emailforensics.backend.user.User;
import com.emailforensics.backend.visual.QrArtifactRepository;
import com.emailforensics.backend.visual.VisualArtifact;
import com.emailforensics.backend.visual.VisualArtifactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForensicReportServiceTest {

    @Mock
    private EmailEvidenceRepository evidenceRepository;

    @Mock
    private AnalysisRunRepository runRepository;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private HeaderAnalysisRepository headerRepository;

    @Mock
    private ReceivedHopRepository hopRepository;

    @Mock
    private ExtractedUrlRepository urlRepository;

    @Mock
    private IpArtifactRepository ipRepository;

    @Mock
    private VisualArtifactRepository visualRepository;

    @Mock
    private QrArtifactRepository qrRepository;

    @Mock
    private ContentAnalysisRepository contentRepository;

    @Mock
    private RiskAssessmentRepository riskRepository;

    @Mock
    private RiskFactorRepository riskFactorRepository;

    @Mock
    private HistoricalMatchRepository matchRepository;

    @Mock
    private CampaignMemberRepository campaignMemberRepository;

    @Mock
    private AiGroundedAssessmentRepository aiRepository;

    @Mock
    private EvidenceStorageService storageService;

    @Mock
    private EmailParserService emailParserService;

    private ForensicReportService service;

    private User analyst;
    private CaseFile caseFile;
    private EmailEvidence evidence;
    private AnalysisRun run;

    @BeforeEach
    void setUp() {
        service = new ForensicReportService(
                evidenceRepository,
                runRepository,
                caseRepository,
                headerRepository,
                hopRepository,
                urlRepository,
                ipRepository,
                visualRepository,
                qrRepository,
                contentRepository,
                riskRepository,
                riskFactorRepository,
                matchRepository,
                campaignMemberRepository,
                aiRepository,
                storageService,
                emailParserService
        );

        analyst = new User(UUID.randomUUID(), "analyst", "hash", "analyst@forensics.local", Role.ANALYST, true, Instant.now());
        caseFile = new CaseFile(UUID.randomUUID(), "CASE-2026-TEST", "Phishing Test Case", "Description", CaseStatus.OPEN, "Notes", analyst, Instant.now(), Instant.now());
        evidence = new EmailEvidence(UUID.randomUUID(), caseFile, "suspicious.eml", "storage/key.eml", 2048L, "hash123", analyst);
        run = new AnalysisRun(UUID.randomUUID(), evidence, "v1.0.0", AnalysisStatus.COMPLETED, "COMPLETED");
    }

    @Test
    void generateReport_populatesAllSectionsSuccessfully() throws Exception {
        UUID emailId = evidence.getId();
        when(evidenceRepository.findById(emailId)).thenReturn(Optional.of(evidence));
        when(runRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)).thenReturn(Optional.of(run));
        when(storageService.computeFileHash("storage/key.eml")).thenReturn("hash123");
        when(storageService.loadAsResource("storage/key.eml")).thenReturn(new ByteArrayResource("Subject: Test".getBytes()));

        ParsedEmailDto parsedEmail = new ParsedEmailDto();
        parsedEmail.setSubject("Suspicious Wire Transfer");
        parsedEmail.setFrom("ceo@spoofed.com");
        parsedEmail.setTo(Collections.singletonList("finance@company.com"));
        parsedEmail.setPlainTextBody("Please transfer funds urgently.");
        AttachmentMetaDto att = new AttachmentMetaDto("invoice.pdf", "application/pdf", 1024L, "pdfhash", false, null, false);
        parsedEmail.setAttachments(Collections.singletonList(att));
        when(emailParserService.parse(any(InputStream.class))).thenReturn(parsedEmail);

        HeaderAnalysis ha = new HeaderAnalysis();
        ha.setFromAddress("ceo@spoofed.com");
        ha.setReturnPath("attacker@bad.org");
        ha.setReportedSpf("FAIL");
        ha.setReportedDkim("FAIL");
        ha.setReportedDmarc("FAIL");
        when(headerRepository.findByAnalysisRunId(run.getId())).thenReturn(Optional.of(ha));

        ReceivedHop hop = new ReceivedHop(UUID.randomUUID(), run, 1, "bad.org", "mx.company.com", "ESMTPS", "198.51.100.1", Instant.now(), 2L, "raw", null);
        when(hopRepository.findByAnalysisRunIdOrderByHopOrderAsc(run.getId())).thenReturn(Collections.singletonList(hop));

        ExtractedUrl url = new ExtractedUrl();
        url.setRawUrl("http://198.51.100.1/login");
        url.setNormalizedUrl("http://198.51.100.1/login");
        url.setDomain("198.51.100.1");
        url.setIsIpUrl(true);
        when(urlRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run.getId())).thenReturn(Collections.singletonList(url));

        IpArtifact ip = new IpArtifact(UUID.randomUUID(), run, "198.51.100.1", "HEADER_HOP", false, "EXTRACTED", Instant.now());
        when(ipRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run.getId())).thenReturn(Collections.singletonList(ip));

        VisualArtifact vis = new VisualArtifact(UUID.randomUUID(), run, "image.png", "image/png", 500L, "imghash", null);
        when(visualRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run.getId())).thenReturn(Collections.singletonList(vis));

        ContentAnalysis ca = new ContentAnalysis(UUID.randomUUID(), run, "v1.0.0", 0.95, "PHISHING", "SUCCESS", true, true, true, false, false, true, "[]", "[]");
        when(contentRepository.findByAnalysisRunId(run.getId())).thenReturn(Optional.of(ca));

        RiskAssessment ra = new RiskAssessment(UUID.randomUUID(), run, 85, "CRITICAL", 90, "HIGH", "High Confidence", "{}", 85, "v1.0.0");
        when(riskRepository.findByAnalysisRunId(run.getId())).thenReturn(Optional.of(ra));

        RiskFactor rf = new RiskFactor(UUID.randomUUID(), run, "AUTHENTICATION", "SPF_FAIL", "CRITICAL", 30, "SPF Failed", "HEADER:SPF");
        when(riskFactorRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run.getId())).thenReturn(Collections.singletonList(rf));

        HistoricalMatch match = new HistoricalMatch(UUID.randomUUID(), run, evidence, "SENDER_DOMAIN", "bad.org", "EXACT", "Domain match");
        when(matchRepository.findBySourceRunId(run.getId())).thenReturn(Collections.singletonList(match));

        Campaign campaign = new Campaign(UUID.randomUUID(), "CAMP-001", "CEO Spoofing", 85, "DOMAIN_CLUSTER", "Active Phish", Instant.now(), Instant.now());
        CampaignMember member = new CampaignMember(UUID.randomUUID(), campaign, evidence, "Domain match", 85);
        when(campaignMemberRepository.findByEmailEvidenceId(emailId)).thenReturn(Collections.singletonList(member));

        AiGroundedAssessment ai = new AiGroundedAssessment(
                UUID.randomUUID(), evidence, run, "openai", "gpt-4o", "v1.0.0", "LIVE_AI",
                "MALICIOUS_PHISHING", "HIGH", "Adversarial email", "[\"SPF Failure\"]", "[\"HEADER:From\"]", "[]", "None", false, "summary", null, 120, 450L
        );
        when(aiRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)).thenReturn(Optional.of(ai));

        UserPrincipal principal = new UserPrincipal(analyst);
        ForensicReportDto report = service.generateReport(emailId, principal);

        assertNotNull(report);
        assertEquals("CASE-2026-TEST", report.getCaseContext().getCaseNumber());
        assertTrue(report.getEvidenceIntegrity().isIntegrityVerified());
        assertEquals("hash123", report.getEvidenceIntegrity().getSha256Hash());
        assertEquals("hash123", report.getEvidenceIntegrity().getRecalculatedHash());
        assertEquals("VERIFIED", report.getEvidenceIntegrity().getIntegrityStatus());
        assertEquals("v1.0.0", report.getReportVersion());
        assertEquals("Suspicious Wire Transfer", report.getMessageSummary().getSubject());
        assertEquals(1, report.getHeaderForensics().getHopsCount());
        assertEquals(1, report.getNetworkAndUrls().getTotalUrlsCount());
        assertEquals(1, report.getVisualsAndArtifacts().getVisualArtifactsCount());
        assertTrue(report.getContentAnalysis().isPromptInjectionDetected());
        assertEquals(85, report.getRiskSummary().getDeterministicRiskScore());
        assertEquals("CRITICAL", report.getRiskSummary().getRiskLevel());
        assertTrue(report.getCorrelationsAndCampaigns().isCampaignMember());
        assertEquals("CAMP-001", report.getCorrelationsAndCampaigns().getCampaignCode());
        assertTrue(report.getAiAssessment().isAssessmentPresent());
        assertEquals("LIVE_AI", report.getAiAssessment().getStatus());
        assertEquals("MALICIOUS_PHISHING", report.getAiAssessment().getThreatClassification());
        assertNotNull(report.getAnalystSignoff());
    }

    @Test
    void generateReport_detectsIntegrityMismatch() throws Exception {
        UUID emailId = evidence.getId();
        when(evidenceRepository.findById(emailId)).thenReturn(Optional.of(evidence));
        when(runRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)).thenReturn(Optional.of(run));
        when(storageService.computeFileHash("storage/key.eml")).thenReturn("tamperedHash999");
        when(storageService.loadAsResource("storage/key.eml")).thenReturn(new ByteArrayResource("Subject: Clean".getBytes()));

        ForensicReportDto report = service.generateReport(emailId, null);

        assertNotNull(report);
        org.junit.jupiter.api.Assertions.assertFalse(report.getEvidenceIntegrity().isIntegrityVerified());
        assertEquals("hash123", report.getEvidenceIntegrity().getSha256Hash());
        assertEquals("tamperedHash999", report.getEvidenceIntegrity().getRecalculatedHash());
        assertEquals("MISMATCH", report.getEvidenceIntegrity().getIntegrityStatus());
    }

    @Test
    void exportReportJson_producesValidPrettyJson() throws Exception {
        UUID emailId = evidence.getId();
        when(evidenceRepository.findById(emailId)).thenReturn(Optional.of(evidence));
        when(runRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)).thenReturn(Optional.of(run));
        when(storageService.loadAsResource("storage/key.eml")).thenReturn(new ByteArrayResource("Subject: Clean".getBytes()));

        ForensicReportDto report = service.generateReport(emailId, null);
        String json = service.exportReportJson(report);

        assertNotNull(json);
        assertTrue(json.contains("\"reportId\""));
        assertTrue(json.contains("\"caseContext\""));
        assertTrue(json.contains("\"evidenceIntegrity\""));
        assertTrue(json.contains("\"recalculatedHash\""));
        assertTrue(json.contains("\"integrityStatus\""));
        assertTrue(json.contains("\"riskSummary\""));
    }

    @Test
    void generateCaseReport_aggregatesEvidenceReports() throws Exception {
        UUID caseId = caseFile.getId();
        when(caseRepository.findById(caseId)).thenReturn(Optional.of(caseFile));
        when(evidenceRepository.findByCaseFileIdOrderByUploadedAtDesc(caseId)).thenReturn(Collections.singletonList(evidence));
        when(runRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(evidence.getId())).thenReturn(Optional.of(run));
        when(storageService.loadAsResource(anyString())).thenReturn(new ByteArrayResource("Subject: Clean".getBytes()));

        RiskAssessment ra = new RiskAssessment(UUID.randomUUID(), run, 80, "HIGH", 85, "HIGH", "High Confidence", "{}", 80, "v1.0.0");
        when(riskRepository.findByAnalysisRunId(run.getId())).thenReturn(Optional.of(ra));

        CaseReportDto caseReport = service.generateCaseReport(caseId, null);

        assertNotNull(caseReport);
        assertEquals("CASE-2026-TEST", caseReport.getCaseNumber());
        assertEquals(1, caseReport.getTotalEvidenceCount());
        assertEquals(1, caseReport.getHighRiskCount());
        assertEquals(0, caseReport.getCriticalRiskCount());
        assertEquals(1, caseReport.getEvidenceReports().size());
    }
}

package com.emailforensics.backend.risk;

import com.emailforensics.backend.content.ContentAnalysisService;
import com.emailforensics.backend.dto.content.ContentAnalysisDto;
import com.emailforensics.backend.dto.headers.HeaderAnalysisDto;
import com.emailforensics.backend.dto.headers.ReceivedHopDto;
import com.emailforensics.backend.dto.infrastructure.IpArtifactDto;
import com.emailforensics.backend.dto.risk.RiskBreakdownDto;
import com.emailforensics.backend.dto.risk.RiskCategoryScoreDto;
import com.emailforensics.backend.dto.risk.RiskFactorDto;
import com.emailforensics.backend.dto.urlintel.ExtractedUrlDto;
import com.emailforensics.backend.dto.visual.VisualArtifactDto;
import com.emailforensics.backend.evidence.AnalysisRun;
import com.emailforensics.backend.evidence.AnalysisRunRepository;
import com.emailforensics.backend.evidence.AnalysisStatus;
import com.emailforensics.backend.evidence.EmailEvidence;
import com.emailforensics.backend.evidence.EmailEvidenceRepository;
import com.emailforensics.backend.headers.HeaderForensicService;
import com.emailforensics.backend.infrastructure.InfrastructureService;
import com.emailforensics.backend.urlintel.UrlIntelService;
import com.emailforensics.backend.visual.VisualAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

class RiskScoringServiceTest {

    private RiskScoringService service;
    private RiskFactorRepository riskFactorRepo;
    private RiskAssessmentRepository riskAssessmentRepo;
    private EmailEvidenceRepository evidenceRepo;
    private AnalysisRunRepository analysisRunRepo;
    private HeaderForensicService headerForensicService;
    private UrlIntelService urlIntelService;
    private InfrastructureService infrastructureService;
    private VisualAnalysisService visualAnalysisService;
    private ContentAnalysisService contentAnalysisService;

    private UUID emailId;
    private UUID runId;
    private EmailEvidence evidence;
    private AnalysisRun run;

    @BeforeEach
    void setUp() {
        riskFactorRepo = Mockito.mock(RiskFactorRepository.class);
        riskAssessmentRepo = Mockito.mock(RiskAssessmentRepository.class);
        evidenceRepo = Mockito.mock(EmailEvidenceRepository.class);
        analysisRunRepo = Mockito.mock(AnalysisRunRepository.class);
        headerForensicService = Mockito.mock(HeaderForensicService.class);
        urlIntelService = Mockito.mock(UrlIntelService.class);
        infrastructureService = Mockito.mock(InfrastructureService.class);
        visualAnalysisService = Mockito.mock(VisualAnalysisService.class);
        contentAnalysisService = Mockito.mock(ContentAnalysisService.class);

        service = new RiskScoringService(
                riskFactorRepo,
                riskAssessmentRepo,
                evidenceRepo,
                analysisRunRepo,
                headerForensicService,
                urlIntelService,
                infrastructureService,
                visualAnalysisService,
                contentAnalysisService
        );

        emailId = UUID.randomUUID();
        runId = UUID.randomUUID();

        evidence = new EmailEvidence();
        evidence.setId(emailId);
        evidence.setStorageKey("vault/test.eml");

        run = new AnalysisRun();
        run.setId(runId);
        run.setEmailEvidence(evidence);
        run.setStatus(AnalysisStatus.RUNNING);

        when(evidenceRepo.findById(emailId)).thenReturn(Optional.of(evidence));
        when(analysisRunRepo.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)).thenReturn(Optional.of(run));
        when(riskFactorRepo.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(riskAssessmentRepo.findByAnalysisRunId(runId)).thenReturn(Optional.empty());
    }

    @Test
    void testCleanBenignEmailNoFindings() {
        HeaderAnalysisDto headers = new HeaderAnalysisDto();
        headers.setReportedSpf("pass");
        headers.setReportedDkim("pass");
        headers.setReportedDmarc("pass");
        headers.setIdentityMismatchFlags(Collections.emptyList());
        when(headerForensicService.getHeaderAnalysis(emailId)).thenReturn(headers);

        ReceivedHopDto hop = new ReceivedHopDto();
        hop.setHopOrder(1);
        hop.setDelaySeconds(5L);
        when(headerForensicService.getReceivedHops(emailId)).thenReturn(List.of(hop));

        ExtractedUrlDto url = new ExtractedUrlDto();
        url.setDomain("example.com");
        url.setNormalizedUrl("https://example.com/info");
        url.setIsIpUrl(false);
        url.setIsShortened(false);
        url.setLookalike(false);
        url.setAnchorMismatch(false);
        url.setUnusualPort(false);
        url.setSubdomainExcess(false);
        url.setDnsStatus("NOERROR");
        when(urlIntelService.getExtractedUrls(emailId)).thenReturn(List.of(url));

        ContentAnalysisDto content = new ContentAnalysisDto();
        content.setPredictedLabel("BENIGN");
        content.setProbability(0.05);
        content.setUrgency(false);
        content.setCredentialRequest(false);
        content.setPaymentInstruction(false);
        content.setImpersonation(false);
        content.setSecrecy(false);
        content.setPromptInjectionDetected(false);
        when(contentAnalysisService.getContentAnalysis(emailId)).thenReturn(Optional.of(content));

        RiskBreakdownDto result = service.evaluateAndSaveRisk(emailId);

        assertNotNull(result);
        assertEquals(0, result.getDeterministicRiskScore());
        assertEquals("LOW", result.getRiskLevel());
        assertEquals(0, result.getTotalUncappedScore());
        assertTrue(result.getFactors().isEmpty());
        assertTrue(result.getEvidenceConfidenceScore() >= 80);
        assertEquals("HIGH", result.getConfidenceLevel());
    }

    @Test
    void testIsolatedFinding() {
        HeaderAnalysisDto headers = new HeaderAnalysisDto();
        headers.setFromAddress("user@company.com");
        headers.setReplyTo("attacker@external.org");
        headers.setIdentityMismatchFlags(List.of("FROM_REPLYTO_MISMATCH"));
        headers.setReportedSpf("pass");
        when(headerForensicService.getHeaderAnalysis(emailId)).thenReturn(headers);

        when(headerForensicService.getReceivedHops(emailId)).thenReturn(Collections.emptyList());
        when(urlIntelService.getExtractedUrls(emailId)).thenReturn(Collections.emptyList());
        when(contentAnalysisService.getContentAnalysis(emailId)).thenReturn(Optional.empty());

        RiskBreakdownDto result = service.evaluateAndSaveRisk(emailId);

        assertNotNull(result);
        assertEquals(15, result.getDeterministicRiskScore());
        assertEquals("LOW", result.getRiskLevel());
        assertEquals(1, result.getFactors().size());
        assertEquals("IDENTITY_REPLY_TO_MISMATCH", result.getFactors().get(0).getFactorCode());
        assertEquals("IDENTITY", result.getFactors().get(0).getCategory());
    }

    @Test
    void testRepeatedIndicatorsDeduplication() {
        HeaderAnalysisDto headers = new HeaderAnalysisDto();
        headers.setIdentityMismatchFlags(Collections.emptyList());
        when(headerForensicService.getHeaderAnalysis(emailId)).thenReturn(headers);

        List<ExtractedUrlDto> urls = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ExtractedUrlDto u = new ExtractedUrlDto();
            u.setDomain("evil-paypal.com");
            u.setNormalizedUrl("http://evil-paypal.com/login?id=" + i);
            u.setLookalike(true);
            u.setIsShortened(true);
            urls.add(u);
        }
        when(urlIntelService.getExtractedUrls(emailId)).thenReturn(urls);

        when(headerForensicService.getReceivedHops(emailId)).thenReturn(Collections.emptyList());
        when(contentAnalysisService.getContentAnalysis(emailId)).thenReturn(Optional.empty());

        RiskBreakdownDto result = service.evaluateAndSaveRisk(emailId);

        assertNotNull(result);
        assertEquals(2, result.getFactors().size());
        assertEquals(40, result.getDeterministicRiskScore());
    }

    @Test
    void testMultipleCategoriesTriggered() {
        HeaderAnalysisDto headers = new HeaderAnalysisDto();
        headers.setReportedDmarc("fail");
        when(headerForensicService.getHeaderAnalysis(emailId)).thenReturn(headers);

        ExtractedUrlDto url = new ExtractedUrlDto();
        url.setDomain("login-bank.com");
        url.setLookalike(true);
        when(urlIntelService.getExtractedUrls(emailId)).thenReturn(List.of(url));

        ContentAnalysisDto content = new ContentAnalysisDto();
        content.setCredentialRequest(true);
        when(contentAnalysisService.getContentAnalysis(emailId)).thenReturn(Optional.of(content));

        RiskBreakdownDto result = service.evaluateAndSaveRisk(emailId);

        assertNotNull(result);
        assertEquals(75, result.getDeterministicRiskScore());
        assertEquals("CRITICAL", result.getRiskLevel());
        assertEquals(3, result.getFactors().size());

        boolean hasAuth = false;
        boolean hasUrl = false;
        boolean hasContent = false;
        for (RiskFactorDto f : result.getFactors()) {
            if ("AUTHENTICATION".equals(f.getCategory())) hasAuth = true;
            if ("URL".equals(f.getCategory())) hasUrl = true;
            if ("CONTENT".equals(f.getCategory())) hasContent = true;
        }
        assertTrue(hasAuth);
        assertTrue(hasUrl);
        assertTrue(hasContent);
    }

    @Test
    void testScoreBoundaryCapping() {
        HeaderAnalysisDto headers = new HeaderAnalysisDto();
        headers.setReportedDmarc("fail");
        headers.setReportedSpf("fail");
        headers.setReportedDkim("fail");
        headers.setIdentityMismatchFlags(List.of("FROM_REPLYTO_MISMATCH", "DISPLAY_NAME_EMAIL_MISMATCH"));
        when(headerForensicService.getHeaderAnalysis(emailId)).thenReturn(headers);

        ExtractedUrlDto url = new ExtractedUrlDto();
        url.setDomain("phish.com");
        url.setIsIpUrl(true);
        url.setLookalike(true);
        url.setAnchorMismatch(true);
        url.setDnsStatus("NXDOMAIN");
        when(urlIntelService.getExtractedUrls(emailId)).thenReturn(List.of(url));

        ContentAnalysisDto content = new ContentAnalysisDto();
        content.setPromptInjectionDetected(true);
        content.setCredentialRequest(true);
        content.setPaymentInstruction(true);
        content.setPredictedLabel("PHISHING");
        content.setProbability(0.95);
        when(contentAnalysisService.getContentAnalysis(emailId)).thenReturn(Optional.of(content));

        RiskBreakdownDto result = service.evaluateAndSaveRisk(emailId);

        assertNotNull(result);
        assertTrue(result.getTotalUncappedScore() > 100);
        assertEquals(100, result.getDeterministicRiskScore());
        assertEquals("CRITICAL", result.getRiskLevel());
    }

    @Test
    void testReproducibility() {
        HeaderAnalysisDto headers = new HeaderAnalysisDto();
        headers.setReportedDmarc("fail");
        when(headerForensicService.getHeaderAnalysis(emailId)).thenReturn(headers);

        RiskBreakdownDto first = service.evaluateAndSaveRisk(emailId);
        RiskBreakdownDto second = service.evaluateAndSaveRisk(emailId);

        assertEquals(first.getDeterministicRiskScore(), second.getDeterministicRiskScore());
        assertEquals(first.getRiskLevel(), second.getRiskLevel());
        assertEquals(first.getEvidenceConfidenceScore(), second.getEvidenceConfidenceScore());
        assertEquals(first.getFactors().size(), second.getFactors().size());
    }
}

package com.emailforensics.backend.risk;

import com.emailforensics.backend.analysis.AnalysisResult;
import com.emailforensics.backend.analysis.AnalysisResultRepository;
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
import com.emailforensics.backend.dto.visual.VisualArtifactDto;
import com.emailforensics.backend.evidence.AnalysisRun;
import com.emailforensics.backend.evidence.AnalysisRunRepository;
import com.emailforensics.backend.evidence.AnalysisStatus;
import com.emailforensics.backend.evidence.EmailEvidence;
import com.emailforensics.backend.evidence.EmailEvidenceRepository;
import com.emailforensics.backend.headers.HeaderAnalysisService;
import com.emailforensics.backend.infrastructure.IpArtifactService;
import com.emailforensics.backend.urlintel.UrlAnalysisService;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

class RiskScoringServiceTest {

    private RiskScoringService service;
    private RiskFactorRepository riskFactorRepo;
    private AnalysisResultRepository analysisResultRepo;
    private EmailEvidenceRepository evidenceRepo;
    private AnalysisRunRepository analysisRunRepo;
    private HeaderAnalysisService headerAnalysisService;
    private UrlAnalysisService urlAnalysisService;
    private IpArtifactService ipArtifactService;
    private VisualAnalysisService visualAnalysisService;
    private ContentAnalysisService contentAnalysisService;

    private UUID emailId;
    private UUID runId;
    private EmailEvidence evidence;
    private AnalysisRun run;

    @BeforeEach
    void setUp() {
        riskFactorRepo = Mockito.mock(RiskFactorRepository.class);
        analysisResultRepo = Mockito.mock(AnalysisResultRepository.class);
        evidenceRepo = Mockito.mock(EmailEvidenceRepository.class);
        analysisRunRepo = Mockito.mock(AnalysisRunRepository.class);
        headerAnalysisService = Mockito.mock(HeaderAnalysisService.class);
        urlAnalysisService = Mockito.mock(UrlAnalysisService.class);
        ipArtifactService = Mockito.mock(IpArtifactService.class);
        visualAnalysisService = Mockito.mock(VisualAnalysisService.class);
        contentAnalysisService = Mockito.mock(ContentAnalysisService.class);

        service = new RiskScoringService(
                riskFactorRepo,
                analysisResultRepo,
                evidenceRepo,
                analysisRunRepo,
                headerAnalysisService,
                urlAnalysisService,
                ipArtifactService,
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
        when(analysisResultRepo.findByAnalysisRunId(runId)).thenReturn(Optional.empty());
    }

    @Test
    void testCleanBenignEmailNoFindings() {
        HeaderAnalysisDto headers = new HeaderAnalysisDto();
        headers.setReportedSpf("pass");
        headers.setReportedDkim("pass");
        headers.setReportedDmarc("pass");
        headers.setIdentityMismatchFlags(Collections.emptyList());
        when(headerAnalysisService.getHeaderAnalysis(emailId)).thenReturn(headers);

        ReceivedHopDto hop = new ReceivedHopDto();
        hop.setHopOrder(1);
        hop.setDelaySeconds(5L);
        when(headerAnalysisService.getReceivedHops(emailId)).thenReturn(List.of(hop));

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
        when(urlAnalysisService.getExtractedUrls(emailId)).thenReturn(List.of(url));

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
        when(headerAnalysisService.getHeaderAnalysis(emailId)).thenReturn(headers);

        when(headerAnalysisService.getReceivedHops(emailId)).thenReturn(Collections.emptyList());
        when(urlAnalysisService.getExtractedUrls(emailId)).thenReturn(Collections.emptyList());
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
        when(headerAnalysisService.getHeaderAnalysis(emailId)).thenReturn(headers);

        List<ExtractedUrlDto> urls = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ExtractedUrlDto u = new ExtractedUrlDto();
            u.setDomain("evil-paypal.com");
            u.setNormalizedUrl("http://evil-paypal.com/login?id=" + i);
            u.setLookalike(true);
            u.setIsShortened(true);
            urls.add(u);
        }
        when(urlAnalysisService.getExtractedUrls(emailId)).thenReturn(urls);

        when(headerAnalysisService.getReceivedHops(emailId)).thenReturn(Collections.emptyList());
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
        when(headerAnalysisService.getHeaderAnalysis(emailId)).thenReturn(headers);

        ExtractedUrlDto url = new ExtractedUrlDto();
        url.setDomain("login-bank.com");
        url.setLookalike(true);
        when(urlAnalysisService.getExtractedUrls(emailId)).thenReturn(List.of(url));

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
        when(headerAnalysisService.getHeaderAnalysis(emailId)).thenReturn(headers);

        ExtractedUrlDto url = new ExtractedUrlDto();
        url.setDomain("phish.com");
        url.setIsIpUrl(true);
        url.setLookalike(true);
        url.setAnchorMismatch(true);
        url.setDnsStatus("NXDOMAIN");
        when(urlAnalysisService.getExtractedUrls(emailId)).thenReturn(List.of(url));

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
        when(headerAnalysisService.getHeaderAnalysis(emailId)).thenReturn(headers);

        RiskBreakdownDto first = service.evaluateAndSaveRisk(emailId);
        RiskBreakdownDto second = service.evaluateAndSaveRisk(emailId);

        assertEquals(first.getDeterministicRiskScore(), second.getDeterministicRiskScore());
        assertEquals(first.getRiskLevel(), second.getRiskLevel());
        assertEquals(first.getEvidenceConfidenceScore(), second.getEvidenceConfidenceScore());
        assertEquals(first.getFactors().size(), second.getFactors().size());
    }

    @Test
    void testChannelStatusesAndCompletenessAlignment() {
        // 5 evaluated channels: HEADER, AUTH, ROUTE, URL, CONTENT. VISUAL is an empty list.
        HeaderAnalysisDto headers = new HeaderAnalysisDto();
        headers.setReportedSpf("pass");
        headers.setReportedDkim("pass");
        headers.setReportedDmarc("fail"); // Triggers AUTHENTICATION category
        when(headerAnalysisService.getHeaderAnalysis(emailId)).thenReturn(headers);

        ReceivedHopDto hop = new ReceivedHopDto();
        hop.setHopOrder(1);
        when(headerAnalysisService.getReceivedHops(emailId)).thenReturn(List.of(hop));

        ExtractedUrlDto url = new ExtractedUrlDto();
        url.setDomain("example.com");
        url.setLookalike(true); // Triggers URL category
        when(urlAnalysisService.getExtractedUrls(emailId)).thenReturn(List.of(url));

        ContentAnalysisDto content = new ContentAnalysisDto();
        content.setPredictedLabel("BENIGN");
        when(contentAnalysisService.getContentAnalysis(emailId)).thenReturn(Optional.of(content));

        // Visual analysis was executed but 0 visual artifacts found (empty list)
        when(visualAnalysisService.getVisualArtifacts(emailId)).thenReturn(Collections.emptyList());

        RiskBreakdownDto result = service.evaluateAndSaveRisk(emailId);

        assertNotNull(result);

        // 1. Verify channel statuses list contains all 6 channels
        List<ChannelStatusDto> statuses = result.getChannelStatuses();
        assertEquals(6, statuses.size());

        // 2. Verify individual channel statuses
        ChannelStatusDto headerStatus = statuses.stream().filter(s -> "HEADER".equals(s.getChannel())).findFirst().orElseThrow();
        ChannelStatusDto authStatus = statuses.stream().filter(s -> "AUTH".equals(s.getChannel())).findFirst().orElseThrow();
        ChannelStatusDto routeStatus = statuses.stream().filter(s -> "ROUTE".equals(s.getChannel())).findFirst().orElseThrow();
        ChannelStatusDto urlStatus = statuses.stream().filter(s -> "URL".equals(s.getChannel())).findFirst().orElseThrow();
        ChannelStatusDto contentStatus = statuses.stream().filter(s -> "CONTENT".equals(s.getChannel())).findFirst().orElseThrow();
        ChannelStatusDto visualStatus = statuses.stream().filter(s -> "VISUAL".equals(s.getChannel())).findFirst().orElseThrow();

        assertEquals("EVALUATED", headerStatus.getStatus());
        assertTrue(headerStatus.isEvaluated());
        assertTrue(headerStatus.isEvidenceFound());

        assertEquals("EVALUATED", authStatus.getStatus());
        assertTrue(authStatus.isEvaluated());
        assertTrue(authStatus.isEvidenceFound());

        assertEquals("EVALUATED", routeStatus.getStatus());
        assertTrue(routeStatus.isEvaluated());
        assertTrue(routeStatus.isEvidenceFound());

        assertEquals("EVALUATED", urlStatus.getStatus());
        assertTrue(urlStatus.isEvaluated());
        assertTrue(urlStatus.isEvidenceFound());

        assertEquals("EVALUATED", contentStatus.getStatus());
        assertTrue(contentStatus.isEvaluated());
        assertTrue(contentStatus.isEvidenceFound());

        // VISUAL has empty list -> MUST be UNAVAILABLE, not evaluated, no evidence found
        assertEquals("UNAVAILABLE", visualStatus.getStatus());
        assertFalse(visualStatus.isEvaluated());
        assertFalse(visualStatus.isEvidenceFound());
        assertEquals(0, visualStatus.getArtifactCount());
        assertTrue(visualStatus.isAnalysisExecuted());

        // 3. Evaluated channels list in DTO must match the 5 evaluated channels
        List<String> evaluatedChannels = result.getEvaluatedChannels();
        assertEquals(5, evaluatedChannels.size());
        assertTrue(evaluatedChannels.containsAll(List.of("HEADER", "AUTH", "ROUTE", "URL", "CONTENT")));
        assertFalse(evaluatedChannels.contains("VISUAL"));

        // 4. Scoring must match: 5 evaluated channels * 10 = 50 completeness pts
        // 2 categories triggered (AUTHENTICATION + URL) -> Multi-Category Support = 25 pts
        // Total confidence = 50 + 25 = 75 (HIGH)
        assertEquals(75, result.getEvidenceConfidenceScore());
        assertEquals("HIGH", result.getConfidenceLevel());

        // 5. Explanation must accurately reflect 5 channels and 50 points
        assertTrue(result.getConfidenceExplanation().contains("5 forensic channels evaluated (50 pts)"));
        assertTrue(result.getConfidenceExplanation().contains("multi-category support (25 pts)"));

        // 6. Verify triggered categories list in DTO accurately reflects the 2 triggered categories
        List<String> triggeredCategories = result.getTriggeredCategories();
        assertEquals(2, triggeredCategories.size());
        assertTrue(triggeredCategories.containsAll(List.of("AUTHENTICATION", "URL")));
    }

    @Test
    void testEmptyUrlAndVisualListsDoNotCountAsEvidence() {
        // Headers present with SPF, but hops, urls, content, visuals are empty
        HeaderAnalysisDto headers = new HeaderAnalysisDto();
        headers.setReportedSpf("pass");
        when(headerAnalysisService.getHeaderAnalysis(emailId)).thenReturn(headers);

        when(headerAnalysisService.getReceivedHops(emailId)).thenReturn(Collections.emptyList());
        when(urlAnalysisService.getExtractedUrls(emailId)).thenReturn(Collections.emptyList());
        when(visualAnalysisService.getVisualArtifacts(emailId)).thenReturn(Collections.emptyList());
        when(contentAnalysisService.getContentAnalysis(emailId)).thenReturn(Optional.empty());

        RiskBreakdownDto result = service.evaluateAndSaveRisk(emailId);

        assertNotNull(result);

        // Only HEADER and AUTH should be evaluated (2 channels)
        assertEquals(2, result.getEvaluatedChannels().size());
        assertTrue(result.getEvaluatedChannels().containsAll(List.of("HEADER", "AUTH")));

        // Empty lists for ROUTE, URL, and VISUAL must NOT give completeness points
        ChannelStatusDto urlStatus = result.getChannelStatuses().stream().filter(s -> "URL".equals(s.getChannel())).findFirst().orElseThrow();
        assertFalse(urlStatus.isEvaluated());
        assertEquals("UNAVAILABLE", urlStatus.getStatus());

        ChannelStatusDto visualStatus = result.getChannelStatuses().stream().filter(s -> "VISUAL".equals(s.getChannel())).findFirst().orElseThrow();
        assertFalse(visualStatus.isEvaluated());
        assertEquals("UNAVAILABLE", visualStatus.getStatus());

        ChannelStatusDto routeStatus = result.getChannelStatuses().stream().filter(s -> "ROUTE".equals(s.getChannel())).findFirst().orElseThrow();
        assertFalse(routeStatus.isEvaluated());
        assertEquals("UNAVAILABLE", routeStatus.getStatus());

        // 2 channels * 10 = 20 completeness points
        assertTrue(result.getConfidenceExplanation().contains("2 forensic channels evaluated"));
    }

    @Test
    void testAllSixChannelsEvaluated() {
        HeaderAnalysisDto headers = new HeaderAnalysisDto();
        headers.setReportedSpf("pass");
        when(headerAnalysisService.getHeaderAnalysis(emailId)).thenReturn(headers);

        ReceivedHopDto hop = new ReceivedHopDto();
        hop.setHopOrder(1);
        when(headerAnalysisService.getReceivedHops(emailId)).thenReturn(List.of(hop));

        ExtractedUrlDto url = new ExtractedUrlDto();
        url.setDomain("trusted.com");
        when(urlAnalysisService.getExtractedUrls(emailId)).thenReturn(List.of(url));

        ContentAnalysisDto content = new ContentAnalysisDto();
        content.setPredictedLabel("BENIGN");
        when(contentAnalysisService.getContentAnalysis(emailId)).thenReturn(Optional.of(content));

        VisualArtifactDto visual = new VisualArtifactDto();
        visual.setContentType("image/png");
        when(visualAnalysisService.getVisualArtifacts(emailId)).thenReturn(List.of(visual));

        RiskBreakdownDto result = service.evaluateAndSaveRisk(emailId);

        assertNotNull(result);
        assertEquals(6, result.getEvaluatedChannels().size());
        assertTrue(result.getEvaluatedChannels().containsAll(List.of("HEADER", "AUTH", "ROUTE", "URL", "CONTENT", "VISUAL")));

        // 6 channels * 10 = 60 completeness points
        assertTrue(result.getConfidenceExplanation().contains("6 forensic channels evaluated"));
    }
}

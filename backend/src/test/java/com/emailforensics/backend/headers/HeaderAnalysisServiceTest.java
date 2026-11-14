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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.core.io.ByteArrayResource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HeaderAnalysisServiceTest {

    private HeaderAnalysisService service;
    private EmailHeaderService mockEmailHeaderService;
    private final Path fixturesDir = Paths.get("..", "test-fixtures", "eml");

    @BeforeEach
    void setUp() {
        mockEmailHeaderService = Mockito.mock(EmailHeaderService.class);
        service = new HeaderAnalysisService(null, null, null, null, null, mockEmailHeaderService);
    }

    @Test
    void shouldExtractHeadersAndDetectMismatches() throws Exception {
        HeaderAuthResult mockAuth = HeaderAuthResult.success("Powerline.ai");
        mockAuth.setSpfResult("pass");
        mockAuth.setDkimResult("pass");
        mockAuth.setDmarcResult("fail");
        mockAuth.setArcResult("none");
        mockAuth.setDkimDomain("phish-sender.test");
        mockAuth.setSummary("Validated via Powerline.ai.");

        when(mockEmailHeaderService.analyzeHeader(any())).thenReturn(mockAuth);

        Path emlPath = fixturesDir.resolve("06-received-chain.eml");
        try (InputStream in = Files.newInputStream(emlPath)) {
            HeaderAnalysisDto dto = service.analyzeStream(in);

            assertEquals("attacker@phish-sender.test", dto.getFromAddress());
            assertEquals("phish-sender.test", dto.getFromDomain());
            assertEquals("phish-collector@external-evil.com", dto.getReplyTo());
            assertEquals("bounce@unrelated-bounce.org", dto.getReturnPath());
            assertEquals("phish-sender.test", dto.getDkimDomain());

            assertEquals("pass", dto.getReportedSpf());
            assertEquals("pass", dto.getReportedDkim());
            assertEquals("fail", dto.getReportedDmarc());
            assertEquals("none", dto.getReportedArc());
            assertEquals("Powerline.ai", dto.getAuthApiProvider());
            assertEquals("SUCCESS", dto.getAuthApiStatus());

            assertNotNull(dto.getVerificationNote());
            assertTrue(dto.getVerificationNote().contains("Powerline.ai"));

            List<String> flags = dto.getIdentityMismatchFlags();
            assertNotNull(flags);
            assertEquals(3, flags.size());

            boolean hasReplyToMismatch = flags.stream().anyMatch(f -> f.startsWith("REPLY_TO_DOMAIN_MISMATCH"));
            boolean hasReturnPathMismatch = flags.stream().anyMatch(f -> f.startsWith("RETURN_PATH_MISMATCH"));
            boolean hasSuspiciousDisplayName = flags.stream().anyMatch(f -> f.startsWith("SUSPICIOUS_DISPLAY_NAME"));

            assertTrue(hasReplyToMismatch);
            assertTrue(hasReturnPathMismatch);
            assertTrue(hasSuspiciousDisplayName);
        }
    }

    @Test
    void shouldExtractChronologicalReceivedHopsAndCalculateDelays() throws Exception {
        Path emlPath = fixturesDir.resolve("06-received-chain.eml");
        try (InputStream in = Files.newInputStream(emlPath)) {
            List<ReceivedHopDto> hops = service.extractHopsFromStream(in);

            assertEquals(3, hops.size());

            ReceivedHopDto hop1 = hops.get(0);
            assertEquals(1, hop1.getHopOrder());
            assertEquals("mail-client.local", hop1.getFromHost());
            assertEquals("192.0.2.10", hop1.getHopIp());
            assertEquals("HTTP", hop1.getProtocol());
            assertNotNull(hop1.getHopTimestamp());
            assertNull(hop1.getDelaySeconds());
            assertNotNull(hop1.getRawHopLine());

            ReceivedHopDto hop2 = hops.get(1);
            assertEquals(2, hop2.getHopOrder());
            assertEquals("outbound.phish-sender.test", hop2.getFromHost());
            assertEquals("relay2.corp.test", hop2.getByHost());
            assertEquals("203.0.113.45", hop2.getHopIp());
            assertEquals("ESMTP", hop2.getProtocol());
            assertNotNull(hop2.getHopTimestamp());
            assertEquals(105L, hop2.getDelaySeconds());

            ReceivedHopDto hop3 = hops.get(2);
            assertEquals(3, hop3.getHopOrder());
            assertEquals("relay2.corp.test", hop3.getFromHost());
            assertEquals("mx.emailforensics.local", hop3.getByHost());
            assertEquals("198.51.100.22", hop3.getHopIp());
            assertEquals("ESMTP", hop3.getProtocol());
            assertNotNull(hop3.getHopTimestamp());
            assertEquals(30L, hop3.getDelaySeconds());
        }
    }

    @Test
    void shouldHandleEmptyReceivedHeadersGracefully() {
        String minimalEmail = "From: user@example.com\r\nSubject: No Hops\r\n\r\nBody";
        InputStream in = new ByteArrayInputStream(minimalEmail.getBytes(StandardCharsets.UTF_8));
        List<ReceivedHopDto> hops = service.extractHopsFromStream(in);

        assertNotNull(hops);
        assertTrue(hops.isEmpty());
    }

    @Test
    void shouldUseApiDkimDomainAndNotGenerateLocalMismatchFlag() {
        HeaderAuthResult mockAuth = HeaderAuthResult.success("Powerline.ai");
        mockAuth.setDkimDomain("unrelated-signer.com");
        when(mockEmailHeaderService.analyzeHeader(any()))
                .thenReturn(mockAuth);

        String email = "From: CEO <ceo@company.com>\r\n" +
                "DKIM-Signature: v=1; a=rsa-sha256; d=unrelated-signer.com;\r\n" +
                "Subject: Test\r\n\r\nHello";
        InputStream in = new ByteArrayInputStream(email.getBytes(StandardCharsets.UTF_8));
        HeaderAnalysisDto dto = service.analyzeStream(in);

        assertEquals("company.com", dto.getFromDomain());
        assertEquals("unrelated-signer.com", dto.getDkimDomain());
        boolean hasDkimMismatch = dto.getIdentityMismatchFlags().stream()
                .anyMatch(f -> f.contains("DKIM_DOMAIN_MISMATCH"));
        assertFalse(hasDkimMismatch);
    }

    @Test
    void shouldHandleLargeHeaderJsonWithoutStackOverflow() {
        AnalysisRunRepository mockRunRepo = Mockito.mock(AnalysisRunRepository.class);
        AnalysisResultRepository mockResultRepo = Mockito.mock(AnalysisResultRepository.class);
        ReceivedHopRepository mockHopRepo = Mockito.mock(ReceivedHopRepository.class);

        HeaderAnalysisService headerService = new HeaderAnalysisService(
                mockResultRepo, mockHopRepo, null, mockRunRepo, null, null
        );

        UUID emailId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        AnalysisRun run = new AnalysisRun();
        run.setId(runId);

        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < 200; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"Header-").append(i).append("\":[\"").append("A".repeat(150)).append("\"]");
        }
        sb.append("}");

        AnalysisResult result = new AnalysisResult();
        result.setHeaderResultId(UUID.randomUUID());
        result.setAnalysisRun(run);
        result.setRawHeadersJson(sb.toString());
        result.setFromAddress("sender@example.com");

        when(mockRunRepo.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId))
                .thenReturn(Optional.of(run));
        when(mockResultRepo.findByAnalysisRunId(runId))
                .thenReturn(Optional.of(result));
        when(mockHopRepo.findByAnalysisRunIdOrderByHopOrderAsc(runId))
                .thenReturn(Collections.emptyList());

        HeaderAnalysisDto dto = headerService.getHeaderAnalysis(emailId);
        assertNotNull(dto);
        assertEquals(200, dto.getRawHeaders().size());
    }

    @Test
    void shouldAnalyzeAndSaveHeadersWithExternalApiData() {
        AnalysisResultRepository mockResultRepo = Mockito.mock(AnalysisResultRepository.class);
        ReceivedHopRepository mockHopRepo = Mockito.mock(ReceivedHopRepository.class);
        EmailEvidenceRepository mockEvidenceRepo = Mockito.mock(EmailEvidenceRepository.class);
        AnalysisRunRepository mockRunRepo = Mockito.mock(AnalysisRunRepository.class);
        EvidenceStorageService mockStorage = Mockito.mock(EvidenceStorageService.class);

        HeaderAnalysisService headerService = new HeaderAnalysisService(
                mockResultRepo, mockHopRepo, mockEvidenceRepo, mockRunRepo, mockStorage, mockEmailHeaderService
        );

        UUID emailId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();

        EmailEvidence evidence = new EmailEvidence();
        evidence.setId(emailId);
        evidence.setStorageKey("vault/test.eml");

        AnalysisRun run = new AnalysisRun();
        run.setId(runId);
        run.setEmailEvidence(evidence);

        String rawEml = "From: Alice <alice@example.com>\r\n" +
                "To: Bob <bob@company.com>\r\n" +
                "Subject: Test email\r\n" +
                "Received: from mail.example.com ([198.51.100.1]) by mx.company.com; Mon, 14 Nov 2026 10:00:00 +0000\r\n\r\n" +
                "Hello Bob";

        when(mockEvidenceRepo.findById(emailId)).thenReturn(Optional.of(evidence));
        when(mockRunRepo.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)).thenReturn(Optional.of(run));
        when(mockStorage.loadAsResource("vault/test.eml"))
                .thenReturn(new ByteArrayResource(rawEml.getBytes(StandardCharsets.UTF_8)));

        HeaderAuthResult mockAuthResult = HeaderAuthResult.success("Powerline.ai");
        mockAuthResult.setSpfResult("pass");
        mockAuthResult.setDkimResult("pass");
        mockAuthResult.setDmarcResult("pass");
        mockAuthResult.setArcResult("none");
        mockAuthResult.setRawResponseJson("{\"status\":\"success\"}");
        mockAuthResult.setSummary("SPF: pass, DKIM: pass, DMARC: pass");

        when(mockEmailHeaderService.analyzeHeader(any())).thenReturn(mockAuthResult);
        when(mockResultRepo.findByAnalysisRunId(runId)).thenReturn(Optional.empty());
        when(mockResultRepo.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mockHopRepo.findByAnalysisRunIdOrderByHopOrderAsc(runId)).thenReturn(Collections.emptyList());

        HeaderAnalysisDto resultDto = headerService.analyzeAndSaveHeaders(emailId);

        assertNotNull(resultDto);
        assertEquals("alice@example.com", resultDto.getFromAddress());
        assertEquals("example.com", resultDto.getFromDomain());
        assertEquals("pass", resultDto.getReportedSpf());
        assertEquals("pass", resultDto.getReportedDkim());
        assertEquals("pass", resultDto.getReportedDmarc());
        assertEquals("Powerline.ai", resultDto.getAuthApiProvider());
        assertEquals("SUCCESS", resultDto.getAuthApiStatus());

        ArgumentCaptor<AnalysisResult> resultCaptor = ArgumentCaptor.forClass(AnalysisResult.class);
        verify(mockResultRepo).saveAndFlush(resultCaptor.capture());
        AnalysisResult saved = resultCaptor.getValue();
        assertEquals("alice@example.com", saved.getFromAddress());
    }
}

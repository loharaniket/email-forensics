package com.emailforensics.backend.headers;

import com.emailforensics.backend.dto.headers.HeaderAnalysisDto;
import com.emailforensics.backend.dto.headers.ReceivedHopDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeaderForensicServiceTest {

    private HeaderForensicService service;
    private final Path fixturesDir = Paths.get("..", "test-fixtures", "eml");

    @BeforeEach
    void setUp() {
        service = new HeaderForensicService(null, null, null, null, null);
    }

    @Test
    void shouldExtractHeadersAndDetectMismatches() throws Exception {
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

            assertNotNull(dto.getVerificationNote());
            assertTrue(dto.getVerificationNote().contains("Authentication results (SPF, DKIM, DMARC, ARC) are extracted as reported"));

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
    void shouldFlagDkimDomainMismatchWhenPresent() {
        String dkimMismatchEmail = "From: CEO <ceo@company.com>\r\n" +
                "DKIM-Signature: v=1; a=rsa-sha256; d=unrelated-signer.com;\r\n" +
                "Subject: Test\r\n\r\nHello";
        InputStream in = new ByteArrayInputStream(dkimMismatchEmail.getBytes(StandardCharsets.UTF_8));
        HeaderAnalysisDto dto = service.analyzeStream(in);

        assertEquals("company.com", dto.getFromDomain());
        assertEquals("unrelated-signer.com", dto.getDkimDomain());
        boolean hasDkimMismatch = dto.getIdentityMismatchFlags().stream()
                .anyMatch(f -> f.startsWith("DKIM_DOMAIN_MISMATCH"));
        assertTrue(hasDkimMismatch);
    }
}

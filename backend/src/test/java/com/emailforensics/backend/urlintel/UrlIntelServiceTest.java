package com.emailforensics.backend.urlintel;

import com.emailforensics.backend.dto.urlintel.ExtractedUrlDto;
import com.emailforensics.backend.email.EmailParserService;
import com.emailforensics.backend.infrastructure.DnsLookupService;
import com.emailforensics.backend.infrastructure.IpClassifier;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrlIntelServiceTest {

    private UrlIntelService urlIntelService;
    private UrlNormalizationService normalizationService;
    private UrlHeuristicsService heuristicsService;
    private UrlExtractionService extractionService;
    private IpClassifier ipClassifier;
    private DnsLookupService dnsLookupService;
    private final Path fixturesDir = Paths.get("..", "test-fixtures", "eml");

    @BeforeEach
    void setUp() {
        ipClassifier = new IpClassifier();
        normalizationService = new UrlNormalizationService();
        heuristicsService = new UrlHeuristicsService(ipClassifier, normalizationService);
        extractionService = new UrlExtractionService();
        dnsLookupService = new DnsLookupService(1000);
        EmailParserService emailParserService = new EmailParserService();

        urlIntelService = new UrlIntelService(
                null, null, null, null,
                emailParserService, extractionService, normalizationService,
                heuristicsService, dnsLookupService, null
        );
    }

    @Test
    void shouldNormalizeUrlsAndStripTrackingParams() {
        String raw = "HTTP://EXAMPLE.COM:80/path/to/page?utm_source=newsletter&utm_medium=email&id=123#fragment";
        UrlNormalizationService.NormalizedUrlResult result = normalizationService.normalize(raw);

        assertEquals("http://example.com/path/to/page?id=123", result.getNormalizedUrl());
        assertEquals("example.com", result.getDomain());
        assertEquals("example.com", result.getRegisteredDomain());
        assertEquals(-1, result.getPort());
    }

    @Test
    void shouldDefangUrlsCorrectly() {
        String defanged = "hxxps://malicious[.]example[.]com:8443/login";
        String cleaned = extractionService.defang(defanged);
        assertEquals("https://malicious.example.com:8443/login", cleaned);
    }

    @Test
    void shouldDetectRegisteredDomainWithTwoLevelTlds() {
        assertEquals("evil.co.uk", normalizationService.extractRegisteredDomain("phishing.evil.co.uk"));
        assertEquals("secure-bank.com.au", normalizationService.extractRegisteredDomain("login.verify.secure-bank.com.au"));
        assertEquals("example.com", normalizationService.extractRegisteredDomain("deep.sub.domain.example.com"));
    }

    @Test
    void shouldDetectHeuristicsForIpUrlAndUnusualPort() {
        assertTrue(heuristicsService.isIpUrl("198.51.100.44"));
        assertFalse(heuristicsService.isIpUrl("secure-bank.test"));

        assertTrue(heuristicsService.isUnusualPort(8443));
        assertTrue(heuristicsService.isUnusualPort(8888));
        assertFalse(heuristicsService.isUnusualPort(80));
        assertFalse(heuristicsService.isUnusualPort(443));
        assertFalse(heuristicsService.isUnusualPort(8080));
        assertFalse(heuristicsService.isUnusualPort(-1));
    }

    @Test
    void shouldDetectShortenersAndLookalikes() {
        assertTrue(heuristicsService.isShortened("bit.ly"));
        assertTrue(heuristicsService.isShortened("tinyurl.com"));
        assertFalse(heuristicsService.isShortened("github.com"));

        assertTrue(heuristicsService.isLookalike("paypa1.com"));
        assertTrue(heuristicsService.isLookalike("micros0ft.com"));
        assertTrue(heuristicsService.isLookalike("paypal-security-update.com"));
        assertTrue(heuristicsService.isLookalike("xn--e1awd7f.com"));
        assertFalse(heuristicsService.isLookalike("paypal.com"));
    }

    @Test
    void shouldDetectAnchorMismatch() {
        String destination = "198.51.100.44";
        String anchor = "https://secure.chase-internal-portal.test/login";
        assertTrue(heuristicsService.isAnchorMismatch(destination, anchor));

        String matchingDest = "paypal.com";
        String matchingAnchor = "Log in to PayPal (https://www.paypal.com/signin)";
        assertFalse(heuristicsService.isAnchorMismatch(matchingDest, matchingAnchor));
    }

    @Test
    void shouldExtractAndAnalyzeAnchorMismatchFixture() throws Exception {
        Path emlPath = fixturesDir.resolve("02-html-anchor-mismatch.eml");
        try (InputStream in = Files.newInputStream(emlPath)) {
            List<ExtractedUrlDto> urls = urlIntelService.extractAndAnalyzeFromStream(in);

            assertNotNull(urls);
            assertEquals(1, urls.size());

            ExtractedUrlDto dto = urls.get(0);
            assertEquals("http://198.51.100.44:8080/login/verify", dto.getRawUrl());
            assertEquals("http://198.51.100.44:8080/login/verify", dto.getNormalizedUrl());
            assertEquals("198.51.100.44", dto.getDomain());
            assertEquals("HTML_ANCHOR", dto.getSourceType());
            assertEquals("https://secure.chase-internal-portal.test/login", dto.getAnchorText());
            assertTrue(dto.getAnchorMismatch());
            assertTrue(dto.getIsIpUrl());
            assertTrue(dto.getUrlRiskScore() >= 75);
            assertEquals("SKIPPED_IP", dto.getDnsStatus());
        }
    }

    @Test
    void shouldExtractPlaintextUrlsWithDeduplication() {
        String emailContent = "From: sender@test.com\r\nSubject: Test\r\n\r\n" +
                "Check this out: https://example.com/page and again https://example.com/page?utm_source=ad";
        InputStream in = new ByteArrayInputStream(emailContent.getBytes(StandardCharsets.UTF_8));
        List<ExtractedUrlDto> urls = urlIntelService.extractAndAnalyzeFromStream(in);

        assertNotNull(urls);
        assertEquals(1, urls.size());
        assertEquals("https://example.com/page", urls.get(0).getNormalizedUrl());
        assertEquals("PLAIN_TEXT", urls.get(0).getSourceType());
    }
}

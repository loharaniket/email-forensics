package com.emailforensics.backend.service;

import com.emailforensics.backend.dto.ParsedHeaderResult;
import com.emailforensics.backend.entity.Indicator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndicatorExtractionServiceTest {

    private IndicatorExtractionService service;

    @BeforeEach
    void setUp() {
        service = new IndicatorExtractionService();
    }

    @Test
    void shouldExtractHeaderDomainsAndPublicIps() {
        ParsedHeaderResult parsed = new ParsedHeaderResult();
        parsed.setFromDomain("example.com");
        parsed.setReplyToDomain("reply.org");
        parsed.setReturnPathDomain("bounce.net");

        Map<String, String> hop1 = new LinkedHashMap<>();
        hop1.put("ip", "198.51.100.25");
        Map<String, String> hop2 = new LinkedHashMap<>();
        hop2.put("ip", "10.0.0.1");
        parsed.setReceivedHops(List.of(hop1, hop2));

        List<Indicator> results = service.extractIndicators(10L, parsed);

        assertNotNull(results);
        assertTrue(results.stream().anyMatch(i -> "DOMAIN".equals(i.getType()) && "example.com".equals(i.getNormalizedValue()) && "HEADER".equals(i.getOrigin())));
        assertTrue(results.stream().anyMatch(i -> "DOMAIN".equals(i.getType()) && "reply.org".equals(i.getNormalizedValue()) && "HEADER".equals(i.getOrigin())));
        assertTrue(results.stream().anyMatch(i -> "DOMAIN".equals(i.getType()) && "bounce.net".equals(i.getNormalizedValue()) && "HEADER".equals(i.getOrigin())));
        assertTrue(results.stream().anyMatch(i -> "IP".equals(i.getType()) && "198.51.100.25".equals(i.getNormalizedValue()) && "HEADER".equals(i.getOrigin())));
        assertFalse(results.stream().anyMatch(i -> "10.0.0.1".equals(i.getNormalizedValue())));
    }

    @Test
    void shouldExtractBodyUrlsAndDomainsAndIps() {
        ParsedHeaderResult parsed = new ParsedHeaderResult();
        parsed.setBodyText("Visit https://phish-site.com/login and check server 203.0.113.15 or local 192.168.1.1");

        List<Indicator> results = service.extractIndicators(20L, parsed);

        assertTrue(results.stream().anyMatch(i -> "URL".equals(i.getType()) && "https://phish-site.com/login".equals(i.getNormalizedValue()) && "BODY".equals(i.getOrigin())));
        assertTrue(results.stream().anyMatch(i -> "DOMAIN".equals(i.getType()) && "phish-site.com".equals(i.getNormalizedValue()) && "BODY".equals(i.getOrigin())));
        assertTrue(results.stream().anyMatch(i -> "IP".equals(i.getType()) && "203.0.113.15".equals(i.getNormalizedValue()) && "BODY".equals(i.getOrigin())));
        assertFalse(results.stream().anyMatch(i -> "192.168.1.1".equals(i.getNormalizedValue())));
    }

    @Test
    void shouldExtractHtmlLinksFormsAndImages() {
        ParsedHeaderResult parsed = new ParsedHeaderResult();
        String html = "<html><body>"
                + "<a href=\"https://secure-bank.org/account\">Update</a>"
                + "<form action=\"https://auth-gateway.com/post\"></form>"
                + "<img src=\"http://cdn-tracker.net/img.png\">"
                + "<a href=\"mailto:admin@test.com\">Mail</a>"
                + "<a href=\"javascript:void(0)\">Click</a>"
                + "</body></html>";
        parsed.setBodyHtml(html);

        List<Indicator> results = service.extractIndicators(30L, parsed);

        assertTrue(results.stream().anyMatch(i -> "URL".equals(i.getType()) && "https://secure-bank.org/account".equals(i.getNormalizedValue()) && "HTML".equals(i.getOrigin())));
        assertTrue(results.stream().anyMatch(i -> "URL".equals(i.getType()) && "https://auth-gateway.com/post".equals(i.getNormalizedValue()) && "HTML".equals(i.getOrigin())));
        assertTrue(results.stream().anyMatch(i -> "URL".equals(i.getType()) && "http://cdn-tracker.net/img.png".equals(i.getNormalizedValue()) && "HTML".equals(i.getOrigin())));
        assertTrue(results.stream().anyMatch(i -> "DOMAIN".equals(i.getType()) && "secure-bank.org".equals(i.getNormalizedValue())));
        assertFalse(results.stream().anyMatch(i -> i.getValue().contains("mailto:")));
        assertFalse(results.stream().anyMatch(i -> i.getValue().contains("javascript:")));
    }

    @Test
    void shouldNormalizeDomainsAndUrls() {
        assertEquals("sub.domain.com", service.normalizeDomain("  SUB.DOMAIN.COM.  "));
        assertEquals("domain.com", service.normalizeDomain("DOMAIN.COM:8080"));

        String rawUrl = "HTTP://Example.COM:80/path/test?user=1&amp;ref=2#fragment";
        assertEquals("http://example.com/path/test?user=1&ref=2", service.normalizeUrl(rawUrl));
    }

    @Test
    void shouldDeduplicateIdenticalIndicatorsWithinSameOrigin() {
        ParsedHeaderResult parsed = new ParsedHeaderResult();
        String html = "<a href=\"https://dup.com/test\">1</a><a href=\"https://dup.com/test\">2</a><a href=\"https://dup.com/test\">3</a>";
        parsed.setBodyHtml(html);

        List<Indicator> results = service.extractIndicators(40L, parsed);
        long urlCount = results.stream().filter(i -> "URL".equals(i.getType())).count();
        long domainCount = results.stream().filter(i -> "DOMAIN".equals(i.getType())).count();

        assertEquals(1, urlCount);
        assertEquals(1, domainCount);
    }

    @Test
    void shouldFilterPrivateAndReservedIps() {
        assertFalse(service.isPublicIp("10.1.2.3"));
        assertFalse(service.isPublicIp("127.0.0.1"));
        assertFalse(service.isPublicIp("169.254.5.6"));
        assertFalse(service.isPublicIp("172.16.0.1"));
        assertFalse(service.isPublicIp("172.31.255.255"));
        assertFalse(service.isPublicIp("192.168.1.100"));
        assertFalse(service.isPublicIp("0.0.0.0"));
        assertFalse(service.isPublicIp("224.0.0.1"));

        assertTrue(service.isPublicIp("8.8.8.8"));
        assertTrue(service.isPublicIp("1.1.1.1"));
        assertTrue(service.isPublicIp("198.51.100.1"));
        assertTrue(service.isPublicIp("203.0.113.50"));
    }
}

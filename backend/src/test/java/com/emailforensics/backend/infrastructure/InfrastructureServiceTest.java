package com.emailforensics.backend.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfrastructureServiceTest {

    private IpClassifier ipClassifier;
    private GeoIpService geoIpService;
    private DnsLookupService dnsLookupService;

    @BeforeEach
    void setUp() {
        ipClassifier = new IpClassifier();
        geoIpService = new GeoIpService(ipClassifier, "nonexistent-geoip.mmdb");
        dnsLookupService = new DnsLookupService(1000);
    }

    @Test
    void shouldClassifyIpv4AndIpv6Correctly() {
        assertTrue(ipClassifier.isIpAddress("192.168.1.1"));
        assertTrue(ipClassifier.isIpAddress("10.0.0.1"));
        assertTrue(ipClassifier.isIpAddress("::1"));
        assertTrue(ipClassifier.isIpAddress("2001:0db8:85a3:0000:0000:8a2e:0370:7334"));
        assertFalse(ipClassifier.isIpAddress("example.com"));
        assertFalse(ipClassifier.isIpAddress("999.999.999.999"));
        assertFalse(ipClassifier.isIpAddress(""));
        assertFalse(ipClassifier.isIpAddress(null));
    }

    @Test
    void shouldIdentifyPrivateAndReservedIpRanges() {
        assertTrue(ipClassifier.isPrivateOrReserved("10.1.2.3"));
        assertTrue(ipClassifier.isPrivateOrReserved("172.16.0.1"));
        assertTrue(ipClassifier.isPrivateOrReserved("172.31.255.255"));
        assertTrue(ipClassifier.isPrivateOrReserved("192.168.0.1"));
        assertTrue(ipClassifier.isPrivateOrReserved("127.0.0.1"));
        assertTrue(ipClassifier.isPrivateOrReserved("169.254.10.20"));
        assertTrue(ipClassifier.isPrivateOrReserved("198.51.100.44"));
        assertTrue(ipClassifier.isPrivateOrReserved("::1"));

        assertFalse(ipClassifier.isPrivateOrReserved("8.8.8.8"));
        assertFalse(ipClassifier.isPrivateOrReserved("1.1.1.1"));
        assertFalse(ipClassifier.isPrivateOrReserved("142.250.190.46"));
    }

    @Test
    void shouldHandlePrivateIpEnrichmentGracefullyWithoutExternalCall() {
        IpArtifact artifact = geoIpService.enrichIp("192.168.1.50", "TEST_CONTEXT");

        assertNotNull(artifact);
        assertEquals("192.168.1.50", artifact.getIpAddress());
        assertTrue(artifact.getIsPrivate());
        assertEquals("PRIVATE_IP_SKIPPED", artifact.getLookupStatus());
        assertEquals("TEST_CONTEXT", artifact.getSourceContext());
    }

    @Test
    void shouldReturnDatabaseUnavailableWhenGeoIpFileMissing() {
        IpArtifact artifact = geoIpService.enrichIp("8.8.8.8", "URL_HOST");

        assertNotNull(artifact);
        assertEquals("8.8.8.8", artifact.getIpAddress());
        assertFalse(artifact.getIsPrivate());
        assertEquals("DB_UNAVAILABLE", artifact.getLookupStatus());
    }

    @Test
    void shouldResolveOrHandleSpecialDnsDomainsWithoutFailure() {
        DnsResult res = dnsLookupService.lookupDomain("secure-bank.test");
        assertNotNull(res);
        assertEquals("RESOLVED", res.getStatus());
        assertFalse(res.getARecords().isEmpty());

        DnsResult nxRes = dnsLookupService.lookupDomain("nonexistent.test");
        assertNotNull(nxRes);
        assertEquals("NXDOMAIN", nxRes.getStatus());
    }

    @Test
    void shouldEnrichPublicIpUsingBundledMaxMindDatabase() {
        GeoIpService realGeoIpService = new GeoIpService(ipClassifier, "GeoLite2-City.mmdb");
        IpArtifact artifact = realGeoIpService.enrichIp("140.82.121.3", "HOP_IP");
        assertNotNull(artifact);
        assertEquals("140.82.121.3", artifact.getIpAddress());
        assertFalse(artifact.getIsPrivate());
        assertEquals("ENRICHED", artifact.getLookupStatus());
        assertEquals("Germany", artifact.getCountry());
        assertEquals("Frankfurt am Main", artifact.getCity());
        assertNotNull(artifact.getLatitude());
        assertNotNull(artifact.getLongitude());
        assertEquals("AS36459", artifact.getAsnNumber());
        assertTrue(artifact.getOrganization().contains("GitHub"));
    }
}

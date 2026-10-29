package com.emailforensics.backend.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfrastructureServiceTest {

    private IpClassifier ipClassifier;
    private DnsLookupService dnsLookupService;
    private InfrastructureService infrastructureService;

    @BeforeEach
    void setUp() {
        ipClassifier = new IpClassifier();
        dnsLookupService = new DnsLookupService(1000);
        infrastructureService = new InfrastructureService(null, null, null, null, ipClassifier);
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
    void shouldClassifyPrivateAndPublicIpArtifacts() {
        IpArtifact privateArt = infrastructureService.createIpArtifact(null, "192.168.1.50", "TEST_CONTEXT");
        assertNotNull(privateArt);
        assertEquals("192.168.1.50", privateArt.getIpAddress());
        assertTrue(privateArt.getIsPrivate());
        assertEquals("PRIVATE_IP_SKIPPED", privateArt.getLookupStatus());
        assertEquals("NOT_CHECKED", privateArt.getDataMode());
        assertEquals("TEST_CONTEXT", privateArt.getSourceContext());

        IpArtifact publicArt = infrastructureService.createIpArtifact(null, "140.82.121.3", "HOP_IP");
        assertNotNull(publicArt);
        assertEquals("140.82.121.3", publicArt.getIpAddress());
        assertFalse(publicArt.getIsPrivate());
        assertEquals("EXTRACTED", publicArt.getLookupStatus());
        assertEquals("REAL", publicArt.getDataMode());
        assertEquals("HOP_IP", publicArt.getSourceContext());
    }

    @Test
    void shouldResolveOrHandleSpecialDnsDomainsWithoutFailure() {
        DnsResult res = dnsLookupService.lookupDomain("secure-bank.test");
        assertNotNull(res);
        assertEquals("RESOLVED", res.getStatus());
        assertEquals("SIMULATED", res.getDataMode());
        assertFalse(res.getARecords().isEmpty());

        DnsResult nxRes = dnsLookupService.lookupDomain("nonexistent.test");
        assertNotNull(nxRes);
        assertEquals("NXDOMAIN", nxRes.getStatus());
        assertEquals("SIMULATED", nxRes.getDataMode());
    }
}

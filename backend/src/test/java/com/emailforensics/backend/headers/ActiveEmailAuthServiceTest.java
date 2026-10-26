package com.emailforensics.backend.headers;

import com.emailforensics.backend.dto.headers.ReceivedHopDto;
import com.emailforensics.backend.infrastructure.DnsLookupService;
import com.emailforensics.backend.infrastructure.IpClassifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActiveEmailAuthServiceTest {

    private ActiveEmailAuthService service;
    private DnsLookupService dnsLookupService;
    private IpClassifier ipClassifier;

    @BeforeEach
    void setUp() {
        dnsLookupService = new DnsLookupService(1000);
        ipClassifier = new IpClassifier();
        service = new ActiveEmailAuthService(dnsLookupService, ipClassifier);
    }

    @Test
    void shouldEvaluateSpfPassForAuthorizedRelayIp() {
        ActiveEmailAuthService.SpfEvaluationResult result = service.evaluateSpf("phish-sender.test", "203.0.113.45");

        assertNotNull(result);
        assertEquals("PASS", result.getStatus());
        assertNotNull(result.getSpfRecord());
        assertTrue(result.getSpfRecord().contains("ip4:203.0.113.45"));
        assertEquals("203.0.113.45", result.getEvaluatedIp());
    }

    @Test
    void shouldEvaluateSpfFailForUnauthorizedRelayIp() {
        ActiveEmailAuthService.SpfEvaluationResult result = service.evaluateSpf("secure-bank.test", "192.0.2.99");

        assertNotNull(result);
        assertEquals("FAIL", result.getStatus());
        assertNotNull(result.getSpfRecord());
        assertTrue(result.getSpfRecord().contains("-all"));
    }

    @Test
    void shouldEvaluateSpfNoneWhenDomainHasNoSpfRecord() {
        ActiveEmailAuthService.SpfEvaluationResult result = service.evaluateSpf("nonexistent-domain.test", "192.0.2.1");

        assertNotNull(result);
        assertEquals("NONE", result.getStatus());
    }

    @Test
    void shouldVerifyDkimPassWhenSelectorExists() {
        String dkimHeader = "v=1; a=rsa-sha256; c=relaxed/relaxed; d=phish-sender.test; s=default; bh=abc1234=; b=xyz5678=";
        ActiveEmailAuthService.DkimEvaluationResult result = service.verifyDkim(dkimHeader, "phish-sender.test");

        assertNotNull(result);
        assertEquals("PASS", result.getStatus());
        assertEquals("default", result.getSelector());
        assertEquals("phish-sender.test", result.getSigningDomain());
        assertNotNull(result.getDkimRecord());
        assertTrue(result.getDkimRecord().contains("v=DKIM1"));
    }

    @Test
    void shouldHandleMissingOrMalformedDkimHeader() {
        ActiveEmailAuthService.DkimEvaluationResult nullResult = service.verifyDkim(null, "example.com");
        assertNotNull(nullResult);
        assertEquals("SIGNATURE_ABSENT", nullResult.getStatus());

        ActiveEmailAuthService.DkimEvaluationResult malformedResult = service.verifyDkim("v=1; no_tags_here", "example.com");
        assertNotNull(malformedResult);
        assertEquals("INVALID_DKIM_TAGS", malformedResult.getStatus());
    }

    @Test
    void shouldEvaluateDmarcPassWhenSpfAlignedAndPassing() {
        ActiveEmailAuthService.SpfEvaluationResult spf = new ActiveEmailAuthService.SpfEvaluationResult(
                "PASS", "v=spf1 ip4:203.0.113.45 ~all", "ip4:203.0.113.45", "203.0.113.45", "phish-sender.test"
        );
        ActiveEmailAuthService.DkimEvaluationResult dkim = new ActiveEmailAuthService.DkimEvaluationResult(
                "SIGNATURE_ABSENT", null, null, null, null
        );

        ActiveEmailAuthService.DmarcEvaluationResult dmarc = service.evaluateDmarc(
                "phish-sender.test", "phish-sender.test", spf, dkim
        );

        assertNotNull(dmarc);
        assertEquals("PASS", dmarc.getStatus());
        assertEquals("reject", dmarc.getPolicy());
        assertTrue(dmarc.isSpfAligned());
        assertFalse(dmarc.isDkimAligned());
    }

    @Test
    void shouldEvaluateDmarcPolicyRejectWhenAuthenticationFails() {
        ActiveEmailAuthService.SpfEvaluationResult spf = new ActiveEmailAuthService.SpfEvaluationResult(
                "FAIL", "v=spf1 ip4:198.51.100.44 -all", "-all", "192.0.2.99", "secure-bank.test"
        );
        ActiveEmailAuthService.DkimEvaluationResult dkim = new ActiveEmailAuthService.DkimEvaluationResult(
                "SELECTOR_NOT_FOUND", null, "missing", "secure-bank.test", "rsa-sha256"
        );

        ActiveEmailAuthService.DmarcEvaluationResult dmarc = service.evaluateDmarc(
                "secure-bank.test", "secure-bank.test", spf, dkim
        );

        assertNotNull(dmarc);
        assertEquals("POLICY_REJECT", dmarc.getStatus());
        assertEquals("reject", dmarc.getPolicy());
    }

    @Test
    void shouldPerformCompleteActiveAuthenticationWorkflow() {
        ReceivedHopDto hop1 = new ReceivedHopDto(
                UUID.randomUUID(), 1, "mail-client.local", "relay1.corp.test", "ESMTP",
                "203.0.113.45", Instant.now(), null, "Received line 1", null
        );
        String dkimHeader = "v=1; a=rsa-sha256; d=phish-sender.test; s=default; bh=abc; b=xyz";

        ActiveEmailAuthService.ActiveAuthResult result = service.performActiveAuthentication(
                "phish-sender.test", "phish-sender.test", dkimHeader, List.of(hop1)
        );

        assertNotNull(result);
        assertEquals("PASS", result.getActiveSpfResult());
        assertEquals("PASS", result.getActiveDkimStatus());
        assertEquals("PASS", result.getActiveDmarcStatus());
        assertNotNull(result.getSummary());
        assertTrue(result.getSummary().contains("Active DNS Email Authentication Verification"));
        assertNotNull(result.getDnsObservationJson());
        assertTrue(result.getDnsObservationJson().contains("\"activeSpf\""));
        assertTrue(result.getDnsObservationJson().contains("\"activeDkim\""));
        assertTrue(result.getDnsObservationJson().contains("\"activeDmarc\""));
    }
}

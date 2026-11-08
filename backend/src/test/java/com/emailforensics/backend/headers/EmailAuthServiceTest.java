package com.emailforensics.backend.headers;

import com.emailforensics.backend.dto.headers.ReceivedHopDto;
import com.emailforensics.backend.infrastructure.DnsLookupService;
import com.emailforensics.backend.infrastructure.IpClassifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailAuthServiceTest {

    private EmailAuthService service;
    private DnsLookupService dnsLookupService;
    private IpClassifier ipClassifier;

    @BeforeEach
    void setUp() {
        dnsLookupService = new DnsLookupService(1000);
        ipClassifier = new IpClassifier();
        service = new EmailAuthService(dnsLookupService, ipClassifier);
    }

    @Test
    void shouldEvaluateSpfPassForAuthorizedRelayIp() {
        EmailAuthService.SpfEvaluationResult result = service.evaluateSpf("phish-sender.test", "203.0.113.45");

        assertNotNull(result);
        assertEquals("PASS", result.getStatus());
        assertNotNull(result.getSpfRecord());
        assertTrue(result.getSpfRecord().contains("ip4:203.0.113.45"));
        assertEquals("203.0.113.45", result.getEvaluatedIp());
    }

    @Test
    void shouldEvaluateSpfFailForUnauthorizedRelayIp() {
        EmailAuthService.SpfEvaluationResult result = service.evaluateSpf("secure-bank.test", "192.0.2.99");

        assertNotNull(result);
        assertEquals("FAIL", result.getStatus());
        assertNotNull(result.getSpfRecord());
        assertTrue(result.getSpfRecord().contains("-all"));
    }

    @Test
    void shouldEvaluateSpfNoneWhenDomainHasNoSpfRecord() {
        EmailAuthService.SpfEvaluationResult result = service.evaluateSpf("nonexistent-domain.test", "192.0.2.1");

        assertNotNull(result);
        assertEquals("NONE", result.getStatus());
    }

    @Test
    void shouldVerifyDkimReportSelectorFoundWithoutCryptographicClaim() {
        String dkimHeader = "v=1; a=rsa-sha256; c=relaxed/relaxed; d=phish-sender.test; s=default; bh=abc1234=; b=xyz5678=";
        EmailAuthService.DkimEvaluationResult result = service.verifyDkim(dkimHeader, "phish-sender.test");

        assertNotNull(result);
        assertEquals("SELECTOR_FOUND", result.getStatus());
        assertFalse(result.isCryptographicVerified());
        assertNotNull(result.getVerificationNote());
        assertEquals("default", result.getSelector());
        assertEquals("phish-sender.test", result.getSigningDomain());
        assertNotNull(result.getDkimRecord());
        assertTrue(result.getDkimRecord().contains("v=DKIM1"));
    }

    @Test
    void shouldHandleMissingOrMalformedDkimHeader() {
        EmailAuthService.DkimEvaluationResult nullResult = service.verifyDkim(null, "example.com");
        assertNotNull(nullResult);
        assertEquals("SIGNATURE_ABSENT", nullResult.getStatus());

        EmailAuthService.DkimEvaluationResult malformedResult = service.verifyDkim("v=1; no_tags_here", "example.com");
        assertNotNull(malformedResult);
        assertEquals("INVALID_DKIM_TAGS", malformedResult.getStatus());
    }

    @Test
    void shouldPrioritizeEnvelopeSenderOverVisibleFromDomainForSpf() {
        ReceivedHopDto hop1 = new ReceivedHopDto(
                UUID.randomUUID(), 1, "mail-relay.test", "mx.corp.test", "ESMTP",
                "203.0.113.45", Instant.now(), null, "Received line 1", null
        );

        EmailAuthService.ActiveAuthResult result = service.performActiveAuthentication(
                "secure-bank.test", "phish-sender.test", null, List.of(hop1)
        );

        assertNotNull(result);
        assertEquals("PASS", result.getActiveSpfResult());
        assertEquals("MAIL_FROM", result.getSpfResult().getEvaluatedIdentity());
        assertEquals("phish-sender.test", result.getSpfResult().getDomain());
    }

    @Test
    void shouldFallbackToHeloWhenReturnPathIsEmpty() {
        ReceivedHopDto hop1 = new ReceivedHopDto(
                UUID.randomUUID(), 1, "phish-sender.test", "mx.corp.test", "ESMTP",
                "203.0.113.45", Instant.now(), null, "Received line 1", null
        );

        EmailAuthService.ActiveAuthResult result = service.performActiveAuthentication(
                "secure-bank.test", "", null, List.of(hop1)
        );

        assertNotNull(result);
        assertEquals("PASS", result.getActiveSpfResult());
        assertEquals("HELO", result.getSpfResult().getEvaluatedIdentity());
        assertEquals("phish-sender.test", result.getSpfResult().getDomain());
    }

    @Test
    void shouldEvaluateDmarcPassWhenSpfAlignedAndPassing() {
        EmailAuthService.SpfEvaluationResult spf = new EmailAuthService.SpfEvaluationResult(
                "PASS", "v=spf1 ip4:203.0.113.45 ~all", "ip4:203.0.113.45", "203.0.113.45", "phish-sender.test"
        );
        EmailAuthService.DkimEvaluationResult dkim = new EmailAuthService.DkimEvaluationResult(
                "SELECTOR_FOUND", "v=DKIM1; p=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQD", "default", "phish-sender.test", "rsa-sha256", false, "Unverified"
        );

        EmailAuthService.DmarcEvaluationResult dmarc = service.evaluateDmarc(
                "phish-sender.test", "phish-sender.test", spf, dkim
        );

        assertNotNull(dmarc);
        assertEquals("PASS", dmarc.getStatus());
        assertEquals("reject", dmarc.getPolicy());
        assertTrue(dmarc.isSpfAligned());
        assertTrue(dmarc.isDkimAligned());
    }

    @Test
    void shouldEvaluateDmarcPolicyRejectWhenAuthenticationFails() {
        EmailAuthService.SpfEvaluationResult spf = new EmailAuthService.SpfEvaluationResult(
                "FAIL", "v=spf1 ip4:198.51.100.44 -all", "-all", "192.0.2.99", "secure-bank.test"
        );
        EmailAuthService.DkimEvaluationResult dkim = new EmailAuthService.DkimEvaluationResult(
                "SELECTOR_NOT_FOUND", null, "missing", "secure-bank.test", "rsa-sha256"
        );

        EmailAuthService.DmarcEvaluationResult dmarc = service.evaluateDmarc(
                "secure-bank.test", "secure-bank.test", spf, dkim
        );

        assertNotNull(dmarc);
        assertEquals("POLICY_REJECT", dmarc.getStatus());
        assertEquals("reject", dmarc.getPolicy());
    }

    @Test
    void shouldNotPassDmarcBasedOnlyOnUnverifiedDkimKeyPublication() {
        EmailAuthService.SpfEvaluationResult spf = new EmailAuthService.SpfEvaluationResult(
                "FAIL", "v=spf1 -all", "-all", "192.0.2.99", "secure-bank.test"
        );
        EmailAuthService.DkimEvaluationResult unverifiedDkim = new EmailAuthService.DkimEvaluationResult(
                "SELECTOR_FOUND", "v=DKIM1; p=MIGf...", "default", "secure-bank.test", "rsa-sha256", false, "Unverified"
        );

        EmailAuthService.DmarcEvaluationResult dmarc = service.evaluateDmarc(
                "secure-bank.test", "secure-bank.test", spf, unverifiedDkim
        );

        assertNotNull(dmarc);
        assertEquals("POLICY_REJECT", dmarc.getStatus());
        assertFalse(dmarc.isSpfAligned() && "PASS".equals(spf.getStatus()));
    }

    @Test
    void shouldPerformCompleteActiveAuthenticationWorkflow() {
        ReceivedHopDto hop1 = new ReceivedHopDto(
                UUID.randomUUID(), 1, "mail-client.local", "relay1.corp.test", "ESMTP",
                "203.0.113.45", Instant.now(), null, "Received line 1", null
        );
        String dkimHeader = "v=1; a=rsa-sha256; d=phish-sender.test; s=default; bh=abc; b=xyz";

        EmailAuthService.ActiveAuthResult result = service.performActiveAuthentication(
                "phish-sender.test", "phish-sender.test", dkimHeader, List.of(hop1)
        );

        assertNotNull(result);
        assertEquals("PASS", result.getActiveSpfResult());
        assertEquals("SELECTOR_FOUND", result.getActiveDkimStatus());
        assertEquals("PASS", result.getActiveDmarcStatus());
        assertNotNull(result.getSummary());
        assertTrue(result.getSummary().contains("Active DNS Email Authentication Verification"));
        assertNotNull(result.getDnsObservationJson());
        assertTrue(result.getDnsObservationJson().contains("\"activeSpf\""));
        assertTrue(result.getDnsObservationJson().contains("\"activeDkim\""));
        assertTrue(result.getDnsObservationJson().contains("\"activeDmarc\""));
    }
}

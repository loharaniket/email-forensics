package com.emailforensics.backend.content;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocialEngineeringDetectorTest {

    private SocialEngineeringDetector detector;

    @BeforeEach
    void setUp() {
        detector = new SocialEngineeringDetector();
    }

    @Test
    void shouldDetectUrgencyPhrase() {
        Map<String, String> channels = Map.of("BODY_TEXT", "Immediate action is required to prevent account termination within 24 hours.");
        SocialEngineeringDetector.SocialEngineeringFindings findings = detector.analyze(channels);

        assertTrue(findings.isUrgency());
        assertFalse(findings.isCredentialRequest());
        assertFalse(findings.isPaymentInstruction());
        assertFalse(findings.getMatches().isEmpty());
        assertEquals("URGENCY", findings.getMatches().get(0).getCategory());
        assertEquals("BODY_TEXT", findings.getMatches().get(0).getSourceChannel());
    }

    @Test
    void shouldDetectCredentialHarvestingAcrossHtmlChannel() {
        Map<String, String> channels = Map.of("HTML_TEXT", "Please verify your password and reset your credentials on the security portal.");
        SocialEngineeringDetector.SocialEngineeringFindings findings = detector.analyze(channels);

        assertTrue(findings.isCredentialRequest());
        assertFalse(findings.isUrgency());
        assertEquals("CREDENTIAL_REQUEST", findings.getMatches().get(0).getCategory());
        assertEquals("HTML_TEXT", findings.getMatches().get(0).getSourceChannel());
    }

    @Test
    void shouldDetectPaymentInstructionsAcrossBodyChannel() {
        Map<String, String> channels = Map.of("BODY_TEXT", "Please execute an urgent wire transfer for overdue invoice INV-449.");
        SocialEngineeringDetector.SocialEngineeringFindings findings = detector.analyze(channels);

        assertTrue(findings.isPaymentInstruction());
        assertTrue(findings.getMatches().stream().anyMatch(m -> "PAYMENT_INSTRUCTION".equals(m.getCategory())));
        assertTrue(findings.getMatches().stream().anyMatch(m -> "BODY_TEXT".equals(m.getSourceChannel())));
    }

    @Test
    void shouldDetectImpersonationAndSecrecyAcrossQrChannel() {
        Map<String, String> channels = Map.of("QR_PAYLOAD", "I am the CEO. Strictly confidential. Keep this between us and do not call.");
        SocialEngineeringDetector.SocialEngineeringFindings findings = detector.analyze(channels);

        assertTrue(findings.isImpersonation());
        assertTrue(findings.isSecrecy());
        assertTrue(findings.getMatches().size() >= 2);
    }

    @Test
    void shouldNotTriggerOnCleanBenignText() {
        Map<String, String> channels = Map.of("BODY_TEXT", "Hello team, here are the meeting notes from yesterday's engineering sync.");
        SocialEngineeringDetector.SocialEngineeringFindings findings = detector.analyze(channels);

        assertFalse(findings.isUrgency());
        assertFalse(findings.isCredentialRequest());
        assertFalse(findings.isPaymentInstruction());
        assertFalse(findings.isImpersonation());
        assertFalse(findings.isSecrecy());
        assertTrue(findings.getMatches().isEmpty());
    }

    @Test
    void shouldHandleEmptyOrNullChannelsGracefully() {
        SocialEngineeringDetector.SocialEngineeringFindings empty = detector.analyze(null);
        assertNotNull(empty);
        assertFalse(empty.isUrgency());
        assertTrue(empty.getMatches().isEmpty());

        SocialEngineeringDetector.SocialEngineeringFindings blank = detector.analyze(Map.of("BODY_TEXT", "   "));
        assertNotNull(blank);
        assertFalse(blank.isUrgency());
    }
}


package com.emailforensics.backend.content;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocialEngineeringRuleEngineTest {

    private SocialEngineeringRuleEngine engine;

    @BeforeEach
    void setUp() {
        engine = new SocialEngineeringRuleEngine();
    }

    @Test
    void shouldDetectUrgencyPhrase() {
        Map<String, String> channels = Map.of("BODY_TEXT", "Immediate action is required to prevent account termination within 24 hours.");
        SocialEngineeringRuleEngine.SocialEngineeringFindings findings = engine.analyze(channels);

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
        SocialEngineeringRuleEngine.SocialEngineeringFindings findings = engine.analyze(channels);

        assertTrue(findings.isCredentialRequest());
        assertFalse(findings.isUrgency());
        assertEquals("CREDENTIAL_REQUEST", findings.getMatches().get(0).getCategory());
        assertEquals("HTML_TEXT", findings.getMatches().get(0).getSourceChannel());
    }

    @Test
    void shouldDetectPaymentInstructionsAcrossOcrChannel() {
        Map<String, String> channels = Map.of("OCR_TEXT", "Please execute an urgent wire transfer for overdue invoice INV-449.");
        SocialEngineeringRuleEngine.SocialEngineeringFindings findings = engine.analyze(channels);

        assertTrue(findings.isPaymentInstruction());
        assertTrue(findings.getMatches().stream().anyMatch(m -> "PAYMENT_INSTRUCTION".equals(m.getCategory())));
        assertTrue(findings.getMatches().stream().anyMatch(m -> "OCR_TEXT".equals(m.getSourceChannel())));
    }

    @Test
    void shouldDetectImpersonationAndSecrecyAcrossQrChannel() {
        Map<String, String> channels = Map.of("QR_PAYLOAD", "I am the CEO. Strictly confidential. Keep this between us and do not call.");
        SocialEngineeringRuleEngine.SocialEngineeringFindings findings = engine.analyze(channels);

        assertTrue(findings.isImpersonation());
        assertTrue(findings.isSecrecy());
        assertTrue(findings.getMatches().size() >= 2);
    }

    @Test
    void shouldNotTriggerOnCleanBenignText() {
        Map<String, String> channels = Map.of("BODY_TEXT", "Hello team, here are the meeting notes from yesterday's engineering sync.");
        SocialEngineeringRuleEngine.SocialEngineeringFindings findings = engine.analyze(channels);

        assertFalse(findings.isUrgency());
        assertFalse(findings.isCredentialRequest());
        assertFalse(findings.isPaymentInstruction());
        assertFalse(findings.isImpersonation());
        assertFalse(findings.isSecrecy());
        assertTrue(findings.getMatches().isEmpty());
    }

    @Test
    void shouldHandleEmptyOrNullChannelsGracefully() {
        SocialEngineeringRuleEngine.SocialEngineeringFindings empty = engine.analyze(null);
        assertNotNull(empty);
        assertFalse(empty.isUrgency());
        assertTrue(empty.getMatches().isEmpty());

        SocialEngineeringRuleEngine.SocialEngineeringFindings blank = engine.analyze(Map.of("BODY_TEXT", "   "));
        assertNotNull(blank);
        assertFalse(blank.isUrgency());
    }
}

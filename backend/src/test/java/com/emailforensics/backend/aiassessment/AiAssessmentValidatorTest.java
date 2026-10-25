package com.emailforensics.backend.aiassessment;

import com.emailforensics.backend.dto.aiassessment.AiStructuredOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiAssessmentValidatorTest {

    private AiAssessmentValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AiAssessmentValidator();
    }

    @Test
    void testValidOutputWithSupportedReferences() {
        List<String> whitelist = List.of("HEADER:From", "URL:https://login.example.com", "RISK:FACTOR_SPF");
        AiStructuredOutput raw = new AiStructuredOutput(
                "MALICIOUS_PHISHING",
                "HIGH",
                "Phishing attempt impersonating legitimate portal.",
                List.of("Suspicious login URL"),
                List.of("HEADER:From", "URL:https://login.example.com"),
                "Static analysis only"
        );

        AiAssessmentValidator.ValidationResult result = validator.validate(raw, whitelist);

        assertEquals("MALICIOUS_PHISHING", result.getValidatedOutput().getThreatClassification());
        assertEquals("HIGH", result.getValidatedOutput().getConfidenceAssessment());
        assertEquals(2, result.getValidatedOutput().getEvidenceReferences().size());
        assertTrue(result.getUnsupportedReferences().isEmpty());
    }

    @Test
    void testUnsupportedReferencesAreRejected() {
        List<String> whitelist = List.of("HEADER:From", "RISK:FACTOR_SPF");
        AiStructuredOutput raw = new AiStructuredOutput(
                "MALICIOUS_PHISHING",
                "HIGH",
                "Phishing message detected.",
                List.of("Hallucinated indicator"),
                List.of("HEADER:From", "HALLUCINATED:NON_EXISTENT_INDICATOR", "RANDOM_PAYLOAD_123"),
                "Caveats"
        );

        AiAssessmentValidator.ValidationResult result = validator.validate(raw, whitelist);

        assertEquals(1, result.getValidatedOutput().getEvidenceReferences().size());
        assertEquals("HEADER:From", result.getValidatedOutput().getEvidenceReferences().get(0));
        assertEquals(2, result.getUnsupportedReferences().size());
        assertTrue(result.getUnsupportedReferences().contains("HALLUCINATED:NON_EXISTENT_INDICATOR"));
        assertTrue(result.getUnsupportedReferences().contains("RANDOM_PAYLOAD_123"));
    }

    @Test
    void testMalformedClassificationAndSizeLimits() {
        List<String> whitelist = List.of("RISK:SCORE");
        String longText = "A".repeat(4000);
        AiStructuredOutput raw = new AiStructuredOutput(
                "COMPLETELY_INVALID_CLASS",
                "INVALID_CONFIDENCE",
                longText,
                List.of("Indicator 1"),
                List.of("RISK:SCORE"),
                longText
        );

        AiAssessmentValidator.ValidationResult result = validator.validate(raw, whitelist);

        assertEquals("INSUFFICIENT_DATA", result.getValidatedOutput().getThreatClassification());
        assertEquals("MEDIUM", result.getValidatedOutput().getConfidenceAssessment());
        assertTrue(result.getValidatedOutput().getSummaryExplanation().length() <= 3050);
        assertTrue(result.getValidatedOutput().getLimitations().length() <= 2050);
    }
}

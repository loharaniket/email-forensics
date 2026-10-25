package com.emailforensics.backend.aiassessment;

import com.emailforensics.backend.dto.aiassessment.AiStructuredOutput;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class AiAssessmentValidator {

    private static final Set<String> ALLOWED_CLASSIFICATIONS = Set.of(
            "MALICIOUS_PHISHING",
            "SUSPICIOUS_CREDENTIAL_HARVESTING",
            "SUSPICIOUS_MALWARE_DELIVERY",
            "BENIGN_LEGITIMATE",
            "INSUFFICIENT_DATA"
    );

    private static final Set<String> ALLOWED_CONFIDENCES = Set.of(
            "HIGH",
            "MEDIUM",
            "LOW"
    );

    public ValidationResult validate(AiStructuredOutput output, List<String> whitelistedReferences) {
        if (output == null) {
            output = new AiStructuredOutput();
        }

        String classification = output.getThreatClassification();
        if (classification == null || !ALLOWED_CLASSIFICATIONS.contains(classification.trim().toUpperCase())) {
            classification = "INSUFFICIENT_DATA";
        } else {
            classification = classification.trim().toUpperCase();
        }

        String confidence = output.getConfidenceAssessment();
        if (confidence == null || !ALLOWED_CONFIDENCES.contains(confidence.trim().toUpperCase())) {
            confidence = "MEDIUM";
        } else {
            confidence = confidence.trim().toUpperCase();
        }

        String summary = output.getSummaryExplanation();
        if (summary == null || summary.isBlank()) {
            summary = "No detailed narrative explanation was generated.";
        } else if (summary.length() > 3000) {
            summary = summary.substring(0, 3000) + "... [truncated to limit]";
        }

        String limitations = output.getLimitations();
        if (limitations == null || limitations.isBlank()) {
            limitations = "Heuristic assessment based on extracted technical indicators and static evidence.";
        } else if (limitations.length() > 2000) {
            limitations = limitations.substring(0, 2000) + "... [truncated to limit]";
        }

        List<String> keyIndicators = new ArrayList<>();
        if (output.getKeyIndicators() != null) {
            for (String ind : output.getKeyIndicators()) {
                if (ind != null && !ind.isBlank()) {
                    String clean = ind.trim();
                    if (clean.length() > 255) {
                        clean = clean.substring(0, 255);
                    }
                    if (!keyIndicators.contains(clean)) {
                        keyIndicators.add(clean);
                    }
                }
                if (keyIndicators.size() >= 20) break;
            }
        }

        Set<String> whitelistSet = new HashSet<>();
        if (whitelistedReferences != null) {
            for (String ref : whitelistedReferences) {
                if (ref != null) whitelistSet.add(ref.toLowerCase());
            }
        }

        List<String> validatedReferences = new ArrayList<>();
        List<String> unsupportedReferences = new ArrayList<>();

        if (output.getEvidenceReferences() != null) {
            for (String ref : output.getEvidenceReferences()) {
                if (ref == null || ref.isBlank()) continue;
                String cleanRef = ref.trim();
                String lowerRef = cleanRef.toLowerCase();

                boolean isMatch = false;
                if (whitelistSet.contains(lowerRef)) {
                    isMatch = true;
                } else {
                    for (String white : whitelistSet) {
                        if (lowerRef.contains(white) || white.contains(lowerRef)) {
                            isMatch = true;
                            break;
                        }
                    }
                }

                if (isMatch) {
                    if (!validatedReferences.contains(cleanRef)) {
                        validatedReferences.add(cleanRef);
                    }
                } else {
                    if (!unsupportedReferences.contains(cleanRef)) {
                        unsupportedReferences.add(cleanRef);
                    }
                }

                if (validatedReferences.size() >= 30) break;
            }
        }

        AiStructuredOutput validatedOutput = new AiStructuredOutput(
                classification,
                confidence,
                summary,
                keyIndicators,
                validatedReferences,
                limitations
        );

        return new ValidationResult(validatedOutput, unsupportedReferences);
    }

    public static class ValidationResult {
        private final AiStructuredOutput validatedOutput;
        private final List<String> unsupportedReferences;

        public ValidationResult(AiStructuredOutput validatedOutput, List<String> unsupportedReferences) {
            this.validatedOutput = validatedOutput;
            this.unsupportedReferences = unsupportedReferences != null ? unsupportedReferences : new ArrayList<>();
        }

        public AiStructuredOutput getValidatedOutput() {
            return validatedOutput;
        }

        public List<String> getUnsupportedReferences() {
            return unsupportedReferences;
        }
    }
}

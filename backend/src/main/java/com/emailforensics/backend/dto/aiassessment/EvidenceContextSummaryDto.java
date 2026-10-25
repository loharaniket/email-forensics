package com.emailforensics.backend.dto.aiassessment;

import java.util.ArrayList;
import java.util.List;

public class EvidenceContextSummaryDto {

    private List<String> whitelistedReferences = new ArrayList<>();
    private String sanitizedPromptContext;
    private boolean untrustedInjectionsDetected;

    public EvidenceContextSummaryDto() {
    }

    public EvidenceContextSummaryDto(List<String> whitelistedReferences,
                                     String sanitizedPromptContext,
                                     boolean untrustedInjectionsDetected) {
        this.whitelistedReferences = whitelistedReferences != null ? whitelistedReferences : new ArrayList<>();
        this.sanitizedPromptContext = sanitizedPromptContext;
        this.untrustedInjectionsDetected = untrustedInjectionsDetected;
    }

    public List<String> getWhitelistedReferences() {
        return whitelistedReferences;
    }

    public void setWhitelistedReferences(List<String> whitelistedReferences) {
        this.whitelistedReferences = whitelistedReferences;
    }

    public String getSanitizedPromptContext() {
        return sanitizedPromptContext;
    }

    public void setSanitizedPromptContext(String sanitizedPromptContext) {
        this.sanitizedPromptContext = sanitizedPromptContext;
    }

    public boolean isUntrustedInjectionsDetected() {
        return untrustedInjectionsDetected;
    }

    public void setUntrustedInjectionsDetected(boolean untrustedInjectionsDetected) {
        this.untrustedInjectionsDetected = untrustedInjectionsDetected;
    }
}

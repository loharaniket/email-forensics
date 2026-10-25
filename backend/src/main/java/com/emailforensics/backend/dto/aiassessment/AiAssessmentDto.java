package com.emailforensics.backend.dto.aiassessment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AiAssessmentDto {

    private UUID id;
    private UUID emailEvidenceId;
    private UUID analysisRunId;
    private String provider;
    private String modelName;
    private String promptVersion;
    private String status;
    private String threatClassification;
    private String confidenceAssessment;
    private String summaryExplanation;
    private List<String> keyIndicators = new ArrayList<>();
    private List<String> evidenceReferences = new ArrayList<>();
    private List<String> unsupportedReferences = new ArrayList<>();
    private String limitationsAndCaveats;
    private boolean untrustedInjectionsDetected;
    private String sanitizedContextSummary;
    private String errorMessage;
    private Integer tokensUsed;
    private Long generationTimeMs;
    private Instant createdAt;

    public AiAssessmentDto() {
    }

    public AiAssessmentDto(UUID id, UUID emailEvidenceId, UUID analysisRunId,
                           String provider, String modelName, String promptVersion,
                           String status, String threatClassification, String confidenceAssessment,
                           String summaryExplanation, List<String> keyIndicators,
                           List<String> evidenceReferences, List<String> unsupportedReferences,
                           String limitationsAndCaveats, boolean untrustedInjectionsDetected,
                           String sanitizedContextSummary, String errorMessage,
                           Integer tokensUsed, Long generationTimeMs, Instant createdAt) {
        this.id = id;
        this.emailEvidenceId = emailEvidenceId;
        this.analysisRunId = analysisRunId;
        this.provider = provider;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
        this.status = status;
        this.threatClassification = threatClassification;
        this.confidenceAssessment = confidenceAssessment;
        this.summaryExplanation = summaryExplanation;
        this.keyIndicators = keyIndicators != null ? keyIndicators : new ArrayList<>();
        this.evidenceReferences = evidenceReferences != null ? evidenceReferences : new ArrayList<>();
        this.unsupportedReferences = unsupportedReferences != null ? unsupportedReferences : new ArrayList<>();
        this.limitationsAndCaveats = limitationsAndCaveats;
        this.untrustedInjectionsDetected = untrustedInjectionsDetected;
        this.sanitizedContextSummary = sanitizedContextSummary;
        this.errorMessage = errorMessage;
        this.tokensUsed = tokensUsed;
        this.generationTimeMs = generationTimeMs;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEmailEvidenceId() {
        return emailEvidenceId;
    }

    public void setEmailEvidenceId(UUID emailEvidenceId) {
        this.emailEvidenceId = emailEvidenceId;
    }

    public UUID getAnalysisRunId() {
        return analysisRunId;
    }

    public void setAnalysisRunId(UUID analysisRunId) {
        this.analysisRunId = analysisRunId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getThreatClassification() {
        return threatClassification;
    }

    public void setThreatClassification(String threatClassification) {
        this.threatClassification = threatClassification;
    }

    public String getConfidenceAssessment() {
        return confidenceAssessment;
    }

    public void setConfidenceAssessment(String confidenceAssessment) {
        this.confidenceAssessment = confidenceAssessment;
    }

    public String getSummaryExplanation() {
        return summaryExplanation;
    }

    public void setSummaryExplanation(String summaryExplanation) {
        this.summaryExplanation = summaryExplanation;
    }

    public List<String> getKeyIndicators() {
        return keyIndicators;
    }

    public void setKeyIndicators(List<String> keyIndicators) {
        this.keyIndicators = keyIndicators;
    }

    public List<String> getEvidenceReferences() {
        return evidenceReferences;
    }

    public void setEvidenceReferences(List<String> evidenceReferences) {
        this.evidenceReferences = evidenceReferences;
    }

    public List<String> getUnsupportedReferences() {
        return unsupportedReferences;
    }

    public void setUnsupportedReferences(List<String> unsupportedReferences) {
        this.unsupportedReferences = unsupportedReferences;
    }

    public String getLimitationsAndCaveats() {
        return limitationsAndCaveats;
    }

    public void setLimitationsAndCaveats(String limitationsAndCaveats) {
        this.limitationsAndCaveats = limitationsAndCaveats;
    }

    public boolean isUntrustedInjectionsDetected() {
        return untrustedInjectionsDetected;
    }

    public void setUntrustedInjectionsDetected(boolean untrustedInjectionsDetected) {
        this.untrustedInjectionsDetected = untrustedInjectionsDetected;
    }

    public String getSanitizedContextSummary() {
        return sanitizedContextSummary;
    }

    public void setSanitizedContextSummary(String sanitizedContextSummary) {
        this.sanitizedContextSummary = sanitizedContextSummary;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getTokensUsed() {
        return tokensUsed;
    }

    public void setTokensUsed(Integer tokensUsed) {
        this.tokensUsed = tokensUsed;
    }

    public Long getGenerationTimeMs() {
        return generationTimeMs;
    }

    public void setGenerationTimeMs(Long generationTimeMs) {
        this.generationTimeMs = generationTimeMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

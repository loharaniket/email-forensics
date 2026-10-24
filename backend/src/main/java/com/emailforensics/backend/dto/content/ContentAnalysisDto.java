package com.emailforensics.backend.dto.content;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ContentAnalysisDto {

    private UUID id;
    private String mlModelVersion;
    private Double probability;
    private String predictedLabel;
    private String mlStatus;
    private MlEvaluationMetricsDto modelMetrics;
    private boolean urgency;
    private boolean credentialRequest;
    private boolean paymentInstruction;
    private boolean impersonation;
    private boolean secrecy;
    private boolean promptInjectionDetected;
    private List<PromptInjectionMatchDto> promptMatches = new ArrayList<>();
    private List<SocialEngineeringMatchDto> socialEngineeringMatches = new ArrayList<>();
    private Instant createdAt;

    public ContentAnalysisDto() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getMlModelVersion() {
        return mlModelVersion;
    }

    public void setMlModelVersion(String mlModelVersion) {
        this.mlModelVersion = mlModelVersion;
    }

    public Double getProbability() {
        return probability;
    }

    public void setProbability(Double probability) {
        this.probability = probability;
    }

    public String getPredictedLabel() {
        return predictedLabel;
    }

    public void setPredictedLabel(String predictedLabel) {
        this.predictedLabel = predictedLabel;
    }

    public String getMlStatus() {
        return mlStatus;
    }

    public void setMlStatus(String mlStatus) {
        this.mlStatus = mlStatus;
    }

    public MlEvaluationMetricsDto getModelMetrics() {
        return modelMetrics;
    }

    public void setModelMetrics(MlEvaluationMetricsDto modelMetrics) {
        this.modelMetrics = modelMetrics;
    }

    public boolean isUrgency() {
        return urgency;
    }

    public void setUrgency(boolean urgency) {
        this.urgency = urgency;
    }

    public boolean isCredentialRequest() {
        return credentialRequest;
    }

    public void setCredentialRequest(boolean credentialRequest) {
        this.credentialRequest = credentialRequest;
    }

    public boolean isPaymentInstruction() {
        return paymentInstruction;
    }

    public void setPaymentInstruction(boolean paymentInstruction) {
        this.paymentInstruction = paymentInstruction;
    }

    public boolean isImpersonation() {
        return impersonation;
    }

    public void setImpersonation(boolean impersonation) {
        this.impersonation = impersonation;
    }

    public boolean isSecrecy() {
        return secrecy;
    }

    public void setSecrecy(boolean secrecy) {
        this.secrecy = secrecy;
    }

    public boolean isPromptInjectionDetected() {
        return promptInjectionDetected;
    }

    public void setPromptInjectionDetected(boolean promptInjectionDetected) {
        this.promptInjectionDetected = promptInjectionDetected;
    }

    public List<PromptInjectionMatchDto> getPromptMatches() {
        return promptMatches;
    }

    public void setPromptMatches(List<PromptInjectionMatchDto> promptMatches) {
        this.promptMatches = promptMatches;
    }

    public List<SocialEngineeringMatchDto> getSocialEngineeringMatches() {
        return socialEngineeringMatches;
    }

    public void setSocialEngineeringMatches(List<SocialEngineeringMatchDto> socialEngineeringMatches) {
        this.socialEngineeringMatches = socialEngineeringMatches;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

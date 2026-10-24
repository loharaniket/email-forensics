package com.emailforensics.backend.content;

import com.emailforensics.backend.evidence.AnalysisRun;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "content_analysis")
public class ContentAnalysis {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_run_id", nullable = false, unique = true)
    private AnalysisRun analysisRun;

    @Column(name = "ml_model_version", length = 80)
    private String mlModelVersion;

    @Column(name = "probability")
    private Double probability;

    @Column(name = "predicted_label", length = 80)
    private String predictedLabel;

    @Column(name = "ml_status", length = 50)
    private String mlStatus;

    @Column(name = "urgency", nullable = false)
    private boolean urgency;

    @Column(name = "credential_request", nullable = false)
    private boolean credentialRequest;

    @Column(name = "payment_instruction", nullable = false)
    private boolean paymentInstruction;

    @Column(name = "impersonation", nullable = false)
    private boolean impersonation;

    @Column(name = "secrecy", nullable = false)
    private boolean secrecy;

    @Column(name = "prompt_injection_detected", nullable = false)
    private boolean promptInjectionDetected;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "prompt_matches", columnDefinition = "jsonb")
    private String promptMatchesJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "social_engineering_matches", columnDefinition = "jsonb")
    private String socialEngineeringMatchesJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public ContentAnalysis() {
    }

    public ContentAnalysis(UUID id, AnalysisRun analysisRun, String mlModelVersion, Double probability,
                           String predictedLabel, String mlStatus, boolean urgency, boolean credentialRequest,
                           boolean paymentInstruction, boolean impersonation, boolean secrecy,
                           boolean promptInjectionDetected, String promptMatchesJson,
                           String socialEngineeringMatchesJson) {
        this.id = id;
        this.analysisRun = analysisRun;
        this.mlModelVersion = mlModelVersion;
        this.probability = probability;
        this.predictedLabel = predictedLabel;
        this.mlStatus = mlStatus;
        this.urgency = urgency;
        this.credentialRequest = credentialRequest;
        this.paymentInstruction = paymentInstruction;
        this.impersonation = impersonation;
        this.secrecy = secrecy;
        this.promptInjectionDetected = promptInjectionDetected;
        this.promptMatchesJson = promptMatchesJson;
        this.socialEngineeringMatchesJson = socialEngineeringMatchesJson;
        this.createdAt = Instant.now();
    }

    @PrePersist
    public void onPrePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public AnalysisRun getAnalysisRun() {
        return analysisRun;
    }

    public void setAnalysisRun(AnalysisRun analysisRun) {
        this.analysisRun = analysisRun;
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

    public String getPromptMatchesJson() {
        return promptMatchesJson;
    }

    public void setPromptMatchesJson(String promptMatchesJson) {
        this.promptMatchesJson = promptMatchesJson;
    }

    public String getSocialEngineeringMatchesJson() {
        return socialEngineeringMatchesJson;
    }

    public void setSocialEngineeringMatchesJson(String socialEngineeringMatchesJson) {
        this.socialEngineeringMatchesJson = socialEngineeringMatchesJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

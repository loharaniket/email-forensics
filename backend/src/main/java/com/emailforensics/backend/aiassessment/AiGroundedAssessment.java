package com.emailforensics.backend.aiassessment;

import com.emailforensics.backend.evidence.AnalysisRun;
import com.emailforensics.backend.evidence.EmailEvidence;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_grounded_assessments")
public class AiGroundedAssessment {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_evidence_id", nullable = false)
    private EmailEvidence emailEvidence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_run_id", nullable = false)
    private AnalysisRun analysisRun;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "prompt_version", nullable = false, length = 50)
    private String promptVersion;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "threat_classification", nullable = false, length = 50)
    private String threatClassification;

    @Column(name = "confidence_assessment", nullable = false, length = 20)
    private String confidenceAssessment;

    @Column(name = "summary_explanation", nullable = false, columnDefinition = "TEXT")
    private String summaryExplanation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "key_indicators_json", columnDefinition = "jsonb")
    private String keyIndicatorsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_references_json", columnDefinition = "jsonb")
    private String evidenceReferencesJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "unsupported_references_json", columnDefinition = "jsonb")
    private String unsupportedReferencesJson;

    @Column(name = "limitations_and_caveats", nullable = false, columnDefinition = "TEXT")
    private String limitationsAndCaveats;

    @Column(name = "untrusted_injections_detected", nullable = false)
    private boolean untrustedInjectionsDetected;

    @Column(name = "sanitized_context_summary", columnDefinition = "TEXT")
    private String sanitizedContextSummary;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "generation_time_ms")
    private Long generationTimeMs;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public AiGroundedAssessment() {
    }

    public AiGroundedAssessment(UUID id, EmailEvidence emailEvidence, AnalysisRun analysisRun,
                                String provider, String modelName, String promptVersion,
                                String status, String threatClassification, String confidenceAssessment,
                                String summaryExplanation, String keyIndicatorsJson,
                                String evidenceReferencesJson, String unsupportedReferencesJson,
                                String limitationsAndCaveats, boolean untrustedInjectionsDetected,
                                String sanitizedContextSummary, String errorMessage,
                                Integer tokensUsed, Long generationTimeMs) {
        this.id = id;
        this.emailEvidence = emailEvidence;
        this.analysisRun = analysisRun;
        this.provider = provider;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
        this.status = status;
        this.threatClassification = threatClassification;
        this.confidenceAssessment = confidenceAssessment;
        this.summaryExplanation = summaryExplanation;
        this.keyIndicatorsJson = keyIndicatorsJson;
        this.evidenceReferencesJson = evidenceReferencesJson;
        this.unsupportedReferencesJson = unsupportedReferencesJson;
        this.limitationsAndCaveats = limitationsAndCaveats;
        this.untrustedInjectionsDetected = untrustedInjectionsDetected;
        this.sanitizedContextSummary = sanitizedContextSummary;
        this.errorMessage = errorMessage;
        this.tokensUsed = tokensUsed;
        this.generationTimeMs = generationTimeMs;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public EmailEvidence getEmailEvidence() {
        return emailEvidence;
    }

    public void setEmailEvidence(EmailEvidence emailEvidence) {
        this.emailEvidence = emailEvidence;
    }

    public AnalysisRun getAnalysisRun() {
        return analysisRun;
    }

    public void setAnalysisRun(AnalysisRun analysisRun) {
        this.analysisRun = analysisRun;
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

    public String getKeyIndicatorsJson() {
        return keyIndicatorsJson;
    }

    public void setKeyIndicatorsJson(String keyIndicatorsJson) {
        this.keyIndicatorsJson = keyIndicatorsJson;
    }

    public String getEvidenceReferencesJson() {
        return evidenceReferencesJson;
    }

    public void setEvidenceReferencesJson(String evidenceReferencesJson) {
        this.evidenceReferencesJson = evidenceReferencesJson;
    }

    public String getUnsupportedReferencesJson() {
        return unsupportedReferencesJson;
    }

    public void setUnsupportedReferencesJson(String unsupportedReferencesJson) {
        this.unsupportedReferencesJson = unsupportedReferencesJson;
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

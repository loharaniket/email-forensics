package com.emailforensics.backend.analysis;

import com.emailforensics.backend.evidence.AnalysisRun;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_analysis_results")
public class AnalysisResult implements Persistable<UUID> {

    @Id
    @Column(name = "analysis_run_id")
    private UUID analysisRunId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_run_id", insertable = false, updatable = false)
    private AnalysisRun analysisRun;

    @Transient
    private boolean isNewEntity = true;

    // ==========================================
    // Header result fields
    // ==========================================

    @Column(name = "header_result_id")
    private UUID headerResultId;

    @Column(name = "sender_email", length = 254)
    private String fromAddress;

    @Column(name = "sender_domain", length = 255)
    private String fromDomain;

    @Column(name = "reply_to", length = 254)
    private String replyTo;

    @Column(name = "return_path", length = 254)
    private String returnPath;

    @Column(name = "dkim_domain", length = 255)
    private String dkimDomain;

    @Column(name = "spf_result", length = 40)
    private String reportedSpf;

    @Column(name = "dkim_result", length = 40)
    private String reportedDkim;

    @Column(name = "dmarc_result", length = 40)
    private String reportedDmarc;

    @Column(name = "arc_result", length = 40)
    private String reportedArc;

    @Column(name = "dns_observation", columnDefinition = "TEXT")
    private String currentDnsObservation;

    @Column(name = "sender_verification", columnDefinition = "TEXT")
    private String independentVerification;

    @Column(name = "sender_mismatch_flags", columnDefinition = "TEXT")
    private String identityMismatchFlags;

    @Column(name = "raw_headers_json", columnDefinition = "TEXT")
    private String rawHeadersJson;

    @Column(name = "header_created_at")
    private Instant headerCreatedAt;

    // ==========================================
    // Content analysis fields
    // ==========================================

    @Column(name = "content_result_id")
    private UUID contentResultId;

    @Column(name = "ml_model_version", length = 80)
    private String mlModelVersion;

    @Column(name = "ml_probability")
    private Double probability;

    @Column(name = "ml_prediction", length = 80)
    private String predictedLabel;

    @Column(name = "ml_status", length = 50)
    private String mlStatus;

    @Column(name = "has_urgent_language")
    private Boolean urgency;

    @Column(name = "asks_for_credentials")
    private Boolean credentialRequest;

    @Column(name = "has_payment_request")
    private Boolean paymentInstruction;

    @Column(name = "detects_impersonation")
    private Boolean impersonation;

    @Column(name = "asks_for_secrecy")
    private Boolean secrecy;

    @Column(name = "has_prompt_injection")
    private Boolean promptInjectionDetected;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "prompt_matches_json", columnDefinition = "jsonb")
    private String promptMatchesJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "social_engineering_matches", columnDefinition = "jsonb")
    private String socialEngineeringMatchesJson;

    @Column(name = "content_created_at")
    private Instant contentCreatedAt;

    // ==========================================
    // Risk assessment fields
    // ==========================================

    @Column(name = "risk_result_id")
    private UUID riskResultId;

    @Column(name = "risk_score")
    private Integer deterministicRiskScore;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "evidence_confidence_score")
    private Integer evidenceConfidenceScore;

    @Column(name = "confidence_level", length = 20)
    private String confidenceLevel;

    @Column(name = "confidence_explanation", columnDefinition = "TEXT")
    private String confidenceExplanation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "risk_scores_by_category_json", columnDefinition = "jsonb")
    private String categoryScoresJson;

    @Column(name = "raw_risk_score")
    private Integer totalUncappedScore;

    @Column(name = "risk_engine_version", length = 50)
    private String engineVersion;

    @Column(name = "risk_checked_at")
    private Instant evaluatedAt;

    @Column(name = "risk_created_at")
    private Instant riskCreatedAt;

    public AnalysisResult() {
    }

    public AnalysisResult(AnalysisRun analysisRun) {
        this.analysisRun = analysisRun;
        if (analysisRun != null) {
            this.analysisRunId = analysisRun.getId();
        }
    }

    @Override
    public UUID getId() {
        return analysisRunId;
    }

    @Override
    public boolean isNew() {
        return isNewEntity;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNewEntity = false;
    }

    @PrePersist
    public void onPrePersist() {
        if (this.analysisRunId == null && this.analysisRun != null) {
            this.analysisRunId = this.analysisRun.getId();
        }
    }

    // Presence checks
    public boolean hasHeaderAnalysis() {
        return this.headerResultId != null;
    }

    public boolean hasContentAnalysis() {
        return this.contentResultId != null;
    }

    public boolean hasRiskAssessment() {
        return this.riskResultId != null;
    }

    // Getters and Setters

    public UUID getAnalysisRunId() {
        return analysisRunId;
    }

    public void setAnalysisRunId(UUID analysisRunId) {
        this.analysisRunId = analysisRunId;
    }

    public AnalysisRun getAnalysisRun() {
        return analysisRun;
    }

    public void setAnalysisRun(AnalysisRun analysisRun) {
        this.analysisRun = analysisRun;
        if (analysisRun != null && this.analysisRunId == null) {
            this.analysisRunId = analysisRun.getId();
        }
    }

    public UUID getHeaderResultId() {
        return headerResultId;
    }

    public void setHeaderResultId(UUID headerResultId) {
        this.headerResultId = headerResultId;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getFromDomain() {
        return fromDomain;
    }

    public void setFromDomain(String fromDomain) {
        this.fromDomain = fromDomain;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public String getReturnPath() {
        return returnPath;
    }

    public void setReturnPath(String returnPath) {
        this.returnPath = returnPath;
    }

    public String getDkimDomain() {
        return dkimDomain;
    }

    public void setDkimDomain(String dkimDomain) {
        this.dkimDomain = dkimDomain;
    }

    public String getReportedSpf() {
        return reportedSpf;
    }

    public void setReportedSpf(String reportedSpf) {
        this.reportedSpf = reportedSpf;
    }

    public String getReportedDkim() {
        return reportedDkim;
    }

    public void setReportedDkim(String reportedDkim) {
        this.reportedDkim = reportedDkim;
    }

    public String getReportedDmarc() {
        return reportedDmarc;
    }

    public void setReportedDmarc(String reportedDmarc) {
        this.reportedDmarc = reportedDmarc;
    }

    public String getReportedArc() {
        return reportedArc;
    }

    public void setReportedArc(String reportedArc) {
        this.reportedArc = reportedArc;
    }

    public String getCurrentDnsObservation() {
        return currentDnsObservation;
    }

    public void setCurrentDnsObservation(String currentDnsObservation) {
        this.currentDnsObservation = currentDnsObservation;
    }

    public String getIndependentVerification() {
        return independentVerification;
    }

    public void setIndependentVerification(String independentVerification) {
        this.independentVerification = independentVerification;
    }

    public String getIdentityMismatchFlags() {
        return identityMismatchFlags;
    }

    public void setIdentityMismatchFlags(String identityMismatchFlags) {
        this.identityMismatchFlags = identityMismatchFlags;
    }

    public String getRawHeadersJson() {
        return rawHeadersJson;
    }

    public void setRawHeadersJson(String rawHeadersJson) {
        this.rawHeadersJson = rawHeadersJson;
    }

    public Instant getHeaderCreatedAt() {
        return headerCreatedAt;
    }

    public void setHeaderCreatedAt(Instant headerCreatedAt) {
        this.headerCreatedAt = headerCreatedAt;
    }

    public UUID getContentResultId() {
        return contentResultId;
    }

    public void setContentResultId(UUID contentResultId) {
        this.contentResultId = contentResultId;
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
        return urgency != null && urgency;
    }

    public Boolean getUrgency() {
        return urgency;
    }

    public void setUrgency(Boolean urgency) {
        this.urgency = urgency;
    }

    public boolean isCredentialRequest() {
        return credentialRequest != null && credentialRequest;
    }

    public Boolean getCredentialRequest() {
        return credentialRequest;
    }

    public void setCredentialRequest(Boolean credentialRequest) {
        this.credentialRequest = credentialRequest;
    }

    public boolean isPaymentInstruction() {
        return paymentInstruction != null && paymentInstruction;
    }

    public Boolean getPaymentInstruction() {
        return paymentInstruction;
    }

    public void setPaymentInstruction(Boolean paymentInstruction) {
        this.paymentInstruction = paymentInstruction;
    }

    public boolean isImpersonation() {
        return impersonation != null && impersonation;
    }

    public Boolean getImpersonation() {
        return impersonation;
    }

    public void setImpersonation(Boolean impersonation) {
        this.impersonation = impersonation;
    }

    public boolean isSecrecy() {
        return secrecy != null && secrecy;
    }

    public Boolean getSecrecy() {
        return secrecy;
    }

    public void setSecrecy(Boolean secrecy) {
        this.secrecy = secrecy;
    }

    public boolean isPromptInjectionDetected() {
        return promptInjectionDetected != null && promptInjectionDetected;
    }

    public Boolean getPromptInjectionDetected() {
        return promptInjectionDetected;
    }

    public void setPromptInjectionDetected(Boolean promptInjectionDetected) {
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

    public Instant getContentCreatedAt() {
        return contentCreatedAt;
    }

    public void setContentCreatedAt(Instant contentCreatedAt) {
        this.contentCreatedAt = contentCreatedAt;
    }

    public UUID getRiskResultId() {
        return riskResultId;
    }

    public void setRiskResultId(UUID riskResultId) {
        this.riskResultId = riskResultId;
    }

    public Integer getDeterministicRiskScore() {
        return deterministicRiskScore;
    }

    public void setDeterministicRiskScore(Integer deterministicRiskScore) {
        this.deterministicRiskScore = deterministicRiskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Integer getEvidenceConfidenceScore() {
        return evidenceConfidenceScore;
    }

    public void setEvidenceConfidenceScore(Integer evidenceConfidenceScore) {
        this.evidenceConfidenceScore = evidenceConfidenceScore;
    }

    public String getConfidenceLevel() {
        return confidenceLevel;
    }

    public void setConfidenceLevel(String confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }

    public String getConfidenceExplanation() {
        return confidenceExplanation;
    }

    public void setConfidenceExplanation(String confidenceExplanation) {
        this.confidenceExplanation = confidenceExplanation;
    }

    public String getCategoryScoresJson() {
        return categoryScoresJson;
    }

    public void setCategoryScoresJson(String categoryScoresJson) {
        this.categoryScoresJson = categoryScoresJson;
    }

    public Integer getTotalUncappedScore() {
        return totalUncappedScore;
    }

    public void setTotalUncappedScore(Integer totalUncappedScore) {
        this.totalUncappedScore = totalUncappedScore;
    }

    public String getEngineVersion() {
        return engineVersion;
    }

    public void setEngineVersion(String engineVersion) {
        this.engineVersion = engineVersion;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(Instant evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }

    public Instant getRiskCreatedAt() {
        return riskCreatedAt;
    }

    public void setRiskCreatedAt(Instant riskCreatedAt) {
        this.riskCreatedAt = riskCreatedAt;
    }
}

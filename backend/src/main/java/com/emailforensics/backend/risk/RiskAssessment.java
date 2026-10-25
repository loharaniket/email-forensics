package com.emailforensics.backend.risk;

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
@Table(name = "risk_assessments")
public class RiskAssessment {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_run_id", nullable = false, unique = true)
    private AnalysisRun analysisRun;

    @Column(name = "deterministic_risk_score", nullable = false)
    private Integer deterministicRiskScore;

    @Column(name = "risk_level", nullable = false, length = 20)
    private String riskLevel;

    @Column(name = "evidence_confidence_score", nullable = false)
    private Integer evidenceConfidenceScore;

    @Column(name = "confidence_level", nullable = false, length = 20)
    private String confidenceLevel;

    @Column(name = "confidence_explanation", nullable = false, columnDefinition = "TEXT")
    private String confidenceExplanation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "category_scores_json", columnDefinition = "JSONB")
    private String categoryScoresJson;

    @Column(name = "total_uncapped_score", nullable = false)
    private Integer totalUncappedScore = 0;

    @Column(name = "engine_version", nullable = false, length = 50)
    private String engineVersion = "v1.0.0";

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public RiskAssessment() {
    }

    public RiskAssessment(UUID id, AnalysisRun analysisRun, Integer deterministicRiskScore,
                          String riskLevel, Integer evidenceConfidenceScore,
                          String confidenceLevel, String confidenceExplanation,
                          String categoryScoresJson, Integer totalUncappedScore,
                          String engineVersion) {
        this.id = id;
        this.analysisRun = analysisRun;
        this.deterministicRiskScore = deterministicRiskScore;
        this.riskLevel = riskLevel;
        this.evidenceConfidenceScore = evidenceConfidenceScore;
        this.confidenceLevel = confidenceLevel;
        this.confidenceExplanation = confidenceExplanation;
        this.categoryScoresJson = categoryScoresJson;
        this.totalUncappedScore = totalUncappedScore;
        this.engineVersion = engineVersion;
        this.evaluatedAt = Instant.now();
        this.createdAt = Instant.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.evaluatedAt == null) {
            this.evaluatedAt = Instant.now();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.engineVersion == null) {
            this.engineVersion = "v1.0.0";
        }
        if (this.totalUncappedScore == null) {
            this.totalUncappedScore = 0;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

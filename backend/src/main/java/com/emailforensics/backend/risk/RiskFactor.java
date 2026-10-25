package com.emailforensics.backend.risk;

import com.emailforensics.backend.evidence.AnalysisRun;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "risk_factors")
public class RiskFactor {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_run_id", nullable = false)
    private AnalysisRun analysisRun;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "factor_code", nullable = false, length = 80)
    private String factorCode;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @Column(name = "weight_points", nullable = false)
    private Integer weightPoints;

    @Column(name = "explanation", nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "evidence_reference", nullable = false, length = 255)
    private String evidenceReference;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public RiskFactor() {
    }

    public RiskFactor(UUID id, AnalysisRun analysisRun, String category, String factorCode,
                      String severity, Integer weightPoints, String explanation,
                      String evidenceReference) {
        this.id = id;
        this.analysisRun = analysisRun;
        this.category = category;
        this.factorCode = factorCode;
        this.severity = severity;
        this.weightPoints = weightPoints;
        this.explanation = explanation;
        this.evidenceReference = evidenceReference;
        this.createdAt = Instant.now();
    }

    @PrePersist
    public void prePersist() {
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getFactorCode() {
        return factorCode;
    }

    public void setFactorCode(String factorCode) {
        this.factorCode = factorCode;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public Integer getWeightPoints() {
        return weightPoints;
    }

    public void setWeightPoints(Integer weightPoints) {
        this.weightPoints = weightPoints;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getEvidenceReference() {
        return evidenceReference;
    }

    public void setEvidenceReference(String evidenceReference) {
        this.evidenceReference = evidenceReference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

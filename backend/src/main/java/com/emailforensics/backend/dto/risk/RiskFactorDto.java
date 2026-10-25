package com.emailforensics.backend.dto.risk;

import java.time.Instant;
import java.util.UUID;

public class RiskFactorDto {

    private UUID id;
    private String category;
    private String factorCode;
    private String severity;
    private Integer weightPoints;
    private String explanation;
    private String evidenceReference;
    private Instant createdAt;

    public RiskFactorDto() {
    }

    public RiskFactorDto(UUID id, String category, String factorCode, String severity,
                         Integer weightPoints, String explanation, String evidenceReference,
                         Instant createdAt) {
        this.id = id;
        this.category = category;
        this.factorCode = factorCode;
        this.severity = severity;
        this.weightPoints = weightPoints;
        this.explanation = explanation;
        this.evidenceReference = evidenceReference;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

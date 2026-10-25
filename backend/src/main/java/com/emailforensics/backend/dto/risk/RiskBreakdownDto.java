package com.emailforensics.backend.dto.risk;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class RiskBreakdownDto {

    private UUID analysisRunId;
    private Integer deterministicRiskScore;
    private String riskLevel;
    private Integer evidenceConfidenceScore;
    private String confidenceLevel;
    private String confidenceExplanation;
    private Integer totalUncappedScore;
    private String engineVersion;
    private Instant evaluatedAt;
    private List<RiskCategoryScoreDto> categoryScores;
    private List<RiskFactorDto> factors;

    public RiskBreakdownDto() {
    }

    public RiskBreakdownDto(UUID analysisRunId, Integer deterministicRiskScore, String riskLevel,
                            Integer evidenceConfidenceScore, String confidenceLevel,
                            String confidenceExplanation, Integer totalUncappedScore,
                            String engineVersion, Instant evaluatedAt,
                            List<RiskCategoryScoreDto> categoryScores,
                            List<RiskFactorDto> factors) {
        this.analysisRunId = analysisRunId;
        this.deterministicRiskScore = deterministicRiskScore;
        this.riskLevel = riskLevel;
        this.evidenceConfidenceScore = evidenceConfidenceScore;
        this.confidenceLevel = confidenceLevel;
        this.confidenceExplanation = confidenceExplanation;
        this.totalUncappedScore = totalUncappedScore;
        this.engineVersion = engineVersion;
        this.evaluatedAt = evaluatedAt;
        this.categoryScores = categoryScores;
        this.factors = factors;
    }

    public UUID getAnalysisRunId() {
        return analysisRunId;
    }

    public void setAnalysisRunId(UUID analysisRunId) {
        this.analysisRunId = analysisRunId;
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

    public List<RiskCategoryScoreDto> getCategoryScores() {
        return categoryScores;
    }

    public void setCategoryScores(List<RiskCategoryScoreDto> categoryScores) {
        this.categoryScores = categoryScores;
    }

    public List<RiskFactorDto> getFactors() {
        return factors;
    }

    public void setFactors(List<RiskFactorDto> factors) {
        this.factors = factors;
    }
}

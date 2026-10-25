package com.emailforensics.backend.dto.risk;

public class RiskCategoryScoreDto {

    private String category;
    private String displayName;
    private Integer score;
    private Integer factorCount;

    public RiskCategoryScoreDto() {
    }

    public RiskCategoryScoreDto(String category, String displayName, Integer score, Integer factorCount) {
        this.category = category;
        this.displayName = displayName;
        this.score = score;
        this.factorCount = factorCount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getFactorCount() {
        return factorCount;
    }

    public void setFactorCount(Integer factorCount) {
        this.factorCount = factorCount;
    }
}

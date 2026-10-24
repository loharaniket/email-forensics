package com.emailforensics.backend.dto.content;

public class PromptInjectionMatchDto {

    private String category;
    private String matchedPattern;
    private String sourceChannel;
    private String explanation;
    private String severity;

    public PromptInjectionMatchDto() {
    }

    public PromptInjectionMatchDto(String category, String matchedPattern, String sourceChannel, String explanation, String severity) {
        this.category = category;
        this.matchedPattern = matchedPattern;
        this.sourceChannel = sourceChannel;
        this.explanation = explanation;
        this.severity = severity;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getMatchedPattern() {
        return matchedPattern;
    }

    public void setMatchedPattern(String matchedPattern) {
        this.matchedPattern = matchedPattern;
    }

    public String getSourceChannel() {
        return sourceChannel;
    }

    public void setSourceChannel(String sourceChannel) {
        this.sourceChannel = sourceChannel;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
}

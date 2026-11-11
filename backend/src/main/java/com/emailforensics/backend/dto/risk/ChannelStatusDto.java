package com.emailforensics.backend.dto.risk;

/**
 * Data transfer object representing the evaluation status of a forensic channel.
 * Backend serves as the single source of truth for channel status across the application.
 */
public class ChannelStatusDto {

    private String channel;           // "HEADER", "AUTH", "ROUTE", "URL", "CONTENT", "VISUAL"
    private String status;            // "EVALUATED" or "UNAVAILABLE"
    private boolean evaluated;        // true if channel evidence was evaluated for confidence scoring
    private boolean analysisExecuted; // true if analysis pipeline stage executed for this channel
    private boolean evidenceFound;    // true if actual forensic evidence / artifacts were found
    private int artifactCount;        // count of artifacts found (e.g. hops count, URLs count, etc.)
    private String details;           // human-readable status details

    public ChannelStatusDto() {
    }

    public ChannelStatusDto(String channel, String status, boolean evaluated,
                            boolean analysisExecuted, boolean evidenceFound,
                            int artifactCount, String details) {
        this.channel = channel;
        this.status = status;
        this.evaluated = evaluated;
        this.analysisExecuted = analysisExecuted;
        this.evidenceFound = evidenceFound;
        this.artifactCount = artifactCount;
        this.details = details;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isEvaluated() {
        return evaluated;
    }

    public void setEvaluated(boolean evaluated) {
        this.evaluated = evaluated;
    }

    public boolean isAnalysisExecuted() {
        return analysisExecuted;
    }

    public void setAnalysisExecuted(boolean analysisExecuted) {
        this.analysisExecuted = analysisExecuted;
    }

    public boolean isEvidenceFound() {
        return evidenceFound;
    }

    public void setEvidenceFound(boolean evidenceFound) {
        this.evidenceFound = evidenceFound;
    }

    public int getArtifactCount() {
        return artifactCount;
    }

    public void setArtifactCount(int artifactCount) {
        this.artifactCount = artifactCount;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}


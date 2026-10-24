package com.emailforensics.backend.dto.evidence;

import com.emailforensics.backend.evidence.AnalysisStatus;

import java.time.Instant;
import java.util.UUID;

public class EvidenceStatusResponse {

    private UUID emailId;
    private UUID analysisRunId;
    private AnalysisStatus status;
    private String currentStage;
    private String pipelineVersion;
    private Integer attemptNumber;
    private Instant startedAt;
    private Instant completedAt;
    private String errorCode;
    private String safeErrorMessage;

    public EvidenceStatusResponse() {
    }

    public EvidenceStatusResponse(UUID emailId, UUID analysisRunId, AnalysisStatus status,
                                  String currentStage, String pipelineVersion, Integer attemptNumber,
                                  Instant startedAt, Instant completedAt, String errorCode,
                                  String safeErrorMessage) {
        this.emailId = emailId;
        this.analysisRunId = analysisRunId;
        this.status = status;
        this.currentStage = currentStage;
        this.pipelineVersion = pipelineVersion;
        this.attemptNumber = attemptNumber;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.errorCode = errorCode;
        this.safeErrorMessage = safeErrorMessage;
    }

    public UUID getEmailId() {
        return emailId;
    }

    public void setEmailId(UUID emailId) {
        this.emailId = emailId;
    }

    public UUID getAnalysisRunId() {
        return analysisRunId;
    }

    public void setAnalysisRunId(UUID analysisRunId) {
        this.analysisRunId = analysisRunId;
    }

    public AnalysisStatus getStatus() {
        return status;
    }

    public void setStatus(AnalysisStatus status) {
        this.status = status;
    }

    public String getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(String currentStage) {
        this.currentStage = currentStage;
    }

    public String getPipelineVersion() {
        return pipelineVersion;
    }

    public void setPipelineVersion(String pipelineVersion) {
        this.pipelineVersion = pipelineVersion;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getSafeErrorMessage() {
        return safeErrorMessage;
    }

    public void setSafeErrorMessage(String safeErrorMessage) {
        this.safeErrorMessage = safeErrorMessage;
    }
}

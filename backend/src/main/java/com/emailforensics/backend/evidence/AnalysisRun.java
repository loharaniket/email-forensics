package com.emailforensics.backend.evidence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analysis_runs")
public class AnalysisRun {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "email_id", nullable = false)
    private EmailEvidence emailEvidence;

    @Column(name = "pipeline_version", nullable = false, length = 40)
    private String pipelineVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AnalysisStatus status;

    @Column(name = "current_stage", length = 80)
    private String currentStage;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "safe_error_message", columnDefinition = "TEXT")
    private String safeErrorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public AnalysisRun() {
    }

    public AnalysisRun(UUID id, EmailEvidence emailEvidence, String pipelineVersion,
                       AnalysisStatus status, String currentStage) {
        this.id = id;
        this.emailEvidence = emailEvidence;
        this.pipelineVersion = pipelineVersion;
        this.status = status;
        this.currentStage = currentStage;
        this.attemptNumber = 1;
        this.createdAt = Instant.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.attemptNumber == null) {
            this.attemptNumber = 1;
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

    public EmailEvidence getEmailEvidence() {
        return emailEvidence;
    }

    public void setEmailEvidence(EmailEvidence emailEvidence) {
        this.emailEvidence = emailEvidence;
    }

    public String getPipelineVersion() {
        return pipelineVersion;
    }

    public void setPipelineVersion(String pipelineVersion) {
        this.pipelineVersion = pipelineVersion;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

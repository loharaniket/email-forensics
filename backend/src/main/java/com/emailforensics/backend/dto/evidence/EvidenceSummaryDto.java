package com.emailforensics.backend.dto.evidence;

import com.emailforensics.backend.evidence.AnalysisStatus;

import java.time.Instant;
import java.util.UUID;

public class EvidenceSummaryDto {

    private UUID id;
    private UUID caseId;
    private String originalFilename;
    private Long fileSizeBytes;
    private String sha256Hash;
    private String subject;
    private String messageId;
    private Instant sentDate;
    private String uploadedByUsername;
    private Instant uploadedAt;
    private AnalysisStatus latestStatus;
    private String currentStage;

    public EvidenceSummaryDto() {
    }

    public EvidenceSummaryDto(UUID id, UUID caseId, String originalFilename, Long fileSizeBytes,
                              String sha256Hash, String subject, String messageId,
                              Instant sentDate, String uploadedByUsername, Instant uploadedAt,
                              AnalysisStatus latestStatus, String currentStage) {
        this.id = id;
        this.caseId = caseId;
        this.originalFilename = originalFilename;
        this.fileSizeBytes = fileSizeBytes;
        this.sha256Hash = sha256Hash;
        this.subject = subject;
        this.messageId = messageId;
        this.sentDate = sentDate;
        this.uploadedByUsername = uploadedByUsername;
        this.uploadedAt = uploadedAt;
        this.latestStatus = latestStatus;
        this.currentStage = currentStage;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    public void setSha256Hash(String sha256Hash) {
        this.sha256Hash = sha256Hash;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public Instant getSentDate() {
        return sentDate;
    }

    public void setSentDate(Instant sentDate) {
        this.sentDate = sentDate;
    }

    public String getUploadedByUsername() {
        return uploadedByUsername;
    }

    public void setUploadedByUsername(String uploadedByUsername) {
        this.uploadedByUsername = uploadedByUsername;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public AnalysisStatus getLatestStatus() {
        return latestStatus;
    }

    public void setLatestStatus(AnalysisStatus latestStatus) {
        this.latestStatus = latestStatus;
    }

    public String getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(String currentStage) {
        this.currentStage = currentStage;
    }
}

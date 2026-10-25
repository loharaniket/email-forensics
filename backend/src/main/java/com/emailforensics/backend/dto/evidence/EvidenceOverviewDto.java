package com.emailforensics.backend.dto.evidence;

import com.emailforensics.backend.evidence.AnalysisStatus;

import java.time.Instant;
import java.util.UUID;

public class EvidenceOverviewDto {

    private UUID emailId;
    private UUID caseId;
    private String caseNumber;
    private String originalFilename;
    private Long fileSizeBytes;
    private String sha256Hash;
    private String subject;
    private String messageId;
    private Instant sentDate;
    private Instant uploadedAt;
    private String uploadedByUsername;
    private AnalysisStatus processingStatus;
    private String currentStage;
    private Integer riskScore;
    private String riskLevel;
    private Integer confidenceScore;

    public EvidenceOverviewDto() {
    }

    public EvidenceOverviewDto(UUID emailId, UUID caseId, String caseNumber, String originalFilename,
                               Long fileSizeBytes, String sha256Hash, String subject,
                               String messageId, Instant sentDate, Instant uploadedAt,
                               String uploadedByUsername, AnalysisStatus processingStatus,
                               String currentStage) {
        this(emailId, caseId, caseNumber, originalFilename, fileSizeBytes, sha256Hash, subject,
                messageId, sentDate, uploadedAt, uploadedByUsername, processingStatus, currentStage,
                null, null, null);
    }

    public EvidenceOverviewDto(UUID emailId, UUID caseId, String caseNumber, String originalFilename,
                               Long fileSizeBytes, String sha256Hash, String subject,
                               String messageId, Instant sentDate, Instant uploadedAt,
                               String uploadedByUsername, AnalysisStatus processingStatus,
                               String currentStage, Integer riskScore, String riskLevel,
                               Integer confidenceScore) {
        this.emailId = emailId;
        this.caseId = caseId;
        this.caseNumber = caseNumber;
        this.originalFilename = originalFilename;
        this.fileSizeBytes = fileSizeBytes;
        this.sha256Hash = sha256Hash;
        this.subject = subject;
        this.messageId = messageId;
        this.sentDate = sentDate;
        this.uploadedAt = uploadedAt;
        this.uploadedByUsername = uploadedByUsername;
        this.processingStatus = processingStatus;
        this.currentStage = currentStage;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.confidenceScore = confidenceScore;
    }

    public UUID getEmailId() {
        return emailId;
    }

    public void setEmailId(UUID emailId) {
        this.emailId = emailId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public String getCaseNumber() {
        return caseNumber;
    }

    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
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

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getUploadedByUsername() {
        return uploadedByUsername;
    }

    public void setUploadedByUsername(String uploadedByUsername) {
        this.uploadedByUsername = uploadedByUsername;
    }

    public AnalysisStatus getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(AnalysisStatus processingStatus) {
        this.processingStatus = processingStatus;
    }

    public String getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(String currentStage) {
        this.currentStage = currentStage;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Integer getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Integer confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
}

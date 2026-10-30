package com.emailforensics.backend.dto.evidence;

import java.util.UUID;

public class EvidenceUploadResponse {

    private UUID emailId;
    private UUID analysisRunId;
    private String status;
    private String statusUrl;
    private String sha256Hash;
    private String originalFilename;
    private Long fileSizeBytes;
    private boolean duplicate;
    private UUID existingCaseId;
    private String existingCaseNumber;
    private String existingCaseTitle;
    private String message;

    public EvidenceUploadResponse() {
    }

    public EvidenceUploadResponse(UUID emailId, UUID analysisRunId, String status, String statusUrl,
                                  String sha256Hash, String originalFilename, Long fileSizeBytes) {
        this(emailId, analysisRunId, status, statusUrl, sha256Hash, originalFilename, fileSizeBytes,
                false, null, null, null, null);
    }

    public EvidenceUploadResponse(UUID emailId, UUID analysisRunId, String status, String statusUrl,
                                  String sha256Hash, String originalFilename, Long fileSizeBytes,
                                  boolean duplicate, UUID existingCaseId, String existingCaseNumber,
                                  String message) {
        this(emailId, analysisRunId, status, statusUrl, sha256Hash, originalFilename, fileSizeBytes,
                duplicate, existingCaseId, existingCaseNumber, null, message);
    }

    public EvidenceUploadResponse(UUID emailId, UUID analysisRunId, String status, String statusUrl,
                                  String sha256Hash, String originalFilename, Long fileSizeBytes,
                                  boolean duplicate, UUID existingCaseId, String existingCaseNumber,
                                  String existingCaseTitle, String message) {
        this.emailId = emailId;
        this.analysisRunId = analysisRunId;
        this.status = status;
        this.statusUrl = statusUrl;
        this.sha256Hash = sha256Hash;
        this.originalFilename = originalFilename;
        this.fileSizeBytes = fileSizeBytes;
        this.duplicate = duplicate;
        this.existingCaseId = existingCaseId;
        this.existingCaseNumber = existingCaseNumber;
        this.existingCaseTitle = existingCaseTitle;
        this.message = message;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusUrl() {
        return statusUrl;
    }

    public void setStatusUrl(String statusUrl) {
        this.statusUrl = statusUrl;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    public void setSha256Hash(String sha256Hash) {
        this.sha256Hash = sha256Hash;
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

    public boolean isDuplicate() {
        return duplicate;
    }

    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }

    public UUID getExistingCaseId() {
        return existingCaseId;
    }

    public void setExistingCaseId(UUID existingCaseId) {
        this.existingCaseId = existingCaseId;
    }

    public String getExistingCaseNumber() {
        return existingCaseNumber;
    }

    public void setExistingCaseNumber(String existingCaseNumber) {
        this.existingCaseNumber = existingCaseNumber;
    }

    public String getExistingCaseTitle() {
        return existingCaseTitle;
    }

    public void setExistingCaseTitle(String existingCaseTitle) {
        this.existingCaseTitle = existingCaseTitle;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

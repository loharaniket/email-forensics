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

    public EvidenceUploadResponse() {
    }

    public EvidenceUploadResponse(UUID emailId, UUID analysisRunId, String status, String statusUrl,
                                  String sha256Hash, String originalFilename, Long fileSizeBytes) {
        this.emailId = emailId;
        this.analysisRunId = analysisRunId;
        this.status = status;
        this.statusUrl = statusUrl;
        this.sha256Hash = sha256Hash;
        this.originalFilename = originalFilename;
        this.fileSizeBytes = fileSizeBytes;
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
}

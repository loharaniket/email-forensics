package com.emailforensics.backend.dto.email;

import com.emailforensics.backend.evidence.AnalysisStatus;

import java.time.Instant;
import java.util.UUID;

public class InvestigationDetailDto {

    private UUID evidenceId;
    private UUID caseId;
    private String caseNumber;
    private String caseTitle;
    private String originalFilename;
    private Long fileSizeBytes;
    private String sha256Hash;
    private Instant uploadedAt;
    private String uploadedByUsername;
    private AnalysisStatus pipelineStatus;
    private String currentStage;
    private ParsedEmailDto parsedEmail;

    public InvestigationDetailDto() {
    }

    public InvestigationDetailDto(UUID evidenceId, UUID caseId, String caseNumber, String caseTitle,
                                  String originalFilename, Long fileSizeBytes, String sha256Hash,
                                  Instant uploadedAt, String uploadedByUsername, AnalysisStatus pipelineStatus,
                                  String currentStage, ParsedEmailDto parsedEmail) {
        this.evidenceId = evidenceId;
        this.caseId = caseId;
        this.caseNumber = caseNumber;
        this.caseTitle = caseTitle;
        this.originalFilename = originalFilename;
        this.fileSizeBytes = fileSizeBytes;
        this.sha256Hash = sha256Hash;
        this.uploadedAt = uploadedAt;
        this.uploadedByUsername = uploadedByUsername;
        this.pipelineStatus = pipelineStatus;
        this.currentStage = currentStage;
        this.parsedEmail = parsedEmail;
    }

    public UUID getEvidenceId() {
        return evidenceId;
    }

    public void setEvidenceId(UUID evidenceId) {
        this.evidenceId = evidenceId;
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

    public String getCaseTitle() {
        return caseTitle;
    }

    public void setCaseTitle(String caseTitle) {
        this.caseTitle = caseTitle;
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

    public AnalysisStatus getPipelineStatus() {
        return pipelineStatus;
    }

    public void setPipelineStatus(AnalysisStatus pipelineStatus) {
        this.pipelineStatus = pipelineStatus;
    }

    public String getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(String currentStage) {
        this.currentStage = currentStage;
    }

    public ParsedEmailDto getParsedEmail() {
        return parsedEmail;
    }

    public void setParsedEmail(ParsedEmailDto parsedEmail) {
        this.parsedEmail = parsedEmail;
    }
}

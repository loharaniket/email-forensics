package com.emailforensics.backend.dto.campaign;

import java.time.Instant;
import java.util.UUID;

public class CampaignMemberDto {

    private UUID id;
    private UUID emailId;
    private UUID caseId;
    private String caseNumber;
    private String caseTitle;
    private String subject;
    private String filename;
    private String membershipReason;
    private Integer matchScore;
    private Integer riskScore;
    private String riskLevel;
    private Instant uploadedAt;

    public CampaignMemberDto() {
    }

    public CampaignMemberDto(UUID id, UUID emailId, UUID caseId, String caseNumber,
                             String caseTitle, String subject, String filename,
                             String membershipReason, Integer matchScore,
                             Integer riskScore, String riskLevel, Instant uploadedAt) {
        this.id = id;
        this.emailId = emailId;
        this.caseId = caseId;
        this.caseNumber = caseNumber;
        this.caseTitle = caseTitle;
        this.subject = subject;
        this.filename = filename;
        this.membershipReason = membershipReason;
        this.matchScore = matchScore;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.uploadedAt = uploadedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getCaseTitle() {
        return caseTitle;
    }

    public void setCaseTitle(String caseTitle) {
        this.caseTitle = caseTitle;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getMembershipReason() {
        return membershipReason;
    }

    public void setMembershipReason(String membershipReason) {
        this.membershipReason = membershipReason;
    }

    public Integer getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Integer matchScore) {
        this.matchScore = matchScore;
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

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}

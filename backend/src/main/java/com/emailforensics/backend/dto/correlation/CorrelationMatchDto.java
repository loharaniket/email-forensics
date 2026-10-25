package com.emailforensics.backend.dto.correlation;

import java.time.Instant;
import java.util.UUID;

public class CorrelationMatchDto {

    private UUID id;
    private UUID sourceRunId;
    private UUID sourceEmailId;
    private UUID matchedEmailId;
    private UUID matchedCaseId;
    private String matchedCaseNumber;
    private String matchedCaseTitle;
    private String matchedEmailSubject;
    private String matchedEmailFilename;
    private Instant matchedSentDate;
    private Instant matchedUploadedAt;
    private String indicatorType;
    private String normalizedIndicatorValue;
    private String matchStrength;
    private String matchExplanation;
    private Instant createdAt;

    public CorrelationMatchDto() {
    }

    public CorrelationMatchDto(UUID id, UUID sourceRunId, UUID sourceEmailId, UUID matchedEmailId,
                               UUID matchedCaseId, String matchedCaseNumber, String matchedCaseTitle,
                               String matchedEmailSubject, String matchedEmailFilename,
                               Instant matchedSentDate, Instant matchedUploadedAt,
                               String indicatorType, String normalizedIndicatorValue,
                               String matchStrength, String matchExplanation, Instant createdAt) {
        this.id = id;
        this.sourceRunId = sourceRunId;
        this.sourceEmailId = sourceEmailId;
        this.matchedEmailId = matchedEmailId;
        this.matchedCaseId = matchedCaseId;
        this.matchedCaseNumber = matchedCaseNumber;
        this.matchedCaseTitle = matchedCaseTitle;
        this.matchedEmailSubject = matchedEmailSubject;
        this.matchedEmailFilename = matchedEmailFilename;
        this.matchedSentDate = matchedSentDate;
        this.matchedUploadedAt = matchedUploadedAt;
        this.indicatorType = indicatorType;
        this.normalizedIndicatorValue = normalizedIndicatorValue;
        this.matchStrength = matchStrength;
        this.matchExplanation = matchExplanation;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSourceRunId() {
        return sourceRunId;
    }

    public void setSourceRunId(UUID sourceRunId) {
        this.sourceRunId = sourceRunId;
    }

    public UUID getSourceEmailId() {
        return sourceEmailId;
    }

    public void setSourceEmailId(UUID sourceEmailId) {
        this.sourceEmailId = sourceEmailId;
    }

    public UUID getMatchedEmailId() {
        return matchedEmailId;
    }

    public void setMatchedEmailId(UUID matchedEmailId) {
        this.matchedEmailId = matchedEmailId;
    }

    public UUID getMatchedCaseId() {
        return matchedCaseId;
    }

    public void setMatchedCaseId(UUID matchedCaseId) {
        this.matchedCaseId = matchedCaseId;
    }

    public String getMatchedCaseNumber() {
        return matchedCaseNumber;
    }

    public void setMatchedCaseNumber(String matchedCaseNumber) {
        this.matchedCaseNumber = matchedCaseNumber;
    }

    public String getMatchedCaseTitle() {
        return matchedCaseTitle;
    }

    public void setMatchedCaseTitle(String matchedCaseTitle) {
        this.matchedCaseTitle = matchedCaseTitle;
    }

    public String getMatchedEmailSubject() {
        return matchedEmailSubject;
    }

    public void setMatchedEmailSubject(String matchedEmailSubject) {
        this.matchedEmailSubject = matchedEmailSubject;
    }

    public String getMatchedEmailFilename() {
        return matchedEmailFilename;
    }

    public void setMatchedEmailFilename(String matchedEmailFilename) {
        this.matchedEmailFilename = matchedEmailFilename;
    }

    public Instant getMatchedSentDate() {
        return matchedSentDate;
    }

    public void setMatchedSentDate(Instant matchedSentDate) {
        this.matchedSentDate = matchedSentDate;
    }

    public Instant getMatchedUploadedAt() {
        return matchedUploadedAt;
    }

    public void setMatchedUploadedAt(Instant matchedUploadedAt) {
        this.matchedUploadedAt = matchedUploadedAt;
    }

    public String getIndicatorType() {
        return indicatorType;
    }

    public void setIndicatorType(String indicatorType) {
        this.indicatorType = indicatorType;
    }

    public String getNormalizedIndicatorValue() {
        return normalizedIndicatorValue;
    }

    public void setNormalizedIndicatorValue(String normalizedIndicatorValue) {
        this.normalizedIndicatorValue = normalizedIndicatorValue;
    }

    public String getMatchStrength() {
        return matchStrength;
    }

    public void setMatchStrength(String matchStrength) {
        this.matchStrength = matchStrength;
    }

    public String getMatchExplanation() {
        return matchExplanation;
    }

    public void setMatchExplanation(String matchExplanation) {
        this.matchExplanation = matchExplanation;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

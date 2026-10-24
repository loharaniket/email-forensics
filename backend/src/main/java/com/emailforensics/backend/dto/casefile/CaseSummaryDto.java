package com.emailforensics.backend.dto.casefile;

import com.emailforensics.backend.casefile.CaseStatus;

import java.time.Instant;
import java.util.UUID;

public class CaseSummaryDto {

    private UUID id;
    private String caseNumber;
    private String title;
    private String description;
    private CaseStatus status;
    private String createdByUsername;
    private Instant createdAt;
    private Instant updatedAt;

    public CaseSummaryDto() {
    }

    public CaseSummaryDto(UUID id, String caseNumber, String title, String description, CaseStatus status, String createdByUsername, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.caseNumber = caseNumber;
        this.title = title;
        this.description = description;
        this.status = status;
        this.createdByUsername = createdByUsername;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCaseNumber() {
        return caseNumber;
    }

    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CaseStatus getStatus() {
        return status;
    }

    public void setStatus(CaseStatus status) {
        this.status = status;
    }

    public String getCreatedByUsername() {
        return createdByUsername;
    }

    public void setCreatedByUsername(String createdByUsername) {
        this.createdByUsername = createdByUsername;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

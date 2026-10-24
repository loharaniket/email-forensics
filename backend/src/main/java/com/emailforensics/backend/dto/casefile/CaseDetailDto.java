package com.emailforensics.backend.dto.casefile;

import com.emailforensics.backend.casefile.CaseStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CaseDetailDto {

    private UUID id;
    private String caseNumber;
    private String title;
    private String description;
    private CaseStatus status;
    private String analystNotes;
    private String createdByUsername;
    private Instant createdAt;
    private Instant updatedAt;
    private List<Object> evidenceSummaries = new ArrayList<>();

    public CaseDetailDto() {
    }

    public CaseDetailDto(UUID id, String caseNumber, String title, String description, CaseStatus status, String analystNotes, String createdByUsername, Instant createdAt, Instant updatedAt, List<Object> evidenceSummaries) {
        this.id = id;
        this.caseNumber = caseNumber;
        this.title = title;
        this.description = description;
        this.status = status;
        this.analystNotes = analystNotes;
        this.createdByUsername = createdByUsername;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.evidenceSummaries = evidenceSummaries != null ? evidenceSummaries : new ArrayList<>();
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

    public String getAnalystNotes() {
        return analystNotes;
    }

    public void setAnalystNotes(String analystNotes) {
        this.analystNotes = analystNotes;
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

    public List<Object> getEvidenceSummaries() {
        return evidenceSummaries;
    }

    public void setEvidenceSummaries(List<Object> evidenceSummaries) {
        this.evidenceSummaries = evidenceSummaries;
    }
}

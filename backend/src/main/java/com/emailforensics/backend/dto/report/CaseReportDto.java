package com.emailforensics.backend.dto.report;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CaseReportDto {

    private String reportId;
    private UUID caseId;
    private String caseNumber;
    private String title;
    private String description;
    private String status;
    private String analystNotes;
    private String generatedBy;
    private Instant generatedAt;
    private int totalEvidenceCount;
    private int highRiskCount;
    private int criticalRiskCount;
    private List<ForensicReportDto> evidenceReports = new ArrayList<>();

    public CaseReportDto() {
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAnalystNotes() {
        return analystNotes;
    }

    public void setAnalystNotes(String analystNotes) {
        this.analystNotes = analystNotes;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(String generatedBy) {
        this.generatedBy = generatedBy;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public int getTotalEvidenceCount() {
        return totalEvidenceCount;
    }

    public void setTotalEvidenceCount(int totalEvidenceCount) {
        this.totalEvidenceCount = totalEvidenceCount;
    }

    public int getHighRiskCount() {
        return highRiskCount;
    }

    public void setHighRiskCount(int highRiskCount) {
        this.highRiskCount = highRiskCount;
    }

    public int getCriticalRiskCount() {
        return criticalRiskCount;
    }

    public void setCriticalRiskCount(int criticalRiskCount) {
        this.criticalRiskCount = criticalRiskCount;
    }

    public List<ForensicReportDto> getEvidenceReports() {
        return evidenceReports;
    }

    public void setEvidenceReports(List<ForensicReportDto> evidenceReports) {
        this.evidenceReports = evidenceReports;
    }
}

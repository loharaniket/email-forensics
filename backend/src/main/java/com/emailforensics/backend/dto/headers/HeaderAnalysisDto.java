package com.emailforensics.backend.dto.headers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HeaderAnalysisDto {

    private UUID analysisRunId;
    private String fromAddress;
    private String fromDomain;
    private String replyTo;
    private String returnPath;
    private String dkimDomain;
    private String reportedSpf;
    private String reportedDkim;
    private String reportedDmarc;
    private String reportedArc;
    private List<String> identityMismatchFlags = new ArrayList<>();
    private String verificationNote;
    private Map<String, List<String>> rawHeaders = new LinkedHashMap<>();
    private Integer totalHopsCount = 0;

    private String activeSpfResult;
    private String activeSpfRecord;
    private String activeSpfRelayIp;
    private String activeDkimStatus;
    private String activeDkimRecord;
    private String activeDkimSelector;
    private String activeDmarcStatus;
    private String activeDmarcPolicy;
    private String activeVerificationSummary;

    public HeaderAnalysisDto() {
    }

    public HeaderAnalysisDto(UUID analysisRunId, String fromAddress, String fromDomain,
                             String replyTo, String returnPath, String dkimDomain,
                             String reportedSpf, String reportedDkim, String reportedDmarc,
                             String reportedArc, List<String> identityMismatchFlags,
                             String verificationNote, Map<String, List<String>> rawHeaders,
                             Integer totalHopsCount) {
        this.analysisRunId = analysisRunId;
        this.fromAddress = fromAddress;
        this.fromDomain = fromDomain;
        this.replyTo = replyTo;
        this.returnPath = returnPath;
        this.dkimDomain = dkimDomain;
        this.reportedSpf = reportedSpf;
        this.reportedDkim = reportedDkim;
        this.reportedDmarc = reportedDmarc;
        this.reportedArc = reportedArc;
        this.identityMismatchFlags = identityMismatchFlags != null ? identityMismatchFlags : new ArrayList<>();
        this.verificationNote = verificationNote;
        this.rawHeaders = rawHeaders != null ? rawHeaders : new LinkedHashMap<>();
        this.totalHopsCount = totalHopsCount != null ? totalHopsCount : 0;
    }

    public UUID getAnalysisRunId() {
        return analysisRunId;
    }

    public void setAnalysisRunId(UUID analysisRunId) {
        this.analysisRunId = analysisRunId;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getFromDomain() {
        return fromDomain;
    }

    public void setFromDomain(String fromDomain) {
        this.fromDomain = fromDomain;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public String getReturnPath() {
        return returnPath;
    }

    public void setReturnPath(String returnPath) {
        this.returnPath = returnPath;
    }

    public String getDkimDomain() {
        return dkimDomain;
    }

    public void setDkimDomain(String dkimDomain) {
        this.dkimDomain = dkimDomain;
    }

    public String getReportedSpf() {
        return reportedSpf;
    }

    public void setReportedSpf(String reportedSpf) {
        this.reportedSpf = reportedSpf;
    }

    public String getReportedDkim() {
        return reportedDkim;
    }

    public void setReportedDkim(String reportedDkim) {
        this.reportedDkim = reportedDkim;
    }

    public String getReportedDmarc() {
        return reportedDmarc;
    }

    public void setReportedDmarc(String reportedDmarc) {
        this.reportedDmarc = reportedDmarc;
    }

    public String getReportedArc() {
        return reportedArc;
    }

    public void setReportedArc(String reportedArc) {
        this.reportedArc = reportedArc;
    }

    public List<String> getIdentityMismatchFlags() {
        return identityMismatchFlags;
    }

    public void setIdentityMismatchFlags(List<String> identityMismatchFlags) {
        this.identityMismatchFlags = identityMismatchFlags;
    }

    public String getVerificationNote() {
        return verificationNote;
    }

    public void setVerificationNote(String verificationNote) {
        this.verificationNote = verificationNote;
    }

    public Map<String, List<String>> getRawHeaders() {
        return rawHeaders;
    }

    public void setRawHeaders(Map<String, List<String>> rawHeaders) {
        this.rawHeaders = rawHeaders;
    }

    public Integer getTotalHopsCount() {
        return totalHopsCount;
    }

    public void setTotalHopsCount(Integer totalHopsCount) {
        this.totalHopsCount = totalHopsCount;
    }

    public String getActiveSpfResult() {
        return activeSpfResult;
    }

    public void setActiveSpfResult(String activeSpfResult) {
        this.activeSpfResult = activeSpfResult;
    }

    public String getActiveSpfRecord() {
        return activeSpfRecord;
    }

    public void setActiveSpfRecord(String activeSpfRecord) {
        this.activeSpfRecord = activeSpfRecord;
    }

    public String getActiveSpfRelayIp() {
        return activeSpfRelayIp;
    }

    public void setActiveSpfRelayIp(String activeSpfRelayIp) {
        this.activeSpfRelayIp = activeSpfRelayIp;
    }

    public String getActiveDkimStatus() {
        return activeDkimStatus;
    }

    public void setActiveDkimStatus(String activeDkimStatus) {
        this.activeDkimStatus = activeDkimStatus;
    }

    public String getActiveDkimRecord() {
        return activeDkimRecord;
    }

    public void setActiveDkimRecord(String activeDkimRecord) {
        this.activeDkimRecord = activeDkimRecord;
    }

    public String getActiveDkimSelector() {
        return activeDkimSelector;
    }

    public void setActiveDkimSelector(String activeDkimSelector) {
        this.activeDkimSelector = activeDkimSelector;
    }

    public String getActiveDmarcStatus() {
        return activeDmarcStatus;
    }

    public void setActiveDmarcStatus(String activeDmarcStatus) {
        this.activeDmarcStatus = activeDmarcStatus;
    }

    public String getActiveDmarcPolicy() {
        return activeDmarcPolicy;
    }

    public void setActiveDmarcPolicy(String activeDmarcPolicy) {
        this.activeDmarcPolicy = activeDmarcPolicy;
    }

    public String getActiveVerificationSummary() {
        return activeVerificationSummary;
    }

    public void setActiveVerificationSummary(String activeVerificationSummary) {
        this.activeVerificationSummary = activeVerificationSummary;
    }
}

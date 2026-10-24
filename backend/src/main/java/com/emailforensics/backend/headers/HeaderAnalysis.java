package com.emailforensics.backend.headers;

import com.emailforensics.backend.evidence.AnalysisRun;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "header_analysis")
public class HeaderAnalysis {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_run_id", nullable = false, unique = true)
    private AnalysisRun analysisRun;

    @Column(name = "from_address", length = 254)
    private String fromAddress;

    @Column(name = "from_domain", length = 255)
    private String fromDomain;

    @Column(name = "reply_to", length = 254)
    private String replyTo;

    @Column(name = "return_path", length = 254)
    private String returnPath;

    @Column(name = "dkim_domain", length = 255)
    private String dkimDomain;

    @Column(name = "reported_spf", length = 40)
    private String reportedSpf;

    @Column(name = "reported_dkim", length = 40)
    private String reportedDkim;

    @Column(name = "reported_dmarc", length = 40)
    private String reportedDmarc;

    @Column(name = "reported_arc", length = 40)
    private String reportedArc;

    @Column(name = "current_dns_observation", columnDefinition = "TEXT")
    private String currentDnsObservation;

    @Column(name = "independent_verification", columnDefinition = "TEXT")
    private String independentVerification;

    @Column(name = "identity_mismatch_flags", columnDefinition = "TEXT")
    private String identityMismatchFlags;

    @Column(name = "raw_headers_json", columnDefinition = "TEXT")
    private String rawHeadersJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public HeaderAnalysis() {
    }

    public HeaderAnalysis(UUID id, AnalysisRun analysisRun, String fromAddress, String fromDomain,
                          String replyTo, String returnPath, String dkimDomain,
                          String reportedSpf, String reportedDkim, String reportedDmarc, String reportedArc,
                          String identityMismatchFlags, String rawHeadersJson) {
        this.id = id;
        this.analysisRun = analysisRun;
        this.fromAddress = fromAddress;
        this.fromDomain = fromDomain;
        this.replyTo = replyTo;
        this.returnPath = returnPath;
        this.dkimDomain = dkimDomain;
        this.reportedSpf = reportedSpf;
        this.reportedDkim = reportedDkim;
        this.reportedDmarc = reportedDmarc;
        this.reportedArc = reportedArc;
        this.identityMismatchFlags = identityMismatchFlags;
        this.rawHeadersJson = rawHeadersJson;
        this.createdAt = Instant.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public AnalysisRun getAnalysisRun() {
        return analysisRun;
    }

    public void setAnalysisRun(AnalysisRun analysisRun) {
        this.analysisRun = analysisRun;
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

    public String getCurrentDnsObservation() {
        return currentDnsObservation;
    }

    public void setCurrentDnsObservation(String currentDnsObservation) {
        this.currentDnsObservation = currentDnsObservation;
    }

    public String getIndependentVerification() {
        return independentVerification;
    }

    public void setIndependentVerification(String independentVerification) {
        this.independentVerification = independentVerification;
    }

    public String getIdentityMismatchFlags() {
        return identityMismatchFlags;
    }

    public void setIdentityMismatchFlags(String identityMismatchFlags) {
        this.identityMismatchFlags = identityMismatchFlags;
    }

    public String getRawHeadersJson() {
        return rawHeadersJson;
    }

    public void setRawHeadersJson(String rawHeadersJson) {
        this.rawHeadersJson = rawHeadersJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

package com.emailforensics.backend.urlintel;

import com.emailforensics.backend.evidence.AnalysisRun;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "extracted_urls")
public class ExtractedUrl {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_run_id", nullable = false)
    private AnalysisRun analysisRun;

    @Column(name = "raw_url", nullable = false, columnDefinition = "TEXT")
    private String rawUrl;

    @Column(name = "normalized_url", nullable = false, columnDefinition = "TEXT")
    private String normalizedUrl;

    @Column(name = "domain", length = 255)
    private String domain;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "anchor_text", columnDefinition = "TEXT")
    private String anchorText;

    @Column(name = "anchor_mismatch", nullable = false)
    private Boolean anchorMismatch = false;

    @Column(name = "is_ip_url", nullable = false)
    private Boolean isIpUrl = false;

    @Column(name = "is_shortened", nullable = false)
    private Boolean isShortened = false;

    @Column(name = "unusual_port", nullable = false)
    private Boolean unusualPort = false;

    @Column(name = "subdomain_excess", nullable = false)
    private Boolean subdomainExcess = false;

    @Column(name = "lookalike", nullable = false)
    private Boolean lookalike = false;

    @Column(name = "dns_status", length = 50)
    private String dnsStatus;

    @Column(name = "url_risk_score")
    private Integer urlRiskScore;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public ExtractedUrl() {
    }

    public ExtractedUrl(UUID id, AnalysisRun analysisRun, String rawUrl, String normalizedUrl,
                        String domain, String sourceType, String anchorText, Boolean anchorMismatch,
                        Boolean isIpUrl, Boolean isShortened, Boolean unusualPort,
                        Boolean subdomainExcess, Boolean lookalike, String dnsStatus,
                        Integer urlRiskScore) {
        this.id = id;
        this.analysisRun = analysisRun;
        this.rawUrl = rawUrl;
        this.normalizedUrl = normalizedUrl;
        this.domain = domain;
        this.sourceType = sourceType;
        this.anchorText = anchorText;
        this.anchorMismatch = anchorMismatch != null ? anchorMismatch : false;
        this.isIpUrl = isIpUrl != null ? isIpUrl : false;
        this.isShortened = isShortened != null ? isShortened : false;
        this.unusualPort = unusualPort != null ? unusualPort : false;
        this.subdomainExcess = subdomainExcess != null ? subdomainExcess : false;
        this.lookalike = lookalike != null ? lookalike : false;
        this.dnsStatus = dnsStatus;
        this.urlRiskScore = urlRiskScore;
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

    public String getRawUrl() {
        return rawUrl;
    }

    public void setRawUrl(String rawUrl) {
        this.rawUrl = rawUrl;
    }

    public String getNormalizedUrl() {
        return normalizedUrl;
    }

    public void setNormalizedUrl(String normalizedUrl) {
        this.normalizedUrl = normalizedUrl;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getAnchorText() {
        return anchorText;
    }

    public void setAnchorText(String anchorText) {
        this.anchorText = anchorText;
    }

    public Boolean getAnchorMismatch() {
        return anchorMismatch;
    }

    public void setAnchorMismatch(Boolean anchorMismatch) {
        this.anchorMismatch = anchorMismatch;
    }

    public Boolean getIsIpUrl() {
        return isIpUrl;
    }

    public void setIsIpUrl(Boolean isIpUrl) {
        this.isIpUrl = isIpUrl;
    }

    public Boolean getIsShortened() {
        return isShortened;
    }

    public void setIsShortened(Boolean isShortened) {
        this.isShortened = isShortened;
    }

    public Boolean getUnusualPort() {
        return unusualPort;
    }

    public void setUnusualPort(Boolean unusualPort) {
        this.unusualPort = unusualPort;
    }

    public Boolean getSubdomainExcess() {
        return subdomainExcess;
    }

    public void setSubdomainExcess(Boolean subdomainExcess) {
        this.subdomainExcess = subdomainExcess;
    }

    public Boolean getLookalike() {
        return lookalike;
    }

    public void setLookalike(Boolean lookalike) {
        this.lookalike = lookalike;
    }

    public String getDnsStatus() {
        return dnsStatus;
    }

    public void setDnsStatus(String dnsStatus) {
        this.dnsStatus = dnsStatus;
    }

    public Integer getUrlRiskScore() {
        return urlRiskScore;
    }

    public void setUrlRiskScore(Integer urlRiskScore) {
        this.urlRiskScore = urlRiskScore;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

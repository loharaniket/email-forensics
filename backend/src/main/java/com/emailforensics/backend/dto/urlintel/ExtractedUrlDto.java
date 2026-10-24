package com.emailforensics.backend.dto.urlintel;

import java.util.UUID;

public class ExtractedUrlDto {

    private UUID id;
    private String rawUrl;
    private String normalizedUrl;
    private String domain;
    private String sourceType;
    private String anchorText;
    private Boolean anchorMismatch;
    private Boolean isIpUrl;
    private Boolean isShortened;
    private Boolean unusualPort;
    private Boolean subdomainExcess;
    private Boolean lookalike;
    private String dnsStatus;
    private Integer urlRiskScore;

    public ExtractedUrlDto() {
    }

    public ExtractedUrlDto(UUID id, String rawUrl, String normalizedUrl, String domain,
                           String sourceType, String anchorText, Boolean anchorMismatch,
                           Boolean isIpUrl, Boolean isShortened, Boolean unusualPort,
                           Boolean subdomainExcess, Boolean lookalike, String dnsStatus,
                           Integer urlRiskScore) {
        this.id = id;
        this.rawUrl = rawUrl;
        this.normalizedUrl = normalizedUrl;
        this.domain = domain;
        this.sourceType = sourceType;
        this.anchorText = anchorText;
        this.anchorMismatch = anchorMismatch;
        this.isIpUrl = isIpUrl;
        this.isShortened = isShortened;
        this.unusualPort = unusualPort;
        this.subdomainExcess = subdomainExcess;
        this.lookalike = lookalike;
        this.dnsStatus = dnsStatus;
        this.urlRiskScore = urlRiskScore;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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
}

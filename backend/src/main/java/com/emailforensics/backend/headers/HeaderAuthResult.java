package com.emailforensics.backend.headers;

import com.emailforensics.backend.dto.headers.ReceivedHopDto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple data holder for email authentication results from external APIs.
 * Stores normalized verdicts (SPF, DKIM, DMARC, ARC), routing hops, and API status.
 */
public class HeaderAuthResult {

    private String providerName = "UNAVAILABLE";
    private String status = "ALL_PROVIDERS_UNAVAILABLE";
    private boolean serviceFailure = false;
    private String errorMessage;

    private String spfResult = "unavailable";
    private String spfRecord;
    private String spfRelayIp;

    private String dkimResult = "unavailable";
    private String dkimDomain;
    private String dkimSelector;
    private String dkimRecord;

    private String dmarcResult = "unavailable";
    private String dmarcPolicy;

    private String arcResult = "unavailable";

    private List<ReceivedHopDto> hops = new ArrayList<>();
    private String rawResponseJson;
    private String summary;
    private Instant evaluatedAt = Instant.now();

    public HeaderAuthResult() {
    }

    public static HeaderAuthResult success(String providerName) {
        HeaderAuthResult result = new HeaderAuthResult();
        result.providerName = providerName;
        result.status = "SUCCESS";
        result.serviceFailure = false;
        result.spfResult = "none";
        result.dkimResult = "none";
        result.dmarcResult = "none";
        result.arcResult = "none";
        result.evaluatedAt = Instant.now();
        return result;
    }

    public static HeaderAuthResult serviceFailure(String providerName, String errorMessage) {
        HeaderAuthResult result = new HeaderAuthResult();
        result.providerName = providerName;
        result.status = "FAILED";
        result.serviceFailure = true;
        result.errorMessage = errorMessage;
        result.spfResult = "unavailable";
        result.dkimResult = "unavailable";
        result.dmarcResult = "unavailable";
        result.arcResult = "unavailable";
        result.summary = providerName + " service failure: " + errorMessage;
        result.evaluatedAt = Instant.now();
        return result;
    }

    public static HeaderAuthResult allUnavailable(String message) {
        HeaderAuthResult result = new HeaderAuthResult();
        result.providerName = "UNAVAILABLE";
        result.status = "ALL_PROVIDERS_UNAVAILABLE";
        result.serviceFailure = true;
        result.errorMessage = message;
        result.spfResult = "unavailable";
        result.dkimResult = "unavailable";
        result.dmarcResult = "unavailable";
        result.arcResult = "unavailable";
        result.summary = message;
        result.evaluatedAt = Instant.now();
        return result;
    }

    // Getters and Setters

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isServiceFailure() {
        return serviceFailure;
    }

    public void setServiceFailure(boolean serviceFailure) {
        this.serviceFailure = serviceFailure;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getSpfResult() {
        return spfResult;
    }

    public void setSpfResult(String spfResult) {
        this.spfResult = spfResult;
    }

    public String getSpfRecord() {
        return spfRecord;
    }

    public void setSpfRecord(String spfRecord) {
        this.spfRecord = spfRecord;
    }

    public String getSpfRelayIp() {
        return spfRelayIp;
    }

    public void setSpfRelayIp(String spfRelayIp) {
        this.spfRelayIp = spfRelayIp;
    }

    public String getDkimResult() {
        return dkimResult;
    }

    public void setDkimResult(String dkimResult) {
        this.dkimResult = dkimResult;
    }

    public String getDkimDomain() {
        return dkimDomain;
    }

    public void setDkimDomain(String dkimDomain) {
        this.dkimDomain = dkimDomain;
    }

    public String getDkimSelector() {
        return dkimSelector;
    }

    public void setDkimSelector(String dkimSelector) {
        this.dkimSelector = dkimSelector;
    }

    public String getDkimRecord() {
        return dkimRecord;
    }

    public void setDkimRecord(String dkimRecord) {
        this.dkimRecord = dkimRecord;
    }

    public String getDmarcResult() {
        return dmarcResult;
    }

    public void setDmarcResult(String dmarcResult) {
        this.dmarcResult = dmarcResult;
    }

    public String getDmarcPolicy() {
        return dmarcPolicy;
    }

    public void setDmarcPolicy(String dmarcPolicy) {
        this.dmarcPolicy = dmarcPolicy;
    }

    public String getArcResult() {
        return arcResult;
    }

    public void setArcResult(String arcResult) {
        this.arcResult = arcResult;
    }

    public List<ReceivedHopDto> getHops() {
        return hops;
    }

    public void setHops(List<ReceivedHopDto> hops) {
        this.hops = hops != null ? hops : new ArrayList<>();
    }

    public String getRawResponseJson() {
        return rawResponseJson;
    }

    public void setRawResponseJson(String rawResponseJson) {
        this.rawResponseJson = rawResponseJson;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(Instant evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }
}

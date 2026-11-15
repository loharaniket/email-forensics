package com.emailforensics.backend.provider;

import java.util.LinkedHashMap;
import java.util.Map;

public class ProviderResult {

    private String providerName;
    private String status;
    private String verdict;
    private String message;
    private Map<String, Object> details = new LinkedHashMap<>();

    public ProviderResult() {
    }

    public ProviderResult(String providerName, String status, String verdict, String message) {
        this.providerName = providerName;
        this.status = status;
        this.verdict = verdict;
        this.message = message;
    }

    public ProviderResult(String providerName, String status, String verdict, String message, Map<String, Object> details) {
        this.providerName = providerName;
        this.status = status;
        this.verdict = verdict;
        this.message = message;
        this.details = details != null ? details : new LinkedHashMap<>();
    }

    public static ProviderResult match(String providerName, String verdict, String message, Map<String, Object> details) {
        return new ProviderResult(providerName, "MATCH", verdict, message, details);
    }

    public static ProviderResult noMatch(String providerName, String message) {
        return new ProviderResult(providerName, "NO_MATCH", "NO_MATCH", message, new LinkedHashMap<>());
    }

    public static ProviderResult unknown(String providerName, String message) {
        return new ProviderResult(providerName, "UNKNOWN", "UNKNOWN", message, new LinkedHashMap<>());
    }

    public static ProviderResult error(String providerName, String message) {
        return new ProviderResult(providerName, "ERROR", "UNKNOWN", message, new LinkedHashMap<>());
    }

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

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details != null ? details : new LinkedHashMap<>();
    }
}

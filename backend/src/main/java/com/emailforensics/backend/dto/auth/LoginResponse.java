package com.emailforensics.backend.dto.auth;

import java.time.Instant;

public class LoginResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private Instant expiresAt;
    private AnalystProfileDto analyst;

    public LoginResponse() {
    }

    public LoginResponse(String accessToken, String tokenType, Instant expiresAt, AnalystProfileDto analyst) {
        this.accessToken = accessToken;
        this.tokenType = tokenType != null ? tokenType : "Bearer";
        this.expiresAt = expiresAt;
        this.analyst = analyst;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public AnalystProfileDto getAnalyst() {
        return analyst;
    }

    public void setAnalyst(AnalystProfileDto analyst) {
        this.analyst = analyst;
    }
}

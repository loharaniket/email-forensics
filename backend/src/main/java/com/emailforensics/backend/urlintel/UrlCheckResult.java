package com.emailforensics.backend.urlintel;

public class UrlCheckResult {

    private String checkName;
    private String serviceName;
    private boolean flagged;
    private int riskScore;
    private String threatType;
    private String summary;

    public UrlCheckResult() {
    }

    public UrlCheckResult(String checkName, String serviceName, boolean flagged, int riskScore, String threatType, String summary) {
        this.checkName = checkName;
        this.serviceName = serviceName;
        this.flagged = flagged;
        this.riskScore = flagged ? Math.max(40, riskScore) : 0;
        this.threatType = threatType;
        this.summary = summary;
    }

    public String getCheckName() {
        return checkName;
    }

    public void setCheckName(String checkName) {
        this.checkName = checkName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public boolean isFlagged() {
        return flagged;
    }

    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public String getThreatType() {
        return threatType;
    }

    public void setThreatType(String threatType) {
        this.threatType = threatType;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}

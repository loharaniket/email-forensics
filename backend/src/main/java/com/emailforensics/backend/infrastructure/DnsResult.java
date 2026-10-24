package com.emailforensics.backend.infrastructure;

import java.util.ArrayList;
import java.util.List;

public class DnsResult {

    private String domain;
    private String status;
    private List<String> aRecords = new ArrayList<>();
    private List<String> mxRecords = new ArrayList<>();

    public DnsResult() {
    }

    public DnsResult(String domain, String status, List<String> aRecords, List<String> mxRecords) {
        this.domain = domain;
        this.status = status;
        this.aRecords = aRecords != null ? aRecords : new ArrayList<>();
        this.mxRecords = mxRecords != null ? mxRecords : new ArrayList<>();
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getARecords() {
        return aRecords;
    }

    public void setARecords(List<String> aRecords) {
        this.aRecords = aRecords;
    }

    public List<String> getMxRecords() {
        return mxRecords;
    }

    public void setMxRecords(List<String> mxRecords) {
        this.mxRecords = mxRecords;
    }
}

package com.emailforensics.backend.dto.infrastructure;

import java.time.Instant;
import java.util.UUID;

public class IpArtifactDto {

    private UUID id;
    private String ipAddress;
    private String sourceContext;
    private Boolean isPrivate;
    private String lookupStatus;
    private Instant observedAt;
    private String dataMode;

    public IpArtifactDto() {
    }

    public IpArtifactDto(UUID id, String ipAddress, String sourceContext, Boolean isPrivate,
                         String lookupStatus, Instant observedAt) {
        this(id, ipAddress, sourceContext, isPrivate, lookupStatus, observedAt, "UNAVAILABLE");
    }

    public IpArtifactDto(UUID id, String ipAddress, String sourceContext, Boolean isPrivate,
                         String lookupStatus, Instant observedAt, String dataMode) {
        this.id = id;
        this.ipAddress = ipAddress;
        this.sourceContext = sourceContext;
        this.isPrivate = isPrivate;
        this.lookupStatus = lookupStatus;
        this.observedAt = observedAt;
        this.dataMode = dataMode;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getSourceContext() {
        return sourceContext;
    }

    public void setSourceContext(String sourceContext) {
        this.sourceContext = sourceContext;
    }

    public Boolean getIsPrivate() {
        return isPrivate;
    }

    public void setIsPrivate(Boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public String getLookupStatus() {
        return lookupStatus;
    }

    public void setLookupStatus(String lookupStatus) {
        this.lookupStatus = lookupStatus;
    }

    public Instant getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(Instant observedAt) {
        this.observedAt = observedAt;
    }

    public String getDataMode() {
        return dataMode;
    }

    public void setDataMode(String dataMode) {
        this.dataMode = dataMode;
    }
}

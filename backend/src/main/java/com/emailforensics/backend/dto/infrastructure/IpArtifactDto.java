package com.emailforensics.backend.dto.infrastructure;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class IpArtifactDto {

    private UUID id;
    private String ipAddress;
    private String sourceContext;
    private Boolean isPrivate;
    private String lookupStatus;
    private String asnNumber;
    private String organization;
    private String country;
    private String city;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String dataSource;
    private Instant observedAt;
    private String dataMode;

    public IpArtifactDto() {
    }

    public IpArtifactDto(UUID id, String ipAddress, String sourceContext, Boolean isPrivate,
                         String lookupStatus, String asnNumber, String organization,
                         String country, String city, BigDecimal latitude,
                         BigDecimal longitude, String dataSource, Instant observedAt) {
        this(id, ipAddress, sourceContext, isPrivate, lookupStatus, asnNumber, organization,
                country, city, latitude, longitude, dataSource, observedAt, "UNAVAILABLE");
    }

    public IpArtifactDto(UUID id, String ipAddress, String sourceContext, Boolean isPrivate,
                         String lookupStatus, String asnNumber, String organization,
                         String country, String city, BigDecimal latitude,
                         BigDecimal longitude, String dataSource, Instant observedAt,
                         String dataMode) {
        this.id = id;
        this.ipAddress = ipAddress;
        this.sourceContext = sourceContext;
        this.isPrivate = isPrivate;
        this.lookupStatus = lookupStatus;
        this.asnNumber = asnNumber;
        this.organization = organization;
        this.country = country;
        this.city = city;
        this.latitude = latitude;
        this.longitude = longitude;
        this.dataSource = dataSource;
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

    public String getAsnNumber() {
        return asnNumber;
    }

    public void setAsnNumber(String asnNumber) {
        this.asnNumber = asnNumber;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
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

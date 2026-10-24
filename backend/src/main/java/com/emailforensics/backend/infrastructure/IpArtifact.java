package com.emailforensics.backend.infrastructure;

import com.emailforensics.backend.evidence.AnalysisRun;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ip_artifacts")
public class IpArtifact {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_run_id", nullable = false)
    private AnalysisRun analysisRun;

    @Column(name = "ip_address", nullable = false, length = 50)
    private String ipAddress;

    @Column(name = "source_context", nullable = false, length = 100)
    private String sourceContext;

    @Column(name = "is_private", nullable = false)
    private Boolean isPrivate = false;

    @Column(name = "lookup_status", nullable = false, length = 50)
    private String lookupStatus;

    @Column(name = "asn_number", length = 50)
    private String asnNumber;

    @Column(name = "organization", length = 255)
    private String organization;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "data_source", length = 100)
    private String dataSource;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public IpArtifact() {
    }

    public IpArtifact(UUID id, AnalysisRun analysisRun, String ipAddress, String sourceContext,
                      Boolean isPrivate, String lookupStatus, String asnNumber, String organization,
                      String country, String city, BigDecimal latitude, BigDecimal longitude,
                      String dataSource, Instant observedAt) {
        this.id = id;
        this.analysisRun = analysisRun;
        this.ipAddress = ipAddress;
        this.sourceContext = sourceContext;
        this.isPrivate = isPrivate != null ? isPrivate : false;
        this.lookupStatus = lookupStatus;
        this.asnNumber = asnNumber;
        this.organization = organization;
        this.country = country;
        this.city = city;
        this.latitude = latitude;
        this.longitude = longitude;
        this.dataSource = dataSource;
        this.observedAt = observedAt != null ? observedAt : Instant.now();
        this.createdAt = Instant.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.observedAt == null) {
            this.observedAt = Instant.now();
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

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

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "data_mode", nullable = false, length = 50)
    private String dataMode = DataProvenance.UNAVAILABLE.name();

    public IpArtifact() {
    }

    public IpArtifact(UUID id, AnalysisRun analysisRun, String ipAddress, String sourceContext,
                      Boolean isPrivate, String lookupStatus, Instant observedAt) {
        this(id, analysisRun, ipAddress, sourceContext, isPrivate, lookupStatus, observedAt,
                DataProvenance.UNAVAILABLE.name());
    }

    public IpArtifact(UUID id, AnalysisRun analysisRun, String ipAddress, String sourceContext,
                      Boolean isPrivate, String lookupStatus, Instant observedAt, String dataMode) {
        this.id = id;
        this.analysisRun = analysisRun;
        this.ipAddress = ipAddress;
        this.sourceContext = sourceContext;
        this.isPrivate = isPrivate != null ? isPrivate : false;
        this.lookupStatus = lookupStatus;
        this.observedAt = observedAt != null ? observedAt : Instant.now();
        this.createdAt = Instant.now();
        this.dataMode = dataMode != null ? dataMode : DataProvenance.UNAVAILABLE.name();
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

    public String getDataMode() {
        return dataMode;
    }

    public void setDataMode(String dataMode) {
        this.dataMode = dataMode;
    }
}

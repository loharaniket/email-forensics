package com.emailforensics.backend.headers;

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
@Table(name = "received_hops")
public class ReceivedHop {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_run_id", nullable = false)
    private AnalysisRun analysisRun;

    @Column(name = "hop_order", nullable = false)
    private Integer hopOrder;

    @Column(name = "from_host", length = 255)
    private String fromHost;

    @Column(name = "by_host", length = 255)
    private String byHost;

    @Column(name = "protocol", length = 50)
    private String protocol;

    @Column(name = "hop_ip", length = 50)
    private String hopIp;

    @Column(name = "hop_timestamp")
    private Instant hopTimestamp;

    @Column(name = "delay_seconds")
    private Long delaySeconds;

    @Column(name = "raw_hop_line", nullable = false, columnDefinition = "TEXT")
    private String rawHopLine;

    @Column(name = "parsing_warning", length = 255)
    private String parsingWarning;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public ReceivedHop() {
    }

    public ReceivedHop(UUID id, AnalysisRun analysisRun, Integer hopOrder, String fromHost,
                       String byHost, String protocol, String hopIp, Instant hopTimestamp,
                       Long delaySeconds, String rawHopLine, String parsingWarning) {
        this.id = id;
        this.analysisRun = analysisRun;
        this.hopOrder = hopOrder;
        this.fromHost = fromHost;
        this.byHost = byHost;
        this.protocol = protocol;
        this.hopIp = hopIp;
        this.hopTimestamp = hopTimestamp;
        this.delaySeconds = delaySeconds;
        this.rawHopLine = rawHopLine;
        this.parsingWarning = parsingWarning;
        this.createdAt = Instant.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
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

    public Integer getHopOrder() {
        return hopOrder;
    }

    public void setHopOrder(Integer hopOrder) {
        this.hopOrder = hopOrder;
    }

    public String getFromHost() {
        return fromHost;
    }

    public void setFromHost(String fromHost) {
        this.fromHost = fromHost;
    }

    public String getByHost() {
        return byHost;
    }

    public void setByHost(String byHost) {
        this.byHost = byHost;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getHopIp() {
        return hopIp;
    }

    public void setHopIp(String hopIp) {
        this.hopIp = hopIp;
    }

    public Instant getHopTimestamp() {
        return hopTimestamp;
    }

    public void setHopTimestamp(Instant hopTimestamp) {
        this.hopTimestamp = hopTimestamp;
    }

    public Long getDelaySeconds() {
        return delaySeconds;
    }

    public void setDelaySeconds(Long delaySeconds) {
        this.delaySeconds = delaySeconds;
    }

    public String getRawHopLine() {
        return rawHopLine;
    }

    public void setRawHopLine(String rawHopLine) {
        this.rawHopLine = rawHopLine;
    }

    public String getParsingWarning() {
        return parsingWarning;
    }

    public void setParsingWarning(String parsingWarning) {
        this.parsingWarning = parsingWarning;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

package com.emailforensics.backend.dto.headers;

import java.time.Instant;
import java.util.UUID;

public class ReceivedHopDto {

    private UUID id;
    private Integer hopOrder;
    private String fromHost;
    private String byHost;
    private String protocol;
    private String hopIp;
    private Instant hopTimestamp;
    private Long delaySeconds;
    private String rawHopLine;
    private String parsingWarning;

    public ReceivedHopDto() {
    }

    public ReceivedHopDto(UUID id, Integer hopOrder, String fromHost, String byHost,
                          String protocol, String hopIp, Instant hopTimestamp,
                          Long delaySeconds, String rawHopLine, String parsingWarning) {
        this.id = id;
        this.hopOrder = hopOrder;
        this.fromHost = fromHost;
        this.byHost = byHost;
        this.protocol = protocol;
        this.hopIp = hopIp;
        this.hopTimestamp = hopTimestamp;
        this.delaySeconds = delaySeconds;
        this.rawHopLine = rawHopLine;
        this.parsingWarning = parsingWarning;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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
}

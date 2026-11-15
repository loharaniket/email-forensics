package com.emailforensics.backend.health;

import java.time.Instant;
import java.util.List;

public class HealthResponse {

    private String status;
    private String database;
    private String databaseName;
    private List<String> tables;
    private long emailCount;
    private long indicatorCount;
    private Instant timestamp;

    public HealthResponse() {
        this.timestamp = Instant.now();
    }

    public HealthResponse(String status, String database, String databaseName, List<String> tables, long emailCount, long indicatorCount) {
        this.status = status;
        this.database = database;
        this.databaseName = databaseName;
        this.tables = tables;
        this.emailCount = emailCount;
        this.indicatorCount = indicatorCount;
        this.timestamp = Instant.now();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public List<String> getTables() {
        return tables;
    }

    public void setTables(List<String> tables) {
        this.tables = tables;
    }

    public long getEmailCount() {
        return emailCount;
    }

    public void setEmailCount(long emailCount) {
        this.emailCount = emailCount;
    }

    public long getIndicatorCount() {
        return indicatorCount;
    }

    public void setIndicatorCount(long indicatorCount) {
        this.indicatorCount = indicatorCount;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}

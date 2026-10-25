package com.emailforensics.backend.correlation;

import com.emailforensics.backend.evidence.AnalysisRun;
import com.emailforensics.backend.evidence.EmailEvidence;
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
@Table(name = "historical_matches")
public class HistoricalMatch {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_run_id", nullable = false)
    private AnalysisRun sourceRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matched_email_id", nullable = false)
    private EmailEvidence matchedEmail;

    @Column(name = "indicator_type", nullable = false, length = 50)
    private String indicatorType;

    @Column(name = "normalized_indicator_value", nullable = false, columnDefinition = "TEXT")
    private String normalizedIndicatorValue;

    @Column(name = "match_strength", nullable = false, length = 20)
    private String matchStrength;

    @Column(name = "match_explanation", nullable = false, columnDefinition = "TEXT")
    private String matchExplanation;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public HistoricalMatch() {
    }

    public HistoricalMatch(UUID id, AnalysisRun sourceRun, EmailEvidence matchedEmail,
                           String indicatorType, String normalizedIndicatorValue,
                           String matchStrength, String matchExplanation) {
        this.id = id;
        this.sourceRun = sourceRun;
        this.matchedEmail = matchedEmail;
        this.indicatorType = indicatorType;
        this.normalizedIndicatorValue = normalizedIndicatorValue;
        this.matchStrength = matchStrength;
        this.matchExplanation = matchExplanation;
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

    public AnalysisRun getSourceRun() {
        return sourceRun;
    }

    public void setSourceRun(AnalysisRun sourceRun) {
        this.sourceRun = sourceRun;
    }

    public EmailEvidence getMatchedEmail() {
        return matchedEmail;
    }

    public void setMatchedEmail(EmailEvidence matchedEmail) {
        this.matchedEmail = matchedEmail;
    }

    public String getIndicatorType() {
        return indicatorType;
    }

    public void setIndicatorType(String indicatorType) {
        this.indicatorType = indicatorType;
    }

    public String getNormalizedIndicatorValue() {
        return normalizedIndicatorValue;
    }

    public void setNormalizedIndicatorValue(String normalizedIndicatorValue) {
        this.normalizedIndicatorValue = normalizedIndicatorValue;
    }

    public String getMatchStrength() {
        return matchStrength;
    }

    public void setMatchStrength(String matchStrength) {
        this.matchStrength = matchStrength;
    }

    public String getMatchExplanation() {
        return matchExplanation;
    }

    public void setMatchExplanation(String matchExplanation) {
        this.matchExplanation = matchExplanation;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

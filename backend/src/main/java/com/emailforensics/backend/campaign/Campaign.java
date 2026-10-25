package com.emailforensics.backend.campaign;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "campaigns")
public class Campaign {

    @Id
    private UUID id;

    @Column(name = "campaign_code", nullable = false, unique = true, length = 50)
    private String campaignCode;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "confidence_score", nullable = false)
    private Integer confidenceScore;

    @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shared_indicators_json", columnDefinition = "JSONB")
    private String sharedIndicatorsJson;

    @Column(name = "first_seen", nullable = false)
    private Instant firstSeen;

    @Column(name = "last_seen", nullable = false)
    private Instant lastSeen;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CampaignMember> members = new ArrayList<>();

    public Campaign() {
    }

    public Campaign(UUID id, String campaignCode, String name, Integer confidenceScore,
                    String summary, String sharedIndicatorsJson, Instant firstSeen, Instant lastSeen) {
        this.id = id;
        this.campaignCode = campaignCode;
        this.name = name;
        this.confidenceScore = confidenceScore;
        this.summary = summary;
        this.sharedIndicatorsJson = sharedIndicatorsJson;
        this.firstSeen = firstSeen != null ? firstSeen : Instant.now();
        this.lastSeen = lastSeen != null ? lastSeen : Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = Instant.now();
        }
        if (this.firstSeen == null) {
            this.firstSeen = Instant.now();
        }
        if (this.lastSeen == null) {
            this.lastSeen = Instant.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCampaignCode() {
        return campaignCode;
    }

    public void setCampaignCode(String campaignCode) {
        this.campaignCode = campaignCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Integer confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSharedIndicatorsJson() {
        return sharedIndicatorsJson;
    }

    public void setSharedIndicatorsJson(String sharedIndicatorsJson) {
        this.sharedIndicatorsJson = sharedIndicatorsJson;
    }

    public Instant getFirstSeen() {
        return firstSeen;
    }

    public void setFirstSeen(Instant firstSeen) {
        this.firstSeen = firstSeen;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<CampaignMember> getMembers() {
        return members;
    }

    public void setMembers(List<CampaignMember> members) {
        this.members = members;
    }
}

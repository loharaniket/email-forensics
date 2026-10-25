package com.emailforensics.backend.campaign;

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
@Table(name = "campaign_members")
public class CampaignMember {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "email_id", nullable = false)
    private EmailEvidence emailEvidence;

    @Column(name = "membership_reason", nullable = false, columnDefinition = "TEXT")
    private String membershipReason;

    @Column(name = "match_score", nullable = false)
    private Integer matchScore;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public CampaignMember() {
    }

    public CampaignMember(UUID id, Campaign campaign, EmailEvidence emailEvidence,
                          String membershipReason, Integer matchScore) {
        this.id = id;
        this.campaign = campaign;
        this.emailEvidence = emailEvidence;
        this.membershipReason = membershipReason;
        this.matchScore = matchScore;
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

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    public EmailEvidence getEmailEvidence() {
        return emailEvidence;
    }

    public void setEmailEvidence(EmailEvidence emailEvidence) {
        this.emailEvidence = emailEvidence;
    }

    public String getMembershipReason() {
        return membershipReason;
    }

    public void setMembershipReason(String membershipReason) {
        this.membershipReason = membershipReason;
    }

    public Integer getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Integer matchScore) {
        this.matchScore = matchScore;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

package com.emailforensics.backend.visual;

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
@Table(name = "qr_artifacts")
public class QrArtifact {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visual_artifact_id", nullable = false)
    private VisualArtifact visualArtifact;

    @Column(name = "decoded_payload", nullable = false, columnDefinition = "TEXT")
    private String decodedPayload;

    @Column(name = "payload_type", nullable = false, length = 50)
    private String payloadType;

    @Column(name = "normalized_url", columnDefinition = "TEXT")
    private String normalizedUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public QrArtifact() {
    }

    public QrArtifact(UUID id, VisualArtifact visualArtifact, String decodedPayload,
                      String payloadType, String normalizedUrl) {
        this.id = id;
        this.visualArtifact = visualArtifact;
        this.decodedPayload = decodedPayload;
        this.payloadType = payloadType;
        this.normalizedUrl = normalizedUrl;
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

    public VisualArtifact getVisualArtifact() {
        return visualArtifact;
    }

    public void setVisualArtifact(VisualArtifact visualArtifact) {
        this.visualArtifact = visualArtifact;
    }

    public String getDecodedPayload() {
        return decodedPayload;
    }

    public void setDecodedPayload(String decodedPayload) {
        this.decodedPayload = decodedPayload;
    }

    public String getPayloadType() {
        return payloadType;
    }

    public void setPayloadType(String payloadType) {
        this.payloadType = payloadType;
    }

    public String getNormalizedUrl() {
        return normalizedUrl;
    }

    public void setNormalizedUrl(String normalizedUrl) {
        this.normalizedUrl = normalizedUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

package com.emailforensics.backend.dto.visual;

import java.time.Instant;
import java.util.UUID;

public class QrArtifactDto {

    private UUID id;
    private UUID imageId;
    private String imageFileName;
    private String decodedPayload;
    private String payloadType;
    private String normalizedUrl;
    private Instant createdAt;

    public QrArtifactDto() {
    }

    public QrArtifactDto(UUID id, String decodedPayload, String payloadType, String normalizedUrl, Instant createdAt) {
        this.id = id;
        this.decodedPayload = decodedPayload;
        this.payloadType = payloadType;
        this.normalizedUrl = normalizedUrl;
        this.createdAt = createdAt;
    }

    public QrArtifactDto(UUID id, UUID imageId, String imageFileName, String decodedPayload, String payloadType, String normalizedUrl, Instant createdAt) {
        this.id = id;
        this.imageId = imageId;
        this.imageFileName = imageFileName;
        this.decodedPayload = decodedPayload;
        this.payloadType = payloadType;
        this.normalizedUrl = normalizedUrl;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getImageId() {
        return imageId;
    }

    public void setImageId(UUID imageId) {
        this.imageId = imageId;
    }

    public String getImageFileName() {
        return imageFileName;
    }

    public void setImageFileName(String imageFileName) {
        this.imageFileName = imageFileName;
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

package com.emailforensics.backend.dto.visual;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VisualArtifactDto {

    private UUID id;
    private String filename;
    private String contentType;
    private Long fileSizeBytes;
    private String sha256Hash;
    private String processingError;
    private Instant createdAt;
    private List<QrArtifactDto> qrArtifacts = new ArrayList<>();
    private String dataMode;

    public VisualArtifactDto() {
    }

    public VisualArtifactDto(UUID id, String filename, String contentType, Long fileSizeBytes,
                             String sha256Hash, String processingError, Instant createdAt,
                             List<QrArtifactDto> qrArtifacts) {
        this(id, filename, contentType, fileSizeBytes, sha256Hash,
                processingError, createdAt, qrArtifacts, "NOT_CHECKED");
    }

    public VisualArtifactDto(UUID id, String filename, String contentType, Long fileSizeBytes,
                             String sha256Hash, String processingError, Instant createdAt,
                             List<QrArtifactDto> qrArtifacts, String dataMode) {
        this.id = id;
        this.filename = filename;
        this.contentType = contentType;
        this.fileSizeBytes = fileSizeBytes;
        this.sha256Hash = sha256Hash;
        this.processingError = processingError;
        this.createdAt = createdAt;
        this.qrArtifacts = qrArtifacts != null ? qrArtifacts : new ArrayList<>();
        this.dataMode = dataMode;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    public void setSha256Hash(String sha256Hash) {
        this.sha256Hash = sha256Hash;
    }

    public String getProcessingError() {
        return processingError;
    }

    public void setProcessingError(String processingError) {
        this.processingError = processingError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<QrArtifactDto> getQrArtifacts() {
        return qrArtifacts;
    }

    public void setQrArtifacts(List<QrArtifactDto> qrArtifacts) {
        this.qrArtifacts = qrArtifacts;
    }

    public String getDataMode() {
        return dataMode;
    }

    public void setDataMode(String dataMode) {
        this.dataMode = dataMode;
    }
}

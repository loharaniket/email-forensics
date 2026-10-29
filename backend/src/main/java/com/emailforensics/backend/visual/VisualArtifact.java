package com.emailforensics.backend.visual;

import com.emailforensics.backend.evidence.AnalysisRun;
import com.emailforensics.backend.infrastructure.DataProvenance;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "visual_artifacts")
public class VisualArtifact {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_run_id", nullable = false)
    private AnalysisRun analysisRun;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.CHAR)
    @Column(name = "sha256_hash", nullable = false, length = 64)
    private String sha256Hash;

    @Column(name = "processing_error", columnDefinition = "TEXT")
    private String processingError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "data_mode", nullable = false, length = 50)
    private String dataMode = DataProvenance.NOT_CHECKED.name();

    @OneToMany(mappedBy = "visualArtifact", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QrArtifact> qrArtifacts = new ArrayList<>();

    public VisualArtifact() {
    }

    public VisualArtifact(UUID id, AnalysisRun analysisRun, String filename, String contentType,
                          Long fileSizeBytes, String sha256Hash, String processingError) {
        this(id, analysisRun, filename, contentType, fileSizeBytes, sha256Hash,
                processingError, DataProvenance.NOT_CHECKED.name());
    }

    public VisualArtifact(UUID id, AnalysisRun analysisRun, String filename, String contentType,
                          Long fileSizeBytes, String sha256Hash, String processingError, String dataMode) {
        this.id = id;
        this.analysisRun = analysisRun;
        this.filename = filename;
        this.contentType = contentType;
        this.fileSizeBytes = fileSizeBytes;
        this.sha256Hash = sha256Hash;
        this.processingError = processingError;
        this.dataMode = dataMode != null ? dataMode : DataProvenance.NOT_CHECKED.name();
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

    public String getDataMode() {
        return dataMode;
    }

    public void setDataMode(String dataMode) {
        this.dataMode = dataMode;
    }

    public List<QrArtifact> getQrArtifacts() {
        return qrArtifacts;
    }

    public void setQrArtifacts(List<QrArtifact> qrArtifacts) {
        this.qrArtifacts = qrArtifacts;
    }
}

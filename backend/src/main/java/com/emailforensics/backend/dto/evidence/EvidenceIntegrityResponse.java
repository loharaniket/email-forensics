package com.emailforensics.backend.dto.evidence;

import java.time.Instant;
import java.util.UUID;

public class EvidenceIntegrityResponse {

    private UUID emailId;
    private String originalFilename;
    private String savedSha256;
    private String recalculatedSha256;
    private boolean intact;
    private Long fileSizeBytes;
    private Instant verifiedAt;

    public EvidenceIntegrityResponse() {
    }

    public EvidenceIntegrityResponse(UUID emailId, String originalFilename, String savedSha256,
                                     String recalculatedSha256, boolean intact, Long fileSizeBytes,
                                     Instant verifiedAt) {
        this.emailId = emailId;
        this.originalFilename = originalFilename;
        this.savedSha256 = savedSha256;
        this.recalculatedSha256 = recalculatedSha256;
        this.intact = intact;
        this.fileSizeBytes = fileSizeBytes;
        this.verifiedAt = verifiedAt;
    }

    public UUID getEmailId() {
        return emailId;
    }

    public void setEmailId(UUID emailId) {
        this.emailId = emailId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getSavedSha256() {
        return savedSha256;
    }

    public void setSavedSha256(String savedSha256) {
        this.savedSha256 = savedSha256;
    }

    public String getRecalculatedSha256() {
        return recalculatedSha256;
    }

    public void setRecalculatedSha256(String recalculatedSha256) {
        this.recalculatedSha256 = recalculatedSha256;
    }

    public boolean isIntact() {
        return intact;
    }

    public void setIntact(boolean intact) {
        this.intact = intact;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
}

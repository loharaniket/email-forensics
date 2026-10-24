package com.emailforensics.backend.evidence;

public class StoredEvidenceFile {

    private String storageKey;
    private String sha256Hash;
    private Long fileSizeBytes;

    public StoredEvidenceFile() {
    }

    public StoredEvidenceFile(String storageKey, String sha256Hash, Long fileSizeBytes) {
        this.storageKey = storageKey;
        this.sha256Hash = sha256Hash;
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    public void setSha256Hash(String sha256Hash) {
        this.sha256Hash = sha256Hash;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }
}

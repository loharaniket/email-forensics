package com.emailforensics.backend.dto.email;

public class InlineImageMetaDto {

    private String filename;
    private String mimeType;
    private Long sizeBytes;
    private String sha256Hash;
    private String contentId;

    public InlineImageMetaDto() {
    }

    public InlineImageMetaDto(String filename, String mimeType, Long sizeBytes, String sha256Hash, String contentId) {
        this.filename = filename;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.sha256Hash = sha256Hash;
        this.contentId = contentId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    public void setSha256Hash(String sha256Hash) {
        this.sha256Hash = sha256Hash;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }
}

package com.emailforensics.backend.dto.email;

public class AttachmentMetaDto {

    private String filename;
    private String mimeType;
    private Long sizeBytes;
    private String sha256Hash;
    private boolean inline;
    private String contentId;
    private boolean riskyExtension;

    public AttachmentMetaDto() {
    }

    public AttachmentMetaDto(String filename, String mimeType, Long sizeBytes, String sha256Hash,
                             boolean inline, String contentId, boolean riskyExtension) {
        this.filename = filename;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.sha256Hash = sha256Hash;
        this.inline = inline;
        this.contentId = contentId;
        this.riskyExtension = riskyExtension;
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

    public boolean isInline() {
        return inline;
    }

    public void setInline(boolean inline) {
        this.inline = inline;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public boolean isRiskyExtension() {
        return riskyExtension;
    }

    public void setRiskyExtension(boolean riskyExtension) {
        this.riskyExtension = riskyExtension;
    }
}

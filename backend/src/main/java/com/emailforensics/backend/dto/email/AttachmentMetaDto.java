package com.emailforensics.backend.dto.email;

public class AttachmentMetaDto {

    private String filename;
    private String mimeType;
    private Long sizeBytes;
    private String sha256Hash;
    private boolean inline;
    private String contentId;
    private boolean riskyExtension;
    private boolean qrDetected;
    private String qrPayload;
    private String qrPayloadType;
    private String qrNormalizedUrl;

    public AttachmentMetaDto() {
    }

    public AttachmentMetaDto(String filename, String mimeType, Long sizeBytes, String sha256Hash,
                             boolean inline, String contentId, boolean riskyExtension) {
        this(filename, mimeType, sizeBytes, sha256Hash, inline, contentId, riskyExtension, false, null, null, null);
    }

    public AttachmentMetaDto(String filename, String mimeType, Long sizeBytes, String sha256Hash,
                             boolean inline, String contentId, boolean riskyExtension,
                             boolean qrDetected, String qrPayload, String qrPayloadType, String qrNormalizedUrl) {
        this.filename = filename;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.sha256Hash = sha256Hash;
        this.inline = inline;
        this.contentId = contentId;
        this.riskyExtension = riskyExtension;
        this.qrDetected = qrDetected;
        this.qrPayload = qrPayload;
        this.qrPayloadType = qrPayloadType;
        this.qrNormalizedUrl = qrNormalizedUrl;
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

    public boolean isQrDetected() {
        return qrDetected;
    }

    public void setQrDetected(boolean qrDetected) {
        this.qrDetected = qrDetected;
    }

    public String getQrPayload() {
        return qrPayload;
    }

    public void setQrPayload(String qrPayload) {
        this.qrPayload = qrPayload;
    }

    public String getQrPayloadType() {
        return qrPayloadType;
    }

    public void setQrPayloadType(String qrPayloadType) {
        this.qrPayloadType = qrPayloadType;
    }

    public String getQrNormalizedUrl() {
        return qrNormalizedUrl;
    }

    public void setQrNormalizedUrl(String qrNormalizedUrl) {
        this.qrNormalizedUrl = qrNormalizedUrl;
    }
}

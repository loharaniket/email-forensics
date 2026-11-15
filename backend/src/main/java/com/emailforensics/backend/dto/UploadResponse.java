package com.emailforensics.backend.dto;

public class UploadResponse {

    private Long id;
    private String fileName;
    private String sha256;
    private String status;
    private String stage;
    private String message;

    public UploadResponse() {
    }

    public UploadResponse(Long id, String fileName, String sha256, String status, String stage, String message) {
        this.id = id;
        this.fileName = fileName;
        this.sha256 = sha256;
        this.status = status;
        this.stage = stage;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

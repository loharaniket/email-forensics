package com.emailforensics.backend.common;

import java.time.Instant;

public class ApiErrorResponse {

    private Instant timestamp;
    private int status;
    private String code;
    private String message;
    private String path;
    private String requestId;

    public ApiErrorResponse() {
        this.timestamp = Instant.now();
    }

    public ApiErrorResponse(int status, String code, String message, String path, String requestId) {
        this.timestamp = Instant.now();
        this.status = status;
        this.code = code;
        this.message = message;
        this.path = path;
        this.requestId = requestId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}

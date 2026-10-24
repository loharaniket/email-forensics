package com.emailforensics.backend.dto.casefile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateCaseRequest {

    @NotBlank(message = "Case title is required")
    @Size(max = 200, message = "Case title cannot exceed 200 characters")
    private String title;

    @Size(max = 2000, message = "Case description cannot exceed 2000 characters")
    private String description;

    public CreateCaseRequest() {
    }

    public CreateCaseRequest(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

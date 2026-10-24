package com.emailforensics.backend.dto.auth;

import java.util.List;
import java.util.UUID;

public class AnalystProfileDto {

    private UUID id;
    private String username;
    private String email;
    private String role;
    private List<String> permissions;

    public AnalystProfileDto() {
    }

    public AnalystProfileDto(UUID id, String username, String email, String role, List<String> permissions) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.permissions = permissions;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}

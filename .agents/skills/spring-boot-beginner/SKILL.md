---
name: spring-boot-beginner
description: Use this skill when generating, modifying, or explaining Spring Boot backend code, Java entities, controllers, services, repositories, or DTOs for a beginner developer.
---

# Skill: Beginner-Friendly Spring Boot Development

This skill provides practical rules, templates, and patterns for writing clean, simple Spring Boot code that a beginner Java developer can easily understand, debug, and maintain.

## When to Use This Skill

Activate this skill whenever:
- Creating or editing Java classes in `backend/src/main/java/com/emailforensics/`.
- Creating REST controllers, service layers, repositories, or entities.
- Explaining how backend components work to the developer.

---

## 1. Core Principles for Beginner Code

1. **Explicit over Implicit:** Write standard getters, setters, and constructors. Never use Lombok annotations.
2. **Simple Names:** Use descriptive names that clearly state purpose (for example: `EmailService`, `uploadEmail`, `sha256Hash`).
3. **No Code Comments:** The source code must contain no comments (`//`, `/* */`, `/** */`). Use simple names and small methods to make code clear. Explanations belong in chat responses or docs.
4. **Constructor Injection:** Always inject dependencies via normal public constructors. Avoid `@Autowired` on private fields.
5. **Basic Annotations Only:** Stick to foundational Spring Boot annotations: `@RestController`, `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@Service`, `@Repository`, `@Entity`, `@Id`, `@GeneratedValue`, `@Column`.

---

## 2. Standard Class Templates

### Entity Template (Without Lombok)
Entities map Java classes to PostgreSQL tables. Keep them straightforward with explicit getters and setters:

```java
package com.emailforensics.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.time.Instant;

@Entity
@Table(name = "emails")
public class Email {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    @Column(name = "status", nullable = false, length = 24)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Email() {
    }

    public Email(String fileName, String filePath, String sha256, String status) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.sha256 = sha256;
        this.status = status;
        this.createdAt = Instant.now();
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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
```

### Repository Template
Use simple Spring Data JPA interfaces. Spring generates standard CRUD queries automatically:

```java
package com.emailforensics.repository;

import com.emailforensics.entity.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EmailRepository extends JpaRepository<Email, Long> {

    Optional<Email> findBySha256(String sha256);
}
```

### Service Template
Services contain business logic and coordinate repositories and helpers:

```java
package com.emailforensics.service;

import com.emailforensics.entity.Email;
import com.emailforensics.repository.EmailRepository;
import com.emailforensics.exception.InvalidEmailException;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final EmailRepository emailRepository;

    public EmailService(EmailRepository emailRepository) {
        this.emailRepository = emailRepository;
    }

    public Email getEmailById(Long id) {
        return emailRepository.findById(id)
                .orElseThrow(() -> new InvalidEmailException("Email not found with id: " + id));
    }
}
```

### Controller Template
Controllers handle incoming HTTP requests, validate input, call the service layer, and return response DTOs:

```java
package com.emailforensics.controller;

import com.emailforensics.dto.UploadResponse;
import com.emailforensics.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/emails")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<UploadResponse> uploadEmail(@RequestParam("file") MultipartFile file) {
        UploadResponse response = emailService.processUpload(file);
        return ResponseEntity.accepted().body(response);
    }
}
```

---

## 3. Explaining Code to the Developer

Whenever you complete a backend feature, provide a simple summary explaining:
1. **What classes were created or modified:** List the file names.
2. **Why each class exists:** State its specific job in one or two sentences.
3. **How data flows:** Trace the request from the Controller through the Service to the Repository and PostgreSQL database.
4. **Key methods:** Explain what each important method does using plain, beginner-friendly terms.

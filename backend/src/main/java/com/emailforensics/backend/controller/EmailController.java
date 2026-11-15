package com.emailforensics.backend.controller;

import com.emailforensics.backend.dto.EmailDetailDto;
import com.emailforensics.backend.dto.IndicatorDto;
import com.emailforensics.backend.dto.UploadResponse;
import com.emailforensics.backend.service.EmailService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/emails")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> analyzeEmail(@RequestParam("file") MultipartFile file) {
        UploadResponse response = emailService.processUpload(file);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> uploadEmail(@RequestParam("file") MultipartFile file) {
        UploadResponse response = emailService.processUpload(file);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailDetailDto> getEmailById(@PathVariable Long id) {
        EmailDetailDto email = emailService.getEmailById(id);
        return ResponseEntity.ok(email);
    }

    @GetMapping("/{id}/indicators")
    public ResponseEntity<List<IndicatorDto>> getIndicatorsByEmailId(@PathVariable Long id) {
        List<IndicatorDto> indicators = emailService.getIndicatorsByEmailId(id);
        return ResponseEntity.ok(indicators);
    }

    @PostMapping("/{id}/check-threats")
    public ResponseEntity<List<IndicatorDto>> checkThreats(@PathVariable Long id) {
        List<IndicatorDto> indicators = emailService.checkThreatIntelligence(id);
        return ResponseEntity.ok(indicators);
    }

    @GetMapping
    public ResponseEntity<List<EmailDetailDto>> getRecentEmails() {
        List<EmailDetailDto> emails = emailService.getRecentEmails();
        return ResponseEntity.ok(emails);
    }

    @GetMapping("/{id}/original")
    public ResponseEntity<Resource> downloadOriginal(@PathVariable Long id) {
        Resource resource = emailService.loadOriginalFile(id);
        String fileName = emailService.getOriginalFilename(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("message/rfc822"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }
}

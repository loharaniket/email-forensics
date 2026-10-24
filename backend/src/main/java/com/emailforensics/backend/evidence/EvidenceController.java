package com.emailforensics.backend.evidence;

import com.emailforensics.backend.dto.email.AttachmentMetaDto;
import com.emailforensics.backend.dto.email.InvestigationDetailDto;
import com.emailforensics.backend.dto.common.PageResponse;
import com.emailforensics.backend.dto.evidence.EvidenceIntegrityResponse;
import com.emailforensics.backend.dto.evidence.EvidenceOverviewDto;
import com.emailforensics.backend.dto.evidence.EvidenceStatusResponse;
import com.emailforensics.backend.dto.evidence.EvidenceSummaryDto;
import com.emailforensics.backend.dto.evidence.EvidenceUploadResponse;
import com.emailforensics.backend.security.UserPrincipal;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class EvidenceController {

    private final EvidenceService evidenceService;

    public EvidenceController(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @PostMapping(value = "/cases/{caseId}/evidence/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EvidenceUploadResponse> uploadEvidence(
            @PathVariable UUID caseId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        EvidenceUploadResponse response = evidenceService.uploadEvidence(caseId, file, currentUser);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/evidence/{emailId}/status")
    public ResponseEntity<EvidenceStatusResponse> getEvidenceStatus(@PathVariable UUID emailId) {
        EvidenceStatusResponse response = evidenceService.getEvidenceStatus(emailId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/cases/{caseId}/evidence")
    public ResponseEntity<PageResponse<EvidenceSummaryDto>> getCaseEvidence(
            @PathVariable UUID caseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int boundedSize = Math.min(Math.max(size, 1), 100);
        int boundedPage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(boundedPage, boundedSize);

        PageResponse<EvidenceSummaryDto> response = evidenceService.getCaseEvidence(caseId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/evidence/{emailId}/overview")
    public ResponseEntity<EvidenceOverviewDto> getEvidenceOverview(@PathVariable UUID emailId) {
        EvidenceOverviewDto response = evidenceService.getEvidenceOverview(emailId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/evidence/{emailId}/integrity")
    public ResponseEntity<EvidenceIntegrityResponse> checkIntegrity(@PathVariable UUID emailId) {
        EvidenceIntegrityResponse response = evidenceService.checkIntegrity(emailId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/evidence/{emailId}/original")
    public ResponseEntity<Resource> downloadOriginal(@PathVariable UUID emailId) {
        Resource resource = evidenceService.loadOriginalFileResource(emailId);
        String filename = evidenceService.getOriginalFilename(emailId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("message/rfc822"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @GetMapping("/evidence/{emailId}/investigation")
    public ResponseEntity<InvestigationDetailDto> getInvestigationDetail(@PathVariable UUID emailId) {
        InvestigationDetailDto response = evidenceService.getInvestigationDetail(emailId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/evidence/{emailId}/attachments")
    public ResponseEntity<List<AttachmentMetaDto>> getEvidenceAttachments(@PathVariable UUID emailId) {
        List<AttachmentMetaDto> response = evidenceService.getEvidenceAttachments(emailId);
        return ResponseEntity.ok(response);
    }
}

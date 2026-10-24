package com.emailforensics.backend.casefile;

import com.emailforensics.backend.dto.casefile.CaseDetailDto;
import com.emailforensics.backend.dto.casefile.CaseSummaryDto;
import com.emailforensics.backend.dto.casefile.CreateCaseRequest;
import com.emailforensics.backend.dto.casefile.UpdateCaseNotesRequest;
import com.emailforensics.backend.dto.common.PageResponse;
import com.emailforensics.backend.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cases")
public class CaseController {

    private final CaseService caseService;

    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    @PostMapping
    public ResponseEntity<CaseDetailDto> createCase(
            @Valid @RequestBody CreateCaseRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        CaseDetailDto created = caseService.createCase(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<CaseSummaryDto>> getCases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) CaseStatus status) {
        PageResponse<CaseSummaryDto> response = caseService.getCases(page, size, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{caseId}")
    public ResponseEntity<CaseDetailDto> getCaseById(@PathVariable UUID caseId) {
        CaseDetailDto caseDetail = caseService.getCaseById(caseId);
        return ResponseEntity.ok(caseDetail);
    }

    @PatchMapping("/{caseId}/notes")
    public ResponseEntity<CaseDetailDto> updateCaseNotes(
            @PathVariable UUID caseId,
            @Valid @RequestBody UpdateCaseNotesRequest request) {
        CaseDetailDto updated = caseService.updateCaseNotes(caseId, request);
        return ResponseEntity.ok(updated);
    }
}

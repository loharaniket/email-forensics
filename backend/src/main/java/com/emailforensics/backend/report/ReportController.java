package com.emailforensics.backend.report;

import com.emailforensics.backend.dto.report.CaseReportDto;
import com.emailforensics.backend.dto.report.ForensicReportDto;
import com.emailforensics.backend.security.UserPrincipal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ReportController {

    private final ForensicReportService reportService;

    public ReportController(ForensicReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/evidence/{emailId}/report")
    public ResponseEntity<ForensicReportDto> getEvidenceReport(
            @PathVariable UUID emailId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ForensicReportDto report = reportService.generateReport(emailId, currentUser);
        return ResponseEntity.ok(report);
    }

    @GetMapping(value = "/evidence/{emailId}/report/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> downloadEvidenceReportJson(
            @PathVariable UUID emailId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ForensicReportDto report = reportService.generateReport(emailId, currentUser);
        String json = reportService.exportReportJson(report);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        String filename = "forensic-report-" + emailId.toString().substring(0, 8) + ".json";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(bytes);
    }

    @GetMapping("/cases/{caseId}/report")
    public ResponseEntity<CaseReportDto> getCaseReport(
            @PathVariable UUID caseId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        CaseReportDto report = reportService.generateCaseReport(caseId, currentUser);
        return ResponseEntity.ok(report);
    }
}

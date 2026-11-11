package com.emailforensics.backend.report;

import com.emailforensics.backend.dto.report.ForensicReportDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/evidence/{emailId}/report")
    public ResponseEntity<ForensicReportDto> getEvidenceReport(@PathVariable UUID emailId) {
        ForensicReportDto report = reportService.generateReport(emailId);
        return ResponseEntity.ok(report);
    }
}

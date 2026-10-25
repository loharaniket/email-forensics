package com.emailforensics.backend.evidence;

import com.emailforensics.backend.content.ContentAnalysisService;
import com.emailforensics.backend.dto.content.ContentAnalysisDto;
import com.emailforensics.backend.dto.email.AttachmentMetaDto;
import com.emailforensics.backend.dto.email.InvestigationDetailDto;
import com.emailforensics.backend.dto.common.PageResponse;
import com.emailforensics.backend.dto.evidence.EvidenceIntegrityResponse;
import com.emailforensics.backend.dto.evidence.EvidenceOverviewDto;
import com.emailforensics.backend.dto.evidence.EvidenceStatusResponse;
import com.emailforensics.backend.dto.evidence.EvidenceSummaryDto;
import com.emailforensics.backend.dto.evidence.EvidenceUploadResponse;
import com.emailforensics.backend.dto.headers.HeaderAnalysisDto;
import com.emailforensics.backend.dto.headers.ReceivedHopDto;
import com.emailforensics.backend.dto.infrastructure.IpArtifactDto;
import com.emailforensics.backend.correlation.CorrelationService;
import com.emailforensics.backend.dto.correlation.CorrelationMatchDto;
import com.emailforensics.backend.dto.risk.RiskBreakdownDto;
import com.emailforensics.backend.dto.urlintel.ExtractedUrlDto;
import com.emailforensics.backend.dto.visual.VisualArtifactDto;
import com.emailforensics.backend.headers.HeaderForensicService;
import com.emailforensics.backend.infrastructure.InfrastructureService;
import com.emailforensics.backend.risk.RiskScoringService;
import com.emailforensics.backend.security.UserPrincipal;
import com.emailforensics.backend.urlintel.UrlIntelService;
import com.emailforensics.backend.visual.VisualAnalysisService;
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
    private final HeaderForensicService headerForensicService;
    private final UrlIntelService urlIntelService;
    private final InfrastructureService infrastructureService;
    private final VisualAnalysisService visualAnalysisService;
    private final ContentAnalysisService contentAnalysisService;
    private final RiskScoringService riskScoringService;
    private final CorrelationService correlationService;

    public EvidenceController(EvidenceService evidenceService,
                              HeaderForensicService headerForensicService,
                              UrlIntelService urlIntelService,
                              InfrastructureService infrastructureService,
                              VisualAnalysisService visualAnalysisService,
                              ContentAnalysisService contentAnalysisService,
                              RiskScoringService riskScoringService,
                              CorrelationService correlationService) {
        this.evidenceService = evidenceService;
        this.headerForensicService = headerForensicService;
        this.urlIntelService = urlIntelService;
        this.infrastructureService = infrastructureService;
        this.visualAnalysisService = visualAnalysisService;
        this.contentAnalysisService = contentAnalysisService;
        this.riskScoringService = riskScoringService;
        this.correlationService = correlationService;
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

    @GetMapping("/evidence/{emailId}/headers")
    public ResponseEntity<HeaderAnalysisDto> getHeaderAnalysis(@PathVariable UUID emailId) {
        HeaderAnalysisDto response = headerForensicService.getHeaderAnalysis(emailId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/evidence/{emailId}/hops")
    public ResponseEntity<List<ReceivedHopDto>> getReceivedHops(@PathVariable UUID emailId) {
        List<ReceivedHopDto> response = headerForensicService.getReceivedHops(emailId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/evidence/{emailId}/urls")
    public ResponseEntity<List<ExtractedUrlDto>> getExtractedUrls(@PathVariable UUID emailId) {
        List<ExtractedUrlDto> response = urlIntelService.getExtractedUrls(emailId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/evidence/{emailId}/infrastructure")
    public ResponseEntity<List<IpArtifactDto>> getInfrastructure(@PathVariable UUID emailId) {
        List<IpArtifactDto> response = infrastructureService.getInfrastructure(emailId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/evidence/{emailId}/visuals")
    public ResponseEntity<List<VisualArtifactDto>> getVisualArtifacts(@PathVariable UUID emailId) {
        List<VisualArtifactDto> response = visualAnalysisService.getVisualArtifacts(emailId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/evidence/{emailId}/content-analysis")
    public ResponseEntity<ContentAnalysisDto> getContentAnalysis(@PathVariable UUID emailId) {
        return contentAnalysisService.getContentAnalysis(emailId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/evidence/{emailId}/risk-breakdown")
    public ResponseEntity<RiskBreakdownDto> getRiskBreakdown(@PathVariable UUID emailId) {
        RiskBreakdownDto response = riskScoringService.getRiskBreakdown(emailId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/evidence/{emailId}/correlations")
    public ResponseEntity<List<CorrelationMatchDto>> getCorrelations(@PathVariable UUID emailId) {
        List<CorrelationMatchDto> response = correlationService.getCorrelations(emailId);
        return ResponseEntity.ok(response);
    }
}

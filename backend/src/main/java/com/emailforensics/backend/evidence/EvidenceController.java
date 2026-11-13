package com.emailforensics.backend.evidence;

import com.emailforensics.backend.content.ContentAnalysisService;
import com.emailforensics.backend.correlation.CorrelationService;
import com.emailforensics.backend.dto.content.ContentAnalysisDto;
import com.emailforensics.backend.dto.correlation.CorrelationMatchDto;
import com.emailforensics.backend.dto.email.AttachmentMetaDto;
import com.emailforensics.backend.dto.email.InvestigationDetailDto;
import com.emailforensics.backend.dto.evidence.EvidenceIntegrityResponse;
import com.emailforensics.backend.dto.evidence.EvidenceStatusResponse;
import com.emailforensics.backend.dto.evidence.EvidenceSummaryDto;
import com.emailforensics.backend.dto.evidence.EvidenceUploadResponse;
import com.emailforensics.backend.dto.headers.HeaderAnalysisDto;
import com.emailforensics.backend.dto.headers.ReceivedHopDto;
import com.emailforensics.backend.dto.infrastructure.IpArtifactDto;
import com.emailforensics.backend.dto.risk.RiskBreakdownDto;
import com.emailforensics.backend.dto.urlintel.ExtractedUrlDto;
import com.emailforensics.backend.dto.visual.QrArtifactDto;
import com.emailforensics.backend.dto.visual.VisualArtifactDto;
import com.emailforensics.backend.headers.HeaderAnalysisService;
import com.emailforensics.backend.infrastructure.IpArtifactService;
import com.emailforensics.backend.risk.RiskScoringService;
import com.emailforensics.backend.urlintel.UrlAnalysisService;
import com.emailforensics.backend.visual.VisualAnalysisService;
import com.emailforensics.backend.visual.VisualArtifact;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class EvidenceController {

    private final EvidenceService evidenceService;
    private final HeaderAnalysisService headerAnalysisService;
    private final UrlAnalysisService urlAnalysisService;
    private final IpArtifactService ipArtifactService;
    private final VisualAnalysisService visualAnalysisService;
    private final ContentAnalysisService contentAnalysisService;
    private final RiskScoringService riskScoringService;
    private final CorrelationService correlationService;

    public EvidenceController(EvidenceService evidenceService,
                              HeaderAnalysisService headerAnalysisService,
                              UrlAnalysisService urlAnalysisService,
                              IpArtifactService ipArtifactService,
                              VisualAnalysisService visualAnalysisService,
                              ContentAnalysisService contentAnalysisService,
                              RiskScoringService riskScoringService,
                              CorrelationService correlationService) {
        this.evidenceService = evidenceService;
        this.headerAnalysisService = headerAnalysisService;
        this.urlAnalysisService = urlAnalysisService;
        this.ipArtifactService = ipArtifactService;
        this.visualAnalysisService = visualAnalysisService;
        this.contentAnalysisService = contentAnalysisService;
        this.riskScoringService = riskScoringService;
        this.correlationService = correlationService;
    }

    @PostMapping(value = "/evidence/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EvidenceUploadResponse> uploadEvidence(
            @RequestParam("file") MultipartFile file) {
        EvidenceUploadResponse response = evidenceService.uploadEvidence(file);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/evidence")
    public ResponseEntity<Page<EvidenceSummaryDto>> getEvidenceList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        int boundedPage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(boundedPage, boundedSize);

        Page<EvidenceSummaryDto> response = evidenceService.getEvidenceList(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/evidence/{emailId}/status")
    public ResponseEntity<EvidenceStatusResponse> getEvidenceStatus(@PathVariable UUID emailId) {
        EvidenceStatusResponse response = evidenceService.getEvidenceStatus(emailId);
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
    public ResponseEntity<List<AttachmentMetaDto>> getAttachments(@PathVariable UUID emailId) {
        List<AttachmentMetaDto> response = evidenceService.getEvidenceAttachments(emailId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/evidence/{emailId}/headers")
    public ResponseEntity<HeaderAnalysisDto> getHeaderAnalysis(@PathVariable UUID emailId) {
        HeaderAnalysisDto response = headerAnalysisService.getHeaderAnalysis(emailId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/evidence/{emailId}/hops")
    public ResponseEntity<List<ReceivedHopDto>> getReceivedHops(@PathVariable UUID emailId) {
        List<ReceivedHopDto> response = headerAnalysisService.getReceivedHops(emailId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/evidence/{emailId}/urls")
    public ResponseEntity<List<ExtractedUrlDto>> getExtractedUrls(@PathVariable UUID emailId) {
        List<ExtractedUrlDto> response = urlAnalysisService.getExtractedUrls(emailId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/evidence/{emailId}/infrastructure")
    public ResponseEntity<List<IpArtifactDto>> getInfrastructure(@PathVariable UUID emailId) {
        List<IpArtifactDto> response = ipArtifactService.getInfrastructure(emailId);
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/evidence/{emailId}/visuals", "/evidence/{emailId}/images"})
    public ResponseEntity<List<VisualArtifactDto>> getVisualArtifacts(@PathVariable UUID emailId) {
        List<VisualArtifactDto> response = visualAnalysisService.getVisualArtifacts(emailId);
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/evidence/{emailId}/qr", "/evidence/{emailId}/qr-codes"})
    public ResponseEntity<List<QrArtifactDto>> getQrArtifacts(@PathVariable UUID emailId) {
        List<QrArtifactDto> response = visualAnalysisService.getQrArtifacts(emailId);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = {"/evidence/{emailId}/images/{imageId}/content", "/evidence/{emailId}/images/{imageId}/file"})
    public ResponseEntity<byte[]> getImageContent(@PathVariable UUID emailId, @PathVariable UUID imageId) {
        VisualArtifact visual = visualAnalysisService.getVisualArtifact(emailId, imageId);
        byte[] imageBytes = visualAnalysisService.getImageData(emailId, imageId);

        MediaType mediaType;
        try {
            mediaType = visual.getContentType() != null
                    ? MediaType.parseMediaType(visual.getContentType())
                    : MediaType.APPLICATION_OCTET_STREAM;
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        String filename = visual.getFilename() != null && !visual.getFilename().trim().isEmpty()
                ? visual.getFilename().trim()
                : "image_" + imageId + ".png";

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(imageBytes);
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

    @PostMapping("/evidence/{emailId}/retry")
    public ResponseEntity<EvidenceStatusResponse> retryAnalysis(@PathVariable UUID emailId) {
        EvidenceStatusResponse response = evidenceService.retryAnalysis(emailId);
        return ResponseEntity.ok(response);
    }
}

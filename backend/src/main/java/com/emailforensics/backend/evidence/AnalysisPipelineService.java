package com.emailforensics.backend.evidence;

import com.emailforensics.backend.aiassessment.AiAssessmentService;
import com.emailforensics.backend.content.ContentAnalysisService;
import com.emailforensics.backend.correlation.CorrelationService;
import com.emailforensics.backend.dto.email.ParsedEmailDto;
import com.emailforensics.backend.email.EmailParserService;
import com.emailforensics.backend.headers.HeaderAnalysisService;
import com.emailforensics.backend.infrastructure.IpArtifactService;
import com.emailforensics.backend.risk.RiskScoringService;
import com.emailforensics.backend.urlintel.UrlAnalysisService;
import com.emailforensics.backend.visual.VisualAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AnalysisPipelineService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisPipelineService.class);

    private final AnalysisProgressService progressService;
    private final EmailEvidenceRepository evidenceRepository;
    private final AnalysisRunRepository analysisRunRepository;
    private final EvidenceStorageService storageService;
    private final EmailParserService emailParserService;
    private final HeaderAnalysisService headerAnalysisService;
    private final UrlAnalysisService urlAnalysisService;
    private final IpArtifactService ipArtifactService;
    private final VisualAnalysisService visualAnalysisService;
    private final ContentAnalysisService contentAnalysisService;
    private final CorrelationService correlationService;
    private final RiskScoringService riskScoringService;
    private final AiAssessmentService aiAssessmentService;

    private final Set<UUID> activeExecutions = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public AnalysisPipelineService(AnalysisProgressService progressService,
                                       EmailEvidenceRepository evidenceRepository,
                                       AnalysisRunRepository analysisRunRepository,
                                       EvidenceStorageService storageService,
                                       EmailParserService emailParserService,
                                       HeaderAnalysisService headerAnalysisService,
                                       UrlAnalysisService urlAnalysisService,
                                       IpArtifactService ipArtifactService,
                                       VisualAnalysisService visualAnalysisService,
                                       ContentAnalysisService contentAnalysisService,
                                       CorrelationService correlationService,
                                       RiskScoringService riskScoringService,
                                       AiAssessmentService aiAssessmentService) {
        this.progressService = progressService;
        this.evidenceRepository = evidenceRepository;
        this.analysisRunRepository = analysisRunRepository;
        this.storageService = storageService;
        this.emailParserService = emailParserService;
        this.headerAnalysisService = headerAnalysisService;
        this.urlAnalysisService = urlAnalysisService;
        this.ipArtifactService = ipArtifactService;
        this.visualAnalysisService = visualAnalysisService;
        this.contentAnalysisService = contentAnalysisService;
        this.correlationService = correlationService;
        this.riskScoringService = riskScoringService;
        this.aiAssessmentService = aiAssessmentService;
    }

    public boolean isPipelineActive(UUID emailId) {
        return activeExecutions.contains(emailId);
    }

    @Async
    public CompletableFuture<AnalysisRun> startAnalysisAsync(UUID emailId, UUID runId) {
        try {
            AnalysisRun result = runPipeline(emailId, runId);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Unhandled error in async forensic pipeline for emailId {}", emailId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    public AnalysisRun runPipeline(UUID emailId, UUID runId) {
        if (!activeExecutions.add(emailId)) {
            log.warn("Forensic pipeline already active for emailId: {}", emailId);
            return analysisRunRepository.findById(runId).orElse(null);
        }

        List<String> partialWarnings = new ArrayList<>();

        try {
            log.info("Starting forensic pipeline for emailId: {}, runId: {}", emailId, runId);
            progressService.markRunning(runId, "PARSING_EMAIL");

            EmailEvidence evidence = evidenceRepository.findById(emailId)
                    .orElseThrow(() -> new IllegalArgumentException("Evidence not found with ID: " + emailId));

            // STAGE 1: PARSING_EMAIL
            try {
                try (InputStream in = storageService.loadAsResource(evidence.getStorageKey()).getInputStream()) {
                    ParsedEmailDto parsed = emailParserService.parse(in);
                    boolean updated = false;
                    if (evidence.getSubject() == null && parsed.getSubject() != null) {
                        evidence.setSubject(parsed.getSubject());
                        updated = true;
                    }
                    if (evidence.getMessageId() == null && parsed.getMessageId() != null) {
                        evidence.setMessageId(parsed.getMessageId());
                        updated = true;
                    }
                    if (evidence.getSentDate() == null && parsed.getSentDate() != null) {
                        evidence.setSentDate(parsed.getSentDate());
                        updated = true;
                    }
                    if (updated) {
                        evidenceRepository.save(evidence);
                    }
                }
            } catch (Exception e) {
                log.error("Stage PARSING_EMAIL failed critically for evidence {}", emailId, e);
                return progressService.markFailed(runId, "PARSING_EMAIL", "MIME_PARSE_FAILED",
                        "Failed to parse MIME structure: " + e.getMessage());
            }

            // STAGE 2: ANALYZING_HEADERS
            try {
                progressService.updateStage(runId, "ANALYZING_HEADERS");
                headerAnalysisService.analyzeAndSaveHeaders(emailId);
            } catch (Exception e) {
                log.error("Stage ANALYZING_HEADERS failed critically for evidence {}", emailId, e);
                return progressService.markFailed(runId, "ANALYZING_HEADERS", "HEADER_ANALYSIS_FAILED",
                        "Failed to analyze email headers: " + e.getMessage());
            }

            // STAGE 3: EXTRACTING_INDICATORS
            try {
                progressService.updateStage(runId, "EXTRACTING_INDICATORS");
                urlAnalysisService.analyzeAndSaveUrls(emailId);
                ipArtifactService.analyzeAndSaveInfrastructure(emailId, null);
            } catch (Exception e) {
                log.warn("Stage EXTRACTING_INDICATORS encountered non-fatal error for evidence {}", emailId, e);
                partialWarnings.add("Indicator extraction warning: " + e.getMessage());
            }

            // STAGE 4: ANALYZING_VISUALS
            try {
                progressService.updateStage(runId, "ANALYZING_VISUALS");
                visualAnalysisService.analyzeAndSaveVisuals(emailId);
            } catch (Exception e) {
                log.warn("Stage ANALYZING_VISUALS encountered non-fatal error for evidence {}", emailId, e);
                partialWarnings.add("Visual analysis warning: " + e.getMessage());
            }

            // STAGE 5: ANALYZING_CONTENT
            try {
                progressService.updateStage(runId, "ANALYZING_CONTENT");
                contentAnalysisService.analyzeContent(emailId);
            } catch (Exception e) {
                log.warn("Stage ANALYZING_CONTENT encountered non-fatal error for evidence {}", emailId, e);
                partialWarnings.add("Content analysis warning: " + e.getMessage());
            }

            // STAGE 6: CORRELATING_THREATS
            try {
                progressService.updateStage(runId, "CORRELATING_THREATS");
                correlationService.correlateAndSave(emailId);
            } catch (Exception e) {
                log.warn("Stage CORRELATING_THREATS encountered non-fatal error for evidence {}", emailId, e);
                partialWarnings.add("Threat correlation warning: " + e.getMessage());
            }

            // STAGE 7: ASSESSING_RISK
            try {
                progressService.updateStage(runId, "ASSESSING_RISK");
                riskScoringService.evaluateAndSaveRisk(emailId);
            } catch (Exception e) {
                log.warn("Stage ASSESSING_RISK encountered non-fatal error for evidence {}", emailId, e);
                partialWarnings.add("Risk assessment warning: " + e.getMessage());
            }

            // STAGE 8: AI_ASSESSMENT
            try {
                progressService.updateStage(runId, "AI_ASSESSMENT");
                aiAssessmentService.generateAssessment(emailId, false, null);
            } catch (Exception e) {
                log.warn("Stage AI_ASSESSMENT encountered non-fatal error for evidence {}", emailId, e);
                partialWarnings.add("AI assessment warning: " + e.getMessage());
            }

            // STAGE 9: FINALIZING
            progressService.updateStage(runId, "FINALIZING");
            if (partialWarnings.isEmpty()) {
                log.info("Forensic pipeline COMPLETED successfully for evidence {}", emailId);
                return progressService.markCompleted(runId, "COMPLETED");
            } else {
                String safeMsg = String.join("; ", partialWarnings);
                log.warn("Forensic pipeline completed with PARTIAL status for evidence {}: {}", emailId, safeMsg);
                return progressService.markPartial(runId, "COMPLETED", safeMsg);
            }

        } catch (Exception unexpected) {
            log.error("Unexpected failure during forensic pipeline for evidence {}", emailId, unexpected);
            return progressService.markFailed(runId, "FAILED", "PIPELINE_ERROR",
                    "Unexpected error during forensic analysis: " + unexpected.getMessage());
        } finally {
            activeExecutions.remove(emailId);
        }
    }
}


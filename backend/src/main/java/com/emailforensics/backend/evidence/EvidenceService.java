package com.emailforensics.backend.evidence;

import com.emailforensics.backend.common.BadRequestException;
import com.emailforensics.backend.common.ResourceNotFoundException;
import com.emailforensics.backend.dto.email.AttachmentMetaDto;
import com.emailforensics.backend.dto.email.InvestigationDetailDto;
import com.emailforensics.backend.dto.email.ParsedEmailDto;
import com.emailforensics.backend.dto.evidence.EvidenceIntegrityResponse;
import com.emailforensics.backend.dto.evidence.EvidenceStatusResponse;
import com.emailforensics.backend.dto.evidence.EvidenceSummaryDto;
import com.emailforensics.backend.dto.evidence.EvidenceUploadResponse;
import com.emailforensics.backend.email.EmailParserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EvidenceService {

    private final EmailEvidenceRepository evidenceRepository;
    private final AnalysisRunRepository analysisRunRepository;
    private final EvidenceStorageService storageService;
    private final EmailParserService emailParserService;
    private final com.emailforensics.backend.analysis.AnalysisResultRepository analysisResultRepository;
    private final AnalysisPipelineService analysisPipelineService;
    private final String pipelineVersion;

    public EvidenceService(EmailEvidenceRepository evidenceRepository,
                           AnalysisRunRepository analysisRunRepository,
                           EvidenceStorageService storageService,
                           EmailParserService emailParserService,
                           com.emailforensics.backend.analysis.AnalysisResultRepository analysisResultRepository,
                           AnalysisPipelineService analysisPipelineService,
                           @Value("${emailforensics.pipeline.version:1.0.0}") String pipelineVersion) {
        this.evidenceRepository = evidenceRepository;
        this.analysisRunRepository = analysisRunRepository;
        this.storageService = storageService;
        this.emailParserService = emailParserService;
        this.analysisResultRepository = analysisResultRepository;
        this.analysisPipelineService = analysisPipelineService;
        this.pipelineVersion = pipelineVersion;
    }

    @Transactional
    public EvidenceUploadResponse uploadEvidence(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Evidence file is required and cannot be empty");
        }

        String rawFilename = file.getOriginalFilename();
        if (rawFilename == null || rawFilename.trim().isEmpty()) {
            throw new BadRequestException("Original filename cannot be empty");
        }

        String sanitizedFilename = storageService.sanitizeFilename(rawFilename);
        if (!sanitizedFilename.toLowerCase().endsWith(".eml")) {
            throw new BadRequestException("Only .eml files are supported as email evidence");
        }

        StoredEvidenceFile stored;
        try (InputStream inputStream = file.getInputStream()) {
            stored = storageService.storeEvidence(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded file stream", e);
        }

        // Global Deduplication:
        // If an email with identical SHA-256 hash already exists, reuse it.
        synchronized (stored.getSha256Hash().intern()) {
            Optional<EmailEvidence> existingEvidenceOpt = evidenceRepository
                    .findFirstBySha256HashOrderByUploadedAtAsc(stored.getSha256Hash());

            if (existingEvidenceOpt.isPresent()) {
                storageService.deleteStoredFile(stored.getStorageKey());

                EmailEvidence existing = existingEvidenceOpt.get();
                AnalysisRun existingRun = analysisRunRepository
                        .findTopByEmailEvidenceIdOrderByCreatedAtDesc(existing.getId())
                        .orElse(null);

                if (existingRun != null && existingRun.getStatus() == AnalysisStatus.QUEUED && !analysisPipelineService.isPipelineActive(existing.getId())) {
                    analysisPipelineService.startAnalysisAsync(existing.getId(), existingRun.getId());
                }

                String status = existingRun != null ? existingRun.getStatus().name() : AnalysisStatus.COMPLETED.name();
                UUID runId = existingRun != null ? existingRun.getId() : null;
                String statusUrl = "/api/v1/evidence/" + existing.getId() + "/status";

                return new EvidenceUploadResponse(
                        existing.getId(),
                        runId,
                        status,
                        statusUrl,
                        existing.getSha256Hash(),
                        existing.getOriginalFilename(),
                        existing.getFileSizeBytes(),
                        true,
                        null,
                        null,
                        null,
                        "Duplicate email evidence detected with identical SHA-256 hash. Reusing existing evidence and forensic analysis results."
                );
            }

            EmailEvidence evidence = new EmailEvidence(
                    UUID.randomUUID(),
                    sanitizedFilename,
                    stored.getStorageKey(),
                    stored.getFileSizeBytes(),
                    stored.getSha256Hash()
            );

            try (InputStream streamForParse = storageService.loadAsResource(stored.getStorageKey()).getInputStream()) {
                ParsedEmailDto parsed = emailParserService.parse(streamForParse);
                if (parsed.getSubject() != null) evidence.setSubject(parsed.getSubject());
                if (parsed.getMessageId() != null) evidence.setMessageId(parsed.getMessageId());
                if (parsed.getSentDate() != null) evidence.setSentDate(parsed.getSentDate());
            } catch (Exception ignored) {
            }

            evidence = evidenceRepository.save(evidence);

            AnalysisRun run = new AnalysisRun(
                    UUID.randomUUID(),
                    evidence,
                    pipelineVersion,
                    AnalysisStatus.QUEUED,
                    "QUEUED"
            );
            run = analysisRunRepository.save(run);

            triggerAnalysisAfterCommit(evidence.getId(), run.getId());

            String statusUrl = "/api/v1/evidence/" + evidence.getId() + "/status";

            return new EvidenceUploadResponse(
                    evidence.getId(),
                    run.getId(),
                    run.getStatus().name(),
                    statusUrl,
                    evidence.getSha256Hash(),
                    evidence.getOriginalFilename(),
                    evidence.getFileSizeBytes(),
                    false,
                    null,
                    null,
                    null,
                    "Evidence preserved and queued for forensic analysis."
            );
        }
    }

    @Transactional
    public EvidenceStatusResponse getEvidenceStatus(UUID emailId) {
        if (!evidenceRepository.existsById(emailId)) {
            throw new ResourceNotFoundException("Email evidence not found with ID: " + emailId);
        }

        AnalysisRun run = analysisRunRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("No analysis run found for evidence: " + emailId));

        if (run.getStatus() == AnalysisStatus.QUEUED && !analysisPipelineService.isPipelineActive(emailId)) {
            analysisPipelineService.startAnalysisAsync(emailId, run.getId());
        }

        return new EvidenceStatusResponse(
                emailId,
                run.getId(),
                run.getStatus(),
                run.getCurrentStage(),
                run.getPipelineVersion(),
                run.getAttemptNumber(),
                run.getStartedAt(),
                run.getCompletedAt(),
                run.getErrorCode(),
                run.getSafeErrorMessage()
        );
    }

    @Transactional(readOnly = true)
    public Page<EvidenceSummaryDto> getEvidenceList(Pageable pageable) {
        Page<EmailEvidence> page = evidenceRepository.findAllByOrderByUploadedAtDesc(pageable);
        List<EvidenceSummaryDto> content = new ArrayList<>();

        for (EmailEvidence item : page.getContent()) {
            Optional<AnalysisRun> latestRun = analysisRunRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(item.getId());
            AnalysisStatus status = latestRun.map(AnalysisRun::getStatus).orElse(AnalysisStatus.QUEUED);
            String currentStage = latestRun.map(AnalysisRun::getCurrentStage).orElse("QUEUED");

            EvidenceSummaryDto dto = new EvidenceSummaryDto(
                    item.getId(),
                    item.getOriginalFilename(),
                    item.getFileSizeBytes(),
                    item.getSha256Hash(),
                    item.getSubject(),
                    item.getMessageId(),
                    item.getSentDate(),
                    item.getUploadedAt(),
                    status,
                    currentStage
            );
            content.add(dto);
        }

        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public EvidenceIntegrityResponse checkIntegrity(UUID emailId) {
        EmailEvidence evidence = evidenceRepository.findById(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("Email evidence not found with ID: " + emailId));

        String recalculatedHash = storageService.computeFileHash(evidence.getStorageKey());
        boolean intact = evidence.getSha256Hash().equalsIgnoreCase(recalculatedHash);

        return new EvidenceIntegrityResponse(
                evidence.getId(),
                evidence.getOriginalFilename(),
                evidence.getSha256Hash(),
                recalculatedHash,
                intact,
                evidence.getFileSizeBytes(),
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public Resource loadOriginalFileResource(UUID emailId) {
        EmailEvidence evidence = evidenceRepository.findById(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("Email evidence not found with ID: " + emailId));
        return storageService.loadAsResource(evidence.getStorageKey());
    }

    @Transactional(readOnly = true)
    public String getOriginalFilename(UUID emailId) {
        EmailEvidence evidence = evidenceRepository.findById(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("Email evidence not found with ID: " + emailId));
        return evidence.getOriginalFilename();
    }

    @Transactional(readOnly = true)
    public InvestigationDetailDto getInvestigationDetail(UUID emailId) {
        EmailEvidence evidence = evidenceRepository.findById(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("Email evidence not found with ID: " + emailId));

        AnalysisRun run = analysisRunRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)
                .orElse(null);

        AnalysisStatus status = run != null ? run.getStatus() : AnalysisStatus.QUEUED;
        String currentStage = run != null ? run.getCurrentStage() : "QUEUED";

        ParsedEmailDto parsed;
        try (InputStream inputStream = storageService.loadAsResource(evidence.getStorageKey()).getInputStream()) {
            parsed = emailParserService.parse(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read evidence stream for parsing", e);
        }

        return new InvestigationDetailDto(
                evidence.getId(),
                evidence.getOriginalFilename(),
                evidence.getFileSizeBytes(),
                evidence.getSha256Hash(),
                evidence.getUploadedAt(),
                status,
                currentStage,
                parsed
        );
    }

    @Transactional
    public EvidenceStatusResponse retryAnalysis(UUID emailId) {
        EmailEvidence evidence = evidenceRepository.findById(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("Email evidence not found with ID: " + emailId));

        AnalysisRun latestRun = analysisRunRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("No analysis run found for evidence: " + emailId));

        if (latestRun.getStatus() == AnalysisStatus.RUNNING) {
            throw new BadRequestException("Forensic analysis is currently running for this email evidence.");
        }

        int nextAttempt = (latestRun.getAttemptNumber() != null ? latestRun.getAttemptNumber() : 1) + 1;
        AnalysisRun newRun = new AnalysisRun(
                UUID.randomUUID(),
                evidence,
                pipelineVersion,
                AnalysisStatus.QUEUED,
                "QUEUED"
        );
        newRun.setAttemptNumber(nextAttempt);
        newRun = analysisRunRepository.save(newRun);

        triggerAnalysisAfterCommit(evidence.getId(), newRun.getId());

        return getEvidenceStatus(emailId);
    }

    private void triggerAnalysisAfterCommit(UUID evidenceId, UUID runId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    analysisPipelineService.startAnalysisAsync(evidenceId, runId);
                }
            });
        } else {
            analysisPipelineService.startAnalysisAsync(evidenceId, runId);
        }
    }

    @Transactional(readOnly = true)
    public List<AttachmentMetaDto> getEvidenceAttachments(UUID emailId) {
        EmailEvidence evidence = evidenceRepository.findById(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("Email evidence not found with ID: " + emailId));

        try (InputStream inputStream = storageService.loadAsResource(evidence.getStorageKey()).getInputStream()) {
            ParsedEmailDto parsed = emailParserService.parse(inputStream);
            return parsed.getAttachments();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read evidence stream for attachments", e);
        }
    }
}

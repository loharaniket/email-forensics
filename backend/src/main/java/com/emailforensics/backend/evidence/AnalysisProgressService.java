package com.emailforensics.backend.evidence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AnalysisProgressService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisProgressService.class);

    private final AnalysisRunRepository analysisRunRepository;

    public AnalysisProgressService(AnalysisRunRepository analysisRunRepository) {
        this.analysisRunRepository = analysisRunRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AnalysisRun markRunning(UUID runId, String stage) {
        AnalysisRun run = analysisRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("AnalysisRun not found with ID: " + runId));
        run.setStatus(AnalysisStatus.RUNNING);
        run.setCurrentStage(stage);
        if (run.getStartedAt() == null) {
            run.setStartedAt(Instant.now());
        }
        return analysisRunRepository.saveAndFlush(run);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AnalysisRun updateStage(UUID runId, String stage) {
        AnalysisRun run = analysisRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("AnalysisRun not found with ID: " + runId));
        run.setCurrentStage(stage);
        return analysisRunRepository.saveAndFlush(run);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AnalysisRun markCompleted(UUID runId, String stage) {
        AnalysisRun run = analysisRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("AnalysisRun not found with ID: " + runId));
        run.setStatus(AnalysisStatus.COMPLETED);
        run.setCurrentStage(stage != null ? stage : "COMPLETED");
        run.setCompletedAt(Instant.now());
        return analysisRunRepository.saveAndFlush(run);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AnalysisRun markPartial(UUID runId, String stage, String warningMessage) {
        AnalysisRun run = analysisRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("AnalysisRun not found with ID: " + runId));
        run.setStatus(AnalysisStatus.PARTIAL);
        run.setCurrentStage(stage != null ? stage : "COMPLETED");
        run.setCompletedAt(Instant.now());
        if (warningMessage != null && !warningMessage.isBlank()) {
            run.setSafeErrorMessage(warningMessage);
        }
        return analysisRunRepository.saveAndFlush(run);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AnalysisRun markFailed(UUID runId, String stage, String errorCode, String errorMessage) {
        AnalysisRun run = analysisRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("AnalysisRun not found with ID: " + runId));
        run.setStatus(AnalysisStatus.FAILED);
        run.setCurrentStage(stage);
        run.setCompletedAt(Instant.now());
        run.setErrorCode(errorCode);
        run.setSafeErrorMessage(errorMessage);
        return analysisRunRepository.saveAndFlush(run);
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void recoverStuckRuns() {
        List<AnalysisRun> runningRuns = analysisRunRepository.findByStatus(AnalysisStatus.RUNNING);
        for (AnalysisRun run : runningRuns) {
            run.setStatus(AnalysisStatus.FAILED);
            run.setCompletedAt(Instant.now());
            run.setErrorCode("SERVER_RESTARTED");
            run.setSafeErrorMessage("The analysis pipeline was interrupted by a server restart. You may retry analysis.");
        }
        if (!runningRuns.isEmpty()) {
            analysisRunRepository.saveAllAndFlush(runningRuns);
        }

        List<AnalysisRun> queuedRuns = analysisRunRepository.findByStatus(AnalysisStatus.QUEUED);
        for (AnalysisRun run : queuedRuns) {
            run.setStatus(AnalysisStatus.FAILED);
            run.setCompletedAt(Instant.now());
            run.setErrorCode("SERVER_RESTARTED");
            run.setSafeErrorMessage("The analysis was queued prior to server restart. You may retry analysis.");
        }
        if (!queuedRuns.isEmpty()) {
            analysisRunRepository.saveAllAndFlush(queuedRuns);
        }
    }
}


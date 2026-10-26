package com.emailforensics.backend.aiassessment;

import com.emailforensics.backend.common.ResourceNotFoundException;
import com.emailforensics.backend.dto.aiassessment.AiAssessmentDto;
import com.emailforensics.backend.dto.aiassessment.AiStructuredOutput;
import com.emailforensics.backend.dto.aiassessment.EvidenceContextSummaryDto;
import com.emailforensics.backend.evidence.AnalysisRun;
import com.emailforensics.backend.evidence.AnalysisRunRepository;
import com.emailforensics.backend.evidence.EmailEvidence;
import com.emailforensics.backend.evidence.EmailEvidenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiAssessmentService {

    private static final String PROMPT_VERSION = "v1.0.0";

    private final AiGroundedAssessmentRepository assessmentRepository;
    private final EmailEvidenceRepository evidenceRepository;
    private final AnalysisRunRepository runRepository;
    private final EvidenceContextAssembler contextAssembler;
    private final AiClientService aiClientService;
    private final AiAssessmentValidator validator;

    public AiAssessmentService(AiGroundedAssessmentRepository assessmentRepository,
                               EmailEvidenceRepository evidenceRepository,
                               AnalysisRunRepository runRepository,
                               EvidenceContextAssembler contextAssembler,
                               AiClientService aiClientService,
                               AiAssessmentValidator validator) {
        this.assessmentRepository = assessmentRepository;
        this.evidenceRepository = evidenceRepository;
        this.runRepository = runRepository;
        this.contextAssembler = contextAssembler;
        this.aiClientService = aiClientService;
        this.validator = validator;
    }

    @Transactional
    public AiAssessmentDto getLatestAssessment(UUID emailId) {
        Optional<AiGroundedAssessment> opt = assessmentRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId);
        if (opt.isPresent()) {
            return mapToDto(opt.get());
        }
        synchronized (emailId.toString().intern()) {
            Optional<AiGroundedAssessment> doubleCheck = assessmentRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId);
            if (doubleCheck.isPresent()) {
                return mapToDto(doubleCheck.get());
            }
            return generateAssessment(emailId, false, null);
        }
    }

    @Transactional(readOnly = true)
    public List<AiAssessmentDto> getAssessmentHistory(UUID emailId) {
        if (!evidenceRepository.existsById(emailId)) {
            throw new ResourceNotFoundException("Evidence not found with ID: " + emailId);
        }
        List<AiGroundedAssessment> all = assessmentRepository.findAllByEmailEvidenceIdOrderByCreatedAtDesc(emailId);
        List<AiAssessmentDto> dtos = new ArrayList<>();
        for (AiGroundedAssessment entity : all) {
            dtos.add(mapToDto(entity));
        }
        return dtos;
    }

    @Transactional
    public AiAssessmentDto generateAssessment(UUID emailId, boolean forceRegenerate, String modelOverride) {
        EmailEvidence evidence = evidenceRepository.findById(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence not found with ID: " + emailId));

        AnalysisRun run = runRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("No analysis run found for evidence: " + emailId));

        if (!forceRegenerate) {
            Optional<AiGroundedAssessment> existing = assessmentRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId);
            if (existing.isPresent()) {
                return mapToDto(existing.get());
            }

            if (evidence.getSha256Hash() != null && !evidence.getSha256Hash().isBlank()) {
                Optional<AiGroundedAssessment> match = assessmentRepository
                        .findTopByEmailEvidenceSha256HashAndStatusOrderByCreatedAtDesc(evidence.getSha256Hash(), "SUCCESS");
                if (match.isPresent()) {
                    AiGroundedAssessment existingMatch = match.get();
                    AiGroundedAssessment cloned = new AiGroundedAssessment(
                            UUID.randomUUID(),
                            evidence,
                            run,
                            existingMatch.getProvider(),
                            existingMatch.getModelName(),
                            PROMPT_VERSION,
                            existingMatch.getStatus(),
                            existingMatch.getThreatClassification(),
                            existingMatch.getConfidenceAssessment(),
                            existingMatch.getSummaryExplanation(),
                            existingMatch.getKeyIndicatorsJson(),
                            existingMatch.getEvidenceReferencesJson(),
                            existingMatch.getUnsupportedReferencesJson(),
                            existingMatch.getLimitationsAndCaveats(),
                            existingMatch.isUntrustedInjectionsDetected(),
                            existingMatch.getSanitizedContextSummary(),
                            null,
                            0,
                            5L
                    );
                    cloned = assessmentRepository.save(cloned);
                    return mapToDto(cloned);
                }
            }
        }

        EvidenceContextSummaryDto contextSummary = contextAssembler.assembleContext(evidence, run);

        AiClientService.GenerationResult genResult = aiClientService.generateAssessment(contextSummary, modelOverride);

        AiAssessmentValidator.ValidationResult valResult = validator.validate(
                genResult.getOutput(),
                contextSummary.getWhitelistedReferences()
        );

        AiStructuredOutput validatedOutput = valResult.getValidatedOutput();

        String keyIndicatorsJson = serializeStringList(validatedOutput.getKeyIndicators());
        String validatedReferencesJson = serializeStringList(validatedOutput.getEvidenceReferences());
        String unsupportedReferencesJson = serializeStringList(valResult.getUnsupportedReferences());

        AiGroundedAssessment assessment = new AiGroundedAssessment(
                UUID.randomUUID(),
                evidence,
                run,
                genResult.getProvider(),
                genResult.getModelName(),
                PROMPT_VERSION,
                genResult.getStatus(),
                validatedOutput.getThreatClassification(),
                validatedOutput.getConfidenceAssessment(),
                validatedOutput.getSummaryExplanation(),
                keyIndicatorsJson,
                validatedReferencesJson,
                unsupportedReferencesJson,
                validatedOutput.getLimitations(),
                contextSummary.isUntrustedInjectionsDetected(),
                contextSummary.getSanitizedPromptContext(),
                genResult.getErrorMessage(),
                genResult.getTokensUsed(),
                genResult.getGenerationTimeMs()
        );

        assessment = assessmentRepository.save(assessment);
        return mapToDto(assessment);
    }

    private AiAssessmentDto mapToDto(AiGroundedAssessment entity) {
        return new AiAssessmentDto(
                entity.getId(),
                entity.getEmailEvidence().getId(),
                entity.getAnalysisRun().getId(),
                entity.getProvider(),
                entity.getModelName(),
                entity.getPromptVersion(),
                entity.getStatus(),
                entity.getThreatClassification(),
                entity.getConfidenceAssessment(),
                entity.getSummaryExplanation(),
                deserializeStringList(entity.getKeyIndicatorsJson()),
                deserializeStringList(entity.getEvidenceReferencesJson()),
                deserializeStringList(entity.getUnsupportedReferencesJson()),
                entity.getLimitationsAndCaveats(),
                entity.isUntrustedInjectionsDetected(),
                entity.getSanitizedContextSummary(),
                entity.getErrorMessage(),
                entity.getTokensUsed(),
                entity.getGenerationTimeMs(),
                entity.getCreatedAt()
        );
    }

    private String serializeStringList(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(list.get(i).replace("\"", "\\\"")).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private List<String> deserializeStringList(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) return new ArrayList<>();
        List<String> list = new ArrayList<>();
        Matcher m = Pattern.compile("\"((?:\\\\\"|[^\"])*)\"").matcher(json);
        while (m.find()) {
            list.add(m.group(1).replace("\\\"", "\""));
        }
        return list;
    }
}

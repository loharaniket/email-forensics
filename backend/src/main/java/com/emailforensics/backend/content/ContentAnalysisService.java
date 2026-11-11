package com.emailforensics.backend.content;

import com.emailforensics.backend.analysis.AnalysisResult;
import com.emailforensics.backend.analysis.AnalysisResultRepository;
import com.emailforensics.backend.dto.content.ContentAnalysisDto;
import com.emailforensics.backend.dto.content.PromptInjectionMatchDto;
import com.emailforensics.backend.dto.content.SocialEngineeringMatchDto;
import com.emailforensics.backend.dto.email.ParsedEmailDto;
import com.emailforensics.backend.email.EmailParserService;
import com.emailforensics.backend.evidence.AnalysisRun;
import com.emailforensics.backend.evidence.AnalysisRunRepository;
import com.emailforensics.backend.evidence.EmailEvidence;
import com.emailforensics.backend.evidence.EmailEvidenceRepository;
import com.emailforensics.backend.evidence.EvidenceStorageService;
import com.emailforensics.backend.visual.QrArtifact;
import com.emailforensics.backend.visual.QrArtifactRepository;
import com.emailforensics.backend.visual.VisualArtifact;
import com.emailforensics.backend.visual.VisualArtifactRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ContentAnalysisService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AnalysisResultRepository analysisResultRepository;
    private final EmailEvidenceRepository emailEvidenceRepository;
    private final AnalysisRunRepository analysisRunRepository;
    private final EvidenceStorageService evidenceStorageService;
    private final EmailParserService emailParserService;
    private final VisualArtifactRepository visualArtifactRepository;
    private final QrArtifactRepository qrArtifactRepository;
    private final GroqSocialEngineeringService groqSocialEngineeringService;
    private final PromptInjectionDetector promptInjectionDetector;
    private final MlClassifierService mlClassifierService;

    public ContentAnalysisService(AnalysisResultRepository analysisResultRepository,
                                  EmailEvidenceRepository emailEvidenceRepository,
                                  AnalysisRunRepository analysisRunRepository,
                                  EvidenceStorageService evidenceStorageService,
                                  EmailParserService emailParserService,
                                  VisualArtifactRepository visualArtifactRepository,
                                  QrArtifactRepository qrArtifactRepository,
                                  GroqSocialEngineeringService groqSocialEngineeringService,
                                  PromptInjectionDetector promptInjectionDetector,
                                  MlClassifierService mlClassifierService) {
        this.analysisResultRepository = analysisResultRepository;
        this.emailEvidenceRepository = emailEvidenceRepository;
        this.analysisRunRepository = analysisRunRepository;
        this.evidenceStorageService = evidenceStorageService;
        this.emailParserService = emailParserService;
        this.visualArtifactRepository = visualArtifactRepository;
        this.qrArtifactRepository = qrArtifactRepository;
        this.groqSocialEngineeringService = groqSocialEngineeringService;
        this.promptInjectionDetector = promptInjectionDetector;
        this.mlClassifierService = mlClassifierService;
    }

    @Transactional
    public ContentAnalysisDto analyzeContent(UUID emailId) {
        EmailEvidence evidence = emailEvidenceRepository.findById(emailId)
                .orElseThrow(() -> new com.emailforensics.backend.common.ResourceNotFoundException("Evidence not found: " + emailId));

        AnalysisRun run = analysisRunRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)
                .orElseThrow(() -> new com.emailforensics.backend.common.ResourceNotFoundException("No analysis run found for evidence: " + emailId));

        ParsedEmailDto parsedEmail;
        try (InputStream in = evidenceStorageService.loadAsResource(evidence.getStorageKey()).getInputStream()) {
            parsedEmail = emailParserService.parse(in);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read evidence stream for content analysis: " + e.getMessage(), e);
        }

        Map<String, String> channelTexts = new LinkedHashMap<>();

        if (parsedEmail.getSubject() != null && !parsedEmail.getSubject().isBlank()) {
            channelTexts.put("SUBJECT", parsedEmail.getSubject());
        }

        if (parsedEmail.getPlainTextBody() != null && !parsedEmail.getPlainTextBody().isBlank()) {
            channelTexts.put("BODY_PLAIN", parsedEmail.getPlainTextBody());
        }

        if (parsedEmail.getExtractedText() != null && !parsedEmail.getExtractedText().isBlank()) {
            channelTexts.put("BODY_HTML", parsedEmail.getExtractedText());
        }

        List<VisualArtifact> visualArtifacts = visualArtifactRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run.getId());
        StringBuilder qrPayloadAggregator = new StringBuilder();

        for (VisualArtifact va : visualArtifacts) {
            List<QrArtifact> qrs = qrArtifactRepository.findByVisualArtifactIdOrderByCreatedAtAsc(va.getId());
            for (QrArtifact qr : qrs) {
                if (qr.getDecodedPayload() != null && !qr.getDecodedPayload().isBlank()) {
                    if (!qrPayloadAggregator.isEmpty()) {
                        qrPayloadAggregator.append("\n");
                    }
                    qrPayloadAggregator.append(qr.getDecodedPayload());
                }
            }
        }

        if (!qrPayloadAggregator.isEmpty()) {
            channelTexts.put("QR_PAYLOAD", qrPayloadAggregator.toString());
        }

        GroqSocialEngineeringService.SocialEngineeringFindings seFindings =
                groqSocialEngineeringService.analyze(channelTexts);

        PromptInjectionDetector.PromptInjectionFindings piFindings =
                promptInjectionDetector.detect(channelTexts);

        StringBuilder combinedTextForMl = new StringBuilder();
        for (String text : channelTexts.values()) {
            combinedTextForMl.append(text).append("\n");
        }

        MlClassifierService.PredictionResult mlResult =
                mlClassifierService.predict(combinedTextForMl.toString());

        String promptMatchesJson = formatPromptMatchesJson(piFindings.getMatches());
        String seMatchesJson = formatSocialEngineeringMatchesJson(seFindings.getMatches());

        AnalysisResult contentAnalysis = analysisResultRepository.findByAnalysisRunId(run.getId())
                .orElseGet(() -> new AnalysisResult(run));

        if (contentAnalysis.getContentResultId() == null) {
            contentAnalysis.setContentResultId(UUID.randomUUID());
        }
        if (contentAnalysis.getContentCreatedAt() == null) {
            contentAnalysis.setContentCreatedAt(Instant.now());
        }
        contentAnalysis.setAnalysisRun(run);
        contentAnalysis.setMlModelVersion(mlClassifierService.getModelVersion());
        contentAnalysis.setProbability(mlResult.getProbability());
        contentAnalysis.setPredictedLabel(mlResult.getPredictedLabel());
        contentAnalysis.setMlStatus(mlResult.getMlStatus());
        contentAnalysis.setUrgency(seFindings.isUrgency());
        contentAnalysis.setCredentialRequest(seFindings.isCredentialRequest());
        contentAnalysis.setPaymentInstruction(seFindings.isPaymentInstruction());
        contentAnalysis.setImpersonation(seFindings.isImpersonation());
        contentAnalysis.setSecrecy(seFindings.isSecrecy());
        contentAnalysis.setPromptInjectionDetected(piFindings.isPromptInjectionDetected());
        contentAnalysis.setPromptMatchesJson(promptMatchesJson);
        contentAnalysis.setSocialEngineeringMatchesJson(seMatchesJson);

        AnalysisResult saved = analysisResultRepository.save(contentAnalysis);

        return toDto(saved, piFindings.getMatches(), seFindings.getMatches(), seFindings.getStatus(), seFindings.getStatusExplanation());
    }

    @Transactional(readOnly = true)
    public Optional<ContentAnalysisDto> getContentAnalysis(UUID emailId) {
        EmailEvidence evidence = emailEvidenceRepository.findById(emailId).orElse(null);
        if (evidence == null) {
            return Optional.empty();
        }

        AnalysisRun run = analysisRunRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)
                .orElse(null);

        if (run == null) {
            return Optional.empty();
        }

        Optional<AnalysisResult> entityOpt = analysisResultRepository.findByAnalysisRunId(run.getId());
        if (entityOpt.isEmpty() || !entityOpt.get().hasContentAnalysis()) {
            return Optional.empty();
        }

        AnalysisResult entity = entityOpt.get();
        List<PromptInjectionMatchDto> promptMatches = parsePromptMatchesJson(entity.getPromptMatchesJson());
        List<SocialEngineeringMatchDto> seMatches = parseSocialEngineeringMatchesJson(entity.getSocialEngineeringMatchesJson());

        return Optional.of(toDto(entity, promptMatches, seMatches, "EVALUATED", null));
    }

    private ContentAnalysisDto toDto(AnalysisResult entity,
                                     List<PromptInjectionMatchDto> promptMatches,
                                     List<SocialEngineeringMatchDto> seMatches,
                                     String seStatus,
                                     String seExplanation) {
        ContentAnalysisDto dto = new ContentAnalysisDto();
        dto.setId(entity.getContentResultId());
        dto.setMlModelVersion(entity.getMlModelVersion());
        dto.setProbability(entity.getProbability());
        dto.setPredictedLabel(entity.getPredictedLabel());
        dto.setMlStatus(entity.getMlStatus());
        dto.setModelMetrics(mlClassifierService.getModelMetrics());
        dto.setUrgency(entity.isUrgency());
        dto.setCredentialRequest(entity.isCredentialRequest());
        dto.setPaymentInstruction(entity.isPaymentInstruction());
        dto.setImpersonation(entity.isImpersonation());
        dto.setSecrecy(entity.isSecrecy());
        dto.setPromptInjectionDetected(entity.isPromptInjectionDetected());
        dto.setPromptMatches(promptMatches != null ? promptMatches : Collections.emptyList());
        dto.setSocialEngineeringMatches(seMatches != null ? seMatches : Collections.emptyList());
        dto.setSocialEngineeringStatus(seStatus != null ? seStatus : "EVALUATED");
        dto.setSocialEngineeringExplanation(seExplanation);
        dto.setCreatedAt(entity.getContentCreatedAt());
        return dto;
    }

    private String formatPromptMatchesJson(List<PromptInjectionMatchDto> list) {
        if (list == null || list.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<PromptInjectionMatchDto> parsePromptMatchesJson(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<PromptInjectionMatchDto>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String formatSocialEngineeringMatchesJson(List<SocialEngineeringMatchDto> list) {
        if (list == null || list.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<SocialEngineeringMatchDto> parseSocialEngineeringMatchesJson(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<SocialEngineeringMatchDto>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}

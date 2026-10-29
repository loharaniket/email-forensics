package com.emailforensics.backend.content;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ContentAnalysisService {

    private static final Pattern OBJECT_PATTERN = Pattern.compile("\\{([^{}]+)\\}");
    private static final Pattern FIELD_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"");

    private final ContentAnalysisRepository contentAnalysisRepository;
    private final EmailEvidenceRepository emailEvidenceRepository;
    private final AnalysisRunRepository analysisRunRepository;
    private final EvidenceStorageService evidenceStorageService;
    private final EmailParserService emailParserService;
    private final VisualArtifactRepository visualArtifactRepository;
    private final QrArtifactRepository qrArtifactRepository;
    private final SocialEngineeringRuleEngine socialEngineeringRuleEngine;
    private final PromptInjectionDetector promptInjectionDetector;
    private final TribuoBaselineService tribuoBaselineService;

    public ContentAnalysisService(ContentAnalysisRepository contentAnalysisRepository,
                                  EmailEvidenceRepository emailEvidenceRepository,
                                  AnalysisRunRepository analysisRunRepository,
                                  EvidenceStorageService evidenceStorageService,
                                  EmailParserService emailParserService,
                                  VisualArtifactRepository visualArtifactRepository,
                                  QrArtifactRepository qrArtifactRepository,
                                  SocialEngineeringRuleEngine socialEngineeringRuleEngine,
                                  PromptInjectionDetector promptInjectionDetector,
                                  TribuoBaselineService tribuoBaselineService) {
        this.contentAnalysisRepository = contentAnalysisRepository;
        this.emailEvidenceRepository = emailEvidenceRepository;
        this.analysisRunRepository = analysisRunRepository;
        this.evidenceStorageService = evidenceStorageService;
        this.emailParserService = emailParserService;
        this.visualArtifactRepository = visualArtifactRepository;
        this.qrArtifactRepository = qrArtifactRepository;
        this.socialEngineeringRuleEngine = socialEngineeringRuleEngine;
        this.promptInjectionDetector = promptInjectionDetector;
        this.tribuoBaselineService = tribuoBaselineService;
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

        if (parsedEmail.getPlainTextBody() != null && !parsedEmail.getPlainTextBody().isBlank()) {
            channelTexts.put("BODY_TEXT", parsedEmail.getPlainTextBody());
        }

        if (parsedEmail.getExtractedText() != null && !parsedEmail.getExtractedText().isBlank()) {
            channelTexts.put("HTML_TEXT", parsedEmail.getExtractedText());
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

        SocialEngineeringRuleEngine.SocialEngineeringFindings seFindings =
                socialEngineeringRuleEngine.analyze(channelTexts);

        PromptInjectionDetector.PromptInjectionFindings piFindings =
                promptInjectionDetector.detect(channelTexts);

        StringBuilder combinedTextForMl = new StringBuilder();
        if (parsedEmail.getSubject() != null && !parsedEmail.getSubject().isBlank()) {
            combinedTextForMl.append(parsedEmail.getSubject()).append("\n");
        }
        for (String text : channelTexts.values()) {
            combinedTextForMl.append(text).append("\n");
        }

        TribuoBaselineService.PredictionResult mlResult =
                tribuoBaselineService.predict(combinedTextForMl.toString());

        String promptMatchesJson = formatPromptMatchesJson(piFindings.getMatches());
        String seMatchesJson = formatSocialEngineeringMatchesJson(seFindings.getMatches());

        ContentAnalysis contentAnalysis = contentAnalysisRepository.findByAnalysisRunId(run.getId())
                .orElseGet(ContentAnalysis::new);

        if (contentAnalysis.getId() == null) {
            contentAnalysis.setId(UUID.randomUUID());
        }
        contentAnalysis.setAnalysisRun(run);
        contentAnalysis.setMlModelVersion(tribuoBaselineService.getModelVersion());
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

        ContentAnalysis saved = contentAnalysisRepository.save(contentAnalysis);

        return toDto(saved, piFindings.getMatches(), seFindings.getMatches());
    }

    @Transactional
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

        Optional<ContentAnalysis> entityOpt = contentAnalysisRepository.findByAnalysisRunId(run.getId());
        if (entityOpt.isEmpty()) {
            return Optional.of(analyzeContent(emailId));
        }

        ContentAnalysis entity = entityOpt.get();
        List<PromptInjectionMatchDto> promptMatches = parsePromptMatchesJson(entity.getPromptMatchesJson());
        List<SocialEngineeringMatchDto> seMatches = parseSocialEngineeringMatchesJson(entity.getSocialEngineeringMatchesJson());

        return Optional.of(toDto(entity, promptMatches, seMatches));
    }

    private ContentAnalysisDto toDto(ContentAnalysis entity,
                                     List<PromptInjectionMatchDto> promptMatches,
                                     List<SocialEngineeringMatchDto> seMatches) {
        ContentAnalysisDto dto = new ContentAnalysisDto();
        dto.setId(entity.getId());
        dto.setMlModelVersion(entity.getMlModelVersion());
        dto.setProbability(entity.getProbability());
        dto.setPredictedLabel(entity.getPredictedLabel());
        dto.setMlStatus(entity.getMlStatus());
        dto.setModelMetrics(tribuoBaselineService.getModelMetrics());
        dto.setUrgency(entity.isUrgency());
        dto.setCredentialRequest(entity.isCredentialRequest());
        dto.setPaymentInstruction(entity.isPaymentInstruction());
        dto.setImpersonation(entity.isImpersonation());
        dto.setSecrecy(entity.isSecrecy());
        dto.setPromptInjectionDetected(entity.isPromptInjectionDetected());
        dto.setPromptMatches(promptMatches != null ? promptMatches : Collections.emptyList());
        dto.setSocialEngineeringMatches(seMatches != null ? seMatches : Collections.emptyList());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    private String formatPromptMatchesJson(List<PromptInjectionMatchDto> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            PromptInjectionMatchDto m = list.get(i);
            sb.append("{")
                    .append("\"category\":\"").append(escapeJson(m.getCategory())).append("\",")
                    .append("\"matchedPattern\":\"").append(escapeJson(m.getMatchedPattern())).append("\",")
                    .append("\"sourceChannel\":\"").append(escapeJson(m.getSourceChannel())).append("\",")
                    .append("\"explanation\":\"").append(escapeJson(m.getExplanation())).append("\",")
                    .append("\"severity\":\"").append(escapeJson(m.getSeverity())).append("\"")
                    .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private List<PromptInjectionMatchDto> parsePromptMatchesJson(String json) {
        List<PromptInjectionMatchDto> list = new ArrayList<>();
        if (json == null || json.trim().isEmpty() || json.equals("[]")) return list;
        Matcher om = OBJECT_PATTERN.matcher(json);
        while (om.find()) {
            Map<String, String> map = parseFieldMap(om.group(1));
            list.add(new PromptInjectionMatchDto(
                    map.get("category"),
                    map.get("matchedPattern"),
                    map.get("sourceChannel"),
                    map.get("explanation"),
                    map.get("severity")
            ));
        }
        return list;
    }

    private String formatSocialEngineeringMatchesJson(List<SocialEngineeringMatchDto> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            SocialEngineeringMatchDto m = list.get(i);
            sb.append("{")
                    .append("\"category\":\"").append(escapeJson(m.getCategory())).append("\",")
                    .append("\"matchedPattern\":\"").append(escapeJson(m.getMatchedPattern())).append("\",")
                    .append("\"sourceChannel\":\"").append(escapeJson(m.getSourceChannel())).append("\",")
                    .append("\"explanation\":\"").append(escapeJson(m.getExplanation())).append("\",")
                    .append("\"severity\":\"").append(escapeJson(m.getSeverity())).append("\"")
                    .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private List<SocialEngineeringMatchDto> parseSocialEngineeringMatchesJson(String json) {
        List<SocialEngineeringMatchDto> list = new ArrayList<>();
        if (json == null || json.trim().isEmpty() || json.equals("[]")) return list;
        Matcher om = OBJECT_PATTERN.matcher(json);
        while (om.find()) {
            Map<String, String> map = parseFieldMap(om.group(1));
            list.add(new SocialEngineeringMatchDto(
                    map.get("category"),
                    map.get("matchedPattern"),
                    map.get("sourceChannel"),
                    map.get("explanation"),
                    map.get("severity")
            ));
        }
        return list;
    }

    private Map<String, String> parseFieldMap(String objContent) {
        Map<String, String> map = new HashMap<>();
        Matcher fm = FIELD_PATTERN.matcher(objContent);
        while (fm.find()) {
            map.put(fm.group(1), unescapeJson(fm.group(2)));
        }
        return map;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\b", "\b")
                .replace("\\f", "\f")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }
}

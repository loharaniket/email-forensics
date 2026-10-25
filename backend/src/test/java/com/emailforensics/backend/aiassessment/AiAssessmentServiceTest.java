package com.emailforensics.backend.aiassessment;

import com.emailforensics.backend.casefile.CaseFile;
import com.emailforensics.backend.dto.aiassessment.AiAssessmentDto;
import com.emailforensics.backend.dto.aiassessment.AiStructuredOutput;
import com.emailforensics.backend.dto.aiassessment.EvidenceContextSummaryDto;
import com.emailforensics.backend.evidence.AnalysisRun;
import com.emailforensics.backend.evidence.AnalysisRunRepository;
import com.emailforensics.backend.evidence.AnalysisStatus;
import com.emailforensics.backend.evidence.EmailEvidence;
import com.emailforensics.backend.evidence.EmailEvidenceRepository;
import com.emailforensics.backend.user.Role;
import com.emailforensics.backend.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAssessmentServiceTest {

    @Mock
    private AiGroundedAssessmentRepository assessmentRepository;

    @Mock
    private EmailEvidenceRepository evidenceRepository;

    @Mock
    private AnalysisRunRepository runRepository;

    @Mock
    private EvidenceContextAssembler contextAssembler;

    @Mock
    private AiClientService aiClientService;

    @Mock
    private AiAssessmentValidator validator;

    private AiAssessmentService service;

    private User analyst;
    private CaseFile caseFile;
    private EmailEvidence evidence;
    private AnalysisRun run;

    @BeforeEach
    void setUp() {
        service = new AiAssessmentService(
                assessmentRepository,
                evidenceRepository,
                runRepository,
                contextAssembler,
                aiClientService,
                validator
        );

        analyst = new User(UUID.randomUUID(), "analyst", "analyst@example.com", "pass", Role.ANALYST, true, java.time.Instant.now());
        caseFile = new CaseFile(UUID.randomUUID(), "CF-2026-001", "Investigation 1", "Desc", com.emailforensics.backend.casefile.CaseStatus.OPEN, "notes", analyst, java.time.Instant.now(), java.time.Instant.now());
        evidence = new EmailEvidence(UUID.randomUUID(), caseFile, "sample.eml", "storage-1", 1024L, "hash123", analyst);
        run = new AnalysisRun(UUID.randomUUID(), evidence, "v1.0.0", AnalysisStatus.COMPLETED, "COMPLETED");
    }

    @Test
    void testGenerateAssessmentSavesAndReturnsDto() {
        UUID emailId = evidence.getId();

        when(evidenceRepository.findById(emailId)).thenReturn(Optional.of(evidence));
        when(runRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)).thenReturn(Optional.of(run));

        EvidenceContextSummaryDto summary = new EvidenceContextSummaryDto(
                List.of("HEADER:From", "RISK:SCORE"),
                "sanitized context",
                false
        );
        when(contextAssembler.assembleContext(evidence, run)).thenReturn(summary);

        AiStructuredOutput rawOutput = new AiStructuredOutput(
                "MALICIOUS_PHISHING",
                "HIGH",
                "Phishing threat detected.",
                List.of("Suspicious URL"),
                List.of("HEADER:From", "HALLUCINATED_REF"),
                "None"
        );
        AiClientService.GenerationResult genResult = new AiClientService.GenerationResult(
                rawOutput,
                "openai",
                "gpt-4o-mini",
                "SUCCESS",
                null,
                150,
                350L
        );
        when(aiClientService.generateAssessment(summary, null)).thenReturn(genResult);

        AiStructuredOutput validatedOutput = new AiStructuredOutput(
                "MALICIOUS_PHISHING",
                "HIGH",
                "Phishing threat detected.",
                List.of("Suspicious URL"),
                List.of("HEADER:From"),
                "None"
        );
        AiAssessmentValidator.ValidationResult valResult = new AiAssessmentValidator.ValidationResult(
                validatedOutput,
                List.of("HALLUCINATED_REF")
        );
        when(validator.validate(rawOutput, summary.getWhitelistedReferences())).thenReturn(valResult);

        when(assessmentRepository.save(any(AiGroundedAssessment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AiAssessmentDto dto = service.generateAssessment(emailId, true, null);

        assertNotNull(dto);
        assertEquals("MALICIOUS_PHISHING", dto.getThreatClassification());
        assertEquals("HIGH", dto.getConfidenceAssessment());
        assertEquals(1, dto.getEvidenceReferences().size());
        assertEquals("HEADER:From", dto.getEvidenceReferences().get(0));
        assertEquals(1, dto.getUnsupportedReferences().size());
        assertEquals("HALLUCINATED_REF", dto.getUnsupportedReferences().get(0));

        verify(assessmentRepository).save(any(AiGroundedAssessment.class));
    }

    @Test
    void testGetAssessmentHistoryRetainsRegenerations() {
        UUID emailId = evidence.getId();
        when(evidenceRepository.existsById(emailId)).thenReturn(true);

        AiGroundedAssessment a1 = new AiGroundedAssessment(
                UUID.randomUUID(), evidence, run, "openai", "gpt-4o-mini", "v1.0.0",
                "SUCCESS", "MALICIOUS_PHISHING", "HIGH", "Summary 1", "[]", "[]", "[]",
                "Limitations", false, "Context", null, 100, 200L
        );
        AiGroundedAssessment a2 = new AiGroundedAssessment(
                UUID.randomUUID(), evidence, run, "grounded-offline", "grounded-heuristic-v1", "v1.0.0",
                "FALLBACK", "MALICIOUS_PHISHING", "MEDIUM", "Summary 2", "[]", "[]", "[]",
                "Limitations", false, "Context", null, 0, 50L
        );

        when(assessmentRepository.findAllByEmailEvidenceIdOrderByCreatedAtDesc(emailId))
                .thenReturn(List.of(a1, a2));

        List<AiAssessmentDto> history = service.getAssessmentHistory(emailId);

        assertEquals(2, history.size());
        assertEquals("Summary 1", history.get(0).getSummaryExplanation());
        assertEquals("Summary 2", history.get(1).getSummaryExplanation());
    }
}

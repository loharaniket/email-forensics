package com.emailforensics.backend.aiassessment;

import com.emailforensics.backend.dto.aiassessment.AiAssessmentDto;
import com.emailforensics.backend.dto.aiassessment.AiGenerationRequestDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/evidence")
public class AiAssessmentController {

    private final AiAssessmentService aiAssessmentService;

    public AiAssessmentController(AiAssessmentService aiAssessmentService) {
        this.aiAssessmentService = aiAssessmentService;
    }

    @GetMapping("/{emailId}/ai-assessment")
    public ResponseEntity<AiAssessmentDto> getLatestAssessment(@PathVariable UUID emailId) {
        AiAssessmentDto dto = aiAssessmentService.getLatestAssessment(emailId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{emailId}/ai-assessments/history")
    public ResponseEntity<List<AiAssessmentDto>> getAssessmentHistory(@PathVariable UUID emailId) {
        List<AiAssessmentDto> history = aiAssessmentService.getAssessmentHistory(emailId);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/{emailId}/ai-assessment/generate")
    public ResponseEntity<AiAssessmentDto> generateAssessment(@PathVariable UUID emailId,
                                                              @RequestBody(required = false) AiGenerationRequestDto request) {
        boolean force = request != null && request.isForceRegenerate();
        String modelOverride = request != null ? request.getModelOverride() : null;
        AiAssessmentDto dto = aiAssessmentService.generateAssessment(emailId, force, modelOverride);
        return ResponseEntity.ok(dto);
    }
}

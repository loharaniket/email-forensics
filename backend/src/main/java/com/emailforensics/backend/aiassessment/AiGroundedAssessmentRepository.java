package com.emailforensics.backend.aiassessment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiGroundedAssessmentRepository extends JpaRepository<AiGroundedAssessment, UUID> {

    Optional<AiGroundedAssessment> findTopByEmailEvidenceIdOrderByCreatedAtDesc(UUID emailEvidenceId);

    Optional<AiGroundedAssessment> findTopByAnalysisRunIdOrderByCreatedAtDesc(UUID analysisRunId);

    Optional<AiGroundedAssessment> findTopByEmailEvidenceSha256HashAndStatusOrderByCreatedAtDesc(String sha256Hash, String status);
}

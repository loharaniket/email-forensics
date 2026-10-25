package com.emailforensics.backend.risk;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, UUID> {

    Optional<RiskAssessment> findByAnalysisRunId(UUID analysisRunId);

    @Query("SELECT ra FROM RiskAssessment ra WHERE ra.analysisRun.emailEvidence.id = :emailId ORDER BY ra.evaluatedAt DESC")
    Optional<RiskAssessment> findTopByEmailEvidenceId(@Param("emailId") UUID emailId);
}

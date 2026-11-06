package com.emailforensics.backend.analysis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, UUID> {

    Optional<AnalysisResult> findByAnalysisRunId(UUID analysisRunId);

    List<AnalysisResult> findByFromDomain(String fromDomain);

    void deleteByAnalysisRunId(UUID analysisRunId);

    @Query("SELECT ar FROM AnalysisResult ar WHERE ar.analysisRun.emailEvidence.id = :emailId ORDER BY ar.evaluatedAt DESC")
    Optional<AnalysisResult> findTopByEmailEvidenceId(@Param("emailId") UUID emailId);
}


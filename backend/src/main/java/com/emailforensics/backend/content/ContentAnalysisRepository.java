package com.emailforensics.backend.content;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentAnalysisRepository extends JpaRepository<ContentAnalysis, UUID> {
    Optional<ContentAnalysis> findByAnalysisRunId(UUID analysisRunId);
    void deleteByAnalysisRunId(UUID analysisRunId);
}

package com.emailforensics.backend.visual;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VisualArtifactRepository extends JpaRepository<VisualArtifact, UUID> {
    List<VisualArtifact> findByAnalysisRunIdOrderByCreatedAtAsc(UUID analysisRunId);
}

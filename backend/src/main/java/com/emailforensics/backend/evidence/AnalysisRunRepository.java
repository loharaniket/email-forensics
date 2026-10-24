package com.emailforensics.backend.evidence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnalysisRunRepository extends JpaRepository<AnalysisRun, UUID> {

    Optional<AnalysisRun> findTopByEmailEvidenceIdOrderByCreatedAtDesc(UUID emailId);

    List<AnalysisRun> findByEmailEvidenceIdOrderByCreatedAtDesc(UUID emailId);
}

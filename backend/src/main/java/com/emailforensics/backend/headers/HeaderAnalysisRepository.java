package com.emailforensics.backend.headers;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HeaderAnalysisRepository extends JpaRepository<HeaderAnalysis, UUID> {

    Optional<HeaderAnalysis> findByAnalysisRunId(UUID analysisRunId);

    List<HeaderAnalysis> findByFromDomain(String fromDomain);
}

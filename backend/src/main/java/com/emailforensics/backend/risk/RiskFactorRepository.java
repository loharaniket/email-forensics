package com.emailforensics.backend.risk;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RiskFactorRepository extends JpaRepository<RiskFactor, UUID> {

    List<RiskFactor> findByAnalysisRunIdOrderByCreatedAtAsc(UUID analysisRunId);

    void deleteByAnalysisRunId(UUID analysisRunId);
}

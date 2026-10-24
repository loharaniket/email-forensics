package com.emailforensics.backend.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IpArtifactRepository extends JpaRepository<IpArtifact, UUID> {

    List<IpArtifact> findByAnalysisRunIdOrderByCreatedAtAsc(UUID analysisRunId);

    List<IpArtifact> findByIpAddress(String ipAddress);
}

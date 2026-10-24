package com.emailforensics.backend.headers;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReceivedHopRepository extends JpaRepository<ReceivedHop, UUID> {

    List<ReceivedHop> findByAnalysisRunIdOrderByHopOrderAsc(UUID analysisRunId);
}

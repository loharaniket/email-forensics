package com.emailforensics.backend.correlation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HistoricalMatchRepository extends JpaRepository<HistoricalMatch, UUID> {

    List<HistoricalMatch> findBySourceRunId(UUID sourceRunId);

    @Query("SELECT hm FROM HistoricalMatch hm WHERE hm.sourceRun.emailEvidence.id = :emailId ORDER BY hm.createdAt DESC")
    List<HistoricalMatch> findBySourceEmailId(@Param("emailId") UUID emailId);

    boolean existsBySourceRunIdAndMatchedEmailIdAndIndicatorTypeAndNormalizedIndicatorValue(
            UUID sourceRunId, UUID matchedEmailId, String indicatorType, String normalizedIndicatorValue
    );

    void deleteBySourceRunId(UUID sourceRunId);
}

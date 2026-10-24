package com.emailforensics.backend.urlintel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExtractedUrlRepository extends JpaRepository<ExtractedUrl, UUID> {

    List<ExtractedUrl> findByAnalysisRunIdOrderByCreatedAtAsc(UUID analysisRunId);

    List<ExtractedUrl> findByNormalizedUrl(String normalizedUrl);

    List<ExtractedUrl> findByDomain(String domain);
}

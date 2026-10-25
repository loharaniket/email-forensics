package com.emailforensics.backend.visual;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QrArtifactRepository extends JpaRepository<QrArtifact, UUID> {
    List<QrArtifact> findByVisualArtifactIdOrderByCreatedAtAsc(UUID visualArtifactId);

    List<QrArtifact> findByDecodedPayload(String decodedPayload);
}

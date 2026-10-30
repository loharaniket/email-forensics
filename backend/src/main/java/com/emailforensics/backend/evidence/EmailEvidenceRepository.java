package com.emailforensics.backend.evidence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailEvidenceRepository extends JpaRepository<EmailEvidence, UUID> {

    Page<EmailEvidence> findByCaseFileIdOrderByUploadedAtDesc(UUID caseId, Pageable pageable);

    List<EmailEvidence> findByCaseFileIdOrderByUploadedAtDesc(UUID caseId);

    Optional<EmailEvidence> findBySha256Hash(String sha256Hash);

    Optional<EmailEvidence> findFirstBySha256HashOrderByUploadedAtAsc(String sha256Hash);

    List<EmailEvidence> findAllBySha256Hash(String sha256Hash);

    Optional<EmailEvidence> findByStorageKey(String storageKey);
}

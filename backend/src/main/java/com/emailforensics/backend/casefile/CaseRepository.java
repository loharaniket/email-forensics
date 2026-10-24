package com.emailforensics.backend.casefile;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CaseRepository extends JpaRepository<CaseFile, UUID> {

    @EntityGraph(attributePaths = {"createdBy"})
    Page<CaseFile> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"createdBy"})
    Page<CaseFile> findByStatus(CaseStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"createdBy"})
    Optional<CaseFile> findById(UUID id);

    boolean existsByCaseNumber(String caseNumber);

    long count();
}

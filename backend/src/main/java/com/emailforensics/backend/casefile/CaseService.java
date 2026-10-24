package com.emailforensics.backend.casefile;

import com.emailforensics.backend.common.BadRequestException;
import com.emailforensics.backend.common.ResourceNotFoundException;
import com.emailforensics.backend.dto.casefile.CaseDetailDto;
import com.emailforensics.backend.dto.casefile.CaseSummaryDto;
import com.emailforensics.backend.dto.casefile.CreateCaseRequest;
import com.emailforensics.backend.dto.casefile.UpdateCaseNotesRequest;
import com.emailforensics.backend.dto.common.PageResponse;
import com.emailforensics.backend.security.UserPrincipal;
import com.emailforensics.backend.user.User;
import com.emailforensics.backend.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Year;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class CaseService {

    private final CaseRepository caseRepository;
    private final UserRepository userRepository;

    public CaseService(CaseRepository caseRepository, UserRepository userRepository) {
        this.caseRepository = caseRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CaseDetailDto createCase(CreateCaseRequest request, UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + principal.getId()));

        String caseNumber = generateCaseNumber();

        CaseFile caseFile = new CaseFile(
                UUID.randomUUID(),
                caseNumber,
                request.getTitle().trim(),
                request.getDescription() != null ? request.getDescription().trim() : null,
                CaseStatus.OPEN,
                null,
                user,
                Instant.now(),
                Instant.now()
        );

        CaseFile saved = caseRepository.save(caseFile);
        return mapToDetailDto(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<CaseSummaryDto> getCases(int page, int size, CaseStatus status) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<CaseFile> casePage;
        if (status != null) {
            casePage = caseRepository.findByStatus(status, pageable);
        } else {
            casePage = caseRepository.findAll(pageable);
        }

        List<CaseSummaryDto> content = casePage.getContent().stream()
                .map(this::mapToSummaryDto)
                .toList();

        return new PageResponse<>(
                content,
                casePage.getNumber(),
                casePage.getSize(),
                casePage.getTotalElements(),
                casePage.getTotalPages(),
                casePage.isLast()
        );
    }

    @Transactional(readOnly = true)
    public CaseDetailDto getCaseById(UUID caseId) {
        CaseFile caseFile = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found with ID: " + caseId));
        return mapToDetailDto(caseFile);
    }

    @Transactional
    public CaseDetailDto updateCaseNotes(UUID caseId, UpdateCaseNotesRequest request) {
        CaseFile caseFile = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found with ID: " + caseId));

        validateStatusTransition(caseFile.getStatus(), request.getStatus());

        caseFile.setAnalystNotes(request.getAnalystNotes());
        caseFile.setStatus(request.getStatus());
        caseFile.setUpdatedAt(Instant.now());

        CaseFile updated = caseRepository.save(caseFile);
        return mapToDetailDto(updated);
    }

    @Transactional(readOnly = true)
    public PageResponse<Object> getCaseEvidence(UUID caseId, int page, int size) {
        if (!caseRepository.existsById(caseId)) {
            throw new ResourceNotFoundException("Case not found with ID: " + caseId);
        }

        return new PageResponse<>(
                Collections.emptyList(),
                page,
                size,
                0,
                0,
                true
        );
    }

    private void validateStatusTransition(CaseStatus currentStatus, CaseStatus targetStatus) {
        if (targetStatus == null) {
            throw new BadRequestException("Target status cannot be null");
        }
        if (currentStatus == targetStatus) {
            return;
        }

        boolean allowed = switch (currentStatus) {
            case OPEN -> targetStatus == CaseStatus.IN_REVIEW || targetStatus == CaseStatus.CLOSED;
            case IN_REVIEW -> targetStatus == CaseStatus.OPEN || targetStatus == CaseStatus.CLOSED;
            case CLOSED -> targetStatus == CaseStatus.OPEN || targetStatus == CaseStatus.IN_REVIEW;
        };

        if (!allowed) {
            throw new BadRequestException("Invalid status transition from " + currentStatus + " to " + targetStatus);
        }
    }

    private String generateCaseNumber() {
        int year = Year.now().getValue();
        long count = caseRepository.count() + 1;
        String candidate = String.format("CASE-%d-%04d", year, count);

        while (caseRepository.existsByCaseNumber(candidate)) {
            count++;
            candidate = String.format("CASE-%d-%04d", year, count);
        }
        return candidate;
    }

    private CaseSummaryDto mapToSummaryDto(CaseFile caseFile) {
        return new CaseSummaryDto(
                caseFile.getId(),
                caseFile.getCaseNumber(),
                caseFile.getTitle(),
                caseFile.getDescription(),
                caseFile.getStatus(),
                caseFile.getCreatedBy() != null ? caseFile.getCreatedBy().getUsername() : null,
                caseFile.getCreatedAt(),
                caseFile.getUpdatedAt()
        );
    }

    private CaseDetailDto mapToDetailDto(CaseFile caseFile) {
        return new CaseDetailDto(
                caseFile.getId(),
                caseFile.getCaseNumber(),
                caseFile.getTitle(),
                caseFile.getDescription(),
                caseFile.getStatus(),
                caseFile.getAnalystNotes(),
                caseFile.getCreatedBy() != null ? caseFile.getCreatedBy().getUsername() : null,
                caseFile.getCreatedAt(),
                caseFile.getUpdatedAt(),
                Collections.emptyList()
        );
    }
}

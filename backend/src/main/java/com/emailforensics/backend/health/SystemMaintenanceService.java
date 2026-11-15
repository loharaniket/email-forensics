package com.emailforensics.backend.health;

import com.emailforensics.backend.evidence.EvidenceStorageService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SystemMaintenanceService {

    @PersistenceContext
    private EntityManager entityManager;

    private final EvidenceStorageService evidenceStorageService;

    public SystemMaintenanceService(EvidenceStorageService evidenceStorageService) {
        this.evidenceStorageService = evidenceStorageService;
    }

    @Transactional
    public TruncateResultDto truncateAllTablesAndEvidence() {
        List<String> tables = List.of(
                "indicators",
                "emails"
        );

        String sql = "TRUNCATE TABLE " + String.join(", ", tables) + " RESTART IDENTITY CASCADE";
        entityManager.createNativeQuery(sql).executeUpdate();
        entityManager.clear();

        int deletedFiles = evidenceStorageService.clearAllStoredFiles();

        return new TruncateResultDto(
                "SUCCESS",
                "All database tables and vaulted evidence files have been truncated and reset.",
                tables,
                deletedFiles
        );
    }
}

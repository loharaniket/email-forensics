package com.emailforensics.backend.health;

import com.emailforensics.backend.config.DataSeeder;
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
    private final DataSeeder dataSeeder;

    public SystemMaintenanceService(
            EvidenceStorageService evidenceStorageService,
            DataSeeder dataSeeder) {
        this.evidenceStorageService = evidenceStorageService;
        this.dataSeeder = dataSeeder;
    }

    @Transactional
    public TruncateResultDto truncateAllTablesAndEvidence() {
        List<String> tables = List.of(
                "ai_grounded_assessments",
                "historical_matches",
                "qr_artifacts",
                "visual_artifacts",
                "ip_artifacts",
                "extracted_urls",
                "received_hops",
                "risk_factors",
                "analysis_results",
                "analysis_runs",
                "email_evidence",
                "fore_cases",
                "users"
        );

        String sql = "TRUNCATE TABLE " + String.join(", ", tables) + " RESTART IDENTITY CASCADE";
        entityManager.createNativeQuery(sql).executeUpdate();
        entityManager.clear();

        // Reseed default users immediately so features continue working without interruption
        dataSeeder.reseedDefaultUsers();

        // Clear all evidence files from the storage vault
        int deletedFiles = evidenceStorageService.clearAllStoredFiles();

        return new TruncateResultDto(
                "SUCCESS",
                "All database tables and vaulted evidence files have been truncated and reset.",
                tables,
                deletedFiles
        );
    }
}


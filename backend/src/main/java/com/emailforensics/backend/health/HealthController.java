package com.emailforensics.backend.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final HealthService healthService;
    private final SystemMaintenanceService maintenanceService;

    public HealthController(HealthService healthService, SystemMaintenanceService maintenanceService) {
        this.healthService = healthService;
        this.maintenanceService = maintenanceService;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> checkHealth() {
        HealthResponse response = healthService.getHealth();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/v1/health")
    public ResponseEntity<HealthResponse> checkHealthLegacy() {
        HealthResponse response = healthService.getHealth();
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/system/truncate-all", "/health/truncate", "/v1/system/truncate-all"})
    public ResponseEntity<TruncateResultDto> truncateAll() {
        TruncateResultDto result = maintenanceService.truncateAllTablesAndEvidence();
        return ResponseEntity.ok(result);
    }
}

package com.emailforensics.backend.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private final SystemMaintenanceService maintenanceService;

    public HealthController(SystemMaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> checkHealth() {
        return ResponseEntity.ok(new HealthResponse("ok"));
    }

    @PostMapping({"/system/truncate-all", "/health/truncate"})
    public ResponseEntity<TruncateResultDto> truncateAll() {
        TruncateResultDto result = maintenanceService.truncateAllTablesAndEvidence();
        return ResponseEntity.ok(result);
    }
}

package com.emailforensics.backend.health;

import com.emailforensics.backend.repository.EmailRepository;
import com.emailforensics.backend.repository.IndicatorRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HealthService {

    private final EmailRepository emailRepository;
    private final IndicatorRepository indicatorRepository;

    public HealthService(EmailRepository emailRepository, IndicatorRepository indicatorRepository) {
        this.emailRepository = emailRepository;
        this.indicatorRepository = indicatorRepository;
    }

    public HealthResponse getHealth() {
        try {
            long emailCount = emailRepository.count();
            long indicatorCount = indicatorRepository.count();
            List<String> tables = List.of("emails", "indicators");
            return new HealthResponse("UP", "CONNECTED", "email_forensics_db", tables, emailCount, indicatorCount);
        } catch (Exception ex) {
            return new HealthResponse("DOWN", "DISCONNECTED: " + ex.getMessage(), "email_forensics_db", List.of("emails", "indicators"), 0, 0);
        }
    }
}

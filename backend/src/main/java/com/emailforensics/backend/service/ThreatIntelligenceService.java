package com.emailforensics.backend.service;

import com.emailforensics.backend.dto.IndicatorDto;
import com.emailforensics.backend.entity.Email;
import com.emailforensics.backend.entity.Indicator;
import com.emailforensics.backend.provider.AbuseIpdbProvider;
import com.emailforensics.backend.provider.IpInfoProvider;
import com.emailforensics.backend.provider.PhishTankProvider;
import com.emailforensics.backend.provider.ProviderResult;
import com.emailforensics.backend.provider.SafeBrowsingProvider;
import com.emailforensics.backend.provider.ThreatFoxProvider;
import com.emailforensics.backend.provider.UrlhausProvider;
import com.emailforensics.backend.repository.EmailRepository;
import com.emailforensics.backend.repository.IndicatorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ThreatIntelligenceService {

    private final SafeBrowsingProvider safeBrowsingProvider;
    private final UrlhausProvider urlhausProvider;
    private final PhishTankProvider phishTankProvider;
    private final ThreatFoxProvider threatFoxProvider;
    private final AbuseIpdbProvider abuseIpdbProvider;
    private final IpInfoProvider ipInfoProvider;
    private final IndicatorRepository indicatorRepository;
    private final EmailRepository emailRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ThreatIntelligenceService(SafeBrowsingProvider safeBrowsingProvider,
                                     UrlhausProvider urlhausProvider,
                                     PhishTankProvider phishTankProvider,
                                     ThreatFoxProvider threatFoxProvider,
                                     AbuseIpdbProvider abuseIpdbProvider,
                                     IpInfoProvider ipInfoProvider,
                                     IndicatorRepository indicatorRepository,
                                     EmailRepository emailRepository) {
        this.safeBrowsingProvider = safeBrowsingProvider;
        this.urlhausProvider = urlhausProvider;
        this.phishTankProvider = phishTankProvider;
        this.threatFoxProvider = threatFoxProvider;
        this.abuseIpdbProvider = abuseIpdbProvider;
        this.ipInfoProvider = ipInfoProvider;
        this.indicatorRepository = indicatorRepository;
        this.emailRepository = emailRepository;
    }

    @Transactional
    public List<IndicatorDto> checkEmailIndicators(Long emailId) {
        List<Indicator> indicators = indicatorRepository.findByEmailId(emailId);
        if (indicators.isEmpty()) {
            return List.of();
        }

        for (Indicator ind : indicators) {
            checkSingleIndicator(ind);
        }

        indicatorRepository.saveAll(indicators);
        updateEmailStage(emailId);

        List<IndicatorDto> dtos = new ArrayList<>();
        for (Indicator ind : indicators) {
            dtos.add(IndicatorDto.fromEntity(ind));
        }
        return dtos;
    }

    private void checkSingleIndicator(Indicator indicator) {
        String type = indicator.getType();
        String val = indicator.getNormalizedValue() != null ? indicator.getNormalizedValue() : indicator.getValue();
        List<ProviderResult> results = new ArrayList<>();

        if ("URL".equalsIgnoreCase(type) || "QR_URL".equalsIgnoreCase(type)) {
            results.add(urlhausProvider.checkUrl(val));
            results.add(phishTankProvider.checkUrl(val));
            results.add(threatFoxProvider.checkIoc(val));
            results.add(safeBrowsingProvider.checkUrl(val));
        } else if ("DOMAIN".equalsIgnoreCase(type)) {
            results.add(urlhausProvider.checkHost(val));
            results.add(threatFoxProvider.checkIoc(val));
            results.add(safeBrowsingProvider.checkUrl("http://" + val));
        } else if ("IP".equalsIgnoreCase(type)) {
            results.add(abuseIpdbProvider.checkIp(val));
            results.add(threatFoxProvider.checkIoc(val));
            results.add(ipInfoProvider.checkIp(val));
        }

        applyResults(indicator, results);
    }

    private void applyResults(Indicator indicator, List<ProviderResult> results) {
        Map<String, Object> detailsMap = new LinkedHashMap<>();
        for (ProviderResult res : results) {
            detailsMap.put(res.getProviderName().toLowerCase(), res);
        }

        try {
            indicator.setDetails(objectMapper.writeValueAsString(detailsMap));
        } catch (Exception e) {
            indicator.setDetails("{}");
        }

        indicator.setVerdict(determineIndicatorVerdict(results));
        indicator.setCheckedAt(Instant.now());
    }

    public String determineIndicatorVerdict(List<ProviderResult> results) {
        boolean hasSuspicious = false;
        boolean hasNoMatch = false;

        for (ProviderResult res : results) {
            if ("MALICIOUS".equalsIgnoreCase(res.getVerdict())) {
                return "MALICIOUS";
            }
            if ("SUSPICIOUS".equalsIgnoreCase(res.getVerdict())) {
                hasSuspicious = true;
            }
            if ("NO_MATCH".equalsIgnoreCase(res.getStatus())) {
                hasNoMatch = true;
            }
        }

        if (hasSuspicious) {
            return "SUSPICIOUS";
        }
        if (hasNoMatch) {
            return "NO_MATCH";
        }
        return "UNKNOWN";
    }

    private void updateEmailStage(Long emailId) {
        Email email = emailRepository.findById(emailId).orElse(null);
        if (email != null) {
            email.setStage("THREAT_INTEL_CHECKED");
            emailRepository.save(email);
        }
    }
}

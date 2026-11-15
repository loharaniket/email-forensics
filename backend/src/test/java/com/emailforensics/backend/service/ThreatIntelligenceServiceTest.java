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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThreatIntelligenceServiceTest {

    @Mock
    private SafeBrowsingProvider safeBrowsingProvider;

    @Mock
    private UrlhausProvider urlhausProvider;

    @Mock
    private PhishTankProvider phishTankProvider;

    @Mock
    private ThreatFoxProvider threatFoxProvider;

    @Mock
    private AbuseIpdbProvider abuseIpdbProvider;

    @Mock
    private IpInfoProvider ipInfoProvider;

    @Mock
    private IndicatorRepository indicatorRepository;

    @Mock
    private EmailRepository emailRepository;

    private ThreatIntelligenceService service;

    @BeforeEach
    void setUp() {
        service = new ThreatIntelligenceService(
                safeBrowsingProvider,
                urlhausProvider,
                phishTankProvider,
                threatFoxProvider,
                abuseIpdbProvider,
                ipInfoProvider,
                indicatorRepository,
                emailRepository
        );
    }

    @Test
    void shouldDetermineMaliciousWhenAnyProviderMatchesMalicious() {
        ProviderResult r1 = ProviderResult.noMatch("URLhaus", "Clean");
        ProviderResult r2 = ProviderResult.match("PhishTank", "MALICIOUS", "Phishing link verified", Map.of());
        String verdict = service.determineIndicatorVerdict(List.of(r1, r2));
        assertEquals("MALICIOUS", verdict);
    }

    @Test
    void shouldDetermineSuspiciousWhenAnyProviderMatchesSuspicious() {
        ProviderResult r1 = ProviderResult.noMatch("URLhaus", "Clean");
        ProviderResult r2 = ProviderResult.match("AbuseIPDB", "SUSPICIOUS", "Reports found", Map.of());
        String verdict = service.determineIndicatorVerdict(List.of(r1, r2));
        assertEquals("SUSPICIOUS", verdict);
    }

    @Test
    void shouldDetermineNoMatchWhenAllProvidersReturnNoMatch() {
        ProviderResult r1 = ProviderResult.noMatch("URLhaus", "Clean");
        ProviderResult r2 = ProviderResult.noMatch("SafeBrowsing", "Clean");
        String verdict = service.determineIndicatorVerdict(List.of(r1, r2));
        assertEquals("NO_MATCH", verdict);
    }

    @Test
    void shouldDetermineUnknownWhenAllProvidersReturnUnknown() {
        ProviderResult r1 = ProviderResult.unknown("URLhaus", "Offline");
        ProviderResult r2 = ProviderResult.unknown("SafeBrowsing", "Missing key");
        String verdict = service.determineIndicatorVerdict(List.of(r1, r2));
        assertEquals("UNKNOWN", verdict);
    }

    @Test
    void shouldCheckEmailIndicatorsAndRouteByType() {
        Indicator urlInd = new Indicator(10L, "URL", "http://evil.com/login", "http://evil.com/login", "HTML");
        Indicator ipInd = new Indicator(10L, "IP", "198.51.100.1", "198.51.100.1", "HEADER");

        when(indicatorRepository.findByEmailId(10L)).thenReturn(List.of(urlInd, ipInd));
        when(emailRepository.findById(10L)).thenReturn(Optional.of(new Email()));

        when(urlhausProvider.checkUrl("http://evil.com/login")).thenReturn(ProviderResult.match("URLhaus", "MALICIOUS", "Malware", Map.of()));
        when(phishTankProvider.checkUrl("http://evil.com/login")).thenReturn(ProviderResult.noMatch("PhishTank", "Clean"));
        when(threatFoxProvider.checkIoc("http://evil.com/login")).thenReturn(ProviderResult.noMatch("ThreatFox", "Clean"));
        when(safeBrowsingProvider.checkUrl("http://evil.com/login")).thenReturn(ProviderResult.unknown("SafeBrowsing", "No key"));

        when(abuseIpdbProvider.checkIp("198.51.100.1")).thenReturn(ProviderResult.noMatch("AbuseIPDB", "Clean"));
        when(threatFoxProvider.checkIoc("198.51.100.1")).thenReturn(ProviderResult.noMatch("ThreatFox", "Clean"));
        when(ipInfoProvider.checkIp("198.51.100.1")).thenReturn(ProviderResult.match("IPinfo", "UNKNOWN", "Context", Map.of("country", "US")));

        List<IndicatorDto> results = service.checkEmailIndicators(10L);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("MALICIOUS", results.get(0).getVerdict());
        assertEquals("NO_MATCH", results.get(1).getVerdict());

        verify(indicatorRepository).saveAll(any());
        verify(emailRepository).save(any(Email.class));
    }
}

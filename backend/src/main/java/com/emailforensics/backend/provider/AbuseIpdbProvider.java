package com.emailforensics.backend.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AbuseIpdbProvider {

    private final RestClient restClient;
    private final String apiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AbuseIpdbProvider(@Value("${threatintel.abuseipdb.api-key:}") String apiKey,
                             @Value("${threatintel.timeout-seconds:5}") int timeoutSeconds) {
        this.apiKey = apiKey;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl("https://api.abuseipdb.com/api/v2")
                .build();
    }

    public ProviderResult checkIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return ProviderResult.unknown("AbuseIPDB", "Empty IP address");
        }

        if (apiKey == null || apiKey.isBlank()) {
            return ProviderResult.unknown("AbuseIPDB", "AbuseIPDB API key not configured");
        }

        try {
            String json = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/check")
                            .queryParam("ipAddress", ip.trim())
                            .queryParam("maxAgeInDays", "90")
                            .build())
                    .header("Key", apiKey)
                    .header("Accept", "application/json")
                    .retrieve()
                    .body(String.class);

            return parseResponse(json);
        } catch (Exception e) {
            return ProviderResult.error("AbuseIPDB", e.getMessage());
        }
    }

    private ProviderResult parseResponse(String json) {
        if (json == null || json.isBlank()) {
            return ProviderResult.unknown("AbuseIPDB", "Empty response from AbuseIPDB");
        }

        try {
            JsonNode data = objectMapper.readTree(json).path("data");
            if (data.isMissingNode()) {
                return ProviderResult.unknown("AbuseIPDB", "Missing data node in AbuseIPDB response");
            }

            int score = data.path("abuseConfidenceScore").asInt(0);
            int totalReports = data.path("totalReports").asInt(0);
            String country = data.path("countryCode").asText();
            String isp = data.path("isp").asText();
            String domain = data.path("domain").asText();

            Map<String, Object> details = new LinkedHashMap<>();
            details.put("abuseConfidenceScore", score);
            details.put("totalReports", totalReports);
            details.put("countryCode", country);
            details.put("isp", isp);
            details.put("domain", domain);

            if (score >= 80) {
                return ProviderResult.match("AbuseIPDB", "MALICIOUS", "High abuse confidence score: " + score + "% (" + totalReports + " reports)", details);
            }
            if (score > 0) {
                return ProviderResult.match("AbuseIPDB", "SUSPICIOUS", "Abuse reports found: " + totalReports + " reports (score: " + score + "%)", details);
            }

            return ProviderResult.noMatch("AbuseIPDB", "Clean IP record with 0% abuse confidence");
        } catch (Exception e) {
            return ProviderResult.error("AbuseIPDB", "Failed to parse AbuseIPDB response: " + e.getMessage());
        }
    }
}

package com.emailforensics.backend.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SafeBrowsingProvider {

    private final RestClient restClient;
    private final String apiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SafeBrowsingProvider(@Value("${threatintel.safebrowsing.api-key:}") String apiKey,
                                @Value("${threatintel.timeout-seconds:5}") int timeoutSeconds) {
        this.apiKey = apiKey;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl("https://safebrowsing.googleapis.com/v4")
                .build();
    }

    public ProviderResult checkUrl(String url) {
        if (url == null || url.isBlank()) {
            return ProviderResult.unknown("SafeBrowsing", "Empty URL");
        }

        if (apiKey == null || apiKey.isBlank()) {
            return ProviderResult.unknown("SafeBrowsing", "Google Safe Browsing API key not configured");
        }

        try {
            Map<String, Object> payload = buildPayload(url);
            String json = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/threatMatches:find")
                            .queryParam("key", apiKey)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            return parseResponse(json);
        } catch (Exception e) {
            return ProviderResult.error("SafeBrowsing", e.getMessage());
        }
    }

    private Map<String, Object> buildPayload(String url) {
        Map<String, Object> client = new LinkedHashMap<>();
        client.put("clientId", "email-forensics-v2");
        client.put("clientVersion", "2.0.0");

        Map<String, Object> threatInfo = new LinkedHashMap<>();
        threatInfo.put("threatTypes", List.of("MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE", "POTENTIALLY_HARMFUL_APPLICATION"));
        threatInfo.put("platformTypes", List.of("ANY_PLATFORM"));
        threatInfo.put("threatEntryTypes", List.of("URL"));
        threatInfo.put("threatEntries", List.of(Map.of("url", url)));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("client", client);
        payload.put("threatInfo", threatInfo);
        return payload;
    }

    private ProviderResult parseResponse(String json) {
        if (json == null || json.isBlank() || "{}".equals(json.trim())) {
            return ProviderResult.noMatch("SafeBrowsing", "No threats detected by Google Safe Browsing");
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode matches = root.path("matches");

            if (matches.isArray() && !matches.isEmpty()) {
                List<String> threats = new ArrayList<>();
                for (JsonNode m : matches) {
                    threats.add(m.path("threatType").asText());
                }
                Map<String, Object> details = new LinkedHashMap<>();
                details.put("threatTypes", threats);
                details.put("matchCount", matches.size());
                return ProviderResult.match("SafeBrowsing", "MALICIOUS", "Flagged by Google Safe Browsing: " + String.join(", ", threats), details);
            }

            return ProviderResult.noMatch("SafeBrowsing", "No threats detected by Google Safe Browsing");
        } catch (Exception e) {
            return ProviderResult.error("SafeBrowsing", "Failed to parse Safe Browsing response: " + e.getMessage());
        }
    }
}

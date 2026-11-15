package com.emailforensics.backend.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class PhishTankProvider {

    private final RestClient restClient;
    private final String appKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PhishTankProvider(@Value("${threatintel.phishtank.api-key:}") String appKey,
                             @Value("${threatintel.timeout-seconds:5}") int timeoutSeconds) {
        this.appKey = appKey;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl("https://checkurl.phishtank.com")
                .defaultHeader("User-Agent", "phishtank/email-forensics-v2")
                .build();
    }

    public ProviderResult checkUrl(String url) {
        if (url == null || url.isBlank()) {
            return ProviderResult.unknown("PhishTank", "Empty URL");
        }

        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("url", url);
            formData.add("format", "json");

            if (appKey != null && !appKey.isBlank()) {
                formData.add("app_key", appKey);
            }

            String responseBody = restClient.post()
                    .uri("/checkurl/")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(String.class);

            return parseResponse(responseBody);
        } catch (Exception e) {
            return ProviderResult.error("PhishTank", e.getMessage());
        }
    }

    private ProviderResult parseResponse(String json) {
        if (json == null || json.isBlank()) {
            return ProviderResult.unknown("PhishTank", "Empty response from PhishTank");
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode results = root.path("results");
            if (results.isMissingNode()) {
                return ProviderResult.unknown("PhishTank", "No results node in PhishTank response");
            }

            boolean inDatabase = results.path("in_database").asBoolean(false);
            if (!inDatabase) {
                return ProviderResult.noMatch("PhishTank", "URL not found in PhishTank database");
            }

            boolean verified = results.path("verified").asBoolean(false);
            boolean valid = results.path("valid").asBoolean(false);
            String phishId = results.path("phish_id").asText();
            String phishDetailUrl = results.path("phish_detail_page").asText();

            Map<String, Object> details = new LinkedHashMap<>();
            details.put("inDatabase", true);
            details.put("verified", verified);
            details.put("valid", valid);
            details.put("phishId", phishId);
            details.put("detailUrl", phishDetailUrl);

            if (verified && valid) {
                return ProviderResult.match("PhishTank", "MALICIOUS", "Verified phishing URL match in PhishTank (ID: " + phishId + ")", details);
            }

            if (verified && !valid) {
                return ProviderResult.noMatch("PhishTank", "Submission verified as not phishing in PhishTank (ID: " + phishId + ")");
            }

            return ProviderResult.match("PhishTank", "SUSPICIOUS", "Unconfirmed phishing submission in PhishTank (ID: " + phishId + ")", details);
        } catch (Exception e) {
            return ProviderResult.error("PhishTank", "Failed to parse PhishTank response: " + e.getMessage());
        }
    }
}

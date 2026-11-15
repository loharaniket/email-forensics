package com.emailforensics.backend.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class ThreatFoxProvider {

    private final RestClient restClient;
    private final String authKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ThreatFoxProvider(@Value("${threatintel.threatfox.api-key:}") String authKey,
                             @Value("${threatintel.timeout-seconds:5}") int timeoutSeconds) {
        this.authKey = authKey;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl("https://threatfox-api.abuse.ch/api/v1/")
                .build();
    }

    public ProviderResult checkIoc(String term) {
        if (term == null || term.isBlank()) {
            return ProviderResult.unknown("ThreatFox", "Empty search term");
        }

        try {
            Map<String, String> payload = new LinkedHashMap<>();
            payload.put("query", "search_ioc");
            payload.put("search_term", term.trim());

            RestClient.RequestHeadersSpec<?> req = restClient.post()
                    .uri("")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload);

            if (authKey != null && !authKey.isBlank()) {
                req.header("Auth-Key", authKey);
            }

            String responseBody = req.retrieve().body(String.class);
            return parseResponse(responseBody, term.trim());
        } catch (Exception e) {
            return ProviderResult.error("ThreatFox", e.getMessage());
        }
    }

    private ProviderResult parseResponse(String json, String term) {
        if (json == null || json.isBlank()) {
            return ProviderResult.unknown("ThreatFox", "Empty response from ThreatFox");
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            String status = root.path("query_status").asText();

            if ("ok".equalsIgnoreCase(status)) {
                JsonNode data = root.path("data");
                if (data.isArray() && !data.isEmpty()) {
                    JsonNode matchingNode = findMatchingIoc(data, term);
                    if (matchingNode != null) {
                        Map<String, Object> details = new LinkedHashMap<>();
                        details.put("queryStatus", status);
                        details.put("ioc", matchingNode.path("ioc").asText());
                        details.put("iocType", matchingNode.path("ioc_type").asText());
                        details.put("threatType", matchingNode.path("threat_type").asText());
                        details.put("malware", matchingNode.path("malware_printable").asText());
                        details.put("confidenceLevel", matchingNode.path("confidence_level").asInt());
                        details.put("firstSeen", matchingNode.path("first_seen").asText());
                        details.put("reporter", matchingNode.path("reporter").asText());

                        int confidence = (int) details.getOrDefault("confidenceLevel", 100);
                        String verdict = confidence >= 50 ? "MALICIOUS" : "SUSPICIOUS";
                        return ProviderResult.match("ThreatFox", verdict, "Confirmed IOC record found in ThreatFox", details);
                    }
                }
                return ProviderResult.noMatch("ThreatFox", "No direct IOC match found in ThreatFox");
            }

            if ("no_result".equalsIgnoreCase(status)) {
                return ProviderResult.noMatch("ThreatFox", "No threat records found in ThreatFox");
            }

            return ProviderResult.unknown("ThreatFox", "ThreatFox status: " + status);
        } catch (Exception e) {
            return ProviderResult.error("ThreatFox", "Failed to parse ThreatFox response: " + e.getMessage());
        }
    }

    private JsonNode findMatchingIoc(JsonNode data, String term) {
        String lowerTerm = term.toLowerCase(Locale.ROOT);
        for (JsonNode node : data) {
            String ioc = node.path("ioc").asText("").trim().toLowerCase(Locale.ROOT);
            String iocType = node.path("ioc_type").asText("");

            if (ioc.equals(lowerTerm)) {
                return node;
            }

            if ("ip:port".equalsIgnoreCase(iocType)) {
                if (ioc.startsWith(lowerTerm + ":") || ioc.equals(lowerTerm)) {
                    return node;
                }
            } else if ("domain".equalsIgnoreCase(iocType)) {
                if (ioc.equals(lowerTerm) || ioc.endsWith("." + lowerTerm)) {
                    return node;
                }
            } else if ("url".equalsIgnoreCase(iocType)) {
                try {
                    URI uri = URI.create(ioc);
                    String host = uri.getHost();
                    if (host != null && (host.equalsIgnoreCase(lowerTerm) || host.endsWith("." + lowerTerm))) {
                        return node;
                    }
                } catch (Exception ignored) {
                }
                if (ioc.equals(lowerTerm)) {
                    return node;
                }
            }
        }
        return null;
    }
}

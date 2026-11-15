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
public class UrlhausProvider {

    private final RestClient restClient;
    private final String authKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UrlhausProvider(@Value("${threatintel.urlhaus.api-key:}") String authKey,
                           @Value("${threatintel.timeout-seconds:5}") int timeoutSeconds) {
        this.authKey = authKey;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl("https://urlhaus-api.abuse.ch/v1")
                .build();
    }

    public ProviderResult checkUrl(String url) {
        if (url == null || url.isBlank()) {
            return ProviderResult.unknown("URLhaus", "Empty URL");
        }

        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("url", url);

            RestClient.RequestHeadersSpec<?> req = restClient.post()
                    .uri("/url/")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData);

            if (authKey != null && !authKey.isBlank()) {
                req.header("Auth-Key", authKey);
            }

            String responseBody = req.retrieve().body(String.class);
            return parseResponse(responseBody, "URLhaus");
        } catch (Exception e) {
            return ProviderResult.error("URLhaus", e.getMessage());
        }
    }

    public ProviderResult checkHost(String host) {
        if (host == null || host.isBlank()) {
            return ProviderResult.unknown("URLhaus", "Empty host");
        }

        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("host", host);

            RestClient.RequestHeadersSpec<?> req = restClient.post()
                    .uri("/host/")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData);

            if (authKey != null && !authKey.isBlank()) {
                req.header("Auth-Key", authKey);
            }

            String responseBody = req.retrieve().body(String.class);
            return parseResponse(responseBody, "URLhaus");
        } catch (Exception e) {
            return ProviderResult.error("URLhaus", e.getMessage());
        }
    }

    private ProviderResult parseResponse(String json, String name) {
        if (json == null || json.isBlank()) {
            return ProviderResult.unknown(name, "Empty response from URLhaus");
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            String status = root.path("query_status").asText();

            if ("ok".equalsIgnoreCase(status)) {
                if (root.has("urls")) {
                    boolean blacklisted = "listed".equalsIgnoreCase(root.path("blacklists").path("spamhaus_dbl").asText())
                            || "listed".equalsIgnoreCase(root.path("blacklists").path("surbl").asText());
                    boolean hasActiveUrl = false;
                    JsonNode urlsNode = root.path("urls");
                    if (urlsNode.isArray()) {
                        for (JsonNode u : urlsNode) {
                            if ("online".equalsIgnoreCase(u.path("url_status").asText())) {
                                hasActiveUrl = true;
                                break;
                            }
                        }
                    }

                    Map<String, Object> details = new LinkedHashMap<>();
                    details.put("queryStatus", status);
                    details.put("urlCount", root.path("url_count").asText());
                    details.put("host", root.path("host").asText());

                    if (blacklisted || hasActiveUrl) {
                        return ProviderResult.match(name, "MALICIOUS", "Active malware distribution match for host in URLhaus", details);
                    }
                    return ProviderResult.noMatch(name, "No active malware distribution on host in URLhaus");
                }

                Map<String, Object> details = new LinkedHashMap<>();
                details.put("queryStatus", status);
                details.put("threat", root.path("threat").asText());
                details.put("urlStatus", root.path("url_status").asText());
                details.put("reporter", root.path("reporter").asText());
                details.put("dateAdded", root.path("date_added").asText());
                return ProviderResult.match(name, "MALICIOUS", "Confirmed malware distribution match in URLhaus", details);
            }

            if ("no_results".equalsIgnoreCase(status)) {
                return ProviderResult.noMatch(name, "No threat record found in URLhaus");
            }

            return ProviderResult.unknown(name, "URLhaus query status: " + status);
        } catch (Exception e) {
            return ProviderResult.error(name, "Failed to parse URLhaus response: " + e.getMessage());
        }
    }
}

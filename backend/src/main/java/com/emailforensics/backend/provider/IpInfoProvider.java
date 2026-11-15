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
public class IpInfoProvider {

    private final RestClient restClient;
    private final String apiToken;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IpInfoProvider(@Value("${threatintel.ipinfo.api-key:}") String apiToken,
                          @Value("${threatintel.timeout-seconds:5}") int timeoutSeconds) {
        this.apiToken = apiToken;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl("https://ipinfo.io")
                .build();
    }

    public ProviderResult checkIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return ProviderResult.unknown("IPinfo", "Empty IP address");
        }

        try {
            RestClient.RequestHeadersSpec<?> req = restClient.get()
                    .uri("/{ip}/json", ip.trim());

            if (apiToken != null && !apiToken.isBlank()) {
                req.header("Authorization", "Bearer " + apiToken);
            }

            String json = req.retrieve().body(String.class);
            return parseResponse(json);
        } catch (Exception e) {
            return ProviderResult.error("IPinfo", e.getMessage());
        }
    }

    private ProviderResult parseResponse(String json) {
        if (json == null || json.isBlank()) {
            return ProviderResult.unknown("IPinfo", "Empty response from IPinfo");
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            String country = root.path("country").asText();
            String city = root.path("city").asText();
            String region = root.path("region").asText();
            String org = root.path("org").asText();
            String timezone = root.path("timezone").asText();

            Map<String, Object> details = new LinkedHashMap<>();
            details.put("country", country);
            details.put("city", city);
            details.put("region", region);
            details.put("org", org);
            details.put("timezone", timezone);

            if (!country.isEmpty() || !org.isEmpty()) {
                return ProviderResult.match("IPinfo", "UNKNOWN", "Geolocation and network ASN context resolved", details);
            }

            return ProviderResult.noMatch("IPinfo", "No geolocation records found");
        } catch (Exception e) {
            return ProviderResult.error("IPinfo", "Failed to parse IPinfo response: " + e.getMessage());
        }
    }
}

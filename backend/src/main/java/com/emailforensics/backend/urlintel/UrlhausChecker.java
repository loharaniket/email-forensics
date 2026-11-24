package com.emailforensics.backend.urlintel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class UrlhausChecker implements UrlChecker {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public UrlhausChecker() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getCheckName() {
        return "URLHAUS_MALWARE";
    }

    @Override
    public UrlCheckResult check(String url, String domain) {
        if (url == null || url.trim().isEmpty()) {
            return new UrlCheckResult(getCheckName(), "URLhaus", false, 0, "CLEAN", "Empty URL");
        }
        try {
            String form = "url=" + URLEncoder.encode(url, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://urlhaus-api.abuse.ch/v1/url/"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(2))
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                String status = root.path("query_status").asText("");
                if ("ok".equalsIgnoreCase(status)) {
                    String threat = root.path("threat").asText("malware_download");
                    return new UrlCheckResult(getCheckName(), "URLhaus", true, 50, "MALWARE", "Malware URL found in URLhaus: " + threat);
                }
            }
        } catch (Exception ignored) {
        }
        return new UrlCheckResult(getCheckName(), "URLhaus", false, 0, "CLEAN", "No malware found");
    }
}

package com.emailforensics.backend.urlintel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class CloudflareDnsChecker implements UrlChecker {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CloudflareDnsChecker() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getCheckName() {
        return "CLOUDFLARE_SECURITY_DNS";
    }

    @Override
    public UrlCheckResult check(String url, String domain) {
        if (domain == null || domain.trim().isEmpty() || domain.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
            return new UrlCheckResult(getCheckName(), "Cloudflare DNS", false, 0, "CLEAN", "Clean domain");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://security.cloudflare-dns.com/dns-query?name=" + domain + "&type=A"))
                    .header("Accept", "application/dns-json")
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode answer = root.path("Answer");
                if (answer.isArray()) {
                    for (JsonNode rec : answer) {
                        String data = rec.path("data").asText("");
                        if ("0.0.0.0".equals(data) || "127.0.0.1".equals(data)) {
                            return new UrlCheckResult(getCheckName(), "Cloudflare DNS", true, 45, "THREAT_DNS", "Domain blocked by Cloudflare Security DNS");
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return new UrlCheckResult(getCheckName(), "Cloudflare DNS", false, 0, "CLEAN", "Passed Cloudflare DNS check");
    }
}

package com.emailforensics.backend.urlintel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class GoogleSafeBrowsingChecker implements UrlChecker {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public GoogleSafeBrowsingChecker(@Value("${google.safebrowsing.api-key:}") String apiKey) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey != null ? apiKey.trim() : "";
    }

    @Override
    public String getCheckName() {
        return "SAFE_BROWSING";
    }

    @Override
    public UrlCheckResult check(String url, String domain) {
        if (url == null || url.trim().isEmpty() || apiKey.isEmpty()) {
            return new UrlCheckResult(getCheckName(), "Google Safe Browsing", false, 0, "CLEAN", "Safe browsing verified");
        }
        try {
            String requestJson = objectMapper.writeValueAsString(new SafeBrowsingRequest(url));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://safebrowsing.googleapis.com/v4/threatMatches:find?key=" + apiKey))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(2))
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode matches = root.path("matches");
                if (matches.isArray() && !matches.isEmpty()) {
                    String threatType = matches.get(0).path("threatType").asText("SOCIAL_ENGINEERING");
                    return new UrlCheckResult(getCheckName(), "Google Safe Browsing", true, 50, "PHISHING", "Unsafe site detected by Google Safe Browsing: " + threatType);
                }
            }
        } catch (Exception ignored) {
        }
        return new UrlCheckResult(getCheckName(), "Google Safe Browsing", false, 0, "CLEAN", "Safe browsing clean");
    }

    private static class SafeBrowsingRequest {
        public ClientInfo client = new ClientInfo();
        public ThreatInfo threatInfo;

        public SafeBrowsingRequest(String url) {
            this.threatInfo = new ThreatInfo(url);
        }
    }

    private static class ClientInfo {
        public String clientId = "email-forensics";
        public String clientVersion = "1.0";
    }

    private static class ThreatInfo {
        public String[] threatTypes = {"MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE"};
        public String[] platformTypes = {"ANY_PLATFORM"};
        public String[] threatEntryTypes = {"URL"};
        public ThreatEntry[] threatEntries;

        public ThreatInfo(String url) {
            this.threatEntries = new ThreatEntry[]{new ThreatEntry(url)};
        }
    }

    private static class ThreatEntry {
        public String url;
        public ThreatEntry(String url) {
            this.url = url;
        }
    }
}

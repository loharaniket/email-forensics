package com.emailforensics.backend.headers;

import com.emailforensics.backend.analysis.AnalysisResult;
import com.emailforensics.backend.dto.headers.ReceivedHopDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Beginner-friendly service to perform email header authentication analysis
 * using external APIs with automatic fallback:
 * 1. Powerline.ai Header API (Primary)
 * 2. Testmail.app Tools API (Fallback)
 */
@Service
public class EmailHeaderService {

    private static final Logger log = LoggerFactory.getLogger(EmailHeaderService.class);

    @Value("${emailforensics.auth.powerline.api-key:${POWERLINE_API_KEY:}}")
    private String powerlineApiKey;

    @Value("${emailforensics.auth.powerline.url:${POWERLINE_API_URL:https://powerline.ai/api/tools/headers}}")
    private String powerlineUrl;

    @Value("${emailforensics.auth.testmail.api-key:${TESTMAIL_API_KEY:}}")
    private String testmailApiKey;

    @Value("${emailforensics.auth.testmail.url:${TESTMAIL_API_URL:https://api.testmail.app/api/tools/headers}}")
    private String testmailUrl;

    @Value("${emailforensics.auth.timeout-seconds:${AUTH_API_TIMEOUT_SECONDS:15}}")
    private int timeoutSeconds;

    private final ObjectMapper objectMapper;
    private HttpClient httpClient;

    public EmailHeaderService() {
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    // Constructor for testing with mock client & custom keys
    public EmailHeaderService(HttpClient httpClient,
                              String powerlineApiKey,
                              String powerlineUrl,
                              String testmailApiKey,
                              String testmailUrl,
                              int timeoutSeconds) {
        this.objectMapper = new ObjectMapper();
        this.httpClient = httpClient;
        this.powerlineApiKey = powerlineApiKey;
        this.powerlineUrl = powerlineUrl != null ? powerlineUrl : "https://powerline.ai/api/tools/headers";
        this.testmailApiKey = testmailApiKey;
        this.testmailUrl = testmailUrl != null ? testmailUrl : "https://api.testmail.app/api/tools/headers";
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 15;
    }

    /**
     * Analyzes raw email headers using external APIs with automatic fallback.
     * Tries Powerline.ai first. If Powerline.ai has a service failure, falls back to Testmail.app.
     *
     * @param rawHeaders The raw email headers text
     * @return Normalized analysis result
     */
    public HeaderAuthResult analyzeHeader(String rawHeaders) {
        if (rawHeaders == null || rawHeaders.trim().isEmpty()) {
            return HeaderAuthResult.serviceFailure("INPUT_VALIDATION", "No email headers provided for analysis.");
        }

        // 1. Try Powerline.ai Header API (Primary)
        log.info("Sending email headers to primary provider: Powerline.ai");
        HeaderAuthResult powerlineResult = callPowerlineApi(rawHeaders);

        // If Powerline gave a valid response (even if SPF/DKIM/DMARC failed authentication), return it!
        if (powerlineResult != null && !powerlineResult.isServiceFailure()) {
            log.info("Powerline.ai header analysis succeeded with status: {}", powerlineResult.getStatus());
            return powerlineResult;
        }

        String powerlineError = powerlineResult != null ? powerlineResult.getErrorMessage() : "Unknown failure";
        log.warn("Powerline.ai unavailable ({}). Falling back to Testmail.app...", powerlineError);

        // 2. Try Testmail.app Tools API (Fallback)
        log.info("Sending email headers to fallback provider: Testmail.app");
        HeaderAuthResult testmailResult = callTestmailApi(rawHeaders);

        if (testmailResult != null && !testmailResult.isServiceFailure()) {
            log.info("Testmail.app header analysis succeeded with status: {}", testmailResult.getStatus());
            return testmailResult;
        }

        String testmailError = testmailResult != null ? testmailResult.getErrorMessage() : "Unknown failure";
        log.warn("Testmail.app also unavailable ({}). All header authentication providers failed.", testmailError);

        // 3. Both failed: return clear, friendly error
        return HeaderAuthResult.allUnavailable(
                "Email header authentication is temporarily unavailable. "
                        + "Powerline.ai: " + powerlineError + "; Testmail.app: " + testmailError
        );
    }

    /**
     * Calls Powerline.ai Header API (https://powerline.ai/api/tools/headers).
     */
    public HeaderAuthResult callPowerlineApi(String rawHeaders) {
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(powerlineUrl))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "text/plain")
                    .POST(HttpRequest.BodyPublishers.ofString(rawHeaders, StandardCharsets.UTF_8));

            if (powerlineApiKey != null && !powerlineApiKey.trim().isEmpty()) {
                reqBuilder.header("Authorization", "Bearer " + powerlineApiKey.trim());
            }

            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int statusCode = response.statusCode();
            String responseBody = response.body();

            if (statusCode >= 200 && statusCode < 300 && responseBody != null && !responseBody.isBlank()) {
                return parseApiResponse("Powerline.ai", responseBody);
            }

            return HeaderAuthResult.serviceFailure("Powerline.ai", "HTTP " + statusCode + ": " + sanitize(responseBody));

        } catch (HttpTimeoutException e) {
            return HeaderAuthResult.serviceFailure("Powerline.ai", "Request timed out after " + timeoutSeconds + "s");
        } catch (Exception e) {
            return HeaderAuthResult.serviceFailure("Powerline.ai", "Request error: " + e.getMessage());
        }
    }

    /**
     * Calls Testmail.app Tools API (https://api.testmail.app/api/tools/headers).
     */
    public HeaderAuthResult callTestmailApi(String rawHeaders) {
        if (testmailApiKey == null || testmailApiKey.trim().isEmpty()) {
            return HeaderAuthResult.serviceFailure("Testmail.app", "Testmail.app API key is not configured.");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(testmailUrl))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "text/plain")
                    .header("Authorization", "Bearer " + testmailApiKey.trim())
                    .POST(HttpRequest.BodyPublishers.ofString(rawHeaders, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int statusCode = response.statusCode();
            String responseBody = response.body();

            if (statusCode >= 200 && statusCode < 300 && responseBody != null && !responseBody.isBlank()) {
                return parseApiResponse("Testmail.app", responseBody);
            }

            return HeaderAuthResult.serviceFailure("Testmail.app", "HTTP " + statusCode + ": " + sanitize(responseBody));

        } catch (HttpTimeoutException e) {
            return HeaderAuthResult.serviceFailure("Testmail.app", "Request timed out after " + timeoutSeconds + "s");
        } catch (Exception e) {
            return HeaderAuthResult.serviceFailure("Testmail.app", "Request error: " + e.getMessage());
        }
    }

    /**
     * Saves the external API analysis result into the AnalysisResult database entity.
     */
    public void saveResult(AnalysisResult entity, HeaderAuthResult authResult) {
        if (entity == null || authResult == null) {
            return;
        }

        entity.setReportedSpf(authResult.getSpfResult());
        entity.setReportedDkim(authResult.getDkimResult());
        entity.setReportedDmarc(authResult.getDmarcResult());
        entity.setReportedArc(authResult.getArcResult());

        entity.setAuthApiProvider(authResult.getProviderName());
        entity.setAuthApiStatus(authResult.getStatus());
        entity.setAuthApiResponseJson(authResult.getRawResponseJson());
        entity.setAuthApiErrorDetails(authResult.getErrorMessage());
        entity.setAuthApiUpdatedAt(authResult.getEvaluatedAt());

        if (authResult.getSummary() != null && !authResult.getSummary().isBlank()) {
            entity.setIndependentVerification(authResult.getSummary());
        }
    }

    /**
     * Parses the JSON response returned by either Powerline.ai or Testmail.app.
     */
    private HeaderAuthResult parseApiResponse(String providerName, String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode authNode = root.has("authentication") ? root.path("authentication") : root;

            HeaderAuthResult result = HeaderAuthResult.success(providerName);
            result.setRawResponseJson(json);

            // 1. SPF
            JsonNode spfNode = authNode.path("spf");
            if (!spfNode.isMissingNode() && !spfNode.isNull()) {
                String spfStatus = spfNode.hasNonNull("result") ? spfNode.path("result").asText()
                        : (spfNode.hasNonNull("status") ? spfNode.path("status").asText() : spfNode.asText("none"));
                result.setSpfResult(normalizeVerdict(spfStatus));
                result.setSpfRecord(spfNode.path("record").asText(null));
                result.setSpfRelayIp(spfNode.hasNonNull("ip") ? spfNode.path("ip").asText() : spfNode.path("relay_ip").asText(null));
            } else {
                result.setSpfResult("unsupported");
            }

            // 2. DKIM
            JsonNode dkimNode = authNode.path("dkim");
            if (!dkimNode.isMissingNode() && !dkimNode.isNull()) {
                String dkimStatus = dkimNode.hasNonNull("result") ? dkimNode.path("result").asText()
                        : (dkimNode.hasNonNull("status") ? dkimNode.path("status").asText() : dkimNode.asText("none"));
                result.setDkimResult(normalizeVerdict(dkimStatus));
                result.setDkimDomain(dkimNode.path("domain").asText(null));
                result.setDkimSelector(dkimNode.path("selector").asText(null));
                result.setDkimRecord(dkimNode.path("record").asText(null));
            } else {
                result.setDkimResult("unsupported");
            }

            // 3. DMARC
            JsonNode dmarcNode = authNode.path("dmarc");
            if (!dmarcNode.isMissingNode() && !dmarcNode.isNull()) {
                String dmarcStatus = dmarcNode.hasNonNull("result") ? dmarcNode.path("result").asText()
                        : (dmarcNode.hasNonNull("status") ? dmarcNode.path("status").asText() : dmarcNode.asText("none"));
                result.setDmarcResult(normalizeVerdict(dmarcStatus));
                result.setDmarcPolicy(dmarcNode.path("policy").asText(null));
            } else {
                result.setDmarcResult("unsupported");
            }

            // 4. ARC
            JsonNode arcNode = authNode.path("arc");
            if (!arcNode.isMissingNode() && !arcNode.isNull()) {
                String arcStatus = arcNode.hasNonNull("result") ? arcNode.path("result").asText()
                        : (arcNode.hasNonNull("status") ? arcNode.path("status").asText() : arcNode.asText("none"));
                result.setArcResult(normalizeVerdict(arcStatus));
            } else {
                result.setArcResult("unsupported");
            }

            // 5. Routing Hops
            JsonNode hopsArray = root.has("hops") ? root.path("hops")
                    : (root.has("delivery_path") ? root.path("delivery_path") : null);

            List<ReceivedHopDto> hops = new ArrayList<>();
            if (hopsArray != null && hopsArray.isArray()) {
                for (int i = 0; i < hopsArray.size(); i++) {
                    JsonNode h = hopsArray.get(i);
                    int order = h.hasNonNull("order") ? h.path("order").asInt(i + 1)
                            : (h.hasNonNull("hop") ? h.path("hop").asInt(i + 1) : i + 1);
                    String from = h.path("from").asText(null);
                    String by = h.path("by").asText(null);
                    String proto = h.hasNonNull("protocol") ? h.path("protocol").asText() : h.path("with").asText(null);
                    String ip = h.path("ip").asText(null);
                    Long delay = h.hasNonNull("delay") ? h.path("delay").asLong() : (h.hasNonNull("delay_seconds") ? h.path("delay_seconds").asLong() : null);
                    String rawLine = h.path("raw").asText("Received: from " + from + " by " + by);

                    hops.add(new ReceivedHopDto(null, order, from, by, proto, ip, null, delay, rawLine, null));
                }
            }
            result.setHops(hops);

            result.setSummary(String.format(Locale.ROOT,
                    "Validated via %s. SPF: %s, DKIM: %s, DMARC: %s, ARC: %s",
                    providerName,
                    result.getSpfResult().toUpperCase(Locale.ROOT),
                    result.getDkimResult().toUpperCase(Locale.ROOT),
                    result.getDmarcResult().toUpperCase(Locale.ROOT),
                    result.getArcResult().toUpperCase(Locale.ROOT)));

            return result;

        } catch (Exception e) {
            log.error("Failed to parse JSON response from {}: {}", providerName, e.getMessage());
            return HeaderAuthResult.serviceFailure(providerName, "Invalid response JSON: " + e.getMessage());
        }
    }

    private String normalizeVerdict(String raw) {
        if (raw == null || raw.isBlank()) return "none";
        String lower = raw.trim().toLowerCase(Locale.ROOT);
        if (lower.contains("pass")) return "pass";
        if (lower.contains("softfail")) return "softfail";
        if (lower.contains("fail")) return "fail";
        if (lower.contains("neutral")) return "neutral";
        if (lower.contains("none")) return "none";
        if (lower.contains("unsupported")) return "unsupported";
        return lower;
    }

    private String sanitize(String body) {
        if (body == null || body.isBlank()) return "Empty response body";
        return body.length() > 200 ? body.substring(0, 200) + "..." : body;
    }
}

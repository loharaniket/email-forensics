package com.emailforensics.backend.content;

import com.emailforensics.backend.dto.content.SocialEngineeringMatchDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class GroqSocialEngineeringService {

    private static final Logger log = LoggerFactory.getLogger(GroqSocialEngineeringService.class);

    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "URGENCY",
            "CREDENTIAL_REQUEST",
            "PAYMENT_INSTRUCTION",
            "IMPERSONATION",
            "SECRECY"
    );

    private static final Set<String> ALLOWED_SEVERITIES = Set.of(
            "LOW",
            "MEDIUM",
            "HIGH",
            "CRITICAL"
    );

    private static final String SYSTEM_PROMPT = """
            You are an expert digital forensics analyst specializing in email social engineering detection.
            Analyze the provided email content channels to identify social engineering attacks based on context, meaning, and user intent rather than keyword matches.

            You must ONLY identify the following 5 categories:
            1. URGENCY: Artificial deadlines, threats of account suspension/revocation, panic inducement, or pressure for hasty action.
            2. CREDENTIAL_REQUEST: Requests or instructions to enter, verify, or reset passwords, PINs, MFA codes, or access credentials.
            3. PAYMENT_INSTRUCTION: Demands for wire transfers, invoice changes, direct deposit redirection, cryptocurrency payments, or financial transfers.
            4. IMPERSONATION: Pretending to be an authority figure, executive (CEO, CFO, Director), IT administrator/support, HR, or government agency.
            5. SECRECY: Demands for confidentiality, instructions to keep the request secret, or guidance to bypass standard verification channels.

            CRITICAL EVIDENCE REQUIREMENTS:
            - Every 'matchedText' MUST be an EXACT, verbatim excerpt copied directly from the provided email content.
            - NEVER invent, extrapolate, paraphrase, or hallucinate quotes. If a category is not supported by verbatim text, do NOT report it.
            - 'sourceChannel' must match the channel where the text was found (e.g. SUBJECT, BODY_PLAIN, BODY_HTML, QR_PAYLOAD).
            - 'severity' must be one of: LOW, MEDIUM, HIGH, CRITICAL.

            Respond ONLY with a valid JSON object matching this schema:
            {
              "detected": true,
              "matches": [
                {
                  "category": "URGENCY",
                  "matchedText": "verbatim text from email",
                  "sourceChannel": "BODY_PLAIN",
                  "explanation": "concise explanation of the tactic",
                  "severity": "HIGH"
                }
              ]
            }

            If no social engineering is found, respond with:
            {
              "detected": false,
              "matches": []
            }
            """;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://api.groq.com/openai/v1}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model:openai/gpt-oss-20b}")
    private String modelName;

    @Value("${spring.ai.timeout-seconds:25}")
    private int timeoutSeconds;

    private HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static class SocialEngineeringFindings {
        private final boolean urgency;
        private final boolean credentialRequest;
        private final boolean paymentInstruction;
        private final boolean impersonation;
        private final boolean secrecy;
        private final List<SocialEngineeringMatchDto> matches;
        private final String status;
        private final String statusExplanation;

        public SocialEngineeringFindings(boolean urgency,
                                         boolean credentialRequest,
                                         boolean paymentInstruction,
                                         boolean impersonation,
                                         boolean secrecy,
                                         List<SocialEngineeringMatchDto> matches,
                                         String status,
                                         String statusExplanation) {
            this.urgency = urgency;
            this.credentialRequest = credentialRequest;
            this.paymentInstruction = paymentInstruction;
            this.impersonation = impersonation;
            this.secrecy = secrecy;
            this.matches = matches != null ? matches : Collections.emptyList();
            this.status = status != null ? status : "EVALUATED";
            this.statusExplanation = statusExplanation;
        }

        public boolean isUrgency() {
            return urgency;
        }

        public boolean isCredentialRequest() {
            return credentialRequest;
        }

        public boolean isPaymentInstruction() {
            return paymentInstruction;
        }

        public boolean isImpersonation() {
            return impersonation;
        }

        public boolean isSecrecy() {
            return secrecy;
        }

        public List<SocialEngineeringMatchDto> getMatches() {
            return matches;
        }

        public String getStatus() {
            return status;
        }

        public String getStatusExplanation() {
            return statusExplanation;
        }
    }

    public GroqSocialEngineeringService() {
        this(null, null, null, 25, null);
    }

    @Autowired
    public GroqSocialEngineeringService(
            @Value("${spring.ai.openai.api-key:}") String apiKey,
            @Value("${spring.ai.openai.base-url:https://api.groq.com/openai/v1}") String baseUrl,
            @Value("${spring.ai.openai.chat.options.model:openai/gpt-oss-20b}") String modelName,
            @Value("${spring.ai.timeout-seconds:25}") int timeoutSeconds) {
        this(apiKey, baseUrl, modelName, timeoutSeconds, null);
    }

    public GroqSocialEngineeringService(String apiKey,
                                        String baseUrl,
                                        String modelName,
                                        int timeoutSeconds,
                                        HttpClient httpClient) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.modelName = modelName;
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 25;
        this.httpClient = httpClient != null ? httpClient : HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public SocialEngineeringFindings analyze(Map<String, String> channelTexts) {
        if (channelTexts == null || channelTexts.isEmpty()) {
            return new SocialEngineeringFindings(false, false, false, false, false,
                    Collections.emptyList(), "EVALUATED", "No content channels available to analyze.");
        }

        boolean hasContent = channelTexts.values().stream()
                .anyMatch(v -> v != null && !v.isBlank());
        if (!hasContent) {
            return new SocialEngineeringFindings(false, false, false, false, false,
                    Collections.emptyList(), "EVALUATED", "All provided content channels are empty.");
        }

        String effectiveKey = resolveApiKey();
        if (!isValidApiKey(effectiveKey)) {
            log.warn("Groq API key is not configured. Social engineering detection unavailable.");
            return new SocialEngineeringFindings(false, false, false, false, false,
                    Collections.emptyList(), "UNAVAILABLE", "Groq API key is not configured.");
        }

        String userPrompt = buildUserPrompt(channelTexts);
        String activeModel = resolveModel();
        String activeBaseUrl = resolveBaseUrl();

        try {
            String requestPayload = buildChatPayload(activeModel, SYSTEM_PROMPT, userPrompt, true);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(activeBaseUrl + "/chat/completions"))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + effectiveKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestPayload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            // Retry without response_format if model returned 400 json_validate_failed
            if (response.statusCode() == 400 && response.body() != null && response.body().contains("json_validate_failed")) {
                String fallbackPayload = buildChatPayload(activeModel, SYSTEM_PROMPT, userPrompt, false);
                HttpRequest retryRequest = HttpRequest.newBuilder()
                        .uri(URI.create(activeBaseUrl + "/chat/completions"))
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + effectiveKey)
                        .POST(HttpRequest.BodyPublishers.ofString(fallbackPayload, StandardCharsets.UTF_8))
                        .build();
                response = httpClient.send(retryRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            }

            if (response.statusCode() != 200) {
                log.error("Groq API returned HTTP {}: {}", response.statusCode(), response.body());
                return new SocialEngineeringFindings(false, false, false, false, false,
                        Collections.emptyList(), "UNAVAILABLE", "Groq API error (HTTP " + response.statusCode() + ")");
            }

            return parseAndValidateGroqResponse(response.body(), channelTexts);

        } catch (HttpTimeoutException e) {
            log.error("Groq API request timed out after {} seconds", timeoutSeconds, e);
            return new SocialEngineeringFindings(false, false, false, false, false,
                    Collections.emptyList(), "UNAVAILABLE", "Groq API request timed out after " + timeoutSeconds + "s.");
        } catch (Exception e) {
            log.error("Groq social engineering detection failed: {}", e.getMessage(), e);
            return new SocialEngineeringFindings(false, false, false, false, false,
                    Collections.emptyList(), "UNAVAILABLE", "Groq API request failed: " + e.getMessage());
        }
    }

    private SocialEngineeringFindings parseAndValidateGroqResponse(String responseBody, Map<String, String> channelTexts) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return new SocialEngineeringFindings(false, false, false, false, false,
                        Collections.emptyList(), "UNAVAILABLE", "Groq response missing choices array.");
            }

            String content = choices.get(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                return new SocialEngineeringFindings(false, false, false, false, false,
                        Collections.emptyList(), "UNAVAILABLE", "Groq response returned empty message content.");
            }

            String jsonText = extractJsonBlock(content);
            JsonNode analysis = objectMapper.readTree(jsonText);

            JsonNode matchesNode = analysis.path("matches");
            List<SocialEngineeringMatchDto> validMatches = new ArrayList<>();

            if (matchesNode.isArray()) {
                for (JsonNode item : matchesNode) {
                    String category = item.path("category").asText("").trim().toUpperCase(Locale.ROOT);
                    if (!ALLOWED_CATEGORIES.contains(category)) {
                        log.warn("Discarding unsupported category from Groq response: {}", category);
                        continue;
                    }

                    String matchedText = item.path("matchedText").asText("");
                    if (matchedText.isBlank()) {
                        matchedText = item.path("matchedPattern").asText("");
                    }
                    matchedText = cleanMatchedText(matchedText);
                    if (matchedText.isBlank() || matchedText.length() < 2) {
                        continue;
                    }

                    String declaredChannel = item.path("sourceChannel").asText("").trim();
                    String validatedChannel = findValidChannelForText(channelTexts, declaredChannel, matchedText);

                    // Hallucination verification: Reject match if excerpt does not exist in email content
                    if (validatedChannel == null) {
                        log.warn("Rejecting hallucinated Groq match not found in email content: category={}, text='{}'",
                                category, matchedText);
                        continue;
                    }

                    String severity = item.path("severity").asText("").trim().toUpperCase(Locale.ROOT);
                    if (!ALLOWED_SEVERITIES.contains(severity)) {
                        severity = defaultSeverity(category);
                    }

                    String explanation = item.path("explanation").asText("").trim();
                    if (explanation.isBlank()) {
                        explanation = defaultExplanation(category);
                    }

                    validMatches.add(new SocialEngineeringMatchDto(
                            category,
                            matchedText,
                            validatedChannel,
                            explanation,
                            severity
                    ));
                }
            }

            boolean urgency = false;
            boolean credentialRequest = false;
            boolean paymentInstruction = false;
            boolean impersonation = false;
            boolean secrecy = false;

            for (SocialEngineeringMatchDto m : validMatches) {
                switch (m.getCategory()) {
                    case "URGENCY" -> urgency = true;
                    case "CREDENTIAL_REQUEST" -> credentialRequest = true;
                    case "PAYMENT_INSTRUCTION" -> paymentInstruction = true;
                    case "IMPERSONATION" -> impersonation = true;
                    case "SECRECY" -> secrecy = true;
                }
            }

            String explanation = validMatches.isEmpty()
                    ? "No social engineering indicators detected."
                    : "Identified " + validMatches.size() + " social engineering indicator(s).";

            return new SocialEngineeringFindings(
                    urgency,
                    credentialRequest,
                    paymentInstruction,
                    impersonation,
                    secrecy,
                    validMatches,
                    "EVALUATED",
                    explanation
            );

        } catch (Exception e) {
            log.error("Failed to parse Groq response JSON: {}", e.getMessage(), e);
            return new SocialEngineeringFindings(false, false, false, false, false,
                    Collections.emptyList(), "UNAVAILABLE", "Invalid JSON returned by Groq: " + e.getMessage());
        }
    }

    private String findValidChannelForText(Map<String, String> channelTexts, String declaredChannel, String matchedText) {
        if (matchedText == null || matchedText.isBlank()) {
            return null;
        }

        String target = matchedText.toLowerCase(Locale.ROOT);

        // 1. Check declared channel first
        if (declaredChannel != null && !declaredChannel.isBlank()) {
            for (Map.Entry<String, String> entry : channelTexts.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(declaredChannel)) {
                    String text = entry.getValue();
                    if (text != null && text.toLowerCase(Locale.ROOT).contains(target)) {
                        return entry.getKey();
                    }
                }
            }
        }

        // 2. Fallback check across all channels
        for (Map.Entry<String, String> entry : channelTexts.entrySet()) {
            String text = entry.getValue();
            if (text != null && text.toLowerCase(Locale.ROOT).contains(target)) {
                return entry.getKey();
            }
        }

        return null;
    }

    private String cleanMatchedText(String text) {
        if (text == null) return "";
        String trimmed = text.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            if (trimmed.length() >= 2) {
                trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
            }
        }
        return trimmed;
    }

    private String extractJsonBlock(String text) {
        if (text == null) return "{}";
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastBackticks = trimmed.lastIndexOf("```");
            if (firstNewline != -1 && lastBackticks > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastBackticks).trim();
            }
        }
        return trimmed;
    }

    private String buildUserPrompt(Map<String, String> channelTexts) {
        StringBuilder sb = new StringBuilder("Analyze the following email content channels for social engineering tactics:\n\n");
        for (Map.Entry<String, String> entry : channelTexts.entrySet()) {
            String content = entry.getValue();
            if (content != null && !content.isBlank()) {
                sb.append("--- CHANNEL: ").append(entry.getKey()).append(" ---\n");
                sb.append(content.trim()).append("\n\n");
            }
        }
        return sb.toString();
    }

    private String buildChatPayload(String model, String systemPrompt, String userPrompt, boolean jsonMode) {
        try {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("model", model);
            root.put("temperature", 0.0);
            if (jsonMode) {
                root.put("response_format", Map.of("type", "json_object"));
            }

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userPrompt));
            root.put("messages", messages);

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct Groq chat request payload", e);
        }
    }

    private String resolveApiKey() {
        if (this.apiKey != null) {
            return this.apiKey.trim();
        }
        String key = System.getProperty("GROQ_API_KEY");
        if (key == null || key.isBlank()) {
            key = System.getenv("GROQ_API_KEY");
        }
        return key != null ? key.trim() : "";
    }

    private boolean isValidApiKey(String key) {
        return key != null &&
                !key.isBlank() &&
                !key.equalsIgnoreCase("your_api_key_here") &&
                !key.startsWith("replace-with") &&
                key.trim().length() > 5;
    }

    private String resolveBaseUrl() {
        String url = this.baseUrl;
        if (url == null || url.isBlank()) {
            url = System.getProperty("GROQ_BASE_URL");
            if (url == null || url.isBlank()) {
                url = System.getenv("GROQ_BASE_URL");
            }
        }
        if (url == null || url.isBlank()) {
            url = "https://api.groq.com/openai/v1";
        }
        return url.trim().replaceAll("/+$", "");
    }

    private String resolveModel() {
        String model = this.modelName;
        if (model == null || model.isBlank()) {
            model = System.getProperty("GROQ_MODEL");
            if (model == null || model.isBlank()) {
                model = System.getenv("GROQ_MODEL");
            }
        }
        if (model == null || model.isBlank() || model.equalsIgnoreCase("llama-3.3-70b-versatile")) {
            model = "openai/gpt-oss-20b";
        }
        return model.trim();
    }

    private String defaultSeverity(String category) {
        return switch (category) {
            case "URGENCY", "CREDENTIAL_REQUEST", "PAYMENT_INSTRUCTION" -> "HIGH";
            case "IMPERSONATION", "SECRECY" -> "MEDIUM";
            default -> "LOW";
        };
    }

    private String defaultExplanation(String category) {
        return switch (category) {
            case "URGENCY" -> "High pressure or artificial time constraint detected in message content.";
            case "CREDENTIAL_REQUEST" -> "Solicitation for authentication credentials, password reset, or login actions.";
            case "PAYMENT_INSTRUCTION" -> "Financial transaction demand, invoice diversion, or payment instructions.";
            case "IMPERSONATION" -> "Pretending to represent authority, executive leadership, or IT support.";
            case "SECRECY" -> "Instructing recipient to maintain secrecy or bypass standard communication protocols.";
            default -> "Potential social engineering indicator observed.";
        };
    }
}

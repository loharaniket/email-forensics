package com.emailforensics.backend.content;

import com.emailforensics.backend.dto.content.SocialEngineeringMatchDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroqSocialEngineeringServiceTest {

    private HttpClient mockHttpClient;
    private HttpResponse<String> mockHttpResponse;
    private GroqSocialEngineeringService service;

    private static final String FAKE_API_KEY = "gsk_test_api_key_1234567890abcdef";

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        mockHttpClient = Mockito.mock(HttpClient.class);
        mockHttpResponse = Mockito.mock(HttpResponse.class);

        service = new GroqSocialEngineeringService(
                FAKE_API_KEY,
                "https://api.groq.com/openai/v1",
                "openai/gpt-oss-20b",
                10,
                mockHttpClient
        );
    }

    private String buildGroqCompletionResponse(String assistantContent) {
        return """
                {
                  "id": "chatcmpl-test-123",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": %s
                      },
                      "finish_reason": "stop"
                    }
                  ]
                }
                """.formatted(quoteJsonString(assistantContent));
    }

    private String quoteJsonString(String raw) {
        return "\"" + raw.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r") + "\"";
    }

    @Test
    void shouldDetectAllFiveSocialEngineeringCategories() throws Exception {
        Map<String, String> channels = new LinkedHashMap<>();
        channels.put("SUBJECT", "Urgent Message from Chief Executive Officer");
        channels.put("BODY_PLAIN", "Immediate action is required! Please verify your password and credentials now. Also remit wire payment of $50,000 to the attached account.");
        channels.put("BODY_HTML", "<p>Please keep this confidential and bypass standard approvals as discussed.</p>");

        String llmJson = """
                {
                  "detected": true,
                  "matches": [
                    {
                      "category": "IMPERSONATION",
                      "matchedText": "Chief Executive Officer",
                      "sourceChannel": "SUBJECT",
                      "explanation": "Executive role impersonation",
                      "severity": "MEDIUM"
                    },
                    {
                      "category": "URGENCY",
                      "matchedText": "Immediate action is required",
                      "sourceChannel": "BODY_PLAIN",
                      "explanation": "Artificial time pressure",
                      "severity": "HIGH"
                    },
                    {
                      "category": "CREDENTIAL_REQUEST",
                      "matchedText": "verify your password and credentials",
                      "sourceChannel": "BODY_PLAIN",
                      "explanation": "Credential harvesting solicitation",
                      "severity": "HIGH"
                    },
                    {
                      "category": "PAYMENT_INSTRUCTION",
                      "matchedText": "remit wire payment of $50,000",
                      "sourceChannel": "BODY_PLAIN",
                      "explanation": "Direct wire transfer demand",
                      "severity": "HIGH"
                    },
                    {
                      "category": "SECRECY",
                      "matchedText": "keep this confidential and bypass standard approvals",
                      "sourceChannel": "BODY_HTML",
                      "explanation": "Instruction to bypass standard channels and maintain secrecy",
                      "severity": "MEDIUM"
                    }
                  ]
                }
                """;

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(buildGroqCompletionResponse(llmJson));
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        GroqSocialEngineeringService.SocialEngineeringFindings findings = service.analyze(channels);

        assertNotNull(findings);
        assertEquals("EVALUATED", findings.getStatus());
        assertTrue(findings.isUrgency());
        assertTrue(findings.isCredentialRequest());
        assertTrue(findings.isPaymentInstruction());
        assertTrue(findings.isImpersonation());
        assertTrue(findings.isSecrecy());
        assertEquals(5, findings.getMatches().size());

        assertTrue(findings.getMatches().stream().anyMatch(m -> "URGENCY".equals(m.getCategory())));
        assertTrue(findings.getMatches().stream().anyMatch(m -> "CREDENTIAL_REQUEST".equals(m.getCategory())));
        assertTrue(findings.getMatches().stream().anyMatch(m -> "PAYMENT_INSTRUCTION".equals(m.getCategory())));
        assertTrue(findings.getMatches().stream().anyMatch(m -> "IMPERSONATION".equals(m.getCategory())));
        assertTrue(findings.getMatches().stream().anyMatch(m -> "SECRECY".equals(m.getCategory())));
    }

    @Test
    void shouldHandleCleanEmailWithoutSocialEngineering() throws Exception {
        Map<String, String> channels = Map.of(
                "SUBJECT", "Weekly Team Meeting",
                "BODY_PLAIN", "Hi all, here are the meeting notes and project updates for this week."
        );

        String llmJson = """
                {
                  "detected": false,
                  "matches": []
                }
                """;

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(buildGroqCompletionResponse(llmJson));
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        GroqSocialEngineeringService.SocialEngineeringFindings findings = service.analyze(channels);

        assertNotNull(findings);
        assertEquals("EVALUATED", findings.getStatus());
        assertFalse(findings.isUrgency());
        assertFalse(findings.isCredentialRequest());
        assertFalse(findings.isPaymentInstruction());
        assertFalse(findings.isImpersonation());
        assertFalse(findings.isSecrecy());
        assertTrue(findings.getMatches().isEmpty());
    }

    @Test
    void shouldHandleEmptyOrBlankChannelsGracefully() throws Exception {
        GroqSocialEngineeringService.SocialEngineeringFindings empty = service.analyze(null);
        assertNotNull(empty);
        assertEquals("EVALUATED", empty.getStatus());
        assertFalse(empty.isUrgency());
        assertTrue(empty.getMatches().isEmpty());

        GroqSocialEngineeringService.SocialEngineeringFindings blank = service.analyze(Map.of("BODY_PLAIN", "   "));
        assertNotNull(blank);
        assertEquals("EVALUATED", blank.getStatus());
        assertFalse(blank.isUrgency());
        assertTrue(blank.getMatches().isEmpty());

        verify(mockHttpClient, never()).send(any(), any());
    }

    @Test
    void shouldHandleMissingApiKeyGracefully() throws Exception {
        GroqSocialEngineeringService noKeyService = new GroqSocialEngineeringService(
                "",
                "https://api.groq.com/openai/v1",
                "openai/gpt-oss-20b",
                10,
                mockHttpClient
        );

        Map<String, String> channels = Map.of("BODY_PLAIN", "Please click here to log in");
        GroqSocialEngineeringService.SocialEngineeringFindings findings = noKeyService.analyze(channels);

        assertNotNull(findings);
        assertEquals("UNAVAILABLE", findings.getStatus());
        assertTrue(findings.getStatusExplanation().contains("API key is not configured"));
        assertFalse(findings.isCredentialRequest());
        assertTrue(findings.getMatches().isEmpty());

        verify(mockHttpClient, never()).send(any(), any());
    }

    @Test
    void shouldRejectHallucinatedOrInventedEvidence() throws Exception {
        Map<String, String> channels = Map.of(
                "BODY_PLAIN", "Hi team, please find the quarterly performance slides attached for discussion."
        );

        // LLM hallucinated a quote that is NOT in the email
        String llmJson = """
                {
                  "detected": true,
                  "matches": [
                    {
                      "category": "URGENCY",
                      "matchedText": "your account will be suspended permanently in 24 hours",
                      "sourceChannel": "BODY_PLAIN",
                      "explanation": "Urgent deadline threat",
                      "severity": "HIGH"
                    }
                  ]
                }
                """;

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(buildGroqCompletionResponse(llmJson));
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        GroqSocialEngineeringService.SocialEngineeringFindings findings = service.analyze(channels);

        assertNotNull(findings);
        // Hallucinated evidence must be rejected!
        assertTrue(findings.getMatches().isEmpty());
        assertFalse(findings.isUrgency());
    }

    @Test
    void shouldDiscardInvalidCategoriesAndNormalizeSeverity() throws Exception {
        Map<String, String> channels = Map.of(
                "BODY_PLAIN", "Act immediately or access will be revoked."
        );

        String llmJson = """
                {
                  "detected": true,
                  "matches": [
                    {
                      "category": "UNKNOWN_CUSTOM_CATEGORY",
                      "matchedText": "Act immediately",
                      "sourceChannel": "BODY_PLAIN",
                      "explanation": "Not an allowed category",
                      "severity": "HIGH"
                    },
                    {
                      "category": "URGENCY",
                      "matchedText": "Act immediately",
                      "sourceChannel": "BODY_PLAIN",
                      "explanation": "Time pressure",
                      "severity": "INVALID_SEVERITY_LEVEL"
                    }
                  ]
                }
                """;

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(buildGroqCompletionResponse(llmJson));
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        GroqSocialEngineeringService.SocialEngineeringFindings findings = service.analyze(channels);

        assertNotNull(findings);
        assertEquals(1, findings.getMatches().size());
        SocialEngineeringMatchDto match = findings.getMatches().get(0);
        assertEquals("URGENCY", match.getCategory());
        assertEquals("HIGH", match.getSeverity()); // Normalized to default severity for URGENCY
        assertTrue(findings.isUrgency());
    }

    @Test
    void shouldHandleInvalidJsonFromGroq() throws Exception {
        Map<String, String> channels = Map.of("BODY_PLAIN", "Verify your account details");

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(buildGroqCompletionResponse("This is not valid JSON at all: { broken ..."));
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        GroqSocialEngineeringService.SocialEngineeringFindings findings = service.analyze(channels);

        assertNotNull(findings);
        assertEquals("UNAVAILABLE", findings.getStatus());
        assertTrue(findings.getStatusExplanation().contains("Invalid JSON"));
        assertFalse(findings.isCredentialRequest());
        assertTrue(findings.getMatches().isEmpty());
    }

    @Test
    void shouldHandleApiErrorsGracefully() throws Exception {
        Map<String, String> channels = Map.of("BODY_PLAIN", "Send wire payment");

        when(mockHttpResponse.statusCode()).thenReturn(429);
        when(mockHttpResponse.body()).thenReturn("{\"error\": {\"message\": \"Rate limit reached\"}}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        GroqSocialEngineeringService.SocialEngineeringFindings findings = service.analyze(channels);

        assertNotNull(findings);
        assertEquals("UNAVAILABLE", findings.getStatus());
        assertTrue(findings.getStatusExplanation().contains("HTTP 429"));
        assertFalse(findings.isPaymentInstruction());
        assertTrue(findings.getMatches().isEmpty());
    }

    @Test
    void shouldHandleTimeoutGracefully() throws Exception {
        Map<String, String> channels = Map.of("BODY_PLAIN", "Urgent notice");

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new HttpTimeoutException("Connection timed out after 10000ms"));

        GroqSocialEngineeringService.SocialEngineeringFindings findings = service.analyze(channels);

        assertNotNull(findings);
        assertEquals("UNAVAILABLE", findings.getStatus());
        assertTrue(findings.getStatusExplanation().contains("timed out"));
        assertFalse(findings.isUrgency());
        assertTrue(findings.getMatches().isEmpty());
    }

    @Test
    void shouldParseMarkdownCodeFencesInResponse() throws Exception {
        Map<String, String> channels = Map.of(
                "QR_PAYLOAD", "https://evil.corp/reset?token=123 (reset your password immediately)"
        );

        String markdownWrappedJson = """
                ```json
                {
                  "detected": true,
                  "matches": [
                    {
                      "category": "CREDENTIAL_REQUEST",
                      "matchedText": "reset your password",
                      "sourceChannel": "QR_PAYLOAD",
                      "explanation": "Password reset in QR payload",
                      "severity": "HIGH"
                    }
                  ]
                }
                ```
                """;

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(buildGroqCompletionResponse(markdownWrappedJson));
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        GroqSocialEngineeringService.SocialEngineeringFindings findings = service.analyze(channels);

        assertNotNull(findings);
        assertEquals("EVALUATED", findings.getStatus());
        assertTrue(findings.isCredentialRequest());
        assertEquals(1, findings.getMatches().size());
        assertEquals("QR_PAYLOAD", findings.getMatches().get(0).getSourceChannel());
    }
}


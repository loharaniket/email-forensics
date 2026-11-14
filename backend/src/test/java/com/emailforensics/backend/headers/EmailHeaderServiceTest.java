package com.emailforensics.backend.headers;

import com.emailforensics.backend.analysis.AnalysisResult;
import com.emailforensics.backend.dto.headers.ReceivedHopDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailHeaderServiceTest {

    private HttpClient mockHttpClient;
    private HttpResponse<String> mockHttpResponse;
    private EmailHeaderService service;

    private final String sampleRawHeaders = """
            From: Alice <alice@example.com>
            To: Bob <bob@company.com>
            Subject: Quarterly Report
            Date: Mon, 14 Nov 2026 10:00:00 +0000
            Message-ID: <12345@example.com>
            """;

    private final String powerlineSuccessJson = """
            {
              "authentication": {
                "spf": {
                  "result": "pass",
                  "record": "v=spf1 include:_spf.google.com ~all",
                  "ip": "209.85.220.41"
                },
                "dkim": {
                  "result": "pass",
                  "domain": "example.com",
                  "selector": "s2023",
                  "record": "v=DKIM1; k=rsa; p=MIGf..."
                },
                "dmarc": {
                  "result": "pass",
                  "policy": "reject"
                },
                "arc": {
                  "result": "pass"
                }
              },
              "hops": [
                {
                  "hop": 1,
                  "from": "mail.example.com",
                  "by": "mx.google.com",
                  "with": "ESMTPS",
                  "ip": "209.85.220.41",
                  "delay": 0
                },
                {
                  "hop": 2,
                  "from": "mx.google.com",
                  "by": "corp.company.com",
                  "with": "ESMTP",
                  "ip": "10.0.0.1",
                  "delay": 4
                }
              ]
            }
            """;

    private final String testmailSuccessJson = """
            {
              "authentication": {
                "spf": {
                  "status": "pass",
                  "record": "v=spf1 mx ~all",
                  "relay_ip": "198.51.100.22"
                },
                "dkim": {
                  "status": "pass",
                  "domain": "example.com",
                  "selector": "default"
                },
                "dmarc": {
                  "status": "pass",
                  "policy": "quarantine"
                },
                "arc": {
                  "status": "none"
                }
              },
              "hops": []
            }
            """;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        mockHttpClient = Mockito.mock(HttpClient.class);
        mockHttpResponse = Mockito.mock(HttpResponse.class);
        service = new EmailHeaderService(
                mockHttpClient,
                "powerline-test-key",
                "https://powerline.ai/api/tools/headers",
                "testmail-test-key",
                "https://api.testmail.app/api/tools/headers",
                10
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnPowerlineWhenPrimarySucceeds() throws Exception {
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(powerlineSuccessJson);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        HeaderAuthResult result = service.analyzeHeader(sampleRawHeaders);

        assertNotNull(result);
        assertEquals("Powerline.ai", result.getProviderName());
        assertEquals("SUCCESS", result.getStatus());
        assertFalse(result.isServiceFailure());
        assertEquals("pass", result.getSpfResult());
        assertEquals("pass", result.getDkimResult());
        assertEquals("pass", result.getDmarcResult());
        assertEquals("pass", result.getArcResult());
        assertEquals("209.85.220.41", result.getSpfRelayIp());
        assertEquals("example.com", result.getDkimDomain());
        assertEquals("reject", result.getDmarcPolicy());

        List<ReceivedHopDto> hops = result.getHops();
        assertEquals(2, hops.size());
        assertEquals(1, hops.get(0).getHopOrder());
        assertEquals("mail.example.com", hops.get(0).getFromHost());
        assertEquals(4L, hops.get(1).getDelaySeconds());

        // Verify only 1 HTTP request was made to Powerline
        verify(mockHttpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldNotTreatAuthenticationFailAsApiFailure() throws Exception {
        // An email failing SPF/DKIM/DMARC is a valid security result, NOT an API outage
        String failAuthJson = """
                {
                  "authentication": {
                    "spf": { "result": "fail" },
                    "dkim": { "result": "fail" },
                    "dmarc": { "result": "fail" },
                    "arc": { "result": "none" }
                  }
                }
                """;

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(failAuthJson);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        HeaderAuthResult result = service.analyzeHeader(sampleRawHeaders);

        assertNotNull(result);
        assertEquals("Powerline.ai", result.getProviderName());
        assertEquals("SUCCESS", result.getStatus());
        assertFalse(result.isServiceFailure());
        assertEquals("fail", result.getSpfResult());
        assertEquals("fail", result.getDkimResult());
        assertEquals("fail", result.getDmarcResult());

        // Crucial: Fallback must NOT trigger on authentication check failure
        verify(mockHttpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldFallbackToTestmailWhenPowerlineFails() throws Exception {
        HttpResponse<String> powerlineFailResponse = Mockito.mock(HttpResponse.class);
        when(powerlineFailResponse.statusCode()).thenReturn(503);
        when(powerlineFailResponse.body()).thenReturn("{\"error\":\"Service Unavailable\"}");

        HttpResponse<String> testmailSuccessResponse = Mockito.mock(HttpResponse.class);
        when(testmailSuccessResponse.statusCode()).thenReturn(200);
        when(testmailSuccessResponse.body()).thenReturn(testmailSuccessJson);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(powerlineFailResponse)
                .thenReturn(testmailSuccessResponse);

        HeaderAuthResult result = service.analyzeHeader(sampleRawHeaders);

        assertNotNull(result);
        assertEquals("Testmail.app", result.getProviderName());
        assertEquals("SUCCESS", result.getStatus());
        assertFalse(result.isServiceFailure());
        assertEquals("pass", result.getSpfResult());
        assertEquals("pass", result.getDkimResult());
        assertEquals("pass", result.getDmarcResult());

        // Verify both providers were called sequentially
        verify(mockHttpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnAllUnavailableWhenBothProvidersFail() throws Exception {
        HttpResponse<String> powerlineFail = Mockito.mock(HttpResponse.class);
        when(powerlineFail.statusCode()).thenReturn(500);
        when(powerlineFail.body()).thenReturn("Internal Server Error");

        HttpResponse<String> testmailFail = Mockito.mock(HttpResponse.class);
        when(testmailFail.statusCode()).thenReturn(503);
        when(testmailFail.body()).thenReturn("Service Unavailable");

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(powerlineFail)
                .thenReturn(testmailFail);

        HeaderAuthResult result = service.analyzeHeader(sampleRawHeaders);

        assertNotNull(result);
        assertEquals("UNAVAILABLE", result.getProviderName());
        assertEquals("ALL_PROVIDERS_UNAVAILABLE", result.getStatus());
        assertTrue(result.isServiceFailure());
        assertEquals("unavailable", result.getSpfResult());
        assertEquals("unavailable", result.getDkimResult());
        assertEquals("unavailable", result.getDmarcResult());
        assertTrue(result.getErrorMessage().contains("temporarily unavailable"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldHandleTimeoutAsServiceFailure() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new HttpTimeoutException("Connection timed out"));

        // Service tries Powerline, gets timeout, tries Testmail, gets timeout
        HeaderAuthResult result = service.analyzeHeader(sampleRawHeaders);

        assertNotNull(result);
        assertEquals("ALL_PROVIDERS_UNAVAILABLE", result.getStatus());
        assertTrue(result.isServiceFailure());
        assertTrue(result.getErrorMessage().contains("timed out"));
    }

    @Test
    void shouldHandleMissingTestmailApiKeyGracefully() {
        EmailHeaderService unconfiguredService = new EmailHeaderService(
                mockHttpClient,
                null,
                "https://powerline.ai/api/tools/headers",
                "", // Missing Testmail key
                "https://api.testmail.app/api/tools/headers",
                10
        );

        HeaderAuthResult tmResult = unconfiguredService.callTestmailApi(sampleRawHeaders);
        assertNotNull(tmResult);
        assertTrue(tmResult.isServiceFailure());
        assertTrue(tmResult.getErrorMessage().contains("not configured"));
    }

    @Test
    void shouldMarkMissingAuthChecksAsUnsupported() throws Exception {
        // Response with only SPF, without DKIM, DMARC, or ARC
        String partialJson = """
                {
                  "authentication": {
                    "spf": { "result": "pass" }
                  }
                }
                """;

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(partialJson);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        HeaderAuthResult result = service.callPowerlineApi(sampleRawHeaders);

        assertNotNull(result);
        assertEquals("pass", result.getSpfResult());
        assertEquals("unsupported", result.getDkimResult());
        assertEquals("unsupported", result.getDmarcResult());
        assertEquals("unsupported", result.getArcResult());
    }

    @Test
    void shouldHandleEmptyHeaderInput() {
        HeaderAuthResult result = service.analyzeHeader("");
        assertNotNull(result);
        assertTrue(result.isServiceFailure());
        assertTrue(result.getErrorMessage().contains("No email headers provided"));
    }

    @Test
    void shouldSaveResultIntoAnalysisResultEntity() {
        AnalysisResult entity = new AnalysisResult();

        HeaderAuthResult authResult = HeaderAuthResult.success("Powerline.ai");
        authResult.setSpfResult("pass");
        authResult.setDkimResult("pass");
        authResult.setDmarcResult("pass");
        authResult.setArcResult("none");
        authResult.setRawResponseJson("{\"status\":\"ok\"}");
        authResult.setSummary("Validated via Powerline.ai.");

        service.saveResult(entity, authResult);

        assertEquals("Powerline.ai", entity.getAuthApiProvider());
        assertEquals("SUCCESS", entity.getAuthApiStatus());
        assertEquals("pass", entity.getReportedSpf());
        assertEquals("pass", entity.getReportedDkim());
        assertEquals("pass", entity.getReportedDmarc());
        assertEquals("none", entity.getReportedArc());
        assertEquals("{\"status\":\"ok\"}", entity.getAuthApiResponseJson());
        assertEquals("Validated via Powerline.ai.", entity.getIndependentVerification());
    }
}

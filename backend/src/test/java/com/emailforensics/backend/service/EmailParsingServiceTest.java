package com.emailforensics.backend.service;

import com.emailforensics.backend.dto.ParsedHeaderResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailParsingServiceTest {

    private EmailParsingService parsingService;

    @BeforeEach
    void setUp() {
        parsingService = new EmailParsingService();
    }

    @Test
    void shouldParseHeadersAndDetectMatchingDomains() {
        String emailText = "From: Alice Support <support@legit.com>\r\n" +
                "To: user@example.com\r\n" +
                "Reply-To: support@legit.com\r\n" +
                "Return-Path: <bounce@legit.com>\r\n" +
                "Subject: Your Monthly Statement\r\n" +
                "Date: Mon, 15 Nov 2026 12:00:00 +0000\r\n" +
                "\r\n" +
                "Hello Alice";

        ByteArrayInputStream input = new ByteArrayInputStream(emailText.getBytes(StandardCharsets.UTF_8));
        ParsedHeaderResult result = parsingService.parseHeaders(input);

        assertNotNull(result);
        assertEquals("Your Monthly Statement", result.getSubject());
        assertEquals("Alice Support <support@legit.com>", result.getSender());
        assertEquals("legit.com", result.getFromDomain());
        assertEquals("support@legit.com", result.getReplyTo());
        assertEquals("bounce@legit.com", result.getReturnPath());
        assertFalse(result.isReplyToMismatch());
        assertFalse(result.isReturnPathMismatch());
    }

    @Test
    void shouldDetectReplyToAndReturnPathMismatch() {
        String emailText = "From: CEO <executive@company.com>\r\n" +
                "To: staff@company.com\r\n" +
                "Reply-To: wire@hacker-domain.com\r\n" +
                "Return-Path: <phish@external-relay.net>\r\n" +
                "Subject: Urgent Wire Request\r\n" +
                "\r\n" +
                "Action needed";

        ByteArrayInputStream input = new ByteArrayInputStream(emailText.getBytes(StandardCharsets.UTF_8));
        ParsedHeaderResult result = parsingService.parseHeaders(input);

        assertNotNull(result);
        assertEquals("CEO <executive@company.com>", result.getSender());
        assertEquals("company.com", result.getFromDomain());
        assertTrue(result.isReplyToMismatch());
        assertTrue(result.isReturnPathMismatch());
    }

    @Test
    void shouldExtractReceivedHopsChain() {
        String emailText = "Received: from mail.outbound.com ([198.51.100.1]) by mx.google.com with ESMTPS id abc; Mon, 15 Nov 2026 10:05:00 -0800\r\n" +
                "Received: from internal.lan (10.0.0.5) by mail.outbound.com with SMTP id 123; Mon, 15 Nov 2026 10:04:00 -0800\r\n" +
                "From: test@example.com\r\n" +
                "Subject: Route Test\r\n" +
                "\r\n" +
                "Body";

        ByteArrayInputStream input = new ByteArrayInputStream(emailText.getBytes(StandardCharsets.UTF_8));
        ParsedHeaderResult result = parsingService.parseHeaders(input);

        assertNotNull(result.getReceivedHops());
        assertEquals(2, result.getReceivedHops().size());
        assertEquals("mail.outbound.com", result.getReceivedHops().get(0).get("from"));
        assertEquals("198.51.100.1", result.getReceivedHops().get(0).get("ip"));
    }
}

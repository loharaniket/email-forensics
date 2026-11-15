package com.emailforensics.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailAuthServiceTest {

    private MailAuthService mailAuthService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        mailAuthService = new MailAuthService("tools/mailauth/verify.mjs");
    }

    @Test
    void shouldReturnFallbackOnNullPath() {
        String result = mailAuthService.authenticateEmail(null);
        assertNotNull(result);
        assertTrue(result.contains("NOT_VERIFIABLE"));
    }

    @Test
    void shouldReturnFallbackOnMissingScript() {
        MailAuthService missingService = new MailAuthService("tools/non_existent.mjs");
        String result = missingService.authenticateEmail(tempDir.resolve("sample.eml"));
        assertNotNull(result);
        assertTrue(result.contains("NOT_VERIFIABLE"));
    }

    @Test
    void shouldAuthenticateValidFileWithHelper() throws Exception {
        Path emlFile = tempDir.resolve("test.eml");
        Files.writeString(emlFile, "From: auth@example.com\r\nSubject: Test\r\n\r\nHello", StandardCharsets.UTF_8);

        String result = mailAuthService.authenticateEmail(emlFile);

        assertNotNull(result);
        assertTrue(result.startsWith("{"));
        assertTrue(result.contains("spf"));
        assertTrue(result.contains("dkim"));
        assertTrue(result.contains("dmarc"));
    }
}

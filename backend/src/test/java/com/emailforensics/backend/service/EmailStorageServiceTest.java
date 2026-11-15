package com.emailforensics.backend.service;

import com.emailforensics.backend.common.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailStorageServiceTest {

    @TempDir
    Path tempDir;

    private EmailStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new EmailStorageService(tempDir.toString());
    }

    @Test
    void shouldComputeAccurateSha256() throws Exception {
        byte[] content = "From: test@example.com\r\nSubject: Test\r\n\r\nBody".getBytes(StandardCharsets.UTF_8);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String expectedHash = HexFormat.of().formatHex(digest.digest(content));

        String computedHash = storageService.computeSha256(content);

        assertEquals(expectedHash, computedHash);
    }

    @Test
    void shouldSaveOriginalEmailUnderUuidDirectory() {
        byte[] content = "Sample raw email content".getBytes(StandardCharsets.UTF_8);

        String relativePath = storageService.saveOriginalEmail(content);

        assertNotNull(relativePath);
        assertTrue(relativePath.endsWith("original.eml"));

        Resource loadedResource = storageService.loadOriginalEmail(relativePath);
        assertTrue(loadedResource.exists());
    }

    @Test
    void shouldRejectEmptyBytesForSaving() {
        assertThrows(BadRequestException.class, () -> storageService.saveOriginalEmail(new byte[0]));
    }

    @Test
    void shouldRejectPathTraversalInLoad() {
        assertThrows(BadRequestException.class, () -> storageService.loadOriginalEmail("../../etc/passwd"));
    }

    @Test
    void shouldSanitizeFilenames() {
        assertEquals("phishing.eml", storageService.sanitizeFileName("../../phishing.eml"));
        assertEquals("invoice.eml", storageService.sanitizeFileName("C:\\Windows\\invoice.eml"));
        assertEquals("evidence.eml", storageService.sanitizeFileName(null));
        assertEquals("evidence.eml", storageService.sanitizeFileName("   "));
        assertEquals("evidence.eml", storageService.sanitizeFileName(".."));
    }
}

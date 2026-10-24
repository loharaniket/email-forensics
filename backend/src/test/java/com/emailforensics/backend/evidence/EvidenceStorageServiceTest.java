package com.emailforensics.backend.evidence;

import com.emailforensics.backend.common.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvidenceStorageServiceTest {

    @TempDir
    Path tempDir;

    private EvidenceStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new EvidenceStorageService(tempDir.toString());
        storageService.init();
    }

    @Test
    void shouldStoreEvidenceAndComputeCorrectSha256() throws Exception {
        byte[] content = "From: sender@example.com\r\nSubject: Test\r\n\r\nHello World".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String expectedHash = HexFormat.of().formatHex(digest.digest(content));

        StoredEvidenceFile stored = storageService.storeEvidence(inputStream);

        assertNotNull(stored.getStorageKey());
        assertTrue(stored.getStorageKey().endsWith(".eml"));
        assertEquals(expectedHash, stored.getSha256Hash());
        assertEquals((long) content.length, stored.getFileSizeBytes());

        String recomputedHash = storageService.computeFileHash(stored.getStorageKey());
        assertEquals(expectedHash, recomputedHash);
    }

    @Test
    void shouldRejectEmptyStream() {
        ByteArrayInputStream emptyStream = new ByteArrayInputStream(new byte[0]);
        assertThrows(BadRequestException.class, () -> storageService.storeEvidence(emptyStream));
    }

    @Test
    void shouldSanitizePathTraversalInFilename() {
        String unsafe1 = "../../etc/passwd.eml";
        assertEquals("passwd.eml", storageService.sanitizeFilename(unsafe1));

        String unsafe2 = "C:\\Windows\\System32\\test.eml";
        assertEquals("test.eml", storageService.sanitizeFilename(unsafe2));

        String unsafe3 = "../../../nested/folder/sample.eml";
        assertEquals("sample.eml", storageService.sanitizeFilename(unsafe3));

        assertEquals("evidence.eml", storageService.sanitizeFilename(null));
        assertEquals("evidence.eml", storageService.sanitizeFilename("   "));
        assertEquals("evidence.eml", storageService.sanitizeFilename(".."));
    }
}

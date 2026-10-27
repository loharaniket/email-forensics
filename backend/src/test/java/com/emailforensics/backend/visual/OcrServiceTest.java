package com.emailforensics.backend.visual;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OcrServiceTest {

    private OcrService ocrService;

    @BeforeEach
    void setUp() {
        ocrService = new OcrService("./nonexistent_tessdata_path_for_test");
    }

    private byte[] createTestImage(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);
        g2.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }

    @Test
    void shouldReportUnavailableWhenTessdataDoesNotExist() throws Exception {
        assertFalse(ocrService.isOcrAvailable());

        byte[] img = createTestImage(100, 100);
        OcrService.OcrResult result = ocrService.processImage(img, "dummyhash123");

        assertNotNull(result);
        assertEquals("UNAVAILABLE", result.getOcrStatus());
        assertEquals("UNAVAILABLE", result.getDataMode());
        assertNull(result.getOcrText());
        assertNotNull(result.getProcessingError());
    }

    @Test
    void shouldReturnExtractedTextWhenRegisteredInMockStore() throws Exception {
        String testHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        ocrService.registerMockOcrText(testHash, "INVOICE #99482 DUE IMMEDIATELY");

        byte[] img = createTestImage(100, 100);
        OcrService.OcrResult result = ocrService.processImage(img, testHash);

        assertNotNull(result);
        assertEquals("EXTRACTED", result.getOcrStatus());
        assertEquals("SIMULATED", result.getDataMode());
        assertEquals("INVOICE #99482 DUE IMMEDIATELY", result.getOcrText());
        assertNull(result.getProcessingError());
    }

    @Test
    void shouldReturnNoTextFoundWhenMockEmpty() throws Exception {
        String testHash = "blankhash123";
        ocrService.registerMockOcrText(testHash, "   ");

        byte[] img = createTestImage(100, 100);
        OcrService.OcrResult result = ocrService.processImage(img, testHash);

        assertNotNull(result);
        assertEquals("NO_TEXT_FOUND", result.getOcrStatus());
        assertEquals("SIMULATED", result.getDataMode());
        assertNull(result.getOcrText());
    }

    @Test
    void shouldFailOnCorruptImageBytes() {
        byte[] corrupt = "not_an_image".getBytes(StandardCharsets.UTF_8);
        OcrService.OcrResult result = ocrService.processImage(corrupt, "corrupthash");

        assertNotNull(result);
        assertEquals("FAILED", result.getOcrStatus());
        assertTrue(result.getProcessingError().contains("Corrupt or unsupported image"));
    }

    @Test
    void shouldFailOnEmptyBytes() {
        OcrService.OcrResult result = ocrService.processImage(new byte[0], "empty");

        assertNotNull(result);
        assertEquals("FAILED", result.getOcrStatus());
    }

    @Test
    void shouldSkipOversizedDimensions() throws Exception {
        byte[] large = createTestImage(4050, 100);
        OcrService.OcrResult result = ocrService.processImage(large, "largehash");

        assertNotNull(result);
        assertEquals("SKIPPED", result.getOcrStatus());
        assertTrue(result.getProcessingError().contains("exceed limit"));
    }
}

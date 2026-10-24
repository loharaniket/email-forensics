package com.emailforensics.backend.visual;

import net.sourceforge.tess4j.Tesseract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OcrService {

    private static final long MAX_IMAGE_BYTES = 10 * 1024 * 1024;
    private static final int MAX_DIMENSION = 4000;

    private final String tessdataPath;
    private final Map<String, String> mockOcrStore = new ConcurrentHashMap<>();

    public OcrService(@Value("${emailforensics.ocr.tessdata-path:./tessdata}") String tessdataPath) {
        this.tessdataPath = tessdataPath;
    }

    public void registerMockOcrText(String sha256Hash, String text) {
        mockOcrStore.put(sha256Hash.toLowerCase(), text);
    }

    public static class OcrResult {
        private String ocrText;
        private String ocrStatus;
        private String processingError;

        public OcrResult(String ocrText, String ocrStatus, String processingError) {
            this.ocrText = ocrText;
            this.ocrStatus = ocrStatus;
            this.processingError = processingError;
        }

        public static OcrResult extracted(String text) {
            return new OcrResult(text, "EXTRACTED", null);
        }

        public static OcrResult noTextFound() {
            return new OcrResult(null, "NO_TEXT_FOUND", null);
        }

        public static OcrResult unavailable(String reason) {
            return new OcrResult(null, "UNAVAILABLE", reason);
        }

        public static OcrResult skipped(String reason) {
            return new OcrResult(null, "SKIPPED", reason);
        }

        public static OcrResult failed(String error) {
            return new OcrResult(null, "FAILED", error);
        }

        public String getOcrText() {
            return ocrText;
        }

        public String getOcrStatus() {
            return ocrStatus;
        }

        public String getProcessingError() {
            return processingError;
        }
    }

    public boolean isOcrAvailable() {
        try {
            File dir = new File(tessdataPath);
            if (!dir.exists() || !dir.isDirectory()) {
                return false;
            }
            File eng = new File(dir, "eng.traineddata");
            return eng.exists();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public OcrResult processImage(byte[] imageBytes, String sha256Hash) {
        if (imageBytes == null || imageBytes.length == 0) {
            return OcrResult.failed("Image payload is empty");
        }

        if (imageBytes.length > MAX_IMAGE_BYTES) {
            return OcrResult.skipped("Image size " + imageBytes.length + " bytes exceeds limit of " + MAX_IMAGE_BYTES);
        }

        if (sha256Hash != null && mockOcrStore.containsKey(sha256Hash.toLowerCase())) {
            String mocked = mockOcrStore.get(sha256Hash.toLowerCase());
            if (mocked == null || mocked.trim().isEmpty()) {
                return OcrResult.noTextFound();
            }
            return OcrResult.extracted(mocked.trim());
        }

        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                return OcrResult.failed("Corrupt or unsupported image format");
            }
        } catch (Exception e) {
            return OcrResult.failed("Failed to decode image: " + e.getMessage());
        }

        if (image.getWidth() > MAX_DIMENSION || image.getHeight() > MAX_DIMENSION) {
            return OcrResult.skipped("Image dimensions " + image.getWidth() + "x" + image.getHeight() + " exceed limit of " + MAX_DIMENSION + "px");
        }

        if (!isOcrAvailable()) {
            return OcrResult.unavailable("Tesseract OCR native engine or trained data not found in " + tessdataPath);
        }

        try {
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessdataPath);
            tesseract.setLanguage("eng");
            String extracted = tesseract.doOCR(image);
            if (extracted == null || extracted.trim().isEmpty()) {
                return OcrResult.noTextFound();
            }
            return OcrResult.extracted(extracted.trim());
        } catch (Throwable t) {
            return OcrResult.failed("OCR processing error: " + t.getMessage());
        }
    }
}

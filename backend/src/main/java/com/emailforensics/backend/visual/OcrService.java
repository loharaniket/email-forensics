package com.emailforensics.backend.visual;

import com.emailforensics.backend.infrastructure.DataProvenance;
import com.emailforensics.backend.visual.fixtures.DefaultOcrFixtureProvider;
import com.emailforensics.backend.visual.fixtures.OcrFixtureProvider;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;

@Service
public class OcrService {

    private static final long MAX_IMAGE_BYTES = 10 * 1024 * 1024;
    private static final int MAX_DIMENSION = 4000;

    private final String tessdataPath;
    private final OcrFixtureProvider fixtureProvider;
    private final Environment environment;

    @Autowired
    public OcrService(
            @Value("${emailforensics.ocr.tessdata-path:./tessdata}") String tessdataPath,
            @Autowired(required = false) OcrFixtureProvider fixtureProvider,
            @Autowired(required = false) Environment environment) {
        this.tessdataPath = tessdataPath;
        this.fixtureProvider = fixtureProvider;
        this.environment = environment;
    }

    public OcrService(String tessdataPath) {
        this(tessdataPath, new DefaultOcrFixtureProvider(), null);
    }

    public OcrService(String tessdataPath, OcrFixtureProvider fixtureProvider) {
        this(tessdataPath, fixtureProvider, null);
    }

    private boolean isProduction() {
        return environment != null && environment.acceptsProfiles(Profiles.of("prod", "production"));
    }

    public void registerMockOcrText(String sha256Hash, String text) {
        if (!isProduction() && fixtureProvider != null) {
            fixtureProvider.registerMockOcrText(sha256Hash, text);
        }
    }

    public OcrFixtureProvider getFixtureProvider() {
        return fixtureProvider;
    }

    public static class OcrResult {
        private String ocrText;
        private String ocrStatus;
        private String processingError;
        private String dataMode;

        public OcrResult(String ocrText, String ocrStatus, String processingError) {
            this(ocrText, ocrStatus, processingError, DataProvenance.REAL.name());
        }

        public OcrResult(String ocrText, String ocrStatus, String processingError, String dataMode) {
            this.ocrText = ocrText;
            this.ocrStatus = ocrStatus;
            this.processingError = processingError;
            this.dataMode = dataMode != null ? dataMode : DataProvenance.REAL.name();
        }

        public static OcrResult extracted(String text) {
            return new OcrResult(text, "EXTRACTED", null, DataProvenance.REAL.name());
        }

        public static OcrResult extracted(String text, String dataMode) {
            return new OcrResult(text, "EXTRACTED", null, dataMode);
        }

        public static OcrResult noTextFound() {
            return new OcrResult(null, "NO_TEXT_FOUND", null, DataProvenance.REAL.name());
        }

        public static OcrResult noTextFound(String dataMode) {
            return new OcrResult(null, "NO_TEXT_FOUND", null, dataMode);
        }

        public static OcrResult unavailable(String reason) {
            return new OcrResult(null, "UNAVAILABLE", reason, DataProvenance.UNAVAILABLE.name());
        }

        public static OcrResult unavailable(String reason, String dataMode) {
            return new OcrResult(null, "UNAVAILABLE", reason, dataMode);
        }

        public static OcrResult skipped(String reason) {
            return new OcrResult(null, "SKIPPED", reason, DataProvenance.NOT_CHECKED.name());
        }

        public static OcrResult skipped(String reason, String dataMode) {
            return new OcrResult(null, "SKIPPED", reason, dataMode);
        }

        public static OcrResult failed(String error) {
            return new OcrResult(null, "FAILED", error, DataProvenance.NOT_CHECKED.name());
        }

        public static OcrResult failed(String error, String dataMode) {
            return new OcrResult(null, "FAILED", error, dataMode);
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

        public String getDataMode() {
            return dataMode;
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
            return OcrResult.failed("Image payload is empty", DataProvenance.NOT_CHECKED.name());
        }

        if (imageBytes.length > MAX_IMAGE_BYTES) {
            return OcrResult.skipped("Image size " + imageBytes.length + " bytes exceeds limit of " + MAX_IMAGE_BYTES, DataProvenance.NOT_CHECKED.name());
        }

        if (!isProduction() && fixtureProvider != null && sha256Hash != null && fixtureProvider.hasMock(sha256Hash)) {
            String mocked = fixtureProvider.getMockOcrText(sha256Hash);
            if (mocked == null || mocked.trim().isEmpty()) {
                return OcrResult.noTextFound(DataProvenance.SIMULATED.name());
            }
            return OcrResult.extracted(mocked.trim(), DataProvenance.SIMULATED.name());
        }

        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                return OcrResult.failed("Corrupt or unsupported image format", DataProvenance.NOT_CHECKED.name());
            }
        } catch (Exception e) {
            return OcrResult.failed("Failed to decode image: " + e.getMessage(), DataProvenance.NOT_CHECKED.name());
        }

        if (image.getWidth() > MAX_DIMENSION || image.getHeight() > MAX_DIMENSION) {
            return OcrResult.skipped("Image dimensions " + image.getWidth() + "x" + image.getHeight() + " exceed limit of " + MAX_DIMENSION + "px", DataProvenance.NOT_CHECKED.name());
        }

        if (!isOcrAvailable()) {
            return OcrResult.unavailable("Tesseract OCR native engine or trained data not found in " + tessdataPath, DataProvenance.UNAVAILABLE.name());
        }

        try {
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessdataPath);
            tesseract.setLanguage("eng");
            String extracted = tesseract.doOCR(image);
            if (extracted == null || extracted.trim().isEmpty()) {
                return OcrResult.noTextFound(DataProvenance.REAL.name());
            }
            return OcrResult.extracted(extracted.trim(), DataProvenance.REAL.name());
        } catch (Throwable t) {
            return OcrResult.failed("OCR processing error: " + t.getMessage(), DataProvenance.UNAVAILABLE.name());
        }
    }
}

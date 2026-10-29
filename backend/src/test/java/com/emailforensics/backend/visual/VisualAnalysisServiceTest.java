package com.emailforensics.backend.visual;

import com.emailforensics.backend.dto.visual.VisualArtifactDto;
import com.emailforensics.backend.evidence.AnalysisRunRepository;
import com.emailforensics.backend.evidence.EmailEvidenceRepository;
import com.emailforensics.backend.evidence.EvidenceStorageService;
import com.emailforensics.backend.infrastructure.DnsLookupService;
import com.emailforensics.backend.infrastructure.IpClassifier;
import com.emailforensics.backend.urlintel.ExtractedUrlRepository;
import com.emailforensics.backend.urlintel.UrlHeuristicsService;
import com.emailforensics.backend.urlintel.UrlNormalizationService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisualAnalysisServiceTest {

    private VisualAnalysisService visualAnalysisService;
    private QrDecodingService qrDecodingService;

    @BeforeEach
    void setUp() {
        VisualArtifactRepository visualArtifactRepo = Mockito.mock(VisualArtifactRepository.class);
        QrArtifactRepository qrArtifactRepo = Mockito.mock(QrArtifactRepository.class);
        EmailEvidenceRepository evidenceRepo = Mockito.mock(EmailEvidenceRepository.class);
        AnalysisRunRepository analysisRunRepo = Mockito.mock(AnalysisRunRepository.class);
        EvidenceStorageService storageService = Mockito.mock(EvidenceStorageService.class);
        ExtractedUrlRepository extractedUrlRepo = Mockito.mock(ExtractedUrlRepository.class);
        DnsLookupService dnsLookupService = Mockito.mock(DnsLookupService.class);

        IpClassifier ipClassifier = new IpClassifier();
        UrlNormalizationService urlNormalizationService = new UrlNormalizationService();
        UrlHeuristicsService urlHeuristicsService = new UrlHeuristicsService(ipClassifier, urlNormalizationService);
        qrDecodingService = new QrDecodingService(urlNormalizationService);

        visualAnalysisService = new VisualAnalysisService(
                visualArtifactRepo,
                qrArtifactRepo,
                evidenceRepo,
                analysisRunRepo,
                storageService,
                qrDecodingService,
                urlNormalizationService,
                urlHeuristicsService,
                dnsLookupService,
                extractedUrlRepo
        );
    }

    private byte[] createEmlWithInlineQrImage(String qrPayload) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(qrPayload, BarcodeFormat.QR_CODE, 200, 200);
        ByteArrayOutputStream imgBaos = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", imgBaos);
        byte[] imgBytes = imgBaos.toByteArray();
        String base64Img = Base64.getMimeEncoder().encodeToString(imgBytes);

        String eml = "From: security@alerts.test\r\n" +
                "To: user@test.org\r\n" +
                "Subject: Security Code\r\n" +
                "MIME-Version: 1.0\r\n" +
                "Content-Type: multipart/related; boundary=\"----=_Part_Boundary_123\"\r\n\r\n" +
                "------=_Part_Boundary_123\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n\r\n" +
                "<html><body><p>Scan QR code</p><img src=\"cid:qr1\" /></body></html>\r\n" +
                "------=_Part_Boundary_123\r\n" +
                "Content-Type: image/png\r\n" +
                "Content-Disposition: inline; filename=\"login_qr.png\"\r\n" +
                "Content-ID: <qr1>\r\n" +
                "Content-Transfer-Encoding: base64\r\n\r\n" +
                base64Img + "\r\n" +
                "------=_Part_Boundary_123--\r\n";
        return eml.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void shouldExtractImageAndDecodeQrPayloadFromEml() throws Exception {
        String testUrl = "https://phishing-gateway.security-check.test/login";
        byte[] emlBytes = createEmlWithInlineQrImage(testUrl);

        try (InputStream in = new ByteArrayInputStream(emlBytes)) {
            List<VisualArtifactDto> visuals = visualAnalysisService.analyzeImagesFromStream(in);

            assertNotNull(visuals);
            assertEquals(1, visuals.size());

            VisualArtifactDto visual = visuals.get(0);
            assertEquals("login_qr.png", visual.getFilename());
            assertEquals("image/png", visual.getContentType());
            assertNotNull(visual.getSha256Hash());
            assertTrue(visual.getFileSizeBytes() > 0);

            assertNotNull(visual.getQrArtifacts());
            assertEquals(1, visual.getQrArtifacts().size());
            assertEquals(testUrl, visual.getQrArtifacts().get(0).getDecodedPayload());
            assertEquals("URL", visual.getQrArtifacts().get(0).getPayloadType());
            assertEquals(testUrl, visual.getQrArtifacts().get(0).getNormalizedUrl());
        }
    }

    @Test
    void shouldHandleEmlWithNoImagesGracefully() throws Exception {
        String plainEml = "From: a@b.com\r\nSubject: Hi\r\n\r\nJust plain text.";
        try (InputStream in = new ByteArrayInputStream(plainEml.getBytes(StandardCharsets.UTF_8))) {
            List<VisualArtifactDto> visuals = visualAnalysisService.analyzeImagesFromStream(in);
            assertNotNull(visuals);
            assertTrue(visuals.isEmpty());
        }
    }

    @Test
    void shouldGenerateFixtureWithInlineQrCode() throws Exception {
        java.io.File fixtureFile = new java.io.File("../test-fixtures/eml/07-inline-qr-code.eml");
        if (!fixtureFile.exists()) {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode("https://verify-account.bank-security.test/login?challenge=83cb45a8", BarcodeFormat.QR_CODE, 250, 250);
            ByteArrayOutputStream imgBaos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", imgBaos);
            byte[] imgBytes = imgBaos.toByteArray();
            String base64Img = Base64.getMimeEncoder().encodeToString(imgBytes);

            String eml = "Received: from mail-relay.phish-domain.test (mail-relay.phish-domain.test [198.51.100.55]) by mx.emailforensics.local with ESMTP id 9988CC; Sat, 24 Oct 2026 12:00:00 +0000\r\n" +
                    "From: Security Team <security@it-support-notice.test>\r\n" +
                    "To: analyst@emailforensics.local\r\n" +
                    "Subject: Urgent MFA Device Re-authentication Required\r\n" +
                    "Date: Sat, 24 Oct 2026 12:00:00 +0000\r\n" +
                    "Message-ID: <qr-mfa-security-notice-20261024@phish-domain.test>\r\n" +
                    "MIME-Version: 1.0\r\n" +
                    "Content-Type: multipart/related; boundary=\"----=_Part_Boundary_Visual_001\"\r\n\r\n" +
                    "------=_Part_Boundary_Visual_001\r\n" +
                    "Content-Type: text/html; charset=UTF-8\r\n\r\n" +
                    "<html><body><h2>Urgent Multi-Factor Authentication Reset</h2><p>Please scan the QR code below using your mobile authenticator app to complete verification:</p><p><img src=\"cid:mfa_qr_code\" alt=\"MFA QR Code\" /></p><p>Thank you,<br/>IT Security</p></body></html>\r\n" +
                    "------=_Part_Boundary_Visual_001\r\n" +
                    "Content-Type: image/png\r\n" +
                    "Content-Disposition: inline; filename=\"mfa_qr_verify.png\"\r\n" +
                    "Content-ID: <mfa_qr_code>\r\n" +
                    "Content-Transfer-Encoding: base64\r\n\r\n" +
                    base64Img + "\r\n" +
                    "------=_Part_Boundary_Visual_001--\r\n";
            java.nio.file.Files.write(fixtureFile.toPath(), eml.getBytes(StandardCharsets.UTF_8));
        }
        assertTrue(fixtureFile.exists());
    }
}

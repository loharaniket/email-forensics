package com.emailforensics.backend.visual;

import com.emailforensics.backend.urlintel.UrlNormalizationService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QrDecodingServiceTest {

    private QrDecodingService qrDecodingService;

    @BeforeEach
    void setUp() {
        UrlNormalizationService urlNormalizationService = new UrlNormalizationService();
        qrDecodingService = new QrDecodingService(urlNormalizationService);
    }

    private byte[] createQrCodePng(String text, int width, int height) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
        return baos.toByteArray();
    }

    private byte[] createBlankImagePng(int width, int height) throws Exception {
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
    void shouldDecodeUrlQrCodeSuccessfully() throws Exception {
        String url = "https://secure-portal.bank-verification.test/auth?token=abc";
        byte[] qrBytes = createQrCodePng(url, 250, 250);

        List<QrDecodingService.DecodedQrPayload> results = qrDecodingService.decodeQrCodes(qrBytes);

        assertNotNull(results);
        assertEquals(1, results.size());
        QrDecodingService.DecodedQrPayload payload = results.get(0);
        assertEquals(url, payload.getPayload());
        assertEquals("URL", payload.getPayloadType());
        assertEquals("https://secure-portal.bank-verification.test/auth?token=abc", payload.getNormalizedUrl());
    }

    @Test
    void shouldDecodeTextQrCodeSuccessfully() throws Exception {
        String text = "Plain text emergency token 987654";
        byte[] qrBytes = createQrCodePng(text, 250, 250);

        List<QrDecodingService.DecodedQrPayload> results = qrDecodingService.decodeQrCodes(qrBytes);

        assertNotNull(results);
        assertEquals(1, results.size());
        QrDecodingService.DecodedQrPayload payload = results.get(0);
        assertEquals(text, payload.getPayload());
        assertEquals("TEXT", payload.getPayloadType());
    }

    @Test
    void shouldReturnEmptyListWhenImageContainsNoQrCode() throws Exception {
        byte[] blankBytes = createBlankImagePng(200, 200);

        List<QrDecodingService.DecodedQrPayload> results = qrDecodingService.decodeQrCodes(blankBytes);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldHandleCorruptImageBytesGracefully() {
        byte[] corrupt = "not_an_image_file_bytes".getBytes(StandardCharsets.UTF_8);

        List<QrDecodingService.DecodedQrPayload> results = qrDecodingService.decodeQrCodes(corrupt);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldHandleNullOrEmptyBytesGracefully() {
        List<QrDecodingService.DecodedQrPayload> r1 = qrDecodingService.decodeQrCodes(null);
        assertNotNull(r1);
        assertTrue(r1.isEmpty());

        List<QrDecodingService.DecodedQrPayload> r2 = qrDecodingService.decodeQrCodes(new byte[0]);
        assertNotNull(r2);
        assertTrue(r2.isEmpty());
    }
}

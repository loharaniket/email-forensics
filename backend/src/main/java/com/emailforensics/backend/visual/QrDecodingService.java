package com.emailforensics.backend.visual;

import com.emailforensics.backend.urlintel.UrlNormalizationService;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class QrDecodingService {

    private final UrlNormalizationService urlNormalizationService;

    public QrDecodingService(UrlNormalizationService urlNormalizationService) {
        this.urlNormalizationService = urlNormalizationService;
    }

    public static class DecodedQrPayload {
        private String payload;
        private String payloadType;
        private String normalizedUrl;

        public DecodedQrPayload(String payload, String payloadType, String normalizedUrl) {
            this.payload = payload;
            this.payloadType = payloadType;
            this.normalizedUrl = normalizedUrl;
        }

        public String getPayload() {
            return payload;
        }

        public String getPayloadType() {
            return payloadType;
        }

        public String getNormalizedUrl() {
            return normalizedUrl;
        }
    }

    public List<DecodedQrPayload> decodeQrCodes(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return Collections.emptyList();
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                return Collections.emptyList();
            }

            BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Set<String> payloads = new LinkedHashSet<>();

            try {
                QRCodeMultiReader multiReader = new QRCodeMultiReader();
                Result[] multiResults = multiReader.decodeMultiple(bitmap);
                if (multiResults != null) {
                    for (Result res : multiResults) {
                        if (res != null && res.getText() != null && !res.getText().trim().isEmpty()) {
                            payloads.add(res.getText().trim());
                        }
                    }
                }
            } catch (NotFoundException ignored) {
            } catch (Exception ignored) {
            }

            if (payloads.isEmpty()) {
                try {
                    MultiFormatReader singleReader = new MultiFormatReader();
                    Result singleResult = singleReader.decode(bitmap);
                    if (singleResult != null && singleResult.getText() != null && !singleResult.getText().trim().isEmpty()) {
                        payloads.add(singleResult.getText().trim());
                    }
                } catch (NotFoundException ignored) {
                } catch (Exception ignored) {
                }
            }

            List<DecodedQrPayload> decodedList = new ArrayList<>();
            for (String raw : payloads) {
                String type = detectPayloadType(raw);
                String normalizedUrl = null;
                if ("URL".equals(type)) {
                    normalizedUrl = urlNormalizationService.normalize(raw).getNormalizedUrl();
                }
                decodedList.add(new DecodedQrPayload(raw, type, normalizedUrl));
            }

            return decodedList;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String detectPayloadType(String text) {
        if (text == null) return "UNKNOWN";
        String s = text.trim().toLowerCase();
        if (s.startsWith("http://") || s.startsWith("https://") || s.startsWith("hxxp://") || s.startsWith("hxxps://") || s.startsWith("www.")) {
            return "URL";
        }
        if (s.startsWith("mailto:")) {
            return "EMAIL";
        }
        if (s.startsWith("wifi:")) {
            return "WIFI";
        }
        return "TEXT";
    }
}

package com.emailforensics.backend.visual;

import com.emailforensics.backend.dto.visual.QrArtifactDto;
import com.emailforensics.backend.dto.visual.VisualArtifactDto;
import com.emailforensics.backend.evidence.AnalysisRun;
import com.emailforensics.backend.evidence.AnalysisRunRepository;
import com.emailforensics.backend.evidence.EmailEvidence;
import com.emailforensics.backend.evidence.EmailEvidenceRepository;
import com.emailforensics.backend.evidence.EvidenceStorageService;
import com.emailforensics.backend.infrastructure.DnsLookupService;
import com.emailforensics.backend.urlintel.ExtractedUrl;
import com.emailforensics.backend.urlintel.ExtractedUrlRepository;
import com.emailforensics.backend.urlintel.UrlHeuristicsService;
import com.emailforensics.backend.urlintel.UrlNormalizationService;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

@Service
public class VisualAnalysisService {

    private final VisualArtifactRepository visualArtifactRepository;
    private final QrArtifactRepository qrArtifactRepository;
    private final EmailEvidenceRepository evidenceRepository;
    private final AnalysisRunRepository analysisRunRepository;
    private final EvidenceStorageService storageService;
    private final QrDecodingService qrDecodingService;
    private final OcrService ocrService;
    private final UrlNormalizationService urlNormalizationService;
    private final UrlHeuristicsService urlHeuristicsService;
    private final DnsLookupService dnsLookupService;
    private final ExtractedUrlRepository extractedUrlRepository;

    public VisualAnalysisService(VisualArtifactRepository visualArtifactRepository,
                                 QrArtifactRepository qrArtifactRepository,
                                 EmailEvidenceRepository evidenceRepository,
                                 AnalysisRunRepository analysisRunRepository,
                                 EvidenceStorageService storageService,
                                 QrDecodingService qrDecodingService,
                                 OcrService ocrService,
                                 UrlNormalizationService urlNormalizationService,
                                 UrlHeuristicsService urlHeuristicsService,
                                 DnsLookupService dnsLookupService,
                                 ExtractedUrlRepository extractedUrlRepository) {
        this.visualArtifactRepository = visualArtifactRepository;
        this.qrArtifactRepository = qrArtifactRepository;
        this.evidenceRepository = evidenceRepository;
        this.analysisRunRepository = analysisRunRepository;
        this.storageService = storageService;
        this.qrDecodingService = qrDecodingService;
        this.ocrService = ocrService;
        this.urlNormalizationService = urlNormalizationService;
        this.urlHeuristicsService = urlHeuristicsService;
        this.dnsLookupService = dnsLookupService;
        this.extractedUrlRepository = extractedUrlRepository;
    }

    public static class ExtractedImage {
        private String filename;
        private String contentType;
        private byte[] data;
        private String sha256Hash;

        public ExtractedImage(String filename, String contentType, byte[] data, String sha256Hash) {
            this.filename = filename;
            this.contentType = contentType;
            this.data = data;
            this.sha256Hash = sha256Hash;
        }

        public String getFilename() {
            return filename;
        }

        public String getContentType() {
            return contentType;
        }

        public byte[] getData() {
            return data;
        }

        public String getSha256Hash() {
            return sha256Hash;
        }
    }

    @Transactional
    public List<VisualArtifactDto> analyzeAndSaveVisuals(UUID emailId) {
        EmailEvidence evidence = evidenceRepository.findById(emailId)
                .orElseThrow(() -> new IllegalArgumentException("Evidence not found: " + emailId));

        AnalysisRun run = analysisRunRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)
                .orElseThrow(() -> new IllegalArgumentException("Analysis run not found for evidence: " + emailId));

        List<VisualArtifact> existing = visualArtifactRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run.getId());
        if (!existing.isEmpty()) {
            return toDtoList(existing);
        }

        List<ExtractedImage> images;
        try (InputStream in = storageService.loadAsResource(evidence.getStorageKey()).getInputStream()) {
            images = extractImages(in);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load evidence for visual analysis", e);
        }

        List<VisualArtifact> savedList = new ArrayList<>();

        for (ExtractedImage img : images) {
            VisualArtifact visual = new VisualArtifact();
            visual.setId(UUID.randomUUID());
            visual.setAnalysisRun(run);
            visual.setFilename(img.getFilename());
            visual.setContentType(img.getContentType());
            visual.setFileSizeBytes((long) img.getData().length);
            visual.setSha256Hash(img.getSha256Hash());

            List<QrDecodingService.DecodedQrPayload> qrs = qrDecodingService.decodeQrCodes(img.getData());
            List<QrArtifact> qrEntities = new ArrayList<>();
            for (QrDecodingService.DecodedQrPayload qr : qrs) {
                QrArtifact qrArtifact = new QrArtifact(
                        UUID.randomUUID(),
                        visual,
                        qr.getPayload(),
                        qr.getPayloadType(),
                        qr.getNormalizedUrl()
                );
                qrEntities.add(qrArtifact);

                if ("URL".equals(qr.getPayloadType()) && qr.getNormalizedUrl() != null) {
                    routeQrUrl(run, qr.getPayload(), qr.getNormalizedUrl());
                }
            }
            visual.setQrArtifacts(qrEntities);

            OcrService.OcrResult ocrResult = ocrService.processImage(img.getData(), img.getSha256Hash());
            visual.setOcrText(ocrResult.getOcrText());
            visual.setOcrStatus(ocrResult.getOcrStatus());
            visual.setProcessingError(ocrResult.getProcessingError());
            visual.setDataMode(ocrResult.getDataMode());

            visualArtifactRepository.save(visual);
            savedList.add(visual);
        }

        return toDtoList(savedList);
    }

    @Transactional
    public List<VisualArtifactDto> getVisualArtifacts(UUID emailId) {
        EmailEvidence evidence = evidenceRepository.findById(emailId)
                .orElseThrow(() -> new IllegalArgumentException("Evidence not found: " + emailId));

        AnalysisRun run = analysisRunRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)
                .orElseThrow(() -> new IllegalArgumentException("Analysis run not found for evidence: " + emailId));

        List<VisualArtifact> list = visualArtifactRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run.getId());
        if (list.isEmpty()) {
            return analyzeAndSaveVisuals(emailId);
        }

        return toDtoList(list);
    }

    public List<VisualArtifactDto> analyzeImagesFromStream(InputStream in) {
        List<ExtractedImage> images = extractImages(in);
        List<VisualArtifactDto> dtos = new ArrayList<>();

        for (ExtractedImage img : images) {
            List<QrDecodingService.DecodedQrPayload> qrs = qrDecodingService.decodeQrCodes(img.getData());
            List<QrArtifactDto> qrDtos = new ArrayList<>();
            for (QrDecodingService.DecodedQrPayload qr : qrs) {
                qrDtos.add(new QrArtifactDto(
                        null,
                        qr.getPayload(),
                        qr.getPayloadType(),
                        qr.getNormalizedUrl(),
                        null
                ));
            }

            OcrService.OcrResult ocrResult = ocrService.processImage(img.getData(), img.getSha256Hash());
            dtos.add(new VisualArtifactDto(
                    null,
                    img.getFilename(),
                    img.getContentType(),
                    (long) img.getData().length,
                    img.getSha256Hash(),
                    ocrResult.getOcrText(),
                    ocrResult.getOcrStatus(),
                    ocrResult.getProcessingError(),
                    null,
                    qrDtos,
                    ocrResult.getDataMode()
            ));
        }

        return dtos;
    }

    public List<ExtractedImage> extractImages(InputStream inputStream) {
        List<ExtractedImage> images = new ArrayList<>();
        try {
            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);
            MimeMessage message = new MimeMessage(session, inputStream);
            collectImages(message, images, 0);
        } catch (Exception ignored) {
        }
        return images;
    }

    private void collectImages(Part part, List<ExtractedImage> images, int depth) {
        if (depth > 15 || part == null) return;
        try {
            if (part.isMimeType("multipart/*")) {
                Object content = part.getContent();
                if (content instanceof Multipart multipart) {
                    int count = multipart.getCount();
                    for (int i = 0; i < count; i++) {
                        collectImages(multipart.getBodyPart(i), images, depth + 1);
                    }
                }
            } else if (part.isMimeType("message/rfc822")) {
                Object content = part.getContent();
                if (content instanceof Part nested) {
                    collectImages(nested, images, depth + 1);
                }
            } else {
                String contentType = part.getContentType();
                String cleanContentType = "application/octet-stream";
                if (contentType != null) {
                    int semi = contentType.indexOf(';');
                    cleanContentType = (semi >= 0 ? contentType.substring(0, semi) : contentType).trim().toLowerCase();
                }

                String filename = part.getFileName();
                if (filename != null) {
                    try {
                        filename = MimeUtility.decodeText(filename);
                    } catch (Exception ignored) {
                    }
                }

                boolean isImage = cleanContentType.startsWith("image/") || isImageFilename(filename);
                if (isImage) {
                    byte[] bytes = readBytes(part);
                    if (bytes != null && bytes.length > 0) {
                        String name = (filename != null && !filename.trim().isEmpty()) ? filename.trim() : "inline_image_" + (images.size() + 1) + ".png";
                        String sha256 = calculateHash(bytes);
                        images.add(new ExtractedImage(name, cleanContentType, bytes, sha256));
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private boolean isImageFilename(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".bmp")
                || lower.endsWith(".tiff") || lower.endsWith(".ico");
    }

    private byte[] readBytes(Part part) {
        try (InputStream is = part.getInputStream();
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[4096];
            int n;
            while ((n = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, n);
            }
            return buffer.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private String calculateHash(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    private void routeQrUrl(AnalysisRun run, String rawUrl, String normalizedUrl) {
        try {
            List<ExtractedUrl> existing = extractedUrlRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run.getId());
            for (ExtractedUrl u : existing) {
                if (normalizedUrl.equalsIgnoreCase(u.getNormalizedUrl())) {
                    return;
                }
            }

            UrlNormalizationService.NormalizedUrlResult norm = urlNormalizationService.normalize(rawUrl);
            boolean isIp = urlHeuristicsService.isIpUrl(norm.getDomain());
            boolean unusualPort = urlHeuristicsService.isUnusualPort(norm.getPort());
            boolean isShortened = urlHeuristicsService.isShortened(norm.getDomain());
            boolean subdomainExcess = urlHeuristicsService.isSubdomainExcess(norm.getDomain());
            boolean lookalike = urlHeuristicsService.isLookalike(norm.getDomain());
            int riskScore = urlHeuristicsService.calculateRiskScore(false, isIp, lookalike, unusualPort, subdomainExcess, isShortened);
            String dnsStatus = isIp ? "SKIPPED_IP" : dnsLookupService.lookupDomain(norm.getDomain()).getStatus();

            ExtractedUrl entity = new ExtractedUrl(
                    UUID.randomUUID(),
                    run,
                    rawUrl,
                    norm.getNormalizedUrl(),
                    norm.getDomain(),
                    "QR_CODE",
                    null,
                    false,
                    isIp,
                    isShortened,
                    unusualPort,
                    subdomainExcess,
                    lookalike,
                    dnsStatus,
                    riskScore
            );
            extractedUrlRepository.save(entity);
        } catch (Exception ignored) {
        }
    }

    private List<VisualArtifactDto> toDtoList(List<VisualArtifact> entities) {
        List<VisualArtifactDto> dtos = new ArrayList<>();
        for (VisualArtifact v : entities) {
            List<QrArtifactDto> qrDtos = new ArrayList<>();
            if (v.getQrArtifacts() != null) {
                for (QrArtifact q : v.getQrArtifacts()) {
                    qrDtos.add(new QrArtifactDto(
                            q.getId(),
                            q.getDecodedPayload(),
                            q.getPayloadType(),
                            q.getNormalizedUrl(),
                            q.getCreatedAt()
                    ));
                }
            }
            dtos.add(new VisualArtifactDto(
                    v.getId(),
                    v.getFilename(),
                    v.getContentType(),
                    v.getFileSizeBytes(),
                    v.getSha256Hash(),
                    v.getOcrText(),
                    v.getOcrStatus(),
                    v.getProcessingError(),
                    v.getCreatedAt(),
                    qrDtos,
                    v.getDataMode()
            ));
        }
        return dtos;
    }
}

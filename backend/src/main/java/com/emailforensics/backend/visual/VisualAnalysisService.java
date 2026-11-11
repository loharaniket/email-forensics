package com.emailforensics.backend.visual;

import com.emailforensics.backend.common.ResourceNotFoundException;
import com.emailforensics.backend.dto.visual.QrArtifactDto;
import com.emailforensics.backend.dto.visual.VisualArtifactDto;
import com.emailforensics.backend.evidence.AnalysisRun;
import com.emailforensics.backend.evidence.AnalysisRunRepository;
import com.emailforensics.backend.evidence.EmailEvidence;
import com.emailforensics.backend.evidence.EmailEvidenceRepository;
import com.emailforensics.backend.evidence.EvidenceStorageService;
import com.emailforensics.backend.infrastructure.DataProvenance;
import com.emailforensics.backend.infrastructure.DnsLookupService;
import com.emailforensics.backend.urlintel.ExtractedUrl;
import com.emailforensics.backend.urlintel.ExtractedUrlRepository;
import com.emailforensics.backend.urlintel.UrlCheckService;
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
import java.util.Collections;
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
    private final UrlNormalizationService urlNormalizationService;
    private final UrlCheckService urlCheckService;
    private final DnsLookupService dnsLookupService;
    private final ExtractedUrlRepository extractedUrlRepository;

    public VisualAnalysisService(VisualArtifactRepository visualArtifactRepository,
                                 QrArtifactRepository qrArtifactRepository,
                                 EmailEvidenceRepository evidenceRepository,
                                 AnalysisRunRepository analysisRunRepository,
                                 EvidenceStorageService storageService,
                                 QrDecodingService qrDecodingService,
                                 UrlNormalizationService urlNormalizationService,
                                 UrlCheckService urlCheckService,
                                 DnsLookupService dnsLookupService,
                                 ExtractedUrlRepository extractedUrlRepository) {
        this.visualArtifactRepository = visualArtifactRepository;
        this.qrArtifactRepository = qrArtifactRepository;
        this.evidenceRepository = evidenceRepository;
        this.analysisRunRepository = analysisRunRepository;
        this.storageService = storageService;
        this.qrDecodingService = qrDecodingService;
        this.urlNormalizationService = urlNormalizationService;
        this.urlCheckService = urlCheckService;
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
            return toDtoList(emailId, existing);
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
            visual.setDataMode(DataProvenance.REAL.name());

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

            visualArtifactRepository.save(visual);
            storageService.storeImageFile(visual.getId().toString(), img.getData());
            savedList.add(visual);
        }

        return toDtoList(emailId, savedList);
    }

    @Transactional(readOnly = true)
    public List<VisualArtifactDto> getVisualArtifacts(UUID emailId) {
        AnalysisRun run = analysisRunRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)
                .orElse(null);
        if (run == null) {
            return Collections.emptyList();
        }

        List<VisualArtifact> list = visualArtifactRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run.getId());
        return toDtoList(emailId, list);
    }

    @Transactional(readOnly = true)
    public VisualArtifact getVisualArtifact(UUID emailId, UUID imageId) {
        VisualArtifact visual = visualArtifactRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image artifact not found: " + imageId));

        if (visual.getAnalysisRun() == null || visual.getAnalysisRun().getEmailEvidence() == null
                || !visual.getAnalysisRun().getEmailEvidence().getId().equals(emailId)) {
            throw new ResourceNotFoundException("Image artifact does not belong to email: " + emailId);
        }
        return visual;
    }

    @Transactional(readOnly = true)
    public byte[] getImageData(UUID emailId, UUID imageId) {
        VisualArtifact visual = getVisualArtifact(emailId, imageId);

        byte[] cached = storageService.loadImageFile(visual.getId().toString());
        if (cached != null && cached.length > 0) {
            return cached;
        }

        EmailEvidence evidence = evidenceRepository.findById(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence not found: " + emailId));

        try (InputStream in = storageService.loadAsResource(evidence.getStorageKey()).getInputStream()) {
            List<ExtractedImage> images = extractImages(in);
            for (ExtractedImage img : images) {
                if (visual.getSha256Hash().equalsIgnoreCase(img.getSha256Hash())
                        || (visual.getFilename() != null && visual.getFilename().equalsIgnoreCase(img.getFilename()))) {
                    storageService.storeImageFile(visual.getId().toString(), img.getData());
                    return img.getData();
                }
            }
        } catch (ResourceNotFoundException rne) {
            throw rne;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read image data from email evidence", e);
        }

        throw new ResourceNotFoundException("Image content not found for artifact: " + imageId);
    }

    @Transactional(readOnly = true)
    public List<QrArtifactDto> getQrArtifacts(UUID emailId) {
        AnalysisRun run = analysisRunRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)
                .orElse(null);
        if (run == null) {
            return Collections.emptyList();
        }

        List<VisualArtifact> visuals = visualArtifactRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run.getId());
        List<QrArtifactDto> qrDtos = new ArrayList<>();
        for (VisualArtifact v : visuals) {
            if (v.getQrArtifacts() != null) {
                for (QrArtifact q : v.getQrArtifacts()) {
                    qrDtos.add(new QrArtifactDto(
                            q.getId(),
                            v.getId(),
                            v.getFilename(),
                            q.getDecodedPayload(),
                            q.getPayloadType(),
                            q.getNormalizedUrl(),
                            q.getCreatedAt()
                    ));
                }
            }
        }
        return qrDtos;
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

            dtos.add(new VisualArtifactDto(
                    null,
                    img.getFilename(),
                    img.getContentType(),
                    (long) img.getData().length,
                    img.getSha256Hash(),
                    null,
                    null,
                    qrDtos,
                    DataProvenance.REAL.name()
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
            boolean isIp = urlCheckService.isIpUrl(norm.getDomain());
            boolean unusualPort = urlCheckService.isUnusualPort(norm.getPort());
            boolean isShortened = urlCheckService.isShortened(norm.getDomain());
            boolean subdomainExcess = urlCheckService.isSubdomainExcess(norm.getDomain());
            boolean lookalike = urlCheckService.isLookalike(norm.getDomain());
            int riskScore = urlCheckService.calculateRiskScore(false, isIp, lookalike, unusualPort, subdomainExcess, isShortened);
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
        UUID emailId = null;
        if (entities != null && !entities.isEmpty() && entities.get(0).getAnalysisRun() != null
                && entities.get(0).getAnalysisRun().getEmailEvidence() != null) {
            emailId = entities.get(0).getAnalysisRun().getEmailEvidence().getId();
        }
        return toDtoList(emailId, entities);
    }

    private List<VisualArtifactDto> toDtoList(UUID emailId, List<VisualArtifact> entities) {
        List<VisualArtifactDto> dtos = new ArrayList<>();
        if (entities == null) {
            return dtos;
        }
        for (VisualArtifact v : entities) {
            List<QrArtifactDto> qrDtos = new ArrayList<>();
            if (v.getQrArtifacts() != null) {
                for (QrArtifact q : v.getQrArtifacts()) {
                    qrDtos.add(new QrArtifactDto(
                            q.getId(),
                            v.getId(),
                            v.getFilename(),
                            q.getDecodedPayload(),
                            q.getPayloadType(),
                            q.getNormalizedUrl(),
                            q.getCreatedAt()
                    ));
                }
            }
            VisualArtifactDto dto = new VisualArtifactDto(
                    v.getId(),
                    v.getFilename(),
                    v.getContentType(),
                    v.getFileSizeBytes(),
                    v.getSha256Hash(),
                    v.getProcessingError(),
                    v.getCreatedAt(),
                    qrDtos,
                    v.getDataMode()
            );
            if (emailId != null) {
                dto.setImageUrl("/api/v1/evidence/" + emailId + "/images/" + v.getId() + "/content");
            }
            dtos.add(dto);
        }
        return dtos;
    }

}

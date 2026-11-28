package com.emailforensics.backend.urlintel;

import com.emailforensics.backend.dto.email.ParsedEmailDto;
import com.emailforensics.backend.dto.urlintel.ExtractedUrlDto;
import com.emailforensics.backend.email.EmailParserService;
import com.emailforensics.backend.evidence.AnalysisRun;
import com.emailforensics.backend.evidence.AnalysisRunRepository;
import com.emailforensics.backend.evidence.EmailEvidence;
import com.emailforensics.backend.evidence.EmailEvidenceRepository;
import com.emailforensics.backend.evidence.EvidenceStorageService;
import com.emailforensics.backend.infrastructure.DnsLookupService;
import com.emailforensics.backend.infrastructure.DnsResult;
import com.emailforensics.backend.infrastructure.IpArtifactService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class UrlAnalysisService {

    private final ExtractedUrlRepository extractedUrlRepository;
    private final EmailEvidenceRepository evidenceRepository;
    private final AnalysisRunRepository analysisRunRepository;
    private final EvidenceStorageService storageService;
    private final EmailParserService emailParserService;
    private final UrlExtractionService urlExtractionService;
    private final UrlNormalizationService urlNormalizationService;
    private final UrlCheckService urlCheckService;
    private final DnsLookupService dnsLookupService;
    private final IpArtifactService ipArtifactService;

    public UrlAnalysisService(ExtractedUrlRepository extractedUrlRepository,
                           EmailEvidenceRepository evidenceRepository,
                           AnalysisRunRepository analysisRunRepository,
                           EvidenceStorageService storageService,
                           EmailParserService emailParserService,
                           UrlExtractionService urlExtractionService,
                           UrlNormalizationService urlNormalizationService,
                           UrlCheckService urlCheckService,
                           DnsLookupService dnsLookupService,
                           IpArtifactService ipArtifactService) {
        this.extractedUrlRepository = extractedUrlRepository;
        this.evidenceRepository = evidenceRepository;
        this.analysisRunRepository = analysisRunRepository;
        this.storageService = storageService;
        this.emailParserService = emailParserService;
        this.urlExtractionService = urlExtractionService;
        this.urlNormalizationService = urlNormalizationService;
        this.urlCheckService = urlCheckService;
        this.dnsLookupService = dnsLookupService;
        this.ipArtifactService = ipArtifactService;
    }

    @Transactional
    public List<ExtractedUrlDto> analyzeAndSaveUrls(UUID emailId) {
        EmailEvidence evidence = evidenceRepository.findById(emailId)
                .orElseThrow(() -> new IllegalArgumentException("Evidence not found: " + emailId));

        AnalysisRun run = analysisRunRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)
                .orElseThrow(() -> new IllegalArgumentException("Analysis run not found for evidence: " + emailId));

        List<ExtractedUrl> existing = extractedUrlRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run.getId());
        if (!existing.isEmpty()) {
            return toDtoList(existing);
        }

        ParsedEmailDto parsed;
        try (InputStream in = storageService.loadAsResource(evidence.getStorageKey()).getInputStream()) {
            parsed = emailParserService.parse(in);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse evidence for URL extraction", e);
        }

        List<UrlExtractionService.RawExtractedUrl> rawUrls = urlExtractionService.extractUrls(
                parsed.getPlainTextBody(), parsed.getHtmlBody()
        );

        List<ExtractedUrl> entitiesToSave = new ArrayList<>();
        List<String> discoveredIps = new ArrayList<>();
        Set<String> seenNormalizedKeys = new HashSet<>();

        for (UrlExtractionService.RawExtractedUrl raw : rawUrls) {
            UrlNormalizationService.NormalizedUrlResult norm = urlNormalizationService.normalize(raw.getRawUrl());
            String dedupeKey = norm.getNormalizedUrl() + "||" + raw.getSourceType() + "||" + (raw.getAnchorText() != null ? raw.getAnchorText() : "");
            if (!seenNormalizedKeys.add(dedupeKey)) {
                continue;
            }

            boolean isIp = urlCheckService.isIpUrl(norm.getDomain());
            boolean unusualPort = urlCheckService.isUnusualPort(norm.getPort());
            boolean isShortened = urlCheckService.isShortened(norm.getDomain());
            boolean subdomainExcess = false;
            boolean lookalike = false;
            boolean anchorMismatch = urlCheckService.isAnchorMismatch(norm.getDomain(), raw.getAnchorText());
            int riskScore = urlCheckService.calculateRiskScore(anchorMismatch, isIp, lookalike, unusualPort, subdomainExcess, isShortened);

            UrlCheckResult externalResult = urlCheckService.checkWithExternalServices(raw.getRawUrl(), norm.getDomain());
            if (externalResult != null && externalResult.isFlagged()) {
                riskScore = Math.max(riskScore, externalResult.getRiskScore());
            }
            if ((isIp || isShortened || unusualPort || anchorMismatch || (externalResult != null && externalResult.isFlagged())) && riskScore < 40) {
                riskScore = 40;
            }

            String dnsStatus;
            String dataMode;
            if (isIp) {
                dnsStatus = "SKIPPED_IP";
                dataMode = "NOT_CHECKED";
                discoveredIps.add(norm.getDomain());
            } else {
                DnsResult dnsResult = dnsLookupService.lookupDomain(norm.getDomain());
                dnsStatus = dnsResult.getStatus();
                dataMode = dnsResult.getDataMode();
                discoveredIps.addAll(dnsResult.getARecords());
            }

            ExtractedUrl entity = new ExtractedUrl(
                    UUID.randomUUID(),
                    run,
                    raw.getRawUrl(),
                    norm.getNormalizedUrl(),
                    norm.getDomain(),
                    raw.getSourceType(),
                    raw.getAnchorText(),
                    anchorMismatch,
                    isIp,
                    isShortened,
                    unusualPort,
                    subdomainExcess,
                    lookalike,
                    dnsStatus,
                    riskScore,
                    dataMode
            );
            String sourceApi = null;
            if (externalResult != null && externalResult.isFlagged()) {
                entity.setThreatType(externalResult.getThreatType());
                entity.setThreatDetails(externalResult.getSummary());
                sourceApi = externalResult.getServiceName();
            } else if (isIp) {
                sourceApi = "IP Address Inspector";
            } else if (anchorMismatch) {
                sourceApi = "Anchor Mismatch Detector";
            } else if (isShortened) {
                sourceApi = "URL Shortener Detector";
            } else if (unusualPort) {
                sourceApi = "Port Inspector";
            }
            entity.setSourceApi(sourceApi);
            entitiesToSave.add(entity);
        }

        if (!entitiesToSave.isEmpty()) {
            extractedUrlRepository.saveAll(entitiesToSave);
            extractedUrlRepository.flush();
        }

        ipArtifactService.analyzeAndSaveInfrastructure(emailId, discoveredIps);

        return toDtoList(entitiesToSave);
    }

    @Transactional(readOnly = true)
    public List<ExtractedUrlDto> getExtractedUrls(UUID emailId) {
        AnalysisRun run = analysisRunRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)
                .orElse(null);
        if (run == null) {
            return Collections.emptyList();
        }

        List<ExtractedUrl> list = extractedUrlRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run.getId());
        return toDtoList(list);
    }

    public List<ExtractedUrlDto> extractAndAnalyzeFromStream(InputStream in) {
        ParsedEmailDto parsed = emailParserService.parse(in);
        List<UrlExtractionService.RawExtractedUrl> rawUrls = urlExtractionService.extractUrls(
                parsed.getPlainTextBody(), parsed.getHtmlBody()
        );

        List<ExtractedUrlDto> dtos = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (UrlExtractionService.RawExtractedUrl raw : rawUrls) {
            UrlNormalizationService.NormalizedUrlResult norm = urlNormalizationService.normalize(raw.getRawUrl());
            String key = norm.getNormalizedUrl() + "||" + raw.getSourceType() + "||" + (raw.getAnchorText() != null ? raw.getAnchorText() : "");
            if (!seen.add(key)) {
                continue;
            }

            boolean isIp = urlCheckService.isIpUrl(norm.getDomain());
            boolean unusualPort = urlCheckService.isUnusualPort(norm.getPort());
            boolean isShortened = urlCheckService.isShortened(norm.getDomain());
            boolean subdomainExcess = false;
            boolean lookalike = false;
            boolean anchorMismatch = urlCheckService.isAnchorMismatch(norm.getDomain(), raw.getAnchorText());
            int riskScore = urlCheckService.calculateRiskScore(anchorMismatch, isIp, lookalike, unusualPort, subdomainExcess, isShortened);

            UrlCheckResult externalResult = urlCheckService.checkWithExternalServices(raw.getRawUrl(), norm.getDomain());
            if (externalResult != null && externalResult.isFlagged()) {
                riskScore = Math.max(riskScore, externalResult.getRiskScore());
            }
            if ((isIp || isShortened || unusualPort || anchorMismatch || (externalResult != null && externalResult.isFlagged())) && riskScore < 40) {
                riskScore = 40;
            }

            String dnsStatus;
            String dataMode;
            if (isIp) {
                dnsStatus = "SKIPPED_IP";
                dataMode = "NOT_CHECKED";
            } else {
                DnsResult dnsResult = dnsLookupService.lookupDomain(norm.getDomain());
                dnsStatus = dnsResult.getStatus();
                dataMode = dnsResult.getDataMode();
            }

            ExtractedUrlDto dto = new ExtractedUrlDto(
                    null,
                    raw.getRawUrl(),
                    norm.getNormalizedUrl(),
                    norm.getDomain(),
                    raw.getSourceType(),
                    raw.getAnchorText(),
                    anchorMismatch,
                    isIp,
                    isShortened,
                    unusualPort,
                    subdomainExcess,
                    lookalike,
                    dnsStatus,
                    riskScore,
                    dataMode
            );
            String sourceApi = null;
            if (externalResult != null && externalResult.isFlagged()) {
                dto.setThreatType(externalResult.getThreatType());
                dto.setThreatDetails(externalResult.getSummary());
                sourceApi = externalResult.getServiceName();
            } else if (isIp) {
                sourceApi = "IP Address Inspector";
            } else if (anchorMismatch) {
                sourceApi = "Anchor Mismatch Detector";
            } else if (isShortened) {
                sourceApi = "URL Shortener Detector";
            } else if (unusualPort) {
                sourceApi = "Port Inspector";
            }
            dto.setSourceApi(sourceApi);
            dtos.add(dto);
        }

        return dtos;
    }

    private List<ExtractedUrlDto> toDtoList(List<ExtractedUrl> entities) {
        List<ExtractedUrlDto> dtos = new ArrayList<>();
        for (ExtractedUrl e : entities) {
            ExtractedUrlDto dto = new ExtractedUrlDto(
                    e.getId(),
                    e.getRawUrl(),
                    e.getNormalizedUrl(),
                    e.getDomain(),
                    e.getSourceType(),
                    e.getAnchorText(),
                    e.getAnchorMismatch(),
                    e.getIsIpUrl(),
                    e.getIsShortened(),
                    e.getUnusualPort(),
                    e.getSubdomainExcess(),
                    e.getLookalike(),
                    e.getDnsStatus(),
                    e.getUrlRiskScore(),
                    e.getDataMode()
            );
            String threatType = e.getThreatType();
            String threatDetails = e.getThreatDetails();
            String sourceApi = e.getSourceApi();
            if (sourceApi == null && e.getUrlRiskScore() != null && e.getUrlRiskScore() > 0) {
                if (Boolean.TRUE.equals(e.getIsIpUrl())) {
                    sourceApi = "IP Address Inspector";
                } else if (Boolean.TRUE.equals(e.getAnchorMismatch())) {
                    sourceApi = "Anchor Mismatch Detector";
                } else if (Boolean.TRUE.equals(e.getIsShortened())) {
                    sourceApi = "URL Shortener Detector";
                } else if (Boolean.TRUE.equals(e.getUnusualPort())) {
                    sourceApi = "Port Inspector";
                } else {
                    UrlCheckResult ext = urlCheckService.checkWithExternalServices(e.getRawUrl(), e.getDomain());
                    if (ext != null && ext.isFlagged()) {
                        sourceApi = ext.getServiceName();
                        if (threatType == null) threatType = ext.getThreatType();
                        if (threatDetails == null) threatDetails = ext.getSummary();
                    }
                }
            }
            dto.setThreatType(threatType);
            dto.setThreatDetails(threatDetails);
            dto.setSourceApi(sourceApi);
            dtos.add(dto);
        }
        return dtos;
    }
}

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
            boolean subdomainExcess = urlCheckService.isSubdomainExcess(norm.getDomain());
            boolean lookalike = urlCheckService.isLookalike(norm.getDomain());
            boolean anchorMismatch = urlCheckService.isAnchorMismatch(norm.getDomain(), raw.getAnchorText());
            int riskScore = urlCheckService.calculateRiskScore(anchorMismatch, isIp, lookalike, unusualPort, subdomainExcess, isShortened);

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
            boolean subdomainExcess = urlCheckService.isSubdomainExcess(norm.getDomain());
            boolean lookalike = urlCheckService.isLookalike(norm.getDomain());
            boolean anchorMismatch = urlCheckService.isAnchorMismatch(norm.getDomain(), raw.getAnchorText());
            int riskScore = urlCheckService.calculateRiskScore(anchorMismatch, isIp, lookalike, unusualPort, subdomainExcess, isShortened);

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

            dtos.add(new ExtractedUrlDto(
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
            ));
        }

        return dtos;
    }

    private List<ExtractedUrlDto> toDtoList(List<ExtractedUrl> entities) {
        List<ExtractedUrlDto> dtos = new ArrayList<>();
        for (ExtractedUrl e : entities) {
            dtos.add(new ExtractedUrlDto(
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
            ));
        }
        return dtos;
    }
}

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
import com.emailforensics.backend.infrastructure.InfrastructureService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class UrlIntelService {

    private final ExtractedUrlRepository extractedUrlRepository;
    private final EmailEvidenceRepository evidenceRepository;
    private final AnalysisRunRepository analysisRunRepository;
    private final EvidenceStorageService storageService;
    private final EmailParserService emailParserService;
    private final UrlExtractionService urlExtractionService;
    private final UrlNormalizationService urlNormalizationService;
    private final UrlHeuristicsService urlHeuristicsService;
    private final DnsLookupService dnsLookupService;
    private final InfrastructureService infrastructureService;

    public UrlIntelService(ExtractedUrlRepository extractedUrlRepository,
                           EmailEvidenceRepository evidenceRepository,
                           AnalysisRunRepository analysisRunRepository,
                           EvidenceStorageService storageService,
                           EmailParserService emailParserService,
                           UrlExtractionService urlExtractionService,
                           UrlNormalizationService urlNormalizationService,
                           UrlHeuristicsService urlHeuristicsService,
                           DnsLookupService dnsLookupService,
                           InfrastructureService infrastructureService) {
        this.extractedUrlRepository = extractedUrlRepository;
        this.evidenceRepository = evidenceRepository;
        this.analysisRunRepository = analysisRunRepository;
        this.storageService = storageService;
        this.emailParserService = emailParserService;
        this.urlExtractionService = urlExtractionService;
        this.urlNormalizationService = urlNormalizationService;
        this.urlHeuristicsService = urlHeuristicsService;
        this.dnsLookupService = dnsLookupService;
        this.infrastructureService = infrastructureService;
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

            boolean isIp = urlHeuristicsService.isIpUrl(norm.getDomain());
            boolean unusualPort = urlHeuristicsService.isUnusualPort(norm.getPort());
            boolean isShortened = urlHeuristicsService.isShortened(norm.getDomain());
            boolean subdomainExcess = urlHeuristicsService.isSubdomainExcess(norm.getDomain());
            boolean lookalike = urlHeuristicsService.isLookalike(norm.getDomain());
            boolean anchorMismatch = urlHeuristicsService.isAnchorMismatch(norm.getDomain(), raw.getAnchorText());
            int riskScore = urlHeuristicsService.calculateRiskScore(anchorMismatch, isIp, lookalike, unusualPort, subdomainExcess, isShortened);

            String dnsStatus;
            if (isIp) {
                dnsStatus = "SKIPPED_IP";
                discoveredIps.add(norm.getDomain());
            } else {
                DnsResult dnsResult = dnsLookupService.lookupDomain(norm.getDomain());
                dnsStatus = dnsResult.getStatus();
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
                    riskScore
            );
            entitiesToSave.add(entity);
        }

        if (!entitiesToSave.isEmpty()) {
            extractedUrlRepository.saveAll(entitiesToSave);
            extractedUrlRepository.flush();
        }

        infrastructureService.analyzeAndSaveInfrastructure(emailId, discoveredIps);

        return toDtoList(entitiesToSave);
    }

    @Transactional
    public List<ExtractedUrlDto> getExtractedUrls(UUID emailId) {
        EmailEvidence evidence = evidenceRepository.findById(emailId)
                .orElseThrow(() -> new IllegalArgumentException("Evidence not found: " + emailId));

        AnalysisRun run = analysisRunRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)
                .orElseThrow(() -> new IllegalArgumentException("Analysis run not found for evidence: " + emailId));

        List<ExtractedUrl> list = extractedUrlRepository.findByAnalysisRunIdOrderByCreatedAtAsc(run.getId());
        if (list.isEmpty()) {
            return analyzeAndSaveUrls(emailId);
        }

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

            boolean isIp = urlHeuristicsService.isIpUrl(norm.getDomain());
            boolean unusualPort = urlHeuristicsService.isUnusualPort(norm.getPort());
            boolean isShortened = urlHeuristicsService.isShortened(norm.getDomain());
            boolean subdomainExcess = urlHeuristicsService.isSubdomainExcess(norm.getDomain());
            boolean lookalike = urlHeuristicsService.isLookalike(norm.getDomain());
            boolean anchorMismatch = urlHeuristicsService.isAnchorMismatch(norm.getDomain(), raw.getAnchorText());
            int riskScore = urlHeuristicsService.calculateRiskScore(anchorMismatch, isIp, lookalike, unusualPort, subdomainExcess, isShortened);

            String dnsStatus = isIp ? "SKIPPED_IP" : dnsLookupService.lookupDomain(norm.getDomain()).getStatus();

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
                    riskScore
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
                    e.getUrlRiskScore()
            ));
        }
        return dtos;
    }
}

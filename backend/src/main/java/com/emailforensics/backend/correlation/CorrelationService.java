package com.emailforensics.backend.correlation;

import com.emailforensics.backend.common.ResourceNotFoundException;
import com.emailforensics.backend.dto.correlation.CorrelationMatchDto;
import com.emailforensics.backend.dto.email.AttachmentMetaDto;
import com.emailforensics.backend.evidence.AnalysisRun;
import com.emailforensics.backend.evidence.AnalysisRunRepository;
import com.emailforensics.backend.evidence.EmailEvidence;
import com.emailforensics.backend.evidence.EmailEvidenceRepository;
import com.emailforensics.backend.evidence.EvidenceService;
import com.emailforensics.backend.headers.HeaderAnalysis;
import com.emailforensics.backend.headers.HeaderAnalysisRepository;
import com.emailforensics.backend.infrastructure.IpArtifact;
import com.emailforensics.backend.infrastructure.IpArtifactRepository;
import com.emailforensics.backend.urlintel.ExtractedUrl;
import com.emailforensics.backend.urlintel.ExtractedUrlRepository;
import com.emailforensics.backend.visual.QrArtifact;
import com.emailforensics.backend.visual.QrArtifactRepository;
import com.emailforensics.backend.visual.VisualArtifact;
import com.emailforensics.backend.visual.VisualArtifactRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class CorrelationService {

    private final HistoricalMatchRepository matchRepository;
    private final EmailEvidenceRepository evidenceRepository;
    private final AnalysisRunRepository runRepository;
    private final ExtractedUrlRepository urlRepository;
    private final IpArtifactRepository ipRepository;
    private final HeaderAnalysisRepository headerRepository;
    private final VisualArtifactRepository visualRepository;
    private final QrArtifactRepository qrRepository;
    private final EvidenceService evidenceService;

    public CorrelationService(HistoricalMatchRepository matchRepository,
                              EmailEvidenceRepository evidenceRepository,
                              AnalysisRunRepository runRepository,
                              ExtractedUrlRepository urlRepository,
                              IpArtifactRepository ipRepository,
                              HeaderAnalysisRepository headerRepository,
                              VisualArtifactRepository visualRepository,
                              QrArtifactRepository qrRepository,
                              @Lazy EvidenceService evidenceService) {
        this.matchRepository = matchRepository;
        this.evidenceRepository = evidenceRepository;
        this.runRepository = runRepository;
        this.urlRepository = urlRepository;
        this.ipRepository = ipRepository;
        this.headerRepository = headerRepository;
        this.visualRepository = visualRepository;
        this.qrRepository = qrRepository;
        this.evidenceService = evidenceService;
    }

    @Transactional
    public List<CorrelationMatchDto> getCorrelations(UUID emailId) {
        List<HistoricalMatch> existing = matchRepository.findBySourceEmailId(emailId);
        if (!existing.isEmpty()) {
            return mapMatchesToDto(existing);
        }
        return correlateAndSave(emailId);
    }

    @Transactional
    public List<CorrelationMatchDto> correlateAndSave(UUID emailId) {
        EmailEvidence sourceEvidence = evidenceRepository.findById(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence not found with ID: " + emailId));

        AnalysisRun sourceRun = runRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis run not found for evidence: " + emailId));

        List<ExtractedUrl> sourceUrls = urlRepository.findByAnalysisRunIdOrderByCreatedAtAsc(sourceRun.getId());
        List<IpArtifact> sourceIps = ipRepository.findByAnalysisRunIdOrderByCreatedAtAsc(sourceRun.getId());
        Optional<HeaderAnalysis> sourceHeader = headerRepository.findByAnalysisRunId(sourceRun.getId());
        List<VisualArtifact> sourceVisuals = visualRepository.findByAnalysisRunIdOrderByCreatedAtAsc(sourceRun.getId());
        List<AttachmentMetaDto> sourceAttachments = evidenceService.getEvidenceAttachments(emailId);

        List<HistoricalMatch> newMatches = new ArrayList<>();
        Set<String> matchDeduplicationKeys = new HashSet<>();

        List<EmailEvidence> sameHashEmails = evidenceRepository.findAllBySha256Hash(sourceEvidence.getSha256Hash());
        for (EmailEvidence other : sameHashEmails) {
            if (!other.getId().equals(sourceEvidence.getId())) {
                addMatchCandidate(newMatches, matchDeduplicationKeys, sourceRun, other,
                        "EMAIL_HASH", sourceEvidence.getSha256Hash(), "EXACT",
                        "Identical email message SHA-256 hash observed in Case " + other.getCaseFile().getCaseNumber());
            }
        }

        if (sourceAttachments != null && !sourceAttachments.isEmpty()) {
            List<EmailEvidence> allOtherEmails = evidenceRepository.findAll();
            for (EmailEvidence other : allOtherEmails) {
                if (other.getId().equals(sourceEvidence.getId())) continue;
                try {
                    List<AttachmentMetaDto> otherAtts = evidenceService.getEvidenceAttachments(other.getId());
                    if (otherAtts != null) {
                        for (AttachmentMetaDto sAtt : sourceAttachments) {
                            if (sAtt.getSha256Hash() == null || sAtt.getSha256Hash().isBlank()) continue;
                            for (AttachmentMetaDto oAtt : otherAtts) {
                                if (sAtt.getSha256Hash().equalsIgnoreCase(oAtt.getSha256Hash())) {
                                    addMatchCandidate(newMatches, matchDeduplicationKeys, sourceRun, other,
                                            "ATTACHMENT_HASH", sAtt.getSha256Hash(), "EXACT",
                                            "Identical attachment file payload (" + sAtt.getFilename() + ") also observed in Case " + other.getCaseFile().getCaseNumber());
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }

        for (ExtractedUrl u : sourceUrls) {
            String norm = u.getNormalizedUrl();
            if (norm != null && !norm.isBlank()) {
                List<ExtractedUrl> normMatches = urlRepository.findByNormalizedUrl(norm);
                for (ExtractedUrl mUrl : normMatches) {
                    EmailEvidence other = mUrl.getAnalysisRun().getEmailEvidence();
                    if (!other.getId().equals(sourceEvidence.getId())) {
                        addMatchCandidate(newMatches, matchDeduplicationKeys, sourceRun, other,
                                "URL", norm, "EXACT",
                                "Exact matching target URL (" + norm + ") identified in Case " + other.getCaseFile().getCaseNumber());
                    }
                }
            }

            String domain = u.getDomain();
            if (domain != null && !domain.isBlank()) {
                List<ExtractedUrl> domMatches = urlRepository.findByDomain(domain);
                for (ExtractedUrl mUrl : domMatches) {
                    EmailEvidence other = mUrl.getAnalysisRun().getEmailEvidence();
                    if (!other.getId().equals(sourceEvidence.getId())) {
                        addMatchCandidate(newMatches, matchDeduplicationKeys, sourceRun, other,
                                "DOMAIN", domain, "HIGH",
                                "Shared attack destination domain (" + domain + ") detected across investigations");
                    }
                }
            }
        }

        for (IpArtifact ip : sourceIps) {
            if (ip.getIsPrivate() != null && !ip.getIsPrivate() && ip.getIpAddress() != null) {
                List<IpArtifact> ipMatches = ipRepository.findByIpAddress(ip.getIpAddress());
                for (IpArtifact mIp : ipMatches) {
                    EmailEvidence other = mIp.getAnalysisRun().getEmailEvidence();
                    if (!other.getId().equals(sourceEvidence.getId())) {
                        addMatchCandidate(newMatches, matchDeduplicationKeys, sourceRun, other,
                                "IP_ADDRESS", ip.getIpAddress(), "EXACT",
                                "Identical transit mail server IP (" + ip.getIpAddress() + ") linked to Case " + other.getCaseFile().getCaseNumber());
                    }
                }
            }
        }

        for (VisualArtifact v : sourceVisuals) {
            List<QrArtifact> qrs = qrRepository.findByVisualArtifactIdOrderByCreatedAtAsc(v.getId());
            for (QrArtifact qr : qrs) {
                if (qr.getDecodedPayload() != null && !qr.getDecodedPayload().isBlank()) {
                    List<QrArtifact> qrMatches = qrRepository.findByDecodedPayload(qr.getDecodedPayload());
                    for (QrArtifact mQr : qrMatches) {
                        EmailEvidence other = mQr.getVisualArtifact().getAnalysisRun().getEmailEvidence();
                        if (!other.getId().equals(sourceEvidence.getId())) {
                            addMatchCandidate(newMatches, matchDeduplicationKeys, sourceRun, other,
                                    "QR_PAYLOAD", qr.getDecodedPayload(), "EXACT",
                                    "Identical decoded QR code destination URL observed in Case " + other.getCaseFile().getCaseNumber());
                        }
                    }
                }
            }
        }

        if (sourceHeader.isPresent()) {
            String fromDomain = sourceHeader.get().getFromDomain();
            if (fromDomain != null && !fromDomain.isBlank()) {
                List<HeaderAnalysis> hMatches = headerRepository.findByFromDomain(fromDomain);
                for (HeaderAnalysis mH : hMatches) {
                    EmailEvidence other = mH.getAnalysisRun().getEmailEvidence();
                    if (!other.getId().equals(sourceEvidence.getId())) {
                        addMatchCandidate(newMatches, matchDeduplicationKeys, sourceRun, other,
                                "SENDER_DOMAIN", fromDomain, "MEDIUM",
                                "Matching sender domain (" + fromDomain + ") found in previous investigation " + other.getCaseFile().getCaseNumber());
                    }
                }
            }
        }

        List<HistoricalMatch> savedMatches = new ArrayList<>();
        for (HistoricalMatch candidate : newMatches) {
            boolean exists = matchRepository.existsBySourceRunIdAndMatchedEmailIdAndIndicatorTypeAndNormalizedIndicatorValue(
                    candidate.getSourceRun().getId(),
                    candidate.getMatchedEmail().getId(),
                    candidate.getIndicatorType(),
                    candidate.getNormalizedIndicatorValue()
            );
            if (!exists) {
                savedMatches.add(matchRepository.save(candidate));
            }
        }

        return mapMatchesToDto(matchRepository.findBySourceEmailId(emailId));
    }

    private void addMatchCandidate(List<HistoricalMatch> list, Set<String> deduplicationKeys,
                                   AnalysisRun sourceRun, EmailEvidence other,
                                   String type, String value, String strength, String explanation) {
        String key = other.getId() + "|" + type + "|" + value;
        if (deduplicationKeys.add(key)) {
            HistoricalMatch hm = new HistoricalMatch(
                    UUID.randomUUID(),
                    sourceRun,
                    other,
                    type,
                    value,
                    strength,
                    explanation
            );
            list.add(hm);
        }
    }

    private List<CorrelationMatchDto> mapMatchesToDto(List<HistoricalMatch> matches) {
        List<CorrelationMatchDto> dtos = new ArrayList<>();
        for (HistoricalMatch m : matches) {
            EmailEvidence matched = m.getMatchedEmail();
            CorrelationMatchDto dto = new CorrelationMatchDto(
                    m.getId(),
                    m.getSourceRun().getId(),
                    m.getSourceRun().getEmailEvidence().getId(),
                    matched.getId(),
                    matched.getCaseFile().getId(),
                    matched.getCaseFile().getCaseNumber(),
                    matched.getCaseFile().getTitle(),
                    matched.getSubject() != null ? matched.getSubject() : "Untitled Email",
                    matched.getOriginalFilename(),
                    matched.getSentDate(),
                    matched.getUploadedAt(),
                    m.getIndicatorType(),
                    m.getNormalizedIndicatorValue(),
                    m.getMatchStrength(),
                    m.getMatchExplanation(),
                    m.getCreatedAt()
            );
            dtos.add(dto);
        }
        return dtos;
    }
}

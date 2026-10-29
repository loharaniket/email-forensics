package com.emailforensics.backend.graph;

import com.emailforensics.backend.common.ResourceNotFoundException;
import com.emailforensics.backend.correlation.HistoricalMatch;
import com.emailforensics.backend.correlation.HistoricalMatchRepository;
import com.emailforensics.backend.dto.email.AttachmentMetaDto;
import com.emailforensics.backend.dto.graph.GraphDataDto;
import com.emailforensics.backend.dto.graph.GraphLinkDto;
import com.emailforensics.backend.dto.graph.GraphNodeDto;
import com.emailforensics.backend.evidence.AnalysisRun;
import com.emailforensics.backend.evidence.AnalysisRunRepository;
import com.emailforensics.backend.evidence.EmailEvidence;
import com.emailforensics.backend.evidence.EmailEvidenceRepository;
import com.emailforensics.backend.evidence.EvidenceService;
import com.emailforensics.backend.infrastructure.IpArtifact;
import com.emailforensics.backend.infrastructure.IpArtifactRepository;
import com.emailforensics.backend.risk.RiskAssessment;
import com.emailforensics.backend.risk.RiskAssessmentRepository;
import com.emailforensics.backend.urlintel.ExtractedUrl;
import com.emailforensics.backend.urlintel.ExtractedUrlRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class GraphService {

    private final EmailEvidenceRepository evidenceRepository;
    private final AnalysisRunRepository runRepository;
    private final ExtractedUrlRepository urlRepository;
    private final IpArtifactRepository ipRepository;
    private final HistoricalMatchRepository matchRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final EvidenceService evidenceService;

    public GraphService(EmailEvidenceRepository evidenceRepository,
                        AnalysisRunRepository runRepository,
                        ExtractedUrlRepository urlRepository,
                        IpArtifactRepository ipRepository,
                        HistoricalMatchRepository matchRepository,
                        RiskAssessmentRepository riskAssessmentRepository,
                        @Lazy EvidenceService evidenceService) {
        this.evidenceRepository = evidenceRepository;
        this.runRepository = runRepository;
        this.urlRepository = urlRepository;
        this.ipRepository = ipRepository;
        this.matchRepository = matchRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.evidenceService = evidenceService;
    }

    @Transactional(readOnly = true)
    public GraphDataDto getEvidenceGraph(UUID emailId) {
        EmailEvidence evidence = evidenceRepository.findById(emailId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence not found with ID: " + emailId));

        Map<String, GraphNodeDto> nodeMap = new HashMap<>();
        List<GraphLinkDto> links = new ArrayList<>();
        Set<String> linkKeys = new HashSet<>();

        String primaryNodeId = "email-" + evidence.getId();
        Integer primaryRisk = null;
        Optional<RiskAssessment> raOpt = riskAssessmentRepository.findTopByEmailEvidenceId(emailId);
        if (raOpt.isPresent()) {
            primaryRisk = raOpt.get().getDeterministicRiskScore();
        }

        String primaryLabel = evidence.getSubject() != null && !evidence.getSubject().isBlank()
                ? evidence.getSubject()
                : evidence.getOriginalFilename();

        GraphNodeDto primaryNode = new GraphNodeDto(
                primaryNodeId,
                primaryLabel,
                "EMAIL",
                26,
                "PRIMARY_EMAIL",
                "#38bdf8",
                primaryRisk
        );
        primaryNode.getMetadata().put("isPrimary", true);
        primaryNode.getMetadata().put("caseId", evidence.getCaseFile().getId().toString());
        primaryNode.getMetadata().put("caseNumber", evidence.getCaseFile().getCaseNumber());
        primaryNode.getMetadata().put("caseTitle", evidence.getCaseFile().getTitle());
        primaryNode.getMetadata().put("filename", evidence.getOriginalFilename());
        primaryNode.getMetadata().put("sha256Hash", evidence.getSha256Hash());
        nodeMap.put(primaryNodeId, primaryNode);

        Optional<AnalysisRun> runOpt = runRepository.findTopByEmailEvidenceIdOrderByCreatedAtDesc(emailId);
        if (runOpt.isPresent()) {
            UUID runId = runOpt.get().getId();

            List<ExtractedUrl> urls = urlRepository.findByAnalysisRunIdOrderByCreatedAtAsc(runId);
            for (ExtractedUrl u : urls) {
                String urlNodeId = "url-" + u.getId();
                String urlColor = (u.getIsIpUrl() || u.getIsShortened() || u.getAnchorMismatch()) ? "#ef4444" : "#f59e0b";
                GraphNodeDto urlNode = new GraphNodeDto(
                        urlNodeId,
                        u.getNormalizedUrl(),
                        "URL",
                        12,
                        "URL",
                        urlColor,
                        u.getUrlRiskScore()
                );
                urlNode.getMetadata().put("rawUrl", u.getRawUrl());
                urlNode.getMetadata().put("sourceType", u.getSourceType());
                nodeMap.put(urlNodeId, urlNode);
                addLink(links, linkKeys, primaryNodeId, urlNodeId, "CONTAINS_URL", "CONTAINS_URL", 0.7);

                if (u.getDomain() != null && !u.getDomain().isBlank()) {
                    String domainNodeId = "domain-" + u.getDomain().toLowerCase();
                    if (!nodeMap.containsKey(domainNodeId)) {
                        GraphNodeDto domNode = new GraphNodeDto(
                                domainNodeId,
                                u.getDomain(),
                                "DOMAIN",
                                14,
                                "DOMAIN",
                                "#eab308",
                                null
                        );
                        nodeMap.put(domainNodeId, domNode);
                    }
                    addLink(links, linkKeys, urlNodeId, domainNodeId, "HOSTED_ON", "HOSTED_ON", 0.6);
                }
            }

            List<IpArtifact> ips = ipRepository.findByAnalysisRunIdOrderByCreatedAtAsc(runId);
            for (IpArtifact ip : ips) {
                String ipNodeId = "ip-" + ip.getId();
                String ipColor = (ip.getIsPrivate() != null && !ip.getIsPrivate()) ? "#ef4444" : "#64748b";
                GraphNodeDto ipNode = new GraphNodeDto(
                        ipNodeId,
                        ip.getIpAddress(),
                        "IP",
                        14,
                        "IP",
                        ipColor,
                        null
                );
                ipNode.getMetadata().put("isPrivate", ip.getIsPrivate());
                nodeMap.put(ipNodeId, ipNode);
                addLink(links, linkKeys, primaryNodeId, ipNodeId, "ROUTED_THROUGH", "ROUTED_THROUGH", 0.6);
            }
        }

        try {
            List<AttachmentMetaDto> atts = evidenceService.getEvidenceAttachments(emailId);
            if (atts != null) {
                for (AttachmentMetaDto att : atts) {
                    String attNodeId = "att-" + (att.getSha256Hash() != null ? att.getSha256Hash() : UUID.randomUUID().toString());
                    if (!nodeMap.containsKey(attNodeId)) {
                        GraphNodeDto attNode = new GraphNodeDto(
                                attNodeId,
                                att.getFilename(),
                                "ATTACHMENT",
                                14,
                                "ATTACHMENT",
                                "#ec4899",
                                null
                        );
                        attNode.getMetadata().put("sha256Hash", att.getSha256Hash());
                        attNode.getMetadata().put("mimeType", att.getMimeType());
                        attNode.getMetadata().put("sizeBytes", att.getSizeBytes());
                        nodeMap.put(attNodeId, attNode);
                    }
                    addLink(links, linkKeys, primaryNodeId, attNodeId, "HAS_ATTACHMENT", "HAS_ATTACHMENT", 0.7);
                }
            }
        } catch (Exception ignored) {
        }

        List<HistoricalMatch> matches = matchRepository.findBySourceEmailId(emailId);
        for (HistoricalMatch m : matches) {
            EmailEvidence matched = m.getMatchedEmail();
            String matchedNodeId = "email-" + matched.getId();

            if (!nodeMap.containsKey(matchedNodeId)) {
                String mLabel = matched.getSubject() != null && !matched.getSubject().isBlank()
                        ? matched.getSubject()
                        : matched.getOriginalFilename();

                Integer mRisk = null;
                Optional<RiskAssessment> mRa = riskAssessmentRepository.findTopByEmailEvidenceId(matched.getId());
                if (mRa.isPresent()) {
                    mRisk = mRa.get().getDeterministicRiskScore();
                }

                GraphNodeDto matchedNode = new GraphNodeDto(
                        matchedNodeId,
                        mLabel,
                        "EMAIL",
                        18,
                        "CORRELATED_EMAIL",
                        "#06b6d4",
                        mRisk
                );
                matchedNode.getMetadata().put("isCorrelated", true);
                matchedNode.getMetadata().put("caseId", matched.getCaseFile().getId().toString());
                matchedNode.getMetadata().put("caseNumber", matched.getCaseFile().getCaseNumber());
                matchedNode.getMetadata().put("filename", matched.getOriginalFilename());
                nodeMap.put(matchedNodeId, matchedNode);
            }

            addLink(links, linkKeys, primaryNodeId, matchedNodeId,
                    m.getIndicatorType() + " (" + m.getMatchStrength() + ")",
                    "HISTORICAL_MATCH",
                    "EXACT".equalsIgnoreCase(m.getMatchStrength()) ? 0.9 : 0.7);
        }

        return new GraphDataDto(new ArrayList<>(nodeMap.values()), links);
    }

    @Transactional(readOnly = true)
    public GraphDataDto getGlobalGraph(int limit) {
        int boundedLimit = Math.min(Math.max(limit, 10), 500);

        Map<String, GraphNodeDto> nodeMap = new HashMap<>();
        List<GraphLinkDto> links = new ArrayList<>();
        Set<String> linkKeys = new HashSet<>();

        List<HistoricalMatch> allMatches = matchRepository.findAll();
        for (HistoricalMatch m : allMatches) {
            if (nodeMap.size() >= boundedLimit) break;
            EmailEvidence src = m.getSourceRun().getEmailEvidence();
            EmailEvidence tgt = m.getMatchedEmail();

            String sId = "email-" + src.getId();
            String tId = "email-" + tgt.getId();

            if (!nodeMap.containsKey(sId)) {
                String label = src.getSubject() != null ? src.getSubject() : src.getOriginalFilename();
                GraphNodeDto sn = new GraphNodeDto(sId, label, "EMAIL", 16, "EMAIL", "#06b6d4", null);
                sn.getMetadata().put("caseNumber", src.getCaseFile().getCaseNumber());
                sn.getMetadata().put("caseId", src.getCaseFile().getId().toString());
                nodeMap.put(sId, sn);
            }

            if (!nodeMap.containsKey(tId)) {
                String label = tgt.getSubject() != null ? tgt.getSubject() : tgt.getOriginalFilename();
                GraphNodeDto tn = new GraphNodeDto(tId, label, "EMAIL", 16, "EMAIL", "#06b6d4", null);
                tn.getMetadata().put("caseNumber", tgt.getCaseFile().getCaseNumber());
                tn.getMetadata().put("caseId", tgt.getCaseFile().getId().toString());
                nodeMap.put(tId, tn);
            }

            addLink(links, linkKeys, sId, tId, m.getIndicatorType(), "HISTORICAL_MATCH", 0.8);
        }

        // Also include standalone email evidence nodes if capacity allows
        if (nodeMap.size() < boundedLimit) {
            List<EmailEvidence> allEvidences = evidenceRepository.findAll();
            for (EmailEvidence ev : allEvidences) {
                if (nodeMap.size() >= boundedLimit) break;
                String eId = "email-" + ev.getId();
                if (!nodeMap.containsKey(eId)) {
                    String label = ev.getSubject() != null && !ev.getSubject().isBlank()
                            ? ev.getSubject()
                            : ev.getOriginalFilename();
                    GraphNodeDto eNode = new GraphNodeDto(eId, label, "EMAIL", 16, "EMAIL", "#06b6d4", null);
                    eNode.getMetadata().put("caseNumber", ev.getCaseFile().getCaseNumber());
                    eNode.getMetadata().put("caseId", ev.getCaseFile().getId().toString());
                    nodeMap.put(eId, eNode);
                }
            }
        }

        return new GraphDataDto(new ArrayList<>(nodeMap.values()), links);
    }

    private void addLink(List<GraphLinkDto> links, Set<String> linkKeys,
                         String source, String target, String label, String type, double strength) {
        String key = source + "->" + target + ":" + type;
        String reverseKey = target + "->" + source + ":" + type;
        if (!linkKeys.contains(key) && !linkKeys.contains(reverseKey)) {
            linkKeys.add(key);
            links.add(new GraphLinkDto(source, target, label, type, strength));
        }
    }
}

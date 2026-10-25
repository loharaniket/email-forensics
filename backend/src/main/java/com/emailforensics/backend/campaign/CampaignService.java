package com.emailforensics.backend.campaign;

import com.emailforensics.backend.common.ResourceNotFoundException;
import com.emailforensics.backend.correlation.HistoricalMatch;
import com.emailforensics.backend.dto.campaign.CampaignDetailDto;
import com.emailforensics.backend.dto.campaign.CampaignMemberDto;
import com.emailforensics.backend.dto.campaign.CampaignSummaryDto;
import com.emailforensics.backend.dto.common.PageResponse;
import com.emailforensics.backend.dto.graph.GraphDataDto;
import com.emailforensics.backend.dto.graph.GraphLinkDto;
import com.emailforensics.backend.dto.graph.GraphNodeDto;
import com.emailforensics.backend.evidence.EmailEvidence;
import com.emailforensics.backend.evidence.EmailEvidenceRepository;
import com.emailforensics.backend.risk.RiskAssessment;
import com.emailforensics.backend.risk.RiskAssessmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CampaignMemberRepository campaignMemberRepository;
    private final EmailEvidenceRepository evidenceRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;

    public CampaignService(CampaignRepository campaignRepository,
                           CampaignMemberRepository campaignMemberRepository,
                           EmailEvidenceRepository evidenceRepository,
                           RiskAssessmentRepository riskAssessmentRepository) {
        this.campaignRepository = campaignRepository;
        this.campaignMemberRepository = campaignMemberRepository;
        this.evidenceRepository = evidenceRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<CampaignSummaryDto> getCampaigns(Pageable pageable) {
        Page<Campaign> page = campaignRepository.findAllByOrderByCreatedAtDesc(pageable);
        List<CampaignSummaryDto> summaries = new ArrayList<>();

        for (Campaign c : page.getContent()) {
            List<CampaignMember> members = campaignMemberRepository.findByCampaignIdOrderByCreatedAtAsc(c.getId());
            List<String> indicators = parseSharedIndicators(c.getSharedIndicatorsJson());

            CampaignSummaryDto dto = new CampaignSummaryDto(
                    c.getId(),
                    c.getCampaignCode(),
                    c.getName(),
                    c.getConfidenceScore(),
                    c.getSummary(),
                    members.size(),
                    c.getFirstSeen(),
                    c.getLastSeen(),
                    c.getCreatedAt(),
                    indicators
            );
            summaries.add(dto);
        }

        return new PageResponse<>(
                summaries,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    @Transactional(readOnly = true)
    public CampaignDetailDto getCampaignDetail(UUID campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + campaignId));

        List<CampaignMember> members = campaignMemberRepository.findByCampaignIdOrderByCreatedAtAsc(campaignId);
        List<CampaignMemberDto> memberDtos = new ArrayList<>();

        for (CampaignMember cm : members) {
            EmailEvidence ev = cm.getEmailEvidence();
            Integer riskScore = null;
            String riskLevel = null;

            Optional<RiskAssessment> ra = riskAssessmentRepository.findTopByEmailEvidenceId(ev.getId());
            if (ra.isPresent()) {
                riskScore = ra.get().getDeterministicRiskScore();
                riskLevel = ra.get().getRiskLevel();
            }

            CampaignMemberDto mDto = new CampaignMemberDto(
                    cm.getId(),
                    ev.getId(),
                    ev.getCaseFile().getId(),
                    ev.getCaseFile().getCaseNumber(),
                    ev.getCaseFile().getTitle(),
                    ev.getSubject() != null ? ev.getSubject() : "Untitled Evidence",
                    ev.getOriginalFilename(),
                    cm.getMembershipReason(),
                    cm.getMatchScore(),
                    riskScore,
                    riskLevel,
                    ev.getUploadedAt()
            );
            memberDtos.add(mDto);
        }

        List<String> indicators = parseSharedIndicators(campaign.getSharedIndicatorsJson());
        GraphDataDto graphData = buildCampaignGraph(campaign, members, indicators);

        return new CampaignDetailDto(
                campaign.getId(),
                campaign.getCampaignCode(),
                campaign.getName(),
                campaign.getConfidenceScore(),
                campaign.getSummary(),
                campaign.getFirstSeen(),
                campaign.getLastSeen(),
                campaign.getCreatedAt(),
                indicators,
                memberDtos,
                graphData
        );
    }

    @Transactional(readOnly = true)
    public List<CampaignSummaryDto> getCampaignsForEvidence(UUID emailId) {
        List<CampaignMember> memberships = campaignMemberRepository.findByEmailEvidenceId(emailId);
        List<CampaignSummaryDto> results = new ArrayList<>();

        for (CampaignMember cm : memberships) {
            Campaign c = cm.getCampaign();
            List<CampaignMember> allMembers = campaignMemberRepository.findByCampaignIdOrderByCreatedAtAsc(c.getId());
            List<String> indicators = parseSharedIndicators(c.getSharedIndicatorsJson());

            results.add(new CampaignSummaryDto(
                    c.getId(),
                    c.getCampaignCode(),
                    c.getName(),
                    c.getConfidenceScore(),
                    c.getSummary(),
                    allMembers.size(),
                    c.getFirstSeen(),
                    c.getLastSeen(),
                    c.getCreatedAt(),
                    indicators
            ));
        }

        return results;
    }

    @Transactional
    public void clusterIntoCampaign(EmailEvidence sourceEmail, List<HistoricalMatch> matches) {
        if (matches == null || matches.isEmpty()) {
            return;
        }

        Set<UUID> matchedEmailIds = new HashSet<>();
        Map<String, String> indicatorMap = new HashMap<>();

        for (HistoricalMatch match : matches) {
            if (match.getMatchedEmail() != null && !match.getMatchedEmail().getId().equals(sourceEmail.getId())) {
                matchedEmailIds.add(match.getMatchedEmail().getId());
                indicatorMap.put(match.getIndicatorType() + ":" + match.getNormalizedIndicatorValue(), match.getMatchStrength());
            }
        }

        if (matchedEmailIds.isEmpty()) {
            return;
        }

        Campaign existingCampaign = null;
        for (UUID matchedId : matchedEmailIds) {
            List<CampaignMember> existingMemberships = campaignMemberRepository.findByEmailEvidenceId(matchedId);
            if (!existingMemberships.isEmpty()) {
                existingCampaign = existingMemberships.get(0).getCampaign();
                break;
            }
        }

        if (existingCampaign == null) {
            List<CampaignMember> sourceMemberships = campaignMemberRepository.findByEmailEvidenceId(sourceEmail.getId());
            if (!sourceMemberships.isEmpty()) {
                existingCampaign = sourceMemberships.get(0).getCampaign();
            }
        }

        if (existingCampaign != null) {
            if (!campaignMemberRepository.existsByCampaignIdAndEmailEvidenceId(existingCampaign.getId(), sourceEmail.getId())) {
                String reason = buildMembershipReason(indicatorMap);
                int score = calculateMatchScore(indicatorMap);

                CampaignMember newMember = new CampaignMember(
                        UUID.randomUUID(),
                        existingCampaign,
                        sourceEmail,
                        reason,
                        score
                );
                campaignMemberRepository.save(newMember);

                existingCampaign.setLastSeen(Instant.now());
                mergeSharedIndicators(existingCampaign, indicatorMap.keySet());
                campaignRepository.save(existingCampaign);
            }

            for (UUID mId : matchedEmailIds) {
                if (!campaignMemberRepository.existsByCampaignIdAndEmailEvidenceId(existingCampaign.getId(), mId)) {
                    Optional<EmailEvidence> matchedEvOpt = evidenceRepository.findById(mId);
                    if (matchedEvOpt.isPresent()) {
                        EmailEvidence matchedEv = matchedEvOpt.get();
                        String mReason = buildMembershipReason(indicatorMap);
                        int mScore = calculateMatchScore(indicatorMap);
                        CampaignMember additionalMember = new CampaignMember(
                                UUID.randomUUID(),
                                existingCampaign,
                                matchedEv,
                                mReason,
                                mScore
                        );
                        campaignMemberRepository.save(additionalMember);
                    }
                }
            }
            return;
        }

        boolean hasStrongMatch = false;
        for (String str : indicatorMap.values()) {
            if ("EXACT".equalsIgnoreCase(str) || "HIGH".equalsIgnoreCase(str)) {
                hasStrongMatch = true;
                break;
            }
        }

        if (!hasStrongMatch) {
            return;
        }

        long count = campaignRepository.count();
        String code = String.format("CAMP-2026-%04d", count + 1);
        String name = deriveCampaignName(indicatorMap, code);
        int confidence = Math.min(95, 75 + (indicatorMap.size() * 5));

        List<String> indicatorList = new ArrayList<>(indicatorMap.keySet());
        String indicatorsJson = serializeStringList(indicatorList);

        Campaign newCampaign = new Campaign(
                UUID.randomUUID(),
                code,
                name,
                confidence,
                "Automated multi-case forensic correlation cluster sharing technical infrastructure and threat indicators.",
                indicatorsJson,
                sourceEmail.getUploadedAt(),
                Instant.now()
        );
        newCampaign = campaignRepository.save(newCampaign);

        String sourceReason = buildMembershipReason(indicatorMap);
        int sourceScore = calculateMatchScore(indicatorMap);
        CampaignMember sourceMember = new CampaignMember(
                UUID.randomUUID(),
                newCampaign,
                sourceEmail,
                sourceReason,
                sourceScore
        );
        campaignMemberRepository.save(sourceMember);

        for (UUID mId : matchedEmailIds) {
            Optional<EmailEvidence> matchedEvOpt = evidenceRepository.findById(mId);
            if (matchedEvOpt.isPresent()) {
                EmailEvidence matchedEv = matchedEvOpt.get();
                String mReason = buildMembershipReason(indicatorMap);
                int mScore = calculateMatchScore(indicatorMap);
                CampaignMember mMember = new CampaignMember(
                        UUID.randomUUID(),
                        newCampaign,
                        matchedEv,
                        mReason,
                        mScore
                );
                campaignMemberRepository.save(mMember);
            }
        }
    }

    private String deriveCampaignName(Map<String, String> indicatorMap, String code) {
        for (String key : indicatorMap.keySet()) {
            String[] parts = key.split(":", 2);
            if (parts.length == 2) {
                String type = parts[0];
                String val = parts[1];
                if ("DOMAIN".equalsIgnoreCase(type) || "URL".equalsIgnoreCase(type)) {
                    return "Coordinated Attack Campaign targeting " + val;
                }
                if ("ATTACHMENT_HASH".equalsIgnoreCase(type)) {
                    return "Malware Payload Distribution Campaign (" + val.substring(0, Math.min(8, val.length())) + ")";
                }
                if ("QR_PAYLOAD".equalsIgnoreCase(type)) {
                    return "Quishing Phishing Campaign via QR Payload";
                }
                if ("IP_ADDRESS".equalsIgnoreCase(type)) {
                    return "Transit Infrastructure Phishing Cluster (" + val + ")";
                }
            }
        }
        return "Coordinated Threat Campaign " + code;
    }

    private String buildMembershipReason(Map<String, String> indicatorMap) {
        StringBuilder sb = new StringBuilder("Correlated with campaign via: ");
        int count = 0;
        for (String key : indicatorMap.keySet()) {
            if (count > 0) sb.append(", ");
            sb.append(key);
            count++;
            if (count >= 3) break;
        }
        if (indicatorMap.size() > 3) {
            sb.append(" (+").append(indicatorMap.size() - 3).append(" more)");
        }
        return sb.toString();
    }

    private int calculateMatchScore(Map<String, String> indicatorMap) {
        int score = 60;
        for (String strength : indicatorMap.values()) {
            if ("EXACT".equalsIgnoreCase(strength)) score += 15;
            else if ("HIGH".equalsIgnoreCase(strength)) score += 10;
            else score += 5;
        }
        return Math.min(99, score);
    }

    private void mergeSharedIndicators(Campaign campaign, Set<String> newIndicators) {
        List<String> current = parseSharedIndicators(campaign.getSharedIndicatorsJson());
        Set<String> merged = new HashSet<>(current);
        merged.addAll(newIndicators);
        campaign.setSharedIndicatorsJson(serializeStringList(new ArrayList<>(merged)));
    }

    private String serializeStringList(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(list.get(i))).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private List<String> parseSharedIndicators(String json) {
        List<String> list = new ArrayList<>();
        if (json == null || json.isBlank() || "[]".equals(json.trim())) {
            return list;
        }
        Matcher m = Pattern.compile("\"((?:\\\\\"|[^\"])*)\"").matcher(json);
        while (m.find()) {
            list.add(unescapeJson(m.group(1)));
        }
        return list;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String unescapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private GraphDataDto buildCampaignGraph(Campaign campaign, List<CampaignMember> members, List<String> indicators) {
        List<GraphNodeDto> nodes = new ArrayList<>();
        List<GraphLinkDto> links = new ArrayList<>();

        String campaignNodeId = "campaign-" + campaign.getId();
        GraphNodeDto cNode = new GraphNodeDto(
                campaignNodeId,
                campaign.getCampaignCode() + ": " + campaign.getName(),
                "CAMPAIGN",
                28,
                "CAMPAIGN",
                "#eab308",
                campaign.getConfidenceScore()
        );
        cNode.getMetadata().put("campaignCode", campaign.getCampaignCode());
        cNode.getMetadata().put("confidenceScore", campaign.getConfidenceScore());
        cNode.getMetadata().put("memberCount", members.size());
        nodes.add(cNode);

        for (CampaignMember cm : members) {
            EmailEvidence ev = cm.getEmailEvidence();
            String emailNodeId = "email-" + ev.getId();
            String label = ev.getSubject() != null && !ev.getSubject().isBlank()
                    ? ev.getSubject()
                    : ev.getOriginalFilename();

            GraphNodeDto eNode = new GraphNodeDto(
                    emailNodeId,
                    label,
                    "EMAIL",
                    18,
                    "EMAIL",
                    "#06b6d4",
                    null
            );
            eNode.getMetadata().put("caseId", ev.getCaseFile().getId().toString());
            eNode.getMetadata().put("caseNumber", ev.getCaseFile().getCaseNumber());
            eNode.getMetadata().put("filename", ev.getOriginalFilename());
            nodes.add(eNode);

            links.add(new GraphLinkDto(
                    emailNodeId,
                    campaignNodeId,
                    "PART_OF_CAMPAIGN",
                    "PART_OF_CAMPAIGN",
                    1.0
            ));
        }

        int indCount = 0;
        for (String ind : indicators) {
            indCount++;
            String indNodeId = "ind-" + indCount;
            String type = "INDICATOR";
            String color = "#a855f7";

            if (ind.startsWith("DOMAIN:") || ind.startsWith("URL:")) {
                type = "DOMAIN";
                color = "#f59e0b";
            } else if (ind.startsWith("IP_ADDRESS:")) {
                type = "IP";
                color = "#ef4444";
            } else if (ind.startsWith("ATTACHMENT_HASH:")) {
                type = "ATTACHMENT";
                color = "#ec4899";
            }

            GraphNodeDto indNode = new GraphNodeDto(
                    indNodeId,
                    ind,
                    type,
                    12,
                    type,
                    color,
                    null
            );
            nodes.add(indNode);

            links.add(new GraphLinkDto(
                    campaignNodeId,
                    indNodeId,
                    "SHARED_INDICATOR",
                    "SHARED_INDICATOR",
                    0.8
            ));
        }

        return new GraphDataDto(nodes, links);
    }
}

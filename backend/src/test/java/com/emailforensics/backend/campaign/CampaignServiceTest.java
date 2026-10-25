package com.emailforensics.backend.campaign;

import com.emailforensics.backend.casefile.CaseFile;
import com.emailforensics.backend.dto.common.PageResponse;
import com.emailforensics.backend.correlation.HistoricalMatch;
import com.emailforensics.backend.dto.campaign.CampaignDetailDto;
import com.emailforensics.backend.dto.campaign.CampaignSummaryDto;
import com.emailforensics.backend.evidence.AnalysisRun;
import com.emailforensics.backend.evidence.AnalysisStatus;
import com.emailforensics.backend.evidence.EmailEvidence;
import com.emailforensics.backend.evidence.EmailEvidenceRepository;
import com.emailforensics.backend.risk.RiskAssessmentRepository;
import com.emailforensics.backend.user.Role;
import com.emailforensics.backend.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CampaignServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignMemberRepository campaignMemberRepository;

    @Mock
    private EmailEvidenceRepository evidenceRepository;

    @Mock
    private RiskAssessmentRepository riskAssessmentRepository;

    private CampaignService campaignService;

    private User analyst;
    private CaseFile caseFile;
    private EmailEvidence email1;
    private EmailEvidence email2;
    private AnalysisRun run1;

    @BeforeEach
    void setUp() {
        campaignService = new CampaignService(
                campaignRepository,
                campaignMemberRepository,
                evidenceRepository,
                riskAssessmentRepository
        );

        analyst = new User(UUID.randomUUID(), "analyst", "analyst@test.local", "hashed", Role.ANALYST, true, Instant.now());
        caseFile = new CaseFile(UUID.randomUUID(), "CASE-2026-0001", "Campaign Investigation", "Desc", com.emailforensics.backend.casefile.CaseStatus.OPEN, "notes", analyst, Instant.now(), Instant.now());
        email1 = new EmailEvidence(UUID.randomUUID(), caseFile, "sample1.eml", "storage-1", 1024L,
                "a1b2c3d4e5f60000000000000000000000000000000000000000000000000001", analyst);
        email2 = new EmailEvidence(UUID.randomUUID(), caseFile, "sample2.eml", "storage-2", 2048L,
                "a1b2c3d4e5f60000000000000000000000000000000000000000000000000002", analyst);

        run1 = new AnalysisRun(UUID.randomUUID(), email1, "v1.0.0", AnalysisStatus.COMPLETED, "MIME_PARSED");
    }

    @Test
    void testClusterIntoCampaign_StrongMatches_CreatesNewCampaign() {
        HistoricalMatch match = new HistoricalMatch(
                UUID.randomUUID(),
                run1,
                email2,
                "DOMAIN",
                "malicious-harvest.com",
                "HIGH",
                "Shared domain detected"
        );

        when(campaignMemberRepository.findByEmailEvidenceId(email2.getId())).thenReturn(Collections.emptyList());
        when(campaignMemberRepository.findByEmailEvidenceId(email1.getId())).thenReturn(Collections.emptyList());
        when(campaignRepository.count()).thenReturn(0L);

        Campaign savedCampaign = new Campaign(
                UUID.randomUUID(),
                "CAMP-2026-0001",
                "Coordinated Attack Campaign targeting malicious-harvest.com",
                80,
                "Summary",
                "[\"DOMAIN:malicious-harvest.com\"]",
                Instant.now(),
                Instant.now()
        );
        when(campaignRepository.save(any(Campaign.class))).thenReturn(savedCampaign);
        when(evidenceRepository.findById(email2.getId())).thenReturn(Optional.of(email2));

        campaignService.clusterIntoCampaign(email1, List.of(match));

        verify(campaignRepository).save(any(Campaign.class));
        verify(campaignMemberRepository, org.mockito.Mockito.times(2)).save(any(CampaignMember.class));
    }

    @Test
    void testGetCampaigns_ReturnsPaginatedSummaries() {
        Campaign campaign = new Campaign(
                UUID.randomUUID(),
                "CAMP-2026-0001",
                "Credential Harvest Campaign",
                85,
                "Summary description",
                "[\"DOMAIN:phish-bank.com\",\"IP_ADDRESS:198.51.100.10\"]",
                Instant.now(),
                Instant.now()
        );

        when(campaignRepository.findAllByOrderByCreatedAtDesc(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(campaign)));
        when(campaignMemberRepository.findByCampaignIdOrderByCreatedAtAsc(campaign.getId()))
                .thenReturn(Collections.emptyList());

        PageResponse<CampaignSummaryDto> response = campaignService.getCampaigns(PageRequest.of(0, 10));

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        CampaignSummaryDto dto = response.getContent().get(0);
        assertEquals("CAMP-2026-0001", dto.getCampaignCode());
        assertEquals("Credential Harvest Campaign", dto.getName());
        assertEquals(2, dto.getSharedIndicators().size());
        assertTrue(dto.getSharedIndicators().contains("DOMAIN:phish-bank.com"));
    }

    @Test
    void testGetCampaignDetail_BuildsGraphWithMembersAndIndicators() {
        Campaign campaign = new Campaign(
                UUID.randomUUID(),
                "CAMP-2026-0001",
                "Targeted Phishing Cluster",
                90,
                "Multi-case cluster",
                "[\"DOMAIN:target-portal.com\"]",
                Instant.now(),
                Instant.now()
        );

        CampaignMember member = new CampaignMember(
                UUID.randomUUID(),
                campaign,
                email1,
                "Matched domain indicator",
                85
        );

        when(campaignRepository.findById(campaign.getId())).thenReturn(Optional.of(campaign));
        when(campaignMemberRepository.findByCampaignIdOrderByCreatedAtAsc(campaign.getId()))
                .thenReturn(List.of(member));
        when(riskAssessmentRepository.findTopByEmailEvidenceId(email1.getId())).thenReturn(Optional.empty());

        CampaignDetailDto detail = campaignService.getCampaignDetail(campaign.getId());

        assertNotNull(detail);
        assertEquals("CAMP-2026-0001", detail.getCampaignCode());
        assertEquals(1, detail.getMembers().size());
        assertEquals(email1.getId(), detail.getMembers().get(0).getEmailId());

        assertNotNull(detail.getGraphData());
        assertFalse(detail.getGraphData().getNodes().isEmpty());
        assertFalse(detail.getGraphData().getLinks().isEmpty());
    }
}

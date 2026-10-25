package com.emailforensics.backend.campaign;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignMemberRepository extends JpaRepository<CampaignMember, UUID> {

    List<CampaignMember> findByCampaignIdOrderByCreatedAtAsc(UUID campaignId);

    List<CampaignMember> findByEmailEvidenceId(UUID emailId);

    Optional<CampaignMember> findByCampaignIdAndEmailEvidenceId(UUID campaignId, UUID emailId);

    boolean existsByCampaignIdAndEmailEvidenceId(UUID campaignId, UUID emailId);
}

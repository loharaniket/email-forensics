package com.emailforensics.backend.campaign;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

    Optional<Campaign> findByCampaignCode(String campaignCode);

    Page<Campaign> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT COUNT(c) FROM Campaign c")
    long countAllCampaigns();

    @Query("SELECT c FROM Campaign c JOIN c.members m WHERE m.emailEvidence.id = :emailId")
    java.util.List<Campaign> findCampaignsByEmailId(@Param("emailId") UUID emailId);
}

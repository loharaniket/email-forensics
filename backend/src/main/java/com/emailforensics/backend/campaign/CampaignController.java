package com.emailforensics.backend.campaign;

import com.emailforensics.backend.dto.common.PageResponse;
import com.emailforensics.backend.dto.campaign.CampaignDetailDto;
import com.emailforensics.backend.dto.campaign.CampaignSummaryDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<CampaignSummaryDto>> getCampaigns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int boundedPage = Math.max(page, 0);
        int boundedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(boundedPage, boundedSize);

        PageResponse<CampaignSummaryDto> response = campaignService.getCampaigns(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{campaignId}")
    public ResponseEntity<CampaignDetailDto> getCampaignDetail(@PathVariable UUID campaignId) {
        CampaignDetailDto response = campaignService.getCampaignDetail(campaignId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/evidence/{emailId}")
    public ResponseEntity<List<CampaignSummaryDto>> getCampaignsForEvidence(@PathVariable UUID emailId) {
        List<CampaignSummaryDto> response = campaignService.getCampaignsForEvidence(emailId);
        return ResponseEntity.ok(response);
    }
}

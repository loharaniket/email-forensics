package com.emailforensics.backend.dto.campaign;

import com.emailforensics.backend.dto.graph.GraphDataDto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CampaignDetailDto {

    private UUID id;
    private String campaignCode;
    private String name;
    private Integer confidenceScore;
    private String summary;
    private Instant firstSeen;
    private Instant lastSeen;
    private Instant createdAt;
    private List<String> sharedIndicators = new ArrayList<>();
    private List<CampaignMemberDto> members = new ArrayList<>();
    private GraphDataDto graphData;

    public CampaignDetailDto() {
    }

    public CampaignDetailDto(UUID id, String campaignCode, String name, Integer confidenceScore,
                             String summary, Instant firstSeen, Instant lastSeen, Instant createdAt,
                             List<String> sharedIndicators, List<CampaignMemberDto> members,
                             GraphDataDto graphData) {
        this.id = id;
        this.campaignCode = campaignCode;
        this.name = name;
        this.confidenceScore = confidenceScore;
        this.summary = summary;
        this.firstSeen = firstSeen;
        this.lastSeen = lastSeen;
        this.createdAt = createdAt;
        this.sharedIndicators = sharedIndicators != null ? sharedIndicators : new ArrayList<>();
        this.members = members != null ? members : new ArrayList<>();
        this.graphData = graphData;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCampaignCode() {
        return campaignCode;
    }

    public void setCampaignCode(String campaignCode) {
        this.campaignCode = campaignCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Integer confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Instant getFirstSeen() {
        return firstSeen;
    }

    public void setFirstSeen(Instant firstSeen) {
        this.firstSeen = firstSeen;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<String> getSharedIndicators() {
        return sharedIndicators;
    }

    public void setSharedIndicators(List<String> sharedIndicators) {
        this.sharedIndicators = sharedIndicators;
    }

    public List<CampaignMemberDto> getMembers() {
        return members;
    }

    public void setMembers(List<CampaignMemberDto> members) {
        this.members = members;
    }

    public GraphDataDto getGraphData() {
        return graphData;
    }

    public void setGraphData(GraphDataDto graphData) {
        this.graphData = graphData;
    }
}

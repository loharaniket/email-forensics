package com.emailforensics.backend.dto.graph;

import java.util.HashMap;
import java.util.Map;

public class GraphNodeDto {

    private String id;
    private String label;
    private String type;
    private Integer val;
    private String group;
    private String color;
    private Integer riskScore;
    private Map<String, Object> metadata = new HashMap<>();

    public GraphNodeDto() {
    }

    public GraphNodeDto(String id, String label, String type, Integer val, String group, String color, Integer riskScore) {
        this.id = id;
        this.label = label;
        this.type = type;
        this.val = val;
        this.group = group;
        this.color = color;
        this.riskScore = riskScore;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getVal() {
        return val;
    }

    public void setVal(Integer val) {
        this.val = val;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}

package com.emailforensics.backend.dto.graph;

public class GraphLinkDto {

    private String source;
    private String target;
    private String label;
    private String type;
    private Double strength;

    public GraphLinkDto() {
    }

    public GraphLinkDto(String source, String target, String label, String type, Double strength) {
        this.source = source;
        this.target = target;
        this.label = label;
        this.type = type;
        this.strength = strength;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
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

    public Double getStrength() {
        return strength;
    }

    public void setStrength(Double strength) {
        this.strength = strength;
    }
}

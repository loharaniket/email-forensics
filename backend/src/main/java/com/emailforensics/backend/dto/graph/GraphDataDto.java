package com.emailforensics.backend.dto.graph;

import java.util.ArrayList;
import java.util.List;

public class GraphDataDto {

    private List<GraphNodeDto> nodes = new ArrayList<>();
    private List<GraphLinkDto> links = new ArrayList<>();

    public GraphDataDto() {
    }

    public GraphDataDto(List<GraphNodeDto> nodes, List<GraphLinkDto> links) {
        this.nodes = nodes != null ? nodes : new ArrayList<>();
        this.links = links != null ? links : new ArrayList<>();
    }

    public List<GraphNodeDto> getNodes() {
        return nodes;
    }

    public void setNodes(List<GraphNodeDto> nodes) {
        this.nodes = nodes;
    }

    public List<GraphLinkDto> getLinks() {
        return links;
    }

    public void setLinks(List<GraphLinkDto> links) {
        this.links = links;
    }
}

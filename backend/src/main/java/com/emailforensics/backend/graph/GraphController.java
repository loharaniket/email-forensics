package com.emailforensics.backend.graph;

import com.emailforensics.backend.dto.graph.GraphDataDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/graph")
public class GraphController {

    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping("/evidence/{emailId}")
    public ResponseEntity<GraphDataDto> getEvidenceGraph(@PathVariable UUID emailId) {
        GraphDataDto response = graphService.getEvidenceGraph(emailId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/global")
    public ResponseEntity<GraphDataDto> getGlobalGraph(@RequestParam(defaultValue = "150") int limit) {
        GraphDataDto response = graphService.getGlobalGraph(limit);
        return ResponseEntity.ok(response);
    }
}

package com.jfc.gnn.model;

import lombok.Builder;
import lombok.Data;
import java.util.*;

@Data
@Builder
public class KnowledgeGraph {
    private List<GraphNode> nodes;
    private List<GraphEdge> edges;
    private Map<String, GraphNode> nodeIndex;
    private Map<String, List<GraphEdge>> adjacencyList;
    
    @Data
    @Builder
    public static class GraphNode {
        private String id;
        private String type;
        private Map<String, Object> properties;
        private float[] features;
        
        // For BOM specific properties
        private String itemCode;
        private String itemName;
        private String specification;
        private NodeCategory category;
    }
    
    @Data
    @Builder
    public static class GraphEdge {
        private String id;
        private String sourceId;
        private String targetId;
        private String edgeType;
        private Map<String, Object> properties;
        private float weight;
        
        // For BOM relationships
        private Double quantity;
        private String relationship; // "hasPart", "isComponentOf", etc.
    }
    
    public enum NodeCategory {
        PRODUCT,
        COMPONENT,
        MATERIAL,
        ASSEMBLY,
        HYDRAULIC_CYLINDER
    }
    
    /**
     * Get all triples for knowledge graph embedding
     */
    public List<Triple> getTriples() {
        List<Triple> triples = new ArrayList<>();
        
        for (GraphEdge edge : edges) {
            triples.add(new Triple(
                edge.getSourceId(),
                edge.getEdgeType(),
                edge.getTargetId()
            ));
        }
        
        return triples;
    }
    
    /**
     * Find neighbors of a node
     */
    public List<GraphNode> getNeighbors(String nodeId) {
        List<GraphNode> neighbors = new ArrayList<>();
        
        List<GraphEdge> edges = adjacencyList.get(nodeId);
        if (edges != null) {
            for (GraphEdge edge : edges) {
                String neighborId = edge.getTargetId().equals(nodeId) 
                    ? edge.getSourceId() 
                    : edge.getTargetId();
                GraphNode neighbor = nodeIndex.get(neighborId);
                if (neighbor != null) {
                    neighbors.add(neighbor);
                }
            }
        }
        
        return neighbors;
    }
}
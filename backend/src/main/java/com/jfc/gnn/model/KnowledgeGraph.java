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
     * Custom builder for KnowledgeGraph
     */
    public static KnowledgeGraphBuilder builder() {
        return new KnowledgeGraphBuilder();
    }
    
    /**
     * Custom builder class with addNode and addEdge methods
     */
    public static class KnowledgeGraphBuilder {
        private List<GraphNode> nodes = new ArrayList<>();
        private List<GraphEdge> edges = new ArrayList<>();
        private Map<String, GraphNode> nodeIndex = new HashMap<>();
        private Map<String, List<GraphEdge>> adjacencyList = new HashMap<>();
        
        public KnowledgeGraphBuilder nodes(List<GraphNode> nodes) {
            this.nodes = nodes != null ? nodes : new ArrayList<>();
            // Rebuild node index
            this.nodeIndex.clear();
            for (GraphNode node : this.nodes) {
                this.nodeIndex.put(node.getId(), node);
            }
            return this;
        }
        
        public KnowledgeGraphBuilder edges(List<GraphEdge> edges) {
            this.edges = edges != null ? edges : new ArrayList<>();
            // Rebuild adjacency list
            this.adjacencyList.clear();
            for (GraphEdge edge : this.edges) {
                this.adjacencyList.computeIfAbsent(edge.getSourceId(), k -> new ArrayList<>()).add(edge);
                this.adjacencyList.computeIfAbsent(edge.getTargetId(), k -> new ArrayList<>()).add(edge);
            }
            return this;
        }
        
        public KnowledgeGraphBuilder nodeIndex(Map<String, GraphNode> nodeIndex) {
            this.nodeIndex = nodeIndex != null ? nodeIndex : new HashMap<>();
            return this;
        }
        
        public KnowledgeGraphBuilder adjacencyList(Map<String, List<GraphEdge>> adjacencyList) {
            this.adjacencyList = adjacencyList != null ? adjacencyList : new HashMap<>();
            return this;
        }
        
        public KnowledgeGraphBuilder addNode(GraphNode node) {
            if (node != null) {
                nodes.add(node);
                nodeIndex.put(node.getId(), node);
                adjacencyList.putIfAbsent(node.getId(), new ArrayList<>());
            }
            return this;
        }
        
        public KnowledgeGraphBuilder addEdge(GraphEdge edge) {
            if (edge != null) {
                edges.add(edge);
                
                // Update adjacency list
                adjacencyList.computeIfAbsent(edge.getSourceId(), k -> new ArrayList<>()).add(edge);
                adjacencyList.computeIfAbsent(edge.getTargetId(), k -> new ArrayList<>()).add(edge);
            }
            return this;
        }
        
        public KnowledgeGraph build() {
            return new KnowledgeGraph(nodes, edges, nodeIndex, adjacencyList);
        }
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
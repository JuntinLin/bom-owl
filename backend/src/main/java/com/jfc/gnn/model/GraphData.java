package com.jfc.gnn.model;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GraphData {
    private float[][] nodeFeatures;
    private int[][] adjacencyMatrix;
    private float[][] edgeFeatures;
    private Map<String, Integer> nodeTypes;
    private Map<String, Integer> edgeTypes;
    private int numNodes;
    private int numEdges;
    
    // Node labels for training
    private int[] nodeLabels;
    
    // Edge information
    private List<EdgeInfo> edges;
    
    @Data
    @Builder
    public static class EdgeInfo {
        private int sourceIdx;
        private int targetIdx;
        private String edgeType;
        private Map<String, Object> attributes;
    }
}
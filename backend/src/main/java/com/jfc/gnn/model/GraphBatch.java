package com.jfc.gnn.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Batch of graph data for training
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphBatch {
    private float[][] nodeFeatures;  // Node feature matrix
    private float[][] adjacency;     // Adjacency matrix
    private float[][] labels;        // Target labels
    private int batchSize;           // Number of samples in batch
    private int numNodes;            // Number of nodes per graph
    
    /**
     * Get feature dimension
     */
    public int getFeatureDim() {
        return nodeFeatures != null && nodeFeatures.length > 0 && nodeFeatures[0] != null 
            ? nodeFeatures[0].length : 0;
    }
    
    /**
     * Get label dimension
     */
    public int getLabelDim() {
        return labels != null && labels.length > 0 && labels[0] != null 
            ? labels[0].length : 0;
    }
}
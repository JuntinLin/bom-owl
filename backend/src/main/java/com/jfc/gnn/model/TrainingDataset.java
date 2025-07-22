package com.jfc.gnn.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Dataset for training Graph Neural Networks
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingDataset {
    private List<GraphBatch> batches;
    private int totalSamples;
    private int batchSize;
    private String datasetName;
    private DatasetType type;
    private List<GraphBatch> testSet;

    
    public enum DatasetType {
        TRAIN,
        VALIDATION,
        TEST
    }
    
    /**
     * Get number of batches
     */
    public int getNumBatches() {
        return batches != null ? batches.size() : 0;
    }
    
    /**
     * Check if dataset is empty
     */
    public boolean isEmpty() {
        return batches == null || batches.isEmpty();
    }
}

package com.jfc.gnn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Options for GNN training
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingOptions {
    private boolean enableEarlyStopping;
    private int patience;
    private double minImprovement;
    private boolean saveCheckpoints;
    private String checkpointPath;
    private boolean verbose;
    private int logFrequency;
    private int epochs;
    
    /**
     * Get default training options
     */
    public static TrainingOptions getDefault() {
        return TrainingOptions.builder()
            .enableEarlyStopping(true)
            .patience(10)
            .minImprovement(0.001)
            .saveCheckpoints(false)
            .verbose(true)
            .logFrequency(10)
            .epochs(100)
            .build();
    }
}
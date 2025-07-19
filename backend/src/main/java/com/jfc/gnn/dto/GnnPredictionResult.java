package com.jfc.gnn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * Result of GNN prediction
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GnnPredictionResult {
    private List<ComponentPrediction> componentPredictions;
    private double overallConfidence;
    private String modelId;
    private long predictionTime;
    private Map<String, Double> featureImportance;
    private String predictionId;
    
    /**
     * Get top N component predictions by confidence
     */
    public List<ComponentPrediction> getTopPredictions(int n) {
        if (componentPredictions == null) return List.of();
        
        return componentPredictions.stream()
            .sorted((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()))
            .limit(n)
            .toList();
    }
    
    /**
     * Check if prediction was successful
     */
    public boolean isSuccessful() {
        return componentPredictions != null && !componentPredictions.isEmpty() 
            && overallConfidence > 0.0;
    }
}
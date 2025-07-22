package com.jfc.gnn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
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
    private List<PredictedComponent> componentPredictions;
    private double overallConfidence;
    private String modelId;
    private long predictionTime;
    private Map<String, Double> featureImportance;
    private String predictionId;
    private Map<String, Object> metrics; 
    
    /**
     * Get top N component predictions by confidence
     */
    public List<PredictedComponent> getTopPredictions(int n) {
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
    
    /**
     * Get metrics (with null safety)
     */
    public Map<String, Object> getMetrics() {
        return metrics != null ? metrics : new HashMap<>();
    }
    
    /**
     * Get a specific component prediction by component ID
     */
    public PredictedComponent getComponentPrediction(String componentId) {
        if (componentPredictions == null || componentId == null) {
            return null;
        }
        
        return componentPredictions.stream()
            .filter(pred -> pred.getComponentCode() != null && 
                           pred.getComponentCode().equals(componentId))
            .findFirst()
            .orElse(null);
    }
}
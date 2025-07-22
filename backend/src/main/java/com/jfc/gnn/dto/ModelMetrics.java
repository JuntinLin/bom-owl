package com.jfc.gnn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;
import java.util.Date;

/**
 * Model performance metrics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelMetrics {
    private String modelId;
    private String modelName;
    private String modelType;
    private double accuracy;
    private double precision;
    private double recall;
    private double f1Score;
    private double loss;
    private Map<String, Double> componentTypeMetrics;
    private Map<String, Double> confusionMatrix;
    private int totalPredictions;
    private int correctPredictions;
    private long inferenceTime;
    private Date lastEvaluated;
    private Map<String, Object> additionalMetrics;
    
    /**
     * Calculate accuracy percentage
     */
    public double getAccuracyPercentage() {
        return accuracy * 100;
    }
    
    /**
     * Get performance summary
     */
    public String getPerformanceSummary() {
        return String.format(
            "Model %s - Accuracy: %.2f%%, Precision: %.2f%%, Recall: %.2f%%, F1: %.2f%%",
            modelId, accuracy * 100, precision * 100, recall * 100, f1Score * 100
        );
    }
}
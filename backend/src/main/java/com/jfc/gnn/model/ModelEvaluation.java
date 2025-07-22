package com.jfc.gnn.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelEvaluation {
    private double accuracy;
    private double precision;
    private double recall;
    private double f1Score;
    private double loss;
    private Map<String, Double> classMetrics;
    private Map<String, Object> additionalMetrics;
    private int totalSamples;
    private int correctPredictions;
}
package com.jfc.gnn.dto;

import com.jfc.gnn.config.GnnModelConfig;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//=====================================================================
//9. PredictionOptions.java - Options for prediction
//=====================================================================

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionOptions {
 private double threshold;                    // Confidence threshold
 private int maxPredictions;                 // Maximum number of predictions
 private GnnModelConfig.ModelType modelType; // Model type to use
 private GnnModelConfig config;              // Model configuration
 private boolean includeAlternatives;        // Include alternative components
 private boolean calculateQuantities;        // Calculate recommended quantities
 
 public static PredictionOptions defaultOptions() {
     return PredictionOptions.builder()
         .threshold(0.5)
         .maxPredictions(100)
         .modelType(GnnModelConfig.ModelType.GCN)
         .includeAlternatives(true)
         .calculateQuantities(true)
         .build();
 }
}
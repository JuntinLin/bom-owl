package com.jfc.gnn.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//=====================================================================
//7. GnnTrainingResult.java - Enhanced training result class
//=====================================================================

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GnnTrainingResult {
 private boolean success;
 private String modelId;
 private long trainingTime;         // Training time in milliseconds
 private double finalLoss;          // Final training loss
 private double bestValidationAccuracy; // Best validation accuracy achieved
 private double accuracy;           // Final test accuracy
 private double precision;          // Precision metric
 private double recall;             // Recall metric
 private double f1Score;            // F1 score
 private List<Double> epochLosses;  // Loss for each epoch
 private int componentCount;        // Number of components in vocabulary
 private String error;              // Error message if training failed
 
 // Additional metrics
 private Map<String, Double> componentTypeAccuracy; // Accuracy per component type
 private List<String> trainingLog;                 // Training log messages
 private int totalEpochs;                          // Total epochs trained
 private boolean earlyStoppedEpochs;               // Whether early stopping was triggered
 private Map<String, Object> hyperparameters;     // Hyperparameters used
 
 /**
  * Calculate training statistics
  */
 public Map<String, Object> getTrainingStatistics() {
     Map<String, Object> stats = new HashMap<>();
     stats.put("success", success);
     stats.put("trainingTimeSeconds", trainingTime / 1000.0);
     stats.put("finalLoss", finalLoss);
     stats.put("bestValidationAccuracy", bestValidationAccuracy);
     stats.put("totalEpochs", totalEpochs);
     stats.put("earlyStoppedEpochs", earlyStoppedEpochs);
     stats.put("componentVocabularySize", componentCount);
     
     if (epochLosses != null && !epochLosses.isEmpty()) {
         stats.put("initialLoss", epochLosses.get(0));
         stats.put("lossDrop", epochLosses.get(0) - finalLoss);
     }
     
     return stats;
 }
 
 /**
  * Get training summary
  */
 public String getTrainingSummary() {
     if (!success) {
         return "Training failed: " + error;
     }
     
     return String.format(
         "Training completed successfully in %.2f seconds. " +
         "Final accuracy: %.4f, Best validation: %.4f, " +
         "Component vocabulary: %d items",
         trainingTime / 1000.0, accuracy, bestValidationAccuracy, componentCount
     );
 }
}
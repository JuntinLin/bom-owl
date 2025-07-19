package com.jfc.gnn.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.jfc.gnn.config.GnnModelConfig;
import com.jfc.gnn.dto.GnnTrainingResult;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
//=====================================================================
//8. TrainedGnnModel.java - Enhanced trained model class  
//=====================================================================

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainedGnnModel {
 private Object model;              // The actual trained model (GCN or GAT)
 private GnnModelConfig config;     // Model configuration used
 private long trainingTime;         // Training time in milliseconds
 private String version;            // Model version
 private Object evaluation;         // Evaluation results
 private String modelType;          // Model type (GCN, GAT, etc.)
 private Date createdAt;           // When model was created
 private String description;        // Model description
 
 /**
  * Get model performance summary
  */
 public Map<String, Object> getPerformanceSummary() {
     Map<String, Object> summary = new HashMap<>();
     summary.put("modelType", modelType);
     summary.put("version", version);
     summary.put("createdAt", createdAt);
     summary.put("trainingTimeSeconds", trainingTime / 1000.0);
     
     if (evaluation instanceof GnnTrainingResult) {
         GnnTrainingResult result = (GnnTrainingResult) evaluation;
         summary.put("accuracy", result.getAccuracy());
         summary.put("precision", result.getPrecision());
         summary.put("recall", result.getRecall());
         summary.put("f1Score", result.getF1Score());
     }
     
     return summary;
 }
}
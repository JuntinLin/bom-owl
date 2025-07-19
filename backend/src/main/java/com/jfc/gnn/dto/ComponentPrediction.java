package com.jfc.gnn.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.jfc.gnn.model.BOMComponent;

//=====================================================================
//10. ComponentPrediction.java - Single component prediction result
//=====================================================================

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentPrediction {
 private int componentIndex;        // Index in vocabulary
 private String componentCode;      // Component code
 private String componentName;      // Component name
 private double confidence;         // Prediction confidence [0,1]
 private int predictedQuantity;     // Predicted quantity needed
 private BOMComponent.ComponentType type; // Component type
 private List<String> alternatives; // Alternative component codes
 private Map<String, Object> properties; // Additional properties
 
 /**
  * Get confidence level as string
  */
 public String getConfidenceLevel() {
     if (confidence >= 0.9) return "VERY_HIGH";
     if (confidence >= 0.8) return "HIGH";
     if (confidence >= 0.7) return "MEDIUM";
     if (confidence >= 0.5) return "LOW";
     return "VERY_LOW";
 }
}
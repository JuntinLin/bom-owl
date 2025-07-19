package com.jfc.gnn.model;
import java.util.List;

import org.nd4j.linalg.api.ndarray.INDArray;

//=====================================================================
//4. BOMTrainingBatch.java - Batch of training examples
//=====================================================================
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BOMTrainingBatch {
 private INDArray features;    // Feature matrix [batchSize, featureSize]
 private INDArray labels;      // Label matrix [batchSize, vocabularySize]
 private int size;             // Number of examples in batch
 private List<BOMTrainingExample> examples; // Original examples for reference
 
 /**
  * Get feature dimensions
  */
 public int getFeatureSize() {
     return features != null ? features.columns() : 0;
 }
 
 /**
  * Get label dimensions  
  */
 public int getLabelSize() {
     return labels != null ? labels.columns() : 0;
 }
}
package com.jfc.gnn.model;
import java.util.List;
import java.util.Map;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import com.jfc.owl.model.bom.BomComponent;

//=====================================================================
//3. BOMTrainingExample.java - Single training example
//=====================================================================
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BOMTrainingExample {
 private String masterItemCode;
 private ProductSpecifications specifications;
 private List<BomComponent> components;
 private INDArray labels; // Binary vector for component presence
 
 /**
  * Get or create labels from components
  */
 public INDArray getLabels() {
     if (labels == null) {
         labels = createLabelsFromComponents();
     }
     return labels;
 }
 
 /**
  * Create binary label vector from components
  */
 private INDArray createLabelsFromComponents() {
     int maxComponents = 1000; // Adjust based on vocabulary size
     float[] labelArray = new float[maxComponents];
     
     for (BomComponent component : components) {
         // Simple hash-based indexing - in production, use proper vocabulary mapping
         int index = Math.abs(component.getComponentId().hashCode()) % maxComponents;
         labelArray[index] = 1.0f;
     }
     
     return Nd4j.create(labelArray);
 }
 
 /**
  * Set labels with proper vocabulary mapping
  */
 public void setLabelsFromVocabulary(Map<String, Integer> componentToIndex, int vocabularySize) {
     float[] labelArray = new float[vocabularySize];
     
     for (BomComponent component : components) {
         Integer index = componentToIndex.get(component.getComponentId());
         if (index != null && index < vocabularySize) {
             labelArray[index] = 1.0f;
         }
     }
     
     this.labels = Nd4j.create(labelArray);
 }
}
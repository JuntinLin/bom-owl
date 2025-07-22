package com.jfc.gnn.model;
import lombok.*;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import com.jfc.owl.model.bom.BomComponent;

import java.util.*;
//=====================================================================
//2. BOMTrainingDataset.java - New class for BOM training data
//=====================================================================
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BOMTrainingDataset {
    private List<BOMTrainingExample> examples;
    private List<BOMTrainingExample> validationSet;
    private List<BOMTrainingExample> testSet;
    private List<String> componentVocabulary;
    private Map<String, Integer> componentToIndex;
    
    public int getSize() {
        return examples != null ? examples.size() : 0;
    }
    
    public void shuffle() {
        if (examples != null) {
            Collections.shuffle(examples);
        }
    }
    
    /**
     * Create batches of training data
     */
    public List<BOMTrainingBatch> getBatches(int batchSize) {
        List<BOMTrainingBatch> batches = new ArrayList<>();
        
        if (examples == null || examples.isEmpty()) {
            return batches;
        }
        
        for (int i = 0; i < examples.size(); i += batchSize) {
            int end = Math.min(i + batchSize, examples.size());
            List<BOMTrainingExample> batchExamples = examples.subList(i, end);
            
            // Convert to matrices
            INDArray features = createFeatureMatrix(batchExamples);
            INDArray labels = createLabelMatrix(batchExamples);
            
            batches.add(BOMTrainingBatch.builder()
                .features(features)
                .labels(labels)
                .size(batchExamples.size())
                .examples(batchExamples)
                .build());
        }
        
        return batches;
    }
    
    /**
     * Create feature matrix from training examples
     */
    private INDArray createFeatureMatrix(List<BOMTrainingExample> examples) {
        int batchSize = examples.size();
        int featureSize = 20; // Fixed feature size for hydraulic cylinder specs
        
        float[][] matrix = new float[batchSize][featureSize];
        
        for (int i = 0; i < examples.size(); i++) {
            BOMTrainingExample example = examples.get(i);
            float[] features = extractFeatures(example.getSpecifications());
            System.arraycopy(features, 0, matrix[i], 0, Math.min(features.length, featureSize));
        }
        
        return Nd4j.create(matrix);
    }
    
    /**
     * Create label matrix from training examples
     */
    private INDArray createLabelMatrix(List<BOMTrainingExample> examples) {
        int batchSize = examples.size();
        int numComponents = componentVocabulary != null ? componentVocabulary.size() : 1000;
        
        float[][] matrix = new float[batchSize][numComponents];
        
        for (int i = 0; i < examples.size(); i++) {
            BOMTrainingExample example = examples.get(i);
            
            // Create binary vector for components
            for (BomComponent component : example.getComponents()) {
                Integer index = componentToIndex.get(component.getComponentId());
                if (index != null && index < numComponents) {
                    matrix[i][index] = 1.0f;
                }
            }
        }
        
        return Nd4j.create(matrix);
    }
    
    /**
     * Extract features from ProductSpecifications
     */
    private float[] extractFeatures(ProductSpecifications specs) {
        float[] features = new float[20];
        int idx = 0;
        
        // Basic numerical specifications
        features[idx++] = parseFloat(specs.getBore(), 0f);
        features[idx++] = parseFloat(specs.getStroke(), 0f);
        features[idx++] = parseFloat(specs.getSeries(), 0f);
        features[idx++] = parseFloat(specs.getType(), 0f);
        
        // One-hot encoding for rod end type
        String rodEndType = specs.getRodEndType();
        features[idx++] = "Y".equals(rodEndType) ? 1.0f : 0.0f;
        features[idx++] = "I".equals(rodEndType) ? 1.0f : 0.0f;
        features[idx++] = "E".equals(rodEndType) ? 1.0f : 0.0f;
        features[idx++] = "F".equals(rodEndType) ? 1.0f : 0.0f;
        
        // One-hot encoding for installation type
        String installation = specs.getInstallation();
        features[idx++] = "FA".equals(installation) ? 1.0f : 0.0f;
        features[idx++] = "CA".equals(installation) ? 1.0f : 0.0f;
        features[idx++] = "CB".equals(installation) ? 1.0f : 0.0f;
        features[idx++] = "TC".equals(installation) ? 1.0f : 0.0f;
        
        // Derived features
        float bore = parseFloat(specs.getBore(), 1f);
        float stroke = parseFloat(specs.getStroke(), 1f);
        features[idx++] = stroke / Math.max(bore, 1f); // Stroke-to-bore ratio
        features[idx++] = bore * stroke; // Volume approximation
        features[idx++] = Math.min(bore / 100f, 1f); // Normalized bore
        features[idx++] = Math.min(stroke / 1000f, 1f); // Normalized stroke
        
        // Fill remaining with zeros
        while (idx < features.length) {
            features[idx++] = 0f;
        }
        
        return features;
    }
    
    private float parseFloat(String value, float defaultValue) {
        try {
            return value != null && !value.isEmpty() ? Float.parseFloat(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Initialize component vocabulary mapping
     */
    public void initializeComponentMapping() {
        if (componentVocabulary != null) {
            componentToIndex = new HashMap<>();
            for (int i = 0; i < componentVocabulary.size(); i++) {
                componentToIndex.put(componentVocabulary.get(i), i);
            }
        }
    }
}
package com.jfc.gnn.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;
import java.util.stream.Collectors;
import com.jfc.owl.model.bom.BomComponent;
import com.jfc.owl.model.enums.ProductType;

import java.util.List;

@Data
@Builder
public class GnnBomGenerationResult {
    private boolean success;
    private String error;
    private String newItemCode;
    private BomStructure generatedBom;
    private Map<String, Double> confidenceScores;
    private PredictionMetrics predictionMetrics;
    private long generationTime;
    
    @Data
    @Builder
    public static class BomStructure {
        private String masterItemCode;
        private String masterItemName;
        private List<BomComponent> components;  // Using the model class
        private Map<String, List<String>> componentRelationships;
        private AssemblySequence assemblySequence;
        private double structureConfidence;
        
        /**
         * Get total component count
         */
        public int getTotalComponentCount() {
            return components != null ? components.size() : 0;
        }
        
        /**
         * Get components by type
         */
        public List<BomComponent> getComponentsByProductType(ProductType type) {
            return components.stream()
                .filter(c -> c.getProductType() == type)
                .collect(Collectors.toList());
        }
        
        /**
         * Calculate total BOM cost (if prices are available)
         */
        public double calculateTotalCost() {
            return components.stream()
                .mapToDouble(c -> c.getQuantity() * c.getUnitPrice())
                .sum();
        }
    }
    
        
    @Data
    @Builder
    public static class PredictionMetrics {
        private double accuracy;
        private double precision;
        private double recall;
        private double f1Score;
        private Map<String, Double> componentTypeMetrics;
        private long inferenceTime;
        private int totalPredictions;
    }
    
    @Data
    @Builder
    public static class AssemblySequence {
        private List<String> steps;
        private Map<String, List<String>> dependencies;
    }
}
package com.jfc.gnn.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;
import java.util.stream.Collectors;
import com.jfc.gnn.model.BOMComponent;
import com.jfc.gnn.model.BOMComponent.ComponentType;

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
        private List<BOMComponent> components;  // Using the model class
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
        public List<BOMComponent> getComponentsByType(ComponentType type) {
            return components.stream()
                .filter(c -> c.getType() == type)
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
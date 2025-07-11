package com.jfc.gnn.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.type.ComponentType;

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
        private List<BomComponent> components;
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
        public List<BomComponent> getComponentsByType(ComponentType type) {
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
    public static class BomComponent {
        private String code;
        private String name;
        private String specification;
        private double quantity;
        private String unit;
        private ComponentType type;
        private double confidence;
        private List<String> alternativeComponents;
        private Map<String, Object> predictedProperties;
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
}
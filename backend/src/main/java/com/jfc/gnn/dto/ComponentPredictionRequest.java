package com.jfc.gnn.dto;

import com.jfc.gnn.model.ProductSpecifications;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentPredictionRequest {
    private String productCode;
    private String modelId;
    private ProductSpecifications specifications;
    private Integer maxComponents;
    private ComponentPredictionType type;   // Type of prediction needed
    private String targetComponentType;     // Specific component type if needed
    
    public enum ComponentPredictionType {
        ALL_COMPONENTS,         // Predict all components
        SPECIFIC_TYPE,         // Predict specific component type (e.g., only seals)
        ALTERNATIVES,          // Find alternative components
        MISSING_COMPONENTS     // Find missing components in partial BOM
    }
}
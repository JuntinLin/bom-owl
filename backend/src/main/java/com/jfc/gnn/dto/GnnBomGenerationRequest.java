package com.jfc.gnn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

import com.jfc.gnn.model.ProductSpecifications;

/**
 * Request for GNN-based BOM generation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GnnBomGenerationRequest {
    private String newItemCode;
    private String newItemDescription;
    private ProductSpecifications specifications;
    private PredictionOptions predictionOptions;
    private Map<String, Object> additionalParameters;
    
    /**
     * Validation method
     */
    public boolean isValid() {
        return newItemCode != null && !newItemCode.isEmpty() &&
               specifications != null;
    }
}
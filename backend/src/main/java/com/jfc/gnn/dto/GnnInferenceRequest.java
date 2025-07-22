package com.jfc.gnn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

import com.jfc.gnn.model.ProductSpecifications;

/**
 * WebSocket request for GNN inference
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GnnInferenceRequest {
    private String requestId;
    private String modelId;
    private String productCode;
    private ProductSpecifications specifications;
    private Map<String, Object> parameters;
    private boolean streamProgress;
    
    /**
     * Generate unique request ID if not provided
     */
    public String getRequestId() {
        if (requestId == null || requestId.isEmpty()) {
            requestId = "REQ_" + System.currentTimeMillis();
        }
        return requestId;
    }
}
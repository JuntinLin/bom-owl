package com.jfc.gnn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictedComponent {
    private String componentCode;
    private String componentName;
    private Double quantity;
    private String unit;
    private Double confidence;
    private String predictedBy;
}
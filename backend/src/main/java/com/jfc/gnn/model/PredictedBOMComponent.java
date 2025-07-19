package com.jfc.gnn.model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import java.util.List;
import java.util.Map;

/**
 * Extended BOMComponent with GNN prediction-specific fields
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class PredictedBOMComponent extends BOMComponent {
	private double confidence;
    private List<String> alternativeComponents;
    private Map<String, Object> predictedProperties;
    
    /**
     * Create a PredictedBOMComponent from a base BOMComponent
     */
    public static PredictedBOMComponent fromBOMComponent(BOMComponent component, 
                                                         double confidence,
                                                         List<String> alternatives,
                                                         Map<String, Object> predictedProps) {
        return PredictedBOMComponent.builder()
            .code(component.getCode())
            .name(component.getName())
            .specification(component.getSpecification())
            .quantity(component.getQuantity())
            .unitPrice(component.getUnitPrice())
            .unit(component.getUnit())
            .type(component.getType())
            .series(component.getSeries())
            .properties(component.getProperties())
            .confidence(confidence)
            .alternativeComponents(alternatives)
            .predictedProperties(predictedProps)
            .build();
    }
}

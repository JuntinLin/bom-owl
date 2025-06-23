package com.jfc.owl.model.bom;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a component within a Bill of Materials (BOM) structure. Each
 * component references an item and includes usage details such as quantity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BomComponent {

	/**
	 * Identifier of the component item
	 */
	private String componentId;

	/**
	 * Type of the component (M = Material, P = Part)
	 */
	private String componentType;

	/**
	 * Quantity of the component required in the BOM
	 */
	private double quantity;

	/**
	 * Unit of measurement for the quantity
	 */
	private String unit;

	/**
	 * Dimensions or size specifications for this component
	 */
	private String dimension;

	/**
	 * Position or location of the component in the assembly
	 */
	private String position;

	/**
	 * Additional notes or comments about the component
	 */
	private String note;

	/**
	 * Line number or sequence number of this component in the BOM
	 */
	private int lineNumber;

	/**
	 * Indicates whether this component is optional or required
	 */
	private boolean optional;

	/**
	 * Alternate component that can be used as a substitute
	 */
	private String alternateComponent;

	/**
	 * Operation or process where this component is used
	 */
	private String operation;

	/**
	 * Scrap factor or waste percentage
	 */
	private double scrapFactor;

	/**
	 * Returns the effective quantity including scrap factor
	 * 
	 * @return The actual quantity needed considering scrap
	 */
	public double getEffectiveQuantity() {
		return quantity * (1 + scrapFactor / 100);
	}

	/**
	 * Checks if this component is a material
	 * 
	 * @return true if this is a material, false if it's a part
	 */
	public boolean isMaterial() {
		return "M".equals(componentType);
	}

	/**
	 * Checks if this component is a part
	 * 
	 * @return true if this is a part, false if it's a material
	 */
	public boolean isPart() {
		return "P".equals(componentType);
	}

	/**
	 * Creates a copy of this component with the same properties
	 * 
	 * @return A new BomComponent with the same values
	 */
	public BomComponent copy() {
		BomComponent copy = new BomComponent();
		copy.setComponentId(this.componentId);
		copy.setComponentType(this.componentType);
		copy.setQuantity(this.quantity);
		copy.setUnit(this.unit);
		copy.setDimension(this.dimension);
		copy.setPosition(this.position);
		copy.setNote(this.note);
		copy.setLineNumber(this.lineNumber);
		copy.setOptional(this.optional);
		copy.setAlternateComponent(this.alternateComponent);
		copy.setOperation(this.operation);
		copy.setScrapFactor(this.scrapFactor);
		return copy;
	}
}
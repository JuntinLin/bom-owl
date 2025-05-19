package com.jfc.owl.model.bom;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Represents a Bill of Materials (BOM) structure. A BOM defines the
 * hierarchical relationship between a parent item and its components.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bom {

	/**
	 * Unique identifier for the BOM
	 */
	private String id;

	/**
	 * Identifier of the parent item (usually a part or product)
	 */
	private String parentItemId;

	/**
	 * Type of the parent item (F = Finished product, P = Part)
	 */
	private String parentItemType;

	/**
	 * Date from which this BOM becomes effective
	 */
	private Date effectiveDate;

	/**
	 * Date until which this BOM is effective (null for indefinite)
	 */
	private Date expiryDate;

	/**
	 * Status of the BOM (Y = Active, N = Inactive)
	 */
	private String status;

	/**
	 * List of components that make up this BOM
	 */
	private List<BomComponent> components = new ArrayList<>();

	/**
	 * Description or notes about this BOM
	 */
	private String description;

	/**
	 * The user who created this BOM
	 */
	private String createdBy;

	/**
	 * Date when this BOM was created
	 */
	private Date creationDate;

	/**
	 * The user who last modified this BOM
	 */
	private String lastModifiedBy;

	/**
	 * Date when this BOM was last modified
	 */
	private Date lastModifiedDate;

	/**
	 * Version number of this BOM
	 */
	private String version;

	/**
	 * Checks if this BOM is active
	 * 
	 * @return true if the BOM is active, false otherwise
	 */
	public boolean isActive() {
		return "Y".equals(status);
	}

	/**
	 * Adds a component to this BOM
	 * 
	 * @param component The component to add
	 */
	public void addComponent(BomComponent component) {
		if (components == null) {
			components = new ArrayList<>();
		}
		components.add(component);
	}

	/**
	 * Gets the total number of components in this BOM
	 * 
	 * @return The number of components
	 */
	public int getComponentCount() {
		return components != null ? components.size() : 0;
	}

	/**
	 * Checks if this BOM is for a finished product
	 * 
	 * @return true if this is a product BOM, false if it's a part BOM
	 */
	public boolean isProductBom() {
		return "F".equals(parentItemType);
	}
}
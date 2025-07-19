package com.jfc.gnn.model;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Data;
import lombok.NoArgsConstructor;

//=====================================================================
//5. BOMComponent.java - Enhanced component class
//=====================================================================

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BOMComponent {
	private String code; // Component code
	private String name; // Component name
	private String specification; // Component specification
	private double quantity; // Required quantity
    private double unitPrice;      // Unit price
	private String unit; // Unit of measurement
	private ComponentType type; // Component type enum
	private String series; // Component series
	private Map<String, Object> properties; // Additional properties

	public enum ComponentType {
		BARREL("Cylinder Barrel"), PISTON("Piston"), PISTON_ROD("Piston Rod"), END_CAP("End Cap"),
		SEAL("Sealing Component"), BUSHING("Bushing"), FASTENER("Fastener"), GASKET("Gasket"), OTHER("Other Component");

		private final String description;

		ComponentType(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}
}
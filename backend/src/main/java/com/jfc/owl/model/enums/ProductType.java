package com.jfc.owl.model.enums;

public enum ProductType {
	BARREL("Cylinder Barrel"), PISTON("Piston"), PISTON_ROD("Piston Rod"), END_CAP("End Cap"),
	SEAL("Sealing Component"), BUSHING("Bushing"), FASTENER("Fastener"), GASKET("Gasket"), OTHER("Other Component");

	private final String description;

	ProductType(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}

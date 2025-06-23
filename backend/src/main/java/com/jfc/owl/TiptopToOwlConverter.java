package com.jfc.owl;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.*;

import com.jfc.rdb.tiptop.entity.BmaFile;
import com.jfc.rdb.tiptop.entity.BmbFile;
import com.jfc.rdb.tiptop.entity.ImaFile;

import java.util.*;

/**
 * Converts TiptopERP database entities to OWL ontology
 */
public class TiptopToOwlConverter {
	private static final String BASE_URI = "http://www.jfc.com/tiptop/ontology#";
	private OntModel ontModel;

	// Ontology classes
	private OntClass materialClass;
	private OntClass masterItemClass;
	private OntClass componentItemClass;
	private OntClass bomClass;
	private OntClass cylinderClass;

	// Ontology properties
	private ObjectProperty hasMasterItem;
	private ObjectProperty hasComponentItem;
	private ObjectProperty isUsedIn;
	private DatatypeProperty itemCode;
	private DatatypeProperty itemName;
	private DatatypeProperty itemSpec;
	private DatatypeProperty effectiveDate;
	private DatatypeProperty expiryDate;
	private DatatypeProperty quantity;
	private DatatypeProperty characteristicCode;

	// New properties for masterItems starting with 3 or 4
	private DatatypeProperty bore;
	private DatatypeProperty stroke;
	private DatatypeProperty rodeEndType;
	private DatatypeProperty series;
	private DatatypeProperty type;
	private DatatypeProperty installation;
	private DatatypeProperty shaftEndJoin;
	private DatatypeProperty accessories;

	public TiptopToOwlConverter() {
		initializeOntology();
	}

	private void initializeOntology() {
		// Create ontology model
		ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);

		// Define classes
		materialClass = ontModel.createClass(BASE_URI + "Material");
		masterItemClass = ontModel.createClass(BASE_URI + "MasterItem");
		componentItemClass = ontModel.createClass(BASE_URI + "ComponentItem");
		bomClass = ontModel.createClass(BASE_URI + "BillOfMaterial");
		cylinderClass = ontModel.createClass(BASE_URI + "Cylinder");

		// Define class hierarchy
		masterItemClass.addSuperClass(materialClass);
		componentItemClass.addSuperClass(materialClass);
		cylinderClass.addSuperClass(materialClass);

		// Define properties
		itemCode = ontModel.createDatatypeProperty(BASE_URI + "itemCode");
		itemCode.addDomain(materialClass);
		itemCode.addRange(XSD.xstring);

		itemName = ontModel.createDatatypeProperty(BASE_URI + "itemName");
		itemName.addDomain(materialClass);
		itemName.addRange(XSD.xstring);

		itemSpec = ontModel.createDatatypeProperty(BASE_URI + "itemSpec");
		itemSpec.addDomain(materialClass);
		itemSpec.addRange(XSD.xstring);

		hasMasterItem = ontModel.createObjectProperty(BASE_URI + "hasMasterItem");
		hasMasterItem.addDomain(bomClass);
		hasMasterItem.addRange(masterItemClass);

		hasComponentItem = ontModel.createObjectProperty(BASE_URI + "hasComponentItem");
		hasComponentItem.addDomain(bomClass);
		hasComponentItem.addRange(componentItemClass);

		isUsedIn = ontModel.createObjectProperty(BASE_URI + "isUsedIn");
		isUsedIn.addDomain(componentItemClass);
		isUsedIn.addRange(masterItemClass);

		effectiveDate = ontModel.createDatatypeProperty(BASE_URI + "effectiveDate");
		effectiveDate.addDomain(bomClass);
		effectiveDate.addRange(XSD.date);

		expiryDate = ontModel.createDatatypeProperty(BASE_URI + "expiryDate");
		expiryDate.addDomain(bomClass);
		expiryDate.addRange(XSD.date);

		quantity = ontModel.createDatatypeProperty(BASE_URI + "quantity");
		quantity.addDomain(bomClass);
		quantity.addRange(XSD.decimal);

		characteristicCode = ontModel.createDatatypeProperty(BASE_URI + "characteristicCode");
		characteristicCode.addDomain(bomClass);
		characteristicCode.addRange(XSD.xstring);

		// Initialize new properties for masterItems with codes starting with 3 or 4
		bore = ontModel.createDatatypeProperty(BASE_URI + "bore");
		bore.addDomain(masterItemClass);
		bore.addRange(XSD.xstring);

		stroke = ontModel.createDatatypeProperty(BASE_URI + "stroke");
		stroke.addDomain(masterItemClass);
		stroke.addRange(XSD.xstring);

		rodeEndType = ontModel.createDatatypeProperty(BASE_URI + "rodeEndType");
		rodeEndType.addDomain(masterItemClass);
		rodeEndType.addRange(XSD.xstring);

		series = ontModel.createDatatypeProperty(BASE_URI + "series");
		series.addDomain(masterItemClass);
		series.addRange(XSD.xstring);

		type = ontModel.createDatatypeProperty(BASE_URI + "type");
		type.addDomain(masterItemClass);
		type.addRange(XSD.xstring);

		installation = ontModel.createDatatypeProperty(BASE_URI + "installation");
		installation.addDomain(masterItemClass);
		installation.addRange(XSD.xstring);

		shaftEndJoin = ontModel.createDatatypeProperty(BASE_URI + "shaftEndJoin");
		shaftEndJoin.addDomain(masterItemClass);
		shaftEndJoin.addRange(XSD.xstring);

		accessories = ontModel.createDatatypeProperty(BASE_URI + "accessories");
		accessories.addDomain(masterItemClass);
		accessories.addRange(XSD.xstring);

	}

	/**
	 * Converts TiptopERP ImaFile to OWL Material Individual
	 */
	public Individual convertImaToMaterial(ImaFile ima) {
		// Sanitize the material code for URI
		String sanitizedCode = sanitizeForUri(ima.getIma01());
		Individual material = materialClass.createIndividual(BASE_URI + "Material_" + sanitizedCode);

		material.addProperty(itemCode, ima.getIma01());
		if (ima.getIma02() != null) {
			material.addProperty(itemName, ima.getIma02());
		}
		if (ima.getIma021() != null) {
			material.addProperty(itemSpec, ima.getIma021());
		}

		return material;
	}

	/**
	 * Converts TiptopERP BOM (BmaFile & BmbFile) to OWL BOM representation
	 */
	public void convertBomStructure(BmaFile bma, List<BmbFile> bmbs, Map<String, Individual> materialMap) {
		// Sanitize codes
		String sanitizedMasterCode = sanitizeForUri(bma.getId().getBma01());

		// Create master item individual
		Individual masterItem = materialMap.get(bma.getId().getBma01());
		if (masterItem == null) {
			// If master item's material doesn't exist yet, create a placeholder
			masterItem = masterItemClass.createIndividual(BASE_URI + "MasterItem_" + sanitizedMasterCode);
			masterItem.addProperty(itemCode, bma.getId().getBma01());
			materialMap.put(bma.getId().getBma01(), masterItem);
		} else {
			// Add master item type to existing material
			masterItem.addRDFType(masterItemClass);
		}

		// Add characteristic code (bma06) to master item
		if (bma.getId().getBma06() != null) {
			masterItem.addProperty(characteristicCode, bma.getId().getBma06());
		}

		// Add new properties for masterItems with codes starting with 3 or 4
		String masterItemCode = bma.getId().getBma01();
		if (masterItemCode != null && masterItemCode.length() >= 15
				&& (masterItemCode.startsWith("3") || masterItemCode.startsWith("4"))) {

			// Extract properties from masterItemCode
			if (masterItemCode.length() >= 8) {
				String boreValue = masterItemCode.substring(5, 8);
				masterItem.addProperty(bore, boreValue);
			}

			if (masterItemCode.length() >= 15) {
				String strokeValue = masterItemCode.substring(10, 14);
				masterItem.addProperty(stroke, strokeValue);

				// RodeEndType is the 15th character
				String rodeEndValue = masterItemCode.substring(14, 15);
				masterItem.addProperty(rodeEndType, rodeEndValue);

				// Accessories is the 16th character.
				String accessories = masterItemCode.substring(15, 16);

			}

			if (masterItemCode.length() >= 5) {
				String seriesValue = masterItemCode.substring(2, 4);
				masterItem.addProperty(series, seriesValue);

				// Type is the 5th character
				String typeValue = masterItemCode.substring(4, 5);
				masterItem.addProperty(type, typeValue);
			}
		}

		// Process each component
		for (BmbFile bmb : bmbs) {
			// Sanitize component and sequence codes
			String sanitizedCompCode = sanitizeForUri(bmb.getId().getBmb03());
			String sanitizedSeqCode = sanitizeForUri(String.valueOf(bmb.getId().getBmb02()));
			// Create component item individual
			Individual componentItem = materialMap.get(bmb.getId().getBmb03());
			if (componentItem == null) {
				// If component item's material doesn't exist yet, create a placeholder
				componentItem = componentItemClass.createIndividual(BASE_URI + "ComponentItem_" + sanitizedCompCode);
				componentItem.addProperty(itemCode, bmb.getId().getBmb03());
				materialMap.put(bmb.getId().getBmb03(), componentItem);
			} else {
				// Add component item type to existing material
				componentItem.addRDFType(componentItemClass);
			}

			// Create BOM relationship
			Individual bomRelation = bomClass.createIndividual(
					BASE_URI + "BOM_" + sanitizedMasterCode + "_" + sanitizedSeqCode + "_" + sanitizedCompCode);

			// Link BOM to master and component
			bomRelation.addProperty(hasMasterItem, masterItem);
			bomRelation.addProperty(hasComponentItem, componentItem);

			// Link component to master (inverse relationship)
			componentItem.addProperty(isUsedIn, masterItem);

			// Add BOM details
			if (bmb.getId().getBmb04() != null) {
				try {
					// Convert java.sql.Date to proper XSD date format
					java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
					String dateStr = sdf.format(bmb.getId().getBmb04());
					bomRelation.addProperty(effectiveDate,
							ResourceFactory.createTypedLiteral(dateStr, XSDDatatype.XSDdate)); // (bmb.getId().getBmb04()));
				} catch (Exception e) {
					// Log error but continue
					System.err.println("Error formatting effective date: " + e.getMessage());
				}
			}

			if (bmb.getBmb05() != null) {
				try {
					// Convert java.sql.Date to proper XSD date format
					java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
					String dateStr = sdf.format(bmb.getBmb05());
					bomRelation.addProperty(expiryDate,
							ResourceFactory.createTypedLiteral(dateStr, XSDDatatype.XSDdate)); // (bmb.getBmb05()));
				} catch (Exception e) {
					// Log error but continue
					System.err.println("Error formatting expiry date: " + e.getMessage());
				}
			}

			if (bmb.getBmb06() != null) {
				bomRelation.addProperty(quantity, ResourceFactory.createTypedLiteral(bmb.getBmb06()));
			}

			if (bmb.getId().getBmb29() != null) {
				String sanitizedCharCode = sanitizeForUri(bmb.getId().getBmb29());
				bomRelation.addProperty(characteristicCode, sanitizedCharCode);
			}

			// Check for installation and shaftEndJoin properties from componentItemCode
			String componentItemCode = bmb.getId().getBmb03();
			if (componentItemCode != null && componentItemCode.length() >= 5) {
				String installationCode = componentItemCode.substring(2, 5);

				// Check for installation values
				Map<String, String> installationMap = new HashMap<>();
				installationMap.put("201", "CA");
				installationMap.put("202", "CB");
				installationMap.put("203", "FA");
				installationMap.put("206", "TC");
				installationMap.put("207", "LA");
				installationMap.put("208", "LB");

				if (installationMap.containsKey(installationCode)) {
					// Add installation property to the master item
					masterItem.addProperty(installation, installationMap.get(installationCode));
				}

				// Check for shaftEndJoin values
				Map<String, String> shaftEndJoinMap = new HashMap<>();
				shaftEndJoinMap.put("209", "Y");
				shaftEndJoinMap.put("210", "I");
				shaftEndJoinMap.put("211", "Pin");

				if (shaftEndJoinMap.containsKey(installationCode)) {
					// Add shaftEndJoin property to the master item
					masterItem.addProperty(shaftEndJoin, shaftEndJoinMap.get(installationCode));
				}
			}
		}
	}

	/**
	 * Exports the ontology model to a file in a specified format
	 */
	public void exportOntology(String filePath, String format) {
		try {
			ontModel.write(new java.io.FileOutputStream(filePath), format);
		} catch (java.io.FileNotFoundException e) {
			throw new RuntimeException("Error writing ontology to file: " + filePath, e);
		}
	}

	/**
	 * Gets the ontology model
	 */
	public OntModel getOntModel() {
		return ontModel;
	}

	/**
	 * Sanitizes material codes for use in URIs Replaces spaces and other invalid
	 * URI characters with underscores
	 */
	private String sanitizeForUri(String code) {
		if (code == null)
			return "";
		// Replace spaces and other problematic characters with underscores
		return code.replaceAll("\\s+", "_").replaceAll("[^a-zA-Z0-9_\\-.]", "_");
	}
}
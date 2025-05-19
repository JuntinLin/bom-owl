package com.jfc.owl;

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
        
        // Define class hierarchy
        masterItemClass.addSuperClass(materialClass);
        componentItemClass.addSuperClass(materialClass);
        
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
     * 	bmb01	varchar2(40)	主件料件編號	主件料件編號儲存該產品結構組合的主件料件編號。主件料件編號需在料件基本資料主檔中。
		bmb02	number(5)	組合項次	組合項次儲存該產品結構組合的順序項次。可作為設定元件料件在該產品結構組合中的用料順序。
		bmb03	varchar2(40)	元件料件編號	元件料件編號儲存該產品結構組合的項次中使用的元件料件編號。元件料件編號需在料件基本資料主檔中。
		bmb29	varchar2(20)	特性代碼
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
            Individual bomRelation = bomClass.createIndividual(BASE_URI + "BOM_" + 
            		sanitizedMasterCode + "_" + sanitizedSeqCode + "_" + sanitizedCompCode);
            
            // Link BOM to master and component
            bomRelation.addProperty(hasMasterItem, masterItem);
            bomRelation.addProperty(hasComponentItem, componentItem);
            
            // Link component to master (inverse relationship)
            componentItem.addProperty(isUsedIn, masterItem);
            
            // Add BOM details
            if (bmb.getId().getBmb04() != null) {
                bomRelation.addProperty(effectiveDate, 
                        ResourceFactory.createTypedLiteral(bmb.getId().getBmb04()));
            }
            
            if (bmb.getBmb05() != null) {
                bomRelation.addProperty(expiryDate, 
                        ResourceFactory.createTypedLiteral(bmb.getBmb05()));
            }
            
            if (bmb.getBmb06() != null) {
                bomRelation.addProperty(quantity, 
                        ResourceFactory.createTypedLiteral(bmb.getBmb06()));
            }
            
            if (bmb.getId().getBmb29() != null) {
            	String sanitizedCharCode = sanitizeForUri(bmb.getId().getBmb29());
                bomRelation.addProperty(characteristicCode, sanitizedCharCode);
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
     * Sanitizes material codes for use in URIs
     * Replaces spaces and other invalid URI characters with underscores
     */
    private String sanitizeForUri(String code) {
        if (code == null) return "";
        // Replace spaces and other problematic characters with underscores
        return code.replaceAll("\\s+", "_")
                  .replaceAll("[^a-zA-Z0-9_\\-.]", "_");
    }
}

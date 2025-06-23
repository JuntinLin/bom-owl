package com.jfc.owl.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jfc.owl.service.BomGeneratorService;
import com.jfc.owl.service.BomOwlExportService;
import com.jfc.rdb.common.dto.AbstractDTOController;
import com.jfc.rdb.common.dto.ApiResponse;
import com.jfc.rdb.tiptop.entity.ImaFile;

import org.apache.jena.ontology.OntModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * REST Controller for generating new BOMs using semantic reasoning
 */
@RestController
@RequestMapping("/bom-generator")
public class BomGeneratorController extends AbstractDTOController<ImaFile> {
    private static final Logger logger = LoggerFactory.getLogger(BomGeneratorController.class);

    @Autowired
    private BomGeneratorService bomGeneratorService;
    
    @Autowired
    private BomOwlExportService bomOwlExportService;

    /**
     * Generate a new BOM structure for a hydraulic cylinder based on its specifications
     * 
     * @param newItemInfo Map containing new item information
     * @return Generated BOM structure
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateNewBom(
            @RequestBody Map<String, String> newItemInfo) {
        
        try {
            String newItemCode = newItemInfo.get("itemCode");
            String itemName = newItemInfo.get("itemName");
            String itemSpec = newItemInfo.get("itemSpec");
            
            if (newItemCode == null || newItemCode.isEmpty()) {
                return error("Item code is required");
            }
            
            logger.info("Generating new BOM for item: {}", newItemCode);
            
            // Generate BOM structure using the service
            Map<String, Object> bomStructure = bomGeneratorService.generateNewBom(
                newItemCode, itemName, itemSpec);
            
            // Convert to frontend-friendly format
            Map<String, Object> result = bomGeneratorService.convertBomStructureForFrontend(bomStructure);
            
            return success(result);
            
        } catch (Exception e) {
            logger.error("Error generating BOM", e);
            return error("Error generating BOM: " + e.getMessage());
        }
    }
    
    /**
     * Create and export an ontology model for a generated BOM
     * 
     * @param bomStructure The generated BOM structure
     * @param format The ontology format (RDF/XML, TURTLE, JSON-LD, N-TRIPLES)
     * @return The exported ontology
     */
    @PostMapping("/export-generated")
    public ResponseEntity<?> exportGeneratedBom(
            @RequestBody Map<String, Object> bomStructure,
            @RequestParam(defaultValue = "JSONLD") String format) {
        
        try {
            String newItemCode = (String) bomStructure.get("masterItemCode");
            
            if (newItemCode == null || newItemCode.isEmpty()) {
                return error("Master item code is required");
            }
            
            logger.info("Creating ontology for generated BOM: {}", newItemCode);
            
            // Create ontology model from the BOM structure
            OntModel ontModel = bomGeneratorService.createNewBomOntology(newItemCode, bomStructure);
            
            // Serialize to the requested format
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            String contentType;
            
            switch (format.toUpperCase()) {
                case "TURTLE":
                case "TTL":
                    ontModel.write(outputStream, "TURTLE");
                    contentType = "text/turtle";
                    break;
                case "N-TRIPLES":
                case "NT":
                    ontModel.write(outputStream, "N-TRIPLES");
                    contentType = "application/n-triples";
                    break;
                case "RDF/XML":
                case "XML":
                    ontModel.write(outputStream, "RDF/XML");
                    contentType = "application/rdf+xml";
                    break;
                case "JSONLD":
                default:
                    ontModel.write(outputStream, "JSON-LD");
                    contentType = "application/ld+json";
                    break;
            }
            
            return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                .body(outputStream.toString());
            
        } catch (Exception e) {
            logger.error("Error exporting generated BOM", e);
            return error("Error exporting generated BOM: " + e.getMessage());
        }
    }
    
    /**
     * Find similar hydraulic cylinders based on specifications
     * 
     * @param newItemCode The code of the new hydraulic cylinder
     * @return List of similar cylinders with similarity scores
     */
    @GetMapping("/similar-cylinders/{newItemCode}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> findSimilarCylinders(
            @PathVariable String newItemCode) {
        
        try {
            logger.info("Finding similar cylinders for: {}", newItemCode);
            
            // Get reference ontology model
            OntModel referenceModel = bomOwlExportService.getBomOntologyForMasterItem(newItemCode);
            
            // Find similar cylinders
            Map<String, Object> result = Map.of(
                "itemCode", newItemCode,
                "similarCylinders", bomGeneratorService.findReferenceCylinders(newItemCode, referenceModel)
            );
            
            return success(result);
            
        } catch (Exception e) {
            logger.error("Error finding similar cylinders", e);
            return error("Error finding similar cylinders: " + e.getMessage());
        }
    }
    
    /**
     * Validate a hydraulic cylinder code format
     * 
     * @param itemCode The code to validate
     * @return Validation result with extracted specifications
     */
    @GetMapping("/validate-code/{itemCode}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateCylinderCode(
            @PathVariable String itemCode) {
        
        try {
            boolean isValid = false;
            String validationMessage = "";
            Map<String, String> specs = null;
            
            // Basic validation
            if (itemCode.length() < 15) {
                validationMessage = "Cylinder code must be at least 15 characters long";
            } else if (!itemCode.startsWith("3") && !itemCode.startsWith("4")) {
                validationMessage = "Hydraulic cylinder code must start with 3 or 4";
            } else {
                isValid = true;
                validationMessage = "Valid hydraulic cylinder code";
                
                // Extract specifications
                specs = Map.of(
                    "series", itemCode.substring(2, 4),
                    "type", itemCode.substring(4, 5),
                    "bore", itemCode.substring(5, 8),
                    "stroke", itemCode.substring(10, 14),
                    "rodEndType", itemCode.substring(14, 15)
                );
            }
            
            Map<String, Object> result = Map.of(
                "itemCode", itemCode,
                "isValid", isValid,
                "message", validationMessage,
                "specifications", specs != null ? specs : Map.of()
            );
            
            return success(result);
            
        } catch (Exception e) {
            logger.error("Error validating cylinder code", e);
            return error("Error validating cylinder code: " + e.getMessage());
        }
    }
}
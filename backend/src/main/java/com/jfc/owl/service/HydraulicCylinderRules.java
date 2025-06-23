package com.jfc.owl.service;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.vocabulary.RDF;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service containing specialized rules and reasoning capabilities for hydraulic cylinders.
 * This service complements the main ReasoningService with domain-specific knowledge.
 */
@Service
public class HydraulicCylinderRules {

    // Namespace constants
    private static final String BASE_URI = "http://www.jfc.com/tiptop/ontology#";
    
    /**
     * Generates SWRL-like rules specific to hydraulic cylinder domain knowledge
     * @return A list of rules in Jena rule format
     */
    public List<String> getHydraulicCylinderRules() {
        List<String> rules = new ArrayList<>();
        
        // Rule 1: Identify hydraulic cylinders with specific bore and stroke
        rules.add(
            "[IdentifyCylinder: (?material rdf:type <" + BASE_URI + "Material>) " +
            "(?material <" + BASE_URI + "itemCode> ?code) " +
            "regex(?code, '^[34].*') " +
            "-> " +
            "(?material rdf:type <" + BASE_URI + "Cylinder>)]"
        );
        
        // Rule 2: Extract bore size from code format (positions 5-8)
        rules.add(
            "[ExtractBoreSize: (?cylinder rdf:type <" + BASE_URI + "Cylinder>) " +
            "(?cylinder <" + BASE_URI + "itemCode> ?code) " +
            "regex(?code, '^[34]..(...).*') " +
            "-> " +
            "(?cylinder <" + BASE_URI + "hasStandardBore> true)]"
        );
        
        // Rule 3: Extract stroke length from code format (positions 10-14)
        rules.add(
            "[ExtractStrokeLength: (?cylinder rdf:type <" + BASE_URI + "Cylinder>) " +
            "(?cylinder <" + BASE_URI + "itemCode> ?code) " +
            "regex(?code, '^[34].....(\\d{4}).*') " +
            "-> " +
            "(?cylinder <" + BASE_URI + "hasStandardStroke> true)]"
        );
        
        // Rule 4: Identify cylinders with FA installation type
        rules.add(
            "[FAInstallationType: (?cylinder rdf:type <" + BASE_URI + "Cylinder>) " +
            "(?cylinder <" + BASE_URI + "installation> 'FA') " +
            "-> " +
            "(?cylinder <" + BASE_URI + "hasFrontAttachment> true)]"
        );
        
        // Rule 5: Standard piston rod inference based on bore size
        rules.add(
            "[StandardPistonRod: (?cylinder rdf:type <" + BASE_URI + "Cylinder>) " +
            "(?cylinder <" + BASE_URI + "bore> ?bore) " +
            "greaterThan(?bore, '050') lessThan(?bore, '080') " +
            "-> " +
            "(?cylinder <" + BASE_URI + "requiresStandardPistonRod> true)]"
        );
        
        // Rule 6: Heavy duty sealing set required for high pressure applications
        rules.add(
            "[HeavyDutySealing: (?cylinder rdf:type <" + BASE_URI + "Cylinder>) " +
            "(?cylinder <" + BASE_URI + "bore> ?bore) " +
            "greaterThan(?bore, '100') " +
            "-> " +
            "(?cylinder <" + BASE_URI + "requiresHeavyDutySealing> true)]"
        );
        
        // Rule 7: Materials compatibility rule
        rules.add(
            "[MaterialsCompatibility: (?cylinder rdf:type <" + BASE_URI + "Cylinder>) " +
            "(?cylinder <" + BASE_URI + "hasStainlessSteelParts> true) " +
            "-> " +
            "(?cylinder <" + BASE_URI + "requiresNonCorrosiveSeals> true)]"
        );
        
        // Rule 8: Component selection based on properties
        rules.add(
            "[ComponentSelection: (?cylinder rdf:type <" + BASE_URI + "Cylinder>) " +
            "(?cylinder <" + BASE_URI + "bore> ?bore) " +
            "(?cylinder <" + BASE_URI + "stroke> ?stroke) " +
            "(?component rdf:type <" + BASE_URI + "ComponentItem>) " +
            "(?component <" + BASE_URI + "fitsMinBore> ?minBore) " +
            "(?component <" + BASE_URI + "fitsMaxBore> ?maxBore) " +
            "greaterThan(?bore, ?minBore) lessThan(?bore, ?maxBore) " +
            "-> " +
            "(?component <" + BASE_URI + "isCompatibleWith> ?cylinder)]"
        );
        
        return rules;
    }
    
    /**
     * Apply hydraulic cylinder specific rules and generate inferences
     * 
     * @param ontModel The original ontology model
     * @return InfModel The inference model with applied rules
     */
    public InfModel applyHydraulicCylinderRules(OntModel ontModel) {
        // Create a rule-based reasoner with domain-specific rules
        List<Rule> ruleList = new ArrayList<>();
        
        // Parse and add each rule
        for (String ruleText : getHydraulicCylinderRules()) {
            ruleList.add(Rule.parseRule(ruleText));
        }
        
        GenericRuleReasoner ruleReasoner = new GenericRuleReasoner(ruleList);
        ruleReasoner.setTransitiveClosureCaching(true);
        
        // Apply rules to the model
        InfModel infModel = ModelFactory.createInfModel(ruleReasoner, ontModel);
        
        return infModel;
    }
    
    /**
     * Suggests components for a hydraulic cylinder based on its specifications
     * 
     * @param masterItemCode The master item code of the hydraulic cylinder
     * @param ontModel The ontology model containing component information
     * @return A map of component types to suggested components
     */
    public Map<String, List<Map<String, String>>> suggestComponents(String masterItemCode, OntModel ontModel) {
        Map<String, List<Map<String, String>>> suggestions = new HashMap<>();
        
        // Apply hydraulic cylinder rules
        InfModel infModel = applyHydraulicCylinderRules(ontModel);
        
        // Get the cylinder individual
        Resource cylinderResource = infModel.getResource(BASE_URI + "Material_" + masterItemCode);
        
        // Check if this is actually a cylinder
        boolean isCylinder = false;
        StmtIterator typeStmts = infModel.listStatements(cylinderResource, RDF.type, (Resource)null);
        while (typeStmts.hasNext()) {
            Statement stmt = typeStmts.next();
            if (stmt.getObject().toString().contains("Cylinder")) {
                isCylinder = true;
                break;
            }
        }
        
        if (!isCylinder) {
            // Not a cylinder, return empty suggestions
            return suggestions;
        }
        
        // Extract bore and stroke
        String bore = "";
        String stroke = "";
        
        StmtIterator boreStmts = infModel.listStatements(cylinderResource, 
                                                        infModel.getProperty(BASE_URI + "bore"), 
                                                        (org.apache.jena.rdf.model.RDFNode)null);
        if (boreStmts.hasNext()) {
            bore = boreStmts.next().getObject().toString();
        }
        
        StmtIterator strokeStmts = infModel.listStatements(cylinderResource, 
                                                          infModel.getProperty(BASE_URI + "stroke"), 
                                                          (org.apache.jena.rdf.model.RDFNode)null);
        if (strokeStmts.hasNext()) {
            stroke = strokeStmts.next().getObject().toString();
        }
        
        // If bore or stroke missing, extract from code
        if (bore.isEmpty() || stroke.isEmpty()) {
            if (masterItemCode.length() >= 15) {
                if (bore.isEmpty() && masterItemCode.length() >= 8) {
                    bore = masterItemCode.substring(5, 8);
                }
                if (stroke.isEmpty() && masterItemCode.length() >= 14) {
                    stroke = masterItemCode.substring(10, 14);
                }
            }
        }
        
        // Create component categories
        suggestions.put("Barrel", new ArrayList<>());
        suggestions.put("Piston", new ArrayList<>());
        suggestions.put("PistonRod", new ArrayList<>());
        suggestions.put("Seals", new ArrayList<>());
        suggestions.put("EndCaps", new ArrayList<>());
        
        // For each component in the ontology, check compatibility based on bore and stroke
        StmtIterator components = infModel.listStatements(null, RDF.type, 
                                                         infModel.getResource(BASE_URI + "ComponentItem"));
        
        while (components.hasNext()) {
            Resource component = components.next().getSubject();
            
            // Get component name and code
            String componentName = "";
            String componentCode = "";
            
            StmtIterator nameStmts = infModel.listStatements(component, 
                                                            infModel.getProperty(BASE_URI + "itemName"), 
                                                            (org.apache.jena.rdf.model.RDFNode)null);
            if (nameStmts.hasNext()) {
                componentName = nameStmts.next().getObject().toString();
            }
            
            StmtIterator codeStmts = infModel.listStatements(component, 
                                                           infModel.getProperty(BASE_URI + "itemCode"), 
                                                           (org.apache.jena.rdf.model.RDFNode)null);
            if (codeStmts.hasNext()) {
                componentCode = codeStmts.next().getObject().toString();
            }
            
            if (componentName.isEmpty() || componentCode.isEmpty()) {
                continue; // Skip components without name or code
            }
            
            // Determine component category based on name or code patterns
            String category = determineComponentCategory(componentName, componentCode);
            
            if (category != null && suggestions.containsKey(category)) {
                // Check if component is compatible with the cylinder bore and stroke
                boolean isCompatible = isComponentCompatible(component, bore, stroke, infModel);
                
                if (isCompatible) {
                    Map<String, String> componentInfo = new HashMap<>();
                    componentInfo.put("code", componentCode);
                    componentInfo.put("name", componentName);
                    
                    // Get specification if available
                    StmtIterator specStmts = infModel.listStatements(component, 
                                                                   infModel.getProperty(BASE_URI + "itemSpec"), 
                                                                   (org.apache.jena.rdf.model.RDFNode)null);
                    if (specStmts.hasNext()) {
                        componentInfo.put("spec", specStmts.next().getObject().toString());
                    }
                    
                    suggestions.get(category).add(componentInfo);
                }
            }
        }
        
        return suggestions;
    }
    
    /**
     * Determines the category of a component based on its name or code
     * 
     * @param componentName The name of the component
     * @param componentCode The code of the component
     * @return The component category or null if not determined
     */
    private String determineComponentCategory(String componentName, String componentCode) {
        String nameLower = componentName.toLowerCase();
        
        if (nameLower.contains("barrel") || nameLower.contains("cylinder tube") || 
            componentCode.startsWith("20")) {
            return "Barrel";
        } else if (nameLower.contains("piston") && !nameLower.contains("rod")) {
            return "Piston";
        } else if (nameLower.contains("piston rod") || nameLower.contains("rod") ||
                  componentCode.startsWith("21")) {
            return "PistonRod";
        } else if (nameLower.contains("seal") || nameLower.contains("wiper") || 
                  nameLower.contains("o-ring") || componentCode.startsWith("25")) {
            return "Seals";
        } else if (nameLower.contains("cap") || nameLower.contains("head") || 
                  componentCode.startsWith("22") || componentCode.startsWith("23")) {
            return "EndCaps";
        }
        
        return null;
    }
    
    /**
     * Checks if a component is compatible with the given bore and stroke
     * 
     * @param component The component resource
     * @param bore The bore size
     * @param stroke The stroke length
     * @param model The inference model
     * @return True if compatible, false otherwise
     */
    private boolean isComponentCompatible(Resource component, String bore, String stroke, InfModel model) {
        // Check if the inference model has already determined compatibility
        StmtIterator compatStmts = model.listStatements(component, 
                                                      model.getProperty(BASE_URI + "isCompatibleWith"), 
                                                      (org.apache.jena.rdf.model.RDFNode)null);
        if (compatStmts.hasNext()) {
            return true; // Already determined as compatible by the reasoner
        }
        
        // Check min/max bore compatibility if bore size is known
        if (!bore.isEmpty()) {
            int boreSize;
            try {
                boreSize = Integer.parseInt(bore);
            } catch (NumberFormatException e) {
                return false; // Invalid bore format
            }
            
            // Check min bore compatibility
            StmtIterator minBoreStmts = model.listStatements(component, 
                                                           model.getProperty(BASE_URI + "fitsMinBore"), 
                                                           (org.apache.jena.rdf.model.RDFNode)null);
            while (minBoreStmts.hasNext()) {
                Statement stmt = minBoreStmts.next();
                try {
                    int minBore = Integer.parseInt(stmt.getObject().toString());
                    if (boreSize < minBore) {
                        return false; // Bore too small
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid values
                }
            }
            
            // Check max bore compatibility
            StmtIterator maxBoreStmts = model.listStatements(component, 
                                                           model.getProperty(BASE_URI + "fitsMaxBore"), 
                                                           (org.apache.jena.rdf.model.RDFNode)null);
            while (maxBoreStmts.hasNext()) {
                Statement stmt = maxBoreStmts.next();
                try {
                    int maxBore = Integer.parseInt(stmt.getObject().toString());
                    if (boreSize > maxBore) {
                        return false; // Bore too large
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid values
                }
            }
        }
        
        // Additional compatibility checks could be implemented here
        // For example, checking stroke compatibility for rod components
        
        return true; // If we've passed all checks, consider it compatible
    }
    
    /**
     * Generates a BOM structure for a new hydraulic cylinder based on specifications
     * 
     * @param newItemCode The code for the new cylinder
     * @param ontModel The ontology model
     * @return A map representing the BOM structure with suggested components
     */
    public Map<String, Object> generateHydraulicCylinderBom(String newItemCode, OntModel ontModel) {
        Map<String, Object> bomStructure = new HashMap<>();
        bomStructure.put("masterItemCode", newItemCode);
        
        // Extract specifications from the new item code
        String series = newItemCode.length() >= 4 ? newItemCode.substring(2, 4) : "";
        String type = newItemCode.length() >= 5 ? newItemCode.substring(4, 5) : "";
        String bore = newItemCode.length() >= 8 ? newItemCode.substring(5, 8) : "";
        String stroke = newItemCode.length() >= 14 ? newItemCode.substring(10, 14) : "";
        String rodEndType = newItemCode.length() >= 15 ? newItemCode.substring(14, 15) : "";
        
        bomStructure.put("specifications", Map.of(
            "series", series,
            "type", type,
            "bore", bore,
            "stroke", stroke,
            "rodEndType", rodEndType
        ));
        
        // Get component suggestions
        Map<String, List<Map<String, String>>> suggestions = suggestComponents(newItemCode, ontModel);
        bomStructure.put("components", suggestions);
        
        // Generate quantities based on component types
        Map<String, Integer> defaultQuantities = new HashMap<>();
        defaultQuantities.put("Barrel", 1);
        defaultQuantities.put("Piston", 1);
        defaultQuantities.put("PistonRod", 1);
        defaultQuantities.put("EndCaps", 2); // Usually we need 2 end caps
        
        // For seals, quantity depends on bore size
        int sealQuantity = 1;
        try {
            int boreValue = Integer.parseInt(bore);
            if (boreValue > 80) {
                sealQuantity = 2; // Larger cylinders need more seals
            }
        } catch (NumberFormatException e) {
            // Use default quantity
        }
        defaultQuantities.put("Seals", sealQuantity);
        
        bomStructure.put("defaultQuantities", defaultQuantities);
        
        return bomStructure;
    }
    
    /**
     * Find similar existing cylinders to use as reference for a new cylinder
     * 
     * @param newItemCode The code for the new cylinder
     * @param ontModel The ontology model containing existing cylinders
     * @return List of similar cylinders with similarity scores
     */
    public List<Map<String, Object>> findSimilarCylinders(String newItemCode, OntModel ontModel) {
        List<Map<String, Object>> similarCylinders = new ArrayList<>();
        
        // Extract specifications from the new item code
        String newSeries = newItemCode.length() >= 4 ? newItemCode.substring(2, 4) : "";
        String newType = newItemCode.length() >= 5 ? newItemCode.substring(4, 5) : "";
        String newBore = newItemCode.length() >= 8 ? newItemCode.substring(5, 8) : "";
        String newStroke = newItemCode.length() >= 14 ? newItemCode.substring(10, 14) : "";
        String newRodEndType = newItemCode.length() >= 15 ? newItemCode.substring(14, 15) : "";
        
        // Find all cylinders in the ontology
        StmtIterator cylinderStmts = ontModel.listStatements(null, RDF.type, 
                                                            ontModel.getResource(BASE_URI + "Cylinder"));
        
        while (cylinderStmts.hasNext()) {
            Resource cylinder = cylinderStmts.next().getSubject();
            
            // Get cylinder code
            String cylinderCode = "";
            StmtIterator codeStmts = ontModel.listStatements(cylinder, 
                                                           ontModel.getProperty(BASE_URI + "itemCode"), 
                                                           (org.apache.jena.rdf.model.RDFNode)null);
            if (codeStmts.hasNext()) {
                cylinderCode = codeStmts.next().getObject().toString();
            }
            
            if (cylinderCode.isEmpty() || cylinderCode.equals(newItemCode)) {
                continue; // Skip if no code or it's the same as the new item
            }
            
            // Extract specifications from the existing cylinder code
            String series = cylinderCode.length() >= 4 ? cylinderCode.substring(2, 4) : "";
            String type = cylinderCode.length() >= 5 ? cylinderCode.substring(4, 5) : "";
            String bore = cylinderCode.length() >= 8 ? cylinderCode.substring(5, 8) : "";
            String stroke = cylinderCode.length() >= 14 ? cylinderCode.substring(10, 14) : "";
            String rodEndType = cylinderCode.length() >= 15 ? cylinderCode.substring(14, 15) : "";
            
            // Calculate similarity score
            int similarityScore = 0;
            
            if (series.equals(newSeries)) similarityScore += 20;
            if (type.equals(newType)) similarityScore += 15;
            if (bore.equals(newBore)) similarityScore += 30;
            if (stroke.equals(newStroke)) similarityScore += 20;
            if (rodEndType.equals(newRodEndType)) similarityScore += 15;
            
            // Only include cylinders with at least 50% similarity
            if (similarityScore >= 50) {
                Map<String, Object> similarCylinder = new HashMap<>();
                similarCylinder.put("code", cylinderCode);
                
                // Get name if available
                StmtIterator nameStmts = ontModel.listStatements(cylinder, 
                                                               ontModel.getProperty(BASE_URI + "itemName"), 
                                                               (org.apache.jena.rdf.model.RDFNode)null);
                if (nameStmts.hasNext()) {
                    similarCylinder.put("name", nameStmts.next().getObject().toString());
                }
                
                // Get spec if available
                StmtIterator specStmts = ontModel.listStatements(cylinder, 
                                                               ontModel.getProperty(BASE_URI + "itemSpec"), 
                                                               (org.apache.jena.rdf.model.RDFNode)null);
                if (specStmts.hasNext()) {
                    similarCylinder.put("spec", specStmts.next().getObject().toString());
                }
                
                similarCylinder.put("similarityScore", similarityScore);
                similarCylinder.put("specifications", Map.of(
                    "series", series,
                    "type", type,
                    "bore", bore,
                    "stroke", stroke,
                    "rodEndType", rodEndType
                ));
                
                similarCylinders.add(similarCylinder);
            }
        }
        
        // Sort by similarity score (descending)
        similarCylinders.sort((a, b) -> {
            Integer scoreA = (Integer) a.get("similarityScore");
            Integer scoreB = (Integer) b.get("similarityScore");
            return scoreB.compareTo(scoreA);
        });
        
        return similarCylinders;
    }
}
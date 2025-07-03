package com.jfc.owl.service; 

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jfc.rdb.tiptop.entity.ImaFile;
import com.jfc.rdb.tiptop.repository.ImaRepository;
import com.jfc.owl.ontology.HydraulicCylinderOntology;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced service for generating new Bills of Materials based on ontology reasoning
 * with specialized hydraulic cylinder domain knowledge
 */
@Service
public class BomGeneratorService { 
    private static final Logger logger = LoggerFactory.getLogger(BomGeneratorService.class);
    private static final String BASE_URI = "http://www.jfc.com/tiptop/ontology#";

    @Autowired
    private BomOwlExportService bomOwlExportService;
    
        
    @Autowired
    private EnhancedHydraulicCylinderRules hydraulicCylinderRules;
    
        
    @Autowired
    private ImaRepository imaRepository;
    
    @Autowired
    private HydraulicCylinderOntology hydraulicCylinderOntology;

    /**
     * Enhanced method to generate a new BOM structure for a hydraulic cylinder 
     * using the specialized ontology
     * 
     * @param newItemCode The code for the new cylinder
     * @param itemName The name for the new cylinder
     * @param itemSpec The specifications for the new cylinder
     * @return A map containing the generated BOM structure with enhanced suggestions
     */
    public Map<String, Object> generateNewBom(String newItemCode, String itemName, String itemSpec) {
        logger.info("Generating enhanced BOM for hydraulic cylinder: {}", newItemCode);
        InfModel reasonedModel = null;
        try {
            // Initialize the specialized hydraulic cylinder ontology
            hydraulicCylinderOntology.initializeHydraulicCylinderOntology();
            
            // Extract and validate specifications from the code
            Map<String, String> specs = extractSpecificationsFromCode(newItemCode);
            
            // Validate specifications using the ontology
            HydraulicCylinderOntology.ValidationResult validation = 
                hydraulicCylinderOntology.validateHydraulicCylinderSpecs(specs);
            
            if (!validation.isValid()) {
                logger.warn("Specification validation failed for {}: {}", newItemCode, validation.getErrors());
                // Continue with warnings but stop on errors
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("error", "Invalid specifications");
                errorResult.put("validationErrors", validation.getErrors());
                errorResult.put("validationWarnings", validation.getWarnings());
                return errorResult;
            }
            
            // Create hydraulic cylinder individual in the ontology
            Individual cylinderIndividual = hydraulicCylinderOntology.createHydraulicCylinderIndividual(newItemCode, specs);
            
            // Get the enhanced ontology model
            OntModel enhancedOntModel = hydraulicCylinderOntology.getOntologyModel();
            
            // Apply reasoning to classify the cylinder and infer relationships
            reasonedModel = hydraulicCylinderRules.applyEnhancedHydraulicCylinderRules(enhancedOntModel);
            
            // Generate compatible components using the specialized ontology
            List<HydraulicCylinderOntology.ComponentSuggestion> componentSuggestions = 
                hydraulicCylinderOntology.generateCompatibleComponents(cylinderIndividual);
            
            // Find similar existing cylinders for reference
            List<Map<String, Object>> similarCylinders = 
                hydraulicCylinderRules.findEnhancedSimilarCylinders(newItemCode, enhancedOntModel);
            
            // Build the enhanced BOM structure
            Map<String, Object> bomStructure = buildEnhancedBomStructure(
                newItemCode, itemName, itemSpec, specs, componentSuggestions, similarCylinders, validation);
            
            logger.info("Successfully generated enhanced BOM for {} with {} component suggestions", 
                       newItemCode, componentSuggestions.size());
            
            return bomStructure;
            
        } catch (Exception e) {
            logger.error("Error generating enhanced BOM for cylinder: " + newItemCode, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            errorResult.put("stackTrace", Arrays.toString(e.getStackTrace()));
            return errorResult;
        }finally {
            // Clean up temporary models
            if (reasonedModel != null) {
                reasonedModel.close();
            }
            // Force garbage collection for large objects
            System.gc();
        }
    }
    
    /**
     * Build enhanced BOM structure with specialized component suggestions
     */
    private Map<String, Object> buildEnhancedBomStructure(
            String newItemCode, String itemName, String itemSpec, 
            Map<String, String> specs,
            List<HydraulicCylinderOntology.ComponentSuggestion> componentSuggestions,
            List<Map<String, Object>> similarCylinders,
            HydraulicCylinderOntology.ValidationResult validation) {
        
        Map<String, Object> bomStructure = new HashMap<>();
        bomStructure.put("masterItemCode", newItemCode);
        bomStructure.put("itemName", itemName);
        bomStructure.put("itemSpec", itemSpec);
        bomStructure.put("specifications", specs);
        
        // Group component suggestions by category
        Map<String, List<Map<String, Object>>> componentsByCategory = 
            groupComponentSuggestionsByCategory(componentSuggestions);
        
        bomStructure.put("components", componentsByCategory);
        
        // Generate intelligent default quantities
        Map<String, Integer> defaultQuantities = generateIntelligentQuantities(specs, componentSuggestions);
        bomStructure.put("defaultQuantities", defaultQuantities);
        
        // Add similar cylinders for reference
        bomStructure.put("similarCylinders", similarCylinders);
        
        // Add validation information
        bomStructure.put("validationWarnings", validation.getWarnings());
        bomStructure.put("validationPassed", validation.isValid());
        
        // Add cylinder classification information
        bomStructure.put("cylinderClassification", determineCylinderClassification(specs));
        
        // Add recommendation scores
        bomStructure.put("overallRecommendationScore", calculateOverallRecommendationScore(componentSuggestions));
        
        // Add component statistics
        bomStructure.put("componentStatistics", generateComponentStatistics(componentSuggestions));
        
        return bomStructure;
    }
    
    /**
     * Group component suggestions by category
     */
    private Map<String, List<Map<String, Object>>> groupComponentSuggestionsByCategory(
            List<HydraulicCylinderOntology.ComponentSuggestion> suggestions) {
        
        Map<String, List<Map<String, Object>>> groupedComponents = new HashMap<>();
        
        for (HydraulicCylinderOntology.ComponentSuggestion suggestion : suggestions) {
            String category = suggestion.getCategory();
            
            Map<String, Object> componentInfo = new HashMap<>();
            componentInfo.put("code", suggestion.getCode());
            componentInfo.put("name", suggestion.getName());
            componentInfo.put("description", suggestion.getDescription());
            componentInfo.put("quantity", suggestion.getQuantity());
            componentInfo.put("compatibilityScore", suggestion.getCompatibilityScore());
            
            // Add recommendation level based on compatibility score
            componentInfo.put("recommendationLevel", getRecommendationLevel(suggestion.getCompatibilityScore()));
            
            groupedComponents.computeIfAbsent(category, k -> new ArrayList<>()).add(componentInfo);
        }
        
        // Sort components within each category by compatibility score (descending)
        groupedComponents.values().forEach(categoryList -> 
            categoryList.sort((a, b) -> {
                Double scoreA = (Double) a.get("compatibilityScore");
                Double scoreB = (Double) b.get("compatibilityScore");
                return Double.compare(scoreB, scoreA);
            })
        );
        
        return groupedComponents;
    }
    
    /**
     * Generate intelligent default quantities based on specifications and component types
     */
    private Map<String, Integer> generateIntelligentQuantities(
            Map<String, String> specs, List<HydraulicCylinderOntology.ComponentSuggestion> suggestions) {
        
        Map<String, Integer> quantities = new HashMap<>();
        
        // Extract bore size for quantity calculations
        int boreSize = 50; // default
        try {
            if (specs.containsKey("bore")) {
                boreSize = Integer.parseInt(specs.get("bore"));
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid bore size format: {}", specs.get("bore"));
        }
        
        // Group suggestions by category to determine quantities
        Map<String, List<HydraulicCylinderOntology.ComponentSuggestion>> byCategory = 
            suggestions.stream().collect(Collectors.groupingBy(HydraulicCylinderOntology.ComponentSuggestion::getCategory));
        
        // Set intelligent quantities based on component category and cylinder size
        for (String category : byCategory.keySet()) {
            int quantity = calculateCategoryQuantity(category, boreSize, specs);
            quantities.put(category, quantity);
        }
        
        return quantities;
    }
    
    /**
     * Calculate quantity for a specific component category
     */
    private int calculateCategoryQuantity(String category, int boreSize, Map<String, String> specs) {
        switch (category) {
            case "CylinderBarrel":
            case "Piston":
            case "PistonRod":
                return 1;
                
            case "EndCap":
            case "HeadEndCap":
            case "RodEndCap":
                return 2; // Usually need both head and rod end caps
                
            case "SealingComponent":
            case "PistonSeal":
            case "RodSeal":
            case "WiperSeal":
                // Larger cylinders may need multiple seals
                return boreSize > 100 ? 2 : 1;
                
            case "ORingSeal":
                // Multiple O-rings typically needed
                return boreSize > 80 ? 4 : 2;
                
            case "Bushing":
            case "RodBushing":
                return boreSize > 80 ? 2 : 1;
                
            case "Gasket":
                return 2; // Usually for both end caps
                
            case "Fastener":
            case "TieRod":
                // Tie rod quantity based on bore size
                if (boreSize <= 50) return 4;
                else if (boreSize <= 100) return 6;
                else if (boreSize <= 150) return 8;
                else return 12;
                
            default:
                return 1;
        }
    }
    
    /**
     * Determine cylinder classification based on specifications
     */
    private Map<String, String> determineCylinderClassification(Map<String, String> specs) {
        Map<String, String> classification = new HashMap<>();
        
        // Bore size classification
        try {
            int bore = Integer.parseInt(specs.getOrDefault("bore", "50"));
            if (bore <= 50) {
                classification.put("boreSize", "Small");
            } else if (bore <= 100) {
                classification.put("boreSize", "Medium");
            } else {
                classification.put("boreSize", "Large");
            }
        } catch (NumberFormatException e) {
            classification.put("boreSize", "Unknown");
        }
        
        // Stroke length classification
        try {
            int stroke = Integer.parseInt(specs.getOrDefault("stroke", "100"));
            if (stroke <= 100) {
                classification.put("strokeLength", "Short");
            } else if (stroke <= 300) {
                classification.put("strokeLength", "Medium");
            } else {
                classification.put("strokeLength", "Long");
            }
        } catch (NumberFormatException e) {
            classification.put("strokeLength", "Unknown");
        }
        
        // Series classification
        String series = specs.getOrDefault("series", "");
        switch (series) {
            case "10":
                classification.put("series", "Standard");
                break;
            case "11":
                classification.put("series", "Heavy Duty");
                break;
            case "12":
                classification.put("series", "Compact");
                break;
            case "13":
                classification.put("series", "Light Duty");
                break;
            default:
                classification.put("series", "Unknown");
        }
        
        // Rod end type classification
        String rodEndType = specs.getOrDefault("rodEndType", "");
        switch (rodEndType) {
            case "Y":
                classification.put("rodEndType", "Yoke");
                break;
            case "I":
                classification.put("rodEndType", "Internal Thread");
                break;
            case "E":
                classification.put("rodEndType", "External Thread");
                break;
            case "P":
                classification.put("rodEndType", "Pin");
                break;
            default:
                classification.put("rodEndType", "Standard");
        }
        
        return classification;
    }
    
    /**
     * Calculate overall recommendation score
     */
    private double calculateOverallRecommendationScore(List<HydraulicCylinderOntology.ComponentSuggestion> suggestions) {
        if (suggestions.isEmpty()) return 0.0;
        
        double totalScore = suggestions.stream()
            .mapToDouble(HydraulicCylinderOntology.ComponentSuggestion::getCompatibilityScore)
            .sum();
        
        return totalScore / suggestions.size();
    }
    
    /**
     * Generate component statistics
     */
    private Map<String, Object> generateComponentStatistics(List<HydraulicCylinderOntology.ComponentSuggestion> suggestions) {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalComponents", suggestions.size());
        
        // Count by category
        Map<String, Long> categoryCount = suggestions.stream()
            .collect(Collectors.groupingBy(HydraulicCylinderOntology.ComponentSuggestion::getCategory, Collectors.counting()));
        stats.put("componentsByCategory", categoryCount);
        
        // Average compatibility score
        double avgScore = suggestions.stream()
            .mapToDouble(HydraulicCylinderOntology.ComponentSuggestion::getCompatibilityScore)
            .average()
            .orElse(0.0);
        stats.put("averageCompatibilityScore", Math.round(avgScore * 100.0) / 100.0);
        
        // High confidence components (score > 0.8)
        long highConfidenceCount = suggestions.stream()
            .mapToDouble(HydraulicCylinderOntology.ComponentSuggestion::getCompatibilityScore)
            .filter(score -> score > 0.8)
            .count();
        stats.put("highConfidenceComponents", highConfidenceCount);
        
        return stats;
    }
    
    /**
     * Get recommendation level based on compatibility score
     */
    private String getRecommendationLevel(double score) {
        if (score >= 0.9) return "Highly Recommended";
        else if (score >= 0.7) return "Recommended";
        else if (score >= 0.5) return "Consider";
        else return "Alternative";
    }
    
    /**
     * Creates a new ontology model with the generated BOM using enhanced domain knowledge
     * 
     * @param newItemCode The code for the new cylinder
     * @param bomStructure The generated BOM structure
     * @return The new ontology model with enhanced relationships
     */
    public OntModel createNewBomOntology(String newItemCode, Map<String, Object> bomStructure) {
        logger.info("Creating enhanced BOM ontology for: {}", newItemCode);
        
        try {
            // Start with the specialized hydraulic cylinder ontology as base
            OntModel enhancedModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
            
            // Copy the domain classes and properties from the specialized ontology
            OntModel domainModel = hydraulicCylinderOntology.getOntologyModel();
            enhancedModel.add(domainModel);
            
            // Get domain classes and properties
            Map<String, OntClass> domainClasses = hydraulicCylinderOntology.getDomainClasses();
            Map<String, org.apache.jena.ontology.OntProperty> domainProperties = hydraulicCylinderOntology.getDomainProperties();
            
            // Create the master cylinder individual with enhanced properties
            Individual masterItem = createEnhancedMasterItem(enhancedModel, newItemCode, bomStructure, 
                                                           domainClasses, domainProperties);
            
            // Process and add component items with enhanced relationships
            addEnhancedComponentItems(enhancedModel, masterItem, bomStructure, domainClasses, domainProperties);
            
            logger.info("Successfully created enhanced BOM ontology for {}", newItemCode);
            return enhancedModel;
            
        } catch (Exception e) {
            logger.error("Error creating enhanced BOM ontology for: " + newItemCode, e);
            throw new RuntimeException("Failed to create enhanced BOM ontology", e);
        }
    }
    
    /**
     * Create enhanced master item individual with specialized properties
     */
    private Individual createEnhancedMasterItem(OntModel model, String itemCode, Map<String, Object> bomStructure,
                                              Map<String, OntClass> domainClasses, 
                                              Map<String, org.apache.jena.ontology.OntProperty> domainProperties) {
        
        // Create the master item as a hydraulic cylinder
        Individual masterItem = domainClasses.get("HydraulicCylinder")
            .createIndividual(BASE_URI + "Material_" + itemCode);
        
        // Add basic properties
        masterItem.addProperty(domainProperties.get("itemCode"), itemCode);
        masterItem.addProperty(domainProperties.get("itemName"), (String) bomStructure.get("itemName"));
        masterItem.addProperty(domainProperties.get("itemSpec"), (String) bomStructure.get("itemSpec"));
        
        // Add hydraulic cylinder specific properties
        Map<String, String> specs = (Map<String, String>) bomStructure.get("specifications");
        if (specs != null) {
            if (specs.containsKey("bore")) {
                masterItem.addProperty(domainProperties.get("bore"), specs.get("bore"));
            }
            if (specs.containsKey("stroke")) {
                masterItem.addProperty(domainProperties.get("stroke"), specs.get("stroke"));
            }
            if (specs.containsKey("series")) {
                masterItem.addProperty(domainProperties.get("series"), specs.get("series"));
            }
            if (specs.containsKey("rodEndType")) {
                masterItem.addProperty(domainProperties.get("rodEndType"), specs.get("rodEndType"));
            }
            if (specs.containsKey("type")) {
                masterItem.addProperty(domainProperties.get("cylinderType"), specs.get("type"));
            }
        }
        
        // Add classification information
        Map<String, String> classification = (Map<String, String>) bomStructure.get("cylinderClassification");
        if (classification != null) {
            // Add additional classification as RDF types based on specifications
            addClassificationTypes(masterItem, classification, domainClasses);
        }
        
        return masterItem;
    }
    
    /**
     * Add classification types to the master item based on specifications
     */
    private void addClassificationTypes(Individual masterItem, Map<String, String> classification,
                                      Map<String, OntClass> domainClasses) {
        
        // Add bore size classification
        String boreSize = classification.get("boreSize");
        if ("Small".equals(boreSize) && domainClasses.containsKey("SmallBoreCylinder")) {
            masterItem.addRDFType(domainClasses.get("SmallBoreCylinder"));
        } else if ("Medium".equals(boreSize) && domainClasses.containsKey("MediumBoreCylinder")) {
            masterItem.addRDFType(domainClasses.get("MediumBoreCylinder"));
        } else if ("Large".equals(boreSize) && domainClasses.containsKey("LargeBoreCylinder")) {
            masterItem.addRDFType(domainClasses.get("LargeBoreCylinder"));
        }
        
        // Add stroke length classification
        String strokeLength = classification.get("strokeLength");
        if ("Short".equals(strokeLength) && domainClasses.containsKey("ShortStrokeCylinder")) {
            masterItem.addRDFType(domainClasses.get("ShortStrokeCylinder"));
        } else if ("Medium".equals(strokeLength) && domainClasses.containsKey("MediumStrokeCylinder")) {
            masterItem.addRDFType(domainClasses.get("MediumStrokeCylinder"));
        } else if ("Long".equals(strokeLength) && domainClasses.containsKey("LongStrokeCylinder")) {
            masterItem.addRDFType(domainClasses.get("LongStrokeCylinder"));
        }
        
        // Add series classification
        String series = classification.get("series");
        if ("Standard".equals(series) && domainClasses.containsKey("StandardCylinder")) {
            masterItem.addRDFType(domainClasses.get("StandardCylinder"));
        } else if ("Heavy Duty".equals(series) && domainClasses.containsKey("HeavyDutyCylinder")) {
            masterItem.addRDFType(domainClasses.get("HeavyDutyCylinder"));
        } else if ("Compact".equals(series) && domainClasses.containsKey("CompactCylinder")) {
            masterItem.addRDFType(domainClasses.get("CompactCylinder"));
        } else if ("Light Duty".equals(series) && domainClasses.containsKey("LightDutyCylinder")) {
            masterItem.addRDFType(domainClasses.get("LightDutyCylinder"));
        }
        
        // Add rod end type classification
        String rodEndType = classification.get("rodEndType");
        if ("Yoke".equals(rodEndType) && domainClasses.containsKey("YokeRodEndCylinder")) {
            masterItem.addRDFType(domainClasses.get("YokeRodEndCylinder"));
        } else if ("Internal Thread".equals(rodEndType) && domainClasses.containsKey("ThreadedRodEndCylinder")) {
            masterItem.addRDFType(domainClasses.get("ThreadedRodEndCylinder"));
        } else if ("External Thread".equals(rodEndType) && domainClasses.containsKey("ThreadedRodEndCylinder")) {
            masterItem.addRDFType(domainClasses.get("ThreadedRodEndCylinder"));
        } else if ("Pin".equals(rodEndType) && domainClasses.containsKey("PinRodEndCylinder")) {
            masterItem.addRDFType(domainClasses.get("PinRodEndCylinder"));
        }
    }
    
    /**
     * Add enhanced component items with specialized relationships
     */
    private void addEnhancedComponentItems(OntModel model, Individual masterItem, Map<String, Object> bomStructure,
                                         Map<String, OntClass> domainClasses, 
                                         Map<String, org.apache.jena.ontology.OntProperty> domainProperties) {
        
        Map<String, List<Map<String, Object>>> components = 
            (Map<String, List<Map<String, Object>>>) bomStructure.get("components");
        
        Map<String, Integer> defaultQuantities = 
            (Map<String, Integer>) bomStructure.get("defaultQuantities");
        
        if (components != null) {
            int sequence = 1;
            
            for (Map.Entry<String, List<Map<String, Object>>> entry : components.entrySet()) {
                String category = entry.getKey();
                List<Map<String, Object>> categoryComponents = entry.getValue();
                
                int defaultQty = defaultQuantities != null && defaultQuantities.containsKey(category) ? 
                    defaultQuantities.get(category) : 1;
                
                // Use the highest scored component in each category
                if (!categoryComponents.isEmpty()) {
                    Map<String, Object> component = categoryComponents.get(0); // Already sorted by score
                    
                    Individual componentItem = createEnhancedComponentItem(
                        model, component, category, domainClasses, domainProperties);
                    
                    // Create enhanced BOM relationship with additional properties
                    createEnhancedBomRelationship(model, masterItem, componentItem, component, 
                                                defaultQty, sequence, domainClasses, domainProperties);
                    
                    sequence++;
                }
            }
        }
    }
    
    /**
     * Create enhanced component item with specialized classification
     */
    private Individual createEnhancedComponentItem(OntModel model, Map<String, Object> component, String category,
                                                 Map<String, OntClass> domainClasses, 
                                                 Map<String, org.apache.jena.ontology.OntProperty> domainProperties) {
        
        String componentCode = (String) component.get("code");
        
        // Determine the appropriate component class based on category
        OntClass componentClass = getComponentClass(category, domainClasses);
        
        Individual componentItem = componentClass.createIndividual(BASE_URI + "Material_" + componentCode);
        
        // Add basic properties
        componentItem.addProperty(domainProperties.get("itemCode"), componentCode);
        componentItem.addProperty(domainProperties.get("itemName"), (String) component.get("name"));
        
        if (component.containsKey("description")) {
            componentItem.addProperty(domainProperties.get("itemSpec"), (String) component.get("description"));
        }
        
        // Add compatibility score as a property for reference
        if (component.containsKey("compatibilityScore")) {
            // We can add this as a custom property or annotation
            // For now, we'll add it as a comment
            String scoreComment = "Compatibility Score: " + component.get("compatibilityScore");
            componentItem.addComment(scoreComment, "en");
        }
        
        return componentItem;
    }
    
    /**
     * Get the appropriate component class based on category
     */
    private OntClass getComponentClass(String category, Map<String, OntClass> domainClasses) {
        switch (category) {
            case "CylinderBarrel":
                return domainClasses.getOrDefault("CylinderBarrel", domainClasses.get("ComponentItem"));
            case "Piston":
                return domainClasses.getOrDefault("Piston", domainClasses.get("ComponentItem"));
            case "PistonRod":
                return domainClasses.getOrDefault("PistonRod", domainClasses.get("ComponentItem"));
            case "SealingComponent":
            case "PistonSeal":
                return domainClasses.getOrDefault("PistonSeal", domainClasses.get("SealingComponent"));
            case "RodSeal":
                return domainClasses.getOrDefault("RodSeal", domainClasses.get("SealingComponent"));
            case "WiperSeal":
                return domainClasses.getOrDefault("WiperSeal", domainClasses.get("SealingComponent"));
            case "ORingSeal":
                return domainClasses.getOrDefault("ORingSeal", domainClasses.get("SealingComponent"));
            case "EndCap":
            case "HeadEndCap":
                return domainClasses.getOrDefault("HeadEndCap", domainClasses.get("EndCap"));
            case "RodEndCap":
                return domainClasses.getOrDefault("RodEndCap", domainClasses.get("EndCap"));
            case "Bushing":
            case "RodBushing":
                return domainClasses.getOrDefault("RodBushing", domainClasses.get("Bushing"));
            case "Gasket":
                return domainClasses.getOrDefault("Gasket", domainClasses.get("ComponentItem"));
            case "Fastener":
            case "TieRod":
                return domainClasses.getOrDefault("TieRod", domainClasses.get("Fastener"));
            default:
                return domainClasses.get("ComponentItem");
        }
    }
    
    /**
     * Create enhanced BOM relationship with additional semantic properties
     */
    private void createEnhancedBomRelationship(OntModel model, Individual masterItem, Individual componentItem,
                                             Map<String, Object> component, int quantity, int sequence,
                                             Map<String, OntClass> domainClasses, 
                                             Map<String, org.apache.jena.ontology.OntProperty> domainProperties) {
        
        String bomId = "BOM_" + masterItem.getLocalName().replace("Material_", "") + "_" + sequence + "_" + 
                      componentItem.getLocalName().replace("Material_", "");
        
        Individual bomRelation = domainClasses.get("BillOfMaterial").createIndividual(BASE_URI + bomId);
        
        // Basic BOM relationships
        bomRelation.addProperty(domainProperties.get("hasMasterItem"), masterItem);
        bomRelation.addProperty(domainProperties.get("hasComponentItem"), componentItem);
        bomRelation.addLiteral(domainProperties.get("quantity"), quantity);
        
        // Enhanced relationships using specialized properties
        masterItem.addProperty(domainProperties.get("hasComponent"), componentItem);
        componentItem.addProperty(domainProperties.get("isComponentOf"), masterItem);
        
        // Add compatibility relationship if score is high
        if (component.containsKey("compatibilityScore")) {
            Double score = (Double) component.get("compatibilityScore");
            if (score >= 0.7) {
                componentItem.addProperty(domainProperties.get("compatibleWith"), masterItem);
            }
            if (score >= 0.9) {
                componentItem.addProperty(domainProperties.get("recommendedFor"), masterItem);
            }
        }
    }
    
    /**
     * Extracts specifications from a hydraulic cylinder code with enhanced validation
     * 
     * @param itemCode The cylinder code
     * @return Map of specifications with validation
     */
    private Map<String, String> extractSpecificationsFromCode(String itemCode) {
        Map<String, String> specs = new HashMap<>();
        
        if (itemCode == null || itemCode.length() < 8) {
            logger.warn("Item code too short for specification extraction: {}", itemCode);
            return specs;
        }
        
        try {
            // Extract series (positions 3-4)
            if (itemCode.length() >= 4) {
                specs.put("series", itemCode.substring(2, 4));
            }
            
            // Extract type (position 5)
            if (itemCode.length() >= 5) {
                specs.put("type", itemCode.substring(4, 5));
            }
            
            // Extract bore (positions 6-8)
            if (itemCode.length() >= 8) {
                String boreStr = itemCode.substring(5, 8);
                // Remove leading zeros for numeric processing
                specs.put("bore", String.valueOf(Integer.parseInt(boreStr)));
            }
            
            // Extract stroke (positions 11-14)
            if (itemCode.length() >= 14) {
                String strokeStr = itemCode.substring(10, 14);
                // Remove leading zeros for numeric processing
                specs.put("stroke", String.valueOf(Integer.parseInt(strokeStr)));
            }
            
            // Extract rod end type (position 15)
            if (itemCode.length() >= 15) {
                specs.put("rodEndType", itemCode.substring(14, 15));
            }
            
            // Extract installation type if available (positions 16-17)
            if (itemCode.length() >= 17) {
                specs.put("installationType", itemCode.substring(15, 17));
            }
            
            // Extract shaft end join if available (position 18)
            if (itemCode.length() >= 18) {
                specs.put("shaftEndJoin", itemCode.substring(17, 18));
            }
            
        } catch (NumberFormatException e) {
            logger.error("Error parsing numeric values from item code: {}", itemCode, e);
        }
        
        return specs;
    }
    
    /**
     * Gets a reference ontology model with existing BOMs, enhanced with domain knowledge
     * 
     * @return The enhanced ontology model with existing BOMs
     */
    private OntModel getReferenceOntologyModel() {
        try {
            // Initialize the specialized ontology first
            hydraulicCylinderOntology.initializeHydraulicCylinderOntology();
            
            // Get the base domain model
            OntModel referenceModel = hydraulicCylinderOntology.getOntologyModel();
            
            // Find reference cylinders and add their BOMs to the model
            List<String> referenceCylinders = findReferenceCylinders();
            
            for (String referenceCode : referenceCylinders) {
                try {
                    // Get the BOM ontology for each reference cylinder
                    OntModel bomModel = bomOwlExportService.getBomOntologyForMasterItem(referenceCode);
                    if (bomModel != null) {
                        // Merge the BOM model into the reference model
                        referenceModel.add(bomModel);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to load BOM for reference cylinder {}: {}", referenceCode, e.getMessage());
                }
            }
            
            logger.info("Built reference ontology model with {} reference cylinders", referenceCylinders.size());
            return referenceModel;
            
        } catch (Exception e) {
            logger.error("Error building reference ontology model", e);
            // Return empty ontology model with domain knowledge
            hydraulicCylinderOntology.initializeHydraulicCylinderOntology();
            return hydraulicCylinderOntology.getOntologyModel();
        }
    }
    
    /**
     * Finds reference hydraulic cylinders from the database with enhanced filtering
     * 
     * @return List of cylinder codes sorted by relevance
     */
    private List<String> findReferenceCylinders() {
        List<String> cylinderCodes = new ArrayList<>();
        
        try {
            // Find items with hydraulic cylinder characteristics
            List<ImaFile> cylinders = imaRepository.findByIma09AndIma10("S", "130 HC");
            
            // Filter and prioritize cylinders
            Map<String, Integer> cylinderPriority = new HashMap<>();
            
            for (ImaFile cylinder : cylinders) {
                String code = cylinder.getIma01();
                if (code != null && isValidHydraulicCylinderCode(code)) {
                    cylinderCodes.add(code);
                    
                    // Calculate priority based on code characteristics
                    int priority = calculateCylinderPriority(code);
                    cylinderPriority.put(code, priority);
                    
                    // Limit to reasonable number for performance
                    if (cylinderCodes.size() >= 20) {
                        break;
                    }
                }
            }
            
            // Sort by priority (higher priority first)
            cylinderCodes.sort((a, b) -> Integer.compare(cylinderPriority.getOrDefault(b, 0), 
                                                        cylinderPriority.getOrDefault(a, 0)));
            
            // Keep top 10 for reference
            if (cylinderCodes.size() > 10) {
                cylinderCodes = cylinderCodes.subList(0, 10);
            }
            
        } catch (Exception e) {
            logger.error("Error finding reference cylinders", e);
        }
        
        return cylinderCodes;
    }
    
    /**
     * Check if a code represents a valid hydraulic cylinder
     */
    private boolean isValidHydraulicCylinderCode(String code) {
        // Basic validation for hydraulic cylinder codes
        return code != null && 
               code.length() >= 8 && 
               (code.startsWith("3") || code.startsWith("4")) &&
               code.matches("^[34]\\d{2}[0-9A-Z].*");
    }
    
    /**
     * Calculate priority for a cylinder code (higher = better reference)
     */
    private int calculateCylinderPriority(String code) {
        int priority = 0;
        
        try {
            Map<String, String> specs = extractSpecificationsFromCode(code);
            
            // Prefer standard series (10)
            if ("10".equals(specs.get("series"))) {
                priority += 10;
            }
            
            // Prefer medium bore sizes (more common)
            String bore = specs.get("bore");
            if (bore != null) {
                int boreValue = Integer.parseInt(bore);
                if (boreValue >= 50 && boreValue <= 100) {
                    priority += 5;
                }
            }
            
            // Prefer standard rod end types
            if ("Y".equals(specs.get("rodEndType"))) {
                priority += 3;
            }
            
            // Prefer complete codes (longer = more complete)
            priority += Math.min(code.length() - 8, 10);
            
        } catch (Exception e) {
            // Lower priority for codes that can't be parsed
            priority = 1;
        }
        
        return priority;
    }
    
    /**
     * Find similar cylinders using enhanced rules service with better error handling
     * 
     * @param newItemCode The new cylinder code
     * @param ontModel The ontology model
     * @return List of similar cylinders with enhanced similarity metrics
     */
    public List<Map<String, Object>> findReferenceCylinders(String newItemCode, OntModel ontModel) {
        try {
            return hydraulicCylinderRules.findEnhancedSimilarCylinders(newItemCode, ontModel);
        } catch (Exception e) {
            logger.error("Error finding reference cylinders for {}: {}", newItemCode, e.getMessage());
            // Return empty list if similarity search fails
            return new ArrayList<>();
        }
    }
    
    /**
     * Enhanced method to convert BOM structure for frontend with additional metadata
     * 
     * @param bomStructure The generated BOM structure
     * @return A comprehensive map for the frontend with enhanced information
     */
    public Map<String, Object> convertBomStructureForFrontend(Map<String, Object> bomStructure) {
        Map<String, Object> result = new HashMap<>();
        
        // Basic item info
        result.put("masterItemCode", bomStructure.get("masterItemCode"));
        result.put("itemName", bomStructure.get("itemName"));
        result.put("itemSpec", bomStructure.get("itemSpec"));
        
        // Enhanced specifications with classification
        result.put("specifications", bomStructure.get("specifications"));
        result.put("cylinderClassification", bomStructure.get("cylinderClassification"));
        
        // Validation results
        result.put("validationPassed", bomStructure.get("validationPassed"));
        result.put("validationWarnings", bomStructure.get("validationWarnings"));
        
        // Convert components format for frontend with enhanced information
        List<Map<String, Object>> componentsForFrontend = new ArrayList<>();
        
        Map<String, List<Map<String, Object>>> components = 
            (Map<String, List<Map<String, Object>>>) bomStructure.get("components");
        
        Map<String, Integer> defaultQuantities = 
            (Map<String, Integer>) bomStructure.get("defaultQuantities");
        
        if (components != null) {
            for (Map.Entry<String, List<Map<String, Object>>> entry : components.entrySet()) {
                String category = entry.getKey();
                List<Map<String, Object>> categoryComponents = entry.getValue();
                
                int defaultQty = defaultQuantities != null && defaultQuantities.containsKey(category) ? 
                    defaultQuantities.get(category) : 1;
                
                // Create enhanced component entry for this category
                Map<String, Object> categoryEntry = new HashMap<>();
                categoryEntry.put("category", category);
                categoryEntry.put("categoryDisplayName", getCategoryDisplayName(category));
                categoryEntry.put("categoryDescription", getCategoryDescription(category));
                categoryEntry.put("defaultQuantity", defaultQty);
                categoryEntry.put("options", categoryComponents);
                categoryEntry.put("isRequired", isCategoryRequired(category));
                categoryEntry.put("maxRecommendations", getMaxRecommendations(category));
                
                // Add category statistics
                if (!categoryComponents.isEmpty()) {
                    double avgScore = categoryComponents.stream()
                        .mapToDouble(comp -> (Double) comp.get("compatibilityScore"))
                        .average()
                        .orElse(0.0);
                    categoryEntry.put("averageCompatibilityScore", Math.round(avgScore * 100.0) / 100.0);
                    
                    Map<String, Object> bestOption = categoryComponents.get(0); // First is best scored
                    categoryEntry.put("recommendedOption", bestOption);
                }
                
                componentsForFrontend.add(categoryEntry);
            }
        }
        
        // Sort categories by importance
        componentsForFrontend.sort((a, b) -> {
            int priorityA = getCategoryPriority((String) a.get("category"));
            int priorityB = getCategoryPriority((String) b.get("category"));
            return Integer.compare(priorityB, priorityA); // Higher priority first
        });
        
        result.put("componentCategories", componentsForFrontend);
        
        // Enhanced similar cylinders with more details
        result.put("similarCylinders", bomStructure.get("similarCylinders"));
        
        // Additional metadata
        result.put("overallRecommendationScore", bomStructure.get("overallRecommendationScore"));
        result.put("componentStatistics", bomStructure.get("componentStatistics"));
        
        // Add generation metadata
        Map<String, Object> generationMetadata = new HashMap<>();
        generationMetadata.put("generatedAt", System.currentTimeMillis());
        generationMetadata.put("totalComponents", 
            components != null ? components.values().stream().mapToInt(List::size).sum() : 0);
        generationMetadata.put("totalCategories", components != null ? components.size() : 0);
        result.put("generationMetadata", generationMetadata);
        
        return result;
    }
    
    /**
     * Get user-friendly display name for component category
     */
    private String getCategoryDisplayName(String category) {
        switch (category) {
            case "CylinderBarrel": return "Cylinder Barrel";
            case "PistonRod": return "Piston Rod";
            case "SealingComponent": return "Sealing Components";
            case "PistonSeal": return "Piston Seals";
            case "RodSeal": return "Rod Seals";
            case "WiperSeal": return "Wiper Seals";
            case "ORingSeal": return "O-Ring Seals";
            case "EndCap": return "End Caps";
            case "HeadEndCap": return "Head End Caps";
            case "RodEndCap": return "Rod End Caps";
            case "RodBushing": return "Rod Bushings";
            case "TieRod": return "Tie Rods";
            default: return category;
        }
    }
    
    /**
     * Get description for component category
     */
    private String getCategoryDescription(String category) {
        switch (category) {
            case "CylinderBarrel": return "Main cylinder body that houses the piston";
            case "Piston": return "Moving component that creates pressure differential";
            case "PistonRod": return "Rod that extends from the piston to transfer force";
            case "PistonSeal": return "Seals that prevent fluid leakage around the piston";
            case "RodSeal": return "Seals that prevent fluid leakage around the rod";
            case "WiperSeal": return "Seals that prevent contamination from entering";
            case "ORingSeal": return "General purpose sealing rings";
            case "EndCap": return "Caps that close the cylinder ends";
            case "RodBushing": return "Bushings that guide and support the rod";
            case "Gasket": return "Gaskets for various sealing applications";
            case "TieRod": return "Rods that hold the cylinder assembly together";
            default: return "Component for hydraulic cylinder assembly";
        }
    }
    
    /**
     * Check if a category is required for cylinder assembly
     */
    private boolean isCategoryRequired(String category) {
        switch (category) {
            case "CylinderBarrel":
            case "Piston":
            case "PistonRod":
            case "EndCap":
            case "PistonSeal":
            case "RodSeal":
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Get maximum number of recommendations to show for a category
     */
    private int getMaxRecommendations(String category) {
        switch (category) {
            case "CylinderBarrel":
            case "Piston":
            case "PistonRod":
                return 3; // Few options for main components
            case "SealingComponent":
            case "PistonSeal":
            case "RodSeal":
            case "ORingSeal":
                return 5; // More options for seals
            default:
                return 3;
        }
    }
    
    /**
     * Get priority for category ordering (higher = more important)
     */
    private int getCategoryPriority(String category) {
        switch (category) {
            case "CylinderBarrel": return 100;
            case "Piston": return 90;
            case "PistonRod": return 80;
            case "EndCap":
            case "HeadEndCap":
            case "RodEndCap": return 70;
            case "PistonSeal": return 60;
            case "RodSeal": return 55;
            case "WiperSeal": return 50;
            case "RodBushing": return 40;
            case "ORingSeal": return 30;
            case "Gasket": return 20;
            case "TieRod": return 10;
            default: return 5;
        }
    }
}
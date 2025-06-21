package com.jfc.owl.service;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.reasoner.ValidityReport;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.jfc.owl.ontology.HydraulicCylinderOntology;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for performing reasoning on ontology models.
 */
@Service
public class ReasoningService {
	private static final Logger logger = LoggerFactory.getLogger(ReasoningService.class);

    @Autowired
    private BomOwlExportService bomOwlExportService;
    
    @Autowired
    private EnhancedHydraulicCylinderRules hydraulicCylinderRules;
    
    @Autowired
    private HydraulicCylinderOntology hydraulicCylinderOntology;

    @Value("${owl.ontology.base-path}")
    private String ontologyBasePath;

    /**
     * Enhanced reasoning that integrates hydraulic cylinder domain knowledge
     * 
     * @param masterItemCode The code of the master item to reason about
     * @param reasonerType   The type of reasoner to use
     * @return A comprehensive ReasoningResult with enhanced inferences
     */
    public Map<String, Object> performReasoning(String masterItemCode, String reasonerType) {
        Map<String, Object> results = new HashMap<>(); 
        
        try {
            logger.info("Performing enhanced reasoning for {} with reasoner type: {}", masterItemCode, reasonerType);
            
            // Get the base ontology model
            OntModel ontModel = bomOwlExportService.getBomOntologyForMasterItem(masterItemCode);
            
            // Check if this is a hydraulic cylinder and enhance accordingly
            boolean isHydraulicCylinder = isHydraulicCylinderItem(masterItemCode);
            
            if (isHydraulicCylinder) {
                // Initialize and merge hydraulic cylinder ontology
                hydraulicCylinderOntology.initializeHydraulicCylinderOntology();
                ontModel.add(hydraulicCylinderOntology.getOntologyModel());
                logger.debug("Merged hydraulic cylinder domain ontology");
            }
            
            // Create enhanced reasoner
            Reasoner reasoner = createEnhancedReasoner(reasonerType, isHydraulicCylinder);
            
            // Apply reasoning
            InfModel infModel = ModelFactory.createInfModel(reasoner, ontModel);
            
            // Validate the model with enhanced validation
            ValidityReport validity = infModel.validate();
            boolean isValid = validity.isValid();
            results.put("isValid", isValid);
            
            // Enhanced validation reporting
            List<Map<String, Object>> validationIssues = processValidationIssues(validity);
            results.put("validationIssues", validationIssues);
            
            // Extract comprehensive inferred statements
            List<Map<String, String>> inferredStatements = extractInferredStatements(infModel, ontModel);
            results.put("inferredStatements", inferredStatements);
            
            // Extract enhanced subclass relationships
            List<Map<String, String>> inferredSubclasses = extractInferredSubclasses(infModel, ontModel);
            results.put("inferredSubclasses", inferredSubclasses);
            
            // Enhanced BOM hierarchy extraction
            results.put("bomHierarchy", extractEnhancedBomHierarchy(masterItemCode, infModel, isHydraulicCylinder));
            
            // Add hydraulic cylinder specific inferences if applicable
            if (isHydraulicCylinder) {
                addHydraulicCylinderInferences(results, masterItemCode, infModel);
            }
            
            // Add reasoning performance metrics
            addReasoningMetrics(results, infModel, ontModel);
            
            logger.info("Enhanced reasoning completed for {} with {} inferences", 
                       masterItemCode, inferredStatements.size());
            
            return results;
            
        } catch (Exception e) {
            logger.error("Error in enhanced reasoning for " + masterItemCode, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            errorResult.put("stackTrace", Arrays.toString(e.getStackTrace()));
            return errorResult;
        }
    }
    
    /**
     * Check if the item is a hydraulic cylinder
     */
    private boolean isHydraulicCylinderItem(String itemCode) {
        return itemCode != null && 
               itemCode.length() >= 2 && 
               (itemCode.startsWith("3") || itemCode.startsWith("4"));
    }
    
    /**
     * Create enhanced reasoner with optional hydraulic cylinder rules
     */
    private Reasoner createEnhancedReasoner(String reasonerType, boolean includeHydraulicRules) {
        Reasoner baseReasoner;
        
        switch (reasonerType) {
            case "OWL_MINI":
                baseReasoner = ReasonerRegistry.getOWLMiniReasoner();
                break;
            case "OWL_MICRO":
                baseReasoner = ReasonerRegistry.getOWLMicroReasoner();
                break;
            case "RDFS":
                baseReasoner = ReasonerRegistry.getRDFSReasoner();
                break;
            case "ENHANCED_HYDRAULIC":
                // Create rule-based reasoner with hydraulic cylinder rules
                if (includeHydraulicRules) {
                    List<String> ruleTexts = hydraulicCylinderRules.getEnhancedHydraulicCylinderRules();
                    List<Rule> rules = new ArrayList<>();
                    
                    for (String ruleText : ruleTexts) {
                        try {
                            rules.add(Rule.parseRule(ruleText));
                        } catch (Exception e) {
                            logger.warn("Failed to parse hydraulic cylinder rule: {}", e.getMessage());
                        }
                    }
                    
                    GenericRuleReasoner ruleReasoner = new GenericRuleReasoner(rules);
                    ruleReasoner.setOWLTranslation(true);
                    ruleReasoner.setTransitiveClosureCaching(true);
                    return ruleReasoner;
                }
                // Fall through to default
            case "OWL":
            default:
                baseReasoner = ReasonerRegistry.getOWLReasoner();
                break;
        }
        
        return baseReasoner;
    }
    
    /**
     * Process validation issues with enhanced categorization
     */
    private List<Map<String, Object>> processValidationIssues(ValidityReport validity) {
        List<Map<String, Object>> validationIssues = new ArrayList<>();
        
        if (!validity.isValid()) {
            Iterator<ValidityReport.Report> reports = validity.getReports();
            while (reports.hasNext()) {
                ValidityReport.Report report = reports.next();
                Map<String, Object> issue = new HashMap<>();
                issue.put("type", report.getType());
                issue.put("description", report.getDescription());
                issue.put("severity", categorizeSeverity(report.getType()));
                
                if (report.getExtension() != null) {
                    issue.put("extension", report.getExtension().toString());
                }
                
                validationIssues.add(issue);
            }
        }
        
        return validationIssues;
    }
    
    /**
     * Categorize validation issue severity
     */
    private String categorizeSeverity(String issueType) {
        if (issueType.toLowerCase().contains("error") || issueType.toLowerCase().contains("inconsistent")) {
            return "ERROR";
        } else if (issueType.toLowerCase().contains("warn")) {
            return "WARNING";
        } else {
            return "INFO";
        }
    }
    
    /**
     * Extract inferred statements with filtering and categorization
     */
    private List<Map<String, String>> extractInferredStatements(InfModel infModel, OntModel ontModel) {
        List<Map<String, String>> inferredStatements = new ArrayList<>();
        
        StmtIterator stmtIterator = infModel.listStatements();
        while (stmtIterator.hasNext()) {
            Statement stmt = stmtIterator.next();
            
            // Only include inferred statements that are not in the original model
            if (!ontModel.contains(stmt)) {
                Map<String, String> statementMap = new HashMap<>();
                statementMap.put("subject", stmt.getSubject().toString());
                statementMap.put("predicate", stmt.getPredicate().toString());
                statementMap.put("object", stmt.getObject().toString());
                statementMap.put("category", categorizeInference(stmt));
                
                inferredStatements.add(statementMap);
            }
        }
        
        return inferredStatements;
    }
    
    /**
     * Categorize inference types for better understanding
     */
    private String categorizeInference(Statement stmt) {
        String predicate = stmt.getPredicate().getLocalName();
        String object = stmt.getObject().toString();
        
        if ("type".equals(predicate)) {
            if (object.contains("Cylinder")) {
                return "Cylinder Classification";
            } else if (object.contains("Component")) {
                return "Component Classification";
            } else {
                return "Type Inference";
            }
        } else if ("subClassOf".equals(predicate)) {
            return "Class Hierarchy";
        } else if (predicate.contains("compatible") || predicate.contains("recommended")) {
            return "Compatibility";
        } else if (predicate.contains("has") || predicate.contains("requires")) {
            return "Relationship";
        } else {
            return "Property Inference";
        }
    }
    
    /**
     * Extract inferred subclass relationships with enhanced analysis
     */
    private List<Map<String, String>> extractInferredSubclasses(InfModel infModel, OntModel ontModel) {
        List<Map<String, String>> inferredSubclasses = new ArrayList<>();
        
        StmtIterator subClassStmts = infModel.listStatements(null, RDFS.subClassOf, (org.apache.jena.rdf.model.RDFNode)null);
        while (subClassStmts.hasNext()) {
            Statement stmt = subClassStmts.next();
            
            // Check if this subclass relationship is inferred
            if (!ontModel.contains(stmt)) {
                Map<String, String> subclassMap = new HashMap<>();
                subclassMap.put("subclass", stmt.getSubject().toString());
                subclassMap.put("superclass", stmt.getObject().toString());
                subclassMap.put("confidence", calculateInferenceConfidence(stmt));
                
                inferredSubclasses.add(subclassMap);
            }
        }
        
        return inferredSubclasses;
    }
    
    /**
     * Calculate confidence level for inferences
     */
    private String calculateInferenceConfidence(Statement stmt) {
        // Simple heuristic based on namespace and content
        String subject = stmt.getSubject().toString();
        String object = stmt.getObject().toString();
        
        if (subject.contains("hydraulic-cylinder") && object.contains("hydraulic-cylinder")) {
            return "HIGH";
        } else if (subject.contains("ontology") && object.contains("ontology")) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
    
    /**
     * Extract enhanced BOM hierarchy with hydraulic cylinder specific information
     */
    private Map<String, Object> extractEnhancedBomHierarchy(String masterItemCode, InfModel infModel, boolean isHydraulicCylinder) {
        Map<String, Object> hierarchy = new HashMap<>();
        
        // Get the master item URI
        String masterItemUri = "http://www.jfc.com/tiptop/ontology#Material_" + masterItemCode;
        
        // Extract master item details
        hierarchy.put("code", masterItemCode);
        hierarchy.put("uri", masterItemUri);
        hierarchy.put("isHydraulicCylinder", isHydraulicCylinder);
        
        // Extract enhanced properties
        Map<String, List<String>> enhancedProperties = extractEnhancedProperties(infModel, masterItemUri, isHydraulicCylinder);
        hierarchy.put("enhancedProperties", enhancedProperties);
        
        // Extract component relationships
        List<Map<String, Object>> components = extractEnhancedComponents(infModel, masterItemUri);
        hierarchy.put("components", components);
        
        // Add hydraulic cylinder specific hierarchy if applicable
        if (isHydraulicCylinder) {
            addHydraulicCylinderHierarchy(hierarchy, infModel, masterItemUri);
        }
        
        return hierarchy;
    }
    
    /**
     * Extract enhanced properties including hydraulic cylinder specific ones
     */
    private Map<String, List<String>> extractEnhancedProperties(InfModel infModel, String masterItemUri, boolean isHydraulicCylinder) {
        Map<String, List<String>> properties = new HashMap<>();
        
        StmtIterator propStmts = infModel.listStatements(infModel.getResource(masterItemUri), null, (org.apache.jena.rdf.model.RDFNode) null);
        while (propStmts.hasNext()) {
            Statement stmt = propStmts.next();
            String predicate = stmt.getPredicate().getLocalName();
            String value = stmt.getObject().toString();
            
            properties.computeIfAbsent(predicate, k -> new ArrayList<>()).add(value);
        }
        
        // Add hydraulic cylinder specific properties if applicable
        if (isHydraulicCylinder) {
            addHydraulicCylinderProperties(properties, infModel, masterItemUri);
        }
        
        return properties;
    }
    
    /**
     * Add hydraulic cylinder specific properties
     */
    private void addHydraulicCylinderProperties(Map<String, List<String>> properties, InfModel infModel, String masterItemUri) {
        // Extract hydraulic cylinder specific properties
        String[] hcProperties = {"bore", "stroke", "series", "rodEndType", "installationType", "cylinderType"};
        
        for (String hcProp : hcProperties) {
            String propUri = "http://www.jfc.com/tiptop/hydraulic-cylinder#" + hcProp;
            StmtIterator hcPropStmts = infModel.listStatements(
                infModel.getResource(masterItemUri), 
                infModel.getProperty(propUri), 
                (org.apache.jena.rdf.model.RDFNode) null);
            
            while (hcPropStmts.hasNext()) {
                Statement stmt = hcPropStmts.next();
                String value = stmt.getObject().toString();
                properties.computeIfAbsent("hc_" + hcProp, k -> new ArrayList<>()).add(value);
            }
        }
    }
    
    /**
     * Extract enhanced component information
     */
    private List<Map<String, Object>> extractEnhancedComponents(InfModel infModel, String masterItemUri) {
        List<Map<String, Object>> components = new ArrayList<>();
        String bomPrefix = "http://www.jfc.com/tiptop/ontology#BOM_";
        
        // Find BOM entries for this master item
        StmtIterator boms = infModel.listStatements(null, 
                                                   infModel.getProperty("http://www.jfc.com/tiptop/ontology#hasMasterItem"), 
                                                   infModel.getResource(masterItemUri));
        
        while (boms.hasNext()) {
            Statement bomStmt = boms.next();
            String bomUri = bomStmt.getSubject().getURI();
            
            Map<String, Object> component = extractComponentDetails(infModel, bomUri);
            if (!component.isEmpty()) {
                components.add(component);
            }
        }
        
        return components;
    }
    
    /**
     * Extract detailed component information from BOM entry
     */
    private Map<String, Object> extractComponentDetails(InfModel infModel, String bomUri) {
        Map<String, Object> component = new HashMap<>();
        
        try {
            // Get component item
            StmtIterator componentItems = infModel.listStatements(infModel.getResource(bomUri),
                                                                 infModel.getProperty("http://www.jfc.com/tiptop/ontology#hasComponentItem"),
                                                                 (org.apache.jena.rdf.model.RDFNode) null);
            
            if (componentItems.hasNext()) {
                Statement componentStmt = componentItems.next();
                String componentUri = componentStmt.getObject().asResource().getURI();
                
                component.put("uri", componentUri);
                component.put("bomUri", bomUri);
                
                // Extract component code
                String componentCode = componentUri.substring(componentUri.lastIndexOf("_") + 1);
                component.put("code", componentCode);
                
                // Extract properties
                Map<String, String> componentProps = extractComponentProperties(infModel, componentUri);
                component.putAll(componentProps);
                
                // Extract BOM specific properties
                Map<String, String> bomProps = extractBomProperties(infModel, bomUri);
                component.putAll(bomProps);
                
                // Add inferred compatibility information
                List<String> compatibilityInfo = extractCompatibilityInfo(infModel, componentUri);
                if (!compatibilityInfo.isEmpty()) {
                    component.put("compatibilityInfo", compatibilityInfo);
                }
            }
        } catch (Exception e) {
            logger.warn("Error extracting component details from BOM {}: {}", bomUri, e.getMessage());
        }
        
        return component;
    }
    
    /**
     * Extract component properties
     */
    private Map<String, String> extractComponentProperties(InfModel infModel, String componentUri) {
        Map<String, String> props = new HashMap<>();
        
        String[] basicProps = {"itemName", "itemSpec", "itemCode"};
        for (String prop : basicProps) {
            String propUri = "http://www.jfc.com/tiptop/ontology#" + prop;
            StmtIterator propStmts = infModel.listStatements(
                infModel.getResource(componentUri),
                infModel.getProperty(propUri),
                (org.apache.jena.rdf.model.RDFNode) null);
            
            if (propStmts.hasNext()) {
                props.put(prop, propStmts.next().getObject().toString());
            }
        }
        
        return props;
    }
    
    /**
     * Extract BOM specific properties
     */
    private Map<String, String> extractBomProperties(InfModel infModel, String bomUri) {
        Map<String, String> props = new HashMap<>();
        
        String[] bomProps = {"quantity", "effectiveDate", "expiryDate"};
        for (String prop : bomProps) {
            String propUri = "http://www.jfc.com/tiptop/ontology#" + prop;
            StmtIterator propStmts = infModel.listStatements(
                infModel.getResource(bomUri),
                infModel.getProperty(propUri),
                (org.apache.jena.rdf.model.RDFNode) null);
            
            if (propStmts.hasNext()) {
                props.put(prop, propStmts.next().getObject().toString());
            }
        }
        
        return props;
    }
    
    /**
     * Extract compatibility information for components
     */
    private List<String> extractCompatibilityInfo(InfModel infModel, String componentUri) {
        List<String> compatibilityInfo = new ArrayList<>();
        
        // Check for compatibility relationships
        String[] compatProps = {"compatibleWith", "recommendedFor", "isComponentOf"};
        
        for (String prop : compatProps) {
            String propUri = "http://www.jfc.com/tiptop/hydraulic-cylinder#" + prop;
            StmtIterator compatStmts = infModel.listStatements(
                infModel.getResource(componentUri),
                infModel.getProperty(propUri),
                (org.apache.jena.rdf.model.RDFNode) null);
            
            while (compatStmts.hasNext()) {
                Statement stmt = compatStmts.next();
                String target = stmt.getObject().toString();
                compatibilityInfo.add(prop + ": " + target);
            }
        }
        
        return compatibilityInfo;
    }
    
    /**
     * Add hydraulic cylinder specific hierarchy information
     */
    private void addHydraulicCylinderHierarchy(Map<String, Object> hierarchy, InfModel infModel, String masterItemUri) {
        Map<String, Object> hcHierarchy = new HashMap<>();
        
        // Extract cylinder classifications
        List<String> classifications = extractCylinderClassifications(infModel, masterItemUri);
        hcHierarchy.put("classifications", classifications);
        
        // Extract component categories
        Map<String, List<String>> componentCategories = extractComponentCategories(infModel, masterItemUri);
        hcHierarchy.put("componentCategories", componentCategories);
        
        // Extract performance characteristics
        Map<String, String> performance = extractPerformanceCharacteristics(infModel, masterItemUri);
        hcHierarchy.put("performanceCharacteristics", performance);
        
        hierarchy.put("hydraulicCylinderHierarchy", hcHierarchy);
    }
    
    /**
     * Extract cylinder classifications from reasoning
     */
    private List<String> extractCylinderClassifications(InfModel infModel, String masterItemUri) {
        List<String> classifications = new ArrayList<>();
        
        StmtIterator typeStmts = infModel.listStatements(
            infModel.getResource(masterItemUri), 
            infModel.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), 
            (org.apache.jena.rdf.model.RDFNode)null);
        
        while (typeStmts.hasNext()) {
            Statement stmt = typeStmts.next();
            String typeUri = stmt.getObject().toString();
            
            if (typeUri.contains("hydraulic-cylinder") || typeUri.contains("Cylinder")) {
                String classification = typeUri.substring(typeUri.lastIndexOf("#") + 1);
                if (!classification.equals("HydraulicCylinder") && !classification.equals("Material")) {
                    classifications.add(classification);
                }
            }
        }
        
        return classifications;
    }
    
    /**
     * Extract component categories for hydraulic cylinders
     */
    private Map<String, List<String>> extractComponentCategories(InfModel infModel, String masterItemUri) {
        Map<String, List<String>> categories = new HashMap<>();
        
        // This would analyze the components and categorize them
        // Implementation depends on the specific component classification logic
        
        return categories;
    }
    
    /**
     * Extract performance characteristics
     */
    private Map<String, String> extractPerformanceCharacteristics(InfModel infModel, String masterItemUri) {
        Map<String, String> performance = new HashMap<>();
        
        String[] perfProps = {"maxPressure", "maxSpeed", "operatingTemperature", "cycleLife"};
        
        for (String prop : perfProps) {
            String propUri = "http://www.jfc.com/tiptop/hydraulic-cylinder#" + prop;
            StmtIterator perfStmts = infModel.listStatements(
                infModel.getResource(masterItemUri),
                infModel.getProperty(propUri),
                (org.apache.jena.rdf.model.RDFNode) null);
            
            if (perfStmts.hasNext()) {
                performance.put(prop, perfStmts.next().getObject().toString());
            }
        }
        
        return performance;
    }
    
    /**
     * Add hydraulic cylinder specific inferences to results
     */
    private void addHydraulicCylinderInferences(Map<String, Object> results, String masterItemCode, InfModel infModel) {
        Map<String, Object> hcInferences = new HashMap<>();
        
        try {
        	// Get the base OntModel - need to cast or get the appropriate model
            OntModel ontModel = null;
            
            // If the raw model is an OntModel, cast it
            if (infModel.getRawModel() instanceof OntModel) {
                ontModel = (OntModel) infModel.getRawModel();
            } else {
                // Otherwise, create an OntModel from the raw model
                ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, infModel.getRawModel());
            }
            
            // Get component suggestions using enhanced rules
            Map<String, List<Map<String, Object>>> componentSuggestions = 
                hydraulicCylinderRules.suggestEnhancedComponents(masterItemCode, ontModel);
            
            hcInferences.put("componentSuggestions", componentSuggestions);
            
            // Find similar cylinders
            List<Map<String, Object>> similarCylinders = 
                hydraulicCylinderRules.findEnhancedSimilarCylinders(masterItemCode, ontModel);
            
            hcInferences.put("similarCylinders", similarCylinders);
            
            // Extract cylinder specifications
            Map<String, String> specifications = extractCylinderSpecifications(infModel, masterItemCode);
            hcInferences.put("specifications", specifications);
            
            results.put("hydraulicCylinderInferences", hcInferences);
            
        } catch (Exception e) {
            logger.warn("Error adding hydraulic cylinder inferences: {}", e.getMessage());
        }
    }
    
    /**
     * Extract cylinder specifications from the inference model
     */
    private Map<String, String> extractCylinderSpecifications(InfModel infModel, String masterItemCode) {
        Map<String, String> specs = new HashMap<>();
        
        String masterItemUri = "http://www.jfc.com/tiptop/ontology#Material_" + masterItemCode;
        String[] specProps = {"bore", "stroke", "series", "rodEndType", "installationType", "cylinderType"};
        
        for (String prop : specProps) {
            String propUri = "http://www.jfc.com/tiptop/hydraulic-cylinder#" + prop;
            StmtIterator specStmts = infModel.listStatements(
                infModel.getResource(masterItemUri),
                infModel.getProperty(propUri),
                (org.apache.jena.rdf.model.RDFNode) null);
            
            if (specStmts.hasNext()) {
                specs.put(prop, specStmts.next().getObject().toString());
            }
        }
        
        return specs;
    }
    
    /**
     * Add reasoning performance metrics
     */
    private void addReasoningMetrics(Map<String, Object> results, InfModel infModel, OntModel ontModel) {
        Map<String, Object> metrics = new HashMap<>();
        
        // Basic metrics
        metrics.put("originalStatements", ontModel.size());
        metrics.put("inferredStatements", infModel.size() - ontModel.size());
        metrics.put("totalStatements", infModel.size());
        
        // Performance ratios
        double inferenceRatio = ontModel.size() > 0 ? 
            (double)(infModel.size() - ontModel.size()) / ontModel.size() : 0.0;
        metrics.put("inferenceRatio", Math.round(inferenceRatio * 100.0) / 100.0);
        
        // Reasoning completeness
        metrics.put("reasoningCompleteness", calculateReasoningCompleteness(infModel, ontModel));
        
        results.put("reasoningMetrics", metrics);
    }
    
    /**
     * Calculate reasoning completeness score
     */
    private double calculateReasoningCompleteness(InfModel infModel, OntModel ontModel) {
        // Simple heuristic based on the number of type inferences vs total entities
        long entities = ontModel.listSubjects().toList().size();
        long typeInferences = infModel.listStatements(null, 
            infModel.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), 
            (org.apache.jena.rdf.model.RDFNode)null).toList().size();
        
        return entities > 0 ? Math.min(1.0, (double)typeInferences / entities) : 0.0;
    }
    
    /**
     * Enhanced SPARQL query execution with hydraulic cylinder support
     * 
     * @param masterItemCode The code of the master item
     * @param queryString    The SPARQL query to execute
     * @return Enhanced query results with domain-specific information
     */
    public Map<String, Object> executeSparqlQuery(String masterItemCode, String queryString) {
        Map<String, Object> results = new HashMap<>();
        
        try {
            logger.info("Executing enhanced SPARQL query for: {}", masterItemCode);
            
            // Get enhanced ontology model
            OntModel ontModel = bomOwlExportService.getBomOntologyForMasterItem(masterItemCode);
            
            // Check if hydraulic cylinder and enhance model
            boolean isHydraulicCylinder = isHydraulicCylinderItem(masterItemCode);
            if (isHydraulicCylinder) {
                hydraulicCylinderOntology.initializeHydraulicCylinderOntology();
                ontModel.add(hydraulicCylinderOntology.getOntologyModel());
            }
            
            // Apply enhanced reasoning
            Reasoner reasoner = createEnhancedReasoner("ENHANCED_HYDRAULIC", isHydraulicCylinder);
            InfModel infModel = ModelFactory.createInfModel(reasoner, ontModel);
            
            // Replace placeholder with actual master item code
            String enhancedQuery = queryString.replace("COMPONENT_CODE", masterItemCode);
            
            // Execute query
            Query query = QueryFactory.create(enhancedQuery);
            try (QueryExecution qexec = QueryExecutionFactory.create(query, infModel)) {
                
                if (query.isSelectType()) {
                    ResultSet rs = qexec.execSelect();
                    
                    List<Map<String, String>> queryResults = new ArrayList<>();
                    List<String> variables = rs.getResultVars();
                    
                    while (rs.hasNext()) {
                        QuerySolution solution = rs.next();
                        Map<String, String> row = new HashMap<>();
                        
                        for (String var : variables) {
                            if (solution.contains(var)) {
                                row.put(var, solution.get(var).toString());
                            } else {
                                row.put(var, null);
                            }
                        }
                        
                        queryResults.add(row);
                    }
                    
                    results.put("type", "SELECT");
                    results.put("variables", variables);
                    results.put("results", queryResults);
                    results.put("resultCount", queryResults.size());
                    
                } else if (query.isConstructType()) {
                    org.apache.jena.rdf.model.Model constructModel = qexec.execConstruct();
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    constructModel.write(outputStream, "TURTLE");
                    
                    results.put("type", "CONSTRUCT");
                    results.put("model", outputStream.toString());
                    results.put("statementCount", constructModel.size());
                    
                } else if (query.isAskType()) {
                    boolean askResult = qexec.execAsk();
                    
                    results.put("type", "ASK");
                    results.put("result", askResult);
                    
                } else if (query.isDescribeType()) {
                    org.apache.jena.rdf.model.Model describeModel = qexec.execDescribe();
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    describeModel.write(outputStream, "TURTLE");
                    
                    results.put("type", "DESCRIBE");
                    results.put("model", outputStream.toString());
                    results.put("statementCount", describeModel.size());
                }
            }
            
            // Add query analysis
            results.put("queryAnalysis", analyzeQuery(enhancedQuery, isHydraulicCylinder));
            
            return results;
            
        } catch (Exception e) {
            logger.error("Error executing enhanced SPARQL query", e);
            results.put("error", e.getMessage());
            results.put("stackTrace", Arrays.toString(e.getStackTrace()));
            return results;
        }
    }
    
    /**
     * Analyze SPARQL query for insights
     */
    private Map<String, Object> analyzeQuery(String queryString, boolean isHydraulicCylinder) {
        Map<String, Object> analysis = new HashMap<>();
        
        // Basic query analysis
        analysis.put("isHydraulicCylinderQuery", isHydraulicCylinder);
        analysis.put("queryLength", queryString.length());
        
        // Count different SPARQL features
        analysis.put("hasFilter", queryString.toUpperCase().contains("FILTER"));
        analysis.put("hasOptional", queryString.toUpperCase().contains("OPTIONAL"));
        analysis.put("hasUnion", queryString.toUpperCase().contains("UNION"));
        analysis.put("hasGroupBy", queryString.toUpperCase().contains("GROUP BY"));
        analysis.put("hasOrderBy", queryString.toUpperCase().contains("ORDER BY"));
        
        // Hydraulic cylinder specific analysis
        if (isHydraulicCylinder) {
            analysis.put("usesHydraulicProperties", 
                queryString.contains("bore") || queryString.contains("stroke") || queryString.contains("series"));
            analysis.put("usesCompatibilityProperties", 
                queryString.contains("compatible") || queryString.contains("recommended"));
        }
        
        return analysis;
    }
    
    /**
     * Enhanced custom rules application with hydraulic cylinder integration
     * 
     * @param masterItemCode The code of the master item
     * @param rules          The rules in Jena rule syntax
     * @return Enhanced reasoning results
     */
    public Map<String, Object> applyCustomRules(String masterItemCode, String rules) {
        Map<String, Object> results = new HashMap<>();
        
        try {
            logger.info("Applying enhanced custom rules for: {}", masterItemCode);
            
            // Get enhanced ontology model
            OntModel ontModel = bomOwlExportService.getBomOntologyForMasterItem(masterItemCode);
            
            // Check if hydraulic cylinder and enhance model
            boolean isHydraulicCylinder = isHydraulicCylinderItem(masterItemCode);
            if (isHydraulicCylinder) {
                hydraulicCylinderOntology.initializeHydraulicCylinderOntology();
                ontModel.add(hydraulicCylinderOntology.getOntologyModel());
                
                // Optionally merge with hydraulic cylinder rules
                String enhancedRules = mergeWithHydraulicRules(rules);
                rules = enhancedRules;
            }
            
            // Create rule-based reasoner
            List<Rule> ruleList = Rule.parseRules(rules);
            Reasoner ruleReasoner = new GenericRuleReasoner(ruleList);
            
            // Bind the reasoner to the model
            InfModel infModel = ModelFactory.createInfModel(ruleReasoner, ontModel);
            
            // Extract inferred statements
            List<Map<String, String>> inferredStatements = extractInferredStatements(infModel, ontModel);
            
            results.put("appliedRules", ruleList.size());
            results.put("inferredStatements", inferredStatements);
            results.put("isHydraulicCylinder", isHydraulicCylinder);
            
            // Add rule analysis
            results.put("ruleAnalysis", analyzeRules(rules, isHydraulicCylinder));
            
            return results;
            
        } catch (Exception e) {
            logger.error("Error applying enhanced custom rules", e);
            results.put("error", e.getMessage());
            results.put("stackTrace", Arrays.toString(e.getStackTrace()));
            return results;
        }
    }
    
    /**
     * Merge custom rules with hydraulic cylinder rules if applicable
     */
    private String mergeWithHydraulicRules(String customRules) {
        try {
            List<String> hcRules = hydraulicCylinderRules.getEnhancedHydraulicCylinderRules();
            StringBuilder mergedRules = new StringBuilder(customRules);
            
            mergedRules.append("\n\n// Enhanced Hydraulic Cylinder Rules\n");
            for (String hcRule : hcRules) {
                mergedRules.append(hcRule).append("\n");
            }
            
            return mergedRules.toString();
            
        } catch (Exception e) {
            logger.warn("Error merging with hydraulic cylinder rules: {}", e.getMessage());
            return customRules;
        }
    }
    
    /**
     * Analyze rules for insights
     */
    private Map<String, Object> analyzeRules(String rules, boolean isHydraulicCylinder) {
        Map<String, Object> analysis = new HashMap<>();
        
        // Count rule types
        long conditionalRules = rules.lines().filter(line -> line.contains("->")).count();
        long complexRules = rules.lines().filter(line -> 
            line.contains("regex") || line.contains("greaterThan") || line.contains("lessThan")).count();
        
        analysis.put("totalRules", conditionalRules);
        analysis.put("complexRules", complexRules);
        analysis.put("ruleComplexity", complexRules > 0 ? "HIGH" : "STANDARD");
        
        // Hydraulic cylinder specific analysis
        if (isHydraulicCylinder) {
            long hcSpecificRules = rules.lines().filter(line -> 
                line.contains("Cylinder") || line.contains("bore") || line.contains("stroke")).count();
            analysis.put("hydraulicCylinderRules", hcSpecificRules);
            analysis.put("domainSpecific", hcSpecificRules > 0);
        }
        
        // Rule pattern analysis
        analysis.put("usesRegex", rules.contains("regex"));
        analysis.put("usesComparisons", rules.contains("greaterThan") || rules.contains("lessThan"));
        analysis.put("usesCompatibility", rules.contains("compatible") || rules.contains("recommended"));
        
        return analysis;
    }
    
    /**
     * Enhanced predefined queries with hydraulic cylinder specific queries
     * 
     * @return A comprehensive list of predefined queries
     */
    public List<Map<String, String>> getPredefinedQueries() {
        List<Map<String, String>> queries = new ArrayList<>();
        
        // Add original queries
        queries.addAll(getBasicQueries());
        
        // Add enhanced hydraulic cylinder queries
        queries.addAll(getHydraulicCylinderQueries());
        
        // Add advanced reasoning queries
        queries.addAll(getAdvancedReasoningQueries());
        
        return queries;
    }
    
    /**
     * Get basic BOM queries
     */
    private List<Map<String, String>> getBasicQueries() {
        List<Map<String, String>> queries = new ArrayList<>();
        
        Map<String, String> query1 = new HashMap<>();
        query1.put("name", "List All Components");
        query1.put("description", "Lists all component items used in any BOM");
        query1.put("category", "Basic");
        query1.put("query", "PREFIX j: <http://www.jfc.com/tiptop/ontology#>\n" +
                           "SELECT ?component ?name ?spec\n" +
                           "WHERE {\n" +
                           "  ?component a j:ComponentItem .\n" +
                           "  ?component j:itemName ?name .\n" +
                           "  ?component j:itemSpec ?spec .\n" +
                           "}");
        queries.add(query1);
        
        Map<String, String> query2 = new HashMap<>();
        query2.put("name", "Find Components by Name");
        query2.put("description", "Finds components with a specific substring in their name");
        query2.put("category", "Basic");
        query2.put("query", "PREFIX j: <http://www.jfc.com/tiptop/ontology#>\n" +
                           "SELECT ?component ?name ?spec\n" +
                           "WHERE {\n" +
                           "  ?component a j:ComponentItem .\n" +
                           "  ?component j:itemName ?name .\n" +
                           "  ?component j:itemSpec ?spec .\n" +
                           "  FILTER(CONTAINS(lcase(?name), \"search_term\"))\n" +
                           "}");
        queries.add(query2);
        
        Map<String, String> query3 = new HashMap<>();
        query3.put("name", "Component Usage Count");
        query3.put("description", "Counts how many times each component is used across all BOMs");
        query3.put("category", "Analysis");
        query3.put("query", "PREFIX j: <http://www.jfc.com/tiptop/ontology#>\n" +
                           "SELECT ?component ?name (COUNT(?bom) as ?usageCount)\n" +
                           "WHERE {\n" +
                           "  ?bom a j:BillOfMaterial .\n" +
                           "  ?bom j:hasComponentItem ?component .\n" +
                           "  ?component j:itemName ?name .\n" +
                           "}\n" +
                           "GROUP BY ?component ?name\n" +
                           "ORDER BY DESC(?usageCount)");
        queries.add(query3);
        
        return queries;
    }
    
    /**
     * Get hydraulic cylinder specific queries
     */
    private List<Map<String, String>> getHydraulicCylinderQueries() {
        List<Map<String, String>> queries = new ArrayList<>();
        
        Map<String, String> query1 = new HashMap<>();
        query1.put("name", "Hydraulic Cylinder Specifications");
        query1.put("description", "Show all hydraulic cylinder specifications for the current master item");
        query1.put("category", "Hydraulic Cylinder");
        query1.put("query", "PREFIX j: <http://www.jfc.com/tiptop/ontology#>\n" +
                           "PREFIX hc: <http://www.jfc.com/tiptop/hydraulic-cylinder#>\n" +
                           "SELECT ?property ?value\n" +
                           "WHERE {\n" +
                           "  ?master j:itemCode \"COMPONENT_CODE\" .\n" +
                           "  ?master ?property ?value .\n" +
                           "  FILTER(?property IN (\n" +
                           "    hc:bore, hc:stroke, hc:rodEndType, \n" +
                           "    hc:series, hc:cylinderType, hc:installationType\n" +
                           "  ))\n" +
                           "}");
        queries.add(query1);
        
        Map<String, String> query2 = new HashMap<>();
        query2.put("name", "Find Cylinders by Bore and Stroke");
        query2.put("description", "Find hydraulic cylinders with specific bore and stroke ranges");
        query2.put("category", "Hydraulic Cylinder");
        query2.put("query", "PREFIX j: <http://www.jfc.com/tiptop/ontology#>\n" +
                           "PREFIX hc: <http://www.jfc.com/tiptop/hydraulic-cylinder#>\n" +
                           "SELECT ?cylinder ?itemCode ?bore ?stroke\n" +
                           "WHERE {\n" +
                           "  ?cylinder a hc:HydraulicCylinder .\n" +
                           "  ?cylinder j:itemCode ?itemCode .\n" +
                           "  ?cylinder hc:bore ?bore .\n" +
                           "  ?cylinder hc:stroke ?stroke .\n" +
                           "  FILTER(xsd:int(?bore) >= 50 && xsd:int(?bore) <= 100)\n" +
                           "  FILTER(xsd:int(?stroke) >= 100 && xsd:int(?stroke) <= 500)\n" +
                           "}\n" +
                           "ORDER BY ?bore ?stroke");
        queries.add(query2);
        
        Map<String, String> query3 = new HashMap<>();
        query3.put("name", "Cylinder Classifications");
        query3.put("description", "Show all inferred classifications for hydraulic cylinders");
        query3.put("category", "Hydraulic Cylinder");
        query3.put("query", "PREFIX j: <http://www.jfc.com/tiptop/ontology#>\n" +
                           "PREFIX hc: <http://www.jfc.com/tiptop/hydraulic-cylinder#>\n" +
                           "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                           "SELECT ?cylinder ?itemCode ?classification\n" +
                           "WHERE {\n" +
                           "  ?cylinder a hc:HydraulicCylinder .\n" +
                           "  ?cylinder j:itemCode ?itemCode .\n" +
                           "  ?cylinder rdf:type ?classification .\n" +
                           "  FILTER(?classification != hc:HydraulicCylinder)\n" +
                           "  FILTER(?classification != j:Material)\n" +
                           "  FILTER(?classification != j:MasterItem)\n" +
                           "}\n" +
                           "ORDER BY ?itemCode");
        queries.add(query3);
        
        Map<String, String> query4 = new HashMap<>();
        query4.put("name", "Compatible Components");
        query4.put("description", "Find components compatible with the current hydraulic cylinder");
        query4.put("category", "Hydraulic Cylinder");
        query4.put("query", "PREFIX j: <http://www.jfc.com/tiptop/ontology#>\n" +
                           "PREFIX hc: <http://www.jfc.com/tiptop/hydraulic-cylinder#>\n" +
                           "SELECT ?component ?componentCode ?componentName ?relationship\n" +
                           "WHERE {\n" +
                           "  ?master j:itemCode \"COMPONENT_CODE\" .\n" +
                           "  ?component ?relationship ?master .\n" +
                           "  ?component j:itemCode ?componentCode .\n" +
                           "  ?component j:itemName ?componentName .\n" +
                           "  FILTER(?relationship IN (hc:compatibleWith, hc:recommendedFor, hc:isComponentOf))\n" +
                           "}\n" +
                           "ORDER BY ?relationship ?componentCode");
        queries.add(query4);
        
        Map<String, String> query5 = new HashMap<>();
        query5.put("name", "Cylinder Performance Analysis");
        query5.put("description", "Analyze performance characteristics of hydraulic cylinders");
        query5.put("category", "Hydraulic Cylinder");
        query5.put("query", "PREFIX j: <http://www.jfc.com/tiptop/ontology#>\n" +
                           "PREFIX hc: <http://www.jfc.com/tiptop/hydraulic-cylinder#>\n" +
                           "SELECT ?cylinder ?series ?bore ?stroke ?maxPressure ?performanceClass\n" +
                           "WHERE {\n" +
                           "  ?cylinder a hc:HydraulicCylinder .\n" +
                           "  ?cylinder hc:series ?series .\n" +
                           "  ?cylinder hc:bore ?bore .\n" +
                           "  ?cylinder hc:stroke ?stroke .\n" +
                           "  OPTIONAL { ?cylinder hc:maxPressure ?maxPressure }\n" +
                           "  OPTIONAL { \n" +
                           "    ?cylinder rdf:type ?performanceClass .\n" +
                           "    FILTER(CONTAINS(STR(?performanceClass), \"Pressure\") || \n" +
                           "           CONTAINS(STR(?performanceClass), \"Speed\") ||\n" +
                           "           CONTAINS(STR(?performanceClass), \"Precision\"))\n" +
                           "  }\n" +
                           "}\n" +
                           "ORDER BY ?series ?bore");
        queries.add(query5);
        
        return queries;
    }
    
    /**
     * Get advanced reasoning queries
     */
    private List<Map<String, String>> getAdvancedReasoningQueries() {
        List<Map<String, String>> queries = new ArrayList<>();
        
        Map<String, String> query1 = new HashMap<>();
        query1.put("name", "Inferred Relationships");
        query1.put("description", "Show all inferred relationships for the current item");
        query1.put("category", "Advanced");
        query1.put("query", "PREFIX j: <http://www.jfc.com/tiptop/ontology#>\n" +
                           "PREFIX hc: <http://www.jfc.com/tiptop/hydraulic-cylinder#>\n" +
                           "SELECT ?subject ?predicate ?object\n" +
                           "WHERE {\n" +
                           "  ?master j:itemCode \"COMPONENT_CODE\" .\n" +
                           "  { ?master ?predicate ?object }\n" +
                           "  UNION\n" +
                           "  { ?subject ?predicate ?master }\n" +
                           "  FILTER(CONTAINS(STR(?predicate), \"compatible\") ||\n" +
                           "         CONTAINS(STR(?predicate), \"recommended\") ||\n" +
                           "         CONTAINS(STR(?predicate), \"requires\") ||\n" +
                           "         CONTAINS(STR(?predicate), \"hasComponent\"))\n" +
                           "}");
        queries.add(query1);
        
        Map<String, String> query2 = new HashMap<>();
        query2.put("name", "Class Hierarchy Analysis");
        query2.put("description", "Analyze the class hierarchy for the current item");
        query2.put("category", "Advanced");
        query2.put("query", "PREFIX j: <http://www.jfc.com/tiptop/ontology#>\n" +
                           "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                           "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                           "SELECT ?class ?superclass ?depth\n" +
                           "WHERE {\n" +
                           "  ?master j:itemCode \"COMPONENT_CODE\" .\n" +
                           "  ?master rdf:type ?class .\n" +
                           "  OPTIONAL {\n" +
                           "    ?class rdfs:subClassOf+ ?superclass .\n" +
                           "  }\n" +
                           "  BIND(IF(BOUND(?superclass), \"SUBCLASS\", \"ROOT\") AS ?depth)\n" +
                           "}\n" +
                           "ORDER BY ?class");
        queries.add(query2);
        
        Map<String, String> query3 = new HashMap<>();
        query3.put("name", "Component Dependency Analysis");
        query3.put("description", "Analyze component dependencies and requirements");
        query3.put("category", "Advanced");
        query3.put("query", "PREFIX j: <http://www.jfc.com/tiptop/ontology#>\n" +
                           "PREFIX hc: <http://www.jfc.com/tiptop/hydraulic-cylinder#>\n" +
                           "SELECT ?component ?componentName ?dependsOn ?dependencyName ?requirementType\n" +
                           "WHERE {\n" +
                           "  ?master j:itemCode \"COMPONENT_CODE\" .\n" +
                           "  ?bom j:hasMasterItem ?master .\n" +
                           "  ?bom j:hasComponentItem ?component .\n" +
                           "  ?component j:itemName ?componentName .\n" +
                           "  OPTIONAL {\n" +
                           "    ?component ?requirementType ?dependsOn .\n" +
                           "    ?dependsOn j:itemName ?dependencyName .\n" +
                           "    FILTER(?requirementType IN (hc:requiresComponent, hc:compatibleWith))\n" +
                           "  }\n" +
                           "}\n" +
                           "ORDER BY ?componentName");
        queries.add(query3);
        
        return queries;
    }
    
    /**
     * Enhanced example rules with hydraulic cylinder integration
     * 
     * @return A comprehensive list of example rules
     */
    public List<Map<String, String>> getExampleRules() {
        List<Map<String, String>> rules = new ArrayList<>();
        
        // Add original rules
        rules.addAll(getBasicExampleRules());
        
        // Add hydraulic cylinder specific rules
        rules.addAll(getHydraulicCylinderExampleRules());
        
        // Add advanced reasoning rules
        rules.addAll(getAdvancedExampleRules());
        
        return rules;
    }
    
    /**
     * Get basic example rules
     */
    private List<Map<String, String>> getBasicExampleRules() {
        List<Map<String, String>> rules = new ArrayList<>();
        
        Map<String, String> rule1 = new HashMap<>();
        rule1.put("name", "Critical Component Rule");
        rule1.put("description", "Identifies components that are used in more than 3 different master items");
        rule1.put("category", "Basic");
        rule1.put("rule", "[CriticalComponentRule: " +
                         "(?component rdf:type <http://www.jfc.com/tiptop/ontology#ComponentItem>) " +
                         "(?bom1 <http://www.jfc.com/tiptop/ontology#hasComponentItem> ?component) " +
                         "(?bom1 <http://www.jfc.com/tiptop/ontology#hasMasterItem> ?master1) " +
                         "(?bom2 <http://www.jfc.com/tiptop/ontology#hasComponentItem> ?component) " +
                         "(?bom2 <http://www.jfc.com/tiptop/ontology#hasMasterItem> ?master2) " +
                         "(?bom3 <http://www.jfc.com/tiptop/ontology#hasComponentItem> ?component) " +
                         "(?bom3 <http://www.jfc.com/tiptop/ontology#hasMasterItem> ?master3) " +
                         "notEqual(?master1, ?master2) notEqual(?master1, ?master3) notEqual(?master2, ?master3) " +
                         "-> (?component rdf:type <http://www.jfc.com/tiptop/ontology#CriticalComponent>)]");
        rules.add(rule1);
        
        Map<String, String> rule2 = new HashMap<>();
        rule2.put("name", "High Usage Component Rule");
        rule2.put("description", "Identifies components with high quantity usage");
        rule2.put("category", "Basic");
        rule2.put("rule", "[HighUsageComponentRule: " +
                         "(?bom <http://www.jfc.com/tiptop/ontology#hasComponentItem> ?component) " +
                         "(?bom <http://www.jfc.com/tiptop/ontology#quantity> ?quantity) " +
                         "greaterThan(?quantity, 5) " +
                         "-> (?component rdf:type <http://www.jfc.com/tiptop/ontology#HighUsageComponent>)]");
        rules.add(rule2);
        
        return rules;
    }
    
    /**
     * Get hydraulic cylinder specific example rules
     */
    private List<Map<String, String>> getHydraulicCylinderExampleRules() {
        List<Map<String, String>> rules = new ArrayList<>();
        
        Map<String, String> rule1 = new HashMap<>();
        rule1.put("name", "Large Bore Cylinder Rule");
        rule1.put("description", "Classifies cylinders with bore >= 100mm as large bore");
        rule1.put("category", "Hydraulic Cylinder");
        rule1.put("rule", "[LargeBoreCylinderRule: " +
                         "(?cylinder rdf:type <http://www.jfc.com/tiptop/hydraulic-cylinder#HydraulicCylinder>) " +
                         "(?cylinder <http://www.jfc.com/tiptop/hydraulic-cylinder#bore> ?bore) " +
                         "greaterThan(?bore, 99) " +
                         "-> (?cylinder rdf:type <http://www.jfc.com/tiptop/hydraulic-cylinder#LargeBoreCylinder>)]");
        rules.add(rule1);
        
        Map<String, String> rule2 = new HashMap<>();
        rule2.put("name", "Heavy Duty Seal Requirement");
        rule2.put("description", "Heavy duty cylinders require high-pressure sealing components");
        rule2.put("category", "Hydraulic Cylinder");
        rule2.put("rule", "[HeavyDutySealRule: " +
                         "(?cylinder rdf:type <http://www.jfc.com/tiptop/hydraulic-cylinder#HeavyDutyCylinder>) " +
                         "(?seal rdf:type <http://www.jfc.com/tiptop/hydraulic-cylinder#SealingComponent>) " +
                         "(?seal <http://www.jfc.com/tiptop/ontology#itemName> ?sealName) " +
                         "regex(?sealName, '.*[Hh]igh.*[Pp]ressure.*') " +
                         "-> (?seal <http://www.jfc.com/tiptop/hydraulic-cylinder#recommendedFor> ?cylinder)]");
        rules.add(rule2);
        
        Map<String, String> rule3 = new HashMap<>();
        rule3.put("name", "Rod End Compatibility Rule");
        rule3.put("description", "Ensures rod end components match cylinder rod end type");
        rule3.put("category", "Hydraulic Cylinder");
        rule3.put("rule", "[RodEndCompatibilityRule: " +
                         "(?cylinder <http://www.jfc.com/tiptop/hydraulic-cylinder#rodEndType> ?rodEndType) " +
                         "(?component <http://www.jfc.com/tiptop/ontology#itemName> ?compName) " +
                         "regex(?compName, concat('.*', ?rodEndType, '.*[Rr]od.*[Ee]nd.*')) " +
                         "-> (?component <http://www.jfc.com/tiptop/hydraulic-cylinder#compatibleWith> ?cylinder)]");
        rules.add(rule3);
        
        Map<String, String> rule4 = new HashMap<>();
        rule4.put("name", "Complex Configuration Rule");
        rule4.put("description", "Identifies cylinders with complex configurations requiring special attention");
        rule4.put("category", "Hydraulic Cylinder");
        rule4.put("rule", "[ComplexConfigurationRule: " +
                         "(?cylinder rdf:type <http://www.jfc.com/tiptop/hydraulic-cylinder#LargeBoreCylinder>) " +
                         "(?cylinder rdf:type <http://www.jfc.com/tiptop/hydraulic-cylinder#LongStrokeCylinder>) " +
                         "(?cylinder rdf:type <http://www.jfc.com/tiptop/hydraulic-cylinder#HeavyDutyCylinder>) " +
                         "-> (?cylinder rdf:type <http://www.jfc.com/tiptop/hydraulic-cylinder#ComplexConfiguration>)]");
        rules.add(rule4);
        
        return rules;
    }
    
    /**
     * Get advanced example rules
     */
    private List<Map<String, String>> getAdvancedExampleRules() {
        List<Map<String, String>> rules = new ArrayList<>();
        
        Map<String, String> rule1 = new HashMap<>();
        rule1.put("name", "Material Upgrade Recommendation");
        rule1.put("description", "Recommends material upgrades for demanding applications");
        rule1.put("category", "Advanced");
        rule1.put("rule", "[MaterialUpgradeRule: " +
                         "(?cylinder rdf:type <http://www.jfc.com/tiptop/hydraulic-cylinder#HighPressureCylinder>) " +
                         "(?component <http://www.jfc.com/tiptop/hydraulic-cylinder#compatibleWith> ?cylinder) " +
                         "(?component <http://www.jfc.com/tiptop/hydraulic-cylinder#material> ?material) " +
                         "regex(?material, '.*[Ss]tandard.*|.*[Cc]arbon.*[Ss]teel.*') " +
                         "-> (?component rdf:type <http://www.jfc.com/tiptop/hydraulic-cylinder#RequiresMaterialUpgrade>)]");
        rules.add(rule1);
        
        Map<String, String> rule2 = new HashMap<>();
        rule2.put("name", "Redundant Sealing Rule");
        rule2.put("description", "Identifies cylinders requiring redundant sealing systems");
        rule2.put("category", "Advanced");
        rule2.put("rule", "[RedundantSealingRule: " +
                         "(?cylinder rdf:type <http://www.jfc.com/tiptop/hydraulic-cylinder#HighPressureCylinder>) " +
                         "(?cylinder rdf:type <http://www.jfc.com/tiptop/hydraulic-cylinder#LargeBoreCylinder>) " +
                         "(?seal rdf:type <http://www.jfc.com/tiptop/hydraulic-cylinder#PistonSeal>) " +
                         "(?seal <http://www.jfc.com/tiptop/hydraulic-cylinder#compatibleWith> ?cylinder) " +
                         "-> (?cylinder <http://www.jfc.com/tiptop/hydraulic-cylinder#requiresRedundantSealing> ?seal)]");
        rules.add(rule2);
        
        return rules;
    }
    
    /**
     * Enhanced reasoner information with hydraulic cylinder capabilities
     * 
     * @return A comprehensive list of available reasoners
     */
    public List<Map<String, String>> getAvailableReasoners() {
        List<Map<String, String>> reasoners = new ArrayList<>();
        
        Map<String, String> reasoner1 = new HashMap<>();
        reasoner1.put("id", "RDFS");
        reasoner1.put("name", "RDFS Rule Reasoner");
        reasoner1.put("description", "Simple RDFS reasoner supporting subclass, subproperty, domain, and range reasoning.");
        reasoner1.put("suitability", "Basic ontology reasoning");
        reasoners.add(reasoner1);
        
        Map<String, String> reasoner2 = new HashMap<>();
        reasoner2.put("id", "OWL_MICRO");
        reasoner2.put("name", "OWL Micro Reasoner");
        reasoner2.put("description", "OWL reasoner with limited features but good performance.");
        reasoner2.put("suitability", "Performance-critical applications");
        reasoners.add(reasoner2);
        
        Map<String, String> reasoner3 = new HashMap<>();
        reasoner3.put("id", "OWL_MINI");
        reasoner3.put("name", "OWL Mini Reasoner");
        reasoner3.put("description", "OWL reasoner with moderate feature set and balanced performance.");
        reasoner3.put("suitability", "General purpose reasoning");
        reasoners.add(reasoner3);
        
        Map<String, String> reasoner4 = new HashMap<>();
        reasoner4.put("id", "OWL");
        reasoner4.put("name", "OWL Reasoner");
        reasoner4.put("description", "Full OWL reasoner with comprehensive feature support.");
        reasoner4.put("suitability", "Complex ontology reasoning");
        reasoners.add(reasoner4);
        
        Map<String, String> reasoner5 = new HashMap<>();
        reasoner5.put("id", "ENHANCED_HYDRAULIC");
        reasoner5.put("name", "Enhanced Hydraulic Cylinder Reasoner");
        reasoner5.put("description", "Specialized rule-based reasoner with hydraulic cylinder domain knowledge.");
        reasoner5.put("suitability", "Hydraulic cylinder applications with domain-specific reasoning");
        reasoners.add(reasoner5);
        
        return reasoners;
    }
    
    /**
     * Get reasoning capabilities summary
     * 
     * @return Summary of enhanced reasoning capabilities
     */
    public Map<String, Object> getReasoningCapabilities() {
        Map<String, Object> capabilities = new HashMap<>();
        
        // Basic capabilities
        capabilities.put("supportedReasonerTypes", getAvailableReasoners().size());
        capabilities.put("predefinedQueries", getPredefinedQueries().size());
        capabilities.put("exampleRules", getExampleRules().size());
        
        // Enhanced capabilities
        Map<String, Object> enhancedFeatures = new HashMap<>();
        enhancedFeatures.put("hydraulicCylinderSupport", true);
        enhancedFeatures.put("domainSpecificRules", true);
        enhancedFeatures.put("componentCompatibilityReasoning", true);
        enhancedFeatures.put("performanceAnalysis", true);
        enhancedFeatures.put("intelligentComponentSuggestion", true);
        
        capabilities.put("enhancedFeatures", enhancedFeatures);
        
        // Integration capabilities
        Map<String, Object> integrationFeatures = new HashMap<>();
        integrationFeatures.put("ontologyIntegration", true);
        integrationFeatures.put("bomGeneration", true);
        integrationFeatures.put("similarityAnalysis", true);
        integrationFeatures.put("validationSupport", true);
        
        capabilities.put("integrationFeatures", integrationFeatures);
        
        // Query categories
        Map<String, Long> queryCategories = getPredefinedQueries().stream()
            .collect(Collectors.groupingBy(
                query -> query.getOrDefault("category", "Other"),
                Collectors.counting()
            ));
        capabilities.put("queryCategories", queryCategories);
        
        // Rule categories
        Map<String, Long> ruleCategories = getExampleRules().stream()
            .collect(Collectors.groupingBy(
                rule -> rule.getOrDefault("category", "Other"),
                Collectors.counting()
            ));
        capabilities.put("ruleCategories", ruleCategories);
        
        return capabilities;
    }
    
    /**
     * Validate reasoning configuration for hydraulic cylinders
     * 
     * @param masterItemCode The master item code
     * @return Validation results and recommendations
     */
    public Map<String, Object> validateReasoningConfiguration(String masterItemCode) {
        Map<String, Object> validation = new HashMap<>();
        List<String> recommendations = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        try {
            // Check if item is hydraulic cylinder
            boolean isHydraulicCylinder = isHydraulicCylinderItem(masterItemCode);
            validation.put("isHydraulicCylinder", isHydraulicCylinder);
            
            if (isHydraulicCylinder) {
                // Validate hydraulic cylinder ontology availability
                try {
                    hydraulicCylinderOntology.initializeHydraulicCylinderOntology();
                    validation.put("hydraulicOntologyAvailable", true);
                    recommendations.add("Use ENHANCED_HYDRAULIC reasoner for optimal hydraulic cylinder reasoning");
                } catch (Exception e) {
                    validation.put("hydraulicOntologyAvailable", false);
                    warnings.add("Hydraulic cylinder ontology not available: " + e.getMessage());
                }
                
                // Validate hydraulic cylinder rules
                try {
                    List<String> hcRules = hydraulicCylinderRules.getEnhancedHydraulicCylinderRules();
                    validation.put("hydraulicRulesCount", hcRules.size());
                    recommendations.add("Apply hydraulic cylinder specific rules for enhanced reasoning");
                } catch (Exception e) {
                    warnings.add("Error accessing hydraulic cylinder rules: " + e.getMessage());
                }
            } else {
                recommendations.add("Use standard OWL reasoner for non-hydraulic cylinder items");
            }
            
            // Validate ontology model availability
            try {
                OntModel ontModel = bomOwlExportService.getBomOntologyForMasterItem(masterItemCode);
                validation.put("ontologyModelAvailable", true);
                validation.put("modelSize", ontModel.size());
                
                if (ontModel.size() == 0) {
                    warnings.add("Ontology model is empty - no reasoning will be performed");
                } else if (ontModel.size() < 10) {
                    warnings.add("Small ontology model - reasoning results may be limited");
                }
            } catch (Exception e) {
                validation.put("ontologyModelAvailable", false);
                warnings.add("Error accessing ontology model: " + e.getMessage());
            }
            
            validation.put("recommendations", recommendations);
            validation.put("warnings", warnings);
            validation.put("configurationValid", warnings.isEmpty());
            
        } catch (Exception e) {
            validation.put("error", "Validation failed: " + e.getMessage());
            validation.put("configurationValid", false);
        }
        
        return validation;
    }
}
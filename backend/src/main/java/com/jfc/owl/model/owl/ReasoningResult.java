package com.jfc.owl.model.owl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the results of reasoning on an ontology.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReasoningResult {

    /**
     * The master item code that was reasoned about
     */
    private String masterItemCode;
    
    /**
     * The type of reasoner used
     */
    private String reasonerType;
    
    /**
     * Whether the ontology is valid according to the reasoner
     */
    private boolean valid;
    
    /**
     * List of validation issues found by the reasoner
     */
    private List<ValidationIssue> validationIssues = new ArrayList<>();
    
    /**
     * List of inferred statements derived by the reasoner
     */
    private List<InferredStatement> inferredStatements = new ArrayList<>();
    
    /**
     * List of inferred subclass relationships
     */
    private List<ClassRelationship> inferredSubclasses = new ArrayList<>();
    
    /**
     * Hierarchical representation of the BOM with inferred information
     */
    private BomHierarchy bomHierarchy;
    
    /**
     * Any error message that occurred during reasoning
     */
    private String errorMessage;
    
    /**
     * Time taken for the reasoning operation (in milliseconds)
     */
    private long processingTimeMs;
    
    /**
     * Represents a validation issue found by the reasoner.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationIssue {
        private String type;
        private String description;
    }
    
    /**
     * Represents an inferred statement in the form of (subject, predicate, object).
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InferredStatement {
        private String subject;
        private String predicate;
        private String object;
    }
    
    /**
     * Represents a class relationship (e.g., subclass, equivalent class).
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassRelationship {
        private String subclass;
        private String superclass;
    }
    
    /**
     * Represents the hierarchical structure of a BOM with inferred information.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BomHierarchy {
        private String code;
        private String uri;
        private Map<String, List<String>> inferredProperties = new HashMap<>();
        private List<ComponentItem> components = new ArrayList<>();
    }
    
    /**
     * Represents a component item in a BOM hierarchy.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentItem {
        private String code;
        private String uri;
        private String name;
        private String spec;
        private String quantity;
        private String effectiveDate;
        private String expiryDate;
        private Map<String, List<String>> inferredProperties = new HashMap<>();
    }
    
    /**
     * Factory method to create a ReasoningResult from a Map returned by the service.
     * 
     * @param resultMap        The Map containing the reasoning results
     * @param masterItemCode   The master item code
     * @param reasonerType     The reasoner type used
     * @param processingTimeMs The processing time in milliseconds
     * @return A ReasoningResult object
     */
    public static ReasoningResult fromMap(Map<String, Object> resultMap, String masterItemCode, String reasonerType, long processingTimeMs) {
        ReasoningResult result = new ReasoningResult();
        result.setMasterItemCode(masterItemCode);
        result.setReasonerType(reasonerType);
        result.setProcessingTimeMs(processingTimeMs);
        
        // Check for errors
        if (resultMap.containsKey("error")) {
            result.setValid(false);
            result.setErrorMessage(resultMap.get("error").toString());
            return result;
        }
        
        // Set validity
        result.setValid(resultMap.containsKey("isValid") ? (Boolean) resultMap.get("isValid") : true);
        
        // Process validation issues
        List<ValidationIssue> validationIssues = new ArrayList<>();
        if (resultMap.containsKey("validationIssues")) {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> issues = (List<Map<String, String>>) resultMap.get("validationIssues");
            
            for (Map<String, String> issue : issues) {
                ValidationIssue validationIssue = new ValidationIssue(
                        issue.get("type"),
                        issue.get("description")
                );
                validationIssues.add(validationIssue);
            }
        }
        result.setValidationIssues(validationIssues);
        
        // Process inferred statements
        List<InferredStatement> inferredStatements = new ArrayList<>();
        if (resultMap.containsKey("inferredStatements")) {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> statements = (List<Map<String, String>>) resultMap.get("inferredStatements");
            
            for (Map<String, String> statement : statements) {
                InferredStatement inferredStatement = new InferredStatement(
                        statement.get("subject"),
                        statement.get("predicate"),
                        statement.get("object")
                );
                inferredStatements.add(inferredStatement);
            }
        }
        result.setInferredStatements(inferredStatements);
        
        // Process inferred subclasses
        List<ClassRelationship> inferredSubclasses = new ArrayList<>();
        if (resultMap.containsKey("inferredSubclasses")) {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> subclasses = (List<Map<String, String>>) resultMap.get("inferredSubclasses");
            
            for (Map<String, String> subclass : subclasses) {
                ClassRelationship classRelationship = new ClassRelationship(
                        subclass.get("subclass"),
                        subclass.get("superclass")
                );
                inferredSubclasses.add(classRelationship);
            }
        }
        result.setInferredSubclasses(inferredSubclasses);
        
        // Process BOM hierarchy
        if (resultMap.containsKey("bomHierarchy")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> hierarchyMap = (Map<String, Object>) resultMap.get("bomHierarchy");
            
            BomHierarchy bomHierarchy = new BomHierarchy();
            bomHierarchy.setCode(hierarchyMap.get("code").toString());
            bomHierarchy.setUri(hierarchyMap.get("uri").toString());
            
            // Process inferred properties for the master item
            if (hierarchyMap.containsKey("inferredProperties")) {
                @SuppressWarnings("unchecked")
                Map<String, List<String>> inferredProps = (Map<String, List<String>>) hierarchyMap.get("inferredProperties");
                bomHierarchy.setInferredProperties(inferredProps);
            }
            
            // Process components
            List<ComponentItem> components = new ArrayList<>();
            if (hierarchyMap.containsKey("components")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> componentMaps = (List<Map<String, Object>>) hierarchyMap.get("components");
                
                for (Map<String, Object> componentMap : componentMaps) {
                    ComponentItem component = new ComponentItem();
                    component.setCode(componentMap.get("code").toString());
                    component.setUri(componentMap.get("uri").toString());
                    
                    if (componentMap.containsKey("name")) {
                        component.setName(componentMap.get("name").toString());
                    }
                    
                    if (componentMap.containsKey("spec")) {
                        component.setSpec(componentMap.get("spec").toString());
                    }
                    
                    if (componentMap.containsKey("quantity")) {
                        component.setQuantity(componentMap.get("quantity").toString());
                    }
                    
                    if (componentMap.containsKey("effectiveDate")) {
                        component.setEffectiveDate(componentMap.get("effectiveDate").toString());
                    }
                    
                    if (componentMap.containsKey("expiryDate")) {
                        component.setExpiryDate(componentMap.get("expiryDate").toString());
                    }
                    
                    components.add(component);
                }
            }
            bomHierarchy.setComponents(components);
            result.setBomHierarchy(bomHierarchy);
        }
        
        return result;
    }
}
package com.jfc.owl.model.owl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a rule in the ontology reasoning system.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OntologyRule {

    /**
     * The unique identifier for the rule
     */
    private String id;
    
    /**
     * The name of the rule
     */
    private String name;
    
    /**
     * A human-readable description of the rule
     */
    private String description;
    
    /**
     * The rule body in Jena rule syntax
     */
    private String ruleBody;
    
    /**
     * The priority of the rule (lower numbers are higher priority)
     */
    private int priority = 100;
    
    /**
     * Whether the rule is enabled
     */
    private boolean enabled = true;
    
    /**
     * Tags/categories for the rule
     */
    private List<String> tags = new ArrayList<>();
    
    /**
     * Whether the rule is predefined in the system
     */
    private boolean predefined = false;
    
    /**
     * The date when the rule was created
     */
    private String createdDate;
    
    /**
     * The user who created the rule
     */
    private String createdBy;
    
    /**
     * The date when the rule was last modified
     */
    private String lastModifiedDate;
    
    /**
     * The user who last modified the rule
     */
    private String lastModifiedBy;
    
    /**
     * Convert the rule to Jena rule syntax
     * 
     * @return The rule in Jena format
     */
    public String toJenaRuleFormat() {
        StringBuilder ruleBuilder = new StringBuilder();
        
        // Add rule name
        ruleBuilder.append("[").append(name.replaceAll("\\s+", "")).append(": ");
        
        // Add rule body
        ruleBuilder.append(ruleBody);
        
        // Close the rule
        ruleBuilder.append("]");
        
        return ruleBuilder.toString();
    }
    
    /**
     * Create a simple rule that adds a type to entities matching a condition
     * 
     * @param name        The name of the rule
     * @param description The description of the rule
     * @param condition   The condition that must be met (Jena rule syntax)
     * @param typeToAdd   The type to add to entities meeting the condition
     * @return A new OntologyRule
     */
    public static OntologyRule createTypeRule(String name, String description, String condition, String typeToAdd) {
        OntologyRule rule = new OntologyRule();
        rule.setName(name);
        rule.setDescription(description);
        
        // Build the rule body
        StringBuilder ruleBody = new StringBuilder();
        ruleBody.append(condition).append(" -> (?x rdf:type <").append(typeToAdd).append(">)");
        
        rule.setRuleBody(ruleBody.toString());
        
        return rule;
    }
    
    /**
     * Create a simple rule that adds a property to entities matching a condition
     * 
     * @param name           The name of the rule
     * @param description    The description of the rule
     * @param condition      The condition that must be met (Jena rule syntax)
     * @param propertyToAdd  The property to add to entities meeting the condition
     * @param propertyValue  The value for the property
     * @return A new OntologyRule
     */
    public static OntologyRule createPropertyRule(String name, String description, String condition, String propertyToAdd, String propertyValue) {
        OntologyRule rule = new OntologyRule();
        rule.setName(name);
        rule.setDescription(description);
        
        // Build the rule body
        StringBuilder ruleBody = new StringBuilder();
        ruleBody.append(condition).append(" -> (?x <").append(propertyToAdd).append("> ").append(propertyValue).append(")");
        
        rule.setRuleBody(ruleBody.toString());
        
        return rule;
    }
}
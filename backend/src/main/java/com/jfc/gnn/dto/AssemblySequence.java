package com.jfc.gnn.dto;

import lombok.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents the assembly sequence for a BOM structure
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssemblySequence {
    
    /**
     * List of assembly steps in order
     */
    private List<AssemblyStep> steps;
    
    /**
     * Total estimated assembly time in minutes
     */
    private double totalAssemblyTime;
    
    /**
     * Assembly difficulty level (1-5)
     */
    private int difficultyLevel;
    
    /**
     * Special tools required for assembly
     */
    private List<String> requiredTools;
    
    /**
     * Assembly constraints or prerequisites
     */
    private List<AssemblyConstraint> constraints;
    
    /**
     * Assembly validation rules
     */
    private List<ValidationRule> validationRules;
    
    /**
     * Represents a single step in the assembly process
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssemblyStep {
        /**
         * Step number in the sequence
         */
        private int stepNumber;
        
        /**
         * Step identifier
         */
        private String stepId;
        
        /**
         * Description of the assembly step
         */
        private String description;
        
        /**
         * Component(s) involved in this step
         */
        private List<String> componentCodes;
        
        /**
         * Estimated time for this step (minutes)
         */
        private double estimatedTime;
        
        /**
         * Required tools for this step
         */
        private List<String> tools;
        
        /**
         * Step type (e.g., "INSTALL", "CONNECT", "TIGHTEN", "TEST")
         */
        private StepType stepType;
        
        /**
         * Dependencies - steps that must be completed before this one
         */
        private List<String> dependencies;
        
        /**
         * Special instructions or warnings
         */
        private String specialInstructions;
        
        /**
         * Quality check points
         */
        private List<String> qualityCheckPoints;
        
        /**
         * Images or diagrams (URLs or file paths)
         */
        private List<String> visualAids;
    }
    
    /**
     * Assembly step types
     */
    public enum StepType {
        PREPARE("Preparation"),
        INSTALL("Installation"),
        CONNECT("Connection"),
        TIGHTEN("Tightening/Fastening"),
        SEAL("Sealing"),
        TEST("Testing"),
        ADJUST("Adjustment"),
        INSPECT("Inspection"),
        COMPLETE("Completion");
        
        private final String description;
        
        StepType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Assembly constraints
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssemblyConstraint {
        /**
         * Constraint type (e.g., "BEFORE", "AFTER", "CONCURRENT")
         */
        private ConstraintType type;
        
        /**
         * Step IDs involved in the constraint
         */
        private List<String> stepIds;
        
        /**
         * Constraint description
         */
        private String description;
        
        /**
         * Is this a hard constraint (must be followed) or soft (recommended)
         */
        private boolean isHardConstraint;
    }
    
    /**
     * Constraint types
     */
    public enum ConstraintType {
        BEFORE("Must be done before"),
        AFTER("Must be done after"),
        CONCURRENT("Can be done concurrently"),
        MUTEX("Cannot be done at the same time"),
        CONDITIONAL("Conditional dependency");
        
        private final String description;
        
        ConstraintType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Validation rules for assembly
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationRule {
        /**
         * Rule identifier
         */
        private String ruleId;
        
        /**
         * Rule description
         */
        private String description;
        
        /**
         * Steps this rule applies to
         */
        private List<String> applicableSteps;
        
        /**
         * Validation type
         */
        private ValidationType validationType;
        
        /**
         * Expected values or ranges
         */
        private Map<String, Object> expectedValues;
        
        /**
         * Error message if validation fails
         */
        private String errorMessage;
    }
    
    /**
     * Validation types
     */
    public enum ValidationType {
        TORQUE_CHECK("Torque specification check"),
        DIMENSION_CHECK("Dimensional verification"),
        ALIGNMENT_CHECK("Alignment verification"),
        LEAK_TEST("Leak testing"),
        FUNCTION_TEST("Functional testing"),
        VISUAL_INSPECTION("Visual inspection");
        
        private final String description;
        
        ValidationType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Calculate critical path through assembly sequence
     */
    public List<AssemblyStep> getCriticalPath() {
        // Implementation for finding critical path
        // This would use graph algorithms to find the longest path
        return steps; // Simplified for now
    }
    
    /**
     * Get steps that can be done in parallel
     */
    public List<List<AssemblyStep>> getParallelSteps() {
        // Implementation for identifying parallel steps
        // Based on dependencies and constraints
        return List.of(steps); // Simplified for now
    }
    
    /**
     * Validate the assembly sequence
     */
    public boolean isValid() {
        // Check for circular dependencies
        // Verify all dependencies are satisfied
        // Check constraints are not violated
        return true; // Simplified for now
    }
    
    /**
     * Get steps for a specific component
     */
    public List<AssemblyStep> getStepsForComponent(String componentCode) {
        return steps.stream()
            .filter(step -> step.getComponentCodes().contains(componentCode))
            .collect(Collectors.toList());
    }
    
    /**
     * Optimize the sequence for minimal assembly time
     */
    public AssemblySequence optimize() {
        // Implementation for optimizing assembly sequence
        // Consider parallel execution, tool changes, etc.
        return this; // Return optimized sequence
    }
}
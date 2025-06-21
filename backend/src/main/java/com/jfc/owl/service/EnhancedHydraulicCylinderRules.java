package com.jfc.owl.service;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.vocabulary.RDF;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jfc.rdb.tiptop.repository.ImaRepository;
import com.jfc.owl.ontology.HydraulicCylinderOntology;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Integrated Enhanced Hydraulic Cylinder Rules Service
 * 
 * This service provides comprehensive rule-based reasoning capabilities for hydraulic cylinders,
 * integrating domain-specific knowledge with OWL ontologies to support intelligent BOM generation,
 * component compatibility assessment, and similarity analysis.
 * 
 * Key Features:
 * - Advanced SWRL-like rules for cylinder classification and component compatibility
 * - Integration with specialized hydraulic cylinder ontology
 * - Intelligent component suggestion with confidence scoring
 * - Comprehensive BOM generation with assembly sequence and maintenance recommendations
 * - Enhanced similarity analysis for finding related cylinders
 * - Support for complex reasoning patterns and performance optimization
 * 
 * @author JFC Engineering Team
 * @version 3.0
 */
@Service
public class EnhancedHydraulicCylinderRules {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedHydraulicCylinderRules.class);
    
    // Namespace constants
    private static final String BASE_URI = "http://www.jfc.com/tiptop/ontology#";
    private static final String HC_URI = "http://www.jfc.com/tiptop/hydraulic-cylinder#";
    
    // Performance optimization caches
    private final Map<String, CylinderSpecs> specsCache = new ConcurrentHashMap<>();
    private final Map<String, List<Rule>> ruleCache = new ConcurrentHashMap<>();
    
    @Autowired
    private ImaRepository imaRepository;
    
    @Autowired
    private HydraulicCylinderOntology hydraulicCylinderOntology;

    /**
     * Generates comprehensive SWRL-like rules specific to hydraulic cylinder domain knowledge
     * @return A list of rules in Jena rule format
     */
    public List<String> getEnhancedHydraulicCylinderRules() {
        List<String> rules = new ArrayList<>();
        
        // Core identification and classification rules
        rules.addAll(getCylinderIdentificationRules());
        rules.addAll(getBoreSizeClassificationRules());
        rules.addAll(getStrokeLengthClassificationRules());
        rules.addAll(getSeriesClassificationRules());
        rules.addAll(getRodEndTypeClassificationRules());
        
        // Component compatibility rules
        rules.addAll(getComponentCompatibilityRules());
        rules.addAll(getBoreBasedCompatibilityRules());
        rules.addAll(getSeriesBasedCompatibilityRules());
        
        // Performance and application rules
        rules.addAll(getPerformanceApplicationRules());
        rules.addAll(getMaterialQualityRules());
        rules.addAll(getEnvironmentalCompatibilityRules());
        
        // Installation and mounting rules
        rules.addAll(getInstallationMountingRules());
        
        // Advanced reasoning rules
        rules.addAll(getAdvancedReasoningRules());
        rules.addAll(getOptimizationRules());
        
        logger.info("Generated {} enhanced hydraulic cylinder rules", rules.size());
        return rules;
    }
    
    /**
     * Core cylinder identification rules
     */
    private List<String> getCylinderIdentificationRules() {
        return Arrays.asList(
            // Identify hydraulic cylinders by code pattern
            "[IdentifyHydraulicCylinder: " +
            "(?material rdf:type <" + BASE_URI + "Material>) " +
            "(?material <" + BASE_URI + "itemCode> ?code) " +
            "regex(?code, '^[34].*') " +
            "-> " +
            "(?material rdf:type <" + HC_URI + "HydraulicCylinder>)]",
            
            // Identify cylinder components by code pattern
            "[IdentifyComponentItem: " +
            "(?material rdf:type <" + BASE_URI + "Material>) " +
            "(?material <" + BASE_URI + "itemCode> ?code) " +
            "regex(?code, '^2.*') " +
            "-> " +
            "(?material rdf:type <" + BASE_URI + "ComponentItem>)]",
            
            // Validate cylinder specifications
            "[ValidateCylinderSpecs: " +
            "(?cylinder rdf:type <" + HC_URI + "HydraulicCylinder>) " +
            "(?cylinder <" + BASE_URI + "itemCode> ?code) " +
            "regex(?code, '^[34][0-9]{13,}.*') " +
            "-> " +
            "(?cylinder <" + HC_URI + "hasValidSpecifications> 'true'^^xsd:boolean)]"
        );
    }
    
    /**
     * Bore size classification rules with enhanced granularity
     */
    private List<String> getBoreSizeClassificationRules() {
        return Arrays.asList(
            // Micro bore (10-29mm)
            "[MicroBoreCylinder: " +
            "(?cylinder rdf:type <" + HC_URI + "HydraulicCylinder>) " +
            "(?cylinder <" + HC_URI + "bore> ?bore) " +
            "regex(?bore, '^0[1-2][0-9]$') " +
            "-> " +
            "(?cylinder rdf:type <" + HC_URI + "MicroBoreCylinder>)]",
            
            // Small bore (30-49mm)
            "[SmallBoreCylinder: " +
            "(?cylinder rdf:type <" + HC_URI + "HydraulicCylinder>) " +
            "(?cylinder <" + HC_URI + "bore> ?bore) " +
            "regex(?bore, '^0[3-4][0-9]$') " +
            "-> " +
            "(?cylinder rdf:type <" + HC_URI + "SmallBoreCylinder>)]",
            
            // Medium bore (50-99mm)
            "[MediumBoreCylinder: " +
            "(?cylinder rdf:type <" + HC_URI + "HydraulicCylinder>) " +
            "(?cylinder <" + HC_URI + "bore> ?bore) " +
            "regex(?bore, '^0[5-9][0-9]$') " +
            "-> " +
            "(?cylinder rdf:type <" + HC_URI + "MediumBoreCylinder>)]",
            
            // Large bore (100-149mm)
            "[LargeBoreCylinder: " +
            "(?cylinder rdf:type <" + HC_URI + "HydraulicCylinder>) " +
            "(?cylinder <" + HC_URI + "bore> ?bore) " +
            "regex(?bore, '^1[0-4][0-9]$') " +
            "-> " +
            "(?cylinder rdf:type <" + HC_URI + "LargeBoreCylinder>)]",
            
            // Extra large bore (150mm+)
            "[ExtraLargeBoreCylinder: " +
            "(?cylinder rdf:type <" + HC_URI + "HydraulicCylinder>) " +
            "(?cylinder <" + HC_URI + "bore> ?bore) " +
            "regex(?bore, '^[1-9][5-9][0-9]$|^[2-9][0-9][0-9]$') " +
            "-> " +
            "(?cylinder rdf:type <" + HC_URI + "ExtraLargeBoreCylinder>)]"
        );
    }
    
    /**
     * Stroke length classification rules with enhanced categories
     */
    private List<String> getStrokeLengthClassificationRules() {
        return Arrays.asList(
            // Short stroke (0-99mm)
            "[ShortStrokeCylinder: " +
            "(?cylinder rdf:type <" + HC_URI + "HydraulicCylinder>) " +
            "(?cylinder <" + HC_URI + "stroke> ?stroke) " +
            "regex(?stroke, '^00[0-9][0-9]$') " +
            "-> " +
            "(?cylinder rdf:type <" + HC_URI + "ShortStrokeCylinder>)]",
            
            // Medium stroke (100-499mm)
            "[MediumStrokeCylinder: " +
            "(?cylinder rdf:type <" + HC_URI + "HydraulicCylinder>) " +
            "(?cylinder <" + HC_URI + "stroke> ?stroke) " +
            "regex(?stroke, '^0[1-4][0-9][0-9]$') " +
            "-> " +
            "(?cylinder rdf:type <" + HC_URI + "MediumStrokeCylinder>)]",
            
            // Long stroke (500-999mm)
            "[LongStrokeCylinder: " +
            "(?cylinder rdf:type <" + HC_URI + "HydraulicCylinder>) " +
            "(?cylinder <" + HC_URI + "stroke> ?stroke) " +
            "regex(?stroke, '^0[5-9][0-9][0-9]$') " +
            "-> " +
            "(?cylinder rdf:type <" + HC_URI + "LongStrokeCylinder>)]",
            
            // Extra long stroke (1000mm+)
            "[ExtraLongStrokeCylinder: " +
            "(?cylinder rdf:type <" + HC_URI + "HydraulicCylinder>) " +
            "(?cylinder <" + HC_URI + "stroke> ?stroke) " +
            "regex(?stroke, '^[1-9][0-9][0-9][0-9]$') " +
            "-> " +
            "(?cylinder rdf:type <" + HC_URI + "ExtraLongStrokeCylinder>)]"
        );
    }
    
    /**
     * Series classification rules
     */
    private List<String> getSeriesClassificationRules() {
        return Arrays.asList(
            "[StandardSeries: " +
            "(?cylinder rdf:type <" + HC_URI + "HydraulicCylinder>) " +
            "(?cylinder <" + HC_URI + "series> '10') " +
            "-> " +
            "(?cylinder rdf:type <" + HC_URI + "StandardCylinder>)]",
            
            "[HeavyDutySeries: " +
            "(?cylinder rdf:type <" + HC_URI + "HydraulicCylinder>) " +
            "(?cylinder <" + HC_URI + "series> '11') " +
            "-> " +
            "(?cylinder rdf:type <" + HC_URI + "HeavyDutyCylinder>)]",
            
            "[CompactSeries: " +
            "(?cylinder rdf:type <" + HC_URI + "HydraulicCylinder>) " +
            "(?cylinder <" + HC_URI + "series> '12') " +
            "-> " +
            "(?cylinder rdf:type <" + HC_URI + "CompactCylinder>)]",
            
            "[LightDutySeries: " +
            "(?cylinder rdf:type <" + HC_URI + "HydraulicCylinder>) " +
            "(?cylinder <" + HC_URI + "series> '13') " +
            "-> " +
            "(?cylinder rdf:type <" + HC_URI + "LightDutyCylinder>)]"
        );
    }
    
    /**
     * Rod end type classification rules
     */
    private List<String> getRodEndTypeClassificationRules() {
        return Arrays.asList(
            "[YokeRodEnd: " +
            "(?cylinder rdf:type <" + HC_URI + "HydraulicCylinder>) " +
            "(?cylinder <" + HC_URI + "rodEndType> 'Y') " +
            "-> " +
            "(?cylinder rdf:type <" + HC_URI + "YokeRodEndCylinder>)]",
            
            "[InternalThreadRodEnd: " +
            "(?cylinder rdf:type <" + HC_URI + "HydraulicCylinder>) " +
            "(?cylinder <" + HC_URI + "rodEndType> 'I') " +
            "-> " +
            "(?cylinder rdf:type <" + HC_URI + "InternalThreadRodEndCylinder>)]",
            
            "[ExternalThreadRodEnd: " +
            "(?cylinder rdf:type <" + HC_URI + "HydraulicCylinder>) " +
            "(?cylinder <" + HC_URI + "rodEndType> 'E') " +
            "-> " +
            "(?cylinder rdf:type <" + HC_URI + "ExternalThreadRodEndCylinder>)]",
            
            "[PinRodEnd: " +
            "(?cylinder rdf:type <" + HC_URI + "HydraulicCylinder>) " +
            "(?cylinder <" + HC_URI + "rodEndType> 'P') " +
            "-> " +
            "(?cylinder rdf:type <" + HC_URI + "PinRodEndCylinder>)]"
        );
    }
    
    /**
     * Component compatibility rules based on multiple factors
     */
    private List<String> getComponentCompatibilityRules() {
        return Arrays.asList(
            // Basic series compatibility
            "[SeriesCompatibility: " +
            "(?cylinder rdf:type <" + HC_URI + "HydraulicCylinder>) " +
            "(?cylinder <" + HC_URI + "series> ?series) " +
            "(?component rdf:type <" + BASE_URI + "ComponentItem>) " +
            "(?component <" + BASE_URI + "itemCode> ?compCode) " +
            "regex(?compCode, concat('^2.', ?series, '.*')) " +
            "-> " +
            "(?component <" + HC_URI + "compatibleWith> ?cylinder)]",
            
            // Barrel component identification
            "[BarrelComponent: " +
            "(?component rdf:type <" + BASE_URI + "ComponentItem>) " +
            "(?component <" + BASE_URI + "itemCode> ?code) " +
            "regex(?code, '^20[0-2].*') " +
            "-> " +
            "(?component rdf:type <" + HC_URI + "CylinderBarrel>)]",
            
            // Piston component identification
            "[PistonComponent: " +
            "(?component rdf:type <" + BASE_URI + "ComponentItem>) " +
            "(?component <" + BASE_URI + "itemCode> ?code) " +
            "regex(?code, '^21[0-1].*') " +
            "-> " +
            "(?component rdf:type <" + HC_URI + "Piston>)]",
            
            // Piston rod component identification
            "[PistonRodComponent: " +
            "(?component rdf:type <" + BASE_URI + "ComponentItem>) " +
            "(?component <" + BASE_URI + "itemCode> ?code) " +
            "regex(?code, '^21[2-3].*') " +
            "-> " +
            "(?component rdf:type <" + HC_URI + "PistonRod>)]",
            
            // End cap component identification
            "[EndCapComponent: " +
            "(?component rdf:type <" + BASE_URI + "ComponentItem>) " +
            "(?component <" + BASE_URI + "itemCode> ?code) " +
            "regex(?code, '^22[0-3].*') " +
            "-> " +
            "(?component rdf:type <" + HC_URI + "EndCap>)]",
            
            // Sealing component identification
            "[SealingComponent: " +
            "(?component rdf:type <" + BASE_URI + "ComponentItem>) " +
            "(?component <" + BASE_URI + "itemCode> ?code) " +
            "regex(?code, '^25[0-2].*') " +
            "-> " +
            "(?component rdf:type <" + HC_URI + "SealingComponent>)]",
            
            // Bushing component identification
            "[BushingComponent: " +
            "(?component rdf:type <" + BASE_URI + "ComponentItem>) " +
            "(?component <" + BASE_URI + "itemCode> ?code) " +
            "regex(?code, '^26[0-1].*') " +
            "-> " +
            "(?component rdf:type <" + HC_URI + "Bushing>)]",
            
            // Gasket component identification
            "[GasketComponent: " +
            "(?component rdf:type <" + BASE_URI + "ComponentItem>) " +
            "(?component <" + BASE_URI + "itemCode> ?code) " +
            "regex(?code, '^27[0-1].*') " +
            "-> " +
            "(?component rdf:type <" + HC_URI + "Gasket>)]"
        );
    }
    
    /**
     * Bore-based compatibility rules for components
     */
    private List<String> getBoreBasedCompatibilityRules() {
        return Arrays.asList(
            // Micro bore compatibility
            "[MicroBoreCompatibility: " +
            "(?cylinder rdf:type <" + HC_URI + "MicroBoreCylinder>) " +
            "(?component rdf:type <" + BASE_URI + "ComponentItem>) " +
            "(?component <" + BASE_URI + "itemCode> ?compCode) " +
            "regex(?compCode, '^2[0-7][0-1].*') " +
            "-> " +
            "(?component <" + HC_URI + "compatibleWith> ?cylinder)]",
            
            // Small bore compatibility
            "[SmallBoreCompatibility: " +
            "(?cylinder rdf:type <" + HC_URI + "SmallBoreCylinder>) " +
            "(?component rdf:type <" + BASE_URI + "ComponentItem>) " +
            "(?component <" + BASE_URI + "itemCode> ?compCode) " +
            "regex(?compCode, '^2[0-7][2-4].*') " +
            "-> " +
            "(?component <" + HC_URI + "compatibleWith> ?cylinder)]",
            
            // Medium bore compatibility
            "[MediumBoreCompatibility: " +
            "(?cylinder rdf:type <" + HC_URI + "MediumBoreCylinder>) " +
            "(?component rdf:type <" + BASE_URI + "ComponentItem>) " +
            "(?component <" + BASE_URI + "itemCode> ?compCode) " +
            "regex(?compCode, '^2[0-7][5-9].*') " +
            "-> " +
            "(?component <" + HC_URI + "compatibleWith> ?cylinder)]",
            
            // Large bore compatibility
            "[LargeBoreCompatibility: " +
            "(?cylinder rdf:type <" + HC_URI + "LargeBoreCylinder>) " +
            "(?component rdf:type <" + BASE_URI + "ComponentItem>) " +
            "(?component <" + BASE_URI + "itemCode> ?compCode) " +
            "regex(?compCode, '^2[1-7][0-4].*') " +
            "-> " +
            "(?component <" + HC_URI + "compatibleWith> ?cylinder)]",
            
            // Extra large bore compatibility
            "[ExtraLargeBoreCompatibility: " +
            "(?cylinder rdf:type <" + HC_URI + "ExtraLargeBoreCylinder>) " +
            "(?component rdf:type <" + BASE_URI + "ComponentItem>) " +
            "(?component <" + BASE_URI + "itemCode> ?compCode) " +
            "regex(?compCode, '^2[1-7][5-9].*|^2[2-7].*') " +
            "-> " +
            "(?component <" + HC_URI + "compatibleWith> ?cylinder)]"
        );
    }
    
    /**
     * Series-based compatibility rules
     */
    private List<String> getSeriesBasedCompatibilityRules() {
        return Arrays.asList(
            // Standard series component compatibility
            "[StandardSeriesCompatibility: " +
            "(?cylinder rdf:type <" + HC_URI + "StandardCylinder>) " +
            "(?component rdf:type <" + BASE_URI + "ComponentItem>) " +
            "(?component <" + BASE_URI + "itemName> ?name) " +
            "regex(?name, '.*[Ss]tandard.*|.*S10.*') " +
            "-> " +
            "(?component <" + HC_URI + "recommendedFor> ?cylinder)]",
            
            // Heavy duty series component compatibility
            "[HeavyDutySeriesCompatibility: " +
            "(?cylinder rdf:type <" + HC_URI + "HeavyDutyCylinder>) " +
            "(?component rdf:type <" + BASE_URI + "ComponentItem>) " +
            "(?component <" + BASE_URI + "itemName> ?name) " +
            "regex(?name, '.*[Hh]eavy.*[Dd]uty.*|.*HD.*|.*S11.*') " +
            "-> " +
            "(?component <" + HC_URI + "recommendedFor> ?cylinder)]"
        );
    }
    
    /**
     * Performance and application rules
     */
    private List<String> getPerformanceApplicationRules() {
        return Arrays.asList(
            // High pressure application
            "[HighPressureApplication: " +
            "(?cylinder rdf:type <" + HC_URI + "HeavyDutyCylinder>) " +
            "(?cylinder <" + HC_URI + "bore> ?bore) " +
            "regex(?bore, '^[1-9][0-9][0-9]$') " +
            "-> " +
            "(?cylinder rdf:type <" + HC_URI + "HighPressureCylinder>)]",
            
            // High speed application
            "[HighSpeedApplication: " +
            "(?cylinder rdf:type <" + HC_URI + "CompactCylinder>) " +
            "(?cylinder <" + HC_URI + "stroke> ?stroke) " +
            "regex(?stroke, '^0[0-2][0-9][0-9]$') " +
            "-> " +
            "(?cylinder rdf:type <" + HC_URI + "HighSpeedCylinder>)]",
            
            // Precision application
            "[PrecisionApplication: " +
            "(?cylinder rdf:type <" + HC_URI + "StandardCylinder>) " +
            "(?cylinder <" + HC_URI + "bore> ?bore) " +
            "regex(?bore, '^0[5-8][0-9]$') " +
            "-> " +
            "(?cylinder rdf:type <" + HC_URI + "PrecisionCylinder>)]"
        );
    }
    
    /**
     * Material quality rules
     */
    private List<String> getMaterialQualityRules() {
        return Arrays.asList(
            // High quality seal requirements
            "[HighQualitySealRequirement: " +
            "(?cylinder rdf:type <" + HC_URI + "HighPressureCylinder>) " +
            "(?seal rdf:type <" + HC_URI + "SealingComponent>) " +
            "(?seal <" + BASE_URI + "itemName> ?sealName) " +
            "regex(?sealName, '.*[Hh]igh.*[Pp]ressure.*|.*[Hh][Pp].*|.*[Vv]iton.*') " +
            "-> " +
            "(?seal <" + HC_URI + "recommendedFor> ?cylinder)]",
            
            // Corrosion resistant requirements
            "[CorrosionResistantRequirement: " +
            "(?cylinder rdf:type <" + HC_URI + "HydraulicCylinder>) " +
            "(?cylinder <" + HC_URI + "environmentType> 'CORROSIVE') " +
            "(?component rdf:type <" + BASE_URI + "ComponentItem>) " +
            "(?component <" + HC_URI + "material> ?material) " +
            "regex(?material, '.*[Ss]tainless.*|.*316.*|.*[Cc]orrosion.*[Rr]esistant.*') " +
            "-> " +
            "(?component <" + HC_URI + "recommendedFor> ?cylinder)]"
        );
    }
    
    /**
     * Environmental compatibility rules
     */
    private List<String> getEnvironmentalCompatibilityRules() {
        return Arrays.asList(
            // High temperature environment
            "[HighTemperatureCompatibility: " +
            "(?cylinder rdf:type <" + HC_URI + "HydraulicCylinder>) " +
            "(?cylinder <" + HC_URI + "operatingTemperature> ?temp) " +
            "greaterThan(?temp, 80) " +
            "(?component rdf:type <" + HC_URI + "SealingComponent>) " +
            "(?component <" + HC_URI + "material> ?material) " +
            "regex(?material, '.*[Vv]iton.*|.*PTFE.*|.*[Ff]luor.*') " +
            "-> " +
            "(?component <" + HC_URI + "recommendedFor> ?cylinder)]",
            
            // Low temperature environment
            "[LowTemperatureCompatibility: " +
            "(?cylinder rdf:type <" + HC_URI + "HydraulicCylinder>) " +
            "(?cylinder <" + HC_URI + "operatingTemperature> ?temp) " +
            "lessThan(?temp, -20) " +
            "(?component rdf:type <" + HC_URI + "SealingComponent>) " +
            "(?component <" + HC_URI + "material> ?material) " +
            "regex(?material, '.*[Ss]ilicone.*|.*[Ll]ow.*[Tt]emp.*') " +
            "-> " +
            "(?component <" + HC_URI + "recommendedFor> ?cylinder)]"
        );
    }
    
    /**
     * Installation and mounting rules
     */
    private List<String> getInstallationMountingRules() {
        return Arrays.asList(
            // Front attachment compatibility
            "[FrontAttachmentCompatibility: " +
            "(?cylinder rdf:type <" + HC_URI + "HydraulicCylinder>) " +
            "(?cylinder <" + HC_URI + "installation> 'FA') " +
            "(?component rdf:type <" + BASE_URI + "ComponentItem>) " +
            "(?component <" + BASE_URI + "itemCode> ?compCode) " +
            "regex(?compCode, '^203.*') " +
            "-> " +
            "(?component <" + HC_URI + "compatibleWith> ?cylinder)]",
            
            // Rear attachment compatibility
            "[RearAttachmentCompatibility: " +
            "(?cylinder rdf:type <" + HC_URI + "HydraulicCylinder>) " +
            "(?cylinder <" + HC_URI + "installation> 'RA') " +
            "(?component rdf:type <" + BASE_URI + "ComponentItem>) " +
            "(?component <" + BASE_URI + "itemCode> ?compCode) " +
            "regex(?compCode, '^204.*') " +
            "-> " +
            "(?component <" + HC_URI + "compatibleWith> ?cylinder)]",
            
            // Trunnion mount compatibility
            "[TrunnionMountCompatibility: " +
            "(?cylinder rdf:type <" + HC_URI + "HydraulicCylinder>) " +
            "(?cylinder <" + HC_URI + "installation> 'TM') " +
            "(?component rdf:type <" + BASE_URI + "ComponentItem>) " +
            "(?component <" + BASE_URI + "itemCode> ?compCode) " +
            "regex(?compCode, '^205.*') " +
            "-> " +
            "(?component <" + HC_URI + "compatibleWith> ?cylinder)]"
        );
    }
    
    /**
     * Advanced reasoning rules for complex relationships
     */
    private List<String> getAdvancedReasoningRules() {
        return Arrays.asList(
            // Complex configuration detection
            "[ComplexConfiguration: " +
            "(?cylinder rdf:type <" + HC_URI + "LargeBoreCylinder>) " +
            "(?cylinder rdf:type <" + HC_URI + "LongStrokeCylinder>) " +
            "(?cylinder rdf:type <" + HC_URI + "HeavyDutyCylinder>) " +
            "-> " +
            "(?cylinder rdf:type <" + HC_URI + "ComplexConfigurationCylinder>)]",
            
            // Redundant sealing requirement
            "[RedundantSealingRequirement: " +
            "(?cylinder rdf:type <" + HC_URI + "HighPressureCylinder>) " +
            "(?cylinder rdf:type <" + HC_URI + "LargeBoreCylinder>) " +
            "-> " +
            "(?cylinder <" + HC_URI + "requiresRedundantSealing> 'true'^^xsd:boolean)]",
            
            // Enhanced bushing requirement
            "[EnhancedBushingRequirement: " +
            "(?cylinder rdf:type <" + HC_URI + "LongStrokeCylinder>) " +
            "(?cylinder rdf:type <" + HC_URI + "HeavyDutyCylinder>) " +
            "-> " +
            "(?cylinder <" + HC_URI + "requiresEnhancedBushing> 'true'^^xsd:boolean)]"
        );
    }
    
    /**
     * Optimization rules for performance
     */
    private List<String> getOptimizationRules() {
        return Arrays.asList(
            // Optimal design detection
            "[OptimalDesign: " +
            "(?cylinder rdf:type <" + HC_URI + "HydraulicCylinder>) " +
            "(?cylinder <" + HC_URI + "bore> ?bore) " +
            "(?cylinder <" + HC_URI + "stroke> ?stroke) " +
            "(?cylinder <" + HC_URI + "rodEndType> 'Y') " +
            "regex(?bore, '^0[8-9][0-9]$|^1[0-2][0-9]$') " +
            "regex(?stroke, '^0[1-5][0-9][0-9]$') " +
            "-> " +
            "(?cylinder rdf:type <" + HC_URI + "OptimalDesignCylinder>)]",
            
            // Performance optimization for high-speed applications
            "[HighSpeedOptimization: " +
            "(?cylinder rdf:type <" + HC_URI + "HighSpeedCylinder>) " +
            "(?component rdf:type <" + HC_URI + "Bushing>) " +
            "(?component <" + HC_URI + "material> ?material) " +
            "regex(?material, '.*[Bb]ronze.*|.*[Ss]elf.*[Ll]ubr.*') " +
            "-> " +
            "(?component <" + HC_URI + "recommendedFor> ?cylinder)]"
        );
    }
    
    /**
     * Apply enhanced hydraulic cylinder specific rules and generate inferences
     * 
     * @param ontModel The original ontology model
     * @return InfModel The inference model with applied rules
     */
    public InfModel applyEnhancedHydraulicCylinderRules(OntModel ontModel) {
        try {
            logger.info("Applying enhanced hydraulic cylinder rules with ontology integration");
            
            // Merge specialized ontology if available
            if (hydraulicCylinderOntology != null && hydraulicCylinderOntology.getOntologyModel() != null) {
                ontModel.add(hydraulicCylinderOntology.getOntologyModel());
                logger.debug("Merged specialized hydraulic cylinder ontology");
            }
            
            // Get or create cached rules
            List<Rule> ruleList = getCachedRules();
            
            // Create and configure the reasoner
            GenericRuleReasoner ruleReasoner = new GenericRuleReasoner(ruleList);
            ruleReasoner.setTransitiveClosureCaching(true);
            ruleReasoner.setOWLTranslation(true);
            ruleReasoner.setDerivationLogging(true);
            
            // Apply rules to the model
            InfModel infModel = ModelFactory.createInfModel(ruleReasoner, ontModel);
            
            // Validate the inference model
            validateInferenceModel(infModel);
            
            logger.info("Enhanced hydraulic cylinder rules applied successfully");
            return infModel;
            
        } catch (Exception e) {
            logger.error("Error applying enhanced hydraulic cylinder rules", e);
            throw new RuntimeException("Failed to apply hydraulic cylinder rules", e);
        }
    }
    
    /**
     * Get cached rules or parse new ones
     */
    private List<Rule> getCachedRules() {
        return ruleCache.computeIfAbsent("hydraulicCylinderRules", k -> {
            List<Rule> rules = new ArrayList<>();
            List<String> ruleTexts = getEnhancedHydraulicCylinderRules();
            
            for (int i = 0; i < ruleTexts.size(); i++) {
                try {
                    Rule rule = Rule.parseRule(ruleTexts.get(i));
                    rules.add(rule);
                } catch (Exception e) {
                    logger.error("Error parsing rule {}: {}", i + 1, e.getMessage());
                }
            }
            
            logger.info("Parsed and cached {} rules", rules.size());
            return rules;
        });
    }
    
    /**
     * Validate the inference model
     */
    private void validateInferenceModel(InfModel infModel) {
        try {
            long cylinderCount = countResourcesOfType(infModel, HC_URI + "HydraulicCylinder");
            long componentCount = countResourcesOfType(infModel, BASE_URI + "ComponentItem");
            
            logger.info("Inference model contains {} cylinders and {} components", 
                       cylinderCount, componentCount);
            
            // Check for classification consistency
            validateClassificationConsistency(infModel);
            
        } catch (Exception e) {
            logger.warn("Error during inference model validation: {}", e.getMessage());
        }
    }
    
    /**
     * Validate classification consistency
     */
    private void validateClassificationConsistency(InfModel infModel) {
        StmtIterator cylinders = infModel.listStatements(null, RDF.type, 
            infModel.getResource(HC_URI + "HydraulicCylinder"));
        
        while (cylinders.hasNext()) {
            Resource cylinder = cylinders.next().getSubject();
            
            // Check bore size classification consistency
            int boreClassifications = 0;
            for (String boreType : Arrays.asList("MicroBore", "SmallBore", "MediumBore", "LargeBore", "ExtraLargeBore")) {
                if (infModel.contains(cylinder, RDF.type, infModel.getResource(HC_URI + boreType + "Cylinder"))) {
                    boreClassifications++;
                }
            }
            
            if (boreClassifications > 1) {
                logger.warn("Multiple bore size classifications for cylinder: {}", cylinder.getURI());
            }
        }
    }
    
    /**
     * Count resources of a specific type
     */
    private long countResourcesOfType(InfModel model, String typeUri) {
        return model.listStatements(null, RDF.type, model.getResource(typeUri))
                   .toList()
                   .size();
    }
    
    /**
     * Enhanced component suggestion with intelligent filtering and scoring
     * 
     * @param masterItemCode The master item code of the hydraulic cylinder
     * @param ontModel The ontology model containing component information
     * @return A map of component types to suggested components with confidence scores
     */
    public Map<String, List<Map<String, Object>>> suggestEnhancedComponents(String masterItemCode, OntModel ontModel) {
        logger.info("Generating enhanced component suggestions for cylinder: {}", masterItemCode);
        
        Map<String, List<Map<String, Object>>> suggestions = new HashMap<>();
        
        try {
            // Apply hydraulic cylinder rules
            InfModel infModel = applyEnhancedHydraulicCylinderRules(ontModel);
            
            // Get the cylinder individual
            Resource cylinderResource = infModel.getResource(BASE_URI + "Material_" + sanitizeForUri(masterItemCode));
            
            // Check if this is actually a cylinder
            if (!checkIfCylinder(infModel, cylinderResource)) {
                logger.warn("Item {} is not identified as a cylinder", masterItemCode);
                return suggestions;
            }
            
            // Extract cylinder specifications (with caching)
            CylinderSpecs specs = extractCylinderSpecs(infModel, cylinderResource, masterItemCode);
            logger.debug("Extracted specs for {}: {}", masterItemCode, specs);
            
            // Initialize component categories
            initializeComponentCategories(suggestions);
            
            // Find compatible components for each category
            findCompatibleComponents(infModel, specs, suggestions);
            
            // Apply intelligent filtering
            applyIntelligentFiltering(suggestions, specs);
            
            // Sort suggestions by confidence score
            sortSuggestionsByConfidence(suggestions);
            
            // Limit suggestions per category for performance
            limitSuggestionsPerCategory(suggestions, 10);
            
            logger.info("Generated {} component categories with suggestions for cylinder {}", 
                       suggestions.size(), masterItemCode);
            
            return suggestions;
            
        } catch (Exception e) {
            logger.error("Error generating enhanced component suggestions for cylinder: " + masterItemCode, e);
            throw new RuntimeException("Failed to generate component suggestions", e);
        }
    }
    
    /**
     * Extract cylinder specifications with caching
     */
    private CylinderSpecs extractCylinderSpecs(InfModel infModel, Resource cylinderResource, String masterItemCode) {
        return specsCache.computeIfAbsent(masterItemCode, k -> {
            CylinderSpecs specs = new CylinderSpecs();
            specs.itemCode = masterItemCode;
            
            // Extract from ontology model
            extractSpecsFromOntology(infModel, cylinderResource, specs);
            
            // If incomplete, extract from item code
            if (!specs.isComplete()) {
                extractSpecsFromCode(specs);
            }
            
            return specs;
        });
    }
    
    /**
     * Extract specifications from ontology
     */
    private void extractSpecsFromOntology(InfModel infModel, Resource cylinderResource, CylinderSpecs specs) {
        // Extract bore
        StmtIterator boreStmts = infModel.listStatements(cylinderResource, 
                                                        infModel.getProperty(HC_URI + "bore"), 
                                                        (org.apache.jena.rdf.model.RDFNode)null);
        if (boreStmts.hasNext()) {
            specs.bore = boreStmts.next().getObject().toString();
        }
        
        // Extract stroke
        StmtIterator strokeStmts = infModel.listStatements(cylinderResource, 
                                                          infModel.getProperty(HC_URI + "stroke"), 
                                                          (org.apache.jena.rdf.model.RDFNode)null);
        if (strokeStmts.hasNext()) {
            specs.stroke = strokeStmts.next().getObject().toString();
        }
        
        // Extract series
        StmtIterator seriesStmts = infModel.listStatements(cylinderResource, 
                                                          infModel.getProperty(HC_URI + "series"), 
                                                          (org.apache.jena.rdf.model.RDFNode)null);
        if (seriesStmts.hasNext()) {
            specs.series = seriesStmts.next().getObject().toString();
        }
        
        // Extract rod end type
        StmtIterator rodEndStmts = infModel.listStatements(cylinderResource, 
                                                          infModel.getProperty(HC_URI + "rodEndType"), 
                                                          (org.apache.jena.rdf.model.RDFNode)null);
        if (rodEndStmts.hasNext()) {
            specs.rodEndType = rodEndStmts.next().getObject().toString();
        }
        
        // Extract installation type
        StmtIterator installStmts = infModel.listStatements(cylinderResource, 
                                                           infModel.getProperty(HC_URI + "installation"), 
                                                           (org.apache.jena.rdf.model.RDFNode)null);
        if (installStmts.hasNext()) {
            specs.installationType = installStmts.next().getObject().toString();
        }
    }
    
    /**
     * Extract specifications from item code
     */
    private void extractSpecsFromCode(CylinderSpecs specs) {
        String code = specs.itemCode;
        if (code != null && code.length() >= 15) {
            // Position 0-1: Product type (3 or 4 for cylinders)
            if (specs.productType.isEmpty() && code.length() >= 2) {
                specs.productType = code.substring(0, 2);
            }
            
            // Position 2-3: Series
            if (specs.series.isEmpty() && code.length() >= 4) {
                specs.series = code.substring(2, 4);
            }
            
            // Position 4: Type
            if (specs.type.isEmpty() && code.length() >= 5) {
                specs.type = code.substring(4, 5);
            }
            
            // Position 5-7: Bore size
            if (specs.bore.isEmpty() && code.length() >= 8) {
                specs.bore = code.substring(5, 8);
            }
            
            // Position 8-9: Reserved/Special features
            if (specs.specialFeatures.isEmpty() && code.length() >= 10) {
                specs.specialFeatures = code.substring(8, 10);
            }
            
            // Position 10-13: Stroke length
            if (specs.stroke.isEmpty() && code.length() >= 14) {
                specs.stroke = code.substring(10, 14);
            }
            
            // Position 14: Rod end type
            if (specs.rodEndType.isEmpty() && code.length() >= 15) {
                specs.rodEndType = code.substring(14, 15);
            }
            
            // Position 15: Installation type (if available)
            if (specs.installationType.isEmpty() && code.length() >= 16) {
                specs.installationType = code.substring(15, 16);
            }
        }
    }
    
    /**
     * Initialize component categories
     */
    private void initializeComponentCategories(Map<String, List<Map<String, Object>>> suggestions) {
        String[] categories = {
            "CylinderBarrel", "Piston", "PistonRod", "EndCap",
            "SealingComponent", "Bushing", "Gasket", "Fastener"
        };
        
        for (String category : categories) {
            suggestions.put(category, new ArrayList<>());
        }
    }
    
    /**
     * Find compatible components
     */
    private void findCompatibleComponents(InfModel infModel, CylinderSpecs specs, 
                                        Map<String, List<Map<String, Object>>> suggestions) {
        
        // Find all components in the ontology
        StmtIterator components = infModel.listStatements(null, RDF.type, 
                                                         infModel.getResource(BASE_URI + "ComponentItem"));
        
        while (components.hasNext()) {
            Resource component = components.next().getSubject();
            
            // Get component details
            ComponentInfo compInfo = extractComponentInfo(infModel, component);
            
            if (compInfo.isValid()) {
                // Determine compatibility and confidence
                ComponentCompatibility compatibility = assessCompatibility(specs, compInfo, infModel, component);
                
                if (compatibility.isCompatible()) {
                    // Determine component category
                    String category = determineComponentCategory(compInfo, infModel, component);
                    
                    if (category != null && suggestions.containsKey(category)) {
                        Map<String, Object> suggestion = createComponentSuggestion(compInfo, compatibility, specs);
                        suggestions.get(category).add(suggestion);
                    }
                }
            }
        }
    }
    
    /**
     * Assess compatibility between cylinder and component
     */
    private ComponentCompatibility assessCompatibility(CylinderSpecs specs, ComponentInfo compInfo, 
                                                     InfModel infModel, Resource component) {
        ComponentCompatibility compatibility = new ComponentCompatibility();
        
        // Check if reasoner inferred compatibility
        Resource cylinderResource = infModel.getResource(BASE_URI + "Material_" + sanitizeForUri(specs.itemCode));
        
        // Check compatibleWith relationship
        if (infModel.contains(component, infModel.getProperty(HC_URI + "compatibleWith"), cylinderResource)) {
            compatibility.setCompatible(true);
            compatibility.setConfidenceScore(0.9);
            compatibility.setReason("Rule-based inference");
            return compatibility;
        }
        
        // Check recommendedFor relationship
        if (infModel.contains(component, infModel.getProperty(HC_URI + "recommendedFor"), cylinderResource)) {
            compatibility.setCompatible(true);
            compatibility.setConfidenceScore(0.95);
            compatibility.setReason("Highly recommended by rules");
            return compatibility;
        }
        
        // Manual compatibility assessment
        double score = calculateCompatibilityScore(specs, compInfo);
        compatibility.setCompatible(score > 0.3);
        compatibility.setConfidenceScore(score);
        compatibility.setReason(generateCompatibilityReason(score));
        
        return compatibility;
    }
    
    /**
     * Calculate compatibility score based on multiple factors
     */
    private double calculateCompatibilityScore(CylinderSpecs specs, ComponentInfo compInfo) {
        double score = 0.0;
        double weight = 0.0;
        
        // Series compatibility (weight: 0.3)
        if (isSeriesCompatible(specs.series, compInfo.code)) {
            score += 0.3;
        }
        weight += 0.3;
        
        // Bore compatibility (weight: 0.25)
        if (isBoreCompatible(specs.bore, compInfo.code, compInfo.name)) {
            score += 0.25;
        }
        weight += 0.25;
        
        // Code pattern compatibility (weight: 0.25)
        if (isCodePatternCompatible(specs, compInfo.code)) {
            score += 0.25;
        }
        weight += 0.25;
        
        // Name-based compatibility (weight: 0.2)
        if (isNameCompatible(specs, compInfo.name)) {
            score += 0.2;
        }
        weight += 0.2;
        
        return weight > 0 ? score / weight : 0.0;
    }
    
    /**
     * Generate compatibility reason based on score
     */
    private String generateCompatibilityReason(double score) {
        if (score >= 0.8) return "Excellent match - multiple compatibility factors";
        if (score >= 0.6) return "Good match - series and size compatible";
        if (score >= 0.4) return "Moderate match - partial compatibility";
        if (score >= 0.3) return "Possible match - limited compatibility";
        return "Low compatibility";
    }
    
    /**
     * Apply intelligent filtering to suggestions
     */
    private void applyIntelligentFiltering(Map<String, List<Map<String, Object>>> suggestions, 
                                         CylinderSpecs specs) {
        // Filter based on cylinder specifications
        for (Map.Entry<String, List<Map<String, Object>>> entry : suggestions.entrySet()) {
            String category = entry.getKey();
            List<Map<String, Object>> components = entry.getValue();
            
            // Apply category-specific filtering
            switch (category) {
                case "SealingComponent":
                    filterSealingComponents(components, specs);
                    break;
                case "Bushing":
                    filterBushingComponents(components, specs);
                    break;
                case "EndCap":
                    filterEndCapComponents(components, specs);
                    break;
            }
        }
    }
    
    /**
     * Filter sealing components based on cylinder requirements
     */
    private void filterSealingComponents(List<Map<String, Object>> components, CylinderSpecs specs) {
        // Enhance scores for appropriate seal types based on bore size
        try {
            int boreSize = Integer.parseInt(specs.bore);
            
            for (Map<String, Object> component : components) {
                String name = (String) component.get("name");
                double currentScore = (double) component.get("confidenceScore");
                
                if (boreSize > 100 && name != null && name.toLowerCase().contains("high pressure")) {
                    component.put("confidenceScore", Math.min(1.0, currentScore * 1.2));
                    component.put("compatibilityReason", component.get("compatibilityReason") + " - Recommended for large bore");
                }
            }
        } catch (NumberFormatException e) {
            // Skip if bore is not numeric
        }
    }
    
    /**
     * Filter bushing components based on stroke length
     */
    private void filterBushingComponents(List<Map<String, Object>> components, CylinderSpecs specs) {
        try {
            int strokeLength = Integer.parseInt(specs.stroke);
            
            for (Map<String, Object> component : components) {
                String name = (String) component.get("name");
                double currentScore = (double) component.get("confidenceScore");
                
                if (strokeLength > 500 && name != null && name.toLowerCase().contains("heavy duty")) {
                    component.put("confidenceScore", Math.min(1.0, currentScore * 1.15));
                    component.put("compatibilityReason", component.get("compatibilityReason") + " - Recommended for long stroke");
                }
            }
        } catch (NumberFormatException e) {
            // Skip if stroke is not numeric
        }
    }
    
    /**
     * Filter end cap components based on installation type
     */
    private void filterEndCapComponents(List<Map<String, Object>> components, CylinderSpecs specs) {
        if (!specs.installationType.isEmpty()) {
            for (Map<String, Object> component : components) {
                String code = (String) component.get("code");
                double currentScore = (double) component.get("confidenceScore");
                
                // Boost score for matching installation type
                if (code != null && code.contains(specs.installationType)) {
                    component.put("confidenceScore", Math.min(1.0, currentScore * 1.1));
                    component.put("compatibilityReason", component.get("compatibilityReason") + " - Matches installation type");
                }
            }
        }
    }
    
    /**
     * Sort suggestions by confidence score
     */
    private void sortSuggestionsByConfidence(Map<String, List<Map<String, Object>>> suggestions) {
        suggestions.values().forEach(categoryList -> 
            categoryList.sort((a, b) -> {
                Double scoreA = (Double) a.get("confidenceScore");
                Double scoreB = (Double) b.get("confidenceScore");
                return Double.compare(scoreB, scoreA); // Descending order
            })
        );
    }
    
    /**
     * Limit suggestions per category
     */
    private void limitSuggestionsPerCategory(Map<String, List<Map<String, Object>>> suggestions, int limit) {
        suggestions.replaceAll((category, list) -> 
            list.size() > limit ? list.subList(0, limit) : list
        );
    }
    
    /**
     * Generates a comprehensive BOM structure for a new hydraulic cylinder
     * 
     * @param newItemCode The code for the new cylinder
     * @param ontModel The ontology model
     * @return A map representing the BOM structure with suggested components
     */
    public Map<String, Object> generateHydraulicCylinderBom(String newItemCode, OntModel ontModel) {
        logger.info("Generating comprehensive hydraulic cylinder BOM for: {}", newItemCode);
        
        Map<String, Object> bomStructure = new HashMap<>();
        bomStructure.put("masterItemCode", newItemCode);
        bomStructure.put("masterItemType", "HydraulicCylinder");
        bomStructure.put("bomVersion", "3.0");
        
        try {
            // Get enhanced component suggestions
            Map<String, List<Map<String, Object>>> componentSuggestions = 
                suggestEnhancedComponents(newItemCode, ontModel);
            
            // Extract specifications
            CylinderSpecs specs = specsCache.get(newItemCode);
            if (specs == null) {
                specs = new CylinderSpecs();
                specs.itemCode = newItemCode;
                extractSpecsFromCode(specs);
            }
            
            // Add specifications to BOM
            bomStructure.put("specifications", createSpecificationMap(specs));
            
            // Add components with enhanced metadata
            bomStructure.put("components", enhanceComponentsWithMetadata(componentSuggestions, specs));
            
            // Generate quantity recommendations
            bomStructure.put("quantityRecommendations", generateQuantityRecommendations(specs));
            
            // Add assembly sequence
            bomStructure.put("assemblySequence", generateAssemblySequence(specs));
            
            // Add maintenance recommendations
            bomStructure.put("maintenanceRecommendations", generateMaintenanceRecommendations(specs));
            
            // Add BOM metadata
            bomStructure.put("metadata", generateBomMetadata(componentSuggestions));
            
            // Add validation results
            bomStructure.put("validation", validateBomCompleteness(componentSuggestions));
            
            logger.info("Generated comprehensive BOM structure for cylinder {}", newItemCode);
            
            return bomStructure;
            
        } catch (Exception e) {
            logger.error("Error generating hydraulic cylinder BOM for: " + newItemCode, e);
            throw new RuntimeException("Failed to generate BOM structure", e);
        }
    }
    
    /**
     * Create specification map from CylinderSpecs
     */
    private Map<String, String> createSpecificationMap(CylinderSpecs specs) {
        Map<String, String> specMap = new HashMap<>();
        specMap.put("productType", specs.productType);
        specMap.put("series", specs.series);
        specMap.put("type", specs.type);
        specMap.put("bore", specs.bore + "mm");
        specMap.put("stroke", specs.stroke + "mm");
        specMap.put("rodEndType", specs.rodEndType);
        specMap.put("installationType", specs.installationType);
        specMap.put("specialFeatures", specs.specialFeatures);
        return specMap;
    }
    
    /**
     * Enhance components with additional metadata
     */
    private Map<String, List<Map<String, Object>>> enhanceComponentsWithMetadata(
            Map<String, List<Map<String, Object>>> components, CylinderSpecs specs) {
        
        Map<String, List<Map<String, Object>>> enhanced = new HashMap<>();
        
        for (Map.Entry<String, List<Map<String, Object>>> entry : components.entrySet()) {
            String category = entry.getKey();
            List<Map<String, Object>> componentList = new ArrayList<>();
            
            for (Map<String, Object> component : entry.getValue()) {
                Map<String, Object> enhancedComponent = new HashMap<>(component);
                
                // Add quantity recommendation
                enhancedComponent.put("recommendedQuantity", calculateRecommendedQuantity(category, specs));
                
                // Add priority level
                enhancedComponent.put("priority", determinePriority(category, 
                    (Double) component.get("confidenceScore")));
                
                // Add replacement cycle
                enhancedComponent.put("replacementCycle", getReplacementCycle(category));
                
                componentList.add(enhancedComponent);
            }
            
            enhanced.put(category, componentList);
        }
        
        return enhanced;
    }
    
    /**
     * Generate quantity recommendations
     */
    private Map<String, Map<String, Integer>> generateQuantityRecommendations(CylinderSpecs specs) {
        Map<String, Map<String, Integer>> recommendations = new HashMap<>();
        
        // Standard configuration
        Map<String, Integer> standard = new HashMap<>();
        standard.put("CylinderBarrel", 1);
        standard.put("Piston", 1);
        standard.put("PistonRod", 1);
        standard.put("EndCap", 2);
        standard.put("SealingComponent", calculateSealQuantity(specs));
        standard.put("Bushing", calculateBushingQuantity(specs));
        standard.put("Gasket", 2);
        standard.put("Fastener", calculateFastenerQuantity(specs));
        recommendations.put("standard", standard);
        
        // Spare parts recommendation
        Map<String, Integer> spares = new HashMap<>();
        spares.put("SealingComponent", Math.max(2, calculateSealQuantity(specs) / 2));
        spares.put("Gasket", 1);
        spares.put("Bushing", 1);
        recommendations.put("spares", spares);
        
        return recommendations;
    }
    
    /**
     * Calculate seal quantity based on specifications
     */
    private int calculateSealQuantity(CylinderSpecs specs) {
        try {
            int bore = Integer.parseInt(specs.bore);
            if (bore <= 50) return 4;  // Small bore: piston seal, rod seal, wiper, O-ring
            if (bore <= 100) return 5;  // Medium bore: additional backup seal
            if (bore <= 150) return 6;  // Large bore: redundant sealing
            return 8;  // Extra large bore: complete redundancy
        } catch (NumberFormatException e) {
            return 4;  // Default
        }
    }
    
    /**
     * Calculate bushing quantity based on specifications
     */
    private int calculateBushingQuantity(CylinderSpecs specs) {
        try {
            int stroke = Integer.parseInt(specs.stroke);
            if (stroke <= 300) return 1;  // Short stroke: single bushing
            if (stroke <= 600) return 2;  // Medium stroke: front and rear
            return 3;  // Long stroke: additional support bushing
        } catch (NumberFormatException e) {
            return 2;  // Default
        }
    }
    
    /**
     * Calculate fastener quantity based on specifications
     */
    private int calculateFastenerQuantity(CylinderSpecs specs) {
        try {
            int bore = Integer.parseInt(specs.bore);
            if (bore <= 50) return 4;   // 4 tie rods for small bore
            if (bore <= 100) return 4;  // 4 tie rods for medium bore
            if (bore <= 150) return 6;  // 6 tie rods for large bore
            return 8;  // 8 tie rods for extra large bore
        } catch (NumberFormatException e) {
            return 4;  // Default
        }
    }
    
    /**
     * Calculate recommended quantity for a component category
     */
    private int calculateRecommendedQuantity(String category, CylinderSpecs specs) {
        switch (category) {
            case "CylinderBarrel":
            case "Piston":
            case "PistonRod":
                return 1;
            case "EndCap":
                return 2;
            case "SealingComponent":
                return calculateSealQuantity(specs);
            case "Bushing":
                return calculateBushingQuantity(specs);
            case "Gasket":
                return 2;
            case "Fastener":
                return calculateFastenerQuantity(specs);
            default:
                return 1;
        }
    }
    
    /**
     * Determine component priority
     */
    private String determinePriority(String category, double confidenceScore) {
        // Critical components
        if (Arrays.asList("CylinderBarrel", "Piston", "PistonRod", "EndCap").contains(category)) {
            return "CRITICAL";
        }
        
        // Essential sealing components
        if (category.equals("SealingComponent")) {
            return confidenceScore > 0.7 ? "HIGH" : "MEDIUM";
        }
        
        // Supporting components
        if (Arrays.asList("Bushing", "Gasket").contains(category)) {
            return confidenceScore > 0.8 ? "MEDIUM" : "LOW";
        }
        
        return "LOW";
    }
    
    /**
     * Get replacement cycle for component category
     */
    private String getReplacementCycle(String category) {
        switch (category) {
            case "CylinderBarrel":
            case "Piston":
            case "PistonRod":
            case "EndCap":
                return "10+ years (inspect annually)";
            case "SealingComponent":
                return "12-18 months";
            case "Bushing":
                return "2-3 years";
            case "Gasket":
                return "During major service";
            case "Fastener":
                return "Replace if damaged";
            default:
                return "As needed";
        }
    }
    
    /**
     * Generate assembly sequence based on cylinder specifications
     */
    private List<Map<String, Object>> generateAssemblySequence(CylinderSpecs specs) {
        List<Map<String, Object>> sequence = new ArrayList<>();
        
        // Step 1: Prepare barrel assembly
        Map<String, Object> step1 = new HashMap<>();
        step1.put("sequence", 1);
        step1.put("operation", "Prepare Barrel Assembly");
        step1.put("description", "Clean and inspect cylinder barrel bore");
        step1.put("requiredComponents", Arrays.asList("CylinderBarrel"));
        step1.put("tools", Arrays.asList("Bore gauge", "Cleaning kit"));
        step1.put("criticalPoints", Arrays.asList("Check bore surface finish", "Verify dimensional tolerance"));
        sequence.add(step1);
        
        // Step 2: Install rear end cap
        Map<String, Object> step2 = new HashMap<>();
        step2.put("sequence", 2);
        step2.put("operation", "Install Rear End Cap");
        step2.put("description", "Mount rear end cap with gasket and fasteners");
        step2.put("requiredComponents", Arrays.asList("EndCap", "Gasket", "Fastener"));
        step2.put("tools", Arrays.asList("Torque wrench", "Assembly fixture"));
        step2.put("criticalPoints", Arrays.asList("Proper gasket alignment", "Correct torque specification"));
        sequence.add(step2);
        
        // Step 3: Assemble piston
        Map<String, Object> step3 = new HashMap<>();
        step3.put("sequence", 3);
        step3.put("operation", "Assemble Piston");
        step3.put("description", "Install seals on piston and check dimensions");
        step3.put("requiredComponents", Arrays.asList("Piston", "SealingComponent"));
        step3.put("tools", Arrays.asList("Seal installation tool", "Micrometer"));
        step3.put("criticalPoints", Arrays.asList("Avoid seal damage", "Verify seal orientation"));
        sequence.add(step3);
        
        // Step 4: Install piston assembly
        Map<String, Object> step4 = new HashMap<>();
        step4.put("sequence", 4);
        step4.put("operation", "Install Piston Assembly");
        step4.put("description", "Insert piston with rod into cylinder barrel");
        step4.put("requiredComponents", Arrays.asList("Piston", "PistonRod"));
        step4.put("tools", Arrays.asList("Assembly guide", "Soft hammer"));
        step4.put("criticalPoints", Arrays.asList("Prevent seal rolling", "Maintain alignment"));
        sequence.add(step4);
        
        // Step 5: Install front end cap
        Map<String, Object> step5 = new HashMap<>();
        step5.put("sequence", 5);
        step5.put("operation", "Install Front End Cap");
        step5.put("description", "Mount front end cap with bushing and seals");
        step5.put("requiredComponents", Arrays.asList("EndCap", "Bushing", "SealingComponent", "Gasket"));
        step5.put("tools", Arrays.asList("Bushing driver", "Torque wrench"));
        step5.put("criticalPoints", Arrays.asList("Bushing alignment", "Rod seal installation"));
        sequence.add(step5);
        
        // Step 6: Final assembly
        Map<String, Object> step6 = new HashMap<>();
        step6.put("sequence", 6);
        step6.put("operation", "Final Assembly");
        step6.put("description", "Install rod end attachment and complete assembly");
        step6.put("requiredComponents", Arrays.asList("PistonRod"));
        step6.put("tools", Arrays.asList("Thread gauge", "Assembly fixture"));
        step6.put("criticalPoints", Arrays.asList("Verify rod end type", "Check thread engagement"));
        sequence.add(step6);
        
        // Step 7: Testing
        Map<String, Object> step7 = new HashMap<>();
        step7.put("sequence", 7);
        step7.put("operation", "Testing and Quality Control");
        step7.put("description", "Perform functional testing and leak check");
        step7.put("requiredComponents", Arrays.asList());
        step7.put("tools", Arrays.asList("Test bench", "Pressure gauge", "Flow meter"));
        step7.put("criticalPoints", Arrays.asList("Pressure test at 1.5x working pressure", "Check smooth operation", "Verify stroke length"));
        sequence.add(step7);
        
        return sequence;
    }
    
    /**
     * Generate maintenance recommendations
     */
    private Map<String, List<Map<String, Object>>> generateMaintenanceRecommendations(CylinderSpecs specs) {
        Map<String, List<Map<String, Object>>> maintenance = new HashMap<>();
        
        // Daily checks
        List<Map<String, Object>> daily = new ArrayList<>();
        daily.add(createMaintenanceItem("Visual Inspection", "Check for external leaks and damage", "Visual"));
        daily.add(createMaintenanceItem("Operation Check", "Verify smooth cylinder movement", "Functional"));
        maintenance.put("daily", daily);
        
        // Weekly maintenance
        List<Map<String, Object>> weekly = new ArrayList<>();
        weekly.add(createMaintenanceItem("Leak Detection", "Check all seal areas for leakage", "Visual"));
        weekly.add(createMaintenanceItem("Mounting Check", "Verify mounting bolts and alignment", "Mechanical"));
        maintenance.put("weekly", weekly);
        
        // Monthly maintenance
        List<Map<String, Object>> monthly = new ArrayList<>();
        monthly.add(createMaintenanceItem("Fluid Analysis", "Check hydraulic fluid condition", "Chemical"));
        monthly.add(createMaintenanceItem("Pressure Check", "Verify operating pressure", "Measurement"));
        monthly.add(createMaintenanceItem("Rod Condition", "Inspect rod surface for scoring", "Visual"));
        maintenance.put("monthly", monthly);
        
        // Annual maintenance
        List<Map<String, Object>> annual = new ArrayList<>();
        annual.add(createMaintenanceItem("Complete Inspection", "Disassemble and inspect all components", "Comprehensive"));
        annual.add(createMaintenanceItem("Seal Replacement", "Replace all sealing components", "Preventive"));
        annual.add(createMaintenanceItem("Dimension Check", "Measure critical dimensions", "Measurement"));
        annual.add(createMaintenanceItem("Performance Test", "Full functional testing", "Functional"));
        maintenance.put("annual", annual);
        
        // Condition-based maintenance
        List<Map<String, Object>> conditionBased = new ArrayList<>();
        conditionBased.add(createMaintenanceItem("Excessive Leakage", "Replace affected seals immediately", "Corrective"));
        conditionBased.add(createMaintenanceItem("Abnormal Operation", "Check for internal damage", "Diagnostic"));
        conditionBased.add(createMaintenanceItem("Pressure Drop", "Inspect seals and valves", "Diagnostic"));
        maintenance.put("conditionBased", conditionBased);
        
        return maintenance;
    }
    
    /**
     * Create maintenance item
     */
    private Map<String, Object> createMaintenanceItem(String task, String description, String type) {
        Map<String, Object> item = new HashMap<>();
        item.put("task", task);
        item.put("description", description);
        item.put("type", type);
        item.put("estimatedTime", estimateMaintenanceTime(type));
        return item;
    }
    
    /**
     * Estimate maintenance time based on type
     */
    private String estimateMaintenanceTime(String type) {
        switch (type) {
            case "Visual": return "5-10 minutes";
            case "Functional": return "10-15 minutes";
            case "Mechanical": return "15-30 minutes";
            case "Chemical": return "20-30 minutes";
            case "Measurement": return "30-45 minutes";
            case "Comprehensive": return "4-8 hours";
            case "Preventive": return "2-4 hours";
            case "Corrective": return "1-3 hours";
            case "Diagnostic": return "1-2 hours";
            default: return "Variable";
        }
    }
    
    /**
     * Generate BOM metadata
     */
    private Map<String, Object> generateBomMetadata(Map<String, List<Map<String, Object>>> components) {
        Map<String, Object> metadata = new HashMap<>();
        
        // Basic statistics
        metadata.put("generatedAt", new Date());
        metadata.put("generatedBy", "Enhanced Hydraulic Cylinder Rules Engine v3.0");
        metadata.put("totalCategories", components.size());
        
        // Component counts
        int totalComponents = 0;
        int highConfidenceComponents = 0;
        
        for (List<Map<String, Object>> categoryComponents : components.values()) {
            totalComponents += categoryComponents.size();
            
            for (Map<String, Object> component : categoryComponents) {
                Double confidence = (Double) component.get("confidenceScore");
                if (confidence != null && confidence > 0.8) {
                    highConfidenceComponents++;
                }
            }
        }
        
        metadata.put("totalComponents", totalComponents);
        metadata.put("highConfidenceComponents", highConfidenceComponents);
        metadata.put("averageConfidence", calculateAverageConfidence(components));
        metadata.put("completenessScore", calculateCompletenessScore(components));
        
        return metadata;
    }
    
    /**
     * Calculate average confidence across all components
     */
    private double calculateAverageConfidence(Map<String, List<Map<String, Object>>> components) {
        double totalScore = 0.0;
        int count = 0;
        
        for (List<Map<String, Object>> categoryComponents : components.values()) {
            for (Map<String, Object> component : categoryComponents) {
                Double confidence = (Double) component.get("confidenceScore");
                if (confidence != null) {
                    totalScore += confidence;
                    count++;
                }
            }
        }
        
        return count > 0 ? Math.round(totalScore / count * 100.0) / 100.0 : 0.0;
    }
    
    /**
     * Calculate BOM completeness score
     */
    private double calculateCompletenessScore(Map<String, List<Map<String, Object>>> components) {
        String[] requiredCategories = {
            "CylinderBarrel", "Piston", "PistonRod", "EndCap", "SealingComponent"
        };
        
        int presentCategories = 0;
        for (String category : requiredCategories) {
            if (components.containsKey(category) && !components.get(category).isEmpty()) {
                presentCategories++;
            }
        }
        
        return Math.round((double) presentCategories / requiredCategories.length * 100.0) / 100.0;
    }
    
    /**
     * Validate BOM completeness
     */
    private Map<String, Object> validateBomCompleteness(Map<String, List<Map<String, Object>>> components) {
        Map<String, Object> validation = new HashMap<>();
        
        // Check required components
        List<String> missingComponents = new ArrayList<>();
        Map<String, Integer> componentCounts = new HashMap<>();
        
        String[] requiredCategories = {
            "CylinderBarrel", "Piston", "PistonRod", "EndCap", "SealingComponent"
        };
        
        for (String category : requiredCategories) {
            if (!components.containsKey(category) || components.get(category).isEmpty()) {
                missingComponents.add(category);
                componentCounts.put(category, 0);
            } else {
                componentCounts.put(category, components.get(category).size());
            }
        }
        
        validation.put("isComplete", missingComponents.isEmpty());
        validation.put("missingComponents", missingComponents);
        validation.put("componentCounts", componentCounts);
        
        // Add warnings
        List<String> warnings = new ArrayList<>();
        if (componentCounts.getOrDefault("EndCap", 0) < 2) {
            warnings.add("Less than 2 end caps found - cylinder requires front and rear end caps");
        }
        if (componentCounts.getOrDefault("SealingComponent", 0) < 3) {
            warnings.add("Insufficient sealing components - minimum 3 required (piston seal, rod seal, wiper)");
        }
        
        validation.put("warnings", warnings);
        validation.put("validationTimestamp", new Date());
        
        return validation;
    }
    
    /**
     * Find similar cylinders using enhanced similarity analysis
     */
    public List<Map<String, Object>> findEnhancedSimilarCylinders(String newItemCode, OntModel ontModel) {
        logger.info("Finding similar cylinders for: {}", newItemCode);
        
        List<Map<String, Object>> similarCylinders = new ArrayList<>();
        
        try {
            // Extract specifications for the new cylinder
            CylinderSpecs newSpecs = new CylinderSpecs();
            newSpecs.itemCode = newItemCode;
            extractSpecsFromCode(newSpecs);
            
            // Apply reasoning
            InfModel infModel = applyEnhancedHydraulicCylinderRules(ontModel);
            
            // Find all cylinders
            StmtIterator cylinderStmts = infModel.listStatements(null, RDF.type, 
                                                                infModel.getResource(HC_URI + "HydraulicCylinder"));
            
            while (cylinderStmts.hasNext()) {
                Resource cylinder = cylinderStmts.next().getSubject();
                
                // Get cylinder code
                String cylinderCode = extractItemCode(infModel, cylinder);
                
                if (cylinderCode.isEmpty() || cylinderCode.equals(newItemCode)) {
                    continue;
                }
                
                // Extract specifications
                CylinderSpecs existingSpecs = specsCache.computeIfAbsent(cylinderCode, k -> {
                    CylinderSpecs specs = new CylinderSpecs();
                    specs.itemCode = cylinderCode;
                    extractSpecsFromOntology(infModel, cylinder, specs);
                    if (!specs.isComplete()) {
                        extractSpecsFromCode(specs);
                    }
                    return specs;
                });
                
                // Calculate similarity
                double similarityScore = calculateSimilarityScore(newSpecs, existingSpecs);
                
                if (similarityScore >= 0.3) {
                    Map<String, Object> similarCylinder = createSimilarCylinderInfo(
                        infModel, cylinder, existingSpecs, similarityScore);
                    similarCylinders.add(similarCylinder);
                }
            }
            
            // Sort by similarity score
            similarCylinders.sort((a, b) -> 
                Double.compare((Double) b.get("similarityScore"), (Double) a.get("similarityScore"))
            );
            
            // Limit results
            if (similarCylinders.size() > 20) {
                similarCylinders = similarCylinders.subList(0, 20);
            }
            
            logger.info("Found {} similar cylinders", similarCylinders.size());
            return similarCylinders;
            
        } catch (Exception e) {
            logger.error("Error finding similar cylinders", e);
            throw new RuntimeException("Failed to find similar cylinders", e);
        }
    }
    
    /**
     * Calculate similarity score between two cylinders
     */
    private double calculateSimilarityScore(CylinderSpecs specs1, CylinderSpecs specs2) {
        double score = 0.0;
        double weight = 0.0;
        
        // Series similarity (weight: 0.25)
        if (specs1.series.equals(specs2.series)) {
            score += 0.25;
        }
        weight += 0.25;
        
        // Bore similarity (weight: 0.3)
        double boreSimilarity = calculateDimensionSimilarity(specs1.bore, specs2.bore, 20);
        score += boreSimilarity * 0.3;
        weight += 0.3;
        
        // Stroke similarity (weight: 0.25)
        double strokeSimilarity = calculateDimensionSimilarity(specs1.stroke, specs2.stroke, 50);
        score += strokeSimilarity * 0.25;
        weight += 0.25;
        
        // Rod end type similarity (weight: 0.1)
        if (specs1.rodEndType.equals(specs2.rodEndType)) {
            score += 0.1;
        }
        weight += 0.1;
        
        // Installation type similarity (weight: 0.1)
        if (specs1.installationType.equals(specs2.installationType)) {
            score += 0.1;
        }
        weight += 0.1;
        
        return weight > 0 ? score / weight : 0.0;
    }
    
    /**
     * Calculate dimension similarity with tolerance
     */
    private double calculateDimensionSimilarity(String dim1, String dim2, int tolerance) {
        try {
            int val1 = Integer.parseInt(dim1);
            int val2 = Integer.parseInt(dim2);
            int diff = Math.abs(val1 - val2);
            
            if (diff == 0) return 1.0;
            if (diff <= tolerance / 4) return 0.9;
            if (diff <= tolerance / 2) return 0.7;
            if (diff <= tolerance) return 0.5;
            if (diff <= tolerance * 2) return 0.3;
            
            return 0.0;
        } catch (NumberFormatException e) {
            return dim1.equals(dim2) ? 1.0 : 0.0;
        }
    }
    
    /**
     * Create similar cylinder information
     */
    private Map<String, Object> createSimilarCylinderInfo(InfModel infModel, Resource cylinder, 
                                                         CylinderSpecs specs, double similarityScore) {
        Map<String, Object> info = new HashMap<>();
        info.put("code", specs.itemCode);
        info.put("similarityScore", Math.round(similarityScore * 100));
        
        // Get additional information
        StmtIterator nameStmts = infModel.listStatements(cylinder, 
                                                       infModel.getProperty(BASE_URI + "itemName"), 
                                                       (org.apache.jena.rdf.model.RDFNode)null);
        if (nameStmts.hasNext()) {
            info.put("name", nameStmts.next().getObject().toString());
        }
        
        // Add specifications
        Map<String, String> specMap = new HashMap<>();
        specMap.put("series", specs.series);
        specMap.put("bore", specs.bore);
        specMap.put("stroke", specs.stroke);
        specMap.put("rodEndType", specs.rodEndType);
        info.put("specifications", specMap);
        
        // Add similarity details
        List<String> similarities = new ArrayList<>();
        if (specs.series.equals(specs.series)) similarities.add("Same series");
        if (specs.bore.equals(specs.bore)) similarities.add("Same bore size");
        if (specs.rodEndType.equals(specs.rodEndType)) similarities.add("Same rod end type");
        info.put("similarities", similarities);
        
        return info;
    }
    
    /**
     * Extract item code from resource
     */
    private String extractItemCode(InfModel infModel, Resource resource) {
        StmtIterator codeStmts = infModel.listStatements(resource, 
                                                       infModel.getProperty(BASE_URI + "itemCode"), 
                                                       (org.apache.jena.rdf.model.RDFNode)null);
        return codeStmts.hasNext() ? codeStmts.next().getObject().toString() : "";
    }
    
    /**
     * Extract component information
     */
    private ComponentInfo extractComponentInfo(InfModel infModel, Resource component) {
        ComponentInfo info = new ComponentInfo();
        
        // Get code
        StmtIterator codeStmts = infModel.listStatements(component, 
                                                       infModel.getProperty(BASE_URI + "itemCode"), 
                                                       (org.apache.jena.rdf.model.RDFNode)null);
        if (codeStmts.hasNext()) {
            info.code = codeStmts.next().getObject().toString();
        }
        
        // Get name
        StmtIterator nameStmts = infModel.listStatements(component, 
                                                       infModel.getProperty(BASE_URI + "itemName"), 
                                                       (org.apache.jena.rdf.model.RDFNode)null);
        if (nameStmts.hasNext()) {
            info.name = nameStmts.next().getObject().toString();
        }
        
        // Get specification
        StmtIterator specStmts = infModel.listStatements(component, 
                                                       infModel.getProperty(BASE_URI + "itemSpec"), 
                                                       (org.apache.jena.rdf.model.RDFNode)null);
        if (specStmts.hasNext()) {
            info.spec = specStmts.next().getObject().toString();
        }
        
        return info;
    }
    
    /**
     * Determine component category from inference model
     */
    private String determineComponentCategory(ComponentInfo compInfo, InfModel infModel, Resource component) {
        // Check inferred types first
        String[] categoryTypes = {
            "CylinderBarrel", "Piston", "PistonRod", "EndCap",
            "SealingComponent", "Bushing", "Gasket", "Fastener"
        };
        
        for (String categoryType : categoryTypes) {
            if (infModel.contains(component, RDF.type, infModel.getResource(HC_URI + categoryType))) {
                return categoryType;
            }
        }
        
        // Fall back to code-based determination
        return determineComponentCategoryByCode(compInfo.code);
    }
    
    /**
     * Determine component category by code pattern
     */
    private String determineComponentCategoryByCode(String code) {
        if (code == null || code.length() < 5) return null;
        
        if (code.substring(0, 2).equals("11"))
    		//case "403": //分群碼
        	return "SealingComponent";
        
        String prefix = code.substring(2, 5);
        switch (prefix) {
            case "023":
            case "024":
            case "021":
                return "CylinderBarrel";
            case "011":
                return "Piston";
            case "016":
                return "PistonRod";
            case "001":
            case "006":
                return "EndCap"; //分群碼二 後3碼
            case "061":
            	return "TieRod";
            case "251":
            	return "Nut";
            case "056":
                return "Bushing";
            case "201":
            	return "CA";
            case "202":
            	return "CB";
            case "203":
            	return "FA";
            case "205":
            	return "TA";
            case "206":
            	return "TC";
            case "207":
            	return "LA";
            case "208":
            	return "LB";
            case "209":
            	return "YConnector";
            case "210":
            	return "IConnector";
            case "211":
            	return "PIN";
            case "212":
            	return "TB";
            case "270":
            case "271":
                return "Gasket";
            case "280":
            case "281":
                return "Fastener";
            default:
                return null;
        }
    }
    
    /**
     * Create component suggestion
     */
    private Map<String, Object> createComponentSuggestion(ComponentInfo compInfo, 
                                                        ComponentCompatibility compatibility,
                                                        CylinderSpecs specs) {
        Map<String, Object> suggestion = new HashMap<>();
        suggestion.put("code", compInfo.code);
        suggestion.put("name", compInfo.name);
        suggestion.put("spec", compInfo.spec);
        suggestion.put("confidenceScore", compatibility.getConfidenceScore());
        suggestion.put("compatibilityReason", compatibility.getReason());
        return suggestion;
    }
    
    /**
     * Check if resource is a cylinder
     */
    private boolean checkIfCylinder(InfModel infModel, Resource resource) {
        return infModel.contains(resource, RDF.type, infModel.getResource(HC_URI + "HydraulicCylinder")) ||
               infModel.contains(resource, RDF.type, infModel.getResource(BASE_URI + "Cylinder"));
    }
    
    /**
     * Check series compatibility
     */
    private boolean isSeriesCompatible(String cylinderSeries, String componentCode) {
        if (cylinderSeries.isEmpty() || componentCode.length() < 4) return false;
        String compSeries = componentCode.substring(2, 4);
        return cylinderSeries.equals(compSeries);
    }
    
    /**
     * Check bore compatibility
     */
    private boolean isBoreCompatible(String bore, String componentCode, String componentName) {
        if (bore.isEmpty()) return false;
        
        // Check code pattern
        if (componentCode.length() >= 5) {
            String compBoreRange = componentCode.substring(3, 5);
            try {
                int boreVal = Integer.parseInt(bore);
                int rangeStart = Integer.parseInt(compBoreRange) * 10;
                int rangeEnd = rangeStart + 9;
                
                if (boreVal >= rangeStart && boreVal <= rangeEnd) {
                    return true;
                }
            } catch (NumberFormatException e) {
                // Continue to name check
            }
        }
        
        // Check name
        if (componentName != null) {
            String nameLower = componentName.toLowerCase();
            return nameLower.contains(bore) || nameLower.contains("ø" + bore) || 
                   nameLower.contains("dia" + bore) || nameLower.contains("bore" + bore);
        }
        
        return false;
    }
    
    /**
     * Check code pattern compatibility
     */
    private boolean isCodePatternCompatible(CylinderSpecs specs, String componentCode) {
        if (componentCode.length() < 6) return false;
        
        // Component codes start with 2
        if (!componentCode.startsWith("2")) return false;
        
        // Check series match if available
        if (!specs.series.isEmpty() && componentCode.length() >= 4) {
            String compSeries = componentCode.substring(2, 4);
            return specs.series.equals(compSeries);
        }
        
        return true;
    }
    
    /**
     * Check name-based compatibility
     */
    private boolean isNameCompatible(CylinderSpecs specs, String componentName) {
        if (componentName == null) return false;
        
        String nameLower = componentName.toLowerCase();
        
        // Check for series
        if (!specs.series.isEmpty() && 
            (nameLower.contains("series " + specs.series) || 
             nameLower.contains("s" + specs.series))) {
            return true;
        }
        
        // Check for bore
        if (!specs.bore.isEmpty() && 
            (nameLower.contains(specs.bore) || 
             nameLower.contains("ø" + specs.bore))) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Utility method to sanitize strings for URI usage
     */
    private String sanitizeForUri(String input) {
        if (input == null) return "";
        return input.replaceAll("\\s+", "_")
                    .replaceAll("[^a-zA-Z0-9_\\-.]", "_");
    }
    
    /**
     * Wrapper method for backward compatibility
     */
    public List<Map<String, Object>> findSimilarCylinders(String newItemCode, OntModel ontModel) {
        return findEnhancedSimilarCylinders(newItemCode, ontModel);
    }
    
    /**
     * Helper class to hold cylinder specifications
     */
    public static class CylinderSpecs {
        public String itemCode = "";
        public String productType = "";
        public String series = "";
        public String type = "";
        public String bore = "";
        public String specialFeatures = "";
        public String stroke = "";
        public String rodEndType = "";
        public String installationType = "";
        
        public boolean isComplete() {
            return !itemCode.isEmpty() && !series.isEmpty() && !bore.isEmpty() && 
                   !stroke.isEmpty() && !rodEndType.isEmpty();
        }
        
        @Override
        public String toString() {
            return String.format("CylinderSpecs{code='%s', series='%s', type='%s', bore='%s', stroke='%s', rodEnd='%s', installation='%s'}", 
                               itemCode, series, type, bore, stroke, rodEndType, installationType);
        }
    }
    
    /**
     * Helper class to hold component information
     */
    public static class ComponentInfo {
        public String code = "";
        public String name = "";
        public String spec = "";
        
        public boolean isValid() {
            return !code.isEmpty();
        }
        
        @Override
        public String toString() {
            return String.format("ComponentInfo{code='%s', name='%s', spec='%s'}", code, name, spec);
        }
    }
    
    /**
     * Helper class to hold compatibility assessment results
     */
    public static class ComponentCompatibility {
        private boolean compatible = false;
        private double confidenceScore = 0.0;
        private String reason = "";
        
        public boolean isCompatible() { 
            return compatible; 
        }
        
        public void setCompatible(boolean compatible) { 
            this.compatible = compatible; 
        }
        
        public double getConfidenceScore() { 
            return confidenceScore; 
        }
        
        public void setConfidenceScore(double score) { 
            this.confidenceScore = Math.max(0.0, Math.min(1.0, score)); 
        }
        
        public String getReason() { 
            return reason; 
        }
        
        public void setReason(String reason) { 
            this.reason = reason != null ? reason : ""; 
        }
        
        @Override
        public String toString() {
            return String.format("ComponentCompatibility{compatible=%s, score=%.2f, reason='%s'}", 
                               compatible, confidenceScore, reason);
        }
    }
}
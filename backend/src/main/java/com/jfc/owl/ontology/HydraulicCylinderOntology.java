package com.jfc.owl.ontology;

import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.OWL;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Specialized ontology class for hydraulic cylinder domain knowledge.
 * This class creates and manages domain-specific classes, properties, and relationships
 * for hydraulic cylinder components and their BOM structures.
 */
/** 2025-06-20
 * Optimized singleton ontology class for hydraulic cylinder domain knowledge.
 * This class creates and manages domain-specific classes, properties, and relationships
 * for hydraulic cylinder components and their BOM structures.
 */
@Component
public class HydraulicCylinderOntology {
    private static final Logger logger = LoggerFactory.getLogger(HydraulicCylinderOntology.class);
    
    // Namespace constants
    private static final String BASE_URI = "http://www.jfc.com/tiptop/ontology#";
    private static final String HC_URI = "http://www.jfc.com/tiptop/hydraulic-cylinder#";
    
    private OntModel ontModel;
    private Map<String, OntClass> classes;
    private Map<String, OntProperty> properties;
    
    private volatile boolean initialized = false;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    /**
     * Initialize the ontology once when the bean is created
     */
    @PostConstruct
    public void init() {
        initializeHydraulicCylinderOntology();
    }
    
    /**
     * Initialize the hydraulic cylinder ontology with specialized classes and properties
     */
    public void initializeHydraulicCylinderOntology() {
    	if (initialized) {
            logger.debug("Hydraulic Cylinder Ontology already initialized");
            return;
        }
        
        lock.writeLock().lock();
    	try {
            // Double-check locking pattern
            if (initialized) {
                return;
            }
	        logger.info("Initializing Hydraulic Cylinder Ontology");
	        
	        // Create ontology model with OWL reasoning support
	        ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RULE_INF);
	        classes = new HashMap<>();
	        properties = new HashMap<>();
	        
	        // Create base ontology
	        Ontology ontology = ontModel.createOntology(HC_URI);
	        ontology.addComment("Hydraulic Cylinder Domain Ontology for BOM Generation", "en");
	        ontology.addVersionInfo("1.0");
	        
	        // Initialize domain classes
	        createDomainClasses();
	        
	        // Initialize domain properties
	        createDomainProperties();
	        
	        // Create class hierarchy
	        establishClassHierarchy();
	        
	        // Add property domains and ranges
	        establishPropertyDomainRanges();
	        
	        // Add specialized constraints and axioms
	        addDomainConstraints();
	        
	        logger.info("Hydraulic Cylinder Ontology initialization completed");
    	}finally {
    		lock.writeLock().unlock();
    	}
    }
    
    /**
     * Create domain-specific classes for hydraulic cylinders
     */
    private void createDomainClasses() {
        logger.debug("Creating domain classes");
        
        // Base Material classes (from existing ontology)
        classes.put("Material", ontModel.createClass(BASE_URI + "Material"));
        classes.put("MasterItem", ontModel.createClass(BASE_URI + "MasterItem"));
        classes.put("ComponentItem", ontModel.createClass(BASE_URI + "ComponentItem"));
        classes.put("BillOfMaterial", ontModel.createClass(BASE_URI + "BillOfMaterial"));
        
        // Hydraulic Cylinder specific classes
        classes.put("HydraulicCylinder", ontModel.createClass(HC_URI + "HydraulicCylinder"));
        
        // Cylinder types by series
        classes.put("StandardCylinder", ontModel.createClass(HC_URI + "StandardCylinder"));
        classes.put("HeavyDutyCylinder", ontModel.createClass(HC_URI + "HeavyDutyCylinder"));
        classes.put("CompactCylinder", ontModel.createClass(HC_URI + "CompactCylinder"));
        classes.put("LightDutyCylinder", ontModel.createClass(HC_URI + "LightDutyCylinder"));
        
        // Cylinder types by bore size
        classes.put("SmallBoreCylinder", ontModel.createClass(HC_URI + "SmallBoreCylinder"));
        classes.put("MediumBoreCylinder", ontModel.createClass(HC_URI + "MediumBoreCylinder"));
        classes.put("LargeBoreCylinder", ontModel.createClass(HC_URI + "LargeBoreCylinder"));
        
        // Cylinder types by stroke length
        classes.put("ShortStrokeCylinder", ontModel.createClass(HC_URI + "ShortStrokeCylinder"));
        classes.put("MediumStrokeCylinder", ontModel.createClass(HC_URI + "MediumStrokeCylinder"));
        classes.put("LongStrokeCylinder", ontModel.createClass(HC_URI + "LongStrokeCylinder"));
        
        // Rod end type classes
        classes.put("YokeRodEndCylinder", ontModel.createClass(HC_URI + "YokeRodEndCylinder"));
        classes.put("ThreadedRodEndCylinder", ontModel.createClass(HC_URI + "ThreadedRodEndCylinder"));
        classes.put("PinRodEndCylinder", ontModel.createClass(HC_URI + "PinRodEndCylinder"));
        
        // Installation type classes
        classes.put("FrontAttachmentCylinder", ontModel.createClass(HC_URI + "FrontAttachmentCylinder"));
        classes.put("RearAttachmentCylinder", ontModel.createClass(HC_URI + "RearAttachmentCylinder"));
        classes.put("TrunnionMountedCylinder", ontModel.createClass(HC_URI + "TrunnionMountedCylinder"));
        
        // Component classes
        classes.put("CylinderBarrel", ontModel.createClass(HC_URI + "CylinderBarrel"));
        classes.put("Piston", ontModel.createClass(HC_URI + "Piston"));
        classes.put("PistonRod", ontModel.createClass(HC_URI + "PistonRod"));
        classes.put("EndCap", ontModel.createClass(HC_URI + "EndCap"));
        classes.put("HeadEndCap", ontModel.createClass(HC_URI + "HeadEndCap"));
        classes.put("RodEndCap", ontModel.createClass(HC_URI + "RodEndCap"));
        
        // Sealing components
        classes.put("SealingComponent", ontModel.createClass(HC_URI + "SealingComponent"));
        classes.put("PistonSeal", ontModel.createClass(HC_URI + "PistonSeal"));
        classes.put("RodSeal", ontModel.createClass(HC_URI + "RodSeal"));
        classes.put("WiperSeal", ontModel.createClass(HC_URI + "WiperSeal"));
        classes.put("ORingSeal", ontModel.createClass(HC_URI + "ORingSeal"));
        
        // Other components
        classes.put("Bushing", ontModel.createClass(HC_URI + "Bushing"));
        classes.put("RodBushing", ontModel.createClass(HC_URI + "RodBushing"));
        classes.put("Gasket", ontModel.createClass(HC_URI + "Gasket"));
        classes.put("Fastener", ontModel.createClass(HC_URI + "Fastener"));
        classes.put("TieRod", ontModel.createClass(HC_URI + "TieRod"));
        
        // Performance and application classes
        classes.put("HighPressureCylinder", ontModel.createClass(HC_URI + "HighPressureCylinder"));
        classes.put("LowPressureCylinder", ontModel.createClass(HC_URI + "LowPressureCylinder"));
        classes.put("HighSpeedCylinder", ontModel.createClass(HC_URI + "HighSpeedCylinder"));
        classes.put("PrecisionCylinder", ontModel.createClass(HC_URI + "PrecisionCylinder"));
        
        // Material quality classes
        classes.put("StandardMaterial", ontModel.createClass(HC_URI + "StandardMaterial"));
        classes.put("CorrosionResistantMaterial", ontModel.createClass(HC_URI + "CorrosionResistantMaterial"));
        classes.put("HighStrengthMaterial", ontModel.createClass(HC_URI + "HighStrengthMaterial"));
    }
    
    /**
     * Create domain-specific properties for hydraulic cylinders
     */
    private void createDomainProperties() {
        logger.debug("Creating domain properties");
        
        // Basic item properties (from existing ontology)
        properties.put("itemCode", ontModel.createDatatypeProperty(BASE_URI + "itemCode"));
        properties.put("itemName", ontModel.createDatatypeProperty(BASE_URI + "itemName"));
        properties.put("itemSpec", ontModel.createDatatypeProperty(BASE_URI + "itemSpec"));
        
        // Hydraulic cylinder specific properties
        properties.put("bore", ontModel.createDatatypeProperty(HC_URI + "bore"));
        properties.put("stroke", ontModel.createDatatypeProperty(HC_URI + "stroke"));
        properties.put("series", ontModel.createDatatypeProperty(HC_URI + "series"));
        properties.put("cylinderType", ontModel.createDatatypeProperty(HC_URI + "cylinderType"));
        properties.put("rodEndType", ontModel.createDatatypeProperty(HC_URI + "rodEndType"));
        properties.put("installationType", ontModel.createDatatypeProperty(HC_URI + "installationType"));
        properties.put("shaftEndJoin", ontModel.createDatatypeProperty(HC_URI + "shaftEndJoin"));
        
        // Performance properties
        properties.put("maxPressure", ontModel.createDatatypeProperty(HC_URI + "maxPressure"));
        properties.put("maxSpeed", ontModel.createDatatypeProperty(HC_URI + "maxSpeed"));
        properties.put("operatingTemperature", ontModel.createDatatypeProperty(HC_URI + "operatingTemperature"));
        properties.put("cycleLife", ontModel.createDatatypeProperty(HC_URI + "cycleLife"));
        
        // Material properties
        properties.put("material", ontModel.createDatatypeProperty(HC_URI + "material"));
        properties.put("surfaceTreatment", ontModel.createDatatypeProperty(HC_URI + "surfaceTreatment"));
        properties.put("hardness", ontModel.createDatatypeProperty(HC_URI + "hardness"));
        
        // Dimensional properties
        properties.put("rodDiameter", ontModel.createDatatypeProperty(HC_URI + "rodDiameter"));
        properties.put("mountingDimension", ontModel.createDatatypeProperty(HC_URI + "mountingDimension"));
        properties.put("closedLength", ontModel.createDatatypeProperty(HC_URI + "closedLength"));
        properties.put("extendedLength", ontModel.createDatatypeProperty(HC_URI + "extendedLength"));
        
        // Component relationship properties
        properties.put("hasComponent", ontModel.createObjectProperty(HC_URI + "hasComponent"));
        properties.put("isComponentOf", ontModel.createObjectProperty(HC_URI + "isComponentOf"));
        properties.put("compatibleWith", ontModel.createObjectProperty(HC_URI + "compatibleWith"));
        properties.put("requiresComponent", ontModel.createObjectProperty(HC_URI + "requiresComponent"));
        properties.put("recommendedFor", ontModel.createObjectProperty(HC_URI + "recommendedFor"));
        
        // BOM relationship properties
        properties.put("hasMasterItem", ontModel.createObjectProperty(BASE_URI + "hasMasterItem"));
        properties.put("hasComponentItem", ontModel.createObjectProperty(BASE_URI + "hasComponentItem"));
        properties.put("quantity", ontModel.createDatatypeProperty(BASE_URI + "quantity"));
        
        // Quality and certification properties
        properties.put("qualityGrade", ontModel.createDatatypeProperty(HC_URI + "qualityGrade"));
        properties.put("certificationStandard", ontModel.createDatatypeProperty(HC_URI + "certificationStandard"));
        properties.put("testPressure", ontModel.createDatatypeProperty(HC_URI + "testPressure"));
        
        // Manufacturing properties
        properties.put("manufacturingProcess", ontModel.createDatatypeProperty(HC_URI + "manufacturingProcess"));
        properties.put("tolerance", ontModel.createDatatypeProperty(HC_URI + "tolerance"));
        properties.put("finishRequirement", ontModel.createDatatypeProperty(HC_URI + "finishRequirement"));
        
        // Application context properties
        properties.put("applicationArea", ontModel.createDatatypeProperty(HC_URI + "applicationArea"));
        properties.put("environmentalCondition", ontModel.createDatatypeProperty(HC_URI + "environmentalCondition"));
        properties.put("loadType", ontModel.createDatatypeProperty(HC_URI + "loadType"));
    }
    
    /**
     * Establish class hierarchy relationships
     */
    private void establishClassHierarchy() {
        logger.debug("Establishing class hierarchy");
        
        // Hydraulic Cylinder is a subclass of MasterItem and Material
        classes.get("HydraulicCylinder").addSuperClass(classes.get("MasterItem"));
        classes.get("HydraulicCylinder").addSuperClass(classes.get("Material"));
        
        // Series-based hierarchy
        classes.get("StandardCylinder").addSuperClass(classes.get("HydraulicCylinder"));
        classes.get("HeavyDutyCylinder").addSuperClass(classes.get("HydraulicCylinder"));
        classes.get("CompactCylinder").addSuperClass(classes.get("HydraulicCylinder"));
        classes.get("LightDutyCylinder").addSuperClass(classes.get("HydraulicCylinder"));
        
        // Bore size hierarchy
        classes.get("SmallBoreCylinder").addSuperClass(classes.get("HydraulicCylinder"));
        classes.get("MediumBoreCylinder").addSuperClass(classes.get("HydraulicCylinder"));
        classes.get("LargeBoreCylinder").addSuperClass(classes.get("HydraulicCylinder"));
        
        // Stroke length hierarchy
        classes.get("ShortStrokeCylinder").addSuperClass(classes.get("HydraulicCylinder"));
        classes.get("MediumStrokeCylinder").addSuperClass(classes.get("HydraulicCylinder"));
        classes.get("LongStrokeCylinder").addSuperClass(classes.get("HydraulicCylinder"));
        
        // Rod end type hierarchy
        classes.get("YokeRodEndCylinder").addSuperClass(classes.get("HydraulicCylinder"));
        classes.get("ThreadedRodEndCylinder").addSuperClass(classes.get("HydraulicCylinder"));
        classes.get("PinRodEndCylinder").addSuperClass(classes.get("HydraulicCylinder"));
        
        // Installation type hierarchy
        classes.get("FrontAttachmentCylinder").addSuperClass(classes.get("HydraulicCylinder"));
        classes.get("RearAttachmentCylinder").addSuperClass(classes.get("HydraulicCylinder"));
        classes.get("TrunnionMountedCylinder").addSuperClass(classes.get("HydraulicCylinder"));
        
        // Component hierarchy
        classes.get("CylinderBarrel").addSuperClass(classes.get("ComponentItem"));
        classes.get("Piston").addSuperClass(classes.get("ComponentItem"));
        classes.get("PistonRod").addSuperClass(classes.get("ComponentItem"));
        classes.get("EndCap").addSuperClass(classes.get("ComponentItem"));
        classes.get("HeadEndCap").addSuperClass(classes.get("EndCap"));
        classes.get("RodEndCap").addSuperClass(classes.get("EndCap"));
        
        // Sealing component hierarchy
        classes.get("SealingComponent").addSuperClass(classes.get("ComponentItem"));
        classes.get("PistonSeal").addSuperClass(classes.get("SealingComponent"));
        classes.get("RodSeal").addSuperClass(classes.get("SealingComponent"));
        classes.get("WiperSeal").addSuperClass(classes.get("SealingComponent"));
        classes.get("ORingSeal").addSuperClass(classes.get("SealingComponent"));
        
        // Other component hierarchy
        classes.get("Bushing").addSuperClass(classes.get("ComponentItem"));
        classes.get("RodBushing").addSuperClass(classes.get("Bushing"));
        classes.get("Gasket").addSuperClass(classes.get("ComponentItem"));
        classes.get("Fastener").addSuperClass(classes.get("ComponentItem"));
        classes.get("TieRod").addSuperClass(classes.get("Fastener"));
        
        // Performance hierarchy
        classes.get("HighPressureCylinder").addSuperClass(classes.get("HydraulicCylinder"));
        classes.get("LowPressureCylinder").addSuperClass(classes.get("HydraulicCylinder"));
        classes.get("HighSpeedCylinder").addSuperClass(classes.get("HydraulicCylinder"));
        classes.get("PrecisionCylinder").addSuperClass(classes.get("HydraulicCylinder"));
        
        // Material hierarchy
        classes.get("StandardMaterial").addSuperClass(classes.get("Material"));
        classes.get("CorrosionResistantMaterial").addSuperClass(classes.get("Material"));
        classes.get("HighStrengthMaterial").addSuperClass(classes.get("Material"));
    }
    
    /**
     * Establish property domains and ranges
     */
    private void establishPropertyDomainRanges() {
        logger.debug("Establishing property domains and ranges");
        
        // Hydraulic cylinder datatype properties
        properties.get("bore").addDomain(classes.get("HydraulicCylinder"));
        properties.get("bore").addRange(ontModel.getProfile().DATARANGE());
        
        properties.get("stroke").addDomain(classes.get("HydraulicCylinder"));
        properties.get("stroke").addRange(ontModel.getProfile().DATARANGE());
        
        properties.get("series").addDomain(classes.get("HydraulicCylinder"));
        properties.get("series").addRange(ontModel.getProfile().DATARANGE());
        
        properties.get("cylinderType").addDomain(classes.get("HydraulicCylinder"));
        properties.get("cylinderType").addRange(ontModel.getProfile().DATARANGE());
        
        properties.get("rodEndType").addDomain(classes.get("HydraulicCylinder"));
        properties.get("rodEndType").addRange(ontModel.getProfile().DATARANGE());
        
        // Performance properties
        properties.get("maxPressure").addDomain(classes.get("HydraulicCylinder"));
        properties.get("maxPressure").addRange(ontModel.getProfile().DATARANGE());
        
        properties.get("maxSpeed").addDomain(classes.get("HydraulicCylinder"));
        properties.get("maxSpeed").addRange(ontModel.getProfile().DATARANGE());
        
        // Object properties
        properties.get("hasComponent").addDomain(classes.get("HydraulicCylinder"));
        properties.get("hasComponent").addRange(classes.get("ComponentItem"));
        
        properties.get("isComponentOf").addDomain(classes.get("ComponentItem"));
        properties.get("isComponentOf").addRange(classes.get("HydraulicCylinder"));
        
        properties.get("compatibleWith").addDomain(classes.get("ComponentItem"));
        properties.get("compatibleWith").addRange(classes.get("HydraulicCylinder"));
        
        // Set inverse properties
        ObjectProperty hasComponent = (ObjectProperty) properties.get("hasComponent");
        ObjectProperty isComponentOf = (ObjectProperty) properties.get("isComponentOf");
        hasComponent.addInverseOf(isComponentOf);
        isComponentOf.addInverseOf(hasComponent);
    }
    
    /**
     * Add domain-specific constraints and axioms
     */
    private void addDomainConstraints() {
        logger.debug("Adding domain constraints and axioms");
        
        // Add cardinality constraints
        addCardinalityConstraints();
        
        // Add value restrictions
        addValueRestrictions();
        
        // Add disjoint classes
        addDisjointClasses();
        
        // Add equivalent classes
        addEquivalentClasses();
        
        // Add functional properties
        addFunctionalProperties();
    }
    
    
    /**
     * Add cardinality constraints for hydraulic cylinders using Jena 4.x
     */
    private void addCardinalityConstraints() {
        // 使用 OWL2 詞彙
        String OWL2 = "http://www.w3.org/2002/07/owl#";
        Property onClass = ontModel.createProperty(OWL2 + "onClass");
        
        // 一個液壓缸必須有恰好一個缸筒
        Restriction hasExactlyOneBarrel = ontModel.createCardinalityRestriction(
            null, properties.get("hasComponent"), 1);
        // 添加限定類型
        hasExactlyOneBarrel.addProperty(onClass, classes.get("CylinderBarrel"));
        classes.get("HydraulicCylinder").addSuperClass(hasExactlyOneBarrel);
        
        // 一個液壓缸必須有恰好一個活塞
        Restriction hasExactlyOnePiston = ontModel.createCardinalityRestriction(
            null, properties.get("hasComponent"), 1);
        hasExactlyOnePiston.addProperty(onClass, classes.get("Piston"));
        classes.get("HydraulicCylinder").addSuperClass(hasExactlyOnePiston);
        
        // 一個液壓缸必須有恰好一個活塞桿
        Restriction hasExactlyOnePistonRod = ontModel.createCardinalityRestriction(
            null, properties.get("hasComponent"), 1);
        hasExactlyOnePistonRod.addProperty(onClass, classes.get("PistonRod"));
        classes.get("HydraulicCylinder").addSuperClass(hasExactlyOnePistonRod);
        
        // 一個液壓缸必須有至少一個密封組件
        Restriction hasAtLeastOneSealing = ontModel.createMinCardinalityRestriction(
            null, properties.get("hasComponent"), 1);
        hasAtLeastOneSealing.addProperty(onClass, classes.get("SealingComponent"));
        classes.get("HydraulicCylinder").addSuperClass(hasAtLeastOneSealing);
        
        // 一個液壓缸通常有兩個端蓋
        Restriction hasTwoEndCaps = ontModel.createCardinalityRestriction(
            null, properties.get("hasComponent"), 2);
        hasTwoEndCaps.addProperty(onClass, classes.get("EndCap"));
        classes.get("HydraulicCylinder").addSuperClass(hasTwoEndCaps);
    }
    
    /**
     * Add value restrictions for hydraulic cylinders
     */
    private void addValueRestrictions() {
        // Small bore cylinders: bore <= 50mm
        Restriction smallBoreRestriction = ontModel.createAllValuesFromRestriction(
            null, properties.get("bore"), ontModel.getProfile().DATARANGE());
        classes.get("SmallBoreCylinder").addSuperClass(smallBoreRestriction);
        
        // Medium bore cylinders: 50mm < bore <= 100mm
        Restriction mediumBoreRestriction = ontModel.createAllValuesFromRestriction(
            null, properties.get("bore"), ontModel.getProfile().DATARANGE());
        classes.get("MediumBoreCylinder").addSuperClass(mediumBoreRestriction);
        
        // Large bore cylinders: bore > 100mm
        Restriction largeBoreRestriction = ontModel.createAllValuesFromRestriction(
            null, properties.get("bore"), ontModel.getProfile().DATARANGE());
        classes.get("LargeBoreCylinder").addSuperClass(largeBoreRestriction);
        
        // High pressure cylinders: maxPressure > 210 bar
        Restriction highPressureRestriction = ontModel.createAllValuesFromRestriction(
            null, properties.get("maxPressure"), ontModel.getProfile().DATARANGE());
        classes.get("HighPressureCylinder").addSuperClass(highPressureRestriction);
    }
    
    /**
     * Add disjoint classes declarations
     */
    private void addDisjointClasses() {
        // Bore size classes are mutually disjoint
        classes.get("SmallBoreCylinder").addDisjointWith(classes.get("MediumBoreCylinder"));
        classes.get("SmallBoreCylinder").addDisjointWith(classes.get("LargeBoreCylinder"));
        classes.get("MediumBoreCylinder").addDisjointWith(classes.get("LargeBoreCylinder"));
        
        // Rod end types are mutually disjoint
        classes.get("YokeRodEndCylinder").addDisjointWith(classes.get("ThreadedRodEndCylinder"));
        classes.get("YokeRodEndCylinder").addDisjointWith(classes.get("PinRodEndCylinder"));
        classes.get("ThreadedRodEndCylinder").addDisjointWith(classes.get("PinRodEndCylinder"));
        
        // Installation types are mutually disjoint
        classes.get("FrontAttachmentCylinder").addDisjointWith(classes.get("RearAttachmentCylinder"));
        classes.get("FrontAttachmentCylinder").addDisjointWith(classes.get("TrunnionMountedCylinder"));
        classes.get("RearAttachmentCylinder").addDisjointWith(classes.get("TrunnionMountedCylinder"));
    }
    
    /**
     * Add equivalent classes based on specifications
     */
    private void addEquivalentClasses() {
        // Create equivalent class for standard series (series = "10")
        Restriction standardSeriesRestriction = ontModel.createHasValueRestriction(
            null, properties.get("series"), ontModel.createTypedLiteral("10"));
        classes.get("StandardCylinder").addEquivalentClass(
            ontModel.createIntersectionClass(null, 
                ontModel.createList(new RDFNode[]{classes.get("HydraulicCylinder"), standardSeriesRestriction})));
        
        // Create equivalent class for heavy duty series (series = "11")
        Restriction heavyDutySeriesRestriction = ontModel.createHasValueRestriction(
            null, properties.get("series"), ontModel.createTypedLiteral("11"));
        classes.get("HeavyDutyCylinder").addEquivalentClass(
            ontModel.createIntersectionClass(null, 
                ontModel.createList(new RDFNode[]{classes.get("HydraulicCylinder"), heavyDutySeriesRestriction})));
        
        // Create equivalent class for yoke rod end (rodEndType = "Y")
        Restriction yokeRodEndRestriction = ontModel.createHasValueRestriction(
            null, properties.get("rodEndType"), ontModel.createTypedLiteral("Y"));
        classes.get("YokeRodEndCylinder").addEquivalentClass(
            ontModel.createIntersectionClass(null, 
                ontModel.createList(new RDFNode[]{classes.get("HydraulicCylinder"), yokeRodEndRestriction})));
    }
    
    /**
     * Add functional properties
     */
    private void addFunctionalProperties() {
        // Each cylinder has exactly one bore size
        ((DatatypeProperty) properties.get("bore")).addProperty(RDF.type, OWL.FunctionalProperty);
        
        // Each cylinder has exactly one stroke length
        ((DatatypeProperty) properties.get("stroke")).addProperty(RDF.type, OWL.FunctionalProperty);
        
        // Each cylinder has exactly one series
        ((DatatypeProperty) properties.get("series")).addProperty(RDF.type, OWL.FunctionalProperty);
        
        // Each cylinder has exactly one rod end type
        ((DatatypeProperty) properties.get("rodEndType")).addProperty(RDF.type, OWL.FunctionalProperty);
    }
    
    /**
     * Create a new hydraulic cylinder individual with specifications
     */
    public Individual createHydraulicCylinderIndividual(String itemCode, Map<String, String> specifications) {
        logger.debug("Creating hydraulic cylinder individual: {}", itemCode);
        
        // Create the individual
        Individual cylinder = classes.get("HydraulicCylinder").createIndividual(HC_URI + "Cylinder_" + itemCode);
        
        // Add basic properties
        cylinder.addProperty(properties.get("itemCode"), itemCode);
        
        // Add specifications
        if (specifications.containsKey("bore")) {
            cylinder.addProperty(properties.get("bore"), specifications.get("bore"));
        }
        if (specifications.containsKey("stroke")) {
            cylinder.addProperty(properties.get("stroke"), specifications.get("stroke"));
        }
        if (specifications.containsKey("series")) {
            cylinder.addProperty(properties.get("series"), specifications.get("series"));
        }
        if (specifications.containsKey("rodEndType")) {
            cylinder.addProperty(properties.get("rodEndType"), specifications.get("rodEndType"));
        }
        if (specifications.containsKey("installationType")) {
            cylinder.addProperty(properties.get("installationType"), specifications.get("installationType"));
        }
        
        // Classify the cylinder based on specifications
        classifyCylinderBySpecifications(cylinder, specifications);
        
        return cylinder;
    }
    
    /**
     * Classify cylinder based on its specifications
     */
    private void classifyCylinderBySpecifications(Individual cylinder, Map<String, String> specifications) {
        // Classify by bore size
        if (specifications.containsKey("bore")) {
            try {
                int bore = Integer.parseInt(specifications.get("bore"));
                if (bore <= 50) {
                    cylinder.addRDFType(classes.get("SmallBoreCylinder"));
                } else if (bore <= 100) {
                    cylinder.addRDFType(classes.get("MediumBoreCylinder"));
                } else {
                    cylinder.addRDFType(classes.get("LargeBoreCylinder"));
                }
            } catch (NumberFormatException e) {
                logger.warn("Invalid bore size format: {}", specifications.get("bore"));
            }
        }
        
        // Classify by stroke length
        if (specifications.containsKey("stroke")) {
            try {
                int stroke = Integer.parseInt(specifications.get("stroke"));
                if (stroke <= 100) {
                    cylinder.addRDFType(classes.get("ShortStrokeCylinder"));
                } else if (stroke <= 300) {
                    cylinder.addRDFType(classes.get("MediumStrokeCylinder"));
                } else {
                    cylinder.addRDFType(classes.get("LongStrokeCylinder"));
                }
            } catch (NumberFormatException e) {
                logger.warn("Invalid stroke length format: {}", specifications.get("stroke"));
            }
        }
        
        // Classify by series
        if (specifications.containsKey("series")) {
            String series = specifications.get("series");
            switch (series) {
                case "10":
                    cylinder.addRDFType(classes.get("StandardCylinder"));
                    break;
                case "11":
                    cylinder.addRDFType(classes.get("HeavyDutyCylinder"));
                    break;
                case "12":
                    cylinder.addRDFType(classes.get("CompactCylinder"));
                    break;
                case "13":
                    cylinder.addRDFType(classes.get("LightDutyCylinder"));
                    break;
            }
        }
        
        // Classify by rod end type
        if (specifications.containsKey("rodEndType")) {
            String rodEndType = specifications.get("rodEndType");
            switch (rodEndType) {
                case "Y":
                    cylinder.addRDFType(classes.get("YokeRodEndCylinder"));
                    break;
                case "I":
                case "E":
                    cylinder.addRDFType(classes.get("ThreadedRodEndCylinder"));
                    break;
                case "P":
                    cylinder.addRDFType(classes.get("PinRodEndCylinder"));
                    break;
            }
        }
        
        // Classify by installation type
        if (specifications.containsKey("installationType")) {
            String installationType = specifications.get("installationType");
            switch (installationType) {
                case "FA":
                    cylinder.addRDFType(classes.get("FrontAttachmentCylinder"));
                    break;
                case "RA":
                    cylinder.addRDFType(classes.get("RearAttachmentCylinder"));
                    break;
                case "TM":
                    cylinder.addRDFType(classes.get("TrunnionMountedCylinder"));
                    break;
            }
        }
    }
    
    /**
     * Generate compatible components for a hydraulic cylinder
     */
    public List<ComponentSuggestion> generateCompatibleComponents(Individual cylinder) {
        logger.debug("Generating compatible components for cylinder: {}", cylinder.getURI());
        
        List<ComponentSuggestion> suggestions = new ArrayList<>();
        
        // Get cylinder specifications
        Map<String, String> specs = extractCylinderSpecifications(cylinder);
        
        // Generate barrel suggestions
        suggestions.addAll(generateBarrelSuggestions(specs));
        
        // Generate piston suggestions
        suggestions.addAll(generatePistonSuggestions(specs));
        
        // Generate piston rod suggestions
        suggestions.addAll(generatePistonRodSuggestions(specs));
        
        // Generate sealing component suggestions
        suggestions.addAll(generateSealingSuggestions(specs));
        
        // Generate end cap suggestions
        suggestions.addAll(generateEndCapSuggestions(specs));
        
        // Generate bushing suggestions
        suggestions.addAll(generateBushingSuggestions(specs));
        
        // Generate fastener suggestions
        suggestions.addAll(generateFastenerSuggestions(specs));
        
        return suggestions;
    }
    
    /**
     * Extract specifications from a hydraulic cylinder individual
     */
    private Map<String, String> extractCylinderSpecifications(Individual cylinder) {
        Map<String, String> specs = new HashMap<>();
        
        // Extract bore
        if (cylinder.hasProperty(properties.get("bore"))) {
            specs.put("bore", cylinder.getPropertyValue(properties.get("bore")).toString());
        }
        
        // Extract stroke
        if (cylinder.hasProperty(properties.get("stroke"))) {
            specs.put("stroke", cylinder.getPropertyValue(properties.get("stroke")).toString());
        }
        
        // Extract series
        if (cylinder.hasProperty(properties.get("series"))) {
            specs.put("series", cylinder.getPropertyValue(properties.get("series")).toString());
        }
        
        // Extract rod end type
        if (cylinder.hasProperty(properties.get("rodEndType"))) {
            specs.put("rodEndType", cylinder.getPropertyValue(properties.get("rodEndType")).toString());
        }
        
        // Extract installation type
        if (cylinder.hasProperty(properties.get("installationType"))) {
            specs.put("installationType", cylinder.getPropertyValue(properties.get("installationType")).toString());
        }
        
        return specs;
    }
    
    /**
     * Generate barrel component suggestions
     */
    private List<ComponentSuggestion> generateBarrelSuggestions(Map<String, String> specs) {
        List<ComponentSuggestion> suggestions = new ArrayList<>();
        
        String bore = specs.get("bore");
        String series = specs.get("series");
        
        if (bore != null && series != null) {
            ComponentSuggestion barrel = new ComponentSuggestion();
            barrel.setCategory("CylinderBarrel");
            barrel.setCode(generateComponentCode("20", series, bore, "B01"));
            barrel.setName(String.format("Cylinder Barrel %smm - Series %s", bore, series));
            barrel.setDescription(String.format("Standard barrel for %smm bore, Series %s hydraulic cylinder", bore, series));
            barrel.setQuantity(1);
            barrel.setCompatibilityScore(1.0);
            suggestions.add(barrel);
            
            // Add alternative materials for corrosive environments
            if (Integer.parseInt(bore) > 80) {
                ComponentSuggestion corrosionResistantBarrel = new ComponentSuggestion();
                corrosionResistantBarrel.setCategory("CylinderBarrel");
                corrosionResistantBarrel.setCode(generateComponentCode("20", series, bore, "B02"));
                corrosionResistantBarrel.setName(String.format("Stainless Steel Barrel %smm - Series %s", bore, series));
                corrosionResistantBarrel.setDescription("Corrosion-resistant stainless steel barrel for harsh environments");
                corrosionResistantBarrel.setQuantity(1);
                corrosionResistantBarrel.setCompatibilityScore(0.9);
                suggestions.add(corrosionResistantBarrel);
            }
        }
        
        return suggestions;
    }
    
    /**
     * Generate piston component suggestions
     */
    private List<ComponentSuggestion> generatePistonSuggestions(Map<String, String> specs) {
        List<ComponentSuggestion> suggestions = new ArrayList<>();
        
        String bore = specs.get("bore");
        String series = specs.get("series");
        
        if (bore != null && series != null) {
            ComponentSuggestion piston = new ComponentSuggestion();
            piston.setCategory("Piston");
            piston.setCode(generateComponentCode("21", series, bore, "P01"));
            piston.setName(String.format("Piston %smm - Series %s", bore, series));
            piston.setDescription(String.format("Standard piston for %smm bore hydraulic cylinder", bore));
            piston.setQuantity(1);
            piston.setCompatibilityScore(1.0);
            suggestions.add(piston);
            
            // Add high-performance piston for heavy-duty series
            if ("11".equals(series)) {
                ComponentSuggestion heavyDutyPiston = new ComponentSuggestion();
                heavyDutyPiston.setCategory("Piston");
                heavyDutyPiston.setCode(generateComponentCode("21", series, bore, "P02"));
                heavyDutyPiston.setName(String.format("Heavy Duty Piston %smm - Series %s", bore, series));
                heavyDutyPiston.setDescription("Reinforced piston for high-pressure applications");
                heavyDutyPiston.setQuantity(1);
                heavyDutyPiston.setCompatibilityScore(0.95);
                suggestions.add(heavyDutyPiston);
            }
        }
        
        return suggestions;
    }
    
    /**
     * Generate piston rod component suggestions
     */
    private List<ComponentSuggestion> generatePistonRodSuggestions(Map<String, String> specs) {
        List<ComponentSuggestion> suggestions = new ArrayList<>();
        
        String bore = specs.get("bore");
        String series = specs.get("series");
        String rodEndType = specs.get("rodEndType");
        
        if (bore != null && series != null) {
            // Calculate standard rod diameter (typically 0.5-0.7 times bore)
            int boreSize = Integer.parseInt(bore);
            int rodDiameter = (int) (boreSize * 0.6);
            
            ComponentSuggestion pistonRod = new ComponentSuggestion();
            pistonRod.setCategory("PistonRod");
            pistonRod.setCode(generateComponentCode("21", series, String.format("%03d", rodDiameter), "R01"));
            pistonRod.setName(String.format("Piston Rod Ø%dmm for %smm Bore", rodDiameter, bore));
            pistonRod.setDescription(String.format("Standard piston rod with %s end connection", 
                getRodEndDescription(rodEndType)));
            pistonRod.setQuantity(1);
            pistonRod.setCompatibilityScore(1.0);
            suggestions.add(pistonRod);
            
            // Add chrome-plated version for better wear resistance
            ComponentSuggestion chromePlatedRod = new ComponentSuggestion();
            chromePlatedRod.setCategory("PistonRod");
            chromePlatedRod.setCode(generateComponentCode("21", series, String.format("%03d", rodDiameter), "R02"));
            chromePlatedRod.setName(String.format("Chrome-Plated Rod Ø%dmm for %smm Bore", rodDiameter, bore));
            chromePlatedRod.setDescription("Chrome-plated piston rod for extended service life");
            chromePlatedRod.setQuantity(1);
            chromePlatedRod.setCompatibilityScore(0.95);
            suggestions.add(chromePlatedRod);
        }
        
        return suggestions;
    }
    
    /**
     * Generate sealing component suggestions
     */
    private List<ComponentSuggestion> generateSealingSuggestions(Map<String, String> specs) {
        List<ComponentSuggestion> suggestions = new ArrayList<>();
        
        String bore = specs.get("bore");
        String series = specs.get("series");
        
        if (bore != null && series != null) {
            int boreSize = Integer.parseInt(bore);
            
            // Piston seal set
            ComponentSuggestion pistonSealSet = new ComponentSuggestion();
            pistonSealSet.setCategory("SealingComponent");
            pistonSealSet.setCode(generateComponentCode("25", series, bore, "S01"));
            pistonSealSet.setName(String.format("Piston Seal Set %smm", bore));
            pistonSealSet.setDescription("Complete piston seal set including primary and secondary seals");
            pistonSealSet.setQuantity(1);
            pistonSealSet.setCompatibilityScore(1.0);
            suggestions.add(pistonSealSet);
            
            // Rod seal
            ComponentSuggestion rodSeal = new ComponentSuggestion();
            rodSeal.setCategory("SealingComponent");
            rodSeal.setCode(generateComponentCode("25", series, bore, "S02"));
            rodSeal.setName(String.format("Rod Seal %smm", bore));
            rodSeal.setDescription("High-performance rod seal for dynamic sealing");
            rodSeal.setQuantity(1);
            rodSeal.setCompatibilityScore(1.0);
            suggestions.add(rodSeal);
            
            // Wiper seal
            ComponentSuggestion wiperSeal = new ComponentSuggestion();
            wiperSeal.setCategory("SealingComponent");
            wiperSeal.setCode(generateComponentCode("25", series, bore, "S03"));
            wiperSeal.setName(String.format("Wiper Seal %smm", bore));
            wiperSeal.setDescription("Wiper seal for contamination protection");
            wiperSeal.setQuantity(1);
            wiperSeal.setCompatibilityScore(1.0);
            suggestions.add(wiperSeal);
            
            // Additional seals for larger cylinders
            if (boreSize > 100) {
                ComponentSuggestion bufferSeal = new ComponentSuggestion();
                bufferSeal.setCategory("SealingComponent");
                bufferSeal.setCode(generateComponentCode("25", series, bore, "S04"));
                bufferSeal.setName(String.format("Buffer Seal %smm", bore));
                bufferSeal.setDescription("Buffer seal for improved sealing performance in large cylinders");
                bufferSeal.setQuantity(1);
                bufferSeal.setCompatibilityScore(0.8);
                suggestions.add(bufferSeal);
            }
        }
        
        return suggestions;
    }
    
    /**
     * Generate end cap component suggestions
     */
    private List<ComponentSuggestion> generateEndCapSuggestions(Map<String, String> specs) {
        List<ComponentSuggestion> suggestions = new ArrayList<>();
        
        String bore = specs.get("bore");
        String series = specs.get("series");
        String installationType = specs.get("installationType");
        
        if (bore != null && series != null) {
            // Head end cap
            ComponentSuggestion headEndCap = new ComponentSuggestion();
            headEndCap.setCategory("EndCap");
            headEndCap.setCode(generateComponentCode("22", series, bore, "C01"));
            headEndCap.setName(String.format("Head End Cap %smm - %s", bore, 
                getInstallationDescription(installationType)));
            headEndCap.setDescription("Head end cap with integrated mounting features");
            headEndCap.setQuantity(1);
            headEndCap.setCompatibilityScore(1.0);
            suggestions.add(headEndCap);
            
            // Rod end cap
            ComponentSuggestion rodEndCap = new ComponentSuggestion();
            rodEndCap.setCategory("EndCap");
            rodEndCap.setCode(generateComponentCode("22", series, bore, "C02"));
            rodEndCap.setName(String.format("Rod End Cap %smm", bore));
            rodEndCap.setDescription("Rod end cap with integrated rod seal housing");
            rodEndCap.setQuantity(1);
            rodEndCap.setCompatibilityScore(1.0);
            suggestions.add(rodEndCap);
        }
        
        return suggestions;
    }
    
    /**
     * Generate bushing component suggestions
     */
    private List<ComponentSuggestion> generateBushingSuggestions(Map<String, String> specs) {
        List<ComponentSuggestion> suggestions = new ArrayList<>();
        
        String bore = specs.get("bore");
        String series = specs.get("series");
        
        if (bore != null && series != null) {
            int boreSize = Integer.parseInt(bore);
            int rodDiameter = (int) (boreSize * 0.6);
            
            // Rod bushing
            ComponentSuggestion rodBushing = new ComponentSuggestion();
            rodBushing.setCategory("Bushing");
            rodBushing.setCode(generateComponentCode("26", series, String.format("%03d", rodDiameter), "B01"));
            rodBushing.setName(String.format("Rod Bushing Ø%dmm", rodDiameter));
            rodBushing.setDescription("Self-lubricating rod bushing for smooth operation");
            rodBushing.setQuantity(1);
            rodBushing.setCompatibilityScore(1.0);
            suggestions.add(rodBushing);
            
            // Additional bushing for larger cylinders
            if (boreSize > 80) {
                ComponentSuggestion guideBushing = new ComponentSuggestion();
                guideBushing.setCategory("Bushing");
                guideBushing.setCode(generateComponentCode("26", series, String.format("%03d", rodDiameter), "B02"));
                guideBushing.setName(String.format("Guide Bushing Ø%dmm", rodDiameter));
                guideBushing.setDescription("Additional guide bushing for improved rod guidance");
                guideBushing.setQuantity(1);
                guideBushing.setCompatibilityScore(0.8);
                suggestions.add(guideBushing);
            }
        }
        
        return suggestions;
    }
    
    /**
     * Generate fastener component suggestions
     */
    private List<ComponentSuggestion> generateFastenerSuggestions(Map<String, String> specs) {
        List<ComponentSuggestion> suggestions = new ArrayList<>();
        
        String bore = specs.get("bore");
        String series = specs.get("series");
        
        if (bore != null && series != null) {
            int boreSize = Integer.parseInt(bore);
            
            // Determine tie rod quantity based on bore size
            int tieRodQuantity = getTieRodQuantity(boreSize);
            
            ComponentSuggestion tieRods = new ComponentSuggestion();
            tieRods.setCategory("Fastener");
            tieRods.setCode(generateComponentCode("27", series, bore, "T01"));
            tieRods.setName(String.format("Tie Rod Set for %smm Cylinder", bore));
            tieRods.setDescription(String.format("Set of %d tie rods with nuts and washers", tieRodQuantity));
            tieRods.setQuantity(tieRodQuantity);
            tieRods.setCompatibilityScore(1.0);
            suggestions.add(tieRods);
            
            // End cap bolts
            ComponentSuggestion endCapBolts = new ComponentSuggestion();
            endCapBolts.setCategory("Fastener");
            endCapBolts.setCode(generateComponentCode("27", series, bore, "B01"));
            endCapBolts.setName(String.format("End Cap Bolt Set for %smm Cylinder", bore));
            endCapBolts.setDescription("High-strength bolts for end cap attachment");
            endCapBolts.setQuantity(1);
            endCapBolts.setCompatibilityScore(1.0);
            suggestions.add(endCapBolts);
        }
        
        return suggestions;
    }
    
    /**
     * Generate component code based on specifications
     */
    private String generateComponentCode(String prefix, String series, String bore, String suffix) {
        return String.format("%s%s%s-%s", prefix, series, bore, suffix);
    }
    
    /**
     * Get rod end type description
     */
    private String getRodEndDescription(String rodEndType) {
        if (rodEndType == null) return "standard";
        
        switch (rodEndType) {
            case "Y": return "yoke";
            case "I": return "internal thread";
            case "E": return "external thread";
            case "P": return "pin";
            default: return "standard";
        }
    }
    
    /**
     * Get installation type description
     */
    private String getInstallationDescription(String installationType) {
        if (installationType == null) return "Standard";
        
        switch (installationType) {
            case "FA": return "Front Attachment";
            case "RA": return "Rear Attachment";
            case "TM": return "Trunnion Mount";
            default: return "Standard";
        }
    }
    
    /**
     * Determine tie rod quantity based on bore size
     */
    private int getTieRodQuantity(int boreSize) {
        if (boreSize <= 50) return 4;
        else if (boreSize <= 100) return 6;
        else if (boreSize <= 150) return 8;
        else return 12;
    }
    
    /**
     * Validate hydraulic cylinder specifications
     */
    public ValidationResult validateHydraulicCylinderSpecs(Map<String, String> specifications) {
        ValidationResult result = new ValidationResult();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Validate bore size
        String bore = specifications.get("bore");
        if (bore == null || bore.isEmpty()) {
            errors.add("Bore size is required");
        } else {
            try {
                int boreValue = Integer.parseInt(bore);
                if (boreValue < 10 || boreValue > 500) {
                    warnings.add("Bore size " + boreValue + "mm is outside typical range (10-500mm)");
                }
            } catch (NumberFormatException e) {
                errors.add("Invalid bore size format: " + bore);
            }
        }
        
        // Validate stroke length
        String stroke = specifications.get("stroke");
        if (stroke == null || stroke.isEmpty()) {
            errors.add("Stroke length is required");
        } else {
            try {
                int strokeValue = Integer.parseInt(stroke);
                if (strokeValue < 10 || strokeValue > 10000) {
                    warnings.add("Stroke length " + strokeValue + "mm is outside typical range (10-10000mm)");
                }
            } catch (NumberFormatException e) {
                errors.add("Invalid stroke length format: " + stroke);
            }
        }
        
        // Validate series
        String series = specifications.get("series");
        if (series == null || series.isEmpty()) {
            errors.add("Series is required");
        } else if (!isValidSeries(series)) {
            warnings.add("Unknown series: " + series + ". Standard series are 10, 11, 12, 13");
        }
        
        // Validate rod end type
        String rodEndType = specifications.get("rodEndType");
        if (rodEndType != null && !rodEndType.isEmpty() && !isValidRodEndType(rodEndType)) {
            warnings.add("Unknown rod end type: " + rodEndType + ". Standard types are Y, I, E, P");
        }
        
        result.setValid(errors.isEmpty());
        result.setErrors(errors);
        result.setWarnings(warnings);
        
        return result;
    }
    
    /**
     * Check if series is valid
     */
    private boolean isValidSeries(String series) {
        return series.matches("^(10|11|12|13)$");
    }
    
    /**
     * Check if rod end type is valid
     */
    private boolean isValidRodEndType(String rodEndType) {
        return rodEndType.matches("^[YIEP]$");
    }
    
    /**
     * Get ontology model
     */
    public OntModel getOntologyModel() {
    	lock.readLock().lock();
        try {
            if (!initialized) {
                initializeHydraulicCylinderOntology();
            }
            return ontModel;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get domain classes
     */
    public Map<String, OntClass> getDomainClasses() {
    	lock.readLock().lock();
        try {
            return new HashMap<>(classes);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get domain properties
     */
    public Map<String, OntProperty> getDomainProperties() {
    	lock.readLock().lock();
        try {
            return new HashMap<>(properties);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Export ontology to file
     */
    public void exportOntology(String filePath, String format) {
        try {
            logger.info("Exporting hydraulic cylinder ontology to: {}", filePath);
            ontModel.write(new java.io.FileOutputStream(filePath), format);
            logger.info("Ontology exported successfully");
        } catch (Exception e) {
            logger.error("Error exporting ontology: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to export ontology", e);
        }
    }
    
    /**
     * Component suggestion class
     */
    public static class ComponentSuggestion {
        private String category;
        private String code;
        private String name;
        private String description;
        private int quantity;
        private double compatibilityScore;
        
        // Getters and setters
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        
        public double getCompatibilityScore() { return compatibilityScore; }
        public void setCompatibilityScore(double compatibilityScore) { this.compatibilityScore = compatibilityScore; }
    }
    
    /**
     * Validation result class
     */
    public static class ValidationResult {
        private boolean valid;
        private List<String> errors;
        private List<String> warnings;
        
        public ValidationResult() {
            this.errors = new ArrayList<>();
            this.warnings = new ArrayList<>();
        }
        
        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
        
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    }
}
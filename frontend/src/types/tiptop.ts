// src/types/tiptop.ts

/**
 * ApiResponse - Standard response format from backend
 */
export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
}

/**
 * ImaFile - Material/Item base information
 */
export interface ImaFile {
  ima01: string;  // Item code
  ima02?: string; // Item name
  ima021?: string; // Item specification
  ima09?: string; // Product type
  ima10?: string; // Product line
}

/**
 * BmaFile - BOM header information
 */
export interface BmaFile {
  id: {
    bma01: string; // Master item code
    bma06: string; // Characteristic code
  };
  bma02?: string;  // Last engineering change notice number
  bma03?: Date;    // Latest engineering change date
  bma04?: string;  // Combination mode reference number
  bma05?: Date;    // Release date
  bmaacti?: string; // Data valid code
}

/**
 * BmbFile - BOM detail/component information
 */
export interface BmbFile {
  id: {
    bmb01: string;   // Master item code
    bmb02: number;   // Component sequence
    bmb03: string;   // Component item code
    bmb04: Date;     // Effective date
    bmb29: string;   // Characteristic code
  };
  bmb05?: Date;      // Expiry date
  bmb06?: number;    // Quantity
  bmb08?: number;    // Loss rate
}

/**
 * BomComponent - Component with combined information for display
 */
export interface BomComponent {
  masterItemCode: string;
  componentItemCode: string;
  componentItemName?: string;
  componentItemSpec?: string;
  sequence: number;
  quantity?: number;
  effectiveDate?: string;
  expiryDate?: string;
  characteristicCode?: string;
  parentCharacteristicCode?: string;
}

/**
 * BomTreeNode - Hierarchical BOM structure
 */
export interface BomTreeNode {
  itemCode: string;
  itemName?: string;
  itemSpec?: string;
  quantity?: number;
  effectiveDate?: Date;
  expiryDate?: Date;
  characteristicCode?: string;
  parentCharacteristicCode?: string;
  children?: BomTreeNode[];
}

/**
 * BomStats - Statistics about BOM data
 */
export interface BomStats {
  totalItems: number;
  masterItemsCount: number;
  componentItemsCount: number;
  bomRelationshipsCount: number;
}

/**
 * OntologyExportFormat - Available formats for ontology export
 */
export enum OntologyExportFormat {
  RDF_XML = 'RDF/XML',
  TURTLE = 'TURTLE',
  JSON_LD = 'JSON-LD',
  N_TRIPLES = 'N-TRIPLES'
}

// -------- Reasoning-related types --------

/**
 * Class in an OWL ontology
 */
export interface OntologyClass {
  iri: string;
  name: string;
  description?: string;
  superClasses: string[];
  subClasses: string[];
  individuals: string[];
  defined: boolean;
  primitive: boolean;
  namespace: string;
  restrictions: string[];
}

/**
 * Property in an OWL ontology
 */
export interface OntologyProperty {
  iri: string;
  name: string;
  description?: string;
  type: 'object' | 'data' | 'annotation';
  domain: string[];
  range: string[];
  superProperties: string[];
  subProperties: string[];
  namespace: string;
  functional: boolean;
  inverseFunctional?: boolean;
  transitive?: boolean;
  symmetric?: boolean;
  asymmetric?: boolean;
  reflexive?: boolean;
  irreflexive?: boolean;
  inverseProperty?: string;
}

/**
 * Rule in an OWL ontology
 */
export interface OntologyRule {
  id: string;
  name: string;
  description?: string;
  ruleBody: string;
  priority?: number;
  enabled: boolean;
  tags?: string[];
  predefined: boolean;
  createdDate?: string;
  createdBy?: string;
  lastModifiedDate?: string;
  lastModifiedBy?: string;
}

/**
 * Validation issue in reasoning
 */
export interface ValidationIssue {
  type: string;
  description: string;
  severity?: string;
  extension?: string;
}

/**
 * Inferred statement from reasoning
 */
export interface InferredStatement {
  subject: string;
  predicate: string;
  object: string;
  category?: string;
}

/**
 * Class relationship from reasoning
 
export interface ClassRelationship {
  subclass: string;
  superclass: string;
}*/

/**
 * Component item in BOM hierarchy with inferences
 
export interface InferredComponentItem {
  code: string;
  uri: string;
  name?: string;
  spec?: string;
  quantity?: string;
  effectiveDate?: string;
  expiryDate?: string;
  inferredProperties?: Record<string, string[]>;
}*/

/**
 * BOM hierarchy with inferred information
 
export interface InferredBomHierarchy {
  code: string;
  uri: string;
  inferredProperties: Record<string, string[]>;
  components: InferredComponentItem[];
}*/

/**
 * Inferred subclass relationship
 */
export interface InferredSubclass {
  subclass: string;
  superclass: string;
  confidence?: string;
}

/**
 * Component item in BOM hierarchy
 */
export interface BomHierarchyComponent {
  uri: string;
  bomUri?: string;
  code: string;
  itemName?: string;
  itemSpec?: string;
  itemCode?: string;
  quantity?: string;
  effectiveDate?: string;
  expiryDate?: string;
  compatibilityInfo?: string[];
}

/**
 * BOM hierarchy with enhanced information
 */
export interface BomHierarchy {
  code: string;
  uri: string;
  isHydraulicCylinder: boolean;
  enhancedProperties?: Record<string, string[]>;
  components?: BomHierarchyComponent[];
  hydraulicCylinderHierarchy?: {
    classifications?: string[];
    componentCategories?: Record<string, string[]>;
    performanceCharacteristics?: Record<string, string>;
  };
}

/**
 * Hydraulic cylinder component suggestion
 */
export interface ComponentSuggestion {
  code: string;
  name: string;
  spec?: string;
  confidenceScore: number;
  compatibilityReason: string;
  reasoningExplanation?: string[];
}

/**
 * Similar cylinder information
 */
export interface SimilarCylinder {
  code: string;
  name?: string;
  similarityScore: number;
  classifications?: string[];
  performanceCharacteristics?: Record<string, string>;
  componentCount?: number;
  similarityReasons?: string[];
}

/**
 * Hydraulic cylinder inferences
 */
export interface HydraulicCylinderInferences {
  componentSuggestions?: Record<string, ComponentSuggestion[]>;
  similarCylinders?: SimilarCylinder[];
  specifications?: Record<string, string>;
}

/**
 * Reasoning performance metrics
 */
export interface ReasoningMetrics {
  originalStatements: number;
  inferredStatements: number;
  totalStatements: number;
  inferenceRatio: number;
  reasoningCompleteness?: number;
}

/**
 * Complete reasoning result
 */
export interface ReasoningResult {
  // Core fields
  isValid: boolean | string; // Can be boolean or "skipped"
  validationIssues: ValidationIssue[];
  inferredStatements: InferredStatement[];
  inferredSubclasses: InferredSubclass[];
  bomHierarchy?: BomHierarchy;

  // Enhanced fields
  modelSize?: number;
  reasonerUsed?: string;
  reasoningTimeout?: boolean;
  reasoningError?: string;
  validationNote?: string;
  inferredCount?: number;
  warnings?: string[];

  // Hydraulic cylinder specific
  hydraulicCylinderInferences?: HydraulicCylinderInferences;

  // Performance metrics
  reasoningMetrics?: ReasoningMetrics;

  // Error handling
  error?: string;
  errorType?: string;
  stackTrace?: string;
}

/**
 * SPARQL query result
 */
export interface SparqlQueryResult {
  type: 'SELECT' | 'CONSTRUCT' | 'ASK' | 'DESCRIBE';
  variables?: string[];
  results?: Record<string, string>[];
  resultCount?: number;
  model?: string;
  statementCount?: number;
  result?: boolean;
  queryAnalysis?: {
    isHydraulicCylinderQuery?: boolean;
    queryLength?: number;
    hasFilter?: boolean;
    hasOptional?: boolean;
    hasUnion?: boolean;
    hasGroupBy?: boolean;
    hasOrderBy?: boolean;
    usesHydraulicProperties?: boolean;
    usesCompatibilityProperties?: boolean;
  };
  error?: string;
  stackTrace?: string[];
}

/**
 * Custom rule application result
 */
export interface CustomRuleResult {
  appliedRules: number;
  inferredStatements: InferredStatement[];
  isHydraulicCylinder?: boolean;
  ruleAnalysis?: {
    totalRules?: number;
    complexRules?: number;
    ruleComplexity?: string;
    hydraulicCylinderRules?: number;
    domainSpecific?: boolean;
    usesRegex?: boolean;
    usesComparisons?: boolean;
    usesCompatibility?: boolean;
  };
  error?: string;
  stackTrace?: string[];
}

/**
 * Information about available reasoners
 */
export interface ReasonerInfo {
  id: string;
  name: string;
  description: string;
  suitability?: string;
  performance?: 'fast' | 'moderate' | 'slow';
}

/**
 * Predefined SPARQL query
 */
export interface PredefinedQuery {
  name: string;
  description: string;
  category?: string;
  query: string;
}

/**
 * Example rule for the ontology
 */
export interface ExampleRule {
  name: string;
  description: string;
    category?: string;
  rule: string;
}
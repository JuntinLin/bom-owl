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
  }
  
  /**
   * Inferred statement from reasoning
   */
  export interface InferredStatement {
    subject: string;
    predicate: string;
    object: string;
  }
  
  /**
   * Class relationship from reasoning
   */
  export interface ClassRelationship {
    subclass: string;
    superclass: string;
  }
  
  /**
   * Component item in BOM hierarchy with inferences
   */
  export interface InferredComponentItem {
    code: string;
    uri: string;
    name?: string;
    spec?: string;
    quantity?: string;
    effectiveDate?: string;
    expiryDate?: string;
    inferredProperties?: Record<string, string[]>;
  }
  
  /**
   * BOM hierarchy with inferred information
   */
  export interface InferredBomHierarchy {
    code: string;
    uri: string;
    inferredProperties: Record<string, string[]>;
    components: InferredComponentItem[];
  }
  
  /**
   * Complete reasoning result
   */
  export interface ReasoningResult {
    masterItemCode: string;
    reasonerType: string;
    valid: boolean;
    validationIssues: ValidationIssue[];
    inferredStatements: InferredStatement[];
    inferredSubclasses: ClassRelationship[];
    bomHierarchy: InferredBomHierarchy;
    errorMessage?: string;
    processingTimeMs: number;
  }
  
  /**
   * SPARQL query result
   */
  export interface SparqlQueryResult {
    type: 'SELECT' | 'CONSTRUCT' | 'ASK' | 'DESCRIBE';
    variables?: string[];
    results?: Record<string, string>[];
    model?: string;
    result?: boolean;
    error?: string;
  }
  
  /**
   * Custom rule application result
   */
  export interface CustomRuleResult {
    appliedRules: number;
    inferredStatements: InferredStatement[];
    error?: string;
  }
  
  /**
   * Information about available reasoners
   */
  export interface ReasonerInfo {
    id: string;
    name: string;
    description: string;
  }
  
  /**
   * Predefined SPARQL query
   */
  export interface PredefinedQuery {
    name: string;
    description: string;
    query: string;
  }
  
  /**
   * Example rule for the ontology
   */
  export interface ExampleRule {
    name: string;
    description: string;
    rule: string;
  }
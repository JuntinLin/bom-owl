# Tiptop ERP to OWL System - Reasoning Module

This document describes the semantic reasoning capabilities integrated into the Tiptop ERP to OWL system. The reasoning module allows users to apply OWL reasoning, execute SPARQL queries, and define custom rules to derive new information from BOM data.

## Architecture Overview

The reasoning system is built with:

- **Backend**: Spring Boot with Apache Jena for OWL reasoning
- **Frontend**: React with TypeScript and Shadcn UI components
- **Data Flow**: 
  1. ERP data → OWL Ontology
  2. OWL Ontology → Reasoning Engine
  3. Reasoning Results → User Interface

## Key Components

### Backend Components

1. **ReasoningService.java**
   - Applies various reasoners (OWL, RDFS) to ontologies
   - Executes SPARQL queries against ontologies
   - Processes custom rules with the Jena rule engine
   - Extracts and formats inference results

2. **ReasoningController.java**
   - REST API endpoints for reasoning operations
   - Error handling and response formatting
   - Authentication and validation

3. **Model Classes**
   - `ReasoningResult.java`: Structures for storing reasoning results
   - `OntologyClass.java`: Representation of classes in the ontology
   - `OntologyProperty.java`: Representation of properties in the ontology
   - `OntologyRule.java`: Representation of rules for inference

### Frontend Components

1. **Reasoning Service (reasoningService.ts)**
   - API client for reasoning operations
   - Error handling and response processing
   - Type definitions for reasoning results

2. **Reasoning Dashboard (ReasoningDashboard.tsx)**
   - Main interface for reasoning operations
   - Selection of reasoners and BOM items
   - Navigation between reasoning tabs

3. **Specialized Components**
   - `ReasonerResults.tsx`: Display of reasoning results
   - `SparqlQueryPanel.tsx`: Interface for SPARQL queries
   - `CustomRulesPanel.tsx`: Interface for custom rules
   - `BomHierarchyViz.tsx`: Visualization of BOM with inferences

## Reasoning Capabilities

### 1. OWL Reasoning

The system supports multiple OWL reasoners:

- **OWL Reasoner**: Full OWL reasoning with class hierarchy, property inference
- **OWL Mini**: Lighter reasoner with limited OWL features
- **OWL Micro**: Minimal OWL reasoner for basic inferences
- **RDFS**: Simple RDFS reasoner for class/property hierarchies

Each reasoner validates the ontology and derives new statements based on OWL semantics.

### 2. SPARQL Querying

Users can execute SPARQL queries against the ontology:

- **Predefined Queries**: Common queries for BOM analysis
- **Custom Queries**: User-defined SPARQL queries
- **Result Visualization**: Tables, graphs, and exports

The system supports all SPARQL query types (SELECT, CONSTRUCT, ASK, DESCRIBE).

### 3. Custom Rules

Users can define and apply custom rules in Jena rule syntax:

- **Predefined Rules**: Common rules for BOM analysis
- **Custom Rule Creation**: Interface for defining new rules
- **Rule Application**: Apply rules to derive new facts

## Data Flow

1. User selects a master item to analyze
2. The system loads the corresponding OWL ontology
3. User selects a reasoning operation (reasoner, SPARQL, rules)
4. The backend processes the request and performs reasoning
5. Results are sent to the frontend and displayed in the UI
6. User can explore inferences, query results, or rule applications

## Best Practices for Using the Reasoning System

1. **Start with Standard Reasoners**:
   - Begin with the OWL or RDFS reasoners to understand the ontology

2. **Use Predefined Queries and Rules**:
   - Leverage the provided templates before creating custom ones

3. **Optimize Complex Queries**:
   - For large BOMs, limit results or use more specific constraints

4. **Custom Rule Development**:
   - Test rules on smaller ontologies before applying to large ones
   - Build rules incrementally, testing at each step

## Implementation Notes

### Backend Implementation

The reasoning system is implemented using Apache Jena's reasoning frameworks:

```java
// Example of creating a reasoner and inference model
Reasoner reasoner = ReasonerRegistry.getOWLReasoner();
InfModel infModel = ModelFactory.createInfModel(reasoner, ontModel);
```

SPARQL queries are processed using Jena's query execution:

```java
Query query = QueryFactory.create(queryString);
try (QueryExecution qexec = QueryExecutionFactory.create(query, infModel)) {
    ResultSet results = qexec.execSelect();
    // Process results
}
```

Custom rules use Jena's rule engine:

```java
List<Rule> ruleList = Rule.parseRules(rulesString);
Reasoner ruleReasoner = new GenericRuleReasoner(ruleList);
InfModel ruleInfModel = ModelFactory.createInfModel(ruleReasoner, ontModel);
```

### Frontend Implementation

The frontend uses React components and custom hooks:

```typescript
// Example of using the reasoning service
const [result, setResult] = useState<ReasoningResult | null>(null);

const performReasoning = async () => {
  try {
    const result = await reasoningService.performReasoning(masterItemCode, reasonerType);
    setResult(result);
  } catch (error) {
    // Handle error
  }
};
```

## Extension Points

The reasoning system can be extended in several ways:

1. **Additional Reasoners**:
   - Add specialized reasoners for specific domains
   - Integrate external reasoning engines

2. **Custom Visualization**:
   - Enhance the visualization of reasoning results
   - Add interactive graphs for exploring inferences

3. **Rule Libraries**:
   - Develop domain-specific rule libraries
   - Create a rule sharing mechanism

4. **Advanced SPARQL**:
   - Support for SPARQL federated queries
   - Add SPARQL result visualizations

## Troubleshooting

Common issues and solutions:

1. **Performance Issues with Large BOMs**:
   - Use the OWL Micro reasoner for better performance
   - Apply reasoning to specific portions of the ontology

2. **Custom Rule Errors**:
   - Check rule syntax against Jena's documentation
   - Test rules incrementally with simplified examples

3. **SPARQL Query Errors**:
   - Verify ontology namespace prefixes
   - Check for typos in property and class URIs

4. **No Inferences Generated**:
   - Confirm that the ontology has logical constructs that enable inference
   - Try different reasoners as they have different capabilities
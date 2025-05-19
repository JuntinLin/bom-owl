package com.jfc.owl.service;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    @Autowired
    private BomOwlExportService bomOwlExportService;

    @Value("${owl.ontology.base-path}")
    private String ontologyBasePath;

    /**
     * Performs OWL reasoning on the BOM ontology for a specific master item.
     * 
     * @param masterItemCode The code of the master item to reason about
     * @param reasonerType   The type of reasoner to use (e.g., "OWL_MEM_MICRO_RULE_INF", "OWL_DL_MEM_RULE_INF")
     * @return A ReasoningResult object containing the inference results
     */
    public Map<String, Object> performReasoning(String masterItemCode, String reasonerType) {
        Map<String, Object> results = new HashMap<>();
        
        try {
            // Get the ontology model for the master item
            OntModel ontModel = bomOwlExportService.getBomOntologyForMasterItem(masterItemCode);
            
            // Create a reasoner based on the specified type
            Reasoner reasoner;
            switch (reasonerType) {
                case "OWL_MINI":
                    reasoner = ReasonerRegistry.getOWLMiniReasoner();
                    break;
                case "OWL_MICRO":
                    reasoner = ReasonerRegistry.getOWLMicroReasoner();
                    break;
                case "RDFS":
                    reasoner = ReasonerRegistry.getRDFSReasoner();
                    break;
                case "OWL":
                default:
                    reasoner = ReasonerRegistry.getOWLReasoner();
                    break;
            }
            
            // Bind the reasoner to the ontology model
            InfModel infModel = ModelFactory.createInfModel(reasoner, ontModel);
            
            // Validate the model
            ValidityReport validity = infModel.validate();
            boolean isValid = validity.isValid();
            results.put("isValid", isValid);
            
            List<Map<String, String>> validationIssues = new ArrayList<>();
            if (!isValid) {
                Iterator<ValidityReport.Report> reports = validity.getReports();
                while (reports.hasNext()) {
                    ValidityReport.Report report = reports.next();
                    Map<String, String> issue = new HashMap<>();
                    issue.put("type", report.getType());
                    issue.put("description", report.getDescription());
                    validationIssues.add(issue);
                }
            }
            results.put("validationIssues", validationIssues);
            
            // Extract inferred statements
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
                    inferredStatements.add(statementMap);
                }
            }
            results.put("inferredStatements", inferredStatements);
            
            // Extract inferred subclass relationships
            List<Map<String, String>> inferredSubclasses = new ArrayList<>();
            /*
            ontModel.listClasses().forEachRemaining(cls -> {
                // For each class in the model, find inferred subclasses
                infModel.listSubClasses(cls, true).forEachRemaining(subClass -> {
                    if (!subClass.equals(cls) && !ontModel.containsResource(RDFS.subClassOf, cls, subClass)) {
                        Map<String, String> subclassMap = new HashMap<>();
                        subclassMap.put("subclass", subClass.toString());
                        subclassMap.put("superclass", cls.toString());
                        inferredSubclasses.add(subclassMap);
                    }
                });
            });*/
         // 直接使用 listStatements 查找所有 rdfs:subClassOf 語句
            StmtIterator subClassStmts = infModel.listStatements(null, RDFS.subClassOf, (org.apache.jena.rdf.model.RDFNode)null);
            while (subClassStmts.hasNext()) {
                Statement stmt = subClassStmts.next();
                
                // 檢查此子類關係是否為推理得出的（不存在於原始模型中）
                if (!ontModel.contains(stmt)) {
                    Map<String, String> subclassMap = new HashMap<>();
                    subclassMap.put("subclass", stmt.getSubject().toString());
                    subclassMap.put("superclass", stmt.getObject().toString());
                    inferredSubclasses.add(subclassMap);
                }
            }
            
            results.put("inferredSubclasses", inferredSubclasses);
            
            // Extract component hierarchy for the master item
            results.put("bomHierarchy", extractBomHierarchy(masterItemCode, infModel));
            
            // Add any additional reasoning results as needed
            
            return results;
            
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            errorResult.put("stackTrace", Arrays.toString(e.getStackTrace()));
            return errorResult;
        }
    }
    
    /**
     * Performs SPARQL queries on the ontology.
     * 
     * @param masterItemCode The code of the master item
     * @param queryString    The SPARQL query to execute
     * @return The query results as a JSON object
     */
    public Map<String, Object> executeSparqlQuery(String masterItemCode, String queryString) {
        Map<String, Object> results = new HashMap<>();
        
        try {
            // Get the ontology model for the master item
            OntModel ontModel = bomOwlExportService.getBomOntologyForMasterItem(masterItemCode);
            
            // Create a reasoner and bind it to the model to allow for inferred results
            Reasoner reasoner = ReasonerRegistry.getOWLReasoner();
            InfModel infModel = ModelFactory.createInfModel(reasoner, ontModel);
            
            // Create and execute the SPARQL query
            Query query = QueryFactory.create(queryString);
            try (QueryExecution qexec = QueryExecutionFactory.create(query, infModel)) {
                
                // Handle different query types
                if (query.isSelectType()) {
                    ResultSet rs = qexec.execSelect();
                    
                    // Convert the result set to a list of maps
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
                    
                } else if (query.isConstructType()) {
                    org.apache.jena.rdf.model.Model constructModel = qexec.execConstruct();
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    constructModel.write(outputStream, "TURTLE");
                    
                    results.put("type", "CONSTRUCT");
                    results.put("model", outputStream.toString());
                    
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
                }
            }
            
            return results;
            
        } catch (Exception e) {
            results.put("error", e.getMessage());
            results.put("stackTrace", Arrays.toString(e.getStackTrace()));
            return results;
        }
    }
    
    /**
     * Applies custom rules to the ontology using the rule-based reasoner.
     * 
     * @param masterItemCode The code of the master item
     * @param rules          The rules in Jena rule syntax
     * @return The reasoning results after applying the rules
     */
    public Map<String, Object> applyCustomRules(String masterItemCode, String rules) {
        Map<String, Object> results = new HashMap<>();
        
        try {
            // Get the ontology model for the master item
            OntModel ontModel = bomOwlExportService.getBomOntologyForMasterItem(masterItemCode);
            
            // Create a rule-based reasoner
            List<Rule> ruleList = Rule.parseRules(rules);
            Reasoner ruleReasoner = new GenericRuleReasoner(ruleList);
            
            // Bind the reasoner to the model
            InfModel infModel = ModelFactory.createInfModel(ruleReasoner, ontModel);
            
            // Extract inferred statements
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
                    inferredStatements.add(statementMap);
                }
            }
            
            results.put("appliedRules", ruleList.size());
            results.put("inferredStatements", inferredStatements);
            
            return results;
            
        } catch (Exception e) {
            results.put("error", e.getMessage());
            results.put("stackTrace", Arrays.toString(e.getStackTrace()));
            return results;
        }
    }
    
    /**
     * Extracts the BOM hierarchy with reasoned information.
     * 
     * @param masterItemCode The code of the master item
     * @param infModel       The inference model to extract from
     * @return A hierarchical structure of the BOM with inferred information
     */
    private Map<String, Object> extractBomHierarchy(String masterItemCode, InfModel infModel) {
        Map<String, Object> hierarchy = new HashMap<>();
        
        // Get the master item URI
        String masterItemUri = "http://www.jfc.com/tiptop/ontology#Material_" + masterItemCode;
        
        // Extract master item details
        hierarchy.put("code", masterItemCode);
        hierarchy.put("uri", masterItemUri);
        
        // Find additional inferred properties for the master item
        Map<String, List<String>> inferredProperties = new HashMap<>();
        StmtIterator properties = infModel.listStatements(infModel.getResource(masterItemUri), null, (org.apache.jena.rdf.model.RDFNode) null);
        while (properties.hasNext()) {
            Statement stmt = properties.next();
            String predicate = stmt.getPredicate().getLocalName();
            
            inferredProperties.computeIfAbsent(predicate, k -> new ArrayList<>())
                              .add(stmt.getObject().toString());
        }
        hierarchy.put("inferredProperties", inferredProperties);
        
        // Get component items - focus on the hasMasterItem and hasComponentItem properties
        List<Map<String, Object>> components = new ArrayList<>();
        String bomPrefix = "http://www.jfc.com/tiptop/ontology#BOM_" + masterItemCode + "_";
        
        // Find BOM entries for this master item
        StmtIterator boms = infModel.listStatements(null, 
                                                   infModel.getProperty("http://www.jfc.com/tiptop/ontology#hasMasterItem"), 
                                                   infModel.getResource(masterItemUri));
        
        while (boms.hasNext()) {
            Statement bomStmt = boms.next();
            String bomUri = bomStmt.getSubject().getURI();
            
            // For each BOM entry, find the component items
            StmtIterator componentItems = infModel.listStatements(infModel.getResource(bomUri),
                                                                 infModel.getProperty("http://www.jfc.com/tiptop/ontology#hasComponentItem"),
                                                                 (org.apache.jena.rdf.model.RDFNode) null);
            
            while (componentItems.hasNext()) {
                Statement componentStmt = componentItems.next();
                String componentUri = componentStmt.getObject().asResource().getURI();
                
                // Extract component details
                Map<String, Object> component = new HashMap<>();
                component.put("uri", componentUri);
                
                // Extract component code from URI (assuming pattern Material_CODE)
                String componentCode = componentUri.substring(componentUri.lastIndexOf("_") + 1);
                component.put("code", componentCode);
                
                // Get item name and spec if available
                StmtIterator nameProps = infModel.listStatements(infModel.getResource(componentUri),
                                                               infModel.getProperty("http://www.jfc.com/tiptop/ontology#itemName"),
                                                               (org.apache.jena.rdf.model.RDFNode) null);
                if (nameProps.hasNext()) {
                    component.put("name", nameProps.next().getObject().toString());
                }
                
                StmtIterator specProps = infModel.listStatements(infModel.getResource(componentUri),
                                                               infModel.getProperty("http://www.jfc.com/tiptop/ontology#itemSpec"),
                                                               (org.apache.jena.rdf.model.RDFNode) null);
                if (specProps.hasNext()) {
                    component.put("spec", specProps.next().getObject().toString());
                }
                
                // Get quantity from BOM
                StmtIterator quantityProps = infModel.listStatements(infModel.getResource(bomUri),
                                                                   infModel.getProperty("http://www.jfc.com/tiptop/ontology#quantity"),
                                                                   (org.apache.jena.rdf.model.RDFNode) null);
                if (quantityProps.hasNext()) {
                    component.put("quantity", quantityProps.next().getObject().toString());
                }
                
                // Also get effective date if available
                StmtIterator dateProps = infModel.listStatements(infModel.getResource(bomUri),
                                                               infModel.getProperty("http://www.jfc.com/tiptop/ontology#effectiveDate"),
                                                               (org.apache.jena.rdf.model.RDFNode) null);
                if (dateProps.hasNext()) {
                    component.put("effectiveDate", dateProps.next().getObject().toString());
                }
                
                // Add component to the list
                components.add(component);
            }
        }
        
        hierarchy.put("components", components);
        return hierarchy;
    }
    
    /**
     * Gets all the predefined SPARQL queries available in the system.
     * 
     * @return A list of predefined queries with their names and query text
     */
    public List<Map<String, String>> getPredefinedQueries() {
        List<Map<String, String>> queries = new ArrayList<>();
        
        // Add some example predefined queries
        Map<String, String> query1 = new HashMap<>();
        query1.put("name", "List All Components");
        query1.put("description", "Lists all component items used in any BOM");
        query1.put("query", "PREFIX j.0: <http://www.jfc.com/tiptop/ontology#>\n" +
                           "SELECT ?component ?name ?spec\n" +
                           "WHERE {\n" +
                           "  ?component a j.0:ComponentItem .\n" +
                           "  ?component j.0:itemName ?name .\n" +
                           "  ?component j.0:itemSpec ?spec .\n" +
                           "}");
        queries.add(query1);
        
        Map<String, String> query2 = new HashMap<>();
        query2.put("name", "Find Components by Name");
        query2.put("description", "Finds components with a specific substring in their name");
        query2.put("query", "PREFIX j.0: <http://www.jfc.com/tiptop/ontology#>\n" +
                           "SELECT ?component ?name ?spec\n" +
                           "WHERE {\n" +
                           "  ?component a j.0:ComponentItem .\n" +
                           "  ?component j.0:itemName ?name .\n" +
                           "  ?component j.0:itemSpec ?spec .\n" +
                           "  FILTER(CONTAINS(lcase(?name), \"search_term\"))\n" +
                           "}");
        queries.add(query2);
        
        Map<String, String> query3 = new HashMap<>();
        query3.put("name", "Component Usage Count");
        query3.put("description", "Counts how many times each component is used across all BOMs");
        query3.put("query", "PREFIX j.0: <http://www.jfc.com/tiptop/ontology#>\n" +
                           "SELECT ?component ?name (COUNT(?bom) as ?usageCount)\n" +
                           "WHERE {\n" +
                           "  ?bom a j.0:BillOfMaterial .\n" +
                           "  ?bom j.0:hasComponentItem ?component .\n" +
                           "  ?component j.0:itemName ?name .\n" +
                           "}\n" +
                           "GROUP BY ?component ?name\n" +
                           "ORDER BY DESC(?usageCount)");
        queries.add(query3);
        
        Map<String, String> query4 = new HashMap<>();
        query4.put("name", "Expired BOMs");
        query4.put("description", "Finds BOMs that have expired (expiryDate in the past)");
        query4.put("query", "PREFIX j.0: <http://www.jfc.com/tiptop/ontology#>\n" +
                           "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                           "SELECT ?bom ?master ?component ?expiryDate\n" +
                           "WHERE {\n" +
                           "  ?bom a j.0:BillOfMaterial .\n" +
                           "  ?bom j.0:hasMasterItem ?master .\n" +
                           "  ?bom j.0:hasComponentItem ?component .\n" +
                           "  ?bom j.0:expiryDate ?expiryDate .\n" +
                           "  FILTER(?expiryDate < \"2025-04-28\"^^xsd:date)\n" +
                           "}");
        queries.add(query4);
        
        Map<String, String> query5 = new HashMap<>();
        query5.put("name", "Material Usage Path");
        query5.put("description", "Shows the usage path of a component through the BOM hierarchy");
        query5.put("query", "PREFIX j.0: <http://www.jfc.com/tiptop/ontology#>\n" +
                           "SELECT ?component ?master ?masterName\n" +
                           "WHERE {\n" +
                           "  ?component a j.0:ComponentItem .\n" +
                           "  ?component j.0:isUsedIn ?master .\n" +
                           "  ?master j.0:itemName ?masterName .\n" +
                           "  FILTER(STRENDS(STR(?component), \"COMPONENT_CODE\"))\n" +
                           "}");
        queries.add(query5);
        
        return queries;
    }
    
    /**
     * Gets example custom rules for the ontology.
     * 
     * @return A list of example rules with their descriptions
     */
    public List<Map<String, String>> getExampleRules() {
        List<Map<String, String>> rules = new ArrayList<>();
        
        Map<String, String> rule1 = new HashMap<>();
        rule1.put("name", "Critical Component Rule");
        rule1.put("description", "Identifies components that are used in more than 5 different master items");
        rule1.put("rule", "[CriticalComponentRule: (?component rdf:type <http://www.jfc.com/tiptop/ontology#ComponentItem>) " +
                         "(?bom1 <http://www.jfc.com/tiptop/ontology#hasComponentItem> ?component) " +
                         "(?bom1 <http://www.jfc.com/tiptop/ontology#hasMasterItem> ?master1) " +
                         "(?bom2 <http://www.jfc.com/tiptop/ontology#hasComponentItem> ?component) " +
                         "(?bom2 <http://www.jfc.com/tiptop/ontology#hasMasterItem> ?master2) " +
                         "(?bom3 <http://www.jfc.com/tiptop/ontology#hasComponentItem> ?component) " +
                         "(?bom3 <http://www.jfc.com/tiptop/ontology#hasMasterItem> ?master3) " +
                         "(?bom4 <http://www.jfc.com/tiptop/ontology#hasComponentItem> ?component) " +
                         "(?bom4 <http://www.jfc.com/tiptop/ontology#hasMasterItem> ?master4) " +
                         "(?bom5 <http://www.jfc.com/tiptop/ontology#hasComponentItem> ?component) " +
                         "(?bom5 <http://www.jfc.com/tiptop/ontology#hasMasterItem> ?master5) " +
                         "notEqual(?master1, ?master2) notEqual(?master1, ?master3) notEqual(?master1, ?master4) notEqual(?master1, ?master5) " +
                         "notEqual(?master2, ?master3) notEqual(?master2, ?master4) notEqual(?master2, ?master5) " +
                         "notEqual(?master3, ?master4) notEqual(?master3, ?master5) " +
                         "notEqual(?master4, ?master5) " +
                         "-> (?component rdf:type <http://www.jfc.com/tiptop/ontology#CriticalComponent>)]");
        rules.add(rule1);
        
        Map<String, String> rule2 = new HashMap<>();
        rule2.put("name", "Low Inventory Risk Rule");
        rule2.put("description", "Identifies components that are at risk of low inventory based on usage patterns");
        rule2.put("rule", "[LowInventoryRiskRule: (?component rdf:type <http://www.jfc.com/tiptop/ontology#ComponentItem>) " +
                         "(?bom <http://www.jfc.com/tiptop/ontology#hasComponentItem> ?component) " +
                         "(?bom <http://www.jfc.com/tiptop/ontology#quantity> ?quantity) " +
                         "greaterThan(?quantity, 5) " +
                         "-> (?component rdf:type <http://www.jfc.com/tiptop/ontology#HighUsageComponent>)]");
        rules.add(rule2);
        
        Map<String, String> rule3 = new HashMap<>();
        rule3.put("name", "Expired BOM Rule");
        rule3.put("description", "Marks BOMs that have expired based on their expiry date");
        rule3.put("rule", "[ExpiredBOMRule: (?bom rdf:type <http://www.jfc.com/tiptop/ontology#BillOfMaterial>) " +
                         "(?bom <http://www.jfc.com/tiptop/ontology#expiryDate> ?date) " +
                         "lessThan(?date, \"2025-04-28\"^^xsd:date) " +
                         "-> (?bom rdf:type <http://www.jfc.com/tiptop/ontology#ExpiredBOM>)]");
        rules.add(rule3);
        
        return rules;
    }
    
    /**
     * Gets information about the reasoners available in the system.
     * 
     * @return A list of available reasoners and their descriptions
     */
    public List<Map<String, String>> getAvailableReasoners() {
        List<Map<String, String>> reasoners = new ArrayList<>();
        
        Map<String, String> reasoner1 = new HashMap<>();
        reasoner1.put("id", "RDFS");
        reasoner1.put("name", "RDFS Rule Reasoner");
        reasoner1.put("description", "Simple RDFS reasoner that supports subclass, subproperty, domain, and range reasoning.");
        reasoners.add(reasoner1);
        
        Map<String, String> reasoner2 = new HashMap<>();
        reasoner2.put("id", "OWL_MICRO");
        reasoner2.put("name", "OWL Micro Reasoner");
        reasoner2.put("description", "OWL reasoner with support for a subset of OWL features. Good for performance but limited expressivity.");
        reasoners.add(reasoner2);
        
        Map<String, String> reasoner3 = new HashMap<>();
        reasoner3.put("id", "OWL_MINI");
        reasoner3.put("name", "OWL Mini Reasoner");
        reasoner3.put("description", "OWL reasoner with support for a larger subset of OWL features than Micro, but still limited.");
        reasoners.add(reasoner3);
        
        Map<String, String> reasoner4 = new HashMap<>();
        reasoner4.put("id", "OWL");
        reasoner4.put("name", "OWL Reasoner");
        reasoner4.put("description", "Full OWL reasoner with support for most OWL features. May be slower but more complete.");
        reasoners.add(reasoner4);
        
        return reasoners;
    }
}
package com.jfc.gnn.converter;


import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.springframework.stereotype.Component;
import com.jfc.gnn.model.*;
import com.jfc.gnn.model.KnowledgeGraph.GraphEdge;
import com.jfc.gnn.model.KnowledgeGraph.GraphNode;
import com.jfc.gnn.model.KnowledgeGraph.KnowledgeGraphBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

@Component
public class OntologyToGraphConverter {
    
    private static final Logger logger = LoggerFactory.getLogger(
        OntologyToGraphConverter.class);
    
    /**
     * Convert OWL ontology to knowledge graph
     */
    public KnowledgeGraph convertOntologyToGraph(OntModel ontModel) {
        logger.info("Converting OWL ontology to knowledge graph");
        
        KnowledgeGraphBuilder builder = KnowledgeGraph.builder();
        
        // Extract all individuals (nodes)
        //ExtIterator<Individual> individuals = ontModel.listIndividuals();
        ExtendedIterator<Individual> individuals = ontModel.listIndividuals();
        while (individuals.hasNext()) {
            Individual ind = individuals.next();
            GraphNode node = createNodeFromIndividual(ind);
            builder.addNode(node);
        }
        
        // Extract all object properties (edges)
        StmtIterator stmts = ontModel.listStatements();
        while (stmts.hasNext()) {
            Statement stmt = stmts.next();
            if (isObjectProperty(stmt)) {
                GraphEdge edge = createEdgeFromStatement(stmt);
                builder.addEdge(edge);
            }
        }
        
        return builder.build();
    }
    /**
     * Check if a statement represents an object property
     */
    private boolean isObjectProperty(Statement stmt) {
        // Object properties link resources (not literals)
        return stmt.getObject().isResource() && 
               !stmt.getPredicate().equals(RDF.type);
    }
    
    /**
     * Convert knowledge graph to GNN-compatible format
     */
    public GraphData convertToGraphData(KnowledgeGraph kg) {
        logger.info("Converting knowledge graph to GNN format");
        
        // Create node feature matrix
        float[][] nodeFeatures = createNodeFeatureMatrix(kg.getNodes());
        
        // Create adjacency matrix
        int[][] adjacencyMatrix = createAdjacencyMatrix(
            kg.getNodes(), 
            kg.getEdges()
        );
        
        // Create edge features
        float[][] edgeFeatures = createEdgeFeatureMatrix(kg.getEdges());
        
        // Create node and edge type mappings
        Map<String, Integer> nodeTypeMap = createNodeTypeMapping(kg.getNodes());
        Map<String, Integer> edgeTypeMap = createEdgeTypeMapping(kg.getEdges());
        
        return GraphData.builder()
            .nodeFeatures(nodeFeatures)
            .adjacencyMatrix(adjacencyMatrix)
            .edgeFeatures(edgeFeatures)
            .nodeTypes(nodeTypeMap)
            .edgeTypes(edgeTypeMap)
            .numNodes(kg.getNodes().size())
            .numEdges(kg.getEdges().size())
            .build();
    }
    
    /**
     * Create node from OWL individual
     */
    private GraphNode createNodeFromIndividual(Individual ind) {
        Map<String, Object> properties = new HashMap<>();
        
        // Extract all properties
        StmtIterator props = ind.listProperties();
        while (props.hasNext()) {
            Statement stmt = props.next();
            String propName = stmt.getPredicate().getLocalName();
            
            if (stmt.getObject().isLiteral()) {
                properties.put(propName, stmt.getObject().asLiteral().getValue());
            } else {
                properties.put(propName, stmt.getObject().toString());
            }
        }
        
        // Determine node type
        String nodeType = determineNodeType(ind);
        
        return GraphNode.builder()
            .id(ind.getURI())
            .type(nodeType)
            .properties(properties)
            .features(extractNodeFeatures(ind))
            .build();
    }
    
    /**
     * Extract numerical features from node
     */
    private float[] extractNodeFeatures(Individual ind) {
        List<Float> features = new ArrayList<>();
        
        // Extract numerical properties
        if (ind.hasProperty(ResourceFactory.createProperty(
                "http://www.jfc.com/tiptop/ontology#bore"))) {
            String bore = ind.getPropertyValue(
                ResourceFactory.createProperty(
                    "http://www.jfc.com/tiptop/ontology#bore"))
                .toString();
            features.add(Float.parseFloat(bore));
        } else {
            features.add(0.0f);
        }
        
        // Add more features...
        // Series encoding
        // Type encoding
        // etc.
        
        // Convert to array
        float[] featureArray = new float[features.size()];
        for (int i = 0; i < features.size(); i++) {
            featureArray[i] = features.get(i);
        }
        
        return featureArray;
    }
    
    /**
     * Create adjacency matrix from edges
     */
    private int[][] createAdjacencyMatrix(
            List<GraphNode> nodes, 
            List<GraphEdge> edges) {
        
        int n = nodes.size();
        int[][] matrix = new int[n][n];
        
        // Create node index mapping
        Map<String, Integer> nodeIndex = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            nodeIndex.put(nodes.get(i).getId(), i);
        }
        
        // Fill adjacency matrix
        for (GraphEdge edge : edges) {
            Integer srcIdx = nodeIndex.get(edge.getSourceId());
            Integer tgtIdx = nodeIndex.get(edge.getTargetId());
            
            if (srcIdx != null && tgtIdx != null) {
                matrix[srcIdx][tgtIdx] = 1;
                // If undirected
                matrix[tgtIdx][srcIdx] = 1;
            }
        }
        
        return matrix;
    }
    
    /**
     * Create node feature matrix
     */
    private float[][] createNodeFeatureMatrix(List<GraphNode> nodes) {
        if (nodes.isEmpty()) {
            return new float[0][0];
        }
        
        int numNodes = nodes.size();
        int featureDim = nodes.get(0).getFeatures().length;
        float[][] matrix = new float[numNodes][featureDim];
        
        for (int i = 0; i < numNodes; i++) {
            matrix[i] = nodes.get(i).getFeatures();
        }
        
        return matrix;
    }
    
    /**
     * Create edge from RDF statement
     */
    private GraphEdge createEdgeFromStatement(Statement stmt) {
        String sourceId = stmt.getSubject().getURI();
        String targetId = stmt.getObject().isResource() ? 
            stmt.getObject().asResource().getURI() : 
            stmt.getObject().toString();
        String edgeType = stmt.getPredicate().getLocalName();
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("predicate", stmt.getPredicate().getURI());
        
        return GraphEdge.builder()
            .id(sourceId + "_" + edgeType + "_" + targetId)
            .sourceId(sourceId)
            .targetId(targetId)
            .edgeType(edgeType)
            .properties(properties)
            .weight(1.0f)
            .build();
    }
    
    /**
     * Create edge feature matrix
     */
    private float[][] createEdgeFeatureMatrix(List<GraphEdge> edges) {
        if (edges.isEmpty()) {
            return new float[0][0];
        }
        
        // Define edge feature dimension (e.g., edge type encoding + weight)
        int featureDim = 5; // Adjust based on your needs
        float[][] matrix = new float[edges.size()][featureDim];
        
        for (int i = 0; i < edges.size(); i++) {
            GraphEdge edge = edges.get(i);
            float[] features = new float[featureDim];
            
            // Feature 0: weight
            features[0] = edge.getWeight();
            
            // Feature 1: quantity (if available)
            features[1] = edge.getQuantity() != null ? 
                edge.getQuantity().floatValue() : 0.0f;
            
            // Features 2-4: edge type encoding (can be one-hot)
            // You can implement more sophisticated encoding here
            
            matrix[i] = features;
        }
        
        return matrix;
    }
    
    /**
     * Create node type mapping
     */
    private Map<String, Integer> createNodeTypeMapping(List<GraphNode> nodes) {
        Map<String, Integer> typeMap = new HashMap<>();
        Set<String> uniqueTypes = new HashSet<>();
        
        // Collect unique node types
        for (GraphNode node : nodes) {
            uniqueTypes.add(node.getType());
        }
        
        // Assign integer IDs to each type
        int typeId = 0;
        for (String type : uniqueTypes) {
            typeMap.put(type, typeId++);
        }
        
        return typeMap;
    }

    /**
     * Create edge type mapping
     */
    private Map<String, Integer> createEdgeTypeMapping(List<GraphEdge> edges) {
        Map<String, Integer> typeMap = new HashMap<>();
        Set<String> uniqueTypes = new HashSet<>();
        
        // Collect unique edge types
        for (GraphEdge edge : edges) {
            uniqueTypes.add(edge.getEdgeType());
        }
        
        // Assign integer IDs to each type
        int typeId = 0;
        for (String type : uniqueTypes) {
            typeMap.put(type, typeId++);
        }
        
        return typeMap;
    }
    /**
     * Determine node type from OWL individual
     */
    private String determineNodeType(Individual ind) {
        // Check RDF types
        ExtendedIterator<Resource> types = ind.listRDFTypes(true);
        while (types.hasNext()) {
            Resource type = types.next();
            String typeName = type.getLocalName();
            if (typeName != null && !typeName.isEmpty()) {
                return typeName;
            }
        }
        
        // Default type
        return "Thing";
    }
}
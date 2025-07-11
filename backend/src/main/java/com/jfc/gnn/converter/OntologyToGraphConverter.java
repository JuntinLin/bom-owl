package com.jfc.gnn.converter;

import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.springframework.stereotype.Component;
import com.jfc.gnn.model.*;
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
        ExtIterator<Individual> individuals = ontModel.listIndividuals();
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
}
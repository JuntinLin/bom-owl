package com.jfc.gnn.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jfc.gnn.model.KnowledgeGraph;
import com.jfc.gnn.model.Triple;
import java.util.*;

/**
 * Service for managing knowledge graphs and graph operations
 */
@Service
public class KnowledgeGraphService {
    
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeGraphService.class);
    
    /**
     * Build a knowledge graph from triples
     */
    public KnowledgeGraph buildFromTriples(List<Triple> triples) {
        logger.info("Building knowledge graph from {} triples", triples.size());
        
        List<KnowledgeGraph.GraphNode> nodes = new ArrayList<>();
        List<KnowledgeGraph.GraphEdge> edges = new ArrayList<>();
        Map<String, KnowledgeGraph.GraphNode> nodeIndex = new HashMap<>();
        Map<String, List<KnowledgeGraph.GraphEdge>> adjacencyList = new HashMap<>();
        
        for (Triple triple : triples) {
            // Create or get subject node
            KnowledgeGraph.GraphNode subjectNode = nodeIndex.computeIfAbsent(
                triple.getSubject(),
                id -> KnowledgeGraph.GraphNode.builder()
                    .id(id)
                    .type("Entity")
                    .build()
            );
            
            // Create or get object node
            KnowledgeGraph.GraphNode objectNode = nodeIndex.computeIfAbsent(
                triple.getObject(),
                id -> KnowledgeGraph.GraphNode.builder()
                    .id(id)
                    .type("Entity")
                    .build()
            );
            
            // Create edge
            KnowledgeGraph.GraphEdge edge = KnowledgeGraph.GraphEdge.builder()
                .id(triple.getSubject() + "_" + triple.getPredicate() + "_" + triple.getObject())
                .sourceId(triple.getSubject())
                .targetId(triple.getObject())
                .edgeType(triple.getPredicate())
                .relationship(triple.getPredicate())
                .weight(1.0f)
                .build();
            
            edges.add(edge);
            adjacencyList.computeIfAbsent(triple.getSubject(), k -> new ArrayList<>()).add(edge);
        }
        
        nodes.addAll(nodeIndex.values());
        
        return KnowledgeGraph.builder()
            .nodes(nodes)
            .edges(edges)
            .nodeIndex(nodeIndex)
            .adjacencyList(adjacencyList)
            .build();
    }
    
    /**
     * Merge multiple knowledge graphs
     */
    public KnowledgeGraph mergeGraphs(List<KnowledgeGraph> graphs) {
        logger.info("Merging {} knowledge graphs", graphs.size());
        
        List<KnowledgeGraph.GraphNode> allNodes = new ArrayList<>();
        List<KnowledgeGraph.GraphEdge> allEdges = new ArrayList<>();
        Map<String, KnowledgeGraph.GraphNode> mergedNodeIndex = new HashMap<>();
        Map<String, List<KnowledgeGraph.GraphEdge>> mergedAdjacencyList = new HashMap<>();
        
        for (KnowledgeGraph graph : graphs) {
            // Merge nodes
            for (KnowledgeGraph.GraphNode node : graph.getNodes()) {
                mergedNodeIndex.putIfAbsent(node.getId(), node);
            }
            
            // Merge edges
            allEdges.addAll(graph.getEdges());
            
            // Merge adjacency lists
            graph.getAdjacencyList().forEach((nodeId, edges) -> {
                mergedAdjacencyList.computeIfAbsent(nodeId, k -> new ArrayList<>()).addAll(edges);
            });
        }
        
        allNodes.addAll(mergedNodeIndex.values());
        
        return KnowledgeGraph.builder()
            .nodes(allNodes)
            .edges(allEdges)
            .nodeIndex(mergedNodeIndex)
            .adjacencyList(mergedAdjacencyList)
            .build();
    }
    
    /**
     * Find paths between two nodes
     */
    public List<List<String>> findPaths(KnowledgeGraph graph, String startNodeId, String endNodeId, int maxDepth) {
        List<List<String>> paths = new ArrayList<>();
        findPathsRecursive(graph, startNodeId, endNodeId, new ArrayList<>(), paths, maxDepth);
        return paths;
    }
    
    private void findPathsRecursive(KnowledgeGraph graph, String currentNode, String targetNode, 
                                   List<String> currentPath, List<List<String>> allPaths, int maxDepth) {
        if (currentPath.size() > maxDepth) return;
        
        currentPath.add(currentNode);
        
        if (currentNode.equals(targetNode)) {
            allPaths.add(new ArrayList<>(currentPath));
        } else {
            List<KnowledgeGraph.GraphEdge> edges = graph.getAdjacencyList().get(currentNode);
            if (edges != null) {
                for (KnowledgeGraph.GraphEdge edge : edges) {
                    String nextNode = edge.getTargetId();
                    if (!currentPath.contains(nextNode)) {
                        findPathsRecursive(graph, nextNode, targetNode, currentPath, allPaths, maxDepth);
                    }
                }
            }
        }
        
        currentPath.remove(currentPath.size() - 1);
    }
}
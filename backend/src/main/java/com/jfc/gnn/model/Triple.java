package com.jfc.gnn.model;

import com.jfc.gnn.model.KnowledgeGraph.GraphEdge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Knowledge Graph Triple representation (Subject, Predicate, Object)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Triple {
    private String subject;     // Source entity
    private String predicate;   // Relationship/Edge type
    private String object;      // Target entity
    
    /**
     * Create a triple from edge information
     */
    public static Triple fromEdge(GraphEdge edge) {
        return Triple.builder()
            .subject(edge.getSourceId())
            .predicate(edge.getEdgeType())
            .object(edge.getTargetId())
            .build();
    }
    
    /**
     * Convert to RDF N-Triple format
     */
    public String toNTriple() {
        return String.format("<%s> <%s> <%s> .", subject, predicate, object);
    }
    
    /**
     * Convert to SPARQL pattern
     */
    public String toSparqlPattern() {
        return String.format("?s <%s> ?o", predicate);
    }
    
    @Override
    public String toString() {
        return String.format("(%s, %s, %s)", subject, predicate, object);
    }
}
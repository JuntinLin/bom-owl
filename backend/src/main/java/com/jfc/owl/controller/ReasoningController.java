package com.jfc.owl.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jfc.owl.service.ReasoningService;
import com.jfc.rdb.common.dto.AbstractDTOController;
import com.jfc.rdb.common.dto.ApiResponse;
import com.jfc.rdb.tiptop.entity.ImaFile;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for ontology reasoning operations.
 */
@RestController
@RequestMapping("/reasoning")
public class ReasoningController extends AbstractDTOController<ImaFile> {

    @Autowired
    private ReasoningService reasoningService;

    /**
     * Perform reasoning on a BOM ontology.
     * 
     * @param masterItemCode The master item code to reason about
     * @param reasonerType   The type of reasoner to use
     * @return The reasoning results
     */
    @GetMapping("/infer/{masterItemCode}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> performReasoning(
            @PathVariable String masterItemCode,
            @RequestParam(defaultValue = "OWL") String reasonerType) {
        
        try {
            Map<String, Object> results = reasoningService.performReasoning(masterItemCode, reasonerType);
            return success(results);
        } catch (Exception e) {
            return error("Error performing reasoning: " + e.getMessage());
        }
    }

    /**
     * Execute a SPARQL query on a BOM ontology.
     * 
     * @param masterItemCode The master item code to query
     * @param query          The SPARQL query to execute
     * @return The query results
     */
    @PostMapping("/sparql/{masterItemCode}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> executeSparqlQuery(
            @PathVariable String masterItemCode,
            @RequestBody String query) {
        
        try {
            Map<String, Object> results = reasoningService.executeSparqlQuery(masterItemCode, query);
            return success(results);
        } catch (Exception e) {
            return error("Error executing SPARQL query: " + e.getMessage());
        }
    }
    
    /**
     * Apply custom rules to a BOM ontology.
     * 
     * @param masterItemCode The master item code to apply rules to
     * @param rules          The custom rules to apply
     * @return The results after applying the rules
     */
    @PostMapping("/rules/{masterItemCode}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> applyCustomRules(
            @PathVariable String masterItemCode,
            @RequestBody String rules) {
        
        try {
            Map<String, Object> results = reasoningService.applyCustomRules(masterItemCode, rules);
            return success(results);
        } catch (Exception e) {
            return error("Error applying custom rules: " + e.getMessage());
        }
    }
    
    /**
     * Get predefined SPARQL queries.
     * 
     * @return A list of predefined queries with names and query text
     */
    @GetMapping("/predefined-queries")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getPredefinedQueries() {
        try {
            List<Map<String, String>> queries = reasoningService.getPredefinedQueries();
            return success(queries);
        } catch (Exception e) {
            return error("Error retrieving predefined queries: " + e.getMessage());
        }
    }
    
    /**
     * Get example rules for the ontology.
     * 
     * @return A list of example rules with descriptions
     */
    @GetMapping("/example-rules")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getExampleRules() {
        try {
            List<Map<String, String>> rules = reasoningService.getExampleRules();
            return success(rules);
        } catch (Exception e) {
            return error("Error retrieving example rules: " + e.getMessage());
        }
    }
    
    /**
     * Get available reasoners in the system.
     * 
     * @return A list of available reasoners with descriptions
     */
    @GetMapping("/available-reasoners")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getAvailableReasoners() {
        try {
            List<Map<String, String>> reasoners = reasoningService.getAvailableReasoners();
            return success(reasoners);
        } catch (Exception e) {
            return error("Error retrieving available reasoners: " + e.getMessage());
        }
    }
}
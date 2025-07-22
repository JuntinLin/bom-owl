package com.jfc.gnn.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.jfc.gnn.dto.*;
import com.jfc.gnn.service.GnnBomGenerationService;
import com.jfc.owl.service.OWLKnowledgeBaseService;
import com.jfc.rdb.common.dto.AbstractDTOController;
import com.jfc.rdb.tiptop.entity.ImaFile;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/gnn/bom")
public class BomGnnController{
	private static final Logger logger = LoggerFactory.getLogger(BomGnnController.class);
    
    @Autowired
    private GnnBomGenerationService gnnBomGenerationService;
    
    @Autowired
    private OWLKnowledgeBaseService owlKnowledgeBaseService;
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "GNN BOM Generation Service");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Generate BOM using GNN prediction
     */
    @PostMapping("/generate")
    public ResponseEntity<GnnBomGenerationResult> generateBomWithGnn(
            @RequestBody GnnBomGenerationRequest request) {
        
    	GnnBomGenerationResult result = gnnBomGenerationService
                .generateBomUsingGnn(request);
            
            return ResponseEntity.ok(result);
    }
    
    /**
     * Train GNN model with new BOM data
     */
    @PostMapping("/train")
    public ResponseEntity<GnnTrainingResult> trainGnnModel(
            @RequestBody GnnTrainingRequest request) {
        
        GnnTrainingResult result = gnnBomGenerationService
            .trainModel(request);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Get model performance metrics
     */
    @GetMapping("/metrics/{modelId}")
    public ResponseEntity<ModelMetrics> getModelMetrics(
            @PathVariable String modelId) {
        
        ModelMetrics metrics = gnnBomGenerationService
            .getModelMetrics(modelId);
        
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Predict components for a new product
     */
    @PostMapping("/predict-components")
    public ResponseEntity<ComponentPredictionResult> predictComponents(
            @RequestBody ComponentPredictionRequest request) {
        
        ComponentPredictionResult result = gnnBomGenerationService
            .predictComponents(request);
        
        return ResponseEntity.ok(result);
    }
}
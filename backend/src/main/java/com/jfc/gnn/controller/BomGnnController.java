package com.jfc.gnn.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.jfc.gnn.dto.*;
import com.jfc.gnn.service.GnnBomGenerationService;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/gnn/bom")
public class BomGnnController {
    
    @Autowired
    private GnnBomGenerationService gnnBomGenerationService;
    
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
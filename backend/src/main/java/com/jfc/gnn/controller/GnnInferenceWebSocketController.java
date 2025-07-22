package com.jfc.gnn.controller;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import com.jfc.gnn.dto.GnnInferenceRequest;
import com.jfc.gnn.dto.PredictedComponent;
import com.jfc.gnn.dto.PredictionOptions;
import com.jfc.gnn.service.GnnBomGenerationService;
import com.jfc.gnn.dto.GnnBomGenerationRequest;
import com.jfc.gnn.dto.GnnBomGenerationResult;
import com.jfc.gnn.dto.GnnInferenceProgress;

@Controller
public class GnnInferenceWebSocketController {
	private static final Logger logger = LoggerFactory.getLogger(GnnInferenceWebSocketController.class);
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private GnnBomGenerationService gnnBomGenerationService;
    
    @MessageMapping("/gnn/inference")
    @SendTo("/topic/inference-progress")
    public GnnInferenceProgress handleInferenceRequest(
            GnnInferenceRequest request) throws Exception {
    	logger.info("Received WebSocket inference request: {}", request.getRequestId());
        
        // Send initial acknowledgment
        GnnInferenceProgress startProgress = GnnInferenceProgress.started(request.getRequestId());
        
        // Process asynchronously to avoid blocking
        CompletableFuture.runAsync(() -> processInference(request));
        
        return startProgress;
    }
    
    /**
     * Process the inference request asynchronously with progress updates
     */
    private void processInference(GnnInferenceRequest request) {
        String destination = "/topic/inference-progress";
        
        try {
            // Step 1: Validate input (10%)
            sendProgress(destination, GnnInferenceProgress.processing(
                request.getRequestId(), 0.1, "Validating input specifications"));
            Thread.sleep(500); // Simulate processing
            
            // Step 2: Load model (20%)
            sendProgress(destination, GnnInferenceProgress.processing(
                request.getRequestId(), 0.2, "Loading GNN model"));
            Thread.sleep(500);
            
            // Step 3: Build knowledge graph (40%)
            sendProgress(destination, GnnInferenceProgress.processing(
                request.getRequestId(), 0.4, "Building knowledge graph"));
            Thread.sleep(1000);
            
            // Step 4: Run inference (70%)
            sendProgress(destination, GnnInferenceProgress.processing(
                request.getRequestId(), 0.7, "Running GNN inference"));
            
            // Prepare BOM generation request
            GnnBomGenerationRequest bomRequest = GnnBomGenerationRequest.builder()
                .newItemCode(request.getProductCode())
                .specifications(request.getSpecifications())
                .predictionOptions(PredictionOptions.builder()
                    .modelId(request.getModelId())
                    .build())
                .build();
            
            // Run actual inference
            GnnBomGenerationResult result = gnnBomGenerationService.generateBomUsingGnn(bomRequest);
            
            // Step 5: Post-process results (90%)
            sendProgress(destination, GnnInferenceProgress.processing(
                request.getRequestId(), 0.9, "Post-processing results"));
            Thread.sleep(500);
            
            // Step 6: Complete (100%)
            if (result.isSuccess() && result.getGeneratedBom() != null) {
                // Convert BOM components to predicted components
                List<PredictedComponent> predictions = result.getGeneratedBom().getComponents().stream()
                    .map(comp -> PredictedComponent.builder()
                        .componentCode(comp.getComponentId())
                        .quantity(comp.getQuantity())
                        .unit(comp.getUnit())
                        .confidence(result.getConfidenceScores().getOrDefault(comp.getComponentId(), 0.8))
                        .predictedBy("GNN")
                        .build())
                    .toList();
                
                sendProgress(destination, GnnInferenceProgress.completed(
                    request.getRequestId(), predictions));
            } else {
                sendProgress(destination, GnnInferenceProgress.error(
                    request.getRequestId(), result.getError()));
            }
            
        } catch (Exception e) {
            logger.error("Error during inference processing", e);
            sendProgress(destination, GnnInferenceProgress.error(
                request.getRequestId(), e.getMessage()));
        }
    }
    
    /**
     * Send progress update to WebSocket topic
     */
    private void sendProgress(String destination, GnnInferenceProgress progress) {
        try {
            messagingTemplate.convertAndSend(destination, progress);
            logger.debug("Sent progress update: {} - {}%", 
                progress.getCurrentStep(), progress.getProgress() * 100);
        } catch (Exception e) {
            logger.error("Error sending progress update", e);
        }
    }
    
    /**
     * Handle client disconnect
     */
    @MessageMapping("/gnn/cancel-inference")
    public void cancelInference(String requestId) {
        logger.info("Cancelling inference request: {}", requestId);
        // TODO: Implement cancellation logic if needed
    }
}
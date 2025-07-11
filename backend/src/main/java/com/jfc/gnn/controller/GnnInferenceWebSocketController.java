package com.jfc.gnn.controller;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import com.jfc.gnn.dto.GnnInferenceRequest;
import com.jfc.gnn.dto.GnnInferenceProgress;

@Controller
public class GnnInferenceWebSocketController {
    
    @MessageMapping("/gnn/inference")
    @SendTo("/topic/inference-progress")
    public GnnInferenceProgress handleInferenceRequest(
            GnnInferenceRequest request) throws Exception {
        
        // Real-time inference progress updates
        return new GnnInferenceProgress(
            request.getRequestId(),
            "Processing",
            0.5,
            "Running GNN inference..."
        );
    }
}
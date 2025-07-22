package com.jfc.gnn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * WebSocket response for GNN inference progress updates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GnnInferenceProgress {
    private String requestId;
    private String status; // "STARTED", "PROCESSING", "COMPLETED", "ERROR"
    private double progress; // 0.0 to 1.0
    private String message;
    private String currentStep;
    private long timestamp;
    private Map<String, Object> intermediateResults;
    private List<PredictedComponent> predictions;
    private String error;
    
    /**
     * Constructor for simple progress updates
     */
    public GnnInferenceProgress(String requestId, String status, double progress, String message) {
        this.requestId = requestId;
        this.status = status;
        this.progress = progress;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Create a started progress update
     */
    public static GnnInferenceProgress started(String requestId) {
        return GnnInferenceProgress.builder()
            .requestId(requestId)
            .status("STARTED")
            .progress(0.0)
            .message("Inference started")
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * Create a processing progress update
     */
    public static GnnInferenceProgress processing(String requestId, double progress, String step) {
        return GnnInferenceProgress.builder()
            .requestId(requestId)
            .status("PROCESSING")
            .progress(progress)
            .currentStep(step)
            .message("Processing: " + step)
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * Create a completed progress update
     */
    public static GnnInferenceProgress completed(String requestId, List<PredictedComponent> predictions) {
        return GnnInferenceProgress.builder()
            .requestId(requestId)
            .status("COMPLETED")
            .progress(1.0)
            .message("Inference completed successfully")
            .predictions(predictions)
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * Create an error progress update
     */
    public static GnnInferenceProgress error(String requestId, String error) {
        return GnnInferenceProgress.builder()
            .requestId(requestId)
            .status("ERROR")
            .progress(0.0)
            .message("Inference failed")
            .error(error)
            .timestamp(System.currentTimeMillis())
            .build();
    }
}
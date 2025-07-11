package com.jfc.gnn.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.IUpdater;
import org.nd4j.linalg.learning.config.Adam;

/**
 * Configuration class for Graph Neural Network models
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GnnModelConfig {
    
    // Model architecture
    @Builder.Default
    private ModelType modelType = ModelType.GCN;
    
    @Builder.Default
    private int inputDim = 128;
    
    @Builder.Default
    private int hiddenDim = 256;
    
    @Builder.Default
    private int outputDim = 128;
    
    @Builder.Default
    private int numLayers = 3;
    
    // For GAT specific
    @Builder.Default
    private int numHeads = 8;
    
    @Builder.Default
    private boolean concatHeads = true;
    
    // Training parameters
    @Builder.Default
    private double learningRate = 0.001;
    
    @Builder.Default
    private double dropout = 0.5;
    
    @Builder.Default
    private WeightInit weightInit = WeightInit.XAVIER;
    
    @Builder.Default
    private Activation activation = Activation.RELU;
    
    @Builder.Default
    private IUpdater updater = new Adam(0.001);
    
    @Builder.Default
    private long seed = 12345;
    
    // Regularization
    @Builder.Default
    private double l2Regularization = 0.0001;
    
    @Builder.Default
    private boolean useDropout = true;
    
    // Batch and training
    @Builder.Default
    private int batchSize = 32;
    
    @Builder.Default
    private int epochs = 100;
    
    // Graph specific parameters
    @Builder.Default
    private boolean addSelfLoops = true;
    
    @Builder.Default
    private boolean normalizeAdjacency = true;
    
    // Model checkpointing
    @Builder.Default
    private boolean enableCheckpointing = true;
    
    @Builder.Default
    private int checkpointInterval = 1000;
    
    @Builder.Default
    private String checkpointPath = "./checkpoints";
    
    // Performance optimization
    @Builder.Default
    private boolean enableGradientClipping = true;
    
    @Builder.Default
    private double gradientClipThreshold = 1.0;
    
    // Early stopping
    @Builder.Default
    private boolean enableEarlyStopping = true;
    
    @Builder.Default
    private int earlyStoppingPatience = 10;
    
    @Builder.Default
    private double earlyStoppingMinDelta = 0.001;
    
    /**
     * Model type enumeration
     */
    public enum ModelType {
        GCN("Graph Convolutional Network"),
        GAT("Graph Attention Network"),
        GRAPH_SAGE("GraphSAGE"),
        GIN("Graph Isomorphism Network"),
        MPNN("Message Passing Neural Network");
        
        private final String description;
        
        ModelType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Validate configuration
     */
    public void validate() {
        if (inputDim <= 0) {
            throw new IllegalArgumentException("Input dimension must be positive");
        }
        if (hiddenDim <= 0) {
            throw new IllegalArgumentException("Hidden dimension must be positive");
        }
        if (outputDim <= 0) {
            throw new IllegalArgumentException("Output dimension must be positive");
        }
        if (numLayers <= 0) {
            throw new IllegalArgumentException("Number of layers must be positive");
        }
        if (modelType == ModelType.GAT && numHeads <= 0) {
            throw new IllegalArgumentException("Number of attention heads must be positive for GAT");
        }
        if (learningRate <= 0) {
            throw new IllegalArgumentException("Learning rate must be positive");
        }
        if (dropout < 0 || dropout > 1) {
            throw new IllegalArgumentException("Dropout must be between 0 and 1");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be positive");
        }
        if (epochs <= 0) {
            throw new IllegalArgumentException("Number of epochs must be positive");
        }
    }
    
    /**
     * Create default configuration for specific model type
     */
    public static GnnModelConfig createDefault(ModelType modelType) {
        GnnModelConfigBuilder builder = GnnModelConfig.builder()
            .modelType(modelType);
        
        switch (modelType) {
            case GCN:
                return builder
                    .numLayers(3)
                    .hiddenDim(256)
                    .dropout(0.5)
                    .build();
                    
            case GAT:
                return builder
                    .numLayers(2)
                    .hiddenDim(256)
                    .numHeads(8)
                    .dropout(0.6)
                    .concatHeads(true)
                    .build();
                    
            case GRAPH_SAGE:
                return builder
                    .numLayers(2)
                    .hiddenDim(256)
                    .dropout(0.5)
                    .build();
                    
            case GIN:
                return builder
                    .numLayers(5)
                    .hiddenDim(256)
                    .dropout(0.5)
                    .build();
                    
            case MPNN:
                return builder
                    .numLayers(3)
                    .hiddenDim(128)
                    .dropout(0.5)
                    .build();
                    
            default:
                return builder.build();
        }
    }
    
    /**
     * Convert to string representation for logging
     */
    @Override
    public String toString() {
        return String.format(
            "GnnModelConfig{type=%s, layers=%d, input=%d, hidden=%d, output=%d, lr=%.4f}",
            modelType, numLayers, inputDim, hiddenDim, outputDim, learningRate
        );
    }
}
package com.jfc.gnn.repository;

import com.jfc.gnn.model.TrainedGnnModel;
import java.io.IOException;
import java.util.List;

/**
 * Interface for model repository operations
 */
public interface ModelRepository {
    
    /**
     * Save a trained model
     */
    String saveModel(TrainedGnnModel model, String modelName) throws IOException;
    
    /**
     * Load a model by ID
     */
    TrainedGnnModel loadModel(String modelId) throws IOException;
    
    /**
     * Delete a model
     */
    void deleteModel(String modelId) throws IOException;
    
    /**
     * List all available models
     */
    List<ModelMetadata> listModels();
    
    /**
     * Check if model exists
     */
    boolean modelExists(String modelId);
}
package com.jfc.gnn.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jfc.gnn.config.GnnModelConfig;
import com.jfc.gnn.core.GraphNeuralNetworkEngine;
import com.jfc.gnn.dto.GnnPredictionResult;
import com.jfc.gnn.dto.GnnTrainingResult;
import com.jfc.gnn.dto.PredictedComponent;
import com.jfc.gnn.model.KnowledgeGraph.GraphNode;
import com.jfc.gnn.service.GnnBomGenerationService;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
//=====================================================================
//8. TrainedGnnModel.java - Enhanced trained model class  
//=====================================================================

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainedGnnModel {
	private static final Logger logger = LoggerFactory.getLogger(TrainedGnnModel.class);

	private String modelId;
	private Object model; // The actual trained model (GCN or GAT)
    private MultiLayerNetwork network; // The actual DL4J network
	private GnnModelConfig config; // Model configuration used
	private long trainingTime; // Training time in milliseconds
	private String version; // Model version
	private Object evaluation; // Evaluation results
	private String modelType; // Model type (GCN, GAT, etc.)
	private Date createdAt; // When model was created
	private String description; // Model description

	/**
	 * Get model performance summary
	 */
	public Map<String, Object> getPerformanceSummary() {
		Map<String, Object> summary = new HashMap<>();
		summary.put("modelType", modelType);
		summary.put("version", version);
		summary.put("createdAt", createdAt);
		summary.put("trainingTimeSeconds", trainingTime / 1000.0);

		if (evaluation instanceof GnnTrainingResult) {
			GnnTrainingResult result = (GnnTrainingResult) evaluation;
			summary.put("accuracy", result.getAccuracy());
			summary.put("precision", result.getPrecision());
			summary.put("recall", result.getRecall());
			summary.put("f1Score", result.getF1Score());
		}

		return summary;
	}
	
	/**
	 * Predict components using the trained model
	 */
	public List<PredictedComponent> predictComponents(GraphNode productNode, Integer maxComponents) {
		// Create ProductSpecifications from GraphNode
	    ProductSpecifications specs = ProductSpecifications.builder()
	        .newItemCode(productNode.getId())
	        .bore(getPropertyAsString(productNode.getProperties(), "bore"))
	        .stroke(getPropertyAsString(productNode.getProperties(), "stroke"))
	        .series(getPropertyAsString(productNode.getProperties(), "series"))
	        .type(getPropertyAsString(productNode.getProperties(), "type"))
	        .rodEndType(getPropertyAsString(productNode.getProperties(), "rodEndType"))
	        .build();
	    
	    GnnPredictionResult result = null;
	    
	    // Get the actual model based on type
	    if (model instanceof GraphNeuralNetworkEngine.GraphConvolutionalNetwork) {
	        GraphNeuralNetworkEngine.GraphConvolutionalNetwork gcn = 
	            (GraphNeuralNetworkEngine.GraphConvolutionalNetwork) model;
	        result = gcn.predictBOM(specs);
	    } else if (model instanceof GraphNeuralNetworkEngine.GraphAttentionNetwork) {
	        GraphNeuralNetworkEngine.GraphAttentionNetwork gat = 
	            (GraphNeuralNetworkEngine.GraphAttentionNetwork) model;
	        result = gat.predictBOM(specs);
	    } else {
	        logger.warn("Unknown model type: {}", model != null ? model.getClass().getName() : "null");
	        return new ArrayList<>();
	    }
	    
	    // Return top N predictions
	    if (result != null) {
	        return result.getTopPredictions(maxComponents != null ? maxComponents : 50);
	    }
	    
	    return new ArrayList<>();
	}

	private String getPropertyAsString(Map<String, Object> properties, String key) {
	    return properties != null && properties.containsKey(key) ? 
	        properties.get(key).toString() : null;
	}
}
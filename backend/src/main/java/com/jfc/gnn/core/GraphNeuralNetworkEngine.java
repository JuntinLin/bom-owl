package com.jfc.gnn.core;
//=====================================================================

//1. Updated GraphNeuralNetworkEngine.java - Based on your original code
//=====================================================================

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.activations.Activation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.jfc.gnn.config.GnnModelConfig;
import com.jfc.gnn.model.*;
import com.jfc.gnn.model.KnowledgeGraph.GraphEdge;
import com.jfc.gnn.model.KnowledgeGraph.GraphNode;

import lombok.Data;

import com.jfc.gnn.dto.*;
import java.util.*;

@Component
public class GraphNeuralNetworkEngine {

	private static final Logger logger = LoggerFactory.getLogger(GraphNeuralNetworkEngine.class);

	/**
	 * Graph Convolutional Network implementation (your original code)
	 */
	@Data
	public class GraphConvolutionalNetwork {

		private MultiLayerNetwork model;
		private GnnModelConfig config;

		public GraphConvolutionalNetwork(GnnModelConfig config) {
			this.config = config;
			this.model = buildGCNModel(config);
		}

		/**
		 * Build GCN model architecture for BOM prediction
		 */
		private MultiLayerNetwork buildGCNModel(GnnModelConfig config) {
            // For BOM prediction, we'll use standard DenseLayer instead of custom GraphConvolutionLayer
            // This avoids the complexity of graph convolution for now and focuses on the prediction task
			MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
					.seed(config.getSeed())
					.weightInit(config.getWeightInit())
					.updater(config.getUpdater())
	                .l2(config.getL2Regularization())
					.list()
					// Input layer - hydraulic cylinder specifications
					.layer(0, new DenseLayer.Builder()
		                    .nIn(config.getInputDim())  // 20 features for hydraulic cylinder specs
		                    .nOut(config.getHiddenDim()) // 256 hidden units
		                    .activation(config.getActivation()) // ReLU
		                    .dropOut(config.getDropout())
		                    .build())
		                // Hidden layer 1
		                .layer(1, new DenseLayer.Builder()
		                    .nIn(config.getHiddenDim())
		                    .nOut(config.getHiddenDim() * 2) // Expand to 512
		                    .activation(config.getActivation())
		                    .dropOut(config.getDropout())
		                    .build())
		                // Hidden layer 2  
		                .layer(2, new DenseLayer.Builder()
		                    .nIn(config.getHiddenDim() * 2)
		                    .nOut(config.getHiddenDim()) // Back to 256
		                    .activation(config.getActivation())
		                    .dropOut(config.getDropout())
		                    .build())
		                // Output layer - component predictions
		                .layer(3, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
		                    .nIn(config.getHiddenDim())
		                    .nOut(config.getOutputDim()) // Number of possible components
		                    .activation(Activation.SIGMOID) // For multi-label classification
		                    .build())
		                .build();

			MultiLayerNetwork model = new MultiLayerNetwork(conf);
			model.init();
			model.setListeners(new ScoreIterationListener(100));

            logger.info("Built BOM Prediction GCN model with {} parameters", model.numParams());
			return model;
		}

		/**
		 * Forward propagation (fixed for DenseLayer model)
		 */
		public INDArray forward(GraphData graphData) {
			INDArray features = Nd4j.create(graphData.getNodeFeatures());
			
			// Apply graph convolution if adjacency matrix is provided
			if (graphData.getAdjacencyMatrix() != null) {
				INDArray adjacency = Nd4j.create(graphData.getAdjacencyMatrix());
				INDArray normalizedAdj = normalizeAdjacency(adjacency);
				// Apply graph convolution: A * X
				features = normalizedAdj.mmul(features);
			}
			
			// For BOM prediction with DenseLayer, we flatten the node features
			// and pass them directly to the model
			INDArray flattenedFeatures = flattenNodeFeatures(features);
			
			// Perform forward pass
			INDArray output = model.output(flattenedFeatures);
			return output;
		}
		
		/**
         * Flatten node features for dense layer input
         */
        private INDArray flattenNodeFeatures(INDArray nodeFeatures) {
            // If we have multiple nodes, we need to aggregate them into a single feature vector
            if (nodeFeatures.rank() > 2) {
                // Reshape to [batchSize, totalFeatures]
                return nodeFeatures.reshape(nodeFeatures.size(0), -1);
            } else if (nodeFeatures.rank() == 2) {
                // Already in correct format [nodes, features] or [batchSize, features]
                // For single example, take mean across nodes
                if (nodeFeatures.rows() > 1) {
                    return nodeFeatures.mean(0).reshape(1, -1);
                }
                return nodeFeatures;
            }
            return nodeFeatures;
        }

		/**
		 * Train the model (Legacy method - kept for compatibility)
		 * @deprecated Use trainForBOMPrediction instead
		 */
        @Deprecated
		public void train(TrainingDataset dataset, int epochs) {
			for (int epoch = 0; epoch < epochs; epoch++) {
				for (GraphBatch batch : dataset.getBatches()) {
					INDArray features = Nd4j.create(batch.getNodeFeatures());
					INDArray labels = Nd4j.create(batch.getLabels());
					//INDArray adjacency = Nd4j.create(batch.getAdjacency());

					model.fit(features, labels);
				}

				if (epoch % 10 == 0) {
					double score = model.score();
					logger.info("Epoch {}: score = {}", epoch, score);
				}
			}
		}

		/**
		 * NEW: Train for BOM prediction
		 */
		public GnnTrainingResult trainForBOMPrediction(BOMTrainingDataset bomDataset) {
			logger.info("Training GCN for BOM prediction with {} examples", bomDataset.getSize());

			long startTime = System.currentTimeMillis();
			List<Double> epochLosses = new ArrayList<>();
			double bestValidationAccuracy = 0.0;

			for (int epoch = 0; epoch < config.getEpochs(); epoch++) {
				double epochLoss = 0.0;
				int batchCount = 0;

				// Process BOM training batches
				List<BOMTrainingBatch> batches = bomDataset.getBatches(config.getBatchSize());

				for (BOMTrainingBatch batch : batches) {
					// Convert BOM data to graph format
					GraphData graphData = convertBOMToGraphData(batch);
					
					INDArray features = Nd4j.create(graphData.getNodeFeatures());
					INDArray labels = batch.getLabels();

					model.fit(features, labels);
					epochLoss += model.score();
					batchCount++;
				}

				epochLoss /= batchCount;
				epochLosses.add(epochLoss);

				// Validation every 5 epochs
				if (epoch % 5 == 0) {
					double validationAccuracy = evaluateBOMValidation(bomDataset.getValidationSet());
					if (validationAccuracy > bestValidationAccuracy) {
						bestValidationAccuracy = validationAccuracy;
					}
					logger.info("GCN Epoch {}: Loss = {:.6f}, Validation Accuracy = {:.4f}", epoch, epochLoss,
							validationAccuracy);
				}
			}

			long trainingTime = System.currentTimeMillis() - startTime;

			return GnnTrainingResult.builder()
					.success(true)
					.modelId("GCN_" + UUID.randomUUID().toString())
					.trainingTime(trainingTime)
					.finalLoss(epochLosses.get(epochLosses.size() - 1))
					.bestValidationAccuracy(bestValidationAccuracy)
					.epochLosses(epochLosses).build();
		}

		/**
		 * NEW: Predict BOM using trained GCN
		 */
		public GnnPredictionResult predictBOM(ProductSpecifications specs) {
			logger.info("Predicting BOM using GCN for: {}", specs.getNewItemCode());

			// Convert specs to graph input
			GraphData graphInput = convertSpecsToGraphData(specs);

			// Forward pass
			INDArray output = forward(graphInput);

			// Decode to component predictions
			List<PredictedComponent> componentPredictions = decodePredictions(output, 0.5);

			return GnnPredictionResult.builder()
					.componentPredictions(componentPredictions)
					.overallConfidence(calculateOverallConfidence(output))
					.modelId("GCN")
					.build();
		}

		// Helper method to normalize adjacency matrix
		private INDArray normalizeAdjacency(INDArray adjacency) {
			int n = adjacency.rows();
			
			// Add self-loops
			INDArray identity = Nd4j.eye(n);
			INDArray adjWithSelfLoops = adjacency.add(identity);
			
			// Compute degree matrix
			INDArray degree = adjWithSelfLoops.sum(1);
			INDArray degreeSqrtInv = Nd4j.zeros(n);
			
			for (int i = 0; i < n; i++) {
				double d = degree.getDouble(i);
				if (d > 0) {
					degreeSqrtInv.putScalar(i, 1.0 / Math.sqrt(d));
				}
			}
			
			// Symmetric normalization: D^(-1/2) * A * D^(-1/2)
			INDArray degreeMatrix = Nd4j.diag(degreeSqrtInv);
			return degreeMatrix.mmul(adjWithSelfLoops).mmul(degreeMatrix);
		}
	}

	/**
	 * Graph Attention Network implementation (your original code)
	 */
	@Data
	public class GraphAttentionNetwork {

		private MultiLayerNetwork model; // Changed from GATModel to MultiLayerNetwork
		private int numHeads;
		private int hiddenDim;
		private GnnModelConfig config;

		public GraphAttentionNetwork(int numHeads, int hiddenDim) {
			this.numHeads = numHeads;
			this.hiddenDim = hiddenDim;
			this.model = buildGATModel();
		}

		public GraphAttentionNetwork(GnnModelConfig config) {
			this.config = config;
			this.numHeads = config.getNumHeads();
			this.hiddenDim = config.getHiddenDim();
			this.model = buildGATModel();
		}

		/**
		 * Build GAT model
		 */
		private MultiLayerNetwork buildGATModel() {
			MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().seed(12345)
					.weightInit(org.deeplearning4j.nn.weights.WeightInit.XAVIER)
					.updater(new org.nd4j.linalg.learning.config.Adam(0.001)).list()
					.layer(0,
							new DenseLayer.Builder().nIn(hiddenDim).nOut(hiddenDim * 2).activation(Activation.RELU)
									.build())
					.layer(1,
							new DenseLayer.Builder().nIn(hiddenDim * 2).nOut(hiddenDim).activation(Activation.RELU)
									.build())
					.layer(2, new OutputLayer.Builder(LossFunctions.LossFunction.MSE).nIn(hiddenDim).nOut(1000) // Output
																												// dimension
																												// for
																												// components
							.activation(Activation.SIGMOID).build())
					.build();

			MultiLayerNetwork network = new MultiLayerNetwork(conf);
			network.init();
			network.setListeners(new ScoreIterationListener(10));

			return network;
		}

		/**
		 * Multi-head attention mechanism (your original)
		 */
		public INDArray multiHeadAttention(INDArray features, INDArray adjacency) {

			List<INDArray> headOutputs = new ArrayList<>();

			for (int h = 0; h < numHeads; h++) {
				INDArray headOutput = singleHeadAttention(features, adjacency, h);
				headOutputs.add(headOutput);
			}

			// Concatenate or average heads
			return concatenateHeads(headOutputs);
		}

		/**
		 * Single attention head (your original)
		 */
		private INDArray singleHeadAttention(INDArray features, INDArray adjacency, int headIndex) {

			// Linear transformation
			INDArray W = getWeight("W_" + headIndex);
			INDArray transformedFeatures = features.mmul(W);

			// Compute attention scores
			INDArray attentionScores = computeAttentionScores(transformedFeatures, adjacency);

			// Apply attention
			INDArray output = applyAttention(transformedFeatures, attentionScores);

			return output;
		}

		/**
		 * Compute attention scores between nodes (your original)
		 */
		private INDArray computeAttentionScores(INDArray features, INDArray adjacency) {

			int numNodes = features.rows();
			INDArray scores = Nd4j.zeros(numNodes, numNodes);

			// Self-attention mechanism
			for (int i = 0; i < numNodes; i++) {
				for (int j = 0; j < numNodes; j++) {
					if (adjacency.getDouble(i, j) > 0) {
						double score = computePairwiseAttention(features.getRow(i), features.getRow(j));
						scores.putScalar(i, j, score);
					}
				}
			}

			// Apply softmax
			return softmaxByRow(scores);
		}

		/**
		 * NEW: Train GAT for BOM prediction
		 */
		public GnnTrainingResult trainForBOMPrediction(BOMTrainingDataset bomDataset) {
			logger.info("Training GAT for BOM prediction with {} examples", bomDataset.getSize());

			long startTime = System.currentTimeMillis();
			List<Double> epochLosses = new ArrayList<>();
			double bestValidationAccuracy = 0.0;

			int epochs = config != null ? config.getEpochs() : 100;
			int batchSize = config != null ? config.getBatchSize() : 32;

			for (int epoch = 0; epoch < epochs; epoch++) {
				double epochLoss = 0.0;
				int batchCount = 0;

				List<BOMTrainingBatch> batches = bomDataset.getBatches(batchSize);

				for (BOMTrainingBatch batch : batches) {
					INDArray features = batch.getFeatures();
					INDArray labels = batch.getLabels();

					// Apply attention mechanism
					INDArray adjacency = createAdjacencyFromBatch(batch);
					INDArray attentionOutput = multiHeadAttention(features, adjacency);

					model.fit(attentionOutput, labels);
					epochLoss += model.score();
					batchCount++;
				}

				epochLoss /= batchCount;
				epochLosses.add(epochLoss);

				if (epoch % 5 == 0) {
					double validationAccuracy = evaluateBOMValidation(bomDataset.getValidationSet());
					if (validationAccuracy > bestValidationAccuracy) {
						bestValidationAccuracy = validationAccuracy;
					}
					logger.info("GAT Epoch {}: Loss = {:.6f}, Validation Accuracy = {:.4f}", epoch, epochLoss,
							validationAccuracy);
				}
			}

			long trainingTime = System.currentTimeMillis() - startTime;

			return GnnTrainingResult.builder().success(true).modelId("GAT_" + UUID.randomUUID().toString())
					.trainingTime(trainingTime).finalLoss(epochLosses.get(epochLosses.size() - 1))
					.bestValidationAccuracy(bestValidationAccuracy).epochLosses(epochLosses).build();
		}

		/**
		 * NEW: Predict BOM using trained GAT
		 */
		public GnnPredictionResult predictBOM(ProductSpecifications specs) {
			logger.info("Predicting BOM using GAT for: {}", specs.getNewItemCode());

			// Convert specs to features
			INDArray features = convertSpecsToFeatures(specs);
			INDArray adjacency = createSelfAttentionAdjacency(features);

			// Apply multi-head attention
			INDArray attentionOutput = multiHeadAttention(features, adjacency);

			// Get final prediction
			INDArray output = model.output(attentionOutput);

			List<PredictedComponent> componentPredictions = decodePredictions(output, 0.5);

			return GnnPredictionResult.builder().componentPredictions(componentPredictions)
					.overallConfidence(calculateOverallConfidence(output)).modelId("GAT").build();
		}

		// Helper methods for GAT
		private INDArray getWeight(String weightName) {
			// Simplified weight retrieval
			return Nd4j.randn(hiddenDim, hiddenDim);
		}

		private INDArray concatenateHeads(List<INDArray> headOutputs) {
			return Nd4j.concat(1, headOutputs.toArray(new INDArray[0]));
		}

		private INDArray applyAttention(INDArray features, INDArray scores) {
			return scores.mmul(features);
		}

		private double computePairwiseAttention(INDArray feat1, INDArray feat2) {
			return feat1.mmul(feat2.transpose()).getDouble(0);
		}

		private INDArray softmaxByRow(INDArray scores) {
			return org.nd4j.linalg.ops.transforms.Transforms.softmax(scores);
		}
		
		
	}

	/**
	 * Knowledge Graph Embedding (your original code)
	 */
	public class KnowledgeGraphEmbedding {

		private Map<String, INDArray> entityEmbeddings;
		private Map<String, INDArray> relationEmbeddings;
		private int embeddingDim;

		public KnowledgeGraphEmbedding(int embeddingDim) {
			this.embeddingDim = embeddingDim;
			this.entityEmbeddings = new HashMap<>();
			this.relationEmbeddings = new HashMap<>();
		}

		/**
		 * Learn embeddings using TransE (your original)
		 */
		public void learnEmbeddings(KnowledgeGraph kg, int epochs) {
			// Initialize embeddings
			initializeEmbeddings(kg);

			// Training loop
			for (int epoch = 0; epoch < epochs; epoch++) {
				List<Triple> triples = kg.getTriples();
				Collections.shuffle(triples);

				for (Triple triple : triples) {
					// Positive sample
					updateEmbeddings(triple, true);

					// Negative sampling
					Triple negativeTriple = generateNegativeSample(triple, kg);
					updateEmbeddings(negativeTriple, false);
				}

				if (epoch % 100 == 0) {
					double loss = computeLoss(kg);
					logger.info("Epoch {}: loss = {}", epoch, loss);
				}
			}
		}

		/**
		 * Get embedding for entity (your original)
		 */
		public INDArray getEntityEmbedding(String entityId) {
			return entityEmbeddings.get(entityId);
		}

		/**
		 * Compute similarity between entities (your original)
		 */
		public double computeSimilarity(String entity1, String entity2) {
			INDArray emb1 = entityEmbeddings.get(entity1);
			INDArray emb2 = entityEmbeddings.get(entity2);

			if (emb1 != null && emb2 != null) {
				return cosineSimilarity(emb1, emb2);
			}

			return 0.0;
		}

		// Placeholder methods - implement as needed
		private void initializeEmbeddings(KnowledgeGraph kg) {
			// Initialize entity embeddings with random values
		    // Extract all unique entities from nodes
		    Set<String> entities = new HashSet<>();
		    for (GraphNode node : kg.getNodes()) {
		        entities.add(node.getId());
		    }
		    
			for (String entity : entities) {
				entityEmbeddings.put(entity, Nd4j.randn(embeddingDim));
			}

			// Initialize relation embeddings with random values
		    // Extract all unique relation types from edges
		    Set<String> relations = new HashSet<>();
		    for (GraphEdge edge : kg.getEdges()) {
		        relations.add(edge.getEdgeType());
		    }
			for (String relation : relations) {
				relationEmbeddings.put(relation, Nd4j.randn(embeddingDim));
			}
		}

		private void updateEmbeddings(Triple triple, boolean positive) {
			// TransE update rule implementation
			double learningRate = 0.01;
			double margin = 1.0;

			INDArray head = entityEmbeddings.get(triple.getSubject());
			INDArray relation = relationEmbeddings.get(triple.getPredicate());
			INDArray tail = entityEmbeddings.get(triple.getObject());

			if (head != null && relation != null && tail != null) {
				// Compute score: ||h + r - t||
				INDArray predicted = head.add(relation);
				INDArray error = predicted.sub(tail);
				double score = error.norm2Number().doubleValue();

				// Update embeddings based on margin ranking loss
				if (positive && score > 0) {
					// Decrease distance for positive samples
					INDArray gradient = error.div(score);
					head.subi(gradient.mul(learningRate));
					relation.subi(gradient.mul(learningRate));
					tail.addi(gradient.mul(learningRate));
				} else if (!positive && score < margin) {
					// Increase distance for negative samples
					INDArray gradient = error.div(score + 1e-8);
					head.addi(gradient.mul(learningRate));
					relation.addi(gradient.mul(learningRate));
					tail.subi(gradient.mul(learningRate));
				}

				// Normalize embeddings to prevent them from growing too large
				head.divi(head.norm2Number().doubleValue() + 1e-8);
				relation.divi(relation.norm2Number().doubleValue() + 1e-8);
				tail.divi(tail.norm2Number().doubleValue() + 1e-8);
			}
		}

		private Triple generateNegativeSample(Triple triple, KnowledgeGraph kg) {
			// Simple negative sampling: randomly replace head or tail
			Random rand = new Random();
			// Get all entity IDs from nodes
		    List<String> entities = new ArrayList<>();
		    for (GraphNode node : kg.getNodes()) {
		        entities.add(node.getId());
		    }

			if (rand.nextBoolean()) {
				// Replace head
				String newSubject = entities.get(rand.nextInt(entities.size()));
		        return new Triple(newSubject, triple.getPredicate(), triple.getObject());
			} else {
				// Replace tail
				String newObject = entities.get(rand.nextInt(entities.size()));
		        return new Triple(triple.getSubject(), triple.getPredicate(), newObject);
			}
		}

		private double computeLoss(KnowledgeGraph kg) {
			double totalLoss = 0.0;
			int count = 0;
			
			// Compute average distance for all triples
			for (Triple triple : kg.getTriples()) {
				INDArray head = entityEmbeddings.get(triple.getSubject());
				INDArray relation = relationEmbeddings.get(triple.getPredicate());
				INDArray tail = entityEmbeddings.get(triple.getObject());
				
				if (head != null && relation != null && tail != null) {
					// TransE score: ||h + r - t||
					double score = head.add(relation).sub(tail).norm2Number().doubleValue();
					totalLoss += score;
					count++;
				}
			}
			
			return count > 0 ? totalLoss / count : 0.0;
		}

		private double cosineSimilarity(INDArray emb1, INDArray emb2) {
			return emb1.mmul(emb2.transpose()).getDouble(0)
					/ (emb1.norm2Number().doubleValue() * emb2.norm2Number().doubleValue());
		}
	}

	// =====================================================================
	// PUBLIC API METHODS (Updated from your original)
	// =====================================================================

	/**
	 * BOM prediction using GNN (Updated from your original)
	 */
	public GnnPredictionResult predict(GraphData graphData, ProductSpecifications specs, PredictionOptions options) {

		logger.info("Running GNN prediction for product specs: {}", specs);

		// Select model type based on configuration
		GnnModelConfig.ModelType modelType = options.getModelType();

		switch (modelType) {
		case GCN:
			GraphConvolutionalNetwork gcn = new GraphConvolutionalNetwork(options.getConfig());
			return gcn.predictBOM(specs);

		case GAT:
			GraphAttentionNetwork gat = new GraphAttentionNetwork(options.getConfig());
			return gat.predictBOM(specs);

		default:
			throw new IllegalArgumentException("Unsupported model type: " + modelType);
		}
	}

	/**
	 * Train GNN model (Updated from your original)
	 */
	public TrainedGnnModel trainModel(TrainingDataset dataset, GnnModelConfig config, TrainingOptions options) {

		logger.info("Training GNN model with config: {}", config);

		long startTime = System.currentTimeMillis();

		// For BOM prediction, convert TrainingDataset to BOMTrainingDataset
		BOMTrainingDataset bomDataset = convertToBOMTrainingDataset(dataset);

		GnnTrainingResult result;
		Object trainedModel;

		switch (config.getModelType()) {
		case GCN:
			GraphConvolutionalNetwork gcn = new GraphConvolutionalNetwork(config);
			result = gcn.trainForBOMPrediction(bomDataset);
			trainedModel = gcn;
			break;

		case GAT:
			GraphAttentionNetwork gat = new GraphAttentionNetwork(config);
			result = gat.trainForBOMPrediction(bomDataset);
			trainedModel = gat;
			break;

		default:
			throw new IllegalArgumentException("Unknown model type: " + config.getModelType());
		}

		long trainingTime = System.currentTimeMillis() - startTime;

		return TrainedGnnModel.builder()
				.model(trainedModel)
				.network((MultiLayerNetwork) ((TrainedGnnModel) trainedModel).getModel())
				.config(config)
				.trainingTime(trainingTime)
				.version(generateModelVersion())
				.evaluation(result)
				.build();
	}

	// =====================================================================
	// HELPER METHODS
	// =====================================================================

	/**
	 * Convert ProductSpecifications to GraphData
	 */
	private GraphData convertSpecsToGraphData(ProductSpecifications specs) {
		// Create a simple graph representation of the specifications
		float[][] nodeFeatures = createNodeFeaturesFromSpecs(specs);
		int[][] adjacencyMatrix = createBasicAdjacencyMatrix(nodeFeatures.length);

		return GraphData.builder().nodeFeatures(nodeFeatures).adjacencyMatrix(adjacencyMatrix)
				.numNodes(nodeFeatures.length).build();
	}

	/**
	 * Convert ProductSpecifications to feature vector
	 */
	private INDArray convertSpecsToFeatures(ProductSpecifications specs) {
		float[] features = new float[20]; // Adjust size as needed
		int idx = 0;

		// Basic specifications
		features[idx++] = parseFloatSafely(specs.getBore(), 0f);
		features[idx++] = parseFloatSafely(specs.getStroke(), 0f);
		features[idx++] = parseFloatSafely(specs.getSeries(), 0f);
		features[idx++] = parseFloatSafely(specs.getType(), 0f);

		// One-hot encoding for categorical features
		String rodEndType = specs.getRodEndType();
		if ("Y".equals(rodEndType))
			features[idx] = 1.0f;
		idx++;
		if ("I".equals(rodEndType))
			features[idx] = 1.0f;
		idx++;
		if ("E".equals(rodEndType))
			features[idx] = 1.0f;
		idx++;

		// Fill remaining with zeros
		while (idx < features.length) {
			features[idx++] = 0f;
		}

		return Nd4j.create(features).reshape(1, features.length);
	}

	/**
	 * Convert BOMTrainingBatch to GraphData
	 */
	private GraphData convertBOMToGraphData(BOMTrainingBatch batch) {
		// Convert BOM batch to graph representation
		float[][] nodeFeatures = batch.getFeatures().toFloatMatrix();
		int numNodes = nodeFeatures.length;
		int[][] adjacencyMatrix = createBasicAdjacencyMatrix(numNodes);

		return GraphData.builder()
				.nodeFeatures(nodeFeatures)
				.adjacencyMatrix(adjacencyMatrix)
				.numNodes(numNodes)
				.build();
	}

	/**
	 * Decode model output to component predictions
	 */
	private List<PredictedComponent> decodePredictions(INDArray output, double threshold) {
		List<PredictedComponent> predictions = new ArrayList<>();

		float[] probabilities = output.toFloatVector();

		for (int i = 0; i < probabilities.length; i++) {
			if (probabilities[i] > threshold) {
				PredictedComponent prediction = PredictedComponent.builder()
						.componentCode("COMP_" + i) // This should map to actual component codes
						.confidence((double) probabilities[i])
						.predictedBy("GNN")  // Or use the actual model type
						.build();
				predictions.add(prediction);
			}
		}

		return predictions;
	}

	/**
	 * Calculate overall confidence from output
	 */
	private double calculateOverallConfidence(INDArray output) {
		float[] probabilities = output.toFloatVector();
		double sum = 0.0;
		int count = 0;

		for (float prob : probabilities) {
			if (prob > 0.5) {
				sum += prob;
				count++;
			}
		}

		return count > 0 ? sum / count : 0.0;
	}

	/**
	 * Evaluate BOM validation set
	 */
	private double evaluateBOMValidation(List<BOMTrainingExample> validationSet) {
		if (validationSet == null || validationSet.isEmpty()) {
			return 0.0;
		}

		// Simplified validation - implement actual evaluation logic
		return 0.75; // Placeholder
	}

	/**
	 * Convert TrainingDataset to BOMTrainingDataset
	 */
	private BOMTrainingDataset convertToBOMTrainingDataset(TrainingDataset dataset) {
		// Convert your existing TrainingDataset to BOMTrainingDataset
		// This is a placeholder - implement based on your data structure
		return BOMTrainingDataset.builder().examples(new ArrayList<>()).validationSet(new ArrayList<>())
				.testSet(new ArrayList<>()).build();
	}

	/**
	 * Create basic adjacency matrix
	 */
	private int[][] createBasicAdjacencyMatrix(int size) {
		int[][] matrix = new int[size][size];
		// Create a simple connected graph
		for (int i = 0; i < size - 1; i++) {
			matrix[i][i + 1] = 1;
			matrix[i + 1][i] = 1;
		}
		return matrix;
	}

	/**
	 * Create node features from specifications
	 */
	private float[][] createNodeFeaturesFromSpecs(ProductSpecifications specs) {
		// Create features for each "node" in the specification
		float[][] features = new float[5][10]; // 5 nodes, 10 features each

		// Node 0: Bore information
		features[0][0] = parseFloatSafely(specs.getBore(), 0f);

		// Node 1: Stroke information
		features[1][0] = parseFloatSafely(specs.getStroke(), 0f);

		// Node 2: Series information
		features[2][0] = parseFloatSafely(specs.getSeries(), 0f);

		// Node 3: Type information
		features[3][0] = parseFloatSafely(specs.getType(), 0f);

		// Node 4: Rod end type (one-hot encoded)
		if ("Y".equals(specs.getRodEndType()))
			features[4][0] = 1.0f;
		else if ("I".equals(specs.getRodEndType()))
			features[4][1] = 1.0f;
		else if ("E".equals(specs.getRodEndType()))
			features[4][2] = 1.0f;

		return features;
	}

	/**
	 * Create adjacency matrix for attention mechanism
	 */
	private INDArray createAdjacencyFromBatch(BOMTrainingBatch batch) {
		int size = batch.getSize();
		return Nd4j.eye(size); // Identity matrix for self-attention
	}

	/**
	 * Create self-attention adjacency matrix
	 */
	private INDArray createSelfAttentionAdjacency(INDArray features) {
		int size = features.rows();
		return Nd4j.ones(size, size); // Fully connected for attention
	}

	/**
	 * Generate model version
	 */
	private String generateModelVersion() {
		return "v1.0_" + System.currentTimeMillis();
	}

	/**
	 * Safely parse float from string
	 */
	private float parseFloatSafely(String value, float defaultValue) {
		try {
			return value != null && !value.isEmpty() ? Float.parseFloat(value) : defaultValue;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
	
	/**
	 * Load a trained model by ID
	 */
	public TrainedGnnModel loadModel(String modelId) {
	    logger.info("Loading model: {}", modelId);
	    
	    try {
	        // TODO: Implement actual model loading from storage/database
	        // For now, determine model type from modelId pattern
	        String modelType = "GCN"; // Default
	        
	        if (modelId != null) {
	            if (modelId.startsWith("GAT_") || modelId.contains("GAT")) {
	                modelType = "GAT";
	            } else if (modelId.startsWith("GCN_") || modelId.contains("GCN")) {
	                modelType = "GCN";
	            }
	        }
	        
	        // Create appropriate model instance based on type
	        Object modelInstance = null;
	        GnnModelConfig config = GnnModelConfig.builder()
	            .modelType(modelType.equals("GAT") ? 
	                GnnModelConfig.ModelType.GAT : 
	                GnnModelConfig.ModelType.GCN)
	            .inputDim(20)
	            .hiddenDim(256)
	            .outputDim(1000)
	            .numHeads(modelType.equals("GAT") ? 8 : 1)
	            .build();
	        
	        if (modelType.equals("GAT")) {
	            modelInstance = new GraphAttentionNetwork(config);
	        } else {
	            modelInstance = new GraphConvolutionalNetwork(config);
	        }
	        
	        // In production, you would:
	        // 1. Load model metadata from database
	        // 2. Load model weights from file storage
	        // 3. Restore the model state
	        
	        return TrainedGnnModel.builder()
	            .modelId(modelId)
	            .model(modelInstance)
	            .modelType(modelType)
	            .config(config)
	            .version("v1.0")
	            .createdAt(new Date())
	            .description("Loaded model " + modelId)
	            .build();
	            
	    } catch (Exception e) {
	        logger.error("Error loading model {}: {}", modelId, e.getMessage());
	        throw new RuntimeException("Failed to load model: " + modelId, e);
	    }
	}
}
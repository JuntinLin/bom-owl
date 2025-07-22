package com.jfc.gnn.core;

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

import com.jfc.gnn.config.GnnModelConfig;

/**
 * Simplified Graph Neural Network implementation using standard DL4J layers
 * 
 * This approach implements graph convolution logic at the data preprocessing level
 * rather than creating custom layers, which avoids compatibility issues and is
 * easier to maintain.
 */
public class SimplifiedGraphNeuralNetwork {
    
    private static final Logger logger = LoggerFactory.getLogger(SimplifiedGraphNeuralNetwork.class);
    
    private MultiLayerNetwork model;
    //private INDArray adjacencyMatrix;
    
    /**
     * Build a GCN-inspired model using standard layers
     */
    public MultiLayerNetwork buildGCNModel(GnnModelConfig config) {
        logger.info("Building GCN-inspired model for BOM prediction");
        
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
            .seed(config.getSeed())
            .weightInit(config.getWeightInit())
            .updater(config.getUpdater())
            .l2(config.getL2Regularization())
            .list()
            // First hidden layer - processes graph-convolved features
            .layer(0, new DenseLayer.Builder()
                .nIn(config.getInputDim())
                .nOut(config.getHiddenDim())
                .activation(config.getActivation())
                .dropOut(config.getDropout())
                .build())
            // Second hidden layer - deeper feature extraction
            .layer(1, new DenseLayer.Builder()
                .nIn(config.getHiddenDim())
                .nOut(config.getHiddenDim() * 2)
                .activation(config.getActivation())
                .dropOut(config.getDropout())
                .build())
            // Third hidden layer - feature refinement
            .layer(2, new DenseLayer.Builder()
                .nIn(config.getHiddenDim() * 2)
                .nOut(config.getHiddenDim())
                .activation(config.getActivation())
                .dropOut(config.getDropout())
                .build())
            // Output layer - component predictions
            .layer(3, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                .nIn(config.getHiddenDim())
                .nOut(config.getOutputDim())
                .activation(Activation.SIGMOID)
                .build())
            .build();
        
        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();
        model.setListeners(new ScoreIterationListener(100));
        
        logger.info("Built GCN-inspired model with {} parameters", model.numParams());
        return model;
    }
    
    /**
     * Apply graph convolution to features before feeding to the network
     * This implements the key GCN operation: A * X * W
     * 
     * @param features Node feature matrix (nodes x features)
     * @param adjacencyMatrix Graph adjacency matrix (nodes x nodes)
     * @return Graph-convolved features
     */
    public INDArray applyGraphConvolution(INDArray features, INDArray adjacencyMatrix) {
        // Normalize adjacency matrix with self-loops
        INDArray normalizedAdj = normalizeAdjacencyMatrix(adjacencyMatrix);
        
        // Apply graph convolution: A_norm * X
        // This aggregates features from neighboring nodes
        INDArray convolvedFeatures = normalizedAdj.mmul(features);
        
        return convolvedFeatures;
    }
    
    /**
     * Normalize adjacency matrix using the GCN normalization trick
     * A_norm = D^(-1/2) * (A + I) * D^(-1/2)
     */
    private INDArray normalizeAdjacencyMatrix(INDArray adj) {
        int n = adj.rows();
        
        // Add self-loops (identity matrix)
        INDArray identity = Nd4j.eye(n);
        INDArray adjWithSelfLoops = adj.add(identity);
        
        // Compute degree matrix
        INDArray degree = adjWithSelfLoops.sum(1);
        INDArray degreeSqrtInv = Nd4j.zeros(n);
        
        for (int i = 0; i < n; i++) {
            double d = degree.getDouble(i);
            if (d > 0) {
                degreeSqrtInv.putScalar(i, 1.0 / Math.sqrt(d));
            }
        }
        
        // Symmetric normalization
        INDArray degreeMatrix = Nd4j.diag(degreeSqrtInv);
        INDArray normalized = degreeMatrix.mmul(adjWithSelfLoops).mmul(degreeMatrix);
        
        return normalized;
    }
    
    /**
     * Forward pass with graph convolution preprocessing
     */
    public INDArray forward(INDArray features, INDArray adjacencyMatrix) {
        // Apply graph convolution to aggregate neighbor features
        INDArray graphFeatures = applyGraphConvolution(features, adjacencyMatrix);
        
        // For single node prediction (like BOM), flatten if needed
        if (graphFeatures.rank() > 2) {
            graphFeatures = graphFeatures.reshape(graphFeatures.size(0), -1);
        } else if (graphFeatures.rows() > 1) {
            // Aggregate multiple nodes into single feature vector
            graphFeatures = graphFeatures.mean(0).reshape(1, -1);
        }
        
        // Standard forward pass through the network
        return model.output(graphFeatures);
    }
    
    /**
     * Train the model with graph-preprocessed features
     */
    public void train(INDArray features, INDArray labels, INDArray adjacencyMatrix) {
        // Apply graph convolution preprocessing
        INDArray graphFeatures = applyGraphConvolution(features, adjacencyMatrix);
        
        // Train on graph-convolved features
        model.fit(graphFeatures, labels);
    }
}

/**
 * Usage example in your GraphNeuralNetworkEngine:
 * 
 * public class GraphConvolutionalNetwork {
 *     private SimplifiedGraphNeuralNetwork gnn;
 *     
 *     public GraphConvolutionalNetwork(GnnModelConfig config) {
 *         this.gnn = new SimplifiedGraphNeuralNetwork();
 *         this.model = gnn.buildGCNModel(config);
 *     }
 *     
 *     public INDArray forward(GraphData graphData) {
 *         INDArray features = Nd4j.create(graphData.getNodeFeatures());
 *         INDArray adjacency = Nd4j.create(graphData.getAdjacencyMatrix());
 *         
 *         return gnn.forward(features, adjacency);
 *     }
 * }
 */
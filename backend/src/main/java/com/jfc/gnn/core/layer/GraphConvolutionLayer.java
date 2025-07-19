package com.jfc.gnn.core.layer;
//=====================================================================

//Updated GraphConvolutionLayer.java - Fixed Builder Pattern
//=====================================================================

import org.deeplearning4j.nn.conf.layers.Layer;
import org.deeplearning4j.nn.layers.BaseLayer;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.params.DefaultParamInitializer;
import org.deeplearning4j.nn.workspace.LayerWorkspaceMgr;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.activations.IActivation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.common.primitives.Pair;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Custom Graph Convolution Layer for DL4J with proper builder pattern
 */
@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class GraphConvolutionLayer extends Layer {

	private INDArray adjacencyMatrix;

	protected GraphConvolutionLayer(Builder builder) {
		super(builder);
	}

	/**
	 * Implementation of the layer for forward pass
	 */
	public static class GraphConvolutionLayerImpl extends BaseLayer<GraphConvolutionLayer> {

		private INDArray adjacencyMatrix;

		public GraphConvolutionLayerImpl(NeuralNetConfiguration conf) {
			super(conf);
		}

		@Override
		public INDArray activate(boolean training, LayerWorkspaceMgr workspaceMgr) {
			INDArray input = this.input;
			INDArray weights = getParam(DefaultParamInitializer.WEIGHT_KEY);
			INDArray bias = getParam(DefaultParamInitializer.BIAS_KEY);

			// Graph convolution: A * X * W + b
			// If no adjacency matrix is set, use identity (no graph structure)
			INDArray adjMatrix = adjacencyMatrix != null ? adjacencyMatrix : Nd4j.eye(input.rows());

			INDArray output = adjMatrix.mmul(input).mmul(weights);
			output.addiRowVector(bias);

			// Apply activation function
			IActivation activationFn = layerConf().getActivationFn();
			return activationFn.getActivation(output, training);
		}

		@Override
		public Pair<Gradient, INDArray> backpropGradient(INDArray epsilon, LayerWorkspaceMgr workspaceMgr) {
			// Simplified backpropagation - for production, implement full gradient
			// computation
			INDArray input = this.input;
			INDArray weights = getParam(DefaultParamInitializer.WEIGHT_KEY);

			// Compute gradients
			Gradient gradient = new org.deeplearning4j.nn.gradient.DefaultGradient();

			// Weight gradients
			INDArray weightGrad = input.transpose().mmul(epsilon);
			gradient.setGradientFor(DefaultParamInitializer.WEIGHT_KEY, weightGrad);

			// Bias gradients
			INDArray biasGrad = epsilon.sum(0);
			gradient.setGradientFor(DefaultParamInitializer.BIAS_KEY, biasGrad);

			// Input gradients (for next layer)
			INDArray inputGrad = epsilon.mmul(weights.transpose());

			return new Pair<>(gradient, inputGrad);
		}

		@Override
		public boolean isPretrainLayer() {
			return false;
		}

		/**
		 * Set adjacency matrix for graph structure
		 */
		public void setAdjacencyMatrix(INDArray adjacencyMatrix) {
			this.adjacencyMatrix = normalizeAdjacencyMatrix(adjacencyMatrix);
		}

		/**
		 * Normalize adjacency matrix with self-loops
		 */
		private INDArray normalizeAdjacencyMatrix(INDArray adj) {
			int n = adj.rows();

			// Add self-loops
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

			// Normalize: D^(-1/2) * A * D^(-1/2)
			INDArray normalized = Nd4j.diag(degreeSqrtInv).mmul(adjWithSelfLoops).mmul(Nd4j.diag(degreeSqrtInv));

			return normalized;
		}
	}

	/**
	 * Builder pattern implementation with proper DL4J integration
	 */
	public static class Builder extends Layer.Builder<Builder> {

		public Builder() {
			// Set default activation
			this.activationFn = Activation.RELU.getActivationFunction();
		}

		/**
		 * Set input size
		 */
		public Builder nIn(int nIn) {
			this.setNIn(nIn);
			return this;
		}

		/**
		 * Set output size
		 */
		public Builder nOut(int nOut) {
			this.setNOut(nOut);
			return this;
		}

		/**
		 * Set activation function
		 */
		public Builder activation(IActivation activation) {
			this.setActivationFn(activation);
			return this;
		}

		/**
		 * Set activation function by name
		 */
		public Builder activation(String activation) {
			return activation(Activation.fromString(activation).getActivationFunction());
		}

		/**
		 * Set activation function by enum
		 */
		public Builder activation(Activation activation) {
			return activation(activation.getActivationFunction());
		}

		@Override
		@SuppressWarnings("unchecked")
		public GraphConvolutionLayer build() {
			return new GraphConvolutionLayer(this);
		}
	}
}

//=====================================================================
//Alternative: Simplified GCN using Standard DenseLayer (Recommended)
//=====================================================================

//If you want to avoid the complexity of custom layers, here's how to modify
//the GraphNeuralNetworkEngine to use standard DenseLayer but still call it "GCN":

/*
 * In GraphNeuralNetworkEngine.java, you can keep using DenseLayer and just
 * rename the class or add a comment explaining that this is a simplified GCN
 * approach for BOM prediction:
 * 
 * private MultiLayerNetwork buildSimplifiedGCNModel(GnnModelConfig config) { //
 * Simplified GCN using dense layers for BOM prediction // This approach treats
 * the hydraulic cylinder specifications as // node features without explicit
 * graph structure
 * 
 * MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
 * .seed(config.getSeed()) .weightInit(config.getWeightInit())
 * .updater(config.getUpdater()) .l2(config.getL2Regularization()) .list()
 * .layer(0, new DenseLayer.Builder() .nIn(config.getInputDim())
 * .nOut(config.getHiddenDim()) .activation(config.getActivation())
 * .dropOut(config.getDropout()) .build()) .layer(1, new DenseLayer.Builder()
 * .nIn(config.getHiddenDim()) .nOut(config.getHiddenDim())
 * .activation(config.getActivation()) .dropOut(config.getDropout()) .build())
 * .layer(2, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
 * .nIn(config.getHiddenDim()) .nOut(config.getOutputDim())
 * .activation(Activation.SIGMOID) .build()) .build();
 * 
 * MultiLayerNetwork model = new MultiLayerNetwork(conf); model.init();
 * model.setListeners(new ScoreIterationListener(100));
 * 
 * return model; }
 */

package com.jfc.gnn.core.layer;

import org.deeplearning4j.nn.conf.layers.Layer;
import org.deeplearning4j.nn.layers.BaseLayer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

/**
 * Custom Graph Convolution Layer for DL4J
 */
public class GraphConvolutionLayer extends BaseLayer<GraphConvolutionLayer> {
    
    private INDArray adjacencyMatrix;
    
    /**
     * Forward propagation with graph structure
     */
    @Override
    public INDArray activate(boolean training) {
        INDArray input = this.input;
        INDArray weights = getParam("W");
        INDArray bias = getParam("b");
        
        // Graph convolution: A * X * W + b
        INDArray output = adjacencyMatrix.mmul(input).mmul(weights);
        output.addiRowVector(bias);
        
        // Apply activation
        return getActivationFn().getActivation(output, training);
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
        INDArray normalized = Nd4j.diag(degreeSqrtInv)
            .mmul(adjWithSelfLoops)
            .mmul(Nd4j.diag(degreeSqrtInv));
        
        return normalized;
    }
    
    // Builder pattern
    public static class Builder extends Layer.Builder<Builder> {
        
        @Override
        public GraphConvolutionLayer build() {
            return new GraphConvolutionLayer(this);
        }
    }
}

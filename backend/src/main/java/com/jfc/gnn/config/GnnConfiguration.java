package com.jfc.gnn.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jfc.gnn.config.GnnModelConfig.ModelType;
import com.jfc.gnn.repository.FileBasedModelRepository;
import com.jfc.gnn.repository.ModelRepository;

import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class GnnConfiguration {
    
    @Value("${gnn.model.path}")
    private String modelPath;
    
    @Value("${gnn.embedding.dim:128}")
    private int embeddingDim;
    
    @Value("${gnn.hidden.dim:256}")
    private int hiddenDim;
    
    @Value("${gnn.num.heads:8}")
    private int numHeads;
    
    @Bean
    public GnnModelConfig defaultGnnConfig() {
        return GnnModelConfig.builder()
            .modelType(ModelType.GCN)
            .inputDim(embeddingDim)
            .hiddenDim(hiddenDim)
            .outputDim(embeddingDim)
            .numHeads(numHeads)
            .dropout(0.5)
            .learningRate(0.001)
            .weightInit(WeightInit.XAVIER)
            .activation(Activation.RELU)
            .build();
    }
    
    @Bean
    public ModelRepository modelRepository() {
        return new FileBasedModelRepository(modelPath);
    }
}
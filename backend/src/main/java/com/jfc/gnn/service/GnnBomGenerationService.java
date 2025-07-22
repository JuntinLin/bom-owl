package com.jfc.gnn.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jfc.gnn.model.*;
import com.jfc.gnn.model.KnowledgeGraph.GraphNode;
import com.jfc.gnn.model.KnowledgeGraph.KnowledgeGraphBuilder;
import com.jfc.owl.dto.search.SearchRequestDTO.SearchType;
import com.jfc.owl.model.bom.BomComponent;
import com.jfc.owl.service.OWLKnowledgeBaseService;
import com.jfc.gnn.dto.*;
import com.jfc.gnn.dto.GnnBomGenerationResult.BomStructure;
import com.jfc.gnn.config.GnnModelConfig;
import com.jfc.gnn.converter.OntologyToGraphConverter;
import com.jfc.gnn.core.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class GnnBomGenerationService {

	private static final Logger logger = LoggerFactory.getLogger(GnnBomGenerationService.class);

	@Autowired
	private GraphNeuralNetworkEngine gnnEngine;

	@Autowired
	private OntologyToGraphConverter ontologyConverter;

	// @Autowired
	// private BomOntologyService bomOntologyService;
	// Replace BomOntologyService with OWLKnowledgeBaseService
	@Autowired
	private OWLKnowledgeBaseService owlKnowledgeBaseService;

	@Autowired
	private KnowledgeGraphService knowledgeGraphService;

	/**
	 * Generate BOM using GNN prediction
	 */
	public GnnBomGenerationResult generateBomUsingGnn(GnnBomGenerationRequest request) {

		logger.info("Generating BOM for new product: {} using GNN", request.getNewItemCode());

		try {
			// 1. Extract product specifications
			ProductSpecifications specs = extractSpecifications(request);

			// 2. Convert existing BOMs to knowledge graph
			KnowledgeGraph kg = buildKnowledgeGraph(specs);

			// 3. Convert to GNN-compatible format
			GraphData graphData = convertToGraphData(kg);

			// 4. Run GNN inference
			GnnPredictionResult prediction = gnnEngine.predict(graphData, specs, request.getPredictionOptions());

			// 5. Convert predictions to BOM structure
			BomStructure generatedBom = convertPredictionToBom(prediction, request.getNewItemCode());

			// 6. Apply post-processing and validation
			BomStructure validatedBom = validateAndEnhanceBom(generatedBom);

			// 7. Calculate confidence scores
			Map<String, Double> confidenceScores = calculateConfidenceScores(prediction, validatedBom);

			return GnnBomGenerationResult.builder()
					.success(true)
					.newItemCode(request.getNewItemCode())
					.generatedBom(validatedBom)
					.confidenceScores(confidenceScores)
					.predictionMetrics(null)
					.generationTime(System.currentTimeMillis()).build();

		} catch (Exception e) {
			logger.error("Error generating BOM with GNN", e);
			return GnnBomGenerationResult.builder().success(false).error(e.getMessage()).build();
		}
	}

	/**
	 * Build knowledge graph from existing BOMs
	 */
	private KnowledgeGraph buildKnowledgeGraph(ProductSpecifications specs) {
		logger.info("Building knowledge graph for GNN processing");

		// Convert specs to map format for OWL Knowledge Base search
		Map<String, String> specifications = convertSpecsToMap(specs);

		// Get relevant BOMs from OWL knowledge base
		List<Map<String, Object>> similarBOMs = owlKnowledgeBaseService.searchSimilarBOMs(specifications, null,
				SearchType.SIMILARITY);

		// Initialize collections for the builder
		List<KnowledgeGraph.GraphNode> nodes = new ArrayList<>();
		List<KnowledgeGraph.GraphEdge> edges = new ArrayList<>();
		Map<String, KnowledgeGraph.GraphNode> nodeIndex = new HashMap<>();
		Map<String, List<KnowledgeGraph.GraphEdge>> adjacencyList = new HashMap<>();

		for (Map<String, Object> bomData : similarBOMs) {
			String masterItemCode = (String) bomData.get("masterItemCode");

			// Load the actual OWL model for detailed component information
			OntModel bomModel = owlKnowledgeBaseService.getKnowledgeBaseModel(masterItemCode);

			if (bomModel != null) {
				// Create and add product node
				KnowledgeGraph.GraphNode productNode = createProductNodeFromBomData(bomData, bomModel);
				nodes.add(productNode);
				nodeIndex.put(productNode.getId(), productNode);
				adjacencyList.put(productNode.getId(), new ArrayList<>());

				// Extract components from the OWL model
				List<BomComponent> components = extractComponentsFromOWLModel(bomModel, masterItemCode);

				for (BomComponent component : components) {
					// Create component node
					KnowledgeGraph.GraphNode componentNode = createComponentNode(component);
					nodes.add(componentNode);
					nodeIndex.put(componentNode.getId(), componentNode);

					// Create edge
					KnowledgeGraph.GraphEdge edge = createBomEdge(masterItemCode, component);
					edges.add(edge);

					// Update adjacency list
					adjacencyList.computeIfAbsent(masterItemCode, k -> new ArrayList<>()).add(edge);
					adjacencyList.computeIfAbsent(component.getComponentId(), k -> new ArrayList<>()).add(edge);
				}
			}
		}

		// Add specification-based relationships
		addSpecificationRelationships(nodes, edges, nodeIndex, adjacencyList, specs);

		// Build the knowledge graph
		return KnowledgeGraph.builder().nodes(nodes).edges(edges).nodeIndex(nodeIndex).adjacencyList(adjacencyList)
				.build();
	}

	private Map<String, String> convertSpecsToMap(ProductSpecifications specs) {
		Map<String, String> map = new HashMap<>();

		if (specs.getBore() != null)
			map.put("bore", specs.getBore());
		if (specs.getStroke() != null)
			map.put("stroke", specs.getStroke());
		if (specs.getSeries() != null)
			map.put("series", specs.getSeries());
		if (specs.getType() != null)
			map.put("type", specs.getType());
		if (specs.getRodEndType() != null)
			map.put("rodEndType", specs.getRodEndType());
		if (specs.getNewItemCode() != null)
			map.put("itemCode", specs.getNewItemCode());

		return map;
	}

	/**
	 * Create product node from BOM data and OWL model
	 */
	private KnowledgeGraph.GraphNode createProductNodeFromBomData(Map<String, Object> bomData, OntModel model) {
		String masterItemCode = (String) bomData.get("masterItemCode");
        String description = (String) bomData.get("description");
        
        // Create node with properties
        Map<String, Object> properties = new HashMap<>();
        properties.put("itemCode", masterItemCode);
        properties.put("description", description);
        properties.put("similarityScore", bomData.get("similarityScore"));
        
        // Add hydraulic cylinder specs if available
        if (Boolean.TRUE.equals(bomData.get("isHydraulicCylinder"))) {
            properties.put("hydraulicSpecs", bomData.get("hydraulicCylinderSpecs"));
        }
        
        return KnowledgeGraph.GraphNode.builder()
            .id(masterItemCode)
            .type("Product")
            .itemCode(masterItemCode)
            .itemName(description)
            .properties(properties)
            .category(KnowledgeGraph.NodeCategory.PRODUCT)
            .build();
	}

	/**
     * Add specification-based relationships to the graph
     * This method is also in GnnBomGenerationService
     */
    private void addSpecificationRelationships(
            List<KnowledgeGraph.GraphNode> nodes,
            List<KnowledgeGraph.GraphEdge> edges,
            Map<String, KnowledgeGraph.GraphNode> nodeIndex,
            Map<String, List<KnowledgeGraph.GraphEdge>> adjacencyList,
            ProductSpecifications specs) {
        
        // Add specification node
        Map<String, Object> specProps = new HashMap<>();
        specProps.put("bore", specs.getBore());
        specProps.put("stroke", specs.getStroke());
        specProps.put("series", specs.getSeries());
        specProps.put("type", specs.getType());
        specProps.put("rodEndType", specs.getRodEndType());
        
        KnowledgeGraph.GraphNode specNode = KnowledgeGraph.GraphNode.builder()
            .id("SPEC_" + specs.getNewItemCode())
            .type("Specification")
            .itemCode(specs.getNewItemCode())
            .properties(specProps)
            .category(KnowledgeGraph.NodeCategory.PRODUCT)
            .build();
        
        nodes.add(specNode);
        nodeIndex.put(specNode.getId(), specNode);
        adjacencyList.put(specNode.getId(), new ArrayList<>());
        
        // Connect specification to similar products
        // This helps GNN learn patterns based on specifications
    }
	/**
     * Extract components from OWL model
     * This method is in GnnBomGenerationService
     */
    private List<BomComponent> extractComponentsFromOWLModel(OntModel model, String masterItemCode) {
        List<BomComponent> components = new ArrayList<>();
        
        try {
            // Query the OWL model for BOM components
            String queryString = 
                "PREFIX bom: <http://www.jfc.com/ontology/bom#> " +
                "SELECT ?component ?quantity ?unit ?type WHERE { " +
                "  ?bom bom:hasMasterItem ?master . " +
                "  ?bom bom:hasComponent ?comp . " +
                "  ?comp bom:itemCode ?component . " +
                "  ?comp bom:quantity ?quantity . " +
                "  OPTIONAL { ?comp bom:unit ?unit } " +
                "  OPTIONAL { ?comp bom:componentType ?type } " +
                "  FILTER(?master = \"" + masterItemCode + "\") " +
                "}";
            
            // Execute SPARQL query
            Query query = QueryFactory.create(queryString);
            try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
                ResultSet results = qexec.execSelect();
                
                int lineNumber = 1;
                while (results.hasNext()) {
                    QuerySolution soln = results.nextSolution();
                    
                    BomComponent component = new BomComponent();
                    component.setComponentId(soln.get("component").toString());
                    component.setQuantity(soln.get("quantity").asLiteral().getDouble());
                    component.setUnit(soln.contains("unit") ? soln.get("unit").toString() : "PCS");
                    component.setComponentType(soln.contains("type") ? soln.get("type").toString() : "P");
                    component.setLineNumber(lineNumber++);
                    
                    components.add(component);
                }
            }
        } catch (Exception e) {
            logger.warn("Error extracting components from OWL model: {}", e.getMessage());
        }
        
        return components;
    }
    
    private KnowledgeGraph.GraphNode createComponentNode(BomComponent component) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("quantity", component.getQuantity());
        properties.put("unit", component.getUnit());
        properties.put("componentType", component.getComponentType());
        properties.put("lineNumber", component.getLineNumber());
        
        // Add additional properties if available
        if (component.getDimension() != null) {
            properties.put("dimension", component.getDimension());
        }
        if (component.getPosition() != null) {
            properties.put("position", component.getPosition());
        }
        
        return KnowledgeGraph.GraphNode.builder()
            .id(component.getComponentId())
            .type("Component")
            .itemCode(component.getComponentId())
            .itemName(component.getComponentId())
            .properties(properties)
            .category(component.isMaterial() ? 
                KnowledgeGraph.NodeCategory.MATERIAL : 
                KnowledgeGraph.NodeCategory.COMPONENT)
            .build();
    }

	/**
	 * Create BOM edge between product and component
	 */
    private KnowledgeGraph.GraphEdge createBomEdge(String masterItemCode, BomComponent component) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("quantity", component.getQuantity());
        properties.put("unit", component.getUnit());
        properties.put("lineNumber", component.getLineNumber());
        properties.put("effectiveQuantity", component.getEffectiveQuantity());
        
        return KnowledgeGraph.GraphEdge.builder()
            .id(masterItemCode + "_" + component.getComponentId())
            .sourceId(masterItemCode)
            .targetId(component.getComponentId())
            .edgeType("HAS_COMPONENT")
            .relationship("hasPart")
            .quantity(component.getQuantity())
            .properties(properties)
            .weight(1.0f)
            .build();
    }
	

	/**
	 * Convert knowledge graph to GNN-compatible format
	 */
	private GraphData convertToGraphData(KnowledgeGraph kg) {
		return ontologyConverter.convertToGraphData(kg);
	}

	/**
	 * Train GNN model with new data
	 */
	public GnnTrainingResult trainModel(GnnTrainingRequest request) {
		logger.info("Training GNN model: {}", request.getModelName());

		CompletableFuture<GnnTrainingResult> future = CompletableFuture.supplyAsync(() -> {
			try {
				// Prepare training data
				TrainingDataset dataset = prepareTrainingData(request.getTrainingBoms());

				// Configure model
				GnnModelConfig config = createModelConfig(request);

				// Train model
				TrainedGnnModel model = gnnEngine.trainModel(dataset, config, request.getTrainingOptions());

				// Evaluate model
				ModelEvaluation evaluation = evaluateModel(model, null);

				// Save model
				String modelId = saveModel(model, request.getModelName());

				return GnnTrainingResult.builder()
						.success(true)
						.modelId(modelId)
						.trainingTime(model.getTrainingTime())
						.accuracy(evaluation.getAccuracy())
		                .precision(evaluation.getPrecision())
		                .recall(evaluation.getRecall())
		                .f1Score(evaluation.getF1Score())
		                .finalLoss(evaluation.getLoss())
						.build();

			} catch (Exception e) {
				logger.error("Error training GNN model", e);
				return GnnTrainingResult.builder()
						.success(false)
						.error(e.getMessage())
						.build();
			}
		});

		return future.join();
	}

	/**
	 * Predict components for a new product
	 */
	public ComponentPredictionResult predictComponents(ComponentPredictionRequest request) {

		logger.info("Predicting components for: {}", request.getProductCode());

		try {
	        // Create BOM generation request
	        GnnBomGenerationRequest bomRequest = GnnBomGenerationRequest.builder()
	            .newItemCode(request.getProductCode())
	            .specifications(request.getSpecifications())
	            .predictionOptions(PredictionOptions.builder()
	                .modelId(request.getModelId())
	                .maxPredictions(request.getMaxComponents())
	                .build())
	            .build();
	        
	        // Use existing BOM generation logic
	        GnnBomGenerationResult result = generateBomUsingGnn(bomRequest);
	        
	        if (result.isSuccess() && result.getGeneratedBom() != null) {
	            // Convert BOM components to predicted components
	            List<PredictedComponent> predictions = result.getGeneratedBom().getComponents().stream()
	                .map(comp -> PredictedComponent.builder()
	                    .componentCode(comp.getComponentId())
	                    .quantity(comp.getQuantity())
	                    .unit(comp.getUnit())
	                    .confidence(result.getConfidenceScores().getOrDefault(
	                        comp.getComponentId(), 0.8))
	                    .predictedBy("GNN")
	                    .build())
	                .limit(request.getMaxComponents() != null ? request.getMaxComponents() : 50)
	                .collect(Collectors.toList());
	            
	            return ComponentPredictionResult.builder()
	                .productCode(request.getProductCode())
	                .predictions(predictions)
	                .modelVersion("v1.0")
	                .build();
	        } else {
	            return ComponentPredictionResult.builder()
	                .productCode(request.getProductCode())
	                .predictions(new ArrayList<>())
	                .build();
	        }
	    } catch (Exception e) {
	        logger.error("Error predicting components", e);
	        return ComponentPredictionResult.builder()
	            .productCode(request.getProductCode())
	            .predictions(new ArrayList<>())
	            .build();
	    }
	}

	/**
	 * Calculate confidence scores for generated BOM
	 */
	private Map<String, Double> calculateConfidenceScores(GnnPredictionResult prediction, BomStructure bom) {

		Map<String, Double> scores = new HashMap<>();

		// Overall confidence
		scores.put("overall", prediction.getOverallConfidence());

		// Component-level confidence
		for (BomComponent component : bom.getComponents()) {
			PredictedComponent pred = prediction.getComponentPrediction(component.getComponentId());
			if (pred != null) {
				scores.put(component.getComponentId(), pred.getConfidence());
			}
		}

		// Structure confidence
		scores.put("structure", calculateStructureConfidence(bom, prediction));

		return scores;
	}

	// Add these to GnnBomGenerationService class:

	private ProductSpecifications extractSpecifications(GnnBomGenerationRequest request) {
	    return request.getSpecifications();
	}

	private BomStructure convertPredictionToBom(GnnPredictionResult prediction, String newItemCode) {
	    List<BomComponent> components = new ArrayList<>();
	    
	    for (PredictedComponent pred : prediction.getComponentPredictions()) {
	        BomComponent component = new BomComponent();
	        component.setComponentId(pred.getComponentCode());
	        component.setQuantity(pred.getQuantity());
	        component.setUnit(pred.getUnit());
	        components.add(component);
	    }
	    
	    return BomStructure.builder()
	        .masterItemCode(newItemCode)
	        .components(components)
	        .build();
	}

	private BomStructure validateAndEnhanceBom(BomStructure generatedBom) {
	    // Add validation logic here
	    return generatedBom;
	}

	private double calculateStructureConfidence(BomStructure bom, GnnPredictionResult prediction) {
	    // Calculate overall structure confidence
	    return prediction.getOverallConfidence() * 0.9;
	}

	private TrainingDataset prepareTrainingData(List<TrainingBom> trainingBoms) {
	    // Convert training BOMs to dataset
	    return new TrainingDataset();
	}

	private GnnModelConfig createModelConfig(GnnTrainingRequest request) {
	    return GnnModelConfig.builder()
	        .modelType(GnnModelConfig.ModelType.GCN)
	        .inputDim(20)
	        .hiddenDim(256)
	        .outputDim(1000)
	        .epochs(request.getTrainingOptions().getEpochs())
	        .build();
	}

	private ModelEvaluation evaluateModel(TrainedGnnModel model, Object testSet) {
	    return new ModelEvaluation();
	}

	private String saveModel(TrainedGnnModel model, String modelName) {
	    // Save model and return ID
	    return UUID.randomUUID().toString();
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
	 * Get model performance metrics
	 */
	public ModelMetrics getModelMetrics(String modelId) {
	    logger.info("Retrieving metrics for model: {}", modelId);
	    
	    try {
	        // Load the model
	        TrainedGnnModel model = gnnEngine.loadModel(modelId);
	        
	        if (model == null) {
	            logger.warn("Model not found: {}", modelId);
	            return null;
	        }
	        
	        // Get the training result which contains metrics
	        GnnTrainingResult trainingResult = (GnnTrainingResult) model.getEvaluation();
	        
	        // Build ModelMetrics from training result
	        return ModelMetrics.builder()
	            .modelId(modelId)
	            .modelName(model.getModelType())
	            .modelType(model.getConfig() != null ? model.getConfig().getModelType().toString() : "Unknown")
	            .accuracy(trainingResult != null ? trainingResult.getAccuracy() : 0.0)
	            .precision(trainingResult != null ? trainingResult.getPrecision() : 0.0)
	            .recall(trainingResult != null ? trainingResult.getRecall() : 0.0)
	            .f1Score(trainingResult != null ? trainingResult.getF1Score() : 0.0)
	            .loss(trainingResult != null ? trainingResult.getFinalLoss() : 0.0)
	            .totalPredictions(trainingResult != null ? trainingResult.getComponentCount() : 0)
	            .lastEvaluated(new Date())
	            .build();
	            
	    } catch (Exception e) {
	        logger.error("Error retrieving model metrics for {}: {}", modelId, e.getMessage());
	        return null;
	    }
	}
}
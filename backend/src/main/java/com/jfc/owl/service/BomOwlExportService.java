package com.jfc.owl.service;

import java.util.*;
import java.io.File;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jfc.owl.TiptopToOwlConverter;
import com.jfc.owl.ontology.HydraulicCylinderOntology;
import com.jfc.rdb.tiptop.entity.BmaFile;
import com.jfc.rdb.tiptop.entity.BmbFile;
import com.jfc.rdb.tiptop.entity.ImaFile;
import com.jfc.rdb.tiptop.repository.BmaRepository;
import com.jfc.rdb.tiptop.repository.BmbRepository;
import com.jfc.rdb.tiptop.repository.ImaRepository;

//import com.jfc.tiptop.entity.*;
//import com.jfc.tiptop.repository.*;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class BomOwlExportService {
	private static final Logger logger = LoggerFactory.getLogger(BomOwlExportService.class);

	@Autowired
	private ImaRepository imaFileRepository;

	@Autowired
	private BmaRepository bmaFileRepository;

	@Autowired
	private BmbRepository bmbFileRepository;

	@Autowired
	private HydraulicCylinderOntology hydraulicCylinderOntology;

	/**
	 * Exports complete BOM structure to OWL ontology
	 * 
	 * @param outputPath Path to store the OWL file
	 * @param format     Format to export (RDF/XML, TURTLE, JSON-LD, etc.)
	 * @return Full path to the generated file
	 */
	@Transactional(readOnly = true)
	public String exportAllBomsToOwl(String outputPath, String format) {
		logger.info("Exporting all BOMs to OWL format: {}", format);

		TiptopToOwlConverter converter = new TiptopToOwlConverter();
		Map<String, Individual> materialMap = new HashMap<>();

		// First, convert all materials (ImaFile)
		// long totalItems = imaRepository.countByIma09AndIma10("S", "130 HC");
		// long masterItemsCount = bmaRepository.countByIma09AndIma10("S", "130 HC");
		// long componentItemsCount =
		// bmbRepository.countDistinctComponentsByMasterItemTypeAndLine("S", "130 HC");

		List<ImaFile> allMaterials = imaFileRepository.findByIma09AndIma10("S", "130 HC"); // imaFileRepository.findAll();
		for (ImaFile ima : allMaterials) {
			Individual material = converter.convertImaToMaterial(ima);
			materialMap.put(ima.getIma01(), material);
		}

		// Then, process all BOMs
		List<BmaFile> allBoms = bmaFileRepository.findByIma09AndIma10("S", "130 HC"); // findAll();
		for (BmaFile bma : allBoms) {
			List<BmbFile> bmbList = bmbFileRepository.findByBmaFile(bma);
			converter.convertBomStructure(bma, bmbList, materialMap);
		}

		// Export the ontology
		String filename = outputPath + File.separator + "tiptop_bom_ontology.owl";
		converter.exportOntology(filename, format);

		return filename;
	}

	/**
	 * Exports ALL BOM structures with their COMPLETE hierarchies to OWL ontology
	 * This processes every master item and its complete component tree
	 * 
	 * @param outputPath Path to store the OWL file
	 * @param format     Format to export (RDF/XML, TURTLE, JSON-LD, etc.)
	 * @return Full path to the generated file
	 */
	@Transactional(readOnly = true)
	public String exportAllCompleteHierarchyBomsToOwl(String outputPath, String format) {
		logger.info("Starting export of all BOMs with complete hierarchies");

		TiptopToOwlConverter converter = new TiptopToOwlConverter();
		Map<String, Individual> materialMap = new HashMap<>();
		Set<String> processedItems = new HashSet<>();

		// First, convert all materials (ImaFile) - this ensures all materials are
		// available in the materialMap
		logger.info("Converting all materials to OWL individuals");
		List<ImaFile> allMaterials = imaFileRepository.findByIma09AndIma10("S", "130 HC");
		for (ImaFile ima : allMaterials) {
			Individual material = converter.convertImaToMaterial(ima);
			materialMap.put(ima.getIma01(), material);
		}

		// Then, find all master items (items that have BOMs)
		logger.info("Finding all master items");
		List<BmaFile> allBoms = bmaFileRepository.findByIma09AndIma10("S", "130 HC");

		// Process each master item with its complete hierarchy
		logger.info("Processing complete hierarchies for {} master items", allBoms.size());
		for (BmaFile bma : allBoms) {
			String masterItemCode = bma.getId().getBma01();
			logger.debug("Processing master item: {}", masterItemCode);

			// Process the complete hierarchy for this master item
			processItemHierarchy(masterItemCode, converter, materialMap, processedItems);
		}

		// Create the output directory if it doesn't exist
		File directory = new File(outputPath);
		if (!directory.exists()) {
			directory.mkdirs();
		}

		// Export the ontology - this will also create the OBDA and properties files
		String filename = outputPath + File.separator + "tiptop_bom_all_complete_hierarchy.owl";
		converter.exportOntology(filename, format);

		logger.info("All BOMs exported successfully with complete hierarchies. Total items processed: {}",
				processedItems.size());
		logger.info("Generated OWL file: {}", filename);
		logger.info("Generated OBDA file: {}", filename.replaceAll("\\.owl$", ".obda"));
		logger.info("Generated properties file: {}", filename.replaceAll("\\.owl$", ".properties"));

		return filename;
	}

	/**
	 * Exports BOM structure for a specific master item to OWL ontology This method
	 * does not process the complete hierarchy
	 * 
	 * @param masterItemCode Master item code
	 * @param outputPath     Path to store the OWL file
	 * @param format         Format to export (RDF/XML, TURTLE, JSON-LD, etc.)
	 * @return Full path to the generated file
	 */
	@Transactional(readOnly = true)
	public String exportBomForMasterItem(String masterItemCode, String outputPath, String format) {
		logger.info("Exporting BOM for master item: {} to format: {}", masterItemCode, format);

		TiptopToOwlConverter converter = new TiptopToOwlConverter();
		Map<String, Individual> materialMap = new HashMap<>();

		// First, ensure master item exists
		ImaFile masterIma = imaFileRepository.findById(masterItemCode)
				.orElseThrow(() -> new IllegalArgumentException("Master item not found: " + masterItemCode));

		// Convert master item
		Individual masterMaterial = converter.convertImaToMaterial(masterIma);
		materialMap.put(masterIma.getIma01(), masterMaterial);

		// Find BmaFile entries for this master item
		List<BmaFile> bomHeaders = bmaFileRepository.findByIdBma01(masterItemCode);

		if (bomHeaders.isEmpty()) {
			throw new IllegalArgumentException("No BOM structure found for master item: " + masterItemCode);
		}

		// For each BOM header, process its components
		for (BmaFile bma : bomHeaders) {
			List<BmbFile> bmbList = bmbFileRepository.findByBmaFile(bma);

			// Fetch and convert all component materials
			for (BmbFile bmb : bmbList) {
				ImaFile componentIma = imaFileRepository.findById(bmb.getId().getBmb03()).orElse(null);

				if (componentIma != null) {
					Individual componentMaterial = converter.convertImaToMaterial(componentIma);
					materialMap.put(componentIma.getIma01(), componentMaterial);
				}
			}

			// Convert the BOM structure
			converter.convertBomStructure(bma, bmbList, materialMap);
		}

		// Export the ontology
		String filename = outputPath + File.separator + "tiptop_bom_" + masterItemCode + ".owl";
		converter.exportOntology(filename, format);

		logger.info("BOM for master item {} exported successfully to: {}", masterItemCode, filename);
		return filename;
	}

	/**
	 * Exports COMPLETE hierarchical BOM structure to OWL ontology, processing all
	 * sub-components at all levels
	 * 
	 * @param masterItemCode Master item code to start from
	 * @param outputPath     Path to store the OWL file
	 * @param format         Format to export (RDF/XML, TURTLE, JSON-LD, etc.)
	 * @return Full path to the generated file
	 * @throws IllegalArgumentException if master item not found
	 */
	@Transactional(readOnly = true)
	public String exportCompleteHierarchyBomToOwl(String masterItemCode, String outputPath, String format) {
		logger.info("Exporting complete BOM hierarchy for master item: {}", masterItemCode);

		TiptopToOwlConverter converter = new TiptopToOwlConverter();
		Map<String, Individual> materialMap = new HashMap<>();
		Set<String> processedItems = new HashSet<>();

		// First, ensure master item exists
		ImaFile masterIma = imaFileRepository.findById(masterItemCode)
				.orElseThrow(() -> new IllegalArgumentException("Master item not found: " + masterItemCode));

		// Convert master item
		Individual masterMaterial = converter.convertImaToMaterial(masterIma);
		materialMap.put(masterIma.getIma01(), masterMaterial);

		// Process the complete BOM hierarchy recursively
		processItemHierarchy(masterItemCode, converter, materialMap, processedItems);

		// Create the output directory if it doesn't exist
		File directory = new File(outputPath);
		if (!directory.exists()) {
			directory.mkdirs();
		}

		// Export the ontology - this will now also create the OBDA and properties files
		String filename = outputPath + File.separator + "tiptop_bom_" + masterItemCode + "_complete.owl";
		converter.exportOntology(filename, format);

		logger.info("BOM hierarchy exported successfully. Total items processed: {}", processedItems.size());
		logger.info("Generated OWL file: {}", filename);
		logger.info("Generated OBDA file: {}", filename.replaceAll("\\.owl$", ".obda"));
		logger.info("Generated properties file: {}", filename.replaceAll("\\.owl$", ".properties"));
		return filename;
	}

	/**
	 * Recursive method to process an item and all its sub-components
	 * 
	 * @param itemCode       Item code to process
	 * @param converter      The ontology converter
	 * @param materialMap    Map of materials to their Individual representations
	 * @param processedItems Set of already processed items to avoid circular
	 *                       references
	 */
	private void processItemHierarchy(String itemCode, TiptopToOwlConverter converter,
			Map<String, Individual> materialMap, Set<String> processedItems) {
		// Avoid processing the same item twice (circular reference protection)
		if (processedItems.contains(itemCode)) {
			logger.debug("Item already processed, skipping: {}", itemCode);
			return;
		}

		logger.debug("Processing item: {}", itemCode);
		processedItems.add(itemCode);

		// Find BmaFile entries for this item
		List<BmaFile> bomHeaders = bmaFileRepository.findByIdBma01(itemCode);

		if (bomHeaders.isEmpty()) {
			logger.debug("No BOM structure found for item: {}", itemCode);
			return; // This is a leaf component with no subcomponents
		}

		// For each BOM header, process its components
		for (BmaFile bma : bomHeaders) {
			List<BmbFile> bmbList = bmbFileRepository.findByBmaFile(bma);

			// Process each component
			for (BmbFile bmb : bmbList) {
				String componentCode = bmb.getId().getBmb03();

				// Fetch component material if needed
				if (!materialMap.containsKey(componentCode)) {
					ImaFile componentIma = imaFileRepository.findById(componentCode).orElse(null);

					if (componentIma != null) {
						Individual componentMaterial = converter.convertImaToMaterial(componentIma);
						materialMap.put(componentCode, componentMaterial);
					} else {
						logger.warn("Component not found in IMA: {}", componentCode);
						continue;
					}
				}

				// Recursively process this component's sub-components
				processItemHierarchy(componentCode, converter, materialMap, processedItems);
			}

			// Convert the BOM structure for this level
			converter.convertBomStructure(bma, bmbList, materialMap);
		}
	}

	/**
	 * Queries BOM structure for a specific master item and returns as OntModel This
	 * can be used by the REST controller to convert to JSON-LD or other formats
	 */
	@Transactional(readOnly = true)
	public OntModel getBomOntologyForMasterItem(String masterItemCode) {
		logger.debug("Getting BOM ontology for master item: {}", masterItemCode);

		TiptopToOwlConverter converter = new TiptopToOwlConverter();
		Map<String, Individual> materialMap = new HashMap<>();

		// Convert master item
		ImaFile masterIma = imaFileRepository.findById(masterItemCode)
				.orElseThrow(() -> new IllegalArgumentException("Master item not found: " + masterItemCode));

		Individual masterMaterial = converter.convertImaToMaterial(masterIma);
		materialMap.put(masterIma.getIma01(), masterMaterial);

		// Process BOM structure
		List<BmaFile> bomHeaders = bmaFileRepository.findByIdBma01(masterItemCode);

		for (BmaFile bma : bomHeaders) {
			List<BmbFile> bmbList = bmbFileRepository.findByBmaFile(bma);

			// Fetch component materials
			for (BmbFile bmb : bmbList) {
				ImaFile componentIma = imaFileRepository.findById(bmb.getId().getBmb03()).orElse(null);

				if (componentIma != null) {
					Individual componentMaterial = converter.convertImaToMaterial(componentIma);
					materialMap.put(componentIma.getIma01(), componentMaterial);
				}
			}

			converter.convertBomStructure(bma, bmbList, materialMap);
		}

		// Check if this is a hydraulic cylinder and enhance with domain knowledge
		OntModel baseModel = converter.getOntModel();
		if (isHydraulicCylinderItem(masterItemCode)) {
			return enhanceWithHydraulicCylinderKnowledge(baseModel, masterItemCode);
		}

		return baseModel;
	}

	/**
	 * Enhance the base BOM model with hydraulic cylinder domain knowledge
	 */
	private OntModel enhanceWithHydraulicCylinderKnowledge(OntModel baseModel, String masterItemCode) {
		try {
			logger.debug("Enhancing model with hydraulic cylinder knowledge for: {}", masterItemCode);

			// Initialize the hydraulic cylinder ontology
			hydraulicCylinderOntology.initializeHydraulicCylinderOntology();

			// Create enhanced model
			OntModel enhancedModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
			enhancedModel.add(baseModel);
			enhancedModel.add(hydraulicCylinderOntology.getOntologyModel());

			// Extract specifications and create hydraulic cylinder individual
			Map<String, String> specs = extractSpecificationsFromCode(masterItemCode);
			if (!specs.isEmpty()) {
				hydraulicCylinderOntology.createHydraulicCylinderIndividual(masterItemCode, specs);
			}

			logger.debug("Successfully enhanced model with hydraulic cylinder knowledge");
			return enhancedModel;

		} catch (Exception e) {
			logger.warn("Failed to enhance model with hydraulic cylinder knowledge: {}", e.getMessage());
			return baseModel; // Return base model if enhancement fails
		}
	}

	/**
	 * Extract hydraulic cylinder specifications from item code
	 */
	private Map<String, String> extractSpecificationsFromCode(String itemCode) {
		Map<String, String> specs = new HashMap<>();

		if (itemCode == null || itemCode.length() < 8) {
			return specs;
		}

		try {
			// Extract series (positions 3-4)
			if (itemCode.length() >= 4) {
				specs.put("series", itemCode.substring(2, 4));
			}

			// Extract bore (positions 6-8)
			if (itemCode.length() >= 8) {
				String boreStr = itemCode.substring(5, 8);
				specs.put("bore", String.valueOf(Integer.parseInt(boreStr)));
			}

			// Extract stroke (positions 11-14)
			if (itemCode.length() >= 14) {
				String strokeStr = itemCode.substring(10, 14);
				specs.put("stroke", String.valueOf(Integer.parseInt(strokeStr)));
			}

			// Extract rod end type (position 15)
			if (itemCode.length() >= 15) {
				specs.put("rodEndType", itemCode.substring(14, 15));
			}

		} catch (Exception e) {
			logger.warn("Error extracting specifications from code {}: {}", itemCode, e.getMessage());
		}

		return specs;
	}

	/**
	 * Check if an item is a hydraulic cylinder based on its code pattern
	 */
	private boolean isHydraulicCylinderItem(String itemCode) {
		return itemCode != null && itemCode.length() >= 2 && (itemCode.startsWith("3") || itemCode.startsWith("4"));
	}

	/**
	 * NEW METHOD: Get all master item codes from the database This method is
	 * required by OWLKnowledgeBaseService
	 * 
	 * @param ima09 Item type (e.g., "S" for standard type). If null or empty, all types are included
	 * @param ima10 Item category (e.g., "130 HC" for hydraulic cylinders). If null or empty, all categories are included
	 * @return List of all master item codes that have BOM structures
	 */
	@Transactional(readOnly = true)
	public List<String> getAllMasterItemCodes(String ima09, String ima10) {
		logger.info("Retrieving all master item codes from database with filters - ima09: {}, ima10: {}", ima09, ima10);
	    
		try {
			List<String> masterItemCodes;
			// Check if both parameters are empty/null
	        if ((ima09 == null || ima09.trim().isEmpty()) && (ima10 == null || ima10.trim().isEmpty())) {
	            // No filters - get all master item codes
	            logger.info("No filters provided, retrieving all master item codes");
	            masterItemCodes = bmaFileRepository.findDistinctMasterItemCodes();
	        } else {
	            // Apply filters
	            logger.info("Applying filters - ima09: {}, ima10: {}", ima09, ima10);
	            masterItemCodes = bmaFileRepository.findDistinctMasterItemCodesByType(ima09, ima10);
	        }
			// Filter to only include items of type "S" and item type "130 HC" if needed
			List<String> filteredCodes = new ArrayList<>();

			for (String masterItemCode : masterItemCodes) {
				try {
					ImaFile imaFile = imaFileRepository.findById(masterItemCode).orElse(null);
					if (imaFile != null) { // && "S".equals(imaFile.getIma09()) && "130 HC".equals(imaFile.getIma10())) {
						filteredCodes.add(masterItemCode);
					}
				} catch (Exception e) {
					logger.warn("Error checking master item {}: {}", masterItemCode, e.getMessage());
				}
			}

			logger.info("Found {} master item codes (filtered from {} total)", filteredCodes.size(),
					masterItemCodes.size());

			return filteredCodes;

		} catch (Exception e) {
			logger.error("Error retrieving master item codes", e);
			throw new RuntimeException("Failed to retrieve master item codes", e);
		}
	}

	/**
	 * NEW METHOD: Get master item codes with BOM statistics
	 * 
	 * @return Map containing master item codes and their BOM statistics
	 */
	@Transactional(readOnly = true)
	public Map<String, Object> getMasterItemStatistics() {
		logger.info("Generating master item statistics");

		Map<String, Object> statistics = new HashMap<>();

		try {
			List<String> allMasterCodes = getAllMasterItemCodes("S", "130 HC");
			Map<String, Integer> componentCounts = new HashMap<>();
			Map<String, Boolean> hasHierarchy = new HashMap<>();

			int totalMasterItems = allMasterCodes.size();
			int hydraulicCylinders = 0;
			int itemsWithHierarchy = 0;

			for (String masterCode : allMasterCodes) {
				try {
					// Count components for this master item
					List<BmaFile> bomHeaders = bmaFileRepository.findByIdBma01(masterCode);
					int componentCount = 0;
					boolean hasSubComponents = false;

					for (BmaFile bma : bomHeaders) {
						List<BmbFile> components = bmbFileRepository.findByBmaFile(bma);
						componentCount += components.size();

						// Check if any component has its own BOM (has hierarchy)
						for (BmbFile component : components) {
							List<BmaFile> subBoms = bmaFileRepository.findByIdBma01(component.getId().getBmb03());
							if (!subBoms.isEmpty()) {
								hasSubComponents = true;
							}
						}
					}

					componentCounts.put(masterCode, componentCount);
					hasHierarchy.put(masterCode, hasSubComponents);

					if (hasSubComponents) {
						itemsWithHierarchy++;
					}

					// Check if hydraulic cylinder
					if (isHydraulicCylinderItem(masterCode)) {
						hydraulicCylinders++;
					}

				} catch (Exception e) {
					logger.warn("Error processing statistics for master item {}: {}", masterCode, e.getMessage());
				}
			}

			statistics.put("totalMasterItems", totalMasterItems);
			statistics.put("hydraulicCylinders", hydraulicCylinders);
			statistics.put("itemsWithHierarchy", itemsWithHierarchy);
			statistics.put("componentCounts", componentCounts);
			statistics.put("hasHierarchy", hasHierarchy);

			// Calculate average components per master item
			double avgComponents = componentCounts.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);
			statistics.put("averageComponentsPerMaster", Math.round(avgComponents * 100.0) / 100.0);

			logger.info("Generated statistics for {} master items", totalMasterItems);

		} catch (Exception e) {
			logger.error("Error generating master item statistics", e);
			throw new RuntimeException("Failed to generate statistics", e);
		}

		return statistics;
	}

	/**
	 * NEW METHOD: Check if a master item exists and has BOM structure
	 * 
	 * @param masterItemCode The master item code to check
	 * @return true if the master item exists and has BOM structure
	 */
	@Transactional(readOnly = true)
	public boolean hasBomStructure(String masterItemCode) {
		try {
			// Check if master item exists
			Optional<ImaFile> masterIma = imaFileRepository.findById(masterItemCode);
			if (!masterIma.isPresent()) {
				return false;
			}

			// Check if it has BOM structure
			List<BmaFile> bomHeaders = bmaFileRepository.findByIdBma01(masterItemCode);
			return !bomHeaders.isEmpty();

		} catch (Exception e) {
			logger.warn("Error checking BOM structure for {}: {}", masterItemCode, e.getMessage());
			return false;
		}
	}

	/**
	 * NEW METHOD: Get BOM summary information for a master item
	 * 
	 * @param masterItemCode The master item code
	 * @return Map containing BOM summary information
	 */
	@Transactional(readOnly = true)
	public Map<String, Object> getBomSummary(String masterItemCode) {
		logger.debug("Getting BOM summary for master item: {}", masterItemCode);

		Map<String, Object> summary = new HashMap<>();

		try {
			// Get master item information
			ImaFile masterIma = imaFileRepository.findById(masterItemCode)
					.orElseThrow(() -> new IllegalArgumentException("Master item not found: " + masterItemCode));

			summary.put("masterItemCode", masterItemCode);
			summary.put("masterItemName", masterIma.getIma02());
			summary.put("masterItemSpec", masterIma.getIma021());
			summary.put("isHydraulicCylinder", isHydraulicCylinderItem(masterItemCode));

			// Get BOM structure information
			List<BmaFile> bomHeaders = bmaFileRepository.findByIdBma01(masterItemCode);
			summary.put("bomCount", bomHeaders.size());

			if (!bomHeaders.isEmpty()) {
				int totalComponents = 0;
				Set<String> uniqueComponents = new HashSet<>();
				List<Map<String, Object>> bomDetails = new ArrayList<>();

				for (BmaFile bma : bomHeaders) {
					List<BmbFile> components = bmbFileRepository.findByBmaFile(bma);
					totalComponents += components.size();

					Map<String, Object> bomDetail = new HashMap<>();
					bomDetail.put("characteristicCode", bma.getId().getBma06());
					bomDetail.put("componentCount", components.size());
					bomDetails.add(bomDetail);

					// Collect unique component codes
					for (BmbFile component : components) {
						uniqueComponents.add(component.getId().getBmb03());
					}
				}

				summary.put("totalComponents", totalComponents);
				summary.put("uniqueComponents", uniqueComponents.size());
				summary.put("bomDetails", bomDetails);

				// Check for hierarchical structure
				boolean hasHierarchy = uniqueComponents.stream()
						.anyMatch(componentCode -> !bmaFileRepository.findByIdBma01(componentCode).isEmpty());
				summary.put("hasHierarchy", hasHierarchy);

				// Add hydraulic cylinder specific information if applicable
				if (isHydraulicCylinderItem(masterItemCode)) {
					Map<String, String> specs = extractSpecificationsFromCode(masterItemCode);
					summary.put("hydraulicCylinderSpecs", specs);
				}
			}

		} catch (Exception e) {
			logger.error("Error getting BOM summary for {}: {}", masterItemCode, e.getMessage());
			throw new RuntimeException("Failed to get BOM summary", e);
		}

		return summary;
	}

	/**
	 * Builds a hierarchical in-memory BOM structure for a master item This is
	 * useful for generating tree-like JSON for frontend visualization
	 */
	@Transactional(readOnly = true)
	public BomTreeNode buildBomTree(String masterItemCode) {
		// Find master item
		ImaFile masterIma = imaFileRepository.findById(masterItemCode)
				.orElseThrow(() -> new IllegalArgumentException("Master item not found: " + masterItemCode));

		// Create root node
		BomTreeNode rootNode = new BomTreeNode();
		rootNode.setItemCode(masterIma.getIma01());
		rootNode.setItemName(masterIma.getIma02());
		rootNode.setItemSpec(masterIma.getIma021());

		// Find BmaFile entries for this master item
		List<BmaFile> bomHeaders = bmaFileRepository.findByIdBma01(masterItemCode);

		// Process each BOM header
		for (BmaFile bma : bomHeaders) {
			String characteristicCode = bma.getId().getBma06();

			// Find components for this BOM
			List<BmbFile> bmbList = bmbFileRepository.findByBmaFile(bma);

			for (BmbFile bmb : bmbList) {
				ImaFile componentIma = imaFileRepository.findById(bmb.getId().getBmb03()).orElse(null);

				if (componentIma != null) {
					BomTreeNode childNode = new BomTreeNode();
					childNode.setItemCode(componentIma.getIma01());
					childNode.setItemName(componentIma.getIma02());
					childNode.setItemSpec(componentIma.getIma021());
					childNode.setQuantity(bmb.getBmb06());
					childNode.setEffectiveDate(bmb.getId().getBmb04());
					childNode.setExpiryDate(bmb.getBmb05());
					childNode.setCharacteristicCode(bmb.getId().getBmb29());
					childNode.setParentCharacteristicCode(characteristicCode);

					// Recursively build the tree if this component has its own BOM
					List<BmaFile> childBoms = bmaFileRepository.findByIdBma01(componentIma.getIma01());
					if (!childBoms.isEmpty()) {
						// This component is also a master item with its own BOM
						BomTreeNode nestedTree = buildBomTree(componentIma.getIma01());
						childNode.setChildren(nestedTree.getChildren());
					}

					rootNode.addChild(childNode);
				}
			}
		}

		return rootNode;
	}

	/**
	 * Inner class for BOM tree representation
	 */
	public static class BomTreeNode {
		private String itemCode;
		private String itemName;
		private String itemSpec;
		private java.math.BigDecimal quantity;
		private java.util.Date effectiveDate;
		private java.util.Date expiryDate;
		private String characteristicCode;
		private String parentCharacteristicCode;
		private List<BomTreeNode> children = new ArrayList<>();

		public void addChild(BomTreeNode child) {
			this.children.add(child);
		}

		// Getters and setters
		public String getItemCode() {
			return itemCode;
		}

		public void setItemCode(String itemCode) {
			this.itemCode = itemCode;
		}

		public String getItemName() {
			return itemName;
		}

		public void setItemName(String itemName) {
			this.itemName = itemName;
		}

		public String getItemSpec() {
			return itemSpec;
		}

		public void setItemSpec(String itemSpec) {
			this.itemSpec = itemSpec;
		}

		public java.math.BigDecimal getQuantity() {
			return quantity;
		}

		public void setQuantity(java.math.BigDecimal quantity) {
			this.quantity = quantity;
		}

		public java.util.Date getEffectiveDate() {
			return effectiveDate;
		}

		public void setEffectiveDate(java.util.Date effectiveDate) {
			this.effectiveDate = effectiveDate;
		}

		public java.util.Date getExpiryDate() {
			return expiryDate;
		}

		public void setExpiryDate(java.util.Date expiryDate) {
			this.expiryDate = expiryDate;
		}

		public String getCharacteristicCode() {
			return characteristicCode;
		}

		public void setCharacteristicCode(String characteristicCode) {
			this.characteristicCode = characteristicCode;
		}

		public String getParentCharacteristicCode() {
			return parentCharacteristicCode;
		}

		public void setParentCharacteristicCode(String parentCharacteristicCode) {
			this.parentCharacteristicCode = parentCharacteristicCode;
		}

		public List<BomTreeNode> getChildren() {
			return children;
		}

		public void setChildren(List<BomTreeNode> children) {
			this.children = children;
		}
	}

}

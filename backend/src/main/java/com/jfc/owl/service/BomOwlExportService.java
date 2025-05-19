package com.jfc.owl.service;

import java.util.*;
import java.io.File;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jfc.owl.TiptopToOwlConverter;
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

	/**
	 * Exports complete BOM structure to OWL ontology
	 * 
	 * @param outputPath Path to store the OWL file
	 * @param format     Format to export (RDF/XML, TURTLE, JSON-LD, etc.)
	 * @return Full path to the generated file
	 */
	@Transactional(readOnly = true)
	public String exportAllBomsToOwl(String outputPath, String format) {
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
     * @param format Format to export (RDF/XML, TURTLE, JSON-LD, etc.)
     * @return Full path to the generated file
     */
    @Transactional(readOnly = true)
    public String exportAllCompleteHierarchyBomsToOwl(String outputPath, String format) {
        logger.info("Starting export of all BOMs with complete hierarchies");
        
        TiptopToOwlConverter converter = new TiptopToOwlConverter();
        Map<String, Individual> materialMap = new HashMap<>();
        Set<String> processedItems = new HashSet<>();
        
        // First, convert all materials (ImaFile) - this ensures all materials are available in the materialMap
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
        
        logger.info("All BOMs exported successfully with complete hierarchies. Total items processed: {}", processedItems.size());
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

		return converter.getOntModel();
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

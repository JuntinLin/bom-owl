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

@Service
public class BomOwlExportService {
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
     * @param format Format to export (RDF/XML, TURTLE, JSON-LD, etc.)
     * @return Full path to the generated file
      */
    @Transactional(readOnly = true)
    public String exportAllBomsToOwl(String outputPath, String format) {
        TiptopToOwlConverter converter = new TiptopToOwlConverter();
        Map<String, Individual> materialMap = new HashMap<>();
        
        // First, convert all materials (ImaFile)
        //long totalItems = imaRepository.countByIma09AndIma10("S", "130 HC");
        //long masterItemsCount = bmaRepository.countByIma09AndIma10("S", "130 HC");
        //long componentItemsCount = bmbRepository.countDistinctComponentsByMasterItemTypeAndLine("S", "130 HC");
        
        List<ImaFile> allMaterials = imaFileRepository.findByIma09AndIma10("S", "130 HC"); //imaFileRepository.findAll();
        for (ImaFile ima : allMaterials) {
            Individual material = converter.convertImaToMaterial(ima);
            materialMap.put(ima.getIma01(), material);
        }
        
        // Then, process all BOMs
        List<BmaFile> allBoms = bmaFileRepository.findByIma09AndIma10("S", "130 HC"); //findAll();
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
     * Exports BOM structure for a specific master item to OWL ontology
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
                ImaFile componentIma = imaFileRepository.findById(bmb.getId().getBmb03())
                        .orElse(null);
                
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
     * Queries BOM structure for a specific master item and returns as OntModel
     * This can be used by the REST controller to convert to JSON-LD or other formats
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
                ImaFile componentIma = imaFileRepository.findById(bmb.getId().getBmb03())
                        .orElse(null);
                
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
     * Builds a hierarchical in-memory BOM structure for a master item
     * This is useful for generating tree-like JSON for frontend visualization
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
                ImaFile componentIma = imaFileRepository.findById(bmb.getId().getBmb03())
                        .orElse(null);
                
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
        public String getItemCode() { return itemCode; }
        public void setItemCode(String itemCode) { this.itemCode = itemCode; }
        
        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }
        
        public String getItemSpec() { return itemSpec; }
        public void setItemSpec(String itemSpec) { this.itemSpec = itemSpec; }
        
        public java.math.BigDecimal getQuantity() { return quantity; }
        public void setQuantity(java.math.BigDecimal quantity) { this.quantity = quantity; }
        
        public java.util.Date getEffectiveDate() { return effectiveDate; }
        public void setEffectiveDate(java.util.Date effectiveDate) { this.effectiveDate = effectiveDate; }
        
        public java.util.Date getExpiryDate() { return expiryDate; }
        public void setExpiryDate(java.util.Date expiryDate) { this.expiryDate = expiryDate; }
        
        public String getCharacteristicCode() { return characteristicCode; }
        public void setCharacteristicCode(String characteristicCode) { this.characteristicCode = characteristicCode; }
        
        public String getParentCharacteristicCode() { return parentCharacteristicCode; }
        public void setParentCharacteristicCode(String parentCharacteristicCode) { 
            this.parentCharacteristicCode = parentCharacteristicCode; 
        }
        
        public List<BomTreeNode> getChildren() { return children; }
        public void setChildren(List<BomTreeNode> children) { this.children = children; }
    }
}

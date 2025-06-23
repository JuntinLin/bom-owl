package com.jfc.owl.service;
import com.jfc.owl.entity.OWLKnowledgeBase;
import org.apache.jena.ontology.OntModel;

import java.util.List;
import java.util.Map;

/**
 * Interface for OWL Knowledge Base Service
 * Defines the contract for managing OWL files as knowledge sources
 */
public interface OWLKnowledgeBaseService {
    
    /**
     * Initialize the knowledge base service
     */
    void initializeKnowledgeBase();
    
    /**
     * Export and save a BOM to the knowledge base
     * 
     * @param masterItemCode The master item code
     * @param format The export format (RDF/XML, TURTLE, JSON-LD, etc.)
     * @param includeHierarchy Whether to include the complete hierarchy
     * @param description Description of the export
     * @return The saved OWLKnowledgeBase entity
     */
    OWLKnowledgeBase exportAndSaveToKnowledgeBase(String masterItemCode, String format, 
                                                  Boolean includeHierarchy, String description);
    
    /**
     * Export all BOMs to knowledge base
     * 
     * @param format The export format
     * @param includeHierarchy Whether to include complete hierarchy
     * @return Export results including success/failure counts
     */
    Map<String, Object> exportAllBOMsToKnowledgeBase(String format, Boolean includeHierarchy);
    
    /**
     * Get knowledge base model for a specific item
     * 
     * @param masterItemCode The master item code
     * @return The OWL ontology model
     */
    OntModel getKnowledgeBaseModel(String masterItemCode);
    
    /**
     * Get the master knowledge base containing all models
     * 
     * @return The master OWL ontology model
     */
    OntModel getMasterKnowledgeBase();
    
    /**
     * Search for similar BOMs based on specifications
     * 
     * @param specifications The search specifications
     * @return List of similar BOMs with similarity scores
     */
    List<Map<String, Object>> searchSimilarBOMs(Map<String, String> specifications);
    
    /**
     * Search for similar hydraulic cylinders
     * 
     * @param specifications The hydraulic cylinder specifications
     * @return List of similar hydraulic cylinders
     */
    List<Map<String, Object>> searchSimilarHydraulicCylinders(Map<String, String> specifications);
    
    /**
     * Update knowledge base entry
     * 
     * @param masterItemCode The master item code
     * @param description New description
     * @return Updated OWLKnowledgeBase entity
     */
    OWLKnowledgeBase updateKnowledgeBaseEntry(String masterItemCode, String description);
    
    /**
     * Get knowledge base statistics
     * 
     * @return Statistics map
     */
    Map<String, Object> getKnowledgeBaseStatistics();
    
    /**
     * Clean up knowledge base
     * 
     * @return Cleanup results
     */
    Map<String, Object> cleanupKnowledgeBase();
    
    /**
     * Search knowledge base by keyword
     * 
     * @param keyword The search keyword
     * @return List of matching entries
     */
    List<OWLKnowledgeBase> searchKnowledgeBase(String keyword);
    
    /**
     * Get knowledge base model as string
     * 
     * @param masterItemCode The master item code
     * @param format The output format
     * @return The model as string
     */
    String getKnowledgeBaseModelAsString(String masterItemCode, String format);
    
    /**
     * Save a generated BOM to the knowledge base
     * 
     * @param newItemCode The new item code
     * @param generatedBOMModel The generated BOM model
     * @param description Description
     * @return The saved entry
     */
    OWLKnowledgeBase saveGeneratedBOMToKnowledgeBase(String newItemCode, OntModel generatedBOMModel, String description);
    
    /**
     * Validate a generated BOM
     * 
     * @param itemCode The item code
     * @param isValid Whether the BOM is valid
     * @param validationNotes Validation notes
     * @return The updated entry
     */
    OWLKnowledgeBase validateGeneratedBOM(String itemCode, boolean isValid, String validationNotes);
    
    /**
     * Find generated BOMs needing validation
     * 
     * @return List of BOMs needing validation
     */
    List<OWLKnowledgeBase> findGeneratedBOMsNeedingValidation();
    
    /**
     * Get statistics about generated BOMs
     * 
     * @return Generated BOM statistics
     */
    Map<String, Object> getGeneratedBOMStatistics();
    
    /**
     * Get knowledge base entries by criteria
     * 
     * @param criteria Search criteria
     * @return List of matching entries
     */
    List<OWLKnowledgeBase> getKnowledgeBaseEntriesByCriteria(Map<String, Object> criteria);
}
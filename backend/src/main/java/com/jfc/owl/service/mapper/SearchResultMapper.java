package com.jfc.owl.service.mapper;

import org.springframework.stereotype.Component;
import com.jfc.owl.entity.OWLKnowledgeBase;
import com.jfc.owl.dto.search.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mapper service to convert between entities and DTOs for search operations
 */
@Component
public class SearchResultMapper {
    
    private static final Logger logger = LoggerFactory.getLogger(SearchResultMapper.class);
    
    /**
     * Convert search results to DTO
     */
    public SearchResultDTO toSearchResultDTO(
            List<Map<String, Object>> results,
            Map<String, String> searchCriteria,
            long startTimeMs,
            SearchResultDTO.SearchConfiguration configuration) {
        
        LocalDateTime startTime = LocalDateTime.now().minusNanos(
            (System.currentTimeMillis() - startTimeMs) * 1_000_000);
        
        List<SimilarBOMDTO> bomDTOs = results.stream()
            .map(this::mapToSimilarBOMDTO)
            .filter(dto -> dto != null) // Filter out any mapping failures
            .collect(Collectors.toList());
        
        return SearchResultDTO.builder()
            .searchId(UUID.randomUUID().toString())
            .status(SearchResultDTO.SearchStatus.COMPLETED)
            .startTime(startTime)
            .endTime(LocalDateTime.now())
            .results(bomDTOs)
            .totalResults(bomDTOs.size())
            .searchCriteria(searchCriteria)
            .durationMs(System.currentTimeMillis() - startTimeMs)
            .itemsProcessed(results.size())
            .configuration(configuration)
            .build();
    }
    
    /**
     * Create search result DTO with error
     */
    public SearchResultDTO createErrorResult(
            String searchId,
            Map<String, String> searchCriteria,
            String error,
            String errorDetail) {
        
        return SearchResultDTO.builder()
            .searchId(searchId != null ? searchId : UUID.randomUUID().toString())
            .status(SearchResultDTO.SearchStatus.FAILED)
            .startTime(LocalDateTime.now())
            .endTime(LocalDateTime.now())
            .searchCriteria(searchCriteria)
            .error(error)
            .errorDetail(errorDetail)
            .totalResults(0)
            .build();
    }
    
    /**
     * Map individual result map to SimilarBOMDTO
     */
    private SimilarBOMDTO mapToSimilarBOMDTO(Map<String, Object> result) {
        try {
            SimilarBOMDTO.SimilarBOMDTOBuilder builder = SimilarBOMDTO.builder();
            
            // Safely extract values with null checks
            if (result.containsKey("masterItemCode")) {
                builder.masterItemCode((String) result.get("masterItemCode"));
            }
            
            if (result.containsKey("fileName")) {
                builder.fileName((String) result.get("fileName"));
            }
            
            if (result.containsKey("description")) {
                builder.description((String) result.get("description"));
            }
            
            if (result.containsKey("similarityScore")) {
                Object score = result.get("similarityScore");
                if (score instanceof Number) {
                    builder.similarityScore(((Number) score).doubleValue());
                }
            }
            
            if (result.containsKey("createdAt")) {
                Object createdAt = result.get("createdAt");
                if (createdAt instanceof LocalDateTime) {
                    builder.createdAt((LocalDateTime) createdAt);
                }
            }
            
            if (result.containsKey("tripleCount")) {
                Object count = result.get("tripleCount");
                if (count instanceof Number) {
                    builder.tripleCount(((Number) count).intValue());
                }
            }
            
            if (result.containsKey("isHydraulicCylinder")) {
                builder.isHydraulicCylinder((Boolean) result.get("isHydraulicCylinder"));
            }
            
            if (result.containsKey("hydraulicCylinderSpecs")) {
                builder.hydraulicCylinderSpecs((String) result.get("hydraulicCylinderSpecs"));
            }
            
            // Additional fields
            if (result.containsKey("sourceSystem")) {
                builder.sourceSystem((String) result.get("sourceSystem"));
            }
            
            if (result.containsKey("validationStatus")) {
                builder.validationStatus((String) result.get("validationStatus"));
            }
            
            if (result.containsKey("componentCount")) {
                Object count = result.get("componentCount");
                if (count instanceof Number) {
                    builder.componentCount(((Number) count).intValue());
                }
            }
            
            if (result.containsKey("qualityScore")) {
                Object score = result.get("qualityScore");
                if (score instanceof Number) {
                    builder.qualityScore(((Number) score).doubleValue());
                }
            }
            
            return builder.build();
            
        } catch (Exception e) {
            logger.error("Error mapping result to SimilarBOMDTO: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Convert OWLKnowledgeBase entity to SimilarBOMDTO with similarity score
     */
    public SimilarBOMDTO toSimilarBOMDTO(OWLKnowledgeBase entity, double similarityScore) {
        return SimilarBOMDTO.builder()
            .masterItemCode(entity.getMasterItemCode())
            .fileName(entity.getFileName())
            .description(entity.getDescription())
            .similarityScore(similarityScore)
            .createdAt(entity.getCreatedAt())
            .tripleCount(entity.getTripleCount())
            .isHydraulicCylinder(entity.getIsHydraulicCylinder())
            .hydraulicCylinderSpecs(entity.getHydraulicCylinderSpecs())
            .sourceSystem(entity.getSourceSystem())
            .validationStatus(entity.getValidationStatus())
            .componentCount(entity.getComponentCount())
            .qualityScore(entity.getQualityScore())
            .build();
    }
    
    /**
     * Create progress DTO
     */
    public SearchProgressDTO createProgressDTO(
            int totalItems, 
            int processedItems, 
            int foundMatches,
            long startTimeMs,
            String currentItem,
            SearchProgressDTO.ProcessingPhase phase) {
        
        long elapsedMs = System.currentTimeMillis() - startTimeMs;
        
        SearchProgressDTO progress = SearchProgressDTO.builder()
            .totalItems(totalItems)
            .processedItems(processedItems)
            .foundMatches(foundMatches)
            .currentItem(currentItem)
            .currentPhase(phase)
            .elapsedTimeMs(elapsedMs)
            .build();
        
        // Update calculated fields
        progress.updateProgress(processedItems, foundMatches);
        
        // Add system metrics if available
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        progress.setMemoryUsageMB((int) usedMemory);
        
        return progress;
    }
    
    /**
     * Create in-progress search result DTO
     */
    public SearchResultDTO createInProgressResult(
            String searchId,
            Map<String, String> searchCriteria,
            SearchProgressDTO progress,
            List<SimilarBOMDTO> partialResults) {
        
        return SearchResultDTO.builder()
            .searchId(searchId)
            .status(SearchResultDTO.SearchStatus.PROCESSING)
            .startTime(LocalDateTime.now().minusNanos(progress.getElapsedTimeMs() * 1_000_000))
            .searchCriteria(searchCriteria)
            .progress(progress)
            .results(partialResults)
            .totalResults(partialResults != null ? partialResults.size() : 0)
            .itemsProcessed(progress.getProcessedItems())
            .build();
    }
}
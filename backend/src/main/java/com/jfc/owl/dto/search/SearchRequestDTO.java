package com.jfc.owl.dto.search;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;

import java.util.Map;

/**
 * DTO for search request
 * Used to receive search parameters from the client
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchRequestDTO {
    
    /**
     * Search specifications (key-value pairs)
     * e.g., {"series": "12", "bore": "50", "stroke": "146"}
     */
    @NotNull(message = "Specifications are required")
    @NotEmpty(message = "Specifications cannot be empty")
    private Map<String, String> specifications;
    
    /**
     * Search options
     */
    @Valid
    private SearchOptions options;
    
    /**
     * Search type
     */
    private SearchType searchType;
    
    /**
     * Enum for search types
     */
    public enum SearchType {
        SIMILARITY("Similarity-based search"),
        EXACT("Exact match search"),
        FUZZY("Fuzzy search"),
        SEMANTIC("Semantic search using ontology");
        
        private final String description;
        
        SearchType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Search options configuration
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SearchOptions {
        
        /**
         * Maximum number of results to return
         */
        @Min(value = 1, message = "Max results must be at least 1")
        @Max(value = 100, message = "Max results cannot exceed 100")
        @Builder.Default
        private int maxResults = 20;
        
        /**
         * Minimum similarity score (0-1)
         */
        @DecimalMin(value = "0.0", message = "Min similarity score must be at least 0")
        @DecimalMax(value = "1.0", message = "Min similarity score cannot exceed 1")
        @Builder.Default
        private double minSimilarityScore = 0.3;
        
        /**
         * Include AI-generated BOMs in results
         */
        @Builder.Default
        private boolean includeGeneratedBOMs = false;
        
        /**
         * Only return validated BOMs
         */
        @Builder.Default
        private boolean onlyValidated = false;
        
        /**
         * Only search hydraulic cylinders
         */
        @Builder.Default
        private boolean onlyHydraulicCylinders = false;
        
        /**
         * Search timeout in seconds
         */
        @Min(value = 1, message = "Timeout must be at least 1 second")
        @Max(value = 300, message = "Timeout cannot exceed 300 seconds")
        @Builder.Default
        private int timeoutSeconds = 30;
        
        /**
         * Use cached results if available
         */
        @Builder.Default
        private boolean useCache = true;
        
        /**
         * Include deleted/inactive items
         */
        @Builder.Default
        private boolean includeInactive = false;
        
        /**
         * Sort order for results
         */
        @Builder.Default
        private SortOrder sortOrder = SortOrder.SIMILARITY_DESC;
        
        /**
         * Filter by source system
         */
        private String sourceSystemFilter;
        
        /**
         * Filter by quality score
         */
        @DecimalMin(value = "0.0", message = "Min quality score must be at least 0")
        @DecimalMax(value = "1.0", message = "Min quality score cannot exceed 1")
        private Double minQualityScore;
        
        /**
         * Include detailed specifications in results
         */
        @Builder.Default
        private boolean includeDetailedSpecs = true;
        
        /**
         * Page number for pagination (1-based)
         */
        @Min(value = 1, message = "Page number must be at least 1")
        @Builder.Default
        private int page = 1;
        
        /**
         * Page size for pagination
         */
        @Min(value = 1, message = "Page size must be at least 1")
        @Max(value = 100, message = "Page size cannot exceed 100")
        @Builder.Default
        private int pageSize = 20;
    }
    
    /**
     * Enum for sort orders
     */
    public enum SortOrder {
        SIMILARITY_DESC("Similarity score descending"),
        SIMILARITY_ASC("Similarity score ascending"),
        CREATED_DESC("Creation date descending"),
        CREATED_ASC("Creation date ascending"),
        QUALITY_DESC("Quality score descending"),
        QUALITY_ASC("Quality score ascending"),
        USAGE_DESC("Usage count descending"),
        USAGE_ASC("Usage count ascending");
        
        private final String description;
        
        SortOrder(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Validate the request
     */
    public boolean isValid() {
        return specifications != null && !specifications.isEmpty();
    }
    
    /**
     * Get effective search options (with defaults if not provided)
     */
    public SearchOptions getEffectiveOptions() {
        return options != null ? options : SearchOptions.builder().build();
    }
    
    /**
     * Get effective search type
     */
    public SearchType getEffectiveSearchType() {
        return searchType != null ? searchType : SearchType.SIMILARITY;
    }
}
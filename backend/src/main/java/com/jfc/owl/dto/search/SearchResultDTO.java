package com.jfc.owl.dto.search;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for search result with metadata
 * Used to transfer search results from service to controller/client
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResultDTO {
    
    /**
     * Unique identifier for this search operation
     */
    private String searchId;
    
    /**
     * Current status of the search
     */
    private SearchStatus status;
    
    /**
     * When the search started
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;
    
    /**
     * When the search completed (null if still processing)
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;
    
    /**
     * List of similar BOMs found
     */
    private List<SimilarBOMDTO> results;
    
    /**
     * Total number of results found
     */
    private int totalResults;
    
    /**
     * Error message if search failed
     */
    private String error;
    
    /**
     * Detailed error information for debugging
     */
    private String errorDetail;
    
    /**
     * Progress information for long-running searches
     */
    private SearchProgressDTO progress;
    
    /**
     * Original search criteria/specifications
     */
    private Map<String, String> searchCriteria;
    
    /**
     * Search duration in milliseconds
     */
    private long durationMs;
    
    /**
     * Number of items processed during search
     */
    private int itemsProcessed;
    
    /**
     * Number of items that timed out during processing
     */
    private int timeoutCount;
    
    /**
     * Search configuration used
     */
    private SearchConfiguration configuration;
    
    /**
     * Enum for search status
     */
    public enum SearchStatus {
        PENDING("Search is queued"),
        PROCESSING("Search is in progress"),
        COMPLETED("Search completed successfully"),
        FAILED("Search failed with error"),
        CANCELLED("Search was cancelled"),
        PARTIAL("Search completed with some failures");
        
        private final String description;
        
        SearchStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Calculate duration if start and end times are set
     */
    public long calculateDuration() {
        if (startTime != null && endTime != null) {
            return java.time.Duration.between(startTime, endTime).toMillis();
        }
        return durationMs;
    }
    
    /**
     * Check if search is complete
     */
    public boolean isComplete() {
        return status == SearchStatus.COMPLETED || 
               status == SearchStatus.FAILED || 
               status == SearchStatus.CANCELLED;
    }
    
    /**
     * Check if search was successful
     */
    public boolean isSuccessful() {
        return status == SearchStatus.COMPLETED && error == null;
    }
    
    /**
     * Get success rate as percentage
     */
    public double getSuccessRate() {
        if (itemsProcessed == 0) return 0.0;
        return ((double) (itemsProcessed - timeoutCount) / itemsProcessed) * 100.0;
    }
    
    /**
     * Inner class for search configuration
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchConfiguration {
        @Builder.Default
        private int maxResults = 20;
        
        @Builder.Default
        private double minSimilarityScore = 0.3;
        
        @Builder.Default
        private int timeoutSeconds = 30;
        
        @Builder.Default
        private boolean useCache = true;
        
        private String searchAlgorithm;
        
        private Map<String, Object> additionalParams;
    }
}
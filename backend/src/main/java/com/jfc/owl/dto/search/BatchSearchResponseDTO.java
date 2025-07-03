package com.jfc.owl.dto.search;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jfc.owl.dto.search.BatchSearchResponseDTO.BatchSearchResult;
import com.jfc.owl.dto.search.BatchSearchResponseDTO.BatchSearchSummary;
import com.jfc.owl.dto.search.BatchSearchResponseDTO.BatchStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for batch search response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchSearchResponseDTO {
    
    /**
     * Batch search ID
     */
    private String batchId;
    
    /**
     * Batch status
     */
    private BatchStatus status;
    
    /**
     * When the batch started
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;
    
    /**
     * When the batch completed
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;
    
    /**
     * Individual search results
     */
    private List<BatchSearchResult> results;
    
    /**
     * Summary statistics
     */
    private BatchSearchSummary summary;
    
    /**
     * Processing duration in milliseconds
     */
    private long durationMs;
    
    /**
     * Batch status enum
     */
    public enum BatchStatus {
        QUEUED,
        PROCESSING,
        COMPLETED,
        FAILED,
        PARTIAL_SUCCESS,
        CANCELLED
    }
    
    /**
     * Individual search result within batch
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BatchSearchResult {
        private String itemId;
        private SearchResultDTO searchResult;
        private String error;
        private long processingTimeMs;
        private List<String> tags;
    }
    
    /**
     * Batch search summary
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BatchSearchSummary {
        private int totalSearches;
        private int successfulSearches;
        private int failedSearches;
        private int totalResultsFound;
        private double averageProcessingTimeMs;
        private double averageSimilarityScore;
        private Map<String, Integer> resultsByTag;
    }
}


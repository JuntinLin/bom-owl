package com.jfc.owl.dto.search;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jfc.owl.dto.search.BatchSearchStatusDTO.BatchProgress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for batch search status/progress
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchSearchStatusDTO {
    
    /**
     * Batch ID
     */
    private String batchId;
    
    /**
     * Current status
     */
    private BatchSearchResponseDTO.BatchStatus status;
    
    /**
     * Progress information
     */
    private BatchProgress progress;
    
    /**
     * Partial results (if available)
     */
    private List<BatchSearchResponseDTO.BatchSearchResult> completedResults;
    
    /**
     * Estimated completion time
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime estimatedCompletionTime;
    
    /**
     * Batch progress details
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BatchProgress {
        private int totalItems;
        private int processedItems;
        private int successfulItems;
        private int failedItems;
        private double percentComplete;
        private String currentItem;
        private long elapsedTimeMs;
        private long estimatedRemainingMs;
    }
}
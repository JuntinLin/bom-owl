package com.jfc.owl.dto.search;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * DTO for tracking search progress
 * Used to provide real-time updates on search operations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchProgressDTO {
    
    /**
     * Total number of items to process
     */
    private int totalItems;
    
    /**
     * Number of items already processed
     */
    private int processedItems;
    
    /**
     * Number of matches found so far
     */
    private int foundMatches;
    
    /**
     * Percentage of completion (0-100)
     */
    private double percentComplete;
    
    /**
     * Currently processing item identifier
     */
    private String currentItem;
    
    /**
     * Current processing phase
     */
    private ProcessingPhase currentPhase;
    
    /**
     * Elapsed time in milliseconds
     */
    private long elapsedTimeMs;
    
    /**
     * Estimated remaining time in milliseconds
     */
    private long estimatedRemainingMs;
    
    /**
     * Estimated completion time
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime estimatedCompletionTime;
    
    /**
     * Average processing time per item in milliseconds
     */
    private double averageTimePerItem;
    
    /**
     * Current processing speed (items per second)
     */
    private double processingSpeed;
    
    /**
     * Memory usage in MB
     */
    private int memoryUsageMB;
    
    /**
     * CPU usage percentage
     */
    private double cpuUsagePercent;
    
    /**
     * Any warning messages during processing
     */
    private String warningMessage;
    
    /**
     * Processing phase enumeration
     */
    public enum ProcessingPhase {
        INITIALIZING("Initializing search"),
        FILTERING("Filtering candidates"),
        CALCULATING("Calculating similarities"),
        SORTING("Sorting results"),
        FINALIZING("Finalizing results");
        
        private final String description;
        
        ProcessingPhase(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Calculate percent complete based on processed items
     */
    public double calculatePercentComplete() {
        if (totalItems == 0) return 0.0;
        return Math.min(100.0, (double) processedItems / totalItems * 100.0);
    }
    
    /**
     * Update progress with new processed count
     */
    public void updateProgress(int newProcessedItems, int newFoundMatches) {
        this.processedItems = newProcessedItems;
        this.foundMatches = newFoundMatches;
        this.percentComplete = calculatePercentComplete();
        
        // Calculate processing speed
        if (elapsedTimeMs > 0) {
            this.processingSpeed = (double) processedItems / (elapsedTimeMs / 1000.0);
            this.averageTimePerItem = (double) elapsedTimeMs / processedItems;
            
            // Estimate remaining time
            if (processedItems < totalItems) {
                int remainingItems = totalItems - processedItems;
                this.estimatedRemainingMs = (long) (averageTimePerItem * remainingItems);
                this.estimatedCompletionTime = LocalDateTime.now()
                    .plusNanos(estimatedRemainingMs * 1_000_000);
            }
        }
    }
    
    /**
     * Check if processing is complete
     */
    public boolean isComplete() {
        return processedItems >= totalItems;
    }
    
    /**
     * Get human-readable elapsed time
     */
    public String getElapsedTimeFormatted() {
        long seconds = elapsedTimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%d:%02d", minutes, seconds % 60);
        } else {
            return String.format("%d seconds", seconds);
        }
    }
    
    /**
     * Get human-readable estimated remaining time
     */
    public String getEstimatedRemainingFormatted() {
        if (estimatedRemainingMs <= 0) return "Calculating...";
        
        long seconds = estimatedRemainingMs / 1000;
        long minutes = seconds / 60;
        
        if (minutes > 0) {
            return String.format("~%d minutes", minutes);
        } else {
            return String.format("~%d seconds", seconds);
        }
    }
}
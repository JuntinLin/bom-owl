package com.jfc.owl.dto.search;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for batch search request
 * Allows searching for multiple items in a single request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchSearchRequestDTO {
    
    /**
     * List of search requests to process
     */
    @NotNull(message = "Search requests are required")
    @Size(min = 1, max = 50, message = "Batch size must be between 1 and 50")
    @Valid
    private List<SearchItem> searchItems;
    
    /**
     * Common search options for all items
     */
    @Valid
    private SearchRequestDTO.SearchOptions commonOptions;
    
    /**
     * Whether to process searches in parallel
     */
    @Builder.Default
    private boolean parallel = true;
    
    /**
     * Whether to continue on individual search failures
     */
    @Builder.Default
    private boolean continueOnError = true;
    
    /**
     * Individual search item
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SearchItem {
        
        /**
         * Unique identifier for this search item
         */
        @NotBlank(message = "Search item ID is required")
        private String itemId;
        
        /**
         * Search specifications
         */
        @NotNull(message = "Specifications are required")
        @NotEmpty(message = "Specifications cannot be empty")
        private Map<String, String> specifications;
        
        /**
         * Optional item-specific search options (overrides common options)
         */
        @Valid
        private SearchRequestDTO.SearchOptions specificOptions;
        
        /**
         * Optional tags for grouping results
         */
        private List<String> tags;
    }
}


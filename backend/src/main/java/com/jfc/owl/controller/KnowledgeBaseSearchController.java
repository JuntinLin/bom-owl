package com.jfc.owl.controller;

import com.jfc.owl.dto.search.*;
import com.jfc.owl.service.OWLKnowledgeBaseService;
import com.jfc.owl.service.cache.SimilarityCacheService;
import com.jfc.owl.service.mapper.SearchResultMapper;
import com.jfc.rdb.common.dto.AbstractDTOController;
import com.jfc.rdb.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * REST Controller for Knowledge Base Search Operations
 * Provides synchronous and asynchronous search endpoints with progress tracking
 */
@RestController
@RequestMapping("/knowledge-base-search")
@Validated
public class KnowledgeBaseSearchController extends AbstractDTOController<Object> {
    
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeBaseSearchController.class);
    
    @Autowired
    private OWLKnowledgeBaseService knowledgeBaseService;
    
    @Autowired
    private SearchResultMapper searchResultMapper;
    
    @Autowired
    private SimilarityCacheService cacheService;
    
    // Store for tracking search progress
    // In-memory progress tracking for searches
    private final Map<String, SearchProgressDTO> searchProgressTracker = new ConcurrentHashMap<>();
    
    // In-memory tracking for batch searches
    private final Map<String, BatchSearchStatusDTO> batchSearchTracker = new ConcurrentHashMap<>();
    
    /**
     * Synchronous similarity search endpoint
     */
    @PostMapping("/search-similar")
    public ResponseEntity<ApiResponse<SearchResultDTO>> searchSimilarBOMs(
            @Valid @RequestBody SearchRequestDTO searchRequest) {
        
        try {
            // Check cache first if enabled
            if (searchRequest.getEffectiveOptions().isUseCache()) {
                SearchResultDTO cachedResult = cacheService.getCachedSearchResults(searchRequest.getSpecifications());
                if (cachedResult != null) {
                    return success(cachedResult);
                }
            }
            
            long startTime = System.currentTimeMillis();
            List<Map<String, Object>> results = knowledgeBaseService.searchSimilarBOMs(
                searchRequest.getSpecifications()
            );
            
            SearchResultDTO.SearchConfiguration config = SearchResultDTO.SearchConfiguration.builder()
                .maxResults(searchRequest.getEffectiveOptions().getMaxResults())
                .minSimilarityScore(searchRequest.getEffectiveOptions().getMinSimilarityScore())
                .timeoutSeconds(searchRequest.getEffectiveOptions().getTimeoutSeconds())
                .useCache(searchRequest.getEffectiveOptions().isUseCache())
                .searchAlgorithm(searchRequest.getEffectiveSearchType().name())
                .build();
            
            SearchResultDTO resultDTO = searchResultMapper.toSearchResultDTO(
                results, 
                searchRequest.getSpecifications(), 
                startTime,
                config
            );
            
            // Cache the results
            if (searchRequest.getEffectiveOptions().isUseCache()) {
                cacheService.cacheSearchResults(searchRequest.getSpecifications(), resultDTO);
            }
            
            return success(resultDTO);
            
        } catch (Exception e) {
            SearchResultDTO errorResult = searchResultMapper.createErrorResult(
                UUID.randomUUID().toString(),
                searchRequest.getSpecifications(),
                "Search failed",
                e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, errorResult, e.getMessage(), "500"));
        }
    }
    
    /**
     * Asynchronous search endpoint
     */
    @PostMapping("/search-similar-async")
    public DeferredResult<ResponseEntity<ApiResponse<SearchResultDTO>>> searchSimilarBOMsAsync(
            @Valid @RequestBody SearchRequestDTO searchRequest) {
        
        long timeoutMs = searchRequest.getEffectiveOptions().getTimeoutSeconds() * 1000L;
        DeferredResult<ResponseEntity<ApiResponse<SearchResultDTO>>> deferredResult = 
            new DeferredResult<>(timeoutMs);
        
        String searchId = UUID.randomUUID().toString();
        
        // Initialize progress tracking
        SearchProgressDTO progress = SearchProgressDTO.builder()
            .totalItems(0) // Will be updated during search
            .processedItems(0)
            .foundMatches(0)
            .percentComplete(0.0)
            .currentPhase(SearchProgressDTO.ProcessingPhase.INITIALIZING)
            .elapsedTimeMs(0)
            .build();
        searchProgressTracker.put(searchId, progress);
        
        // Execute search asynchronously
        CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                // Update progress
                progress.setCurrentPhase(SearchProgressDTO.ProcessingPhase.FILTERING);
                
                // Check cache first
                if (searchRequest.getEffectiveOptions().isUseCache()) {
                    SearchResultDTO cachedResult = cacheService.getCachedSearchResults(
                        searchRequest.getSpecifications()
                    );
                    if (cachedResult != null) {
                        cachedResult.setSearchId(searchId);
                        return cachedResult;
                    }
                }
                
                // Perform actual search
                progress.setCurrentPhase(SearchProgressDTO.ProcessingPhase.CALCULATING);
                List<Map<String, Object>> results = knowledgeBaseService.searchSimilarBOMs(
                    searchRequest.getSpecifications()
                );
                
                // Update progress
                progress.setCurrentPhase(SearchProgressDTO.ProcessingPhase.SORTING);
                progress.setTotalItems(results.size());
                progress.setProcessedItems(results.size());
                progress.setFoundMatches(results.size());
                progress.setPercentComplete(100.0);
                
                // Create configuration
                SearchResultDTO.SearchConfiguration config = SearchResultDTO.SearchConfiguration.builder()
                    .maxResults(searchRequest.getEffectiveOptions().getMaxResults())
                    .minSimilarityScore(searchRequest.getEffectiveOptions().getMinSimilarityScore())
                    .timeoutSeconds(searchRequest.getEffectiveOptions().getTimeoutSeconds())
                    .useCache(searchRequest.getEffectiveOptions().isUseCache())
                    .searchAlgorithm(searchRequest.getEffectiveSearchType().name())
                    .build();
                
                // Map results
                SearchResultDTO resultDTO = searchResultMapper.toSearchResultDTO(
                    results, 
                    searchRequest.getSpecifications(), 
                    startTime,
                    config
                );
                resultDTO.setSearchId(searchId);
                resultDTO.setProgress(progress);
                
                // Cache results if enabled
                if (searchRequest.getEffectiveOptions().isUseCache()) {
                    cacheService.cacheSearchResults(searchRequest.getSpecifications(), resultDTO);
                }
                
                return resultDTO;
                
            } catch (Exception e) {
                progress.setCurrentPhase(SearchProgressDTO.ProcessingPhase.FINALIZING);
                progress.setWarningMessage("Search failed: " + e.getMessage());
                
                return searchResultMapper.createErrorResult(
                    searchId,
                    searchRequest.getSpecifications(),
                    "Search failed",
                    e.getMessage()
                );
            }
        }).whenComplete((result, throwable) -> {
            if (throwable != null) {
                deferredResult.setResult(error("Search failed: " + throwable.getMessage()));
            } else {
                deferredResult.setResult(success(result));
            }
            
            // Clean up progress after delay
            CompletableFuture.delayedExecutor(5, TimeUnit.MINUTES)
                .execute(() -> searchProgressTracker.remove(searchId));
        });
        
        // Handle timeout
        deferredResult.onTimeout(() -> {
            progress.setWarningMessage("Search timed out");
            SearchResultDTO timeoutResult = searchResultMapper.createErrorResult(
                searchId,
                searchRequest.getSpecifications(),
                "Search timed out",
                "The search operation exceeded the timeout limit of " + 
                searchRequest.getEffectiveOptions().getTimeoutSeconds() + " seconds"
            );
            deferredResult.setResult(success(timeoutResult));
        });
        
        // Return search ID immediately
        SearchResultDTO initialResult = SearchResultDTO.builder()
            .searchId(searchId)
            .status(SearchResultDTO.SearchStatus.PROCESSING)
            .startTime(LocalDateTime.now())
            .searchCriteria(searchRequest.getSpecifications())
            .progress(progress)
            .build();
        
        deferredResult.setResult(success(initialResult));
        
        return deferredResult;
    }
    
    /**
     * Get search progress
     */
    @GetMapping("/search-progress/{searchId}")
    public ResponseEntity<ApiResponse<SearchProgressDTO>> getSearchProgress(
            @PathVariable @NotBlank String searchId) {
        
        SearchProgressDTO progress = searchProgressTracker.get(searchId);
        
        if (progress != null) {
            return success(progress);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(false, null, "Search not found: " + searchId, "404"));
        }
    }
    
    /**
     * Batch search endpoint
     */
    @PostMapping("/search-batch")
    public DeferredResult<ResponseEntity<ApiResponse<BatchSearchResponseDTO>>> searchBatch(
            @Valid @RequestBody BatchSearchRequestDTO batchRequest) {
        
        DeferredResult<ResponseEntity<ApiResponse<BatchSearchResponseDTO>>> deferredResult = 
            new DeferredResult<>(300000L); // 5 minute timeout for batch
        
        String batchId = UUID.randomUUID().toString();
        
        // Initialize batch status
        BatchSearchStatusDTO.BatchProgress batchProgress = BatchSearchStatusDTO.BatchProgress.builder()
            .totalItems(batchRequest.getSearchItems().size())
            .processedItems(0)
            .successfulItems(0)
            .failedItems(0)
            .percentComplete(0.0)
            .elapsedTimeMs(0)
            .build();
        
        BatchSearchStatusDTO batchStatus = BatchSearchStatusDTO.builder()
            .batchId(batchId)
            .status(BatchSearchResponseDTO.BatchStatus.PROCESSING)
            .progress(batchProgress)
            .build();
        
        batchSearchTracker.put(batchId, batchStatus);
        
        // Process batch asynchronously
        CompletableFuture.supplyAsync(() -> {
            LocalDateTime startTime = LocalDateTime.now();
            List<BatchSearchResponseDTO.BatchSearchResult> results = new ConcurrentHashMap<String, BatchSearchResponseDTO.BatchSearchResult>().values().stream().toList();
            
            if (batchRequest.isParallel()) {
                // Process in parallel
                results = batchRequest.getSearchItems().parallelStream()
                    .map(item -> processSingleSearchItem(item, batchRequest.getCommonOptions()))
                    .peek(result -> updateBatchProgress(batchId, result))
                    .toList();
            } else {
                // Process sequentially
                results = batchRequest.getSearchItems().stream()
                    .map(item -> processSingleSearchItem(item, batchRequest.getCommonOptions()))
                    .peek(result -> updateBatchProgress(batchId, result))
                    .toList();
            }
            
            // Calculate summary
            int successCount = (int) results.stream().filter(r -> r.getError() == null).count();
            int failCount = results.size() - successCount;
            
            BatchSearchResponseDTO.BatchSearchSummary summary = BatchSearchResponseDTO.BatchSearchSummary.builder()
                .totalSearches(results.size())
                .successfulSearches(successCount)
                .failedSearches(failCount)
                .totalResultsFound(results.stream()
                    .filter(r -> r.getSearchResult() != null)
                    .mapToInt(r -> r.getSearchResult().getTotalResults())
                    .sum())
                .averageProcessingTimeMs(results.stream()
                    .mapToLong(BatchSearchResponseDTO.BatchSearchResult::getProcessingTimeMs)
                    .average()
                    .orElse(0.0))
                .build();
            
            BatchSearchResponseDTO.BatchStatus finalStatus = failCount == 0 ? 
                BatchSearchResponseDTO.BatchStatus.COMPLETED :
                (successCount > 0 ? BatchSearchResponseDTO.BatchStatus.PARTIAL_SUCCESS : 
                 BatchSearchResponseDTO.BatchStatus.FAILED);
            
            return BatchSearchResponseDTO.builder()
                .batchId(batchId)
                .status(finalStatus)
                .startTime(startTime)
                .endTime(LocalDateTime.now())
                .results(results)
                .summary(summary)
                .durationMs(java.time.Duration.between(startTime, LocalDateTime.now()).toMillis())
                .build();
            
        }).whenComplete((result, throwable) -> {
            if (throwable != null) {
                deferredResult.setResult(error("Batch search failed: " + throwable.getMessage()));
            } else {
                deferredResult.setResult(success(result));
            }
            
            // Clean up after delay
            CompletableFuture.delayedExecutor(10, TimeUnit.MINUTES)
                .execute(() -> batchSearchTracker.remove(batchId));
        });
        
        return deferredResult;
    }
    
    /**
     * Get batch search status
     */
    @GetMapping("/search-batch/{batchId}/status")
    public ResponseEntity<ApiResponse<BatchSearchStatusDTO>> getBatchSearchStatus(
            @PathVariable @NotBlank String batchId) {
        
        BatchSearchStatusDTO status = batchSearchTracker.get(batchId);
        
        if (status != null) {
            return success(status);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(false, null, "Batch search not found: " + batchId, "404"));
        }
    }
    
    /**
     * Clear all caches
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<ApiResponse<Map<String, String>>> clearCache() {
        cacheService.clearAllCaches();
        return success(Map.of("message", "All caches cleared successfully"));
    }
    
    /**
     * Get cache statistics
     * This method should replace the existing getCacheStats method in KnowledgeBaseSearchController
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<ApiResponse<CacheStatsDTO>> getCacheStats() {
        // Get individual cache statistics
        Map<String, Map<String, Object>> cacheStatistics = cacheService.getCacheStatistics();
        Map<String, Long> cacheSizes = cacheService.getCacheSizes();
        
        // Create aggregated statistics
        CacheStatsDTO.CacheStatistics aggregatedStats = 
            CacheStatsDTO.CacheStatistics.fromCacheStats(cacheStatistics, cacheSizes);
        
        // Build the DTO
        CacheStatsDTO cacheStatsDTO = CacheStatsDTO.builder()
            .statistics(aggregatedStats)
            .sizes(cacheSizes)
            .build();
        
        return success(cacheStatsDTO);
    }
    
    // Helper methods
    
    private BatchSearchResponseDTO.BatchSearchResult processSingleSearchItem(
            BatchSearchRequestDTO.SearchItem item,
            SearchRequestDTO.SearchOptions commonOptions) {
        
        long itemStartTime = System.currentTimeMillis();
        
        try {
            // Merge options
            SearchRequestDTO.SearchOptions effectiveOptions = 
                item.getSpecificOptions() != null ? item.getSpecificOptions() : commonOptions;
            
            // Create search request
            SearchRequestDTO searchRequest = SearchRequestDTO.builder()
                .specifications(item.getSpecifications())
                .options(effectiveOptions)
                .build();
            
            // Perform search
            List<Map<String, Object>> results = knowledgeBaseService.searchSimilarBOMs(
                searchRequest.getSpecifications()
            );
            
            SearchResultDTO.SearchConfiguration config = SearchResultDTO.SearchConfiguration.builder()
                .maxResults(effectiveOptions.getMaxResults())
                .minSimilarityScore(effectiveOptions.getMinSimilarityScore())
                .build();
            
            SearchResultDTO searchResult = searchResultMapper.toSearchResultDTO(
                results,
                item.getSpecifications(),
                itemStartTime,
                config
            );
            
            return BatchSearchResponseDTO.BatchSearchResult.builder()
                .itemId(item.getItemId())
                .searchResult(searchResult)
                .processingTimeMs(System.currentTimeMillis() - itemStartTime)
                .tags(item.getTags())
                .build();
                
        } catch (Exception e) {
            return BatchSearchResponseDTO.BatchSearchResult.builder()
                .itemId(item.getItemId())
                .error(e.getMessage())
                .processingTimeMs(System.currentTimeMillis() - itemStartTime)
                .tags(item.getTags())
                .build();
        }
    }
    
    private void updateBatchProgress(String batchId, BatchSearchResponseDTO.BatchSearchResult result) {
        BatchSearchStatusDTO status = batchSearchTracker.get(batchId);
        if (status != null && status.getProgress() != null) {
            BatchSearchStatusDTO.BatchProgress progress = status.getProgress();
            progress.setProcessedItems(progress.getProcessedItems() + 1);
            
            if (result.getError() == null) {
                progress.setSuccessfulItems(progress.getSuccessfulItems() + 1);
            } else {
                progress.setFailedItems(progress.getFailedItems() + 1);
            }
            
            progress.setPercentComplete(
                (double) progress.getProcessedItems() / progress.getTotalItems() * 100.0
            );
            progress.setCurrentItem(result.getItemId());
        }
    }
}
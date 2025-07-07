package com.jfc.owl.controller;

import com.jfc.owl.dto.search.*;
import com.jfc.owl.dto.search.SearchRequestDTO.SearchOptions;
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
import java.util.concurrent.*;
import java.util.stream.Collectors;

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
    
    // Thread pool for search operations
    private final ExecutorService searchExecutor = Executors.newFixedThreadPool(10);
    
    // Store for tracking search progress
    private final Map<String, SearchProgressDTO> searchProgressTracker = new ConcurrentHashMap<>();
    
    // In-memory tracking for batch searches
    private final Map<String, BatchSearchStatusDTO> batchSearchTracker = new ConcurrentHashMap<>();
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Check if service is responsive
            health.put("status", "UP");
            health.put("timestamp", LocalDateTime.now());
            
            // Check knowledge base service
            Map<String, Object> kbStats = knowledgeBaseService.getKnowledgeBaseStatistics();
            health.put("knowledgeBase", Map.of(
                "status", kbStats.get("status"),
                "totalEntries", kbStats.get("totalEntries"),
                "cacheSize", kbStats.get("cacheSize")
            ));
            
            // Check cache service
            Map<String, Long> cacheSizes = cacheService.getCacheSizes();
            health.put("cache", Map.of(
                "status", "UP",
                "sizes", cacheSizes
            ));
            
            // Thread pool status
            if (searchExecutor instanceof ThreadPoolExecutor) {
                ThreadPoolExecutor tpe = (ThreadPoolExecutor) searchExecutor;
                health.put("threadPool", Map.of(
                    "activeThreads", tpe.getActiveCount(),
                    "poolSize", tpe.getPoolSize(),
                    "queueSize", tpe.getQueue().size(),
                    "completedTasks", tpe.getCompletedTaskCount()
                ));
            }
            
            // Active searches
            health.put("activeSearches", searchProgressTracker.size());
            health.put("activeBatchSearches", batchSearchTracker.size());
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            logger.error("Health check failed", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }
    
    /**
     * Get search results by ID (for async search)
     */
    @GetMapping("/search-results/{searchId}")
    public ResponseEntity<ApiResponse<SearchResultDTO>> getSearchResults(
            @PathVariable @NotBlank String searchId) {
        
        // This would typically retrieve from a persistent store
        // For now, return from progress tracker if available
        SearchProgressDTO progress = searchProgressTracker.get(searchId);
        
        if (progress != null && progress.getPercentComplete() >= 100) {
            // Search is complete, return results
            // In a real implementation, you'd retrieve the actual results from storage
            SearchResultDTO result = SearchResultDTO.builder()
                .searchId(searchId)
                .status(SearchResultDTO.SearchStatus.COMPLETED)
                .totalResults(progress.getFoundMatches())
                .build();
            
            return success(result);
        } else if (progress != null) {
            // Still processing
            SearchResultDTO result = SearchResultDTO.builder()
                .searchId(searchId)
                .status(SearchResultDTO.SearchStatus.PROCESSING)
                .progress(progress)
                .build();
            
            return success(result);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(false, null, "Search not found: " + searchId, "404"));
        }
    }

    /**
     * Enhanced synchronous similarity search endpoint with timeout handling
     */
    @PostMapping("/search-similar")
    public ResponseEntity<ApiResponse<SearchResultDTO>> searchSimilarBOMs(
            @Valid @RequestBody SearchRequestDTO searchRequest) {
        
        String searchId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        
        try {
            // Check cache first if enabled
            if (searchRequest.getEffectiveOptions().isUseCache()) {
                SearchResultDTO cachedResult = cacheService.getCachedSearchResults(searchRequest.getSpecifications());
                if (cachedResult != null) {
                    logger.info("Returning cached results for search: {}", searchId);
                    cachedResult.setSearchId(searchId);
                    return success(cachedResult);
                }
            }
            // Get the search type from the request
            SearchRequestDTO.SearchType searchType = searchRequest.getEffectiveSearchType();
            
            // Use CompletableFuture with timeout
            /*
            int timeoutSeconds = searchRequest.getEffectiveOptions().getTimeoutSeconds();
            CompletableFuture<List<Map<String, Object>>> searchFuture = CompletableFuture.supplyAsync(() -> 
                knowledgeBaseService.searchSimilarBOMs(searchRequest.getSpecifications(),
                	    searchRequest.getOptions()),
                searchExecutor
            );
            
            List<Map<String, Object>> results;
            try {
                results = searchFuture.get(timeoutSeconds, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                logger.warn("Search timeout after {} seconds for searchId: {}", timeoutSeconds, searchId);
                
                // Cancel the search
                searchFuture.cancel(true);
                
                // Return partial results if available
                SearchResultDTO timeoutResult = searchResultMapper.createTimeoutResult(
                    searchId,
                    searchRequest.getSpecifications(),
                    timeoutSeconds
                );
                
                return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                    .body(new ApiResponse<>(false, timeoutResult, 
                        "Search timeout after " + timeoutSeconds + " seconds", "408"));
            }*/
            
         // 移除超時限制 - 直接調用服務方法
            List<Map<String, Object>> results = knowledgeBaseService.searchSimilarBOMs(
                searchRequest.getSpecifications(),
                searchRequest.getOptions(), searchType
            );
            
            // 添加日誌
            logger.info("Raw search results count: {} for search type: {}", 
                    results != null ? results.size() : 0, searchType);
            
            
            // Build configuration
            SearchResultDTO.SearchConfiguration config = SearchResultDTO.SearchConfiguration.builder()
                .maxResults(searchRequest.getEffectiveOptions().getMaxResults())
                .minSimilarityScore(searchRequest.getEffectiveOptions().getMinSimilarityScore())
                .timeoutSeconds(searchRequest.getEffectiveOptions().getTimeoutSeconds()) //.timeoutSeconds(timeoutSeconds)
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
            
            // add log
            logger.info("Mapped SearchResultDTO - totalResults: {}, results size: {}", 
                resultDTO.getTotalResults(), 
                resultDTO.getResults() != null ? resultDTO.getResults().size() : 0);
            
            
            // Cache the results
            if (searchRequest.getEffectiveOptions().isUseCache() && resultDTO.getTotalResults() > 0) {
                cacheService.cacheSearchResults(searchRequest.getSpecifications(), resultDTO);
            }
            
            logger.info("Search {} completed successfully in {} ms with {} results", 
                searchId, resultDTO.getDurationMs(), resultDTO.getTotalResults());
            
            return success(resultDTO);
            
        /*} catch  (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Search interrupted for searchId: {}", searchId, e);
            
            SearchResultDTO errorResult = searchResultMapper.createErrorResult(
                searchId,
                searchRequest.getSpecifications(),
                "Search interrupted",
                e.getMessage()
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, errorResult, "Search interrupted", "500"));
          */  
        } catch (Exception e) {
            logger.error("Search failed for searchId: {}", searchId, e);
            
            SearchResultDTO errorResult = searchResultMapper.createErrorResult(
                searchId,
                searchRequest.getSpecifications(),
                "Search failed",
                e.getMessage()
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, errorResult, e.getMessage(), "500"));
        }
    }
    
    /**
     * Enhanced asynchronous search endpoint with better progress tracking
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
            .totalItems(0)
            .processedItems(0)
            .foundMatches(0)
            .percentComplete(0.0)
            .currentPhase(SearchProgressDTO.ProcessingPhase.INITIALIZING)
            .elapsedTimeMs(0)
            .build();
        searchProgressTracker.put(searchId, progress);
        
        // Return search ID immediately
        SearchResultDTO initialResult = SearchResultDTO.builder()
            .searchId(searchId)
            .status(SearchResultDTO.SearchStatus.PROCESSING)
            .startTime(LocalDateTime.now())
            .searchCriteria(searchRequest.getSpecifications())
            .progress(progress)
            .build();
        
        deferredResult.setResult(success(initialResult));
        
        // Execute search asynchronously
        CompletableFuture.runAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                // Update progress - Filtering phase
                updateSearchProgress(searchId, SearchProgressDTO.ProcessingPhase.FILTERING, 10.0);
                
                // Check cache first
                if (searchRequest.getEffectiveOptions().isUseCache()) {
                    SearchResultDTO cachedResult = cacheService.getCachedSearchResults(
                        searchRequest.getSpecifications()
                    );
                    if (cachedResult != null) {
                        cachedResult.setSearchId(searchId);
                        updateSearchProgress(searchId, SearchProgressDTO.ProcessingPhase.FINALIZING, 100.0);
                        // Store result for retrieval
                        storeSearchResult(searchId, cachedResult);
                        return;
                    }
                }
                
                // Update progress - Calculating phase
                updateSearchProgress(searchId, SearchProgressDTO.ProcessingPhase.CALCULATING, 30.0);
             // Get search type
                SearchRequestDTO.SearchType searchType = searchRequest.getEffectiveSearchType();
                // Perform actual search with progress updates
                List<Map<String, Object>> results = performSearchWithProgress(
                    searchId, 
                    searchRequest.getSpecifications(),
                    searchRequest.getOptions(),  // 傳遞 options
                    searchType  
                );
                
                // Update progress - Sorting phase
                updateSearchProgress(searchId, SearchProgressDTO.ProcessingPhase.SORTING, 80.0);
                
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
                
                // Update progress - Finalizing
                updateSearchProgress(searchId, SearchProgressDTO.ProcessingPhase.FINALIZING, 100.0);
                progress.setFoundMatches(results.size());
                
                // Cache results if enabled
                if (searchRequest.getEffectiveOptions().isUseCache() && resultDTO.getTotalResults() > 0) {
                    cacheService.cacheSearchResults(searchRequest.getSpecifications(), resultDTO);
                }
                
                // Store result for retrieval
                storeSearchResult(searchId, resultDTO);
                
            } catch (Exception e) {
                logger.error("Async search failed for searchId: {}", searchId, e);
                progress.setWarningMessage("Search failed: " + e.getMessage());
                
                SearchResultDTO errorResult = searchResultMapper.createErrorResult(
                    searchId,
                    searchRequest.getSpecifications(),
                    "Search failed",
                    e.getMessage()
                );
                
                storeSearchResult(searchId, errorResult);
            } finally {
                // Clean up progress after delay
                CompletableFuture.delayedExecutor(5, TimeUnit.MINUTES)
                    .execute(() -> searchProgressTracker.remove(searchId));
            }
        }, searchExecutor);
        
        // Handle timeout
        deferredResult.onTimeout(() -> {
            progress.setWarningMessage("Search timed out");
            logger.warn("Async search timeout for searchId: {}", searchId);
        });
        
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
            // Update elapsed time
            if (progress.getPercentComplete() < 100) {
                long currentTime = System.currentTimeMillis();
                long startTime = currentTime - progress.getElapsedTimeMs();
                progress.setElapsedTimeMs(currentTime - startTime);
                
                // Estimate remaining time
                if (progress.getPercentComplete() > 0) {
                    long estimatedTotal = (long) (progress.getElapsedTimeMs() / (progress.getPercentComplete() / 100.0));
                    progress.setEstimatedRemainingMs(estimatedTotal - progress.getElapsedTimeMs());
                }
            }
            
            return success(progress);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(false, null, "Search not found: " + searchId, "404"));
        }
    }
    
    /**
     * Enhanced batch search endpoint with better error handling
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
            List<BatchSearchResponseDTO.BatchSearchResult> results = new ArrayList<>();
            
            if (batchRequest.isParallel()) {
                // Process in parallel with controlled concurrency
                int parallelism = Math.min(batchRequest.getSearchItems().size(), 5);
                ForkJoinPool customThreadPool = new ForkJoinPool(parallelism);
                
                try {
                	Callable<List<BatchSearchResponseDTO.BatchSearchResult>> parallelTask = () ->
                    batchRequest.getSearchItems().parallelStream()
                        .map(item -> processSingleSearchItem(item, batchRequest.getCommonOptions(), batchId))
                        .collect(Collectors.toList());
                
                    results = customThreadPool.submit(parallelTask).get();
                } catch (Exception e) {
                    logger.error("Parallel batch processing failed", e);
                 // Re-throw or handle appropriately based on your requirements
                    throw new RuntimeException("Batch processing failed", e);
                } finally {
                    customThreadPool.shutdown();
                    try {
                        // Wait for termination
                        if (!customThreadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                            customThreadPool.shutdownNow();
                            // Wait a bit for tasks to respond to being cancelled
                            if (!customThreadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                                logger.error("ForkJoinPool did not terminate");
                            }
                        }
                    } catch (InterruptedException ie) {
                        // (Re-)Cancel if current thread also interrupted
                        customThreadPool.shutdownNow();
                        // Preserve interrupt status
                        Thread.currentThread().interrupt();
                    }
                }
            } else {
                // Process sequentially
                results = batchRequest.getSearchItems().stream()
                    .map(item -> processSingleSearchItem(item, batchRequest.getCommonOptions(), batchId))
                    .collect(Collectors.toList());
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
            
        }, searchExecutor).whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.error("Batch search failed for batchId: {}", batchId, throwable);
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
        try {
            cacheService.clearAllCaches();
            logger.info("All caches cleared successfully");
            return success(Map.of("message", "All caches cleared successfully"));
        } catch (Exception e) {
            logger.error("Failed to clear cache", e);
            return error("Failed to clear cache: " + e.getMessage());
        }
    }
    
    /**
     * Get cache statistics
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<ApiResponse<CacheStatsDTO>> getCacheStats() {
        try {
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
            
        } catch (Exception e) {
            logger.error("Failed to get cache stats", e);
            return error("Failed to get cache stats: " + e.getMessage());
        }
    }
    
    // Helper methods
    
    private BatchSearchResponseDTO.BatchSearchResult processSingleSearchItem(
            BatchSearchRequestDTO.SearchItem item,
            SearchRequestDTO.SearchOptions commonOptions,
            String batchId) {
        
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
            
            // Perform search with timeout
            CompletableFuture<List<Map<String, Object>>> searchFuture = CompletableFuture.supplyAsync(() ->
                knowledgeBaseService.searchSimilarBOMs(searchRequest.getSpecifications(),
                	    searchRequest.getOptions()),
                searchExecutor
            );
            
            List<Map<String, Object>> results;
            try {
                results = searchFuture.get(effectiveOptions.getTimeoutSeconds(), TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                throw new RuntimeException("Search timeout for item: " + item.getItemId());
            }
            
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
            
            // Update batch progress
            updateBatchProgress(batchId, searchResult.getError() == null);
            
            return BatchSearchResponseDTO.BatchSearchResult.builder()
                .itemId(item.getItemId())
                .searchResult(searchResult)
                .processingTimeMs(System.currentTimeMillis() - itemStartTime)
                .tags(item.getTags())
                .build();
                
        } catch (Exception e) {
            logger.error("Failed to process search item: {}", item.getItemId(), e);
            
            // Update batch progress
            updateBatchProgress(batchId, false);
            
            return BatchSearchResponseDTO.BatchSearchResult.builder()
                .itemId(item.getItemId())
                .error(e.getMessage())
                .processingTimeMs(System.currentTimeMillis() - itemStartTime)
                .tags(item.getTags())
                .build();
        }
    }
    
    private void updateBatchProgress(String batchId, boolean success) {
        BatchSearchStatusDTO status = batchSearchTracker.get(batchId);
        if (status != null && status.getProgress() != null) {
            BatchSearchStatusDTO.BatchProgress progress = status.getProgress();
            progress.setProcessedItems(progress.getProcessedItems() + 1);
            
            if (success) {
                progress.setSuccessfulItems(progress.getSuccessfulItems() + 1);
            } else {
                progress.setFailedItems(progress.getFailedItems() + 1);
            }
            
            progress.setPercentComplete(
                (double) progress.getProcessedItems() / progress.getTotalItems() * 100.0
            );
        }
    }
    
    private void updateSearchProgress(String searchId, SearchProgressDTO.ProcessingPhase phase, double percentComplete) {
        SearchProgressDTO progress = searchProgressTracker.get(searchId);
        if (progress != null) {
            progress.setCurrentPhase(phase);
            progress.setPercentComplete(percentComplete);
            progress.setElapsedTimeMs(System.currentTimeMillis());
        }
    }
    
    private List<Map<String, Object>> performSearchWithProgress(String searchId, 
    		Map<String, String> specifications,
            SearchOptions options,
            SearchRequestDTO.SearchType searchType) {
        // This is a simplified version - in reality, you'd integrate progress updates
        // throughout the actual search process
        List<Map<String, Object>> results = knowledgeBaseService.searchSimilarBOMs(
        		specifications, 
                options,  // 現在傳遞 options
                searchType  // Pass the search type
                );
        
        
        SearchProgressDTO progress = searchProgressTracker.get(searchId);
        if (progress != null) {
            progress.setTotalItems(results.size());
            progress.setProcessedItems(results.size());
        }
        
        return results;
    }
    
    private void storeSearchResult(String searchId, SearchResultDTO result) {
        // In a real implementation, you'd store this in a persistent store
        // For now, we'll just log it
        logger.info("Search {} completed with status: {}", searchId, result.getStatus());
    }
    
    // Cleanup on shutdown
    @jakarta.annotation.PreDestroy
    public void cleanup() {
        logger.info("Shutting down search executor");
        searchExecutor.shutdown();
        try {
            if (!searchExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                searchExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            searchExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
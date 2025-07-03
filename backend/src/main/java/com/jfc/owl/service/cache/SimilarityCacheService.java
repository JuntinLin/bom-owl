package com.jfc.owl.service.cache;

import com.jfc.owl.dto.search.SearchResultDTO;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for caching similarity search results and related data
 */
@Service
public class SimilarityCacheService {
    private static final Logger logger = LoggerFactory.getLogger(SimilarityCacheService.class);
    
    // Cache for similarity scores between items
    private final Cache<String, Double> similarityScoreCache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .recordStats() // Enable statistics
            .build();

    // Cache for search results
    private final Cache<String, SearchResultDTO> searchResultsCache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .recordStats() // Enable statistics
            .build();

    /**
     * Get cached similarity score
     */
    public Double getCachedSimilarityScore(String key1, String key2) {
        String cacheKey = createCacheKey(key1, key2);
        return similarityScoreCache.getIfPresent(cacheKey);
    }

    /**
     * Cache similarity score
     */
    public void cacheSimilarityScore(String key1, String key2, double score) {
        String cacheKey = createCacheKey(key1, key2);
        similarityScoreCache.put(cacheKey, score);
    }

    /**
     * Get cached search results
     */
    public SearchResultDTO getCachedSearchResults(Map<String, String> specifications) {
        String cacheKey = createSearchCacheKey(specifications);
        return searchResultsCache.getIfPresent(cacheKey);
    }

    /**
     * Cache search results
     */
    public void cacheSearchResults(Map<String, String> specifications, SearchResultDTO results) {
        String cacheKey = createSearchCacheKey(specifications);
        searchResultsCache.put(cacheKey, results);
    }

    /**
     * Clear all caches
     */
    public void clearAllCaches() {
        similarityScoreCache.invalidateAll();
        searchResultsCache.invalidateAll();
        logger.info("All caches cleared");
    }

    /**
     * Get cache statistics as serializable maps
     */
    public Map<String, Map<String, Object>> getCacheStatistics() {
        Map<String, Map<String, Object>> stats = new HashMap<>();
        
        // Convert CacheStats to serializable map for similarity score cache
        stats.put("similarityScores", convertCacheStatsToMap(similarityScoreCache.stats()));
        
        // Convert CacheStats to serializable map for search results cache
        stats.put("searchResults", convertCacheStatsToMap(searchResultsCache.stats()));
        
        return stats;
    }
    
    /**
     * Get cache sizes
     */
    public Map<String, Long> getCacheSizes() {
        Map<String, Long> sizes = new HashMap<>();
        sizes.put("similarityScores", similarityScoreCache.size());
        sizes.put("searchResults", searchResultsCache.size());
        return sizes;
    }
    
    /**
     * Convert CacheStats to a serializable map
     */
    private Map<String, Object> convertCacheStatsToMap(CacheStats stats) {
        Map<String, Object> statsMap = new HashMap<>();
        
        statsMap.put("hitCount", stats.hitCount());
        statsMap.put("missCount", stats.missCount());
        statsMap.put("loadSuccessCount", stats.loadSuccessCount());
        statsMap.put("loadExceptionCount", stats.loadExceptionCount());
        statsMap.put("totalLoadTime", stats.totalLoadTime());
        statsMap.put("evictionCount", stats.evictionCount());
        statsMap.put("requestCount", stats.requestCount());
        
        // Calculate rates
        double hitRate = stats.hitRate();
        double missRate = stats.missRate();
        double loadExceptionRate = stats.loadExceptionRate();
        double averageLoadPenalty = stats.averageLoadPenalty();
        
        statsMap.put("hitRate", Double.isNaN(hitRate) ? 0.0 : hitRate);
        statsMap.put("missRate", Double.isNaN(missRate) ? 0.0 : missRate);
        statsMap.put("loadExceptionRate", Double.isNaN(loadExceptionRate) ? 0.0 : loadExceptionRate);
        statsMap.put("averageLoadPenalty", Double.isNaN(averageLoadPenalty) ? 0.0 : averageLoadPenalty);
        
        // Add formatted percentages
        statsMap.put("hitRatePercentage", String.format("%.2f%%", 
            Double.isNaN(hitRate) ? 0.0 : hitRate * 100));
        statsMap.put("missRatePercentage", String.format("%.2f%%", 
            Double.isNaN(missRate) ? 0.0 : missRate * 100));
        
        return statsMap;
    }

    /**
     * Create cache key for similarity scores
     */
    private String createCacheKey(String key1, String key2) {
        // Ensure consistent ordering
        if (key1.compareTo(key2) < 0) {
            return key1 + ":" + key2;
        } else {
            return key2 + ":" + key1;
        }
    }

    /**
     * Create cache key for search results
     */
    private String createSearchCacheKey(Map<String, String> specifications) {
        return specifications.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(","));
    }
    
    /**
     * Get cache performance summary
     */
    public Map<String, Object> getCachePerformanceSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        // Overall statistics
        long totalHits = similarityScoreCache.stats().hitCount() + 
                        searchResultsCache.stats().hitCount();
        long totalMisses = similarityScoreCache.stats().missCount() + 
                          searchResultsCache.stats().missCount();
        long totalRequests = totalHits + totalMisses;
        
        summary.put("totalHits", totalHits);
        summary.put("totalMisses", totalMisses);
        summary.put("totalRequests", totalRequests);
        summary.put("overallHitRate", totalRequests > 0 ? 
            String.format("%.2f%%", (double) totalHits / totalRequests * 100) : "0.00%");
        
        // Individual cache performance
        Map<String, Object> cachePerformance = new HashMap<>();
        cachePerformance.put("similarityScores", Map.of(
            "size", similarityScoreCache.size(),
            "hitRate", String.format("%.2f%%", similarityScoreCache.stats().hitRate() * 100)
        ));
        cachePerformance.put("searchResults", Map.of(
            "size", searchResultsCache.size(),
            "hitRate", String.format("%.2f%%", searchResultsCache.stats().hitRate() * 100)
        ));
        
        summary.put("cachePerformance", cachePerformance);
        
        return summary;
    }
    
    /**
     * Warm up cache with frequently used items (optional)
     */
    public void warmUpCache(List<String> frequentItems) {
        logger.info("Warming up cache with {} items", frequentItems.size());
        // Implementation depends on your specific needs
        // This is a placeholder for cache warming logic
    }
}
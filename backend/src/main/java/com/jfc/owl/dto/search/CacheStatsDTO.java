package com.jfc.owl.dto.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for cache statistics matching frontend expectations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheStatsDTO {
    
    /**
     * Cache statistics with hit rates, miss rates, etc.
     */
    private CacheStatistics statistics;
    
    /**
     * Cache sizes by cache name
     */
    private Map<String, Long> sizes;
    
    /**
     * Detailed cache statistics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheStatistics {
        private long hitCount;
        private long missCount;
        private double hitRate;
        private long evictionCount;
        private double averageLoadTime;
        private long totalCacheSize;
        
        // Additional computed fields for easier frontend consumption
        private String hitRatePercentage;
        private String missRatePercentage;
        private long requestCount;
        private double missRate;
        
        /**
         * Create from individual cache stats
         */
        public static CacheStatistics fromCacheStats(Map<String, Map<String, Object>> cacheStats, 
                                                     Map<String, Long> cacheSizes) {
            long totalHits = 0;
            long totalMisses = 0;
            long totalEvictions = 0;
            double totalLoadTime = 0;
            int cacheCount = 0;
            
            // Aggregate stats from all caches
            for (Map<String, Object> stats : cacheStats.values()) {
                totalHits += getLongValue(stats, "hitCount");
                totalMisses += getLongValue(stats, "missCount");
                totalEvictions += getLongValue(stats, "evictionCount");
                totalLoadTime += getDoubleValue(stats, "averageLoadPenalty");
                cacheCount++;
            }
            
            long totalRequests = totalHits + totalMisses;
            double hitRate = totalRequests > 0 ? (double) totalHits / totalRequests : 0.0;
            double missRate = totalRequests > 0 ? (double) totalMisses / totalRequests : 0.0;
            double avgLoadTime = cacheCount > 0 ? totalLoadTime / cacheCount : 0.0;
            
            // Calculate total cache size
            long totalSize = cacheSizes.values().stream().mapToLong(Long::longValue).sum();
            
            return CacheStatistics.builder()
                .hitCount(totalHits)
                .missCount(totalMisses)
                .hitRate(hitRate)
                .missRate(missRate)
                .evictionCount(totalEvictions)
                .averageLoadTime(avgLoadTime)
                .totalCacheSize(totalSize)
                .hitRatePercentage(String.format("%.1f%%", hitRate * 100))
                .missRatePercentage(String.format("%.1f%%", missRate * 100))
                .requestCount(totalRequests)
                .build();
        }
        
        private static long getLongValue(Map<String, Object> map, String key) {
            Object value = map.get(key);
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return 0L;
        }
        
        private static double getDoubleValue(Map<String, Object> map, String key) {
            Object value = map.get(key);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return 0.0;
        }
    }
}
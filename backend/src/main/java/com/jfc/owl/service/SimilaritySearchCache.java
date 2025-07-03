package com.jfc.owl.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Add caching for similarity search results
 */
@Service
public class SimilaritySearchCache {
	private static final Logger logger = LoggerFactory.getLogger(SimilaritySearchCache.class);
	// Cache for similarity scores between items
	private final Cache<String, Double> similarityScoreCache = CacheBuilder.newBuilder().maximumSize(10000)
			.expireAfterWrite(1, TimeUnit.HOURS).build();

	// Cache for search results
	private final Cache<String, List<Map<String, Object>>> searchResultsCache = CacheBuilder.newBuilder()
			.maximumSize(100).expireAfterWrite(30, TimeUnit.MINUTES).build();

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
	public List<Map<String, Object>> getCachedSearchResults(Map<String, String> specifications) {
		String cacheKey = createSearchCacheKey(specifications);
		return searchResultsCache.getIfPresent(cacheKey);
	}

	/**
	 * Cache search results
	 */
	public void cacheSearchResults(Map<String, String> specifications, List<Map<String, Object>> results) {
		String cacheKey = createSearchCacheKey(specifications);
		searchResultsCache.put(cacheKey, results);
	}

	/**
	 * Clear all caches
	 */
	public void clearAllCaches() {
		similarityScoreCache.invalidateAll();
		searchResultsCache.invalidateAll();
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
		return specifications.entrySet().stream().sorted(Map.Entry.comparingByKey())
				.map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(","));
	}


}

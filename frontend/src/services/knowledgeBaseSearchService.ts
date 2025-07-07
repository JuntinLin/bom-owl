// src/services/knowledgeBaseSearchService.ts

import axios, { AxiosError, CancelTokenSource } from 'axios';
import axiosRetry from 'axios-retry';
import createAxiosWithInterceptors from '@/utils/axiosInterceptor';
import { ApiResponse } from '@/types/tiptop';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/owl';
const BASE_URL = '/knowledge-base-search';

// Types matching backend DTOs (保持不變)
export interface SearchRequestDTO {
  specifications: Record<string, string>;
  options?: SearchOptions;
  searchType?: SearchType;
}

export interface SearchOptions {
  maxResults?: number;
  minSimilarityScore?: number;
  includeGeneratedBOMs?: boolean;
  onlyValidated?: boolean;
  onlyHydraulicCylinders?: boolean;
  timeoutSeconds?: number;
  useCache?: boolean;
  includeInactive?: boolean;
  sortOrder?: SortOrder;
  sourceSystemFilter?: string;
  minQualityScore?: number;
  includeDetailedSpecs?: boolean;
  page?: number;
  pageSize?: number;
}

export enum SearchType {
  SIMILARITY = 'SIMILARITY',
  EXACT = 'EXACT',
  FUZZY = 'FUZZY',
  SEMANTIC = 'SEMANTIC'
}

export enum SortOrder {
  SIMILARITY_DESC = 'SIMILARITY_DESC',
  SIMILARITY_ASC = 'SIMILARITY_ASC',
  CREATED_DESC = 'CREATED_DESC',
  CREATED_ASC = 'CREATED_ASC',
  QUALITY_DESC = 'QUALITY_DESC',
  QUALITY_ASC = 'QUALITY_ASC',
  USAGE_DESC = 'USAGE_DESC',
  USAGE_ASC = 'USAGE_ASC'
}

export interface SearchResultDTO {
  searchId: string;
  status: SearchStatus;
  startTime: string;
  endTime?: string;
  results?: SimilarBOMDTO[];
  totalResults: number;
  error?: string;
  errorDetail?: string;
  progress?: SearchProgressDTO;
  searchCriteria: Record<string, string>;
  durationMs: number;
  itemsProcessed: number;
  timeoutCount: number;
  configuration?: SearchConfiguration;
}

export enum SearchStatus {
  PENDING = 'PENDING',
  PROCESSING = 'PROCESSING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED',
  PARTIAL = 'PARTIAL'
}

export interface SearchConfiguration {
  maxResults: number;
  minSimilarityScore: number;
  timeoutSeconds: number;
  useCache: boolean;
  searchAlgorithm?: string;
  additionalParams?: Record<string, any>;
}

export interface SimilarBOMDTO {
  masterItemCode: string;
  fileName: string;
  description: string;
  similarityScore: number;
  createdAt: string;
  tripleCount: number;
  isHydraulicCylinder?: boolean;
  hydraulicCylinderSpecs?: string;
  sourceSystem?: string;
  validationStatus?: string;
  componentCount?: number;
  qualityScore?: number;
  fileSize: number;
  format: string;
  includeHierarchy?: boolean;
  usageCount?: number;
  lastUsedAt?: string;
  tags?: string;
  parsedSpecs?: HydraulicCylinderSpecs;
}

export interface HydraulicCylinderSpecs {
  series: string;
  type: string;
  bore: string;
  stroke: string;
  rodEndType: string;
  installationType?: string;
  shaftEndJoin?: string;
}

export interface SearchProgressDTO {
  totalItems: number;
  processedItems: number;
  foundMatches: number;
  percentComplete: number;
  currentItem?: string;
  currentPhase: ProcessingPhase;
  elapsedTimeMs: number;
  estimatedRemainingMs?: number;
  estimatedCompletionTime?: string;
  averageTimePerItem?: number;
  processingSpeed?: number;
  memoryUsageMB?: number;
  cpuUsagePercent?: number;
  warningMessage?: string;
}

export enum ProcessingPhase {
  INITIALIZING = 'INITIALIZING',
  FILTERING = 'FILTERING',
  CALCULATING = 'CALCULATING',
  SORTING = 'SORTING',
  FINALIZING = 'FINALIZING'
}

export interface BatchSearchRequestDTO {
  searchItems: SearchItem[];
  commonOptions?: SearchOptions;
  parallel?: boolean;
  continueOnError?: boolean;
}

export interface SearchItem {
  itemId: string;
  specifications: Record<string, string>;
  specificOptions?: SearchOptions;
  tags?: string[];
}

export interface BatchSearchResponseDTO {
  batchId: string;
  status: BatchStatus;
  startTime: string;
  endTime?: string;
  results: BatchSearchResult[];
  summary: BatchSearchSummary;
  durationMs: number;
}

export enum BatchStatus {
  QUEUED = 'QUEUED',
  PROCESSING = 'PROCESSING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  PARTIAL_SUCCESS = 'PARTIAL_SUCCESS',
  CANCELLED = 'CANCELLED'
}

export interface BatchSearchResult {
  itemId: string;
  searchResult?: SearchResultDTO;
  error?: string;
  processingTimeMs: number;
  tags?: string[];
}

export interface BatchSearchSummary {
  totalSearches: number;
  successfulSearches: number;
  failedSearches: number;
  totalResultsFound: number;
  averageProcessingTimeMs: number;
  averageSimilarityScore?: number;
  resultsByTag?: Record<string, number>;
}

export interface BatchSearchStatusDTO {
  batchId: string;
  status: BatchStatus;
  progress: BatchProgress;
  completedResults?: BatchSearchResult[];
  estimatedCompletionTime?: string;
}

export interface BatchProgress {
  totalItems: number;
  processedItems: number;
  successfulItems: number;
  failedItems: number;
  percentComplete: number;
  currentItem?: string;
  elapsedTimeMs?: number;
  estimatedRemainingMs?: number;
}

export interface SearchResponseWrapper<T> {
  status: ResponseStatus;
  message: string;
  timestamp: string;
  data?: T;
  metadata?: ResponseMetadata;
  error?: ErrorDetails;
}

export enum ResponseStatus {
  SUCCESS = 'SUCCESS',
  PARTIAL_SUCCESS = 'PARTIAL_SUCCESS',
  ERROR = 'ERROR',
  WARNING = 'WARNING'
}

export interface ResponseMetadata {
  requestId: string;
  processingTimeMs: number;
  apiVersion?: string;
  totalCount?: number;
  returnedCount?: number;
  pagination?: PaginationInfo;
}

export interface PaginationInfo {
  currentPage: number;
  pageSize: number;
  totalPages: number;
  totalElements: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface ErrorDetails {
  code: string;
  message: string;
  detail?: string;
  path?: string;
  timestamp: string;
  traceId?: string;
}

export interface CacheStatsDTO {
  statistics: CacheStatistics;
  sizes: Record<string, number>;
}

export interface CacheStatistics {
  hitCount: number;
  missCount: number;
  hitRate: number;
  evictionCount: number;
  averageLoadTime: number;
  totalCacheSize: number;
}

export interface SearchRetryOptions {
  maxRetries?: number;
  progressiveTimeout?: boolean;
  fallbackToAsync?: boolean;
  reduceScope?: boolean;
}

// Create axios instance with NO timeout (0 = no timeout)
const searchApi = createAxiosWithInterceptors(API_BASE_URL);
searchApi.defaults.timeout = 0; // 無超時限制

// Configure retry mechanism - 移除超時相關的重試條件
axiosRetry(searchApi, {
  retries: 3,
  retryDelay: (retryCount) => retryCount * 2000,
  retryCondition: (error: AxiosError) => {
    // 只在網路錯誤時重試，不在超時時重試
    return axiosRetry.isNetworkError(error) && error.code !== 'ECONNABORTED';
  }
});

// Error handling - 移除超時相關的錯誤處理
const handleApiError = (error: unknown) => {
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError;
    
    if (axiosError.response) {
      const data = axiosError.response.data as any;
      console.error('Search API error:', data);
      
      if (data.message) {
        throw new Error(data.message);
      }
      
      switch (axiosError.response.status) {
        case 400:
          throw new Error('Invalid search parameters');
        case 404:
          throw new Error('Search resource not found');
        case 500:
          throw new Error('Search service error');
        default:
          throw new Error(`Search failed (${axiosError.response.status})`);
      }
    }
    
    if (axiosError.request) {
      throw new Error('Could not connect to search service');
    }
    
    throw new Error(axiosError.message || 'Unknown search error');
  }
  
  console.error('Unexpected search error:', error);
  throw new Error('An unexpected search error occurred');
};

export const knowledgeBaseSearchService = {
  // Store cancel tokens for active requests
  activeCancelTokens: new Map<string, CancelTokenSource>(),

  /**
   * Perform synchronous similarity search - 無超時限制
   */
  searchSimilar: async (request: SearchRequestDTO): Promise<SearchResultDTO> => {
    try {
      const response = await searchApi.post<ApiResponse<SearchResultDTO>>(
        `${BASE_URL}/search-similar`,
        request,
        {
          timeout: 0 // 無超時限制
        }
      );
      
      if (!response.data.success) {
        throw new Error(response.data.message || 'Search failed');
      }
      
      return response.data.data;
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Perform search with retry strategy - 移除超時相關邏輯
   */
  searchSimilarWithRetry: async (
    request: SearchRequestDTO,
    options?: SearchRetryOptions
  ): Promise<SearchResultDTO> => {
    const { 
      maxRetries = 2,
      fallbackToAsync = true,
      reduceScope = true 
    } = options || {};
    
    let lastError: Error | null = null;
    
    // 直接嘗試搜索，不考慮超時
    for (let attempt = 0; attempt <= maxRetries; attempt++) {
      try {
        // Cancel any existing search
        knowledgeBaseSearchService.cancelSearch('search-similar');
        
        // Create new cancel token
        const cancelToken = axios.CancelToken.source();
        knowledgeBaseSearchService.activeCancelTokens.set('search-similar', cancelToken);
        
        // 如果重試，可以選擇縮小搜索範圍
        const adjustedRequest = (attempt > 0 && reduceScope) ? {
          ...request,
          options: {
            ...request.options,
            maxResults: Math.max(5, Math.floor((request.options?.maxResults || 20) / (attempt + 1))),
            minSimilarityScore: Math.min(0.95, (request.options?.minSimilarityScore || 0.7) + (0.1 * attempt))
          }
        } : request;
        
        console.log(`Search attempt ${attempt + 1}/${maxRetries + 1}`);
        
        const response = await searchApi.post<ApiResponse<SearchResultDTO>>(
          `${BASE_URL}/search-similar`,
          adjustedRequest,
          {
            timeout: 0, // 無超時限制
            cancelToken: cancelToken.token
          }
        );
        
        if (!response.data.success) {
          throw new Error(response.data.message || 'Search failed');
        }
        
        // Clear cancel token on success
        knowledgeBaseSearchService.activeCancelTokens.delete('search-similar');
        
        return response.data.data;
        
      } catch (error) {
        lastError = error as Error;
        
        if (axios.isCancel(error)) {
          throw new Error('Search was cancelled');
        }
        
        // 如果是網路錯誤，等待後重試
        if (axios.isAxiosError(error) && !error.response) {
          console.warn(`Network error on attempt ${attempt + 1}/${maxRetries + 1}`);
          
          if (attempt < maxRetries) {
            await new Promise(resolve => setTimeout(resolve, Math.min(1000 * Math.pow(2, attempt), 5000)));
            continue;
          }
        }
        
        // 其他錯誤不重試
        break;
      }
    }
    
    // 如果失敗且啟用了異步回退
    if (fallbackToAsync && lastError) {
      console.log('Falling back to async search');
      return knowledgeBaseSearchService.searchSimilarAsync(request);
    }
    
    handleApiError(lastError);
    throw lastError;
  },

  /**
   * Perform asynchronous similarity search - 無超時限制
   */
  searchSimilarAsync: async (request: SearchRequestDTO): Promise<SearchResultDTO> => {
    try {
      const response = await searchApi.post<ApiResponse<SearchResultDTO>>(
        `${BASE_URL}/search-similar-async`,
        request,
        {
          timeout: 0 // 無超時限制
        }
      );
      
      if (!response.data.success) {
        throw new Error(response.data.message || 'Async search failed');
      }
      
      return response.data.data;
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Get search progress
   */
  getSearchProgress: async (searchId: string): Promise<SearchProgressDTO> => {
    try {
      const response = await searchApi.get<ApiResponse<SearchProgressDTO>>(
        `${BASE_URL}/search-progress/${searchId}`,
        {
          timeout: 0 // 無超時限制
        }
      );
      
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to get search progress');
      }
      
      return response.data.data;
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Get search results
   */
  getSearchResults: async (searchId: string): Promise<SearchResultDTO> => {
    try {
      const response = await searchApi.get<ApiResponse<SearchResultDTO>>(
        `${BASE_URL}/search-results/${searchId}`,
        {
          timeout: 0 // 無超時限制
        }
      );
      
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to get search results');
      }
      
      return response.data.data;
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Cancel search
   */
  cancelSearch: (searchKey: string) => {
    const cancelToken = knowledgeBaseSearchService.activeCancelTokens.get(searchKey);
    if (cancelToken) {
      cancelToken.cancel('Search cancelled by user');
      knowledgeBaseSearchService.activeCancelTokens.delete(searchKey);
    }
  },

  /**
   * Cancel all active searches
   */
  cancelAllSearches: () => {
    knowledgeBaseSearchService.activeCancelTokens.forEach((cancelToken, key) => {
      cancelToken.cancel('All searches cancelled');
    });
    knowledgeBaseSearchService.activeCancelTokens.clear();
  },

  /**
   * Perform batch search - 無超時限制
   */
  searchBatch: async (request: BatchSearchRequestDTO): Promise<BatchSearchResponseDTO> => {
    try {
      // Cancel any existing batch search
      knowledgeBaseSearchService.cancelSearch('batch-search');
      
      const cancelToken = axios.CancelToken.source();
      knowledgeBaseSearchService.activeCancelTokens.set('batch-search', cancelToken);
      
      const response = await searchApi.post<ApiResponse<BatchSearchResponseDTO>>(
        `${BASE_URL}/search-batch`,
        request,
        {
          timeout: 0, // 無超時限制
          cancelToken: cancelToken.token
        }
      );
      
      knowledgeBaseSearchService.activeCancelTokens.delete('batch-search');
      
      if (!response.data.success) {
        throw new Error(response.data.message || 'Batch search failed');
      }
      
      return response.data.data;
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Get batch search status
   */
  getBatchSearchStatus: async (batchId: string): Promise<BatchSearchStatusDTO> => {
    try {
      const response = await searchApi.get<ApiResponse<BatchSearchStatusDTO>>(
        `${BASE_URL}/search-batch/${batchId}/status`,
        {
          timeout: 0 // 無超時限制
        }
      );
      
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to get batch status');
      }
      
      return response.data.data;
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Clear search cache
   */
  clearCache: async (): Promise<void> => {
    try {
      const response = await searchApi.post<ApiResponse<any>>(
        `${BASE_URL}/cache/clear`,
        {},
        {
          timeout: 0 // 無超時限制
        }
      );
      
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to clear cache');
      }
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Get cache statistics
   */
  getCacheStats: async (): Promise<CacheStatsDTO> => {
    try {
      const response = await searchApi.get<ApiResponse<CacheStatsDTO>>(
        `${BASE_URL}/cache/stats`,
        {
          timeout: 0 // 無超時限制
        }
      );
      
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to get cache stats');
      }
      
      return response.data.data;
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Poll for async search results - 無超時限制，可以無限等待
   */
  pollSearchResults: async (
    searchId: string, 
    onProgress?: (progress: SearchProgressDTO) => void,
    pollInterval = 1000,
    maxAttempts = Number.MAX_SAFE_INTEGER // 設為最大整數，實際上無限等待
  ): Promise<SearchResultDTO> => {
    let attempts = 0;
    
    while (attempts < maxAttempts) {
      try {
        const progress = await knowledgeBaseSearchService.getSearchProgress(searchId);
        
        if (onProgress) {
          onProgress(progress);
        }
        
        // Check if search is complete
        if (progress.percentComplete >= 100 || progress.currentPhase === ProcessingPhase.FINALIZING) {
          // Get final results
          return await knowledgeBaseSearchService.getSearchResults(searchId);
        }
        
        // Wait before next poll
        await new Promise(resolve => setTimeout(resolve, pollInterval));
        attempts++;
        
      } catch (error) {
        console.error('Error polling search results:', error);
        throw error;
      }
    }
    
    throw new Error('Search polling exceeded maximum attempts');
  },

  /**
   * Perform chunked search for large datasets - 移除超時相關邏輯
   */
  searchSimilarChunked: async (
    specifications: Record<string, string>,
    options?: SearchOptions
  ): Promise<SearchResultDTO> => {
    const nonEmptySpecs = Object.entries(specifications)
      .filter(([_, value]) => value && value.trim())
      .reduce((acc, [key, value]) => ({ ...acc, [key]: value }), {});
    
    // If few specifications, use simple search
    if (Object.keys(nonEmptySpecs).length <= 2) {
      return knowledgeBaseSearchService.searchSimilar({
        specifications: nonEmptySpecs,
        options
      });
    }
    
    // For complex searches, automatically use async approach
    const request: SearchRequestDTO = {
      specifications: nonEmptySpecs,
      options: {
        ...options,
        useCache: true
      }
    };
    
    // Try sync first with retry, then fallback to async
    return knowledgeBaseSearchService.searchSimilarWithRetry(request, {
      maxRetries: 1,
      fallbackToAsync: true,
      reduceScope: true
    });
  },

  /**
   * Optimize search request based on criteria - 移除超時相關邏輯
   */
  optimizeSearchRequest: (request: SearchRequestDTO): SearchRequestDTO => {
    const nonEmptySpecs = Object.entries(request.specifications)
      .filter(([_, value]) => value && value.trim())
      .reduce((acc, [key, value]) => ({ ...acc, [key]: value }), {} as Record<string, string>);
    
    const specCount = Object.keys(nonEmptySpecs).length;
    const hasComplexSpecs = 'bore' in nonEmptySpecs || 'stroke' in nonEmptySpecs;
    
    // Auto-adjust parameters based on complexity
    if (specCount > 3 || hasComplexSpecs) {
      return {
        ...request,
        specifications: nonEmptySpecs,
        options: {
          ...request.options,
          maxResults: Math.min(request.options?.maxResults || 20, 15),
          minSimilarityScore: Math.max(request.options?.minSimilarityScore || 0.7, 0.75),
          useCache: true,
          pageSize: Math.min(request.options?.pageSize || 20, 10)
        },
        searchType: request.searchType || SearchType.SIMILARITY
      };
    }
    
    return {
      ...request,
      specifications: nonEmptySpecs
    };
  },

  /**
   * Build search request with defaults - 移除超時相關邏輯
   */
  buildSearchRequest: (
    specifications: Record<string, string>,
    options?: Partial<SearchOptions>,
    searchType?: SearchType
  ): SearchRequestDTO => {
    const defaultOptions: SearchOptions = {
      maxResults: 20,
      minSimilarityScore: 0.3,
      includeGeneratedBOMs: false,
      onlyValidated: false,
      onlyHydraulicCylinders: false,
      useCache: true,
      includeInactive: false,
      sortOrder: SortOrder.SIMILARITY_DESC,
      includeDetailedSpecs: true,
      page: 1,
      pageSize: 20
    };
    
    const request = {
      specifications,
      options: { ...defaultOptions, ...options },
      searchType: searchType || SearchType.SIMILARITY
    };
    
    // Optimize the request
    return knowledgeBaseSearchService.optimizeSearchRequest(request);
  },

  /**
   * Estimate search complexity and recommend approach - 更新估計時間
   */
  estimateSearchComplexity: (specifications: Record<string, string>, totalKBSize?: number): {
    complexity: 'low' | 'medium' | 'high' | 'very-high';
    recommendedApproach: 'sync' | 'sync-with-retry' | 'async';
    estimatedTime: number;
    recommendations: string[];
  } => {
    const nonEmptySpecs = Object.entries(specifications)
      .filter(([_, value]) => value && value.trim())
      .length;
    
    const hasComplexSpecs = specifications.bore || specifications.stroke;
    const kbSize = totalKBSize || 1000; // Default estimate
    
    let complexity: 'low' | 'medium' | 'high' | 'very-high' = 'low';
    let estimatedTime = 5;
    const recommendations: string[] = [];
    
    // Calculate complexity score
    let score = 0;
    score += nonEmptySpecs * 2;
    score += hasComplexSpecs ? 3 : 0;
    score += kbSize > 5000 ? 5 : kbSize > 1000 ? 3 : 1;
    
    if (score <= 5) {
      complexity = 'low';
      estimatedTime = 5;
    } else if (score <= 10) {
      complexity = 'medium';
      estimatedTime = 30;
      recommendations.push('Consider enabling cache for faster results');
    } else if (score <= 15) {
      complexity = 'high';
      estimatedTime = 60;
      recommendations.push('Search may take longer time, please be patient');
      recommendations.push('Consider using async search for better experience');
    } else {
      complexity = 'very-high';
      estimatedTime = 120;
      recommendations.push('This is a complex search that may take several minutes');
      recommendations.push('Consider narrowing search criteria if possible');
      recommendations.push('Async search is recommended');
    }
    
    const recommendedApproach = 
      complexity === 'low' ? 'sync' :
      complexity === 'medium' ? 'sync-with-retry' : 'async';
    
    return {
      complexity,
      recommendedApproach,
      estimatedTime,
      recommendations
    };
  },

  /**
   * Convert search results to legacy format (for backward compatibility)
   */
  convertToLegacyFormat: (searchResult: SearchResultDTO): any[] => {
    if (!searchResult.results) return [];
    
    return searchResult.results.map(item => ({
      masterItemCode: item.masterItemCode,
      fileName: item.fileName,
      description: item.description,
      similarityScore: item.similarityScore,
      createdAt: item.createdAt,
      tripleCount: item.tripleCount,
      format: item.format,
      fileSize: item.fileSize,
      // Additional fields for compatibility
      isHydraulicCylinder: item.isHydraulicCylinder,
      sourceSystem: item.sourceSystem,
      validationStatus: item.validationStatus,
      usageCount: item.usageCount
    }));
  },

  /**
   * Calculate search statistics
   */
  calculateSearchStats: (results: SimilarBOMDTO[]): {
    avgSimilarity: number;
    avgQualityScore: number;
    validatedCount: number;
    aiGeneratedCount: number;
    hydraulicCylinderCount: number;
  } => {
    if (results.length === 0) {
      return {
        avgSimilarity: 0,
        avgQualityScore: 0,
        validatedCount: 0,
        aiGeneratedCount: 0,
        hydraulicCylinderCount: 0
      };
    }
    
    const stats = results.reduce((acc, item) => {
      acc.totalSimilarity += item.similarityScore || 0;
      acc.totalQualityScore += item.qualityScore || 0;
      if (item.validationStatus === 'VALIDATED' || item.validationStatus === 'VALID') {
        acc.validatedCount++;
      }
      if (item.sourceSystem === 'AI_GENERATED') {
        acc.aiGeneratedCount++;
      }
      if (item.isHydraulicCylinder) {
        acc.hydraulicCylinderCount++;
      }
      return acc;
    }, {
      totalSimilarity: 0,
      totalQualityScore: 0,
      validatedCount: 0,
      aiGeneratedCount: 0,
      hydraulicCylinderCount: 0
    });
    
    return {
      avgSimilarity: stats.totalSimilarity / results.length,
      avgQualityScore: stats.totalQualityScore / results.length,
      validatedCount: stats.validatedCount,
      aiGeneratedCount: stats.aiGeneratedCount,
      hydraulicCylinderCount: stats.hydraulicCylinderCount
    };
  },

  /**
   * Perform health check
   */
  healthCheck: async (): Promise<boolean> => {
    try {
      const response = await searchApi.get(`${BASE_URL}/health`, {
        timeout: 5000 // 健康檢查保留短超時
      });
      return response.status === 200;
    } catch {
      return false;
    }
  }
};

export default knowledgeBaseSearchService;
// src/services/knowledgeBaseSearchService.ts

import axios, { AxiosError } from 'axios';
import axiosRetry from 'axios-retry';
import createAxiosWithInterceptors from '@/utils/axiosInterceptor';
import { ApiResponse } from '@/types/tiptop';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/owl';
const BASE_URL = '/knowledge-base-search';

// Types matching backend DTOs

// SearchRequestDTO.java
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

// SearchResultDTO.java
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

// SimilarBOMDTO.java
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

// SearchProgressDTO.java
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

// BatchSearchRequestDTO.java
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

// BatchSearchResponseDTO.java
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

// BatchSearchStatusDTO.java
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

// SearchResponseWrapper.java
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

// Cache statistics types
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

// Create axios instance with interceptors
const searchApi = createAxiosWithInterceptors(API_BASE_URL);
searchApi.defaults.timeout = 60000; // 60 seconds

// Configure retry mechanism
axiosRetry(searchApi, {
  retries: 3,
  retryDelay: (retryCount) => retryCount * 2000,
  retryCondition: (error: AxiosError) => {
    return axiosRetry.isNetworkOrIdempotentRequestError(error) ||
      error.code === 'ECONNABORTED';
  }
});

// Error handling
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
        case 408:
          throw new Error('Search request timeout');
        case 500:
          throw new Error('Search service error');
        default:
          throw new Error(`Search failed (${axiosError.response.status})`);
      }
    }
    
    if (axiosError.code === 'ECONNABORTED') {
      throw new Error('Search operation timeout. Try reducing the search scope.');
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
  /**
   * Perform synchronous similarity search
   */
  searchSimilar: async (request: SearchRequestDTO): Promise<SearchResultDTO> => {
    try {
      const response = await searchApi.post<ApiResponse<SearchResultDTO>>(
        `${BASE_URL}/search-similar`,
        request
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
   * Perform asynchronous similarity search
   */
  searchSimilarAsync: async (request: SearchRequestDTO): Promise<SearchResultDTO> => {
    try {
      const response = await searchApi.post<ApiResponse<SearchResultDTO>>(
        `${BASE_URL}/search-similar-async`,
        request
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
        `${BASE_URL}/search-progress/${searchId}`
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
   * Perform batch search
   */
  searchBatch: async (request: BatchSearchRequestDTO): Promise<BatchSearchResponseDTO> => {
    try {
      const response = await searchApi.post<ApiResponse<BatchSearchResponseDTO>>(
        `${BASE_URL}/search-batch`,
        request
      );
      
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
        `${BASE_URL}/search-batch/${batchId}/status`
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
        `${BASE_URL}/cache/clear`
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
        `${BASE_URL}/cache/stats`
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
   * Poll for async search results
   */
  pollSearchResults: async (
    searchId: string, 
    onProgress?: (progress: SearchProgressDTO) => void,
    pollInterval = 1000,
    maxAttempts = 60
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
          // Get final results - assuming there's an endpoint for this
          // You might need to adjust based on actual backend implementation
          const finalResponse = await searchApi.get<ApiResponse<SearchResultDTO>>(
            `${BASE_URL}/search-results/${searchId}`
          );
          
          if (finalResponse.data.success) {
            return finalResponse.data.data;
          }
        }
        
        // Wait before next poll
        await new Promise(resolve => setTimeout(resolve, pollInterval));
        attempts++;
        
      } catch (error) {
        console.error('Error polling search results:', error);
        throw error;
      }
    }
    
    throw new Error('Search polling timeout exceeded');
  },

  /**
   * Build search request with defaults
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
      timeoutSeconds: 30,
      useCache: true,
      includeInactive: false,
      sortOrder: SortOrder.SIMILARITY_DESC,
      includeDetailedSpecs: true,
      page: 1,
      pageSize: 20
    };
    
    return {
      specifications,
      options: { ...defaultOptions, ...options },
      searchType: searchType || SearchType.SIMILARITY
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
  }
};

export default knowledgeBaseSearchService;
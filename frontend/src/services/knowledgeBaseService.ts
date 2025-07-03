// src/services/knowledgeBaseService.ts

import axios, { AxiosError } from 'axios';
import axiosRetry from 'axios-retry';
import createAxiosWithInterceptors from '@/utils/axiosInterceptor';
import { ApiResponse } from '@/types/tiptop';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/owl';
const BASE_URL = '/owl-knowledge-base';

// Types for Knowledge Base
export interface KnowledgeBaseEntry {
  id: number;
  masterItemCode: string;
  fileName: string;
  filePath: string;
  format: string;
  includeHierarchy: boolean;
  description: string;
  fileSize: number;
  fileHash: string;
  tripleCount: number;
  createdAt: string;
  updatedAt: string;
  active: boolean;
  version: number;
  usageCount: number;
  lastUsedAt?: string;
  tags?: string;
}

export interface KnowledgeBaseStats {
  totalEntries: number;
  totalFileSize: number;
  totalTriples: number;
  hydraulicCylinderCount: number;
  formatDistribution: Record<string, number>;
  cacheSize: number;
  lastMasterUpdate: string;
  hydraulicCylinderPercentage?: number;
  totalStorageSizeFormatted?: string;
  averageFileSize?: number;
  averageTripleCount?: number;
  averageQualityScore?: number;
  topUsedItems?: Array<{
    masterItemCode: string;
    usageCount: number;
    description: string;
  }>;
  recentlyAccessedCount?: number;
  validEntries?: number;
  invalidEntries?: number;
  pendingValidation?: number;
}

export interface BatchExportResult {
  batchId?: string; // Added for new batch processing
  totalItems: number;
  successfulExports: string[];
  failedExports: string[];
  successCount: number;
  failureCount: number;
  masterKnowledgeBaseFile: string;
}

export interface CleanupResult {
  deletedEntries: string[];
  errorEntries: string[];
  deletedCount: number;
  errorCount: number;
}

export interface ExportRequest {
  masterItemCode: string;
  format: string;
  includeHierarchy: boolean;
  description: string;
}

export interface BatchExportRequest {
  format: string;
  includeHierarchy: boolean;
}

export interface BatchProgress {
  batchId: string;
  totalItems: number;
  processedItems: number;
  successCount: number;
  failureCount: number;
  status: string;
  startTime: string;
  endTime?: string;
  progressPercentage?: number;
  estimatedRemainingSeconds?: number;
}

export interface ProcessingLog {
  batchId: string;
  status: string;
  totalItems: number;
  processedItems: number;
  successCount: number;
  failureCount: number;
  startTime: string;
  endTime?: string;
  errorDetails?: string;
  skippedCount?: number;
  averageTimePerItem?: number;
  estimatedCompletionTime?: string;
  lastProcessedItemCode?: string;
  checkpointData?: string;
  processingParameters?: string;
  notes?: string;
}

// Use the axios instance with basic interceptors
const knowledgeBaseApi = createAxiosWithInterceptors(API_BASE_URL);
// Configure timeout for potentially longer operations
knowledgeBaseApi.defaults.timeout = 120000; // 2 minutes for knowledge base operations

// Add request interceptor for debugging
knowledgeBaseApi.interceptors.request.use(request => {
  console.log('Starting Knowledge Base Request', {
    url: request.url,
    method: request.method,
    baseURL: request.baseURL
  });
  return request;
});

// Configure retry mechanism
axiosRetry(knowledgeBaseApi, {
  retries: 3,
  retryDelay: (retryCount) => {
    return retryCount * 3000; // Longer delays for knowledge base operations
  },
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
      console.error('Knowledge Base API error:', data);

      if (data.message) {
        throw new Error(data.message);
      }

      switch (axiosError.response.status) {
        case 400:
          throw new Error('Invalid request parameters');
        case 404:
          throw new Error('Knowledge base entry not found');
        case 409:
          throw new Error('Knowledge base conflict - entry already exists');
        case 500:
          throw new Error('Knowledge base server error');
        default:
          throw new Error(`Knowledge base operation failed (${axiosError.response.status})`);
      }
    }

    if (axiosError.code === 'ECONNABORTED') {
      throw new Error('Knowledge base operation timeout. Large operations may take several minutes.');
    }

    if (axiosError.request) {
      console.error('Knowledge base network error:', axiosError);
      throw new Error('Could not connect to knowledge base service');
    }

    throw new Error(axiosError.message || 'Unknown knowledge base error');
  }

  console.error('Unexpected knowledge base error:', error);
  throw new Error('An unexpected knowledge base error occurred');
};

export const knowledgeBaseService = {
  /**
   * Initialize the knowledge base system
   */
  initializeKnowledgeBase: async (): Promise<string> => {
    try {
      const response = await knowledgeBaseApi.post<ApiResponse<string>>(
        `${BASE_URL}/initialize`
      );
      
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to initialize knowledge base');
      }
      
      return response.data.data;
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Export a single item to the knowledge base
   */
  exportSingleToKnowledgeBase: async (request: ExportRequest): Promise<KnowledgeBaseEntry> => {
    try {
      const response = await knowledgeBaseApi.post<ApiResponse<KnowledgeBaseEntry>>(
        `${BASE_URL}/export-single`,
        request
      );
      
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to export to knowledge base');
      }
      
      return response.data.data;
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Batch export all BOMs to the knowledge base
   */
  exportAllToKnowledgeBase: async (request: BatchExportRequest): Promise<BatchExportResult> => {
    try {
      const response = await knowledgeBaseApi.post<ApiResponse<BatchExportResult>>(
        `${BASE_URL}/export-all`,
        request
      );
      
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to batch export to knowledge base');
      }
      
      return response.data.data;
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Get knowledge base statistics
   */
  getStatistics: async (): Promise<KnowledgeBaseStats> => {
    try {
      const response = await knowledgeBaseApi.get<ApiResponse<KnowledgeBaseStats>>(
        `${BASE_URL}/statistics`
      );
      
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to get knowledge base statistics');
      }
      
      return response.data.data;
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * NOTE: searchSimilarBOMs has been moved to knowledgeBaseSearchService
   * Use knowledgeBaseSearchService.searchSimilar() instead
   * 
   * Example migration:
   * Before: 
   *   const results = await knowledgeBaseService.searchSimilarBOMs(specifications);
   * 
   * After:
   *   import knowledgeBaseSearchService from '@/services/knowledgeBaseSearchService';
   *   const searchRequest = knowledgeBaseSearchService.buildSearchRequest(specifications);
   *   const searchResult = await knowledgeBaseSearchService.searchSimilar(searchRequest);
   *   const results = searchResult.results || [];
   */

  /**
   * Search knowledge base entries by keyword
   */
  searchKnowledgeBase: async (keyword: string): Promise<KnowledgeBaseEntry[]> => {
    try {
      const response = await knowledgeBaseApi.get<ApiResponse<KnowledgeBaseEntry[]>>(
        `${BASE_URL}/search`,
        { params: { keyword } }
      );
      
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to search knowledge base');
      }
      
      return response.data.data;
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Update a knowledge base entry
   */
  updateKnowledgeBaseEntry: async (masterItemCode: string, description: string): Promise<KnowledgeBaseEntry> => {
    try {
      const response = await knowledgeBaseApi.put<ApiResponse<KnowledgeBaseEntry>>(
        `${BASE_URL}/update/${masterItemCode}`,
        { description }
      );
      
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to update knowledge base entry');
      }
      
      return response.data.data;
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Clean up the knowledge base
   */
  cleanupKnowledgeBase: async (): Promise<CleanupResult> => {
    try {
      const response = await knowledgeBaseApi.post<ApiResponse<CleanupResult>>(
        `${BASE_URL}/cleanup`
      );
      
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to cleanup knowledge base');
      }
      
      return response.data.data;
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Get an OWL model from the knowledge base
   */
  getKnowledgeBaseModel: async (masterItemCode: string, format = 'RDF/XML'): Promise<string> => {
    try {
      const response = await knowledgeBaseApi.get<ApiResponse<string>>(
        `${BASE_URL}/model/${masterItemCode}`,
        { params: { format } }
      );
      
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to get knowledge base model');
      }
      
      return response.data.data;
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Download an OWL file from the knowledge base
   */
  downloadOWLFile: async (masterItemCode: string, format = 'RDF/XML'): Promise<void> => {
    try {
      const response = await knowledgeBaseApi.get(
        `${BASE_URL}/download/${masterItemCode}`,
        {
          params: { format },
          responseType: 'blob'
        }
      );
      
      // Create download link
      const blob = new Blob([response.data], { type: getContentType(format) });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${masterItemCode}_knowledge_base.${getFileExtension(format)}`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Get batch processing progress
   */
  getBatchProgress: async (batchId: string): Promise<BatchProgress> => {
    try {
      const response = await knowledgeBaseApi.get<ApiResponse<BatchProgress>>(
        `${BASE_URL}/batch-progress/${batchId}`
      );
      
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to get batch progress');
      }
      
      return response.data.data;
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Resume a paused or interrupted batch export
   */
  resumeBatchExport: async (batchId: string): Promise<BatchExportResult> => {
    try {
      const response = await knowledgeBaseApi.post<ApiResponse<BatchExportResult>>(
        `${BASE_URL}/resume-batch/${batchId}`
      );
      
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to resume batch export');
      }
      
      return response.data.data;
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Pause a running batch export
   */
  pauseBatchExport: async (batchId: string): Promise<void> => {
    try {
      const response = await knowledgeBaseApi.post<ApiResponse<any>>(
        `${BASE_URL}/pause-batch/${batchId}`
      );
      
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to pause batch export');
      }
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Cancel a running or paused batch export
   */
  cancelBatchExport: async (batchId: string): Promise<void> => {
    try {
      const response = await knowledgeBaseApi.post<ApiResponse<any>>(
        `${BASE_URL}/cancel-batch/${batchId}`
      );
      
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to cancel batch export');
      }
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Get batch processing history
   */
  getBatchHistory: async (limit = 10): Promise<ProcessingLog[]> => {
    try {
      const response = await knowledgeBaseApi.get<ApiResponse<ProcessingLog[]>>(
        `${BASE_URL}/batch-history`,
        { params: { limit } }
      );
      
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to get batch history');
      }
      
      return response.data.data;
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Get knowledge base entries by tag
   */
  getEntriesByTag: async (tag: string): Promise<KnowledgeBaseEntry[]> => {
    try {
      const response = await knowledgeBaseApi.get<ApiResponse<KnowledgeBaseEntry[]>>(
        `${BASE_URL}/tag/${tag}`
      );
      
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to get entries by tag');
      }
      
      return response.data.data;
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Get most frequently used knowledge base entries
   */
  getMostUsedEntries: async (limit = 10): Promise<KnowledgeBaseEntry[]> => {
    try {
      const response = await knowledgeBaseApi.get<ApiResponse<KnowledgeBaseEntry[]>>(
        `${BASE_URL}/most-used`,
        { params: { limit } }
      );
      
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to get most used entries');
      }
      
      return response.data.data;
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Validate knowledge base integrity
   */
  validateKnowledgeBase: async (): Promise<{valid: boolean; issues: string[]}> => {
    try {
      const response = await knowledgeBaseApi.post<ApiResponse<{valid: boolean; issues: string[]}>>(
        `${BASE_URL}/validate`
      );
      
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to validate knowledge base');
      }
      
      return response.data.data;
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  }
};

// Utility functions
const getContentType = (format: string): string => {
  switch (format.toUpperCase()) {
    case 'TURTLE':
    case 'TTL':
      return 'text/turtle';
    case 'N-TRIPLES':
    case 'NT':
      return 'application/n-triples';
    case 'JSON-LD':
    case 'JSONLD':
      return 'application/ld+json';
    case 'RDF/XML':
    case 'XML':
    default:
      return 'application/rdf+xml';
  }
};

const getFileExtension = (format: string): string => {
  switch (format.toUpperCase()) {
    case 'TURTLE':
    case 'TTL':
      return 'ttl';
    case 'N-TRIPLES':
    case 'NT':
      return 'nt';
    case 'JSON-LD':
    case 'JSONLD':
      return 'jsonld';
    case 'RDF/XML':
    case 'XML':
    default:
      return 'owl';
  }
};

export default knowledgeBaseService;
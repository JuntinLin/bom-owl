// src/services/bomGeneratorService.ts

import axios, { AxiosError } from 'axios';
import axiosRetry from 'axios-retry';
import createAxiosWithInterceptors from '@/utils/axiosInterceptor';
import { ApiResponse } from '@/types/tiptop';
import { SimilarBOMDTO } from './knowledgeBaseSearchService';


const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/owl';
const BASE_URL = '/bom-generator';

// Types for BOM generation
export interface NewItemInfo {
  itemCode: string;
  itemName?: string;
  itemSpec?: string;
}

export interface CylinderSpecifications {
  series: string;
  type: string;
  bore: string;
  stroke: string;
  rodEndType: string;
  installationType?: string;
  shaftEndJoin?: string;
}

export interface ComponentOption {
  code: string;
  name: string;
  description?: string;
  spec?: string;
  quantity?: number;
  compatibilityScore?: number;
  recommendationLevel?: string;
}

export interface ComponentCategory {
  category: string;
  categoryDisplayName?: string;
  categoryDescription?: string;
  defaultQuantity: number;
  options: ComponentOption[];
  isRequired?: boolean;
  maxRecommendations?: number;
  recommendedOption?: ComponentOption;
  averageCompatibilityScore?: number;
}

export interface CylinderClassification {
  boreSize?: string;
  strokeLength?: string;
  series?: string;
  rodEndType?: string;
}

export interface ComponentStatistics {
  totalComponents: number;
  componentsByCategory?: Record<string, number>;
  averageCompatibilityScore: number;
  highConfidenceComponents: number;
}

export interface SimilarCylinder {
  code: string;
  name?: string;
  spec?: string;
  similarityScore: number;
  specifications: CylinderSpecifications;
}

export interface GeneratedBom {
  masterItemCode: string;
  itemName?: string;
  itemSpec?: string;
  specifications: CylinderSpecifications;
  cylinderClassification?: CylinderClassification;
  validationPassed?: boolean;
  validationWarnings?: string[];
  componentCategories: ComponentCategory[];
  similarCylinders: SimilarCylinder[];
  knowledgeBaseSuggestions?: SimilarBOMDTO[]; // Updated to use SimilarBOMDTO
  overallRecommendationScore?: number;
  componentStatistics?: ComponentStatistics;
  generationMetadata?: {
    generatedAt: number;
    totalComponents: number;
    totalCategories: number;
  };
  defaultQuantities?: Record<string, number>;
}

export interface CodeValidationResult {
  itemCode: string;
  isValid: boolean;
  message: string;
  specifications: CylinderSpecifications;
}

// Use the axios instance with basic interceptors
const bomGeneratorApi = createAxiosWithInterceptors(API_BASE_URL);
// Configure timeout for potentially longer operations
bomGeneratorApi.defaults.timeout = 45000; // 45 seconds

// Add request interceptor for debugging
bomGeneratorApi.interceptors.request.use(request => {
  console.log('Starting Request', {
    url: request.url,
    method: request.method,
    baseURL: request.baseURL
  });
  return request;
});

// Configure retry mechanism
axiosRetry(bomGeneratorApi, {
  retries: 2,
  retryDelay: (retryCount) => {
    return retryCount * 2000;
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
      // Handle backend returned errors
      const data = axiosError.response.data as any;
      console.error('Backend error:', data);

      // If backend returns detailed error message
      if (data.message) {
        throw new Error(data.message);
      }

      // Handle different HTTP status codes
      switch (axiosError.response.status) {
        case 400:
          throw new Error('Invalid request parameters, please check your input');
        case 404:
          throw new Error('The requested resource was not found');
        case 500:
          throw new Error('Internal server error, please try again later');
        default:
          throw new Error(`Request failed (${axiosError.response.status})`);
      }
    }

    if (axiosError.code === 'ECONNABORTED') {
      throw new Error('Request timeout. BOM generation operations may take a long time for complex structures.');
    }

    if (axiosError.request) {
      console.error('Network error:', axiosError);
      throw new Error('Could not connect to the server, please check your network connection');
    }
    console.error('Axios error:', axiosError);
    throw new Error(axiosError.message || 'Unknown network error');
  }
  console.error('Unexpected error:', error);
  throw new Error('An unexpected error occurred');
};

export const bomGeneratorService = {
  /**
   * Generate a new BOM structure for a hydraulic cylinder
   * 
   * @param newItemInfo The information for the new hydraulic cylinder
   * @returns Promise with the generated BOM structure
   */
  generateNewBom: async (newItemInfo: NewItemInfo): Promise<GeneratedBom> => {
    try {
      const response = await bomGeneratorApi.post<ApiResponse<GeneratedBom>>(
        `${BASE_URL}/generate`,
        newItemInfo
      );
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to generate BOM');
      }
      return response.data.data;
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Export a generated BOM as an ontology
   * 
   * @param bomStructure The generated BOM structure
   * @param format The ontology format (RDF/XML, TURTLE, JSON-LD, N-TRIPLES)
   * @returns Promise with the exported ontology as text
   */
  exportGeneratedBom: async (bomStructure: GeneratedBom, format = 'JSONLD'): Promise<string> => {
    try {
      const response = await bomGeneratorApi.post<string>(
        `${BASE_URL}/export-generated`,
        bomStructure,
        {
          params: { format },
          responseType: 'text'
        }
      );
      return response.data;
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Find similar hydraulic cylinders based on a new cylinder code
   * 
   * @param newItemCode The code of the new hydraulic cylinder
   * @returns Promise with similar cylinders and their similarity scores
   */
  findSimilarCylinders: async (newItemCode: string): Promise<{ itemCode: string; similarCylinders: SimilarCylinder[] }> => {
    try {
      const response = await bomGeneratorApi.get<ApiResponse<{ itemCode: string; similarCylinders: SimilarCylinder[] }>>(
        `${BASE_URL}/similar-cylinders/${newItemCode}`
      );
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to find similar cylinders');
      }
      return response.data.data;
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Validate a hydraulic cylinder code format
   * 
   * @param itemCode The code to validate
   * @returns Promise with validation result and extracted specifications
   */
  validateCylinderCode: async (itemCode: string): Promise<CodeValidationResult> => {
    try {
      const response = await bomGeneratorApi.get<ApiResponse<CodeValidationResult>>(
        `${BASE_URL}/validate-code/${itemCode}`
      );
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to validate cylinder code');
      }
      return response.data.data;
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Save a generated BOM to the knowledge base
   * 
   * @param bomStructure The generated BOM structure to save
   * @param description Description for the knowledge base entry
   * @returns Promise indicating success
   */
  saveGeneratedBomToKnowledgeBase: async (
    bomStructure: GeneratedBom, 
    description?: string
  ): Promise<void> => {
    try {
      const response = await bomGeneratorApi.post<ApiResponse<any>>(
        `${BASE_URL}/save-to-knowledge-base`,
        {
          bomStructure,
          description: description || `Generated BOM for ${bomStructure.masterItemCode}`
        }
      );
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to save BOM to knowledge base');
      }
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  }
};

export default bomGeneratorService;
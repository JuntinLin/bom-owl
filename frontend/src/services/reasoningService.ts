// src/services/reasoningService.ts

import axios, { AxiosError } from 'axios';
import axiosRetry from 'axios-retry';
import createAxiosWithInterceptors from '@/utils/axiosInterceptor';
import { ApiResponse, 
  ReasoningResult, 
  SparqlQueryResult, 
  PredefinedQuery, 
  ExampleRule, 
  ReasonerInfo,
  CustomRuleResult  } from '@/types/tiptop';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/owl';
const BASE_URL = '/reasoning';

// Use the axios instance with basic interceptors
const reasoningApi = createAxiosWithInterceptors(API_BASE_URL);
// Configure timeout separately
reasoningApi.defaults.timeout = 120000; // 45 seconds for potentially long reasoning operations

// Add request interceptor for debugging
reasoningApi.interceptors.request.use(request => {
  console.log('Starting Request', {
      url: request.url,
      method: request.method,
      baseURL: request.baseURL
  });
  return request;
});

// Configure retry mechanism
axiosRetry(reasoningApi, {
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
            throw new Error('Request timeout. Reasoning operations may take a long time for complex ontologies.');
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

export const reasoningService = {
  /**
   * Perform reasoning on a BOM ontology.
   * 
   * @param masterItemCode The master item code to reason about
   * @param reasonerType   The type of reasoner to use
   * @returns Promise with the reasoning results
   */
  performReasoning: async (masterItemCode: string, reasonerType = 'OWL'): Promise<ReasoningResult> => {
    try {
      const response = await reasoningApi.get<ApiResponse<ReasoningResult>>(
        `${BASE_URL}/infer/${masterItemCode}`, 
        { params: { reasonerType } }
      );
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to perform reasoning');
      }
      return response.data.data;
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Execute a SPARQL query on a BOM ontology.
   * 
   * @param masterItemCode The master item code to query
   * @param query          The SPARQL query to execute
   * @returns Promise with the query results
   */
  executeSparqlQuery: async (masterItemCode: string, query: string): Promise<SparqlQueryResult> => {
    try {
      const response = await reasoningApi.post<ApiResponse<SparqlQueryResult>>(
        `${BASE_URL}/sparql/${masterItemCode}`,
        query,
        { headers: { 'Content-Type': 'text/plain' } }
      );
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to execute SPARQL query');
      }
      return response.data.data;
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Apply custom rules to a BOM ontology.
   * 
   * @param masterItemCode The master item code to apply rules to
   * @param rules          The custom rules to apply
   * @returns Promise with the results after applying the rules
   */
  applyCustomRules: async (masterItemCode: string, rules: string): Promise<CustomRuleResult> => {
    try {
      const response = await reasoningApi.post<ApiResponse<CustomRuleResult>>(
        `${BASE_URL}/rules/${masterItemCode}`,
        rules,
        { headers: { 'Content-Type': 'text/plain' } }
      );
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to apply custom rules');
      }
      return response.data.data;
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Get predefined SPARQL queries.
   * 
   * @returns Promise with a list of predefined queries
   */
  getPredefinedQueries: async (): Promise<PredefinedQuery[]> => {
    try {
      const response = await reasoningApi.get<ApiResponse<PredefinedQuery[]>>(
        `${BASE_URL}/predefined-queries`
      );
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to retrieve predefined queries');
      }
      return response.data.data || [];
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Get example rules for the ontology.
   * 
   * @returns Promise with a list of example rules
   */
  getExampleRules: async (): Promise<ExampleRule[]> => {
    try {
      const response = await reasoningApi.get<ApiResponse<ExampleRule[]>>(
        `${BASE_URL}/example-rules`
      );
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to retrieve example rules');
      }
      return response.data.data || [];
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  },

  /**
   * Get available reasoners in the system.
   * 
   * @returns Promise with a list of available reasoners
   */
  getAvailableReasoners: async (): Promise<ReasonerInfo[]> => {
    try {
      const response = await reasoningApi.get<ApiResponse<ReasonerInfo[]>>(
        `${BASE_URL}/available-reasoners`
      );
      if (!response.data.success) {
        throw new Error(response.data.message || 'Failed to retrieve available reasoners');
      }
      return response.data.data || [];
    } catch (error) {
      handleApiError(error);
      throw error;
    }
  }
};

export default reasoningService;
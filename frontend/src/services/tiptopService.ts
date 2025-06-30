// src/services/tiptopService.ts

import axios, { AxiosError } from 'axios';
import axiosRetry from 'axios-retry';
import createAxiosWithInterceptors from '@/utils/axiosInterceptor';
import { BomTreeNode, ImaFile, BomComponent, BomStats, ApiResponse, OntologyExportFormat } from '@/types/tiptop';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/owl';

// Use the axios instance with basic interceptors
const tiptopApi = createAxiosWithInterceptors(API_BASE_URL);
// Configure timeout separately
tiptopApi.defaults.timeout = 30000; // 30 seconds

// Add request interceptor for debugging
tiptopApi.interceptors.request.use(request => {
    console.log('Starting Request', {
        url: request.url,
        method: request.method,
        baseURL: request.baseURL
    });
    return request;
});

// Configure retry mechanism
axiosRetry(tiptopApi, {
    retries: 3,
    retryDelay: (retryCount) => {
        return retryCount * 1500;
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
            throw new Error('Request timeout, please try again later');
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

export const tiptopService = {
    // Get all materials
    getAllMaterials: async (): Promise<ImaFile[]> => {
        try {
            const response = await tiptopApi.get<ApiResponse<ImaFile[]>>('/bom/materials');
            if (!response.data.success) {
                throw new Error(response.data.message || 'Failed to retrieve materials');
            }
            return response.data.data || [];
        } catch (error) {
            handleApiError(error);
            throw error;
        }
    },

    // Search materials by query
    searchMaterials: async (query: string): Promise<ImaFile[]> => {
        try {
            const response = await tiptopApi.get<ApiResponse<ImaFile[]>>('/bom/materials/search', {
                params: { query }
            });
            if (!response.data.success) {
                throw new Error(response.data.message || 'Failed to search materials');
            }
            return response.data.data || [];
        } catch (error) {
            handleApiError(error);
            throw error;
        }
    },

    // Search materials by product type and line
    searchByProductTypeAndLine: async (type?: string, line?: string): Promise<ImaFile[]> => {
        try {
            const response = await tiptopApi.get<ApiResponse<ImaFile[]>>('/bom/materials/product', {
                params: { type, line }
            });
            
            if (!response.data.success) {
                throw new Error(response.data.message || 'Failed to search by product type and line');
            }
            
            return response.data.data || [];
        } catch (error) {
            handleApiError(error);
            throw error;
        }
    },

    // Advanced search for materials
    searchItemsComplex: async (params: {
        code?: string;
        name?: string;
        spec?: string;
    }): Promise<ImaFile[]> => {
        try {
            const response = await tiptopApi.post<ApiResponse<ImaFile[]>>('/bom/items/search', params);
            if (!response.data.success) {
                throw new Error(response.data.message || 'Failed to search items');
            }
            return response.data.data || [];
        } catch (error) {
            handleApiError(error);
            throw error;
        }
    },

    // Get material by code
    getMaterialByCode: async (code: string): Promise<ImaFile> => {
        try {
            const response = await tiptopApi.get<ApiResponse<ImaFile>>(`/bom/materials/${code}`);
            if (!response.data.success) {
                throw new Error(response.data.message || 'Failed to retrieve material');
            }
            return response.data.data;
        } catch (error) {
            handleApiError(error);
            throw error;
        }
    },

    // Get BOM components for a master item
    getBomComponents: async (masterItemCode: string): Promise<BomComponent[]> => {
        try {
            const response = await tiptopApi.get<ApiResponse<BomComponent[]>>(`/bom/components/${masterItemCode}`);
            if (!response.data.success) {
                throw new Error(response.data.message || 'Failed to retrieve BOM components');
            }
            return response.data.data || [];
        } catch (error) {
            handleApiError(error);
            throw error;
        }
    },

    // Get BOM tree for a master item
    getBomTree: async (masterItemCode: string): Promise<BomTreeNode> => {
        try {
            const response = await tiptopApi.get<ApiResponse<BomTreeNode>>(`/bom/tree/${masterItemCode}`);
            if (!response.data.success) {
                throw new Error(response.data.message || 'Failed to retrieve BOM tree');
            }
            return response.data.data;
        } catch (error) {
            handleApiError(error);
            throw error;
        }
    },

    // Get BOM statistics
    getBomStats: async (): Promise<BomStats> => {
        try {
            const response = await tiptopApi.get<ApiResponse<BomStats>>('/bom/stats');
            if (!response.data.success) {
                throw new Error(response.data.message || 'Failed to retrieve BOM statistics');
            }
            return response.data.data;
        } catch (error) {
            handleApiError(error);
            throw error;
        }
    },

    // Export BOM for a master item - With Complete Hierarchy
    exportBom: async (masterItemCode: string, format: OntologyExportFormat = OntologyExportFormat.RDF_XML): Promise<string> => {
        try {
            // Use a different approach for file downloads
            const url = `${API_BASE_URL}/bom/export/${masterItemCode}?format=${format}`;
            window.location.href = url;
            return url;
        } catch (error) {
            handleApiError(error);
            throw error;
        }
    },

    // Export BOM as OWL - Simple version (direct components only)
    exportSimpleBom: async (
        masterItemCode: string, 
        format: OntologyExportFormat = OntologyExportFormat.RDF_XML
    ): Promise<string> => {
        try {
            const url = `${API_BASE_URL}/bom/export-simple/${masterItemCode}?format=${format}`;
            window.location.href = url;
            return url;
        } catch (error) {
            handleApiError(error);
            throw error;
        }
    },

    // Export all BOMs as OWL - Complete Hierarchy
    exportAllBoms: async (format: string = 'RDF/XML'): Promise<string> => {
        try {
            // Use a different approach for file downloads
            const url = `${API_BASE_URL}/bom/export-all?format=${format}`;
            window.location.href = url;
            return url;
        } catch (error) {
            handleApiError(error);
            throw error;
        }
    },

    // Export all BOMs as OWL - Simple version (direct components only)
    exportAllSimpleBoms: async (
        format: OntologyExportFormat = OntologyExportFormat.RDF_XML
    ): Promise<string> => {
        try {
            const url = `${API_BASE_URL}/bom/export-all-simple?format=${format}`;
            window.location.href = url;
            return url;
        } catch (error) {
            handleApiError(error);
            throw error;
        }
    },

    // Get BOM ontology as text
    getBomOntology: async (masterItemCode: string, format: string = 'JSONLD'): Promise<string> => {
        try {
            const response = await tiptopApi.get<string>(
                `/bom/ontology/${masterItemCode}`,
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

    // Get complete BOM ontology
    getCompleteBomOntology: async (
        masterItemCode: string,
        format: string = 'JSONLD'
    ): Promise<string> => {
        try {
            const response = await tiptopApi.get<string>(
                `/bom/ontology-complete/${masterItemCode}`,
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
    }
};

export default tiptopService;
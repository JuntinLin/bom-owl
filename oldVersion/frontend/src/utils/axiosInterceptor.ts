// src/utils/axiosInterceptor.ts

import axios, { AxiosInstance } from 'axios';

// Create axios instance with basic interceptors (no authentication)
const createAxiosWithInterceptors = (baseURL: string): AxiosInstance => {
    const api = axios.create({
        baseURL,
        headers: {
            'Content-Type': 'application/json',
        },
        timeout: 30000, // 30 seconds default timeout
    });

    // Response interceptor for logging and error handling
    api.interceptors.response.use(
        (response) => {
            // Log successful responses if needed
            return response;
        },
        (error) => {
            // Log errors for debugging
            console.error('API Error:', error);
            return Promise.reject(error);
        }
    );

    return api;
};

export default createAxiosWithInterceptors;
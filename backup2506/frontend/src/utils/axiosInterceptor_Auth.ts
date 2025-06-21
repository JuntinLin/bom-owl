// src/utils/axiosInterceptor.ts

import axios, { AxiosInstance } from 'axios';
//import { useAuth } from '@/hooks/useAuth'; // Import your auth hook or service

// Create axios instance with interceptors for authentication
const createAxiosWithInterceptors = (baseURL: string): AxiosInstance => {
    const api = axios.create({
        baseURL,
        headers: {
            'Content-Type': 'application/json',
        },
        timeout: 30000, // 30 seconds default timeout
    });

    // Request interceptor to add authentication token
    api.interceptors.request.use(
        (config) => {
            // Get token from localStorage or your auth service
            const token = localStorage.getItem('token');
            
            // If token exists, add it to the headers
            if (token) {
                config.headers['Authorization'] = `Bearer ${token}`;
            }
            
            return config;
        },
        (error) => {
            return Promise.reject(error);
        }
    );

    // Response interceptor for handling common responses
    api.interceptors.response.use(
        (response) => {
            return response;
        },
        async (error) => {
            const originalRequest = error.config;

            // Handle 401 Unauthorized errors - token might be expired
            if (error.response && error.response.status === 401 && !originalRequest._retry) {
                originalRequest._retry = true;
                
                try {
                    // Attempt to refresh the token if you have a refresh token mechanism
                    // This depends on your authentication strategy
                    const refreshToken = localStorage.getItem('refreshToken');
                    
                    if (refreshToken) {
                        // Call your refresh token endpoint
                        // const response = await axios.post('/auth/refresh', { refreshToken });
                        // const newToken = response.data.token;
                        
                        // Update token in localStorage
                        // localStorage.setItem('token', newToken);
                        
                        // Update the failed request with the new token
                        // originalRequest.headers['Authorization'] = `Bearer ${newToken}`;
                        
                        // Retry the original request
                        // return api(originalRequest);
                    }
                    
                    // If no refresh token or refresh fails, redirect to login
                    window.location.href = '/login';
                    return Promise.reject(error);
                } catch (refreshError) {
                    // If refresh token attempt fails, redirect to login
                    localStorage.removeItem('token');
                    localStorage.removeItem('refreshToken');
                    window.location.href = '/login';
                    return Promise.reject(refreshError);
                }
            }

            return Promise.reject(error);
        }
    );

    return api;
};

export default createAxiosWithInterceptors;
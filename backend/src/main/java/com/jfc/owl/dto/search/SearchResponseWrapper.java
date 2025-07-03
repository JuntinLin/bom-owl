package com.jfc.owl.dto.search;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * Wrapper for search API responses
 * Provides consistent response structure with metadata
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResponseWrapper<T> {
    
    /**
     * Response status
     */
    private ResponseStatus status;
    
    /**
     * Response message
     */
    private String message;
    
    /**
     * Response timestamp
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * Response data (SearchResultDTO, SearchProgressDTO, etc.)
     */
    private T data;
    
    /**
     * Response metadata
     */
    private ResponseMetadata metadata;
    
    /**
     * Error details (only for error responses)
     */
    private ErrorDetails error;
    
    /**
     * Response status enum
     */
    public enum ResponseStatus {
        SUCCESS,
        PARTIAL_SUCCESS,
        ERROR,
        WARNING
    }
    
    /**
     * Response metadata
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseMetadata {
        private String requestId;
        private long processingTimeMs;
        private String apiVersion;
        private int totalCount;
        private int returnedCount;
        private PaginationInfo pagination;
    }
    
    /**
     * Pagination information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationInfo {
        private int currentPage;
        private int pageSize;
        private int totalPages;
        private long totalElements;
        private boolean hasNext;
        private boolean hasPrevious;
    }
    
    /**
     * Error details
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetails {
        private String code;
        private String message;
        private String detail;
        private String path;
        private LocalDateTime timestamp;
        private String traceId;
    }
    
    /**
     * Create success response
     */
    public static <T> SearchResponseWrapper<T> success(T data, String message) {
        return SearchResponseWrapper.<T>builder()
            .status(ResponseStatus.SUCCESS)
            .message(message)
            .data(data)
            .build();
    }
    
    /**
     * Create success response with metadata
     */
    public static <T> SearchResponseWrapper<T> success(T data, String message, ResponseMetadata metadata) {
        return SearchResponseWrapper.<T>builder()
            .status(ResponseStatus.SUCCESS)
            .message(message)
            .data(data)
            .metadata(metadata)
            .build();
    }
    
    /**
     * Create error response
     */
    public static <T> SearchResponseWrapper<T> error(String message, ErrorDetails error) {
        return SearchResponseWrapper.<T>builder()
            .status(ResponseStatus.ERROR)
            .message(message)
            .error(error)
            .build();
    }
    
    /**
     * Create partial success response
     */
    public static <T> SearchResponseWrapper<T> partialSuccess(T data, String message) {
        return SearchResponseWrapper.<T>builder()
            .status(ResponseStatus.PARTIAL_SUCCESS)
            .message(message)
            .data(data)
            .build();
    }
}
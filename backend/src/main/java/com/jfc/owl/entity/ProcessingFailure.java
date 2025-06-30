package com.jfc.owl.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 處理失敗記錄實體類
 * 詳細記錄每個失敗項目的資訊
 */
@Entity
@Table(name = "owl_processing_failures", indexes = {
    @Index(name = "idx_pf_batch_id", columnList = "batch_id"),
    @Index(name = "idx_pf_master_code", columnList = "master_item_code"),
    @Index(name = "idx_pf_resolved", columnList = "resolved")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"stackTrace", "processingLog"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProcessingFailure {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    
    @Column(name = "batch_id", nullable = false, length = 50)
    private String batchId;
    
    @Column(name = "master_item_code", nullable = false, length = 50)
    private String masterItemCode;
    
    @Column(name = "error_type", length = 100)
    private String errorType;
    
    @Column(name = "error_message", length = 2000)
    private String errorMessage;
    
    @Lob
    @Column(name = "stack_trace")
    private String stackTrace;
    
    @Column(name = "failure_time", nullable = false)
    @Builder.Default
    private LocalDateTime failureTime = LocalDateTime.now();
    
    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;
    
    @Column(name = "resolved")
    @Builder.Default
    private Boolean resolved = false;
    
    @Column(name = "resolution_notes", length = 1000)
    private String resolutionNotes;
    
    // 關聯到處理記錄
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", referencedColumnName = "batch_id", insertable = false, updatable = false)
    private ProcessingLog processingLog;
    
    // ==================== 錯誤類型枚舉 ====================
    public enum ErrorType {
        FILE_NOT_FOUND("檔案不存在"),
        EXPORT_FAILED("匯出失敗"),
        VALIDATION_FAILED("驗證失敗"),
        DATABASE_ERROR("資料庫錯誤"),
        PARSING_ERROR("解析錯誤"),
        TIMEOUT("逾時"),
        MEMORY_ERROR("記憶體錯誤"),
        NETWORK_ERROR("網路錯誤"),
        PERMISSION_DENIED("權限不足"),
        UNKNOWN("未知錯誤");
        
        private final String description;
        
        ErrorType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // ==================== 輔助方法 ====================
    
    /**
     * 從異常創建失敗記錄
     */
    public static ProcessingFailure fromException(String batchId, String masterItemCode, Exception e) {
        ProcessingFailure failure = new ProcessingFailure();
        failure.setBatchId(batchId);
        failure.setMasterItemCode(masterItemCode);
        failure.setErrorType(determineErrorType(e));
        failure.setErrorMessage(e.getMessage());
        failure.setStackTrace(getStackTraceString(e));
        failure.setFailureTime(LocalDateTime.now());
        return failure;
    }
    
    /**
     * 判斷錯誤類型
     */
    private static String determineErrorType(Throwable e) {
        if (e instanceof java.io.FileNotFoundException) {
            return ErrorType.FILE_NOT_FOUND.name();
        } else if (e instanceof java.util.concurrent.TimeoutException) {
            return ErrorType.TIMEOUT.name();
        } else if (e instanceof OutOfMemoryError) {
            return ErrorType.MEMORY_ERROR.name();
        } else if (e instanceof org.springframework.dao.DataAccessException) {
            return ErrorType.DATABASE_ERROR.name();
        } else if (e.getMessage() != null && e.getMessage().contains("parse")) {
            return ErrorType.PARSING_ERROR.name();
        } else if (e.getMessage() != null && e.getMessage().contains("network")) {
            return ErrorType.NETWORK_ERROR.name();
        } else if (e.getMessage() != null && e.getMessage().contains("permission")) {
            return ErrorType.PERMISSION_DENIED.name();
        } else {
            return ErrorType.UNKNOWN.name();
        }
    }
    
    /**
     * 獲取堆疊追蹤字串
     */
    private static String getStackTraceString(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * 增加重試次數
     */
    public void incrementRetryCount() {
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
    }
    
    /**
     * 標記為已解決
     */
    public void markResolved(String notes) {
        this.resolved = true;
        this.resolutionNotes = notes;
    }
    
    /**
     * 檢查是否可以重試
     */
    public boolean canRetry(int maxRetries) {
        return !resolved && (retryCount == null || retryCount < maxRetries);
    }
    
    /**
     * 獲取簡短的錯誤訊息（用於顯示）
     */
    public String getShortErrorMessage() {
        if (errorMessage == null) return "";
        return errorMessage.length() > 100 ? 
            errorMessage.substring(0, 97) + "..." : errorMessage;
    }
    
    /**
     * 獲取錯誤類型描述
     */
    public String getErrorTypeDescription() {
        try {
            return ErrorType.valueOf(errorType).getDescription();
        } catch (Exception e) {
            return errorType;
        }
    }
    
    /**
     * 檢查是否為暫時性錯誤（可能通過重試解決）
     */
    public boolean isTransientError() {
        return ErrorType.TIMEOUT.name().equals(errorType) ||
               ErrorType.NETWORK_ERROR.name().equals(errorType) ||
               ErrorType.MEMORY_ERROR.name().equals(errorType);
    }
    
    // ==================== JPA Lifecycle Methods ====================
    
    @PrePersist
    public void prePersist() {
        if (failureTime == null) failureTime = LocalDateTime.now();
        if (retryCount == null) retryCount = 0;
        if (resolved == null) resolved = false;
    }
}
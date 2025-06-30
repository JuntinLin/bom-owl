package com.jfc.owl.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 處理記錄實體類
 * 用於追蹤批次處理進度和狀態
 */
@Entity
@Table(name = "owl_processing_log", indexes = {
    @Index(name = "idx_pl_batch_id", columnList = "batch_id"),
    @Index(name = "idx_pl_status", columnList = "status"),
    @Index(name = "idx_pl_start_time", columnList = "start_time")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"errorDetails", "checkpointData"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProcessingLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    
    @Column(name = "batch_id", nullable = false, unique = true, length = 50)
    private String batchId;
    
    @Column(name = "batch_name", length = 255)
    private String batchName;
    
    @Column(name = "total_items", nullable = false)
    @Builder.Default
    private Integer totalItems = 0;
    
    @Column(name = "processed_items")
    @Builder.Default
    private Integer processedItems = 0;
    
    @Column(name = "success_count")
    @Builder.Default
    private Integer successCount = 0;
    
    @Column(name = "failure_count")
    @Builder.Default
    private Integer failureCount = 0;
    
    @Column(name = "skipped_count")
    @Builder.Default
    private Integer skippedCount = 0;
    
    @Column(name = "start_time", nullable = false)
    @Builder.Default
    private LocalDateTime startTime = LocalDateTime.now();
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ProcessingStatus status = ProcessingStatus.INITIALIZING;
    
    @Lob
    @Column(name = "error_details")
    private String errorDetails;
    
    @Lob
    @Column(name = "processing_parameters")
    private String processingParameters;
    
    @Column(name = "average_time_per_item")
    private Double averageTimePerItem;
    
    @Column(name = "estimated_completion_time")
    private LocalDateTime estimatedCompletionTime;
    
    @Column(name = "last_update_time")
    @Builder.Default
    private LocalDateTime lastUpdateTime = LocalDateTime.now();
    
    @Column(name = "initiated_by", length = 100)
    private String initiatedBy;
    
    @Column(name = "notes", length = 1000)
    private String notes;
    
    // 效能統計
    @Column(name = "total_file_size")
    @Builder.Default
    private Long totalFileSize = 0L;
    
    @Column(name = "total_triple_count")
    @Builder.Default
    private Long totalTripleCount = 0L;
    
    @Column(name = "min_processing_time_ms")
    private Long minProcessingTimeMs;
    
    @Column(name = "max_processing_time_ms")
    private Long maxProcessingTimeMs;
    
    // 檢查點支援（用於斷點續傳）
    @Column(name = "last_processed_item_code", length = 50)
    private String lastProcessedItemCode;
    
    @Lob
    @Column(name = "checkpoint_data")
    private String checkpointData;
    
    // 關聯的失敗記錄
    @OneToMany(mappedBy = "processingLog", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProcessingFailure> failures = new ArrayList<>();
    
    // ==================== 處理狀態枚舉 ====================
    public enum ProcessingStatus {
        INITIALIZING("初始化中"),
        PROCESSING("處理中"),
        PAUSED("已暫停"),
        COMPLETED("已完成"),
        FAILED("失敗"),
        CANCELLED("已取消");
        
        private final String description;
        
        ProcessingStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // ==================== 輔助方法 ====================
    
    /**
     * 更新處理進度
     */
    public void updateProgress(int processed, int success, int failed, int skipped) {
        this.processedItems = processed;
        this.successCount = success;
        this.failureCount = failed;
        this.skippedCount = skipped;
        this.lastUpdateTime = LocalDateTime.now();
        
        // 計算平均處理時間
        if (processed > 0) {
            long elapsedMs = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
            this.averageTimePerItem = elapsedMs / (double) processed;
            
            // 估算剩餘時間
            if (totalItems > processed) {
                int remaining = totalItems - processed;
                long estimatedRemainingMs = (long) (remaining * averageTimePerItem);
                this.estimatedCompletionTime = LocalDateTime.now().plusNanos(estimatedRemainingMs * 1_000_000);
            }
        }
    }
    
    /**
     * 標記為完成
     */
    public void markCompleted() {
        this.status = ProcessingStatus.COMPLETED;
        this.endTime = LocalDateTime.now();
        this.lastUpdateTime = LocalDateTime.now();
    }
    
    /**
     * 標記為失敗
     */
    public void markFailed(String errorMessage) {
        this.status = ProcessingStatus.FAILED;
        this.endTime = LocalDateTime.now();
        this.errorDetails = errorMessage;
        this.lastUpdateTime = LocalDateTime.now();
    }
    
    /**
     * 暫停處理
     */
    public void pause() {
        if (this.status == ProcessingStatus.PROCESSING) {
            this.status = ProcessingStatus.PAUSED;
            this.lastUpdateTime = LocalDateTime.now();
        }
    }
    
    /**
     * 恢復處理
     */
    public void resume() {
        if (this.status == ProcessingStatus.PAUSED) {
            this.status = ProcessingStatus.PROCESSING;
            this.lastUpdateTime = LocalDateTime.now();
        }
    }
    
    /**
     * 獲取處理進度百分比
     */
    public double getProgressPercentage() {
        if (totalItems == null || totalItems == 0) return 0.0;
        return (processedItems * 100.0) / totalItems;
    }
    
    /**
     * 獲取處理持續時間（秒）
     */
    public long getDurationSeconds() {
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, end).getSeconds();
    }
    
    /**
     * 獲取成功率
     */
    public double getSuccessRate() {
        if (processedItems == null || processedItems == 0) return 0.0;
        return (successCount * 100.0) / processedItems;
    }
    
    /**
     * 檢查是否可以恢復處理
     */
    public boolean isResumable() {
        return status == ProcessingStatus.PAUSED || 
               (status == ProcessingStatus.FAILED && processedItems < totalItems);
    }
    
    /**
     * 更新效能統計
     */
    public void updatePerformanceStats(long fileSize, long tripleCount, long processingTimeMs) {
        // 更新總計
        this.totalFileSize = (this.totalFileSize == null ? 0 : this.totalFileSize) + fileSize;
        this.totalTripleCount = (this.totalTripleCount == null ? 0 : this.totalTripleCount) + tripleCount;
        
        // 更新最小/最大處理時間
        if (this.minProcessingTimeMs == null || processingTimeMs < this.minProcessingTimeMs) {
            this.minProcessingTimeMs = processingTimeMs;
        }
        if (this.maxProcessingTimeMs == null || processingTimeMs > this.maxProcessingTimeMs) {
            this.maxProcessingTimeMs = processingTimeMs;
        }
    }
    
    /**
     * 設置檢查點
     */
    public void setCheckpoint(String itemCode, String checkpointData) {
        this.lastProcessedItemCode = itemCode;
        this.checkpointData = checkpointData;
        this.lastUpdateTime = LocalDateTime.now();
    }
    
    /**
     * 獲取格式化的持續時間
     */
    public String getFormattedDuration() {
        long seconds = getDurationSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        if (hours > 0) {
            return String.format("%d小時 %d分鐘 %d秒", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%d分鐘 %d秒", minutes, secs);
        } else {
            return String.format("%d秒", secs);
        }
    }
    
    // ==================== JPA Lifecycle Methods ====================
    
    @PrePersist
    public void prePersist() {
    	if (totalItems == null) totalItems = 0;
        if (startTime == null) startTime = LocalDateTime.now();
        if (lastUpdateTime == null) lastUpdateTime = LocalDateTime.now();
        if (status == null) status = ProcessingStatus.INITIALIZING;
        if (processedItems == null) processedItems = 0;
        if (successCount == null) successCount = 0;
        if (failureCount == null) failureCount = 0;
        if (skippedCount == null) skippedCount = 0;
        if (totalFileSize == null) totalFileSize = 0L;
        if (totalTripleCount == null) totalTripleCount = 0L;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.lastUpdateTime = LocalDateTime.now();
    }
}
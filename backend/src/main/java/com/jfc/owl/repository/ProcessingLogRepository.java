package com.jfc.owl.repository;

import com.jfc.owl.entity.ProcessingLog;
import com.jfc.owl.entity.ProcessingLog.ProcessingStatus;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 處理記錄Repository介面
 * 提供批次處理記錄的資料存取操作
 */
@Repository
public interface ProcessingLogRepository extends JpaRepository<ProcessingLog, Long> {
    
    // ==================== 基本查詢方法 ====================
    
    /**
     * 根據批次ID查找處理記錄
     */
    Optional<ProcessingLog> findByBatchId(String batchId);
    
    /**
     * 根據狀態查找處理記錄
     */
    List<ProcessingLog> findByStatus(ProcessingStatus status);
    
    /**
     * 查找處理中的記錄
     */
    List<ProcessingLog> findByStatusIn(List<ProcessingStatus> statuses);
    
    /**
     * 根據發起人查找處理記錄
     */
    List<ProcessingLog> findByInitiatedBy(String initiatedBy);
    
    // ==================== 時間相關查詢 ====================
    
    /**
     * 查找指定時間範圍內的處理記錄
     */
    List<ProcessingLog> findByStartTimeBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 查找最近的處理記錄
     
    @Query(value = "SELECT * FROM (SELECT * FROM owl_processing_log ORDER BY start_time DESC) WHERE ROWNUM <= 10", 
    	       nativeQuery = true)*/
    List<ProcessingLog> findTop10ByOrderByStartTimeDesc();
    
 
    List<ProcessingLog> findAllByOrderByStartTimeDesc(Pageable pageable);
    
    /**
     * 查找未完成的處理記錄（用於恢復）
     */
    @Query("SELECT pl FROM ProcessingLog pl WHERE pl.status IN ('PROCESSING', 'PAUSED') " +
           "AND pl.lastUpdateTime < :cutoffTime ORDER BY pl.startTime DESC")
    List<ProcessingLog> findStalledProcesses(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * 查找可以恢復的處理記錄
     */
    @Query("SELECT pl FROM ProcessingLog pl WHERE pl.status = 'PAUSED' " +
           "OR (pl.status = 'FAILED' AND pl.processedItems < pl.totalItems) " +
           "ORDER BY pl.startTime DESC")
    List<ProcessingLog> findResumableProcesses();
    
    // ==================== 統計查詢方法 ====================
    
    /**
     * 獲取處理統計資訊
     */
    @Query("SELECT " +
           "COUNT(pl) as totalBatches, " +
           "COUNT(CASE WHEN pl.status = 'COMPLETED' THEN 1 END) as completedBatches, " +
           "COUNT(CASE WHEN pl.status = 'FAILED' THEN 1 END) as failedBatches, " +
           "COUNT(CASE WHEN pl.status = 'PROCESSING' THEN 1 END) as processingBatches, " +
           "AVG(pl.averageTimePerItem) as avgTimePerItem, " +
           "SUM(pl.totalItems) as totalItemsProcessed, " +
           "SUM(pl.successCount) as totalSuccessCount " +
           "FROM ProcessingLog pl")
    Object[] getProcessingStatistics();
    
    /**
     * 獲取每日處理統計
     */
    @Query("SELECT DATE(pl.startTime) as date, " +
           "COUNT(pl) as batchCount, " +
           "SUM(pl.totalItems) as totalItems, " +
           "SUM(pl.successCount) as successCount, " +
           "SUM(pl.failureCount) as failureCount " +
           "FROM ProcessingLog pl " +
           "WHERE pl.startTime >= :startDate " +
           "GROUP BY DATE(pl.startTime) " +
           "ORDER BY date DESC")
    List<Object[]> getDailyProcessingStatistics(@Param("startDate") LocalDateTime startDate);
    
    /**
     * 獲取處理效能統計
     */
    @Query("SELECT " +
           "AVG(pl.totalFileSize) as avgFileSize, " +
           "AVG(pl.totalTripleCount) as avgTripleCount, " +
           "MIN(pl.minProcessingTimeMs) as minProcessingTime, " +
           "MAX(pl.maxProcessingTimeMs) as maxProcessingTime, " +
           "AVG(CASE WHEN pl.endTime IS NOT NULL THEN " +
           "    TIMESTAMPDIFF(SECOND, pl.startTime, pl.endTime) END) as avgDurationSeconds " +
           "FROM ProcessingLog pl " +
           "WHERE pl.status = 'COMPLETED'")
    Object[] getPerformanceStatistics();
    
    /**
     * 按狀態統計批次數量
     */
    @Query("SELECT pl.status, COUNT(pl) FROM ProcessingLog pl GROUP BY pl.status")
    List<Object[]> countByStatus();
    
    // ==================== 進階查詢方法 ====================
    
    /**
     * 查找需要清理的舊批次記錄
     */
    @Query("SELECT pl FROM ProcessingLog pl WHERE " +
           "pl.status IN ('COMPLETED', 'FAILED', 'CANCELLED') " +
           "AND pl.endTime < :cutoffDate")
    List<ProcessingLog> findOldCompletedBatches(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * 查找具有失敗項目的批次
     */
    @Query("SELECT pl FROM ProcessingLog pl WHERE pl.failureCount > 0 " +
           "ORDER BY pl.failureCount DESC")
    List<ProcessingLog> findBatchesWithFailures();
    
    /**
     * 查找效能最佳的批次
     */
    @Query("SELECT pl FROM ProcessingLog pl WHERE pl.status = 'COMPLETED' " +
            "AND pl.averageTimePerItem IS NOT NULL " +
            "ORDER BY pl.averageTimePerItem ASC")
    List<ProcessingLog> findTopPerformingBatches();
    
    /**
     * 查找效能最差的批次
     */
    @Query("SELECT pl FROM ProcessingLog pl WHERE pl.status = 'COMPLETED' " +
            "AND pl.averageTimePerItem IS NOT NULL " +
            "ORDER BY pl.averageTimePerItem DESC")
    List<ProcessingLog> findWorstPerformingBatches();
    
    /**
     * 檢查是否有正在處理的批次
     */
    @Query("SELECT COUNT(pl) > 0 FROM ProcessingLog pl WHERE pl.status = 'PROCESSING'")
    boolean hasActiveProcessing();
    
    /**
     * 獲取最後一個完成的批次
     */
    @Query("SELECT pl FROM ProcessingLog pl WHERE pl.status = 'COMPLETED' " +
           "ORDER BY pl.endTime DESC LIMIT 1")
    Optional<ProcessingLog> findLastCompletedBatch();
    
    /**
     * 根據檢查點查找批次（用於斷點續傳）
     */
    @Query("SELECT pl FROM ProcessingLog pl WHERE " +
           "pl.lastProcessedItemCode = :itemCode " +
           "AND pl.status IN ('PAUSED', 'FAILED')")
    List<ProcessingLog> findByCheckpoint(@Param("itemCode") String itemCode);
    
    /**
     * 更新處理進度（原子操作）
     */
    @Query("UPDATE ProcessingLog pl SET " +
           "pl.processedItems = :processedItems, " +
           "pl.successCount = :successCount, " +
           "pl.failureCount = :failureCount, " +
           "pl.lastUpdateTime = CURRENT_TIMESTAMP, " +
           "pl.lastProcessedItemCode = :lastItemCode " +
           "WHERE pl.batchId = :batchId")
    void updateProgress(@Param("batchId") String batchId,
                       @Param("processedItems") int processedItems,
                       @Param("successCount") int successCount,
                       @Param("failureCount") int failureCount,
                       @Param("lastItemCode") String lastItemCode);
    
    
}
package com.jfc.owl.repository;

import com.jfc.owl.entity.ProcessingFailure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 處理失敗記錄Repository介面
 * 提供失敗項目的詳細追蹤和分析
 */
@Repository
public interface ProcessingFailureRepository extends JpaRepository<ProcessingFailure, Long> {
    
    // ==================== 基本查詢方法 ====================
    
    /**
     * 根據批次ID查找失敗記錄
     */
    List<ProcessingFailure> findByBatchId(String batchId);
    
    /**
     * 根據批次ID和料號查找失敗記錄
     */
    Optional<ProcessingFailure> findByBatchIdAndMasterItemCode(String batchId, String masterItemCode);
    
    /**
     * 根據料號查找所有失敗記錄
     */
    List<ProcessingFailure> findByMasterItemCode(String masterItemCode);
    
    /**
     * 查找未解決的失敗記錄
     */
    List<ProcessingFailure> findByResolvedFalse();
    
    /**
     * 查找已解決的失敗記錄
     */
    List<ProcessingFailure> findByResolvedTrue();
    
    // ==================== 錯誤類型查詢 ====================
    
    /**
     * 根據錯誤類型查找失敗記錄
     */
    List<ProcessingFailure> findByErrorType(String errorType);
    
    /**
     * 根據批次ID和錯誤類型查找
     */
    List<ProcessingFailure> findByBatchIdAndErrorType(String batchId, String errorType);
    
    /**
     * 統計各種錯誤類型的數量
     */
    @Query("SELECT pf.errorType, COUNT(pf) FROM ProcessingFailure pf " +
           "GROUP BY pf.errorType ORDER BY COUNT(pf) DESC")
    List<Object[]> countByErrorType();
    
    /**
     * 統計批次中各種錯誤類型的數量
     */
    @Query("SELECT pf.errorType, COUNT(pf) FROM ProcessingFailure pf " +
           "WHERE pf.batchId = :batchId " +
           "GROUP BY pf.errorType ORDER BY COUNT(pf) DESC")
    List<Object[]> countByErrorTypeForBatch(@Param("batchId") String batchId);
    
    // ==================== 重試相關查詢 ====================
    
    /**
     * 查找可以重試的失敗記錄
     */
    @Query("SELECT pf FROM ProcessingFailure pf WHERE " +
           "pf.resolved = false AND pf.retryCount < :maxRetries " +
           "AND pf.batchId = :batchId " +
           "ORDER BY pf.retryCount ASC, pf.failureTime ASC")
    List<ProcessingFailure> findRetryableFailures(@Param("batchId") String batchId, 
                                                  @Param("maxRetries") int maxRetries);
    
    /**
     * 查找暫時性錯誤（可能通過重試解決）
     */
    @Query("SELECT pf FROM ProcessingFailure pf WHERE " +
           "pf.resolved = false " +
           "AND pf.errorType IN ('TIMEOUT', 'NETWORK_ERROR', 'MEMORY_ERROR') " +
           "AND pf.retryCount < :maxRetries")
    List<ProcessingFailure> findTransientErrors(@Param("maxRetries") int maxRetries);
    
    /**
     * 更新重試次數
     */
    @Modifying
    @Transactional
    @Query("UPDATE ProcessingFailure pf SET pf.retryCount = pf.retryCount + 1 " +
           "WHERE pf.id = :id")
    void incrementRetryCount(@Param("id") Long id);
    
    // ==================== 統計分析查詢 ====================
    
    /**
     * 獲取失敗統計資訊
     */
    @Query("SELECT " +
           "COUNT(pf) as totalFailures, " +
           "COUNT(DISTINCT pf.masterItemCode) as uniqueItems, " +
           "COUNT(CASE WHEN pf.resolved = true THEN 1 END) as resolvedCount, " +
           "AVG(pf.retryCount) as avgRetryCount " +
           "FROM ProcessingFailure pf")
    Object[] getFailureStatistics();
    
    /**
     * 獲取批次失敗統計
     */
    @Query("SELECT " +
           "pf.batchId, " +
           "COUNT(pf) as failureCount, " +
           "COUNT(CASE WHEN pf.resolved = true THEN 1 END) as resolvedCount, " +
           "MAX(pf.retryCount) as maxRetryCount " +
           "FROM ProcessingFailure pf " +
           "GROUP BY pf.batchId " +
           "ORDER BY failureCount DESC")
    List<Object[]> getBatchFailureStatistics();
    
    /**
     * 查找最常失敗的項目
     */
    @Query("SELECT pf.masterItemCode, COUNT(pf) as failureCount " +
           "FROM ProcessingFailure pf " +
           "GROUP BY pf.masterItemCode " +
           "HAVING COUNT(pf) > 1 " +
           "ORDER BY failureCount DESC")
    List<Object[]> findFrequentlyFailingItems();
    
    /**
     * 獲取每日失敗趨勢
     */
    @Query("SELECT DATE(pf.failureTime) as date, COUNT(pf) as failureCount " +
           "FROM ProcessingFailure pf " +
           "WHERE pf.failureTime >= :startDate " +
           "GROUP BY DATE(pf.failureTime) " +
           "ORDER BY date")
    List<Object[]> getDailyFailureTrend(@Param("startDate") LocalDateTime startDate);
    
    // ==================== 解決和清理方法 ====================
    
    /**
     * 標記為已解決
     */
    @Modifying
    @Transactional
    @Query("UPDATE ProcessingFailure pf SET " +
           "pf.resolved = true, " +
           "pf.resolutionNotes = :notes " +
           "WHERE pf.id = :id")
    void markAsResolved(@Param("id") Long id, @Param("notes") String notes);
    
    /**
     * 批量標記為已解決
     */
    @Modifying
    @Transactional
    @Query("UPDATE ProcessingFailure pf SET " +
           "pf.resolved = true, " +
           "pf.resolutionNotes = :notes " +
           "WHERE pf.batchId = :batchId AND pf.errorType = :errorType")
    int markBatchAsResolved(@Param("batchId") String batchId, 
                           @Param("errorType") String errorType,
                           @Param("notes") String notes);
    
    /**
     * 刪除舊的已解決記錄
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ProcessingFailure pf WHERE " +
           "pf.resolved = true AND pf.failureTime < :cutoffDate")
    int deleteOldResolvedFailures(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    // ==================== 進階查詢方法 ====================
    
    /**
     * 查找包含特定錯誤訊息的記錄
     */
    @Query("SELECT pf FROM ProcessingFailure pf WHERE " +
           "LOWER(pf.errorMessage) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<ProcessingFailure> findByErrorMessageContaining(@Param("keyword") String keyword);
    
    /**
     * 查找最近的失敗記錄
     */
    List<ProcessingFailure> findTop10ByOrderByFailureTimeDesc();
    
    /**
     * 檢查項目是否在某批次中失敗過
     */
    @Query("SELECT COUNT(pf) > 0 FROM ProcessingFailure pf WHERE " +
           "pf.batchId = :batchId AND pf.masterItemCode = :itemCode")
    boolean hasFailedInBatch(@Param("batchId") String batchId, 
                            @Param("itemCode") String itemCode);
    
    /**
     * 獲取失敗項目的詳細資訊（包含堆疊追蹤）
     */
    @Query("SELECT pf FROM ProcessingFailure pf WHERE pf.id = :id")
    Optional<ProcessingFailure> findByIdWithDetails(@Param("id") Long id);
}
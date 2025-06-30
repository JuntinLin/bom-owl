package com.jfc.owl.repository;

import com.jfc.owl.entity.SystemConfig;
import com.jfc.owl.entity.SystemConfig.ConfigType;
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
 * 系統設定Repository介面
 * 提供系統配置的存取和管理
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {
    
    // ==================== 基本查詢方法 ====================
    
    /**
     * 根據配置鍵查找（主鍵查詢的別名方法）
     */
    default Optional<SystemConfig> findByConfigKey(String configKey) {
        return findById(configKey);
    }
    
    /**
     * 根據配置類型查找
     */
    List<SystemConfig> findByConfigType(ConfigType configType);
    
    /**
     * 查找包含特定關鍵字的配置（在key或description中）
     */
    @Query("SELECT sc FROM SystemConfig sc WHERE " +
           "LOWER(sc.configKey) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(sc.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<SystemConfig> searchConfigs(@Param("keyword") String keyword);
    
    // ==================== 配置值查詢方法 ====================
    
    /**
     * 獲取字串配置值
     */
    @Query("SELECT sc.configValue FROM SystemConfig sc WHERE sc.configKey = :key")
    Optional<String> getConfigValue(@Param("key") String key);
    
    /**
     * 獲取整數配置值
     */
    @Query("SELECT CAST(sc.configValue AS INTEGER) FROM SystemConfig sc " +
           "WHERE sc.configKey = :key AND sc.configType = 'INTEGER'")
    Optional<Integer> getIntegerValue(@Param("key") String key);
    
    /**
     * 獲取布林配置值
     */
    @Query("SELECT CASE WHEN sc.configValue IN ('true', '1') THEN true ELSE false END " +
           "FROM SystemConfig sc WHERE sc.configKey = :key AND sc.configType = 'BOOLEAN'")
    Optional<Boolean> getBooleanValue(@Param("key") String key);
    
    /**
     * 批量獲取配置值
     */
    @Query("SELECT sc FROM SystemConfig sc WHERE sc.configKey IN :keys")
    List<SystemConfig> findByConfigKeys(@Param("keys") List<String> keys);
    
    // ==================== 更新方法 ====================
    
    /**
     * 更新配置值
     */
    @Modifying
    @Transactional
    @Query("UPDATE SystemConfig sc SET " +
           "sc.configValue = :value, " +
           "sc.lastUpdated = CURRENT_TIMESTAMP " +
           "WHERE sc.configKey = :key")
    int updateConfigValue(@Param("key") String key, @Param("value") String value);
    
    /**
     * 批量更新配置值
     */
    @Modifying
    @Transactional
    @Query("UPDATE SystemConfig sc SET " +
           "sc.configValue = CASE sc.configKey " +
           "    WHEN :key1 THEN :value1 " +
           "    WHEN :key2 THEN :value2 " +
           "    ELSE sc.configValue END, " +
           "sc.lastUpdated = CURRENT_TIMESTAMP " +
           "WHERE sc.configKey IN (:key1, :key2)")
    int updateMultipleConfigs(@Param("key1") String key1, @Param("value1") String value1,
                             @Param("key2") String key2, @Param("value2") String value2);
    
    // ==================== 配置組查詢 ====================
    
    /**
     * 根據配置鍵前綴查找（用於獲取相關配置組）
     */
    @Query("SELECT sc FROM SystemConfig sc WHERE sc.configKey LIKE CONCAT(:prefix, '%')")
    List<SystemConfig> findByKeyPrefix(@Param("prefix") String prefix);
    
    /**
     * 獲取所有批次處理相關配置
     */
    default List<SystemConfig> getBatchProcessingConfigs() {
        return findByKeyPrefix("batch.");
    }
    
    /**
     * 獲取所有系統限制相關配置
     */
    default List<SystemConfig> getSystemLimitConfigs() {
        return findByKeyPrefix("limit.");
    }
    
    /**
     * 獲取所有路徑相關配置
     */
    @Query("SELECT sc FROM SystemConfig sc WHERE " +
           "sc.configKey LIKE '%.path' OR sc.configKey LIKE '%.directory'")
    List<SystemConfig> getPathConfigs();
    
    // ==================== 維護和監控方法 ====================
    
    /**
     * 查找過期的配置（長時間未更新）
     */
    @Query("SELECT sc FROM SystemConfig sc WHERE sc.lastUpdated < :cutoffDate")
    List<SystemConfig> findOutdatedConfigs(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * 查找最近更新的配置
     */
    List<SystemConfig> findTop10ByOrderByLastUpdatedDesc();
    
    /**
     * 獲取配置統計資訊
     */
    @Query("SELECT " +
           "COUNT(sc) as totalConfigs, " +
           "COUNT(CASE WHEN sc.configType = 'INTEGER' THEN 1 END) as integerConfigs, " +
           "COUNT(CASE WHEN sc.configType = 'BOOLEAN' THEN 1 END) as booleanConfigs, " +
           "COUNT(CASE WHEN sc.configType = 'STRING' THEN 1 END) as stringConfigs " +
           "FROM SystemConfig sc")
    Object[] getConfigStatistics();
    
    /**
     * 檢查配置是否存在
     */
    @Query("SELECT COUNT(sc) > 0 FROM SystemConfig sc WHERE sc.configKey = :key")
    boolean existsByConfigKey(@Param("key") String key);
    
    // ==================== 特定配置查詢方法 ====================
    
    /**
     * 獲取批次大小配置
     */
    @Query("SELECT CAST(sc.configValue AS INTEGER) FROM SystemConfig sc " +
           "WHERE sc.configKey = 'batch.size'")
    Optional<Integer> getBatchSize();
    
    /**
     * 獲取執行緒池大小配置
     */
    @Query("SELECT CAST(sc.configValue AS INTEGER) FROM SystemConfig sc " +
           "WHERE sc.configKey = 'thread.pool.size'")
    Optional<Integer> getThreadPoolSize();
    
    /**
     * 獲取重試次數配置
     */
    @Query("SELECT CAST(sc.configValue AS INTEGER) FROM SystemConfig sc " +
           "WHERE sc.configKey = 'retry.max.attempts'")
    Optional<Integer> getMaxRetryAttempts();
    
    /**
     * 檢查是否啟用自動恢復
     */
    @Query("SELECT CASE WHEN sc.configValue IN ('true', '1') THEN true ELSE false END " +
           "FROM SystemConfig sc WHERE sc.configKey = 'auto.resume.enabled'")
    Optional<Boolean> isAutoResumeEnabled();
    
    /**
     * 檢查是否處於維護模式
     */
    @Query("SELECT CASE WHEN sc.configValue IN ('true', '1') THEN true ELSE false END " +
           "FROM SystemConfig sc WHERE sc.configKey = 'maintenance.mode'")
    Optional<Boolean> isMaintenanceMode();
    
    // ==================== 配置驗證方法 ====================
    
    /**
     * 查找無效的配置值
     */
    @Query(value = "SELECT sc FROM SystemConfig sc WHERE " +
           "(sc.configType = 'INTEGER' AND sc.configValue NOT REGEXP '^-?[0-9]+$') OR " +
           "(sc.configType = 'BOOLEAN' AND sc.configValue NOT IN ('true', 'false', '1', '0'))",
           nativeQuery = true)
    List<SystemConfig> findInvalidConfigs();
    
    /**
     * 重置配置為預設值
     */
    @Modifying
    @Transactional
    @Query("UPDATE SystemConfig sc SET " +
           "sc.configValue = CASE sc.configKey " +
           "    WHEN 'batch.size' THEN '50' " +
           "    WHEN 'thread.pool.size' THEN '8' " +
           "    WHEN 'timeout.seconds' THEN '120' " +
           "    WHEN 'retry.max.attempts' THEN '3' " +
           "    WHEN 'auto.resume.enabled' THEN 'true' " +
           "    ELSE sc.configValue END, " +
           "sc.lastUpdated = CURRENT_TIMESTAMP " +
           "WHERE sc.configKey IN ('batch.size', 'thread.pool.size', 'timeout.seconds', " +
           "                       'retry.max.attempts', 'auto.resume.enabled')")
    int resetToDefaults();
}
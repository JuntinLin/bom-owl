package com.jfc.owl.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 系統設定實體類
 * 用於儲存系統參數和配置
 */
@Entity
@Table(name = "owl_system_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SystemConfig {
    
    @Id
    @Column(name = "config_key", length = 100)
    @EqualsAndHashCode.Include
    private String configKey;
    
    @Column(name = "config_value", length = 500)
    private String configValue;
    
    @Column(name = "config_type", length = 20)
    @Enumerated(EnumType.STRING)
    private ConfigType configType;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "last_updated")
    @Builder.Default
    private LocalDateTime lastUpdated = LocalDateTime.now();
    
    // ==================== 配置類型枚舉 ====================
    public enum ConfigType {
        STRING("字串"),
        INTEGER("整數"),
        LONG("長整數"),
        DOUBLE("浮點數"),
        BOOLEAN("布林值"),
        JSON("JSON格式"),
        LIST("列表"),
        DATE("日期"),
        DURATION("時間長度");
        
        private final String description;
        
        ConfigType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // ==================== 系統配置鍵值常量 ====================
    public static class Keys {
        public static final String BATCH_SIZE = "batch.size";
        public static final String THREAD_POOL_SIZE = "thread.pool.size";
        public static final String TIMEOUT_SECONDS = "timeout.seconds";
        public static final String RETRY_MAX_ATTEMPTS = "retry.max.attempts";
        public static final String AUTO_RESUME_ENABLED = "auto.resume.enabled";
        public static final String EXPORT_PATH = "export.path";
        public static final String BACKUP_PATH = "backup.path";
        public static final String CLEANUP_DAYS = "cleanup.days";
        public static final String QUALITY_THRESHOLD = "quality.threshold";
        public static final String MAX_FILE_SIZE = "max.file.size";
        public static final String ENABLE_COMPRESSION = "enable.compression";
        public static final String LOG_LEVEL = "log.level";
        public static final String MAINTENANCE_MODE = "maintenance.mode";
        public static final String NOTIFICATION_EMAIL = "notification.email";
        public static final String MONITORING_ENABLED = "monitoring.enabled";
    }
    
    // ==================== 輔助方法 ====================
    
    /**
     * 獲取字串值
     */
    public String getStringValue() {
        return configValue;
    }
    
    /**
     * 獲取整數值
     */
    public Integer getIntegerValue() {
        try {
            return configValue != null ? Integer.parseInt(configValue) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 獲取長整數值
     */
    public Long getLongValue() {
        try {
            return configValue != null ? Long.parseLong(configValue) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 獲取浮點數值
     */
    public Double getDoubleValue() {
        try {
            return configValue != null ? Double.parseDouble(configValue) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 獲取布林值
     */
    public Boolean getBooleanValue() {
        return "true".equalsIgnoreCase(configValue) || "1".equals(configValue);
    }
    
    /**
     * 設置值（自動轉換為字串）
     */
    public void setValue(Object value) {
        if (value == null) {
            this.configValue = null;
        } else {
            this.configValue = value.toString();
        }
        this.lastUpdated = LocalDateTime.now();
    }
    
    /**
     * 創建預設配置
     */
    public static SystemConfig createDefault(String key, Object value, ConfigType type, String description) {
        SystemConfig config = new SystemConfig();
        config.setConfigKey(key);
        config.setValue(value);
        config.setConfigType(type);
        config.setDescription(description);
        config.setLastUpdated(LocalDateTime.now());
        return config;
    }
    
    /**
     * 檢查配置是否過期（超過指定天數未更新）
     */
    public boolean isOutdated(int days) {
        if (lastUpdated == null) return true;
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        return lastUpdated.isBefore(cutoff);
    }
    
    /**
     * 驗證配置值是否符合類型
     */
    public boolean isValidValue() {
        if (configValue == null || configType == null) return true;
        
        try {
            switch (configType) {
                case INTEGER:
                    Integer.parseInt(configValue);
                    return true;
                case LONG:
                    Long.parseLong(configValue);
                    return true;
                case DOUBLE:
                    Double.parseDouble(configValue);
                    return true;
                case BOOLEAN:
                    return "true".equalsIgnoreCase(configValue) || 
                           "false".equalsIgnoreCase(configValue) ||
                           "1".equals(configValue) || "0".equals(configValue);
                case DATE:
                    LocalDateTime.parse(configValue);
                    return true;
                default:
                    return true;
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    // ==================== JPA Lifecycle Methods ====================
    
    @PrePersist
    @PreUpdate
    public void prePersistOrUpdate() {
        this.lastUpdated = LocalDateTime.now();
        
        // 驗證配置值
        if (!isValidValue()) {
            throw new IllegalArgumentException(
                String.format("Invalid value '%s' for config type %s", configValue, configType)
            );
        }
    }
}
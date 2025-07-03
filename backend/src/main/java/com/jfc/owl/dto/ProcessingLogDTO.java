package com.jfc.owl.dto;

import lombok.Data;
import com.jfc.owl.entity.ProcessingLog;
import java.time.LocalDateTime;

/**
 * ProcessingLog 的數據傳輸對象
 * 用於 API 響應，避免懶加載問題
 */
@Data
public class ProcessingLogDTO {
    private Long id;
    private String batchId;
    private String batchName;
    private Integer totalItems;
    private Integer processedItems;
    private Integer successCount;
    private Integer failureCount;
    private Integer skippedCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String processingParameters;
    private Double averageTimePerItem;
    private LocalDateTime estimatedCompletionTime;
    private LocalDateTime lastUpdateTime;
    private String initiatedBy;
    private String notes;
    private Long totalFileSize;
    private Long totalTripleCount;
    private Long minProcessingTimeMs;
    private Long maxProcessingTimeMs;
    private String lastProcessedItemCode;
    
    // 計算欄位
    private Double progressPercentage;
    private Long durationSeconds;
    private Double successRate;
    private Boolean resumable;
    private String formattedDuration;
    
    /**
     * 從實體轉換為 DTO
     */
    public static ProcessingLogDTO fromEntity(ProcessingLog entity) {
        ProcessingLogDTO dto = new ProcessingLogDTO();
        dto.setId(entity.getId());
        dto.setBatchId(entity.getBatchId());
        dto.setBatchName(entity.getBatchName());
        dto.setTotalItems(entity.getTotalItems());
        dto.setProcessedItems(entity.getProcessedItems());
        dto.setSuccessCount(entity.getSuccessCount());
        dto.setFailureCount(entity.getFailureCount());
        dto.setSkippedCount(entity.getSkippedCount());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        dto.setProcessingParameters(entity.getProcessingParameters());
        dto.setAverageTimePerItem(entity.getAverageTimePerItem());
        dto.setEstimatedCompletionTime(entity.getEstimatedCompletionTime());
        dto.setLastUpdateTime(entity.getLastUpdateTime());
        dto.setInitiatedBy(entity.getInitiatedBy());
        dto.setNotes(entity.getNotes());
        dto.setTotalFileSize(entity.getTotalFileSize());
        dto.setTotalTripleCount(entity.getTotalTripleCount());
        dto.setMinProcessingTimeMs(entity.getMinProcessingTimeMs());
        dto.setMaxProcessingTimeMs(entity.getMaxProcessingTimeMs());
        dto.setLastProcessedItemCode(entity.getLastProcessedItemCode());
        
        // 計算欄位
        dto.setProgressPercentage(entity.getProgressPercentage());
        dto.setDurationSeconds(entity.getDurationSeconds());
        dto.setSuccessRate(entity.getSuccessRate());
        dto.setResumable(entity.isResumable());
        dto.setFormattedDuration(entity.getFormattedDuration());
        
        return dto;
    }
}
//OWLKnowledgeBaseController.java - OWL知識庫控制器
package com.jfc.owl.controller;

import com.jfc.owl.entity.OWLKnowledgeBase;
import com.jfc.owl.entity.ProcessingLog;
import com.jfc.owl.dto.ProcessingLogDTO;
import com.jfc.owl.repository.ProcessingLogRepository;
import com.jfc.owl.service.OWLKnowledgeBaseService;
import com.jfc.rdb.common.dto.AbstractDTOController;
import com.jfc.rdb.common.dto.ApiResponse;
import com.jfc.rdb.tiptop.entity.ImaFile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
* OWL知識庫管理控制器
*/
@RestController
@RequestMapping("/owl-knowledge-base")
public class OWLKnowledgeBaseController extends AbstractDTOController<ImaFile> {
	private static final Logger logger = LoggerFactory.getLogger(OWLKnowledgeBaseController.class);

	@Autowired
	private OWLKnowledgeBaseService knowledgeBaseService;

	@Autowired
	private ProcessingLogRepository processingLogRepository;

	/**
	 * 初始化知識庫
	 */
	@PostMapping("/initialize")
	public ResponseEntity<ApiResponse<String>> initializeKnowledgeBase() {
		try {
			knowledgeBaseService.initializeKnowledgeBase();
			return success("Knowledge base initialized successfully");
		} catch (Exception e) {
			logger.error("Error initializing knowledge base", e);
			return error("Failed to initialize knowledge base: " + e.getMessage());
		}
	}

	/**
	 * 匯出單一料件BOM到知識庫
	 */
	@PostMapping("/export-single")
	public ResponseEntity<ApiResponse<OWLKnowledgeBase>> exportSingleToKnowledgeBase(
			@RequestBody Map<String, Object> request) {
		try {
			String masterItemCode = (String) request.get("masterItemCode");
			String format = (String) request.getOrDefault("format", "RDF/XML");
			Boolean includeHierarchy = (Boolean) request.getOrDefault("includeHierarchy", true);
			String description = (String) request.getOrDefault("description", "");

			OWLKnowledgeBase result = knowledgeBaseService.exportAndSaveToKnowledgeBase(masterItemCode, format,
					includeHierarchy, description);

			return success(result);
		} catch (Exception e) {
			logger.error("Error exporting single item to knowledge base", e);
			return error("Failed to export to knowledge base: " + e.getMessage());
		}
	}

	/**
	 * 批量匯出所有BOM到知識庫
	 */
	@PostMapping("/export-all")
	public ResponseEntity<ApiResponse<Map<String, Object>>> exportAllToKnowledgeBase(
			@RequestBody Map<String, Object> request) {
		try {
			String format = (String) request.getOrDefault("format", "RDF/XML");
			Boolean includeHierarchy = (Boolean) request.getOrDefault("includeHierarchy", true);

			Map<String, Object> result = knowledgeBaseService.exportAllBOMsToKnowledgeBase(format, includeHierarchy);

			return success(result);
		} catch (Exception e) {
			logger.error("Error batch exporting to knowledge base", e);
			return error("Failed to batch export to knowledge base: " + e.getMessage());
		}
	}

	/**
	 * 獲取知識庫統計資訊
	 */
	@GetMapping("/statistics")
	public ResponseEntity<ApiResponse<Map<String, Object>>> getKnowledgeBaseStatistics() {
		try {
			Map<String, Object> stats = knowledgeBaseService.getKnowledgeBaseStatistics();
			return success(stats);
		} catch (Exception e) {
			logger.error("Error getting knowledge base statistics", e);
			return error("Failed to get statistics: " + e.getMessage());
		}
	}

	/**
	 * 更新知識庫條目
	 */
	@PutMapping("/update/{masterItemCode}")
	public ResponseEntity<ApiResponse<OWLKnowledgeBase>> updateKnowledgeBaseEntry(@PathVariable String masterItemCode,
			@RequestBody Map<String, String> request) {
		try {
			String description = request.get("description");
			OWLKnowledgeBase result = knowledgeBaseService.updateKnowledgeBaseEntry(masterItemCode, description);
			return success(result);
		} catch (Exception e) {
			logger.error("Error updating knowledge base entry", e);
			return error("Failed to update knowledge base entry: " + e.getMessage());
		}
	}

	/**
	 * 清理知識庫
	 */
	@PostMapping("/cleanup")
	public ResponseEntity<ApiResponse<Map<String, Object>>> cleanupKnowledgeBase() {
		try {
			Map<String, Object> result = knowledgeBaseService.cleanupKnowledgeBase();
			return success(result);
		} catch (Exception e) {
			logger.error("Error cleaning up knowledge base", e);
			return error("Failed to cleanup knowledge base: " + e.getMessage());
		}
	}

	/**
	 * 搜索知識庫條目
	 */
	@GetMapping("/search")
	public ResponseEntity<ApiResponse<List<OWLKnowledgeBase>>> searchKnowledgeBase(@RequestParam String keyword) {
		try {
			List<OWLKnowledgeBase> results = knowledgeBaseService.searchKnowledgeBase(keyword);
			return success(results);
		} catch (Exception e) {
			logger.error("Error searching knowledge base", e);
			return error("Failed to search knowledge base: " + e.getMessage());
		}
	}

	/**
	 * 獲取知識庫中的OWL模型（用於BOM生成）
	 */
	@GetMapping("/model/{masterItemCode}")
	public ResponseEntity<ApiResponse<String>> getKnowledgeBaseModel(@PathVariable String masterItemCode,
			@RequestParam(defaultValue = "RDF/XML") String format) {
		try {
			String modelContent = knowledgeBaseService.getKnowledgeBaseModelAsString(masterItemCode, format);
			return success(modelContent);
		} catch (Exception e) {
			logger.error("Error getting knowledge base model", e);
			return error("Failed to get knowledge base model: " + e.getMessage());
		}
	}

	/**
	 * 獲取批次處理進度 - 使用 DTO
	 */
	@GetMapping("/batch-progress/{batchId}")
	public ResponseEntity<ApiResponse<ProcessingLogDTO>> getBatchProgress(@PathVariable String batchId) {
		try {
			ProcessingLog log = processingLogRepository.findByBatchId(batchId).orElse(null);
			if (log == null) {
				return error("Batch not found: " + batchId);
			}

			// 轉換為 DTO
			ProcessingLogDTO dto = ProcessingLogDTO.fromEntity(log);
			
			// 額外的進度計算（如果需要即時計算）
			if ("PROCESSING".equals(log.getStatus().name()) && log.getProcessedItems() > 0) {
				long elapsedSeconds = java.time.Duration.between(log.getStartTime(), LocalDateTime.now()).getSeconds();
				double avgSecondsPerItem = elapsedSeconds / (double) log.getProcessedItems();
				int remainingItems = log.getTotalItems() - log.getProcessedItems();
				long estimatedRemainingSeconds = (long) (remainingItems * avgSecondsPerItem);
				// 可以在 DTO 中添加這個欄位
			}

			return success(dto);
		} catch (Exception e) {
			logger.error("Error getting batch progress", e);
			return error("Failed to get batch progress: " + e.getMessage());
		}
	}

	/**
	 * 繼續未完成的批次處理
	 */
	@PostMapping("/resume-batch/{batchId}")
	public ResponseEntity<ApiResponse<Map<String, Object>>> resumeBatch(@PathVariable String batchId) {
		try {
			Map<String, Object> result = knowledgeBaseService.resumeBatchExport(batchId);
			return success(result);
		} catch (Exception e) {
			logger.error("Error resuming batch", e);
			return error("Failed to resume batch: " + e.getMessage());
		}
	}

	/**
	 * 暫停批次處理
	 */
	@PostMapping("/pause-batch/{batchId}")
	public ResponseEntity<ApiResponse<Map<String, Object>>> pauseBatch(@PathVariable String batchId) {
		try {
			Map<String, Object> result = knowledgeBaseService.pauseBatchExport(batchId);
			return success(result);
		} catch (Exception e) {
			logger.error("Error pausing batch", e);
			return error("Failed to pause batch: " + e.getMessage());
		}
	}

	/**
	 * 取消批次處理
	 */
	@PostMapping("/cancel-batch/{batchId}")
	public ResponseEntity<ApiResponse<Map<String, Object>>> cancelBatch(@PathVariable String batchId) {
		try {
			Map<String, Object> result = knowledgeBaseService.cancelBatchExport(batchId);
			return success(result);
		} catch (Exception e) {
			logger.error("Error cancelling batch", e);
			return error("Failed to cancel batch: " + e.getMessage());
		}
	}

	/**
	 * 獲取所有批次處理記錄 - 使用 DTO 避免懶加載問題
	 */
	@GetMapping("/batch-history")
	public ResponseEntity<ApiResponse<List<ProcessingLogDTO>>> getBatchHistory(
			@RequestParam(defaultValue = "10") int limit) {
		try {
			logger.info("Fetching batch history with limit: {}", limit);

			List<ProcessingLog> history = new ArrayList<>();
			
			try {
				// 嘗試使用 Spring Data JPA 方法
				history = processingLogRepository.findTop10ByOrderByStartTimeDesc();
				logger.info("Successfully fetched {} batch history records using findTop10 method", history.size());
			} catch (Exception e1) {
				// 如果失敗，嘗試使用 Pageable
				logger.warn("findTop10 method failed, trying with Pageable: {}", e1.getMessage());
				try {
					Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "startTime"));
					history = processingLogRepository.findAll(pageable).getContent();
					logger.info("Successfully fetched {} batch history records using Pageable", history.size());
				} catch (Exception e2) {
					// 如果都失敗，使用 findAll 並在記憶體中限制
					logger.warn("Pageable method failed, trying findAll: {}", e2.getMessage());
					try {
						List<ProcessingLog> allLogs = processingLogRepository.findAll(
							Sort.by(Sort.Direction.DESC, "startTime")
						);
						if (allLogs != null && !allLogs.isEmpty()) {
							history = allLogs.stream()
								.limit(limit)
								.collect(Collectors.toList());
						}
						logger.info("Successfully fetched {} batch history records using findAll", history.size());
					} catch (Exception e3) {
						logger.error("All methods failed, returning empty list: {}", e3.getMessage());
						history = new ArrayList<>();
					}
				}
			}
			
			// 確保不返回 null
			if (history == null) {
				history = new ArrayList<>();
			}
			
			// 轉換為 DTO 列表，避免懶加載問題
			List<ProcessingLogDTO> dtoList = history.stream()
				.map(log -> {
					try {
						// 確保必要欄位不為 null
						if (log.getTotalItems() == null) log.setTotalItems(0);
						if (log.getProcessedItems() == null) log.setProcessedItems(0);
						if (log.getSuccessCount() == null) log.setSuccessCount(0);
						if (log.getFailureCount() == null) log.setFailureCount(0);
						if (log.getSkippedCount() == null) log.setSkippedCount(0);
						if (log.getTotalFileSize() == null) log.setTotalFileSize(0L);
						if (log.getTotalTripleCount() == null) log.setTotalTripleCount(0L);
						
						// 轉換為 DTO
						return ProcessingLogDTO.fromEntity(log);
					} catch (Exception e) {
						logger.error("Error converting ProcessingLog to DTO: {}", e.getMessage());
						// 返回一個基本的 DTO 以避免整個請求失敗
						ProcessingLogDTO errorDto = new ProcessingLogDTO();
						errorDto.setBatchId(log.getBatchId());
						errorDto.setStatus("ERROR");
						errorDto.setNotes("Error converting entity to DTO");
						return errorDto;
					}
				})
				.filter(dto -> dto != null)  // 過濾掉任何 null 值
				.collect(Collectors.toList());
			
			logger.info("Returning {} batch history DTOs", dtoList.size());
			return success(dtoList);
		} catch (Exception e) {
			logger.error("Error getting batch history", e);
			// 返回空列表而不是錯誤，以避免前端崩潰
			return success(new ArrayList<ProcessingLogDTO>());
		}
	}
}
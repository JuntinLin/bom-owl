//OWLKnowledgeBaseController.java - OWL知識庫控制器
package com.jfc.owl.controller;

import com.jfc.owl.entity.OWLKnowledgeBase;
import com.jfc.owl.entity.ProcessingLog;
import com.jfc.owl.repository.ProcessingLogRepository;
import com.jfc.owl.service.OWLKnowledgeBaseService;
import com.jfc.rdb.common.dto.AbstractDTOController;
import com.jfc.rdb.common.dto.ApiResponse;
import com.jfc.rdb.tiptop.entity.ImaFile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	 * 搜索相似BOM用於新產品生成
	 */
	@PostMapping("/search-similar")
	public ResponseEntity<ApiResponse<List<Map<String, Object>>>> searchSimilarBOMs(
			@RequestBody Map<String, String> specifications) {
		try {
			List<Map<String, Object>> similarBOMs = knowledgeBaseService.searchSimilarBOMs(specifications);
			return success(similarBOMs);
		} catch (Exception e) {
			logger.error("Error searching similar BOMs", e);
			return error("Failed to search similar BOMs: " + e.getMessage());
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
	 * 獲取批次處理進度
	 */
	@GetMapping("/batch-progress/{batchId}")
	public ResponseEntity<ApiResponse<Map<String, Object>>> getBatchProgress(@PathVariable String batchId) {
		try {
			ProcessingLog log = processingLogRepository.findByBatchId(batchId).orElse(null);
			if (log == null) {
				return error("Batch not found: " + batchId);
			}

			Map<String, Object> progress = new HashMap<>();
			progress.put("batchId", log.getBatchId());
			progress.put("totalItems", log.getTotalItems());
			progress.put("processedItems", log.getProcessedItems());
			progress.put("successCount", log.getSuccessCount());
			progress.put("failureCount", log.getFailureCount());
			progress.put("status", log.getStatus());
			progress.put("startTime", log.getStartTime());
			progress.put("endTime", log.getEndTime());

			// 計算進度百分比
			if (log.getTotalItems() > 0) {
				progress.put("progressPercentage", (log.getProcessedItems() * 100.0) / log.getTotalItems());
			}

			// 估算剩餘時間
			if ("PROCESSING".equals(log.getStatus()) && log.getProcessedItems() > 0) {
				long elapsedSeconds = java.time.Duration.between(log.getStartTime(), LocalDateTime.now()).getSeconds();
				double avgSecondsPerItem = elapsedSeconds / (double) log.getProcessedItems();
				int remainingItems = log.getTotalItems() - log.getProcessedItems();
				long estimatedRemainingSeconds = (long) (remainingItems * avgSecondsPerItem);
				progress.put("estimatedRemainingSeconds", estimatedRemainingSeconds);
			}

			return success(progress);
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
			// 實現繼續處理邏輯
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
	 * 獲取所有批次處理記錄
	 */
	@GetMapping("/batch-history")
	public ResponseEntity<ApiResponse<List<ProcessingLog>>> getBatchHistory(
			@RequestParam(defaultValue = "10") int limit) {
		try {
			List<ProcessingLog> history = processingLogRepository.findTop10ByOrderByStartTimeDesc();
			return success(history);
		} catch (Exception e) {
			logger.error("Error getting batch history", e);
			return error("Failed to get batch history: " + e.getMessage());
		}
	}
}
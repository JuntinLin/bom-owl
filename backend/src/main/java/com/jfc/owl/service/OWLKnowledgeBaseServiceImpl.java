//OWLKnowledgeBaseService.java - OWL知識庫管理服務
package com.jfc.owl.service;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jfc.owl.entity.OWLKnowledgeBase;
import com.jfc.owl.ontology.HydraulicCylinderOntology;
import com.jfc.owl.repository.OWLKnowledgeBaseRepository;

import jakarta.transaction.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
* OWL知識庫管理服務
* 負責管理、存儲和檢索OWL檔案，作為新產品BOM生成的知識來源
*/
/**
 * Enhanced OWL Knowledge Base Management Service Manages, stores, and retrieves
 * OWL files as knowledge sources for new product BOM generation Integrated with
 * hydraulic cylinder domain ontology
 */
@Service
@Primary // This is the primary implementation
@Profile({ "prod", "staging", "default" }) // Active in production and staging
@Transactional
public class OWLKnowledgeBaseServiceImpl implements OWLKnowledgeBaseService {
	private static final Logger logger = LoggerFactory.getLogger(OWLKnowledgeBaseServiceImpl.class);

	@Value("${owl.knowledge.base.path:./owl-knowledge-base}")
	private String knowledgeBasePath;

	@Value("${owl.knowledge.base.backup.path:./owl-knowledge-base-backup}")
	private String backupPath;

	@Autowired
	private OWLKnowledgeBaseRepository knowledgeBaseRepository;

	@Autowired
	private BomOwlExportService bomOwlExportService;

	@Autowired
	private HydraulicCylinderOntology hydraulicCylinderOntology;

	// 內存中的OWL模型緩存，提高訪問速度
	// Memory cache for OWL models
	private final Map<String, OntModel> modelCache = new ConcurrentHashMap<>();

	// 合併後的主知識庫模型
	// Master knowledge base
	private OntModel masterKnowledgeBase;
	private LocalDateTime lastMasterUpdate;

	/**
	 * 初始化知識庫服務
	 */
	public void initializeKnowledgeBase() {
		logger.info("Initializing OWL Knowledge Base Service");

		// 建立知識庫目錄
		createDirectories();

		// Initialize hydraulic cylinder ontology
		try {
			hydraulicCylinderOntology.initializeHydraulicCylinderOntology();
			logger.info("Hydraulic cylinder ontology initialized successfully");
		} catch (Exception e) {
			logger.warn("Failed to initialize hydraulic cylinder ontology: {}", e.getMessage());
		}

		// 載入現有的OWL檔案
		loadExistingOWLFiles();

		// 建立或更新主知識庫
		updateMasterKnowledgeBase();

		logger.info("OWL Knowledge Base Service initialized successfully");
	}

	/**
	 * 從ERP匯出並保存OWL檔案到知識庫
	 */
	@Override
	public OWLKnowledgeBase exportAndSaveToKnowledgeBase(String masterItemCode, String format, Boolean includeHierarchy,
			String description) {
		logger.info("=== Starting export for item: {} ===", masterItemCode);
		try {
			// 1. Validate input
			if (masterItemCode == null || masterItemCode.trim().isEmpty()) {
				throw new IllegalArgumentException("Master item code cannot be empty");
			}
			// 2. Create directories if not exist
			createDirectories();
			logger.debug("Directories verified/created");

			// 生成檔案名稱 Generate file name
			// 3. Generate file name
			String fileName = generateOWLFileName(masterItemCode, format, includeHierarchy);
			logger.debug("Generated filename: {}", fileName);
			// String filePath = Paths.get(knowledgeBasePath, fileName).toString();
			// String filePath = new File(knowledgeBasePath, fileName).getAbsolutePath();

			// 匯出OWL檔案 Export OWL file from ERP
			// 4. Export OWL file from ERP
			String exportedFilePath;
			try {
				if (includeHierarchy) {
					exportedFilePath = bomOwlExportService.exportCompleteHierarchyBomToOwl(masterItemCode,
							knowledgeBasePath, format);
				} else {
					exportedFilePath = bomOwlExportService.exportBomForMasterItem(masterItemCode, knowledgeBasePath,
							format);
				}
				logger.info("OWL file exported successfully to: {}", exportedFilePath);
			} catch (Exception e) {
				logger.error("Failed to export OWL from ERP for item: {}", masterItemCode, e);
				throw new RuntimeException("ERP export failed: " + e.getMessage(), e);
			}

			// 計算檔案大小和hash Calculate file metrics
			// 5. Verify file exists
			File owlFile = new File(exportedFilePath);
			if (!owlFile.exists()) {
				throw new RuntimeException("Exported file does not exist: " + exportedFilePath);
			}

			// 6. Calculate file metrics
			long fileSize = owlFile.length();
			String fileHash = calculateFileHash(owlFile);
			logger.debug("File size: {}, Hash: {}", fileSize, fileHash);

			// 7. Try to load and validate OWL model
			// 載入OWL模型進行驗證 Load OWL model for validation
			// TEMPORARILY SKIP OWL MODEL VALIDATION
			int tripleCount = 0;
			try {
				OntModel model = loadOWLModel(exportedFilePath);
				tripleCount = (int) model.size();
				// Add to cache
				modelCache.put(masterItemCode, model);
				logger.debug("OWL model loaded successfully, triple count: {}", tripleCount);
			} catch (Exception e) {
				logger.warn("Could not load OWL model for validation, continuing anyway: {}", e.getMessage());
				// Continue without validation
			}

			// 保存到資料庫記錄 Create knowledge base entry
			// 8. Check if entry already exists
			Optional<OWLKnowledgeBase> existing = knowledgeBaseRepository
					.findByMasterItemCodeAndActiveTrue(masterItemCode);

			if (existing.isPresent()) {
				logger.info("Entry already exists for {}, updating...", masterItemCode);
				OWLKnowledgeBase kb = existing.get();
				kb.setActive(false);
				knowledgeBaseRepository.save(kb);
			}

			// 9. Create new knowledge base entry
			OWLKnowledgeBase knowledgeBase = new OWLKnowledgeBase();
			knowledgeBase.setMasterItemCode(masterItemCode);
			knowledgeBase.setFileName(fileName);
			knowledgeBase.setFilePath(exportedFilePath);
			knowledgeBase.setFormat(format);
			knowledgeBase.setIncludeHierarchy(includeHierarchy);
			knowledgeBase.setDescription(description);
			knowledgeBase.setFileSize(fileSize);
			knowledgeBase.setFileHash(fileHash);
			knowledgeBase.setTripleCount(tripleCount);
			knowledgeBase.setCreatedAt(LocalDateTime.now());
			knowledgeBase.setUpdatedAt(LocalDateTime.now());
			knowledgeBase.setActive(true);
			knowledgeBase.setVersion(1);
			knowledgeBase.setSourceSystem("TIPTOP");

			// Check if hydraulic cylinder and add metadata
			// 10. Check if hydraulic cylinder
			if (isHydraulicCylinderItem(masterItemCode)) {
				knowledgeBase.setIsHydraulicCylinder(true);
				Map<String, String> specs = extractSpecificationsFromCode(masterItemCode);
				knowledgeBase.setHydraulicCylinderSpecs(convertSpecsToJson(specs));
				logger.debug("Identified as hydraulic cylinder with specs: {}", specs);
			}

			// 11. Save to database
			try {
				knowledgeBase = knowledgeBaseRepository.save(knowledgeBase);
				logger.info("Successfully saved to database with ID: {}", knowledgeBase.getId());
			} catch (Exception e) {
				logger.error("Failed to save to database: {}", e.getMessage(), e);
				// Try to clean up the file if database save fails
				try {
					owlFile.delete();
				} catch (Exception ex) {
					logger.warn("Could not delete file after failed DB save: {}", ex.getMessage());
				}
				throw new RuntimeException("Database save failed: " + e.getMessage(), e);
			}

			// 更新主知識庫 Update master knowledge base
			// 12. Update master knowledge base (async)
			try {
				updateMasterKnowledgeBase();
			} catch (Exception e) {
				logger.warn("Failed to update master knowledge base: {}", e.getMessage());
				// Don't fail the entire process
			}

			logger.info("=== Export completed successfully for: {} ===", masterItemCode);
			return knowledgeBase;

		} catch (Exception e) {
			logger.error("=== Export failed for item: {} ===", masterItemCode, e);
	        throw new RuntimeException("Failed to export and save OWL file for " + masterItemCode, e);
		}
	}

	/**
	 * 批量匯出所有BOM並保存到知識庫
	 */
	// Enhanced batch export with better progress tracking
	@Override
	public Map<String, Object> exportAllBOMsToKnowledgeBase(String format, Boolean includeHierarchy) {
		logger.info("=== Starting batch export of all BOMs ===");

		Map<String, Object> result = new HashMap<>();
		List<String> successfulExports = new ArrayList<>();
		List<Map<String, String>> failedExports = new ArrayList<>();

		try {
			// Get all master item codes from ERP
			// 獲取所有主料件
			List<String> masterItemCodes = bomOwlExportService.getAllMasterItemCodes("S", "130 HC");
			logger.info("Found {} master items to export", masterItemCodes.size());

			if (masterItemCodes.isEmpty()) {
	            logger.warn("No master items found to export!");
	            result.put("totalItems", 0);
	            result.put("successfulExports", successfulExports);
	            result.put("failedExports", failedExports);
	            result.put("successCount", 0);
	            result.put("failureCount", 0);
	            return result;
	        }
	        
	        // Process in batches to avoid memory issues
	        int batchSize = 10;
			int progressCount = 0;
			
			for (String masterItemCode : masterItemCodes) {
				try {
					logger.info("Processing item {}/{}: {}", 
		                    progressCount + 1, masterItemCodes.size(), masterItemCode);
					exportAndSaveToKnowledgeBase(
							masterItemCode, format, includeHierarchy,
							"Batch export from ERP system");
					successfulExports.add(masterItemCode);
					logger.info("✓ Successfully exported: {}", masterItemCode);
					
				} catch (Exception e) {
					logger.error("✗ Failed to export item: {}", masterItemCode, e);
	                
	                Map<String, String> failureInfo = new HashMap<>();
	                failureInfo.put("itemCode", masterItemCode);
	                failureInfo.put("error", e.getMessage());
	                failureInfo.put("errorType", e.getClass().getSimpleName());
	                failedExports.add(failureInfo);
				}
				progressCount++;
				// Log progress every 10 items
	            if (progressCount % batchSize == 0) {
	                logger.info("=== Progress: {}/{} items processed ({} success, {} failed) ===", 
	                    progressCount, masterItemCodes.size(), 
	                    successfulExports.size(), failedExports.size());
	                
	                // Optional: flush to database
	                knowledgeBaseRepository.flush();
	            }
			}

			// 生成合併的完整知識庫檔案 Generate master knowledge base file
			// Generate master knowledge base file
	        String masterFileName = null;
	        try {
	            masterFileName = generateMasterKnowledgeBaseFile(format);
	            logger.info("Generated master knowledge base file: {}", masterFileName);
	        } catch (Exception e) {
	            logger.error("Failed to generate master knowledge base file", e);
	        }

	        // Prepare detailed results
			result.put("totalItems", masterItemCodes.size());
			result.put("successfulExports", successfulExports);
			result.put("failedExports", failedExports);
			result.put("successCount", successfulExports.size());
			result.put("failureCount", failedExports.size());
			result.put("masterKnowledgeBaseFile", masterFileName);

			// Add hydraulic cylinder specific statistics
			long hydraulicCylinderCount = successfulExports.stream()
					.filter(this::isHydraulicCylinderItem)
		            .count();
			result.put("hydraulicCylinderCount", hydraulicCylinderCount);

			// Add summary
	        result.put("summary", String.format(
	            "Batch export completed: %d/%d successful (%.1f%%), %d hydraulic cylinders",
	            successfulExports.size(), masterItemCodes.size(),
	            (double) successfulExports.size() / masterItemCodes.size() * 100,
	            hydraulicCylinderCount
	        ));
	        
	        logger.info("=== Batch export completed ===");
	        logger.info("Success: {}, Failed: {}", successfulExports.size(), failedExports.size());
	        
	        if (!failedExports.isEmpty()) {
	            logger.error("Failed items details:");
	            failedExports.forEach(failure -> 
	                logger.error("  - {}: {}", failure.get("itemCode"), failure.get("error"))
	            );
	        }
		} catch (Exception e) {
			logger.error("Critical error during batch export", e);
	        result.put("criticalError", e.getMessage());
	        throw new RuntimeException("Batch export failed: " + e.getMessage(), e);
		}

		return result;
	}

	/**
	 * 獲取知識庫中的OWL模型用於BOM生成
	 */
	public OntModel getKnowledgeBaseModel(String masterItemCode) {
		// 首先檢查緩存
		if (modelCache.containsKey(masterItemCode)) {
			return modelCache.get(masterItemCode);
		}

		// 從資料庫查詢
		Optional<OWLKnowledgeBase> knowledgeBase = knowledgeBaseRepository
				.findByMasterItemCodeAndActiveTrue(masterItemCode);

		if (knowledgeBase.isPresent()) {
			try {
				OntModel model = loadOWLModel(knowledgeBase.get().getFilePath());

				// Enhance with hydraulic cylinder knowledge if applicable
				if (knowledgeBase.get().getIsHydraulicCylinder() != null
						&& knowledgeBase.get().getIsHydraulicCylinder()) {
					model = enhanceWithHydraulicCylinderKnowledge(model, masterItemCode);
				}

				modelCache.put(masterItemCode, model);
				return model;
			} catch (Exception e) {
				logger.error("Error loading OWL model for item: {}", masterItemCode, e);
			}
		}

		return null;
	}

	/**
	 * 獲取合併的主知識庫模型
	 */
	public OntModel getMasterKnowledgeBase() {
		// 檢查是否需要更新
		if (masterKnowledgeBase == null || isKnowledgeBaseOutdated()) {
			updateMasterKnowledgeBase();
		}

		return masterKnowledgeBase;
	}

	/**
	 * 搜索相似的BOM結構用於新產品生成
	 */
	public List<Map<String, Object>> searchSimilarBOMs(Map<String, String> specifications) {
		logger.info("Searching for similar BOMs based on specifications");

		List<Map<String, Object>> similarBOMs = new ArrayList<>();

		try {
			// 從知識庫中查詢活躍的OWL檔案
			List<OWLKnowledgeBase> activeKnowledgeBases = knowledgeBaseRepository.findByActiveTrue();

			for (OWLKnowledgeBase kb : activeKnowledgeBases) {
				try {
					OntModel model = getKnowledgeBaseModel(kb.getMasterItemCode());
					if (model != null) {
						double similarity = calculateSimilarityScore(model, specifications, kb);

						if (similarity > 0.3) { // 30%以上相似度
							Map<String, Object> similarBOM = new HashMap<>();
							similarBOM.put("masterItemCode", kb.getMasterItemCode());
							similarBOM.put("fileName", kb.getFileName());
							similarBOM.put("description", kb.getDescription());
							similarBOM.put("similarityScore", Math.round(similarity * 100.0) / 100.0);
							similarBOM.put("createdAt", kb.getCreatedAt());
							similarBOM.put("tripleCount", kb.getTripleCount());
							similarBOM.put("isHydraulicCylinder", kb.getIsHydraulicCylinder());

							if (kb.getIsHydraulicCylinder() != null && kb.getIsHydraulicCylinder()) {
								similarBOM.put("hydraulicCylinderSpecs", kb.getHydraulicCylinderSpecs());
							}

							similarBOMs.add(similarBOM);
						}
					}
				} catch (Exception e) {
					logger.warn("Error processing knowledge base entry: {}", kb.getMasterItemCode(), e);
				}
			}

			// 按相似度排序
			similarBOMs.sort(
					(a, b) -> Double.compare((Double) b.get("similarityScore"), (Double) a.get("similarityScore")));

			// Limit results
			if (similarBOMs.size() > 20) {
				similarBOMs = similarBOMs.subList(0, 20);
			}

			logger.info("Found {} similar BOMs", similarBOMs.size());
		} catch (Exception e) {
			logger.error("Error searching similar BOMs", e);
		}

		return similarBOMs;
	}

	/**
	 * Search similar hydraulic cylinders specifically
	 */
	public List<Map<String, Object>> searchSimilarHydraulicCylinders(Map<String, String> specifications) {
		logger.info("Searching for similar hydraulic cylinders");

		List<Map<String, Object>> similarCylinders = new ArrayList<>();

		try {
			// Query only hydraulic cylinder entries
			List<OWLKnowledgeBase> hydraulicCylinders = knowledgeBaseRepository
					.findByActiveTrueAndIsHydraulicCylinderTrue();

			for (OWLKnowledgeBase kb : hydraulicCylinders) {
				try {
					double similarity = calculateHydraulicCylinderSimilarity(specifications, kb);

					if (similarity > 0.2) { // 20% threshold for hydraulic cylinders
						Map<String, Object> similarCylinder = new HashMap<>();
						similarCylinder.put("masterItemCode", kb.getMasterItemCode());
						similarCylinder.put("fileName", kb.getFileName());
						similarCylinder.put("description", kb.getDescription());
						similarCylinder.put("similarityScore", Math.round(similarity * 100.0) / 100.0);
						similarCylinder.put("hydraulicCylinderSpecs", kb.getHydraulicCylinderSpecs());
						similarCylinder.put("createdAt", kb.getCreatedAt());

						similarCylinders.add(similarCylinder);
					}
				} catch (Exception e) {
					logger.warn("Error processing hydraulic cylinder: {}", kb.getMasterItemCode(), e);
				}
			}

			// Sort by similarity score
			similarCylinders.sort(
					(a, b) -> Double.compare((Double) b.get("similarityScore"), (Double) a.get("similarityScore")));

			logger.info("Found {} similar hydraulic cylinders", similarCylinders.size());

		} catch (Exception e) {
			logger.error("Error searching similar hydraulic cylinders", e);
		}

		return similarCylinders;
	}

	/**
	 * 更新知識庫中的OWL檔案
	 */
	public OWLKnowledgeBase updateKnowledgeBaseEntry(String masterItemCode, String description) {
		logger.info("Updating knowledge base entry for item: {}", masterItemCode);

		Optional<OWLKnowledgeBase> existing = knowledgeBaseRepository.findByMasterItemCodeAndActiveTrue(masterItemCode);

		if (existing.isPresent()) {
			OWLKnowledgeBase kb = existing.get();

			// Re-export 重新匯出
			String format = kb.getFormat();
			boolean includeHierarchy = kb.getIncludeHierarchy();

			// 標記舊版本為非活躍
			kb.setActive(false);
			knowledgeBaseRepository.save(kb);

			// Remove from cache
			modelCache.remove(masterItemCode);

			// 建立新版本
			return exportAndSaveToKnowledgeBase(masterItemCode, format, includeHierarchy, description);
		}

		throw new RuntimeException("Knowledge base entry not found for item: " + masterItemCode);
	}

	/**
	 * 獲取知識庫統計資訊
	 */
	public Map<String, Object> getKnowledgeBaseStatistics() {
		Map<String, Object> stats = new HashMap<>();

		try {
			long totalEntries = knowledgeBaseRepository.countByActiveTrue();
			long totalFileSize = knowledgeBaseRepository.sumFileSizeByActiveTrue();
			long totalTriples = knowledgeBaseRepository.sumTripleCountByActiveTrue();
			long hydraulicCylinderCount = knowledgeBaseRepository.countByActiveTrueAndIsHydraulicCylinderTrue();

			Map<String, Long> formatDistribution = knowledgeBaseRepository.countByFormatAndActiveTrue();

			stats.put("totalEntries", totalEntries);
			stats.put("totalFileSize", totalFileSize);
			stats.put("totalTriples", totalTriples);
			stats.put("hydraulicCylinderCount", hydraulicCylinderCount);
			stats.put("formatDistribution", formatDistribution);
			stats.put("cacheSize", modelCache.size());
			stats.put("lastMasterUpdate", lastMasterUpdate);

			// Calculate percentages
			if (totalEntries > 0) {
				double hydraulicCylinderPercentage = (double) hydraulicCylinderCount / totalEntries * 100;
				stats.put("hydraulicCylinderPercentage", Math.round(hydraulicCylinderPercentage * 100.0) / 100.0);
			}

		} catch (Exception e) {
			logger.error("Error getting knowledge base statistics", e);
		}

		return stats;
	}

	/**
	 * Clean up knowledge base (delete expired or invalid entries) 清理知識庫（刪除過期或無效的條目）
	 */
	public Map<String, Object> cleanupKnowledgeBase() {
		logger.info("Starting knowledge base cleanup");

		Map<String, Object> result = new HashMap<>();
		List<String> deletedEntries = new ArrayList<>();
		List<String> errorEntries = new ArrayList<>();

		try {
			List<OWLKnowledgeBase> allEntries = knowledgeBaseRepository.findAll();

			for (OWLKnowledgeBase entry : allEntries) {
				try {
					File owlFile = new File(entry.getFilePath());

					// 檢查檔案是否存在
					if (!owlFile.exists()) {
						knowledgeBaseRepository.delete(entry);
						deletedEntries.add(entry.getMasterItemCode());
						continue;
					}

					// 檢查檔案是否損壞
					try {
						loadOWLModel(entry.getFilePath());
					} catch (Exception e) {
						logger.warn("Corrupted OWL file detected: {}", entry.getFilePath());
						// 可以選擇刪除或標記為需要重新生成
						entry.setActive(false);
						knowledgeBaseRepository.save(entry);
						errorEntries.add(entry.getMasterItemCode());
					}

				} catch (Exception e) {
					logger.error("Error processing entry: {}", entry.getMasterItemCode(), e);
					errorEntries.add(entry.getMasterItemCode());
				}
			}

			// 清理緩存
			modelCache.clear();

			result.put("deletedEntries", deletedEntries);
			result.put("errorEntries", errorEntries);
			result.put("deletedCount", deletedEntries.size());
			result.put("errorCount", errorEntries.size());

			logger.info("Knowledge base cleanup completed. Deleted: {}, Errors: {}", deletedEntries.size(),
					errorEntries.size());

		} catch (Exception e) {
			logger.error("Error during knowledge base cleanup", e);
			throw new RuntimeException("Knowledge base cleanup failed", e);
		}

		return result;
	}

	// ==================== 私有輔助方法 ====================

	private void createDirectories() {
		try {
			Files.createDirectories(Paths.get(knowledgeBasePath));
			Files.createDirectories(Paths.get(backupPath));
			Files.createDirectories(Paths.get(knowledgeBasePath, "generated"));
			Files.createDirectories(Paths.get(knowledgeBasePath, "hydraulic-cylinders"));
			Files.createDirectories(Paths.get(knowledgeBasePath, "standard-boms"));
		} catch (IOException e) {
			throw new RuntimeException("Failed to create knowledge base directories", e);
		}
	}

	private void loadExistingOWLFiles() {
		try {
			File knowledgeDir = new File(knowledgeBasePath);
			if (knowledgeDir.exists() && knowledgeDir.isDirectory()) {
				File[] owlFiles = knowledgeDir.listFiles(
						(dir, name) -> name.endsWith(".owl") || name.endsWith(".ttl") || name.endsWith(".jsonld"));

				if (owlFiles != null) {
					logger.info("Found {} existing OWL files in knowledge base", owlFiles.length);
				}
			}
		} catch (Exception e) {
			logger.warn("Error loading existing OWL files", e);
		}
	}

	private void updateMasterKnowledgeBase() {
		logger.info("Updating master knowledge base");

		try {
			masterKnowledgeBase = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);

			// Add hydraulic cylinder ontology as base
			try {
				masterKnowledgeBase.add(hydraulicCylinderOntology.getOntologyModel());
				logger.debug("Added hydraulic cylinder ontology to master knowledge base");
			} catch (Exception e) {
				logger.warn("Failed to add hydraulic cylinder ontology to master: {}", e.getMessage());
			}

			// 合併所有活躍的知識庫條目
			List<OWLKnowledgeBase> activeEntries = knowledgeBaseRepository.findByActiveTrue();

			for (OWLKnowledgeBase entry : activeEntries) {
				try {
					OntModel model = loadOWLModel(entry.getFilePath());
					masterKnowledgeBase.add(model);
				} catch (Exception e) {
					logger.warn("Error adding model to master knowledge base: {}", entry.getFilePath(), e);
				}
			}

			lastMasterUpdate = LocalDateTime.now();

			// 保存合併後的主知識庫檔案
			String masterFilePath = Paths.get(knowledgeBasePath, "master_knowledge_base.owl").toString();
			try (FileOutputStream fos = new FileOutputStream(masterFilePath)) {
				masterKnowledgeBase.write(fos, "RDF/XML");
			}

			logger.info("Master knowledge base updated with {} models", activeEntries.size());

		} catch (Exception e) {
			logger.error("Error updating master knowledge base", e);
		}
	}

	private boolean isKnowledgeBaseOutdated() {
		if (lastMasterUpdate == null)
			return true;

		// 檢查是否有新的更新（這裡可以根據需要調整策略）
		LocalDateTime latestUpdate = knowledgeBaseRepository.findLatestUpdateTime();
		return latestUpdate != null && latestUpdate.isAfter(lastMasterUpdate);
	}

	private boolean isHydraulicCylinderItem(String itemCode) {
		return itemCode != null && itemCode.length() >= 2 && (itemCode.startsWith("3") || itemCode.startsWith("4"));
	}

	private Map<String, String> extractSpecificationsFromCode(String itemCode) {
		Map<String, String> specs = new HashMap<>();

		if (itemCode == null || itemCode.length() < 8) {
			return specs;
		}

		try {
			// Extract series (positions 3-4)
			if (itemCode.length() >= 4) {
				specs.put("series", itemCode.substring(2, 4));
			}

			// Extract bore (positions 6-8)
			if (itemCode.length() >= 8) {
				String boreStr = itemCode.substring(5, 8);
				specs.put("bore", String.valueOf(Integer.parseInt(boreStr)));
			}

			// Extract stroke (positions 11-14)
			if (itemCode.length() >= 14) {
				String strokeStr = itemCode.substring(10, 14);
				specs.put("stroke", String.valueOf(Integer.parseInt(strokeStr)));
			}

			// Extract rod end type (position 15)
			if (itemCode.length() >= 15) {
				specs.put("rodEndType", itemCode.substring(14, 15));
			}

		} catch (Exception e) {
			logger.warn("Error extracting specifications from code {}: {}", itemCode, e.getMessage());
		}

		return specs;
	}

	private OntModel enhanceWithHydraulicCylinderKnowledge(OntModel baseModel, String masterItemCode) {
		try {
			OntModel enhancedModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
			enhancedModel.add(baseModel);
			enhancedModel.add(hydraulicCylinderOntology.getOntologyModel());

			// Create hydraulic cylinder individual with specifications
			Map<String, String> specs = extractSpecificationsFromCode(masterItemCode);
			if (!specs.isEmpty()) {
				hydraulicCylinderOntology.createHydraulicCylinderIndividual(masterItemCode, specs);
			}

			return enhancedModel;
		} catch (Exception e) {
			logger.warn("Failed to enhance with hydraulic cylinder knowledge: {}", e.getMessage());
			return baseModel;
		}
	}

	private String generateOWLFileName(String masterItemCode, String format, boolean includeHierarchy) {
		String timestamp = LocalDateTime.now().toString().replaceAll("[^0-9]", "");
		String prefix = includeHierarchy ? "complete" : "simple";
		String extension = getFileExtension(format);

		// Ensure no special characters in filename
		String safeItemCode = masterItemCode.replaceAll("[^a-zA-Z0-9-_]", "_");

		return String.format("%s_%s_%s.%s", prefix, safeItemCode, timestamp, extension);
	}

	private String generateMasterKnowledgeBaseFile(String format) throws IOException {
		String fileName = "master_knowledge_base_" + LocalDateTime.now().toString().replaceAll("[^0-9]", "") + "."
				+ getFileExtension(format);
		String filePath = Paths.get(knowledgeBasePath, fileName).toString();

		try (FileOutputStream fos = new FileOutputStream(filePath)) {
			masterKnowledgeBase.write(fos, format);
		}

		return fileName;
	}

	private String getFileExtension(String format) {
		switch (format.toUpperCase()) {
		case "TURTLE":
		case "TTL":
			return "ttl";
		case "JSON-LD":
		case "JSONLD":
			return "jsonld";
		case "N-TRIPLES":
		case "NT":
			return "nt";
		default:
			return "owl";
		}
	}

	private OntModel loadOWLModel(String filePath) {
		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		// Fix the file path for Jena
		try {
			// Convert to proper file URI
			File file = new File(filePath);
			String fileURI = file.toURI().toString();

			// Use Jena's RDFDataMgr instead of FileManager for better path handling
			model.read(fileURI);

			return model;
		} catch (Exception e) {
			logger.error("Error loading OWL model from: {}", filePath, e);
			throw new RuntimeException("Failed to load OWL model", e);
		}
	}

	private String calculateFileHash(File file) {
		// 實現檔案hash計算（MD5或SHA-256）
		try {
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
			byte[] data = Files.readAllBytes(file.toPath());
			byte[] hash = md.digest(data);

			StringBuilder sb = new StringBuilder();
			for (byte b : hash) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (Exception e) {
			logger.warn("Error calculating file hash", e);
			return null;
		}
	}

	private double calculateSimilarityScore(OntModel model, Map<String, String> specifications, OWLKnowledgeBase kb) {
		// 實現相似度計算邏輯
		// 這裡可以基於本體中的屬性值進行比較
		// Enhanced similarity calculation with hydraulic cylinder support
		double score = 0.0;

		try {
			// 提取模型中的規格資訊並與目標規格比較
			// ... 具體實現邏輯
			// If both are hydraulic cylinders, use specialized comparison
			if (kb.getIsHydraulicCylinder() != null && kb.getIsHydraulicCylinder()
					&& specifications.containsKey("series")) {

				score = calculateHydraulicCylinderSimilarity(specifications, kb);
			} else {
				// General BOM similarity calculation
				score = calculateGeneralBomSimilarity(model, specifications);
			}

		} catch (Exception e) {
			logger.warn("Error calculating similarity score", e);
		}

		return score;
	}

	private double calculateHydraulicCylinderSimilarity(Map<String, String> targetSpecs, OWLKnowledgeBase kb) {
		try {
			// Parse existing specs from knowledge base
			Map<String, String> existingSpecs = parseSpecifications(kb.getHydraulicCylinderSpecs());

			double score = 0.0;
			int factors = 0;

			// Series similarity (30% weight)
			if (targetSpecs.containsKey("series") && existingSpecs.containsKey("series")) {
				if (targetSpecs.get("series").equals(existingSpecs.get("series"))) {
					score += 0.3;
				}
				factors++;
			}

			// Bore similarity (35% weight)
			if (targetSpecs.containsKey("bore") && existingSpecs.containsKey("bore")) {
				score += calculateBoreSimilarity(targetSpecs.get("bore"), existingSpecs.get("bore")) * 0.35;
				factors++;
			}

			// Stroke similarity (25% weight)
			if (targetSpecs.containsKey("stroke") && existingSpecs.containsKey("stroke")) {
				score += calculateStrokeSimilarity(targetSpecs.get("stroke"), existingSpecs.get("stroke")) * 0.25;
				factors++;
			}

			// Rod end type similarity (10% weight)
			if (targetSpecs.containsKey("rodEndType") && existingSpecs.containsKey("rodEndType")) {
				if (targetSpecs.get("rodEndType").equals(existingSpecs.get("rodEndType"))) {
					score += 0.1;
				}
				factors++;
			}

			return factors > 0 ? score : 0.0;

		} catch (Exception e) {
			logger.warn("Error calculating hydraulic cylinder similarity", e);
			return 0.0;
		}
	}

	private double calculateGeneralBomSimilarity(OntModel model, Map<String, String> specifications) {
		// General BOM similarity calculation logic
		// This can be enhanced based on specific requirements
		return 0.5; // Default similarity for general BOMs
	}

	private Map<String, String> parseSpecifications(String specsString) {
		Map<String, String> specs = new HashMap<>();

		if (specsString == null || specsString.isEmpty()) {
			return specs;
		}

		try {
			// Parse the specifications string (assuming key=value format)
			String[] pairs = specsString.replaceAll("[{}]", "").split(",");
			for (String pair : pairs) {
				String[] keyValue = pair.trim().split("=");
				if (keyValue.length == 2) {
					specs.put(keyValue[0].trim(), keyValue[1].trim());
				}
			}
		} catch (Exception e) {
			logger.warn("Error parsing specifications: {}", specsString, e);
		}

		return specs;
	}

	private double calculateBoreSimilarity(String bore1, String bore2) {
		try {
			int b1 = Integer.parseInt(bore1);
			int b2 = Integer.parseInt(bore2);

			int diff = Math.abs(b1 - b2);

			if (diff == 0)
				return 1.0;
			if (diff <= 5)
				return 0.9;
			if (diff <= 10)
				return 0.7;
			if (diff <= 20)
				return 0.5;
			if (diff <= 50)
				return 0.3;

			return 0.0;

		} catch (NumberFormatException e) {
			return bore1.equals(bore2) ? 1.0 : 0.0;
		}
	}

	private double calculateStrokeSimilarity(String stroke1, String stroke2) {
		try {
			int s1 = Integer.parseInt(stroke1);
			int s2 = Integer.parseInt(stroke2);

			int diff = Math.abs(s1 - s2);

			if (diff == 0)
				return 1.0;
			if (diff <= 10)
				return 0.9;
			if (diff <= 25)
				return 0.7;
			if (diff <= 50)
				return 0.5;
			if (diff <= 100)
				return 0.3;

			return 0.0;

		} catch (NumberFormatException e) {
			return stroke1.equals(stroke2) ? 1.0 : 0.0;
		}
	}

	/**
	 * Save a generated BOM to the knowledge base for future learning This method
	 * saves BOMs that were generated by the AI system (not from ERP)
	 * 
	 * @param newItemCode       The item code for the generated BOM
	 * @param generatedBOMModel The OWL model containing the generated BOM
	 * @param description       Description of the generated BOM
	 * @return The saved OWLKnowledgeBase entity
	 */
	public OWLKnowledgeBase saveGeneratedBOMToKnowledgeBase(String newItemCode, OntModel generatedBOMModel,
			String description) {
		logger.info("Saving generated BOM to knowledge base: {}", newItemCode);

		try {
			// Generate unique filename for generated BOM
			String fileName = generateGeneratedBOMFileName(newItemCode);
			String filePath = Paths.get(knowledgeBasePath, "generated", fileName).toString();

			// Create generated subfolder if it doesn't exist
			Files.createDirectories(Paths.get(knowledgeBasePath, "generated"));

			// Save the OWL model to file
			try (FileOutputStream fos = new FileOutputStream(filePath)) {
				generatedBOMModel.write(fos, "RDF/XML");
			}

			// Calculate file metrics
			File owlFile = new File(filePath);
			long fileSize = owlFile.length();
			String fileHash = calculateFileHash(owlFile);
			int tripleCount = (int) generatedBOMModel.size();

			// Create knowledge base entry
			OWLKnowledgeBase knowledgeBase = OWLKnowledgeBase.builder().masterItemCode(newItemCode).fileName(fileName)
					.filePath(filePath).format("RDF/XML").includeHierarchy(true).description(description)
					.fileSize(fileSize).fileHash(fileHash).tripleCount(tripleCount).active(true).version(1)
					.sourceSystem("AI_GENERATED") // Mark as AI generated
					.validationStatus("PENDING_VALIDATION") // Needs validation
					.tags("GENERATED,PREDICTIVE,UNVALIDATED").build();

			// Check if hydraulic cylinder
			if (OWLKnowledgeBase.isHydraulicCylinderCode(newItemCode)) {
				knowledgeBase.setIsHydraulicCylinder(true);
				Map<String, String> specs = extractSpecificationsFromCode(newItemCode);
				if (!specs.isEmpty()) {
					knowledgeBase.setHydraulicCylinderSpecs(convertSpecsToJson(specs));
				}
			}

			// Extract component count from the model
			int componentCount = extractComponentCount(generatedBOMModel);
			knowledgeBase.setComponentCount(componentCount);

			// Calculate initial quality score (lower for generated BOMs)
			knowledgeBase.setQualityScore(0.3); // Start with lower score for unvalidated BOMs

			// Save to repository
			knowledgeBase = knowledgeBaseRepository.save(knowledgeBase);

			// Add to cache with special marker
			String cacheKey = "GENERATED_" + newItemCode;
			modelCache.put(cacheKey, generatedBOMModel);

			logger.info("Generated BOM saved successfully: {} ({})", fileName, fileSize);
			return knowledgeBase;

		} catch (Exception e) {
			logger.error("Error saving generated BOM to knowledge base: {}", newItemCode, e);
			throw new RuntimeException("Failed to save generated BOM", e);
		}
	}

	/**
	 * Update a generated BOM after validation with actual ERP data This promotes a
	 * generated BOM to validated status
	 */
	public OWLKnowledgeBase validateGeneratedBOM(String itemCode, boolean isValid, String validationNotes) {
		logger.info("Validating generated BOM: {} - Valid: {}", itemCode, isValid);

		Optional<OWLKnowledgeBase> existing = knowledgeBaseRepository.findByMasterItemCodeAndActiveTrue(itemCode);

		if (existing.isPresent()) {
			OWLKnowledgeBase kb = existing.get();

			// Check if this is a generated BOM
			if (!"AI_GENERATED".equals(kb.getSourceSystem())) {
				throw new IllegalArgumentException("Item is not a generated BOM: " + itemCode);
			}

			if (isValid) {
				kb.setValidationStatus("VALIDATED");
				kb.setQualityScore(0.8); // Increase quality score for validated BOMs
				kb.setTags("GENERATED,VALIDATED,PREDICTIVE");
			} else {
				kb.setValidationStatus("INVALID");
				kb.setQualityScore(0.1); // Lower score for invalid predictions
				kb.setTags("GENERATED,INVALID,LEARNING");
			}

			kb.setErrorMessages(validationNotes);
			kb.setUpdatedAt(LocalDateTime.now());

			return knowledgeBaseRepository.save(kb);
		}

		throw new RuntimeException("Generated BOM not found: " + itemCode);
	}

	/**
	 * Search for generated BOMs that need validation
	 */
	public List<OWLKnowledgeBase> findGeneratedBOMsNeedingValidation() {
		return knowledgeBaseRepository.findAll().stream().filter(kb -> "AI_GENERATED".equals(kb.getSourceSystem()))
				.filter(kb -> "PENDING_VALIDATION".equals(kb.getValidationStatus())).filter(kb -> kb.getActive())
				.sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())).collect(Collectors.toList());
	}

	/**
	 * Get statistics about generated BOMs
	 */
	public Map<String, Object> getGeneratedBOMStatistics() {
		Map<String, Object> stats = new HashMap<>();

		List<OWLKnowledgeBase> generatedBOMs = knowledgeBaseRepository.findAll().stream()
				.filter(kb -> "AI_GENERATED".equals(kb.getSourceSystem())).filter(kb -> kb.getActive())
				.collect(Collectors.toList());

		long totalGenerated = generatedBOMs.size();
		long validated = generatedBOMs.stream().filter(kb -> "VALIDATED".equals(kb.getValidationStatus())).count();
		long invalid = generatedBOMs.stream().filter(kb -> "INVALID".equals(kb.getValidationStatus())).count();
		long pending = generatedBOMs.stream().filter(kb -> "PENDING_VALIDATION".equals(kb.getValidationStatus()))
				.count();

		double validationRate = totalGenerated > 0 ? (double) validated / totalGenerated : 0.0;

		stats.put("totalGenerated", totalGenerated);
		stats.put("validated", validated);
		stats.put("invalid", invalid);
		stats.put("pendingValidation", pending);
		stats.put("validationRate", Math.round(validationRate * 100.0) / 100.0);

		// Calculate average quality scores
		double avgQualityScore = generatedBOMs.stream()
				.mapToDouble(kb -> kb.getQualityScore() != null ? kb.getQualityScore() : 0.0).average().orElse(0.0);

		stats.put("averageQualityScore", Math.round(avgQualityScore * 100.0) / 100.0);

		return stats;
	}

	// Helper methods

	private String generateGeneratedBOMFileName(String itemCode) {
		String timestamp = LocalDateTime.now().toString().replaceAll("[^0-9]", "");
		return String.format("generated_%s_%s.owl", itemCode, timestamp);
	}

	private int extractComponentCount(OntModel model) {
		try {
			// Count individuals that represent components
			// We count individuals that have at least one OWL class type
			return (int) model.listIndividuals().toList().stream().filter(ind -> {
				// Check if the individual has any class types
				return ind.listOntClasses(true).hasNext();
			}).count();
		} catch (Exception e) {
			logger.warn("Error extracting component count: {}", e.getMessage());
			return 0;
		}
	}

	private String convertSpecsToJson(Map<String, String> specs) {
		if (specs == null || specs.isEmpty()) {
			return "{}";
		}
		try {
			// Simple JSON conversion
			StringBuilder json = new StringBuilder("{");
			boolean first = true;
			for (Map.Entry<String, String> entry : specs.entrySet()) {
				if (!first)
					json.append(",");
				json.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
				first = false;
			}
			json.append("}");
			return json.toString();
		} catch (Exception e) {
			logger.warn("Error converting specs to JSON: {}", e.getMessage());
			return "{}";
		}
	}

	/**
	 * Clean up old or invalid generated BOMs
	 */
	public Map<String, Object> cleanupGeneratedBOMs(int daysOld) {
		logger.info("Cleaning up generated BOMs older than {} days", daysOld);

		Map<String, Object> result = new HashMap<>();
		List<String> deletedItems = new ArrayList<>();

		LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);

		List<OWLKnowledgeBase> oldGeneratedBOMs = knowledgeBaseRepository.findAll().stream()
				.filter(kb -> "AI_GENERATED".equals(kb.getSourceSystem()))
				.filter(kb -> kb.getCreatedAt().isBefore(cutoffDate))
				.filter(kb -> !"VALIDATED".equals(kb.getValidationStatus())).collect(Collectors.toList());

		for (OWLKnowledgeBase kb : oldGeneratedBOMs) {
			try {
				// Delete file
				File file = new File(kb.getFilePath());
				if (file.exists()) {
					file.delete();
				}

				// Remove from cache
				modelCache.remove("GENERATED_" + kb.getMasterItemCode());

				// Delete from database
				knowledgeBaseRepository.delete(kb);
				deletedItems.add(kb.getMasterItemCode());

			} catch (Exception e) {
				logger.warn("Error deleting generated BOM: {}", kb.getMasterItemCode(), e);
			}
		}

		result.put("deletedCount", deletedItems.size());
		result.put("deletedItems", deletedItems);

		logger.info("Cleaned up {} generated BOMs", deletedItems.size());
		return result;
	}

	/**
	 * 搜索知識庫條目 Search knowledge base entries by keyword
	 * 
	 * @param keyword 搜索關鍵字
	 * @return 符合條件的知識庫條目列表
	 */
	public List<OWLKnowledgeBase> searchKnowledgeBase(String keyword) {
		logger.info("Searching knowledge base with keyword: {}", keyword);

		if (keyword == null || keyword.trim().isEmpty()) {
			return new ArrayList<>();
		}

		try {
			// Search in multiple fields
			String searchPattern = "%" + keyword.toLowerCase() + "%";

			List<OWLKnowledgeBase> results = knowledgeBaseRepository.findAll().stream().filter(kb -> kb.getActive())
					.filter(kb -> {
						// Search in master item code
						if (kb.getMasterItemCode() != null
								&& kb.getMasterItemCode().toLowerCase().contains(keyword.toLowerCase())) {
							return true;
						}

						// Search in description
						if (kb.getDescription() != null
								&& kb.getDescription().toLowerCase().contains(keyword.toLowerCase())) {
							return true;
						}

						// Search in file name
						if (kb.getFileName() != null
								&& kb.getFileName().toLowerCase().contains(keyword.toLowerCase())) {
							return true;
						}

						// Search in tags
						if (kb.getTags() != null && kb.getTags().toLowerCase().contains(keyword.toLowerCase())) {
							return true;
						}

						// Search in hydraulic cylinder specs if applicable
						if (kb.getIsHydraulicCylinder() != null && kb.getIsHydraulicCylinder()
								&& kb.getHydraulicCylinderSpecs() != null
								&& kb.getHydraulicCylinderSpecs().toLowerCase().contains(keyword.toLowerCase())) {
							return true;
						}

						return false;
					}).sorted((a, b) -> {
						// Sort by relevance (items where keyword matches code come first)
						boolean aCodeMatch = a.getMasterItemCode() != null
								&& a.getMasterItemCode().toLowerCase().contains(keyword.toLowerCase());
						boolean bCodeMatch = b.getMasterItemCode() != null
								&& b.getMasterItemCode().toLowerCase().contains(keyword.toLowerCase());

						if (aCodeMatch && !bCodeMatch)
							return -1;
						if (!aCodeMatch && bCodeMatch)
							return 1;

						// Then sort by updated date
						return b.getUpdatedAt().compareTo(a.getUpdatedAt());
					}).collect(Collectors.toList());

			logger.info("Found {} results for keyword: {}", results.size(), keyword);
			return results;

		} catch (Exception e) {
			logger.error("Error searching knowledge base", e);
			return new ArrayList<>();
		}
	}

	/**
	 * 獲取知識庫中的OWL模型內容作為字符串（用於BOM生成） Get knowledge base model content as string
	 * 
	 * @param masterItemCode 主料件代碼
	 * @param format         輸出格式 (RDF/XML, TURTLE, JSON-LD, N-TRIPLES)
	 * @return OWL模型的字符串表示
	 */
	public String getKnowledgeBaseModelAsString(String masterItemCode, String format) {
		logger.info("Getting knowledge base model as string for item: {} in format: {}", masterItemCode, format);

		try {
			// Get the OWL model
			OntModel model = getKnowledgeBaseModel(masterItemCode);

			if (model == null) {
				// Try to find in the database
				Optional<OWLKnowledgeBase> knowledgeBase = knowledgeBaseRepository
						.findByMasterItemCodeAndActiveTrue(masterItemCode);

				if (knowledgeBase.isPresent()) {
					// Read from file
					String filePath = knowledgeBase.get().getFilePath();
					File owlFile = new File(filePath);

					if (owlFile.exists()) {
						// If the file is already in the requested format, return it directly
						if (isFileInFormat(filePath, format)) {
							return new String(Files.readAllBytes(owlFile.toPath()), StandardCharsets.UTF_8);
						}

						// Otherwise, load and convert
						model = loadOWLModel(filePath);
					}
				}

				if (model == null) {
					throw new RuntimeException("Model not found for item: " + masterItemCode);
				}
			}

			// Convert model to string in requested format
			StringWriter writer = new StringWriter();

			// Determine the Jena format string
			String jenaFormat = getJenaFormatString(format);

			// Write the model
			model.write(writer, jenaFormat);

			String result = writer.toString();
			logger.debug("Model converted to {} format, size: {} characters", format, result.length());

			return result;

		} catch (Exception e) {
			logger.error("Error getting knowledge base model as string for item: {}", masterItemCode, e);
			throw new RuntimeException("Failed to get knowledge base model as string", e);
		}
	}

	// Helper method to check if file is already in the requested format
	private boolean isFileInFormat(String filePath, String format) {
		String extension = filePath.substring(filePath.lastIndexOf('.') + 1).toLowerCase();
		String expectedExtension = getFileExtension(format);
		return extension.equals(expectedExtension);
	}

	// Helper method to convert format string to Jena format
	private String getJenaFormatString(String format) {
		switch (format.toUpperCase()) {
		case "TURTLE":
		case "TTL":
			return "TURTLE";
		case "JSON-LD":
		case "JSONLD":
			return "JSON-LD";
		case "N-TRIPLES":
		case "NT":
			return "N-TRIPLE";
		case "N3":
			return "N3";
		case "RDF/XML":
		case "RDF":
		case "XML":
		default:
			return "RDF/XML";
		}
	}

	// Additional helper method to get knowledge base entries by criteria
	public List<OWLKnowledgeBase> getKnowledgeBaseEntriesByCriteria(Map<String, Object> criteria) {
		logger.info("Getting knowledge base entries by criteria: {}", criteria);

		List<OWLKnowledgeBase> results = knowledgeBaseRepository.findByActiveTrue();

		// Apply filters based on criteria
		if (criteria.containsKey("format")) {
			String format = (String) criteria.get("format");
			results = results.stream().filter(kb -> format.equalsIgnoreCase(kb.getFormat()))
					.collect(Collectors.toList());
		}

		if (criteria.containsKey("includeHierarchy")) {
			Boolean includeHierarchy = (Boolean) criteria.get("includeHierarchy");
			results = results.stream().filter(kb -> includeHierarchy.equals(kb.getIncludeHierarchy()))
					.collect(Collectors.toList());
		}

		if (criteria.containsKey("isHydraulicCylinder")) {
			Boolean isHydraulicCylinder = (Boolean) criteria.get("isHydraulicCylinder");
			results = results.stream().filter(kb -> isHydraulicCylinder.equals(kb.getIsHydraulicCylinder()))
					.collect(Collectors.toList());
		}

		if (criteria.containsKey("minQualityScore")) {
			Double minScore = (Double) criteria.get("minQualityScore");
			results = results.stream().filter(kb -> kb.getQualityScore() != null && kb.getQualityScore() >= minScore)
					.collect(Collectors.toList());
		}

		if (criteria.containsKey("sourceSystem")) {
			String sourceSystem = (String) criteria.get("sourceSystem");
			results = results.stream().filter(kb -> sourceSystem.equals(kb.getSourceSystem()))
					.collect(Collectors.toList());
		}

		if (criteria.containsKey("validationStatus")) {
			String validationStatus = (String) criteria.get("validationStatus");
			results = results.stream().filter(kb -> validationStatus.equals(kb.getValidationStatus()))
					.collect(Collectors.toList());
		}

		// Sort by updated date (newest first)
		results.sort((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()));

		logger.info("Found {} entries matching criteria", results.size());
		return results;
	}

}
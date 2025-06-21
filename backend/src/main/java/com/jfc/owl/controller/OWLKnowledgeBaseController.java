//OWLKnowledgeBaseController.java - OWL知識庫控制器
package com.jfc.owl.controller;

import com.jfc.owl.entity.OWLKnowledgeBase;
import com.jfc.owl.service.OWLKnowledgeBaseService;
import com.jfc.rdb.common.dto.AbstractDTOController;
import com.jfc.rdb.common.dto.ApiResponse;
import com.jfc.rdb.tiptop.entity.ImaFile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
         
         OWLKnowledgeBase result = knowledgeBaseService.exportAndSaveToKnowledgeBase(
             masterItemCode, format, includeHierarchy, description);
         
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
 public ResponseEntity<ApiResponse<OWLKnowledgeBase>> updateKnowledgeBaseEntry(
         @PathVariable String masterItemCode,
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
 public ResponseEntity<ApiResponse<List<OWLKnowledgeBase>>> searchKnowledgeBase(
         @RequestParam String keyword) {
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
 public ResponseEntity<ApiResponse<String>> getKnowledgeBaseModel(
         @PathVariable String masterItemCode,
         @RequestParam(defaultValue = "RDF/XML") String format) {
     try {
         String modelContent = knowledgeBaseService.getKnowledgeBaseModelAsString(masterItemCode, format);
         return success(modelContent);
     } catch (Exception e) {
         logger.error("Error getting knowledge base model", e);
         return error("Failed to get knowledge base model: " + e.getMessage());
     }
 }
}
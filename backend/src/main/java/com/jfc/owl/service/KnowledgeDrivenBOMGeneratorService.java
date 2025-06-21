//KnowledgeDrivenBOMGeneratorService.java
package com.jfc.owl.service;

import org.apache.jena.ontology.OntModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
* 基於知識庫驅動的BOM生成服務
* 整合ERP資料匯出、知識庫管理和智能BOM生成
*/
@Service
public class KnowledgeDrivenBOMGeneratorService {
 private static final Logger logger = LoggerFactory.getLogger(KnowledgeDrivenBOMGeneratorService.class);
 
 @Autowired
 private OWLKnowledgeBaseService knowledgeBaseService;
 
 @Autowired
 private BomOwlExportService bomOwlExportService;
 
 @Autowired
 private EnhancedHydraulicCylinderRules hydraulicCylinderRules;

 /**
  * 完整的知識驅動BOM生成流程
  * 這個方法展示了整個從ERP到智能BOM生成的完整流程
  */
 public Map<String, Object> generateIntelligentBOM(String newItemCode, Map<String, String> specifications) {
     logger.info("Starting knowledge-driven BOM generation for: {}", newItemCode);
     
     Map<String, Object> result = new HashMap<>();
     
     try {
         // ==================== 第一階段：建立/更新知識庫 ====================
         logger.info("Phase 1: Building/Updating Knowledge Base");
         
         // 1.1 檢查知識庫是否已初始化
         if (!isKnowledgeBaseInitialized()) {
             logger.info("Knowledge base not initialized, starting initial export from ERP");
             initializeKnowledgeBaseFromERP();
         }
         
         // 1.2 檢查是否有新的ERP資料需要同步
         syncLatestERPChanges();
         
         // ==================== 第二階段：智能BOM生成 ====================
         logger.info("Phase 2: Intelligent BOM Generation");
         
         // 2.1 從知識庫搜索相似的產品
         List<Map<String, Object>> similarProducts = findSimilarProductsFromKnowledgeBase(specifications);
         result.put("similarProducts", similarProducts);
         
         // 2.2 基於相似產品生成BOM建議
         Map<String, Object> bomSuggestions = generateBOMFromSimilarProducts(newItemCode, specifications, similarProducts);
         result.put("bomSuggestions", bomSuggestions);
         
         // 2.3 應用領域專家規則進行優化
         Map<String, Object> optimizedBOM = applyDomainExpertRules(newItemCode, specifications, bomSuggestions);
         result.put("optimizedBOM", optimizedBOM);
         
         // ==================== 第三階段：驗證與學習 ====================
         logger.info("Phase 3: Validation and Learning");
         
         // 3.1 驗證生成的BOM
         Map<String, Object> validationResult = validateGeneratedBOM(optimizedBOM);
         result.put("validation", validationResult);
         
         // 3.2 將新生成的BOM加入知識庫（用於未來學習）
         if ((Boolean) validationResult.get("isValid")) {
             saveGeneratedBOMToKnowledgeBase(newItemCode, optimizedBOM);
         }
         
         // ==================== 第四階段：結果整合 ====================
         result.put("newItemCode", newItemCode);
         result.put("specifications", specifications);
         result.put("generationStrategy", "knowledge-driven");
         result.put("knowledgeBaseStats", knowledgeBaseService.getKnowledgeBaseStatistics());
         
         logger.info("Knowledge-driven BOM generation completed successfully for: {}", newItemCode);
         return result;
         
     } catch (Exception e) {
         logger.error("Error in knowledge-driven BOM generation for: {}", newItemCode, e);
         throw new RuntimeException("Failed to generate intelligent BOM", e);
     }
 }
 
 /**
  * 檢查知識庫是否已初始化
  */
 private boolean isKnowledgeBaseInitialized() {
     try {
         Map<String, Object> stats = knowledgeBaseService.getKnowledgeBaseStatistics();
         long totalEntries = (Long) stats.get("totalEntries");
         return totalEntries > 0;
     } catch (Exception e) {
         return false;
     }
 }
 
 /**
  * 從ERP初始化知識庫
  */
 private void initializeKnowledgeBaseFromERP() {
     logger.info("Initializing knowledge base from ERP data");
     
     try {
         // 批量匯出所有ERP中的BOM到知識庫
         Map<String, Object> exportResult = knowledgeBaseService.exportAllBOMsToKnowledgeBase("RDF/XML", true);
         
         int successCount = (Integer) exportResult.get("successCount");
         int failureCount = (Integer) exportResult.get("failureCount");
         
         logger.info("Knowledge base initialization completed. Success: {}, Failed: {}", successCount, failureCount);
         
         if (failureCount > 0) {
             logger.warn("Some items failed to export: {}", exportResult.get("failedExports"));
         }
         
     } catch (Exception e) {
         logger.error("Failed to initialize knowledge base from ERP", e);
         throw new RuntimeException("Knowledge base initialization failed", e);
     }
 }
 
 /**
  * 同步最新的ERP變更
  */
 private void syncLatestERPChanges() {
     logger.debug("Checking for latest ERP changes to sync");
     
     try {
         // 這裡可以實現增量同步邏輯
         // 例如：檢查ERP中的最後修改時間，只同步新變更的BOM
         
         // 示例：獲取最近7天內變更的料件
         List<String> recentlyChangedItems = getRecentlyChangedItemsFromERP(7);
         
         for (String itemCode : recentlyChangedItems) {
             try {
                 // 更新知識庫中的對應條目
                 knowledgeBaseService.exportAndSaveToKnowledgeBase(
                     itemCode, "RDF/XML", true, "Auto-sync from ERP changes"
                 );
                 logger.debug("Synced item to knowledge base: {}", itemCode);
             } catch (Exception e) {
                 logger.warn("Failed to sync item {}: {}", itemCode, e.getMessage());
             }
         }
         
         if (!recentlyChangedItems.isEmpty()) {
             logger.info("Synced {} recently changed items to knowledge base", recentlyChangedItems.size());
         }
         
     } catch (Exception e) {
         logger.warn("Error during ERP sync: {}", e.getMessage());
         // 同步失敗不應該影響主流程
     }
 }
 
 /**
  * 從知識庫搜索相似產品
  */
 private List<Map<String, Object>> findSimilarProductsFromKnowledgeBase(Map<String, String> specifications) {
     logger.info("Searching for similar products in knowledge base");
     
     try {
         // 使用知識庫服務搜索相似BOM
         List<Map<String, Object>> similarBOMs = knowledgeBaseService.searchSimilarBOMs(specifications);
         
         // 按相似度排序並限制數量
         List<Map<String, Object>> topSimilar = similarBOMs.stream()
             .sorted((a, b) -> Double.compare(
                 (Double) b.get("similarityScore"), 
                 (Double) a.get("similarityScore")
             ))
             .limit(5) // 取前5個最相似的
             .collect(Collectors.toList());
         
         logger.info("Found {} similar products with similarity scores", topSimilar.size());
         
         return topSimilar;
         
     } catch (Exception e) {
         logger.error("Error searching similar products", e);
         return new ArrayList<>();
     }
 }
 
 /**
  * 基於相似產品生成BOM建議
  */
 private Map<String, Object> generateBOMFromSimilarProducts(String newItemCode, 
                                                           Map<String, String> specifications,
                                                           List<Map<String, Object>> similarProducts) {
     logger.info("Generating BOM suggestions from {} similar products", similarProducts.size());
     
     Map<String, Object> bomSuggestions = new HashMap<>();
     Map<String, List<Map<String, Object>>> componentSuggestions = new HashMap<>();
     
     try {
         // 分析相似產品的組件模式
         Map<String, Map<String, Integer>> componentFrequency = analyzeComponentPatterns(similarProducts);
         
         // 為每個組件類別生成建議
         for (Map.Entry<String, Map<String, Integer>> categoryEntry : componentFrequency.entrySet()) {
             String category = categoryEntry.getKey();
             Map<String, Integer> components = categoryEntry.getValue();
             
             List<Map<String, Object>> categorySuggestions = new ArrayList<>();
             
             // 按使用頻率排序組件
             components.entrySet().stream()
                 .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                 .limit(3) // 每類別最多3個建議
                 .forEach(compEntry -> {
                     Map<String, Object> suggestion = new HashMap<>();
                     suggestion.put("componentCode", compEntry.getKey());
                     suggestion.put("frequency", compEntry.getValue());
                     suggestion.put("confidence", calculateConfidence(compEntry.getValue(), similarProducts.size()));
                     
                     // 從知識庫獲取組件詳細資訊
                     enrichComponentInformation(suggestion);
                     
                     categorySuggestions.add(suggestion);
                 });
             
             componentSuggestions.put(category, categorySuggestions);
         }
         
         bomSuggestions.put("componentSuggestions", componentSuggestions);
         bomSuggestions.put("baselineProducts", similarProducts);
         bomSuggestions.put("generationMethod", "similarity-based");
         
         return bomSuggestions;
         
     } catch (Exception e) {
         logger.error("Error generating BOM from similar products", e);
         return bomSuggestions;
     }
 }
 
 /**
  * 應用領域專家規則進行BOM優化
  */
 private Map<String, Object> applyDomainExpertRules(String newItemCode, 
                                                   Map<String, String> specifications,
                                                   Map<String, Object> bomSuggestions) {
     logger.info("Applying domain expert rules for BOM optimization");
     
     try {
         // 使用液壓缸專家規則進行優化
         OntModel knowledgeModel = knowledgeBaseService.getMasterKnowledgeBase();
         
         // 應用液壓缸專家規則
         Map<String, Object> expertOptimizations = hydraulicCylinderRules.generateHydraulicCylinderBom(newItemCode, knowledgeModel);
         
         // 合併基於相似性的建議和專家規則的建議
         Map<String, Object> optimizedBOM = mergeBOMSuggestions(bomSuggestions, expertOptimizations);
         
         // 添加優化標記
         optimizedBOM.put("optimizationApplied", true);
         optimizedBOM.put("expertRulesUsed", "HydraulicCylinderRules");
         
         return optimizedBOM;
         
     } catch (Exception e) {
         logger.error("Error applying domain expert rules", e);
         // 如果專家規則失敗，返回原始建議
         return bomSuggestions;
     }
 }
 
 /**
  * 驗證生成的BOM
  */
 private Map<String, Object> validateGeneratedBOM(Map<String, Object> generatedBOM) {
     logger.info("Validating generated BOM");
     
     Map<String, Object> validationResult = new HashMap<>();
     List<String> errors = new ArrayList<>();
     List<String> warnings = new ArrayList<>();
     
     try {
         // 基本驗證
         if (!generatedBOM.containsKey("componentSuggestions")) {
             errors.add("No component suggestions found");
         }
         
         // 組件數量驗證
         @SuppressWarnings("unchecked")
         Map<String, List<Map<String, Object>>> components = 
             (Map<String, List<Map<String, Object>>>) generatedBOM.get("componentSuggestions");
         
         if (components != null) {
             // 檢查必要組件類別
             String[] requiredCategories = {"Barrel", "Piston", "PistonRod", "Seals", "EndCaps"};
             for (String category : requiredCategories) {
                 if (!components.containsKey(category) || components.get(category).isEmpty()) {
                     warnings.add("Missing suggestions for required category: " + category);
                 }
             }
             
             // 檢查每個類別的建議數量
             for (Map.Entry<String, List<Map<String, Object>>> entry : components.entrySet()) {
                 if (entry.getValue().size() < 1) {
                     warnings.add("Insufficient suggestions for category: " + entry.getKey());
                 }
             }
         }
         
         // 設定驗證結果
         boolean isValid = errors.isEmpty();
         validationResult.put("isValid", isValid);
         validationResult.put("errors", errors);
         validationResult.put("warnings", warnings);
         validationResult.put("validationScore", calculateValidationScore(errors.size(), warnings.size()));
         
         if (isValid) {
             logger.info("BOM validation passed with {} warnings", warnings.size());
         } else {
             logger.warn("BOM validation failed with {} errors and {} warnings", errors.size(), warnings.size());
         }
         
         return validationResult;
         
     } catch (Exception e) {
         logger.error("Error during BOM validation", e);
         validationResult.put("isValid", false);
         validationResult.put("errors", Arrays.asList("Validation process failed: " + e.getMessage()));
         return validationResult;
     }
 }
 
 /**
  * 將生成的BOM保存到知識庫以供未來學習
  */
 private void saveGeneratedBOMToKnowledgeBase(String newItemCode, Map<String, Object> generatedBOM) {
     logger.info("Saving generated BOM to knowledge base for future learning: {}", newItemCode);
     
     try {
         // 創建基於生成BOM的OWL模型
         OntModel newBOMModel = createOWLModelFromGeneratedBOM(newItemCode, generatedBOM);
         
         // 保存到知識庫
         // 注意：這裡保存的是"生成的"BOM，標記為預測性的，與實際ERP資料區分
         knowledgeBaseService.saveGeneratedBOMToKnowledgeBase(
             newItemCode, 
             newBOMModel, 
             "Generated BOM for new product - awaiting validation"
         );
         
         logger.info("Generated BOM saved to knowledge base: {}", newItemCode);
         
     } catch (Exception e) {
         logger.error("Error saving generated BOM to knowledge base", e);
         // 保存失敗不應該影響主流程
     }
 }
 
 // ==================== 輔助方法 ====================
 
 private List<String> getRecentlyChangedItemsFromERP(int days) {
     // 實現獲取最近變更項目的邏輯
     // 這裡應該查詢ERP資料庫的變更記錄
     return new ArrayList<>();
 }
 
 private Map<String, Map<String, Integer>> analyzeComponentPatterns(List<Map<String, Object>> similarProducts) {
     Map<String, Map<String, Integer>> patterns = new HashMap<>();
     
     for (Map<String, Object> product : similarProducts) {
         // 分析每個相似產品的組件模式
         // 實現組件頻率統計邏輯
     }
     
     return patterns;
 }
 
 private double calculateConfidence(int frequency, int totalProducts) {
     return (double) frequency / totalProducts;
 }
 
 private void enrichComponentInformation(Map<String, Object> suggestion) {
     // 從知識庫或ERP獲取組件的詳細資訊
     String componentCode = (String) suggestion.get("componentCode");
     // 添加組件名稱、規格等資訊
 }
 
 private Map<String, Object> mergeBOMSuggestions(Map<String, Object> similarityBased, Map<String, Object> expertBased) {
     // 實現兩種建議的合併邏輯
     // 可以根據權重、置信度等進行智能合併
     return similarityBased; // 簡化實現
 }
 
 private double calculateValidationScore(int errorCount, int warningCount) {
     if (errorCount > 0) return 0.0;
     return Math.max(0.0, 1.0 - (warningCount * 0.1));
 }
 
 private OntModel createOWLModelFromGeneratedBOM(String newItemCode, Map<String, Object> generatedBOM) {
     // 實現從生成的BOM創建OWL模型的邏輯
     return null; // 簡化實現
 }
}
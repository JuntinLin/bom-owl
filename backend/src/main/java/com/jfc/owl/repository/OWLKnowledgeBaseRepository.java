//OWLKnowledgeBaseRepository.java - OWL知識庫Repository介面
package com.jfc.owl.repository;

import com.jfc.owl.entity.OWLKnowledgeBase;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * OWL知識庫Repository介面 Enhanced OWL Knowledge Base Repository Interface Now
 * includes hydraulic cylinder specific methods and advanced analytics
 */
@Repository
public interface OWLKnowledgeBaseRepository extends JpaRepository<OWLKnowledgeBase, Long> {
	// ==================== Basic Query Methods ====================
	// 基本查詢方法
	Optional<OWLKnowledgeBase> findByMasterItemCodeAndActiveTrue(String masterItemCode);

	List<OWLKnowledgeBase> findByActiveTrue();

	List<OWLKnowledgeBase> findByMasterItemCode(String masterItemCode);

	List<OWLKnowledgeBase> findByFormat(String format);

	List<OWLKnowledgeBase> findByIncludeHierarchy(Boolean includeHierarchy);

// ==================== Hydraulic Cylinder Specific Methods ====================

	/**
	 * Find active hydraulic cylinder entries
	 */
	List<OWLKnowledgeBase> findByActiveTrueAndIsHydraulicCylinderTrue();

	/**
	 * Find non-hydraulic cylinder entries
	 */
	@Query("SELECT kb FROM OWLKnowledgeBase kb WHERE kb.active = true "
			+ "AND (kb.isHydraulicCylinder = false OR kb.isHydraulicCylinder IS NULL)")
	List<OWLKnowledgeBase> findNonHydraulicCylinderEntries();

	/**
	 * Count active hydraulic cylinder entries
	 */
	@Query("SELECT COUNT(o) FROM OWLKnowledgeBase o WHERE o.active = true AND o.isHydraulicCylinder = true")
	long countByActiveTrueAndIsHydraulicCylinderTrue();

	/**
	 * Find hydraulic cylinders by series
	 */
	@Query("SELECT kb FROM OWLKnowledgeBase kb WHERE kb.active = true " + "AND kb.isHydraulicCylinder = true "
			+ "AND kb.hydraulicCylinderSpecs LIKE CONCAT('%series=', :series, '%') " + "ORDER BY kb.masterItemCode")
	List<OWLKnowledgeBase> findHydraulicCylindersBySeries(@Param("series") String series);

	/**
	 * Find hydraulic cylinders by specifications pattern
	 */
	@Query("SELECT kb FROM OWLKnowledgeBase kb WHERE kb.active = true " + "AND kb.isHydraulicCylinder = true "
			+ "AND kb.hydraulicCylinderSpecs LIKE %:specsPattern% " + "ORDER BY kb.masterItemCode")
	List<OWLKnowledgeBase> findByHydraulicCylinderSpecsContaining(@Param("specsPattern") String specsPattern);

	/**
	 * Search hydraulic cylinders by multiple criteria
	 */
	@Query("SELECT kb FROM OWLKnowledgeBase kb WHERE kb.active = true " + "AND kb.isHydraulicCylinder = true "
			+ "AND (:series IS NULL OR kb.hydraulicCylinderSpecs LIKE CONCAT('%series=', :series, '%')) "
			+ "AND (:bore IS NULL OR kb.hydraulicCylinderSpecs LIKE CONCAT('%bore=', :bore, '%')) "
			+ "AND (:rodEndType IS NULL OR kb.hydraulicCylinderSpecs LIKE CONCAT('%rodEndType=', :rodEndType, '%')) "
			+ "ORDER BY kb.masterItemCode")
	List<OWLKnowledgeBase> findByHydraulicCylinderSpecs(@Param("series") String series, @Param("bore") String bore,
			@Param("rodEndType") String rodEndType);

	// ==================== Statistical Query Methods ====================
	// 統計查詢方法
	@Query("SELECT COUNT(kb) FROM OWLKnowledgeBase kb WHERE kb.active = true")
	long countByActiveTrue();

	@Query("SELECT COALESCE(SUM(kb.fileSize), 0) FROM OWLKnowledgeBase kb WHERE kb.active = true")
	Long sumFileSizeByActiveTrue();

	@Query("SELECT COALESCE(SUM(kb.tripleCount), 0)  FROM OWLKnowledgeBase kb WHERE kb.active = true")
	Long sumTripleCountByActiveTrue();

	// 格式分佈統計
	/**
	 * Get format distribution for active entries
	 */
	@Query("SELECT kb.format as format, COUNT(kb) as count FROM OWLKnowledgeBase kb WHERE kb.active = true GROUP BY kb.format")
	List<Object[]> getFormatDistribution();

	// 轉換為Map的便利方法
	/**
	 * Convert format distribution to Map
	 */
	default Map<String, Long> countByFormatAndActiveTrue() {
		try {
			List<Object[]> results = getFormatDistribution();
			Map<String, Long> distribution = new HashMap<>();
			for (Object[] result : results) {
				String format = (String) result[0];
				Long count = (Long) result[1];
				distribution.put(format != null ? format : "UNKNOWN", count != null ? count : 0L);
			}

			return distribution;
		} catch (Exception e) {
			// 如果查詢失敗，返回空 Map
			return new HashMap<>();
		}
	}

	/**
	 * Get comprehensive statistics
	 */
	@Query("SELECT " + "COUNT(kb) as totalEntries, "
			+ "COUNT(CASE WHEN kb.isHydraulicCylinder = true THEN 1 END) as hydraulicCylinderCount, "
			+ "COUNT(CASE WHEN kb.includeHierarchy = true THEN 1 END) as hierarchyCount, "
			+ "AVG(kb.fileSize) as avgFileSize, " + "AVG(kb.tripleCount) as avgTripleCount, "
			+ "AVG(kb.qualityScore) as avgQualityScore " + "FROM OWLKnowledgeBase kb WHERE kb.active = true")
	Object[] getComprehensiveStatistics();

	/**
	 * Get hydraulic cylinder specific statistics
	 */
	@Query("SELECT " + "COUNT(kb) as totalHydraulicCylinders, " + "AVG(kb.fileSize) as avgFileSize, "
			+ "AVG(kb.tripleCount) as avgTripleCount, " + "AVG(kb.componentCount) as avgComponentCount, "
			+ "AVG(kb.qualityScore) as avgQualityScore "
			+ "FROM OWLKnowledgeBase kb WHERE kb.active = true AND kb.isHydraulicCylinder = true")
	Object[] getHydraulicCylinderStatistics();

	// ==================== Time-based Query Methods ====================
	// 時間相關查詢
	@Query("SELECT MAX(kb.updatedAt) FROM OWLKnowledgeBase kb WHERE kb.active = true")
	LocalDateTime findLatestUpdateTime();

	List<OWLKnowledgeBase> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

	@Query("SELECT kb FROM OWLKnowledgeBase kb WHERE kb.active = true "
			+ "AND kb.createdAt BETWEEN :startDate AND :endDate " + "ORDER BY kb.createdAt DESC")
	List<OWLKnowledgeBase> findByDateRange(@Param("startDate") LocalDateTime startDate,
			@Param("endDate") LocalDateTime endDate);

	/**
	 * Find entries that need refresh (older than specified date)
	 */
	@Query("SELECT kb FROM OWLKnowledgeBase kb WHERE kb.active = true " + "AND kb.updatedAt < :cutoffDate "
			+ "ORDER BY kb.updatedAt ASC")
	List<OWLKnowledgeBase> findEntriesNeedingRefresh(@Param("cutoffDate") LocalDateTime cutoffDate);

	/**
	 * Find recently accessed entries
	 */
	@Query("SELECT kb FROM OWLKnowledgeBase kb WHERE kb.active = true " + "AND kb.lastAccessed > :since "
			+ "ORDER BY kb.lastAccessed DESC")
	List<OWLKnowledgeBase> findRecentlyAccessed(@Param("since") LocalDateTime since);

	// ==================== Usage and Performance Methods ====================
	// 使用頻率查詢
	List<OWLKnowledgeBase> findTop10ByActiveTrueOrderByUsageCountDesc();

	List<OWLKnowledgeBase> findByLastUsedAtAfter(LocalDateTime date);

	/**
	 * Find entries by quality score range
	 */
	@Query("SELECT kb FROM OWLKnowledgeBase kb WHERE kb.active = true "
			+ "AND kb.qualityScore BETWEEN :minScore AND :maxScore " + "ORDER BY kb.qualityScore DESC")
	List<OWLKnowledgeBase> findByQualityScoreRange(@Param("minScore") double minScore,
			@Param("maxScore") double maxScore);

	/**
	 * Find high-quality entries
	 */
	@Query("SELECT kb FROM OWLKnowledgeBase kb WHERE kb.active = true " + "AND kb.qualityScore >= 0.8 "
			+ "ORDER BY kb.qualityScore DESC, kb.usageCount DESC")
	List<OWLKnowledgeBase> findHighQualityEntries();

	// ==================== Search and Filter Methods ====================

	// 搜索相關方法
	@Query("SELECT kb FROM OWLKnowledgeBase kb WHERE kb.active = true AND "
			+ "(LOWER(kb.masterItemCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "LOWER(kb.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "LOWER(kb.tags) LIKE LOWER(CONCAT('%', :keyword, '%')))")
	List<OWLKnowledgeBase> searchByKeyword(@Param("keyword") String keyword);

	// 根據tags查詢
	@Query("SELECT kb FROM OWLKnowledgeBase kb WHERE kb.active = true AND kb.tags LIKE CONCAT('%', :tag, '%')")
	List<OWLKnowledgeBase> findByTag(@Param("tag") String tag);

	/**
	 * Advanced search with multiple filters
	 */
	@Query("SELECT kb FROM OWLKnowledgeBase kb WHERE kb.active = true "
			+ "AND (:format IS NULL OR kb.format = :format) "
			+ "AND (:includeHierarchy IS NULL OR kb.includeHierarchy = :includeHierarchy) "
			+ "AND (:isHydraulicCylinder IS NULL OR kb.isHydraulicCylinder = :isHydraulicCylinder) "
			+ "AND (:minQualityScore IS NULL OR kb.qualityScore >= :minQualityScore) "
			+ "ORDER BY kb.qualityScore DESC, kb.usageCount DESC")
	List<OWLKnowledgeBase> findByMultipleCriteria(@Param("format") String format,
			@Param("includeHierarchy") Boolean includeHierarchy,
			@Param("isHydraulicCylinder") Boolean isHydraulicCylinder,
			@Param("minQualityScore") Double minQualityScore);

	/**
	 * Find similar entries by master item code pattern
	 */
	@Query("SELECT kb FROM OWLKnowledgeBase kb WHERE kb.active = true " + "AND kb.masterItemCode LIKE :pattern "
			+ "AND kb.masterItemCode != :excludeCode " + "ORDER BY kb.masterItemCode")
	List<OWLKnowledgeBase> findSimilarByPattern(@Param("pattern") String pattern,
			@Param("excludeCode") String excludeCode);

	// ==================== Hierarchy and Structure Methods ====================

	List<OWLKnowledgeBase> findByActiveTrueAndIncludeHierarchyTrue();

	List<OWLKnowledgeBase> findByActiveTrueAndIncludeHierarchyFalse();

	/**
	 * Find entries with hierarchical structure
	 */
	@Query("SELECT kb FROM OWLKnowledgeBase kb WHERE kb.active = true " + "AND kb.hasHierarchy = true "
			+ "ORDER BY kb.hierarchyDepth DESC")
	List<OWLKnowledgeBase> findEntriesWithHierarchy();

	/**
	 * Find entries by hierarchy depth
	 */
	@Query("SELECT kb FROM OWLKnowledgeBase kb WHERE kb.active = true " + "AND kb.hierarchyDepth >= :minDepth "
			+ "ORDER BY kb.hierarchyDepth DESC")
	List<OWLKnowledgeBase> findByHierarchyDepth(@Param("minDepth") int minDepth);

	// ==================== File and Storage Methods ====================

	/**
	 * Find entries by file size range
	 */
	@Query("SELECT kb FROM OWLKnowledgeBase kb WHERE kb.active = true "
			+ "AND kb.fileSize BETWEEN :minSize AND :maxSize " + "ORDER BY kb.fileSize DESC")
	List<OWLKnowledgeBase> findByFileSizeRange(@Param("minSize") long minSize, @Param("maxSize") long maxSize);

	/**
	 * Find large entries (for performance monitoring)
	 */
	@Query("SELECT kb FROM OWLKnowledgeBase kb WHERE kb.active = true " + "AND kb.tripleCount > :threshold "
			+ "ORDER BY kb.tripleCount DESC")
	List<OWLKnowledgeBase> findLargeEntries(@Param("threshold") int threshold);

	/**
	 * Find entries for file validation
	 */
	@Query("SELECT kb FROM OWLKnowledgeBase kb WHERE kb.active = true " + "AND kb.filePath IS NOT NULL")
	List<OWLKnowledgeBase> findEntriesForFileValidation();

	// ==================== Validation and Quality Methods ====================

	/**
	 * Find entries by validation status
	 */
	List<OWLKnowledgeBase> findByActiveTrueAndValidationStatus(String validationStatus);

	/**
	 * Find entries with errors
	 */
	@Query("SELECT kb FROM OWLKnowledgeBase kb WHERE kb.active = true "
			+ "AND kb.errorMessages IS NOT NULL AND kb.errorMessages != ''")
	List<OWLKnowledgeBase> findEntriesWithErrors();

	/**
	 * Find entries needing validation
	 */
	@Query("SELECT kb FROM OWLKnowledgeBase kb WHERE kb.active = true "
			+ "AND (kb.validationStatus = 'PENDING' OR kb.validationStatus IS NULL)")
	List<OWLKnowledgeBase> findEntriesNeedingValidation();

	// ==================== Maintenance and Cleanup Methods ====================

	/**
	 * Find duplicate entries (same master item code, multiple active versions)
	 */
	@Query("SELECT kb.masterItemCode, COUNT(kb) as count FROM OWLKnowledgeBase kb " + "WHERE kb.active = true "
			+ "GROUP BY kb.masterItemCode " + "HAVING COUNT(kb) > 1")
	List<Object[]> findDuplicateEntries();

	/**
	 * Find orphaned entries (files that might not exist)
	 */
	@Query("SELECT kb FROM OWLKnowledgeBase kb WHERE kb.active = true " + "AND kb.lastAccessed IS NULL "
			+ "AND kb.createdAt < :cutoffDate")
	List<OWLKnowledgeBase> findOrphanedEntries(@Param("cutoffDate") LocalDateTime cutoffDate);

	/**
	 * Delete inactive entries older than specified date
	 */
	@Modifying
	@Transactional
	// 刪除過期的非活躍記錄
	@Query("DELETE FROM OWLKnowledgeBase kb WHERE kb.active = false AND kb.updatedAt < :cutoffDate")
	void deleteInactiveOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

	/**
	 * Update usage statistics
	 */
	@Modifying
	@Transactional
	@Query("UPDATE OWLKnowledgeBase kb SET kb.usageCount = kb.usageCount + 1, "
			+ "kb.lastAccessed = :accessTime WHERE kb.id = :id")
	void updateUsageStatistics(@Param("id") Long id, @Param("accessTime") LocalDateTime accessTime);

	// ==================== Analytics and Reporting Methods ====================

	/**
	 * Get usage statistics by format
	 */
	@Query("SELECT kb.format, COUNT(kb) as entryCount, " + "AVG(kb.fileSize) as avgFileSize, "
			+ "AVG(kb.tripleCount) as avgTripleCount, " + "SUM(kb.usageCount) as totalUsage "
			+ "FROM OWLKnowledgeBase kb WHERE kb.active = true " + "GROUP BY kb.format")
	List<Object[]> getUsageStatisticsByFormat();

	/**
	 * Get monthly creation statistics
	 */
	@Query("SELECT YEAR(kb.createdAt) as year, MONTH(kb.createdAt) as month, COUNT(kb) as count "
			+ "FROM OWLKnowledgeBase kb WHERE kb.active = true " + "GROUP BY YEAR(kb.createdAt), MONTH(kb.createdAt) "
			+ "ORDER BY year DESC, month DESC")
	List<Object[]> getMonthlyCreationStatistics();

	/**
	 * Get trend analysis data
	 */
	@Query("SELECT DATE(kb.createdAt) as date, " + "COUNT(kb) as totalEntries, "
			+ "COUNT(CASE WHEN kb.isHydraulicCylinder = true THEN 1 END) as hydraulicCylinderEntries "
			+ "FROM OWLKnowledgeBase kb WHERE kb.active = true " + "AND kb.createdAt >= :startDate "
			+ "GROUP BY DATE(kb.createdAt) " + "ORDER BY date")
	List<Object[]> getTrendAnalysis(@Param("startDate") LocalDateTime startDate);
}

// OWLKnowledgeBase.java - OWL知識庫實體類
package com.jfc.owl.entity;

import lombok.*;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Index;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
* OWL知識庫實體類
* 用於記錄保存在系統中的OWL檔案資訊
*/
/**
 * Enhanced OWL Knowledge Base Entity Records information about OWL files stored   
 * in the system Now includes hydraulic cylinder specific support
 */
@Entity
@Table(name = "owl_knowledge_base", indexes = { 
		@Index(name = "idx_master_item_code", columnList = "master_item_code"),
		@Index(name = "idx_active", columnList = "active"),
		@Index(name = "idx_hydraulic_cylinder", columnList = "is_hydraulic_cylinder"),
		@Index(name = "idx_created_at", columnList = "created_at"),
		@Index(name = "idx_format", columnList = "format") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = { "fileHash", "hydraulicCylinderSpecs", "errorMessages" }) // Exclude sensitive data from toString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OWLKnowledgeBase {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	@Column(name = "master_item_code", nullable = false, length = 50)
	private String masterItemCode;

	@Column(name = "file_name", nullable = false, length = 255)
	private String fileName;

	@Column(name = "file_path", nullable = false, length = 500)
	private String filePath;

	@Column(name = "format", nullable = false, length = 20)
	private String format;

	@Column(name = "include_hierarchy", nullable = false)
	private Boolean includeHierarchy;

	@Column(name = "description", length = 1000)
	private String description;

	@Column(name = "file_size")
	private Long fileSize;

	@Column(name = "file_hash", length = 64)
	private String fileHash;

	@Column(name = "triple_count")
	private Integer tripleCount;

	@Column(name = "created_at", nullable = false)
	@Builder.Default
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "updated_at", nullable = false)
	@Builder.Default
	private LocalDateTime updatedAt = LocalDateTime.now();

	@Column(name = "active", nullable = false)
	@Builder.Default
	private Boolean active = true;

	@Column(name = "version", nullable = false)
	@Builder.Default
	private Integer version = 1;

	@Column(name = "tags", length = 500)
	private String tags;

	@Column(name = "usage_count")
	@Builder.Default
	private Integer usageCount = 0;

	@Column(name = "last_used_at")
	private LocalDateTime lastUsedAt;

//==================== New Hydraulic Cylinder Specific Fields ====================

	/**
	 * Flag to indicate if this entry represents a hydraulic cylinder
	 */
	@Column(name = "is_hydraulic_cylinder")
	private Boolean isHydraulicCylinder;

	/**
	 * Hydraulic cylinder specifications stored as JSON string Contains bore,
	 * stroke, series, rod end type, etc.
	 */
	@Lob
	@Column(name = "hydraulic_cylinder_specs", columnDefinition = "TEXT")
	private String hydraulicCylinderSpecs;

	/**
	 * Quality score of the BOM (based on completeness, accuracy, etc.)
	 */
	@Column(name = "quality_score")
	private Double qualityScore;

	/**
	 * Number of components in the BOM
	 */
	@Column(name = "component_count")
	private Integer componentCount;

	/**
	 * Indicates if this BOM has hierarchical structure
	 */
	@Column(name = "has_hierarchy")
	private Boolean hasHierarchy;

	/**
	 * Maximum depth of the BOM hierarchy
	 */
	@Column(name = "hierarchy_depth")
	private Integer hierarchyDepth;

	/**
	 * Source system or method used to generate this entry
	 */
	@Column(name = "source_system", length = 100)
	@Builder.Default
	private String sourceSystem = "TIPTOP";

	/**
	 * Checksum for data integrity verification
	 */
	@Column(name = "checksum", length = 64)
	private String checksum;

	/**
	 * Last accessed timestamp
	 */
	@Column(name = "last_accessed")
	private LocalDateTime lastAccessed;

	/**
	 * Priority level for processing (1=highest, 5=lowest)
	 */
	@Column(name = "priority_level")
	@Builder.Default
	private Integer priorityLevel = 3;

	/**
	 * Validation status of the OWL file
	 */
	@Column(name = "validation_status", length = 50)
	@Builder.Default
	private String validationStatus = "PENDING";

	/**
	 * Error messages from validation or processing
	 */
	@Lob
	@Column(name = "error_messages", columnDefinition = "TEXT")
	private String errorMessages;

	/**
	 * Performance metrics as JSON string
	 */
	@Lob
	@Column(name = "performance_metrics", columnDefinition = "TEXT")
	private String performanceMetrics;

	/**
	 * Similarity threshold for this entry (for search optimization)
	 */
	@Column(name = "similarity_threshold")
	@Builder.Default
	private Double similarityThreshold = 0.7;

	/**
	 * Export duration in milliseconds
	 */
	@Column(name = "export_duration_ms")
	private Long exportDurationMs;

	/**
	 * File compression ratio (if applicable)
	 */
	@Column(name = "compression_ratio")
	private Double compressionRatio;

//==================== Constructors ====================

	/**
	 * Convenience constructor for basic OWL knowledge base entry
	 */
	// 自定義建構子 - 保留原有的便利建構子
	public OWLKnowledgeBase(String masterItemCode, String fileName, String filePath, String format) {
		this.masterItemCode = masterItemCode;
		this.fileName = fileName;
		this.filePath = filePath;
		this.format = format;
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
		this.active = true;
		this.version = 1;
		this.usageCount = 0;
		this.sourceSystem = "TIPTOP";
		this.priorityLevel = 3;
		this.validationStatus = "PENDING";
		this.similarityThreshold = 0.7;

		// Auto-detect hydraulic cylinder
		this.isHydraulicCylinder = isHydraulicCylinderCode(masterItemCode);
	}
	
	/**
     * Enhanced constructor with hydraulic cylinder support
     */
    public OWLKnowledgeBase(String masterItemCode, String fileName, String filePath, String format, 
                           Boolean includeHierarchy, String description) {
        this(masterItemCode, fileName, filePath, format);
        this.includeHierarchy = includeHierarchy;
        this.description = description;
    }

	// 輔助方法
// ==================== Helper Methods ====================
    
    /**
     * Increment usage count and update last used timestamp
     */
	public void incrementUsageCount() {
		this.usageCount = (this.usageCount == null ? 0 : this.usageCount) + 1;
		this.lastUsedAt = LocalDateTime.now();
		this.lastAccessed = LocalDateTime.now();
	}
	
	/**
     * Mark as accessed (for performance tracking)
     */
    public void markAccessed() {
        this.lastAccessed = LocalDateTime.now();
        incrementUsageCount();
    }
    
    /**
     * Set validation status with timestamp update
     */
    public void setValidationStatus(String status, String errorMessage) {
        this.validationStatus = status;
        if (errorMessage != null && !errorMessage.trim().isEmpty()) {
            this.errorMessages = errorMessage;
        }
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Check if this entry represents a hydraulic cylinder based on item code
     */
    public static boolean isHydraulicCylinderCode(String itemCode) {
        return itemCode != null && 
               itemCode.length() >= 2 && 
               (itemCode.startsWith("3") || itemCode.startsWith("4"));
    }
    
    /**
     * Auto-detect and set hydraulic cylinder flag
     */
    public void detectHydraulicCylinder() {
        this.isHydraulicCylinder = isHydraulicCylinderCode(this.masterItemCode);
    }
    
    /**
     * Set hydraulic cylinder specifications with auto-detection
     */
    public void setHydraulicCylinderSpecs(String specs) {
        this.hydraulicCylinderSpecs = specs;
        if (specs != null && !specs.trim().isEmpty()) {
            this.isHydraulicCylinder = true;
        }
    }
    
    /**
     * Calculate and set quality score based on various factors
     */
    public void calculateQualityScore() {
        double score = 0.0;
        int factors = 0;
        
        // File size factor (larger files generally indicate more complete BOMs)
        if (fileSize != null && fileSize > 0) {
            score += Math.min(1.0, fileSize / 100000.0); // Normalize to 100KB baseline
            factors++;
        }
        
        // Triple count factor
        if (tripleCount != null && tripleCount > 0) {
            score += Math.min(1.0, tripleCount / 1000.0); // Normalize to 1000 triples baseline
            factors++;
        }
        
        // Component count factor
        if (componentCount != null && componentCount > 0) {
            score += Math.min(1.0, componentCount / 50.0); // Normalize to 50 components baseline
            factors++;
        }
        
        // Hierarchy factor (BOMs with hierarchy are generally more valuable)
        if (hasHierarchy != null && hasHierarchy) {
            score += 0.5;
            factors++;
        }
        
        // Hydraulic cylinder factor (domain-specific knowledge adds value)
        if (isHydraulicCylinder != null && isHydraulicCylinder) {
            score += 0.3;
            factors++;
        }
        
        // Validation status factor
        if ("VALID".equals(validationStatus)) {
            score += 0.5;
            factors++;
        } else if ("INVALID".equals(validationStatus)) {
            score -= 0.2;
        }
        
        // Calculate average and clamp to [0,1]
        this.qualityScore = factors > 0 ? Math.max(0.0, Math.min(1.0, score / factors)) : 0.0;
    }
    
    /**
     * Get a human-readable quality rating
     */
    public String getQualityRating() {
        if (qualityScore == null) return "UNKNOWN";
        
        if (qualityScore >= 0.8) return "EXCELLENT";
        else if (qualityScore >= 0.6) return "GOOD";
        else if (qualityScore >= 0.4) return "FAIR";
        else if (qualityScore >= 0.2) return "POOR";
        else return "VERY_POOR";
    }
    
    /**
     * Check if this entry needs refresh (based on age and usage)
     */
    public boolean needsRefresh(int maxDaysOld) {
        if (updatedAt == null) return true;
        
        LocalDateTime cutoff = LocalDateTime.now().minusDays(maxDaysOld);
        return updatedAt.isBefore(cutoff);
    }
    
    /**
     * Get file size in human-readable format
     */
    public String getFileSizeFormatted() {
        if (fileSize == null || fileSize == 0) return "0 B";
        
        String[] units = {"B", "KB", "MB", "GB"};
        double size = fileSize.doubleValue();
        int unitIndex = 0;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.1f %s", size, units[unitIndex]);
    }
    
    /**
     * Update performance metrics
     */
    public void updatePerformanceMetrics(long processingTimeMs, int memoryUsageMB) {
        String metrics = String.format(
            "{\"processingTime\": %d, \"memoryUsage\": %d, \"timestamp\": \"%s\"}", 
            processingTimeMs, memoryUsageMB, LocalDateTime.now()
        );
        this.performanceMetrics = metrics;
        this.exportDurationMs = processingTimeMs;
    }
    
    // ==================== JPA Lifecycle Methods ====================
    
    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (active == null) active = true;
        if (version == null) version = 1;
        if (usageCount == null) usageCount = 0;
        if (isHydraulicCylinder == null) detectHydraulicCylinder();
        if (sourceSystem == null) sourceSystem = "TIPTOP";
        if (priorityLevel == null) priorityLevel = 3;
        if (validationStatus == null) validationStatus = "PENDING";
        if (similarityThreshold == null) similarityThreshold = 0.7;
    }

	@PreUpdate
	public void preUpdate() {
		this.updatedAt = LocalDateTime.now();
		// Recalculate quality score on update
        calculateQualityScore();
	}
	
// ==================== Validation Methods ====================
    
    /**
     * Validate the entity state
     */
    public boolean isValid() {
        return masterItemCode != null && !masterItemCode.trim().isEmpty() &&
               fileName != null && !fileName.trim().isEmpty() &&
               filePath != null && !filePath.trim().isEmpty() &&
               format != null && !format.trim().isEmpty();
    }
    
    /**
     * Get validation errors
     */
    public List<String> getValidationErrors() {
        List<String> errors = new ArrayList<>();
        
        if (masterItemCode == null || masterItemCode.trim().isEmpty()) {
            errors.add("Master item code is required");
        }
        if (fileName == null || fileName.trim().isEmpty()) {
            errors.add("File name is required");
        }
        if (filePath == null || filePath.trim().isEmpty()) {
            errors.add("File path is required");
        }
        if (format == null || format.trim().isEmpty()) {
            errors.add("Format is required");
        }
        if (fileSize != null && fileSize < 0) {
            errors.add("File size cannot be negative");
        }
        if (tripleCount != null && tripleCount < 0) {
            errors.add("Triple count cannot be negative");
        }
        
        return errors;
    }
}
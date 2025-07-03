package com.jfc.owl.dto.search;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * DTO for individual similar BOM result
 * Represents a single BOM that matches the search criteria
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SimilarBOMDTO {
    
    /**
     * Master item code of the BOM
     */
    private String masterItemCode;
    
    /**
     * File name in the knowledge base
     */
    private String fileName;
    
    /**
     * Description of the BOM
     */
    private String description;
    
    /**
     * Similarity score (0-100)
     */
    private Double similarityScore;
    
    /**
     * When this BOM was created
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    /**
     * Number of RDF triples in the OWL file
     */
    private Integer tripleCount;
    
    /**
     * Whether this is a hydraulic cylinder BOM
     */
    private Boolean isHydraulicCylinder;
    
    /**
     * Hydraulic cylinder specifications (JSON string)
     */
    private String hydraulicCylinderSpecs;
    
    /**
     * Source system (e.g., "TIPTOP", "AI_GENERATED")
     */
    private String sourceSystem;
    
    /**
     * Validation status
     */
    private String validationStatus;
    
    /**
     * Number of components in the BOM
     */
    private Integer componentCount;
    
    /**
     * Quality score (0-1)
     */
    private Double qualityScore;
    
    /**
     * File size in bytes
     */
    private Long fileSize;
    
    /**
     * OWL format (RDF/XML, TURTLE, etc.)
     */
    private String format;
    
    /**
     * Whether the BOM includes hierarchy
     */
    private Boolean includeHierarchy;
    
    /**
     * Usage count
     */
    private Integer usageCount;
    
    /**
     * Last used timestamp
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastUsedAt;
    
    /**
     * Tags associated with this BOM
     */
    private String tags;
    
    /**
     * Parsed hydraulic cylinder specifications
     */
    private HydraulicCylinderSpecs parsedSpecs;
    
    /**
     * Get similarity score as percentage string
     */
    public String getSimilarityScorePercentage() {
        if (similarityScore == null) return "0%";
        return String.format("%.1f%%", similarityScore);
    }
    
    /**
     * Get quality rating based on quality score
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
     * Check if this BOM is validated
     */
    public boolean isValidated() {
        return "VALIDATED".equals(validationStatus) || "VALID".equals(validationStatus);
    }
    
    /**
     * Check if this BOM is AI generated
     */
    public boolean isAIGenerated() {
        return "AI_GENERATED".equals(sourceSystem);
    }
    
    /**
     * Inner class for hydraulic cylinder specifications
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HydraulicCylinderSpecs {
        private String series;
        private String type;
        private String bore;
        private String stroke;
        private String rodEndType;
        private String installationType;
        private String shaftEndJoin;
    }
}
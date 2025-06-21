package com.jfc.owl.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jfc.rdb.tiptop.entity.ImaFile;
import com.jfc.rdb.tiptop.repository.ImaRepository;

import java.util.*;

/**
 * Database-based similarity service for hydraulic cylinders
 * This implementation works without OWL/Jena dependencies
 */
@Service
public class DatabaseSimilarityService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseSimilarityService.class);

    @Autowired
    private ImaRepository imaRepository;

    /**
     * Find similar hydraulic cylinders using database queries
     * This is the database-based implementation of findEnhancedSimilarCylinders
     * 
     * @param newItemCode The new hydraulic cylinder code
     * @return List of similar cylinders with similarity scores
     */
    public List<Map<String, Object>> findEnhancedSimilarCylinders(String newItemCode) {
        logger.info("Finding enhanced similar cylinders for: {}", newItemCode);
        
        List<Map<String, Object>> similarCylinders = new ArrayList<>();
        
        try {
            // Extract specifications from the new item code
            CylinderSpecs newSpecs = extractSpecifications(newItemCode);
            
            if (!newSpecs.isValid()) {
                logger.warn("Invalid cylinder code format: {}", newItemCode);
                return similarCylinders;
            }
            
            // Get all hydraulic cylinders from database
            List<ImaFile> allCylinders = imaRepository.findByIma09AndIma10("S", "130 HC");
            
            for (ImaFile cylinder : allCylinders) {
                String existingCode = cylinder.getIma01();
                
                // Skip self and non-hydraulic cylinders
                if (existingCode.equals(newItemCode) || !isHydraulicCylinder(existingCode)) {
                    continue;
                }
                
                // Extract specifications from existing cylinder
                CylinderSpecs existingSpecs = extractSpecifications(existingCode);
                
                if (!existingSpecs.isValid()) {
                    continue;
                }
                
                // Calculate enhanced similarity score
                double similarityScore = calculateEnhancedSimilarityScore(newSpecs, existingSpecs);
                
                // Only include cylinders with meaningful similarity (30% threshold)
                if (similarityScore >= 30.0) {
                    Map<String, Object> similarCylinder = createSimilarCylinderInfo(
                        cylinder, existingSpecs, similarityScore);
                    similarCylinders.add(similarCylinder);
                }
            }
            
            // Sort by similarity score (descending)
            similarCylinders.sort((a, b) -> {
                Double scoreA = (Double) a.get("similarityScore");
                Double scoreB = (Double) b.get("similarityScore");
                return Double.compare(scoreB, scoreA);
            });
            
            // Limit to top 10 most similar cylinders
            if (similarCylinders.size() > 10) {
                similarCylinders = similarCylinders.subList(0, 10);
            }
            
            logger.info("Found {} similar cylinders for {}", similarCylinders.size(), newItemCode);
            return similarCylinders;
            
        } catch (Exception e) {
            logger.error("Error finding enhanced similar cylinders for: " + newItemCode, e);
            return similarCylinders;
        }
    }
    
    /**
     * Extract specifications from hydraulic cylinder code
     */
    private CylinderSpecs extractSpecifications(String itemCode) {
        CylinderSpecs specs = new CylinderSpecs();
        specs.itemCode = itemCode;
        
        if (itemCode != null && itemCode.length() >= 15) {
            try {
                specs.productType = itemCode.substring(0, 1);
                specs.series = itemCode.substring(2, 4);
                specs.type = itemCode.substring(4, 5);
                specs.bore = itemCode.substring(5, 8);
                specs.stroke = itemCode.substring(10, 14);
                specs.rodEndType = itemCode.substring(14, 15);
                specs.valid = true;
            } catch (StringIndexOutOfBoundsException e) {
                logger.debug("Error extracting specs from code: {}", itemCode);
                specs.valid = false;
            }
        }
        
        return specs;
    }
    
    /**
     * Check if item code represents a hydraulic cylinder
     */
    private boolean isHydraulicCylinder(String itemCode) {
        return itemCode != null && 
               itemCode.length() >= 15 && 
               (itemCode.startsWith("3") || itemCode.startsWith("4"));
    }
    
    /**
     * Calculate enhanced similarity score with weighted factors
     */
    private double calculateEnhancedSimilarityScore(CylinderSpecs newSpecs, CylinderSpecs existingSpecs) {
        double score = 0.0;
        
        // Product type similarity (weight: 10%)
        if (newSpecs.productType.equals(existingSpecs.productType)) {
            score += 10.0;
        }
        
        // Series similarity (weight: 25%)
        if (newSpecs.series.equals(existingSpecs.series)) {
            score += 25.0;
        }
        
        // Type similarity (weight: 10%)
        if (newSpecs.type.equals(existingSpecs.type)) {
            score += 10.0;
        }
        
        // Bore similarity (weight: 35%) - most important
        score += calculateBoreSimilarity(newSpecs.bore, existingSpecs.bore) * 35.0;
        
        // Stroke similarity (weight: 15%)
        score += calculateStrokeSimilarity(newSpecs.stroke, existingSpecs.stroke) * 15.0;
        
        // Rod end type similarity (weight: 5%)
        if (newSpecs.rodEndType.equals(existingSpecs.rodEndType)) {
            score += 5.0;
        }
        
        return Math.min(score, 100.0); // Cap at 100%
    }
    
    /**
     * Calculate bore size similarity (returns 0.0 to 1.0)
     */
    private double calculateBoreSimilarity(String bore1, String bore2) {
        try {
            int b1 = Integer.parseInt(bore1);
            int b2 = Integer.parseInt(bore2);
            
            int diff = Math.abs(b1 - b2);
            
            // Perfect match
            if (diff == 0) return 1.0;
            
            // Close matches get high scores
            if (diff <= 5) return 0.95;   // Within 5mm
            if (diff <= 10) return 0.85;  // Within 10mm
            if (diff <= 15) return 0.70;  // Within 15mm
            if (diff <= 25) return 0.50;  // Within 25mm
            if (diff <= 40) return 0.30;  // Within 40mm
            
            return 0.0;
            
        } catch (NumberFormatException e) {
            return bore1.equals(bore2) ? 1.0 : 0.0;
        }
    }
    
    /**
     * Calculate stroke length similarity (returns 0.0 to 1.0)
     */
    private double calculateStrokeSimilarity(String stroke1, String stroke2) {
        try {
            int s1 = Integer.parseInt(stroke1);
            int s2 = Integer.parseInt(stroke2);
            
            int diff = Math.abs(s1 - s2);
            
            // Perfect match
            if (diff == 0) return 1.0;
            
            // Close matches get high scores
            if (diff <= 10) return 0.95;   // Within 10mm
            if (diff <= 25) return 0.85;   // Within 25mm
            if (diff <= 50) return 0.70;   // Within 50mm
            if (diff <= 100) return 0.50;  // Within 100mm
            if (diff <= 200) return 0.30;  // Within 200mm
            
            return 0.0;
            
        } catch (NumberFormatException e) {
            return stroke1.equals(stroke2) ? 1.0 : 0.0;
        }
    }
    
    /**
     * Create similar cylinder information object
     */
    private Map<String, Object> createSimilarCylinderInfo(ImaFile cylinder, CylinderSpecs specs, double similarityScore) {
        Map<String, Object> info = new HashMap<>();
        
        info.put("code", specs.itemCode);
        info.put("name", cylinder.getIma02());
        info.put("spec", cylinder.getIma021());
        info.put("similarityScore", Math.round(similarityScore * 100.0) / 100.0); // Round to 2 decimal places
        
        // Add specifications
        Map<String, String> specifications = new HashMap<>();
        specifications.put("productType", specs.productType);
        specifications.put("series", specs.series);
        specifications.put("type", specs.type);
        specifications.put("bore", specs.bore);
        specifications.put("stroke", specs.stroke);
        specifications.put("rodEndType", specs.rodEndType);
        info.put("specifications", specifications);
        
        // Add compatibility reasons
        List<String> reasons = new ArrayList<>();
        if (specs.series.equals(specs.series)) reasons.add("Same series");
        if (specs.type.equals(specs.type)) reasons.add("Same type");
        if (specs.rodEndType.equals(specs.rodEndType)) reasons.add("Same rod end type");
        
        info.put("matchReasons", reasons);
        
        return info;
    }
    
    /**
     * Generate component suggestions based on similar cylinders
     */
    public Map<String, List<Map<String, String>>> generateComponentSuggestionsFromSimilar(String newItemCode) {
        Map<String, List<Map<String, String>>> suggestions = new HashMap<>();
        
        // Initialize categories
        suggestions.put("Barrel", new ArrayList<>());
        suggestions.put("Piston", new ArrayList<>());
        suggestions.put("PistonRod", new ArrayList<>());
        suggestions.put("Seals", new ArrayList<>());
        suggestions.put("EndCaps", new ArrayList<>());
        
        try {
            CylinderSpecs specs = extractSpecifications(newItemCode);
            if (!specs.isValid()) {
                return suggestions;
            }
            
            // Generate suggestions based on specifications
            String bore = specs.bore;
            String series = specs.series;
            
            // Barrel suggestions
            suggestions.get("Barrel").add(Map.of(
                "code", "20" + series + bore + "-B01",
                "name", "Cylinder Barrel " + bore + "mm",
                "spec", "Standard barrel for " + bore + "mm bore cylinder"
            ));
            
            // Piston suggestions  
            suggestions.get("Piston").add(Map.of(
                "code", "21" + series + bore + "-P01", 
                "name", "Piston " + bore + "mm",
                "spec", "Standard piston for " + bore + "mm bore"
            ));
            
            // Piston rod suggestions
            suggestions.get("PistonRod").add(Map.of(
                "code", "21" + series + bore + "-R01",
                "name", "Piston Rod " + bore + "mm", 
                "spec", "Standard piston rod for " + bore + "mm cylinder"
            ));
            
            // Seal suggestions
            suggestions.get("Seals").addAll(Arrays.asList(
                Map.of(
                    "code", "25" + series + bore + "-S01",
                    "name", "Piston Seal Set " + bore + "mm",
                    "spec", "Complete piston seal set"
                ),
                Map.of(
                    "code", "25" + series + bore + "-S02", 
                    "name", "Rod Seal " + bore + "mm",
                    "spec", "Rod seal for " + bore + "mm cylinder"
                ),
                Map.of(
                    "code", "25" + series + bore + "-S03",
                    "name", "Wiper Seal " + bore + "mm", 
                    "spec", "Wiper seal for contamination protection"
                )
            ));
            
            // End cap suggestions
            suggestions.get("EndCaps").addAll(Arrays.asList(
                Map.of(
                    "code", "22" + series + bore + "-C01",
                    "name", "Head End Cap " + bore + "mm",
                    "spec", "Head end cap for " + bore + "mm cylinder"
                ),
                Map.of(
                    "code", "22" + series + bore + "-C02",
                    "name", "Rod End Cap " + bore + "mm", 
                    "spec", "Rod end cap for " + bore + "mm cylinder"
                )
            ));
            
        } catch (Exception e) {
            logger.error("Error generating component suggestions for: " + newItemCode, e);
        }
        
        return suggestions;
    }
    
    /**
     * Helper class to hold cylinder specifications
     */
    public static class CylinderSpecs {
        public String itemCode = "";
        public String productType = "";
        public String series = "";
        public String type = "";
        public String bore = "";
        public String stroke = "";
        public String rodEndType = "";
        public boolean valid = false;
        
        public boolean isValid() {
            return valid && !itemCode.isEmpty() && !bore.isEmpty() && !stroke.isEmpty();
        }
        
        @Override
        public String toString() {
            return String.format("CylinderSpecs{code='%s', type='%s', series='%s', bore='%s', stroke='%s', rodEnd='%s'}", 
                itemCode, productType, series, bore, stroke, rodEndType);
        }
    }
}
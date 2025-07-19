package com.jfc.gnn.model;
import java.util.HashMap;
import java.util.Map;

//=====================================================================
//6. ProductSpecifications.java - Enhanced specifications class
//=====================================================================
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSpecifications {
 private String newItemCode;   // New item code to generate BOM for
 private String bore;          // Cylinder bore diameter (mm)
 private String stroke;        // Cylinder stroke length (mm)
 private String series;        // Product series (10, 11, 12, 13, etc.)
 private String type;          // Type within series
 private String rodEndType;    // Rod end type (Y, I, E, F, etc.)
 private String installation;  // Installation type (FA, CA, CB, TC, etc.)
 private String pressure;      // Operating pressure (bar)
 private String environment;   // Operating environment
 private String application;   // Application type
 
 // Additional specifications
 private Map<String, String> customSpecs;
 
 /**
  * Convert to feature map
  */
 public Map<String, String> toFeatureMap() {
     Map<String, String> map = new HashMap<>();
     map.put("bore", bore);
     map.put("stroke", stroke);
     map.put("series", series);
     map.put("type", type);
     map.put("rodEndType", rodEndType);
     map.put("installation", installation);
     map.put("pressure", pressure);
     map.put("environment", environment);
     map.put("application", application);
     
     if (customSpecs != null) {
         map.putAll(customSpecs);
     }
     
     return map;
 }
 
 /**
  * Validate specifications
  */
 public boolean isValid() {
     return newItemCode != null && !newItemCode.isEmpty() &&
            bore != null && !bore.isEmpty() &&
            stroke != null && !stroke.isEmpty() &&
            series != null && !series.isEmpty();
 }
 
 /**
  * Get numeric bore value
  */
 public float getBoreNumeric() {
     try {
         return bore != null ? Float.parseFloat(bore) : 0f;
     } catch (NumberFormatException e) {
         return 0f;
     }
 }
 
 /**
  * Get numeric stroke value
  */
 public float getStrokeNumeric() {
     try {
         return stroke != null ? Float.parseFloat(stroke) : 0f;
     } catch (NumberFormatException e) {
         return 0f;
     }
 }
}
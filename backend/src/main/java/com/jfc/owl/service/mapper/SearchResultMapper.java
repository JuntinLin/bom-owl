package com.jfc.owl.service.mapper;

import com.jfc.owl.dto.search.SearchResultDTO;
import com.jfc.owl.dto.search.SimilarBOMDTO;
import com.jfc.owl.entity.OWLKnowledgeBase;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper for converting search results to DTOs
 */
@Component
public class SearchResultMapper {

	/**
	 * Convert search results to SearchResultDTO
	 */
	public SearchResultDTO toSearchResultDTO(List<Map<String, Object>> results
			, Map<String, String> searchCriteria,
			long startTime, SearchResultDTO.SearchConfiguration configuration) {

		long endTime = System.currentTimeMillis();

		// 確保正確設置 totalResults
		int totalResults = results != null ? results.size() : 0;
		
		// Convert results to SimilarBOMDTOs
		//List<SimilarBOMDTO> similarBOMs = results.stream().map(this::mapToSimilarBOMDTO).collect(Collectors.toList());
		List<SimilarBOMDTO> similarBOMs = new ArrayList<>();
	    if (results != null && !results.isEmpty()) {
	        for (Map<String, Object> result : results) {
	            SimilarBOMDTO dto = mapToSimilarBOMDTO(result);
	            similarBOMs.add(dto);
	        }
	    }
	    
		return SearchResultDTO.builder()
				.status(SearchResultDTO.SearchStatus.COMPLETED)
				.startTime(LocalDateTime.now().minusNanos((endTime - startTime) * 1_000_000))
				.endTime(LocalDateTime.now()).results(similarBOMs).totalResults(similarBOMs.size())
				.results(similarBOMs)  // 確保設置 results
		        .totalResults(totalResults)  // 確保設置 totalResults
				.searchCriteria(searchCriteria)
				.durationMs(endTime - startTime)
				.itemsProcessed(results.size())
				.timeoutCount(0)
				.configuration(configuration)
				.build();
	}

	/**
	 * Create error result
	 */
	public SearchResultDTO createErrorResult(String searchId, Map<String, String> searchCriteria, String error,
			String errorDetail) {

		return SearchResultDTO.builder().searchId(searchId).status(SearchResultDTO.SearchStatus.FAILED)
				.startTime(LocalDateTime.now()).endTime(LocalDateTime.now()).error(error).errorDetail(errorDetail)
				.searchCriteria(searchCriteria).totalResults(0).durationMs(0).itemsProcessed(0).build();
	}

	/**
	 * Create timeout result
	 */
	public SearchResultDTO createTimeoutResult(String searchId, Map<String, String> searchCriteria,
			int timeoutSeconds) {

		return SearchResultDTO.builder().searchId(searchId).status(SearchResultDTO.SearchStatus.PARTIAL)
				.startTime(LocalDateTime.now().minusSeconds(timeoutSeconds)).endTime(LocalDateTime.now())
				.error("Search timeout")
				.errorDetail("The search operation exceeded the timeout limit of " + timeoutSeconds + " seconds")
				.searchCriteria(searchCriteria).totalResults(0).durationMs(timeoutSeconds * 1000L).itemsProcessed(0)
				.timeoutCount(1).build();
	}

	/**
	 * Map individual result to SimilarBOMDTO
	 */
	private SimilarBOMDTO mapToSimilarBOMDTO(Map<String, Object> result) {
		SimilarBOMDTO.SimilarBOMDTOBuilder builder = SimilarBOMDTO.builder()
				.masterItemCode(getStringValue(result, "masterItemCode")).fileName(getStringValue(result, "fileName"))
				.description(getStringValue(result, "description"))
				.similarityScore(getDoubleValue(result, "similarityScore"))
				.createdAt(getLocalDateTimeValue(result, "createdAt")).tripleCount(getIntValue(result, "tripleCount"))
				.fileSize(getLongValue(result, "fileSize", 0L)).format(getStringValue(result, "format", "RDF/XML"));

		// Optional fields
		if (result.containsKey("isHydraulicCylinder")) {
			builder.isHydraulicCylinder(getBooleanValue(result, "isHydraulicCylinder"));
		}

		if (result.containsKey("hydraulicCylinderSpecs")) {
			builder.hydraulicCylinderSpecs(getStringValue(result, "hydraulicCylinderSpecs"));

			// Try to parse specs if present
			String specsJson = getStringValue(result, "hydraulicCylinderSpecs");
			if (specsJson != null && !specsJson.isEmpty()) {
				SimilarBOMDTO.HydraulicCylinderSpecs parsedSpecs = parseHydraulicSpecs(specsJson);
				if (parsedSpecs != null) {
					builder.parsedSpecs(parsedSpecs);
				}
			}
		}

		if (result.containsKey("sourceSystem")) {
			builder.sourceSystem(getStringValue(result, "sourceSystem"));
		}

		if (result.containsKey("validationStatus")) {
			builder.validationStatus(getStringValue(result, "validationStatus"));
		}

		if (result.containsKey("componentCount")) {
			builder.componentCount(getIntValue(result, "componentCount"));
		}

		if (result.containsKey("qualityScore")) {
			builder.qualityScore(getDoubleValue(result, "qualityScore"));
		}

		if (result.containsKey("usageCount")) {
			builder.usageCount(getIntValue(result, "usageCount"));
		}

		if (result.containsKey("tags")) {
			builder.tags(getStringValue(result, "tags"));
		}

		if (result.containsKey("lastUsedAt")) {
			builder.lastUsedAt(getLocalDateTimeValue(result, "lastUsedAt"));
		}

		return builder.build();
	}

	/**
	 * Parse hydraulic cylinder specifications from JSON string
	 */
	private SimilarBOMDTO.HydraulicCylinderSpecs parseHydraulicSpecs(String specsJson) {
		try {
			// Simple JSON parsing - in production, use Jackson or Gson
			SimilarBOMDTO.HydraulicCylinderSpecs specs = new SimilarBOMDTO.HydraulicCylinderSpecs();

			// Remove braces and split by comma
			String content = specsJson.trim();
			if (content.startsWith("{") && content.endsWith("}")) {
				content = content.substring(1, content.length() - 1);
			}

			// Parse key-value pairs
			String[] pairs = content.split(",");
			for (String pair : pairs) {
				String[] keyValue = pair.trim().split(":");
				if (keyValue.length == 2) {
					String key = keyValue[0].trim().replaceAll("\"", "");
					String value = keyValue[1].trim().replaceAll("\"", "");

					switch (key) {
					case "series":
						specs.setSeries(value);
						break;
					case "type":
						specs.setType(value);
						break;
					case "bore":
						specs.setBore(value);
						break;
					case "stroke":
						specs.setStroke(value);
						break;
					case "rodEndType":
						specs.setRodEndType(value);
						break;
					case "installationType":
						specs.setInstallationType(value);
						break;
					case "shaftEndJoin":
						specs.setShaftEndJoin(value);
						break;
					}
				}
			}

			return specs;

		} catch (Exception e) {
			// Log error and return null
			return null;
		}
	}

	// Helper methods for safe type conversion

	private String getStringValue(Map<String, Object> map, String key) {
		return getStringValue(map, key, "");
	}

	private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
		Object value = map.get(key);
		return value != null ? value.toString() : defaultValue;
	}

	private double getDoubleValue(Map<String, Object> map, String key) {
		return getDoubleValue(map, key, 0.0);
	}

	private double getDoubleValue(Map<String, Object> map, String key, double defaultValue) {
		Object value = map.get(key);
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		try {
			return value != null ? Double.parseDouble(value.toString()) : defaultValue;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private int getIntValue(Map<String, Object> map, String key) {
		return getIntValue(map, key, 0);
	}

	private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
		Object value = map.get(key);
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		try {
			return value != null ? Integer.parseInt(value.toString()) : defaultValue;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private long getLongValue(Map<String, Object> map, String key, long defaultValue) {
		Object value = map.get(key);
		if (value instanceof Number) {
			return ((Number) value).longValue();
		}
		try {
			return value != null ? Long.parseLong(value.toString()) : defaultValue;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private boolean getBooleanValue(Map<String, Object> map, String key) {
		Object value = map.get(key);
		if (value instanceof Boolean) {
			return (Boolean) value;
		}
		return value != null && Boolean.parseBoolean(value.toString());
	}

	private LocalDateTime getLocalDateTimeValue(Map<String, Object> map, String key) {
		Object value = map.get(key);
		if (value == null) {
			return null;
		}

		if (value instanceof LocalDateTime) {
			return (LocalDateTime) value;
		}

		// Try to parse string representation
		try {
			String dateStr = value.toString();

			// Try different date formats
			// ISO format with T separator
			if (dateStr.contains("T")) {
				return LocalDateTime.parse(dateStr);
			}

			// Format with space separator
			if (dateStr.contains(" ")) {
				// Try common formats
				try {
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
					return LocalDateTime.parse(dateStr, formatter);
				} catch (DateTimeParseException e) {
					// Try replacing space with T
					return LocalDateTime.parse(dateStr.replace(" ", "T"));
				}
			}

			// Try default parsing
			return LocalDateTime.parse(dateStr);

		} catch (Exception e) {
			// Log warning and return null
			// In production, you might want to log this
			return null;
		}
	}
}
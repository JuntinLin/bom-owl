package com.jfc.gnn.repository;

import lombok.Builder;
import lombok.Data;
import com.jfc.gnn.config.GnnModelConfig;
import java.util.Date;
import java.util.Map;

/**
 * Metadata for stored models
 */
@Data
@Builder
public class ModelMetadata {
    private String modelId;
    private String modelName;
    private String version;
    private GnnModelConfig config;
    private Date createdAt;
    private Date lastModified;
    private long fileSize;
    private String description;
    private Map<String, Object> metrics;
}
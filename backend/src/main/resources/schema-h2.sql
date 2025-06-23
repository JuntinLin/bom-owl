-- Drop table if exists (for clean testing)
DROP TABLE IF EXISTS owl_knowledge_base CASCADE;

-- Create OWL Knowledge Base table with all enhancements
CREATE TABLE owl_knowledge_base (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    master_item_code VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    format VARCHAR(20) NOT NULL,
    include_hierarchy BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(1000),
    file_size BIGINT,
    file_hash VARCHAR(64),
    triple_count INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version INTEGER NOT NULL DEFAULT 1,
    tags VARCHAR(500),
    usage_count INTEGER DEFAULT 0,
    last_used_at TIMESTAMP,
    
    -- Hydraulic Cylinder Specific Fields
    is_hydraulic_cylinder BOOLEAN DEFAULT FALSE,
    hydraulic_cylinder_specs CLOB,
    quality_score DOUBLE,
    component_count INTEGER,
    has_hierarchy BOOLEAN,
    hierarchy_depth INTEGER,
    source_system VARCHAR(100) DEFAULT 'TIPTOP',
    checksum VARCHAR(64),
    last_accessed TIMESTAMP,
    priority_level INTEGER DEFAULT 3,
    validation_status VARCHAR(50) DEFAULT 'PENDING',
    error_messages CLOB,
    performance_metrics CLOB,
    similarity_threshold DOUBLE DEFAULT 0.7,
    export_duration_ms BIGINT,
    compression_ratio DOUBLE
);

-- Create indexes for performance
CREATE INDEX idx_master_item_code ON owl_knowledge_base(master_item_code);
CREATE INDEX idx_active ON owl_knowledge_base(active);
CREATE INDEX idx_hydraulic_cylinder ON owl_knowledge_base(is_hydraulic_cylinder);
CREATE INDEX idx_created_at ON owl_knowledge_base(created_at);
CREATE INDEX idx_format ON owl_knowledge_base(format);
CREATE INDEX idx_quality_score ON owl_knowledge_base(quality_score);
CREATE INDEX idx_validation_status ON owl_knowledge_base(validation_status);
CREATE INDEX idx_usage_count ON owl_knowledge_base(usage_count);
CREATE INDEX idx_last_accessed ON owl_knowledge_base(last_accessed);

-- Create composite indexes for complex queries
CREATE INDEX idx_active_hydraulic ON owl_knowledge_base(active, is_hydraulic_cylinder);
CREATE INDEX idx_active_format ON owl_knowledge_base(active, format);
CREATE INDEX idx_active_quality ON owl_knowledge_base(active, quality_score);

-- Create function-based index for hydraulic cylinder detection (H2 doesn't support, but documenting intent)
-- CREATE INDEX idx_hydraulic_prefix ON owl_knowledge_base(CASE WHEN SUBSTR(master_item_code, 1, 1) IN ('3', '4') THEN 1 ELSE 0 END);
-- Drop table if exists (for clean testing)
-- DROP TABLE IF EXISTS owl_processing_log CASCADE;
-- DROP TABLE IF EXISTS owl_knowledge_base CASCADE;

-- Create OWL Knowledge Base table with all enhancements
CREATE TABLE IF NOT EXISTS owl_knowledge_base (
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
    compression_ratio DOUBLE,
    
    batch_id VARCHAR(50),
    processing_sequence INTEGER,
    retry_count INTEGER DEFAULT 0,
    last_error_time TIMESTAMP
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
CREATE INDEX idx_batch_id ON owl_knowledge_base(batch_id);

CREATE INDEX idx_owl_kb_source_system ON owl_knowledge_base(source_system);
CREATE INDEX idx_owl_kb_hydraulic_active ON owl_knowledge_base(is_hydraulic_cylinder, active);

-- Create composite indexes for complex queries
CREATE INDEX idx_active_hydraulic ON owl_knowledge_base(active, is_hydraulic_cylinder);
CREATE INDEX idx_active_format ON owl_knowledge_base(active, format);
CREATE INDEX idx_active_quality ON owl_knowledge_base(active, quality_score);
CREATE INDEX idx_active_master_code ON owl_knowledge_base(active, master_item_code);

-- 新增：處理進度追蹤表
CREATE TABLE owl_processing_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id VARCHAR(50) NOT NULL UNIQUE,
    batch_name VARCHAR(255),
    total_items INTEGER NOT NULL,
    processed_items INTEGER DEFAULT 0,
    success_count INTEGER DEFAULT 0,
    failure_count INTEGER DEFAULT 0,
    skipped_count INTEGER DEFAULT 0,
    start_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'INITIALIZING',
    error_details CLOB,
    processing_parameters CLOB,
    average_time_per_item DOUBLE,
    estimated_completion_time TIMESTAMP,
    last_update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    initiated_by VARCHAR(100),
    notes VARCHAR(1000),
    
    -- 效能統計
    total_file_size BIGINT DEFAULT 0,
    total_triple_count BIGINT DEFAULT 0,
    min_processing_time_ms BIGINT,
    max_processing_time_ms BIGINT,
    
    -- 檢查點支援（用於斷點續傳）
    last_processed_item_code VARCHAR(50),
    checkpoint_data CLOB,
    
    CONSTRAINT chk_status CHECK (status IN ('INITIALIZING', 'PROCESSING', 'PAUSED', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

-- Create indexes for processing log
CREATE INDEX idx_pl_batch_id ON owl_processing_log(batch_id);
CREATE INDEX idx_pl_status ON owl_processing_log(status);
CREATE INDEX idx_pl_start_time ON owl_processing_log(start_time);

-- 新增：處理失敗記錄表（詳細記錄每個失敗項目）
CREATE TABLE IF NOT EXISTS owl_processing_failures (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id VARCHAR(50) NOT NULL,
    master_item_code VARCHAR(50) NOT NULL,
    error_type VARCHAR(100),
    error_message VARCHAR(2000),
    stack_trace CLOB,
    failure_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    retry_count INTEGER DEFAULT 0,
    resolved BOOLEAN DEFAULT FALSE,
    resolution_notes VARCHAR(1000),
    
    FOREIGN KEY (batch_id) REFERENCES owl_processing_log(batch_id)
);

CREATE INDEX idx_pf_batch_id ON owl_processing_failures(batch_id);
CREATE INDEX idx_pf_master_code ON owl_processing_failures(master_item_code);
CREATE INDEX idx_pf_resolved ON owl_processing_failures(resolved);

-- 新增：系統設定表（用於儲存系統參數）
CREATE TABLE owl_system_config (
    config_key VARCHAR(100) PRIMARY KEY,
    config_value VARCHAR(500),
    config_type VARCHAR(20),
    description VARCHAR(500),
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 插入預設系統設定
INSERT INTO owl_system_config (config_key, config_value, config_type, description) VALUES
('batch.size', '50', 'INTEGER', 'Default batch size for processing'),
('thread.pool.size', '8', 'INTEGER', 'Thread pool size for parallel processing'),
('timeout.seconds', '120', 'INTEGER', 'Default timeout in seconds'),
('retry.max.attempts', '3', 'INTEGER', 'Maximum retry attempts for failed items'),
('auto.resume.enabled', 'true', 'BOOLEAN', 'Enable automatic resume of interrupted batches');

-- 建立視圖：活躍的液壓缸知識庫
CREATE OR REPLACE VIEW v_active_hydraulic_cylinders AS
SELECT 
    id,
    master_item_code,
    file_name,
    format,
    quality_score,
    component_count,
    hydraulic_cylinder_specs,
    created_at,
    usage_count,
    last_accessed
FROM owl_knowledge_base
WHERE active = TRUE 
    AND is_hydraulic_cylinder = TRUE
    AND validation_status = 'VALIDATED'
ORDER BY quality_score DESC, usage_count DESC;

-- 建立視圖：批次處理摘要
CREATE OR REPLACE VIEW v_batch_processing_summary AS
SELECT 
    batch_id,
    batch_name,
    status,
    total_items,
    processed_items,
    success_count,
    failure_count,
    CASE 
        WHEN total_items > 0 THEN ROUND((processed_items * 100.0 / total_items), 2)
        ELSE 0 
    END as progress_percentage,
    start_time,
    end_time,
    CASE 
        WHEN end_time IS NOT NULL THEN 
            TIMESTAMPDIFF(SECOND, start_time, end_time)
        ELSE 
            TIMESTAMPDIFF(SECOND, start_time, CURRENT_TIMESTAMP)
    END as duration_seconds
FROM owl_processing_log
ORDER BY start_time DESC;

-- 為了支援大量資料處理，增加 H2 特定的效能設定
-- 這些指令會在應用程式啟動時執行
-- SET CACHE_SIZE 131072;  -- 增加快取大小到 128MB
-- SET MAX_MEMORY_ROWS 100000;  -- 記憶體中最大行數
-- SET COMPRESS_LOB LZF;  -- 啟用 LOB 壓縮
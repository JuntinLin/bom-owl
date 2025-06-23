-- Insert sample test data for OWL Knowledge Base
-- Regular BOMs
INSERT INTO owl_knowledge_base (
    master_item_code, file_name, file_path, format, include_hierarchy, 
    description, file_size, triple_count, quality_score, component_count,
    has_hierarchy, hierarchy_depth, is_hydraulic_cylinder, tags
) VALUES 
(
    'A001', 'bom_A001.owl', './owl-exports/bom_A001.owl', 'RDF/XML', true,
    'Standard component BOM', 15360, 250, 0.75, 15,
    true, 3, false, 'standard,component,assembly'
),
(
    'B002', 'bom_B002.ttl', './owl-exports/bom_B002.ttl', 'TURTLE', true,
    'Electronic assembly BOM', 22528, 380, 0.82, 25,
    true, 4, false, 'electronic,assembly,pcb'
);

-- Hydraulic Cylinder BOMs
INSERT INTO owl_knowledge_base (
    master_item_code, file_name, file_path, format, include_hierarchy,
    description, file_size, triple_count, quality_score, component_count,
    has_hierarchy, hierarchy_depth, is_hydraulic_cylinder, 
    hydraulic_cylinder_specs, tags, validation_status
) VALUES 
(
    '30202000001', 'hc_30202000001.owl', './owl-exports/hc_30202000001.owl', 'RDF/XML', true,
    'Hydraulic Cylinder - 50mm bore, 200mm stroke', 45056, 650, 0.88, 42,
    true, 5, true,
    '{"series":"HTM","bore":"50","stroke":"200","rodEndType":"THREADED","mountingType":"FLANGE","pressure":"210","rodDiameter":"28"}',
    'hydraulic,cylinder,HTM,50mm', 'VALID'
),
(
    '40303000002', 'hc_40303000002.owl', './owl-exports/hc_40303000002.owl', 'RDF/XML', true,
    'Hydraulic Cylinder - 80mm bore, 300mm stroke', 52224, 720, 0.91, 48,
    true, 5, true,
    '{"series":"HTH","bore":"80","stroke":"300","rodEndType":"CLEVIS","mountingType":"TRUNNION","pressure":"250","rodDiameter":"45"}',
    'hydraulic,cylinder,HTH,80mm,heavy-duty', 'VALID'
),
(
    '30101500003', 'hc_30101500003.owl', './owl-exports/hc_30101500003.owl', 'TURTLE', true,
    'Hydraulic Cylinder - 32mm bore, 150mm stroke', 38912, 580, 0.85, 38,
    true, 4, true,
    '{"series":"HTS","bore":"32","stroke":"150","rodEndType":"SPHERICAL","mountingType":"FOOT","pressure":"160","rodDiameter":"18"}',
    'hydraulic,cylinder,HTS,32mm,compact', 'VALID'
);

-- High-usage entry
INSERT INTO owl_knowledge_base (
    master_item_code, file_name, file_path, format, include_hierarchy,
    description, file_size, triple_count, quality_score, component_count,
    has_hierarchy, hierarchy_depth, is_hydraulic_cylinder, tags,
    usage_count, last_used_at, last_accessed, validation_status
) VALUES 
(
    'C003', 'bom_C003.owl', './owl-exports/bom_C003.owl', 'RDF/XML', true,
    'Popular assembly used in multiple products', 35840, 520, 0.92, 35,
    true, 4, false, 'popular,assembly,reusable',
    150, CURRENT_TIMESTAMP - INTERVAL '2' HOUR, CURRENT_TIMESTAMP - INTERVAL '2' HOUR, 'VALID'
);

-- Entry with errors
INSERT INTO owl_knowledge_base (
    master_item_code, file_name, file_path, format, include_hierarchy,
    description, file_size, triple_count, quality_score, component_count,
    has_hierarchy, hierarchy_depth, is_hydraulic_cylinder, tags,
    validation_status, error_messages
) VALUES 
(
    'E001', 'bom_E001.owl', './owl-exports/bom_E001.owl', 'RDF/XML', false,
    'BOM with validation errors', 8192, 120, 0.35, 8,
    false, 1, false, 'error,incomplete',
    'INVALID', 'Missing required components: seal_ring, o_ring. Circular dependency detected between components.'
);

-- Update timestamps for testing
UPDATE owl_knowledge_base SET updated_at = CURRENT_TIMESTAMP;
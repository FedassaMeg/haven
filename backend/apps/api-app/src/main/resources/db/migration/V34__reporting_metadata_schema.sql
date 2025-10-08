-- V34: Reporting Metadata Schema
-- HUD field mapping repository with JSONB columns
-- Versioned by HUD Data Standards release (2024 v1.0, etc.)

-- Reporting Field Mapping Table
-- Maps Haven domain fields to HUD HMIS CSV, APR, CAPER, SPM, PIT/HIC specifications
CREATE TABLE reporting_field_mapping (
    mapping_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_field VARCHAR(200) NOT NULL,
    source_entity VARCHAR(100) NOT NULL,
    target_hud_element_id VARCHAR(200) NOT NULL,
    hud_specification_type VARCHAR(50) NOT NULL,
    target_data_type VARCHAR(50) NOT NULL,
    transform_expression TEXT,
    transform_language VARCHAR(20) NOT NULL DEFAULT 'NONE',
    vawa_sensitive_field BOOLEAN NOT NULL DEFAULT FALSE,
    vawa_suppression_behavior VARCHAR(20),
    effective_from DATE NOT NULL,
    effective_to DATE,
    hud_notice_reference VARCHAR(200),
    justification TEXT,
    required_flag CHAR(1) NOT NULL DEFAULT 'O',
    validation_rules VARCHAR(500),
    csv_field_name VARCHAR(100),
    csv_field_order INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_required_flag CHECK (required_flag IN ('R', 'C', 'O')),
    CONSTRAINT chk_transform_language CHECK (transform_language IN ('NONE', 'SQL', 'JAVA_EL')),
    CONSTRAINT chk_hud_spec_type CHECK (hud_specification_type IN ('HMIS_CSV', 'CoC_APR', 'ESG_CAPER', 'SYSTEM_PERFORMANCE_MEASURES', 'PIT_HIC')),
    CONSTRAINT chk_vawa_suppression CHECK (vawa_suppression_behavior IN ('SUPPRESS', 'AGGREGATE_ONLY', 'REDACT') OR vawa_suppression_behavior IS NULL)
);

CREATE INDEX idx_mapping_source_field ON reporting_field_mapping(source_field);
CREATE INDEX idx_mapping_target_element ON reporting_field_mapping(target_hud_element_id);
CREATE INDEX idx_mapping_effective ON reporting_field_mapping(effective_from, effective_to);
CREATE INDEX idx_mapping_vawa ON reporting_field_mapping(vawa_sensitive_field);
CREATE INDEX idx_mapping_spec ON reporting_field_mapping(hud_specification_type);
CREATE INDEX idx_mapping_entity ON reporting_field_mapping(source_entity);

-- Report Specification Table
-- Stores complete HUD report definitions with sections and calculation rules
CREATE TABLE report_specification (
    spec_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    spec_name VARCHAR(200) NOT NULL,
    hud_specification_type VARCHAR(50) NOT NULL,
    spec_version VARCHAR(50) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    hud_notice_reference VARCHAR(200),
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_spec_type CHECK (hud_specification_type IN ('HMIS_CSV', 'CoC_APR', 'ESG_CAPER', 'SYSTEM_PERFORMANCE_MEASURES', 'PIT_HIC'))
);

CREATE INDEX idx_spec_type ON report_specification(hud_specification_type);
CREATE INDEX idx_spec_effective ON report_specification(effective_from, effective_to);

-- Transformation Rule Table
-- Reusable transformation logic for HUD calculations
CREATE TABLE transformation_rule (
    rule_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    transform_language VARCHAR(20) NOT NULL,
    transform_expression TEXT NOT NULL,
    parameters JSONB,
    examples JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_rule_language CHECK (transform_language IN ('SQL', 'JAVA_EL'))
);

CREATE INDEX idx_rule_name ON transformation_rule(rule_name);

-- Report Section Table (for APR, CAPER, SPM reports)
CREATE TABLE report_section (
    section_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    spec_id UUID NOT NULL REFERENCES report_specification(spec_id) ON DELETE CASCADE,
    section_code VARCHAR(50) NOT NULL,
    section_title VARCHAR(500) NOT NULL,
    section_order INTEGER NOT NULL,
    parent_section_id UUID REFERENCES report_section(section_id),
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_section_spec ON report_section(spec_id);
CREATE INDEX idx_section_parent ON report_section(parent_section_id);

-- Section Field Mapping Table
-- Links report sections to field mappings
CREATE TABLE section_field_mapping (
    section_field_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    section_id UUID NOT NULL REFERENCES report_section(section_id) ON DELETE CASCADE,
    mapping_id UUID NOT NULL REFERENCES reporting_field_mapping(mapping_id) ON DELETE CASCADE,
    field_order INTEGER,
    aggregation_function VARCHAR(50),
    filter_expression TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_aggregation CHECK (aggregation_function IN ('COUNT', 'SUM', 'AVG', 'MIN', 'MAX', 'DISTINCT_COUNT') OR aggregation_function IS NULL)
);

CREATE INDEX idx_section_field_section ON section_field_mapping(section_id);
CREATE INDEX idx_section_field_mapping ON section_field_mapping(mapping_id);

COMMENT ON TABLE reporting_field_mapping IS 'Maps Haven domain fields to HUD specifications with VAWA sensitivity flags';
COMMENT ON COLUMN reporting_field_mapping.vawa_sensitive_field IS 'Fields requiring consent check before export (CurrentLivingSituation DV indicators, Health & DV services)';
COMMENT ON COLUMN reporting_field_mapping.effective_from IS 'HUD Data Standards release date (e.g., 2024-10-01 for HUD 2024 v1.0)';
COMMENT ON TABLE transformation_rule IS 'Reusable HUD calculation logic (age calculation, project type grouping, VAWA redaction)';

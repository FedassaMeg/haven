-- PIT/HIC Materialized Views for Optimized HUD Reporting
-- Provides pre-aggregated data for fast report generation

-- ========================================
-- PIT Census Aggregation Tables
-- ========================================

-- Main PIT census data table
CREATE TABLE pit_census_data (
    census_id UUID PRIMARY KEY,
    census_date DATE NOT NULL,
    continuum_code VARCHAR(10) NOT NULL,
    organization_id VARCHAR(50) NOT NULL,

    -- Household counts
    total_households INTEGER DEFAULT 0,
    households_with_children INTEGER DEFAULT 0,
    households_without_children INTEGER DEFAULT 0,
    households_with_only_children INTEGER DEFAULT 0,

    -- Person counts by age
    total_persons INTEGER DEFAULT 0,
    persons_under_18 INTEGER DEFAULT 0,
    persons_18_to_24 INTEGER DEFAULT 0,
    persons_over_24 INTEGER DEFAULT 0,

    -- Person counts by gender
    persons_male INTEGER DEFAULT 0,
    persons_female INTEGER DEFAULT 0,
    persons_non_binary INTEGER DEFAULT 0,
    persons_transgender INTEGER DEFAULT 0,
    persons_questioning INTEGER DEFAULT 0,

    -- Person counts by race/ethnicity
    persons_white INTEGER DEFAULT 0,
    persons_black_african_american INTEGER DEFAULT 0,
    persons_asian INTEGER DEFAULT 0,
    persons_american_indian_alaska_native INTEGER DEFAULT 0,
    persons_native_hawaiian_pacific_islander INTEGER DEFAULT 0,
    persons_multiple_races INTEGER DEFAULT 0,
    persons_hispanic_latino INTEGER DEFAULT 0,

    -- Special populations
    veteran_persons INTEGER DEFAULT 0,
    chronically_homeless_persons INTEGER DEFAULT 0,
    persons_with_disabilities INTEGER DEFAULT 0,
    persons_fleeing_dv INTEGER DEFAULT 0,
    unaccompanied_youth INTEGER DEFAULT 0,
    parenting_youth INTEGER DEFAULT 0,

    -- Location counts
    sheltered_emergency_shelter INTEGER DEFAULT 0,
    sheltered_transitional_housing INTEGER DEFAULT 0,
    sheltered_safe_haven INTEGER DEFAULT 0,
    unsheltered_persons INTEGER DEFAULT 0,

    -- Data quality metrics
    records_with_missing_data INTEGER DEFAULT 0,
    records_with_data_quality_issues INTEGER DEFAULT 0,
    data_completion_rate DECIMAL(5, 2),

    -- Metadata
    generated_at TIMESTAMP NOT NULL,
    generated_by VARCHAR(100) NOT NULL,
    metadata JSONB,

    CONSTRAINT uk_pit_census_date_continuum_org UNIQUE (census_date, continuum_code, organization_id)
);

CREATE INDEX idx_pit_census_date ON pit_census_data(census_date);
CREATE INDEX idx_pit_census_continuum ON pit_census_data(continuum_code);
CREATE INDEX idx_pit_census_organization ON pit_census_data(organization_id);
CREATE INDEX idx_pit_census_generated_at ON pit_census_data(generated_at);

-- ========================================
-- HIC Inventory Aggregation Tables
-- ========================================

-- Main HIC inventory data table
CREATE TABLE hic_inventory_data (
    inventory_id UUID PRIMARY KEY,
    inventory_date DATE NOT NULL,
    continuum_code VARCHAR(10) NOT NULL,
    organization_id VARCHAR(50) NOT NULL,

    -- Emergency Shelter inventory
    emergency_shelter_total_beds INTEGER DEFAULT 0,
    emergency_shelter_total_units INTEGER DEFAULT 0,
    emergency_shelter_veteran_beds INTEGER DEFAULT 0,
    emergency_shelter_youth_beds INTEGER DEFAULT 0,
    emergency_shelter_family_beds INTEGER DEFAULT 0,
    emergency_shelter_adult_only_beds INTEGER DEFAULT 0,
    emergency_shelter_chronic_beds INTEGER DEFAULT 0,
    emergency_shelter_seasonal_beds INTEGER DEFAULT 0,
    emergency_shelter_overflow_beds INTEGER DEFAULT 0,
    emergency_shelter_voucher_beds INTEGER DEFAULT 0,

    -- Transitional Housing inventory
    transitional_housing_total_beds INTEGER DEFAULT 0,
    transitional_housing_total_units INTEGER DEFAULT 0,
    transitional_housing_veteran_beds INTEGER DEFAULT 0,
    transitional_housing_youth_beds INTEGER DEFAULT 0,
    transitional_housing_family_beds INTEGER DEFAULT 0,
    transitional_housing_adult_only_beds INTEGER DEFAULT 0,
    transitional_housing_chronic_beds INTEGER DEFAULT 0,

    -- Safe Haven inventory
    safe_haven_total_beds INTEGER DEFAULT 0,
    safe_haven_total_units INTEGER DEFAULT 0,

    -- Rapid Re-Housing inventory
    rapid_rehousing_total_beds INTEGER DEFAULT 0,
    rapid_rehousing_total_units INTEGER DEFAULT 0,
    rapid_rehousing_veteran_beds INTEGER DEFAULT 0,
    rapid_rehousing_youth_beds INTEGER DEFAULT 0,
    rapid_rehousing_family_beds INTEGER DEFAULT 0,
    rapid_rehousing_adult_only_beds INTEGER DEFAULT 0,

    -- Permanent Supportive Housing inventory
    permanent_supportive_total_beds INTEGER DEFAULT 0,
    permanent_supportive_total_units INTEGER DEFAULT 0,
    permanent_supportive_veteran_beds INTEGER DEFAULT 0,
    permanent_supportive_youth_beds INTEGER DEFAULT 0,
    permanent_supportive_family_beds INTEGER DEFAULT 0,
    permanent_supportive_adult_only_beds INTEGER DEFAULT 0,
    permanent_supportive_chronic_beds INTEGER DEFAULT 0,

    -- Other Permanent Housing inventory
    other_permanent_housing_total_beds INTEGER DEFAULT 0,
    other_permanent_housing_total_units INTEGER DEFAULT 0,

    -- Utilization rates
    emergency_shelter_utilization_rate DECIMAL(5, 2),
    transitional_housing_utilization_rate DECIMAL(5, 2),
    safe_haven_utilization_rate DECIMAL(5, 2),
    rapid_rehousing_utilization_rate DECIMAL(5, 2),
    permanent_supportive_utilization_rate DECIMAL(5, 2),

    -- PIT occupancy
    pit_occupied_emergency_shelter INTEGER DEFAULT 0,
    pit_occupied_transitional_housing INTEGER DEFAULT 0,
    pit_occupied_safe_haven INTEGER DEFAULT 0,
    pit_occupied_rapid_rehousing INTEGER DEFAULT 0,
    pit_occupied_permanent_supportive INTEGER DEFAULT 0,

    -- Data quality metrics
    projects_with_missing_inventory INTEGER DEFAULT 0,
    projects_with_inconsistent_data INTEGER DEFAULT 0,
    inventory_data_completion_rate DECIMAL(5, 2),

    -- Funding source data
    beds_by_funding_source JSONB,
    units_by_funding_source JSONB,

    -- Metadata
    generated_at TIMESTAMP NOT NULL,
    generated_by VARCHAR(100) NOT NULL,
    metadata JSONB,

    CONSTRAINT uk_hic_inventory_date_continuum_org UNIQUE (inventory_date, continuum_code, organization_id)
);

CREATE INDEX idx_hic_inventory_date ON hic_inventory_data(inventory_date);
CREATE INDEX idx_hic_inventory_continuum ON hic_inventory_data(continuum_code);
CREATE INDEX idx_hic_inventory_organization ON hic_inventory_data(organization_id);
CREATE INDEX idx_hic_inventory_generated_at ON hic_inventory_data(generated_at);

-- ========================================
-- Materialized Views for Fast Aggregation
-- ========================================

-- PIT Census Summary by Continuum
CREATE MATERIALIZED VIEW mv_pit_census_summary AS
SELECT
    census_date,
    continuum_code,
    SUM(total_persons) as total_persons,
    SUM(total_households) as total_households,
    SUM(sheltered_emergency_shelter) as total_sheltered_es,
    SUM(sheltered_transitional_housing) as total_sheltered_th,
    SUM(sheltered_safe_haven) as total_sheltered_sh,
    SUM(unsheltered_persons) as total_unsheltered,
    SUM(veteran_persons) as total_veterans,
    SUM(chronically_homeless_persons) as total_chronic,
    SUM(persons_with_disabilities) as total_disabled,
    SUM(persons_fleeing_dv) as total_fleeing_dv,
    AVG(data_completion_rate) as avg_data_completion_rate,
    MAX(generated_at) as last_updated
FROM pit_census_data
GROUP BY census_date, continuum_code;

CREATE UNIQUE INDEX idx_mv_pit_census_summary ON mv_pit_census_summary(census_date, continuum_code);

-- HIC Inventory Summary by Continuum
CREATE MATERIALIZED VIEW mv_hic_inventory_summary AS
SELECT
    inventory_date,
    continuum_code,
    SUM(emergency_shelter_total_beds) as total_es_beds,
    SUM(transitional_housing_total_beds) as total_th_beds,
    SUM(safe_haven_total_beds) as total_sh_beds,
    SUM(rapid_rehousing_total_beds) as total_rrh_beds,
    SUM(permanent_supportive_total_beds) as total_psh_beds,
    SUM(other_permanent_housing_total_beds) as total_oph_beds,
    SUM(emergency_shelter_total_units) as total_es_units,
    SUM(transitional_housing_total_units) as total_th_units,
    SUM(safe_haven_total_units) as total_sh_units,
    SUM(rapid_rehousing_total_units) as total_rrh_units,
    SUM(permanent_supportive_total_units) as total_psh_units,
    SUM(other_permanent_housing_total_units) as total_oph_units,
    AVG(emergency_shelter_utilization_rate) as avg_es_utilization,
    AVG(transitional_housing_utilization_rate) as avg_th_utilization,
    AVG(permanent_supportive_utilization_rate) as avg_psh_utilization,
    AVG(inventory_data_completion_rate) as avg_data_completion_rate,
    MAX(generated_at) as last_updated
FROM hic_inventory_data
GROUP BY inventory_date, continuum_code;

CREATE UNIQUE INDEX idx_mv_hic_inventory_summary ON mv_hic_inventory_summary(inventory_date, continuum_code);

-- Combined PIT/HIC View for Annual Reports
CREATE MATERIALIZED VIEW mv_pit_hic_annual_report AS
SELECT
    p.census_date as report_date,
    p.continuum_code,
    p.total_persons,
    p.total_households,
    p.total_sheltered_es + p.total_sheltered_th + p.total_sheltered_sh as total_sheltered,
    p.total_unsheltered,
    p.total_veterans,
    p.total_chronic,
    h.total_es_beds + h.total_th_beds + h.total_sh_beds as total_emergency_beds,
    h.total_rrh_beds + h.total_psh_beds + h.total_oph_beds as total_permanent_beds,
    h.avg_es_utilization,
    h.avg_psh_utilization,
    CASE
        WHEN h.total_es_beds > 0 THEN
            CAST(p.total_sheltered_es AS DECIMAL) / h.total_es_beds * 100
        ELSE 0
    END as pit_es_occupancy_rate,
    p.last_updated as pit_last_updated,
    h.last_updated as hic_last_updated
FROM mv_pit_census_summary p
LEFT JOIN mv_hic_inventory_summary h
    ON p.census_date = h.inventory_date
    AND p.continuum_code = h.continuum_code;

CREATE UNIQUE INDEX idx_mv_pit_hic_annual ON mv_pit_hic_annual_report(report_date, continuum_code);

-- ========================================
-- ETL Job Tracking Tables
-- ========================================

CREATE TABLE pit_hic_etl_jobs (
    job_id UUID PRIMARY KEY,
    job_type VARCHAR(20) NOT NULL, -- 'PIT_CENSUS' or 'HIC_INVENTORY'
    process_date DATE NOT NULL,
    trigger_type VARCHAR(20) NOT NULL, -- 'SCHEDULED', 'MANUAL', 'ADHOC'
    state VARCHAR(20) NOT NULL, -- 'PENDING', 'RUNNING', 'COMPLETED', 'FAILED'
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    processed_count INTEGER DEFAULT 0,
    error_count INTEGER DEFAULT 0,
    initiated_by VARCHAR(100),
    error_message TEXT,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_etl_jobs_state ON pit_hic_etl_jobs(state);
CREATE INDEX idx_etl_jobs_process_date ON pit_hic_etl_jobs(process_date);
CREATE INDEX idx_etl_jobs_created_at ON pit_hic_etl_jobs(created_at);

-- Job metrics table
CREATE TABLE pit_hic_etl_metrics (
    metric_id UUID PRIMARY KEY,
    job_id UUID REFERENCES pit_hic_etl_jobs(job_id),
    metric_type VARCHAR(50) NOT NULL,
    metric_value DECIMAL(10, 2),
    metric_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    details JSONB
);

CREATE INDEX idx_etl_metrics_job_id ON pit_hic_etl_metrics(job_id);
CREATE INDEX idx_etl_metrics_type ON pit_hic_etl_metrics(metric_type);

-- ========================================
-- Data Quality Monitoring Tables
-- ========================================

CREATE TABLE pit_hic_data_quality_checks (
    check_id UUID PRIMARY KEY,
    census_id UUID,
    inventory_id UUID,
    check_type VARCHAR(50) NOT NULL,
    check_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    passed BOOLEAN NOT NULL,
    error_count INTEGER DEFAULT 0,
    warning_count INTEGER DEFAULT 0,
    errors JSONB,
    warnings JSONB,
    metadata JSONB
);

CREATE INDEX idx_quality_checks_census_id ON pit_hic_data_quality_checks(census_id);
CREATE INDEX idx_quality_checks_inventory_id ON pit_hic_data_quality_checks(inventory_id);
CREATE INDEX idx_quality_checks_type ON pit_hic_data_quality_checks(check_type);
CREATE INDEX idx_quality_checks_date ON pit_hic_data_quality_checks(check_date);

-- ========================================
-- Export Audit Trail
-- ========================================

CREATE TABLE pit_hic_export_audit (
    export_id UUID PRIMARY KEY,
    export_type VARCHAR(50) NOT NULL, -- 'PIT', 'HIC', 'COMBINED'
    export_format VARCHAR(20) NOT NULL, -- 'CSV', 'PDF', 'JSON', 'XML'
    census_date DATE,
    continuum_code VARCHAR(10),
    organization_id VARCHAR(50),
    exported_by VARCHAR(100) NOT NULL,
    exported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    export_purpose TEXT,
    file_path TEXT,
    file_size_bytes BIGINT,
    record_count INTEGER,
    metadata JSONB
);

CREATE INDEX idx_export_audit_type ON pit_hic_export_audit(export_type);
CREATE INDEX idx_export_audit_date ON pit_hic_export_audit(census_date);
CREATE INDEX idx_export_audit_exported_at ON pit_hic_export_audit(exported_at);
CREATE INDEX idx_export_audit_exported_by ON pit_hic_export_audit(exported_by);

-- ========================================
-- Functions for Materialized View Refresh
-- ========================================

-- Function to refresh all PIT/HIC materialized views
CREATE OR REPLACE FUNCTION refresh_pit_hic_views()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_pit_census_summary;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_hic_inventory_summary;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_pit_hic_annual_report;
END;
$$ LANGUAGE plpgsql;

-- Scheduled job to refresh views daily at 2 AM
-- Note: This requires pg_cron extension
-- CREATE EXTENSION IF NOT EXISTS pg_cron;
-- SELECT cron.schedule('refresh-pit-hic-views', '0 2 * * *', 'SELECT refresh_pit_hic_views();');

-- ========================================
-- Duplicate Detection Table
-- ========================================

CREATE TABLE pit_hic_duplicate_detection (
    detection_id UUID PRIMARY KEY,
    census_id UUID,
    client_hash VARCHAR(256) NOT NULL,
    detection_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    duplicate_count INTEGER DEFAULT 0,
    resolution_status VARCHAR(20), -- 'PENDING', 'RESOLVED', 'IGNORED'
    resolved_by VARCHAR(100),
    resolved_at TIMESTAMP,
    notes TEXT
);

CREATE INDEX idx_duplicate_detection_census ON pit_hic_duplicate_detection(census_id);
CREATE INDEX idx_duplicate_detection_hash ON pit_hic_duplicate_detection(client_hash);
CREATE INDEX idx_duplicate_detection_status ON pit_hic_duplicate_detection(resolution_status);
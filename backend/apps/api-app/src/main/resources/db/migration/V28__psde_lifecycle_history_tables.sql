-- ============================================================================
-- V28: PSDE Lifecycle and History Tables Creation
-- ============================================================================

-- Set default schema
SET search_path TO haven, public;

-- ============================================================================
-- Create lookup tables for correction reasons and lifecycle status
-- ============================================================================

-- Correction reason codes for PSDE data corrections
CREATE TABLE IF NOT EXISTS psde_correction_reasons (
    code VARCHAR(30) PRIMARY KEY,
    description TEXT NOT NULL,
    requires_supervisor_approval BOOLEAN DEFAULT false,
    allows_backdating BOOLEAN DEFAULT false,
    max_backdate_days INTEGER DEFAULT 30,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Insert standard correction reason codes
INSERT INTO psde_correction_reasons (code, description, requires_supervisor_approval, allows_backdating, max_backdate_days) VALUES
('DATA_ENTRY', 'Correction of data entry error', false, true, 7),
('CLIENT_CORRECTION', 'Client provided corrected information', false, true, 30),
('SYSTEM_ERROR', 'System or technical error correction', true, true, 90),
('POLICY_CHANGE', 'Correction due to policy interpretation change', true, true, 365),
('AUDIT', 'Correction based on audit finding', true, true, 730),
('SUPERVISOR', 'Correction following supervisor review', true, true, 30)
ON CONFLICT (code) DO NOTHING;

-- PSDE lifecycle status enumeration
CREATE TABLE IF NOT EXISTS psde_lifecycle_status (
    status VARCHAR(20) PRIMARY KEY,
    description TEXT NOT NULL,
    is_active_status BOOLEAN DEFAULT false,
    sort_order INTEGER DEFAULT 0
);

-- Insert lifecycle status values
INSERT INTO psde_lifecycle_status (status, description, is_active_status, sort_order) VALUES
('ACTIVE', 'Currently active record', true, 1),
('SUPERSEDED', 'Superseded by newer version', false, 2),
('CORRECTED', 'Corrected by another record', false, 3),
('DELETED', 'Soft deleted record', false, 4)
ON CONFLICT (status) DO NOTHING;

-- ============================================================================
-- Create main PSDE history table
-- ============================================================================

-- Main PSDE records table with full lifecycle support
CREATE TABLE IF NOT EXISTS intake_psde_records (
    -- Primary identifiers
    record_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    enrollment_id UUID NOT NULL REFERENCES program_enrollments(id),
    client_id UUID NOT NULL REFERENCES clients(id),

    -- Data collection metadata
    information_date DATE NOT NULL,
    collection_stage VARCHAR(50) NOT NULL,
    collected_by VARCHAR(100) NOT NULL,

    -- Income & Benefits (4.02-4.03)
    total_monthly_income INTEGER,
    income_from_any_source VARCHAR(20),
    is_earned_income_imputed BOOLEAN DEFAULT false,
    is_other_income_imputed BOOLEAN DEFAULT false,

    -- Health Insurance (4.04)
    covered_by_health_insurance VARCHAR(20),
    no_insurance_reason VARCHAR(20),
    has_vawa_protected_health_info BOOLEAN DEFAULT false,

    -- Disability Information (4.05-4.10)
    physical_disability VARCHAR(20),
    developmental_disability VARCHAR(20),
    chronic_health_condition VARCHAR(20),
    hiv_aids VARCHAR(20),
    mental_health_disorder VARCHAR(20),
    substance_use_disorder VARCHAR(20),
    has_disability_related_vawa_info BOOLEAN DEFAULT false,

    -- Domestic Violence (4.11) with enhanced VAWA protections
    domestic_violence VARCHAR(20),
    domestic_violence_recency VARCHAR(20),
    currently_fleeing_domestic_violence VARCHAR(20),
    dv_redaction_level VARCHAR(50) DEFAULT 'NO_REDACTION',
    vawa_confidentiality_requested BOOLEAN DEFAULT false,

    -- RRH Move-in specifics
    residential_move_in_date DATE,
    move_in_type VARCHAR(30),
    is_subsidized_by_rrh BOOLEAN DEFAULT false,

    -- Lifecycle management fields
    effective_start TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    effective_end TIMESTAMP WITH TIME ZONE,
    version INTEGER NOT NULL DEFAULT 1,
    lifecycle_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' REFERENCES psde_lifecycle_status(status),

    -- Supersession tracking
    superseded_at TIMESTAMP WITH TIME ZONE,
    superseded_by VARCHAR(100),
    supersedes UUID REFERENCES intake_psde_records(record_id),

    -- Correction tracking
    is_correction BOOLEAN DEFAULT false,
    corrects_record_id UUID REFERENCES intake_psde_records(record_id),
    correction_reason VARCHAR(30) REFERENCES psde_correction_reasons(code),
    corrected_at TIMESTAMP WITH TIME ZONE,
    corrected_by VARCHAR(100),

    -- Backdating support
    is_backdated BOOLEAN DEFAULT false,
    backdating_reason TEXT,

    -- Audit and data quality
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    idempotency_key VARCHAR(100) UNIQUE,

    -- Data quality tracking
    data_quality_score DECIMAL(3,2) DEFAULT 0.0,
    hmis_compliance_flags TEXT[], -- Array of compliance issue codes
    validation_errors TEXT[], -- Array of validation error messages

    CONSTRAINT chk_effective_period CHECK (effective_end IS NULL OR effective_end > effective_start),
    CONSTRAINT chk_version_positive CHECK (version > 0),
    CONSTRAINT chk_correction_logic CHECK (
        (is_correction = false AND corrects_record_id IS NULL AND correction_reason IS NULL) OR
        (is_correction = true AND corrects_record_id IS NOT NULL AND correction_reason IS NOT NULL)
    ),
    CONSTRAINT chk_backdating_logic CHECK (
        (is_backdated = false AND backdating_reason IS NULL) OR
        (is_backdated = true AND backdating_reason IS NOT NULL)
    )
);

-- ============================================================================
-- Create supporting tables for audit and reporting
-- ============================================================================

-- PSDE data changes audit log
CREATE TABLE IF NOT EXISTS psde_change_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    record_id UUID NOT NULL REFERENCES intake_psde_records(record_id),
    change_type VARCHAR(20) NOT NULL, -- CREATE, UPDATE, CORRECT, DELETE
    changed_fields TEXT[], -- Array of field names that changed
    old_values JSONB, -- Previous field values
    new_values JSONB, -- New field values
    changed_by VARCHAR(100) NOT NULL,
    change_reason TEXT,
    occurred_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ip_address INET,
    user_agent TEXT
);

-- PSDE compliance matrix for HUD reporting
CREATE TABLE IF NOT EXISTS psde_compliance_matrix (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    record_id UUID NOT NULL REFERENCES intake_psde_records(record_id),
    compliance_check_date DATE NOT NULL DEFAULT CURRENT_DATE,

    -- HUD Data Quality Requirements
    income_data_complete BOOLEAN DEFAULT false,
    disability_data_complete BOOLEAN DEFAULT false,
    dv_data_complete BOOLEAN DEFAULT false,
    health_insurance_complete BOOLEAN DEFAULT false,

    -- Conditional Logic Compliance
    dv_conditional_logic_valid BOOLEAN DEFAULT false,
    income_logic_valid BOOLEAN DEFAULT false,
    rrh_move_in_logic_valid BOOLEAN DEFAULT false,

    -- VAWA Compliance
    vawa_protections_applied BOOLEAN DEFAULT false,
    redaction_level_appropriate BOOLEAN DEFAULT false,
    confidentiality_respected BOOLEAN DEFAULT false,

    -- Overall compliance score (0.0 to 1.0)
    overall_compliance_score DECIMAL(3,2) DEFAULT 0.0,
    compliance_grade VARCHAR(2), -- A+, A, B+, B, C+, C, D+, D, F

    -- Issues and recommendations
    compliance_issues TEXT[],
    recommendations TEXT[],

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL
);

-- ============================================================================
-- Create indexes for performance
-- ============================================================================

-- Primary access patterns
CREATE INDEX IF NOT EXISTS idx_psde_records_enrollment ON intake_psde_records(enrollment_id);
CREATE INDEX IF NOT EXISTS idx_psde_records_client ON intake_psde_records(client_id);
CREATE INDEX IF NOT EXISTS idx_psde_records_info_date ON intake_psde_records(information_date);
CREATE INDEX IF NOT EXISTS idx_psde_records_collection_stage ON intake_psde_records(collection_stage);

-- Lifecycle management indexes
CREATE INDEX IF NOT EXISTS idx_psde_records_effective_period ON intake_psde_records(effective_start, effective_end);
CREATE INDEX IF NOT EXISTS idx_psde_records_lifecycle_status ON intake_psde_records(lifecycle_status);
CREATE INDEX IF NOT EXISTS idx_psde_records_version ON intake_psde_records(version);

-- Active records optimization
CREATE INDEX IF NOT EXISTS idx_psde_records_active ON intake_psde_records(enrollment_id, lifecycle_status, effective_start)
    WHERE lifecycle_status = 'ACTIVE';

-- Correction tracking indexes
CREATE INDEX IF NOT EXISTS idx_psde_records_corrections ON intake_psde_records(corrects_record_id)
    WHERE is_correction = true;
CREATE INDEX IF NOT EXISTS idx_psde_records_correction_reason ON intake_psde_records(correction_reason);
CREATE INDEX IF NOT EXISTS idx_psde_records_corrected_at ON intake_psde_records(corrected_at);

-- Supersession tracking
CREATE INDEX IF NOT EXISTS idx_psde_records_supersedes ON intake_psde_records(supersedes);
CREATE INDEX IF NOT EXISTS idx_psde_records_superseded_at ON intake_psde_records(superseded_at);

-- Audit and compliance indexes
CREATE INDEX IF NOT EXISTS idx_psde_records_created_at ON intake_psde_records(created_at);
CREATE INDEX IF NOT EXISTS idx_psde_records_collected_by ON intake_psde_records(collected_by);
CREATE INDEX IF NOT EXISTS idx_psde_records_idempotency ON intake_psde_records(idempotency_key) WHERE idempotency_key IS NOT NULL;

-- Change log indexes
CREATE INDEX IF NOT EXISTS idx_psde_change_log_record ON psde_change_log(record_id);
CREATE INDEX IF NOT EXISTS idx_psde_change_log_type ON psde_change_log(change_type);
CREATE INDEX IF NOT EXISTS idx_psde_change_log_occurred ON psde_change_log(occurred_at);
CREATE INDEX IF NOT EXISTS idx_psde_change_log_changed_by ON psde_change_log(changed_by);

-- Compliance matrix indexes
CREATE INDEX IF NOT EXISTS idx_psde_compliance_record ON psde_compliance_matrix(record_id);
CREATE INDEX IF NOT EXISTS idx_psde_compliance_check_date ON psde_compliance_matrix(compliance_check_date);
CREATE INDEX IF NOT EXISTS idx_psde_compliance_score ON psde_compliance_matrix(overall_compliance_score);
CREATE INDEX IF NOT EXISTS idx_psde_compliance_grade ON psde_compliance_matrix(compliance_grade);

-- ============================================================================
-- Create triggers for audit logging and data quality
-- ============================================================================

-- Trigger function for updated_at timestamp
CREATE OR REPLACE FUNCTION update_psde_updated_at()
RETURNS trigger AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply updated_at trigger
DROP TRIGGER IF EXISTS update_intake_psde_records_updated_at ON intake_psde_records;
CREATE TRIGGER update_intake_psde_records_updated_at
    BEFORE UPDATE ON intake_psde_records
    FOR EACH ROW EXECUTE FUNCTION update_psde_updated_at();

-- Trigger function for automatic change logging
CREATE OR REPLACE FUNCTION log_psde_changes()
RETURNS trigger AS $$
DECLARE
    change_type_val VARCHAR(20);
    changed_fields_arr TEXT[] := ARRAY[]::TEXT[];
    old_vals JSONB := '{}'::JSONB;
    new_vals JSONB := '{}'::JSONB;
BEGIN
    -- Determine change type
    IF TG_OP = 'INSERT' THEN
        change_type_val := 'CREATE';
        IF NEW.is_correction THEN
            change_type_val := 'CORRECT';
        END IF;
    ELSIF TG_OP = 'UPDATE' THEN
        change_type_val := 'UPDATE';
        IF OLD.lifecycle_status != NEW.lifecycle_status AND NEW.lifecycle_status = 'DELETED' THEN
            change_type_val := 'DELETE';
        END IF;
    END IF;

    -- For updates, track changed fields
    IF TG_OP = 'UPDATE' THEN
        -- Check each field for changes and build arrays
        IF OLD.total_monthly_income IS DISTINCT FROM NEW.total_monthly_income THEN
            changed_fields_arr := array_append(changed_fields_arr, 'total_monthly_income');
            old_vals := jsonb_set(old_vals, '{total_monthly_income}', to_jsonb(OLD.total_monthly_income));
            new_vals := jsonb_set(new_vals, '{total_monthly_income}', to_jsonb(NEW.total_monthly_income));
        END IF;

        IF OLD.domestic_violence IS DISTINCT FROM NEW.domestic_violence THEN
            changed_fields_arr := array_append(changed_fields_arr, 'domestic_violence');
            old_vals := jsonb_set(old_vals, '{domestic_violence}', to_jsonb(OLD.domestic_violence));
            new_vals := jsonb_set(new_vals, '{domestic_violence}', to_jsonb(NEW.domestic_violence));
        END IF;

        -- Add more field comparisons as needed...
    END IF;

    -- Insert change log entry
    INSERT INTO psde_change_log (
        record_id,
        change_type,
        changed_fields,
        old_values,
        new_values,
        changed_by,
        occurred_at
    ) VALUES (
        COALESCE(NEW.record_id, OLD.record_id),
        change_type_val,
        changed_fields_arr,
        old_vals,
        new_vals,
        COALESCE(NEW.updated_by, NEW.collected_by, 'SYSTEM'),
        CURRENT_TIMESTAMP
    );

    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- Apply change logging trigger
DROP TRIGGER IF EXISTS psde_change_logging ON intake_psde_records;
CREATE TRIGGER psde_change_logging
    AFTER INSERT OR UPDATE OR DELETE ON intake_psde_records
    FOR EACH ROW EXECUTE FUNCTION log_psde_changes();

-- ============================================================================
-- Create views for common queries
-- ============================================================================

-- View for currently active PSDE records
CREATE OR REPLACE VIEW active_psde_records AS
SELECT
    r.*,
    c.client_number,
    c.first_name,
    c.last_name,
    pe.enrollment_date,
    pe.status as enrollment_status
FROM intake_psde_records r
JOIN clients c ON r.client_id = c.id
JOIN program_enrollments pe ON r.enrollment_id = pe.id
WHERE r.lifecycle_status = 'ACTIVE'
AND (r.effective_end IS NULL OR r.effective_end > CURRENT_TIMESTAMP);

-- View for compliance reporting
CREATE OR REPLACE VIEW psde_compliance_report AS
SELECT
    r.record_id,
    r.client_id,
    r.enrollment_id,
    r.information_date,
    r.collection_stage,
    r.lifecycle_status,
    cm.overall_compliance_score,
    cm.compliance_grade,
    CASE
        WHEN r.domestic_violence IS NOT NULL AND r.domestic_violence != 'DATA_NOT_COLLECTED' THEN true
        ELSE false
    END as has_dv_data,
    CASE
        WHEN r.vawa_confidentiality_requested = true THEN true
        ELSE false
    END as vawa_protected,
    array_length(cm.compliance_issues, 1) as issue_count
FROM intake_psde_records r
LEFT JOIN psde_compliance_matrix cm ON r.record_id = cm.record_id
WHERE r.lifecycle_status = 'ACTIVE';

-- ============================================================================
-- Add comments for documentation
-- ============================================================================

COMMENT ON TABLE intake_psde_records IS 'Main PSDE records table with full lifecycle and audit support';
COMMENT ON TABLE psde_correction_reasons IS 'Lookup table for standardized correction reason codes';
COMMENT ON TABLE psde_lifecycle_status IS 'Enumeration of record lifecycle states';
COMMENT ON TABLE psde_change_log IS 'Detailed audit log of all changes to PSDE records';
COMMENT ON TABLE psde_compliance_matrix IS 'HUD compliance tracking and reporting matrix';

COMMENT ON VIEW active_psde_records IS 'View of currently active PSDE records with client and enrollment context';
COMMENT ON VIEW psde_compliance_report IS 'Compliance reporting view for HUD audit exports';
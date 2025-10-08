-- V38: Export Audit Metadata Schema
-- Compliance tracking for HUD export jobs per 24 CFR 578

-- Export Audit Metadata Table
CREATE TABLE export_audit_metadata (
    export_audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    export_job_id UUID NOT NULL UNIQUE,

    -- Access Context
    requested_by_user_id UUID NOT NULL,
    requested_by_user_name VARCHAR(255) NOT NULL,
    ip_address VARCHAR(100),
    session_id VARCHAR(255),
    user_agent VARCHAR(500),
    access_reason VARCHAR(1000),

    -- Export Parameters
    export_type VARCHAR(50) NOT NULL,
    reporting_period_start DATE NOT NULL,
    reporting_period_end DATE NOT NULL,
    coc_code VARCHAR(50),

    -- Integrity Verification
    sha256_hash VARCHAR(64) NOT NULL,
    blob_storage_url VARCHAR(1000),

    -- Statistics
    total_records_generated BIGINT NOT NULL,
    vawa_supressed_records BIGINT NOT NULL DEFAULT 0,
    vawa_redacted_fields BIGINT NOT NULL DEFAULT 0,

    -- Timestamps
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,

    CONSTRAINT chk_export_type CHECK (export_type IN ('HMIS_CSV', 'CoC_APR', 'ESG_CAPER', 'SYSTEM_PERFORMANCE_MEASURES', 'PIT_HIC')),
    CONSTRAINT chk_period CHECK (reporting_period_start <= reporting_period_end)
);

CREATE INDEX idx_export_audit_job ON export_audit_metadata(export_job_id);
CREATE INDEX idx_export_audit_user ON export_audit_metadata(requested_by_user_id);
CREATE INDEX idx_export_audit_generated ON export_audit_metadata(generated_at);
CREATE INDEX idx_export_audit_period ON export_audit_metadata(reporting_period_start, reporting_period_end);
CREATE INDEX idx_export_audit_expires ON export_audit_metadata(expires_at);

-- Export Included Projects (ElementCollection)
CREATE TABLE export_included_projects (
    export_audit_id UUID NOT NULL REFERENCES export_audit_metadata(export_audit_id) ON DELETE CASCADE,
    project_id UUID NOT NULL,
    PRIMARY KEY (export_audit_id, project_id)
);

CREATE INDEX idx_export_projects_audit ON export_included_projects(export_audit_id);

-- Export Generated Files (ElementCollection)
CREATE TABLE export_generated_files (
    export_audit_id UUID NOT NULL REFERENCES export_audit_metadata(export_audit_id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (export_audit_id, file_name)
);

CREATE INDEX idx_export_files_audit ON export_generated_files(export_audit_id);

COMMENT ON TABLE export_audit_metadata IS 'Audit trail for HUD export jobs - captures who, what, when, and integrity verification per 24 CFR 578';
COMMENT ON COLUMN export_audit_metadata.sha256_hash IS 'SHA-256 hash of combined CSV content for integrity verification';
COMMENT ON COLUMN export_audit_metadata.vawa_supressed_records IS 'Count of records fully suppressed due to VAWA protection (no consent)';
COMMENT ON COLUMN export_audit_metadata.vawa_redacted_fields IS 'Count of individual fields redacted due to VAWA protection';
COMMENT ON COLUMN export_audit_metadata.expires_at IS 'Retention policy expiration (90 days per HUD guidance)';

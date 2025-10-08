-- Add package integrity fields to export_audit_metadata
ALTER TABLE export_audit_metadata
ADD COLUMN digital_signature VARCHAR(512),
ADD COLUMN encrypted BOOLEAN DEFAULT FALSE,
ADD COLUMN encryption_algorithm VARCHAR(100);

-- Create index for integrity verification queries
CREATE INDEX idx_export_digital_signature ON export_audit_metadata(digital_signature);

-- Create new v2 audit metadata table with enhanced package tracking
CREATE TABLE export_audit_metadata_v2 (
    export_audit_id UUID PRIMARY KEY,
    export_job_id UUID NOT NULL UNIQUE,

    -- Access context
    requested_by_user_id UUID NOT NULL,
    requested_by_user_name VARCHAR(255) NOT NULL,
    ip_address VARCHAR(100),
    session_id VARCHAR(255),
    user_agent VARCHAR(500),
    access_reason VARCHAR(1000),

    -- Export parameters
    export_type VARCHAR(50) NOT NULL,
    reporting_period_start DATE NOT NULL,
    reporting_period_end DATE NOT NULL,
    coc_code VARCHAR(50),

    -- Integrity verification
    manifest_hash VARCHAR(64) NOT NULL,
    digital_signature VARCHAR(512),
    blob_storage_url VARCHAR(1000),

    -- Package metadata
    encrypted BOOLEAN DEFAULT FALSE,
    encryption_algorithm VARCHAR(100),

    -- Statistics
    total_records_generated BIGINT NOT NULL,
    vawa_supressed_records BIGINT NOT NULL,
    vawa_redacted_fields BIGINT NOT NULL,

    -- Timestamps
    generated_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP
);

-- Supporting tables
CREATE TABLE export_v2_included_projects (
    export_audit_id UUID NOT NULL,
    project_id UUID NOT NULL,
    CONSTRAINT fk_export_v2_projects FOREIGN KEY (export_audit_id) REFERENCES export_audit_metadata_v2(export_audit_id) ON DELETE CASCADE
);

CREATE TABLE export_v2_generated_files (
    export_audit_id UUID NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    CONSTRAINT fk_export_v2_files FOREIGN KEY (export_audit_id) REFERENCES export_audit_metadata_v2(export_audit_id) ON DELETE CASCADE
);

-- Indexes for v2
CREATE INDEX idx_v2_export_job_id ON export_audit_metadata_v2(export_job_id);
CREATE INDEX idx_v2_export_requested_by ON export_audit_metadata_v2(requested_by_user_id);
CREATE INDEX idx_v2_export_generated_at ON export_audit_metadata_v2(generated_at);
CREATE INDEX idx_v2_export_period ON export_audit_metadata_v2(reporting_period_start, reporting_period_end);
CREATE INDEX idx_v2_digital_signature ON export_audit_metadata_v2(digital_signature);

-- Comment on signature usage
COMMENT ON COLUMN export_audit_metadata_v2.digital_signature IS 'HMAC-SHA256 signature of manifest for tamper detection';
COMMENT ON COLUMN export_audit_metadata_v2.manifest_hash IS 'SHA-256 hash of manifest.json file';
COMMENT ON COLUMN export_audit_metadata_v2.encrypted IS 'Whether export package is encrypted with AES-256-GCM';

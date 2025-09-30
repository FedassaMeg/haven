-- ==================================
-- V27: Coordinated Entry Import/Export Support
-- Adds job tracking, export receipts, and consent ledger update queue
-- ==================================

CREATE TABLE IF NOT EXISTS ce_import_jobs (
    id UUID PRIMARY KEY,
    source_system VARCHAR(100) NOT NULL,
    import_format VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    initiated_by VARCHAR(200),
    file_name VARCHAR(255),
    total_records INTEGER DEFAULT 0,
    successful_records INTEGER DEFAULT 0,
    failed_records INTEGER DEFAULT 0,
    warning_count INTEGER DEFAULT 0,
    error_log TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_ce_import_jobs_status ON ce_import_jobs(status);
CREATE INDEX IF NOT EXISTS idx_ce_import_jobs_created_at ON ce_import_jobs(created_at DESC);

CREATE TABLE IF NOT EXISTS ce_export_receipts (
    id UUID PRIMARY KEY,
    enrollment_id UUID NOT NULL,
    consent_id UUID NOT NULL,
    packet_id UUID,
    recipient VARCHAR(255) NOT NULL,
    export_hash VARCHAR(128) NOT NULL,
    share_scopes TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    encryption_scheme VARCHAR(50) NOT NULL,
    encryption_key_id VARCHAR(120) NOT NULL,
    delivery_endpoint VARCHAR(255),
    package_location VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(200) NOT NULL,
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_ce_export_receipts_enrollment ON ce_export_receipts(enrollment_id);
CREATE INDEX IF NOT EXISTS idx_ce_export_receipts_consent ON ce_export_receipts(consent_id);
CREATE INDEX IF NOT EXISTS idx_ce_export_receipts_created_at ON ce_export_receipts(created_at DESC);

CREATE TABLE IF NOT EXISTS consent_ledger_update_queue (
    id UUID PRIMARY KEY,
    consent_id UUID NOT NULL,
    packet_id UUID,
    source_system VARCHAR(100) NOT NULL,
    payload_hash VARCHAR(128) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT
);

CREATE INDEX IF NOT EXISTS idx_consent_ledger_update_queue_status ON consent_ledger_update_queue(status);
CREATE INDEX IF NOT EXISTS idx_consent_ledger_update_queue_created ON consent_ledger_update_queue(created_at DESC);

COMMENT ON TABLE ce_import_jobs IS 'Tracking for HMIS/Vendor Coordinated Entry imports';
COMMENT ON TABLE ce_export_receipts IS 'Receipts for encrypted CE export packages with consent metadata';
COMMENT ON TABLE consent_ledger_update_queue IS 'Queue of consent ledger updates triggered by CE import/export actions';

-- V30: VSP Export Metadata with CE-specific anonymization and VAWA compliance
-- This migration creates comprehensive tracking for VSP exports with recipient categorization

-- Create VSP export metadata table
CREATE TABLE vsp_export_metadata (
    export_id UUID PRIMARY KEY,
    recipient VARCHAR(500) NOT NULL,
    recipient_category VARCHAR(50) NOT NULL,
    consent_basis VARCHAR(500) NOT NULL,
    packet_hash VARCHAR(128) NOT NULL,
    ce_hash_key VARCHAR(128) NOT NULL,
    export_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    expiry_date TIMESTAMP,
    share_scopes TEXT[],
    anonymization_rules JSONB NOT NULL,
    metadata JSONB,
    initiated_by VARCHAR(200) NOT NULL,
    status VARCHAR(30) NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    revoked_by VARCHAR(200),
    revocation_reason VARCHAR(1000),
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED', 'PENDING_APPROVAL')),
    CONSTRAINT chk_recipient_category CHECK (recipient_category IN (
        'VICTIM_SERVICE_PROVIDER', 'LEGAL_AID', 'LAW_ENFORCEMENT',
        'HEALTHCARE_PROVIDER', 'GOVERNMENT_AGENCY', 'RESEARCH_INSTITUTION',
        'COC_LEAD', 'HMIS_LEAD', 'EMERGENCY_SHELTER', 'TRANSITIONAL_HOUSING',
        'INTERNAL_USE', 'CLIENT_REQUEST', 'UNAUTHORIZED'
    ))
);

-- Create indexes for efficient querying
CREATE INDEX idx_vsp_export_recipient ON vsp_export_metadata(recipient);
CREATE INDEX idx_vsp_export_packet_hash ON vsp_export_metadata(packet_hash);
CREATE INDEX idx_vsp_export_ce_hash ON vsp_export_metadata(ce_hash_key);
CREATE INDEX idx_vsp_export_status ON vsp_export_metadata(status);
CREATE INDEX idx_vsp_export_expiry ON vsp_export_metadata(expiry_date) WHERE expiry_date IS NOT NULL;
CREATE INDEX idx_vsp_export_timestamp ON vsp_export_metadata(export_timestamp);
CREATE INDEX idx_vsp_export_recipient_category ON vsp_export_metadata(recipient_category);
CREATE INDEX idx_vsp_export_revoked ON vsp_export_metadata(revoked_at) WHERE revoked_at IS NOT NULL;

-- Create audit table for VSP export access
CREATE TABLE vsp_export_audit (
    audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    export_id UUID REFERENCES vsp_export_metadata(export_id),
    action VARCHAR(50) NOT NULL,
    performed_by VARCHAR(200) NOT NULL,
    performed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    details JSONB,

    CONSTRAINT chk_action CHECK (action IN (
        'EXPORT_CREATED', 'EXPORT_ACCESSED', 'EXPORT_REVOKED',
        'EXPORT_EXPIRED', 'EXPORT_DOWNLOADED', 'EXPORT_SHARED'
    ))
);

CREATE INDEX idx_vsp_export_audit_export ON vsp_export_audit(export_id);
CREATE INDEX idx_vsp_export_audit_performed_at ON vsp_export_audit(performed_at);
CREATE INDEX idx_vsp_export_audit_performed_by ON vsp_export_audit(performed_by);

-- Create recipient statistics view for dashboard
CREATE OR REPLACE VIEW vsp_recipient_statistics AS
SELECT
    recipient,
    recipient_category,
    COUNT(*) as total_exports,
    COUNT(*) FILTER (WHERE status = 'ACTIVE') as active_exports,
    COUNT(*) FILTER (WHERE status = 'REVOKED') as revoked_exports,
    COUNT(*) FILTER (WHERE status = 'EXPIRED') as expired_exports,
    MIN(export_timestamp) as first_export_date,
    MAX(export_timestamp) as last_export_date,
    AVG(CASE
        WHEN expiry_date IS NOT NULL
        THEN EXTRACT(EPOCH FROM (expiry_date - export_timestamp)) / 86400
        ELSE NULL
    END) as avg_expiry_days
FROM vsp_export_metadata
GROUP BY recipient, recipient_category;

-- Create function to automatically expire old exports
CREATE OR REPLACE FUNCTION expire_vsp_exports()
RETURNS INTEGER AS $$
DECLARE
    expired_count INTEGER;
BEGIN
    UPDATE vsp_export_metadata
    SET status = 'EXPIRED',
        updated_at = CURRENT_TIMESTAMP
    WHERE status = 'ACTIVE'
    AND expiry_date IS NOT NULL
    AND expiry_date <= CURRENT_TIMESTAMP;

    GET DIAGNOSTICS expired_count = ROW_COUNT;

    -- Log expiration actions
    INSERT INTO vsp_export_audit (export_id, action, performed_by, details)
    SELECT export_id, 'EXPORT_EXPIRED', 'system',
           jsonb_build_object('auto_expired', true, 'expiry_date', expiry_date)
    FROM vsp_export_metadata
    WHERE status = 'EXPIRED'
    AND updated_at >= CURRENT_TIMESTAMP - INTERVAL '1 minute';

    RETURN expired_count;
END;
$$ LANGUAGE plpgsql;

-- Create function to validate VAWA compliance
CREATE OR REPLACE FUNCTION validate_vawa_compliance(
    p_recipient_category VARCHAR(50),
    p_share_scopes TEXT[]
) RETURNS BOOLEAN AS $$
BEGIN
    -- Check if recipient is authorized for victim data
    IF p_recipient_category NOT IN (
        'VICTIM_SERVICE_PROVIDER', 'LEGAL_AID', 'HEALTHCARE_PROVIDER',
        'COC_LEAD', 'HMIS_LEAD', 'EMERGENCY_SHELTER', 'TRANSITIONAL_HOUSING',
        'INTERNAL_USE', 'CLIENT_REQUEST'
    ) THEN
        RETURN FALSE;
    END IF;

    -- Check for DV data sharing restrictions
    IF 'DV_DATA' = ANY(p_share_scopes) AND p_recipient_category NOT IN (
        'VICTIM_SERVICE_PROVIDER', 'EMERGENCY_SHELTER', 'INTERNAL_USE', 'CLIENT_REQUEST'
    ) THEN
        RETURN FALSE;
    END IF;

    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to validate VAWA compliance on insert/update
CREATE OR REPLACE FUNCTION validate_vsp_export_trigger()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT validate_vawa_compliance(NEW.recipient_category, NEW.share_scopes) THEN
        RAISE EXCEPTION 'VAWA compliance validation failed for recipient category % with share scopes %',
            NEW.recipient_category, NEW.share_scopes;
    END IF;

    -- Update timestamp on modification
    IF TG_OP = 'UPDATE' THEN
        NEW.updated_at = CURRENT_TIMESTAMP;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER vsp_export_vawa_validation
    BEFORE INSERT OR UPDATE ON vsp_export_metadata
    FOR EACH ROW
    EXECUTE FUNCTION validate_vsp_export_trigger();

-- Create function to get anonymization level based on recipient category
CREATE OR REPLACE FUNCTION get_anonymization_level(p_recipient_category VARCHAR(50))
RETURNS VARCHAR(20) AS $$
BEGIN
    RETURN CASE p_recipient_category
        WHEN 'VICTIM_SERVICE_PROVIDER' THEN 'MINIMAL'
        WHEN 'EMERGENCY_SHELTER' THEN 'MINIMAL'
        WHEN 'INTERNAL_USE' THEN 'MINIMAL'
        WHEN 'CLIENT_REQUEST' THEN 'MINIMAL'
        WHEN 'LEGAL_AID' THEN 'STANDARD'
        WHEN 'HEALTHCARE_PROVIDER' THEN 'STANDARD'
        WHEN 'COC_LEAD' THEN 'STANDARD'
        WHEN 'HMIS_LEAD' THEN 'STANDARD'
        WHEN 'TRANSITIONAL_HOUSING' THEN 'STANDARD'
        ELSE 'FULL'
    END;
END;
$$ LANGUAGE plpgsql;

-- Create materialized view for export analytics
CREATE MATERIALIZED VIEW vsp_export_analytics AS
SELECT
    DATE_TRUNC('month', export_timestamp) as month,
    recipient_category,
    get_anonymization_level(recipient_category) as anonymization_level,
    COUNT(*) as export_count,
    COUNT(DISTINCT recipient) as unique_recipients,
    COUNT(DISTINCT ce_hash_key) as unique_households,
    COUNT(*) FILTER (WHERE status = 'REVOKED') as revoked_count,
    AVG(CASE
        WHEN revoked_at IS NOT NULL
        THEN EXTRACT(EPOCH FROM (revoked_at - export_timestamp)) / 3600
        ELSE NULL
    END) as avg_hours_to_revocation
FROM vsp_export_metadata
GROUP BY DATE_TRUNC('month', export_timestamp), recipient_category;

CREATE INDEX idx_vsp_export_analytics_month ON vsp_export_analytics(month);
CREATE INDEX idx_vsp_export_analytics_category ON vsp_export_analytics(recipient_category);

-- Add comments for documentation
COMMENT ON TABLE vsp_export_metadata IS 'Tracks all VSP exports with CE-specific anonymization and VAWA compliance';
COMMENT ON COLUMN vsp_export_metadata.ce_hash_key IS 'CE-specific hash key replacing household IDs for anonymization';
COMMENT ON COLUMN vsp_export_metadata.recipient_category IS 'VAWA-compliant categorization of the data recipient';
COMMENT ON COLUMN vsp_export_metadata.anonymization_rules IS 'JSON object defining CE-specific anonymization rules applied';
COMMENT ON COLUMN vsp_export_metadata.share_scopes IS 'Array of CE share scopes authorized for this export';

-- Grant appropriate permissions
GRANT SELECT ON vsp_export_metadata TO haven_readonly;
GRANT SELECT, INSERT, UPDATE ON vsp_export_metadata TO haven_app;
GRANT SELECT, INSERT ON vsp_export_audit TO haven_app;
GRANT SELECT ON vsp_recipient_statistics TO haven_app;
GRANT SELECT ON vsp_export_analytics TO haven_readonly;
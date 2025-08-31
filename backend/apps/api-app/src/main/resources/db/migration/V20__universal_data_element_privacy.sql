-- ============================================================================
-- V20: Universal Data Element Privacy Controls
-- ============================================================================
-- Implements privacy controls for Race and Ethnicity data with aliasing,
-- masking, and precision controls per HMIS 2024 standards
-- ============================================================================

SET search_path TO haven, public;

-- ============================================================================
-- Create Required Roles and Views
-- ============================================================================

-- Ensure authenticated role exists for RLS policies
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        CREATE ROLE authenticated NOLOGIN;
    END IF;
END $$;

-- Grant authenticated role to current user
GRANT authenticated TO CURRENT_USER;

-- Create user_roles view if it doesn't exist (referenced by RLS policies)
-- This assumes users table has id and role columns - adjust as needed
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_roles') THEN
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users') THEN
            CREATE VIEW user_roles AS 
            SELECT id AS user_id, 'USER' AS role FROM users
            WHERE EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'id');
        END IF;
    END IF;
END $$;

-- ============================================================================
-- Create Privacy Control Enums
-- ============================================================================

-- Race Redaction Strategy
DO $$ BEGIN
    CREATE TYPE race_redaction_strategy AS ENUM (
        'FULL_DISCLOSURE',    -- No redaction - full race information
        'GENERALIZED',        -- Broader categories (e.g., "Multiple races")
        'CATEGORY_ONLY',      -- Only whether race is known/unknown
        'MASKED',             -- Partial information (e.g., only primary race)
        'ALIASED',            -- Consistent but false information
        'HIDDEN'              -- Complete redaction
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Ethnicity Precision Level
DO $$ BEGIN
    CREATE TYPE ethnicity_precision AS ENUM (
        'FULL',           -- Complete ethnicity information
        'CATEGORY_ONLY',  -- Only whether ethnicity is known
        'REDACTED',       -- Shows field exists but value is redacted
        'HIDDEN'          -- Field is completely hidden
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Data Access Purpose
DO $$ BEGIN
    CREATE TYPE data_access_purpose AS ENUM (
        'DIRECT_SERVICE',     -- Providing direct services to client
        'CASE_MANAGEMENT',    -- Case management activities
        'REPORTING',          -- Aggregate reporting
        'RESEARCH',           -- Research and analysis
        'COURT_ORDERED',      -- Court-ordered disclosure
        'AUDIT',              -- Compliance audit
        'EMERGENCY',          -- Emergency access
        'VSP_SHARING',        -- Victim Service Provider data sharing
        'HMIS_EXPORT'         -- HMIS data export
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- ============================================================================
-- Update client_demographics table with HMIS-compliant fields
-- ============================================================================

-- Add HMIS Race and Ethnicity columns
ALTER TABLE client_demographics 
    ADD COLUMN IF NOT EXISTS hmis_race TEXT[] DEFAULT '{}',
    ADD COLUMN IF NOT EXISTS hmis_ethnicity VARCHAR(50),
    ADD COLUMN IF NOT EXISTS race_data_quality INTEGER,
    ADD COLUMN IF NOT EXISTS ethnicity_data_quality INTEGER;

-- Add privacy control columns
ALTER TABLE client_demographics
    ADD COLUMN IF NOT EXISTS default_race_strategy race_redaction_strategy DEFAULT 'FULL_DISCLOSURE',
    ADD COLUMN IF NOT EXISTS default_ethnicity_precision ethnicity_precision DEFAULT 'FULL',
    ADD COLUMN IF NOT EXISTS privacy_notes TEXT;

-- Create indexes for demographic fields
CREATE INDEX IF NOT EXISTS idx_client_demographics_hmis_race 
    ON client_demographics USING GIN (hmis_race);
    
CREATE INDEX IF NOT EXISTS idx_client_demographics_hmis_ethnicity 
    ON client_demographics (hmis_ethnicity);

-- ============================================================================
-- Privacy Control Override Table
-- ============================================================================

CREATE TABLE IF NOT EXISTS demographic_privacy_overrides (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    
    -- Override settings per purpose
    purpose data_access_purpose NOT NULL,
    race_strategy race_redaction_strategy,
    ethnicity_precision ethnicity_precision,
    
    -- Override metadata
    override_reason TEXT,
    expires_at TIMESTAMP WITH TIME ZONE,
    authorized_by UUID REFERENCES users(id),
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    
    -- Unique constraint per client and purpose
    CONSTRAINT unique_client_purpose UNIQUE (client_id, purpose)
);

-- Create index for lookup
CREATE INDEX idx_privacy_overrides_client_purpose 
    ON demographic_privacy_overrides (client_id, purpose);

-- ============================================================================
-- Aliasing Cache Table (for consistent aliases)
-- ============================================================================

CREATE TABLE IF NOT EXISTS demographic_alias_cache (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    
    -- Aliased values (stored encrypted in production)
    aliased_race TEXT[],
    aliased_ethnicity VARCHAR(50),
    alias_seed INTEGER NOT NULL, -- For consistent generation
    
    -- Cache management
    valid_from TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    valid_until TIMESTAMP WITH TIME ZONE,
    
    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Unique per client
    CONSTRAINT unique_client_alias UNIQUE (client_id)
);

-- ============================================================================
-- Access Log Table for Demographic Data
-- ============================================================================

CREATE TABLE IF NOT EXISTS demographic_access_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    
    -- Access details
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    accessed_by UUID NOT NULL REFERENCES users(id),
    access_purpose data_access_purpose NOT NULL,
    
    -- What was accessed and how it was redacted
    race_accessed BOOLEAN DEFAULT FALSE,
    race_strategy_applied race_redaction_strategy,
    ethnicity_accessed BOOLEAN DEFAULT FALSE,
    ethnicity_precision_applied ethnicity_precision,
    
    -- Context
    access_context JSONB, -- Additional context (e.g., report ID, export ID)
    ip_address INET,
    user_agent TEXT,
    
    -- Timestamp
    accessed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for audit queries
CREATE INDEX idx_demographic_access_log_client 
    ON demographic_access_log (client_id, accessed_at DESC);
    
CREATE INDEX idx_demographic_access_log_user 
    ON demographic_access_log (accessed_by, accessed_at DESC);
    
CREATE INDEX idx_demographic_access_log_purpose 
    ON demographic_access_log (access_purpose, accessed_at DESC);

-- ============================================================================
-- Privacy Control Configuration Table (Organization-wide settings)
-- ============================================================================

CREATE TABLE IF NOT EXISTS privacy_control_config (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL,
    
    -- Default strategies by purpose
    purpose data_access_purpose NOT NULL,
    default_race_strategy race_redaction_strategy NOT NULL,
    default_ethnicity_precision ethnicity_precision NOT NULL,
    
    -- Configuration flags
    require_authorization_for_full_disclosure BOOLEAN DEFAULT TRUE,
    enable_aliasing BOOLEAN DEFAULT TRUE,
    audit_all_access BOOLEAN DEFAULT TRUE,
    
    -- Metadata
    effective_date DATE NOT NULL DEFAULT CURRENT_DATE,
    expires_date DATE,
    
    -- Audit
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    
    -- Unique per organization and purpose
    CONSTRAINT unique_org_purpose_config UNIQUE (organization_id, purpose)
);

-- ============================================================================
-- Helper Functions
-- ============================================================================

-- Function to get effective privacy controls for a client
CREATE OR REPLACE FUNCTION get_effective_privacy_controls(
    p_client_id UUID,
    p_purpose data_access_purpose,
    p_user_id UUID
) RETURNS TABLE (
    race_strategy race_redaction_strategy,
    ethnicity_precision ethnicity_precision,
    source TEXT
) AS $$
BEGIN
    -- First check for client-specific overrides
    RETURN QUERY
    SELECT 
        dpo.race_strategy,
        dpo.ethnicity_precision,
        'override'::TEXT as source
    FROM demographic_privacy_overrides dpo
    WHERE dpo.client_id = p_client_id 
        AND dpo.purpose = p_purpose
        AND (dpo.expires_at IS NULL OR dpo.expires_at > CURRENT_TIMESTAMP)
    LIMIT 1;
    
    -- If no override, check organization config
    IF NOT FOUND THEN
        RETURN QUERY
        SELECT 
            pcc.default_race_strategy,
            pcc.default_ethnicity_precision,
            'organization'::TEXT as source
        FROM privacy_control_config pcc
        JOIN clients c ON c.id = p_client_id
        WHERE pcc.purpose = p_purpose
            AND (pcc.expires_date IS NULL OR pcc.expires_date > CURRENT_DATE)
        LIMIT 1;
    END IF;
    
    -- If still not found, return defaults from client_demographics
    IF NOT FOUND THEN
        RETURN QUERY
        SELECT 
            cd.default_race_strategy,
            cd.default_ethnicity_precision,
            'default'::TEXT as source
        FROM client_demographics cd
        WHERE cd.client_id = p_client_id
        LIMIT 1;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Function to log demographic data access
CREATE OR REPLACE FUNCTION log_demographic_access(
    p_client_id UUID,
    p_user_id UUID,
    p_purpose data_access_purpose,
    p_race_accessed BOOLEAN,
    p_race_strategy race_redaction_strategy,
    p_ethnicity_accessed BOOLEAN,
    p_ethnicity_precision ethnicity_precision,
    p_context JSONB DEFAULT NULL
) RETURNS UUID AS $$
DECLARE
    v_log_id UUID;
BEGIN
    INSERT INTO demographic_access_log (
        client_id,
        accessed_by,
        access_purpose,
        race_accessed,
        race_strategy_applied,
        ethnicity_accessed,
        ethnicity_precision_applied,
        access_context
    ) VALUES (
        p_client_id,
        p_user_id,
        p_purpose,
        p_race_accessed,
        p_race_strategy,
        p_ethnicity_accessed,
        p_ethnicity_precision,
        p_context
    ) RETURNING id INTO v_log_id;
    
    RETURN v_log_id;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- Default Privacy Control Settings
-- ============================================================================

-- Insert default organization-wide privacy settings for each purpose
INSERT INTO privacy_control_config (
    organization_id,
    purpose,
    default_race_strategy,
    default_ethnicity_precision,
    require_authorization_for_full_disclosure,
    enable_aliasing,
    audit_all_access,
    created_by
) VALUES 
    ('00000000-0000-0000-0000-000000000000'::UUID, 'DIRECT_SERVICE', 'FULL_DISCLOSURE', 'FULL', FALSE, FALSE, TRUE, NULL),
    ('00000000-0000-0000-0000-000000000000'::UUID, 'CASE_MANAGEMENT', 'FULL_DISCLOSURE', 'FULL', FALSE, FALSE, TRUE, NULL),
    ('00000000-0000-0000-0000-000000000000'::UUID, 'REPORTING', 'GENERALIZED', 'CATEGORY_ONLY', TRUE, FALSE, TRUE, NULL),
    ('00000000-0000-0000-0000-000000000000'::UUID, 'RESEARCH', 'ALIASED', 'REDACTED', TRUE, TRUE, TRUE, NULL),
    ('00000000-0000-0000-0000-000000000000'::UUID, 'COURT_ORDERED', 'CATEGORY_ONLY', 'CATEGORY_ONLY', TRUE, FALSE, TRUE, NULL),
    ('00000000-0000-0000-0000-000000000000'::UUID, 'AUDIT', 'CATEGORY_ONLY', 'REDACTED', TRUE, FALSE, TRUE, NULL),
    ('00000000-0000-0000-0000-000000000000'::UUID, 'EMERGENCY', 'MASKED', 'CATEGORY_ONLY', FALSE, FALSE, TRUE, NULL),
    ('00000000-0000-0000-0000-000000000000'::UUID, 'VSP_SHARING', 'ALIASED', 'REDACTED', TRUE, TRUE, TRUE, NULL),
    ('00000000-0000-0000-0000-000000000000'::UUID, 'HMIS_EXPORT', 'GENERALIZED', 'CATEGORY_ONLY', FALSE, FALSE, TRUE, NULL)
ON CONFLICT (organization_id, purpose) DO NOTHING;

-- ============================================================================
-- Row Level Security Policies
-- ============================================================================

-- Enable RLS on privacy tables
ALTER TABLE demographic_privacy_overrides ENABLE ROW LEVEL SECURITY;
ALTER TABLE demographic_alias_cache ENABLE ROW LEVEL SECURITY;
ALTER TABLE demographic_access_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE privacy_control_config ENABLE ROW LEVEL SECURITY;

-- Privacy override policies (only admins can manage)
CREATE POLICY demographic_privacy_overrides_admin ON demographic_privacy_overrides
    FOR ALL
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM user_roles ur
            WHERE ur.user_id = current_setting('app.current_user_id')::UUID
                AND ur.role IN ('ADMIN', 'PRIVACY_OFFICER')
        )
    );

-- Alias cache is system-managed only
CREATE POLICY demographic_alias_cache_system ON demographic_alias_cache
    FOR ALL
    TO authenticated
    USING (FALSE)
    WITH CHECK (FALSE);

-- Access log readable by admins and privacy officers
CREATE POLICY demographic_access_log_read ON demographic_access_log
    FOR SELECT
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM user_roles ur
            WHERE ur.user_id = current_setting('app.current_user_id')::UUID
                AND ur.role IN ('ADMIN', 'PRIVACY_OFFICER', 'AUDITOR')
        )
    );

-- Privacy config managed by admins
CREATE POLICY privacy_control_config_admin ON privacy_control_config
    FOR ALL
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM user_roles ur
            WHERE ur.user_id = current_setting('app.current_user_id')::UUID
                AND ur.role = 'ADMIN'
        )
    );

-- ============================================================================
-- Comments for Documentation
-- ============================================================================

COMMENT ON TABLE demographic_privacy_overrides IS 
    'Client-specific privacy control overrides for demographic data access';
    
COMMENT ON TABLE demographic_alias_cache IS 
    'Cache for consistent demographic data aliases used in research/VSP sharing';
    
COMMENT ON TABLE demographic_access_log IS 
    'Audit log for all demographic data access with applied privacy controls';
    
COMMENT ON TABLE privacy_control_config IS 
    'Organization-wide default privacy control settings by access purpose';

COMMENT ON FUNCTION get_effective_privacy_controls IS 
    'Returns the effective privacy controls for a client based on overrides and defaults';
    
COMMENT ON FUNCTION log_demographic_access IS 
    'Logs demographic data access for audit purposes';
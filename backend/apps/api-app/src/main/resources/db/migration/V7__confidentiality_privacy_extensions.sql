-- ============================================================================
-- V7: Confidentiality & Privacy Extensions for DV Survivor Safety
-- Implements California Evidence Code §1037.x DV counselor-victim privilege
-- VAWA confidentiality requirements and Safe at Home address protections
-- ============================================================================

-- Set default schema
SET search_path TO haven, public;

-- ============================================================================
-- New Enums for Confidentiality & Privacy
-- ============================================================================

-- Data system type for HMIS vs Comparable Database segregation
DO $$ BEGIN
    CREATE TYPE data_system AS ENUM (
        'COMPARABLE_DB',
        'HMIS'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Safe at Home program status
DO $$ BEGIN
    CREATE TYPE safe_at_home_status AS ENUM (
        'NOT_ENROLLED',
        'ENROLLED', 
        'PENDING',
        'EXPIRED'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Consent types for VAWA compliance
DO $$ BEGIN
    CREATE TYPE consent_type AS ENUM (
        'INFORMATION_SHARING',
        'DISCLOSURE_TO_PARTNER_AGENCY',
        'COURT_TESTIMONY',
        'HMIS_PARTICIPATION',
        'COMPARABLE_DB_PARTICIPATION',
        'PHOTO_RELEASE',
        'MEDIA_PARTICIPATION'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- ============================================================================
-- Extend clients table with confidentiality fields
-- ============================================================================

-- Add confidentiality and privacy fields to clients table
ALTER TABLE clients 
ADD COLUMN IF NOT EXISTS alias_name VARCHAR(200),
ADD COLUMN IF NOT EXISTS data_system data_system DEFAULT 'COMPARABLE_DB',
ADD COLUMN IF NOT EXISTS hmis_client_key VARCHAR(100),
ADD COLUMN IF NOT EXISTS safe_at_home_participant BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS safe_at_home_status safe_at_home_status DEFAULT 'NOT_ENROLLED',
ADD COLUMN IF NOT EXISTS is_confidential_location BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS substitute_address_line1 VARCHAR(255),
ADD COLUMN IF NOT EXISTS substitute_address_line2 VARCHAR(255),
ADD COLUMN IF NOT EXISTS substitute_city VARCHAR(100),
ADD COLUMN IF NOT EXISTS substitute_state VARCHAR(50),
ADD COLUMN IF NOT EXISTS substitute_postal_code VARCHAR(20),
ADD COLUMN IF NOT EXISTS substitute_country VARCHAR(100) DEFAULT 'USA';

-- Add contact safety preferences
ALTER TABLE clients
ADD COLUMN IF NOT EXISTS ok_to_text BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS ok_to_voicemail BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS contact_code_word VARCHAR(100),
ADD COLUMN IF NOT EXISTS quiet_hours_start TIME,
ADD COLUMN IF NOT EXISTS quiet_hours_end TIME;

-- ============================================================================
-- Consent Management Tables
-- ============================================================================

-- Consent records with time-limited and revocable nature per VAWA
CREATE TABLE IF NOT EXISTS client_consents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    consent_type consent_type NOT NULL,
    purpose TEXT NOT NULL,
    granted_date DATE NOT NULL DEFAULT CURRENT_DATE,
    expiration_date DATE,
    is_active BOOLEAN DEFAULT true,
    granted_by VARCHAR(255) NOT NULL, -- Staff member who obtained consent
    granted_by_user_id UUID REFERENCES users(id),
    revoked_date DATE,
    revoked_by VARCHAR(255),
    revoked_by_user_id UUID REFERENCES users(id),
    revocation_reason TEXT,
    consent_document_id UUID REFERENCES documents(id),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_consent_dates CHECK (
        expiration_date IS NULL OR expiration_date >= granted_date
    ),
    CONSTRAINT chk_revocation_dates CHECK (
        revoked_date IS NULL OR revoked_date >= granted_date
    )
);

-- ============================================================================
-- Role-based Note Protection
-- ============================================================================

-- Add confidentiality levels to case notes for DV counselor privilege
ALTER TABLE case_notes 
ADD COLUMN IF NOT EXISTS confidentiality_level VARCHAR(50) DEFAULT 'STANDARD',
ADD COLUMN IF NOT EXISTS requires_dv_privilege BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS accessible_roles TEXT[] DEFAULT ARRAY['ADMIN', 'SUPERVISOR', 'CASE_MANAGER'];

-- Create confidentiality levels enum
DO $$ BEGIN
    CREATE TYPE confidentiality_level AS ENUM (
        'PUBLIC',
        'STANDARD', 
        'CONFIDENTIAL',
        'DV_PRIVILEGED',
        'LEGAL_PROTECTED'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Remove the default before type conversion
ALTER TABLE case_notes 
ALTER COLUMN confidentiality_level DROP DEFAULT;

-- Update the column to use the enum
ALTER TABLE case_notes 
ALTER COLUMN confidentiality_level TYPE confidentiality_level 
USING confidentiality_level::confidentiality_level;

-- Set the default value after type conversion
ALTER TABLE case_notes 
ALTER COLUMN confidentiality_level SET DEFAULT 'STANDARD'::confidentiality_level;

-- ============================================================================
-- Indexes for Performance and Security
-- ============================================================================

-- Consent management indexes
CREATE INDEX IF NOT EXISTS idx_client_consents_client_id ON client_consents(client_id);
CREATE INDEX IF NOT EXISTS idx_client_consents_type ON client_consents(consent_type);
CREATE INDEX IF NOT EXISTS idx_client_consents_active ON client_consents(is_active) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_client_consents_expiration ON client_consents(expiration_date) WHERE expiration_date IS NOT NULL;

-- Privacy and safety indexes
CREATE INDEX IF NOT EXISTS idx_clients_data_system ON clients(data_system);
CREATE INDEX IF NOT EXISTS idx_clients_safe_at_home ON clients(safe_at_home_participant) WHERE safe_at_home_participant = true;
CREATE INDEX IF NOT EXISTS idx_clients_confidential_location ON clients(is_confidential_location) WHERE is_confidential_location = true;

-- Confidential notes indexes
CREATE INDEX IF NOT EXISTS idx_case_notes_confidentiality ON case_notes(confidentiality_level);
CREATE INDEX IF NOT EXISTS idx_case_notes_dv_privilege ON case_notes(requires_dv_privilege) WHERE requires_dv_privilege = true;

-- ============================================================================
-- Security Functions
-- ============================================================================

-- Function to check if current user can access confidential notes
CREATE OR REPLACE FUNCTION can_access_confidential_note(
    note_confidentiality confidentiality_level,
    note_accessible_roles TEXT[],
    user_role user_role
)
RETURNS BOOLEAN AS $$
BEGIN
    -- Admin can access everything
    IF user_role = 'ADMIN' THEN
        RETURN TRUE;
    END IF;
    
    -- Check if user role is in accessible roles
    IF user_role::TEXT = ANY(note_accessible_roles) THEN
        RETURN TRUE;
    END IF;
    
    -- DV privileged notes require special access
    IF note_confidentiality = 'DV_PRIVILEGED' THEN
        RETURN user_role IN ('ADMIN', 'SUPERVISOR');
    END IF;
    
    RETURN FALSE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to get safe address for client (returns substitute if confidential)
CREATE OR REPLACE FUNCTION get_safe_client_address(client_uuid UUID)
RETURNS TABLE (
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255), 
    city VARCHAR(100),
    state VARCHAR(50),
    postal_code VARCHAR(20),
    country VARCHAR(100)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        CASE 
            WHEN c.is_confidential_location AND c.substitute_address_line1 IS NOT NULL 
            THEN c.substitute_address_line1
            ELSE c.address_line1
        END,
        CASE 
            WHEN c.is_confidential_location AND c.substitute_address_line1 IS NOT NULL 
            THEN c.substitute_address_line2
            ELSE c.address_line2
        END,
        CASE 
            WHEN c.is_confidential_location AND c.substitute_address_line1 IS NOT NULL 
            THEN c.substitute_city
            ELSE c.city
        END,
        CASE 
            WHEN c.is_confidential_location AND c.substitute_address_line1 IS NOT NULL 
            THEN c.substitute_state
            ELSE c.state
        END,
        CASE 
            WHEN c.is_confidential_location AND c.substitute_address_line1 IS NOT NULL 
            THEN c.substitute_postal_code
            ELSE c.postal_code
        END,
        CASE 
            WHEN c.is_confidential_location AND c.substitute_address_line1 IS NOT NULL 
            THEN c.substitute_country
            ELSE c.country
        END
    FROM clients c
    WHERE c.id = client_uuid;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================================================
-- Triggers for Consent Management
-- ============================================================================

-- Trigger to automatically revoke expired consents
CREATE OR REPLACE FUNCTION check_consent_expiration()
RETURNS TRIGGER AS $$
BEGIN
    -- Auto-revoke expired consents
    UPDATE client_consents 
    SET 
        is_active = false,
        revoked_date = CURRENT_DATE,
        revocation_reason = 'Automatic expiration'
    WHERE 
        expiration_date < CURRENT_DATE 
        AND is_active = true
        AND revoked_date IS NULL;
    
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Daily trigger to check consent expiration
-- Note: In production, this should be handled by a scheduled job
CREATE OR REPLACE FUNCTION daily_consent_check()
RETURNS void AS $$
BEGIN
    UPDATE client_consents 
    SET 
        is_active = false,
        revoked_date = CURRENT_DATE,
        revocation_reason = 'Automatic expiration'
    WHERE 
        expiration_date < CURRENT_DATE 
        AND is_active = true
        AND revoked_date IS NULL;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- Updated Triggers for Timestamp Management
-- ============================================================================

-- Apply updated_at trigger to new tables
DROP TRIGGER IF EXISTS update_client_consents_updated_at ON client_consents;
CREATE TRIGGER update_client_consents_updated_at BEFORE UPDATE ON client_consents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- Comments for Documentation
-- ============================================================================

COMMENT ON TABLE client_consents IS 'VAWA-compliant consent management with time-limited and revocable permissions';
COMMENT ON COLUMN clients.alias_name IS 'Safe communication name to protect client identity';
COMMENT ON COLUMN clients.data_system IS 'HMIS vs Comparable Database designation per HUD requirements';
COMMENT ON COLUMN clients.safe_at_home_participant IS 'California Safe at Home address confidentiality program participation';
COMMENT ON COLUMN clients.is_confidential_location IS 'True location is confidential, use substitute address for mailings';
COMMENT ON COLUMN case_notes.confidentiality_level IS 'Protection level including DV counselor-victim privilege per CA Evidence Code §1037.x';
COMMENT ON COLUMN case_notes.requires_dv_privilege IS 'Note protected by DV counselor-victim privilege';

-- ============================================================================
-- Default Constraint Updates
-- ============================================================================

-- Ensure DV clients default to Comparable DB unless explicitly enrolled in HMIS
ALTER TABLE clients 
ADD CONSTRAINT chk_dv_data_system 
CHECK (
    data_system = 'COMPARABLE_DB' OR 
    (data_system = 'HMIS' AND hmis_client_key IS NOT NULL)
);

-- Ensure confidential locations have substitute addresses
ALTER TABLE clients
ADD CONSTRAINT chk_confidential_address
CHECK (
    NOT is_confidential_location OR 
    (substitute_address_line1 IS NOT NULL AND substitute_city IS NOT NULL)
);
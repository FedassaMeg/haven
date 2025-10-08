-- ============================================================================
-- V9: PII Separation Strategy Implementation
-- ============================================================================
-- This migration implements the "Separate PII from service data" gotcha
-- Different tables/streams to simplify redaction and "need-to-know" access

-- Set default schema
SET search_path TO haven, public;

-- ============================================================================
-- Create PII Classification Types
-- ============================================================================

-- PII Access Levels
DO $$ BEGIN
    CREATE TYPE pii_access_level AS ENUM (
        'PUBLIC',           -- No PII restrictions
        'INTERNAL',         -- Organization staff only
        'RESTRICTED',       -- Case team + supervisors only
        'CONFIDENTIAL',     -- Designated staff only
        'HIGHLY_CONFIDENTIAL' -- Minimal access, specific authorization
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- PII Data Categories
DO $$ BEGIN
    CREATE TYPE pii_category AS ENUM (
        'DIRECT_IDENTIFIER',    -- Name, SSN, Photo
        'QUASI_IDENTIFIER',     -- DOB, Address, Phone
        'SENSITIVE_ATTRIBUTE',  -- Medical, Financial, Legal
        'CONTACT_INFO',         -- Email, Phone, Address
        'HOUSEHOLD_INFO',       -- Family composition
        'SERVICE_DATA'          -- Non-PII service information
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- ============================================================================
-- Create PII-separated tables
-- ============================================================================

-- Client Identity (HIGH PII - Direct Identifiers)
CREATE TABLE IF NOT EXISTS client_identity (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_id UUID NOT NULL UNIQUE REFERENCES clients(id) ON DELETE CASCADE,
    
    -- Direct identifiers - HIGHLY CONFIDENTIAL
    legal_first_name VARCHAR(100),
    legal_middle_name VARCHAR(100),
    legal_last_name VARCHAR(100),
    ssn_encrypted VARCHAR(255),
    drivers_license_encrypted VARCHAR(255),
    
    -- Alias/Safety names - CONFIDENTIAL
    preferred_name VARCHAR(100),
    alias_name VARCHAR(100),
    use_alias_only BOOLEAN DEFAULT FALSE,
    
    -- Classification
    access_level pii_access_level NOT NULL DEFAULT 'HIGHLY_CONFIDENTIAL',
    redaction_required BOOLEAN DEFAULT TRUE,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id)
);

-- Client Demographics (MEDIUM PII - Quasi Identifiers)
CREATE TABLE IF NOT EXISTS client_demographics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_id UUID NOT NULL UNIQUE REFERENCES clients(id) ON DELETE CASCADE,
    
    -- Demographics - RESTRICTED
    date_of_birth DATE,
    date_of_birth_approximate BOOLEAN DEFAULT FALSE,
    gender gender,
    ethnicity VARCHAR(100),
    race VARCHAR(100),
    primary_language VARCHAR(50),
    
    -- Classification
    access_level pii_access_level NOT NULL DEFAULT 'RESTRICTED',
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id)
);

-- Client Contact Information (MEDIUM PII - Contact Data)
CREATE TABLE IF NOT EXISTS client_contact_info (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    
    -- Contact details - RESTRICTED
    contact_type VARCHAR(50) NOT NULL, -- 'phone', 'email', 'address'
    contact_value_encrypted TEXT,
    contact_use VARCHAR(50), -- 'home', 'work', 'mobile', 'emergency'
    is_primary BOOLEAN DEFAULT FALSE,
    is_safe_to_contact BOOLEAN DEFAULT TRUE,
    contact_instructions TEXT,
    
    -- Geographic info (for reporting without full address)
    city VARCHAR(100),
    state VARCHAR(50),
    zip_code_first3 VARCHAR(3), -- Partial zip for geographic analysis
    
    -- Confidentiality
    is_confidential_address BOOLEAN DEFAULT FALSE,
    confidentiality_reason VARCHAR(200),
    
    -- Classification
    access_level pii_access_level NOT NULL DEFAULT 'RESTRICTED',
    
    -- Period of validity
    valid_from TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    valid_to TIMESTAMP WITH TIME ZONE,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id)
);

-- Emergency Contacts (HIGH PII - Third Party Information)
CREATE TABLE IF NOT EXISTS client_emergency_contacts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    
    -- Emergency contact - CONFIDENTIAL
    contact_name_encrypted VARCHAR(255),
    contact_phone_encrypted VARCHAR(255),
    relationship VARCHAR(100),
    is_primary_emergency_contact BOOLEAN DEFAULT FALSE,
    can_contact_about_client BOOLEAN DEFAULT TRUE,
    
    -- Classification
    access_level pii_access_level NOT NULL DEFAULT 'CONFIDENTIAL',
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id)
);

-- Service Data (LOW/NO PII - Service Information)
-- Update existing clients table to become service-focused
-- This will be done by removing PII columns and keeping service data

-- ============================================================================
-- PII Access Control Tables
-- ============================================================================

-- User PII Access Permissions
CREATE TABLE IF NOT EXISTS user_pii_access (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    client_id UUID REFERENCES clients(id), -- NULL for global access
    pii_category pii_category NOT NULL,
    access_level pii_access_level NOT NULL,
    
    -- Access context
    granted_by UUID REFERENCES users(id),
    granted_reason TEXT,
    
    -- Time-based access
    granted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,
    
    -- Revocation
    is_revoked BOOLEAN DEFAULT FALSE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    revoked_by UUID REFERENCES users(id),
    revoked_reason TEXT,
    
    -- Unique constraint
    UNIQUE(user_id, client_id, pii_category)
);

-- Role-based PII Access Templates
CREATE TABLE IF NOT EXISTS role_pii_access_template (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    role_name VARCHAR(100) NOT NULL,
    pii_category pii_category NOT NULL,
    max_access_level pii_access_level NOT NULL,
    requires_justification BOOLEAN DEFAULT FALSE,
    auto_expires_days INTEGER,
    
    UNIQUE(role_name, pii_category)
);

-- PII Access Audit Log
CREATE TABLE IF NOT EXISTS pii_access_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    client_id UUID NOT NULL REFERENCES clients(id),
    pii_category pii_category NOT NULL,
    access_level pii_access_level NOT NULL,
    
    -- Access details
    accessed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    access_method VARCHAR(50), -- 'direct_query', 'report', 'export', 'api'
    ip_address INET,
    user_agent TEXT,
    
    -- Context
    business_justification TEXT,
    case_id UUID REFERENCES cases(id),
    session_id VARCHAR(255)
);

-- ============================================================================
-- Insert role-based access templates
-- ============================================================================

INSERT INTO role_pii_access_template (role_name, pii_category, max_access_level, requires_justification, auto_expires_days) VALUES
-- Case Managers - need broad access
('CASE_MANAGER', 'DIRECT_IDENTIFIER', 'CONFIDENTIAL', TRUE, 90),
('CASE_MANAGER', 'QUASI_IDENTIFIER', 'RESTRICTED', FALSE, 90),
('CASE_MANAGER', 'CONTACT_INFO', 'RESTRICTED', FALSE, 90),
('CASE_MANAGER', 'HOUSEHOLD_INFO', 'RESTRICTED', FALSE, 90),
('CASE_MANAGER', 'SERVICE_DATA', 'PUBLIC', FALSE, NULL),

-- Supervisors - elevated access
('SUPERVISOR', 'DIRECT_IDENTIFIER', 'HIGHLY_CONFIDENTIAL', TRUE, 180),
('SUPERVISOR', 'QUASI_IDENTIFIER', 'RESTRICTED', FALSE, 180),
('SUPERVISOR', 'CONTACT_INFO', 'RESTRICTED', FALSE, 180),
('SUPERVISOR', 'HOUSEHOLD_INFO', 'RESTRICTED', FALSE, 180),
('SUPERVISOR', 'SERVICE_DATA', 'PUBLIC', FALSE, NULL),

-- Front desk / Intake - limited access
('INTAKE_COORDINATOR', 'DIRECT_IDENTIFIER', 'RESTRICTED', TRUE, 30),
('INTAKE_COORDINATOR', 'QUASI_IDENTIFIER', 'RESTRICTED', FALSE, 30),
('INTAKE_COORDINATOR', 'CONTACT_INFO', 'RESTRICTED', FALSE, 30),
('INTAKE_COORDINATOR', 'SERVICE_DATA', 'INTERNAL', FALSE, NULL),

-- Data analysts - aggregate access only
('DATA_ANALYST', 'DIRECT_IDENTIFIER', 'PUBLIC', TRUE, 30),
('DATA_ANALYST', 'QUASI_IDENTIFIER', 'INTERNAL', TRUE, 30),
('DATA_ANALYST', 'SERVICE_DATA', 'PUBLIC', FALSE, NULL),

-- Administrators - emergency access
('ADMINISTRATOR', 'DIRECT_IDENTIFIER', 'HIGHLY_CONFIDENTIAL', TRUE, 30),
('ADMINISTRATOR', 'QUASI_IDENTIFIER', 'RESTRICTED', TRUE, 30),
('ADMINISTRATOR', 'CONTACT_INFO', 'RESTRICTED', TRUE, 30),
('ADMINISTRATOR', 'HOUSEHOLD_INFO', 'RESTRICTED', TRUE, 30),
('ADMINISTRATOR', 'SERVICE_DATA', 'PUBLIC', FALSE, NULL)

ON CONFLICT (role_name, pii_category) DO NOTHING;

-- ============================================================================
-- Indexes for performance and security
-- ============================================================================

-- Identity table indexes
CREATE INDEX IF NOT EXISTS idx_client_identity_client_id ON client_identity(client_id);
CREATE INDEX IF NOT EXISTS idx_client_identity_access_level ON client_identity(access_level);

-- Demographics table indexes
CREATE INDEX IF NOT EXISTS idx_client_demographics_client_id ON client_demographics(client_id);
CREATE INDEX IF NOT EXISTS idx_client_demographics_access_level ON client_demographics(access_level);

-- Contact info indexes
CREATE INDEX IF NOT EXISTS idx_client_contact_info_client_id ON client_contact_info(client_id);
CREATE INDEX IF NOT EXISTS idx_client_contact_info_type ON client_contact_info(contact_type);
CREATE INDEX IF NOT EXISTS idx_client_contact_info_access_level ON client_contact_info(access_level);
CREATE INDEX IF NOT EXISTS idx_client_contact_info_validity ON client_contact_info(valid_from, valid_to);

-- Emergency contacts indexes
CREATE INDEX IF NOT EXISTS idx_client_emergency_contacts_client_id ON client_emergency_contacts(client_id);
CREATE INDEX IF NOT EXISTS idx_client_emergency_contacts_access_level ON client_emergency_contacts(access_level);

-- Access control indexes
CREATE INDEX IF NOT EXISTS idx_user_pii_access_user_client ON user_pii_access(user_id, client_id);
CREATE INDEX IF NOT EXISTS idx_user_pii_access_category ON user_pii_access(pii_category);
CREATE INDEX IF NOT EXISTS idx_user_pii_access_expires ON user_pii_access(expires_at) WHERE expires_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_user_pii_access_active ON user_pii_access(user_id, client_id, pii_category) WHERE is_revoked = FALSE;

-- Audit log indexes
CREATE INDEX IF NOT EXISTS idx_pii_access_log_user_time ON pii_access_log(user_id, accessed_at);
CREATE INDEX IF NOT EXISTS idx_pii_access_log_client_time ON pii_access_log(client_id, accessed_at);
CREATE INDEX IF NOT EXISTS idx_pii_access_log_category ON pii_access_log(pii_category);
CREATE INDEX IF NOT EXISTS idx_pii_access_log_session ON pii_access_log(session_id);

-- ============================================================================
-- Triggers for updated_at timestamps
-- ============================================================================

-- Client identity triggers
DROP TRIGGER IF EXISTS update_client_identity_updated_at ON client_identity;
CREATE TRIGGER update_client_identity_updated_at BEFORE UPDATE ON client_identity
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Client demographics triggers  
DROP TRIGGER IF EXISTS update_client_demographics_updated_at ON client_demographics;
CREATE TRIGGER update_client_demographics_updated_at BEFORE UPDATE ON client_demographics
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Client contact info triggers
DROP TRIGGER IF EXISTS update_client_contact_info_updated_at ON client_contact_info;
CREATE TRIGGER update_client_contact_info_updated_at BEFORE UPDATE ON client_contact_info
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Emergency contacts triggers
DROP TRIGGER IF EXISTS update_client_emergency_contacts_updated_at ON client_emergency_contacts;
CREATE TRIGGER update_client_emergency_contacts_updated_at BEFORE UPDATE ON client_emergency_contacts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- Views for authorized access
-- ============================================================================

-- Authorized client view (respects PII access levels)
-- This view will be used by application layer with user context
CREATE OR REPLACE VIEW authorized_client_view AS
SELECT 
    c.id,
    c.client_number,
    
    -- Identity (conditionally visible based on access)
    CASE WHEN ci.access_level <= 'RESTRICTED' THEN ci.legal_first_name END as first_name,
    CASE WHEN ci.access_level <= 'RESTRICTED' THEN ci.legal_last_name END as last_name,
    CASE WHEN ci.access_level <= 'CONFIDENTIAL' THEN ci.preferred_name END as preferred_name,
    
    -- Demographics (conditionally visible)
    CASE WHEN cd.access_level <= 'RESTRICTED' THEN cd.date_of_birth END as date_of_birth,
    CASE WHEN cd.access_level <= 'RESTRICTED' THEN cd.gender END as gender,
    
    -- Service data (always visible to case team)
    c.status,
    c.intake_date,
    c.assigned_case_manager_id,
    c.organization_id,
    c.created_at
    
FROM clients c
LEFT JOIN client_identity ci ON c.id = ci.client_id
LEFT JOIN client_demographics cd ON c.id = cd.client_id;

-- ============================================================================
-- Comments for documentation
-- ============================================================================

COMMENT ON TABLE client_identity IS 'HIGH PII: Direct identifiers requiring highest protection';
COMMENT ON TABLE client_demographics IS 'MEDIUM PII: Quasi-identifiers with restricted access';
COMMENT ON TABLE client_contact_info IS 'MEDIUM PII: Contact information with need-to-know access';
COMMENT ON TABLE client_emergency_contacts IS 'HIGH PII: Third-party contact information';
COMMENT ON TABLE user_pii_access IS 'PII access control matrix for users and roles';
COMMENT ON TABLE pii_access_log IS 'Audit trail for all PII access attempts';

COMMENT ON TYPE pii_access_level IS 'Classification levels for PII data protection';
COMMENT ON TYPE pii_category IS 'Categories of personally identifiable information';
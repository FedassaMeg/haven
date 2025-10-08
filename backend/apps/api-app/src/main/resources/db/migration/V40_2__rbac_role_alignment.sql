-- ============================================================================
-- V40: RBAC Role Alignment with Keycloak
-- ============================================================================
-- Purpose: Create RBAC metadata tables for Keycloak role synchronization
-- Date: 2025-10-07
-- Related: KEYCLOAK_ROLE_INVENTORY.md, setup-keycloak-roles-aligned.ps1
-- Note: Enum extensions moved to V40a due to PostgreSQL transaction requirements
-- ============================================================================

SET search_path TO haven, public;

-- ============================================================================
-- 1. Create RBAC metadata tables
-- ============================================================================

-- Role definitions with display metadata
CREATE TABLE IF NOT EXISTS rbac_roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    keycloak_role_id VARCHAR(255) UNIQUE,           -- Keycloak UUID for sync
    role_name VARCHAR(100) UNIQUE NOT NULL,         -- Kebab-case name from Keycloak
    role_enum user_role UNIQUE NOT NULL,            -- Internal enum mapping
    display_name VARCHAR(150) NOT NULL,             -- Human-readable name
    description TEXT,                                -- Role description
    is_composite BOOLEAN DEFAULT false,             -- True if composite role
    is_active BOOLEAN DEFAULT true,                 -- Can be disabled without deletion
    requires_mfa BOOLEAN DEFAULT false,             -- MFA requirement flag
    session_timeout_minutes INTEGER DEFAULT 480,    -- Session duration
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Role hierarchy for composite roles
CREATE TABLE IF NOT EXISTS rbac_role_composites (
    parent_role_id UUID NOT NULL REFERENCES rbac_roles(id) ON DELETE CASCADE,
    child_role_id UUID NOT NULL REFERENCES rbac_roles(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (parent_role_id, child_role_id),
    CHECK (parent_role_id != child_role_id)
);

-- Permission definitions
CREATE TABLE IF NOT EXISTS rbac_permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    resource_type VARCHAR(100) NOT NULL,            -- e.g., CLIENT, CASE, EXPORT
    action VARCHAR(50) NOT NULL,                    -- e.g., CREATE, READ, UPDATE, DELETE
    scope VARCHAR(50) DEFAULT 'ALL',                -- ALL, OWN, TEAM, DEPARTMENT
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (resource_type, action, scope)
);

-- Role-Permission mappings
CREATE TABLE IF NOT EXISTS rbac_role_permissions (
    role_id UUID NOT NULL REFERENCES rbac_roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES rbac_permissions(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id)
);

-- Keycloak sync audit log
CREATE TABLE IF NOT EXISTS rbac_sync_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sync_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    sync_type VARCHAR(50) NOT NULL,                 -- FULL, INCREMENTAL, MANUAL
    roles_added INTEGER DEFAULT 0,
    roles_updated INTEGER DEFAULT 0,
    roles_removed INTEGER DEFAULT 0,
    drift_detected BOOLEAN DEFAULT false,
    drift_details JSONB,
    sync_status VARCHAR(20) NOT NULL,               -- SUCCESS, FAILED, PARTIAL
    error_message TEXT,
    performed_by UUID REFERENCES users(id)
);

-- ============================================================================
-- 2. Seed RBAC roles
-- ============================================================================

INSERT INTO rbac_roles (role_name, role_enum, display_name, description, is_composite, requires_mfa, session_timeout_minutes)
VALUES
    -- Existing roles
    ('admin', 'ADMIN', 'Administrator', 'System Administrator with full access to all functions and data', false, true, 240),
    ('supervisor', 'SUPERVISOR', 'Supervisor', 'Program Supervisor with team oversight, case review, and reporting capabilities', false, false, 480),
    ('case-manager', 'CASE_MANAGER', 'Case Manager', 'Primary case management staff with full client service capabilities', false, false, 480),
    ('intake-specialist', 'INTAKE_SPECIALIST', 'Intake Specialist', 'Client intake coordination and initial assessment', false, false, 480),
    ('report-viewer', 'REPORT_VIEWER', 'Report Viewer', 'Read-only access to reports and aggregated data', false, false, 480),
    ('external-partner', 'EXTERNAL_PARTNER', 'External Partner', 'Limited access for external collaborators and partners', false, false, 240),

    -- New roles
    ('ce-intake', 'CE_INTAKE', 'Community Engagement & Intake', 'Community outreach, intake coordination, and partnership management', false, false, 480),
    ('dv-advocate', 'DV_ADVOCATE', 'DV Advocate', 'Specialized domestic violence case support and crisis intervention', false, false, 480),
    ('compliance-auditor', 'COMPLIANCE_AUDITOR', 'Compliance Auditor', 'System audit, compliance review, and data quality oversight (read-only for cases)', false, true, 360),
    ('exec', 'EXEC', 'Executive', 'Executive leadership with strategic oversight (composite role)', true, true, 240)
ON CONFLICT (role_name) DO UPDATE SET
    display_name = EXCLUDED.display_name,
    description = EXCLUDED.description,
    is_composite = EXCLUDED.is_composite,
    requires_mfa = EXCLUDED.requires_mfa,
    session_timeout_minutes = EXCLUDED.session_timeout_minutes,
    updated_at = CURRENT_TIMESTAMP;

-- ============================================================================
-- 3. Define composite role hierarchy
-- ============================================================================

-- exec role composition: supervisor + compliance-auditor + report-viewer
INSERT INTO rbac_role_composites (parent_role_id, child_role_id)
SELECT
    (SELECT id FROM rbac_roles WHERE role_name = 'exec'),
    child.id
FROM rbac_roles child
WHERE child.role_name IN ('supervisor', 'compliance-auditor', 'report-viewer')
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 4. Seed baseline permissions
-- ============================================================================

-- Client resource permissions
INSERT INTO rbac_permissions (resource_type, action, scope, description)
VALUES
    ('CLIENT', 'CREATE', 'ALL', 'Create new client profiles'),
    ('CLIENT', 'READ', 'ALL', 'View all client profiles'),
    ('CLIENT', 'READ', 'OWN', 'View only assigned client profiles'),
    ('CLIENT', 'UPDATE', 'ALL', 'Update any client profile'),
    ('CLIENT', 'UPDATE', 'OWN', 'Update only assigned client profiles'),
    ('CLIENT', 'DELETE', 'ALL', 'Archive/delete client profiles'),
    ('CLIENT', 'EXPORT', 'ALL', 'Export client data for HMIS/reporting'),

    -- Case resource permissions
    ('CASE', 'CREATE', 'ALL', 'Create new cases'),
    ('CASE', 'READ', 'ALL', 'View all cases'),
    ('CASE', 'READ', 'OWN', 'View only assigned cases'),
    ('CASE', 'UPDATE', 'ALL', 'Update any case'),
    ('CASE', 'UPDATE', 'OWN', 'Update only assigned cases'),
    ('CASE', 'CLOSE', 'ALL', 'Close any case'),
    ('CASE', 'CLOSE', 'OWN', 'Close only assigned cases'),
    ('CASE', 'AUDIT', 'ALL', 'Audit case records for compliance'),

    -- Enrollment permissions
    ('ENROLLMENT', 'CREATE', 'ALL', 'Create program enrollments'),
    ('ENROLLMENT', 'READ', 'ALL', 'View all enrollments'),
    ('ENROLLMENT', 'UPDATE', 'ALL', 'Update enrollment status'),
    ('ENROLLMENT', 'TERMINATE', 'ALL', 'Terminate enrollments'),

    -- Report permissions
    ('REPORT', 'READ', 'ALL', 'View all reports'),
    ('REPORT', 'CREATE', 'ALL', 'Generate custom reports'),
    ('REPORT', 'EXPORT', 'ALL', 'Export report data'),

    -- System permissions
    ('SYSTEM', 'ADMIN', 'ALL', 'Full system administration'),
    ('SYSTEM', 'AUDIT', 'ALL', 'Audit system activity'),
    ('SYSTEM', 'CONFIG', 'ALL', 'Configure system settings')
ON CONFLICT (resource_type, action, scope) DO NOTHING;

-- ============================================================================
-- 5. Assign permissions to roles
-- ============================================================================

-- Admin: Full access
INSERT INTO rbac_role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM rbac_roles WHERE role_name = 'admin'),
    id
FROM rbac_permissions
ON CONFLICT DO NOTHING;

-- Supervisor: All case/client operations, reporting
INSERT INTO rbac_role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM rbac_roles WHERE role_name = 'supervisor'),
    id
FROM rbac_permissions
WHERE (resource_type = 'CLIENT' AND action IN ('READ', 'UPDATE', 'CREATE'))
   OR (resource_type = 'CASE' AND action IN ('READ', 'UPDATE', 'CLOSE', 'CREATE'))
   OR (resource_type = 'ENROLLMENT' AND action IN ('READ', 'UPDATE', 'CREATE', 'TERMINATE'))
   OR (resource_type = 'REPORT' AND action IN ('READ', 'CREATE', 'EXPORT'))
ON CONFLICT DO NOTHING;

-- Case Manager: Own cases + clients
INSERT INTO rbac_role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM rbac_roles WHERE role_name = 'case-manager'),
    id
FROM rbac_permissions
WHERE (resource_type = 'CLIENT' AND action IN ('READ', 'UPDATE', 'CREATE') AND scope IN ('OWN', 'ALL'))
   OR (resource_type = 'CASE' AND action IN ('READ', 'UPDATE', 'CREATE', 'CLOSE') AND scope = 'OWN')
   OR (resource_type = 'ENROLLMENT' AND action IN ('READ', 'CREATE', 'UPDATE'))
ON CONFLICT DO NOTHING;

-- CE Intake: Intake-focused permissions
INSERT INTO rbac_role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM rbac_roles WHERE role_name = 'ce-intake'),
    id
FROM rbac_permissions
WHERE (resource_type = 'CLIENT' AND action IN ('CREATE', 'READ', 'UPDATE') AND scope = 'OWN')
   OR (resource_type = 'ENROLLMENT' AND action IN ('CREATE', 'READ'))
ON CONFLICT DO NOTHING;

-- DV Advocate: Full client/case access for specialized support
INSERT INTO rbac_role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM rbac_roles WHERE role_name = 'dv-advocate'),
    id
FROM rbac_permissions
WHERE (resource_type = 'CLIENT' AND action IN ('CREATE', 'READ', 'UPDATE'))
   OR (resource_type = 'CASE' AND action IN ('CREATE', 'READ', 'UPDATE', 'CLOSE') AND scope = 'OWN')
ON CONFLICT DO NOTHING;

-- Compliance Auditor: Read-only audit access
INSERT INTO rbac_role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM rbac_roles WHERE role_name = 'compliance-auditor'),
    id
FROM rbac_permissions
WHERE (resource_type = 'CASE' AND action = 'AUDIT')
   OR (resource_type = 'CLIENT' AND action = 'READ' AND scope = 'ALL')
   OR (resource_type = 'SYSTEM' AND action = 'AUDIT')
   OR (resource_type = 'REPORT' AND action IN ('READ', 'EXPORT'))
ON CONFLICT DO NOTHING;

-- Report Viewer: Read-only reports
INSERT INTO rbac_role_permissions (role_id, permission_id)
SELECT
    (SELECT id FROM rbac_roles WHERE role_name = 'report-viewer'),
    id
FROM rbac_permissions
WHERE resource_type = 'REPORT' AND action = 'READ'
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 6. Create indexes for performance
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_rbac_roles_keycloak_id ON rbac_roles(keycloak_role_id);
CREATE INDEX IF NOT EXISTS idx_rbac_roles_enum ON rbac_roles(role_enum);
CREATE INDEX IF NOT EXISTS idx_rbac_role_permissions_role ON rbac_role_permissions(role_id);
CREATE INDEX IF NOT EXISTS idx_rbac_role_permissions_perm ON rbac_role_permissions(permission_id);
CREATE INDEX IF NOT EXISTS idx_rbac_sync_log_timestamp ON rbac_sync_log(sync_timestamp DESC);

-- ============================================================================
-- 7. Add audit trigger for role changes
-- ============================================================================

CREATE OR REPLACE FUNCTION audit_rbac_role_changes()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        INSERT INTO audit.audit_log (
            table_name,
            record_id,
            operation,
            old_values,
            new_values,
            changed_by
        ) VALUES (
            'rbac_roles',
            NEW.id,
            'UPDATE',
            row_to_json(OLD),
            row_to_json(NEW),
            current_setting('app.current_user_id', true)::UUID
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER rbac_roles_audit
    AFTER UPDATE ON rbac_roles
    FOR EACH ROW
    EXECUTE FUNCTION audit_rbac_role_changes();

-- ============================================================================
-- Comments for documentation
-- ============================================================================

COMMENT ON TABLE rbac_roles IS 'Role definitions synchronized with Keycloak realm roles';
COMMENT ON TABLE rbac_role_composites IS 'Composite role hierarchy (e.g., exec includes supervisor)';
COMMENT ON TABLE rbac_permissions IS 'Fine-grained permission definitions for resources and actions';
COMMENT ON TABLE rbac_role_permissions IS 'Many-to-many mapping of roles to permissions';
COMMENT ON TABLE rbac_sync_log IS 'Audit log for Keycloak role synchronization jobs';

COMMENT ON COLUMN rbac_roles.keycloak_role_id IS 'Keycloak UUID for bidirectional sync';
COMMENT ON COLUMN rbac_roles.role_name IS 'Kebab-case role name matching Keycloak';
COMMENT ON COLUMN rbac_roles.role_enum IS 'Internal enum for application code';
COMMENT ON COLUMN rbac_roles.requires_mfa IS 'Flag for MFA enforcement on sensitive roles';
COMMENT ON COLUMN rbac_permissions.scope IS 'Permission scope: ALL (any record), OWN (assigned only), TEAM, DEPARTMENT';

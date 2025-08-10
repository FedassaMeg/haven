-- ============================================================================
-- V1: Create Core Schema for Haven DV Case Management System
-- ============================================================================

-- Create schemas
CREATE SCHEMA IF NOT EXISTS haven;
CREATE SCHEMA IF NOT EXISTS audit;

-- Set default schema
SET search_path TO haven, public;

-- ============================================================================
-- Extensions
-- ============================================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- Enums and Custom Types
-- ============================================================================

-- User roles
DO $$ BEGIN
    CREATE TYPE user_role AS ENUM (
        'ADMIN',
        'SUPERVISOR',
        'CASE_MANAGER',
        'INTAKE_SPECIALIST',
        'REPORT_VIEWER',
        'EXTERNAL_PARTNER'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Client status
DO $$ BEGIN
    CREATE TYPE client_status AS ENUM (
        'ACTIVE',
        'INACTIVE',
        'ARCHIVED',
        'DECEASED'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Case status
DO $$ BEGIN
    CREATE TYPE case_status AS ENUM (
        'OPEN',
        'IN_PROGRESS',
        'PENDING_REVIEW',
        'CLOSED',
        'REOPENED'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Case priority
DO $$ BEGIN
    CREATE TYPE case_priority AS ENUM (
        'CRITICAL',
        'HIGH',
        'MEDIUM',
        'LOW'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Gender
DO $$ BEGIN
    CREATE TYPE gender AS ENUM (
        'MALE',
        'FEMALE',
        'NON_BINARY',
        'TRANSGENDER',
        'OTHER',
        'PREFER_NOT_TO_SAY'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Incident severity
DO $$ BEGIN
    CREATE TYPE incident_severity AS ENUM (
        'CRITICAL',
        'HIGH',
        'MEDIUM',
        'LOW',
        'INFORMATIONAL'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Program status
DO $$ BEGIN
    CREATE TYPE program_status AS ENUM (
        'ACTIVE',
        'INACTIVE',
        'ARCHIVED'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Enrollment status
DO $$ BEGIN
    CREATE TYPE enrollment_status AS ENUM (
        'PENDING',
        'ACTIVE',
        'COMPLETED',
        'WITHDRAWN',
        'TERMINATED'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- ============================================================================
-- Core Tables
-- ============================================================================

-- Users table (for authentication and authorization)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    keycloak_id VARCHAR(255) UNIQUE,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role user_role NOT NULL DEFAULT 'CASE_MANAGER',
    department VARCHAR(100),
    phone VARCHAR(20),
    is_active BOOLEAN DEFAULT true,
    last_login_at TIMESTAMP WITH TIME ZONE,
    password_changed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id)
);

-- Organizations (for multi-tenant support)
CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(50),
    postal_code VARCHAR(20),
    country VARCHAR(100) DEFAULT 'USA',
    phone VARCHAR(20),
    email VARCHAR(255),
    website VARCHAR(255),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Clients (people receiving services)
CREATE TABLE clients (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_number VARCHAR(50) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    last_name VARCHAR(100) NOT NULL,
    preferred_name VARCHAR(100),
    date_of_birth DATE,
    ssn_encrypted VARCHAR(255), -- Encrypted SSN
    gender gender,
    email VARCHAR(255),
    phone_primary VARCHAR(20),
    phone_secondary VARCHAR(20),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(50),
    postal_code VARCHAR(20),
    country VARCHAR(100) DEFAULT 'USA',
    emergency_contact_name VARCHAR(200),
    emergency_contact_phone VARCHAR(20),
    emergency_contact_relationship VARCHAR(100),
    status client_status DEFAULT 'ACTIVE',
    intake_date DATE NOT NULL DEFAULT CURRENT_DATE,
    discharge_date DATE,
    organization_id UUID REFERENCES organizations(id),
    assigned_case_manager_id UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    version INTEGER DEFAULT 1,
    CONSTRAINT chk_dates CHECK (discharge_date IS NULL OR discharge_date >= intake_date)
);

-- Household members
CREATE TABLE household_members (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    relationship VARCHAR(100),
    date_of_birth DATE,
    gender gender,
    is_dependent BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Cases
CREATE TABLE cases (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    case_number VARCHAR(50) UNIQUE NOT NULL,
    client_id UUID NOT NULL REFERENCES clients(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status case_status DEFAULT 'OPEN',
    priority case_priority DEFAULT 'MEDIUM',
    assigned_to UUID REFERENCES users(id),
    supervisor_id UUID REFERENCES users(id),
    opened_date DATE NOT NULL DEFAULT CURRENT_DATE,
    closed_date DATE,
    reopened_date DATE,
    last_activity_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    organization_id UUID REFERENCES organizations(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    version INTEGER DEFAULT 1,
    CONSTRAINT chk_case_dates CHECK (closed_date IS NULL OR closed_date >= opened_date)
);

-- Case notes
CREATE TABLE case_notes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    note_type VARCHAR(50) NOT NULL, -- 'GENERAL', 'PROGRESS', 'INCIDENT', 'CONTACT'
    subject VARCHAR(255),
    content TEXT NOT NULL,
    is_confidential BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id)
);

-- Incidents
CREATE TABLE incidents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    incident_number VARCHAR(50) UNIQUE NOT NULL,
    client_id UUID NOT NULL REFERENCES clients(id),
    case_id UUID REFERENCES cases(id),
    incident_date TIMESTAMP WITH TIME ZONE NOT NULL,
    reported_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    incident_type VARCHAR(100) NOT NULL,
    severity incident_severity DEFAULT 'MEDIUM',
    location VARCHAR(255),
    description TEXT NOT NULL,
    injuries_reported BOOLEAN DEFAULT false,
    police_involved BOOLEAN DEFAULT false,
    police_report_number VARCHAR(100),
    follow_up_required BOOLEAN DEFAULT false,
    follow_up_notes TEXT,
    resolution TEXT,
    resolved_date TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id)
);

-- Programs
CREATE TABLE programs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    program_code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    capacity INTEGER,
    current_enrollment INTEGER DEFAULT 0,
    start_date DATE,
    end_date DATE,
    status program_status DEFAULT 'ACTIVE',
    eligibility_criteria TEXT,
    organization_id UUID REFERENCES organizations(id),
    coordinator_id UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_capacity CHECK (current_enrollment <= capacity),
    CONSTRAINT chk_program_dates CHECK (end_date IS NULL OR end_date >= start_date)
);

-- Program enrollments
CREATE TABLE program_enrollments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_id UUID NOT NULL REFERENCES clients(id),
    program_id UUID NOT NULL REFERENCES programs(id),
    case_id UUID REFERENCES cases(id),
    enrollment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    expected_completion_date DATE,
    actual_completion_date DATE,
    status enrollment_status DEFAULT 'PENDING',
    withdrawal_reason TEXT,
    completion_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    UNIQUE(client_id, program_id, enrollment_date)
);

-- Documents/Files
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50),
    file_size BIGINT,
    file_path VARCHAR(500),
    s3_key VARCHAR(500),
    mime_type VARCHAR(100),
    document_type VARCHAR(100),
    description TEXT,
    entity_type VARCHAR(50), -- 'CLIENT', 'CASE', 'INCIDENT', 'PROGRAM'
    entity_id UUID NOT NULL,
    is_confidential BOOLEAN DEFAULT false,
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    uploaded_by UUID REFERENCES users(id),
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by UUID REFERENCES users(id)
);

-- Appointments/Meetings
CREATE TABLE appointments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_id UUID NOT NULL REFERENCES clients(id),
    case_id UUID REFERENCES cases(id),
    staff_id UUID NOT NULL REFERENCES users(id),
    appointment_type VARCHAR(100),
    scheduled_date DATE NOT NULL,
    scheduled_time TIME NOT NULL,
    duration_minutes INTEGER DEFAULT 60,
    location VARCHAR(255),
    is_virtual BOOLEAN DEFAULT false,
    meeting_link VARCHAR(500),
    status VARCHAR(50) DEFAULT 'SCHEDULED', -- 'SCHEDULED', 'COMPLETED', 'CANCELLED', 'NO_SHOW'
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id)
);

-- ============================================================================
-- Audit Tables
-- ============================================================================

-- Audit log for tracking all changes
CREATE TABLE audit.audit_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    table_name VARCHAR(100) NOT NULL,
    record_id UUID NOT NULL,
    action VARCHAR(20) NOT NULL, -- 'INSERT', 'UPDATE', 'DELETE'
    old_values JSONB,
    new_values JSONB,
    changed_fields TEXT[],
    user_id UUID,
    username VARCHAR(100),
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Login audit
CREATE TABLE audit.login_audit (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID,
    username VARCHAR(100),
    ip_address INET,
    user_agent TEXT,
    login_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    success BOOLEAN NOT NULL,
    failure_reason VARCHAR(255)
);

-- ============================================================================
-- Indexes for Performance
-- ============================================================================

-- Users indexes
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_keycloak_id ON users(keycloak_id);
CREATE INDEX idx_users_active ON users(is_active) WHERE is_active = true;

-- Clients indexes
CREATE INDEX idx_clients_client_number ON clients(client_number);
CREATE INDEX idx_clients_name ON clients(last_name, first_name);
CREATE INDEX idx_clients_status ON clients(status);
CREATE INDEX idx_clients_case_manager ON clients(assigned_case_manager_id);
CREATE INDEX idx_clients_organization ON clients(organization_id);

-- Cases indexes
CREATE INDEX idx_cases_case_number ON cases(case_number);
CREATE INDEX idx_cases_client ON cases(client_id);
CREATE INDEX idx_cases_status ON cases(status);
CREATE INDEX idx_cases_assigned_to ON cases(assigned_to);
CREATE INDEX idx_cases_opened_date ON cases(opened_date);

-- Incidents indexes
CREATE INDEX idx_incidents_client ON incidents(client_id);
CREATE INDEX idx_incidents_case ON incidents(case_id);
CREATE INDEX idx_incidents_date ON incidents(incident_date);
CREATE INDEX idx_incidents_severity ON incidents(severity);

-- Program enrollments indexes
CREATE INDEX idx_enrollments_client ON program_enrollments(client_id);
CREATE INDEX idx_enrollments_program ON program_enrollments(program_id);
CREATE INDEX idx_enrollments_status ON program_enrollments(status);

-- Documents indexes
CREATE INDEX idx_documents_entity ON documents(entity_type, entity_id);
CREATE INDEX idx_documents_uploaded_by ON documents(uploaded_by);

-- Audit indexes
CREATE INDEX idx_audit_log_table_record ON audit.audit_log(table_name, record_id);
CREATE INDEX idx_audit_log_created_at ON audit.audit_log(created_at);
CREATE INDEX idx_audit_log_user ON audit.audit_log(user_id);

-- ============================================================================
-- Functions and Triggers
-- ============================================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply updated_at trigger to all relevant tables
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_clients_updated_at BEFORE UPDATE ON clients
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_cases_updated_at BEFORE UPDATE ON cases
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_incidents_updated_at BEFORE UPDATE ON incidents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Function to generate client numbers
CREATE OR REPLACE FUNCTION generate_client_number()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.client_number IS NULL THEN
        NEW.client_number := 'CL-' || TO_CHAR(CURRENT_DATE, 'YYYY') || '-' || 
                            LPAD(NEXTVAL('client_number_seq')::TEXT, 6, '0');
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Sequence for client numbers
CREATE SEQUENCE client_number_seq START WITH 1;

-- Trigger for client number generation
CREATE TRIGGER generate_client_number_trigger BEFORE INSERT ON clients
    FOR EACH ROW EXECUTE FUNCTION generate_client_number();

-- Similar sequences and triggers for case numbers and incident numbers
CREATE SEQUENCE case_number_seq START WITH 1;
CREATE SEQUENCE incident_number_seq START WITH 1;

-- ============================================================================
-- Initial Data
-- ============================================================================

-- Insert default organization
INSERT INTO organizations (id, name, code, description)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Haven DV Services',
    'HAVEN',
    'Primary organization for Haven DV Case Management System'
);

-- Insert system admin user (password will be managed by Keycloak)
INSERT INTO users (id, username, email, first_name, last_name, role)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    'admin',
    'admin@haven.org',
    'System',
    'Administrator',
    'ADMIN'
);

-- ============================================================================
-- Permissions and Security
-- ============================================================================

-- Create read-only role for reporting
CREATE ROLE haven_readonly;
GRANT USAGE ON SCHEMA haven TO haven_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA haven TO haven_readonly;

-- Create application role with full access
CREATE ROLE haven_app;
GRANT ALL ON SCHEMA haven TO haven_app;
GRANT ALL ON ALL TABLES IN SCHEMA haven TO haven_app;
GRANT ALL ON ALL SEQUENCES IN SCHEMA haven TO haven_app;

-- Comments for documentation
COMMENT ON TABLE clients IS 'Core table storing client information for case management';
COMMENT ON TABLE cases IS 'Cases associated with clients, tracking service delivery';
COMMENT ON TABLE incidents IS 'Critical incidents requiring documentation and follow-up';
COMMENT ON TABLE programs IS 'Service programs available to clients';
COMMENT ON TABLE program_enrollments IS 'Client enrollments in various programs';
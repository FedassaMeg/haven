-- ============================================================================
-- V8: Read Models Schema Creation
-- ============================================================================

-- Set default schema
SET search_path TO haven, public;

-- ============================================================================
-- Handle view dependencies for schema changes
-- ============================================================================

-- Temporarily drop dependent views to allow column type changes
DROP VIEW IF EXISTS active_program_enrollments CASCADE;
DROP VIEW IF EXISTS enrollment_service_summary CASCADE;

-- ============================================================================
-- Create read model tables for CQRS projections
-- ============================================================================

-- Triage Alerts table for dashboard
CREATE TABLE IF NOT EXISTS triage_alerts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_id UUID NOT NULL REFERENCES clients(id),
    alert_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    description TEXT NOT NULL,
    due_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    case_number VARCHAR(50),
    assigned_worker_id UUID REFERENCES users(id),
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    acknowledged_by UUID REFERENCES users(id),
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolved_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Caseload View table for case management
CREATE TABLE IF NOT EXISTS caseload_view (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    case_id UUID NOT NULL REFERENCES cases(id),
    case_number VARCHAR(50) NOT NULL,
    client_id UUID NOT NULL REFERENCES clients(id),
    worker_id UUID REFERENCES users(id),
    stage VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    risk_level VARCHAR(20) NOT NULL DEFAULT 'LOW',
    enrollment_date DATE,
    service_count INTEGER DEFAULT 0,
    requires_attention BOOLEAN DEFAULT false,
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    active_alerts TEXT[], -- JSON array of alert descriptions
    
    UNIQUE(case_id)
);

-- Funding Compliance View table for funding tracking
CREATE TABLE IF NOT EXISTS funding_compliance_view (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_id UUID NOT NULL REFERENCES clients(id),
    funding_source VARCHAR(100) NOT NULL,
    total_allocated DECIMAL(10,2) DEFAULT 0.00,
    total_spent DECIMAL(10,2) DEFAULT 0.00,
    remaining_amount DECIMAL(10,2) DEFAULT 0.00,
    compliance_status VARCHAR(30) NOT NULL DEFAULT 'COMPLIANT',
    documentation_gaps TEXT[],
    spending_period_start DATE,
    spending_period_end DATE,
    last_transaction_date DATE,
    requires_review BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- Indexes for performance
-- ============================================================================

-- Triage alerts indexes
CREATE INDEX IF NOT EXISTS idx_triage_alerts_client ON triage_alerts(client_id);
CREATE INDEX IF NOT EXISTS idx_triage_alerts_type ON triage_alerts(alert_type);
CREATE INDEX IF NOT EXISTS idx_triage_alerts_severity ON triage_alerts(severity);
CREATE INDEX IF NOT EXISTS idx_triage_alerts_status ON triage_alerts(status);
CREATE INDEX IF NOT EXISTS idx_triage_alerts_due_date ON triage_alerts(due_date);
CREATE INDEX IF NOT EXISTS idx_triage_alerts_worker ON triage_alerts(assigned_worker_id);

-- Caseload view indexes
CREATE INDEX IF NOT EXISTS idx_caseload_view_client ON caseload_view(client_id);
CREATE INDEX IF NOT EXISTS idx_caseload_view_worker ON caseload_view(worker_id);
CREATE INDEX IF NOT EXISTS idx_caseload_view_stage ON caseload_view(stage);
CREATE INDEX IF NOT EXISTS idx_caseload_view_status ON caseload_view(status);
CREATE INDEX IF NOT EXISTS idx_caseload_view_risk ON caseload_view(risk_level);
CREATE INDEX IF NOT EXISTS idx_caseload_view_attention ON caseload_view(requires_attention) WHERE requires_attention = true;

-- Funding compliance indexes
CREATE INDEX IF NOT EXISTS idx_funding_compliance_client ON funding_compliance_view(client_id);
CREATE INDEX IF NOT EXISTS idx_funding_compliance_source ON funding_compliance_view(funding_source);
CREATE INDEX IF NOT EXISTS idx_funding_compliance_status ON funding_compliance_view(compliance_status);
CREATE INDEX IF NOT EXISTS idx_funding_compliance_review ON funding_compliance_view(requires_review) WHERE requires_review = true;
CREATE INDEX IF NOT EXISTS idx_funding_compliance_period ON funding_compliance_view(spending_period_start, spending_period_end);

-- ============================================================================
-- Triggers for updated_at timestamps
-- ============================================================================

-- Apply updated_at triggers to read model tables
DROP TRIGGER IF EXISTS update_triage_alerts_updated_at ON triage_alerts;
CREATE TRIGGER update_triage_alerts_updated_at BEFORE UPDATE ON triage_alerts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_caseload_view_updated_at ON caseload_view;
CREATE TRIGGER update_caseload_view_updated_at BEFORE UPDATE ON caseload_view
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_funding_compliance_view_updated_at ON funding_compliance_view;
CREATE TRIGGER update_funding_compliance_view_updated_at BEFORE UPDATE ON funding_compliance_view
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- Re-create views that were dropped earlier
-- ============================================================================

-- Active enrollments view (recreated with proper column types)
CREATE OR REPLACE VIEW active_program_enrollments AS
SELECT 
    pe.*,
    c.client_number,
    c.first_name,
    c.last_name,
    p.name as program_name,
    p.program_code
FROM program_enrollments pe
JOIN clients c ON pe.client_id = c.id
JOIN programs p ON pe.program_id = p.id
WHERE pe.status = 'ACTIVE' 
AND pe.enrollment_period_end IS NULL;

-- Enrollment with service counts (recreated)
CREATE OR REPLACE VIEW enrollment_service_summary AS
SELECT 
    pe.id as enrollment_id,
    pe.client_id,
    pe.program_id,
    pe.enrollment_date,
    pe.status,
    COUNT(se.id) as total_services,
    COUNT(CASE WHEN se.outcome = 'COMPLETED' THEN 1 END) as completed_services,
    MAX(se.service_date) as last_service_date
FROM program_enrollments pe
LEFT JOIN service_episodes se ON pe.id = se.enrollment_id
GROUP BY pe.id, pe.client_id, pe.program_id, pe.enrollment_date, pe.status;

-- ============================================================================
-- Comments for documentation
-- ============================================================================

COMMENT ON TABLE triage_alerts IS 'Read model for triage dashboard alerts and notifications';
COMMENT ON TABLE caseload_view IS 'Read model for worker caseload management and filtering';
COMMENT ON TABLE funding_compliance_view IS 'Read model for funding source tracking and compliance monitoring';
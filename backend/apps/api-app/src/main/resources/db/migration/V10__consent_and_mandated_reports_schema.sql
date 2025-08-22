-- ============================================================================
-- V10: Consent and Mandated Reports Schema
-- Creates tables for consent management and mandated reporting workflow
-- ============================================================================

-- Extend existing client_consents table with additional columns needed for domain model
-- Table already exists from V7, just add missing columns

-- Add columns that don't exist in V7 schema
ALTER TABLE haven.client_consents 
ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'GRANTED',
ADD COLUMN IF NOT EXISTS recipient_organization VARCHAR(255),
ADD COLUMN IF NOT EXISTS recipient_contact VARCHAR(500),
ADD COLUMN IF NOT EXISTS granted_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS is_vawa_protected BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS limitations TEXT,
ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- Update granted_at from existing granted_date
UPDATE haven.client_consents 
SET granted_at = granted_date::timestamp 
WHERE granted_at IS NULL AND granted_date IS NOT NULL;

-- Update expires_at from existing expiration_date  
UPDATE haven.client_consents 
SET expires_at = expiration_date::timestamp 
WHERE expires_at IS NULL AND expiration_date IS NOT NULL;

-- Update status based on is_active flag
UPDATE haven.client_consents 
SET status = CASE 
    WHEN is_active = true AND (expiration_date IS NULL OR expiration_date >= CURRENT_DATE) THEN 'GRANTED'
    WHEN revoked_date IS NOT NULL THEN 'REVOKED' 
    WHEN expiration_date < CURRENT_DATE THEN 'EXPIRED'
    ELSE 'DRAFT'
END
WHERE status IS NULL;

-- Mandated reports table
CREATE TABLE IF NOT EXISTS haven.mandated_reports (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL,
    client_id UUID NOT NULL,
    report_type VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    report_number VARCHAR(100) UNIQUE,
    reporting_agency VARCHAR(200),
    agency_contact_info VARCHAR(500),
    incident_description TEXT NOT NULL,
    incident_date_time TIMESTAMP NOT NULL,
    report_created_at TIMESTAMP NOT NULL,
    filing_deadline TIMESTAMP,
    filed_at TIMESTAMP,
    created_by_user_id UUID NOT NULL,
    filed_by_user_id UUID,
    investigation_outcome TEXT,
    agency_response TEXT,
    is_emergency_report BOOLEAN DEFAULT FALSE,
    legal_justification TEXT NOT NULL,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Document attachments for mandated reports
CREATE TABLE IF NOT EXISTS haven.mandated_report_documents (
    id UUID PRIMARY KEY,
    mandated_report_id UUID NOT NULL REFERENCES haven.mandated_reports(id) ON DELETE CASCADE,
    document_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    document_type VARCHAR(100),
    file_size BIGINT,
    mime_type VARCHAR(100),
    is_required BOOLEAN DEFAULT FALSE,
    description TEXT,
    attached_by_user_id UUID NOT NULL,
    attached_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Follow-up actions for mandated reports
CREATE TABLE IF NOT EXISTS haven.mandated_report_follow_ups (
    id UUID PRIMARY KEY,
    mandated_report_id UUID NOT NULL REFERENCES haven.mandated_reports(id) ON DELETE CASCADE,
    action_description TEXT NOT NULL,
    added_by_user_id UUID NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    completed_by_user_id UUID
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_client_consents_type_status ON haven.client_consents(consent_type, status);
CREATE INDEX IF NOT EXISTS idx_client_consents_expires_at ON haven.client_consents(expires_at);
CREATE INDEX IF NOT EXISTS idx_client_consents_recipient_org ON haven.client_consents(recipient_organization);
CREATE INDEX IF NOT EXISTS idx_client_consents_vawa_protected ON haven.client_consents(is_vawa_protected) WHERE is_vawa_protected = TRUE;

CREATE INDEX IF NOT EXISTS idx_mandated_reports_case_id ON haven.mandated_reports(case_id);
CREATE INDEX IF NOT EXISTS idx_mandated_reports_client_id ON haven.mandated_reports(client_id);
CREATE INDEX IF NOT EXISTS idx_mandated_reports_type_status ON haven.mandated_reports(report_type, status);
CREATE INDEX IF NOT EXISTS idx_mandated_reports_status ON haven.mandated_reports(status);
CREATE INDEX IF NOT EXISTS idx_mandated_reports_filing_deadline ON haven.mandated_reports(filing_deadline);
CREATE INDEX IF NOT EXISTS idx_mandated_reports_emergency ON haven.mandated_reports(is_emergency_report) WHERE is_emergency_report = TRUE;
CREATE INDEX IF NOT EXISTS idx_mandated_reports_overdue ON haven.mandated_reports(status, filing_deadline) WHERE status = 'DRAFT';

CREATE INDEX IF NOT EXISTS idx_mandated_report_docs_report_id ON haven.mandated_report_documents(mandated_report_id);
CREATE INDEX IF NOT EXISTS idx_mandated_report_docs_document_id ON haven.mandated_report_documents(document_id);
CREATE INDEX IF NOT EXISTS idx_mandated_report_docs_required ON haven.mandated_report_documents(is_required) WHERE is_required = TRUE;

CREATE INDEX IF NOT EXISTS idx_mandated_report_followups_report_id ON haven.mandated_report_follow_ups(mandated_report_id);
CREATE INDEX IF NOT EXISTS idx_mandated_report_followups_completed ON haven.mandated_report_follow_ups(completed_at);

-- Constraints and business rules
ALTER TABLE haven.client_consents 
    ADD CONSTRAINT chk_consent_status 
    CHECK (status IN ('GRANTED', 'REVOKED', 'EXPIRED', 'PENDING', 'DENIED'));

ALTER TABLE haven.client_consents 
    ADD CONSTRAINT chk_consent_type 
    CHECK (consent_type IN (
        'INFORMATION_SHARING', 'HMIS_PARTICIPATION', 'COURT_TESTIMONY', 
        'LEGAL_COUNSEL_COMMUNICATION', 'MEDICAL_INFORMATION_SHARING', 
        'FAMILY_CONTACT', 'RESEARCH_PARTICIPATION', 'MEDIA_RELEASE', 
        'REFERRAL_SHARING', 'FOLLOW_UP_CONTACT'
    ));

ALTER TABLE haven.mandated_reports 
    ADD CONSTRAINT chk_report_status 
    CHECK (status IN (
        'DRAFT', 'FILED', 'ACKNOWLEDGED', 'UNDER_INVESTIGATION', 
        'INVESTIGATION_COMPLETE', 'SUBSTANTIATED', 'UNSUBSTANTIATED', 
        'INCONCLUSIVE', 'CLOSED', 'OVERDUE'
    ));

ALTER TABLE haven.mandated_reports 
    ADD CONSTRAINT chk_report_type 
    CHECK (report_type IN (
        'CHILD_ABUSE', 'ELDER_ABUSE', 'DOMESTIC_VIOLENCE', 'SEXUAL_ASSAULT', 
        'HUMAN_TRAFFICKING', 'MENTAL_HEALTH_HOLD', 'COMMUNICABLE_DISEASE', 
        'COURT_ORDERED', 'WELFARE_FRAUD', 'SUSPICIOUS_DEATH'
    ));

-- Ensure filing deadline is after incident date
ALTER TABLE haven.mandated_reports 
    ADD CONSTRAINT chk_filing_deadline_after_incident 
    CHECK (filing_deadline >= incident_date_time);

-- Ensure filed_at is after report creation if present
ALTER TABLE haven.mandated_reports 
    ADD CONSTRAINT chk_filed_at_after_creation 
    CHECK (filed_at IS NULL OR filed_at >= report_created_at);

-- Comments for documentation
COMMENT ON TABLE haven.client_consents IS 'Client consent and Release of Information (ROI) records with VAWA compliance';
COMMENT ON TABLE haven.mandated_reports IS 'Mandated reporting workflow with legal compliance tracking';
COMMENT ON TABLE haven.mandated_report_documents IS 'Document attachments for mandated reports';
COMMENT ON TABLE haven.mandated_report_follow_ups IS 'Follow-up actions and their completion status';

COMMENT ON COLUMN haven.client_consents.is_vawa_protected IS 'VAWA (Violence Against Women Act) protection flag for sensitive consents';
COMMENT ON COLUMN haven.mandated_reports.legal_justification IS 'Legal basis and justification for the mandated report';
COMMENT ON COLUMN haven.mandated_reports.is_emergency_report IS 'Flag indicating emergency reports requiring immediate attention';
COMMENT ON COLUMN haven.mandated_reports.filing_deadline IS 'Legal deadline for filing the report with appropriate agency';
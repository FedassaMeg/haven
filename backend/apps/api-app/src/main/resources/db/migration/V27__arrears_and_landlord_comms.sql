-- V8: Arrears tracking enhancements and landlord communication logging

-- Create enum for assistance payment subtypes
CREATE TYPE assistance_payment_subtype AS ENUM (
    'RENT_CURRENT',
    'RENT_ARREARS',
    'UTILITY_CURRENT',
    'UTILITY_ARREARS',
    'SECURITY_DEPOSIT',
    'APPLICATION_FEE',
    'MOVING_COSTS',
    'OTHER'
);

-- Enhance assistance_payments table with arrears tracking
ALTER TABLE assistance_payments
    ADD COLUMN subtype assistance_payment_subtype NOT NULL DEFAULT 'OTHER',
    ADD COLUMN period_start DATE,
    ADD COLUMN period_end DATE;

-- Add index for efficient queries on subtype and payment date
CREATE INDEX idx_assistance_payments_subtype_date ON assistance_payments(subtype, payment_date);

-- Add check constraint to ensure period validity
ALTER TABLE assistance_payments
    ADD CONSTRAINT chk_period_validity CHECK (
        (period_start IS NULL AND period_end IS NULL) OR
        (period_start <= period_end)
    );

-- Add check constraint for arrears requiring period fields
ALTER TABLE assistance_payments
    ADD CONSTRAINT chk_arrears_period_required CHECK (
        (subtype NOT IN ('RENT_ARREARS', 'UTILITY_ARREARS')) OR
        (period_start IS NOT NULL AND period_end IS NOT NULL)
    );

-- Create landlord_communications table for tracking all landlord interactions
CREATE TABLE landlord_communications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    landlord_id UUID NOT NULL REFERENCES landlords(id),
    client_id UUID NOT NULL REFERENCES clients(id),
    housing_assistance_id UUID REFERENCES housing_assistance(id),
    channel VARCHAR(30) NOT NULL CHECK (channel IN ('PHONE', 'EMAIL', 'FAX', 'PORTAL', 'IN_PERSON', 'OTHER')),
    subject VARCHAR(255),
    body TEXT,
    shared_fields JSONB,
    recipient_contact VARCHAR(255),
    consent_checked BOOLEAN NOT NULL DEFAULT FALSE,
    consent_type VARCHAR(50),
    sent_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (sent_status IN ('SENT', 'FAILED', 'DRAFT')),
    sent_at TIMESTAMPTZ,
    sent_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for efficient queries
CREATE INDEX idx_landlord_communications_landlord ON landlord_communications(landlord_id);
CREATE INDEX idx_landlord_communications_client ON landlord_communications(client_id);
CREATE INDEX idx_landlord_communications_sent_at ON landlord_communications(sent_at);

-- Create view for arrears payment summaries
CREATE VIEW arrears_payments AS
SELECT 
    ap.client_id,
    ap.enrollment_id,
    ap.subtype,
    ap.amount,
    ap.payment_date,
    ap.period_start,
    ap.period_end,
    DATE_PART('year', ap.period_start) AS arrears_year,
    DATE_PART('month', ap.period_start) AS arrears_month,
    f.name AS funder_name,
    ha.id AS housing_assistance_id
FROM assistance_payments ap
LEFT JOIN housing_assistance ha ON ap.enrollment_id = ha.enrollment_id
LEFT JOIN funders f ON ap.funder_id = f.id
WHERE ap.subtype IN ('RENT_ARREARS', 'UTILITY_ARREARS');

-- Extend active_assistance view to include arrears totals
CREATE OR REPLACE VIEW active_assistance AS
SELECT 
    ha.id,
    ha.enrollment_id,
    ha.client_id,
    ha.assistance_type,
    ha.status,
    ha.start_date,
    ha.end_date,
    ha.monthly_amount,
    ha.total_budget,
    COALESCE(SUM(ap.amount) FILTER (WHERE ap.subtype NOT IN ('RENT_ARREARS', 'UTILITY_ARREARS')), 0) AS total_current_paid,
    COALESCE(SUM(ap.amount) FILTER (WHERE ap.subtype IN ('RENT_ARREARS', 'UTILITY_ARREARS')), 0) AS total_arrears_paid,
    COALESCE(SUM(ap.amount), 0) AS total_paid,
    ha.total_budget - COALESCE(SUM(ap.amount), 0) AS remaining_budget
FROM housing_assistance ha
LEFT JOIN assistance_payments ap ON ha.enrollment_id = ap.enrollment_id
WHERE ha.status = 'ACTIVE'
GROUP BY ha.id, ha.enrollment_id, ha.client_id, ha.assistance_type, 
         ha.status, ha.start_date, ha.end_date, ha.monthly_amount, ha.total_budget;

-- Data migration: Backfill existing payment_type values to new subtype
UPDATE assistance_payments
SET subtype = CASE
    WHEN payment_type = 'DEPOSIT' THEN 'SECURITY_DEPOSIT'::assistance_payment_subtype
    WHEN payment_type = 'UTILITIES' THEN 'UTILITY_CURRENT'::assistance_payment_subtype
    WHEN payment_type = 'RENT' THEN 'RENT_CURRENT'::assistance_payment_subtype
    WHEN payment_type = 'APPLICATION_FEE' THEN 'APPLICATION_FEE'::assistance_payment_subtype
    WHEN payment_type = 'MOVING' THEN 'MOVING_COSTS'::assistance_payment_subtype
    ELSE 'OTHER'::assistance_payment_subtype
END
WHERE subtype = 'OTHER'::assistance_payment_subtype;

-- Add trigger to update updated_at timestamp
CREATE TRIGGER update_landlord_communications_updated_at
    BEFORE UPDATE ON landlord_communications
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
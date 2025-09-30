-- V27.1: Arrears tracking enhancements and landlord communication logging

-- Create enum for assistance payment subtypes if needed
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'assistance_payment_subtype') THEN
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
    END IF;
END
$$;

-- Enhance assistance_payments table with arrears tracking
ALTER TABLE assistance_payments
    ADD COLUMN IF NOT EXISTS subtype assistance_payment_subtype NOT NULL DEFAULT 'OTHER';
ALTER TABLE assistance_payments
    ADD COLUMN IF NOT EXISTS period_start DATE;
ALTER TABLE assistance_payments
    ADD COLUMN IF NOT EXISTS period_end DATE;

-- Add index for efficient queries on subtype and payment date
CREATE INDEX IF NOT EXISTS idx_assistance_payments_subtype_date
    ON assistance_payments(subtype, payment_date);

-- Add check constraint to ensure period validity if missing
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_period_validity'
          AND conrelid = 'assistance_payments'::regclass
    ) THEN
        ALTER TABLE assistance_payments
            ADD CONSTRAINT chk_period_validity CHECK (
                (period_start IS NULL AND period_end IS NULL) OR
                (period_start <= period_end)
            );
    END IF;
END
$$;

-- Add check constraint for arrears requiring period fields if missing
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_arrears_period_required'
          AND conrelid = 'assistance_payments'::regclass
    ) THEN
        ALTER TABLE assistance_payments
            ADD CONSTRAINT chk_arrears_period_required CHECK (
                (subtype NOT IN ('RENT_ARREARS', 'UTILITY_ARREARS')) OR
                (period_start IS NOT NULL AND period_end IS NOT NULL)
            );
    END IF;
END
$$;

-- Create landlord_communications table for tracking landlord interactions
CREATE TABLE IF NOT EXISTS landlord_communications (
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
CREATE INDEX IF NOT EXISTS idx_landlord_communications_landlord
    ON landlord_communications(landlord_id);
CREATE INDEX IF NOT EXISTS idx_landlord_communications_client
    ON landlord_communications(client_id);
CREATE INDEX IF NOT EXISTS idx_landlord_communications_status
    ON landlord_communications(sent_status);

-- Refresh updated_at trigger handling
DROP TRIGGER IF EXISTS trg_landlord_communications_updated_at ON landlord_communications;

CREATE OR REPLACE FUNCTION set_landlord_communications_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_landlord_communications_updated_at
AFTER UPDATE ON landlord_communications
FOR EACH ROW
EXECUTE PROCEDURE set_landlord_communications_updated_at();


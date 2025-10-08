-- HMIS Health Insurance Schema - FY2024 Data Standards
-- Supports multiple records per enrollment for lifecycle events

-- Create ENUM types for PostgreSQL
CREATE TYPE five_point_response AS ENUM ('NO', 'YES', 'CLIENT_DOESNT_KNOW', 'CLIENT_REFUSED', 'DATA_NOT_COLLECTED');

CREATE TYPE hopwa_no_insurance_reason AS ENUM (
    'APPLIED_PENDING',
    'APPLIED_NOT_ELIGIBLE', 
    'CLIENT_DID_NOT_APPLY',
    'TYPE_NA',
    'CLIENT_DOESNT_KNOW',
    'CLIENT_REFUSED',
    'DATA_NOT_COLLECTED'
);

-- Health Insurance Records Table
CREATE TABLE health_insurance_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    enrollment_id UUID NOT NULL,
    client_id UUID NOT NULL,
    information_date DATE NOT NULL,
    record_type VARCHAR(20) NOT NULL, -- START, UPDATE, ANNUAL, EXIT, MINOR18
    
    -- Overall Insurance Status (4.04.1)
    covered_by_health_insurance INTEGER NOT NULL, -- 0=No, 1=Yes, 8=DK, 9=Refused, 99=DNC
    
    -- Individual Insurance Sources (4.04.2-4.04.A) - Boolean for each source
    medicaid BOOLEAN DEFAULT FALSE,
    medicare BOOLEAN DEFAULT FALSE,
    schip BOOLEAN DEFAULT FALSE, -- State Children's Health Insurance Program
    vha_medical_services BOOLEAN DEFAULT FALSE, -- VA Medical Services
    employer_provided BOOLEAN DEFAULT FALSE,
    cobra BOOLEAN DEFAULT FALSE, -- Consolidated Omnibus Budget Reconciliation Act
    private_pay BOOLEAN DEFAULT FALSE, -- Purchased directly/Private payment
    state_adult_health_insurance BOOLEAN DEFAULT FALSE,
    indian_health_service BOOLEAN DEFAULT FALSE,
    other_insurance BOOLEAN DEFAULT FALSE,
    other_insurance_specify TEXT, -- Free text for "Other" source
    
    -- HOPWA-specific field (only required for HOPWA programs when no insurance)
    hopwa_no_insurance_reason hopwa_no_insurance_reason NULL,
    
    -- Audit fields
    collected_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    -- Foreign key constraints
    CONSTRAINT fk_health_insurance_enrollment FOREIGN KEY (enrollment_id) 
        REFERENCES program_enrollments(id) ON DELETE CASCADE,
    CONSTRAINT fk_health_insurance_client FOREIGN KEY (client_id) 
        REFERENCES clients(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_health_insurance_enrollment_date ON health_insurance_records(enrollment_id, information_date);
CREATE INDEX idx_health_insurance_client_date ON health_insurance_records(client_id, information_date);
CREATE INDEX idx_health_insurance_record_type ON health_insurance_records(record_type);
CREATE UNIQUE INDEX idx_health_insurance_enrollment_date_type 
    ON health_insurance_records(enrollment_id, information_date, record_type);

-- Check constraints for HMIS compliance
ALTER TABLE health_insurance_records ADD CONSTRAINT chk_covered_by_health_insurance 
    CHECK (covered_by_health_insurance IN (0, 1, 8, 9, 99));

ALTER TABLE health_insurance_records ADD CONSTRAINT chk_record_type 
    CHECK (record_type IN ('START', 'UPDATE', 'ANNUAL', 'EXIT', 'MINOR18'));

-- Data quality constraint: if covered_by_health_insurance = 1 (Yes), at least one source must be true
ALTER TABLE health_insurance_records ADD CONSTRAINT chk_has_insurance_sources 
    CHECK (
        covered_by_health_insurance != 1 OR 
        (medicaid = TRUE OR medicare = TRUE OR schip = TRUE OR 
         vha_medical_services = TRUE OR employer_provided = TRUE OR 
         cobra = TRUE OR private_pay = TRUE OR state_adult_health_insurance = TRUE OR
         indian_health_service = TRUE OR other_insurance = TRUE)
    );

-- Data quality constraint: if covered_by_health_insurance = 0 (No), no sources should be true
ALTER TABLE health_insurance_records ADD CONSTRAINT chk_no_insurance_sources 
    CHECK (
        covered_by_health_insurance != 0 OR 
        (medicaid = FALSE AND medicare = FALSE AND schip = FALSE AND 
         vha_medical_services = FALSE AND employer_provided = FALSE AND 
         cobra = FALSE AND private_pay = FALSE AND state_adult_health_insurance = FALSE AND
         indian_health_service = FALSE AND other_insurance = FALSE)
    );

-- HOPWA constraint: if all sources are false and program is HOPWA, reason should be provided
-- Note: This constraint would need program/funder context, implemented in application logic instead

-- Comments for documentation
COMMENT ON TABLE health_insurance_records IS 'HMIS Health Insurance records per FY2024 Data Standards. Multiple records per enrollment for lifecycle events.';
COMMENT ON COLUMN health_insurance_records.record_type IS 'Type of data collection: START (project entry), UPDATE (change), ANNUAL (assessment), EXIT (project exit), MINOR18 (minor turns 18)';
COMMENT ON COLUMN health_insurance_records.covered_by_health_insurance IS 'Overall insurance status: 0=No, 1=Yes, 8=Client doesn''t know, 9=Client refused, 99=Data not collected';
COMMENT ON COLUMN health_insurance_records.hopwa_no_insurance_reason IS 'Required for HOPWA programs when client has no health insurance';
COMMENT ON COLUMN health_insurance_records.other_insurance_specify IS 'Free text description when other_insurance = TRUE';

-- Trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_health_insurance_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_health_insurance_updated_at
    BEFORE UPDATE ON health_insurance_records
    FOR EACH ROW
    EXECUTE FUNCTION update_health_insurance_updated_at();

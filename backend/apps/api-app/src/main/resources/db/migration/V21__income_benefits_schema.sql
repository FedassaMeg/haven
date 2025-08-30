-- HMIS Income and Benefits Schema - FY2024 Data Standards
-- Supports multiple records per enrollment for lifecycle events

-- Income and Benefits Records Table
CREATE TABLE income_benefits (
    record_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    enrollment_id UUID NOT NULL,
    client_id UUID NOT NULL,
    information_date DATE NOT NULL,
    record_type VARCHAR(20) NOT NULL, -- START, UPDATE, ANNUAL, EXIT, MINOR18
    
    -- Overall Income Status (4.02.1) 
    income_from_any_source INTEGER NOT NULL, -- 0=No, 1=Yes, 8=DK, 9=Refused, 99=DNC
    total_monthly_income INTEGER, -- Total amount in dollars
    
    -- Overall Benefits Status (4.03.1)
    benefits_from_any_source INTEGER NOT NULL DEFAULT 99, -- 0=No, 1=Yes, 8=DK, 9=Refused, 99=DNC
    
    -- Individual Income Sources (4.02.2-4.02.A) - Yes/No/DK/Refused/DNC + Amount
    earned_income INTEGER DEFAULT 99, -- DisabilityType enum values
    earned_income_amount INTEGER,
    
    unemployment_income INTEGER DEFAULT 99,
    unemployment_income_amount INTEGER,
    
    supplemental_security_income INTEGER DEFAULT 99,
    supplemental_security_income_amount INTEGER,
    
    social_security_disability_income INTEGER DEFAULT 99,
    social_security_disability_income_amount INTEGER,
    
    va_disability_service_connected INTEGER DEFAULT 99,
    va_disability_service_connected_amount INTEGER,
    
    va_disability_non_service_connected INTEGER DEFAULT 99,
    va_disability_non_service_connected_amount INTEGER,
    
    private_disability_income INTEGER DEFAULT 99,
    private_disability_income_amount INTEGER,
    
    workers_compensation INTEGER DEFAULT 99,
    workers_compensation_amount INTEGER,
    
    tanf_income INTEGER DEFAULT 99,
    tanf_income_amount INTEGER,
    
    general_assistance INTEGER DEFAULT 99,
    general_assistance_amount INTEGER,
    
    social_security_retirement INTEGER DEFAULT 99,
    social_security_retirement_amount INTEGER,
    
    pension_from_former_job INTEGER DEFAULT 99,
    pension_from_former_job_amount INTEGER,
    
    child_support INTEGER DEFAULT 99,
    child_support_amount INTEGER,
    
    alimony INTEGER DEFAULT 99,
    alimony_amount INTEGER,
    
    other_income_source INTEGER DEFAULT 99,
    other_income_amount INTEGER,
    other_income_source_identify TEXT, -- Free text for "Other" source
    
    -- Individual Non-Cash Benefits (4.03.2-4.03.A) - Yes/No/DK/Refused/DNC
    snap INTEGER DEFAULT 99, -- Supplemental Nutrition Assistance Program (Food Stamps)
    wic INTEGER DEFAULT 99, -- Special Supplemental Nutrition Program for WIC
    tanf_child_care INTEGER DEFAULT 99, -- TANF Child Care services
    tanf_transportation INTEGER DEFAULT 99, -- TANF Transportation services
    other_tanf INTEGER DEFAULT 99, -- Other TANF-funded services
    other_benefits_source INTEGER DEFAULT 99, -- Other source
    other_benefits_specify TEXT, -- Free text for "Other benefits" source
    
    -- Audit fields
    collected_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    -- Foreign key constraints
    CONSTRAINT fk_income_benefits_enrollment FOREIGN KEY (enrollment_id) 
        REFERENCES program_enrollments(enrollment_id) ON DELETE CASCADE,
    CONSTRAINT fk_income_benefits_client FOREIGN KEY (client_id) 
        REFERENCES clients(client_id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_income_benefits_enrollment ON income_benefits(enrollment_id);
CREATE INDEX idx_income_benefits_client ON income_benefits(client_id);
CREATE INDEX idx_income_benefits_info_date ON income_benefits(information_date);
CREATE INDEX idx_income_benefits_record_type ON income_benefits(record_type);
CREATE UNIQUE INDEX idx_income_benefits_enrollment_date_type 
    ON income_benefits(enrollment_id, information_date, record_type);

-- Check constraints for HMIS compliance
ALTER TABLE income_benefits ADD CONSTRAINT chk_income_from_any_source 
    CHECK (income_from_any_source IN (0, 1, 8, 9, 99));

ALTER TABLE income_benefits ADD CONSTRAINT chk_benefits_from_any_source 
    CHECK (benefits_from_any_source IN (0, 1, 8, 9, 99));

ALTER TABLE income_benefits ADD CONSTRAINT chk_record_type 
    CHECK (record_type IN ('START', 'UPDATE', 'ANNUAL', 'EXIT', 'MINOR18'));

ALTER TABLE income_benefits ADD CONSTRAINT chk_disability_type_values 
    CHECK (
        earned_income IN (0, 1, 8, 9, 99) AND
        unemployment_income IN (0, 1, 8, 9, 99) AND
        supplemental_security_income IN (0, 1, 8, 9, 99) AND
        social_security_disability_income IN (0, 1, 8, 9, 99) AND
        va_disability_service_connected IN (0, 1, 8, 9, 99) AND
        va_disability_non_service_connected IN (0, 1, 8, 9, 99) AND
        private_disability_income IN (0, 1, 8, 9, 99) AND
        workers_compensation IN (0, 1, 8, 9, 99) AND
        tanf_income IN (0, 1, 8, 9, 99) AND
        general_assistance IN (0, 1, 8, 9, 99) AND
        social_security_retirement IN (0, 1, 8, 9, 99) AND
        pension_from_former_job IN (0, 1, 8, 9, 99) AND
        child_support IN (0, 1, 8, 9, 99) AND
        alimony IN (0, 1, 8, 9, 99) AND
        other_income_source IN (0, 1, 8, 9, 99) AND
        snap IN (0, 1, 8, 9, 99) AND
        wic IN (0, 1, 8, 9, 99) AND
        tanf_child_care IN (0, 1, 8, 9, 99) AND
        tanf_transportation IN (0, 1, 8, 9, 99) AND
        other_tanf IN (0, 1, 8, 9, 99) AND
        other_benefits_source IN (0, 1, 8, 9, 99)
    );

-- Data quality constraint: if income_from_any_source = 0 (No), total should be 0 or NULL
ALTER TABLE income_benefits ADD CONSTRAINT chk_no_income_consistency 
    CHECK (
        income_from_any_source != 0 OR 
        total_monthly_income IS NULL OR 
        total_monthly_income = 0
    );

-- Data quality constraint: if income_from_any_source = 1 (Yes), total should be > 0
ALTER TABLE income_benefits ADD CONSTRAINT chk_has_income_consistency 
    CHECK (
        income_from_any_source != 1 OR 
        (total_monthly_income IS NOT NULL AND total_monthly_income > 0)
    );

-- Comments for documentation
COMMENT ON TABLE income_benefits IS 'HMIS Income and Sources records per FY2024 Data Standards. Multiple records per enrollment for lifecycle events.';
COMMENT ON COLUMN income_benefits.record_type IS 'Type of data collection: START (project entry), UPDATE (change), ANNUAL (assessment), EXIT (project exit), MINOR18 (minor turns 18)';
COMMENT ON COLUMN income_benefits.income_from_any_source IS 'Overall income status: 0=No, 1=Yes, 8=Client doesn''t know, 9=Client refused, 99=Data not collected';
COMMENT ON COLUMN income_benefits.benefits_from_any_source IS 'Overall benefits status: 0=No, 1=Yes, 8=Client doesn''t know, 9=Client refused, 99=Data not collected';
COMMENT ON COLUMN income_benefits.total_monthly_income IS 'Total monthly income amount in dollars';
COMMENT ON COLUMN income_benefits.other_income_source_identify IS 'Free text description when other_income_source = 1 (Yes)';
COMMENT ON COLUMN income_benefits.other_benefits_specify IS 'Free text description when other_benefits_source = 1 (Yes)';

-- Trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_income_benefits_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_income_benefits_updated_at
    BEFORE UPDATE ON income_benefits
    FOR EACH ROW
    EXECUTE FUNCTION update_income_benefits_updated_at();
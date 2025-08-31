-- ==================================
-- V23: Physical Disability Records
-- HMIS FY2024 Data Standards - Physical Disability
-- UDE 3.08 Physical Disability (5-point response)
-- ==================================

-- Ensure haven_application role exists
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'haven_application') THEN
        CREATE ROLE haven_application NOLOGIN;
    END IF;
END $$;

-- Grant haven_application role to current user for development
GRANT haven_application TO CURRENT_USER;

-- Create enum for data collection stage
CREATE TYPE data_collection_stage AS ENUM (
    'PROJECT_START',
    'UPDATE', 
    'PROJECT_EXIT'
);

-- Physical Disability Records table
CREATE TABLE physical_disability_records (
    id UUID NOT NULL,
    enrollment_id UUID NOT NULL,
    client_id UUID NOT NULL,
    information_date DATE NOT NULL,
    stage data_collection_stage NOT NULL,
    
    -- Physical Disability fields (UDE 3.08)
    physical_disability five_point_response NOT NULL DEFAULT 'DATA_NOT_COLLECTED',
    physical_expected_long_term five_point_response DEFAULT 'DATA_NOT_COLLECTED',
    
    -- Correction tracking
    is_correction BOOLEAN NOT NULL DEFAULT FALSE,
    corrects_record_id UUID NULL,
    
    -- Audit fields
    collected_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT pk_physical_disability_records PRIMARY KEY (id),
    CONSTRAINT fk_physical_disability_enrollment FOREIGN KEY (enrollment_id) 
        REFERENCES program_enrollments(id) ON DELETE CASCADE,
    CONSTRAINT fk_physical_disability_corrects FOREIGN KEY (corrects_record_id) 
        REFERENCES physical_disability_records(id) ON DELETE SET NULL
);

-- Indexes for performance
CREATE INDEX idx_physical_disability_enrollment_id ON physical_disability_records(enrollment_id);
CREATE INDEX idx_physical_disability_client_id ON physical_disability_records(client_id);
CREATE INDEX idx_physical_disability_enrollment_client_date 
    ON physical_disability_records(enrollment_id, client_id, information_date);
CREATE INDEX idx_physical_disability_enrollment_client_stage 
    ON physical_disability_records(enrollment_id, client_id, stage);
CREATE INDEX idx_physical_disability_information_date ON physical_disability_records(information_date);
CREATE INDEX idx_physical_disability_corrects ON physical_disability_records(corrects_record_id) 
    WHERE corrects_record_id IS NOT NULL;

-- Partial unique constraint: one non-correction record per enrollment/client/stage
CREATE UNIQUE INDEX uniq_physical_disability_enrollment_client_stage_non_correction 
    ON physical_disability_records(enrollment_id, client_id, stage) 
    WHERE is_correction = FALSE;

-- Data quality constraints
ALTER TABLE physical_disability_records 
    ADD CONSTRAINT chk_physical_disability_future_date 
    CHECK (information_date <= CURRENT_DATE);

-- Correction consistency: corrected record cannot correct itself
ALTER TABLE physical_disability_records 
    ADD CONSTRAINT chk_physical_disability_correction_not_self 
    CHECK (corrects_record_id != id OR corrects_record_id IS NULL);

-- Physical expected long-term constraint: required when physical_disability = YES
-- Note: This will be enforced at application level due to enum complexity

-- Comments for documentation
COMMENT ON TABLE physical_disability_records IS 'Physical disability records per HMIS FY2024 UDE 3.08 - tracks physical disabilities with long-term expectations';
COMMENT ON COLUMN physical_disability_records.stage IS 'Data collection stage: PROJECT_START at enrollment entry, UPDATE for changes, PROJECT_EXIT at exit';
COMMENT ON COLUMN physical_disability_records.physical_disability IS 'UDE 3.08.1 - Does client have a physical disability?';
COMMENT ON COLUMN physical_disability_records.physical_expected_long_term IS 'UDE 3.08.2 - If physical disability, is it expected to be long-continuing or indefinite?';
COMMENT ON COLUMN physical_disability_records.is_correction IS 'TRUE if this record corrects a previous record rather than recording a change';
COMMENT ON COLUMN physical_disability_records.corrects_record_id IS 'Points to the record being corrected, if this is a correction';

-- Grant permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON physical_disability_records TO haven_application;
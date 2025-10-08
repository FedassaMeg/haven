-- ==================================
-- V24: Client Disability Records
-- HMIS FY2024 Data Standards - Disabilities UDE 3.08-3.13
-- ==================================

-- Create enum for disability kinds if not exists
DO $$ BEGIN
    CREATE TYPE disability_kind AS ENUM (
        'PHYSICAL',
        'DEVELOPMENTAL',
        'CHRONIC_HEALTH_CONDITION',
        'HIV_AIDS',
        'MENTAL_HEALTH',
        'SUBSTANCE_USE'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Create enum for five-point response if not exists
DO $$ BEGIN
    CREATE TYPE hmis_five_point AS ENUM (
        'YES',
        'NO',
        'CLIENT_DOESNT_KNOW',
        'CLIENT_REFUSED',
        'DATA_NOT_COLLECTED'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Client Disability Records table
CREATE TABLE client_disability_records (
    id UUID NOT NULL,
    enrollment_id UUID NOT NULL,
    client_id UUID NOT NULL,
    information_date DATE NOT NULL,
    stage data_collection_stage NOT NULL,
    disability_kind disability_kind NOT NULL,
    
    -- Disability fields (five-point responses)
    has_disability hmis_five_point NOT NULL DEFAULT 'DATA_NOT_COLLECTED',
    expected_long_term hmis_five_point DEFAULT 'DATA_NOT_COLLECTED',
    
    -- Correction tracking
    is_correction BOOLEAN NOT NULL DEFAULT FALSE,
    corrects_record_id UUID NULL,
    
    -- Audit fields
    collected_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT pk_client_disability_records PRIMARY KEY (id),
    CONSTRAINT fk_disability_enrollment FOREIGN KEY (enrollment_id) 
        REFERENCES program_enrollments(id) ON DELETE CASCADE,
    CONSTRAINT fk_disability_corrects FOREIGN KEY (corrects_record_id) 
        REFERENCES client_disability_records(id) ON DELETE SET NULL
);

-- Indexes for performance
CREATE INDEX idx_disability_enrollment_id ON client_disability_records(enrollment_id);
CREATE INDEX idx_disability_client_id ON client_disability_records(client_id);
CREATE INDEX idx_disability_enrollment_client_date 
    ON client_disability_records(enrollment_id, client_id, information_date);
CREATE INDEX idx_disability_enrollment_client_stage_kind 
    ON client_disability_records(enrollment_id, client_id, stage, disability_kind);
CREATE INDEX idx_disability_information_date ON client_disability_records(information_date);
CREATE INDEX idx_disability_kind ON client_disability_records(disability_kind);
CREATE INDEX idx_disability_corrects ON client_disability_records(corrects_record_id) 
    WHERE corrects_record_id IS NOT NULL;

-- Partial unique constraint: one non-correction record per enrollment/client/stage/kind
CREATE UNIQUE INDEX uniq_disability_enrollment_client_stage_kind_non_correction 
    ON client_disability_records(enrollment_id, client_id, stage, disability_kind) 
    WHERE is_correction = FALSE;

-- Data quality constraints
ALTER TABLE client_disability_records 
    ADD CONSTRAINT chk_disability_future_date 
    CHECK (information_date <= CURRENT_DATE);

-- Correction consistency
ALTER TABLE client_disability_records 
    ADD CONSTRAINT chk_disability_correction_not_self 
    CHECK (corrects_record_id != id OR corrects_record_id IS NULL);

-- Comments for documentation
COMMENT ON TABLE client_disability_records IS 'Client disability records per HMIS FY2024 UDE 3.08-3.13 - tracks all disability types with lifecycle management';
COMMENT ON COLUMN client_disability_records.stage IS 'Data collection stage: PROJECT_START, UPDATE, PROJECT_EXIT';
COMMENT ON COLUMN client_disability_records.disability_kind IS 'Type of disability: PHYSICAL, DEVELOPMENTAL, CHRONIC_HEALTH_CONDITION, HIV_AIDS, MENTAL_HEALTH, SUBSTANCE_USE';
COMMENT ON COLUMN client_disability_records.has_disability IS 'Does client have this disability? (five-point response)';
COMMENT ON COLUMN client_disability_records.expected_long_term IS 'If has disability, is it expected to be long-term? (five-point response)';
COMMENT ON COLUMN client_disability_records.is_correction IS 'TRUE if this record corrects a previous record';
COMMENT ON COLUMN client_disability_records.corrects_record_id IS 'Points to the record being corrected';

-- Grant permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON client_disability_records TO haven_application;
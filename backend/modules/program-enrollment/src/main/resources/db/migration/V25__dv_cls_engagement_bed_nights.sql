-- ==================================
-- V25: Domestic Violence, Current Living Situation, Engagement, Bed Nights, CE
-- HMIS FY2024 Data Standards
-- ==================================

-- Create enum for prior living situation (HMIS UDE 3.917)
DO $$ BEGIN
    CREATE TYPE prior_living_situation AS ENUM (
        'EMERGENCY_SHELTER',
        'SAFE_HAVEN', 
        'TRANSITIONAL_HOUSING',
        'PLACE_NOT_MEANT_FOR_HABITATION',
        'PSYCHIATRIC_HOSPITAL',
        'SUBSTANCE_ABUSE_TREATMENT',
        'HOSPITAL',
        'JAIL_PRISON',
        'FOSTER_CARE_HOME',
        'LONG_TERM_CARE',
        'DOUBLED_UP',
        'DOUBLED_UP_FRIENDS',
        'HOTEL_MOTEL_NO_VOUCHER',
        'RENTAL_HOUSING',
        'RENTAL_WITH_SUBSIDY',
        'RENTAL_WITH_OTHER_SUBSIDY',
        'OWNED_BY_CLIENT',
        'OWNED_WITH_SUBSIDY',
        'PERMANENT_HOUSING',
        'RAPID_REHOUSING',
        'RESIDENTIAL_PROJECT',
        'OTHER',
        'CLIENT_DOESNT_KNOW',
        'CLIENT_PREFERS_NOT_TO_ANSWER',
        'DATA_NOT_COLLECTED'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Create enum for DV recency
CREATE TYPE dv_recency AS ENUM (
    'WITHIN_3_MONTHS',
    'THREE_TO_SIX_MONTHS',
    'SIX_TO_12_MONTHS',
    'MORE_THAN_12_MONTHS',
    'CLIENT_DOESNT_KNOW',
    'CLIENT_REFUSED',
    'DATA_NOT_COLLECTED'
);

-- ==================================
-- Domestic Violence Records
-- ==================================
CREATE TABLE dv_records (
    id UUID NOT NULL,
    enrollment_id UUID NOT NULL,
    client_id UUID NOT NULL,
    information_date DATE NOT NULL,
    stage data_collection_stage NOT NULL,
    
    -- DV fields (HMIS 4.11)
    dv_history hmis_five_point NOT NULL DEFAULT 'DATA_NOT_COLLECTED',
    currently_fleeing hmis_five_point DEFAULT 'DATA_NOT_COLLECTED',
    when_experienced dv_recency DEFAULT 'DATA_NOT_COLLECTED',
    
    -- Correction tracking
    is_correction BOOLEAN NOT NULL DEFAULT FALSE,
    corrects_record_id UUID NULL,
    
    -- Audit fields
    collected_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT pk_dv_records PRIMARY KEY (id),
    CONSTRAINT fk_dv_enrollment FOREIGN KEY (enrollment_id) 
        REFERENCES program_enrollments(id) ON DELETE CASCADE,
    CONSTRAINT fk_dv_corrects FOREIGN KEY (corrects_record_id) 
        REFERENCES dv_records(id) ON DELETE SET NULL
);

-- DV indexes
CREATE INDEX idx_dv_enrollment_id ON dv_records(enrollment_id);
CREATE INDEX idx_dv_client_id ON dv_records(client_id);
CREATE INDEX idx_dv_enrollment_client_date ON dv_records(enrollment_id, client_id, information_date);
CREATE INDEX idx_dv_enrollment_client_stage ON dv_records(enrollment_id, client_id, stage);
CREATE INDEX idx_dv_currently_fleeing ON dv_records(currently_fleeing) WHERE currently_fleeing = 'YES';

-- Unique constraint for non-correction records
CREATE UNIQUE INDEX uniq_dv_enrollment_client_stage_non_correction 
    ON dv_records(enrollment_id, client_id, stage) 
    WHERE is_correction = FALSE;

-- ==================================
-- Current Living Situations
-- ==================================
CREATE TABLE current_living_situations (
    id UUID NOT NULL,
    enrollment_id UUID NOT NULL,
    client_id UUID NOT NULL,
    contact_date DATE NOT NULL,
    
    -- Living situation (using existing enum from prior_living_situation)
    living_situation prior_living_situation NOT NULL,
    location_description TEXT,
    
    -- Verification
    verified_by VARCHAR(100),
    
    -- Optional time fields
    contact_time TIME,
    duration_minutes INTEGER,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    
    CONSTRAINT pk_current_living_situations PRIMARY KEY (id),
    CONSTRAINT fk_cls_enrollment FOREIGN KEY (enrollment_id) 
        REFERENCES program_enrollments(id) ON DELETE CASCADE
);

-- CLS indexes
CREATE INDEX idx_cls_enrollment_id ON current_living_situations(enrollment_id);
CREATE INDEX idx_cls_client_id ON current_living_situations(client_id);
CREATE INDEX idx_cls_contact_date ON current_living_situations(contact_date);
CREATE INDEX idx_cls_enrollment_contact_date ON current_living_situations(enrollment_id, contact_date DESC);

-- ==================================
-- Date of Engagement
-- ==================================
CREATE TABLE date_of_engagement_records (
    id UUID NOT NULL,
    enrollment_id UUID NOT NULL,
    client_id UUID NOT NULL,
    engagement_date DATE NOT NULL,
    
    -- Correction tracking
    is_correction BOOLEAN NOT NULL DEFAULT FALSE,
    corrects_record_id UUID NULL,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    
    CONSTRAINT pk_date_of_engagement PRIMARY KEY (id),
    CONSTRAINT fk_engagement_enrollment FOREIGN KEY (enrollment_id) 
        REFERENCES program_enrollments(id) ON DELETE CASCADE,
    CONSTRAINT fk_engagement_corrects FOREIGN KEY (corrects_record_id) 
        REFERENCES date_of_engagement_records(id) ON DELETE SET NULL
);

-- Engagement indexes
CREATE INDEX idx_engagement_enrollment_id ON date_of_engagement_records(enrollment_id);
CREATE INDEX idx_engagement_client_id ON date_of_engagement_records(client_id);

-- Unique constraint: only one effective engagement date per enrollment
CREATE UNIQUE INDEX uniq_engagement_enrollment_non_correction 
    ON date_of_engagement_records(enrollment_id) 
    WHERE is_correction = FALSE;

-- ==================================
-- Bed Nights
-- ==================================
CREATE TABLE bed_nights (
    id UUID NOT NULL,
    enrollment_id UUID NOT NULL,
    client_id UUID NOT NULL,
    bed_night_date DATE NOT NULL,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    
    CONSTRAINT pk_bed_nights PRIMARY KEY (id),
    CONSTRAINT fk_bed_night_enrollment FOREIGN KEY (enrollment_id) 
        REFERENCES program_enrollments(id) ON DELETE CASCADE
);

-- Bed night indexes
CREATE INDEX idx_bed_night_enrollment_id ON bed_nights(enrollment_id);
CREATE INDEX idx_bed_night_client_id ON bed_nights(client_id);
CREATE INDEX idx_bed_night_date ON bed_nights(bed_night_date);

-- Unique constraint: one bed night per enrollment per date
CREATE UNIQUE INDEX uniq_bed_night_enrollment_date 
    ON bed_nights(enrollment_id, bed_night_date);

-- ==================================
-- Coordinated Entry Assessments
-- ==================================
CREATE TABLE ce_assessments (
    id UUID NOT NULL,
    enrollment_id UUID NOT NULL,
    client_id UUID NOT NULL,
    assessment_date DATE NOT NULL,
    
    -- Assessment details
    assessment_type VARCHAR(50) NOT NULL,
    assessment_level VARCHAR(50),
    tool_used VARCHAR(100),
    score NUMERIC(10,2),
    prioritization_status VARCHAR(50),
    location VARCHAR(200),
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    
    CONSTRAINT pk_ce_assessments PRIMARY KEY (id),
    CONSTRAINT fk_ce_assessment_enrollment FOREIGN KEY (enrollment_id) 
        REFERENCES program_enrollments(id) ON DELETE CASCADE
);

-- CE Assessment indexes
CREATE INDEX idx_ce_assessment_enrollment_id ON ce_assessments(enrollment_id);
CREATE INDEX idx_ce_assessment_client_id ON ce_assessments(client_id);
CREATE INDEX idx_ce_assessment_date ON ce_assessments(assessment_date);
CREATE INDEX idx_ce_assessment_type ON ce_assessments(assessment_type);

-- ==================================
-- Coordinated Entry Events
-- ==================================
CREATE TABLE ce_events (
    id UUID NOT NULL,
    enrollment_id UUID NOT NULL,
    client_id UUID NOT NULL,
    event_date DATE NOT NULL,
    
    -- Event details
    event_type VARCHAR(50) NOT NULL,
    result VARCHAR(100),
    status VARCHAR(50),
    referral_destination VARCHAR(200),
    outcome_date DATE,
    
    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    
    CONSTRAINT pk_ce_events PRIMARY KEY (id),
    CONSTRAINT fk_ce_event_enrollment FOREIGN KEY (enrollment_id) 
        REFERENCES program_enrollments(id) ON DELETE CASCADE
);

-- CE Event indexes
CREATE INDEX idx_ce_event_enrollment_id ON ce_events(enrollment_id);
CREATE INDEX idx_ce_event_client_id ON ce_events(client_id);
CREATE INDEX idx_ce_event_date ON ce_events(event_date);
CREATE INDEX idx_ce_event_type ON ce_events(event_type);

-- Data quality constraints
ALTER TABLE dv_records ADD CONSTRAINT chk_dv_future_date CHECK (information_date <= CURRENT_DATE);
ALTER TABLE current_living_situations ADD CONSTRAINT chk_cls_future_date CHECK (contact_date <= CURRENT_DATE);
ALTER TABLE date_of_engagement_records ADD CONSTRAINT chk_engagement_future_date CHECK (engagement_date <= CURRENT_DATE);
ALTER TABLE bed_nights ADD CONSTRAINT chk_bed_night_future_date CHECK (bed_night_date <= CURRENT_DATE);
ALTER TABLE ce_assessments ADD CONSTRAINT chk_ce_assessment_future_date CHECK (assessment_date <= CURRENT_DATE);
ALTER TABLE ce_events ADD CONSTRAINT chk_ce_event_future_date CHECK (event_date <= CURRENT_DATE);

-- Comments for documentation
COMMENT ON TABLE dv_records IS 'Domestic violence records per HMIS FY2024 Element 4.11';
COMMENT ON TABLE current_living_situations IS 'Current living situation contacts per HMIS FY2024';
COMMENT ON TABLE date_of_engagement_records IS 'Date of engagement for street outreach programs';
COMMENT ON TABLE bed_nights IS 'Emergency shelter bed night tracking';
COMMENT ON TABLE ce_assessments IS 'Coordinated Entry assessments';
COMMENT ON TABLE ce_events IS 'Coordinated Entry events and referrals';

-- Grant permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON dv_records TO haven_application;
GRANT SELECT, INSERT, UPDATE, DELETE ON current_living_situations TO haven_application;
GRANT SELECT, INSERT, UPDATE, DELETE ON date_of_engagement_records TO haven_application;
GRANT SELECT, INSERT, UPDATE, DELETE ON bed_nights TO haven_application;
GRANT SELECT, INSERT, UPDATE, DELETE ON ce_assessments TO haven_application;
GRANT SELECT, INSERT, UPDATE, DELETE ON ce_events TO haven_application;
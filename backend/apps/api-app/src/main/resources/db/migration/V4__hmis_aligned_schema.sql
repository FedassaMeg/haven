-- ============================================================================
-- V4: HMIS-Aligned Schema Refactoring
-- ============================================================================

-- Set default schema
SET search_path TO haven, public;

-- ============================================================================
-- New HMIS-aligned enums
-- ============================================================================

-- HMIS relationship to head of household
DO $$ BEGIN
    CREATE TYPE hmis_relationship_to_head AS ENUM (
        'SELF_HEAD_OF_HOUSEHOLD',
        'HEAD_OF_HOUSEHOLD_SPOUSE_PARTNER',
        'HEAD_OF_HOUSEHOLD_CHILD',
        'HEAD_OF_HOUSEHOLD_STEP_CHILD',
        'HEAD_OF_HOUSEHOLD_GRANDCHILD',
        'HEAD_OF_HOUSEHOLD_PARENT',
        'HEAD_OF_HOUSEHOLD_SIBLING',
        'OTHER_RELATIVE',
        'UNRELATED_HOUSEHOLD_MEMBER',
        'CLIENT_DOESNT_KNOW',
        'CLIENT_REFUSED',
        'DATA_NOT_COLLECTED'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- HMIS residence prior to entry
DO $$ BEGIN
    CREATE TYPE hmis_residence_prior AS ENUM (
        'HOMELESS_SITUATION',
        'INSTITUTIONAL_SETTING',
        'HOUSED',
        'CLIENT_DOESNT_KNOW',
        'CLIENT_REFUSED',
        'DATA_NOT_COLLECTED'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- HMIS length of stay
DO $$ BEGIN
    CREATE TYPE hmis_length_of_stay AS ENUM (
        'ONE_WEEK_OR_LESS',
        'MORE_THAN_ONE_WEEK_BUT_LESS_THAN_ONE_MONTH',
        'ONE_TO_THREE_MONTHS',
        'MORE_THAN_THREE_MONTHS_BUT_LESS_THAN_ONE_YEAR',
        'ONE_YEAR_OR_LONGER',
        'CLIENT_DOESNT_KNOW',
        'CLIENT_REFUSED',
        'DATA_NOT_COLLECTED'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- HMIS exit destinations
DO $$ BEGIN
    CREATE TYPE hmis_exit_destination AS ENUM (
        'EMERGENCY_SHELTER',
        'TRANSITIONAL_HOUSING_FOR_HOMELESS_PERSONS',
        'PERMANENT_HOUSING_IN_A_HOUSING_PROGRAM',
        'OWNED_BY_CLIENT_NO_ONGOING_HOUSING_SUBSIDY',
        'OWNED_BY_CLIENT_WITH_ONGOING_HOUSING_SUBSIDY',
        'RENTAL_BY_CLIENT_NO_ONGOING_HOUSING_SUBSIDY',
        'RENTAL_BY_CLIENT_WITH_VASH_HOUSING_SUBSIDY',
        'RENTAL_BY_CLIENT_WITH_OTHER_ONGOING_HOUSING_SUBSIDY',
        'STAYING_OR_LIVING_WITH_FAMILY_TEMPORARY_TENURE',
        'STAYING_OR_LIVING_WITH_FRIENDS_TEMPORARY_TENURE',
        'STAYING_OR_LIVING_WITH_FAMILY_PERMANENT_TENURE',
        'STAYING_OR_LIVING_WITH_FRIENDS_PERMANENT_TENURE',
        'HOTEL_OR_MOTEL_PAID_FOR_WITHOUT_EMERGENCY_SHELTER_VOUCHER',
        'HOSPITAL_OR_OTHER_RESIDENTIAL_NON_PSYCHIATRIC_MEDICAL_FACILITY',
        'JAIL_PRISON_OR_JUVENILE_DETENTION_FACILITY',
        'LONG_TERM_CARE_FACILITY_OR_NURSING_HOME',
        'PSYCHIATRIC_HOSPITAL_OR_OTHER_PSYCHIATRIC_FACILITY',
        'SUBSTANCE_ABUSE_TREATMENT_FACILITY_OR_DETOX_CENTER',
        'DECEASED',
        'OTHER',
        'CLIENT_DOESNT_KNOW',
        'CLIENT_REFUSED',
        'DATA_NOT_COLLECTED'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Service episode types
DO $$ BEGIN
    CREATE TYPE service_episode_type AS ENUM (
        'CASE_MANAGEMENT',
        'COUNSELING_INDIVIDUAL',
        'COUNSELING_GROUP',
        'COUNSELING_FAMILY',
        'CRISIS_INTERVENTION',
        'SAFETY_PLANNING',
        'LEGAL_ADVOCACY',
        'COURT_ACCOMPANIMENT',
        'HOUSING_ASSISTANCE',
        'FINANCIAL_ASSISTANCE',
        'TRANSPORTATION',
        'CHILDCARE',
        'EDUCATION_SERVICES',
        'EMPLOYMENT_SERVICES',
        'HEALTHCARE_SERVICES',
        'MENTAL_HEALTH_SERVICES',
        'SUBSTANCE_ABUSE_SERVICES',
        'OTHER'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- ============================================================================
-- Refactored Program Enrollments Table (now the primary aggregate)
-- ============================================================================

-- Drop and recreate program_enrollments with HMIS alignment
DROP TABLE IF EXISTS program_enrollments CASCADE;

CREATE TABLE program_enrollments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_id UUID NOT NULL REFERENCES clients(id),
    program_id UUID NOT NULL REFERENCES programs(id),
    
    -- HMIS Universal Data Elements for enrollment
    enrollment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    relationship_to_head hmis_relationship_to_head DEFAULT 'SELF_HEAD_OF_HOUSEHOLD',
    residence_prior_to_entry hmis_residence_prior,
    length_of_stay_prior_to_entry hmis_length_of_stay,
    entry_from_street_outreach BOOLEAN DEFAULT false,
    
    -- Status and lifecycle
    status enrollment_status DEFAULT 'ACTIVE',
    enrollment_period_start TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    enrollment_period_end TIMESTAMP WITH TIME ZONE,
    
    -- Metadata
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    version INTEGER DEFAULT 1,
    
    -- Ensure no duplicate active enrollments
    UNIQUE(client_id, program_id, enrollment_date)
);

-- ============================================================================
-- Service Episodes Table (entities within ProgramEnrollment aggregate)
-- ============================================================================

CREATE TABLE IF NOT EXISTS service_episodes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    enrollment_id UUID NOT NULL REFERENCES program_enrollments(id) ON DELETE CASCADE,
    
    -- Service details
    service_type service_episode_type NOT NULL,
    service_date DATE NOT NULL,
    service_start_time TIME,
    service_end_time TIME,
    duration_minutes INTEGER,
    
    -- Service provision
    description TEXT,
    location VARCHAR(255),
    provided_by UUID REFERENCES users(id),
    outcome VARCHAR(50), -- 'COMPLETED', 'PARTIALLY_COMPLETED', 'NOT_COMPLETED', 'CANCELLED', 'NO_SHOW'
    
    -- Notes and documentation
    notes TEXT,
    is_billable BOOLEAN DEFAULT false,
    billing_code VARCHAR(50),
    
    -- Metadata
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id)
);

-- ============================================================================
-- Project Exits Table (entities within ProgramEnrollment aggregate)
-- ============================================================================

CREATE TABLE IF NOT EXISTS project_exits (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    enrollment_id UUID NOT NULL REFERENCES program_enrollments(id) ON DELETE CASCADE,
    
    -- HMIS required exit data
    exit_date DATE NOT NULL,
    destination hmis_exit_destination NOT NULL,
    
    -- Housing outcomes
    exited_to_permanent_housing BOOLEAN,
    housing_assessment VARCHAR(100),
    subsidy_information VARCHAR(100),
    
    -- Program completion
    counseling_sessions_completed BOOLEAN,
    exit_reason VARCHAR(100),
    completion_status VARCHAR(50), -- 'COMPLETED', 'PARTIAL', 'UNSUCCESSFUL'
    
    -- Documentation
    exit_notes TEXT,
    follow_up_required BOOLEAN DEFAULT false,
    follow_up_date DATE,
    
    -- Metadata
    recorded_by UUID REFERENCES users(id),
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Only one exit per enrollment
    UNIQUE(enrollment_id)
);

-- ============================================================================
-- Case-Enrollment Links Table (many-to-many relationship)
-- ============================================================================

CREATE TABLE IF NOT EXISTS case_program_enrollments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    enrollment_id UUID NOT NULL REFERENCES program_enrollments(id) ON DELETE CASCADE,
    
    -- Link metadata
    linked_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    linked_by UUID REFERENCES users(id),
    linkage_reason TEXT,
    is_primary BOOLEAN DEFAULT false,
    
    -- Unique constraint
    UNIQUE(case_id, enrollment_id)
);

-- ============================================================================
-- Update existing tables for HMIS alignment
-- ============================================================================

-- Add HMIS identifiers to clients table
ALTER TABLE clients ADD COLUMN IF NOT EXISTS hmis_personal_id VARCHAR(32);
ALTER TABLE clients ADD COLUMN IF NOT EXISTS ssn_data_quality INTEGER DEFAULT 99; -- 99 = Data not collected

-- Add program type categorization
ALTER TABLE programs ADD COLUMN IF NOT EXISTS hmis_project_type VARCHAR(50);
ALTER TABLE programs ADD COLUMN IF NOT EXISTS target_population VARCHAR(100);
ALTER TABLE programs ADD COLUMN IF NOT EXISTS geographic_area VARCHAR(100);

-- ============================================================================
-- Indexes for Performance
-- ============================================================================

-- Program enrollment indexes
CREATE INDEX IF NOT EXISTS idx_program_enrollments_client ON program_enrollments(client_id);
CREATE INDEX IF NOT EXISTS idx_program_enrollments_program ON program_enrollments(program_id);
CREATE INDEX IF NOT EXISTS idx_program_enrollments_status ON program_enrollments(status);
CREATE INDEX IF NOT EXISTS idx_program_enrollments_date ON program_enrollments(enrollment_date);
CREATE INDEX IF NOT EXISTS idx_program_enrollments_period ON program_enrollments(enrollment_period_start, enrollment_period_end);

-- Service episode indexes
CREATE INDEX IF NOT EXISTS idx_service_episodes_enrollment ON service_episodes(enrollment_id);
CREATE INDEX IF NOT EXISTS idx_service_episodes_date ON service_episodes(service_date);
CREATE INDEX IF NOT EXISTS idx_service_episodes_type ON service_episodes(service_type);

-- Case-enrollment link indexes
CREATE INDEX IF NOT EXISTS idx_case_enrollments_case ON case_program_enrollments(case_id);
CREATE INDEX IF NOT EXISTS idx_case_enrollments_enrollment ON case_program_enrollments(enrollment_id);
CREATE INDEX IF NOT EXISTS idx_case_enrollments_primary ON case_program_enrollments(case_id) WHERE is_primary = true;

-- Project exit indexes
CREATE INDEX IF NOT EXISTS idx_project_exits_enrollment ON project_exits(enrollment_id);
CREATE INDEX IF NOT EXISTS idx_project_exits_date ON project_exits(exit_date);
CREATE INDEX IF NOT EXISTS idx_project_exits_destination ON project_exits(destination);

-- ============================================================================
-- Triggers
-- ============================================================================

-- Update enrollment period end when exit is recorded
CREATE OR REPLACE FUNCTION update_enrollment_on_exit()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE program_enrollments 
    SET enrollment_period_end = NEW.recorded_at,
        status = 'COMPLETED',
        updated_at = CURRENT_TIMESTAMP
    WHERE id = NEW.enrollment_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS update_enrollment_on_exit_trigger ON project_exits;
CREATE TRIGGER update_enrollment_on_exit_trigger
    AFTER INSERT ON project_exits
    FOR EACH ROW EXECUTE FUNCTION update_enrollment_on_exit();

-- Apply updated_at triggers to new tables
DROP TRIGGER IF EXISTS update_program_enrollments_updated_at ON program_enrollments;
CREATE TRIGGER update_program_enrollments_updated_at BEFORE UPDATE ON program_enrollments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_service_episodes_updated_at ON service_episodes;
CREATE TRIGGER update_service_episodes_updated_at BEFORE UPDATE ON service_episodes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- Views for HMIS Reporting
-- ============================================================================

-- Active enrollments view
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

-- Enrollment with service counts
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
-- Comments for Documentation
-- ============================================================================

COMMENT ON TABLE program_enrollments IS 'HMIS-aligned program enrollments - primary aggregate for case work';
COMMENT ON TABLE service_episodes IS 'Individual service interactions within program enrollment';
COMMENT ON TABLE project_exits IS 'Formal program exits with HMIS-required destination tracking';
COMMENT ON TABLE case_program_enrollments IS 'Links between case coordination and program enrollments';
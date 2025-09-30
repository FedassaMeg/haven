-- Migration for Joint TH/RRH Support
-- Adds enrollment linkage, RRH move-in date, and joint project grouping

-- Add predecessor enrollment linkage for TH->RRH transitions
ALTER TABLE program_enrollments
ADD COLUMN IF NOT EXISTS predecessor_enrollment_id UUID NULL REFERENCES program_enrollments(id);

-- Add RRH-specific move-in date
ALTER TABLE program_enrollments
ADD COLUMN IF NOT EXISTS residential_move_in_date DATE NULL;

-- Add household_id column for HMIS compliance
ALTER TABLE program_enrollments
ADD COLUMN IF NOT EXISTS household_id VARCHAR(255) NULL;

-- Add joint project grouping to link TH and RRH program components
ALTER TABLE programs
ADD COLUMN IF NOT EXISTS joint_project_group_code VARCHAR(50) NULL;

-- Index for efficient predecessor lookups
CREATE INDEX IF NOT EXISTS idx_program_enrollments_predecessor 
ON program_enrollments(predecessor_enrollment_id) 
WHERE predecessor_enrollment_id IS NOT NULL;

-- Index for joint project grouping
CREATE INDEX IF NOT EXISTS idx_programs_joint_group 
ON programs(joint_project_group_code) 
WHERE joint_project_group_code IS NOT NULL;

-- Index for finding enrollments with move-in dates
CREATE INDEX IF NOT EXISTS idx_program_enrollments_move_in_date 
ON program_enrollments(residential_move_in_date) 
WHERE residential_move_in_date IS NOT NULL;

-- View for combined enrollment service history
CREATE OR REPLACE VIEW joint_enrollment_service_summary AS
WITH RECURSIVE enrollment_chain AS (
    -- Base case: start with any enrollment
    SELECT 
        pe.id as enrollment_id,
        pe.id as root_enrollment_id,
        pe.predecessor_enrollment_id,
        pe.client_id,
        pe.program_id,
        pe.enrollment_date,
        CASE 
            WHEN pe.enrollment_period_end IS NOT NULL THEN pe.enrollment_period_end::date
            WHEN pex.exit_date IS NOT NULL THEN pex.exit_date
            ELSE NULL
        END as exit_date,
        pe.residential_move_in_date,
        0 as chain_depth
    FROM program_enrollments pe
    LEFT JOIN project_exits pex ON pex.enrollment_id = pe.id
    WHERE pe.predecessor_enrollment_id IS NULL
    
    UNION ALL
    
    -- Recursive case: follow the chain
    SELECT 
        pe.id as enrollment_id,
        ec.root_enrollment_id,
        pe.predecessor_enrollment_id,
        pe.client_id,
        pe.program_id,
        pe.enrollment_date,
        CASE 
            WHEN pe.enrollment_period_end IS NOT NULL THEN pe.enrollment_period_end::date
            WHEN pex.exit_date IS NOT NULL THEN pex.exit_date
            ELSE NULL
        END as exit_date,
        pe.residential_move_in_date,
        ec.chain_depth + 1 as chain_depth
    FROM program_enrollments pe
    LEFT JOIN project_exits pex ON pex.enrollment_id = pe.id
    INNER JOIN enrollment_chain ec ON pe.predecessor_enrollment_id = ec.enrollment_id
)
SELECT 
    ec.root_enrollment_id,
    ec.enrollment_id,
    ec.chain_depth,
    ec.client_id,
    p.name as program_name,
    p.hmis_project_type,
    p.joint_project_group_code,
    ec.enrollment_date,
    ec.exit_date,
    ec.residential_move_in_date,
    COUNT(se.id) as service_count,
    MIN(se.service_date) as first_service_date,
    MAX(se.service_date) as last_service_date
FROM enrollment_chain ec
LEFT JOIN programs p ON ec.program_id = p.id
LEFT JOIN service_episodes se ON se.enrollment_id = ec.enrollment_id
GROUP BY 
    ec.root_enrollment_id,
    ec.enrollment_id,
    ec.chain_depth,
    ec.client_id,
    p.name,
    p.hmis_project_type,
    p.joint_project_group_code,
    ec.enrollment_date,
    ec.exit_date,
    ec.residential_move_in_date
ORDER BY ec.root_enrollment_id, ec.chain_depth;

-- Function to validate TH->RRH transition eligibility
CREATE OR REPLACE FUNCTION validate_th_rrh_transition(
    p_th_enrollment_id UUID,
    p_rrh_program_id UUID
) RETURNS BOOLEAN AS $$
DECLARE
    v_th_project_type INT;
    v_rrh_project_type INT;
    v_th_group_code VARCHAR(50);
    v_rrh_group_code VARCHAR(50);
    v_th_exit_date DATE;
BEGIN
    -- Get TH enrollment details
    SELECT p.hmis_project_type, p.joint_project_group_code, pe.exit_date
    INTO v_th_project_type, v_th_group_code, v_th_exit_date
    FROM program_enrollments pe
    JOIN programs p ON pe.program_id = p.id
    WHERE pe.id = p_th_enrollment_id;
    
    -- Get RRH program details
    SELECT hmis_project_type, joint_project_group_code
    INTO v_rrh_project_type, v_rrh_group_code
    FROM programs
    WHERE id = p_rrh_program_id;
    
    -- Validate transition rules
    -- 1. TH must be Transitional Housing type (2) or Joint TH/RRH (15)
    IF v_th_project_type NOT IN (2, 15) THEN
        RETURN FALSE;
    END IF;
    
    -- 2. RRH must be Rapid Re-Housing type (13) or Joint TH/RRH (15)
    IF v_rrh_project_type NOT IN (13, 15) THEN
        RETURN FALSE;
    END IF;
    
    -- 3. If group codes exist, they must match
    IF v_th_group_code IS NOT NULL AND v_rrh_group_code IS NOT NULL THEN
        IF v_th_group_code != v_rrh_group_code THEN
            RETURN FALSE;
        END IF;
    END IF;
    
    -- 4. TH enrollment should not be already exited (optional rule)
    -- Uncomment if you want to enforce this
    -- IF v_th_exit_date IS NOT NULL THEN
    --     RETURN FALSE;
    -- END IF;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

COMMENT ON COLUMN program_enrollments.predecessor_enrollment_id IS 'Links to the previous enrollment in a transition (e.g., TH enrollment when transitioning to RRH)';
COMMENT ON COLUMN program_enrollments.residential_move_in_date IS 'Date client moved into RRH housing (HMIS data element 3.20)';
COMMENT ON COLUMN programs.joint_project_group_code IS 'Groups TH and RRH components of a joint TH/RRH project';


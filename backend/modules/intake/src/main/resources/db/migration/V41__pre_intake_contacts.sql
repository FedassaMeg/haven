-- ============================================================================
-- Pre-Intake Contacts Table
-- ============================================================================
--
-- Purpose: Store temporary client contact records during the intake workflow
-- before full demographic information is collected.
--
-- Lifecycle:
-- - Created when Step 1 (Initial Contact) is completed
-- - Updated as user progresses through Steps 2-7
-- - Promoted to full client when Step 8 (Demographics) is completed
-- - Auto-deleted after 30 days if not promoted (TTL)
--
-- VAWA Compliance:
-- - Minimal PII collection before consent
-- - Supports alias names for client safety
-- - TTL ensures temporary data doesn't persist indefinitely
-- ============================================================================

CREATE TABLE IF NOT EXISTS haven.pre_intake_contacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Basic identification
    client_alias VARCHAR(200) NOT NULL,
    contact_date DATE NOT NULL,
    referral_source VARCHAR(100) NOT NULL,
    intake_worker_name VARCHAR(200) NOT NULL,

    -- Workflow progress tracking (JSONB for flexibility)
    workflow_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    current_step INTEGER NOT NULL DEFAULT 1,

    -- Lifecycle management
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expired BOOLEAN NOT NULL DEFAULT FALSE,
    promoted BOOLEAN NOT NULL DEFAULT FALSE,
    promoted_client_id UUID,

    -- Optimistic locking
    version BIGINT NOT NULL DEFAULT 0,

    -- Foreign key to promoted client (if promoted)
    CONSTRAINT fk_promoted_client FOREIGN KEY (promoted_client_id)
        REFERENCES haven.clients(id)
        ON DELETE SET NULL,

    -- Check constraints
    CONSTRAINT check_contact_date_not_future
        CHECK (contact_date <= CURRENT_DATE),

    CONSTRAINT check_expires_at_after_created
        CHECK (expires_at > created_at),

    CONSTRAINT check_current_step_range
        CHECK (current_step >= 1 AND current_step <= 10),

    CONSTRAINT check_promoted_client_id_when_promoted
        CHECK (
            (promoted = TRUE AND promoted_client_id IS NOT NULL) OR
            (promoted = FALSE AND promoted_client_id IS NULL)
        )
);

-- ============================================================================
-- Indexes
-- ============================================================================

-- Index for TTL cleanup job
CREATE INDEX idx_pre_intake_contacts_expires_at
    ON haven.pre_intake_contacts(expires_at)
    WHERE promoted = FALSE AND expired = FALSE;

-- Index for finding contacts by worker
CREATE INDEX idx_pre_intake_contacts_worker
    ON haven.pre_intake_contacts(intake_worker_name)
    WHERE promoted = FALSE AND expired = FALSE;

-- Index for finding contacts by alias (for search)
CREATE INDEX idx_pre_intake_contacts_alias
    ON haven.pre_intake_contacts
    USING gin(client_alias gin_trgm_ops);

-- Index for finding promoted contacts
CREATE INDEX idx_pre_intake_contacts_promoted_client
    ON haven.pre_intake_contacts(promoted_client_id)
    WHERE promoted = TRUE;

-- Partial index for active (non-promoted, non-expired) contacts
CREATE INDEX idx_pre_intake_contacts_active
    ON haven.pre_intake_contacts(created_at DESC)
    WHERE promoted = FALSE AND expired = FALSE;

-- ============================================================================
-- Referral Source Enum Type
-- ============================================================================

DO $$ BEGIN
    CREATE TYPE haven.referral_source AS ENUM (
        'SELF_REFERRAL',
        'LAW_ENFORCEMENT',
        'DOMESTIC_VIOLENCE_HOTLINE',
        'HOSPITAL_EMERGENCY_ROOM',
        'COURT_REFERRAL',
        'CHILD_PROTECTIVE_SERVICES',
        'COMMUNITY_OUTREACH',
        'SOCIAL_SERVICES_AGENCY',
        'FAITH_BASED_ORGANIZATION',
        'SCHOOL_COUNSELOR',
        'FRIEND_FAMILY',
        'PREVIOUS_PROGRAM_PARTICIPANT',
        'OTHER'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Update referral_source column to use enum
ALTER TABLE haven.pre_intake_contacts
    ALTER COLUMN referral_source TYPE haven.referral_source
    USING referral_source::haven.referral_source;

-- ============================================================================
-- Trigger to update updated_at timestamp
-- ============================================================================

CREATE OR REPLACE FUNCTION haven.update_pre_intake_contact_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_pre_intake_contact_updated_at
    BEFORE UPDATE ON haven.pre_intake_contacts
    FOR EACH ROW
    EXECUTE FUNCTION haven.update_pre_intake_contact_updated_at();

-- ============================================================================
-- Comments
-- ============================================================================

COMMENT ON TABLE haven.pre_intake_contacts IS
    'Temporary client contact records during intake workflow. Auto-expires after 30 days if not promoted.';

COMMENT ON COLUMN haven.pre_intake_contacts.client_alias IS
    'Alias name for client safety (may not be real name)';

COMMENT ON COLUMN haven.pre_intake_contacts.workflow_data IS
    'JSONB storage for intake step data (steps 1-7). Structure: {"step_1": {...}, "step_2": {...}, ...}';

COMMENT ON COLUMN haven.pre_intake_contacts.expires_at IS
    'Expiration timestamp (typically created_at + 30 days). Records auto-deleted after this time.';

COMMENT ON COLUMN haven.pre_intake_contacts.promoted_client_id IS
    'Reference to full client record if this temp contact was promoted (Step 8)';

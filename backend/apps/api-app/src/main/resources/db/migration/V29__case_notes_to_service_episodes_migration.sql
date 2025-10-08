-- Migration script to convert historical case notes to service episodes
-- This script creates a temporary mapping table and migration functions

-- Create temporary mapping table for migration tracking
CREATE TABLE IF NOT EXISTS case_note_migration_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_note_id UUID NOT NULL,
    service_episode_id UUID,
    migration_status VARCHAR(20) NOT NULL DEFAULT 'pending',
    migration_timestamp TIMESTAMP,
    mapping_confidence VARCHAR(20) NOT NULL, -- 'high', 'medium', 'low'
    detected_service_type VARCHAR(100),
    detected_duration_minutes INTEGER,
    detected_funding_source VARCHAR(100),
    manual_review_required BOOLEAN DEFAULT false,
    manual_review_reason TEXT,
    migration_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indices for migration table
CREATE INDEX IF NOT EXISTS idx_case_note_migration_status ON case_note_migration_log(migration_status);
CREATE INDEX IF NOT EXISTS idx_case_note_migration_confidence ON case_note_migration_log(mapping_confidence);
CREATE INDEX IF NOT EXISTS idx_case_note_migration_review ON case_note_migration_log(manual_review_required);

-- Function to detect service type from case note content
CREATE OR REPLACE FUNCTION detect_service_type_from_content(content TEXT)
RETURNS VARCHAR(100) AS $$
BEGIN
    -- Convert to lowercase for pattern matching
    content := LOWER(content);

    -- Crisis intervention patterns
    IF content ~ '(crisis|emergency|urgent|immediate|safety plan|danger|threat)' THEN
        RETURN 'CRISIS_INTERVENTION';
    END IF;

    -- Counseling patterns
    IF content ~ '(counseling|therapy|therapeutic|session|trauma|coping|mental health|psychological)' THEN
        RETURN 'INDIVIDUAL_COUNSELING';
    END IF;

    -- Legal advocacy patterns
    IF content ~ '(legal|attorney|lawyer|court|restraining order|protective order|custody|divorce)' THEN
        RETURN 'LEGAL_ADVOCACY';
    END IF;

    -- Medical advocacy patterns
    IF content ~ '(medical|doctor|hospital|health|medication|treatment|appointment)' THEN
        RETURN 'MEDICAL_ADVOCACY';
    END IF;

    -- Housing assistance patterns
    IF content ~ '(housing|apartment|shelter|rent|eviction|lease|landlord|utilities)' THEN
        RETURN 'HOUSING_ASSISTANCE';
    END IF;

    -- Financial assistance patterns
    IF content ~ '(financial|money|benefits|food stamps|welfare|assistance|budget|expenses)' THEN
        RETURN 'FINANCIAL_ASSISTANCE';
    END IF;

    -- Transportation patterns
    IF content ~ '(transportation|bus|car|ride|travel|gas|vehicle)' THEN
        RETURN 'TRANSPORTATION';
    END IF;

    -- Childcare patterns
    IF content ~ '(childcare|children|kids|school|daycare|babysitter)' THEN
        RETURN 'CHILDCARE';
    END IF;

    -- Default to case management
    RETURN 'CASE_MANAGEMENT';
END;
$$ LANGUAGE plpgsql;

-- Function to detect duration from case note content
CREATE OR REPLACE FUNCTION detect_duration_from_content(content TEXT)
RETURNS INTEGER AS $$
DECLARE
    duration_match TEXT;
BEGIN
    -- Look for explicit duration mentions
    duration_match := (regexp_matches(LOWER(content), '(\d+)\s*(hour|hr|minute|min)', 'g'))[1];

    IF duration_match IS NOT NULL THEN
        -- Check if it's hours or minutes
        IF content ~ duration_match || '\s*(hour|hr)' THEN
            RETURN duration_match::INTEGER * 60; -- Convert hours to minutes
        ELSE
            RETURN duration_match::INTEGER; -- Already in minutes
        END IF;
    END IF;

    -- Estimate based on service type and content length
    IF LENGTH(content) > 500 THEN
        RETURN 60; -- Longer notes suggest longer sessions
    ELSIF LENGTH(content) > 200 THEN
        RETURN 45;
    ELSE
        RETURN 30; -- Default duration
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Function to detect funding source from case note or case context
CREATE OR REPLACE FUNCTION detect_funding_source(content TEXT, case_id UUID)
RETURNS VARCHAR(100) AS $$
DECLARE
    funding_source VARCHAR(100);
BEGIN
    -- Check content for funding source mentions
    content := LOWER(content);

    IF content ~ '(vawa|violence against women)' THEN
        RETURN 'DOJ-VAWA';
    ELSIF content ~ '(hud|continuum of care|coc)' THEN
        RETURN 'HUD-COC';
    ELSIF content ~ '(cal-oes|california office of emergency)' THEN
        RETURN 'CAL-OES';
    ELSIF content ~ '(fema|emergency solutions)' THEN
        RETURN 'FEMA-ESG';
    ELSIF content ~ '(hopwa|housing opportunities)' THEN
        RETURN 'HUD-HOPWA';
    END IF;

    -- TODO: Add logic to check case funding or program enrollment
    -- This would require joining with program_enrollments table

    RETURN 'HUD-COC'; -- Default funding source
END;
$$ LANGUAGE plpgsql;

-- Function to determine mapping confidence
CREATE OR REPLACE FUNCTION calculate_mapping_confidence(
    note_content TEXT,
    detected_service_type VARCHAR(100),
    note_created_at TIMESTAMP
)
RETURNS VARCHAR(20) AS $$
BEGIN
    -- High confidence criteria
    IF (
        -- Recent notes (within last 2 years)
        note_created_at > CURRENT_TIMESTAMP - INTERVAL '2 years'
        AND LENGTH(note_content) > 100
        AND detected_service_type != 'CASE_MANAGEMENT'
    ) THEN
        RETURN 'high';
    END IF;

    -- Medium confidence criteria
    IF (
        note_created_at > CURRENT_TIMESTAMP - INTERVAL '5 years'
        AND LENGTH(note_content) > 50
    ) THEN
        RETURN 'medium';
    END IF;

    -- Everything else is low confidence
    RETURN 'low';
END;
$$ LANGUAGE plpgsql;

-- Function to check if manual review is required
CREATE OR REPLACE FUNCTION requires_manual_review(
    note_content TEXT,
    confidence VARCHAR(20),
    detected_service_type VARCHAR(100)
)
RETURNS BOOLEAN AS $$
BEGIN
    -- Always review low confidence mappings
    IF confidence = 'low' THEN
        RETURN true;
    END IF;

    -- Review notes with sensitive content
    IF LOWER(note_content) ~ '(confidential|privileged|attorney|legal|crisis|emergency|safety)' THEN
        RETURN true;
    END IF;

    -- Review very short or very long notes
    IF LENGTH(note_content) < 20 OR LENGTH(note_content) > 2000 THEN
        RETURN true;
    END IF;

    RETURN false;
END;
$$ LANGUAGE plpgsql;

-- Main migration analysis function
-- This populates the migration log without actually creating service episodes
CREATE OR REPLACE FUNCTION analyze_case_notes_for_migration()
RETURNS INTEGER AS $$
DECLARE
    note_record RECORD;
    detected_type VARCHAR(100);
    detected_duration INTEGER;
    detected_funding VARCHAR(100);
    confidence VARCHAR(20);
    needs_review BOOLEAN;
    review_reason TEXT;
    processed_count INTEGER := 0;
BEGIN
    -- Clear existing migration log
    TRUNCATE case_note_migration_log;

    -- Process all case notes
    FOR note_record IN
        SELECT cn.id as note_id, cn.content, cn.created_at, cn.created_by as author_id,
               c.id as case_id, c.client_id
        FROM case_notes cn
        JOIN cases c ON cn.case_id = c.id
        WHERE cn.content IS NOT NULL AND LENGTH(TRIM(cn.content)) > 0
        ORDER BY cn.created_at DESC
    LOOP
        -- Detect service characteristics
        detected_type := detect_service_type_from_content(note_record.content);
        detected_duration := detect_duration_from_content(note_record.content);
        detected_funding := detect_funding_source(note_record.content, note_record.case_id);
        confidence := calculate_mapping_confidence(note_record.content, detected_type, note_record.created_at);
        needs_review := requires_manual_review(note_record.content, confidence, detected_type);

        -- Determine review reason
        review_reason := NULL;
        IF needs_review THEN
            IF confidence = 'low' THEN
                review_reason := 'Low confidence mapping';
            ELSIF LOWER(note_record.content) ~ '(confidential|privileged)' THEN
                review_reason := 'Contains privileged content';
            ELSIF LENGTH(note_record.content) < 20 THEN
                review_reason := 'Note too short';
            ELSIF LENGTH(note_record.content) > 2000 THEN
                review_reason := 'Note too long';
            ELSE
                review_reason := 'Sensitive content detected';
            END IF;
        END IF;

        -- Insert migration record
        INSERT INTO case_note_migration_log (
            case_note_id,
            migration_status,
            mapping_confidence,
            detected_service_type,
            detected_duration_minutes,
            detected_funding_source,
            manual_review_required,
            manual_review_reason
        ) VALUES (
            note_record.note_id,
            'pending',
            confidence,
            detected_type,
            detected_duration,
            detected_funding,
            needs_review,
            review_reason
        );

        processed_count := processed_count + 1;

        -- Log progress every 1000 records
        IF processed_count % 1000 = 0 THEN
            RAISE NOTICE 'Processed % case notes', processed_count;
        END IF;
    END LOOP;

    RAISE NOTICE 'Migration analysis complete. Processed % total case notes', processed_count;
    RETURN processed_count;
END;
$$ LANGUAGE plpgsql;

-- Function to create service episodes from approved migration records
CREATE OR REPLACE FUNCTION execute_case_note_migration(
    migration_batch_size INTEGER DEFAULT 100
)
RETURNS INTEGER AS $$
DECLARE
    migration_record RECORD;
    new_episode_id UUID;
    enrollment_id UUID;
    program_id VARCHAR(100);
    created_count INTEGER := 0;
BEGIN
    -- Process pending migrations that don't require manual review
    FOR migration_record IN
        SELECT ml.*, cn.content, cn.created_at, cn.created_by as author_id,
               c.client_id, c.id as case_id
        FROM case_note_migration_log ml
        JOIN case_notes cn ON ml.case_note_id = cn.id
        JOIN cases c ON cn.case_id = c.id
        WHERE ml.migration_status = 'pending'
          AND ml.manual_review_required = false
        ORDER BY cn.created_at DESC
        LIMIT migration_batch_size
    LOOP
        -- Find an active enrollment for the client
        SELECT pe.id, pe.program_id INTO enrollment_id, program_id
        FROM program_enrollments pe
        WHERE pe.client_id = migration_record.client_id
          AND pe.status = 'ACTIVE'
        ORDER BY pe.enrollment_date DESC
        LIMIT 1;

        -- If no active enrollment, find the most recent one
        IF enrollment_id IS NULL THEN
            SELECT pe.id, pe.program_id INTO enrollment_id, program_id
            FROM program_enrollments pe
            WHERE pe.client_id = migration_record.client_id
            ORDER BY pe.enrollment_date DESC
            LIMIT 1;
        END IF;

        -- Skip if no enrollment found
        IF enrollment_id IS NULL THEN
            UPDATE case_note_migration_log
            SET migration_status = 'failed',
                migration_notes = 'No program enrollment found for client'
            WHERE id = migration_record.id;
            CONTINUE;
        END IF;

        -- Generate new service episode ID
        new_episode_id := gen_random_uuid();

        -- Create service episode record
        INSERT INTO service_episodes (
            id,
            client_id,
            enrollment_id,
            program_id,
            program_name,
            service_type,
            service_category,
            delivery_mode,
            service_date,
            planned_duration_minutes,
            actual_duration_minutes,
            primary_provider_id,
            primary_provider_name,
            primary_funding_source_funder_id,
            primary_funding_source_funder_name,
            primary_funding_source_funding_type,
            service_description,
            service_outcome,
            completion_status,
            notes,
            is_confidential,
            created_at,
            last_modified_at,
            created_by,
            last_modified_by
        )
        SELECT
            new_episode_id,
            migration_record.client_id,
            enrollment_id,
            program_id,
            'Legacy Program', -- Default program name
            migration_record.detected_service_type,
            CASE
                WHEN migration_record.detected_service_type IN ('INDIVIDUAL_COUNSELING', 'GROUP_COUNSELING') THEN 'COUNSELING'
                WHEN migration_record.detected_service_type = 'CRISIS_INTERVENTION' THEN 'CRISIS_RESPONSE'
                WHEN migration_record.detected_service_type IN ('LEGAL_ADVOCACY', 'MEDICAL_ADVOCACY') THEN 'ADVOCACY'
                WHEN migration_record.detected_service_type IN ('HOUSING_ASSISTANCE', 'FINANCIAL_ASSISTANCE') THEN 'ASSISTANCE'
                ELSE 'CASE_MANAGEMENT'
            END,
            'IN_PERSON', -- Default delivery mode
            migration_record.created_at::DATE,
            migration_record.detected_duration_minutes,
            migration_record.detected_duration_minutes,
            migration_record.author_id,
            'Legacy Staff Member', -- Default provider name
            migration_record.detected_funding_source,
            CASE migration_record.detected_funding_source
                WHEN 'DOJ-VAWA' THEN 'DOJ Violence Against Women Act'
                WHEN 'HUD-COC' THEN 'HUD Continuum of Care'
                WHEN 'CAL-OES' THEN 'California Office of Emergency Services'
                WHEN 'FEMA-ESG' THEN 'FEMA Emergency Solutions Grant'
                WHEN 'HUD-HOPWA' THEN 'HUD Housing Opportunities for Persons With AIDS'
                ELSE 'Federal Grant'
            END,
            'FEDERAL',
            migration_record.content, -- Use original note content as description
            'Service migrated from legacy case note system', -- Default outcome
            'COMPLETED', -- Assume completed for historical notes
            'Migrated from case note ID: ' || migration_record.case_note_id,
            false, -- Default not confidential
            migration_record.created_at,
            CURRENT_TIMESTAMP,
            migration_record.author_id,
            'SYSTEM_MIGRATION';

        -- Update migration log
        UPDATE case_note_migration_log
        SET service_episode_id = new_episode_id,
            migration_status = 'completed',
            migration_timestamp = CURRENT_TIMESTAMP,
            migration_notes = 'Successfully migrated to service episode'
        WHERE id = migration_record.id;

        created_count := created_count + 1;

        -- Log progress
        IF created_count % 50 = 0 THEN
            RAISE NOTICE 'Created % service episodes', created_count;
        END IF;
    END LOOP;

    RAISE NOTICE 'Migration batch complete. Created % service episodes', created_count;
    RETURN created_count;
END;
$$ LANGUAGE plpgsql;

-- Function to generate migration report
CREATE OR REPLACE FUNCTION generate_migration_report()
RETURNS TABLE (
    total_case_notes BIGINT,
    high_confidence BIGINT,
    medium_confidence BIGINT,
    low_confidence BIGINT,
    requires_review BIGINT,
    completed_migrations BIGINT,
    pending_migrations BIGINT,
    failed_migrations BIGINT,
    service_type_breakdown JSONB
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        (SELECT COUNT(*) FROM case_note_migration_log) as total_case_notes,
        (SELECT COUNT(*) FROM case_note_migration_log WHERE mapping_confidence = 'high') as high_confidence,
        (SELECT COUNT(*) FROM case_note_migration_log WHERE mapping_confidence = 'medium') as medium_confidence,
        (SELECT COUNT(*) FROM case_note_migration_log WHERE mapping_confidence = 'low') as low_confidence,
        (SELECT COUNT(*) FROM case_note_migration_log WHERE manual_review_required = true) as requires_review,
        (SELECT COUNT(*) FROM case_note_migration_log WHERE migration_status = 'completed') as completed_migrations,
        (SELECT COUNT(*) FROM case_note_migration_log WHERE migration_status = 'pending') as pending_migrations,
        (SELECT COUNT(*) FROM case_note_migration_log WHERE migration_status = 'failed') as failed_migrations,
        (SELECT jsonb_object_agg(detected_service_type, type_count)
         FROM (
             SELECT detected_service_type, COUNT(*) as type_count
             FROM case_note_migration_log
             GROUP BY detected_service_type
         ) subq
        ) as service_type_breakdown;
END;
$$ LANGUAGE plpgsql;

-- Create a view for manual review cases
CREATE OR REPLACE VIEW migration_manual_review AS
SELECT
    ml.id,
    ml.case_note_id,
    ml.mapping_confidence,
    ml.detected_service_type,
    ml.detected_duration_minutes,
    ml.manual_review_reason,
    cn.content as note_content,
    cn.created_at as note_date,
    cn.created_by as author_id,
    c.client_id,
    LENGTH(cn.content) as content_length
FROM case_note_migration_log ml
JOIN case_notes cn ON ml.case_note_id = cn.id
JOIN cases c ON cn.case_id = c.id
WHERE ml.manual_review_required = true
  AND ml.migration_status = 'pending'
ORDER BY cn.created_at DESC;

-- Add comments to document the migration process
COMMENT ON TABLE case_note_migration_log IS 'Tracks the migration of historical case notes to service episodes';
COMMENT ON FUNCTION analyze_case_notes_for_migration() IS 'Analyzes all case notes and prepares migration plan';
COMMENT ON FUNCTION execute_case_note_migration(INTEGER) IS 'Executes migration for approved records in batches';
COMMENT ON FUNCTION generate_migration_report() IS 'Generates summary report of migration progress';
COMMENT ON VIEW migration_manual_review IS 'Lists case notes requiring manual review before migration';

-- Initial analysis (commented out - run manually when ready)
-- SELECT analyze_case_notes_for_migration();
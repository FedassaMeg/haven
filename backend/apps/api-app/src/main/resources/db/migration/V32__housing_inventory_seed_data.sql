-- Housing Inventory Data Migration and Seeding Script
-- Seeds existing projects/beds with comparable flags and historical data

-- Migrate existing housing assistance data to inventory structure
WITH project_source AS (
    SELECT
        pr.program_code,
        pr.name,
        COALESCE(pr.organization_id::text, 'LEGACY-ORG') AS organization_id,
        COALESCE(MIN(ha.created_at), CURRENT_TIMESTAMP) AS created_at,
        NULLIF(COUNT(DISTINCT ha.assigned_unit_id), 0) AS funded_beds,
        CASE
            WHEN BOOL_OR(ha.assistance_type::text LIKE 'RRH%') THEN 'RAPID_REHOUSING'
            WHEN BOOL_OR(ha.assistance_type::text LIKE 'TH%') THEN 'TRANSITIONAL_HOUSING'
            WHEN BOOL_OR(ha.assistance_type::text LIKE 'PSH%') THEN 'PERMANENT_SUPPORTIVE_HOUSING'
            WHEN BOOL_OR(ha.assistance_type::text LIKE 'ESG%') THEN 'EMERGENCY_SHELTER'
            ELSE 'OTHER'
        END AS project_type
    FROM housing_assistance ha
    JOIN program_enrollments pe ON ha.enrollment_id = pe.id
    JOIN programs pr ON pe.program_id = pr.id
    WHERE ha.status IN ('ACTIVE', 'UNIT_ASSIGNED', 'APPROVED')
    GROUP BY pr.program_code, pr.name, pr.organization_id
)
INSERT INTO housing_projects (
    project_id,
    project_name,
    project_code,
    project_type,
    status,
    is_comparable,
    funded_beds,
    organization_id,
    created_at,
    created_by
)
SELECT
    'PRJ-' || ps.program_code,
    ps.name,
    ps.program_code,
    ps.project_type,
    'ACTIVE',
    FALSE,
    ps.funded_beds,
    ps.organization_id,
    ps.created_at,
    'system-migration'
FROM project_source ps
WHERE NOT EXISTS (
    SELECT 1 FROM housing_projects hp WHERE hp.project_code = ps.program_code
);

-- Create default sites for migrated projects
WITH site_source AS (
    SELECT
        hp.project_id,
        MAX(ru.address_line1) FILTER (WHERE ru.address_line1 IS NOT NULL) AS address_line1,
        MAX(ru.city) FILTER (WHERE ru.city IS NOT NULL) AS city,
        MAX(ru.state) FILTER (WHERE ru.state IS NOT NULL) AS state,
        MAX(ru.postal_code) FILTER (WHERE ru.postal_code IS NOT NULL) AS postal_code,
        NULLIF(hp.funded_beds, 0) AS funded_beds,
        NULLIF(COUNT(DISTINCT ru.id), 0) AS unit_count
    FROM housing_projects hp
    LEFT JOIN programs pr ON pr.program_code = hp.project_code
    LEFT JOIN program_enrollments pe ON pe.program_id = pr.id
    LEFT JOIN housing_assistance ha ON ha.enrollment_id = pe.id
    LEFT JOIN rental_units ru ON ru.id = ha.assigned_unit_id
    GROUP BY hp.project_id, hp.funded_beds
)
INSERT INTO project_sites (
    site_id,
    project_id,
    site_name,
    address,
    city,
    state,
    zip_code,
    total_capacity,
    created_at
)
SELECT
    'SITE-' || ss.project_id,
    ss.project_id,
    'Main Location',
    COALESCE(ss.address_line1, 'Address Not Available'),
    COALESCE(ss.city, 'Unknown'),
    COALESCE(ss.state, 'NA'),
    COALESCE(ss.postal_code, '00000'),
    COALESCE(ss.funded_beds, ss.unit_count),
    CURRENT_TIMESTAMP
FROM site_source ss
WHERE NOT EXISTS (
    SELECT 1 FROM project_sites ps WHERE ps.project_id = ss.project_id
);

-- Create default buildings for sites
INSERT INTO buildings (
    building_id,
    site_id,
    building_name,
    building_number,
    floor_count,
    total_capacity,
    created_at
)
SELECT
    'BLD-' || ps.site_id,
    ps.site_id,
    'Building A',
    'A',
    1,
    ps.total_capacity,
    CURRENT_TIMESTAMP
FROM project_sites ps
WHERE NOT EXISTS (
    SELECT 1 FROM buildings b WHERE b.site_id = ps.site_id
);

-- Migrate rental units to housing units
WITH unit_context AS (
    SELECT DISTINCT
        ru.id,
        ru.unit_id,
        ru.bedrooms,
        ru.square_footage,
        ru.market_rent,
        ru.assisted_rent,
        ru.is_accessible,
        ru.status,
        ru.created_at,
        hp.project_id,
        b.building_id
    FROM rental_units ru
    JOIN housing_assistance ha ON ha.assigned_unit_id = ru.id
    JOIN program_enrollments pe ON ha.enrollment_id = pe.id
    JOIN programs pr ON pe.program_id = pr.id
    JOIN housing_projects hp ON hp.project_code = pr.program_code
    JOIN project_sites ps ON ps.project_id = hp.project_id
    JOIN buildings b ON b.site_id = ps.site_id
)
INSERT INTO housing_units (
    unit_id,
    building_id,
    unit_number,
    unit_type,
    capacity,
    current_occupancy,
    status,
    square_footage,
    monthly_rent,
    wheelchair_accessible,
    last_inspection_date,
    next_inspection_due,
    created_at
)
SELECT
    'UNIT-' || uc.unit_id,
    uc.building_id,
    uc.unit_id,
    CASE
        WHEN COALESCE(uc.bedrooms, 0) <= 0 THEN 'STUDIO'
        WHEN uc.bedrooms = 1 THEN 'ONE_BEDROOM'
        WHEN uc.bedrooms = 2 THEN 'TWO_BEDROOM'
        WHEN uc.bedrooms = 3 THEN 'THREE_BEDROOM'
        ELSE 'FOUR_BEDROOM_PLUS'
    END,
    GREATEST(COALESCE(uc.bedrooms, 1) * 2, 1),
    0,
    CASE uc.status
        WHEN 'OCCUPIED' THEN 'OCCUPIED'
        WHEN 'UNAVAILABLE' THEN 'UNAVAILABLE'
        WHEN 'INSPECTION_FAILED' THEN 'UNAVAILABLE'
        WHEN 'CONDEMNED' THEN 'UNAVAILABLE'
        WHEN 'INSPECTION_PENDING' THEN 'INSPECTION_PENDING'
        ELSE 'AVAILABLE'
    END,
    CASE WHEN uc.square_footage IS NULL THEN NULL ELSE ROUND(uc.square_footage)::int END,
    COALESCE(uc.assisted_rent, uc.market_rent),
    COALESCE(uc.is_accessible, FALSE),
    ir.last_inspection_date,
    ir.next_inspection_due,
    COALESCE(uc.created_at, CURRENT_TIMESTAMP)
FROM unit_context uc
LEFT JOIN LATERAL (
    SELECT
        ir.inspection_date::timestamp AS last_inspection_date,
        ir.next_inspection_due::timestamp AS next_inspection_due
    FROM inspection_records ir
    WHERE ir.unit_id = uc.id
    ORDER BY ir.inspection_date DESC
    LIMIT 1
) ir ON TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM housing_units hu WHERE hu.unit_id = 'UNIT-' || uc.unit_id
);

-- Import inspection history
INSERT INTO unit_inspections (
    inspection_id,
    unit_id,
    inspection_type,
    inspection_date,
    inspector_name,
    result,
    habitability_score,
    safety_compliant,
    requires_repairs,
    repair_notes,
    notes,
    created_at
)
SELECT
    'UI-' || ir.id,
    'UNIT-' || ru.unit_id,
    CASE
        WHEN UPPER(ir.inspection_type) LIKE 'INITIAL%' THEN 'INITIAL_INSPECTION'
        WHEN UPPER(ir.inspection_type) LIKE 'ANNUAL%' THEN 'ANNUAL_INSPECTION'
        WHEN UPPER(ir.inspection_type) LIKE 'QUARTER%' THEN 'QUARTERLY_INSPECTION'
        WHEN UPPER(ir.inspection_type) LIKE 'SPECIAL%' THEN 'SPECIAL_INSPECTION'
        WHEN UPPER(ir.inspection_type) LIKE 'COMPLAINT%' THEN 'SPECIAL_INSPECTION'
        ELSE 'QUALITY_CONTROL'
    END,
    ir.inspection_date::timestamp,
    COALESCE(ir.inspector_name, 'Unknown Inspector'),
    CASE ir.result
        WHEN 'PASS' THEN 'PASSED'
        WHEN 'FAIL' THEN 'FAILED'
        WHEN 'CONDITIONAL_PASS' THEN 'CONDITIONAL_PASS'
        ELSE 'PENDING'
    END,
    NULL,
    COALESCE(ir.meets_hqs, TRUE),
    COALESCE(NOT ir.meets_hqs, FALSE),
    NULL,
    ir.overall_notes,
    COALESCE(ir.created_at, CURRENT_TIMESTAMP)
FROM inspection_records ir
JOIN rental_units ru ON ru.id = ir.unit_id
JOIN housing_units hu ON hu.unit_id = 'UNIT-' || ru.unit_id
WHERE NOT EXISTS (
    SELECT 1 FROM unit_inspections ui WHERE ui.inspection_id = 'UI-' || ir.id
);

-- Create initial capacity snapshots for all projects
INSERT INTO capacity_snapshots (
    snapshot_id,
    project_id,
    total_capacity,
    funded_beds,
    occupied_beds,
    available_beds,
    snapshot_date,
    captured_by,
    reason,
    utilization_rate
)
SELECT
    'SNAP-' || gen_random_uuid(),
    hp.project_id,
    COALESCE(SUM(hu.capacity), 0),
    hp.funded_beds,
    COALESCE(SUM(hu.current_occupancy), 0),
    COALESCE(SUM(hu.capacity), 0) - COALESCE(SUM(hu.current_occupancy), 0),
    CURRENT_TIMESTAMP,
    'system-migration',
    'Initial migration snapshot',
    CASE
        WHEN SUM(hu.capacity) > 0 THEN (CAST(SUM(hu.current_occupancy) AS DECIMAL) / SUM(hu.capacity)) * 100
        ELSE 0
    END
FROM housing_projects hp
LEFT JOIN project_sites ps ON ps.project_id = hp.project_id
LEFT JOIN buildings b ON b.site_id = ps.site_id
LEFT JOIN housing_units hu ON hu.building_id = b.building_id
GROUP BY hp.project_id, hp.funded_beds;

-- Mark projects with complete data as comparable
UPDATE housing_projects
SET is_comparable = TRUE,
    updated_at = CURRENT_TIMESTAMP,
    updated_by = 'system-migration'
WHERE project_id IN (
    SELECT hp.project_id
    FROM housing_projects hp
    JOIN project_sites ps ON ps.project_id = hp.project_id
    JOIN buildings b ON b.site_id = ps.site_id
    JOIN housing_units hu ON hu.building_id = b.building_id
    WHERE hp.funded_beds IS NOT NULL
        AND hp.funded_beds > 0
        AND hu.last_inspection_date IS NOT NULL
        AND hu.last_inspection_date > CURRENT_DATE - INTERVAL '1 year'
    GROUP BY hp.project_id
    HAVING COUNT(DISTINCT hu.unit_id) > 0
);

-- Import bed-night usage from shelter stays
DO $$
DECLARE
    shelter_table regclass;
BEGIN
    shelter_table := COALESCE(to_regclass('shelter_stays'), to_regclass('haven.shelter_stays'));
    IF shelter_table IS NULL THEN
        RETURN;
    END IF;

    EXECUTE format(
        'INSERT INTO bed_night_usage (
            usage_id,
            unit_id,
            client_id,
            entry_date,
            exit_date,
            usage_source,
            created_at
        )
        SELECT
            ''BNU-'' || gen_random_uuid(),
            hu.unit_id,
            ss.client_id,
            ss.entry_date,
            ss.exit_date,
            ''SHELTER_STAY'',
            COALESCE(ss.created_at, CURRENT_TIMESTAMP)
        FROM %s ss
        JOIN housing_units hu ON hu.unit_number = ss.unit_identifier
        WHERE ss.entry_date >= CURRENT_DATE - INTERVAL ''1 year''
            AND NOT EXISTS (
                SELECT 1 FROM bed_night_usage bnu
                WHERE bnu.client_id = ss.client_id
                    AND bnu.entry_date = ss.entry_date
            );',
        shelter_table::text
    );
END
$$ LANGUAGE plpgsql;

-- Update current occupancy based on active stays
UPDATE housing_units hu
SET current_occupancy = (
    SELECT COUNT(*)
    FROM bed_night_usage bnu
    WHERE bnu.unit_id = hu.unit_id
        AND (bnu.exit_date IS NULL OR bnu.exit_date > CURRENT_TIMESTAMP)
),
status = CASE
    WHEN (
        SELECT COUNT(*)
        FROM bed_night_usage bnu
        WHERE bnu.unit_id = hu.unit_id
            AND (bnu.exit_date IS NULL OR bnu.exit_date > CURRENT_TIMESTAMP)
    ) >= hu.capacity THEN 'OCCUPIED'
    WHEN (
        SELECT COUNT(*)
        FROM bed_night_usage bnu
        WHERE bnu.unit_id = hu.unit_id
            AND (bnu.exit_date IS NULL OR bnu.exit_date > CURRENT_TIMESTAMP)
    ) > 0 THEN 'OCCUPIED'
    ELSE 'AVAILABLE'
END,
updated_at = CURRENT_TIMESTAMP;

-- Schedule upcoming inspections for units due
INSERT INTO inspection_schedules (
    schedule_id,
    project_id,
    inspection_type,
    scheduled_date,
    status,
    created_at,
    created_by
)
SELECT DISTINCT
    'INSP-' || gen_random_uuid(),
    hp.project_id,
    'ANNUAL_INSPECTION',
    hu.next_inspection_due,
    'SCHEDULED',
    CURRENT_TIMESTAMP,
    'system-migration'
FROM housing_units hu
JOIN buildings b ON hu.building_id = b.building_id
JOIN project_sites ps ON b.site_id = ps.site_id
JOIN housing_projects hp ON ps.project_id = hp.project_id
WHERE hu.next_inspection_due IS NOT NULL
    AND hu.next_inspection_due > CURRENT_DATE
    AND hu.next_inspection_due <= CURRENT_DATE + INTERVAL '3 months'
    AND NOT EXISTS (
        SELECT 1 FROM inspection_schedules ins
        WHERE ins.project_id = hp.project_id
            AND ins.scheduled_date = hu.next_inspection_due
    );

-- Create validation report temporary table
CREATE TEMP TABLE migration_validation_report AS
SELECT
    'Projects Migrated' AS metric,
    COUNT(*) AS count
FROM housing_projects
WHERE created_by = 'system-migration'
UNION ALL
SELECT
    'Sites Created',
    COUNT(*)
FROM project_sites
WHERE created_at >= CURRENT_DATE
UNION ALL
SELECT
    'Buildings Created',
    COUNT(*)
FROM buildings
WHERE created_at >= CURRENT_DATE
UNION ALL
SELECT
    'Units Migrated',
    COUNT(*)
FROM housing_units
WHERE created_at >= CURRENT_DATE
UNION ALL
SELECT
    'Inspections Imported',
    COUNT(*)
FROM unit_inspections
WHERE created_at >= CURRENT_DATE
UNION ALL
SELECT
    'Bed-Nights Imported',
    COUNT(*)
FROM bed_night_usage
WHERE usage_source IN ('SHELTER_STAY', 'CE_REFERRAL')
UNION ALL
SELECT
    'Comparable Projects',
    COUNT(*)
FROM housing_projects
WHERE is_comparable = TRUE
UNION ALL
SELECT
    'Projects Needing Inspection',
    COUNT(DISTINCT hp.project_id)
FROM housing_projects hp
JOIN project_sites ps ON ps.project_id = hp.project_id
JOIN buildings b ON b.site_id = ps.site_id
JOIN housing_units hu ON hu.building_id = b.building_id
WHERE hu.next_inspection_due <= CURRENT_DATE + INTERVAL '30 days';

-- Output validation report
SELECT * FROM migration_validation_report ORDER BY metric;








-- Housing Inventory Management Schema
-- Supports project, site, building, unit hierarchy with inspection and utilization tracking

-- Housing Projects Table
CREATE TABLE housing_projects (
    project_id VARCHAR(50) PRIMARY KEY,
    project_name VARCHAR(255) NOT NULL,
    project_code VARCHAR(100),
    project_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    is_comparable BOOLEAN NOT NULL DEFAULT FALSE,
    funded_beds INTEGER,
    total_capacity INTEGER,
    organization_id VARCHAR(50) NOT NULL,
    funder_id VARCHAR(50),
    grant_id VARCHAR(50),

    -- Accessibility features (embedded)
    wheelchair_accessible BOOLEAN DEFAULT FALSE,
    hearing_accessible BOOLEAN DEFAULT FALSE,
    visual_accessible BOOLEAN DEFAULT FALSE,
    has_elevator BOOLEAN DEFAULT FALSE,
    has_ramp BOOLEAN DEFAULT FALSE,
    has_grab_bars BOOLEAN DEFAULT FALSE,
    has_accessible_bathroom BOOLEAN DEFAULT FALSE,
    has_accessible_kitchen BOOLEAN DEFAULT FALSE,
    has_widened_doorways BOOLEAN DEFAULT FALSE,
    ada_compliant BOOLEAN DEFAULT FALSE,
    accessibility_notes VARCHAR(500),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP,
    updated_by VARCHAR(50),

    CONSTRAINT chk_project_type CHECK (project_type IN (
        'EMERGENCY_SHELTER', 'TRANSITIONAL_HOUSING', 'PERMANENT_SUPPORTIVE_HOUSING',
        'RAPID_REHOUSING', 'SAFE_HAVEN', 'HOMELESSNESS_PREVENTION',
        'COORDINATED_ENTRY', 'SERVICES_ONLY', 'OTHER'
    )),
    CONSTRAINT chk_project_status CHECK (status IN (
        'ACTIVE', 'INACTIVE', 'SUSPENDED', 'CLOSED', 'PENDING_APPROVAL'
    ))
);

-- Project Sites Table
CREATE TABLE project_sites (
    site_id VARCHAR(50) PRIMARY KEY,
    project_id VARCHAR(50) NOT NULL,
    site_name VARCHAR(255) NOT NULL,
    address VARCHAR(500),
    city VARCHAR(100),
    state CHAR(2),
    zip_code VARCHAR(10),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    total_capacity INTEGER,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    FOREIGN KEY (project_id) REFERENCES housing_projects(project_id) ON DELETE CASCADE
);

-- Buildings Table
CREATE TABLE buildings (
    building_id VARCHAR(50) PRIMARY KEY,
    site_id VARCHAR(50) NOT NULL,
    building_name VARCHAR(255) NOT NULL,
    building_number VARCHAR(100),
    floor_count INTEGER,
    total_capacity INTEGER,

    -- Accessibility features (embedded)
    wheelchair_accessible BOOLEAN DEFAULT FALSE,
    hearing_accessible BOOLEAN DEFAULT FALSE,
    visual_accessible BOOLEAN DEFAULT FALSE,
    has_elevator BOOLEAN DEFAULT FALSE,
    has_ramp BOOLEAN DEFAULT FALSE,
    has_grab_bars BOOLEAN DEFAULT FALSE,
    has_accessible_bathroom BOOLEAN DEFAULT FALSE,
    has_accessible_kitchen BOOLEAN DEFAULT FALSE,
    has_widened_doorways BOOLEAN DEFAULT FALSE,
    ada_compliant BOOLEAN DEFAULT FALSE,
    accessibility_notes VARCHAR(500),

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    FOREIGN KEY (site_id) REFERENCES project_sites(site_id) ON DELETE CASCADE
);

-- Housing Units Table
CREATE TABLE housing_units (
    unit_id VARCHAR(50) PRIMARY KEY,
    building_id VARCHAR(50) NOT NULL,
    unit_number VARCHAR(100) NOT NULL,
    unit_type VARCHAR(50) NOT NULL,
    floor_number INTEGER,
    capacity INTEGER NOT NULL,
    current_occupancy INTEGER DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
    square_footage INTEGER,
    monthly_rent DECIMAL(10, 2),

    -- Accessibility features (embedded)
    wheelchair_accessible BOOLEAN DEFAULT FALSE,
    hearing_accessible BOOLEAN DEFAULT FALSE,
    visual_accessible BOOLEAN DEFAULT FALSE,
    has_elevator BOOLEAN DEFAULT FALSE,
    has_ramp BOOLEAN DEFAULT FALSE,
    has_grab_bars BOOLEAN DEFAULT FALSE,
    has_accessible_bathroom BOOLEAN DEFAULT FALSE,
    has_accessible_kitchen BOOLEAN DEFAULT FALSE,
    has_widened_doorways BOOLEAN DEFAULT FALSE,
    ada_compliant BOOLEAN DEFAULT FALSE,
    accessibility_notes VARCHAR(500),

    last_inspection_date TIMESTAMP,
    next_inspection_due TIMESTAMP,
    is_comparable BOOLEAN NOT NULL DEFAULT FALSE,
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    FOREIGN KEY (building_id) REFERENCES buildings(building_id) ON DELETE CASCADE,

    CONSTRAINT chk_unit_type CHECK (unit_type IN (
        'SINGLE_ROOM', 'DOUBLE_ROOM', 'DORMITORY', 'STUDIO',
        'ONE_BEDROOM', 'TWO_BEDROOM', 'THREE_BEDROOM', 'FOUR_BEDROOM_PLUS',
        'FAMILY_UNIT', 'SCATTERED_SITE', 'BED', 'OTHER'
    )),
    CONSTRAINT chk_unit_status CHECK (status IN (
        'AVAILABLE', 'OCCUPIED', 'RESERVED', 'MAINTENANCE', 'OFFLINE', 'DECOMMISSIONED'
    )),
    CONSTRAINT chk_occupancy CHECK (current_occupancy >= 0 AND current_occupancy <= capacity)
);

-- Capacity Snapshots Table (for historical tracking)
CREATE TABLE capacity_snapshots (
    snapshot_id VARCHAR(50) PRIMARY KEY,
    project_id VARCHAR(50) NOT NULL,
    total_capacity INTEGER,
    funded_beds INTEGER,
    occupied_beds INTEGER,
    available_beds INTEGER,
    snapshot_date TIMESTAMP NOT NULL,
    captured_by VARCHAR(50) NOT NULL,
    reason VARCHAR(500),
    utilization_rate DECIMAL(5, 2),

    FOREIGN KEY (project_id) REFERENCES housing_projects(project_id) ON DELETE CASCADE
);

-- Inspection Schedules Table
CREATE TABLE inspection_schedules (
    schedule_id VARCHAR(50) PRIMARY KEY,
    project_id VARCHAR(50) NOT NULL,
    inspection_type VARCHAR(50) NOT NULL,
    scheduled_date TIMESTAMP NOT NULL,
    inspector_id VARCHAR(50),
    inspector_name VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',
    reminder_sent BOOLEAN DEFAULT FALSE,
    reminder_date TIMESTAMP,
    completed_date TIMESTAMP,
    next_due_date TIMESTAMP,
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP,

    FOREIGN KEY (project_id) REFERENCES housing_projects(project_id) ON DELETE CASCADE,

    CONSTRAINT chk_inspection_type CHECK (inspection_type IN (
        'INITIAL_INSPECTION', 'ANNUAL_INSPECTION', 'QUARTERLY_INSPECTION',
        'MONTHLY_INSPECTION', 'SPECIAL_INSPECTION', 'COMPLAINT_BASED',
        'PRE_OCCUPANCY', 'MOVE_OUT', 'QUALITY_CONTROL'
    )),
    CONSTRAINT chk_inspection_status CHECK (status IN (
        'SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'RESCHEDULED', 'OVERDUE'
    ))
);

-- Unit Inspections Table
CREATE TABLE unit_inspections (
    inspection_id VARCHAR(50) PRIMARY KEY,
    unit_id VARCHAR(50) NOT NULL,
    inspection_type VARCHAR(50) NOT NULL,
    inspection_date TIMESTAMP NOT NULL,
    inspector_name VARCHAR(255),
    result VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    habitability_score INTEGER,
    safety_compliant BOOLEAN DEFAULT TRUE,
    ada_compliant BOOLEAN DEFAULT FALSE,
    requires_repairs BOOLEAN DEFAULT FALSE,
    repair_notes VARCHAR(1000),
    evidence_url VARCHAR(500),
    evidence_encrypted BOOLEAN DEFAULT FALSE,
    next_due_date TIMESTAMP,
    notes VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_inspection_result CHECK (result IN (
        'PASSED', 'FAILED', 'CONDITIONAL_PASS', 'PENDING', 'NOT_APPLICABLE'
    )),
    CONSTRAINT chk_habitability_score CHECK (habitability_score IS NULL OR (habitability_score >= 0 AND habitability_score <= 100))
);

-- Bed-Night Usage Table
CREATE TABLE bed_night_usage (
    usage_id VARCHAR(50) PRIMARY KEY,
    unit_id VARCHAR(50) NOT NULL,
    client_id VARCHAR(50),
    entry_date TIMESTAMP NOT NULL,
    exit_date TIMESTAMP,
    usage_source VARCHAR(50) NOT NULL,
    referral_id VARCHAR(50),
    imported_by VARCHAR(50),
    imported_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_usage_source CHECK (usage_source IN (
        'SHELTER_STAY', 'CE_REFERRAL', 'MANUAL_ENTRY', 'IMPORTED'
    ))
);

-- Indexes for performance
CREATE INDEX idx_housing_projects_org ON housing_projects(organization_id);
CREATE INDEX idx_housing_projects_type ON housing_projects(project_type);
CREATE INDEX idx_housing_projects_status ON housing_projects(status);
CREATE INDEX idx_housing_projects_comparable ON housing_projects(is_comparable);

CREATE INDEX idx_project_sites_project ON project_sites(project_id);
CREATE INDEX idx_project_sites_active ON project_sites(is_active);

CREATE INDEX idx_buildings_site ON buildings(site_id);
CREATE INDEX idx_buildings_active ON buildings(is_active);

CREATE INDEX idx_housing_units_building ON housing_units(building_id);
CREATE INDEX idx_housing_units_status ON housing_units(status);
CREATE INDEX idx_housing_units_type ON housing_units(unit_type);
CREATE INDEX idx_housing_units_comparable ON housing_units(is_comparable);
CREATE INDEX idx_housing_units_inspection_due ON housing_units(next_inspection_due);

CREATE INDEX idx_capacity_snapshots_project ON capacity_snapshots(project_id);
CREATE INDEX idx_capacity_snapshots_date ON capacity_snapshots(snapshot_date);

CREATE INDEX idx_inspection_schedules_project ON inspection_schedules(project_id);
CREATE INDEX idx_inspection_schedules_date ON inspection_schedules(scheduled_date);
CREATE INDEX idx_inspection_schedules_status ON inspection_schedules(status);
CREATE INDEX idx_inspection_schedules_inspector ON inspection_schedules(inspector_id);

CREATE INDEX idx_unit_inspections_unit ON unit_inspections(unit_id);
CREATE INDEX idx_unit_inspections_date ON unit_inspections(inspection_date);
CREATE INDEX idx_unit_inspections_result ON unit_inspections(result);

CREATE INDEX idx_bed_night_usage_unit ON bed_night_usage(unit_id);
CREATE INDEX idx_bed_night_usage_client ON bed_night_usage(client_id);
CREATE INDEX idx_bed_night_usage_entry ON bed_night_usage(entry_date);
CREATE INDEX idx_bed_night_usage_exit ON bed_night_usage(exit_date);
CREATE INDEX idx_bed_night_usage_referral ON bed_night_usage(referral_id);

-- Comments for documentation
COMMENT ON TABLE housing_projects IS 'Housing projects managed by the organization with HUD compliance tracking';
COMMENT ON TABLE project_sites IS 'Physical locations of housing projects';
COMMENT ON TABLE buildings IS 'Buildings within project sites';
COMMENT ON TABLE housing_units IS 'Individual housing units (rooms, apartments, beds)';
COMMENT ON TABLE capacity_snapshots IS 'Historical tracking of project capacity and utilization';
COMMENT ON TABLE inspection_schedules IS 'Scheduled inspections for housing projects';
COMMENT ON TABLE unit_inspections IS 'Completed inspection records for individual units';
COMMENT ON TABLE bed_night_usage IS 'Tracking of bed-night usage from shelter stays and CE referrals';
-- ============================================================================
-- V5: Housing & Financial Assistance Domain Separation
-- ============================================================================

-- Set default schema
SET search_path TO haven, public;

-- ============================================================================
-- Enums for Housing & Financial Assistance
-- ============================================================================

-- Rental assistance types
DO $$ BEGIN
    CREATE TYPE rental_assistance_type AS ENUM (
        'RRH_SHORT_TERM_RENTAL_ASSISTANCE',
        'RRH_MEDIUM_TERM_RENTAL_ASSISTANCE',
        'TH_TEMPORARY_HOUSING',
        'TH_SUPPORTIVE_SERVICES',
        'PSH_RENTAL_ASSISTANCE',
        'PSH_SUPPORTIVE_SERVICES',
        'ESG_RENTAL_ASSISTANCE',
        'ESG_RENTAL_APPLICATION_FEES',
        'ESG_SECURITY_DEPOSITS',
        'ESG_UTILITY_DEPOSITS',
        'ESG_UTILITY_PAYMENTS',
        'ESG_MOVING_COSTS',
        'HOME_TENANT_BASED_RENTAL_ASSISTANCE',
        'UTILITY_ASSISTANCE',
        'SECURITY_DEPOSIT_ASSISTANCE',
        'FIRST_MONTH_RENT',
        'LAST_MONTH_RENT',
        'MOVING_COSTS_ASSISTANCE',
        'APPLICATION_FEE_ASSISTANCE'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Financial assistance types
DO $$ BEGIN
    CREATE TYPE financial_assistance_type AS ENUM (
        'EMERGENCY_CASH_ASSISTANCE',
        'EMERGENCY_FOOD_ASSISTANCE',
        'EMERGENCY_CLOTHING',
        'EMERGENCY_MEDICAL_COSTS',
        'TRANSPORTATION_ASSISTANCE',
        'BUS_PASSES',
        'GAS_VOUCHERS',
        'CAR_REPAIR_ASSISTANCE',
        'EMPLOYMENT_ASSISTANCE',
        'JOB_TRAINING_COSTS',
        'WORK_UNIFORMS',
        'CHILDCARE_FOR_EMPLOYMENT',
        'EDUCATION_ASSISTANCE',
        'SCHOOL_SUPPLIES',
        'TUITION_ASSISTANCE',
        'LEGAL_FEES',
        'COURT_COSTS',
        'DOCUMENT_FEES',
        'MEDICAL_COPAYS',
        'PRESCRIPTION_ASSISTANCE',
        'MENTAL_HEALTH_SERVICES',
        'DENTAL_CARE',
        'CHILDCARE_ASSISTANCE',
        'CHILD_SUPPORT_PAYMENTS',
        'FAMILY_REUNIFICATION_COSTS',
        'HOUSEHOLD_GOODS',
        'FURNITURE_ASSISTANCE',
        'APPLIANCE_ASSISTANCE',
        'PHONE_SERVICE',
        'INTERNET_SERVICE',
        'COMPUTER_ASSISTANCE',
        'OTHER_ASSISTANCE'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Assistance status
DO $$ BEGIN
    CREATE TYPE assistance_status AS ENUM (
        'REQUESTED',
        'PENDING_APPROVAL',
        'APPROVED',
        'UNIT_ASSIGNED',
        'VENDOR_ASSIGNED',
        'ACTIVE',
        'COMPLETED',
        'TERMINATED',
        'DENIED',
        'CANCELLED'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Approval status
DO $$ BEGIN
    CREATE TYPE approval_status AS ENUM (
        'PENDING',
        'APPROVED',
        'REJECTED'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Approval type
DO $$ BEGIN
    CREATE TYPE approval_type AS ENUM (
        'APPROVED',
        'REJECTED'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Landlord status
DO $$ BEGIN
    CREATE TYPE landlord_status AS ENUM (
        'PENDING_VERIFICATION',
        'APPROVED',
        'SUSPENDED',
        'TERMINATED',
        'INACTIVE'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Unit status
DO $$ BEGIN
    CREATE TYPE unit_status AS ENUM (
        'AVAILABLE',
        'INSPECTION_PENDING',
        'INSPECTION_PASSED',
        'INSPECTION_FAILED',
        'OCCUPIED',
        'UNAVAILABLE',
        'CONDEMNED'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Inspection result
DO $$ BEGIN
    CREATE TYPE inspection_result AS ENUM (
        'PASS',
        'CONDITIONAL_PASS',
        'FAIL',
        'INCOMPLETE'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Payment status
DO $$ BEGIN
    CREATE TYPE payment_status AS ENUM (
        'AUTHORIZED',
        'PROCESSED',
        'FAILED',
        'CANCELLED'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- ============================================================================
-- Funding Sources Table
-- ============================================================================

CREATE TABLE IF NOT EXISTS funding_sources (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    funding_source_code VARCHAR(50) UNIQUE NOT NULL,
    funding_source_name VARCHAR(255) NOT NULL,
    funding_type VARCHAR(100) NOT NULL, -- ESG, CoC, HOME, VASH, etc.
    total_allocation DECIMAL(12,2) NOT NULL,
    remaining_balance DECIMAL(12,2) NOT NULL,
    availability_start_date DATE,
    availability_end_date DATE,
    allowed_assistance_types TEXT[], -- JSON array of allowed types
    eligibility_criteria TEXT[],
    max_monthly_amount DECIMAL(10,2),
    max_duration_months INTEGER,
    requires_two_person_approval BOOLEAN DEFAULT true,
    grant_number VARCHAR(100),
    administered_by VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- Landlords Table
-- ============================================================================

CREATE TABLE IF NOT EXISTS landlords (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    landlord_id VARCHAR(50) UNIQUE NOT NULL,
    business_name VARCHAR(255) NOT NULL,
    contact_person_name VARCHAR(200),
    contact_title VARCHAR(100),
    business_address_line1 VARCHAR(255),
    business_address_line2 VARCHAR(255),
    business_city VARCHAR(100),
    business_state VARCHAR(50),
    business_postal_code VARCHAR(20),
    business_country VARCHAR(100) DEFAULT 'USA',
    tax_id VARCHAR(50),
    status landlord_status DEFAULT 'PENDING_VERIFICATION',
    is_approved BOOLEAN DEFAULT false,
    approval_date DATE,
    approved_by UUID REFERENCES users(id),
    payment_preference VARCHAR(50), -- DIRECT_DEPOSIT, CHECK, etc.
    banking_information TEXT, -- Encrypted
    accepts_housing_vouchers BOOLEAN DEFAULT false,
    specializations TEXT[], -- Senior housing, accessible units, etc.
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- Rental Units Table
-- ============================================================================

CREATE TABLE IF NOT EXISTS rental_units (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    unit_id VARCHAR(50) UNIQUE NOT NULL,
    landlord_id UUID REFERENCES landlords(id),
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(50) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(100) DEFAULT 'USA',
    market_rent DECIMAL(10,2),
    assisted_rent DECIMAL(10,2),
    bedrooms INTEGER,
    bathrooms DECIMAL(3,1),
    square_footage DECIMAL(8,2),
    unit_type VARCHAR(50), -- APARTMENT, SINGLE_FAMILY_HOME, etc.
    status unit_status DEFAULT 'AVAILABLE',
    lease_start_date DATE,
    lease_end_date DATE,
    property_manager_contact VARCHAR(255),
    is_accessible BOOLEAN DEFAULT false,
    accessibility_features TEXT[],
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- Housing Assistance Table
-- ============================================================================

CREATE TABLE IF NOT EXISTS housing_assistance (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_id UUID NOT NULL REFERENCES clients(id),
    enrollment_id UUID NOT NULL REFERENCES program_enrollments(id),
    assistance_type rental_assistance_type NOT NULL,
    status assistance_status DEFAULT 'REQUESTED',
    requested_amount DECIMAL(10,2) NOT NULL,
    approved_amount DECIMAL(10,2),
    requested_duration_months INTEGER,
    approved_duration_months INTEGER,
    justification TEXT,
    requested_by UUID REFERENCES users(id),
    
    -- Approval workflow
    approval_level VARCHAR(100),
    required_approval_count INTEGER,
    funding_source_code VARCHAR(50) REFERENCES funding_sources(funding_source_code),
    
    -- Unit and lease management
    assigned_unit_id UUID REFERENCES rental_units(id),
    lease_start_date DATE,
    lease_end_date DATE,
    monthly_rent DECIMAL(10,2),
    landlord_id UUID REFERENCES landlords(id),
    
    -- Payment tracking
    total_paid DECIMAL(10,2) DEFAULT 0.00,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 1
);

-- ============================================================================
-- Financial Assistance Table
-- ============================================================================

CREATE TABLE IF NOT EXISTS financial_assistance (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_id UUID NOT NULL REFERENCES clients(id),
    enrollment_id UUID NOT NULL REFERENCES program_enrollments(id),
    assistance_type financial_assistance_type NOT NULL,
    status assistance_status DEFAULT 'REQUESTED',
    requested_amount DECIMAL(10,2) NOT NULL,
    approved_amount DECIMAL(10,2),
    purpose VARCHAR(500) NOT NULL,
    justification TEXT,
    requested_by UUID REFERENCES users(id),
    is_emergency BOOLEAN DEFAULT false,
    
    -- Approval workflow
    approval_level VARCHAR(100),
    required_approval_count INTEGER,
    funding_source_code VARCHAR(50) REFERENCES funding_sources(funding_source_code),
    
    -- Vendor/Payee information
    vendor_id VARCHAR(100),
    vendor_name VARCHAR(255),
    vendor_type VARCHAR(50), -- DIRECT_PAYMENT, VOUCHER, REIMBURSEMENT
    
    -- Payment tracking
    total_paid DECIMAL(10,2) DEFAULT 0.00,
    payment_due_date DATE,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 1
);

-- ============================================================================
-- Approval Chains Table
-- ============================================================================

CREATE TABLE IF NOT EXISTS approval_chains (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type VARCHAR(50) NOT NULL, -- 'HOUSING_ASSISTANCE', 'FINANCIAL_ASSISTANCE'
    entity_id UUID NOT NULL,
    status approval_status DEFAULT 'PENDING',
    required_approval_level VARCHAR(100),
    required_approval_count INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- Approvals Table
-- ============================================================================

CREATE TABLE IF NOT EXISTS approvals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    approval_chain_id UUID NOT NULL REFERENCES approval_chains(id) ON DELETE CASCADE,
    approver_id UUID NOT NULL REFERENCES users(id),
    approver_role VARCHAR(100),
    approver_name VARCHAR(200),
    approval_type approval_type NOT NULL,
    notes TEXT,
    approved_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- Inspection Records Table
-- ============================================================================

CREATE TABLE IF NOT EXISTS inspection_records (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    unit_id UUID NOT NULL REFERENCES rental_units(id) ON DELETE CASCADE,
    inspection_date DATE NOT NULL,
    inspector_id UUID REFERENCES users(id),
    inspector_name VARCHAR(200),
    inspection_type VARCHAR(50), -- INITIAL, ANNUAL, COMPLAINT, etc.
    result inspection_result,
    overall_notes TEXT,
    next_inspection_due DATE,
    meets_fair_market_rent BOOLEAN,
    meets_hqs BOOLEAN, -- Housing Quality Standards
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- Inspection Deficiencies Table
-- ============================================================================

CREATE TABLE IF NOT EXISTS inspection_deficiencies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    inspection_id UUID NOT NULL REFERENCES inspection_records(id) ON DELETE CASCADE,
    category VARCHAR(100), -- Electrical, Plumbing, HVAC, etc.
    description TEXT NOT NULL,
    severity VARCHAR(20), -- CRITICAL, MAJOR, MINOR
    is_resolved BOOLEAN DEFAULT false,
    resolved_by VARCHAR(200),
    resolution_notes TEXT,
    resolved_date DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- Assistance Payments Table
-- ============================================================================

CREATE TABLE IF NOT EXISTS assistance_payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    payment_id UUID UNIQUE NOT NULL,
    assistance_type VARCHAR(20) NOT NULL, -- 'HOUSING' or 'FINANCIAL'
    assistance_id UUID NOT NULL, -- References housing_assistance or financial_assistance
    amount DECIMAL(10,2) NOT NULL,
    payment_date DATE NOT NULL,
    payment_type VARCHAR(100), -- RENT, DEPOSIT, UTILITIES, etc.
    payment_method VARCHAR(50),
    payee_id VARCHAR(100),
    payee_name VARCHAR(255),
    funding_source_code VARCHAR(50) REFERENCES funding_sources(funding_source_code),
    authorized_by UUID REFERENCES users(id),
    status payment_status DEFAULT 'AUTHORIZED',
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- Landlord Contact Methods Table
-- ============================================================================

CREATE TABLE IF NOT EXISTS landlord_contacts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    landlord_id UUID NOT NULL REFERENCES landlords(id) ON DELETE CASCADE,
    contact_type VARCHAR(50) NOT NULL, -- PHONE, EMAIL, FAX
    contact_value VARCHAR(255) NOT NULL,
    is_primary BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- Landlord Notes Table
-- ============================================================================

CREATE TABLE IF NOT EXISTS landlord_notes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    landlord_id UUID NOT NULL REFERENCES landlords(id) ON DELETE CASCADE,
    note_type VARCHAR(50),
    content TEXT NOT NULL,
    author_id UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- Remove housing/financial assistance from service episodes
-- ============================================================================

-- Update service episode types to remove housing and financial assistance
-- These are now separate domains
UPDATE service_episodes 
SET service_type = 'OTHER' 
WHERE service_type IN ('HOUSING_ASSISTANCE', 'FINANCIAL_ASSISTANCE');

-- ============================================================================
-- Indexes for Performance
-- ============================================================================

-- Funding sources indexes
CREATE INDEX IF NOT EXISTS idx_funding_sources_code ON funding_sources(funding_source_code);
CREATE INDEX IF NOT EXISTS idx_funding_sources_type ON funding_sources(funding_type);

-- Landlords indexes
CREATE INDEX IF NOT EXISTS idx_landlords_id ON landlords(landlord_id);
CREATE INDEX IF NOT EXISTS idx_landlords_status ON landlords(status);
CREATE INDEX IF NOT EXISTS idx_landlords_approved ON landlords(is_approved);

-- Rental units indexes
CREATE INDEX IF NOT EXISTS idx_rental_units_id ON rental_units(unit_id);
CREATE INDEX IF NOT EXISTS idx_rental_units_landlord ON rental_units(landlord_id);
CREATE INDEX IF NOT EXISTS idx_rental_units_status ON rental_units(status);
CREATE INDEX IF NOT EXISTS idx_rental_units_location ON rental_units(city, state);

-- Housing assistance indexes
CREATE INDEX IF NOT EXISTS idx_housing_assistance_client ON housing_assistance(client_id);
CREATE INDEX IF NOT EXISTS idx_housing_assistance_enrollment ON housing_assistance(enrollment_id);
CREATE INDEX IF NOT EXISTS idx_housing_assistance_status ON housing_assistance(status);
CREATE INDEX IF NOT EXISTS idx_housing_assistance_type ON housing_assistance(assistance_type);

-- Financial assistance indexes
CREATE INDEX IF NOT EXISTS idx_financial_assistance_client ON financial_assistance(client_id);
CREATE INDEX IF NOT EXISTS idx_financial_assistance_enrollment ON financial_assistance(enrollment_id);
CREATE INDEX IF NOT EXISTS idx_financial_assistance_status ON financial_assistance(status);
CREATE INDEX IF NOT EXISTS idx_financial_assistance_emergency ON financial_assistance(is_emergency) WHERE is_emergency = true;

-- Approval chains indexes
CREATE INDEX IF NOT EXISTS idx_approval_chains_entity ON approval_chains(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_approval_chains_status ON approval_chains(status);

-- Payments indexes
CREATE INDEX IF NOT EXISTS idx_assistance_payments_assistance ON assistance_payments(assistance_type, assistance_id);
CREATE INDEX IF NOT EXISTS idx_assistance_payments_date ON assistance_payments(payment_date);
CREATE INDEX IF NOT EXISTS idx_assistance_payments_funding ON assistance_payments(funding_source_code);

-- Inspection indexes
CREATE INDEX IF NOT EXISTS idx_inspection_records_unit ON inspection_records(unit_id);
CREATE INDEX IF NOT EXISTS idx_inspection_records_date ON inspection_records(inspection_date);
CREATE INDEX IF NOT EXISTS idx_inspection_deficiencies_inspection ON inspection_deficiencies(inspection_id);

-- ============================================================================
-- Triggers
-- ============================================================================

-- Update funding source balance when payments are made
CREATE OR REPLACE FUNCTION update_funding_source_balance()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'PROCESSED' AND OLD.status != 'PROCESSED' THEN
        -- Deduct from funding source when payment is processed
        UPDATE funding_sources 
        SET remaining_balance = remaining_balance - NEW.amount,
            updated_at = CURRENT_TIMESTAMP
        WHERE funding_source_code = NEW.funding_source_code;
    ELSIF OLD.status = 'PROCESSED' AND NEW.status != 'PROCESSED' THEN
        -- Add back to funding source if payment is reversed
        UPDATE funding_sources 
        SET remaining_balance = remaining_balance + OLD.amount,
            updated_at = CURRENT_TIMESTAMP
        WHERE funding_source_code = OLD.funding_source_code;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS update_funding_source_balance_trigger ON assistance_payments;
CREATE TRIGGER update_funding_source_balance_trigger
    AFTER UPDATE ON assistance_payments
    FOR EACH ROW EXECUTE FUNCTION update_funding_source_balance();

-- Apply updated_at triggers to new tables
DROP TRIGGER IF EXISTS update_funding_sources_updated_at ON funding_sources;
CREATE TRIGGER update_funding_sources_updated_at BEFORE UPDATE ON funding_sources
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_landlords_updated_at ON landlords;
CREATE TRIGGER update_landlords_updated_at BEFORE UPDATE ON landlords
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_rental_units_updated_at ON rental_units;
CREATE TRIGGER update_rental_units_updated_at BEFORE UPDATE ON rental_units
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_housing_assistance_updated_at ON housing_assistance;
CREATE TRIGGER update_housing_assistance_updated_at BEFORE UPDATE ON housing_assistance
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_financial_assistance_updated_at ON financial_assistance;
CREATE TRIGGER update_financial_assistance_updated_at BEFORE UPDATE ON financial_assistance
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- Views for Reporting
-- ============================================================================

-- Active assistance view
CREATE OR REPLACE VIEW active_assistance AS
SELECT 
    'HOUSING' as assistance_category,
    ha.id,
    ha.client_id,
    ha.enrollment_id,
    ha.assistance_type::text as assistance_type,
    ha.status,
    ha.approved_amount,
    ha.total_paid,
    ha.created_at,
    c.first_name,
    c.last_name,
    c.client_number
FROM housing_assistance ha
JOIN clients c ON ha.client_id = c.id
WHERE ha.status IN ('APPROVED', 'UNIT_ASSIGNED', 'ACTIVE')

UNION ALL

SELECT 
    'FINANCIAL' as assistance_category,
    fa.id,
    fa.client_id,
    fa.enrollment_id,
    fa.assistance_type::text as assistance_type,
    fa.status,
    fa.approved_amount,
    fa.total_paid,
    fa.created_at,
    c.first_name,
    c.last_name,
    c.client_number
FROM financial_assistance fa
JOIN clients c ON fa.client_id = c.id
WHERE fa.status IN ('APPROVED', 'VENDOR_ASSIGNED', 'ACTIVE');

-- Pending approvals view
CREATE OR REPLACE VIEW pending_approvals AS
SELECT 
    ac.id as approval_chain_id,
    ac.entity_type,
    ac.entity_id,
    ac.required_approval_count,
    COUNT(a.id) as current_approval_count,
    ac.required_approval_count - COUNT(a.id) as approvals_needed,
    ac.created_at
FROM approval_chains ac
LEFT JOIN approvals a ON ac.id = a.approval_chain_id AND a.approval_type = 'APPROVED'
WHERE ac.status = 'PENDING'
GROUP BY ac.id, ac.entity_type, ac.entity_id, ac.required_approval_count, ac.created_at;

-- ============================================================================
-- Comments for Documentation
-- ============================================================================

COMMENT ON TABLE housing_assistance IS 'RRH/TH housing assistance with approval workflows and unit management';
COMMENT ON TABLE financial_assistance IS 'Non-housing financial assistance with emergency processing';
COMMENT ON TABLE funding_sources IS 'Funding sources with restrictions and balance tracking';
COMMENT ON TABLE landlords IS 'Landlord engagement and approval tracking';
COMMENT ON TABLE rental_units IS 'Rental units with inspection and lease management';
COMMENT ON TABLE approval_chains IS 'Two-person integrity approval workflows';
COMMENT ON TABLE assistance_payments IS 'Payment tracking by funding source with restrictions';
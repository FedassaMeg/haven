-- V6: Safety Assessment Domain Schema
-- Creates tables for safety planning, lethality assessment, contact preferences, and location rules

-- =====================================================================================
-- SAFETY ASSESSMENTS - Main aggregate table
-- =====================================================================================
CREATE TABLE IF NOT EXISTS safety_assessments (
    safety_assessment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'INITIATED',
    overall_risk_level VARCHAR(20) NOT NULL DEFAULT 'MINIMAL',
    primary_case_manager_id VARCHAR(255) NOT NULL,
    supervising_manager_id VARCHAR(255),
    next_review_due DATE,
    requires_immediate_intervention BOOLEAN DEFAULT false,
    confidentiality_level VARCHAR(50) NOT NULL DEFAULT 'RESTRICTED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_safety_assessments_client 
        FOREIGN KEY (client_id) REFERENCES clients(id),
    CONSTRAINT chk_safety_assessment_status 
        CHECK (status IN ('INITIATED', 'IN_PROGRESS', 'COMPLETED', 'UNDER_REVIEW', 'EXPIRED')),
    CONSTRAINT chk_safety_risk_level 
        CHECK (overall_risk_level IN ('MINIMAL', 'LOW', 'MODERATE', 'HIGH', 'EXTREME')),
    CONSTRAINT chk_safety_confidentiality_level 
        CHECK (confidentiality_level IN ('PUBLIC', 'RESTRICTED', 'CONFIDENTIAL', 'TOP_SECRET'))
);

-- =====================================================================================
-- SAFETY PLANS - Safety planning with confidential history
-- =====================================================================================
CREATE TABLE IF NOT EXISTS safety_plans (
    safety_plan_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    current_version_id UUID NOT NULL,
    safety_assessment_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    plan_version_number INTEGER NOT NULL DEFAULT 1,
    
    -- Core safety planning elements
    warning_signs_of_danger TEXT[],
    coping_strategies TEXT[],
    safe_location_plan TEXT,
    children_safety_plan TEXT,
    workplace_safety_plan TEXT,
    transportation_plan TEXT,
    important_documents_to_gather TEXT[],
    financial_safety_plan TEXT[],
    legal_safety_plan TEXT,
    emotional_safety_plan TEXT,
    
    -- Plan metadata
    developed_by VARCHAR(255) NOT NULL,
    developed_with TEXT, -- Client involvement level
    last_review_date DATE,
    next_review_due DATE,
    review_notes TEXT,
    
    -- Confidentiality and access
    confidentiality_level VARCHAR(50) NOT NULL DEFAULT 'RESTRICTED',
    is_client_copy_provided BOOLEAN DEFAULT false,
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_safety_plans_assessment 
        FOREIGN KEY (safety_assessment_id) REFERENCES safety_assessments(safety_assessment_id),
    CONSTRAINT chk_safety_plan_status 
        CHECK (status IN ('DRAFT', 'ACTIVE', 'NEEDS_REVIEW', 'SUPERSEDED', 'ARCHIVED')),
    CONSTRAINT chk_safety_plan_confidentiality 
        CHECK (confidentiality_level IN ('PUBLIC', 'RESTRICTED', 'CONFIDENTIAL', 'TOP_SECRET'))
);

-- =====================================================================================
-- SAFETY PLAN ACCESS CONTROL - Who can view/edit safety plans
-- =====================================================================================
CREATE TABLE IF NOT EXISTS safety_plan_authorized_viewers (
    safety_plan_id UUID NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    authorized_by VARCHAR(255) NOT NULL,
    authorized_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (safety_plan_id, user_id),
    CONSTRAINT fk_safety_plan_viewers_plan 
        FOREIGN KEY (safety_plan_id) REFERENCES safety_plans(safety_plan_id) ON DELETE CASCADE
);

-- =====================================================================================
-- SAFETY CONTACTS - Emergency and support contacts within safety plans
-- =====================================================================================
CREATE TABLE IF NOT EXISTS safety_contacts (
    contact_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    safety_plan_id UUID NOT NULL,
    contact_type VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    relationship VARCHAR(255),
    phone_number VARCHAR(50),
    alternate_phone VARCHAR(50),
    email VARCHAR(255),
    address TEXT,
    is_available_24_hour BOOLEAN DEFAULT false,
    special_instructions TEXT,
    can_provide_transportation BOOLEAN DEFAULT false,
    can_provide_emergency_housing BOOLEAN DEFAULT false,
    
    CONSTRAINT fk_safety_contacts_plan 
        FOREIGN KEY (safety_plan_id) REFERENCES safety_plans(safety_plan_id) ON DELETE CASCADE,
    CONSTRAINT chk_safety_contact_type 
        CHECK (contact_type IN ('EMERGENCY', 'SUPPORT'))
);

-- =====================================================================================
-- LETHALITY ASSESSMENTS - Standardized DV risk assessments
-- =====================================================================================
CREATE TABLE IF NOT EXISTS lethality_assessments (
    assessment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    safety_assessment_id UUID NOT NULL,
    tool_used VARCHAR(50) NOT NULL,
    calculated_risk_level VARCHAR(20) NOT NULL,
    total_score INTEGER,
    assessor_id VARCHAR(255) NOT NULL,
    assessor_role VARCHAR(50) NOT NULL,
    assessment_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    immediate_risk_factors TEXT,
    recommendations TEXT,
    requires_immediate_intervention BOOLEAN DEFAULT false,
    
    CONSTRAINT fk_lethality_assessments_safety 
        FOREIGN KEY (safety_assessment_id) REFERENCES safety_assessments(safety_assessment_id),
    CONSTRAINT chk_lethality_tool 
        CHECK (tool_used IN ('ODARA', 'DANGER_ASSESSMENT', 'LAP', 'CUSTOM_DV_SCREENING')),
    CONSTRAINT chk_lethality_risk_level 
        CHECK (calculated_risk_level IN ('MINIMAL', 'LOW', 'MODERATE', 'HIGH', 'EXTREME'))
);

-- =====================================================================================
-- LETHALITY ASSESSMENT RESPONSES - Individual question responses
-- =====================================================================================
CREATE TABLE IF NOT EXISTS lethality_assessment_responses (
    response_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id UUID NOT NULL,
    question_id VARCHAR(100) NOT NULL,
    response_value TEXT NOT NULL,
    response_type VARCHAR(20) NOT NULL DEFAULT 'BOOLEAN',
    
    CONSTRAINT fk_lethality_responses_assessment 
        FOREIGN KEY (assessment_id) REFERENCES lethality_assessments(assessment_id) ON DELETE CASCADE,
    CONSTRAINT chk_response_type 
        CHECK (response_type IN ('BOOLEAN', 'TEXT', 'INTEGER', 'SCALE'))
);

-- =====================================================================================
-- CONTACT SAFETY PREFERENCES - Communication security preferences
-- =====================================================================================
CREATE TABLE IF NOT EXISTS contact_safety_preferences (
    preferences_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    safety_assessment_id UUID NOT NULL,
    client_id UUID NOT NULL,
    safety_level VARCHAR(50) NOT NULL DEFAULT 'STANDARD',
    version_number INTEGER NOT NULL DEFAULT 1,
    
    -- Contact restrictions
    no_home_visits BOOLEAN DEFAULT false,
    no_workplace_contact BOOLEAN DEFAULT false,
    no_voicemails BOOLEAN DEFAULT false,
    no_text_messages BOOLEAN DEFAULT false,
    require_encrypted_communication BOOLEAN DEFAULT false,
    
    -- Code words and special instructions
    use_code_words BOOLEAN DEFAULT false,
    code_word_for_safe VARCHAR(100),
    code_word_for_danger VARCHAR(100),
    special_instructions TEXT,
    
    -- Emergency override settings
    allow_emergency_override BOOLEAN DEFAULT true,
    
    -- Metadata
    last_updated_by VARCHAR(255) NOT NULL,
    last_updated_by_role VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_contact_preferences_assessment 
        FOREIGN KEY (safety_assessment_id) REFERENCES safety_assessments(safety_assessment_id),
    CONSTRAINT fk_contact_preferences_client 
        FOREIGN KEY (client_id) REFERENCES clients(id),
    CONSTRAINT chk_contact_safety_level 
        CHECK (safety_level IN ('STANDARD', 'ELEVATED', 'HIGH_RISK', 'EMERGENCY_ONLY'))
);

-- =====================================================================================
-- APPROVED CONTACT METHODS - Safe communication methods
-- =====================================================================================
CREATE TABLE IF NOT EXISTS approved_contact_methods (
    preferences_id UUID NOT NULL,
    contact_method VARCHAR(50) NOT NULL,
    method_details TEXT,
    added_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (preferences_id, contact_method),
    CONSTRAINT fk_approved_methods_preferences 
        FOREIGN KEY (preferences_id) REFERENCES contact_safety_preferences(preferences_id) ON DELETE CASCADE,
    CONSTRAINT chk_contact_method 
        CHECK (contact_method IN ('SECURE_PHONE', 'SECURE_EMAIL', 'OFFICE_VISIT', 
                                  'DESIGNATED_LOCATION', 'INTERMEDIARY_CONTACT', 'ENCRYPTED_MESSAGE'))
);

-- =====================================================================================
-- SAFE CONTACT INFORMATION - Approved phone numbers and emails
-- =====================================================================================
CREATE TABLE IF NOT EXISTS safe_contact_information (
    preferences_id UUID NOT NULL,
    contact_type VARCHAR(20) NOT NULL,
    contact_value VARCHAR(255) NOT NULL,
    label VARCHAR(100),
    is_primary BOOLEAN DEFAULT false,
    added_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (preferences_id, contact_type, contact_value),
    CONSTRAINT fk_safe_contact_preferences 
        FOREIGN KEY (preferences_id) REFERENCES contact_safety_preferences(preferences_id) ON DELETE CASCADE,
    CONSTRAINT chk_safe_contact_type 
        CHECK (contact_type IN ('PHONE', 'EMAIL'))
);

-- =====================================================================================
-- UNSAFE CONTACT INFORMATION - Numbers/emails to avoid
-- =====================================================================================
CREATE TABLE IF NOT EXISTS unsafe_contact_information (
    preferences_id UUID NOT NULL,
    contact_type VARCHAR(20) NOT NULL,
    contact_value VARCHAR(255) NOT NULL,
    reason TEXT,
    added_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (preferences_id, contact_type, contact_value),
    CONSTRAINT fk_unsafe_contact_preferences 
        FOREIGN KEY (preferences_id) REFERENCES contact_safety_preferences(preferences_id) ON DELETE CASCADE,
    CONSTRAINT chk_unsafe_contact_type 
        CHECK (contact_type IN ('PHONE', 'EMAIL', 'LOCATION'))
);

-- =====================================================================================
-- SAFE CONTACT TIME WINDOWS - When it's safe to contact client
-- =====================================================================================
CREATE TABLE IF NOT EXISTS safe_contact_time_windows (
    preferences_id UUID NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    safe_days_of_week VARCHAR(20)[] NOT NULL,
    time_zone VARCHAR(50),
    notes TEXT,
    
    PRIMARY KEY (preferences_id),
    CONSTRAINT fk_time_windows_preferences 
        FOREIGN KEY (preferences_id) REFERENCES contact_safety_preferences(preferences_id) ON DELETE CASCADE
);

-- =====================================================================================
-- CONFIDENTIAL LOCATION RULES - Address/location confidentiality
-- =====================================================================================
CREATE TABLE IF NOT EXISTS confidential_location_rules (
    rules_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    safety_assessment_id UUID NOT NULL,
    client_id UUID NOT NULL,
    confidentiality_level VARCHAR(50) NOT NULL DEFAULT 'RESTRICTED',
    version_number INTEGER NOT NULL DEFAULT 1,
    
    -- Disclosure rules
    allow_partial_address_sharing BOOLEAN DEFAULT false,
    allow_general_area_sharing BOOLEAN DEFAULT false,
    allow_no_address_sharing BOOLEAN DEFAULT false,
    
    -- Emergency access
    allow_emergency_access BOOLEAN DEFAULT true,
    emergency_access_protocol TEXT,
    
    -- Geographic restrictions
    restricted_radius DECIMAL(10,2), -- Miles from a specific point
    restricted_radius_center TEXT, -- Address or coordinates
    
    -- Legal and compliance
    legal_basis_for_confidentiality TEXT,
    confidentiality_expiration_date DATE,
    requires_court_order_to_disclose BOOLEAN DEFAULT false,
    
    -- Metadata
    established_by VARCHAR(255) NOT NULL,
    established_by_role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_location_rules_assessment 
        FOREIGN KEY (safety_assessment_id) REFERENCES safety_assessments(safety_assessment_id),
    CONSTRAINT fk_location_rules_client 
        FOREIGN KEY (client_id) REFERENCES clients(id),
    CONSTRAINT chk_location_confidentiality_level 
        CHECK (confidentiality_level IN ('PUBLIC', 'RESTRICTED', 'CONFIDENTIAL', 'TOP_SECRET'))
);

-- =====================================================================================
-- PROTECTED LOCATIONS - Specific addresses under protection
-- =====================================================================================
CREATE TABLE IF NOT EXISTS protected_locations (
    location_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rules_id UUID NOT NULL,
    address TEXT NOT NULL,
    location_type VARCHAR(50) NOT NULL,
    protection_reason TEXT NOT NULL,
    protection_start_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    protection_expiry_date DATE,
    is_active BOOLEAN DEFAULT true,
    
    CONSTRAINT fk_protected_locations_rules 
        FOREIGN KEY (rules_id) REFERENCES confidential_location_rules(rules_id) ON DELETE CASCADE,
    CONSTRAINT chk_location_type 
        CHECK (location_type IN ('HOME_ADDRESS', 'WORK_ADDRESS', 'SHELTER_ADDRESS', 
                                 'FAMILY_ADDRESS', 'SAFE_HOUSE', 'SERVICE_LOCATION', 'OTHER'))
);

-- =====================================================================================
-- LOCATION ACCESS PERMISSIONS - User-specific location access rights
-- =====================================================================================
CREATE TABLE IF NOT EXISTS location_access_permissions (
    rules_id UUID NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    permission_level VARCHAR(20) NOT NULL,
    granted_by VARCHAR(255) NOT NULL,
    granted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (rules_id, user_id),
    CONSTRAINT fk_location_permissions_rules 
        FOREIGN KEY (rules_id) REFERENCES confidential_location_rules(rules_id) ON DELETE CASCADE,
    CONSTRAINT chk_location_permission_level 
        CHECK (permission_level IN ('FULL_ACCESS', 'PARTIAL_ACCESS', 'GENERAL_ACCESS', 'NO_ACCESS'))
);

-- =====================================================================================
-- LOCATION ACCESS AUDIT TRAIL - Track all location access attempts
-- =====================================================================================
CREATE TABLE IF NOT EXISTS location_access_audit (
    audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rules_id UUID NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    user_role VARCHAR(50) NOT NULL,
    access_action VARCHAR(50) NOT NULL,
    location_or_reason TEXT NOT NULL,
    performed_by VARCHAR(255) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_location_audit_rules 
        FOREIGN KEY (rules_id) REFERENCES confidential_location_rules(rules_id) ON DELETE CASCADE,
    CONSTRAINT chk_location_access_action 
        CHECK (access_action IN ('ACCESS_ATTEMPT', 'USER_AUTHORIZED', 'ACCESS_REVOKED', 
                                 'EMERGENCY_ACCESS', 'PERMISSION_GRANTED'))
);

-- =====================================================================================
-- SAFETY ALERTS - Active safety alerts for clients
-- =====================================================================================
CREATE TABLE IF NOT EXISTS safety_alerts (
    alert_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    safety_assessment_id UUID NOT NULL,
    client_id UUID NOT NULL,
    alert_message TEXT NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cleared_by VARCHAR(255),
    cleared_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT true,
    
    CONSTRAINT fk_safety_alerts_assessment 
        FOREIGN KEY (safety_assessment_id) REFERENCES safety_assessments(safety_assessment_id),
    CONSTRAINT fk_safety_alerts_client 
        FOREIGN KEY (client_id) REFERENCES clients(id),
    CONSTRAINT chk_safety_alert_type 
        CHECK (alert_type IN ('GENERAL', 'HIGH_RISK', 'IMMEDIATE_DANGER', 'CONTACT_RESTRICTION', 
                              'LOCATION_CONFIDENTIAL', 'EMERGENCY'))
);

-- =====================================================================================
-- SAFETY NOTIFICATIONS - Notification history and status
-- =====================================================================================
CREATE TABLE IF NOT EXISTS safety_notifications (
    notification_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    safety_assessment_id UUID NOT NULL,
    client_id UUID NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    urgency VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP WITH TIME ZONE,
    acknowledged_by VARCHAR(255),
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    escalated BOOLEAN DEFAULT false,
    escalation_scheduled_at TIMESTAMP WITH TIME ZONE,
    escalation_delay_minutes INTEGER,
    
    CONSTRAINT fk_safety_notifications_assessment 
        FOREIGN KEY (safety_assessment_id) REFERENCES safety_assessments(safety_assessment_id),
    CONSTRAINT fk_safety_notifications_client 
        FOREIGN KEY (client_id) REFERENCES clients(id),
    CONSTRAINT chk_notification_type 
        CHECK (notification_type IN ('EMERGENCY_ALERT', 'IMMEDIATE_INTERVENTION', 'RISK_ESCALATION',
                                     'RISK_CHANGE', 'SAFETY_PLAN_UPDATE', 'LETHALITY_ASSESSMENT_COMPLETED',
                                     'OVERDUE_REVIEW', 'CONTACT_RESTRICTION', 'LOCATION_CONFIDENTIALITY',
                                     'WORKFLOW_ACTION_OVERDUE')),
    CONSTRAINT chk_notification_urgency 
        CHECK (urgency IN ('EMERGENCY', 'HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT chk_notification_status 
        CHECK (status IN ('PENDING', 'SENT', 'ACKNOWLEDGED', 'ESCALATED', 'EXPIRED'))
);

-- =====================================================================================
-- NOTIFICATION RECIPIENTS - Who receives each notification
-- =====================================================================================
CREATE TABLE IF NOT EXISTS notification_recipients (
    notification_id UUID NOT NULL,
    recipient_id VARCHAR(255) NOT NULL,
    delivery_method VARCHAR(50) NOT NULL,
    delivered_at TIMESTAMP WITH TIME ZONE,
    delivery_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    
    PRIMARY KEY (notification_id, recipient_id, delivery_method),
    CONSTRAINT fk_notification_recipients 
        FOREIGN KEY (notification_id) REFERENCES safety_notifications(notification_id) ON DELETE CASCADE,
    CONSTRAINT chk_delivery_method 
        CHECK (delivery_method IN ('IMMEDIATE_PHONE_CALL', 'SMS_TEXT', 'EMAIL_URGENT', 'EMAIL',
                                   'SYSTEM_ALERT', 'PUSH_NOTIFICATION', 'PAGER')),
    CONSTRAINT chk_delivery_status 
        CHECK (delivery_status IN ('PENDING', 'DELIVERED', 'FAILED', 'ACKNOWLEDGED'))
);

-- =====================================================================================
-- SAFETY WORKFLOW ACTIONS - Workflow actions triggered by safety assessments
-- =====================================================================================
CREATE TABLE IF NOT EXISTS safety_workflow_actions (
    action_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    safety_assessment_id UUID NOT NULL,
    client_id UUID NOT NULL,
    action_type VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    assigned_to VARCHAR(255) NOT NULL,
    max_response_time_minutes INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    completed_by VARCHAR(255),
    completion_notes TEXT,
    
    CONSTRAINT fk_workflow_actions_assessment 
        FOREIGN KEY (safety_assessment_id) REFERENCES safety_assessments(safety_assessment_id),
    CONSTRAINT fk_workflow_actions_client 
        FOREIGN KEY (client_id) REFERENCES clients(id),
    CONSTRAINT chk_workflow_action_status 
        CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'ESCALATED', 'OVERDUE', 'CANCELLED'))
);

-- =====================================================================================
-- INDEXES for Performance
-- =====================================================================================

-- Safety Assessments
CREATE INDEX IF NOT EXISTS idx_safety_assessments_client_id ON safety_assessments(client_id);
CREATE INDEX IF NOT EXISTS idx_safety_assessments_status ON safety_assessments(status);
CREATE INDEX IF NOT EXISTS idx_safety_assessments_risk_level ON safety_assessments(overall_risk_level);
CREATE INDEX IF NOT EXISTS idx_safety_assessments_review_due ON safety_assessments(next_review_due);
CREATE INDEX IF NOT EXISTS idx_safety_assessments_case_manager ON safety_assessments(primary_case_manager_id);

-- Safety Plans
CREATE INDEX IF NOT EXISTS idx_safety_plans_assessment_id ON safety_plans(safety_assessment_id);
CREATE INDEX IF NOT EXISTS idx_safety_plans_status ON safety_plans(status);
CREATE INDEX IF NOT EXISTS idx_safety_plans_version ON safety_plans(plan_version_number);

-- Lethality Assessments
CREATE INDEX IF NOT EXISTS idx_lethality_assessments_safety_id ON lethality_assessments(safety_assessment_id);
CREATE INDEX IF NOT EXISTS idx_lethality_assessments_risk_level ON lethality_assessments(calculated_risk_level);
CREATE INDEX IF NOT EXISTS idx_lethality_assessments_date ON lethality_assessments(assessment_date);
CREATE INDEX IF NOT EXISTS idx_lethality_assessments_assessor ON lethality_assessments(assessor_id);

-- Contact Safety Preferences
CREATE INDEX IF NOT EXISTS idx_contact_preferences_assessment_id ON contact_safety_preferences(safety_assessment_id);
CREATE INDEX IF NOT EXISTS idx_contact_preferences_client_id ON contact_safety_preferences(client_id);
CREATE INDEX IF NOT EXISTS idx_contact_preferences_safety_level ON contact_safety_preferences(safety_level);

-- Location Rules
CREATE INDEX IF NOT EXISTS idx_location_rules_assessment_id ON confidential_location_rules(safety_assessment_id);
CREATE INDEX IF NOT EXISTS idx_location_rules_client_id ON confidential_location_rules(client_id);
CREATE INDEX IF NOT EXISTS idx_location_rules_confidentiality ON confidential_location_rules(confidentiality_level);

-- Safety Alerts
CREATE INDEX IF NOT EXISTS idx_safety_alerts_assessment_id ON safety_alerts(safety_assessment_id);
CREATE INDEX IF NOT EXISTS idx_safety_alerts_client_id ON safety_alerts(client_id);
CREATE INDEX IF NOT EXISTS idx_safety_alerts_active ON safety_alerts(is_active);
CREATE INDEX IF NOT EXISTS idx_safety_alerts_type ON safety_alerts(alert_type);

-- Safety Notifications
CREATE INDEX IF NOT EXISTS idx_safety_notifications_assessment_id ON safety_notifications(safety_assessment_id);
CREATE INDEX IF NOT EXISTS idx_safety_notifications_client_id ON safety_notifications(client_id);
CREATE INDEX IF NOT EXISTS idx_safety_notifications_status ON safety_notifications(status);
CREATE INDEX IF NOT EXISTS idx_safety_notifications_urgency ON safety_notifications(urgency);
CREATE INDEX IF NOT EXISTS idx_safety_notifications_created_at ON safety_notifications(created_at);

-- Workflow Actions
CREATE INDEX IF NOT EXISTS idx_workflow_actions_assessment_id ON safety_workflow_actions(safety_assessment_id);
CREATE INDEX IF NOT EXISTS idx_workflow_actions_client_id ON safety_workflow_actions(client_id);
CREATE INDEX IF NOT EXISTS idx_workflow_actions_assigned_to ON safety_workflow_actions(assigned_to);
CREATE INDEX IF NOT EXISTS idx_workflow_actions_status ON safety_workflow_actions(status);
CREATE INDEX IF NOT EXISTS idx_workflow_actions_created_at ON safety_workflow_actions(created_at);

-- Audit trails
CREATE INDEX IF NOT EXISTS idx_location_audit_rules_id ON location_access_audit(rules_id);
CREATE INDEX IF NOT EXISTS idx_location_audit_user_id ON location_access_audit(user_id);
CREATE INDEX IF NOT EXISTS idx_location_audit_timestamp ON location_access_audit(timestamp);

-- =====================================================================================
-- COMMENTS for documentation
-- =====================================================================================
COMMENT ON TABLE safety_assessments IS 'Main safety assessment aggregate containing overall risk assessment and safety coordination';
COMMENT ON TABLE safety_plans IS 'Confidential safety plans with version control and role-based access';
COMMENT ON TABLE lethality_assessments IS 'Evidence-based lethality assessments using standardized tools (ODARA, DA, LAP)';
COMMENT ON TABLE contact_safety_preferences IS 'Client communication safety preferences and restrictions';
COMMENT ON TABLE confidential_location_rules IS 'Location confidentiality and address protection rules';
COMMENT ON TABLE safety_alerts IS 'Active safety alerts requiring staff attention';
COMMENT ON TABLE safety_notifications IS 'Safety notification system with escalation and acknowledgment tracking';
COMMENT ON TABLE safety_workflow_actions IS 'Safety-driven workflow actions with timing and completion tracking';
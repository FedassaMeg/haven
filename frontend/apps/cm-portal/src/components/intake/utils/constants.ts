/**
 * VAWA-Compliant Intake Workflow Constants
 * Version: 2.0
 *
 * This module defines constants, HUD codes, HMIS enums, and configuration values
 * for the 10-step intake workflow.
 */

import type {
  ReferralSource,
  HmisParticipationStatus,
  LethalityScreeningTool,
  RiskLevel,
  PoliceInvolvementStatus,
  ProtectiveOrderStatus,
  HomelessCategory,
  LengthOfStay,
  IncomeSource,
  HouseholdType,
  GoalCategory,
  ProjectType,
  EnrollmentStatus,
  VeteranStatus,
  DisablingCondition,
  DataQuality,
} from './types';

// =============================================================================
// HUD CODES (24 CFR Part 578)
// =============================================================================

/**
 * HUD Homeless Definition Categories (24 CFR �578.3)
 */
export const HOMELESS_CATEGORIES: Record<HomelessCategory, string> = {
  '1': 'Category 1: Literally Homeless',
  '2': 'Category 2: Imminent Risk of Homelessness',
  '3': 'Category 3: Homeless Under Other Federal Statutes',
  '4': 'Category 4: Fleeing/Attempting to Flee Domestic Violence',
  'NOT_HOMELESS': 'Not Homeless',
};

/**
 * HUD Prior Living Situation Codes (HMIS Data Standards)
 */
export const PRIOR_LIVING_SITUATIONS = {
  EMERGENCY_SHELTER: { code: '101', label: 'Emergency shelter, including hotel or motel paid for with emergency shelter voucher' },
  TRANSITIONAL_HOUSING: { code: '116', label: 'Transitional housing for homeless persons (including homeless youth)' },
  PLACE_NOT_MEANT_FOR_HABITATION: { code: '118', label: 'Place not meant for habitation (e.g., a vehicle, an abandoned building, bus/train/subway station/airport, outside)' },
  SAFE_HAVEN: { code: '215', label: 'Safe Haven' },
  INTERIM_HOUSING: { code: '206', label: 'Interim housing' },
  HOTEL_MOTEL_NO_VOUCHER: { code: '207', label: 'Hotel or motel paid for without emergency shelter voucher' },
  STAYING_WITH_FAMILY_TEMPORARY: { code: '225', label: 'Staying or living in a family member\'s room, apartment or house' },
  STAYING_WITH_FRIENDS_TEMPORARY: { code: '226', label: 'Staying or living in a friend\'s room, apartment or house' },
  OWNED_BY_CLIENT: { code: '312', label: 'Owned by client, with ongoing housing subsidy' },
  OWNED_BY_CLIENT_NO_SUBSIDY: { code: '313', label: 'Owned by client, no ongoing housing subsidy' },
  RENTAL_NO_SUBSIDY: { code: '314', label: 'Rental by client, no ongoing housing subsidy' },
  RENTAL_WITH_SUBSIDY: { code: '329', label: 'Rental by client, with ongoing housing subsidy (other than RRH)' },
  PSYCHIATRIC_HOSPITAL: { code: '332', label: 'Psychiatric hospital or other psychiatric facility' },
  SUBSTANCE_ABUSE_TREATMENT: { code: '333', label: 'Substance abuse treatment facility or detox center' },
  HOSPITAL_NON_PSYCHIATRIC: { code: '334', label: 'Hospital or other residential non-psychiatric medical facility' },
  JAIL_PRISON: { code: '335', label: 'Jail, prison or juvenile detention facility' },
  FOSTER_CARE_HOME: { code: '336', label: 'Foster care home or foster care group home' },
  LONG_TERM_CARE: { code: '410', label: 'Long-term care facility or nursing home' },
  RESIDENTIAL_PROJECT: { code: '421', label: 'Residential project or halfway house with no homeless criteria' },
  HOST_HOME_FAMILY: { code: '422', label: 'Host Home (non-crisis)' },
  OTHER: { code: '435', label: 'Other' },
  CLIENT_DOESNT_KNOW: { code: '8', label: 'Client doesn\'t know' },
  CLIENT_REFUSED: { code: '9', label: 'Client refused' },
  DATA_NOT_COLLECTED: { code: '99', label: 'Data not collected' },
} as const;

/**
 * HUD Project Types (HMIS Data Standards)
 */
export const PROJECT_TYPES: Record<ProjectType, string> = {
  ES: 'Emergency Shelter',
  TH: 'Transitional Housing',
  RRH: 'Rapid Re-Housing',
  PSH: 'Permanent Supportive Housing',
  SO: 'Safe Haven',
  PH: 'Other Permanent Housing',
  DAY: 'Day Shelter',
  SSO: 'Services Only',
  HP: 'Homelessness Prevention',
  CE: 'Coordinated Entry',
};

/**
 * HUD Relationship to Head of Household Codes
 */
export const RELATIONSHIP_TO_HOH = {
  SELF: { code: '1', label: 'Self (head of household)' },
  HEAD_OF_HOUSEHOLD_SPOUSE: { code: '2', label: 'Head of household\'s spouse or partner' },
  HEAD_OF_HOUSEHOLD_CHILD: { code: '3', label: 'Head of household\'s child' },
  HEAD_OF_HOUSEHOLD_OTHER_RELATION: { code: '4', label: 'Head of household\'s other relation member (other relation to HoH)' },
  OTHER_NON_RELATION: { code: '5', label: 'Other: non-relation member' },
} as const;

/**
 * HUD Race Codes (Multi-select)
 */
export const RACE_CODES = {
  AMERICAN_INDIAN_ALASKA_NATIVE: { code: '1', label: 'American Indian, Alaska Native, or Indigenous' },
  ASIAN: { code: '2', label: 'Asian or Asian American' },
  BLACK_AFRICAN_AMERICAN: { code: '3', label: 'Black, African American, or African' },
  NATIVE_HAWAIIAN_PACIFIC_ISLANDER: { code: '4', label: 'Native Hawaiian or Pacific Islander' },
  WHITE: { code: '5', label: 'White' },
  CLIENT_DOESNT_KNOW: { code: '8', label: 'Client doesn\'t know' },
  CLIENT_REFUSED: { code: '9', label: 'Client prefers not to answer' },
  DATA_NOT_COLLECTED: { code: '99', label: 'Data not collected' },
} as const;

/**
 * HUD Ethnicity Codes
 */
export const ETHNICITY_CODES = {
  NON_HISPANIC_LATINO: { code: '0', label: 'Non-Hispanic/Non-Latin(a)(o)(x)' },
  HISPANIC_LATINO: { code: '1', label: 'Hispanic/Latin(a)(o)(x)' },
  CLIENT_DOESNT_KNOW: { code: '8', label: 'Client doesn\'t know' },
  CLIENT_REFUSED: { code: '9', label: 'Client prefers not to answer' },
  DATA_NOT_COLLECTED: { code: '99', label: 'Data not collected' },
} as const;

/**
 * HUD Gender Codes (Multi-select for HMIS)
 */
export const GENDER_CODES = {
  FEMALE: { code: '0', label: 'Woman/Girl' },
  MALE: { code: '1', label: 'Man/Boy' },
  NON_BINARY: { code: '2', label: 'Non-Binary' },
  CULTURALLY_SPECIFIC: { code: '3', label: 'Culturally Specific Identity (e.g., Two-Spirit)' },
  TRANSGENDER: { code: '4', label: 'Transgender' },
  QUESTIONING: { code: '5', label: 'Questioning' },
  DIFFERENT_IDENTITY: { code: '6', label: 'Different Identity' },
  CLIENT_DOESNT_KNOW: { code: '8', label: 'Client doesn\'t know' },
  CLIENT_REFUSED: { code: '9', label: 'Client prefers not to answer' },
  DATA_NOT_COLLECTED: { code: '99', label: 'Data not collected' },
} as const;

// =============================================================================
// HMIS ENUMS
// =============================================================================

export const HMIS_PARTICIPATION_STATUS: Record<HmisParticipationStatus, string> = {
  PARTICIPATING: 'Full HMIS Participation',
  NON_PARTICIPATING: 'Declined HMIS',
  PENDING: 'Decision Pending',
  VAWA_EXEMPT: 'VAWA Confidentiality Protection',
};

export const VETERAN_STATUS: Record<VeteranStatus, string> = {
  YES: 'Yes',
  NO: 'No',
  CLIENT_DOESNT_KNOW: 'Client doesn\'t know',
  CLIENT_REFUSED: 'Client refused',
  DATA_NOT_COLLECTED: 'Data not collected',
};

export const DISABLING_CONDITION: Record<DisablingCondition, string> = {
  YES: 'Yes',
  NO: 'No',
  CLIENT_DOESNT_KNOW: 'Client doesn\'t know',
  CLIENT_REFUSED: 'Client refused',
  DATA_NOT_COLLECTED: 'Data not collected',
};

export const DATA_QUALITY: Record<DataQuality, string> = {
  1: 'Full, verified',
  2: 'Partial or approximate',
  8: 'Client doesn\'t know',
  9: 'Client refused',
  99: 'Data not collected',
};

export const ENROLLMENT_STATUS: Record<EnrollmentStatus, string> = {
  ACTIVE: 'Active - Enrolled and receiving services',
  PENDING_APPROVAL: 'Pending Approval',
  WAITLISTED: 'Waitlisted',
  DENIED: 'Enrollment Denied',
  WITHDRAWN: 'Client Withdrew Application',
};

// =============================================================================
// REFERRAL SOURCES
// =============================================================================

export const REFERRAL_SOURCES: Record<ReferralSource, string> = {
  SELF: 'Client Self-Referred',
  HOTLINE: 'DV Hotline Referral',
  AGENCY: 'Partner Agency Referral',
  OUTREACH: 'Outreach Program',
  COURT: 'Court-Ordered Services',
  LAW_ENFORCEMENT: 'Law Enforcement Referral',
  HEALTHCARE: 'Healthcare Provider',
  SHELTER: 'Emergency Shelter',
  OTHER: 'Other',
};

// =============================================================================
// RISK ASSESSMENT TOOLS & LEVELS
// =============================================================================

export const LETHALITY_SCREENING_TOOLS: Record<LethalityScreeningTool, string> = {
  DANGER_ASSESSMENT: 'Danger Assessment (Jacquelyn Campbell)',
  ODARA: 'Ontario Domestic Assault Risk Assessment (ODARA)',
  DVSI: 'Domestic Violence Screening Instrument (DVSI)',
  LAP: 'Lethality Assessment Program (LAP)',
  MOSAIC: 'MOSAIC Threat Assessment',
  OTHER: 'Other Validated Tool',
  NONE: 'No Formal Tool Used',
};

export const RISK_LEVELS: Record<RiskLevel, { label: string; color: string; description: string }> = {
  MINIMAL: {
    label: 'Minimal',
    color: 'green',
    description: 'No risk indicators present',
  },
  LOW: {
    label: 'Low',
    color: 'blue',
    description: '1-2 risk indicators',
  },
  MODERATE: {
    label: 'Moderate',
    color: 'yellow',
    description: '3-5 risk indicators',
  },
  HIGH: {
    label: 'High',
    color: 'orange',
    description: '6-8 risk indicators',
  },
  SEVERE: {
    label: 'Severe',
    color: 'red',
    description: '9+ indicators or immediate threat',
  },
  NOT_ASSESSED: {
    label: 'Not Assessed',
    color: 'gray',
    description: 'Assessment not yet completed',
  },
};

export const POLICE_INVOLVEMENT_STATUS: Record<PoliceInvolvementStatus, string> = {
  NONE: 'No Police Involvement',
  REPORT_FILED: 'Report Filed (No Active Case)',
  ONGOING_CASE: 'Active Criminal Case',
  CHARGES_PENDING: 'Charges Filed, Pending Court',
  RESTRAINING_ORDER: 'Criminal Restraining Order in Place',
  UNKNOWN: 'Client Unsure or Declined to Answer',
};

export const PROTECTIVE_ORDER_STATUS: Record<ProtectiveOrderStatus, string> = {
  NONE: 'No Protective Order',
  TEMPORARY: 'Temporary/Emergency Order in Place',
  PERMANENT: 'Permanent Order Granted',
  EXPIRED: 'Order Expired',
  PENDING: 'Application Pending',
  VIOLATED: 'Order in Place but Violated',
  UNKNOWN: 'Client Unsure',
};

// =============================================================================
// INCOME SOURCES
// =============================================================================

export const INCOME_SOURCES: Record<IncomeSource, string> = {
  EMPLOYMENT: 'Employment Income',
  SSI: 'Supplemental Security Income (SSI)',
  SSDI: 'Social Security Disability Insurance (SSDI)',
  TANF: 'Temporary Assistance for Needy Families (TANF)',
  UNEMPLOYMENT: 'Unemployment Benefits',
  CHILD_SUPPORT: 'Child Support',
  ALIMONY: 'Alimony or Spousal Support',
  PENSION: 'Pension or Retirement Income',
  VETERANS_BENEFITS: 'Veterans Benefits',
  WORKERS_COMP: 'Workers Compensation',
  OTHER: 'Other Income',
  NONE: 'No Income',
};

export const LENGTH_OF_STAY: Record<LengthOfStay, string> = {
  ONE_NIGHT_OR_LESS: 'One night or less',
  TWO_TO_SIX_NIGHTS: '2 to 6 nights',
  ONE_WEEK_TO_ONE_MONTH: 'One week to one month',
  ONE_TO_THREE_MONTHS: '1 to 3 months',
  THREE_TO_SIX_MONTHS: '3 to 6 months',
  SIX_TO_TWELVE_MONTHS: '6 to 12 months',
  MORE_THAN_A_YEAR: 'More than a year',
  UNKNOWN: 'Unknown',
};

// =============================================================================
// HOUSEHOLD TYPES
// =============================================================================

export const HOUSEHOLD_TYPES: Record<HouseholdType, string> = {
  SINGLE_ADULT: 'Single Adult',
  SINGLE_ADULT_WITH_CHILDREN: 'Single Adult with Children',
  COUPLE_NO_CHILDREN: 'Couple without Children',
  COUPLE_WITH_CHILDREN: 'Couple with Children',
  MULTI_GENERATIONAL: 'Multi-Generational Household',
  OTHER: 'Other Household Type',
};

// =============================================================================
// HOUSING BARRIERS
// =============================================================================

export const HOUSING_BARRIERS = {
  EVICTION_HISTORY: 'Eviction History',
  RENTAL_DEBT: 'Rental or Utility Debt',
  POOR_CREDIT: 'Poor Credit or Collections',
  CRIMINAL_BACKGROUND: 'Criminal Background',
  NO_INCOME: 'No Income',
  INSUFFICIENT_INCOME: 'Insufficient Income',
  NO_RENTAL_HISTORY: 'No Rental History',
  NO_REFERENCES: 'No Personal/Professional References',
  NO_IDENTIFICATION: 'No Government-Issued ID',
  NO_SOCIAL_SECURITY: 'No Social Security Card',
  PET_OWNERSHIP: 'Pet Ownership',
  TRANSPORTATION: 'Lack of Transportation',
  CHILDCARE: 'Lack of Childcare',
  LANGUAGE_BARRIER: 'Language Barrier',
  DISABILITY: 'Disability Accommodation Needs',
  MENTAL_HEALTH: 'Mental Health Condition',
  SUBSTANCE_USE: 'Substance Use Disorder',
  DOMESTIC_VIOLENCE: 'Fleeing Domestic Violence',
  DISCRIMINATION: 'History of Housing Discrimination',
  FAMILY_SIZE: 'Large Family Size',
} as const;

export type HousingBarrier = keyof typeof HOUSING_BARRIERS;

// =============================================================================
// CLIENT STRENGTHS (Empowerment Focus)
// =============================================================================

export const CLIENT_STRENGTHS = {
  RESILIENCE: 'Demonstrates resilience in difficult situations',
  MOTIVATION: 'Highly motivated to achieve goals',
  SUPPORT_NETWORK: 'Strong family or friend support network',
  EMPLOYMENT_HISTORY: 'Positive employment history',
  PARENTING_SKILLS: 'Strong parenting skills',
  COMMUNICATION: 'Excellent communication skills',
  PROBLEM_SOLVING: 'Good problem-solving abilities',
  ORGANIZATION: 'Organized and detail-oriented',
  ADVOCACY: 'Able to self-advocate effectively',
  EDUCATION: 'Completed education or training',
  WORK_ETHIC: 'Strong work ethic',
  BUDGETING: 'Good budgeting/financial management',
  CONFLICT_RESOLUTION: 'Conflict resolution skills',
  CULTURAL_CONNECTIONS: 'Strong cultural or community connections',
  CREATIVE: 'Creative and resourceful',
  COMPASSIONATE: 'Compassionate and empathetic',
  LEADERSHIP: 'Natural leadership abilities',
  TECHNICAL_SKILLS: 'Technical or vocational skills',
  MULTILINGUAL: 'Multilingual abilities',
  RECOVERY: 'Committed to recovery/sobriety',
} as const;

export type ClientStrength = keyof typeof CLIENT_STRENGTHS;

// =============================================================================
// GOAL CATEGORIES
// =============================================================================

export const GOAL_CATEGORIES: Record<GoalCategory, string> = {
  HOUSING_SEARCH: 'Housing Search & Placement',
  EMPLOYMENT: 'Employment & Job Training',
  EDUCATION: 'Education & Skills Development',
  COUNSELING: 'Counseling & Mental Health Services',
  BENEFITS_APPLICATION: 'Benefits Application & Enrollment',
  LEGAL_ADVOCACY: 'Legal Advocacy & Support',
  HEALTH_CARE: 'Health Care & Medical Services',
  CHILDCARE: 'Childcare & Parenting Support',
  TRANSPORTATION: 'Transportation Assistance',
  FINANCIAL_STABILITY: 'Financial Stability & Budgeting',
  SAFETY_PLANNING: 'Safety Planning',
  LIFE_SKILLS: 'Life Skills & Independent Living',
  OTHER: 'Other',
};

// =============================================================================
// DOCUMENT TAGS (Access Control)
// =============================================================================

export const DOCUMENT_TAGS = {
  PUBLIC: 'public',
  CONFIDENTIAL: 'confidential',
  VAWA_PROTECTED: 'vawa-protected',
  CONSENT_FORM: 'consent-form',
  IDENTITY_DOCUMENT: 'identity-document',
  INCOME_VERIFICATION: 'income-verification',
  MEDICAL_RECORD: 'medical-record',
  LEGAL_DOCUMENT: 'legal-document',
  PROTECTIVE_ORDER: 'protective-order',
  HMIS_SHAREABLE: 'hmis-shareable',
  HMIS_RESTRICTED: 'hmis-restricted',
  AUTO_DELETE: 'auto-delete',
  PERMANENT_RECORD: 'permanent-record',
} as const;

export type DocumentTag = (typeof DOCUMENT_TAGS)[keyof typeof DOCUMENT_TAGS];

/**
 * Document retention periods (in days)
 */
export const DOCUMENT_RETENTION = {
  CONSENT_FORMS: 2555, // 7 years
  IDENTITY_DOCUMENTS: 90, // 90 days after verification
  TEMPORARY_UPLOADS: 30, // 30 days
  LEGAL_DOCUMENTS: 3650, // 10 years
  SERVICE_RECORDS: 2555, // 7 years
  PROTECTIVE_ORDERS: 3650, // 10 years
  GENERAL_CORRESPONDENCE: 365, // 1 year
} as const;

// =============================================================================
// WORKFLOW STEP DEFINITIONS
// =============================================================================

export interface WorkflowStep {
  id: number;
  name: string;
  title: string;
  description: string;
  vawaCompliant: boolean;
  required: boolean;
  estimatedMinutes: number;
}

export const WORKFLOW_STEPS: WorkflowStep[] = [
  {
    id: 1,
    name: 'initialContact',
    title: 'Initial Contact / Referral Intake',
    description: 'Capture referral source and initial safety assessment without collecting identifying information',
    vawaCompliant: true,
    required: true,
    estimatedMinutes: 10,
  },
  {
    id: 2,
    name: 'safetyConsent',
    title: 'Safety & Consent Check',
    description: 'Establish safe contact methods and obtain informed consent before proceeding',
    vawaCompliant: true,
    required: true,
    estimatedMinutes: 15,
  },
  {
    id: 3,
    name: 'riskAssessment',
    title: 'Crisis / Risk Assessment',
    description: 'Conduct lethality screening and immediate safety assessment',
    vawaCompliant: true,
    required: true,
    estimatedMinutes: 20,
  },
  {
    id: 4,
    name: 'eligibility',
    title: 'Eligibility & Program Match',
    description: 'Determine program eligibility based on homeless status, income, and household composition',
    vawaCompliant: false,
    required: true,
    estimatedMinutes: 15,
  },
  {
    id: 5,
    name: 'housingBarriers',
    title: 'Housing Barrier Assessment',
    description: 'Identify barriers to housing stability and generate support plan',
    vawaCompliant: false,
    required: true,
    estimatedMinutes: 20,
  },
  {
    id: 6,
    name: 'servicePlan',
    title: 'Service Plan & Case Assignment',
    description: 'Assign case manager, establish goals, and create follow-up schedule',
    vawaCompliant: false,
    required: true,
    estimatedMinutes: 25,
  },
  {
    id: 7,
    name: 'documentation',
    title: 'Documentation Uploads',
    description: 'Upload required consent forms and supporting documents',
    vawaCompliant: true,
    required: true,
    estimatedMinutes: 10,
  },
  {
    id: 8,
    name: 'demographics',
    title: 'Demographics & Outcome Baseline',
    description: 'Collect full identifying information and HMIS data elements',
    vawaCompliant: false,
    required: true,
    estimatedMinutes: 15,
  },
  {
    id: 9,
    name: 'enrollment',
    title: 'Service Enrollment Confirmation',
    description: 'Confirm program enrollment and create HMIS entry',
    vawaCompliant: false,
    required: true,
    estimatedMinutes: 10,
  },
  {
    id: 10,
    name: 'followUp',
    title: 'Follow-Up / Continuous Reassessment',
    description: 'Configure ongoing reassessment schedule and audit protection',
    vawaCompliant: false,
    required: false,
    estimatedMinutes: 5,
  },
];

/**
 * Total estimated intake time
 */
export const TOTAL_ESTIMATED_MINUTES = WORKFLOW_STEPS.reduce(
  (sum, step) => sum + step.estimatedMinutes,
  0
);

// =============================================================================
// FEATURE FLAGS
// =============================================================================

export const FEATURE_FLAGS = {
  /** Enable digital signature capture */
  ENABLE_DIGITAL_SIGNATURES: true,

  /** Enable automatic risk assessment routing */
  ENABLE_AUTO_RISK_ROUTING: true,

  /** Enable HMIS real-time integration */
  ENABLE_HMIS_INTEGRATION: false,

  /** Enable SMS notifications */
  ENABLE_SMS_NOTIFICATIONS: true,

  /** Enable client portal access */
  ENABLE_CLIENT_PORTAL: false,

  /** Enable multi-language support */
  ENABLE_MULTI_LANGUAGE: true,

  /** Enable document OCR scanning */
  ENABLE_DOCUMENT_OCR: false,

  /** Enable AI-powered program matching */
  ENABLE_AI_MATCHING: false,

  /** Strict VAWA compliance mode */
  STRICT_VAWA_MODE: true,

  /** Enable audit trail export */
  ENABLE_AUDIT_EXPORT: true,

  /** Enable waitlist management */
  ENABLE_WAITLIST: true,

  /** Enable coordinated entry integration */
  ENABLE_COORDINATED_ENTRY: false,
} as const;

export type FeatureFlag = keyof typeof FEATURE_FLAGS;

// =============================================================================
// DEFAULT VALUES
// =============================================================================

export const DEFAULTS = {
  /** Default HMIS participation status */
  HMIS_PARTICIPATION: 'PENDING' as HmisParticipationStatus,

  /** Default risk level */
  RISK_LEVEL: 'NOT_ASSESSED' as RiskLevel,

  /** Default data quality */
  DATA_QUALITY: 99 as DataQuality,

  /** Default follow-up reminder days before */
  REMINDER_DAYS_BEFORE: 7,

  /** Default reassessment interval (days) */
  REASSESSMENT_INTERVAL: 90,

  /** Default audit log retention (days) */
  AUDIT_LOG_RETENTION: 2555, // 7 years

  /** Default document expiration (days) */
  DOCUMENT_EXPIRATION: 90,

  /** Minimum age for head of household */
  MIN_HOH_AGE: 18,

  /** Maximum caseload per case manager */
  MAX_CASELOAD: 25,

  /** Emergency contact timeout (hours) */
  EMERGENCY_CONTACT_TIMEOUT: 24,

  /** Session timeout (minutes) */
  SESSION_TIMEOUT: 30,

  /** Auto-save interval (seconds) */
  AUTO_SAVE_INTERVAL: 60,

  /** Workflow version */
  WORKFLOW_VERSION: '2.0',
} as const;

// =============================================================================
// VALIDATION RULES
// =============================================================================

export const VALIDATION = {
  /** Phone number regex (US format) */
  PHONE_REGEX: /^(\+?1)?[\s.-]?\(?([0-9]{3})\)?[\s.-]?([0-9]{3})[\s.-]?([0-9]{4})$/,

  /** Email regex */
  EMAIL_REGEX: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,

  /** SSN regex (with or without dashes) */
  SSN_REGEX: /^(?!000|666)[0-8][0-9]{2}-?(?!00)[0-9]{2}-?(?!0000)[0-9]{4}$/,

  /** ZIP code regex (5 or 9 digit) */
  ZIP_REGEX: /^[0-9]{5}(-[0-9]{4})?$/,

  /** Min client alias length */
  MIN_ALIAS_LENGTH: 2,

  /** Max client alias length */
  MAX_ALIAS_LENGTH: 50,

  /** Max aspirations text length */
  MAX_ASPIRATIONS_LENGTH: 1000,

  /** Max goal description length */
  MAX_GOAL_DESCRIPTION_LENGTH: 500,

  /** Max safety notes length */
  MAX_SAFETY_NOTES_LENGTH: 2000,

  /** Min code word length */
  MIN_CODE_WORD_LENGTH: 4,

  /** Max file upload size (bytes) - 10MB */
  MAX_FILE_SIZE: 10 * 1024 * 1024,

  /** Allowed file types for uploads */
  ALLOWED_FILE_TYPES: [
    'application/pdf',
    'image/jpeg',
    'image/png',
    'image/gif',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  ],
} as const;

// =============================================================================
// UI CONSTANTS
// =============================================================================

export const UI = {
  /** Colors for risk levels */
  RISK_COLORS: {
    MINIMAL: '#10b981',
    LOW: '#3b82f6',
    MODERATE: '#f59e0b',
    HIGH: '#f97316',
    SEVERE: '#ef4444',
    NOT_ASSESSED: '#6b7280',
  },

  /** Toast notification durations (ms) */
  TOAST_DURATION: {
    SUCCESS: 3000,
    ERROR: 5000,
    WARNING: 4000,
    INFO: 3000,
  },

  /** Loading states */
  LOADING_MESSAGES: [
    'Loading intake data...',
    'Validating information...',
    'Calculating eligibility...',
    'Generating service plan...',
    'Saving your progress...',
  ],

  /** Empty states */
  EMPTY_STATES: {
    NO_CLIENTS: 'No clients found',
    NO_DOCUMENTS: 'No documents uploaded',
    NO_GOALS: 'No goals defined yet',
    NO_APPOINTMENTS: 'No upcoming appointments',
    NO_TASKS: 'All tasks completed',
  },
} as const;

// =============================================================================
// SECURITY CONSTANTS
// =============================================================================

export const SECURITY = {
  /** VAWA confidentiality flag */
  VAWA_FLAG: 'VAWA_CONFIDENTIAL',

  /** Document encryption algorithm */
  ENCRYPTION_ALGORITHM: 'AES-256-GCM',

  /** Password minimum length */
  MIN_PASSWORD_LENGTH: 12,

  /** Maximum login attempts */
  MAX_LOGIN_ATTEMPTS: 5,

  /** Account lockout duration (minutes) */
  LOCKOUT_DURATION: 30,

  /** Required permissions for intake */
  REQUIRED_PERMISSIONS: ['intake:create', 'client:create', 'document:upload'],

  /** Sensitive field markers */
  SENSITIVE_FIELDS: [
    'socialSecurityNumber',
    'birthDate',
    'residentialAddress',
    'codeWord',
    'safePhoneNumber',
    'safeEmail',
  ],
} as const;

// =============================================================================
// API ENDPOINTS (for reference)
// =============================================================================

export const API_ENDPOINTS = {
  CREATE_INTAKE: '/api/intake',
  UPDATE_INTAKE: '/api/intake/:id',
  GET_INTAKE: '/api/intake/:id',
  UPLOAD_DOCUMENT: '/api/intake/:id/documents',
  ASSIGN_CASE_MANAGER: '/api/intake/:id/assign',
  CREATE_ENROLLMENT: '/api/intake/:id/enroll',
  VALIDATE_ELIGIBILITY: '/api/intake/:id/eligibility',
  EXPORT_AUDIT_LOG: '/api/intake/:id/audit',
} as const;

// =============================================================================
// ERROR SEVERITY LEVELS
// =============================================================================

/**
 * Error severity levels for validation
 */
export const ERROR_SEVERITY = {
  /** ERROR (Blocking) - Prevents step progression, must be resolved to continue */
  ERROR: 'ERROR',
  /** WARNING (Non-blocking) - Allows step progression, requires staff acknowledgment */
  WARNING: 'WARNING',
} as const;

export type ErrorSeverity = (typeof ERROR_SEVERITY)[keyof typeof ERROR_SEVERITY];

/**
 * Severity-specific UI styling configuration
 */
export const SEVERITY_CONFIG = {
  ERROR: {
    color: '#ef4444',
    bgColor: '#fef2f2',
    borderColor: '#fca5a5',
    icon: 'error',
    label: 'Error',
    canProgress: false,
    description: 'Prevents step progression. Must be resolved to continue.',
  },
  WARNING: {
    color: '#f59e0b',
    bgColor: '#fffbeb',
    borderColor: '#fcd34d',
    icon: 'warning',
    label: 'Warning',
    canProgress: true,
    description: 'Allows step progression. Requires staff acknowledgment.',
  },
} as const;

/**
 * Examples of ERROR (Blocking) validations
 */
export const ERROR_EXAMPLES = [
  'Missing required field',
  'Invalid data format',
  'Consent not provided',
  'Entry date in future',
  'Invalid email format',
  'Invalid phone number format',
  'SSN format invalid',
  'File size exceeds limit',
  'Required document not uploaded',
  'Staff confirmation missing',
] as const;

/**
 * Examples of WARNING (Non-blocking) validations
 */
export const WARNING_EXAMPLES = [
  'High risk client (safety routing suggested)',
  'HMIS participation declined (data sharing restricted)',
  'Missing optional field (data quality impact)',
  'Data quality concern (reporting readiness affected)',
  'No client strengths identified',
  'Emergency shelter needed',
  'Children may be unsafe (CPS consultation suggested)',
  'Criminal record identified (legal advocacy suggested)',
  'Photo release not obtained',
  'APR not ready (missing fields)',
] as const;

// =============================================================================
// ERROR MESSAGES
// =============================================================================

export const ERROR_MESSAGES = {
  // Field validation errors
  REQUIRED_FIELD: 'This field is required',
  INVALID_EMAIL: 'Please enter a valid email address',
  INVALID_PHONE: 'Please enter a valid phone number',
  INVALID_SSN: 'Please enter a valid Social Security Number',
  INVALID_ZIP: 'Please enter a valid ZIP code',
  INVALID_DATE: 'Please enter a valid date',
  FUTURE_DATE: 'Date cannot be in the future',

  // File upload errors
  FILE_TOO_LARGE: 'File size exceeds 10MB limit',
  INVALID_FILE_TYPE: 'File type not supported',
  UPLOAD_FAILED: 'Document upload failed. Please try again.',

  // Consent errors (BLOCKING)
  CONSENT_REQUIRED: 'Client consent is required to proceed',
  SIGNATURE_REQUIRED: 'Digital signature is required to confirm consent',

  // Network/system errors
  NETWORK_ERROR: 'Network error. Please check your connection.',
  SAVE_FAILED: 'Failed to save. Please try again.',
  SESSION_EXPIRED: 'Your session has expired. Please log in again.',

  // Business logic errors
  INVALID_HOUSEHOLD_SIZE: 'Total household size does not match adults + children',
  NO_CONTACT_METHOD: 'At least one safe contact method must be selected',
  NO_PROGRAM_SELECTED: 'Program selection is required',
  NO_CASE_MANAGER: 'Case manager assignment is required',
  NO_GOALS: 'At least one client goal is required',
  STAFF_CONFIRMATION_REQUIRED: 'Staff must confirm all information is reviewed and accurate',
} as const;

// =============================================================================
// WARNING MESSAGES
// =============================================================================

export const WARNING_MESSAGES = {
  // Safety warnings
  HIGH_RISK_CLIENT: 'HIGH/SEVERE risk detected. Consider immediate safety planning and emergency resources.',
  UNSAFE_CLIENT: 'Client may be in immediate danger. Consider crisis protocol routing.',
  EMERGENCY_SHELTER_NEEDED: 'Client needs emergency shelter. Prioritize immediate placement.',
  MEDICAL_NEEDS: 'Client has immediate medical needs. Coordinate medical referral.',
  CHILDREN_UNSAFE: 'Children may be unsafe. Consider CPS consultation and child protection measures.',

  // Data quality warnings
  HMIS_DECLINED: 'Client declined HMIS participation. Data sharing will be restricted.',
  VAWA_EXEMPT: 'VAWA confidentiality protections apply. Strict data access controls enforced.',
  NO_CONTACT_INFO: 'No contact information provided. Communication may be challenging.',
  NO_STRENGTHS: 'No client strengths identified. Consider empowerment-focused assessment.',
  NO_ASPIRATIONS: 'Client aspirations not documented. Consider goal-setting discussion.',

  // Eligibility warnings
  INELIGIBLE_ALL_PROGRAMS: 'Client appears ineligible for all programs. Consider alternative resources and referrals.',

  // Housing barrier warnings
  CRIMINAL_RECORD: 'Criminal record identified. Consider legal advocacy and expungement services.',
  SEX_OFFENDER: 'Client is registered sex offender. Specialized housing assistance may be required.',
  NO_TRANSPORTATION: 'Lack of reliable transportation may impact employment and housing search. Consider transportation assistance.',
  ISOLATED: 'Client is isolated from support network. Consider peer support groups and community connections.',

  // Documentation warnings
  PHOTO_RELEASE_MISSING: 'Photo/video release not obtained. Client photos cannot be used for marketing purposes.',

  // Enrollment warnings
  PENDING_APPROVAL: 'Enrollment pending approval. Ensure client is notified of expected timeline.',
  WAITLISTED: 'Client waitlisted. Provide alternative resources and expected wait time.',

  // Reporting warnings
  APR_NOT_READY: 'Annual Performance Report (APR) not ready. Missing fields: {fields}',
  CAPER_NOT_READY: 'CAPER not ready. Missing fields: {fields}',
  LOW_DATA_QUALITY: 'Data quality score ({score}%) is below 80%. Improve data completeness.',
  LOW_DATA_COMPLETENESS: 'Data completeness ({completeness}%) is below 80%. Review and fill missing information.',
} as const;

// =============================================================================
// SUCCESS MESSAGES
// =============================================================================

export const SUCCESS_MESSAGES = {
  INTAKE_CREATED: 'Intake successfully created',
  INTAKE_UPDATED: 'Intake successfully updated',
  DOCUMENT_UPLOADED: 'Document uploaded successfully',
  CASE_MANAGER_ASSIGNED: 'Case manager assigned successfully',
  ENROLLMENT_CONFIRMED: 'Client enrollment confirmed',
  CONSENT_RECORDED: 'Consent recorded successfully',
  AUTO_SAVED: 'Progress automatically saved',
  STEP_COMPLETED: 'Step completed successfully',
  VALIDATION_PASSED: 'All validations passed',
} as const;

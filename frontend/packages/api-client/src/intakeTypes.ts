/**
 * Intake API Types
 *
 * Shared request/response DTOs for backend intake APIs.
 * These types extend the frontend intake types and provide the contract
 * between frontend and backend for all intake-related API calls.
 */

// =============================================================================
// PRE-INTAKE CONTACT (Temp Client)
// =============================================================================

export interface CreatePreIntakeContactRequest {
  /** ISO 8601 date string for contact date */
  contactDate: string;

  /** Time of initial contact (HH:mm format) */
  contactTime: string;

  /** Source of referral */
  referralSource: string;

  /** Additional details about referral source (if OTHER selected) */
  referralSourceDetails?: string;

  /** Client alias or initials (NOT full name - collected after consent) */
  clientAlias: string;

  /** Client initials for quick reference (optional) */
  clientInitials?: string;

  /** Immediate safety flag - asked during initial contact */
  safeToContactNow: boolean;

  /** If unsafe, route to crisis protocol */
  needsImmediateCrisisIntervention: boolean;

  /** Worker who made initial contact */
  intakeWorkerName: string;
}

export interface CreatePreIntakeContactResponse {
  /** Temporary client ID (UUID or similar) */
  tempClientId: string;

  /** Timestamp when record was created */
  createdAt: string;

  /** Expiration date for temp record (30 days from creation) */
  expiresAt: string;
}

export interface UpdatePreIntakeContactRequest {
  /** Partial update to temp client data */
  contactDate?: string;
  contactTime?: string;
  referralSource?: string;
  referralSourceDetails?: string;
  clientAlias?: string;
  safeToContactNow?: boolean;
  needsImmediateCrisisIntervention?: boolean;
}

export interface GetPreIntakeContactResponse {
  tempClientId: string;
  contactDate: string;
  contactTime: string;
  referralSource: string;
  referralSourceDetails?: string;
  clientAlias: string;
  clientInitials?: string;
  safeToContactNow: boolean;
  needsImmediateCrisisIntervention: boolean;
  intakeWorkerName: string;
  createdAt: string;
  expiresAt: string;
}

// =============================================================================
// PROGRAM MATCHING
// =============================================================================

export interface MatchProgramsRequest {
  /** Homeless status data for eligibility determination */
  homelessStatus: {
    category: string;
    startDate: string;
    priorLivingSituation?: string;
    lengthOfStay?: number;
  };

  /** Income data */
  income: {
    monthlyIncome: number;
    incomeSource: string[];
    verified: boolean;
  };

  /** Household composition */
  householdComposition: {
    adults: number;
    children: number;
    totalSize: number;
    minorsUnder18: number;
    minorsUnder5: number;
  };

  /** Veteran status (affects eligibility for some programs) */
  veteranStatus?: string;

  /** Disabling condition (HUD requirement) */
  disablingCondition?: string;

  /** DV survivor status (for VAWA programs) */
  dvSurvivor?: boolean;
}

export interface ProgramSelection {
  /** Program ID */
  id: string;

  /** Program name */
  name: string;

  /** Project type (ES, TH, RRH, PSH, etc.) */
  type: string;

  /** Funding source */
  fundingSource: string;

  /** Daily rate (if applicable) */
  dailyRate?: number;

  /** Program capacity information */
  capacity: {
    total: number;
    available: number;
    waitlist: number;
  };

  /** Eligibility criteria */
  eligibilityCriteria: {
    minIncome?: number;
    maxIncome?: number;
    householdSizeMin?: number;
    householdSizeMax?: number;
    requiresDisability?: boolean;
    requiresVeteranStatus?: boolean;
    requiresDVSurvivor?: boolean;
  };
}

export interface EligibilityResults {
  /** Programs the client is eligible for */
  eligiblePrograms: string[];

  /** Programs the client is NOT eligible for (with reasons) */
  ineligiblePrograms: Array<{
    programId: string;
    programName: string;
    reasons: string[];
  }>;

  /** Recommended program (best match based on needs and capacity) */
  recommendedProgram: string | null;

  /** Confidence score for recommendation (0-100) */
  recommendationConfidence?: number;
}

export interface MatchProgramsResponse {
  /** List of eligible programs */
  eligiblePrograms: ProgramSelection[];

  /** Recommended program (null if no good match) */
  recommendedProgram: ProgramSelection | null;

  /** Detailed eligibility results */
  eligibilityResults: EligibilityResults;

  /** Timestamp when matching was performed */
  matchedAt: string;
}

export interface GetAvailableProgramsRequest {
  /** Filter by project type */
  type?: string;

  /** Filter by funding source */
  fundingSource?: string;

  /** Only show programs with available capacity */
  hasCapacity?: boolean;

  /** Include waitlisted programs */
  includeWaitlist?: boolean;
}

export interface GetAvailableProgramsResponse {
  programs: ProgramSelection[];
  total: number;
}

// =============================================================================
// ENROLLMENT CREATION
// =============================================================================

export interface CreateEnrollmentRequest {
  /** Client ID (promoted from temp client) */
  clientId: string;

  /** Project/Program ID */
  projectId: string;

  /** Entry date (24 CFR §578.103(a) compliant) */
  entryDate: string;

  /** Project type (ES, TH, RRH, PSH, etc.) */
  projectType: string;

  /** Relationship to head of household */
  relationshipToHoH: string;

  /** Funding source */
  fundingSource: string;

  /** Household ID (if part of existing household) */
  householdId?: string;

  /** Move-in date (for RRH/PSH) */
  moveInDate?: string;

  /** Cost allocation data */
  costAllocation?: {
    dailyRate: number;
    fundingStartDate: string;
    anticipatedEndDate?: string;
  };

  /** Staff member confirming enrollment */
  confirmedBy: string;

  /** Notification method used */
  notificationMethod?: string;
}

export interface CreateEnrollmentResponse {
  /** Enrollment ID */
  enrollmentId: string;

  /** Client ID */
  clientId: string;

  /** Project ID */
  projectId: string;

  /** Entry date */
  entryDate: string;

  /** Enrollment status */
  status: 'ACTIVE' | 'PENDING_APPROVAL' | 'WAITLISTED' | 'DENIED' | 'WITHDRAWN';

  /** HMIS record ID (if created) */
  hmisRecordId?: string;

  /** Timestamp when enrollment was created */
  createdAt: string;

  /** Whether HMIS sync is pending */
  hmisSyncPending: boolean;
}

export interface UpdateEnrollmentRequest {
  /** Update entry date */
  entryDate?: string;

  /** Update move-in date */
  moveInDate?: string;

  /** Update enrollment status */
  status?: 'ACTIVE' | 'PENDING_APPROVAL' | 'WAITLISTED' | 'DENIED' | 'WITHDRAWN';

  /** Update cost allocation */
  costAllocation?: {
    dailyRate: number;
    fundingStartDate: string;
    anticipatedEndDate?: string;
  };
}

// =============================================================================
// CLIENT PROMOTION (Temp → Full)
// =============================================================================

export interface PromoteClientRequest {
  /** Temporary client ID to promote */
  tempClientId: string;

  /** Full legal name (collected in Step 8) */
  name: {
    firstName: string;
    middleName?: string;
    lastName: string;
    suffix?: string;
    preferredName?: string;
    nameDataQuality: number;
  };

  /** Government identifiers */
  identifiers: {
    socialSecurityNumber?: string;
    ssnDataQuality: number;
    stateIdNumber?: string;
    stateIdState?: string;
    birthDate: string;
    dobDataQuality: number;
    birthPlace?: string;
  };

  /** Demographics */
  demographics: {
    gender: string;
    hmisGender: string[];
    race: string[];
    ethnicity: string;
    primaryLanguage: string;
    interpreterNeeded: boolean;
    preferredLanguage?: string;
  };

  /** Veteran status */
  veteranStatus: string;

  /** Disabling condition */
  disablingCondition: string;

  /** VAWA protected status */
  vawaProtected: boolean;

  /** Contact information */
  contactInfo?: {
    phones: Array<{
      type: string;
      number: string;
      primary: boolean;
    }>;
    emails: Array<{
      type: string;
      address: string;
      primary: boolean;
    }>;
    addressConfidential: boolean;
  };
}

export interface PromoteClientResponse {
  /** Newly created client ID */
  clientId: string;

  /** HMIS client ID (pseudonymized if VAWA protected) */
  hmisClientId: string;

  /** Original temp client ID (for reference) */
  tempClientId: string;

  /** Timestamp when promotion occurred */
  promotedAt: string;

  /** Whether client is VAWA protected */
  vawaProtected: boolean;
}

// =============================================================================
// RECURRING TASKS
// =============================================================================

export interface CreateRecurringTaskRequest {
  /** Client ID */
  clientId: string;

  /** Enrollment ID */
  enrollmentId: string;

  /** Task type */
  type: 'PROGRESS_REVIEW' | 'SAFETY_CHECK' | 'HOUSING_STABILITY' | 'MONTHLY_CHECKIN' | 'REASSESSMENT' | 'OTHER';

  /** Task description */
  description: string;

  /** Start date for recurrence */
  startDate: string;

  /** Recurrence rule (RRULE format) */
  recurrenceRule: string;

  /** End date for recurrence (optional) */
  endDate?: string;

  /** Assigned to (case manager ID) */
  assignedTo: string;

  /** Priority level */
  priority: 'LOW' | 'MEDIUM' | 'HIGH';

  /** Reminder settings */
  reminders?: Array<{
    method: 'EMAIL' | 'SMS' | 'PHONE' | 'IN_APP';
    daysBefore: number;
  }>;

  /** Related goal ID (if task is for goal tracking) */
  goalId?: string;
}

export interface CreateRecurringTaskResponse {
  /** Task series ID */
  taskSeriesId: string;

  /** Individual task IDs created */
  taskIds: string[];

  /** First occurrence date */
  firstOccurrence: string;

  /** Next occurrence date */
  nextOccurrence?: string;

  /** Total occurrences created */
  totalOccurrences: number;

  /** Created at timestamp */
  createdAt: string;
}

export interface GetTasksRequest {
  /** Filter by client ID */
  clientId?: string;

  /** Filter by enrollment ID */
  enrollmentId?: string;

  /** Filter by assigned user */
  assignedTo?: string;

  /** Filter by status */
  status?: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

  /** Filter by due date range */
  dueDateStart?: string;
  dueDateEnd?: string;

  /** Include completed tasks */
  includeCompleted?: boolean;
}

export interface TaskResponse {
  taskId: string;
  clientId: string;
  enrollmentId: string;
  type: string;
  description: string;
  dueDate: string;
  assignedTo: string;
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  priority: 'LOW' | 'MEDIUM' | 'HIGH';
  createdAt: string;
  completedAt?: string;
  remindersSent: number;
}

// =============================================================================
// DOCUMENT UPLOAD
// =============================================================================

export interface UploadDocumentRequest {
  /** Client ID */
  clientId: string;

  /** Document type */
  documentType: string;

  /** File data (base64 or multipart) */
  file: File | string;

  /** File metadata */
  metadata: {
    filename: string;
    mimeType: string;
    fileSize: number;
  };

  /** Document tags */
  tags?: string[];

  /** Auto-delete after X days (for temporary docs) */
  expirationDays?: number;

  /** VAWA protected */
  vawaProtected?: boolean;
}

export interface UploadDocumentResponse {
  /** Document ID */
  documentId: string;

  /** File ID in storage system */
  fileId: string;

  /** Upload timestamp */
  uploadedAt: string;

  /** File size */
  fileSize: number;

  /** Whether file is encrypted */
  encrypted: boolean;

  /** Encryption algorithm used */
  encryptionAlgorithm?: string;

  /** Expiration date (if temporary) */
  expiresAt?: string;
}

export interface GetDocumentRequest {
  /** Document ID */
  documentId: string;

  /** Client ID (for authorization) */
  clientId: string;
}

export interface GetDocumentResponse {
  documentId: string;
  clientId: string;
  documentType: string;
  filename: string;
  mimeType: string;
  fileSize: number;
  uploadedAt: string;
  uploadedBy: string;
  verified: boolean;
  verifiedAt?: string;
  tags: string[];
  expiresAt?: string;
  /** Signed URL for download (temporary, expires in 15 minutes) */
  downloadUrl: string;
}

// =============================================================================
// ERROR RESPONSES
// =============================================================================

export interface ApiErrorResponse {
  /** Error code */
  code: string;

  /** Human-readable error message */
  message: string;

  /** Detailed error information */
  details?: Record<string, any>;

  /** Field-level validation errors */
  fieldErrors?: Array<{
    field: string;
    message: string;
    code: string;
  }>;

  /** Timestamp */
  timestamp: string;

  /** Request ID for tracking */
  requestId: string;
}

// =============================================================================
// COMMON TYPES
// =============================================================================

export interface PaginationRequest {
  page?: number;
  pageSize?: number;
  sortBy?: string;
  sortOrder?: 'ASC' | 'DESC';
}

export interface PaginatedResponse<T> {
  data: T[];
  pagination: {
    page: number;
    pageSize: number;
    totalPages: number;
    totalItems: number;
  };
}

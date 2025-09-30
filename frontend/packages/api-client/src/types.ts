import { z } from 'zod';

// API Response types
export interface ApiResponse<T> {
  data: T;
  success: boolean;
  message?: string;
}

export interface ApiError {
  code: string;
  message: string;
  status: number;
  timestamp: string;
  fieldErrors?: Record<string, string>;
}

export interface PaginatedResponse<T> {
  data: T[];
  pagination: {
    page: number;
    limit: number;
    total: number;
    totalPages: number;
  };
}

// FHIR-inspired value object schemas
export const HumanNameSchema = z.object({
  use: z.enum(['USUAL', 'OFFICIAL', 'TEMP', 'NICKNAME', 'ANONYMOUS', 'OLD', 'MAIDEN']),
  family: z.string(),
  given: z.array(z.string()),
  prefix: z.array(z.string()).optional(),
  suffix: z.array(z.string()).optional(),
  text: z.string().optional(),
});

export const AddressSchema = z.object({
  line1: z.string(),
  line2: z.string().optional(),
  city: z.string(),
  state: z.string(),
  postalCode: z.string(),
  country: z.string(),
  type: z.enum(['POSTAL', 'PHYSICAL', 'BOTH']),
  use: z.enum(['HOME', 'WORK', 'TEMP', 'OLD', 'BILLING']),
});

export const ContactPointSchema = z.object({
  system: z.enum(['PHONE', 'FAX', 'EMAIL', 'PAGER', 'URL', 'SMS', 'OTHER']),
  value: z.string(),
  use: z.enum(['HOME', 'WORK', 'TEMP', 'OLD', 'MOBILE']),
  rank: z.number().optional(),
});

export const CodeableConceptSchema = z.object({
  coding: z.array(z.object({
    system: z.string().optional(),
    version: z.string().optional(),
    code: z.string(),
    display: z.string().optional(),
    userSelected: z.boolean().optional(),
  })),
  text: z.string().optional(),
});

export const PeriodSchema = z.object({
  start: z.string().optional(), // ISO string
  end: z.string().optional(),   // ISO string
});

// Domain entity schemas
export const ClientSchema = z.object({
  id: z.string().uuid(),
  name: HumanNameSchema,
  gender: z.enum(['MALE', 'FEMALE', 'OTHER', 'UNKNOWN']),
  birthDate: z.string().optional(), // ISO date string
  addresses: z.array(AddressSchema),
  telecoms: z.array(ContactPointSchema),
  status: z.enum(['ACTIVE', 'INACTIVE', 'SUSPENDED', 'ENTERED_IN_ERROR']),
  createdAt: z.string(), // ISO string
});

export const CaseSchema = z.object({
  id: z.string().uuid(),
  clientId: z.string().uuid(),
  caseType: CodeableConceptSchema,
  priority: CodeableConceptSchema,
  status: z.enum(['OPEN', 'IN_PROGRESS', 'ON_HOLD', 'CLOSED', 'CANCELLED']),
  description: z.string(),
  assignment: z.object({
    assigneeId: z.string(),
    role: CodeableConceptSchema,
    assignedAt: z.string(), // ISO string
  }).optional(),
  noteCount: z.number(),
  createdAt: z.string(), // ISO string
  period: PeriodSchema.optional(),
});

// Household Composition schemas
export const HouseholdMemberSchema = z.object({
  membershipId: z.string().uuid(),
  householdCompositionId: z.string().uuid(),
  memberId: z.string().uuid(),
  memberFirstName: z.string(),
  memberLastName: z.string(),
  memberFullName: z.string(),
  memberDateOfBirth: z.string().optional(),
  relationshipCode: z.string().optional(),
  relationshipDisplay: z.string().optional(),
  membershipStartDate: z.string(), // ISO date
  membershipEndDate: z.string().optional(), // ISO date
  isActive: z.boolean(),
  isHeadOfHousehold: z.boolean(),
  recordedBy: z.string(),
  reason: z.string().optional(),
  recordedAt: z.string(), // ISO datetime
  membershipDurationDays: z.number(),
});

export const HouseholdCompositionSchema = z.object({
  id: z.string().uuid(),
  headOfHouseholdId: z.string().uuid(),
  headOfHouseholdFirstName: z.string(),
  headOfHouseholdLastName: z.string(),
  headOfHouseholdFullName: z.string(),
  headOfHouseholdDateOfBirth: z.string().optional(),
  compositionDate: z.string(), // ISO date
  householdType: z.enum([
    'SINGLE_ADULT', 'FAMILY_WITH_CHILDREN', 'COUPLE_NO_CHILDREN', 
    'MULTIGENERATIONAL', 'SHARED_HOUSING', 'TEMPORARY_CUSTODY', 
    'FOSTER_CARE', 'OTHER'
  ]),
  notes: z.string().optional(),
  createdAt: z.string(), // ISO datetime
  currentHouseholdSize: z.number(),
  totalMembersCount: z.number(),
  activeChildrenCount: z.number(),
  allMembers: z.array(HouseholdMemberSchema),
  activeMembers: z.array(HouseholdMemberSchema),
  custodyChanges: z.array(z.object({
    membershipId: z.string().uuid(),
    childId: z.string().uuid(),
    childFirstName: z.string(),
    childLastName: z.string(),
    previousRelationshipCode: z.string().optional(),
    newRelationshipCode: z.string(),
    effectiveDate: z.string(), // ISO date
    courtOrderReference: z.string().optional(),
    recordedBy: z.string(),
    recordedAt: z.string(), // ISO datetime
  })),
});

export const CreateHouseholdCompositionRequestSchema = z.object({
  headOfHouseholdId: z.string().uuid(),
  effectiveDate: z.string(), // ISO date
  householdType: z.enum([
    'SINGLE_ADULT', 'FAMILY_WITH_CHILDREN', 'COUPLE_NO_CHILDREN', 
    'MULTIGENERATIONAL', 'SHARED_HOUSING', 'TEMPORARY_CUSTODY', 
    'FOSTER_CARE', 'OTHER'
  ]),
  recordedBy: z.string(),
  notes: z.string().optional(),
});

export const AddHouseholdMemberRequestSchema = z.object({
  memberId: z.string().uuid(),
  relationship: CodeableConceptSchema,
  effectiveFrom: z.string(), // ISO date
  effectiveTo: z.string().optional(), // ISO date
  recordedBy: z.string(),
  reason: z.string().optional(),
});

// Inferred types from schemas
export type HumanName = z.infer<typeof HumanNameSchema>;
export type Address = z.infer<typeof AddressSchema>;
export type HouseholdMember = z.infer<typeof HouseholdMemberSchema>;
export type HouseholdComposition = z.infer<typeof HouseholdCompositionSchema>;
export type CreateHouseholdCompositionRequest = z.infer<typeof CreateHouseholdCompositionRequestSchema>;
export type AddHouseholdMemberRequest = z.infer<typeof AddHouseholdMemberRequestSchema>;
export type ContactPoint = z.infer<typeof ContactPointSchema>;
export type CodeableConcept = z.infer<typeof CodeableConceptSchema>;
export type Period = z.infer<typeof PeriodSchema>;
export type Client = z.infer<typeof ClientSchema>;
export type Case = z.infer<typeof CaseSchema>;

// Request/Command types
// FHIR-style create client request
const FhirAddressRequestSchema = z.object({
  use: z.enum(['HOME', 'WORK', 'TEMP', 'OLD', 'BILLING']).optional(),
  type: z.enum(['POSTAL', 'PHYSICAL', 'BOTH']).optional(),
  text: z.string().optional(),
  line: z.array(z.string()).optional(),
  city: z.string().optional(),
  district: z.string().optional(),
  state: z.string().optional(),
  postalCode: z.string().optional(),
  country: z.string().optional(),
});

const FhirContactPointRequestSchema = z.object({
  system: z.enum(['PHONE', 'FAX', 'EMAIL', 'PAGER', 'URL', 'SMS', 'OTHER']),
  value: z.string(),
  use: z.enum(['HOME', 'WORK', 'TEMP', 'OLD', 'MOBILE']).optional(),
  rank: z.number().optional(),
});

const FhirContactRequestSchema = z.object({
  relationship: z.array(z.object({ text: z.string().optional() })).optional(),
  name: HumanNameSchema.optional(),
  telecom: z.array(FhirContactPointRequestSchema).optional(),
  address: FhirAddressRequestSchema.optional(),
  gender: z.string().optional(),
  organization: z.string().optional(),
  period: z.any().optional(),
});

export const CreateClientRequestSchema = z.object({
  name: HumanNameSchema,
  gender: z.enum(['MALE', 'FEMALE', 'OTHER', 'UNKNOWN']),
  birthDate: z.string().optional(),
  maritalStatus: z.object({
    text: z.string().optional(),
    coding: z.array(z.object({
      system: z.string().optional(),
      version: z.string().optional(),
      code: z.string().optional(),
      display: z.string().optional(),
      userSelected: z.boolean().optional(),
    })).optional(),
  }).optional(),
  addresses: z.array(FhirAddressRequestSchema).optional(),
  telecoms: z.array(FhirContactPointRequestSchema).optional(),
  contact: z.array(FhirContactRequestSchema).optional(),
  communication: z.array(z.object({
    language: z.object({ text: z.string().optional(), coding: z.array(z.any()).optional() }).optional(),
    preferred: z.boolean().optional(),
  })).optional(),
  status: z.string().optional(),
});

export const UpdateClientDemographicsRequestSchema = z.object({
  givenName: z.string().min(1).max(100),
  familyName: z.string().min(1).max(100),
  gender: z.enum(['MALE', 'FEMALE', 'OTHER', 'UNKNOWN']),
  birthDate: z.string().optional(),
});

export const AddClientAddressRequestSchema = z.object({
  address: AddressSchema,
});

export const AddClientTelecomRequestSchema = z.object({
  telecom: ContactPointSchema,
});

export const OpenCaseRequestSchema = z.object({
  clientId: z.string().uuid(),
  caseType: CodeableConceptSchema,
  priority: CodeableConceptSchema.optional(),
  description: z.string().min(1).max(1000),
});

export const AssignCaseRequestSchema = z.object({
  assigneeId: z.string().min(1),
  role: CodeableConceptSchema,
});

export const AddCaseNoteRequestSchema = z.object({
  content: z.string().min(1).max(2000),
  authorId: z.string().min(1),
});

// Service Episode Types
export const ServiceTypeSchema = z.enum([
  // Housing Services
  'EMERGENCY_SHELTER', 'TRANSITIONAL_HOUSING', 'RAPID_REHOUSING', 'PERMANENT_SUPPORTIVE_HOUSING',
  'HOUSING_SEARCH_ASSISTANCE', 'HOUSING_STABILITY_CASE_MANAGEMENT', 'RENT_ASSISTANCE', 'UTILITY_ASSISTANCE',
  'SECURITY_DEPOSIT_ASSISTANCE',
  
  // Crisis Intervention & Safety
  'CRISIS_INTERVENTION', 'SAFETY_PLANNING', 'RISK_ASSESSMENT', 'EMERGENCY_RESPONSE',
  'MOBILE_CRISIS_RESPONSE', 'HOTLINE_CRISIS_CALL',
  
  // Counseling & Mental Health
  'INDIVIDUAL_COUNSELING', 'GROUP_COUNSELING', 'FAMILY_COUNSELING', 'TRAUMA_COUNSELING',
  'SUBSTANCE_ABUSE_COUNSELING', 'MENTAL_HEALTH_SERVICES', 'PSYCHIATRIC_EVALUATION',
  'THERAPY_SESSION', 'SUPPORT_GROUP',
  
  // Legal Services
  'LEGAL_ADVOCACY', 'COURT_ACCOMPANIMENT', 'PROTECTION_ORDER_ASSISTANCE',
  'IMMIGRATION_LEGAL_SERVICES', 'FAMILY_LAW_ASSISTANCE', 'LEGAL_CLINIC',
  'LEGAL_CONSULTATION', 'DOCUMENT_PREPARATION',
  
  // Case Management
  'CASE_MANAGEMENT', 'SERVICE_PLANNING', 'RESOURCE_COORDINATION', 'FOLLOW_UP_CONTACT',
  'DISCHARGE_PLANNING', 'INTAKE_ASSESSMENT', 'COMPREHENSIVE_ASSESSMENT',
  
  // Financial Assistance
  'EMERGENCY_FINANCIAL_ASSISTANCE', 'BENEFIT_ASSISTANCE', 'EMPLOYMENT_ASSISTANCE',
  'FINANCIAL_LITERACY', 'BUDGET_COUNSELING',
  
  // Healthcare & Medical
  'MEDICAL_ADVOCACY', 'HEALTHCARE_COORDINATION', 'MEDICAL_ACCOMPANIMENT',
  'HEALTH_EDUCATION', 'REPRODUCTIVE_HEALTH_SERVICES',
  
  // Children & Family Services
  'CHILDCARE', 'CHILDREN_COUNSELING', 'PARENTING_SUPPORT', 'FAMILY_REUNIFICATION',
  'SUPERVISED_VISITATION',
  
  // Education & Life Skills
  'EDUCATION_SERVICES', 'GED_PREPARATION', 'LIFE_SKILLS_TRAINING', 'JOB_TRAINING',
  'COMPUTER_LITERACY',
  
  // Transportation & Support
  'TRANSPORTATION', 'INTERPRETATION_SERVICES', 'CHILDCARE_DURING_SERVICES',
  'FOOD_ASSISTANCE', 'CLOTHING_ASSISTANCE',
  
  // Information & Referral
  'INFORMATION_AND_REFERRAL', 'RESOURCE_REFERRAL', 'COMMUNITY_EDUCATION',
  'PREVENTION_EDUCATION',
  
  // Specialized DV Services
  'DV_COUNSELING', 'DV_SUPPORT_GROUP', 'DV_SAFETY_PLANNING', 'STALKING_ADVOCACY',
  
  // Sexual Assault Services
  'SA_COUNSELING', 'SA_CRISIS_INTERVENTION', 'SANE_ACCOMPANIMENT', 'SA_SUPPORT_GROUP',
  
  // Other
  'OTHER'
]);

export const ServiceCategorySchema = z.enum([
  'HOUSING', 'CRISIS_RESPONSE', 'COUNSELING', 'LEGAL', 'CASE_MANAGEMENT',
  'FINANCIAL', 'HEALTHCARE', 'CHILDREN_FAMILY', 'EDUCATION', 'SUPPORT_SERVICES',
  'INFORMATION', 'DV_SPECIFIC', 'SA_SPECIFIC', 'OTHER'
]);

export const ServiceDeliveryModeSchema = z.enum([
  'IN_PERSON', 'PHONE', 'VIDEO_CONFERENCE', 'TEXT_MESSAGE', 'EMAIL', 'CHAT',
  'GROUP_IN_PERSON', 'GROUP_VIRTUAL', 'OUTREACH', 'ACCOMPANIMENT', 'RESIDENTIAL', 'OTHER'
]);

export const ServiceCompletionStatusSchema = z.enum([
  'SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'PARTIALLY_COMPLETED',
  'CANCELLED', 'NO_SHOW', 'POSTPONED'
]);

export const FunderTypeSchema = z.enum([
  'FEDERAL', 'STATE', 'LOCAL', 'PRIVATE', 'CORPORATE', 'UNFUNDED'
]);

export const BillingRateCategorySchema = z.enum([
  'FEDERAL_RATE', 'STATE_RATE', 'LOCAL_RATE', 'PRIVATE_RATE', 'NO_BILLING'
]);

export const FundingSourceSchema = z.object({
  funderId: z.string(),
  funderName: z.string(),
  grantNumber: z.string().optional(),
  funderType: FunderTypeSchema,
  programName: z.string(),
  requiresOutcomeTracking: z.boolean(),
  allowsConfidentialServices: z.boolean(),
  billingRateCategory: BillingRateCategorySchema,
});

export const ServiceTypeResponseSchema = z.object({
  name: z.string(),
  description: z.string(),
  category: ServiceCategorySchema,
  requiresConfidentialHandling: z.boolean(),
  isBillableService: z.boolean(),
  typicalMinDuration: z.number(),
  typicalMaxDuration: z.number(),
});

export const ServiceDeliveryModeResponseSchema = z.object({
  name: z.string(),
  description: z.string(),
  allowsConfidentialServices: z.boolean(),
  hasReducedBillingRate: z.boolean(),
  isRemoteDelivery: z.boolean(),
  billingMultiplier: z.number(),
});

export const ServiceEpisodeSchema = z.object({
  id: z.string().uuid(),
  clientId: z.string().uuid(),
  enrollmentId: z.string(),
  programId: z.string(),
  programName: z.string(),
  serviceType: ServiceTypeSchema,
  serviceCategory: ServiceCategorySchema,
  deliveryMode: ServiceDeliveryModeSchema,
  serviceDate: z.string(),
  startTime: z.string().optional(),
  endTime: z.string().optional(),
  plannedDurationMinutes: z.number().optional(),
  actualDurationMinutes: z.number().optional(),
  primaryProviderId: z.string(),
  primaryProviderName: z.string(),
  additionalProviderIds: z.array(z.string()),
  primaryFundingSource: FundingSourceSchema,
  additionalFundingSources: z.array(FundingSourceSchema),
  onBehalfOfOrganization: z.string().optional(),
  isBillable: z.boolean(),
  billingCode: z.string().optional(),
  billingRate: z.number().optional(),
  totalBillableAmount: z.number().optional(),
  serviceDescription: z.string().optional(),
  serviceGoals: z.string().optional(),
  serviceOutcome: z.string().optional(),
  completionStatus: ServiceCompletionStatusSchema,
  followUpRequired: z.string().optional(),
  followUpDate: z.string().optional(),
  notes: z.string().optional(),
  isConfidential: z.boolean(),
  confidentialityReason: z.string().optional(),
  isRestrictedAccess: z.boolean(),
  authorizedViewerIds: z.array(z.string()),
  serviceLocation: z.string().optional(),
  serviceLocationAddress: z.string().optional(),
  isOffSite: z.boolean(),
  contextNotes: z.string().optional(),
  isCourtOrdered: z.boolean(),
  courtOrderNumber: z.string().optional(),
  requiresDocumentation: z.boolean(),
  attachedDocumentIds: z.array(z.string()),
  qualityAssuranceNotes: z.string().optional(),
  createdAt: z.string(),
  lastModifiedAt: z.string(),
  createdBy: z.string(),
  lastModifiedBy: z.string(),
});

// Service Episode Request Schemas
export const CreateServiceEpisodeRequestSchema = z.object({
  clientId: z.string().uuid(),
  enrollmentId: z.string().min(1),
  programId: z.string().min(1),
  programName: z.string().min(1),
  serviceType: ServiceTypeSchema,
  deliveryMode: ServiceDeliveryModeSchema,
  serviceDate: z.string(),
  plannedDurationMinutes: z.number().positive().optional(),
  primaryProviderId: z.string().min(1),
  primaryProviderName: z.string().min(1),
  funderId: z.string().min(1),
  funderName: z.string().optional(),
  grantNumber: z.string().optional(),
  serviceDescription: z.string().max(1000).optional(),
  isConfidential: z.boolean(),
});

export const StartServiceRequestSchema = z.object({
  startTime: z.string(),
  location: z.string().optional(),
});

export const CompleteServiceRequestSchema = z.object({
  endTime: z.string(),
  outcome: z.string().max(1000).optional(),
  status: ServiceCompletionStatusSchema,
  notes: z.string().max(2000).optional(),
});

export const QuickCrisisServiceRequestSchema = z.object({
  clientId: z.string().uuid(),
  enrollmentId: z.string().min(1),
  programId: z.string().min(1),
  providerId: z.string().min(1),
  providerName: z.string().min(1),
  isConfidential: z.boolean(),
});

export const QuickCounselingServiceRequestSchema = z.object({
  clientId: z.string().uuid(),
  enrollmentId: z.string().min(1),
  programId: z.string().min(1),
  serviceType: ServiceTypeSchema,
  providerId: z.string().min(1),
  providerName: z.string().min(1),
});

export const QuickCaseManagementServiceRequestSchema = z.object({
  clientId: z.string().uuid(),
  enrollmentId: z.string().min(1),
  programId: z.string().min(1),
  deliveryMode: ServiceDeliveryModeSchema,
  providerId: z.string().min(1),
  providerName: z.string().min(1),
  description: z.string().max(500).optional(),
});

export const UpdateOutcomeRequestSchema = z.object({
  outcome: z.string().max(1000).optional(),
  followUpRequired: z.string().max(500).optional(),
  followUpDate: z.string().optional(),
});

export const ServiceSearchCriteriaSchema = z.object({
  clientId: z.string().uuid().optional(),
  enrollmentId: z.string().optional(),
  programId: z.string().optional(),
  serviceType: ServiceTypeSchema.optional(),
  serviceCategory: ServiceCategorySchema.optional(),
  deliveryMode: ServiceDeliveryModeSchema.optional(),
  startDate: z.string().optional(),
  endDate: z.string().optional(),
  providerId: z.string().optional(),
  confidentialOnly: z.boolean().default(false),
  courtOrderedOnly: z.boolean().default(false),
  followUpRequired: z.boolean().default(false),
});

export const ServiceStatisticsSchema = z.object({
  totalServices: z.number(),
  completedServices: z.number(),
  inProgressServices: z.number(),
  confidentialServices: z.number(),
  courtOrderedServices: z.number(),
  servicesRequiringFollowUp: z.number(),
  overdueServices: z.number(),
  totalHours: z.number(),
  averageDurationMinutes: z.number(),
  serviceTypeBreakdown: z.record(z.number()),
  serviceCategoryBreakdown: z.record(z.number()),
  deliveryModeBreakdown: z.record(z.number()),
  fundingSourceBreakdown: z.record(z.number()),
  programBreakdown: z.record(z.number()),
});

// Inferred types
export type ServiceType = z.infer<typeof ServiceTypeSchema>;
export type ServiceCategory = z.infer<typeof ServiceCategorySchema>;
export type ServiceDeliveryMode = z.infer<typeof ServiceDeliveryModeSchema>;
export type ServiceCompletionStatus = z.infer<typeof ServiceCompletionStatusSchema>;
export type FunderType = z.infer<typeof FunderTypeSchema>;
export type BillingRateCategory = z.infer<typeof BillingRateCategorySchema>;
export type FundingSource = z.infer<typeof FundingSourceSchema>;
export type ServiceTypeResponse = z.infer<typeof ServiceTypeResponseSchema>;
export type ServiceDeliveryModeResponse = z.infer<typeof ServiceDeliveryModeResponseSchema>;
export type ServiceEpisode = z.infer<typeof ServiceEpisodeSchema>;

export type CreateServiceEpisodeRequest = z.infer<typeof CreateServiceEpisodeRequestSchema>;
export type StartServiceRequest = z.infer<typeof StartServiceRequestSchema>;
export type CompleteServiceRequest = z.infer<typeof CompleteServiceRequestSchema>;
export type QuickCrisisServiceRequest = z.infer<typeof QuickCrisisServiceRequestSchema>;
export type QuickCounselingServiceRequest = z.infer<typeof QuickCounselingServiceRequestSchema>;
export type QuickCaseManagementServiceRequest = z.infer<typeof QuickCaseManagementServiceRequestSchema>;
export type UpdateOutcomeRequest = z.infer<typeof UpdateOutcomeRequestSchema>;
export type ServiceSearchCriteria = z.infer<typeof ServiceSearchCriteriaSchema>;
export type ServiceStatistics = z.infer<typeof ServiceStatisticsSchema>;

export const UpdateCaseStatusRequestSchema = z.object({
  newStatus: z.enum(['OPEN', 'IN_PROGRESS', 'ON_HOLD', 'CLOSED', 'CANCELLED']),
});

export const CloseCaseRequestSchema = z.object({
  reason: z.string().min(1).max(500),
});

// Inferred request types
export type CreateClientRequest = z.infer<typeof CreateClientRequestSchema>;
export type UpdateClientDemographicsRequest = z.infer<typeof UpdateClientDemographicsRequestSchema>;
export type AddClientAddressRequest = z.infer<typeof AddClientAddressRequestSchema>;
export type AddClientTelecomRequest = z.infer<typeof AddClientTelecomRequestSchema>;
export type OpenCaseRequest = z.infer<typeof OpenCaseRequestSchema>;
export type AssignCaseRequest = z.infer<typeof AssignCaseRequestSchema>;
export type AddCaseNoteRequest = z.infer<typeof AddCaseNoteRequestSchema>;
export type UpdateCaseStatusRequest = z.infer<typeof UpdateCaseStatusRequestSchema>;
export type CloseCaseRequest = z.infer<typeof CloseCaseRequestSchema>;

// Query parameters
export interface ClientSearchParams {
  name?: string;
  activeOnly?: boolean;
  page?: number;
  limit?: number;
}

export interface CaseSearchParams {
  clientId?: string;
  assigneeId?: string;
  activeOnly?: boolean;
  requiresAttention?: boolean;
  page?: number;
  limit?: number;
}

// Triage Dashboard types
export interface TriageAlert {
  id: string;
  clientId: string;
  clientName: string;
  alertType: string;
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
  description: string;
  dueDate: string;
  status: 'ACTIVE' | 'ACKNOWLEDGED' | 'IN_PROGRESS' | 'RESOLVED' | 'EXPIRED';
  caseNumber?: string;
  assignedWorkerId?: string;
  assignedWorkerName?: string;
  isOverdue: boolean;
  daysUntilDue: number;
}

export interface TriageDashboardData {
  criticalCount: number;
  highCount: number;
  mediumCount: number;
  lowCount: number;
  overdueCount: number;
  upcomingAlerts: TriageAlert[];
  overdueAlerts: TriageAlert[];
}

// Caseload types
export interface CaseloadItem {
  caseId: string;
  caseNumber: string;
  clientId: string;
  clientName: string;
  workerId?: string;
  workerName?: string;
  stage: 'INTAKE' | 'ACTIVE' | 'HOUSING_SEARCH' | 'STABILIZATION' | 'EXIT_PLANNING' | 'FOLLOW_UP' | 'CLOSED';
  stageDescription: string;
  riskLevel: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'STABLE';
  programName?: string;
  enrollmentDate: string;
  lastServiceDate?: string;
  serviceCount: number;
  daysSinceLastContact?: number;
  activeAlerts: string[];
  status: string;
  requiresAttention: boolean;
  needsUrgentAttention: boolean;
  isOverdue: boolean;
  isSafeAtHome?: boolean;
  isConfidentialLocation?: boolean;
  dataSystem?: 'HMIS' | 'COMPARABLE_DB';
}

export interface CaseloadResponse {
  cases: CaseloadItem[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  stageCounts: Record<string, number>;
  riskCounts: Record<string, number>;
  highRiskCount: number;
  overdueCount: number;
}

export interface WorkerCaseload {
  workerId: string;
  workerName?: string;
  totalCases: number;
  intakeCases: number;
  activeCases: number;
  housingSearchCases: number;
  highRiskCases: number;
  requiringAttention: number;
}

export interface TeamOverview {
  workerCaseloads: WorkerCaseload[];
  totalActiveCases: number;
  averageCaseload: number;
}

// Funding Compliance types
export interface FundingComplianceView {
  fundingSourceId: string;
  fundingSourceName: string;
  fundingType: string;
  totalBudget: number;
  amountSpent: number;
  amountCommitted: number;
  amountAvailable: number;
  utilizationPercentage: number;
  fundingPeriodStart: string;
  fundingPeriodEnd: string;
  daysRemaining: number;
  complianceStatus: 'COMPLIANT' | 'ATTENTION_NEEDED' | 'AT_RISK' | 'NON_COMPLIANT' | 'UNDER_REVIEW';
  documentationGaps: DocumentationGap[];
  pendingPayments: PendingPayment[];
  spendDownTracking: SpendDownTracking;
  lastAuditDate?: string;
  grantNumber: string;
  programArea: string;
}

export interface DocumentationGap {
  clientId: string;
  clientName: string;
  missingDocument: string;
  dueDate: string;
  daysOverdue?: number;
  caseNumber: string;
}

export interface PendingPayment {
  paymentId: string;
  clientId: string;
  clientName: string;
  amount: number;
  paymentType: string;
  requestDate: string;
  dueDate: string;
  approvalStatus: string;
  vendorName?: string;
}

export interface SpendDownTracking {
  quarterlyTarget: number;
  quarterlySpent: number;
  monthlyTarget: number;
  monthlySpent: number;
  dailyBurnRate: number;
  projectedYearEndSpend: number;
  spendVelocity: number;
  isUnderspending: boolean;
  isOverspending: boolean;
  daysToTargetReached?: number;
}

// Confidentiality types
export interface ConfidentialityGuardrails {
  clientId: string;
  clientName: string;
  isSafeAtHome: boolean;
  isComparableDbOnly: boolean;
  hasConfidentialLocation: boolean;
  hasRestrictedData: boolean;
  dataSystem: string;
  visibilityLevel: 'PUBLIC' | 'RESTRICTED' | 'CONFIDENTIAL' | 'PRIVILEGED';
  lastUpdated: string;
  bannerWarningText?: string;
  bannerSeverity?: 'CRITICAL' | 'HIGH' | 'MEDIUM';
}

// Restricted Notes types
export interface RestrictedNote {
  noteId: string;
  clientId: string;
  clientName: string;
  caseId?: string;
  caseNumber?: string;
  noteType: 'STANDARD' | 'COUNSELING' | 'PRIVILEGED_COUNSELING' | 'LEGAL_ADVOCACY' | 'ATTORNEY_CLIENT' | 'SAFETY_PLAN' | 'MEDICAL' | 'THERAPEUTIC' | 'INTERNAL_ADMIN';
  content: string;
  authorId: string;
  authorName: string;
  createdAt: string;
  lastModified: string;
  authorizedViewers?: string[];
  visibilityScope: 'PUBLIC' | 'CASE_TEAM' | 'CLINICAL_ONLY' | 'LEGAL_TEAM' | 'SAFETY_TEAM' | 'MEDICAL_TEAM' | 'ADMIN_ONLY' | 'AUTHOR_ONLY' | 'ATTORNEY_CLIENT' | 'CUSTOM';
  isSealed: boolean;
  sealReason?: string;
  sealedAt?: string;
  sealedBy?: string;
  visibilityWarning?: string;
  requiresSpecialHandling: boolean;
}

// Compliance & Audit types
export interface ComplianceMetric {
  name: string;
  description: string;
  target: number;
  achieved: number;
  unit?: string;
  category?: string;
  lastUpdated?: string;
}

export interface ComplianceOverview {
  overallScore: number;
  metrics?: ComplianceMetric[];
  lastAuditDate?: string;
}

// HUD Compliance Matrix types
export interface HudDataElement {
  hudId: string;
  name: string;
  description: string;
  mandatory: boolean;
  category: string;
  owningAggregate: string;
  dataType: string;
  domainImplementation: ImplementationStatus;
  apiImplementation: ImplementationStatus;
  uiImplementation: ImplementationStatus;
  notes: string;
}

export interface ImplementationStatus {
  implemented: boolean;
  location: string;
  details: string;
}

export interface HudComplianceMatrix {
  version: string;
  generatedAt: string;
  hudElements: HudDataElement[];
  overallComplianceScore: number;
  summary: HudComplianceSummary;
}

export interface HudComplianceSummary {
  totalElements: number;
  fullyImplemented: number;
  partiallyImplemented: number;
  notImplemented: number;
  byCategory: Record<string, CategorySummary>;
}

export interface CategorySummary {
  displayName: string;
  totalElements: number;
  implementedElements: number;
  compliancePercentage: number;
}

export interface ElementCoverageDetail {
  hudId: string;
  name: string;
  description: string;
  mandatory: boolean;
  category: string;
  owningAggregate: string;
  domainCoverage: CoverageDetail;
  apiCoverage: CoverageDetail;
  uiCoverage: CoverageDetail;
  notes: string;
}

export interface CoverageDetail {
  layer: string;
  implemented: boolean;
  location: string;
  details: string;
}

export interface ComplianceSummaryResponse {
  overallScore: number;
  totalElements: number;
  fullyImplemented: number;
  partiallyImplemented: number;
  notImplemented: number;
  categories: Record<string, CategorySummary>;
  lastUpdated: string;
}

export interface AuditEntry {
  id: string;
  userId?: string;
  userName?: string;
  action: string;
  resource: string;
  timestamp: string;
  details: string;
  result: 'SUCCESS' | 'FAILURE' | 'WARNING';
  metadata?: Record<string, any>;
}

export interface AuditLogFilters {
  action?: string;
  resource?: string;
  userId?: string;
  startDate?: string;
  endDate?: string;
}

// Mandated Reports types
export interface MandatedReport {
  id: string;
  title: string;
  description: string;
  type: 'FEDERAL' | 'STATE' | 'LOCAL' | 'FUNDER';
  frequency: 'MONTHLY' | 'QUARTERLY' | 'SEMI_ANNUAL' | 'ANNUAL' | 'ON_DEMAND';
  dueDate: string;
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'REVIEW' | 'SUBMITTED' | 'APPROVED' | 'REJECTED';
  priority: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
  assignedTo?: string;
  assignedToName?: string;
  submissionDeadline: string;
  lastSubmissionDate?: string;
  isOverdue: boolean;
  daysUntilDue: number;
  completionPercentage: number;
  requiredSections: string[];
  completedSections: string[];
  fundingSource?: string;
  regulatoryBody: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateMandatedReportRequest {
  title: string;
  description: string;
  type: 'FEDERAL' | 'STATE' | 'LOCAL' | 'FUNDER';
  frequency: 'MONTHLY' | 'QUARTERLY' | 'SEMI_ANNUAL' | 'ANNUAL' | 'ON_DEMAND';
  priority: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
  submissionDeadline: string;
  assignedTo?: string;
  requiredSections: string[];
  fundingSource?: string;
  regulatoryBody: string;
}

// Billing types
export interface BillingRecord {
  id: string;
  episodeId: string;
  clientId: string;
  clientName: string;
  serviceType: string;
  providerId: string;
  providerName: string;
  serviceDate: string;
  startTime?: string;
  endTime?: string;
  duration: number;
  billableAmount: number;
  fundingSource: string;
  billingCode?: string;
  status: 'PENDING' | 'SUBMITTED' | 'PAID' | 'REJECTED' | 'PROCESSING';
  submittedAt?: string;
  paidAt?: string;
  rejectedAt?: string;
  rejectionReason?: string;
  processingNotes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface BillingStatistics {
  totalBillable: number;
  totalPaid: number;
  totalPending: number;
  totalSubmitted: number;
  totalRejected: number;
  totalProcessing: number;
  averageRate: number;
  totalHours: number;
  reimbursementRate: number;
  averageProcessingDays: number;
  rejectionRate: number;
  monthlyRevenue: number;
  yearToDateRevenue: number;
  fundingSourceBreakdown: Record<string, {
    amount: number;
    count: number;
    percentage: number;
  }>;
  statusBreakdown: Record<string, {
    amount: number;
    count: number;
    percentage: number;
  }>;
  monthlyTrends: Array<{
    month: string;
    billable: number;
    paid: number;
    submitted: number;
    rejected: number;
  }>;
}

export interface BillingExportRequest {
  format: 'csv' | 'excel' | 'pdf';
  dateRange: {
    startDate: string;
    endDate: string;
  };
  filters?: {
    status?: string[];
    fundingSource?: string[];
    providerId?: string;
    clientId?: string;
  };
  includeDetails?: boolean;
  includeSummary?: boolean;
  groupBy?: 'provider' | 'funding_source' | 'client' | 'service_type';
}

export interface BillingExportResponse {
  exportId: string;
  filename: string;
  downloadUrl?: string;
  status: 'PROCESSING' | 'COMPLETED' | 'FAILED';
  createdAt: string;
  completedAt?: string;
  error?: string;
}

export interface GeneratedReport {
  id: string;
  title: string;
  description?: string;
  type: 'BILLING_SUMMARY' | 'REVENUE_ANALYSIS' | 'FUNDING_UTILIZATION' | 'PROVIDER_PERFORMANCE' | 'COMPLIANCE_REPORT';
  format: 'pdf' | 'excel' | 'csv';
  parameters: Record<string, any>;
  status: 'GENERATING' | 'COMPLETED' | 'FAILED' | 'QUEUED';
  downloadUrl?: string;
  filename?: string;
  fileSize?: number;
  generatedAt?: string;
  expiresAt?: string;
  createdBy: string;
  createdAt: string;
  error?: string;
}

export interface GenerateReportRequest {
  title: string;
  description?: string;
  type: 'BILLING_SUMMARY' | 'REVENUE_ANALYSIS' | 'FUNDING_UTILIZATION' | 'PROVIDER_PERFORMANCE' | 'COMPLIANCE_REPORT';
  format: 'pdf' | 'excel' | 'csv';
  parameters: {
    dateRange: {
      startDate: string;
      endDate: string;
    };
    filters?: {
      fundingSource?: string[];
      providerId?: string[];
      clientId?: string[];
      serviceType?: string[];
    };
    includeCharts?: boolean;
    includeTrends?: boolean;
    groupBy?: string[];
  };
}

// Assistance Payment Types with Arrears Support
export enum AssistancePaymentSubtype {
  RENT_CURRENT = 'RENT_CURRENT',
  RENT_ARREARS = 'RENT_ARREARS',
  UTILITY_CURRENT = 'UTILITY_CURRENT',
  UTILITY_ARREARS = 'UTILITY_ARREARS',
  SECURITY_DEPOSIT = 'SECURITY_DEPOSIT',
  APPLICATION_FEE = 'APPLICATION_FEE',
  MOVING_COSTS = 'MOVING_COSTS',
  OTHER = 'OTHER'
}

export interface AssistancePaymentRequest {
  housingAssistanceId: string;
  amount: number;
  paymentDate: string;
  paymentType: string;
  subtype: AssistancePaymentSubtype;
  periodStart?: string; // Required for arrears
  periodEnd?: string; // Required for arrears
  payeeId: string;
  payeeName: string;
  authorizedBy: string;
  notes?: string;
  fundingSourceCode?: string;
}

export interface AssistancePaymentResponse {
  paymentId: string;
  housingAssistanceId: string;
  clientId: string;
  enrollmentId: string;
  amount: number;
  paymentDate: string;
  paymentType: string;
  subtype: AssistancePaymentSubtype;
  periodStart?: string;
  periodEnd?: string;
  payeeId: string;
  payeeName: string;
  authorizedBy: string;
  status: string;
  fundingSourceCode?: string;
  notes?: string;
  createdAt: string;
  updatedAt: string;
  arrearMonths?: number;
  isArrears: boolean;
}

export interface ArrearsSummary {
  clientId: string;
  totalRentArrears: number;
  totalUtilityArrears: number;
  earliestArrearsDate?: string;
  latestArrearsDate?: string;
  monthsInArrears: number;
  details: ArrearsDetail[];
}

export interface ArrearsDetail {
  periodStart: string;
  periodEnd: string;
  amount: number;
  subtype: AssistancePaymentSubtype;
  paymentDate: string;
  status: string;
}

// Landlord Communication Types
export enum LandlordCommunicationChannel {
  PHONE = 'PHONE',
  EMAIL = 'EMAIL',
  TEXT = 'TEXT',
  FAX = 'FAX',
  PORTAL = 'PORTAL',
  IN_PERSON = 'IN_PERSON',
  OTHER = 'OTHER'
}

export enum LandlordCommunicationStatus {
  DRAFT = 'DRAFT',
  SENT = 'SENT',
  FAILED = 'FAILED'
}

export interface LandlordCommunicationRequest {
  clientId: string;
  housingAssistanceId?: string;
  channel: LandlordCommunicationChannel;
  subject: string;
  body: string;
  requestedFields?: Record<string, any>;
  recipientEmail?: string;
  recipientPhone?: string;
  recipientFax?: string;
  recipientPortalId?: string;
  preferredChannel?: string;
  notes?: string;
  urgent?: boolean;
}

export interface LandlordCommunicationResponse {
  id: string;
  landlordId: string;
  clientId: string;
  housingAssistanceId?: string;
  channel: string;
  subject: string;
  body: string;
  sharedFields?: Record<string, any>;
  recipientContact: string;
  consentChecked: boolean;
  consentType?: string;
  sentStatus: LandlordCommunicationStatus;
  sentAt?: string;
  sentBy: string;
  createdAt: string;
  updatedAt: string;
  landlordName?: string;
  clientName?: string;
  hasAttachments?: boolean;
  statusDescription?: string;
}

export interface ConsentCheckResponse {
  clientId: string;
  landlordId: string;
  hasInformationSharingConsent: boolean;
  hasReferralSharingConsent: boolean;
  consentValid: boolean;
  message: string;
}

export interface SafetyCheckResponse {
  clientId: string;
  channel: string;
  channelAllowed: boolean;
  restrictions: string[];
  message: string;
}

export interface LandlordCommunicationFilters {
  clientId?: string;
  landlordId?: string;
  channel?: LandlordCommunicationChannel;
  status?: LandlordCommunicationStatus;
  startDate?: string;
  endDate?: string;
  consentType?: string;
}

// Enrollment types
export interface EnrollmentSummary {
  id: string;
  clientId: string;
  programId: string;
  programName?: string;
  enrollmentDate: string;
  predecessorEnrollmentId?: string;
  residentialMoveInDate?: string;
  householdId?: string;
  status: string;
  stage?: 'INTAKE' | 'ACTIVE' | 'HOUSING_SEARCH' | 'STABILIZATION' | 'EXIT_PLANNING' | 'FOLLOW_UP' | 'CLOSED';
  programType?: string;
}

export interface IntakeAssessment {
  enrollmentId: string;
  clientId: string;
  assessmentDate: string;
  assessmentType: 'INTAKE' | 'COMPREHENSIVE' | 'REASSESSMENT';
  assessorName: string;
  assessorId: string;
  status: 'DRAFT' | 'IN_PROGRESS' | 'COMPLETED' | 'APPROVED';
  completionPercentage: number;
  lastModified: string;
  sections: IntakeSection[];
}

export interface IntakeSection {
  id: string;
  name: string;
  description?: string;
  isRequired: boolean;
  isCompleted: boolean;
  completedAt?: string;
  completedBy?: string;
  fields: IntakeField[];
}

export interface IntakeField {
  id: string;
  name: string;
  type: 'TEXT' | 'NUMBER' | 'DATE' | 'BOOLEAN' | 'SELECT' | 'MULTI_SELECT' | 'TEXTAREA';
  label: string;
  description?: string;
  isRequired: boolean;
  value?: any;
  options?: IntakeFieldOption[];
  validation?: IntakeFieldValidation;
}

export interface IntakeFieldOption {
  value: string;
  label: string;
  description?: string;
}

export interface IntakeFieldValidation {
  minLength?: number;
  maxLength?: number;
  pattern?: string;
  min?: number;
  max?: number;
}

export interface UpdateIntakeFieldRequest {
  fieldId: string;
  value: any;
}

// Financial Assistance types
export interface FinancialAssistanceRequest {
  id: string;
  clientId: string;
  enrollmentId: string;
  requestType: AssistanceRequestType;
  amount: number;
  requestDate: string;
  coveragePeriodStart?: string;
  coveragePeriodEnd?: string;
  payeeId: string;
  payeeName: string;
  fundingSourceId: string;
  fundingSourceName: string;
  justification: string;
  notes?: string;
  status: AssistanceRequestStatus;
  approvalWorkflow: ApprovalStep[];
  supportingDocuments: SupportingDocument[];
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  lastModifiedBy: string;
}

export interface CreateAssistanceRequestRequest {
  clientId: string;
  enrollmentId: string;
  requestType: AssistanceRequestType;
  amount: number;
  coveragePeriodStart?: string;
  coveragePeriodEnd?: string;
  payeeId: string;
  payeeName: string;
  fundingSourceId: string;
  justification: string;
  notes?: string;
  supportingDocumentIds: string[];
}

export interface AssistanceLedgerEntry {
  id: string;
  clientId: string;
  enrollmentId: string;
  date: string;
  type: AssistanceRequestType;
  description: string;
  amount: number;
  fundingSource: string;
  status: AssistancePaymentStatus;
  payeeName: string;
  approvedBy?: string;
  paidDate?: string;
  checkNumber?: string;
  transactionId?: string;
}

export interface AssistanceSummary {
  clientId: string;
  enrollmentId: string;
  totalApproved: number;
  totalDisbursed: number;
  totalPending: number;
  budgetCap: number;
  remainingBudget: number;
  byType: {
    rent: AssistanceTypeSummary;
    utilities: AssistanceTypeSummary;
    deposit: AssistanceTypeSummary;
    moving: AssistanceTypeSummary;
    other: AssistanceTypeSummary;
  };
}

export interface AssistanceTypeSummary {
  approved: number;
  disbursed: number;
  pending: number;
  monthlyAmount?: number;
  coverageEndDate?: string;
}

export interface ApprovalStep {
  id: string;
  stepName: string;
  approverRole: string;
  approverName?: string;
  approverId?: string;
  status: ApprovalStatus;
  approvedAt?: string;
  comments?: string;
  isRequired: boolean;
  order: number;
}

export interface SupportingDocument {
  id: string;
  filename: string;
  fileType: string;
  fileSize: number;
  uploadedAt: string;
  uploadedBy: string;
  documentType: SupportingDocumentType;
  description?: string;
}

export interface Payee {
  id: string;
  name: string;
  type: PayeeType;
  contactInfo: PayeeContact;
  taxInfo: PayeeTaxInfo;
  bankInfo?: PayeeBankInfo;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PayeeContact {
  address: {
    line1: string;
    line2?: string;
    city: string;
    state: string;
    postalCode: string;
  };
  phone?: string;
  email?: string;
  contactPerson?: string;
}

export interface PayeeTaxInfo {
  taxId: string;
  taxIdType: TaxIdType;
  w9OnFile: boolean;
  w9ExpirationDate?: string;
}

export interface PayeeBankInfo {
  accountType: BankAccountType;
  routingNumber: string;
  accountNumber: string; // Should be encrypted in real implementation
  achSetupDate?: string;
}

export interface FundingSource {
  id: string;
  name: string;
  type: FundingSourceType;
  program: string;
  totalBudget: number;
  remainingBudget: number;
  budgetPeriodStart: string;
  budgetPeriodEnd: string;
  clientCapLimit?: number;
  monthlyCapLimit?: number;
  isActive: boolean;
  requiresDocuments: SupportingDocumentType[];
  approvalThresholds: ApprovalThreshold[];
}

export interface ApprovalThreshold {
  threshold: number;
  requiredApprovers: string[];
  description: string;
}

export interface ApprovalQueueItem {
  requestId: string;
  clientId: string;
  clientName: string;
  requestType: AssistanceRequestType;
  amount: number;
  caseManagerName: string;
  submittedDate: string;
  currentApprovalStep: string;
  daysWaiting: number;
  isUrgent: boolean;
  fundingSource: string;
}

// Enums
export enum AssistanceRequestType {
  RENT = 'RENT',
  RENT_ARREARS = 'RENT_ARREARS',
  UTILITIES = 'UTILITIES',
  UTILITY_ARREARS = 'UTILITY_ARREARS',
  SECURITY_DEPOSIT = 'SECURITY_DEPOSIT',
  APPLICATION_FEE = 'APPLICATION_FEE',
  MOVING_COSTS = 'MOVING_COSTS',
  OTHER = 'OTHER'
}

export enum AssistanceRequestStatus {
  DRAFT = 'DRAFT',
  SUBMITTED = 'SUBMITTED',
  UNDER_REVIEW = 'UNDER_REVIEW',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  DISBURSED = 'DISBURSED',
  CANCELLED = 'CANCELLED'
}

export enum AssistancePaymentStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  PAID = 'PAID',
  REJECTED = 'REJECTED',
  CANCELLED = 'CANCELLED'
}

export enum ApprovalStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  SKIPPED = 'SKIPPED'
}

export enum SupportingDocumentType {
  LEASE_AGREEMENT = 'LEASE_AGREEMENT',
  RENTAL_AGREEMENT = 'RENTAL_AGREEMENT',
  UTILITY_BILL = 'UTILITY_BILL',
  EVICTION_NOTICE = 'EVICTION_NOTICE',
  MOVE_IN_INSPECTION = 'MOVE_IN_INSPECTION',
  INCOME_VERIFICATION = 'INCOME_VERIFICATION',
  VENDOR_INVOICE = 'VENDOR_INVOICE',
  RECEIPT = 'RECEIPT',
  OTHER = 'OTHER'
}

export enum PayeeType {
  LANDLORD = 'LANDLORD',
  PROPERTY_MANAGER = 'PROPERTY_MANAGER',
  UTILITY_COMPANY = 'UTILITY_COMPANY',
  MOVING_COMPANY = 'MOVING_COMPANY',
  VENDOR = 'VENDOR',
  INDIVIDUAL = 'INDIVIDUAL'
}

export enum TaxIdType {
  SSN = 'SSN',
  EIN = 'EIN',
  ITIN = 'ITIN'
}

export enum BankAccountType {
  CHECKING = 'CHECKING',
  SAVINGS = 'SAVINGS'
}

export enum FundingSourceType {
  HUD_COC = 'HUD_COC',
  HUD_RRH = 'HUD_RRH',
  HUD_PSH = 'HUD_PSH',
  VAWA = 'VAWA',
  STATE_FUNDING = 'STATE_FUNDING',
  LOCAL_FUNDING = 'LOCAL_FUNDING',
  PRIVATE_FOUNDATION = 'PRIVATE_FOUNDATION',
  OTHER = 'OTHER'
}

// Consent Management Types
export enum ConsentType {
  INFORMATION_SHARING = 'INFORMATION_SHARING',
  HMIS_PARTICIPATION = 'HMIS_PARTICIPATION',
  COURT_TESTIMONY = 'COURT_TESTIMONY',
  MEDICAL_INFORMATION_SHARING = 'MEDICAL_INFORMATION_SHARING',
  REFERRAL_SHARING = 'REFERRAL_SHARING',
  RESEARCH_PARTICIPATION = 'RESEARCH_PARTICIPATION',
  LEGAL_COUNSEL_COMMUNICATION = 'LEGAL_COUNSEL_COMMUNICATION',
  FAMILY_CONTACT = 'FAMILY_CONTACT'
}

export enum ConsentStatus {
  GRANTED = 'GRANTED',
  REVOKED = 'REVOKED',
  EXPIRED = 'EXPIRED'
}

export interface ConsentLedgerEntry {
  id: string;
  clientId: string;
  consentType: ConsentType;
  status: ConsentStatus;
  purpose: string;
  recipientOrganization?: string;
  recipientContact?: string;
  grantedAt: string;
  expiresAt?: string;
  revokedAt?: string;
  grantedByUserId: string;
  revokedByUserId?: string;
  revocationReason?: string;
  isVAWAProtected: boolean;
  limitations?: string;
  lastUpdatedAt: string;
  isExpired: boolean;
  isExpiringSoon: boolean;
  daysUntilExpiration: number;
}

export interface ConsentSearchParams {
  clientId?: string;
  consentType?: ConsentType;
  status?: ConsentStatus;
  recipientOrganization?: string;
  grantedAfter?: string;
  grantedBefore?: string;
  includeVAWAProtected?: boolean;
}

export interface ConsentAuditEntry {
  id: string;
  consentId: string;
  clientId: string;
  eventType: string;
  consentType?: ConsentType;
  actingUserId?: string;
  occurredAt: string;
  eventData: string;
  reason?: string;
  recipientOrganization?: string;
  ipAddress?: string;
  userAgent?: string;
}

export interface ConsentStatistics {
  totalConsents: number;
  activeConsents: number;
  revokedConsents: number;
  expiredConsents: number;
  expiringSoonCount: number;
  vAWAProtectedCount: number;
  typeBreakdown: ConsentTypeStatistics[];
  recentActivity: RecentActivitySummary;
}

export interface ConsentTypeStatistics {
  consentType: string;
  totalCount: number;
  activeCount: number;
}

export interface RecentActivitySummary {
  consentsGrantedLast30Days: number;
  consentsRevokedLast30Days: number;
  consentsExpiredLast30Days: number;
}

// Coordinated Entry Types

export type CeShareScope =
  | 'COC_COORDINATED_ENTRY'
  | 'HMIS_PARTICIPATION'
  | 'BY_NAME_LIST'
  | 'VAWA_RESTRICTED_PARTNERS'
  | 'SYSTEM_PERFORMANCE'
  | 'ADMIN_AUDIT';

export type CeHashAlgorithm = 'SHA256_SALT' | 'BCRYPT';

export type CeAssessmentType =
  | 'CRISIS_NEEDS'
  | 'HOUSING_NEEDS'
  | 'PREVENTION'
  | 'DIVERSION_PROBLEM_SOLVING'
  | 'TRANSFER'
  | 'YOUTH'
  | 'FAMILY'
  | 'OTHER';

export type CeAssessmentLevel = 'PRE_SCREEN' | 'FULL_ASSESSMENT' | 'POST_ASSESSMENT';

export type CePrioritizationStatus =
  | 'PRIORITIZED'
  | 'ACTIVE_NO_OPENING'
  | 'NOT_PRIORITIZED'
  | 'NO_LONGER_PRIORITY'
  | 'OTHER';

export type CeEventType =
  | 'REFERRAL_TO_PREVENTION'
  | 'REFERRAL_TO_STREET_OUTREACH'
  | 'REFERRAL_TO_NAVIGATION'
  | 'REFERRAL_TO_PH'
  | 'REFERRAL_TO_RRH'
  | 'REFERRAL_TO_ES'
  | 'EVENT_SAFETY_PLANNING'
  | 'EVENT_DIVERSION'
  | 'EVENT_OTHER';

export type CeEventStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CLOSED';

export type CeEventResult =
  | 'CLIENT_ACCEPTED'
  | 'CLIENT_DECLINED'
  | 'PROVIDER_DECLINED'
  | 'EXPIRED'
  | 'NO_CONTACT'
  | 'OTHER';

export interface CeAssessment {
  id: string;
  enrollmentId: string;
  clientId: string;
  assessmentDate: string;
  assessmentType: CeAssessmentType;
  assessmentLevel: CeAssessmentLevel | null;
  toolUsed: string | null;
  score: number | null;
  prioritizationStatus: CePrioritizationStatus | null;
  location: string | null;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  packetId: string | null;
  consentLedgerId: string | null;
  consentScope: CeShareScope[];
}

export interface CreateCeAssessmentRequest {
  clientId: string;
  assessmentDate: string;
  assessmentType: CeAssessmentType;
  assessmentLevel?: CeAssessmentLevel | null;
  toolUsed?: string | null;
  score?: number | null;
  prioritizationStatus?: CePrioritizationStatus | null;
  location?: string | null;
  consentId: string;
  consentLedgerId?: string | null;
  shareScopes?: CeShareScope[];
  hashAlgorithm?: CeHashAlgorithm;
  encryptionScheme?: string;
  encryptionKeyId: string;
  encryptionMetadata?: Record<string, string>;
  encryptionTags?: string[];
  createdBy: string;
  recipientOrganization?: string | null;
}

export interface CeEvent {
  id: string;
  enrollmentId: string;
  clientId: string;
  eventDate: string;
  eventType: CeEventType;
  result: CeEventResult | null;
  status: CeEventStatus;
  referralDestination: string | null;
  outcomeDate: string | null;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  packetId: string | null;
  consentLedgerId: string | null;
  consentScope: CeShareScope[];
}

export interface CreateCeEventRequest {
  clientId: string;
  eventDate: string;
  eventType: CeEventType;
  result?: CeEventResult | null;
  status: CeEventStatus;
  referralDestination?: string | null;
  outcomeDate?: string | null;
  consentId: string;
  consentLedgerId?: string | null;
  shareScopes?: CeShareScope[];
  hashAlgorithm?: CeHashAlgorithm;
  encryptionScheme?: string;
  encryptionKeyId: string;
  encryptionMetadata?: Record<string, string>;
  encryptionTags?: string[];
  createdBy: string;
}

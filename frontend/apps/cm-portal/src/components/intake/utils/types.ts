/**
 * VAWA-Compliant Intake Workflow Type Definitions
 * Version: 2.0
 *
 * This module defines stage-specific data interfaces for the 10-step intake workflow.
 * Each interface represents a discrete stage to enable data segregation and progressive
 * disclosure in compliance with VAWA confidentiality requirements.
 */

// =============================================================================
// STEP 1: INITIAL CONTACT / REFERRAL INTAKE
// =============================================================================

export interface InitialContactData {
  /** ISO 8601 date string for contact date */
  contactDate: string;

  /** Time of initial contact (HH:mm format) */
  contactTime: string;

  /** Source of referral */
  referralSource: ReferralSource;

  /** Additional details about referral source (required if OTHER selected) */
  referralSourceDetails?: string;

  /** Client alias or initials (NOT full name - collected after consent) */
  clientAlias: string;

  /** Client initials for quick reference (optional) */
  clientInitials?: string;

  /** Auto-generated temporary client ID (UUID) */
  tempClientId?: string;

  /** Immediate safety flag - asked during initial contact */
  safeToContactNow: boolean | null;

  /** If unsafe, route to crisis protocol */
  needsImmediateCrisisIntervention: boolean;

  /** Worker who made initial contact */
  intakeWorkerName: string;
}

export type ReferralSource =
  | 'SELF'           // Client self-referred
  | 'HOTLINE'        // DV hotline referral
  | 'AGENCY'         // Partner agency referral
  | 'OUTREACH'       // Outreach program
  | 'COURT'          // Court-ordered services
  | 'LAW_ENFORCEMENT'
  | 'HEALTHCARE'
  | 'SHELTER'
  | 'OTHER';

// =============================================================================
// STEP 2: SAFETY & CONSENT CHECK
// =============================================================================

export interface SafetyAndConsentData {
  /** Safe contact method preferences */
  safeContactMethods: SafeContactPreferences;

  /** Emergency contact information (optional, requires consent) */
  emergencyContact?: EmergencyContactInfo;

  /** All consent records with timestamps */
  consents: ConsentRecords;

  /** Digital signature for consent forms */
  digitalSignature?: DigitalSignature;
}

export interface SafeContactPreferences {
  /** Safe to call this client */
  okToCall: boolean;

  /** Safe to send text messages */
  okToText: boolean;

  /** Safe to send emails */
  okToEmail: boolean;

  /** Safe to leave voicemail messages */
  okToVoicemail: boolean;

  /** Primary safe phone number */
  safePhoneNumber?: string;

  /** Safe email address */
  safeEmail?: string;

  /** Security code word for identity verification */
  codeWord?: string;

  /** Time when client should NOT be contacted (24hr format) */
  quietHoursStart?: string;

  /** Time when contact can resume (24hr format) */
  quietHoursEnd?: string;

  /** Additional safety notes for staff */
  safetyNotes?: string;
}

export interface EmergencyContactInfo {
  /** Full name of emergency contact */
  name: string;

  /** Relationship to client */
  relationship: string;

  /** Contact phone number */
  phone: string;

  /** Contact email (optional) */
  email?: string;

  /** Client has consented to sharing information with this contact */
  consentToShare: boolean;

  /** Date consent was obtained */
  consentDate?: string;
}

export interface ConsentRecords {
  /** Consent to receive services */
  consentToServices: boolean;
  consentToServicesDate?: string;
  consentToServicesVersion?: string; // Track consent form version

  /** Consent to data collection and storage */
  consentToDataCollection: boolean;
  consentToDataCollectionDate?: string;

  /** Consent to HMIS participation */
  consentToHmis: boolean;
  consentToHmisDate?: string;

  /** HMIS participation status */
  hmisParticipationStatus: HmisParticipationStatus;

  /** Client is VAWA-exempt (affects HMIS data sharing) */
  vawaExempt: boolean;

  /** Consent to photo/video for marketing (optional) */
  consentToPhotoRelease?: boolean;
  consentToPhotoReleaseDate?: string;

  /** Consent to share data with partner agencies */
  consentToDataSharing?: boolean;
  consentToDataSharingDate?: string;
  consentToDataSharingAgencies?: string[];
}

export type HmisParticipationStatus =
  | 'PARTICIPATING'        // Full HMIS participation
  | 'NON_PARTICIPATING'    // Declined HMIS
  | 'PENDING'              // Decision not yet made
  | 'VAWA_EXEMPT';         // VAWA confidentiality protection

export interface DigitalSignature {
  /** Signature has been captured */
  signed: boolean;

  /** Base64-encoded signature image data */
  signatureData?: string;

  /** Typed name (alternative to drawn signature) */
  typedName?: string;

  /** IP address of signing device (for audit) */
  ipAddress?: string;

  /** ISO timestamp of signature */
  timestamp?: string;

  /** User agent string */
  userAgent?: string;
}

// =============================================================================
// STEP 3: CRISIS / RISK ASSESSMENT
// =============================================================================

export interface RiskAssessmentData {
  /** Lethality screening results */
  lethalityScreening: LethalityScreening;

  /** Immediate safety assessment */
  immediateSafety: ImmediateSafetyAssessment;

  /** Information about dependents */
  dependents: DependentsInfo;

  /** Auto-route to safety planning workflow if true */
  autoRouteToSafety: boolean;

  /** Overall risk level (auto-calculated or manual override) */
  overallRiskLevel: RiskLevel;

  /** Risk assessment completed by */
  assessedBy: string;

  /** Date and time of assessment */
  assessmentDateTime: string;
}

export interface LethalityScreening {
  /** Tool used for screening */
  screeningTool: LethalityScreeningTool;

  /** Numeric score (if tool produces one) */
  score?: number;

  /** Maximum possible score for context */
  maxScore?: number;

  /** Risk level based on screening */
  riskLevel: RiskLevel;

  /** Date screening was performed */
  assessmentDate: string;

  /** ID of staff who conducted assessment */
  assessorId: string;

  /** Assessor name for display */
  assessorName?: string;

  /** Individual question responses (for audit trail) */
  responses?: LethalityScreeningResponses;

  /** Additional notes from assessor */
  assessorNotes?: string;
}

export type LethalityScreeningTool =
  | 'DANGER_ASSESSMENT'    // Jacquelyn Campbell's Danger Assessment
  | 'ODARA'                // Ontario Domestic Assault Risk Assessment
  | 'DVSI'                 // Domestic Violence Screening Instrument
  | 'LAP'                  // Lethality Assessment Program
  | 'MOSAIC'               // MOSAIC Threat Assessment
  | 'OTHER'                // Other validated tool
  | 'NONE';                // No formal tool used

export type RiskLevel =
  | 'MINIMAL'              // No indicators present
  | 'LOW'                  // 1-2 indicators
  | 'MODERATE'             // 3-5 indicators
  | 'HIGH'                 // 6-8 indicators
  | 'SEVERE'               // 9+ indicators or immediate threat
  | 'NOT_ASSESSED';        // Assessment not yet completed

/**
 * Responses to lethality screening questions
 * Structure varies by tool; this is a flexible container
 */
export interface LethalityScreeningResponses {
  [questionId: string]: {
    question: string;
    answer: boolean | string | number;
    weight?: number; // For scoring
  };
}

export interface ImmediateSafetyAssessment {
  /** Is client currently in a safe location */
  currentlySafe: boolean | null;

  /** Does client have a safe place to stay tonight */
  safePlaceToStay: boolean | null;

  /** Client needs emergency shelter immediately */
  needsEmergencyShelter: boolean | null;

  /** Client has immediate medical needs */
  hasImmediateMedicalNeeds: boolean | null;

  /** Medical needs description */
  medicalNeedsDescription?: string;

  /** Police involvement status */
  policeInvolvement: PoliceInvolvementStatus;

  /** Police report number (if applicable) */
  policeReportNumber?: string;

  /** Protective order status */
  protectiveOrderStatus: ProtectiveOrderStatus;

  /** Date protective order was issued/expires */
  protectiveOrderDate?: string;

  /** Court case number for protective order */
  protectiveOrderCaseNumber?: string;

  /** Is client currently fleeing DV situation */
  currentlyFleeing: boolean | null;

  /** Date client fled (if applicable) */
  dateOfFlight?: string;

  /** Does client feel safe returning home */
  safeToReturnHome: boolean | null;

  /** Has client's location been compromised */
  locationCompromised: boolean | null;
}

export type PoliceInvolvementStatus =
  | 'NONE'                 // No police involvement
  | 'REPORT_FILED'         // Report filed but no active case
  | 'ONGOING_CASE'         // Active criminal case
  | 'CHARGES_PENDING'      // Charges filed, pending court
  | 'RESTRAINING_ORDER'    // Criminal restraining order in place
  | 'UNKNOWN';             // Client unsure or declined to answer

export type ProtectiveOrderStatus =
  | 'NONE'                 // No protective order
  | 'TEMPORARY'            // Temporary/emergency order in place
  | 'PERMANENT'            // Permanent order granted
  | 'EXPIRED'              // Had order but it expired
  | 'PENDING'              // Application pending
  | 'VIOLATED'             // Order in place but violated
  | 'UNKNOWN';             // Client unsure

export interface DependentsInfo {
  /** Client has minor children */
  hasMinors: boolean;

  /** Number of minor children */
  numberOfMinors?: number;

  /** Ages of minor children */
  minorAges?: number[];

  /** Any children under age 5 */
  hasInfants: boolean;

  /** Dependents with special needs */
  hasSpecialNeeds: boolean;

  /** Description of special needs */
  specialNeedsDetails?: string;

  /** Client has pets */
  hasPets: boolean;

  /** Pet details (type, number, special needs) */
  petDetails?: string;

  /** Are children currently safe */
  childrenCurrentlySafe: boolean | null;

  /** CPS involvement */
  cpsInvolvement: boolean | null;

  /** CPS case number */
  cpsCaseNumber?: string;
}

// =============================================================================
// STEP 4: ELIGIBILITY & PROGRAM MATCH
// =============================================================================

export interface EligibilityData {
  /** Homeless status per HUD definition */
  homelessStatus: HomelessStatusData;

  /** Income information for eligibility determination */
  income: IncomeData;

  /** Household composition */
  householdComposition: HouseholdCompositionData;

  /** Citizenship documentation (if required by funding) */
  citizenship: CitizenshipData;

  /** Calculated eligibility results */
  eligibilityResults: EligibilityResults;

  /** Staff override reason (if manual adjustment made) */
  overrideReason?: string;

  /** Selected program after matching */
  selectedProgram?: ProgramSelection;
}

export interface HomelessStatusData {
  /** Is client currently homeless */
  currentlyHomeless: boolean;

  /** HUD homeless definition category (24 CFR §578.3) */
  homelessCategory: HomelessCategory;

  /** Prior living situation (HUD code) */
  priorLivingSituation: string;

  /** Prior living situation code */
  priorLivingSituationCode?: string;

  /** Length of stay in prior situation */
  lengthOfStay: LengthOfStay;

  /** Date homelessness began (approximate OK) */
  dateHomelessBegan?: string;

  /** Approximate date flag */
  dateHomelessBeganApproximate: boolean;

  /** Times homeless in past 3 years */
  timesHomelessPast3Years?: number;

  /** Total months homeless in past 3 years */
  monthsHomelessPast3Years?: number;

  /** Reason for homelessness */
  reasonForHomelessness?: string[];

  /** Verification method */
  verificationMethod?: string;
}

export type HomelessCategory =
  | '1'  // Literally homeless
  | '2'  // Imminent risk of homelessness
  | '3'  // Homeless under other federal statutes
  | '4'  // Fleeing/attempting to flee DV
  | 'NOT_HOMELESS';

export type LengthOfStay =
  | 'ONE_NIGHT_OR_LESS'
  | 'TWO_TO_SIX_NIGHTS'
  | 'ONE_WEEK_TO_ONE_MONTH'
  | 'ONE_TO_THREE_MONTHS'
  | 'THREE_TO_SIX_MONTHS'
  | 'SIX_TO_TWELVE_MONTHS'
  | 'MORE_THAN_A_YEAR'
  | 'UNKNOWN';

export interface IncomeData {
  /** Client has any income */
  hasIncome: boolean;

  /** Monthly income amount (USD) */
  monthlyIncome?: number;

  /** Annual income (calculated or provided) */
  annualIncome?: number;

  /** Income sources */
  incomeSources?: IncomeSource[];

  /** Verification documents provided */
  verificationProvided: boolean;

  /** Verification method */
  verificationMethod?: string;

  /** Income below area median */
  belowAreaMedian: boolean | null;

  /** Percentage of area median income */
  percentOfAMI?: number;
}

export type IncomeSource =
  | 'EMPLOYMENT'
  | 'SSI'
  | 'SSDI'
  | 'TANF'
  | 'UNEMPLOYMENT'
  | 'CHILD_SUPPORT'
  | 'ALIMONY'
  | 'PENSION'
  | 'VETERANS_BENEFITS'
  | 'WORKERS_COMP'
  | 'OTHER'
  | 'NONE';

export interface HouseholdCompositionData {
  /** Number of adults (18+) */
  adults: number;

  /** Number of children (<18) */
  children: number;

  /** Total household size */
  totalSize: number;

  /** Household type classification */
  householdType: HouseholdType;

  /** Client is head of household */
  clientIsHoH: boolean;

  /** Household members (if entering as group) */
  members?: HouseholdMember[];
}

export type HouseholdType =
  | 'SINGLE_ADULT'
  | 'SINGLE_ADULT_WITH_CHILDREN'
  | 'COUPLE_NO_CHILDREN'
  | 'COUPLE_WITH_CHILDREN'
  | 'MULTI_GENERATIONAL'
  | 'OTHER';

export interface HouseholdMember {
  /** Relationship to head of household */
  relationshipToHoH: string;

  /** Age (or age range if exact unknown) */
  age?: number;

  /** Gender */
  gender?: string;

  /** Special needs */
  hasSpecialNeeds: boolean;
}

export interface CitizenshipData {
  /** Citizenship documentation required by funding source */
  documentationRequired: boolean;

  /** Documentation has been provided */
  documentationProvided?: boolean;

  /** Type of documentation provided */
  documentationType?: string;

  /** Funding source requiring documentation */
  fundingSourceRequirement?: string;

  /** Waiver applies */
  waiverApplies: boolean;

  /** Waiver reason */
  waiverReason?: string;
}

export interface EligibilityResults {
  /** Eligible for Transitional Housing */
  eligibleForTH: boolean;

  /** Eligible for Rapid Re-Housing */
  eligibleForRRH: boolean;

  /** Eligible for Permanent Supportive Housing */
  eligibleForPSH: boolean;

  /** Eligible for other programs (Emergency Shelter, Day Shelter, etc.) */
  eligibleForOther: string[];

  /** Recommended program ID (from matching algorithm) */
  recommendedProgramId?: string;

  /** Reason for recommendation */
  recommendationReason?: string;

  /** Reasons for ineligibility (if any) */
  ineligibilityReasons?: string[];

  /** Waitlist status for desired programs */
  waitlistStatus?: { [programId: string]: number }; // programId -> position in queue
}

export interface ProgramSelection {
  /** Selected program ID */
  programId: string;

  /** Program name */
  programName: string;

  /** Program type */
  programType: 'TH' | 'RRH' | 'PSH' | 'ES' | 'SSO' | 'OTHER';

  /** Funding source */
  fundingSource: string;

  /** Available capacity */
  availableSlots?: number;

  /** Expected entry date */
  expectedEntryDate?: string;

  /** Staff who made selection */
  selectedBy: string;

  /** Selection date */
  selectionDate: string;

  /** Daily rate associated with the program */
  dailyRate?: number;

  /** Primary program location / site */
  location?: string;
}

// =============================================================================
// STEP 5: HOUSING BARRIER ASSESSMENT
// =============================================================================

export interface HousingBarrierData {
  /** Rental history information */
  rentalHistory: RentalHistoryData;

  /** Credit and financial history */
  creditHistory: CreditHistoryData;

  /** Criminal background information */
  criminalBackground: CriminalBackgroundData;

  /** Employment status */
  employmentStatus: EmploymentStatusData;

  /** Support network */
  supportNetwork: SupportNetworkData;

  /** Transportation access */
  transportation: TransportationData;

  /** List of identified barriers */
  identifiedBarriers: string[];

  /** Housing stability plan (auto-generated recommendations) */
  stabilityPlan: StabilityPlan;

  /** Overall barrier severity */
  barrierSeverity: 'LOW' | 'MODERATE' | 'HIGH' | 'SEVERE';
}

export interface RentalHistoryData {
  /** Client has rental history */
  hasRentalHistory: boolean;

  /** Eviction history exists */
  evictionHistory: boolean;

  /** Number of evictions */
  evictionCount?: number;

  /** Date of most recent eviction */
  lastEvictionDate?: string;

  /** Landlord reference quality */
  landlordReferences: 'POSITIVE' | 'NEGATIVE' | 'MIXED' | 'NONE' | 'UNKNOWN';

  /** Landlord contact information (if positive) */
  landlordContacts?: Array<{ name: string; phone: string; dates: string }>;

  /** Rental debt amount */
  rentalDebt?: number;

  /** Utility debt */
  utilityDebt?: number;
}

export interface CreditHistoryData {
  /** Client disclosed credit history */
  disclosed: boolean;

  /** Credit score (if known) */
  creditScore?: number;

  /** Has collections */
  collections: boolean;

  /** Collections amount */
  collectionsAmount?: number;

  /** Bankruptcy history */
  bankruptcy: boolean;

  /** Bankruptcy type */
  bankruptcyType?: 'CHAPTER_7' | 'CHAPTER_13';

  /** Bankruptcy discharge date */
  bankruptcyDischargeDate?: string;

  /** Medical debt */
  medicalDebt: boolean;

  /** Student loan debt */
  studentLoanDebt: boolean;
}

export interface CriminalBackgroundData {
  /** Client disclosed criminal background */
  disclosed: boolean;

  /** Has criminal record */
  hasRecord?: boolean;

  /** Types of convictions */
  convictionTypes?: string[];

  /** Felony or misdemeanor */
  convictionLevel?: ('FELONY' | 'MISDEMEANOR')[];

  /** Most recent conviction date */
  mostRecentConvictionDate?: string;

  /** Release date (if incarcerated) */
  releaseDate?: string;

  /** On probation/parole */
  onSupervision: boolean;

  /** Probation/parole officer contact */
  supervisionOfficer?: { name: string; phone: string };

  /** Sex offender registry */
  registeredSexOffender: boolean;
}

export interface EmploymentStatusData {
  /** Currently employed */
  currentlyEmployed: boolean;

  /** Employment type */
  employmentType?: 'FULL_TIME' | 'PART_TIME' | 'SEASONAL' | 'TEMPORARY' | 'SELF_EMPLOYED' | 'GIG_WORK';

  /** Employer name */
  employer?: string;

  /** Job title */
  jobTitle?: string;

  /** Start date at current job */
  startDate?: string;

  /** Hours per week */
  hoursPerWeek?: number;

  /** Hourly wage */
  hourlyWage?: number;

  /** Looking for work */
  lookingForWork: boolean;

  /** Barriers to employment */
  employmentBarriers?: string[];

  /** Has resume */
  hasResume: boolean;

  /** Job training interest */
  interestedInTraining: boolean;

  /** Desired fields */
  desiredFields?: string[];
}

export interface SupportNetworkData {
  /** Has support network */
  hasSupportNetwork: boolean;

  /** Types of support available */
  networkTypes?: ('FAMILY' | 'FRIENDS' | 'FAITH_COMMUNITY' | 'COWORKERS' | 'NEIGHBORS' | 'OTHER')[];

  /** Can provide personal references */
  canProvideReferences: boolean;

  /** Number of references available */
  numberOfReferences?: number;

  /** Support network is local */
  localNetwork: boolean;

  /** Support network can provide housing assistance */
  networkCanHouse: boolean;

  /** Isolated from support network */
  isolated: boolean;
}

export interface TransportationData {
  /** Has reliable transportation */
  hasReliableTransportation: boolean;

  /** Transportation modes */
  transportationModes?: ('CAR' | 'PUBLIC_TRANSIT' | 'BIKE' | 'WALK' | 'RIDESHARE' | 'FRIEND_FAMILY' | 'OTHER')[];

  /** Has valid driver's license */
  driversLicense: boolean;

  /** License suspended */
  licenseSuspended: boolean;

  /** Has vehicle */
  hasVehicle: boolean;

  /** Vehicle reliable */
  vehicleReliable?: boolean;

  /** Vehicle insurance */
  vehicleInsurance?: boolean;

  /** Access to public transit */
  publicTransitAccess: boolean;

  /** Transportation costs per month */
  monthlyTransportCost?: number;
}

export interface StabilityPlan {
  /** Needs employment support */
  needsEmploymentSupport: boolean;

  /** Needs landlord mediation */
  needsLandlordMediation: boolean;

  /** Needs credit repair assistance */
  needsCreditRepair: boolean;

  /** Needs mental health referral */
  needsMentalHealthReferral: boolean;

  /** Needs substance abuse referral */
  needsSubstanceAbuseReferral: boolean;

  /** Needs legal advocacy */
  needsLegalAdvocacy: boolean;

  /** Needs financial literacy training */
  needsFinancialLiteracy: boolean;

  /** Needs childcare assistance */
  needsChildcare: boolean;

  /** Needs transportation assistance */
  needsTransportation: boolean;

  /** Priority interventions (ranked) */
  priorityInterventions: Array<{ intervention: string; priority: number }>;

  /** Estimated timeline to housing stability */
  estimatedTimeline?: '0_3_MONTHS' | '3_6_MONTHS' | '6_12_MONTHS' | 'OVER_12_MONTHS';
}

// =============================================================================
// STEP 6: SERVICE PLAN & CASE ASSIGNMENT
// =============================================================================

export interface ServicePlanData {
  /** Assigned case manager */
  assignedCaseManager?: CaseManagerAssignment;

  /** Client goals */
  goals: ClientGoal[];

  /** Client strengths (empowerment focus) */
  clientStrengths: string[];

  /** Client aspirations (free text) */
  clientAspirations: string;

  /** Follow-up schedule */
  followUpSchedule: FollowUpSchedule;

  /** Digital goal agreement */
  digitalGoalAgreement?: DigitalSignature;

  /** Service plan created date */
  createdDate: string;

  /** Service plan version */
  version: number;
}

export interface CaseManagerAssignment {
  /** Case manager ID */
  id: string;

  /** Case manager name */
  name: string;

  /** Email */
  email: string;

  /** Phone */
  phone: string;

  /** Assignment date */
  assignmentDate: string;

  /** Caseload size */
  currentCaseload?: number;

  /** Specializations */
  specializations?: string[];
}

export interface ClientGoal {
  /** Unique goal ID */
  id: string;

  /** Goal category */
  category: GoalCategory;

  /** Goal description */
  description: string;

  /** Specific, measurable outcome */
  measurableOutcome: string;

  /** Target completion date */
  targetDate?: string;

  /** Priority level */
  priority: 'HIGH' | 'MEDIUM' | 'LOW';

  /** Current status */
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'ABANDONED';

  /** Action steps */
  actionSteps?: string[];

  /** Responsible party */
  responsibleParty: 'CLIENT' | 'CASE_MANAGER' | 'BOTH' | 'EXTERNAL';

  /** Progress notes */
  progressNotes?: Array<{ date: string; note: string; author: string }>;

  /** Completion date (if completed) */
  completionDate?: string;
}

export type GoalCategory =
  | 'HOUSING_SEARCH'
  | 'EMPLOYMENT'
  | 'EDUCATION'
  | 'COUNSELING'
  | 'BENEFITS_APPLICATION'
  | 'LEGAL_ADVOCACY'
  | 'HEALTH_CARE'
  | 'CHILDCARE'
  | 'TRANSPORTATION'
  | 'FINANCIAL_STABILITY'
  | 'SAFETY_PLANNING'
  | 'LIFE_SKILLS'
  | 'OTHER';

export interface FollowUpSchedule {
  /** 30-day check-in */
  day30: boolean;

  /** 60-day check-in */
  day60: boolean;

  /** 90-day assessment */
  day90: boolean;

  /** Custom follow-up dates */
  customDates?: string[];

  /** Preferred contact method for follow-ups */
  preferredContactMethod?: 'PHONE' | 'EMAIL' | 'IN_PERSON' | 'VIDEO';

  /** Preferred contact time */
  preferredContactTime?: string;
}

// =============================================================================
// STEP 7: DOCUMENTATION UPLOADS
// =============================================================================

export interface DocumentationData {
  /** Required consent documents */
  requiredDocuments: RequiredDocuments;

  /** Optional supporting documents */
  optionalDocuments: OptionalDocuments;

  /** Document tags for access control */
  documentTags: Record<string, string[]>;

  /** Auto-deletion rules for temporary docs */
  expirationRules: Record<string, number>;

  /** Document upload metadata */
  uploadMetadata: Record<string, DocumentMetadata>;
}

export interface RequiredDocuments {
  /** VAWA consent form */
  vawaConsent: DocumentStatus;

  /** HMIS consent form */
  hmisConsent: DocumentStatus;

  /** Photo/video release consent */
  photoRelease: DocumentStatus;

  /** Service agreement */
  serviceAgreement: DocumentStatus;

  /** ROI (Release of Information) for partner agencies */
  releaseOfInformation?: DocumentStatus;
}

export interface OptionalDocuments {
  /** Photo ID */
  photoId: DocumentStatus;

  /** Birth certificate */
  birthCertificate: DocumentStatus;

  /** Social Security card */
  ssnCard: DocumentStatus;

  /** Insurance card */
  insuranceCard: DocumentStatus;

  /** Income verification (paystubs, benefit letters) */
  incomeVerification: DocumentStatus;

  /** Lease agreement */
  leaseAgreement: DocumentStatus;

  /** Protective order documentation */
  protectiveOrder: DocumentStatus;

  /** Other documents */
  other: Record<string, DocumentStatus>;
}

export interface DocumentStatus {
  /** Document uploaded */
  uploaded: boolean;

  /** File ID in storage system */
  fileId?: string;

  /** Original filename */
  filename?: string;

  /** Upload timestamp */
  uploadDate?: string;

  /** Uploaded by */
  uploadedBy?: string;

  /** Document verified by staff */
  verified: boolean;

  /** Verification date */
  verificationDate?: string;

  /** Waived (not required) */
  waived: boolean;

  /** Waiver reason */
  waiverReason?: string;
}

export interface DocumentMetadata {
  /** File size in bytes */
  fileSize: number;

  /** MIME type */
  mimeType: string;

  /** Encryption status */
  encrypted: boolean;

  /** Encryption algorithm */
  encryptionAlgorithm?: string;

  /** Access log */
  accessLog: Array<{ userId: string; timestamp: string; action: string }>;

  /** Expiration date (for auto-deletion) */
  expirationDate?: string;

  /** Retention policy */
  retentionPolicy?: string;
}

// =============================================================================
// STEP 8: DEMOGRAPHICS & OUTCOME BASELINE
// =============================================================================

export interface DemographicsBaselineData {
  /** Full legal name (NOW collected after consent) */
  name: LegalName;

  /** Government identifiers */
  identifiers: GovernmentIdentifiers;

  /** Demographic information */
  demographics: DemographicInfo;

  /** Veteran status */
  veteranStatus: VeteranStatus;

  /** Disabling condition */
  disablingCondition: DisablingCondition;

  /** HMIS pseudonymization data */
  pseudonymization: PseudonymizationData;

  /** Contact information (from Step 2, confirmed here) */
  contactInfo: ContactInformation;
}

export interface LegalName {
  /** Legal first name */
  firstName: string;

  /** Middle name */
  middleName?: string;

  /** Legal last name */
  lastName: string;

  /** Suffix (Jr., Sr., III, etc.) */
  suffix?: string;

  /** Preferred/chosen name */
  preferredName?: string;

  /** Name data quality */
  nameDataQuality: DataQuality;
}

export interface GovernmentIdentifiers {
  /** Social Security Number */
  socialSecurityNumber?: string;

  /** SSN data quality */
  ssnDataQuality: DataQuality;

  /** State ID number */
  stateIdNumber?: string;

  /** State ID issuing state */
  stateIdState?: string;

  /** Date of birth */
  birthDate: string;

  /** DOB data quality */
  dobDataQuality: DataQuality;

  /** Birth place (city, state) */
  birthPlace?: string;
}

export type DataQuality =
  | 1   // Full, verified
  | 2   // Partial or approximate
  | 8   // Client doesn't know
  | 9   // Client refused
  | 99; // Data not collected

export interface DemographicInfo {
  /** Date of birth */
  birthDate: string;

  /** Calculated age */
  age?: number;

  /** Administrative gender (for records) */
  gender: string;

  /** HMIS gender (self-identified, multi-select) */
  hmisGender: string[];

  /** Race (multi-select) */
  race: string[];

  /** Ethnicity */
  ethnicity: string;

  /** Primary language */
  primaryLanguage: string;

  /** Interpreter needed */
  interpreterNeeded: boolean;

  /** Preferred language for services */
  preferredLanguage?: string;
}

export type VeteranStatus =
  | 'YES'
  | 'NO'
  | 'CLIENT_DOESNT_KNOW'
  | 'CLIENT_REFUSED'
  | 'DATA_NOT_COLLECTED';

export type DisablingCondition =
  | 'YES'
  | 'NO'
  | 'CLIENT_DOESNT_KNOW'
  | 'CLIENT_REFUSED'
  | 'DATA_NOT_COLLECTED';

export interface PseudonymizationData {
  /** HMIS-safe client ID (hashed or pseudonymized) */
  hmisClientId?: string;

  /** VAWA protected status (restricts data sharing) */
  vawaProtected: boolean;

  /** Pseudonymization method */
  pseudonymizationMethod?: 'HASHED' | 'TOKENIZED' | 'ENCRYPTED';

  /** Export restricted */
  exportRestricted: boolean;
}

export interface ContactInformation {
  /** Phone numbers */
  phones: Array<{
    type: 'HOME' | 'MOBILE' | 'WORK';
    number: string;
    primary: boolean;
  }>;

  /** Email addresses */
  emails: Array<{
    type: 'PERSONAL' | 'WORK';
    address: string;
    primary: boolean;
  }>;

  /** Mailing address */
  mailingAddress?: Address;

  /** Residential address (may differ from mailing) */
  residentialAddress?: Address;

  /** Address confidentiality flag */
  addressConfidential: boolean;
}

export interface Address {
  /** Street address line 1 */
  line1: string;

  /** Street address line 2 (apt, unit, etc.) */
  line2?: string;

  /** City */
  city: string;

  /** State/province */
  state: string;

  /** ZIP/postal code */
  postalCode: string;

  /** Country */
  country: string;

  /** Address type */
  type: 'HOME' | 'WORK' | 'SHELTER' | 'MAILING' | 'OTHER';

  /** Address is confidential (Safe at Home, etc.) */
  confidential: boolean;
}

// =============================================================================
// STEP 9: SERVICE ENROLLMENT CONFIRMATION
// =============================================================================

export interface EnrollmentConfirmationData {
  /** Enrollment details */
  enrollment: EnrollmentDetails;

  /** Cost allocation for funding tracking */
  costAllocation: CostAllocation;

  /** Enrollment status */
  enrollmentStatus: EnrollmentStatus;

  /** Staff confirmation */
  staffConfirmation: StaffConfirmation;
}

export interface EnrollmentDetails {
  /** Entry date (24 CFR §578.103(a)) */
  entryDate: string;

  /** Project type */
  projectType: ProjectType;

  /** Project ID */
  projectId: string;

  /** Project name */
  projectName: string;

  /** Funding source */
  fundingSource: string;

  /** Relationship to head of household */
  relationshipToHoH: string;

  /** Enrollment ID (once created) */
  enrollmentId?: string;

  /** Move-in date (for RRH/PSH) */
  moveInDate?: string;
}

export type ProjectType =
  | 'ES'    // Emergency Shelter
  | 'TH'    // Transitional Housing
  | 'RRH'   // Rapid Re-Housing
  | 'PSH'   // Permanent Supportive Housing
  | 'SO'    // Safe Haven
  | 'PH'    // Other Permanent Housing
  | 'DAY'   // Day Shelter
  | 'SSO'   // Services Only
  | 'HP'    // Homelessness Prevention
  | 'CE';   // Coordinated Entry

export interface CostAllocation {
  /** Daily rate for this client */
  dailyRate?: number;

  /** Funding start date */
  fundingStartDate: string;

  /** Anticipated end date */
  anticipatedEndDate?: string;

  /** Total anticipated cost */
  totalAnticipatedCost?: number;

  /** Funding source details */
  fundingSourceDetails?: {
    source: string;
    grantNumber: string;
    availableFunds: number;
  };
}

export type EnrollmentStatus =
  | 'ACTIVE'              // Enrolled and receiving services
  | 'PENDING_APPROVAL'    // Enrollment submitted, awaiting approval
  | 'WAITLISTED'          // On waitlist for program
  | 'DENIED'              // Enrollment denied
  | 'WITHDRAWN';          // Client withdrew application

export interface StaffConfirmation {
  /** Staff member who confirmed enrollment */
  confirmedBy: string;

  /** Confirmation timestamp */
  confirmationDate: string;

  /** All information reviewed and accurate */
  accuracyConfirmed: boolean;

  /** HMIS entry confirmed */
  hmisEntryConfirmed: boolean;

  /** Client notified of enrollment */
  clientNotified: boolean;

  /** Client notification method */
  notificationMethod?: 'PHONE' | 'EMAIL' | 'IN_PERSON' | 'LETTER';
}

// =============================================================================
// STEP 10: FOLLOW-UP / CONTINUOUS REASSESSMENT
// =============================================================================

export interface FollowUpConfigData {
  /** Reassessment schedule configuration */
  reassessmentSchedule: ReassessmentSchedule;

  /** Audit protection settings */
  auditProtection: AuditProtectionSettings;

  /** HUD reporting readiness */
  reportingReadiness: ReportingReadiness;

  /** Next steps for client */
  nextSteps: NextSteps;
}

export interface ReassessmentSchedule {
  /** Monthly check-ins enabled */
  monthlyCheckIns: boolean;

  /** 90-day review enabled */
  day90Review: boolean;

  /** Annual assessment enabled */
  annualAssessment: boolean;

  /** Custom schedule dates */
  customSchedule?: Array<{
    date: string;
    type: string;
    description: string;
  }>;

  /** Reminder method */
  reminderMethod: 'EMAIL' | 'SMS' | 'PHONE' | 'IN_APP';

  /** Days before reminder */
  reminderDaysBefore: number;
}

export interface AuditProtectionSettings {
  /** Lock previous assessments from editing */
  lockPreviousAssessments: boolean;

  /** Prevent retroactive edits */
  preventRetroactiveEdits: boolean;

  /** Audit log retention period (days) */
  auditLogRetention: number;

  /** Version history enabled */
  versionHistoryEnabled: boolean;
}

export interface ReportingReadiness {
  /** Ready for Annual Performance Report */
  aprReady: boolean;

  /** APR missing fields */
  aprMissingFields?: string[];

  /** Ready for CAPER */
  caperReady: boolean;

  /** CAPER missing fields */
  caperMissingFields?: string[];

  /** Data quality score (0-100) */
  dataQualityScore?: number;

  /** Data completeness percentage */
  dataCompleteness?: number;
}

export interface NextSteps {
  /** Upcoming appointments */
  upcomingAppointments: Array<{
    date: string;
    type: string;
    description: string;
    reminderSet: boolean;
  }>;

  /** Pending tasks for client */
  clientTasks: string[];

  /** Pending tasks for case manager */
  caseManagerTasks: string[];

  /** Next reassessment date */
  nextReassessmentDate: string;
}

// =============================================================================
// MASTER INTAKE DATA (Composite of all steps)
// =============================================================================

export interface MasterIntakeData {
  /** Step 1: Initial Contact */
  step1_initialContact: InitialContactData;

  /** Step 2: Safety & Consent */
  step2_safetyConsent: SafetyAndConsentData;

  /** Step 3: Risk Assessment */
  step3_riskAssessment: RiskAssessmentData;

  /** Step 4: Eligibility */
  step4_eligibility: EligibilityData;

  /** Step 5: Housing Barriers */
  step5_housingBarriers: HousingBarrierData;

  /** Step 6: Service Plan */
  step6_servicePlan: ServicePlanData;

  /** Step 7: Documentation */
  step7_documentation: DocumentationData;

  /** Step 8: Demographics */
  step8_demographics: DemographicsBaselineData;

  /** Step 9: Enrollment */
  step9_enrollment: EnrollmentConfirmationData;

  /** Step 10: Follow-up */
  step10_followUp: FollowUpConfigData;

  /** Overall intake metadata */
  intakeMetadata: IntakeMetadata;
}

export interface IntakeMetadata {
  /** Intake worker ID */
  intakeWorkerId: string;

  /** Intake worker name */
  intakeWorkerName: string;

  /** Date intake started */
  intakeDate: string;

  /** Location where intake conducted */
  intakeLocation: string;

  /** Data collection stage */
  dataCollectionStage: string;

  /** Workflow version */
  workflowVersion: string;

  /** Completion status */
  completionStatus: 'IN_PROGRESS' | 'COMPLETED' | 'ABANDONED';

  /** Date completed */
  completionDate?: string;

  /** Total time spent on intake (minutes) */
  totalTimeSpent?: number;

  /** Steps completed */
  stepsCompleted: number[];

  /** Last updated timestamp */
  lastUpdated: string;
}

// =============================================================================
// UTILITY TYPES
// =============================================================================

export interface ValidationError {
  field: string;
  message: string;
  severity: 'ERROR' | 'WARNING';
}

export interface StepValidationResult {
  isValid: boolean;
  errors: ValidationError[];
  warnings: ValidationError[];
}

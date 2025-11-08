/**
 * VAWA-Compliant Intake Workflow
 * Version: 2.0
 *
 * Central export point for all intake workflow modules.
 * This file provides convenient access to types, constants, validation,
 * and business logic functions.
 */

// =============================================================================
// TYPE EXPORTS
// =============================================================================

export type {
  // Step 1: Initial Contact
  InitialContactData,
  ReferralSource,

  // Step 2: Safety & Consent
  SafetyAndConsentData,
  SafeContactPreferences,
  EmergencyContactInfo,
  ConsentRecords,
  HmisParticipationStatus,
  DigitalSignature,

  // Step 3: Risk Assessment
  RiskAssessmentData,
  LethalityScreening,
  LethalityScreeningTool,
  RiskLevel,
  LethalityScreeningResponses,
  ImmediateSafetyAssessment,
  PoliceInvolvementStatus,
  ProtectiveOrderStatus,
  DependentsInfo,

  // Step 4: Eligibility
  EligibilityData,
  HomelessStatusData,
  HomelessCategory,
  LengthOfStay,
  IncomeData,
  IncomeSource,
  HouseholdCompositionData,
  HouseholdType,
  HouseholdMember,
  CitizenshipData,
  EligibilityResults,
  ProgramSelection,

  // Step 5: Housing Barriers
  HousingBarrierData,
  RentalHistoryData,
  CreditHistoryData,
  CriminalBackgroundData,
  EmploymentStatusData,
  SupportNetworkData,
  TransportationData,
  StabilityPlan,

  // Step 6: Service Plan
  ServicePlanData,
  CaseManagerAssignment,
  ClientGoal,
  GoalCategory,
  FollowUpSchedule,

  // Step 7: Documentation
  DocumentationData,
  RequiredDocuments,
  OptionalDocuments,
  DocumentStatus,
  DocumentMetadata,

  // Step 8: Demographics
  DemographicsBaselineData,
  LegalName,
  GovernmentIdentifiers,
  DataQuality,
  DemographicInfo,
  VeteranStatus,
  DisablingCondition,
  PseudonymizationData,
  ContactInformation,
  Address,

  // Step 9: Enrollment
  EnrollmentConfirmationData,
  EnrollmentDetails,
  ProjectType,
  CostAllocation,
  EnrollmentStatus,
  StaffConfirmation,

  // Step 10: Follow-up
  FollowUpConfigData,
  ReassessmentSchedule,
  AuditProtectionSettings,
  ReportingReadiness,
  NextSteps,

  // Master data structure
  MasterIntakeData,
  IntakeMetadata,

  // Validation types
  ValidationError,
  StepValidationResult,
} from './utils/types';

// =============================================================================
// CONSTANT EXPORTS
// =============================================================================

export {
  // HUD Codes
  HOMELESS_CATEGORIES,
  PRIOR_LIVING_SITUATIONS,
  PROJECT_TYPES,
  RELATIONSHIP_TO_HOH,
  RACE_CODES,
  ETHNICITY_CODES,
  GENDER_CODES,

  // HMIS Enums
  HMIS_PARTICIPATION_STATUS,
  VETERAN_STATUS,
  DISABLING_CONDITION,
  DATA_QUALITY,
  ENROLLMENT_STATUS,

  // Referral & Assessment
  REFERRAL_SOURCES,
  LETHALITY_SCREENING_TOOLS,
  RISK_LEVELS,
  POLICE_INVOLVEMENT_STATUS,
  PROTECTIVE_ORDER_STATUS,

  // Income & Housing
  INCOME_SOURCES,
  LENGTH_OF_STAY,
  HOUSEHOLD_TYPES,
  HOUSING_BARRIERS,
  CLIENT_STRENGTHS,

  // Service Planning
  GOAL_CATEGORIES,

  // Documentation
  DOCUMENT_TAGS,
  DOCUMENT_RETENTION,

  // Workflow Configuration
  WORKFLOW_STEPS,
  TOTAL_ESTIMATED_MINUTES,

  // Feature Flags & Defaults
  FEATURE_FLAGS,
  DEFAULTS,

  // Validation Rules
  VALIDATION,

  // UI Constants
  UI,

  // Security
  SECURITY,

  // API Endpoints
  API_ENDPOINTS,

  // Error Severity
  ERROR_SEVERITY,
  SEVERITY_CONFIG,
  ERROR_EXAMPLES,
  WARNING_EXAMPLES,
  type ErrorSeverity,

  // Messages
  ERROR_MESSAGES,
  WARNING_MESSAGES,
  SUCCESS_MESSAGES,

  // Type helpers
  type HousingBarrier,
  type ClientStrength,
  type DocumentTag,
  type FeatureFlag,
  type WorkflowStep,
} from './utils/constants';

// =============================================================================
// VALIDATION EXPORTS
// =============================================================================

export {
  // Individual step validators
  validateStep1,
  validateStep2,
  validateStep3,
  validateStep4,
  validateStep5,
  validateStep6,
  validateStep7,
  validateStep8,
  validateStep9,
  validateStep10,

  // Master validator
  validateAllSteps,

  // Grouped export
  IntakeValidation,
} from './lib/validation';

// =============================================================================
// BUSINESS LOGIC EXPORTS
// =============================================================================

export {
  // Risk Assessment
  calculateOverallRiskLevel,
  shouldAutoRouteToSafety,

  // Eligibility
  determineEligibility,
  isChronicallyHomeless,

  // Housing Barriers
  calculateBarrierSeverity,
  generateStabilityPlan,

  // Data Quality
  calculateDataQualityScore,
  checkAprReadiness,
  checkCaperReadiness,

  // Utilities
  calculateAge,
  formatPhoneNumber,
  formatSSN,
  generateHmisClientId,
  calculatePercentOfAMI,
  isExtremelyLowIncome,
  generateTempClientId,
  isMinor,
  isYouth,
  calculateStepCompleteness,

  // Grouped export
  IntakeBusinessLogic,
} from './lib/businessLogic';

// =============================================================================
// INTEGRATION HELPERS
// =============================================================================

/**
 * Hook for step validation with automatic error/warning state management
 *
 * @example
 * ```typescript
 * const { validate, errors, warnings, isValid } = useStepValidation(validateStep1);
 *
 * const handleNext = () => {
 *   if (validate(formData)) {
 *     onComplete(formData);
 *   }
 * };
 * ```
 */
export function createStepValidator<T>(
  validateFn: (data: T) => StepValidationResult
) {
  return (data: T) => {
    const result = validateFn(data);
    return {
      ...result,
      hasErrors: result.errors.length > 0,
      hasWarnings: result.warnings.length > 0,
      errorCount: result.errors.length,
      warningCount: result.warnings.length,
    };
  };
}

/**
 * Get validation result for a specific field
 *
 * @example
 * ```typescript
 * const emailError = getFieldError('safeEmail', validationResult.errors);
 * ```
 */
export function getFieldError(
  fieldName: string,
  errors: ValidationError[]
): ValidationError | undefined {
  return errors.find(error => error.field === fieldName);
}

/**
 * Get all warnings for a specific field
 */
export function getFieldWarnings(
  fieldName: string,
  warnings: ValidationError[]
): ValidationError[] {
  return warnings.filter(warning => warning.field === fieldName);
}

/**
 * Check if a specific field has errors
 */
export function hasFieldError(
  fieldName: string,
  errors: ValidationError[]
): boolean {
  return errors.some(error => error.field === fieldName);
}

/**
 * Get error message for a field (returns first error only)
 */
export function getFieldErrorMessage(
  fieldName: string,
  errors: ValidationError[]
): string | undefined {
  const error = getFieldError(fieldName, errors);
  return error?.message;
}

/**
 * Group errors by field name
 */
export function groupErrorsByField(
  errors: ValidationError[]
): Record<string, ValidationError[]> {
  return errors.reduce((acc, error) => {
    const field = error.field;
    if (!acc[field]) {
      acc[field] = [];
    }
    acc[field].push(error);
    return acc;
  }, {} as Record<string, ValidationError[]>);
}

/**
 * Check if intake is complete enough to submit
 */
export function canSubmitIntake(masterData: Partial<MasterIntakeData>): {
  canSubmit: boolean;
  reason?: string;
  missingSteps: number[];
} {
  const missingSteps: number[] = [];
  let reason: string | undefined;

  // Check required steps
  if (!masterData.step1_initialContact) {
    missingSteps.push(1);
  }

  if (!masterData.step2_safetyConsent) {
    missingSteps.push(2);
  }

  if (!masterData.step3_riskAssessment) {
    missingSteps.push(3);
  }

  if (!masterData.step4_eligibility) {
    missingSteps.push(4);
  }

  if (!masterData.step6_servicePlan) {
    missingSteps.push(6);
  }

  if (!masterData.step7_documentation) {
    missingSteps.push(7);
  }

  if (!masterData.step8_demographics) {
    missingSteps.push(8);
  }

  if (!masterData.step9_enrollment) {
    missingSteps.push(9);
  }

  // Check for consent (BLOCKING)
  if (!masterData.step2_safetyConsent?.consents.consentToServices) {
    reason = 'Client consent to services is required';
    return { canSubmit: false, reason, missingSteps };
  }

  // If any required steps missing
  if (missingSteps.length > 0) {
    reason = `Missing required steps: ${missingSteps.join(', ')}`;
    return { canSubmit: false, reason, missingSteps };
  }

  return { canSubmit: true, missingSteps: [] };
}

/**
 * Calculate overall intake progress percentage
 */
export function calculateIntakeProgress(masterData: Partial<MasterIntakeData>): {
  percentage: number;
  completedSteps: number;
  totalSteps: number;
  nextStep: number | null;
} {
  const totalSteps = 10;
  let completedSteps = 0;
  let nextStep: number | null = null;

  const stepChecks = [
    !!masterData.step1_initialContact,
    !!masterData.step2_safetyConsent,
    !!masterData.step3_riskAssessment,
    !!masterData.step4_eligibility,
    !!masterData.step5_housingBarriers,
    !!masterData.step6_servicePlan,
    !!masterData.step7_documentation,
    !!masterData.step8_demographics,
    !!masterData.step9_enrollment,
    !!masterData.step10_followUp,
  ];

  stepChecks.forEach((isComplete, index) => {
    if (isComplete) {
      completedSteps++;
    } else if (nextStep === null) {
      nextStep = index + 1;
    }
  });

  const percentage = Math.round((completedSteps / totalSteps) * 100);

  return {
    percentage,
    completedSteps,
    totalSteps,
    nextStep,
  };
}

/**
 * Get workflow step configuration by step number
 */
export function getStepConfig(stepNumber: number) {
  return WORKFLOW_STEPS.find(step => step.id === stepNumber);
}

// =============================================================================
// COMPONENT EXPORTS
// =============================================================================

export {
  Step1_InitialContact,
  Step2_SafetyAndConsent,
  Step3_RiskAssessment,
  Step4_EligibilityMatch,
  Step5_HousingBarriers,
  Step6_ServicePlan,
  Step7_DocumentUpload,
  Step8_Demographics,
  Step9_EnrollmentConfirmation,
  Step10_FollowUpConfig,
  ReviewStep,
} from './components';

/**
 * Check if step is VAWA-compliant (can be completed before full consent)
 */
export function isVawaCompliantStep(stepNumber: number): boolean {
  const step = getStepConfig(stepNumber);
  return step?.vawaCompliant ?? false;
}

/**
 * Get required steps (excludes optional steps like step 10)
 */
export function getRequiredSteps() {
  return WORKFLOW_STEPS.filter(step => step.required);
}

/**
 * Format validation errors for display in UI
 */
export function formatValidationErrors(errors: ValidationError[]): string[] {
  return errors.map(error => `${error.field}: ${error.message}`);
}

/**
 * Check if risk level requires immediate action
 */
export function requiresImmediateAction(riskLevel: RiskLevel): boolean {
  return riskLevel === 'SEVERE' || riskLevel === 'HIGH';
}

/**
 * Get recommended next action based on intake state
 */
export function getRecommendedNextAction(masterData: Partial<MasterIntakeData>): {
  action: string;
  description: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
} {
  // Check for crisis situations
  if (masterData.step3_riskAssessment) {
    const { overallRiskLevel } = masterData.step3_riskAssessment;
    if (requiresImmediateAction(overallRiskLevel)) {
      return {
        action: 'SAFETY_PLANNING',
        description: 'Complete immediate safety planning and crisis intervention',
        priority: 'HIGH',
      };
    }
  }

  // Check for incomplete required steps
  const { missingSteps } = canSubmitIntake(masterData);
  if (missingSteps.length > 0) {
    const nextStep = Math.min(...missingSteps);
    const stepConfig = getStepConfig(nextStep);
    return {
      action: 'COMPLETE_INTAKE',
      description: `Complete ${stepConfig?.title || `Step ${nextStep}`}`,
      priority: 'HIGH',
    };
  }

  // Check for pending case manager assignment
  if (!masterData.step6_servicePlan?.assignedCaseManager) {
    return {
      action: 'ASSIGN_CASE_MANAGER',
      description: 'Assign case manager to client',
      priority: 'HIGH',
    };
  }

  // Check for enrollment confirmation
  if (masterData.step9_enrollment?.enrollmentStatus === 'PENDING_APPROVAL') {
    return {
      action: 'CONFIRM_ENROLLMENT',
      description: 'Complete enrollment confirmation',
      priority: 'MEDIUM',
    };
  }

  // Default to follow-up configuration
  return {
    action: 'CONFIGURE_FOLLOWUP',
    description: 'Set up follow-up and reassessment schedule',
    priority: 'LOW',
  };
}

// =============================================================================
// DEFAULT EXPORT (OPTIONAL CONVENIENCE EXPORT)
// =============================================================================
// Note: Default export removed to avoid circular dependency issues during module initialization.
// All exports are available as named exports instead.

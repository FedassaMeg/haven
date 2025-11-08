/**
 * VAWA-Compliant Intake Workflow Business Logic
 * Version: 2.0
 *
 * This module provides business logic, auto-calculations, and decision-making functions
 * for the intake workflow.
 */

import type {
  RiskLevel,
  EligibilityData,
  EligibilityResults,
  HousingBarrierData,
  StabilityPlan,
  MasterIntakeData,
  HomelessCategory,
} from '../utils/types';

import { DEFAULTS } from '../utils/constants';

// =============================================================================
// RISK ASSESSMENT LOGIC
// =============================================================================

/**
 * Calculate overall risk level based on multiple factors
 * Escalates risk when immediate safety concerns are present
 */
export function calculateOverallRiskLevel(
  lethalityRiskLevel: RiskLevel,
  currentlySafe: boolean | null,
  safePlaceToStay: boolean | null,
  needsEmergencyShelter: boolean | null
): RiskLevel {
  // If lethality assessment is SEVERE, always return SEVERE
  if (lethalityRiskLevel === 'SEVERE') {
    return 'SEVERE';
  }

  // Escalate if client is currently unsafe
  if (currentlySafe === false) {
    // If already HIGH or SEVERE, keep it; otherwise escalate to HIGH
    if (lethalityRiskLevel === 'HIGH' || lethalityRiskLevel === 'SEVERE') {
      return lethalityRiskLevel;
    }
    return 'HIGH';
  }

  // Escalate if no safe place to stay or needs emergency shelter
  if (safePlaceToStay === false || needsEmergencyShelter === true) {
    // Escalate by one level if currently MODERATE or below
    switch (lethalityRiskLevel) {
      case 'MINIMAL':
      case 'LOW':
        return 'MODERATE';
      case 'MODERATE':
        return 'HIGH';
      case 'HIGH':
      case 'SEVERE':
        return lethalityRiskLevel;
      default:
        return 'MODERATE';
    }
  }

  // Otherwise, return the lethality screening risk level
  return lethalityRiskLevel;
}

/**
 * Determine if client should be auto-routed to safety planning workflow
 * This triggers immediate crisis intervention protocols
 */
export function shouldAutoRouteToSafety(
  riskLevel: RiskLevel,
  currentlySafe: boolean | null,
  needsEmergencyShelter: boolean | null,
  childrenCurrentlySafe: boolean | null
): boolean {
  // Auto-route if risk is SEVERE or HIGH
  if (riskLevel === 'SEVERE' || riskLevel === 'HIGH') {
    return true;
  }

  // Auto-route if client is currently unsafe
  if (currentlySafe === false) {
    return true;
  }

  // Auto-route if client needs emergency shelter
  if (needsEmergencyShelter === true) {
    return true;
  }

  // Auto-route if children are unsafe (mandatory reporting territory)
  if (childrenCurrentlySafe === false) {
    return true;
  }

  return false;
}

// =============================================================================
// ELIGIBILITY DETERMINATION LOGIC
// =============================================================================

/**
 * Determine program eligibility based on client data
 * NOTE: Production systems should use backend logic with full funding rules
 * This is a simplified client-side version for UI guidance
 */
export function determineEligibility(data: Partial<EligibilityData>): EligibilityResults {
  const results: EligibilityResults = {
    eligibleForTH: false,
    eligibleForRRH: false,
    eligibleForPSH: false,
    eligibleForOther: [],
    recommendedProgramId: undefined,
    recommendationReason: undefined,
    ineligibilityReasons: [],
  };

  if (!data.homelessStatus || !data.income || !data.householdComposition) {
    results.ineligibilityReasons.push('Incomplete eligibility data');
    return results;
  }

  const { homelessStatus, income, householdComposition } = data;

  // Must be homeless or at imminent risk
  const isHomeless = homelessStatus.currentlyHomeless;
  const homelessCategory = homelessStatus.homelessCategory;

  if (!isHomeless && homelessCategory !== '2' && homelessCategory !== '4') {
    results.ineligibilityReasons.push('Not currently homeless or at imminent risk');
    return results;
  }

  // Emergency Shelter (ES) - Always eligible if homeless
  if (isHomeless) {
    results.eligibleForOther.push('ES');
  }

  // Transitional Housing (TH) - Families with children preferred
  if (householdComposition.children > 0) {
    results.eligibleForTH = true;
  }

  // Rapid Re-Housing (RRH) - Has some income or income potential
  const monthlyIncome = income.monthlyIncome || 0;
  if (monthlyIncome >= 0 && monthlyIncome <= 3000) {
    results.eligibleForRRH = true;
  }

  // Permanent Supportive Housing (PSH) - Chronically homeless
  // Chronic homelessness: 4+ episodes in 3 years OR 12+ consecutive months
  const timesHomeless = homelessStatus.timesHomelessPast3Years || 0;
  const monthsHomeless = homelessStatus.monthsHomelessPast3Years || 0;

  if (timesHomeless >= 4 || monthsHomeless >= 12) {
    results.eligibleForPSH = true;
  }

  // Fleeing DV - eligible for specialized DV programs
  if (homelessCategory === '4') {
    results.eligibleForOther.push('DV_SHELTER');
    results.eligibleForOther.push('SAFE_HAVEN');
  }

  // Determine recommended program (priority order: RRH > TH > PSH > ES)
  if (results.eligibleForRRH) {
    results.recommendedProgramId = 'RRH';
    results.recommendationReason = 'Client has income potential and can benefit from rapid re-housing support';
  } else if (results.eligibleForTH) {
    results.recommendedProgramId = 'TH';
    results.recommendationReason = 'Family with children - transitional housing provides stability and support services';
  } else if (results.eligibleForPSH) {
    results.recommendedProgramId = 'PSH';
    results.recommendationReason = 'Chronically homeless - permanent supportive housing provides long-term stability';
  } else if (results.eligibleForOther.includes('ES')) {
    results.recommendedProgramId = 'ES';
    results.recommendationReason = 'Emergency shelter provides immediate safety and case management';
  }

  // Check for ineligibility reasons
  if (!results.eligibleForTH && !results.eligibleForRRH && !results.eligibleForPSH && results.eligibleForOther.length === 0) {
    results.ineligibilityReasons.push('Does not meet criteria for available programs');
  }

  return results;
}

/**
 * Check if client meets chronically homeless definition
 * Per HUD: 4+ episodes in 3 years OR 12+ consecutive months
 */
export function isChronicallyHomeless(
  timesHomelessPast3Years: number | undefined,
  monthsHomelessPast3Years: number | undefined
): boolean {
  if (timesHomelessPast3Years && timesHomelessPast3Years >= 4) {
    return true;
  }

  if (monthsHomelessPast3Years && monthsHomelessPast3Years >= 12) {
    return true;
  }

  return false;
}

// =============================================================================
// HOUSING BARRIER SEVERITY CALCULATION
// =============================================================================

/**
 * Calculate housing barrier severity based on identified barriers
 * Returns severity level: LOW, MODERATE, HIGH, SEVERE
 */
export function calculateBarrierSeverity(data: Partial<HousingBarrierData>): 'LOW' | 'MODERATE' | 'HIGH' | 'SEVERE' {
  let score = 0;

  if (!data) {
    return 'LOW';
  }

  const { rentalHistory, creditHistory, criminalBackground, employmentStatus, supportNetwork, transportation } = data;

  // Rental history barriers
  if (rentalHistory?.evictionHistory) {
    score += 2;
    if (rentalHistory.evictionCount && rentalHistory.evictionCount > 1) {
      score += 1; // Additional point for multiple evictions
    }
  }

  if (rentalHistory?.rentalDebt && rentalHistory.rentalDebt > 0) {
    score += 1;
  }

  // Credit barriers
  if (creditHistory?.creditScore && creditHistory.creditScore < 500) {
    score += 2;
  }

  if (creditHistory?.collections) {
    score += 1;
  }

  if (creditHistory?.bankruptcy) {
    score += 1;
  }

  // Criminal background barriers
  if (criminalBackground?.hasRecord) {
    score += 1;
  }

  if (criminalBackground?.registeredSexOffender) {
    score += 3; // SEVERE barrier
  }

  // Employment barriers
  if (employmentStatus?.currentlyEmployed === false) {
    score += 1;
  }

  // Support network barriers
  if (supportNetwork?.isolated) {
    score += 1;
  }

  // Transportation barriers
  if (transportation?.hasReliableTransportation === false) {
    score += 1;
  }

  // Determine severity based on score
  if (score === 0) {
    return 'LOW';
  } else if (score >= 1 && score <= 3) {
    return 'LOW';
  } else if (score >= 4 && score <= 6) {
    return 'MODERATE';
  } else if (score >= 7 && score <= 9) {
    return 'HIGH';
  } else {
    return 'SEVERE';
  }
}

/**
 * Generate stability plan with prioritized interventions
 * Auto-recommends services based on identified barriers
 */
export function generateStabilityPlan(data: Partial<HousingBarrierData>): StabilityPlan {
  const plan: StabilityPlan = {
    needsEmploymentSupport: false,
    needsLandlordMediation: false,
    needsCreditRepair: false,
    needsMentalHealthReferral: false,
    needsSubstanceAbuseReferral: false,
    needsLegalAdvocacy: false,
    needsFinancialLiteracy: false,
    needsChildcare: false,
    needsTransportation: false,
    priorityInterventions: [],
    estimatedTimeline: undefined,
  };

  if (!data) {
    return plan;
  }

  const priorities: Array<{ intervention: string; priority: number }> = [];

  // Employment support
  if (data.employmentStatus?.currentlyEmployed === false || data.employmentStatus?.lookingForWork) {
    plan.needsEmploymentSupport = true;
    priorities.push({ intervention: 'Employment support and job training', priority: 1 });
  }

  // Landlord mediation
  if (data.rentalHistory?.evictionHistory || (data.rentalHistory?.rentalDebt && data.rentalHistory.rentalDebt > 0)) {
    plan.needsLandlordMediation = true;
    priorities.push({ intervention: 'Landlord mediation and rental debt resolution', priority: 2 });
  }

  // Credit repair
  if (
    data.creditHistory?.collections ||
    (data.creditHistory?.creditScore && data.creditHistory.creditScore < 600) ||
    data.creditHistory?.bankruptcy
  ) {
    plan.needsCreditRepair = true;
    priorities.push({ intervention: 'Credit repair and financial counseling', priority: 3 });
  }

  // Legal advocacy
  if (data.criminalBackground?.hasRecord || data.rentalHistory?.evictionHistory) {
    plan.needsLegalAdvocacy = true;
    priorities.push({ intervention: 'Legal advocacy and record expungement', priority: 4 });
  }

  // Financial literacy
  if (!data.employmentStatus?.currentlyEmployed || data.creditHistory?.collections) {
    plan.needsFinancialLiteracy = true;
    priorities.push({ intervention: 'Financial literacy training', priority: 5 });
  }

  // Transportation
  if (data.transportation?.hasReliableTransportation === false) {
    plan.needsTransportation = true;
    priorities.push({ intervention: 'Transportation assistance', priority: 6 });
  }

  // Childcare (if applicable from other data)
  if (data.employmentStatus?.employmentBarriers?.includes('CHILDCARE')) {
    plan.needsChildcare = true;
    priorities.push({ intervention: 'Childcare assistance', priority: 7 });
  }

  // Sort by priority and assign to plan
  plan.priorityInterventions = priorities.sort((a, b) => a.priority - b.priority);

  // Estimate timeline based on barrier severity
  const severity = calculateBarrierSeverity(data);
  switch (severity) {
    case 'LOW':
      plan.estimatedTimeline = '0_3_MONTHS';
      break;
    case 'MODERATE':
      plan.estimatedTimeline = '3_6_MONTHS';
      break;
    case 'HIGH':
      plan.estimatedTimeline = '6_12_MONTHS';
      break;
    case 'SEVERE':
      plan.estimatedTimeline = 'OVER_12_MONTHS';
      break;
  }

  return plan;
}

// =============================================================================
// DATA QUALITY METRICS
// =============================================================================

/**
 * Calculate data quality score (0-100) based on field completion
 * Higher score = more complete data
 */
export function calculateDataQualityScore(masterData: Partial<MasterIntakeData>): number {
  let totalFields = 0;
  let completedFields = 0;

  // Helper to check if field is completed
  const isCompleted = (value: any): boolean => {
    if (value === null || value === undefined) return false;
    if (typeof value === 'string' && value.trim() === '') return false;
    if (typeof value === 'number' && value === 0) return true; // 0 is valid
    if (Array.isArray(value) && value.length === 0) return false;
    return true;
  };

  // Step 1: Initial Contact (7 critical fields)
  if (masterData.step1_initialContact) {
    const step1 = masterData.step1_initialContact;
    totalFields += 7;
    if (isCompleted(step1.clientAlias)) completedFields++;
    if (isCompleted(step1.contactDate)) completedFields++;
    if (isCompleted(step1.contactTime)) completedFields++;
    if (isCompleted(step1.referralSource)) completedFields++;
    if (step1.safeToContactNow !== null) completedFields++;
    if (isCompleted(step1.intakeWorkerName)) completedFields++;
    if (isCompleted(step1.tempClientId)) completedFields++;
  }

  // Step 2: Safety & Consent (5 critical fields)
  if (masterData.step2_safetyConsent) {
    const step2 = masterData.step2_safetyConsent;
    totalFields += 5;
    if (step2.consents.consentToServices) completedFields++;
    if (step2.consents.consentToDataCollection) completedFields++;
    if (isCompleted(step2.consents.hmisParticipationStatus)) completedFields++;
    if (step2.digitalSignature?.signed) completedFields++;
    if (
      step2.safeContactMethods.okToCall ||
      step2.safeContactMethods.okToText ||
      step2.safeContactMethods.okToEmail ||
      step2.safeContactMethods.okToVoicemail
    ) {
      completedFields++;
    }
  }

  // Step 3: Risk Assessment (5 critical fields)
  if (masterData.step3_riskAssessment) {
    const step3 = masterData.step3_riskAssessment;
    totalFields += 5;
    if (isCompleted(step3.lethalityScreening.screeningTool)) completedFields++;
    if (isCompleted(step3.lethalityScreening.riskLevel)) completedFields++;
    if (step3.immediateSafety.currentlySafe !== null) completedFields++;
    if (isCompleted(step3.immediateSafety.policeInvolvement)) completedFields++;
    if (isCompleted(step3.immediateSafety.protectiveOrderStatus)) completedFields++;
  }

  // Step 4: Eligibility (6 critical fields)
  if (masterData.step4_eligibility) {
    const step4 = masterData.step4_eligibility;
    totalFields += 6;
    if (isCompleted(step4.homelessStatus.homelessCategory)) completedFields++;
    if (step4.income.hasIncome !== undefined) completedFields++;
    if (isCompleted(step4.householdComposition.householdType)) completedFields++;
    if (step4.householdComposition.adults > 0) completedFields++;
    if (step4.householdComposition.children >= 0) completedFields++;
    if (step4.selectedProgram?.programId) completedFields++;
  }

  // Step 6: Service Plan (3 critical fields)
  if (masterData.step6_servicePlan) {
    const step6 = masterData.step6_servicePlan;
    totalFields += 3;
    if (step6.assignedCaseManager?.id) completedFields++;
    if (step6.goals && step6.goals.length > 0) completedFields++;
    if (step6.followUpSchedule.day30 || step6.followUpSchedule.day60 || step6.followUpSchedule.day90) completedFields++;
  }

  // Step 8: Demographics (8 critical fields)
  if (masterData.step8_demographics) {
    const step8 = masterData.step8_demographics;
    totalFields += 8;
    if (isCompleted(step8.name.firstName)) completedFields++;
    if (isCompleted(step8.name.lastName)) completedFields++;
    if (isCompleted(step8.identifiers.birthDate)) completedFields++;
    if (isCompleted(step8.identifiers.ssnDataQuality)) completedFields++;
    if (isCompleted(step8.demographics.gender)) completedFields++;
    if (step8.demographics.race && step8.demographics.race.length > 0) completedFields++;
    if (isCompleted(step8.demographics.ethnicity)) completedFields++;
    if (isCompleted(step8.veteranStatus)) completedFields++;
  }

  // Step 9: Enrollment (4 critical fields)
  if (masterData.step9_enrollment) {
    const step9 = masterData.step9_enrollment;
    totalFields += 4;
    if (isCompleted(step9.enrollment.entryDate)) completedFields++;
    if (isCompleted(step9.enrollment.projectId)) completedFields++;
    if (isCompleted(step9.enrollment.projectType)) completedFields++;
    if (step9.staffConfirmation.accuracyConfirmed) completedFields++;
  }

  // Calculate percentage
  if (totalFields === 0) return 0;
  return Math.round((completedFields / totalFields) * 100);
}

/**
 * Check if intake data is ready for Annual Performance Report (APR)
 * Returns readiness status and list of missing fields
 */
export function checkAprReadiness(masterData: Partial<MasterIntakeData>): {
  ready: boolean;
  missingFields: string[];
} {
  const missingFields: string[] = [];

  // Required for APR: Name
  if (!masterData.step8_demographics?.name.firstName || !masterData.step8_demographics?.name.lastName) {
    missingFields.push('Full name (first and last)');
  }

  // Required for APR: Date of Birth
  if (!masterData.step8_demographics?.identifiers.birthDate) {
    missingFields.push('Date of birth');
  }

  // Required for APR: Gender
  if (!masterData.step8_demographics?.demographics.gender) {
    missingFields.push('Gender');
  }

  // Required for APR: Race (at least one)
  if (!masterData.step8_demographics?.demographics.race || masterData.step8_demographics.demographics.race.length === 0) {
    missingFields.push('Race');
  }

  // Required for APR: Ethnicity
  if (!masterData.step8_demographics?.demographics.ethnicity) {
    missingFields.push('Ethnicity');
  }

  // Required for APR: Veteran status
  if (!masterData.step8_demographics?.veteranStatus) {
    missingFields.push('Veteran status');
  }

  // Required for APR: Disabling condition
  if (!masterData.step8_demographics?.disablingCondition) {
    missingFields.push('Disabling condition');
  }

  // Required for APR: Prior living situation
  if (!masterData.step4_eligibility?.homelessStatus.priorLivingSituation) {
    missingFields.push('Prior living situation');
  }

  // Required for APR: Entry date
  if (!masterData.step9_enrollment?.enrollment.entryDate) {
    missingFields.push('Entry date');
  }

  // Required for APR: Relationship to HoH
  if (!masterData.step9_enrollment?.enrollment.relationshipToHoH) {
    missingFields.push('Relationship to head of household');
  }

  return {
    ready: missingFields.length === 0,
    missingFields,
  };
}

/**
 * Check if intake data is ready for CAPER reporting
 * Similar to APR but with additional CoC-specific requirements
 */
export function checkCaperReadiness(masterData: Partial<MasterIntakeData>): {
  ready: boolean;
  missingFields: string[];
} {
  // Start with APR requirements
  const aprCheck = checkAprReadiness(masterData);
  const missingFields = [...aprCheck.missingFields];

  // Additional CAPER requirements

  // Income information
  if (masterData.step4_eligibility?.income.hasIncome === undefined) {
    missingFields.push('Income status');
  }

  // Homeless status
  if (!masterData.step4_eligibility?.homelessStatus.homelessCategory) {
    missingFields.push('Homeless category');
  }

  // Disabling condition details
  if (!masterData.step8_demographics?.disablingCondition) {
    missingFields.push('Disabling condition status');
  }

  return {
    ready: missingFields.length === 0,
    missingFields,
  };
}

// =============================================================================
// UTILITY FUNCTIONS
// =============================================================================

/**
 * Calculate age from birth date
 */
export function calculateAge(birthDate: string): number {
  const birth = new Date(birthDate);
  const today = new Date();

  let age = today.getFullYear() - birth.getFullYear();
  const monthDiff = today.getMonth() - birth.getMonth();

  // Adjust if birthday hasn't occurred this year
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
    age--;
  }

  return age;
}

/**
 * Format phone number to (XXX) XXX-XXXX format
 */
export function formatPhoneNumber(phone: string): string {
  // Remove all non-digit characters
  const digits = phone.replace(/\D/g, '');

  // Handle 10-digit and 11-digit (with country code) numbers
  if (digits.length === 10) {
    return `(${digits.slice(0, 3)}) ${digits.slice(3, 6)}-${digits.slice(6)}`;
  } else if (digits.length === 11 && digits[0] === '1') {
    return `+1 (${digits.slice(1, 4)}) ${digits.slice(4, 7)}-${digits.slice(7)}`;
  }

  // Return original if format is unexpected
  return phone;
}

/**
 * Format SSN with optional masking
 * @param ssn - Social Security Number
 * @param masked - If true, returns XXX-XX-1234 format
 */
export function formatSSN(ssn: string, masked: boolean = false): string {
  // Remove all non-digit characters
  const digits = ssn.replace(/\D/g, '');

  if (digits.length !== 9) {
    return ssn; // Return original if not 9 digits
  }

  if (masked) {
    // Show only last 4 digits
    return `XXX-XX-${digits.slice(5)}`;
  }

  // Full format: XXX-XX-XXXX
  return `${digits.slice(0, 3)}-${digits.slice(3, 5)}-${digits.slice(5)}`;
}

/**
 * Generate HMIS-safe client ID using hashing
 * This is a simplified version - production should use secure backend hashing
 */
export function generateHmisClientId(
  firstName: string,
  lastName: string,
  birthDate: string,
  ssn?: string
): string {
  // Concatenate identifiers
  const baseString = `${firstName.toLowerCase()}${lastName.toLowerCase()}${birthDate}${ssn || ''}`;

  // Simple hash function (production should use crypto library)
  let hash = 0;
  for (let i = 0; i < baseString.length; i++) {
    const char = baseString.charCodeAt(i);
    hash = (hash << 5) - hash + char;
    hash = hash & hash; // Convert to 32-bit integer
  }

  // Convert to positive number and format as ID
  const positiveHash = Math.abs(hash);
  return `HMIS-${positiveHash.toString(36).toUpperCase().padStart(8, '0')}`;
}

/**
 * Calculate percentage of Area Median Income (AMI)
 * Requires AMI data for the jurisdiction (placeholder values used here)
 */
export function calculatePercentOfAMI(
  householdSize: number,
  annualIncome: number,
  jurisdictionAMI?: Record<number, number>
): number {
  // Default AMI values (2024 national average - should be jurisdiction-specific)
  const defaultAMI: Record<number, number> = {
    1: 54000,
    2: 61700,
    3: 69450,
    4: 77150,
    5: 83350,
    6: 89500,
    7: 95700,
    8: 101850,
  };

  const amiTable = jurisdictionAMI || defaultAMI;
  const baseAMI = amiTable[Math.min(householdSize, 8)] || amiTable[8];

  if (baseAMI === 0) return 0;

  return Math.round((annualIncome / baseAMI) * 100);
}

/**
 * Determine if household is extremely low income (< 30% AMI)
 */
export function isExtremelyLowIncome(householdSize: number, annualIncome: number): boolean {
  const percentAMI = calculatePercentOfAMI(householdSize, annualIncome);
  return percentAMI < 30;
}

/**
 * Generate a temporary client ID for use before full enrollment
 */
export function generateTempClientId(): string {
  const timestamp = Date.now().toString(36);
  const random = Math.random().toString(36).substring(2, 8);
  return `TEMP-${timestamp}-${random}`.toUpperCase();
}

/**
 * Check if client is a minor (under 18)
 */
export function isMinor(birthDate: string): boolean {
  const age = calculateAge(birthDate);
  return age < 18;
}

/**
 * Check if client qualifies as youth (18-24)
 */
export function isYouth(birthDate: string): boolean {
  const age = calculateAge(birthDate);
  return age >= 18 && age <= 24;
}

/**
 * Calculate completeness percentage for a specific step
 */
export function calculateStepCompleteness(stepData: any, requiredFields: string[]): number {
  if (!stepData || requiredFields.length === 0) return 0;

  let completedCount = 0;

  for (const fieldPath of requiredFields) {
    const value = getNestedValue(stepData, fieldPath);
    if (value !== null && value !== undefined && value !== '') {
      completedCount++;
    }
  }

  return Math.round((completedCount / requiredFields.length) * 100);
}

/**
 * Helper function to get nested object values by path
 */
function getNestedValue(obj: any, path: string): any {
  return path.split('.').reduce((current, key) => current?.[key], obj);
}

// =============================================================================
// EXPORT ALL BUSINESS LOGIC FUNCTIONS
// =============================================================================

export const IntakeBusinessLogic = {
  // Risk assessment
  calculateOverallRiskLevel,
  shouldAutoRouteToSafety,

  // Eligibility
  determineEligibility,
  isChronicallyHomeless,

  // Housing barriers
  calculateBarrierSeverity,
  generateStabilityPlan,

  // Data quality
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
};

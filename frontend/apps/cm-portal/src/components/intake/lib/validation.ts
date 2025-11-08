/**
 * VAWA-Compliant Intake Workflow Validation Functions
 * Version: 2.0
 *
 * This module provides comprehensive validation for all 10 steps of the intake workflow.
 * Each validation function returns errors (blocking) and warnings (non-blocking).
 */

import type {
  InitialContactData,
  SafetyAndConsentData,
  RiskAssessmentData,
  EligibilityData,
  HousingBarrierData,
  ServicePlanData,
  DocumentationData,
  DemographicsBaselineData,
  EnrollmentConfirmationData,
  FollowUpConfigData,
  ValidationError,
  StepValidationResult,
  MasterIntakeData,
} from '../utils/types';

import { VALIDATION, ERROR_MESSAGES } from '../utils/constants';

// =============================================================================
// HELPER FUNCTIONS
// =============================================================================

/**
 * Check if a string is empty or only whitespace
 */
function isEmpty(value: string | undefined | null): boolean {
  return !value || value.trim().length === 0;
}

/**
 * Validate email format
 */
function isValidEmail(email: string): boolean {
  return VALIDATION.EMAIL_REGEX.test(email);
}

/**
 * Validate phone format (US)
 */
function isValidPhone(phone: string): boolean {
  return VALIDATION.PHONE_REGEX.test(phone);
}

/**
 * Validate SSN format
 */
function isValidSSN(ssn: string): boolean {
  return VALIDATION.SSN_REGEX.test(ssn);
}

/**
 * Check if date is in the future
 */
function isFutureDate(dateString: string): boolean {
  const date = new Date(dateString);
  const now = new Date();
  now.setHours(0, 0, 0, 0); // Compare dates only, ignore time
  return date > now;
}

/**
 * Check if date is valid
 */
function isValidDate(dateString: string | undefined | null): boolean {
  if (!dateString) return false;
  const date = new Date(dateString);
  return !isNaN(date.getTime());
}

/**
 * Create an error object
 */
function createError(field: string, message: string): ValidationError {
  return { field, message, severity: 'ERROR' };
}

/**
 * Create a warning object
 */
function createWarning(field: string, message: string): ValidationError {
  return { field, message, severity: 'WARNING' };
}

// =============================================================================
// STEP 1: INITIAL CONTACT / REFERRAL INTAKE
// =============================================================================

export function validateStep1(data: InitialContactData): StepValidationResult {
  const errors: ValidationError[] = [];
  const warnings: ValidationError[] = [];

  // Required: Client alias (min 2 chars)
  if (isEmpty(data.clientAlias)) {
    errors.push(createError('clientAlias', 'Client alias is required'));
  } else if (data.clientAlias.length < VALIDATION.MIN_ALIAS_LENGTH) {
    errors.push(createError('clientAlias', `Client alias must be at least ${VALIDATION.MIN_ALIAS_LENGTH} characters`));
  } else if (data.clientAlias.length > VALIDATION.MAX_ALIAS_LENGTH) {
    errors.push(createError('clientAlias', `Client alias must not exceed ${VALIDATION.MAX_ALIAS_LENGTH} characters`));
  }

  // Required: Contact date (not future)
  if (isEmpty(data.contactDate)) {
    errors.push(createError('contactDate', 'Contact date is required'));
  } else if (!isValidDate(data.contactDate)) {
    errors.push(createError('contactDate', 'Contact date is not valid'));
  } else if (isFutureDate(data.contactDate)) {
    errors.push(createError('contactDate', 'Contact date cannot be in the future'));
  }

  // Required: Contact time
  if (isEmpty(data.contactTime)) {
    errors.push(createError('contactTime', 'Contact time is required'));
  }

  // Required: Referral source
  if (isEmpty(data.referralSource)) {
    errors.push(createError('referralSource', 'Referral source is required'));
  }

  // Conditional: If referral source = OTHER � details required
  if (data.referralSource === 'OTHER' && isEmpty(data.referralSourceDetails)) {
    errors.push(createError('referralSourceDetails', 'Please provide details for "Other" referral source'));
  }

  // Required: Safe to contact flag
  if (data.safeToContactNow === null) {
    errors.push(createError('safeToContactNow', 'Safety status is required'));
  }

  // Required: Intake worker name
  if (isEmpty(data.intakeWorkerName)) {
    errors.push(createError('intakeWorkerName', 'Intake worker name is required'));
  }

  // Warning: If unsafe contact � suggest crisis protocol
  if (data.safeToContactNow === false || data.needsImmediateCrisisIntervention) {
    warnings.push(createWarning('safeToContactNow', 'Client may be in immediate danger. Consider crisis protocol routing.'));
  }

  return {
    isValid: errors.length === 0,
    errors,
    warnings,
  };
}

// =============================================================================
// STEP 2: SAFETY & CONSENT CHECK
// =============================================================================

export function validateStep2(data: SafetyAndConsentData): StepValidationResult {
  const errors: ValidationError[] = [];
  const warnings: ValidationError[] = [];

  const { safeContactMethods, consents, emergencyContact, digitalSignature } = data;

  // Required: At least 1 safe contact method
  const hasContactMethod =
    safeContactMethods.okToCall ||
    safeContactMethods.okToText ||
    safeContactMethods.okToEmail ||
    safeContactMethods.okToVoicemail;

  if (!hasContactMethod) {
    errors.push(createError('safeContactMethods', 'At least one safe contact method must be selected'));
  }

  // Conditional: If okToCall or okToText � phone required (10 digits)
  if ((safeContactMethods.okToCall || safeContactMethods.okToText) && isEmpty(safeContactMethods.safePhoneNumber)) {
    errors.push(createError('safePhoneNumber', 'Phone number is required for call/text contact'));
  } else if (safeContactMethods.safePhoneNumber && !isValidPhone(safeContactMethods.safePhoneNumber)) {
    errors.push(createError('safePhoneNumber', ERROR_MESSAGES.INVALID_PHONE));
  }

  // Conditional: If okToEmail � email required (valid format)
  if (safeContactMethods.okToEmail && isEmpty(safeContactMethods.safeEmail)) {
    errors.push(createError('safeEmail', 'Email address is required for email contact'));
  } else if (safeContactMethods.safeEmail && !isValidEmail(safeContactMethods.safeEmail)) {
    errors.push(createError('safeEmail', ERROR_MESSAGES.INVALID_EMAIL));
  }

  // Conditional: If quiet hours start � end time required
  if (safeContactMethods.quietHoursStart && isEmpty(safeContactMethods.quietHoursEnd)) {
    errors.push(createError('quietHoursEnd', 'Quiet hours end time is required when start time is specified'));
  }

  // Required: Consent to services (BLOCKING)
  if (!consents.consentToServices) {
    errors.push(createError('consentToServices', ERROR_MESSAGES.CONSENT_REQUIRED));
  }

  // Required: Consent to data collection
  if (!consents.consentToDataCollection) {
    errors.push(createError('consentToDataCollection', 'Consent to data collection is required'));
  }

  // Required: HMIS participation status
  if (isEmpty(consents.hmisParticipationStatus)) {
    errors.push(createError('hmisParticipationStatus', 'HMIS participation status is required'));
  }

  // Required: Digital signature (if any consent given)
  const hasAnyConsent = consents.consentToServices || consents.consentToDataCollection || consents.consentToHmis;
  if (hasAnyConsent && !digitalSignature?.signed) {
    errors.push(createError('digitalSignature', 'Digital signature is required to confirm consent'));
  }

  // Digital signature validation
  if (digitalSignature?.signed) {
    if (!digitalSignature.signatureData && isEmpty(digitalSignature.typedName)) {
      errors.push(createError('digitalSignature', 'Either drawn signature or typed name is required'));
    }
  }

  // Conditional: If emergency contact � validate required fields
  if (emergencyContact) {
    if (isEmpty(emergencyContact.name)) {
      errors.push(createError('emergencyContact.name', 'Emergency contact name is required'));
    }
    if (isEmpty(emergencyContact.phone)) {
      errors.push(createError('emergencyContact.phone', 'Emergency contact phone is required'));
    } else if (!isValidPhone(emergencyContact.phone)) {
      errors.push(createError('emergencyContact.phone', ERROR_MESSAGES.INVALID_PHONE));
    }
    if (isEmpty(emergencyContact.relationship)) {
      errors.push(createError('emergencyContact.relationship', 'Emergency contact relationship is required'));
    }
    if (!emergencyContact.consentToShare) {
      errors.push(createError('emergencyContact.consentToShare', 'Consent to share information with emergency contact is required'));
    }
  }

  // Warning: If HMIS declined � data sharing restricted
  if (consents.hmisParticipationStatus === 'NON_PARTICIPATING') {
    warnings.push(createWarning('hmisParticipationStatus', 'Client declined HMIS participation. Data sharing will be restricted.'));
  }

  // Warning: If VAWA exempt � confidentiality applies
  if (consents.vawaExempt || consents.hmisParticipationStatus === 'VAWA_EXEMPT') {
    warnings.push(createWarning('vawaExempt', 'VAWA confidentiality protections apply. Strict data access controls enforced.'));
  }

  return {
    isValid: errors.length === 0,
    errors,
    warnings,
  };
}

// =============================================================================
// STEP 3: CRISIS / RISK ASSESSMENT
// =============================================================================

export function validateStep3(data: RiskAssessmentData): StepValidationResult {
  const errors: ValidationError[] = [];
  const warnings: ValidationError[] = [];

  const { lethalityScreening, immediateSafety, dependents } = data;

  // Required: Screening tool selection
  if (isEmpty(lethalityScreening.screeningTool)) {
    errors.push(createError('screeningTool', 'Lethality screening tool selection is required'));
  }

  // Required: Risk level (not NOT_ASSESSED)
  if (isEmpty(lethalityScreening.riskLevel)) {
    errors.push(createError('riskLevel', 'Risk level assessment is required'));
  } else if (lethalityScreening.riskLevel === 'NOT_ASSESSED') {
    errors.push(createError('riskLevel', 'Risk level must be assessed before proceeding'));
  }

  // Required: Assessment date
  if (isEmpty(lethalityScreening.assessmentDate)) {
    errors.push(createError('assessmentDate', 'Assessment date is required'));
  } else if (!isValidDate(lethalityScreening.assessmentDate)) {
    errors.push(createError('assessmentDate', 'Assessment date is not valid'));
  } else if (isFutureDate(lethalityScreening.assessmentDate)) {
    errors.push(createError('assessmentDate', 'Assessment date cannot be in the future'));
  }

  // Required: Assessor ID
  if (isEmpty(lethalityScreening.assessorId)) {
    errors.push(createError('assessorId', 'Assessor ID is required'));
  }

  // Required: Currently safe status
  if (immediateSafety.currentlySafe === null) {
    errors.push(createError('currentlySafe', 'Current safety status is required'));
  }

  // Required: Safe place to stay status
  if (immediateSafety.safePlaceToStay === null) {
    errors.push(createError('safePlaceToStay', 'Safe place to stay status is required'));
  }

  // Required: Police involvement
  if (isEmpty(immediateSafety.policeInvolvement)) {
    errors.push(createError('policeInvolvement', 'Police involvement status is required'));
  }

  // Required: Protective order status
  if (isEmpty(immediateSafety.protectiveOrderStatus)) {
    errors.push(createError('protectiveOrderStatus', 'Protective order status is required'));
  }

  // Conditional: If protective order exists � date required
  if (
    immediateSafety.protectiveOrderStatus &&
    ['TEMPORARY', 'PERMANENT', 'EXPIRED', 'VIOLATED'].includes(immediateSafety.protectiveOrderStatus) &&
    isEmpty(immediateSafety.protectiveOrderDate)
  ) {
    errors.push(createError('protectiveOrderDate', 'Protective order date is required'));
  }

  // Conditional: If has minors � count required (min 1)
  if (dependents.hasMinors && (!dependents.numberOfMinors || dependents.numberOfMinors < 1)) {
    errors.push(createError('numberOfMinors', 'Number of minor children is required and must be at least 1'));
  }

  // Warning: If risk = SEVERE/HIGH � emergency routing
  if (lethalityScreening.riskLevel === 'SEVERE' || lethalityScreening.riskLevel === 'HIGH') {
    warnings.push(createWarning('riskLevel', 'HIGH/SEVERE risk detected. Consider immediate safety planning and emergency resources.'));
  }

  // Warning: If needs shelter � prioritize placement
  if (immediateSafety.needsEmergencyShelter) {
    warnings.push(createWarning('needsEmergencyShelter', 'Client needs emergency shelter. Prioritize immediate placement.'));
  }

  // Warning: If medical needs � referral suggested
  if (immediateSafety.hasImmediateMedicalNeeds) {
    warnings.push(createWarning('hasImmediateMedicalNeeds', 'Client has immediate medical needs. Coordinate medical referral.'));
  }

  // Warning: If children unsafe � CPS consideration
  if (dependents.hasMinors && dependents.childrenCurrentlySafe === false) {
    warnings.push(createWarning('childrenCurrentlySafe', 'Children may be unsafe. Consider CPS consultation and child protection measures.'));
  }

  return {
    isValid: errors.length === 0,
    errors,
    warnings,
  };
}

// =============================================================================
// STEP 4: ELIGIBILITY & PROGRAM MATCH
// =============================================================================

export function validateStep4(data: EligibilityData): StepValidationResult {
  const errors: ValidationError[] = [];
  const warnings: ValidationError[] = [];

  const { homelessStatus, income, householdComposition, citizenship, selectedProgram } = data;

  // Required: Homeless status
  if (homelessStatus.currentlyHomeless === undefined) {
    errors.push(createError('currentlyHomeless', 'Homeless status is required'));
  }

  // Required: Homeless category
  if (isEmpty(homelessStatus.homelessCategory)) {
    errors.push(createError('homelessCategory', 'HUD homeless category is required'));
  }

  // Conditional: If homeless � prior living situation + length of stay required
  if (homelessStatus.currentlyHomeless) {
    if (isEmpty(homelessStatus.priorLivingSituation)) {
      errors.push(createError('priorLivingSituation', 'Prior living situation is required for homeless clients'));
    }
    if (isEmpty(homelessStatus.lengthOfStay)) {
      errors.push(createError('lengthOfStay', 'Length of stay in prior situation is required'));
    }
  }

  // Required: Income status
  if (income.hasIncome === undefined) {
    errors.push(createError('hasIncome', 'Income status is required'));
  }

  // Conditional: If has income � amount + source required
  if (income.hasIncome) {
    if (!income.monthlyIncome || income.monthlyIncome <= 0) {
      errors.push(createError('monthlyIncome', 'Monthly income amount is required and must be greater than 0'));
    }
    if (!income.incomeSources || income.incomeSources.length === 0) {
      errors.push(createError('incomeSources', 'At least one income source must be selected'));
    }
  }

  // Required: Household composition (adults, children, type)
  if (!householdComposition.adults || householdComposition.adults < 1) {
    errors.push(createError('adults', 'Number of adults is required and must be at least 1'));
  }

  if (householdComposition.children === undefined || householdComposition.children < 0) {
    errors.push(createError('children', 'Number of children is required'));
  }

  if (isEmpty(householdComposition.householdType)) {
    errors.push(createError('householdType', 'Household type is required'));
  }

  // Validate total size matches
  const expectedTotal = householdComposition.adults + householdComposition.children;
  if (householdComposition.totalSize !== expectedTotal) {
    errors.push(createError('totalSize', `Total household size (${householdComposition.totalSize}) does not match adults + children (${expectedTotal})`));
  }

  // Conditional: If citizenship docs required � verification needed
  if (citizenship.documentationRequired && !citizenship.documentationProvided && !citizenship.waiverApplies) {
    errors.push(createError('citizenship', 'Citizenship documentation is required by funding source, or waiver must be applied'));
  }

  // Required: Program selection
  if (!selectedProgram || isEmpty(selectedProgram.programId)) {
    errors.push(createError('selectedProgram', 'Program selection is required'));
  }

  // Warning: If ineligible for all programs � alternative resources
  const { eligibilityResults } = data;
  if (
    eligibilityResults &&
    !eligibilityResults.eligibleForTH &&
    !eligibilityResults.eligibleForRRH &&
    !eligibilityResults.eligibleForPSH &&
    (!eligibilityResults.eligibleForOther || eligibilityResults.eligibleForOther.length === 0)
  ) {
    warnings.push(createWarning('eligibilityResults', 'Client appears ineligible for all programs. Consider alternative resources and referrals.'));
  }

  return {
    isValid: errors.length === 0,
    errors,
    warnings,
  };
}

// =============================================================================
// STEP 5: HOUSING BARRIER ASSESSMENT
// =============================================================================

export function validateStep5(data: HousingBarrierData): StepValidationResult {
  const errors: ValidationError[] = [];
  const warnings: ValidationError[] = [];

  const { rentalHistory, employmentStatus, criminalBackground, transportation } = data;

  // Required: At least 1 barrier section completed
  const hasRentalData = rentalHistory.hasRentalHistory !== undefined;
  const hasEmploymentData = employmentStatus.currentlyEmployed !== undefined;
  const hasCriminalData = criminalBackground.disclosed !== undefined;
  const hasTransportData = transportation.hasReliableTransportation !== undefined;

  if (!hasRentalData && !hasEmploymentData && !hasCriminalData && !hasTransportData) {
    errors.push(createError('housingBarriers', 'At least one housing barrier section must be completed'));
  }

  // Conditional: If eviction history � count required (min 1)
  if (rentalHistory.evictionHistory && (!rentalHistory.evictionCount || rentalHistory.evictionCount < 1)) {
    errors.push(createError('evictionCount', 'Number of evictions is required and must be at least 1'));
  }

  // Conditional: If employed � type + employer required
  if (employmentStatus.currentlyEmployed) {
    if (isEmpty(employmentStatus.employmentType)) {
      errors.push(createError('employmentType', 'Employment type is required for employed clients'));
    }
    if (isEmpty(employmentStatus.employer)) {
      errors.push(createError('employer', 'Employer name is required for employed clients'));
    }
  }

  // Warning: Criminal record � legal advocacy suggested
  if (criminalBackground.hasRecord) {
    warnings.push(createWarning('criminalBackground', 'Criminal record identified. Consider legal advocacy and expungement services.'));
  }

  // Warning: Sex offender status � specialized assistance
  if (criminalBackground.registeredSexOffender) {
    warnings.push(createWarning('registeredSexOffender', 'Client is registered sex offender. Specialized housing assistance may be required.'));
  }

  // Warning: No transportation � impact on employment/housing
  if (!transportation.hasReliableTransportation) {
    warnings.push(createWarning('transportation', 'Lack of reliable transportation may impact employment and housing search. Consider transportation assistance.'));
  }

  // Warning: Isolated � support group referral
  if (data.supportNetwork?.isolated) {
    warnings.push(createWarning('isolated', 'Client is isolated from support network. Consider peer support groups and community connections.'));
  }

  return {
    isValid: errors.length === 0,
    errors,
    warnings,
  };
}

// =============================================================================
// STEP 6: SERVICE PLAN & CASE ASSIGNMENT
// =============================================================================

export function validateStep6(data: ServicePlanData): StepValidationResult {
  const errors: ValidationError[] = [];
  const warnings: ValidationError[] = [];

  const { assignedCaseManager, goals, followUpSchedule } = data;

  // Required: Case manager assignment
  if (!assignedCaseManager || isEmpty(assignedCaseManager.id)) {
    errors.push(createError('assignedCaseManager', 'Case manager assignment is required'));
  }

  // Required: At least 1 goal
  if (!goals || goals.length === 0) {
    errors.push(createError('goals', 'At least one client goal is required'));
  } else {
    // Conditional: Each goal must have category, description
    goals.forEach((goal, index) => {
      if (isEmpty(goal.category)) {
        errors.push(createError(`goals[${index}].category`, `Goal ${index + 1}: Category is required`));
      }
      if (isEmpty(goal.description)) {
        errors.push(createError(`goals[${index}].description`, `Goal ${index + 1}: Description is required`));
      }
      if (isEmpty(goal.measurableOutcome)) {
        errors.push(createError(`goals[${index}].measurableOutcome`, `Goal ${index + 1}: Measurable outcome is required`));
      }
      if (goal.description && goal.description.length > VALIDATION.MAX_GOAL_DESCRIPTION_LENGTH) {
        errors.push(createError(`goals[${index}].description`, `Goal ${index + 1}: Description exceeds maximum length`));
      }
    });
  }

  // Required: Follow-up schedule (at least 1 interval)
  if (!followUpSchedule.day30 && !followUpSchedule.day60 && !followUpSchedule.day90 && (!followUpSchedule.customDates || followUpSchedule.customDates.length === 0)) {
    errors.push(createError('followUpSchedule', 'At least one follow-up interval must be selected'));
  }

  // Warning: No strengths identified � empowerment opportunity
  if (!data.clientStrengths || data.clientStrengths.length === 0) {
    warnings.push(createWarning('clientStrengths', 'No client strengths identified. Consider empowerment-focused assessment.'));
  }

  // Warning: No aspirations � goal-setting guidance
  if (isEmpty(data.clientAspirations)) {
    warnings.push(createWarning('clientAspirations', 'Client aspirations not documented. Consider goal-setting discussion.'));
  } else if (data.clientAspirations.length > VALIDATION.MAX_ASPIRATIONS_LENGTH) {
    errors.push(createError('clientAspirations', `Aspirations text exceeds maximum length of ${VALIDATION.MAX_ASPIRATIONS_LENGTH} characters`));
  }

  return {
    isValid: errors.length === 0,
    errors,
    warnings,
  };
}

// =============================================================================
// STEP 7: DOCUMENTATION UPLOADS
// =============================================================================

export function validateStep7(data: DocumentationData): StepValidationResult {
  const errors: ValidationError[] = [];
  const warnings: ValidationError[] = [];

  const { requiredDocuments } = data;

  // Required: VAWA consent (uploaded OR waived)
  if (!requiredDocuments.vawaConsent.uploaded && !requiredDocuments.vawaConsent.waived) {
    errors.push(createError('vawaConsent', 'VAWA consent form must be uploaded or waived'));
  }

  // Conditional: If waived � reason required
  if (requiredDocuments.vawaConsent.waived && isEmpty(requiredDocuments.vawaConsent.waiverReason)) {
    errors.push(createError('vawaConsent.waiverReason', 'Waiver reason is required for VAWA consent'));
  }

  // Required: HMIS consent (uploaded OR waived)
  if (!requiredDocuments.hmisConsent.uploaded && !requiredDocuments.hmisConsent.waived) {
    errors.push(createError('hmisConsent', 'HMIS consent form must be uploaded or waived'));
  }

  // Conditional: If waived � reason required
  if (requiredDocuments.hmisConsent.waived && isEmpty(requiredDocuments.hmisConsent.waiverReason)) {
    errors.push(createError('hmisConsent.waiverReason', 'Waiver reason is required for HMIS consent'));
  }

  // Required: Service agreement (uploaded OR waived)
  if (!requiredDocuments.serviceAgreement.uploaded && !requiredDocuments.serviceAgreement.waived) {
    errors.push(createError('serviceAgreement', 'Service agreement must be uploaded or waived'));
  }

  // Conditional: If waived � reason required
  if (requiredDocuments.serviceAgreement.waived && isEmpty(requiredDocuments.serviceAgreement.waiverReason)) {
    errors.push(createError('serviceAgreement.waiverReason', 'Waiver reason is required for service agreement'));
  }

  // Warning: Photo release not obtained � marketing limitation
  if (!requiredDocuments.photoRelease.uploaded && !requiredDocuments.photoRelease.waived) {
    warnings.push(createWarning('photoRelease', 'Photo/video release not obtained. Client photos cannot be used for marketing purposes.'));
  }

  return {
    isValid: errors.length === 0,
    errors,
    warnings,
  };
}

// =============================================================================
// STEP 8: DEMOGRAPHICS & OUTCOME BASELINE
// =============================================================================

export function validateStep8(data: DemographicsBaselineData): StepValidationResult {
  const errors: ValidationError[] = [];
  const warnings: ValidationError[] = [];

  const { name, identifiers, demographics, veteranStatus, disablingCondition, pseudonymization } = data;

  // Required: First name, Last name
  if (isEmpty(name.firstName)) {
    errors.push(createError('firstName', 'First name is required'));
  }

  if (isEmpty(name.lastName)) {
    errors.push(createError('lastName', 'Last name is required'));
  }

  // Required: Date of birth (not future)
  if (isEmpty(identifiers.birthDate)) {
    errors.push(createError('birthDate', 'Date of birth is required'));
  } else if (!isValidDate(identifiers.birthDate)) {
    errors.push(createError('birthDate', 'Date of birth is not valid'));
  } else if (isFutureDate(identifiers.birthDate)) {
    errors.push(createError('birthDate', 'Date of birth cannot be in the future'));
  }

  // Validate SSN if provided
  if (identifiers.socialSecurityNumber && !isValidSSN(identifiers.socialSecurityNumber)) {
    errors.push(createError('socialSecurityNumber', ERROR_MESSAGES.INVALID_SSN));
  }

  // Required: SSN data quality
  if (!identifiers.ssnDataQuality) {
    errors.push(createError('ssnDataQuality', 'SSN data quality indicator is required'));
  }

  // Required: Veteran status
  if (isEmpty(veteranStatus)) {
    errors.push(createError('veteranStatus', 'Veteran status is required'));
  }

  // Required: Disabling condition
  if (isEmpty(disablingCondition)) {
    errors.push(createError('disablingCondition', 'Disabling condition status is required'));
  }

  // Conditional (HMIS participants only): Gender, HMIS gender, Race, Ethnicity
  const isHmisParticipant = !pseudonymization.vawaProtected && !pseudonymization.exportRestricted;

  if (isHmisParticipant) {
    if (isEmpty(demographics.gender)) {
      errors.push(createError('gender', 'Gender is required for HMIS participants'));
    }

    if (!demographics.hmisGender || demographics.hmisGender.length === 0) {
      errors.push(createError('hmisGender', 'HMIS gender (self-identified) is required for HMIS participants'));
    }

    if (!demographics.race || demographics.race.length === 0) {
      errors.push(createError('race', 'Race is required for HMIS participants'));
    }

    if (isEmpty(demographics.ethnicity)) {
      errors.push(createError('ethnicity', 'Ethnicity is required for HMIS participants'));
    }
  }

  // Warning: No contact info � communication challenge
  if (data.contactInfo) {
    const hasPhone = data.contactInfo.phones && data.contactInfo.phones.length > 0;
    const hasEmail = data.contactInfo.emails && data.contactInfo.emails.length > 0;

    if (!hasPhone && !hasEmail) {
      warnings.push(createWarning('contactInfo', 'No contact information provided. Communication may be challenging.'));
    }
  }

  return {
    isValid: errors.length === 0,
    errors,
    warnings,
  };
}

// =============================================================================
// STEP 9: SERVICE ENROLLMENT CONFIRMATION
// =============================================================================

export function validateStep9(data: EnrollmentConfirmationData): StepValidationResult {
  const errors: ValidationError[] = [];
  const warnings: ValidationError[] = [];

  const { enrollment, staffConfirmation } = data;

  // Required: Entry date (not future)
  if (isEmpty(enrollment.entryDate)) {
    errors.push(createError('entryDate', 'Entry date is required'));
  } else if (!isValidDate(enrollment.entryDate)) {
    errors.push(createError('entryDate', 'Entry date is not valid'));
  } else if (isFutureDate(enrollment.entryDate)) {
    errors.push(createError('entryDate', 'Entry date cannot be in the future'));
  }

  // Required: Project ID, type, funding source
  if (isEmpty(enrollment.projectId)) {
    errors.push(createError('projectId', 'Project ID is required'));
  }

  if (isEmpty(enrollment.projectType)) {
    errors.push(createError('projectType', 'Project type is required'));
  }

  if (isEmpty(enrollment.fundingSource)) {
    errors.push(createError('fundingSource', 'Funding source is required'));
  }

  // Required: Relationship to HoH
  if (isEmpty(enrollment.relationshipToHoH)) {
    errors.push(createError('relationshipToHoH', 'Relationship to head of household is required'));
  }

  // Required: Staff accuracy confirmation
  if (!staffConfirmation.accuracyConfirmed) {
    errors.push(createError('accuracyConfirmed', 'Staff must confirm all information is reviewed and accurate'));
  }

  // Required: Confirming staff ID
  if (isEmpty(staffConfirmation.confirmedBy)) {
    errors.push(createError('confirmedBy', 'Confirming staff member ID is required'));
  }

  // Warning: If pending approval � notify client of timeline
  if (data.enrollmentStatus === 'PENDING_APPROVAL') {
    warnings.push(createWarning('enrollmentStatus', 'Enrollment pending approval. Ensure client is notified of expected timeline.'));
  }

  // Warning: If waitlisted � provide alternative resources
  if (data.enrollmentStatus === 'WAITLISTED') {
    warnings.push(createWarning('enrollmentStatus', 'Client waitlisted. Provide alternative resources and expected wait time.'));
  }

  return {
    isValid: errors.length === 0,
    errors,
    warnings,
  };
}

// =============================================================================
// STEP 10: FOLLOW-UP / CONTINUOUS REASSESSMENT
// =============================================================================

export function validateStep10(data: FollowUpConfigData): StepValidationResult {
  const errors: ValidationError[] = [];
  const warnings: ValidationError[] = [];

  const { reassessmentSchedule, reportingReadiness } = data;

  // Required: At least 1 reassessment interval
  if (
    !reassessmentSchedule.monthlyCheckIns &&
    !reassessmentSchedule.day90Review &&
    !reassessmentSchedule.annualAssessment &&
    (!reassessmentSchedule.customSchedule || reassessmentSchedule.customSchedule.length === 0)
  ) {
    errors.push(createError('reassessmentSchedule', 'At least one reassessment interval must be configured'));
  }

  // Warning: APR not ready � address missing fields
  if (reportingReadiness.aprReady === false && reportingReadiness.aprMissingFields && reportingReadiness.aprMissingFields.length > 0) {
    warnings.push(
      createWarning('aprReadiness', `Annual Performance Report (APR) not ready. Missing fields: ${reportingReadiness.aprMissingFields.join(', ')}`)
    );
  }

  // Warning: CAPER not ready � address missing fields
  if (reportingReadiness.caperReady === false && reportingReadiness.caperMissingFields && reportingReadiness.caperMissingFields.length > 0) {
    warnings.push(
      createWarning('caperReadiness', `CAPER not ready. Missing fields: ${reportingReadiness.caperMissingFields.join(', ')}`)
    );
  }

  // Warning: Data quality < 80% � improve completeness
  if (reportingReadiness.dataQualityScore !== undefined && reportingReadiness.dataQualityScore < 80) {
    warnings.push(
      createWarning('dataQualityScore', `Data quality score (${reportingReadiness.dataQualityScore}%) is below 80%. Improve data completeness.`)
    );
  }

  if (reportingReadiness.dataCompleteness !== undefined && reportingReadiness.dataCompleteness < 80) {
    warnings.push(
      createWarning('dataCompleteness', `Data completeness (${reportingReadiness.dataCompleteness}%) is below 80%. Review and fill missing information.`)
    );
  }

  return {
    isValid: errors.length === 0,
    errors,
    warnings,
  };
}

// =============================================================================
// MASTER VALIDATION (ALL STEPS)
// =============================================================================

/**
 * Validate all steps of the intake workflow
 * Returns combined validation results across all completed steps
 */
export function validateAllSteps(data: MasterIntakeData): StepValidationResult {
  const allErrors: ValidationError[] = [];
  const allWarnings: ValidationError[] = [];

  // Step 1: Initial Contact
  const step1Result = validateStep1(data.step1_initialContact);
  allErrors.push(...step1Result.errors.map(e => ({ ...e, field: `step1.${e.field}` })));
  allWarnings.push(...step1Result.warnings.map(w => ({ ...w, field: `step1.${w.field}` })));

  // Step 2: Safety & Consent
  const step2Result = validateStep2(data.step2_safetyConsent);
  allErrors.push(...step2Result.errors.map(e => ({ ...e, field: `step2.${e.field}` })));
  allWarnings.push(...step2Result.warnings.map(w => ({ ...w, field: `step2.${w.field}` })));

  // Step 3: Risk Assessment
  const step3Result = validateStep3(data.step3_riskAssessment);
  allErrors.push(...step3Result.errors.map(e => ({ ...e, field: `step3.${e.field}` })));
  allWarnings.push(...step3Result.warnings.map(w => ({ ...w, field: `step3.${w.field}` })));

  // Step 4: Eligibility
  const step4Result = validateStep4(data.step4_eligibility);
  allErrors.push(...step4Result.errors.map(e => ({ ...e, field: `step4.${e.field}` })));
  allWarnings.push(...step4Result.warnings.map(w => ({ ...w, field: `step4.${w.field}` })));

  // Step 5: Housing Barriers
  const step5Result = validateStep5(data.step5_housingBarriers);
  allErrors.push(...step5Result.errors.map(e => ({ ...e, field: `step5.${e.field}` })));
  allWarnings.push(...step5Result.warnings.map(w => ({ ...w, field: `step5.${w.field}` })));

  // Step 6: Service Plan
  const step6Result = validateStep6(data.step6_servicePlan);
  allErrors.push(...step6Result.errors.map(e => ({ ...e, field: `step6.${e.field}` })));
  allWarnings.push(...step6Result.warnings.map(w => ({ ...w, field: `step6.${w.field}` })));

  // Step 7: Documentation
  const step7Result = validateStep7(data.step7_documentation);
  allErrors.push(...step7Result.errors.map(e => ({ ...e, field: `step7.${e.field}` })));
  allWarnings.push(...step7Result.warnings.map(w => ({ ...w, field: `step7.${w.field}` })));

  // Step 8: Demographics
  const step8Result = validateStep8(data.step8_demographics);
  allErrors.push(...step8Result.errors.map(e => ({ ...e, field: `step8.${e.field}` })));
  allWarnings.push(...step8Result.warnings.map(w => ({ ...w, field: `step8.${w.field}` })));

  // Step 9: Enrollment
  const step9Result = validateStep9(data.step9_enrollment);
  allErrors.push(...step9Result.errors.map(e => ({ ...e, field: `step9.${e.field}` })));
  allWarnings.push(...step9Result.warnings.map(w => ({ ...w, field: `step9.${w.field}` })));

  // Step 10: Follow-up
  const step10Result = validateStep10(data.step10_followUp);
  allErrors.push(...step10Result.errors.map(e => ({ ...e, field: `step10.${e.field}` })));
  allWarnings.push(...step10Result.warnings.map(w => ({ ...w, field: `step10.${w.field}` })));

  return {
    isValid: allErrors.length === 0,
    errors: allErrors,
    warnings: allWarnings,
  };
}

// =============================================================================
// EXPORT ALL VALIDATION FUNCTIONS
// =============================================================================

export const IntakeValidation = {
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
  validateAllSteps,
};

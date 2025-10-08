import type { IntakePsdeResponse } from '../types';

/**
 * Frontend utility for role-based PSDE data redaction
 * Implements client-side data masking for VAWA compliance
 */

export interface RedactionOptions {
  userRoles: string[];
  redactionLevel?: string;
  showRedactionNotices?: boolean;
}

export interface RedactedPsdeData extends Omit<IntakePsdeResponse, 'domesticViolence' | 'domesticViolenceRecency' | 'currentlyFleeingDomesticViolence'> {
  domesticViolence?: string | '[REDACTED]';
  domesticViolenceRecency?: string | '[REDACTED]';
  currentlyFleeingDomesticViolence?: string | '[REDACTED]';
  physicalDisability?: string | '[REDACTED]';
  mentalHealthDisorder?: string | '[REDACTED]';
  coveredByHealthInsurance?: string | '[REDACTED]';
  redactionApplied: boolean;
  redactionReason?: string;
  accessibleFields: string[];
}

/**
 * Main redaction function that applies role-based data masking
 */
export function applyPsdeRedaction(
  data: IntakePsdeResponse,
  options: RedactionOptions
): RedactedPsdeData {
  const { userRoles, redactionLevel, showRedactionNotices = true } = options;

  const accessControl = determineAccessLevel(userRoles, redactionLevel);

  const redactedData: RedactedPsdeData = {
    ...data,
    redactionApplied: false,
    accessibleFields: [],
  };

  // Apply redaction based on access level
  if (!accessControl.canAccessDvData) {
    // Full DV redaction
    redactedData.domesticViolence = '[REDACTED - Insufficient Access]';
    redactedData.domesticViolenceRecency = '[REDACTED - Insufficient Access]';
    redactedData.currentlyFleeingDomesticViolence = '[REDACTED - Insufficient Access]';
    redactedData.redactionApplied = true;
    redactedData.redactionReason = 'User role does not permit access to domestic violence data';
  } else if (!accessControl.canAccessSensitiveData) {
    // Partial DV redaction
    redactedData.domesticViolence = data.domesticViolence; // Basic status visible
    redactedData.domesticViolenceRecency = '[REDACTED - DV Specialist Access Required]';
    redactedData.currentlyFleeingDomesticViolence = '[REDACTED - DV Specialist Access Required]';
    redactedData.redactionApplied = true;
    redactedData.redactionReason = 'Sensitive DV details require specialist access';
  } else {
    // Full access
    redactedData.domesticViolence = data.domesticViolence;
    redactedData.domesticViolenceRecency = data.domesticViolenceRecency;
    redactedData.currentlyFleeingDomesticViolence = data.currentlyFleeingDomesticViolence;
    redactedData.accessibleFields.push('domesticViolence', 'domesticViolenceRecency', 'currentlyFleeingDomesticViolence');
  }

  // Apply VAWA-protected health information redaction
  if (data.hasVawaProtectedHealthInfo && !accessControl.canAccessSensitiveData) {
    redactedData.coveredByHealthInsurance = '[REDACTED - VAWA Protected]';
    redactedData.redactionApplied = true;
  } else {
    redactedData.coveredByHealthInsurance = data.coveredByHealthInsurance;
    redactedData.accessibleFields.push('coveredByHealthInsurance');
  }

  // Apply VAWA-protected disability information redaction
  if (data.hasDisabilityRelatedVawaInfo && !accessControl.canAccessSensitiveData) {
    redactedData.physicalDisability = '[REDACTED - VAWA Protected]';
    redactedData.mentalHealthDisorder = '[REDACTED - VAWA Protected]';
    redactedData.redactionApplied = true;
  } else {
    redactedData.physicalDisability = data.physicalDisability;
    redactedData.mentalHealthDisorder = data.mentalHealthDisorder;
    redactedData.accessibleFields.push('physicalDisability', 'mentalHealthDisorder');
  }

  // Income and basic demographics are generally accessible
  redactedData.accessibleFields.push(
    'totalMonthlyIncome',
    'incomeFromAnySource',
    'informationDate',
    'collectionStage',
    'residentialMoveInDate'
  );

  return redactedData;
}

/**
 * Determine user access level based on roles and data sensitivity
 */
function determineAccessLevel(userRoles: string[], redactionLevel?: string) {
  const canAccessDvData = userRoles.some(role =>
    ['DV_SPECIALIST', 'ADMIN', 'CASE_MANAGER', 'SAFETY_COORDINATOR'].includes(role)
  );

  const canAccessSensitiveData = userRoles.some(role =>
    ['DV_SPECIALIST', 'ADMIN'].includes(role)
  );

  const hasAdminOverride = userRoles.some(role =>
    ['ADMIN', 'SYSTEM_ADMINISTRATOR'].includes(role)
  );

  // Override based on specific redaction level
  if (redactionLevel === 'VICTIM_REQUESTED_CONFIDENTIALITY' && !hasAdminOverride) {
    return {
      canAccessDvData: false,
      canAccessSensitiveData: false,
      hasAdminOverride: false
    };
  }

  return {
    canAccessDvData,
    canAccessSensitiveData,
    hasAdminOverride
  };
}

/**
 * Generate redaction notice component data
 */
export function getRedactionNotice(
  redactedData: RedactedPsdeData,
  fieldName: string
): {
  showNotice: boolean;
  noticeType: 'info' | 'warning' | 'error';
  message: string;
} {
  const fieldValue = redactedData[fieldName as keyof RedactedPsdeData];

  if (typeof fieldValue === 'string' && fieldValue.startsWith('[REDACTED')) {
    return {
      showNotice: true,
      noticeType: 'warning',
      message: getRedactionMessage(fieldValue)
    };
  }

  if (redactedData.accessibleFields.includes(fieldName)) {
    return {
      showNotice: true,
      noticeType: 'info',
      message: 'You have access to this information'
    };
  }

  return {
    showNotice: false,
    noticeType: 'info',
    message: ''
  };
}

/**
 * Get user-friendly redaction message
 */
function getRedactionMessage(redactedValue: string): string {
  if (redactedValue.includes('Insufficient Access')) {
    return 'Access denied. Contact your supervisor or a DV specialist for assistance.';
  }

  if (redactedValue.includes('DV Specialist Access Required')) {
    return 'This sensitive information requires DV specialist privileges.';
  }

  if (redactedValue.includes('VAWA Protected')) {
    return 'This information is protected under VAWA confidentiality requirements.';
  }

  return 'This information is not accessible with your current role.';
}

/**
 * Check if a field should be masked in UI inputs
 */
export function shouldMaskField(
  fieldName: string,
  userRoles: string[],
  isDvRelated: boolean = false,
  isVawaProtected: boolean = false
): boolean {
  const canAccessDvData = userRoles.some(role =>
    ['DV_SPECIALIST', 'ADMIN', 'CASE_MANAGER'].includes(role)
  );

  const canAccessSensitiveData = userRoles.some(role =>
    ['DV_SPECIALIST', 'ADMIN'].includes(role)
  );

  // Always mask if DV-related and no DV access
  if (isDvRelated && !canAccessDvData) {
    return true;
  }

  // Mask VAWA-protected fields if no sensitive access
  if (isVawaProtected && !canAccessSensitiveData) {
    return true;
  }

  // Mask specific sensitive DV fields
  const sensitiveDvFields = [
    'domesticViolenceRecency',
    'currentlyFleeingDomesticViolence',
    'vawaConfidentialityRequested'
  ];

  if (sensitiveDvFields.includes(fieldName) && !canAccessSensitiveData) {
    return true;
  }

  return false;
}

/**
 * Get masked input placeholder text
 */
export function getMaskedPlaceholder(fieldName: string, userRoles: string[]): string {
  const canAccessDvData = userRoles.some(role =>
    ['DV_SPECIALIST', 'ADMIN', 'CASE_MANAGER'].includes(role)
  );

  if (!canAccessDvData) {
    return '*** Restricted - Contact DV Specialist ***';
  }

  return '*** Sensitive Information - Restricted Access ***';
}

/**
 * Audit log helper for redaction events
 */
export function logRedactionEvent(
  recordId: string,
  userId: string,
  redactionApplied: boolean,
  redactionReason?: string
) {
  // This would integrate with the audit logging system
  console.log('PSDE Redaction Event:', {
    recordId,
    userId,
    redactionApplied,
    redactionReason,
    timestamp: new Date().toISOString()
  });

  // In a real implementation, this would call the audit API
  // await apiClient.post('/api/intake-psde/audit/redaction', { ... });
}

/**
 * Utility to safely display field values in UI
 */
export function safeDisplayValue(
  value: any,
  fieldName: string,
  redactionOptions: RedactionOptions
): string {
  if (value === null || value === undefined) {
    return 'Not provided';
  }

  if (typeof value === 'string' && value.startsWith('[REDACTED')) {
    return '*** REDACTED ***';
  }

  // Apply additional formatting based on field type
  switch (fieldName) {
    case 'totalMonthlyIncome':
      return typeof value === 'number' ? `$${value.toLocaleString()}` : String(value);
    case 'informationDate':
    case 'residentialMoveInDate':
      return value instanceof Date ? value.toLocaleDateString() : String(value);
    default:
      return String(value);
  }
}
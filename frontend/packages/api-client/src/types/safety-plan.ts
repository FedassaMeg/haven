export interface SafetyPlan {
  id: string;
  clientId: string;
  version: number;
  status: SafetyPlanStatus;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  createdByName: string;
  updatedBy?: string;
  updatedByName?: string;
  activatedAt?: string;
  archivedAt?: string;
  
  // Plan sections
  triggersAndRisks: SafetyPlanSection;
  warningSign: SafetyPlanSection;
  safeContacts: SafeContact[];
  escapePlan: SafetyPlanSection;
  techSafety: SafetyPlanSection;
  copingStrategies?: SafetyPlanSection;
  importantDocuments?: SafetyPlanSection;
  childrenSafety?: SafetyPlanSection;
  petSafety?: SafetyPlanSection;
  
  // Metadata
  nextReviewDate?: string;
  reviewFrequency?: ReviewFrequency;
  restrictedSections?: string[];
  isConfidential: boolean;
  quickHideEnabled: boolean;
  autoSaveEnabled: boolean;
  
  // Audit
  accessLog?: AccessLogEntry[];
  changeHistory?: ChangeHistoryEntry[];
}

export interface SafetyPlanSection {
  content: string;
  items?: string[];
  lastModified: string;
  modifiedBy: string;
  visibility: SectionVisibility;
  isEncrypted?: boolean;
}

export interface SafeContact {
  id: string;
  name: string;
  relationship: string;
  phone?: string;
  email?: string;
  address?: string;
  contactMethod: ContactMethod;
  safetyNotes: string;
  isEmergencyContact: boolean;
  isPrimaryContact: boolean;
  visibility: SectionVisibility;
  availableHours?: string;
  alternateContact?: string;
}

export enum SafetyPlanStatus {
  DRAFT = 'DRAFT',
  ACTIVE = 'ACTIVE',
  ARCHIVED = 'ARCHIVED',
  UNDER_REVIEW = 'UNDER_REVIEW',
  EXPIRED = 'EXPIRED'
}

export enum SectionVisibility {
  CLIENT_ONLY = 'CLIENT_ONLY',
  CLIENT_AND_CASE_MANAGER = 'CLIENT_AND_CASE_MANAGER',
  STAFF_ONLY = 'STAFF_ONLY',
  EMERGENCY_ONLY = 'EMERGENCY_ONLY',
  HIDDEN = 'HIDDEN'
}

export enum ContactMethod {
  CALL_ONLY = 'CALL_ONLY',
  TEXT_ONLY = 'TEXT_ONLY',
  EMAIL_ONLY = 'EMAIL_ONLY',
  IN_PERSON = 'IN_PERSON',
  SECURE_APP = 'SECURE_APP',
  NO_CONTACT = 'NO_CONTACT'
}

export enum ReviewFrequency {
  WEEKLY = 'WEEKLY',
  BIWEEKLY = 'BIWEEKLY',
  MONTHLY = 'MONTHLY',
  QUARTERLY = 'QUARTERLY',
  AS_NEEDED = 'AS_NEEDED'
}

export interface AccessLogEntry {
  id: string;
  userId: string;
  userName: string;
  action: 'VIEW' | 'EDIT' | 'PRINT' | 'EXPORT' | 'SHARE';
  timestamp: string;
  ipAddress?: string;
  deviceType?: string;
  sections?: string[];
}

export interface ChangeHistoryEntry {
  id: string;
  version: number;
  changeType: 'CREATE' | 'UPDATE' | 'ARCHIVE' | 'RESTORE';
  changedBy: string;
  changedByName: string;
  timestamp: string;
  changeDescription: string;
  sectionsModified?: string[];
  previousValue?: any;
  newValue?: any;
}

export interface SafetyPlanReminder {
  id: string;
  planId: string;
  clientId: string;
  type: ReminderType;
  dueDate: string;
  message: string;
  isCompleted: boolean;
  completedDate?: string;
  completedBy?: string;
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
}

export enum ReminderType {
  REVIEW_PLAN = 'REVIEW_PLAN',
  UPDATE_CONTACTS = 'UPDATE_CONTACTS',
  CHECK_DOCUMENTS = 'CHECK_DOCUMENTS',
  PRACTICE_ESCAPE = 'PRACTICE_ESCAPE',
  TECH_SAFETY_CHECK = 'TECH_SAFETY_CHECK',
  CUSTOM = 'CUSTOM'
}

export interface QuickHideConfig {
  enabled: boolean;
  redirectUrl: string;
  hotkey?: string;
  panicButton: boolean;
  autoLockTimeout?: number;
  clearHistory: boolean;
}

export interface SafetyResource {
  id: string;
  category: ResourceCategory;
  name: string;
  description: string;
  phone?: string;
  website?: string;
  address?: string;
  is24Hour: boolean;
  isConfidential: boolean;
  notes?: string;
}

export enum ResourceCategory {
  DV_HOTLINE = 'DV_HOTLINE',
  EMERGENCY_SHELTER = 'EMERGENCY_SHELTER',
  LEGAL_AID = 'LEGAL_AID',
  COUNSELING = 'COUNSELING',
  POLICE = 'POLICE',
  MEDICAL = 'MEDICAL',
  CRISIS_LINE = 'CRISIS_LINE',
  OTHER = 'OTHER'
}
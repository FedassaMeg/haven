export interface ServiceEncounter {
  id: string;
  clientId: string;
  caseId?: string;
  date: string;
  type: ServiceEncounterType;
  duration?: number; // in minutes
  location?: ServiceLocation;
  provider: string;
  providerId: string;
  notes: string;
  isConfidential?: boolean;
  attachments?: Attachment[];
  followUpRequired?: boolean;
  followUpDate?: string;
  createdAt: string;
  updatedAt: string;
}

export enum ServiceEncounterType {
  CASE_MANAGEMENT = 'CASE_MANAGEMENT',
  COUNSELING = 'COUNSELING',
  HOUSING_NAVIGATION = 'HOUSING_NAVIGATION',
  LEGAL_ADVOCACY = 'LEGAL_ADVOCACY',
  FINANCIAL_ASSISTANCE = 'FINANCIAL_ASSISTANCE',
  SAFETY_PLANNING = 'SAFETY_PLANNING',
  CRISIS_INTERVENTION = 'CRISIS_INTERVENTION',
  GROUP_SESSION = 'GROUP_SESSION',
  OUTREACH = 'OUTREACH',
  PHONE_CALL = 'PHONE_CALL',
  EMAIL = 'EMAIL',
  TEXT_MESSAGE = 'TEXT_MESSAGE',
  VIRTUAL_MEETING = 'VIRTUAL_MEETING',
  HOME_VISIT = 'HOME_VISIT',
  OFFICE_VISIT = 'OFFICE_VISIT',
  COMMUNITY_MEETING = 'COMMUNITY_MEETING',
  COURT_APPEARANCE = 'COURT_APPEARANCE',
  MEDICAL_APPOINTMENT = 'MEDICAL_APPOINTMENT',
  OTHER = 'OTHER'
}

export enum ServiceLocation {
  OFFICE = 'OFFICE',
  HOME = 'HOME',
  COMMUNITY = 'COMMUNITY',
  COURT = 'COURT',
  VIRTUAL = 'VIRTUAL',
  PHONE = 'PHONE',
  SAFE_LOCATION = 'SAFE_LOCATION',
  OTHER = 'OTHER'
}

export interface Goal {
  id: string;
  clientId: string;
  caseId?: string;
  category: GoalCategory;
  title: string;
  description?: string;
  targetDate: string;
  status: GoalStatus;
  progress: number; // 0-100
  milestones?: Milestone[];
  assignedTo?: string;
  assignedToId?: string;
  priority: Priority;
  notes?: string;
  createdAt: string;
  updatedAt: string;
  completedAt?: string;
}

export enum GoalCategory {
  HOUSING_STABILITY = 'HOUSING_STABILITY',
  EMPLOYMENT = 'EMPLOYMENT',
  EDUCATION = 'EDUCATION',
  FINANCIAL_STABILITY = 'FINANCIAL_STABILITY',
  LEGAL = 'LEGAL',
  HEALTH_WELLNESS = 'HEALTH_WELLNESS',
  MENTAL_HEALTH = 'MENTAL_HEALTH',
  SUBSTANCE_USE = 'SUBSTANCE_USE',
  FAMILY_REUNIFICATION = 'FAMILY_REUNIFICATION',
  SAFETY = 'SAFETY',
  BENEFITS = 'BENEFITS',
  LIFE_SKILLS = 'LIFE_SKILLS',
  CHILDCARE = 'CHILDCARE',
  TRANSPORTATION = 'TRANSPORTATION',
  OTHER = 'OTHER'
}

export enum GoalStatus {
  NOT_STARTED = 'NOT_STARTED',
  IN_PROGRESS = 'IN_PROGRESS',
  ON_HOLD = 'ON_HOLD',
  AT_RISK = 'AT_RISK',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED'
}

export enum Priority {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  URGENT = 'URGENT'
}

export interface Milestone {
  id: string;
  goalId: string;
  title: string;
  description?: string;
  targetDate: string;
  completedDate?: string;
  isCompleted: boolean;
  notes?: string;
}

export interface Referral {
  id: string;
  clientId: string;
  caseId?: string;
  referralType: ReferralType;
  organizationName: string;
  contactName?: string;
  contactPhone?: string;
  contactEmail?: string;
  reason: string;
  urgency: Priority;
  status: ReferralStatus;
  sentDate: string;
  followUpDate?: string;
  responseDate?: string;
  outcome?: string;
  notes?: string;
  isWarmHandoff: boolean;
  consentProvided: boolean;
  createdAt: string;
  updatedAt: string;
}

export enum ReferralType {
  LEGAL_AID = 'LEGAL_AID',
  COUNSELING = 'COUNSELING',
  MEDICAL = 'MEDICAL',
  MENTAL_HEALTH = 'MENTAL_HEALTH',
  SUBSTANCE_ABUSE = 'SUBSTANCE_ABUSE',
  HOUSING = 'HOUSING',
  EMPLOYMENT = 'EMPLOYMENT',
  EDUCATION = 'EDUCATION',
  CHILDCARE = 'CHILDCARE',
  FOOD_ASSISTANCE = 'FOOD_ASSISTANCE',
  TRANSPORTATION = 'TRANSPORTATION',
  FINANCIAL = 'FINANCIAL',
  DV_SHELTER = 'DV_SHELTER',
  DV_SERVICES = 'DV_SERVICES',
  IMMIGRATION = 'IMMIGRATION',
  BENEFITS = 'BENEFITS',
  OTHER = 'OTHER'
}

export enum ReferralStatus {
  PENDING = 'PENDING',
  SENT = 'SENT',
  ACKNOWLEDGED = 'ACKNOWLEDGED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  DECLINED = 'DECLINED',
  NO_RESPONSE = 'NO_RESPONSE',
  CLIENT_DECLINED = 'CLIENT_DECLINED'
}

export interface Attachment {
  id: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
  uploadedAt: string;
  uploadedBy: string;
  isConfidential: boolean;
}

export interface ServiceCalendarEvent {
  id: string;
  title: string;
  start: string;
  end: string;
  type: 'encounter' | 'goal' | 'referral' | 'reminder';
  relatedId: string;
  clientId: string;
  clientName?: string;
  location?: string;
  notes?: string;
  isRecurring?: boolean;
  recurringPattern?: RecurringPattern;
}

export interface RecurringPattern {
  frequency: 'daily' | 'weekly' | 'biweekly' | 'monthly';
  interval: number;
  endDate?: string;
  daysOfWeek?: number[];
  dayOfMonth?: number;
}

export interface ServiceReminder {
  id: string;
  clientId: string;
  type: ReminderType;
  message: string;
  dueDate: string;
  isCompleted: boolean;
  completedDate?: string;
  completedBy?: string;
  priority: Priority;
  relatedType?: 'goal' | 'referral' | 'encounter';
  relatedId?: string;
}

export enum ReminderType {
  MISSED_CONTACT = 'MISSED_CONTACT',
  MONTHLY_CHECKIN = 'MONTHLY_CHECKIN',
  GOAL_FOLLOWUP = 'GOAL_FOLLOWUP',
  REFERRAL_FOLLOWUP = 'REFERRAL_FOLLOWUP',
  DOCUMENT_DUE = 'DOCUMENT_DUE',
  APPOINTMENT = 'APPOINTMENT',
  CONSENT_EXPIRING = 'CONSENT_EXPIRING',
  CASE_REVIEW = 'CASE_REVIEW',
  CUSTOM = 'CUSTOM'
}
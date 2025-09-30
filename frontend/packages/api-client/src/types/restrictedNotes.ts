export interface RestrictedNote {
  noteId: string;
  clientId: string;
  clientName: string;
  caseId: string;
  caseNumber: string;
  noteType: string;
  content: string;
  title: string;
  authorId: string;
  authorName: string;
  createdAt: string;
  lastModified: string;
  authorizedViewers?: string[];
  visibilityScope: string;
  isSealed: boolean;
  sealReason?: string;
  sealedAt?: string;
  sealedBy?: string;
  requiresSpecialHandling: boolean;
  visibilityWarning?: string;
}

export interface CreateRestrictedNoteRequest {
  clientId: string;
  clientName: string;
  caseId: string;
  caseNumber: string;
  noteType: string;
  content: string;
  title: string;
  authorizedViewers?: string[];
  visibilityScope?: string;
}

export interface UpdateRestrictedNoteRequest {
  content: string;
  updateReason: string;
}

export interface SealNoteRequest {
  sealReason: string;
  legalBasis: string;
  temporary: boolean;
  expiresAt?: string;
}

export interface UnsealNoteRequest {
  unsealReason: string;
  legalBasis: string;
}

export interface RestrictedNoteResponse {
  noteId: string;
  message: string;
  success: boolean;
}

export interface NoteAuditEntry {
  noteId: string;
  eventType: string;
  performedBy: string;
  performedByName: string;
  userRoles: string[];
  performedAt: string;
  reason: string;
  accessMethod?: string;
  ipAddress?: string;
  userAgent?: string;
  contentViewed: boolean;
  details: string;
}

export interface ComplianceReport {
  noteId: string;
  generatedAt: string;
  totalEvents: number;
  accessEventCount: number;
  modificationEventCount: number;
  sealEventCount: number;
  uniqueUsersAccessed: number;
  hasPolicyViolations: boolean;
  policyViolationDetails?: string[];
}

export interface RestrictedNoteFilters {
  noteType?: string;
  visibilityScope?: string;
  isSealed?: boolean;
  authorId?: string;
  dateFrom?: string;
  dateTo?: string;
}
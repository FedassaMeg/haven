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

// Inferred types from schemas
export type HumanName = z.infer<typeof HumanNameSchema>;
export type Address = z.infer<typeof AddressSchema>;
export type ContactPoint = z.infer<typeof ContactPointSchema>;
export type CodeableConcept = z.infer<typeof CodeableConceptSchema>;
export type Period = z.infer<typeof PeriodSchema>;
export type Client = z.infer<typeof ClientSchema>;
export type Case = z.infer<typeof CaseSchema>;

// Request/Command types
export const CreateClientRequestSchema = z.object({
  givenName: z.string().min(1).max(100),
  familyName: z.string().min(1).max(100),
  gender: z.enum(['MALE', 'FEMALE', 'OTHER', 'UNKNOWN']),
  birthDate: z.string().optional(),
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
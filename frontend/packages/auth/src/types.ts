import { KeycloakInstance, KeycloakProfile } from 'keycloak-js';

export interface User {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
  groups: string[];
  attributes?: Record<string, any>;
}

export interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  token: string | null;
  refreshToken: string | null;
}

export interface KeycloakConfig {
  url: string;
  realm: string;
  clientId: string;
}

export interface AuthContextType extends AuthState {
  keycloak: KeycloakInstance | null;
  login: (redirectUri?: string) => Promise<void>;
  logout: (redirectUri?: string) => Promise<void>;
  register: (redirectUri?: string) => Promise<void>;
  updateToken: (minValidity?: number) => Promise<boolean>;
  hasRole: (role: string) => boolean;
  hasGroup: (group: string) => boolean;
  hasPermission: (resource: string, action: string) => boolean;
}

export interface AuthProviderProps {
  children: React.ReactNode;
  config: KeycloakConfig;
  initOptions?: {
    onLoad?: 'login-required' | 'check-sso';
    silentCheckSsoRedirectUri?: string;
    checkLoginIframe?: boolean;
    enableLogging?: boolean;
  };
  onAuthSuccess?: (user: User) => void;
  onAuthError?: (error: any) => void;
  loadingComponent?: React.ComponentType;
}

export interface ProtectedRouteProps {
  children: React.ReactNode;
  roles?: string[];
  groups?: string[];
  redirectTo?: string;
  fallback?: React.ComponentType;
}

export interface PermissionGuardProps {
  resource: string;
  action: string;
  children: React.ReactNode;
  fallback?: React.ReactNode;
}

// Permission types for RBAC
export interface Permission {
  resource: string;
  actions: string[];
}

export interface Role {
  name: string;
  permissions: Permission[];
}

// Common roles in healthcare systems
export enum UserRole {
  ADMIN = 'admin',
  SUPERVISOR = 'supervisor',
  CASE_MANAGER = 'case-manager',
  INTAKE_SPECIALIST = 'intake-specialist',
  CE_INTAKE = 'ce-intake',
  DV_ADVOCATE = 'dv-advocate',
  COMPLIANCE_AUDITOR = 'compliance-auditor',
  EXEC = 'exec',
  REPORT_VIEWER = 'report-viewer',
  EXTERNAL_PARTNER = 'external-partner',
  COUNSELOR = 'counselor',
  ADVOCATE = 'advocate',
  DATA_ANALYST = 'data-analyst',
}

// Common permissions
export enum ResourceType {
  CLIENT = 'client',
  CASE = 'case',
  REPORT = 'report',
  USER = 'user',
  SYSTEM = 'system',
}

export enum ActionType {
  CREATE = 'create',
  READ = 'read',
  UPDATE = 'update',
  DELETE = 'delete',
  ASSIGN = 'assign',
  CLOSE = 'close',
}
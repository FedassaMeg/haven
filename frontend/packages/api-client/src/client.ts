import axios, { AxiosInstance, AxiosError, AxiosRequestConfig } from 'axios';
import type {
  ApiResponse,
  ApiError,
  PaginatedResponse,
  Client,
  Case,
  CreateClientRequest,
  UpdateClientDemographicsRequest,
  AddClientAddressRequest,
  AddClientTelecomRequest,
  OpenCaseRequest,
  AssignCaseRequest,
  AddCaseNoteRequest,
  UpdateCaseStatusRequest,
  CloseCaseRequest,
  ClientSearchParams,
  CaseSearchParams,
  ServiceEpisode,
  CreateServiceEpisodeRequest,
  CreateServiceEpisodeResponse,
  StartServiceRequest,
  CompleteServiceRequest,
  QuickCrisisServiceRequest,
  QuickCounselingServiceRequest,
  QuickCaseManagementServiceRequest,
  UpdateOutcomeRequest,
  ServiceSearchCriteria,
  ServiceStatistics,
  ServiceTypeResponse,
  ServiceDeliveryModeResponse,
  FundingSource,
  EnrollmentSummary,
  TransitionToRrhRequest,
  TransitionToRrhResponse,
  CombinedServicesResponse,
  IntakeAssessment,
  UpdateIntakeFieldRequest,
  FinancialAssistanceRequest,
  CreateAssistanceRequestRequest,
  AssistanceLedgerEntry,
  AssistanceSummary,
  ApprovalQueueItem,
  Payee,
  HouseholdComposition,
  HouseholdMember,
  CreateHouseholdCompositionRequest,
  AddHouseholdMemberRequest,
  CeAssessment,
  CreateCeAssessmentRequest,
  CeEvent,
  CreateCeEventRequest,
  ConsentLedgerEntry,
  ConsentSearchParams,
  ConsentAuditEntry,
  ConsentStatistics,
  PaginatedConsentResponse,
} from './types';

import type {
  RestrictedNote,
  CreateRestrictedNoteRequest,
  UpdateRestrictedNoteRequest,
  SealNoteRequest,
  UnsealNoteRequest,
  RestrictedNoteResponse,
  NoteAuditEntry,
  ComplianceReport,
  RestrictedNoteFilters
} from './types/restrictedNotes';

export class ApiClient {
  private axios: AxiosInstance;
  private authToken: string | null = null;
  private isRefreshing = false;
  private refreshSubscribers: Array<(token: string) => void> = [];

  constructor(
    baseUrl: string,
    private onTokenRefresh?: () => Promise<string | null>,
    private onAuthRequired?: () => void
  ) {
    this.axios = axios.create({
      baseURL: baseUrl,
      timeout: 10000,
    });

    this.setupInterceptors();
  }

  private setupInterceptors() {
    // Request interceptor for auth token and content type
    this.axios.interceptors.request.use(
      (config) => {
        if (this.authToken) {
          config.headers.Authorization = `Bearer ${this.authToken}`;
        }
        
        // Set Content-Type to application/json by default, unless:
        // 1. It's already set (e.g., for file uploads)
        // 2. The data is FormData (let Axios handle multipart/form-data with boundary)
        if (!config.headers['Content-Type'] && !(config.data instanceof FormData)) {
          config.headers['Content-Type'] = 'application/json';
        }
        
        return config;
      },
      (error) => {
        return Promise.reject(error);
      }
    );

    // Response interceptor for error handling
    this.axios.interceptors.response.use(
      (response) => response,
      async (error: AxiosError<ApiError>) => {
        const status = error.response?.status;
        const originalRequest = error.config as AxiosRequestConfig & { _retry?: boolean };

        // Handle 401 Unauthorized
        if (status === 401) {
          // Avoid infinite loops - don't retry if this was already a retry attempt
          if (originalRequest._retry) {
            // If refresh also failed, redirect to login
            if (this.onAuthRequired) {
              this.onAuthRequired();
            } else {
              window.location.href = '/login';
            }
            return Promise.reject(error);
          }

          originalRequest._retry = true;

          // If not already refreshing, attempt to refresh the token
          if (!this.isRefreshing) {
            this.isRefreshing = true;

            try {
              const newToken = this.onTokenRefresh ? await this.onTokenRefresh() : null;
              
              if (newToken) {
                this.setAuthToken(newToken);
                this.onRefreshSuccess(newToken);
                
                // Retry the original request with new token
                if (originalRequest.headers) {
                  originalRequest.headers.Authorization = `Bearer ${newToken}`;
                }
                return this.axios(originalRequest);
              } else {
                // No token received, redirect to login
                if (this.onAuthRequired) {
                  this.onAuthRequired();
                } else {
                  window.location.href = '/login';
                }
                return Promise.reject(error);
              }
            } catch (refreshError) {
              // Refresh failed, redirect to login
              if (this.onAuthRequired) {
                this.onAuthRequired();
              } else {
                window.location.href = '/login';
              }
              return Promise.reject(refreshError);
            } finally {
              this.isRefreshing = false;
              this.refreshSubscribers = [];
            }
          } else {
            // Already refreshing, queue this request
            return new Promise((resolve, reject) => {
              this.refreshSubscribers.push((token: string) => {
                if (originalRequest.headers) {
                  originalRequest.headers.Authorization = `Bearer ${token}`;
                }
                resolve(this.axios(originalRequest));
              });
            });
          }
        }

        // For non-401 errors, keep the existing error normalization
        const apiError: ApiError = {
          code: error.response?.data?.code || 'UNKNOWN_ERROR',
          message: error.response?.data?.message || error.message || 'An error occurred',
          status: error.response?.status || 500,
          timestamp: error.response?.data?.timestamp || new Date().toISOString(),
          fieldErrors: error.response?.data?.fieldErrors,
        };

        return Promise.reject(apiError);
      }
    );
  }

  private onRefreshSuccess(token: string) {
    this.refreshSubscribers.forEach(callback => callback(token));
  }

  setAuthToken(token: string | null) {
    this.authToken = token;
  }

  // Generic HTTP methods
  private async get<T>(path: string, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.axios.get<T>(path, config);
    return response.data;
  }

  private async post<T>(path: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.axios.post<T>(path, data, config);
    return response.data;
  }

  private async put<T>(path: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.axios.put<T>(path, data, config);
    return response.data;
  }

  private async delete<T>(path: string, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.axios.delete<T>(path, config);
    return response.data;
  }

  // Client API methods
  async getClients(params?: ClientSearchParams): Promise<Client[]> {
    const queryParams = new URLSearchParams();
    if (params?.name) queryParams.append('name', params.name);
    if (params?.activeOnly) queryParams.append('activeOnly', 'true');
    if (params?.page) queryParams.append('page', params.page.toString());
    if (params?.limit) queryParams.append('limit', params.limit.toString());

    const query = queryParams.toString();
    return this.get<Client[]>(`/clients${query ? '?' + query : ''}`);
  }

  async getClient(id: string): Promise<Client> {
    return this.get<Client>(`/clients/${id}`);
  }

  async createClient(data: CreateClientRequest): Promise<{ id: string; resourceType: string }> {
    return this.post<{ id: string; resourceType: string }>('/clients', data);
  }

  async updateClientDemographics(
    id: string,
    data: UpdateClientDemographicsRequest
  ): Promise<void> {
    return this.put<void>(`/clients/${id}/demographics`, data);
  }

  async addClientAddress(id: string, data: AddClientAddressRequest): Promise<void> {
    return this.post<void>(`/clients/${id}/addresses`, data);
  }

  async addClientTelecom(id: string, data: AddClientTelecomRequest): Promise<void> {
    return this.post<void>(`/clients/${id}/telecoms`, data);
  }

  // Case API methods
  async getCases(params?: CaseSearchParams): Promise<PaginatedResponse<Case>> {
    const queryParams = new URLSearchParams();
    if (params?.clientId) queryParams.append('clientId', params.clientId);
    if (params?.assigneeId) queryParams.append('assigneeId', params.assigneeId);
    if (params?.activeOnly) queryParams.append('activeOnly', 'true');
    if (params?.requiresAttention) queryParams.append('requiresAttention', 'true');
    if (params?.page) queryParams.append('page', params.page.toString());
    if (params?.limit) queryParams.append('limit', params.limit.toString());

    const query = queryParams.toString();
    return this.get<PaginatedResponse<Case>>(`/cases${query ? '?' + query : ''}`);
  }

  async getCase(id: string): Promise<Case> {
    return this.get<Case>(`/cases/${id}`);
  }

  async getCasesByClient(clientId: string): Promise<PaginatedResponse<Case>> {
    return this.getCases({ clientId });
  }

  async openCase(data: OpenCaseRequest): Promise<{ id: string; resourceType: string }> {
    return this.post<{ id: string; resourceType: string }>('/cases', data);
  }

  async assignCase(id: string, data: AssignCaseRequest): Promise<void> {
    return this.put<void>(`/cases/${id}/assignment`, data);
  }

  async addCaseNote(id: string, data: AddCaseNoteRequest): Promise<void> {
    return this.post<void>(`/cases/${id}/notes`, data);
  }

  async updateCaseStatus(id: string, data: UpdateCaseStatusRequest): Promise<void> {
    return this.put<void>(`/cases/${id}/status`, data);
  }

  async closeCase(id: string, data: CloseCaseRequest): Promise<void> {
    return this.post<void>(`/cases/${id}/close`, data);
  }

  // Health check
  async healthCheck(): Promise<{ status: string; timestamp: string }> {
    return this.get<{ status: string; timestamp: string }>('/actuator/health');
  }

  // Triage Dashboard API methods
  async getTriageDashboard(params?: {
    workerId?: string;
    daysAhead?: number;
  }): Promise<TriageDashboardData> {
    const queryParams = new URLSearchParams();
    if (params?.workerId) queryParams.append('workerId', params.workerId);
    if (params?.daysAhead) queryParams.append('daysAhead', params.daysAhead.toString());
    
    const query = queryParams.toString();
    return this.get<TriageDashboardData>(`/triage/dashboard${query ? '?' + query : ''}`);
  }

  async getTriageAlerts(params?: {
    severity?: string;
    type?: string;
    status?: string;
    workerId?: string;
  }): Promise<TriageAlert[]> {
    const queryParams = new URLSearchParams();
    if (params?.severity) queryParams.append('severity', params.severity);
    if (params?.type) queryParams.append('type', params.type);
    if (params?.status) queryParams.append('status', params.status);
    if (params?.workerId) queryParams.append('workerId', params.workerId);
    
    const query = queryParams.toString();
    return this.get<TriageAlert[]>(`/triage/alerts${query ? '?' + query : ''}`);
  }

  async acknowledgeAlert(alertId: string): Promise<void> {
    return this.put<void>(`/triage/alerts/${alertId}/acknowledge`);
  }

  async resolveAlert(alertId: string): Promise<void> {
    return this.put<void>(`/triage/alerts/${alertId}/resolve`);
  }

  // Caseload API methods
  async getCaseload(params?: {
    workerId?: string;
    stage?: string;
    riskLevel?: string;
    programId?: string;
    requiresAttention?: boolean;
    page?: number;
    size?: number;
    sort?: string;
  }): Promise<CaseloadResponse> {
    const queryParams = new URLSearchParams();
    if (params?.workerId) queryParams.append('workerId', params.workerId);
    if (params?.stage) queryParams.append('stage', params.stage);
    if (params?.riskLevel) queryParams.append('riskLevel', params.riskLevel);
    if (params?.programId) queryParams.append('programId', params.programId);
    if (params?.requiresAttention !== undefined) queryParams.append('requiresAttention', params.requiresAttention.toString());
    if (params?.page !== undefined) queryParams.append('page', params.page.toString());
    if (params?.size !== undefined) queryParams.append('size', params.size.toString());
    if (params?.sort) queryParams.append('sort', params.sort);
    
    const query = queryParams.toString();
    return this.get<CaseloadResponse>(`/caseload${query ? '?' + query : ''}`);
  }

  async getMyCaseload(params?: {
    page?: number;
    size?: number;
  }): Promise<CaseloadResponse> {
    const queryParams = new URLSearchParams();
    if (params?.page !== undefined) queryParams.append('page', params.page.toString());
    if (params?.size !== undefined) queryParams.append('size', params.size.toString());
    
    const query = queryParams.toString();
    return this.get<CaseloadResponse>(`/caseload/my-cases${query ? '?' + query : ''}`);
  }

  async getTeamOverview(): Promise<TeamOverview> {
    return this.get<TeamOverview>('/caseload/team-overview');
  }

  async getConfidentialCases(): Promise<CaseloadItem[]> {
    return this.get<CaseloadItem[]>('/caseload/confidential');
  }

  // =====================================
  // Service Episodes
  // =====================================

  async createServiceEpisode(request: CreateServiceEpisodeRequest): Promise<CreateServiceEpisodeResponse> {
    return this.post<CreateServiceEpisodeResponse>('/api/v1/service-episodes', request);
  }

  async startService(episodeId: string, request: StartServiceRequest): Promise<void> {
    return this.post<void>(`/api/v1/service-episodes/${episodeId}/start`, request);
  }

  async completeService(episodeId: string, request: CompleteServiceRequest): Promise<void> {
    return this.post<void>(`/api/v1/service-episodes/${episodeId}/complete`, request);
  }

  async getServiceEpisode(episodeId: string): Promise<ServiceEpisode> {
    return this.get<ServiceEpisode>(`/api/v1/service-episodes/${episodeId}`);
  }

  async getServicesForClient(clientId: string): Promise<ServiceEpisode[]> {
    return this.get<ServiceEpisode[]>(`/api/v1/service-episodes/client/${clientId}`);
  }

  async getServicesForEnrollment(enrollmentId: string): Promise<ServiceEpisode[]> {
    return this.get<ServiceEpisode[]>(`/api/v1/service-episodes/enrollment/${enrollmentId}`);
  }

  async searchServices(criteria: ServiceSearchCriteria): Promise<ServiceEpisode[]> {
    const queryParams = new URLSearchParams();

    if (criteria.clientId) queryParams.append('clientId', criteria.clientId);
    if (criteria.enrollmentId) queryParams.append('enrollmentId', criteria.enrollmentId);
    if (criteria.programId) queryParams.append('programId', criteria.programId);
    if (criteria.serviceType) queryParams.append('serviceType', criteria.serviceType);
    if (criteria.serviceCategory) queryParams.append('serviceCategory', criteria.serviceCategory);
    if (criteria.deliveryMode) queryParams.append('deliveryMode', criteria.deliveryMode);
    if (criteria.startDate) queryParams.append('startDate', criteria.startDate);
    if (criteria.endDate) queryParams.append('endDate', criteria.endDate);
    if (criteria.providerId) queryParams.append('providerId', criteria.providerId);
    if (criteria.confidentialOnly) queryParams.append('confidentialOnly', 'true');
    if (criteria.courtOrderedOnly) queryParams.append('courtOrderedOnly', 'true');
    if (criteria.followUpRequired) queryParams.append('followUpRequired', 'true');

    const query = queryParams.toString();
    return this.get<ServiceEpisode[]>(`/api/v1/service-episodes/search${query ? '?' + query : ''}`);
  }

  async getServicesRequiringFollowUp(): Promise<ServiceEpisode[]> {
    return this.get<ServiceEpisode[]>('/api/v1/service-episodes/follow-up');
  }

  async updateServiceOutcome(episodeId: string, request: UpdateOutcomeRequest): Promise<void> {
    return this.put<void>(`/api/v1/service-episodes/${episodeId}/outcome`, request);
  }

  async addProviderToService(episodeId: string, request: {
    providerId: string;
    providerName: string;
    role?: string;
  }): Promise<void> {
    return this.post<void>(`/api/v1/service-episodes/${episodeId}/providers`, request);
  }

  async addFundingSourceToService(episodeId: string, request: {
    funderId: string;
    funderName?: string;
    grantNumber?: string;
    programName?: string;
    allocationPercentage: number;
  }): Promise<void> {
    return this.post<void>(`/api/v1/service-episodes/${episodeId}/funding`, request);
  }

  async markServiceAsCourtOrdered(episodeId: string, request: {
    courtOrderNumber: string;
  }): Promise<void> {
    return this.post<void>(`/api/v1/service-episodes/${episodeId}/court-order`, request);
  }

  async getServiceStatistics(startDate: string, endDate: string): Promise<ServiceStatistics> {
    const queryParams = new URLSearchParams();
    queryParams.append('startDate', startDate);
    queryParams.append('endDate', endDate);

    return this.get<ServiceStatistics>(`/api/v1/service-episodes/statistics?${queryParams.toString()}`);
  }

  // Quick service creation methods
  async createCrisisIntervention(request: QuickCrisisServiceRequest): Promise<CreateServiceEpisodeResponse> {
    return this.post<CreateServiceEpisodeResponse>('/api/v1/service-episodes/quick/crisis-intervention', request);
  }

  async createCounselingSession(request: QuickCounselingServiceRequest): Promise<CreateServiceEpisodeResponse> {
    return this.post<CreateServiceEpisodeResponse>('/api/v1/service-episodes/quick/counseling', request);
  }

  async createCaseManagementContact(request: QuickCaseManagementServiceRequest): Promise<CreateServiceEpisodeResponse> {
    return this.post<CreateServiceEpisodeResponse>('/api/v1/service-episodes/quick/case-management', request);
  }

  // Service configuration lookups
  async getServiceTypes(): Promise<ServiceTypeResponse[]> {
    return this.get<ServiceTypeResponse[]>('/api/v1/service-episodes/service-types');
  }

  async getDeliveryModes(): Promise<ServiceDeliveryModeResponse[]> {
    return this.get<ServiceDeliveryModeResponse[]>('/api/v1/service-episodes/delivery-modes');
  }

  async getFundingSources(): Promise<FundingSource[]> {
    return this.get<FundingSource[]>('/api/v1/service-episodes/funding-sources');
  }

  // =====================================
  // Enrollments
  // =====================================

  async getClientEnrollments(clientId: string): Promise<EnrollmentSummary[]> {
    return this.get<EnrollmentSummary[]>(`/api/v1/enrollments/client/${clientId}`);
  }

  async getEnrollmentChain(enrollmentId: string): Promise<EnrollmentSummary[]> {
    return this.get<EnrollmentSummary[]>(`/api/v1/enrollments/${enrollmentId}/chain`);
  }

  async transitionToRrh(thEnrollmentId: string, request: TransitionToRrhRequest): Promise<TransitionToRrhResponse> {
    return this.post<TransitionToRrhResponse>(`/api/v1/enrollments/${thEnrollmentId}/transition-to-rrh`, request);
  }

  async getCombinedServices(enrollmentId: string): Promise<CombinedServicesResponse> {
    return this.get<CombinedServicesResponse>(`/api/v1/enrollments/${enrollmentId}/combined-services`);
  }

  async updateMoveInDate(enrollmentId: string, moveInDate: string): Promise<void> {
    return this.patch(`/api/v1/enrollments/${enrollmentId}/move-in-date`, { moveInDate });
  }

  async getHudCoverage(): Promise<{
    apiName: string;
    elements: Array<{
      hudId: string;
      elementName: string;
      route: string;
      method: string;
      fieldName: string;
      implemented: boolean;
    }>;
    implementedCount: number;
    totalCount: number;
  }> {
    return this.get('/api/v1/enrollments/hud-coverage');
  }

  private async patch<T>(path: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.axios.patch<T>(path, data, config);
    return response.data;
  }

  // =====================================
  // Intake Assessment
  // =====================================

  async getIntakeAssessment(enrollmentId: string): Promise<IntakeAssessment> {
    return this.get<IntakeAssessment>(`/api/v1/intake/${enrollmentId}`);
  }

  async updateIntakeField(enrollmentId: string, request: UpdateIntakeFieldRequest): Promise<void> {
    return this.put<void>(`/api/v1/intake/${enrollmentId}/fields`, request);
  }

  async completeIntakeSection(enrollmentId: string, sectionId: string): Promise<void> {
    return this.post<void>(`/api/v1/intake/${enrollmentId}/sections/${sectionId}/complete`);
  }

  async submitIntakeAssessment(enrollmentId: string): Promise<void> {
    return this.post<void>(`/api/v1/intake/${enrollmentId}/submit`);
  }

  // =====================================
  // Financial Assistance
  // =====================================

  async getAssistanceRequests(clientId: string): Promise<FinancialAssistanceRequest[]> {
    return this.get<FinancialAssistanceRequest[]>(`/api/v1/financial-assistance/client/${clientId}`);
  }

  async getAssistanceRequest(requestId: string): Promise<FinancialAssistanceRequest> {
    return this.get<FinancialAssistanceRequest>(`/api/v1/financial-assistance/${requestId}`);
  }

  async createAssistanceRequest(request: CreateAssistanceRequestRequest): Promise<{ id: string }> {
    return this.post<{ id: string }>('/api/v1/financial-assistance', request);
  }

  async updateAssistanceRequest(requestId: string, request: Partial<CreateAssistanceRequestRequest>): Promise<void> {
    return this.put<void>(`/api/v1/financial-assistance/${requestId}`, request);
  }

  async submitAssistanceRequest(requestId: string): Promise<void> {
    return this.post<void>(`/api/v1/financial-assistance/${requestId}/submit`);
  }

  async approveAssistanceRequest(requestId: string, comments?: string): Promise<void> {
    return this.post<void>(`/api/v1/financial-assistance/${requestId}/approve`, { comments });
  }

  async rejectAssistanceRequest(requestId: string, reason: string): Promise<void> {
    return this.post<void>(`/api/v1/financial-assistance/${requestId}/reject`, { reason });
  }

  async getAssistanceSummary(clientId: string, enrollmentId: string): Promise<AssistanceSummary> {
    return this.get<AssistanceSummary>(`/api/v1/financial-assistance/summary/${clientId}/${enrollmentId}`);
  }

  async getAssistanceLedger(clientId: string, enrollmentId?: string): Promise<AssistanceLedgerEntry[]> {
    const path = enrollmentId 
      ? `/api/v1/financial-assistance/ledger/${clientId}/${enrollmentId}`
      : `/api/v1/financial-assistance/ledger/${clientId}`;
    return this.get<AssistanceLedgerEntry[]>(path);
  }

  async exportLedger(clientId: string, format: 'csv' | 'pdf', enrollmentId?: string): Promise<{ downloadUrl: string }> {
    const params = new URLSearchParams();
    params.append('format', format);
    if (enrollmentId) params.append('enrollmentId', enrollmentId);
    
    return this.post<{ downloadUrl: string }>(`/api/v1/financial-assistance/ledger/${clientId}/export?${params.toString()}`);
  }

  async getApprovalQueue(): Promise<ApprovalQueueItem[]> {
    return this.get<ApprovalQueueItem[]>('/api/v1/financial-assistance/approval-queue');
  }

  async getPayees(): Promise<Payee[]> {
    return this.get<Payee[]>('/api/v1/payees');
  }

  async getPayee(payeeId: string): Promise<Payee> {
    return this.get<Payee>(`/api/v1/payees/${payeeId}`);
  }

  // Household Composition API methods
  async getHouseholdComposition(householdId: string): Promise<HouseholdComposition> {
    return this.get<HouseholdComposition>(`/api/households/${householdId}`);
  }

  async getHouseholdMembers(householdId: string, asOfDate?: string): Promise<HouseholdMember[]> {
    const params = asOfDate ? `?asOfDate=${asOfDate}` : '';
    return this.get<HouseholdMember[]>(`/api/households/${householdId}/members${params}`);
  }

  async getActiveHouseholdForClient(clientId: string, asOfDate?: string): Promise<HouseholdComposition> {
    const params = asOfDate ? `?asOfDate=${asOfDate}` : '';
    return this.get<HouseholdComposition>(`/api/households/active-for-client/${clientId}${params}`);
  }

  async getHouseholdsByClient(clientId: string): Promise<HouseholdComposition[]> {
    return this.get<HouseholdComposition[]>(`/api/households/by-member/${clientId}`);
  }

  async createHouseholdComposition(request: CreateHouseholdCompositionRequest): Promise<HouseholdComposition> {
    return this.post<HouseholdComposition>('/api/households', request);
  }

  async addHouseholdMember(householdId: string, request: AddHouseholdMemberRequest): Promise<void> {
    return this.post<void>(`/api/households/${householdId}/members`, request);
  }

  async removeHouseholdMember(
    householdId: string, 
    memberId: string, 
    request: { effectiveDate: string; recordedBy: string; reason: string }
  ): Promise<void> {
    return this.delete<void>(`/api/households/${householdId}/members/${memberId}`, { data: request });
  }

  // Coordinated Entry
  async getCeAssessments(enrollmentId: string): Promise<CeAssessment[]> {
    return this.get<CeAssessment[]>(`/api/v1/enrollments/${enrollmentId}/ce-assessments`);
  }

  async createCeAssessment(enrollmentId: string, request: CreateCeAssessmentRequest): Promise<CeAssessment> {
    return this.post<CeAssessment>(`/api/v1/enrollments/${enrollmentId}/ce-assessments`, request);
  }

  async getCeEvents(enrollmentId: string): Promise<CeEvent[]> {
    return this.get<CeEvent[]>(`/api/v1/enrollments/${enrollmentId}/ce-events`);
  }

  async createCeEvent(enrollmentId: string, request: CreateCeEventRequest): Promise<CeEvent> {
    return this.post<CeEvent>(`/api/v1/enrollments/${enrollmentId}/ce-events`, request);
  }

  async createPayee(payee: Omit<Payee, 'id' | 'createdAt' | 'updatedAt'>): Promise<{ id: string }> {
    return this.post<{ id: string }>('/api/v1/payees', payee);
  }

  async getFinancialFundingSources(): Promise<FundingSource[]> {
    return this.get<FundingSource[]>('/api/v1/funding-sources');
  }

  async uploadSupportingDocument(file: File, documentType: string, description?: string): Promise<{ id: string; filename: string }> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('documentType', documentType);
    if (description) formData.append('description', description);

    return this.post<{ id: string; filename: string }>('/api/v1/documents/upload', formData);
  }

  // Restricted Notes API
  restrictedNotes = {
    getClientNotes: (clientId: string, filters?: RestrictedNoteFilters): Promise<RestrictedNote[]> => {
      const params = new URLSearchParams();
      if (filters) {
        Object.entries(filters).forEach(([key, value]) => {
          if (value !== undefined && value !== null) {
            params.append(key, String(value));
          }
        });
      }
      const queryString = params.toString();
      const url = `/api/clients/${clientId}/restricted-notes${queryString ? `?${queryString}` : ''}`;
      return this.get<RestrictedNote[]>(url);
    },

    getAccessibleNotes: (filters?: RestrictedNoteFilters): Promise<RestrictedNote[]> => {
      const params = new URLSearchParams();
      if (filters) {
        Object.entries(filters).forEach(([key, value]) => {
          if (value !== undefined && value !== null) {
            params.append(key, String(value));
          }
        });
      }
      const queryString = params.toString();
      const url = `/api/restricted-notes/accessible${queryString ? `?${queryString}` : ''}`;
      return this.get<RestrictedNote[]>(url);
    },

    getNote: (noteId: string): Promise<RestrictedNote> => {
      return this.get<RestrictedNote>(`/api/restricted-notes/${noteId}`);
    },

    createNote: (data: CreateRestrictedNoteRequest): Promise<RestrictedNoteResponse> => {
      return this.post<RestrictedNoteResponse>('/api/restricted-notes', data);
    },

    updateNote: (noteId: string, data: UpdateRestrictedNoteRequest): Promise<RestrictedNoteResponse> => {
      return this.put<RestrictedNoteResponse>(`/api/restricted-notes/${noteId}`, data);
    },

    sealNote: (noteId: string, data: SealNoteRequest): Promise<RestrictedNoteResponse> => {
      return this.post<RestrictedNoteResponse>(`/api/restricted-notes/${noteId}/seal`, data);
    },

    unsealNote: (noteId: string, data: UnsealNoteRequest): Promise<RestrictedNoteResponse> => {
      return this.post<RestrictedNoteResponse>(`/api/restricted-notes/${noteId}/unseal`, data);
    },

    getAuditTrail: (noteId: string): Promise<NoteAuditEntry[]> => {
      return this.get<NoteAuditEntry[]>(`/api/restricted-notes/${noteId}/audit-trail`);
    },

    getComplianceReport: (noteId: string): Promise<ComplianceReport> => {
      return this.get<ComplianceReport>(`/api/restricted-notes/${noteId}/compliance-report`);
    },

    getAccessLog: (noteId: string): Promise<NoteAuditEntry[]> => {
      return this.get<NoteAuditEntry[]>(`/api/restricted-notes/${noteId}/access-log`);
    }
  };

  // =====================================
  // Consent Ledger API
  // =====================================

  consentLedger = {
    /**
     * Search consent ledger with comprehensive filters and pagination
     */
    searchConsents: (params: ConsentSearchParams): Promise<PaginatedConsentResponse> => {
      const queryParams = new URLSearchParams();

      if (params.clientId) queryParams.append('clientId', params.clientId);
      if (params.consentType) queryParams.append('consentType', params.consentType);
      if (params.status) queryParams.append('status', params.status);
      if (params.recipientOrganization) queryParams.append('recipientOrganization', params.recipientOrganization);
      if (params.grantedAfter) queryParams.append('grantedAfter', params.grantedAfter);
      if (params.grantedBefore) queryParams.append('grantedBefore', params.grantedBefore);
      if (params.includeVAWAProtected !== undefined) queryParams.append('includeVAWAProtected', String(params.includeVAWAProtected));
      if (params.page !== undefined) queryParams.append('page', String(params.page));
      if (params.size !== undefined) queryParams.append('size', String(params.size));

      const queryString = queryParams.toString();
      return this.get<PaginatedConsentResponse>(`/api/consent-ledger/search${queryString ? `?${queryString}` : ''}`);
    },

    /**
     * Get consent ledger for a specific client
     */
    getClientConsents: (clientId: string, activeOnly: boolean = false): Promise<ConsentLedgerEntry[]> => {
      const queryParams = new URLSearchParams();
      if (activeOnly) queryParams.append('activeOnly', 'true');

      const queryString = queryParams.toString();
      return this.get<ConsentLedgerEntry[]>(`/api/consent-ledger/client/${clientId}${queryString ? `?${queryString}` : ''}`);
    },

    /**
     * Get consents expiring soon (requiring review)
     */
    getConsentsExpiringSoon: (daysAhead: number = 30): Promise<ConsentLedgerEntry[]> => {
      return this.get<ConsentLedgerEntry[]>(`/api/consent-ledger/expiring-soon?daysAhead=${daysAhead}`);
    },

    /**
     * Get expired consents
     */
    getExpiredConsents: (): Promise<ConsentLedgerEntry[]> => {
      return this.get<ConsentLedgerEntry[]>('/api/consent-ledger/expired');
    },

    /**
     * Get consents by recipient organization
     */
    getConsentsByRecipient: (recipientOrganization: string): Promise<ConsentLedgerEntry[]> => {
      return this.get<ConsentLedgerEntry[]>(`/api/consent-ledger/recipient/${recipientOrganization}`);
    },

    /**
     * Get VAWA protected consents (restricted access)
     */
    getVAWAProtectedConsents: (): Promise<ConsentLedgerEntry[]> => {
      return this.get<ConsentLedgerEntry[]>('/api/consent-ledger/vawa-protected');
    },

    /**
     * Get audit trail for a specific consent
     */
    getConsentAuditTrail: (consentId: string): Promise<ConsentAuditEntry[]> => {
      return this.get<ConsentAuditEntry[]>(`/api/consent-ledger/${consentId}/audit-trail`);
    },

    /**
     * Export consent ledger data (CSV format)
     */
    exportConsentLedger: async (params: ConsentSearchParams): Promise<Blob> => {
      const queryParams = new URLSearchParams();

      if (params.clientId) queryParams.append('clientId', params.clientId);
      if (params.consentType) queryParams.append('consentType', params.consentType);
      if (params.status) queryParams.append('status', params.status);
      if (params.recipientOrganization) queryParams.append('recipientOrganization', params.recipientOrganization);
      if (params.grantedAfter) queryParams.append('grantedAfter', params.grantedAfter);
      if (params.grantedBefore) queryParams.append('grantedBefore', params.grantedBefore);
      if (params.includeVAWAProtected !== undefined) queryParams.append('includeVAWAProtected', String(params.includeVAWAProtected));

      const queryString = queryParams.toString();
      const response = await this.axios.get(`/api/consent-ledger/export${queryString ? `?${queryString}` : ''}`, {
        responseType: 'blob'
      });
      return response.data;
    },

    /**
     * Get consent statistics for dashboard
     */
    getConsentStatistics: (): Promise<ConsentStatistics> => {
      return this.get<ConsentStatistics>('/api/consent-ledger/statistics');
    }
  };
}

// Default instance - can be customized with token refresh and auth handlers
export const apiClient = new ApiClient(
  process.env.NEXT_PUBLIC_API_URL || '/api'
);

// Factory function to create a client with custom auth handlers
export const createApiClient = (
  baseUrl?: string,
  onTokenRefresh?: () => Promise<string | null>,
  onAuthRequired?: () => void
) => {
  return new ApiClient(
    baseUrl || process.env.NEXT_PUBLIC_API_URL || '/api',
    onTokenRefresh,
    onAuthRequired
  );
};

// Helper function to handle API errors in components
export const handleApiError = (error: ApiError): string => {
  if (error.fieldErrors && Object.keys(error.fieldErrors).length > 0) {
    const fieldErrors = Object.values(error.fieldErrors).join(', ');
    return `Validation errors: ${fieldErrors}`;
  }
  return error.message;
};

// Export error class for instanceof checks
export { ApiError } from './types';

// Export new types for triage and caseload
export type {
  TriageDashboardData,
  TriageAlert,
  CaseloadResponse,
  CaseloadItem,
  TeamOverview,
} from './types';

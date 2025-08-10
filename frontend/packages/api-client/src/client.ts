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
} from './types';

export class ApiClient {
  private axios: AxiosInstance;
  private authToken: string | null = null;

  constructor(baseUrl: string) {
    this.axios = axios.create({
      baseURL: baseUrl,
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    this.setupInterceptors();
  }

  private setupInterceptors() {
    // Request interceptor for auth token
    this.axios.interceptors.request.use(
      (config) => {
        if (this.authToken) {
          config.headers.Authorization = `Bearer ${this.authToken}`;
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
      (error: AxiosError<ApiError>) => {
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
  async getCases(params?: CaseSearchParams): Promise<Case[]> {
    const queryParams = new URLSearchParams();
    if (params?.clientId) queryParams.append('clientId', params.clientId);
    if (params?.assigneeId) queryParams.append('assigneeId', params.assigneeId);
    if (params?.activeOnly) queryParams.append('activeOnly', 'true');
    if (params?.requiresAttention) queryParams.append('requiresAttention', 'true');
    if (params?.page) queryParams.append('page', params.page.toString());
    if (params?.limit) queryParams.append('limit', params.limit.toString());

    const query = queryParams.toString();
    return this.get<Case[]>(`/cases${query ? '?' + query : ''}`);
  }

  async getCase(id: string): Promise<Case> {
    return this.get<Case>(`/cases/${id}`);
  }

  async getCasesByClient(clientId: string): Promise<Case[]> {
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
    return this.get<{ status: string; timestamp: string }>('/health');
  }
}

// Default instance
export const apiClient = new ApiClient(
  process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api'
);

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
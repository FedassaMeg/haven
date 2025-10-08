import { useState, useEffect, useCallback } from 'react';
import { apiClient, handleApiError } from './client';
import type {
  Client,
  Case,
  CreateClientRequest,
  UpdateClientDemographicsRequest,
  OpenCaseRequest,
  ClientSearchParams,
  CaseSearchParams,
  ApiError,
  TriageDashboardData,
  TriageAlert,
  CaseloadResponse,
  CaseloadItem,
  TeamOverview,
  ServiceEpisode,
  CreateServiceEpisodeRequest,
  StartServiceRequest,
  CompleteServiceRequest,
  ServiceSearchCriteria,
  ServiceStatistics,
  ServiceTypeResponse,
  ServiceDeliveryModeResponse,
  FundingSource,
  CaseNote,
  AddCaseNoteRequest,
  ComplianceOverview,
  ComplianceMetric,
  AuditEntry,
  EnrollmentSummary,
  IntakeAssessment,
  FinancialAssistanceRequest,
  CreateAssistanceRequestRequest,
  AssistanceLedgerEntry,
  AssistanceSummary,
  Payee,
  AuditLogFilters,
  MandatedReport,
  CreateMandatedReportRequest,
  BillingRecord,
  BillingStatistics,
  BillingExportRequest,
  BillingExportResponse,
  GeneratedReport,
  GenerateReportRequest,
  HouseholdComposition,
  HouseholdMember,
  CreateHouseholdCompositionRequest,
  AddHouseholdMemberRequest,
  ConsentLedgerEntry,
  ConsentSearchParams,
  ConsentAuditEntry,
  ConsentStatistics,
  CeAssessment,
  CreateCeAssessmentRequest,
  CeEvent,
  CreateCeEventRequest,
  CeShareScope,
} from './types';

// Generic API state hook
interface ApiState<T> {
  data: T | null;
  loading: boolean;
  error: string | null;
}

function useApiState<T>(initialData: T | null = null): [
  ApiState<T>,
  {
    setData: (data: T | null) => void;
    setLoading: (loading: boolean) => void;
    setError: (error: string | null) => void;
    reset: () => void;
  }
] {
  const [state, setState] = useState<ApiState<T>>({
    data: initialData,
    loading: false,
    error: null,
  });

  const setData = useCallback((data: T | null) => {
    setState(prev => ({ ...prev, data, error: null, loading: false }));
  }, []);

  const setLoading = useCallback((loading: boolean) => {
    setState(prev => ({ ...prev, loading }));
  }, []);

  const setError = useCallback((error: string | null) => {
    setState(prev => ({ ...prev, error, loading: false }));
  }, []);

  const reset = useCallback(() => {
    setState({ data: initialData, loading: false, error: null });
  }, [initialData]);

  return [state, { setData, setLoading, setError, reset }];
}

// Client hooks
export function useClients(params?: ClientSearchParams) {
  const [state, { setData, setLoading, setError }] = useApiState<Client[]>([]);

  console.log(state);
  
  // Stabilize params to prevent infinite re-renders
  const paramsKey = JSON.stringify(params || {});

  useEffect(() => {
    const fetchClients = async () => {
      setLoading(true);
      try {
        const clients = await apiClient.getClients(params);
        setData(clients);
      } catch (error) {
        setError(handleApiError(error as ApiError));
      }
    };
    
    fetchClients();
  }, [paramsKey, setData, setLoading, setError]);

  const refetch = useCallback(async () => {
    setLoading(true);
    try {
      const clients = await apiClient.getClients(params);
      setData(clients);
    } catch (error) {
      setError(handleApiError(error as ApiError));
    }
  }, [params, setData, setLoading, setError]);

  return {
    clients: state.data,
    loading: state.loading,
    error: state.error,
    refetch,
  };
}

export function useClient(id: string | null) {
  const [state, { setData, setLoading, setError }] = useApiState<Client>();
  const [hasError, setHasError] = useState(false);

  const fetchClient = useCallback(async () => {
    if (!id) return;
    
    setLoading(true);
    setHasError(false);
    try {
      const client = await apiClient.getClient(id);
      setData(client);
    } catch (error) {
      setError(handleApiError(error as ApiError));
      setHasError(true);
    }
  }, [id, setData, setLoading, setError]);

  useEffect(() => {
    // Only fetch if we don't have an error
    if (!hasError) {
      fetchClient();
    }
  }, [fetchClient, hasError]);

  return {
    client: state.data,
    loading: state.loading,
    error: state.error,
    refetch: fetchClient,
  };
}

export function useCreateClient() {
  const [state, { setLoading, setError, reset }] = useApiState();

  const createClient = useCallback(async (data: CreateClientRequest) => {
    setLoading(true);
    try {
      const result = await apiClient.createClient(data);
      reset();
      return result;
    } catch (error) {
      setError(handleApiError(error as ApiError));
      throw error;
    }
  }, [setLoading, setError, reset]);

  return {
    createClient,
    loading: state.loading,
    error: state.error,
  };
}

export function useUpdateClientDemographics() {
  const [state, { setLoading, setError, reset }] = useApiState();

  const updateDemographics = useCallback(async (id: string, data: UpdateClientDemographicsRequest) => {
    setLoading(true);
    try {
      await apiClient.updateClientDemographics(id, data);
      reset();
    } catch (error) {
      setError(handleApiError(error as ApiError));
      throw error;
    }
  }, [setLoading, setError, reset]);

  return {
    updateDemographics,
    loading: state.loading,
    error: state.error,
  };
}

// Case hooks
export function useCases(params?: CaseSearchParams) {
  const [state, { setData, setLoading, setError }] = useApiState<Case[]>([]);
  
  // Stabilize params to prevent infinite re-renders
  const paramsKey = JSON.stringify(params || {});

  useEffect(() => {
    const fetchCases = async () => {
      setLoading(true);
      try {
        const response = await apiClient.getCases(params);
        setData(response.data);
      } catch (error) {
        setError(handleApiError(error as ApiError));
      }
    };
    
    fetchCases();
  }, [paramsKey, setData, setLoading, setError]);

  const refetch = useCallback(async () => {
    setLoading(true);
    try {
      const response = await apiClient.getCases(params);
      setData(response.data);
    } catch (error) {
      setError(handleApiError(error as ApiError));
    }
  }, [params, setData, setLoading, setError]);

  return {
    cases: state.data,
    loading: state.loading,
    error: state.error,
    refetch,
  };
}

export function useCase(id: string | null) {
  const [state, { setData, setLoading, setError }] = useApiState<Case>();
  const [hasError, setHasError] = useState(false);

  const fetchCase = useCallback(async () => {
    if (!id) return;
    
    setLoading(true);
    setHasError(false);
    try {
      const caseData = await apiClient.getCase(id);
      setData(caseData);
    } catch (error) {
      setError(handleApiError(error as ApiError));
      setHasError(true);
    }
  }, [id, setData, setLoading, setError]);

  useEffect(() => {
    // Only fetch if we don't have an error
    if (!hasError) {
      fetchCase();
    }
  }, [fetchCase, hasError]);

  return {
    case: state.data,
    loading: state.loading,
    error: state.error,
    refetch: fetchCase,
  };
}

export function useClientCases(clientId: string | null) {
  const [state, { setData, setLoading, setError }] = useApiState<Case[]>([]);
  const [hasError, setHasError] = useState(false);

  const fetchClientCases = useCallback(async () => {
    if (!clientId) return;
    
    setLoading(true);
    setHasError(false);
    try {
      const response = await apiClient.getCasesByClient(clientId);
      setData(response.data);
    } catch (error) {
      setError(handleApiError(error as ApiError));
      setHasError(true);
    }
  }, [clientId, setData, setLoading, setError]);

  useEffect(() => {
    // Only fetch if we don't have an error
    if (!hasError) {
      fetchClientCases();
    }
  }, [fetchClientCases, hasError]);

  return {
    cases: state.data,
    loading: state.loading,
    error: state.error,
    refetch: fetchClientCases,
  };
}

export function useOpenCase() {
  const [state, { setLoading, setError, reset }] = useApiState();

  const openCase = useCallback(async (data: OpenCaseRequest) => {
    setLoading(true);
    try {
      const result = await apiClient.openCase(data);
      reset();
      return result;
    } catch (error) {
      setError(handleApiError(error as ApiError));
      throw error;
    }
  }, [setLoading, setError, reset]);

  return {
    openCase,
    loading: state.loading,
    error: state.error,
  };
}

// Triage Dashboard hooks
export function useTriageDashboard(params?: { workerId?: string; daysAhead?: number }) {
  const [state, { setData, setLoading, setError }] = useApiState<TriageDashboardData>();
  const paramsKey = JSON.stringify(params || {});

  useEffect(() => {
    const fetchDashboard = async () => {
      setLoading(true);
      try {
        const dashboard = await apiClient.getTriageDashboard(params);
        setData(dashboard);
      } catch (error) {
        setError(handleApiError(error as ApiError));
      }
    };
    
    fetchDashboard();
  }, [paramsKey, setData, setLoading, setError]);

  const refetch = useCallback(async () => {
    setLoading(true);
    try {
      const dashboard = await apiClient.getTriageDashboard(params);
      setData(dashboard);
    } catch (error) {
      setError(handleApiError(error as ApiError));
    }
  }, [params, setData, setLoading, setError]);

  return {
    dashboard: state.data,
    loading: state.loading,
    error: state.error,
    refetch,
  };
}

export function useTriageAlerts(params?: {
  severity?: string;
  type?: string;
  status?: string;
  workerId?: string;
}) {
  const [state, { setData, setLoading, setError }] = useApiState<TriageAlert[]>([]);
  const paramsKey = JSON.stringify(params || {});

  useEffect(() => {
    const fetchAlerts = async () => {
      setLoading(true);
      try {
        const alerts = await apiClient.getTriageAlerts(params);
        setData(alerts);
      } catch (error) {
        setError(handleApiError(error as ApiError));
      }
    };
    
    fetchAlerts();
  }, [paramsKey, setData, setLoading, setError]);

  const acknowledgeAlert = useCallback(async (alertId: string) => {
    try {
      await apiClient.acknowledgeAlert(alertId);
      // Refetch alerts after acknowledging
      const alerts = await apiClient.getTriageAlerts(params);
      setData(alerts);
    } catch (error) {
      setError(handleApiError(error as ApiError));
    }
  }, [params, setData, setError]);

  const resolveAlert = useCallback(async (alertId: string) => {
    try {
      await apiClient.resolveAlert(alertId);
      // Refetch alerts after resolving
      const alerts = await apiClient.getTriageAlerts(params);
      setData(alerts);
    } catch (error) {
      setError(handleApiError(error as ApiError));
    }
  }, [params, setData, setError]);

  return {
    alerts: state.data,
    loading: state.loading,
    error: state.error,
    acknowledgeAlert,
    resolveAlert,
  };
}

// Caseload hooks
export function useCaseload(params?: {
  workerId?: string;
  stage?: string;
  riskLevel?: string;
  programId?: string;
  requiresAttention?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}) {
  const [state, { setData, setLoading, setError }] = useApiState<CaseloadResponse>();
  const paramsKey = JSON.stringify(params || {});

  useEffect(() => {
    const fetchCaseload = async () => {
      setLoading(true);
      try {
        const caseload = await apiClient.getCaseload(params);
        setData(caseload);
      } catch (error) {
        setError(handleApiError(error as ApiError));
      }
    };
    
    fetchCaseload();
  }, [paramsKey, setData, setLoading, setError]);

  const refetch = useCallback(async () => {
    setLoading(true);
    try {
      const caseload = await apiClient.getCaseload(params);
      setData(caseload);
    } catch (error) {
      setError(handleApiError(error as ApiError));
    }
  }, [params, setData, setLoading, setError]);

  return {
    caseload: state.data,
    loading: state.loading,
    error: state.error,
    refetch,
  };
}

export function useMyCaseload(params?: { page?: number; size?: number }) {
  const [state, { setData, setLoading, setError }] = useApiState<CaseloadResponse>();
  const paramsKey = JSON.stringify(params || {});

  useEffect(() => {
    const fetchMyCaseload = async () => {
      setLoading(true);
      try {
        const caseload = await apiClient.getMyCaseload(params);
        setData(caseload);
      } catch (error) {
        setError(handleApiError(error as ApiError));
      }
    };
    
    fetchMyCaseload();
  }, [paramsKey, setData, setLoading, setError]);

  const refetch = useCallback(async () => {
    setLoading(true);
    try {
      const caseload = await apiClient.getMyCaseload(params);
      setData(caseload);
    } catch (error) {
      setError(handleApiError(error as ApiError));
    }
  }, [params, setData, setLoading, setError]);

  return {
    caseload: state.data,
    loading: state.loading,
    error: state.error,
    refetch,
  };
}

export function useTeamOverview() {
  const [state, { setData, setLoading, setError }] = useApiState<TeamOverview>();

  useEffect(() => {
    const fetchOverview = async () => {
      setLoading(true);
      try {
        const overview = await apiClient.getTeamOverview();
        setData(overview);
      } catch (error) {
        setError(handleApiError(error as ApiError));
      }
    };
    
    fetchOverview();
  }, [setData, setLoading, setError]);

  const refetch = useCallback(async () => {
    setLoading(true);
    try {
      const overview = await apiClient.getTeamOverview();
      setData(overview);
    } catch (error) {
      setError(handleApiError(error as ApiError));
    }
  }, [setData, setLoading, setError]);

  return {
    overview: state.data,
    loading: state.loading,
    error: state.error,
    refetch,
  };
}

export function useConfidentialCases() {
  const [state, { setData, setLoading, setError }] = useApiState<CaseloadItem[]>([]);

  useEffect(() => {
    const fetchConfidentialCases = async () => {
      setLoading(true);
      try {
        const cases = await apiClient.getConfidentialCases();
        setData(cases);
      } catch (error) {
        setError(handleApiError(error as ApiError));
      }
    };
    
    fetchConfidentialCases();
  }, [setData, setLoading, setError]);

  const refetch = useCallback(async () => {
    setLoading(true);
    try {
      const cases = await apiClient.getConfidentialCases();
      setData(cases);
    } catch (error) {
      setError(handleApiError(error as ApiError));
    }
  }, [setData, setLoading, setError]);

  return {
    cases: state.data,
    loading: state.loading,
    error: state.error,
    refetch,
  };
}

// Utility hooks
// ServiceEpisode hooks
export function useServiceEpisodes(criteria?: ServiceSearchCriteria) {
  const [state, { setData, setLoading, setError }] = useApiState<{
    episodes: ServiceEpisode[];
    totalElements: number;
    totalPages: number;
  }>({ episodes: [], totalElements: 0, totalPages: 0 });
  const paramsKey = JSON.stringify(criteria || {});

  useEffect(() => {
    const fetchServiceEpisodes = async () => {
      setLoading(true);
      try {
        const episodes = await apiClient.searchServiceEpisodes(criteria);
        // For now, simulate pagination response - in real API this would be handled server-side
        setData({
          episodes,
          totalElements: episodes.length,
          totalPages: Math.ceil(episodes.length / (criteria?.size || 20))
        });
      } catch (error) {
        setError(handleApiError(error as ApiError));
      }
    };
    
    fetchServiceEpisodes();
  }, [paramsKey, setData, setLoading, setError]);

  const refetch = useCallback(async () => {
    setLoading(true);
    try {
      const episodes = await apiClient.searchServiceEpisodes(criteria);
      setData({
        episodes,
        totalElements: episodes.length,
        totalPages: Math.ceil(episodes.length / (criteria?.size || 20))
      });
    } catch (error) {
      setError(handleApiError(error as ApiError));
    }
  }, [criteria, setData, setLoading, setError]);

  return {
    serviceEpisodes: state.data?.episodes,
    totalElements: state.data?.totalElements,
    totalPages: state.data?.totalPages,
    loading: state.loading,
    error: state.error,
    refetch,
  };
}

export function useServiceEpisode(id: string | null) {
  const [state, { setData, setLoading, setError }] = useApiState<ServiceEpisode>();
  const [hasError, setHasError] = useState(false);

  const fetchServiceEpisode = useCallback(async () => {
    if (!id) return;
    
    setLoading(true);
    setHasError(false);
    try {
      const episode = await apiClient.getServiceEpisode(id);
      setData(episode);
    } catch (error) {
      setError(handleApiError(error as ApiError));
      setHasError(true);
    }
  }, [id, setData, setLoading, setError]);

  useEffect(() => {
    if (!hasError) {
      fetchServiceEpisode();
    }
  }, [fetchServiceEpisode, hasError]);

  return {
    serviceEpisode: state.data,
    loading: state.loading,
    error: state.error,
    refetch: fetchServiceEpisode,
  };
}

export function useClientServiceEpisodes(clientId: string | null) {
  const [state, { setData, setLoading, setError }] = useApiState<ServiceEpisode[]>([]);
  const [hasError, setHasError] = useState(false);

  const fetchClientServiceEpisodes = useCallback(async () => {
    if (!clientId) return;
    
    setLoading(true);
    setHasError(false);
    try {
      const episodes = await apiClient.getServiceEpisodesByClient(clientId);
      setData(episodes);
    } catch (error) {
      setError(handleApiError(error as ApiError));
      setHasError(true);
    }
  }, [clientId, setData, setLoading, setError]);

  useEffect(() => {
    if (!hasError) {
      fetchClientServiceEpisodes();
    }
  }, [fetchClientServiceEpisodes, hasError]);

  return {
    serviceEpisodes: state.data,
    loading: state.loading,
    error: state.error,
    refetch: fetchClientServiceEpisodes,
  };
}

export function useEnrollmentServiceEpisodes(enrollmentId: string | null) {
  const [state, { setData, setLoading, setError }] = useApiState<ServiceEpisode[]>([]);
  const [hasError, setHasError] = useState(false);

  const fetchEnrollmentServiceEpisodes = useCallback(async () => {
    if (!enrollmentId) return;
    
    setLoading(true);
    setHasError(false);
    try {
      const episodes = await apiClient.getServiceEpisodesByEnrollment(enrollmentId);
      setData(episodes);
    } catch (error) {
      setError(handleApiError(error as ApiError));
      setHasError(true);
    }
  }, [enrollmentId, setData, setLoading, setError]);

  useEffect(() => {
    if (!hasError) {
      fetchEnrollmentServiceEpisodes();
    }
  }, [fetchEnrollmentServiceEpisodes, hasError]);

  return {
    serviceEpisodes: state.data,
    loading: state.loading,
    error: state.error,
    refetch: fetchEnrollmentServiceEpisodes,
  };
}

export function useActiveServiceEpisodes(providerId?: string) {
  const [state, { setData, setLoading, setError }] = useApiState<ServiceEpisode[]>([]);

  useEffect(() => {
    const fetchActiveEpisodes = async () => {
      setLoading(true);
      try {
        const criteria: ServiceSearchCriteria = {
          status: 'IN_PROGRESS',
          ...(providerId && { providerId }),
        };
        const episodes = await apiClient.searchServiceEpisodes(criteria);
        setData(episodes);
      } catch (error) {
        setError(handleApiError(error as ApiError));
      }
    };
    
    fetchActiveEpisodes();
  }, [providerId, setData, setLoading, setError]);

  const refetch = useCallback(async () => {
    setLoading(true);
    try {
      const criteria: ServiceSearchCriteria = {
        status: 'IN_PROGRESS',
        ...(providerId && { providerId }),
      };
      const episodes = await apiClient.searchServiceEpisodes(criteria);
      setData(episodes);
    } catch (error) {
      setError(handleApiError(error as ApiError));
    }
  }, [providerId, setData, setLoading, setError]);

  return {
    activeServices: state.data,
    loading: state.loading,
    error: state.error,
    refetch,
  };
}

export function useCreateServiceEpisode() {
  const [state, { setLoading, setError, reset }] = useApiState();

  const createServiceEpisode = useCallback(async (data: CreateServiceEpisodeRequest) => {
    setLoading(true);
    try {
      const result = await apiClient.createServiceEpisode(data);
      reset();
      return result;
    } catch (error) {
      setError(handleApiError(error as ApiError));
      throw error;
    }
  }, [setLoading, setError, reset]);

  return {
    createServiceEpisode,
    loading: state.loading,
    error: state.error,
  };
}

export function useStartService() {
  const [state, { setLoading, setError, reset }] = useApiState();

  const startService = useCallback(async (episodeId: string, data: StartServiceRequest) => {
    setLoading(true);
    try {
      await apiClient.startService(episodeId, data);
      reset();
    } catch (error) {
      setError(handleApiError(error as ApiError));
      throw error;
    }
  }, [setLoading, setError, reset]);

  return {
    startService,
    loading: state.loading,
    error: state.error,
  };
}

export function useCompleteService() {
  const [state, { setLoading, setError, reset }] = useApiState();

  const completeService = useCallback(async (episodeId: string, data: CompleteServiceRequest) => {
    setLoading(true);
    try {
      await apiClient.completeService(episodeId, data);
      reset();
    } catch (error) {
      setError(handleApiError(error as ApiError));
      throw error;
    }
  }, [setLoading, setError, reset]);

  return {
    completeService,
    loading: state.loading,
    error: state.error,
  };
}

export function useServiceStatistics(params?: { 
  providerId?: string; 
  dateRange?: { start: Date; end: Date }; 
}) {
  const [state, { setData, setLoading, setError }] = useApiState<ServiceStatistics>();
  const paramsKey = JSON.stringify(params || {});

  useEffect(() => {
    const fetchStatistics = async () => {
      setLoading(true);
      try {
        const statistics = await apiClient.getServiceStatistics(params);
        setData(statistics);
      } catch (error) {
        setError(handleApiError(error as ApiError));
      }
    };
    
    fetchStatistics();
  }, [paramsKey, setData, setLoading, setError]);

  const refetch = useCallback(async () => {
    setLoading(true);
    try {
      const statistics = await apiClient.getServiceStatistics(params);
      setData(statistics);
    } catch (error) {
      setError(handleApiError(error as ApiError));
    }
  }, [params, setData, setLoading, setError]);

  return {
    statistics: state.data,
    loading: state.loading,
    error: state.error,
    refetch,
  };
}

export function useServiceTypes() {
  const [state, { setData, setLoading, setError }] = useApiState<ServiceTypeResponse[]>([]);

  useEffect(() => {
    const fetchServiceTypes = async () => {
      setLoading(true);
      try {
        const types = await apiClient.getServiceTypes();
        setData(types);
      } catch (error) {
        setError(handleApiError(error as ApiError));
      }
    };
    
    fetchServiceTypes();
  }, [setData, setLoading, setError]);

  return {
    serviceTypes: state.data,
    loading: state.loading,
    error: state.error,
  };
}

export function useServiceDeliveryModes() {
  const [state, { setData, setLoading, setError }] = useApiState<ServiceDeliveryModeResponse[]>([]);

  useEffect(() => {
    const fetchDeliveryModes = async () => {
      setLoading(true);
      try {
        const modes = await apiClient.getServiceDeliveryModes();
        setData(modes);
      } catch (error) {
        setError(handleApiError(error as ApiError));
      }
    };
    
    fetchDeliveryModes();
  }, [setData, setLoading, setError]);

  return {
    deliveryModes: state.data,
    loading: state.loading,
    error: state.error,
  };
}

export function useFundingSources() {
  const [state, { setData, setLoading, setError }] = useApiState<FundingSource[]>([]);

  useEffect(() => {
    const fetchFundingSources = async () => {
      setLoading(true);
      try {
        const sources = await apiClient.getFundingSources();
        setData(sources);
      } catch (error) {
        setError(handleApiError(error as ApiError));
      }
    };
    
    fetchFundingSources();
  }, [setData, setLoading, setError]);

  return {
    fundingSources: state.data,
    loading: state.loading,
    error: state.error,
  };
}

// Case Notes hooks
export function useCaseNotes(caseId: string | null) {
  const [state, { setData, setLoading, setError }] = useApiState<CaseNote[]>([]);
  const [hasError, setHasError] = useState(false);

  const fetchCaseNotes = useCallback(async () => {
    if (!caseId) return;
    
    setLoading(true);
    setHasError(false);
    try {
      const notes = await apiClient.getCaseNotes(caseId);
      setData(notes);
    } catch (error) {
      setError(handleApiError(error as ApiError));
      setHasError(true);
    }
  }, [caseId, setData, setLoading, setError]);

  useEffect(() => {
    if (!hasError) {
      fetchCaseNotes();
    }
  }, [fetchCaseNotes, hasError]);

  const addNote = useCallback(async (noteData: AddCaseNoteRequest) => {
    try {
      await apiClient.addCaseNote(noteData);
      // Refetch notes after adding
      await fetchCaseNotes();
    } catch (error) {
      setError(handleApiError(error as ApiError));
      throw error;
    }
  }, [fetchCaseNotes, setError]);

  return {
    notes: state.data,
    loading: state.loading,
    error: state.error,
    refetch: fetchCaseNotes,
    addNote,
  };
}

export function useApiHealth() {
  const [state, { setData, setLoading, setError }] = useApiState<{
    status: string;
    timestamp: string;
  }>();
  const [hasError, setHasError] = useState(false);

  const checkHealth = useCallback(async () => {
    setLoading(true);
    setHasError(false);
    try {
      const health = await apiClient.healthCheck();
      setData(health);
    } catch (error) {
      setError(handleApiError(error as ApiError));
      setHasError(true);
    }
  }, [setData, setLoading, setError]);

  useEffect(() => {
    // Only fetch if we don't have an error
    if (!hasError) {
      checkHealth();
    }
  }, [checkHealth, hasError]);

  return {
    health: state.data,
    loading: state.loading,
    error: state.error,
    refetch: checkHealth,
  };
}

// Compliance hooks
export function useComplianceOverview() {
  const [state, { setData, setLoading, setError }] = useApiState<ComplianceOverview>();

  useEffect(() => {
    const fetchOverview = async () => {
      setLoading(true);
      try {
        // Simulate API call - replace with actual API call when available
        const mockOverview: ComplianceOverview = {
          overallScore: 87,
          metrics: [
            {
              name: "Documentation Completeness",
              description: "Percentage of cases with complete documentation",
              target: 95,
              achieved: 92,
              unit: "%",
              category: "Documentation"
            },
            {
              name: "Service Frequency",
              description: "Clients receiving services within required timeframes",
              target: 90,
              achieved: 85,
              unit: "%",
              category: "Service Delivery"
            },
            {
              name: "Consent Forms",
              description: "Valid consent forms on file",
              target: 100,
              achieved: 98,
              unit: "%",
              category: "Legal Compliance"
            }
          ],
          lastAuditDate: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString()
        };
        setData(mockOverview);
      } catch (error) {
        setError(handleApiError(error as ApiError));
      }
    };
    
    fetchOverview();
  }, [setData, setLoading, setError]);

  const refetch = useCallback(async () => {
    setLoading(true);
    try {
      // Replace with actual API call
      const mockOverview: ComplianceOverview = {
        overallScore: 87,
        metrics: [],
        lastAuditDate: new Date().toISOString()
      };
      setData(mockOverview);
    } catch (error) {
      setError(handleApiError(error as ApiError));
    }
  }, [setData, setLoading, setError]);

  return {
    overview: state.data,
    loading: state.loading,
    error: state.error,
    refetch,
  };
}

// HUD Compliance hooks
export function useHudComplianceMatrix() {
  const [state, { setData, setLoading, setError }] = useApiState<HudComplianceMatrix>();

  useEffect(() => {
    const fetchMatrix = async () => {
      setLoading(true);
      try {
        const response = await fetch('/api/v1/compliance/matrix');
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        const matrix: HudComplianceMatrix = await response.json();
        setData(matrix);
      } catch (error) {
        setError(handleApiError(error as ApiError));
      }
    };
    
    fetchMatrix();
  }, [setData, setLoading, setError]);

  const refetch = useCallback(async () => {
    setLoading(true);
    try {
      const response = await fetch('/api/v1/compliance/matrix');
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
      const matrix: HudComplianceMatrix = await response.json();
      setData(matrix);
    } catch (error) {
      setError(handleApiError(error as ApiError));
    }
  }, [setData, setLoading, setError]);

  return {
    matrix: state.data,
    loading: state.loading,
    error: state.error,
    refetch,
  };
}

export function useHudComplianceSummary() {
  const [state, { setData, setLoading, setError }] = useApiState<ComplianceSummaryResponse>();

  useEffect(() => {
    const fetchSummary = async () => {
      setLoading(true);
      try {
        const response = await fetch('/api/v1/compliance/summary');
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        const summary: ComplianceSummaryResponse = await response.json();
        setData(summary);
      } catch (error) {
        setError(handleApiError(error as ApiError));
      }
    };
    
    fetchSummary();
  }, [setData, setLoading, setError]);

  const refetch = useCallback(async () => {
    setLoading(true);
    try {
      const response = await fetch('/api/v1/compliance/summary');
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
      const summary: ComplianceSummaryResponse = await response.json();
      setData(summary);
    } catch (error) {
      setError(handleApiError(error as ApiError));
    }
  }, [setData, setLoading, setError]);

  return {
    summary: state.data,
    loading: state.loading,
    error: state.error,
    refetch,
  };
}

export function useHudElementsByCategory(category?: string) {
  const [state, { setData, setLoading, setError }] = useApiState<HudDataElement[]>([]);

  useEffect(() => {
    const fetchElements = async () => {
      setLoading(true);
      try {
        const url = new URL('/api/v1/compliance/elements', window.location.origin);
        if (category) {
          url.searchParams.set('category', category);
        }
        
        const response = await fetch(url.toString());
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        const elements: HudDataElement[] = await response.json();
        setData(elements);
      } catch (error) {
        setError(handleApiError(error as ApiError));
      }
    };
    
    fetchElements();
  }, [category, setData, setLoading, setError]);

  const refetch = useCallback(async () => {
    setLoading(true);
    try {
      const url = new URL('/api/v1/compliance/elements', window.location.origin);
      if (category) {
        url.searchParams.set('category', category);
      }
      
      const response = await fetch(url.toString());
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
      const elements: HudDataElement[] = await response.json();
      setData(elements);
    } catch (error) {
      setError(handleApiError(error as ApiError));
    }
  }, [category, setData, setLoading, setError]);

  return {
    elements: state.data,
    loading: state.loading,
    error: state.error,
    refetch,
  };
}

// Audit Log hooks
export function useAuditLog(filters?: AuditLogFilters) {
  const [state, { setData, setLoading, setError }] = useApiState<AuditEntry[]>([]);
  const filtersKey = JSON.stringify(filters || {});

  useEffect(() => {
    const fetchAuditLog = async () => {
      setLoading(true);
      try {
        // Simulate API call - replace with actual API call when available
        const mockAuditLog: AuditEntry[] = [
          {
            id: '1',
            userId: 'user-123',
            userName: 'John Smith',
            action: 'CREATE',
            resource: 'CLIENT',
            timestamp: new Date().toISOString(),
            details: 'Created new client record',
            result: 'SUCCESS',
            metadata: { clientId: 'client-456' }
          },
          {
            id: '2',
            userId: 'user-456',
            userName: 'Jane Doe',
            action: 'UPDATE',
            resource: 'CASE',
            timestamp: new Date(Date.now() - 60 * 60 * 1000).toISOString(),
            details: 'Updated case status',
            result: 'SUCCESS',
            metadata: { caseId: 'case-789' }
          }
        ];
        setData(mockAuditLog);
      } catch (error) {
        setError(handleApiError(error as ApiError));
      }
    };
    
    fetchAuditLog();
  }, [filtersKey, setData, setLoading, setError]);

  const refetch = useCallback(async () => {
    setLoading(true);
    try {
      // Replace with actual API call
      const mockAuditLog: AuditEntry[] = [];
      setData(mockAuditLog);
    } catch (error) {
      setError(handleApiError(error as ApiError));
    }
  }, [filters, setData, setLoading, setError]);

  return {
    auditLog: state.data,
    loading: state.loading,
    error: state.error,
    refetch,
  };
}

// Mandated Reports hooks
export function useMandatedReports(filters?: {
  type?: string;
  status?: string;
  priority?: string;
  assignedTo?: string;
  dueWithinDays?: number;
}) {
  const [state, { setData, setLoading, setError }] = useApiState<MandatedReport[]>([]);
  const filtersKey = JSON.stringify(filters || {});

  useEffect(() => {
    const fetchMandatedReports = async () => {
      setLoading(true);
      try {
        const currentDate = new Date();
        const mockReports: MandatedReport[] = [
          {
            id: '1',
            title: 'HUD Annual Progress Report',
            description: 'Annual report on housing outcomes and service delivery metrics for HUD CoC funding',
            type: 'FEDERAL',
            frequency: 'ANNUAL',
            dueDate: new Date(currentDate.getFullYear(), 11, 31).toISOString(),
            status: 'IN_PROGRESS',
            priority: 'HIGH',
            assignedTo: 'user-123',
            assignedToName: 'Sarah Johnson',
            submissionDeadline: new Date(currentDate.getFullYear(), 11, 31).toISOString(),
            lastSubmissionDate: new Date(currentDate.getFullYear() - 1, 11, 15).toISOString(),
            isOverdue: false,
            daysUntilDue: Math.ceil((new Date(currentDate.getFullYear(), 11, 31).getTime() - currentDate.getTime()) / (1000 * 60 * 60 * 24)),
            completionPercentage: 65,
            requiredSections: ['Demographics', 'Service Delivery', 'Housing Outcomes', 'Financial Summary'],
            completedSections: ['Demographics', 'Service Delivery', 'Housing Outcomes'],
            fundingSource: 'HUD CoC PSH',
            regulatoryBody: 'U.S. Department of Housing and Urban Development',
            createdAt: new Date(currentDate.getFullYear(), 9, 1).toISOString(),
            updatedAt: new Date().toISOString()
          },
          {
            id: '2',
            title: 'State DV Services Quarterly Report',
            description: 'Quarterly domestic violence services report including safety outcomes and client demographics',
            type: 'STATE',
            frequency: 'QUARTERLY',
            dueDate: new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 15).toISOString(),
            status: 'NOT_STARTED',
            priority: 'CRITICAL',
            assignedTo: 'user-456',
            assignedToName: 'Michael Chen',
            submissionDeadline: new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 15).toISOString(),
            lastSubmissionDate: new Date(currentDate.getFullYear(), currentDate.getMonth() - 3, 10).toISOString(),
            isOverdue: false,
            daysUntilDue: Math.ceil((new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 15).getTime() - currentDate.getTime()) / (1000 * 60 * 60 * 24)),
            completionPercentage: 0,
            requiredSections: ['Client Demographics', 'Service Statistics', 'Safety Outcomes', 'Financial Data'],
            completedSections: [],
            fundingSource: 'State VOCA Funds',
            regulatoryBody: 'State Coalition Against Domestic Violence',
            createdAt: new Date(currentDate.getFullYear(), currentDate.getMonth() - 2, 1).toISOString(),
            updatedAt: new Date(currentDate.getFullYear(), currentDate.getMonth() - 1, 15).toISOString()
          },
          {
            id: '3',
            title: 'HMIS Data Quality Report',
            description: 'Monthly data quality assessment and completeness report for HMIS participation',
            type: 'LOCAL',
            frequency: 'MONTHLY',
            dueDate: new Date(currentDate.getFullYear(), currentDate.getMonth(), 5).toISOString(),
            status: 'OVERDUE',
            priority: 'HIGH',
            assignedTo: 'user-789',
            assignedToName: 'Lisa Rodriguez',
            submissionDeadline: new Date(currentDate.getFullYear(), currentDate.getMonth(), 5).toISOString(),
            lastSubmissionDate: new Date(currentDate.getFullYear(), currentDate.getMonth() - 2, 3).toISOString(),
            isOverdue: true,
            daysUntilDue: Math.ceil((new Date(currentDate.getFullYear(), currentDate.getMonth(), 5).getTime() - currentDate.getTime()) / (1000 * 60 * 60 * 24)),
            completionPercentage: 30,
            requiredSections: ['Data Completeness', 'Data Quality Metrics', 'Error Resolution'],
            completedSections: ['Data Completeness'],
            fundingSource: 'CoC HMIS',
            regulatoryBody: 'Regional CoC Board',
            createdAt: new Date(currentDate.getFullYear(), currentDate.getMonth() - 1, 1).toISOString(),
            updatedAt: new Date(currentDate.getFullYear(), currentDate.getMonth() - 1, 20).toISOString()
          },
          {
            id: '4',
            title: 'Foundation Grant Impact Report',
            description: 'Semi-annual impact assessment for private foundation funding including client outcomes',
            type: 'FUNDER',
            frequency: 'SEMI_ANNUAL',
            dueDate: new Date(currentDate.getFullYear(), 5, 30).toISOString(),
            status: 'SUBMITTED',
            priority: 'MEDIUM',
            assignedTo: 'user-321',
            assignedToName: 'David Park',
            submissionDeadline: new Date(currentDate.getFullYear(), 5, 30).toISOString(),
            lastSubmissionDate: new Date(currentDate.getFullYear(), 5, 25).toISOString(),
            isOverdue: false,
            daysUntilDue: Math.ceil((new Date(currentDate.getFullYear() + 1, 5, 30).getTime() - currentDate.getTime()) / (1000 * 60 * 60 * 24)),
            completionPercentage: 100,
            requiredSections: ['Program Impact', 'Client Outcomes', 'Budget Utilization', 'Success Stories'],
            completedSections: ['Program Impact', 'Client Outcomes', 'Budget Utilization', 'Success Stories'],
            fundingSource: 'ABC Family Foundation',
            regulatoryBody: 'ABC Family Foundation',
            createdAt: new Date(currentDate.getFullYear(), 3, 1).toISOString(),
            updatedAt: new Date(currentDate.getFullYear(), 5, 25).toISOString()
          },
          {
            id: '5',
            title: 'Federal VAWA Grant Compliance Report',
            description: 'Annual Violence Against Women Act grant compliance and performance report',
            type: 'FEDERAL',
            frequency: 'ANNUAL',
            dueDate: new Date(currentDate.getFullYear(), 8, 30).toISOString(),
            status: 'REVIEW',
            priority: 'CRITICAL',
            assignedTo: 'user-654',
            assignedToName: 'Jennifer Adams',
            submissionDeadline: new Date(currentDate.getFullYear(), 8, 30).toISOString(),
            lastSubmissionDate: new Date(currentDate.getFullYear() - 1, 8, 28).toISOString(),
            isOverdue: currentDate > new Date(currentDate.getFullYear(), 8, 30),
            daysUntilDue: Math.ceil((new Date(currentDate.getFullYear(), 8, 30).getTime() - currentDate.getTime()) / (1000 * 60 * 60 * 24)),
            completionPercentage: 85,
            requiredSections: ['Grant Performance', 'Legal Advocacy Metrics', 'Training Requirements', 'Financial Report'],
            completedSections: ['Grant Performance', 'Legal Advocacy Metrics', 'Training Requirements'],
            fundingSource: 'DOJ VAWA',
            regulatoryBody: 'U.S. Department of Justice',
            createdAt: new Date(currentDate.getFullYear(), 6, 1).toISOString(),
            updatedAt: new Date().toISOString()
          }
        ];

        let filteredReports = mockReports;

        if (filters?.type) {
          filteredReports = filteredReports.filter(report => report.type === filters.type);
        }
        if (filters?.status) {
          filteredReports = filteredReports.filter(report => report.status === filters.status);
        }
        if (filters?.priority) {
          filteredReports = filteredReports.filter(report => report.priority === filters.priority);
        }
        if (filters?.assignedTo) {
          filteredReports = filteredReports.filter(report => report.assignedTo === filters.assignedTo);
        }
        if (filters?.dueWithinDays) {
          filteredReports = filteredReports.filter(report => report.daysUntilDue <= filters.dueWithinDays!);
        }

        setData(filteredReports);
      } catch (error) {
        setError(handleApiError(error as ApiError));
      }
    };
    
    fetchMandatedReports();
  }, [filtersKey, setData, setLoading, setError]);

  const refetch = useCallback(async () => {
    setLoading(true);
    try {
      const mockReports: MandatedReport[] = [];
      setData(mockReports);
    } catch (error) {
      setError(handleApiError(error as ApiError));
    }
  }, [filters, setData, setLoading, setError]);

  const updateReportStatus = useCallback(async (reportId: string, status: MandatedReport['status']) => {
    try {
      if (state.data) {
        const updatedReports = state.data.map(report => 
          report.id === reportId 
            ? { ...report, status, updatedAt: new Date().toISOString() }
            : report
        );
        setData(updatedReports);
      }
    } catch (error) {
      setError(handleApiError(error as ApiError));
      throw error;
    }
  }, [state.data, setData, setError]);

  const updateReportProgress = useCallback(async (reportId: string, completedSections: string[]) => {
    try {
      if (state.data) {
        const updatedReports = state.data.map(report => {
          if (report.id === reportId) {
            const completionPercentage = Math.round((completedSections.length / report.requiredSections.length) * 100);
            return { 
              ...report, 
              completedSections,
              completionPercentage,
              updatedAt: new Date().toISOString()
            };
          }
          return report;
        });
        setData(updatedReports);
      }
    } catch (error) {
      setError(handleApiError(error as ApiError));
      throw error;
    }
  }, [state.data, setData, setError]);

  return {
    reports: state.data,
    loading: state.loading,
    error: state.error,
    refetch,
    updateReportStatus,
    updateReportProgress,
  };
}

export function useCreateMandatedReport() {
  const [state, { setLoading, setError, reset }] = useApiState();

  const createMandatedReport = useCallback(async (data: CreateMandatedReportRequest): Promise<MandatedReport> => {
    setLoading(true);
    try {
      const currentDate = new Date();
      const dueDate = new Date(data.submissionDeadline);
      const daysUntilDue = Math.ceil((dueDate.getTime() - currentDate.getTime()) / (1000 * 60 * 60 * 24));
      
      const newReport: MandatedReport = {
        id: `report-${Date.now()}`,
        title: data.title,
        description: data.description,
        type: data.type,
        frequency: data.frequency,
        dueDate: data.submissionDeadline,
        status: 'NOT_STARTED',
        priority: data.priority,
        assignedTo: data.assignedTo,
        assignedToName: data.assignedTo ? `User ${data.assignedTo}` : undefined,
        submissionDeadline: data.submissionDeadline,
        isOverdue: daysUntilDue < 0,
        daysUntilDue,
        completionPercentage: 0,
        requiredSections: data.requiredSections,
        completedSections: [],
        fundingSource: data.fundingSource,
        regulatoryBody: data.regulatoryBody,
        createdAt: currentDate.toISOString(),
        updatedAt: currentDate.toISOString(),
      };

      reset();
      return newReport;
    } catch (error) {
      setError(handleApiError(error as ApiError));
      throw error;
    }
  }, [setLoading, setError, reset]);

  return {
    createMandatedReport,
    loading: state.loading,
    error: state.error,
  };
}

// Billing hooks
export function useBillingStatistics(params?: {
  dateRange?: { startDate: string; endDate: string };
  providerId?: string;
  fundingSource?: string;
}) {
  const [state, { setData, setLoading, setError }] = useApiState<BillingStatistics>();
  const paramsKey = JSON.stringify(params || {});

  useEffect(() => {
    const fetchBillingStatistics = async () => {
      setLoading(true);
      try {
        const currentDate = new Date();
        const currentYear = currentDate.getFullYear();
        const currentMonth = currentDate.getMonth();

        const mockStatistics: BillingStatistics = {
          totalBillable: 125780.50,
          totalPaid: 98450.25,
          totalPending: 18230.75,
          totalSubmitted: 7850.00,
          totalRejected: 1249.50,
          totalProcessing: 0,
          averageRate: 85.50,
          totalHours: 1471.2,
          reimbursementRate: 78.3,
          averageProcessingDays: 12.5,
          rejectionRate: 0.99,
          monthlyRevenue: 18230.75,
          yearToDateRevenue: 125780.50,
          fundingSourceBreakdown: {
            'HUD CoC': {
              amount: 45250.25,
              count: 145,
              percentage: 36.0
            },
            'VAWA': {
              amount: 32180.75,
              count: 98,
              percentage: 25.6
            },
            'State VOCA': {
              amount: 28430.00,
              count: 112,
              percentage: 22.6
            },
            'Cal OES': {
              amount: 14920.50,
              count: 67,
              percentage: 11.9
            },
            'Foundation': {
              amount: 4999.00,
              count: 23,
              percentage: 4.0
            }
          },
          statusBreakdown: {
            'PAID': {
              amount: 98450.25,
              count: 324,
              percentage: 78.3
            },
            'PENDING': {
              amount: 18230.75,
              count: 67,
              percentage: 14.5
            },
            'SUBMITTED': {
              amount: 7850.00,
              count: 28,
              percentage: 6.2
            },
            'REJECTED': {
              amount: 1249.50,
              count: 8,
              percentage: 0.99
            }
          },
          monthlyTrends: [
            {
              month: `${currentYear - 1}-12`,
              billable: 14250.00,
              paid: 11200.00,
              submitted: 2400.00,
              rejected: 650.00
            },
            {
              month: `${currentYear}-01`,
              billable: 15680.50,
              paid: 12340.25,
              submitted: 2890.25,
              rejected: 450.00
            },
            {
              month: `${currentYear}-02`,
              billable: 13920.75,
              paid: 10850.50,
              submitted: 2670.25,
              rejected: 400.00
            },
            {
              month: `${currentYear}-03`,
              billable: 16450.25,
              paid: 13250.00,
              submitted: 2800.25,
              rejected: 400.00
            },
            {
              month: `${currentYear}-04`,
              billable: 14780.00,
              paid: 11450.75,
              submitted: 2929.25,
              rejected: 400.00
            },
            {
              month: `${currentYear}-05`,
              billable: 15230.75,
              paid: 12180.50,
              submitted: 2650.25,
              rejected: 400.00
            }
          ]
        };

        if (params?.providerId) {
          mockStatistics.totalBillable *= 0.3;
          mockStatistics.totalPaid *= 0.3;
          mockStatistics.totalPending *= 0.3;
          mockStatistics.totalSubmitted *= 0.3;
          mockStatistics.totalRejected *= 0.3;
        }

        setData(mockStatistics);
      } catch (error) {
        setError(handleApiError(error as ApiError));
      }
    };
    
    fetchBillingStatistics();
  }, [paramsKey, setData, setLoading, setError]);

  const refetch = useCallback(async () => {
    setLoading(true);
    try {
      const mockStatistics: BillingStatistics = {
        totalBillable: 125780.50,
        totalPaid: 98450.25,
        totalPending: 18230.75,
        totalSubmitted: 7850.00,
        totalRejected: 1249.50,
        totalProcessing: 0,
        averageRate: 85.50,
        totalHours: 1471.2,
        reimbursementRate: 78.3,
        averageProcessingDays: 12.5,
        rejectionRate: 0.99,
        monthlyRevenue: 18230.75,
        yearToDateRevenue: 125780.50,
        fundingSourceBreakdown: {},
        statusBreakdown: {},
        monthlyTrends: []
      };
      setData(mockStatistics);
    } catch (error) {
      setError(handleApiError(error as ApiError));
    }
  }, [params, setData, setLoading, setError]);

  return {
    billingStats: state.data,
    loading: state.loading,
    error: state.error,
    refetch,
  };
}

export function useExportBilling() {
  const [state, { setLoading, setError, reset }] = useApiState<BillingExportResponse>();

  const exportBilling = useCallback(async (request: BillingExportRequest): Promise<BillingExportResponse> => {
    setLoading(true);
    try {
      const exportResponse: BillingExportResponse = {
        exportId: `export-${Date.now()}`,
        filename: `billing-export-${new Date().toISOString().split('T')[0]}.${request.format}`,
        status: 'PROCESSING',
        createdAt: new Date().toISOString(),
      };

      setTimeout(async () => {
        exportResponse.status = 'COMPLETED';
        exportResponse.completedAt = new Date().toISOString();
        exportResponse.downloadUrl = `/api/exports/${exportResponse.exportId}/download`;
        
        if (typeof window !== 'undefined') {
          const blob = new Blob(['Mock billing export data'], { type: 'text/csv' });
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = exportResponse.filename;
          document.body.appendChild(a);
          a.click();
          document.body.removeChild(a);
          URL.revokeObjectURL(url);
        }
      }, 1500);

      reset();
      return exportResponse;
    } catch (error) {
      setError(handleApiError(error as ApiError));
      throw error;
    }
  }, [setLoading, setError, reset]);

  return {
    exportBilling,
    loading: state.loading,
    error: state.error,
  };
}

export function useGenerateReport() {
  const [state, { setLoading, setError, reset }] = useApiState<GeneratedReport>();

  const generateReport = useCallback(async (request: GenerateReportRequest): Promise<GeneratedReport> => {
    setLoading(true);
    try {
      const reportResponse: GeneratedReport = {
        id: `report-${Date.now()}`,
        title: request.title,
        description: request.description,
        type: request.type,
        format: request.format,
        parameters: request.parameters,
        status: 'GENERATING',
        createdBy: 'current-user-id',
        createdAt: new Date().toISOString(),
      };

      setTimeout(async () => {
        reportResponse.status = 'COMPLETED';
        reportResponse.generatedAt = new Date().toISOString();
        reportResponse.expiresAt = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString();
        reportResponse.downloadUrl = `/api/reports/${reportResponse.id}/download`;
        reportResponse.filename = `${request.title.toLowerCase().replace(/\s+/g, '-')}-${new Date().toISOString().split('T')[0]}.${request.format}`;
        reportResponse.fileSize = 1024 * 250;

        if (typeof window !== 'undefined') {
          const blob = new Blob(['Mock generated report data'], { 
            type: request.format === 'pdf' ? 'application/pdf' : 'text/csv' 
          });
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = reportResponse.filename;
          document.body.appendChild(a);
          a.click();
          document.body.removeChild(a);
          URL.revokeObjectURL(url);
        }
      }, 2000);

      reset();
      return reportResponse;
    } catch (error) {
      setError(handleApiError(error as ApiError));
      throw error;
    }
  }, [setLoading, setError, reset]);

  return {
    generateReport,
    loading: state.loading,
    error: state.error,
  };
}

// =====================================
// Enrollment and Intake hooks
// =====================================

export function useClientEnrollments(clientId: string | null) {
  const [state, { setData, setLoading, setError }] = useApiState<EnrollmentSummary[]>([]);
  const [hasError, setHasError] = useState(false);

  const fetchEnrollments = useCallback(async () => {
    if (!clientId) return;
    
    setLoading(true);
    setHasError(false);
    try {
      const enrollments = await apiClient.getClientEnrollments(clientId);
      setData(enrollments);
    } catch (error) {
      setError(handleApiError(error as ApiError));
      setHasError(true);
    }
  }, [clientId, setData, setLoading, setError]);

  useEffect(() => {
    if (!hasError) {
      fetchEnrollments();
    }
  }, [fetchEnrollments, hasError]);

  return {
    enrollments: state.data,
    loading: state.loading,
    error: state.error,
    refetch: fetchEnrollments,
  };
}

export function useIntakeAssessment(enrollmentId: string | null) {
  const [state, { setData, setLoading, setError }] = useApiState<IntakeAssessment>();
  const [hasError, setHasError] = useState(false);

  const fetchIntakeAssessment = useCallback(async () => {
    if (!enrollmentId) return;
    
    setLoading(true);
    setHasError(false);
    try {
      const assessment = await apiClient.getIntakeAssessment(enrollmentId);
      setData(assessment);
    } catch (error) {
      setError(handleApiError(error as ApiError));
      setHasError(true);
    }
  }, [enrollmentId, setData, setLoading, setError]);

  useEffect(() => {
    if (!hasError) {
      fetchIntakeAssessment();
    }
  }, [fetchIntakeAssessment, hasError]);

  const updateField = useCallback(async (fieldId: string, value: any) => {
    if (!enrollmentId) return;
    
    try {
      await apiClient.updateIntakeField(enrollmentId, { fieldId, value });
      // Refetch assessment after updating
      await fetchIntakeAssessment();
    } catch (error) {
      setError(handleApiError(error as ApiError));
      throw error;
    }
  }, [enrollmentId, fetchIntakeAssessment, setError]);

  const completeSection = useCallback(async (sectionId: string) => {
    if (!enrollmentId) return;
    
    try {
      await apiClient.completeIntakeSection(enrollmentId, sectionId);
      // Refetch assessment after completing section
      await fetchIntakeAssessment();
    } catch (error) {
      setError(handleApiError(error as ApiError));
      throw error;
    }
  }, [enrollmentId, fetchIntakeAssessment, setError]);

  const submitAssessment = useCallback(async () => {
    if (!enrollmentId) return;
    
    try {
      await apiClient.submitIntakeAssessment(enrollmentId);
      // Refetch assessment after submission
      await fetchIntakeAssessment();
    } catch (error) {
      setError(handleApiError(error as ApiError));
      throw error;
    }
  }, [enrollmentId, fetchIntakeAssessment, setError]);

  return {
    assessment: state.data,
    loading: state.loading,
    error: state.error,
    refetch: fetchIntakeAssessment,
    updateField,
    completeSection,
    submitAssessment,
  };
}

// =====================================
// Financial Assistance hooks
// =====================================

export function useAssistanceRequests(clientId: string | null) {
  const [state, { setData, setLoading, setError }] = useApiState<FinancialAssistanceRequest[]>([]);
  const [hasError, setHasError] = useState(false);

  const fetchRequests = useCallback(async () => {
    if (!clientId) return;
    
    setLoading(true);
    setHasError(false);
    try {
      const requests = await apiClient.getAssistanceRequests(clientId);
      setData(requests);
    } catch (error) {
      setError(handleApiError(error as ApiError));
      setHasError(true);
    }
  }, [clientId, setData, setLoading, setError]);

  useEffect(() => {
    if (!hasError) {
      fetchRequests();
    }
  }, [fetchRequests, hasError]);

  return {
    requests: state.data,
    loading: state.loading,
    error: state.error,
    refetch: fetchRequests,
  };
}

export function useAssistanceSummary(clientId: string | null, enrollmentId: string | null) {
  const [state, { setData, setLoading, setError }] = useApiState<AssistanceSummary>();
  const [hasError, setHasError] = useState(false);

  const fetchSummary = useCallback(async () => {
    if (!clientId || !enrollmentId) return;
    
    setLoading(true);
    setHasError(false);
    try {
      const summary = await apiClient.getAssistanceSummary(clientId, enrollmentId);
      setData(summary);
    } catch (error) {
      setError(handleApiError(error as ApiError));
      setHasError(true);
    }
  }, [clientId, enrollmentId, setData, setLoading, setError]);

  useEffect(() => {
    if (!hasError) {
      fetchSummary();
    }
  }, [fetchSummary, hasError]);

  return {
    summary: state.data,
    loading: state.loading,
    error: state.error,
    refetch: fetchSummary,
  };
}

export function useAssistanceLedger(clientId: string | null, enrollmentId?: string | null) {
  const [state, { setData, setLoading, setError }] = useApiState<AssistanceLedgerEntry[]>([]);
  const [hasError, setHasError] = useState(false);

  const fetchLedger = useCallback(async () => {
    if (!clientId) return;
    
    setLoading(true);
    setHasError(false);
    try {
      const ledger = await apiClient.getAssistanceLedger(clientId, enrollmentId || undefined);
      setData(ledger);
    } catch (error) {
      setError(handleApiError(error as ApiError));
      setHasError(true);
    }
  }, [clientId, enrollmentId, setData, setLoading, setError]);

  useEffect(() => {
    if (!hasError) {
      fetchLedger();
    }
  }, [fetchLedger, hasError]);

  return {
    ledger: state.data,
    loading: state.loading,
    error: state.error,
    refetch: fetchLedger,
  };
}

export function useCreateAssistanceRequest() {
  const [state, { setLoading, setError, reset }] = useApiState();

  const createRequest = useCallback(async (requestData: CreateAssistanceRequestRequest) => {
    setLoading(true);
    try {
      const result = await apiClient.createAssistanceRequest(requestData);
      reset();
      return result;
    } catch (error) {
      setError(handleApiError(error as ApiError));
      throw error;
    }
  }, [setLoading, setError, reset]);

  const submitRequest = useCallback(async (requestId: string) => {
    setLoading(true);
    try {
      await apiClient.submitAssistanceRequest(requestId);
      reset();
    } catch (error) {
      setError(handleApiError(error as ApiError));
      throw error;
    }
  }, [setLoading, setError, reset]);

  return {
    createRequest,
    submitRequest,
    loading: state.loading,
    error: state.error,
  };
}

export function usePayees() {
  const [state, { setData, setLoading, setError }] = useApiState<Payee[]>([]);

  useEffect(() => {
    const fetchPayees = async () => {
      setLoading(true);
      try {
        const payees = await apiClient.getPayees();
        setData(payees);
      } catch (error) {
        setError(handleApiError(error as ApiError));
      }
    };
    
    fetchPayees();
  }, [setData, setLoading, setError]);

  return {
    payees: state.data,
    loading: state.loading,
    error: state.error,
  };
}

// Household Composition Hooks

export function useHouseholdComposition(householdId: string) {
  const [state, { setData, setLoading, setError }] = useApiState<HouseholdComposition>(null);

  const fetchHousehold = useCallback(async () => {
    if (!householdId) return;
    
    setLoading(true);
    try {
      const household = await apiClient.getHouseholdComposition(householdId);
      setData(household);
    } catch (error) {
      setError(handleApiError(error as ApiError));
    }
  }, [householdId, setData, setLoading, setError]);

  useEffect(() => {
    fetchHousehold();
  }, [fetchHousehold]);

  return {
    household: state.data,
    loading: state.loading,
    error: state.error,
    refetch: fetchHousehold,
  };
}

export function useActiveHouseholdForClient(clientId: string, asOfDate?: string) {
  const [state, { setData, setLoading, setError }] = useApiState<HouseholdComposition>(null);

  const fetchActiveHousehold = useCallback(async () => {
    if (!clientId) return;
    
    setLoading(true);
    try {
      const household = await apiClient.getActiveHouseholdForClient(clientId, asOfDate);
      setData(household);
    } catch (error) {
      setError(handleApiError(error as ApiError));
    }
  }, [clientId, asOfDate, setData, setLoading, setError]);

  useEffect(() => {
    fetchActiveHousehold();
  }, [fetchActiveHousehold]);

  return {
    household: state.data,
    loading: state.loading,
    error: state.error,
    refetch: fetchActiveHousehold,
  };
}

export function useHouseholdMembers(householdId: string, asOfDate?: string) {
  const [state, { setData, setLoading, setError }] = useApiState<HouseholdMember[]>([]);

  const fetchMembers = useCallback(async () => {
    if (!householdId) return;
    
    setLoading(true);
    try {
      const members = await apiClient.getHouseholdMembers(householdId, asOfDate);
      setData(members);
    } catch (error) {
      setError(handleApiError(error as ApiError));
    }
  }, [householdId, asOfDate, setData, setLoading, setError]);

  useEffect(() => {
    fetchMembers();
  }, [fetchMembers]);

  return {
    members: state.data,
    loading: state.loading,
    error: state.error,
    refetch: fetchMembers,
  };
}

export function useHouseholdManagement() {
  const [state, { setLoading, setError }] = useApiState<null>(null);

  const createHousehold = useCallback(async (request: CreateHouseholdCompositionRequest) => {
    setLoading(true);
    try {
      const result = await apiClient.createHouseholdComposition(request);
      return result;
    } catch (error) {
      setError(handleApiError(error as ApiError));
      throw error;
    } finally {
      setLoading(false);
    }
  }, [setLoading, setError]);

  const addMember = useCallback(async (householdId: string, request: AddHouseholdMemberRequest) => {
    setLoading(true);
    try {
      await apiClient.addHouseholdMember(householdId, request);
    } catch (error) {
      setError(handleApiError(error as ApiError));
      throw error;
    } finally {
      setLoading(false);
    }
  }, [setLoading, setError]);

  const removeMember = useCallback(async (
    householdId: string, 
    memberId: string, 
    request: { effectiveDate: string; recordedBy: string; reason: string }
  ) => {
    setLoading(true);
    try {
      await apiClient.removeHouseholdMember(householdId, memberId, request);
    } catch (error) {
      setError(handleApiError(error as ApiError));
      throw error;
    } finally {
      setLoading(false);
    }
  }, [setLoading, setError]);

  return {
    createHousehold,
    addMember,
    removeMember,
    loading: state.loading,
    error: state.error,
  };
}

// Consent Ledger Hooks
export function useClientConsents(clientId: string, activeOnly: boolean = false) {
  const [state, { setLoading, setSuccess, setError }] = useApiState<ConsentLedgerEntry[]>([]);

  const fetchConsents = useCallback(async () => {
    if (!clientId) return;
    
    setLoading(true);
    try {
      const response = await fetch(`/api/consent-ledger/client/${clientId}?activeOnly=${activeOnly}`);
      if (!response.ok) {
        throw new Error(`Failed to fetch consents: ${response.statusText}`);
      }
      const consents = await response.json();
      setSuccess(consents);
    } catch (error) {
      setError(handleApiError(error as ApiError));
    } finally {
      setLoading(false);
    }
  }, [clientId, activeOnly, setLoading, setSuccess, setError]);

  useEffect(() => {
    fetchConsents();
  }, [fetchConsents]);

  return {
    consents: state.data || [],
    loading: state.loading,
    error: state.error,
    refresh: fetchConsents,
  };
}

export function useConsentSearch() {
  const [state, { setLoading, setSuccess, setError }] = useApiState<{
    data: ConsentLedgerEntry[];
    pagination: any;
  }>({ data: [], pagination: null });

  const searchConsents = useCallback(async (params: ConsentSearchParams & { page?: number; size?: number }) => {
    setLoading(true);
    try {
      const searchParams = new URLSearchParams();
      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          searchParams.append(key, value.toString());
        }
      });

      const response = await fetch(`/api/consent-ledger/search?${searchParams}`);
      if (!response.ok) {
        throw new Error(`Failed to search consents: ${response.statusText}`);
      }
      const results = await response.json();
      setSuccess(results);
    } catch (error) {
      setError(handleApiError(error as ApiError));
    } finally {
      setLoading(false);
    }
  }, [setLoading, setSuccess, setError]);

  return {
    results: state.data?.data || [],
    pagination: state.data?.pagination,
    loading: state.loading,
    error: state.error,
    search: searchConsents,
  };
}

export function useConsentAuditTrail(consentId: string) {
  const [state, { setLoading, setSuccess, setError }] = useApiState<ConsentAuditEntry[]>([]);

  const fetchAuditTrail = useCallback(async () => {
    if (!consentId) return;
    
    setLoading(true);
    try {
      const response = await fetch(`/api/consent-ledger/${consentId}/audit-trail`);
      if (!response.ok) {
        throw new Error(`Failed to fetch audit trail: ${response.statusText}`);
      }
      const auditTrail = await response.json();
      setSuccess(auditTrail);
    } catch (error) {
      setError(handleApiError(error as ApiError));
    } finally {
      setLoading(false);
    }
  }, [consentId, setLoading, setSuccess, setError]);

  useEffect(() => {
    fetchAuditTrail();
  }, [fetchAuditTrail]);

  return {
    auditTrail: state.data || [],
    loading: state.loading,
    error: state.error,
    refresh: fetchAuditTrail,
  };
}

export function useConsentStatistics() {
  const [state, { setLoading, setSuccess, setError }] = useApiState<ConsentStatistics | null>(null);

  const fetchStatistics = useCallback(async () => {
    setLoading(true);
    try {
      const response = await fetch('/api/consent-ledger/statistics');
      if (!response.ok) {
        throw new Error(`Failed to fetch statistics: ${response.statusText}`);
      }
      const statistics = await response.json();
      setSuccess(statistics);
    } catch (error) {
      setError(handleApiError(error as ApiError));
    } finally {
      setLoading(false);
    }
  }, [setLoading, setSuccess, setError]);

  useEffect(() => {
    fetchStatistics();
  }, [fetchStatistics]);

  return {
    statistics: state.data,
    loading: state.loading,
    error: state.error,
    refresh: fetchStatistics,
  };
}

// Coordinated Entry Hooks

export function useCeAssessments(enrollmentId: string | null) {
  const [state, { setData, setLoading, setError }] = useApiState<CeAssessment[]>([]);

  const fetchAssessments = useCallback(async () => {
    if (!enrollmentId) return;
    setLoading(true);
    try {
      const assessments = await apiClient.getCeAssessments(enrollmentId);
      setData(assessments);
    } catch (error) {
      setError(handleApiError(error as ApiError));
    }
  }, [enrollmentId, setData, setLoading, setError]);

  useEffect(() => {
    fetchAssessments();
  }, [fetchAssessments]);

  return {
    assessments: state.data || [],
    loading: state.loading,
    error: state.error,
    refresh: fetchAssessments,
  };
}

export function useCreateCeAssessment(enrollmentId: string | null) {
  const [state, { setLoading, setError }] = useApiState<null>(null);

  const createAssessment = useCallback(async (payload: CreateCeAssessmentRequest) => {
    if (!enrollmentId) {
      throw new Error('Enrollment ID is required to create an assessment');
    }

    setLoading(true);
    try {
      return await apiClient.createCeAssessment(enrollmentId, payload);
    } catch (error) {
      setError(handleApiError(error as ApiError));
      throw error;
    } finally {
      setLoading(false);
    }
  }, [enrollmentId, setLoading, setError]);

  return {
    createAssessment,
    loading: state.loading,
    error: state.error,
  };
}

export function useCeEvents(enrollmentId: string | null) {
  const [state, { setData, setLoading, setError }] = useApiState<CeEvent[]>([]);

  const fetchEvents = useCallback(async () => {
    if (!enrollmentId) return;
    setLoading(true);
    try {
      const events = await apiClient.getCeEvents(enrollmentId);
      setData(events);
    } catch (error) {
      setError(handleApiError(error as ApiError));
    }
  }, [enrollmentId, setData, setLoading, setError]);

  useEffect(() => {
    fetchEvents();
  }, [fetchEvents]);

  return {
    events: state.data || [],
    loading: state.loading,
    error: state.error,
    refresh: fetchEvents,
  };
}

export function useCreateCeEvent(enrollmentId: string | null) {
  const [state, { setLoading, setError }] = useApiState<null>(null);

  const createEvent = useCallback(async (payload: CreateCeEventRequest) => {
    if (!enrollmentId) {
      throw new Error('Enrollment ID is required to record an event');
    }

    setLoading(true);
    try {
      return await apiClient.createCeEvent(enrollmentId, payload);
    } catch (error) {
      setError(handleApiError(error as ApiError));
      throw error;
    } finally {
      setLoading(false);
    }
  }, [enrollmentId, setLoading, setError]);

  return {
    createEvent,
    loading: state.loading,
    error: state.error,
  };
}

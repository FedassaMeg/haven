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

// Utility hooks
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
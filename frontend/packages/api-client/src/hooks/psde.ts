import { useState, useEffect, useCallback } from 'react';
import { apiClient, handleApiError } from '../client';
import type {
  IntakePsdeRequest,
  IntakePsdeResponse,
  ApiError
} from '../types';

/**
 * Custom hook for managing Intake PSDE records
 * Provides CRUD operations with role-based access control
 */
export function useIntakePsde(enrollmentId?: string) {
  const [psdeData, setPsdeData] = useState<IntakePsdeResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Fetch PSDE records for an enrollment
  const fetchPsdeRecords = useCallback(async (enrollmentId: string) => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.get(`/api/enrollments/${enrollmentId}/intake-psde`);
      setPsdeData(response.data);
    } catch (err) {
      const errorMessage = handleApiError(err);
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  }, []);

  // Create new PSDE record
  const createPsdeRecord = useCallback(async (
    enrollmentId: string,
    data: IntakePsdeRequest
  ): Promise<IntakePsdeResponse> => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.post(
        `/api/enrollments/${enrollmentId}/intake-psde`,
        data
      );

      // Update local state
      setPsdeData(prev => [...prev, response.data]);
      return response.data;
    } catch (err) {
      const errorMessage = handleApiError(err);
      setError(errorMessage);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  // Update existing PSDE record
  const updatePsdeRecord = useCallback(async (
    enrollmentId: string,
    recordId: string,
    data: Partial<IntakePsdeRequest>
  ): Promise<IntakePsdeResponse> => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.put(
        `/api/enrollments/${enrollmentId}/intake-psde/${recordId}`,
        data
      );

      // Update local state
      setPsdeData(prev =>
        prev.map(record =>
          record.recordId === recordId ? response.data : record
        )
      );
      return response.data;
    } catch (err) {
      const errorMessage = handleApiError(err);
      setError(errorMessage);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  // Get single PSDE record
  const getPsdeRecord = useCallback(async (
    enrollmentId: string,
    recordId: string
  ): Promise<IntakePsdeResponse> => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.get(
        `/api/enrollments/${enrollmentId}/intake-psde/${recordId}`
      );
      return response.data;
    } catch (err) {
      const errorMessage = handleApiError(err);
      setError(errorMessage);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  // Validate PSDE record
  const validatePsdeRecord = useCallback(async (
    enrollmentId: string,
    data: IntakePsdeRequest
  ) => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.post(
        `/api/enrollments/${enrollmentId}/intake-psde/validate`,
        data
      );
      return response.data;
    } catch (err) {
      const errorMessage = handleApiError(err);
      setError(errorMessage);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  // Update VAWA confidentiality status
  const updateVawaConfidentiality = useCallback(async (
    enrollmentId: string,
    recordId: string,
    confidentialityRequested: boolean,
    reason: string
  ) => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.post(
        `/api/enrollments/${enrollmentId}/intake-psde/${recordId}/vawa-confidentiality`,
        { confidentialityRequested, reason }
      );

      // Update local state
      setPsdeData(prev =>
        prev.map(record =>
          record.recordId === recordId ? response.data : record
        )
      );
      return response.data;
    } catch (err) {
      const errorMessage = handleApiError(err);
      setError(errorMessage);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  // Fetch data on enrollment change
  useEffect(() => {
    if (enrollmentId) {
      fetchPsdeRecords(enrollmentId);
    }
  }, [enrollmentId, fetchPsdeRecords]);

  return {
    psdeData,
    loading,
    error,
    fetchPsdeRecords,
    createPsdeRecord,
    updatePsdeRecord,
    getPsdeRecord,
    validatePsdeRecord,
    updateVawaConfidentiality,
    refetch: () => enrollmentId && fetchPsdeRecords(enrollmentId)
  };
}

/**
 * Hook for managing PSDE data quality and compliance
 */
export function usePsdeDataQuality(enrollmentId?: string) {
  const [dataQuality, setDataQuality] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchDataQuality = useCallback(async (enrollmentId: string) => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.get(
        `/api/enrollments/${enrollmentId}/intake-psde/data-quality`
      );
      setDataQuality(response.data);
    } catch (err) {
      const errorMessage = handleApiError(err);
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (enrollmentId) {
      fetchDataQuality(enrollmentId);
    }
  }, [enrollmentId, fetchDataQuality]);

  return {
    dataQuality,
    loading,
    error,
    refetch: () => enrollmentId && fetchDataQuality(enrollmentId)
  };
}

/**
 * Hook for PSDE audit logging and access tracking
 */
export function usePsdeAuditLog(filters?: any) {
  const [auditLogs, setAuditLogs] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchAuditLogs = useCallback(async (filters: any = {}) => {
    setLoading(true);
    setError(null);
    try {
      const queryParams = new URLSearchParams(filters).toString();
      const response = await apiClient.get(
        `/api/intake-psde/audit-logs${queryParams ? `?${queryParams}` : ''}`
      );
      setAuditLogs(response.data);
    } catch (err) {
      const errorMessage = handleApiError(err);
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAuditLogs(filters);
  }, [filters, fetchAuditLogs]);

  return {
    auditLogs,
    loading,
    error,
    refetch: () => fetchAuditLogs(filters)
  };
}

/**
 * Hook for role-based PSDE access control
 */
export function usePsdeAccessControl() {
  const [userRoles, setUserRoles] = useState<string[]>([]);
  const [accessMatrix, setAccessMatrix] = useState<any>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const fetchUserRoles = async () => {
      setLoading(true);
      try {
        const response = await apiClient.get('/api/auth/user-roles');
        setUserRoles(response.data.roles);

        const accessResponse = await apiClient.get('/api/intake-psde/access-matrix');
        setAccessMatrix(accessResponse.data);
      } catch (err) {
        console.error('Failed to fetch user roles or access matrix:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchUserRoles();
  }, []);

  const canAccessDvData = useCallback(() => {
    return userRoles.some(role =>
      ['DV_SPECIALIST', 'ADMIN', 'CASE_MANAGER', 'SAFETY_COORDINATOR'].includes(role)
    );
  }, [userRoles]);

  const canAccessSensitiveData = useCallback(() => {
    return userRoles.some(role =>
      ['DV_SPECIALIST', 'ADMIN'].includes(role)
    );
  }, [userRoles]);

  const canCreatePsdeRecord = useCallback(() => {
    return userRoles.some(role =>
      ['DV_SPECIALIST', 'ADMIN', 'CASE_MANAGER', 'DATA_ENTRY_SPECIALIST'].includes(role)
    );
  }, [userRoles]);

  const canModifyRedactionLevel = useCallback(() => {
    return userRoles.some(role =>
      ['DV_SPECIALIST', 'ADMIN'].includes(role)
    );
  }, [userRoles]);

  const hasAdministrativeOverride = useCallback(() => {
    return userRoles.some(role =>
      ['ADMIN', 'SYSTEM_ADMINISTRATOR'].includes(role)
    );
  }, [userRoles]);

  return {
    userRoles,
    accessMatrix,
    loading,
    canAccessDvData,
    canAccessSensitiveData,
    canCreatePsdeRecord,
    canModifyRedactionLevel,
    hasAdministrativeOverride
  };
}

/**
 * Hook for PSDE reporting and analytics
 */
export function usePsdeReporting() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const generateComplianceReport = useCallback(async (params: any) => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.post('/api/intake-psde/reports/compliance', params);
      return response.data;
    } catch (err) {
      const errorMessage = handleApiError(err);
      setError(errorMessage);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const generateDataQualityReport = useCallback(async (params: any) => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.post('/api/intake-psde/reports/data-quality', params);
      return response.data;
    } catch (err) {
      const errorMessage = handleApiError(err);
      setError(errorMessage);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const exportPsdeData = useCallback(async (params: any) => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.post('/api/intake-psde/export', params, {
        responseType: 'blob'
      });
      return response.data;
    } catch (err) {
      const errorMessage = handleApiError(err);
      setError(errorMessage);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  return {
    loading,
    error,
    generateComplianceReport,
    generateDataQualityReport,
    exportPsdeData
  };
}
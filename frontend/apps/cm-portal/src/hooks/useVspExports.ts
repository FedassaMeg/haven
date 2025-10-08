import { useState, useEffect, useCallback } from 'react';
import { useApi } from '../../../packages/api-client/src/hooks';
import { ApiResponse } from '../../../packages/api-client/src/types';

interface VspExportRequest {
  recipient: string;
  recipientType: string;
  consentBasis: string;
  cocId: string;
  enrollmentIds: string[];
  startDate?: string;
  endDate?: string;
  shareScopes: string[];
  exportFormat: string;
  encryptionKeyId: string;
  expiryDays?: number;
  includeAssessments: boolean;
  includeEvents: boolean;
  includeReferrals: boolean;
  exportReason?: string;
  additionalRedactions?: string[];
}

interface VspExportResponse {
  exportId: string;
  recipient: string;
  ceHashKey: string;
  exportTimestamp: string;
  expiryDate?: string;
  status: string;
  message: string;
}

interface ShareHistoryEntry {
  exportId: string;
  exportTimestamp: string;
  expiryDate?: string;
  status: string;
  shareScopes: string[];
  consentBasis: string;
  ceHashKey: string;
  revokedAt?: string;
  revokedBy?: string;
  revocationReason?: string;
}

interface ShareHistoryResponse {
  recipient: string;
  exports: ShareHistoryEntry[];
  totalExports: number;
  activeExports: number;
  revokedExports: number;
  expiredExports: number;
  firstExportDate?: string;
  lastExportDate?: string;
}

interface RevokeRequest {
  exportId: string;
  reason: string;
}

interface RevokeResponse {
  exportId: string;
  status: string;
  message: string;
  revokedBy: string;
}

/**
 * Hook for creating a VSP export
 */
export const useCreateVspExport = () => {
  const api = useApi();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const createExport = useCallback(async (request: VspExportRequest): Promise<VspExportResponse> => {
    setLoading(true);
    setError(null);

    try {
      const response = await api.post<VspExportResponse>('/api/v1/vsp-exports', request);
      return response.data;
    } catch (err) {
      const error = err as Error;
      setError(error);
      throw error;
    } finally {
      setLoading(false);
    }
  }, [api]);

  return { createExport, loading, error };
};

/**
 * Hook for fetching VSP export history
 */
export const useVspExportHistory = (recipient: string) => {
  const api = useApi();
  const [data, setData] = useState<ShareHistoryResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const fetchHistory = useCallback(async () => {
    if (!recipient) return;

    setLoading(true);
    setError(null);

    try {
      const response = await api.get<ShareHistoryResponse>(
        `/api/v1/vsp-exports/history/${encodeURIComponent(recipient)}`
      );
      setData(response.data);
    } catch (err) {
      const error = err as Error;
      setError(error);
    } finally {
      setLoading(false);
    }
  }, [api, recipient]);

  useEffect(() => {
    fetchHistory();
  }, [fetchHistory]);

  return { data, loading, error, refetch: fetchHistory };
};

/**
 * Hook for revoking a VSP export
 */
export const useRevokeVspExport = () => {
  const api = useApi();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const mutate = useCallback(async ({ exportId, reason }: RevokeRequest): Promise<RevokeResponse> => {
    setLoading(true);
    setError(null);

    try {
      const response = await api.post<RevokeResponse>(
        `/api/v1/vsp-exports/${exportId}/revoke`,
        { reason }
      );
      return response.data;
    } catch (err) {
      const error = err as Error;
      setError(error);
      throw error;
    } finally {
      setLoading(false);
    }
  }, [api]);

  return { mutate, loading, error };
};

/**
 * Hook for processing expired VSP exports
 */
export const useProcessExpiredExports = () => {
  const api = useApi();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const processExpired = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await api.post('/api/v1/vsp-exports/process-expired');
      return response.data;
    } catch (err) {
      const error = err as Error;
      setError(error);
      throw error;
    } finally {
      setLoading(false);
    }
  }, [api]);

  return { processExpired, loading, error };
};

/**
 * Hook for fetching VSP recipient categories
 */
export const useVspRecipientCategories = () => {
  const categories = [
    { value: 'VICTIM_SERVICE_PROVIDER', label: 'Victim Service Provider', vawaCompliant: true },
    { value: 'LEGAL_AID', label: 'Legal Aid', vawaCompliant: true },
    { value: 'LAW_ENFORCEMENT', label: 'Law Enforcement', vawaCompliant: false },
    { value: 'HEALTHCARE_PROVIDER', label: 'Healthcare Provider', vawaCompliant: true },
    { value: 'GOVERNMENT_AGENCY', label: 'Government Agency', vawaCompliant: false },
    { value: 'RESEARCH_INSTITUTION', label: 'Research Institution', vawaCompliant: false },
    { value: 'COC_LEAD', label: 'CoC Lead Agency', vawaCompliant: true },
    { value: 'HMIS_LEAD', label: 'HMIS Lead Agency', vawaCompliant: true },
    { value: 'EMERGENCY_SHELTER', label: 'Emergency Shelter', vawaCompliant: true },
    { value: 'TRANSITIONAL_HOUSING', label: 'Transitional Housing', vawaCompliant: true },
    { value: 'INTERNAL_USE', label: 'Internal Use', vawaCompliant: true },
    { value: 'CLIENT_REQUEST', label: 'Client Request', vawaCompliant: true },
  ];

  return categories;
};

/**
 * Hook for VSP export statistics
 */
export const useVspExportStatistics = () => {
  const api = useApi();
  const [data, setData] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const fetchStatistics = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await api.get('/api/v1/vsp-exports/statistics');
      setData(response.data);
    } catch (err) {
      const error = err as Error;
      setError(error);
    } finally {
      setLoading(false);
    }
  }, [api]);

  useEffect(() => {
    fetchStatistics();
  }, [fetchStatistics]);

  return { data, loading, error, refetch: fetchStatistics };
};
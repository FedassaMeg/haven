/**
 * API client for HUD HMIS Export operations
 */

const BASE_URL = '/api/exports';

export type ReportType = 'CoC_APR' | 'ESG_CAPER' | 'SPM' | 'PIT' | 'HIC';

export type ExportJobState = 'QUEUED' | 'MATERIALIZING' | 'VALIDATING' | 'COMPLETE' | 'FAILED';

export interface ExportConfiguration {
  reportType: ReportType;
  operatingYearStart: string;
  operatingYearEnd: string;
  projectIds: string[];
  includeAggregateOnly: boolean;
  exportNotes?: string;
}

export interface CreateExportConfigurationResponse {
  exportJobId: string;
  state: ExportJobState;
  messages: string[];
}

export interface ConsentWarning {
  clientId: string;
  clientInitials: string;
  warningType: 'MISSING_CONSENT' | 'CONSENT_REVOKED' | 'CONSENT_EXPIRED';
  consentStatus: string;
  consentDate?: string;
  consentExpiryDate?: string;
  blocksIndividualData: boolean;
  requiresAggregateOnlyMode: boolean;
  affectedDataElements: string;
  recommendation: string;
}

export interface ConsentWarningsResponse {
  warnings: ConsentWarning[];
  hasBlockingIssues: boolean;
  requiresAggregateMode: boolean;
  summary: string;
}

export interface ExportJobStatus {
  exportJobId: string;
  state: ExportJobState;
  exportType: ReportType;
  reportingPeriodStart: string;
  reportingPeriodEnd: string;
  queuedAt: string;
  startedAt?: string;
  completedAt?: string;
  errorMessage?: string;
  downloadUrl?: string;
  sha256Hash?: string;
  totalRecords?: number;
  vawaSupressedRecords?: number;
}

export interface ExportJobSummary {
  exportJobId: string;
  state: ExportJobState;
  exportType: ReportType;
  reportingPeriodStart: string;
  reportingPeriodEnd: string;
  queuedAt: string;
  completedAt?: string;
  projectCount: number;
}

export interface EligibleProject {
  projectId: string;
  projectName: string;
  projectType: string;
  hudProjectId: string;
  userHasAccess: boolean;
  accessReason: string;
}

export const exportApi = {
  /**
   * Create a new export configuration
   */
  async createExportConfiguration(config: ExportConfiguration): Promise<CreateExportConfigurationResponse> {
    const response = await fetch(`${BASE_URL}/configurations`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(config),
    });
    if (!response.ok) {
      throw new Error(`Failed to create export configuration: ${response.statusText}`);
    }
    return response.json();
  },

  /**
   * Get VAWA consent warnings for export
   */
  async getConsentWarnings(exportJobId: string): Promise<ConsentWarningsResponse> {
    const response = await fetch(`${BASE_URL}/${exportJobId}/consent-warnings`);
    if (!response.ok) {
      throw new Error(`Failed to fetch consent warnings: ${response.statusText}`);
    }
    return response.json();
  },

  /**
   * Get export job status
   */
  async getExportJobStatus(exportJobId: string): Promise<ExportJobStatus> {
    const response = await fetch(`${BASE_URL}/${exportJobId}/status`);
    if (!response.ok) {
      throw new Error(`Failed to fetch export job status: ${response.statusText}`);
    }
    return response.json();
  },

  /**
   * Get all export jobs for current user
   */
  async getAllExportJobs(state?: ExportJobState): Promise<ExportJobSummary[]> {
    const params = new URLSearchParams();
    if (state) params.append('state', state);

    const response = await fetch(`${BASE_URL}?${params.toString()}`);
    if (!response.ok) {
      throw new Error(`Failed to fetch export jobs: ${response.statusText}`);
    }
    return response.json();
  },

  /**
   * Cancel export job
   */
  async cancelExportJob(exportJobId: string): Promise<void> {
    const response = await fetch(`${BASE_URL}/${exportJobId}/cancel`, {
      method: 'POST',
    });
    if (!response.ok) {
      throw new Error(`Failed to cancel export job: ${response.statusText}`);
    }
  },

  /**
   * Retry failed export job
   */
  async retryExportJob(exportJobId: string): Promise<void> {
    const response = await fetch(`${BASE_URL}/${exportJobId}/retry`, {
      method: 'POST',
    });
    if (!response.ok) {
      throw new Error(`Failed to retry export job: ${response.statusText}`);
    }
  },

  /**
   * Download export artifact
   */
  async downloadExport(exportJobId: string): Promise<string> {
    const response = await fetch(`${BASE_URL}/${exportJobId}/download`);
    if (!response.ok) {
      throw new Error(`Failed to download export: ${response.statusText}`);
    }
    // Returns redirect URL
    return response.url;
  },

  /**
   * Get eligible projects for export
   */
  async getEligibleProjects(reportType?: ReportType): Promise<EligibleProject[]> {
    const params = new URLSearchParams();
    if (reportType) params.append('reportType', reportType);

    const response = await fetch(`${BASE_URL}/eligible-projects?${params.toString()}`);
    if (!response.ok) {
      throw new Error(`Failed to fetch eligible projects: ${response.statusText}`);
    }
    return response.json();
  },

  /**
   * Poll export job status until completion
   */
  async pollExportJobStatus(
    exportJobId: string,
    onProgress: (status: ExportJobStatus) => void,
    intervalMs: number = 2000,
    maxAttempts: number = 150
  ): Promise<ExportJobStatus> {
    let attempts = 0;

    return new Promise((resolve, reject) => {
      const poll = async () => {
        try {
          const status = await exportApi.getExportJobStatus(exportJobId);
          onProgress(status);

          if (status.state === 'COMPLETE' || status.state === 'FAILED') {
            resolve(status);
            return;
          }

          attempts++;
          if (attempts >= maxAttempts) {
            reject(new Error('Export job polling timeout'));
            return;
          }

          setTimeout(poll, intervalMs);
        } catch (error) {
          reject(error);
        }
      };

      poll();
    });
  },
};

/**
 * API client for TH/RRH Project Linkage operations
 */

const BASE_URL = '/api/v1/project-linkages';

export interface ProjectLinkage {
  linkageId: string;
  thProjectId: string;
  rrhProjectId: string;
  thHudProjectId: string;
  rrhHudProjectId: string;
  thProjectName: string;
  rrhProjectName: string;
  linkageEffectiveDate: string;
  linkageEndDate?: string;
  status: 'ACTIVE' | 'REVOKED' | 'EXPIRED';
  linkageReason: string;
  linkageNotes?: string;
  createdBy: string;
  lastModifiedBy: string;
  createdAt: string;
  lastModifiedAt: string;
}

export interface CreateLinkageRequest {
  thProjectId: string;
  rrhProjectId: string;
  linkageEffectiveDate: string;
  linkageReason: string;
  linkageNotes?: string;
}

export interface ModifyLinkageRequest {
  linkageEffectiveDate?: string;
  linkageReason?: string;
  linkageNotes?: string;
}

export interface RevokeLinkageRequest {
  revocationDate: string;
  revocationReason: string;
}

export const projectLinkageApi = {
  /**
   * Get all project linkages
   */
  async getAllLinkages(): Promise<ProjectLinkage[]> {
    const response = await fetch(BASE_URL);
    if (!response.ok) {
      throw new Error(`Failed to fetch linkages: ${response.statusText}`);
    }
    return response.json();
  },

  /**
   * Get a specific linkage by ID
   */
  async getLinkage(linkageId: string): Promise<ProjectLinkage> {
    const response = await fetch(`${BASE_URL}/${linkageId}`);
    if (!response.ok) {
      throw new Error(`Failed to fetch linkage: ${response.statusText}`);
    }
    return response.json();
  },

  /**
   * Create a new project linkage
   */
  async createLinkage(request: CreateLinkageRequest): Promise<ProjectLinkage> {
    const response = await fetch(BASE_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });
    if (!response.ok) {
      throw new Error(`Failed to create linkage: ${response.statusText}`);
    }
    return response.json();
  },

  /**
   * Modify an existing linkage
   */
  async modifyLinkage(linkageId: string, request: ModifyLinkageRequest): Promise<ProjectLinkage> {
    const response = await fetch(`${BASE_URL}/${linkageId}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });
    if (!response.ok) {
      throw new Error(`Failed to modify linkage: ${response.statusText}`);
    }
    return response.json();
  },

  /**
   * Revoke a project linkage
   */
  async revokeLinkage(linkageId: string, request: RevokeLinkageRequest): Promise<void> {
    const response = await fetch(`${BASE_URL}/${linkageId}/revoke`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });
    if (!response.ok) {
      throw new Error(`Failed to revoke linkage: ${response.statusText}`);
    }
  },

  /**
   * Get linkages by TH project
   */
  async getLinkagesByThProject(thProjectId: string): Promise<ProjectLinkage[]> {
    const response = await fetch(`${BASE_URL}/th-project/${thProjectId}`);
    if (!response.ok) {
      throw new Error(`Failed to fetch linkages: ${response.statusText}`);
    }
    return response.json();
  },

  /**
   * Get linkages by RRH project
   */
  async getLinkagesByRrhProject(rrhProjectId: string): Promise<ProjectLinkage[]> {
    const response = await fetch(`${BASE_URL}/rrh-project/${rrhProjectId}`);
    if (!response.ok) {
      throw new Error(`Failed to fetch linkages: ${response.statusText}`);
    }
    return response.json();
  },
};

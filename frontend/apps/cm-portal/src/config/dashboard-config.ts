import { UserRole } from '@haven/auth';

export interface DashboardWidget {
  id: string;
  type: 'cases' | 'priorities' | 'updates' | 'wellbeing' | 'resources' | 'analytics' | 'system-status' | 'team-management';
  title: string;
  order: number;
  size: 'small' | 'medium' | 'large' | 'full';
  permissions?: string[];
  config?: Record<string, any>;
}

export interface DashboardLayout {
  columns: number;
  sidebar: boolean;
  widgets: DashboardWidget[];
}

export interface RoleDashboardConfig {
  role: UserRole | string;
  layout: DashboardLayout;
  customizable: boolean;
  description: string;
}

// Default dashboard configurations by role
export const DEFAULT_DASHBOARD_CONFIGS: Record<string, RoleDashboardConfig> = {
  [UserRole.CASE_MANAGER]: {
    role: UserRole.CASE_MANAGER,
    description: 'Case Manager focused view with active cases, priorities, and wellbeing support',
    customizable: true,
    layout: {
      columns: 3,
      sidebar: true,
      widgets: [
        {
          id: 'active-cases',
          type: 'cases',
          title: 'My Active Cases',
          order: 1,
          size: 'large',
          config: {
            filter: 'assigned',
            limit: 5,
            showDetails: true
          }
        },
        {
          id: 'priorities',
          type: 'priorities',
          title: 'Today\'s Priorities',
          order: 2,
          size: 'large',
          config: {
            showUrgentFirst: true,
            allowComplete: true
          }
        },
        {
          id: 'recent-updates',
          type: 'updates',
          title: 'Recent Updates',
          order: 3,
          size: 'medium',
          config: {
            filter: 'my-cases',
            limit: 3
          }
        },
        {
          id: 'wellbeing',
          type: 'wellbeing',
          title: 'Wellbeing Check',
          order: 4,
          size: 'medium',
          config: {
            showBreathingExercise: true,
            trackProgress: true
          }
        },
        {
          id: 'resources',
          type: 'resources',
          title: 'Quick Resources',
          order: 5,
          size: 'medium',
          config: {
            showCrisisHotline: true,
            showLegalTemplates: true,
            showPeerSupport: true
          }
        }
      ]
    }
  },
  
  [UserRole.ADMIN]: {
    role: UserRole.ADMIN,
    description: 'Administrator overview with system analytics, team management, and system status',
    customizable: true,
    layout: {
      columns: 4,
      sidebar: true,
      widgets: [
        {
          id: 'system-analytics',
          type: 'analytics',
          title: 'System Analytics',
          order: 1,
          size: 'large',
          permissions: ['system:read'],
          config: {
            showCaseMetrics: true,
            showUserActivity: true,
            showPerformanceStats: true
          }
        },
        {
          id: 'all-cases',
          type: 'cases',
          title: 'All Cases Overview',
          order: 2,
          size: 'large',
          permissions: ['case:read-all'],
          config: {
            filter: 'all',
            limit: 8,
            showAssignments: true,
            showStatus: true
          }
        },
        {
          id: 'team-management',
          type: 'team-management',
          title: 'Team Management',
          order: 3,
          size: 'medium',
          permissions: ['user:manage'],
          config: {
            showActiveUsers: true,
            showWorkload: true,
            allowAssignments: true
          }
        },
        {
          id: 'system-status',
          type: 'system-status',
          title: 'System Status',
          order: 4,
          size: 'medium',
          permissions: ['system:monitor'],
          config: {
            showUptime: true,
            showAlerts: true,
            showBackups: true
          }
        },
        {
          id: 'critical-updates',
          type: 'updates',
          title: 'Critical Updates',
          order: 5,
          size: 'medium',
          config: {
            filter: 'critical',
            limit: 5,
            showSystemAlerts: true
          }
        },
        {
          id: 'admin-resources',
          type: 'resources',
          title: 'Admin Resources',
          order: 6,
          size: 'small',
          permissions: ['system:admin'],
          config: {
            showUserManagement: true,
            showSystemSettings: true,
            showReports: true,
            showBackup: true
          }
        }
      ]
    }
  },

  // Default fallback configuration
  'default': {
    role: 'default',
    description: 'Basic dashboard view with essential features',
    customizable: false,
    layout: {
      columns: 2,
      sidebar: false,
      widgets: [
        {
          id: 'my-cases',
          type: 'cases',
          title: 'My Cases',
          order: 1,
          size: 'large',
          config: {
            filter: 'assigned',
            limit: 3
          }
        },
        {
          id: 'recent-activity',
          type: 'updates',
          title: 'Recent Activity',
          order: 2,
          size: 'large',
          config: {
            limit: 5
          }
        }
      ]
    }
  }
};

export function getDashboardConfig(userRoles: string[]): RoleDashboardConfig {
  // Check for admin role first
  if (userRoles.includes(UserRole.ADMIN)) {
    return DEFAULT_DASHBOARD_CONFIGS[UserRole.ADMIN];
  }
  
  // Check for case manager role
  if (userRoles.includes(UserRole.CASE_MANAGER)) {
    return DEFAULT_DASHBOARD_CONFIGS[UserRole.CASE_MANAGER];
  }
  
  // Check for supervisor role
  if (userRoles.includes(UserRole.SUPERVISOR)) {
    // Supervisors get admin-like view but with different permissions
    return {
      ...DEFAULT_DASHBOARD_CONFIGS[UserRole.ADMIN],
      role: UserRole.SUPERVISOR,
      description: 'Supervisor view with team oversight and case management'
    };
  }
  
  // Return default configuration
  return DEFAULT_DASHBOARD_CONFIGS.default;
}

export function filterWidgetsByPermissions(widgets: DashboardWidget[], userPermissions: string[]): DashboardWidget[] {
  return widgets.filter(widget => {
    if (!widget.permissions || widget.permissions.length === 0) {
      return true;
    }
    
    return widget.permissions.some(permission => userPermissions.includes(permission));
  });
}
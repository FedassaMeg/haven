import React from 'react';
import { Button } from '@haven/ui';
import { DashboardWidget } from '../../../config/dashboard-config';
import { DashboardWidgetContainer } from '../DashboardWidget';

interface ResourcesWidgetProps {
  widget: DashboardWidget;
}

export function ResourcesWidget({ widget }: ResourcesWidgetProps) {
  const config = widget.config || {};

  const resources = [
    {
      key: 'showCrisisHotline',
      title: 'Crisis Hotline',
      subtitle: '24/7 Support'
    },
    {
      key: 'showLegalTemplates',
      title: 'Legal Templates',
      subtitle: 'Forms & Documents'
    },
    {
      key: 'showPeerSupport',
      title: 'Peer Support',
      subtitle: 'Connect with colleagues'
    },
    {
      key: 'showUserManagement',
      title: 'User Management',
      subtitle: 'Manage system users'
    },
    {
      key: 'showSystemSettings',
      title: 'System Settings',
      subtitle: 'Configure system'
    },
    {
      key: 'showReports',
      title: 'Reports',
      subtitle: 'Generate reports'
    },
    {
      key: 'showBackup',
      title: 'Backup & Recovery',
      subtitle: 'Data management'
    }
  ];

  const filteredResources = resources.filter(resource => config[resource.key]);

  return (
    <DashboardWidgetContainer widget={widget}>
      <div className="space-y-3">
        {filteredResources.map((resource) => (
          <Button key={resource.key} variant="ghost" className="w-full justify-start text-left h-auto p-3">
            <div>
              <p className="font-medium">{resource.title}</p>
              <p className="text-xs text-slate-500">{resource.subtitle}</p>
            </div>
          </Button>
        ))}
        {filteredResources.length === 0 && (
          <p className="text-sm text-slate-500 text-center py-4">No resources configured</p>
        )}
      </div>
    </DashboardWidgetContainer>
  );
}
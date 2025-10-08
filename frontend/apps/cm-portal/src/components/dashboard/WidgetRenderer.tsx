import React from 'react';
import { DashboardWidget } from '../../config/dashboard-config';
import {
  CasesWidget,
  AnalyticsWidget,
  TeamManagementWidget,
  SystemStatusWidget,
  PrioritiesWidget,
  UpdatesWidget,
  WellbeingWidget,
  ResourcesWidget
} from './widgets';

interface WidgetRendererProps {
  widget: DashboardWidget;
}

export function WidgetRenderer({ widget }: WidgetRendererProps) {
  switch (widget.type) {
    case 'cases':
      return <CasesWidget widget={widget} />;
    
    case 'analytics':
      return <AnalyticsWidget widget={widget} />;
    
    case 'team-management':
      return <TeamManagementWidget widget={widget} />;
    
    case 'system-status':
      return <SystemStatusWidget widget={widget} />;
    
    case 'priorities':
      return <PrioritiesWidget widget={widget} />;
    
    case 'updates':
      return <UpdatesWidget widget={widget} />;
    
    case 'wellbeing':
      return <WellbeingWidget widget={widget} />;
    
    case 'resources':
      return <ResourcesWidget widget={widget} />;
    
    default:
      return (
        <div className="p-4 border border-slate-200 rounded-lg">
          <div className="text-slate-500 text-center">
            Widget type "{widget.type}" not implemented
          </div>
        </div>
      );
  }
}
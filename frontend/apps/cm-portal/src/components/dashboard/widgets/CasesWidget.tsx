import React from 'react';
import { Badge } from '@haven/ui';
import { useCases } from '@haven/api-client';
import { useCurrentUser } from '@haven/auth';
import { DashboardWidget } from '../../../config/dashboard-config';
import { DashboardWidgetContainer } from '../DashboardWidget';

interface CasesWidgetProps {
  widget: DashboardWidget;
}

export function CasesWidget({ widget }: CasesWidgetProps) {
  const { user } = useCurrentUser();
  const { cases, loading: casesLoading } = useCases({ activeOnly: true });
  const config = widget.config || {};

  const filteredCases = React.useMemo(() => {
    if (!cases) return [];
    
    let filtered = cases;
    
    // Apply role-based filtering
    if (config.filter === 'assigned') {
      filtered = cases.filter(c => c.assignment?.assigneeId === user?.id);
    } else if (config.filter === 'all') {
      // Admin view - show all cases
      filtered = cases.filter(c => c.status !== 'CLOSED');
    } else {
      // Default - show active cases
      filtered = cases.filter(c => c.status !== 'CLOSED');
    }
    
    return filtered.slice(0, config.limit || 5);
  }, [cases, config, user?.id]);

  const getBadgeStyle = (status: string) => {
    switch (status) {
      case 'OPEN':
        return { bg: 'bg-amber-100', text: 'text-amber-800', border: 'border-amber-200', label: 'Follow-up needed' };
      case 'IN_PROGRESS':
        return { bg: 'bg-green-100', text: 'text-green-800', border: 'border-green-200', label: 'On track' };
      default:
        return { bg: 'bg-cyan-100', text: 'text-cyan-800', border: 'border-cyan-200', label: 'New referral' };
    }
  };

  if (casesLoading) {
    return (
      <DashboardWidgetContainer widget={widget}>
        <div className="flex items-center justify-center py-8">
          <div className="text-slate-500">Loading cases...</div>
        </div>
      </DashboardWidgetContainer>
    );
  }

  return (
    <DashboardWidgetContainer widget={widget}>
      <div className="space-y-4">
        {filteredCases.map((case_, index) => {
          const statusBadge = getBadgeStyle(case_.status);
          
          return (
            <div key={case_.id} className="border border-slate-200 rounded-lg p-4 hover:shadow-sm transition-shadow">
              <div className="flex items-start justify-between mb-3">
                <div>
                  <h3 className="font-semibold text-slate-800">Case #2024-{case_.id.slice(0, 4)}</h3>
                  <p className="text-sm text-slate-600">{case_.description}</p>
                </div>
                <Badge variant="secondary" className={`${statusBadge.bg} ${statusBadge.text} ${statusBadge.border}`}>
                  {statusBadge.label}
                </Badge>
              </div>
              
              {config.showDetails && (
                <p className="text-sm text-slate-600 mb-3">
                  {case_.description || `Client ID: ${case_.clientId?.slice(0, 8)}`}
                </p>
              )}
              
              {config.showAssignments && case_.assignment && (
                <div className="text-xs text-slate-500">
                  Assigned to: {case_.assignment.assigneeName || 'Unassigned'}
                </div>
              )}
              
              {config.showStatus && (
                <div className="flex items-center gap-2 text-xs text-slate-500 mt-2">
                  <span>Last updated: {new Date(case_.updatedAt || case_.createdAt).toLocaleString()}</span>
                </div>
              )}
            </div>
          );
        })}
        
        {filteredCases.length === 0 && (
          <p className="text-sm text-slate-500 text-center py-4">
            {config.filter === 'assigned' ? 'No assigned cases' : 'No active cases'}
          </p>
        )}
      </div>
    </DashboardWidgetContainer>
  );
}
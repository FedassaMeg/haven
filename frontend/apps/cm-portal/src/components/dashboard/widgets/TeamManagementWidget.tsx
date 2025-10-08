import React from 'react';
import { Badge, Button } from '@haven/ui';
import { DashboardWidget } from '../../../config/dashboard-config';
import { DashboardWidgetContainer } from '../DashboardWidget';

interface TeamManagementWidgetProps {
  widget: DashboardWidget;
}

// Mock team data - in a real app, this would come from an API
const mockTeamMembers = [
  { id: '1', name: 'Sarah Johnson', role: 'Case Manager', activeCases: 8, status: 'active' },
  { id: '2', name: 'Mike Chen', role: 'Social Worker', activeCases: 12, status: 'active' },
  { id: '3', name: 'Elena Rodriguez', role: 'Case Manager', activeCases: 6, status: 'active' },
  { id: '4', name: 'David Park', role: 'Supervisor', activeCases: 15, status: 'busy' }
];

export function TeamManagementWidget({ widget }: TeamManagementWidgetProps) {
  const config = widget.config || {};

  const getStatusBadge = (status: string, caseCount: number) => {
    if (caseCount > 12) {
      return { bg: 'bg-red-100', text: 'text-red-800', border: 'border-red-200', label: 'High Load' };
    } else if (caseCount > 8) {
      return { bg: 'bg-amber-100', text: 'text-amber-800', border: 'border-amber-200', label: 'Busy' };
    } else {
      return { bg: 'bg-green-100', text: 'text-green-800', border: 'border-green-200', label: 'Available' };
    }
  };

  return (
    <DashboardWidgetContainer widget={widget}>
      <div className="space-y-4">
        {config.showActiveUsers && (
          <div className="mb-4">
            <div className="grid grid-cols-2 gap-4 text-center">
              <div className="p-3 bg-blue-50 rounded-lg">
                <div className="text-xl font-bold text-blue-700">{mockTeamMembers.length}</div>
                <div className="text-sm text-blue-600">Active Staff</div>
              </div>
              <div className="p-3 bg-green-50 rounded-lg">
                <div className="text-xl font-bold text-green-700">
                  {mockTeamMembers.reduce((sum, member) => sum + member.activeCases, 0)}
                </div>
                <div className="text-sm text-green-600">Total Cases</div>
              </div>
            </div>
          </div>
        )}

        <div className="space-y-3">
          {mockTeamMembers.map((member) => {
            const statusBadge = getStatusBadge(member.status, member.activeCases);
            
            return (
              <div key={member.id} className="flex items-center justify-between p-3 border border-slate-200 rounded-lg">
                <div className="flex-1">
                  <h4 className="font-medium text-slate-800">{member.name}</h4>
                  <p className="text-sm text-slate-600">{member.role}</p>
                  {config.showWorkload && (
                    <p className="text-xs text-slate-500">{member.activeCases} active cases</p>
                  )}
                </div>
                
                <div className="flex items-center gap-2">
                  <Badge variant="secondary" className={`${statusBadge.bg} ${statusBadge.text} ${statusBadge.border}`}>
                    {statusBadge.label}
                  </Badge>
                  
                  {config.allowAssignments && (
                    <Button variant="ghost" size="sm" className="text-xs">
                      Assign
                    </Button>
                  )}
                </div>
              </div>
            );
          })}
        </div>

        {config.allowAssignments && (
          <div className="pt-4 border-t border-slate-200">
            <Button variant="outline" size="sm" className="w-full">
              Manage Team Assignments
            </Button>
          </div>
        )}
      </div>
    </DashboardWidgetContainer>
  );
}
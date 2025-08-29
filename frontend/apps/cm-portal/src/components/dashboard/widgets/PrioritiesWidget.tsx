import React from 'react';
import { Button } from '@haven/ui';
import { DashboardWidget } from '../../../config/dashboard-config';
import { DashboardWidgetContainer } from '../DashboardWidget';

interface PrioritiesWidgetProps {
  widget: DashboardWidget;
}

const todaysPriorities = [
  { id: '1', type: 'urgent', title: 'Safety check - Sarah M.', due: '2:00 PM today' },
  { id: '2', type: 'normal', title: 'Complete case notes - Maria L.', due: 'End of day' },
  { id: '3', type: 'normal', title: 'Team meeting preparation', due: 'Tomorrow 9:00 AM' }
];

export function PrioritiesWidget({ widget }: PrioritiesWidgetProps) {
  const config = widget.config || {};

  const sortedPriorities = config.showUrgentFirst 
    ? [...todaysPriorities].sort((a, b) => a.type === 'urgent' ? -1 : 1)
    : todaysPriorities;

  return (
    <DashboardWidgetContainer widget={widget}>
      <div className="space-y-3">
        {sortedPriorities.map((priority) => (
          <div 
            key={priority.id} 
            className={`flex items-center gap-3 p-3 rounded-lg border ${
              priority.type === 'urgent' 
                ? 'bg-amber-50 border-amber-200' 
                : 'border-slate-200'
            }`}
          >
            <div className="flex-1">
              <p className="font-medium text-slate-800">{priority.title}</p>
              <p className="text-sm text-slate-600">Due: {priority.due}</p>
            </div>
            {config.allowComplete && (
              <Button variant="ghost" size="sm" className="text-xs">
                Complete
              </Button>
            )}
          </div>
        ))}
      </div>
    </DashboardWidgetContainer>
  );
}
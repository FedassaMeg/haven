import React from 'react';
import { DashboardWidget } from '../../../config/dashboard-config';
import { DashboardWidgetContainer } from '../DashboardWidget';

interface UpdatesWidgetProps {
  widget: DashboardWidget;
}

const mockUpdates = [
  { id: '1', caseNumber: 'Case #2024-0156', message: 'Client confirmed safe housing placement', time: '2 hours ago', type: 'normal' },
  { id: '2', caseNumber: 'Case #2024-0142', message: 'Legal documents submitted to court', time: '4 hours ago', type: 'normal' },
  { id: '3', caseNumber: 'System Update', message: 'New crisis protocol guidelines available', time: '1 day ago', type: 'system' },
  { id: '4', caseNumber: 'Critical Alert', message: 'Emergency protocol activated', time: '30 min ago', type: 'critical' }
];

export function UpdatesWidget({ widget }: UpdatesWidgetProps) {
  const config = widget.config || {};

  const filteredUpdates = React.useMemo(() => {
    let updates = mockUpdates;
    
    if (config.filter === 'critical') {
      updates = updates.filter(u => u.type === 'critical' || u.type === 'system');
    } else if (config.filter === 'my-cases') {
      updates = updates.filter(u => u.type === 'normal');
    }
    
    return updates.slice(0, config.limit || 3);
  }, [config]);

  return (
    <DashboardWidgetContainer widget={widget}>
      <div className="space-y-4">
        {filteredUpdates.map((update) => (
          <div key={update.id} className="text-sm">
            <p className="font-medium text-slate-800">{update.caseNumber}</p>
            <p className="text-slate-600">{update.message}</p>
            <p className="text-xs text-slate-500 mt-1">{update.time}</p>
          </div>
        ))}
        {filteredUpdates.length === 0 && (
          <p className="text-sm text-slate-500 text-center py-4">No recent updates</p>
        )}
      </div>
    </DashboardWidgetContainer>
  );
}
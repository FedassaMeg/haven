import React from 'react';
import { Badge, Button } from '@haven/ui';
import { DashboardWidget } from '../../../config/dashboard-config';
import { DashboardWidgetContainer } from '../DashboardWidget';

interface SystemStatusWidgetProps {
  widget: DashboardWidget;
}

// Mock system status data - in a real app, this would come from monitoring APIs
const mockSystemStatus = {
  uptime: '99.9%',
  lastBackup: '2 hours ago',
  activeUsers: 24,
  systemAlerts: [
    { id: '1', type: 'warning', message: 'High CPU usage on server 2', time: '5 min ago' },
    { id: '2', type: 'info', message: 'Database backup completed', time: '2 hours ago' }
  ],
  services: [
    { name: 'Database', status: 'healthy', responseTime: '12ms' },
    { name: 'API Gateway', status: 'healthy', responseTime: '8ms' },
    { name: 'File Storage', status: 'degraded', responseTime: '45ms' },
    { name: 'Email Service', status: 'healthy', responseTime: '15ms' }
  ]
};

export function SystemStatusWidget({ widget }: SystemStatusWidgetProps) {
  const config = widget.config || {};

  const getServiceStatusBadge = (status: string) => {
    switch (status) {
      case 'healthy':
        return { bg: 'bg-green-100', text: 'text-green-800', border: 'border-green-200' };
      case 'degraded':
        return { bg: 'bg-amber-100', text: 'text-amber-800', border: 'border-amber-200' };
      case 'down':
        return { bg: 'bg-red-100', text: 'text-red-800', border: 'border-red-200' };
      default:
        return { bg: 'bg-slate-100', text: 'text-slate-800', border: 'border-slate-200' };
    }
  };

  const getAlertBadge = (type: string) => {
    switch (type) {
      case 'error':
        return { bg: 'bg-red-100', text: 'text-red-800', border: 'border-red-200' };
      case 'warning':
        return { bg: 'bg-amber-100', text: 'text-amber-800', border: 'border-amber-200' };
      case 'info':
        return { bg: 'bg-blue-100', text: 'text-blue-800', border: 'border-blue-200' };
      default:
        return { bg: 'bg-slate-100', text: 'text-slate-800', border: 'border-slate-200' };
    }
  };

  return (
    <DashboardWidgetContainer widget={widget}>
      <div className="space-y-4">
        {/* System Overview */}
        <div className="grid grid-cols-2 gap-4">
          {config.showUptime && (
            <div className="text-center p-3 bg-green-50 rounded-lg">
              <div className="text-lg font-bold text-green-700">{mockSystemStatus.uptime}</div>
              <div className="text-sm text-green-600">Uptime</div>
            </div>
          )}
          
          <div className="text-center p-3 bg-blue-50 rounded-lg">
            <div className="text-lg font-bold text-blue-700">{mockSystemStatus.activeUsers}</div>
            <div className="text-sm text-blue-600">Active Users</div>
          </div>
        </div>

        {/* Service Status */}
        <div className="space-y-2">
          <h4 className="font-medium text-slate-700">Services</h4>
          {mockSystemStatus.services.map((service) => {
            const statusBadge = getServiceStatusBadge(service.status);
            return (
              <div key={service.name} className="flex items-center justify-between p-2 border border-slate-200 rounded">
                <div className="flex-1">
                  <span className="text-sm font-medium text-slate-800">{service.name}</span>
                  <span className="text-xs text-slate-500 ml-2">({service.responseTime})</span>
                </div>
                <Badge variant="secondary" className={`text-xs ${statusBadge.bg} ${statusBadge.text} ${statusBadge.border}`}>
                  {service.status}
                </Badge>
              </div>
            );
          })}
        </div>

        {/* Alerts */}
        {config.showAlerts && mockSystemStatus.systemAlerts.length > 0 && (
          <div className="space-y-2">
            <h4 className="font-medium text-slate-700">Recent Alerts</h4>
            {mockSystemStatus.systemAlerts.slice(0, 3).map((alert) => {
              const alertBadge = getAlertBadge(alert.type);
              return (
                <div key={alert.id} className="p-3 border border-slate-200 rounded-lg">
                  <div className="flex items-start gap-2">
                    <Badge variant="secondary" className={`text-xs ${alertBadge.bg} ${alertBadge.text} ${alertBadge.border}`}>
                      {alert.type}
                    </Badge>
                    <div className="flex-1">
                      <p className="text-sm text-slate-800">{alert.message}</p>
                      <p className="text-xs text-slate-500 mt-1">{alert.time}</p>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* Backup Info */}
        {config.showBackups && (
          <div className="p-3 bg-slate-50 rounded-lg">
            <div className="flex items-center justify-between">
              <div>
                <span className="text-sm font-medium text-slate-700">Last Backup</span>
                <p className="text-xs text-slate-600">{mockSystemStatus.lastBackup}</p>
              </div>
              <Button variant="ghost" size="sm" className="text-xs">
                View Logs
              </Button>
            </div>
          </div>
        )}
      </div>
    </DashboardWidgetContainer>
  );
}
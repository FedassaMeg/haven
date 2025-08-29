import React from 'react';
import { Card, Badge } from '@haven/ui';
import { useCases } from '@haven/api-client';
import { DashboardWidget } from '../../../config/dashboard-config';
import { DashboardWidgetContainer } from '../DashboardWidget';

interface AnalyticsWidgetProps {
  widget: DashboardWidget;
}

export function AnalyticsWidget({ widget }: AnalyticsWidgetProps) {
  const { cases, loading } = useCases({ activeOnly: false });
  const config = widget.config || {};

  const analytics = React.useMemo(() => {
    if (!cases) return null;

    const totalCases = cases.length;
    const activeCases = cases.filter(c => c.status !== 'CLOSED').length;
    const openCases = cases.filter(c => c.status === 'OPEN').length;
    const inProgressCases = cases.filter(c => c.status === 'IN_PROGRESS').length;
    const closedThisMonth = cases.filter(c => {
      const closedDate = new Date(c.updatedAt || c.createdAt);
      const thisMonth = new Date();
      return c.status === 'CLOSED' && 
        closedDate.getMonth() === thisMonth.getMonth() && 
        closedDate.getFullYear() === thisMonth.getFullYear();
    }).length;

    return {
      totalCases,
      activeCases,
      openCases,
      inProgressCases,
      closedThisMonth,
      completionRate: totalCases > 0 ? Math.round((closedThisMonth / totalCases) * 100) : 0
    };
  }, [cases]);

  if (loading) {
    return (
      <DashboardWidgetContainer widget={widget}>
        <div className="flex items-center justify-center py-8">
          <div className="text-slate-500">Loading analytics...</div>
        </div>
      </DashboardWidgetContainer>
    );
  }

  if (!analytics) {
    return (
      <DashboardWidgetContainer widget={widget}>
        <div className="text-slate-500 text-center py-4">No data available</div>
      </DashboardWidgetContainer>
    );
  }

  return (
    <DashboardWidgetContainer widget={widget}>
      <div className="space-y-6">
        {/* Case Metrics */}
        {config.showCaseMetrics && (
          <div className="grid grid-cols-2 gap-4">
            <div className="text-center p-4 bg-blue-50 rounded-lg">
              <div className="text-2xl font-bold text-blue-700">{analytics.activeCases}</div>
              <div className="text-sm text-blue-600">Active Cases</div>
            </div>
            <div className="text-center p-4 bg-green-50 rounded-lg">
              <div className="text-2xl font-bold text-green-700">{analytics.closedThisMonth}</div>
              <div className="text-sm text-green-600">Closed This Month</div>
            </div>
          </div>
        )}

        {/* Status Breakdown */}
        <div className="space-y-3">
          <h4 className="font-medium text-slate-700">Case Status Breakdown</h4>
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-sm text-slate-600">Open Cases</span>
              <Badge variant="secondary" className="bg-amber-100 text-amber-800 border-amber-200">
                {analytics.openCases}
              </Badge>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-sm text-slate-600">In Progress</span>
              <Badge variant="secondary" className="bg-green-100 text-green-800 border-green-200">
                {analytics.inProgressCases}
              </Badge>
            </div>
          </div>
        </div>

        {/* Performance Stats */}
        {config.showPerformanceStats && (
          <div className="p-4 bg-slate-50 rounded-lg">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-medium text-slate-700">Monthly Completion Rate</span>
              <span className="text-lg font-bold text-slate-800">{analytics.completionRate}%</span>
            </div>
            <div className="w-full bg-slate-200 rounded-full h-2">
              <div 
                className="bg-blue-600 h-2 rounded-full transition-all duration-300" 
                style={{ width: `${Math.min(analytics.completionRate, 100)}%` }}
              />
            </div>
          </div>
        )}

        {/* User Activity Placeholder */}
        {config.showUserActivity && (
          <div className="space-y-2">
            <h4 className="font-medium text-slate-700">System Activity</h4>
            <div className="text-sm text-slate-600">
              <div className="flex justify-between">
                <span>Active Users Today</span>
                <span className="font-medium">12</span>
              </div>
              <div className="flex justify-between mt-1">
                <span>Cases Updated Today</span>
                <span className="font-medium">{Math.floor(analytics.activeCases * 0.3)}</span>
              </div>
            </div>
          </div>
        )}
      </div>
    </DashboardWidgetContainer>
  );
}
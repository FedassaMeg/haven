import React from 'react';
import { Card, CardHeader, CardTitle, CardContent, Badge, Button } from '@haven/ui';
import { ServiceEpisode } from '@haven/api-client';
import { format, subDays, isAfter, isBefore } from 'date-fns';

interface ServiceDashboardProps {
  serviceEpisodes: ServiceEpisode[];
  clientId: string;
  loading: boolean;
}

export default function ServiceDashboard({ serviceEpisodes, clientId, loading }: ServiceDashboardProps) {
  if (loading) {
    return (
      <Card>
        <CardContent className="p-6">
          <div className="text-center">Loading service dashboard...</div>
        </CardContent>
      </Card>
    );
  }

  // Dashboard calculations
  const now = new Date();
  const thirtyDaysAgo = subDays(now, 30);
  const recentServices = serviceEpisodes.filter(s => isAfter(new Date(s.serviceDate), thirtyDaysAgo));

  const completedServices = serviceEpisodes.filter(s => s.completionStatus === 'COMPLETED');
  const scheduledServices = serviceEpisodes.filter(s => s.completionStatus === 'SCHEDULED');
  const inProgressServices = serviceEpisodes.filter(s => s.completionStatus === 'IN_PROGRESS');
  const servicesNeedingFollowUp = serviceEpisodes.filter(s => s.requiresFollowUp);
  const overdueFollowUps = serviceEpisodes.filter(s =>
    s.followUpDate && isBefore(new Date(s.followUpDate), now)
  );

  // Service type breakdown
  const serviceTypeBreakdown = serviceEpisodes.reduce((acc, service) => {
    acc[service.serviceType] = (acc[service.serviceType] || 0) + 1;
    return acc;
  }, {} as Record<string, number>);

  // Funding source breakdown
  const fundingBreakdown = serviceEpisodes.reduce((acc, service) => {
    const funder = service.primaryFundingSource?.funderName || 'Unknown';
    acc[funder] = (acc[funder] || 0) + 1;
    return acc;
  }, {} as Record<string, number>);

  // Calculate total service hours
  const totalServiceHours = completedServices.reduce((total, service) => {
    return total + (service.actualDurationMinutes || 0);
  }, 0) / 60;

  // Recent activity
  const recentActivity = serviceEpisodes
    .slice()
    .sort((a, b) => new Date(b.lastModifiedAt || b.createdAt).getTime() - new Date(a.lastModifiedAt || a.createdAt).getTime())
    .slice(0, 5);

  const getServiceTypeIcon = (serviceType: string) => {
    switch (serviceType) {
      case 'CRISIS_INTERVENTION': return '🚨';
      case 'INDIVIDUAL_COUNSELING': return '💬';
      case 'GROUP_COUNSELING': return '👥';
      case 'CASE_MANAGEMENT': return '📋';
      case 'LEGAL_ADVOCACY': return '⚖️';
      case 'MEDICAL_ADVOCACY': return '🏥';
      case 'HOUSING_ASSISTANCE': return '🏠';
      case 'FINANCIAL_ASSISTANCE': return '💰';
      default: return '📝';
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'COMPLETED': return 'bg-green-100 text-green-800';
      case 'IN_PROGRESS': return 'bg-yellow-100 text-yellow-800';
      case 'SCHEDULED': return 'bg-blue-100 text-blue-800';
      case 'CANCELLED': return 'bg-red-100 text-red-800';
      case 'NO_SHOW': return 'bg-gray-100 text-gray-800';
      case 'POSTPONED': return 'bg-purple-100 text-purple-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <div className="space-y-6">
      {/* Key Metrics */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-slate-600">Total Services</p>
                <p className="text-2xl font-bold text-slate-900">{serviceEpisodes.length}</p>
              </div>
              <div className="text-2xl">📊</div>
            </div>
            <p className="text-xs text-slate-500 mt-1">
              {recentServices.length} in last 30 days
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-slate-600">Completed</p>
                <p className="text-2xl font-bold text-green-600">{completedServices.length}</p>
              </div>
              <div className="text-2xl">✅</div>
            </div>
            <p className="text-xs text-slate-500 mt-1">
              {serviceEpisodes.length > 0 ? Math.round((completedServices.length / serviceEpisodes.length) * 100) : 0}% completion rate
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-slate-600">Service Hours</p>
                <p className="text-2xl font-bold text-blue-600">{totalServiceHours.toFixed(1)}</p>
              </div>
              <div className="text-2xl">⏱️</div>
            </div>
            <p className="text-xs text-slate-500 mt-1">
              Total completed hours
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-slate-600">Follow-ups</p>
                <p className="text-2xl font-bold text-orange-600">{servicesNeedingFollowUp.length}</p>
              </div>
              <div className="text-2xl">📋</div>
            </div>
            <p className="text-xs text-slate-500 mt-1">
              {overdueFollowUps.length} overdue
            </p>
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Service Type Breakdown */}
        <Card>
          <CardHeader>
            <CardTitle>Service Types</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {Object.entries(serviceTypeBreakdown)
                .sort(([,a], [,b]) => b - a)
                .slice(0, 8)
                .map(([type, count]) => (
                  <div key={type} className="flex items-center justify-between">
                    <div className="flex items-center space-x-2">
                      <span className="text-lg">{getServiceTypeIcon(type)}</span>
                      <span className="text-sm font-medium">{type.replace(/_/g, ' ')}</span>
                    </div>
                    <div className="flex items-center space-x-2">
                      <span className="text-sm text-slate-600">{count}</span>
                      <div className="w-16 bg-slate-200 rounded-full h-2">
                        <div
                          className="bg-blue-600 h-2 rounded-full"
                          style={{
                            width: `${(count / serviceEpisodes.length) * 100}%`
                          }}
                        />
                      </div>
                    </div>
                  </div>
                ))}
            </div>
          </CardContent>
        </Card>

        {/* Funding Sources */}
        <Card>
          <CardHeader>
            <CardTitle>Funding Sources</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {Object.entries(fundingBreakdown)
                .sort(([,a], [,b]) => b - a)
                .map(([funder, count]) => (
                  <div key={funder} className="flex items-center justify-between">
                    <span className="text-sm font-medium">{funder}</span>
                    <div className="flex items-center space-x-2">
                      <span className="text-sm text-slate-600">{count}</span>
                      <div className="w-16 bg-slate-200 rounded-full h-2">
                        <div
                          className="bg-green-600 h-2 rounded-full"
                          style={{
                            width: `${(count / serviceEpisodes.length) * 100}%`
                          }}
                        />
                      </div>
                    </div>
                  </div>
                ))}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Current Status Overview */}
      <Card>
        <CardHeader>
          <CardTitle>Current Status</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="text-center p-4 bg-blue-50 rounded-lg">
              <div className="text-2xl font-bold text-blue-600">{scheduledServices.length}</div>
              <div className="text-sm text-blue-700">Scheduled Services</div>
            </div>
            <div className="text-center p-4 bg-yellow-50 rounded-lg">
              <div className="text-2xl font-bold text-yellow-600">{inProgressServices.length}</div>
              <div className="text-sm text-yellow-700">In Progress</div>
            </div>
            <div className="text-center p-4 bg-orange-50 rounded-lg">
              <div className="text-2xl font-bold text-orange-600">{overdueFollowUps.length}</div>
              <div className="text-sm text-orange-700">Overdue Follow-ups</div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Recent Activity */}
      <Card>
        <CardHeader>
          <CardTitle>Recent Activity</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            {recentActivity.length === 0 ? (
              <div className="text-center text-slate-600 py-4">
                No recent service activity
              </div>
            ) : (
              recentActivity.map((service) => (
                <div key={service.episodeId} className="flex items-center justify-between p-3 bg-slate-50 rounded-lg">
                  <div className="flex items-center space-x-3">
                    <span className="text-lg">{getServiceTypeIcon(service.serviceType)}</span>
                    <div>
                      <div className="font-medium text-sm">{service.serviceType.replace(/_/g, ' ')}</div>
                      <div className="text-xs text-slate-600">
                        {format(new Date(service.serviceDate), 'MMM d, yyyy')} • {service.primaryProviderName}
                      </div>
                    </div>
                  </div>
                  <Badge className={`text-xs ${getStatusColor(service.completionStatus)}`}>
                    {service.completionStatus}
                  </Badge>
                </div>
              ))
            )}
          </div>
        </CardContent>
      </Card>

      {/* Action Items */}
      {(overdueFollowUps.length > 0 || inProgressServices.length > 0) && (
        <Card>
          <CardHeader>
            <CardTitle className="text-red-600">Action Required</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {overdueFollowUps.map((service) => (
                <div key={service.episodeId} className="flex items-center justify-between p-3 bg-red-50 border border-red-200 rounded-lg">
                  <div className="flex items-center space-x-3">
                    <span className="text-lg">⚠️</span>
                    <div>
                      <div className="font-medium text-sm text-red-800">
                        Follow-up overdue: {service.serviceType.replace(/_/g, ' ')}
                      </div>
                      <div className="text-xs text-red-600">
                        Due: {service.followUpDate && format(new Date(service.followUpDate), 'MMM d, yyyy')}
                      </div>
                    </div>
                  </div>
                  <Button size="sm" variant="outline" className="text-red-600 border-red-300">
                    Complete Follow-up
                  </Button>
                </div>
              ))}

              {inProgressServices.map((service) => (
                <div key={service.episodeId} className="flex items-center justify-between p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
                  <div className="flex items-center space-x-3">
                    <span className="text-lg">⏳</span>
                    <div>
                      <div className="font-medium text-sm text-yellow-800">
                        Service in progress: {service.serviceType.replace(/_/g, ' ')}
                      </div>
                      <div className="text-xs text-yellow-600">
                        Started: {service.startTime && format(new Date(service.startTime), 'MMM d, yyyy h:mm a')}
                      </div>
                    </div>
                  </div>
                  <Button size="sm" variant="outline" className="text-yellow-600 border-yellow-300">
                    Complete Service
                  </Button>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
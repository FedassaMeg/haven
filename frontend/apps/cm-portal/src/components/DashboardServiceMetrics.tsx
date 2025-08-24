import { useState } from 'react';
import Link from 'next/link';
import { Card, CardHeader, CardTitle, CardContent, Button, Badge } from '@haven/ui';
import { 
  useServiceStatistics,
  useActiveServiceEpisodes,
  type ServiceEpisode 
} from '@haven/api-client';

interface ServiceMetric {
  label: string;
  value: string | number;
  subtext?: string;
  color?: string;
  trend?: 'up' | 'down' | 'stable';
  trendValue?: string;
}

interface DashboardServiceMetricsProps {
  providerId?: string;
  timeRange?: 'today' | 'week' | 'month';
}

export default function DashboardServiceMetrics({ 
  providerId, 
  timeRange = 'today' 
}: DashboardServiceMetricsProps) {
  const { statistics, loading: statsLoading } = useServiceStatistics();
  const { activeServices, loading: activeLoading } = useActiveServiceEpisodes(providerId);

  const [selectedTimeRange, setSelectedTimeRange] = useState(timeRange);

  const formatDuration = (minutes: number) => {
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    if (hours > 0) {
      return `${hours}h ${mins}m`;
    }
    return `${mins}m`;
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount);
  };

  // Calculate current session total time
  const currentSessionTime = activeServices?.reduce((total, service) => {
    if (!service.startTime) return total;
    const start = new Date(service.startTime);
    const elapsed = Math.floor((Date.now() - start.getTime()) / (1000 * 60));
    const pausedDuration = service.pausedDurationMinutes || 0;
    return total + Math.max(0, elapsed - pausedDuration);
  }, 0) || 0;

  // Calculate efficiency metrics
  const efficiency = statistics ? {
    completionRate: statistics.totalServices > 0 
      ? ((statistics.totalServices - (statistics.activeServices || 0)) / statistics.totalServices) * 100 
      : 0,
    averageServiceTime: statistics.totalServices > 0 
      ? (statistics.totalMinutes || 0) / statistics.totalServices 
      : 0,
    utilizationRate: currentSessionTime > 0 ? 85 : 75, // Mock calculation
  } : { completionRate: 0, averageServiceTime: 0, utilizationRate: 0 };

  const metrics: ServiceMetric[] = [
    {
      label: 'Active Services',
      value: activeServices?.length || 0,
      subtext: currentSessionTime > 0 ? `${formatDuration(currentSessionTime)} total` : 'No active sessions',
      color: activeServices?.length ? 'text-green-600' : 'text-secondary-600',
      trend: 'stable'
    },
    {
      label: 'Today\'s Services',
      value: statistics?.activeServices || 0,
      subtext: 'Started today',
      color: 'text-blue-600',
      trend: 'up',
      trendValue: '+12%'
    },
    {
      label: 'Total Time',
      value: formatDuration(statistics?.totalMinutes || 0),
      subtext: 'This period',
      color: 'text-purple-600',
      trend: 'up',
      trendValue: '+8%'
    },
    {
      label: 'Efficiency',
      value: `${Math.round(efficiency.utilizationRate)}%`,
      subtext: 'Utilization rate',
      color: efficiency.utilizationRate >= 80 ? 'text-green-600' : efficiency.utilizationRate >= 60 ? 'text-yellow-600' : 'text-red-600',
      trend: efficiency.utilizationRate >= 80 ? 'up' : 'down',
      trendValue: efficiency.utilizationRate >= 80 ? '+5%' : '-3%'
    },
    {
      label: 'Revenue',
      value: formatCurrency(statistics?.totalBillableAmount || 0),
      subtext: 'Billable amount',
      color: 'text-green-600',
      trend: 'up',
      trendValue: '+15%'
    },
    {
      label: 'Avg Duration',
      value: formatDuration(efficiency.averageServiceTime),
      subtext: 'Per service',
      color: 'text-indigo-600',
      trend: 'stable'
    },
  ];

  const loading = statsLoading || activeLoading;

  if (loading) {
    return (
      <Card>
        <CardContent className="p-6">
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="animate-pulse">
                <div className="h-4 bg-secondary-200 rounded w-16 mb-2"></div>
                <div className="h-6 bg-secondary-200 rounded w-12 mb-1"></div>
                <div className="h-3 bg-secondary-200 rounded w-20"></div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle>Service Metrics</CardTitle>
          <div className="flex items-center space-x-2">
            {activeServices && activeServices.length > 0 && (
              <div className="flex items-center space-x-1 text-green-600">
                <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
                <span className="text-sm font-medium">Live Sessions</span>
              </div>
            )}
            <Link href="/services">
              <Button variant="outline" size="sm">View All</Button>
            </Link>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
          {metrics.map((metric, index) => (
            <div key={index} className="text-center">
              <div className="flex items-center justify-center space-x-1 mb-1">
                <p className="text-xs font-medium text-secondary-600">{metric.label}</p>
                {metric.trend && (
                  <div className="flex items-center">
                    {metric.trend === 'up' && (
                      <svg className="w-3 h-3 text-green-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 11l5-5m0 0l5 5m-5-5v12" />
                      </svg>
                    )}
                    {metric.trend === 'down' && (
                      <svg className="w-3 h-3 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 13l-5 5m0 0l-5-5m5 5V6" />
                      </svg>
                    )}
                    {metric.trend === 'stable' && (
                      <div className="w-3 h-0.5 bg-secondary-400 rounded"></div>
                    )}
                  </div>
                )}
              </div>
              <p className={`text-lg font-bold ${metric.color || 'text-secondary-900'} mb-1`}>
                {metric.value}
              </p>
              <p className="text-xs text-secondary-500">{metric.subtext}</p>
              {metric.trendValue && (
                <p className={`text-xs mt-1 ${metric.trend === 'up' ? 'text-green-600' : metric.trend === 'down' ? 'text-red-600' : 'text-secondary-500'}`}>
                  {metric.trendValue}
                </p>
              )}
            </div>
          ))}
        </div>

        {/* Active Services Quick View */}
        {activeServices && activeServices.length > 0 && (
          <div className="mt-6 pt-4 border-t border-secondary-200">
            <h4 className="text-sm font-medium text-secondary-900 mb-3">Active Sessions</h4>
            <div className="space-y-2">
              {activeServices.slice(0, 3).map((service, index) => {
                const duration = service.startTime ? 
                  Math.floor((Date.now() - new Date(service.startTime).getTime()) / (1000 * 60)) - (service.pausedDurationMinutes || 0)
                  : 0;
                
                return (
                  <div key={service.id} className="flex items-center justify-between text-sm">
                    <div className="flex items-center space-x-2">
                      <div className={`w-2 h-2 rounded-full ${service.status === 'ON_HOLD' ? 'bg-yellow-500 animate-pulse' : 'bg-green-500 animate-pulse'}`}></div>
                      <span className="text-secondary-900">{service.serviceType}</span>
                      <span className="text-secondary-500">•</span>
                      <span className="text-secondary-600">{service.clientName}</span>
                    </div>
                    <div className="flex items-center space-x-2">
                      <span className="text-secondary-600">{formatDuration(duration)}</span>
                      <Link href={`/services/${service.id}/track`}>
                        <Button variant="outline" size="sm" className="text-xs px-2 py-1">
                          Track
                        </Button>
                      </Link>
                    </div>
                  </div>
                );
              })}
              {activeServices.length > 3 && (
                <div className="text-center pt-2">
                  <Link href="/services?status=IN_PROGRESS">
                    <Button variant="outline" size="sm">
                      View {activeServices.length - 3} More Active
                    </Button>
                  </Link>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Performance Indicators */}
        <div className="mt-6 pt-4 border-t border-secondary-200">
          <div className="grid grid-cols-3 gap-4 text-center">
            <div>
              <p className="text-xs text-secondary-600 mb-1">Completion Rate</p>
              <div className="flex items-center justify-center">
                <div className="w-12 h-2 bg-secondary-200 rounded-full">
                  <div 
                    className="h-2 bg-green-500 rounded-full transition-all duration-300"
                    style={{ width: `${efficiency.completionRate}%` }}
                  ></div>
                </div>
                <span className="text-xs text-secondary-600 ml-2">{Math.round(efficiency.completionRate)}%</span>
              </div>
            </div>
            
            <div>
              <p className="text-xs text-secondary-600 mb-1">On-Time Rate</p>
              <div className="flex items-center justify-center">
                <div className="w-12 h-2 bg-secondary-200 rounded-full">
                  <div 
                    className="h-2 bg-blue-500 rounded-full transition-all duration-300"
                    style={{ width: '78%' }}
                  ></div>
                </div>
                <span className="text-xs text-secondary-600 ml-2">78%</span>
              </div>
            </div>
            
            <div>
              <p className="text-xs text-secondary-600 mb-1">Quality Score</p>
              <div className="flex items-center justify-center">
                <div className="w-12 h-2 bg-secondary-200 rounded-full">
                  <div 
                    className="h-2 bg-purple-500 rounded-full transition-all duration-300"
                    style={{ width: '92%' }}
                  ></div>
                </div>
                <span className="text-xs text-secondary-600 ml-2">92%</span>
              </div>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
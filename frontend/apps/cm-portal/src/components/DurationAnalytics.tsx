import { useState, useMemo } from 'react';
import { Card, CardHeader, CardTitle, CardContent, Select, Badge } from '@haven/ui';
import { 
  useServiceStatistics,
  useServiceEpisodes,
  type ServiceEpisode 
} from '@haven/api-client';

interface DurationMetrics {
  averageDuration: number;
  medianDuration: number;
  totalDuration: number;
  completedServices: number;
  overtimeServices: number;
  onTimeServices: number;
  efficiencyRate: number;
}

interface ServiceTypeAnalysis {
  serviceType: string;
  totalEpisodes: number;
  averageDuration: number;
  expectedDuration: number;
  variancePercent: number;
  overtimeRate: number;
}

interface DurationAnalyticsProps {
  providerId?: string;
  clientId?: string;
  timeRange?: 'week' | 'month' | 'quarter' | 'year';
}

export default function DurationAnalytics({ 
  providerId, 
  clientId, 
  timeRange = 'month' 
}: DurationAnalyticsProps) {
  const [selectedTimeRange, setSelectedTimeRange] = useState(timeRange);
  const [selectedServiceType, setSelectedServiceType] = useState<string>('all');

  // Calculate date range
  const dateRange = useMemo(() => {
    const now = new Date();
    const ranges = {
      week: 7,
      month: 30,
      quarter: 90,
      year: 365
    };
    
    const startDate = new Date(now.getTime() - ranges[selectedTimeRange] * 24 * 60 * 60 * 1000);
    return {
      startDateFrom: startDate.toISOString(),
      startDateTo: now.toISOString()
    };
  }, [selectedTimeRange]);

  const { serviceEpisodes, loading } = useServiceEpisodes({
    providerId,
    clientId,
    status: 'COMPLETED',
    ...dateRange,
    page: 0,
    size: 1000 // Get all completed services for analysis
  });

  const { statistics } = useServiceStatistics();

  // Filter services by type if selected
  const filteredServices = useMemo(() => {
    if (!serviceEpisodes) return [];
    if (selectedServiceType === 'all') return serviceEpisodes;
    return serviceEpisodes.filter(s => s.serviceType === selectedServiceType);
  }, [serviceEpisodes, selectedServiceType]);

  // Calculate duration metrics
  const metrics = useMemo((): DurationMetrics => {
    if (!filteredServices.length) {
      return {
        averageDuration: 0,
        medianDuration: 0,
        totalDuration: 0,
        completedServices: 0,
        overtimeServices: 0,
        onTimeServices: 0,
        efficiencyRate: 0
      };
    }

    const durations = filteredServices
      .map(s => s.actualDurationMinutes || 0)
      .filter(d => d > 0)
      .sort((a, b) => a - b);

    const totalDuration = durations.reduce((sum, d) => sum + d, 0);
    const averageDuration = totalDuration / durations.length;
    const medianDuration = durations.length > 0 ? durations[Math.floor(durations.length / 2)] : 0;

    // Calculate overtime services
    const overtimeServices = filteredServices.filter(s => 
      s.actualDurationMinutes && 
      s.expectedDurationMinutes && 
      s.actualDurationMinutes > s.expectedDurationMinutes
    ).length;

    const onTimeServices = filteredServices.length - overtimeServices;
    const efficiencyRate = filteredServices.length > 0 ? (onTimeServices / filteredServices.length) * 100 : 0;

    return {
      averageDuration,
      medianDuration,
      totalDuration,
      completedServices: filteredServices.length,
      overtimeServices,
      onTimeServices,
      efficiencyRate
    };
  }, [filteredServices]);

  // Analyze by service type
  const serviceTypeAnalysis = useMemo((): ServiceTypeAnalysis[] => {
    if (!serviceEpisodes) return [];

    const typeGroups = serviceEpisodes.reduce((acc, service) => {
      const type = service.serviceType;
      if (!acc[type]) {
        acc[type] = [];
      }
      acc[type].push(service);
      return acc;
    }, {} as Record<string, ServiceEpisode[]>);

    return Object.entries(typeGroups).map(([serviceType, episodes]) => {
      const completedEpisodes = episodes.filter(e => e.actualDurationMinutes && e.actualDurationMinutes > 0);
      const totalDuration = completedEpisodes.reduce((sum, e) => sum + (e.actualDurationMinutes || 0), 0);
      const averageDuration = completedEpisodes.length > 0 ? totalDuration / completedEpisodes.length : 0;
      
      // Get typical expected duration for this service type
      const expectedDurations = completedEpisodes
        .map(e => e.expectedDurationMinutes)
        .filter(d => d && d > 0);
      const expectedDuration = expectedDurations.length > 0 
        ? expectedDurations.reduce((sum, d) => sum + d, 0) / expectedDurations.length 
        : 0;

      const variancePercent = expectedDuration > 0 
        ? ((averageDuration - expectedDuration) / expectedDuration) * 100 
        : 0;

      const overtimeCount = completedEpisodes.filter(e => 
        e.actualDurationMinutes && 
        e.expectedDurationMinutes && 
        e.actualDurationMinutes > e.expectedDurationMinutes
      ).length;

      const overtimeRate = completedEpisodes.length > 0 ? (overtimeCount / completedEpisodes.length) * 100 : 0;

      return {
        serviceType,
        totalEpisodes: episodes.length,
        averageDuration,
        expectedDuration,
        variancePercent,
        overtimeRate
      };
    }).sort((a, b) => b.totalEpisodes - a.totalEpisodes);
  }, [serviceEpisodes]);

  const formatDuration = (minutes: number) => {
    const hours = Math.floor(minutes / 60);
    const mins = Math.round(minutes % 60);
    if (hours > 0) {
      return `${hours}h ${mins}m`;
    }
    return `${mins}m`;
  };

  const formatPercent = (value: number) => {
    return `${Math.round(value)}%`;
  };

  const getVarianceColor = (variance: number) => {
    if (variance > 20) return 'text-red-600';
    if (variance > 10) return 'text-yellow-600';
    if (variance < -10) return 'text-blue-600';
    return 'text-green-600';
  };

  const getEfficiencyColor = (rate: number) => {
    if (rate >= 80) return 'text-green-600';
    if (rate >= 60) return 'text-yellow-600';
    return 'text-red-600';
  };

  if (loading) {
    return (
      <Card>
        <CardContent className="p-6">
          <div className="flex items-center justify-center">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
          </div>
        </CardContent>
      </Card>
    );
  }

  const uniqueServiceTypes = serviceEpisodes 
    ? Array.from(new Set(serviceEpisodes.map(s => s.serviceType)))
    : [];

  return (
    <div className="space-y-6">
      {/* Controls */}
      <Card>
        <CardHeader>
          <CardTitle>Duration Analytics</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-secondary-700 mb-1">Time Range</label>
              <Select
                value={selectedTimeRange}
                onChange={(value) => setSelectedTimeRange(value as any)}
                options={[
                  { value: 'week', label: 'Last 7 days' },
                  { value: 'month', label: 'Last 30 days' },
                  { value: 'quarter', label: 'Last 90 days' },
                  { value: 'year', label: 'Last year' }
                ]}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-secondary-700 mb-1">Service Type</label>
              <Select
                value={selectedServiceType}
                onChange={setSelectedServiceType}
                options={[
                  { value: 'all', label: 'All Service Types' },
                  ...uniqueServiceTypes.map(type => ({ value: type, label: type }))
                ]}
              />
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Key Metrics */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4">
            <div className="text-center">
              <p className="text-2xl font-bold text-primary-600">{formatDuration(metrics.averageDuration)}</p>
              <p className="text-sm text-secondary-600">Average Duration</p>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="text-center">
              <p className="text-2xl font-bold text-secondary-900">{metrics.completedServices}</p>
              <p className="text-sm text-secondary-600">Completed Services</p>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="text-center">
              <p className={`text-2xl font-bold ${getEfficiencyColor(metrics.efficiencyRate)}`}>
                {formatPercent(metrics.efficiencyRate)}
              </p>
              <p className="text-sm text-secondary-600">On-Time Rate</p>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="text-center">
              <p className="text-2xl font-bold text-purple-600">{formatDuration(metrics.totalDuration)}</p>
              <p className="text-sm text-secondary-600">Total Time</p>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Service Type Analysis */}
      <Card>
        <CardHeader>
          <CardTitle>Service Type Performance</CardTitle>
        </CardHeader>
        <CardContent>
          {serviceTypeAnalysis.length > 0 ? (
            <div className="space-y-4">
              {serviceTypeAnalysis.map((analysis) => (
                <div key={analysis.serviceType} className="border border-secondary-200 rounded-lg p-4">
                  <div className="flex items-center justify-between mb-3">
                    <h4 className="font-medium text-secondary-900">{analysis.serviceType}</h4>
                    <Badge variant="outline">{analysis.totalEpisodes} episodes</Badge>
                  </div>
                  
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                    <div>
                      <p className="text-secondary-600">Average Duration</p>
                      <p className="font-medium">{formatDuration(analysis.averageDuration)}</p>
                    </div>
                    <div>
                      <p className="text-secondary-600">Expected Duration</p>
                      <p className="font-medium">{formatDuration(analysis.expectedDuration)}</p>
                    </div>
                    <div>
                      <p className="text-secondary-600">Variance</p>
                      <p className={`font-medium ${getVarianceColor(analysis.variancePercent)}`}>
                        {analysis.variancePercent > 0 ? '+' : ''}{formatPercent(analysis.variancePercent)}
                      </p>
                    </div>
                    <div>
                      <p className="text-secondary-600">Overtime Rate</p>
                      <p className={`font-medium ${analysis.overtimeRate > 30 ? 'text-red-600' : analysis.overtimeRate > 15 ? 'text-yellow-600' : 'text-green-600'}`}>
                        {formatPercent(analysis.overtimeRate)}
                      </p>
                    </div>
                  </div>

                  {/* Performance indicator */}
                  <div className="mt-3 pt-3 border-t border-secondary-200">
                    {analysis.variancePercent > 20 && analysis.overtimeRate > 30 ? (
                      <div className="flex items-center space-x-2 text-red-600">
                        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
                        </svg>
                        <span className="text-sm">Needs attention: High variance and overtime rate</span>
                      </div>
                    ) : analysis.variancePercent < 10 && analysis.overtimeRate < 15 ? (
                      <div className="flex items-center space-x-2 text-green-600">
                        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                        </svg>
                        <span className="text-sm">Performing well: Consistent duration timing</span>
                      </div>
                    ) : (
                      <div className="flex items-center space-x-2 text-yellow-600">
                        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                        <span className="text-sm">Room for improvement: Monitor duration patterns</span>
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-8">
              <p className="text-secondary-600">No completed services found for the selected criteria.</p>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Duration Distribution */}
      <Card>
        <CardHeader>
          <CardTitle>Duration Insights</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <h4 className="font-medium text-secondary-900 mb-3">Timing Performance</h4>
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-sm text-secondary-600">On-time services</span>
                  <div className="flex items-center space-x-2">
                    <div className="w-16 bg-secondary-200 rounded-full h-2">
                      <div 
                        className="bg-green-500 h-2 rounded-full" 
                        style={{ width: `${(metrics.onTimeServices / Math.max(metrics.completedServices, 1)) * 100}%` }}
                      ></div>
                    </div>
                    <span className="text-sm font-medium">{metrics.onTimeServices}</span>
                  </div>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm text-secondary-600">Overtime services</span>
                  <div className="flex items-center space-x-2">
                    <div className="w-16 bg-secondary-200 rounded-full h-2">
                      <div 
                        className="bg-red-500 h-2 rounded-full" 
                        style={{ width: `${(metrics.overtimeServices / Math.max(metrics.completedServices, 1)) * 100}%` }}
                      ></div>
                    </div>
                    <span className="text-sm font-medium">{metrics.overtimeServices}</span>
                  </div>
                </div>
              </div>
            </div>

            <div>
              <h4 className="font-medium text-secondary-900 mb-3">Duration Statistics</h4>
              <div className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-secondary-600">Average:</span>
                  <span className="font-medium">{formatDuration(metrics.averageDuration)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-secondary-600">Median:</span>
                  <span className="font-medium">{formatDuration(metrics.medianDuration)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-secondary-600">Total Time:</span>
                  <span className="font-medium">{formatDuration(metrics.totalDuration)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-secondary-600">Efficiency Rate:</span>
                  <span className={`font-medium ${getEfficiencyColor(metrics.efficiencyRate)}`}>
                    {formatPercent(metrics.efficiencyRate)}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
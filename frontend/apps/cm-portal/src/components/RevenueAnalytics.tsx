import { useState, useMemo } from 'react';
import { Card, CardHeader, CardTitle, CardContent, Select, Badge } from '@haven/ui';
import { 
  useServiceEpisodes,
  useBillingStatistics,
  type ServiceEpisode 
} from '@haven/api-client';

interface RevenueMetrics {
  totalRevenue: number;
  billedRevenue: number;
  paidRevenue: number;
  pendingRevenue: number;
  projectedRevenue: number;
  revenueGrowth: number;
  averageServiceValue: number;
  collectionRate: number;
}

interface FundingSourceRevenue {
  source: string;
  revenue: number;
  percentage: number;
  services: number;
  averageValue: number;
  trend: 'up' | 'down' | 'stable';
}

interface MonthlyRevenue {
  month: string;
  revenue: number;
  services: number;
  hours: number;
}

interface RevenueAnalyticsProps {
  timeRange?: 'month' | 'quarter' | 'year';
  providerId?: string;
  programId?: string;
}

export default function RevenueAnalytics({ 
  timeRange = 'month',
  providerId,
  programId 
}: RevenueAnalyticsProps) {
  const [selectedTimeRange, setSelectedTimeRange] = useState(timeRange);
  const [selectedView, setSelectedView] = useState<'overview' | 'trends' | 'sources' | 'projections'>('overview');

  // Calculate date range
  const dateRange = useMemo(() => {
    const now = new Date();
    const ranges = {
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
    ...dateRange,
    status: 'COMPLETED',
    providerId,
    programId,
    page: 0,
    size: 1000
  });

  const { billingStats } = useBillingStatistics();

  // Calculate revenue metrics
  const metrics = useMemo((): RevenueMetrics => {
    if (!serviceEpisodes) {
      return {
        totalRevenue: 0,
        billedRevenue: 0,
        paidRevenue: 0,
        pendingRevenue: 0,
        projectedRevenue: 0,
        revenueGrowth: 0,
        averageServiceValue: 0,
        collectionRate: 0
      };
    }

    const billableServices = serviceEpisodes.filter(s => s.billableAmount && s.billableAmount > 0);
    const totalRevenue = billableServices.reduce((sum, s) => sum + (s.billableAmount || 0), 0);
    
    // Mock billing status data (would come from actual billing system)
    const paidRevenue = totalRevenue * 0.65; // 65% collection rate
    const billedRevenue = totalRevenue * 0.85; // 85% billed
    const pendingRevenue = totalRevenue - billedRevenue;
    
    // Project based on current rate
    const daysInPeriod = selectedTimeRange === 'month' ? 30 : selectedTimeRange === 'quarter' ? 90 : 365;
    const dailyAverage = totalRevenue / daysInPeriod;
    const remainingDays = selectedTimeRange === 'month' ? 30 - new Date().getDate() : 
                          selectedTimeRange === 'quarter' ? 90 - ((Date.now() - new Date(new Date().getFullYear(), Math.floor(new Date().getMonth() / 3) * 3, 1).getTime()) / (1000 * 60 * 60 * 24)) :
                          365 - new Date().getDayOfYear();
    const projectedRevenue = totalRevenue + (dailyAverage * remainingDays);
    
    const averageServiceValue = billableServices.length > 0 ? totalRevenue / billableServices.length : 0;
    const collectionRate = billedRevenue > 0 ? (paidRevenue / billedRevenue) * 100 : 0;
    const revenueGrowth = 12.5; // Mock growth rate

    return {
      totalRevenue,
      billedRevenue,
      paidRevenue,
      pendingRevenue,
      projectedRevenue,
      revenueGrowth,
      averageServiceValue,
      collectionRate
    };
  }, [serviceEpisodes, selectedTimeRange]);

  // Analyze funding source revenue
  const fundingSourceRevenue = useMemo((): FundingSourceRevenue[] => {
    if (!serviceEpisodes) return [];

    const sourceGroups = serviceEpisodes.reduce((acc, service) => {
      const source = service.fundingSources?.[0]?.funderName || 'Unspecified';
      if (!acc[source]) {
        acc[source] = { revenue: 0, services: 0, hours: 0 };
      }
      acc[source].revenue += service.billableAmount || 0;
      acc[source].services += 1;
      acc[source].hours += (service.actualDurationMinutes || 0) / 60;
      return acc;
    }, {} as Record<string, { revenue: number; services: number; hours: number }>);

    const totalRevenue = Object.values(sourceGroups).reduce((sum, group) => sum + group.revenue, 0);

    return Object.entries(sourceGroups)
      .map(([source, data]) => ({
        source,
        revenue: data.revenue,
        percentage: totalRevenue > 0 ? (data.revenue / totalRevenue) * 100 : 0,
        services: data.services,
        averageValue: data.services > 0 ? data.revenue / data.services : 0,
        trend: Math.random() > 0.5 ? 'up' : Math.random() > 0.3 ? 'stable' : 'down' as any // Mock trend
      }))
      .sort((a, b) => b.revenue - a.revenue);
  }, [serviceEpisodes]);

  // Generate monthly revenue trends (mock data for demonstration)
  const monthlyTrends = useMemo((): MonthlyRevenue[] => {
    const months = ['Oct', 'Nov', 'Dec', 'Jan', 'Feb', 'Mar'];
    return months.map((month, index) => ({
      month,
      revenue: metrics.totalRevenue * (0.8 + Math.random() * 0.4) / 6, // Distribute revenue across months
      services: Math.floor(Math.random() * 50) + 20,
      hours: Math.floor(Math.random() * 200) + 100
    }));
  }, [metrics.totalRevenue]);

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount);
  };

  const formatPercent = (value: number) => {
    return `${Math.round(value)}%`;
  };

  const getTrendIcon = (trend: string) => {
    switch (trend) {
      case 'up':
        return <svg className="w-4 h-4 text-green-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 11l5-5m0 0l5 5m-5-5v12" />
        </svg>;
      case 'down':
        return <svg className="w-4 h-4 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 13l-5 5m0 0l-5-5m5 5V6" />
        </svg>;
      default:
        return <div className="w-4 h-1 bg-secondary-400 rounded"></div>;
    }
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

  return (
    <div className="space-y-6">
      {/* Controls */}
      <Card>
        <CardHeader>
          <CardTitle>Revenue Analytics</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center space-x-4">
            <div>
              <label className="block text-sm font-medium text-secondary-700 mb-1">Time Range</label>
              <Select
                value={selectedTimeRange}
                onChange={(value) => setSelectedTimeRange(value as any)}
                options={[
                  { value: 'month', label: 'Last 30 days' },
                  { value: 'quarter', label: 'Last 90 days' },
                  { value: 'year', label: 'Last year' }
                ]}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-secondary-700 mb-1">View</label>
              <Select
                value={selectedView}
                onChange={(value) => setSelectedView(value as any)}
                options={[
                  { value: 'overview', label: 'Overview' },
                  { value: 'trends', label: 'Trends' },
                  { value: 'sources', label: 'Funding Sources' },
                  { value: 'projections', label: 'Projections' }
                ]}
              />
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Key Revenue Metrics */}
      {selectedView === 'overview' && (
        <>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <Card>
              <CardContent className="p-4">
                <div className="text-center">
                  <p className="text-2xl font-bold text-green-600">{formatCurrency(metrics.totalRevenue)}</p>
                  <p className="text-sm text-secondary-600">Total Revenue</p>
                  <div className="flex items-center justify-center space-x-1 mt-1">
                    <svg className="w-3 h-3 text-green-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 11l5-5m0 0l5 5m-5-5v12" />
                    </svg>
                    <span className="text-xs text-green-600">+{formatPercent(metrics.revenueGrowth)}</span>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="p-4">
                <div className="text-center">
                  <p className="text-2xl font-bold text-blue-600">{formatCurrency(metrics.paidRevenue)}</p>
                  <p className="text-sm text-secondary-600">Collected</p>
                  <p className="text-xs text-secondary-500 mt-1">
                    {formatPercent(metrics.collectionRate)} rate
                  </p>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="p-4">
                <div className="text-center">
                  <p className="text-2xl font-bold text-yellow-600">{formatCurrency(metrics.pendingRevenue)}</p>
                  <p className="text-sm text-secondary-600">Pending</p>
                  <p className="text-xs text-secondary-500 mt-1">
                    {formatPercent((metrics.pendingRevenue / metrics.totalRevenue) * 100)} of total
                  </p>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="p-4">
                <div className="text-center">
                  <p className="text-2xl font-bold text-purple-600">{formatCurrency(metrics.averageServiceValue)}</p>
                  <p className="text-sm text-secondary-600">Avg Service Value</p>
                  <p className="text-xs text-secondary-500 mt-1">Per episode</p>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Revenue Breakdown */}
          <Card>
            <CardHeader>
              <CardTitle>Revenue Breakdown</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div>
                  <h4 className="font-medium text-secondary-900 mb-3">Collection Status</h4>
                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-secondary-600">Paid</span>
                      <div className="flex items-center space-x-2">
                        <div className="w-16 bg-secondary-200 rounded-full h-2">
                          <div 
                            className="bg-green-500 h-2 rounded-full" 
                            style={{ width: `${(metrics.paidRevenue / metrics.totalRevenue) * 100}%` }}
                          ></div>
                        </div>
                        <span className="text-sm font-medium">{formatCurrency(metrics.paidRevenue)}</span>
                      </div>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-secondary-600">Billed</span>
                      <div className="flex items-center space-x-2">
                        <div className="w-16 bg-secondary-200 rounded-full h-2">
                          <div 
                            className="bg-blue-500 h-2 rounded-full" 
                            style={{ width: `${((metrics.billedRevenue - metrics.paidRevenue) / metrics.totalRevenue) * 100}%` }}
                          ></div>
                        </div>
                        <span className="text-sm font-medium">{formatCurrency(metrics.billedRevenue - metrics.paidRevenue)}</span>
                      </div>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-secondary-600">Pending</span>
                      <div className="flex items-center space-x-2">
                        <div className="w-16 bg-secondary-200 rounded-full h-2">
                          <div 
                            className="bg-yellow-500 h-2 rounded-full" 
                            style={{ width: `${(metrics.pendingRevenue / metrics.totalRevenue) * 100}%` }}
                          ></div>
                        </div>
                        <span className="text-sm font-medium">{formatCurrency(metrics.pendingRevenue)}</span>
                      </div>
                    </div>
                  </div>
                </div>

                <div>
                  <h4 className="font-medium text-secondary-900 mb-3">Key Metrics</h4>
                  <div className="space-y-2 text-sm">
                    <div className="flex justify-between">
                      <span className="text-secondary-600">Collection Rate:</span>
                      <span className="font-medium">{formatPercent(metrics.collectionRate)}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-secondary-600">Average Service:</span>
                      <span className="font-medium">{formatCurrency(metrics.averageServiceValue)}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-secondary-600">Growth Rate:</span>
                      <span className="font-medium text-green-600">+{formatPercent(metrics.revenueGrowth)}</span>
                    </div>
                  </div>
                </div>

                <div>
                  <h4 className="font-medium text-secondary-900 mb-3">Projections</h4>
                  <div className="space-y-2 text-sm">
                    <div className="flex justify-between">
                      <span className="text-secondary-600">Projected Total:</span>
                      <span className="font-medium">{formatCurrency(metrics.projectedRevenue)}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-secondary-600">Expected Growth:</span>
                      <span className="font-medium text-blue-600">
                        {formatCurrency(metrics.projectedRevenue - metrics.totalRevenue)}
                      </span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-secondary-600">Target Achievement:</span>
                      <span className="font-medium">87%</span>
                    </div>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </>
      )}

      {/* Funding Source Analysis */}
      {selectedView === 'sources' && (
        <Card>
          <CardHeader>
            <CardTitle>Revenue by Funding Source</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {fundingSourceRevenue.map((source, index) => (
                <div key={source.source} className="border border-secondary-200 rounded-lg p-4">
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center space-x-3">
                      <h4 className="font-medium text-secondary-900">{source.source}</h4>
                      {getTrendIcon(source.trend)}
                    </div>
                    <div className="flex items-center space-x-3">
                      <Badge variant="outline">{source.services} services</Badge>
                      <span className="text-lg font-bold text-green-600">
                        {formatCurrency(source.revenue)}
                      </span>
                    </div>
                  </div>
                  
                  <div className="grid grid-cols-3 gap-4 text-sm">
                    <div>
                      <p className="text-secondary-600">Revenue Share</p>
                      <div className="flex items-center space-x-2 mt-1">
                        <div className="w-20 bg-secondary-200 rounded-full h-2">
                          <div 
                            className="bg-primary-500 h-2 rounded-full" 
                            style={{ width: `${source.percentage}%` }}
                          ></div>
                        </div>
                        <span className="font-medium">{formatPercent(source.percentage)}</span>
                      </div>
                    </div>
                    <div>
                      <p className="text-secondary-600">Avg Service Value</p>
                      <p className="font-medium">{formatCurrency(source.averageValue)}</p>
                    </div>
                    <div>
                      <p className="text-secondary-600">Total Services</p>
                      <p className="font-medium">{source.services}</p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Monthly Trends */}
      {selectedView === 'trends' && (
        <Card>
          <CardHeader>
            <CardTitle>Revenue Trends</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-6">
              {monthlyTrends.map((month, index) => (
                <div key={month.month} className="flex items-center justify-between p-3 border border-secondary-200 rounded-lg">
                  <div className="flex items-center space-x-4">
                    <div className="w-12 text-center">
                      <span className="text-sm font-medium text-secondary-900">{month.month}</span>
                    </div>
                    <div className="w-32 bg-secondary-200 rounded-full h-3">
                      <div 
                        className="bg-green-500 h-3 rounded-full" 
                        style={{ width: `${(month.revenue / Math.max(...monthlyTrends.map(m => m.revenue))) * 100}%` }}
                      ></div>
                    </div>
                  </div>
                  <div className="flex items-center space-x-6 text-sm">
                    <div className="text-center">
                      <p className="text-secondary-600">Revenue</p>
                      <p className="font-medium">{formatCurrency(month.revenue)}</p>
                    </div>
                    <div className="text-center">
                      <p className="text-secondary-600">Services</p>
                      <p className="font-medium">{month.services}</p>
                    </div>
                    <div className="text-center">
                      <p className="text-secondary-600">Hours</p>
                      <p className="font-medium">{month.hours}h</p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Projections */}
      {selectedView === 'projections' && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <Card>
            <CardHeader>
              <CardTitle>Revenue Projections</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="text-center p-4 bg-blue-50 border border-blue-200 rounded-lg">
                  <p className="text-2xl font-bold text-blue-900">{formatCurrency(metrics.projectedRevenue)}</p>
                  <p className="text-sm text-blue-700">Projected {selectedTimeRange} Total</p>
                </div>
                
                <div className="space-y-3">
                  <div className="flex justify-between">
                    <span className="text-sm text-secondary-600">Current Revenue:</span>
                    <span className="font-medium">{formatCurrency(metrics.totalRevenue)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-sm text-secondary-600">Projected Additional:</span>
                    <span className="font-medium text-green-600">
                      {formatCurrency(metrics.projectedRevenue - metrics.totalRevenue)}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-sm text-secondary-600">Expected Growth:</span>
                    <span className="font-medium">
                      {formatPercent(((metrics.projectedRevenue - metrics.totalRevenue) / metrics.totalRevenue) * 100)}
                    </span>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Target Analysis</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="text-center p-4 bg-purple-50 border border-purple-200 rounded-lg">
                  <p className="text-2xl font-bold text-purple-900">87%</p>
                  <p className="text-sm text-purple-700">Target Achievement</p>
                </div>
                
                <div className="space-y-3">
                  <div className="flex justify-between">
                    <span className="text-sm text-secondary-600">Monthly Target:</span>
                    <span className="font-medium">{formatCurrency(metrics.projectedRevenue * 1.15)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-sm text-secondary-600">Gap to Target:</span>
                    <span className="font-medium text-red-600">
                      {formatCurrency(metrics.projectedRevenue * 0.15)}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-sm text-secondary-600">Required Daily:</span>
                    <span className="font-medium">
                      {formatCurrency((metrics.projectedRevenue * 0.15) / 30)}
                    </span>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}
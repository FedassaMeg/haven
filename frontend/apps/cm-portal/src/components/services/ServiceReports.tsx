import React, { useState } from 'react';
import { Card, CardHeader, CardTitle, CardContent, Button, Select, SelectContent, SelectItem, SelectTrigger, SelectValue, Input } from '@haven/ui';
import { ServiceEpisode } from '@haven/api-client';
import { format, startOfMonth, endOfMonth, startOfYear, endOfYear, subMonths, parseISO } from 'date-fns';

interface ServiceReportsProps {
  serviceEpisodes: ServiceEpisode[];
  clientId: string;
  loading: boolean;
}

type ReportPeriod = 'this_month' | 'last_month' | 'this_year' | 'custom';

export default function ServiceReports({ serviceEpisodes, clientId, loading }: ServiceReportsProps) {
  const [reportPeriod, setReportPeriod] = useState<ReportPeriod>('this_month');
  const [customStartDate, setCustomStartDate] = useState('');
  const [customEndDate, setCustomEndDate] = useState('');

  if (loading) {
    return (
      <Card>
        <CardContent className="p-6">
          <div className="text-center">Loading service reports...</div>
        </CardContent>
      </Card>
    );
  }

  // Get date range based on selected period
  const getDateRange = () => {
    const now = new Date();

    switch (reportPeriod) {
      case 'this_month':
        return { start: startOfMonth(now), end: endOfMonth(now) };
      case 'last_month':
        const lastMonth = subMonths(now, 1);
        return { start: startOfMonth(lastMonth), end: endOfMonth(lastMonth) };
      case 'this_year':
        return { start: startOfYear(now), end: endOfYear(now) };
      case 'custom':
        return {
          start: customStartDate ? parseISO(customStartDate) : startOfMonth(now),
          end: customEndDate ? parseISO(customEndDate) : endOfMonth(now)
        };
      default:
        return { start: startOfMonth(now), end: endOfMonth(now) };
    }
  };

  const { start: periodStart, end: periodEnd } = getDateRange();

  // Filter services for the selected period
  const periodServices = serviceEpisodes.filter(service => {
    const serviceDate = new Date(service.serviceDate);
    return serviceDate >= periodStart && serviceDate <= periodEnd;
  });

  // Calculate report metrics
  const completedServices = periodServices.filter(s => s.completionStatus === 'COMPLETED');
  const cancelledServices = periodServices.filter(s => s.completionStatus === 'CANCELLED' || s.completionStatus === 'NO_SHOW');
  const totalDuration = completedServices.reduce((sum, s) => sum + (s.actualDurationMinutes || 0), 0);
  const averageDuration = completedServices.length > 0 ? totalDuration / completedServices.length : 0;

  // Service type analysis
  const serviceTypeStats = periodServices.reduce((acc, service) => {
    const type = service.serviceType;
    if (!acc[type]) {
      acc[type] = {
        count: 0,
        completed: 0,
        cancelled: 0,
        totalDuration: 0,
        averageDuration: 0
      };
    }

    acc[type].count++;
    if (service.completionStatus === 'COMPLETED') {
      acc[type].completed++;
      acc[type].totalDuration += (service.actualDurationMinutes || 0);
    }
    if (service.completionStatus === 'CANCELLED' || service.completionStatus === 'NO_SHOW') {
      acc[type].cancelled++;
    }

    acc[type].averageDuration = acc[type].completed > 0 ? acc[type].totalDuration / acc[type].completed : 0;

    return acc;
  }, {} as Record<string, any>);

  // Funding source analysis
  const fundingStats = periodServices.reduce((acc, service) => {
    const funder = service.primaryFundingSource?.funderName || 'Unknown';
    if (!acc[funder]) {
      acc[funder] = {
        count: 0,
        totalHours: 0,
        estimatedCost: 0
      };
    }

    acc[funder].count++;
    if (service.actualDurationMinutes) {
      acc[funder].totalHours += service.actualDurationMinutes / 60;
    }
    if (service.totalBillableAmount) {
      acc[funder].estimatedCost += service.totalBillableAmount;
    }

    return acc;
  }, {} as Record<string, any>);

  // Provider analysis
  const providerStats = periodServices.reduce((acc, service) => {
    const provider = service.primaryProviderName;
    if (!acc[provider]) {
      acc[provider] = {
        count: 0,
        completedCount: 0,
        totalDuration: 0
      };
    }

    acc[provider].count++;
    if (service.completionStatus === 'COMPLETED') {
      acc[provider].completedCount++;
      acc[provider].totalDuration += (service.actualDurationMinutes || 0);
    }

    return acc;
  }, {} as Record<string, any>);

  const exportReport = () => {
    const reportData = {
      period: `${format(periodStart, 'MMM d, yyyy')} - ${format(periodEnd, 'MMM d, yyyy')}`,
      clientId,
      totalServices: periodServices.length,
      completedServices: completedServices.length,
      cancelledServices: cancelledServices.length,
      totalHours: totalDuration / 60,
      averageDuration: averageDuration,
      serviceTypes: serviceTypeStats,
      fundingSources: fundingStats,
      providers: providerStats,
      services: periodServices
    };

    const dataStr = JSON.stringify(reportData, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `service-report-${clientId}-${format(new Date(), 'yyyy-MM-dd')}.json`;
    link.click();
    URL.revokeObjectURL(url);
  };

  const printReport = () => {
    window.print();
  };

  return (
    <div className="space-y-6 print:text-black">
      {/* Report Controls */}
      <Card className="print:hidden">
        <CardHeader>
          <CardTitle>Report Settings</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 items-end">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Report Period
              </label>
              <Select value={reportPeriod} onValueChange={(value: ReportPeriod) => setReportPeriod(value)}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="this_month">This Month</SelectItem>
                  <SelectItem value="last_month">Last Month</SelectItem>
                  <SelectItem value="this_year">This Year</SelectItem>
                  <SelectItem value="custom">Custom Range</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {reportPeriod === 'custom' && (
              <>
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Start Date
                  </label>
                  <Input
                    type="date"
                    value={customStartDate}
                    onChange={(e) => setCustomStartDate(e.target.value)}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    End Date
                  </label>
                  <Input
                    type="date"
                    value={customEndDate}
                    onChange={(e) => setCustomEndDate(e.target.value)}
                  />
                </div>
              </>
            )}

            <div className="flex space-x-2">
              <Button onClick={exportReport} variant="outline" size="sm">
                Export JSON
              </Button>
              <Button onClick={printReport} variant="outline" size="sm">
                Print Report
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Report Header */}
      <Card>
        <CardHeader>
          <CardTitle>Service Delivery Report</CardTitle>
          <div className="text-sm text-slate-600">
            {format(periodStart, 'MMM d, yyyy')} - {format(periodEnd, 'MMM d, yyyy')}
          </div>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div className="text-center">
              <div className="text-2xl font-bold text-blue-600">{periodServices.length}</div>
              <div className="text-sm text-slate-600">Total Services</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-green-600">{completedServices.length}</div>
              <div className="text-sm text-slate-600">Completed</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-orange-600">{(totalDuration / 60).toFixed(1)}</div>
              <div className="text-sm text-slate-600">Total Hours</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-purple-600">{averageDuration.toFixed(0)}</div>
              <div className="text-sm text-slate-600">Avg Minutes</div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Service Type Breakdown */}
      <Card>
        <CardHeader>
          <CardTitle>Service Type Analysis</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b">
                  <th className="text-left py-2">Service Type</th>
                  <th className="text-right py-2">Total</th>
                  <th className="text-right py-2">Completed</th>
                  <th className="text-right py-2">Cancelled</th>
                  <th className="text-right py-2">Completion %</th>
                  <th className="text-right py-2">Avg Duration</th>
                </tr>
              </thead>
              <tbody>
                {Object.entries(serviceTypeStats).map(([type, stats]) => (
                  <tr key={type} className="border-b">
                    <td className="py-2">{type.replace(/_/g, ' ')}</td>
                    <td className="text-right py-2">{stats.count}</td>
                    <td className="text-right py-2">{stats.completed}</td>
                    <td className="text-right py-2">{stats.cancelled}</td>
                    <td className="text-right py-2">
                      {stats.count > 0 ? Math.round((stats.completed / stats.count) * 100) : 0}%
                    </td>
                    <td className="text-right py-2">{Math.round(stats.averageDuration)}min</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      {/* Funding Source Analysis */}
      <Card>
        <CardHeader>
          <CardTitle>Funding Source Analysis</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b">
                  <th className="text-left py-2">Funding Source</th>
                  <th className="text-right py-2">Services</th>
                  <th className="text-right py-2">Total Hours</th>
                  <th className="text-right py-2">Estimated Cost</th>
                </tr>
              </thead>
              <tbody>
                {Object.entries(fundingStats).map(([funder, stats]) => (
                  <tr key={funder} className="border-b">
                    <td className="py-2">{funder}</td>
                    <td className="text-right py-2">{stats.count}</td>
                    <td className="text-right py-2">{stats.totalHours.toFixed(1)}</td>
                    <td className="text-right py-2">
                      {stats.estimatedCost > 0 ? `$${stats.estimatedCost.toFixed(2)}` : 'N/A'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      {/* Provider Analysis */}
      <Card>
        <CardHeader>
          <CardTitle>Provider Performance</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b">
                  <th className="text-left py-2">Provider</th>
                  <th className="text-right py-2">Total Services</th>
                  <th className="text-right py-2">Completed</th>
                  <th className="text-right py-2">Completion %</th>
                  <th className="text-right py-2">Total Hours</th>
                </tr>
              </thead>
              <tbody>
                {Object.entries(providerStats).map(([provider, stats]) => (
                  <tr key={provider} className="border-b">
                    <td className="py-2">{provider}</td>
                    <td className="text-right py-2">{stats.count}</td>
                    <td className="text-right py-2">{stats.completedCount}</td>
                    <td className="text-right py-2">
                      {stats.count > 0 ? Math.round((stats.completedCount / stats.count) * 100) : 0}%
                    </td>
                    <td className="text-right py-2">{(stats.totalDuration / 60).toFixed(1)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      {/* Service Details */}
      <Card>
        <CardHeader>
          <CardTitle>Service Details</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            {periodServices.length === 0 ? (
              <div className="text-center text-slate-600 py-8">
                No services found for the selected period
              </div>
            ) : (
              periodServices.map((service) => (
                <div key={service.episodeId} className="p-3 border border-slate-200 rounded-lg">
                  <div className="flex items-start justify-between">
                    <div>
                      <div className="font-medium text-sm">
                        {service.serviceType.replace(/_/g, ' ')}
                      </div>
                      <div className="text-xs text-slate-600">
                        {format(new Date(service.serviceDate), 'MMM d, yyyy')} • {service.primaryProviderName}
                      </div>
                      <div className="text-xs text-slate-600">
                        {service.deliveryMode} • {service.actualDurationMinutes || service.plannedDurationMinutes}min
                      </div>
                      {service.serviceOutcome && (
                        <div className="text-xs text-slate-700 mt-1">
                          Outcome: {service.serviceOutcome}
                        </div>
                      )}
                    </div>
                    <div className="text-right">
                      <div className={`text-xs px-2 py-1 rounded ${
                        service.completionStatus === 'COMPLETED' ? 'bg-green-100 text-green-800' :
                        service.completionStatus === 'IN_PROGRESS' ? 'bg-yellow-100 text-yellow-800' :
                        service.completionStatus === 'SCHEDULED' ? 'bg-blue-100 text-blue-800' :
                        'bg-gray-100 text-gray-800'
                      }`}>
                        {service.completionStatus}
                      </div>
                      {service.primaryFundingSource && (
                        <div className="text-xs text-slate-600 mt-1">
                          {service.primaryFundingSource.funderName}
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
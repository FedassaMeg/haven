import { useState, useMemo } from 'react';
import Link from 'next/link';
import { ProtectedRoute, useCurrentUser } from '@haven/auth';
import { Card, CardHeader, CardTitle, CardContent, Button, Select, Badge, Table } from '@haven/ui';
import { 
  useServiceEpisodes,
  useBillingStatistics,
  useExportBilling,
  type ServiceEpisode 
} from '@haven/api-client';
import AppLayout from '../../components/AppLayout';

interface BillingRecord {
  id: string;
  episodeId: string;
  clientName: string;
  serviceType: string;
  providerId: string;
  providerName: string;
  startDate: string;
  duration: number;
  billableAmount: number;
  fundingSource: string;
  status: 'PENDING' | 'SUBMITTED' | 'PAID' | 'REJECTED';
  submittedAt?: string;
  paidAt?: string;
}

interface BillingMetrics {
  totalBillable: number;
  totalPaid: number;
  totalPending: number;
  totalSubmitted: number;
  totalRejected: number;
  averageRate: number;
  totalHours: number;
  reimbursementRate: number;
}

const BILLING_STATUS_COLORS = {
  PENDING: 'bg-yellow-100 text-yellow-800',
  SUBMITTED: 'bg-blue-100 text-blue-800',
  PAID: 'bg-green-100 text-green-800',
  REJECTED: 'bg-red-100 text-red-800',
};

const FUNDING_SOURCE_RATES = {
  'HUD-COC': 75.00,
  'VAWA': 95.00,
  'CAL-OES': 85.00,
  'CDBG': 65.00,
  'FOUNDATION': 80.00,
  'DEFAULT': 70.00,
};

function BillingContent() {
  const { user } = useCurrentUser();
  const [dateRange, setDateRange] = useState('month');
  const [statusFilter, setStatusFilter] = useState('all');
  const [fundingFilter, setFundingFilter] = useState('all');
  const [providerFilter, setProviderFilter] = useState(user?.id || 'all');
  
  const { exportBilling, loading: exportLoading } = useExportBilling();

  // Calculate date range
  const searchParams = useMemo(() => {
    const now = new Date();
    const ranges = {
      week: 7,
      month: 30,
      quarter: 90,
      year: 365
    };
    
    const startDate = new Date(now.getTime() - ranges[dateRange as keyof typeof ranges] * 24 * 60 * 60 * 1000);
    
    return {
      startDateFrom: startDate.toISOString(),
      startDateTo: now.toISOString(),
      status: statusFilter !== 'all' ? 'COMPLETED' : undefined,
      providerId: providerFilter !== 'all' ? providerFilter : undefined,
      page: 0,
      size: 100
    };
  }, [dateRange, statusFilter, providerFilter]);

  const { serviceEpisodes, loading } = useServiceEpisodes(searchParams);
  const { billingStats, loading: statsLoading } = useBillingStatistics();

  // Transform service episodes to billing records
  const billingRecords = useMemo((): BillingRecord[] => {
    if (!serviceEpisodes) return [];
    
    return serviceEpisodes
      .filter(episode => episode.status === 'COMPLETED' && episode.billableAmount && episode.billableAmount > 0)
      .map(episode => {
        const fundingSource = episode.fundingSources?.[0]?.funderName || 'DEFAULT';
        const billingStatus = episode.billingStatus || 'PENDING';
        
        return {
          id: `bill-${episode.id}`,
          episodeId: episode.id,
          clientName: episode.clientName,
          serviceType: episode.serviceType,
          providerId: episode.primaryProviderId,
          providerName: episode.primaryProviderName,
          startDate: episode.startTime || episode.createdAt,
          duration: episode.actualDurationMinutes || 0,
          billableAmount: episode.billableAmount || 0,
          fundingSource,
          status: billingStatus as BillingRecord['status'],
          submittedAt: episode.billingSubmittedAt,
          paidAt: episode.billingPaidAt,
        };
      });
  }, [serviceEpisodes]);

  // Filter billing records
  const filteredRecords = useMemo(() => {
    return billingRecords.filter(record => {
      if (statusFilter !== 'all' && record.status !== statusFilter) return false;
      if (fundingFilter !== 'all' && !record.fundingSource.includes(fundingFilter)) return false;
      return true;
    });
  }, [billingRecords, statusFilter, fundingFilter]);

  // Calculate metrics
  const metrics = useMemo((): BillingMetrics => {
    const totalBillable = filteredRecords.reduce((sum, r) => sum + r.billableAmount, 0);
    const totalPaid = filteredRecords.filter(r => r.status === 'PAID').reduce((sum, r) => sum + r.billableAmount, 0);
    const totalPending = filteredRecords.filter(r => r.status === 'PENDING').reduce((sum, r) => sum + r.billableAmount, 0);
    const totalSubmitted = filteredRecords.filter(r => r.status === 'SUBMITTED').reduce((sum, r) => sum + r.billableAmount, 0);
    const totalRejected = filteredRecords.filter(r => r.status === 'REJECTED').reduce((sum, r) => sum + r.billableAmount, 0);
    const totalHours = filteredRecords.reduce((sum, r) => sum + r.duration, 0) / 60;
    const averageRate = totalHours > 0 ? totalBillable / totalHours : 0;
    const reimbursementRate = totalBillable > 0 ? (totalPaid / totalBillable) * 100 : 0;

    return {
      totalBillable,
      totalPaid,
      totalPending,
      totalSubmitted,
      totalRejected,
      averageRate,
      totalHours,
      reimbursementRate,
    };
  }, [filteredRecords]);

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(amount);
  };

  const formatDuration = (minutes: number) => {
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    if (hours > 0) {
      return `${hours}h ${mins}m`;
    }
    return `${mins}m`;
  };

  const handleExportBilling = async () => {
    try {
      await exportBilling({
        ...searchParams,
        format: 'csv',
        includeDetails: true,
      });
    } catch (error) {
      console.error('Failed to export billing:', error);
      alert('Failed to export billing data. Please try again.');
    }
  };

  const columns = [
    {
      header: 'Date',
      accessorKey: 'startDate',
      cell: ({ row }: any) => new Date(row.original.startDate).toLocaleDateString(),
    },
    {
      header: 'Client',
      accessorKey: 'clientName',
      cell: ({ row }: any) => (
        <div>
          <div className="font-medium text-secondary-900">{row.original.clientName}</div>
          <div className="text-sm text-secondary-500">{row.original.serviceType}</div>
        </div>
      ),
    },
    {
      header: 'Provider',
      accessorKey: 'providerName',
      cell: ({ row }: any) => (
        <div className="text-sm text-secondary-900">{row.original.providerName}</div>
      ),
    },
    {
      header: 'Duration',
      accessorKey: 'duration',
      cell: ({ row }: any) => formatDuration(row.original.duration),
    },
    {
      header: 'Amount',
      accessorKey: 'billableAmount',
      cell: ({ row }: any) => (
        <div className="font-medium text-secondary-900">
          {formatCurrency(row.original.billableAmount)}
        </div>
      ),
    },
    {
      header: 'Funding',
      accessorKey: 'fundingSource',
      cell: ({ row }: any) => (
        <Badge variant="outline" className="text-xs">
          {row.original.fundingSource}
        </Badge>
      ),
    },
    {
      header: 'Status',
      accessorKey: 'status',
      cell: ({ row }: any) => (
        <Badge className={BILLING_STATUS_COLORS[row.original.status as keyof typeof BILLING_STATUS_COLORS]}>
          {row.original.status}
        </Badge>
      ),
    },
    {
      header: 'Actions',
      id: 'actions',
      cell: ({ row }: any) => (
        <div className="flex items-center space-x-2">
          <Link href={`/services/${row.original.episodeId}`}>
            <Button variant="outline" size="sm">View</Button>
          </Link>
          {row.original.status === 'PENDING' && (
            <Button size="sm">Submit</Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-secondary-900">Billing & Revenue</h1>
          <p className="text-secondary-600">Track service billing, reimbursements, and financial performance</p>
        </div>
        <div className="flex items-center space-x-3">
          <Button 
            variant="outline" 
            onClick={handleExportBilling} 
            loading={exportLoading}
          >
            <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-4-4m4 4l4-4m-6 4h8a2 2 0 002-2V5a2 2 0 00-2-2H6a2 2 0 00-2-2v14a2 2 0 002 2z" />
            </svg>
            Export
          </Button>
          <Link href="/billing/reports">
            <Button>
              <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
              </svg>
              Reports
            </Button>
          </Link>
        </div>
      </div>

      {/* Key Metrics */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-secondary-600">Total Billable</p>
                <p className="text-2xl font-bold text-secondary-900">
                  {formatCurrency(metrics.totalBillable)}
                </p>
              </div>
              <div className="w-8 h-8 bg-blue-100 rounded-lg flex items-center justify-center">
                <svg className="w-4 h-4 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1" />
                </svg>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-secondary-600">Paid</p>
                <p className="text-2xl font-bold text-green-600">
                  {formatCurrency(metrics.totalPaid)}
                </p>
              </div>
              <div className="w-8 h-8 bg-green-100 rounded-lg flex items-center justify-center">
                <svg className="w-4 h-4 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-secondary-600">Pending</p>
                <p className="text-2xl font-bold text-yellow-600">
                  {formatCurrency(metrics.totalPending)}
                </p>
              </div>
              <div className="w-8 h-8 bg-yellow-100 rounded-lg flex items-center justify-center">
                <svg className="w-4 h-4 text-yellow-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-secondary-600">Reimbursement Rate</p>
                <p className="text-2xl font-bold text-purple-600">
                  {Math.round(metrics.reimbursementRate)}%
                </p>
              </div>
              <div className="w-8 h-8 bg-purple-100 rounded-lg flex items-center justify-center">
                <svg className="w-4 h-4 text-purple-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
                </svg>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Status Breakdown */}
      <Card>
        <CardHeader>
          <CardTitle>Billing Status Overview</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="text-center p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
              <p className="text-2xl font-bold text-yellow-700">{formatCurrency(metrics.totalPending)}</p>
              <p className="text-sm text-yellow-600">Pending</p>
              <p className="text-xs text-yellow-500">
                {filteredRecords.filter(r => r.status === 'PENDING').length} records
              </p>
            </div>
            
            <div className="text-center p-4 bg-blue-50 border border-blue-200 rounded-lg">
              <p className="text-2xl font-bold text-blue-700">{formatCurrency(metrics.totalSubmitted)}</p>
              <p className="text-sm text-blue-600">Submitted</p>
              <p className="text-xs text-blue-500">
                {filteredRecords.filter(r => r.status === 'SUBMITTED').length} records
              </p>
            </div>
            
            <div className="text-center p-4 bg-green-50 border border-green-200 rounded-lg">
              <p className="text-2xl font-bold text-green-700">{formatCurrency(metrics.totalPaid)}</p>
              <p className="text-sm text-green-600">Paid</p>
              <p className="text-xs text-green-500">
                {filteredRecords.filter(r => r.status === 'PAID').length} records
              </p>
            </div>
            
            <div className="text-center p-4 bg-red-50 border border-red-200 rounded-lg">
              <p className="text-2xl font-bold text-red-700">{formatCurrency(metrics.totalRejected)}</p>
              <p className="text-sm text-red-600">Rejected</p>
              <p className="text-xs text-red-500">
                {filteredRecords.filter(r => r.status === 'REJECTED').length} records
              </p>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Filters */}
      <Card>
        <CardHeader>
          <CardTitle>Filters</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div>
              <label className="block text-sm font-medium text-secondary-700 mb-1">Date Range</label>
              <Select
                value={dateRange}
                onChange={setDateRange}
                options={[
                  { value: 'week', label: 'Last 7 days' },
                  { value: 'month', label: 'Last 30 days' },
                  { value: 'quarter', label: 'Last 90 days' },
                  { value: 'year', label: 'Last year' },
                ]}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-secondary-700 mb-1">Status</label>
              <Select
                value={statusFilter}
                onChange={setStatusFilter}
                options={[
                  { value: 'all', label: 'All Statuses' },
                  { value: 'PENDING', label: 'Pending' },
                  { value: 'SUBMITTED', label: 'Submitted' },
                  { value: 'PAID', label: 'Paid' },
                  { value: 'REJECTED', label: 'Rejected' },
                ]}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-secondary-700 mb-1">Funding Source</label>
              <Select
                value={fundingFilter}
                onChange={setFundingFilter}
                options={[
                  { value: 'all', label: 'All Sources' },
                  { value: 'HUD', label: 'HUD/COC' },
                  { value: 'VAWA', label: 'VAWA' },
                  { value: 'CAL-OES', label: 'Cal OES' },
                  { value: 'CDBG', label: 'CDBG' },
                  { value: 'FOUNDATION', label: 'Foundation' },
                ]}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-secondary-700 mb-1">Provider</label>
              <Select
                value={providerFilter}
                onChange={setProviderFilter}
                options={[
                  { value: 'all', label: 'All Providers' },
                  { value: user?.id || '', label: 'My Services' },
                ]}
              />
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Additional Metrics */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card>
          <CardHeader>
            <CardTitle>Hourly Metrics</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              <div className="flex justify-between">
                <span className="text-sm text-secondary-600">Total Hours</span>
                <span className="font-medium">{metrics.totalHours.toFixed(1)}h</span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm text-secondary-600">Average Rate</span>
                <span className="font-medium">{formatCurrency(metrics.averageRate)}/hr</span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm text-secondary-600">Billable Hours</span>
                <span className="font-medium">{(metrics.totalBillable / metrics.averageRate).toFixed(1)}h</span>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>This Month</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              <div className="flex justify-between">
                <span className="text-sm text-secondary-600">Services</span>
                <span className="font-medium">{filteredRecords.length}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm text-secondary-600">Revenue</span>
                <span className="font-medium">{formatCurrency(metrics.totalBillable)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm text-secondary-600">Collection Rate</span>
                <span className="font-medium">{Math.round(metrics.reimbursementRate)}%</span>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Performance</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              <div className="flex justify-between">
                <span className="text-sm text-secondary-600">Avg Service Value</span>
                <span className="font-medium">
                  {formatCurrency(filteredRecords.length > 0 ? metrics.totalBillable / filteredRecords.length : 0)}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm text-secondary-600">Processing Time</span>
                <span className="font-medium">5.2 days</span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm text-secondary-600">Rejection Rate</span>
                <span className="font-medium">
                  {filteredRecords.length > 0 
                    ? Math.round((filteredRecords.filter(r => r.status === 'REJECTED').length / filteredRecords.length) * 100)
                    : 0}%
                </span>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Billing Records Table */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>Billing Records ({filteredRecords.length})</CardTitle>
            <Button variant="outline" onClick={() => window.location.reload()}>
              <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
              Refresh
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex items-center justify-center py-8">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
            </div>
          ) : filteredRecords.length > 0 ? (
            <Table
              data={filteredRecords}
              columns={columns}
            />
          ) : (
            <div className="text-center py-8">
              <svg className="w-12 h-12 text-secondary-400 mx-auto mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1" />
              </svg>
              <h3 className="text-lg font-medium text-secondary-900 mb-1">No billing records found</h3>
              <p className="text-secondary-600">No billable services match your current filters.</p>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

export default function BillingPage() {
  return (
    <ProtectedRoute>
      <AppLayout 
        title="Billing & Revenue" 
        breadcrumbs={[
          { label: 'Dashboard', href: '/dashboard' },
          { label: 'Billing' }
        ]}
      >
        <div className="p-6">
          <BillingContent />
        </div>
      </AppLayout>
    </ProtectedRoute>
  );
}
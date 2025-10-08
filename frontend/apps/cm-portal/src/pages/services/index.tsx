import { useState, useEffect } from 'react';
import Link from 'next/link';
import { ProtectedRoute, useCurrentUser } from '@haven/auth';
import { Card, CardHeader, CardTitle, CardContent, Button, Input, Select, Badge, Table, DatePicker, Checkbox } from '@haven/ui';
import { 
  useServiceEpisodes, 
  useServiceStatistics,
  useServiceTypes,
  useServiceDeliveryModes,
  type ServiceEpisode,
  type ServiceSearchCriteria 
} from '@haven/api-client';
import AppLayout from '../../components/AppLayout';

const SERVICE_STATUS_COLORS = {
  CREATED: 'bg-gray-100 text-gray-800',
  IN_PROGRESS: 'bg-blue-100 text-blue-800',
  COMPLETED: 'bg-green-100 text-green-800',
  CANCELLED: 'bg-red-100 text-red-800',
  ON_HOLD: 'bg-yellow-100 text-yellow-800',
};

const SERVICE_CATEGORY_FILTERS = [
  { value: '', label: 'All Categories' },
  { value: 'CRISIS_RESPONSE', label: 'Crisis Response' },
  { value: 'DV_SPECIFIC', label: 'Domestic Violence' },
  { value: 'SA_SPECIFIC', label: 'Sexual Assault' },
  { value: 'COUNSELING', label: 'Counseling' },
  { value: 'CASE_MANAGEMENT', label: 'Case Management' },
  { value: 'LEGAL_ADVOCACY', label: 'Legal Advocacy' },
  { value: 'HOUSING_SERVICES', label: 'Housing Services' },
  { value: 'FINANCIAL_ASSISTANCE', label: 'Financial Assistance' },
  { value: 'SAFETY_PLANNING', label: 'Safety Planning' },
];

const SERVICE_STATUS_FILTERS = [
  { value: '', label: 'All Statuses' },
  { value: 'CREATED', label: 'Created' },
  { value: 'IN_PROGRESS', label: 'In Progress' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'CANCELLED', label: 'Cancelled' },
  { value: 'ON_HOLD', label: 'On Hold' },
];

function ServicesContent() {
  const { user } = useCurrentUser();
  const [searchCriteria, setSearchCriteria] = useState<ServiceSearchCriteria>({
    page: 0,
    size: 20,
    providerId: user?.id,
  });

  const { serviceEpisodes, loading, totalElements, refetch } = useServiceEpisodes(searchCriteria);
  const { statistics } = useServiceStatistics();
  const { serviceTypes } = useServiceTypes();
  const { deliveryModes } = useServiceDeliveryModes();

  const [filters, setFilters] = useState({
    clientName: '',
    serviceCategory: '',
    status: '',
    serviceType: '',
    deliveryMode: '',
    programName: '',
    providerId: '',
    dateRange: 'last-30-days',
    startDate: '',
    endDate: '',
    minDuration: '',
    maxDuration: '',
    minAmount: '',
    maxAmount: '',
    hasOutcome: false,
    showMyServicesOnly: true,
  });

  const [showAdvanced, setShowAdvanced] = useState(false);

  useEffect(() => {
    const newCriteria: ServiceSearchCriteria = {
      page: searchCriteria.page,
      size: searchCriteria.size,
      clientName: filters.clientName || undefined,
      serviceCategory: filters.serviceCategory || undefined,
      status: filters.status || undefined,
      serviceType: filters.serviceType || undefined,
      deliveryMode: filters.deliveryMode || undefined,
      programName: filters.programName || undefined,
      providerId: filters.showMyServicesOnly ? user?.id : (filters.providerId || undefined),
    };

    // Handle date filtering
    if (filters.startDate) {
      newCriteria.startDateFrom = filters.startDate;
    } else if (filters.dateRange !== 'all') {
      // Add date range filter
      const now = new Date();
      if (filters.dateRange === 'last-7-days') {
        newCriteria.startDateFrom = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000).toISOString();
      } else if (filters.dateRange === 'last-30-days') {
        newCriteria.startDateFrom = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000).toISOString();
      } else if (filters.dateRange === 'last-90-days') {
        newCriteria.startDateFrom = new Date(now.getTime() - 90 * 24 * 60 * 60 * 1000).toISOString();
      }
    }

    if (filters.endDate) {
      newCriteria.startDateTo = filters.endDate;
    }

    // Duration filtering
    if (filters.minDuration) {
      newCriteria.minDuration = parseInt(filters.minDuration);
    }
    if (filters.maxDuration) {
      newCriteria.maxDuration = parseInt(filters.maxDuration);
    }

    // Amount filtering
    if (filters.minAmount) {
      newCriteria.minBillableAmount = parseFloat(filters.minAmount);
    }
    if (filters.maxAmount) {
      newCriteria.maxBillableAmount = parseFloat(filters.maxAmount);
    }

    // Outcome filtering
    if (filters.hasOutcome) {
      newCriteria.hasOutcome = true;
    }

    setSearchCriteria(newCriteria);
  }, [filters, user?.id, searchCriteria.page, searchCriteria.size]);

  const formatDuration = (minutes?: number) => {
    if (!minutes) return 'Not started';
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    if (hours > 0) {
      return `${hours}h ${mins}m`;
    }
    return `${mins}m`;
  };

  const formatCurrency = (amount?: number) => {
    if (!amount) return '-';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(amount);
  };

  const columns = [
    {
      header: 'Client',
      accessorKey: 'clientName',
      cell: ({ row }: any) => (
        <div>
          <div className="font-medium text-secondary-900">{row.original.clientName}</div>
          <div className="text-sm text-secondary-500">ID: {row.original.clientId.slice(0, 8)}</div>
        </div>
      ),
    },
    {
      header: 'Service',
      accessorKey: 'serviceType',
      cell: ({ row }: any) => (
        <div>
          <div className="font-medium text-secondary-900">{row.original.serviceType}</div>
          <div className="text-sm text-secondary-500">{row.original.deliveryMode}</div>
        </div>
      ),
    },
    {
      header: 'Program',
      accessorKey: 'programName',
      cell: ({ row }: any) => (
        <div>
          <div className="text-sm text-secondary-900">{row.original.programName}</div>
          <div className="text-xs text-secondary-500">ID: {row.original.enrollmentId}</div>
        </div>
      ),
    },
    {
      header: 'Status',
      accessorKey: 'status',
      cell: ({ row }: any) => (
        <Badge className={SERVICE_STATUS_COLORS[row.original.status as keyof typeof SERVICE_STATUS_COLORS]}>
          {row.original.status.replace('_', ' ')}
        </Badge>
      ),
    },
    {
      header: 'Duration',
      accessorKey: 'actualDurationMinutes',
      cell: ({ row }: any) => formatDuration(row.original.actualDurationMinutes),
    },
    {
      header: 'Billable Amount',
      accessorKey: 'billableAmount',
      cell: ({ row }: any) => formatCurrency(row.original.billableAmount),
    },
    {
      header: 'Last Updated',
      accessorKey: 'lastModified',
      cell: ({ row }: any) => row.original.lastModified ? new Date(row.original.lastModified).toLocaleDateString() : '-',
    },
    {
      header: 'Actions',
      id: 'actions',
      cell: ({ row }: any) => (
        <div className="flex items-center space-x-2">
          <Link href={`/services/${row.original.id}`}>
            <Button variant="outline" size="sm">View</Button>
          </Link>
          {row.original.status === 'CREATED' && (
            <Link href={`/services/${row.original.id}/start`}>
              <Button size="sm">Start</Button>
            </Link>
          )}
          {row.original.status === 'IN_PROGRESS' && (
            <Link href={`/services/${row.original.id}/track`}>
              <Button variant="outline" size="sm">Track</Button>
            </Link>
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
          <h1 className="text-2xl font-bold text-secondary-900">Service Episodes</h1>
          <p className="text-secondary-600">Manage service delivery episodes with duration tracking and billing</p>
        </div>
        <div className="flex items-center space-x-3">
          <Link href="/services/new">
            <Button>
              <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
              </svg>
              New Service
            </Button>
          </Link>
        </div>
      </div>

      {/* Statistics Cards */}
      {statistics && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-secondary-600">Total Services</p>
                  <p className="text-2xl font-bold text-secondary-900">{statistics.totalServices}</p>
                </div>
                <div className="w-8 h-8 bg-blue-100 rounded-lg flex items-center justify-center">
                  <svg className="w-4 h-4 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                  </svg>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-secondary-600">In Progress</p>
                  <p className="text-2xl font-bold text-secondary-900">{statistics.activeServices}</p>
                </div>
                <div className="w-8 h-8 bg-green-100 rounded-lg flex items-center justify-center">
                  <svg className="w-4 h-4 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
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
                  <p className="text-sm text-secondary-600">Total Hours</p>
                  <p className="text-2xl font-bold text-secondary-900">{Math.round((statistics.totalMinutes || 0) / 60)}</p>
                </div>
                <div className="w-8 h-8 bg-purple-100 rounded-lg flex items-center justify-center">
                  <svg className="w-4 h-4 text-purple-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                  </svg>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-secondary-600">Billable Amount</p>
                  <p className="text-2xl font-bold text-secondary-900">
                    {formatCurrency(statistics.totalBillableAmount)}
                  </p>
                </div>
                <div className="w-8 h-8 bg-yellow-100 rounded-lg flex items-center justify-center">
                  <svg className="w-4 h-4 text-yellow-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1" />
                  </svg>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Filters */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>Search & Filters</CardTitle>
            <div className="flex items-center space-x-3">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setShowAdvanced(!showAdvanced)}
              >
                {showAdvanced ? 'Hide' : 'Show'} Advanced Filters
                <svg className={`w-4 h-4 ml-2 transition-transform ${showAdvanced ? 'rotate-180' : ''}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                </svg>
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setFilters({
                  clientName: '',
                  serviceCategory: '',
                  status: '',
                  serviceType: '',
                  deliveryMode: '',
                  programName: '',
                  providerId: '',
                  dateRange: 'last-30-days',
                  startDate: '',
                  endDate: '',
                  minDuration: '',
                  maxDuration: '',
                  minAmount: '',
                  maxAmount: '',
                  hasOutcome: false,
                  showMyServicesOnly: true,
                })}
              >
                Clear All
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-6">
          {/* Basic Filters */}
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <Input
              label="Client Name"
              placeholder="Search by client name..."
              value={filters.clientName}
              onChange={(e) => setFilters(prev => ({ ...prev, clientName: e.target.value }))}
            />

            <Select
              label="Status"
              value={filters.status}
              onChange={(value) => setFilters(prev => ({ ...prev, status: value }))}
              options={SERVICE_STATUS_FILTERS}
            />

            <Select
              label="Service Category"
              value={filters.serviceCategory}
              onChange={(value) => setFilters(prev => ({ ...prev, serviceCategory: value }))}
              options={SERVICE_CATEGORY_FILTERS}
            />

            <Select
              label="Date Range"
              value={filters.dateRange}
              onChange={(value) => setFilters(prev => ({ ...prev, dateRange: value }))}
              options={[
                { value: 'last-7-days', label: 'Last 7 days' },
                { value: 'last-30-days', label: 'Last 30 days' },
                { value: 'last-90-days', label: 'Last 90 days' },
                { value: 'all', label: 'All time' },
                { value: 'custom', label: 'Custom range' },
              ]}
            />
          </div>

          {/* Quick Options */}
          <div className="flex items-center space-x-6">
            <label className="flex items-center space-x-2">
              <Checkbox
                checked={filters.showMyServicesOnly}
                onChange={(checked) => setFilters(prev => ({ ...prev, showMyServicesOnly: checked }))}
              />
              <span className="text-sm font-medium text-secondary-700">Show only my services</span>
            </label>
            <label className="flex items-center space-x-2">
              <Checkbox
                checked={filters.hasOutcome}
                onChange={(checked) => setFilters(prev => ({ ...prev, hasOutcome: checked }))}
              />
              <span className="text-sm font-medium text-secondary-700">Has outcome recorded</span>
            </label>
          </div>

          {/* Advanced Filters */}
          {showAdvanced && (
            <div className="space-y-4 pt-4 border-t border-secondary-200">
              <h4 className="text-sm font-medium text-secondary-900">Advanced Filters</h4>
              
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <Select
                  label="Service Type"
                  value={filters.serviceType}
                  onChange={(value) => setFilters(prev => ({ ...prev, serviceType: value }))}
                  options={[
                    { value: '', label: 'All Service Types' },
                    ...(serviceTypes?.map(type => ({ value: type.name, label: type.name })) || [])
                  ]}
                />

                <Select
                  label="Delivery Mode"
                  value={filters.deliveryMode}
                  onChange={(value) => setFilters(prev => ({ ...prev, deliveryMode: value }))}
                  options={[
                    { value: '', label: 'All Delivery Modes' },
                    ...(deliveryModes?.map(mode => ({ value: mode.name, label: mode.name })) || [])
                  ]}
                />

                <Input
                  label="Program Name"
                  placeholder="Search by program..."
                  value={filters.programName}
                  onChange={(e) => setFilters(prev => ({ ...prev, programName: e.target.value }))}
                />
              </div>

              {/* Custom Date Range */}
              {filters.dateRange === 'custom' && (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <Input
                    type="date"
                    label="Start Date"
                    value={filters.startDate}
                    onChange={(e) => setFilters(prev => ({ ...prev, startDate: e.target.value }))}
                  />
                  <Input
                    type="date"
                    label="End Date"
                    value={filters.endDate}
                    onChange={(e) => setFilters(prev => ({ ...prev, endDate: e.target.value }))}
                  />
                </div>
              )}

              {/* Duration and Amount Filters */}
              <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <Input
                  type="number"
                  label="Min Duration (minutes)"
                  placeholder="0"
                  value={filters.minDuration}
                  onChange={(e) => setFilters(prev => ({ ...prev, minDuration: e.target.value }))}
                />
                <Input
                  type="number"
                  label="Max Duration (minutes)"
                  placeholder="999"
                  value={filters.maxDuration}
                  onChange={(e) => setFilters(prev => ({ ...prev, maxDuration: e.target.value }))}
                />
                <Input
                  type="number"
                  label="Min Amount ($)"
                  placeholder="0.00"
                  step="0.01"
                  value={filters.minAmount}
                  onChange={(e) => setFilters(prev => ({ ...prev, minAmount: e.target.value }))}
                />
                <Input
                  type="number"
                  label="Max Amount ($)"
                  placeholder="999.99"
                  step="0.01"
                  value={filters.maxAmount}
                  onChange={(e) => setFilters(prev => ({ ...prev, maxAmount: e.target.value }))}
                />
              </div>

              {!filters.showMyServicesOnly && (
                <Input
                  label="Provider ID"
                  placeholder="Filter by specific provider ID..."
                  value={filters.providerId}
                  onChange={(e) => setFilters(prev => ({ ...prev, providerId: e.target.value }))}
                />
              )}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Search Results Summary */}
      {(Object.values(filters).some(v => v !== '' && v !== false && v !== 'last-30-days' && v !== true) || !filters.showMyServicesOnly) && (
        <Card className="bg-blue-50 border-blue-200">
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-3">
                <div className="w-8 h-8 bg-blue-100 rounded-lg flex items-center justify-center">
                  <svg className="w-4 h-4 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                </div>
                <div>
                  <p className="text-sm font-medium text-blue-900">
                    {totalElements} service episodes found
                  </p>
                  <p className="text-xs text-blue-700">
                    Filters active: {
                      [
                        filters.clientName && 'Client Name',
                        filters.status && 'Status',
                        filters.serviceCategory && 'Category',
                        filters.serviceType && 'Service Type',
                        filters.deliveryMode && 'Delivery Mode',
                        filters.programName && 'Program',
                        filters.dateRange !== 'last-30-days' && 'Date Range',
                        filters.minDuration && 'Min Duration',
                        filters.maxDuration && 'Max Duration',
                        filters.minAmount && 'Min Amount',
                        filters.maxAmount && 'Max Amount',
                        filters.hasOutcome && 'Has Outcome',
                        !filters.showMyServicesOnly && 'All Providers'
                      ].filter(Boolean).join(', ') || 'None'
                    }
                  </p>
                </div>
              </div>
              <div className="flex items-center space-x-2">
                <Button variant="outline" size="sm">
                  <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                  </svg>
                  Export Results
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Services Table */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>Service Episodes ({totalElements || 0})</CardTitle>
            <div className="flex items-center space-x-2">
              <Select
                value={searchCriteria.size.toString()}
                onChange={(value) => setSearchCriteria(prev => ({ ...prev, size: parseInt(value), page: 0 }))}
                options={[
                  { value: '10', label: '10 per page' },
                  { value: '20', label: '20 per page' },
                  { value: '50', label: '50 per page' },
                  { value: '100', label: '100 per page' },
                ]}
              />
              <Button variant="outline" onClick={() => refetch()}>
                <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
                Refresh
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex items-center justify-center py-8">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
            </div>
          ) : serviceEpisodes?.length ? (
            <Table
              data={serviceEpisodes}
              columns={columns}
            />
          ) : (
            <div className="text-center py-8">
              <svg className="w-12 h-12 text-secondary-400 mx-auto mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
              </svg>
              <h3 className="text-lg font-medium text-secondary-900 mb-1">No service episodes found</h3>
              <p className="text-secondary-600 mb-4">Get started by creating a new service episode.</p>
              <Link href="/services/new">
                <Button>Create Service Episode</Button>
              </Link>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Pagination */}
      {serviceEpisodes?.length && totalElements && totalElements > 20 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-secondary-600">
            Showing {searchCriteria.page * searchCriteria.size + 1} to{' '}
            {Math.min((searchCriteria.page + 1) * searchCriteria.size, totalElements)} of{' '}
            {totalElements} results
          </p>
          <div className="flex items-center space-x-2">
            <Button
              variant="outline"
              onClick={() => setSearchCriteria(prev => ({ ...prev, page: Math.max(0, prev.page - 1) }))}
              disabled={searchCriteria.page === 0}
            >
              Previous
            </Button>
            <Button
              variant="outline"
              onClick={() => setSearchCriteria(prev => ({ ...prev, page: prev.page + 1 }))}
              disabled={(searchCriteria.page + 1) * searchCriteria.size >= totalElements}
            >
              Next
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}

export default function ServicesPage() {
  return (
    <ProtectedRoute>
      <AppLayout 
        title="Service Episodes" 
        breadcrumbs={[
          { label: 'Dashboard', href: '/dashboard' },
          { label: 'Services' }
        ]}
      >
        <div className="p-6">
          <ServicesContent />
        </div>
      </AppLayout>
    </ProtectedRoute>
  );
}
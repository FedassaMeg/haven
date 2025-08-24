import { useState } from 'react';
import Link from 'next/link';
import { ProtectedRoute } from '@haven/auth';
import { Card, CardHeader, CardTitle, CardContent, Table, Button, Input, Badge, EmptyState, Select } from '@haven/ui';
import { useCases, type Case } from '@haven/api-client';
import AppLayout from '../../components/AppLayout';

function CasesContent() {
  const [filters, setFilters] = useState({
    assigneeId: '',
    status: '',
    requiresAttention: false,
  });
  
  const { cases, loading, error, refetch } = useCases({
    assigneeId: filters.assigneeId || undefined,
    activeOnly: filters.status === 'ACTIVE',
    requiresAttention: filters.requiresAttention,
  });

  const columns = [
    {
      key: 'caseNumber' as const,
      label: 'Case #',
      render: (value: string, case_: Case) => (
        <Link href={`/cases/${case_.id}`} className="text-primary-600 hover:text-primary-700 font-mono font-medium">
          #{case_.caseNumber || case_.id.slice(0, 8)}
        </Link>
      ),
    },
    {
      key: 'clientName' as const,
      label: 'Client',
      render: (value: string, case_: Case) => (
        <div>
          <Link href={`/clients/${case_.clientId}`} className="text-primary-600 hover:text-primary-700 font-medium">
            {case_.clientName || 'Unknown Client'}
          </Link>
          <p className="text-xs text-secondary-500">ID: {case_.clientId?.slice(0, 8)}</p>
        </div>
      ),
    },
    {
      key: 'status' as const,
      label: 'Status',
      render: (value: string) => {
        const statusConfig = {
          OPEN: { variant: 'warning' as const, label: 'Open' },
          IN_PROGRESS: { variant: 'primary' as const, label: 'In Progress' },
          NEEDS_ATTENTION: { variant: 'destructive' as const, label: 'Needs Attention' },
          CLOSED: { variant: 'secondary' as const, label: 'Closed' },
        };
        const config = statusConfig[value as keyof typeof statusConfig] || { variant: 'secondary' as const, label: value };
        return <Badge variant={config.variant}>{config.label}</Badge>;
      },
    },
    {
      key: 'riskLevel' as const,
      label: 'Risk Level',
      render: (value: string) => {
        const riskConfig = {
          LOW: { variant: 'success' as const, bg: 'bg-green-100 text-green-800' },
          MODERATE: { variant: 'warning' as const, bg: 'bg-yellow-100 text-yellow-800' },
          HIGH: { variant: 'destructive' as const, bg: 'bg-red-100 text-red-800' },
          CRITICAL: { variant: 'destructive' as const, bg: 'bg-red-200 text-red-900' },
        };
        const config = riskConfig[value as keyof typeof riskConfig] || { variant: 'secondary' as const, bg: 'bg-gray-100 text-gray-800' };
        return (
          <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${config.bg}`}>
            {value}
          </span>
        );
      },
    },
    {
      key: 'assignee' as const,
      label: 'Assigned To',
      render: (value: any, case_: Case) => (
        <div className="text-sm">
          <p className="font-medium text-secondary-900">{case_.assignment?.assigneeName || 'Unassigned'}</p>
          {case_.assignment?.assignedAt && (
            <p className="text-xs text-secondary-500">
              Since: {new Date(case_.assignment.assignedAt).toLocaleDateString()}
            </p>
          )}
        </div>
      ),
    },
    {
      key: 'lastActivity' as const,
      label: 'Last Activity',
      render: (value: string, case_: Case) => (
        <div className="text-sm">
          <p className="text-secondary-900">
            {case_.lastActivityDate ? new Date(case_.lastActivityDate).toLocaleDateString() : 'No activity'}
          </p>
          <p className="text-xs text-secondary-500">{case_.lastActivityType || ''}</p>
        </div>
      ),
    },
    {
      key: 'actions' as const,
      label: '',
      width: '120px',
      render: (value: any, case_: Case) => (
        <div className="flex space-x-2">
          <Link href={`/cases/${case_.id}`}>
            <Button size="sm" variant="outline">View</Button>
          </Link>
          <Link href={`/cases/${case_.id}/notes`}>
            <Button size="sm" variant="ghost">Notes</Button>
          </Link>
        </div>
      ),
    },
  ];

  const handleFilterChange = (key: string, value: any) => {
    setFilters(prev => ({ ...prev, [key]: value }));
  };

  const attentionCases = cases?.filter(c => c.requiresAttention) || [];
  const highRiskCases = cases?.filter(c => c.riskLevel === 'HIGH' || c.riskLevel === 'CRITICAL') || [];

  return (
    <div className="space-y-6">
      {/* Header Actions */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-secondary-900">Case Management</h1>
          <p className="text-secondary-600">Track and manage active cases</p>
        </div>
        <div className="flex items-center space-x-3">
          <Button onClick={() => refetch()} variant="outline">
            <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            Refresh
          </Button>
          <Link href="/cases/new">
            <Button>
              <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
              </svg>
              Open New Case
            </Button>
          </Link>
        </div>
      </div>

      {/* Quick Stats */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center">
              <div className="p-2 bg-primary-100 rounded-lg">
                <svg className="w-6 h-6 text-primary-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-secondary-600">Total Cases</p>
                <p className="text-2xl font-bold text-secondary-900">{cases?.length || 0}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center">
              <div className="p-2 bg-amber-100 rounded-lg">
                <svg className="w-6 h-6 text-amber-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
                </svg>
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-secondary-600">Need Attention</p>
                <p className="text-2xl font-bold text-secondary-900">{attentionCases.length}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center">
              <div className="p-2 bg-red-100 rounded-lg">
                <svg className="w-6 h-6 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
                </svg>
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-secondary-600">High Risk</p>
                <p className="text-2xl font-bold text-secondary-900">{highRiskCases.length}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center">
              <div className="p-2 bg-green-100 rounded-lg">
                <svg className="w-6 h-6 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-secondary-600">On Track</p>
                <p className="text-2xl font-bold text-secondary-900">
                  {cases?.filter(c => !c.requiresAttention && c.status !== 'CLOSED').length || 0}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Filters */}
      <Card>
        <CardHeader>
          <CardTitle>Search & Filter Cases</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <Select
              label="Status"
              value={filters.status}
              onChange={(value) => handleFilterChange('status', value)}
              options={[
                { value: '', label: 'All Statuses' },
                { value: 'ACTIVE', label: 'Active Only' },
                { value: 'OPEN', label: 'Open' },
                { value: 'IN_PROGRESS', label: 'In Progress' },
                { value: 'CLOSED', label: 'Closed' },
              ]}
            />
            <Select
              label="Assigned To"
              value={filters.assigneeId}
              onChange={(value) => handleFilterChange('assigneeId', value)}
              options={[
                { value: '', label: 'All Workers' },
                { value: 'me', label: 'My Cases' },
                { value: 'unassigned', label: 'Unassigned' },
              ]}
            />
            <div className="flex items-end">
              <label className="flex items-center space-x-2 pt-6">
                <input
                  type="checkbox"
                  checked={filters.requiresAttention}
                  onChange={(e) => handleFilterChange('requiresAttention', e.target.checked)}
                  className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-secondary-300 rounded"
                />
                <span className="text-sm font-medium text-secondary-700">
                  Requires attention only
                </span>
              </label>
            </div>
            <div className="flex items-end space-x-2">
              <Link href="/cases/attention">
                <Button variant="outline" size="sm" className="bg-amber-50 border-amber-200 text-amber-700 hover:bg-amber-100">
                  Priority Cases
                </Button>
              </Link>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Cases Table */}
      {error ? (
        <Card>
          <CardContent className="text-center py-12">
            <div className="text-error-600 mb-4">
              <svg className="w-12 h-12 mx-auto mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
              </svg>
              <p className="text-lg font-medium">Error loading cases</p>
              <p className="text-sm text-secondary-600">{error}</p>
            </div>
            <Button onClick={() => refetch()}>Try Again</Button>
          </CardContent>
        </Card>
      ) : cases && cases.length === 0 && !loading ? (
        <Card>
          <CardContent>
            <EmptyState
              icon={
                <svg className="mx-auto h-12 w-12 text-secondary-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
              }
              title="No cases found"
              description="No cases match your current filters"
              action={{
                label: 'Open New Case',
                onClick: () => window.location.href = '/cases/new'
              }}
            />
          </CardContent>
        </Card>
      ) : (
        <Table
          data={cases || []}
          columns={columns}
          loading={loading}
          emptyMessage="No cases found"
        />
      )}
    </div>
  );
}

export default function CasesPage() {
  return (
    <ProtectedRoute>
      <AppLayout 
        title="Cases" 
        breadcrumbs={[
          { label: 'Dashboard', href: '/dashboard' },
          { label: 'Cases' }
        ]}
      >
        <div className="p-6">
          <CasesContent />
        </div>
      </AppLayout>
    </ProtectedRoute>
  );
}
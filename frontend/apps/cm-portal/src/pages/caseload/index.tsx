import { useState } from 'react';
import Link from 'next/link';
import { ProtectedRoute, useCurrentUser } from '@haven/auth';
import { Card, CardHeader, CardTitle, CardContent, Badge, Button, Table, Input, Tabs, TabsList, TabsTrigger, TabsContent } from '@haven/ui';
import { useCaseload, type CaseloadItem } from '@haven/api-client';
import AppLayout from '../../components/AppLayout';

function CaseloadContent() {
  const { user } = useCurrentUser();
  const [activeTab, setActiveTab] = useState('my-cases');
  const [filters, setFilters] = useState({
    stage: '',
    riskLevel: '',
    requiresAttention: false,
    workerId: user?.id || '',
    page: 0,
    size: 20,
  });

  const { caseload, loading, refetch } = useCaseload({
    ...filters,
    workerId: activeTab === 'my-cases' ? user?.id : filters.workerId
  });

  const stageColors = {
    INTAKE: 'bg-blue-100 text-blue-800 border-blue-200',
    ACTIVE: 'bg-green-100 text-green-800 border-green-200',
    HOUSING_SEARCH: 'bg-purple-100 text-purple-800 border-purple-200',
    STABILIZATION: 'bg-indigo-100 text-indigo-800 border-indigo-200',
    EXIT_PLANNING: 'bg-yellow-100 text-yellow-800 border-yellow-200',
    FOLLOW_UP: 'bg-orange-100 text-orange-800 border-orange-200',
    CLOSED: 'bg-gray-100 text-gray-800 border-gray-200',
  };

  const riskColors = {
    CRITICAL: 'bg-red-100 text-red-800 border-red-200',
    HIGH: 'bg-orange-100 text-orange-800 border-orange-200',
    MEDIUM: 'bg-yellow-100 text-yellow-800 border-yellow-200',
    LOW: 'bg-blue-100 text-blue-800 border-blue-200',
    STABLE: 'bg-green-100 text-green-800 border-green-200',
  };

  const handleFilterChange = (key: string, value: any) => {
    setFilters(prev => ({
      ...prev,
      [key]: value,
      page: 0, // Reset to first page when filtering
    }));
  };

  const caseloadColumns = [
    {
      key: 'caseNumber' as const,
      label: 'Case #',
      width: '120px',
      render: (value: string, item: CaseloadItem) => (
        <div>
          <div className="font-medium">{value}</div>
          {item.isSafeAtHome && (
            <Badge variant="secondary" className="bg-purple-100 text-purple-800 border-purple-200 text-xs mt-1">
              Safe at Home
            </Badge>
          )}
          {item.dataSystem === 'COMPARABLE_DB' && (
            <Badge variant="secondary" className="bg-amber-100 text-amber-800 border-amber-200 text-xs mt-1">
              Comparable DB
            </Badge>
          )}
        </div>
      ),
    },
    {
      key: 'clientName' as const,
      label: 'Client',
      render: (value: string, item: CaseloadItem) => (
        <div>
          <div className="font-medium">{value}</div>
          {item.programName && (
            <div className="text-sm text-slate-600">{item.programName}</div>
          )}
        </div>
      ),
    },
    {
      key: 'stage' as const,
      label: 'Stage',
      render: (value: string, item: CaseloadItem) => (
        <div>
          <Badge variant="secondary" className={stageColors[value as keyof typeof stageColors]}>
            {value.replace('_', ' ')}
          </Badge>
          <div className="text-xs text-slate-600 mt-1">
            {item.stageDescription}
          </div>
        </div>
      ),
    },
    {
      key: 'riskLevel' as const,
      label: 'Risk',
      width: '100px',
      render: (value: string) => (
        <Badge variant="secondary" className={riskColors[value as keyof typeof riskColors]}>
          {value}
        </Badge>
      ),
    },
    {
      key: 'workerName' as const,
      label: 'Assigned Worker',
      render: (value: string) => value || 'Unassigned',
    },
    {
      key: 'lastServiceDate' as const,
      label: 'Last Contact',
      render: (value: string, item: CaseloadItem) => (
        <div className={item.isOverdue ? 'text-red-600' : ''}>
          {value ? new Date(value).toLocaleDateString() : 'No contact'}
          {item.daysSinceLastContact !== undefined && (
            <div className="text-xs text-slate-600">
              {item.daysSinceLastContact} days ago
            </div>
          )}
        </div>
      ),
    },
    {
      key: 'serviceCount' as const,
      label: 'Services',
      width: '80px',
      render: (value: number) => (
        <div className="text-center font-medium">{value}</div>
      ),
    },
    {
      key: 'activeAlerts' as const,
      label: 'Alerts',
      render: (value: string[], item: CaseloadItem) => (
        <div className="space-y-1">
          {item.requiresAttention && (
            <Badge variant="secondary" className="bg-red-100 text-red-800 border-red-200 text-xs">
              Needs Attention
            </Badge>
          )}
          {item.needsUrgentAttention && (
            <Badge variant="secondary" className="bg-red-100 text-red-800 border-red-200 text-xs">
              Urgent
            </Badge>
          )}
          {value && value.length > 0 && (
            <div className="text-xs text-slate-600">
              {value.slice(0, 2).join(', ')}
              {value.length > 2 && ` +${value.length - 2} more`}
            </div>
          )}
        </div>
      ),
    },
    {
      key: 'actions' as const,
      label: 'Actions',
      width: '120px',
      render: (value: any, item: CaseloadItem) => (
        <div className="flex space-x-2">
          <Link href={`/cases/${item.caseId}`}>
            <Button size="sm" variant="outline">View</Button>
          </Link>
          <Button size="sm" variant="ghost" onClick={() => {/* TODO: Open update modal */}}>
            Update
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Caseload Management</h1>
          <p className="text-slate-600">View and manage cases across enrollment stages</p>
        </div>
        <div className="flex items-center space-x-3">
          <Button onClick={refetch} variant="outline" size="sm">
            <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            Refresh
          </Button>
          <Link href="/caseload/team">
            <Button variant="outline">Team Overview</Button>
          </Link>
        </div>
      </div>

      {/* Tabs */}
      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList className="grid w-full grid-cols-4">
          <TabsTrigger value="my-cases">My Caseload</TabsTrigger>
          <TabsTrigger value="all-cases">All Cases</TabsTrigger>
          <TabsTrigger value="high-risk">High Risk</TabsTrigger>
          <TabsTrigger value="confidential">Confidential</TabsTrigger>
        </TabsList>

        <TabsContent value={activeTab} className="mt-6">

      {/* Summary Stats */}
      {caseload && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-slate-600">Total Cases</p>
                  <p className="text-2xl font-bold text-slate-800">{caseload.totalElements}</p>
                </div>
                <div className="h-8 w-8 bg-blue-200 rounded-full flex items-center justify-center">
                  <svg className="w-4 h-4 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                    <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-slate-600">High Risk</p>
                  <p className="text-2xl font-bold text-red-600">{caseload.highRiskCount}</p>
                </div>
                <div className="h-8 w-8 bg-red-200 rounded-full flex items-center justify-center">
                  <svg className="w-4 h-4 text-red-600" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                  </svg>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-slate-600">Overdue</p>
                  <p className="text-2xl font-bold text-orange-600">{caseload.overdueCount}</p>
                </div>
                <div className="h-8 w-8 bg-orange-200 rounded-full flex items-center justify-center">
                  <svg className="w-4 h-4 text-orange-600" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm.707-10.293a1 1 0 00-1.414-1.414l-3 3a1 1 0 001.414 1.414l2.293-2.293V15a1 1 0 102 0V8a1 1 0 00-.293-.707z" clipRule="evenodd" />
                  </svg>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-slate-600">Active Stages</p>
                  <p className="text-2xl font-bold text-green-600">
                    {Object.values(caseload.stageCounts || {}).reduce((a, b) => a + b, 0)}
                  </p>
                </div>
                <div className="h-8 w-8 bg-green-200 rounded-full flex items-center justify-center">
                  <svg className="w-4 h-4 text-green-600" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M3 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1z" clipRule="evenodd" />
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
          <CardTitle>Filters</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Stage</label>
              <select
                value={filters.stage}
                onChange={(e) => handleFilterChange('stage', e.target.value)}
                className="w-full border border-slate-300 rounded px-3 py-2 text-sm"
              >
                <option value="">All Stages</option>
                <option value="INTAKE">Intake</option>
                <option value="ACTIVE">Active</option>
                <option value="HOUSING_SEARCH">Housing Search</option>
                <option value="STABILIZATION">Stabilization</option>
                <option value="EXIT_PLANNING">Exit Planning</option>
                <option value="FOLLOW_UP">Follow-up</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Risk Level</label>
              <select
                value={filters.riskLevel}
                onChange={(e) => handleFilterChange('riskLevel', e.target.value)}
                className="w-full border border-slate-300 rounded px-3 py-2 text-sm"
              >
                <option value="">All Risk Levels</option>
                <option value="CRITICAL">Critical</option>
                <option value="HIGH">High</option>
                <option value="MEDIUM">Medium</option>
                <option value="LOW">Low</option>
                <option value="STABLE">Stable</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Worker ID</label>
              <Input
                value={filters.workerId}
                onChange={(e) => handleFilterChange('workerId', e.target.value)}
                placeholder="Enter worker ID"
              />
            </div>
            <div className="flex items-end">
              <label className="flex items-center space-x-2">
                <input
                  type="checkbox"
                  checked={filters.requiresAttention}
                  onChange={(e) => handleFilterChange('requiresAttention', e.target.checked)}
                  className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-slate-300 rounded"
                />
                <span className="text-sm font-medium text-slate-700">Requires Attention Only</span>
              </label>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Caseload Table */}
      <Card>
        <CardHeader>
          <CardTitle>Cases</CardTitle>
        </CardHeader>
        <CardContent>
          <Table
            data={caseload?.cases || []}
            columns={caseloadColumns}
            loading={loading}
            emptyMessage="No cases found"
          />
          
          {/* Pagination */}
          {caseload && caseload.totalPages > 1 && (
            <div className="flex items-center justify-between mt-4">
              <div className="text-sm text-slate-600">
                Showing {filters.page * filters.size + 1} to{' '}
                {Math.min((filters.page + 1) * filters.size, caseload.totalElements)} of{' '}
                {caseload.totalElements} cases
              </div>
              <div className="flex space-x-2">
                <Button
                  size="sm"
                  variant="outline"
                  disabled={filters.page === 0}
                  onClick={() => handleFilterChange('page', filters.page - 1)}
                >
                  Previous
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  disabled={filters.page >= caseload.totalPages - 1}
                  onClick={() => handleFilterChange('page', filters.page + 1)}
                >
                  Next
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

        </TabsContent>
      </Tabs>
    </div>
  );
}

export default function CaseloadPage() {
  return (
    <ProtectedRoute>
      <AppLayout 
        title="Caseload Management" 
        breadcrumbs={[
          { label: 'Dashboard', href: '/dashboard' },
          { label: 'Caseload Management' }
        ]}
      >
        <div className="p-6">
          <CaseloadContent />
        </div>
      </AppLayout>
    </ProtectedRoute>
  );
}
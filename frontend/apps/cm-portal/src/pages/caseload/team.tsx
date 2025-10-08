import { useState } from 'react';
import Link from 'next/link';
import { ProtectedRoute, PermissionGuard } from '@haven/auth';
import { Card, CardHeader, CardTitle, CardContent, Badge, Button, Table, Select } from '@haven/ui';
import { useTeamOverview, type WorkerCaseload } from '@haven/api-client';
import AppLayout from '../../components/AppLayout';

function TeamOverviewContent() {
  const [sortBy, setSortBy] = useState('totalCases');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');
  
  const { teamOverview, loading, refetch } = useTeamOverview();

  const sortedWorkers = teamOverview?.workerCaseloads?.sort((a, b) => {
    const aValue = a[sortBy as keyof WorkerCaseload] as number;
    const bValue = b[sortBy as keyof WorkerCaseload] as number;
    return sortOrder === 'asc' ? aValue - bValue : bValue - aValue;
  }) || [];

  const columns = [
    {
      key: 'workerName' as const,
      label: 'Case Manager',
      render: (value: string, worker: WorkerCaseload) => (
        <div>
          <p className="font-medium text-secondary-900">{value || 'Unknown Worker'}</p>
          <p className="text-xs text-secondary-500">ID: {worker.workerId?.slice(0, 8)}</p>
        </div>
      ),
    },
    {
      key: 'totalCases' as const,
      label: 'Total Cases',
      width: '100px',
      render: (value: number, worker: WorkerCaseload) => (
        <div className="text-center">
          <p className="text-lg font-bold text-secondary-900">{value}</p>
          <p className="text-xs text-secondary-500">
            {teamOverview?.averageCaseload ? 
              value > teamOverview.averageCaseload ? '+' + (value - teamOverview.averageCaseload) : 
              value < teamOverview.averageCaseload ? '-' + (teamOverview.averageCaseload - value) : 'avg'
              : ''
            }
          </p>
        </div>
      ),
    },
    {
      key: 'intakeCases' as const,
      label: 'Intake',
      width: '80px',
      render: (value: number) => (
        <div className="text-center">
          <Badge variant="outline" className="bg-blue-50 text-blue-700 border-blue-200">
            {value}
          </Badge>
        </div>
      ),
    },
    {
      key: 'activeCases' as const,
      label: 'Active',
      width: '80px',
      render: (value: number) => (
        <div className="text-center">
          <Badge variant="outline" className="bg-green-50 text-green-700 border-green-200">
            {value}
          </Badge>
        </div>
      ),
    },
    {
      key: 'housingSearchCases' as const,
      label: 'Housing',
      width: '80px',
      render: (value: number) => (
        <div className="text-center">
          <Badge variant="outline" className="bg-purple-50 text-purple-700 border-purple-200">
            {value}
          </Badge>
        </div>
      ),
    },
    {
      key: 'highRiskCases' as const,
      label: 'High Risk',
      width: '100px',
      render: (value: number) => (
        <div className="text-center">
          <Badge variant={value > 0 ? 'destructive' : 'secondary'}>
            {value}
          </Badge>
        </div>
      ),
    },
    {
      key: 'requiringAttention' as const,
      label: 'Need Attention',
      width: '120px',
      render: (value: number) => (
        <div className="text-center">
          <Badge variant={value > 0 ? 'warning' : 'secondary'}>
            {value}
          </Badge>
        </div>
      ),
    },
    {
      key: 'workload' as const,
      label: 'Workload Status',
      render: (value: any, worker: WorkerCaseload) => {
        const average = teamOverview?.averageCaseload || 0;
        const total = worker.totalCases;
        
        let status: 'light' | 'normal' | 'heavy' | 'overloaded' = 'normal';
        let color = 'bg-green-100 text-green-800 border-green-200';
        
        if (total === 0) {
          status = 'light';
          color = 'bg-blue-100 text-blue-800 border-blue-200';
        } else if (total < average * 0.8) {
          status = 'light';
          color = 'bg-blue-100 text-blue-800 border-blue-200';
        } else if (total > average * 1.3) {
          status = 'overloaded';
          color = 'bg-red-100 text-red-800 border-red-200';
        } else if (total > average * 1.1) {
          status = 'heavy';
          color = 'bg-yellow-100 text-yellow-800 border-yellow-200';
        }
        
        return (
          <Badge variant="outline" className={color}>
            {status.charAt(0).toUpperCase() + status.slice(1)}
          </Badge>
        );
      },
    },
    {
      key: 'actions' as const,
      label: 'Actions',
      width: '120px',
      render: (value: any, worker: WorkerCaseload) => (
        <div className="flex space-x-2">
          <Link href={`/caseload?workerId=${worker.workerId}`}>
            <Button size="sm" variant="outline">View Cases</Button>
          </Link>
        </div>
      ),
    },
  ];

  const getWorkloadDistribution = () => {
    if (!teamOverview?.workerCaseloads) return { light: 0, normal: 0, heavy: 0, overloaded: 0 };
    
    const average = teamOverview.averageCaseload;
    return teamOverview.workerCaseloads.reduce((acc, worker) => {
      const total = worker.totalCases;
      if (total === 0 || total < average * 0.8) acc.light++;
      else if (total > average * 1.3) acc.overloaded++;
      else if (total > average * 1.1) acc.heavy++;
      else acc.normal++;
      return acc;
    }, { light: 0, normal: 0, heavy: 0, overloaded: 0 });
  };

  const workloadDistribution = getWorkloadDistribution();

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-secondary-900">Team Caseload Overview</h1>
          <p className="text-secondary-600">Monitor team capacity and workload distribution</p>
        </div>
        <div className="flex items-center space-x-3">
          <Button onClick={refetch} variant="outline">
            <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            Refresh
          </Button>
          <Link href="/caseload">
            <Button variant="outline">View My Caseload</Button>
          </Link>
        </div>
      </div>

      {/* Team Summary Stats */}
      {teamOverview && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center">
                <div className="p-2 bg-primary-100 rounded-lg">
                  <svg className="w-6 h-6 text-primary-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                  </svg>
                </div>
                <div className="ml-4">
                  <p className="text-sm font-medium text-secondary-600">Active Workers</p>
                  <p className="text-2xl font-bold text-secondary-900">{teamOverview.workerCaseloads?.length || 0}</p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-4">
              <div className="flex items-center">
                <div className="p-2 bg-green-100 rounded-lg">
                  <svg className="w-6 h-6 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                  </svg>
                </div>
                <div className="ml-4">
                  <p className="text-sm font-medium text-secondary-600">Total Cases</p>
                  <p className="text-2xl font-bold text-secondary-900">{teamOverview.totalActiveCases}</p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-4">
              <div className="flex items-center">
                <div className="p-2 bg-blue-100 rounded-lg">
                  <svg className="w-6 h-6 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                  </svg>
                </div>
                <div className="ml-4">
                  <p className="text-sm font-medium text-secondary-600">Average Caseload</p>
                  <p className="text-2xl font-bold text-secondary-900">{teamOverview.averageCaseload}</p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-4">
              <div className="flex items-center">
                <div className="p-2 bg-amber-100 rounded-lg">
                  <svg className="w-6 h-6 text-amber-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
                <div className="ml-4">
                  <p className="text-sm font-medium text-secondary-600">Overloaded Workers</p>
                  <p className="text-2xl font-bold text-secondary-900">{workloadDistribution.overloaded}</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Main Team Table */}
        <div className="lg:col-span-3">
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle>Team Caseload Distribution</CardTitle>
                <div className="flex items-center space-x-2">
                  <Select
                    value={sortBy}
                    onChange={setSortBy}
                    options={[
                      { value: 'totalCases', label: 'Total Cases' },
                      { value: 'highRiskCases', label: 'High Risk' },
                      { value: 'requiringAttention', label: 'Needs Attention' },
                      { value: 'intakeCases', label: 'Intake Cases' },
                      { value: 'activeCases', label: 'Active Cases' },
                    ]}
                  />
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc')}
                  >
                    {sortOrder === 'asc' ? '↑' : '↓'}
                  </Button>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <Table
                data={sortedWorkers}
                columns={columns}
                loading={loading}
                emptyMessage="No team members found"
              />
            </CardContent>
          </Card>
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Workload Distribution */}
          <Card>
            <CardHeader>
              <CardTitle>Workload Distribution</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <div className="w-3 h-3 bg-blue-500 rounded-full"></div>
                    <span className="text-sm">Light Load</span>
                  </div>
                  <span className="font-medium">{workloadDistribution.light}</span>
                </div>
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <div className="w-3 h-3 bg-green-500 rounded-full"></div>
                    <span className="text-sm">Normal Load</span>
                  </div>
                  <span className="font-medium">{workloadDistribution.normal}</span>
                </div>
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <div className="w-3 h-3 bg-yellow-500 rounded-full"></div>
                    <span className="text-sm">Heavy Load</span>
                  </div>
                  <span className="font-medium">{workloadDistribution.heavy}</span>
                </div>
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <div className="w-3 h-3 bg-red-500 rounded-full"></div>
                    <span className="text-sm">Overloaded</span>
                  </div>
                  <span className="font-medium">{workloadDistribution.overloaded}</span>
                </div>
              </div>
              
              {workloadDistribution.overloaded > 0 && (
                <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg">
                  <p className="text-sm text-red-700 font-medium">
                    ⚠️ {workloadDistribution.overloaded} worker{workloadDistribution.overloaded !== 1 ? 's' : ''} overloaded
                  </p>
                  <p className="text-xs text-red-600 mt-1">
                    Consider case redistribution or additional support
                  </p>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Stage Summary */}
          <Card>
            <CardHeader>
              <CardTitle>Cases by Stage</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-sm">Intake</span>
                  <Badge className="bg-blue-100 text-blue-800">
                    {sortedWorkers.reduce((sum, w) => sum + w.intakeCases, 0)}
                  </Badge>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm">Active</span>
                  <Badge className="bg-green-100 text-green-800">
                    {sortedWorkers.reduce((sum, w) => sum + w.activeCases, 0)}
                  </Badge>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm">Housing Search</span>
                  <Badge className="bg-purple-100 text-purple-800">
                    {sortedWorkers.reduce((sum, w) => sum + w.housingSearchCases, 0)}
                  </Badge>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm">High Risk</span>
                  <Badge className="bg-red-100 text-red-800">
                    {sortedWorkers.reduce((sum, w) => sum + w.highRiskCases, 0)}
                  </Badge>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Quick Actions */}
          <Card>
            <CardHeader>
              <CardTitle>Team Actions</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <Button variant="outline" className="w-full justify-start">
                  <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3a1 1 0 011-1h6a1 1 0 011 1v4h3a1 1 0 110 2h-1v9a2 2 0 01-2 2H7a2 2 0 01-2-2V9H4a1 1 0 110-2h4z" />
                  </svg>
                  Generate Team Report
                </Button>
                <Button variant="outline" className="w-full justify-start">
                  <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                  </svg>
                  Redistribute Cases
                </Button>
                <Button variant="outline" className="w-full justify-start">
                  <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 100 4m0-4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 100 4m0-4v2m0-6V4" />
                  </svg>
                  Capacity Planning
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}

export default function TeamOverviewPage() {
  return (
    <ProtectedRoute>
      <PermissionGuard
        resource="caseload"
        action="view_team"
        fallback={
          <div className="p-6">
            <div className="text-center">
              <h2 className="text-xl font-semibold text-secondary-900 mb-2">Access Restricted</h2>
              <p className="text-secondary-600">You don't have permission to view team caseload information.</p>
            </div>
          </div>
        }
      >
        <AppLayout 
          title="Team Caseload Overview" 
          breadcrumbs={[
            { label: 'Dashboard', href: '/dashboard' },
            { label: 'Caseload', href: '/caseload' },
            { label: 'Team Overview' }
          ]}
        >
          <div className="p-6">
            <TeamOverviewContent />
          </div>
        </AppLayout>
      </PermissionGuard>
    </ProtectedRoute>
  );
}
import { useState } from 'react';
import Link from 'next/link';
import { ProtectedRoute, useCurrentUser } from '@haven/auth';
import { Card, CardHeader, CardTitle, CardContent, Table, Button, Input, Badge, EmptyState, Select, Tabs, TabsList, TabsTrigger, TabsContent } from '@haven/ui';
import { useMandatedReports, type MandatedReport } from '@haven/api-client';
import AppLayout from '../../components/AppLayout';

function MandatedReportsContent() {
  const { user } = useCurrentUser();
  const [activeTab, setActiveTab] = useState('all');
  const [filters, setFilters] = useState({
    status: '',
    reportType: '',
    caseId: '',
  });
  
  const { reports, loading, error, refetch } = useMandatedReports({
    status: filters.status || undefined,
    reportType: filters.reportType || undefined,
    caseId: filters.caseId || undefined,
  });

  const getStatusConfig = (status: string) => {
    const configs = {
      DRAFT: { variant: 'secondary' as const, label: 'Draft', bg: 'bg-gray-100 text-gray-800' },
      PENDING_REVIEW: { variant: 'warning' as const, label: 'Pending Review', bg: 'bg-yellow-100 text-yellow-800' },
      APPROVED: { variant: 'success' as const, label: 'Approved', bg: 'bg-green-100 text-green-800' },
      FILED: { variant: 'primary' as const, label: 'Filed', bg: 'bg-blue-100 text-blue-800' },
      REJECTED: { variant: 'destructive' as const, label: 'Rejected', bg: 'bg-red-100 text-red-800' },
      RESPONSE_RECEIVED: { variant: 'success' as const, label: 'Response Received', bg: 'bg-green-100 text-green-800' },
    };
    return configs[status as keyof typeof configs] || { variant: 'secondary' as const, label: status, bg: 'bg-gray-100 text-gray-800' };
  };

  const getReportTypeConfig = (type: string) => {
    const configs = {
      CPS: { label: 'Child Protective Services', color: 'text-red-700', icon: '👶' },
      APS: { label: 'Adult Protective Services', color: 'text-blue-700', icon: '👥' },
      LAW_ENFORCEMENT: { label: 'Law Enforcement', color: 'text-purple-700', icon: '👮' },
      MEDICAL: { label: 'Medical Report', color: 'text-green-700', icon: '🏥' },
      COURT: { label: 'Court Mandated', color: 'text-gray-700', icon: '⚖️' },
    };
    return configs[type as keyof typeof configs] || { label: type, color: 'text-gray-700', icon: '📄' };
  };

  const columns = [
    {
      key: 'reportNumber' as const,
      label: 'Report #',
      render: (value: string, report: MandatedReport) => (
        <Link href={`/mandated-reports/${report.id}`} className="text-primary-600 hover:text-primary-700 font-mono font-medium">
          #{report.reportNumber || report.id.slice(0, 8)}
        </Link>
      ),
    },
    {
      key: 'reportType' as const,
      label: 'Type',
      render: (value: string) => {
        const config = getReportTypeConfig(value);
        return (
          <div className="flex items-center space-x-2">
            <span className="text-lg">{config.icon}</span>
            <div>
              <p className={`font-medium ${config.color}`}>{config.label}</p>
            </div>
          </div>
        );
      },
    },
    {
      key: 'caseNumber' as const,
      label: 'Case',
      render: (value: string, report: MandatedReport) => (
        <div>
          <Link href={`/cases/${report.caseId}`} className="text-primary-600 hover:text-primary-700 font-medium">
            Case #{report.caseNumber || report.caseId?.slice(0, 8)}
          </Link>
          <p className="text-xs text-secondary-500">
            Client: {report.clientName || 'Unknown'}
          </p>
        </div>
      ),
    },
    {
      key: 'status' as const,
      label: 'Status',
      render: (value: string) => {
        const config = getStatusConfig(value);
        return <Badge variant={config.variant}>{config.label}</Badge>;
      },
    },
    {
      key: 'submittedBy' as const,
      label: 'Reporter',
      render: (value: string, report: MandatedReport) => (
        <div>
          <p className="font-medium text-secondary-900">{report.submittedBy || 'Unknown'}</p>
          <p className="text-xs text-secondary-500">
            {report.submittedAt ? new Date(report.submittedAt).toLocaleDateString() : 'Not submitted'}
          </p>
        </div>
      ),
    },
    {
      key: 'dueDates' as const,
      label: 'Timeline',
      render: (value: any, report: MandatedReport) => {
        const isOverdue = report.dueDate && new Date(report.dueDate) < new Date();
        return (
          <div>
            {report.dueDate && (
              <p className={`text-sm ${isOverdue ? 'text-red-600 font-medium' : 'text-secondary-900'}`}>
                Due: {new Date(report.dueDate).toLocaleDateString()}
              </p>
            )}
            {report.filedAt && (
              <p className="text-xs text-green-600">
                Filed: {new Date(report.filedAt).toLocaleDateString()}
              </p>
            )}
          </div>
        );
      },
    },
    {
      key: 'actions' as const,
      label: '',
      width: '120px',
      render: (value: any, report: MandatedReport) => (
        <div className="flex space-x-2">
          <Link href={`/mandated-reports/${report.id}`}>
            <Button size="sm" variant="outline">View</Button>
          </Link>
          {(report.status === 'DRAFT' || report.status === 'PENDING_REVIEW') && (
            <Link href={`/mandated-reports/${report.id}/edit`}>
              <Button size="sm" variant="ghost">Edit</Button>
            </Link>
          )}
        </div>
      ),
    },
  ];

  const filteredReports = reports?.filter(report => {
    switch (activeTab) {
      case 'pending':
        return ['DRAFT', 'PENDING_REVIEW'].includes(report.status);
      case 'filed':
        return ['FILED', 'RESPONSE_RECEIVED'].includes(report.status);
      case 'overdue':
        return report.dueDate && new Date(report.dueDate) < new Date() && !['FILED', 'RESPONSE_RECEIVED'].includes(report.status);
      default:
        return true;
    }
  }) || [];

  const pendingCount = reports?.filter(r => ['DRAFT', 'PENDING_REVIEW'].includes(r.status)).length || 0;
  const overdueCount = reports?.filter(r => 
    r.dueDate && new Date(r.dueDate) < new Date() && !['FILED', 'RESPONSE_RECEIVED'].includes(r.status)
  ).length || 0;
  const filedCount = reports?.filter(r => ['FILED', 'RESPONSE_RECEIVED'].includes(r.status)).length || 0;

  return (
    <div className="space-y-6">
      {/* Header Actions */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-secondary-900">Mandated Reports</h1>
          <p className="text-secondary-600">Manage mandatory reporting requirements and compliance</p>
        </div>
        <div className="flex items-center space-x-3">
          <Button onClick={() => refetch()} variant="outline">
            <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            Refresh
          </Button>
          <Link href="/mandated-reports/new">
            <Button>
              <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
              </svg>
              Create Report
            </Button>
          </Link>
        </div>
      </div>

      {/* Quick Stats */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center">
              <div className="p-2 bg-amber-100 rounded-lg">
                <svg className="w-6 h-6 text-amber-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-secondary-600">Pending Action</p>
                <p className="text-2xl font-bold text-secondary-900">{pendingCount}</p>
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
                <p className="text-sm font-medium text-secondary-600">Overdue</p>
                <p className="text-2xl font-bold text-secondary-900">{overdueCount}</p>
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
                <p className="text-sm font-medium text-secondary-600">Filed</p>
                <p className="text-2xl font-bold text-secondary-900">{filedCount}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center">
              <div className="p-2 bg-primary-100 rounded-lg">
                <svg className="w-6 h-6 text-primary-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
              </div>
              <div className="ml-4">
                <p className="text-sm font-medium text-secondary-600">Total Reports</p>
                <p className="text-2xl font-bold text-secondary-900">{reports?.length || 0}</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Alert for Overdue Reports */}
      {overdueCount > 0 && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <div className="flex items-center">
            <svg className="w-6 h-6 text-red-600 mr-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
            </svg>
            <div>
              <h3 className="font-medium text-red-800">
                {overdueCount} Overdue Report{overdueCount !== 1 ? 's' : ''}
              </h3>
              <p className="text-sm text-red-700">
                These reports require immediate attention to maintain compliance.
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Tabs */}
      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList className="grid w-full grid-cols-4">
          <TabsTrigger value="all">
            All Reports ({reports?.length || 0})
          </TabsTrigger>
          <TabsTrigger value="pending" className={pendingCount > 0 ? "bg-amber-50" : ""}>
            Pending ({pendingCount})
          </TabsTrigger>
          <TabsTrigger value="overdue" className={overdueCount > 0 ? "bg-red-50" : ""}>
            Overdue ({overdueCount})
          </TabsTrigger>
          <TabsTrigger value="filed">
            Filed ({filedCount})
          </TabsTrigger>
        </TabsList>

        <div className="mt-6">
          {/* Filters */}
          <Card className="mb-6">
            <CardHeader>
              <CardTitle>Search & Filter Reports</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <Select
                  label="Report Type"
                  value={filters.reportType}
                  onChange={(value) => setFilters(prev => ({ ...prev, reportType: value }))}
                  options={[
                    { value: '', label: 'All Types' },
                    { value: 'CPS', label: 'Child Protective Services' },
                    { value: 'APS', label: 'Adult Protective Services' },
                    { value: 'LAW_ENFORCEMENT', label: 'Law Enforcement' },
                    { value: 'MEDICAL', label: 'Medical Report' },
                    { value: 'COURT', label: 'Court Mandated' },
                  ]}
                />
                <Select
                  label="Status"
                  value={filters.status}
                  onChange={(value) => setFilters(prev => ({ ...prev, status: value }))}
                  options={[
                    { value: '', label: 'All Statuses' },
                    { value: 'DRAFT', label: 'Draft' },
                    { value: 'PENDING_REVIEW', label: 'Pending Review' },
                    { value: 'APPROVED', label: 'Approved' },
                    { value: 'FILED', label: 'Filed' },
                    { value: 'REJECTED', label: 'Rejected' },
                    { value: 'RESPONSE_RECEIVED', label: 'Response Received' },
                  ]}
                />
                <Input
                  label="Case ID"
                  placeholder="Enter case ID..."
                  value={filters.caseId}
                  onChange={(e) => setFilters(prev => ({ ...prev, caseId: e.target.value }))}
                />
                <div className="flex items-end">
                  <Button
                    variant="outline"
                    onClick={() => setFilters({ status: '', reportType: '', caseId: '' })}
                    className="w-full"
                  >
                    Clear Filters
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Reports Table */}
          <TabsContent value={activeTab}>
            {error ? (
              <Card>
                <CardContent className="text-center py-12">
                  <div className="text-error-600 mb-4">
                    <svg className="w-12 h-12 mx-auto mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
                    </svg>
                    <p className="text-lg font-medium">Error loading reports</p>
                    <p className="text-sm text-secondary-600">{error}</p>
                  </div>
                  <Button onClick={() => refetch()}>Try Again</Button>
                </CardContent>
              </Card>
            ) : filteredReports.length === 0 && !loading ? (
              <Card>
                <CardContent>
                  <EmptyState
                    icon={
                      <svg className="mx-auto h-12 w-12 text-secondary-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                      </svg>
                    }
                    title={`No ${activeTab === 'all' ? '' : activeTab} reports found`}
                    description={activeTab === 'all' ? 
                      "No mandatory reports have been created yet" : 
                      `No reports match the ${activeTab} criteria`
                    }
                    action={{
                      label: 'Create Report',
                      onClick: () => window.location.href = '/mandated-reports/new'
                    }}
                  />
                </CardContent>
              </Card>
            ) : (
              <Table
                data={filteredReports}
                columns={columns}
                loading={loading}
                emptyMessage="No reports found"
              />
            )}
          </TabsContent>
        </div>
      </Tabs>
    </div>
  );
}

export default function MandatedReportsPage() {
  return (
    <ProtectedRoute>
      <AppLayout 
        title="Mandated Reports" 
        breadcrumbs={[
          { label: 'Dashboard', href: '/dashboard' },
          { label: 'Mandated Reports' }
        ]}
      >
        <div className="p-6">
          <MandatedReportsContent />
        </div>
      </AppLayout>
    </ProtectedRoute>
  );
}
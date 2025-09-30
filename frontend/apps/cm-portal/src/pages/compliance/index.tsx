import { useState } from 'react';
import Link from 'next/link';
import { ProtectedRoute, PermissionGuard } from '@haven/auth';
import { Card, CardHeader, CardTitle, CardContent, Badge, Button, Tabs, TabsList, TabsTrigger, TabsContent, Table, Select } from '@haven/ui';
import { useComplianceOverview, useAuditLog, useHudComplianceSummary, useHudElementsByCategory, type ComplianceMetric, type AuditEntry, type ComplianceSummaryResponse, type HudDataElement } from '@haven/api-client';
import AppLayout from '../../components/AppLayout';

function ComplianceContent() {
  const [activeTab, setActiveTab] = useState('overview');
  const [auditFilters, setAuditFilters] = useState({
    action: '',
    resource: '',
    userId: '',
    startDate: '',
    endDate: '',
  });

  const { overview, refetch } = useComplianceOverview();
  const { auditLog, loading: auditLoading } = useAuditLog(auditFilters);
  const { summary: hudSummary, loading: hudSummaryLoading } = useHudComplianceSummary();
  const { elements: hudElements, loading: hudElementsLoading } = useHudElementsByCategory();

  const getComplianceStatus = (metric: ComplianceMetric) => {
    const percentage = (metric.achieved / metric.target) * 100;
    if (percentage >= 95) return { status: 'excellent', color: 'bg-green-100 text-green-800 border-green-200', label: 'Excellent' };
    if (percentage >= 80) return { status: 'good', color: 'bg-blue-100 text-blue-800 border-blue-200', label: 'Good' };
    if (percentage >= 60) return { status: 'warning', color: 'bg-yellow-100 text-yellow-800 border-yellow-200', label: 'Needs Attention' };
    return { status: 'critical', color: 'bg-red-100 text-red-800 border-red-200', label: 'Critical' };
  };

  const auditColumns = [
    {
      key: 'timestamp' as const,
      label: 'Date/Time',
      width: '150px',
      render: (value: string) => (
        <div>
          <p className="text-sm text-secondary-900">{new Date(value).toLocaleDateString()}</p>
          <p className="text-xs text-secondary-500">{new Date(value).toLocaleTimeString()}</p>
        </div>
      ),
    },
    {
      key: 'action' as const,
      label: 'Action',
      render: (value: string, entry: AuditEntry) => (
        <div>
          <p className="font-medium text-secondary-900">{value}</p>
          <p className="text-xs text-secondary-500">{entry.resource}</p>
        </div>
      ),
    },
    {
      key: 'userName' as const,
      label: 'User',
      render: (value: string, entry: AuditEntry) => (
        <div>
          <p className="text-sm text-secondary-900">{value || 'System'}</p>
          <p className="text-xs text-secondary-500">{entry.userId?.slice(0, 8)}</p>
        </div>
      ),
    },
    {
      key: 'details' as const,
      label: 'Details',
      render: (value: string, entry: AuditEntry) => (
        <div className="max-w-xs">
          <p className="text-sm text-secondary-900 truncate" title={value}>
            {value}
          </p>
          {entry.metadata && (
            <p className="text-xs text-secondary-500">
              {Object.keys(entry.metadata).length} additional fields
            </p>
          )}
        </div>
      ),
    },
    {
      key: 'result' as const,
      label: 'Result',
      width: '100px',
      render: (value: string) => (
        <Badge variant={value === 'SUCCESS' ? 'success' : value === 'FAILURE' ? 'error' : 'secondary'}>
          {value}
        </Badge>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-secondary-900">Compliance & Reporting</h1>
          <p className="text-secondary-600">Monitor regulatory compliance and system audit trails</p>
        </div>
        <div className="flex items-center space-x-3">
          <Button onClick={refetch} variant="outline">
            <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            Refresh
          </Button>
          <Link href="/compliance/exports">
            <Button>
              <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              Generate Reports
            </Button>
          </Link>
        </div>
      </div>

      {/* Tabs */}
      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList className="grid w-full grid-cols-5">
          <TabsTrigger value="overview">Compliance Overview</TabsTrigger>
          <TabsTrigger value="hud">HUD Compliance</TabsTrigger>
          <TabsTrigger value="audit">Audit Log</TabsTrigger>
          <TabsTrigger value="funding">Funding Compliance</TabsTrigger>
          <TabsTrigger value="privacy">Privacy & Security</TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="mt-6">
          {overview && (
            <>
              {/* Compliance Summary */}
              <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
                <Card>
                  <CardContent className="p-4">
                    <div className="flex items-center">
                      <div className="p-2 bg-green-100 rounded-lg">
                        <svg className="w-6 h-6 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                      </div>
                      <div className="ml-4">
                        <p className="text-sm font-medium text-secondary-600">Overall Score</p>
                        <p className="text-2xl font-bold text-secondary-900">{overview.overallScore}%</p>
                      </div>
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardContent className="p-4">
                    <div className="flex items-center">
                      <div className="p-2 bg-blue-100 rounded-lg">
                        <svg className="w-6 h-6 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" />
                        </svg>
                      </div>
                      <div className="ml-4">
                        <p className="text-sm font-medium text-secondary-600">Active Metrics</p>
                        <p className="text-2xl font-bold text-secondary-900">{overview.metrics?.length || 0}</p>
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
                        <p className="text-sm font-medium text-secondary-600">Needs Attention</p>
                        <p className="text-2xl font-bold text-secondary-900">
                          {overview.metrics?.filter((m: ComplianceMetric) => getComplianceStatus(m).status === 'warning' || getComplianceStatus(m).status === 'critical').length || 0}
                        </p>
                      </div>
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardContent className="p-4">
                    <div className="flex items-center">
                      <div className="p-2 bg-purple-100 rounded-lg">
                        <svg className="w-6 h-6 text-purple-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3a1 1 0 011-1h6a1 1 0 011 1v4h3a1 1 0 110 2h-1v9a2 2 0 01-2 2H7a2 2 0 01-2-2V9H4a1 1 0 110-2h4z" />
                        </svg>
                      </div>
                      <div className="ml-4">
                        <p className="text-sm font-medium text-secondary-600">Last Audit</p>
                        <p className="text-sm font-bold text-secondary-900">
                          {overview.lastAuditDate ? new Date(overview.lastAuditDate).toLocaleDateString() : 'Never'}
                        </p>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              </div>

              {/* Compliance Metrics */}
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <Card>
                  <CardHeader>
                    <CardTitle>Compliance Metrics</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-4">
                      {overview.metrics?.map((metric: ComplianceMetric, index: number) => {
                        const status = getComplianceStatus(metric);
                        const percentage = (metric.achieved / metric.target) * 100;
                        
                        return (
                          <div key={index} className="space-y-2">
                            <div className="flex items-center justify-between">
                              <h4 className="font-medium text-secondary-900">{metric.name}</h4>
                              <Badge variant="outline" className={status.color}>
                                {status.label}
                              </Badge>
                            </div>
                            <div className="flex items-center justify-between text-sm text-secondary-600">
                              <span>{metric.achieved} / {metric.target}</span>
                              <span>{percentage.toFixed(1)}%</span>
                            </div>
                            <div className="w-full bg-secondary-200 rounded-full h-2">
                              <div 
                                className={`h-2 rounded-full ${
                                  status.status === 'excellent' ? 'bg-green-500' :
                                  status.status === 'good' ? 'bg-blue-500' :
                                  status.status === 'warning' ? 'bg-yellow-500' : 'bg-red-500'
                                }`}
                                style={{ width: `${Math.min(percentage, 100)}%` }}
                              />
                            </div>
                            <p className="text-xs text-secondary-500">{metric.description}</p>
                          </div>
                        );
                      })}
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <CardTitle>Recent Compliance Issues</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-4">
                      <div className="p-3 bg-red-50 border border-red-200 rounded-lg">
                        <div className="flex items-center justify-between">
                          <h4 className="font-medium text-red-900">Missing Documentation</h4>
                          <Badge variant="error">Critical</Badge>
                        </div>
                        <p className="text-sm text-red-700 mt-1">
                          3 cases missing required intake documentation
                        </p>
                        <Button variant="outline" size="sm" className="mt-2 border-red-300 text-red-700">
                          Review Cases
                        </Button>
                      </div>

                      <div className="p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
                        <div className="flex items-center justify-between">
                          <h4 className="font-medium text-yellow-900">Service Frequency</h4>
                          <Badge variant="warning">Warning</Badge>
                        </div>
                        <p className="text-sm text-yellow-700 mt-1">
                          5 clients haven't received services in required timeframe
                        </p>
                        <Button variant="outline" size="sm" className="mt-2 border-yellow-300 text-yellow-700">
                          Schedule Services
                        </Button>
                      </div>

                      <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg">
                        <div className="flex items-center justify-between">
                          <h4 className="font-medium text-blue-900">Consent Renewals</h4>
                          <Badge variant="primary">Info</Badge>
                        </div>
                        <p className="text-sm text-blue-700 mt-1">
                          2 consent forms expiring within 30 days
                        </p>
                        <Button variant="outline" size="sm" className="mt-2 border-blue-300 text-blue-700">
                          Renewal Reminder
                        </Button>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              </div>
            </>
          )}
        </TabsContent>

        <TabsContent value="hud" className="mt-6">
          {hudSummary && (
            <>
              {/* HUD Compliance Summary */}
              <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
                <Card>
                  <CardContent className="p-4">
                    <div className="flex items-center">
                      <div className="p-2 bg-blue-100 rounded-lg">
                        <svg className="w-6 h-6 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                      </div>
                      <div className="ml-4">
                        <p className="text-sm font-medium text-secondary-600">HUD Compliance Score</p>
                        <p className="text-2xl font-bold text-secondary-900">{hudSummary.overallScore.toFixed(1)}%</p>
                      </div>
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardContent className="p-4">
                    <div className="flex items-center">
                      <div className="p-2 bg-green-100 rounded-lg">
                        <svg className="w-6 h-6 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                        </svg>
                      </div>
                      <div className="ml-4">
                        <p className="text-sm font-medium text-secondary-600">Fully Implemented</p>
                        <p className="text-2xl font-bold text-secondary-900">{hudSummary.fullyImplemented}</p>
                        <p className="text-xs text-secondary-500">of {hudSummary.totalElements} elements</p>
                      </div>
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardContent className="p-4">
                    <div className="flex items-center">
                      <div className="p-2 bg-yellow-100 rounded-lg">
                        <svg className="w-6 h-6 text-yellow-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
                        </svg>
                      </div>
                      <div className="ml-4">
                        <p className="text-sm font-medium text-secondary-600">Partially Implemented</p>
                        <p className="text-2xl font-bold text-secondary-900">{hudSummary.partiallyImplemented}</p>
                        <p className="text-xs text-secondary-500">need completion</p>
                      </div>
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardContent className="p-4">
                    <div className="flex items-center">
                      <div className="p-2 bg-red-100 rounded-lg">
                        <svg className="w-6 h-6 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                        </svg>
                      </div>
                      <div className="ml-4">
                        <p className="text-sm font-medium text-secondary-600">Not Implemented</p>
                        <p className="text-2xl font-bold text-secondary-900">{hudSummary.notImplemented}</p>
                        <p className="text-xs text-secondary-500">require implementation</p>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              </div>

              {/* HUD Categories */}
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <Card>
                  <CardHeader>
                    <CardTitle>HUD Element Categories</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-4">
                      {Object.entries(hudSummary.categories).map(([key, category]) => {
                        const percentage = category.compliancePercentage;
                        const getStatusColor = (pct: number) => {
                          if (pct >= 90) return 'bg-green-500';
                          if (pct >= 70) return 'bg-blue-500';
                          if (pct >= 50) return 'bg-yellow-500';
                          return 'bg-red-500';
                        };
                        
                        return (
                          <div key={key} className="space-y-2">
                            <div className="flex items-center justify-between">
                              <h4 className="font-medium text-secondary-900">{category.displayName}</h4>
                              <Badge variant="outline" className={
                                percentage >= 90 ? 'bg-green-100 text-green-800 border-green-200' :
                                percentage >= 70 ? 'bg-blue-100 text-blue-800 border-blue-200' :
                                percentage >= 50 ? 'bg-yellow-100 text-yellow-800 border-yellow-200' :
                                'bg-red-100 text-red-800 border-red-200'
                              }>
                                {percentage >= 90 ? 'Excellent' :
                                 percentage >= 70 ? 'Good' :
                                 percentage >= 50 ? 'Fair' : 'Needs Work'}
                              </Badge>
                            </div>
                            <div className="flex items-center justify-between text-sm text-secondary-600">
                              <span>{category.implementedElements} / {category.totalElements} implemented</span>
                              <span>{percentage.toFixed(1)}%</span>
                            </div>
                            <div className="w-full bg-secondary-200 rounded-full h-2">
                              <div 
                                className={`h-2 rounded-full ${getStatusColor(percentage)}`}
                                style={{ width: `${Math.min(percentage, 100)}%` }}
                              />
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <CardTitle>Implementation Actions</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-4">
                      {hudSummary.notImplemented > 0 && (
                        <div className="p-3 bg-red-50 border border-red-200 rounded-lg">
                          <div className="flex items-center justify-between">
                            <h4 className="font-medium text-red-900">Missing Elements</h4>
                            <Badge variant="error">Critical</Badge>
                          </div>
                          <p className="text-sm text-red-700 mt-1">
                            {hudSummary.notImplemented} HUD elements have no implementation
                          </p>
                          <Button variant="outline" size="sm" className="mt-2 border-red-300 text-red-700">
                            View Missing Elements
                          </Button>
                        </div>
                      )}

                      {hudSummary.partiallyImplemented > 0 && (
                        <div className="p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
                          <div className="flex items-center justify-between">
                            <h4 className="font-medium text-yellow-900">Partial Implementation</h4>
                            <Badge variant="warning">Warning</Badge>
                          </div>
                          <p className="text-sm text-yellow-700 mt-1">
                            {hudSummary.partiallyImplemented} elements need UI or API completion
                          </p>
                          <Button variant="outline" size="sm" className="mt-2 border-yellow-300 text-yellow-700">
                            Complete Implementation
                          </Button>
                        </div>
                      )}

                      <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg">
                        <div className="flex items-center justify-between">
                          <h4 className="font-medium text-blue-900">Export Matrix</h4>
                          <Badge variant="primary">Available</Badge>
                        </div>
                        <p className="text-sm text-blue-700 mt-1">
                          Download compliance matrix for external review
                        </p>
                        <div className="flex space-x-2 mt-2">
                          <Button 
                            variant="outline" 
                            size="sm" 
                            className="border-blue-300 text-blue-700"
                            onClick={() => window.open('/api/v1/compliance/matrix/export/json')}
                          >
                            JSON
                          </Button>
                          <Button 
                            variant="outline" 
                            size="sm" 
                            className="border-blue-300 text-blue-700"
                            onClick={() => window.open('/api/v1/compliance/matrix/export/yaml')}
                          >
                            YAML
                          </Button>
                        </div>
                      </div>

                      {hudSummary.overallScore >= 90 && (
                        <div className="p-3 bg-green-50 border border-green-200 rounded-lg">
                          <div className="flex items-center justify-between">
                            <h4 className="font-medium text-green-900">Production Ready</h4>
                            <Badge variant="success">Excellent</Badge>
                          </div>
                          <p className="text-sm text-green-700 mt-1">
                            HUD compliance score meets production standards
                          </p>
                        </div>
                      )}
                    </div>
                  </CardContent>
                </Card>
              </div>

              {/* Matrix Updated */}
              <Card className="mt-6">
                <CardHeader>
                  <CardTitle>Matrix Information</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm text-secondary-600">
                        Matrix last updated: {new Date(hudSummary.lastUpdated).toLocaleString()}
                      </p>
                      <p className="text-xs text-secondary-500 mt-1">
                        This compliance matrix is automatically generated from the codebase
                      </p>
                    </div>
                    <div className="flex space-x-2">
                      <Button 
                        variant="outline"
                        onClick={() => window.open('/api/v1/compliance/matrix/validate')}
                      >
                        Validate Matrix
                      </Button>
                      <Button variant="outline">
                        View Details
                      </Button>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </>
          )}
          
          {hudSummaryLoading && (
            <div className="flex items-center justify-center h-64">
              <div className="text-center">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600 mx-auto"></div>
                <p className="text-secondary-600 mt-2">Loading HUD compliance data...</p>
              </div>
            </div>
          )}
        </TabsContent>

        <TabsContent value="audit" className="mt-6">
          {/* Audit Filters */}
          <Card className="mb-6">
            <CardHeader>
              <CardTitle>Filter Audit Log</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
                <Select
                  label="Action"
                  value={auditFilters.action}
                  onChange={(value: string) => setAuditFilters(prev => ({ ...prev, action: value }))}
                  options={[
                    { value: '', label: 'All Actions' },
                    { value: 'CREATE', label: 'Create' },
                    { value: 'READ', label: 'Read/View' },
                    { value: 'UPDATE', label: 'Update' },
                    { value: 'DELETE', label: 'Delete' },
                    { value: 'LOGIN', label: 'Login' },
                    { value: 'LOGOUT', label: 'Logout' },
                    { value: 'EXPORT', label: 'Export' },
                  ]}
                />
                <Select
                  label="Resource"
                  value={auditFilters.resource}
                  onChange={(value: string) => setAuditFilters(prev => ({ ...prev, resource: value }))}
                  options={[
                    { value: '', label: 'All Resources' },
                    { value: 'CLIENT', label: 'Client' },
                    { value: 'CASE', label: 'Case' },
                    { value: 'MANDATED_REPORT', label: 'Mandated Report' },
                    { value: 'CONSENT', label: 'Consent' },
                    { value: 'FINANCIAL_ASSISTANCE', label: 'Financial Assistance' },
                    { value: 'HOUSING_ASSISTANCE', label: 'Housing Assistance' },
                  ]}
                />
                <div>
                  <label className="block text-sm font-medium text-secondary-700 mb-1">Start Date</label>
                  <input
                    type="date"
                    value={auditFilters.startDate}
                    onChange={(e) => setAuditFilters(prev => ({ ...prev, startDate: e.target.value }))}
                    className="w-full border border-secondary-300 rounded px-3 py-2 text-sm"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-secondary-700 mb-1">End Date</label>
                  <input
                    type="date"
                    value={auditFilters.endDate}
                    onChange={(e) => setAuditFilters(prev => ({ ...prev, endDate: e.target.value }))}
                    className="w-full border border-secondary-300 rounded px-3 py-2 text-sm"
                  />
                </div>
                <div className="flex items-end">
                  <Button
                    variant="outline"
                    onClick={() => setAuditFilters({ action: '', resource: '', userId: '', startDate: '', endDate: '' })}
                    className="w-full"
                  >
                    Clear Filters
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Audit Log Table */}
          <Card>
            <CardHeader>
              <CardTitle>System Audit Log</CardTitle>
            </CardHeader>
            <CardContent>
              <Table
                data={auditLog || []}
                columns={auditColumns}
                loading={auditLoading}
                emptyMessage="No audit entries found"
              />
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="funding" className="mt-6">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <Card className="lg:col-span-2">
              <CardHeader>
                <CardTitle>Funding Source Compliance</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-6">
                  <div className="p-4 border border-secondary-200 rounded-lg">
                    <div className="flex items-center justify-between mb-4">
                      <h4 className="font-medium text-secondary-900">VAWA Grant Requirements</h4>
                      <Badge variant="success">Compliant</Badge>
                    </div>
                    <div className="space-y-3">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-secondary-600">Service Documentation</span>
                        <span className="text-sm font-medium text-green-600">98% Complete</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-secondary-600">Quarterly Reports</span>
                        <span className="text-sm font-medium text-green-600">On Time</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-secondary-600">Budget Utilization</span>
                        <span className="text-sm font-medium text-green-600">85%</span>
                      </div>
                    </div>
                  </div>

                  <div className="p-4 border border-secondary-200 rounded-lg">
                    <div className="flex items-center justify-between mb-4">
                      <h4 className="font-medium text-secondary-900">ESG Rapid Rehousing</h4>
                      <Badge variant="warning">Needs Attention</Badge>
                    </div>
                    <div className="space-y-3">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-secondary-600">Income Verification</span>
                        <span className="text-sm font-medium text-yellow-600">75% Complete</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-secondary-600">Housing Inspections</span>
                        <span className="text-sm font-medium text-green-600">92% Complete</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-secondary-600">HMIS Data Quality</span>
                        <span className="text-sm font-medium text-red-600">68% - Below Threshold</span>
                      </div>
                    </div>
                  </div>

                  <div className="p-4 border border-secondary-200 rounded-lg">
                    <div className="flex items-center justify-between mb-4">
                      <h4 className="font-medium text-secondary-900">State DV Formula Grant</h4>
                      <Badge variant="success">Compliant</Badge>
                    </div>
                    <div className="space-y-3">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-secondary-600">Client Demographics</span>
                        <span className="text-sm font-medium text-green-600">100% Complete</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-secondary-600">Service Hours Tracking</span>
                        <span className="text-sm font-medium text-green-600">95% Complete</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-secondary-600">Annual Report</span>
                        <span className="text-sm font-medium text-green-600">Submitted</span>
                      </div>
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Upcoming Deadlines</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div className="p-3 bg-red-50 border border-red-200 rounded-lg">
                    <h4 className="font-medium text-red-900">HMIS Data Quality Report</h4>
                    <p className="text-sm text-red-700 mt-1">Due in 3 days</p>
                    <Button variant="outline" size="sm" className="mt-2 border-red-300 text-red-700">
                      Generate Report
                    </Button>
                  </div>

                  <div className="p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
                    <h4 className="font-medium text-yellow-900">ESG Quarterly Report</h4>
                    <p className="text-sm text-yellow-700 mt-1">Due in 2 weeks</p>
                    <Button variant="outline" size="sm" className="mt-2 border-yellow-300 text-yellow-700">
                      Start Report
                    </Button>
                  </div>

                  <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg">
                    <h4 className="font-medium text-blue-900">VAWA Performance Report</h4>
                    <p className="text-sm text-blue-700 mt-1">Due in 1 month</p>
                    <Button variant="outline" size="sm" className="mt-2 border-blue-300 text-blue-700">
                      View Requirements
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="privacy" className="mt-6">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <Card>
              <CardHeader>
                <CardTitle>Privacy & Data Protection</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div className="flex items-center justify-between p-3 bg-green-50 border border-green-200 rounded-lg">
                    <div>
                      <h4 className="font-medium text-green-900">Data Encryption</h4>
                      <p className="text-sm text-green-700">All PII encrypted at rest and in transit</p>
                    </div>
                    <Badge variant="success">Active</Badge>
                  </div>

                  <div className="flex items-center justify-between p-3 bg-green-50 border border-green-200 rounded-lg">
                    <div>
                      <h4 className="font-medium text-green-900">Access Controls</h4>
                      <p className="text-sm text-green-700">Role-based permissions enforced</p>
                    </div>
                    <Badge variant="success">Active</Badge>
                  </div>

                  <div className="flex items-center justify-between p-3 bg-green-50 border border-green-200 rounded-lg">
                    <div>
                      <h4 className="font-medium text-green-900">Audit Logging</h4>
                      <p className="text-sm text-green-700">All data access logged and monitored</p>
                    </div>
                    <Badge variant="success">Active</Badge>
                  </div>

                  <div className="flex items-center justify-between p-3 bg-blue-50 border border-blue-200 rounded-lg">
                    <div>
                      <h4 className="font-medium text-blue-900">Backup & Recovery</h4>
                      <p className="text-sm text-blue-700">Daily backups with 99.9% recovery guarantee</p>
                    </div>
                    <Badge variant="primary">Configured</Badge>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Security Monitoring</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div className="text-center p-3 bg-secondary-50 rounded-lg">
                      <p className="text-2xl font-bold text-secondary-900">0</p>
                      <p className="text-sm text-secondary-600">Security Incidents</p>
                    </div>
                    <div className="text-center p-3 bg-secondary-50 rounded-lg">
                      <p className="text-2xl font-bold text-secondary-900">847</p>
                      <p className="text-sm text-secondary-600">Logins Today</p>
                    </div>
                  </div>

                  <div className="space-y-3">
                    <h4 className="font-medium text-secondary-900">Recent Security Events</h4>
                    <div className="space-y-2 text-sm">
                      <div className="flex items-center justify-between">
                        <span className="text-secondary-600">Failed login attempts</span>
                        <span className="font-medium">3 (blocked)</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-secondary-600">Password changes</span>
                        <span className="font-medium">12 today</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-secondary-600">Permission violations</span>
                        <span className="font-medium">0</span>
                      </div>
                    </div>
                  </div>

                  <Button variant="outline" className="w-full">
                    View Full Security Report
                  </Button>
                </div>
              </CardContent>
            </Card>
          </div>
        </TabsContent>
      </Tabs>
    </div>
  );
}

export default function CompliancePage() {
  return (
    <ProtectedRoute>
      <PermissionGuard
        resource="compliance"
        action="view"
        fallback={
          <div className="p-6">
            <div className="text-center">
              <h2 className="text-xl font-semibold text-secondary-900 mb-2">Access Restricted</h2>
              <p className="text-secondary-600">You don't have permission to view compliance information.</p>
            </div>
          </div>
        }
      >
        <AppLayout 
          title="Compliance & Reporting" 
          breadcrumbs={[
            { label: 'Dashboard', href: '/dashboard' },
            { label: 'Compliance' }
          ]}
        >
          <div className="p-6">
            <ComplianceContent />
          </div>
        </AppLayout>
      </PermissionGuard>
    </ProtectedRoute>
  );
}
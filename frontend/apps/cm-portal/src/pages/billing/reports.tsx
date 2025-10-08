import { useState, useMemo } from 'react';
import Link from 'next/link';
import { ProtectedRoute, useCurrentUser } from '@haven/auth';
import { Card, CardHeader, CardTitle, CardContent, Button, Select, Badge, Input } from '@haven/ui';
import { 
  useServiceStatistics,
  useServiceEpisodes,
  useGenerateReport,
  type ServiceEpisode 
} from '@haven/api-client';
import AppLayout from '../../components/AppLayout';
import DurationAnalytics from '../../components/DurationAnalytics';

interface ReportTemplate {
  id: string;
  name: string;
  description: string;
  category: 'financial' | 'operational' | 'compliance' | 'performance';
  requiredParams: string[];
  outputFormats: ('pdf' | 'csv' | 'excel')[];
}

const REPORT_TEMPLATES: ReportTemplate[] = [
  {
    id: 'financial-summary',
    name: 'Financial Summary Report',
    description: 'Comprehensive financial overview including revenue, billing, and reimbursements',
    category: 'financial',
    requiredParams: ['dateRange'],
    outputFormats: ['pdf', 'csv', 'excel']
  },
  {
    id: 'service-utilization',
    name: 'Service Utilization Report',
    description: 'Analysis of service delivery patterns, duration, and efficiency metrics',
    category: 'operational',
    requiredParams: ['dateRange', 'serviceType'],
    outputFormats: ['pdf', 'csv']
  },
  {
    id: 'provider-performance',
    name: 'Provider Performance Report',
    description: 'Individual provider statistics including caseload, hours, and outcomes',
    category: 'performance',
    requiredParams: ['dateRange', 'providerId'],
    outputFormats: ['pdf', 'excel']
  },
  {
    id: 'funding-compliance',
    name: 'Funding Source Compliance',
    description: 'Compliance tracking for specific funding sources and grant requirements',
    category: 'compliance',
    requiredParams: ['dateRange', 'fundingSource'],
    outputFormats: ['pdf', 'csv']
  },
  {
    id: 'hmis-export',
    name: 'HMIS Data Export',
    description: 'HMIS-compliant CSV export for federal reporting requirements',
    category: 'compliance',
    requiredParams: ['dateRange'],
    outputFormats: ['csv']
  },
  {
    id: 'cal-oes-report',
    name: 'Cal OES DV/RCP Report',
    description: 'California Office of Emergency Services domestic violence reporting',
    category: 'compliance',
    requiredParams: ['dateRange', 'quarter'],
    outputFormats: ['pdf', 'excel']
  },
  {
    id: 'billing-reconciliation',
    name: 'Billing Reconciliation',
    description: 'Detailed billing reconciliation with payment tracking',
    category: 'financial',
    requiredParams: ['dateRange', 'fundingSource'],
    outputFormats: ['excel', 'csv']
  },
  {
    id: 'outcome-tracking',
    name: 'Client Outcome Tracking',
    description: 'Service outcomes and goal achievement metrics',
    category: 'performance',
    requiredParams: ['dateRange', 'programId'],
    outputFormats: ['pdf', 'excel']
  }
];

const CATEGORY_COLORS = {
  financial: 'bg-green-100 text-green-800',
  operational: 'bg-blue-100 text-blue-800',
  compliance: 'bg-purple-100 text-purple-800',
  performance: 'bg-orange-100 text-orange-800',
};

function ReportsContent() {
  const { user } = useCurrentUser();
  const [selectedCategory, setSelectedCategory] = useState<string>('all');
  const [selectedTemplate, setSelectedTemplate] = useState<ReportTemplate | null>(null);
  const [reportParams, setReportParams] = useState<Record<string, any>>({
    dateRange: 'month',
    providerId: user?.id || '',
    serviceType: 'all',
    fundingSource: 'all',
    programId: 'all',
    quarter: 'Q4-2024',
    format: 'pdf'
  });
  
  const { generateReport, loading: generateLoading } = useGenerateReport();
  const { statistics } = useServiceStatistics();

  // Filter templates by category
  const filteredTemplates = useMemo(() => {
    if (selectedCategory === 'all') return REPORT_TEMPLATES;
    return REPORT_TEMPLATES.filter(template => template.category === selectedCategory);
  }, [selectedCategory]);

  const handleGenerateReport = async () => {
    if (!selectedTemplate) return;

    try {
      await generateReport({
        templateId: selectedTemplate.id,
        parameters: reportParams,
        format: reportParams.format,
      });
    } catch (error) {
      console.error('Failed to generate report:', error);
      alert('Failed to generate report. Please try again.');
    }
  };

  const updateParam = (key: string, value: any) => {
    setReportParams(prev => ({ ...prev, [key]: value }));
  };

  const getParamInput = (paramName: string) => {
    switch (paramName) {
      case 'dateRange':
        return (
          <Select
            value={reportParams.dateRange}
            onChange={(value) => updateParam('dateRange', value)}
            options={[
              { value: 'week', label: 'Last 7 days' },
              { value: 'month', label: 'Last 30 days' },
              { value: 'quarter', label: 'Last 90 days' },
              { value: 'year', label: 'Last year' },
              { value: 'custom', label: 'Custom Range' },
            ]}
          />
        );
      case 'serviceType':
        return (
          <Select
            value={reportParams.serviceType}
            onChange={(value) => updateParam('serviceType', value)}
            options={[
              { value: 'all', label: 'All Service Types' },
              { value: 'INDIVIDUAL_COUNSELING', label: 'Individual Counseling' },
              { value: 'GROUP_COUNSELING', label: 'Group Counseling' },
              { value: 'CRISIS_INTERVENTION', label: 'Crisis Intervention' },
              { value: 'CASE_MANAGEMENT', label: 'Case Management' },
              { value: 'LEGAL_ADVOCACY', label: 'Legal Advocacy' },
            ]}
          />
        );
      case 'fundingSource':
        return (
          <Select
            value={reportParams.fundingSource}
            onChange={(value) => updateParam('fundingSource', value)}
            options={[
              { value: 'all', label: 'All Funding Sources' },
              { value: 'HUD-COC', label: 'HUD Continuum of Care' },
              { value: 'VAWA', label: 'VAWA' },
              { value: 'CAL-OES', label: 'Cal OES' },
              { value: 'CDBG', label: 'CDBG' },
              { value: 'FOUNDATION', label: 'Foundation Grants' },
            ]}
          />
        );
      case 'providerId':
        return (
          <Select
            value={reportParams.providerId}
            onChange={(value) => updateParam('providerId', value)}
            options={[
              { value: user?.id || '', label: 'My Services' },
              { value: 'all', label: 'All Providers' },
            ]}
          />
        );
      case 'programId':
        return (
          <Select
            value={reportParams.programId}
            onChange={(value) => updateParam('programId', value)}
            options={[
              { value: 'all', label: 'All Programs' },
              { value: 'emergency-shelter', label: 'Emergency Shelter' },
              { value: 'transitional-housing', label: 'Transitional Housing' },
              { value: 'domestic-violence', label: 'Domestic Violence' },
              { value: 'counseling-services', label: 'Counseling Services' },
            ]}
          />
        );
      case 'quarter':
        return (
          <Select
            value={reportParams.quarter}
            onChange={(value) => updateParam('quarter', value)}
            options={[
              { value: 'Q1-2024', label: 'Q1 2024' },
              { value: 'Q2-2024', label: 'Q2 2024' },
              { value: 'Q3-2024', label: 'Q3 2024' },
              { value: 'Q4-2024', label: 'Q4 2024' },
              { value: 'Q1-2025', label: 'Q1 2025' },
            ]}
          />
        );
      default:
        return (
          <Input
            value={reportParams[paramName] || ''}
            onChange={(e) => updateParam(paramName, e.target.value)}
            placeholder={`Enter ${paramName}`}
          />
        );
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-secondary-900">Reports & Analytics</h1>
          <p className="text-secondary-600">Generate comprehensive reports for compliance, billing, and performance analysis</p>
        </div>
        <div className="flex items-center space-x-3">
          <Link href="/billing">
            <Button variant="outline">Back to Billing</Button>
          </Link>
        </div>
      </div>

      {/* Quick Stats */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4">
            <div className="text-center">
              <p className="text-2xl font-bold text-blue-600">{statistics?.totalServices || 0}</p>
              <p className="text-sm text-secondary-600">Total Services</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="text-center">
              <p className="text-2xl font-bold text-green-600">
                ${(statistics?.totalBillableAmount || 0).toLocaleString()}
              </p>
              <p className="text-sm text-secondary-600">Total Revenue</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="text-center">
              <p className="text-2xl font-bold text-purple-600">
                {Math.round((statistics?.totalMinutes || 0) / 60)}h
              </p>
              <p className="text-sm text-secondary-600">Service Hours</p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="text-center">
              <p className="text-2xl font-bold text-orange-600">{statistics?.activeServices || 0}</p>
              <p className="text-sm text-secondary-600">Active Services</p>
            </div>
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Report Templates */}
        <div className="lg:col-span-2 space-y-6">
          {/* Category Filter */}
          <Card>
            <CardHeader>
              <CardTitle>Report Categories</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex flex-wrap gap-2">
                <Button
                  variant={selectedCategory === 'all' ? 'default' : 'outline'}
                  onClick={() => setSelectedCategory('all')}
                  size="sm"
                >
                  All Reports
                </Button>
                <Button
                  variant={selectedCategory === 'financial' ? 'default' : 'outline'}
                  onClick={() => setSelectedCategory('financial')}
                  size="sm"
                >
                  Financial
                </Button>
                <Button
                  variant={selectedCategory === 'operational' ? 'default' : 'outline'}
                  onClick={() => setSelectedCategory('operational')}
                  size="sm"
                >
                  Operational
                </Button>
                <Button
                  variant={selectedCategory === 'compliance' ? 'default' : 'outline'}
                  onClick={() => setSelectedCategory('compliance')}
                  size="sm"
                >
                  Compliance
                </Button>
                <Button
                  variant={selectedCategory === 'performance' ? 'default' : 'outline'}
                  onClick={() => setSelectedCategory('performance')}
                  size="sm"
                >
                  Performance
                </Button>
              </div>
            </CardContent>
          </Card>

          {/* Report Templates Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {filteredTemplates.map((template) => (
              <Card 
                key={template.id}
                className={`cursor-pointer transition-all hover:shadow-md ${
                  selectedTemplate?.id === template.id ? 'ring-2 ring-primary-500 bg-primary-50' : ''
                }`}
                onClick={() => setSelectedTemplate(template)}
              >
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <CardTitle className="text-base">{template.name}</CardTitle>
                    <Badge className={CATEGORY_COLORS[template.category]}>
                      {template.category}
                    </Badge>
                  </div>
                </CardHeader>
                <CardContent>
                  <p className="text-sm text-secondary-600 mb-3">
                    {template.description}
                  </p>
                  <div className="flex items-center justify-between">
                    <div className="flex flex-wrap gap-1">
                      {template.outputFormats.map((format) => (
                        <Badge key={format} variant="outline" className="text-xs">
                          {format.toUpperCase()}
                        </Badge>
                      ))}
                    </div>
                    {selectedTemplate?.id === template.id && (
                      <div className="w-4 h-4 bg-primary-600 rounded-full flex items-center justify-center">
                        <svg className="w-2.5 h-2.5 text-white" fill="currentColor" viewBox="0 0 20 20">
                          <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                        </svg>
                      </div>
                    )}
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>

        {/* Report Configuration */}
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>Report Configuration</CardTitle>
            </CardHeader>
            <CardContent>
              {selectedTemplate ? (
                <div className="space-y-4">
                  <div>
                    <h4 className="font-medium text-secondary-900 mb-2">{selectedTemplate.name}</h4>
                    <p className="text-sm text-secondary-600 mb-4">{selectedTemplate.description}</p>
                  </div>

                  {/* Required Parameters */}
                  {selectedTemplate.requiredParams.map((param) => (
                    <div key={param}>
                      <label className="block text-sm font-medium text-secondary-700 mb-1 capitalize">
                        {param.replace(/([A-Z])/g, ' $1').trim()}
                      </label>
                      {getParamInput(param)}
                    </div>
                  ))}

                  {/* Output Format */}
                  <div>
                    <label className="block text-sm font-medium text-secondary-700 mb-1">Output Format</label>
                    <Select
                      value={reportParams.format}
                      onChange={(value) => updateParam('format', value)}
                      options={selectedTemplate.outputFormats.map(format => ({
                        value: format,
                        label: format.toUpperCase()
                      }))}
                    />
                  </div>

                  {/* Generate Button */}
                  <Button 
                    onClick={handleGenerateReport} 
                    loading={generateLoading}
                    className="w-full"
                  >
                    <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-4-4m4 4l4-4m-6 4h8a2 2 0 002-2V5a2 2 0 00-2-2H6a2 2 0 00-2-2v14a2 2 0 002 2z" />
                    </svg>
                    Generate Report
                  </Button>
                </div>
              ) : (
                <div className="text-center py-8">
                  <svg className="w-12 h-12 text-secondary-400 mx-auto mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                  </svg>
                  <h3 className="text-lg font-medium text-secondary-900 mb-1">Select a Report</h3>
                  <p className="text-secondary-600">Choose a report template to configure and generate</p>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Quick Actions */}
          <Card>
            <CardHeader>
              <CardTitle>Quick Reports</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <Button variant="outline" className="w-full justify-start text-sm">
                  <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                  </svg>
                  Monthly Financial Summary
                </Button>
                <Button variant="outline" className="w-full justify-start text-sm">
                  <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  Service Hours Report
                </Button>
                <Button variant="outline" className="w-full justify-start text-sm">
                  <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  HMIS Export
                </Button>
                <Button variant="outline" className="w-full justify-start text-sm">
                  <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 8v8m-4-5v5m-4-2v2m-2 4h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                  </svg>
                  Performance Dashboard
                </Button>
              </div>
            </CardContent>
          </Card>

          {/* Recent Reports */}
          <Card>
            <CardHeader>
              <CardTitle>Recent Reports</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <div className="flex items-center justify-between p-2 bg-secondary-50 rounded">
                  <div>
                    <p className="text-sm font-medium text-secondary-900">Financial Summary</p>
                    <p className="text-xs text-secondary-500">Dec 15, 2024</p>
                  </div>
                  <Button variant="outline" size="sm">Download</Button>
                </div>
                <div className="flex items-center justify-between p-2 bg-secondary-50 rounded">
                  <div>
                    <p className="text-sm font-medium text-secondary-900">HMIS Export</p>
                    <p className="text-xs text-secondary-500">Dec 12, 2024</p>
                  </div>
                  <Button variant="outline" size="sm">Download</Button>
                </div>
                <div className="flex items-center justify-between p-2 bg-secondary-50 rounded">
                  <div>
                    <p className="text-sm font-medium text-secondary-900">Provider Performance</p>
                    <p className="text-xs text-secondary-500">Dec 10, 2024</p>
                  </div>
                  <Button variant="outline" size="sm">Download</Button>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Duration Analytics */}
      <DurationAnalytics 
        providerId={reportParams.providerId !== 'all' ? reportParams.providerId : undefined}
        timeRange={reportParams.dateRange as any}
      />
    </div>
  );
}

export default function ReportsPage() {
  return (
    <ProtectedRoute>
      <AppLayout 
        title="Reports & Analytics" 
        breadcrumbs={[
          { label: 'Dashboard', href: '/dashboard' },
          { label: 'Billing', href: '/billing' },
          { label: 'Reports' }
        ]}
      >
        <div className="p-6">
          <ReportsContent />
        </div>
      </AppLayout>
    </ProtectedRoute>
  );
}
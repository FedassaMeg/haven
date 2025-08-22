import React, { useState } from 'react';
import { ProtectedRoute, useCurrentUser } from '@haven/auth';
import { Card, CardHeader, CardTitle, CardContent, Badge, Button, Table } from '@haven/ui';
import { DocumentationGap } from '@haven/api-client';
import AppLayout from '../../components/AppLayout';

// Mock data for demonstration
const mockDocumentationGaps: DocumentationGap[] = [
  {
    clientId: 'client-1',
    clientName: 'Jane Smith',
    missingDocument: 'Income Verification (Pay Stubs)',
    dueDate: '2024-09-15',
    daysOverdue: 5,
    caseNumber: 'DV-2024-001'
  },
  {
    clientId: 'client-2',
    clientName: 'Maria Garcia',
    missingDocument: 'Lease Agreement Copy',
    dueDate: '2024-09-20',
    daysOverdue: 0,
    caseNumber: 'DV-2024-002'
  },
  {
    clientId: 'client-3',
    clientName: 'Sarah Johnson',
    missingDocument: 'Social Security Card',
    dueDate: '2024-09-10',
    daysOverdue: 10,
    caseNumber: 'DV-2024-003'
  },
  {
    clientId: 'client-4',
    clientName: 'Lisa Brown',
    missingDocument: 'Birth Certificate for Child',
    dueDate: '2024-09-25',
    daysOverdue: 0,
    caseNumber: 'DV-2024-004'
  },
  {
    clientId: 'client-5',
    clientName: 'Amanda Davis',
    missingDocument: 'Bank Statements (3 months)',
    dueDate: '2024-09-08',
    daysOverdue: 12,
    caseNumber: 'DV-2024-005'
  }
];

function DocumentationGapsContent() {
  const { user } = useCurrentUser();
  const [filter, setFilter] = useState<'all' | 'overdue' | 'upcoming'>('all');
  const [sortBy, setSortBy] = useState<'dueDate' | 'daysOverdue' | 'clientName'>('daysOverdue');

  const getFilteredGaps = () => {
    let filtered = [...mockDocumentationGaps];

    // Apply filter
    switch (filter) {
      case 'overdue':
        filtered = filtered.filter(gap => gap.daysOverdue && gap.daysOverdue > 0);
        break;
      case 'upcoming':
        filtered = filtered.filter(gap => !gap.daysOverdue || gap.daysOverdue <= 0);
        break;
      default:
        break;
    }

    // Apply sort
    filtered.sort((a, b) => {
      switch (sortBy) {
        case 'daysOverdue':
          return (b.daysOverdue || 0) - (a.daysOverdue || 0);
        case 'dueDate':
          return new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime();
        case 'clientName':
          return a.clientName.localeCompare(b.clientName);
        default:
          return 0;
      }
    });

    return filtered;
  };

  const filteredGaps = getFilteredGaps();
  const overdueCount = mockDocumentationGaps.filter(gap => gap.daysOverdue && gap.daysOverdue > 0).length;
  const upcomingCount = mockDocumentationGaps.filter(gap => !gap.daysOverdue || gap.daysOverdue <= 0).length;

  const getOverdueColor = (daysOverdue?: number) => {
    if (!daysOverdue || daysOverdue <= 0) return 'text-slate-600';
    if (daysOverdue <= 3) return 'text-yellow-600';
    if (daysOverdue <= 7) return 'text-orange-600';
    return 'text-red-600';
  };

  const documentationColumns = [
    {
      key: 'clientName' as const,
      label: 'Client',
      render: (value: string, gap: DocumentationGap) => (
        <div>
          <div className="font-medium">{value}</div>
          <div className="text-sm text-slate-600">{gap.caseNumber}</div>
        </div>
      ),
    },
    {
      key: 'missingDocument' as const,
      label: 'Missing Document',
      render: (value: string) => (
        <div className="font-medium">{value}</div>
      ),
    },
    {
      key: 'dueDate' as const,
      label: 'Due Date',
      render: (value: string, gap: DocumentationGap) => (
        <div>
          <div className={gap.daysOverdue && gap.daysOverdue > 0 ? 'text-red-600 font-medium' : ''}>
            {new Date(value).toLocaleDateString()}
          </div>
          {gap.daysOverdue && gap.daysOverdue > 0 ? (
            <div className="text-sm text-red-600">
              {gap.daysOverdue} days overdue
            </div>
          ) : (
            <div className="text-sm text-slate-600">
              {Math.ceil((new Date(value).getTime() - new Date().getTime()) / (1000 * 60 * 60 * 24))} days remaining
            </div>
          )}
        </div>
      ),
    },
    {
      key: 'status' as const,
      label: 'Status',
      render: (value: any, gap: DocumentationGap) => (
        <Badge 
          variant="secondary" 
          className={
            gap.daysOverdue && gap.daysOverdue > 0
              ? 'bg-red-100 text-red-800 border-red-200'
              : 'bg-yellow-100 text-yellow-800 border-yellow-200'
          }
        >
          {gap.daysOverdue && gap.daysOverdue > 0 ? 'Overdue' : 'Pending'}
        </Badge>
      ),
    },
    {
      key: 'actions' as const,
      label: 'Actions',
      render: (value: any, gap: DocumentationGap) => (
        <div className="flex space-x-2">
          <Button size="sm" variant="outline">
            Request
          </Button>
          <Button size="sm" variant="ghost">
            Mark Received
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
          <h1 className="text-2xl font-bold text-slate-800">Documentation Gap Tracking</h1>
          <p className="text-slate-600">Monitor and manage missing client documentation for compliance</p>
        </div>
        <div className="flex items-center space-x-3">
          <Button variant="outline" size="sm">
            Export Report
          </Button>
          <Button variant="outline" size="sm">
            <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            Refresh
          </Button>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card className="border-red-200 bg-red-50">
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-red-600">Overdue</p>
                <p className="text-2xl font-bold text-red-700">{overdueCount}</p>
              </div>
              <div className="h-8 w-8 bg-red-200 rounded-full flex items-center justify-center">
                <svg className="w-4 h-4 text-red-600" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                </svg>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card className="border-yellow-200 bg-yellow-50">
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-yellow-600">Upcoming</p>
                <p className="text-2xl font-bold text-yellow-700">{upcomingCount}</p>
              </div>
              <div className="h-8 w-8 bg-yellow-200 rounded-full flex items-center justify-center">
                <svg className="w-4 h-4 text-yellow-600" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z" clipRule="evenodd" />
                </svg>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card className="border-slate-200 bg-slate-50">
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-slate-600">Total Gaps</p>
                <p className="text-2xl font-bold text-slate-700">{mockDocumentationGaps.length}</p>
              </div>
              <div className="h-8 w-8 bg-slate-200 rounded-full flex items-center justify-center">
                <svg className="w-4 h-4 text-slate-600" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M4 4a2 2 0 012-2h8a2 2 0 012 2v12a1 1 0 110 2h-3a1 1 0 01-1-1v-1a1 1 0 00-1-1H9a1 1 0 00-1 1v1a1 1 0 01-1 1H4a1 1 0 110-2V4zm3 1h2v2H7V5zm2 4H7v2h2V9zm2-4h2v2h-2V5zm2 4h-2v2h2V9z" clipRule="evenodd" />
                </svg>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card className="border-green-200 bg-green-50">
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-green-600">Compliance Rate</p>
                <p className="text-2xl font-bold text-green-700">
                  {Math.round(((mockDocumentationGaps.length - overdueCount) / mockDocumentationGaps.length) * 100)}%
                </p>
              </div>
              <div className="h-8 w-8 bg-green-200 rounded-full flex items-center justify-center">
                <svg className="w-4 h-4 text-green-600" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                </svg>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Filters and Controls */}
      <Card>
        <CardHeader>
          <CardTitle>Filters & Controls</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap items-center gap-4">
            <div className="flex items-center space-x-2">
              <label className="text-sm font-medium text-slate-700">Filter:</label>
              <select
                value={filter}
                onChange={(e) => setFilter(e.target.value as typeof filter)}
                className="border border-slate-300 rounded px-3 py-1 text-sm"
              >
                <option value="all">All Gaps</option>
                <option value="overdue">Overdue Only</option>
                <option value="upcoming">Upcoming Only</option>
              </select>
            </div>
            <div className="flex items-center space-x-2">
              <label className="text-sm font-medium text-slate-700">Sort by:</label>
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value as typeof sortBy)}
                className="border border-slate-300 rounded px-3 py-1 text-sm"
              >
                <option value="daysOverdue">Days Overdue</option>
                <option value="dueDate">Due Date</option>
                <option value="clientName">Client Name</option>
              </select>
            </div>
            <div className="ml-auto">
              <Button size="sm">
                Send Reminder Emails
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Documentation Gaps Table */}
      <Card>
        <CardHeader>
          <CardTitle>Documentation Gaps ({filteredGaps.length})</CardTitle>
        </CardHeader>
        <CardContent>
          <Table
            data={filteredGaps}
            columns={documentationColumns}
            loading={false}
            emptyMessage="No documentation gaps found"
          />
        </CardContent>
      </Card>

      {/* Common Documents Reference */}
      <Card>
        <CardHeader>
          <CardTitle>Common Required Documents</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div>
              <h4 className="font-medium text-slate-700 mb-2">Identity & Eligibility</h4>
              <ul className="text-sm text-slate-600 space-y-1">
                <li>• Government-issued photo ID</li>
                <li>• Social Security card</li>
                <li>• Birth certificate</li>
                <li>• Immigration documents (if applicable)</li>
              </ul>
            </div>
            <div>
              <h4 className="font-medium text-slate-700 mb-2">Financial Information</h4>
              <ul className="text-sm text-slate-600 space-y-1">
                <li>• Income verification (pay stubs, benefits)</li>
                <li>• Bank statements (3 months)</li>
                <li>• Tax returns (if applicable)</li>
                <li>• Asset documentation</li>
              </ul>
            </div>
            <div>
              <h4 className="font-medium text-slate-700 mb-2">Housing & Services</h4>
              <ul className="text-sm text-slate-600 space-y-1">
                <li>• Lease agreements</li>
                <li>• Utility bills</li>
                <li>• Medical records (if relevant)</li>
                <li>• Court documents (protective orders, custody)</li>
              </ul>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

export default function DocumentationGapsPage() {
  return (
    <ProtectedRoute requiredRoles={['admin', 'supervisor', 'case_manager', 'compliance_officer']}>
      <AppLayout 
        title="Documentation Gaps" 
        breadcrumbs={[
          { label: 'Dashboard', href: '/dashboard' },
          { label: 'Compliance', href: '/compliance' },
          { label: 'Documentation Gaps' }
        ]}
      >
        <div className="p-6">
          <DocumentationGapsContent />
        </div>
      </AppLayout>
    </ProtectedRoute>
  );
}
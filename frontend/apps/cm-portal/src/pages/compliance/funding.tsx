import React, { useState } from 'react';
import { ProtectedRoute, useCurrentUser } from '@haven/auth';
import { Card, CardHeader, CardTitle, CardContent, Badge, Button, Table, Progress } from '@haven/ui';
import { FundingComplianceView } from '@haven/api-client';
import AppLayout from '../../components/AppLayout';

// Mock data for demonstration
const mockFundingData: FundingComplianceView[] = [
  {
    fundingSourceId: '1',
    fundingSourceName: 'VAWA Housing Grant',
    fundingType: 'Federal Grant',
    totalBudget: 500000,
    amountSpent: 325000,
    amountCommitted: 75000,
    amountAvailable: 100000,
    utilizationPercentage: 80,
    fundingPeriodStart: '2024-01-01',
    fundingPeriodEnd: '2024-12-31',
    daysRemaining: 45,
    complianceStatus: 'COMPLIANT',
    documentationGaps: [
      {
        clientId: 'client1',
        clientName: 'Jane Smith',
        missingDocument: 'Income Verification',
        dueDate: '2024-09-15',
        daysOverdue: 5,
        caseNumber: 'DV-2024-001'
      }
    ],
    pendingPayments: [
      {
        paymentId: 'pay1',
        clientId: 'client2',
        clientName: 'Mary Johnson',
        amount: 1200,
        paymentType: 'Rental Assistance',
        requestDate: '2024-08-20',
        dueDate: '2024-09-01',
        approvalStatus: 'APPROVED',
        vendorName: 'ABC Property Management'
      }
    ],
    spendDownTracking: {
      quarterlyTarget: 125000,
      quarterlySpent: 95000,
      monthlyTarget: 41667,
      monthlySpent: 32000,
      dailyBurnRate: 1200,
      projectedYearEndSpend: 475000,
      spendVelocity: 0.95,
      isUnderspending: true,
      isOverspending: false,
      daysToTargetReached: 25
    },
    grantNumber: 'VAWA-2024-001',
    programArea: 'Housing Assistance'
  },
  {
    fundingSourceId: '2',
    fundingSourceName: 'FVPSA Emergency Shelter',
    fundingType: 'Federal Grant',
    totalBudget: 250000,
    amountSpent: 210000,
    amountCommitted: 35000,
    amountAvailable: 5000,
    utilizationPercentage: 98,
    fundingPeriodStart: '2024-01-01',
    fundingPeriodEnd: '2024-12-31',
    daysRemaining: 45,
    complianceStatus: 'AT_RISK',
    documentationGaps: [],
    pendingPayments: [],
    spendDownTracking: {
      quarterlyTarget: 62500,
      quarterlySpent: 70000,
      monthlyTarget: 20833,
      monthlySpent: 23500,
      dailyBurnRate: 850,
      projectedYearEndSpend: 255000,
      spendVelocity: 1.12,
      isUnderspending: false,
      isOverspending: true,
      daysToTargetReached: 6
    },
    grantNumber: 'FVPSA-2024-002',
    programArea: 'Emergency Services'
  }
];

function FundingComplianceDashboardContent() {
  const { user } = useCurrentUser();
  const [selectedFunding, setSelectedFunding] = useState<FundingComplianceView | null>(null);
  const [viewMode, setViewMode] = useState<'overview' | 'details'>('overview');

  const getComplianceStatusColor = (status: string) => {
    switch (status) {
      case 'COMPLIANT':
        return 'bg-green-100 text-green-800 border-green-200';
      case 'ATTENTION_NEEDED':
        return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'AT_RISK':
        return 'bg-orange-100 text-orange-800 border-orange-200';
      case 'NON_COMPLIANT':
        return 'bg-red-100 text-red-800 border-red-200';
      case 'UNDER_REVIEW':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(amount);
  };

  const formatPercentage = (value: number) => {
    return `${value.toFixed(1)}%`;
  };

  const getTotalMetrics = () => {
    return mockFundingData.reduce((acc, funding) => ({
      totalBudget: acc.totalBudget + funding.totalBudget,
      totalSpent: acc.totalSpent + funding.amountSpent,
      totalCommitted: acc.totalCommitted + funding.amountCommitted,
      totalAvailable: acc.totalAvailable + funding.amountAvailable,
      totalGaps: acc.totalGaps + funding.documentationGaps.length,
      totalPending: acc.totalPending + funding.pendingPayments.length
    }), { totalBudget: 0, totalSpent: 0, totalCommitted: 0, totalAvailable: 0, totalGaps: 0, totalPending: 0 });
  };

  const metrics = getTotalMetrics();

  const fundingColumns = [
    {
      key: 'fundingSourceName' as const,
      label: 'Funding Source',
      render: (value: string, funding: FundingComplianceView) => (
        <div>
          <div className="font-medium">{value}</div>
          <div className="text-sm text-slate-600">{funding.grantNumber}</div>
        </div>
      ),
    },
    {
      key: 'complianceStatus' as const,
      label: 'Status',
      render: (value: string) => (
        <Badge variant="secondary" className={getComplianceStatusColor(value)}>
          {value.replace('_', ' ')}
        </Badge>
      ),
    },
    {
      key: 'utilizationPercentage' as const,
      label: 'Utilization',
      render: (value: number) => (
        <div className="w-24">
          <div className="flex justify-between text-sm mb-1">
            <span>{formatPercentage(value)}</span>
          </div>
          <Progress value={value} className="h-2" />
        </div>
      ),
    },
    {
      key: 'amountAvailable' as const,
      label: 'Available',
      render: (value: number, funding: FundingComplianceView) => (
        <div>
          <div className="font-medium">{formatCurrency(value)}</div>
          <div className="text-sm text-slate-600">
            of {formatCurrency(funding.totalBudget)}
          </div>
        </div>
      ),
    },
    {
      key: 'daysRemaining' as const,
      label: 'Days Left',
      render: (value: number) => (
        <div className={value < 30 ? 'text-red-600 font-medium' : 'text-slate-600'}>
          {value} days
        </div>
      ),
    },
    {
      key: 'actions' as const,
      label: 'Actions',
      render: (value: any, funding: FundingComplianceView) => (
        <Button
          size="sm"
          variant="outline"
          onClick={() => {
            setSelectedFunding(funding);
            setViewMode('details');
          }}
        >
          View Details
        </Button>
      ),
    },
  ];

  if (viewMode === 'details' && selectedFunding) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <Button
              variant="ghost"
              onClick={() => setViewMode('overview')}
              className="mb-2"
            >
              ← Back to Overview
            </Button>
            <h1 className="text-2xl font-bold text-slate-800">
              {selectedFunding.fundingSourceName}
            </h1>
            <p className="text-slate-600">{selectedFunding.grantNumber} • {selectedFunding.programArea}</p>
          </div>
          <Badge variant="secondary" className={getComplianceStatusColor(selectedFunding.complianceStatus)}>
            {selectedFunding.complianceStatus.replace('_', ' ')}
          </Badge>
        </div>

        {/* Spend-Down Tracking */}
        <Card>
          <CardHeader>
            <CardTitle>Spend-Down Analysis</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <div>
                <h4 className="font-medium text-slate-700 mb-3">Quarterly Performance</h4>
                <div className="space-y-2">
                  <div className="flex justify-between">
                    <span className="text-sm text-slate-600">Target:</span>
                    <span className="font-medium">{formatCurrency(selectedFunding.spendDownTracking.quarterlyTarget)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-sm text-slate-600">Spent:</span>
                    <span className="font-medium">{formatCurrency(selectedFunding.spendDownTracking.quarterlySpent)}</span>
                  </div>
                  <div className="pt-2">
                    <Progress 
                      value={(selectedFunding.spendDownTracking.quarterlySpent / selectedFunding.spendDownTracking.quarterlyTarget) * 100} 
                      className="h-2" 
                    />
                  </div>
                </div>
              </div>

              <div>
                <h4 className="font-medium text-slate-700 mb-3">Monthly Performance</h4>
                <div className="space-y-2">
                  <div className="flex justify-between">
                    <span className="text-sm text-slate-600">Target:</span>
                    <span className="font-medium">{formatCurrency(selectedFunding.spendDownTracking.monthlyTarget)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-sm text-slate-600">Spent:</span>
                    <span className="font-medium">{formatCurrency(selectedFunding.spendDownTracking.monthlySpent)}</span>
                  </div>
                  <div className="pt-2">
                    <Progress 
                      value={(selectedFunding.spendDownTracking.monthlySpent / selectedFunding.spendDownTracking.monthlyTarget) * 100} 
                      className="h-2" 
                    />
                  </div>
                </div>
              </div>

              <div>
                <h4 className="font-medium text-slate-700 mb-3">Projections</h4>
                <div className="space-y-2">
                  <div className="flex justify-between">
                    <span className="text-sm text-slate-600">Daily Burn Rate:</span>
                    <span className="font-medium">{formatCurrency(selectedFunding.spendDownTracking.dailyBurnRate)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-sm text-slate-600">Projected Year-End:</span>
                    <span className="font-medium">{formatCurrency(selectedFunding.spendDownTracking.projectedYearEndSpend)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-sm text-slate-600">Spend Velocity:</span>
                    <span className={`font-medium ${selectedFunding.spendDownTracking.spendVelocity > 1 ? 'text-orange-600' : 'text-green-600'}`}>
                      {selectedFunding.spendDownTracking.spendVelocity.toFixed(2)}x
                    </span>
                  </div>
                </div>
              </div>
            </div>

            {(selectedFunding.spendDownTracking.isUnderspending || selectedFunding.spendDownTracking.isOverspending) && (
              <div className="mt-4 p-3 rounded-lg bg-yellow-50 border border-yellow-200">
                <div className="flex items-center">
                  <svg className="w-5 h-5 text-yellow-600 mr-2" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                  </svg>
                  <span className="text-sm font-medium text-yellow-800">
                    {selectedFunding.spendDownTracking.isOverspending 
                      ? 'This funding source is overspending relative to the timeline'
                      : 'This funding source is underspending relative to the timeline'
                    }
                  </span>
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Documentation Gaps */}
        {selectedFunding.documentationGaps.length > 0 && (
          <Card>
            <CardHeader>
              <CardTitle>Documentation Gaps ({selectedFunding.documentationGaps.length})</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                {selectedFunding.documentationGaps.map((gap, index) => (
                  <div key={index} className="border rounded-lg p-3">
                    <div className="flex justify-between items-start">
                      <div>
                        <div className="font-medium">{gap.clientName}</div>
                        <div className="text-sm text-slate-600">{gap.caseNumber}</div>
                        <div className="text-sm">{gap.missingDocument}</div>
                      </div>
                      <div className="text-right">
                        <div className="text-sm text-slate-600">Due: {new Date(gap.dueDate).toLocaleDateString()}</div>
                        {gap.daysOverdue && gap.daysOverdue > 0 && (
                          <div className="text-sm text-red-600 font-medium">
                            {gap.daysOverdue} days overdue
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        )}

        {/* Pending Payments */}
        {selectedFunding.pendingPayments.length > 0 && (
          <Card>
            <CardHeader>
              <CardTitle>Pending Payments ({selectedFunding.pendingPayments.length})</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                {selectedFunding.pendingPayments.map((payment, index) => (
                  <div key={index} className="border rounded-lg p-3">
                    <div className="flex justify-between items-start">
                      <div>
                        <div className="font-medium">{payment.clientName}</div>
                        <div className="text-sm text-slate-600">{payment.paymentType}</div>
                        {payment.vendorName && (
                          <div className="text-sm text-slate-600">Vendor: {payment.vendorName}</div>
                        )}
                      </div>
                      <div className="text-right">
                        <div className="font-medium">{formatCurrency(payment.amount)}</div>
                        <div className="text-sm text-slate-600">Due: {new Date(payment.dueDate).toLocaleDateString()}</div>
                        <Badge variant="secondary" className="mt-1">
                          {payment.approvalStatus}
                        </Badge>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        )}
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Funding Compliance Dashboard</h1>
          <p className="text-slate-600">Monitor grant spend-down rates, compliance status, and documentation requirements</p>
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
      <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-6 gap-4">
        <Card>
          <CardContent className="p-4">
            <div className="text-sm font-medium text-slate-600">Total Budget</div>
            <div className="text-2xl font-bold text-slate-900">{formatCurrency(metrics.totalBudget)}</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="text-sm font-medium text-slate-600">Amount Spent</div>
            <div className="text-2xl font-bold text-slate-900">{formatCurrency(metrics.totalSpent)}</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="text-sm font-medium text-slate-600">Committed</div>
            <div className="text-2xl font-bold text-slate-900">{formatCurrency(metrics.totalCommitted)}</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="text-sm font-medium text-slate-600">Available</div>
            <div className="text-2xl font-bold text-green-600">{formatCurrency(metrics.totalAvailable)}</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="text-sm font-medium text-slate-600">Doc Gaps</div>
            <div className="text-2xl font-bold text-orange-600">{metrics.totalGaps}</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="text-sm font-medium text-slate-600">Pending</div>
            <div className="text-2xl font-bold text-blue-600">{metrics.totalPending}</div>
          </CardContent>
        </Card>
      </div>

      {/* Funding Sources Table */}
      <Card>
        <CardHeader>
          <CardTitle>Funding Sources</CardTitle>
        </CardHeader>
        <CardContent>
          <Table
            data={mockFundingData}
            columns={fundingColumns}
            loading={false}
            emptyMessage="No funding sources found"
          />
        </CardContent>
      </Card>
    </div>
  );
}

export default function FundingCompliancePage() {
  return (
    <ProtectedRoute requiredRoles={['admin', 'supervisor', 'financial_admin']}>
      <AppLayout 
        title="Funding Compliance" 
        breadcrumbs={[
          { label: 'Dashboard', href: '/dashboard' },
          { label: 'Compliance', href: '/compliance' },
          { label: 'Funding Overview' }
        ]}
      >
        <div className="p-6">
          <FundingComplianceDashboardContent />
        </div>
      </AppLayout>
    </ProtectedRoute>
  );
}
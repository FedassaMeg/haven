import { ProtectedRoute, useCurrentUser } from '@haven/auth';
import { Card, CardHeader, CardTitle, CardContent, Badge, Button } from '@haven/ui';
import { useClients, useCases } from '@haven/api-client';
import AppLayout from '../components/AppLayout';
import Link from 'next/link';
import { useState } from 'react';

interface DashboardStatsProps {
  title: string;
  value: string | number;
  subtitle?: string;
  trend?: {
    value: string;
    isPositive: boolean;
  };
  icon?: React.ReactNode;
}

const DashboardStats: React.FC<DashboardStatsProps> = ({
  title,
  value,
  subtitle,
  trend,
  icon,
}) => {
  return (
    <Card>
      <CardContent className="p-6">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-secondary-500">{title}</p>
            <p className="text-3xl font-bold text-secondary-900">{value}</p>
            {subtitle && (
              <p className="text-sm text-secondary-600">{subtitle}</p>
            )}
            {trend && (
              <div className="flex items-center mt-2">
                <span
                  className={`text-sm font-medium ${
                    trend.isPositive ? 'text-success-600' : 'text-error-600'
                  }`}
                >
                  {trend.isPositive ? '+' : ''}{trend.value}
                </span>
                <span className="text-sm text-secondary-500 ml-2">from last month</span>
              </div>
            )}
          </div>
          {icon && (
            <div className="p-3 bg-primary-100 rounded-lg">
              <div className="w-6 h-6 text-primary-600">{icon}</div>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
};

interface RecentActivityItem {
  id: string;
  type: 'client_created' | 'case_opened' | 'case_assigned' | 'case_closed';
  description: string;
  timestamp: string;
  user: string;
}

const RecentActivity: React.FC = () => {
  // Mock data - in real app this would come from API
  const activities: RecentActivityItem[] = [
    {
      id: '1',
      type: 'client_created',
      description: 'New client "John Smith" was created',
      timestamp: '2 hours ago',
      user: 'Sarah Johnson',
    },
    {
      id: '2', 
      type: 'case_opened',
      description: 'Case "Housing Assessment" was opened for Maria Garcia',
      timestamp: '4 hours ago',
      user: 'Mike Chen',
    },
    {
      id: '3',
      type: 'case_assigned',
      description: 'Case "Financial Aid" was assigned to Jessica Brown',
      timestamp: '6 hours ago',
      user: 'David Wilson',
    },
    {
      id: '4',
      type: 'case_closed',
      description: 'Case "Employment Services" was closed successfully',
      timestamp: '1 day ago',
      user: 'Lisa Rodriguez',
    },
  ];

  const getActivityIcon = (type: string) => {
    switch (type) {
      case 'client_created':
        return (
          <div className="w-8 h-8 bg-success-100 rounded-full flex items-center justify-center">
            <svg className="w-4 h-4 text-success-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
            </svg>
          </div>
        );
      case 'case_opened':
        return (
          <div className="w-8 h-8 bg-primary-100 rounded-full flex items-center justify-center">
            <svg className="w-4 h-4 text-primary-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
          </div>
        );
      case 'case_assigned':
        return (
          <div className="w-8 h-8 bg-warning-100 rounded-full flex items-center justify-center">
            <svg className="w-4 h-4 text-warning-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
          </div>
        );
      case 'case_closed':
        return (
          <div className="w-8 h-8 bg-secondary-100 rounded-full flex items-center justify-center">
            <svg className="w-4 h-4 text-secondary-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4" />
            </svg>
          </div>
        );
      default:
        return null;
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Recent Activity</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {activities.map((activity) => (
            <div key={activity.id} className="flex items-start space-x-3">
              {getActivityIcon(activity.type)}
              <div className="flex-1 min-w-0">
                <p className="text-sm text-secondary-900">{activity.description}</p>
                <p className="text-xs text-secondary-500">
                  by {activity.user} • {activity.timestamp}
                </p>
              </div>
            </div>
          ))}
        </div>
        <div className="mt-4 pt-4 border-t border-secondary-200">
          <Link href="/activity" className="text-sm text-primary-600 hover:text-primary-700">
            View all activity
          </Link>
        </div>
      </CardContent>
    </Card>
  );
};

function DashboardContent() {
  const { user, fullName } = useCurrentUser();
  const { clients, loading: clientsLoading } = useClients({ activeOnly: true });
  const { cases, loading: casesLoading } = useCases({ activeOnly: true });

  const activeCases = cases?.filter(c => c.status !== 'CLOSED') || [];
  const myCases = cases?.filter(c => c.assignment?.assigneeId === user?.id) || [];
  const casesNeedingAttention = cases?.filter(c => {
    // Mock logic for cases needing attention
    const createdDays = new Date().getTime() - new Date(c.createdAt).getTime();
    return createdDays > 30 * 24 * 60 * 60 * 1000; // Over 30 days old
  }) || [];

  return (
    <div className="space-y-6">
      {/* Welcome Header */}
      <div className="bg-gradient-to-r from-primary-600 to-primary-700 rounded-lg p-6 text-white">
        <h1 className="text-2xl font-bold mb-2">Welcome back, {fullName || user?.firstName}</h1>
        <p className="text-primary-100">
          {new Date().toLocaleDateString('en-US', {
            weekday: 'long',
            year: 'numeric',
            month: 'long',
            day: 'numeric',
          })}
        </p>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <DashboardStats
          title="Total Clients"
          value={clientsLoading ? '...' : clients?.length || 0}
          subtitle="Active clients"
          trend={{ value: '12%', isPositive: true }}
          icon={
            <svg fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0z" />
            </svg>
          }
        />
        <DashboardStats
          title="Active Cases"
          value={casesLoading ? '...' : activeCases.length}
          subtitle="Open cases"
          trend={{ value: '8%', isPositive: true }}
          icon={
            <svg fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
          }
        />
        <DashboardStats
          title="My Cases"
          value={casesLoading ? '...' : myCases.length}
          subtitle="Assigned to me"
          icon={
            <svg fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
          }
        />
        <DashboardStats
          title="Needs Attention"
          value={casesLoading ? '...' : casesNeedingAttention.length}
          subtitle="Overdue cases"
          trend={{ value: '3', isPositive: false }}
          icon={
            <svg fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
            </svg>
          }
        />
      </div>

      {/* Main Content Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Quick Actions */}
        <Card>
          <CardHeader>
            <CardTitle>Quick Actions</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 gap-4">
              <Link href="/clients/new">
                <Button variant="outline" className="w-full h-20 flex-col space-y-2">
                  <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                  </svg>
                  <span>Add Client</span>
                </Button>
              </Link>
              <Link href="/cases/new">
                <Button variant="outline" className="w-full h-20 flex-col space-y-2">
                  <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                  </svg>
                  <span>Open Case</span>
                </Button>
              </Link>
              <Link href="/clients">
                <Button variant="outline" className="w-full h-20 flex-col space-y-2">
                  <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                  <span>Search Clients</span>
                </Button>
              </Link>
              <Link href="/reports">
                <Button variant="outline" className="w-full h-20 flex-col space-y-2">
                  <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                  </svg>
                  <span>Reports</span>
                </Button>
              </Link>
            </div>
          </CardContent>
        </Card>

        {/* Recent Activity */}
        <RecentActivity />
      </div>

      {/* Cases Needing Attention */}
      {casesNeedingAttention.length > 0 && (
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="flex items-center">
                <svg className="w-5 h-5 text-warning-500 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
                </svg>
                Cases Needing Attention
              </CardTitle>
              <Badge variant="warning">{casesNeedingAttention.length}</Badge>
            </div>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {casesNeedingAttention.slice(0, 3).map((case_) => (
                <div key={case_.id} className="flex items-center justify-between p-3 border border-warning-200 rounded-lg bg-warning-50">
                  <div>
                    <p className="font-medium text-secondary-900">{case_.description}</p>
                    <p className="text-sm text-secondary-600">
                      Client ID: {case_.clientId} • Opened {new Date(case_.createdAt).toLocaleDateString()}
                    </p>
                  </div>
                  <Link href={`/cases/${case_.id}`}>
                    <Button size="sm">View Case</Button>
                  </Link>
                </div>
              ))}
              {casesNeedingAttention.length > 3 && (
                <div className="text-center pt-2">
                  <Link href="/cases/attention" className="text-sm text-primary-600 hover:text-primary-700">
                    View all {casesNeedingAttention.length} cases needing attention
                  </Link>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}

export default function DashboardPage() {
  return (
    <ProtectedRoute>
      <AppLayout title="Dashboard">
        <div className="p-6">
          <DashboardContent />
        </div>
      </AppLayout>
    </ProtectedRoute>
  );
}
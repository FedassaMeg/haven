import { ProtectedRoute, useCurrentUser } from '@haven/auth';
import { Card, CardHeader, CardTitle, CardContent, Badge, Button } from '@haven/ui';
import { useClients, useCases } from '@haven/api-client';
import AppLayout from '../components/AppLayout';
import Link from 'next/link';
import { useState } from 'react';
import { 
  Bell, Search, FileText, Calendar, Users, AlertTriangle, Heart, Phone, 
  ChevronRight, Clock, CheckCircle2, UserPlus, FolderOpen, BarChart3, 
  Shield, ArrowUp, ArrowDown 
} from 'lucide-react';

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
  const activities: RecentActivityItem[] = [
    {
      id: '1',
      type: 'client_created',
      description: 'Client confirmed safe housing placement',
      timestamp: '2 hours ago',
      user: 'System',
    },
    {
      id: '2', 
      type: 'case_opened',
      description: 'Legal documents submitted to court',
      timestamp: '4 hours ago',
      user: 'Mike Chen',
    },
    {
      id: '3',
      type: 'case_assigned',
      description: 'New crisis protocol guidelines available',
      timestamp: '1 day ago',
      user: 'Admin',
    },
  ];

  return (
    <div className="space-y-4">
      {activities.map((activity) => (
        <div key={activity.id} className="text-sm">
          <p className="font-medium text-slate-800">Case #2024-{activity.id.padStart(4, '0')}</p>
          <p className="text-slate-600">{activity.description}</p>
          <p className="text-xs text-slate-500 mt-1">{activity.timestamp}</p>
        </div>
      ))}
    </div>
  );
};

function DashboardContent() {
  const { user, fullName } = useCurrentUser();
  const { clients, loading: clientsLoading } = useClients({ activeOnly: true });
  const { cases, loading: casesLoading } = useCases({ activeOnly: true });
  const [searchQuery, setSearchQuery] = useState('');

  const activeCases = cases?.filter(c => c.status !== 'CLOSED') || [];
  const myCases = cases?.filter(c => c.assignment?.assigneeId === user?.id) || [];
  const casesNeedingAttention = cases?.filter(c => {
    const createdDays = new Date().getTime() - new Date(c.createdAt).getTime();
    return createdDays > 30 * 24 * 60 * 60 * 1000; // Over 30 days old
  }) || [];
  
  const todaysPriorities = [
    { id: '1', type: 'urgent', title: 'Safety check - Sarah M.', due: '2:00 PM today', icon: AlertTriangle },
    { id: '2', type: 'normal', title: 'Complete case notes - Maria L.', due: 'End of day', icon: FileText },
    { id: '3', type: 'normal', title: 'Team meeting preparation', due: 'Tomorrow 9:00 AM', icon: Users }
  ];

  const greeting = new Date().getHours() < 12 ? 'morning' : new Date().getHours() < 18 ? 'afternoon' : 'evening';

  return (
    <div className="min-h-screen bg-slate-50">
      {/* Enhanced Header with Search */}
      <header className="bg-white border-b border-slate-200 px-6 py-4">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-6 flex-1">
            <h1 className="font-bold text-xl text-blue-600">Haven Portal</h1>
            <div className="relative max-w-md flex-1">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-slate-400 w-4 h-4" />
              <input
                type="text"
                placeholder="Search cases, clients, or notes..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-10 pr-4 py-2 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent w-full text-sm"
              />
            </div>
          </div>
          <div className="flex items-center gap-4">
            <Button variant="ghost" size="sm" className="relative">
              <Bell className="w-4 h-4" />
              <span className="absolute -top-1 -right-1 h-2 w-2 bg-red-500 rounded-full"></span>
            </Button>
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center">
                <span className="text-blue-600 font-medium text-sm">
                  {user?.firstName?.charAt(0)}{user?.lastName?.charAt(0)}
                </span>
              </div>
            </div>
          </div>
        </div>
      </header>

      <div className="max-w-7xl mx-auto px-6 py-8">
        {/* Welcome Section */}
        <div className="mb-8">
          <h2 className="font-bold text-2xl text-slate-800 mb-2">
            Good {greeting}, {fullName || user?.firstName}.
          </h2>
          <p className="text-slate-600">You have {myCases.length} cases to review today.</p>
        </div>

        {/* Enhanced Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
          <Card className="hover:shadow-md transition-shadow">
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-slate-500">Total Clients</p>
                  <p className="text-3xl font-bold text-slate-900 mt-1">
                    {clientsLoading ? '...' : clients?.length || 0}
                  </p>
                  <div className="flex items-center mt-2">
                    <ArrowUp className="w-4 h-4 text-green-500 mr-1" />
                    <span className="text-sm font-medium text-green-600">+12%</span>
                    <span className="text-sm text-slate-500 ml-2">from last month</span>
                  </div>
                </div>
                <div className="p-3 bg-blue-100 rounded-lg">
                  <Users className="w-6 h-6 text-blue-600" />
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="hover:shadow-md transition-shadow">
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-slate-500">Active Cases</p>
                  <p className="text-3xl font-bold text-slate-900 mt-1">
                    {casesLoading ? '...' : activeCases.length}
                  </p>
                  <div className="flex items-center mt-2">
                    <ArrowUp className="w-4 h-4 text-green-500 mr-1" />
                    <span className="text-sm font-medium text-green-600">+8%</span>
                    <span className="text-sm text-slate-500 ml-2">from last month</span>
                  </div>
                </div>
                <div className="p-3 bg-amber-100 rounded-lg">
                  <FolderOpen className="w-6 h-6 text-amber-600" />
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="hover:shadow-md transition-shadow">
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-slate-500">My Cases</p>
                  <p className="text-3xl font-bold text-slate-900 mt-1">
                    {casesLoading ? '...' : myCases.length}
                  </p>
                  <p className="text-sm text-slate-600 mt-1">Assigned to me</p>
                </div>
                <div className="p-3 bg-green-100 rounded-lg">
                  <Shield className="w-6 h-6 text-green-600" />
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="hover:shadow-md transition-shadow">
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-slate-500">Needs Attention</p>
                  <p className="text-3xl font-bold text-slate-900 mt-1">
                    {casesLoading ? '...' : casesNeedingAttention.length}
                  </p>
                  <div className="flex items-center mt-2">
                    <ArrowDown className="w-4 h-4 text-red-500 mr-1" />
                    <span className="text-sm font-medium text-red-600">3</span>
                    <span className="text-sm text-slate-500 ml-2">overdue</span>
                  </div>
                </div>
                <div className="p-3 bg-red-100 rounded-lg">
                  <AlertTriangle className="w-6 h-6 text-red-600" />
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Main Content Area */}
          <div className="lg:col-span-2 space-y-6">
            {/* Active Cases Panel */}
            <Card>
              <CardHeader>
                <CardTitle className="font-bold text-lg text-slate-800">Active Cases</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                {activeCases.slice(0, 3).map((case_, index) => {
                  const statusBadge = case_.status === 'OPEN' ? 
                    { bg: 'bg-amber-100', text: 'text-amber-800', border: 'border-amber-200', label: 'Follow-up needed' } :
                    case_.status === 'IN_PROGRESS' ?
                    { bg: 'bg-green-100', text: 'text-green-800', border: 'border-green-200', label: 'On track' } :
                    { bg: 'bg-cyan-100', text: 'text-cyan-800', border: 'border-cyan-200', label: 'New referral' };
                  
                  return (
                    <div key={case_.id} className="border border-slate-200 rounded-lg p-4 hover:shadow-sm transition-shadow">
                      <div className="flex items-start justify-between mb-3">
                        <div>
                          <h3 className="font-semibold text-slate-800">Case #2024-{case_.id.slice(0, 4)}</h3>
                          <p className="text-sm text-slate-600">{case_.description}</p>
                        </div>
                        <Badge variant="secondary" className={`${statusBadge.bg} ${statusBadge.text} ${statusBadge.border}`}>
                          {statusBadge.label}
                        </Badge>
                      </div>
                      <p className="text-sm text-slate-600 mb-3">
                        Client ID: {case_.clientId?.slice(0, 8)}
                      </p>
                      <div className="flex items-center gap-2 text-xs text-slate-500">
                        <Calendar className="w-3 h-3" />
                        <span>Last updated: {new Date(case_.updatedAt || case_.createdAt).toLocaleString()}</span>
                      </div>
                    </div>
                  );
                })}
                {activeCases.length === 0 && (
                  <p className="text-sm text-slate-500 text-center py-4">No active cases</p>
                )}
              </CardContent>
            </Card>

            {/* Today's Priorities */}
            <Card>
              <CardHeader>
                <CardTitle className="font-bold text-lg text-slate-800">Today's Priorities</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                {todaysPriorities.map((priority) => {
                  const Icon = priority.icon;
                  return (
                    <div 
                      key={priority.id} 
                      className={`flex items-center gap-3 p-3 rounded-lg border ${
                        priority.type === 'urgent' 
                          ? 'bg-amber-50 border-amber-200' 
                          : 'border-slate-200'
                      }`}
                    >
                      <Icon className={`w-4 h-4 flex-shrink-0 ${
                        priority.type === 'urgent' ? 'text-amber-600' : 'text-slate-500'
                      }`} />
                      <div className="flex-1">
                        <p className="font-medium text-slate-800">{priority.title}</p>
                        <p className="text-sm text-slate-600">Due: {priority.due}</p>
                      </div>
                    </div>
                  );
                })}
              </CardContent>
            </Card>
          </div>

          {/* Sidebar */}
          <div className="space-y-6">
            {/* Recent Updates */}
            <Card>
              <CardHeader>
                <CardTitle className="font-bold text-lg text-slate-800">Recent Updates</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <RecentActivity />
              </CardContent>
            </Card>

            {/* Wellbeing Reminder */}
            <Card className="bg-blue-50 border-blue-200">
              <CardHeader>
                <CardTitle className="font-bold text-lg text-blue-800 flex items-center gap-2">
                  <Heart className="w-5 h-5" />
                  Wellbeing Check
                </CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-sm text-blue-700 mb-4">
                  You've handled {myCases.length} cases today. Remember to take breaks when needed.
                </p>
                <Button
                  variant="outline"
                  size="sm"
                  className="w-full border-blue-300 text-blue-700 hover:bg-blue-100 bg-transparent"
                >
                  Take a Breathing Break
                </Button>
              </CardContent>
            </Card>

            {/* Quick Resources */}
            <Card>
              <CardHeader>
                <CardTitle className="font-bold text-lg text-slate-800">Quick Resources</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <Button variant="ghost" className="w-full justify-start text-left h-auto p-3 hover:bg-slate-50">
                  <Phone className="w-4 h-4 mr-3 flex-shrink-0" />
                  <div>
                    <p className="font-medium">Crisis Hotline</p>
                    <p className="text-xs text-slate-500">24/7 Support</p>
                  </div>
                </Button>
                <Button variant="ghost" className="w-full justify-start text-left h-auto p-3 hover:bg-slate-50">
                  <FileText className="w-4 h-4 mr-3 flex-shrink-0" />
                  <div>
                    <p className="font-medium">Legal Templates</p>
                    <p className="text-xs text-slate-500">Forms & Documents</p>
                  </div>
                </Button>
                <Button variant="ghost" className="w-full justify-start text-left h-auto p-3 hover:bg-slate-50">
                  <Users className="w-4 h-4 mr-3 flex-shrink-0" />
                  <div>
                    <p className="font-medium">Peer Support</p>
                    <p className="text-xs text-slate-500">Connect with colleagues</p>
                  </div>
                </Button>
              </CardContent>
            </Card>
          </div>
        </div>

        {/* Cases Needing Attention */}
        {casesNeedingAttention.length > 0 && (
          <Card className="mt-6">
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle className="flex items-center font-bold text-lg text-slate-800">
                  <AlertTriangle className="w-5 h-5 text-amber-500 mr-2" />
                  Cases Needing Attention
                </CardTitle>
                <Badge variant="warning" className="bg-amber-100 text-amber-800">
                  {casesNeedingAttention.length}
                </Badge>
              </div>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                {casesNeedingAttention.slice(0, 3).map((case_) => (
                  <div key={case_.id} className="flex items-center justify-between p-3 border border-amber-200 rounded-lg bg-amber-50">
                    <div>
                      <p className="font-medium text-slate-900">{case_.description}</p>
                      <p className="text-sm text-slate-600">
                        Client ID: {case_.clientId?.slice(0, 8)} • Opened {new Date(case_.createdAt).toLocaleDateString()}
                      </p>
                    </div>
                    <Link href={`/cases/${case_.id}`}>
                      <Button size="sm" className="bg-amber-600 hover:bg-amber-700">View Case</Button>
                    </Link>
                  </div>
                ))}
                {casesNeedingAttention.length > 3 && (
                  <div className="text-center pt-2">
                    <Link href="/cases/attention" className="text-sm text-blue-600 hover:text-blue-700 font-medium">
                      View all {casesNeedingAttention.length} cases needing attention
                    </Link>
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
}

export default function DashboardPage() {
  return (
    <ProtectedRoute>
      <AppLayout title="Dashboard" hideHeader={true}>
        <DashboardContent />
      </AppLayout>
    </ProtectedRoute>
  );
}
import { ProtectedRoute, useCurrentUser } from '@haven/auth';
import { Card, CardHeader, CardTitle, CardContent, Badge, Button } from '@haven/ui';
import { useCases } from '@haven/api-client';
import AppLayout from '../components/AppLayout';
import { useState, useEffect } from 'react';
// import { 
//   Bell, Search, FileText, Calendar, Users, AlertTriangle, Heart, Phone 
// } from 'lucide-react';


function DashboardContent() {
  const { user, fullName } = useCurrentUser();
  const { cases, loading: casesLoading } = useCases({ activeOnly: true });
  const [greeting, setGreeting] = useState('');

  const activeCases = cases?.filter(c => c.status !== 'CLOSED') || [];
  const myCases = cases?.filter(c => c.assignment?.assigneeId === user?.id) || [];
  
  const todaysPriorities = [
    { id: '1', type: 'urgent', title: 'Safety check - Sarah M.', due: '2:00 PM today' },
    { id: '2', type: 'normal', title: 'Complete case notes - Maria L.', due: 'End of day' },
    { id: '3', type: 'normal', title: 'Team meeting preparation', due: 'Tomorrow 9:00 AM' }
  ];

  useEffect(() => {
    const hour = new Date().getHours();
    setGreeting(hour < 12 ? 'morning' : hour < 18 ? 'afternoon' : 'evening');
  }, []);

  return (
    <div className="p-6">
      <div className="max-w-7xl mx-auto">
        {/* Welcome Section */}
        <div className="mb-8">
          <h2 className="font-heading font-black text-2xl text-slate-800 mb-2">
            Good {greeting}, {fullName || user?.firstName}.
          </h2>
          <p className="text-slate-600">You have {myCases.length} cases to review today.</p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Main Content Area */}
          <div className="lg:col-span-2 space-y-6">
            {/* Case Overview Panel */}
            <Card>
              <CardHeader>
                <CardTitle className="font-heading font-bold text-lg text-slate-800">Active Cases</CardTitle>
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
                        {case_.description || `Client ID: ${case_.clientId?.slice(0, 8)}`}
                      </p>
                      <div className="flex items-center gap-2 text-xs text-slate-500">
                        {/* <Calendar className="w-3 h-3" /> */}
                        {/* <span>Last updated: {new Date(case_.updatedAt || case_.createdAt).toLocaleString()}</span> */}
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
                <CardTitle className="font-heading font-bold text-lg text-slate-800">Today's Priorities</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                {todaysPriorities.map((priority) => {
                  // const Icon = priority.icon;
                  return (
                    <div 
                      key={priority.id} 
                      className={`flex items-center gap-3 p-3 rounded-lg border ${
                        priority.type === 'urgent' 
                          ? 'bg-amber-50 border-amber-200' 
                          : 'border-slate-200'
                      }`}
                    >
                      {/* <Icon className={`w-4 h-4 flex-shrink-0 ${
                        priority.type === 'urgent' ? 'text-amber-600' : 'text-slate-500'
                      }`} /> */}
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
                <CardTitle className="font-heading font-bold text-lg text-slate-800">Recent Updates</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="text-sm">
                  <p className="font-medium text-slate-800">Case #2024-0156</p>
                  <p className="text-slate-600">Client confirmed safe housing placement</p>
                  <p className="text-xs text-slate-500 mt-1">2 hours ago</p>
                </div>
                <div className="text-sm">
                  <p className="font-medium text-slate-800">Case #2024-0142</p>
                  <p className="text-slate-600">Legal documents submitted to court</p>
                  <p className="text-xs text-slate-500 mt-1">4 hours ago</p>
                </div>
                <div className="text-sm">
                  <p className="font-medium text-slate-800">System Update</p>
                  <p className="text-slate-600">New crisis protocol guidelines available</p>
                  <p className="text-xs text-slate-500 mt-1">1 day ago</p>
                </div>
              </CardContent>
            </Card>

            {/* Wellbeing Reminder */}
            <Card className="bg-blue-50 border-blue-200">
              <CardHeader>
                <CardTitle className="font-heading font-bold text-lg text-blue-800 flex items-center gap-2">
                  {/* <Heart className="w-5 h-5" /> */}
                  Wellbeing Check
                </CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-sm text-blue-700 mb-4">
                  You've handled {myCases.length} intense cases today. Take a 2-minute pause when you're ready.
                </p>
                <Button
                  variant="outline"
                  size="sm"
                  className="w-full border-blue-300 text-blue-700 hover:bg-blue-100 bg-transparent"
                >
                  Breathing Exercise
                </Button>
              </CardContent>
            </Card>

            {/* Quick Resources */}
            <Card>
              <CardHeader>
                <CardTitle className="font-heading font-bold text-lg text-slate-800">Quick Resources</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <Button variant="ghost" className="w-full justify-start text-left h-auto p-3">
                  {/* <Phone className="w-4 h-4 mr-3 flex-shrink-0" /> */}
                  <div>
                    <p className="font-medium">Crisis Hotline</p>
                    <p className="text-xs text-slate-500">24/7 Support</p>
                  </div>
                </Button>
                <Button variant="ghost" className="w-full justify-start text-left h-auto p-3">
                  {/* <FileText className="w-4 h-4 mr-3 flex-shrink-0" /> */}
                  <div>
                    <p className="font-medium">Legal Templates</p>
                    <p className="text-xs text-slate-500">Forms & Documents</p>
                  </div>
                </Button>
                <Button variant="ghost" className="w-full justify-start text-left h-auto p-3">
                  {/* <Users className="w-4 h-4 mr-3 flex-shrink-0" /> */}
                  <div>
                    <p className="font-medium">Peer Support</p>
                    <p className="text-xs text-slate-500">Connect with colleagues</p>
                  </div>
                </Button>
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}

export default function DashboardPage() {
  return (
    <ProtectedRoute>
      <AppLayout title="Dashboard">
        <DashboardContent />
      </AppLayout>
    </ProtectedRoute>
  );
};
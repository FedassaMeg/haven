import { ProtectedRoute, useCurrentUser, usePermissions } from '@haven/auth';
import AppLayout from '../components/AppLayout';
import { useState, useEffect, useMemo } from 'react';
import { getDashboardConfig, filterWidgetsByPermissions } from '../config/dashboard-config';
import { WidgetRenderer } from '../components/dashboard/WidgetRenderer';

function DashboardContent() {
  const { user, fullName, isAdmin, isCaseManager } = useCurrentUser();
  const { hasPermission } = usePermissions();
  const [greeting, setGreeting] = useState('');

  // Get role-based dashboard configuration
  const dashboardConfig = useMemo(() => {
    if (!user) return null;
    return getDashboardConfig(user.roles || []);
  }, [user]);

  // Filter widgets based on user permissions
  const visibleWidgets = useMemo(() => {
    if (!dashboardConfig || !user) return [];
    
    // Mock user permissions - in a real app, this would come from the user object or API
    const userPermissions = [
      'case:read',
      ...(isAdmin ? ['system:read', 'system:admin', 'system:monitor', 'user:manage', 'case:read-all'] : []),
      ...(isCaseManager ? ['case:assign'] : [])
    ];
    
    return filterWidgetsByPermissions(dashboardConfig.layout.widgets, userPermissions)
      .sort((a, b) => a.order - b.order);
  }, [dashboardConfig, user, isAdmin, isCaseManager]);

  useEffect(() => {
    const hour = new Date().getHours();
    setGreeting(hour < 12 ? 'morning' : hour < 18 ? 'afternoon' : 'evening');
  }, []);

  if (!dashboardConfig) {
    return (
      <div className="p-6">
        <div className="max-w-7xl mx-auto">
          <div className="text-center py-8">
            <div className="text-slate-500">Loading dashboard...</div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6">
      <div className="max-w-7xl mx-auto">
        {/* Welcome Section with Role Context */}
        <div className="mb-8">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="font-heading font-black text-2xl text-slate-800 mb-2">
                Good {greeting}, {fullName || user?.firstName}.
              </h2>
              <p className="text-slate-600">{dashboardConfig.description}</p>
            </div>
            {dashboardConfig.customizable && (
              <div className="text-xs text-slate-500">
                Dashboard: {dashboardConfig.role}
              </div>
            )}
          </div>
        </div>

        {/* Dynamic Widget Grid */}
        <div className={`grid gap-6 grid-cols-1 ${
          dashboardConfig.layout.columns === 4 ? 'lg:grid-cols-4' :
          dashboardConfig.layout.columns === 3 ? 'lg:grid-cols-3' :
          'lg:grid-cols-2'
        }`}>
          {visibleWidgets.map((widget) => (
            <WidgetRenderer key={widget.id} widget={widget} />
          ))}
          
          {visibleWidgets.length === 0 && (
            <div className="col-span-full text-center py-8">
              <div className="text-slate-500">No widgets available for your role.</div>
            </div>
          )}
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
import React from 'react';
import { useRouter } from 'next/router';
import { Layout, type NavigationItem } from '@haven/ui';
import { UserProfileDropdown, useCurrentUser, PermissionGuard } from '@haven/auth';
import { ResourceType, ActionType } from '@haven/auth';
import { 
  Home, 
  Users, 
  FileText, 
  Zap, 
  AlertTriangle, 
  DollarSign, 
  Archive, 
  Shield, 
  BarChart3, 
  Settings,
  FileBarChart,
  Search,
  Bell
} from 'lucide-react';

interface AppLayoutProps {
  children: React.ReactNode;
  title?: string;
  breadcrumbs?: Array<{ label: string; href?: string }>;
}

export const AppLayout: React.FC<AppLayoutProps> = ({ children, title, breadcrumbs }) => {
  const router = useRouter();
  const { user, fullName } = useCurrentUser();
  const [searchQuery, setSearchQuery] = React.useState('');

  // Navigation items with permission-based visibility
  const navigationItems: NavigationItem[] = [
    {
      label: 'Dashboard',
      href: '/dashboard',
      active: router.pathname === '/dashboard',
      icon: <Home className="w-5 h-5" />,
    },
    {
      label: 'Clients',
      href: '/clients',
      active: router.pathname.startsWith('/clients'),
      icon: <Users className="w-5 h-5" />,
      children: [
        {
          label: 'All Clients',
          href: '/clients',
          active: router.pathname === '/clients',
        },
        {
          label: 'New Intake',
          href: '/intake',
          active: router.pathname === '/intake',
        },
      ],
    },
    {
      label: 'Cases',
      href: '/cases',
      active: router.pathname.startsWith('/cases'),
      icon: <FileText className="w-5 h-5" />,
      children: [
        {
          label: 'Active Cases',
          href: '/cases',
          active: router.pathname === '/cases',
        },
        {
          label: 'My Cases',
          href: '/cases/my-cases',
          active: router.pathname === '/cases/my-cases',
        },
        {
          label: 'Needs Attention',
          href: '/cases/attention',
          active: router.pathname === '/cases/attention',
        },
      ],
    },
    {
      label: 'Services',
      href: '/services',
      active: router.pathname.startsWith('/services'),
      icon: <Zap className="w-5 h-5" />,
      children: [
        {
          label: 'All Services',
          href: '/services',
          active: router.pathname === '/services',
        },
        {
          label: 'Active Services',
          href: '/services?status=IN_PROGRESS',
          active: router.pathname === '/services' && router.query.status === 'IN_PROGRESS',
        },
        {
          label: 'Create Service',
          href: '/services/new',
          active: router.pathname === '/services/new',
        },
        {
          label: 'My Services',
          href: '/services?providerId=' + user?.id,
          active: router.pathname === '/services' && router.query.providerId === user?.id,
        },
      ],
    },
    {
      label: 'Mandated Reports',
      href: '/mandated-reports',
      active: router.pathname.startsWith('/mandated-reports'),
      icon: <FileBarChart className="w-5 h-5" />,
      children: [
        {
          label: 'All Reports',
          href: '/mandated-reports',
          active: router.pathname === '/mandated-reports',
        },
        {
          label: 'Create Report',
          href: '/mandated-reports/new',
          active: router.pathname === '/mandated-reports/new',
        },
        {
          label: 'Pending Review',
          href: '/mandated-reports?status=PENDING_REVIEW',
          active: router.pathname === '/mandated-reports' && router.query.status === 'PENDING_REVIEW',
        },
      ],
    },
    {
      label: 'Billing',
      href: '/billing',
      active: router.pathname.startsWith('/billing'),
      icon: <DollarSign className="w-5 h-5" />,
      children: [
        {
          label: 'Billing Dashboard',
          href: '/billing',
          active: router.pathname === '/billing',
        },
        {
          label: 'Reports & Analytics',
          href: '/billing/reports',
          active: router.pathname === '/billing/reports',
        },
        {
          label: 'Export Data',
          href: '/billing/export',
          active: router.pathname === '/billing/export',
        },
      ],
    },
    {
      label: 'Triage Center',
      href: '/triage',
      active: router.pathname.startsWith('/triage'),
      icon: <AlertTriangle className="w-5 h-5" />,
    },
    {
      label: 'Caseload',
      href: '/caseload',
      active: router.pathname.startsWith('/caseload'),
      icon: <Archive className="w-5 h-5" />,
      children: [
        {
          label: 'My Caseload',
          href: '/caseload/my-cases',
          active: router.pathname === '/caseload/my-cases',
        },
        {
          label: 'Team Overview',
          href: '/caseload/team',
          active: router.pathname === '/caseload/team',
        },
        {
          label: 'High Risk',
          href: '/caseload/high-risk',
          active: router.pathname === '/caseload/high-risk',
        },
      ],
    },
  ];

  // Admin-only navigation items
  const adminNavigationItems: NavigationItem[] = [
    {
      label: 'Compliance',
      href: '/compliance',
      active: router.pathname.startsWith('/compliance'),
      icon: <Shield className="w-5 h-5" />,
      children: [
        {
          label: 'Funding Overview',
          href: '/compliance/funding',
          active: router.pathname === '/compliance/funding',
        },
        {
          label: 'Documentation',
          href: '/compliance/documentation',
          active: router.pathname === '/compliance/documentation',
        },
        {
          label: 'Export Reports',
          href: '/compliance/exports',
          active: router.pathname === '/compliance/exports',
        },
      ],
    },
    {
      label: 'Reports',
      href: '/reports',
      active: router.pathname.startsWith('/reports'),
      icon: <BarChart3 className="w-5 h-5" />,
    },
    {
      label: 'Administration',
      href: '/admin',
      active: router.pathname.startsWith('/admin'),
      icon: <Settings className="w-5 h-5" />,
      children: [
        {
          label: 'User Management',
          href: '/admin/users',
          active: router.pathname === '/admin/users',
        },
        {
          label: 'System Settings',
          href: '/admin/settings',
          active: router.pathname === '/admin/settings',
        },
      ],
    },
  ];

  // Filter navigation items based on permissions
  const getVisibleNavigationItems = () => {
    const items = [...navigationItems];
    
    // Add admin/supervisor items if user has permission
    const hasAdminAccess = user?.roles.includes('admin') || user?.roles.includes('supervisor');
    
    if (hasAdminAccess) {
      items.push(...adminNavigationItems);
    }
    
    return items;
  };

  const logo = (
    <div className="flex items-center gap-2 px-2 py-2">
      <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-primary-foreground font-bold">
        H
      </div>
      <div className="grid flex-1 text-left text-sm leading-tight group-data-[collapsible=icon]:opacity-0 group-data-[collapsible=icon]:w-0 overflow-hidden transition-all">
        <span className="truncate font-semibold">Haven</span>
        <span className="truncate text-xs text-muted-foreground">Case Management</span>
      </div>
    </div>
  );

  const footer = (
    <div className="text-center text-xs text-secondary-500">
      <p>&copy; 2024 Haven CMS</p>
      <p>v1.0.0</p>
    </div>
  );

  return (
    <Layout
      sidebar={{
        navigation: getVisibleNavigationItems(),
        logo,
        footer,
      }}
      header={{
        title,
        breadcrumbs,
        actions: (
          <div className="flex items-center space-x-4">
            {/* Search */}
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-secondary-400 w-4 h-4" />
              <input
                type="text"
                placeholder="Search..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-10 pr-4 py-2 border border-secondary-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent w-64"
              />
            </div>
            
            {/* Notifications */}
            <button className="p-2 text-secondary-400 hover:text-secondary-500 relative">
              <Bell className="w-6 h-6" />
              <span className="absolute top-0 right-0 block h-2 w-2 bg-error-500 rounded-full"></span>
            </button>

            {/* User menu */}
            <UserProfileDropdown />
          </div>
        ),
      }}
    >
      {children}
    </Layout>
  );
};

export default AppLayout;
import React from 'react';
import { useRouter } from 'next/router';
import { Layout, type NavigationItem } from '@haven/ui';
import { UserProfileDropdown, useCurrentUser, PermissionGuard } from '@haven/auth';
import { ResourceType, ActionType } from '@haven/auth';

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
      icon: (
        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
        </svg>
      ),
    },
    {
      label: 'Clients',
      href: '/clients',
      active: router.pathname.startsWith('/clients'),
      icon: (
        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0z" />
        </svg>
      ),
      children: [
        {
          label: 'All Clients',
          href: '/clients',
          active: router.pathname === '/clients',
        },
        {
          label: 'Add Client',
          href: '/clients/new',
          active: router.pathname === '/clients/new',
        },
      ],
    },
    {
      label: 'Cases',
      href: '/cases',
      active: router.pathname.startsWith('/cases'),
      icon: (
        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
      ),
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
      label: 'Triage Center',
      href: '/triage',
      active: router.pathname.startsWith('/triage'),
      icon: (
        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
        </svg>
      ),
    },
    {
      label: 'Caseload',
      href: '/caseload',
      active: router.pathname.startsWith('/caseload'),
      icon: (
        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
        </svg>
      ),
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
      icon: (
        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
        </svg>
      ),
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
      icon: (
        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
        </svg>
      ),
    },
    {
      label: 'Administration',
      href: '/admin',
      active: router.pathname.startsWith('/admin'),
      icon: (
        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
        </svg>
      ),
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
    <div className="flex items-center space-x-2">
      <div className="w-8 h-8 bg-primary-600 rounded flex items-center justify-center">
        <span className="text-white font-bold text-lg">H</span>
      </div>
      <span className="text-xl font-bold text-secondary-900">Haven</span>
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
              <svg className="absolute left-3 top-1/2 transform -translate-y-1/2 text-secondary-400 w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
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
              <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
              </svg>
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
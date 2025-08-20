import React from 'react';
import { cva, type VariantProps } from 'class-variance-authority';
import { SidebarNavigation, type NavigationItem, Breadcrumb, type BreadcrumbItem } from './Navigation';
import { SearchBar, NotificationsDropdown, UserProfileDropdown, type NotificationItem, type UserProfile } from './Header';
import { useBreakpoint } from '../hooks/useMediaQuery';
import { twMerge } from 'tailwind-merge';

export interface LayoutProps {
  children: React.ReactNode;
  sidebar?: {
    navigation?: NavigationItem[];
    logo?: React.ReactNode;
    footer?: React.ReactNode;
  };
  header?: {
    title?: string;
    breadcrumbs?: BreadcrumbItem[];
    actions?: React.ReactNode;
    showSearch?: boolean;
    onSearch?: (query: string) => void;
    notifications?: NotificationItem[];
    onMarkAsRead?: (id: string) => void;
    onMarkAllAsRead?: () => void;
    user?: UserProfile;
    onSignOut?: () => void;
    onProfile?: () => void;
    onSettings?: () => void;
  };
  className?: string;
}

const layoutVariants = cva(
  'min-h-screen bg-gray-50 grid',
  {
    variants: {
      layout: {
        sidebarWithHeader: 'grid-cols-1 grid-rows-[auto_1fr] [grid-template-areas:"header"_"content"] lg:grid-cols-[min-content_minmax(0,1fr)_auto] lg:grid-rows-[auto_1fr] lg:[grid-template-areas:"sidebar_header_header"_"sidebar_content_content"]',
        sidebarOnly: 'grid-cols-1 grid-rows-[auto_1fr] [grid-template-areas:"header"_"content"] lg:grid-cols-[min-content_minmax(0,1fr)_auto] lg:grid-rows-1 lg:[grid-template-areas:"sidebar_content_content"]',
        headerOnly: 'grid-cols-1 grid-rows-[auto_1fr] [grid-template-areas:"header"_"content"] lg:grid-cols-[auto_minmax(0,1fr)_auto] lg:[grid-template-areas:"header_header_header"_"._content_."]',
        contentOnly: 'grid-cols-1 grid-rows-1 [grid-template-areas:"content"] lg:grid-cols-[auto_minmax(0,1fr)_auto] lg:[grid-template-areas:"._content_."]',
      },
    },
    defaultVariants: {
      layout: 'contentOnly',
    },
  }
);

const sidebarVariants = cva(
  'bg-white border-r border-gray-200 transition-all duration-300',
  {
    variants: {
      collapsed: {
        true: '',
        false: '',
      },
    },
    defaultVariants: {
      collapsed: false,
    },
  }
);

const sidebarContentVariants = cva(
  'flex-1 overflow-y-auto',
  {
    variants: {
      collapsed: {
        true: 'px-3 py-4',
        false: 'px-4 py-6',
      },
    },
    defaultVariants: {
      collapsed: false,
    },
  }
);

const mobileOverlayVariants = cva(
  'fixed inset-0 z-50 lg:hidden',
  {
    variants: {
      open: {
        true: 'block',
        false: 'hidden',
      },
    },
    defaultVariants: {
      open: false,
    },
  }
);

export const Layout: React.FC<LayoutProps> = ({
  children,
  sidebar,
  header,
  className,
}) => {
  const [sidebarOpen, setSidebarOpen] = React.useState(false);
  const [sidebarCollapsed, setSidebarCollapsed] = React.useState(false);
  const { isDesktop } = useBreakpoint();

  // Determine layout type based on props
  const getLayoutType = () => {
    if (sidebar && header) return 'sidebarWithHeader';
    if (sidebar && !header) return 'sidebarOnly';
    if (!sidebar && header) return 'headerOnly';
    return 'contentOnly';
  };

  // Default navigation items
  const defaultNavigation: NavigationItem[] = [
    {
      label: 'Home',
      href: '/',
      icon: (
        <svg fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
        </svg>
      ),
    },
    {
      label: 'Clients',
      href: '/clients',
      icon: (
        <svg fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
        </svg>
      ),
    },
    {
      label: 'Cases',
      href: '/cases',
      icon: (
        <svg fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
      ),
    },
  ];

  const navigationItems = sidebar?.navigation || defaultNavigation;

  const renderMobileSidebarContent = () => (
    <>
      <div className="flex items-center justify-between h-16 px-4 border-b border-secondary-200">
        {sidebar?.logo || (
          <div className="flex items-center">
            <div className="w-8 h-8 rounded-md bg-primary-600 flex items-center justify-center text-white font-bold text-lg mr-2">
              H
            </div>
            <span className="text-xl font-semibold text-secondary-900">Haven</span>
          </div>
        )}
        <button
          onClick={() => setSidebarOpen(false)}
          className="text-secondary-400 hover:text-secondary-600"
        >
          <span className="sr-only">Close sidebar</span>
          <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>
      <div className={sidebarContentVariants({ collapsed: false })}>
        <SidebarNavigation items={navigationItems} collapsed={false} />
      </div>
      {sidebar?.footer && (
        <div className="flex-shrink-0 p-4 border-t border-secondary-200">
          {sidebar.footer}
        </div>
      )}
    </>
  );

  const renderHeader = () => {
    if (!header && sidebar && !isDesktop) {
      // Mobile-only header with just menu button
      return (
        <header className="bg-white border-b border-secondary-200 lg:hidden">
          <div className="h-16 px-4 flex items-center">
            <button
              onClick={() => setSidebarOpen(true)}
              className="text-secondary-400 hover:text-secondary-600"
            >
              <span className="sr-only">Open sidebar</span>
              <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
              </svg>
            </button>
          </div>
        </header>
      );
    }

    if (!header) return null;

    return (
      <header className="bg-white border-b border-secondary-200 shadow-sm">
        <div className="h-16 px-4 sm:px-6 lg:px-8 flex items-center justify-between">
          {/* Left side */}
          <div className="flex items-center flex-1">
            {/* Mobile menu button */}
            {sidebar && !isDesktop && (
              <button
                onClick={() => setSidebarOpen(true)}
                className="text-secondary-400 hover:text-secondary-600 mr-4"
              >
                <span className="sr-only">Open sidebar</span>
                <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                </svg>
              </button>
            )}
            
            {/* Title and breadcrumbs */}
            <div className="flex-1">
              {header?.breadcrumbs && <Breadcrumb items={header.breadcrumbs} className="mb-1" />}
              {header?.title && (
                <h1 className="text-2xl font-semibold text-secondary-900">{header.title}</h1>
              )}
            </div>

          {/* Search Bar */}
          {header?.showSearch && (
            <SearchBar
              onSearch={header.onSearch}
              className="ml-4 max-w-md flex-1 hidden md:block"
            />
          )}
        </div>
        
        {/* Right side */}
        <div className="flex items-center space-x-4 ml-4">
          {/* Custom Actions */}
          {header?.actions && (
            <div className="flex items-center space-x-2">
              {header.actions}
            </div>
          )}

          {/* Notifications */}
          {header?.notifications && (
            <NotificationsDropdown
              notifications={header.notifications}
              onMarkAsRead={header.onMarkAsRead}
              onMarkAllAsRead={header.onMarkAllAsRead}
            />
          )}

          {/* User Profile */}
          {header?.user && (
            <UserProfileDropdown
              user={header.user}
              onSignOut={header.onSignOut}
              onProfile={header.onProfile}
              onSettings={header.onSettings}
            />
          )}
        </div>
      </div>
    </header>
    );
  };

  return (
    <div className={layoutVariants({ layout: getLayoutType(), className })}>
      {/* Mobile sidebar overlay */}
      {sidebar && !isDesktop && (
        <div
          className={mobileOverlayVariants({ open: sidebarOpen })}
        >
          {/* Backdrop */}
          <div 
            className="fixed inset-0 bg-gray-600 bg-opacity-75" 
            onClick={() => setSidebarOpen(false)} 
          />
          
          {/* Sidebar panel */}
          <div className="fixed inset-y-0 left-0 flex flex-col w-64 max-w-xs bg-white">
            {/* Sidebar content with flex column layout */}
            {renderMobileSidebarContent()}
          </div>
        </div>
      )}

      {/* Desktop sidebar */}
      {sidebar && isDesktop && (
        <aside className={`${sidebarVariants({ collapsed: sidebarCollapsed })} [grid-area:sidebar] ${header ? 'grid grid-rows-subgrid' : ''}`}>
          {/* Logo section - aligns with header row */}
          {header && (
            <div className="flex items-center justify-between px-3 border-b border-secondary-200 bg-white">
            {sidebarCollapsed && isDesktop ? (
              <div className="w-10 h-10 rounded-md bg-primary-600 flex items-center justify-center text-white font-bold text-lg flex-shrink-0">
                H
              </div>
            ) : (
              sidebar?.logo || (
                <div className="flex items-center">
                  <div className="w-8 h-8 rounded-md bg-primary-600 flex items-center justify-center text-white font-bold text-lg mr-2 flex-shrink-0">
                    H
                  </div>
                  <span className="text-xl font-semibold text-secondary-900 whitespace-nowrap">Haven</span>
                </div>
              )
            )}
            <button
              onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
              className="text-secondary-400 hover:text-secondary-600 p-1 flex-shrink-0"
            >
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                {sidebarCollapsed ? (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 5l7 7-7 7M5 5l7 7-7 7" />
                ) : (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 19l-7-7 7-7m8 14l-7-7 7-7" />
                )}
              </svg>
            </button>
          </div>
          )}
          
          {/* Navigation section - spans remaining height */}
          <div className={`flex flex-col overflow-hidden bg-white ${!header ? 'h-full' : ''}`}>
            <div className={sidebarContentVariants({ collapsed: sidebarCollapsed && isDesktop })}>
              <SidebarNavigation items={navigationItems} collapsed={sidebarCollapsed && isDesktop} />
            </div>
            {sidebar?.footer && (!sidebarCollapsed || !isDesktop) && (
              <div className="flex-shrink-0 p-4 border-t border-secondary-200">
                {sidebar.footer}
              </div>
            )}
          </div>
        </aside>
      )}

      {/* Header */}
      {(header || (sidebar && !isDesktop)) && (
        <div className="[grid-area:header]">
          {renderHeader()}
        </div>
      )}

      {/* Main content area - single div with max-width constraint */}
      <main className="[grid-area:content] overflow-auto bg-gray-50 min-h-0 px-4 sm:px-6 lg:px-8 py-6 max-w-7xl justify-self-center w-full">
        {children}
      </main>
    </div>
  );
};

export interface PageProps {
  children: React.ReactNode;
  title?: string;
  description?: string;
  actions?: React.ReactNode;
  className?: string;
}

export const Page: React.FC<PageProps> = ({
  children,
  title,
  description,
  actions,
  className,
}) => {
  return (
    <div className={twMerge('grid gap-8', className)}>
      {(title || description || actions) && (
        <header className="flex items-start justify-between gap-4 flex-wrap">
          <div className="min-w-0 flex-1">
            {title && <h1 className="text-3xl font-bold text-secondary-900">{title}</h1>}
            {description && (
              <p className="mt-2 text-secondary-600">{description}</p>
            )}
          </div>
          {actions && <div className="flex items-center gap-4">{actions}</div>}
        </header>
      )}
      {children}
    </div>
  );
};

export interface EmptyStateProps {
  icon?: React.ReactNode;
  title: string;
  description: string;
  action?: {
    label: string;
    onClick: () => void;
  };
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon,
  title,
  description,
  action,
}) => {
  return (
    <div className="text-center py-12">
      {icon && (
        <div className="mx-auto h-12 w-12 text-secondary-400 mb-4">
          {icon}
        </div>
      )}
      <h3 className="text-lg font-medium text-secondary-900 mb-2">{title}</h3>
      <p className="text-secondary-500 mb-6">{description}</p>
      {action && (
        <button onClick={action.onClick} className="btn-primary">
          {action.label}
        </button>
      )}
    </div>
  );
};
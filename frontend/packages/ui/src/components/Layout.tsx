import React from 'react';
import { clsx } from 'clsx';
import { SidebarNavigation, type NavigationItem, Breadcrumb, type BreadcrumbItem } from './Navigation';

export interface LayoutProps {
  children: React.ReactNode;
  sidebar?: {
    navigation: NavigationItem[];
    logo?: React.ReactNode;
    footer?: React.ReactNode;
  };
  header?: {
    title?: string;
    breadcrumbs?: BreadcrumbItem[];
    actions?: React.ReactNode;
  };
  className?: string;
}

export const Layout: React.FC<LayoutProps> = ({
  children,
  sidebar,
  header,
  className,
}) => {
  const [sidebarOpen, setSidebarOpen] = React.useState(false);

  return (
    <div className={clsx('min-h-screen bg-secondary-50', className)}>
      {/* Mobile sidebar */}
      {sidebar && (
        <>
          <div
            className={clsx(
              'fixed inset-0 z-40 lg:hidden',
              sidebarOpen ? 'block' : 'hidden'
            )}
          >
            <div className="fixed inset-0 bg-secondary-600 bg-opacity-75" onClick={() => setSidebarOpen(false)} />
            <div className="relative flex flex-col w-64 max-w-xs bg-white">
              <div className="flex items-center justify-between h-16 px-4 bg-white border-b border-secondary-200">
                {sidebar.logo}
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
              <div className="flex-1 px-4 py-6 overflow-y-auto">
                <SidebarNavigation items={sidebar.navigation} />
              </div>
              {sidebar.footer && (
                <div className="flex-shrink-0 p-4 border-t border-secondary-200">
                  {sidebar.footer}
                </div>
              )}
            </div>
          </div>

          {/* Desktop sidebar */}
          <div className="hidden lg:fixed lg:inset-y-0 lg:flex lg:w-64 lg:flex-col">
            <div className="flex flex-col flex-1 min-h-0 bg-white border-r border-secondary-200">
              {sidebar.logo && (
                <div className="flex items-center h-16 px-4 bg-white border-b border-secondary-200">
                  {sidebar.logo}
                </div>
              )}
              <div className="flex-1 px-4 py-6 overflow-y-auto">
                <SidebarNavigation items={sidebar.navigation} />
              </div>
              {sidebar.footer && (
                <div className="flex-shrink-0 p-4 border-t border-secondary-200">
                  {sidebar.footer}
                </div>
              )}
            </div>
          </div>
        </>
      )}

      {/* Main content */}
      <div className={clsx(sidebar && 'lg:pl-64')}>
        {/* Header */}
        {(header || sidebar) && (
          <div className="sticky top-0 z-10 bg-white border-b border-secondary-200">
            <div className="flex items-center justify-between h-16 px-4 sm:px-6 lg:px-8">
              <div className="flex items-center">
                {sidebar && (
                  <button
                    onClick={() => setSidebarOpen(true)}
                    className="text-secondary-400 hover:text-secondary-600 lg:hidden mr-4"
                  >
                    <span className="sr-only">Open sidebar</span>
                    <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                    </svg>
                  </button>
                )}
                
                <div>
                  {header?.breadcrumbs && <Breadcrumb items={header.breadcrumbs} className="mb-1" />}
                  {header?.title && (
                    <h1 className="text-2xl font-semibold text-secondary-900">{header.title}</h1>
                  )}
                </div>
              </div>
              
              {header?.actions && (
                <div className="flex items-center space-x-4">
                  {header.actions}
                </div>
              )}
            </div>
          </div>
        )}

        {/* Page content */}
        <main className="flex-1">
          {children}
        </main>
      </div>
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
    <div className={clsx('py-6', className)}>
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {(title || description || actions) && (
          <div className="mb-8">
            <div className="flex items-center justify-between">
              <div>
                {title && <h1 className="text-3xl font-bold text-secondary-900">{title}</h1>}
                {description && (
                  <p className="mt-2 text-secondary-600">{description}</p>
                )}
              </div>
              {actions && <div className="flex items-center space-x-4">{actions}</div>}
            </div>
          </div>
        )}
        {children}
      </div>
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
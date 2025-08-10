import React from 'react';
import Link from 'next/link';
import { clsx } from 'clsx';

export interface NavigationItem {
  label: string;
  href: string;
  icon?: React.ReactNode;
  active?: boolean;
  disabled?: boolean;
  children?: NavigationItem[];
}

export interface SidebarNavigationProps {
  items: NavigationItem[];
  className?: string;
}

export const SidebarNavigation: React.FC<SidebarNavigationProps> = ({
  items,
  className,
}) => {
  return (
    <nav className={clsx('space-y-1', className)}>
      {items.map((item, index) => (
        <NavigationLink key={`${item.href}-${index}`} item={item} />
      ))}
    </nav>
  );
};

const NavigationLink: React.FC<{ item: NavigationItem }> = ({ item }) => {
  const [isOpen, setIsOpen] = React.useState(false);

  const linkClasses = clsx(
    'group flex items-center px-3 py-2 text-sm font-medium rounded-md transition-colors',
    item.active
      ? 'bg-primary-100 text-primary-700 border-r-2 border-primary-500'
      : 'text-secondary-600 hover:bg-secondary-100 hover:text-secondary-900',
    item.disabled && 'opacity-50 cursor-not-allowed'
  );

  if (item.children && item.children.length > 0) {
    return (
      <div>
        <button
          onClick={() => setIsOpen(!isOpen)}
          className={linkClasses}
          disabled={item.disabled}
        >
          {item.icon && <span className="mr-3 h-5 w-5 flex-shrink-0">{item.icon}</span>}
          <span className="flex-1 text-left">{item.label}</span>
          <svg
            className={clsx(
              'ml-auto h-5 w-5 transform transition-transform',
              isOpen ? 'rotate-90' : 'rotate-0'
            )}
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
          </svg>
        </button>
        {isOpen && (
          <div className="ml-6 mt-1 space-y-1">
            {item.children.map((child, childIndex) => (
              <NavigationLink key={`${child.href}-${childIndex}`} item={child} />
            ))}
          </div>
        )}
      </div>
    );
  }

  return (
    <Link href={item.href} className={linkClasses}>
      {item.icon && <span className="mr-3 h-5 w-5 flex-shrink-0">{item.icon}</span>}
      <span>{item.label}</span>
    </Link>
  );
};

export interface BreadcrumbItem {
  label: string;
  href?: string;
}

export interface BreadcrumbProps {
  items: BreadcrumbItem[];
  className?: string;
}

export const Breadcrumb: React.FC<BreadcrumbProps> = ({ items, className }) => {
  return (
    <nav className={clsx('flex', className)} aria-label="Breadcrumb">
      <ol className="flex items-center space-x-2">
        {items.map((item, index) => (
          <li key={index} className="flex items-center">
            {index > 0 && (
              <svg
                className="flex-shrink-0 h-4 w-4 text-secondary-400 mx-2"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
              </svg>
            )}
            {item.href ? (
              <Link
                href={item.href}
                className="text-sm font-medium text-secondary-500 hover:text-secondary-700"
              >
                {item.label}
              </Link>
            ) : (
              <span className="text-sm font-medium text-secondary-900">{item.label}</span>
            )}
          </li>
        ))}
      </ol>
    </nav>
  );
};

export interface TabItem {
  label: string;
  value: string;
  disabled?: boolean;
}

export interface TabsProps {
  items: TabItem[];
  activeTab: string;
  onTabChange: (value: string) => void;
  className?: string;
}

export const Tabs: React.FC<TabsProps> = ({ items, activeTab, onTabChange, className }) => {
  return (
    <div className={className}>
      <div className="border-b border-secondary-200">
        <nav className="-mb-px flex space-x-8" aria-label="Tabs">
          {items.map((tab) => (
            <button
              key={tab.value}
              onClick={() => !tab.disabled && onTabChange(tab.value)}
              className={clsx(
                'whitespace-nowrap py-2 px-1 border-b-2 font-medium text-sm',
                tab.value === activeTab
                  ? 'border-primary-500 text-primary-600'
                  : 'border-transparent text-secondary-500 hover:text-secondary-700 hover:border-secondary-300',
                tab.disabled && 'opacity-50 cursor-not-allowed'
              )}
              disabled={tab.disabled}
            >
              {tab.label}
            </button>
          ))}
        </nav>
      </div>
    </div>
  );
};
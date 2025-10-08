import React from 'react';
import Link from 'next/link';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '../lib/utils';

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
  collapsed?: boolean;
  className?: string;
}

const sidebarNavigationVariants = cva(
  'space-y-1',
  {
    variants: {},
    defaultVariants: {},
  }
);

export const SidebarNavigation: React.FC<SidebarNavigationProps> = ({
  items,
  collapsed = false,
  className,
}) => {
  return (
    <nav className={sidebarNavigationVariants({ className })}>
      {items.map((item, index) => (
        <NavigationLink key={`${item.href}-${index}`} item={item} collapsed={collapsed} />
      ))}
    </nav>
  );
};

const navigationLinkVariants = cva(
  'group flex items-center text-sm font-medium rounded-md transition-colors',
  {
    variants: {
      collapsed: {
        true: 'px-2 py-2 justify-center',
        false: 'px-3 py-2',
      },
      active: {
        true: 'bg-primary-100 text-primary-700 border-r-2 border-primary-500',
        false: 'text-secondary-600 hover:bg-secondary-100 hover:text-secondary-900',
      },
      disabled: {
        true: 'opacity-50 cursor-not-allowed',
        false: '',
      },
    },
    defaultVariants: {
      collapsed: false,
      active: false,
      disabled: false,
    },
  }
);

const iconVariants = cva(
  'h-5 w-5 flex-shrink-0',
  {
    variants: {
      collapsed: {
        true: '',
        false: 'mr-3',
      },
    },
    defaultVariants: {
      collapsed: false,
    },
  }
);

const chevronVariants = cva(
  'ml-auto h-5 w-5 transform transition-transform',
  {
    variants: {
      open: {
        true: 'rotate-90',
        false: 'rotate-0',
      },
    },
    defaultVariants: {
      open: false,
    },
  }
);

const NavigationLink: React.FC<{ item: NavigationItem; collapsed?: boolean }> = ({ item, collapsed = false }) => {
  const [isOpen, setIsOpen] = React.useState(false);

  const linkClasses = navigationLinkVariants({
    collapsed,
    active: item.active,
    disabled: item.disabled,
  });

  if (item.children && item.children.length > 0) {
    return (
      <div>
        <button
          onClick={() => setIsOpen(!isOpen)}
          className={linkClasses}
          disabled={item.disabled}
        >
          {item.icon && <span className={iconVariants({ collapsed })}>{item.icon}</span>}
          {!collapsed && <span className="flex-1 text-left">{item.label}</span>}
          {!collapsed && <svg
            className={chevronVariants({ open: isOpen })}
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
          </svg>}
        </button>
        {isOpen && !collapsed && (
          <div className="ml-6 mt-1 space-y-1">
            {item.children.map((child, childIndex) => (
              <NavigationLink key={`${child.href}-${childIndex}`} item={child} collapsed={collapsed} />
            ))}
          </div>
        )}
      </div>
    );
  }

  return (
    <Link href={item.href} className={linkClasses} title={collapsed ? item.label : undefined}>
      {item.icon && <span className={cn('h-5 w-5 flex-shrink-0', !collapsed && 'mr-3')}>{item.icon}</span>}
      {!collapsed && <span>{item.label}</span>}
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

const breadcrumbVariants = cva(
  'flex',
  {
    variants: {},
    defaultVariants: {},
  }
);

export const Breadcrumb: React.FC<BreadcrumbProps> = ({ items, className }) => {
  return (
    <nav className={breadcrumbVariants({ className })} aria-label="Breadcrumb">
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

export interface LegacyTabsProps {
  items: TabItem[];
  activeTab: string;
  onTabChange: (value: string) => void;
  className?: string;
}

const tabButtonVariants = cva(
  'whitespace-nowrap py-2 px-1 border-b-2 font-medium text-sm',
  {
    variants: {
      active: {
        true: 'border-primary-500 text-primary-600',
        false: 'border-transparent text-secondary-500 hover:text-secondary-700 hover:border-secondary-300',
      },
      disabled: {
        true: 'opacity-50 cursor-not-allowed',
        false: '',
      },
    },
    compoundVariants: [
      {
        active: false,
        disabled: true,
        class: 'hover:text-secondary-500 hover:border-transparent',
      },
    ],
    defaultVariants: {
      active: false,
      disabled: false,
    },
  }
);

export const LegacyTabs: React.FC<LegacyTabsProps> = ({ items, activeTab, onTabChange, className }) => {
  return (
    <div className={className}>
      <div className="border-b border-secondary-200">
        <nav className="-mb-px flex space-x-8" aria-label="Tabs">
          {items.map((tab) => (
            <button
              key={tab.value}
              onClick={() => !tab.disabled && onTabChange(tab.value)}
              className={tabButtonVariants({
                active: tab.value === activeTab,
                disabled: tab.disabled,
              })}
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
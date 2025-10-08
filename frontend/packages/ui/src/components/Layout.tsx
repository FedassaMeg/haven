import { BarChart3, Bell, Briefcase, Calendar, ChevronRight, FileBarChart, FileText, Home, Settings, Shield, Users } from "lucide-react";
import Link from "next/link";
import React from "react";
import { twMerge } from "tailwind-merge";
import { cn } from "../lib/utils";
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "./collapsible";
import {
  NotificationsDropdown,
  SearchBar,
  UserProfileDropdown,
  type NotificationItem,
  type UserProfile,
} from "./Header";
import {
  Breadcrumb,
  type BreadcrumbItem,
  type NavigationItem,
} from "./Navigation";
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarInset,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenuSub,
  SidebarMenuSubButton,
  SidebarMenuSubItem,
  SidebarProvider,
  SidebarTrigger,
} from "./sidebar";

export interface NavigationGroup {
  label?: string;
  items: NavigationItem[];
}

// NOTE: This Layout component contains an inline sidebar implementation
// There's also a standalone AppSidebar component at apps/cm-portal/src/components/AppSidebar.tsx
// The inline version here supports grouped navigation and submenus and is currently being used
// Future refactoring could move to use the standalone AppSidebar component exclusively

export interface LayoutProps {
  children: React.ReactNode;
  sidebar?: {
    navigation?: NavigationItem[] | NavigationGroup[];
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

export const Layout: React.FC<LayoutProps> = ({
  children,
  sidebar,
  header,
  className,
}) => {
  const renderNavigationItem = (item: NavigationItem) => {
    if (item.children && item.children.length > 0) {
      return (
        <Collapsible key={item.href} asChild defaultOpen={item.active}>
          <SidebarMenuItem>
            <SidebarMenuButton asChild tooltip={item.label}>
              <CollapsibleTrigger>
                {item.icon}
                <span>{item.label}</span>
                <ChevronRight className="ml-auto transition-transform duration-200 group-data-[state=open]/collapsible:rotate-90" />
              </CollapsibleTrigger>
            </SidebarMenuButton>
            <CollapsibleContent>
              <SidebarMenuSub>
                {item.children.map((child) => (
                  <SidebarMenuSubItem key={child.href}>
                    <SidebarMenuSubButton asChild isActive={child.active}>
                      <Link href={child.href}>
                        <span>{child.label}</span>
                      </Link>
                    </SidebarMenuSubButton>
                  </SidebarMenuSubItem>
                ))}
              </SidebarMenuSub>
            </CollapsibleContent>
          </SidebarMenuItem>
        </Collapsible>
      );
    }

    return (
      <SidebarMenuItem key={item.href}>
        <SidebarMenuButton asChild isActive={item.active} tooltip={item.label}>
          <Link href={item.href}>
            {item.icon}
            <span>{item.label}</span>
          </Link>
        </SidebarMenuButton>
      </SidebarMenuItem>
    );
  };

  // Default navigation with groups and submenus
  const defaultNavigation: NavigationGroup[] = [
    {
      label: "Main",
      items: [
        {
          label: "Dashboard",
          href: "/",
          icon: <Home className="h-4 w-4" />,
        },
        {
          label: "Triage",
          href: "/triage",
          icon: <Bell className="h-4 w-4" />,
        },
      ],
    },
    {
      label: "Client Management",
      items: [
        {
          label: "Clients",
          href: "/clients",
          icon: <Users className="h-4 w-4" />,
          children: [
            {
              label: "All Clients",
              href: "/clients",
            },
            {
              label: "Add Client",
              href: "/intake",
            },
            {
              label: "Client Search",
              href: "/clients/search",
            },
          ],
        },
        {
          label: "Caseload",
          href: "/caseload",
          icon: <Briefcase className="h-4 w-4" />,
          children: [
            {
              label: "My Caseload",
              href: "/caseload",
            },
            {
              label: "Team Caseload",
              href: "/caseload/team",
            },
          ],
        },
        {
          label: "Cases",
          href: "/cases",
          icon: <FileText className="h-4 w-4" />,
        },
      ],
    },
    {
      label: "Services",
      items: [
        {
          label: "Services",
          href: "/services",
          icon: <Calendar className="h-4 w-4" />,
        },
        {
          label: "Billing",
          href: "/billing",
          icon: <BarChart3 className="h-4 w-4" />,
        },
      ],
    },
    {
      label: "Compliance",
      items: [
        {
          label: "Compliance",
          href: "/compliance",
          icon: <Shield className="h-4 w-4" />,
        },
        {
          label: "Mandated Reports",
          href: "/mandated-reports",
          icon: <FileBarChart className="h-4 w-4" />,
        },
      ],
    },
    {
      label: "System",
      items: [
        {
          label: "Settings",
          href: "/settings",
          icon: <Settings className="h-4 w-4" />,
        },
      ],
    },
  ];

  const navigationItems = sidebar?.navigation || defaultNavigation;

  const renderHeader = () => {
    if (!header && !sidebar) return null;

    return (
      <header className="flex h-16 shrink-0 items-center gap-2 transition-[width,height] ease-linear group-has-[[data-collapsible=icon]]/sidebar-wrapper:h-12">
        <div className="flex items-center gap-2 px-4">
          {sidebar && <SidebarTrigger className="-ml-1" />}
          {header?.breadcrumbs && <Breadcrumb items={header.breadcrumbs} />}
        </div>
        <div className="flex-1">
          {header?.title && (
            <h1 className="text-2xl font-semibold text-foreground">
              {header.title}
            </h1>
          )}
        </div>
        <div className="flex items-center gap-2 px-4">
          {/* Search Bar */}
          {header?.showSearch && (
            <SearchBar
              onSearch={header.onSearch}
              className="max-w-md flex-1 hidden md:block"
            />
          )}
          {/* Custom Actions */}
          {header?.actions && (
            <div className="flex items-center gap-2">{header.actions}</div>
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
      </header>
    );
  };

  if (!sidebar) {
    // Layout without sidebar
    return (
      <div className={cn("min-h-screen bg-background", className)}>
        {header && <div className="border-b">{renderHeader()}</div>}
        <main className="flex-1 overflow-auto px-4 sm:px-6 lg:px-8 py-6">
          {children}
        </main>
      </div>
    );
  }

  // Layout with sidebar
  return (
    <SidebarProvider>
      <Sidebar collapsible="icon">
        <SidebarHeader>
          {sidebar.logo || (
            <div className="flex items-center gap-2 px-2 py-2">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-primary-foreground font-bold">
                H
              </div>
              <div className="grid flex-1 text-left text-sm leading-tight group-data-[collapsible=icon]:opacity-0 group-data-[collapsible=icon]:w-0 overflow-hidden transition-all">
                <span className="truncate font-semibold">Haven</span>
                <span className="truncate text-xs text-muted-foreground">Case Management</span>
              </div>
            </div>
          )}
        </SidebarHeader>
        <SidebarContent className="overflow-x-hidden">
          {(() => {
            const items = navigationItems;
            if (!items || items.length === 0) {
              return null;
            }
            
            const isGroupedNavigation = (items: NavigationItem[] | NavigationGroup[]): items is NavigationGroup[] => {
              return items.length > 0 && 'items' in items[0];
            };

            if (isGroupedNavigation(items)) {
              return items.map((group, index) => (
                <SidebarGroup key={group.label || index}>
                  {group.label && <SidebarGroupLabel>{group.label}</SidebarGroupLabel>}
                  <SidebarGroupContent>
                    <SidebarMenu>
                      {group.items && group.items.length > 0 ? group.items.map(renderNavigationItem) : null}
                    </SidebarMenu>
                  </SidebarGroupContent>
                </SidebarGroup>
              ));
            }

            return (
              <SidebarGroup>
                <SidebarGroupContent>
                  <SidebarMenu>
                    {(items as NavigationItem[]).map(renderNavigationItem)}
                  </SidebarMenu>
                </SidebarGroupContent>
              </SidebarGroup>
            );
          })()}
        </SidebarContent>
        {sidebar.footer && (
          <SidebarFooter>
            {sidebar.footer}
          </SidebarFooter>
        )}
      </Sidebar>
      <SidebarInset>
        {(header || sidebar) && (
          <div className="border-b bg-background">
            {renderHeader()}
          </div>
        )}
        <main className="flex-1 p-4 md:p-6 lg:p-8">
          <div className="space-y-4">
            {children}
          </div>
        </main>
      </SidebarInset>
    </SidebarProvider>
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
    <div className={twMerge("grid gap-8", className)}>
      {(title || description || actions) && (
        <header className="flex items-start justify-between gap-4 flex-wrap">
          <div className="min-w-0 flex-1">
            {title && (
              <h1 className="text-3xl font-bold text-secondary-900">{title}</h1>
            )}
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
        <div className="mx-auto h-12 w-12 text-secondary-400 mb-4">{icon}</div>
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

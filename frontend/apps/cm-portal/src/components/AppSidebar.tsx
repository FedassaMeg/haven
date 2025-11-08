import React from "react";
import Link from "next/link";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
  NavigationItem,
  Sidebar, 
  SidebarContent, 
  SidebarFooter, 
  SidebarGroup, 
  SidebarGroupContent,
  SidebarGroupLabel, 
  SidebarHeader, 
  SidebarMenu, 
  SidebarMenuButton, 
  SidebarMenuItem,
  SidebarMenuSub,
  SidebarMenuSubButton,
  SidebarMenuSubItem
} from "@haven/ui";
import { } from "@haven/ui/src/components/Collapsible";
import { ChevronRight } from "lucide-react";

export interface NavigationGroup {
  label?: string;
  items: NavigationItem[];
}

interface AppSidebarProps {
  items: NavigationItem[] | NavigationGroup[];
  logo?: React.ReactNode;
  footer?: React.ReactNode;
}

const AppSidebar: React.FC<AppSidebarProps> = ({ items, logo, footer }) => {
  const isGroupedNavigation = (items: NavigationItem[] | NavigationGroup[]): items is NavigationGroup[] => {
    return items.length > 0 && 'items' in items[0];
  };

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

  const renderNavigationGroups = () => {
    if (!items || items.length === 0) {
      return null;
    }

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
  };

  return (
    <Sidebar collapsible="icon">
      <SidebarHeader>
        {logo || (
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
        {renderNavigationGroups()}
      </SidebarContent>
      {footer && (
        <SidebarFooter>
          {footer}
        </SidebarFooter>
      )}
    </Sidebar>
  );
};

export default AppSidebar;
export type { NavigationGroup, AppSidebarProps };
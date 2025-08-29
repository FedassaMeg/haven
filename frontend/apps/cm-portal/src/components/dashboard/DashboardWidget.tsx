import React from 'react';
import { Card, CardHeader, CardTitle, CardContent } from '@haven/ui';
import { DashboardWidget } from '../../config/dashboard-config';

interface DashboardWidgetProps {
  widget: DashboardWidget;
  children: React.ReactNode;
  className?: string;
}

export function DashboardWidgetContainer({ widget, children, className = '' }: DashboardWidgetProps) {
  const getSizeClass = (size: string) => {
    switch (size) {
      case 'small':
        return 'col-span-1';
      case 'medium':
        return 'col-span-1';
      case 'large':
        return 'col-span-1 lg:col-span-2';
      case 'full':
        return 'col-span-full';
      default:
        return 'col-span-1';
    }
  };

  const baseClasses = `${getSizeClass(widget.size)} ${className}`;

  return (
    <Card className={baseClasses} style={{ order: widget.order }}>
      <CardHeader>
        <CardTitle className="font-heading font-bold text-lg text-slate-800">
          {widget.title}
        </CardTitle>
      </CardHeader>
      <CardContent>
        {children}
      </CardContent>
    </Card>
  );
}
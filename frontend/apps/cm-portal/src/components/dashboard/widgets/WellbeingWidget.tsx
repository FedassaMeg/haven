import React from 'react';
import { Button } from '@haven/ui';
import { DashboardWidget } from '../../../config/dashboard-config';
import { DashboardWidgetContainer } from '../DashboardWidget';

interface WellbeingWidgetProps {
  widget: DashboardWidget;
}

export function WellbeingWidget({ widget }: WellbeingWidgetProps) {
  const config = widget.config || {};

  return (
    <DashboardWidgetContainer widget={widget} className="bg-blue-50 border-blue-200">
      <div>
        <p className="text-sm text-blue-700 mb-4">
          You've handled multiple intense cases today. Take a 2-minute pause when you're ready.
        </p>
        {config.showBreathingExercise && (
          <Button
            variant="outline"
            size="sm"
            className="w-full border-blue-300 text-blue-700 hover:bg-blue-100 bg-transparent"
          >
            Breathing Exercise
          </Button>
        )}
      </div>
    </DashboardWidgetContainer>
  );
}
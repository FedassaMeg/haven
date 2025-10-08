import { useState, useEffect } from 'react';
import { Badge } from '@haven/ui';
import type { ServiceCalendarEvent } from '@haven/api-client/src/types/services';

interface ServiceCalendarProps {
  clientId: string;
  events?: ServiceCalendarEvent[];
}

const ServiceCalendar: React.FC<ServiceCalendarProps> = ({ clientId, events = [] }) => {
  const [currentDate, setCurrentDate] = useState(new Date());
  const [calendarDays, setCalendarDays] = useState<Date[]>([]);

  useEffect(() => {
    generateCalendarDays();
  }, [currentDate]);

  const generateCalendarDays = () => {
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const startDate = new Date(firstDay);
    startDate.setDate(startDate.getDate() - firstDay.getDay());
    
    const days: Date[] = [];
    const endDate = new Date(lastDay);
    endDate.setDate(endDate.getDate() + (6 - lastDay.getDay()));
    
    for (let d = new Date(startDate); d <= endDate; d.setDate(d.getDate() + 1)) {
      days.push(new Date(d));
    }
    
    setCalendarDays(days);
  };

  // Mock events for demonstration
  const mockEvents: ServiceCalendarEvent[] = [
    {
      id: '1',
      title: 'Case Management Meeting',
      start: '2024-05-15T10:00:00',
      end: '2024-05-15T11:00:00',
      type: 'encounter',
      relatedId: 'enc-1',
      clientId,
      clientName: 'Jane Doe',
      location: 'Office',
      notes: 'Monthly check-in'
    },
    {
      id: '2',
      title: 'Housing Goal Review',
      start: '2024-05-20T14:00:00',
      end: '2024-05-20T15:00:00',
      type: 'goal',
      relatedId: 'goal-1',
      clientId,
      clientName: 'Jane Doe'
    },
    {
      id: '3',
      title: 'Legal Aid Follow-up',
      start: '2024-05-22T09:00:00',
      end: '2024-05-22T09:30:00',
      type: 'referral',
      relatedId: 'ref-1',
      clientId,
      clientName: 'Jane Doe',
      location: 'Phone'
    },
    {
      id: '4',
      title: 'Monthly Check-in Reminder',
      start: '2024-05-25T00:00:00',
      end: '2024-05-25T23:59:59',
      type: 'reminder',
      relatedId: 'rem-1',
      clientId,
      clientName: 'Jane Doe'
    }
  ];

  const displayEvents = events.length > 0 ? events : mockEvents;

  const getEventsForDay = (day: Date) => {
    return displayEvents.filter(event => {
      const eventDate = new Date(event.start);
      return eventDate.getDate() === day.getDate() &&
             eventDate.getMonth() === day.getMonth() &&
             eventDate.getFullYear() === day.getFullYear();
    });
  };

  const getEventColor = (type: ServiceCalendarEvent['type']) => {
    switch (type) {
      case 'encounter':
        return 'bg-primary-100 text-primary-900 border-primary-200';
      case 'goal':
        return 'bg-success-100 text-success-900 border-success-200';
      case 'referral':
        return 'bg-warning-100 text-warning-900 border-warning-200';
      case 'reminder':
        return 'bg-secondary-100 text-secondary-900 border-secondary-200';
      default:
        return 'bg-gray-100 text-gray-900 border-gray-200';
    }
  };

  const getEventIcon = (type: ServiceCalendarEvent['type']) => {
    switch (type) {
      case 'encounter':
        return '📋';
      case 'goal':
        return '🎯';
      case 'referral':
        return '🔗';
      case 'reminder':
        return '⏰';
      default:
        return '📅';
    }
  };

  const isToday = (date: Date) => {
    const today = new Date();
    return date.getDate() === today.getDate() &&
           date.getMonth() === today.getMonth() &&
           date.getFullYear() === today.getFullYear();
  };

  const isCurrentMonth = (date: Date) => {
    return date.getMonth() === currentDate.getMonth();
  };

  const formatTime = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleTimeString('en-US', { 
      hour: 'numeric', 
      minute: '2-digit',
      hour12: true 
    });
  };

  return (
    <div className="w-full">
      {/* Calendar Header */}
      <div className="mb-4 text-center">
        <h3 className="text-lg font-semibold text-secondary-900">
          {currentDate.toLocaleDateString('en-US', { month: 'long', year: 'numeric' })}
        </h3>
      </div>

      {/* Day Headers */}
      <div className="grid grid-cols-7 gap-px bg-secondary-200 border border-secondary-200 rounded-t-lg overflow-hidden">
        {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map(day => (
          <div key={day} className="bg-secondary-50 py-2 text-center">
            <span className="text-xs font-medium text-secondary-700">{day}</span>
          </div>
        ))}
      </div>

      {/* Calendar Grid */}
      <div className="grid grid-cols-7 gap-px bg-secondary-200 border-x border-b border-secondary-200 rounded-b-lg overflow-hidden">
        {calendarDays.map((day, index) => {
          const dayEvents = getEventsForDay(day);
          const isCurrentMonthDay = isCurrentMonth(day);
          const isTodayDate = isToday(day);

          return (
            <div
              key={index}
              className={`
                bg-white min-h-[100px] p-2
                ${!isCurrentMonthDay ? 'bg-secondary-50' : ''}
                ${isTodayDate ? 'bg-primary-50' : ''}
              `}
            >
              <div className="flex items-center justify-between mb-1">
                <span className={`
                  text-sm font-medium
                  ${!isCurrentMonthDay ? 'text-secondary-400' : 'text-secondary-900'}
                  ${isTodayDate ? 'text-primary-700' : ''}
                `}>
                  {day.getDate()}
                </span>
                {dayEvents.length > 0 && (
                  <Badge variant="secondary" size="sm" className="text-xs px-1 py-0">
                    {dayEvents.length}
                  </Badge>
                )}
              </div>

              <div className="space-y-1">
                {dayEvents.slice(0, 2).map((event) => (
                  <div
                    key={event.id}
                    className={`
                      text-xs px-1 py-0.5 rounded border cursor-pointer hover:opacity-80
                      ${getEventColor(event.type)}
                    `}
                    title={`${event.title}\n${formatTime(event.start)}${event.location ? `\n${event.location}` : ''}`}
                  >
                    <div className="flex items-center space-x-1 truncate">
                      <span>{getEventIcon(event.type)}</span>
                      <span className="truncate">{event.title}</span>
                    </div>
                    {event.type !== 'reminder' && (
                      <div className="text-xs opacity-75">
                        {formatTime(event.start)}
                      </div>
                    )}
                  </div>
                ))}
                {dayEvents.length > 2 && (
                  <div className="text-xs text-secondary-600 text-center">
                    +{dayEvents.length - 2} more
                  </div>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* Legend */}
      <div className="mt-4 flex items-center justify-center space-x-4">
        <div className="flex items-center space-x-1">
          <div className="w-3 h-3 bg-primary-100 border border-primary-200 rounded"></div>
          <span className="text-xs text-secondary-600">Encounters</span>
        </div>
        <div className="flex items-center space-x-1">
          <div className="w-3 h-3 bg-success-100 border border-success-200 rounded"></div>
          <span className="text-xs text-secondary-600">Goals</span>
        </div>
        <div className="flex items-center space-x-1">
          <div className="w-3 h-3 bg-warning-100 border border-warning-200 rounded"></div>
          <span className="text-xs text-secondary-600">Referrals</span>
        </div>
        <div className="flex items-center space-x-1">
          <div className="w-3 h-3 bg-secondary-100 border border-secondary-200 rounded"></div>
          <span className="text-xs text-secondary-600">Reminders</span>
        </div>
      </div>
    </div>
  );
};

export default ServiceCalendar;
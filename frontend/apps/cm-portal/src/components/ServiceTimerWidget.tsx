import { useState, useEffect } from 'react';
import Link from 'next/link';
import { Card, CardHeader, CardTitle, CardContent, Button, Badge } from '@haven/ui';
import { 
  useActiveServiceEpisodes,
  usePauseService,
  useResumeService,
  type ServiceEpisode 
} from '@haven/api-client';

interface ActiveServiceTimerProps {
  episode: ServiceEpisode;
  onPause: () => void;
  onResume: () => void;
  onNavigate: (id: string) => void;
}

const ActiveServiceTimer: React.FC<ActiveServiceTimerProps> = ({ 
  episode, 
  onPause, 
  onResume, 
  onNavigate 
}) => {
  const [currentTime, setCurrentTime] = useState(new Date());

  useEffect(() => {
    const interval = setInterval(() => {
      setCurrentTime(new Date());
    }, 1000);

    return () => clearInterval(interval);
  }, []);

  const calculateDuration = () => {
    if (!episode.startTime) return 0;
    const start = new Date(episode.startTime);
    const elapsed = Math.floor((currentTime.getTime() - start.getTime()) / (1000 * 60));
    const pausedDuration = episode.pausedDurationMinutes || 0;
    return Math.max(0, elapsed - pausedDuration);
  };

  const formatDuration = (totalMinutes: number) => {
    const hours = Math.floor(totalMinutes / 60);
    const minutes = totalMinutes % 60;
    
    if (hours > 0) {
      return `${hours}h ${minutes}m`;
    }
    return `${minutes}m`;
  };

  const duration = calculateDuration();
  const isOvertime = episode.expectedDurationMinutes && duration > episode.expectedDurationMinutes;
  const isPaused = episode.status === 'ON_HOLD';

  return (
    <div className={`border rounded-lg p-3 ${isOvertime ? 'border-red-200 bg-red-50' : 'border-green-200 bg-green-50'}`}>
      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center space-x-2">
          <div className={`w-2 h-2 rounded-full ${isPaused ? 'bg-yellow-500 animate-pulse' : 'bg-green-500 animate-pulse'}`}></div>
          <span className="font-medium text-secondary-900 text-sm">{episode.serviceType}</span>
        </div>
        <Badge variant={isPaused ? 'warning' : 'success'} className="text-xs">
          {isPaused ? 'Paused' : 'Active'}
        </Badge>
      </div>
      
      <div className="flex items-center justify-between mb-2">
        <div>
          <p className="text-xs text-secondary-600">{episode.clientName}</p>
          <p className={`text-lg font-mono font-bold ${isOvertime ? 'text-red-700' : 'text-green-700'}`}>
            {formatDuration(duration)}
          </p>
        </div>
        <div className="flex items-center space-x-1">
          {!isPaused ? (
            <Button 
              variant="outline" 
              size="sm" 
              onClick={onPause}
              className="text-xs px-2 py-1"
            >
              Pause
            </Button>
          ) : (
            <Button 
              variant="outline" 
              size="sm" 
              onClick={onResume}
              className="text-xs px-2 py-1"
            >
              Resume
            </Button>
          )}
          <Button 
            size="sm" 
            onClick={() => onNavigate(episode.id)}
            className="text-xs px-2 py-1"
          >
            Track
          </Button>
        </div>
      </div>
      
      {isOvertime && (
        <div className="text-xs text-red-600">
          ⚠️ Over expected duration ({episode.expectedDurationMinutes}m)
        </div>
      )}
    </div>
  );
};

interface ServiceTimerWidgetProps {
  providerId?: string;
  compact?: boolean;
}

export default function ServiceTimerWidget({ providerId, compact = false }: ServiceTimerWidgetProps) {
  const { activeServices, loading, refetch } = useActiveServiceEpisodes(providerId);
  const { pauseService } = usePauseService();
  const { resumeService } = useResumeService();

  const handlePause = async (episodeId: string) => {
    try {
      await pauseService(episodeId);
      await refetch();
    } catch (error) {
      console.error('Failed to pause service:', error);
    }
  };

  const handleResume = async (episodeId: string) => {
    try {
      await resumeService(episodeId);
      await refetch();
    } catch (error) {
      console.error('Failed to resume service:', error);
    }
  };

  const handleNavigateToTracking = (episodeId: string) => {
    window.open(`/services/${episodeId}/track`, '_blank');
  };

  if (loading) {
    return (
      <Card>
        <CardContent className="p-4">
          <div className="flex items-center justify-center">
            <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-primary-600"></div>
          </div>
        </CardContent>
      </Card>
    );
  }

  if (!activeServices || activeServices.length === 0) {
    if (compact) return null;
    
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-sm">Active Services</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-center py-4">
            <svg className="w-8 h-8 text-secondary-400 mx-auto mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <p className="text-sm text-secondary-600">No active services</p>
          </div>
        </CardContent>
      </Card>
    );
  }

  const totalActiveTime = activeServices.reduce((total, service) => {
    if (!service.startTime) return total;
    const start = new Date(service.startTime);
    const elapsed = Math.floor((Date.now() - start.getTime()) / (1000 * 60));
    const pausedDuration = service.pausedDurationMinutes || 0;
    return total + Math.max(0, elapsed - pausedDuration);
  }, 0);

  if (compact) {
    return (
      <div className="bg-white border border-secondary-200 rounded-lg p-3">
        <div className="flex items-center justify-between mb-2">
          <div className="flex items-center space-x-2">
            <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
            <span className="text-sm font-medium text-secondary-900">
              {activeServices.length} Active Service{activeServices.length !== 1 ? 's' : ''}
            </span>
          </div>
          <span className="text-sm text-secondary-600">
            {Math.floor(totalActiveTime / 60)}h {totalActiveTime % 60}m
          </span>
        </div>
        <div className="space-y-1">
          {activeServices.slice(0, 2).map((service) => (
            <div key={service.id} className="text-xs text-secondary-600">
              {service.serviceType} - {service.clientName}
            </div>
          ))}
          {activeServices.length > 2 && (
            <div className="text-xs text-secondary-500">
              +{activeServices.length - 2} more
            </div>
          )}
        </div>
        <Link href="/services?status=IN_PROGRESS">
          <Button variant="outline" size="sm" className="w-full mt-2 text-xs">
            View All
          </Button>
        </Link>
      </div>
    );
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle className="flex items-center space-x-2">
            <span>Active Services</span>
            <Badge variant="primary">{activeServices.length}</Badge>
          </CardTitle>
          <Link href="/services?status=IN_PROGRESS">
            <Button variant="outline" size="sm">View All</Button>
          </Link>
        </div>
      </CardHeader>
      <CardContent>
        <div className="space-y-3">
          {/* Total Time Summary */}
          <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg">
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium text-blue-900">Total Active Time</span>
              <span className="text-lg font-bold text-blue-900">
                {Math.floor(totalActiveTime / 60)}h {totalActiveTime % 60}m
              </span>
            </div>
          </div>

          {/* Individual Service Timers */}
          <div className="space-y-2">
            {activeServices.map((service) => (
              <ActiveServiceTimer
                key={service.id}
                episode={service}
                onPause={() => handlePause(service.id)}
                onResume={() => handleResume(service.id)}
                onNavigate={handleNavigateToTracking}
              />
            ))}
          </div>

          {/* Quick Actions */}
          <div className="pt-3 border-t border-secondary-200">
            <div className="grid grid-cols-2 gap-2">
              <Link href="/services/new?type=crisis">
                <Button variant="outline" size="sm" className="w-full text-xs">
                  Quick Crisis
                </Button>
              </Link>
              <Link href="/services/new">
                <Button variant="outline" size="sm" className="w-full text-xs">
                  New Service
                </Button>
              </Link>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
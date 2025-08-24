import { useState } from 'react';
import Link from 'next/link';
import { Card, CardHeader, CardTitle, CardContent, Button, Badge, Modal } from '@haven/ui';
import { 
  useServiceEpisodes, 
  useServiceStatistics,
  type ServiceEpisode 
} from '@haven/api-client';
import QuickServiceCreator from './QuickServiceCreator';

const SERVICE_STATUS_COLORS = {
  CREATED: 'bg-gray-100 text-gray-800',
  IN_PROGRESS: 'bg-blue-100 text-blue-800',
  COMPLETED: 'bg-green-100 text-green-800',
  CANCELLED: 'bg-red-100 text-red-800',
  ON_HOLD: 'bg-yellow-100 text-yellow-800',
};

interface ServiceEpisodeWidgetProps {
  providerId?: string;
  clientId?: string;
  limit?: number;
}

export default function ServiceEpisodeWidget({ 
  providerId, 
  clientId, 
  limit = 5 
}: ServiceEpisodeWidgetProps) {
  const [showQuickCreator, setShowQuickCreator] = useState(false);
  
  const { serviceEpisodes, loading } = useServiceEpisodes({
    providerId,
    clientId,
    page: 0,
    size: limit,
  });

  const { statistics } = useServiceStatistics();

  const formatDuration = (minutes?: number) => {
    if (!minutes) return '0m';
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    if (hours > 0) {
      return `${hours}h ${mins}m`;
    }
    return `${mins}m`;
  };

  const getStatusInfo = (episode: ServiceEpisode) => {
    const now = new Date();
    const isToday = episode.startTime && 
      new Date(episode.startTime).toDateString() === now.toDateString();
    
    if (episode.status === 'IN_PROGRESS' && isToday) {
      return { 
        label: 'Live', 
        color: 'bg-green-100 text-green-800',
        icon: '🔴' // Live indicator
      };
    }
    
    const statusInfo = SERVICE_STATUS_COLORS[episode.status as keyof typeof SERVICE_STATUS_COLORS];
    return {
      label: episode.status.replace('_', ' '),
      color: statusInfo,
      icon: episode.status === 'IN_PROGRESS' ? '⏳' : 
            episode.status === 'COMPLETED' ? '✅' : 
            episode.status === 'CREATED' ? '📋' : '⏸️'
    };
  };

  const handleQuickServiceSuccess = (episodeId: string) => {
    setShowQuickCreator(false);
    // Optionally navigate to the new episode or refresh the list
  };

  return (
    <>
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>
              {clientId ? 'Recent Services' : 'Service Activity'}
            </CardTitle>
            <div className="flex items-center space-x-2">
              <Button 
                variant="outline" 
                size="sm"
                onClick={() => setShowQuickCreator(true)}
              >
                <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                </svg>
                Quick Start
              </Button>
              <Link href="/services">
                <Button variant="outline" size="sm">
                  View All
                </Button>
              </Link>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {/* Statistics Summary */}
          {statistics && !clientId && (
            <div className="grid grid-cols-4 gap-3 mb-6 p-3 bg-secondary-50 rounded-lg">
              <div className="text-center">
                <p className="text-lg font-bold text-secondary-900">{statistics.activeServices}</p>
                <p className="text-xs text-secondary-600">Active</p>
              </div>
              <div className="text-center">
                <p className="text-lg font-bold text-secondary-900">{statistics.totalServices}</p>
                <p className="text-xs text-secondary-600">Total</p>
              </div>
              <div className="text-center">
                <p className="text-lg font-bold text-secondary-900">
                  {statistics.totalMinutes ? Math.round(statistics.totalMinutes / 60) : 0}h
                </p>
                <p className="text-xs text-secondary-600">Hours</p>
              </div>
              <div className="text-center">
                <p className="text-lg font-bold text-secondary-900">
                  ${(statistics.totalBillableAmount || 0).toLocaleString()}
                </p>
                <p className="text-xs text-secondary-600">Billable</p>
              </div>
            </div>
          )}

          {/* Service Episodes List */}
          {loading ? (
            <div className="flex items-center justify-center py-8">
              <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-primary-600"></div>
            </div>
          ) : serviceEpisodes && serviceEpisodes.length > 0 ? (
            <div className="space-y-3">
              {serviceEpisodes.map((episode) => {
                const statusInfo = getStatusInfo(episode);
                return (
                  <div key={episode.id} className="border border-secondary-200 rounded-lg p-3 hover:bg-secondary-50 transition-colors">
                    <div className="flex items-center justify-between mb-2">
                      <div className="flex items-center space-x-3">
                        <span className="text-sm">{statusInfo.icon}</span>
                        <div>
                          <h4 className="font-medium text-secondary-900 text-sm">
                            {episode.serviceType}
                          </h4>
                          <p className="text-xs text-secondary-600">
                            {episode.clientName} • {episode.deliveryMode}
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center space-x-2">
                        <Badge className={`text-xs ${statusInfo.color}`}>
                          {statusInfo.label}
                        </Badge>
                        <Link href={`/services/${episode.id}`}>
                          <Button variant="outline" size="sm" className="text-xs px-2 py-1">
                            {episode.status === 'IN_PROGRESS' ? 'Track' : 'View'}
                          </Button>
                        </Link>
                      </div>
                    </div>
                    
                    <div className="grid grid-cols-3 gap-3 text-xs text-secondary-600">
                      <div>
                        <span className="font-medium">Duration:</span> {formatDuration(episode.actualDurationMinutes)}
                      </div>
                      <div>
                        <span className="font-medium">Provider:</span> {episode.primaryProviderName}
                      </div>
                      <div>
                        <span className="font-medium">
                          {episode.status === 'IN_PROGRESS' ? 'Started:' : 'Date:'}
                        </span>{' '}
                        {episode.startTime ? 
                          new Date(episode.startTime).toLocaleDateString() : 
                          new Date(episode.createdAt).toLocaleDateString()
                        }
                      </div>
                    </div>
                    
                    {episode.isConfidential && (
                      <div className="mt-2 flex items-center space-x-1">
                        <svg className="w-3 h-3 text-yellow-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m0 0v2m0-2h2m-2 0h-2m8-8V7a4 4 0 10-8 0v4m8 0a2 2 0 012 2v6a2 2 0 01-2 2H6a2 2 0 01-2-2v-6a2 2 0 012-2h8z" />
                        </svg>
                        <span className="text-xs text-yellow-700">Confidential</span>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="text-center py-6">
              <svg className="w-8 h-8 text-secondary-400 mx-auto mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
              </svg>
              <p className="text-sm text-secondary-600 mb-3">No recent service episodes</p>
              <Button 
                size="sm" 
                onClick={() => setShowQuickCreator(true)}
              >
                Create First Service
              </Button>
            </div>
          )}

          {/* Quick Action Buttons */}
          <div className="mt-4 pt-4 border-t border-secondary-200">
            <div className="grid grid-cols-3 gap-2">
              <Link href="/services/new?type=crisis">
                <Button variant="outline" size="sm" className="w-full text-xs">
                  🚨 Crisis
                </Button>
              </Link>
              <Link href="/services/new?type=counseling">
                <Button variant="outline" size="sm" className="w-full text-xs">
                  💬 Counseling
                </Button>
              </Link>
              <Link href="/services/new?type=case-management">
                <Button variant="outline" size="sm" className="w-full text-xs">
                  📋 Case Mgmt
                </Button>
              </Link>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Quick Service Creator Modal */}
      <Modal isOpen={showQuickCreator} onClose={() => setShowQuickCreator(false)}>
        <div className="p-6">
          <QuickServiceCreator
            clientId={clientId || ''}
            onSuccess={handleQuickServiceSuccess}
            onCancel={() => setShowQuickCreator(false)}
          />
        </div>
      </Modal>
    </>
  );
}
import { useState } from 'react';
import { useRouter } from 'next/router';
import Link from 'next/link';
import { ProtectedRoute, useCurrentUser } from '@haven/auth';
import { Card, CardHeader, CardTitle, CardContent, Badge, Button, Tabs, TabsList, TabsTrigger, TabsContent, Textarea } from '@haven/ui';
import { useCase, useCaseNotes, useEnrollmentServiceEpisodes, type Case, type CaseNote, type ServiceEpisode } from '@haven/api-client';
import AppLayout from '../../components/AppLayout';

interface CaseTimelineItemProps {
  type: 'note' | 'status_change' | 'assignment' | 'document' | 'service';
  title: string;
  description?: string;
  author?: string;
  timestamp: Date;
  metadata?: Record<string, any>;
}

const CaseTimelineItem: React.FC<CaseTimelineItemProps> = ({ 
  type, title, description, author, timestamp, metadata 
}) => {
  const iconConfig = {
    note: { icon: '📝', bg: 'bg-blue-100', color: 'text-blue-600' },
    status_change: { icon: '🔄', bg: 'bg-yellow-100', color: 'text-yellow-600' },
    assignment: { icon: '👤', bg: 'bg-green-100', color: 'text-green-600' },
    document: { icon: '📄', bg: 'bg-purple-100', color: 'text-purple-600' },
    service: { icon: '🛠️', bg: 'bg-cyan-100', color: 'text-cyan-600' },
  };

  const config = iconConfig[type] || iconConfig.note;

  return (
    <div className="flex space-x-3">
      <div className={`w-8 h-8 ${config.bg} rounded-full flex items-center justify-center flex-shrink-0`}>
        <span className="text-sm">{config.icon}</span>
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex items-center justify-between">
          <p className="text-sm font-medium text-secondary-900">{title}</p>
          <time className="text-xs text-secondary-500">{timestamp.toLocaleString()}</time>
        </div>
        {description && (
          <p className="text-sm text-secondary-600 mt-1">{description}</p>
        )}
        {author && (
          <p className="text-xs text-secondary-500 mt-1">by {author}</p>
        )}
        {metadata && Object.keys(metadata).length > 0 && (
          <div className="mt-2 flex flex-wrap gap-2">
            {Object.entries(metadata).map(([key, value]) => (
              <Badge key={key} variant="ghost" size="sm">
                {key}: {String(value)}
              </Badge>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

interface ServiceEpisodeCardProps {
  episode: ServiceEpisode;
  onRefresh?: () => void;
}

const ServiceEpisodeCard: React.FC<ServiceEpisodeCardProps> = ({ episode, onRefresh }) => {
  const getStatusConfig = (status: string) => {
    switch (status) {
      case 'SCHEDULED':
        return { variant: 'secondary' as const, label: 'Scheduled', color: 'bg-gray-400' };
      case 'IN_PROGRESS':
        return { variant: 'primary' as const, label: 'In Progress', color: 'bg-blue-500 animate-pulse' };
      case 'ON_HOLD':
        return { variant: 'warning' as const, label: 'On Hold', color: 'bg-yellow-500 animate-pulse' };
      case 'COMPLETED':
        return { variant: 'success' as const, label: 'Completed', color: 'bg-green-500' };
      case 'CANCELLED':
        return { variant: 'destructive' as const, label: 'Cancelled', color: 'bg-red-500' };
      default:
        return { variant: 'secondary' as const, label: status, color: 'bg-gray-400' };
    }
  };

  const formatDuration = (minutes: number) => {
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    if (hours > 0) {
      return `${hours}h ${mins}m`;
    }
    return `${mins}m`;
  };

  const statusConfig = getStatusConfig(episode.status);
  const currentDuration = episode.startTime && episode.status === 'IN_PROGRESS' ? 
    Math.floor((Date.now() - new Date(episode.startTime).getTime()) / (1000 * 60)) - (episode.pausedDurationMinutes || 0)
    : null;

  return (
    <div className="border border-secondary-200 rounded-lg p-4">
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center space-x-3">
          <div className={`w-3 h-3 rounded-full ${statusConfig.color}`}></div>
          <div>
            <h4 className="font-medium text-secondary-900">{episode.serviceType}</h4>
            <p className="text-sm text-secondary-600">
              {episode.deliveryMode} • {episode.programName || 'General Services'}
            </p>
          </div>
        </div>
        <div className="flex items-center space-x-2">
          <Badge variant={statusConfig.variant}>{statusConfig.label}</Badge>
          <Link href={`/services/${episode.id}`}>
            <Button variant="outline" size="sm">
              {episode.status === 'IN_PROGRESS' ? 'Track' : 'View'}
            </Button>
          </Link>
        </div>
      </div>
      <div className="grid grid-cols-3 gap-4 text-sm">
        <div>
          <span className="text-secondary-600">Duration:</span>{' '}
          {currentDuration ? `${formatDuration(currentDuration)} (ongoing)` : 
           episode.actualDurationMinutes ? formatDuration(episode.actualDurationMinutes) :
           episode.plannedDurationMinutes ? `${formatDuration(episode.plannedDurationMinutes)} (planned)` : 'N/A'}
        </div>
        <div>
          <span className="text-secondary-600">Provider:</span> {episode.primaryProvider || 'Unassigned'}
        </div>
        <div>
          <span className="text-secondary-600">Date:</span>{' '}
          {episode.startTime ? new Date(episode.startTime).toLocaleDateString() :
           episode.scheduledTime ? new Date(episode.scheduledTime).toLocaleDateString() : 'Not scheduled'}
        </div>
      </div>
      {episode.notes && (
        <div className="mt-3 pt-3 border-t border-secondary-200">
          <p className="text-sm text-secondary-700">{episode.notes}</p>
        </div>
      )}
    </div>
  );
};

function CaseDetailContent({ case: caseData }: { case: Case }) {
  const { user } = useCurrentUser();
  const [newNote, setNewNote] = useState('');
  const [activeTab, setActiveTab] = useState('overview');
  const { notes, loading: notesLoading, addNote } = useCaseNotes(caseData.id);
  
  // Get service episodes for this case (using case ID as enrollment ID)
  const enrollmentId = `CASE-${caseData.id.slice(0, 8)}`;
  const { serviceEpisodes, loading: servicesLoading, refetch: refetchServices } = useEnrollmentServiceEpisodes(enrollmentId);

  const handleAddNote = async () => {
    if (!newNote.trim()) return;
    
    try {
      await addNote({
        caseId: caseData.id,
        content: newNote,
        noteType: 'GENERAL',
        isConfidential: false,
      });
      setNewNote('');
    } catch (error) {
      console.error('Failed to add note:', error);
    }
  };

  const riskLevelConfig = {
    LOW: { bg: 'bg-green-100 text-green-800 border-green-200', label: 'Low Risk' },
    MODERATE: { bg: 'bg-yellow-100 text-yellow-800 border-yellow-200', label: 'Moderate Risk' },
    HIGH: { bg: 'bg-red-100 text-red-800 border-red-200', label: 'High Risk' },
    CRITICAL: { bg: 'bg-red-200 text-red-900 border-red-300', label: 'Critical Risk' },
  };

  const statusConfig = {
    OPEN: { variant: 'warning' as const, label: 'Open' },
    IN_PROGRESS: { variant: 'primary' as const, label: 'In Progress' },
    NEEDS_ATTENTION: { variant: 'destructive' as const, label: 'Needs Attention' },
    CLOSED: { variant: 'secondary' as const, label: 'Closed' },
  };

  const currentRisk = riskLevelConfig[caseData.riskLevel as keyof typeof riskLevelConfig] || 
    { bg: 'bg-gray-100 text-gray-800 border-gray-200', label: caseData.riskLevel };

  const currentStatus = statusConfig[caseData.status as keyof typeof statusConfig] || 
    { variant: 'secondary' as const, label: caseData.status };

  // Create timeline from service episodes and case activities
  const createTimelineItems = (): CaseTimelineItemProps[] => {
    const items: CaseTimelineItemProps[] = [];
    
    // Add mock case events
    items.push(
      {
        type: 'note',
        title: 'Initial intake completed',
        description: 'Client provided comprehensive history and immediate safety needs assessed.',
        author: 'Sarah Johnson',
        timestamp: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000),
      },
      {
        type: 'assignment',
        title: 'Case assigned to case manager',
        description: 'Assigned to Sarah Johnson for ongoing support and service coordination.',
        author: 'System',
        timestamp: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000),
      }
    );

    // Add service episode events
    if (serviceEpisodes) {
      serviceEpisodes.forEach(episode => {
        if (episode.startTime) {
          items.push({
            type: 'service',
            title: `${episode.serviceType} started`,
            description: `${episode.deliveryMode} service session began.`,
            author: episode.primaryProvider || 'Staff',
            timestamp: new Date(episode.startTime),
            metadata: {
              status: episode.status,
              ...(episode.plannedDurationMinutes && { 
                'planned duration': `${Math.floor(episode.plannedDurationMinutes / 60)}h ${episode.plannedDurationMinutes % 60}m` 
              }),
            },
          });
        }

        if (episode.endTime && episode.status === 'COMPLETED') {
          items.push({
            type: 'service',
            title: `${episode.serviceType} completed`,
            description: episode.outcome || 'Service session completed successfully.',
            author: episode.primaryProvider || 'Staff',
            timestamp: new Date(episode.endTime),
            metadata: {
              ...(episode.actualDurationMinutes && { 
                duration: `${Math.floor(episode.actualDurationMinutes / 60)}h ${episode.actualDurationMinutes % 60}m` 
              }),
              ...(episode.billableAmount && { 
                'billable amount': `$${episode.billableAmount}` 
              }),
            },
          });
        }
      });
    }

    // Sort by timestamp (newest first)
    return items.sort((a, b) => b.timestamp.getTime() - a.timestamp.getTime());
  };

  const timelineItems = createTimelineItems();

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <div className="flex items-center space-x-3 mb-2">
            <h1 className="text-2xl font-bold text-secondary-900">
              Case #{caseData.caseNumber || caseData.id.slice(0, 8)}
            </h1>
            <Badge variant={currentStatus.variant}>{currentStatus.label}</Badge>
            <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium border ${currentRisk.bg}`}>
              {currentRisk.label}
            </span>
            {caseData.requiresAttention && (
              <Badge variant="destructive">
                <svg className="w-3 h-3 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
                </svg>
                Attention Required
              </Badge>
            )}
          </div>
          <div className="text-secondary-600 space-y-1">
            <p>
              Client: <Link href={`/clients/${caseData.clientId}`} className="text-primary-600 hover:text-primary-700 font-medium">
                {caseData.clientName || 'Unknown Client'}
              </Link>
            </p>
            <p>Opened: {caseData.openedAt ? new Date(caseData.openedAt).toLocaleDateString() : 'Unknown'}</p>
            {caseData.assignment && (
              <p>Assigned to: <span className="font-medium">{caseData.assignment.assigneeName}</span></p>
            )}
          </div>
        </div>
        <div className="flex items-center space-x-3">
          <Button variant="outline">
            <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 100 4m0-4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 100 4m0-4v2m0-6V4" />
            </svg>
            Update Status
          </Button>
          <Button variant="outline">
            <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3a1 1 0 011-1h6a1 1 0 011 1v4h3a1 1 0 110 2h-1v9a2 2 0 01-2 2H7a2 2 0 01-2-2V9H4a1 1 0 110-2h4z" />
            </svg>
            Generate Report
          </Button>
          {caseData.status !== 'CLOSED' && (
            <Link href={`/services/new?clientId=${caseData.clientId}&enrollmentId=${enrollmentId}`}>
              <Button>
                <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                Add Service
              </Button>
            </Link>
          )}
        </div>
      </div>

      {/* Tabs */}
      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList className="grid w-full grid-cols-5">
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="notes">Notes & Timeline</TabsTrigger>
          <TabsTrigger value="services">Services</TabsTrigger>
          <TabsTrigger value="documents">Documents</TabsTrigger>
          <TabsTrigger value="safety">Safety Planning</TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Case Information */}
            <div className="lg:col-span-2 space-y-6">
              <Card>
                <CardHeader>
                  <CardTitle>Case Summary</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    <div>
                      <h4 className="font-medium text-secondary-900 mb-2">Presenting Issues</h4>
                      <p className="text-sm text-secondary-700">
                        {caseData.description || 'Client experiencing domestic violence, seeking immediate safety planning and housing assistance. Multiple incidents reported over the past 6 months.'}
                      </p>
                    </div>
                    <div>
                      <h4 className="font-medium text-secondary-900 mb-2">Current Goals</h4>
                      <ul className="text-sm text-secondary-700 space-y-1">
                        <li>• Establish safe, permanent housing</li>
                        <li>• Complete legal protection order process</li>
                        <li>• Access trauma-informed counseling services</li>
                        <li>• Develop sustainable safety plan</li>
                      </ul>
                    </div>
                    <div>
                      <h4 className="font-medium text-secondary-900 mb-2">Next Steps</h4>
                      <ul className="text-sm text-secondary-700 space-y-1">
                        <li>• Schedule follow-up housing search meeting</li>
                        <li>• Submit financial assistance application</li>
                        <li>• Coordinate with legal advocate</li>
                      </ul>
                    </div>
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardHeader>
                  <CardTitle>Participants & Contacts</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div>
                        <h4 className="font-medium text-secondary-900 mb-2">Primary Client</h4>
                        <div className="p-3 bg-secondary-50 rounded-lg">
                          <p className="font-medium">{caseData.clientName}</p>
                          <p className="text-sm text-secondary-600">Primary participant</p>
                        </div>
                      </div>
                      <div>
                        <h4 className="font-medium text-secondary-900 mb-2">Case Manager</h4>
                        <div className="p-3 bg-secondary-50 rounded-lg">
                          <p className="font-medium">{caseData.assignment?.assigneeName || 'Unassigned'}</p>
                          <p className="text-sm text-secondary-600">Lead case manager</p>
                        </div>
                      </div>
                    </div>
                    
                    <div>
                      <h4 className="font-medium text-secondary-900 mb-2">Additional Participants</h4>
                      <div className="space-y-2">
                        <div className="flex items-center justify-between p-3 bg-secondary-50 rounded-lg">
                          <div>
                            <p className="font-medium">Legal Advocate - Maria Rodriguez</p>
                            <p className="text-sm text-secondary-600">Protection order assistance</p>
                          </div>
                          <Badge variant="outline">Active</Badge>
                        </div>
                        <div className="flex items-center justify-between p-3 bg-secondary-50 rounded-lg">
                          <div>
                            <p className="font-medium">Housing Specialist - David Chen</p>
                            <p className="text-sm text-secondary-600">Housing search coordination</p>
                          </div>
                          <Badge variant="outline">Active</Badge>
                        </div>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>

            {/* Sidebar */}
            <div className="space-y-6">
              <Card>
                <CardHeader>
                  <CardTitle>Quick Stats</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-secondary-600">Days Open</span>
                      <span className="font-medium">
                        {caseData.openedAt ? Math.floor((Date.now() - new Date(caseData.openedAt).getTime()) / (1000 * 60 * 60 * 24)) : 'N/A'}
                      </span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-secondary-600">Total Services</span>
                      <span className="font-medium">{serviceEpisodes?.length || 0}</span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-secondary-600">Active Services</span>
                      <span className="font-medium">
                        {serviceEpisodes?.filter(ep => ep.status === 'IN_PROGRESS').length || 0}
                      </span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-secondary-600">Last Contact</span>
                      <span className="font-medium">
                        {serviceEpisodes && serviceEpisodes.length > 0 ? 
                          (() => {
                            const lastService = serviceEpisodes
                              .filter(ep => ep.endTime || ep.startTime)
                              .sort((a, b) => {
                                const timeA = new Date(a.endTime || a.startTime!).getTime();
                                const timeB = new Date(b.endTime || b.startTime!).getTime();
                                return timeB - timeA;
                              })[0];
                            
                            if (lastService) {
                              const lastTime = new Date(lastService.endTime || lastService.startTime!);
                              const daysDiff = Math.floor((Date.now() - lastTime.getTime()) / (1000 * 60 * 60 * 24));
                              return daysDiff === 0 ? 'Today' : `${daysDiff} day${daysDiff > 1 ? 's' : ''} ago`;
                            }
                            return 'No contact';
                          })()
                          : 'No contact'
                        }
                      </span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-secondary-600">Documents</span>
                      <span className="font-medium">12</span>
                    </div>
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardHeader>
                  <CardTitle>Risk Assessment</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-secondary-600">Lethality Assessment</span>
                      <Badge variant="destructive">High</Badge>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-secondary-600">Safety Planning</span>
                      <Badge variant="success">Complete</Badge>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-secondary-600">Last Updated</span>
                      <span className="text-sm">3 days ago</span>
                    </div>
                  </div>
                  <div className="mt-4 pt-4 border-t border-secondary-200">
                    <Button variant="outline" className="w-full">
                      <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                      </svg>
                      Update Assessment
                    </Button>
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardHeader>
                  <CardTitle>Quick Actions</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    <Button variant="outline" className="w-full justify-start">
                      <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                      </svg>
                      Schedule Appointment
                    </Button>
                    <Button variant="outline" className="w-full justify-start">
                      <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
                      </svg>
                      Contact Client
                    </Button>
                    <Button variant="outline" className="w-full justify-start">
                      <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 4V2a1 1 0 011-1h8a1 1 0 011 1v2h3a1 1 0 110 2h-1v12a2 2 0 01-2 2H7a2 2 0 01-2-2V6H4a1 1 0 110-2h3z" />
                      </svg>
                      Request Documents
                    </Button>
                  </div>
                </CardContent>
              </Card>
            </div>
          </div>
        </TabsContent>

        <TabsContent value="notes" className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div className="lg:col-span-2">
              <Card>
                <CardHeader>
                  <CardTitle>Case Timeline</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-6">
                    {timelineItems.map((item, index) => (
                      <CaseTimelineItem key={index} {...item} />
                    ))}
                    {notes?.map((note) => (
                      <CaseTimelineItem
                        key={note.id}
                        type="note"
                        title={note.noteType === 'CONFIDENTIAL' ? 'Confidential Note' : 'Case Note'}
                        description={note.content}
                        author={note.author}
                        timestamp={new Date(note.createdAt)}
                        metadata={note.isConfidential ? { confidential: 'Yes' } : undefined}
                      />
                    ))}
                  </div>
                </CardContent>
              </Card>
            </div>

            <div>
              <Card>
                <CardHeader>
                  <CardTitle>Add New Note</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    <Textarea
                      placeholder="Enter case note..."
                      value={newNote}
                      onChange={(e) => setNewNote(e.target.value)}
                      rows={4}
                    />
                    <div className="flex flex-col space-y-2">
                      <label className="flex items-center space-x-2">
                        <input
                          type="checkbox"
                          className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-secondary-300 rounded"
                        />
                        <span className="text-sm text-secondary-700">Mark as confidential</span>
                      </label>
                    </div>
                    <Button onClick={handleAddNote} className="w-full" disabled={!newNote.trim()}>
                      <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                      </svg>
                      Add Note
                    </Button>
                  </div>
                </CardContent>
              </Card>
            </div>
          </div>
        </TabsContent>

        <TabsContent value="services">
          <div className="space-y-6">
            {/* Service Episodes List */}
            <Card>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <CardTitle>Service Episodes</CardTitle>
                  <div className="flex items-center space-x-2">
                    <Button onClick={() => refetchServices()} variant="outline" size="sm">
                      <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                      </svg>
                      Refresh
                    </Button>
                    <Link href={`/services/new?clientId=${caseData.clientId}&enrollmentId=${enrollmentId}`}>
                      <Button size="sm">
                        <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                        </svg>
                        New Service
                      </Button>
                    </Link>
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                {servicesLoading ? (
                  <div className="space-y-4">
                    {Array.from({ length: 3 }).map((_, i) => (
                      <div key={i} className="animate-pulse border border-secondary-200 rounded-lg p-4">
                        <div className="flex items-center justify-between mb-3">
                          <div className="flex items-center space-x-3">
                            <div className="w-3 h-3 bg-secondary-200 rounded-full"></div>
                            <div>
                              <div className="h-4 bg-secondary-200 rounded w-32 mb-1"></div>
                              <div className="h-3 bg-secondary-200 rounded w-24"></div>
                            </div>
                          </div>
                          <div className="flex items-center space-x-2">
                            <div className="h-6 bg-secondary-200 rounded w-16"></div>
                            <div className="h-8 bg-secondary-200 rounded w-12"></div>
                          </div>
                        </div>
                        <div className="grid grid-cols-3 gap-4">
                          <div className="h-3 bg-secondary-200 rounded w-20"></div>
                          <div className="h-3 bg-secondary-200 rounded w-24"></div>
                          <div className="h-3 bg-secondary-200 rounded w-16"></div>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : serviceEpisodes && serviceEpisodes.length > 0 ? (
                  <div className="space-y-4">
                    {serviceEpisodes.map((episode) => (
                      <ServiceEpisodeCard 
                        key={episode.id} 
                        episode={episode} 
                        onRefresh={refetchServices}
                      />
                    ))}
                  </div>
                ) : (
                  <div className="text-center py-8">
                    <div className="max-w-sm mx-auto">
                      <svg className="w-16 h-16 mx-auto mb-4 text-secondary-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                      </svg>
                      <h3 className="text-lg font-medium text-secondary-900 mb-2">No Service Episodes</h3>
                      <p className="text-secondary-600 mb-4">This case doesn't have any service episodes yet.</p>
                      <Link href={`/services/new?clientId=${caseData.clientId}&enrollmentId=${enrollmentId}`}>
                        <Button>
                          <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                          </svg>
                          Create First Service
                        </Button>
                      </Link>
                    </div>
                  </div>
                )}

                {serviceEpisodes && serviceEpisodes.length > 0 && (
                  <div className="mt-6 text-center">
                    <Link href={`/services?clientId=${caseData.clientId}`}>
                      <Button variant="outline">
                        View All Services
                        <svg className="w-4 h-4 ml-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 8l4 4m0 0l-4 4m4-4H3" />
                        </svg>
                      </Button>
                    </Link>
                  </div>
                )}
              </CardContent>
            </Card>

            {/* Quick Service Creator */}
            <Card>
              <CardHeader>
                <CardTitle>Quick Service Creation</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <Link href={`/services/new?clientId=${caseData.clientId}&enrollmentId=${enrollmentId}&type=crisis`}>
                    <div className="p-4 border border-secondary-200 rounded-lg hover:border-primary-300 hover:bg-primary-50 transition-colors cursor-pointer">
                      <div className="flex items-center space-x-3 mb-2">
                        <div className="w-8 h-8 bg-red-100 rounded-lg flex items-center justify-center">
                          <svg className="w-4 h-4 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
                          </svg>
                        </div>
                        <div>
                          <h4 className="font-medium text-secondary-900">Crisis Response</h4>
                          <p className="text-sm text-secondary-600">Immediate intervention</p>
                        </div>
                      </div>
                      <p className="text-xs text-secondary-500">Start crisis intervention service immediately</p>
                    </div>
                  </Link>

                  <Link href={`/services/new?clientId=${caseData.clientId}&enrollmentId=${enrollmentId}&type=counseling`}>
                    <div className="p-4 border border-secondary-200 rounded-lg hover:border-primary-300 hover:bg-primary-50 transition-colors cursor-pointer">
                      <div className="flex items-center space-x-3 mb-2">
                        <div className="w-8 h-8 bg-blue-100 rounded-lg flex items-center justify-center">
                          <svg className="w-4 h-4 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 8h2a2 2 0 012 2v6a2 2 0 01-2 2h-2v4l-4-4H9a2 2 0 01-2-2v-6a2 2 0 012-2h8z" />
                          </svg>
                        </div>
                        <div>
                          <h4 className="font-medium text-secondary-900">Counseling</h4>
                          <p className="text-sm text-secondary-600">Therapy session</p>
                        </div>
                      </div>
                      <p className="text-xs text-secondary-500">Schedule or start counseling service</p>
                    </div>
                  </Link>

                  <Link href={`/services/new?clientId=${caseData.clientId}&enrollmentId=${enrollmentId}&type=case-management`}>
                    <div className="p-4 border border-secondary-200 rounded-lg hover:border-primary-300 hover:bg-primary-50 transition-colors cursor-pointer">
                      <div className="flex items-center space-x-3 mb-2">
                        <div className="w-8 h-8 bg-green-100 rounded-lg flex items-center justify-center">
                          <svg className="w-4 h-4 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
                          </svg>
                        </div>
                        <div>
                          <h4 className="font-medium text-secondary-900">Case Management</h4>
                          <p className="text-sm text-secondary-600">Coordination & advocacy</p>
                        </div>
                      </div>
                      <p className="text-xs text-secondary-500">Coordinate services and resources</p>
                    </div>
                  </Link>
                </div>
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="documents">
          <Card>
            <CardHeader>
              <CardTitle>Case Documents</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-center py-8">
                <p className="text-secondary-600">Document management coming soon...</p>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="safety">
          <Card>
            <CardHeader>
              <CardTitle>Safety Planning</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-center py-8">
                <p className="text-secondary-600">Safety planning tools coming soon...</p>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}

function CaseNotFound() {
  return (
    <div className="text-center py-12">
      <div className="max-w-md mx-auto">
        <svg className="w-24 h-24 mx-auto mb-6 text-secondary-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
        </svg>
        <h2 className="text-2xl font-semibold text-secondary-900 mb-2">Case Not Found</h2>
        <p className="text-secondary-600 mb-6">The case you're looking for doesn't exist or has been removed.</p>
        <div className="space-x-3">
          <Link href="/cases">
            <Button>Back to Cases</Button>
          </Link>
          <Link href="/dashboard">
            <Button variant="outline">Dashboard</Button>
          </Link>
        </div>
      </div>
    </div>
  );
}

export default function CaseDetailPage() {
  const router = useRouter();
  const { id } = router.query;
  const { case: caseData, loading, error } = useCase(id as string);

  if (loading) {
    return (
      <ProtectedRoute>
        <AppLayout title="Loading...">
          <div className="flex items-center justify-center min-h-96">
            <div className="text-center">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600 mx-auto mb-4"></div>
              <p className="text-secondary-600">Loading case details...</p>
            </div>
          </div>
        </AppLayout>
      </ProtectedRoute>
    );
  }

  if (error || !caseData) {
    return (
      <ProtectedRoute>
        <AppLayout 
          title="Case Not Found"
          breadcrumbs={[
            { label: 'Dashboard', href: '/dashboard' },
            { label: 'Cases', href: '/cases' },
            { label: 'Not Found' }
          ]}
        >
          <div className="p-6">
            <CaseNotFound />
          </div>
        </AppLayout>
      </ProtectedRoute>
    );
  }

  const caseNumber = caseData.caseNumber || `#${caseData.id.slice(0, 8)}`;

  return (
    <ProtectedRoute>
      <AppLayout 
        title={`Case ${caseNumber}`}
        breadcrumbs={[
          { label: 'Dashboard', href: '/dashboard' },
          { label: 'Cases', href: '/cases' },
          { label: caseNumber }
        ]}
      >
        <div className="p-6">
          <CaseDetailContent case={caseData} />
        </div>
      </AppLayout>
    </ProtectedRoute>
  );
}
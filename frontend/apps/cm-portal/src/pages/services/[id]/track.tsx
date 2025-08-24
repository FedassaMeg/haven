import { useState, useEffect, useRef } from 'react';
import { useRouter } from 'next/router';
import Link from 'next/link';
import { ProtectedRoute, useCurrentUser } from '@haven/auth';
import { Card, CardHeader, CardTitle, CardContent, Button, Input, Textarea, Badge, Modal } from '@haven/ui';
import { 
  useServiceEpisode, 
  useStartService,
  useCompleteService,
  usePauseService,
  useResumeService,
  useAddServiceNote,
  type ServiceEpisode,
  type CompleteServiceRequest 
} from '@haven/api-client';
import AppLayout from '../../../components/AppLayout';
import ServiceMilestones from '../../../components/ServiceMilestones';

interface TimerDisplayProps {
  startTime: string;
  isPaused?: boolean;
  pausedDuration?: number;
  onTick?: (currentDuration: number) => void;
}

const TimerDisplay: React.FC<TimerDisplayProps> = ({ 
  startTime, 
  isPaused = false, 
  pausedDuration = 0,
  onTick 
}) => {
  const [currentTime, setCurrentTime] = useState(new Date());
  const intervalRef = useRef<NodeJS.Timeout>();

  useEffect(() => {
    if (!isPaused) {
      intervalRef.current = setInterval(() => {
        const now = new Date();
        setCurrentTime(now);
        
        const elapsed = Math.floor((now.getTime() - new Date(startTime).getTime()) / (1000 * 60));
        const totalDuration = elapsed - pausedDuration;
        
        if (onTick) {
          onTick(totalDuration);
        }
      }, 1000);
    } else {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    }

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    };
  }, [startTime, isPaused, pausedDuration, onTick]);

  const calculateDuration = () => {
    const start = new Date(startTime);
    const elapsed = Math.floor((currentTime.getTime() - start.getTime()) / (1000 * 60));
    return Math.max(0, elapsed - pausedDuration);
  };

  const formatDuration = (totalMinutes: number) => {
    const hours = Math.floor(totalMinutes / 60);
    const minutes = totalMinutes % 60;
    const seconds = Math.floor(((currentTime.getTime() - new Date(startTime).getTime()) % 60000) / 1000);
    
    if (hours > 0) {
      return `${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
    }
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  };

  const duration = calculateDuration();

  return (
    <div className="text-center">
      <div className="text-6xl font-mono font-bold text-primary-600 mb-2">
        {formatDuration(duration)}
      </div>
      <div className="text-sm text-secondary-600">
        {isPaused ? (
          <span className="flex items-center justify-center space-x-1">
            <div className="w-2 h-2 bg-yellow-500 rounded-full animate-pulse"></div>
            <span>Paused</span>
          </span>
        ) : (
          <span className="flex items-center justify-center space-x-1">
            <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
            <span>Active</span>
          </span>
        )}
      </div>
      <div className="text-xs text-secondary-500 mt-1">
        Started: {new Date(startTime).toLocaleTimeString()}
      </div>
      {pausedDuration > 0 && (
        <div className="text-xs text-secondary-500">
          Paused time: {Math.floor(pausedDuration)} minutes
        </div>
      )}
    </div>
  );
};

interface QuickNoteProps {
  onAdd: (note: string) => void;
  loading?: boolean;
}

const QuickNote: React.FC<QuickNoteProps> = ({ onAdd, loading }) => {
  const [note, setNote] = useState('');
  const [showForm, setShowForm] = useState(false);

  const handleSubmit = () => {
    if (note.trim()) {
      onAdd(note.trim());
      setNote('');
      setShowForm(false);
    }
  };

  if (!showForm) {
    return (
      <Button 
        variant="outline" 
        onClick={() => setShowForm(true)}
        className="w-full"
      >
        <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
        </svg>
        Add Quick Note
      </Button>
    );
  }

  return (
    <div className="space-y-3">
      <Textarea
        placeholder="Add a quick note about the service progress..."
        value={note}
        onChange={(e) => setNote(e.target.value)}
        rows={3}
        autoFocus
      />
      <div className="flex items-center space-x-2">
        <Button onClick={handleSubmit} loading={loading} disabled={!note.trim()}>
          Add Note
        </Button>
        <Button variant="outline" onClick={() => setShowForm(false)}>
          Cancel
        </Button>
      </div>
    </div>
  );
};

function ServiceTrackingContent() {
  const router = useRouter();
  const { id } = router.query;
  const { user } = useCurrentUser();
  
  const { serviceEpisode, loading, refetch } = useServiceEpisode(id as string);
  const { startService, loading: startLoading } = useStartService();
  const { completeService, loading: completeLoading } = useCompleteService();
  const { pauseService, loading: pauseLoading } = usePauseService();
  const { resumeService, loading: resumeLoading } = useResumeService();
  const { addServiceNote, loading: noteLoading } = useAddServiceNote();

  const [showCompleteModal, setShowCompleteModal] = useState(false);
  const [currentDuration, setCurrentDuration] = useState(0);
  const [notes, setNotes] = useState<string[]>([]);

  const [completeForm, setCompleteForm] = useState<CompleteServiceRequest>({
    outcome: '',
    notes: '',
    status: 'COMPLETED_SUCCESSFULLY',
  });

  useEffect(() => {
    if (serviceEpisode?.actualDurationMinutes) {
      setCurrentDuration(serviceEpisode.actualDurationMinutes);
    }
  }, [serviceEpisode]);

  const handleStartService = async () => {
    if (!serviceEpisode?.id) return;
    
    try {
      await startService(serviceEpisode.id);
      await refetch();
    } catch (error) {
      console.error('Failed to start service:', error);
      alert('Failed to start service. Please try again.');
    }
  };

  const handlePauseService = async () => {
    if (!serviceEpisode?.id) return;
    
    try {
      await pauseService(serviceEpisode.id);
      await refetch();
    } catch (error) {
      console.error('Failed to pause service:', error);
      alert('Failed to pause service. Please try again.');
    }
  };

  const handleResumeService = async () => {
    if (!serviceEpisode?.id) return;
    
    try {
      await resumeService(serviceEpisode.id);
      await refetch();
    } catch (error) {
      console.error('Failed to resume service:', error);
      alert('Failed to resume service. Please try again.');
    }
  };

  const handleCompleteService = async () => {
    if (!serviceEpisode?.id) return;
    
    try {
      await completeService(serviceEpisode.id, completeForm);
      setShowCompleteModal(false);
      router.push(`/services/${serviceEpisode.id}`);
    } catch (error) {
      console.error('Failed to complete service:', error);
      alert('Failed to complete service. Please try again.');
    }
  };

  const handleAddNote = async (note: string) => {
    if (!serviceEpisode?.id) return;
    
    try {
      await addServiceNote(serviceEpisode.id, {
        note,
        timestamp: new Date().toISOString(),
        duration: currentDuration,
      });
      setNotes(prev => [...prev, `${new Date().toLocaleTimeString()}: ${note}`]);
    } catch (error) {
      console.error('Failed to add note:', error);
      alert('Failed to add note. Please try again.');
    }
  };

  const calculateBillableAmount = (duration: number) => {
    if (!serviceEpisode?.fundingSources || serviceEpisode.fundingSources.length === 0) {
      return duration * 1.5; // Default rate
    }
    
    // Calculate based on funding sources and their rates
    return serviceEpisode.fundingSources.reduce((total, funding) => {
      const rate = 1.5; // Base rate - would come from funding source configuration
      const allocation = funding.allocationPercentage / 100;
      return total + (duration * rate * allocation);
    }, 0);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (!serviceEpisode) {
    return (
      <div className="text-center py-8">
        <h3 className="text-lg font-medium text-secondary-900 mb-1">Service episode not found</h3>
        <p className="text-secondary-600 mb-4">The service episode you're trying to track doesn't exist.</p>
        <Link href="/services">
          <Button>Back to Services</Button>
        </Link>
      </div>
    );
  }

  const isActive = serviceEpisode.status === 'IN_PROGRESS';
  const isPaused = serviceEpisode.status === 'ON_HOLD';
  const canStart = serviceEpisode.status === 'CREATED';
  const expectedDuration = serviceEpisode.expectedDurationMinutes || 60;
  const isOvertime = currentDuration > expectedDuration;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-secondary-900">Service Tracking</h1>
          <p className="text-secondary-600">
            {serviceEpisode.serviceType} for {serviceEpisode.clientName}
          </p>
        </div>
        <div className="flex items-center space-x-3">
          <Link href={`/services/${serviceEpisode.id}`}>
            <Button variant="outline">View Details</Button>
          </Link>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Timer Section */}
        <div className="lg:col-span-2">
          <Card>
            <CardHeader>
              <CardTitle className="text-center">Service Timer</CardTitle>
            </CardHeader>
            <CardContent>
              {isActive || isPaused ? (
                <div className="space-y-6">
                  <TimerDisplay
                    startTime={serviceEpisode.startTime!}
                    isPaused={isPaused}
                    pausedDuration={serviceEpisode.pausedDurationMinutes || 0}
                    onTick={setCurrentDuration}
                  />
                  
                  {/* Duration Comparison */}
                  <div className="grid grid-cols-3 gap-4 text-center">
                    <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg">
                      <p className="text-sm text-blue-600">Expected</p>
                      <p className="text-lg font-bold text-blue-900">
                        {Math.floor(expectedDuration / 60)}h {expectedDuration % 60}m
                      </p>
                    </div>
                    <div className={`p-3 border rounded-lg ${isOvertime ? 'bg-red-50 border-red-200' : 'bg-green-50 border-green-200'}`}>
                      <p className={`text-sm ${isOvertime ? 'text-red-600' : 'text-green-600'}`}>Current</p>
                      <p className={`text-lg font-bold ${isOvertime ? 'text-red-900' : 'text-green-900'}`}>
                        {Math.floor(currentDuration / 60)}h {currentDuration % 60}m
                      </p>
                    </div>
                    <div className="p-3 bg-purple-50 border border-purple-200 rounded-lg">
                      <p className="text-sm text-purple-600">Billable</p>
                      <p className="text-lg font-bold text-purple-900">
                        ${calculateBillableAmount(currentDuration).toFixed(2)}
                      </p>
                    </div>
                  </div>

                  {isOvertime && (
                    <div className="p-3 bg-amber-50 border border-amber-200 rounded-lg">
                      <div className="flex items-center space-x-2">
                        <svg className="w-4 h-4 text-amber-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
                        </svg>
                        <span className="text-sm font-medium text-amber-800">Service is over expected duration</span>
                      </div>
                      <p className="text-sm text-amber-700 mt-1">
                        Consider completing the service or updating the expected duration.
                      </p>
                    </div>
                  )}

                  {/* Control Buttons */}
                  <div className="flex items-center justify-center space-x-4">
                    {isActive && (
                      <>
                        <Button 
                          variant="outline" 
                          onClick={handlePauseService} 
                          loading={pauseLoading}
                        >
                          <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 9v6m4-6v6" />
                          </svg>
                          Pause
                        </Button>
                        <Button 
                          onClick={() => setShowCompleteModal(true)}
                          className="bg-green-600 hover:bg-green-700"
                        >
                          <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                          </svg>
                          Complete Service
                        </Button>
                      </>
                    )}
                    
                    {isPaused && (
                      <Button 
                        onClick={handleResumeService} 
                        loading={resumeLoading}
                        className="bg-blue-600 hover:bg-blue-700"
                      >
                        <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.828 14.828a4 4 0 01-5.656 0M9 10h1m4 0h1m-6 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                        Resume
                      </Button>
                    )}
                  </div>
                </div>
              ) : canStart ? (
                <div className="text-center space-y-4">
                  <div className="py-8">
                    <svg className="w-16 h-16 text-secondary-400 mx-auto mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.828 14.828a4 4 0 01-5.656 0M9 10h1m4 0h1m-6 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    <h3 className="text-lg font-medium text-secondary-900 mb-2">Ready to Start</h3>
                    <p className="text-secondary-600 mb-4">Click the button below to begin tracking this service episode.</p>
                  </div>
                  <Button 
                    onClick={handleStartService} 
                    loading={startLoading}
                    size="lg"
                    className="bg-green-600 hover:bg-green-700"
                  >
                    <svg className="w-5 h-5 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.828 14.828a4 4 0 01-5.656 0M9 10h1m4 0h1m-6 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    Start Service
                  </Button>
                </div>
              ) : (
                <div className="text-center py-8">
                  <h3 className="text-lg font-medium text-secondary-900 mb-2">Service Completed</h3>
                  <p className="text-secondary-600">This service episode has been completed.</p>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Service Milestones */}
          {(isActive || isPaused) && (
            <ServiceMilestones
              episodeId={serviceEpisode.id}
              currentDuration={currentDuration}
              expectedDuration={expectedDuration}
              isActive={isActive}
            />
          )}

          {/* Service Progress Notes */}
          {(isActive || isPaused) && (
            <Card>
              <CardHeader>
                <CardTitle>Session Notes</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <QuickNote onAdd={handleAddNote} loading={noteLoading} />
                  
                  {notes.length > 0 && (
                    <div className="space-y-2">
                      <h4 className="font-medium text-secondary-900">Notes from this session:</h4>
                      <div className="space-y-2">
                        {notes.map((note, index) => (
                          <div key={index} className="p-2 bg-secondary-50 rounded text-sm">
                            {note}
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          )}
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Service Information */}
          <Card>
            <CardHeader>
              <CardTitle>Service Details</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <div>
                <label className="text-sm font-medium text-secondary-700">Service Type</label>
                <p className="text-secondary-900">{serviceEpisode.serviceType}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-secondary-700">Delivery Mode</label>
                <p className="text-secondary-900">{serviceEpisode.deliveryMode}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-secondary-700">Program</label>
                <p className="text-secondary-900">{serviceEpisode.programName}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-secondary-700">Provider</label>
                <p className="text-secondary-900">{serviceEpisode.primaryProviderName}</p>
              </div>
              {serviceEpisode.isConfidential && (
                <div className="p-2 bg-yellow-50 border border-yellow-200 rounded">
                  <div className="flex items-center space-x-1">
                    <svg className="w-4 h-4 text-yellow-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m0 0v2m0-2h2m-2 0h-2m8-8V7a4 4 0 10-8 0v4m8 0a2 2 0 012 2v6a2 2 0 01-2 2H6a2 2 0 01-2-2v-6a2 2 0 012-2h8z" />
                    </svg>
                    <span className="text-sm font-medium text-yellow-800">Confidential Service</span>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Time Milestones */}
          <Card>
            <CardHeader>
              <CardTitle>Time Milestones</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <div className="flex items-center justify-between text-sm">
                  <span className="text-secondary-600">25% Complete</span>
                  <span className={currentDuration >= expectedDuration * 0.25 ? 'text-green-600' : 'text-secondary-400'}>
                    {Math.floor(expectedDuration * 0.25)} min
                    {currentDuration >= expectedDuration * 0.25 && ' ✓'}
                  </span>
                </div>
                <div className="flex items-center justify-between text-sm">
                  <span className="text-secondary-600">50% Complete</span>
                  <span className={currentDuration >= expectedDuration * 0.5 ? 'text-green-600' : 'text-secondary-400'}>
                    {Math.floor(expectedDuration * 0.5)} min
                    {currentDuration >= expectedDuration * 0.5 && ' ✓'}
                  </span>
                </div>
                <div className="flex items-center justify-between text-sm">
                  <span className="text-secondary-600">75% Complete</span>
                  <span className={currentDuration >= expectedDuration * 0.75 ? 'text-green-600' : 'text-secondary-400'}>
                    {Math.floor(expectedDuration * 0.75)} min
                    {currentDuration >= expectedDuration * 0.75 && ' ✓'}
                  </span>
                </div>
                <div className="flex items-center justify-between text-sm">
                  <span className="text-secondary-600">Expected Complete</span>
                  <span className={currentDuration >= expectedDuration ? 'text-amber-600' : 'text-secondary-400'}>
                    {expectedDuration} min
                    {currentDuration >= expectedDuration && ' ⚠️'}
                  </span>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Quick Actions */}
          <Card>
            <CardHeader>
              <CardTitle>Quick Actions</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-2">
                <Link href={`/clients/${serviceEpisode.clientId}`}>
                  <Button variant="outline" className="w-full justify-start">
                    <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                    </svg>
                    View Client
                  </Button>
                </Link>
                <Link href={`/services/new?clientId=${serviceEpisode.clientId}&enrollmentId=${serviceEpisode.enrollmentId}`}>
                  <Button variant="outline" className="w-full justify-start">
                    <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                    </svg>
                    Create Follow-up
                  </Button>
                </Link>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Complete Service Modal */}
      <Modal isOpen={showCompleteModal} onClose={() => setShowCompleteModal(false)}>
        <div className="p-6">
          <h3 className="text-lg font-medium text-secondary-900 mb-4">Complete Service Episode</h3>
          
          <div className="space-y-4">
            <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg">
              <div className="text-sm text-blue-800">
                <strong>Final Duration:</strong> {Math.floor(currentDuration / 60)}h {currentDuration % 60}m
                <br />
                <strong>Billable Amount:</strong> ${calculateBillableAmount(currentDuration).toFixed(2)}
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-secondary-700 mb-1">Completion Status</label>
              <select
                value={completeForm.status}
                onChange={(e) => setCompleteForm(prev => ({ ...prev, status: e.target.value as any }))}
                className="block w-full px-3 py-2 border border-secondary-300 rounded-md"
              >
                <option value="COMPLETED_SUCCESSFULLY">Completed Successfully</option>
                <option value="COMPLETED_WITH_CONCERNS">Completed with Concerns</option>
                <option value="PARTIALLY_COMPLETED">Partially Completed</option>
                <option value="CANCELLED_BY_CLIENT">Cancelled by Client</option>
                <option value="CANCELLED_BY_PROVIDER">Cancelled by Provider</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-secondary-700 mb-1">Outcome Summary</label>
              <Textarea
                value={completeForm.outcome}
                onChange={(e) => setCompleteForm(prev => ({ ...prev, outcome: e.target.value }))}
                placeholder="Describe the service outcome and any achievements..."
                rows={3}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-secondary-700 mb-1">Service Notes</label>
              <Textarea
                value={completeForm.notes}
                onChange={(e) => setCompleteForm(prev => ({ ...prev, notes: e.target.value }))}
                placeholder="Additional notes about the service delivery..."
                rows={3}
              />
            </div>
          </div>

          <div className="flex items-center justify-end space-x-3 mt-6">
            <Button variant="outline" onClick={() => setShowCompleteModal(false)}>
              Cancel
            </Button>
            <Button onClick={handleCompleteService} loading={completeLoading}>
              Complete Service
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}

export default function ServiceTrackingPage() {
  return (
    <ProtectedRoute>
      <AppLayout 
        title="Service Tracking" 
        breadcrumbs={[
          { label: 'Dashboard', href: '/dashboard' },
          { label: 'Services', href: '/services' },
          { label: 'Track Service' }
        ]}
      >
        <div className="p-6">
          <ServiceTrackingContent />
        </div>
      </AppLayout>
    </ProtectedRoute>
  );
}
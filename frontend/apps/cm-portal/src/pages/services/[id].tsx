import { useState, useEffect } from 'react';
import { useRouter } from 'next/router';
import Link from 'next/link';
import { ProtectedRoute, useCurrentUser } from '@haven/auth';
import { Card, CardHeader, CardTitle, CardContent, Button, Input, Textarea, Badge, Modal } from '@haven/ui';
import { 
  useServiceEpisode, 
  useStartService,
  useCompleteService,
  useUpdateOutcome,
  type ServiceEpisode,
  type CompleteServiceRequest,
  type UpdateOutcomeRequest 
} from '@haven/api-client';
import AppLayout from '../../components/AppLayout';

const SERVICE_STATUS_COLORS = {
  CREATED: 'bg-gray-100 text-gray-800',
  IN_PROGRESS: 'bg-blue-100 text-blue-800',
  COMPLETED: 'bg-green-100 text-green-800',
  CANCELLED: 'bg-red-100 text-red-800',
  ON_HOLD: 'bg-yellow-100 text-yellow-800',
};

function ServiceEpisodeContent() {
  const router = useRouter();
  const { id } = router.query;
  const { user } = useCurrentUser();
  
  const { serviceEpisode, loading, refetch } = useServiceEpisode(id as string);
  const { startService, loading: startLoading } = useStartService();
  const { completeService, loading: completeLoading } = useCompleteService();
  const { updateOutcome, loading: updateLoading } = useUpdateOutcome();

  const [showCompleteModal, setShowCompleteModal] = useState(false);
  const [showOutcomeModal, setShowOutcomeModal] = useState(false);
  const [currentTime, setCurrentTime] = useState<Date>(new Date());

  // Update current time every minute for real-time tracking
  useEffect(() => {
    const interval = setInterval(() => setCurrentTime(new Date()), 60000);
    return () => clearInterval(interval);
  }, []);

  const [completeForm, setCompleteForm] = useState<CompleteServiceRequest>({
    outcome: '',
    notes: '',
    status: 'COMPLETED_SUCCESSFULLY',
  });

  const [outcomeForm, setOutcomeForm] = useState<UpdateOutcomeRequest>({
    outcome: '',
    followUpRequired: '',
    followUpDate: undefined,
  });

  const formatDuration = (minutes?: number) => {
    if (!minutes) return '0 minutes';
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    if (hours > 0) {
      return `${hours}h ${mins}m`;
    }
    return `${mins}m`;
  };

  const formatCurrency = (amount?: number) => {
    if (!amount) return '$0.00';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(amount);
  };

  const calculateCurrentDuration = (startTime?: string) => {
    if (!startTime) return 0;
    const start = new Date(startTime);
    const diffMs = currentTime.getTime() - start.getTime();
    return Math.floor(diffMs / (1000 * 60)); // Convert to minutes
  };

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

  const handleCompleteService = async () => {
    if (!serviceEpisode?.id) return;
    
    try {
      await completeService(serviceEpisode.id, completeForm);
      setShowCompleteModal(false);
      await refetch();
    } catch (error) {
      console.error('Failed to complete service:', error);
      alert('Failed to complete service. Please try again.');
    }
  };

  const handleUpdateOutcome = async () => {
    if (!serviceEpisode?.id) return;
    
    try {
      await updateOutcome(serviceEpisode.id, outcomeForm);
      setShowOutcomeModal(false);
      await refetch();
    } catch (error) {
      console.error('Failed to update outcome:', error);
      alert('Failed to update outcome. Please try again.');
    }
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
        <p className="text-secondary-600 mb-4">The service episode you're looking for doesn't exist.</p>
        <Link href="/services">
          <Button>Back to Services</Button>
        </Link>
      </div>
    );
  }

  const currentDuration = serviceEpisode.status === 'IN_PROGRESS' 
    ? calculateCurrentDuration(serviceEpisode.startTime) 
    : serviceEpisode.actualDurationMinutes || 0;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-secondary-900">Service Episode</h1>
          <p className="text-secondary-600">{serviceEpisode.serviceType} for {serviceEpisode.clientName}</p>
        </div>
        <div className="flex items-center space-x-3">
          <Link href="/services">
            <Button variant="outline">Back to Services</Button>
          </Link>
          
          {serviceEpisode.status === 'CREATED' && (
            <Button onClick={handleStartService} loading={startLoading}>
              <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.828 14.828a4 4 0 01-5.656 0M9 10h1m4 0h1m-6 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              Start Service
            </Button>
          )}
          
          {serviceEpisode.status === 'IN_PROGRESS' && (
            <Button onClick={() => setShowCompleteModal(true)}>
              <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
              </svg>
              Complete Service
            </Button>
          )}

          {serviceEpisode.status === 'COMPLETED' && (
            <Button variant="outline" onClick={() => setShowOutcomeModal(true)}>
              <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
              </svg>
              Update Outcome
            </Button>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Content */}
        <div className="lg:col-span-2 space-y-6">
          {/* Service Overview */}
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle>Service Overview</CardTitle>
                <Badge className={SERVICE_STATUS_COLORS[serviceEpisode.status as keyof typeof SERVICE_STATUS_COLORS]}>
                  {serviceEpisode.status.replace('_', ' ')}
                </Badge>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-medium text-secondary-700">Client</label>
                  <p className="text-secondary-900">{serviceEpisode.clientName}</p>
                  <p className="text-sm text-secondary-500">ID: {serviceEpisode.clientId.slice(0, 8)}</p>
                </div>
                
                <div>
                  <label className="text-sm font-medium text-secondary-700">Service Type</label>
                  <p className="text-secondary-900">{serviceEpisode.serviceType}</p>
                  <p className="text-sm text-secondary-500">{serviceEpisode.deliveryMode}</p>
                </div>

                <div>
                  <label className="text-sm font-medium text-secondary-700">Program</label>
                  <p className="text-secondary-900">{serviceEpisode.programName}</p>
                  <p className="text-sm text-secondary-500">Enrollment: {serviceEpisode.enrollmentId}</p>
                </div>

                <div>
                  <label className="text-sm font-medium text-secondary-700">Primary Provider</label>
                  <p className="text-secondary-900">{serviceEpisode.primaryProviderName}</p>
                </div>
              </div>

              {serviceEpisode.description && (
                <div>
                  <label className="text-sm font-medium text-secondary-700">Description</label>
                  <p className="text-secondary-900">{serviceEpisode.description}</p>
                </div>
              )}

              {serviceEpisode.isConfidential && (
                <div className="p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
                  <div className="flex items-center space-x-2">
                    <svg className="w-4 h-4 text-yellow-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m0 0v2m0-2h2m-2 0h-2m8-8V7a4 4 0 10-8 0v4m8 0a2 2 0 012 2v6a2 2 0 01-2 2H6a2 2 0 01-2-2v-6a2 2 0 012-2h8z" />
                    </svg>
                    <span className="text-sm font-medium text-yellow-800">Confidential Service</span>
                  </div>
                  <p className="text-sm text-yellow-700 mt-1">This service requires special confidential handling.</p>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Time Tracking */}
          <Card>
            <CardHeader>
              <CardTitle>Time Tracking</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="text-center p-4 bg-blue-50 border border-blue-200 rounded-lg">
                  <p className="text-sm text-blue-600 mb-1">Expected Duration</p>
                  <p className="text-2xl font-bold text-blue-900">
                    {formatDuration(serviceEpisode.expectedDurationMinutes)}
                  </p>
                </div>

                <div className="text-center p-4 bg-green-50 border border-green-200 rounded-lg">
                  <p className="text-sm text-green-600 mb-1">Current Duration</p>
                  <p className="text-2xl font-bold text-green-900">
                    {formatDuration(currentDuration)}
                  </p>
                  {serviceEpisode.status === 'IN_PROGRESS' && (
                    <p className="text-xs text-green-600 mt-1">Live tracking</p>
                  )}
                </div>

                <div className="text-center p-4 bg-purple-50 border border-purple-200 rounded-lg">
                  <p className="text-sm text-purple-600 mb-1">Billable Amount</p>
                  <p className="text-2xl font-bold text-purple-900">
                    {formatCurrency(serviceEpisode.billableAmount)}
                  </p>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                <div>
                  <label className="text-secondary-600">Created</label>
                  <p className="text-secondary-900">
                    {serviceEpisode.createdAt ? new Date(serviceEpisode.createdAt).toLocaleString() : '-'}
                  </p>
                </div>
                
                <div>
                  <label className="text-secondary-600">Last Modified</label>
                  <p className="text-secondary-900">
                    {serviceEpisode.lastModified ? new Date(serviceEpisode.lastModified).toLocaleString() : '-'}
                  </p>
                </div>

                {serviceEpisode.startTime && (
                  <div>
                    <label className="text-secondary-600">Started</label>
                    <p className="text-secondary-900">
                      {new Date(serviceEpisode.startTime).toLocaleString()}
                    </p>
                  </div>
                )}

                {serviceEpisode.endTime && (
                  <div>
                    <label className="text-secondary-600">Completed</label>
                    <p className="text-secondary-900">
                      {new Date(serviceEpisode.endTime).toLocaleString()}
                    </p>
                  </div>
                )}
              </div>
            </CardContent>
          </Card>

          {/* Outcomes */}
          {(serviceEpisode.outcome || serviceEpisode.notes) && (
            <Card>
              <CardHeader>
                <CardTitle>Outcomes & Notes</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                {serviceEpisode.outcome && (
                  <div>
                    <label className="text-sm font-medium text-secondary-700">Outcome</label>
                    <p className="text-secondary-900">{serviceEpisode.outcome}</p>
                  </div>
                )}

                {serviceEpisode.notes && (
                  <div>
                    <label className="text-sm font-medium text-secondary-700">Notes</label>
                    <p className="text-secondary-900">{serviceEpisode.notes}</p>
                  </div>
                )}

                {serviceEpisode.followUpRequired && (
                  <div>
                    <label className="text-sm font-medium text-secondary-700">Follow-up Required</label>
                    <p className="text-secondary-900">{serviceEpisode.followUpRequired}</p>
                    {serviceEpisode.followUpDate && (
                      <p className="text-sm text-secondary-600">
                        Due: {new Date(serviceEpisode.followUpDate).toLocaleDateString()}
                      </p>
                    )}
                  </div>
                )}
              </CardContent>
            </Card>
          )}
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Funding Sources */}
          {serviceEpisode.fundingSources && serviceEpisode.fundingSources.length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle>Funding Sources</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  {serviceEpisode.fundingSources.map((funding, index) => (
                    <div key={index} className="p-3 border border-secondary-200 rounded-lg">
                      <div className="flex items-center justify-between mb-1">
                        <span className="font-medium text-secondary-900">{funding.funderName}</span>
                        <span className="text-sm text-secondary-600">{funding.allocationPercentage}%</span>
                      </div>
                      {funding.grantNumber && (
                        <p className="text-sm text-secondary-600">Grant: {funding.grantNumber}</p>
                      )}
                      {funding.programName && (
                        <p className="text-sm text-secondary-600">Program: {funding.programName}</p>
                      )}
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}

          {/* Additional Providers */}
          {serviceEpisode.additionalProviders && serviceEpisode.additionalProviders.length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle>Additional Providers</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-2">
                  {serviceEpisode.additionalProviders.map((provider, index) => (
                    <div key={index} className="flex items-center justify-between p-2 bg-secondary-50 rounded">
                      <span className="text-secondary-900">{provider.providerName}</span>
                      {provider.role && (
                        <span className="text-sm text-secondary-600">{provider.role}</span>
                      )}
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}

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
                    View Client Profile
                  </Button>
                </Link>
                
                <Link href={`/services/new?clientId=${serviceEpisode.clientId}&enrollmentId=${serviceEpisode.enrollmentId}`}>
                  <Button variant="outline" className="w-full justify-start">
                    <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                    </svg>
                    Create Follow-up Service
                  </Button>
                </Link>

                {serviceEpisode.status === 'COMPLETED' && (
                  <Button variant="outline" className="w-full justify-start">
                    <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                    Generate Report
                  </Button>
                )}
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

      {/* Update Outcome Modal */}
      <Modal isOpen={showOutcomeModal} onClose={() => setShowOutcomeModal(false)}>
        <div className="p-6">
          <h3 className="text-lg font-medium text-secondary-900 mb-4">Update Service Outcome</h3>
          
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-secondary-700 mb-1">Updated Outcome</label>
              <Textarea
                value={outcomeForm.outcome}
                onChange={(e) => setOutcomeForm(prev => ({ ...prev, outcome: e.target.value }))}
                placeholder="Update the service outcome..."
                rows={3}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-secondary-700 mb-1">Follow-up Required</label>
              <Textarea
                value={outcomeForm.followUpRequired}
                onChange={(e) => setOutcomeForm(prev => ({ ...prev, followUpRequired: e.target.value }))}
                placeholder="Describe any follow-up actions needed..."
                rows={2}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-secondary-700 mb-1">Follow-up Date</label>
              <Input
                type="date"
                value={outcomeForm.followUpDate}
                onChange={(e) => setOutcomeForm(prev => ({ ...prev, followUpDate: e.target.value }))}
              />
            </div>
          </div>

          <div className="flex items-center justify-end space-x-3 mt-6">
            <Button variant="outline" onClick={() => setShowOutcomeModal(false)}>
              Cancel
            </Button>
            <Button onClick={handleUpdateOutcome} loading={updateLoading}>
              Update Outcome
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}

export default function ServiceEpisodePage() {
  return (
    <ProtectedRoute>
      <AppLayout 
        title="Service Episode" 
        breadcrumbs={[
          { label: 'Dashboard', href: '/dashboard' },
          { label: 'Services', href: '/services' },
          { label: 'Service Episode' }
        ]}
      >
        <div className="p-6">
          <ServiceEpisodeContent />
        </div>
      </AppLayout>
    </ProtectedRoute>
  );
}
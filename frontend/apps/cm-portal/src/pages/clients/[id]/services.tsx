import React, { useState } from 'react';
import { useRouter } from 'next/router';
import { ProtectedRoute, useCurrentUser } from '@haven/auth';
import { Card, CardHeader, CardTitle, CardContent, Button, Tabs, TabsList, TabsTrigger, TabsContent } from '@haven/ui';
import {
  useServiceEpisodes,
  useCreateServiceEpisode,
  useStartService,
  useCompleteService,
  useUpdateServiceOutcome,
  ServiceEpisode,
  ServiceType,
  ServiceDeliveryMode,
  FundingSource
} from '@haven/api-client';
import AppLayout from '../../../components/AppLayout';
import ServiceEpisodeList from '../../../components/services/ServiceEpisodeList';
import ServiceCalendar from '../../../components/services/ServiceCalendar';
import CreateServiceEpisodeModal from '../../../components/services/CreateServiceEpisodeModal';
import ServiceDashboard from '../../../components/services/ServiceDashboard';
import ServiceReports from '../../../components/services/ServiceReports';

function ClientServicesContent() {
  const router = useRouter();
  const { user } = useCurrentUser();
  const { id: clientId } = router.query;

  // API hooks
  const { data: serviceEpisodes = [], isLoading, error, refetch } = useServiceEpisodes({ clientId: clientId as string });
  const createServiceMutation = useCreateServiceEpisode();
  const startServiceMutation = useStartService();
  const completeServiceMutation = useCompleteService();
  const updateOutcomeMutation = useUpdateServiceOutcome();

  // UI state
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [activeTab, setActiveTab] = useState('list');

  const handleCreateService = async (serviceData: {
    enrollmentId: string;
    programId: string;
    programName: string;
    serviceType: ServiceType;
    deliveryMode: ServiceDeliveryMode;
    serviceDate: string;
    plannedDurationMinutes: number;
    primaryProviderName: string;
    fundingSource: FundingSource;
    serviceDescription: string;
    isConfidential: boolean;
  }) => {
    try {
      await createServiceMutation.mutateAsync({
        clientId: clientId as string,
        ...serviceData,
        primaryProviderId: user?.id || '',
      });
      setCreateModalOpen(false);
      refetch();
    } catch (error) {
      console.error('Failed to create service episode:', error);
    }
  };

  const handleStartService = async (episodeId: string, location: string) => {
    try {
      await startServiceMutation.mutateAsync({
        episodeId,
        startTime: new Date().toISOString(),
        location,
      });
      refetch();
    } catch (error) {
      console.error('Failed to start service:', error);
    }
  };

  const handleCompleteService = async (episodeId: string, outcome: string, notes: string) => {
    try {
      await completeServiceMutation.mutateAsync({
        episodeId,
        endTime: new Date().toISOString(),
        outcome,
        status: 'COMPLETED',
        notes,
      });
      refetch();
    } catch (error) {
      console.error('Failed to complete service:', error);
    }
  };

  const handleUpdateOutcome = async (episodeId: string, outcome: string, followUpRequired?: string, followUpDate?: string) => {
    try {
      await updateOutcomeMutation.mutateAsync({
        episodeId,
        outcome,
        followUpRequired,
        followUpDate,
      });
      refetch();
    } catch (error) {
      console.error('Failed to update outcome:', error);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Service Episodes</h1>
          <p className="text-slate-600">Comprehensive service delivery tracking and management</p>
        </div>
        <div className="flex items-center space-x-3">
          <Button
            onClick={() => setCreateModalOpen(true)}
            className="bg-blue-600 hover:bg-blue-700"
          >
            Create Service Episode
          </Button>
        </div>
      </div>

      {/* Service Management Tabs */}
      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList>
          <TabsTrigger value="list">Service List</TabsTrigger>
          <TabsTrigger value="calendar">Calendar</TabsTrigger>
          <TabsTrigger value="dashboard">Dashboard</TabsTrigger>
          <TabsTrigger value="reports">Reports</TabsTrigger>
        </TabsList>

        <TabsContent value="list" className="space-y-6">
          <ServiceEpisodeList
            serviceEpisodes={serviceEpisodes}
            onStartService={handleStartService}
            onCompleteService={handleCompleteService}
            onUpdateOutcome={handleUpdateOutcome}
            loading={isLoading}
            error={error}
          />
        </TabsContent>

        <TabsContent value="calendar" className="space-y-6">
          <ServiceCalendar
            serviceEpisodes={serviceEpisodes}
            onCreateService={handleCreateService}
            onStartService={handleStartService}
            onCompleteService={handleCompleteService}
            loading={isLoading}
          />
        </TabsContent>

        <TabsContent value="dashboard" className="space-y-6">
          <ServiceDashboard
            serviceEpisodes={serviceEpisodes}
            clientId={clientId as string}
            loading={isLoading}
          />
        </TabsContent>

        <TabsContent value="reports" className="space-y-6">
          <ServiceReports
            serviceEpisodes={serviceEpisodes}
            clientId={clientId as string}
            loading={isLoading}
          />
        </TabsContent>
      </Tabs>

      {/* Create Service Episode Modal */}
      <CreateServiceEpisodeModal
        isOpen={createModalOpen}
        onClose={() => setCreateModalOpen(false)}
        onCreate={handleCreateService}
        loading={createServiceMutation.isPending}
        clientId={clientId as string}
      />

      {/* Service Statistics Card */}
      <Card>
        <CardHeader>
          <CardTitle>Service Summary</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div className="text-center">
              <div className="text-2xl font-bold text-blue-600">
                {serviceEpisodes.length}
              </div>
              <div className="text-sm text-slate-600">Total Episodes</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-green-600">
                {serviceEpisodes.filter(s => s.completionStatus === 'COMPLETED').length}
              </div>
              <div className="text-sm text-slate-600">Completed</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-yellow-600">
                {serviceEpisodes.filter(s => s.completionStatus === 'IN_PROGRESS').length}
              </div>
              <div className="text-sm text-slate-600">In Progress</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-purple-600">
                {serviceEpisodes.filter(s => s.requiresFollowUp).length}
              </div>
              <div className="text-sm text-slate-600">Need Follow-up</div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

export default function ClientServicesPage() {
  return (
    <ProtectedRoute requiredRoles={['admin', 'supervisor', 'case_manager', 'clinician', 'legal_advocate']}>
      <AppLayout
        title="Client Services"
        breadcrumbs={[
          { label: 'Dashboard', href: '/dashboard' },
          { label: 'Clients', href: '/clients' },
          { label: 'Client Details', href: '/clients/[id]' },
          { label: 'Services' }
        ]}
      >
        <div className="p-6">
          <ClientServicesContent />
        </div>
      </AppLayout>
    </ProtectedRoute>
  );
}
import {
  useClients,
  useCreateServiceEpisode,
  useServiceDeliveryModes,
  useServiceTypes,
  type Client,
  type CreateServiceEpisodeRequest,
  type ServiceDeliveryMode,
  type ServiceType
} from '@haven/api-client';
import { ProtectedRoute, useCurrentUser } from '@haven/auth';
import { Button, Card, CardContent, CardHeader, CardTitle, Input, Select, Textarea } from '@haven/ui';
import Link from 'next/link';
import { useRouter } from 'next/router';
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import AppLayout from '../../components/AppLayout';

interface ServiceFormData {
  clientId: string;
  enrollmentId: string;
  programId: string;
  programName: string;
  serviceType: ServiceType | '';
  deliveryMode: ServiceDeliveryMode | '';
  primaryProviderId: string;
  primaryProviderName: string;
  description: string;
  isConfidential: boolean;
  
  // Funding allocation
  fundingSources: Array<{
    funderId: string;
    funderName: string;
    grantNumber: string;
    programName: string;
    allocationPercentage: number;
  }>;
  
  // Quick start options
  startImmediately: boolean;
  expectedDurationMinutes?: number;
}

const PROGRAMS = [
  { value: 'emergency-shelter', label: 'Emergency Shelter Program' },
  { value: 'transitional-housing', label: 'Transitional Housing Program' },
  { value: 'domestic-violence', label: 'Domestic Violence Program' },
  { value: 'crisis-intervention', label: 'Crisis Intervention Program' },
  { value: 'counseling-services', label: 'Counseling Services Program' },
  { value: 'case-management', label: 'Case Management Program' },
  { value: 'legal-advocacy', label: 'Legal Advocacy Program' },
  { value: 'substance-abuse', label: 'Substance Abuse Program' },
];

const COMMON_FUNDERS = [
  { id: 'hud-coc', name: 'HUD Continuum of Care', type: 'FEDERAL' },
  { id: 'vawa', name: 'VAWA (Violence Against Women Act)', type: 'FEDERAL' },
  { id: 'cal-oes', name: 'Cal OES (California Office of Emergency Services)', type: 'STATE' },
  { id: 'cdbg', name: 'CDBG (Community Development Block Grant)', type: 'FEDERAL' },
  { id: 'local-foundation', name: 'Local Foundation Grant', type: 'PRIVATE' },
];

function NewServiceContent() {
  const router = useRouter();
  const { user } = useCurrentUser();
  const { clients } = useClients({ activeOnly: true });
  const { serviceTypes } = useServiceTypes();
  const { deliveryModes } = useServiceDeliveryModes();
  const { createServiceEpisode, loading } = useCreateServiceEpisode();
  
  const initialValues: ServiceFormData = {
    clientId: '',
    enrollmentId: '',
    programId: '',
    programName: '',
    serviceType: '',
    deliveryMode: '',
    primaryProviderId: user?.id || '',
    primaryProviderName: user?.name || '',
    description: '',
    isConfidential: false,
    fundingSources: [],
    startImmediately: false,
    expectedDurationMinutes: undefined,
  };

  const { values, errors, setValue, validate, reset } = useForm(initialValues, {
    clientId: { required: 'Client selection is required' },
    enrollmentId: { required: 'Enrollment ID is required' },
    programId: { required: 'Program selection is required' },
    serviceType: { required: 'Service type is required' },
    deliveryMode: { required: 'Delivery mode is required' },
    primaryProviderName: { required: 'Provider name is required' },
    description: { required: 'Service description is required', minLength: 10 },
  });

  const [selectedClient, setSelectedClient] = useState<Client | null>(null);
  const [selectedServiceType, setSelectedServiceType] = useState<any>(null);

  // Pre-populate fields from query parameters
  useEffect(() => {
    if (router.query.clientId) {
      setValue('clientId', router.query.clientId as string);
    }
    if (router.query.enrollmentId) {
      setValue('enrollmentId', router.query.enrollmentId as string);
    }
    
    // Pre-select service type based on type parameter
    if (router.query.type && serviceTypes) {
      const typeMap: Record<string, ServiceType> = {
        'crisis': 'CRISIS_INTERVENTION',
        'counseling': 'INDIVIDUAL_COUNSELING',
        'case-management': 'CASE_MANAGEMENT'
      };
      
      const serviceType = typeMap[router.query.type as string];
      if (serviceType) {
        setValue('serviceType', serviceType);
      }
    }
    
    // Set program based on service type
    if (router.query.type) {
      const programMap: Record<string, string> = {
        'crisis': 'crisis-intervention',
        'counseling': 'counseling-services',
        'case-management': 'case-management'
      };
      
      const programId = programMap[router.query.type as string];
      if (programId) {
        setValue('programId', programId);
        const program = PROGRAMS.find(p => p.value === programId);
        if (program) {
          setValue('programName', program.label);
        }
      }
    }
  }, [router.query, setValue, serviceTypes]);

  useEffect(() => {
    if (values.clientId && clients) {
      const client = clients.find(c => c.id === values.clientId);
      setSelectedClient(client || null);
    } else {
      setSelectedClient(null);
    }
  }, [values.clientId, clients]);

  useEffect(() => {
    if (values.serviceType && serviceTypes) {
      const serviceType = serviceTypes.find(st => st.name === values.serviceType);
      setSelectedServiceType(serviceType || null);
      
      // Auto-set confidentiality based on service type
      if (serviceType?.requiresConfidentialHandling) {
        setValue('isConfidential', true);
      }
      
      // Set expected duration from service type
      if (serviceType?.typicalMinDuration) {
        setValue('expectedDurationMinutes', serviceType.typicalMinDuration);
      }
    }
  }, [values.serviceType, serviceTypes, setValue]);

  const addFundingSource = () => {
    setValue('fundingSources', [
      ...values.fundingSources,
      {
        funderId: '',
        funderName: '',
        grantNumber: '',
        programName: '',
        allocationPercentage: 100 - values.fundingSources.reduce((sum, fs) => sum + fs.allocationPercentage, 0),
      }
    ]);
  };

  const updateFundingSource = (index: number, field: string, value: any) => {
    const updated = [...values.fundingSources];
    updated[index] = { ...updated[index], [field]: value };
    setValue('fundingSources', updated);
  };

  const removeFundingSource = (index: number) => {
    setValue('fundingSources', values.fundingSources.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    const formErrors = validate();
    if (Object.keys(formErrors).length > 0) return;

    // Validate funding allocation
    const totalAllocation = values.fundingSources.reduce((sum, fs) => sum + fs.allocationPercentage, 0);
    if (values.fundingSources.length > 0 && Math.abs(totalAllocation - 100) > 0.01) {
      alert('Funding allocation must total 100%');
      return;
    }

    try {
      const serviceData: CreateServiceEpisodeRequest = {
        clientId: values.clientId,
        enrollmentId: values.enrollmentId,
        programId: values.programId,
        programName: values.programName,
        serviceType: values.serviceType as ServiceType,
        deliveryMode: values.deliveryMode as ServiceDeliveryMode,
        primaryProviderId: values.primaryProviderId,
        primaryProviderName: values.primaryProviderName,
        description: values.description,
        isConfidential: values.isConfidential,
        fundingSources: values.fundingSources,
        expectedDurationMinutes: values.expectedDurationMinutes,
      };

      const newEpisode = await createServiceEpisode(serviceData);
      
      // Start immediately if requested
      if (values.startImmediately) {
        // Navigate to service tracking page
        router.push(`/services/${newEpisode.id}/track`);
      } else {
        router.push(`/services/${newEpisode.id}`);
      }
    } catch (error) {
      console.error('Failed to create service episode:', error);
      alert('Failed to create service episode. Please try again.');
    }
  };

  const clientOptions = clients?.map(client => ({
    value: client.id,
    label: client.name ? 
      `${client.name.given?.join(' ') || ''} ${client.name.family || ''}`.trim() :
      `Client ${client.id.slice(0, 8)}`
  })) || [];

  const serviceTypeOptions = serviceTypes?.map(st => ({
    value: st.name,
    label: `${st.name} - ${st.description}`,
    disabled: false
  })) || [];

  const deliveryModeOptions = deliveryModes?.map(dm => ({
    value: dm.name,
    label: `${dm.name} - ${dm.description}`,
    disabled: false
  })) || [];

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-secondary-900">Create Service Episode</h1>
          <p className="text-secondary-600">Start a new service delivery episode with duration tracking</p>
        </div>
        <div className="flex items-center space-x-3">
          <Link href="/services">
            <Button variant="outline">Cancel</Button>
          </Link>
          <Button type="submit" loading={loading}>
            <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
            </svg>
            Create Service
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Form */}
        <div className="lg:col-span-2 space-y-6">
          {/* Client & Program Information */}
          <Card>
            <CardHeader>
              <CardTitle>Client & Program Information</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <Select
                label="Select Client"
                value={values.clientId}
                onChange={(value) => setValue('clientId', value)}
                options={[
                  { value: '', label: 'Choose a client...' },
                  ...clientOptions
                ]}
                error={errors.clientId}
                required
              />

              {selectedClient && (
                <div className="p-4 bg-secondary-50 border border-secondary-200 rounded-lg">
                  <div className="flex items-center justify-between mb-2">
                    <h4 className="font-medium text-secondary-900">
                      {selectedClient.name ? 
                        `${selectedClient.name.given?.join(' ') || ''} ${selectedClient.name.family || ''}`.trim() :
                        'Selected Client'
                      }
                    </h4>
                    <Link href={`/clients/${selectedClient.id}`} className="text-primary-600 hover:text-primary-700 text-sm">
                      View Profile
                    </Link>
                  </div>
                  <div className="grid grid-cols-2 gap-4 text-sm text-secondary-600">
                    <div>ID: {selectedClient.id.slice(0, 8)}</div>
                    <div>Status: {selectedClient.status}</div>
                  </div>
                </div>
              )}

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <Input
                  label="Enrollment ID"
                  value={values.enrollmentId}
                  onChange={(e) => setValue('enrollmentId', e.target.value)}
                  placeholder="e.g., ENR-2024-001"
                  error={errors.enrollmentId}
                  required
                />

                <Select
                  label="Program"
                  value={values.programId}
                  onChange={(value) => {
                    setValue('programId', value);
                    const program = PROGRAMS.find(p => p.value === value);
                    if (program) {
                      setValue('programName', program.label);
                    }
                  }}
                  options={[
                    { value: '', label: 'Select program...' },
                    ...PROGRAMS
                  ]}
                  error={errors.programId}
                  required
                />
              </div>
            </CardContent>
          </Card>

          {/* Service Details */}
          <Card>
            <CardHeader>
              <CardTitle>Service Details</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <Select
                  label="Service Type"
                  value={values.serviceType}
                  onChange={(value) => setValue('serviceType', value)}
                  options={[
                    { value: '', label: 'Select service type...' },
                    ...serviceTypeOptions
                  ]}
                  error={errors.serviceType}
                  required
                />

                <Select
                  label="Delivery Mode"
                  value={values.deliveryMode}
                  onChange={(value) => setValue('deliveryMode', value)}
                  options={[
                    { value: '', label: 'Select delivery mode...' },
                    ...deliveryModeOptions
                  ]}
                  error={errors.deliveryMode}
                  required
                />
              </div>

              {selectedServiceType && (
                <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg">
                  <h4 className="font-medium text-blue-900 mb-2">Service Type Information</h4>
                  <div className="grid grid-cols-2 gap-4 text-sm text-blue-800">
                    <div>Category: {selectedServiceType.category}</div>
                    <div>Billable: {selectedServiceType.isBillableService ? 'Yes' : 'No'}</div>
                    <div>Typical Duration: {selectedServiceType.typicalMinDuration}-{selectedServiceType.typicalMaxDuration} minutes</div>
                    <div>Confidential: {selectedServiceType.requiresConfidentialHandling ? 'Required' : 'Optional'}</div>
                  </div>
                </div>
              )}

              <Textarea
                label="Service Description"
                placeholder="Describe the specific service to be provided, goals, and approach..."
                value={values.description}
                onChange={(e) => setValue('description', e.target.value)}
                rows={4}
                error={errors.description}
                required
              />

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <Input
                  label="Primary Provider Name"
                  value={values.primaryProviderName}
                  onChange={(e) => setValue('primaryProviderName', e.target.value)}
                  error={errors.primaryProviderName}
                  required
                />

                <Input
                  label="Expected Duration (minutes)"
                  type="number"
                  value={values.expectedDurationMinutes?.toString() || ''}
                  onChange={(e) => setValue('expectedDurationMinutes', parseInt(e.target.value) || undefined)}
                  placeholder="e.g., 60"
                />
              </div>

              <div className="flex items-center space-x-2">
                <input
                  type="checkbox"
                  id="confidential"
                  checked={values.isConfidential}
                  onChange={(e) => setValue('isConfidential', e.target.checked)}
                  className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-secondary-300 rounded"
                />
                <label htmlFor="confidential" className="text-sm text-secondary-700">
                  This service requires confidential handling
                </label>
              </div>
            </CardContent>
          </Card>

          {/* Funding Sources */}
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle>Funding Sources</CardTitle>
                <Button type="button" variant="outline" onClick={addFundingSource}>
                  Add Funding Source
                </Button>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              {values.fundingSources.length === 0 ? (
                <p className="text-secondary-600 text-sm">No funding sources added. Service will use default program funding.</p>
              ) : (
                values.fundingSources.map((funding, index) => (
                  <div key={index} className="p-4 border border-secondary-200 rounded-lg space-y-3">
                    <div className="flex items-center justify-between">
                      <h4 className="font-medium text-secondary-900">Funding Source #{index + 1}</h4>
                      <Button 
                        type="button" 
                        variant="outline" 
                        onClick={() => removeFundingSource(index)}
                        className="text-red-600 hover:text-red-700"
                      >
                        Remove
                      </Button>
                    </div>
                    
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                      <Select
                        label="Funder"
                        value={funding.funderId}
                        onChange={(value) => {
                          updateFundingSource(index, 'funderId', value);
                          const funder = COMMON_FUNDERS.find(f => f.id === value);
                          if (funder) {
                            updateFundingSource(index, 'funderName', funder.name);
                          }
                        }}
                        options={[
                          { value: '', label: 'Select funder...' },
                          ...COMMON_FUNDERS.map(f => ({ value: f.id, label: f.name }))
                        ]}
                        required
                      />

                      <Input
                        label="Grant Number"
                        value={funding.grantNumber}
                        onChange={(e) => updateFundingSource(index, 'grantNumber', e.target.value)}
                        placeholder="e.g., VAWA-2024-001"
                      />

                      <Input
                        label="Program Name"
                        value={funding.programName}
                        onChange={(e) => updateFundingSource(index, 'programName', e.target.value)}
                        placeholder="e.g., Emergency Services"
                      />

                      <Input
                        label="Allocation %"
                        type="number"
                        min="0"
                        max="100"
                        value={funding.allocationPercentage.toString()}
                        onChange={(e) => updateFundingSource(index, 'allocationPercentage', parseFloat(e.target.value) || 0)}
                        required
                      />
                    </div>
                  </div>
                ))
              )}
              
              {values.fundingSources.length > 0 && (
                <div className="text-sm text-secondary-600">
                  Total Allocation: {values.fundingSources.reduce((sum, fs) => sum + fs.allocationPercentage, 0).toFixed(1)}%
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Quick Start Options */}
          <Card>
            <CardHeader>
              <CardTitle>Service Options</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center space-x-2">
                <input
                  type="checkbox"
                  id="start-immediately"
                  checked={values.startImmediately}
                  onChange={(e) => setValue('startImmediately', e.target.checked)}
                  className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-secondary-300 rounded"
                />
                <label htmlFor="start-immediately" className="text-sm text-secondary-700">
                  Start service immediately after creation
                </label>
              </div>

              {values.startImmediately && (
                <div className="p-3 bg-green-50 border border-green-200 rounded-lg text-sm text-green-800">
                  Service will begin immediately and you'll be redirected to the tracking interface.
                </div>
              )}
            </CardContent>
          </Card>

          {/* Service Guidelines */}
          {selectedServiceType && (
            <Card>
              <CardHeader>
                <CardTitle>Service Guidelines</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-3 text-sm text-secondary-600">
                  <div>
                    <strong>Category:</strong> {selectedServiceType.category}
                  </div>
                  <div>
                    <strong>Typical Duration:</strong> {selectedServiceType.typicalMinDuration}-{selectedServiceType.typicalMaxDuration} minutes
                  </div>
                  {selectedServiceType.requiresConfidentialHandling && (
                    <div className="p-2 bg-yellow-50 border border-yellow-200 rounded text-yellow-800">
                      <strong>⚠️ Confidential Service:</strong> Special handling required
                    </div>
                  )}
                  {selectedServiceType.isBillableService && (
                    <div className="p-2 bg-blue-50 border border-blue-200 rounded text-blue-800">
                      <strong>💰 Billable Service:</strong> Track time accurately for billing
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          )}

          {/* Quick Actions */}
          <Card>
            <CardHeader>
              <CardTitle>After Creation</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3 text-sm text-secondary-600">
                <div className="flex items-center space-x-2">
                  <svg className="w-4 h-4 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                  </svg>
                  <span>Start service delivery</span>
                </div>
                <div className="flex items-center space-x-2">
                  <svg className="w-4 h-4 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                  </svg>
                  <span>Track time and progress</span>
                </div>
                <div className="flex items-center space-x-2">
                  <svg className="w-4 h-4 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                  </svg>
                  <span>Document outcomes</span>
                </div>
                <div className="flex items-center space-x-2">
                  <svg className="w-4 h-4 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                  </svg>
                  <span>Generate billing records</span>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </form>
  );
}

export default function NewServicePage() {
  return (
    <ProtectedRoute>
      <AppLayout 
        title="Create Service Episode" 
        breadcrumbs={[
          { label: 'Dashboard', href: '/dashboard' },
          { label: 'Services', href: '/services' },
          { label: 'New Service' }
        ]}
      >
        <div className="p-6">
          <NewServiceContent />
        </div>
      </AppLayout>
    </ProtectedRoute>
  );
}
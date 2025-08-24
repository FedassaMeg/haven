import { useState } from 'react';
import { Card, CardHeader, CardTitle, CardContent, Button, Input, Select, Textarea } from '@haven/ui';
import { 
  useCreateQuickCrisisService,
  useCreateQuickCounselingService,
  useCreateQuickCaseManagementService,
  type QuickCrisisServiceRequest,
  type QuickCounselingServiceRequest,
  type QuickCaseManagementServiceRequest,
  type ServiceType,
  type ServiceDeliveryMode
} from '@haven/api-client';

interface QuickServiceCreatorProps {
  clientId: string;
  enrollmentId?: string;
  onSuccess?: (episodeId: string) => void;
  onCancel?: () => void;
}

type QuickServiceType = 'crisis' | 'counseling' | 'case-management';

const CRISIS_PROGRAMS = [
  { value: 'emergency-shelter', label: 'Emergency Shelter Program' },
  { value: 'crisis-intervention', label: 'Crisis Intervention Program' },
  { value: 'domestic-violence', label: 'Domestic Violence Program' },
  { value: 'hotline-support', label: '24/7 Hotline Support' },
];

const COUNSELING_TYPES: ServiceType[] = [
  'INDIVIDUAL_COUNSELING',
  'GROUP_COUNSELING',
  'TRAUMA_COUNSELING',
  'SUBSTANCE_ABUSE_COUNSELING',
  'FAMILY_COUNSELING',
  'COUPLES_COUNSELING',
];

const CASE_MGMT_PROGRAMS = [
  { value: 'case-management', label: 'General Case Management' },
  { value: 'transitional-housing', label: 'Transitional Housing Program' },
  { value: 'legal-advocacy', label: 'Legal Advocacy Program' },
  { value: 'financial-assistance', label: 'Financial Assistance Program' },
];

const DELIVERY_MODES: ServiceDeliveryMode[] = [
  'IN_PERSON',
  'PHONE',
  'VIDEO_CALL',
  'EMAIL',
  'TEXT_MESSAGING',
];

export default function QuickServiceCreator({ 
  clientId, 
  enrollmentId = '', 
  onSuccess, 
  onCancel 
}: QuickServiceCreatorProps) {
  const [serviceType, setServiceType] = useState<QuickServiceType>('crisis');
  const [loading, setLoading] = useState(false);

  const { createQuickCrisisService } = useCreateQuickCrisisService();
  const { createQuickCounselingService } = useCreateQuickCounselingService();
  const { createQuickCaseManagementService } = useCreateQuickCaseManagementService();

  // Crisis Service Form
  const [crisisForm, setCrisisForm] = useState<QuickCrisisServiceRequest>({
    clientId,
    enrollmentId,
    programId: '',
    providerId: '',
    providerName: '',
    isConfidential: true,
  });

  // Counseling Service Form
  const [counselingForm, setCounselingForm] = useState<QuickCounselingServiceRequest>({
    clientId,
    enrollmentId,
    programId: '',
    serviceType: '' as ServiceType,
    providerId: '',
    providerName: '',
  });

  // Case Management Form
  const [caseMgmtForm, setCaseMgmtForm] = useState<QuickCaseManagementServiceRequest>({
    clientId,
    enrollmentId,
    programId: '',
    deliveryMode: '' as ServiceDeliveryMode,
    providerId: '',
    providerName: '',
    description: '',
  });

  const handleSubmit = async () => {
    setLoading(true);
    try {
      let response;
      
      if (serviceType === 'crisis') {
        response = await createQuickCrisisService(crisisForm);
      } else if (serviceType === 'counseling') {
        response = await createQuickCounselingService(counselingForm);
      } else if (serviceType === 'case-management') {
        response = await createQuickCaseManagementService(caseMgmtForm);
      }

      if (response?.episodeId && onSuccess) {
        onSuccess(response.episodeId);
      }
    } catch (error) {
      console.error('Failed to create quick service:', error);
      alert('Failed to create service. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Quick Service Creation</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* Service Type Selection */}
        <div>
          <label className="block text-sm font-medium text-secondary-700 mb-2">Service Type</label>
          <div className="grid grid-cols-3 gap-2">
            <Button
              variant={serviceType === 'crisis' ? 'default' : 'outline'}
              onClick={() => setServiceType('crisis')}
              className="text-xs"
            >
              Crisis Response
            </Button>
            <Button
              variant={serviceType === 'counseling' ? 'default' : 'outline'}
              onClick={() => setServiceType('counseling')}
              className="text-xs"
            >
              Counseling
            </Button>
            <Button
              variant={serviceType === 'case-management' ? 'default' : 'outline'}
              onClick={() => setServiceType('case-management')}
              className="text-xs"
            >
              Case Management
            </Button>
          </div>
        </div>

        {/* Crisis Service Form */}
        {serviceType === 'crisis' && (
          <div className="space-y-3">
            <Input
              label="Enrollment ID"
              value={crisisForm.enrollmentId}
              onChange={(e) => setCrisisForm(prev => ({ ...prev, enrollmentId: e.target.value }))}
              placeholder="e.g., ENR-2024-001"
              required
            />

            <Select
              label="Crisis Program"
              value={crisisForm.programId}
              onChange={(value) => setCrisisForm(prev => ({ ...prev, programId: value }))}
              options={[
                { value: '', label: 'Select program...' },
                ...CRISIS_PROGRAMS
              ]}
              required
            />

            <Input
              label="Provider Name"
              value={crisisForm.providerName}
              onChange={(e) => setCrisisForm(prev => ({ ...prev, providerName: e.target.value }))}
              placeholder="Your name"
              required
            />

            <div className="flex items-center space-x-2">
              <input
                type="checkbox"
                id="crisis-confidential"
                checked={crisisForm.isConfidential}
                onChange={(e) => setCrisisForm(prev => ({ ...prev, isConfidential: e.target.checked }))}
                className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-secondary-300 rounded"
              />
              <label htmlFor="crisis-confidential" className="text-sm text-secondary-700">
                Confidential crisis service
              </label>
            </div>
          </div>
        )}

        {/* Counseling Service Form */}
        {serviceType === 'counseling' && (
          <div className="space-y-3">
            <Input
              label="Enrollment ID"
              value={counselingForm.enrollmentId}
              onChange={(e) => setCounselingForm(prev => ({ ...prev, enrollmentId: e.target.value }))}
              placeholder="e.g., ENR-2024-001"
              required
            />

            <Select
              label="Counseling Type"
              value={counselingForm.serviceType}
              onChange={(value) => setCounselingForm(prev => ({ ...prev, serviceType: value as ServiceType }))}
              options={[
                { value: '', label: 'Select counseling type...' },
                ...COUNSELING_TYPES.map(type => ({ 
                  value: type, 
                  label: type.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase()) 
                }))
              ]}
              required
            />

            <Select
              label="Program"
              value={counselingForm.programId}
              onChange={(value) => setCounselingForm(prev => ({ ...prev, programId: value }))}
              options={[
                { value: '', label: 'Select program...' },
                { value: 'counseling-services', label: 'Counseling Services Program' },
                { value: 'domestic-violence', label: 'Domestic Violence Program' },
                { value: 'substance-abuse', label: 'Substance Abuse Program' },
              ]}
              required
            />

            <Input
              label="Provider Name"
              value={counselingForm.providerName}
              onChange={(e) => setCounselingForm(prev => ({ ...prev, providerName: e.target.value }))}
              placeholder="Your name"
              required
            />
          </div>
        )}

        {/* Case Management Service Form */}
        {serviceType === 'case-management' && (
          <div className="space-y-3">
            <Input
              label="Enrollment ID"
              value={caseMgmtForm.enrollmentId}
              onChange={(e) => setCaseMgmtForm(prev => ({ ...prev, enrollmentId: e.target.value }))}
              placeholder="e.g., ENR-2024-001"
              required
            />

            <Select
              label="Delivery Mode"
              value={caseMgmtForm.deliveryMode}
              onChange={(value) => setCaseMgmtForm(prev => ({ ...prev, deliveryMode: value as ServiceDeliveryMode }))}
              options={[
                { value: '', label: 'Select delivery mode...' },
                ...DELIVERY_MODES.map(mode => ({ 
                  value: mode, 
                  label: mode.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase()) 
                }))
              ]}
              required
            />

            <Select
              label="Program"
              value={caseMgmtForm.programId}
              onChange={(value) => setCaseMgmtForm(prev => ({ ...prev, programId: value }))}
              options={[
                { value: '', label: 'Select program...' },
                ...CASE_MGMT_PROGRAMS
              ]}
              required
            />

            <Input
              label="Provider Name"
              value={caseMgmtForm.providerName}
              onChange={(e) => setCaseMgmtForm(prev => ({ ...prev, providerName: e.target.value }))}
              placeholder="Your name"
              required
            />

            <Textarea
              label="Service Description"
              value={caseMgmtForm.description || ''}
              onChange={(e) => setCaseMgmtForm(prev => ({ ...prev, description: e.target.value }))}
              placeholder="Brief description of case management activities..."
              rows={3}
            />
          </div>
        )}

        {/* Action Buttons */}
        <div className="flex items-center justify-end space-x-3 pt-4">
          {onCancel && (
            <Button variant="outline" onClick={onCancel}>
              Cancel
            </Button>
          )}
          <Button onClick={handleSubmit} loading={loading}>
            <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
            Create & Start Service
          </Button>
        </div>

        {/* Service Type Info */}
        <div className="p-3 bg-secondary-50 border border-secondary-200 rounded-lg text-sm">
          {serviceType === 'crisis' && (
            <div>
              <strong>Crisis Response:</strong> Immediate crisis intervention services with confidential handling. 
              Service will be automatically started upon creation.
            </div>
          )}
          {serviceType === 'counseling' && (
            <div>
              <strong>Counseling Services:</strong> Professional counseling sessions with duration tracking. 
              Choose the appropriate counseling type for accurate billing and reporting.
            </div>
          )}
          {serviceType === 'case-management' && (
            <div>
              <strong>Case Management:</strong> Ongoing case management support with flexible delivery modes. 
              Perfect for coordination and advocacy activities.
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
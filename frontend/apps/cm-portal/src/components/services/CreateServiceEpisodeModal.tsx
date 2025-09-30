import React, { useState, useEffect } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, Button, Input, Textarea, Select, SelectContent, SelectItem, SelectTrigger, SelectValue, Label, Checkbox, Card, CardContent } from '@haven/ui';
import {
  ServiceType,
  ServiceDeliveryMode,
  FundingSource,
  useServiceTypes,
  useDeliveryModes,
  useFundingSources,
  useClientEnrollments
} from '@haven/api-client';

interface CreateServiceEpisodeModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreate: (serviceData: {
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
  }) => void;
  loading?: boolean;
  clientId: string;
}

interface QuickServiceTemplate {
  name: string;
  serviceType: ServiceType;
  deliveryMode: ServiceDeliveryMode;
  plannedDurationMinutes: number;
  description: string;
  isConfidential: boolean;
}

const quickTemplates: QuickServiceTemplate[] = [
  {
    name: 'Crisis Intervention',
    serviceType: 'CRISIS_INTERVENTION' as ServiceType,
    deliveryMode: 'IN_PERSON' as ServiceDeliveryMode,
    plannedDurationMinutes: 60,
    description: 'Crisis intervention and safety planning',
    isConfidential: true,
  },
  {
    name: 'Individual Counseling',
    serviceType: 'INDIVIDUAL_COUNSELING' as ServiceType,
    deliveryMode: 'IN_PERSON' as ServiceDeliveryMode,
    plannedDurationMinutes: 50,
    description: 'Individual therapeutic counseling session',
    isConfidential: true,
  },
  {
    name: 'Case Management',
    serviceType: 'CASE_MANAGEMENT' as ServiceDeliveryMode,
    deliveryMode: 'IN_PERSON' as ServiceDeliveryMode,
    plannedDurationMinutes: 30,
    description: 'Case management consultation and planning',
    isConfidential: false,
  },
  {
    name: 'Legal Advocacy',
    serviceType: 'LEGAL_ADVOCACY' as ServiceType,
    deliveryMode: 'IN_PERSON' as ServiceDeliveryMode,
    plannedDurationMinutes: 45,
    description: 'Legal advocacy and consultation',
    isConfidential: true,
  },
  {
    name: 'Support Group',
    serviceType: 'GROUP_COUNSELING' as ServiceType,
    deliveryMode: 'IN_PERSON' as ServiceDeliveryMode,
    plannedDurationMinutes: 90,
    description: 'Group support session',
    isConfidential: false,
  },
];

export default function CreateServiceEpisodeModal({
  isOpen,
  onClose,
  onCreate,
  loading = false,
  clientId,
}: CreateServiceEpisodeModalProps) {
  // Form state
  const [enrollmentId, setEnrollmentId] = useState('');
  const [programId, setProgramId] = useState('');
  const [programName, setProgramName] = useState('');
  const [serviceType, setServiceType] = useState<ServiceType | ''>('');
  const [deliveryMode, setDeliveryMode] = useState<ServiceDeliveryMode | ''>('');
  const [serviceDate, setServiceDate] = useState(new Date().toISOString().split('T')[0]);
  const [plannedDurationMinutes, setPlannedDurationMinutes] = useState(30);
  const [primaryProviderName, setPrimaryProviderName] = useState('');
  const [selectedFunding, setSelectedFunding] = useState<FundingSource | null>(null);
  const [serviceDescription, setServiceDescription] = useState('');
  const [isConfidential, setIsConfidential] = useState(false);

  // API hooks
  const { data: serviceTypes = [] } = useServiceTypes();
  const { data: deliveryModes = [] } = useDeliveryModes();
  const { data: fundingSources = [] } = useFundingSources();
  const { data: enrollments = [] } = useClientEnrollments(clientId);

  // Reset form when modal opens/closes
  useEffect(() => {
    if (!isOpen) {
      setEnrollmentId('');
      setProgramId('');
      setProgramName('');
      setServiceType('');
      setDeliveryMode('');
      setServiceDate(new Date().toISOString().split('T')[0]);
      setPlannedDurationMinutes(30);
      setPrimaryProviderName('');
      setSelectedFunding(null);
      setServiceDescription('');
      setIsConfidential(false);
    }
  }, [isOpen]);

  // Update program info when enrollment changes
  useEffect(() => {
    if (enrollmentId) {
      const enrollment = enrollments.find(e => e.id === enrollmentId);
      if (enrollment) {
        setProgramId(enrollment.programId);
        setProgramName(enrollment.programName);
      }
    }
  }, [enrollmentId, enrollments]);

  const handleQuickTemplate = (template: QuickServiceTemplate) => {
    setServiceType(template.serviceType);
    setDeliveryMode(template.deliveryMode);
    setPlannedDurationMinutes(template.plannedDurationMinutes);
    setServiceDescription(template.description);
    setIsConfidential(template.isConfidential);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!enrollmentId || !serviceType || !deliveryMode || !selectedFunding || !primaryProviderName) {
      return;
    }

    onCreate({
      enrollmentId,
      programId,
      programName,
      serviceType: serviceType as ServiceType,
      deliveryMode: deliveryMode as ServiceDeliveryMode,
      serviceDate,
      plannedDurationMinutes,
      primaryProviderName,
      fundingSource: selectedFunding,
      serviceDescription,
      isConfidential,
    });
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Create Service Episode</DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Quick Templates */}
          <Card>
            <CardContent className="p-4">
              <Label className="text-sm font-medium mb-3 block">Quick Service Templates</Label>
              <div className="grid grid-cols-2 md:grid-cols-3 gap-2">
                {quickTemplates.map((template) => (
                  <Button
                    key={template.name}
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => handleQuickTemplate(template)}
                    className="text-left h-auto p-3"
                  >
                    <div>
                      <div className="font-medium text-sm">{template.name}</div>
                      <div className="text-xs text-slate-600">{template.plannedDurationMinutes}min</div>
                    </div>
                  </Button>
                ))}
              </div>
            </CardContent>
          </Card>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Left Column - Basic Information */}
            <div className="space-y-4">
              <div>
                <Label htmlFor="enrollment">Program Enrollment *</Label>
                <Select value={enrollmentId} onValueChange={setEnrollmentId} required>
                  <SelectTrigger>
                    <SelectValue placeholder="Select enrollment" />
                  </SelectTrigger>
                  <SelectContent>
                    {enrollments.map((enrollment) => (
                      <SelectItem key={enrollment.id} value={enrollment.id}>
                        {enrollment.programName} - {enrollment.status}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div>
                <Label htmlFor="serviceType">Service Type *</Label>
                <Select value={serviceType} onValueChange={setServiceType} required>
                  <SelectTrigger>
                    <SelectValue placeholder="Select service type" />
                  </SelectTrigger>
                  <SelectContent>
                    {serviceTypes.map((type) => (
                      <SelectItem key={type.value} value={type.value}>
                        {type.label} - {type.category}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div>
                <Label htmlFor="deliveryMode">Delivery Mode *</Label>
                <Select value={deliveryMode} onValueChange={setDeliveryMode} required>
                  <SelectTrigger>
                    <SelectValue placeholder="Select delivery mode" />
                  </SelectTrigger>
                  <SelectContent>
                    {deliveryModes.map((mode) => (
                      <SelectItem key={mode.value} value={mode.value}>
                        {mode.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div>
                <Label htmlFor="serviceDate">Service Date *</Label>
                <Input
                  id="serviceDate"
                  type="date"
                  value={serviceDate}
                  onChange={(e) => setServiceDate(e.target.value)}
                  required
                />
              </div>

              <div>
                <Label htmlFor="duration">Planned Duration (minutes) *</Label>
                <Input
                  id="duration"
                  type="number"
                  min="5"
                  max="480"
                  step="5"
                  value={plannedDurationMinutes}
                  onChange={(e) => setPlannedDurationMinutes(parseInt(e.target.value) || 30)}
                  required
                />
              </div>
            </div>

            {/* Right Column - Provider and Funding */}
            <div className="space-y-4">
              <div>
                <Label htmlFor="provider">Primary Provider *</Label>
                <Input
                  id="provider"
                  value={primaryProviderName}
                  onChange={(e) => setPrimaryProviderName(e.target.value)}
                  placeholder="Provider name"
                  required
                />
              </div>

              <div>
                <Label htmlFor="funding">Funding Source *</Label>
                <Select
                  value={selectedFunding?.funderId || ''}
                  onValueChange={(value) => {
                    const funding = fundingSources.find(f => f.funderId === value);
                    setSelectedFunding(funding || null);
                  }}
                  required
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select funding source" />
                  </SelectTrigger>
                  <SelectContent>
                    {fundingSources.map((funding) => (
                      <SelectItem key={funding.funderId} value={funding.funderId}>
                        <div>
                          <div className="font-medium">{funding.funderName}</div>
                          <div className="text-sm text-slate-600">{funding.fundingType}</div>
                        </div>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {selectedFunding && (
                <Card className="p-3 bg-blue-50">
                  <div className="text-sm">
                    <div className="font-medium">{selectedFunding.funderName}</div>
                    <div className="text-slate-600">
                      Type: {selectedFunding.fundingType}
                    </div>
                    {selectedFunding.grantNumber && (
                      <div className="text-slate-600">
                        Grant: {selectedFunding.grantNumber}
                      </div>
                    )}
                    <div className="text-slate-600">
                      Requires Outcome Tracking: {selectedFunding.requiresOutcomeTracking ? 'Yes' : 'No'}
                    </div>
                    <div className="text-slate-600">
                      Allows Confidential Services: {selectedFunding.allowsConfidentialServices ? 'Yes' : 'No'}
                    </div>
                  </div>
                </Card>
              )}

              <div className="flex items-center space-x-2">
                <Checkbox
                  id="confidential"
                  checked={isConfidential}
                  onCheckedChange={setIsConfidential}
                />
                <Label htmlFor="confidential" className="text-sm">
                  Confidential Service
                </Label>
              </div>

              {isConfidential && selectedFunding && !selectedFunding.allowsConfidentialServices && (
                <div className="text-sm text-amber-700 bg-amber-50 border border-amber-200 rounded p-2">
                  ⚠️ Warning: Selected funding source does not allow confidential services
                </div>
              )}
            </div>
          </div>

          {/* Service Description */}
          <div>
            <Label htmlFor="description">Service Description</Label>
            <Textarea
              id="description"
              value={serviceDescription}
              onChange={(e) => setServiceDescription(e.target.value)}
              placeholder="Describe the service to be provided"
              rows={3}
            />
          </div>

          {/* Compliance Warnings */}
          {selectedFunding?.requiresOutcomeTracking && (
            <div className="text-sm text-blue-700 bg-blue-50 border border-blue-200 rounded p-3">
              📊 This funding source requires outcome tracking. Please ensure you document outcomes after service completion.
            </div>
          )}

          {/* Action Buttons */}
          <div className="flex justify-end space-x-2 pt-4 border-t">
            <Button type="button" variant="outline" onClick={onClose}>
              Cancel
            </Button>
            <Button
              type="submit"
              disabled={loading || !enrollmentId || !serviceType || !deliveryMode || !selectedFunding || !primaryProviderName}
            >
              {loading ? 'Creating...' : 'Create Service Episode'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
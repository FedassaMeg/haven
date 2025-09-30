import React, { useState, useEffect } from 'react';
import { Card, CardHeader, CardTitle, CardContent, Button, Input, Textarea, Select, SelectContent, SelectItem, SelectTrigger, SelectValue, Label, Checkbox } from '@haven/ui';
import { ServiceType, ServiceDeliveryMode } from '@haven/api-client';

interface OfflineServiceData {
  id: string;
  clientId: string;
  enrollmentId: string;
  serviceType: ServiceType;
  deliveryMode: ServiceDeliveryMode;
  serviceDate: string;
  startTime: string;
  endTime: string;
  primaryProviderName: string;
  serviceDescription: string;
  serviceOutcome: string;
  notes: string;
  isConfidential: boolean;
  location: string;
  createdOffline: boolean;
  timestamp: string;
}

interface OfflineServiceFormProps {
  clientId: string;
  onSave: (service: OfflineServiceData) => void;
  onCancel: () => void;
  initialData?: Partial<OfflineServiceData>;
}

export default function OfflineServiceForm({ clientId, onSave, onCancel, initialData }: OfflineServiceFormProps) {
  const [formData, setFormData] = useState<Partial<OfflineServiceData>>({
    clientId,
    serviceDate: new Date().toISOString().split('T')[0],
    startTime: new Date().toTimeString().slice(0, 5),
    isConfidential: false,
    createdOffline: true,
    ...initialData
  });

  const [isOnline, setIsOnline] = useState(navigator.onLine);

  useEffect(() => {
    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    const serviceData: OfflineServiceData = {
      id: formData.id || `offline-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
      clientId,
      enrollmentId: formData.enrollmentId || '',
      serviceType: formData.serviceType || 'CASE_MANAGEMENT',
      deliveryMode: formData.deliveryMode || 'IN_PERSON',
      serviceDate: formData.serviceDate || '',
      startTime: formData.startTime || '',
      endTime: formData.endTime || '',
      primaryProviderName: formData.primaryProviderName || '',
      serviceDescription: formData.serviceDescription || '',
      serviceOutcome: formData.serviceOutcome || '',
      notes: formData.notes || '',
      isConfidential: formData.isConfidential || false,
      location: formData.location || '',
      createdOffline: true,
      timestamp: new Date().toISOString()
    };

    onSave(serviceData);
  };

  const updateField = (field: keyof OfflineServiceData, value: any) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle>
            {isOnline ? 'Service Documentation' : 'Offline Service Documentation'}
          </CardTitle>
          {!isOnline && (
            <div className="flex items-center space-x-2 text-amber-600">
              <span className="w-2 h-2 bg-amber-500 rounded-full"></span>
              <span className="text-sm">Offline Mode</span>
            </div>
          )}
        </div>
      </CardHeader>
      <CardContent>
        {!isOnline && (
          <div className="mb-4 p-3 bg-amber-50 border border-amber-200 rounded-lg">
            <div className="text-sm text-amber-800">
              📱 You are currently offline. This service will be saved locally and synced when connection is restored.
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <Label htmlFor="serviceType">Service Type *</Label>
              <Select
                value={formData.serviceType || ''}
                onValueChange={(value: ServiceType) => updateField('serviceType', value)}
                required
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select service type" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="CASE_MANAGEMENT">Case Management</SelectItem>
                  <SelectItem value="INDIVIDUAL_COUNSELING">Individual Counseling</SelectItem>
                  <SelectItem value="GROUP_COUNSELING">Group Counseling</SelectItem>
                  <SelectItem value="CRISIS_INTERVENTION">Crisis Intervention</SelectItem>
                  <SelectItem value="LEGAL_ADVOCACY">Legal Advocacy</SelectItem>
                  <SelectItem value="MEDICAL_ADVOCACY">Medical Advocacy</SelectItem>
                  <SelectItem value="HOUSING_ASSISTANCE">Housing Assistance</SelectItem>
                  <SelectItem value="FINANCIAL_ASSISTANCE">Financial Assistance</SelectItem>
                  <SelectItem value="TRANSPORTATION">Transportation</SelectItem>
                  <SelectItem value="CHILDCARE">Childcare</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div>
              <Label htmlFor="deliveryMode">Delivery Mode *</Label>
              <Select
                value={formData.deliveryMode || ''}
                onValueChange={(value: ServiceDeliveryMode) => updateField('deliveryMode', value)}
                required
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select delivery mode" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="IN_PERSON">In Person</SelectItem>
                  <SelectItem value="PHONE">Phone</SelectItem>
                  <SelectItem value="VIDEO_CALL">Video Call</SelectItem>
                  <SelectItem value="EMAIL">Email</SelectItem>
                  <SelectItem value="TEXT_MESSAGE">Text Message</SelectItem>
                  <SelectItem value="HOME_VISIT">Home Visit</SelectItem>
                  <SelectItem value="COMMUNITY_OUTREACH">Community Outreach</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div>
              <Label htmlFor="serviceDate">Service Date *</Label>
              <Input
                id="serviceDate"
                type="date"
                value={formData.serviceDate || ''}
                onChange={(e) => updateField('serviceDate', e.target.value)}
                required
              />
            </div>

            <div>
              <Label htmlFor="location">Location</Label>
              <Input
                id="location"
                value={formData.location || ''}
                onChange={(e) => updateField('location', e.target.value)}
                placeholder="Service location"
              />
            </div>

            <div>
              <Label htmlFor="startTime">Start Time</Label>
              <Input
                id="startTime"
                type="time"
                value={formData.startTime || ''}
                onChange={(e) => updateField('startTime', e.target.value)}
              />
            </div>

            <div>
              <Label htmlFor="endTime">End Time</Label>
              <Input
                id="endTime"
                type="time"
                value={formData.endTime || ''}
                onChange={(e) => updateField('endTime', e.target.value)}
              />
            </div>
          </div>

          <div>
            <Label htmlFor="primaryProviderName">Provider Name *</Label>
            <Input
              id="primaryProviderName"
              value={formData.primaryProviderName || ''}
              onChange={(e) => updateField('primaryProviderName', e.target.value)}
              placeholder="Name of service provider"
              required
            />
          </div>

          <div>
            <Label htmlFor="serviceDescription">Service Description</Label>
            <Textarea
              id="serviceDescription"
              value={formData.serviceDescription || ''}
              onChange={(e) => updateField('serviceDescription', e.target.value)}
              placeholder="Describe the service provided"
              rows={3}
            />
          </div>

          <div>
            <Label htmlFor="serviceOutcome">Service Outcome</Label>
            <Textarea
              id="serviceOutcome"
              value={formData.serviceOutcome || ''}
              onChange={(e) => updateField('serviceOutcome', e.target.value)}
              placeholder="Describe the outcome and client progress"
              rows={3}
            />
          </div>

          <div>
            <Label htmlFor="notes">Additional Notes</Label>
            <Textarea
              id="notes"
              value={formData.notes || ''}
              onChange={(e) => updateField('notes', e.target.value)}
              placeholder="Any additional notes or observations"
              rows={2}
            />
          </div>

          <div className="flex items-center space-x-2">
            <Checkbox
              id="isConfidential"
              checked={formData.isConfidential || false}
              onCheckedChange={(checked) => updateField('isConfidential', checked)}
            />
            <Label htmlFor="isConfidential" className="text-sm">
              Confidential Service
            </Label>
          </div>

          <div className="flex justify-end space-x-2 pt-4 border-t">
            <Button type="button" variant="outline" onClick={onCancel}>
              Cancel
            </Button>
            <Button type="submit">
              {isOnline ? 'Save Service' : 'Save Offline'}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}
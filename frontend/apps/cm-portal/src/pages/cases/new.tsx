import { useState, useEffect } from 'react';
import { useRouter } from 'next/router';
import Link from 'next/link';
import { ProtectedRoute, useCurrentUser } from '@haven/auth';
import { Card, CardHeader, CardTitle, CardContent, Button, Input, Select, Textarea, RadioGroup, RadioGroupItem } from '@haven/ui';
import { useClients, useCreateCase, type Client } from '@haven/api-client';
import AppLayout from '../../components/AppLayout';

interface CaseFormData {
  clientId: string;
  description: string;
  riskLevel: 'LOW' | 'MODERATE' | 'HIGH' | 'CRITICAL';
  urgency: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  presentingIssues: string[];
  immediateNeeds: string[];
  safetyPlan: {
    hasActiveSafetyPlan: boolean;
    needsImmediateSafety: boolean;
    currentLocation: 'SAFE' | 'UNSAFE' | 'UNKNOWN';
    notes: string;
  };
  assigneeId?: string;
  referralSource: string;
  confidentialityLevel: 'STANDARD' | 'CONFIDENTIAL' | 'RESTRICTED';
}

const PRESENTING_ISSUES = [
  'Physical Violence',
  'Emotional Abuse',
  'Financial Abuse',
  'Sexual Violence',
  'Stalking/Harassment',
  'Threats',
  'Housing Instability',
  'Legal Issues',
  'Child Safety Concerns',
  'Medical Needs',
  'Mental Health Support',
  'Substance Use',
];

const IMMEDIATE_NEEDS = [
  'Emergency Shelter',
  'Safety Planning',
  'Medical Attention',
  'Legal Protection Order',
  'Financial Assistance',
  'Transportation',
  'Child Care',
  'Food/Basic Needs',
  'Counseling Services',
  'Housing Search',
  'Legal Advocacy',
  'Crisis Intervention',
];

const REFERRAL_SOURCES = [
  'Self-referral',
  'Law Enforcement',
  'Hospital/Medical',
  'Legal Services',
  'Social Services',
  'School/Education',
  'Friend/Family',
  'Community Organization',
  'Hotline',
  'Court',
  'Other Agency',
  'Previous Client',
];

function NewCaseContent() {
  const router = useRouter();
  const { user } = useCurrentUser();
  const { clients } = useClients({ activeOnly: true });
  const { createCase, loading } = useCreateCase();
  
  const [formData, setFormData] = useState<CaseFormData>({
    clientId: '',
    description: '',
    riskLevel: 'MODERATE',
    urgency: 'MEDIUM',
    presentingIssues: [],
    immediateNeeds: [],
    safetyPlan: {
      hasActiveSafetyPlan: false,
      needsImmediateSafety: false,
      currentLocation: 'UNKNOWN',
      notes: '',
    },
    referralSource: '',
    confidentialityLevel: 'STANDARD',
  });

  const [selectedClient, setSelectedClient] = useState<Client | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});

  // Pre-populate client if passed via router state
  useEffect(() => {
    if (router.query.clientId) {
      setFormData(prev => ({ ...prev, clientId: router.query.clientId as string }));
    }
  }, [router.query]);

  useEffect(() => {
    if (formData.clientId && clients) {
      const client = clients.find(c => c.id === formData.clientId);
      setSelectedClient(client || null);
    } else {
      setSelectedClient(null);
    }
  }, [formData.clientId, clients]);

  const handleInputChange = (field: keyof CaseFormData, value: any) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: '' }));
    }
  };

  const handleSafetyPlanChange = (field: keyof CaseFormData['safetyPlan'], value: any) => {
    setFormData(prev => ({
      ...prev,
      safetyPlan: { ...prev.safetyPlan, [field]: value }
    }));
  };

  const handleArrayToggle = (field: 'presentingIssues' | 'immediateNeeds', value: string) => {
    setFormData(prev => ({
      ...prev,
      [field]: prev[field].includes(value)
        ? prev[field].filter(item => item !== value)
        : [...prev[field], value]
    }));
  };

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.clientId) newErrors.clientId = 'Client selection is required';
    if (!formData.description.trim()) newErrors.description = 'Case description is required';
    if (!formData.referralSource) newErrors.referralSource = 'Referral source is required';
    if (formData.presentingIssues.length === 0) newErrors.presentingIssues = 'At least one presenting issue must be selected';

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) return;

    try {
      const caseData = {
        clientId: formData.clientId,
        description: formData.description,
        riskLevel: formData.riskLevel,
        urgency: formData.urgency,
        presentingIssues: formData.presentingIssues,
        immediateNeeds: formData.immediateNeeds,
        safetyPlan: formData.safetyPlan,
        assigneeId: formData.assigneeId || user?.id,
        referralSource: formData.referralSource,
        confidentialityLevel: formData.confidentialityLevel,
        status: 'OPEN',
      };

      const newCase = await createCase(caseData);
      router.push(`/cases/${newCase.id}`);
    } catch (error) {
      console.error('Failed to create case:', error);
      setErrors({ submit: 'Failed to create case. Please try again.' });
    }
  };

  const clientOptions = clients?.map(client => ({
    value: client.id,
    label: client.name ? 
      `${client.name.given?.join(' ') || ''} ${client.name.family || ''}`.trim() :
      `Client ${client.id.slice(0, 8)}`
  })) || [];

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-secondary-900">Open New Case</h1>
          <p className="text-secondary-600">Create a new case for client intake and service provision</p>
        </div>
        <div className="flex items-center space-x-3">
          <Link href="/cases">
            <Button variant="outline">Cancel</Button>
          </Link>
          <Button type="submit" loading={loading}>
            <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
            </svg>
            Open Case
          </Button>
        </div>
      </div>

      {errors.submit && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <p className="text-red-700">{errors.submit}</p>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Form */}
        <div className="lg:col-span-2 space-y-6">
          {/* Client Selection */}
          <Card>
            <CardHeader>
              <CardTitle>Client Information</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <Select
                label="Select Client"
                value={formData.clientId}
                onChange={(value) => handleInputChange('clientId', value)}
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
                  <div className="grid grid-cols-2 gap-4 text-sm">
                    <div>
                      <span className="text-secondary-600">ID:</span> {selectedClient.id.slice(0, 8)}
                    </div>
                    <div>
                      <span className="text-secondary-600">Status:</span> {selectedClient.status}
                    </div>
                    {selectedClient.birthDate && (
                      <div>
                        <span className="text-secondary-600">DOB:</span> {new Date(selectedClient.birthDate).toLocaleDateString()}
                      </div>
                    )}
                    {selectedClient.gender && (
                      <div>
                        <span className="text-secondary-600">Gender:</span> {selectedClient.gender}
                      </div>
                    )}
                  </div>
                </div>
              )}

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <Select
                  label="Referral Source"
                  value={formData.referralSource}
                  onChange={(value) => handleInputChange('referralSource', value)}
                  options={[
                    { value: '', label: 'Select source...' },
                    ...REFERRAL_SOURCES.map(source => ({ value: source, label: source }))
                  ]}
                  error={errors.referralSource}
                  required
                />

                <Select
                  label="Confidentiality Level"
                  value={formData.confidentialityLevel}
                  onChange={(value) => handleInputChange('confidentialityLevel', value)}
                  options={[
                    { value: 'STANDARD', label: 'Standard' },
                    { value: 'CONFIDENTIAL', label: 'Confidential' },
                    { value: 'RESTRICTED', label: 'Restricted Access' },
                  ]}
                />
              </div>
            </CardContent>
          </Card>

          {/* Case Details */}
          <Card>
            <CardHeader>
              <CardTitle>Case Details</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <Textarea
                label="Case Description"
                placeholder="Describe the presenting situation, client needs, and initial assessment..."
                value={formData.description}
                onChange={(e) => handleInputChange('description', e.target.value)}
                rows={4}
                error={errors.description}
                required
              />

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-secondary-700 mb-2">
                    Risk Level *
                  </label>
                  <RadioGroup
                    value={formData.riskLevel}
                    onValueChange={(value) => handleInputChange('riskLevel', value)}
                  >
                    <div className="space-y-2">
                      <div className="flex items-center space-x-2">
                        <RadioGroupItem value="LOW" id="risk-low" />
                        <label htmlFor="risk-low" className="text-sm text-green-700">Low Risk</label>
                      </div>
                      <div className="flex items-center space-x-2">
                        <RadioGroupItem value="MODERATE" id="risk-moderate" />
                        <label htmlFor="risk-moderate" className="text-sm text-yellow-700">Moderate Risk</label>
                      </div>
                      <div className="flex items-center space-x-2">
                        <RadioGroupItem value="HIGH" id="risk-high" />
                        <label htmlFor="risk-high" className="text-sm text-red-700">High Risk</label>
                      </div>
                      <div className="flex items-center space-x-2">
                        <RadioGroupItem value="CRITICAL" id="risk-critical" />
                        <label htmlFor="risk-critical" className="text-sm text-red-900">Critical Risk</label>
                      </div>
                    </div>
                  </RadioGroup>
                </div>

                <div>
                  <label className="block text-sm font-medium text-secondary-700 mb-2">
                    Urgency Level
                  </label>
                  <RadioGroup
                    value={formData.urgency}
                    onValueChange={(value) => handleInputChange('urgency', value)}
                  >
                    <div className="space-y-2">
                      <div className="flex items-center space-x-2">
                        <RadioGroupItem value="LOW" id="urgency-low" />
                        <label htmlFor="urgency-low" className="text-sm">Low</label>
                      </div>
                      <div className="flex items-center space-x-2">
                        <RadioGroupItem value="MEDIUM" id="urgency-medium" />
                        <label htmlFor="urgency-medium" className="text-sm">Medium</label>
                      </div>
                      <div className="flex items-center space-x-2">
                        <RadioGroupItem value="HIGH" id="urgency-high" />
                        <label htmlFor="urgency-high" className="text-sm">High</label>
                      </div>
                      <div className="flex items-center space-x-2">
                        <RadioGroupItem value="CRITICAL" id="urgency-critical" />
                        <label htmlFor="urgency-critical" className="text-sm">Critical</label>
                      </div>
                    </div>
                  </RadioGroup>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Presenting Issues */}
          <Card>
            <CardHeader>
              <CardTitle>Presenting Issues</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-secondary-600 mb-4">Select all issues that apply to this case:</p>
              {errors.presentingIssues && (
                <p className="text-red-600 text-sm mb-4">{errors.presentingIssues}</p>
              )}
              <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
                {PRESENTING_ISSUES.map((issue) => (
                  <label key={issue} className="flex items-center space-x-2 p-3 border border-secondary-200 rounded-lg hover:bg-secondary-50 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={formData.presentingIssues.includes(issue)}
                      onChange={() => handleArrayToggle('presentingIssues', issue)}
                      className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-secondary-300 rounded"
                    />
                    <span className="text-sm text-secondary-900">{issue}</span>
                  </label>
                ))}
              </div>
            </CardContent>
          </Card>

          {/* Immediate Needs */}
          <Card>
            <CardHeader>
              <CardTitle>Immediate Needs</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-secondary-600 mb-4">Select immediate needs requiring attention:</p>
              <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
                {IMMEDIATE_NEEDS.map((need) => (
                  <label key={need} className="flex items-center space-x-2 p-3 border border-secondary-200 rounded-lg hover:bg-secondary-50 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={formData.immediateNeeds.includes(need)}
                      onChange={() => handleArrayToggle('immediateNeeds', need)}
                      className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-secondary-300 rounded"
                    />
                    <span className="text-sm text-secondary-900">{need}</span>
                  </label>
                ))}
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Safety Assessment */}
          <Card>
            <CardHeader>
              <CardTitle>Safety Assessment</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-secondary-700 mb-2">
                  Current Safety Status
                </label>
                <RadioGroup
                  value={formData.safetyPlan.currentLocation}
                  onValueChange={(value) => handleSafetyPlanChange('currentLocation', value)}
                >
                  <div className="space-y-2">
                    <div className="flex items-center space-x-2">
                      <RadioGroupItem value="SAFE" id="location-safe" />
                      <label htmlFor="location-safe" className="text-sm text-green-700">Safe Location</label>
                    </div>
                    <div className="flex items-center space-x-2">
                      <RadioGroupItem value="UNSAFE" id="location-unsafe" />
                      <label htmlFor="location-unsafe" className="text-sm text-red-700">Unsafe Location</label>
                    </div>
                    <div className="flex items-center space-x-2">
                      <RadioGroupItem value="UNKNOWN" id="location-unknown" />
                      <label htmlFor="location-unknown" className="text-sm text-secondary-700">Unknown</label>
                    </div>
                  </div>
                </RadioGroup>
              </div>

              <div className="space-y-3">
                <label className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    checked={formData.safetyPlan.hasActiveSafetyPlan}
                    onChange={(e) => handleSafetyPlanChange('hasActiveSafetyPlan', e.target.checked)}
                    className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-secondary-300 rounded"
                  />
                  <span className="text-sm text-secondary-700">Has existing safety plan</span>
                </label>

                <label className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    checked={formData.safetyPlan.needsImmediateSafety}
                    onChange={(e) => handleSafetyPlanChange('needsImmediateSafety', e.target.checked)}
                    className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-secondary-300 rounded"
                  />
                  <span className="text-sm text-secondary-700">Needs immediate safety intervention</span>
                </label>
              </div>

              <Textarea
                label="Safety Notes"
                placeholder="Additional safety concerns or notes..."
                value={formData.safetyPlan.notes}
                onChange={(e) => handleSafetyPlanChange('notes', e.target.value)}
                rows={3}
              />
            </CardContent>
          </Card>

          {/* Quick Actions */}
          <Card>
            <CardHeader>
              <CardTitle>After Case Creation</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3 text-sm text-secondary-600">
                <div className="flex items-center space-x-2">
                  <svg className="w-4 h-4 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                  </svg>
                  <span>Complete initial safety planning</span>
                </div>
                <div className="flex items-center space-x-2">
                  <svg className="w-4 h-4 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                  </svg>
                  <span>Schedule follow-up appointment</span>
                </div>
                <div className="flex items-center space-x-2">
                  <svg className="w-4 h-4 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                  </svg>
                  <span>Coordinate with service providers</span>
                </div>
                <div className="flex items-center space-x-2">
                  <svg className="w-4 h-4 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                  </svg>
                  <span>Document initial assessment</span>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Help */}
          <Card>
            <CardHeader>
              <CardTitle>Need Help?</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <Button variant="outline" className="w-full justify-start text-sm">
                  <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  Case Creation Guide
                </Button>
                <Button variant="outline" className="w-full justify-start text-sm">
                  <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
                  </svg>
                  Contact Supervisor
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </form>
  );
}

export default function NewCasePage() {
  return (
    <ProtectedRoute>
      <AppLayout 
        title="Open New Case" 
        breadcrumbs={[
          { label: 'Dashboard', href: '/dashboard' },
          { label: 'Cases', href: '/cases' },
          { label: 'New Case' }
        ]}
      >
        <div className="p-6">
          <NewCaseContent />
        </div>
      </AppLayout>
    </ProtectedRoute>
  );
}
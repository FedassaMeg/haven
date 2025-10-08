import { useState, useEffect } from 'react';
import { useRouter } from 'next/router';
import Link from 'next/link';
import { ProtectedRoute, useCurrentUser } from '@haven/auth';
import { Card, CardHeader, CardTitle, CardContent, Button, Input, Select, Textarea, RadioGroup, RadioGroupItem } from '@haven/ui';
import { useCases, useCreateMandatedReport, type Case } from '@haven/api-client';
import AppLayout from '../../components/AppLayout';

interface MandatedReportFormData {
  caseId: string;
  reportType: 'CPS' | 'APS' | 'LAW_ENFORCEMENT' | 'MEDICAL' | 'COURT';
  urgency: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  incidentDate?: string;
  reportingReason: string;
  incidentDescription: string;
  injuriesOrHarm: string;
  witnessInformation: string;
  actionsTaken: string;
  recommendations: string;
  followUpRequired: boolean;
  followUpDetails: string;
  isConfidential: boolean;
  mandatedReporter: {
    name: string;
    title: string;
    organization: string;
    phone: string;
    email: string;
  };
  receivingAgency: {
    name: string;
    contactPerson: string;
    phone: string;
    address: string;
  };
}

const REPORT_TYPES = [
  {
    value: 'CPS',
    label: 'Child Protective Services',
    description: 'Report suspected child abuse or neglect',
    icon: '👶',
    color: 'text-red-700 bg-red-50 border-red-200',
  },
  {
    value: 'APS',
    label: 'Adult Protective Services',
    description: 'Report suspected adult abuse, neglect, or exploitation',
    icon: '👥',
    color: 'text-blue-700 bg-blue-50 border-blue-200',
  },
  {
    value: 'LAW_ENFORCEMENT',
    label: 'Law Enforcement',
    description: 'Report criminal activity or immediate safety concerns',
    icon: '👮',
    color: 'text-purple-700 bg-purple-50 border-purple-200',
  },
  {
    value: 'MEDICAL',
    label: 'Medical Report',
    description: 'Document medical evidence of abuse or injury',
    icon: '🏥',
    color: 'text-green-700 bg-green-50 border-green-200',
  },
  {
    value: 'COURT',
    label: 'Court Mandated',
    description: 'Court-ordered reporting or documentation',
    icon: '⚖️',
    color: 'text-gray-700 bg-gray-50 border-gray-200',
  },
];

function NewMandatedReportContent() {
  const router = useRouter();
  const { user } = useCurrentUser();
  const { cases } = useCases({ activeOnly: true });
  const { createReport, loading } = useCreateMandatedReport();
  
  const [step, setStep] = useState(1);
  const [formData, setFormData] = useState<MandatedReportFormData>({
    caseId: '',
    reportType: 'CPS',
    urgency: 'MEDIUM',
    reportingReason: '',
    incidentDescription: '',
    injuriesOrHarm: '',
    witnessInformation: '',
    actionsTaken: '',
    recommendations: '',
    followUpRequired: false,
    followUpDetails: '',
    isConfidential: true,
    mandatedReporter: {
      name: user?.firstName && user?.lastName ? `${user.firstName} ${user.lastName}` : '',
      title: user?.title || '',
      organization: 'Haven DV Services',
      phone: user?.phone || '',
      email: user?.email || '',
    },
    receivingAgency: {
      name: '',
      contactPerson: '',
      phone: '',
      address: '',
    },
  });

  const [selectedCase, setSelectedCase] = useState<Case | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});

  // Pre-populate case if passed via router state
  useEffect(() => {
    if (router.query.caseId) {
      setFormData(prev => ({ ...prev, caseId: router.query.caseId as string }));
    }
  }, [router.query]);

  useEffect(() => {
    if (formData.caseId && cases) {
      const case_ = cases.find(c => c.id === formData.caseId);
      setSelectedCase(case_ || null);
    } else {
      setSelectedCase(null);
    }
  }, [formData.caseId, cases]);

  const handleInputChange = (field: keyof MandatedReportFormData, value: any) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: '' }));
    }
  };

  const handleReporterChange = (field: keyof MandatedReportFormData['mandatedReporter'], value: string) => {
    setFormData(prev => ({
      ...prev,
      mandatedReporter: { ...prev.mandatedReporter, [field]: value }
    }));
  };

  const handleAgencyChange = (field: keyof MandatedReportFormData['receivingAgency'], value: string) => {
    setFormData(prev => ({
      ...prev,
      receivingAgency: { ...prev.receivingAgency, [field]: value }
    }));
  };

  const validateStep = (stepNumber: number): boolean => {
    const newErrors: Record<string, string> = {};

    if (stepNumber === 1) {
      if (!formData.caseId) newErrors.caseId = 'Case selection is required';
      if (!formData.reportingReason.trim()) newErrors.reportingReason = 'Reporting reason is required';
    }

    if (stepNumber === 2) {
      if (!formData.incidentDescription.trim()) newErrors.incidentDescription = 'Incident description is required';
    }

    if (stepNumber === 3) {
      if (!formData.mandatedReporter.name.trim()) newErrors['mandatedReporter.name'] = 'Reporter name is required';
      if (!formData.mandatedReporter.phone.trim()) newErrors['mandatedReporter.phone'] = 'Reporter phone is required';
      if (!formData.receivingAgency.name.trim()) newErrors['receivingAgency.name'] = 'Receiving agency is required';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleNext = () => {
    if (validateStep(step)) {
      setStep(prev => prev + 1);
    }
  };

  const handlePrevious = () => {
    setStep(prev => prev - 1);
  };

  const handleSubmit = async (asDraft = false) => {
    if (!asDraft && !validateStep(step)) return;

    try {
      const reportData = {
        ...formData,
        status: asDraft ? 'DRAFT' : 'PENDING_REVIEW',
        submittedBy: user?.id,
        submittedAt: asDraft ? undefined : new Date().toISOString(),
      };

      const newReport = await createReport(reportData);
      router.push(`/mandated-reports/${newReport.id}`);
    } catch (error) {
      console.error('Failed to create report:', error);
      setErrors({ submit: 'Failed to create report. Please try again.' });
    }
  };

  const caseOptions = cases?.map(case_ => ({
    value: case_.id,
    label: `Case #${case_.caseNumber || case_.id.slice(0, 8)} - ${case_.clientName || 'Unknown Client'}`
  })) || [];

  const selectedReportType = REPORT_TYPES.find(type => type.value === formData.reportType);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-secondary-900">Create Mandated Report</h1>
          <p className="text-secondary-600">Complete mandatory reporting requirements</p>
        </div>
        <div className="flex items-center space-x-3">
          <Link href="/mandated-reports">
            <Button variant="outline">Cancel</Button>
          </Link>
          {step > 1 && (
            <Button onClick={handlePrevious} variant="outline">
              Previous
            </Button>
          )}
          {step < 3 ? (
            <Button onClick={handleNext}>
              Next
            </Button>
          ) : (
            <div className="flex space-x-2">
              <Button onClick={() => handleSubmit(true)} variant="outline" loading={loading}>
                Save as Draft
              </Button>
              <Button onClick={() => handleSubmit(false)} loading={loading}>
                Submit for Review
              </Button>
            </div>
          )}
        </div>
      </div>

      {/* Progress Steps */}
      <div className="flex items-center space-x-4 mb-8">
        {[1, 2, 3].map((stepNumber) => (
          <div key={stepNumber} className="flex items-center">
            <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium ${
              stepNumber <= step 
                ? 'bg-primary-600 text-white' 
                : 'bg-secondary-200 text-secondary-600'
            }`}>
              {stepNumber}
            </div>
            <span className={`ml-2 text-sm ${
              stepNumber <= step ? 'text-primary-600 font-medium' : 'text-secondary-500'
            }`}>
              {stepNumber === 1 && 'Report Details'}
              {stepNumber === 2 && 'Incident Information'}
              {stepNumber === 3 && 'Reporter & Agency Info'}
            </span>
            {stepNumber < 3 && (
              <div className={`w-12 h-0.5 mx-4 ${
                stepNumber < step ? 'bg-primary-600' : 'bg-secondary-200'
              }`} />
            )}
          </div>
        ))}
      </div>

      {errors.submit && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <p className="text-red-700">{errors.submit}</p>
        </div>
      )}

      {/* Step 1: Report Details */}
      {step === 1 && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>Case & Report Type</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <Select
                  label="Select Case"
                  value={formData.caseId}
                  onChange={(value) => handleInputChange('caseId', value)}
                  options={[
                    { value: '', label: 'Choose a case...' },
                    ...caseOptions
                  ]}
                  error={errors.caseId}
                  required
                />

                {selectedCase && (
                  <div className="p-4 bg-secondary-50 border border-secondary-200 rounded-lg">
                    <div className="flex items-center justify-between mb-2">
                      <h4 className="font-medium text-secondary-900">
                        Case #{selectedCase.caseNumber || selectedCase.id.slice(0, 8)}
                      </h4>
                      <Link href={`/cases/${selectedCase.id}`} className="text-primary-600 hover:text-primary-700 text-sm">
                        View Case
                      </Link>
                    </div>
                    <div className="grid grid-cols-2 gap-4 text-sm">
                      <div>
                        <span className="text-secondary-600">Client:</span> {selectedCase.clientName || 'Unknown'}
                      </div>
                      <div>
                        <span className="text-secondary-600">Status:</span> {selectedCase.status}
                      </div>
                      <div>
                        <span className="text-secondary-600">Risk Level:</span> {selectedCase.riskLevel}
                      </div>
                      <div>
                        <span className="text-secondary-600">Assigned To:</span> {selectedCase.assignment?.assigneeName || 'Unassigned'}
                      </div>
                    </div>
                  </div>
                )}

                <div>
                  <label className="block text-sm font-medium text-secondary-700 mb-3">
                    Report Type *
                  </label>
                  <div className="grid grid-cols-1 gap-3">
                    {REPORT_TYPES.map((type) => (
                      <label key={type.value} className={`flex items-center p-4 border-2 rounded-lg cursor-pointer transition-colors ${
                        formData.reportType === type.value
                          ? `${type.color} border-current`
                          : 'border-secondary-200 hover:border-secondary-300'
                      }`}>
                        <input
                          type="radio"
                          name="reportType"
                          value={type.value}
                          checked={formData.reportType === type.value}
                          onChange={(e) => handleInputChange('reportType', e.target.value)}
                          className="sr-only"
                        />
                        <div className="flex items-center space-x-3">
                          <span className="text-2xl">{type.icon}</span>
                          <div>
                            <p className="font-medium text-secondary-900">{type.label}</p>
                            <p className="text-sm text-secondary-600">{type.description}</p>
                          </div>
                        </div>
                      </label>
                    ))}
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
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

                  <Input
                    label="Incident Date"
                    type="date"
                    value={formData.incidentDate || ''}
                    onChange={(e) => handleInputChange('incidentDate', e.target.value)}
                  />
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Reporting Reason</CardTitle>
              </CardHeader>
              <CardContent>
                <Textarea
                  label="Why is this report being made?"
                  placeholder="Describe the specific reason for mandatory reporting, legal obligations, and circumstances requiring this report..."
                  value={formData.reportingReason}
                  onChange={(e) => handleInputChange('reportingReason', e.target.value)}
                  rows={4}
                  error={errors.reportingReason}
                  required
                />
              </CardContent>
            </Card>
          </div>

          <div className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>Report Type Information</CardTitle>
              </CardHeader>
              <CardContent>
                {selectedReportType && (
                  <div className={`p-4 rounded-lg border ${selectedReportType.color}`}>
                    <div className="flex items-center space-x-3 mb-3">
                      <span className="text-2xl">{selectedReportType.icon}</span>
                      <h3 className="font-medium">{selectedReportType.label}</h3>
                    </div>
                    <p className="text-sm mb-4">{selectedReportType.description}</p>
                    
                    {formData.reportType === 'CPS' && (
                      <div className="text-sm space-y-2">
                        <p><strong>Required Information:</strong></p>
                        <ul className="list-disc list-inside space-y-1 text-xs">
                          <li>Child's name, age, and location</li>
                          <li>Nature and extent of abuse/neglect</li>
                          <li>Evidence of previous incidents</li>
                          <li>Immediate safety concerns</li>
                        </ul>
                      </div>
                    )}
                    
                    {formData.reportType === 'APS' && (
                      <div className="text-sm space-y-2">
                        <p><strong>Required Information:</strong></p>
                        <ul className="list-disc list-inside space-y-1 text-xs">
                          <li>Adult's name, age, and location</li>
                          <li>Type of abuse, neglect, or exploitation</li>
                          <li>Suspected perpetrator information</li>
                          <li>Vulnerable adult's capacity</li>
                        </ul>
                      </div>
                    )}
                    
                    {formData.reportType === 'LAW_ENFORCEMENT' && (
                      <div className="text-sm space-y-2">
                        <p><strong>Required Information:</strong></p>
                        <ul className="list-disc list-inside space-y-1 text-xs">
                          <li>Criminal activity details</li>
                          <li>Immediate safety threats</li>
                          <li>Evidence preservation needs</li>
                          <li>Witness information</li>
                        </ul>
                      </div>
                    )}
                  </div>
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Privacy Settings</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  <label className="flex items-center space-x-2">
                    <input
                      type="checkbox"
                      checked={formData.isConfidential}
                      onChange={(e) => handleInputChange('isConfidential', e.target.checked)}
                      className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-secondary-300 rounded"
                    />
                    <span className="text-sm text-secondary-700">Mark as confidential</span>
                  </label>
                  <p className="text-xs text-secondary-500">
                    Confidential reports have restricted access and additional privacy protections.
                  </p>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      )}

      {/* Step 2: Incident Information */}
      {step === 2 && (
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>Incident Description</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <Textarea
                label="Detailed Incident Description"
                placeholder="Provide a detailed, factual description of the incident(s) that led to this report. Include dates, times, locations, and specific behaviors observed..."
                value={formData.incidentDescription}
                onChange={(e) => handleInputChange('incidentDescription', e.target.value)}
                rows={6}
                error={errors.incidentDescription}
                required
              />

              <Textarea
                label="Injuries or Harm Observed"
                placeholder="Describe any physical injuries, emotional trauma, or other harm observed. Be specific about severity and duration..."
                value={formData.injuriesOrHarm}
                onChange={(e) => handleInputChange('injuriesOrHarm', e.target.value)}
                rows={4}
              />

              <Textarea
                label="Witness Information"
                placeholder="List any witnesses to the incident(s), their relationship to those involved, and brief description of what they observed..."
                value={formData.witnessInformation}
                onChange={(e) => handleInputChange('witnessInformation', e.target.value)}
                rows={4}
              />

              <Textarea
                label="Actions Taken"
                placeholder="Describe any immediate actions taken in response to the incident, medical care provided, safety measures implemented, etc..."
                value={formData.actionsTaken}
                onChange={(e) => handleInputChange('actionsTaken', e.target.value)}
                rows={4}
              />

              <Textarea
                label="Recommendations"
                placeholder="Provide recommendations for follow-up actions, additional services needed, or ongoing safety measures..."
                value={formData.recommendations}
                onChange={(e) => handleInputChange('recommendations', e.target.value)}
                rows={4}
              />

              <div className="space-y-3">
                <label className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    checked={formData.followUpRequired}
                    onChange={(e) => handleInputChange('followUpRequired', e.target.checked)}
                    className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-secondary-300 rounded"
                  />
                  <span className="text-sm font-medium text-secondary-700">Follow-up required</span>
                </label>

                {formData.followUpRequired && (
                  <Textarea
                    label="Follow-up Details"
                    placeholder="Describe specific follow-up actions required, timelines, and responsible parties..."
                    value={formData.followUpDetails}
                    onChange={(e) => handleInputChange('followUpDetails', e.target.value)}
                    rows={3}
                  />
                )}
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Step 3: Reporter & Agency Information */}
      {step === 3 && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Card>
            <CardHeader>
              <CardTitle>Mandated Reporter Information</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <Input
                label="Full Name"
                value={formData.mandatedReporter.name}
                onChange={(e) => handleReporterChange('name', e.target.value)}
                error={errors['mandatedReporter.name']}
                required
              />

              <Input
                label="Title/Position"
                value={formData.mandatedReporter.title}
                onChange={(e) => handleReporterChange('title', e.target.value)}
              />

              <Input
                label="Organization"
                value={formData.mandatedReporter.organization}
                onChange={(e) => handleReporterChange('organization', e.target.value)}
              />

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <Input
                  label="Phone Number"
                  value={formData.mandatedReporter.phone}
                  onChange={(e) => handleReporterChange('phone', e.target.value)}
                  error={errors['mandatedReporter.phone']}
                  required
                />

                <Input
                  label="Email Address"
                  type="email"
                  value={formData.mandatedReporter.email}
                  onChange={(e) => handleReporterChange('email', e.target.value)}
                />
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Receiving Agency Information</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <Input
                label="Agency Name"
                value={formData.receivingAgency.name}
                onChange={(e) => handleAgencyChange('name', e.target.value)}
                error={errors['receivingAgency.name']}
                required
                placeholder="e.g., Department of Children and Families"
              />

              <Input
                label="Contact Person"
                value={formData.receivingAgency.contactPerson}
                onChange={(e) => handleAgencyChange('contactPerson', e.target.value)}
                placeholder="Primary contact at receiving agency"
              />

              <Input
                label="Phone Number"
                value={formData.receivingAgency.phone}
                onChange={(e) => handleAgencyChange('phone', e.target.value)}
                placeholder="Agency contact phone"
              />

              <Textarea
                label="Agency Address"
                value={formData.receivingAgency.address}
                onChange={(e) => handleAgencyChange('address', e.target.value)}
                rows={3}
                placeholder="Full mailing address of receiving agency"
              />
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}

export default function NewMandatedReportPage() {
  return (
    <ProtectedRoute>
      <AppLayout 
        title="Create Mandated Report" 
        breadcrumbs={[
          { label: 'Dashboard', href: '/dashboard' },
          { label: 'Mandated Reports', href: '/mandated-reports' },
          { label: 'New Report' }
        ]}
      >
        <div className="p-6">
          <NewMandatedReportContent />
        </div>
      </AppLayout>
    </ProtectedRoute>
  );
}
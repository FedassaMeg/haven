import { useState, useEffect } from 'react';
import { useRouter } from 'next/router';
import Link from 'next/link';
import { ProtectedRoute } from '@haven/auth';
import { Card, CardHeader, CardTitle, CardContent, Button, Badge, Select, Input, Textarea, Label } from '@haven/ui';
import { useClient } from '@haven/api-client';
import AppLayout from '../../components/AppLayout';

// Program types matching HMIS standards
const PROGRAM_TYPES = [
  { value: 'ES', label: 'Emergency Shelter (ES)', description: 'Emergency shelter for individuals and families' },
  { value: 'TH', label: 'Transitional Housing (TH)', description: 'Transitional housing with supportive services' },
  { value: 'RRH', label: 'Rapid Re-Housing (RRH)', description: 'Rapid re-housing assistance' },
  { value: 'PSH', label: 'Permanent Supportive Housing (PSH)', description: 'Permanent housing with supportive services' },
  { value: 'PH', label: 'Permanent Housing (PH)', description: 'Permanent housing without ongoing services' },
  { value: 'SO', label: 'Street Outreach (SO)', description: 'Outreach to unsheltered individuals' },
  { value: 'PREV', label: 'Homelessness Prevention', description: 'Prevention assistance for at-risk households' },
  { value: 'CE', label: 'Coordinated Entry (CE)', description: 'Coordinated entry assessment and referral' },
  { value: 'SSO', label: 'Services Only (SSO)', description: 'Services without housing assistance' },
];

// HMIS Data Collection Stages
const DATA_COLLECTION_STAGES = [
  { value: 'PROJECT_START', label: 'Project Start', description: 'Initial enrollment data collection' },
  { value: 'PROJECT_UPDATE', label: 'Project Update/Annual Assessment', description: 'Annual update or assessment' },
  { value: 'PROJECT_EXIT', label: 'Project Exit', description: 'Exit assessment' },
];

// Living Situation codes (HMIS R2.07)
const LIVING_SITUATIONS = [
  { value: '101', label: 'Emergency Shelter' },
  { value: '116', label: 'Place not meant for habitation' },
  { value: '118', label: 'Safe Haven' },
  { value: '204', label: 'Psychiatric hospital or other psychiatric facility' },
  { value: '205', label: 'Substance abuse treatment facility or detox center' },
  { value: '206', label: 'Hospital or other residential non-psychiatric medical facility' },
  { value: '207', label: 'Jail, prison or juvenile detention facility' },
  { value: '215', label: 'Foster care home or foster care group home' },
  { value: '225', label: 'Long-term care facility or nursing home' },
  { value: '302', label: 'Transitional housing for homeless persons' },
  { value: '314', label: 'Hotel or motel paid for without emergency shelter voucher' },
  { value: '329', label: 'Residential project or halfway house with no homeless criteria' },
  { value: '332', label: 'Host Home (non-crisis)' },
  { value: '335', label: 'Staying or living with family, temporary tenure' },
  { value: '336', label: 'Staying or living with friends, temporary tenure' },
  { value: '410', label: 'Rental by client, no ongoing housing subsidy' },
  { value: '421', label: 'Owned by client, no ongoing housing subsidy' },
  { value: '435', label: 'Rental by client, with VASH housing subsidy' },
  { value: '436', label: 'Rental by client, with other ongoing housing subsidy' },
  { value: '8', label: 'Client doesn\'t know' },
  { value: '9', label: 'Client prefers not to answer' },
  { value: '99', label: 'Data not collected' },
];

// Length of Stay codes
const LENGTH_OF_STAY_OPTIONS = [
  { value: '10', label: 'One night or less' },
  { value: '11', label: 'Two to six nights' },
  { value: '2', label: 'One week or more, but less than one month' },
  { value: '3', label: 'One month or more, but less than 90 days' },
  { value: '4', label: '90 days or more but less than one year' },
  { value: '5', label: 'One year or longer' },
  { value: '8', label: 'Client doesn\'t know' },
  { value: '9', label: 'Client prefers not to answer' },
  { value: '99', label: 'Data not collected' },
];

interface EnrollmentFormData {
  clientId: string;
  programId: string;
  programType: string;
  enrollmentDate: string;
  dataCollectionStage: string;

  // Housing History
  priorLivingSituation: string;
  lengthOfStay: string;
  timesHomelessPast3Years: number;
  monthsHomelessPast3Years: number;
  approximateDateHomeless?: string;

  // Household Composition
  householdType: 'SINGLE_ADULT' | 'FAMILY_WITH_CHILDREN' | 'COUPLE_NO_CHILDREN' | 'MULTIGENERATIONAL' | 'OTHER';
  relationshipToHoH: string;

  // Housing Move-In (for RRH/PSH)
  residentialMoveInDate?: string;

  // Enrollment Details
  entryLocation: string;
  notes: string;

  // Worker Information
  enrollmentWorker: string;
  enrollmentWorkerId: string;
}

export default function NewEnrollmentPage() {
  const router = useRouter();
  const { clientId } = router.query;

  const { client, loading: clientLoading } = useClient(clientId as string);

  const [formData, setFormData] = useState<EnrollmentFormData>({
    clientId: (clientId as string) || '',
    programId: '',
    programType: '',
    enrollmentDate: new Date().toISOString().split('T')[0],
    dataCollectionStage: 'PROJECT_START',
    priorLivingSituation: '',
    lengthOfStay: '',
    timesHomelessPast3Years: 0,
    monthsHomelessPast3Years: 0,
    approximateDateHomeless: '',
    householdType: 'SINGLE_ADULT',
    relationshipToHoH: 'SELF',
    residentialMoveInDate: '',
    entryLocation: '',
    notes: '',
    enrollmentWorker: '',
    enrollmentWorkerId: '',
  });

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  // Update clientId in formData when query param changes
  useEffect(() => {
    if (clientId && typeof clientId === 'string') {
      setFormData(prev => ({ ...prev, clientId }));
    }
  }, [clientId]);

  // Validate clientId is present
  useEffect(() => {
    if (router.isReady && !clientId) {
      router.push('/clients');
    }
  }, [router.isReady, clientId, router]);

  const updateFormData = (updates: Partial<EnrollmentFormData>) => {
    setFormData(prev => ({ ...prev, ...updates }));
    // Clear errors for updated fields
    const updatedFields = Object.keys(updates);
    setErrors(prev => {
      const newErrors = { ...prev };
      updatedFields.forEach(field => delete newErrors[field]);
      return newErrors;
    });
  };

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.clientId) {
      newErrors.clientId = 'Client ID is required';
    }
    if (!formData.programId) {
      newErrors.programId = 'Program selection is required';
    }
    if (!formData.programType) {
      newErrors.programType = 'Program type is required';
    }
    if (!formData.enrollmentDate) {
      newErrors.enrollmentDate = 'Enrollment date is required';
    }
    if (!formData.priorLivingSituation) {
      newErrors.priorLivingSituation = 'Prior living situation is required for HMIS compliance';
    }
    if (!formData.lengthOfStay) {
      newErrors.lengthOfStay = 'Length of stay is required for HMIS compliance';
    }
    if (!formData.entryLocation) {
      newErrors.entryLocation = 'Entry location is required';
    }
    if (!formData.enrollmentWorker) {
      newErrors.enrollmentWorker = 'Enrollment worker name is required';
    }
    if (!formData.enrollmentWorkerId) {
      newErrors.enrollmentWorkerId = 'Enrollment worker ID is required';
    }

    // Validate RRH/PSH specific fields
    if (['RRH', 'PSH', 'PH'].includes(formData.programType)) {
      if (formData.residentialMoveInDate && formData.residentialMoveInDate < formData.enrollmentDate) {
        newErrors.residentialMoveInDate = 'Move-in date cannot be before enrollment date';
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async () => {
    if (!validateForm()) {
      return;
    }

    setSubmitting(true);
    try {
      // TODO: Implement API call to create enrollment
      const enrollmentData = {
        clientId: formData.clientId,
        programId: formData.programId,
        enrollmentDate: formData.enrollmentDate,
        dataCollectionStage: formData.dataCollectionStage,
        priorLivingSituation: formData.priorLivingSituation,
        lengthOfStay: formData.lengthOfStay,
        timesHomelessPast3Years: formData.timesHomelessPast3Years,
        monthsHomelessPast3Years: formData.monthsHomelessPast3Years,
        approximateDateHomeless: formData.approximateDateHomeless,
        householdType: formData.householdType,
        relationshipToHoH: formData.relationshipToHoH,
        residentialMoveInDate: formData.residentialMoveInDate,
        entryLocation: formData.entryLocation,
        notes: formData.notes,
        enrolledBy: formData.enrollmentWorkerId,
      };

      // await apiClient.createEnrollment(enrollmentData);
      console.log('Creating enrollment with data:', enrollmentData);

      // For now, simulate successful enrollment
      await new Promise(resolve => setTimeout(resolve, 1000));

      // Navigate back to client detail page
      router.push(`/clients/${formData.clientId}?tab=enrollments&enrollment=new`);
    } catch (error) {
      console.error('Failed to create enrollment:', error);
      setErrors({ submit: 'Failed to create enrollment. Please try again.' });
    } finally {
      setSubmitting(false);
    }
  };

  if (!router.isReady) {
    return null;
  }

  if (!clientId) {
    return null;
  }

  if (clientLoading) {
    return (
      <ProtectedRoute>
        <AppLayout title="Enroll Client">
          <div className="flex items-center justify-center h-64">
            <div className="text-secondary-600">Loading client information...</div>
          </div>
        </AppLayout>
      </ProtectedRoute>
    );
  }

  if (!client) {
    return (
      <ProtectedRoute>
        <AppLayout title="Enroll Client">
          <div className="flex items-center justify-center h-64">
            <div className="text-error-600">Client not found</div>
          </div>
        </AppLayout>
      </ProtectedRoute>
    );
  }

  const fullName = client.name
    ? `${client.name.given?.join(' ') || ''} ${client.name.family || ''}`.trim()
    : 'Unknown';

  const selectedProgramType = PROGRAM_TYPES.find(p => p.value === formData.programType);

  return (
    <ProtectedRoute>
      <AppLayout
        title="Enroll Client in Program"
        breadcrumbs={[
          { label: 'Dashboard', href: '/dashboard' },
          { label: 'Clients', href: '/clients' },
          { label: fullName, href: `/clients/${clientId}` },
          { label: 'New Enrollment' }
        ]}
      >
        <div className="p-6">
          <div className="max-w-4xl mx-auto">
            {/* Client Header */}
            <Card className="mb-6">
              <CardContent className="p-6">
                <div className="flex items-center justify-between">
                  <div>
                    <h2 className="text-2xl font-bold text-secondary-900">Enrolling: {fullName}</h2>
                    <p className="text-secondary-600 mt-1">Client ID: {clientId}</p>
                  </div>
                  <Badge variant="outline" size="lg">
                    {client.gender}
                  </Badge>
                </div>
              </CardContent>
            </Card>

            {/* Enrollment Form */}
            <Card className="mb-6">
              <CardHeader>
                <CardTitle>Program Enrollment Information</CardTitle>
              </CardHeader>
              <CardContent>
                {/* Error Summary */}
                {Object.keys(errors).length > 0 && (
                  <div className="bg-destructive-50 border border-destructive-200 rounded-lg p-4 mb-6">
                    <div className="flex">
                      <svg className="h-5 w-5 text-destructive-600 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
                        <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                      </svg>
                      <div className="ml-3">
                        <h3 className="text-sm font-medium text-destructive-800">
                          Please fix the following errors to continue:
                        </h3>
                        <ul className="mt-2 text-sm text-destructive-700 space-y-1">
                          {Object.entries(errors).map(([field, error]) => (
                            <li key={field}>• {error}</li>
                          ))}
                        </ul>
                      </div>
                    </div>
                  </div>
                )}

                <div className="space-y-6">
                  {/* Program Selection */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                      <Label htmlFor="programType" className="required">Program Type</Label>
                      <Select
                        id="programType"
                        value={formData.programType}
                        onChange={(e) => updateFormData({ programType: e.target.value })}
                        className={errors.programType ? 'border-error-500' : ''}
                      >
                        <option value="">Select program type</option>
                        {PROGRAM_TYPES.map(type => (
                          <option key={type.value} value={type.value}>
                            {type.label}
                          </option>
                        ))}
                      </Select>
                      {selectedProgramType && (
                        <p className="text-xs text-secondary-600 mt-1">{selectedProgramType.description}</p>
                      )}
                      {errors.programType && (
                        <p className="text-sm text-error-600 mt-1">{errors.programType}</p>
                      )}
                    </div>

                    <div>
                      <Label htmlFor="programId" className="required">Program/Project</Label>
                      <Select
                        id="programId"
                        value={formData.programId}
                        onChange={(e) => updateFormData({ programId: e.target.value })}
                        className={errors.programId ? 'border-error-500' : ''}
                        disabled={!formData.programType}
                      >
                        <option value="">Select program</option>
                        {/* TODO: Load programs based on selected type */}
                        <option value="program-1">Safe Haven Emergency Shelter</option>
                        <option value="program-2">RRH - Families Program</option>
                        <option value="program-3">PSH - Single Adults</option>
                        <option value="program-4">DV Transitional Housing</option>
                      </Select>
                      {errors.programId && (
                        <p className="text-sm text-error-600 mt-1">{errors.programId}</p>
                      )}
                    </div>
                  </div>

                  {/* Enrollment Details */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                      <Label htmlFor="enrollmentDate" className="required">Enrollment Date</Label>
                      <Input
                        id="enrollmentDate"
                        type="date"
                        value={formData.enrollmentDate}
                        onChange={(e) => updateFormData({ enrollmentDate: e.target.value })}
                        className={errors.enrollmentDate ? 'border-error-500' : ''}
                        max={new Date().toISOString().split('T')[0]}
                      />
                      {errors.enrollmentDate && (
                        <p className="text-sm text-error-600 mt-1">{errors.enrollmentDate}</p>
                      )}
                    </div>

                    <div>
                      <Label htmlFor="dataCollectionStage" className="required">Data Collection Stage</Label>
                      <Select
                        id="dataCollectionStage"
                        value={formData.dataCollectionStage}
                        onChange={(e) => updateFormData({ dataCollectionStage: e.target.value })}
                      >
                        {DATA_COLLECTION_STAGES.map(stage => (
                          <option key={stage.value} value={stage.value}>
                            {stage.label}
                          </option>
                        ))}
                      </Select>
                      <p className="text-xs text-secondary-600 mt-1">
                        {DATA_COLLECTION_STAGES.find(s => s.value === formData.dataCollectionStage)?.description}
                      </p>
                    </div>
                  </div>

                  {/* Housing History (HMIS Required) */}
                  <div className="border-t pt-6">
                    <h3 className="text-lg font-semibold text-secondary-900 mb-4">
                      Housing History
                      <span className="text-sm font-normal text-secondary-600 ml-2">(HMIS Required)</span>
                    </h3>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      <div>
                        <Label htmlFor="priorLivingSituation" className="required">
                          Prior Living Situation
                        </Label>
                        <Select
                          id="priorLivingSituation"
                          value={formData.priorLivingSituation}
                          onChange={(e) => updateFormData({ priorLivingSituation: e.target.value })}
                          className={errors.priorLivingSituation ? 'border-error-500' : ''}
                        >
                          <option value="">Select living situation</option>
                          {LIVING_SITUATIONS.map(situation => (
                            <option key={situation.value} value={situation.value}>
                              {situation.label}
                            </option>
                          ))}
                        </Select>
                        {errors.priorLivingSituation && (
                          <p className="text-sm text-error-600 mt-1">{errors.priorLivingSituation}</p>
                        )}
                      </div>

                      <div>
                        <Label htmlFor="lengthOfStay" className="required">Length of Stay in Prior Living Situation</Label>
                        <Select
                          id="lengthOfStay"
                          value={formData.lengthOfStay}
                          onChange={(e) => updateFormData({ lengthOfStay: e.target.value })}
                          className={errors.lengthOfStay ? 'border-error-500' : ''}
                        >
                          <option value="">Select length of stay</option>
                          {LENGTH_OF_STAY_OPTIONS.map(option => (
                            <option key={option.value} value={option.value}>
                              {option.label}
                            </option>
                          ))}
                        </Select>
                        {errors.lengthOfStay && (
                          <p className="text-sm text-error-600 mt-1">{errors.lengthOfStay}</p>
                        )}
                      </div>

                      <div>
                        <Label htmlFor="approximateDateHomeless">Approximate Date Homelessness Started</Label>
                        <Input
                          id="approximateDateHomeless"
                          type="date"
                          value={formData.approximateDateHomeless}
                          onChange={(e) => updateFormData({ approximateDateHomeless: e.target.value })}
                          max={new Date().toISOString().split('T')[0]}
                        />
                      </div>

                      <div>
                        <Label htmlFor="timesHomelessPast3Years">
                          Number of Times Homeless in Past 3 Years
                        </Label>
                        <Input
                          id="timesHomelessPast3Years"
                          type="number"
                          min="0"
                          value={formData.timesHomelessPast3Years}
                          onChange={(e) => updateFormData({ timesHomelessPast3Years: parseInt(e.target.value) || 0 })}
                        />
                      </div>

                      <div>
                        <Label htmlFor="monthsHomelessPast3Years">
                          Total Months Homeless in Past 3 Years
                        </Label>
                        <Input
                          id="monthsHomelessPast3Years"
                          type="number"
                          min="0"
                          max="36"
                          value={formData.monthsHomelessPast3Years}
                          onChange={(e) => updateFormData({ monthsHomelessPast3Years: parseInt(e.target.value) || 0 })}
                        />
                      </div>
                    </div>
                  </div>

                  {/* Household Composition */}
                  <div className="border-t pt-6">
                    <h3 className="text-lg font-semibold text-secondary-900 mb-4">Household Composition</h3>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      <div>
                        <Label htmlFor="householdType">Household Type</Label>
                        <Select
                          id="householdType"
                          value={formData.householdType}
                          onChange={(e) => updateFormData({ householdType: e.target.value as any })}
                        >
                          <option value="SINGLE_ADULT">Single Adult</option>
                          <option value="FAMILY_WITH_CHILDREN">Family with Children</option>
                          <option value="COUPLE_NO_CHILDREN">Couple without Children</option>
                          <option value="MULTIGENERATIONAL">Multigenerational</option>
                          <option value="OTHER">Other</option>
                        </Select>
                      </div>

                      <div>
                        <Label htmlFor="relationshipToHoH">Relationship to Head of Household</Label>
                        <Select
                          id="relationshipToHoH"
                          value={formData.relationshipToHoH}
                          onChange={(e) => updateFormData({ relationshipToHoH: e.target.value })}
                        >
                          <option value="SELF">Self (Head of Household)</option>
                          <option value="SPOUSE">Spouse or Partner</option>
                          <option value="CHILD">Child</option>
                          <option value="PARENT">Parent</option>
                          <option value="SIBLING">Sibling</option>
                          <option value="OTHER_FAMILY">Other Family Member</option>
                          <option value="OTHER">Other</option>
                        </Select>
                      </div>
                    </div>
                  </div>

                  {/* Housing Move-In (for RRH/PSH/PH) */}
                  {['RRH', 'PSH', 'PH'].includes(formData.programType) && (
                    <div className="border-t pt-6">
                      <h3 className="text-lg font-semibold text-secondary-900 mb-4">Housing Move-In Details</h3>
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div>
                          <Label htmlFor="residentialMoveInDate">Residential Move-In Date (if applicable)</Label>
                          <Input
                            id="residentialMoveInDate"
                            type="date"
                            value={formData.residentialMoveInDate}
                            onChange={(e) => updateFormData({ residentialMoveInDate: e.target.value })}
                            min={formData.enrollmentDate}
                            className={errors.residentialMoveInDate ? 'border-error-500' : ''}
                          />
                          <p className="text-xs text-secondary-600 mt-1">
                            Leave blank if client has not yet moved into housing
                          </p>
                          {errors.residentialMoveInDate && (
                            <p className="text-sm text-error-600 mt-1">{errors.residentialMoveInDate}</p>
                          )}
                        </div>
                      </div>
                    </div>
                  )}

                  {/* Entry Location & Notes */}
                  <div className="border-t pt-6">
                    <h3 className="text-lg font-semibold text-secondary-900 mb-4">Additional Information</h3>

                    <div className="space-y-4">
                      <div>
                        <Label htmlFor="entryLocation" className="required">Entry Location</Label>
                        <Input
                          id="entryLocation"
                          type="text"
                          placeholder="e.g., Main Office, Outreach Site, Partner Agency"
                          value={formData.entryLocation}
                          onChange={(e) => updateFormData({ entryLocation: e.target.value })}
                          className={errors.entryLocation ? 'border-error-500' : ''}
                        />
                        {errors.entryLocation && (
                          <p className="text-sm text-error-600 mt-1">{errors.entryLocation}</p>
                        )}
                      </div>

                      <div>
                        <Label htmlFor="notes">Enrollment Notes</Label>
                        <Textarea
                          id="notes"
                          rows={4}
                          placeholder="Additional notes or special circumstances regarding this enrollment..."
                          value={formData.notes}
                          onChange={(e) => updateFormData({ notes: e.target.value })}
                        />
                      </div>
                    </div>
                  </div>

                  {/* Worker Information */}
                  <div className="border-t pt-6">
                    <h3 className="text-lg font-semibold text-secondary-900 mb-4">Worker Information</h3>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      <div>
                        <Label htmlFor="enrollmentWorker" className="required">Enrollment Worker Name</Label>
                        <Input
                          id="enrollmentWorker"
                          type="text"
                          placeholder="Full name of worker completing enrollment"
                          value={formData.enrollmentWorker}
                          onChange={(e) => updateFormData({ enrollmentWorker: e.target.value })}
                          className={errors.enrollmentWorker ? 'border-error-500' : ''}
                        />
                        {errors.enrollmentWorker && (
                          <p className="text-sm text-error-600 mt-1">{errors.enrollmentWorker}</p>
                        )}
                      </div>

                      <div>
                        <Label htmlFor="enrollmentWorkerId" className="required">Worker ID/Badge Number</Label>
                        <Input
                          id="enrollmentWorkerId"
                          type="text"
                          placeholder="Worker identification number"
                          value={formData.enrollmentWorkerId}
                          onChange={(e) => updateFormData({ enrollmentWorkerId: e.target.value })}
                          className={errors.enrollmentWorkerId ? 'border-error-500' : ''}
                        />
                        {errors.enrollmentWorkerId && (
                          <p className="text-sm text-error-600 mt-1">{errors.enrollmentWorkerId}</p>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Action Buttons */}
            <div className="flex items-center justify-between">
              <Link href={`/clients/${formData.clientId}`}>
                <Button variant="outline">Cancel</Button>
              </Link>

              <div className="flex items-center space-x-4">
                <Button
                  onClick={handleSubmit}
                  disabled={submitting}
                  className="bg-success-600 hover:bg-success-700"
                >
                  {submitting ? 'Creating Enrollment...' : 'Complete Enrollment'}
                </Button>
              </div>
            </div>
          </div>
        </div>
      </AppLayout>
    </ProtectedRoute>
  );
}

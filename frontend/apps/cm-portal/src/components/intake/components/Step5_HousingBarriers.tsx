/**
 * Step 5: Housing Barrier Assessment
 *
 * Identifies barriers to housing stability and auto-generates support plan.
 * Uses accordion layout for better UX with multiple assessment areas.
 */

import React, { useState, useEffect } from 'react';
import {
  Field,
  FieldContent,
  FieldError,
  FieldGroup,
  FieldLabel,
  FieldLegend,
  FieldSet,
  FieldTitle,
  FormCheckbox,
  FormSelect,
  Input,
  Label,
  RadioGroup,
  RadioGroupItem,
  Textarea,
} from '@haven/ui';
import type { HousingBarrierData, ValidationError } from '../utils/types';
import { validateStep5 } from '../lib/validation';
import { calculateBarrierSeverity, generateStabilityPlan } from '../index';

interface Step5Props {
  data: Partial<HousingBarrierData>;
  errors: ValidationError[];
  warnings: ValidationError[];
  onChange: (updates: Partial<HousingBarrierData>) => void;
  onComplete: (data: HousingBarrierData) => void;
  onBack: () => void;
}

export const Step5_HousingBarriers: React.FC<Step5Props> = ({
  data,
  errors,
  warnings,
  onChange,
  onComplete,
  onBack,
}) => {
  const [formData, setFormData] = useState<Partial<HousingBarrierData>>({
    rentalHistory: {
      hasRentalHistory: false,
      evictionHistory: false,
      landlordReferences: 'NONE',
      ...data.rentalHistory,
    },
    creditHistory: {
      disclosed: false,
      collections: false,
      bankruptcy: false,
      medicalDebt: false,
      studentLoanDebt: false,
      ...data.creditHistory,
    },
    criminalBackground: {
      disclosed: false,
      onSupervision: false,
      registeredSexOffender: false,
      ...data.criminalBackground,
    },
    employmentStatus: {
      currentlyEmployed: false,
      lookingForWork: false,
      hasResume: false,
      interestedInTraining: false,
      ...data.employmentStatus,
    },
    supportNetwork: {
      hasSupportNetwork: false,
      canProvideReferences: false,
      localNetwork: false,
      networkCanHouse: false,
      isolated: false,
      ...data.supportNetwork,
    },
    transportation: {
      hasReliableTransportation: false,
      driversLicense: false,
      licenseSuspended: false,
      hasVehicle: false,
      publicTransitAccess: false,
      ...data.transportation,
    },
    identifiedBarriers: data.identifiedBarriers || [],
    stabilityPlan: data.stabilityPlan || {
      needsEmploymentSupport: false,
      needsLandlordMediation: false,
      needsCreditRepair: false,
      needsMentalHealthReferral: false,
      needsSubstanceAbuseReferral: false,
      needsLegalAdvocacy: false,
      needsFinancialLiteracy: false,
      needsChildcare: false,
      needsTransportation: false,
      priorityInterventions: [],
    },
    barrierSeverity: 'LOW',
    ...data,
  });

  const [validationErrors, setValidationErrors] = useState<ValidationError[]>(errors);
  const [validationWarnings, setValidationWarnings] = useState<ValidationError[]>(warnings);
  const [openAccordion, setOpenAccordion] = useState<string>('rental');

  useEffect(() => {
    setFormData(prev => ({ ...prev, ...data }));
  }, [data]);

  useEffect(() => {
    setValidationErrors(errors);
    setValidationWarnings(warnings);
  }, [errors, warnings]);

  // Auto-calculate barrier severity and generate stability plan
  useEffect(() => {
    const severity = calculateBarrierSeverity(formData as HousingBarrierData);
    const stabilityPlan = generateStabilityPlan(formData as HousingBarrierData);

    if (severity !== formData.barrierSeverity) {
      setFormData(prev => ({ ...prev, barrierSeverity: severity }));
      onChange({ barrierSeverity: severity });
    }

    if (JSON.stringify(stabilityPlan) !== JSON.stringify(formData.stabilityPlan)) {
      setFormData(prev => ({ ...prev, stabilityPlan }));
      onChange({ stabilityPlan });
    }
  }, [
    formData.rentalHistory,
    formData.creditHistory,
    formData.criminalBackground,
    formData.employmentStatus,
    formData.supportNetwork,
    formData.transportation,
  ]);

  const handleChange = (field: keyof HousingBarrierData, value: any) => {
    const updates = { [field]: value };
    setFormData(prev => ({ ...prev, ...updates }));
    onChange(updates);
  };

  const handleNestedChange = (parent: keyof HousingBarrierData, field: string, value: any) => {
    const updates = {
      [parent]: {
        ...(formData[parent] as any),
        [field]: value,
      },
    };
    setFormData(prev => ({ ...prev, ...updates }));
    onChange(updates);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    const result = validateStep5(formData as HousingBarrierData);
    setValidationErrors(result.errors);
    setValidationWarnings(result.warnings);

    if (result.isValid) {
      onComplete(formData as HousingBarrierData);
    } else {
      const firstErrorField = result.errors[0]?.field;
      if (firstErrorField) {
        document.getElementById(`field-${firstErrorField}`)?.focus();
      }
    }
  };

  const toggleAccordion = (section: string) => {
    setOpenAccordion(openAccordion === section ? '' : section);
  };

  const getFieldError = (fieldName: string): string | undefined => {
    return validationErrors.find(e => e.field === fieldName)?.message;
  };

  const getFieldWarning = (fieldName: string): string | undefined => {
    return validationWarnings.find(w => w.field === fieldName)?.message;
  };

  const severityConfig = {
    LOW: { color: '#10b981', label: 'Low', description: 'Few barriers identified' },
    MODERATE: { color: '#f59e0b', label: 'Moderate', description: 'Some barriers present' },
    HIGH: { color: '#ef4444', label: 'High', description: 'Significant barriers' },
    SEVERE: { color: '#dc2626', label: 'Severe', description: 'Multiple critical barriers' },
  };

  const currentSeverity = severityConfig[formData.barrierSeverity || 'LOW'];

  return (
    <div className="intake-step step-5">
      <div className="step-header">
        <h2>Step 5: Housing Barrier Assessment</h2>
        <p className="step-description">
          Identify barriers to housing stability. Information is used to create a personalized
          support plan.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="intake-form">
        {/* Accordion Layout */}
        <div className="accordion">
          {/* Section 1: Rental History */}
          <div className="accordion-item">
            <button
              type="button"
              className={`accordion-header ${openAccordion === 'rental' ? 'open' : ''}`}
              onClick={() => toggleAccordion('rental')}
            >
              <span>1. Rental History</span>
              <span className="accordion-icon">{openAccordion === 'rental' ? '▼' : '▶'}</span>
            </button>

            {openAccordion === 'rental' && (
              <div className="accordion-content">
                <FieldGroup>
                  <Field>
                    <FieldTitle>Does client have rental history?</FieldTitle>
                    <FieldContent>
                      <RadioGroup
                        value={
                          formData.rentalHistory?.hasRentalHistory === true
                            ? 'yes'
                            : formData.rentalHistory?.hasRentalHistory === false
                            ? 'no'
                            : ''
                        }
                        onValueChange={value =>
                          handleNestedChange('rentalHistory', 'hasRentalHistory', value === 'yes')
                        }
                      >
                        <div className="flex flex-col gap-2">
                          <div className="flex items-center gap-2">
                            <RadioGroupItem value="yes" id="field-hasRentalHistory-yes" />
                            <Label htmlFor="field-hasRentalHistory-yes">Yes</Label>
                          </div>
                          <div className="flex items-center gap-2">
                            <RadioGroupItem value="no" id="field-hasRentalHistory-no" />
                            <Label htmlFor="field-hasRentalHistory-no">No</Label>
                          </div>
                        </div>
                      </RadioGroup>
                    </FieldContent>
                  </Field>
                </FieldGroup>

                {formData.rentalHistory?.hasRentalHistory && (
                  <>
                    <FieldGroup>
                      <Field>
                        <FieldTitle>Eviction history?</FieldTitle>
                        <FieldContent>
                          <RadioGroup
                            value={
                              formData.rentalHistory?.evictionHistory === true
                                ? 'yes'
                                : formData.rentalHistory?.evictionHistory === false
                                ? 'no'
                                : ''
                            }
                            onValueChange={value =>
                              handleNestedChange('rentalHistory', 'evictionHistory', value === 'yes')
                            }
                          >
                            <div className="flex flex-col gap-2">
                              <div className="flex items-center gap-2">
                                <RadioGroupItem value="yes" id="field-evictionHistory-yes" />
                                <Label htmlFor="field-evictionHistory-yes">Yes</Label>
                              </div>
                              <div className="flex items-center gap-2">
                                <RadioGroupItem value="no" id="field-evictionHistory-no" />
                                <Label htmlFor="field-evictionHistory-no">No</Label>
                              </div>
                            </div>
                          </RadioGroup>
                        </FieldContent>
                      </Field>
                    </FieldGroup>

                    {formData.rentalHistory?.evictionHistory && (
                      <FieldGroup>
                        <Field data-invalid={!!getFieldError('evictionCount')}>
                          <FieldLabel htmlFor="field-evictionCount">Number of Evictions</FieldLabel>
                          <FieldContent>
                            <Input
                              id="field-evictionCount"
                              type="number"
                              min={1}
                              value={formData.rentalHistory?.evictionCount || ''}
                              onChange={e =>
                                handleNestedChange(
                                  'rentalHistory',
                                  'evictionCount',
                                  parseInt(e.target.value, 10) || 0
                                )
                              }
                            />
                            <FieldError>{getFieldError('evictionCount')}</FieldError>
                          </FieldContent>
                        </Field>

                        <Field>
                          <FieldLabel htmlFor="field-lastEvictionDate">
                            Most Recent Eviction Date
                          </FieldLabel>
                          <FieldContent>
                            <Input
                              id="field-lastEvictionDate"
                              type="date"
                              value={formData.rentalHistory?.lastEvictionDate || ''}
                              onChange={e =>
                                handleNestedChange(
                                  'rentalHistory',
                                  'lastEvictionDate',
                                  e.target.value
                                )
                              }
                              max={new Date().toISOString().split('T')[0]}
                            />
                          </FieldContent>
                        </Field>
                      </FieldGroup>
                    )}

                    <FieldGroup>
                      <Field data-invalid={!!getFieldError('landlordReferences')}>
                        <FieldLabel htmlFor="field-landlordReferences">
                          Landlord References
                        </FieldLabel>
                        <FieldContent>
                          <FormSelect
                            id="field-landlordReferences"
                            value={formData.rentalHistory?.landlordReferences || ''}
                            onChange={value =>
                              handleNestedChange('rentalHistory', 'landlordReferences', value)
                            }
                            options={[
                              { value: 'POSITIVE', label: 'Positive' },
                              { value: 'NEGATIVE', label: 'Negative' },
                              { value: 'MIXED', label: 'Mixed' },
                              { value: 'NONE', label: 'None' },
                              { value: 'UNKNOWN', label: 'Unknown' },
                            ]}
                            placeholder="Select landlord reference status"
                            error={getFieldError('landlordReferences')}
                          />
                        </FieldContent>
                      </Field>
                    </FieldGroup>

                    <FieldGroup>
                      <Field data-invalid={!!getFieldError('rentalDebt')}>
                        <FieldLabel htmlFor="field-rentalDebt">Rental Debt ($)</FieldLabel>
                        <FieldContent>
                          <Input
                            id="field-rentalDebt"
                            type="number"
                            min={0}
                            step={0.01}
                            value={formData.rentalHistory?.rentalDebt || ''}
                            onChange={e =>
                              handleNestedChange(
                                'rentalHistory',
                                'rentalDebt',
                                parseFloat(e.target.value) || 0
                              )
                            }
                          />
                          <FieldError>{getFieldError('rentalDebt')}</FieldError>
                        </FieldContent>
                      </Field>

                      <Field data-invalid={!!getFieldError('utilityDebt')}>
                        <FieldLabel htmlFor="field-utilityDebt">Utility Debt ($)</FieldLabel>
                        <FieldContent>
                          <Input
                            id="field-utilityDebt"
                            type="number"
                            min={0}
                            step={0.01}
                            value={formData.rentalHistory?.utilityDebt || ''}
                            onChange={e =>
                              handleNestedChange(
                                'rentalHistory',
                                'utilityDebt',
                                parseFloat(e.target.value) || 0
                              )
                            }
                          />
                          <FieldError>{getFieldError('utilityDebt')}</FieldError>
                        </FieldContent>
                      </Field>
                    </FieldGroup>
                  </>
                )}
              </div>
            )}
          </div>

          {/* Section 2: Credit & Financial History */}
          <div className="accordion-item">
            <button
              type="button"
              className={`accordion-header ${openAccordion === 'credit' ? 'open' : ''}`}
              onClick={() => toggleAccordion('credit')}
            >
              <span>2. Credit & Financial History</span>
              <span className="accordion-icon">{openAccordion === 'credit' ? '▼' : '▶'}</span>
            </button>

            {openAccordion === 'credit' && (
              <div className="accordion-content">
                <FieldGroup>
                  <Field>
                    <FieldContent>
                      <FormCheckbox
                        id="field-creditDisclosed"
                        label="Client disclosed credit history"
                        checked={formData.creditHistory?.disclosed === true}
                        onCheckedChange={checked =>
                          handleNestedChange('creditHistory', 'disclosed', checked === true)
                        }
                      />
                    </FieldContent>
                  </Field>
                </FieldGroup>

                {formData.creditHistory?.disclosed && (
                  <>
                    <FieldGroup>
                      <Field data-invalid={!!getFieldError('creditScore')}>
                        <FieldLabel htmlFor="field-creditScore">Credit Score (300-850)</FieldLabel>
                        <FieldContent>
                          <Input
                            id="field-creditScore"
                            type="number"
                            min={300}
                            max={850}
                            value={formData.creditHistory?.creditScore || ''}
                            onChange={e =>
                              handleNestedChange(
                                'creditHistory',
                                'creditScore',
                                parseInt(e.target.value, 10) || 0
                              )
                            }
                          />
                          <FieldError>{getFieldError('creditScore')}</FieldError>
                          {formData.creditHistory?.creditScore && (
                            <div className="credit-score-indicator mt-2">
                              <div
                                className="score-bar"
                                style={{
                                  width: `${
                                    ((formData.creditHistory.creditScore - 300) / 550) * 100
                                  }%`,
                                  backgroundColor:
                                    formData.creditHistory.creditScore >= 700
                                      ? '#10b981'
                                      : formData.creditHistory.creditScore >= 650
                                      ? '#f59e0b'
                                      : '#ef4444',
                                }}
                              />
                            </div>
                          )}
                        </FieldContent>
                      </Field>
                    </FieldGroup>

                    <FieldGroup data-slot="checkbox-group">
                      <Field>
                        <FieldContent>
                          <FormCheckbox
                            id="field-creditCollections"
                            label="Has collections"
                            checked={formData.creditHistory?.collections === true}
                            onCheckedChange={checked =>
                              handleNestedChange('creditHistory', 'collections', checked === true)
                            }
                          />
                        </FieldContent>
                      </Field>
                      {formData.creditHistory?.collections && (
                        <Field data-invalid={!!getFieldError('collectionsAmount')}>
                          <FieldLabel htmlFor="field-collectionsAmount">
                            Collections Amount ($)
                          </FieldLabel>
                          <FieldContent>
                            <Input
                              id="field-collectionsAmount"
                              type="number"
                              min={0}
                              step={0.01}
                              value={formData.creditHistory?.collectionsAmount || ''}
                              onChange={e =>
                                handleNestedChange(
                                  'creditHistory',
                                  'collectionsAmount',
                                  parseFloat(e.target.value) || 0
                                )
                              }
                            />
                            <FieldError>{getFieldError('collectionsAmount')}</FieldError>
                          </FieldContent>
                        </Field>
                      )}
                      <Field>
                        <FieldContent>
                          <FormCheckbox
                            id="field-creditBankruptcy"
                            label="Bankruptcy history"
                            checked={formData.creditHistory?.bankruptcy === true}
                            onCheckedChange={checked =>
                              handleNestedChange('creditHistory', 'bankruptcy', checked === true)
                            }
                          />
                        </FieldContent>
                      </Field>
                      {formData.creditHistory?.bankruptcy && (
                        <>
                          <Field data-invalid={!!getFieldError('bankruptcyType')}>
                            <FieldLabel htmlFor="field-bankruptcyType">Bankruptcy Type</FieldLabel>
                            <FieldContent>
                              <FormSelect
                                id="field-bankruptcyType"
                                value={formData.creditHistory?.bankruptcyType || ''}
                                onChange={value =>
                                  handleNestedChange('creditHistory', 'bankruptcyType', value)
                                }
                                options={[
                                  { value: '', label: '-- Select Type --' },
                                  { value: 'CHAPTER_7', label: 'Chapter 7' },
                                  { value: 'CHAPTER_13', label: 'Chapter 13' },
                                ]}
                                placeholder="Select bankruptcy type"
                                error={getFieldError('bankruptcyType')}
                              />
                            </FieldContent>
                          </Field>
                          <Field data-invalid={!!getFieldError('bankruptcyDischargeDate')}>
                            <FieldLabel htmlFor="field-bankruptcyDischargeDate">
                              Discharge Date
                            </FieldLabel>
                            <FieldContent>
                              <Input
                                id="field-bankruptcyDischargeDate"
                                type="date"
                                value={formData.creditHistory?.bankruptcyDischargeDate || ''}
                                onChange={e =>
                                  handleNestedChange(
                                    'creditHistory',
                                    'bankruptcyDischargeDate',
                                    e.target.value
                                  )
                                }
                              />
                              <FieldError>{getFieldError('bankruptcyDischargeDate')}</FieldError>
                            </FieldContent>
                          </Field>
                        </>
                      )}
                      <Field>
                        <FieldContent>
                          <FormCheckbox
                            id="field-medicalDebt"
                            label="Medical debt"
                            checked={formData.creditHistory?.medicalDebt === true}
                            onCheckedChange={checked =>
                              handleNestedChange('creditHistory', 'medicalDebt', checked === true)
                            }
                          />
                        </FieldContent>
                      </Field>
                      <Field>
                        <FieldContent>
                          <FormCheckbox
                            id="field-studentLoanDebt"
                            label="Student loan debt"
                            checked={formData.creditHistory?.studentLoanDebt === true}
                            onCheckedChange={checked =>
                              handleNestedChange(
                                'creditHistory',
                                'studentLoanDebt',
                                checked === true
                              )
                            }
                          />
                        </FieldContent>
                      </Field>
                    </FieldGroup>
                  </>
                )}
              </div>
            )}
          </div>

          {/* Section 3: Criminal Background */}
          <div className="accordion-item">
            <button
              type="button"
              className={`accordion-header ${openAccordion === 'criminal' ? 'open' : ''}`}
              onClick={() => toggleAccordion('criminal')}
            >
              <span>3. Criminal Background (Voluntary)</span>
              <span className="accordion-icon">{openAccordion === 'criminal' ? '▼' : '▶'}</span>
            </button>

            {openAccordion === 'criminal' && (
              <div className="accordion-content">
                <FieldGroup>
                  <Field>
                    <FieldContent>
                      <FormCheckbox
                        id="field-criminalDisclosed"
                        label="Client disclosed criminal background"
                        checked={formData.criminalBackground?.disclosed === true}
                        onCheckedChange={checked =>
                          handleNestedChange('criminalBackground', 'disclosed', checked === true)
                        }
                      />
                    </FieldContent>
                  </Field>
                </FieldGroup>

                {formData.criminalBackground?.disclosed && (
                  <>
                    <FieldGroup>
                      <Field>
                        <FieldTitle>Has criminal record?</FieldTitle>
                        <FieldContent>
                          <RadioGroup
                            value={
                              formData.criminalBackground?.hasRecord === true
                                ? 'yes'
                                : formData.criminalBackground?.hasRecord === false
                                ? 'no'
                                : ''
                            }
                            onValueChange={value =>
                              handleNestedChange('criminalBackground', 'hasRecord', value === 'yes')
                            }
                          >
                            <div className="flex flex-col gap-2">
                              <div className="flex items-center gap-2">
                                <RadioGroupItem value="yes" id="field-hasRecord-yes" />
                                <Label htmlFor="field-hasRecord-yes">Yes</Label>
                              </div>
                              <div className="flex items-center gap-2">
                                <RadioGroupItem value="no" id="field-hasRecord-no" />
                                <Label htmlFor="field-hasRecord-no">No</Label>
                              </div>
                            </div>
                          </RadioGroup>
                        </FieldContent>
                      </Field>
                    </FieldGroup>

                    {formData.criminalBackground?.hasRecord && (
                      <>
                        <FieldGroup>
                          <Field>
                            <FieldLabel htmlFor="field-mostRecentConvictionDate">
                              Most Recent Conviction Date
                            </FieldLabel>
                            <FieldContent>
                              <Input
                                id="field-mostRecentConvictionDate"
                                type="date"
                                value={formData.criminalBackground?.mostRecentConvictionDate || ''}
                                onChange={e =>
                                  handleNestedChange(
                                    'criminalBackground',
                                    'mostRecentConvictionDate',
                                    e.target.value
                                  )
                                }
                                max={new Date().toISOString().split('T')[0]}
                              />
                            </FieldContent>
                          </Field>
                        </FieldGroup>

                        <FieldGroup>
                          <Field>
                            <FieldTitle>On probation/parole?</FieldTitle>
                            <FieldContent>
                              <RadioGroup
                                value={
                                  formData.criminalBackground?.onSupervision === true
                                    ? 'yes'
                                    : formData.criminalBackground?.onSupervision === false
                                    ? 'no'
                                    : ''
                                }
                                onValueChange={value =>
                                  handleNestedChange(
                                    'criminalBackground',
                                    'onSupervision',
                                    value === 'yes'
                                  )
                                }
                              >
                                <div className="flex flex-col gap-2">
                                  <div className="flex items-center gap-2">
                                    <RadioGroupItem value="yes" id="field-onSupervision-yes" />
                                    <Label htmlFor="field-onSupervision-yes">Yes</Label>
                                  </div>
                                  <div className="flex items-center gap-2">
                                    <RadioGroupItem value="no" id="field-onSupervision-no" />
                                    <Label htmlFor="field-onSupervision-no">No</Label>
                                  </div>
                                </div>
                              </RadioGroup>
                            </FieldContent>
                          </Field>
                        </FieldGroup>

                        <FieldGroup>
                          <Field>
                            <FieldContent>
                              <FormCheckbox
                                id="field-registeredSexOffender"
                                label="Registered sex offender"
                                checked={formData.criminalBackground?.registeredSexOffender === true}
                                onCheckedChange={checked =>
                                  handleNestedChange(
                                    'criminalBackground',
                                    'registeredSexOffender',
                                    checked === true
                                  )
                                }
                              />
                            </FieldContent>
                          </Field>
                        </FieldGroup>

                        {formData.criminalBackground?.registeredSexOffender && (
                          <div className="alert alert-danger">
                            <span className="alert-icon">⚠️</span>
                            <span>
                              This significantly limits housing options. Specialized assistance
                              required.
                            </span>
                          </div>
                        )}

                        {getFieldWarning('criminalBackground') && (
                          <div className="alert alert-warning">
                            <span className="alert-icon">⚠️</span>
                            <span>{getFieldWarning('criminalBackground')}</span>
                          </div>
                        )}
                      </>
                    )}
                  </>
                )}
              </div>
            )}
          </div>

          {/* Section 4: Employment Status */}
          <div className="accordion-item">
            <button
              type="button"
              className={`accordion-header ${openAccordion === 'employment' ? 'open' : ''}`}
              onClick={() => toggleAccordion('employment')}
            >
              <span>4. Employment Status</span>
              <span className="accordion-icon">{openAccordion === 'employment' ? '▼' : '▶'}</span>
            </button>

            {openAccordion === 'employment' && (
              <div className="accordion-content">
                <FieldGroup>
                  <Field>
                    <FieldTitle>Currently employed?</FieldTitle>
                    <FieldContent>
                      <RadioGroup
                        value={
                          formData.employmentStatus?.currentlyEmployed === true
                            ? 'yes'
                            : formData.employmentStatus?.currentlyEmployed === false
                            ? 'no'
                            : ''
                        }
                        onValueChange={value =>
                          handleNestedChange('employmentStatus', 'currentlyEmployed', value === 'yes')
                        }
                      >
                        <div className="flex flex-col gap-2">
                          <div className="flex items-center gap-2">
                            <RadioGroupItem value="yes" id="field-currentlyEmployed-yes" />
                            <Label htmlFor="field-currentlyEmployed-yes">Yes</Label>
                          </div>
                          <div className="flex items-center gap-2">
                            <RadioGroupItem value="no" id="field-currentlyEmployed-no" />
                            <Label htmlFor="field-currentlyEmployed-no">No</Label>
                          </div>
                        </div>
                      </RadioGroup>
                    </FieldContent>
                  </Field>
                </FieldGroup>

                {formData.employmentStatus?.currentlyEmployed && (
                  <>
                    <FieldGroup>
                      <Field data-invalid={!!getFieldError('employmentType')}>
                        <FieldLabel htmlFor="field-employmentType">Employment Type</FieldLabel>
                        <FieldContent>
                          <FormSelect
                            id="field-employmentType"
                            value={formData.employmentStatus?.employmentType || ''}
                            onChange={value =>
                              handleNestedChange('employmentStatus', 'employmentType', value)
                            }
                            options={[
                              { value: '', label: '-- Select Type --' },
                              { value: 'FULL_TIME', label: 'Full-time' },
                              { value: 'PART_TIME', label: 'Part-time' },
                              { value: 'SEASONAL', label: 'Seasonal' },
                              { value: 'TEMPORARY', label: 'Temporary' },
                              { value: 'SELF_EMPLOYED', label: 'Self-employed' },
                              { value: 'GIG_WORK', label: 'Gig work' },
                            ]}
                            placeholder="Select employment type"
                            error={getFieldError('employmentType')}
                          />
                        </FieldContent>
                      </Field>
                      <Field data-invalid={!!getFieldError('employer')}>
                        <FieldLabel htmlFor="field-employer">Employer Name</FieldLabel>
                        <FieldContent>
                          <Input
                            id="field-employer"
                            value={formData.employmentStatus?.employer || ''}
                            onChange={e =>
                              handleNestedChange('employmentStatus', 'employer', e.target.value)
                            }
                          />
                          <FieldError>{getFieldError('employer')}</FieldError>
                        </FieldContent>
                      </Field>
                      <Field>
                        <FieldLabel htmlFor="field-jobTitle">Job Title</FieldLabel>
                        <FieldContent>
                          <Input
                            id="field-jobTitle"
                            value={formData.employmentStatus?.jobTitle || ''}
                            onChange={e =>
                              handleNestedChange('employmentStatus', 'jobTitle', e.target.value)
                            }
                          />
                        </FieldContent>
                      </Field>
                    </FieldGroup>

                    <FieldGroup>
                      <Field>
                        <FieldLabel htmlFor="field-hoursPerWeek">Hours per Week</FieldLabel>
                        <FieldContent>
                          <Input
                            id="field-hoursPerWeek"
                            type="number"
                            min={0}
                            max={168}
                            value={formData.employmentStatus?.hoursPerWeek || ''}
                            onChange={e =>
                              handleNestedChange(
                                'employmentStatus',
                                'hoursPerWeek',
                                parseInt(e.target.value, 10) || 0
                              )
                            }
                          />
                        </FieldContent>
                      </Field>
                      <Field>
                        <FieldLabel htmlFor="field-hourlyWage">Hourly Wage ($)</FieldLabel>
                        <FieldContent>
                          <Input
                            id="field-hourlyWage"
                            type="number"
                            min={0}
                            step={0.01}
                            value={formData.employmentStatus?.hourlyWage || ''}
                            onChange={e =>
                              handleNestedChange(
                                'employmentStatus',
                                'hourlyWage',
                                parseFloat(e.target.value) || 0
                              )
                            }
                          />
                        </FieldContent>
                      </Field>
                    </FieldGroup>
                  </>
                )}

                <FieldGroup data-slot="checkbox-group">
                  <Field>
                    <FieldContent>
                      <FormCheckbox
                        id="field-lookingForWork"
                        label="Looking for work"
                        checked={formData.employmentStatus?.lookingForWork === true}
                        onCheckedChange={checked =>
                          handleNestedChange('employmentStatus', 'lookingForWork', checked === true)
                        }
                      />
                    </FieldContent>
                  </Field>
                  <Field>
                    <FieldContent>
                      <FormCheckbox
                        id="field-hasResume"
                        label="Has resume"
                        checked={formData.employmentStatus?.hasResume === true}
                        onCheckedChange={checked =>
                          handleNestedChange('employmentStatus', 'hasResume', checked === true)
                        }
                      />
                    </FieldContent>
                  </Field>
                  <Field>
                    <FieldContent>
                      <FormCheckbox
                        id="field-interestedInTraining"
                        label="Interested in job training"
                        checked={formData.employmentStatus?.interestedInTraining === true}
                        onCheckedChange={checked =>
                          handleNestedChange(
                            'employmentStatus',
                            'interestedInTraining',
                            checked === true
                          )
                        }
                      />
                    </FieldContent>
                  </Field>
                </FieldGroup>
              </div>
            )}
          </div>

          {/* Section 5: Support Network */}
          <div className="accordion-item">
            <button
              type="button"
              className={`accordion-header ${openAccordion === 'support' ? 'open' : ''}`}
              onClick={() => toggleAccordion('support')}
            >
              <span>5. Support Network</span>
              <span className="accordion-icon">{openAccordion === 'support' ? '▼' : '▶'}</span>
            </button>

            {openAccordion === 'support' && (
              <div className="accordion-content">
                <FieldGroup>
                  <Field>
                    <FieldTitle>Has support network?</FieldTitle>
                    <FieldContent>
                      <RadioGroup
                        value={
                          formData.supportNetwork?.hasSupportNetwork === true
                            ? 'yes'
                            : formData.supportNetwork?.hasSupportNetwork === false
                            ? 'no'
                            : ''
                        }
                        onValueChange={value =>
                          handleNestedChange('supportNetwork', 'hasSupportNetwork', value === 'yes')
                        }
                      >
                        <div className="flex flex-col gap-2">
                          <div className="flex items-center gap-2">
                            <RadioGroupItem value="yes" id="field-hasSupportNetwork-yes" />
                            <Label htmlFor="field-hasSupportNetwork-yes">Yes</Label>
                          </div>
                          <div className="flex items-center gap-2">
                            <RadioGroupItem value="no" id="field-hasSupportNetwork-no" />
                            <Label htmlFor="field-hasSupportNetwork-no">No</Label>
                          </div>
                        </div>
                      </RadioGroup>
                    </FieldContent>
                  </Field>
                </FieldGroup>

                {formData.supportNetwork?.hasSupportNetwork && (
                  <>
                    <FieldGroup>
                      <Field>
                        <FieldLabel htmlFor="field-numberOfReferences">
                          Number of References Available
                        </FieldLabel>
                        <FieldContent>
                          <Input
                            id="field-numberOfReferences"
                            type="number"
                            min={0}
                            value={formData.supportNetwork?.numberOfReferences || ''}
                            onChange={e =>
                              handleNestedChange(
                                'supportNetwork',
                                'numberOfReferences',
                                parseInt(e.target.value, 10) || 0
                              )
                            }
                          />
                        </FieldContent>
                      </Field>
                    </FieldGroup>

                    <FieldGroup data-slot="checkbox-group">
                      <Field>
                        <FieldContent>
                          <FormCheckbox
                            id="field-canProvideReferences"
                            label="Can provide personal references"
                            checked={formData.supportNetwork?.canProvideReferences === true}
                            onCheckedChange={checked =>
                              handleNestedChange(
                                'supportNetwork',
                                'canProvideReferences',
                                checked === true
                              )
                            }
                          />
                        </FieldContent>
                      </Field>
                      <Field>
                        <FieldContent>
                          <FormCheckbox
                            id="field-localNetwork"
                            label="Support network is local"
                            checked={formData.supportNetwork?.localNetwork === true}
                            onCheckedChange={checked =>
                              handleNestedChange('supportNetwork', 'localNetwork', checked === true)
                            }
                          />
                        </FieldContent>
                      </Field>
                      <Field>
                        <FieldContent>
                          <FormCheckbox
                            id="field-networkCanHouse"
                            label="Network can provide housing assistance"
                            checked={formData.supportNetwork?.networkCanHouse === true}
                            onCheckedChange={checked =>
                              handleNestedChange(
                                'supportNetwork',
                                'networkCanHouse',
                                checked === true
                              )
                            }
                          />
                        </FieldContent>
                      </Field>
                    </FieldGroup>
                  </>
                )}

                <FieldGroup>
                  <Field>
                    <FieldContent>
                      <FormCheckbox
                        id="field-isolated"
                        label="Isolated from support network"
                        checked={formData.supportNetwork?.isolated === true}
                        onCheckedChange={checked =>
                          handleNestedChange('supportNetwork', 'isolated', checked === true)
                        }
                      />
                    </FieldContent>
                  </Field>
                </FieldGroup>

                {formData.supportNetwork?.isolated && getFieldWarning('isolated') && (
                  <div className="alert alert-warning">
                    <span className="alert-icon">⚠️</span>
                    <span>{getFieldWarning('isolated')}</span>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Section 6: Transportation */}
          <div className="accordion-item">
            <button
              type="button"
              className={`accordion-header ${openAccordion === 'transportation' ? 'open' : ''}`}
              onClick={() => toggleAccordion('transportation')}
            >
              <span>6. Transportation</span>
              <span className="accordion-icon">{openAccordion === 'transportation' ? '▼' : '▶'}</span>
            </button>

            {openAccordion === 'transportation' && (
              <div className="accordion-content">
                <FieldGroup>
                  <Field>
                    <FieldTitle>Has reliable transportation?</FieldTitle>
                    <FieldContent>
                      <RadioGroup
                        value={
                          formData.transportation?.hasReliableTransportation === true
                            ? 'yes'
                            : formData.transportation?.hasReliableTransportation === false
                            ? 'no'
                            : ''
                        }
                        onValueChange={value =>
                          handleNestedChange(
                            'transportation',
                            'hasReliableTransportation',
                            value === 'yes'
                          )
                        }
                      >
                        <div className="flex flex-col gap-2">
                          <div className="flex items-center gap-2">
                            <RadioGroupItem value="yes" id="field-hasReliableTransportation-yes" />
                            <Label htmlFor="field-hasReliableTransportation-yes">Yes</Label>
                          </div>
                          <div className="flex items-center gap-2">
                            <RadioGroupItem value="no" id="field-hasReliableTransportation-no" />
                            <Label htmlFor="field-hasReliableTransportation-no">No</Label>
                          </div>
                        </div>
                      </RadioGroup>
                    </FieldContent>
                  </Field>
                </FieldGroup>

                <FieldGroup data-slot="checkbox-group">
                  <Field>
                    <FieldContent>
                      <FormCheckbox
                        id="field-driversLicense"
                        label="Valid driver's license"
                        checked={formData.transportation?.driversLicense === true}
                        onCheckedChange={checked =>
                          handleNestedChange('transportation', 'driversLicense', checked === true)
                        }
                      />
                    </FieldContent>
                  </Field>

                  {!formData.transportation?.driversLicense && (
                    <Field>
                      <FieldContent>
                        <FormCheckbox
                          id="field-licenseSuspended"
                          label="License suspended"
                          checked={formData.transportation?.licenseSuspended === true}
                          onCheckedChange={checked =>
                            handleNestedChange(
                              'transportation',
                              'licenseSuspended',
                              checked === true
                            )
                          }
                        />
                      </FieldContent>
                    </Field>
                  )}

                  <Field>
                    <FieldContent>
                      <FormCheckbox
                        id="field-hasVehicle"
                        label="Owns vehicle"
                        checked={formData.transportation?.hasVehicle === true}
                        onCheckedChange={checked =>
                          handleNestedChange('transportation', 'hasVehicle', checked === true)
                        }
                      />
                    </FieldContent>
                  </Field>

                  {formData.transportation?.hasVehicle && (
                    <>
                      <Field>
                        <FieldContent>
                          <FormCheckbox
                            id="field-vehicleReliable"
                            label="Vehicle is reliable"
                            checked={formData.transportation?.vehicleReliable === true}
                            onCheckedChange={checked =>
                              handleNestedChange(
                                'transportation',
                                'vehicleReliable',
                                checked === true
                              )
                            }
                          />
                        </FieldContent>
                      </Field>
                      <Field>
                        <FieldContent>
                          <FormCheckbox
                            id="field-vehicleInsurance"
                            label="Has vehicle insurance"
                            checked={formData.transportation?.vehicleInsurance === true}
                            onCheckedChange={checked =>
                              handleNestedChange(
                                'transportation',
                                'vehicleInsurance',
                                checked === true
                              )
                            }
                          />
                        </FieldContent>
                      </Field>
                    </>
                  )}

                  <Field>
                    <FieldContent>
                      <FormCheckbox
                        id="field-publicTransitAccess"
                        label="Access to public transit"
                        checked={formData.transportation?.publicTransitAccess === true}
                        onCheckedChange={checked =>
                          handleNestedChange(
                            'transportation',
                            'publicTransitAccess',
                            checked === true
                          )
                        }
                      />
                    </FieldContent>
                  </Field>
                </FieldGroup>

                <Field data-invalid={!!getFieldError('monthlyTransportCost')}>
                  <FieldLabel htmlFor="field-monthlyTransportCost">
                    Monthly Transportation Costs ($)
                  </FieldLabel>
                  <FieldContent>
                    <Input
                      id="field-monthlyTransportCost"
                      type="number"
                      min={0}
                      step={0.01}
                      value={formData.transportation?.monthlyTransportCost || ''}
                      onChange={e =>
                        handleNestedChange(
                          'transportation',
                          'monthlyTransportCost',
                          parseFloat(e.target.value) || 0
                        )
                      }
                    />
                    <FieldError>{getFieldError('monthlyTransportCost')}</FieldError>
                  </FieldContent>
                </Field>

                {!formData.transportation?.hasReliableTransportation &&
                  getFieldWarning('transportation') && (
                    <div className="alert alert-warning">
                      <span className="alert-icon">⚠️</span>
                      <span>{getFieldWarning('transportation')}</span>
                    </div>
                  )}
              </div>
            )}
          </div>
        </div>

        {/* Barrier Summary & Stability Plan */}
        <section className="form-section barrier-summary">
          <h3>Barrier Summary & Housing Stability Plan</h3>

          <div className="severity-badge-container">
            <div
              className="severity-badge"
              style={{ backgroundColor: currentSeverity.color, color: '#ffffff' }}
            >
              <span className="severity-label">Barrier Severity</span>
              <span className="severity-value">{currentSeverity.label}</span>
              <span className="severity-description">{currentSeverity.description}</span>
            </div>
          </div>

          {formData.stabilityPlan && (
            <div className="stability-plan">
              <FieldSet>
                <FieldLegend>Recommended Interventions</FieldLegend>
                <FieldGroup data-slot="checkbox-group">
                  <Field>
                    <FieldContent>
                      <FormCheckbox
                        id="field-needsEmploymentSupport"
                        label="Employment support needed"
                        checked={formData.stabilityPlan.needsEmploymentSupport === true}
                        disabled
                      />
                    </FieldContent>
                  </Field>
                  <Field>
                    <FieldContent>
                      <FormCheckbox
                        id="field-needsLandlordMediation"
                        label="Landlord mediation needed"
                        checked={formData.stabilityPlan.needsLandlordMediation === true}
                        disabled
                      />
                    </FieldContent>
                  </Field>
                  <Field>
                    <FieldContent>
                      <FormCheckbox
                        id="field-needsCreditRepair"
                        label="Credit repair assistance needed"
                        checked={formData.stabilityPlan.needsCreditRepair === true}
                        disabled
                      />
                    </FieldContent>
                  </Field>
                  <Field>
                    <FieldContent>
                      <FormCheckbox
                        id="field-needsLegalAdvocacy"
                        label="Legal advocacy needed"
                        checked={formData.stabilityPlan.needsLegalAdvocacy === true}
                        disabled
                      />
                    </FieldContent>
                  </Field>
                  <Field>
                    <FieldContent>
                      <FormCheckbox
                        id="field-needsFinancialLiteracy"
                        label="Financial literacy training needed"
                        checked={formData.stabilityPlan.needsFinancialLiteracy === true}
                        disabled
                      />
                    </FieldContent>
                  </Field>
                  <Field>
                    <FieldContent>
                      <FormCheckbox
                        id="field-needsTransportation"
                        label="Transportation assistance needed"
                        checked={formData.stabilityPlan.needsTransportation === true}
                        disabled
                      />
                    </FieldContent>
                  </Field>
                </FieldGroup>

                {formData.stabilityPlan.estimatedTimeline && (
                  <Field>
                    <FieldTitle>Estimated Timeline to Stability</FieldTitle>
                    <FieldContent>
                      <span>{formData.stabilityPlan.estimatedTimeline.replace(/_/g, ' ')}</span>
                    </FieldContent>
                  </Field>
                )}
              </FieldSet>
            </div>
          )}
        </section>

        {/* Form Actions */}
        <div className="form-actions">
          <button type="button" onClick={onBack} className="btn btn-secondary">
            Back to Step 4
          </button>
          <button type="submit" className="btn btn-primary">
            Continue to Step 6
          </button>
        </div>
      </form>

      {/* Validation Summary */}
      {validationErrors.length > 0 && (
        <div className="validation-summary error-summary" role="alert">
          <h4>Please correct the following errors:</h4>
          <ul>
            {validationErrors.map((error, index) => (
              <li key={index}>
                <strong>{error.field}:</strong> {error.message}
              </li>
            ))}
          </ul>
        </div>
      )}

      {validationWarnings.length > 0 && (
        <div className="validation-summary warning-summary" role="alert">
          <h4>Warnings:</h4>
          <ul>
            {validationWarnings.map((warning, index) => (
              <li key={index}>
                <strong>{warning.field}:</strong> {warning.message}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
};

/**
 * Step 4: Eligibility & Program Match
 *
 * Determines homeless status per HUD definition and matches to appropriate programs.
 * Implements HUD 24 CFR 578.3 homeless categories.
 */

import React, { useState, useEffect, useMemo } from 'react';
import {
  Button,
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
import type { EligibilityData, ValidationError, ProgramSelection } from '../utils/types';
import { validateStep4 } from '../lib/validation';
import {
  HOMELESS_CATEGORIES,
  PRIOR_LIVING_SITUATIONS,
  LENGTH_OF_STAY,
  INCOME_SOURCES,
  HOUSEHOLD_TYPES,
  determineEligibility,
  isChronicallyHomeless,
} from '../index';

type ProgramTypeOption = ProgramSelection['programType'];

interface ProgramCatalogEntry {
  id: string;
  name: string;
  programType: ProgramTypeOption;
  fundingSource: string;
  availableSlots: number;
  dailyRate: number;
  location: string;
}

const PROGRAM_TYPE_LABELS: Record<ProgramTypeOption, string> = {
  TH: 'Transitional Housing (TH)',
  RRH: 'Rapid Re-Housing (RRH)',
  PSH: 'Permanent Supportive Housing (PSH)',
  ES: 'Emergency Shelter (ES)',
  SSO: 'Services Only (SSO)',
  OTHER: 'Other Program Type',
};

const PROGRAM_CATALOG: ProgramCatalogEntry[] = [
  {
    id: 'RRH-001',
    name: 'Rapid Re-Housing – Downtown Bridge',
    programType: 'RRH',
    fundingSource: 'HUD CoC RRH',
    availableSlots: 4,
    dailyRate: 42,
    location: 'Downtown',
  },
  {
    id: 'RRH-002',
    name: 'Families First Rapid Re-Housing',
    programType: 'RRH',
    fundingSource: 'ESG RRH',
    availableSlots: 6,
    dailyRate: 39,
    location: 'Eastside',
  },
  {
    id: 'TH-001',
    name: 'Transitional Housing – SafeStart',
    programType: 'TH',
    fundingSource: 'HUD CoC TH',
    availableSlots: 2,
    dailyRate: 55,
    location: 'North Campus',
  },
  {
    id: 'PSH-001',
    name: 'PSH – Keys to Home',
    programType: 'PSH',
    fundingSource: 'HUD CoC PSH',
    availableSlots: 3,
    dailyRate: 68,
    location: 'West End',
  },
  {
    id: 'ES-001',
    name: 'Emergency Shelter – Haven House',
    programType: 'ES',
    fundingSource: 'City ESG ES',
    availableSlots: 12,
    dailyRate: 28,
    location: 'Central Intake',
  },
  {
    id: 'SSO-001',
    name: 'Client Stabilization – Services Only',
    programType: 'SSO',
    fundingSource: 'HUD CoC SSO',
    availableSlots: 10,
    dailyRate: 22,
    location: 'Community Hub',
  },
  {
    id: 'OTHER-001',
    name: 'Housing Navigation – Flexible Fund',
    programType: 'OTHER',
    fundingSource: 'Private Foundation',
    availableSlots: 5,
    dailyRate: 18,
    location: 'Virtual / Field Based',
  },
];

const PROGRAM_TYPE_FILTER_OPTIONS: Array<{ value: ProgramTypeOption | 'ALL'; label: string }> = [
  { value: 'ALL', label: 'Show all program types' },
  { value: 'RRH', label: PROGRAM_TYPE_LABELS.RRH },
  { value: 'TH', label: PROGRAM_TYPE_LABELS.TH },
  { value: 'PSH', label: PROGRAM_TYPE_LABELS.PSH },
  { value: 'ES', label: PROGRAM_TYPE_LABELS.ES },
  { value: 'SSO', label: PROGRAM_TYPE_LABELS.SSO },
  { value: 'OTHER', label: PROGRAM_TYPE_LABELS.OTHER },
];

const DEFAULT_SELECTED_BY = 'Eligibility Specialist';

interface Step4Props {
  data: Partial<EligibilityData>;
  errors: ValidationError[];
  warnings: ValidationError[];
  onChange: (updates: Partial<EligibilityData>) => void;
  onComplete: (data: EligibilityData) => void;
  onBack: () => void;
}

export const Step4_EligibilityMatch: React.FC<Step4Props> = ({
  data,
  errors,
  warnings,
  onChange,
  onComplete,
  onBack,
}) => {
  const [formData, setFormData] = useState<Partial<EligibilityData>>({
    homelessStatus: {
      currentlyHomeless: false,
      homelessCategory: 'NOT_HOMELESS',
      priorLivingSituation: '',
      lengthOfStay: 'UNKNOWN',
      dateHomelessBeganApproximate: false,
      ...data.homelessStatus,
    },
    income: {
      hasIncome: false,
      verificationProvided: false,
      belowAreaMedian: null,
      ...data.income,
    },
    householdComposition: {
      adults: 1,
      children: 0,
      totalSize: 1,
      householdType: 'SINGLE_ADULT',
      clientIsHoH: true,
      ...data.householdComposition,
    },
    citizenship: {
      documentationRequired: false,
      waiverApplies: false,
      ...data.citizenship,
    },
    eligibilityResults: data.eligibilityResults || {
      eligibleForTH: false,
      eligibleForRRH: false,
      eligibleForPSH: false,
      eligibleForOther: [],
    },
    ...data,
  });

  const [validationErrors, setValidationErrors] = useState<ValidationError[]>(errors);
  const [validationWarnings, setValidationWarnings] = useState<ValidationError[]>(warnings);
  const [isCalculatingEligibility, setIsCalculatingEligibility] = useState(false);
  const recommendedProgramType = formData.eligibilityResults?.recommendedProgramId as
    | ProgramTypeOption
    | undefined;
  const [programTypeFilter, setProgramTypeFilter] = useState<ProgramTypeOption | 'ALL'>(
    formData.selectedProgram?.programType || recommendedProgramType || 'ALL',
  );

  useEffect(() => {
    setFormData(prev => ({ ...prev, ...data }));
  }, [data]);

  useEffect(() => {
    setValidationErrors(errors);
    setValidationWarnings(warnings);
  }, [errors, warnings]);

  useEffect(() => {
    if (formData.selectedProgram?.programType) {
      setProgramTypeFilter(formData.selectedProgram.programType);
      return;
    }

    if (
      recommendedProgramType &&
      programTypeFilter === 'ALL' &&
      PROGRAM_TYPE_LABELS[recommendedProgramType]
    ) {
      setProgramTypeFilter(recommendedProgramType);
    }
  }, [formData.selectedProgram?.programType, recommendedProgramType, programTypeFilter]);

  // Auto-calculate household size
  useEffect(() => {
    if (formData.householdComposition) {
      const totalSize =
        (formData.householdComposition.adults || 0) +
        (formData.householdComposition.children || 0);

      if (totalSize !== formData.householdComposition.totalSize) {
        handleNestedChange('householdComposition', 'totalSize', totalSize);
      }
    }
  }, [
    formData.householdComposition?.adults,
    formData.householdComposition?.children,
  ]);

  // Auto-calculate annual income
  useEffect(() => {
    if (formData.income?.monthlyIncome) {
      const annualIncome = formData.income.monthlyIncome * 12;
      if (annualIncome !== formData.income.annualIncome) {
        handleNestedChange('income', 'annualIncome', annualIncome);
      }
    }
  }, [formData.income?.monthlyIncome]);

  const handleChange = (field: keyof EligibilityData, value: any) => {
    const updates = { [field]: value };
    setFormData(prev => ({ ...prev, ...updates }));
    onChange(updates);
  };

  const handleNestedChange = (parent: keyof EligibilityData, field: string, value: any) => {
    const updates = {
      [parent]: {
        ...(formData[parent] as any),
        [field]: value,
      },
    };
    setFormData(prev => ({ ...prev, ...updates }));
    onChange(updates);
  };

  const handleCalculateEligibility = async () => {
    setIsCalculatingEligibility(true);

    // Simulate API call (replace with actual API call)
    await new Promise(resolve => setTimeout(resolve, 1000));

    // Use business logic to determine eligibility
    const eligibilityResults = determineEligibility(formData as EligibilityData);

    setFormData(prev => ({
      ...prev,
      eligibilityResults,
    }));

    onChange({ eligibilityResults });

    setIsCalculatingEligibility(false);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    const result = validateStep4(formData as EligibilityData);
    setValidationErrors(result.errors);
    setValidationWarnings(result.warnings);

    if (result.isValid) {
      onComplete(formData as EligibilityData);
    } else {
      const firstErrorField = result.errors[0]?.field;
      if (firstErrorField) {
        document.getElementById(`field-${firstErrorField}`)?.focus();
      }
    }
  };

  const handleProgramTypeChange = (value: string) => {
    const nextValue = (value as ProgramTypeOption | 'ALL') || 'ALL';
    setProgramTypeFilter(nextValue);

    if (
      formData.selectedProgram &&
      nextValue !== 'ALL' &&
      formData.selectedProgram.programType !== nextValue
    ) {
      handleChange('selectedProgram', undefined);
    }
  };

  const handleProgramSelect = (programId: string) => {
    if (!programId) {
      handleChange('selectedProgram', undefined);
      return;
    }

    const program = PROGRAM_CATALOG.find(entry => entry.id === programId);
    if (!program) {
      handleChange('selectedProgram', undefined);
      return;
    }

    const selection: ProgramSelection = {
      programId: program.id,
      programName: program.name,
      programType: program.programType,
      fundingSource: program.fundingSource,
      availableSlots: program.availableSlots,
      expectedEntryDate: formData.selectedProgram?.expectedEntryDate,
      selectedBy: formData.selectedProgram?.selectedBy || DEFAULT_SELECTED_BY,
      selectionDate: new Date().toISOString(),
      dailyRate: program.dailyRate,
      location: program.location,
    };

    handleChange('selectedProgram', selection);
  };

  const handleExpectedEntryDateChange = (value: string) => {
    if (!formData.selectedProgram) {
      return;
    }

    handleChange('selectedProgram', {
      ...formData.selectedProgram,
      expectedEntryDate: value || undefined,
    });
  };

  const handleOverrideReasonChange = (value: string) => {
    handleChange('overrideReason', value);
  };

  const getFieldError = (fieldName: string): string | undefined => {
    return validationErrors.find(e => e.field === fieldName)?.message;
  };

  const getFieldWarning = (fieldName: string): string | undefined => {
    return validationWarnings.find(w => w.field === fieldName)?.message;
  };

  const currentlyHomelessError = getFieldError('currentlyHomeless');
  const homelessCategoryError = getFieldError('homelessCategory');
  const priorLivingSituationError = getFieldError('priorLivingSituation');
  const lengthOfStayError = getFieldError('lengthOfStay');
  const hasIncomeError = getFieldError('hasIncome');
  const monthlyIncomeError = getFieldError('monthlyIncome');
  const incomeSourcesError = getFieldError('incomeSources');
  const adultsError = getFieldError('adults');
  const childrenError = getFieldError('children');
  const householdTypeError = getFieldError('householdType');
  const citizenshipError = getFieldError('citizenship');
  const selectedProgramError = getFieldError('selectedProgram');
  const eligibilityResultsWarning = getFieldWarning('eligibilityResults');

  const currentlyHomelessValue =
    formData.homelessStatus?.currentlyHomeless === true
      ? 'yes'
      : formData.homelessStatus?.currentlyHomeless === false
      ? 'no'
      : '';

  const hasIncomeValue =
    formData.income?.hasIncome === true ? 'yes' : formData.income?.hasIncome === false ? 'no' : '';

  const clientIsHoHValue =
    formData.householdComposition?.clientIsHoH === true
      ? 'yes'
      : formData.householdComposition?.clientIsHoH === false
      ? 'no'
      : '';

  const isCurrentlyHomeless = formData.homelessStatus?.currentlyHomeless === true;
  const hasIncome = formData.income?.hasIncome === true;
  const documentationRequired = formData.citizenship?.documentationRequired === true;
  const documentationProvided = formData.citizenship?.documentationProvided === true;
  const waiverApplies = formData.citizenship?.waiverApplies === true;
  const filteredPrograms = useMemo(
    () =>
      PROGRAM_CATALOG.filter(program =>
        programTypeFilter === 'ALL' ? true : program.programType === programTypeFilter,
      ),
    [programTypeFilter],
  );
  const selectedProgramId = formData.selectedProgram?.programId ?? '';
  const recommendedProgramTypeLabel = recommendedProgramType
    ? PROGRAM_TYPE_LABELS[recommendedProgramType] ?? recommendedProgramType
    : undefined;
  const isUsingRecommendedType =
    !!formData.selectedProgram &&
    !!recommendedProgramType &&
    formData.selectedProgram.programType === recommendedProgramType;

  return (
    <div className="intake-step step-4">
      <div className="step-header">
        <h2>Step 4: Eligibility & Program Match</h2>
        <p className="step-description">
          Determine homeless status per HUD definition and match to appropriate housing programs.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="intake-form">
        {/* Section 1: Homeless Status */}
        <FieldSet>
          <FieldLegend>Homeless Status (HUD 24 CFR 578.3)</FieldLegend>
          <FieldGroup>
            <Field data-invalid={!!currentlyHomelessError}>
              <FieldTitle>
                Is client currently homeless? <span className="text-destructive">*</span>
              </FieldTitle>
              <FieldContent>
                <RadioGroup
                  value={currentlyHomelessValue}
                  onValueChange={value =>
                    handleNestedChange('homelessStatus', 'currentlyHomeless', value === 'yes')
                  }
                >
                  <div className="flex flex-col gap-2">
                    <div className="flex items-center gap-2">
                      <RadioGroupItem value="yes" id="field-currentlyHomeless-yes" />
                      <Label htmlFor="field-currentlyHomeless-yes">Yes</Label>
                    </div>
                    <div className="flex items-center gap-2">
                      <RadioGroupItem value="no" id="field-currentlyHomeless-no" />
                      <Label htmlFor="field-currentlyHomeless-no">No</Label>
                    </div>
                  </div>
                </RadioGroup>
                <FieldError>{currentlyHomelessError}</FieldError>
              </FieldContent>
            </Field>
          </FieldGroup>

          {isCurrentlyHomeless && (
            <>
              <FieldGroup>
                <Field data-invalid={!!homelessCategoryError}>
                  <FieldLabel htmlFor="field-homelessCategory">
                    HUD Homeless Category <span className="text-destructive">*</span>
                  </FieldLabel>
                  <FieldContent>
                    <FormSelect
                      id="field-homelessCategory"
                      value={formData.homelessStatus?.homelessCategory || ''}
                      onChange={value => handleNestedChange('homelessStatus', 'homelessCategory', value)}
                      options={Object.entries(HOMELESS_CATEGORIES).map(([key, label]) => ({
                        value: key,
                        label,
                      }))}
                      placeholder="Select category"
                      error={homelessCategoryError}
                    />
                  </FieldContent>
                </Field>

                <Field data-invalid={!!priorLivingSituationError}>
                  <FieldLabel htmlFor="field-priorLivingSituation">
                    Prior Living Situation <span className="text-destructive">*</span>
                  </FieldLabel>
                  <FieldContent>
                    <FormSelect
                      id="field-priorLivingSituation"
                      value={formData.homelessStatus?.priorLivingSituation || ''}
                      onChange={value =>
                        handleNestedChange('homelessStatus', 'priorLivingSituation', value)
                      }
                      options={Object.entries(PRIOR_LIVING_SITUATIONS).map(([key, value]) => ({
                        value: key,
                        label: value.label,
                      }))}
                      placeholder="Select prior living situation"
                      error={priorLivingSituationError}
                    />
                  </FieldContent>
                </Field>
              </FieldGroup>

              <FieldGroup>
                <Field data-invalid={!!lengthOfStayError}>
                  <FieldLabel htmlFor="field-lengthOfStay">
                    Length of Stay in Prior Situation <span className="text-destructive">*</span>
                  </FieldLabel>
                  <FieldContent>
                    <FormSelect
                      id="field-lengthOfStay"
                      value={formData.homelessStatus?.lengthOfStay || ''}
                      onChange={value => handleNestedChange('homelessStatus', 'lengthOfStay', value)}
                      options={Object.entries(LENGTH_OF_STAY).map(([key, label]) => ({
                        value: key,
                        label,
                      }))}
                      placeholder="Select length of stay"
                      error={lengthOfStayError}
                    />
                  </FieldContent>
                </Field>
              </FieldGroup>

              <FieldGroup>
                <Field>
                  <FieldLabel htmlFor="field-dateHomelessBegan">Date Homelessness Began</FieldLabel>
                  <FieldContent>
                    <Input
                      id="field-dateHomelessBegan"
                      type="date"
                      value={formData.homelessStatus?.dateHomelessBegan || ''}
                      onChange={e =>
                        handleNestedChange('homelessStatus', 'dateHomelessBegan', e.target.value)
                      }
                      max={new Date().toISOString().split('T')[0]}
                    />
                  </FieldContent>
                </Field>
                <Field>
                  <FieldContent>
                    <FormCheckbox
                      id="field-dateHomelessBeganApproximate"
                      label="Date is approximate"
                      checked={formData.homelessStatus?.dateHomelessBeganApproximate === true}
                      onCheckedChange={checked =>
                        handleNestedChange(
                          'homelessStatus',
                          'dateHomelessBeganApproximate',
                          checked === true
                        )
                      }
                    />
                  </FieldContent>
                </Field>
              </FieldGroup>

              <FieldGroup>
                <Field>
                  <FieldLabel htmlFor="field-timesHomelessPast3Years">
                    Times Homeless in Past 3 Years
                  </FieldLabel>
                  <FieldContent>
                    <Input
                      id="field-timesHomelessPast3Years"
                      type="number"
                      min={0}
                      value={formData.homelessStatus?.timesHomelessPast3Years ?? ''}
                      onChange={e =>
                        handleNestedChange(
                          'homelessStatus',
                          'timesHomelessPast3Years',
                          parseInt(e.target.value, 10) || 0
                        )
                      }
                    />
                  </FieldContent>
                </Field>
                <Field>
                  <FieldLabel htmlFor="field-monthsHomelessPast3Years">
                    Total Months Homeless in Past 3 Years
                  </FieldLabel>
                  <FieldContent>
                    <Input
                      id="field-monthsHomelessPast3Years"
                      type="number"
                      min={0}
                      max={36}
                      value={formData.homelessStatus?.monthsHomelessPast3Years ?? ''}
                      onChange={e =>
                        handleNestedChange(
                          'homelessStatus',
                          'monthsHomelessPast3Years',
                          parseInt(e.target.value, 10) || 0
                        )
                      }
                    />
                  </FieldContent>
                </Field>
              </FieldGroup>

              {isChronicallyHomeless(formData.homelessStatus) && (
                <div className="alert alert-info">
                  <span className="alert-icon">??</span>
                  <span>
                    <strong>Chronically Homeless:</strong> Client meets HUD definition of chronic
                    homelessness. Eligible for PSH programs.
                  </span>
                </div>
              )}
            </>
          )}
        </FieldSet>
        {/* Section 2: Income Information */}
        <FieldSet>
          <FieldLegend>Income Information</FieldLegend>
          <FieldGroup>
            <Field data-invalid={!!hasIncomeError}>
              <FieldTitle>
                Does client have income? <span className="text-destructive">*</span>
              </FieldTitle>
              <FieldContent>
                <RadioGroup
                  value={hasIncomeValue}
                  onValueChange={value => handleNestedChange('income', 'hasIncome', value === 'yes')}
                >
                  <div className="flex flex-col gap-2">
                    <div className="flex items-center gap-2">
                      <RadioGroupItem value="yes" id="field-hasIncome-yes" />
                      <Label htmlFor="field-hasIncome-yes">Yes</Label>
                    </div>
                    <div className="flex items-center gap-2">
                      <RadioGroupItem value="no" id="field-hasIncome-no" />
                      <Label htmlFor="field-hasIncome-no">No</Label>
                    </div>
                  </div>
                </RadioGroup>
                <FieldError>{hasIncomeError}</FieldError>
              </FieldContent>
            </Field>
          </FieldGroup>

          {hasIncome && (
            <>
              <FieldGroup>
                <Field data-invalid={!!monthlyIncomeError}>
                  <FieldLabel htmlFor="field-monthlyIncome">
                    Monthly Income <span className="text-destructive">*</span>
                  </FieldLabel>
                  <FieldContent>
                    <div className="input-with-prefix">
                      <span className="input-prefix">$</span>
                      <Input
                        id="field-monthlyIncome"
                        type="number"
                        min={0}
                        step={0.01}
                        value={formData.income?.monthlyIncome ?? ''}
                        onChange={e =>
                          handleNestedChange(
                            'income',
                            'monthlyIncome',
                            parseFloat(e.target.value) || 0
                          )
                        }
                        aria-invalid={!!monthlyIncomeError}
                      />
                    </div>
                    <FieldError>{monthlyIncomeError}</FieldError>
                  </FieldContent>
                </Field>

                <Field>
                  <FieldTitle>Annual Income (auto-calculated)</FieldTitle>
                  <FieldContent>
                    <div className="readonly-value">
                      ${(formData.income?.annualIncome || 0).toLocaleString()}
                    </div>
                  </FieldContent>
                </Field>
              </FieldGroup>

              <FieldGroup>
                <Field data-invalid={!!incomeSourcesError}>
                  <FieldTitle>
                    Income Sources <span className="text-destructive">*</span>
                  </FieldTitle>
                  <FieldContent>
                    <div className="flex flex-col gap-2">
                      {Object.entries(INCOME_SOURCES).map(([key, label]) => {
                        const checkboxId = `field-incomeSource-${key.toLowerCase()}`;
                        const currentSources = formData.income?.incomeSources || [];
                        const isChecked = currentSources.includes(key as any);
                        return (
                          <FormCheckbox
                            key={key}
                            id={checkboxId}
                            label={label}
                            checked={isChecked}
                            onCheckedChange={checked => {
                              const sources = checked === true
                                ? [...currentSources, key]
                                : currentSources.filter(source => source !== key);
                              handleNestedChange('income', 'incomeSources', sources);
                            }}
                          />
                        );
                      })}
                    </div>
                    <FieldError>{incomeSourcesError}</FieldError>
                  </FieldContent>
                </Field>
              </FieldGroup>

              <FieldGroup>
                <Field>
                  <FieldContent>
                    <FormCheckbox
                      id="field-verificationProvided"
                      label="Verification documents provided"
                      checked={formData.income?.verificationProvided === true}
                      onCheckedChange={checked =>
                        handleNestedChange('income', 'verificationProvided', checked === true)
                      }
                    />
                  </FieldContent>
                </Field>

                {formData.income?.verificationProvided && (
                  <Field>
                    <FieldLabel htmlFor="field-verificationMethod">Verification Method</FieldLabel>
                    <FieldContent>
                      <Input
                        id="field-verificationMethod"
                        type="text"
                        value={formData.income?.verificationMethod || ''}
                        onChange={e =>
                          handleNestedChange('income', 'verificationMethod', e.target.value)
                        }
                        placeholder="e.g., Paystub, Benefits letter, Bank statement"
                      />
                    </FieldContent>
                  </Field>
                )}
              </FieldGroup>
            </>
          )}
        </FieldSet>
        {/* Section 3: Household Composition */}
        <FieldSet>
          <FieldLegend>Household Composition</FieldLegend>
          <FieldGroup>
            <Field data-invalid={!!adultsError}>
              <FieldLabel htmlFor="field-adults">
                Adults (18+) <span className="text-destructive">*</span>
              </FieldLabel>
              <FieldContent>
                <Input
                  id="field-adults"
                  type="number"
                  min={1}
                  value={formData.householdComposition?.adults ?? ''}
                  onChange={e =>
                    handleNestedChange(
                      'householdComposition',
                      'adults',
                      parseInt(e.target.value, 10) || 0
                    )
                  }
                  aria-invalid={!!adultsError}
                />
                <FieldError>{adultsError}</FieldError>
              </FieldContent>
            </Field>

            <Field data-invalid={!!childrenError}>
              <FieldLabel htmlFor="field-children">
                Children (&lt;18) <span className="text-destructive">*</span>
              </FieldLabel>
              <FieldContent>
                <Input
                  id="field-children"
                  type="number"
                  min={0}
                  value={formData.householdComposition?.children ?? ''}
                  onChange={e =>
                    handleNestedChange(
                      'householdComposition',
                      'children',
                      parseInt(e.target.value, 10) || 0
                    )
                  }
                  aria-invalid={!!childrenError}
                />
                <FieldError>{childrenError}</FieldError>
              </FieldContent>
            </Field>

            <Field>
              <FieldTitle>Total Household Size</FieldTitle>
              <FieldContent>
                <div className="readonly-value">
                  {formData.householdComposition?.totalSize || 0}
                </div>
              </FieldContent>
            </Field>
          </FieldGroup>

          <FieldGroup>
            <Field data-invalid={!!householdTypeError}>
              <FieldLabel htmlFor="field-householdType">
                Household Type <span className="text-destructive">*</span>
              </FieldLabel>
              <FieldContent>
                <FormSelect
                  id="field-householdType"
                  value={formData.householdComposition?.householdType || ''}
                  onChange={value => handleNestedChange('householdComposition', 'householdType', value)}
                  options={Object.entries(HOUSEHOLD_TYPES).map(([key, label]) => ({
                    value: key,
                    label,
                  }))}
                  placeholder="Select household type"
                  error={householdTypeError}
                />
              </FieldContent>
            </Field>
          </FieldGroup>

          <FieldGroup>
            <Field>
              <FieldTitle>Is client head of household?</FieldTitle>
              <FieldContent>
                <RadioGroup
                  value={clientIsHoHValue}
                  onValueChange={value =>
                    handleNestedChange('householdComposition', 'clientIsHoH', value === 'yes')
                  }
                >
                  <div className="flex flex-col gap-2">
                    <div className="flex items-center gap-2">
                      <RadioGroupItem value="yes" id="field-clientIsHoH-yes" />
                      <Label htmlFor="field-clientIsHoH-yes">Yes</Label>
                    </div>
                    <div className="flex items-center gap-2">
                      <RadioGroupItem value="no" id="field-clientIsHoH-no" />
                      <Label htmlFor="field-clientIsHoH-no">No</Label>
                    </div>
                  </div>
                </RadioGroup>
              </FieldContent>
            </Field>
          </FieldGroup>
        </FieldSet>
        {/* Section 4: Citizenship Documentation */}
        <FieldSet>
          <FieldLegend>Citizenship Documentation</FieldLegend>
          <FieldGroup>
            <Field>
              <FieldContent>
                <FormCheckbox
                  id="field-documentationRequired"
                  label="Citizenship documentation required by funding source"
                  checked={documentationRequired}
                  onCheckedChange={checked =>
                    handleNestedChange('citizenship', 'documentationRequired', checked === true)
                  }
                />
              </FieldContent>
            </Field>
          </FieldGroup>

          {documentationRequired && (
            <>
              <FieldGroup>
                <Field>
                  <FieldContent>
                    <FormCheckbox
                      id="field-documentationProvided"
                      label="Documentation provided"
                      checked={documentationProvided}
                      onCheckedChange={checked =>
                        handleNestedChange('citizenship', 'documentationProvided', checked === true)
                      }
                    />
                  </FieldContent>
                </Field>

                {documentationProvided && (
                  <Field>
                    <FieldLabel htmlFor="field-documentationType">Documentation Type</FieldLabel>
                    <FieldContent>
                      <Input
                        id="field-documentationType"
                        type="text"
                        value={formData.citizenship?.documentationType || ''}
                        onChange={e =>
                          handleNestedChange('citizenship', 'documentationType', e.target.value)
                        }
                        placeholder="e.g., Birth Certificate, Passport, Naturalization Papers"
                      />
                    </FieldContent>
                  </Field>
                )}
              </FieldGroup>

              <FieldGroup>
                <Field>
                  <FieldLabel htmlFor="field-fundingSourceRequirement">Funding Source Requirement</FieldLabel>
                  <FieldContent>
                    <Input
                      id="field-fundingSourceRequirement"
                      type="text"
                      value={formData.citizenship?.fundingSourceRequirement || ''}
                      onChange={e =>
                        handleNestedChange('citizenship', 'fundingSourceRequirement', e.target.value)
                      }
                      placeholder="e.g., HUD CoC, ESG"
                    />
                  </FieldContent>
                </Field>
              </FieldGroup>

              <FieldGroup>
                <Field>
                  <FieldContent>
                    <FormCheckbox
                      id="field-waiverApplies"
                      label="Waiver applies"
                      checked={waiverApplies}
                      onCheckedChange={checked =>
                        handleNestedChange('citizenship', 'waiverApplies', checked === true)
                      }
                    />
                  </FieldContent>
                </Field>

                {waiverApplies && (
                  <Field>
                    <FieldLabel htmlFor="field-waiverReason">Waiver Reason</FieldLabel>
                    <FieldContent>
                      <Textarea
                        id="field-waiverReason"
                        value={formData.citizenship?.waiverReason || ''}
                        onChange={e =>
                          handleNestedChange('citizenship', 'waiverReason', e.target.value)
                        }
                        placeholder="Explain why waiver applies"
                        rows={3}
                      />
                    </FieldContent>
                  </Field>
                )}
              </FieldGroup>

              <FieldGroup>
                <Field data-invalid={!!citizenshipError}>
                  <FieldContent>
                    <FieldError>{citizenshipError}</FieldError>
                  </FieldContent>
                </Field>
              </FieldGroup>
            </>
          )}
        </FieldSet>
        {/* Section 5: Program Matching */}
        <FieldSet>
          <FieldLegend>Program Matching</FieldLegend>
          <FieldGroup>
            <Field>
              <FieldTitle>Eligibility Calculator</FieldTitle>
              <FieldContent className="flex flex-wrap items-center gap-3">
                <Button type="button" onClick={handleCalculateEligibility} disabled={isCalculatingEligibility}>
                  {isCalculatingEligibility ? 'Calculating...' : 'Calculate Eligibility'}
                </Button>
              </FieldContent>
            </Field>
          </FieldGroup>

          {formData.eligibilityResults && (
            <FieldGroup>
              <Field>
                <FieldTitle>Eligibility Results</FieldTitle>
                <FieldContent>
                  <div className="eligibility-results">
                    <div className="eligibility-badges">
                      <div
                        className={`eligibility-badge ${
                          formData.eligibilityResults.eligibleForTH ? 'eligible' : 'not-eligible'
                        }`}
                      >
                        <span className="badge-icon">
                          {formData.eligibilityResults.eligibleForTH ? '?' : '?'}
                        </span>
                        <span>Transitional Housing</span>
                      </div>

                      <div
                        className={`eligibility-badge ${
                          formData.eligibilityResults.eligibleForRRH ? 'eligible' : 'not-eligible'
                        }`}
                      >
                        <span className="badge-icon">
                          {formData.eligibilityResults.eligibleForRRH ? '?' : '?'}
                        </span>
                        <span>Rapid Re-Housing</span>
                      </div>

                      <div
                        className={`eligibility-badge ${
                          formData.eligibilityResults.eligibleForPSH ? 'eligible' : 'not-eligible'
                        }`}
                      >
                        <span className="badge-icon">
                          {formData.eligibilityResults.eligibleForPSH ? '?' : '?'}
                        </span>
                        <span>Permanent Supportive Housing</span>
                      </div>
                    </div>

                    {formData.eligibilityResults.ineligibilityReasons &&
                      formData.eligibilityResults.ineligibilityReasons.length > 0 && (
                        <div className="ineligibility-reasons">
                          <h5>Ineligibility Reasons:</h5>
                          <ul>
                            {formData.eligibilityResults.ineligibilityReasons.map((reason, index) => (
                              <li key={index}>{reason}</li>
                            ))}
                          </ul>
                        </div>
                      )}

                    {eligibilityResultsWarning && (
                      <div className="alert alert-warning">
                        <span className="alert-icon">??</span>
                        <span>{eligibilityResultsWarning}</span>
                      </div>
                    )}
                  </div>
                </FieldContent>
              </Field>
            </FieldGroup>
          )}

          <FieldGroup>
            <Field>
              <FieldLabel htmlFor="field-programTypeFilter">Filter by Program Type</FieldLabel>
              <FieldContent>
                <FormSelect
                  id="field-programTypeFilter"
                  value={programTypeFilter}
                  onChange={handleProgramTypeChange}
                  options={PROGRAM_TYPE_FILTER_OPTIONS.map(option => ({
                    value: option.value,
                    label: option.label,
                  }))}
                  placeholder="Select program type"
                />
                {recommendedProgramTypeLabel && (
                  <div className="text-xs text-muted-foreground mt-1">
                    Recommended type: <span className="font-medium">{recommendedProgramTypeLabel}</span>
                    {formData.eligibilityResults?.recommendationReason
                      ? ` – ${formData.eligibilityResults.recommendationReason}`
                      : ''}
                  </div>
                )}
              </FieldContent>
            </Field>
            <Field data-invalid={!!selectedProgramError}>
              <FieldLabel htmlFor="field-programSelection">
                Select Program to Enroll Client <span className="text-destructive">*</span>
              </FieldLabel>
              <FieldContent>
                <FormSelect
                  id="field-programSelection"
                  value={selectedProgramId}
                  onChange={handleProgramSelect}
                  options={filteredPrograms.map(program => ({
                    value: program.id,
                    label: `${program.name} • ${program.location} • ${program.availableSlots} open`,
                  }))}
                  placeholder={
                    filteredPrograms.length
                      ? 'Choose a program...'
                      : programTypeFilter === 'ALL'
                      ? 'No programs are configured'
                      : 'No programs available for this type'
                  }
                  disabled={filteredPrograms.length === 0}
                />
                <FieldError>{selectedProgramError}</FieldError>
              </FieldContent>
            </Field>
          </FieldGroup>

          {formData.selectedProgram ? (
            <div className="recommended-program-card">
              <div className="card-header">
                <span className="star-icon">?</span>
                <span>SELECTED PROGRAM</span>
              </div>
              <div className="card-body">
                <h5>{formData.selectedProgram.programName}</h5>
                <div className="program-details">
                  <div className="detail-row">
                    <span className="label">Program Type:</span>
                    <span className="value">
                      {PROGRAM_TYPE_LABELS[formData.selectedProgram.programType] ??
                        formData.selectedProgram.programType}
                    </span>
                  </div>
                  <div className="detail-row">
                    <span className="label">Funding Source:</span>
                    <span className="value">{formData.selectedProgram.fundingSource}</span>
                  </div>
                  {formData.selectedProgram.location && (
                    <div className="detail-row">
                      <span className="label">Location:</span>
                      <span className="value">{formData.selectedProgram.location}</span>
                    </div>
                  )}
                  {typeof formData.selectedProgram.availableSlots === 'number' && (
                    <div className="detail-row">
                      <span className="label">Slots Available:</span>
                      <span className="value">{formData.selectedProgram.availableSlots}</span>
                    </div>
                  )}
                  {formData.selectedProgram.dailyRate !== undefined && (
                    <div className="detail-row">
                      <span className="label">Daily Rate:</span>
                      <span className="value">
                        {formData.selectedProgram.dailyRate
                          ? `$${formData.selectedProgram.dailyRate.toFixed(2)}`
                          : 'N/A'}
                      </span>
                    </div>
                  )}
                  {formData.selectedProgram.expectedEntryDate && (
                    <div className="detail-row">
                      <span className="label">Expected Entry Date:</span>
                      <span className="value">
                        {new Date(formData.selectedProgram.expectedEntryDate).toLocaleDateString()}
                      </span>
                    </div>
                  )}
                </div>
                <p className="recommendation-reason">
                  <strong>Selected By:</strong> {formData.selectedProgram.selectedBy} &middot;{' '}
                  {new Date(formData.selectedProgram.selectionDate).toLocaleString()}
                </p>
              </div>
            </div>
          ) : recommendedProgramTypeLabel ? (
            <div className="alert alert-info mt-4">
              <span className="alert-icon">??</span>
              <div>
                <strong>Recommended Program Type:</strong> {recommendedProgramTypeLabel}
                {formData.eligibilityResults?.recommendationReason
                  ? ` – ${formData.eligibilityResults.recommendationReason}`
                  : ''}
                . Select a specific program from the list above to continue.
              </div>
            </div>
          ) : (
            <div className="text-sm text-muted-foreground mt-4">
              No automated recommendation is available. Select the most appropriate program for the
              client.
            </div>
          )}

          {formData.selectedProgram && (
            <FieldGroup>
              <Field>
                <FieldLabel htmlFor="field-expectedEntryDate">Expected Entry Date</FieldLabel>
                <FieldContent>
                  <Input
                    id="field-expectedEntryDate"
                    type="date"
                    value={formData.selectedProgram.expectedEntryDate || ''}
                    onChange={e => handleExpectedEntryDateChange(e.target.value)}
                  />
                </FieldContent>
              </Field>
              {!isUsingRecommendedType && recommendedProgramTypeLabel && (
                <Field>
                  <FieldLabel htmlFor="field-overrideReason">
                    Override Reason (different than recommended type)
                  </FieldLabel>
                  <FieldContent>
                    <Textarea
                      id="field-overrideReason"
                      value={formData.overrideReason || ''}
                      onChange={e => handleOverrideReasonChange(e.target.value)}
                      placeholder="Briefly explain why this program was selected."
                      rows={3}
                    />
                  </FieldContent>
                </Field>
              )}
            </FieldGroup>
          )}
        </FieldSet>
        {/* Form Actions */}
        <div className="form-actions">
          <Button type="button" variant="secondary" onClick={onBack}>
            Back to Step 3
          </Button>
          <Button type="submit">Continue to Step 5</Button>
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












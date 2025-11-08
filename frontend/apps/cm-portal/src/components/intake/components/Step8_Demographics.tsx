/**
 * Step 8: Demographics & Outcome Baseline
 *
 * Collects HUD-required demographics AFTER consent obtained (full name NOW collected).
 * Ensures VAWA compliance by collecting PII only after proper consent.
 */

import React, { useState, useEffect } from 'react';
import type { DemographicsBaselineData, ValidationError, DataQuality } from '../utils/types';

interface Step8Props {
  data: Partial<DemographicsBaselineData>;
  errors: ValidationError[];
  onChange: (updates: Partial<DemographicsBaselineData>) => void;
  onComplete: (data: DemographicsBaselineData) => void;
  onBack?: () => void;
  clientAlias?: string; // From Step 1, for reference
  safeContactPhone?: string; // From Step 2
  safeContactEmail?: string; // From Step 2
}

export function Step8_Demographics({
  data,
  errors,
  onChange,
  onComplete,
  onBack,
  clientAlias,
  safeContactPhone,
  safeContactEmail,
}: Step8Props) {
  const [formData, setFormData] = useState<Partial<DemographicsBaselineData>>({
    name: {
      firstName: '',
      middleName: '',
      lastName: '',
      suffix: '',
      preferredName: '',
      nameDataQuality: 1,
      ...data.name,
    },
    identifiers: {
      socialSecurityNumber: '',
      ssnDataQuality: 99,
      stateIdNumber: '',
      stateIdState: '',
      birthDate: '',
      dobDataQuality: 1,
      birthPlace: '',
      ...data.identifiers,
    },
    demographics: {
      birthDate: '',
      age: undefined,
      gender: '',
      hmisGender: [],
      race: [],
      ethnicity: '',
      primaryLanguage: 'English',
      interpreterNeeded: false,
      preferredLanguage: '',
      ...data.demographics,
    },
    veteranStatus: data.veteranStatus || 'DATA_NOT_COLLECTED',
    disablingCondition: data.disablingCondition || 'DATA_NOT_COLLECTED',
    pseudonymization: {
      hmisClientId: '',
      vawaProtected: false,
      pseudonymizationMethod: 'HASHED',
      exportRestricted: false,
      ...data.pseudonymization,
    },
    contactInfo: data.contactInfo || {
      phones: [],
      emails: [],
      addressConfidential: false,
    },
  });

  const [ssnMasked, setSsnMasked] = useState(true);
  const [calculatedAge, setCalculatedAge] = useState<number | undefined>();

  // Generate HMIS Client ID on mount if not exists
  useEffect(() => {
    if (!formData.pseudonymization?.hmisClientId) {
      const hmisId = generateHmisClientId();
      handleNestedChange('pseudonymization', 'hmisClientId', hmisId);
    }
  }, []);

  // Auto-calculate age from birthDate
  useEffect(() => {
    if (formData.identifiers?.birthDate) {
      const age = calculateAge(formData.identifiers.birthDate);
      setCalculatedAge(age);
      handleNestedChange('demographics', 'age', age);
    }
  }, [formData.identifiers?.birthDate]);

  const handleChange = (field: keyof DemographicsBaselineData, value: any) => {
    const updated = { ...formData, [field]: value };
    setFormData(updated);
    onChange(updated);
  };

  const handleNestedChange = (section: string, field: string, value: any) => {
    const updated = {
      ...formData,
      [section]: {
        ...(formData[section as keyof DemographicsBaselineData] as any),
        [field]: value,
      },
    };
    setFormData(updated);
    onChange(updated);
  };

  const handleArrayToggle = (section: string, field: string, value: string) => {
    const current = (formData[section as keyof DemographicsBaselineData] as any)?.[field] || [];
    const updated = current.includes(value)
      ? current.filter((v: string) => v !== value)
      : [...current, value];
    handleNestedChange(section, field, updated);
  };

  const formatSSN = (value: string): string => {
    const digits = value.replace(/\D/g, '');
    if (digits.length <= 3) return digits;
    if (digits.length <= 5) return `${digits.slice(0, 3)}-${digits.slice(3)}`;
    return `${digits.slice(0, 3)}-${digits.slice(3, 5)}-${digits.slice(5, 9)}`;
  };

  const maskSSN = (ssn: string): string => {
    const digits = ssn.replace(/\D/g, '');
    if (digits.length !== 9) return ssn;
    return `XXX-XX-${digits.slice(5)}`;
  };

  const handleSSNChange = (value: string) => {
    const formatted = formatSSN(value);
    handleNestedChange('identifiers', 'socialSecurityNumber', formatted);
  };

  const handleSubmit = () => {
    if (formData as DemographicsBaselineData) {
      onComplete(formData as DemographicsBaselineData);
    }
  };

  const getError = (field: string): ValidationError | undefined => {
    return errors.find(e => e.field === field);
  };

  return (
    <div className="intake-step">
      <div className="step-header">
        <h2>Step 8: Demographics & Baseline Data</h2>
        <p className="step-description">
          Collect HUD-required demographics and personal information. This data is collected
          AFTER consent has been obtained in Step 2.
        </p>
      </div>

      {clientAlias && (
        <div className="alert alert-info">
          <span className="alert-icon">ℹ️</span>
          <div className="alert-content">
            <strong>Client Alias: {clientAlias}</strong>
            <p>Now collecting full legal name after consent obtained.</p>
          </div>
        </div>
      )}

      <form className="intake-form">
        {/* Full Legal Name */}
        <section className="form-section">
          <h3>Full Legal Name</h3>
          <p className="section-help">
            Legal name as it appears on government-issued identification.
          </p>

          <div className="form-row">
            <div className="form-field">
              <label>
                First Name <span className="required">*</span>
              </label>
              <input
                type="text"
                value={formData.name?.firstName || ''}
                onChange={e => handleNestedChange('name', 'firstName', e.target.value)}
                className={getError('name.firstName') ? 'error' : ''}
              />
              {getError('name.firstName') && (
                <span className="field-error">{getError('name.firstName')?.message}</span>
              )}
            </div>

            <div className="form-field">
              <label>Middle Name</label>
              <input
                type="text"
                value={formData.name?.middleName || ''}
                onChange={e => handleNestedChange('name', 'middleName', e.target.value)}
              />
            </div>
          </div>

          <div className="form-row">
            <div className="form-field">
              <label>
                Last Name <span className="required">*</span>
              </label>
              <input
                type="text"
                value={formData.name?.lastName || ''}
                onChange={e => handleNestedChange('name', 'lastName', e.target.value)}
                className={getError('name.lastName') ? 'error' : ''}
              />
              {getError('name.lastName') && (
                <span className="field-error">{getError('name.lastName')?.message}</span>
              )}
            </div>

            <div className="form-field">
              <label>Suffix</label>
              <input
                type="text"
                placeholder="Jr., Sr., III, etc."
                value={formData.name?.suffix || ''}
                onChange={e => handleNestedChange('name', 'suffix', e.target.value)}
              />
            </div>
          </div>

          <div className="form-field">
            <label>Preferred Name (if different)</label>
            <input
              type="text"
              value={formData.name?.preferredName || ''}
              onChange={e => handleNestedChange('name', 'preferredName', e.target.value)}
            />
            <span className="field-help">Name the client prefers to be called.</span>
          </div>

          <div className="form-field">
            <label>Name Data Quality</label>
            <select
              value={formData.name?.nameDataQuality || 1}
              onChange={e => handleNestedChange('name', 'nameDataQuality', Number(e.target.value) as DataQuality)}
            >
              <option value={1}>Full name reported</option>
              <option value={2}>Partial name</option>
              <option value={8}>Client doesn't know</option>
              <option value={9}>Client refused</option>
              <option value={99}>Data not collected</option>
            </select>
          </div>
        </section>

        {/* Government Identifiers */}
        <section className="form-section">
          <h3>Government Identifiers</h3>

          {/* SSN */}
          <div className="subsection">
            <h4>Social Security Number</h4>
            <div className="form-field">
              <label>SSN</label>
              <div className="input-with-button">
                <input
                  type={ssnMasked ? 'password' : 'text'}
                  value={formData.identifiers?.socialSecurityNumber || ''}
                  onChange={e => handleSSNChange(e.target.value)}
                  placeholder="XXX-XX-XXXX"
                  maxLength={11}
                />
                <button
                  type="button"
                  className="btn-secondary btn-sm"
                  onClick={() => setSsnMasked(!ssnMasked)}
                >
                  {ssnMasked ? 'Show' : 'Hide'}
                </button>
              </div>
              <span className="field-help">SSN will be masked as XXX-XX-{formData.identifiers?.socialSecurityNumber?.slice(-4) || '****'}</span>
            </div>

            <div className="form-field">
              <label>SSN Data Quality</label>
              <select
                value={formData.identifiers?.ssnDataQuality || 99}
                onChange={e => handleNestedChange('identifiers', 'ssnDataQuality', Number(e.target.value) as DataQuality)}
              >
                <option value={1}>Full SSN</option>
                <option value={2}>Partial SSN</option>
                <option value={8}>Client doesn't know</option>
                <option value={9}>Client refused</option>
                <option value={99}>Data not collected</option>
              </select>
            </div>
          </div>

          {/* State ID */}
          <div className="subsection">
            <h4>State ID (Optional)</h4>
            <div className="form-row">
              <div className="form-field">
                <label>ID Number</label>
                <input
                  type="text"
                  value={formData.identifiers?.stateIdNumber || ''}
                  onChange={e => handleNestedChange('identifiers', 'stateIdNumber', e.target.value)}
                />
              </div>

              <div className="form-field">
                <label>Issuing State</label>
                <select
                  value={formData.identifiers?.stateIdState || ''}
                  onChange={e => handleNestedChange('identifiers', 'stateIdState', e.target.value)}
                >
                  <option value="">Select state...</option>
                  <option value="AL">Alabama</option>
                  <option value="AK">Alaska</option>
                  <option value="AZ">Arizona</option>
                  <option value="CA">California</option>
                  <option value="FL">Florida</option>
                  <option value="TX">Texas</option>
                  {/* Add more states as needed */}
                </select>
              </div>
            </div>
          </div>

          {/* Date of Birth */}
          <div className="subsection">
            <h4>Date of Birth</h4>
            <div className="form-row">
              <div className="form-field">
                <label>
                  Date of Birth <span className="required">*</span>
                </label>
                <input
                  type="date"
                  value={formData.identifiers?.birthDate || ''}
                  onChange={e => handleNestedChange('identifiers', 'birthDate', e.target.value)}
                  max={new Date().toISOString().split('T')[0]}
                  className={getError('identifiers.birthDate') ? 'error' : ''}
                />
                {getError('identifiers.birthDate') && (
                  <span className="field-error">{getError('identifiers.birthDate')?.message}</span>
                )}
              </div>

              <div className="form-field">
                <label>Calculated Age</label>
                <div className="readonly-value">
                  {calculatedAge !== undefined ? `${calculatedAge} years` : 'N/A'}
                </div>
              </div>
            </div>

            <div className="form-field">
              <label>DOB Data Quality</label>
              <select
                value={formData.identifiers?.dobDataQuality || 1}
                onChange={e => handleNestedChange('identifiers', 'dobDataQuality', Number(e.target.value) as DataQuality)}
              >
                <option value={1}>Full DOB reported</option>
                <option value={2}>Approximate or partial DOB</option>
                <option value={8}>Client doesn't know</option>
                <option value={9}>Client refused</option>
                <option value={99}>Data not collected</option>
              </select>
            </div>

            <div className="form-field">
              <label>Birth Place (City, State)</label>
              <input
                type="text"
                placeholder="e.g., Los Angeles, CA"
                value={formData.identifiers?.birthPlace || ''}
                onChange={e => handleNestedChange('identifiers', 'birthPlace', e.target.value)}
              />
            </div>
          </div>
        </section>

        {/* Demographics (HMIS Universal Data Elements) */}
        <section className="form-section">
          <h3>Demographics (HMIS Universal Data Elements)</h3>

          {/* Administrative Gender */}
          <div className="form-field">
            <label>
              Administrative Gender <span className="required">*</span>
            </label>
            <select
              value={formData.demographics?.gender || ''}
              onChange={e => handleNestedChange('demographics', 'gender', e.target.value)}
              className={getError('demographics.gender') ? 'error' : ''}
            >
              <option value="">Select gender...</option>
              <option value="MALE">Male</option>
              <option value="FEMALE">Female</option>
              <option value="OTHER">Other</option>
              <option value="UNKNOWN">Unknown</option>
            </select>
            <span className="field-help">Gender recorded for administrative purposes.</span>
            {getError('demographics.gender') && (
              <span className="field-error">{getError('demographics.gender')?.message}</span>
            )}
          </div>

          {/* HMIS Gender (Multi-select) */}
          <fieldset>
            <legend>HMIS Gender (Self-Identified)</legend>
            <div className="checkbox-group">
              {['Woman', 'Man', 'Culturally Specific Identity', 'Transgender', 'Questioning', 'Different Identity'].map(option => (
                <label key={option} className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={formData.demographics?.hmisGender?.includes(option) || false}
                    onChange={() => handleArrayToggle('demographics', 'hmisGender', option)}
                  />
                  {option}
                </label>
              ))}
              <label className="checkbox-label">
                <input
                  type="checkbox"
                  checked={formData.demographics?.hmisGender?.includes('CLIENT_DOESNT_KNOW') || false}
                  onChange={() => handleArrayToggle('demographics', 'hmisGender', 'CLIENT_DOESNT_KNOW')}
                />
                Client doesn't know
              </label>
              <label className="checkbox-label">
                <input
                  type="checkbox"
                  checked={formData.demographics?.hmisGender?.includes('CLIENT_REFUSED') || false}
                  onChange={() => handleArrayToggle('demographics', 'hmisGender', 'CLIENT_REFUSED')}
                />
                Client prefers not to answer
              </label>
            </div>
          </fieldset>

          {/* Race (Multi-select) */}
          <fieldset>
            <legend>Race (Select all that apply)</legend>
            <div className="checkbox-group">
              {[
                'American Indian/Alaska Native',
                'Asian/Asian American',
                'Black/African American',
                'Native Hawaiian/Pacific Islander',
                'White',
              ].map(option => (
                <label key={option} className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={formData.demographics?.race?.includes(option) || false}
                    onChange={() => handleArrayToggle('demographics', 'race', option)}
                  />
                  {option}
                </label>
              ))}
              <label className="checkbox-label">
                <input
                  type="checkbox"
                  checked={formData.demographics?.race?.includes('CLIENT_DOESNT_KNOW') || false}
                  onChange={() => handleArrayToggle('demographics', 'race', 'CLIENT_DOESNT_KNOW')}
                />
                Client doesn't know
              </label>
              <label className="checkbox-label">
                <input
                  type="checkbox"
                  checked={formData.demographics?.race?.includes('CLIENT_REFUSED') || false}
                  onChange={() => handleArrayToggle('demographics', 'race', 'CLIENT_REFUSED')}
                />
                Client prefers not to answer
              </label>
            </div>
          </fieldset>

          {/* Ethnicity */}
          <fieldset>
            <legend>Ethnicity</legend>
            <div className="radio-group">
              {[
                { value: 'HISPANIC', label: 'Hispanic/Latin(a)(o)(x)' },
                { value: 'NON_HISPANIC', label: 'Non-Hispanic' },
                { value: 'CLIENT_DOESNT_KNOW', label: "Client doesn't know" },
                { value: 'CLIENT_REFUSED', label: 'Client prefers not to answer' },
              ].map(option => (
                <label key={option.value} className="radio-label">
                  <input
                    type="radio"
                    name="ethnicity"
                    value={option.value}
                    checked={formData.demographics?.ethnicity === option.value}
                    onChange={e => handleNestedChange('demographics', 'ethnicity', e.target.value)}
                  />
                  {option.label}
                </label>
              ))}
            </div>
          </fieldset>

          {/* Language */}
          <div className="form-field">
            <label>Primary Language</label>
            <select
              value={formData.demographics?.primaryLanguage || 'English'}
              onChange={e => handleNestedChange('demographics', 'primaryLanguage', e.target.value)}
            >
              <option value="English">English</option>
              <option value="Spanish">Spanish</option>
              <option value="Mandarin">Mandarin</option>
              <option value="Vietnamese">Vietnamese</option>
              <option value="Arabic">Arabic</option>
              <option value="Other">Other</option>
            </select>
          </div>

          <div className="form-field">
            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={formData.demographics?.interpreterNeeded || false}
                onChange={e => handleNestedChange('demographics', 'interpreterNeeded', e.target.checked)}
              />
              Interpreter needed
            </label>
          </div>

          {formData.demographics?.interpreterNeeded && (
            <div className="form-field">
              <label>Preferred Language for Services</label>
              <input
                type="text"
                value={formData.demographics?.preferredLanguage || ''}
                onChange={e => handleNestedChange('demographics', 'preferredLanguage', e.target.value)}
              />
            </div>
          )}
        </section>

        {/* Veteran Status & Disability */}
        <section className="form-section">
          <h3>Veteran Status & Disability</h3>
          <p className="section-help">HUD-required fields for program eligibility.</p>

          <div className="form-field">
            <label>Veteran Status</label>
            <select
              value={formData.veteranStatus || 'DATA_NOT_COLLECTED'}
              onChange={e => handleChange('veteranStatus', e.target.value)}
            >
              <option value="YES">Yes</option>
              <option value="NO">No</option>
              <option value="CLIENT_DOESNT_KNOW">Client doesn't know</option>
              <option value="CLIENT_REFUSED">Client refused</option>
              <option value="DATA_NOT_COLLECTED">Data not collected</option>
            </select>
          </div>

          <div className="form-field">
            <label>Disabling Condition</label>
            <select
              value={formData.disablingCondition || 'DATA_NOT_COLLECTED'}
              onChange={e => handleChange('disablingCondition', e.target.value)}
            >
              <option value="YES">Yes</option>
              <option value="NO">No</option>
              <option value="CLIENT_DOESNT_KNOW">Client doesn't know</option>
              <option value="CLIENT_REFUSED">Client refused</option>
              <option value="DATA_NOT_COLLECTED">Data not collected</option>
            </select>
            <span className="field-help">
              <a
                href="https://hudexchange.info/programs/hmis/hmis-data-standards/"
                target="_blank"
                rel="noopener noreferrer"
                className="external-link"
              >
                View HUD definition of disabling condition
              </a>
            </span>
          </div>
        </section>

        {/* Contact Information (Read-only from Step 2) */}
        <section className="form-section">
          <h3>Contact Information</h3>
          <p className="section-help">Contact information from Step 2 (Safe Contact Methods).</p>

          <div className="contact-display">
            {safeContactPhone && (
              <div className="contact-item">
                <strong>Safe Phone:</strong> {safeContactPhone}
              </div>
            )}
            {safeContactEmail && (
              <div className="contact-item">
                <strong>Safe Email:</strong> {safeContactEmail}
              </div>
            )}
            {!safeContactPhone && !safeContactEmail && (
              <p className="section-help">No contact information provided in Step 2.</p>
            )}
          </div>
        </section>

        {/* HMIS Pseudonymization */}
        <section className="form-section">
          <h3>HMIS Pseudonymization</h3>

          <div className="hmis-id-display">
            <label>HMIS Client ID (Auto-generated)</label>
            <div className="readonly-value">
              {formData.pseudonymization?.hmisClientId || 'Generating...'}
            </div>
          </div>

          {formData.pseudonymization?.vawaProtected && (
            <div className="alert alert-warning">
              <span className="alert-icon">⚠️</span>
              <div className="alert-content">
                <strong>VAWA PROTECTED</strong>
                <p>Personal identifiers will be pseudonymized for HMIS export.</p>
              </div>
            </div>
          )}

          <div className="form-field">
            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={formData.pseudonymization?.vawaProtected || false}
                onChange={e => handleNestedChange('pseudonymization', 'vawaProtected', e.target.checked)}
              />
              VAWA Protected Status
            </label>
            <span className="field-help">Restricts data sharing under VAWA confidentiality rules.</span>
          </div>

          <div className="form-field">
            <label>Pseudonymization Method</label>
            <select
              value={formData.pseudonymization?.pseudonymizationMethod || 'HASHED'}
              onChange={e => handleNestedChange('pseudonymization', 'pseudonymizationMethod', e.target.value)}
            >
              <option value="HASHED">Hashed</option>
              <option value="TOKENIZED">Tokenized</option>
              <option value="ENCRYPTED">Encrypted</option>
            </select>
          </div>

          <div className="form-field">
            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={formData.pseudonymization?.exportRestricted || false}
                onChange={e => handleNestedChange('pseudonymization', 'exportRestricted', e.target.checked)}
              />
              Restrict data export
            </label>
            <span className="field-help">Prevents automatic export to external HMIS systems.</span>
          </div>
        </section>

        {/* Validation Summary */}
        {errors.length > 0 && (
          <div className="error-summary">
            <h4>Please correct the following errors:</h4>
            <ul>
              {errors.map((error, index) => (
                <li key={index}>{error.message}</li>
              ))}
            </ul>
          </div>
        )}

        {/* Form Actions */}
        <div className="form-actions">
          {onBack && (
            <button type="button" className="btn btn-secondary" onClick={onBack}>
              Back
            </button>
          )}
          <button type="button" className="btn btn-primary" onClick={handleSubmit}>
            Continue to Enrollment Confirmation
          </button>
        </div>
      </form>
    </div>
  );
}

// =============================================================================
// HELPER FUNCTIONS
// =============================================================================

function generateHmisClientId(): string {
  // Generate a HMIS-compliant client ID (example implementation)
  const prefix = 'HMIS';
  const timestamp = Date.now().toString(36).toUpperCase();
  const random = Math.random().toString(36).substring(2, 8).toUpperCase();
  return `${prefix}-${timestamp}-${random}`;
}

function calculateAge(birthDate: string): number {
  const today = new Date();
  const birth = new Date(birthDate);
  let age = today.getFullYear() - birth.getFullYear();
  const monthDiff = today.getMonth() - birth.getMonth();

  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
    age--;
  }

  return age;
}

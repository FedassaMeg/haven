/**
 * Step 2: Safety & Consent Check
 *
 * Verifies safety and obtains digital consent before proceeding.
 * VAWA-compliant: Collects safe contact methods and consent forms.
 */

import React, { useState, useEffect, useRef } from 'react';
import {
  Button,
  Field,
  FieldContent,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
  FieldLegend,
  FieldSet,
  FieldTitle,
  FormCheckbox,
  Input,
  Label,
  RadioGroup,
  RadioGroupItem,
  Textarea,
} from '@haven/ui';
import type { SafetyAndConsentData, ValidationError, EmergencyContactInfo } from '../utils/types';
import { validateStep2 } from '../lib/validation';
import { formatPhoneNumber } from '../index';

interface Step2Props {
  data: Partial<SafetyAndConsentData>;
  errors: ValidationError[];
  warnings: ValidationError[];
  onChange: (updates: Partial<SafetyAndConsentData>) => void;
  onComplete: (data: SafetyAndConsentData) => void;
  onBack: () => void;
  tempClientId: string;
}

export const Step2_SafetyAndConsent: React.FC<Step2Props> = ({
  data,
  errors,
  warnings,
  onChange,
  onComplete,
  onBack,
  tempClientId,
}) => {
  const [formData, setFormData] = useState<Partial<SafetyAndConsentData>>({
    safeContactMethods: {
      okToCall: false,
      okToText: false,
      okToEmail: false,
      okToVoicemail: false,
      ...data.safeContactMethods,
    },
    consents: {
      consentToServices: false,
      consentToDataCollection: false,
      consentToHmis: false,
      hmisParticipationStatus: 'PENDING',
      vawaExempt: false,
      ...data.consents,
    },
    ...data,
  });

  const [validationErrors, setValidationErrors] = useState<ValidationError[]>(errors || []);
  const [validationWarnings, setValidationWarnings] = useState<ValidationError[]>(warnings || []);
  const [showEmergencyContact, setShowEmergencyContact] = useState(!!data.emergencyContact);
  const [signatureTab, setSignatureTab] = useState<'draw' | 'type'>('type');
  const [hasScrolledConsent, setHasScrolledConsent] = useState(false);

  const consentTextRef = useRef<HTMLDivElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [isDrawing, setIsDrawing] = useState(false);
  const [signatureData, setSignatureData] = useState<string | undefined>(
    data.digitalSignature?.signatureData
  );

  // Update form data when props change
  useEffect(() => {
    setFormData(prev => ({ ...prev, ...data }));
  }, [data]);

  // Update validation errors/warnings when props change
  useEffect(() => {
    setValidationErrors(errors || []);
    setValidationWarnings(warnings || []);
  }, [errors, warnings]);

  // Check if consent box is scrollable, if not auto-enable checkbox
  useEffect(() => {
    const consentBox = consentTextRef.current;
    if (consentBox) {
      // If content doesn't overflow (not scrollable), enable checkbox immediately
      const isScrollable = consentBox.scrollHeight > consentBox.clientHeight;
      if (!isScrollable) {
        setHasScrolledConsent(true);
      }
    }
  }, []);

  const handleChange = (field: keyof SafetyAndConsentData, value: any) => {
    const updates = { [field]: value };
    setFormData(prev => ({ ...prev, ...updates }));
    onChange(updates);
  };

  const handleNestedChange = (
    parent: keyof SafetyAndConsentData,
    field: string,
    value: any
  ) => {
    const updates = {
      [parent]: {
        ...(formData[parent] as any),
        [field]: value,
      },
    };
    setFormData(prev => ({ ...prev, ...updates }));
    onChange(updates);
  };

  const handlePhoneChange = (field: string, value: string) => {
    // Format phone number as user types
    const formatted = formatPhoneNumber(value);
    handleNestedChange('safeContactMethods', field, formatted);
  };

  const handleConsentChange = (field: string, checked: boolean) => {
    const updates = {
      [field]: checked,
      [`${field}Date`]: checked ? new Date().toISOString() : undefined,
      [`${field}Version`]: checked ? '2.0' : undefined,
    };

    setFormData(prev => ({
      ...prev,
      consents: {
        ...prev.consents,
        ...updates,
      } as any,
    }));

    onChange({
      consents: {
        ...formData.consents,
        ...updates,
      } as any,
    });
  };

  const handleHmisParticipationChange = (status: string) => {
    const vawaExempt = status === 'VAWA_EXEMPT';
    const consentToHmis = status === 'PARTICIPATING';

    setFormData(prev => ({
      ...prev,
      consents: {
        ...prev.consents,
        hmisParticipationStatus: status as any,
        vawaExempt,
        consentToHmis,
        consentToHmisDate: consentToHmis ? new Date().toISOString() : undefined,
      },
    }));

    onChange({
      consents: {
        ...formData.consents,
        hmisParticipationStatus: status as any,
        vawaExempt,
        consentToHmis,
        consentToHmisDate: consentToHmis ? new Date().toISOString() : undefined,
      },
    });
  };

  const updateEmergencyContact = (updates: Partial<EmergencyContactInfo>) => {
    handleChange('emergencyContact', {
      ...(formData.emergencyContact || {}),
      ...updates,
    } as EmergencyContactInfo);
  };

  // Canvas signature handlers
  const startDrawing = (e: React.MouseEvent<HTMLCanvasElement>) => {
    setIsDrawing(true);
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const rect = canvas.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    ctx.beginPath();
    ctx.moveTo(x, y);
  };

  const draw = (e: React.MouseEvent<HTMLCanvasElement>) => {
    if (!isDrawing) return;

    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const rect = canvas.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    ctx.lineTo(x, y);
    ctx.strokeStyle = '#000';
    ctx.lineWidth = 2;
    ctx.lineCap = 'round';
    ctx.stroke();
  };

  const stopDrawing = () => {
    if (!isDrawing) return;
    setIsDrawing(false);

    const canvas = canvasRef.current;
    if (!canvas) return;

    // Save signature data
    const dataUrl = canvas.toDataURL('image/png');
    setSignatureData(dataUrl);

    handleChange('digitalSignature', {
      signed: true,
      signatureData: dataUrl,
      timestamp: new Date().toISOString(),
      ipAddress: 'client-side', // This should be captured server-side
      userAgent: navigator.userAgent,
    });
  };

  const clearSignature = () => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    ctx.clearRect(0, 0, canvas.width, canvas.height);
    setSignatureData(undefined);

    handleChange('digitalSignature', {
      signed: false,
      signatureData: undefined,
      typedName: undefined,
    });
  };

  const handleTypedNameChange = (typedName: string) => {
    handleChange('digitalSignature', {
      signed: !!typedName,
      typedName,
      timestamp: new Date().toISOString(),
      ipAddress: 'client-side',
      userAgent: navigator.userAgent,
    });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    // Validate
    const result = validateStep2(formData as SafetyAndConsentData);
    setValidationErrors(result.errors);
    setValidationWarnings(result.warnings);

    if (result.isValid) {
      onComplete(formData as SafetyAndConsentData);
    } else {
      // Scroll to first error
      const firstErrorField = result.errors[0]?.field;
      if (firstErrorField) {
        document.getElementById(`field-${firstErrorField}`)?.focus();
      }
    }
  };

  const getFieldError = (fieldName: string): string | undefined => {
    return validationErrors.find(e => e.field === fieldName)?.message;
  };

  const getFieldWarning = (fieldName: string): string | undefined => {
    return validationWarnings.find(w => w.field === fieldName)?.message;
  };

  const safeContactMethodsError = getFieldError('safeContactMethods');
  const safePhoneNumberError = getFieldError('safePhoneNumber');
  const safeEmailError = getFieldError('safeEmail');
  const quietHoursEndError = getFieldError('quietHoursEnd');
  const emergencyContactNameError = getFieldError('emergencyContact.name');
  const emergencyContactRelationshipError = getFieldError('emergencyContact.relationship');
  const emergencyContactPhoneError = getFieldError('emergencyContact.phone');
  const emergencyContactConsentError = getFieldError('emergencyContact.consentToShare');
  const consentToServicesError = getFieldError('consentToServices');
  const consentToDataCollectionError = getFieldError('consentToDataCollection');
  const hmisParticipationStatusError = getFieldError('hmisParticipationStatus');
  const digitalSignatureError = getFieldError('digitalSignature');
  const hmisParticipationWarning = getFieldWarning('hmisParticipationStatus');
  const vawaExemptWarning = getFieldWarning('vawaExempt');

  const hasAnyConsent =
    formData.consents?.consentToServices ||
    formData.consents?.consentToDataCollection ||
    formData.consents?.consentToHmis;

  return (
    <div className="intake-step step-2">
      <div className="step-header">
        <h2>Step 2: Safety & Consent Check</h2>
        <p className="step-description">
          Verify safety and obtain digital consent before proceeding. All consents are required to
          continue with services.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="intake-form">
        <FieldSet>
          <FieldLegend>Safe Contact Methods</FieldLegend>
          <FieldDescription>
            Select all safe ways to contact the client. We will never reach out using methods you
            don't approve.
          </FieldDescription>
          <FieldGroup data-slot="checkbox-group">
            <Field data-invalid={!!safeContactMethodsError}>
              <FieldTitle>Approved Contact Channels</FieldTitle>
              <FieldContent>
                <div className="flex flex-col gap-2">
                  <FormCheckbox
                    id="field-safeContactMethods-okToCall"
                    label="Safe to call"
                    checked={formData.safeContactMethods?.okToCall === true}
                    onCheckedChange={checked =>
                      handleNestedChange('safeContactMethods', 'okToCall', checked === true)
                    }
                  />
                  <FormCheckbox
                    id="field-safeContactMethods-okToText"
                    label="Safe to send text messages"
                    checked={formData.safeContactMethods?.okToText === true}
                    onCheckedChange={checked =>
                      handleNestedChange('safeContactMethods', 'okToText', checked === true)
                    }
                  />
                  <FormCheckbox
                    id="field-safeContactMethods-okToEmail"
                    label="Safe to send emails"
                    checked={formData.safeContactMethods?.okToEmail === true}
                    onCheckedChange={checked =>
                      handleNestedChange('safeContactMethods', 'okToEmail', checked === true)
                    }
                  />
                  <FormCheckbox
                    id="field-safeContactMethods-okToVoicemail"
                    label="Safe to leave voicemail messages"
                    checked={formData.safeContactMethods?.okToVoicemail === true}
                    onCheckedChange={checked =>
                      handleNestedChange('safeContactMethods', 'okToVoicemail', checked === true)
                    }
                  />
                </div>
                <FieldError>{safeContactMethodsError}</FieldError>
              </FieldContent>
            </Field>
          </FieldGroup>

          {(formData.safeContactMethods?.okToCall || formData.safeContactMethods?.okToText) && (
            <FieldGroup>
              <Field data-invalid={!!safePhoneNumberError}>
                <FieldLabel htmlFor="field-safePhoneNumber">
                  Safe Phone Number <span className="text-destructive">*</span>
                </FieldLabel>
                <FieldContent>
                  <Input
                    id="field-safePhoneNumber"
                    type="tel"
                    value={formData.safeContactMethods?.safePhoneNumber || ''}
                    onChange={e => handlePhoneChange('safePhoneNumber', e.target.value)}
                    placeholder="(555) 123-4567"
                    aria-invalid={!!safePhoneNumberError}
                  />
                  <FieldError>{safePhoneNumberError}</FieldError>
                </FieldContent>
              </Field>
            </FieldGroup>
          )}

          {formData.safeContactMethods?.okToEmail && (
            <FieldGroup>
              <Field data-invalid={!!safeEmailError}>
                <FieldLabel htmlFor="field-safeEmail">
                  Safe Email Address <span className="text-destructive">*</span>
                </FieldLabel>
                <FieldContent>
                  <Input
                    id="field-safeEmail"
                    type="email"
                    value={formData.safeContactMethods?.safeEmail || ''}
                    onChange={e =>
                      handleNestedChange('safeContactMethods', 'safeEmail', e.target.value)
                    }
                    placeholder="client@example.com"
                    aria-invalid={!!safeEmailError}
                  />
                  <FieldError>{safeEmailError}</FieldError>
                </FieldContent>
              </Field>
            </FieldGroup>
          )}

          <FieldGroup>
            <Field>
              <FieldLabel htmlFor="field-codeWord">Security Code Word (Optional)</FieldLabel>
              <FieldContent>
                <Input
                  id="field-codeWord"
                  type="text"
                  value={formData.safeContactMethods?.codeWord || ''}
                  onChange={e =>
                    handleNestedChange('safeContactMethods', 'codeWord', e.target.value)
                  }
                  placeholder="A word only you and staff will know"
                  minLength={4}
                />
                <FieldDescription>
                  This code word will be used to verify your identity during phone calls.
                </FieldDescription>
              </FieldContent>
            </Field>
          </FieldGroup>

          <FieldGroup>
            <Field>
              <FieldLabel htmlFor="field-quietHoursStart">Quiet Hours Start (Optional)</FieldLabel>
              <FieldContent>
                <Input
                  id="field-quietHoursStart"
                  type="time"
                  value={formData.safeContactMethods?.quietHoursStart || ''}
                  onChange={e =>
                    handleNestedChange('safeContactMethods', 'quietHoursStart', e.target.value)
                  }
                />
              </FieldContent>
            </Field>
            <Field data-invalid={!!quietHoursEndError}>
              <FieldLabel htmlFor="field-quietHoursEnd">Quiet Hours End</FieldLabel>
              <FieldContent>
                <Input
                  id="field-quietHoursEnd"
                  type="time"
                  value={formData.safeContactMethods?.quietHoursEnd || ''}
                  onChange={e =>
                    handleNestedChange('safeContactMethods', 'quietHoursEnd', e.target.value)
                  }
                  disabled={!formData.safeContactMethods?.quietHoursStart}
                  aria-invalid={!!quietHoursEndError}
                />
                <FieldError>{quietHoursEndError}</FieldError>
              </FieldContent>
            </Field>
          </FieldGroup>

          <FieldGroup>
            <Field>
              <FieldLabel htmlFor="field-safetyNotes">Safety Notes (Optional)</FieldLabel>
              <FieldContent>
                <Textarea
                  id="field-safetyNotes"
                  value={formData.safeContactMethods?.safetyNotes || ''}
                  onChange={e =>
                    handleNestedChange('safeContactMethods', 'safetyNotes', e.target.value)
                  }
                  placeholder="Any additional safety considerations for staff"
                  rows={3}
                  maxLength={2000}
                />
              </FieldContent>
            </Field>
          </FieldGroup>
        </FieldSet>

        <FieldSet>
          <FieldLegend>Emergency Contact (Optional)</FieldLegend>
          <FieldGroup data-slot="checkbox-group">
            <Field>
              <FieldContent>
                <FormCheckbox
                  id="field-addEmergencyContact"
                  label="Add emergency contact"
                  checked={showEmergencyContact}
                  onCheckedChange={checked => setShowEmergencyContact(checked === true)}
                />
              </FieldContent>
            </Field>
          </FieldGroup>

          {showEmergencyContact && (
            <FieldGroup>
              <Field data-invalid={!!emergencyContactNameError}>
                <FieldLabel htmlFor="field-emergencyContactName">
                  Name <span className="text-destructive">*</span>
                </FieldLabel>
                <FieldContent>
                  <Input
                    id="field-emergencyContactName"
                    type="text"
                    value={formData.emergencyContact?.name || ''}
                    onChange={e => updateEmergencyContact({ name: e.target.value })}
                    aria-invalid={!!emergencyContactNameError}
                  />
                  <FieldError>{emergencyContactNameError}</FieldError>
                </FieldContent>
              </Field>

              <Field data-invalid={!!emergencyContactRelationshipError}>
                <FieldLabel htmlFor="field-emergencyContactRelationship">
                  Relationship <span className="text-destructive">*</span>
                </FieldLabel>
                <FieldContent>
                  <Input
                    id="field-emergencyContactRelationship"
                    type="text"
                    value={formData.emergencyContact?.relationship || ''}
                    onChange={e => updateEmergencyContact({ relationship: e.target.value })}
                    placeholder="e.g., Sister, Friend, Social Worker"
                    aria-invalid={!!emergencyContactRelationshipError}
                  />
                  <FieldError>{emergencyContactRelationshipError}</FieldError>
                </FieldContent>
              </Field>

              <Field data-invalid={!!emergencyContactPhoneError}>
                <FieldLabel htmlFor="field-emergencyContactPhone">
                  Phone <span className="text-destructive">*</span>
                </FieldLabel>
                <FieldContent>
                  <Input
                    id="field-emergencyContactPhone"
                    type="tel"
                    value={formData.emergencyContact?.phone || ''}
                    onChange={e =>
                      updateEmergencyContact({ phone: formatPhoneNumber(e.target.value) })
                    }
                    placeholder="(555) 123-4567"
                    aria-invalid={!!emergencyContactPhoneError}
                  />
                  <FieldError>{emergencyContactPhoneError}</FieldError>
                </FieldContent>
              </Field>

              <Field>
                <FieldLabel htmlFor="field-emergencyContactEmail">Email (Optional)</FieldLabel>
                <FieldContent>
                  <Input
                    id="field-emergencyContactEmail"
                    type="email"
                    value={formData.emergencyContact?.email || ''}
                    onChange={e => updateEmergencyContact({ email: e.target.value })}
                    placeholder="emergency@example.com"
                  />
                </FieldContent>
              </Field>

              <Field data-invalid={!!emergencyContactConsentError}>
                <FieldTitle>Information Sharing Consent</FieldTitle>
                <FieldContent>
                  <FormCheckbox
                    id="field-emergencyContactConsent"
                    label="I consent to sharing my information with this contact *"
                    checked={formData.emergencyContact?.consentToShare === true}
                    onCheckedChange={checked =>
                      updateEmergencyContact({
                        consentToShare: checked === true,
                        consentDate: checked ? new Date().toISOString() : undefined,
                      })
                    }
                  />
                  <FieldError>{emergencyContactConsentError}</FieldError>
                </FieldContent>
              </Field>
            </FieldGroup>
          )}
        </FieldSet>

        {/* Section 3: Consent Forms */}
        <FieldSet className="consent-section">
          <FieldLegend>Consent Forms</FieldLegend>
          <FieldDescription>
            The following consents are required to receive services. Please review each consent
            carefully.
          </FieldDescription>
          <FieldGroup>
            <Field data-invalid={!!consentToServicesError}>
              <FieldTitle>Consent to Services</FieldTitle>
              <FieldContent>
                <div
                  ref={consentTextRef}
                  className="consent-text"
                  onScroll={() => setHasScrolledConsent(true)}
                >
                  <p>
                    I, the undersigned, hereby consent to receiving services from [Organization
                    Name]. I understand that:
                  </p>
                  <ul>
                    <li>Services are provided voluntarily and I may discontinue at any time</li>
                    <li>All information shared will be kept confidential per VAWA requirements</li>
                    <li>Services include case management, advocacy, and referrals as needed</li>
                    <li>My participation may be documented for funding and reporting purposes</li>
                  </ul>
                </div>
                {!hasScrolledConsent && (
                  <p className="text-sm text-secondary-600 mt-2">
                    Please scroll through the consent text above to enable the checkbox
                  </p>
                )}
                <FormCheckbox
                  id="field-consentToServices"
                  label="I consent to receiving services *"
                  checked={formData.consents?.consentToServices === true}
                  onCheckedChange={checked =>
                    handleConsentChange('consentToServices', checked === true)
                  }
                  disabled={!hasScrolledConsent}
                />
                {formData.consents?.consentToServicesDate && (
                  <p className="consent-date">
                    Consented on{' '}
                    {new Date(formData.consents.consentToServicesDate).toLocaleString()}
                  </p>
                )}
                <FieldError>{consentToServicesError}</FieldError>
              </FieldContent>
            </Field>

            <Field data-invalid={!!consentToDataCollectionError}>
              <FieldTitle>Consent to Data Collection</FieldTitle>
              <FieldContent>
                <div className="consent-text">
                  <p>
                    I consent to the collection, storage, and use of my personal information for the
                    purposes of:
                  </p>
                  <ul>
                    <li>Coordinating services and support</li>
                    <li>Meeting federal and state reporting requirements</li>
                    <li>Program evaluation and quality improvement</li>
                    <li>Aggregate data analysis (de-identified)</li>
                  </ul>
                  <p>
                    My data will be stored securely and shared only as permitted under VAWA
                    confidentiality protections.
                  </p>
                </div>
                <FormCheckbox
                  id="field-consentToDataCollection"
                  label="I consent to data collection *"
                  checked={formData.consents?.consentToDataCollection === true}
                  onCheckedChange={checked =>
                    handleConsentChange('consentToDataCollection', checked === true)
                  }
                />
                <FieldError>{consentToDataCollectionError}</FieldError>
              </FieldContent>
            </Field>

            <Field data-invalid={!!hmisParticipationStatusError}>
              <FieldTitle>HMIS Participation</FieldTitle>
              <FieldContent>
                <div className="consent-text">
                  <p>
                    The Homeless Management Information System (HMIS) is a database used by homeless
                    service providers to coordinate care and track outcomes. Your participation is:
                  </p>
                  <ul>
                    <li>Voluntary - you may decline without affecting your services</li>
                    <li>
                      Beneficial - helps coordinate care across providers and improves service
                      quality
                    </li>
                    <li>Protected - data is secured and access is strictly controlled</li>
                  </ul>
                  <p>
                    <strong>VAWA Protection:</strong> If you are fleeing domestic violence, you may
                    request enhanced confidentiality protections.
                  </p>
                </div>

                <RadioGroup
                  value={formData.consents?.hmisParticipationStatus || 'PENDING'}
                  onValueChange={handleHmisParticipationChange}
                >
                  <div className="flex flex-col gap-2">
                    <div className="flex items-center gap-2">
                      <RadioGroupItem
                        value="PARTICIPATING"
                        id="field-hmisParticipation-participating"
                      />
                      <Label htmlFor="field-hmisParticipation-participating">
                        I consent to HMIS participation
                      </Label>
                    </div>
                    <div className="flex items-center gap-2">
                      <RadioGroupItem
                        value="NON_PARTICIPATING"
                        id="field-hmisParticipation-nonParticipating"
                      />
                      <Label htmlFor="field-hmisParticipation-nonParticipating">
                        I decline HMIS participation
                      </Label>
                    </div>
                    <div className="flex items-center gap-2">
                      <RadioGroupItem value="VAWA_EXEMPT" id="field-hmisParticipation-vawaExempt" />
                      <Label htmlFor="field-hmisParticipation-vawaExempt">
                        VAWA-exempt (enhanced confidentiality)
                      </Label>
                    </div>
                  </div>
                </RadioGroup>

                <FieldError>{hmisParticipationStatusError}</FieldError>

                {formData.consents?.hmisParticipationStatus === 'NON_PARTICIPATING' &&
                  hmisParticipationWarning && (
                    <div className="alert alert-warning" role="alert">
                      <span className="alert-icon">??</span>
                      <span>{hmisParticipationWarning}</span>
                    </div>
                  )}

                {formData.consents?.hmisParticipationStatus === 'VAWA_EXEMPT' &&
                  vawaExemptWarning && (
                    <div className="alert alert-warning" role="alert">
                      <span className="alert-icon">??</span>
                      <span>{vawaExemptWarning}</span>
                    </div>
                  )}
              </FieldContent>
            </Field>
          </FieldGroup>
        </FieldSet>

        {/* Section 4: Digital Signature */}
        {hasAnyConsent && (
          <FieldSet className="signature-section">
            <FieldLegend>Digital Signature</FieldLegend>
            <FieldDescription>
              Please provide your signature to confirm all consents above.
            </FieldDescription>

            <div className="signature-tabs">
              <button
                type="button"
                className={`tab ${signatureTab === 'type' ? 'active' : ''}`}
                onClick={() => setSignatureTab('type')}
              >
                Type Name
              </button>
              <button
                type="button"
                className={`tab ${signatureTab === 'draw' ? 'active' : ''}`}
                onClick={() => setSignatureTab('draw')}
              >
                Draw Signature
              </button>
            </div>

            {signatureTab === 'type' && (
              <FieldGroup>
                <Field data-invalid={!!digitalSignatureError}>
                  <FieldLabel htmlFor="field-typedName">
                    Type Your Full Name <span className="text-destructive">*</span>
                  </FieldLabel>
                  <FieldContent>
                    <Input
                      id="field-typedName"
                      type="text"
                      value={formData.digitalSignature?.typedName || ''}
                      onChange={e => handleTypedNameChange(e.target.value)}
                      placeholder="Enter your full legal name"
                      className="signature-input"
                      aria-invalid={!!digitalSignatureError}
                    />
                    <FieldError>{digitalSignatureError}</FieldError>
                  </FieldContent>
                </Field>
              </FieldGroup>
            )}

            {signatureTab === 'draw' && (
              <FieldGroup>
                <Field data-invalid={!!digitalSignatureError}>
                  <FieldTitle>Draw Signature</FieldTitle>
                  <FieldContent>
                    <canvas
                      ref={canvasRef}
                      width={600}
                      height={200}
                      className="signature-canvas"
                      onMouseDown={startDrawing}
                      onMouseMove={draw}
                      onMouseUp={stopDrawing}
                      onMouseLeave={stopDrawing}
                    />
                    <Button type="button" variant="secondary" size="sm" onClick={clearSignature}>
                      Clear Signature
                    </Button>
                    <FieldError>{digitalSignatureError}</FieldError>
                  </FieldContent>
                </Field>
              </FieldGroup>
            )}

            {formData.digitalSignature?.signed && (
              <div className="signature-metadata">
                <p>
                  Signed on {new Date(formData.digitalSignature.timestamp!).toLocaleString()}
                </p>
              </div>
            )}
          </FieldSet>
        )}

        {/* Form Actions */}
        <div className="form-actions">
          <Button type="button" variant="secondary" onClick={onBack}>
            Back to Step 1
          </Button>
          <Button type="submit">Continue to Step 3</Button>
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



/**
 * Step 1: Initial Contact / Referral Intake
 *
 * Captures minimal safe information before eligibility determined.
 * VAWA-compliant: Uses alias/initials instead of full name.
 */

import React, { useState, useEffect } from "react";
import type { InitialContactData, ValidationError } from "../utils/types";
import { validateStep1 } from "../lib/validation";
import { REFERRAL_SOURCES, generateTempClientId } from "../index";
import {
  Button,
  Field,
  FieldContent,
  FieldError,
  FieldGroup,
  FieldLabel,
  FieldDescription,
  FieldLegend,
  FieldSeparator,
  FieldSet,
  FormCheckbox,
  FormSelect,
  Input,
  Label,
  RadioGroup,
  RadioGroupItem,
} from "@haven/ui";

interface Step1Props {
  data: Partial<InitialContactData>;
  errors: ValidationError[];
  onChange: (updates: Partial<InitialContactData>) => void;
  onComplete: (data: InitialContactData) => void;
  onBack?: () => void;
}

export const Step1_InitialContact: React.FC<Step1Props> = ({
  data,
  errors,
  onChange,
  onComplete,
  onBack,
}) => {
  const [formData, setFormData] = useState<Partial<InitialContactData>>({
    contactDate: data.contactDate || new Date().toISOString().split("T")[0],
    contactTime: data.contactTime || new Date().toTimeString().slice(0, 5),
    safeToContactNow: data.safeToContactNow ?? null,
    needsImmediateCrisisIntervention:
      data.needsImmediateCrisisIntervention || false,
    ...data,
  });

  const [validationErrors, setValidationErrors] =
    useState<ValidationError[]>(errors);
  const [showCrisisAlert, setShowCrisisAlert] = useState(false);

  // Update form data when props change
  useEffect(() => {
    setFormData((prev) => ({ ...prev, ...data }));
  }, [data]);

  // Update validation errors when props change
  useEffect(() => {
    setValidationErrors(errors);
  }, [errors]);

  // Show crisis alert if unsafe
  useEffect(() => {
    setShowCrisisAlert(formData.safeToContactNow === false);
  }, [formData.safeToContactNow]);

  const handleChange = (field: keyof InitialContactData, value: any) => {
    const updates = { [field]: value };
    setFormData((prev) => ({ ...prev, ...updates }));
    onChange(updates);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    // Generate temp client ID if not present
    if (!formData.tempClientId) {
      formData.tempClientId = generateTempClientId();
    }

    // Validate
    const result = validateStep1(formData as InitialContactData);
    setValidationErrors(result.errors);

    if (result.isValid) {
      onComplete(formData as InitialContactData);
    } else {
      // Scroll to first error
      const firstErrorField = result.errors[0]?.field;
      if (firstErrorField) {
        document.getElementById(`field-${firstErrorField}`)?.focus();
      }
    }
  };

  const getFieldError = (fieldName: string): string | undefined => {
    return validationErrors.find((e) => e.field === fieldName)?.message;
  };

  const contactDateError = getFieldError("contactDate");
  const contactTimeError = getFieldError("contactTime");
  const referralSourceError = getFieldError("referralSource");
  const referralSourceDetailsError = getFieldError("referralSourceDetails");
  const clientAliasError = getFieldError("clientAlias");
  const safeToContactNowError = getFieldError("safeToContactNow");
  const intakeWorkerNameError = getFieldError("intakeWorkerName");

  const safeToContactNowValue =
    formData.safeToContactNow === true
      ? "yes"
      : formData.safeToContactNow === false
      ? "no"
      : "unknown";

  const handleSafeToContactChange = (value: string) => {
    if (value === "yes") {
      handleChange("safeToContactNow", true);
    } else if (value === "no") {
      handleChange("safeToContactNow", false);
    } else {
      handleChange("safeToContactNow", null);
    }
  };

  return (
    <div className="intake-step step-1">
      <div className="step-header">
        <h2>Step 1: Initial Contact / Referral Intake</h2>
        <p className="step-description">
          Capture minimal safe information before eligibility is determined. Use
          alias or initials to protect client identity.
        </p>
      </div>

      {/* Crisis Alert Banner */}
      {showCrisisAlert && (
        <div className="alert alert-danger" role="alert">
          <div className="alert-icon">⚠️</div>
          <div className="alert-content">
            <strong>Client Safety Alert</strong>
            <p>
              Client may need immediate crisis intervention. Consider routing to
              emergency protocol or proceeding directly to Step 3 (Risk
              Assessment).
            </p>
          </div>
        </div>
      )}

      <form onSubmit={handleSubmit} className="intake-form">
        {/* Section 1: Contact Date/Time */}
        <FieldSet>
          <FieldLegend>Contact Information</FieldLegend>
          <FieldGroup>
            <Field data-invalid={!!contactDateError}>
              <FieldLabel htmlFor="field-contactDate">
                Contact Date <span className="text-destructive">*</span>
              </FieldLabel>
              <FieldContent>
                <Input
                  id="field-contactDate"
                  type="date"
                  value={formData.contactDate || ""}
                  onChange={(e) => handleChange("contactDate", e.target.value)}
                  max={new Date().toISOString().split("T")[0]}
                  aria-invalid={!!contactDateError}
                  aria-describedby={
                    contactDateError ? "error-contactDate" : undefined
                  }
                />
                <FieldError id="error-contactDate">
                  {contactDateError}
                </FieldError>
              </FieldContent>
            </Field>
            <Field data-invalid={!!contactTimeError}>
              <FieldLabel htmlFor="field-contactTime">
                Contact Time <span className="text-destructive">*</span>
              </FieldLabel>
              <FieldContent>
                <Input
                  id="field-contactTime"
                  type="time"
                  value={formData.contactTime || ""}
                  onChange={(e) => handleChange("contactTime", e.target.value)}
                  aria-invalid={!!contactTimeError}
                  aria-describedby={
                    contactTimeError ? "error-contactTime" : undefined
                  }
                />
                <FieldError id="error-contactTime">
                  {contactTimeError}
                </FieldError>
              </FieldContent>
            </Field>
          </FieldGroup>
        </FieldSet>

        {/* Section 2: Referral Source */}
        <FieldSet>
          <FieldLegend>Referral Information</FieldLegend>
          <FieldSeparator />
          <FieldGroup>
            <Field data-invalid={!!referralSourceError}>
              <FieldLabel htmlFor="field-referralSource">
                Referral Source <span className="text-destructive">*</span>
              </FieldLabel>
              <FieldContent>
                <FormSelect
                  id="field-referralSource"
                  value={formData.referralSource || ""}
                  onChange={(value) => handleChange("referralSource", value)}
                  options={Object.entries(REFERRAL_SOURCES).map(
                    ([key, label]) => ({
                      value: key,
                      label,
                    })
                  )}
                  error={referralSourceError}
                />
              </FieldContent>
            </Field>
            {/* Conditional: If "OTHER" selected */}
            {formData.referralSource === "OTHER" && (
              <Field data-invalid={!!referralSourceDetailsError}>
                <FieldLabel htmlFor="field-referralSourceDetails">
                  Please Specify <span className="text-destructive">*</span>
                </FieldLabel>
                <FieldContent>
                  <Input
                    id="field-referralSourceDetails"
                    type="text"
                    value={formData.referralSourceDetails || ""}
                    onChange={(e) =>
                      handleChange("referralSourceDetails", e.target.value)
                    }
                    placeholder="Enter referral source details"
                    aria-invalid={!!referralSourceDetailsError}
                    aria-describedby={
                      referralSourceDetailsError
                        ? "error-referralSourceDetails"
                        : undefined
                    }
                  />
                  <FieldError id="error-referralSourceDetails">
                    {referralSourceDetailsError}
                  </FieldError>
                </FieldContent>
              </Field>
            )}
          </FieldGroup>
        </FieldSet>

        {/* Section 3: Client Alias */}
        <FieldSet>
          <FieldLegend>Client Identification</FieldLegend>
          <FieldGroup>
            <Field data-invalid={!!clientAliasError}>
              <FieldLabel htmlFor="field-clientAlias">
                Client Alias or Initials{" "}
                <span className="text-destructive">*</span>
              </FieldLabel>
              <FieldContent>
                <Input
                  id="field-clientAlias"
                  type="text"
                  value={formData.clientAlias || ""}
                  onChange={(e) => handleChange("clientAlias", e.target.value)}
                  placeholder="e.g., Jane D., JD, Client 123"
                  maxLength={50}
                  aria-invalid={!!clientAliasError}
                  aria-describedby={
                    clientAliasError ? "error-clientAlias" : undefined
                  }
                />
                <FieldDescription>
                  Use alias or initials to protect client identity. Full name
                  will be collected after consent is obtained.
                </FieldDescription>
                <FieldError id="error-clientAlias">{clientAliasError}</FieldError>
              </FieldContent>
            </Field>
            <Field>
              <FieldLabel htmlFor="field-clientInitials">
                Client Initials (Optional)
              </FieldLabel>
              <FieldContent>
                <Input
                  id="field-clientInitials"
                  type="text"
                  value={formData.clientInitials || ""}
                  onChange={(e) =>
                    handleChange("clientInitials", e.target.value.toUpperCase())
                  }
                  placeholder="e.g., JD"
                  maxLength={5}
                  pattern="[A-Z]{1,5}"
                  style={{ textTransform: "uppercase" }}
                />
              </FieldContent>
            </Field>
          </FieldGroup>
        </FieldSet>

        {/* Section 4: Immediate Safety Check */}
        <FieldSet>
          <FieldLegend>Immediate Safety Assessment</FieldLegend>
          <FieldGroup>
            <Field data-invalid={!!safeToContactNowError}>
              <FieldLabel htmlFor="field-safeToContactNow-yes">
                Is it safe to contact the client now?{" "}
                <span className="text-destructive">*</span>
              </FieldLabel>
              <FieldContent>
                <RadioGroup
                  value={safeToContactNowValue}
                  onValueChange={handleSafeToContactChange}
                >
                  <div className="flex items-center gap-2">
                    <RadioGroupItem
                      value="yes"
                      id="field-safeToContactNow-yes"
                    />
                    <Label htmlFor="field-safeToContactNow-yes">
                      Yes, it is safe
                    </Label>
                  </div>
                  <div className="flex items-center gap-2">
                    <RadioGroupItem value="no" id="field-safeToContactNow-no" />
                    <Label htmlFor="field-safeToContactNow-no">
                      No, client is not safe
                    </Label>
                  </div>
                  <div className="flex items-center gap-2">
                    <RadioGroupItem
                      value="unknown"
                      id="field-safeToContactNow-unknown"
                    />
                    <Label htmlFor="field-safeToContactNow-unknown">
                      Unsure / Client didn't specify
                    </Label>
                  </div>
                </RadioGroup>
                <FieldError id="error-safeToContactNow">
                  {safeToContactNowError}
                </FieldError>
              </FieldContent>
            </Field>

            <Field>
              <FieldContent>
                <FormCheckbox
                  id="field-needsImmediateCrisisIntervention"
                  label="Client needs immediate crisis intervention"
                  checked={formData.needsImmediateCrisisIntervention === true}
                  onCheckedChange={(checked) =>
                    handleChange(
                      "needsImmediateCrisisIntervention",
                      checked === true
                    )
                  }
                />
              </FieldContent>
            </Field>
          </FieldGroup>
        </FieldSet>

        {/* Section 5: Intake Worker */}
        <FieldSet>
          <FieldLegend>Staff Information</FieldLegend>
          <FieldGroup>
            <Field data-invalid={!!intakeWorkerNameError}>
              <FieldLabel htmlFor="field-intakeWorkerName">
                Intake Worker Name <span className="text-destructive">*</span>
              </FieldLabel>
              <FieldContent>
                <Input
                  id="field-intakeWorkerName"
                  type="text"
                  value={formData.intakeWorkerName || ""}
                  onChange={(e) =>
                    handleChange("intakeWorkerName", e.target.value)
                  }
                  placeholder="Enter your name"
                  aria-invalid={!!intakeWorkerNameError}
                  aria-describedby={
                    intakeWorkerNameError ? "error-intakeWorkerName" : undefined
                  }
                />
                <FieldError id="error-intakeWorkerName">
                  {intakeWorkerNameError}
                </FieldError>
              </FieldContent>
            </Field>
          </FieldGroup>
        </FieldSet>

        {/* Form Actions */}
        <div className="form-actions">
          {onBack && (
            <Button type="button" variant="secondary" onClick={onBack}>
              Back
            </Button>
          )}
          <Button type="submit">
            Continue to Step 2
          </Button>
        </div>
      </form>

      {/* Summary of validation errors */}
      {validationErrors.length > 0 && (
        <div className="validation-summary" role="alert">
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
    </div>
  );
};

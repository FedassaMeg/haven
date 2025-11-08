/**
 * Step 3: Crisis / Risk Assessment
 *
 * Conducts lethality screening and immediate safety assessment to prioritize client needs.
 * Uses validated screening tools and auto-calculates risk levels.
 */

import {
  Button,
  Field,
  FieldContent,
  FieldError,
  FieldGroup,
  FieldLabel,
  FieldLegend,
  FieldSeparator,
  FieldSet,
  FormCheckbox,
  FormSelect,
  Input,
  Label,
  RadioGroup,
  RadioGroupItem,
  Textarea,
} from "@haven/ui";
import React, { useEffect, useState } from "react";
import {
  LETHALITY_SCREENING_TOOLS,
  POLICE_INVOLVEMENT_STATUS,
  PROTECTIVE_ORDER_STATUS,
  RISK_LEVELS,
  calculateOverallRiskLevel,
  shouldAutoRouteToSafety,
} from "../index";
import { validateStep3 } from "../lib/validation";
import type {
  LethalityScreeningResponses,
  RiskAssessmentData,
  RiskLevel,
  ValidationError,
} from "../utils/types";
import { LethalityToolRenderer } from "./lethalityTools/ToolSelector";

interface Step3Props {
  data: Partial<RiskAssessmentData>;
  errors: ValidationError[];
  warnings: ValidationError[];
  onChange: (updates: Partial<RiskAssessmentData>) => void;
  onComplete: (data: RiskAssessmentData) => void;
  onBack: () => void;
  assessorInfo?: { id: string; name: string };
}

export const Step3_RiskAssessment: React.FC<Step3Props> = ({
  data,
  errors,
  warnings,
  onChange,
  onComplete,
  onBack,
  assessorInfo,
}) => {
  // Provide default assessor info if not passed
  const defaultAssessorInfo = assessorInfo || {
    id: "TEMP-ASSESSOR",
    name: "Current User",
  };

  const [formData, setFormData] = useState<Partial<RiskAssessmentData>>({
    lethalityScreening: {
      screeningTool: "NONE",
      riskLevel: "NOT_ASSESSED",
      assessmentDate: new Date().toISOString().split("T")[0],
      assessorId: defaultAssessorInfo.id,
      assessorName: defaultAssessorInfo.name,
      ...data.lethalityScreening,
    },
    immediateSafety: {
      currentlySafe: null,
      safePlaceToStay: null,
      needsEmergencyShelter: null,
      hasImmediateMedicalNeeds: null,
      policeInvolvement: "NONE",
      protectiveOrderStatus: "NONE",
      currentlyFleeing: null,
      safeToReturnHome: null,
      locationCompromised: null,
      ...data.immediateSafety,
    },
    dependents: {
      hasMinors: false,
      hasInfants: false,
      hasSpecialNeeds: false,
      hasPets: false,
      childrenCurrentlySafe: null,
      cpsInvolvement: null,
      ...data.dependents,
    },
    overallRiskLevel: "NOT_ASSESSED",
    autoRouteToSafety: false,
    assessedBy: defaultAssessorInfo.name,
    assessmentDateTime: new Date().toISOString(),
    ...data,
  });

  const [validationErrors, setValidationErrors] =
    useState<ValidationError[]>(errors);
  const [validationWarnings, setValidationWarnings] =
    useState<ValidationError[]>(warnings);

  // Update when props change
  useEffect(() => {
    setFormData((prev) => ({ ...prev, ...data }));
  }, [data]);

  useEffect(() => {
    setValidationErrors(errors);
    setValidationWarnings(warnings);
  }, [errors, warnings]);

  // Calculate overall risk level when relevant data changes
  useEffect(() => {
    if (formData.lethalityScreening?.riskLevel && formData.immediateSafety) {
      const overallRisk = calculateOverallRiskLevel(
        formData.lethalityScreening.riskLevel,
        formData.immediateSafety.currentlySafe,
        formData.immediateSafety.safePlaceToStay,
        formData.immediateSafety.needsEmergencyShelter
      );

      const autoRoute = shouldAutoRouteToSafety(
        overallRisk,
        formData.immediateSafety.currentlySafe,
        formData.immediateSafety.needsEmergencyShelter,
        formData.dependents?.childrenCurrentlySafe || null
      );

      setFormData((prev) => ({
        ...prev,
        overallRiskLevel: overallRisk,
        autoRouteToSafety: autoRoute,
      }));

      onChange({
        overallRiskLevel: overallRisk,
        autoRouteToSafety: autoRoute,
      });
    }
  }, [
    formData.lethalityScreening?.riskLevel,
    formData.immediateSafety?.currentlySafe,
    formData.immediateSafety?.safePlaceToStay,
    formData.immediateSafety?.needsEmergencyShelter,
    formData.dependents?.childrenCurrentlySafe,
  ]);

  const handleChange = (field: keyof RiskAssessmentData, value: any) => {
    const updates = { [field]: value };
    setFormData((prev) => ({ ...prev, ...updates }));
    onChange(updates);
  };

  const handleNestedChange = (
    parent: keyof RiskAssessmentData,
    field: string,
    value: any
  ) => {
    const updates = {
      [parent]: {
        ...(formData[parent] as any),
        [field]: value,
      },
    };
    setFormData((prev) => ({ ...prev, ...updates }));
    onChange(updates);
  };

  const handleLethalityResponsesChange = (
    responses: LethalityScreeningResponses
  ) => {
    handleNestedChange("lethalityScreening", "responses", responses);
  };

  const handleScoreCalculated = (
    score: number,
    maxScore: number,
    riskLevel: RiskLevel
  ) => {
    setFormData((prev) => ({
      ...prev,
      lethalityScreening: {
        ...prev.lethalityScreening!,
        score,
        maxScore,
        riskLevel,
      },
    }));

    onChange({
      lethalityScreening: {
        ...formData.lethalityScreening!,
        score,
        maxScore,
        riskLevel,
      },
    });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    // Validate
    const result = validateStep3(formData as RiskAssessmentData);
    setValidationErrors(result.errors);
    setValidationWarnings(result.warnings);

    if (result.isValid) {
      onComplete(formData as RiskAssessmentData);
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

  const getFieldWarning = (fieldName: string): string | undefined => {
    return validationWarnings.find((w) => w.field === fieldName)?.message;
  };

  const booleanToRadioValue = (
    value: boolean | null | undefined,
    includeUnknown = true
  ) => {
    if (value === true) return "yes";
    if (value === false) return "no";
    return includeUnknown ? "unknown" : undefined;
  };

  const radioValueToBoolean = (value: string): boolean | null => {
    if (value === "yes") return true;
    if (value === "no") return false;
    return null;
  };

  const riskLevelConfig = formData.overallRiskLevel
    ? RISK_LEVELS[formData.overallRiskLevel]
    : null;
  const isHighRisk =
    formData.overallRiskLevel === "HIGH" ||
    formData.overallRiskLevel === "SEVERE";

  return (
    <div className="intake-step step-3">
      <div className="step-header">
        <h2>Step 3: Crisis / Risk Assessment</h2>
        <p className="step-description">
          Conduct lethality screening and immediate safety assessment to
          prioritize client needs and coordinate appropriate interventions.
        </p>
      </div>

      {/* High Risk Alert Banner */}
      {isHighRisk && (
        <div className="alert alert-danger" role="alert">
          <div className="alert-icon">⚠️</div>
          <div className="alert-content">
            <strong>HIGH PRIORITY CLIENT</strong>
            <p>
              This client has been flagged for immediate safety intervention.
              Consider:
            </p>
            <ul>
              <li>Emergency shelter placement</li>
              <li>Safety planning session</li>
              <li>Medical referral (if needed)</li>
              <li>Law enforcement notification (if applicable)</li>
            </ul>
          </div>
        </div>
      )}

      <form onSubmit={handleSubmit} className="intake-form">
        {/* Section 1: Lethality Screening Tool */}
        <div>
          <FieldSet>
            <FieldLegend>Lethality Screening</FieldLegend>
            <FieldSeparator />
            <FieldGroup>
              <Field orientation="responsive">
                <FieldContent>
                  <FieldLabel htmlFor="field-screeningTool">
                    Screening Tool*
                  </FieldLabel>
                </FieldContent>
                <FormSelect
                  id="field-screeningTool"
                  value={formData.lethalityScreening?.screeningTool || ""}
                  onChange={(value) =>
                    handleNestedChange(
                      "lethalityScreening",
                      "screeningTool",
                      value
                    )
                  }
                  error={getFieldError("screeningTool")}
                  options={Object.entries(LETHALITY_SCREENING_TOOLS).map(
                    ([key, label]) => ({
                      value: key,
                      label,
                    })
                  )}
                />
              </Field>
            </FieldGroup>
            <FieldGroup>
              <Field>
                <FieldContent>
                  <FieldLabel htmlFor="field-assessmentDate">
                    Assessment Date *
                  </FieldLabel>
                </FieldContent>
                <Input
                  id="field-assessmentDate"
                  type="date"
                  value={formData.lethalityScreening?.assessmentDate || ""}
                  onChange={(e) =>
                    handleNestedChange(
                      "lethalityScreening",
                      "assessmentDate",
                      e.target.value
                    )
                  }
                  max={new Date().toISOString().split("T")[0]}
                  className={getFieldError("assessmentDate") ? "error" : ""}
                />
                <FieldError>
                  {getFieldError("assessmentDate") && (
                    <span className="field-error" role="alert">
                      {getFieldError("assessmentDate")}
                    </span>
                  )}
                </FieldError>
              </Field>
              <Field>
                <FieldContent>
                  <FieldLabel htmlFor="field-assessorName">
                    Assessor Name
                  </FieldLabel>
                </FieldContent>
                <Input
                  id="field-assessorName"
                  type="text"
                  value={formData.lethalityScreening?.assessorName || ""}
                  readOnly
                  disabled
                  className="readonly"
                />
              </Field>
            </FieldGroup>
          </FieldSet>

          {/* Render tool-specific questionnaire */}
          {formData.lethalityScreening?.screeningTool &&
            formData.lethalityScreening.screeningTool !== "NONE" &&
            formData.lethalityScreening.screeningTool !== "OTHER" && (
              <div className="lethality-tool-container">
                <LethalityToolRenderer
                  tool={formData.lethalityScreening.screeningTool}
                  responses={formData.lethalityScreening.responses || {}}
                  onChange={handleLethalityResponsesChange}
                  onScoreCalculated={handleScoreCalculated}
                />
              </div>
            )}

          {/* Manual Risk Level Selector - for NONE or OTHER tools */}
          {formData.lethalityScreening?.screeningTool &&
            (formData.lethalityScreening.screeningTool === "NONE" ||
              formData.lethalityScreening.screeningTool === "OTHER") && (
              <Field>
                <FieldContent>
                  <FieldLabel htmlFor="field-manualRiskLevel">Risk Level Assessment*</FieldLabel>
                </FieldContent>
                <FormSelect
                  id="field-manualRiskLevel"
                  value={
                    formData.lethalityScreening?.riskLevel || "NOT_ASSESSED"
                  }
                  onChange={(value) =>
                    handleNestedChange(
                      "lethalityScreening",
                      "riskLevel",
                      value as RiskLevel
                    )
                  }
                  options={Object.entries(RISK_LEVELS).map(([key, config]) => ({
                    value: key,
                    label: `${config.label} - ${config.description}`,
                  }))}
                  error={getFieldError("riskLevel")}
                />
                <span className="field-help">
                  Based on your professional assessment, select the appropriate
                  risk level for this client.
                </span>
              </Field>
            )}

          <div className="grid w-full max-w-sm items-center gap-3">
            <Label htmlFor="field-assessorNotes">
              Assessor Notes (Optional)
            </Label>
            <Textarea
              id="field-assessorNotes"
              value={formData.lethalityScreening?.assessorNotes || ""}
              onChange={(e) =>
                handleNestedChange(
                  "lethalityScreening",
                  "assessorNotes",
                  e.target.value
                )
              }
              placeholder="Additional observations or context"
              rows={3}
            />
          </div>
        </div>

        {/* Section 2: Immediate Safety Assessment */}
        <FieldSet>
          <FieldLegend>Immediate Safety Assessment</FieldLegend>
          <FieldSeparator />
          <FieldGroup>
            <Field>
              <FieldLabel htmlFor="field-currentlySafe-yes">
                Is the client currently safe?
                {" "}
                <span className="text-destructive">*</span>
              </FieldLabel>
              <FieldContent>
                <RadioGroup
                  value={booleanToRadioValue(formData.immediateSafety?.currentlySafe)}
                  onValueChange={(value) =>
                    handleNestedChange(
                      "immediateSafety",
                      "currentlySafe",
                      radioValueToBoolean(value)
                    )
                  }
                  className="grid gap-2 sm:grid-cols-3"
                  aria-invalid={!!getFieldError("currentlySafe")}
                >
                  <div className="flex items-center gap-2">
                    <RadioGroupItem value="yes" id="field-currentlySafe-yes" />
                    <Label htmlFor="field-currentlySafe-yes">Yes</Label>
                  </div>
                  <div className="flex items-center gap-2">
                    <RadioGroupItem value="no" id="field-currentlySafe-no" />
                    <Label htmlFor="field-currentlySafe-no">No</Label>
                  </div>
                  <div className="flex items-center gap-2">
                    <RadioGroupItem value="unknown" id="field-currentlySafe-unknown" />
                    <Label htmlFor="field-currentlySafe-unknown">Unsure</Label>
                  </div>
                </RadioGroup>
                <FieldError>{getFieldError("currentlySafe")}</FieldError>
              </FieldContent>
            </Field>
            <Field>
              <FieldLabel htmlFor="field-safePlaceToStay-yes">
                Does client have a safe place to stay tonight?
                {" "}
                <span className="text-destructive">*</span>
              </FieldLabel>
              <FieldContent>
                <RadioGroup
                  value={booleanToRadioValue(formData.immediateSafety?.safePlaceToStay)}
                  onValueChange={(value) =>
                    handleNestedChange(
                      "immediateSafety",
                      "safePlaceToStay",
                      radioValueToBoolean(value)
                    )
                  }
                  className="grid gap-2 sm:grid-cols-3"
                  aria-invalid={!!getFieldError("safePlaceToStay")}
                >
                  <div className="flex items-center gap-2">
                    <RadioGroupItem value="yes" id="field-safePlaceToStay-yes" />
                    <Label htmlFor="field-safePlaceToStay-yes">Yes</Label>
                  </div>
                  <div className="flex items-center gap-2">
                    <RadioGroupItem value="no" id="field-safePlaceToStay-no" />
                    <Label htmlFor="field-safePlaceToStay-no">No</Label>
                  </div>
                  <div className="flex items-center gap-2">
                    <RadioGroupItem value="unknown" id="field-safePlaceToStay-unknown" />
                    <Label htmlFor="field-safePlaceToStay-unknown">Unsure</Label>
                  </div>
                </RadioGroup>
                <FieldError>{getFieldError("safePlaceToStay")}</FieldError>
              </FieldContent>
            </Field>
            <Field>
              <FieldLabel>Immediate Needs</FieldLabel>
              <FieldContent>
                <div className="flex flex-col gap-2">
                  <FormCheckbox
                    id="field-needsEmergencyShelter"
                    label="Client needs emergency shelter immediately"
                    checked={formData.immediateSafety?.needsEmergencyShelter === true}
                    onCheckedChange={(checked) =>
                      handleNestedChange(
                        "immediateSafety",
                        "needsEmergencyShelter",
                        checked === true
                      )
                    }
                  />
                  <FormCheckbox
                    id="field-hasImmediateMedicalNeeds"
                    label="Client has immediate medical needs"
                    checked={formData.immediateSafety?.hasImmediateMedicalNeeds === true}
                    onCheckedChange={(checked) =>
                      handleNestedChange(
                        "immediateSafety",
                        "hasImmediateMedicalNeeds",
                        checked === true
                      )
                    }
                  />
                </div>
              </FieldContent>
            </Field>
            {formData.immediateSafety?.hasImmediateMedicalNeeds && (
              <Field>
                <FieldLabel htmlFor="field-medicalNeedsDescription">
                  Describe Medical Needs
                  {" "}
                  <span className="text-destructive">*</span>
                </FieldLabel>
                <FieldContent>
                  <Textarea
                    id="field-medicalNeedsDescription"
                    value={formData.immediateSafety?.medicalNeedsDescription || ""}
                    onChange={(e) =>
                      handleNestedChange(
                        "immediateSafety",
                        "medicalNeedsDescription",
                        e.target.value
                      )
                    }
                    placeholder="Describe the immediate medical needs"
                    rows={3}
                    aria-invalid={!!getFieldError("medicalNeedsDescription")}
                  />
                  <FieldError>{getFieldError("medicalNeedsDescription")}</FieldError>
                </FieldContent>
              </Field>
            )}
          </FieldGroup>

          <FieldSeparator>Police &amp; Legal Status</FieldSeparator>
          <FieldGroup>
            <Field>
              <FieldLabel htmlFor="field-policeInvolvement">
                Police Involvement
                {" "}
                <span className="text-destructive">*</span>
              </FieldLabel>
              <FieldContent>
                <FormSelect
                  id="field-policeInvolvement"
                  value={formData.immediateSafety?.policeInvolvement || ""}
                  onChange={(value) =>
                    handleNestedChange(
                      "immediateSafety",
                      "policeInvolvement",
                      value
                    )
                  }
                  options={Object.entries(POLICE_INVOLVEMENT_STATUS).map(
                    ([key, label]) => ({
                      value: key,
                      label,
                    })
                  )}
                  error={getFieldError("policeInvolvement")}
                />
              </FieldContent>
            </Field>

            {formData.immediateSafety?.policeInvolvement &&
              formData.immediateSafety.policeInvolvement !== "NONE" && (
                <Field>
                  <FieldLabel htmlFor="field-policeReportNumber">
                    Police Report Number
                  </FieldLabel>
                  <FieldContent>
                    <Input
                      id="field-policeReportNumber"
                      type="text"
                      value={formData.immediateSafety?.policeReportNumber || ""}
                      onChange={(e) =>
                        handleNestedChange(
                          "immediateSafety",
                          "policeReportNumber",
                          e.target.value
                        )
                      }
                      placeholder="Enter report number"
                    />
                  </FieldContent>
                </Field>
              )}

            <Field>
              <FieldLabel htmlFor="field-protectiveOrderStatus">
                Protective Order Status
                {" "}
                <span className="text-destructive">*</span>
              </FieldLabel>
              <FieldContent>
                <FormSelect
                  id="field-protectiveOrderStatus"
                  value={formData.immediateSafety?.protectiveOrderStatus || ""}
                  onChange={(value) =>
                    handleNestedChange(
                      "immediateSafety",
                      "protectiveOrderStatus",
                      value
                    )
                  }
                  options={Object.entries(PROTECTIVE_ORDER_STATUS).map(
                    ([key, label]) => ({
                      value: key,
                      label,
                    })
                  )}
                  error={getFieldError("protectiveOrderStatus")}
                />
              </FieldContent>
            </Field>

            {formData.immediateSafety?.protectiveOrderStatus &&
              ["TEMPORARY", "PERMANENT", "EXPIRED", "VIOLATED"].includes(
                formData.immediateSafety.protectiveOrderStatus
              ) && (
                <>
                  <Field>
                    <FieldLabel htmlFor="field-protectiveOrderDate">
                      Protective Order Date
                    </FieldLabel>
                    <FieldContent>
                      <Input
                        id="field-protectiveOrderDate"
                        type="date"
                        value={
                          formData.immediateSafety?.protectiveOrderDate || ""
                        }
                        onChange={(e) =>
                          handleNestedChange(
                            "immediateSafety",
                            "protectiveOrderDate",
                            e.target.value
                          )
                        }
                        max={new Date().toISOString().split("T")[0]}
                        aria-invalid={!!getFieldError("protectiveOrderDate")}
                      />
                      <FieldError>{getFieldError("protectiveOrderDate")}</FieldError>
                    </FieldContent>
                  </Field>

                  <Field>
                    <FieldLabel htmlFor="field-protectiveOrderCaseNumber">
                      Court Case Number
                    </FieldLabel>
                    <FieldContent>
                      <Input
                        id="field-protectiveOrderCaseNumber"
                        type="text"
                        value={
                          formData.immediateSafety?.protectiveOrderCaseNumber ||
                          ""
                        }
                        onChange={(e) =>
                          handleNestedChange(
                            "immediateSafety",
                            "protectiveOrderCaseNumber",
                            e.target.value
                          )
                        }
                        placeholder="Enter case number"
                      />
                    </FieldContent>
                  </Field>
                </>
              )}
          </FieldGroup>

          <FieldSeparator>Flight Status</FieldSeparator>
          <FieldGroup>
            <Field>
              <FieldLabel htmlFor="field-currentlyFleeing-yes">
                Is client currently fleeing a DV situation?
              </FieldLabel>
              <FieldContent>
                <RadioGroup
                  value={booleanToRadioValue(
                    formData.immediateSafety?.currentlyFleeing,
                    false
                  )}
                  onValueChange={(value) =>
                    handleNestedChange(
                      "immediateSafety",
                      "currentlyFleeing",
                      radioValueToBoolean(value)
                    )
                  }
                  className="grid gap-2 sm:grid-cols-2"
                >
                  <div className="flex items-center gap-2">
                    <RadioGroupItem value="yes" id="field-currentlyFleeing-yes" />
                    <Label htmlFor="field-currentlyFleeing-yes">Yes</Label>
                  </div>
                  <div className="flex items-center gap-2">
                    <RadioGroupItem value="no" id="field-currentlyFleeing-no" />
                    <Label htmlFor="field-currentlyFleeing-no">No</Label>
                  </div>
                </RadioGroup>
              </FieldContent>
            </Field>

            {formData.immediateSafety?.currentlyFleeing && (
              <Field>
                <FieldLabel htmlFor="field-dateOfFlight">
                  Date of Flight
                </FieldLabel>
                <FieldContent>
                  <Input
                    id="field-dateOfFlight"
                    type="date"
                    value={formData.immediateSafety?.dateOfFlight || ""}
                    onChange={(e) =>
                      handleNestedChange(
                        "immediateSafety",
                        "dateOfFlight",
                        e.target.value
                      )
                    }
                    max={new Date().toISOString().split("T")[0]}
                  />
                </FieldContent>
              </Field>
            )}

            <Field>
              <FieldLabel htmlFor="field-safeToReturnHome-yes">
                Does client feel safe returning home?
              </FieldLabel>
              <FieldContent>
                <RadioGroup
                  value={booleanToRadioValue(
                    formData.immediateSafety?.safeToReturnHome
                  )}
                  onValueChange={(value) =>
                    handleNestedChange(
                      "immediateSafety",
                      "safeToReturnHome",
                      radioValueToBoolean(value)
                    )
                  }
                  className="grid gap-2 sm:grid-cols-3"
                >
                  <div className="flex items-center gap-2">
                    <RadioGroupItem value="yes" id="field-safeToReturnHome-yes" />
                    <Label htmlFor="field-safeToReturnHome-yes">Yes</Label>
                  </div>
                  <div className="flex items-center gap-2">
                    <RadioGroupItem value="no" id="field-safeToReturnHome-no" />
                    <Label htmlFor="field-safeToReturnHome-no">No</Label>
                  </div>
                  <div className="flex items-center gap-2">
                    <RadioGroupItem value="unknown" id="field-safeToReturnHome-unknown" />
                    <Label htmlFor="field-safeToReturnHome-unknown">Unsure</Label>
                  </div>
                </RadioGroup>
              </FieldContent>
            </Field>

            <Field>
              <FieldLabel htmlFor="field-locationCompromised-yes">
                Has client's location been compromised?
              </FieldLabel>
              <FieldContent>
                <RadioGroup
                  value={booleanToRadioValue(
                    formData.immediateSafety?.locationCompromised
                  )}
                  onValueChange={(value) =>
                    handleNestedChange(
                      "immediateSafety",
                      "locationCompromised",
                      radioValueToBoolean(value)
                    )
                  }
                  className="grid gap-2 sm:grid-cols-3"
                >
                  <div className="flex items-center gap-2">
                    <RadioGroupItem value="yes" id="field-locationCompromised-yes" />
                    <Label htmlFor="field-locationCompromised-yes">Yes</Label>
                  </div>
                  <div className="flex items-center gap-2">
                    <RadioGroupItem value="no" id="field-locationCompromised-no" />
                    <Label htmlFor="field-locationCompromised-no">No</Label>
                  </div>
                  <div className="flex items-center gap-2">
                    <RadioGroupItem value="unknown" id="field-locationCompromised-unknown" />
                    <Label htmlFor="field-locationCompromised-unknown">Unknown</Label>
                  </div>
                </RadioGroup>
              </FieldContent>
            </Field>
          </FieldGroup>
        </FieldSet>

        {/* Section 3: Dependents Information */}
        <FieldSet>
          <FieldLegend>Dependents Information</FieldLegend>
          <FieldSeparator />
          <FieldGroup>
            <Field>
              <FieldContent>
                <FormCheckbox
                  id="field-hasMinors"
                  label="Client has minor children"
                  checked={formData.dependents?.hasMinors === true}
                  onCheckedChange={(checked) =>
                    handleNestedChange(
                      "dependents",
                      "hasMinors",
                      checked === true
                    )
                  }
                />
              </FieldContent>
            </Field>

            {formData.dependents?.hasMinors && (
              <FieldGroup>
                <Field>
                  <FieldLabel htmlFor="field-numberOfMinors">
                    Number of Minors
                    {" "}
                    <span className="text-destructive">*</span>
                  </FieldLabel>
                  <FieldContent>
                    <Input
                      id="field-numberOfMinors"
                      type="number"
                      min="1"
                      value={formData.dependents?.numberOfMinors || ""}
                      onChange={(e) =>
                        handleNestedChange(
                          "dependents",
                          "numberOfMinors",
                          parseInt(e.target.value, 10) || 0
                        )
                      }
                      aria-invalid={!!getFieldError("numberOfMinors")}
                    />
                    <FieldError>{getFieldError("numberOfMinors")}</FieldError>
                  </FieldContent>
                </Field>

                <Field>
                  <FieldLabel htmlFor="field-minorAges">
                    Ages of Children (comma-separated)
                  </FieldLabel>
                  <FieldContent>
                    <Input
                      id="field-minorAges"
                      type="text"
                      value={formData.dependents?.minorAges?.join(", ") || ""}
                      onChange={(e) =>
                        handleNestedChange(
                          "dependents",
                          "minorAges",
                          e.target.value
                            .split(",")
                            .map((age) => parseInt(age.trim(), 10))
                            .filter((age) => !Number.isNaN(age))
                        )
                      }
                      placeholder="e.g., 3, 7, 12"
                    />
                  </FieldContent>
                </Field>

                <Field>
                  <FieldLabel>Additional Details</FieldLabel>
                  <FieldContent>
                    <div className="flex flex-col gap-2">
                      <FormCheckbox
                        id="field-hasInfants"
                        label="Any children under age 5?"
                        checked={formData.dependents?.hasInfants === true}
                        onCheckedChange={(checked) =>
                          handleNestedChange(
                            "dependents",
                            "hasInfants",
                            checked === true
                          )
                        }
                      />
                      <FormCheckbox
                        id="field-hasSpecialNeeds"
                        label="Dependents with special needs"
                        checked={formData.dependents?.hasSpecialNeeds === true}
                        onCheckedChange={(checked) =>
                          handleNestedChange(
                            "dependents",
                            "hasSpecialNeeds",
                            checked === true
                          )
                        }
                      />
                    </div>
                  </FieldContent>
                </Field>

                {formData.dependents?.hasSpecialNeeds && (
                  <Field>
                    <FieldLabel htmlFor="field-specialNeedsDetails">
                      Describe Special Needs
                    </FieldLabel>
                    <FieldContent>
                      <Textarea
                        id="field-specialNeedsDetails"
                        value={formData.dependents?.specialNeedsDetails || ""}
                        onChange={(e) =>
                          handleNestedChange(
                            "dependents",
                            "specialNeedsDetails",
                            e.target.value
                          )
                        }
                        placeholder="Describe any special needs or accommodations required"
                        rows={3}
                      />
                    </FieldContent>
                  </Field>
                )}

                <Field>
                  <FieldLabel htmlFor="field-childrenCurrentlySafe-yes">
                    Are children currently safe?
                  </FieldLabel>
                  <FieldContent>
                    <RadioGroup
                      value={booleanToRadioValue(
                        formData.dependents?.childrenCurrentlySafe
                      )}
                      onValueChange={(value) =>
                        handleNestedChange(
                          "dependents",
                          "childrenCurrentlySafe",
                          radioValueToBoolean(value)
                        )
                      }
                      className="grid gap-2 sm:grid-cols-3"
                    >
                      <div className="flex items-center gap-2">
                        <RadioGroupItem value="yes" id="field-childrenCurrentlySafe-yes" />
                        <Label htmlFor="field-childrenCurrentlySafe-yes">Yes</Label>
                      </div>
                      <div className="flex items-center gap-2">
                        <RadioGroupItem value="no" id="field-childrenCurrentlySafe-no" />
                        <Label htmlFor="field-childrenCurrentlySafe-no">No</Label>
                      </div>
                      <div className="flex items-center gap-2">
                        <RadioGroupItem value="unknown" id="field-childrenCurrentlySafe-unknown" />
                        <Label htmlFor="field-childrenCurrentlySafe-unknown">Unknown</Label>
                      </div>
                    </RadioGroup>
                    <FieldError>{getFieldError("childrenCurrentlySafe")}</FieldError>
                  </FieldContent>
                </Field>

                {getFieldWarning("childrenCurrentlySafe") && (
                  <div className="alert alert-warning" role="alert">
                    <span className="alert-icon">⚠️</span>
                    <span>{getFieldWarning("childrenCurrentlySafe")}</span>
                  </div>
                )}

                <Field>
                  <FieldLabel htmlFor="field-cpsInvolvement-yes">
                    CPS Involvement?
                  </FieldLabel>
                  <FieldContent>
                    <RadioGroup
                      value={booleanToRadioValue(formData.dependents?.cpsInvolvement)}
                      onValueChange={(value) =>
                        handleNestedChange(
                          "dependents",
                          "cpsInvolvement",
                          radioValueToBoolean(value)
                        )
                      }
                      className="grid gap-2 sm:grid-cols-3"
                    >
                      <div className="flex items-center gap-2">
                        <RadioGroupItem value="yes" id="field-cpsInvolvement-yes" />
                        <Label htmlFor="field-cpsInvolvement-yes">Yes</Label>
                      </div>
                      <div className="flex items-center gap-2">
                        <RadioGroupItem value="no" id="field-cpsInvolvement-no" />
                        <Label htmlFor="field-cpsInvolvement-no">No</Label>
                      </div>
                      <div className="flex items-center gap-2">
                        <RadioGroupItem value="unknown" id="field-cpsInvolvement-unknown" />
                        <Label htmlFor="field-cpsInvolvement-unknown">Unknown</Label>
                      </div>
                    </RadioGroup>
                    <FieldError>{getFieldError("cpsInvolvement")}</FieldError>
                  </FieldContent>
                </Field>

                {formData.dependents?.cpsInvolvement && (
                  <Field>
                    <FieldLabel htmlFor="field-cpsCaseNumber">
                      CPS Case Number
                    </FieldLabel>
                    <FieldContent>
                      <Input
                        id="field-cpsCaseNumber"
                        type="text"
                        value={formData.dependents?.cpsCaseNumber || ""}
                        onChange={(e) =>
                          handleNestedChange(
                            "dependents",
                            "cpsCaseNumber",
                            e.target.value
                          )
                        }
                        placeholder="Enter CPS case number"
                      />
                    </FieldContent>
                  </Field>
                )}
              </FieldGroup>
            )}

            <Field>
              <FieldContent>
                <FormCheckbox
                  id="field-hasPets"
                  label="Client has pets"
                  checked={formData.dependents?.hasPets === true}
                  onCheckedChange={(checked) =>
                    handleNestedChange(
                      "dependents",
                      "hasPets",
                      checked === true
                    )
                  }
                />
              </FieldContent>
            </Field>

            {formData.dependents?.hasPets && (
              <Field>
                <FieldLabel htmlFor="field-petDetails">
                  Pet Details
                </FieldLabel>
                <FieldContent>
                  <Textarea
                    id="field-petDetails"
                    value={formData.dependents?.petDetails || ""}
                    onChange={(e) =>
                      handleNestedChange(
                        "dependents",
                        "petDetails",
                        e.target.value
                      )
                    }
                    placeholder="Type, number, and any special needs (e.g., 2 dogs, 1 cat - service animal)"
                    rows={2}
                  />
                </FieldContent>
              </Field>
            )}
          </FieldGroup>
        </FieldSet>

        {/* Section 4: Risk Summary */}
        {formData.overallRiskLevel &&
          formData.overallRiskLevel !== "NOT_ASSESSED" && (
            <div>
              <h3>Risk Summary</h3>

              <div className="risk-badge-container">
                <div
                  className="risk-badge"
                  style={{
                    backgroundColor: riskLevelConfig?.color,
                    color: "#ffffff",
                  }}
                >
                  <span className="risk-label">Overall Risk Level</span>
                  <span className="risk-value">{riskLevelConfig?.label}</span>
                  <span className="risk-description">
                    {riskLevelConfig?.description}
                  </span>
                </div>
              </div>

              {formData.autoRouteToSafety && (
                <FormCheckbox
                  id="field-autoRouteToSafety"
                  label="Auto-route to safety planning workflow"
                  checked={formData.autoRouteToSafety}
                  onCheckedChange={(checked) =>
                    handleChange("autoRouteToSafety", checked === true)
                  }
                />
              )}
            </div>
          )}

        {/* Form Actions */}
        <div className="form-actions">
          <Button type="button" onClick={onBack} variant="secondary">
            Back to Step 2
          </Button>
          <Button type="submit">Continue to Step 4</Button>
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

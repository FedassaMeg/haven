/**
 * Review Step: Comprehensive review of all intake data before final submission
 *
 * Displays summaries of all 10 steps with edit capabilities and validation status.
 * Final step before creating client and enrollment records.
 */

import React, { useState } from 'react';
import type { MasterIntakeData, ValidationError } from '../utils/types';

interface ReviewStepProps {
  data: Partial<MasterIntakeData>;
  errors: ValidationError[];
  onEdit: (stepNumber: number) => void;
  onSubmit: () => void;
  onBack?: () => void;
  isSubmitting?: boolean;
}

interface StepSummary {
  stepNumber: number;
  title: string;
  complete: boolean;
  warnings: string[];
  summary: React.ReactNode;
}

export function ReviewStep({
  data,
  errors,
  onEdit,
  onSubmit,
  onBack,
  isSubmitting = false,
}: ReviewStepProps) {
  const [activeTab, setActiveTab] = useState<number>(1);
  const [certificationChecked, setCertificationChecked] = useState(false);
  const [showSubmitModal, setShowSubmitModal] = useState(false);

  // Calculate step completion
  const getStepCompleteness = (): StepSummary[] => {
    return [
      {
        stepNumber: 1,
        title: 'Initial Contact',
        complete: !!data.initialContact?.contactDate && !!data.initialContact?.clientAlias,
        warnings: [],
        summary: (
          <div className="step-summary">
            <div className="summary-row">
              <strong>Contact Date:</strong> {data.initialContact?.contactDate || 'N/A'}
            </div>
            <div className="summary-row">
              <strong>Contact Time:</strong> {data.initialContact?.contactTime || 'N/A'}
            </div>
            <div className="summary-row">
              <strong>Referral Source:</strong> {data.initialContact?.referralSource || 'N/A'}
            </div>
            <div className="summary-row">
              <strong>Client Alias:</strong> {data.initialContact?.clientAlias || 'N/A'}
            </div>
            <div className="summary-row">
              <strong>Safe to Contact:</strong>{' '}
              {data.initialContact?.safeToContactNow ? 'Yes' : 'No'}
            </div>
          </div>
        ),
      },
      {
        stepNumber: 2,
        title: 'Safety & Consent',
        complete: !!data.safetyAndConsent?.digitalSignature,
        warnings: !data.safetyAndConsent?.consents?.hmisParticipation
          ? ['HMIS participation declined']
          : [],
        summary: (
          <div className="step-summary">
            <div className="summary-row">
              <strong>Safe Contact Methods:</strong>
              <ul className="compact-list">
                {data.safetyAndConsent?.safeContactMethods?.okToCall && <li>Phone</li>}
                {data.safetyAndConsent?.safeContactMethods?.okToText && <li>Text</li>}
                {data.safetyAndConsent?.safeContactMethods?.okToEmail && <li>Email</li>}
              </ul>
            </div>
            <div className="summary-row">
              <strong>Consent Status:</strong>
              <span className={data.safetyAndConsent?.digitalSignature ? 'status-ok' : 'status-warning'}>
                {data.safetyAndConsent?.digitalSignature ? 'Signed' : 'Not signed'}
              </span>
            </div>
            <div className="summary-row">
              <strong>HMIS Participation:</strong>{' '}
              {data.safetyAndConsent?.consents?.hmisParticipation ? 'Yes' : 'No'}
            </div>
          </div>
        ),
      },
      {
        stepNumber: 3,
        title: 'Risk Assessment',
        complete: !!data.riskAssessment?.overallRiskLevel,
        warnings:
          data.riskAssessment?.overallRiskLevel === 'HIGH' ||
          data.riskAssessment?.overallRiskLevel === 'SEVERE'
            ? ['High risk client - ensure safety plan in place']
            : [],
        summary: (
          <div className="step-summary">
            <div className="summary-row">
              <strong>Lethality Tool:</strong>{' '}
              {data.riskAssessment?.lethalityScreening?.screeningTool || 'N/A'}
            </div>
            {data.riskAssessment?.lethalityScreening?.score !== undefined && (
              <div className="summary-row">
                <strong>Score:</strong> {data.riskAssessment.lethalityScreening.score} /{' '}
                {data.riskAssessment.lethalityScreening.maxScore}
              </div>
            )}
            <div className="summary-row">
              <strong>Overall Risk Level:</strong>
              <span className={`risk-badge risk-${data.riskAssessment?.overallRiskLevel?.toLowerCase()}`}>
                {data.riskAssessment?.overallRiskLevel || 'N/A'}
              </span>
            </div>
            <div className="summary-row">
              <strong>Dependents:</strong>{' '}
              {data.riskAssessment?.dependentsInfo?.hasMinors
                ? `${data.riskAssessment.dependentsInfo.numberOfMinors || 0} minors`
                : 'None'}
            </div>
          </div>
        ),
      },
      {
        stepNumber: 4,
        title: 'Eligibility & Program Match',
        complete: !!data.eligibilityMatch?.eligibilityResults,
        warnings: [],
        summary: (
          <div className="step-summary">
            <div className="summary-row">
              <strong>Homeless Status:</strong>{' '}
              {data.eligibilityMatch?.homelessStatus?.category || 'N/A'}
            </div>
            <div className="summary-row">
              <strong>Income:</strong> ${data.eligibilityMatch?.income?.monthlyIncome || 0}/month
            </div>
            <div className="summary-row">
              <strong>Household Size:</strong>{' '}
              {data.eligibilityMatch?.householdComposition?.totalSize || 1}
            </div>
            {data.eligibilityMatch?.eligibilityResults?.recommendedProgram && (
              <div className="summary-row highlighted">
                <strong>Selected Program:</strong>{' '}
                {data.eligibilityMatch.eligibilityResults.recommendedProgram}
              </div>
            )}
          </div>
        ),
      },
      {
        stepNumber: 5,
        title: 'Housing Barriers',
        complete: !!data.housingBarriers?.barrierSeverity,
        warnings: [],
        summary: (
          <div className="step-summary">
            <div className="summary-row">
              <strong>Barrier Severity:</strong>
              <span className={`severity-badge severity-${data.housingBarriers?.barrierSeverity?.toLowerCase()}`}>
                {data.housingBarriers?.barrierSeverity || 'N/A'}
              </span>
            </div>
            <div className="summary-row">
              <strong>Identified Barriers:</strong>
              <ul className="compact-list">
                {data.housingBarriers?.rentalHistory?.evictions && <li>Eviction history</li>}
                {(data.housingBarriers?.creditHistory?.score || 0) < 600 && <li>Low credit score</li>}
                {data.housingBarriers?.criminalHistory?.hasCriminalRecord && <li>Criminal record</li>}
              </ul>
            </div>
          </div>
        ),
      },
      {
        stepNumber: 6,
        title: 'Service Plan',
        complete: !!data.servicePlan?.assignedCaseManager && (data.servicePlan?.goals?.length || 0) > 0,
        warnings: [],
        summary: (
          <div className="step-summary">
            <div className="summary-row">
              <strong>Case Manager:</strong> {data.servicePlan?.assignedCaseManager || 'Not assigned'}
            </div>
            <div className="summary-row">
              <strong>Number of Goals:</strong> {data.servicePlan?.goals?.length || 0}
            </div>
            <div className="summary-row">
              <strong>Follow-up Schedule:</strong>
              <ul className="compact-list">
                {data.servicePlan?.followUpSchedule?.day30 && <li>30-day check-in</li>}
                {data.servicePlan?.followUpSchedule?.day60 && <li>60-day review</li>}
                {data.servicePlan?.followUpSchedule?.day90 && <li>90-day assessment</li>}
              </ul>
            </div>
            <div className="summary-row">
              <strong>Goal Agreement:</strong>{' '}
              {data.servicePlan?.goalAgreement?.signature ? 'Signed' : 'Not signed'}
            </div>
          </div>
        ),
      },
      {
        stepNumber: 7,
        title: 'Documentation',
        complete: getDocumentCount('required') >= 4,
        warnings:
          getDocumentCount('required') < 4 ? ['Required documents incomplete'] : [],
        summary: (
          <div className="step-summary">
            <div className="summary-row">
              <strong>Required Documents:</strong> {getDocumentCount('required')} / 4
            </div>
            <div className="summary-row">
              <strong>Optional Documents:</strong> {getDocumentCount('optional')} / 7
            </div>
            <div className="summary-row">
              <strong>Compliance Status:</strong>
              <span className={getDocumentCount('required') >= 4 ? 'status-ok' : 'status-warning'}>
                {getDocumentCount('required') >= 4 ? '✅ Complete' : '⚠️ Incomplete'}
              </span>
            </div>
          </div>
        ),
      },
      {
        stepNumber: 8,
        title: 'Demographics',
        complete: !!data.demographics?.name?.firstName && !!data.demographics?.identifiers?.birthDate,
        warnings: [],
        summary: (
          <div className="step-summary">
            <div className="summary-row">
              <strong>Full Name:</strong>{' '}
              {data.demographics?.name?.firstName} {data.demographics?.name?.lastName || ''}
            </div>
            <div className="summary-row">
              <strong>DOB:</strong> {data.demographics?.identifiers?.birthDate || 'N/A'}{' '}
              {data.demographics?.demographics?.age && `(${data.demographics.demographics.age} years)`}
            </div>
            <div className="summary-row">
              <strong>Gender:</strong> {data.demographics?.demographics?.gender || 'N/A'}
            </div>
            <div className="summary-row">
              <strong>Veteran Status:</strong> {data.demographics?.veteranStatus || 'N/A'}
            </div>
            <div className="summary-row">
              <strong>VAWA Protected:</strong>{' '}
              {data.demographics?.pseudonymization?.vawaProtected ? 'Yes' : 'No'}
            </div>
          </div>
        ),
      },
      {
        stepNumber: 9,
        title: 'Enrollment Confirmation',
        complete:
          !!data.enrollmentConfirmation?.staffConfirmation?.accuracyConfirmed &&
          !!data.enrollmentConfirmation?.staffConfirmation?.hmisEntryConfirmed &&
          !!data.enrollmentConfirmation?.staffConfirmation?.clientNotified,
        warnings: [],
        summary: (
          <div className="step-summary">
            <div className="summary-row">
              <strong>Entry Date:</strong> {data.enrollmentConfirmation?.enrollment?.entryDate || 'N/A'}
            </div>
            <div className="summary-row">
              <strong>Program:</strong> {data.enrollmentConfirmation?.enrollment?.projectName || 'N/A'}
            </div>
            <div className="summary-row">
              <strong>Daily Rate:</strong> $
              {data.enrollmentConfirmation?.costAllocation?.dailyRate?.toFixed(2) || '0.00'}
            </div>
            <div className="summary-row">
              <strong>Total Cost:</strong> $
              {data.enrollmentConfirmation?.costAllocation?.totalAnticipatedCost?.toLocaleString() || '0'}
            </div>
            <div className="summary-row">
              <strong>Status:</strong>
              <span className={`enrollment-status status-${data.enrollmentConfirmation?.enrollmentStatus?.toLowerCase()}`}>
                {data.enrollmentConfirmation?.enrollmentStatus || 'N/A'}
              </span>
            </div>
          </div>
        ),
      },
      {
        stepNumber: 10,
        title: 'Follow-Up Configuration',
        complete: !!data.followUpConfig?.reassessmentSchedule,
        warnings:
          (data.followUpConfig?.reportingReadiness?.dataQualityScore || 0) < 80
            ? ['Data quality below recommended threshold']
            : [],
        summary: (
          <div className="step-summary">
            <div className="summary-row">
              <strong>Reassessment Schedule:</strong>
              <ul className="compact-list">
                {data.followUpConfig?.reassessmentSchedule?.monthlyCheckIns && <li>Monthly check-ins</li>}
                {data.followUpConfig?.reassessmentSchedule?.day90Review && <li>90-day review</li>}
                {data.followUpConfig?.reassessmentSchedule?.annualAssessment && <li>Annual assessment</li>}
              </ul>
            </div>
            <div className="summary-row">
              <strong>Data Quality Score:</strong>{' '}
              {data.followUpConfig?.reportingReadiness?.dataQualityScore || 0}/100
            </div>
            <div className="summary-row">
              <strong>APR Ready:</strong>{' '}
              {data.followUpConfig?.reportingReadiness?.aprReady ? '✅ Yes' : '⚠️ No'}
            </div>
            <div className="summary-row">
              <strong>CAPER Ready:</strong>{' '}
              {data.followUpConfig?.reportingReadiness?.caperReady ? '✅ Yes' : '⚠️ No'}
            </div>
          </div>
        ),
      },
    ];
  };

  const getDocumentCount = (type: 'required' | 'optional'): number => {
    const docs = type === 'required' ? data.documentation?.requiredDocuments : data.documentation?.optionalDocuments;
    if (!docs) return 0;

    return Object.values(docs).filter((doc: any) => {
      if (typeof doc === 'object' && doc !== null && 'uploaded' in doc) {
        return doc.uploaded || doc.waived;
      }
      return false;
    }).length;
  };

  const getOverallCompletion = (): number => {
    const steps = getStepCompleteness();
    const completedSteps = steps.filter(s => s.complete).length;
    return Math.round((completedSteps / steps.length) * 100);
  };

  const getBlockingErrors = (): ValidationError[] => {
    return errors.filter(e => e.severity === 'ERROR');
  };

  const canSubmit = (): boolean => {
    return getBlockingErrors().length === 0 && certificationChecked;
  };

  const handleSubmitClick = () => {
    if (!canSubmit()) return;
    setShowSubmitModal(true);
  };

  const handleConfirmSubmit = () => {
    setShowSubmitModal(false);
    onSubmit();
  };

  const steps = getStepCompleteness();
  const overallCompletion = getOverallCompletion();
  const blockingErrors = getBlockingErrors();
  const warnings = steps.flatMap(s => s.warnings);

  return (
    <div className="intake-step review-step">
      <div className="step-header">
        <h2>Review & Submit</h2>
        <p className="step-description">
          Review all intake data before final submission. Edit any section as needed.
        </p>
      </div>

      {/* Overall Progress */}
      <div className="overall-progress-card">
        <div className="progress-header">
          <h3>Overall Completion</h3>
          <span className="progress-percentage">{overallCompletion}%</span>
        </div>
        <div className="progress-bar-large">
          <div className="progress-fill-large" style={{ width: `${overallCompletion}%` }} />
        </div>
        <div className="progress-stats">
          <div className="stat">
            <strong>Data Quality:</strong> {data.followUpConfig?.reportingReadiness?.dataQualityScore || 0}/100
          </div>
          <div className="stat">
            <strong>Blocking Errors:</strong> {blockingErrors.length}
          </div>
          <div className="stat">
            <strong>Warnings:</strong> {warnings.length}
          </div>
        </div>
      </div>

      {/* Tabs Navigation */}
      <div className="review-tabs">
        {steps.map(step => (
          <button
            key={step.stepNumber}
            type="button"
            className={`tab-button ${activeTab === step.stepNumber ? 'active' : ''} ${
              step.complete ? 'complete' : 'incomplete'
            }`}
            onClick={() => setActiveTab(step.stepNumber)}
          >
            <span className="tab-icon">{step.complete ? '✅' : '⚠️'}</span>
            <span className="tab-label">
              Step {step.stepNumber}: {step.title}
            </span>
          </button>
        ))}
      </div>

      {/* Active Tab Content */}
      <div className="review-content">
        {steps.map(
          step =>
            activeTab === step.stepNumber && (
              <div key={step.stepNumber} className="step-review-card">
                <div className="card-header">
                  <h3>
                    Step {step.stepNumber}: {step.title}
                  </h3>
                  <button type="button" className="btn-secondary btn-sm" onClick={() => onEdit(step.stepNumber)}>
                    Edit
                  </button>
                </div>

                {step.warnings.length > 0 && (
                  <div className="alert alert-warning">
                    <span className="alert-icon">⚠️</span>
                    <div className="alert-content">
                      <ul>
                        {step.warnings.map((warning, idx) => (
                          <li key={idx}>{warning}</li>
                        ))}
                      </ul>
                    </div>
                  </div>
                )}

                {step.summary}
              </div>
            )
        )}
      </div>

      {/* Validation Summary */}
      {blockingErrors.length > 0 && (
        <div className="error-summary">
          <h4>Blocking Errors (Must be resolved before submission)</h4>
          <ul>
            {blockingErrors.map((error, idx) => (
              <li key={idx}>
                <strong>Step {error.field?.split('.')[0]}:</strong> {error.message}
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Final Certification */}
      <div className="certification-section">
        <label className="certification-checkbox">
          <input
            type="checkbox"
            checked={certificationChecked}
            onChange={e => setCertificationChecked(e.target.checked)}
          />
          <span>I certify that all information provided is accurate to the best of my knowledge.</span>
        </label>
      </div>

      {/* Form Actions */}
      <div className="form-actions">
        {onBack && (
          <button type="button" className="btn btn-secondary" onClick={onBack}>
            Back to Step 10
          </button>
        )}
        <button
          type="button"
          className="btn btn-primary btn-submit"
          onClick={handleSubmitClick}
          disabled={!canSubmit() || isSubmitting}
        >
          {isSubmitting ? 'Submitting...' : 'Submit Intake'}
        </button>
      </div>

      {/* Submit Confirmation Modal */}
      {showSubmitModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <h3>Confirm Intake Submission</h3>
            <p>
              You are about to submit this intake and create official client and enrollment records.
              This action cannot be undone.
            </p>
            <div className="modal-summary">
              <div>
                <strong>Client:</strong> {data.demographics?.name?.firstName}{' '}
                {data.demographics?.name?.lastName}
              </div>
              <div>
                <strong>Program:</strong> {data.enrollmentConfirmation?.enrollment?.projectName}
              </div>
              <div>
                <strong>Entry Date:</strong> {data.enrollmentConfirmation?.enrollment?.entryDate}
              </div>
            </div>
            <div className="modal-actions">
              <button type="button" className="btn btn-secondary" onClick={() => setShowSubmitModal(false)}>
                Cancel
              </button>
              <button type="button" className="btn btn-primary" onClick={handleConfirmSubmit}>
                Confirm Submission
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

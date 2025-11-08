/**
 * Step 9: Service Enrollment Confirmation
 *
 * Finalizes enrollment details and activates service funding tracking.
 * Creates official enrollment record with cost allocation.
 */

import React, { useState, useEffect } from 'react';
import type {
  EnrollmentConfirmationData,
  ValidationError,
  ProjectType,
  EnrollmentStatus,
} from '../utils/types';

interface Step9Props {
  data: Partial<EnrollmentConfirmationData>;
  errors: ValidationError[];
  onChange: (updates: Partial<EnrollmentConfirmationData>) => void;
  onComplete: (data: EnrollmentConfirmationData) => void;
  onBack?: () => void;
  onChangeProgram?: () => void;
  // From previous steps
  clientName?: string;
  clientDOB?: string;
  householdSize?: number;
  selectedProgram?: {
    id: string;
    name: string;
    type: ProjectType;
    fundingSource: string;
    dailyRate: number;
  };
  initialContactDate?: string;
  currentUser?: string;
}

export function Step9_EnrollmentConfirmation({
  data,
  errors,
  onChange,
  onComplete,
  onBack,
  onChangeProgram,
  clientName,
  clientDOB,
  householdSize,
  selectedProgram,
  initialContactDate,
  currentUser = 'Current Staff Member',
}: Step9Props) {
  const [formData, setFormData] = useState<Partial<EnrollmentConfirmationData>>({
    enrollment: {
      entryDate: new Date().toISOString().split('T')[0],
      projectType: selectedProgram?.type || 'ES',
      projectId: selectedProgram?.id || '',
      projectName: selectedProgram?.name || '',
      fundingSource: selectedProgram?.fundingSource || '',
      relationshipToHoH: 'SELF',
      ...data.enrollment,
    },
    costAllocation: {
      dailyRate: selectedProgram?.dailyRate,
      fundingStartDate: new Date().toISOString().split('T')[0],
      anticipatedEndDate: undefined,
      totalAnticipatedCost: undefined,
      fundingSourceDetails: {
        source: selectedProgram?.fundingSource || '',
        grantNumber: '',
        availableFunds: 0,
      },
      ...data.costAllocation,
    },
    enrollmentStatus: data.enrollmentStatus || 'PENDING_APPROVAL',
    staffConfirmation: {
      confirmedBy: currentUser,
      confirmationDate: new Date().toISOString(),
      accuracyConfirmed: false,
      hmisEntryConfirmed: false,
      clientNotified: false,
      notificationMethod: undefined,
      ...data.staffConfirmation,
    },
  });

  const [notifyCoordinator, setNotifyCoordinator] = useState(false);

  // Auto-calculate anticipated end date based on program type
  useEffect(() => {
    if (formData.enrollment?.entryDate && formData.enrollment?.projectType) {
      const entryDate = new Date(formData.enrollment.entryDate);
      let anticipatedEndDate: string | undefined;

      switch (formData.enrollment.projectType) {
        case 'TH':
          // Transitional Housing: 24 months max
          anticipatedEndDate = new Date(entryDate.setMonth(entryDate.getMonth() + 24))
            .toISOString()
            .split('T')[0];
          break;
        case 'RRH':
          // Rapid Re-Housing: 18 months typical
          anticipatedEndDate = new Date(entryDate.setMonth(entryDate.getMonth() + 18))
            .toISOString()
            .split('T')[0];
          break;
        case 'PSH':
          // Permanent Supportive Housing: Indefinite
          anticipatedEndDate = undefined;
          break;
        case 'ES':
          // Emergency Shelter: 90 days typical
          anticipatedEndDate = new Date(entryDate.setDate(entryDate.getDate() + 90))
            .toISOString()
            .split('T')[0];
          break;
        default:
          anticipatedEndDate = undefined;
      }

      if (anticipatedEndDate !== formData.costAllocation?.anticipatedEndDate) {
        handleNestedChange('costAllocation', 'anticipatedEndDate', anticipatedEndDate);
      }
    }
  }, [formData.enrollment?.entryDate, formData.enrollment?.projectType]);

  // Auto-calculate total anticipated cost
  useEffect(() => {
    if (
      formData.costAllocation?.dailyRate &&
      formData.enrollment?.entryDate &&
      formData.costAllocation?.anticipatedEndDate
    ) {
      const start = new Date(formData.enrollment.entryDate);
      const end = new Date(formData.costAllocation.anticipatedEndDate);
      const days = Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24));
      const totalCost = days * formData.costAllocation.dailyRate;

      if (totalCost !== formData.costAllocation?.totalAnticipatedCost) {
        handleNestedChange('costAllocation', 'totalAnticipatedCost', totalCost);
      }
    }
  }, [
    formData.costAllocation?.dailyRate,
    formData.enrollment?.entryDate,
    formData.costAllocation?.anticipatedEndDate,
  ]);

  const handleChange = (field: keyof EnrollmentConfirmationData, value: any) => {
    const updated = { ...formData, [field]: value };
    setFormData(updated);
    onChange(updated);
  };

  const handleNestedChange = (section: string, field: string, value: any) => {
    const updated = {
      ...formData,
      [section]: {
        ...(formData[section as keyof EnrollmentConfirmationData] as any),
        [field]: value,
      },
    };
    setFormData(updated);
    onChange(updated);
  };

  const handleSubmit = async () => {
    if (canConfirm() && formData as EnrollmentConfirmationData) {
      // In real implementation, would call enrollmentApi.create()
      // For now, just complete the step
      onComplete(formData as EnrollmentConfirmationData);
    }
  };

  const handleSaveDraft = () => {
    onChange(formData);
    // In real implementation, would save draft to database
  };

  const handleNotifyCoordinator = () => {
    setNotifyCoordinator(true);
    // In real implementation, would send notification email/message
  };

  const canConfirm = (): boolean => {
    return (
      formData.staffConfirmation?.accuracyConfirmed === true &&
      formData.staffConfirmation?.hmisEntryConfirmed === true &&
      formData.staffConfirmation?.clientNotified === true
    );
  };

  const getError = (field: string): ValidationError | undefined => {
    return errors.find(e => e.field === field);
  };

  const getStatusBadgeClass = (status: EnrollmentStatus): string => {
    switch (status) {
      case 'ACTIVE':
        return 'status-badge-active';
      case 'PENDING_APPROVAL':
        return 'status-badge-pending';
      case 'WAITLISTED':
        return 'status-badge-waitlisted';
      default:
        return 'status-badge-default';
    }
  };

  const getProjectTypeLabel = (type: ProjectType): string => {
    const labels: Record<ProjectType, string> = {
      ES: 'Emergency Shelter',
      TH: 'Transitional Housing',
      RRH: 'Rapid Re-Housing',
      PSH: 'Permanent Supportive Housing',
      SO: 'Safe Haven',
      PH: 'Other Permanent Housing',
      DAY: 'Day Shelter',
      SSO: 'Services Only',
      HP: 'Homelessness Prevention',
      CE: 'Coordinated Entry',
    };
    return labels[type] || type;
  };

  return (
    <div className="intake-step">
      <div className="step-header">
        <h2>Step 9: Enrollment Confirmation</h2>
        <p className="step-description">
          Review and finalize enrollment details. This will activate service funding tracking
          and create the official enrollment record.
        </p>
      </div>

      <form className="intake-form">
        {/* Enrollment Summary */}
        <section className="form-section enrollment-summary">
          <h3>Enrollment Summary</h3>

          <div className="summary-grid">
            <div className="summary-item">
              <label>Client Name</label>
              <div className="summary-value">{clientName || 'Not provided'}</div>
            </div>

            <div className="summary-item">
              <label>Date of Birth</label>
              <div className="summary-value">{clientDOB || 'Not provided'}</div>
            </div>

            <div className="summary-item">
              <label>Household Size</label>
              <div className="summary-value">{householdSize || 'Not provided'}</div>
            </div>
          </div>

          {selectedProgram && (
            <div className="selected-program-card">
              <div className="card-header">
                <span>Selected Program</span>
                {onChangeProgram && (
                  <button type="button" className="btn-sm btn-secondary" onClick={onChangeProgram}>
                    Change Program
                  </button>
                )}
              </div>
              <div className="card-body">
                <h4>{selectedProgram.name}</h4>
                <div className="program-meta">
                  <span className="program-type-badge">
                    {getProjectTypeLabel(selectedProgram.type)}
                  </span>
                  <span className="funding-source">{selectedProgram.fundingSource}</span>
                </div>
              </div>
            </div>
          )}
        </section>

        {/* Enrollment Details */}
        <section className="form-section">
          <h3>Enrollment Details</h3>

          <div className="form-field">
            <label>
              Entry Date (24 CFR §578.103(a)) <span className="required">*</span>
            </label>
            <input
              type="date"
              value={formData.enrollment?.entryDate || ''}
              onChange={e => handleNestedChange('enrollment', 'entryDate', e.target.value)}
              max={new Date().toISOString().split('T')[0]}
              min={initialContactDate}
              className={getError('enrollment.entryDate') ? 'error' : ''}
            />
            <span className="field-help">
              Cannot be future date or before initial contact date ({initialContactDate || 'N/A'}).
            </span>
            {getError('enrollment.entryDate') && (
              <span className="field-error">{getError('enrollment.entryDate')?.message}</span>
            )}
          </div>

          <div className="form-field">
            <label>
              Relationship to Head of Household <span className="required">*</span>
            </label>
            <select
              value={formData.enrollment?.relationshipToHoH || 'SELF'}
              onChange={e => handleNestedChange('enrollment', 'relationshipToHoH', e.target.value)}
            >
              <option value="SELF">Self (head of household)</option>
              <option value="CHILD">Head of household's child</option>
              <option value="SPOUSE_PARTNER">Head of household's spouse/partner</option>
              <option value="OTHER_RELATION">Head of household's other relation</option>
              <option value="NON_RELATION">Other: non-relation member</option>
            </select>
          </div>

          {(formData.enrollment?.projectType === 'RRH' ||
            formData.enrollment?.projectType === 'PSH') && (
            <div className="form-field">
              <label>Move-In Date (Optional)</label>
              <input
                type="date"
                value={formData.enrollment?.moveInDate || ''}
                onChange={e => handleNestedChange('enrollment', 'moveInDate', e.target.value)}
                min={formData.enrollment?.entryDate}
              />
              <span className="field-help">Can be set later after housing is secured.</span>
            </div>
          )}
        </section>

        {/* Cost Allocation */}
        <section className="form-section cost-allocation">
          <h3>Cost Allocation (Auto-calculated)</h3>

          <div className="cost-grid">
            <div className="cost-item">
              <label>Daily Rate</label>
              <div className="cost-value">
                ${formData.costAllocation?.dailyRate?.toFixed(2) || '0.00'}
              </div>
            </div>

            <div className="cost-item">
              <label>Funding Start Date</label>
              <div className="cost-value">
                {formData.costAllocation?.fundingStartDate || 'N/A'}
              </div>
            </div>

            <div className="cost-item">
              <label>Anticipated End Date</label>
              <div className="cost-value">
                {formData.costAllocation?.anticipatedEndDate || 'Indefinite (PSH)'}
              </div>
            </div>

            <div className="cost-item highlighted">
              <label>Total Anticipated Cost</label>
              <div className="cost-value total">
                ${formData.costAllocation?.totalAnticipatedCost?.toLocaleString() || 'N/A'}
              </div>
            </div>
          </div>

          {formData.costAllocation?.fundingSourceDetails && (
            <div className="funding-source-card">
              <h4>Funding Source Details</h4>
              <div className="funding-details">
                <div className="detail-row">
                  <span className="label">Source:</span>
                  <span className="value">
                    {formData.costAllocation.fundingSourceDetails.source}
                  </span>
                </div>
                <div className="detail-row">
                  <span className="label">Grant Number:</span>
                  <span className="value">
                    {formData.costAllocation.fundingSourceDetails.grantNumber || 'N/A'}
                  </span>
                </div>
                <div className="detail-row">
                  <span className="label">Available Funds:</span>
                  <span className="value">
                    ${formData.costAllocation.fundingSourceDetails.availableFunds?.toLocaleString() || '0'}
                  </span>
                </div>
              </div>
            </div>
          )}
        </section>

        {/* Enrollment Status */}
        <section className="form-section">
          <h3>Enrollment Status</h3>

          <div className="status-display">
            <span className={`status-badge ${getStatusBadgeClass(formData.enrollmentStatus!)}`}>
              {formData.enrollmentStatus?.replace('_', ' ')}
            </span>
          </div>

          {formData.enrollmentStatus === 'PENDING_APPROVAL' && (
            <div className="alert alert-info">
              <span className="alert-icon">ℹ️</span>
              <div className="alert-content">
                <p>Awaiting approval from program coordinator</p>
                <button
                  type="button"
                  className="btn-sm btn-secondary"
                  onClick={handleNotifyCoordinator}
                  disabled={notifyCoordinator}
                >
                  {notifyCoordinator ? 'Coordinator Notified' : 'Notify Coordinator'}
                </button>
              </div>
            </div>
          )}

          {formData.enrollmentStatus === 'WAITLISTED' && (
            <div className="waitlist-info">
              <div className="info-row">
                <label>Position in Queue:</label>
                <span className="value">#5</span>
              </div>
              <div className="info-row">
                <label>Estimated Wait Time:</label>
                <span className="value">2-3 weeks</span>
              </div>
            </div>
          )}
        </section>

        {/* Staff Confirmation */}
        <section className="form-section staff-confirmation">
          <h3>Staff Confirmation</h3>
          <p className="section-help critical">
            All confirmations required before enrollment can be finalized.
          </p>

          <div className="confirmation-checklist">
            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={formData.staffConfirmation?.accuracyConfirmed || false}
                onChange={e =>
                  handleNestedChange('staffConfirmation', 'accuracyConfirmed', e.target.checked)
                }
              />
              I have reviewed all information and it is accurate
            </label>

            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={formData.staffConfirmation?.hmisEntryConfirmed || false}
                onChange={e =>
                  handleNestedChange('staffConfirmation', 'hmisEntryConfirmed', e.target.checked)
                }
              />
              HMIS entry will be completed within 24 hours
            </label>

            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={formData.staffConfirmation?.clientNotified || false}
                onChange={e =>
                  handleNestedChange('staffConfirmation', 'clientNotified', e.target.checked)
                }
              />
              Client has been notified of enrollment status
            </label>
          </div>

          {formData.staffConfirmation?.clientNotified && (
            <div className="form-field">
              <label>
                Notification Method <span className="required">*</span>
              </label>
              <select
                value={formData.staffConfirmation?.notificationMethod || ''}
                onChange={e =>
                  handleNestedChange('staffConfirmation', 'notificationMethod', e.target.value)
                }
                className={getError('staffConfirmation.notificationMethod') ? 'error' : ''}
              >
                <option value="">Select method...</option>
                <option value="PHONE">Phone</option>
                <option value="EMAIL">Email</option>
                <option value="IN_PERSON">In-person</option>
                <option value="LETTER">Letter</option>
              </select>
              {getError('staffConfirmation.notificationMethod') && (
                <span className="field-error">
                  {getError('staffConfirmation.notificationMethod')?.message}
                </span>
              )}
            </div>
          )}

          <div className="confirmation-metadata">
            <div className="meta-row">
              <label>Confirming Staff:</label>
              <span className="value">{formData.staffConfirmation?.confirmedBy}</span>
            </div>
            <div className="meta-row">
              <label>Confirmation Date:</label>
              <span className="value">
                {new Date(formData.staffConfirmation?.confirmationDate || '').toLocaleDateString()}
              </span>
            </div>
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

        {/* Final Actions */}
        <div className="form-actions">
          <div className="left-actions">
            {onBack && (
              <button type="button" className="btn btn-secondary" onClick={onBack}>
                Back
              </button>
            )}
            <button type="button" className="btn btn-secondary" onClick={handleSaveDraft}>
              Save as Draft
            </button>
          </div>
          <button
            type="button"
            className="btn btn-primary btn-confirm"
            onClick={handleSubmit}
            disabled={!canConfirm()}
          >
            Confirm Enrollment
          </button>
        </div>

        {!canConfirm() && (
          <div className="alert alert-warning">
            <span className="alert-icon">⚠️</span>
            <div className="alert-content">
              <p>Complete all staff confirmations to enable enrollment.</p>
            </div>
          </div>
        )}
      </form>
    </div>
  );
}

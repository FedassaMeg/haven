/**
 * Step 6: Service Plan & Case Assignment
 *
 * Assigns case manager, sets client goals, and schedules follow-ups.
 * Implements empowerment-focused goal setting and digital goal agreement.
 */

import React, { useState, useEffect, useRef } from 'react';
import type {
  ServicePlanData,
  ValidationError,
  ClientGoal,
  CaseManagerAssignment,
  DigitalSignature,
} from '../utils/types';
import { validateStep6 } from '../lib/validation';
import { GOAL_CATEGORIES, CLIENT_STRENGTHS } from '../index';

interface Step6Props {
  data: Partial<ServicePlanData>;
  errors: ValidationError[];
  warnings: ValidationError[];
  onChange: (updates: Partial<ServicePlanData>) => void;
  onComplete: (data: ServicePlanData) => void;
  onBack: () => void;
}

// Mock case managers (replace with actual API call)
const MOCK_CASE_MANAGERS: CaseManagerAssignment[] = [
  {
    id: 'cm1',
    name: 'Sarah Johnson',
    email: 'sjohnson@haven.org',
    phone: '(555) 123-4567',
    assignmentDate: new Date().toISOString(),
    currentCaseload: 15,
    specializations: ['DV Support', 'Family Services', 'Housing Navigation'],
  },
  {
    id: 'cm2',
    name: 'Michael Chen',
    email: 'mchen@haven.org',
    phone: '(555) 234-5678',
    assignmentDate: new Date().toISOString(),
    currentCaseload: 12,
    specializations: ['Employment', 'Benefits Navigation', 'Mental Health'],
  },
  {
    id: 'cm3',
    name: 'Patricia Martinez',
    email: 'pmartinez@haven.org',
    phone: '(555) 345-6789',
    assignmentDate: new Date().toISOString(),
    currentCaseload: 18,
    specializations: ['Legal Advocacy', 'Immigration', 'Bilingual Services'],
  },
];

export const Step6_ServicePlan: React.FC<Step6Props> = ({
  data,
  errors,
  warnings,
  onChange,
  onComplete,
  onBack,
}) => {
  const [formData, setFormData] = useState<Partial<ServicePlanData>>({
    goals: data.goals || [],
    clientStrengths: data.clientStrengths || [],
    clientAspirations: data.clientAspirations || '',
    followUpSchedule: {
      day30: true,
      day60: true,
      day90: true,
      customDates: [],
      ...data.followUpSchedule,
    },
    createdDate: data.createdDate || new Date().toISOString(),
    version: data.version || 1,
    ...data,
  });

  const [validationErrors, setValidationErrors] = useState<ValidationError[]>(errors);
  const [validationWarnings, setValidationWarnings] = useState<ValidationError[]>(warnings);
  const [availableCaseManagers] = useState<CaseManagerAssignment[]>(MOCK_CASE_MANAGERS);
  const [customStrength, setCustomStrength] = useState('');
  const [clientPresent, setClientPresent] = useState(false);
  const [goalAgreed, setGoalAgreed] = useState(false);

  // Signature canvas
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [isDrawing, setIsDrawing] = useState(false);
  const [signatureData, setSignatureData] = useState<string | undefined>(
    data.digitalGoalAgreement?.signatureData
  );

  useEffect(() => {
    setFormData(prev => ({ ...prev, ...data }));
  }, [data]);

  useEffect(() => {
    setValidationErrors(errors);
    setValidationWarnings(warnings);
  }, [errors, warnings]);

  const handleChange = (field: keyof ServicePlanData, value: any) => {
    const updates = { [field]: value };
    setFormData(prev => ({ ...prev, ...updates }));
    onChange(updates);
  };

  const handleNestedChange = (parent: keyof ServicePlanData, field: string, value: any) => {
    const updates = {
      [parent]: {
        ...(formData[parent] as any),
        [field]: value,
      },
    };
    setFormData(prev => ({ ...prev, ...updates }));
    onChange(updates);
  };

  const handleCaseManagerChange = (cmId: string) => {
    const selectedCM = availableCaseManagers.find(cm => cm.id === cmId);
    if (selectedCM) {
      const assignment: CaseManagerAssignment = {
        ...selectedCM,
        assignmentDate: new Date().toISOString(),
      };
      handleChange('assignedCaseManager', assignment);
    }
  };

  const handleAddGoal = () => {
    const newGoal: ClientGoal = {
      id: `goal-${Date.now()}`,
      category: 'HOUSING_SEARCH',
      description: '',
      measurableOutcome: '',
      priority: 'MEDIUM',
      status: 'NOT_STARTED',
      responsibleParty: 'BOTH',
      actionSteps: [],
    };

    const updatedGoals = [...(formData.goals || []), newGoal];
    handleChange('goals', updatedGoals);
  };

  const handleRemoveGoal = (goalId: string) => {
    const updatedGoals = (formData.goals || []).filter(g => g.id !== goalId);
    handleChange('goals', updatedGoals);
  };

  const handleGoalChange = (goalId: string, field: keyof ClientGoal, value: any) => {
    const updatedGoals = (formData.goals || []).map(goal =>
      goal.id === goalId ? { ...goal, [field]: value } : goal
    );
    handleChange('goals', updatedGoals);
  };

  const handleAddStrength = (strength: string) => {
    if (!formData.clientStrengths?.includes(strength)) {
      const updatedStrengths = [...(formData.clientStrengths || []), strength];
      handleChange('clientStrengths', updatedStrengths);
    }
  };

  const handleRemoveStrength = (strength: string) => {
    const updatedStrengths = (formData.clientStrengths || []).filter(s => s !== strength);
    handleChange('clientStrengths', updatedStrengths);
  };

  const handleAddCustomStrength = () => {
    if (customStrength.trim()) {
      handleAddStrength(customStrength.trim());
      setCustomStrength('');
    }
  };

  // Signature canvas handlers
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

    const dataUrl = canvas.toDataURL('image/png');
    setSignatureData(dataUrl);

    const signature: DigitalSignature = {
      signed: true,
      signatureData: dataUrl,
      timestamp: new Date().toISOString(),
      ipAddress: 'client-side',
      userAgent: navigator.userAgent,
    };

    handleChange('digitalGoalAgreement', signature);
  };

  const clearSignature = () => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    ctx.clearRect(0, 0, canvas.width, canvas.height);
    setSignatureData(undefined);
    handleChange('digitalGoalAgreement', undefined);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    const result = validateStep6(formData as ServicePlanData);
    setValidationErrors(result.errors);
    setValidationWarnings(result.warnings);

    if (result.isValid) {
      onComplete(formData as ServicePlanData);
    } else {
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

  return (
    <div className="intake-step step-6">
      <div className="step-header">
        <h2>Step 6: Service Plan & Case Assignment</h2>
        <p className="step-description">
          Assign a case manager, collaborate on client goals, and establish a follow-up schedule.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="intake-form">
        {/* Section 1: Case Manager Assignment */}
        <section className="form-section">
          <h3>Case Manager Assignment</h3>

          <div className="form-field">
            <label htmlFor="field-assignedCaseManager">
              Select Case Manager <span className="required">*</span>
            </label>
            <select
              id="field-assignedCaseManager"
              value={formData.assignedCaseManager?.id || ''}
              onChange={e => handleCaseManagerChange(e.target.value)}
              className={getFieldError('assignedCaseManager') ? 'error' : ''}
            >
              <option value="">-- Select Case Manager --</option>
              {availableCaseManagers.map(cm => (
                <option key={cm.id} value={cm.id}>
                  {cm.name} - Caseload: {cm.currentCaseload}/20 -{' '}
                  {cm.specializations?.join(', ')}
                </option>
              ))}
            </select>
            {getFieldError('assignedCaseManager') && (
              <span className="field-error" role="alert">
                {getFieldError('assignedCaseManager')}
              </span>
            )}
          </div>

          {formData.assignedCaseManager && (
            <div className="case-manager-card">
              <h4>{formData.assignedCaseManager.name}</h4>
              <div className="cm-details">
                <div className="detail-row">
                  <span className="label">Email:</span>
                  <span className="value">{formData.assignedCaseManager.email}</span>
                </div>
                <div className="detail-row">
                  <span className="label">Phone:</span>
                  <span className="value">{formData.assignedCaseManager.phone}</span>
                </div>
                <div className="detail-row">
                  <span className="label">Current Caseload:</span>
                  <span className="value">
                    {formData.assignedCaseManager.currentCaseload}/20
                  </span>
                </div>
                <div className="detail-row">
                  <span className="label">Specializations:</span>
                  <div className="specialization-badges">
                    {formData.assignedCaseManager.specializations?.map((spec, i) => (
                      <span key={i} className="badge">
                        {spec}
                      </span>
                    ))}
                  </div>
                </div>
                <div className="detail-row">
                  <span className="label">Assignment Date:</span>
                  <span className="value">
                    {new Date(formData.assignedCaseManager.assignmentDate).toLocaleDateString()}
                  </span>
                </div>
              </div>
            </div>
          )}
        </section>

        {/* Section 2: Goal Setting */}
        <section className="form-section">
          <h3>Client Goals</h3>
          <p className="section-help">
            Collaborate with the client to set SMART goals (Specific, Measurable, Achievable,
            Relevant, Time-bound). At least one goal is required.
          </p>

          <button type="button" onClick={handleAddGoal} className="btn btn-secondary btn-sm">
            + Add Goal
          </button>

          {formData.goals && formData.goals.length > 0 && (
            <div className="goals-list">
              {formData.goals.map((goal, index) => (
                <div key={goal.id} className="goal-card">
                  <div className="goal-header">
                    <h4>Goal {index + 1}</h4>
                    <button
                      type="button"
                      onClick={() => handleRemoveGoal(goal.id)}
                      className="btn-remove"
                      aria-label="Remove goal"
                    >
                      ✕
                    </button>
                  </div>

                  <div className="form-row">
                    <div className="form-field">
                      <label htmlFor={`field-goal-${goal.id}-category`}>
                        Category <span className="required">*</span>
                      </label>
                      <select
                        id={`field-goal-${goal.id}-category`}
                        value={goal.category}
                        onChange={e => handleGoalChange(goal.id, 'category', e.target.value)}
                        className={getFieldError(`goals[${index}].category`) ? 'error' : ''}
                      >
                        {Object.entries(GOAL_CATEGORIES).map(([key, label]) => (
                          <option key={key} value={key}>
                            {label}
                          </option>
                        ))}
                      </select>
                      {getFieldError(`goals[${index}].category`) && (
                        <span className="field-error" role="alert">
                          {getFieldError(`goals[${index}].category`)}
                        </span>
                      )}
                    </div>

                    <div className="form-field">
                      <label htmlFor={`field-goal-${goal.id}-priority`}>Priority</label>
                      <select
                        id={`field-goal-${goal.id}-priority`}
                        value={goal.priority}
                        onChange={e => handleGoalChange(goal.id, 'priority', e.target.value)}
                      >
                        <option value="HIGH">High</option>
                        <option value="MEDIUM">Medium</option>
                        <option value="LOW">Low</option>
                      </select>
                    </div>
                  </div>

                  <div className="form-field">
                    <label htmlFor={`field-goal-${goal.id}-description`}>
                      Goal Description <span className="required">*</span>
                    </label>
                    <textarea
                      id={`field-goal-${goal.id}-description`}
                      value={goal.description}
                      onChange={e => handleGoalChange(goal.id, 'description', e.target.value)}
                      placeholder="Describe the goal in detail"
                      rows={2}
                      className={getFieldError(`goals[${index}].description`) ? 'error' : ''}
                    />
                    {getFieldError(`goals[${index}].description`) && (
                      <span className="field-error" role="alert">
                        {getFieldError(`goals[${index}].description`)}
                      </span>
                    )}
                  </div>

                  <div className="form-field">
                    <label htmlFor={`field-goal-${goal.id}-measurableOutcome`}>
                      Measurable Outcome (SMART Format) <span className="required">*</span>
                    </label>
                    <textarea
                      id={`field-goal-${goal.id}-measurableOutcome`}
                      value={goal.measurableOutcome}
                      onChange={e => handleGoalChange(goal.id, 'measurableOutcome', e.target.value)}
                      placeholder="How will we measure success? (e.g., 'Apply to 5 jobs per week')"
                      rows={2}
                      className={getFieldError(`goals[${index}].measurableOutcome`) ? 'error' : ''}
                    />
                    {getFieldError(`goals[${index}].measurableOutcome`) && (
                      <span className="field-error" role="alert">
                        {getFieldError(`goals[${index}].measurableOutcome`)}
                      </span>
                    )}
                  </div>

                  <div className="form-row">
                    <div className="form-field">
                      <label htmlFor={`field-goal-${goal.id}-targetDate`}>
                        Target Completion Date
                      </label>
                      <input
                        id={`field-goal-${goal.id}-targetDate`}
                        type="date"
                        value={goal.targetDate || ''}
                        onChange={e => handleGoalChange(goal.id, 'targetDate', e.target.value)}
                        min={new Date().toISOString().split('T')[0]}
                      />
                    </div>

                    <div className="form-field">
                      <label htmlFor={`field-goal-${goal.id}-responsibleParty`}>
                        Responsible Party
                      </label>
                      <select
                        id={`field-goal-${goal.id}-responsibleParty`}
                        value={goal.responsibleParty}
                        onChange={e => handleGoalChange(goal.id, 'responsibleParty', e.target.value)}
                      >
                        <option value="CLIENT">Client</option>
                        <option value="CASE_MANAGER">Case Manager</option>
                        <option value="BOTH">Both</option>
                        <option value="EXTERNAL">External Partner</option>
                      </select>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}

          {getFieldError('goals') && (
            <span className="field-error" role="alert">
              {getFieldError('goals')}
            </span>
          )}
        </section>

        {/* Section 3: Client Strengths */}
        <section className="form-section">
          <h3>Client Strengths (Empowerment Focus)</h3>
          <p className="section-help">Identify at least 3 client strengths to guide goal-setting.</p>

          <div className="form-field">
            <label>Select Strengths</label>
            <div className="strength-selector">
              {Object.entries(CLIENT_STRENGTHS).map(([key, label]) => (
                <button
                  key={key}
                  type="button"
                  onClick={() => handleAddStrength(label)}
                  className={`strength-option ${
                    formData.clientStrengths?.includes(label) ? 'selected' : ''
                  }`}
                  disabled={formData.clientStrengths?.includes(label)}
                >
                  {label}
                </button>
              ))}
            </div>
          </div>

          <div className="form-field">
            <label htmlFor="field-customStrength">Add Custom Strength</label>
            <div className="input-with-button">
              <input
                id="field-customStrength"
                type="text"
                value={customStrength}
                onChange={e => setCustomStrength(e.target.value)}
                onKeyPress={e => e.key === 'Enter' && (e.preventDefault(), handleAddCustomStrength())}
                placeholder="Enter a custom strength"
              />
              <button type="button" onClick={handleAddCustomStrength} className="btn btn-sm">
                Add
              </button>
            </div>
          </div>

          {formData.clientStrengths && formData.clientStrengths.length > 0 && (
            <div className="strength-tags">
              {formData.clientStrengths.map((strength, i) => (
                <span key={i} className="strength-tag">
                  {strength}
                  <button
                    type="button"
                    onClick={() => handleRemoveStrength(strength)}
                    className="tag-remove"
                    aria-label={`Remove ${strength}`}
                  >
                    ✕
                  </button>
                </span>
              ))}
            </div>
          )}

          {getFieldWarning('clientStrengths') && (
            <div className="alert alert-warning">
              <span className="alert-icon">⚠️</span>
              <span>{getFieldWarning('clientStrengths')}</span>
            </div>
          )}
        </section>

        {/* Section 4: Client Aspirations */}
        <section className="form-section">
          <h3>Client Aspirations</h3>

          <div className="form-field">
            <label htmlFor="field-clientAspirations">
              What are your long-term goals and dreams?
            </label>
            <textarea
              id="field-clientAspirations"
              value={formData.clientAspirations || ''}
              onChange={e => handleChange('clientAspirations', e.target.value)}
              placeholder="Share your hopes and aspirations for the future..."
              rows={4}
              maxLength={2000}
            />
            <span className="field-help">
              This helps us align services with your own aspirations and vision for your life.
            </span>
            {getFieldWarning('clientAspirations') && (
              <div className="alert alert-warning">
                <span className="alert-icon">⚠️</span>
                <span>{getFieldWarning('clientAspirations')}</span>
              </div>
            )}
          </div>
        </section>

        {/* Section 5: Follow-up Schedule */}
        <section className="form-section">
          <h3>Follow-up Schedule</h3>

          <div className="checkbox-group">
            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={formData.followUpSchedule?.day30 || false}
                onChange={e => handleNestedChange('followUpSchedule', 'day30', e.target.checked)}
              />
              <span>30-day check-in</span>
            </label>

            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={formData.followUpSchedule?.day60 || false}
                onChange={e => handleNestedChange('followUpSchedule', 'day60', e.target.checked)}
              />
              <span>60-day progress review</span>
            </label>

            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={formData.followUpSchedule?.day90 || false}
                onChange={e => handleNestedChange('followUpSchedule', 'day90', e.target.checked)}
              />
              <span>90-day reassessment</span>
            </label>
          </div>

          <div className="form-row">
            <div className="form-field">
              <label htmlFor="field-preferredContactMethod">Preferred Contact Method</label>
              <select
                id="field-preferredContactMethod"
                value={formData.followUpSchedule?.preferredContactMethod || ''}
                onChange={e =>
                  handleNestedChange('followUpSchedule', 'preferredContactMethod', e.target.value)
                }
              >
                <option value="">-- Select Method --</option>
                <option value="PHONE">Phone</option>
                <option value="EMAIL">Email</option>
                <option value="IN_PERSON">In-person</option>
                <option value="VIDEO">Video call</option>
              </select>
            </div>

            <div className="form-field">
              <label htmlFor="field-preferredContactTime">Preferred Contact Time</label>
              <input
                id="field-preferredContactTime"
                type="time"
                value={formData.followUpSchedule?.preferredContactTime || ''}
                onChange={e =>
                  handleNestedChange('followUpSchedule', 'preferredContactTime', e.target.value)
                }
              />
            </div>
          </div>

          {getFieldError('followUpSchedule') && (
            <span className="field-error" role="alert">
              {getFieldError('followUpSchedule')}
            </span>
          )}
        </section>

        {/* Section 6: Digital Goal Agreement */}
        <section className="form-section">
          <h3>Digital Goal Agreement</h3>

          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={clientPresent}
              onChange={e => setClientPresent(e.target.checked)}
            />
            <span>Client is present to review and agree to goals</span>
          </label>

          {clientPresent && (
            <div className="goal-agreement-section">
              <div className="goal-summary">
                <h4>Goal Summary</h4>
                {formData.goals && formData.goals.length > 0 ? (
                  <ul>
                    {formData.goals.map((goal, i) => (
                      <li key={goal.id}>
                        <strong>Goal {i + 1}:</strong> {goal.description || 'Not specified'}
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className="no-goals">No goals have been added yet.</p>
                )}
              </div>

              <label className="checkbox-label">
                <input
                  type="checkbox"
                  checked={goalAgreed}
                  onChange={e => setGoalAgreed(e.target.checked)}
                />
                <span>I agree to work toward these goals</span>
              </label>

              {goalAgreed && (
                <div className="signature-section">
                  <h5>Client Signature</h5>
                  <canvas
                    ref={canvasRef}
                    width={600}
                    height={150}
                    className="signature-canvas"
                    onMouseDown={startDrawing}
                    onMouseMove={draw}
                    onMouseUp={stopDrawing}
                    onMouseLeave={stopDrawing}
                  />
                  <button type="button" onClick={clearSignature} className="btn btn-secondary btn-sm">
                    Clear Signature
                  </button>

                  {formData.digitalGoalAgreement?.signed && (
                    <div className="signature-metadata">
                      <p>
                        Signed on{' '}
                        {new Date(formData.digitalGoalAgreement.timestamp!).toLocaleString()}
                      </p>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}
        </section>

        {/* Form Actions */}
        <div className="form-actions">
          <button type="button" onClick={onBack} className="btn btn-secondary">
            Back to Step 5
          </button>
          <button type="submit" className="btn btn-primary">
            Continue to Step 7
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

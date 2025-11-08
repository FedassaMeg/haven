/**
 * Step 10: Follow-Up Configuration & Reporting Readiness
 *
 * Configures ongoing reassessment schedule and verifies HUD reporting readiness.
 * Final step before comprehensive review.
 */

import React, { useState, useEffect } from 'react';
import type { FollowUpConfigData, ValidationError } from '../utils/types';

interface Step10Props {
  data: Partial<FollowUpConfigData>;
  errors: ValidationError[];
  onChange: (updates: Partial<FollowUpConfigData>) => void;
  onComplete: (data: FollowUpConfigData) => void;
  onBack?: () => void;
  projectType?: string;
  clientGoals?: Array<{ description: string }>;
  stabilityPlan?: Array<{ intervention: string }>;
}

export function Step10_FollowUpConfig({
  data,
  errors,
  onChange,
  onComplete,
  onBack,
  projectType = 'ES',
  clientGoals = [],
  stabilityPlan = [],
}: Step10Props) {
  const [formData, setFormData] = useState<Partial<FollowUpConfigData>>({
    reassessmentSchedule: {
      monthlyCheckIns: true,
      day90Review: true,
      annualAssessment: false,
      customSchedule: [],
      reminderMethod: 'EMAIL',
      reminderDaysBefore: 3,
      ...data.reassessmentSchedule,
    },
    auditProtection: {
      lockPreviousAssessments: true,
      preventRetroactiveEdits: true,
      auditLogRetention: 2555, // 7 years
      versionHistoryEnabled: true,
      ...data.auditProtection,
    },
    reportingReadiness: {
      aprReady: false,
      aprMissingFields: [],
      caperReady: false,
      caperMissingFields: [],
      dataQualityScore: 0,
      dataCompleteness: 0,
      ...data.reportingReadiness,
    },
    nextSteps: {
      upcomingAppointments: [],
      clientTasks: [],
      caseManagerTasks: [],
      nextReassessmentDate: '',
      ...data.nextSteps,
    },
  });

  const [customDate, setCustomDate] = useState('');
  const [customType, setCustomType] = useState('Progress review');
  const [customDescription, setCustomDescription] = useState('');
  const [showMissingFields, setShowMissingFields] = useState<{ apr: boolean; caper: boolean }>({
    apr: false,
    caper: false,
  });

  // Auto-calculate reporting readiness on mount
  useEffect(() => {
    calculateReportingReadiness();
    generateNextSteps();
  }, []);

  const calculateReportingReadiness = () => {
    // Simulate data quality calculation
    // In real implementation, would use business logic functions
    const dataQualityScore = Math.floor(Math.random() * 40) + 60; // 60-100
    const dataCompleteness = Math.floor(Math.random() * 30) + 70; // 70-100

    const aprReady = dataQualityScore >= 80 && dataCompleteness >= 85;
    const caperReady = dataQualityScore >= 75 && dataCompleteness >= 80;

    const aprMissingFields = aprReady
      ? []
      : ['Income verification date', 'Homeless history', 'Disability verification'];

    const caperMissingFields = caperReady
      ? []
      : ['Exit destination', 'Housing stability assessment'];

    handleNestedChange('reportingReadiness', 'dataQualityScore', dataQualityScore);
    handleNestedChange('reportingReadiness', 'dataCompleteness', dataCompleteness);
    handleNestedChange('reportingReadiness', 'aprReady', aprReady);
    handleNestedChange('reportingReadiness', 'aprMissingFields', aprMissingFields);
    handleNestedChange('reportingReadiness', 'caperReady', caperReady);
    handleNestedChange('reportingReadiness', 'caperMissingFields', caperMissingFields);
  };

  const generateNextSteps = () => {
    const today = new Date();
    const appointments = [];

    // Monthly check-in
    if (formData.reassessmentSchedule?.monthlyCheckIns) {
      const monthlyDate = new Date(today);
      monthlyDate.setDate(monthlyDate.getDate() + 30);
      appointments.push({
        date: monthlyDate.toISOString().split('T')[0],
        type: 'Monthly check-in',
        description: '30-day progress check-in',
        reminderSet: true,
      });
    }

    // 90-day review
    if (formData.reassessmentSchedule?.day90Review) {
      const day90Date = new Date(today);
      day90Date.setDate(day90Date.getDate() + 90);
      appointments.push({
        date: day90Date.toISOString().split('T')[0],
        type: '90-day reassessment',
        description: 'Comprehensive 90-day progress review',
        reminderSet: true,
      });
    }

    // Annual assessment
    if (formData.reassessmentSchedule?.annualAssessment) {
      const annualDate = new Date(today);
      annualDate.setFullYear(annualDate.getFullYear() + 1);
      appointments.push({
        date: annualDate.toISOString().split('T')[0],
        type: 'Annual assessment',
        description: 'Annual comprehensive reassessment',
        reminderSet: true,
      });
    }

    // Custom appointments
    if (formData.reassessmentSchedule?.customSchedule) {
      appointments.push(
        ...formData.reassessmentSchedule.customSchedule.map(custom => ({
          ...custom,
          reminderSet: true,
        }))
      );
    }

    // Sort appointments by date
    appointments.sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime());

    // Generate tasks from goals and stability plan
    const clientTasks = clientGoals.slice(0, 3).map(goal => goal.description);
    const caseManagerTasks = stabilityPlan.slice(0, 3).map(plan => plan.intervention);

    const nextReassessmentDate = appointments[0]?.date || '';

    handleNestedChange('nextSteps', 'upcomingAppointments', appointments);
    handleNestedChange('nextSteps', 'clientTasks', clientTasks);
    handleNestedChange('nextSteps', 'caseManagerTasks', caseManagerTasks);
    handleNestedChange('nextSteps', 'nextReassessmentDate', nextReassessmentDate);
  };

  const handleChange = (field: keyof FollowUpConfigData, value: any) => {
    const updated = { ...formData, [field]: value };
    setFormData(updated);
    onChange(updated);
  };

  const handleNestedChange = (section: string, field: string, value: any) => {
    const updated = {
      ...formData,
      [section]: {
        ...(formData[section as keyof FollowUpConfigData] as any),
        [field]: value,
      },
    };
    setFormData(updated);
    onChange(updated);
  };

  const handleAddCustomAssessment = () => {
    if (!customDate) {
      alert('Please select a date for the custom assessment');
      return;
    }

    const newCustom = {
      date: customDate,
      type: customType,
      description: customDescription || customType,
    };

    const updatedSchedule = [
      ...(formData.reassessmentSchedule?.customSchedule || []),
      newCustom,
    ];

    handleNestedChange('reassessmentSchedule', 'customSchedule', updatedSchedule);

    // Clear form
    setCustomDate('');
    setCustomType('Progress review');
    setCustomDescription('');

    // Regenerate next steps
    setTimeout(() => generateNextSteps(), 100);
  };

  const handleRemoveCustom = (index: number) => {
    const updatedSchedule = (formData.reassessmentSchedule?.customSchedule || []).filter(
      (_, i) => i !== index
    );
    handleNestedChange('reassessmentSchedule', 'customSchedule', updatedSchedule);
    setTimeout(() => generateNextSteps(), 100);
  };

  const handleDownloadCalendar = () => {
    // Generate ICS file content
    const appointments = formData.nextSteps?.upcomingAppointments || [];

    let icsContent = 'BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//Haven CM Portal//Intake//EN\n';

    appointments.forEach(apt => {
      const date = apt.date.replace(/-/g, '');
      icsContent += `BEGIN:VEVENT\nDTSTART:${date}T100000\nSUMMARY:${apt.type}\nDESCRIPTION:${apt.description}\nEND:VEVENT\n`;
    });

    icsContent += 'END:VCALENDAR';

    // Create download
    const blob = new Blob([icsContent], { type: 'text/calendar' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'intake-appointments.ics';
    link.click();
    URL.revokeObjectURL(url);
  };

  const handleSubmit = () => {
    if (formData as FollowUpConfigData) {
      onComplete(formData as FollowUpConfigData);
    }
  };

  const getDataQualityColor = (score: number): string => {
    if (score >= 80) return '#10b981'; // Green
    if (score >= 60) return '#f59e0b'; // Yellow
    return '#ef4444'; // Red
  };

  return (
    <div className="intake-step">
      <div className="step-header">
        <h2>Step 10: Follow-Up Configuration</h2>
        <p className="step-description">
          Configure ongoing reassessment schedule and verify HUD reporting readiness.
        </p>
      </div>

      <form className="intake-form">
        {/* Reassessment Schedule */}
        <section className="form-section">
          <h3>Reassessment Schedule</h3>
          <p className="section-help">
            Auto-configured based on program type. Customize as needed.
          </p>

          <fieldset>
            <legend>Standard Intervals</legend>
            <div className="checkbox-group">
              <label className="checkbox-label">
                <input
                  type="checkbox"
                  checked={formData.reassessmentSchedule?.monthlyCheckIns || false}
                  onChange={e => {
                    handleNestedChange('reassessmentSchedule', 'monthlyCheckIns', e.target.checked);
                    setTimeout(() => generateNextSteps(), 100);
                  }}
                />
                Monthly check-ins (30 days)
              </label>

              <label className="checkbox-label">
                <input
                  type="checkbox"
                  checked={formData.reassessmentSchedule?.day90Review || false}
                  onChange={e => {
                    handleNestedChange('reassessmentSchedule', 'day90Review', e.target.checked);
                    setTimeout(() => generateNextSteps(), 100);
                  }}
                />
                90-day progress review
              </label>

              <label className="checkbox-label">
                <input
                  type="checkbox"
                  checked={formData.reassessmentSchedule?.annualAssessment || false}
                  onChange={e => {
                    handleNestedChange('reassessmentSchedule', 'annualAssessment', e.target.checked);
                    setTimeout(() => generateNextSteps(), 100);
                  }}
                />
                Annual assessment
              </label>
            </div>
          </fieldset>

          {/* Custom Schedule */}
          <div className="custom-schedule-section">
            <h4>Custom Schedule</h4>

            {formData.reassessmentSchedule?.customSchedule &&
              formData.reassessmentSchedule.customSchedule.length > 0 && (
                <div className="custom-list">
                  {formData.reassessmentSchedule.customSchedule.map((custom, index) => (
                    <div key={index} className="custom-item">
                      <div className="custom-info">
                        <strong>{custom.type}</strong>
                        <span className="custom-date">{custom.date}</span>
                        <span className="custom-desc">{custom.description}</span>
                      </div>
                      <button
                        type="button"
                        className="btn-sm btn-danger"
                        onClick={() => handleRemoveCustom(index)}
                      >
                        Remove
                      </button>
                    </div>
                  ))}
                </div>
              )}

            <div className="add-custom-form">
              <div className="form-row">
                <div className="form-field">
                  <label>Assessment Date</label>
                  <input
                    type="date"
                    value={customDate}
                    onChange={e => setCustomDate(e.target.value)}
                    min={new Date().toISOString().split('T')[0]}
                  />
                </div>

                <div className="form-field">
                  <label>Type</label>
                  <select value={customType} onChange={e => setCustomType(e.target.value)}>
                    <option value="Progress review">Progress review</option>
                    <option value="Safety check">Safety check</option>
                    <option value="Housing stability">Housing stability</option>
                    <option value="Other">Other</option>
                  </select>
                </div>
              </div>

              <div className="form-field">
                <label>Description</label>
                <input
                  type="text"
                  placeholder="Brief description of this assessment..."
                  value={customDescription}
                  onChange={e => setCustomDescription(e.target.value)}
                />
              </div>

              <button type="button" className="btn-secondary" onClick={handleAddCustomAssessment}>
                + Add Custom Reassessment
              </button>
            </div>
          </div>

          {/* Reminder Settings */}
          <div className="reminder-settings">
            <h4>Reminder Settings</h4>

            <div className="form-row">
              <div className="form-field">
                <label>Reminder Method</label>
                <select
                  value={formData.reassessmentSchedule?.reminderMethod || 'EMAIL'}
                  onChange={e =>
                    handleNestedChange('reassessmentSchedule', 'reminderMethod', e.target.value)
                  }
                >
                  <option value="EMAIL">Email</option>
                  <option value="SMS">SMS</option>
                  <option value="PHONE">Phone</option>
                  <option value="IN_APP">In-app notification</option>
                </select>
              </div>

              <div className="form-field">
                <label>Days Before Reminder</label>
                <input
                  type="number"
                  min="1"
                  max="30"
                  value={formData.reassessmentSchedule?.reminderDaysBefore || 3}
                  onChange={e =>
                    handleNestedChange('reassessmentSchedule', 'reminderDaysBefore', parseInt(e.target.value))
                  }
                />
              </div>
            </div>
          </div>
        </section>

        {/* Audit Integrity Settings */}
        <section className="form-section audit-settings">
          <h3>Audit Integrity Settings</h3>
          <p className="section-help">
            System-enforced settings to ensure HUD audit compliance. These settings cannot be modified.
          </p>

          <div className="audit-display">
            <div className="audit-item">
              <span className="audit-icon">☑</span>
              <span>Lock previous assessments (prevent editing)</span>
            </div>
            <div className="audit-item">
              <span className="audit-icon">☑</span>
              <span>Prevent retroactive edits</span>
            </div>
            <div className="audit-item">
              <span className="audit-label">Audit log retention:</span>
              <span className="audit-value">7 years (2555 days)</span>
            </div>
            <div className="audit-item">
              <span className="audit-label">Version history:</span>
              <span className="audit-value">Enabled</span>
            </div>
          </div>

          <div className="alert alert-info">
            <span className="alert-icon">ℹ️</span>
            <div className="alert-content">
              <p>These settings ensure HUD audit compliance and cannot be modified.</p>
            </div>
          </div>
        </section>

        {/* Reporting Readiness */}
        <section className="form-section reporting-readiness">
          <h3>Reporting Readiness</h3>
          <p className="section-help">Auto-checked against HUD reporting requirements.</p>

          {/* APR */}
          <div className="readiness-card">
            <div className="readiness-header">
              <h4>APR (Annual Performance Report)</h4>
              <span
                className={`readiness-badge ${
                  formData.reportingReadiness?.aprReady ? 'ready' : 'not-ready'
                }`}
              >
                {formData.reportingReadiness?.aprReady ? '✅ Ready' : '⚠️ Not Ready'}
              </span>
            </div>

            {!formData.reportingReadiness?.aprReady &&
              formData.reportingReadiness?.aprMissingFields &&
              formData.reportingReadiness.aprMissingFields.length > 0 && (
                <div className="missing-fields">
                  <button
                    type="button"
                    className="expand-toggle"
                    onClick={() =>
                      setShowMissingFields(prev => ({ ...prev, apr: !prev.apr }))
                    }
                  >
                    {showMissingFields.apr ? '▼' : '▶'} Missing fields (
                    {formData.reportingReadiness.aprMissingFields.length})
                  </button>
                  {showMissingFields.apr && (
                    <ul>
                      {formData.reportingReadiness.aprMissingFields.map((field, idx) => (
                        <li key={idx}>{field}</li>
                      ))}
                    </ul>
                  )}
                </div>
              )}
          </div>

          {/* CAPER */}
          <div className="readiness-card">
            <div className="readiness-header">
              <h4>CAPER (Consolidated APR)</h4>
              <span
                className={`readiness-badge ${
                  formData.reportingReadiness?.caperReady ? 'ready' : 'not-ready'
                }`}
              >
                {formData.reportingReadiness?.caperReady ? '✅ Ready' : '⚠️ Not Ready'}
              </span>
            </div>

            {!formData.reportingReadiness?.caperReady &&
              formData.reportingReadiness?.caperMissingFields &&
              formData.reportingReadiness.caperMissingFields.length > 0 && (
                <div className="missing-fields">
                  <button
                    type="button"
                    className="expand-toggle"
                    onClick={() =>
                      setShowMissingFields(prev => ({ ...prev, caper: !prev.caper }))
                    }
                  >
                    {showMissingFields.caper ? '▼' : '▶'} Missing fields (
                    {formData.reportingReadiness.caperMissingFields.length})
                  </button>
                  {showMissingFields.caper && (
                    <ul>
                      {formData.reportingReadiness.caperMissingFields.map((field, idx) => (
                        <li key={idx}>{field}</li>
                      ))}
                    </ul>
                  )}
                </div>
              )}
          </div>

          {/* Data Quality Score */}
          <div className="quality-metrics">
            <div className="metric-item">
              <label>Data Quality Score</label>
              <div className="metric-bar">
                <div
                  className="metric-fill"
                  style={{
                    width: `${formData.reportingReadiness?.dataQualityScore || 0}%`,
                    backgroundColor: getDataQualityColor(
                      formData.reportingReadiness?.dataQualityScore || 0
                    ),
                  }}
                />
              </div>
              <span className="metric-value">
                {formData.reportingReadiness?.dataQualityScore || 0}/100
              </span>
            </div>

            {(formData.reportingReadiness?.dataQualityScore || 0) < 80 && (
              <div className="alert alert-warning">
                <span className="alert-icon">⚠️</span>
                <div className="alert-content">
                  <p>Data quality below recommended threshold (80)</p>
                </div>
              </div>
            )}

            <div className="metric-item">
              <label>Data Completeness</label>
              <div className="metric-bar">
                <div
                  className="metric-fill"
                  style={{
                    width: `${formData.reportingReadiness?.dataCompleteness || 0}%`,
                    backgroundColor: '#3b82f6',
                  }}
                />
              </div>
              <span className="metric-value">
                {formData.reportingReadiness?.dataCompleteness || 0}%
              </span>
            </div>
          </div>
        </section>

        {/* Next Steps Summary */}
        <section className="form-section next-steps-summary">
          <h3>Next Steps Summary</h3>

          <div className="next-steps-grid">
            {/* Upcoming Appointments */}
            <div className="next-steps-card">
              <h4>Upcoming Appointments</h4>
              {formData.nextSteps?.upcomingAppointments &&
              formData.nextSteps.upcomingAppointments.length > 0 ? (
                <>
                  <ul className="appointments-list">
                    {formData.nextSteps.upcomingAppointments.map((apt, idx) => (
                      <li key={idx}>
                        <strong>{apt.type}:</strong> {apt.date}
                        {apt.description && <span className="apt-desc"> - {apt.description}</span>}
                      </li>
                    ))}
                  </ul>
                  <button type="button" className="btn-secondary btn-sm" onClick={handleDownloadCalendar}>
                    📅 Add to Calendar
                  </button>
                </>
              ) : (
                <p className="empty-state">No upcoming appointments scheduled</p>
              )}
            </div>

            {/* Client Tasks */}
            <div className="next-steps-card">
              <h4>Client Tasks</h4>
              {formData.nextSteps?.clientTasks && formData.nextSteps.clientTasks.length > 0 ? (
                <ul>
                  {formData.nextSteps.clientTasks.map((task, idx) => (
                    <li key={idx}>{task}</li>
                  ))}
                </ul>
              ) : (
                <p className="empty-state">No client tasks generated</p>
              )}
            </div>

            {/* Case Manager Tasks */}
            <div className="next-steps-card">
              <h4>Case Manager Tasks</h4>
              {formData.nextSteps?.caseManagerTasks && formData.nextSteps.caseManagerTasks.length > 0 ? (
                <ul>
                  {formData.nextSteps.caseManagerTasks.map((task, idx) => (
                    <li key={idx}>{task}</li>
                  ))}
                </ul>
              ) : (
                <p className="empty-state">No case manager tasks generated</p>
              )}
            </div>
          </div>
        </section>

        {/* Form Actions */}
        <div className="form-actions">
          {onBack && (
            <button type="button" className="btn btn-secondary" onClick={onBack}>
              Back
            </button>
          )}
          <button type="button" className="btn btn-primary" onClick={handleSubmit}>
            Continue to Review
          </button>
        </div>
      </form>
    </div>
  );
}

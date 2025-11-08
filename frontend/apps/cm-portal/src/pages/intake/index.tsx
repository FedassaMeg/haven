/**
 * Main Intake Workflow Orchestrator (v3 - XState)
 *
 * Manages the complete 10-step intake workflow with:
 * - XState v5 state machine for workflow orchestration
 * - Auto-save to localStorage
 * - Step navigation and validation
 * - Client promotion (temp → full)
 * - Final submission and enrollment creation
 */

import React, { useEffect, useMemo } from 'react';
import { useRouter } from 'next/router';
import Link from 'next/link';
import { useActorRef, useSelector } from '@xstate/react';
import { ProtectedRoute } from '@haven/auth';
import { Card, CardHeader, CardTitle, CardContent, Button } from '@haven/ui';
import {
  Step1_InitialContact,
  Step2_SafetyAndConsent,
  Step3_RiskAssessment,
  Step4_EligibilityMatch,
  Step5_HousingBarriers,
  Step6_ServicePlan,
  Step7_DocumentUpload,
  Step8_Demographics,
  Step9_EnrollmentConfirmation,
  Step10_FollowUpConfig,
  ReviewStep,
} from '../../components/intake';
import type {
  MasterIntakeData,
  ValidationError,
  ProjectType,
} from '../../components/intake/utils/types';
import type { IntakeWorkflowServices } from '../../components/intake/state/intakeWorkflow.machine';
import { createIntakeWorkflowMachine } from '../../components/intake/state/intakeWorkflow.machine';
import AppLayout from '../../components/AppLayout';

// ============================================================================
// WORKFLOW CONFIGURATION
// ============================================================================

const WORKFLOW_STEPS = [
  { id: 1, name: 'Initial Contact', component: Step1_InitialContact, required: true },
  { id: 2, name: 'Safety & Consent', component: Step2_SafetyAndConsent, required: true },
  { id: 3, name: 'Risk Assessment', component: Step3_RiskAssessment, required: true },
  { id: 4, name: 'Eligibility', component: Step4_EligibilityMatch, required: true },
  { id: 5, name: 'Housing Barriers', component: Step5_HousingBarriers, required: false },
  { id: 6, name: 'Service Planning', component: Step6_ServicePlan, required: true },
  { id: 7, name: 'Documentation', component: Step7_DocumentUpload, required: true },
  { id: 8, name: 'Demographics', component: Step8_Demographics, required: true },
  { id: 9, name: 'Enrollment', component: Step9_EnrollmentConfirmation, required: true },
  { id: 10, name: 'Follow-up Setup', component: Step10_FollowUpConfig, required: false },
  { id: 11, name: 'Review & Submit', component: ReviewStep, required: true },
];

const STORAGE_KEY = 'intake_workflow_v3';

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

const generateTempClientId = (): string => {
  return `TEMP-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
};

const createEnrollment = async (clientId: string): Promise<string> => {
  console.log('Creating enrollment for client:', clientId);
  await new Promise(resolve => setTimeout(resolve, 1000));
  const mockEnrollmentId = `ENROLL-${Date.now()}`;
  console.log('Enrollment created:', mockEnrollmentId);
  return mockEnrollmentId;
};

const uploadDocuments = async (clientId: string, enrollmentId: string): Promise<void> => {
  console.log('Uploading documents for client:', clientId, 'enrollment:', enrollmentId);
  await new Promise(resolve => setTimeout(resolve, 500));
  console.log('Documents uploaded');
};

const createFollowUpTasks = async (clientId: string, enrollmentId: string): Promise<void> => {
  console.log('Creating follow-up tasks for client:', clientId, 'enrollment:', enrollmentId);
  await new Promise(resolve => setTimeout(resolve, 500));
  console.log('Follow-up tasks created');
};

const validateAllSteps = (context: any): Promise<Record<number, ValidationError[]>> => {
  // In real implementation, would validate each step's data
  return Promise.resolve({});
};

// ============================================================================
// MAIN COMPONENT
// ============================================================================

export default function IntakeWorkflowV3() {
  const router = useRouter();

  // Create XState machine services
  const services = useMemo<IntakeWorkflowServices>(() => ({
    loadWorkflowFromStorage: async () => {
      try {
        const stored = localStorage.getItem(STORAGE_KEY);
        if (!stored) return undefined;
        const parsed = JSON.parse(stored);
        console.log('Loaded workflow from storage:', parsed);
        return parsed;
      } catch (error) {
        console.error('Failed to load workflow from storage:', error);
        return undefined;
      }
    },
    saveWorkflowToStorage: async (input) => {
      try {
        const data = {
          masterData: input.masterData,
          currentStep: input.currentStep,
          completedSteps: input.completedSteps,
          tempClientId: input.tempClientId,
          promotedClientId: input.promotedClientId,
          timestamp: new Date().toISOString(),
        };
        localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
        console.log('Saved workflow to storage');
      } catch (error) {
        console.error('Failed to save workflow to storage:', error);
        throw error;
      }
    },
    clearWorkflowFromStorage: async () => {
      localStorage.removeItem(STORAGE_KEY);
      console.log('Cleared workflow from storage');
    },
    promoteClient: async (input) => {
      try {
        console.log('Promoting temp client to full client...');
        // In real implementation, would call API to create client
        const mockClientId = `CLIENT-${Date.now()}`;
        console.log(`Client promoted: ${input.tempClientId} → ${mockClientId}`);
        return mockClientId;
      } catch (error) {
        console.error('Failed to promote client:', error);
        throw error;
      }
    },
    validateAllSteps: async (input) => {
      return validateAllSteps(input.context);
    },
    createEnrollment: async (input) => {
      return createEnrollment(input.clientId);
    },
    uploadDocuments: async (input) => {
      return uploadDocuments(input.clientId, input.enrollmentId);
    },
    createFollowUpTasks: async (input) => {
      return createFollowUpTasks(input.clientId, input.enrollmentId);
    },
    navigate: async (input) => {
      router.push(input.path);
    },
  }), [router]);

  // Create machine instance
  const machine = useMemo(() => createIntakeWorkflowMachine(services), [services]);

  // Create actor reference
  const actorRef = useActorRef(machine);

  // Select state values using useSelector
  const masterData = useSelector(actorRef, (state) => state.context.masterData);
  const currentStep = useSelector(actorRef, (state) => state.context.currentStep);
  const completedSteps = useSelector(actorRef, (state) => state.context.completedSteps);
  const tempClientId = useSelector(actorRef, (state) => state.context.tempClientId);
  const promotedClientId = useSelector(actorRef, (state) => state.context.promotedClientId);
  const errors = useSelector(actorRef, (state) => state.context.errors);
  const isSubmitting = useSelector(actorRef, (state) => state.context.isSubmitting);
  const showExitModal = useSelector(actorRef, (state) => state.context.showExitModal);
  const hasUnsavedChanges = useSelector(actorRef, (state) => state.context.hasUnsavedChanges);

  // Warnings state (not in machine, keeping local for now)
  const warnings: Record<number, ValidationError[]> = {};

  // ============================================================================
  // LIFECYCLE HOOKS
  // ============================================================================

  // Warn before closing with unsaved changes
  useEffect(() => {
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      if (hasUnsavedChanges) {
        e.preventDefault();
        e.returnValue = '';
      }
    };

    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => window.removeEventListener('beforeunload', handleBeforeUnload);
  }, [hasUnsavedChanges]);

  // ============================================================================
  // EVENT HANDLERS (XState)
  // ============================================================================

  const handleStepNavigation = (stepId: number) => {
    actorRef.send({ type: 'NAVIGATE.STEP', step: stepId as any });
  };

  const handleBack = () => {
    if (currentStep > 1) {
      actorRef.send({ type: 'GOTO.STEP', step: (currentStep - 1) as any });
    }
  };

  const handleStepCompletion = (stepId: number, stepData: any) => {
    console.log(`Step ${stepId} completed with data:`, stepData);
    actorRef.send({ type: 'STEP.COMPLETE', stepId: stepId as any, stepData });
  };

  const handleStepDataChange = (stepId: number, updates: any) => {
    actorRef.send({ type: 'STEP.UPDATE', stepId: stepId as any, updates });
  };

  const handleFinalSubmit = () => {
    actorRef.send({ type: 'SUBMIT.REQUEST' });
  };

  const handleSaveManually = () => {
    actorRef.send({ type: 'SAVE.REQUEST' });
  };

  // Exit handling
  const handleExit = () => {
    actorRef.send({ type: 'EXIT.REQUEST' });
  };

  const handleConfirmExit = () => {
    actorRef.send({ type: 'EXIT.SAVE_AND_EXIT' });
  };

  const handleCancelExit = () => {
    actorRef.send({ type: 'EXIT.CANCEL' });
  };

  const handleDiscardExit = () => {
    actorRef.send({ type: 'EXIT.DISCARD' });
  };

  // ============================================================================
  // RENDER
  // ============================================================================

  const currentStepConfig = WORKFLOW_STEPS.find(s => s.id === currentStep);
  const CurrentStepComponent = currentStepConfig?.component;

  const getStepData = (stepId: number) => {
    switch (stepId) {
      case 1:
        return masterData.initialContact || {};
      case 2:
        return masterData.safetyAndConsent || {};
      case 3:
        return masterData.riskAssessment || {};
      case 4:
        return masterData.eligibilityMatch || {};
      case 5:
        return masterData.housingBarriers || {};
      case 6:
        return masterData.servicePlan || {};
      case 7:
        return masterData.documentation || {};
      case 8:
        return masterData.demographics || {};
      case 9:
        return masterData.enrollmentConfirmation || {};
      case 10:
        return masterData.followUpConfig || {};
      case 11:
        return masterData; // Review step gets all data
      default:
        return {};
    }
  };

  return (
    <ProtectedRoute>
      <AppLayout
        title="Client Intake"
        breadcrumbs={[
          { label: 'Dashboard', href: '/dashboard' },
          { label: 'Clients', href: '/clients' },
          { label: 'New Intake' }
        ]}
      >
        <div className="p-6">
          <div className="max-w-5xl mx-auto">
            {/* Header */}
            <div className="mb-6">
              <h1 className="text-3xl font-bold text-secondary-900">Client Intake Workflow</h1>
              <p className="text-secondary-600 mt-2">
                Complete intake process following HMIS 2024 standards
                {tempClientId && (
                  <span className="ml-4 text-sm font-mono text-secondary-500">
                    Temp ID: {tempClientId}
                    {promotedClientId && <span className="text-success-600 font-semibold"> → {promotedClientId}</span>}
                  </span>
                )}
              </p>
              <div className="mt-2 flex gap-2">
                <Button variant="outline" size="sm" onClick={handleSaveManually}>
                  Save Draft
                </Button>
                {hasUnsavedChanges && (
                  <span className="text-xs text-warning-600 self-center">Unsaved changes</span>
                )}
              </div>
            </div>

            {/* Progress Steps */}
            <div className="mb-8">
              <nav aria-label="Progress">
                <ol className="flex items-center justify-between">
                  {WORKFLOW_STEPS.map((step, index) => (
                    <li key={step.id} className="relative flex-1">
                      {index !== 0 && (
                        <div
                          className={`absolute left-0 top-5 -ml-px mt-0.5 h-0.5 w-full ${
                            completedSteps.includes(step.id)
                              ? 'bg-primary-600'
                              : currentStep === step.id
                              ? 'bg-primary-300'
                              : 'bg-secondary-200'
                          }`}
                        />
                      )}
                      <button
                        onClick={() => handleStepNavigation(step.id)}
                        disabled={!completedSteps.includes(step.id) && step.id !== currentStep + 1 && step.id !== 1}
                        className={`group relative flex flex-col items-center ${
                          !completedSteps.includes(step.id) && step.id !== currentStep + 1 && step.id !== 1 ? 'cursor-not-allowed' : 'cursor-pointer'
                        }`}
                      >
                        <span
                          className={`flex h-10 w-10 items-center justify-center rounded-full ${
                            completedSteps.includes(step.id)
                              ? 'bg-primary-600 text-white'
                              : currentStep === step.id
                              ? 'border-2 border-primary-600 bg-white text-primary-600'
                              : 'border-2 border-secondary-300 bg-white text-secondary-500'
                          }`}
                        >
                          {completedSteps.includes(step.id) ? (
                            <svg className="h-5 w-5" fill="currentColor" viewBox="0 0 20 20">
                              <path
                                fillRule="evenodd"
                                d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                                clipRule="evenodd"
                              />
                            </svg>
                          ) : (
                            step.id
                          )}
                        </span>
                        <span className="mt-2 text-xs font-medium text-secondary-900">
                          {step.name}
                        </span>
                        {step.required && (
                          <span className="text-xs text-destructive-600">*</span>
                        )}
                      </button>
                    </li>
                  ))}
                </ol>
              </nav>
            </div>

            {/* Step Content */}
            <Card className="mb-6">
              <CardHeader>
                <CardTitle>
                  Step {currentStep}: {WORKFLOW_STEPS[currentStep - 1]?.name}
                </CardTitle>
              </CardHeader>
              <CardContent>
                {/* Validation Error Summary */}
                {errors[currentStep] && errors[currentStep].length > 0 && (
                  <div className="bg-destructive-50 border border-destructive-200 rounded-lg p-4 mb-6">
                    <div className="flex">
                      <svg className="h-5 w-5 text-destructive-600 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
                        <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                      </svg>
                      <div className="ml-3">
                        <h3 className="text-sm font-medium text-destructive-800">
                          Please fix the following errors to continue:
                        </h3>
                        <ul className="mt-2 text-sm text-destructive-700 space-y-1">
                          {errors[currentStep].map((error, idx) => (
                            <li key={idx}>• {error.message}</li>
                          ))}
                        </ul>
                      </div>
                    </div>
                  </div>
                )}

                {/* Render Current Step */}
                {CurrentStepComponent && currentStep !== 11 && (
                  <CurrentStepComponent
                    data={getStepData(currentStep)}
                    errors={errors[currentStep] || []}
                    warnings={warnings[currentStep] || []}
                    onChange={(updates: any) => handleStepDataChange(currentStep, updates)}
                    onComplete={(stepData: any) => handleStepCompletion(currentStep, stepData)}
                    onBack={currentStep > 1 ? handleBack : undefined}
                    {...(currentStep === 3 && {
                      clientAlias: masterData.initialContact?.clientAlias,
                    })}
                    {...(currentStep === 4 && {
                      initialContactDate: masterData.initialContact?.contactDate,
                    })}
                    {...(currentStep === 6 && {
                      clientAlias: masterData.initialContact?.clientAlias,
                    })}
                    {...(currentStep === 8 && {
                      clientAlias: masterData.initialContact?.clientAlias,
                      safeContactPhone: masterData.safetyAndConsent?.safeContactMethods?.safePhoneNumber,
                      safeContactEmail: masterData.safetyAndConsent?.safeContactMethods?.safeEmail,
                    })}
                    {...(currentStep === 9 && {
                      clientName: masterData.demographics?.name
                        ? `${masterData.demographics.name.firstName} ${masterData.demographics.name.lastName}`
                        : undefined,
                      clientDOB: masterData.demographics?.identifiers?.birthDate,
                      householdSize: masterData.eligibilityMatch?.householdComposition?.totalSize,
                      selectedProgram: masterData.eligibilityMatch?.selectedProgram
                        ? {
                            id: masterData.eligibilityMatch.selectedProgram.programId,
                            name: masterData.eligibilityMatch.selectedProgram.programName,
                            type: (
                              [
                                'ES',
                                'TH',
                                'RRH',
                                'PSH',
                                'SO',
                                'PH',
                                'DAY',
                                'SSO',
                                'HP',
                                'CE',
                              ].includes(masterData.eligibilityMatch.selectedProgram.programType)
                                ? masterData.eligibilityMatch.selectedProgram.programType
                                : 'SSO'
                            ) as ProjectType,
                            fundingSource: masterData.eligibilityMatch.selectedProgram.fundingSource,
                            dailyRate: masterData.eligibilityMatch.selectedProgram.dailyRate ?? 0,
                          }
                        : undefined,
                      initialContactDate: masterData.initialContact?.contactDate,
                    })}
                    {...(currentStep === 10 && {
                      projectType: masterData.enrollmentConfirmation?.enrollment?.projectType,
                      clientGoals: masterData.servicePlan?.goals || [],
                    })}
                  />
                )}

                {/* Review Step */}
                {currentStep === 11 && (
                  <ReviewStep
                    data={masterData}
                    errors={Object.values(errors).flat()}
                    onEdit={handleStepNavigation}
                    onSubmit={handleFinalSubmit}
                    onBack={handleBack}
                    isSubmitting={isSubmitting}
                  />
                )}
              </CardContent>
            </Card>

            {/* Cancel Button - Always visible, navigation handled by step components */}
            {currentStep !== 11 && (
              <div className="flex items-center justify-end">
                <Link href="/clients">
                  <Button variant="ghost" onClick={handleExit}>Cancel Intake</Button>
                </Link>
              </div>
            )}

            {/* Navigation Buttons - Only shown for Review Step (11) */}
            {currentStep === 11 && (
              <div className="flex items-center justify-between">
                <Button
                  type="button"
                  variant="outline"
                  onClick={handleBack}
                  disabled={currentStep === 1}
                >
                  Previous
                </Button>

                <div className="flex items-center space-x-4">
                  <Link href="/clients">
                    <Button variant="ghost" onClick={handleExit}>Cancel Intake</Button>
                  </Link>

                  <Button
                    onClick={handleFinalSubmit}
                    disabled={isSubmitting}
                    className="bg-success-600 hover:bg-success-700"
                  >
                    {isSubmitting ? 'Completing Intake...' : 'Complete Intake'}
                  </Button>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Exit Confirmation Modal */}
        {showExitModal && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
              <h3 className="text-lg font-semibold text-secondary-900 mb-2">Exit Intake Workflow?</h3>
              <p className="text-secondary-600 mb-6">You have unsaved changes. What would you like to do?</p>
              <div className="flex justify-end gap-3">
                <Button variant="outline" onClick={handleCancelExit}>
                  Cancel
                </Button>
                <Button variant="outline" onClick={handleDiscardExit}>
                  Discard Changes
                </Button>
                <Button onClick={handleConfirmExit}>
                  Save & Exit
                </Button>
              </div>
            </div>
          </div>
        )}
      </AppLayout>
    </ProtectedRoute>
  );
}

import { assign, enqueueActions, fromPromise, raise, setup } from 'xstate';
import type { MasterIntakeData, ValidationError } from '../utils/types';

const AUTO_SAVE_DELAY = 2000;

const STEP_SEQUENCE = [
  { id: 1, next: 2 },
  { id: 2, prev: 1, next: 3 },
  { id: 3, prev: 2, next: 4 },
  { id: 4, prev: 3, next: 5 },
  { id: 5, prev: 4, next: 6 },
  { id: 6, prev: 5, next: 7 },
  { id: 7, prev: 6, next: 8 },
  { id: 8, prev: 7, next: 9 },
  { id: 9, prev: 8, next: 10 },
  { id: 10, prev: 9, next: 11 },
  { id: 11, prev: 10 },
] as const;

type StepId = (typeof STEP_SEQUENCE)[number]['id'];
type StepWithData = Exclude<StepId, 11>;

const STEP_DATA_KEYS: Record<StepWithData, keyof MasterIntakeData> = {
  1: 'initialContact',
  2: 'safetyAndConsent',
  3: 'riskAssessment',
  4: 'eligibilityMatch',
  5: 'housingBarriers',
  6: 'servicePlan',
  7: 'documentation',
  8: 'demographics',
  9: 'enrollmentConfirmation',
  10: 'followUpConfig',
};

export interface WorkflowSnapshot {
  masterData?: Partial<MasterIntakeData>;
  currentStep?: number;
  completedSteps?: number[];
  tempClientId?: string | null;
  promotedClientId?: string | null;
}
export interface IntakeWorkflowContext {
  masterData: Partial<MasterIntakeData>;
  currentStep: StepId;
  completedSteps: number[];
  tempClientId: string | null;
  promotedClientId: string | null;
  enrollmentId: string | null;
  errors: Record<number, ValidationError[]>;
  hasUnsavedChanges: boolean;
  showExitModal: boolean;
  isSubmitting: boolean;
  demographicsPayload: unknown;
  redirectPath: string;
}

export type IntakeWorkflowEvent =
  | { type: 'GOTO.STEP'; step: StepId }
  | { type: 'NAVIGATE.STEP'; step: StepId }
  | { type: 'STEP.UPDATE'; stepId: StepId; updates: unknown }
  | { type: 'STEP.COMPLETE'; stepId: StepId; stepData: unknown }
  | { type: 'DATA.DIRTY' }
  | { type: 'SAVE.REQUEST' }
  | { type: 'SAVE.COMPLETE' }
  | { type: 'EXIT.REQUEST' }
  | { type: 'EXIT.CANCEL' }
  | { type: 'EXIT.SAVE_AND_EXIT' }
  | { type: 'EXIT.DISCARD' }
  | { type: 'SUBMIT.REQUEST' }
  | { type: 'PROMOTION.REQUEST' };

type PromoteResult = string | { clientId: string };
type EnrollmentResult = string | { enrollmentId: string };

export interface IntakeWorkflowServices {
  loadWorkflowFromStorage: () => Promise<WorkflowSnapshot | undefined>;
  saveWorkflowToStorage: (input: {
    masterData: Partial<MasterIntakeData>;
    currentStep: StepId;
    completedSteps: number[];
    tempClientId: string | null;
    promotedClientId: string | null;
  }) => Promise<void>;
  clearWorkflowFromStorage: () => Promise<void>;
  promoteClient: (input: { demographics: unknown; tempClientId: string | null }) => Promise<PromoteResult>;
  validateAllSteps: (input: { context: IntakeWorkflowContext }) => Promise<Record<number, ValidationError[]>>;
  createEnrollment: (input: { clientId: string }) => Promise<EnrollmentResult>;
  uploadDocuments: (input: { clientId: string; enrollmentId: string }) => Promise<void>;
  createFollowUpTasks: (input: { clientId: string; enrollmentId: string }) => Promise<void>;
  navigate: (input: { path: string }) => Promise<void> | void;
}

const initialContext: IntakeWorkflowContext = {
  masterData: {},
  currentStep: 1,
  completedSteps: [],
  tempClientId: null,
  promotedClientId: null,
  enrollmentId: null,
  errors: {},
  hasUnsavedChanges: false,
  showExitModal: false,
  isSubmitting: false,
  demographicsPayload: null,
  redirectPath: '/clients',
};

function markCompleted(completed: number[], stepId: StepId): number[] {
  return completed.includes(stepId) ? completed : [...completed, stepId].sort((a, b) => a - b);
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function applyStepUpdate(masterData: Partial<MasterIntakeData>, stepId: StepWithData, updates: unknown) {
  const key = STEP_DATA_KEYS[stepId];
  const existing = masterData[key];
  if (isPlainObject(existing) && isPlainObject(updates)) {
    return { ...masterData, [key]: { ...(existing as Record<string, unknown>), ...(updates as Record<string, unknown>) } };
  }
  return { ...masterData, [key]: updates as MasterIntakeData[typeof key] };
}

function setStepPayload(masterData: Partial<MasterIntakeData>, stepId: StepWithData, payload: unknown) {
  const key = STEP_DATA_KEYS[stepId];
  return { ...masterData, [key]: payload as MasterIntakeData[typeof key] };
}

function generateTempClientId() {
  return `TEMP-${Date.now()}-${Math.random().toString(36).slice(2, 11)}`;
}

function extractClientId(output: PromoteResult | undefined, fallback: string | null) {
  if (!output) return fallback;
  if (typeof output === 'string') return output;
  const value = (output as { clientId?: unknown }).clientId;
  return typeof value === 'string' ? value : fallback;
}

function extractEnrollmentId(output: EnrollmentResult | undefined, fallback: string | null) {
  if (!output) return fallback;
  if (typeof output === 'string') return output;
  const value = (output as { enrollmentId?: unknown }).enrollmentId;
  return typeof value === 'string' ? value : fallback;
}

function completeStepContext(context: IntakeWorkflowContext, stepId: StepId, stepData: unknown) {
  const updates: Partial<IntakeWorkflowContext> = {
    completedSteps: markCompleted(context.completedSteps, stepId),
  };
  if (stepId !== 11) {
    updates.masterData = setStepPayload(context.masterData, stepId as StepWithData, stepData);
  }
  if (stepId === 1 && !context.tempClientId) {
    const tempId = generateTempClientId();
    updates.tempClientId = tempId;
    updates.masterData = { ...(updates.masterData ?? setStepPayload(context.masterData, 1, stepData)), tempClientId: tempId };
  }
  if (stepId === 8) {
    updates.demographicsPayload = stepData;
  }
  return updates;
}

function canNavigate(context: IntakeWorkflowContext, step: StepId) {
  return step === 1 || step === (context.currentStep + 1) || context.completedSteps.includes(step);
}

function resetWorkflowContext(): Partial<IntakeWorkflowContext> {
  return {
    masterData: {},
    completedSteps: [],
    currentStep: 1,
    tempClientId: null,
    promotedClientId: null,
    enrollmentId: null,
    hasUnsavedChanges: false,
    demographicsPayload: null,
  };
}
const stepStates = Object.fromEntries(
  STEP_SEQUENCE.map(({ id, prev, next }) => {
    const on: Record<string, unknown> = {};
    if (prev) {
      on.BACK = `step${prev}`;
    }
    if (id !== 11) {
      on['STEP.UPDATE'] = {
        guard: ({ event }: { event: IntakeWorkflowEvent }) => event.type === 'STEP.UPDATE' && event.stepId === id,
        actions: [
          assign(({ context, event }) => ({
            masterData: applyStepUpdate(context.masterData, id as StepWithData, (event as Extract<IntakeWorkflowEvent, { type: 'STEP.UPDATE' }>).updates),
          })),
          'flagDirty',
        ],
      };
      const completeActions = [
        assign(({ context, event }: { context: IntakeWorkflowContext; event: IntakeWorkflowEvent }) => completeStepContext(context, id, (event as Extract<IntakeWorkflowEvent, { type: 'STEP.COMPLETE' }>).stepData)),
        ...(id === 8
          ? [
              enqueueActions(({ context, enqueue }: { context: IntakeWorkflowContext; enqueue: any }) => {
                if (context.tempClientId && !context.promotedClientId) {
                  enqueue.raise({ type: 'PROMOTION.REQUEST' });
                }
              }),
            ]
          : []),
        'flagDirty',
      ];
      on['STEP.COMPLETE'] = {
        guard: ({ event }: { event: IntakeWorkflowEvent }) => event.type === 'STEP.COMPLETE' && event.stepId === id,
        ...(next ? { target: `step${next}` } : {}),
        actions: completeActions,
      };
    }
    return [
      `step${id}`,
      {
        entry: assign({ currentStep: () => id }),
        on,
      },
    ];
  }),
) as Record<string, unknown>;

const gotoTransitions = STEP_SEQUENCE.map(({ id }) => ({
  guard: ({ event }: { event: IntakeWorkflowEvent }) => event.type === 'GOTO.STEP' && event.step === id,
  target: `step${id}` as const,
}));

const navigateTransitions = STEP_SEQUENCE.map(({ id }) => ({
  guard: ({ context, event }: { context: IntakeWorkflowContext; event: IntakeWorkflowEvent }) =>
    event.type === 'NAVIGATE.STEP' && event.step === id && canNavigate(context, id),
  target: `step${id}` as const,
  actions: 'requestSave',
}));
export const createIntakeWorkflowMachine = (services: IntakeWorkflowServices) =>
  setup({
    types: {
      context: {} as IntakeWorkflowContext,
      events: {} as IntakeWorkflowEvent,
    },
    delays: { AUTO_SAVE_DELAY },
    guards: {
      hasUnsavedChanges: ({ context }) => context.hasUnsavedChanges,
      shouldPromote: ({ context }) => Boolean(context.tempClientId && !context.promotedClientId && context.demographicsPayload),
      needsPromotionBeforeSubmit: ({ context }) => Boolean(context.tempClientId && !context.promotedClientId),
      hasErrors: ({ context, event }) => Object.keys((event as any).output ?? {}).length > 0,
    },
    actors: {
      loadWorkflowFromStorage: fromPromise(() => services.loadWorkflowFromStorage()),
      saveWorkflowToStorage: fromPromise(({ input }: { input: Parameters<IntakeWorkflowServices['saveWorkflowToStorage']>[0] }) =>
        services.saveWorkflowToStorage(input),
      ),
      clearWorkflowFromStorage: fromPromise(() => services.clearWorkflowFromStorage()),
      promoteClient: fromPromise(({ input }: { input: Parameters<IntakeWorkflowServices['promoteClient']>[0] }) =>
        services.promoteClient(input),
      ),
      validateAllSteps: fromPromise(({ input }: { input: Parameters<IntakeWorkflowServices['validateAllSteps']>[0] }) =>
        services.validateAllSteps(input),
      ),
      createEnrollment: fromPromise(({ input }: { input: Parameters<IntakeWorkflowServices['createEnrollment']>[0] }) =>
        services.createEnrollment(input),
      ),
      uploadDocuments: fromPromise(({ input }: { input: Parameters<IntakeWorkflowServices['uploadDocuments']>[0] }) =>
        services.uploadDocuments(input),
      ),
      createFollowUpTasks: fromPromise(({ input }: { input: Parameters<IntakeWorkflowServices['createFollowUpTasks']>[0] }) =>
        services.createFollowUpTasks(input),
      ),
      navigate: fromPromise(async ({ input }: { input: Parameters<IntakeWorkflowServices['navigate']>[0] }) => {
        await services.navigate(input);
      }),
    },
    actions: {
      flagDirty: raise({ type: 'DATA.DIRTY' }),
      requestSave: raise({ type: 'SAVE.REQUEST' }),
      notifySaveComplete: raise({ type: 'SAVE.COMPLETE' }),
    },
  }).createMachine({
    id: 'IntakeWorkflow',
    initial: 'initializing',
    context: initialContext,
    states: {
      initializing: {
        invoke: {
          id: 'loadDraft',
          src: 'loadWorkflowFromStorage',
          onDone: {
            target: 'active',
            actions: assign(({ context, event }) => ({
              masterData: event.output?.masterData ?? context.masterData,
              currentStep: (event.output?.currentStep as StepId | undefined) ?? context.currentStep,
              completedSteps: event.output?.completedSteps ?? context.completedSteps,
              tempClientId: event.output?.tempClientId ?? context.tempClientId,
              promotedClientId: event.output?.promotedClientId ?? context.promotedClientId,
            })),
          },
          onError: 'active',
        },
      },
      active: {
        id: 'active',
        initial: "session",
        entry: [
          assign({ isSubmitting: () => false }),
          raise(({ context }) => ({ type: 'GOTO.STEP', step: context.currentStep })),
        ],
        on: { 'SUBMIT.REQUEST': 'submitting' },
        states: {
          session: {
            type: 'parallel',
            states: {
              stepFlow: {
                initial: 'step1',
                states: stepStates,
                on: {
                  'GOTO.STEP': gotoTransitions,
                  'NAVIGATE.STEP': navigateTransitions,
                },
              },
              persistence: {
                initial: 'clean',
                states: {
                  clean: {
                    on: {
                      'DATA.DIRTY': { target: 'dirty', actions: assign({ hasUnsavedChanges: () => true }) },
                      'SAVE.REQUEST': 'manualSaving',
                    },
                  },
                  dirty: {
                    after: { AUTO_SAVE_DELAY: 'autoSaving' },
                    on: {
                      'DATA.DIRTY': 'dirty',
                      'SAVE.REQUEST': 'manualSaving',
                    },
                  },
                  autoSaving: {
                    invoke: {
                      id: 'autoSave',
                      src: 'saveWorkflowToStorage',
                      input: ({ context }) => ({
                        masterData: context.masterData,
                        currentStep: context.currentStep,
                        completedSteps: context.completedSteps,
                        tempClientId: context.tempClientId,
                        promotedClientId: context.promotedClientId,
                      }),
                      onDone: { target: 'clean', actions: assign({ hasUnsavedChanges: () => false }) },
                      onError: 'dirty',
                    },
                  },
                  manualSaving: {
                    id: 'workflow.manualSave',
                    invoke: {
                      src: 'saveWorkflowToStorage',
                      input: ({ context }) => ({
                        masterData: context.masterData,
                        currentStep: context.currentStep,
                        completedSteps: context.completedSteps,
                        tempClientId: context.tempClientId,
                        promotedClientId: context.promotedClientId,
                      }),
                      onDone: {
                        target: 'clean',
                        actions: [assign({ hasUnsavedChanges: () => false }), 'notifySaveComplete'],
                      },
                      onError: 'dirty',
                    },
                  },
                },
              },
              exitControl: {
                initial: 'hidden',
                states: {
                  hidden: {
                    on: {
                      'EXIT.REQUEST': [
                        { guard: 'hasUnsavedChanges', target: 'prompt', actions: assign({ showExitModal: () => true }) },
                        { target: 'navigateAway' },
                      ],
                    },
                  },
                  prompt: {
                    on: {
                      'EXIT.CANCEL': { target: 'hidden', actions: assign({ showExitModal: () => false }) },
                      'EXIT.SAVE_AND_EXIT': { target: 'savingToExit', actions: assign({ showExitModal: () => true }) },
                      'EXIT.DISCARD': 'discarding',
                    },
                  },
                  savingToExit: {
                    entry: 'requestSave',
                    on: {
                      'SAVE.COMPLETE': { target: 'navigateAway', actions: assign({ showExitModal: () => false }) },
                    },
                  },
                  discarding: {
                    invoke: {
                      id: 'exitClear',
                      src: 'clearWorkflowFromStorage',
                      onDone: {
                        target: 'navigateAway',
                        actions: assign(() => ({ ...resetWorkflowContext(), showExitModal: false })),
                      },
                      onError: 'prompt',
                    },
                  },
                  navigateAway: {
                    invoke: {
                      id: 'exitNavigate',
                      src: 'navigate',
                      input: ({ context }) => ({ path: context.redirectPath }),
                      onDone: 'hidden',
                      onError: 'hidden',
                    },
                  },
                },
              },
              promotion: {
                initial: 'idle',
                states: {
                  idle: {
                    on: { 'PROMOTION.REQUEST': { guard: 'shouldPromote', target: 'promoting' } },
                  },
                  promoting: {
                    invoke: {
                      id: 'promoteEffect',
                      src: 'promoteClient',
                      input: ({ context }) => ({
                        demographics: context.demographicsPayload,
                        tempClientId: context.tempClientId,
                      }),
                      onDone: {
                        target: 'idle',
                        actions: assign(({ context, event }) => ({
                          promotedClientId: extractClientId(event.output, context.promotedClientId),
                          demographicsPayload: null,
                        })),
                      },
                      onError: {
                        target: 'idle',
                        actions: assign({ demographicsPayload: () => null }),
                      },
                    },
                  },
                },
              },
            },
          },
        },
      },
      submitting: {
        id: 'submitting',
        initial: 'validating',
        entry: assign({ isSubmitting: () => true }),
        exit: assign({ isSubmitting: () => false }),
        states: {
          validating: {
            invoke: {
              id: 'validate',
              src: 'validateAllSteps',
              input: ({ context }) => ({ context }),
              onDone: [
                { guard: 'hasErrors', target: '#active', actions: assign(({ context, event }) => ({ errors: event.output ?? context.errors })) },
                { target: 'maybePromote', actions: assign({ errors: () => ({}) }) },
              ],
              onError: '#active',
            },
          },
          maybePromote: {
            always: [
              { guard: 'needsPromotionBeforeSubmit', target: 'promoting' },
              { target: 'creatingEnrollment' },
            ],
          },
          promoting: {
            invoke: {
              id: 'submitPromote',
              src: 'promoteClient',
              input: ({ context }) => ({ demographics: context.masterData.demographics, tempClientId: context.tempClientId }),
              onDone: {
                target: 'creatingEnrollment',
                actions: assign(({ context, event }) => ({
                  promotedClientId: extractClientId(event.output, context.promotedClientId),
                })),
              },
              onError: '#active',
            },
          },
          creatingEnrollment: {
            invoke: {
              id: 'createEnrollment',
              src: 'createEnrollment',
              input: ({ context }) => ({ clientId: (context.promotedClientId ?? context.tempClientId)! }),
              onDone: {
                target: 'uploadingDocuments',
                actions: assign(({ context, event }) => ({
                  enrollmentId: extractEnrollmentId(event.output, context.enrollmentId),
                })),
              },
              onError: '#active',
            },
          },
          uploadingDocuments: {
            invoke: {
              id: 'uploadDocuments',
              src: 'uploadDocuments',
              input: ({ context }) => ({
                clientId: (context.promotedClientId ?? context.tempClientId)!,
                enrollmentId: context.enrollmentId!,
              }),
              onDone: 'creatingFollowUps',
              onError: '#active',
            },
          },
          creatingFollowUps: {
            invoke: {
              id: 'createFollowUps',
              src: 'createFollowUpTasks',
              input: ({ context }) => ({
                clientId: (context.promotedClientId ?? context.tempClientId)!,
                enrollmentId: context.enrollmentId!,
              }),
              onDone: 'clearingDraft',
              onError: '#active',
            },
          },
          clearingDraft: {
            invoke: {
              id: 'clearAfterSubmit',
              src: 'clearWorkflowFromStorage',
              onDone: {
                target: 'redirecting',
                actions: assign(({ context }) => ({ ...resetWorkflowContext(), errors: context.errors })),
              },
              onError: '#active',
            },
          },
          redirecting: {
            invoke: {
              id: 'submitNavigate',
              src: 'navigate',
              input: ({ context }) => ({ path: context.redirectPath }),
              onDone: 'complete',
              onError: 'complete',
            },
          },
          complete: { type: 'final' },
        },
        onDone: 'done',
      },
      done: { type: 'final' },
    },
  });

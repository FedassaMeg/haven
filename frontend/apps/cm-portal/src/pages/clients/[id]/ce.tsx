import { useMemo, useState, useEffect } from 'react';
import { useRouter } from 'next/router';
import { ProtectedRoute, useCurrentUser } from '@haven/auth';
import {
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  Button,
  Input,
  Select,
  Textarea,
  Badge,
  Table,
  Alert,
  AlertDescription,
  EmptyState,
} from '@haven/ui';
import AppLayout from '../../../components/AppLayout';
import {
  useClient,
  useClientEnrollments,
  useClientConsents,
  useCeAssessments,
  useCeEvents,
  useCreateCeAssessment,
  useCreateCeEvent,
} from '@haven/api-client';
import type {
  CeShareScope,
  CeAssessmentType,
  CeAssessmentLevel,
  CePrioritizationStatus,
  CeHashAlgorithm,
  CeEventType,
  CeEventStatus,
  CeEventResult,
  CreateCeAssessmentRequest,
  CreateCeEventRequest,
} from '@haven/api-client';
import { format } from 'date-fns';

const SHARE_SCOPE_OPTIONS: Array<{ value: CeShareScope; label: string; description: string }> = [
  {
    value: 'COC_COORDINATED_ENTRY',
    label: 'CoC Coordinated Entry',
    description: 'Share within the CoC CE network for coordinated case conferencing.',
  },
  {
    value: 'HMIS_PARTICIPATION',
    label: 'HMIS Participation',
    description: 'Authorize inclusion in HMIS data sets for reporting.',
  },
  {
    value: 'BY_NAME_LIST',
    label: 'By-Name List',
    description: 'Permit use on protected by-name or priority lists.',
  },
  {
    value: 'VAWA_RESTRICTED_PARTNERS',
    label: 'VAWA Restricted Partners',
    description: 'Limit visibility to VAWA-cleared partner agencies.',
  },
  {
    value: 'SYSTEM_PERFORMANCE',
    label: 'System Performance',
    description: 'Allow use in anonymized HUD performance metrics.',
  },
  {
    value: 'ADMIN_AUDIT',
    label: 'Administrative Audit',
    description: 'Enable administrative audit access for compliance.',
  },
];

const HASH_OPTIONS: Array<{ value: CeHashAlgorithm; label: string }> = [
  { value: 'SHA256_SALT', label: 'Salted SHA-256 (100k iterations)' },
  { value: 'BCRYPT', label: 'BCrypt (12 rounds)' },
];

const DEFAULT_ENCRYPTION_SCHEME = 'AES-256-GCM';

const formatDate = (value?: string | null) => {
  if (!value) return '—';
  try {
    return format(new Date(value), 'MMM d, yyyy');
  } catch (error) {
    return value;
  }
};

const todayInputValue = () => new Date().toISOString().split('T')[0];

function parseMetadataInput(input: string): Record<string, string> {
  if (!input.trim()) return {};
  return input
    .split('\n')
    .map(line => line.trim())
    .filter(Boolean)
    .reduce<Record<string, string>>((acc, line) => {
      const separatorIndex = line.indexOf('=');
      if (separatorIndex > 0) {
        const key = line.slice(0, separatorIndex).trim();
        const value = line.slice(separatorIndex + 1).trim();
        if (key) {
          acc[key] = value;
        }
      }
      return acc;
    }, {});
}

const CeWorkflowPage = () => {
  const router = useRouter();
  const { id } = router.query;
  const clientId = typeof id === 'string' ? id : null;
  const { user, fullName } = useCurrentUser();
  const actorName = fullName || user?.email || 'Unknown User';

  const { client, loading: clientLoading } = useClient(clientId);
  const { enrollments, loading: enrollmentsLoading } = useClientEnrollments(clientId);
  const { consents, loading: consentLoading, error: consentError } = useClientConsents(clientId ?? '', false);

  const [selectedEnrollmentId, setSelectedEnrollmentId] = useState<string | null>(null);

  useEffect(() => {
    if (!selectedEnrollmentId && enrollments && enrollments.length > 0) {
      setSelectedEnrollmentId(enrollments[0].enrollmentId);
    }
  }, [enrollments, selectedEnrollmentId]);

  const clientName = useMemo(() => {
    return [client?.demographics?.firstName, client?.demographics?.lastName]
      .filter(Boolean)
      .join(' ');
  }, [client]);

  const { assessments, loading: assessmentsLoading, error: assessmentsError, refresh: refreshAssessments } =
    useCeAssessments(selectedEnrollmentId);
  const { events, loading: eventsLoading, error: eventsError, refresh: refreshEvents } =
    useCeEvents(selectedEnrollmentId);

  const { createAssessment, loading: creatingAssessment, error: createAssessmentError } =
    useCreateCeAssessment(selectedEnrollmentId);
  const { createEvent, loading: creatingEvent, error: createEventError } =
    useCreateCeEvent(selectedEnrollmentId);

  const [assessmentForm, setAssessmentForm] = useState({
    consentId: '',
    consentLedgerId: '',
    assessmentDate: todayInputValue(),
    assessmentType: 'CRISIS_NEEDS' as CeAssessmentType,
    assessmentLevel: 'FULL_ASSESSMENT' as CeAssessmentLevel,
    toolUsed: '',
    score: '',
    prioritizationStatus: 'PRIORITIZED' as CePrioritizationStatus,
    hashAlgorithm: 'SHA256_SALT' as CeHashAlgorithm,
    encryptionScheme: DEFAULT_ENCRYPTION_SCHEME,
    encryptionKeyId: '',
    encryptionMetadata: '',
    encryptionTags: '',
    shareScopes: new Set<CeShareScope>(['COC_COORDINATED_ENTRY']),
  });

  const [eventForm, setEventForm] = useState({
    consentId: '',
    consentLedgerId: '',
    eventDate: todayInputValue(),
    eventType: 'REFERRAL_TO_PREVENTION' as CeEventType,
    result: 'CLIENT_ACCEPTED' as CeEventResult,
    status: 'PENDING' as CeEventStatus,
    referralDestination: '',
    outcomeDate: '',
    hashAlgorithm: 'SHA256_SALT' as CeHashAlgorithm,
    encryptionScheme: DEFAULT_ENCRYPTION_SCHEME,
    encryptionKeyId: '',
    encryptionMetadata: '',
    encryptionTags: '',
    shareScopes: new Set<CeShareScope>(['COC_COORDINATED_ENTRY']),
  });

  const selectedAssessmentConsent = useMemo(
    () => consents.find(c => c.id === assessmentForm.consentId),
    [consents, assessmentForm.consentId]
  );
  const selectedEventConsent = useMemo(
    () => consents.find(c => c.id === eventForm.consentId),
    [consents, eventForm.consentId]
  );

  useEffect(() => {
    if (selectedAssessmentConsent && !assessmentForm.consentLedgerId) {
      setAssessmentForm(prev => ({ ...prev, consentLedgerId: selectedAssessmentConsent.id }));
    }
  }, [selectedAssessmentConsent, assessmentForm.consentLedgerId]);

  useEffect(() => {
    if (selectedEventConsent && !eventForm.consentLedgerId) {
      setEventForm(prev => ({ ...prev, consentLedgerId: selectedEventConsent.id }));
    }
  }, [selectedEventConsent, eventForm.consentLedgerId]);

  const toggleAssessmentScope = (scope: CeShareScope) => {
    setAssessmentForm(prev => {
      const next = new Set(prev.shareScopes);
      if (next.has(scope)) {
        next.delete(scope);
      } else {
        next.add(scope);
      }
      return { ...prev, shareScopes: next };
    });
  };

  const toggleEventScope = (scope: CeShareScope) => {
    setEventForm(prev => {
      const next = new Set(prev.shareScopes);
      if (next.has(scope)) {
        next.delete(scope);
      } else {
        next.add(scope);
      }
      return { ...prev, shareScopes: next };
    });
  };

  const handleSubmitAssessment = async (evt: React.FormEvent) => {
    evt.preventDefault();
    if (!clientId || !selectedEnrollmentId || !assessmentForm.consentId) return;

    const payload: CreateCeAssessmentRequest = {
      clientId,
      consentId: assessmentForm.consentId,
      consentLedgerId: assessmentForm.consentLedgerId || undefined,
      assessmentDate: assessmentForm.assessmentDate,
      assessmentType: assessmentForm.assessmentType,
      assessmentLevel: assessmentForm.assessmentLevel,
      toolUsed: assessmentForm.toolUsed || undefined,
      score: assessmentForm.score ? Number(assessmentForm.score) : undefined,
      prioritizationStatus: assessmentForm.prioritizationStatus,
      shareScopes: Array.from(assessmentForm.shareScopes),
      hashAlgorithm: assessmentForm.hashAlgorithm,
      encryptionScheme: assessmentForm.encryptionScheme || undefined,
      encryptionKeyId: assessmentForm.encryptionKeyId,
      encryptionMetadata: parseMetadataInput(assessmentForm.encryptionMetadata),
      encryptionTags: assessmentForm.encryptionTags
        ? assessmentForm.encryptionTags.split(',').map(tag => tag.trim()).filter(Boolean)
        : undefined,
      createdBy: actorName,
    };

    await createAssessment(payload);
    setAssessmentForm(prev => ({
      ...prev,
      assessmentDate: todayInputValue(),
      score: '',
      toolUsed: prev.toolUsed,
      encryptionMetadata: '',
      encryptionTags: '',
    }));
    await refreshAssessments();
  };

  const handleSubmitEvent = async (evt: React.FormEvent) => {
    evt.preventDefault();
    if (!clientId || !selectedEnrollmentId || !eventForm.consentId) return;

    const payload: CreateCeEventRequest = {
      clientId,
      consentId: eventForm.consentId,
      consentLedgerId: eventForm.consentLedgerId || undefined,
      eventDate: eventForm.eventDate,
      eventType: eventForm.eventType,
      result: eventForm.result,
      status: eventForm.status,
      referralDestination: eventForm.referralDestination || undefined,
      outcomeDate: eventForm.outcomeDate || undefined,
      shareScopes: Array.from(eventForm.shareScopes),
      hashAlgorithm: eventForm.hashAlgorithm,
      encryptionScheme: eventForm.encryptionScheme || undefined,
      encryptionKeyId: eventForm.encryptionKeyId,
      encryptionMetadata: parseMetadataInput(eventForm.encryptionMetadata),
      encryptionTags: eventForm.encryptionTags
        ? eventForm.encryptionTags.split(',').map(tag => tag.trim()).filter(Boolean)
        : undefined,
      createdBy: actorName,
    };

    await createEvent(payload);
    setEventForm(prev => ({
      ...prev,
      eventDate: todayInputValue(),
      referralDestination: '',
      outcomeDate: '',
      encryptionMetadata: '',
      encryptionTags: '',
    }));
    await refreshEvents();
  };

  const loading = clientLoading || enrollmentsLoading || consentLoading;

  const enrollmentOptions = useMemo(() => {
    return [{ value: '', label: 'Select enrollment...' }].concat(
      (enrollments || []).map(enrollment => ({
        value: enrollment.enrollmentId,
        label: `${formatDate(enrollment.enrollmentDate)} • ${enrollment.programName}`,
      }))
    );
  }, [enrollments]);

  const consentOptions = useMemo(() => {
    return [{ value: '', label: 'Select consent...' }].concat(
      consents.map(consent => ({
        value: consent.id,
        label: `${consent.consentType.replace(/_/g, ' ')} • ${formatDate(consent.grantedAt)}`,
      }))
    );
  }, [consents]);

  const hasVawaConsent = Boolean(
    selectedAssessmentConsent?.isVAWAProtected || selectedEventConsent?.isVAWAProtected
  );

  return (
    <ProtectedRoute>
      <AppLayout
        title={client ? `Coordinated Entry • ${clientName}`.trim() : 'Coordinated Entry'}
        breadcrumbs={[
          { label: 'Clients', href: '/clients' },
          client ? { label: clientName || 'Client', href: `/clients/${client.id}` } : { label: 'Client' },
          { label: 'Coordinated Entry' },
        ]}
      >
        <div className="space-y-6">
          {loading && (
            <Card>
              <CardContent className="py-10 text-center text-secondary-500">
                Loading coordinated entry data…
              </CardContent>
            </Card>
          )}

          {!loading && !client && (
            <EmptyState
              title="Client not found"
              description="We could not locate this client. Please verify the link and try again."
            />
          )}

          {client && (
            <>
              <Card>
                <CardHeader>
                  <CardTitle>Client Summary</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="flex flex-wrap items-center gap-4">
                    <div>
                      <p className="text-sm text-secondary-500">Client</p>
                      <p className="text-lg font-semibold text-secondary-900">{clientName}</p>
                    </div>
                    <div>
                      <p className="text-sm text-secondary-500">Active Consents</p>
                      <p className="text-lg font-semibold text-secondary-900">{consents.length}</p>
                    </div>
                    <div className="min-w-[220px]">
                      <p className="text-sm text-secondary-500 mb-1">Selected Enrollment</p>
                      <Select
                        value={selectedEnrollmentId || ''}
                        onChange={value => setSelectedEnrollmentId(value || null)}
                        options={enrollmentOptions}
                      />
                    </div>
                  </div>

                  {consentError && (
                    <Alert>
                      <AlertDescription>{consentError}</AlertDescription>
                    </Alert>
                  )}

                  {hasVawaConsent && (
                    <Alert className="border-rose-200 bg-rose-50 text-rose-700">
                      <AlertDescription>
                        VAWA-protected consent in use. Downstream sharing is limited to cleared partners.
                      </AlertDescription>
                    </Alert>
                  )}

                  <div className="flex flex-wrap gap-3">
                    {consents.map(consent => (
                      <div key={consent.id} className="px-3 py-2 border rounded-md border-secondary-200">
                        <div className="flex items-center gap-2">
                          <Badge variant={consent.status === 'GRANTED' ? 'success' : 'outline'}>{consent.status}</Badge>
                          {consent.isVAWAProtected && <Badge variant="destructive">VAWA</Badge>}
                        </div>
                        <p className="mt-1 text-sm font-medium text-secondary-800">
                          {consent.consentType.replace(/_/g, ' ')}
                        </p>
                        <p className="text-xs text-secondary-500">Granted {formatDate(consent.grantedAt)}</p>
                      </div>
                    ))}
                    {consents.length === 0 && (
                      <p className="text-sm text-secondary-600">
                        No active consents found. Record consent before sharing Coordinated Entry data.
                      </p>
                    )}
                  </div>
                </CardContent>
              </Card>

              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <Card>
                  <CardHeader>
                    <CardTitle>Record CE Assessment</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <form className="space-y-4" onSubmit={handleSubmitAssessment}>
                      <Select
                        label="Consent"
                        value={assessmentForm.consentId}
                        onChange={value => setAssessmentForm(prev => ({ ...prev, consentId: value, consentLedgerId: value }))}
                        options={consentOptions}
                        required
                      />
                      <Input
                        label="Assessment Date"
                        type="date"
                        value={assessmentForm.assessmentDate}
                        onChange={e => setAssessmentForm(prev => ({ ...prev, assessmentDate: e.target.value }))}
                        required
                      />
                      <Select
                        label="Assessment Type"
                        value={assessmentForm.assessmentType}
                        onChange={value => setAssessmentForm(prev => ({ ...prev, assessmentType: value as CeAssessmentType }))}
                        options={[
                          { value: 'CRISIS_NEEDS', label: 'Crisis Needs' },
                          { value: 'HOUSING_NEEDS', label: 'Housing Needs' },
                          { value: 'PREVENTION', label: 'Prevention' },
                          { value: 'DIVERSION_PROBLEM_SOLVING', label: 'Diversion / Problem Solving' },
                          { value: 'TRANSFER', label: 'Transfer' },
                          { value: 'YOUTH', label: 'Youth' },
                          { value: 'FAMILY', label: 'Family' },
                          { value: 'OTHER', label: 'Other' },
                        ]}
                        required
                      />
                      <Select
                        label="Assessment Level"
                        value={assessmentForm.assessmentLevel}
                        onChange={value => setAssessmentForm(prev => ({ ...prev, assessmentLevel: value as CeAssessmentLevel }))}
                        options={[
                          { value: 'PRE_SCREEN', label: 'Pre-Screen' },
                          { value: 'FULL_ASSESSMENT', label: 'Full Assessment' },
                          { value: 'POST_ASSESSMENT', label: 'Post Assessment' },
                        ]}
                      />
                      <Input
                        label="Assessment Tool"
                        value={assessmentForm.toolUsed}
                        onChange={e => setAssessmentForm(prev => ({ ...prev, toolUsed: e.target.value }))}
                        placeholder="e.g., VI-SPDAT"
                      />
                      <Input
                        label="Score"
                        type="number"
                        value={assessmentForm.score}
                        onChange={e => setAssessmentForm(prev => ({ ...prev, score: e.target.value }))}
                        step="0.01"
                        min="0"
                      />

                      <div>
                        <p className="text-sm font-medium text-secondary-700 mb-2">Allowed Share Scopes</p>
                        <div className="space-y-2">
                          {SHARE_SCOPE_OPTIONS.map(option => (
                            <label key={option.value} className="flex items-start gap-2 text-sm text-secondary-700">
                              <input
                                type="checkbox"
                                checked={assessmentForm.shareScopes.has(option.value)}
                                onChange={() => toggleAssessmentScope(option.value)}
                                className="mt-1 h-4 w-4 text-primary-600 focus:ring-primary-500 border-secondary-300 rounded"
                              />
                              <span>
                                <span className="font-medium text-secondary-900">{option.label}</span>
                                <br />
                                <span className="text-xs text-secondary-500">{option.description}</span>
                              </span>
                            </label>
                          ))}
                        </div>
                      </div>

                      <Select
                        label="Hash Algorithm"
                        value={assessmentForm.hashAlgorithm}
                        onChange={value => setAssessmentForm(prev => ({ ...prev, hashAlgorithm: value as CeHashAlgorithm }))}
                        options={HASH_OPTIONS}
                      />
                      <Input
                        label="Encryption Key ID"
                        value={assessmentForm.encryptionKeyId}
                        onChange={e => setAssessmentForm(prev => ({ ...prev, encryptionKeyId: e.target.value }))}
                        placeholder="e.g., kms-key-123"
                        required
                      />
                      <Input
                        label="Encryption Scheme"
                        value={assessmentForm.encryptionScheme}
                        onChange={e => setAssessmentForm(prev => ({ ...prev, encryptionScheme: e.target.value }))}
                      />
                      <Textarea
                        label="Encryption Tags"
                        placeholder="Comma separated tags"
                        value={assessmentForm.encryptionTags}
                        onChange={e => setAssessmentForm(prev => ({ ...prev, encryptionTags: e.target.value }))}
                      />
                      <Textarea
                        label="Encryption Metadata"
                        placeholder="key=value per line"
                        value={assessmentForm.encryptionMetadata}
                        onChange={e => setAssessmentForm(prev => ({ ...prev, encryptionMetadata: e.target.value }))}
                      />

                      {createAssessmentError && (
                        <Alert>
                          <AlertDescription>{createAssessmentError}</AlertDescription>
                        </Alert>
                      )}

                      <Button type="submit" disabled={creatingAssessment || !selectedEnrollmentId}>
                        {creatingAssessment ? 'Saving assessment…' : 'Save Assessment'}
                      </Button>
                    </form>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <CardTitle>Record CE Event or Referral</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <form className="space-y-4" onSubmit={handleSubmitEvent}>
                      <Select
                        label="Consent"
                        value={eventForm.consentId}
                        onChange={value => setEventForm(prev => ({ ...prev, consentId: value, consentLedgerId: value }))}
                        options={consentOptions}
                        required
                      />
                      <Input
                        label="Event Date"
                        type="date"
                        value={eventForm.eventDate}
                        onChange={e => setEventForm(prev => ({ ...prev, eventDate: e.target.value }))}
                        required
                      />
                      <Select
                        label="Event Type"
                        value={eventForm.eventType}
                        onChange={value => setEventForm(prev => ({ ...prev, eventType: value as CeEventType }))}
                        options={[
                          { value: 'REFERRAL_TO_PREVENTION', label: 'Referral • Prevention' },
                          { value: 'REFERRAL_TO_STREET_OUTREACH', label: 'Referral • Street Outreach' },
                          { value: 'REFERRAL_TO_NAVIGATION', label: 'Referral • Navigation' },
                          { value: 'REFERRAL_TO_PH', label: 'Referral • Permanent Housing' },
                          { value: 'REFERRAL_TO_RRH', label: 'Referral • Rapid Re-Housing' },
                          { value: 'REFERRAL_TO_ES', label: 'Referral • Emergency Shelter' },
                          { value: 'EVENT_SAFETY_PLANNING', label: 'Safety Planning' },
                          { value: 'EVENT_DIVERSION', label: 'Diversion' },
                          { value: 'EVENT_OTHER', label: 'Other Event' },
                        ]}
                        required
                      />
                      <Select
                        label="Event Status"
                        value={eventForm.status}
                        onChange={value => setEventForm(prev => ({ ...prev, status: value as CeEventStatus }))}
                        options={[
                          { value: 'PENDING', label: 'Pending' },
                          { value: 'IN_PROGRESS', label: 'In Progress' },
                          { value: 'COMPLETED', label: 'Completed' },
                          { value: 'CLOSED', label: 'Closed' },
                        ]}
                        required
                      />
                      <Select
                        label="Event Result"
                        value={eventForm.result}
                        onChange={value => setEventForm(prev => ({ ...prev, result: value as CeEventResult }))}
                        options={[
                          { value: 'CLIENT_ACCEPTED', label: 'Client Accepted' },
                          { value: 'CLIENT_DECLINED', label: 'Client Declined' },
                          { value: 'PROVIDER_DECLINED', label: 'Provider Declined' },
                          { value: 'EXPIRED', label: 'Expired' },
                          { value: 'NO_CONTACT', label: 'No Contact' },
                          { value: 'OTHER', label: 'Other' },
                        ]}
                      />
                      <Input
                        label="Referral Destination"
                        value={eventForm.referralDestination}
                        onChange={e => setEventForm(prev => ({ ...prev, referralDestination: e.target.value }))}
                        placeholder="Receiving program or partner"
                      />
                      <Input
                        label="Outcome Date"
                        type="date"
                        value={eventForm.outcomeDate}
                        onChange={e => setEventForm(prev => ({ ...prev, outcomeDate: e.target.value }))}
                      />

                      <div>
                        <p className="text-sm font-medium text-secondary-700 mb-2">Allowed Share Scopes</p>
                        <div className="space-y-2">
                          {SHARE_SCOPE_OPTIONS.map(option => (
                            <label key={option.value} className="flex items-start gap-2 text-sm text-secondary-700">
                              <input
                                type="checkbox"
                                checked={eventForm.shareScopes.has(option.value)}
                                onChange={() => toggleEventScope(option.value)}
                                className="mt-1 h-4 w-4 text-primary-600 focus:ring-primary-500 border-secondary-300 rounded"
                              />
                              <span>
                                <span className="font-medium text-secondary-900">{option.label}</span>
                                <br />
                                <span className="text-xs text-secondary-500">{option.description}</span>
                              </span>
                            </label>
                          ))}
                        </div>
                      </div>

                      <Select
                        label="Hash Algorithm"
                        value={eventForm.hashAlgorithm}
                        onChange={value => setEventForm(prev => ({ ...prev, hashAlgorithm: value as CeHashAlgorithm }))}
                        options={HASH_OPTIONS}
                      />
                      <Input
                        label="Encryption Key ID"
                        value={eventForm.encryptionKeyId}
                        onChange={e => setEventForm(prev => ({ ...prev, encryptionKeyId: e.target.value }))}
                        placeholder="e.g., kms-key-123"
                        required
                      />
                      <Input
                        label="Encryption Scheme"
                        value={eventForm.encryptionScheme}
                        onChange={e => setEventForm(prev => ({ ...prev, encryptionScheme: e.target.value }))}
                      />
                      <Textarea
                        label="Encryption Tags"
                        placeholder="Comma separated tags"
                        value={eventForm.encryptionTags}
                        onChange={e => setEventForm(prev => ({ ...prev, encryptionTags: e.target.value }))}
                      />
                      <Textarea
                        label="Encryption Metadata"
                        placeholder="key=value per line"
                        value={eventForm.encryptionMetadata}
                        onChange={e => setEventForm(prev => ({ ...prev, encryptionMetadata: e.target.value }))}
                      />

                      {createEventError && (
                        <Alert>
                          <AlertDescription>{createEventError}</AlertDescription>
                        </Alert>
                      )}

                      <Button type="submit" disabled={creatingEvent || !selectedEnrollmentId}>
                        {creatingEvent ? 'Saving event…' : 'Save Event'}
                      </Button>
                    </form>
                  </CardContent>
                </Card>
              </div>

              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <Card>
                  <CardHeader>
                    <CardTitle>Assessment History</CardTitle>
                  </CardHeader>
                  <CardContent>
                    {assessmentsError && (
                      <Alert>
                        <AlertDescription>{assessmentsError}</AlertDescription>
                      </Alert>
                    )}
                    {assessmentsLoading ? (
                      <p className="text-sm text-secondary-600">Loading assessments…</p>
                    ) : assessments.length === 0 ? (
                      <p className="text-sm text-secondary-600">No assessments recorded for this enrollment.</p>
                    ) : (
                      <Table
                        columns={[
                          { header: 'Date', accessor: 'date' },
                          { header: 'Type', accessor: 'type' },
                          { header: 'Score', accessor: 'score' },
                          { header: 'Scopes', accessor: 'scopes' },
                          { header: 'Packet', accessor: 'packet' },
                        ]}
                        data={assessments.map(item => ({
                          key: item.id,
                          date: formatDate(item.assessmentDate),
                          type: item.assessmentType.replace(/_/g, ' '),
                          score: item.score ?? '—',
                          scopes: item.consentScope.join(', ') || 'Default',
                          packet: item.packetId ? `${item.packetId.slice(0, 8)}…` : '—',
                        }))}
                      />
                    )}
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <CardTitle>Event & Referral History</CardTitle>
                  </CardHeader>
                  <CardContent>
                    {eventsError && (
                      <Alert>
                        <AlertDescription>{eventsError}</AlertDescription>
                      </Alert>
                    )}
                    {eventsLoading ? (
                      <p className="text-sm text-secondary-600">Loading events…</p>
                    ) : events.length === 0 ? (
                      <p className="text-sm text-secondary-600">No events recorded for this enrollment.</p>
                    ) : (
                      <Table
                        columns={[
                          { header: 'Date', accessor: 'date' },
                          { header: 'Event', accessor: 'event' },
                          { header: 'Status', accessor: 'status' },
                          { header: 'Result', accessor: 'result' },
                          { header: 'Scopes', accessor: 'scopes' },
                        ]}
                        data={events.map(item => ({
                          key: item.id,
                          date: formatDate(item.eventDate),
                          event: item.eventType.replace(/_/g, ' '),
                          status: item.status,
                          result: item.result || '—',
                          scopes: item.consentScope.join(', ') || 'Default',
                        }))}
                      />
                    )}
                  </CardContent>
                </Card>
              </div>
            </>
          )}
        </div>
      </AppLayout>
    </ProtectedRoute>
  );
};

export default CeWorkflowPage;

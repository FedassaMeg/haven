import { useState, useEffect, useCallback } from 'react';
import { useRouter } from 'next/router';
import Link from 'next/link';
import { ProtectedRoute } from '@haven/auth';
import { Card, CardHeader, CardTitle, CardContent, Badge, Button, Tabs, TabsList, TabsTrigger, TabsContent } from '@haven/ui';
import { 
  useClient, 
  useClientEnrollments, 
  useIntakeAssessment, 
  useAssistanceRequests,
  useAssistanceSummary,
  useAssistanceLedger,
  useCreateAssistanceRequest,
  usePayees,
  type Client, 
  type EnrollmentSummary, 
  type IntakeAssessment as IntakeAssessmentType,
  type FinancialAssistanceRequest,
  type CreateAssistanceRequestRequest,
  AssistanceRequestType,
  AssistanceRequestStatus,
  SupportingDocumentType
} from '@haven/api-client';
import AppLayout from '../../../components/AppLayout';

// Services components
import ServiceEncounterLog from '../../../components/services/ServiceEncounterLog';
import GoalTracker from '../../../components/services/GoalTracker';
import ReferralsPanel from '../../../components/services/ReferralsPanel';
import ServiceCalendar from '../../../components/services/ServiceCalendar';
import type { ServiceEncounter, Goal, Referral } from '@haven/api-client/src/types/services';

// Safety Plan components
import SafetyPlanDashboard from '../../../components/safety-plan/SafetyPlanDashboard';
import SafetyPlanEditor from '../../../components/safety-plan/SafetyPlanEditor';
import SafetyPlanHistory from '../../../components/safety-plan/SafetyPlanHistory';
import QuickHideButton from '../../../components/safety-plan/QuickHideButton';
import type { SafetyPlan } from '@haven/api-client/src/types/safety-plan';

// Consent components
import { ConsentLedgerCard } from '../../../components/ConsentLedgerCard';

interface InfoRowProps {
  label: string;
  value?: string | React.ReactNode;
  span?: boolean;
}

const InfoRow: React.FC<InfoRowProps> = ({ label, value, span = false }) => (
  <div className={`grid ${span ? 'grid-cols-1' : 'grid-cols-3'} gap-2`}>
    <dt className="text-sm font-medium text-secondary-500">{label}:</dt>
    <dd className={`text-sm text-secondary-900 ${span ? '' : 'col-span-2'}`}>
      {value || 'N/A'}
    </dd>
  </div>
);

interface ClientHeaderProps {
  client: Client;
  onQuickHide: () => void;
}

const ClientHeader: React.FC<ClientHeaderProps> = ({ client, onQuickHide }) => {
  const fullName = client.name 
    ? `${client.name.given?.join(' ') || ''} ${client.name.family || ''}`.trim()
    : 'Unknown';

  return (
    <div className="bg-white border-b border-secondary-200 px-6 py-4 mb-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <div className="w-12 h-12 bg-secondary-200 rounded-full flex items-center justify-center">
            <span className="text-lg font-semibold text-secondary-600">
              {fullName.split(' ').map(n => n[0]).join('').toUpperCase()}
            </span>
          </div>
          <div>
            <div className="flex items-center space-x-3">
              <h2 className="text-xl font-semibold text-secondary-900">{fullName}</h2>
              {client.name?.text && client.name.text !== fullName && (
                <span className="text-sm text-secondary-600">(Alias: "{client.name.text}")</span>
              )}
              {client.isDVSurvivor && (
                <Badge variant="destructive" size="sm">
                  <svg className="w-3 h-3 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-.834-1.964-.834-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
                  </svg>
                  DV Safety Flag
                </Badge>
              )}
            </div>
            <div className="flex items-center space-x-4 mt-1">
              <Badge variant="outline" size="sm">Program: RRH</Badge>
              <Badge variant="success" size="sm">Status: Active</Badge>
            </div>
          </div>
        </div>
        {client.isDVSurvivor && <QuickHideButton onClick={onQuickHide} />}
      </div>
    </div>
  );
};

function ClientContent({ client }: { client: Client }) {
  const router = useRouter();
  const [activeTab, setActiveTab] = useState('overview');
  
  // Services state
  const [showAddEncounter, setShowAddEncounter] = useState(false);
  const [showAddGoal, setShowAddGoal] = useState(false);
  const [showAddReferral, setShowAddReferral] = useState(false);
  const [encounters, setEncounters] = useState<ServiceEncounter[]>([]);
  const [goals, setGoals] = useState<Goal[]>([]);
  const [referrals, setReferrals] = useState<Referral[]>([]);

  // Safety Plan state
  const [currentView, setCurrentView] = useState<'dashboard' | 'edit' | 'history'>('dashboard');
  const [activePlan, setActivePlan] = useState<SafetyPlan | null>(null);
  const [plans, setPlans] = useState<SafetyPlan[]>([]);
  const [isEditing, setIsEditing] = useState(false);
  const [editingPlan, setEditingPlan] = useState<SafetyPlan | null>(null);

  // Enrollment and Intake state
  const { enrollments, loading: enrollmentsLoading } = useClientEnrollments(client.id);
  const [selectedEnrollment, setSelectedEnrollment] = useState<EnrollmentSummary | null>(null);
  const { 
    assessment, 
    loading: assessmentLoading, 
    updateField, 
    completeSection, 
    submitAssessment 
  } = useIntakeAssessment(selectedEnrollment?.id || null);

  // Financial Assistance state
  const { requests: assistanceRequests, loading: requestsLoading, refetch: refetchRequests } = useAssistanceRequests(client.id);
  const { summary: assistanceSummary, loading: summaryLoading } = useAssistanceSummary(client.id, selectedEnrollment?.id || null);
  const { ledger: assistanceLedger, loading: ledgerLoading } = useAssistanceLedger(client.id, selectedEnrollment?.id || null);
  const { createRequest, submitRequest, loading: creatingRequest } = useCreateAssistanceRequest();
  const { payees, loading: payeesLoading } = usePayees();
  const [showNewRequestForm, setShowNewRequestForm] = useState(false);
  const [showLedgerView, setShowLedgerView] = useState(false);
  const [newRequest, setNewRequest] = useState<Partial<CreateAssistanceRequestRequest>>({
    clientId: client.id,
    enrollmentId: selectedEnrollment?.id || '',
    requestType: AssistanceRequestType.RENT,
    amount: 0,
    payeeId: '',
    payeeName: '',
    fundingSourceId: '',
    justification: '',
    supportingDocumentIds: []
  });

  // Quick Hide functionality
  const handleQuickHide = useCallback(() => {
    // Clear sensitive data from screen
    document.body.style.display = 'none';
    
    // Redirect to neutral page
    setTimeout(() => {
      router.push('/dashboard');
    }, 100);
    
    // Optional: Clear browser history
    if (window.history && window.history.replaceState) {
      window.history.replaceState({}, '', '/dashboard');
    }
  }, [router]);

  // Keyboard shortcut for Quick Hide (Ctrl+Shift+H)
  useEffect(() => {
    const handleKeyPress = (e: KeyboardEvent) => {
      if (e.ctrlKey && e.shiftKey && e.key === 'H') {
        e.preventDefault();
        handleQuickHide();
      }
    };

    document.addEventListener('keydown', handleKeyPress);
    return () => document.removeEventListener('keydown', handleKeyPress);
  }, [handleQuickHide]);

  // Load services data
  useEffect(() => {
    // This would be API calls for services
  }, [client.id]);

  // Load safety plans
  useEffect(() => {
    // This would be an API call
    loadSafetyPlans();
  }, [client.id]);

  // Auto-select first enrollment when enrollments are loaded
  useEffect(() => {
    if (enrollments && enrollments.length > 0 && !selectedEnrollment) {
      // Prefer INTAKE stage enrollments, then ACTIVE ones, or just use the first
      const intakeEnrollment = enrollments.find(e => e.stage === 'INTAKE');
      const activeEnrollment = enrollments.find(e => e.stage === 'ACTIVE');
      setSelectedEnrollment(intakeEnrollment || activeEnrollment || enrollments[0]);
    }
  }, [enrollments, selectedEnrollment]);

  // Update new request form when enrollment changes
  useEffect(() => {
    if (selectedEnrollment) {
      setNewRequest(prev => ({
        ...prev,
        enrollmentId: selectedEnrollment.id
      }));
    }
  }, [selectedEnrollment]);

  // Helper functions for client details
  const fullName = client.name 
    ? `${client.name.given?.join(' ') || ''} ${client.name.family || ''}`.trim()
    : 'Unknown';

  const primaryAddress = client.addresses?.find(addr => addr.use === 'HOME') || client.addresses?.[0];
  const primaryPhone = client.telecoms?.find(t => t.system === 'PHONE' && t.use === 'HOME') || 
                      client.telecoms?.find(t => t.system === 'PHONE');
  const primaryEmail = client.telecoms?.find(t => t.system === 'EMAIL');

  const formatAddress = (address: any) => {
    if (!address) return null;
    const parts = [
      address.line?.join(', '),
      address.city,
      address.state,
      address.postalCode
    ].filter(Boolean);
    return parts.join(', ');
  };

  const formatPeriod = (period: any) => {
    if (!period) return null;
    const start = period.start ? new Date(period.start).toLocaleDateString() : 'Unknown';
    const end = period.end ? new Date(period.end).toLocaleDateString() : 'Present';
    return `${start} - ${end}`;
  };

  const loadSafetyPlans = async () => {
    // Mock data - would come from API
    const mockPlans: SafetyPlan[] = [
      {
        id: 'sp-1',
        clientId: client.id,
        version: 2,
        status: 'ACTIVE' as any,
        createdAt: '2024-04-01T00:00:00Z',
        updatedAt: '2024-05-10T00:00:00Z',
        createdBy: 'user-1',
        createdByName: 'Case Manager A',
        updatedBy: 'user-2',
        updatedByName: 'Case Manager B',
        activatedAt: '2024-05-10T00:00:00Z',
        
        triggersAndRisks: {
          content: "Partner's work release dates\nLate-night unknown calls",
          items: ["Partner's work release dates", "Late-night unknown calls"],
          lastModified: '2024-05-10T00:00:00Z',
          modifiedBy: 'user-2',
          visibility: 'CLIENT_AND_CASE_MANAGER' as any
        },
        
        warningSign: {
          content: 'Sudden text messages, blocked caller IDs',
          items: ['Sudden text messages', 'Blocked caller IDs'],
          lastModified: '2024-05-10T00:00:00Z',
          modifiedBy: 'user-2',
          visibility: 'CLIENT_AND_CASE_MANAGER' as any
        },
        
        safeContacts: [
          {
            id: 'sc-1',
            name: 'Sister',
            relationship: 'Family',
            phone: '916-xxx-xxxx',
            contactMethod: 'CALL_ONLY' as any,
            safetyNotes: 'Calls only, never text',
            isEmergencyContact: true,
            isPrimaryContact: true,
            visibility: 'CLIENT_AND_CASE_MANAGER' as any
          },
          {
            id: 'sc-2',
            name: 'Advocate',
            relationship: 'DV Advocate',
            phone: '[Restricted]',
            contactMethod: 'SECURE_APP' as any,
            safetyNotes: 'Contact through secure app only',
            isEmergencyContact: true,
            isPrimaryContact: false,
            visibility: 'STAFF_ONLY' as any
          }
        ],
        
        escapePlan: {
          content: "Go to pre-arranged friend's house\nKeep bag at shelter locker",
          items: ["Go to pre-arranged friend's house", "Keep bag at shelter locker"],
          lastModified: '2024-05-10T00:00:00Z',
          modifiedBy: 'user-2',
          visibility: 'CLIENT_AND_CASE_MANAGER' as any
        },
        
        techSafety: {
          content: 'Change social media password monthly\nAvoid geotagging',
          items: ['Change social media password monthly', 'Avoid geotagging'],
          lastModified: '2024-05-10T00:00:00Z',
          modifiedBy: 'user-2',
          visibility: 'CLIENT_AND_CASE_MANAGER' as any
        },
        
        nextReviewDate: '2024-06-10',
        reviewFrequency: 'MONTHLY' as any,
        isConfidential: true,
        quickHideEnabled: true,
        autoSaveEnabled: true
      }
    ];
    
    setPlans(mockPlans);
    setActivePlan(mockPlans.find(p => p.status === 'ACTIVE') || mockPlans[0]);
  };

  // Safety Plan handlers
  const handleNewVersion = () => {
    if (activePlan) {
      const newPlan: SafetyPlan = {
        ...activePlan,
        id: `sp-new-${Date.now()}`,
        version: activePlan.version + 1,
        status: 'DRAFT' as any,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        activatedAt: undefined
      };
      setEditingPlan(newPlan);
      setCurrentView('edit');
      setIsEditing(true);
    }
  };

  const handleEditPlan = (plan: SafetyPlan) => {
    setEditingPlan(plan);
    setCurrentView('edit');
    setIsEditing(true);
  };

  const handleSavePlan = async (plan: SafetyPlan) => {
    // API call to save plan
    console.log('Saving plan:', plan);
    
    // Update local state
    const updatedPlans = [...plans];
    const index = updatedPlans.findIndex(p => p.id === plan.id);
    if (index >= 0) {
      updatedPlans[index] = plan;
    } else {
      updatedPlans.push(plan);
    }
    setPlans(updatedPlans);
    
    if (plan.status === 'ACTIVE') {
      setActivePlan(plan);
    }
    
    setCurrentView('dashboard');
    setIsEditing(false);
  };

  const handleActivatePlan = async (plan: SafetyPlan) => {
    // Deactivate current active plan
    const updatedPlans = plans.map(p => ({
      ...p,
      status: p.id === plan.id ? 'ACTIVE' as any : (p.status === 'ACTIVE' ? 'ARCHIVED' as any : p.status)
    }));
    
    setPlans(updatedPlans);
    setActivePlan({ ...plan, status: 'ACTIVE' as any });
  };

  // Financial Assistance handlers
  const handleCreateRequest = async () => {
    if (!selectedEnrollment || !newRequest.requestType || !newRequest.amount || !newRequest.payeeId || !newRequest.fundingSourceId) {
      return;
    }

    try {
      const requestData: CreateAssistanceRequestRequest = {
        clientId: client.id,
        enrollmentId: selectedEnrollment.id,
        requestType: newRequest.requestType,
        amount: newRequest.amount,
        payeeId: newRequest.payeeId,
        payeeName: newRequest.payeeName || '',
        fundingSourceId: newRequest.fundingSourceId,
        justification: newRequest.justification || '',
        notes: newRequest.notes,
        supportingDocumentIds: newRequest.supportingDocumentIds || [],
        ...(newRequest.coveragePeriodStart && { coveragePeriodStart: newRequest.coveragePeriodStart }),
        ...(newRequest.coveragePeriodEnd && { coveragePeriodEnd: newRequest.coveragePeriodEnd })
      };

      const result = await createRequest(requestData);
      await refetchRequests();
      setShowNewRequestForm(false);
      setNewRequest({
        clientId: client.id,
        enrollmentId: selectedEnrollment.id,
        requestType: AssistanceRequestType.RENT,
        amount: 0,
        payeeId: '',
        payeeName: '',
        fundingSourceId: '',
        justification: '',
        supportingDocumentIds: []
      });
      return result;
    } catch (error) {
      console.error('Failed to create assistance request:', error);
    }
  };

  const getStatusBadgeVariant = (status: AssistanceRequestStatus) => {
    switch (status) {
      case AssistanceRequestStatus.APPROVED:
        return 'success';
      case AssistanceRequestStatus.DISBURSED:
        return 'primary';
      case AssistanceRequestStatus.REJECTED:
        return 'destructive';
      case AssistanceRequestStatus.UNDER_REVIEW:
        return 'warning';
      default:
        return 'secondary';
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(amount);
  };

  return (
    <div className="space-y-6">
      <ClientHeader client={client} onQuickHide={handleQuickHide} />
      
      {/* Tab Navigation */}
      <div className="px-6">
        <Tabs value={activeTab} onValueChange={setActiveTab}>
          <TabsList className="mb-6">
            <TabsTrigger value="overview">Overview</TabsTrigger>
            <TabsTrigger value="intake">Intake</TabsTrigger>
            <TabsTrigger value="safety">Safety Plan</TabsTrigger>
            <TabsTrigger value="services">Services</TabsTrigger>
            <TabsTrigger value="financials">Financials</TabsTrigger>
            <TabsTrigger value="documents">Documents</TabsTrigger>
          </TabsList>

          {/* Overview Tab Content */}
          <TabsContent value="overview" className="space-y-6">
            {/* Header */}
            <div className="flex items-center justify-between">
              <div>
                <div className="flex items-center space-x-3">
                  <h1 className="text-2xl font-bold text-secondary-900">{fullName}</h1>
                  <Badge variant={client.status === 'ACTIVE' ? 'success' : 
                                client.status === 'INACTIVE' ? 'secondary' : 'warning'}>
                    {client.status}
                  </Badge>
                </div>
                <p className="text-secondary-600">Client ID: {client.id}</p>
                {client.createdAt && (
                  <p className="text-sm text-secondary-500">
                    Created: {new Date(client.createdAt).toLocaleDateString()}
                  </p>
                )}
              </div>
              <div className="flex items-center space-x-3">
                <Link href={`/clients/${client.id}/edit`}>
                  <Button>
                    <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                    </svg>
                    Edit Client
                  </Button>
                </Link>
                <Link href="/cases/new" className="state={{ clientId: client.id }}">
                  <Button variant="outline">
                    <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                    Open Case
                  </Button>
                </Link>
              </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* Main Information */}
              <div className="lg:col-span-2 space-y-6">
                {/* Demographics */}
                <Card>
                  <CardHeader>
                    <CardTitle>Demographics</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div className="space-y-3">
                        <InfoRow label="Full Name" value={fullName} />
                        <InfoRow label="Preferred Name" value={client.name?.text} />
                        <InfoRow label="Gender" value={client.gender?.toLowerCase().replace('_', ' ')} />
                        <InfoRow label="Date of Birth" 
                          value={client.birthDate ? new Date(client.birthDate).toLocaleDateString() : undefined} />
                      </div>
                      <div className="space-y-3">
                        <InfoRow label="Marital Status" value={client.maritalStatus?.text} />
                        <InfoRow label="Language" value={client.communication?.[0]?.language?.text} />
                        <InfoRow label="Preferred Contact" value={client.communication?.[0]?.preferred ? 'Phone' : undefined} />
                      </div>
                    </div>
                  </CardContent>
                </Card>

                {/* Contact Information */}
                <Card>
                  <CardHeader>
                    <CardTitle>Contact Information</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-4">
                      <InfoRow label="Primary Phone" value={primaryPhone?.value} />
                      <InfoRow label="Email" value={primaryEmail?.value} />
                      <InfoRow label="Primary Address" value={formatAddress(primaryAddress)} span />
                      
                      {/* All Addresses */}
                      {client.addresses && client.addresses.length > 1 && (
                        <div className="mt-4 pt-4 border-t border-secondary-200">
                          <h4 className="text-sm font-medium text-secondary-700 mb-3">All Addresses</h4>
                          <div className="space-y-3">
                            {client.addresses.map((address, index) => (
                              <div key={index} className="bg-white p-3 rounded border">
                                <div className="flex items-center justify-between mb-2">
                                  <Badge variant="outline">{address.use || 'Other'}</Badge>
                                  {address.period && (
                                    <span className="text-xs text-secondary-500">
                                      {formatPeriod(address.period)}
                                    </span>
                                  )}
                                </div>
                                <p className="text-sm text-secondary-900">{formatAddress(address)}</p>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}

                      {/* All Contact Points */}
                      {client.telecoms && client.telecoms.length > 0 && (
                        <div className="mt-4 pt-4 border-t border-secondary-200">
                          <h4 className="text-sm font-medium text-secondary-700 mb-3">All Contact Methods</h4>
                          <div className="space-y-2">
                            {client.telecoms.map((telecom, index) => (
                              <div key={index} className="flex items-center justify-between py-2 px-3 bg-white rounded border">
                                <div className="flex items-center space-x-3">
                                  <Badge variant="outline" size="sm">
                                    {telecom.system}
                                  </Badge>
                                  <span className="text-sm text-secondary-900">{telecom.value}</span>
                                </div>
                                <div className="flex items-center space-x-2">
                                  {telecom.use && (
                                    <span className="text-xs text-secondary-500 capitalize">
                                      {telecom.use.toLowerCase()}
                                    </span>
                                  )}
                                  {telecom.period && (
                                    <span className="text-xs text-secondary-500">
                                      {formatPeriod(telecom.period)}
                                    </span>
                                  )}
                                </div>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  </CardContent>
                </Card>

                {/* Household Members */}
                {client.householdMembers && client.householdMembers.length > 0 && (
                  <Card>
                    <CardHeader>
                      <div className="flex items-center justify-between">
                        <CardTitle>Household Members</CardTitle>
                        <Button size="sm" variant="outline">
                          <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                          </svg>
                          Add Member
                        </Button>
                      </div>
                    </CardHeader>
                    <CardContent>
                      <div className="space-y-4">
                        {client.householdMembers.map((member, index) => (
                          <div key={index} className="border border-secondary-200 rounded-lg p-4">
                            <div className="flex items-center justify-between mb-3">
                              <div>
                                <h4 className="font-medium text-secondary-900">
                                  {member.name?.family ? 
                                    `${member.name.given?.join(' ') || ''} ${member.name.family}`.trim() :
                                    'Household Member'
                                  }
                                </h4>
                                <p className="text-sm text-secondary-600">
                                  {member.birthDate && `Born: ${new Date(member.birthDate).toLocaleDateString()}`}
                                </p>
                              </div>
                              <div className="flex items-center space-x-2">
                                <Badge variant="outline">
                                  {member.relationship || 'Member'}
                                </Badge>
                                {member.gender && (
                                  <Badge variant="ghost" size="sm">
                                    {member.gender}
                                  </Badge>
                                )}
                              </div>
                            </div>
                            {member.notes && (
                              <div className="mt-3 pt-3 border-t border-secondary-200">
                                <p className="text-sm text-secondary-700">{member.notes}</p>
                              </div>
                            )}
                          </div>
                        ))}
                      </div>
                    </CardContent>
                  </Card>
                )}

                {/* Consent Management */}
                <ConsentLedgerCard clientId={id} />

                {/* Emergency Contact */}
                {client.contact && client.contact.length > 0 && (
                  <Card>
                    <CardHeader>
                      <CardTitle>Emergency Contacts</CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="space-y-4">
                        {client.contact.map((contact, index) => (
                          <div key={index} className="p-4 bg-secondary-50 rounded-lg">
                            <div className="flex items-center justify-between mb-3">
                              <h4 className="font-medium text-secondary-900">
                                {contact.name?.family ? 
                                  `${contact.name.given?.join(' ') || ''} ${contact.name.family}`.trim() :
                                  'Emergency Contact'
                                }
                              </h4>
                              <Badge variant="outline">
                                {contact.relationship?.[0]?.text || 'Contact'}
                              </Badge>
                            </div>
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                              {contact.telecom?.map((tel, telIndex) => (
                                <div key={telIndex} className="flex items-center space-x-2">
                                  <Badge variant="ghost" size="sm">{tel.system}</Badge>
                                  <span className="text-sm">{tel.value}</span>
                                </div>
                              ))}
                            </div>
                            {contact.address && (
                              <div className="mt-3 pt-3 border-t border-secondary-200">
                                <p className="text-sm text-secondary-700">{formatAddress(contact.address)}</p>
                              </div>
                            )}
                          </div>
                        ))}
                      </div>
                    </CardContent>
                  </Card>
                )}
              </div>

              {/* Sidebar */}
              <div className="space-y-6">
                {/* Quick Stats */}
                <Card>
                  <CardHeader>
                    <CardTitle>Quick Stats</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-4">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-secondary-600">Active Cases</span>
                        <Badge variant="primary">3</Badge>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-secondary-600">Total Cases</span>
                        <span className="text-sm font-medium">7</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-secondary-600">Last Contact</span>
                        <span className="text-sm font-medium">2 days ago</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-secondary-600">Case Manager</span>
                        <span className="text-sm font-medium">Sarah Johnson</span>
                      </div>
                    </div>
                  </CardContent>
                </Card>

                {/* Recent Activity */}
                <Card>
                  <CardHeader>
                    <CardTitle>Recent Activity</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-3">
                      <div className="flex items-start space-x-3">
                        <div className="w-2 h-2 bg-primary-500 rounded-full mt-2"></div>
                        <div className="flex-1">
                          <p className="text-sm text-secondary-900">Case updated</p>
                          <p className="text-xs text-secondary-500">2 hours ago</p>
                        </div>
                      </div>
                      <div className="flex items-start space-x-3">
                        <div className="w-2 h-2 bg-success-500 rounded-full mt-2"></div>
                        <div className="flex-1">
                          <p className="text-sm text-secondary-900">Address verified</p>
                          <p className="text-xs text-secondary-500">1 day ago</p>
                        </div>
                      </div>
                      <div className="flex items-start space-x-3">
                        <div className="w-2 h-2 bg-warning-500 rounded-full mt-2"></div>
                        <div className="flex-1">
                          <p className="text-sm text-secondary-900">Document requested</p>
                          <p className="text-xs text-secondary-500">3 days ago</p>
                        </div>
                      </div>
                    </div>
                    <div className="mt-4 pt-4 border-t border-secondary-200">
                      <Link href={`/clients/${client.id}/activity`} className="text-sm text-primary-600 hover:text-primary-700">
                        View all activity
                      </Link>
                    </div>
                  </CardContent>
                </Card>

                {/* Actions */}
                <Card>
                  <CardHeader>
                    <CardTitle>Actions</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-3">
                      <Link href={`/cases?clientId=${client.id}`}>
                        <Button variant="outline" className="w-full justify-start">
                          <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                          </svg>
                          View Cases
                        </Button>
                      </Link>
                      <Link href={`/services?clientId=${client.id}`}>
                        <Button variant="outline" className="w-full justify-start">
                          <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                          </svg>
                          View Services
                        </Button>
                      </Link>
                      <Link href={`/services/new?clientId=${client.id}`}>
                        <Button variant="outline" className="w-full justify-start">
                          <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                          </svg>
                          Create Service
                        </Button>
                      </Link>
                      <Button variant="outline" className="w-full justify-start">
                        <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                        </svg>
                        Generate Report
                      </Button>
                      <Button variant="outline" className="w-full justify-start">
                        <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 4V2a1 1 0 011-1h8a1 1 0 011 1v2h3a1 1 0 110 2h-1v12a2 2 0 01-2 2H7a2 2 0 01-2-2V6H4a1 1 0 110-2h3z" />
                        </svg>
                        Archive Client
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              </div>
            </div>
          </TabsContent>

          {/* Services Tab Content */}
          <TabsContent value="services" className="space-y-6">
            {/* Services Dashboard Header */}
            <div className="bg-white border border-secondary-200 rounded-lg p-4">
              <div className="flex items-center justify-between">
                <h2 className="text-lg font-semibold text-secondary-900">Services Dashboard</h2>
                <div className="flex items-center space-x-3">
                  <Button 
                    onClick={() => setShowAddEncounter(true)}
                    size="sm"
                  >
                    <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                    </svg>
                    Add Service Encounter
                  </Button>
                  <Button 
                    onClick={() => setShowAddGoal(true)}
                    size="sm"
                    variant="outline"
                  >
                    <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    Add Goal
                  </Button>
                  <Button 
                    onClick={() => setShowAddReferral(true)}
                    size="sm"
                    variant="outline"
                  >
                    <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
                    </svg>
                    Add Referral
                  </Button>
                </div>
              </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* Service Log Timeline - Main Column */}
              <div className="lg:col-span-2">
                <ServiceEncounterLog 
                  clientId={client.id}
                  encounters={encounters}
                  onAddEncounter={() => setShowAddEncounter(true)}
                  showAddDialog={showAddEncounter}
                  onCloseDialog={() => setShowAddEncounter(false)}
                />
              </div>

              {/* Side Panels */}
              <div className="space-y-6">
                {/* Goal Tracker Panel */}
                <GoalTracker
                  clientId={client.id}
                  goals={goals}
                  onAddGoal={() => setShowAddGoal(true)}
                  showAddDialog={showAddGoal}
                  onCloseDialog={() => setShowAddGoal(false)}
                />

                {/* Referrals Panel */}
                <ReferralsPanel
                  clientId={client.id}
                  referrals={referrals}
                  onAddReferral={() => setShowAddReferral(true)}
                  showAddDialog={showAddReferral}
                  onCloseDialog={() => setShowAddReferral(false)}
                />

                {/* Quick Actions */}
                <Card>
                  <CardHeader>
                    <CardTitle className="text-base">Quick Actions</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-2">
                      <Button variant="ghost" className="w-full justify-start" size="sm">
                        <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                        </svg>
                        Schedule Appointment
                      </Button>
                      <Button variant="ghost" className="w-full justify-start" size="sm">
                        <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                        </svg>
                        Generate Service Report
                      </Button>
                      <Button variant="ghost" className="w-full justify-start" size="sm">
                        <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                        Set Reminder
                      </Button>
                      <Button variant="ghost" className="w-full justify-start" size="sm">
                        <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                        </svg>
                        Export Service History
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              </div>
            </div>

            {/* Calendar View Section */}
            <div className="mt-8">
              <Card>
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <CardTitle>Service Calendar</CardTitle>
                    <div className="flex items-center space-x-2">
                      <Button size="sm" variant="outline">
                        <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
                        </svg>
                        Previous
                      </Button>
                      <Button size="sm" variant="outline">Today</Button>
                      <Button size="sm" variant="outline">
                        Next
                        <svg className="w-4 h-4 ml-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                        </svg>
                      </Button>
                    </div>
                  </div>
                </CardHeader>
                <CardContent>
                  <ServiceCalendar clientId={client.id} />
                </CardContent>
              </Card>
            </div>
          </TabsContent>

          {/* Safety Plan Tab Content */}
          <TabsContent value="safety" className="space-y-6">
            {/* Safety Plan Dashboard Header */}
            <div className="bg-white border border-secondary-200 rounded-lg p-4">
              <div className="flex items-center justify-between">
                <h2 className="text-lg font-semibold text-secondary-900">Safety Plan Dashboard</h2>
                <div className="flex items-center space-x-3">
                  <Button 
                    onClick={handleNewVersion}
                    size="sm"
                    disabled={!activePlan}
                  >
                    <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                    </svg>
                    New Version
                  </Button>
                  <QuickHideButton onClick={handleQuickHide} variant="outline" />
                  <Button 
                    onClick={() => setCurrentView('history')}
                    size="sm"
                    variant="outline"
                  >
                    <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    View History
                  </Button>
                </div>
              </div>
            </div>

            {/* Content based on current view */}
            {currentView === 'dashboard' && activePlan && (
              <SafetyPlanDashboard
                plan={activePlan}
                onEdit={() => handleEditPlan(activePlan)}
                onNewVersion={handleNewVersion}
                onQuickHide={handleQuickHide}
              />
            )}
            
            {currentView === 'edit' && editingPlan && (
              <SafetyPlanEditor
                plan={editingPlan}
                onSave={handleSavePlan}
                onCancel={() => {
                  setCurrentView('dashboard');
                  setIsEditing(false);
                }}
                onQuickHide={handleQuickHide}
                autoSaveEnabled={true}
              />
            )}
            
            {currentView === 'history' && (
              <SafetyPlanHistory
                plans={plans}
                activePlanId={activePlan?.id}
                onViewPlan={(plan) => {
                  setActivePlan(plan);
                  setCurrentView('dashboard');
                }}
                onRestorePlan={(plan) => handleActivatePlan(plan)}
                onEditPlan={handleEditPlan}
              />
            )}
            
            {!activePlan && currentView === 'dashboard' && (
              <Card>
                <CardContent>
                  <div className="text-center py-12">
                    <svg className="w-16 h-16 mx-auto mb-4 text-secondary-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                    </svg>
                    <h3 className="text-lg font-semibold text-secondary-900 mb-2">No Active Safety Plan</h3>
                    <p className="text-secondary-600 mb-6">Create a safety plan to help protect the client.</p>
                    <Button onClick={() => {
                      const newPlan: SafetyPlan = {
                        id: `sp-new-${Date.now()}`,
                        clientId: client.id,
                        version: 1,
                        status: 'DRAFT' as any,
                        createdAt: new Date().toISOString(),
                        updatedAt: new Date().toISOString(),
                        createdBy: 'current-user',
                        createdByName: 'Current User',
                        triggersAndRisks: {
                          content: '',
                          items: [],
                          lastModified: new Date().toISOString(),
                          modifiedBy: 'current-user',
                          visibility: 'CLIENT_AND_CASE_MANAGER' as any
                        },
                        warningSign: {
                          content: '',
                          items: [],
                          lastModified: new Date().toISOString(),
                          modifiedBy: 'current-user',
                          visibility: 'CLIENT_AND_CASE_MANAGER' as any
                        },
                        safeContacts: [],
                        escapePlan: {
                          content: '',
                          items: [],
                          lastModified: new Date().toISOString(),
                          modifiedBy: 'current-user',
                          visibility: 'CLIENT_AND_CASE_MANAGER' as any
                        },
                        techSafety: {
                          content: '',
                          items: [],
                          lastModified: new Date().toISOString(),
                          modifiedBy: 'current-user',
                          visibility: 'CLIENT_AND_CASE_MANAGER' as any
                        },
                        isConfidential: true,
                        quickHideEnabled: true,
                        autoSaveEnabled: true
                      };
                      setEditingPlan(newPlan);
                      setCurrentView('edit');
                      setIsEditing(true);
                    }}>
                      Create Safety Plan
                    </Button>
                  </div>
                </CardContent>
              </Card>
            )}
          </TabsContent>

          {/* Intake Tab Content */}
          <TabsContent value="intake" className="space-y-6">
            {/* Intake Dashboard Header */}
            <div className="bg-white border border-secondary-200 rounded-lg p-4">
              <div className="flex items-center justify-between">
                <h2 className="text-lg font-semibold text-secondary-900">Intake Assessment</h2>
                <div className="flex items-center space-x-3">
                  {enrollments && enrollments.length > 1 && (
                    <select
                      value={selectedEnrollment?.id || ''}
                      onChange={(e) => {
                        const enrollment = enrollments.find(enr => enr.id === e.target.value);
                        setSelectedEnrollment(enrollment || null);
                      }}
                      className="px-3 py-1 border border-secondary-300 rounded text-sm"
                    >
                      <option value="">Select Enrollment</option>
                      {enrollments.map((enrollment) => (
                        <option key={enrollment.id} value={enrollment.id}>
                          {enrollment.programName} - {new Date(enrollment.enrollmentDate).toLocaleDateString()} ({enrollment.stage})
                        </option>
                      ))}
                    </select>
                  )}
                  {assessment && (
                    <Button
                      onClick={() => submitAssessment()}
                      size="sm"
                      disabled={assessment.completionPercentage < 100}
                    >
                      <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                      Submit Assessment
                    </Button>
                  )}
                </div>
              </div>
            </div>

            {/* Loading states */}
            {(enrollmentsLoading || assessmentLoading) && (
              <div className="flex items-center justify-center py-12">
                <div className="text-center">
                  <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600 mx-auto mb-4"></div>
                  <p className="text-secondary-600">Loading intake information...</p>
                </div>
              </div>
            )}

            {/* No enrollments state */}
            {!enrollmentsLoading && (!enrollments || enrollments.length === 0) && (
              <Card>
                <CardContent>
                  <div className="text-center py-12">
                    <svg className="w-16 h-16 mx-auto mb-4 text-secondary-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                    <h3 className="text-lg font-semibold text-secondary-900 mb-2">No Program Enrollments</h3>
                    <p className="text-secondary-600 mb-6">This client is not currently enrolled in any programs that require intake assessment.</p>
                    <Link href={`/enrollments/new?clientId=${client.id}`}>
                      <Button>
                        Create New Enrollment
                      </Button>
                    </Link>
                  </div>
                </CardContent>
              </Card>
            )}

            {/* Assessment content */}
            {!assessmentLoading && assessment && (
              <div className="space-y-6">
                {/* Assessment Overview */}
                <Card>
                  <CardHeader>
                    <div className="flex items-center justify-between">
                      <CardTitle>Assessment Overview</CardTitle>
                      <div className="flex items-center space-x-4">
                        <div className="flex items-center space-x-2">
                          <span className="text-sm text-secondary-600">Progress:</span>
                          <div className="w-32 bg-secondary-200 rounded-full h-2">
                            <div 
                              className="bg-primary-600 h-2 rounded-full transition-all duration-300"
                              style={{ width: `${assessment.completionPercentage}%` }}
                            ></div>
                          </div>
                          <span className="text-sm font-medium text-secondary-900">
                            {assessment.completionPercentage}%
                          </span>
                        </div>
                        <Badge 
                          variant={
                            assessment.status === 'COMPLETED' ? 'success' :
                            assessment.status === 'IN_PROGRESS' ? 'warning' :
                            assessment.status === 'APPROVED' ? 'primary' : 'secondary'
                          }
                        >
                          {assessment.status.replace('_', ' ')}
                        </Badge>
                      </div>
                    </div>
                  </CardHeader>
                  <CardContent>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                      <div>
                        <span className="text-sm text-secondary-600">Assessment Type:</span>
                        <p className="font-medium">{assessment.assessmentType.replace('_', ' ')}</p>
                      </div>
                      <div>
                        <span className="text-sm text-secondary-600">Assessor:</span>
                        <p className="font-medium">{assessment.assessorName}</p>
                      </div>
                      <div>
                        <span className="text-sm text-secondary-600">Last Modified:</span>
                        <p className="font-medium">{new Date(assessment.lastModified).toLocaleDateString()}</p>
                      </div>
                    </div>
                  </CardContent>
                </Card>

                {/* Assessment Sections */}
                <div className="space-y-4">
                  {assessment.sections.map((section) => (
                    <Card key={section.id}>
                      <CardHeader>
                        <div className="flex items-center justify-between">
                          <div className="flex items-center space-x-3">
                            <CardTitle className="text-lg">{section.name}</CardTitle>
                            {section.isRequired && (
                              <Badge variant="destructive" size="sm">Required</Badge>
                            )}
                            {section.isCompleted && (
                              <Badge variant="success" size="sm">
                                <svg className="w-3 h-3 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                </svg>
                                Complete
                              </Badge>
                            )}
                          </div>
                          {!section.isCompleted && (
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => completeSection(section.id)}
                            >
                              Mark Complete
                            </Button>
                          )}
                        </div>
                        {section.description && (
                          <p className="text-sm text-secondary-600">{section.description}</p>
                        )}
                      </CardHeader>
                      <CardContent>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                          {section.fields.map((field) => (
                            <div key={field.id} className="space-y-2">
                              <label className="block text-sm font-medium text-secondary-700">
                                {field.label}
                                {field.isRequired && <span className="text-red-500 ml-1">*</span>}
                              </label>
                              {field.description && (
                                <p className="text-xs text-secondary-500">{field.description}</p>
                              )}
                              
                              {/* Field input based on type */}
                              {field.type === 'TEXT' && (
                                <input
                                  type="text"
                                  value={field.value || ''}
                                  onChange={(e) => updateField(field.id, e.target.value)}
                                  className="w-full px-3 py-2 border border-secondary-300 rounded-md focus:ring-primary-500 focus:border-primary-500"
                                  placeholder={field.label}
                                />
                              )}
                              
                              {field.type === 'TEXTAREA' && (
                                <textarea
                                  value={field.value || ''}
                                  onChange={(e) => updateField(field.id, e.target.value)}
                                  rows={3}
                                  className="w-full px-3 py-2 border border-secondary-300 rounded-md focus:ring-primary-500 focus:border-primary-500"
                                  placeholder={field.label}
                                />
                              )}
                              
                              {field.type === 'NUMBER' && (
                                <input
                                  type="number"
                                  value={field.value || ''}
                                  onChange={(e) => updateField(field.id, e.target.value)}
                                  className="w-full px-3 py-2 border border-secondary-300 rounded-md focus:ring-primary-500 focus:border-primary-500"
                                  placeholder={field.label}
                                />
                              )}
                              
                              {field.type === 'DATE' && (
                                <input
                                  type="date"
                                  value={field.value || ''}
                                  onChange={(e) => updateField(field.id, e.target.value)}
                                  className="w-full px-3 py-2 border border-secondary-300 rounded-md focus:ring-primary-500 focus:border-primary-500"
                                />
                              )}
                              
                              {field.type === 'BOOLEAN' && (
                                <div className="flex items-center space-x-3">
                                  <label className="flex items-center">
                                    <input
                                      type="radio"
                                      name={field.id}
                                      checked={field.value === true}
                                      onChange={() => updateField(field.id, true)}
                                      className="mr-2"
                                    />
                                    Yes
                                  </label>
                                  <label className="flex items-center">
                                    <input
                                      type="radio"
                                      name={field.id}
                                      checked={field.value === false}
                                      onChange={() => updateField(field.id, false)}
                                      className="mr-2"
                                    />
                                    No
                                  </label>
                                </div>
                              )}
                              
                              {field.type === 'SELECT' && field.options && (
                                <select
                                  value={field.value || ''}
                                  onChange={(e) => updateField(field.id, e.target.value)}
                                  className="w-full px-3 py-2 border border-secondary-300 rounded-md focus:ring-primary-500 focus:border-primary-500"
                                >
                                  <option value="">Select {field.label}</option>
                                  {field.options.map((option) => (
                                    <option key={option.value} value={option.value}>
                                      {option.label}
                                    </option>
                                  ))}
                                </select>
                              )}
                              
                              {field.type === 'MULTI_SELECT' && field.options && (
                                <div className="space-y-2">
                                  {field.options.map((option) => (
                                    <label key={option.value} className="flex items-center">
                                      <input
                                        type="checkbox"
                                        checked={(field.value || []).includes(option.value)}
                                        onChange={(e) => {
                                          const currentValues = field.value || [];
                                          const newValues = e.target.checked
                                            ? [...currentValues, option.value]
                                            : currentValues.filter(v => v !== option.value);
                                          updateField(field.id, newValues);
                                        }}
                                        className="mr-2"
                                      />
                                      {option.label}
                                    </label>
                                  ))}
                                </div>
                              )}
                            </div>
                          ))}
                        </div>
                        
                        {section.completedAt && (
                          <div className="mt-4 pt-4 border-t border-secondary-200">
                            <p className="text-xs text-secondary-500">
                              Completed on {new Date(section.completedAt).toLocaleDateString()} by {section.completedBy}
                            </p>
                          </div>
                        )}
                      </CardContent>
                    </Card>
                  ))}
                </div>
              </div>
            )}

            {/* No assessment available */}
            {!assessmentLoading && selectedEnrollment && !assessment && (
              <Card>
                <CardContent>
                  <div className="text-center py-12">
                    <svg className="w-16 h-16 mx-auto mb-4 text-secondary-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                    <h3 className="text-lg font-semibold text-secondary-900 mb-2">No Intake Assessment</h3>
                    <p className="text-secondary-600 mb-6">No intake assessment found for the selected enrollment.</p>
                    <Button>
                      Create Intake Assessment
                    </Button>
                  </div>
                </CardContent>
              </Card>
            )}
          </TabsContent>
          
          {/* Financial Assistance Tab Content */}
          <TabsContent value="financials" className="space-y-6">
            {/* Financial Assistance Dashboard Header */}
            <div className="bg-white border border-secondary-200 rounded-lg p-4">
              <div className="flex items-center justify-between">
                <h2 className="text-lg font-semibold text-secondary-900">Financial Assistance Dashboard</h2>
                <div className="flex items-center space-x-3">
                  <Button 
                    onClick={() => setShowNewRequestForm(true)}
                    size="sm"
                  >
                    <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                    </svg>
                    New Request
                  </Button>
                  <Button 
                    onClick={() => setShowLedgerView(true)}
                    size="sm"
                    variant="outline"
                  >
                    <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                    View Ledger
                  </Button>
                  <Button 
                    size="sm"
                    variant="outline"
                  >
                    <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                    </svg>
                    Upload Supporting Docs
                  </Button>
                </div>
              </div>
            </div>

            {!showNewRequestForm && !showLedgerView && (
              <>
                {/* Assistance Summary Cards */}
                {assistanceSummary && (
                  <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                    <Card>
                      <CardHeader className="pb-2">
                        <CardTitle className="text-sm font-medium text-secondary-600">RENT</CardTitle>
                      </CardHeader>
                      <CardContent>
                        <div className="space-y-1">
                          <p className="text-lg font-semibold">
                            {formatCurrency(assistanceSummary.byType.rent.monthlyAmount || 0)} / mo
                          </p>
                          <p className="text-sm text-secondary-600">
                            {assistanceSummary.byType.rent.coverageEndDate 
                              ? `Approved, ends ${new Date(assistanceSummary.byType.rent.coverageEndDate).toLocaleDateString()}`
                              : 'No active coverage'
                            }
                          </p>
                        </div>
                      </CardContent>
                    </Card>

                    <Card>
                      <CardHeader className="pb-2">
                        <CardTitle className="text-sm font-medium text-secondary-600">UTILITIES</CardTitle>
                      </CardHeader>
                      <CardContent>
                        <div className="space-y-1">
                          <p className="text-lg font-semibold">
                            {formatCurrency(assistanceSummary.byType.utilities.monthlyAmount || 0)} / mo
                          </p>
                          <p className="text-sm text-secondary-600">
                            {assistanceSummary.byType.utilities.pending > 0 ? 'Pending Approval' : 'No active coverage'}
                          </p>
                        </div>
                      </CardContent>
                    </Card>

                    <Card>
                      <CardHeader className="pb-2">
                        <CardTitle className="text-sm font-medium text-secondary-600">DEPOSIT</CardTitle>
                      </CardHeader>
                      <CardContent>
                        <div className="space-y-1">
                          <p className="text-lg font-semibold">
                            {formatCurrency(assistanceSummary.byType.deposit.disbursed)}
                          </p>
                          <p className="text-sm text-secondary-600">
                            {assistanceSummary.byType.deposit.disbursed > 0 
                              ? 'Paid' 
                              : 'Not requested'
                            }
                          </p>
                        </div>
                      </CardContent>
                    </Card>

                    <Card>
                      <CardHeader className="pb-2">
                        <CardTitle className="text-sm font-medium text-secondary-600">BUDGET STATUS</CardTitle>
                      </CardHeader>
                      <CardContent>
                        <div className="space-y-2">
                          <div className="flex justify-between text-sm">
                            <span>Remaining:</span>
                            <span className="font-medium">{formatCurrency(assistanceSummary.remainingBudget)}</span>
                          </div>
                          <div className="w-full bg-secondary-200 rounded-full h-2">
                            <div 
                              className="bg-primary-600 h-2 rounded-full"
                              style={{ 
                                width: `${Math.min(100, ((assistanceSummary.budgetCap - assistanceSummary.remainingBudget) / assistanceSummary.budgetCap) * 100)}%` 
                              }}
                            ></div>
                          </div>
                          <p className="text-xs text-secondary-500">
                            {formatCurrency(assistanceSummary.budgetCap - assistanceSummary.remainingBudget)} of {formatCurrency(assistanceSummary.budgetCap)} used
                          </p>
                        </div>
                      </CardContent>
                    </Card>
                  </div>
                )}

                {/* Recent Requests */}
                <Card>
                  <CardHeader>
                    <CardTitle>Recent Requests</CardTitle>
                  </CardHeader>
                  <CardContent>
                    {requestsLoading ? (
                      <div className="flex items-center justify-center py-8">
                        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
                      </div>
                    ) : assistanceRequests && assistanceRequests.length > 0 ? (
                      <div className="overflow-x-auto">
                        <table className="w-full">
                          <thead>
                            <tr className="border-b border-secondary-200">
                              <th className="text-left py-3 px-2 text-sm font-medium text-secondary-600">DATE</th>
                              <th className="text-left py-3 px-2 text-sm font-medium text-secondary-600">TYPE</th>
                              <th className="text-left py-3 px-2 text-sm font-medium text-secondary-600">AMOUNT</th>
                              <th className="text-left py-3 px-2 text-sm font-medium text-secondary-600">PAYEE</th>
                              <th className="text-left py-3 px-2 text-sm font-medium text-secondary-600">STATUS</th>
                              <th className="text-left py-3 px-2 text-sm font-medium text-secondary-600">ACTION</th>
                            </tr>
                          </thead>
                          <tbody>
                            {assistanceRequests.slice(0, 10).map((request) => (
                              <tr key={request.id} className="border-b border-secondary-100">
                                <td className="py-3 px-2 text-sm">
                                  {new Date(request.requestDate).toLocaleDateString()}
                                </td>
                                <td className="py-3 px-2 text-sm">
                                  {request.requestType.replace(/_/g, ' ')}
                                </td>
                                <td className="py-3 px-2 text-sm font-medium">
                                  {formatCurrency(request.amount)}
                                </td>
                                <td className="py-3 px-2 text-sm">
                                  {request.payeeName}
                                </td>
                                <td className="py-3 px-2">
                                  <Badge variant={getStatusBadgeVariant(request.status)}>
                                    {request.status.replace(/_/g, ' ')}
                                  </Badge>
                                </td>
                                <td className="py-3 px-2">
                                  <Button size="sm" variant="ghost">
                                    View
                                  </Button>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    ) : (
                      <div className="text-center py-8 text-secondary-500">
                        No assistance requests found
                      </div>
                    )}
                  </CardContent>
                </Card>

                {/* Budget Summary */}
                {assistanceSummary && (
                  <Card>
                    <CardHeader>
                      <CardTitle>Budget Summary</CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                        <div className="text-center p-4 bg-green-50 rounded-lg">
                          <p className="text-sm text-green-600">Total Approved</p>
                          <p className="text-xl font-bold text-green-800">
                            {formatCurrency(assistanceSummary.totalApproved)}
                          </p>
                        </div>
                        <div className="text-center p-4 bg-blue-50 rounded-lg">
                          <p className="text-sm text-blue-600">Total Disbursed</p>
                          <p className="text-xl font-bold text-blue-800">
                            {formatCurrency(assistanceSummary.totalDisbursed)}
                          </p>
                        </div>
                        <div className="text-center p-4 bg-amber-50 rounded-lg">
                          <p className="text-sm text-amber-600">Pending</p>
                          <p className="text-xl font-bold text-amber-800">
                            {formatCurrency(assistanceSummary.totalPending)}
                          </p>
                        </div>
                        <div className="text-center p-4 bg-primary-50 rounded-lg">
                          <p className="text-sm text-primary-600">Remaining Budget</p>
                          <p className="text-xl font-bold text-primary-800">
                            {formatCurrency(assistanceSummary.remainingBudget)}
                          </p>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                )}
              </>
            )}

            {/* New Request Form */}
            {showNewRequestForm && (
              <Card>
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <CardTitle>New Financial Assistance Request</CardTitle>
                    <Button 
                      variant="ghost" 
                      size="sm"
                      onClick={() => setShowNewRequestForm(false)}
                    >
                      <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                      </svg>
                    </Button>
                  </div>
                </CardHeader>
                <CardContent className="space-y-6">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-secondary-700 mb-2">
                        Request Type <span className="text-red-500">*</span>
                      </label>
                      <select
                        value={newRequest.requestType}
                        onChange={(e) => setNewRequest(prev => ({ ...prev, requestType: e.target.value as AssistanceRequestType }))}
                        className="w-full px-3 py-2 border border-secondary-300 rounded-md focus:ring-primary-500 focus:border-primary-500"
                      >
                        <option value={AssistanceRequestType.RENT}>Rent</option>
                        <option value={AssistanceRequestType.RENT_ARREARS}>Rent Arrears</option>
                        <option value={AssistanceRequestType.UTILITIES}>Utilities</option>
                        <option value={AssistanceRequestType.UTILITY_ARREARS}>Utility Arrears</option>
                        <option value={AssistanceRequestType.SECURITY_DEPOSIT}>Security Deposit</option>
                        <option value={AssistanceRequestType.APPLICATION_FEE}>Application Fee</option>
                        <option value={AssistanceRequestType.MOVING_COSTS}>Moving Costs</option>
                        <option value={AssistanceRequestType.OTHER}>Other</option>
                      </select>
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-secondary-700 mb-2">
                        Amount <span className="text-red-500">*</span>
                      </label>
                      <input
                        type="number"
                        value={newRequest.amount || ''}
                        onChange={(e) => setNewRequest(prev => ({ ...prev, amount: parseFloat(e.target.value) || 0 }))}
                        className="w-full px-3 py-2 border border-secondary-300 rounded-md focus:ring-primary-500 focus:border-primary-500"
                        placeholder="0.00"
                        step="0.01"
                        min="0"
                      />
                    </div>

                    {(newRequest.requestType === AssistanceRequestType.RENT || 
                      newRequest.requestType === AssistanceRequestType.UTILITIES) && (
                      <>
                        <div>
                          <label className="block text-sm font-medium text-secondary-700 mb-2">
                            Coverage Period Start
                          </label>
                          <input
                            type="date"
                            value={newRequest.coveragePeriodStart || ''}
                            onChange={(e) => setNewRequest(prev => ({ ...prev, coveragePeriodStart: e.target.value }))}
                            className="w-full px-3 py-2 border border-secondary-300 rounded-md focus:ring-primary-500 focus:border-primary-500"
                          />
                        </div>

                        <div>
                          <label className="block text-sm font-medium text-secondary-700 mb-2">
                            Coverage Period End
                          </label>
                          <input
                            type="date"
                            value={newRequest.coveragePeriodEnd || ''}
                            onChange={(e) => setNewRequest(prev => ({ ...prev, coveragePeriodEnd: e.target.value }))}
                            className="w-full px-3 py-2 border border-secondary-300 rounded-md focus:ring-primary-500 focus:border-primary-500"
                          />
                        </div>
                      </>
                    )}

                    <div>
                      <label className="block text-sm font-medium text-secondary-700 mb-2">
                        Payee/Vendor <span className="text-red-500">*</span>
                      </label>
                      <select
                        value={newRequest.payeeId}
                        onChange={(e) => {
                          const selectedPayee = payees.find(p => p.id === e.target.value);
                          setNewRequest(prev => ({ 
                            ...prev, 
                            payeeId: e.target.value,
                            payeeName: selectedPayee?.name || ''
                          }));
                        }}
                        className="w-full px-3 py-2 border border-secondary-300 rounded-md focus:ring-primary-500 focus:border-primary-500"
                      >
                        <option value="">Select Payee</option>
                        {payees.map((payee) => (
                          <option key={payee.id} value={payee.id}>
                            {payee.name} ({payee.type.replace(/_/g, ' ')})
                          </option>
                        ))}
                      </select>
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-secondary-700 mb-2">
                        Funding Source <span className="text-red-500">*</span>
                      </label>
                      <select
                        value={newRequest.fundingSourceId}
                        onChange={(e) => setNewRequest(prev => ({ ...prev, fundingSourceId: e.target.value }))}
                        className="w-full px-3 py-2 border border-secondary-300 rounded-md focus:ring-primary-500 focus:border-primary-500"
                      >
                        <option value="">Select Funding Source</option>
                        <option value="hud-rrh">HUD RRH</option>
                        <option value="vawa">VAWA</option>
                        <option value="state-funds">State Funding</option>
                        <option value="local-funds">Local Funding</option>
                      </select>
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-secondary-700 mb-2">
                      Justification / Notes <span className="text-red-500">*</span>
                    </label>
                    <textarea
                      value={newRequest.justification}
                      onChange={(e) => setNewRequest(prev => ({ ...prev, justification: e.target.value }))}
                      rows={4}
                      className="w-full px-3 py-2 border border-secondary-300 rounded-md focus:ring-primary-500 focus:border-primary-500"
                      placeholder="Example: May rent due; client still job-searching. Has interview scheduled for next week."
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-secondary-700 mb-2">
                      Supporting Documents
                    </label>
                    <div className="border border-dashed border-secondary-300 rounded-lg p-6 text-center">
                      <svg className="w-8 h-8 mx-auto mb-2 text-secondary-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                      </svg>
                      <p className="text-sm text-secondary-600 mb-4">
                        Upload lease agreement, utility bills, or other supporting documents
                      </p>
                      <div className="flex items-center justify-center space-x-4">
                        <Button size="sm" variant="outline">Upload Lease</Button>
                        <Button size="sm" variant="outline">Upload Utility Bill</Button>
                        <Button size="sm" variant="outline">Upload Other</Button>
                      </div>
                    </div>
                  </div>

                  {/* Approval Workflow Preview */}
                  <div>
                    <label className="block text-sm font-medium text-secondary-700 mb-2">
                      Approval Workflow
                    </label>
                    <div className="bg-secondary-50 rounded-lg p-4">
                      <div className="space-y-2">
                        <div className="flex items-center space-x-3">
                          <div className="w-6 h-6 bg-green-500 rounded-full flex items-center justify-center">
                            <svg className="w-3 h-3 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                            </svg>
                          </div>
                          <span className="text-sm">Level 1: Case Manager</span>
                        </div>
                        <div className="flex items-center space-x-3">
                          <div className="w-6 h-6 bg-secondary-300 rounded-full flex items-center justify-center">
                            <div className="w-2 h-2 bg-secondary-500 rounded-full"></div>
                          </div>
                          <span className="text-sm text-secondary-600">Level 2: Supervisor (Pending)</span>
                        </div>
                        <div className="flex items-center space-x-3">
                          <div className="w-6 h-6 bg-secondary-300 rounded-full flex items-center justify-center">
                            <div className="w-2 h-2 bg-secondary-500 rounded-full"></div>
                          </div>
                          <span className="text-sm text-secondary-600">Finance Review (Pending)</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center justify-between pt-6 border-t border-secondary-200">
                    <Button 
                      variant="outline" 
                      onClick={() => setShowNewRequestForm(false)}
                    >
                      Cancel
                    </Button>
                    <Button 
                      onClick={handleCreateRequest}
                      disabled={creatingRequest || !newRequest.requestType || !newRequest.amount || !newRequest.payeeId || !newRequest.fundingSourceId}
                    >
                      {creatingRequest ? 'Creating...' : 'Submit for Approval'}
                    </Button>
                  </div>
                </CardContent>
              </Card>
            )}

            {/* Ledger View */}
            {showLedgerView && (
              <Card>
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <CardTitle>Client Ledger - {fullName} (RRH)</CardTitle>
                    <div className="flex items-center space-x-3">
                      <Button size="sm" variant="outline">
                        <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-4-4m4 4l4-4m-7 14H5a2 2 0 01-2-2V9a2 2 0 012-2h6l2 2h6a2 2 0 012 2v11a2 2 0 01-2 2h-9z" />
                        </svg>
                        Export CSV
                      </Button>
                      <Button size="sm" variant="outline">
                        <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-4-4m4 4l4-4m-7 14H5a2 2 0 01-2-2V9a2 2 0 012-2h6l2 2h6a2 2 0 012 2v11a2 2 0 01-2 2h-9z" />
                        </svg>
                        Export PDF
                      </Button>
                      <Button size="sm" variant="outline">
                        HMIS Sync
                      </Button>
                      <Button 
                        variant="ghost" 
                        size="sm"
                        onClick={() => setShowLedgerView(false)}
                      >
                        <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                        </svg>
                      </Button>
                    </div>
                  </div>
                </CardHeader>
                <CardContent>
                  {ledgerLoading ? (
                    <div className="flex items-center justify-center py-8">
                      <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
                    </div>
                  ) : assistanceLedger && assistanceLedger.length > 0 ? (
                    <div className="overflow-x-auto">
                      <table className="w-full">
                        <thead>
                          <tr className="border-b border-secondary-200">
                            <th className="text-left py-3 px-2 text-sm font-medium text-secondary-600">DATE</th>
                            <th className="text-left py-3 px-2 text-sm font-medium text-secondary-600">TYPE</th>
                            <th className="text-left py-3 px-2 text-sm font-medium text-secondary-600">DESCRIPTION</th>
                            <th className="text-left py-3 px-2 text-sm font-medium text-secondary-600">AMOUNT</th>
                            <th className="text-left py-3 px-2 text-sm font-medium text-secondary-600">FUNDING</th>
                            <th className="text-left py-3 px-2 text-sm font-medium text-secondary-600">PAYEE</th>
                            <th className="text-left py-3 px-2 text-sm font-medium text-secondary-600">STATUS</th>
                          </tr>
                        </thead>
                        <tbody>
                          {assistanceLedger.map((entry) => (
                            <tr key={entry.id} className="border-b border-secondary-100">
                              <td className="py-3 px-2 text-sm">
                                {new Date(entry.date).toLocaleDateString()}
                              </td>
                              <td className="py-3 px-2 text-sm">
                                {entry.type.replace(/_/g, ' ')}
                              </td>
                              <td className="py-3 px-2 text-sm">
                                {entry.description}
                              </td>
                              <td className="py-3 px-2 text-sm font-medium">
                                {formatCurrency(entry.amount)}
                              </td>
                              <td className="py-3 px-2 text-sm">
                                {entry.fundingSource}
                              </td>
                              <td className="py-3 px-2 text-sm">
                                {entry.payeeName}
                              </td>
                              <td className="py-3 px-2">
                                <Badge variant={getStatusBadgeVariant(entry.status as AssistanceRequestStatus)}>
                                  {entry.status.replace(/_/g, ' ')}
                                </Badge>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  ) : (
                    <div className="text-center py-8 text-secondary-500">
                      No ledger entries found
                    </div>
                  )}
                </CardContent>
              </Card>
            )}
          </TabsContent>
          
          <TabsContent value="documents">
            <div className="text-center py-12 text-secondary-500">
              Documents would be displayed here
            </div>
          </TabsContent>
        </Tabs>
      </div>

      {/* Actions Footer */}
      <div className="bg-white border-t border-secondary-200 px-6 py-4 mt-6 flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <Button variant="outline">Save Draft</Button>
          <Button>Submit Update</Button>
        </div>
        <Button variant="destructive" size="sm">
          <svg className="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-.834-1.964-.834-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
          </svg>
          Flag Incident
        </Button>
      </div>
    </div>
  );
}

export default function ClientPage() {
  const router = useRouter();
  const { id } = router.query;
  const { client, loading, error } = useClient(id as string);

  if (loading) {
    return (
      <ProtectedRoute>
        <AppLayout title="Loading...">
          <div className="flex items-center justify-center min-h-96">
            <div className="text-center">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600 mx-auto mb-4"></div>
              <p className="text-secondary-600">Loading client...</p>
            </div>
          </div>
        </AppLayout>
      </ProtectedRoute>
    );
  }

  if (error || !client) {
    return (
      <ProtectedRoute>
        <AppLayout 
          title="Client Not Found"
          breadcrumbs={[
            { label: 'Dashboard', href: '/dashboard' },
            { label: 'Clients', href: '/clients' },
            { label: 'Not Found' }
          ]}
        >
          <div className="p-6">
            <div className="text-center py-12">
              <h2 className="text-2xl font-semibold text-secondary-900 mb-2">Client Not Found</h2>
              <p className="text-secondary-600">Unable to load client information.</p>
            </div>
          </div>
        </AppLayout>
      </ProtectedRoute>
    );
  }

  const clientName = client.name 
    ? `${client.name.given?.join(' ') || ''} ${client.name.family || ''}`.trim()
    : 'Unknown Client';

  return (
    <ProtectedRoute>
      <AppLayout 
        title={`${clientName}`}
        breadcrumbs={[
          { label: 'Dashboard', href: '/dashboard' },
          { label: 'Clients', href: '/clients' },
          { label: clientName }
        ]}
        fullWidth
      >
        <ClientContent client={client} />
      </AppLayout>
    </ProtectedRoute>
  );
}
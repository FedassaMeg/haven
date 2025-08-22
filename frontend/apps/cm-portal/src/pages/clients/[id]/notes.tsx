import React, { useState } from 'react';
import { useRouter } from 'next/router';
import { ProtectedRoute, useCurrentUser } from '@haven/auth';
import { Card, CardHeader, CardTitle, CardContent } from '@haven/ui';
import { RestrictedNote, ConfidentialityGuardrails } from '@haven/api-client';
import AppLayout from '../../../components/AppLayout';
import ConfidentialityBanner from '../../../components/ConfidentialityBanner';
import RestrictedNotesPanel from '../../../components/RestrictedNotesPanel';

// Mock data for demonstration
const mockGuardrails: ConfidentialityGuardrails = {
  clientId: 'client-123',
  clientName: 'Jane Smith',
  isSafeAtHome: true,
  isComparableDbOnly: false,
  hasConfidentialLocation: true,
  hasRestrictedData: true,
  dataSystem: 'HMIS',
  visibilityLevel: 'CONFIDENTIAL',
  lastUpdated: '2024-08-20T10:30:00Z',
  bannerWarningText: '⚠️ CONFIDENTIAL CLIENT: Safe at Home participant. Confidential location protection. Contains privileged/restricted information.',
  bannerSeverity: 'CRITICAL'
};

const mockNotes: RestrictedNote[] = [
  {
    noteId: 'note-1',
    clientId: 'client-123',
    clientName: 'Jane Smith',
    caseId: 'case-456',
    caseNumber: 'DV-2024-001',
    noteType: 'STANDARD',
    content: 'Initial intake completed. Client presented with housing needs and safety concerns. Developed safety plan and discussed available resources.',
    authorId: 'user-1',
    authorName: 'Sarah Wilson',
    createdAt: '2024-08-20T14:30:00Z',
    lastModified: '2024-08-20T14:30:00Z',
    visibilityScope: 'CASE_TEAM',
    isSealed: false,
    requiresSpecialHandling: false
  },
  {
    noteId: 'note-2',
    clientId: 'client-123',
    clientName: 'Jane Smith',
    caseId: 'case-456',
    caseNumber: 'DV-2024-001',
    noteType: 'PRIVILEGED_COUNSELING',
    content: 'Therapeutic session discussing trauma history and coping strategies. Client shared details about abuse patterns and discussed therapeutic goals.',
    authorId: 'user-2',
    authorName: 'Dr. Emily Johnson',
    createdAt: '2024-08-19T11:00:00Z',
    lastModified: '2024-08-19T11:00:00Z',
    visibilityScope: 'CLINICAL_ONLY',
    isSealed: false,
    requiresSpecialHandling: true,
    visibilityWarning: '🔐 PRIVILEGED COUNSELING - Confidential therapeutic communication'
  },
  {
    noteId: 'note-3',
    clientId: 'client-123',
    clientName: 'Jane Smith',
    caseId: 'case-456',
    caseNumber: 'DV-2024-001',
    noteType: 'ATTORNEY_CLIENT',
    content: 'Legal consultation regarding protective order options and custody concerns. Discussed evidence gathering and court procedures.',
    authorId: 'user-3',
    authorName: 'Attorney Lisa Rodriguez',
    createdAt: '2024-08-18T16:45:00Z',
    lastModified: '2024-08-18T16:45:00Z',
    visibilityScope: 'ATTORNEY_CLIENT',
    isSealed: false,
    requiresSpecialHandling: true,
    visibilityWarning: '⚖️ ATTORNEY-CLIENT PRIVILEGED - Confidential legal communication'
  },
  {
    noteId: 'note-4',
    clientId: 'client-123',
    clientName: 'Jane Smith',
    caseId: 'case-456',
    caseNumber: 'DV-2024-001',
    noteType: 'SAFETY_PLAN',
    content: 'Updated safety plan after incident on 8/15. Reviewed escape routes, emergency contacts, and safety items. Client relocated to secure housing.',
    authorId: 'user-4',
    authorName: 'Maria Garcia',
    createdAt: '2024-08-17T09:15:00Z',
    lastModified: '2024-08-17T09:15:00Z',
    visibilityScope: 'SAFETY_TEAM',
    isSealed: true,
    sealReason: 'Contains sensitive location information that could compromise client safety',
    sealedAt: '2024-08-17T10:00:00Z',
    sealedBy: 'user-4',
    requiresSpecialHandling: true
  },
  {
    noteId: 'note-5',
    clientId: 'client-123',
    clientName: 'Jane Smith',
    caseId: 'case-456',
    caseNumber: 'DV-2024-001',
    noteType: 'INTERNAL_ADMIN',
    content: 'Administrative note: Client has requested all communications go through secure channels only. Do not use regular mail or email.',
    authorId: 'user-1',
    authorName: 'Sarah Wilson',
    createdAt: '2024-08-16T13:20:00Z',
    lastModified: '2024-08-16T13:20:00Z',
    visibilityScope: 'ADMIN_ONLY',
    isSealed: false,
    requiresSpecialHandling: false
  }
];

function ClientNotesContent() {
  const router = useRouter();
  const { user } = useCurrentUser();
  const { id: clientId } = router.query;
  const [notes, setNotes] = useState<RestrictedNote[]>(mockNotes);

  const handleCreateNote = async (noteType: RestrictedNote['noteType'], content: string) => {
    const newNote: RestrictedNote = {
      noteId: `note-${Date.now()}`,
      clientId: clientId as string,
      clientName: mockGuardrails.clientName,
      caseId: 'case-456',
      caseNumber: 'DV-2024-001',
      noteType,
      content,
      authorId: user?.id || 'current-user',
      authorName: user?.fullName || 'Current User',
      createdAt: new Date().toISOString(),
      lastModified: new Date().toISOString(),
      visibilityScope: getDefaultVisibilityScope(noteType),
      isSealed: false,
      requiresSpecialHandling: ['PRIVILEGED_COUNSELING', 'ATTORNEY_CLIENT'].includes(noteType),
      visibilityWarning: getVisibilityWarning(noteType)
    };

    setNotes(prev => [newNote, ...prev]);
  };

  const handleSealNote = async (noteId: string, reason: string) => {
    setNotes(prev => prev.map(note => 
      note.noteId === noteId 
        ? {
            ...note,
            isSealed: true,
            sealReason: reason,
            sealedAt: new Date().toISOString(),
            sealedBy: user?.id || 'current-user'
          }
        : note
    ));
  };

  const handleUnsealNote = async (noteId: string) => {
    setNotes(prev => prev.map(note => 
      note.noteId === noteId 
        ? {
            ...note,
            isSealed: false,
            sealReason: undefined,
            sealedAt: undefined,
            sealedBy: undefined
          }
        : note
    ));
  };

  const getDefaultVisibilityScope = (noteType: RestrictedNote['noteType']): RestrictedNote['visibilityScope'] => {
    switch (noteType) {
      case 'PRIVILEGED_COUNSELING':
      case 'COUNSELING':
      case 'THERAPEUTIC':
        return 'CLINICAL_ONLY';
      case 'ATTORNEY_CLIENT':
      case 'LEGAL_ADVOCACY':
        return 'ATTORNEY_CLIENT';
      case 'SAFETY_PLAN':
        return 'SAFETY_TEAM';
      case 'MEDICAL':
        return 'MEDICAL_TEAM';
      case 'INTERNAL_ADMIN':
        return 'ADMIN_ONLY';
      default:
        return 'CASE_TEAM';
    }
  };

  const getVisibilityWarning = (noteType: RestrictedNote['noteType']): string | undefined => {
    switch (noteType) {
      case 'PRIVILEGED_COUNSELING':
        return '🔐 PRIVILEGED COUNSELING - Confidential therapeutic communication';
      case 'ATTORNEY_CLIENT':
        return '⚖️ ATTORNEY-CLIENT PRIVILEGED - Confidential legal communication';
      default:
        return undefined;
    }
  };

  return (
    <div className="space-y-6">
      {/* Confidentiality Banner */}
      <ConfidentialityBanner guardrails={mockGuardrails} />

      {/* Client Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">
            Case Notes - {mockGuardrails.clientName}
          </h1>
          <p className="text-slate-600">Secure case documentation with visibility controls</p>
        </div>
        <div className="flex items-center space-x-2">
          <div className="text-right text-sm">
            <div className="text-slate-600">Case #: DV-2024-001</div>
            <div className="text-slate-500">Client ID: {clientId}</div>
          </div>
        </div>
      </div>

      {/* Notes Panel */}
      <RestrictedNotesPanel
        notes={notes}
        onCreateNote={handleCreateNote}
        onSealNote={handleSealNote}
        onUnsealNote={handleUnsealNote}
        loading={false}
      />

      {/* Compliance Information */}
      <Card>
        <CardHeader>
          <CardTitle>Privacy & Compliance Information</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <h4 className="font-medium text-slate-700 mb-2">Data Sharing Restrictions</h4>
              <div className="space-y-2 text-sm">
                {mockGuardrails.isSafeAtHome && (
                  <div className="flex items-center space-x-2">
                    <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-purple-100 text-purple-800">
                      🏠 Safe at Home
                    </span>
                    <span className="text-slate-600">Address confidentiality program participant</span>
                  </div>
                )}
                {mockGuardrails.isComparableDbOnly && (
                  <div className="flex items-center space-x-2">
                    <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-indigo-100 text-indigo-800">
                      📊 Comparable DB Only
                    </span>
                    <span className="text-slate-600">No HMIS data sharing permitted</span>
                  </div>
                )}
                {mockGuardrails.hasConfidentialLocation && (
                  <div className="flex items-center space-x-2">
                    <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-pink-100 text-pink-800">
                      📍 Confidential Location
                    </span>
                    <span className="text-slate-600">Location information is protected</span>
                  </div>
                )}
              </div>
            </div>
            
            <div>
              <h4 className="font-medium text-slate-700 mb-2">Note Type Permissions</h4>
              <div className="space-y-1 text-sm text-slate-600">
                <div>• Standard notes: Visible to case team</div>
                <div>• Clinical notes: Licensed clinicians only</div>
                <div>• Legal notes: Legal advocates and attorneys</div>
                <div>• Privileged notes: Author and authorized personnel only</div>
                <div>• Sealed notes: Restricted access by seal authority</div>
              </div>
            </div>
          </div>
          
          <div className="mt-4 p-3 bg-blue-50 border border-blue-200 rounded-lg">
            <div className="flex items-start space-x-2">
              <svg className="w-5 h-5 text-blue-600 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
              </svg>
              <div className="text-sm">
                <div className="font-medium text-blue-800">Privacy Notice</div>
                <div className="text-blue-700">
                  All case notes are subject to confidentiality protections under applicable federal and state laws. 
                  Access is restricted based on professional role and need-to-know basis.
                </div>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

export default function ClientNotesPage() {
  return (
    <ProtectedRoute requiredRoles={['admin', 'supervisor', 'case_manager', 'clinician', 'legal_advocate']}>
      <AppLayout 
        title="Client Notes" 
        breadcrumbs={[
          { label: 'Dashboard', href: '/dashboard' },
          { label: 'Clients', href: '/clients' },
          { label: 'Client Details', href: '/clients/[id]' },
          { label: 'Notes' }
        ]}
      >
        <div className="p-6">
          <ClientNotesContent />
        </div>
      </AppLayout>
    </ProtectedRoute>
  );
}
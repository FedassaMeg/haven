import React, { useState } from 'react';
import { Card, CardHeader, CardTitle, CardContent, Badge, Button, Alert, AlertDescription } from '@haven/ui';
import { RestrictedNote } from '@haven/api-client';
import { useCurrentUser } from '@haven/auth';

interface RestrictedNotesPanelProps {
  notes: RestrictedNote[];
  onCreateNote?: (noteType: RestrictedNote['noteType'], content: string) => void;
  onSealNote?: (noteId: string, reason: string) => void;
  onUnsealNote?: (noteId: string) => void;
  loading?: boolean;
  className?: string;
}

export const RestrictedNotesPanel: React.FC<RestrictedNotesPanelProps> = ({
  notes,
  onCreateNote,
  onSealNote,
  onUnsealNote,
  loading = false,
  className = ''
}) => {
  const { user } = useCurrentUser();
  const [selectedNoteType, setSelectedNoteType] = useState<RestrictedNote['noteType']>('STANDARD');
  const [noteContent, setNoteContent] = useState('');
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [filterType, setFilterType] = useState<RestrictedNote['noteType'] | 'ALL'>('ALL');
  const [sealingNoteId, setSealingNoteId] = useState<string | null>(null);
  const [sealReason, setSealReason] = useState('');

  const noteTypeLabels = {
    STANDARD: 'Standard Case Note',
    COUNSELING: 'Counseling Session',
    PRIVILEGED_COUNSELING: 'Privileged Counseling',
    LEGAL_ADVOCACY: 'Legal Advocacy',
    ATTORNEY_CLIENT: 'Attorney-Client Privileged',
    SAFETY_PLAN: 'Safety Planning',
    MEDICAL: 'Medical Information',
    THERAPEUTIC: 'Therapeutic Session',
    INTERNAL_ADMIN: 'Internal Administrative'
  };

  const visibilityScopeLabels = {
    PUBLIC: 'Public',
    CASE_TEAM: 'Case Team',
    CLINICAL_ONLY: 'Clinical Staff Only',
    LEGAL_TEAM: 'Legal Team Only',
    SAFETY_TEAM: 'Safety Team Only',
    MEDICAL_TEAM: 'Medical Staff Only',
    ADMIN_ONLY: 'Administrators Only',
    AUTHOR_ONLY: 'Author Only',
    ATTORNEY_CLIENT: 'Attorney-Client Privileged',
    CUSTOM: 'Custom Access'
  };

  const getNoteTypeColor = (noteType: RestrictedNote['noteType']) => {
    switch (noteType) {
      case 'PRIVILEGED_COUNSELING':
      case 'ATTORNEY_CLIENT':
        return 'bg-red-100 text-red-800 border-red-200';
      case 'LEGAL_ADVOCACY':
        return 'bg-purple-100 text-purple-800 border-purple-200';
      case 'COUNSELING':
      case 'THERAPEUTIC':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'MEDICAL':
        return 'bg-green-100 text-green-800 border-green-200';
      case 'SAFETY_PLAN':
        return 'bg-orange-100 text-orange-800 border-orange-200';
      case 'INTERNAL_ADMIN':
        return 'bg-gray-100 text-gray-800 border-gray-200';
      default:
        return 'bg-slate-100 text-slate-800 border-slate-200';
    }
  };

  const filteredNotes = filterType === 'ALL' 
    ? notes 
    : notes.filter(note => note.noteType === filterType);

  const handleCreateNote = async () => {
    if (!noteContent.trim() || !onCreateNote) return;
    
    await onCreateNote(selectedNoteType, noteContent);
    setNoteContent('');
    setShowCreateForm(false);
  };

  const handleSealNote = async (noteId: string) => {
    if (!sealReason.trim() || !onSealNote) return;
    
    await onSealNote(noteId, sealReason);
    setSealingNoteId(null);
    setSealReason('');
  };

  const canViewNote = (note: RestrictedNote) => {
    // This would normally check against user roles and permissions
    // For demo purposes, we'll assume the user can view most notes
    if (note.isSealed && note.sealedBy !== user?.id) {
      return false;
    }
    
    if (note.visibilityScope === 'AUTHOR_ONLY' && note.authorId !== user?.id) {
      return false;
    }
    
    return true;
  };

  const canEditNote = (note: RestrictedNote) => {
    return note.authorId === user?.id && !note.isSealed;
  };

  return (
    <div className={`space-y-4 ${className}`}>
      {/* Header and Controls */}
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-lg font-medium text-slate-900">Case Notes</h3>
          <p className="text-sm text-slate-600">Secure case documentation with visibility controls</p>
        </div>
        <div className="flex items-center space-x-2">
          <select
            value={filterType}
            onChange={(e) => setFilterType(e.target.value as RestrictedNote['noteType'] | 'ALL')}
            className="border border-slate-300 rounded px-3 py-1 text-sm"
          >
            <option value="ALL">All Types</option>
            {Object.entries(noteTypeLabels).map(([value, label]) => (
              <option key={value} value={value}>{label}</option>
            ))}
          </select>
          <Button
            size="sm"
            onClick={() => setShowCreateForm(!showCreateForm)}
          >
            Add Note
          </Button>
        </div>
      </div>

      {/* Create Note Form */}
      {showCreateForm && (
        <Card>
          <CardHeader>
            <CardTitle>Create New Note</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Note Type
              </label>
              <select
                value={selectedNoteType}
                onChange={(e) => setSelectedNoteType(e.target.value as RestrictedNote['noteType'])}
                className="w-full border border-slate-300 rounded px-3 py-2"
              >
                {Object.entries(noteTypeLabels).map(([value, label]) => (
                  <option key={value} value={value}>{label}</option>
                ))}
              </select>
            </div>
            
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Content
              </label>
              <textarea
                value={noteContent}
                onChange={(e) => setNoteContent(e.target.value)}
                rows={4}
                className="w-full border border-slate-300 rounded px-3 py-2"
                placeholder="Enter note content..."
              />
            </div>

            {(selectedNoteType === 'PRIVILEGED_COUNSELING' || selectedNoteType === 'ATTORNEY_CLIENT') && (
              <Alert>
                <AlertDescription>
                  ⚠️ This note type is protected by confidentiality privileges and will have restricted access.
                </AlertDescription>
              </Alert>
            )}

            <div className="flex justify-end space-x-2">
              <Button variant="outline" onClick={() => setShowCreateForm(false)}>
                Cancel
              </Button>
              <Button onClick={handleCreateNote} disabled={!noteContent.trim()}>
                Create Note
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Notes List */}
      <div className="space-y-3">
        {loading ? (
          <div className="text-center py-8">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600 mx-auto"></div>
            <p className="text-slate-600 mt-2">Loading notes...</p>
          </div>
        ) : filteredNotes.length === 0 ? (
          <Card>
            <CardContent className="text-center py-8">
              <p className="text-slate-600">No notes found</p>
            </CardContent>
          </Card>
        ) : (
          filteredNotes.map((note) => (
            <Card key={note.noteId} className={note.requiresSpecialHandling ? 'border-red-200' : ''}>
              <CardContent className="p-4">
                {/* Note Warning */}
                {note.visibilityWarning && (
                  <Alert className="mb-3">
                    <AlertDescription>{note.visibilityWarning}</AlertDescription>
                  </Alert>
                )}

                {/* Note Header */}
                <div className="flex items-start justify-between mb-3">
                  <div className="flex items-center space-x-2">
                    <Badge className={getNoteTypeColor(note.noteType)}>
                      {noteTypeLabels[note.noteType]}
                    </Badge>
                    <Badge variant="outline">
                      {visibilityScopeLabels[note.visibilityScope]}
                    </Badge>
                    {note.isSealed && (
                      <Badge className="bg-gray-900 text-white">
                        🚫 SEALED
                      </Badge>
                    )}
                  </div>
                  <div className="text-sm text-slate-500">
                    {new Date(note.createdAt).toLocaleDateString()} at {new Date(note.createdAt).toLocaleTimeString()}
                  </div>
                </div>

                {/* Note Content */}
                {canViewNote(note) ? (
                  <div className="mb-3">
                    <div className="prose prose-sm max-w-none">
                      {note.content}
                    </div>
                  </div>
                ) : (
                  <div className="mb-3 p-3 bg-gray-100 rounded">
                    <p className="text-gray-600 text-sm">
                      🔒 This note is sealed or you don't have permission to view its contents.
                    </p>
                    {note.sealReason && (
                      <p className="text-gray-500 text-xs mt-1">
                        Reason: {note.sealReason}
                      </p>
                    )}
                  </div>
                )}

                {/* Note Footer */}
                <div className="flex items-center justify-between text-sm text-slate-600">
                  <div>
                    By {note.authorName}
                    {note.lastModified !== note.createdAt && (
                      <span> • Modified {new Date(note.lastModified).toLocaleDateString()}</span>
                    )}
                  </div>
                  <div className="flex items-center space-x-2">
                    {canEditNote(note) && !note.isSealed && (
                      <Button size="xs" variant="ghost">
                        Edit
                      </Button>
                    )}
                    {canEditNote(note) && !note.isSealed && onSealNote && (
                      <Button 
                        size="xs" 
                        variant="ghost"
                        onClick={() => setSealingNoteId(note.noteId)}
                      >
                        Seal
                      </Button>
                    )}
                    {note.isSealed && note.sealedBy === user?.id && onUnsealNote && (
                      <Button 
                        size="xs" 
                        variant="ghost"
                        onClick={() => onUnsealNote(note.noteId)}
                      >
                        Unseal
                      </Button>
                    )}
                  </div>
                </div>
              </CardContent>
            </Card>
          ))
        )}
      </div>

      {/* Seal Note Modal */}
      {sealingNoteId && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <Card className="w-full max-w-md">
            <CardHeader>
              <CardTitle>Seal Note</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1">
                  Reason for sealing
                </label>
                <textarea
                  value={sealReason}
                  onChange={(e) => setSealReason(e.target.value)}
                  rows={3}
                  className="w-full border border-slate-300 rounded px-3 py-2"
                  placeholder="Enter reason for sealing this note..."
                />
              </div>
              <div className="flex justify-end space-x-2">
                <Button variant="outline" onClick={() => {
                  setSealingNoteId(null);
                  setSealReason('');
                }}>
                  Cancel
                </Button>
                <Button 
                  onClick={() => handleSealNote(sealingNoteId)}
                  disabled={!sealReason.trim()}
                >
                  Seal Note
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
};

export default RestrictedNotesPanel;
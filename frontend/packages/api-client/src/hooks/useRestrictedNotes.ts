import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ApiClient } from '../client';
import type { 
  RestrictedNote, 
  CreateRestrictedNoteRequest, 
  UpdateRestrictedNoteRequest,
  SealNoteRequest,
  UnsealNoteRequest 
} from '../types/restrictedNotes';

export function useRestrictedNotes(clientId?: string) {
  return useQuery({
    queryKey: ['restricted-notes', 'client', clientId],
    queryFn: () => ApiClient.restrictedNotes.getClientNotes(clientId!),
    enabled: !!clientId,
  });
}

export function useAccessibleNotes() {
  return useQuery({
    queryKey: ['restricted-notes', 'accessible'],
    queryFn: () => ApiClient.restrictedNotes.getAccessibleNotes(),
  });
}

export function useRestrictedNote(noteId?: string) {
  return useQuery({
    queryKey: ['restricted-notes', noteId],
    queryFn: () => ApiClient.restrictedNotes.getNote(noteId!),
    enabled: !!noteId,
  });
}

export function useCreateRestrictedNote() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (data: CreateRestrictedNoteRequest) => 
      ApiClient.restrictedNotes.createNote(data),
    onSuccess: (response, variables) => {
      // Invalidate related queries
      queryClient.invalidateQueries({ 
        queryKey: ['restricted-notes', 'client', variables.clientId] 
      });
      queryClient.invalidateQueries({ 
        queryKey: ['restricted-notes', 'accessible'] 
      });
    },
  });
}

export function useUpdateRestrictedNote() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ noteId, data }: { noteId: string; data: UpdateRestrictedNoteRequest }) =>
      ApiClient.restrictedNotes.updateNote(noteId, data),
    onSuccess: (_, variables) => {
      // Invalidate related queries
      queryClient.invalidateQueries({ 
        queryKey: ['restricted-notes', variables.noteId] 
      });
      queryClient.invalidateQueries({ 
        queryKey: ['restricted-notes', 'accessible'] 
      });
    },
  });
}

export function useSealNote() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ noteId, data }: { noteId: string; data: SealNoteRequest }) =>
      ApiClient.restrictedNotes.sealNote(noteId, data),
    onSuccess: (_, variables) => {
      // Invalidate related queries
      queryClient.invalidateQueries({ 
        queryKey: ['restricted-notes', variables.noteId] 
      });
      queryClient.invalidateQueries({ 
        queryKey: ['restricted-notes', 'accessible'] 
      });
    },
  });
}

export function useUnsealNote() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ noteId, data }: { noteId: string; data: UnsealNoteRequest }) =>
      ApiClient.restrictedNotes.unsealNote(noteId, data),
    onSuccess: (_, variables) => {
      // Invalidate related queries
      queryClient.invalidateQueries({ 
        queryKey: ['restricted-notes', variables.noteId] 
      });
      queryClient.invalidateQueries({ 
        queryKey: ['restricted-notes', 'accessible'] 
      });
    },
  });
}

export function useNoteAuditTrail(noteId?: string) {
  return useQuery({
    queryKey: ['restricted-notes', noteId, 'audit'],
    queryFn: () => ApiClient.restrictedNotes.getAuditTrail(noteId!),
    enabled: !!noteId,
  });
}

export function useComplianceReport(noteId?: string) {
  return useQuery({
    queryKey: ['restricted-notes', noteId, 'compliance'],
    queryFn: () => ApiClient.restrictedNotes.getComplianceReport(noteId!),
    enabled: !!noteId,
  });
}

export function useAccessLog(noteId?: string) {
  return useQuery({
    queryKey: ['restricted-notes', noteId, 'access-log'],
    queryFn: () => ApiClient.restrictedNotes.getAccessLog(noteId!),
    enabled: !!noteId,
  });
}
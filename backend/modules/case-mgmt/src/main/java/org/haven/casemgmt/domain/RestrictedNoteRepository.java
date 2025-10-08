package org.haven.casemgmt.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RestrictedNoteRepository {
    
    Optional<RestrictedNote> findById(RestrictedNoteId noteId);
    
    void save(RestrictedNote aggregate);
    
    void delete(RestrictedNote aggregate);
    
    RestrictedNoteId nextId();
    
    List<RestrictedNote> findByClientId(UUID clientId);
    
    List<RestrictedNote> findByCaseId(UUID caseId);
    
    List<RestrictedNote> findByAuthorId(UUID authorId);
    
    List<RestrictedNote> findByNoteType(RestrictedNote.NoteType noteType);
    
    List<RestrictedNote> findByVisibilityScope(RestrictedNote.VisibilityScope scope);
    
    List<RestrictedNote> findSealedNotes();
    
    List<RestrictedNote> findExpiredTemporarySeals(Instant asOf);
    
    List<RestrictedNote> findAccessibleToUser(UUID userId, List<String> userRoles);
    
    List<RestrictedNote> findByClientIdAccessibleToUser(UUID clientId, UUID userId, List<String> userRoles);
    
    List<RestrictedNote> findByCaseIdAccessibleToUser(UUID caseId, UUID userId, List<String> userRoles);
    
    boolean hasValidAccess(UUID noteId, UUID userId, List<String> userRoles);
}
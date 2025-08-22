package org.haven.readmodels.infrastructure;

import org.haven.readmodels.domain.RestrictedNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RestrictedNoteRepository {
    
    Optional<RestrictedNote> findById(UUID noteId);
    
    void save(RestrictedNote note);
    
    void delete(UUID noteId);
    
    Page<RestrictedNote> findByClientId(UUID clientId, Pageable pageable);
    
    Page<RestrictedNote> findByCaseId(UUID caseId, Pageable pageable);
    
    Page<RestrictedNote> findByAuthorId(UUID authorId, Pageable pageable);
    
    List<RestrictedNote> findByNoteType(RestrictedNote.NoteType noteType);
    
    List<RestrictedNote> findByVisibilityScope(RestrictedNote.VisibilityScope scope);
    
    Page<RestrictedNote> findByClientIdAndNoteType(UUID clientId, RestrictedNote.NoteType noteType, Pageable pageable);
    
    Page<RestrictedNote> findByCaseIdAndNoteType(UUID caseId, RestrictedNote.NoteType noteType, Pageable pageable);
    
    Page<RestrictedNote> findAccessibleToUser(UUID userId, List<String> userRoles, Pageable pageable);
    
    Page<RestrictedNote> findByClientIdAccessibleToUser(UUID clientId, UUID userId, List<String> userRoles, Pageable pageable);
    
    List<RestrictedNote> findSealedNotes();
    
    List<RestrictedNote> findByNoteTypes(List<RestrictedNote.NoteType> types);
    
    List<RestrictedNote> findByVisibilityScopes(List<RestrictedNote.VisibilityScope> scopes);
    
    List<RestrictedNote> findByCreatedAtBetween(Instant start, Instant end);
    
    Long countByClientId(UUID clientId);
    
    Long countByAuthorId(UUID authorId);
    
    Long countByNoteType(RestrictedNote.NoteType noteType);
    
    Long countSealedNotes();
}
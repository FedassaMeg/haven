package org.haven.readmodels.infrastructure;

import org.haven.readmodels.domain.RestrictedNote;
import org.haven.shared.security.AccessContext;
import org.haven.shared.security.ConfidentialityPolicyService;
import org.haven.shared.security.PolicyDecision;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class RestrictedNoteRepositoryImpl implements RestrictedNoteRepository {
    
    private final JpaRestrictedNoteRepository jpaRepository;
    private final ConfidentialityPolicyService policyService;

    @Autowired
    public RestrictedNoteRepositoryImpl(JpaRestrictedNoteRepository jpaRepository,
                                       ConfidentialityPolicyService policyService) {
        this.jpaRepository = jpaRepository;
        this.policyService = policyService;
    }
    
    @Override
    public Optional<RestrictedNote> findById(UUID noteId) {
        return jpaRepository.findById(noteId)
                .map(JpaRestrictedNoteEntity::toDomain);
    }
    
    @Override
    public void save(RestrictedNote note) {
        Optional<JpaRestrictedNoteEntity> existing = jpaRepository.findById(note.getNoteId());
        
        if (existing.isPresent()) {
            existing.get().updateFrom(note);
            jpaRepository.save(existing.get());
        } else {
            JpaRestrictedNoteEntity entity = new JpaRestrictedNoteEntity(note);
            jpaRepository.save(entity);
        }
    }
    
    @Override
    public void delete(UUID noteId) {
        jpaRepository.deleteById(noteId);
    }
    
    @Override
    public Page<RestrictedNote> findByClientId(UUID clientId, Pageable pageable) {
        Page<JpaRestrictedNoteEntity> entityPage = jpaRepository.findByClientId(clientId, pageable);
        List<RestrictedNote> notes = entityPage.getContent().stream()
                .map(JpaRestrictedNoteEntity::toDomain)
                .collect(Collectors.toList());
        return new PageImpl<>(notes, pageable, entityPage.getTotalElements());
    }
    
    @Override
    public Page<RestrictedNote> findByCaseId(UUID caseId, Pageable pageable) {
        Page<JpaRestrictedNoteEntity> entityPage = jpaRepository.findByCaseId(caseId, pageable);
        List<RestrictedNote> notes = entityPage.getContent().stream()
                .map(JpaRestrictedNoteEntity::toDomain)
                .collect(Collectors.toList());
        return new PageImpl<>(notes, pageable, entityPage.getTotalElements());
    }
    
    @Override
    public Page<RestrictedNote> findByAuthorId(UUID authorId, Pageable pageable) {
        Page<JpaRestrictedNoteEntity> entityPage = jpaRepository.findByAuthorId(authorId, pageable);
        List<RestrictedNote> notes = entityPage.getContent().stream()
                .map(JpaRestrictedNoteEntity::toDomain)
                .collect(Collectors.toList());
        return new PageImpl<>(notes, pageable, entityPage.getTotalElements());
    }
    
    @Override
    public List<RestrictedNote> findByNoteType(RestrictedNote.NoteType noteType) {
        return jpaRepository.findByNoteType(noteType).stream()
                .map(JpaRestrictedNoteEntity::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<RestrictedNote> findByVisibilityScope(RestrictedNote.VisibilityScope scope) {
        return jpaRepository.findByVisibilityScope(scope).stream()
                .map(JpaRestrictedNoteEntity::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public Page<RestrictedNote> findByClientIdAndNoteType(UUID clientId, RestrictedNote.NoteType noteType, Pageable pageable) {
        Page<JpaRestrictedNoteEntity> entityPage = jpaRepository.findByClientIdAndNoteType(clientId, noteType, pageable);
        List<RestrictedNote> notes = entityPage.getContent().stream()
                .map(JpaRestrictedNoteEntity::toDomain)
                .collect(Collectors.toList());
        return new PageImpl<>(notes, pageable, entityPage.getTotalElements());
    }
    
    @Override
    public Page<RestrictedNote> findByCaseIdAndNoteType(UUID caseId, RestrictedNote.NoteType noteType, Pageable pageable) {
        Page<JpaRestrictedNoteEntity> entityPage = jpaRepository.findByCaseIdAndNoteType(caseId, noteType, pageable);
        List<RestrictedNote> notes = entityPage.getContent().stream()
                .map(JpaRestrictedNoteEntity::toDomain)
                .collect(Collectors.toList());
        return new PageImpl<>(notes, pageable, entityPage.getTotalElements());
    }
    
    @Override
    public Page<RestrictedNote> findAccessibleToUser(UUID userId, List<String> userRoles, Pageable pageable) {
        Page<JpaRestrictedNoteEntity> entityPage = jpaRepository.findAccessibleToUser(userId, pageable);
        AccessContext context = AccessContext.fromRoleStrings(
                userId,
                "System User",
                userRoles,
                "Repository access check",
                "0.0.0.0",
                "repository-session",
                "Repository"
        );

        List<RestrictedNote> notes = entityPage.getContent().stream()
                .map(JpaRestrictedNoteEntity::toDomain)
                .filter(note -> {
                    PolicyDecision decision = policyService.canAccessNote(
                            note.getNoteId(),
                            note.getAuthorId(),
                            note.getNoteType().name(),
                            note.getVisibilityScope().name(),
                            note.getIsSealed() != null && note.getIsSealed(),
                            note.getSealedBy(),
                            note.getAuthorizedViewers(),
                            context
                    );
                    return decision.isAllowed();
                })
                .collect(Collectors.toList());
        return new PageImpl<>(notes, pageable, entityPage.getTotalElements());
    }

    @Override
    public Page<RestrictedNote> findByClientIdAccessibleToUser(UUID clientId, UUID userId, List<String> userRoles, Pageable pageable) {
        Page<JpaRestrictedNoteEntity> entityPage = jpaRepository.findByClientIdAccessibleToUser(clientId, userId, pageable);
        AccessContext context = AccessContext.fromRoleStrings(
                userId,
                "System User",
                userRoles,
                "Repository access check",
                "0.0.0.0",
                "repository-session",
                "Repository"
        );

        List<RestrictedNote> notes = entityPage.getContent().stream()
                .map(JpaRestrictedNoteEntity::toDomain)
                .filter(note -> {
                    PolicyDecision decision = policyService.canAccessNote(
                            note.getNoteId(),
                            note.getAuthorId(),
                            note.getNoteType().name(),
                            note.getVisibilityScope().name(),
                            note.getIsSealed() != null && note.getIsSealed(),
                            note.getSealedBy(),
                            note.getAuthorizedViewers(),
                            context
                    );
                    return decision.isAllowed();
                })
                .collect(Collectors.toList());
        return new PageImpl<>(notes, pageable, entityPage.getTotalElements());
    }
    
    @Override
    public List<RestrictedNote> findSealedNotes() {
        return jpaRepository.findSealedNotes().stream()
                .map(JpaRestrictedNoteEntity::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<RestrictedNote> findByNoteTypes(List<RestrictedNote.NoteType> types) {
        return jpaRepository.findByNoteTypes(types).stream()
                .map(JpaRestrictedNoteEntity::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<RestrictedNote> findByVisibilityScopes(List<RestrictedNote.VisibilityScope> scopes) {
        return jpaRepository.findByVisibilityScopes(scopes).stream()
                .map(JpaRestrictedNoteEntity::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<RestrictedNote> findByCreatedAtBetween(Instant start, Instant end) {
        return jpaRepository.findByCreatedAtBetween(start, end).stream()
                .map(JpaRestrictedNoteEntity::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public Long countByClientId(UUID clientId) {
        return jpaRepository.countByClientId(clientId);
    }
    
    @Override
    public Long countByAuthorId(UUID authorId) {
        return jpaRepository.countByAuthorId(authorId);
    }
    
    @Override
    public Long countByNoteType(RestrictedNote.NoteType noteType) {
        return jpaRepository.countByNoteType(noteType);
    }
    
    @Override
    public Long countSealedNotes() {
        return jpaRepository.countSealedNotes();
    }
}
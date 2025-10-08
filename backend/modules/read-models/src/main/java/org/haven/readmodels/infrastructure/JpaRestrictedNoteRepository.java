package org.haven.readmodels.infrastructure;

import org.haven.readmodels.domain.RestrictedNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaRestrictedNoteRepository extends JpaRepository<JpaRestrictedNoteEntity, UUID> {
    
    Page<JpaRestrictedNoteEntity> findByClientId(UUID clientId, Pageable pageable);
    
    Page<JpaRestrictedNoteEntity> findByCaseId(UUID caseId, Pageable pageable);
    
    Page<JpaRestrictedNoteEntity> findByAuthorId(UUID authorId, Pageable pageable);
    
    List<JpaRestrictedNoteEntity> findByNoteType(RestrictedNote.NoteType noteType);
    
    List<JpaRestrictedNoteEntity> findByVisibilityScope(RestrictedNote.VisibilityScope scope);
    
    @Query("SELECT n FROM JpaRestrictedNoteEntity n WHERE n.clientId = :clientId AND n.noteType = :noteType")
    Page<JpaRestrictedNoteEntity> findByClientIdAndNoteType(@Param("clientId") UUID clientId, 
                                                           @Param("noteType") RestrictedNote.NoteType noteType, 
                                                           Pageable pageable);
    
    @Query("SELECT n FROM JpaRestrictedNoteEntity n WHERE n.caseId = :caseId AND n.noteType = :noteType")
    Page<JpaRestrictedNoteEntity> findByCaseIdAndNoteType(@Param("caseId") UUID caseId, 
                                                         @Param("noteType") RestrictedNote.NoteType noteType, 
                                                         Pageable pageable);
    
    @Query("SELECT n FROM JpaRestrictedNoteEntity n WHERE n.authorId = :userId OR :userId MEMBER OF n.authorizedViewers")
    Page<JpaRestrictedNoteEntity> findAccessibleToUser(@Param("userId") UUID userId, Pageable pageable);
    
    @Query("SELECT n FROM JpaRestrictedNoteEntity n WHERE n.clientId = :clientId AND " +
           "(n.authorId = :userId OR :userId MEMBER OF n.authorizedViewers)")
    Page<JpaRestrictedNoteEntity> findByClientIdAccessibleToUser(@Param("clientId") UUID clientId, 
                                                                @Param("userId") UUID userId, 
                                                                Pageable pageable);
    
    @Query("SELECT n FROM JpaRestrictedNoteEntity n WHERE n.isSealed = true")
    List<JpaRestrictedNoteEntity> findSealedNotes();
    
    @Query("SELECT n FROM JpaRestrictedNoteEntity n WHERE n.noteType IN :types")
    List<JpaRestrictedNoteEntity> findByNoteTypes(@Param("types") List<RestrictedNote.NoteType> types);
    
    @Query("SELECT n FROM JpaRestrictedNoteEntity n WHERE n.visibilityScope IN :scopes")
    List<JpaRestrictedNoteEntity> findByVisibilityScopes(@Param("scopes") List<RestrictedNote.VisibilityScope> scopes);
    
    @Query("SELECT n FROM JpaRestrictedNoteEntity n WHERE n.createdAt BETWEEN :start AND :end")
    List<JpaRestrictedNoteEntity> findByCreatedAtBetween(@Param("start") Instant start, @Param("end") Instant end);
    
    @Query("SELECT COUNT(n) FROM JpaRestrictedNoteEntity n WHERE n.clientId = :clientId")
    Long countByClientId(@Param("clientId") UUID clientId);
    
    @Query("SELECT COUNT(n) FROM JpaRestrictedNoteEntity n WHERE n.authorId = :authorId")
    Long countByAuthorId(@Param("authorId") UUID authorId);
    
    @Query("SELECT COUNT(n) FROM JpaRestrictedNoteEntity n WHERE n.noteType = :noteType")
    Long countByNoteType(@Param("noteType") RestrictedNote.NoteType noteType);
    
    @Query("SELECT COUNT(n) FROM JpaRestrictedNoteEntity n WHERE n.isSealed = true")
    Long countSealedNotes();
}
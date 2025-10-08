package org.haven.casemgmt.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface RestrictedNoteReadModelRepository extends JpaRepository<RestrictedNoteReadModel, UUID> {
    
    List<RestrictedNoteReadModel> findByClientIdOrderByCreatedAtDesc(UUID clientId);
    
    List<RestrictedNoteReadModel> findByCaseIdOrderByCreatedAtDesc(UUID caseId);
    
    List<RestrictedNoteReadModel> findByAuthorIdOrderByCreatedAtDesc(UUID authorId);
    
    List<RestrictedNoteReadModel> findByNoteTypeOrderByCreatedAtDesc(RestrictedNoteReadModel.NoteType noteType);
    
    List<RestrictedNoteReadModel> findByVisibilityScopeOrderByCreatedAtDesc(RestrictedNoteReadModel.VisibilityScope visibilityScope);
    
    List<RestrictedNoteReadModel> findByIsSealedOrderByCreatedAtDesc(boolean isSealed);
    
    List<RestrictedNoteReadModel> findByIsTemporaryTrueAndExpiresAtBeforeOrderByExpiresAt(Instant expirationTime);
    
    @Query("SELECT n FROM RestrictedNoteReadModel n WHERE n.clientId = :clientId AND n.visibilityScope IN :allowedScopes ORDER BY n.createdAt DESC")
    List<RestrictedNoteReadModel> findByClientIdAndVisibilityScopeIn(@Param("clientId") UUID clientId, @Param("allowedScopes") List<RestrictedNoteReadModel.VisibilityScope> allowedScopes);
    
    @Query("SELECT n FROM RestrictedNoteReadModel n WHERE n.caseId = :caseId AND n.visibilityScope IN :allowedScopes ORDER BY n.createdAt DESC")
    List<RestrictedNoteReadModel> findByCaseIdAndVisibilityScopeIn(@Param("caseId") UUID caseId, @Param("allowedScopes") List<RestrictedNoteReadModel.VisibilityScope> allowedScopes);
    
    @Query("SELECT n FROM RestrictedNoteReadModel n WHERE n.authorId = :authorId AND n.isSealed = false ORDER BY n.createdAt DESC")
    List<RestrictedNoteReadModel> findNonSealedByAuthorId(@Param("authorId") UUID authorId);
    
    @Query("SELECT n FROM RestrictedNoteReadModel n WHERE n.clientId = :clientId AND n.noteType IN :noteTypes ORDER BY n.createdAt DESC")
    List<RestrictedNoteReadModel> findByClientIdAndNoteTypeIn(@Param("clientId") UUID clientId, @Param("noteTypes") List<RestrictedNoteReadModel.NoteType> noteTypes);
    
    @Query("SELECT n FROM RestrictedNoteReadModel n WHERE n.createdAt BETWEEN :startDate AND :endDate ORDER BY n.createdAt DESC")
    List<RestrictedNoteReadModel> findByCreatedAtBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
    
    @Query("SELECT n FROM RestrictedNoteReadModel n WHERE n.clientId = :clientId AND n.isSealed = :isSealed AND n.visibilityScope IN :allowedScopes ORDER BY n.createdAt DESC")
    List<RestrictedNoteReadModel> findByClientIdAndSealedStatusAndVisibilityScope(@Param("clientId") UUID clientId, @Param("isSealed") boolean isSealed, @Param("allowedScopes") List<RestrictedNoteReadModel.VisibilityScope> allowedScopes);
}
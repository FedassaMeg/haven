package org.haven.programenrollment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaProgramEnrollmentRepository extends JpaRepository<JpaProgramEnrollmentEntity, UUID> {
    
    List<JpaProgramEnrollmentEntity> findByClientId(UUID clientId);
    
    List<JpaProgramEnrollmentEntity> findByProgramId(UUID programId);
    
    List<JpaProgramEnrollmentEntity> findByStatus(JpaProgramEnrollmentEntity.EnrollmentStatus status);
    
    List<JpaProgramEnrollmentEntity> findByClientIdAndStatus(UUID clientId, JpaProgramEnrollmentEntity.EnrollmentStatus status);
    
    // Joint TH/RRH specific queries
    List<JpaProgramEnrollmentEntity> findByPredecessorEnrollmentId(UUID predecessorId);
    
    @Query("SELECT e FROM JpaProgramEnrollmentEntity e WHERE e.predecessorEnrollmentId = :enrollmentId OR e.id = :enrollmentId")
    List<JpaProgramEnrollmentEntity> findEnrollmentChain(@Param("enrollmentId") UUID enrollmentId);
    
    @Query("SELECT e FROM JpaProgramEnrollmentEntity e WHERE e.householdId = :householdId ORDER BY e.enrollmentDate")
    List<JpaProgramEnrollmentEntity> findByHouseholdId(@Param("householdId") String householdId);
    
    @Query("SELECT e FROM JpaProgramEnrollmentEntity e WHERE e.residentialMoveInDate IS NOT NULL AND e.residentialMoveInDate BETWEEN :startDate AND :endDate")
    List<JpaProgramEnrollmentEntity> findByMoveInDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("""
        WITH RECURSIVE enrollment_chain AS (
            SELECT e.id, e.client_id, e.program_id, e.predecessor_enrollment_id, 
                   e.enrollment_date, e.household_id, 0 as chain_depth
            FROM program_enrollments e 
            WHERE e.id = :rootEnrollmentId
            
            UNION ALL
            
            SELECT e.id, e.client_id, e.program_id, e.predecessor_enrollment_id,
                   e.enrollment_date, e.household_id, ec.chain_depth + 1
            FROM program_enrollments e
            INNER JOIN enrollment_chain ec ON e.predecessor_enrollment_id = ec.id
        )
        SELECT e.* FROM JpaProgramEnrollmentEntity e 
        WHERE e.id IN (SELECT ec.id FROM enrollment_chain ec)
        ORDER BY e.enrollmentDate
        """)
    List<JpaProgramEnrollmentEntity> findCompleteEnrollmentChain(@Param("rootEnrollmentId") UUID rootEnrollmentId);
    
    @Query("SELECT COUNT(e) FROM JpaProgramEnrollmentEntity e WHERE e.predecessorEnrollmentId IS NOT NULL")
    long countLinkedEnrollments();
    
    Optional<JpaProgramEnrollmentEntity> findByClientIdAndProgramIdAndStatus(
        UUID clientId, UUID programId, JpaProgramEnrollmentEntity.EnrollmentStatus status);
    
    /**
     * Find enrollments by enrollment date range (for compliance reporting)
     */
    List<JpaProgramEnrollmentEntity> findAllByEnrollmentDateBetween(LocalDate startDate, LocalDate endDate);
}
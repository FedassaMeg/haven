package org.haven.programenrollment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository for Disability Records
 * Supports HMIS-compliant lifecycle queries for all disability types
 */
@Repository
public interface JpaDisabilityRepository extends JpaRepository<JpaDisabilityEntity, UUID> {
    
    /**
     * Find all disability records for a specific enrollment
     */
    List<JpaDisabilityEntity> findByEnrollmentIdOrderByInformationDateDesc(UUID enrollmentId);
    
    /**
     * Find all disability records for a specific client
     */
    List<JpaDisabilityEntity> findByClientIdOrderByInformationDateDesc(UUID clientId);
    
    /**
     * Find records by enrollment and disability kind
     */
    List<JpaDisabilityEntity> findByEnrollmentIdAndDisabilityKindOrderByInformationDateDesc(
        UUID enrollmentId, String disabilityKind);
    
    /**
     * Find the most recent disability record for an enrollment and kind
     */
    Optional<JpaDisabilityEntity> findFirstByEnrollmentIdAndDisabilityKindOrderByInformationDateDesc(
        UUID enrollmentId, String disabilityKind);
    
    /**
     * Find records by stage and kind
     */
    List<JpaDisabilityEntity> findByEnrollmentIdAndStageAndDisabilityKindOrderByInformationDateDesc(
        UUID enrollmentId, String stage, String disabilityKind);
    
    /**
     * Find specific record by enrollment, stage, and kind
     */
    Optional<JpaDisabilityEntity> findByEnrollmentIdAndStageAndDisabilityKindAndIsCorrection(
        UUID enrollmentId, String stage, String disabilityKind, Boolean isCorrection);
    
    /**
     * Find records within a date range for reporting
     */
    @Query("SELECT d FROM JpaDisabilityEntity d WHERE d.informationDate >= :startDate AND d.informationDate <= :endDate ORDER BY d.informationDate DESC")
    List<JpaDisabilityEntity> findByInformationDateBetween(
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate);
    
    /**
     * Find latest record per enrollment per disability kind for reporting
     */
    @Query("""
        SELECT d FROM JpaDisabilityEntity d 
        WHERE (d.enrollmentId, d.disabilityKind, d.informationDate) IN (
            SELECT d2.enrollmentId, d2.disabilityKind, MAX(d2.informationDate) 
            FROM JpaDisabilityEntity d2 
            WHERE d2.informationDate >= :startDate 
            AND d2.informationDate <= :endDate 
            AND d2.isCorrection = FALSE
            GROUP BY d2.enrollmentId, d2.disabilityKind
        )
        AND d.isCorrection = FALSE
        ORDER BY d.enrollmentId, d.disabilityKind
        """)
    List<JpaDisabilityEntity> findLatestRecordPerEnrollmentAndKind(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Find latest record per enrollment (single record per enrollment across all disability kinds)
     */
    @Query("""
        SELECT d FROM JpaDisabilityEntity d 
        WHERE (d.enrollmentId, d.informationDate) IN (
            SELECT d2.enrollmentId, MAX(d2.informationDate) 
            FROM JpaDisabilityEntity d2 
            WHERE d2.informationDate >= :startDate 
            AND d2.informationDate <= :endDate 
            AND d2.isCorrection = FALSE
            GROUP BY d2.enrollmentId
        )
        AND d.isCorrection = FALSE
        ORDER BY d.enrollmentId
        """)
    List<JpaDisabilityEntity> findLatestRecordPerEnrollment(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Find records indicating disabling conditions
     */
    @Query("""
        SELECT d FROM JpaDisabilityEntity d 
        WHERE d.hasDisability = 'YES' 
        AND d.expectedLongTerm = 'YES'
        AND d.informationDate >= :startDate 
        AND d.informationDate <= :endDate
        AND d.isCorrection = FALSE
        """)
    List<JpaDisabilityEntity> findRecordsWithDisablingConditions(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Find records indicating disabling conditions for specific disability kind
     */
    @Query("""
        SELECT d FROM JpaDisabilityEntity d 
        WHERE d.hasDisability = 'YES' 
        AND d.expectedLongTerm = 'YES'
        AND d.disabilityKind = :disabilityKind
        AND d.informationDate >= :startDate 
        AND d.informationDate <= :endDate
        AND d.isCorrection = FALSE
        """)
    List<JpaDisabilityEntity> findRecordsWithDisablingConditions(
        @Param("disabilityKind") String disabilityKind,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Find behavioral health disability records (mental health and substance use)
     */
    @Query("""
        SELECT d FROM JpaDisabilityEntity d 
        WHERE d.disabilityKind IN ('MENTAL_HEALTH', 'SUBSTANCE_USE')
        AND d.informationDate >= :startDate 
        AND d.informationDate <= :endDate
        AND d.isCorrection = FALSE
        """)
    List<JpaDisabilityEntity> findBehavioralHealthDisabilityRecords(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Find records by disability kind
     */
    @Query("""
        SELECT d FROM JpaDisabilityEntity d 
        WHERE d.disabilityKind = :disabilityKind
        AND d.informationDate >= :startDate 
        AND d.informationDate <= :endDate
        AND d.isCorrection = FALSE
        """)
    List<JpaDisabilityEntity> findByDisabilityKindAndDateRange(
        @Param("disabilityKind") String disabilityKind,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Find enrollments missing PROJECT_START records for specific disability kind
     */
    @Query("""
        SELECT DISTINCT e.id FROM JpaProgramEnrollmentEntity e 
        WHERE NOT EXISTS (
            SELECT d FROM JpaDisabilityEntity d 
            WHERE d.enrollmentId = e.id 
            AND d.stage = 'PROJECT_START'
            AND d.disabilityKind = :disabilityKind
        )
        """)
    List<UUID> findEnrollmentsMissingProjectStartRecord(@Param("disabilityKind") String disabilityKind);
    
    /**
     * Find enrollments missing PROJECT_EXIT records for specific disability kind
     */
    @Query("""
        SELECT DISTINCT e.id FROM JpaProgramEnrollmentEntity e 
        WHERE NOT EXISTS (
            SELECT d FROM JpaDisabilityEntity d 
            WHERE d.enrollmentId = e.id 
            AND d.stage = 'PROJECT_EXIT'
            AND d.disabilityKind = :disabilityKind
        )
        AND e.projectExit IS NOT NULL
        """)
    List<UUID> findEnrollmentsMissingProjectExitRecord(@Param("disabilityKind") String disabilityKind);
    
    /**
     * Find records with data quality issues
     */
    @Query("""
        SELECT d FROM JpaDisabilityEntity d 
        WHERE d.isCorrection = FALSE
        AND (
            (d.hasDisability = 'YES' AND (d.expectedLongTerm IS NULL OR d.expectedLongTerm = 'DATA_NOT_COLLECTED'))
            OR (d.hasDisability = 'NO' AND d.expectedLongTerm IS NOT NULL AND d.expectedLongTerm != 'DATA_NOT_COLLECTED')
        )
        """)
    List<JpaDisabilityEntity> findRecordsWithDataQualityIssues();
    
    /**
     * Find correction records for a specific original record
     */
    List<JpaDisabilityEntity> findByCorrectsRecordIdOrderByCreatedAtDesc(UUID originalRecordId);
    
    /**
     * Check if enrollment has records for specific disability kind
     */
    boolean existsByEnrollmentIdAndDisabilityKind(UUID enrollmentId, String disabilityKind);
    
    /**
     * Check if enrollment has PROJECT_START record for specific disability kind
     */
    boolean existsByEnrollmentIdAndStageAndDisabilityKind(UUID enrollmentId, String stage, String disabilityKind);
    
    /**
     * Check if enrollment has record for specific disability kind and stage - convenience method
     */
    default boolean existsByEnrollmentIdAndDisabilityKindAndStage(UUID enrollmentId, String disabilityKind, String stage) {
        return existsByEnrollmentIdAndStageAndDisabilityKind(enrollmentId, stage, disabilityKind);
    }
    
    /**
     * Find records for HMIS CSV export
     */
    @Query("""
        SELECT d FROM JpaDisabilityEntity d 
        WHERE d.informationDate >= :startDate 
        AND d.informationDate <= :endDate 
        AND d.isCorrection = FALSE
        ORDER BY d.enrollmentId, d.disabilityKind, d.informationDate DESC
        """)
    List<JpaDisabilityEntity> findForHmisExport(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Count records by disability kind for reporting
     */
    @Query("SELECT d.disabilityKind, COUNT(d) FROM JpaDisabilityEntity d WHERE d.informationDate >= :startDate AND d.informationDate <= :endDate AND d.isCorrection = FALSE GROUP BY d.disabilityKind")
    List<Object[]> countRecordsByDisabilityKind(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Count records with disabling conditions for specific disability kind
     */
    @Query("""
        SELECT COUNT(d) FROM JpaDisabilityEntity d 
        WHERE d.hasDisability = 'YES' 
        AND d.expectedLongTerm = 'YES'
        AND d.disabilityKind = :disabilityKind
        AND d.informationDate >= :startDate 
        AND d.informationDate <= :endDate
        AND d.isCorrection = FALSE
        """)
    Long countRecordsWithDisablingConditions(
        @Param("disabilityKind") String disabilityKind,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Count behavioral health disability records
     */
    @Query("""
        SELECT COUNT(d) FROM JpaDisabilityEntity d 
        WHERE d.disabilityKind IN ('MENTAL_HEALTH', 'SUBSTANCE_USE')
        AND d.informationDate >= :startDate 
        AND d.informationDate <= :endDate
        AND d.isCorrection = FALSE
        """)
    Long countBehavioralHealthDisabilityRecords(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
}

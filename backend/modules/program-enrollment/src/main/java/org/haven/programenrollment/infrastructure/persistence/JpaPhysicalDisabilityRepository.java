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
 * JPA Repository for Physical Disability Records
 * Supports HMIS-compliant lifecycle queries for UDE 3.08
 */
@Repository
public interface JpaPhysicalDisabilityRepository extends JpaRepository<JpaPhysicalDisabilityEntity, UUID> {
    
    /**
     * Find all physical disability records for a specific enrollment
     */
    List<JpaPhysicalDisabilityEntity> findByEnrollmentIdOrderByInformationDateDesc(UUID enrollmentId);
    
    /**
     * Find all physical disability records for a specific client
     */
    List<JpaPhysicalDisabilityEntity> findByClientIdOrderByInformationDateDesc(UUID clientId);
    
    /**
     * Find the most recent physical disability record for an enrollment
     */
    Optional<JpaPhysicalDisabilityEntity> findFirstByEnrollmentIdOrderByInformationDateDesc(UUID enrollmentId);
    
    /**
     * Find records by stage (PROJECT_START, UPDATE, PROJECT_EXIT)
     */
    List<JpaPhysicalDisabilityEntity> findByEnrollmentIdAndStageOrderByInformationDateDesc(
        UUID enrollmentId, String stage);
    
    /**
     * Find the PROJECT_START record for an enrollment
     */
    Optional<JpaPhysicalDisabilityEntity> findByEnrollmentIdAndStage(UUID enrollmentId, String stage);
    
    /**
     * Find physical disability records within a date range for reporting
     */
    @Query("SELECT p FROM JpaPhysicalDisabilityEntity p WHERE p.informationDate >= :startDate AND p.informationDate <= :endDate ORDER BY p.informationDate DESC")
    List<JpaPhysicalDisabilityEntity> findByInformationDateBetween(
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate);
    
    /**
     * Find the latest record per enrollment for reporting (non-correction records only)
     */
    @Query("""
        SELECT p FROM JpaPhysicalDisabilityEntity p 
        WHERE (p.enrollmentId, p.informationDate) IN (
            SELECT p2.enrollmentId, MAX(p2.informationDate) 
            FROM JpaPhysicalDisabilityEntity p2 
            WHERE p2.informationDate >= :startDate 
            AND p2.informationDate <= :endDate 
            AND p2.isCorrection = FALSE
            GROUP BY p2.enrollmentId
        )
        AND p.isCorrection = FALSE
        ORDER BY p.enrollmentId
        """)
    List<JpaPhysicalDisabilityEntity> findLatestRecordPerEnrollment(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Find latest effective record per enrollment/stage combination
     */
    @Query("""
        SELECT p FROM JpaPhysicalDisabilityEntity p 
        WHERE (p.enrollmentId, p.stage, p.informationDate) IN (
            SELECT p2.enrollmentId, p2.stage, MAX(p2.informationDate) 
            FROM JpaPhysicalDisabilityEntity p2 
            WHERE p2.informationDate >= :startDate 
            AND p2.informationDate <= :endDate 
            AND p2.isCorrection = FALSE
            GROUP BY p2.enrollmentId, p2.stage
        )
        AND p.isCorrection = FALSE
        ORDER BY p.enrollmentId, p.stage, p.informationDate DESC
        """)
    List<JpaPhysicalDisabilityEntity> findLatestRecordPerEnrollmentAndStage(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Find records indicating disabling conditions (physical_disability = YES AND physical_expected_long_term = YES)
     */
    @Query("""
        SELECT p FROM JpaPhysicalDisabilityEntity p 
        WHERE p.physicalDisability = 'YES' 
        AND p.physicalExpectedLongTerm = 'YES'
        AND p.informationDate >= :startDate 
        AND p.informationDate <= :endDate
        AND p.isCorrection = FALSE
        """)
    List<JpaPhysicalDisabilityEntity> findRecordsWithDisablingConditions(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Find records with physical disabilities (regardless of long-term expectation)
     */
    @Query("""
        SELECT p FROM JpaPhysicalDisabilityEntity p 
        WHERE p.physicalDisability = 'YES'
        AND p.informationDate >= :startDate 
        AND p.informationDate <= :endDate
        AND p.isCorrection = FALSE
        """)
    List<JpaPhysicalDisabilityEntity> findRecordsWithPhysicalDisabilities(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Find enrollments missing PROJECT_START records
     */
    @Query("""
        SELECT DISTINCT e.id FROM JpaProgramEnrollmentEntity e 
        WHERE NOT EXISTS (
            SELECT p FROM JpaPhysicalDisabilityEntity p 
            WHERE p.enrollmentId = e.id 
            AND p.stage = 'PROJECT_START'
        )
        """)
    List<UUID> findEnrollmentsMissingProjectStartRecord();
    
    /**
     * Find enrollments that exited but don't have PROJECT_EXIT records
     */
    @Query("""
        SELECT DISTINCT e.id FROM JpaProgramEnrollmentEntity e 
        WHERE e.projectExit IS NOT NULL
        AND NOT EXISTS (
            SELECT p FROM JpaPhysicalDisabilityEntity p 
            WHERE p.enrollmentId = e.id 
            AND p.stage = 'PROJECT_EXIT'
        )
        """)
    List<UUID> findEnrollmentsMissingProjectExitRecord();
    
    /**
     * Find records that may need correction (data quality issues)
     */
    @Query("""
        SELECT p FROM JpaPhysicalDisabilityEntity p 
        WHERE p.isCorrection = FALSE
        AND (
            (p.physicalDisability = 'YES' AND (p.physicalExpectedLongTerm IS NULL OR p.physicalExpectedLongTerm = 'DATA_NOT_COLLECTED'))
            OR (p.physicalDisability = 'NO' AND p.physicalExpectedLongTerm IS NOT NULL AND p.physicalExpectedLongTerm != 'DATA_NOT_COLLECTED')
        )
        """)
    List<JpaPhysicalDisabilityEntity> findRecordsWithDataQualityIssues();
    
    /**
     * Find correction records for a specific original record
     */
    List<JpaPhysicalDisabilityEntity> findByCorrectsRecordIdOrderByCreatedAtDesc(UUID originalRecordId);
    
    /**
     * Check if enrollment has any physical disability records
     */
    boolean existsByEnrollmentId(UUID enrollmentId);
    
    /**
     * Check if enrollment has PROJECT_START record
     */
    boolean existsByEnrollmentIdAndStage(UUID enrollmentId, String stage);
    
    /**
     * Check if enrollment has records for a specific stage (non-correction)
     */
    boolean existsByEnrollmentIdAndStageAndIsCorrection(UUID enrollmentId, String stage, Boolean isCorrection);
    
    /**
     * Find records for HMIS CSV export within date range
     */
    @Query("""
        SELECT p FROM JpaPhysicalDisabilityEntity p 
        WHERE p.informationDate >= :startDate 
        AND p.informationDate <= :endDate 
        AND p.isCorrection = FALSE
        ORDER BY p.enrollmentId, p.informationDate DESC
        """)
    List<JpaPhysicalDisabilityEntity> findForHmisExport(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Count records by stage for reporting
     */
    @Query("SELECT p.stage, COUNT(p) FROM JpaPhysicalDisabilityEntity p WHERE p.informationDate >= :startDate AND p.informationDate <= :endDate AND p.isCorrection = FALSE GROUP BY p.stage")
    List<Object[]> countRecordsByStage(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Count records with disabling conditions for reporting
     */
    @Query("""
        SELECT COUNT(p) FROM JpaPhysicalDisabilityEntity p 
        WHERE p.physicalDisability = 'YES' 
        AND p.physicalExpectedLongTerm = 'YES'
        AND p.informationDate >= :startDate 
        AND p.informationDate <= :endDate
        AND p.isCorrection = FALSE
        """)
    Long countRecordsWithDisablingConditions(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
}

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
 * JPA Repository for Domestic Violence Records
 * Supports HMIS-compliant lifecycle queries with enhanced security considerations
 */
@Repository
public interface JpaDvRepository extends JpaRepository<JpaDvEntity, UUID> {
    
    /**
     * Find all DV records for a specific enrollment
     */
    List<JpaDvEntity> findByEnrollmentIdOrderByInformationDateDesc(UUID enrollmentId);
    
    /**
     * Find all DV records for a specific client
     */
    List<JpaDvEntity> findByClientIdOrderByInformationDateDesc(UUID clientId);
    
    /**
     * Find the most recent DV record for an enrollment
     */
    Optional<JpaDvEntity> findFirstByEnrollmentIdOrderByInformationDateDesc(UUID enrollmentId);
    
    /**
     * Find records by stage
     */
    List<JpaDvEntity> findByEnrollmentIdAndStageOrderByInformationDateDesc(UUID enrollmentId, String stage);
    
    /**
     * Find specific record by enrollment and stage
     */
    Optional<JpaDvEntity> findByEnrollmentIdAndStageAndIsCorrection(UUID enrollmentId, String stage, Boolean isCorrection);
    
    /**
     * Find records with DV history = YES (high sensitivity)
     */
    @Query("SELECT d FROM JpaDvEntity d WHERE d.dvHistory = 'YES' AND d.informationDate >= :startDate AND d.informationDate <= :endDate")
    List<JpaDvEntity> findRecordsWithDvHistory(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Find records with clients currently fleeing DV (highest risk)
     */
    @Query("SELECT d FROM JpaDvEntity d WHERE d.currentlyFleeing = 'YES' AND d.informationDate >= :startDate AND d.informationDate <= :endDate")
    List<JpaDvEntity> findRecordsCurrentlyFleeingDv(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Find recent DV records (within 6 months)
     */
    @Query("""
        SELECT d FROM JpaDvEntity d 
        WHERE d.whenExperienced IN ('WITHIN_3_MONTHS', 'THREE_TO_SIX_MONTHS')
        AND d.informationDate >= :startDate 
        AND d.informationDate <= :endDate
        """)
    List<JpaDvEntity> findRecentDvRecords(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Find latest effective record per enrollment
     */
    @Query("""
        SELECT d FROM JpaDvEntity d 
        WHERE (d.enrollmentId, d.informationDate) IN (
            SELECT d2.enrollmentId, MAX(d2.informationDate) 
            FROM JpaDvEntity d2 
            WHERE d2.informationDate >= :startDate 
            AND d2.informationDate <= :endDate 
            AND d2.isCorrection = FALSE
            GROUP BY d2.enrollmentId
        )
        AND d.isCorrection = FALSE
        ORDER BY d.enrollmentId
        """)
    List<JpaDvEntity> findLatestRecordPerEnrollment(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Find records with data quality issues
     */
    @Query("""
        SELECT d FROM JpaDvEntity d 
        WHERE d.isCorrection = FALSE
        AND (
            (d.dvHistory = 'YES' AND (d.whenExperienced IS NULL OR d.whenExperienced = 'DATA_NOT_COLLECTED'))
            OR (d.dvHistory = 'NO' AND d.whenExperienced IS NOT NULL AND d.whenExperienced != 'DATA_NOT_COLLECTED')
        )
        """)
    List<JpaDvEntity> findRecordsWithDataQualityIssues();
    
    /**
     * Check if enrollment has DV records
     */
    boolean existsByEnrollmentId(UUID enrollmentId);
    
    /**
     * Check if enrollment has PROJECT_START record
     */
    boolean existsByEnrollmentIdAndStage(UUID enrollmentId, String stage);
    
    /**
     * Find records for HMIS export (with privacy considerations)
     */
    @Query("""
        SELECT d FROM JpaDvEntity d 
        WHERE d.informationDate >= :startDate 
        AND d.informationDate <= :endDate 
        AND d.isCorrection = FALSE
        ORDER BY d.enrollmentId, d.informationDate DESC
        """)
    List<JpaDvEntity> findForHmisExport(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Count high-risk DV cases for reporting
     */
    @Query("SELECT COUNT(d) FROM JpaDvEntity d WHERE d.currentlyFleeing = 'YES' AND d.informationDate >= :startDate AND d.informationDate <= :endDate AND d.isCorrection = FALSE")
    Long countHighRiskCases(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Find enrollments missing PROJECT_START DV records
     */
    @Query("""
        SELECT DISTINCT e.id FROM JpaProgramEnrollmentEntity e 
        WHERE NOT EXISTS (
            SELECT d FROM JpaDvEntity d 
            WHERE d.enrollmentId = e.id 
            AND d.stage = 'PROJECT_START'
        )
        """)
    List<UUID> findEnrollmentsMissingProjectStartRecord();
    
    /**
     * Find enrollments missing PROJECT_EXIT DV records
     */
    @Query("""
        SELECT DISTINCT e.id FROM JpaProgramEnrollmentEntity e 
        WHERE NOT EXISTS (
            SELECT d FROM JpaDvEntity d 
            WHERE d.enrollmentId = e.id 
            AND d.stage = 'PROJECT_EXIT'
        )
        AND e.projectExit IS NOT NULL
        """)
    List<UUID> findEnrollmentsMissingProjectExitRecord();
    
    /**
     * Find correction records for a specific original record
     */
    List<JpaDvEntity> findByCorrectsRecordIdOrderByCreatedAtDesc(UUID originalRecordId);
}

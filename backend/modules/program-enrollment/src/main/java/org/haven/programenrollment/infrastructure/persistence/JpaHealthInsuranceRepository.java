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
 * JPA Repository for Health Insurance Records
 * Supports HMIS-compliant lifecycle queries
 */
@Repository
public interface JpaHealthInsuranceRepository extends JpaRepository<JpaHealthInsuranceEntity, UUID> {
    
    /**
     * Find all health insurance records for a specific enrollment
     */
    List<JpaHealthInsuranceEntity> findByEnrollmentIdOrderByInformationDateDesc(UUID enrollmentId);
    
    /**
     * Find all health insurance records for a specific client
     */
    List<JpaHealthInsuranceEntity> findByClientIdOrderByInformationDateDesc(UUID clientId);
    
    /**
     * Find the most recent health insurance record for an enrollment
     */
    Optional<JpaHealthInsuranceEntity> findFirstByEnrollmentIdOrderByInformationDateDesc(UUID enrollmentId);
    
    /**
     * Find health insurance records by type (START, UPDATE, ANNUAL, EXIT, MINOR18)
     */
    List<JpaHealthInsuranceEntity> findByEnrollmentIdAndRecordTypeOrderByInformationDateDesc(
        UUID enrollmentId, String recordType);
    
    /**
     * Find the START record for an enrollment
     */
    Optional<JpaHealthInsuranceEntity> findByEnrollmentIdAndRecordType(UUID enrollmentId, String recordType);
    
    /**
     * Find health insurance records within a date range for reporting
     */
    @Query("SELECT h FROM JpaHealthInsuranceEntity h WHERE h.informationDate >= :startDate AND h.informationDate <= :endDate ORDER BY h.informationDate DESC")
    List<JpaHealthInsuranceEntity> findByInformationDateBetween(
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate);
    
    /**
     * Find enrollments that need annual health insurance assessments
     * (enrollments that started more than a year ago and don't have annual assessment in last year)
     */
    @Query("""
        SELECT DISTINCT h1.enrollmentId FROM JpaHealthInsuranceEntity h1 
        WHERE h1.recordType = 'START' 
        AND h1.informationDate <= :oneYearAgo
        AND NOT EXISTS (
            SELECT h2 FROM JpaHealthInsuranceEntity h2 
            WHERE h2.enrollmentId = h1.enrollmentId 
            AND h2.recordType = 'ANNUAL' 
            AND h2.informationDate >= :oneYearAgo
        )
        AND NOT EXISTS (
            SELECT h3 FROM JpaHealthInsuranceEntity h3 
            WHERE h3.enrollmentId = h1.enrollmentId 
            AND h3.recordType = 'EXIT'
        )
        """)
    List<UUID> findEnrollmentIdsNeedingAnnualAssessment(@Param("oneYearAgo") LocalDate oneYearAgo);
    
    /**
     * Find clients who turned 18 and need health insurance assessment
     */
    @Query("""
        SELECT DISTINCT h.clientId FROM JpaHealthInsuranceEntity h 
        WHERE h.clientId IN :clientIds
        AND NOT EXISTS (
            SELECT h2 FROM JpaHealthInsuranceEntity h2 
            WHERE h2.clientId = h.clientId 
            AND h2.recordType = 'MINOR18'
            AND h2.informationDate >= :since18thBirthday
        )
        """)
    List<UUID> findClientsNeedingMinor18Assessment(
        @Param("clientIds") List<UUID> clientIds,
        @Param("since18thBirthday") LocalDate since18thBirthday);
    
    /**
     * Check if enrollment has any health insurance records
     */
    boolean existsByEnrollmentId(UUID enrollmentId);
    
    /**
     * Check if enrollment has START record (required for HMIS compliance)
     */
    boolean existsByEnrollmentIdAndRecordType(UUID enrollmentId, String recordType);
    
    /**
     * Find records that may need HOPWA reason
     * (no coverage and no sources, potentially for HOPWA programs)
     */
    @Query("""
        SELECT h FROM JpaHealthInsuranceEntity h 
        WHERE h.coveredByHealthInsurance = 0
        AND h.medicaid = FALSE AND h.medicare = FALSE AND h.schip = FALSE
        AND h.vhaMedicalServices = FALSE AND h.employerProvided = FALSE
        AND h.cobra = FALSE AND h.privatePay = FALSE 
        AND h.stateAdultHealthInsurance = FALSE AND h.indianHealthService = FALSE
        AND h.otherInsurance = FALSE
        AND h.hopwaNoInsuranceReason IS NULL
        """)
    List<JpaHealthInsuranceEntity> findRecordsPotentiallyNeedingHopwaReason();
    
    /**
     * Find records for HMIS CSV export within date range
     */
    @Query("""
        SELECT h FROM JpaHealthInsuranceEntity h 
        WHERE h.informationDate >= :startDate 
        AND h.informationDate <= :endDate 
        ORDER BY h.enrollmentId, h.informationDate DESC
        """)
    List<JpaHealthInsuranceEntity> findForHmisExport(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Get latest health insurance record per enrollment for reporting
     */
    @Query("""
        SELECT h FROM JpaHealthInsuranceEntity h 
        WHERE (h.enrollmentId, h.informationDate) IN (
            SELECT h2.enrollmentId, MAX(h2.informationDate) 
            FROM JpaHealthInsuranceEntity h2 
            WHERE h2.informationDate >= :startDate 
            AND h2.informationDate <= :endDate 
            GROUP BY h2.enrollmentId
        )
        ORDER BY h.enrollmentId
        """)
    List<JpaHealthInsuranceEntity> findLatestRecordPerEnrollment(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Find records with government insurance coverage
     */
    @Query("""
        SELECT h FROM JpaHealthInsuranceEntity h 
        WHERE (h.medicaid = TRUE OR h.medicare = TRUE OR h.schip = TRUE OR 
               h.vhaMedicalServices = TRUE OR h.stateAdultHealthInsurance = TRUE OR 
               h.indianHealthService = TRUE)
        AND h.informationDate >= :startDate 
        AND h.informationDate <= :endDate
        """)
    List<JpaHealthInsuranceEntity> findWithGovernmentInsurance(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Find records with private insurance coverage
     */
    @Query("""
        SELECT h FROM JpaHealthInsuranceEntity h 
        WHERE (h.employerProvided = TRUE OR h.cobra = TRUE OR 
               h.privatePay = TRUE OR h.otherInsurance = TRUE)
        AND h.informationDate >= :startDate 
        AND h.informationDate <= :endDate
        """)
    List<JpaHealthInsuranceEntity> findWithPrivateInsurance(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
}
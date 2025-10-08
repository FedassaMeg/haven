package org.haven.programenrollment.infrastructure.persistence;

import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.haven.clientprofile.domain.ClientId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository for Income and Benefits Records
 * Supports HMIS-compliant lifecycle queries
 */
@Repository
public interface JpaIncomeBenefitsRepository extends JpaRepository<JpaIncomeBenefitsEntity, UUID> {
    
    /**
     * Find all income records for a specific enrollment
     */
    List<JpaIncomeBenefitsEntity> findByEnrollmentIdOrderByInformationDateDesc(UUID enrollmentId);
    
    /**
     * Find all income records for a specific client
     */
    List<JpaIncomeBenefitsEntity> findByClientIdOrderByInformationDateDesc(UUID clientId);
    
    /**
     * Find the most recent income record for an enrollment
     */
    Optional<JpaIncomeBenefitsEntity> findFirstByEnrollmentIdOrderByInformationDateDesc(UUID enrollmentId);
    
    /**
     * Find income records by type (START, UPDATE, ANNUAL, EXIT, MINOR18)
     */
    List<JpaIncomeBenefitsEntity> findByEnrollmentIdAndRecordTypeOrderByInformationDateDesc(
        UUID enrollmentId, String recordType);
    
    /**
     * Find the START record for an enrollment
     */
    Optional<JpaIncomeBenefitsEntity> findByEnrollmentIdAndRecordType(UUID enrollmentId, String recordType);
    
    /**
     * Find income records within a date range for reporting
     */
    @Query("SELECT i FROM JpaIncomeBenefitsEntity i WHERE i.informationDate >= :startDate AND i.informationDate <= :endDate ORDER BY i.informationDate DESC")
    List<JpaIncomeBenefitsEntity> findByInformationDateBetween(
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate);
    
    /**
     * Find enrollments that need annual assessments
     * (enrollments that started more than a year ago and don't have annual assessment in last year)
     */
    @Query("""
        SELECT DISTINCT i1.enrollmentId FROM JpaIncomeBenefitsEntity i1 
        WHERE i1.recordType = 'START' 
        AND i1.informationDate <= :oneYearAgo
        AND NOT EXISTS (
            SELECT i2 FROM JpaIncomeBenefitsEntity i2 
            WHERE i2.enrollmentId = i1.enrollmentId 
            AND i2.recordType = 'ANNUAL' 
            AND i2.informationDate >= :oneYearAgo
        )
        AND NOT EXISTS (
            SELECT i3 FROM JpaIncomeBenefitsEntity i3 
            WHERE i3.enrollmentId = i1.enrollmentId 
            AND i3.recordType = 'EXIT'
        )
        """)
    List<UUID> findEnrollmentIdsNeedingAnnualAssessment(@Param("oneYearAgo") LocalDate oneYearAgo);
    
    /**
     * Find clients who turned 18 and need income assessment
     * (clients who have minor status but no income record after 18th birthday)
     */
    @Query("""
        SELECT DISTINCT i.clientId FROM JpaIncomeBenefitsEntity i 
        WHERE i.clientId IN :clientIds
        AND NOT EXISTS (
            SELECT i2 FROM JpaIncomeBenefitsEntity i2 
            WHERE i2.clientId = i.clientId 
            AND i2.recordType = 'MINOR18'
            AND i2.informationDate >= :since18thBirthday
        )
        """)
    List<UUID> findClientsNeedingMinor18Assessment(
        @Param("clientIds") List<UUID> clientIds,
        @Param("since18thBirthday") LocalDate since18thBirthday);
    
    /**
     * Check if enrollment has any income records
     */
    boolean existsByEnrollmentId(UUID enrollmentId);
    
    /**
     * Check if enrollment has START record (required for HMIS compliance)
     */
    boolean existsByEnrollmentIdAndRecordType(UUID enrollmentId, String recordType);
    
    /**
     * Find enrollments with income but no sources specified (data quality issue)
     */
    @Query("""
        SELECT i FROM JpaIncomeBenefitsEntity i 
        WHERE i.incomeFromAnySource = 1
        AND i.totalMonthlyIncome > 0
        AND (i.earnedIncome IS NULL OR i.earnedIncome = 99)
        AND (i.unemploymentIncome IS NULL OR i.unemploymentIncome = 99)
        AND (i.supplementalSecurityIncome IS NULL OR i.supplementalSecurityIncome = 99)
        AND (i.socialSecurityDisabilityIncome IS NULL OR i.socialSecurityDisabilityIncome = 99)
        AND (i.vaDisabilityServiceConnected IS NULL OR i.vaDisabilityServiceConnected = 99)
        AND (i.vaDisabilityNonServiceConnected IS NULL OR i.vaDisabilityNonServiceConnected = 99)
        AND (i.privateDisabilityIncome IS NULL OR i.privateDisabilityIncome = 99)
        AND (i.workersCompensation IS NULL OR i.workersCompensation = 99)
        AND (i.tanfIncome IS NULL OR i.tanfIncome = 99)
        AND (i.generalAssistance IS NULL OR i.generalAssistance = 99)
        AND (i.socialSecurityRetirement IS NULL OR i.socialSecurityRetirement = 99)
        AND (i.pensionFromFormerJob IS NULL OR i.pensionFromFormerJob = 99)
        AND (i.childSupport IS NULL OR i.childSupport = 99)
        AND (i.alimony IS NULL OR i.alimony = 99)
        AND (i.otherIncomeSource IS NULL OR i.otherIncomeSource = 99)
        """)
    List<JpaIncomeBenefitsEntity> findIncomeWithoutSources();
    
    /**
     * Find records for HMIS CSV export within date range
     */
    @Query("""
        SELECT i FROM JpaIncomeBenefitsEntity i 
        WHERE i.informationDate >= :startDate 
        AND i.informationDate <= :endDate 
        ORDER BY i.enrollmentId, i.informationDate DESC
        """)
    List<JpaIncomeBenefitsEntity> findForHmisExport(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    /**
     * Get latest income record per enrollment for reporting
     */
    @Query("""
        SELECT i FROM JpaIncomeBenefitsEntity i 
        WHERE (i.enrollmentId, i.informationDate) IN (
            SELECT i2.enrollmentId, MAX(i2.informationDate) 
            FROM JpaIncomeBenefitsEntity i2 
            WHERE i2.informationDate >= :startDate 
            AND i2.informationDate <= :endDate 
            GROUP BY i2.enrollmentId
        )
        ORDER BY i.enrollmentId
        """)
    List<JpaIncomeBenefitsEntity> findLatestRecordPerEnrollment(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
}
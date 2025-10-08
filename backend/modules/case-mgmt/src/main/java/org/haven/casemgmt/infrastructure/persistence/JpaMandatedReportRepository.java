package org.haven.casemgmt.infrastructure.persistence;

import org.haven.casemgmt.domain.mandatedreport.ReportStatus;
import org.haven.casemgmt.domain.mandatedreport.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository interface for MandatedReport entities
 */
@Repository
public interface JpaMandatedReportRepository extends JpaRepository<JpaMandatedReportEntity, UUID> {
    
    /**
     * Find reports by case ID
     */
    List<JpaMandatedReportEntity> findByCaseId(UUID caseId);
    
    /**
     * Find reports by client ID
     */
    List<JpaMandatedReportEntity> findByClientId(UUID clientId);
    
    /**
     * Find reports by status
     */
    List<JpaMandatedReportEntity> findByStatus(ReportStatus status);
    
    /**
     * Find reports by type
     */
    List<JpaMandatedReportEntity> findByReportType(ReportType reportType);
    
    /**
     * Find report by report number
     */
    Optional<JpaMandatedReportEntity> findByReportNumber(String reportNumber);
    
    /**
     * Find reports that are overdue (DRAFT status past filing deadline)
     */
    @Query("SELECT r FROM JpaMandatedReportEntity r WHERE r.status = :draftStatus AND r.filingDeadline < :currentTime")
    List<JpaMandatedReportEntity> findOverdueReports(@Param("draftStatus") ReportStatus draftStatus, 
                                                    @Param("currentTime") Instant currentTime);
    
    /**
     * Find emergency reports
     */
    List<JpaMandatedReportEntity> findByIsEmergencyReportTrue();
    
    /**
     * Find reports by status and created by user
     */
    List<JpaMandatedReportEntity> findByStatusAndCreatedByUserId(ReportStatus status, UUID createdByUserId);
    
    /**
     * Find reports filed within date range
     */
    @Query("SELECT r FROM JpaMandatedReportEntity r WHERE r.filedAt BETWEEN :startDate AND :endDate")
    List<JpaMandatedReportEntity> findReportsFiledBetween(@Param("startDate") Instant startDate, 
                                                         @Param("endDate") Instant endDate);
    
    /**
     * Find reports approaching deadline (within specified hours)
     */
    @Query("SELECT r FROM JpaMandatedReportEntity r WHERE r.status = :draftStatus AND r.filingDeadline BETWEEN :currentTime AND :warningTime")
    List<JpaMandatedReportEntity> findReportsApproachingDeadline(@Param("draftStatus") ReportStatus draftStatus,
                                                                @Param("currentTime") Instant currentTime,
                                                                @Param("warningTime") Instant warningTime);
}
package org.haven.reportingmetadata.infrastructure.persistence;

import org.haven.reportingmetadata.domain.HudSpecificationType;
import org.haven.reportingmetadata.domain.ReportSpecification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for ReportSpecification
 */
@Repository
public interface JpaReportSpecificationRepository extends JpaRepository<ReportSpecification, UUID> {

    /**
     * Find specifications by report type
     */
    List<ReportSpecification> findByReportType(HudSpecificationType reportType);

    /**
     * Find specification by section identifier
     */
    Optional<ReportSpecification> findBySectionIdentifier(String sectionIdentifier);

    /**
     * Find active specifications on date
     */
    @Query("SELECT s FROM ReportSpecification s WHERE s.effectiveFrom <= :date AND (s.effectiveTo IS NULL OR s.effectiveTo >= :date)")
    List<ReportSpecification> findActiveOn(@Param("date") LocalDate date);

    /**
     * Find currently active specifications
     */
    @Query("SELECT s FROM ReportSpecification s WHERE s.effectiveFrom <= CURRENT_DATE AND (s.effectiveTo IS NULL OR s.effectiveTo >= CURRENT_DATE)")
    List<ReportSpecification> findCurrentlyActive();

    /**
     * Find active specifications by type
     */
    @Query("SELECT s FROM ReportSpecification s WHERE s.reportType = :type AND s.effectiveFrom <= CURRENT_DATE AND (s.effectiveTo IS NULL OR s.effectiveTo >= CURRENT_DATE)")
    List<ReportSpecification> findActiveByType(@Param("type") HudSpecificationType type);

    /**
     * Find specifications requiring VAWA consent checks
     */
    List<ReportSpecification> findByRequiresVawaConsentCheckTrue();
}

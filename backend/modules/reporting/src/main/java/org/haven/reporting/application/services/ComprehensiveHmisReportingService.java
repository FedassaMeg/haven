package org.haven.reporting.application.services;

import org.haven.reporting.domain.hmis.*;
import org.haven.programenrollment.domain.*;
import org.haven.programenrollment.infrastructure.persistence.*;
import org.haven.programenrollment.application.services.*;
import org.haven.shared.vo.hmis.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprehensive HMIS Reporting Service using real domain data
 * Replaces legacy approach with full integration of new HMIS data elements
 * Provides accurate reporting for all HMIS FY2024 data requirements
 */
@Service
@Transactional(readOnly = true)
public class ComprehensiveHmisReportingService {
    
    private final JpaProgramEnrollmentRepository enrollmentRepository;
    private final DisabilityLifecycleService disabilityService;
    private final DvLifecycleService dvService;
    private final CurrentLivingSituationService clsService;
    private final DateOfEngagementService engagementService;
    private final BedNightService bedNightService;
    
    public ComprehensiveHmisReportingService(
            JpaProgramEnrollmentRepository enrollmentRepository,
            DisabilityLifecycleService disabilityService,
            DvLifecycleService dvService,
            CurrentLivingSituationService clsService,
            DateOfEngagementService engagementService,
            BedNightService bedNightService) {
        this.enrollmentRepository = enrollmentRepository;
        this.disabilityService = disabilityService;
        this.dvService = dvService;
        this.clsService = clsService;
        this.engagementService = engagementService;
        this.bedNightService = bedNightService;
    }
    
    /**
     * Generate comprehensive disabilities report using real domain data
     */
    public List<HmisDisabilitiesProjection> generateDisabilitiesReport(
            LocalDate startDate, 
            LocalDate endDate,
            String exportId) {
        
        List<JpaProgramEnrollmentEntity> enrollmentEntities = enrollmentRepository
            .findByEntryDateBetween(startDate, endDate);
        
        List<HmisDisabilitiesProjection> projections = new ArrayList<>();
        
        for (JpaProgramEnrollmentEntity entity : enrollmentEntities) {
            ProgramEnrollment enrollment = entity.toDomainObject();
            String userId = getCurrentUserId();
            
            // Generate projections for all relevant stages
            projections.addAll(
                HmisDisabilitiesProjection.createAllStagesForEnrollment(enrollment, userId, exportId)
            );
        }
        
        return projections;
    }
    
    /**
     * Generate domestic violence report with enhanced privacy protection
     */
    public List<HmisDomesticViolenceProjection> generateDomesticViolenceReport(
            LocalDate startDate,
            LocalDate endDate,
            String exportId,
            boolean allowSensitiveExport) {
        
        List<JpaProgramEnrollmentEntity> enrollmentEntities = enrollmentRepository
            .findByEntryDateBetween(startDate, endDate);
        
        List<HmisDomesticViolenceProjection> projections = new ArrayList<>();
        
        for (JpaProgramEnrollmentEntity entity : enrollmentEntities) {
            ProgramEnrollment enrollment = entity.toDomainObject();
            String userId = getCurrentUserId();
            
            // Get DV records for all stages
            for (DataCollectionStage stage : DataCollectionStage.values()) {
                DvRecord dvRecord = enrollment.getDvRecord(stage);
                if (dvRecord != null) {
                    HmisDomesticViolenceProjection projection = 
                        HmisDomesticViolenceProjection.fromDvRecordWithPrivacyProtection(
                            enrollment.getId().value().toString(),
                            HmisPersonalId.of(enrollment.getClientId().value().toString()),
                            dvRecord,
                            userId,
                            exportId,
                            allowSensitiveExport
                        );
                    projections.add(projection);
                }
            }
        }
        
        return projections;
    }
    
    /**
     * Generate comprehensive enrollment report with all new data elements
     */
    public ComprehensiveEnrollmentReport generateComprehensiveEnrollmentReport(
            UUID enrollmentId,
            String exportId) {
        
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        String userId = getCurrentUserId();
        
        // Generate all projections for this enrollment
        List<HmisDisabilitiesProjection> disabilitiesProjections = 
            HmisDisabilitiesProjection.createAllStagesForEnrollment(enrollment, userId, exportId);
        
        List<HmisDomesticViolenceProjection> dvProjections = 
            HmisDomesticViolenceProjection.createAllStagesForEnrollment(enrollment, userId, exportId);
        
        // Get current living situation data
        List<CurrentLivingSituation> clsRecords = clsService
            .getCurrentLivingSituationsForEnrollment(enrollmentId);
        
        // Get engagement data
        DateOfEngagement engagementRecord = engagementService.getDateOfEngagement(enrollmentId);
        
        // Get bed night data
        List<BedNight> bedNights = bedNightService.getBedNightsForEnrollment(enrollmentId);
        
        // Calculate summary statistics
        EnrollmentSummaryStatistics summaryStats = calculateSummaryStatistics(
            enrollment, disabilitiesProjections, dvProjections, clsRecords, bedNights);
        
        return new ComprehensiveEnrollmentReport(
            enrollment.getId().value(),
            enrollment.getClientId().value(),
            enrollment.getEntryDate(),
            enrollment.getExitDate(),
            disabilitiesProjections,
            dvProjections,
            clsRecords,
            engagementRecord,
            bedNights,
            summaryStats
        );
    }
    
    /**
     * Generate HMIS data quality report using real domain data
     */
    public HmisDataQualityReport generateDataQualityReport(
            LocalDate startDate,
            LocalDate endDate) {
        
        List<JpaProgramEnrollmentEntity> enrollmentEntities = enrollmentRepository
            .findByEntryDateBetween(startDate, endDate);
        
        DataQualityStatistics stats = new DataQualityStatistics();
        List<DataQualityIssue> issues = new ArrayList<>();
        
        for (JpaProgramEnrollmentEntity entity : enrollmentEntities) {
            ProgramEnrollment enrollment = entity.toDomainObject();
            UUID enrollmentId = enrollment.getId().value();
            
            stats.totalEnrollments++;
            
            // Check disability data quality
            assessDisabilityDataQuality(enrollmentId, enrollment, stats, issues);
            
            // Check DV data quality
            assessDvDataQuality(enrollmentId, enrollment, stats, issues);
            
            // Check engagement data quality
            assessEngagementDataQuality(enrollmentId, enrollment, stats, issues);
            
            // Check current living situation data quality
            assessCurrentLivingSituationDataQuality(enrollmentId, enrollment, stats, issues);
        }
        
        return new HmisDataQualityReport(
            startDate,
            endDate,
            stats,
            issues,
            LocalDateTime.now()
        );
    }
    
    /**
     * Generate high-risk client report focusing on DV and safety concerns
     */
    public HighRiskClientReport generateHighRiskClientReport(LocalDate reportDate) {
        // Get high-risk clients currently fleeing DV
        List<UUID> highRiskClients = dvService.getHighRiskClientsRequiringIntervention();
        
        // Get enrollments requiring safety protocols
        List<UUID> enrollmentsRequiringSafetyProtocols = dvService.getEnrollmentsRequiringSafetyProtocols();
        
        // Get enrollments missing recent contact (engagement gaps)
        List<UUID> enrollmentsMissingRecentContact = clsService
            .findEnrollmentsMissingRecentContact(30); // 30 days
        
        List<HighRiskClientSummary> clientSummaries = new ArrayList<>();
        
        for (UUID clientId : highRiskClients) {
            // Get all enrollments for this client
            List<JpaProgramEnrollmentEntity> clientEnrollments = enrollmentRepository
                .findByClientId(clientId);
            
            for (JpaProgramEnrollmentEntity entity : clientEnrollments) {
                ProgramEnrollment enrollment = entity.toDomainObject();
                
                // Get DV safety assessment
                DvSafetyAssessment safetyAssessment = dvService.getDvSafetyAssessment(enrollment.getId().value());
                
                // Get current living situation status
                CurrentLivingSituationStatus clsStatus = clsService
                    .getCurrentLivingSituationStatus(enrollment.getId().value());
                
                HighRiskClientSummary summary = new HighRiskClientSummary(
                    clientId,
                    enrollment.getId().value(),
                    safetyAssessment,
                    clsStatus,
                    enrollmentsRequiringSafetyProtocols.contains(enrollment.getId().value()),
                    enrollmentsMissingRecentContact.contains(enrollment.getId().value())
                );
                
                clientSummaries.add(summary);
            }
        }
        
        return new HighRiskClientReport(
            reportDate,
            highRiskClients.size(),
            enrollmentsRequiringSafetyProtocols.size(),
            enrollmentsMissingRecentContact.size(),
            clientSummaries
        );
    }
    
    /**
     * Generate bed utilization report for emergency shelters
     */
    public BedUtilizationReport generateBedUtilizationReport(
            LocalDate startDate,
            LocalDate endDate,
            int totalBedCapacity) {
        
        BedNightService.BedUtilizationMetrics metrics = bedNightService
            .calculateBedUtilizationMetrics(startDate, endDate);
        
        // Get daily occupancy rates
        List<DailyOccupancyRate> dailyRates = new ArrayList<>();
        LocalDate currentDate = startDate;
        
        while (!currentDate.isAfter(endDate)) {
            BedNightService.BedAvailabilityReport dailyReport = bedNightService
                .getBedAvailabilityReport(currentDate, totalBedCapacity);
            
            dailyRates.add(new DailyOccupancyRate(
                currentDate,
                dailyReport.bedsUsed(),
                dailyReport.occupancyRate()
            ));
            
            currentDate = currentDate.plusDays(1);
        }
        
        // Find enrollments with irregular bed patterns
        List<UUID> enrollmentsWithGaps = bedNightService.findEnrollmentsWithBedNightGaps(7);
        
        return new BedUtilizationReport(
            startDate,
            endDate,
            totalBedCapacity,
            metrics.totalBedNights(),
            metrics.averageDailyUtilization(),
            dailyRates,
            enrollmentsWithGaps
        );
    }
    
    private void assessDisabilityDataQuality(UUID enrollmentId, ProgramEnrollment enrollment,
                                           DataQualityStatistics stats, List<DataQualityIssue> issues) {
        // Check if enrollment meets disability compliance
        boolean meetsCompliance = disabilityService.meetsDisabilityDataCompliance(enrollmentId);
        
        if (meetsCompliance) {
            stats.enrollmentsWithCompleteDisabilityData++;
        } else {
            issues.add(new DataQualityIssue(
                enrollmentId,
                enrollment.getClientId().value(),
                "DISABILITY_COMPLIANCE",
                "Enrollment does not meet disability data compliance requirements"
            ));
        }
        
        // Check for missing PROJECT_START records
        for (DisabilityKind kind : DisabilityKind.values()) {
            if (!disabilityService.hasProjectStartRecord(enrollmentId, kind)) {
                issues.add(new DataQualityIssue(
                    enrollmentId,
                    enrollment.getClientId().value(),
                    "MISSING_DISABILITY_START",
                    "Missing PROJECT_START record for " + kind.name() + " disability"
                ));
            }
        }
    }
    
    private void assessDvDataQuality(UUID enrollmentId, ProgramEnrollment enrollment,
                                   DataQualityStatistics stats, List<DataQualityIssue> issues) {
        boolean meetsCompliance = dvService.meetsDvDataCompliance(enrollmentId);
        
        if (meetsCompliance) {
            stats.enrollmentsWithCompleteDvData++;
        } else {
            issues.add(new DataQualityIssue(
                enrollmentId,
                enrollment.getClientId().value(),
                "DV_COMPLIANCE",
                "Enrollment does not meet DV data compliance requirements"
            ));
        }
        
        // Check for data quality issues in DV records
        List<DvRecord> dvIssues = dvService.findRecordsWithDataQualityIssues();
        for (DvRecord record : dvIssues) {
            if (record.getEnrollmentId().value().equals(enrollmentId)) {
                issues.add(new DataQualityIssue(
                    enrollmentId,
                    enrollment.getClientId().value(),
                    "DV_DATA_QUALITY",
                    "DV record has data quality issues: " + record.getRecordId()
                ));
            }
        }
    }
    
    private void assessEngagementDataQuality(UUID enrollmentId, ProgramEnrollment enrollment,
                                           DataQualityStatistics stats, List<DataQualityIssue> issues) {
        boolean hasEngagement = engagementService.hasDateOfEngagement(enrollmentId);
        boolean meetsCompliance = engagementService.meetsEngagementCompliance(enrollmentId);
        
        if (hasEngagement) {
            stats.enrollmentsWithEngagementDate++;
        }
        
        if (!meetsCompliance) {
            issues.add(new DataQualityIssue(
                enrollmentId,
                enrollment.getClientId().value(),
                "ENGAGEMENT_COMPLIANCE",
                "Enrollment does not meet engagement date compliance requirements"
            ));
        }
    }
    
    private void assessCurrentLivingSituationDataQuality(UUID enrollmentId, ProgramEnrollment enrollment,
                                                       DataQualityStatistics stats, List<DataQualityIssue> issues) {
        boolean hasRecentContact = clsService.hasRecentStreetContact(enrollmentId, 90);
        
        if (hasRecentContact) {
            stats.enrollmentsWithRecentClsContact++;
        }
        
        // Check for enrollments with engagement gaps
        if (!clsService.hasRecentStreetContact(enrollmentId, 30)) {
            issues.add(new DataQualityIssue(
                enrollmentId,
                enrollment.getClientId().value(),
                "CLS_ENGAGEMENT_GAP",
                "Enrollment has not had recent current living situation contact"
            ));
        }
    }
    
    private EnrollmentSummaryStatistics calculateSummaryStatistics(
            ProgramEnrollment enrollment,
            List<HmisDisabilitiesProjection> disabilities,
            List<HmisDomesticViolenceProjection> dvProjections,
            List<CurrentLivingSituation> clsRecords,
            List<BedNight> bedNights) {
        
        // Count disabling conditions
        int disablingConditionsCount = (int) disabilities.stream()
            .filter(HmisDisabilitiesProjection::hasDisablingConditions)
            .count();
        
        // Assess DV risk level
        HmisDomesticViolenceProjection.RiskLevel maxRiskLevel = dvProjections.stream()
            .map(HmisDomesticViolenceProjection::assessRiskLevel)
            .max(Enum::compareTo)
            .orElse(HmisDomesticViolenceProjection.RiskLevel.NONE);
        
        // Count unsheltered contacts
        long unshelteredContacts = clsRecords.stream()
            .filter(CurrentLivingSituation::isUnsheltered)
            .count();
        
        return new EnrollmentSummaryStatistics(
            disablingConditionsCount,
            maxRiskLevel,
            (int) unshelteredContacts,
            bedNights.size()
        );
    }
    
    private String getCurrentUserId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "SYSTEM";
    }
    
    // Value objects for reporting
    
    public record ComprehensiveEnrollmentReport(
        UUID enrollmentId,
        UUID clientId,
        LocalDate entryDate,
        LocalDate exitDate,
        List<HmisDisabilitiesProjection> disabilitiesProjections,
        List<HmisDomesticViolenceProjection> dvProjections,
        List<CurrentLivingSituation> currentLivingSituations,
        DateOfEngagement dateOfEngagement,
        List<BedNight> bedNights,
        EnrollmentSummaryStatistics summaryStats
    ) {}
    
    public record EnrollmentSummaryStatistics(
        int disablingConditionsCount,
        HmisDomesticViolenceProjection.RiskLevel dvRiskLevel,
        int unshelteredContactsCount,
        int totalBedNights
    ) {}
    
    public record HmisDataQualityReport(
        LocalDate reportStartDate,
        LocalDate reportEndDate,
        DataQualityStatistics statistics,
        List<DataQualityIssue> issues,
        LocalDateTime generatedAt
    ) {}
    
    public record DataQualityStatistics(
        int totalEnrollments,
        int enrollmentsWithCompleteDisabilityData,
        int enrollmentsWithCompleteDvData,
        int enrollmentsWithEngagementDate,
        int enrollmentsWithRecentClsContact
    ) {
        public DataQualityStatistics() {
            this(0, 0, 0, 0, 0);
        }
    }
    
    public record DataQualityIssue(
        UUID enrollmentId,
        UUID clientId,
        String issueType,
        String description
    ) {}
    
    public record HighRiskClientReport(
        LocalDate reportDate,
        int totalHighRiskClients,
        int enrollmentsRequiringSafetyProtocols,
        int enrollmentsWithEngagementGaps,
        List<HighRiskClientSummary> clientSummaries
    ) {}
    
    public record HighRiskClientSummary(
        UUID clientId,
        UUID enrollmentId,
        DvSafetyAssessment safetyAssessment,
        CurrentLivingSituationStatus livingSituationStatus,
        boolean requiresSafetyProtocols,
        boolean hasEngagementGaps
    ) {}
    
    public record BedUtilizationReport(
        LocalDate startDate,
        LocalDate endDate,
        int totalBedCapacity,
        long totalBedNightsUsed,
        double averageDailyUtilization,
        List<DailyOccupancyRate> dailyOccupancyRates,
        List<UUID> enrollmentsWithIrregularPatterns
    ) {}
    
    public record DailyOccupancyRate(
        LocalDate date,
        long bedsUsed,
        double occupancyRate
    ) {}
}
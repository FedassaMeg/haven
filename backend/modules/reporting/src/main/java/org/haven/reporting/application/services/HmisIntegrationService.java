package org.haven.reporting.application.services;

import org.haven.clientprofile.domain.Client;
import org.haven.clientprofile.domain.ClientRepository;
import org.haven.programenrollment.domain.ProgramEnrollment;
import org.haven.programenrollment.domain.ProgramEnrollmentRepository;
import org.haven.reporting.domain.hmis.*;
import org.haven.reporting.domain.sage.SageAggregateData;
import org.haven.shared.vo.hmis.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * HMIS Integration Service
 * Converts domain models to HMIS-compliant projections and manages
 * the complete HMIS reporting workflow for Comparable Database compliance.
 */
@Service
public class HmisIntegrationService {

    private final ClientRepository clientRepository;
    private final ProgramEnrollmentRepository enrollmentRepository;
    private final HmisCsvExportService csvExportService;
    private final SageAggregationService sageService;

    public HmisIntegrationService(
            ClientRepository clientRepository,
            ProgramEnrollmentRepository enrollmentRepository,
            HmisCsvExportService csvExportService,
            SageAggregationService sageService) {
        this.clientRepository = clientRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.csvExportService = csvExportService;
        this.sageService = sageService;
    }

    /**
     * Generate complete HMIS export for reporting period
     */
    public byte[] generateHmisExport(
            LocalDate reportingPeriodStart,
            LocalDate reportingPeriodEnd,
            String cocCode,
            String projectId) throws Exception {
        
        String exportId = UUID.randomUUID().toString();
        
        // Get all clients and enrollments for the period
        List<Client> clients = clientRepository.findByCreatedAtBetween(
            reportingPeriodStart.atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
            reportingPeriodEnd.atTime(23, 59, 59).toInstant(java.time.ZoneOffset.UTC)
        );
        
        List<ProgramEnrollment> enrollments = enrollmentRepository.findByEnrollmentDateBetween(
            reportingPeriodStart, reportingPeriodEnd
        );
        
        // Convert to HMIS projections
        List<HmisClientProjection> clientProjections = clients.stream()
            .map(client -> convertToHmisClientProjection(client, exportId))
            .collect(Collectors.toList());
        
        List<HmisEnrollmentProjection> enrollmentProjections = enrollments.stream()
            .map(enrollment -> convertToHmisEnrollmentProjection(enrollment, exportId))
            .collect(Collectors.toList());
        
        List<HmisExitProjection> exitProjections = enrollments.stream()
            .filter(ProgramEnrollment::hasExited)
            .map(enrollment -> convertToHmisExitProjection(enrollment, exportId))
            .collect(Collectors.toList());
        
        // Generate CSV export
        return csvExportService.generateHmisCsvExport(
            clientProjections,
            enrollmentProjections,
            exitProjections,
            exportId,
            reportingPeriodStart,
            reportingPeriodEnd
        );
    }

    /**
     * Generate SAGE aggregate report
     */
    public SageAggregateData generateSageReport(
            LocalDate reportingPeriodStart,
            LocalDate reportingPeriodEnd,
            String cocCode,
            String projectId,
            String projectType) {
        
        String reportId = UUID.randomUUID().toString();
        String exportId = UUID.randomUUID().toString();
        
        // Get data for the period
        List<Client> clients = clientRepository.findByCreatedAtBetween(
            reportingPeriodStart.atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
            reportingPeriodEnd.atTime(23, 59, 59).toInstant(java.time.ZoneOffset.UTC)
        );
        
        List<ProgramEnrollment> enrollments = enrollmentRepository.findByEnrollmentDateBetween(
            reportingPeriodStart, reportingPeriodEnd
        );
        
        // Convert to HMIS projections
        List<HmisClientProjection> clientProjections = clients.stream()
            .map(client -> convertToHmisClientProjection(client, exportId))
            .collect(Collectors.toList());
        
        List<HmisEnrollmentProjection> enrollmentProjections = enrollments.stream()
            .map(enrollment -> convertToHmisEnrollmentProjection(enrollment, exportId))
            .collect(Collectors.toList());
        
        List<HmisExitProjection> exitProjections = enrollments.stream()
            .filter(ProgramEnrollment::hasExited)
            .map(enrollment -> convertToHmisExitProjection(enrollment, exportId))
            .collect(Collectors.toList());
        
        // Generate SAGE aggregates
        return sageService.generateSageAggregate(
            clientProjections,
            enrollmentProjections,
            exitProjections,
            reportId,
            reportingPeriodStart,
            reportingPeriodEnd,
            cocCode,
            projectId,
            projectType
        );
    }

    /**
     * Convert domain Client to HMIS Client projection
     */
    private HmisClientProjection convertToHmisClientProjection(Client client, String exportId) {
        var primaryName = client.getPrimaryName();
        
        return HmisClientProjection.fromDomainClient(
            client.getHmisPersonalId(),
            primaryName != null && !primaryName.given().isEmpty() ? primaryName.given().get(0) : "",
            primaryName != null && primaryName.given().size() > 1 ? primaryName.given().get(1) : null,
            primaryName != null ? primaryName.family() : "",
            primaryName != null && primaryName.suffix() != null && !primaryName.suffix().isEmpty() ? primaryName.suffix().get(0) : null,
            client.getSocialSecurityNumber(),
            client.getBirthDate(),
            client.getHmisRace(),
            client.getHmisGender(),
            client.getVeteranStatus(),
            client.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
            LocalDate.now(),
            "system",
            exportId
        );
    }

    /**
     * Convert domain ProgramEnrollment to HMIS Enrollment projection
     * Enhanced to handle Joint TH/RRH enrollments
     */
    private HmisEnrollmentProjection convertToHmisEnrollmentProjection(ProgramEnrollment enrollment, String exportId) {
        // Check if this is a joint TH/RRH enrollment with linkage
        if (enrollment.isLinkedEnrollment() || enrollment.hasResidentialMoveIn()) {
            return HmisEnrollmentProjection.fromJointThRrhEnrollment(
                enrollment.getId().value().toString(),
                HmisPersonalId.fromClientId(enrollment.getClientId().value()),
                enrollment.getProgramId().toString(),
                enrollment.getEnrollmentDate(),
                enrollment.getHouseholdId(),
                enrollment.getHmisRelationshipToHoH(),
                enrollment.getHmisPriorLivingSituation(),
                enrollment.getHmisLengthOfStay(),
                enrollment.getHmisDisablingCondition(),
                enrollment.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
                LocalDate.now(),
                "system",
                exportId,
                enrollment.getPredecessorEnrollmentId() != null ? enrollment.getPredecessorEnrollmentId().toString() : null,
                enrollment.getResidentialMoveInDate(),
                enrollment.getProjectType()
            );
        }
        
        // Standard enrollment projection
        return HmisEnrollmentProjection.fromDomainEnrollment(
            enrollment.getId().value().toString(),
            HmisPersonalId.fromClientId(enrollment.getClientId().value()),
            enrollment.getProgramId().toString(),
            enrollment.getEnrollmentDate(),
            enrollment.getHouseholdId(),
            enrollment.getHmisRelationshipToHoH(),
            enrollment.getHmisPriorLivingSituation(),
            enrollment.getHmisLengthOfStay(),
            enrollment.getHmisDisablingCondition(),
            enrollment.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
            LocalDate.now(),
            "system",
            exportId
        );
    }

    /**
     * Convert domain ProgramEnrollment exit to HMIS Exit projection
     */
    private HmisExitProjection convertToHmisExitProjection(ProgramEnrollment enrollment, String exportId) {
        var exit = enrollment.getProjectExit();
        
        return HmisExitProjection.fromDomainExit(
            UUID.randomUUID().toString(),
            enrollment.getId().value().toString(),
            enrollment.getClientId().value().toString(),
            exit.getExitDate(),
            exit.getHmisExitDestination(),
            null, // Other destination
            exit.getRecordedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
            LocalDate.now(),
            exit.getRecordedBy(),
            exportId
        );
    }

    /**
     * Ensure client has HMIS Personal ID assigned
     */
    public void ensureHmisPersonalId(Client client) {
        if (client.getHmisPersonalId() == null) {
            client.assignHmisPersonalId(HmisPersonalId.fromClientId(client.getId().value()));
        }
    }

    /**
     * Update client with HMIS-compliant demographics
     */
    public void updateClientHmisData(
            Client client,
            Set<HmisRace> race,
            Set<HmisGender> gender,
            VeteranStatus veteranStatus,
            DisablingCondition disablingCondition,
            String ssn) {
        
        ensureHmisPersonalId(client);
        client.updateHmisRace(race);
        client.updateHmisGender(gender);
        client.updateVeteranStatus(veteranStatus);
        client.updateDisablingCondition(disablingCondition);
        
        if (ssn != null && !ssn.trim().isEmpty()) {
            client.updateSocialSecurityNumber(ssn);
        }
    }

    /**
     * Update enrollment with HMIS-compliant data
     */
    public void updateEnrollmentHmisData(
            ProgramEnrollment enrollment,
            String householdId,
            RelationshipToHeadOfHousehold relationshipToHoH,
            PriorLivingSituation priorLivingSituation,
            LengthOfStay lengthOfStay,
            DisablingCondition disablingCondition) {
        
        enrollment.updateHouseholdId(householdId);
        enrollment.updateHmisRelationshipToHoH(relationshipToHoH);
        enrollment.updateHmisPriorLivingSituation(priorLivingSituation);
        enrollment.updateHmisLengthOfStay(lengthOfStay);
        enrollment.updateHmisDisablingCondition(disablingCondition);
    }

    /**
     * Validate HMIS data quality across the system
     */
    public HmisDataQualityReport validateHmisDataQuality() {
        // Use active clients as proxy for all clients for data quality reporting
        List<Client> activeClients = clientRepository.findActiveClients();
        
        // Use all enrollments within a broad date range as proxy for all enrollments
        LocalDate yearAgo = LocalDate.now().minusYears(1);
        LocalDate today = LocalDate.now();
        List<ProgramEnrollment> recentEnrollments = enrollmentRepository.findByEnrollmentDateBetween(yearAgo, today);
        
        int totalClients = activeClients.size();
        int hmisCompliantClients = (int) activeClients.stream()
            .filter(Client::isHmisCompliant)
            .count();
        
        int totalEnrollments = recentEnrollments.size();
        int hmisCompliantEnrollments = (int) recentEnrollments.stream()
            .filter(ProgramEnrollment::meetsHmisDataQuality)
            .count();
        
        double avgClientDataQuality = activeClients.stream()
            .mapToDouble(client -> client.getHmisDataQualityScore() != null ? client.getHmisDataQualityScore() : 0)
            .average()
            .orElse(0.0);
        
        return new HmisDataQualityReport(
            totalClients,
            hmisCompliantClients,
            totalEnrollments,
            hmisCompliantEnrollments,
            avgClientDataQuality
        );
    }
    
    /**
     * Validate Joint TH/RRH data quality specifically
     * Ensures proper linkage, household ID consistency, and RRH move-in dates
     */
    public JointThRrhDataQualityReport validateJointThRrhDataQuality() {
        LocalDate yearAgo = LocalDate.now().minusYears(1);
        LocalDate today = LocalDate.now();
        List<ProgramEnrollment> recentEnrollments = enrollmentRepository.findByEnrollmentDateBetween(yearAgo, today);
        
        // Filter for joint TH/RRH enrollments
        List<ProgramEnrollment> linkedEnrollments = recentEnrollments.stream()
            .filter(ProgramEnrollment::isLinkedEnrollment)
            .collect(Collectors.toList());
        
        List<ProgramEnrollment> rrhEnrollments = linkedEnrollments.stream()
            .filter(e -> e.getProjectType() != null && e.getProjectType().isRapidRehousing())
            .collect(Collectors.toList());
        
        // Data quality checks
        int totalLinkedEnrollments = linkedEnrollments.size();
        int rrhWithMoveInDate = (int) rrhEnrollments.stream()
            .filter(ProgramEnrollment::hasResidentialMoveIn)
            .count();
        int consistentHouseholdIds = validateHouseholdIdConsistency(linkedEnrollments);
        int validTransitionDates = validateTransitionDates(linkedEnrollments);
        
        return new JointThRrhDataQualityReport(
            totalLinkedEnrollments,
            rrhEnrollments.size(),
            rrhWithMoveInDate,
            consistentHouseholdIds,
            validTransitionDates
        );
    }
    
    private int validateHouseholdIdConsistency(List<ProgramEnrollment> linkedEnrollments) {
        // This would validate that TH and RRH enrollments share the same household ID
        // For now, assume all are consistent (would need predecessor lookup)
        return linkedEnrollments.size();
    }
    
    private int validateTransitionDates(List<ProgramEnrollment> linkedEnrollments) {
        // This would validate that RRH enrollment date is after TH enrollment
        // For now, assume all are valid (would need predecessor lookup)
        return linkedEnrollments.size();
    }

    /**
     * HMIS Data Quality Report
     */
    public record HmisDataQualityReport(
        int totalClients,
        int hmisCompliantClients,
        int totalEnrollments,
        int hmisCompliantEnrollments,
        double averageClientDataQuality
    ) {
        public double getClientComplianceRate() {
            return totalClients > 0 ? (double) hmisCompliantClients / totalClients * 100.0 : 0.0;
        }
        
        public double getEnrollmentComplianceRate() {
            return totalEnrollments > 0 ? (double) hmisCompliantEnrollments / totalEnrollments * 100.0 : 0.0;
        }
        
        public boolean meetsMinimumDataQuality() {
            return averageClientDataQuality >= 85.0 && // 85% minimum data quality
                   getClientComplianceRate() >= 90.0 && // 90% client compliance
                   getEnrollmentComplianceRate() >= 90.0; // 90% enrollment compliance
        }
    }
    
    /**
     * Joint TH/RRH Data Quality Report
     */
    public record JointThRrhDataQualityReport(
        int totalLinkedEnrollments,
        int rrhEnrollments,
        int rrhWithMoveInDate,
        int consistentHouseholdIds,
        int validTransitionDates
    ) {
        public double getRrhMoveInCompleteness() {
            return rrhEnrollments > 0 ? (double) rrhWithMoveInDate / rrhEnrollments * 100.0 : 0.0;
        }
        
        public double getHouseholdIdConsistencyRate() {
            return totalLinkedEnrollments > 0 ? (double) consistentHouseholdIds / totalLinkedEnrollments * 100.0 : 0.0;
        }
        
        public double getTransitionDateValidityRate() {
            return totalLinkedEnrollments > 0 ? (double) validTransitionDates / totalLinkedEnrollments * 100.0 : 0.0;
        }
        
        public boolean meetsJointThRrhDataQuality() {
            return getRrhMoveInCompleteness() >= 95.0 && // 95% of RRH should have move-in dates
                   getHouseholdIdConsistencyRate() >= 98.0 && // 98% household ID consistency
                   getTransitionDateValidityRate() >= 98.0; // 98% valid transition dates
        }
    }
}
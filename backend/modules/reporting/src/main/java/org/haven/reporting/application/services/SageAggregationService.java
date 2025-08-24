package org.haven.reporting.application.services;

import org.haven.reporting.domain.hmis.*;
import org.haven.reporting.domain.sage.SageAggregateData;
import org.haven.shared.vo.hmis.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SAGE Aggregation Service
 * Generates aggregate data for HMIS reporting without exposing PII.
 * Used for Annual Homeless Assessment Report (AHAR) and other federal reports.
 */
@Service
public class SageAggregationService {

    /**
     * Generate SAGE aggregate data from HMIS projections
     */
    public SageAggregateData generateSageAggregate(
            List<HmisClientProjection> clients,
            List<HmisEnrollmentProjection> enrollments,
            List<HmisExitProjection> exits,
            String reportId,
            LocalDate reportingPeriodStart,
            LocalDate reportingPeriodEnd,
            String cocCode,
            String projectId,
            String projectType) {
        
        // Filter data for reporting period
        List<HmisEnrollmentProjection> periodEnrollments = filterEnrollmentsByPeriod(
            enrollments, reportingPeriodStart, reportingPeriodEnd);
        
        List<HmisExitProjection> periodExits = filterExitsByPeriod(
            exits, reportingPeriodStart, reportingPeriodEnd);
        
        // Get unique clients for the period
        Set<String> uniqueClientIds = periodEnrollments.stream()
            .map(e -> e.personalId().value())
            .collect(Collectors.toSet());
        
        List<HmisClientProjection> periodClients = clients.stream()
            .filter(c -> uniqueClientIds.contains(c.personalId().value()))
            .toList();

        return new SageAggregateData(
            reportId,
            reportingPeriodStart,
            reportingPeriodEnd,
            cocCode,
            projectId,
            projectType,
            
            // Client Demographics
            periodClients.size(),
            calculateAdults(periodClients),
            calculateChildren(periodClients),
            calculateUnaccompaniedYouth(periodClients, periodEnrollments),
            calculateFamilies(periodEnrollments),
            calculateIndividuals(periodEnrollments),
            
            // Demographics Breakdowns
            calculateGenderBreakdown(periodClients),
            calculateRaceBreakdown(periodClients),
            calculateEthnicityBreakdown(periodClients),
            
            // Veteran Status
            calculateVeterans(periodClients),
            calculateVeteransWithDisability(periodClients),
            
            // Disability Status
            calculateClientsWithDisability(periodEnrollments),
            
            // Chronic Homelessness
            calculateChronicallyHomelessIndividuals(periodEnrollments),
            calculateChronicallyHomelessFamilies(periodEnrollments),
            calculateChronicallyHomelessPersons(periodEnrollments),
            
            // Housing Outcomes
            calculateExitsToPermanentHousing(periodExits),
            calculateExitsToTemporaryHousing(periodExits),
            calculateReturnsToHomelessness(periodExits),
            
            // Length of Stay
            calculateLengthOfStayBreakdown(periodEnrollments),
            
            // Prior Living Situation
            calculatePriorLivingSituationBreakdown(periodEnrollments),
            
            // Exit Destinations
            calculateExitDestinationBreakdown(periodExits),
            
            // Service Utilization
            periodEnrollments.size(), // Total service episodes
            Map.of("Enrollment", periodEnrollments.size()), // Service type breakdown
            
            // Income and Benefits (placeholder - would need additional data)
            null,
            null,
            
            // Data Quality
            calculateNameDataQuality(periodClients),
            calculateSsnDataQuality(periodClients),
            calculateDobDataQuality(periodClients),
            calculateDestinationDataQuality(periodExits),
            
            // Report Metadata
            LocalDate.now(),
            "system",
            "1.0"
        );
    }

    private List<HmisEnrollmentProjection> filterEnrollmentsByPeriod(
            List<HmisEnrollmentProjection> enrollments, 
            LocalDate start, LocalDate end) {
        return enrollments.stream()
            .filter(e -> !e.entryDate().isBefore(start) && !e.entryDate().isAfter(end))
            .toList();
    }

    private List<HmisExitProjection> filterExitsByPeriod(
            List<HmisExitProjection> exits, 
            LocalDate start, LocalDate end) {
        return exits.stream()
            .filter(e -> e.exitDate() != null && 
                        !e.exitDate().isBefore(start) && 
                        !e.exitDate().isAfter(end))
            .toList();
    }

    private Integer calculateAdults(List<HmisClientProjection> clients) {
        return (int) clients.stream()
            .filter(this::isAdult)
            .count();
    }

    private Integer calculateChildren(List<HmisClientProjection> clients) {
        return (int) clients.stream()
            .filter(this::isChild)
            .count();
    }

    private Integer calculateUnaccompaniedYouth(List<HmisClientProjection> clients, 
                                               List<HmisEnrollmentProjection> enrollments) {
        // Youth aged 18-24 who are head of household
        Set<String> headOfHouseholdIds = enrollments.stream()
            .filter(e -> e.relationshipToHoH().isHeadOfHousehold())
            .map(e -> e.personalId().value())
            .collect(Collectors.toSet());
        
        return (int) clients.stream()
            .filter(c -> headOfHouseholdIds.contains(c.personalId().value()))
            .filter(this::isYouth)
            .count();
    }

    private Integer calculateFamilies(List<HmisEnrollmentProjection> enrollments) {
        // Count households with children or multiple adults
        Map<String, List<HmisEnrollmentProjection>> households = enrollments.stream()
            .collect(Collectors.groupingBy(HmisEnrollmentProjection::householdId));
        
        return (int) households.values().stream()
            .filter(household -> household.size() > 1 || 
                    household.stream().anyMatch(e -> e.relationshipToHoH().isFamilyMember()))
            .count();
    }

    private Integer calculateIndividuals(List<HmisEnrollmentProjection> enrollments) {
        Map<String, List<HmisEnrollmentProjection>> households = enrollments.stream()
            .collect(Collectors.groupingBy(HmisEnrollmentProjection::householdId));
        
        return (int) households.values().stream()
            .filter(household -> household.size() == 1 &&
                    household.stream().allMatch(e -> e.relationshipToHoH().isHeadOfHousehold()))
            .count();
    }

    private Map<String, Integer> calculateGenderBreakdown(List<HmisClientProjection> clients) {
        Map<String, Integer> breakdown = new HashMap<>();
        
        for (HmisClientProjection client : clients) {
            for (HmisGender gender : client.gender()) {
                breakdown.merge(gender.name(), 1, Integer::sum);
            }
        }
        
        return breakdown;
    }

    private Map<String, Integer> calculateRaceBreakdown(List<HmisClientProjection> clients) {
        Map<String, Integer> breakdown = new HashMap<>();
        
        for (HmisClientProjection client : clients) {
            for (HmisRace race : client.race()) {
                breakdown.merge(race.name(), 1, Integer::sum);
            }
        }
        
        return breakdown;
    }

    private Map<String, Integer> calculateEthnicityBreakdown(List<HmisClientProjection> clients) {
        // Simplified - would need ethnicity field in client projection
        return Map.of("DATA_NOT_COLLECTED", clients.size());
    }

    private Integer calculateVeterans(List<HmisClientProjection> clients) {
        return (int) clients.stream()
            .filter(c -> c.veteranStatus().isVeteran())
            .count();
    }

    private Integer calculateVeteransWithDisability(List<HmisClientProjection> clients) {
        // Would need disability information cross-referenced with veteran status
        return 0; // Placeholder
    }

    private Integer calculateClientsWithDisability(List<HmisEnrollmentProjection> enrollments) {
        return (int) enrollments.stream()
            .filter(e -> e.disablingCondition().hasDisablingCondition())
            .count();
    }

    private Integer calculateChronicallyHomelessIndividuals(List<HmisEnrollmentProjection> enrollments) {
        return (int) enrollments.stream()
            .filter(HmisEnrollmentProjection::isChronicallyHomeless)
            .filter(e -> !e.isFamilyHousehold())
            .count();
    }

    private Integer calculateChronicallyHomelessFamilies(List<HmisEnrollmentProjection> enrollments) {
        return (int) enrollments.stream()
            .filter(HmisEnrollmentProjection::isChronicallyHomeless)
            .filter(HmisEnrollmentProjection::isFamilyHousehold)
            .collect(Collectors.groupingBy(HmisEnrollmentProjection::householdId))
            .size();
    }

    private Integer calculateChronicallyHomelessPersons(List<HmisEnrollmentProjection> enrollments) {
        return (int) enrollments.stream()
            .filter(HmisEnrollmentProjection::isChronicallyHomeless)
            .count();
    }

    private Integer calculateExitsToPermanentHousing(List<HmisExitProjection> exits) {
        return (int) exits.stream()
            .filter(HmisExitProjection::isSuccessfulOutcome)
            .count();
    }

    private Integer calculateExitsToTemporaryHousing(List<HmisExitProjection> exits) {
        return (int) exits.stream()
            .filter(e -> e.destination().isTemporaryDestination())
            .count();
    }

    private Integer calculateReturnsToHomelessness(List<HmisExitProjection> exits) {
        return (int) exits.stream()
            .filter(HmisExitProjection::isReturnToHomelessness)
            .count();
    }

    private Map<String, Integer> calculateLengthOfStayBreakdown(List<HmisEnrollmentProjection> enrollments) {
        return enrollments.stream()
            .collect(Collectors.groupingBy(
                e -> e.lengthOfStayPriorToDiEntry().name(),
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
    }

    private Map<String, Integer> calculatePriorLivingSituationBreakdown(List<HmisEnrollmentProjection> enrollments) {
        return enrollments.stream()
            .collect(Collectors.groupingBy(
                e -> e.priorLivingSituation().name(),
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
    }

    private Map<String, Integer> calculateExitDestinationBreakdown(List<HmisExitProjection> exits) {
        return exits.stream()
            .collect(Collectors.groupingBy(
                HmisExitProjection::getDestinationCategory,
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
    }

    private Double calculateNameDataQuality(List<HmisClientProjection> clients) {
        if (clients.isEmpty()) return null;
        
        long goodQuality = clients.stream()
            .filter(c -> c.nameDataQuality() != null && c.nameDataQuality() == 1)
            .count();
        
        return (double) goodQuality / clients.size() * 100.0;
    }

    private Double calculateSsnDataQuality(List<HmisClientProjection> clients) {
        if (clients.isEmpty()) return null;
        
        long goodQuality = clients.stream()
            .filter(c -> c.ssnDataQuality() != null && c.ssnDataQuality() == 1)
            .count();
        
        return (double) goodQuality / clients.size() * 100.0;
    }

    private Double calculateDobDataQuality(List<HmisClientProjection> clients) {
        if (clients.isEmpty()) return null;
        
        long goodQuality = clients.stream()
            .filter(c -> c.dobDataQuality() != null && c.dobDataQuality() == 1)
            .count();
        
        return (double) goodQuality / clients.size() * 100.0;
    }

    private Double calculateDestinationDataQuality(List<HmisExitProjection> exits) {
        if (exits.isEmpty()) return null;
        
        long goodQuality = exits.stream()
            .filter(e -> e.destination() != ProjectExitDestination.DATA_NOT_COLLECTED)
            .count();
        
        return (double) goodQuality / exits.size() * 100.0;
    }

    private boolean isAdult(HmisClientProjection client) {
        if (client.dateOfBirth() == null) return true; // Assume adult if unknown
        return LocalDate.now().minusYears(18).isAfter(client.dateOfBirth());
    }

    private boolean isChild(HmisClientProjection client) {
        if (client.dateOfBirth() == null) return false;
        return LocalDate.now().minusYears(18).isBefore(client.dateOfBirth()) ||
               LocalDate.now().minusYears(18).equals(client.dateOfBirth());
    }

    private boolean isYouth(HmisClientProjection client) {
        if (client.dateOfBirth() == null) return false;
        LocalDate now = LocalDate.now();
        return now.minusYears(25).isBefore(client.dateOfBirth()) &&
               now.minusYears(18).isAfter(client.dateOfBirth());
    }
}
package org.haven.servicedelivery.application.services;

import org.haven.servicedelivery.domain.ServiceEpisode;
import org.haven.shared.vo.services.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service Reporting Service
 * Domain service - framework independent
 * Generates statistics and reports for service delivery
 */
public class ServiceReportingService {

    /**
     * Generate comprehensive service statistics
     */
    public ServiceStatistics generateStatistics(List<ServiceEpisode> services) {
        if (services.isEmpty()) {
            return ServiceStatistics.empty();
        }

        int totalServices = services.size();
        int completedServices = (int) services.stream().filter(ServiceEpisode::isCompleted).count();
        int inProgressServices = (int) services.stream().filter(ServiceEpisode::isInProgress).count();
        int confidentialServices = (int) services.stream().filter(ServiceEpisode::isConfidential).count();
        int courtOrderedServices = (int) services.stream().filter(ServiceEpisode::isCourtOrdered).count();
        int servicesRequiringFollowUp = (int) services.stream().filter(ServiceEpisode::requiresFollowUp).count();
        int overdueServices = (int) services.stream().filter(ServiceEpisode::isOverdue).count();

        Double totalHours = services.stream()
            .mapToDouble(s -> s.getActualBillableHours() != null ? s.getActualBillableHours() : 0.0)
            .sum();

        Double averageDuration = services.stream()
            .filter(s -> s.getActualDurationMinutes() != null)
            .mapToInt(ServiceEpisode::getActualDurationMinutes)
            .average()
            .orElse(0.0);

        Map<ServiceType, Integer> serviceTypeBreakdown = services.stream()
            .collect(Collectors.groupingBy(
                ServiceEpisode::getServiceType,
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));

        Map<ServiceCategory, Integer> serviceCategoryBreakdown = services.stream()
            .collect(Collectors.groupingBy(
                ServiceEpisode::getServiceCategory,
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));

        Map<ServiceDeliveryMode, Integer> deliveryModeBreakdown = services.stream()
            .collect(Collectors.groupingBy(
                ServiceEpisode::getDeliveryMode,
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));

        Map<FundingSource, Integer> fundingSourceBreakdown = services.stream()
            .collect(Collectors.groupingBy(
                ServiceEpisode::getPrimaryFundingSource,
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));

        Map<String, Integer> programBreakdown = services.stream()
            .collect(Collectors.groupingBy(
                ServiceEpisode::getProgramName,
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));

        return new ServiceStatistics(
            totalServices,
            completedServices,
            inProgressServices,
            confidentialServices,
            courtOrderedServices,
            servicesRequiringFollowUp,
            overdueServices,
            totalHours,
            averageDuration,
            serviceTypeBreakdown,
            serviceCategoryBreakdown,
            deliveryModeBreakdown,
            fundingSourceBreakdown,
            programBreakdown
        );
    }

    /**
     * Generate client service summary
     */
    public ClientServiceSummary generateClientSummary(List<ServiceEpisode> clientServices) {
        if (clientServices.isEmpty()) {
            return new ClientServiceSummary(0, 0.0, null, null, List.of());
        }

        int totalServices = clientServices.size();
        Double totalHours = clientServices.stream()
            .mapToDouble(s -> s.getActualBillableHours() != null ? s.getActualBillableHours() : 0.0)
            .sum();

        LocalDate firstServiceDate = clientServices.stream()
            .map(ServiceEpisode::getServiceDate)
            .min(LocalDate::compareTo)
            .orElse(null);

        LocalDate lastServiceDate = clientServices.stream()
            .map(ServiceEpisode::getServiceDate)
            .max(LocalDate::compareTo)
            .orElse(null);

        List<String> serviceTypes = clientServices.stream()
            .map(s -> s.getServiceType().getDescription())
            .distinct()
            .sorted()
            .toList();

        return new ClientServiceSummary(
            totalServices,
            totalHours,
            firstServiceDate,
            lastServiceDate,
            serviceTypes
        );
    }

    /**
     * Generate program utilization report
     */
    public ProgramUtilizationReport generateProgramUtilization(String programId, 
                                                              List<ServiceEpisode> programServices,
                                                              LocalDate reportPeriodStart,
                                                              LocalDate reportPeriodEnd) {
        
        var periodServices = programServices.stream()
            .filter(s -> !s.getServiceDate().isBefore(reportPeriodStart) && 
                        !s.getServiceDate().isAfter(reportPeriodEnd))
            .toList();

        int uniqueClients = (int) periodServices.stream()
            .map(s -> s.getClientId().value())
            .distinct()
            .count();

        int totalServiceHours = periodServices.stream()
            .filter(s -> s.getActualDurationMinutes() != null)
            .mapToInt(ServiceEpisode::getActualDurationMinutes)
            .sum() / 60;

        Double averageServicesPerClient = uniqueClients > 0 ? 
            (double) periodServices.size() / uniqueClients : 0.0;

        Map<ServiceType, Integer> serviceDistribution = periodServices.stream()
            .collect(Collectors.groupingBy(
                ServiceEpisode::getServiceType,
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));

        return new ProgramUtilizationReport(
            programId,
            reportPeriodStart,
            reportPeriodEnd,
            uniqueClients,
            periodServices.size(),
            totalServiceHours,
            averageServicesPerClient,
            serviceDistribution
        );
    }

    /**
     * Generate funder compliance report
     */
    public FunderComplianceReport generateFunderCompliance(FundingSource fundingSource,
                                                          List<ServiceEpisode> fundedServices) {
        
        int totalServices = fundedServices.size();
        int compliantServices = (int) fundedServices.stream()
            .filter(this::isCompliantService)
            .count();

        Double complianceRate = totalServices > 0 ? 
            (double) compliantServices / totalServices * 100.0 : 0.0;

        List<String> complianceIssues = fundedServices.stream()
            .filter(s -> !isCompliantService(s))
            .map(this::getComplianceIssue)
            .distinct()
            .toList();

        boolean requiresOutcomeTracking = fundingSource.requiresOutcomeTracking();
        int servicesWithOutcomes = (int) fundedServices.stream()
            .filter(s -> s.getServiceOutcome() != null && !s.getServiceOutcome().trim().isEmpty())
            .count();

        Double outcomeTrackingRate = requiresOutcomeTracking && totalServices > 0 ? 
            (double) servicesWithOutcomes / totalServices * 100.0 : null;

        return new FunderComplianceReport(
            fundingSource,
            totalServices,
            compliantServices,
            complianceRate,
            complianceIssues,
            requiresOutcomeTracking,
            servicesWithOutcomes,
            outcomeTrackingRate
        );
    }

    private boolean isCompliantService(ServiceEpisode service) {
        // Check basic compliance requirements
        if (service.getServiceType() == null || service.getDeliveryMode() == null) {
            return false;
        }

        // Check funding source compatibility
        if (!service.getPrimaryFundingSource().isCompatibleWith(service.getServiceType())) {
            return false;
        }

        // Check confidentiality requirements
        if (service.getServiceType().requiresConfidentialHandling() && 
            !service.getPrimaryFundingSource().allowsConfidentialServices()) {
            return false;
        }

        // Check duration reasonableness
        if (service.getActualDurationMinutes() != null) {
            var typicalRange = service.getServiceType().getTypicalDuration();
            if (service.getActualDurationMinutes() < typicalRange.minMinutes() / 2 ||
                service.getActualDurationMinutes() > typicalRange.maxMinutes() * 3) {
                return false;
            }
        }

        return true;
    }

    private String getComplianceIssue(ServiceEpisode service) {
        if (service.getServiceType() == null) return "Missing service type";
        if (service.getDeliveryMode() == null) return "Missing delivery mode";
        
        if (!service.getPrimaryFundingSource().isCompatibleWith(service.getServiceType())) {
            return "Service type incompatible with funding source";
        }
        
        if (service.getServiceType().requiresConfidentialHandling() && 
            !service.getPrimaryFundingSource().allowsConfidentialServices()) {
            return "Confidential service with non-confidential funding";
        }
        
        if (service.getActualDurationMinutes() != null) {
            var typicalRange = service.getServiceType().getTypicalDuration();
            if (service.getActualDurationMinutes() < typicalRange.minMinutes() / 2) {
                return "Service duration too short";
            }
            if (service.getActualDurationMinutes() > typicalRange.maxMinutes() * 3) {
                return "Service duration too long";
            }
        }
        
        return "Unknown compliance issue";
    }

    // Supporting records
    public record ServiceStatistics(
        int totalServices,
        int completedServices,
        int inProgressServices,
        int confidentialServices,
        int courtOrderedServices,
        int servicesRequiringFollowUp,
        int overdueServices,
        Double totalHours,
        Double averageDurationMinutes,
        Map<ServiceType, Integer> serviceTypeBreakdown,
        Map<ServiceCategory, Integer> serviceCategoryBreakdown,
        Map<ServiceDeliveryMode, Integer> deliveryModeBreakdown,
        Map<FundingSource, Integer> fundingSourceBreakdown,
        Map<String, Integer> programBreakdown
    ) {
        public static ServiceStatistics empty() {
            return new ServiceStatistics(0, 0, 0, 0, 0, 0, 0, 0.0, 0.0, 
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        }
        
        public Double getCompletionRate() {
            return totalServices > 0 ? (double) completedServices / totalServices * 100.0 : 0.0;
        }
    }

    public record ClientServiceSummary(
        int totalServices,
        Double totalHours,
        LocalDate firstServiceDate,
        LocalDate lastServiceDate,
        List<String> serviceTypes
    ) {}

    public record ProgramUtilizationReport(
        String programId,
        LocalDate reportPeriodStart,
        LocalDate reportPeriodEnd,
        int uniqueClients,
        int totalServices,
        int totalServiceHours,
        Double averageServicesPerClient,
        Map<ServiceType, Integer> serviceDistribution
    ) {}

    public record FunderComplianceReport(
        FundingSource fundingSource,
        int totalServices,
        int compliantServices,
        Double complianceRate,
        List<String> complianceIssues,
        boolean requiresOutcomeTracking,
        int servicesWithOutcomes,
        Double outcomeTrackingRate
    ) {}
}
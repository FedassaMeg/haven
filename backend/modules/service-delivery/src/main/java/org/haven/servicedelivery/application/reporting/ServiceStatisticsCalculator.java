package org.haven.servicedelivery.application.reporting;

import org.haven.servicedelivery.application.queries.ServiceEpisodeDTO;
import org.haven.servicedelivery.domain.ServiceStatistics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service Statistics Calculator
 * Pure business logic for calculating service statistics
 * No dependencies on repositories or infrastructure
 */
public class ServiceStatisticsCalculator {

    /**
     * Generate statistics from service data
     */
    public ServiceStatistics calculateStatistics(
            LocalDate startDate,
            LocalDate endDate,
            List<ServiceEpisodeDTO> services) {

        int totalServices = services.size();
        int completedServices = (int) services.stream().filter(ServiceEpisodeDTO::isCompleted).count();
        int pendingServices = (int) services.stream().filter(s -> !s.isCompleted()).count();
        int cancelledServices = 0; // TODO: implement when status available

        double avgDuration = services.stream()
            .filter(s -> s.actualDurationMinutes() != null)
            .mapToInt(ServiceEpisodeDTO::actualDurationMinutes)
            .average()
            .orElse(0.0);

        BigDecimal averageDuration = BigDecimal.valueOf(avgDuration);

        int uniqueClients = (int) services.stream()
            .map(ServiceEpisodeDTO::clientId)
            .distinct()
            .count();

        int confidentialServices = (int) services.stream()
            .filter(ServiceEpisodeDTO::isConfidential)
            .count();

        int courtOrderedServices = (int) services.stream()
            .filter(ServiceEpisodeDTO::isCourtOrdered)
            .count();

        int servicesRequiringFollowUp = (int) services.stream()
            .filter(ServiceEpisodeDTO::requiresFollowUp)
            .count();

        return new ServiceStatistics(
            startDate,
            endDate,
            totalServices,
            completedServices,
            pendingServices,
            cancelledServices,
            averageDuration,
            uniqueClients,
            confidentialServices,
            courtOrderedServices,
            servicesRequiringFollowUp
        );
    }

    /**
     * Calculate statistics for multiple date ranges
     */
    public List<ServiceStatistics> calculateTrendStatistics(
            LocalDate overallStartDate,
            LocalDate overallEndDate,
            List<ServiceEpisodeDTO> allServices,
            TrendPeriod period) {

        var result = new java.util.ArrayList<ServiceStatistics>();
        LocalDate currentStart = overallStartDate;

        while (!currentStart.isAfter(overallEndDate)) {
            LocalDate currentEnd = calculatePeriodEnd(currentStart, period);
            if (currentEnd.isAfter(overallEndDate)) {
                currentEnd = overallEndDate;
            }

            final LocalDate periodStart = currentStart;
            final LocalDate periodEnd = currentEnd;

            var periodServices = allServices.stream()
                .filter(s -> !s.serviceDate().isBefore(periodStart) && !s.serviceDate().isAfter(periodEnd))
                .toList();

            result.add(calculateStatistics(periodStart, periodEnd, periodServices));

            currentStart = currentEnd.plusDays(1);
        }

        return result;
    }

    private LocalDate calculatePeriodEnd(LocalDate start, TrendPeriod period) {
        return switch (period) {
            case WEEKLY -> start.plusWeeks(1).minusDays(1);
            case MONTHLY -> start.plusMonths(1).minusDays(1);
            case QUARTERLY -> start.plusMonths(3).minusDays(1);
            case YEARLY -> start.plusYears(1).minusDays(1);
        };
    }

    public enum TrendPeriod {
        WEEKLY, MONTHLY, QUARTERLY, YEARLY
    }
}

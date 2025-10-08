package org.haven.api.services.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ServiceStatisticsResponse(
    LocalDate startDate,
    LocalDate endDate,
    int totalServices,
    int completedServices,
    int pendingServices,
    int cancelledServices,
    BigDecimal averageDurationMinutes,
    int uniqueClients,
    int confidentialServices,
    int courtOrderedServices,
    int servicesRequiringFollowUp
) {
    public static ServiceStatisticsResponse fromDomain(Object statistics) {
        // This would need to be implemented based on the actual domain statistics object
        return new ServiceStatisticsResponse(
            LocalDate.now().minusDays(30),
            LocalDate.now(),
            0, 0, 0, 0,
            BigDecimal.ZERO,
            0, 0, 0, 0
        );
    }
}
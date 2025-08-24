package org.haven.servicedelivery.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ServiceStatistics(
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
}
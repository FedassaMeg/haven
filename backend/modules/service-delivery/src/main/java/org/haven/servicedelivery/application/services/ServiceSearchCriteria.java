package org.haven.servicedelivery.application.services;

import org.haven.shared.vo.services.ServiceType;
import org.haven.shared.vo.services.ServiceCategory;
import org.haven.shared.vo.services.ServiceDeliveryMode;

import java.time.LocalDate;
import java.util.UUID;

public record ServiceSearchCriteria(
    UUID clientId,
    String enrollmentId,
    String programId,
    ServiceType serviceType,
    ServiceCategory serviceCategory,
    ServiceDeliveryMode deliveryMode,
    LocalDate startDate,
    LocalDate endDate,
    String providerId,
    boolean confidentialOnly,
    boolean courtOrderedOnly,
    boolean followUpRequired
) {
}
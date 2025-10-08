package org.haven.servicedelivery.application.queries;

import org.haven.shared.vo.services.ServiceCategory;
import org.haven.shared.vo.services.ServiceDeliveryMode;
import org.haven.shared.vo.services.ServiceType;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Data Transfer Object for Service Episode
 * Used for read operations to decouple queries from domain model
 */
public record ServiceEpisodeDTO(
    UUID episodeId,
    UUID clientId,
    String enrollmentId,
    String programId,
    String programName,
    ServiceType serviceType,
    ServiceCategory serviceCategory,
    ServiceDeliveryMode deliveryMode,
    LocalDate serviceDate,
    Integer plannedDurationMinutes,
    Integer actualDurationMinutes,
    String primaryProviderId,
    String primaryProviderName,
    boolean isConfidential,
    boolean isCourtOrdered,
    boolean requiresFollowUp,
    boolean isCompleted,
    boolean isBillable
) {}

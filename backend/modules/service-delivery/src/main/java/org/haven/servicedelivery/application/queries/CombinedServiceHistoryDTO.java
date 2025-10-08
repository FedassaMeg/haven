package org.haven.servicedelivery.application.queries;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for combined service history across enrollment chain
 */
public record CombinedServiceHistoryDTO(
    UUID rootEnrollmentId,
    List<UUID> enrollmentChain,
    int totalServiceCount,
    LocalDate firstServiceDate,
    LocalDate lastServiceDate,
    List<EnrollmentServiceSummaryDTO> enrollmentSummaries,
    List<ServiceEpisodeDTO> allServices
) {}

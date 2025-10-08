package org.haven.servicedelivery.application.queries;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for enrollment service summary
 */
public record EnrollmentServiceSummaryDTO(
    UUID enrollmentId,
    int serviceCount,
    LocalDate firstServiceDate,
    LocalDate lastServiceDate,
    List<ServiceEpisodeDTO> services
) {}

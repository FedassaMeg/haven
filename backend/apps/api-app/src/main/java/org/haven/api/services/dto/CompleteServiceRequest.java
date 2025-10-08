package org.haven.api.services.dto;

import org.haven.servicedelivery.domain.ServiceEpisode.ServiceCompletionStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CompleteServiceRequest(
    @NotNull
    LocalDateTime endTime,
    
    @Size(max = 1000)
    String outcome,
    
    @NotNull
    ServiceCompletionStatus status,
    
    @Size(max = 2000)
    String notes
) {
}
package org.haven.api.services.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record StartServiceRequest(
    @NotNull
    LocalDateTime startTime,
    
    @Size(max = 255)
    String location
) {
}
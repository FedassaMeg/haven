package org.haven.api.households.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record RemoveHouseholdMemberRequest(
    @NotNull(message = "Effective date is required")
    LocalDate effectiveDate,
    
    @NotBlank(message = "Recorded by is required")
    String recordedBy,
    
    @NotBlank(message = "Reason is required")
    String reason
) {}
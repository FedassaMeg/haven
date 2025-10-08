package org.haven.api.households.dto;

import org.haven.clientprofile.domain.HouseholdComposition;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.UUID;

public record CreateHouseholdCompositionRequest(
    @NotNull(message = "Head of household ID is required")
    UUID headOfHouseholdId,
    
    @NotNull(message = "Effective date is required")
    LocalDate effectiveDate,
    
    @NotNull(message = "Household type is required")
    HouseholdComposition.HouseholdType householdType,
    
    @NotBlank(message = "Recorded by is required")
    String recordedBy,
    
    String notes
) {}
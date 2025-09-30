package org.haven.clientprofile.application.commands;

import org.haven.clientprofile.domain.ClientId;
import org.haven.clientprofile.domain.HouseholdComposition;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record CreateHouseholdCompositionCmd(
    @NotNull(message = "Head of household ID is required")
    ClientId headOfHouseholdId,
    
    @NotNull(message = "Effective date is required")
    LocalDate effectiveDate,
    
    @NotNull(message = "Household type is required")
    HouseholdComposition.HouseholdType householdType,
    
    @NotBlank(message = "Recorded by is required")
    String recordedBy,
    
    String notes
) {}
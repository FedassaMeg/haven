package org.haven.clientprofile.application.commands;

import org.haven.clientprofile.domain.ClientId;
import org.haven.clientprofile.domain.HouseholdCompositionId;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record RemoveHouseholdMemberCmd(
    @NotNull(message = "Household composition ID is required")
    HouseholdCompositionId compositionId,
    
    @NotNull(message = "Member ID is required")
    ClientId memberId,
    
    @NotNull(message = "Effective date is required")
    LocalDate effectiveDate,
    
    @NotBlank(message = "Recorded by is required")
    String recordedBy,
    
    @NotBlank(message = "Reason is required")
    String reason
) {}
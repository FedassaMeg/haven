package org.haven.clientprofile.application.commands;

import org.haven.clientprofile.domain.ClientId;
import org.haven.clientprofile.domain.HouseholdCompositionId;
import org.haven.shared.vo.CodeableConcept;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;

import java.time.LocalDate;

public record AddHouseholdMemberToCompositionCmd(
    @NotNull(message = "Household composition ID is required")
    HouseholdCompositionId compositionId,
    
    @NotNull(message = "Member ID is required")
    ClientId memberId,
    
    @NotNull(message = "Relationship is required")
    @Valid
    CodeableConcept relationship,
    
    @NotNull(message = "Effective from date is required")
    LocalDate effectiveFrom,
    
    LocalDate effectiveTo,
    
    @NotBlank(message = "Recorded by is required")
    String recordedBy,
    
    String reason
) {}
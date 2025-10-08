package org.haven.clientprofile.application.commands;

import org.haven.clientprofile.domain.ClientId;
import org.haven.clientprofile.domain.HouseholdCompositionId;
import org.haven.shared.vo.CodeableConcept;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;

import java.time.LocalDate;

public record UpdateMemberRelationshipCmd(
    @NotNull(message = "Household composition ID is required")
    HouseholdCompositionId compositionId,
    
    @NotNull(message = "Member ID is required")
    ClientId memberId,
    
    @NotNull(message = "New relationship is required")
    @Valid
    CodeableConcept newRelationship,
    
    @NotNull(message = "Effective date is required")
    LocalDate effectiveDate,
    
    @NotBlank(message = "Recorded by is required")
    String recordedBy,
    
    @NotBlank(message = "Reason is required")
    String reason
) {}
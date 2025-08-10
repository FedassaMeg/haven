package org.haven.clientprofile.application.commands;

import org.haven.clientprofile.domain.ClientId;
import org.haven.clientprofile.domain.HouseholdMemberId;
import org.haven.shared.vo.CodeableConcept;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

public record AddHouseholdMemberCmd(
    @NotNull(message = "Client ID is required")
    ClientId clientId,
    
    @NotNull(message = "Member ID is required")
    HouseholdMemberId memberId,
    
    @NotNull(message = "Relationship is required")
    @Valid
    CodeableConcept relationship
) {}
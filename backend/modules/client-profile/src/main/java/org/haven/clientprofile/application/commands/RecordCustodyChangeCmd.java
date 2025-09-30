package org.haven.clientprofile.application.commands;

import org.haven.clientprofile.domain.ClientId;
import org.haven.clientprofile.domain.HouseholdCompositionId;
import org.haven.shared.vo.CodeableConcept;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;

import java.time.LocalDate;

public record RecordCustodyChangeCmd(
    @NotNull(message = "Household composition ID is required")
    HouseholdCompositionId compositionId,
    
    @NotNull(message = "Child ID is required")
    ClientId childId,
    
    @NotNull(message = "New custody relationship is required")
    @Valid
    CodeableConcept newCustodyRelationship,
    
    @NotNull(message = "Effective date is required")
    LocalDate effectiveDate,
    
    @NotBlank(message = "Court order reference is required")
    String courtOrder,
    
    @NotBlank(message = "Recorded by is required")
    String recordedBy
) {}
package org.haven.api.households.dto;

import org.haven.shared.vo.CodeableConcept;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.UUID;

public record RecordCustodyChangeRequest(
    @NotNull(message = "Child ID is required")
    UUID childId,
    
    @NotNull(message = "New custody relationship is required")
    CodeableConcept newCustodyRelationship,
    
    @NotNull(message = "Effective date is required")
    LocalDate effectiveDate,
    
    @NotBlank(message = "Court order reference is required")
    String courtOrder,
    
    @NotBlank(message = "Recorded by is required")
    String recordedBy
) {}
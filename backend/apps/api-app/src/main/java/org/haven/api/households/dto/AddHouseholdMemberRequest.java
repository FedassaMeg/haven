package org.haven.api.households.dto;

import org.haven.shared.vo.CodeableConcept;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.UUID;

public record AddHouseholdMemberRequest(
    @NotNull(message = "Member ID is required")
    UUID memberId,
    
    @NotNull(message = "Relationship is required")
    CodeableConcept relationship,
    
    @NotNull(message = "Effective from date is required")
    LocalDate effectiveFrom,
    
    LocalDate effectiveTo,
    
    @NotBlank(message = "Recorded by is required")
    String recordedBy,
    
    String reason
) {}
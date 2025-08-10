package org.haven.casemgmt.application.commands;

import org.haven.clientprofile.domain.ClientId;
import org.haven.shared.vo.CodeableConcept;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OpenCaseCmd(
    @NotNull(message = "Client ID is required")
    ClientId clientId,
    
    @NotNull(message = "Case type is required")
    CodeableConcept caseType,
    
    CodeableConcept priority,
    
    @NotBlank(message = "Description is required")
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description
) {}
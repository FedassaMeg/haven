package org.haven.casemgmt.application.commands;

import org.haven.casemgmt.domain.CaseId;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CloseCaseCmd(
    @NotNull(message = "Case ID is required")
    CaseId caseId,
    
    @NotBlank(message = "Reason is required")
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    String reason
) {}
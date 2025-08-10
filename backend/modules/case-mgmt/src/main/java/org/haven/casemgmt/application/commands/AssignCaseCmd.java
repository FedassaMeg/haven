package org.haven.casemgmt.application.commands;

import org.haven.casemgmt.domain.CaseId;
import org.haven.shared.vo.CodeableConcept;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record AssignCaseCmd(
    @NotNull(message = "Case ID is required")
    CaseId caseId,
    
    @NotBlank(message = "Assignee ID is required")
    String assigneeId,
    
    @NotNull(message = "Role is required")
    CodeableConcept role
) {}
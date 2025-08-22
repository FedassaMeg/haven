package org.haven.casemgmt.application.commands;

import org.haven.casemgmt.domain.CaseId;
import org.haven.casemgmt.domain.CaseAssignment.AssignmentType;
import org.haven.shared.vo.CodeableConcept;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record AssignCaseCmd(
    @NotNull(message = "Case ID is required")
    CaseId caseId,
    
    @NotBlank(message = "Assignee ID is required")
    String assigneeId,
    
    @NotBlank(message = "Assignee name is required")
    String assigneeName,
    
    @NotNull(message = "Role is required")
    CodeableConcept role,
    
    @NotNull(message = "Assignment type is required")
    AssignmentType assignmentType,
    
    String reason,
    
    @NotBlank(message = "Assigned by is required")
    String assignedBy
) {}
package org.haven.casemgmt.application.queries;

import jakarta.validation.constraints.NotBlank;

public record GetCasesByAssigneeQuery(
    @NotBlank(message = "Assignee ID is required")
    String assigneeId
) {}
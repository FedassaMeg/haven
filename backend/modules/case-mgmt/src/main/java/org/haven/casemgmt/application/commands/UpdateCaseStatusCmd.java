package org.haven.casemgmt.application.commands;

import org.haven.casemgmt.domain.CaseId;
import org.haven.casemgmt.domain.CaseRecord.CaseStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateCaseStatusCmd(
    @NotNull(message = "Case ID is required")
    CaseId caseId,
    
    @NotNull(message = "New status is required")
    CaseStatus newStatus
) {}
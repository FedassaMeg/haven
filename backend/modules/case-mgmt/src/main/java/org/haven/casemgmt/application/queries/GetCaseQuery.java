package org.haven.casemgmt.application.queries;

import org.haven.casemgmt.domain.CaseId;
import jakarta.validation.constraints.NotNull;

public record GetCaseQuery(
    @NotNull(message = "Case ID is required")
    CaseId caseId
) {}
package org.haven.casemgmt.application.queries;

import org.haven.clientprofile.domain.ClientId;
import jakarta.validation.constraints.NotNull;

public record GetCasesByClientQuery(
    @NotNull(message = "Client ID is required")
    ClientId clientId
) {}
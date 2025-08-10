package org.haven.clientprofile.application.queries;

import org.haven.clientprofile.domain.ClientId;
import jakarta.validation.constraints.NotNull;

public record GetClientQuery(
    @NotNull(message = "Client ID is required")
    ClientId clientId
) {}

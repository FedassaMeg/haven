package org.haven.clientprofile.application.commands;

import org.haven.clientprofile.domain.ClientId;
import org.haven.shared.vo.ContactPoint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

public record AddClientTelecomCmd(
    @NotNull(message = "Client ID is required")
    ClientId clientId,
    
    @NotNull(message = "Contact point is required")
    @Valid
    ContactPoint telecom
) {}
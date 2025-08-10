package org.haven.clientprofile.application.commands;

import org.haven.clientprofile.domain.ClientId;
import org.haven.shared.vo.Address;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

public record AddClientAddressCmd(
    @NotNull(message = "Client ID is required")
    ClientId clientId,
    
    @NotNull(message = "Address is required")
    @Valid
    Address address
) {}
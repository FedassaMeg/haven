package org.haven.api.financialledger.dto;

import jakarta.validation.constraints.NotBlank;

public record CloseLedgerRequest(
    @NotBlank String reason
) {}
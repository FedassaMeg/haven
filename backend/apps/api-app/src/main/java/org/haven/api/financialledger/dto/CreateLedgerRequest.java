package org.haven.api.financialledger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateLedgerRequest(
    @NotNull UUID clientId,
    @NotNull UUID enrollmentId,
    @NotNull UUID householdId,
    @NotBlank String ledgerName,
    boolean isVawaProtected
) {}
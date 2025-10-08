package org.haven.api.financialledger.dto;

import org.haven.financialassistance.domain.ledger.ArrearsType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RecordArrearsRequest(
    @NotBlank String arrearsId,
    @NotNull @Positive BigDecimal amount,
    @NotNull ArrearsType arrearsType,
    @NotBlank String payeeId,
    @NotBlank String payeeName,
    @NotNull LocalDate periodStart,
    @NotNull LocalDate periodEnd
) {}
package org.haven.api.financialledger.dto;

import org.haven.financialassistance.domain.ledger.PaymentSubtype;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RecordPaymentRequest(
    @NotBlank String paymentId,
    @NotBlank String assistanceId,
    @NotNull @Positive BigDecimal amount,
    String fundingSourceCode,
    String hudCategoryCode,
    @NotNull PaymentSubtype subtype,
    @NotBlank String payeeId,
    @NotBlank String payeeName,
    @NotNull LocalDate paymentDate,
    LocalDate periodStart,
    LocalDate periodEnd
) {}
package org.haven.api.financialledger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RecordDepositRequest(
    @NotBlank String depositId,
    @NotNull @Positive BigDecimal amount,
    @NotBlank String fundingSourceCode,
    @NotBlank String depositSource,
    @NotNull LocalDate depositDate
) {}
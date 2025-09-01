package org.haven.housingassistance.application.commands;

import org.haven.housingassistance.domain.HousingAssistance.AssistancePaymentSubtype;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AuthorizePaymentCmd(
    @NotNull(message = "Housing assistance ID is required")
    UUID housingAssistanceId,
    
    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
    BigDecimal amount,
    
    @NotNull(message = "Payment date is required")
    LocalDate paymentDate,
    
    @NotBlank(message = "Payment type is required")
    String paymentType,
    
    @NotNull(message = "Payment subtype is required")
    AssistancePaymentSubtype subtype,
    
    // Required for arrears subtypes
    LocalDate periodStart,
    LocalDate periodEnd,
    
    @NotBlank(message = "Payee ID is required")
    String payeeId,
    
    @NotBlank(message = "Payee name is required")
    String payeeName,
    
    @NotBlank(message = "Authorized by is required")
    String authorizedBy
) {}
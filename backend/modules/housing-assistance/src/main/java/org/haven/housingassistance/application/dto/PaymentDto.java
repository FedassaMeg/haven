package org.haven.housingassistance.application.dto;

import org.haven.housingassistance.domain.HousingAssistance.AssistancePaymentSubtype;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentDto(
    UUID paymentId,
    UUID housingAssistanceId,
    UUID clientId,
    UUID enrollmentId,
    BigDecimal amount,
    LocalDate paymentDate,
    String paymentType,
    AssistancePaymentSubtype subtype,
    LocalDate periodStart,
    LocalDate periodEnd,
    String payeeId,
    String payeeName,
    String authorizedBy,
    String status,
    Boolean isArrears
) {
    public static PaymentDto from(org.haven.housingassistance.domain.HousingAssistance.Payment payment, 
                                  UUID housingAssistanceId, UUID clientId, UUID enrollmentId) {
        return new PaymentDto(
            payment.getPaymentId(),
            housingAssistanceId,
            clientId,
            enrollmentId,
            payment.getAmount(),
            payment.getPaymentDate(),
            payment.getPaymentType(),
            payment.getSubtype(),
            payment.getPeriodStart(),
            payment.getPeriodEnd(),
            payment.getPayeeId(),
            payment.getPayeeName(),
            payment.getAuthorizedBy(),
            payment.getStatus().name(),
            payment.getSubtype() == AssistancePaymentSubtype.RENT_ARREARS ||
            payment.getSubtype() == AssistancePaymentSubtype.UTILITY_ARREARS
        );
    }
}
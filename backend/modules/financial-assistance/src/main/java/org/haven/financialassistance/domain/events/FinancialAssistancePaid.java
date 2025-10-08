package org.haven.financialassistance.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.financialassistance.domain.FinancialAssistanceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class FinancialAssistancePaid extends DomainEvent {
    private final UUID clientId;
    private final UUID enrollmentId;
    private final FinancialAssistanceType assistanceType;
    private final BigDecimal approvedAmount;
    private final BigDecimal paidAmount;
    private final LocalDate paymentDate;
    private final String paymentMethod;
    private final String paymentReference;
    private final String checkNumber;
    private final String paidBy;
    private final UUID paidByUserId;
    private final String paymentNotes;
    private final boolean isPartialPayment;
    private final BigDecimal remainingBalance;

    public FinancialAssistancePaid(
        UUID financialAssistanceId,
        UUID clientId,
        UUID enrollmentId,
        FinancialAssistanceType assistanceType,
        BigDecimal approvedAmount,
        BigDecimal paidAmount,
        LocalDate paymentDate,
        String paymentMethod,
        String paymentReference,
        String checkNumber,
        String paidBy,
        UUID paidByUserId,
        String paymentNotes,
        boolean isPartialPayment,
        BigDecimal remainingBalance,
        Instant occurredAt
    ) {
        super(financialAssistanceId, occurredAt != null ? occurredAt : Instant.now());
        if (financialAssistanceId == null) throw new IllegalArgumentException("Financial assistance ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (enrollmentId == null) throw new IllegalArgumentException("Enrollment ID cannot be null");
        if (assistanceType == null) throw new IllegalArgumentException("Assistance type cannot be null");
        if (approvedAmount == null) throw new IllegalArgumentException("Approved amount cannot be null");
        if (paidAmount == null) throw new IllegalArgumentException("Paid amount cannot be null");
        if (paymentDate == null) throw new IllegalArgumentException("Payment date cannot be null");
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) throw new IllegalArgumentException("Payment method cannot be null or empty");
        if (paidBy == null || paidBy.trim().isEmpty()) throw new IllegalArgumentException("Paid by cannot be null or empty");

        this.clientId = clientId;
        this.enrollmentId = enrollmentId;
        this.assistanceType = assistanceType;
        this.approvedAmount = approvedAmount;
        this.paidAmount = paidAmount;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod;
        this.paymentReference = paymentReference;
        this.checkNumber = checkNumber;
        this.paidBy = paidBy;
        this.paidByUserId = paidByUserId;
        this.paymentNotes = paymentNotes;
        this.isPartialPayment = isPartialPayment;
        this.remainingBalance = remainingBalance;
    }

    // JavaBean style getters
    public UUID getFinancialAssistanceId() { return getAggregateId(); }
    public UUID getClientId() { return clientId; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public FinancialAssistanceType getAssistanceType() { return assistanceType; }
    public BigDecimal getApprovedAmount() { return approvedAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getPaymentReference() { return paymentReference; }
    public String getCheckNumber() { return checkNumber; }
    public String getPaidBy() { return paidBy; }
    public UUID getPaidByUserId() { return paidByUserId; }
    public String getPaymentNotes() { return paymentNotes; }
    public boolean getIsPartialPayment() { return isPartialPayment; }
    public BigDecimal getRemainingBalance() { return remainingBalance; }

    // Record style getters
    public UUID financialAssistanceId() { return getAggregateId(); }
    public UUID clientId() { return clientId; }
    public UUID enrollmentId() { return enrollmentId; }
    public FinancialAssistanceType assistanceType() { return assistanceType; }
    public BigDecimal approvedAmount() { return approvedAmount; }
    public BigDecimal paidAmount() { return paidAmount; }
    public LocalDate paymentDate() { return paymentDate; }
    public String paymentMethod() { return paymentMethod; }
    public String paymentReference() { return paymentReference; }
    public String checkNumber() { return checkNumber; }
    public String paidBy() { return paidBy; }
    public UUID paidByUserId() { return paidByUserId; }
    public String paymentNotes() { return paymentNotes; }
    public boolean isPartialPayment() { return isPartialPayment; }
    public BigDecimal remainingBalance() { return remainingBalance; }
}
package org.haven.financialassistance.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.financialassistance.domain.FinancialAssistanceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class FinancialAssistanceApproved extends DomainEvent {
    private final UUID clientId;
    private final UUID enrollmentId;
    private final FinancialAssistanceType assistanceType;
    private final BigDecimal requestedAmount;
    private final BigDecimal approvedAmount;
    private final String approvalReason;
    private final String approvedBy;
    private final UUID approvedByUserId;
    private final LocalDate approvalDate;
    private final LocalDate paymentDueDate;
    private final String paymentMethod;
    private final String conditions;

    public FinancialAssistanceApproved(
        UUID financialAssistanceId,
        UUID clientId,
        UUID enrollmentId,
        FinancialAssistanceType assistanceType,
        BigDecimal requestedAmount,
        BigDecimal approvedAmount,
        String approvalReason,
        String approvedBy,
        UUID approvedByUserId,
        LocalDate approvalDate,
        LocalDate paymentDueDate,
        String paymentMethod,
        String conditions,
        Instant occurredAt
    ) {
        super(financialAssistanceId, occurredAt != null ? occurredAt : Instant.now());
        if (financialAssistanceId == null) throw new IllegalArgumentException("Financial assistance ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (enrollmentId == null) throw new IllegalArgumentException("Enrollment ID cannot be null");
        if (assistanceType == null) throw new IllegalArgumentException("Assistance type cannot be null");
        if (requestedAmount == null) throw new IllegalArgumentException("Requested amount cannot be null");
        if (approvedAmount == null) throw new IllegalArgumentException("Approved amount cannot be null");
        if (approvedBy == null || approvedBy.trim().isEmpty()) throw new IllegalArgumentException("Approved by cannot be null or empty");
        if (approvalDate == null) throw new IllegalArgumentException("Approval date cannot be null");

        this.clientId = clientId;
        this.enrollmentId = enrollmentId;
        this.assistanceType = assistanceType;
        this.requestedAmount = requestedAmount;
        this.approvedAmount = approvedAmount;
        this.approvalReason = approvalReason;
        this.approvedBy = approvedBy;
        this.approvedByUserId = approvedByUserId;
        this.approvalDate = approvalDate;
        this.paymentDueDate = paymentDueDate;
        this.paymentMethod = paymentMethod;
        this.conditions = conditions;
    }

    // JavaBean style getters
    public UUID getFinancialAssistanceId() { return getAggregateId(); }
    public UUID getClientId() { return clientId; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public FinancialAssistanceType getAssistanceType() { return assistanceType; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public BigDecimal getApprovedAmount() { return approvedAmount; }
    public String getApprovalReason() { return approvalReason; }
    public String getApprovedBy() { return approvedBy; }
    public UUID getApprovedByUserId() { return approvedByUserId; }
    public LocalDate getApprovalDate() { return approvalDate; }
    public LocalDate getPaymentDueDate() { return paymentDueDate; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getConditions() { return conditions; }

    // Record style getters
    public UUID financialAssistanceId() { return getAggregateId(); }
    public UUID clientId() { return clientId; }
    public UUID enrollmentId() { return enrollmentId; }
    public FinancialAssistanceType assistanceType() { return assistanceType; }
    public BigDecimal requestedAmount() { return requestedAmount; }
    public BigDecimal approvedAmount() { return approvedAmount; }
    public String approvalReason() { return approvalReason; }
    public String approvedBy() { return approvedBy; }
    public UUID approvedByUserId() { return approvedByUserId; }
    public LocalDate approvalDate() { return approvalDate; }
    public LocalDate paymentDueDate() { return paymentDueDate; }
    public String paymentMethod() { return paymentMethod; }
    public String conditions() { return conditions; }
}
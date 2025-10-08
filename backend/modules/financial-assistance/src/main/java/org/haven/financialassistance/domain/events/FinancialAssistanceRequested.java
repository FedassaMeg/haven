package org.haven.financialassistance.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.financialassistance.domain.FinancialAssistanceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class FinancialAssistanceRequested extends DomainEvent {
    private final UUID clientId;
    private final UUID enrollmentId;
    private final FinancialAssistanceType assistanceType;
    private final BigDecimal requestedAmount;
    private final String purpose;
    private final String justification;
    private final String requestedBy;
    private final Boolean isEmergency;

    public FinancialAssistanceRequested(
        UUID financialAssistanceId,
        UUID clientId,
        UUID enrollmentId,
        FinancialAssistanceType assistanceType,
        BigDecimal requestedAmount,
        String purpose,
        String justification,
        String requestedBy,
        Boolean isEmergency,
        Instant occurredAt
    ) {
        super(financialAssistanceId, occurredAt != null ? occurredAt : Instant.now());
        if (financialAssistanceId == null) throw new IllegalArgumentException("Financial assistance ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (enrollmentId == null) throw new IllegalArgumentException("Enrollment ID cannot be null");
        if (assistanceType == null) throw new IllegalArgumentException("Assistance type cannot be null");
        if (requestedAmount == null) throw new IllegalArgumentException("Requested amount cannot be null");
        if (purpose == null) throw new IllegalArgumentException("Purpose cannot be null");
        if (requestedBy == null) throw new IllegalArgumentException("Requested by cannot be null");

        this.clientId = clientId;
        this.enrollmentId = enrollmentId;
        this.assistanceType = assistanceType;
        this.requestedAmount = requestedAmount;
        this.purpose = purpose;
        this.justification = justification;
        this.requestedBy = requestedBy;
        this.isEmergency = isEmergency != null ? isEmergency : false;
    }

    // JavaBean style getters
    public UUID getFinancialAssistanceId() { return getAggregateId(); }
    public UUID getClientId() { return clientId; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public FinancialAssistanceType getAssistanceType() { return assistanceType; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public String getPurpose() { return purpose; }
    public String getJustification() { return justification; }
    public String getRequestedBy() { return requestedBy; }
    public Boolean getIsEmergency() { return isEmergency; }

    // Record style getters
    public UUID financialAssistanceId() { return getAggregateId(); }
    public UUID clientId() { return clientId; }
    public UUID enrollmentId() { return enrollmentId; }
    public FinancialAssistanceType assistanceType() { return assistanceType; }
    public BigDecimal requestedAmount() { return requestedAmount; }
    public String purpose() { return purpose; }
    public String justification() { return justification; }
    public String requestedBy() { return requestedBy; }
    public Boolean isEmergency() { return isEmergency; }
}
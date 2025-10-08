package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class RequestSubmitted extends DomainEvent {
    private final UUID clientId;
    private final UUID caseId;
    private final UUID enrollmentId;
    private final CodeableConcept assistanceType;
    private final BigDecimal requestedAmount;
    private final String purpose;
    private final String justification;
    private final String requestedBy;
    private final boolean isEmergency;

    public RequestSubmitted(UUID requestId, UUID clientId, UUID caseId, UUID enrollmentId, CodeableConcept assistanceType, BigDecimal requestedAmount, String purpose, String justification, String requestedBy, boolean isEmergency, Instant occurredAt) {
        super(requestId, occurredAt != null ? occurredAt : Instant.now());
        if (requestId == null) throw new IllegalArgumentException("Request ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (caseId == null) throw new IllegalArgumentException("Case ID cannot be null");
        if (enrollmentId == null) throw new IllegalArgumentException("Enrollment ID cannot be null");
        if (assistanceType == null) throw new IllegalArgumentException("Assistance type cannot be null");
        if (requestedAmount == null) throw new IllegalArgumentException("Requested amount cannot be null");
        if (purpose == null || purpose.trim().isEmpty()) throw new IllegalArgumentException("Purpose cannot be null or empty");
        if (requestedBy == null || requestedBy.trim().isEmpty()) throw new IllegalArgumentException("Requested by cannot be null or empty");

        this.clientId = clientId;
        this.caseId = caseId;
        this.enrollmentId = enrollmentId;
        this.assistanceType = assistanceType;
        this.requestedAmount = requestedAmount;
        this.purpose = purpose;
        this.justification = justification;
        this.requestedBy = requestedBy;
        this.isEmergency = isEmergency;
    }

    public UUID clientId() {
        return clientId;
    }

    public UUID caseId() {
        return caseId;
    }

    public UUID enrollmentId() {
        return enrollmentId;
    }

    public CodeableConcept assistanceType() {
        return assistanceType;
    }

    public BigDecimal requestedAmount() {
        return requestedAmount;
    }

    public String purpose() {
        return purpose;
    }

    public String justification() {
        return justification;
    }

    public String requestedBy() {
        return requestedBy;
    }

    public boolean isEmergency() {
        return isEmergency;
    }


    public UUID requestId() {
        return getAggregateId();
    }

    @Override
    public String eventType() {
        return "RequestSubmitted";
    }

    // JavaBean-style getters
    public UUID getClientId() { return clientId; }
    public UUID getCaseId() { return caseId; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public CodeableConcept getAssistanceType() { return assistanceType; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public String getPurpose() { return purpose; }
    public String getJustification() { return justification; }
    public String getRequestedBy() { return requestedBy; }
    public boolean IsEmergency() { return isEmergency; }
}
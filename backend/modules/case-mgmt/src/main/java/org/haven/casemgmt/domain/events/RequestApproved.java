package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class RequestApproved extends DomainEvent {
    private final UUID clientId;
    private final BigDecimal requestedAmount;
    private final BigDecimal approvedAmount;
    private final String approvedBy;
    private final String approvalNotes;

    public RequestApproved(UUID requestId, UUID clientId, BigDecimal requestedAmount, BigDecimal approvedAmount, String approvedBy, String approvalNotes, Instant occurredAt) {
        super(requestId, occurredAt != null ? occurredAt : Instant.now());
        if (requestId == null) throw new IllegalArgumentException("Request ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (requestedAmount == null) throw new IllegalArgumentException("Requested amount cannot be null");
        if (approvedAmount == null) throw new IllegalArgumentException("Approved amount cannot be null");
        if (approvedBy == null || approvedBy.trim().isEmpty()) throw new IllegalArgumentException("Approved by cannot be null or empty");

        this.clientId = clientId;
        this.requestedAmount = requestedAmount;
        this.approvedAmount = approvedAmount;
        this.approvedBy = approvedBy;
        this.approvalNotes = approvalNotes;
    }

    public UUID clientId() {
        return clientId;
    }

    public BigDecimal requestedAmount() {
        return requestedAmount;
    }

    public BigDecimal approvedAmount() {
        return approvedAmount;
    }

    public String approvedBy() {
        return approvedBy;
    }

    public String approvalNotes() {
        return approvalNotes;
    }


    // JavaBean-style getters
    public UUID getClientId() { return clientId; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public BigDecimal getApprovedAmount() { return approvedAmount; }
    public String getApprovedBy() { return approvedBy; }
    public String getApprovalNotes() { return approvalNotes; }
}
package org.haven.casemgmt.domain.events;

import org.haven.shared.events.DomainEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class RequestPaid extends DomainEvent {
    private final UUID clientId;
    private final BigDecimal approvedAmount;
    private final BigDecimal paidAmount;
    private final String paymentMethod;
    private final String vendorName;
    private final String paymentReference;
    private final String paidBy;

    public RequestPaid(UUID requestId, UUID clientId, BigDecimal approvedAmount, BigDecimal paidAmount, String paymentMethod, String vendorName, String paymentReference, String paidBy, Instant occurredAt) {
        super(requestId, occurredAt != null ? occurredAt : Instant.now());
        if (requestId == null) throw new IllegalArgumentException("Request ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (approvedAmount == null) throw new IllegalArgumentException("Approved amount cannot be null");
        if (paidAmount == null) throw new IllegalArgumentException("Paid amount cannot be null");
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) throw new IllegalArgumentException("Payment method cannot be null or empty");
        if (paidBy == null || paidBy.trim().isEmpty()) throw new IllegalArgumentException("Paid by cannot be null or empty");

        this.clientId = clientId;
        this.approvedAmount = approvedAmount;
        this.paidAmount = paidAmount;
        this.paymentMethod = paymentMethod;
        this.vendorName = vendorName;
        this.paymentReference = paymentReference;
        this.paidBy = paidBy;
    }

    public UUID clientId() {
        return clientId;
    }

    public BigDecimal approvedAmount() {
        return approvedAmount;
    }

    public BigDecimal paidAmount() {
        return paidAmount;
    }

    public String paymentMethod() {
        return paymentMethod;
    }

    public String vendorName() {
        return vendorName;
    }

    public String paymentReference() {
        return paymentReference;
    }

    public String paidBy() {
        return paidBy;
    }


    // JavaBean-style getters
    public UUID getClientId() { return clientId; }
    public BigDecimal getApprovedAmount() { return approvedAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getVendorName() { return vendorName; }
    public String getPaymentReference() { return paymentReference; }
    public String getPaidBy() { return paidBy; }
}
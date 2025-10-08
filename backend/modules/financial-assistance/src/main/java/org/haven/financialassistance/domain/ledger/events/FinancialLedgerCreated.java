package org.haven.financialassistance.domain.ledger.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public class FinancialLedgerCreated extends DomainEvent {
    private final UUID clientId;
    private final UUID enrollmentId;
    private final UUID householdId;
    private final String ledgerName;
    private final boolean isVawaProtected;
    private final String createdBy;

    public FinancialLedgerCreated(
        UUID ledgerId,
        UUID clientId,
        UUID enrollmentId,
        UUID householdId,
        String ledgerName,
        boolean isVawaProtected,
        String createdBy,
        Instant occurredAt
    ) {
        super(ledgerId, occurredAt);
        this.clientId = clientId;
        this.enrollmentId = enrollmentId;
        this.householdId = householdId;
        this.ledgerName = ledgerName;
        this.isVawaProtected = isVawaProtected;
        this.createdBy = createdBy;
    }

    // JavaBean style getters
    public UUID getLedgerId() { return getAggregateId(); }
    public UUID getClientId() { return clientId; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public UUID getHouseholdId() { return householdId; }
    public String getLedgerName() { return ledgerName; }
    public boolean getIsVawaProtected() { return isVawaProtected; }
    public String getCreatedBy() { return createdBy; }

    // Record style getters
    public UUID ledgerId() { return getAggregateId(); }
    public UUID clientId() { return clientId; }
    public UUID enrollmentId() { return enrollmentId; }
    public UUID householdId() { return householdId; }
    public String ledgerName() { return ledgerName; }
    public boolean isVawaProtected() { return isVawaProtected; }
    public String createdBy() { return createdBy; }

    public static FinancialLedgerCreated create(UUID ledgerId, UUID clientId, UUID enrollmentId,
                                              UUID householdId, String ledgerName, boolean isVawaProtected,
                                              String createdBy) {
        return new FinancialLedgerCreated(
            ledgerId, clientId, enrollmentId, householdId, ledgerName, isVawaProtected, createdBy, Instant.now()
        );
    }
}
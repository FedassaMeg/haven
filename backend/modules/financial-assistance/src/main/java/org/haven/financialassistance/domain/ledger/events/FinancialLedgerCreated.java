package org.haven.financialassistance.domain.ledger.events;

import org.haven.shared.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record FinancialLedgerCreated(
    UUID ledgerId,
    UUID clientId,
    UUID enrollmentId,
    UUID householdId,
    String ledgerName,
    boolean isVawaProtected,
    String createdBy,
    Instant occurredAt
) implements DomainEvent {

    @Override
    public UUID aggregateId() {
        return ledgerId;
    }

    @Override
    public String eventType() {
        return "FinancialLedgerCreated";
    }

    public static FinancialLedgerCreated create(UUID ledgerId, UUID clientId, UUID enrollmentId,
                                              UUID householdId, String ledgerName, boolean isVawaProtected,
                                              String createdBy) {
        return new FinancialLedgerCreated(
            ledgerId, clientId, enrollmentId, householdId, ledgerName, isVawaProtected, createdBy, Instant.now()
        );
    }
}
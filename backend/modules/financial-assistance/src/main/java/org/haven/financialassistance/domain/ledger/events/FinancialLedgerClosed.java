package org.haven.financialassistance.domain.ledger.events;

import org.haven.shared.events.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FinancialLedgerClosed(
    UUID ledgerId,
    String reason,
    BigDecimal finalDebits,
    BigDecimal finalCredits,
    BigDecimal finalBalance,
    String closedBy,
    Instant occurredAt
) implements DomainEvent {

    @Override
    public UUID aggregateId() {
        return ledgerId;
    }

    @Override
    public String eventType() {
        return "FinancialLedgerClosed";
    }

    public static FinancialLedgerClosed create(UUID ledgerId, String reason, BigDecimal finalDebits,
                                             BigDecimal finalCredits, BigDecimal finalBalance, String closedBy) {
        return new FinancialLedgerClosed(
            ledgerId, reason, finalDebits, finalCredits, finalBalance, closedBy, Instant.now()
        );
    }
}
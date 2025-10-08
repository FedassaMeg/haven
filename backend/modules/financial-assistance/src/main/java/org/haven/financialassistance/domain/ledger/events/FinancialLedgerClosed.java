package org.haven.financialassistance.domain.ledger.events;

import org.haven.shared.events.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class FinancialLedgerClosed extends DomainEvent {
    private final String reason;
    private final BigDecimal finalDebits;
    private final BigDecimal finalCredits;
    private final BigDecimal finalBalance;
    private final String closedBy;

    public FinancialLedgerClosed(
        UUID ledgerId,
        String reason,
        BigDecimal finalDebits,
        BigDecimal finalCredits,
        BigDecimal finalBalance,
        String closedBy,
        Instant occurredAt
    ) {
        super(ledgerId, occurredAt);
        this.reason = reason;
        this.finalDebits = finalDebits;
        this.finalCredits = finalCredits;
        this.finalBalance = finalBalance;
        this.closedBy = closedBy;
    }

    // JavaBean style getters
    public UUID getLedgerId() { return getAggregateId(); }
    public String getReason() { return reason; }
    public BigDecimal getFinalDebits() { return finalDebits; }
    public BigDecimal getFinalCredits() { return finalCredits; }
    public BigDecimal getFinalBalance() { return finalBalance; }
    public String getClosedBy() { return closedBy; }

    // Record style getters
    public UUID ledgerId() { return getAggregateId(); }
    public String reason() { return reason; }
    public BigDecimal finalDebits() { return finalDebits; }
    public BigDecimal finalCredits() { return finalCredits; }
    public BigDecimal finalBalance() { return finalBalance; }
    public String closedBy() { return closedBy; }

    public static FinancialLedgerClosed create(UUID ledgerId, String reason, BigDecimal finalDebits,
                                             BigDecimal finalCredits, BigDecimal finalBalance, String closedBy) {
        return new FinancialLedgerClosed(
            ledgerId, reason, finalDebits, finalCredits, finalBalance, closedBy, Instant.now()
        );
    }
}
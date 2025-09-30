package org.haven.api.financialledger.dto;

import org.haven.financialassistance.domain.ledger.FinancialLedger;
import org.haven.financialassistance.domain.ledger.LedgerStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LedgerSummaryResponse(
    UUID ledgerId,
    UUID clientId,
    UUID householdId,
    String ledgerName,
    LedgerStatus status,
    BigDecimal balance,
    boolean isVawaProtected,
    boolean isBalanced,
    int transactionCount,
    Instant createdAt,
    Instant lastModified
) {
    public static LedgerSummaryResponse fromDomain(FinancialLedger ledger) {
        return new LedgerSummaryResponse(
            ledger.getId().value(),
            ledger.getClientId().value(),
            ledger.getHouseholdId(),
            ledger.getLedgerName(),
            ledger.getStatus(),
            ledger.getBalance(),
            ledger.isVawaProtected(),
            ledger.isBalanced(),
            ledger.getEntries().size() / 2, // Each transaction creates 2 entries
            ledger.getCreatedAt(),
            ledger.getLastModified()
        );
    }
}
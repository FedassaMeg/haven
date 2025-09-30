package org.haven.api.financialledger.dto;

import org.haven.financialassistance.domain.ledger.FinancialLedger;
import org.haven.financialassistance.domain.ledger.LedgerStatus;
import org.haven.financialassistance.domain.ledger.VawaRedactionLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LedgerDetailResponse(
    UUID ledgerId,
    UUID clientId,
    UUID enrollmentId,
    UUID householdId,
    String ledgerName,
    LedgerStatus status,
    BigDecimal totalDebits,
    BigDecimal totalCredits,
    BigDecimal balance,
    boolean isVawaProtected,
    VawaRedactionLevel redactionLevel,
    List<LedgerEntryResponse> entries,
    Instant createdAt,
    Instant lastModified,
    String createdBy
) {
    public static LedgerDetailResponse fromDomain(FinancialLedger ledger) {
        List<LedgerEntryResponse> entryResponses = ledger.getEntries().stream()
            .map(LedgerEntryResponse::fromDomain)
            .toList();

        return new LedgerDetailResponse(
            ledger.getId().value(),
            ledger.getClientId().value(),
            ledger.getEnrollmentId().value(),
            ledger.getHouseholdId(),
            ledger.getLedgerName(),
            ledger.getStatus(),
            ledger.getTotalDebits(),
            ledger.getTotalCredits(),
            ledger.getBalance(),
            ledger.isVawaProtected(),
            ledger.getRedactionLevel(),
            entryResponses,
            ledger.getCreatedAt(),
            ledger.getLastModified(),
            ledger.getCreatedBy()
        );
    }

    public boolean isBalanced() {
        return totalDebits.compareTo(totalCredits) == 0;
    }

    public int getTransactionCount() {
        return entries.size() / 2; // Each transaction creates 2 entries
    }
}
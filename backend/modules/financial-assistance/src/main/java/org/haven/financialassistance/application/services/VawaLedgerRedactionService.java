package org.haven.financialassistance.application.services;

import org.haven.financialassistance.domain.ledger.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service responsible for applying VAWA (Violence Against Women Act) redaction rules
 * to financial ledger data when viewed by landlords or external parties
 */
@Service
public class VawaLedgerRedactionService {

    /**
     * Redact ledger entries for landlord view based on VAWA protection level
     */
    public List<LedgerEntry> redactEntriesForLandlord(List<LedgerEntry> entries, VawaRedactionLevel redactionLevel, String landlordId) {
        return entries.stream()
            .filter(entry -> shouldShowEntryToLandlord(entry, redactionLevel))
            .map(entry -> redactEntry(entry, redactionLevel, landlordId))
            .toList();
    }

    /**
     * Create a redacted view of the entire ledger for landlord access
     */
    public LedgerLandlordView createLandlordView(FinancialLedger ledger, String landlordId) {
        if (!ledger.isVawaProtected()) {
            return new LedgerLandlordView(
                ledger.getId().value(),
                ledger.getClientId().value(),
                ledger.getLedgerName(),
                landlordId,
                ledger.getEntriesForPayee(landlordId),
                calculateLandlordBalance(ledger.getEntriesForPayee(landlordId)),
                false
            );
        }

        // Apply VAWA redaction
        List<LedgerEntry> redactedEntries = redactEntriesForLandlord(
            ledger.getEntriesForPayee(landlordId),
            ledger.getRedactionLevel(),
            landlordId
        );

        return new LedgerLandlordView(
            ledger.getId().value(),
            null, // Redact client ID
            "[CONFIDENTIAL CLIENT]",
            landlordId,
            redactedEntries,
            calculateRedactedBalance(redactedEntries, ledger.getRedactionLevel()),
            true
        );
    }

    /**
     * Determine if an entry should be visible to the landlord
     */
    private boolean shouldShowEntryToLandlord(LedgerEntry entry, VawaRedactionLevel redactionLevel) {
        return switch (redactionLevel) {
            case NONE -> true;
            case PARTIAL, FULL -> entry.getPayeeId() != null;
            case COMPLETE -> false;
        };
    }

    /**
     * Apply redaction to a single ledger entry
     */
    private LedgerEntry redactEntry(LedgerEntry entry, VawaRedactionLevel redactionLevel, String landlordId) {
        return switch (redactionLevel) {
            case NONE -> entry;
            case PARTIAL -> new LedgerEntry(
                entry.getEntryId(),
                "[REDACTED]",
                entry.getEntryType(),
                entry.getAccountClassification(),
                entry.getAmount(), // Keep amount visible
                redactDescription(entry.getDescription()),
                null, // Redact funding source
                entry.getHudCategoryCode(),
                entry.getPayeeId(),
                entry.getPayeeName(),
                entry.getPeriodStart(),
                entry.getPeriodEnd(),
                "[SYSTEM]",
                entry.getRecordedAt()
            );
            case FULL -> new LedgerEntry(
                entry.getEntryId(),
                "[REDACTED]",
                entry.getEntryType(),
                AccountClassification.OTHER_EXPENSE, // Generic classification
                BigDecimal.ZERO, // Hide amount
                "[VAWA PROTECTED - DETAILS REDACTED]",
                null,
                null,
                entry.getPayeeId(),
                entry.getPayeeName(),
                null, // Hide period details
                null,
                "[SYSTEM]",
                entry.getRecordedAt()
            );
            case COMPLETE -> throw new IllegalArgumentException("Complete redaction should filter out entries");
        };
    }

    /**
     * Redact sensitive information from descriptions while preserving basic payment info
     */
    private String redactDescription(String originalDescription) {
        if (originalDescription == null) {
            return null;
        }

        // Remove specific dates, amounts, and funding references
        return originalDescription
            .replaceAll("\\$[0-9,.]+ ", "$[AMOUNT] ")
            .replaceAll("\\b\\d{4}-\\d{2}-\\d{2}\\b", "[DATE]")
            .replaceAll("Grant\\s+\\w+", "Grant [REDACTED]")
            .replaceAll("Fund\\s+\\w+", "Fund [REDACTED]");
    }

    /**
     * Calculate balance visible to landlord
     */
    private BigDecimal calculateLandlordBalance(List<LedgerEntry> entries) {
        return entries.stream()
            .map(entry -> entry.getEntryType() == EntryType.CREDIT ?
                         entry.getAmount() : entry.getAmount().negate())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate redacted balance based on redaction level
     */
    private BigDecimal calculateRedactedBalance(List<LedgerEntry> redactedEntries, VawaRedactionLevel redactionLevel) {
        if (redactionLevel == VawaRedactionLevel.FULL || redactionLevel == VawaRedactionLevel.COMPLETE) {
            return BigDecimal.ZERO; // Hide balance completely
        }
        return calculateLandlordBalance(redactedEntries);
    }

    /**
     * Data transfer object for landlord view of the ledger
     */
    public static class LedgerLandlordView {
        private final UUID ledgerId;
        private final UUID clientId; // May be null for VAWA protected
        private final String clientName; // May be redacted
        private final String landlordId;
        private final List<LedgerEntry> visibleEntries;
        private final BigDecimal visibleBalance;
        private final boolean isVawaProtected;

        public LedgerLandlordView(UUID ledgerId, UUID clientId, String clientName, String landlordId,
                                List<LedgerEntry> visibleEntries, BigDecimal visibleBalance, boolean isVawaProtected) {
            this.ledgerId = ledgerId;
            this.clientId = clientId;
            this.clientName = clientName;
            this.landlordId = landlordId;
            this.visibleEntries = visibleEntries;
            this.visibleBalance = visibleBalance;
            this.isVawaProtected = isVawaProtected;
        }

        // Getters
        public UUID getLedgerId() { return ledgerId; }
        public UUID getClientId() { return clientId; }
        public String getClientName() { return clientName; }
        public String getLandlordId() { return landlordId; }
        public List<LedgerEntry> getVisibleEntries() { return visibleEntries; }
        public BigDecimal getVisibleBalance() { return visibleBalance; }
        public boolean isVawaProtected() { return isVawaProtected; }

        public int getVisibleTransactionCount() {
            return visibleEntries.size();
        }

        public BigDecimal getVisiblePaymentTotal() {
            return visibleEntries.stream()
                .filter(entry -> entry.getEntryType() == EntryType.CREDIT)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }
}
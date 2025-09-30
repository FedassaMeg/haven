package org.haven.financialassistance.domain.ledger;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Individual ledger entry representing one side of a double-entry transaction
 */
public class LedgerEntry {

    private final UUID entryId;
    private final String transactionId;
    private final EntryType entryType;
    private final AccountClassification accountClassification;
    private final BigDecimal amount;
    private final String description;
    private final String fundingSourceCode;
    private final String hudCategoryCode;
    private final String payeeId;
    private final String payeeName;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final String recordedBy;
    private final Instant recordedAt;

    public LedgerEntry(UUID entryId, String transactionId, EntryType entryType,
                      AccountClassification accountClassification, BigDecimal amount,
                      String description, String fundingSourceCode, String hudCategoryCode,
                      String payeeId, String payeeName, LocalDate periodStart, LocalDate periodEnd,
                      String recordedBy, Instant recordedAt) {
        this.entryId = entryId;
        this.transactionId = transactionId;
        this.entryType = entryType;
        this.accountClassification = accountClassification;
        this.amount = amount;
        this.description = description;
        this.fundingSourceCode = fundingSourceCode;
        this.hudCategoryCode = hudCategoryCode;
        this.payeeId = payeeId;
        this.payeeName = payeeName;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.recordedBy = recordedBy;
        this.recordedAt = recordedAt;

        // Validation
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (entryType == null) {
            throw new IllegalArgumentException("Entry type is required");
        }
        if (accountClassification == null) {
            throw new IllegalArgumentException("Account classification is required");
        }
    }

    // Getters
    public UUID getEntryId() { return entryId; }
    public String getTransactionId() { return transactionId; }
    public EntryType getEntryType() { return entryType; }
    public AccountClassification getAccountClassification() { return accountClassification; }
    public BigDecimal getAmount() { return amount; }
    public String getDescription() { return description; }
    public String getFundingSourceCode() { return fundingSourceCode; }
    public String getHudCategoryCode() { return hudCategoryCode; }
    public String getPayeeId() { return payeeId; }
    public String getPayeeName() { return payeeName; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public String getRecordedBy() { return recordedBy; }
    public Instant getRecordedAt() { return recordedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LedgerEntry that = (LedgerEntry) o;
        return entryId.equals(that.entryId);
    }

    @Override
    public int hashCode() {
        return entryId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("LedgerEntry{entryId=%s, type=%s, account=%s, amount=%s, description='%s'}",
                           entryId, entryType, accountClassification, amount, description);
    }
}
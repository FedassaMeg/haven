package org.haven.api.financialledger.dto;

import org.haven.financialassistance.domain.ledger.AccountClassification;
import org.haven.financialassistance.domain.ledger.EntryType;
import org.haven.financialassistance.domain.ledger.LedgerEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LedgerEntryResponse(
    UUID entryId,
    String transactionId,
    EntryType entryType,
    AccountClassification accountClassification,
    BigDecimal amount,
    String description,
    String fundingSourceCode,
    String hudCategoryCode,
    String payeeId,
    String payeeName,
    LocalDate periodStart,
    LocalDate periodEnd,
    String recordedBy,
    Instant recordedAt
) {
    public static LedgerEntryResponse fromDomain(LedgerEntry entry) {
        return new LedgerEntryResponse(
            entry.getEntryId(),
            entry.getTransactionId(),
            entry.getEntryType(),
            entry.getAccountClassification(),
            entry.getAmount(),
            entry.getDescription(),
            entry.getFundingSourceCode(),
            entry.getHudCategoryCode(),
            entry.getPayeeId(),
            entry.getPayeeName(),
            entry.getPeriodStart(),
            entry.getPeriodEnd(),
            entry.getRecordedBy(),
            entry.getRecordedAt()
        );
    }

    public boolean isDebit() {
        return entryType == EntryType.DEBIT;
    }

    public boolean isCredit() {
        return entryType == EntryType.CREDIT;
    }

    public boolean hasPayee() {
        return payeeId != null && !payeeId.isBlank();
    }

    public boolean isArrearsEntry() {
        return description != null && description.toLowerCase().contains("arrears");
    }
}
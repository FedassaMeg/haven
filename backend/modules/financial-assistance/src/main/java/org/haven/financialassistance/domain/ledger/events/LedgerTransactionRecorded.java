package org.haven.financialassistance.domain.ledger.events;

import org.haven.financialassistance.domain.ledger.AccountClassification;
import org.haven.financialassistance.domain.ledger.TransactionType;
import org.haven.shared.events.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LedgerTransactionRecorded(
    UUID ledgerId,
    String transactionId,
    TransactionType transactionType,
    BigDecimal amount,
    String fundingSourceCode,
    String hudCategoryCode,
    String description,
    String payeeId,
    String payeeName,
    LocalDate periodStart,
    LocalDate periodEnd,
    UUID debitEntryId,
    AccountClassification debitAccount,
    UUID creditEntryId,
    AccountClassification creditAccount,
    String recordedBy,
    Instant occurredAt
) implements DomainEvent {

    @Override
    public UUID aggregateId() {
        return ledgerId;
    }

    @Override
    public String eventType() {
        return "LedgerTransactionRecorded";
    }

    public static LedgerTransactionRecorded create(UUID ledgerId, String transactionId,
                                                 TransactionType transactionType, BigDecimal amount,
                                                 String fundingSourceCode, String hudCategoryCode,
                                                 String description, String payeeId, String payeeName,
                                                 LocalDate periodStart, LocalDate periodEnd,
                                                 UUID debitEntryId, AccountClassification debitAccount,
                                                 UUID creditEntryId, AccountClassification creditAccount,
                                                 String recordedBy) {
        return new LedgerTransactionRecorded(
            ledgerId, transactionId, transactionType, amount, fundingSourceCode, hudCategoryCode,
            description, payeeId, payeeName, periodStart, periodEnd, debitEntryId, debitAccount,
            creditEntryId, creditAccount, recordedBy, Instant.now()
        );
    }
}
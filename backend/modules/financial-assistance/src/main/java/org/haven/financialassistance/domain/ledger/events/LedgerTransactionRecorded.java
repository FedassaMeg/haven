package org.haven.financialassistance.domain.ledger.events;

import org.haven.financialassistance.domain.ledger.AccountClassification;
import org.haven.financialassistance.domain.ledger.TransactionType;
import org.haven.shared.events.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class LedgerTransactionRecorded extends DomainEvent {
    private final String transactionId;
    private final TransactionType transactionType;
    private final BigDecimal amount;
    private final String fundingSourceCode;
    private final String hudCategoryCode;
    private final String description;
    private final String payeeId;
    private final String payeeName;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final UUID debitEntryId;
    private final AccountClassification debitAccount;
    private final UUID creditEntryId;
    private final AccountClassification creditAccount;
    private final String recordedBy;

    public LedgerTransactionRecorded(
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
    ) {
        super(ledgerId, occurredAt);
        this.transactionId = transactionId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.fundingSourceCode = fundingSourceCode;
        this.hudCategoryCode = hudCategoryCode;
        this.description = description;
        this.payeeId = payeeId;
        this.payeeName = payeeName;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.debitEntryId = debitEntryId;
        this.debitAccount = debitAccount;
        this.creditEntryId = creditEntryId;
        this.creditAccount = creditAccount;
        this.recordedBy = recordedBy;
    }

    // JavaBean style getters
    public UUID getLedgerId() { return getAggregateId(); }
    public String getTransactionId() { return transactionId; }
    public TransactionType getTransactionType() { return transactionType; }
    public BigDecimal getAmount() { return amount; }
    public String getFundingSourceCode() { return fundingSourceCode; }
    public String getHudCategoryCode() { return hudCategoryCode; }
    public String getDescription() { return description; }
    public String getPayeeId() { return payeeId; }
    public String getPayeeName() { return payeeName; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public UUID getDebitEntryId() { return debitEntryId; }
    public AccountClassification getDebitAccount() { return debitAccount; }
    public UUID getCreditEntryId() { return creditEntryId; }
    public AccountClassification getCreditAccount() { return creditAccount; }
    public String getRecordedBy() { return recordedBy; }

    // Record style getters
    public UUID ledgerId() { return getAggregateId(); }
    public String transactionId() { return transactionId; }
    public TransactionType transactionType() { return transactionType; }
    public BigDecimal amount() { return amount; }
    public String fundingSourceCode() { return fundingSourceCode; }
    public String hudCategoryCode() { return hudCategoryCode; }
    public String description() { return description; }
    public String payeeId() { return payeeId; }
    public String payeeName() { return payeeName; }
    public LocalDate periodStart() { return periodStart; }
    public LocalDate periodEnd() { return periodEnd; }
    public UUID debitEntryId() { return debitEntryId; }
    public AccountClassification debitAccount() { return debitAccount; }
    public UUID creditEntryId() { return creditEntryId; }
    public AccountClassification creditAccount() { return creditAccount; }
    public String recordedBy() { return recordedBy; }

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
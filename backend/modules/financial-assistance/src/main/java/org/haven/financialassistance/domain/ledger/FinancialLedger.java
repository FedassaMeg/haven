package org.haven.financialassistance.domain.ledger;

import org.haven.clientprofile.domain.ClientId;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.haven.shared.domain.AggregateRoot;
import org.haven.shared.events.DomainEvent;
import org.haven.financialassistance.domain.ledger.events.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Financial Ledger aggregate implementing double-entry accounting for all financial assistance transactions.
 * Provides unified tracking of payments, deposits, arrears, and landlord communications with full auditability.
 */
public class FinancialLedger extends AggregateRoot<FinancialLedgerId> {

    private ClientId clientId;
    private ProgramEnrollmentId enrollmentId;
    private UUID householdId;
    private String ledgerName;
    private LedgerStatus status;

    // Double-entry accounting
    private List<LedgerEntry> entries = new ArrayList<>();
    private BigDecimal totalDebits;
    private BigDecimal totalCredits;
    private BigDecimal balance;

    // VAWA and privacy controls
    private boolean isVawaProtected;
    private VawaRedactionLevel redactionLevel;

    // Metadata
    private Instant createdAt;
    private Instant lastModified;
    private String createdBy;

    public static FinancialLedger create(ClientId clientId, ProgramEnrollmentId enrollmentId,
                                       UUID householdId, String ledgerName, boolean isVawaProtected,
                                       String createdBy) {
        FinancialLedgerId id = FinancialLedgerId.generate();
        FinancialLedger ledger = new FinancialLedger();
        ledger.apply(new FinancialLedgerCreated(
            id.value(),
            clientId.value(),
            enrollmentId.value(),
            householdId,
            ledgerName,
            isVawaProtected,
            createdBy,
            Instant.now()
        ));
        return ledger;
    }

    public void recordTransaction(String transactionId, TransactionType transactionType,
                                BigDecimal amount, String fundingSourceCode, String hudCategoryCode,
                                String description, String payeeId, String payeeName,
                                LocalDate periodStart, LocalDate periodEnd, String recordedBy) {

        if (status == LedgerStatus.CLOSED) {
            throw new IllegalStateException("Cannot record transactions on closed ledger");
        }

        // Create debit and credit entries for double-entry accounting
        UUID debitEntryId = UUID.randomUUID();
        UUID creditEntryId = UUID.randomUUID();

        // Determine account classifications based on transaction type
        AccountClassification debitAccount = getDebitAccount(transactionType);
        AccountClassification creditAccount = getCreditAccount(transactionType);

        apply(new LedgerTransactionRecorded(
            id.value(),
            transactionId,
            transactionType,
            amount,
            fundingSourceCode,
            hudCategoryCode,
            description,
            payeeId,
            payeeName,
            periodStart,
            periodEnd,
            debitEntryId,
            debitAccount,
            creditEntryId,
            creditAccount,
            recordedBy,
            Instant.now()
        ));
    }

    public void recordPayment(String paymentId, String assistanceId, BigDecimal amount,
                            String fundingSourceCode, String hudCategoryCode, PaymentSubtype subtype,
                            String payeeId, String payeeName, LocalDate paymentDate,
                            LocalDate periodStart, LocalDate periodEnd, String recordedBy) {

        TransactionType transactionType = mapPaymentSubtypeToTransactionType(subtype);
        String description = buildPaymentDescription(subtype, paymentDate, periodStart, periodEnd);

        recordTransaction(paymentId, transactionType, amount, fundingSourceCode, hudCategoryCode,
                        description, payeeId, payeeName, periodStart, periodEnd, recordedBy);
    }

    public void recordDeposit(String depositId, BigDecimal amount, String fundingSourceCode,
                            String depositSource, LocalDate depositDate, String recordedBy) {

        String description = String.format("Deposit from %s on %s", depositSource, depositDate);
        recordTransaction(depositId, TransactionType.FUNDING_DEPOSIT, amount, fundingSourceCode, null,
                        description, null, depositSource, depositDate, depositDate, recordedBy);
    }

    public void recordArrears(String arrearsId, BigDecimal amount, ArrearsType arrearsType,
                            String payeeId, String payeeName, LocalDate periodStart, LocalDate periodEnd,
                            String recordedBy) {

        TransactionType transactionType = (arrearsType == ArrearsType.RENT) ?
            TransactionType.RENT_ARREARS : TransactionType.UTILITY_ARREARS;
        String description = String.format("%s arrears for period %s to %s",
                                         arrearsType.name(), periodStart, periodEnd);

        recordTransaction(arrearsId, transactionType, amount, null, getHudCategoryForArrears(arrearsType),
                        description, payeeId, payeeName, periodStart, periodEnd, recordedBy);
    }

    public void recordLandlordCommunication(String communicationId, String landlordId, String landlordName,
                                          CommunicationType communicationType, String subject, String content,
                                          LocalDate communicationDate, String recordedBy) {

        // Only record communication metadata, not content for VAWA protection
        String sanitizedContent = isVawaProtected ? "[VAWA PROTECTED - CONTENT REDACTED]" : content;

        apply(new LandlordCommunicationRecorded(
            id.value(),
            communicationId,
            landlordId,
            landlordName,
            communicationType,
            subject,
            sanitizedContent,
            communicationDate,
            recordedBy,
            Instant.now()
        ));
    }

    public void attachDocument(String documentId, String documentName, String documentType,
                             String uploadedBy, byte[] documentContent) {

        // Redact document content for VAWA protected cases
        byte[] sanitizedContent = isVawaProtected ? new byte[0] : documentContent;

        apply(new DocumentAttached(
            id.value(),
            documentId,
            documentName,
            documentType,
            sanitizedContent,
            uploadedBy,
            Instant.now()
        ));
    }

    public void closeLedger(String reason, String closedBy) {
        if (status == LedgerStatus.CLOSED) {
            throw new IllegalStateException("Ledger is already closed");
        }

        // Verify ledger is balanced before closing
        if (!isBalanced()) {
            throw new IllegalStateException("Cannot close unbalanced ledger. Debits: " + totalDebits +
                                          ", Credits: " + totalCredits);
        }

        apply(new FinancialLedgerClosed(
            id.value(),
            reason,
            totalDebits,
            totalCredits,
            balance,
            closedBy,
            Instant.now()
        ));
    }

    private AccountClassification getDebitAccount(TransactionType transactionType) {
        return switch (transactionType) {
            case RENT_PAYMENT, RENT_ARREARS -> AccountClassification.RENT_EXPENSE;
            case UTILITY_PAYMENT, UTILITY_ARREARS -> AccountClassification.UTILITY_EXPENSE;
            case SECURITY_DEPOSIT -> AccountClassification.SECURITY_DEPOSIT_ASSET;
            case MOVING_COSTS -> AccountClassification.MOVING_EXPENSE;
            case FUNDING_DEPOSIT -> AccountClassification.CASH_ASSET;
            case OTHER_PAYMENT -> AccountClassification.OTHER_EXPENSE;
        };
    }

    private AccountClassification getCreditAccount(TransactionType transactionType) {
        return switch (transactionType) {
            case FUNDING_DEPOSIT -> AccountClassification.FUNDING_LIABILITY;
            default -> AccountClassification.CASH_ASSET;
        };
    }

    private TransactionType mapPaymentSubtypeToTransactionType(PaymentSubtype subtype) {
        return switch (subtype) {
            case RENT_CURRENT -> TransactionType.RENT_PAYMENT;
            case RENT_ARREARS -> TransactionType.RENT_ARREARS;
            case UTILITY_CURRENT -> TransactionType.UTILITY_PAYMENT;
            case UTILITY_ARREARS -> TransactionType.UTILITY_ARREARS;
            case SECURITY_DEPOSIT -> TransactionType.SECURITY_DEPOSIT;
            case MOVING_COSTS -> TransactionType.MOVING_COSTS;
            case OTHER -> TransactionType.OTHER_PAYMENT;
        };
    }

    private String buildPaymentDescription(PaymentSubtype subtype, LocalDate paymentDate,
                                         LocalDate periodStart, LocalDate periodEnd) {
        String baseDescription = subtype.getDisplayName() + " payment on " + paymentDate;
        if (periodStart != null && periodEnd != null) {
            baseDescription += " for period " + periodStart + " to " + periodEnd;
        }
        return baseDescription;
    }

    private String getHudCategoryForArrears(ArrearsType arrearsType) {
        return switch (arrearsType) {
            case RENT -> "4.02"; // HUD rental assistance category
            case UTILITY -> "4.03"; // HUD utility assistance category
        };
    }

    @Override
    protected void when(DomainEvent event) {
        if (event instanceof FinancialLedgerCreated e) {
            this.id = FinancialLedgerId.of(e.ledgerId());
            this.clientId = new ClientId(e.clientId());
            this.enrollmentId = new ProgramEnrollmentId(e.enrollmentId());
            this.householdId = e.householdId();
            this.ledgerName = e.ledgerName();
            this.isVawaProtected = e.isVawaProtected();
            this.redactionLevel = e.isVawaProtected() ? VawaRedactionLevel.FULL : VawaRedactionLevel.NONE;
            this.status = LedgerStatus.ACTIVE;
            this.totalDebits = BigDecimal.ZERO;
            this.totalCredits = BigDecimal.ZERO;
            this.balance = BigDecimal.ZERO;
            this.createdBy = e.createdBy();
            this.createdAt = e.occurredAt();
            this.lastModified = e.occurredAt();

        } else if (event instanceof LedgerTransactionRecorded e) {
            // Create debit entry
            LedgerEntry debitEntry = new LedgerEntry(
                e.debitEntryId(),
                e.transactionId(),
                EntryType.DEBIT,
                e.debitAccount(),
                e.amount(),
                e.description(),
                e.fundingSourceCode(),
                e.hudCategoryCode(),
                e.payeeId(),
                e.payeeName(),
                e.periodStart(),
                e.periodEnd(),
                e.recordedBy(),
                e.occurredAt()
            );

            // Create credit entry
            LedgerEntry creditEntry = new LedgerEntry(
                e.creditEntryId(),
                e.transactionId(),
                EntryType.CREDIT,
                e.creditAccount(),
                e.amount(),
                e.description(),
                e.fundingSourceCode(),
                e.hudCategoryCode(),
                e.payeeId(),
                e.payeeName(),
                e.periodStart(),
                e.periodEnd(),
                e.recordedBy(),
                e.occurredAt()
            );

            this.entries.add(debitEntry);
            this.entries.add(creditEntry);
            this.totalDebits = totalDebits.add(e.amount());
            this.totalCredits = totalCredits.add(e.amount());
            this.balance = totalCredits.subtract(totalDebits);
            this.lastModified = e.occurredAt();

        } else if (event instanceof FinancialLedgerClosed e) {
            this.status = LedgerStatus.CLOSED;
            this.lastModified = e.occurredAt();

        } else {
            throw new IllegalArgumentException("Unhandled event: " + event.getClass());
        }
    }

    public boolean isBalanced() {
        return totalDebits.compareTo(totalCredits) == 0;
    }

    public List<LedgerEntry> getEntriesForLandlordView(String landlordId) {
        if (!isVawaProtected) {
            return getEntriesForPayee(landlordId);
        }

        // For VAWA protected cases, return redacted entries
        return entries.stream()
            .filter(entry -> landlordId.equals(entry.getPayeeId()))
            .map(this::redactEntryForLandlord)
            .toList();
    }

    private LedgerEntry redactEntryForLandlord(LedgerEntry entry) {
        return new LedgerEntry(
            entry.getEntryId(),
            "[REDACTED]",
            entry.getEntryType(),
            entry.getAccountClassification(),
            entry.getAmount(),
            "[VAWA PROTECTED - DETAILS REDACTED]",
            null, // Redact funding source
            entry.getHudCategoryCode(),
            entry.getPayeeId(),
            entry.getPayeeName(),
            entry.getPeriodStart(),
            entry.getPeriodEnd(),
            "[SYSTEM]",
            entry.getRecordedAt()
        );
    }

    public List<LedgerEntry> getEntriesForPayee(String payeeId) {
        return entries.stream()
            .filter(entry -> payeeId.equals(entry.getPayeeId()))
            .toList();
    }

    // Getters
    public ClientId getClientId() { return clientId; }
    public ProgramEnrollmentId getEnrollmentId() { return enrollmentId; }
    public UUID getHouseholdId() { return householdId; }
    public String getLedgerName() { return ledgerName; }
    public LedgerStatus getStatus() { return status; }
    public List<LedgerEntry> getEntries() { return List.copyOf(entries); }
    public BigDecimal getTotalDebits() { return totalDebits; }
    public BigDecimal getTotalCredits() { return totalCredits; }
    public BigDecimal getBalance() { return balance; }
    public boolean isVawaProtected() { return isVawaProtected; }
    public VawaRedactionLevel getRedactionLevel() { return redactionLevel; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastModified() { return lastModified; }
    public String getCreatedBy() { return createdBy; }
}
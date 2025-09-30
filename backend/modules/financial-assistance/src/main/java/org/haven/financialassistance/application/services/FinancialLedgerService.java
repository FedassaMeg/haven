package org.haven.financialassistance.application.services;

import org.haven.clientprofile.domain.ClientId;
import org.haven.financialassistance.domain.ledger.*;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service for managing financial ledgers and integrating with payment workflows
 */
@Service
@Transactional
public class FinancialLedgerService {

    private final FinancialLedgerRepository ledgerRepository;
    private final VawaLedgerRedactionService redactionService;

    public FinancialLedgerService(FinancialLedgerRepository ledgerRepository,
                                VawaLedgerRedactionService redactionService) {
        this.ledgerRepository = ledgerRepository;
        this.redactionService = redactionService;
    }

    /**
     * Create a new financial ledger for a client
     */
    public FinancialLedgerId createLedger(ClientId clientId, ProgramEnrollmentId enrollmentId,
                                        UUID householdId, String ledgerName, boolean isVawaProtected,
                                        String createdBy) {
        FinancialLedger ledger = FinancialLedger.create(
            clientId, enrollmentId, householdId, ledgerName, isVawaProtected, createdBy
        );
        ledgerRepository.save(ledger);
        return ledger.getId();
    }

    /**
     * Record a payment transaction from existing payment workflows
     */
    public void recordPaymentTransaction(FinancialLedgerId ledgerId, String paymentId, String assistanceId,
                                       BigDecimal amount, String fundingSourceCode, String hudCategoryCode,
                                       PaymentSubtype subtype, String payeeId, String payeeName,
                                       LocalDate paymentDate, LocalDate periodStart, LocalDate periodEnd,
                                       String recordedBy) {
        FinancialLedger ledger = ledgerRepository.findById(ledgerId)
            .orElseThrow(() -> new IllegalArgumentException("Ledger not found: " + ledgerId));

        ledger.recordPayment(paymentId, assistanceId, amount, fundingSourceCode, hudCategoryCode,
                           subtype, payeeId, payeeName, paymentDate, periodStart, periodEnd, recordedBy);

        ledgerRepository.save(ledger);
    }

    /**
     * Record a funding deposit
     */
    public void recordFundingDeposit(FinancialLedgerId ledgerId, String depositId, BigDecimal amount,
                                   String fundingSourceCode, String depositSource, LocalDate depositDate,
                                   String recordedBy) {
        FinancialLedger ledger = ledgerRepository.findById(ledgerId)
            .orElseThrow(() -> new IllegalArgumentException("Ledger not found: " + ledgerId));

        ledger.recordDeposit(depositId, amount, fundingSourceCode, depositSource, depositDate, recordedBy);
        ledgerRepository.save(ledger);
    }

    /**
     * Record arrears for a client
     */
    public void recordArrears(FinancialLedgerId ledgerId, String arrearsId, BigDecimal amount,
                            ArrearsType arrearsType, String payeeId, String payeeName,
                            LocalDate periodStart, LocalDate periodEnd, String recordedBy) {
        FinancialLedger ledger = ledgerRepository.findById(ledgerId)
            .orElseThrow(() -> new IllegalArgumentException("Ledger not found: " + ledgerId));

        ledger.recordArrears(arrearsId, amount, arrearsType, payeeId, payeeName,
                           periodStart, periodEnd, recordedBy);
        ledgerRepository.save(ledger);
    }

    /**
     * Record landlord communication
     */
    public void recordLandlordCommunication(FinancialLedgerId ledgerId, String communicationId,
                                          String landlordId, String landlordName, CommunicationType type,
                                          String subject, String content, LocalDate communicationDate,
                                          String recordedBy) {
        FinancialLedger ledger = ledgerRepository.findById(ledgerId)
            .orElseThrow(() -> new IllegalArgumentException("Ledger not found: " + ledgerId));

        ledger.recordLandlordCommunication(communicationId, landlordId, landlordName, type,
                                         subject, content, communicationDate, recordedBy);
        ledgerRepository.save(ledger);
    }

    /**
     * Attach a document to the ledger
     */
    public void attachDocument(FinancialLedgerId ledgerId, String documentId, String documentName,
                             String documentType, String uploadedBy, byte[] documentContent) {
        FinancialLedger ledger = ledgerRepository.findById(ledgerId)
            .orElseThrow(() -> new IllegalArgumentException("Ledger not found: " + ledgerId));

        ledger.attachDocument(documentId, documentName, documentType, uploadedBy, documentContent);
        ledgerRepository.save(ledger);
    }

    /**
     * Get ledger view for landlord with VAWA protections applied
     */
    @Transactional(readOnly = true)
    public Optional<VawaLedgerRedactionService.LedgerLandlordView> getLandlordView(FinancialLedgerId ledgerId, String landlordId) {
        return ledgerRepository.findById(ledgerId)
            .map(ledger -> redactionService.createLandlordView(ledger, landlordId));
    }

    /**
     * Get all ledgers for a client
     */
    @Transactional(readOnly = true)
    public List<FinancialLedger> getClientLedgers(ClientId clientId) {
        return ledgerRepository.findByClientId(clientId);
    }

    /**
     * Get active ledger for a client (if exists)
     */
    @Transactional(readOnly = true)
    public Optional<FinancialLedger> getActiveClientLedger(ClientId clientId) {
        return ledgerRepository.findByClientIdAndStatus(clientId, LedgerStatus.ACTIVE);
    }

    /**
     * Find or create an active ledger for a client
     */
    public FinancialLedger getOrCreateActiveLedger(ClientId clientId, ProgramEnrollmentId enrollmentId,
                                                 UUID householdId, boolean isVawaProtected, String createdBy) {
        Optional<FinancialLedger> existingLedger = getActiveClientLedger(clientId);
        if (existingLedger.isPresent()) {
            return existingLedger.get();
        }

        String ledgerName = "Financial Assistance Ledger - " + clientId.value();
        FinancialLedgerId ledgerId = createLedger(clientId, enrollmentId, householdId,
                                                ledgerName, isVawaProtected, createdBy);
        return ledgerRepository.findById(ledgerId)
            .orElseThrow(() -> new IllegalStateException("Failed to create ledger"));
    }

    /**
     * Close a ledger
     */
    public void closeLedger(FinancialLedgerId ledgerId, String reason, String closedBy) {
        FinancialLedger ledger = ledgerRepository.findById(ledgerId)
            .orElseThrow(() -> new IllegalArgumentException("Ledger not found: " + ledgerId));

        ledger.closeLedger(reason, closedBy);
        ledgerRepository.save(ledger);
    }

    /**
     * Find unbalanced ledgers for audit purposes
     */
    @Transactional(readOnly = true)
    public List<FinancialLedger> findUnbalancedLedgers() {
        return ledgerRepository.findUnbalancedLedgers();
    }

    /**
     * Find ledgers with overdue arrears
     */
    @Transactional(readOnly = true)
    public List<FinancialLedger> findLedgersWithOverdueArrears() {
        return ledgerRepository.findLedgersWithOverdueArrears();
    }

    /**
     * Find ledgers with unmatched deposits
     */
    @Transactional(readOnly = true)
    public List<FinancialLedger> findLedgersWithUnmatchedDeposits() {
        return ledgerRepository.findLedgersWithUnmatchedDeposits();
    }
}
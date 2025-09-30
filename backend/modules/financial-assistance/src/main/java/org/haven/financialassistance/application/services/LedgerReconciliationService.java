package org.haven.financialassistance.application.services;

import org.haven.financialassistance.domain.ledger.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for reconciling financial ledger data against external accounting system exports
 */
@Service
@Transactional(readOnly = true)
public class LedgerReconciliationService {

    private final FinancialLedgerRepository ledgerRepository;

    public LedgerReconciliationService(FinancialLedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    /**
     * Reconcile ledger data against accounting system export
     */
    public ReconciliationReport reconcileWithAccountingExport(AccountingExportData exportData, LocalDate reconciliationDate) {
        List<ReconciliationDiscrepancy> discrepancies = new ArrayList<>();

        // Get all ledgers that should be included in reconciliation
        List<FinancialLedger> ledgers = getAllActiveLedgersForReconciliation(reconciliationDate);

        // Check for unmatched payments in ledgers
        for (FinancialLedger ledger : ledgers) {
            List<ReconciliationDiscrepancy> ledgerDiscrepancies = findLedgerDiscrepancies(ledger, exportData);
            discrepancies.addAll(ledgerDiscrepancies);
        }

        // Check for unmatched deposits in export data
        List<ReconciliationDiscrepancy> depositDiscrepancies = findUnmatchedDeposits(exportData, ledgers);
        discrepancies.addAll(depositDiscrepancies);

        // Check for balance discrepancies
        List<ReconciliationDiscrepancy> balanceDiscrepancies = findBalanceDiscrepancies(ledgers, exportData);
        discrepancies.addAll(balanceDiscrepancies);

        return new ReconciliationReport(
            reconciliationDate,
            ledgers.size(),
            exportData.getTotalTransactions(),
            discrepancies,
            calculateTotalDiscrepancyAmount(discrepancies),
            discrepancies.isEmpty()
        );
    }

    /**
     * Generate daily reconciliation report
     */
    public DailyReconciliationSummary generateDailyReconciliation(LocalDate date) {
        List<FinancialLedger> unbalancedLedgers = ledgerRepository.findUnbalancedLedgers();
        List<FinancialLedger> overdueArrears = ledgerRepository.findLedgersWithOverdueArrears();
        List<FinancialLedger> unmatchedDeposits = ledgerRepository.findLedgersWithUnmatchedDeposits();

        BigDecimal totalUnbalancedAmount = unbalancedLedgers.stream()
            .map(l -> l.getTotalDebits().subtract(l.getTotalCredits()).abs())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DailyReconciliationSummary(
            date,
            unbalancedLedgers.size(),
            totalUnbalancedAmount,
            overdueArrears.size(),
            unmatchedDeposits.size(),
            calculateOverdueArrearsTotal(overdueArrears),
            calculateUnmatchedDepositsTotal(unmatchedDeposits)
        );
    }

    /**
     * Find discrepancies for a specific funding source
     */
    public FundingSourceReconciliation reconcileFundingSource(String fundingSourceCode, LocalDate startDate, LocalDate endDate) {
        List<FinancialLedger> fundingSourceLedgers = ledgerRepository.findByFundingSourceCode(fundingSourceCode);

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        List<String> transactionIds = new ArrayList<>();

        for (FinancialLedger ledger : fundingSourceLedgers) {
            for (LedgerEntry entry : ledger.getEntries()) {
                if (isFundingSourceMatch(entry, fundingSourceCode) &&
                    isInDateRange(entry, startDate, endDate)) {

                    if (entry.getEntryType() == EntryType.DEBIT) {
                        totalDebits = totalDebits.add(entry.getAmount());
                    } else {
                        totalCredits = totalCredits.add(entry.getAmount());
                    }

                    transactionIds.add(entry.getTransactionId());
                }
            }
        }

        return new FundingSourceReconciliation(
            fundingSourceCode,
            startDate,
            endDate,
            totalDebits,
            totalCredits,
            totalCredits.subtract(totalDebits), // Net funding received
            transactionIds.size(),
            transactionIds
        );
    }

    private List<FinancialLedger> getAllActiveLedgersForReconciliation(LocalDate reconciliationDate) {
        // TODO: Implement based on your business rules for which ledgers to include
        return ledgerRepository.findByFundingSourceCode(null); // Placeholder
    }

    private List<ReconciliationDiscrepancy> findLedgerDiscrepancies(FinancialLedger ledger, AccountingExportData exportData) {
        List<ReconciliationDiscrepancy> discrepancies = new ArrayList<>();

        for (LedgerEntry entry : ledger.getEntries()) {
            if (entry.getEntryType() == EntryType.CREDIT) { // Focus on payments out
                String transactionId = entry.getTransactionId();
                if (!exportData.hasTransaction(transactionId)) {
                    discrepancies.add(new ReconciliationDiscrepancy(
                        DiscrepancyType.MISSING_IN_EXPORT,
                        ledger.getId().value(),
                        transactionId,
                        entry.getAmount(),
                        "Transaction found in ledger but missing from accounting export",
                        entry.getRecordedAt().atZone(ZoneId.systemDefault()).toLocalDate()
                    ));
                } else {
                    // Check amount matches
                    BigDecimal exportAmount = exportData.getTransactionAmount(transactionId);
                    if (entry.getAmount().compareTo(exportAmount) != 0) {
                        discrepancies.add(new ReconciliationDiscrepancy(
                            DiscrepancyType.AMOUNT_MISMATCH,
                            ledger.getId().value(),
                            transactionId,
                            entry.getAmount().subtract(exportAmount),
                            String.format("Amount mismatch - Ledger: %s, Export: %s",
                                        entry.getAmount(), exportAmount),
                            entry.getRecordedAt().atZone(ZoneId.systemDefault()).toLocalDate()
                        ));
                    }
                }
            }
        }

        return discrepancies;
    }

    private List<ReconciliationDiscrepancy> findUnmatchedDeposits(AccountingExportData exportData, List<FinancialLedger> ledgers) {
        List<ReconciliationDiscrepancy> discrepancies = new ArrayList<>();

        // Get all transaction IDs from ledgers
        List<String> ledgerTransactionIds = ledgers.stream()
            .flatMap(ledger -> ledger.getEntries().stream())
            .map(LedgerEntry::getTransactionId)
            .distinct()
            .toList();

        // Find transactions in export that aren't in any ledger
        for (String exportTransactionId : exportData.getTransactionIds()) {
            if (!ledgerTransactionIds.contains(exportTransactionId)) {
                discrepancies.add(new ReconciliationDiscrepancy(
                    DiscrepancyType.MISSING_IN_LEDGER,
                    null,
                    exportTransactionId,
                    exportData.getTransactionAmount(exportTransactionId),
                    "Transaction found in accounting export but missing from ledgers",
                    exportData.getTransactionDate(exportTransactionId)
                ));
            }
        }

        return discrepancies;
    }

    private List<ReconciliationDiscrepancy> findBalanceDiscrepancies(List<FinancialLedger> ledgers, AccountingExportData exportData) {
        List<ReconciliationDiscrepancy> discrepancies = new ArrayList<>();

        for (FinancialLedger ledger : ledgers) {
            if (!ledger.isBalanced()) {
                BigDecimal imbalance = ledger.getTotalDebits().subtract(ledger.getTotalCredits());
                discrepancies.add(new ReconciliationDiscrepancy(
                    DiscrepancyType.LEDGER_IMBALANCE,
                    ledger.getId().value(),
                    "BALANCE_CHECK",
                    imbalance,
                    String.format("Ledger is not balanced - Imbalance: %s", imbalance),
                    LocalDate.now()
                ));
            }
        }

        return discrepancies;
    }

    private boolean isFundingSourceMatch(LedgerEntry entry, String fundingSourceCode) {
        return fundingSourceCode.equals(entry.getFundingSourceCode());
    }

    private boolean isInDateRange(LedgerEntry entry, LocalDate startDate, LocalDate endDate) {
        LocalDate entryDate = entry.getRecordedAt().atZone(ZoneId.systemDefault()).toLocalDate();
        return !entryDate.isBefore(startDate) && !entryDate.isAfter(endDate);
    }

    private BigDecimal calculateTotalDiscrepancyAmount(List<ReconciliationDiscrepancy> discrepancies) {
        return discrepancies.stream()
            .map(ReconciliationDiscrepancy::amount)
            .map(BigDecimal::abs)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateOverdueArrearsTotal(List<FinancialLedger> overdueArrears) {
        // TODO: Implement calculation of total overdue arrears
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateUnmatchedDepositsTotal(List<FinancialLedger> unmatchedDeposits) {
        // TODO: Implement calculation of total unmatched deposits
        return BigDecimal.ZERO;
    }

    // Data classes for reconciliation results
    public record ReconciliationReport(
        LocalDate reconciliationDate,
        int totalLedgers,
        int totalExportTransactions,
        List<ReconciliationDiscrepancy> discrepancies,
        BigDecimal totalDiscrepancyAmount,
        boolean isBalanced
    ) {}

    public record ReconciliationDiscrepancy(
        DiscrepancyType type,
        java.util.UUID ledgerId,
        String transactionId,
        BigDecimal amount,
        String description,
        LocalDate transactionDate
    ) {}

    public enum DiscrepancyType {
        MISSING_IN_EXPORT,
        MISSING_IN_LEDGER,
        AMOUNT_MISMATCH,
        LEDGER_IMBALANCE,
        DUPLICATE_TRANSACTION
    }

    public record DailyReconciliationSummary(
        LocalDate date,
        int unbalancedLedgerCount,
        BigDecimal totalUnbalancedAmount,
        int overdueArrearsCount,
        int unmatchedDepositsCount,
        BigDecimal totalOverdueArrears,
        BigDecimal totalUnmatchedDeposits
    ) {}

    public record FundingSourceReconciliation(
        String fundingSourceCode,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalDebits,
        BigDecimal totalCredits,
        BigDecimal netFunding,
        int transactionCount,
        List<String> transactionIds
    ) {}

    // Placeholder for accounting export data interface
    public interface AccountingExportData {
        boolean hasTransaction(String transactionId);
        BigDecimal getTransactionAmount(String transactionId);
        LocalDate getTransactionDate(String transactionId);
        List<String> getTransactionIds();
        int getTotalTransactions();
    }
}
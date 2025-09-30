package org.haven.financialassistance.application.services;

import org.haven.financialassistance.domain.ledger.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for automated alerts for overdue arrears, unmatched deposits, and financial discrepancies
 */
@Service
@Transactional
public class FinancialAlertsService {

    private final FinancialLedgerRepository ledgerRepository;
    private final NotificationService notificationService;

    public FinancialAlertsService(FinancialLedgerRepository ledgerRepository,
                                NotificationService notificationService) {
        this.ledgerRepository = ledgerRepository;
        this.notificationService = notificationService;
    }

    /**
     * Run daily automated alerts check - executes at 8 AM every day
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void runDailyAlertsCheck() {
        generateOverdueArrearsAlerts();
        generateUnmatchedDepositsAlerts();
        generateUnbalancedLedgerAlerts();
        generateLargeDisbursementAlerts();
        generateVawaComplianceAlerts();
    }

    /**
     * Generate alerts for overdue arrears
     */
    public List<FinancialAlert> generateOverdueArrearsAlerts() {
        List<FinancialAlert> alerts = new ArrayList<>();
        List<FinancialLedger> ledgersWithOverdueArrears = ledgerRepository.findLedgersWithOverdueArrears();

        for (FinancialLedger ledger : ledgersWithOverdueArrears) {
            List<OverdueArrearsDetail> overdueDetails = findOverdueArrearsInLedger(ledger);

            if (!overdueDetails.isEmpty()) {
                BigDecimal totalOverdue = overdueDetails.stream()
                    .map(OverdueArrearsDetail::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                FinancialAlert alert = new FinancialAlert(
                    AlertType.OVERDUE_ARREARS,
                    AlertSeverity.HIGH,
                    ledger.getClientId().value(),
                    ledger.getId().value(),
                    "Overdue Arrears Detected",
                    String.format("Client has $%s in overdue arrears across %d items. Immediate attention required.",
                                totalOverdue, overdueDetails.size()),
                    totalOverdue,
                    LocalDate.now(),
                    overdueDetails
                );

                alerts.add(alert);
                sendAlert(alert);
            }
        }

        return alerts;
    }

    /**
     * Generate alerts for unmatched deposits
     */
    public List<FinancialAlert> generateUnmatchedDepositsAlerts() {
        List<FinancialAlert> alerts = new ArrayList<>();
        List<FinancialLedger> ledgersWithUnmatchedDeposits = ledgerRepository.findLedgersWithUnmatchedDeposits();

        for (FinancialLedger ledger : ledgersWithUnmatchedDeposits) {
            List<UnmatchedDepositDetail> unmatchedDetails = findUnmatchedDepositsInLedger(ledger);

            if (!unmatchedDetails.isEmpty()) {
                BigDecimal totalUnmatched = unmatchedDetails.stream()
                    .map(UnmatchedDepositDetail::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                FinancialAlert alert = new FinancialAlert(
                    AlertType.UNMATCHED_DEPOSITS,
                    AlertSeverity.MEDIUM,
                    ledger.getClientId().value(),
                    ledger.getId().value(),
                    "Unmatched Deposits Found",
                    String.format("$%s in deposits have not been matched to expenses after 30+ days.",
                                totalUnmatched),
                    totalUnmatched,
                    LocalDate.now(),
                    unmatchedDetails
                );

                alerts.add(alert);
                sendAlert(alert);
            }
        }

        return alerts;
    }

    /**
     * Generate alerts for unbalanced ledgers
     */
    public List<FinancialAlert> generateUnbalancedLedgerAlerts() {
        List<FinancialAlert> alerts = new ArrayList<>();
        List<FinancialLedger> unbalancedLedgers = ledgerRepository.findUnbalancedLedgers();

        for (FinancialLedger ledger : unbalancedLedgers) {
            BigDecimal imbalance = ledger.getTotalDebits().subtract(ledger.getTotalCredits()).abs();

            FinancialAlert alert = new FinancialAlert(
                AlertType.LEDGER_IMBALANCE,
                AlertSeverity.CRITICAL,
                ledger.getClientId().value(),
                ledger.getId().value(),
                "Ledger Imbalance Detected",
                String.format("Ledger is out of balance by $%s. Immediate reconciliation required.", imbalance),
                imbalance,
                LocalDate.now(),
                List.of(new LedgerImbalanceDetail(
                    ledger.getTotalDebits(),
                    ledger.getTotalCredits(),
                    imbalance,
                    ledger.getLastModified().atZone(ZoneId.systemDefault()).toLocalDate()
                ))
            );

            alerts.add(alert);
            sendAlert(alert);
        }

        return alerts;
    }

    /**
     * Generate alerts for large disbursements requiring additional approval
     */
    public List<FinancialAlert> generateLargeDisbursementAlerts() {
        List<FinancialAlert> alerts = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate oneWeekAgo = today.minusDays(7);

        // TODO: Query for large disbursements made in the last week
        // This would typically involve checking recent transactions above a threshold
        BigDecimal largeDisbursementThreshold = new BigDecimal("5000.00");

        List<FinancialLedger> allLedgers = ledgerRepository.findByFundingSourceCode(null); // Placeholder

        for (FinancialLedger ledger : allLedgers) {
            List<LargeDisbursementDetail> largeTransactions = findLargeRecentTransactions(
                ledger, largeDisbursementThreshold, oneWeekAgo, today
            );

            if (!largeTransactions.isEmpty()) {
                BigDecimal totalLarge = largeTransactions.stream()
                    .map(LargeDisbursementDetail::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                FinancialAlert alert = new FinancialAlert(
                    AlertType.LARGE_DISBURSEMENT,
                    AlertSeverity.MEDIUM,
                    ledger.getClientId().value(),
                    ledger.getId().value(),
                    "Large Disbursements Detected",
                    String.format("$%s in large disbursements (>$%s) made in the last 7 days.",
                                totalLarge, largeDisbursementThreshold),
                    totalLarge,
                    LocalDate.now(),
                    largeTransactions
                );

                alerts.add(alert);
                sendAlert(alert);
            }
        }

        return alerts;
    }

    /**
     * Generate alerts for VAWA compliance issues
     */
    public List<FinancialAlert> generateVawaComplianceAlerts() {
        List<FinancialAlert> alerts = new ArrayList<>();

        // Find VAWA-protected ledgers that might have compliance issues
        List<FinancialLedger> vawaProtectedLedgers = ledgerRepository.findByFundingSourceCode(null)
            .stream()
            .filter(FinancialLedger::isVawaProtected)
            .toList();

        for (FinancialLedger ledger : vawaProtectedLedgers) {
            List<VawaComplianceIssue> complianceIssues = checkVawaCompliance(ledger);

            if (!complianceIssues.isEmpty()) {
                FinancialAlert alert = new FinancialAlert(
                    AlertType.VAWA_COMPLIANCE,
                    AlertSeverity.HIGH,
                    ledger.getClientId().value(),
                    ledger.getId().value(),
                    "VAWA Compliance Issues",
                    String.format("Found %d potential VAWA compliance issues requiring review.",
                                complianceIssues.size()),
                    BigDecimal.ZERO,
                    LocalDate.now(),
                    complianceIssues
                );

                alerts.add(alert);
                sendAlert(alert);
            }
        }

        return alerts;
    }

    /**
     * Generate custom alert for specific conditions
     */
    public FinancialAlert generateCustomAlert(UUID clientId, UUID ledgerId, String title, String message,
                                            AlertType type, AlertSeverity severity, BigDecimal amount) {
        FinancialAlert alert = new FinancialAlert(
            type,
            severity,
            clientId,
            ledgerId,
            title,
            message,
            amount,
            LocalDate.now(),
            List.of()
        );

        sendAlert(alert);
        return alert;
    }

    private List<OverdueArrearsDetail> findOverdueArrearsInLedger(FinancialLedger ledger) {
        List<OverdueArrearsDetail> overdue = new ArrayList<>();
        LocalDate cutoffDate = LocalDate.now().minusDays(30); // Consider 30+ days as overdue

        for (LedgerEntry entry : ledger.getEntries()) {
            if (isArrearsEntry(entry) && entry.getRecordedAt().atZone(ZoneId.systemDefault()).toLocalDate().isBefore(cutoffDate)) {
                long daysOverdue = ChronoUnit.DAYS.between(entry.getRecordedAt().atZone(ZoneId.systemDefault()).toLocalDate(), LocalDate.now());
                overdue.add(new OverdueArrearsDetail(
                    entry.getTransactionId(),
                    entry.getAmount(),
                    entry.getPayeeName(),
                    entry.getRecordedAt().atZone(ZoneId.systemDefault()).toLocalDate(),
                    daysOverdue,
                    getArrearsType(entry)
                ));
            }
        }

        return overdue;
    }

    private List<UnmatchedDepositDetail> findUnmatchedDepositsInLedger(FinancialLedger ledger) {
        List<UnmatchedDepositDetail> unmatched = new ArrayList<>();
        LocalDate cutoffDate = LocalDate.now().minusDays(30);

        for (LedgerEntry entry : ledger.getEntries()) {
            if (isDepositEntry(entry) && entry.getRecordedAt().atZone(ZoneId.systemDefault()).toLocalDate().isBefore(cutoffDate)) {
                // TODO: Check if deposit has been matched to expenses
                boolean isMatched = checkIfDepositIsMatched(entry, ledger);
                if (!isMatched) {
                    long daysUnmatched = ChronoUnit.DAYS.between(entry.getRecordedAt().atZone(ZoneId.systemDefault()).toLocalDate(), LocalDate.now());
                    unmatched.add(new UnmatchedDepositDetail(
                        entry.getTransactionId(),
                        entry.getAmount(),
                        entry.getFundingSourceCode(),
                        entry.getRecordedAt().atZone(ZoneId.systemDefault()).toLocalDate(),
                        daysUnmatched
                    ));
                }
            }
        }

        return unmatched;
    }

    private List<LargeDisbursementDetail> findLargeRecentTransactions(FinancialLedger ledger,
                                                                     BigDecimal threshold,
                                                                     LocalDate startDate, LocalDate endDate) {
        List<LargeDisbursementDetail> large = new ArrayList<>();

        for (LedgerEntry entry : ledger.getEntries()) {
            LocalDate entryDate = entry.getRecordedAt().atZone(ZoneId.systemDefault()).toLocalDate();
            if (entry.getEntryType() == EntryType.DEBIT &&
                entry.getAmount().compareTo(threshold) > 0 &&
                !entryDate.isBefore(startDate) && !entryDate.isAfter(endDate)) {

                large.add(new LargeDisbursementDetail(
                    entry.getTransactionId(),
                    entry.getAmount(),
                    entry.getPayeeName(),
                    entry.getRecordedBy(),
                    entryDate
                ));
            }
        }

        return large;
    }

    private List<VawaComplianceIssue> checkVawaCompliance(FinancialLedger ledger) {
        List<VawaComplianceIssue> issues = new ArrayList<>();

        // TODO: Implement VAWA compliance checks such as:
        // - Verifying proper redaction levels
        // - Checking for unauthorized access to protected data
        // - Ensuring landlord communications are properly sanitized
        // - Validating document redaction

        return issues;
    }

    private boolean isArrearsEntry(LedgerEntry entry) {
        return entry.getDescription() != null &&
               entry.getDescription().toLowerCase().contains("arrears");
    }

    private boolean isDepositEntry(LedgerEntry entry) {
        return entry.getEntryType() == EntryType.CREDIT &&
               entry.getAccountClassification() == AccountClassification.FUNDING_LIABILITY;
    }

    private boolean checkIfDepositIsMatched(LedgerEntry depositEntry, FinancialLedger ledger) {
        // TODO: Implement logic to check if a deposit has been matched to corresponding expenses
        return false; // Placeholder
    }

    private ArrearsType getArrearsType(LedgerEntry entry) {
        if (entry.getDescription().toLowerCase().contains("rent")) {
            return ArrearsType.RENT;
        } else if (entry.getDescription().toLowerCase().contains("utility")) {
            return ArrearsType.UTILITY;
        }
        return ArrearsType.RENT; // Default
    }

    private void sendAlert(FinancialAlert alert) {
        // Determine recipients based on alert type and severity
        Set<String> recipients = determineAlertRecipients(alert);

        for (String recipient : recipients) {
            notificationService.sendFinancialAlert(recipient, alert);
        }
    }

    private Set<String> determineAlertRecipients(FinancialAlert alert) {
        // TODO: Implement logic to determine who should receive each type of alert
        return Set.of("financial-admin@organization.com", "case-manager@organization.com");
    }

    // Alert data classes
    public record FinancialAlert(
        AlertType type,
        AlertSeverity severity,
        UUID clientId,
        UUID ledgerId,
        String title,
        String message,
        BigDecimal amount,
        LocalDate alertDate,
        List<?> details
    ) {}

    public enum AlertType {
        OVERDUE_ARREARS,
        UNMATCHED_DEPOSITS,
        LEDGER_IMBALANCE,
        LARGE_DISBURSEMENT,
        VAWA_COMPLIANCE,
        FRAUD_DETECTION,
        BUDGET_EXCEEDED
    }

    public enum AlertSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public record OverdueArrearsDetail(
        String transactionId,
        BigDecimal amount,
        String payeeName,
        LocalDate dueDate,
        long daysOverdue,
        ArrearsType arrearsType
    ) {}

    public record UnmatchedDepositDetail(
        String transactionId,
        BigDecimal amount,
        String fundingSource,
        LocalDate depositDate,
        long daysUnmatched
    ) {}

    public record LedgerImbalanceDetail(
        BigDecimal totalDebits,
        BigDecimal totalCredits,
        BigDecimal imbalanceAmount,
        LocalDate lastModified
    ) {}

    public record LargeDisbursementDetail(
        String transactionId,
        BigDecimal amount,
        String payeeName,
        String authorizedBy,
        LocalDate transactionDate
    ) {}

    public record VawaComplianceIssue(
        String issueType,
        String description,
        String recommendation,
        LocalDate identifiedDate
    ) {}

    // Placeholder notification service interface
    public interface NotificationService {
        void sendFinancialAlert(String recipient, FinancialAlert alert);
    }
}
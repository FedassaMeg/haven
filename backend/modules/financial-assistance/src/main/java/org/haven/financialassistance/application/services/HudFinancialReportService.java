package org.haven.financialassistance.application.services;

import org.haven.financialassistance.domain.ledger.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating HUD-compliant financial reports from ledger data
 */
@Service
@Transactional(readOnly = true)
public class HudFinancialReportService {

    private final FinancialLedgerRepository ledgerRepository;

    public HudFinancialReportService(FinancialLedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    /**
     * Generate HUD Annual Performance Report (APR) financial data
     */
    public HudAprFinancialReport generateAprFinancialReport(LocalDate reportStartDate, LocalDate reportEndDate) {
        List<FinancialLedger> ledgers = getAllLedgersInPeriod(reportStartDate, reportEndDate);

        Map<String, BigDecimal> categoryTotals = calculateHudCategoryTotals(ledgers, reportStartDate, reportEndDate);
        Map<String, Integer> categoryClientCounts = calculateHudCategoryClientCounts(ledgers, reportStartDate, reportEndDate);

        return new HudAprFinancialReport(
            reportStartDate,
            reportEndDate,
            categoryTotals,
            categoryClientCounts,
            calculateTotalExpended(categoryTotals),
            calculateUniqueClientsServed(ledgers)
        );
    }

    /**
     * Generate HUD Rental Assistance Report
     */
    public HudRentalAssistanceReport generateRentalAssistanceReport(LocalDate reportStartDate, LocalDate reportEndDate) {
        List<FinancialLedger> ledgers = getAllLedgersInPeriod(reportStartDate, reportEndDate);

        BigDecimal totalRentPayments = calculateCategoryTotal(ledgers, "4.02", reportStartDate, reportEndDate);
        BigDecimal totalUtilityPayments = calculateCategoryTotal(ledgers, "4.03", reportStartDate, reportEndDate);
        BigDecimal totalSecurityDeposits = calculateCategoryTotal(ledgers, "4.04", reportStartDate, reportEndDate);
        BigDecimal totalMovingCosts = calculateCategoryTotal(ledgers, "4.05", reportStartDate, reportEndDate);

        int householdsWithRentalAssistance = countHouseholdsWithCategory(ledgers, "4.02", reportStartDate, reportEndDate);
        int householdsWithUtilityAssistance = countHouseholdsWithCategory(ledgers, "4.03", reportStartDate, reportEndDate);

        List<HudRentalAssistanceDetail> details = generateRentalAssistanceDetails(ledgers, reportStartDate, reportEndDate);

        return new HudRentalAssistanceReport(
            reportStartDate,
            reportEndDate,
            totalRentPayments,
            totalUtilityPayments,
            totalSecurityDeposits,
            totalMovingCosts,
            householdsWithRentalAssistance,
            householdsWithUtilityAssistance,
            details
        );
    }

    /**
     * Generate HUD Financial Assistance Summary by funding source
     */
    public HudFundingSourceReport generateFundingSourceReport(String fundingSourceCode,
                                                            LocalDate reportStartDate, LocalDate reportEndDate) {
        List<FinancialLedger> fundingSourceLedgers = ledgerRepository.findByFundingSourceCode(fundingSourceCode);

        BigDecimal totalExpended = BigDecimal.ZERO;
        BigDecimal totalReceived = BigDecimal.ZERO;
        Map<String, BigDecimal> categoryBreakdown = new HashMap<>();
        Set<UUID> uniqueHouseholds = new HashSet<>();

        for (FinancialLedger ledger : fundingSourceLedgers) {
            uniqueHouseholds.add(ledger.getHouseholdId());

            for (LedgerEntry entry : ledger.getEntries()) {
                if (isInDateRange(entry, reportStartDate, reportEndDate) &&
                    isFundingSourceMatch(entry, fundingSourceCode)) {

                    if (entry.getEntryType() == EntryType.DEBIT) {
                        totalExpended = totalExpended.add(entry.getAmount());

                        String category = entry.getHudCategoryCode() != null ? entry.getHudCategoryCode() : "OTHER";
                        categoryBreakdown.merge(category, entry.getAmount(), BigDecimal::add);
                    } else if (entry.getEntryType() == EntryType.CREDIT &&
                              entry.getAccountClassification() == AccountClassification.FUNDING_LIABILITY) {
                        totalReceived = totalReceived.add(entry.getAmount());
                    }
                }
            }
        }

        return new HudFundingSourceReport(
            fundingSourceCode,
            reportStartDate,
            reportEndDate,
            totalReceived,
            totalExpended,
            totalReceived.subtract(totalExpended), // Remaining balance
            uniqueHouseholds.size(),
            categoryBreakdown
        );
    }

    /**
     * Generate HUD CAPER (Consolidated Annual Performance and Evaluation Report) data
     */
    public HudCaperFinancialData generateCaperFinancialData(LocalDate reportStartDate, LocalDate reportEndDate) {
        List<FinancialLedger> ledgers = getAllLedgersInPeriod(reportStartDate, reportEndDate);

        return new HudCaperFinancialData(
            reportStartDate,
            reportEndDate,
            calculateCaperExpenseCategories(ledgers, reportStartDate, reportEndDate),
            calculateCaperClientMetrics(ledgers, reportStartDate, reportEndDate),
            calculateCaperOutcomeMetrics(ledgers, reportStartDate, reportEndDate)
        );
    }

    /**
     * Generate arrears analysis report for HUD compliance
     */
    public HudArrearsAnalysisReport generateArrearsAnalysisReport(LocalDate reportStartDate, LocalDate reportEndDate) {
        List<FinancialLedger> ledgers = getAllLedgersInPeriod(reportStartDate, reportEndDate);

        BigDecimal totalArrearsPayments = BigDecimal.ZERO;
        int householdsWithArrears = 0;
        Map<ArrearsType, BigDecimal> arrearsTypeBreakdown = new HashMap<>();
        Map<Integer, BigDecimal> arrearsAgeBreakdown = new HashMap<>(); // Age in months

        for (FinancialLedger ledger : ledgers) {
            boolean hasArrears = false;

            for (LedgerEntry entry : ledger.getEntries()) {
                if (isInDateRange(entry, reportStartDate, reportEndDate) && isArrearsEntry(entry)) {
                    totalArrearsPayments = totalArrearsPayments.add(entry.getAmount());
                    hasArrears = true;

                    // Categorize by arrears type
                    ArrearsType arrearsType = getArrearsType(entry);
                    arrearsTypeBreakdown.merge(arrearsType, entry.getAmount(), BigDecimal::add);

                    // Categorize by age
                    int ageInMonths = calculateArrearsAge(entry);
                    arrearsAgeBreakdown.merge(ageInMonths, entry.getAmount(), BigDecimal::add);
                }
            }

            if (hasArrears) {
                householdsWithArrears++;
            }
        }

        return new HudArrearsAnalysisReport(
            reportStartDate,
            reportEndDate,
            totalArrearsPayments,
            householdsWithArrears,
            arrearsTypeBreakdown,
            arrearsAgeBreakdown
        );
    }

    private List<FinancialLedger> getAllLedgersInPeriod(LocalDate startDate, LocalDate endDate) {
        // TODO: Implement repository method to find ledgers with transactions in date range
        return new ArrayList<>();
    }

    private Map<String, BigDecimal> calculateHudCategoryTotals(List<FinancialLedger> ledgers,
                                                              LocalDate startDate, LocalDate endDate) {
        Map<String, BigDecimal> totals = new HashMap<>();

        for (FinancialLedger ledger : ledgers) {
            for (LedgerEntry entry : ledger.getEntries()) {
                if (isInDateRange(entry, startDate, endDate) &&
                    entry.getEntryType() == EntryType.DEBIT &&
                    entry.getHudCategoryCode() != null) {

                    totals.merge(entry.getHudCategoryCode(), entry.getAmount(), BigDecimal::add);
                }
            }
        }

        return totals;
    }

    private Map<String, Integer> calculateHudCategoryClientCounts(List<FinancialLedger> ledgers,
                                                                 LocalDate startDate, LocalDate endDate) {
        Map<String, Set<UUID>> categoryClients = new HashMap<>();

        for (FinancialLedger ledger : ledgers) {
            for (LedgerEntry entry : ledger.getEntries()) {
                if (isInDateRange(entry, startDate, endDate) &&
                    entry.getEntryType() == EntryType.DEBIT &&
                    entry.getHudCategoryCode() != null) {

                    categoryClients.computeIfAbsent(entry.getHudCategoryCode(), k -> new HashSet<>())
                                  .add(ledger.getClientId().value());
                }
            }
        }

        return categoryClients.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().size()
            ));
    }

    private BigDecimal calculateCategoryTotal(List<FinancialLedger> ledgers, String hudCategoryCode,
                                            LocalDate startDate, LocalDate endDate) {
        return ledgers.stream()
            .flatMap(ledger -> ledger.getEntries().stream())
            .filter(entry -> isInDateRange(entry, startDate, endDate) &&
                           entry.getEntryType() == EntryType.DEBIT &&
                           hudCategoryCode.equals(entry.getHudCategoryCode()))
            .map(LedgerEntry::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int countHouseholdsWithCategory(List<FinancialLedger> ledgers, String hudCategoryCode,
                                          LocalDate startDate, LocalDate endDate) {
        return (int) ledgers.stream()
            .filter(ledger -> ledger.getEntries().stream()
                .anyMatch(entry -> isInDateRange(entry, startDate, endDate) &&
                                 entry.getEntryType() == EntryType.DEBIT &&
                                 hudCategoryCode.equals(entry.getHudCategoryCode())))
            .count();
    }

    private boolean isInDateRange(LedgerEntry entry, LocalDate startDate, LocalDate endDate) {
        LocalDate entryDate = entry.getRecordedAt().atZone(ZoneId.systemDefault()).toLocalDate();
        return !entryDate.isBefore(startDate) && !entryDate.isAfter(endDate);
    }

    private boolean isFundingSourceMatch(LedgerEntry entry, String fundingSourceCode) {
        return fundingSourceCode.equals(entry.getFundingSourceCode());
    }

    private boolean isArrearsEntry(LedgerEntry entry) {
        return entry.getDescription() != null &&
               entry.getDescription().toLowerCase().contains("arrears");
    }

    private ArrearsType getArrearsType(LedgerEntry entry) {
        if (entry.getDescription().toLowerCase().contains("rent")) {
            return ArrearsType.RENT;
        } else if (entry.getDescription().toLowerCase().contains("utility")) {
            return ArrearsType.UTILITY;
        }
        return ArrearsType.RENT; // Default
    }

    private int calculateArrearsAge(LedgerEntry entry) {
        // TODO: Implement logic to calculate how old the arrears are
        // This would typically involve comparing period dates to payment dates
        return 1; // Placeholder
    }

    private BigDecimal calculateTotalExpended(Map<String, BigDecimal> categoryTotals) {
        return categoryTotals.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int calculateUniqueClientsServed(List<FinancialLedger> ledgers) {
        return (int) ledgers.stream()
            .map(ledger -> ledger.getClientId().value())
            .distinct()
            .count();
    }

    private List<HudRentalAssistanceDetail> generateRentalAssistanceDetails(List<FinancialLedger> ledgers,
                                                                           LocalDate startDate, LocalDate endDate) {
        // TODO: Implement detailed rental assistance breakdown
        return new ArrayList<>();
    }

    private Map<String, BigDecimal> calculateCaperExpenseCategories(List<FinancialLedger> ledgers,
                                                                   LocalDate startDate, LocalDate endDate) {
        // TODO: Implement CAPER-specific expense categorization
        return new HashMap<>();
    }

    private Map<String, Integer> calculateCaperClientMetrics(List<FinancialLedger> ledgers,
                                                           LocalDate startDate, LocalDate endDate) {
        // TODO: Implement CAPER client metrics
        return new HashMap<>();
    }

    private Map<String, BigDecimal> calculateCaperOutcomeMetrics(List<FinancialLedger> ledgers,
                                                               LocalDate startDate, LocalDate endDate) {
        // TODO: Implement CAPER outcome metrics
        return new HashMap<>();
    }

    // Report data classes
    public record HudAprFinancialReport(
        LocalDate reportStartDate,
        LocalDate reportEndDate,
        Map<String, BigDecimal> categoryTotals,
        Map<String, Integer> categoryClientCounts,
        BigDecimal totalExpended,
        int uniqueClientsServed
    ) {}

    public record HudRentalAssistanceReport(
        LocalDate reportStartDate,
        LocalDate reportEndDate,
        BigDecimal totalRentPayments,
        BigDecimal totalUtilityPayments,
        BigDecimal totalSecurityDeposits,
        BigDecimal totalMovingCosts,
        int householdsWithRentalAssistance,
        int householdsWithUtilityAssistance,
        List<HudRentalAssistanceDetail> details
    ) {}

    public record HudRentalAssistanceDetail(
        UUID clientId,
        UUID householdId,
        String hudCategoryCode,
        BigDecimal amount,
        LocalDate serviceDate,
        String payeeName
    ) {}

    public record HudFundingSourceReport(
        String fundingSourceCode,
        LocalDate reportStartDate,
        LocalDate reportEndDate,
        BigDecimal totalReceived,
        BigDecimal totalExpended,
        BigDecimal remainingBalance,
        int householdsServed,
        Map<String, BigDecimal> categoryBreakdown
    ) {}

    public record HudCaperFinancialData(
        LocalDate reportStartDate,
        LocalDate reportEndDate,
        Map<String, BigDecimal> expenseCategories,
        Map<String, Integer> clientMetrics,
        Map<String, BigDecimal> outcomeMetrics
    ) {}

    public record HudArrearsAnalysisReport(
        LocalDate reportStartDate,
        LocalDate reportEndDate,
        BigDecimal totalArrearsPayments,
        int householdsWithArrears,
        Map<ArrearsType, BigDecimal> arrearsTypeBreakdown,
        Map<Integer, BigDecimal> arrearsAgeBreakdown
    ) {}
}
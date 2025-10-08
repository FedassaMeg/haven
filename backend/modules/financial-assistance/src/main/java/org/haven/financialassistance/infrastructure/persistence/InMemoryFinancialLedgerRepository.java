package org.haven.financialassistance.infrastructure.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.haven.clientprofile.domain.ClientId;
import org.haven.financialassistance.domain.ledger.AccountClassification;
import org.haven.financialassistance.domain.ledger.EntryType;
import org.haven.financialassistance.domain.ledger.FinancialLedger;
import org.haven.financialassistance.domain.ledger.FinancialLedgerId;
import org.haven.financialassistance.domain.ledger.FinancialLedgerRepository;
import org.haven.financialassistance.domain.ledger.LedgerEntry;
import org.haven.financialassistance.domain.ledger.LedgerStatus;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.springframework.stereotype.Repository;

/**
 * In-memory implementation of {@link FinancialLedgerRepository} until a persistent adapter is delivered.
 * Supports the lifecycle and alert services while preserving aggregate behaviour.
 */
@Repository
public class InMemoryFinancialLedgerRepository implements FinancialLedgerRepository {

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();
    private static final int ALERT_LOOKBACK_DAYS = 30;

    private final Map<FinancialLedgerId, FinancialLedger> store = new ConcurrentHashMap<>();

    @Override
    public void save(FinancialLedger ledger) {
        Objects.requireNonNull(ledger, "ledger must not be null");
        Objects.requireNonNull(ledger.getId(), "ledger id must not be null");
        store.put(ledger.getId(), ledger);
    }

    @Override
    public Optional<FinancialLedger> findById(FinancialLedgerId id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<FinancialLedger> findByClientId(ClientId clientId) {
        Objects.requireNonNull(clientId, "clientId must not be null");
        return store.values().stream()
            .filter(ledger -> clientId.equals(ledger.getClientId()))
            .collect(Collectors.toList());
    }

    @Override
    public List<FinancialLedger> findByEnrollmentId(ProgramEnrollmentId enrollmentId) {
        Objects.requireNonNull(enrollmentId, "enrollmentId must not be null");
        return store.values().stream()
            .filter(ledger -> enrollmentId.equals(ledger.getEnrollmentId()))
            .collect(Collectors.toList());
    }

    @Override
    public List<FinancialLedger> findByHouseholdId(UUID householdId) {
        Objects.requireNonNull(householdId, "householdId must not be null");
        return store.values().stream()
            .filter(ledger -> householdId.equals(ledger.getHouseholdId()))
            .collect(Collectors.toList());
    }

    @Override
    public Optional<FinancialLedger> findByClientIdAndStatus(ClientId clientId, LedgerStatus status) {
        Objects.requireNonNull(clientId, "clientId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        return store.values().stream()
            .filter(ledger -> clientId.equals(ledger.getClientId()) && ledger.getStatus() == status)
            .findFirst();
    }

    @Override
    public List<FinancialLedger> findActiveByPayeeId(String payeeId) {
        if (payeeId == null || payeeId.isBlank()) {
            return List.of();
        }
        return store.values().stream()
            .filter(ledger -> ledger.getStatus() == LedgerStatus.ACTIVE)
            .filter(ledger -> ledger.getEntries().stream().anyMatch(entry -> payeeId.equals(entry.getPayeeId())))
            .collect(Collectors.toList());
    }

    @Override
    public List<FinancialLedger> findByFundingSourceCode(String fundingSourceCode) {
        if (fundingSourceCode == null || fundingSourceCode.isBlank()) {
            return List.of();
        }
        return store.values().stream()
            .filter(ledger -> ledger.getEntries().stream()
                .anyMatch(entry -> fundingSourceCode.equals(entry.getFundingSourceCode())))
            .collect(Collectors.toList());
    }

    @Override
    public List<FinancialLedger> findUnbalancedLedgers() {
        return store.values().stream()
            .filter(ledger -> !ledger.isBalanced())
            .collect(Collectors.toList());
    }

    @Override
    public List<FinancialLedger> findLedgersWithOverdueArrears() {
        return store.values().stream()
            .filter(this::hasOverdueArrears)
            .collect(Collectors.toList());
    }

    @Override
    public List<FinancialLedger> findLedgersWithUnmatchedDeposits() {
        return store.values().stream()
            .filter(this::hasStaleDeposits)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteById(FinancialLedgerId id) {
        if (id == null) {
            return;
        }
        store.remove(id);
    }

    private boolean hasOverdueArrears(FinancialLedger ledger) {
        LocalDate cutoff = LocalDate.now().minusDays(ALERT_LOOKBACK_DAYS);
        return ledger.getEntries().stream()
            .anyMatch(entry -> isArrearsEntry(entry) && isBefore(entry.getRecordedAt(), cutoff));
    }

    private boolean hasStaleDeposits(FinancialLedger ledger) {
        LocalDate cutoff = LocalDate.now().minusDays(ALERT_LOOKBACK_DAYS);
        return ledger.getEntries().stream()
            .anyMatch(entry -> isDepositEntry(entry) && isBefore(entry.getRecordedAt(), cutoff));
    }

    private boolean isArrearsEntry(LedgerEntry entry) {
        String description = entry.getDescription();
        return description != null && description.toLowerCase().contains("arrears");
    }

    private boolean isDepositEntry(LedgerEntry entry) {
        return entry.getEntryType() == EntryType.CREDIT &&
               entry.getAccountClassification() == AccountClassification.FUNDING_LIABILITY;
    }

    private boolean isBefore(Instant instant, LocalDate cutoff) {
        if (instant == null) {
            return false;
        }
        return instant.atZone(ZONE_ID).toLocalDate().isBefore(cutoff);
    }
}

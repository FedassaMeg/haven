package org.haven.financialassistance.domain.ledger;

import org.haven.clientprofile.domain.ClientId;
import org.haven.programenrollment.domain.ProgramEnrollmentId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Financial Ledger persistence
 */
public interface FinancialLedgerRepository {

    void save(FinancialLedger ledger);

    Optional<FinancialLedger> findById(FinancialLedgerId id);

    List<FinancialLedger> findByClientId(ClientId clientId);

    List<FinancialLedger> findByEnrollmentId(ProgramEnrollmentId enrollmentId);

    List<FinancialLedger> findByHouseholdId(UUID householdId);

    Optional<FinancialLedger> findByClientIdAndStatus(ClientId clientId, LedgerStatus status);

    List<FinancialLedger> findActiveByPayeeId(String payeeId);

    List<FinancialLedger> findByFundingSourceCode(String fundingSourceCode);

    List<FinancialLedger> findUnbalancedLedgers();

    List<FinancialLedger> findLedgersWithOverdueArrears();

    List<FinancialLedger> findLedgersWithUnmatchedDeposits();

    void deleteById(FinancialLedgerId id);
}
package org.haven.api.financialledger;

import org.haven.financialassistance.domain.ledger.LedgerStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LedgerSearchCriteria(
    UUID clientId,
    UUID householdId,
    String fundingSourceCode,
    LedgerStatus status,
    Boolean hasOverdueArrears,
    Boolean hasUnmatchedDeposits,
    LocalDate createdAfter,
    LocalDate createdBefore,
    BigDecimal minBalance,
    BigDecimal maxBalance
) {}
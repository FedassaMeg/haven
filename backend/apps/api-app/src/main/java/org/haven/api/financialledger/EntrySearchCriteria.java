package org.haven.api.financialledger;

import org.haven.financialassistance.domain.ledger.AccountClassification;
import org.haven.financialassistance.domain.ledger.EntryType;

import java.time.LocalDate;
import java.util.UUID;

public record EntrySearchCriteria(
    UUID ledgerId,
    EntryType entryType,
    AccountClassification accountClassification,
    String payeeId,
    String fundingSourceCode,
    String hudCategoryCode,
    LocalDate fromDate,
    LocalDate toDate
) {}
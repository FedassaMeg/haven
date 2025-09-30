package org.haven.financialassistance.domain.ledger;

public enum LedgerStatus {
    ACTIVE("Active - accepting transactions"),
    CLOSED("Closed - no further transactions allowed"),
    SUSPENDED("Suspended - temporarily not accepting transactions"),
    UNDER_REVIEW("Under Review - pending audit or investigation");

    private final String description;

    LedgerStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
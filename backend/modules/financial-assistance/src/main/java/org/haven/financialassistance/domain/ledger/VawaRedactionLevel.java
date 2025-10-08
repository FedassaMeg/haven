package org.haven.financialassistance.domain.ledger;

public enum VawaRedactionLevel {
    NONE("No redaction - full visibility"),
    PARTIAL("Partial redaction - hide sensitive details but show amounts"),
    FULL("Full redaction - hide all transaction details"),
    COMPLETE("Complete redaction - hide existence of transactions");

    private final String description;

    VawaRedactionLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean allowsAmountVisibility() {
        return this == NONE || this == PARTIAL;
    }

    public boolean allowsTransactionVisibility() {
        return this != COMPLETE;
    }

    public boolean allowsDetailVisibility() {
        return this == NONE;
    }
}
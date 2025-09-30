package org.haven.financialassistance.domain.ledger;

public enum PaymentSubtype {
    RENT_CURRENT("Current Rent"),
    RENT_ARREARS("Rent Arrears"),
    UTILITY_CURRENT("Current Utilities"),
    UTILITY_ARREARS("Utility Arrears"),
    SECURITY_DEPOSIT("Security Deposit"),
    MOVING_COSTS("Moving Costs"),
    OTHER("Other");

    private final String displayName;

    PaymentSubtype(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isArrears() {
        return this == RENT_ARREARS || this == UTILITY_ARREARS;
    }

    public boolean isCurrent() {
        return this == RENT_CURRENT || this == UTILITY_CURRENT;
    }
}
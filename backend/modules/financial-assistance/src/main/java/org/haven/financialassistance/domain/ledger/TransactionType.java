package org.haven.financialassistance.domain.ledger;

public enum TransactionType {
    RENT_PAYMENT("Rent Payment", "Current month rent payment"),
    RENT_ARREARS("Rent Arrears", "Past due rent payment"),
    UTILITY_PAYMENT("Utility Payment", "Current utility payment"),
    UTILITY_ARREARS("Utility Arrears", "Past due utility payment"),
    SECURITY_DEPOSIT("Security Deposit", "Security deposit payment"),
    MOVING_COSTS("Moving Costs", "Moving and relocation expenses"),
    FUNDING_DEPOSIT("Funding Deposit", "Grant or funding deposit received"),
    OTHER_PAYMENT("Other Payment", "Other assistance payment");

    private final String displayName;
    private final String description;

    TransactionType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isArrearsType() {
        return this == RENT_ARREARS || this == UTILITY_ARREARS;
    }

    public boolean isCurrentPaymentType() {
        return this == RENT_PAYMENT || this == UTILITY_PAYMENT;
    }

    public boolean isHousingRelated() {
        return this == RENT_PAYMENT || this == RENT_ARREARS ||
               this == UTILITY_PAYMENT || this == UTILITY_ARREARS ||
               this == SECURITY_DEPOSIT || this == MOVING_COSTS;
    }
}
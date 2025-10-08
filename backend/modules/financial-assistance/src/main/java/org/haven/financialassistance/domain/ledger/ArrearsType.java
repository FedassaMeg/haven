package org.haven.financialassistance.domain.ledger;

public enum ArrearsType {
    RENT("Rent arrears - past due rental payments"),
    UTILITY("Utility arrears - past due utility payments");

    private final String description;

    ArrearsType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
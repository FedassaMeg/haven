package org.haven.financialassistance.domain.ledger;

public enum EntryType {
    DEBIT("Debit - money flowing out or expense incurred"),
    CREDIT("Credit - money flowing in or liability/revenue recorded");

    private final String description;

    EntryType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
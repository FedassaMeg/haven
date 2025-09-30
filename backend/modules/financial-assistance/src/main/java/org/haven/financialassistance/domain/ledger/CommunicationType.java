package org.haven.financialassistance.domain.ledger;

public enum CommunicationType {
    EMAIL("Email communication"),
    PHONE("Phone call"),
    LETTER("Written letter"),
    IN_PERSON("In-person meeting"),
    FAX("Fax transmission"),
    TEXT_MESSAGE("Text/SMS message"),
    OTHER("Other communication method");

    private final String description;

    CommunicationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isElectronic() {
        return this == EMAIL || this == TEXT_MESSAGE || this == FAX;
    }

    public boolean isVerbal() {
        return this == PHONE || this == IN_PERSON;
    }

    public boolean isWritten() {
        return this == LETTER || this == EMAIL || this == FAX;
    }
}
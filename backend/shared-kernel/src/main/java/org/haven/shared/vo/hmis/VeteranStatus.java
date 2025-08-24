package org.haven.shared.vo.hmis;

/**
 * HMIS Universal Data Element 3.07 Veteran Status
 * Indicates whether the client has ever spent time in the United States Armed Forces.
 * Only collected for adult clients (18 years or older).
 * Aligned with HMIS 2024 Data Standards.
 */
public enum VeteranStatus {
    NO("No"),
    YES("Yes"),
    CLIENT_DOESNT_KNOW("Client doesn't know"),
    CLIENT_PREFERS_NOT_TO_ANSWER("Client prefers not to answer"),
    DATA_NOT_COLLECTED("Data not collected");

    private final String description;

    VeteranStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isVeteran() {
        return this == YES;
    }

    public boolean isKnownStatus() {
        return this == YES || this == NO;
    }
}
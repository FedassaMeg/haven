package org.haven.shared.vo.hmis;

/**
 * HMIS Universal Data Element 3.08 Disabling Condition
 * Indicates whether the client has at least one disabling condition that is expected
 * to be of long-continued and indefinite duration and substantially impair their ability
 * to live independently.
 * Aligned with HMIS 2024 Data Standards.
 */
public enum DisablingCondition {
    NO("No"),
    YES("Yes"),
    CLIENT_DOESNT_KNOW("Client doesn't know"),
    CLIENT_PREFERS_NOT_TO_ANSWER("Client prefers not to answer"),
    DATA_NOT_COLLECTED("Data not collected");

    private final String description;

    DisablingCondition(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean hasDisablingCondition() {
        return this == YES;
    }

    public boolean isKnownStatus() {
        return this == YES || this == NO;
    }

    /**
     * This field is used to determine program eligibility and priority for certain
     * housing and service programs, particularly those serving chronically homeless individuals.
     */
    public boolean isEligibilityRelevant() {
        return isKnownStatus();
    }
}
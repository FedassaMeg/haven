package org.haven.programenrollment.domain.ce;

/**
 * Status of a Coordinated Entry referral.
 */
public enum CeReferralStatus {
    PENDING("Pending", "Referral is pending response"),
    ACCEPTED("Accepted", "Referral was accepted by the provider"),
    REJECTED("Rejected", "Referral was rejected"),
    EXPIRED("Expired", "Referral expired before response"),
    CANCELLED("Cancelled", "Referral was cancelled"),
    IN_PROCESS("In Process", "Referral is being processed"),
    HOUSED("Housed", "Client was successfully housed through this referral");

    private final String displayName;
    private final String description;

    CeReferralStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
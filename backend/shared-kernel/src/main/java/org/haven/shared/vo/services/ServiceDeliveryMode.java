package org.haven.shared.vo.services;

/**
 * Service delivery mode for tracking how services are provided
 * Important for billing, outcome tracking, and program compliance
 */
public enum ServiceDeliveryMode {
    IN_PERSON("In-Person"),
    PHONE("Phone"),
    VIDEO_CONFERENCE("Video Conference"),
    TEXT_MESSAGE("Text Message"),
    EMAIL("Email"),
    CHAT("Online Chat"),
    GROUP_IN_PERSON("Group In-Person"),
    GROUP_VIRTUAL("Group Virtual"),
    OUTREACH("Outreach/Field"),
    ACCOMPANIMENT("Accompaniment"),
    RESIDENTIAL("Residential"),
    OTHER("Other");

    private final String description;

    ServiceDeliveryMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Determine if this delivery mode allows confidential services
     */
    public boolean allowsConfidentialServices() {
        return switch (this) {
            case IN_PERSON, PHONE, VIDEO_CONFERENCE, CHAT -> true;
            case TEXT_MESSAGE, EMAIL -> false; // Less secure
            case GROUP_IN_PERSON, GROUP_VIRTUAL -> false; // Group settings
            case OUTREACH, ACCOMPANIMENT, RESIDENTIAL -> true;
            case OTHER -> false; // Default to not allowed
        };
    }

    /**
     * Determine if this delivery mode typically has lower billing rates
     */
    public boolean hasReducedBillingRate() {
        return switch (this) {
            case PHONE, TEXT_MESSAGE, EMAIL, CHAT -> true;
            case GROUP_IN_PERSON, GROUP_VIRTUAL -> true; // Shared cost
            default -> false;
        };
    }

    /**
     * Determine if this delivery mode is considered remote
     */
    public boolean isRemoteDelivery() {
        return switch (this) {
            case PHONE, VIDEO_CONFERENCE, TEXT_MESSAGE, EMAIL, CHAT, GROUP_VIRTUAL -> true;
            default -> false;
        };
    }

    /**
     * Get billing multiplier for this delivery mode
     */
    public double getBillingMultiplier() {
        return switch (this) {
            case IN_PERSON, ACCOMPANIMENT, OUTREACH -> 1.0;
            case VIDEO_CONFERENCE -> 0.95;
            case PHONE -> 0.85;
            case GROUP_IN_PERSON -> 0.75;
            case GROUP_VIRTUAL -> 0.70;
            case TEXT_MESSAGE, EMAIL, CHAT -> 0.50;
            case RESIDENTIAL -> 1.25; // Higher rate for residential services
            case OTHER -> 1.0;
        };
    }
}
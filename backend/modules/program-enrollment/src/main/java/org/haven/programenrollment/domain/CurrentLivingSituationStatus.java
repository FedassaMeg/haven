package org.haven.programenrollment.domain;

/**
 * Represents the current status of a client's living situation
 * Used for reporting and eligibility determinations
 */
public enum CurrentLivingSituationStatus {
    UNSHELTERED("Client is currently in an unsheltered situation"),
    EMERGENCY_SHELTER("Client is currently in emergency shelter"),
    TRANSITIONAL_HOUSING("Client is currently in transitional housing"),
    PERMANENT_HOUSING("Client is currently in permanent housing"),
    INSTITUTIONAL("Client is currently in an institutional setting"),
    TEMPORARY_HOUSING("Client is currently in temporary housing"),
    OTHER("Other living situation"),
    UNKNOWN("Current living situation unknown");
    
    private final String description;
    
    CurrentLivingSituationStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
package org.haven.reporting.domain;

/**
 * Export job state machine
 * QUEUED → MATERIALIZING → VALIDATING → COMPLETE/FAILED
 */
public enum ExportJobState {
    QUEUED("Queued for processing"),
    MATERIALIZING("Materializing views and generating CSV data"),
    VALIDATING("Validating HUD compliance and data quality"),
    COMPLETE("Export completed successfully"),
    FAILED("Export failed with errors");

    private final String description;

    ExportJobState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminal() {
        return this == COMPLETE || this == FAILED;
    }

    public boolean canTransitionTo(ExportJobState newState) {
        switch (this) {
            case QUEUED:
                return newState == MATERIALIZING || newState == FAILED;
            case MATERIALIZING:
                return newState == VALIDATING || newState == FAILED;
            case VALIDATING:
                return newState == COMPLETE || newState == FAILED;
            case COMPLETE:
            case FAILED:
                return false; // Terminal states cannot transition
            default:
                return false;
        }
    }
}

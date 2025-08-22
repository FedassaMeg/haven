package org.haven.casemgmt.domain.mandatedreport;

/**
 * Status of a mandated report through its lifecycle
 */
public enum ReportStatus {
    /**
     * Report is being drafted but not yet filed
     */
    DRAFT("Draft", false),
    
    /**
     * Report has been filed with appropriate agency
     */
    FILED("Filed", false),
    
    /**
     * Agency has acknowledged receipt of the report
     */
    ACKNOWLEDGED("Acknowledged", false),
    
    /**
     * Agency is investigating the report
     */
    UNDER_INVESTIGATION("Under Investigation", false),
    
    /**
     * Investigation has been completed
     */
    INVESTIGATION_COMPLETE("Investigation Complete", false),
    
    /**
     * Report was substantiated and action taken
     */
    SUBSTANTIATED("Substantiated", true),
    
    /**
     * Report was not substantiated
     */
    UNSUBSTANTIATED("Unsubstantiated", true),
    
    /**
     * Investigation was inconclusive
     */
    INCONCLUSIVE("Inconclusive", true),
    
    /**
     * Report was closed without investigation
     */
    CLOSED("Closed", true),
    
    /**
     * Report filing was overdue
     */
    OVERDUE("Overdue", false);
    
    private final String displayName;
    private final boolean isFinal;
    
    ReportStatus(String displayName, boolean isFinal) {
        this.displayName = displayName;
        this.isFinal = isFinal;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isFinal() {
        return isFinal;
    }
    
    public boolean isActive() {
        return !isFinal && this != OVERDUE;
    }
    
    public boolean requiresAction() {
        return this == DRAFT || this == OVERDUE;
    }
    
    /**
     * Get next valid status transitions
     */
    public ReportStatus[] getValidTransitions() {
        return switch (this) {
            case DRAFT -> new ReportStatus[]{FILED, OVERDUE};
            case FILED -> new ReportStatus[]{ACKNOWLEDGED, UNDER_INVESTIGATION, CLOSED};
            case ACKNOWLEDGED -> new ReportStatus[]{UNDER_INVESTIGATION, CLOSED};
            case UNDER_INVESTIGATION -> new ReportStatus[]{INVESTIGATION_COMPLETE, SUBSTANTIATED, UNSUBSTANTIATED, INCONCLUSIVE};
            case INVESTIGATION_COMPLETE -> new ReportStatus[]{SUBSTANTIATED, UNSUBSTANTIATED, INCONCLUSIVE, CLOSED};
            case OVERDUE -> new ReportStatus[]{FILED}; // Can still file overdue reports
            default -> new ReportStatus[]{}; // Final states have no transitions
        };
    }
    
    public boolean canTransitionTo(ReportStatus newStatus) {
        ReportStatus[] validTransitions = getValidTransitions();
        for (ReportStatus validStatus : validTransitions) {
            if (validStatus == newStatus) {
                return true;
            }
        }
        return false;
    }
}
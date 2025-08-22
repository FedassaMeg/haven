package org.haven.casemgmt.domain.mandatedreport;

/**
 * Types of mandated reports that must be filed
 */
public enum ReportType {
    /**
     * Child abuse report to Child Protective Services
     */
    CHILD_ABUSE("Child Abuse Report", "CPS", true, 24),
    
    /**
     * Elder abuse report to Adult Protective Services  
     */
    ELDER_ABUSE("Elder Abuse Report", "APS", true, 24),
    
    /**
     * Domestic violence incident report to law enforcement
     */
    DOMESTIC_VIOLENCE("Domestic Violence Report", "Police", false, 72),
    
    /**
     * Sexual assault report to law enforcement
     */
    SEXUAL_ASSAULT("Sexual Assault Report", "Police", true, 24),
    
    /**
     * Human trafficking report to appropriate authorities
     */
    HUMAN_TRAFFICKING("Human Trafficking Report", "FBI/Police", true, 24),
    
    /**
     * Mental health hold or evaluation report
     */
    MENTAL_HEALTH_HOLD("Mental Health Hold", "County Mental Health", true, 2),
    
    /**
     * Communicable disease report to health department
     */
    COMMUNICABLE_DISEASE("Communicable Disease Report", "Health Department", true, 24),
    
    /**
     * Court-ordered report or testimony
     */
    COURT_ORDERED("Court Ordered Report", "Court System", true, 0), // Immediate
    
    /**
     * Welfare fraud report
     */
    WELFARE_FRAUD("Welfare Fraud Report", "Social Services", false, 120),
    
    /**
     * Death report for suspicious circumstances
     */
    SUSPICIOUS_DEATH("Suspicious Death Report", "Coroner/Police", true, 2);
    
    private final String displayName;
    private final String reportingAgency;
    private final boolean isEmergency;
    private final int hoursToFile; // Maximum hours allowed to file
    
    ReportType(String displayName, String reportingAgency, boolean isEmergency, int hoursToFile) {
        this.displayName = displayName;
        this.reportingAgency = reportingAgency;
        this.isEmergency = isEmergency;
        this.hoursToFile = hoursToFile;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getReportingAgency() {
        return reportingAgency;
    }
    
    public boolean isEmergency() {
        return isEmergency;
    }
    
    public int getHoursToFile() {
        return hoursToFile;
    }
    
    /**
     * Returns true if this report type requires immediate filing
     */
    public boolean requiresImmediateAction() {
        return hoursToFile <= 2;
    }
    
    /**
     * Returns true if this report type involves child safety
     */
    public boolean involvesChildSafety() {
        return this == CHILD_ABUSE || this == SEXUAL_ASSAULT || this == HUMAN_TRAFFICKING;
    }
    
    /**
     * Returns true if this report type requires follow-up investigation
     */
    public boolean requiresFollowUp() {
        return isEmergency || this == COURT_ORDERED;
    }
}
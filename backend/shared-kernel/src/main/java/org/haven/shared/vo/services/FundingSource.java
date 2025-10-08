package org.haven.shared.vo.services;

import java.util.Objects;

/**
 * Funding source value object for tracking service funding and billing
 * Supports multiple funding streams common in homeless and DV services
 */
public class FundingSource {
    private final String funderId;
    private final String funderName;
    private final String grantNumber;
    private final FunderType funderType;
    private final String programName;
    private final boolean requiresOutcomeTracking;
    private final boolean allowsConfidentialServices;

    public FundingSource(String funderId, String funderName, String grantNumber, 
                        FunderType funderType, String programName,
                        boolean requiresOutcomeTracking, boolean allowsConfidentialServices) {
        this.funderId = Objects.requireNonNull(funderId, "Funder ID cannot be null");
        this.funderName = Objects.requireNonNull(funderName, "Funder name cannot be null");
        this.grantNumber = grantNumber;
        this.funderType = Objects.requireNonNull(funderType, "Funder type cannot be null");
        this.programName = programName;
        this.requiresOutcomeTracking = requiresOutcomeTracking;
        this.allowsConfidentialServices = allowsConfidentialServices;
    }

    // Common funding sources factory methods
    public static FundingSource hudCoc(String grantNumber, String programName) {
        return new FundingSource(
            "HUD-COC", 
            "HUD Continuum of Care", 
            grantNumber, 
            FunderType.FEDERAL, 
            programName,
            true,  // HUD requires outcome tracking
            false  // HUD programs typically don't allow fully confidential services
        );
    }

    public static FundingSource vawa(String grantNumber, String programName) {
        return new FundingSource(
            "DOJ-VAWA", 
            "DOJ Violence Against Women Act", 
            grantNumber, 
            FunderType.FEDERAL, 
            programName,
            true,  // VAWA requires outcome tracking
            true   // VAWA allows confidential services
        );
    }

    public static FundingSource calOes(String grantNumber, String programName) {
        return new FundingSource(
            "CAL-OES", 
            "California Office of Emergency Services", 
            grantNumber, 
            FunderType.STATE, 
            programName,
            true,  // Cal OES requires detailed reporting
            true   // Cal OES DV programs allow confidential services
        );
    }

    public static FundingSource fema(String grantNumber, String programName) {
        return new FundingSource(
            "FEMA-ESG", 
            "FEMA Emergency Solutions Grant", 
            grantNumber, 
            FunderType.FEDERAL, 
            programName,
            true,  // FEMA requires outcome tracking
            false  // FEMA programs typically don't allow confidential services
        );
    }

    public static FundingSource hopwa(String grantNumber, String programName) {
        return new FundingSource(
            "HUD-HOPWA", 
            "HUD Housing Opportunities for Persons with AIDS", 
            grantNumber, 
            FunderType.FEDERAL, 
            programName,
            true,  // HOPWA requires outcome tracking
            true   // HOPWA allows confidential services due to health privacy
        );
    }

    public static FundingSource privateFoundation(String funderId, String funderName, 
                                                 String grantNumber, String programName) {
        return new FundingSource(
            funderId, 
            funderName, 
            grantNumber, 
            FunderType.PRIVATE, 
            programName,
            false, // Private foundations may not require detailed outcome tracking
            true   // Private foundations typically allow confidential services
        );
    }

    public static FundingSource noFunding() {
        return new FundingSource(
            "NONE", 
            "No Funding Source", 
            null, 
            FunderType.UNFUNDED, 
            "Volunteer Services",
            false, // No outcome tracking required for unfunded services
            true   // Volunteer services can be confidential
        );
    }

    public enum FunderType {
        FEDERAL("Federal Government"),
        STATE("State Government"),
        LOCAL("Local Government"),
        PRIVATE("Private Foundation"),
        CORPORATE("Corporate Grant"),
        UNFUNDED("No Funding");

        private final String description;

        FunderType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public boolean isGovernmentFunding() {
            return this == FEDERAL || this == STATE || this == LOCAL;
        }
    }

    // Getters
    public String getFunderId() { return funderId; }
    public String getFunderName() { return funderName; }
    public String getGrantNumber() { return grantNumber; }
    public FunderType getFunderType() { return funderType; }
    public String getProgramName() { return programName; }
    public boolean requiresOutcomeTracking() { return requiresOutcomeTracking; }
    public boolean allowsConfidentialServices() { return allowsConfidentialServices; }

    /**
     * Check if this funding source is compatible with a service type
     */
    public boolean isCompatibleWith(ServiceType serviceType) {
        // Government funding typically doesn't allow certain confidential services
        if (funderType.isGovernmentFunding() && !allowsConfidentialServices) {
            return !serviceType.requiresConfidentialHandling();
        }
        return true;
    }

    /**
     * Determine billing rate category for this funding source
     */
    public BillingRateCategory getBillingRateCategory() {
        return switch (funderType) {
            case FEDERAL -> BillingRateCategory.FEDERAL_RATE;
            case STATE -> BillingRateCategory.STATE_RATE;
            case LOCAL -> BillingRateCategory.LOCAL_RATE;
            case PRIVATE, CORPORATE -> BillingRateCategory.PRIVATE_RATE;
            case UNFUNDED -> BillingRateCategory.NO_BILLING;
        };
    }

    public enum BillingRateCategory {
        FEDERAL_RATE,
        STATE_RATE,
        LOCAL_RATE,
        PRIVATE_RATE,
        NO_BILLING
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FundingSource that = (FundingSource) o;
        return Objects.equals(funderId, that.funderId) &&
               Objects.equals(grantNumber, that.grantNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(funderId, grantNumber);
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", funderName, grantNumber != null ? grantNumber : "No Grant");
    }
}
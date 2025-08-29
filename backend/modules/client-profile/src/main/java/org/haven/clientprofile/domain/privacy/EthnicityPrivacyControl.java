package org.haven.clientprofile.domain.privacy;

import org.haven.shared.vo.hmis.HmisEthnicity;
import org.haven.shared.vo.hmis.HmisEthnicity.EthnicityPrecision;
import java.util.*;

/**
 * Privacy control for Ethnicity data implementing precision controls.
 * Supports varying levels of detail disclosure based on access context.
 */
public class EthnicityPrivacyControl {
    
    private final HmisEthnicity actualEthnicity;
    private final EthnicityPrecision precisionLevel;
    private final UUID clientId;
    
    public EthnicityPrivacyControl(HmisEthnicity actualEthnicity, 
                                  EthnicityPrecision precisionLevel,
                                  UUID clientId) {
        this.actualEthnicity = actualEthnicity != null ? actualEthnicity : HmisEthnicity.DATA_NOT_COLLECTED;
        this.precisionLevel = precisionLevel != null ? precisionLevel : EthnicityPrecision.FULL;
        this.clientId = clientId;
    }
    
    /**
     * Returns the ethnicity data based on the precision level
     */
    public HmisEthnicity getRedactedEthnicity() {
        return switch (precisionLevel) {
            case FULL -> actualEthnicity;
            case CATEGORY_ONLY -> getCategoryOnlyEthnicity();
            case REDACTED -> HmisEthnicity.CLIENT_PREFERS_NOT_TO_ANSWER;
            case HIDDEN -> HmisEthnicity.DATA_NOT_COLLECTED;
        };
    }
    
    /**
     * Returns ethnicity as category only (known/unknown)
     */
    private HmisEthnicity getCategoryOnlyEthnicity() {
        if (actualEthnicity.isKnownEthnicity()) {
            // Don't reveal whether Hispanic/Latino, just that we have the data
            return HmisEthnicity.CLIENT_PREFERS_NOT_TO_ANSWER;
        }
        return actualEthnicity; // Return the "unknown" type as-is
    }
    
    /**
     * Gets a human-readable description based on precision level
     */
    public String getRedactedDescription() {
        return actualEthnicity.getRedactedValue(precisionLevel);
    }
    
    /**
     * Returns a statistical representation for reporting
     */
    public String getStatisticalCategory() {
        return switch (precisionLevel) {
            case FULL, CATEGORY_ONLY -> {
                if (actualEthnicity.isKnownEthnicity()) {
                    yield actualEthnicity == HmisEthnicity.HISPANIC_LATINO ? 
                        "Hispanic/Latino" : "Non-Hispanic/Latino";
                } else {
                    yield "Unknown/Not Reported";
                }
            }
            case REDACTED -> "Protected";
            case HIDDEN -> "N/A";
        };
    }
    
    /**
     * Creates a report-safe version for aggregate reporting
     */
    public Map<String, Object> getReportingProjection() {
        Map<String, Object> projection = new HashMap<>();
        
        projection.put("hasEthnicityData", actualEthnicity.isKnownEthnicity());
        projection.put("precisionLevel", precisionLevel.name());
        
        if (precisionLevel == EthnicityPrecision.FULL) {
            projection.put("ethnicity", actualEthnicity.name());
            projection.put("description", actualEthnicity.getDescription());
        } else if (precisionLevel == EthnicityPrecision.CATEGORY_ONLY) {
            projection.put("category", getStatisticalCategory());
        }
        
        return projection;
    }
    
    /**
     * Checks if the current precision allows full disclosure
     */
    public boolean isFullDisclosure() {
        return precisionLevel == EthnicityPrecision.FULL;
    }
    
    /**
     * Gets aliased ethnicity for maximum privacy
     * Uses client ID to ensure consistency
     */
    public HmisEthnicity getAliasedEthnicity() {
        if (!actualEthnicity.isKnownEthnicity()) {
            return actualEthnicity; // Don't alias unknown values
        }
        
        // Use client ID to generate consistent alias
        Random random = new Random(clientId.hashCode());
        boolean useHispanic = random.nextBoolean();
        
        // Return opposite of actual value for aliasing
        if (actualEthnicity == HmisEthnicity.HISPANIC_LATINO) {
            return useHispanic ? actualEthnicity : HmisEthnicity.NON_HISPANIC_LATINO;
        } else {
            return useHispanic ? HmisEthnicity.HISPANIC_LATINO : actualEthnicity;
        }
    }
    
    /**
     * Builder for creating EthnicityPrivacyControl instances
     */
    public static class Builder {
        private HmisEthnicity ethnicity = HmisEthnicity.DATA_NOT_COLLECTED;
        private EthnicityPrecision precision = EthnicityPrecision.FULL;
        private UUID clientId;
        
        public Builder withEthnicity(HmisEthnicity ethnicity) {
            this.ethnicity = ethnicity;
            return this;
        }
        
        public Builder withPrecision(EthnicityPrecision precision) {
            this.precision = precision;
            return this;
        }
        
        public Builder withClientId(UUID clientId) {
            this.clientId = clientId;
            return this;
        }
        
        public EthnicityPrivacyControl build() {
            return new EthnicityPrivacyControl(ethnicity, precision, clientId);
        }
    }
}
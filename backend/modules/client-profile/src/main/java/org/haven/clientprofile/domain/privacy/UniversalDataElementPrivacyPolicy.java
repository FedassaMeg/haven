package org.haven.clientprofile.domain.privacy;

import org.haven.clientprofile.domain.pii.PIIAccessContext;
import org.haven.clientprofile.domain.pii.PIIAccessLevel;
import org.haven.clientprofile.domain.pii.PIICategory;
import org.haven.shared.vo.hmis.HmisEthnicity.EthnicityPrecision;
import org.haven.clientprofile.domain.privacy.RacePrivacyControl.RaceRedactionStrategy;
import java.util.UUID;

/**
 * Central policy for Universal Data Element privacy controls.
 * Determines appropriate redaction strategies based on access context and purpose.
 */
public class UniversalDataElementPrivacyPolicy {
    
    /**
     * Determines the appropriate race redaction strategy based on access context
     */
    public RaceRedactionStrategy determineRaceStrategy(PIIAccessContext context, 
                                                       DataAccessPurpose purpose,
                                                       UUID clientId) {
        // Check if user has explicit race data access
        if (!context.hasAccess(PIICategory.QUASI_IDENTIFIER, PIIAccessLevel.RESTRICTED)) {
            return RaceRedactionStrategy.HIDDEN;
        }
        
        // Determine strategy based on purpose and access level
        return switch (purpose) {
            case DIRECT_SERVICE -> {
                if (context.hasAccess(PIICategory.QUASI_IDENTIFIER, PIIAccessLevel.CONFIDENTIAL)) {
                    yield RaceRedactionStrategy.FULL_DISCLOSURE;
                } else {
                    yield RaceRedactionStrategy.GENERALIZED;
                }
            }
            case CASE_MANAGEMENT -> {
                if (context.isAssignedCaseWorker(clientId)) {
                    yield RaceRedactionStrategy.FULL_DISCLOSURE;
                } else {
                    yield RaceRedactionStrategy.MASKED;
                }
            }
            case REPORTING -> RaceRedactionStrategy.GENERALIZED;
            case RESEARCH -> RaceRedactionStrategy.ALIASED;
            case COURT_ORDERED -> {
                // Court orders may require full disclosure
                if (context.hasLegalAuthorization()) {
                    yield RaceRedactionStrategy.FULL_DISCLOSURE;
                } else {
                    yield RaceRedactionStrategy.CATEGORY_ONLY;
                }
            }
            case AUDIT -> RaceRedactionStrategy.CATEGORY_ONLY;
            case EMERGENCY -> RaceRedactionStrategy.MASKED;
            case VSP_SHARING -> {
                // Victim Service Providers get maximum privacy
                yield RaceRedactionStrategy.ALIASED;
            }
            case HMIS_EXPORT -> {
                // HMIS exports require some level of data
                if (context.hasAccess(PIICategory.QUASI_IDENTIFIER, PIIAccessLevel.INTERNAL)) {
                    yield RaceRedactionStrategy.GENERALIZED;
                } else {
                    yield RaceRedactionStrategy.CATEGORY_ONLY;
                }
            }
        };
    }
    
    /**
     * Determines the appropriate ethnicity precision level based on access context
     */
    public EthnicityPrecision determineEthnicityPrecision(PIIAccessContext context,
                                                         DataAccessPurpose purpose,
                                                         UUID clientId) {
        // Check if user has ethnicity data access
        if (!context.hasAccess(PIICategory.QUASI_IDENTIFIER, PIIAccessLevel.RESTRICTED)) {
            return EthnicityPrecision.HIDDEN;
        }
        
        // Determine precision based on purpose and access level
        return switch (purpose) {
            case DIRECT_SERVICE -> {
                if (context.hasAccess(PIICategory.QUASI_IDENTIFIER, PIIAccessLevel.CONFIDENTIAL)) {
                    yield EthnicityPrecision.FULL;
                } else {
                    yield EthnicityPrecision.CATEGORY_ONLY;
                }
            }
            case CASE_MANAGEMENT -> {
                if (context.isAssignedCaseWorker(clientId)) {
                    yield EthnicityPrecision.FULL;
                } else {
                    yield EthnicityPrecision.CATEGORY_ONLY;
                }
            }
            case REPORTING -> EthnicityPrecision.CATEGORY_ONLY;
            case RESEARCH -> EthnicityPrecision.REDACTED;
            case COURT_ORDERED -> {
                if (context.hasLegalAuthorization()) {
                    yield EthnicityPrecision.FULL;
                } else {
                    yield EthnicityPrecision.CATEGORY_ONLY;
                }
            }
            case AUDIT -> EthnicityPrecision.REDACTED;
            case EMERGENCY -> EthnicityPrecision.CATEGORY_ONLY;
            case VSP_SHARING -> EthnicityPrecision.REDACTED;
            case HMIS_EXPORT -> {
                if (context.hasAccess(PIICategory.QUASI_IDENTIFIER, PIIAccessLevel.INTERNAL)) {
                    yield EthnicityPrecision.CATEGORY_ONLY;
                } else {
                    yield EthnicityPrecision.REDACTED;
                }
            }
        };
    }
    
    /**
     * Checks if race/ethnicity data should be included in a given context
     */
    public boolean shouldIncludeDemographics(PIIAccessContext context, DataAccessPurpose purpose) {
        // Never include for anonymous access
        if (context.isAnonymous()) {
            return false;
        }
        
        // Check minimum access requirements
        boolean hasMinimumAccess = context.hasAccess(PIICategory.QUASI_IDENTIFIER, PIIAccessLevel.INTERNAL);
        
        // Some purposes never include demographics
        if (purpose == DataAccessPurpose.AUDIT) {
            return false;
        }
        
        return hasMinimumAccess;
    }
    
    /**
     * Determines if aliasing should be used instead of redaction
     */
    public boolean shouldUseAliasing(PIIAccessContext context, DataAccessPurpose purpose) {
        // Aliasing is preferred for research and VSP sharing
        return purpose == DataAccessPurpose.RESEARCH || 
               purpose == DataAccessPurpose.VSP_SHARING;
    }
    
    /**
     * Gets a description of the privacy controls being applied
     */
    public String getPrivacyNotice(RaceRedactionStrategy raceStrategy, 
                                  EthnicityPrecision ethnicityPrecision) {
        StringBuilder notice = new StringBuilder();
        notice.append("Privacy Controls Applied: ");
        
        // Race controls
        notice.append("Race data is ");
        switch (raceStrategy) {
            case FULL_DISCLOSURE -> notice.append("fully disclosed");
            case GENERALIZED -> notice.append("generalized to categories");
            case CATEGORY_ONLY -> notice.append("reduced to known/unknown status");
            case MASKED -> notice.append("partially masked");
            case ALIASED -> notice.append("replaced with consistent aliases");
            case HIDDEN -> notice.append("completely hidden");
        }
        
        notice.append(". Ethnicity data is ");
        switch (ethnicityPrecision) {
            case FULL -> notice.append("fully disclosed");
            case CATEGORY_ONLY -> notice.append("shown as category only");
            case REDACTED -> notice.append("redacted");
            case HIDDEN -> notice.append("completely hidden");
        }
        notice.append(".");
        
        return notice.toString();
    }
    
    /**
     * Purpose for accessing demographic data
     */
    public enum DataAccessPurpose {
        DIRECT_SERVICE,     // Providing direct services to client
        CASE_MANAGEMENT,    // Case management activities
        REPORTING,          // Aggregate reporting
        RESEARCH,           // Research and analysis
        COURT_ORDERED,      // Court-ordered disclosure
        AUDIT,              // Compliance audit
        EMERGENCY,          // Emergency access
        VSP_SHARING,        // Victim Service Provider data sharing
        HMIS_EXPORT         // HMIS data export
    }
}
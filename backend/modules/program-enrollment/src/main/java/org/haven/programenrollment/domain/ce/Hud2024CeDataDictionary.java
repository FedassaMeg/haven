package org.haven.programenrollment.domain.ce;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * HUD 2024 Coordinated Entry Data Dictionary Mappings
 * Implements the 2024 HUD CE Assessment, Event, and Referral Data Standards
 * Reference: HUD Exchange 2024 HMIS Data Standards Manual
 */
public class Hud2024CeDataDictionary {

    /**
     * CE Assessment Data Elements (4.19)
     */
    public static class CeAssessmentDataElements {
        // 4.19.1 - Assessment Date
        public static final String ASSESSMENT_DATE = "AssessmentDate";
        public static final Class<?> ASSESSMENT_DATE_TYPE = LocalDate.class;

        // 4.19.2 - Assessment Location
        public static final String ASSESSMENT_LOCATION = "AssessmentLocation";
        public static final Map<String, String> ASSESSMENT_LOCATION_VALUES = Map.of(
            "1", "Provider-administered on-site",
            "2", "Provider-administered off-site location",
            "3", "Virtual (phone, video, etc.)"
        );

        // 4.19.3 - Assessment Type
        public static final String ASSESSMENT_TYPE = "AssessmentType";
        public static final Map<String, String> ASSESSMENT_TYPE_VALUES = Map.of(
            "1", "Phone",
            "2", "Virtual",
            "3", "In Person"
        );

        // 4.19.4 - Assessment Level
        public static final String ASSESSMENT_LEVEL = "AssessmentLevel";
        public static final Map<String, String> ASSESSMENT_LEVEL_VALUES = Map.of(
            "1", "Crisis Needs Assessment",
            "2", "Housing Needs Assessment"
        );

        // 4.19.5 - Prioritization Status
        public static final String PRIORITIZATION_STATUS = "PrioritizationStatus";
        public static final Map<String, String> PRIORITIZATION_STATUS_VALUES = Map.of(
            "1", "Placed on prioritization list",
            "2", "Not placed on prioritization list",
            "8", "Client doesn't know",
            "9", "Client prefers not to answer",
            "99", "Data not collected"
        );
    }

    /**
     * CE Event Data Elements (4.20)
     */
    public static class CeEventDataElements {
        // 4.20.1 - Event Date
        public static final String EVENT_DATE = "EventDate";
        public static final Class<?> EVENT_DATE_TYPE = LocalDateTime.class;

        // 4.20.2 - Event Type
        public static final String EVENT = "Event";
        public static final Map<String, String> EVENT_VALUES = Map.ofEntries(
            Map.entry("1", "Referral to Prevention Assistance project"),
            Map.entry("2", "Problem Solving/Diversion/Rapid Resolution intervention or service"),
            Map.entry("3", "Referral to scheduled Coordinated Entry Crisis Needs Assessment"),
            Map.entry("4", "Referral to scheduled Coordinated Entry Housing Needs Assessment"),
            Map.entry("6", "Referral to Emergency Shelter bed opening"),
            Map.entry("7", "Referral to Transitional Housing bed/unit opening"),
            Map.entry("10", "Referral to permanent housing project/unit/opening"),
            Map.entry("11", "Referral to Joint TH-RRH project/unit/opening"),
            Map.entry("12", "Referral to Homelessness Prevention project"),
            Map.entry("14", "Referral to RRH project"),
            Map.entry("15", "Referral to PSH project"),
            Map.entry("16", "Referral to Other PH project"),
            Map.entry("18", "Referral to Emergency Housing Voucher (EHV)"),
            Map.entry("19", "Referral to Public Housing unit opening"),
            Map.entry("20", "Referral to Housing Choice Voucher opportunity"),
            Map.entry("21", "Referral to Foster Youth to Independence (FYI) Voucher"),
            Map.entry("22", "Referral to Family Unification Program (FUP) Voucher")
        );

        // 4.20.3 - Referral Result
        public static final String REFERRAL_RESULT = "ReferralResult";
        public static final Map<String, String> REFERRAL_RESULT_VALUES = Map.of(
            "1", "Successful referral: client accepted",
            "2", "Unsuccessful referral: client rejected",
            "3", "Unsuccessful referral: provider rejected",
            "8", "Client doesn't know",
            "9", "Client prefers not to answer"
        );

        // 4.20.A - Referral Case Manager Information
        public static final String REFERRAL_CASE_MANAGER_INFO = "ReferralCaseManagerInfo";
        public static final String REFERRAL_CASE_MANAGER_CONTACT = "ReferralCaseManagerContact";
    }

    /**
     * CE Referral Data Elements - Extended
     */
    public static class CeReferralDataElements {
        // Housing Project/Program Information
        public static final String REFERRED_PROJECT_ID = "ReferredProjectID";
        public static final String REFERRED_PROJECT_NAME = "ReferredProjectName";
        public static final String REFERRED_ORGANIZATION = "ReferredOrganization";

        // Referral Dates
        public static final String REFERRAL_DATE = "ReferralDate";
        public static final String REFERRAL_EXPIRATION_DATE = "ReferralExpirationDate";
        public static final String REFERRAL_RESPONSE_DATE = "ReferralResponseDate";

        // Referral Outcome Details
        public static final String REFERRAL_ACCEPTED_DATE = "ReferralAcceptedDate";
        public static final String REFERRAL_REJECTED_DATE = "ReferralRejectedDate";
        public static final String REFERRAL_REJECTION_REASON = "ReferralRejectionReason";
        public static final Map<String, String> REJECTION_REASON_VALUES = Map.of(
            "1", "Client ineligible for project",
            "2", "Client does not meet project criteria",
            "3", "No vacancy available",
            "4", "Client declined referral",
            "5", "Client did not show for appointment",
            "6", "Client unable to be contacted",
            "7", "Other"
        );
    }

    /**
     * CE-Required Client Data Elements
     * These must be captured/verified during CE process
     */
    public static class CeClientDataElements {
        // Unique Identifier (hashed for privacy)
        public static final String CLIENT_UNIQUE_ID = "ClientUniqueID";
        public static final String CLIENT_HASH_ALGORITHM = "ClientHashAlgorithm";
        public static final String CLIENT_HASH_SALT = "ClientHashSalt";

        // Household Information
        public static final String HOUSEHOLD_ID = "HouseholdID";
        public static final String HOUSEHOLD_TYPE = "HouseholdType";

        // Vulnerability Score Components
        public static final String VI_SPDAT_SCORE = "VISPDATScore";
        public static final String VULNERABILITY_INDEX = "VulnerabilityIndex";
        public static final String CHRONICITY_SCORE = "ChronicityScore";
        public static final String PRIORITIZATION_SCORE = "PrioritizationScore";
    }

    /**
     * Consent and Privacy Elements for CE
     */
    public static class CeConsentElements {
        // Consent Types
        public static final String CONSENT_TYPE = "ConsentType";
        public static final Map<String, String> CONSENT_TYPE_VALUES = Map.of(
            "CE_ASSESSMENT", "Consent for CE Assessment",
            "CE_REFERRAL", "Consent for CE Referral",
            "CE_DATA_SHARE", "Consent for CE Data Sharing",
            "HMIS_PARTICIPATION", "Consent for HMIS Participation",
            "VAWA_CONFIDENTIAL", "VAWA Confidentiality Request"
        );

        // Share Scopes
        public static final Set<String> DEFAULT_SHARE_SCOPES = Set.of(
            "COC_COORDINATED_ENTRY",
            "HMIS_PARTICIPATING_AGENCIES"
        );

        public static final Set<String> RESTRICTED_SHARE_SCOPES = Set.of(
            "VAWA_COMPLIANT_PROVIDERS",
            "DOMESTIC_VIOLENCE_PROVIDERS"
        );
    }

    /**
     * Data Quality and Validation Rules
     */
    public static class CeDataQualityRules {
        // Required fields for valid CE Assessment
        public static final Set<String> REQUIRED_ASSESSMENT_FIELDS = Set.of(
            CeAssessmentDataElements.ASSESSMENT_DATE,
            CeAssessmentDataElements.ASSESSMENT_TYPE,
            CeAssessmentDataElements.ASSESSMENT_LEVEL,
            CeClientDataElements.CLIENT_UNIQUE_ID
        );

        // Required fields for valid CE Event
        public static final Set<String> REQUIRED_EVENT_FIELDS = Set.of(
            CeEventDataElements.EVENT_DATE,
            CeEventDataElements.EVENT,
            CeClientDataElements.CLIENT_UNIQUE_ID
        );

        // Required fields for valid CE Referral
        public static final Set<String> REQUIRED_REFERRAL_FIELDS = Set.of(
            CeReferralDataElements.REFERRAL_DATE,
            CeReferralDataElements.REFERRED_PROJECT_ID,
            CeEventDataElements.REFERRAL_RESULT,
            CeClientDataElements.CLIENT_UNIQUE_ID
        );
    }

    /**
     * Encryption and Security Standards for CE Data
     */
    public static class CeSecurityStandards {
        // Supported Hash Algorithms
        public static final String SHA256_SALT = "SHA256_SALT";
        public static final String BCRYPT = "BCRYPT";
        public static final String ARGON2 = "ARGON2";

        // Supported Encryption Schemes
        public static final String AES_256_GCM = "AES-256-GCM";
        public static final String AES_256_CBC = "AES-256-CBC";

        // Key Management
        public static final String KEY_ROTATION_DAYS = "90";
        public static final String MIN_KEY_LENGTH_BITS = "256";

        // Audit Requirements
        public static final boolean AUDIT_ALL_ACCESS = true;
        public static final boolean AUDIT_ALL_EXPORTS = true;
        public static final boolean AUDIT_CONSENT_CHANGES = true;
    }

    /**
     * Export Format Specifications
     */
    public static class CeExportFormats {
        // HUD CSV Format (2024 Specification)
        public static final String HUD_CSV_VERSION = "2024.1.0";
        public static final String CSV_DELIMITER = ",";
        public static final String CSV_ENCODING = "UTF-8";

        // HUD XML Format (2024 Specification)
        public static final String HUD_XML_VERSION = "2024.1";
        public static final String XML_NAMESPACE = "https://www.hudexchange.info/Resources/2024";
        public static final String XML_SCHEMA = "HMIS_XML_2024.xsd";

        // Export File Naming Convention
        public static final String EXPORT_FILE_PATTERN = "CE_Export_{CoC}_{Date}_{Time}_{Type}";
    }
}

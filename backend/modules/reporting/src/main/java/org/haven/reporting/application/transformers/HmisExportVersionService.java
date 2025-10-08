package org.haven.reporting.application.transformers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Service to manage HMIS export version gating and feature flags
 *
 * Supports version-aware HMIS CSV exports per HUD Data Standards:
 * - FY2022 (October 1, 2022 - September 30, 2024)
 * - FY2024 (October 1, 2024 - present)
 *
 * Configuration-driven to allow legacy exports for historical compliance
 */
@Service
public class HmisExportVersionService {

    private final HmisExportVersion configuredVersion;
    private final boolean strictMode;

    public HmisExportVersionService(
            @Value("${hmis.export.version:FY2024}") String version,
            @Value("${hmis.export.compatibility-mode:strict}") String compatibilityMode) {

        this.configuredVersion = HmisExportVersion.valueOf(version);
        this.strictMode = "strict".equalsIgnoreCase(compatibilityMode);
    }

    /**
     * Get currently configured export version
     */
    public HmisExportVersion getVersion() {
        return configuredVersion;
    }

    /**
     * Check if FY2024 features are supported
     */
    public boolean supportsFY2024() {
        return configuredVersion.isAtLeast(HmisExportVersion.FY2024);
    }

    /**
     * Check if FY2022 compatibility mode is active
     */
    public boolean supportsFY2022() {
        return configuredVersion == HmisExportVersion.FY2022 || !strictMode;
    }

    /**
     * Check if specific field is supported in current version
     *
     * @param fieldName Field name to check
     * @param csvFile CSV file name
     * @return true if field is supported in current version
     */
    public boolean supportsField(String fieldName, String csvFile) {
        return switch (fieldName) {
            // FY2024+ fields
            case "DataCollectionStage" -> supportsFY2024();
            case "IncomeFromAnySource", "BenefitsFromAnySource" -> supportsFY2024();
            case "OtherIncomeSourceIdentify", "OtherBenefitsSourceIdentify" -> supportsFY2024();

            // HealthAndDV FY2024+ fields
            case "COBRA", "StateHealthInsforAdults", "IndianHealthServices" -> supportsFY2024();
            case "PhysicalDisabilityLongterm", "DevelopmentalDisabilityLongterm",
                 "ChronicHealthConditionLongterm", "HIVLongterm",
                 "MentalHealthDisorderLongterm", "SubstanceUseDisorderLongterm" -> supportsFY2024();
            case "TCellCount", "TCellSource", "ViralLoadCount", "ViralLoadSource", "AntiRetroviral" -> supportsFY2024();
            case "CurrentlyFleeing", "WhenOccurred" -> supportsFY2024();

            // All other fields exist in FY2022+
            default -> true;
        };
    }

    /**
     * Get effective date for current version
     */
    public LocalDate getEffectiveDate() {
        return configuredVersion.getEffectiveFrom();
    }

    /**
     * Check if export should include legacy field for backwards compatibility
     */
    public boolean includeLegacyField(String fieldName) {
        // In strict mode, only include fields for current version
        // In compatibility mode, include both current and legacy fields
        return !strictMode;
    }

    /**
     * HMIS Export Version enum
     */
    public enum HmisExportVersion {
        FY2022(LocalDate.of(2022, 10, 1), LocalDate.of(2024, 9, 30)),
        FY2024(LocalDate.of(2024, 10, 1), null); // Open-ended (current)

        private final LocalDate effectiveFrom;
        private final LocalDate effectiveTo;

        HmisExportVersion(LocalDate effectiveFrom, LocalDate effectiveTo) {
            this.effectiveFrom = effectiveFrom;
            this.effectiveTo = effectiveTo;
        }

        public LocalDate getEffectiveFrom() {
            return effectiveFrom;
        }

        public LocalDate getEffectiveTo() {
            return effectiveTo;
        }

        public boolean isActive(LocalDate date) {
            boolean afterStart = !date.isBefore(effectiveFrom);
            boolean beforeEnd = effectiveTo == null || !date.isAfter(effectiveTo);
            return afterStart && beforeEnd;
        }

        public boolean isAtLeast(HmisExportVersion other) {
            return this.compareTo(other) >= 0;
        }
    }
}

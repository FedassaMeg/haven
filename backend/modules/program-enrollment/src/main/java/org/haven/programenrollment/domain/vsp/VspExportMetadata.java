package org.haven.programenrollment.domain.vsp;

import org.haven.programenrollment.domain.ce.CeShareScope;
import org.haven.shared.vo.hmis.VawaRecipientCategory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Domain entity for VSP export metadata tracking
 * Stores comprehensive export history with VAWA-compliant recipient categorization
 */
public class VspExportMetadata {

    private final UUID exportId;
    private final String recipient;
    private final VawaRecipientCategory recipientCategory;
    private final String consentBasis;
    private final String packetHash;
    private final String ceHashKey; // CE-specific hash key replacing household IDs
    private final Instant exportTimestamp;
    private final LocalDateTime expiryDate;
    private final Set<CeShareScope> shareScopes;
    private final AnonymizationRules anonymizationRules;
    private final Map<String, Object> metadata;
    private final String initiatedBy;
    private ExportStatus status;
    private Instant revokedAt;
    private String revokedBy;
    private String revocationReason;

    public VspExportMetadata(
            UUID exportId,
            String recipient,
            VawaRecipientCategory recipientCategory,
            String consentBasis,
            String packetHash,
            String ceHashKey,
            Instant exportTimestamp,
            LocalDateTime expiryDate,
            Set<CeShareScope> shareScopes,
            AnonymizationRules anonymizationRules,
            Map<String, Object> metadata,
            String initiatedBy) {
        this.exportId = exportId;
        this.recipient = recipient;
        this.recipientCategory = recipientCategory;
        this.consentBasis = consentBasis;
        this.packetHash = packetHash;
        this.ceHashKey = ceHashKey;
        this.exportTimestamp = exportTimestamp;
        this.expiryDate = expiryDate;
        this.shareScopes = new HashSet<>(shareScopes);
        this.anonymizationRules = anonymizationRules;
        this.metadata = new HashMap<>(metadata);
        this.initiatedBy = initiatedBy;
        this.status = ExportStatus.ACTIVE;
    }

    /**
     * Revoke the export with reason and audit trail
     */
    public void revoke(String revokedBy, String reason) {
        if (this.status == ExportStatus.REVOKED) {
            throw new IllegalStateException("Export is already revoked");
        }
        if (this.status == ExportStatus.EXPIRED) {
            throw new IllegalStateException("Cannot revoke expired export");
        }

        this.status = ExportStatus.REVOKED;
        this.revokedAt = Instant.now();
        this.revokedBy = revokedBy;
        this.revocationReason = reason;
    }

    /**
     * Check if export has expired
     */
    public boolean isExpired() {
        return expiryDate != null && LocalDateTime.now().isAfter(expiryDate);
    }

    /**
     * Update status to expired if past expiry date
     */
    public void checkAndUpdateExpiry() {
        if (isExpired() && status == ExportStatus.ACTIVE) {
            this.status = ExportStatus.EXPIRED;
        }
    }

    /**
     * Validate VAWA recipient category compliance
     */
    public boolean isVawaCompliant() {
        return recipientCategory != null &&
               recipientCategory.isAuthorizedForVictimData();
    }

    /**
     * Get anonymized export data applying CE-specific rules
     */
    public Map<String, Object> getAnonymizedData(Map<String, Object> originalData) {
        return anonymizationRules.apply(originalData, ceHashKey);
    }

    // Getters
    public UUID getExportId() { return exportId; }
    public String getRecipient() { return recipient; }
    public VawaRecipientCategory getRecipientCategory() { return recipientCategory; }
    public String getConsentBasis() { return consentBasis; }
    public String getPacketHash() { return packetHash; }
    public String getCeHashKey() { return ceHashKey; }
    public Instant getExportTimestamp() { return exportTimestamp; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public Set<CeShareScope> getShareScopes() { return Collections.unmodifiableSet(shareScopes); }
    public AnonymizationRules getAnonymizationRules() { return anonymizationRules; }
    public Map<String, Object> getMetadata() { return Collections.unmodifiableMap(metadata); }
    public String getInitiatedBy() { return initiatedBy; }
    public ExportStatus getStatus() { return status; }
    public Instant getRevokedAt() { return revokedAt; }
    public String getRevokedBy() { return revokedBy; }
    public String getRevocationReason() { return revocationReason; }

    public enum ExportStatus {
        ACTIVE,
        REVOKED,
        EXPIRED,
        PENDING_APPROVAL
    }

    /**
     * CE-specific anonymization rules
     */
    public static class AnonymizationRules {
        private final boolean suppressLocationMetadata;
        private final boolean replaceHouseholdIds;
        private final boolean redactDvIndicators;
        private final boolean anonymizeDates;
        private final Set<String> fieldsToRedact;
        private final Map<String, String> fieldMappings;

        public AnonymizationRules(
                boolean suppressLocationMetadata,
                boolean replaceHouseholdIds,
                boolean redactDvIndicators,
                boolean anonymizeDates,
                Set<String> fieldsToRedact,
                Map<String, String> fieldMappings) {
            this.suppressLocationMetadata = suppressLocationMetadata;
            this.replaceHouseholdIds = replaceHouseholdIds;
            this.redactDvIndicators = redactDvIndicators;
            this.anonymizeDates = anonymizeDates;
            this.fieldsToRedact = new HashSet<>(fieldsToRedact);
            this.fieldMappings = new HashMap<>(fieldMappings);
        }

        /**
         * Apply anonymization rules to data
         */
        @SuppressWarnings("unchecked")
        public Map<String, Object> apply(Map<String, Object> data, String ceHashKey) {
            Map<String, Object> anonymized = new HashMap<>(data);

            // Suppress location metadata
            if (suppressLocationMetadata) {
                anonymized.remove("locationData");
                anonymized.remove("gpsCoordinates");
                anonymized.remove("address");
                anonymized.remove("zipCode");
            }

            // Replace household IDs with CE hash keys
            if (replaceHouseholdIds && anonymized.containsKey("householdId")) {
                anonymized.put("householdId", ceHashKey);
                anonymized.put("householdHash", generateHouseholdHash(ceHashKey));
            }

            // Redact DV indicators
            if (redactDvIndicators) {
                anonymized.remove("dvStatus");
                anonymized.remove("fleeingDv");
                anonymized.remove("domesticViolenceIndicator");
            }

            // Anonymize dates
            if (anonymizeDates) {
                anonymizeDateFields(anonymized);
            }

            // Apply field-specific redactions
            for (String field : fieldsToRedact) {
                anonymized.remove(field);
            }

            // Apply field mappings
            for (Map.Entry<String, String> mapping : fieldMappings.entrySet()) {
                if (anonymized.containsKey(mapping.getKey())) {
                    Object value = anonymized.remove(mapping.getKey());
                    anonymized.put(mapping.getValue(), value);
                }
            }

            return anonymized;
        }

        private void anonymizeDateFields(Map<String, Object> data) {
            // Anonymize dates to month/year only
            List<String> dateFields = Arrays.asList("dateOfBirth", "entryDate", "exitDate", "assessmentDate");
            for (String field : dateFields) {
                if (data.containsKey(field) && data.get(field) != null) {
                    data.put(field, anonymizeDate(data.get(field)));
                }
            }
        }

        private String anonymizeDate(Object date) {
            // Convert to month/year format only
            if (date instanceof LocalDateTime) {
                LocalDateTime dt = (LocalDateTime) date;
                return String.format("%02d/%d", dt.getMonthValue(), dt.getYear());
            }
            return "REDACTED";
        }

        private String generateHouseholdHash(String ceHashKey) {
            // Generate consistent hash for household based on CE hash key
            return "HH_" + Integer.toHexString(ceHashKey.hashCode());
        }

        // Builder for AnonymizationRules
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private boolean suppressLocationMetadata = true;
            private boolean replaceHouseholdIds = true;
            private boolean redactDvIndicators = true;
            private boolean anonymizeDates = false;
            private Set<String> fieldsToRedact = new HashSet<>();
            private Map<String, String> fieldMappings = new HashMap<>();

            public Builder suppressLocationMetadata(boolean suppress) {
                this.suppressLocationMetadata = suppress;
                return this;
            }

            public Builder replaceHouseholdIds(boolean replace) {
                this.replaceHouseholdIds = replace;
                return this;
            }

            public Builder redactDvIndicators(boolean redact) {
                this.redactDvIndicators = redact;
                return this;
            }

            public Builder anonymizeDates(boolean anonymize) {
                this.anonymizeDates = anonymize;
                return this;
            }

            public Builder addFieldToRedact(String field) {
                this.fieldsToRedact.add(field);
                return this;
            }

            public Builder addFieldMapping(String from, String to) {
                this.fieldMappings.put(from, to);
                return this;
            }

            public AnonymizationRules build() {
                return new AnonymizationRules(
                    suppressLocationMetadata,
                    replaceHouseholdIds,
                    redactDvIndicators,
                    anonymizeDates,
                    fieldsToRedact,
                    fieldMappings
                );
            }
        }
    }
}
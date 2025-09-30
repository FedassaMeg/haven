package org.haven.programenrollment.application.services;

import org.haven.programenrollment.domain.IntakePsdeRecord;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Represents the complete audit trail for an IntakePsdeRecord
 * Includes all versions, corrections, and related records
 */
public class IntakePsdeAuditTrail {

    private final UUID primaryRecordId;
    private final List<AuditTrailEntry> entries;
    private final AuditTrailSummary summary;

    private IntakePsdeAuditTrail(UUID primaryRecordId, List<AuditTrailEntry> entries) {
        this.primaryRecordId = primaryRecordId;
        this.entries = entries;
        this.summary = generateSummary();
    }

    /**
     * Create audit trail from a record and its related records
     */
    public static IntakePsdeAuditTrail create(IntakePsdeRecord primaryRecord, List<IntakePsdeRecord> allRelatedRecords) {
        List<AuditTrailEntry> entries = allRelatedRecords.stream()
            .map(AuditTrailEntry::from)
            .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
            .collect(Collectors.toList());

        return new IntakePsdeAuditTrail(primaryRecord.getRecordId(), entries);
    }

    /**
     * Generate summary statistics for the audit trail
     */
    private AuditTrailSummary generateSummary() {
        long totalVersions = entries.stream()
            .filter(entry -> entry.getEntryType() == AuditTrailEntryType.VERSION_UPDATE)
            .count();

        long totalCorrections = entries.stream()
            .filter(entry -> entry.getEntryType() == AuditTrailEntryType.CORRECTION)
            .count();

        Instant firstCreated = entries.stream()
            .filter(entry -> entry.getEntryType() == AuditTrailEntryType.CREATION)
            .map(AuditTrailEntry::getTimestamp)
            .min(Instant::compareTo)
            .orElse(null);

        Instant lastModified = entries.stream()
            .map(AuditTrailEntry::getTimestamp)
            .max(Instant::compareTo)
            .orElse(null);

        List<String> involvedUsers = entries.stream()
            .map(AuditTrailEntry::getModifiedBy)
            .distinct()
            .collect(Collectors.toList());

        return new AuditTrailSummary(
            totalVersions,
            totalCorrections,
            firstCreated,
            lastModified,
            involvedUsers
        );
    }

    // Getters
    public UUID getPrimaryRecordId() { return primaryRecordId; }
    public List<AuditTrailEntry> getEntries() { return entries; }
    public AuditTrailSummary getSummary() { return summary; }

    /**
     * Individual entry in the audit trail
     */
    public static class AuditTrailEntry {
        private final UUID recordId;
        private final AuditTrailEntryType entryType;
        private final Instant timestamp;
        private final String modifiedBy;
        private final String description;
        private final List<String> changedFields;
        private final UUID relatedRecordId;
        private final String correctionReason;
        private final IntakePsdeLifecycleService.IntakePsdeLifecycleStatus lifecycleStatus;

        private AuditTrailEntry(
                UUID recordId,
                AuditTrailEntryType entryType,
                Instant timestamp,
                String modifiedBy,
                String description,
                List<String> changedFields,
                UUID relatedRecordId,
                String correctionReason,
                IntakePsdeLifecycleService.IntakePsdeLifecycleStatus lifecycleStatus) {
            this.recordId = recordId;
            this.entryType = entryType;
            this.timestamp = timestamp;
            this.modifiedBy = modifiedBy;
            this.description = description;
            this.changedFields = changedFields;
            this.relatedRecordId = relatedRecordId;
            this.correctionReason = correctionReason;
            this.lifecycleStatus = lifecycleStatus;
        }

        public static AuditTrailEntry from(IntakePsdeRecord record) {
            AuditTrailEntryType entryType = determineEntryType(record);
            String description = generateDescription(record, entryType);

            return new AuditTrailEntry(
                record.getRecordId(),
                entryType,
                record.getCreatedAt(),
                record.getCollectedBy(),
                description,
                List.of(), // TODO: Implement changed fields tracking
                record.getCorrectsRecordId(),
                record.getCorrectionReason(),
                record.getLifecycleStatus() != null ?
                    IntakePsdeLifecycleService.IntakePsdeLifecycleStatus.valueOf(record.getLifecycleStatus()) : null
            );
        }

        private static AuditTrailEntryType determineEntryType(IntakePsdeRecord record) {
            if (record.getIsCorrection() != null && record.getIsCorrection()) {
                return AuditTrailEntryType.CORRECTION;
            }
            if (record.getVersion() != null && record.getVersion() > 1) {
                return AuditTrailEntryType.VERSION_UPDATE;
            }
            if (record.getIsBackdated() != null && record.getIsBackdated()) {
                return AuditTrailEntryType.BACKDATED_ENTRY;
            }
            return AuditTrailEntryType.CREATION;
        }

        private static String generateDescription(IntakePsdeRecord record, AuditTrailEntryType entryType) {
            return switch (entryType) {
                case CREATION -> "Initial PSDE record created";
                case VERSION_UPDATE -> "Record updated (version " + record.getVersion() + ")";
                case CORRECTION -> "Correction applied: " +
                    (record.getCorrectionReason() != null ? record.getCorrectionReason() : "Unknown reason");
                case BACKDATED_ENTRY -> "Backdated entry created for " + record.getInformationDate();
                case SUPERSESSION -> "Record superseded by newer version";
            };
        }

        // Getters
        public UUID getRecordId() { return recordId; }
        public AuditTrailEntryType getEntryType() { return entryType; }
        public Instant getTimestamp() { return timestamp; }
        public String getModifiedBy() { return modifiedBy; }
        public String getDescription() { return description; }
        public List<String> getChangedFields() { return changedFields; }
        public UUID getRelatedRecordId() { return relatedRecordId; }
        public String getCorrectionReason() { return correctionReason; }
        public IntakePsdeLifecycleService.IntakePsdeLifecycleStatus getLifecycleStatus() { return lifecycleStatus; }
    }

    /**
     * Summary of audit trail statistics
     */
    public static class AuditTrailSummary {
        private final long totalVersions;
        private final long totalCorrections;
        private final Instant firstCreated;
        private final Instant lastModified;
        private final List<String> involvedUsers;

        public AuditTrailSummary(
                long totalVersions,
                long totalCorrections,
                Instant firstCreated,
                Instant lastModified,
                List<String> involvedUsers) {
            this.totalVersions = totalVersions;
            this.totalCorrections = totalCorrections;
            this.firstCreated = firstCreated;
            this.lastModified = lastModified;
            this.involvedUsers = involvedUsers;
        }

        // Getters
        public long getTotalVersions() { return totalVersions; }
        public long getTotalCorrections() { return totalCorrections; }
        public Instant getFirstCreated() { return firstCreated; }
        public Instant getLastModified() { return lastModified; }
        public List<String> getInvolvedUsers() { return involvedUsers; }
    }

    /**
     * Types of audit trail entries
     */
    public enum AuditTrailEntryType {
        CREATION,
        VERSION_UPDATE,
        CORRECTION,
        BACKDATED_ENTRY,
        SUPERSESSION
    }
}
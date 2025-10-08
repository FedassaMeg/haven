package org.haven.programenrollment.application.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data quality alert for TH/RRH transition violations
 */
public class DataQualityAlert {

    private UUID id;
    private AlertType alertType;
    private String message;
    private UUID enrollmentId;
    private Severity severity;
    private LocalDate createdDate;
    private boolean resolved;
    private String resolvedBy;
    private String resolutionNotes;
    private LocalDateTime resolvedAt;

    public DataQualityAlert(UUID id, AlertType alertType, String message, UUID enrollmentId,
                           Severity severity, LocalDate createdDate, boolean resolved,
                           String resolvedBy, String resolutionNotes) {
        this.id = id;
        this.alertType = alertType;
        this.message = message;
        this.enrollmentId = enrollmentId;
        this.severity = severity;
        this.createdDate = createdDate;
        this.resolved = resolved;
        this.resolvedBy = resolvedBy;
        this.resolutionNotes = resolutionNotes;
    }

    /**
     * Resolve the alert
     */
    public void resolve(String resolvedBy, String resolutionNotes) {
        this.resolved = true;
        this.resolvedBy = resolvedBy;
        this.resolutionNotes = resolutionNotes;
        this.resolvedAt = LocalDateTime.now();
    }

    public enum AlertType {
        MOVE_IN_DATE_VIOLATION("Move-in Date Violation"),
        EXCESSIVE_TRANSITION_GAP("Excessive Transition Gap"),
        MISSING_PREDECESSOR("Missing Predecessor Linkage"),
        OVERLAPPING_ENROLLMENTS("Overlapping Enrollments"),
        HOUSEHOLD_INCONSISTENCY("Household Inconsistency");

        private final String displayName;

        AlertType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum Severity {
        LOW("Low"),
        MEDIUM("Medium"),
        HIGH("High"),
        CRITICAL("Critical");

        private final String displayName;

        Severity(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // Getters
    public UUID getId() { return id; }
    public AlertType getAlertType() { return alertType; }
    public String getMessage() { return message; }
    public UUID getEnrollmentId() { return enrollmentId; }
    public Severity getSeverity() { return severity; }
    public LocalDate getCreatedDate() { return createdDate; }
    public boolean isResolved() { return resolved; }
    public String getResolvedBy() { return resolvedBy; }
    public String getResolutionNotes() { return resolutionNotes; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
}
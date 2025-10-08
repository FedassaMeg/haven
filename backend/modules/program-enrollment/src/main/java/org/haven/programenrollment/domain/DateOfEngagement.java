package org.haven.programenrollment.domain;

import org.haven.clientprofile.domain.ClientId;
import java.time.LocalDate;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Date of Engagement Record
 * Tracks the date when a client was first engaged in services
 * Required for street outreach programs per HMIS FY2024 standards
 */
public class DateOfEngagement {
    
    private UUID recordId;
    private ProgramEnrollmentId enrollmentId;
    private ClientId clientId;
    private LocalDate engagementDate;
    
    // Correction tracking
    private boolean isCorrection;
    private UUID correctsRecordId;
    
    // Audit fields
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    
    public DateOfEngagement() {
        this.recordId = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.isCorrection = false;
    }
    
    /**
     * Create initial date of engagement
     */
    public static DateOfEngagement create(
            ProgramEnrollmentId enrollmentId,
            ClientId clientId,
            LocalDate engagementDate,
            String createdBy) {
        
        if (engagementDate == null) {
            throw new IllegalArgumentException("Engagement date is required");
        }
        
        if (engagementDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Engagement date cannot be in the future");
        }
        
        DateOfEngagement record = new DateOfEngagement();
        record.enrollmentId = enrollmentId;
        record.clientId = clientId;
        record.engagementDate = engagementDate;
        record.createdBy = createdBy;
        
        return record;
    }
    
    /**
     * Create correction for existing engagement date
     */
    public static DateOfEngagement createCorrection(
            DateOfEngagement originalRecord,
            LocalDate newEngagementDate,
            String createdBy) {
        
        if (newEngagementDate == null) {
            throw new IllegalArgumentException("Engagement date is required");
        }
        
        if (newEngagementDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Engagement date cannot be in the future");
        }
        
        DateOfEngagement correction = new DateOfEngagement();
        correction.enrollmentId = originalRecord.enrollmentId;
        correction.clientId = originalRecord.clientId;
        correction.engagementDate = newEngagementDate;
        correction.isCorrection = true;
        correction.correctsRecordId = originalRecord.recordId;
        correction.createdBy = createdBy;
        
        return correction;
    }
    
    /**
     * Check if engagement date is valid
     */
    public boolean isEngagementDateValid() {
        return engagementDate != null && !engagementDate.isAfter(LocalDate.now());
    }
    
    /**
     * Check if engagement date is after enrollment date
     */
    public boolean isAfterEnrollmentDate(LocalDate enrollmentDate) {
        return engagementDate != null && 
               enrollmentDate != null && 
               !engagementDate.isBefore(enrollmentDate);
    }
    
    /**
     * Check if engagement occurred within specified days of enrollment
     */
    public boolean isWithinDaysOfEnrollment(LocalDate enrollmentDate, int days) {
        if (engagementDate == null || enrollmentDate == null) {
            return false;
        }
        
        LocalDate maxDate = enrollmentDate.plusDays(days);
        return !engagementDate.isBefore(enrollmentDate) && !engagementDate.isAfter(maxDate);
    }
    
    /**
     * Calculate days between enrollment and engagement
     */
    public Long getDaysFromEnrollment(LocalDate enrollmentDate) {
        if (engagementDate == null || enrollmentDate == null) {
            return null;
        }
        
        return java.time.temporal.ChronoUnit.DAYS.between(enrollmentDate, engagementDate);
    }
    
    /**
     * Check if this is a timely engagement (within 14 days)
     */
    public boolean isTimelyEngagement(LocalDate enrollmentDate) {
        return isWithinDaysOfEnrollment(enrollmentDate, 14);
    }
    
    // Getters
    public UUID getRecordId() { return recordId; }
    public ProgramEnrollmentId getEnrollmentId() { return enrollmentId; }
    public ClientId getClientId() { return clientId; }
    public LocalDate getEngagementDate() { return engagementDate; }
    public boolean isCorrection() { return isCorrection; }
    public UUID getCorrectsRecordId() { return correctsRecordId; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DateOfEngagement that = (DateOfEngagement) o;
        return Objects.equals(recordId, that.recordId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(recordId);
    }
}
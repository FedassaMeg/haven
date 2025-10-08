package org.haven.programenrollment.domain;

import org.haven.clientprofile.domain.ClientId;
import java.time.LocalDate;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Bed Night Record
 * Tracks individual nights stayed in emergency shelter
 * Required for Night-by-Night emergency shelter tracking per HMIS standards
 */
public class BedNight {
    
    private UUID recordId;
    private ProgramEnrollmentId enrollmentId;
    private ClientId clientId;
    private LocalDate bedNightDate;
    
    // Audit fields
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt; // May not be needed
    
    public BedNight() {
        this.recordId = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    /**
     * Create a single bed night record
     */
    public static BedNight create(
            ProgramEnrollmentId enrollmentId,
            ClientId clientId,
            LocalDate bedNightDate,
            String createdBy) {
        
        if (bedNightDate == null) {
            throw new IllegalArgumentException("Bed night date is required");
        }
        
        if (bedNightDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Bed night date cannot be in the future");
        }
        
        BedNight bedNight = new BedNight();
        bedNight.enrollmentId = enrollmentId;
        bedNight.clientId = clientId;
        bedNight.bedNightDate = bedNightDate;
        bedNight.createdBy = createdBy;
        
        return bedNight;
    }
    
    /**
     * Check if bed night date is valid
     */
    public boolean isBedNightDateValid() {
        return bedNightDate != null && !bedNightDate.isAfter(LocalDate.now());
    }
    
    /**
     * Check if bed night is within enrollment period
     */
    public boolean isWithinEnrollmentPeriod(LocalDate enrollmentDate, LocalDate exitDate) {
        if (bedNightDate == null || enrollmentDate == null) {
            return false;
        }
        
        if (bedNightDate.isBefore(enrollmentDate)) {
            return false;
        }
        
        if (exitDate != null && bedNightDate.isAfter(exitDate)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if this is a recent bed night (within last 90 days)
     */
    public boolean isRecent() {
        if (bedNightDate == null) {
            return false;
        }
        
        LocalDate ninetyDaysAgo = LocalDate.now().minusDays(90);
        return !bedNightDate.isBefore(ninetyDaysAgo);
    }
    
    /**
     * Get days since this bed night
     */
    public Long getDaysSinceBedNight() {
        if (bedNightDate == null) {
            return null;
        }
        
        return java.time.temporal.ChronoUnit.DAYS.between(bedNightDate, LocalDate.now());
    }
    
    // Getters
    public UUID getRecordId() { return recordId; }
    public ProgramEnrollmentId getEnrollmentId() { return enrollmentId; }
    public ClientId getClientId() { return clientId; }
    public LocalDate getBedNightDate() { return bedNightDate; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BedNight that = (BedNight) o;
        return Objects.equals(recordId, that.recordId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(recordId);
    }
}
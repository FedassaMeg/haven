package org.haven.programenrollment.domain;

import org.haven.shared.vo.hmis.PriorLivingSituation;
import org.haven.clientprofile.domain.ClientId;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Current Living Situation Contact
 * Tracks client contacts and their living situation at time of contact
 * Per HMIS FY2024 standards for street outreach and other programs
 */
public class CurrentLivingSituation {
    
    private UUID recordId;
    private ProgramEnrollmentId enrollmentId;
    private ClientId clientId;
    private LocalDate contactDate;
    
    // Living situation details
    private PriorLivingSituation livingSituation;
    private String locationDescription;
    
    // Verification
    private String verifiedBy;
    
    // Optional time tracking
    private LocalTime contactTime;
    private Integer durationMinutes;
    
    // Audit fields
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    
    public CurrentLivingSituation() {
        this.recordId = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    /**
     * Create a new current living situation contact
     */
    public static CurrentLivingSituation create(
            ProgramEnrollmentId enrollmentId,
            ClientId clientId,
            LocalDate contactDate,
            PriorLivingSituation livingSituation,
            String createdBy) {
        
        CurrentLivingSituation cls = new CurrentLivingSituation();
        cls.enrollmentId = enrollmentId;
        cls.clientId = clientId;
        cls.contactDate = contactDate;
        cls.livingSituation = livingSituation;
        cls.createdBy = createdBy;
        
        return cls;
    }
    
    /**
     * Create with additional details
     */
    public static CurrentLivingSituation createWithDetails(
            ProgramEnrollmentId enrollmentId,
            ClientId clientId,
            LocalDate contactDate,
            PriorLivingSituation livingSituation,
            String locationDescription,
            String verifiedBy,
            String createdBy) {
        
        CurrentLivingSituation cls = create(enrollmentId, clientId, contactDate, livingSituation, createdBy);
        cls.locationDescription = locationDescription;
        cls.verifiedBy = verifiedBy;
        
        return cls;
    }
    
    /**
     * Update location description
     */
    public void updateLocationDescription(String locationDescription) {
        this.locationDescription = locationDescription;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Update verification information
     */
    public void updateVerification(String verifiedBy) {
        this.verifiedBy = verifiedBy;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Add time details to the contact
     */
    public void addTimeDetails(LocalTime contactTime, Integer durationMinutes) {
        this.contactTime = contactTime;
        this.durationMinutes = durationMinutes;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Check if contact date is valid (not in future)
     */
    public boolean isContactDateValid() {
        return contactDate != null && !contactDate.isAfter(LocalDate.now());
    }
    
    /**
     * Check if client is literally homeless based on living situation
     */
    public boolean isLiterallyHomeless() {
        return livingSituation != null && livingSituation.isLiterallyHomeless();
    }
    
    /**
     * Check if this represents an unsheltered contact
     */
    public boolean isUnsheltered() {
        return livingSituation == PriorLivingSituation.PLACE_NOT_MEANT_FOR_HABITATION;
    }
    
    /**
     * Check if this represents an emergency shelter contact
     */
    public boolean isEmergencyShelter() {
        return livingSituation == PriorLivingSituation.EMERGENCY_SHELTER;
    }
    
    /**
     * Check if this is a street outreach contact
     */
    public boolean isStreetOutreachContact() {
        return isUnsheltered() || 
               livingSituation == PriorLivingSituation.SAFE_HAVEN;
    }
    
    /**
     * Check if client is at risk of homelessness
     */
    public boolean isAtRiskOfHomelessness() {
        return livingSituation != null && livingSituation.isAtRiskOfHomelessness();
    }
    
    /**
     * Check if client is in stable housing
     */
    public boolean isStablyHoused() {
        return livingSituation != null && livingSituation.isStableHousing();
    }
    
    /**
     * Check if this contact has been verified
     */
    public boolean isVerified() {
        return verifiedBy != null && !verifiedBy.trim().isEmpty();
    }
    
    /**
     * Check if this is transitional housing
     */
    public boolean isTransitionalHousing() {
        return livingSituation == PriorLivingSituation.TRANSITIONAL_HOUSING;
    }
    
    /**
     * Check if this is permanent housing
     */
    public boolean isPermanentHousing() {
        return livingSituation != null && livingSituation.isPermanentHousing();
    }
    
    /**
     * Check if this is an institutional setting
     */
    public boolean isInstitutional() {
        return livingSituation != null && livingSituation.isInstitutional();
    }
    
    /**
     * Check if this is temporary housing
     */
    public boolean isTemporaryHousing() {
        return livingSituation != null && livingSituation.isTemporaryHousing();
    }
    
    /**
     * Check if this is a homeless situation
     */
    public boolean isHomelessSituation() {
        return livingSituation != null && livingSituation.isLiterallyHomeless();
    }
    
    /**
     * Check if this is temporarily homeless
     */
    public boolean isTemporarilyHomeless() {
        return livingSituation != null && livingSituation.isTemporaryHousing();
    }
    
    /**
     * Check if this is a known situation (not refused/unknown)
     */
    public boolean isKnownSituation() {
        return livingSituation != null && 
               livingSituation != PriorLivingSituation.CLIENT_DOESNT_KNOW &&
               livingSituation != PriorLivingSituation.CLIENT_PREFERS_NOT_TO_ANSWER &&
               livingSituation != PriorLivingSituation.DATA_NOT_COLLECTED;
    }
    
    /**
     * Get contact duration in hours
     */
    public Double getContactDurationHours() {
        if (durationMinutes == null || durationMinutes <= 0) {
            return null;
        }
        return durationMinutes / 60.0;
    }
    
    // Getters
    public UUID getRecordId() { return recordId; }
    public ProgramEnrollmentId getEnrollmentId() { return enrollmentId; }
    public ClientId getClientId() { return clientId; }
    public LocalDate getContactDate() { return contactDate; }
    public PriorLivingSituation getLivingSituation() { return livingSituation; }
    public String getLocationDescription() { return locationDescription; }
    public String getVerifiedBy() { return verifiedBy; }
    public LocalTime getContactTime() { return contactTime; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CurrentLivingSituation that = (CurrentLivingSituation) o;
        return Objects.equals(recordId, that.recordId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(recordId);
    }
}
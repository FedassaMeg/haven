package org.haven.programenrollment.domain;

import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.time.LocalDate;

/**
 * HMIS-aligned project exit entity
 * Tracks formal exit from program with destination and outcome data
 */
public class ProjectExit {
    private LocalDate exitDate;
    private CodeableConcept exitReason;
    private CodeableConcept destination;
    private CodeableConcept housingAssessment;
    private CodeableConcept subsidyInformation;
    private String notes;
    private Boolean counselingSessionsCompleted;
    private Boolean exitedToPermanentHousing;
    private String recordedBy;
    private Instant recordedAt;
    
    public ProjectExit(LocalDate exitDate, CodeableConcept exitReason, 
                      CodeableConcept destination, String recordedBy) {
        this.exitDate = exitDate;
        this.exitReason = exitReason;
        this.destination = destination;
        this.recordedBy = recordedBy;
        this.recordedAt = Instant.now();
    }
    
    public void updateHousingOutcome(CodeableConcept housingAssessment, 
                                   Boolean exitedToPermanentHousing) {
        this.housingAssessment = housingAssessment;
        this.exitedToPermanentHousing = exitedToPermanentHousing;
    }
    
    public void updateSubsidyInformation(CodeableConcept subsidyInfo) {
        this.subsidyInformation = subsidyInfo;
    }
    
    public void recordCounselingCompletion(Boolean completed) {
        this.counselingSessionsCompleted = completed;
    }
    
    public void addNotes(String notes) {
        this.notes = notes;
    }
    
    // HMIS exit destination categories
    public enum HmisExitDestination {
        EMERGENCY_SHELTER,
        TRANSITIONAL_HOUSING,
        PERMANENT_HOUSING,
        PSYCHIATRIC_HOSPITAL,
        SUBSTANCE_ABUSE_TREATMENT,
        HOSPITAL_NON_PSYCHIATRIC,
        JAIL_PRISON,
        CLIENT_DOESNT_KNOW,
        CLIENT_REFUSED,
        NO_EXIT_INTERVIEW_COMPLETED,
        OTHER,
        DECEASED
    }
    
    // HMIS exit reasons
    public enum HmisExitReason {
        COMPLETED_PROGRAM,
        NON_COMPLIANCE_WITH_PROGRAM,
        CRIMINAL_ACTIVITY,
        REACHED_MAXIMUM_TIME_ALLOWED,
        NEEDS_COULD_NOT_BE_MET,
        DISAGREEMENT_WITH_RULES,
        LEFT_FOR_HOUSING_OPPORTUNITY,
        CLIENT_VOLUNTARILY_LEFT,
        OTHER,
        DECEASED
    }
    
    // Getters
    public LocalDate getExitDate() { return exitDate; }
    public CodeableConcept getExitReason() { return exitReason; }
    public CodeableConcept getDestination() { return destination; }
    public CodeableConcept getHousingAssessment() { return housingAssessment; }
    public CodeableConcept getSubsidyInformation() { return subsidyInformation; }
    public String getNotes() { return notes; }
    public Boolean getCounselingSessionsCompleted() { return counselingSessionsCompleted; }
    public Boolean getExitedToPermanentHousing() { return exitedToPermanentHousing; }
    public String getRecordedBy() { return recordedBy; }
    public Instant getRecordedAt() { return recordedAt; }
}
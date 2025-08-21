package org.haven.programenrollment.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "project_exits", schema = "haven")
public class JpaProjectExitEntity {
    
    @Id
    @Column(name = "id")
    private UUID id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private JpaProgramEnrollmentEntity enrollment;
    
    @Column(name = "exit_date", nullable = false)
    private LocalDate exitDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "destination", nullable = false)
    private HmisExitDestination destination;
    
    @Column(name = "exited_to_permanent_housing")
    private Boolean exitedToPermanentHousing;
    
    @Column(name = "housing_assessment")
    private String housingAssessment;
    
    @Column(name = "subsidy_information")
    private String subsidyInformation;
    
    @Column(name = "counseling_sessions_completed")
    private Boolean counselingSessionsCompleted;
    
    @Column(name = "exit_reason")
    private String exitReason;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "completion_status")
    private CompletionStatus completionStatus;
    
    @Column(name = "exit_notes", columnDefinition = "TEXT")
    private String exitNotes;
    
    @Column(name = "follow_up_required")
    private Boolean followUpRequired;
    
    @Column(name = "follow_up_date")
    private LocalDate followUpDate;
    
    @Column(name = "recorded_by")
    private UUID recordedBy;
    
    @Column(name = "recorded_at")
    private Instant recordedAt;
    
    // Constructors
    protected JpaProjectExitEntity() {}
    
    public JpaProjectExitEntity(UUID id, JpaProgramEnrollmentEntity enrollment,
                              LocalDate exitDate, HmisExitDestination destination,
                              UUID recordedBy) {
        this.id = id;
        this.enrollment = enrollment;
        this.exitDate = exitDate;
        this.destination = destination;
        this.recordedBy = recordedBy;
        this.recordedAt = Instant.now();
    }
    
    // Enums
    public enum HmisExitDestination {
        EMERGENCY_SHELTER,
        TRANSITIONAL_HOUSING_FOR_HOMELESS_PERSONS,
        PERMANENT_HOUSING_IN_A_HOUSING_PROGRAM,
        OWNED_BY_CLIENT_NO_ONGOING_HOUSING_SUBSIDY,
        OWNED_BY_CLIENT_WITH_ONGOING_HOUSING_SUBSIDY,
        RENTAL_BY_CLIENT_NO_ONGOING_HOUSING_SUBSIDY,
        RENTAL_BY_CLIENT_WITH_VASH_HOUSING_SUBSIDY,
        RENTAL_BY_CLIENT_WITH_OTHER_ONGOING_HOUSING_SUBSIDY,
        STAYING_OR_LIVING_WITH_FAMILY_TEMPORARY_TENURE,
        STAYING_OR_LIVING_WITH_FRIENDS_TEMPORARY_TENURE,
        STAYING_OR_LIVING_WITH_FAMILY_PERMANENT_TENURE,
        STAYING_OR_LIVING_WITH_FRIENDS_PERMANENT_TENURE,
        HOTEL_OR_MOTEL_PAID_FOR_WITHOUT_EMERGENCY_SHELTER_VOUCHER,
        HOSPITAL_OR_OTHER_RESIDENTIAL_NON_PSYCHIATRIC_MEDICAL_FACILITY,
        JAIL_PRISON_OR_JUVENILE_DETENTION_FACILITY,
        LONG_TERM_CARE_FACILITY_OR_NURSING_HOME,
        PSYCHIATRIC_HOSPITAL_OR_OTHER_PSYCHIATRIC_FACILITY,
        SUBSTANCE_ABUSE_TREATMENT_FACILITY_OR_DETOX_CENTER,
        DECEASED,
        OTHER,
        CLIENT_DOESNT_KNOW,
        CLIENT_REFUSED,
        DATA_NOT_COLLECTED
    }
    
    public enum CompletionStatus {
        COMPLETED,
        PARTIAL,
        UNSUCCESSFUL
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public JpaProgramEnrollmentEntity getEnrollment() { return enrollment; }
    public void setEnrollment(JpaProgramEnrollmentEntity enrollment) { this.enrollment = enrollment; }
    
    public LocalDate getExitDate() { return exitDate; }
    public void setExitDate(LocalDate exitDate) { this.exitDate = exitDate; }
    
    public HmisExitDestination getDestination() { return destination; }
    public void setDestination(HmisExitDestination destination) { this.destination = destination; }
    
    public Boolean getExitedToPermanentHousing() { return exitedToPermanentHousing; }
    public void setExitedToPermanentHousing(Boolean exitedToPermanentHousing) { 
        this.exitedToPermanentHousing = exitedToPermanentHousing; 
    }
    
    public String getHousingAssessment() { return housingAssessment; }
    public void setHousingAssessment(String housingAssessment) { this.housingAssessment = housingAssessment; }
    
    public String getSubsidyInformation() { return subsidyInformation; }
    public void setSubsidyInformation(String subsidyInformation) { this.subsidyInformation = subsidyInformation; }
    
    public Boolean getCounselingSessionsCompleted() { return counselingSessionsCompleted; }
    public void setCounselingSessionsCompleted(Boolean counselingSessionsCompleted) { 
        this.counselingSessionsCompleted = counselingSessionsCompleted; 
    }
    
    public String getExitReason() { return exitReason; }
    public void setExitReason(String exitReason) { this.exitReason = exitReason; }
    
    public CompletionStatus getCompletionStatus() { return completionStatus; }
    public void setCompletionStatus(CompletionStatus completionStatus) { this.completionStatus = completionStatus; }
    
    public String getExitNotes() { return exitNotes; }
    public void setExitNotes(String exitNotes) { this.exitNotes = exitNotes; }
    
    public Boolean getFollowUpRequired() { return followUpRequired; }
    public void setFollowUpRequired(Boolean followUpRequired) { this.followUpRequired = followUpRequired; }
    
    public LocalDate getFollowUpDate() { return followUpDate; }
    public void setFollowUpDate(LocalDate followUpDate) { this.followUpDate = followUpDate; }
    
    public UUID getRecordedBy() { return recordedBy; }
    public void setRecordedBy(UUID recordedBy) { this.recordedBy = recordedBy; }
    
    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }
}
package org.haven.programenrollment.infrastructure.persistence;

import jakarta.persistence.*;
import org.haven.shared.vo.CodeableConcept;
import org.haven.shared.vo.Period;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "program_enrollments", schema = "haven")
public class JpaProgramEnrollmentEntity {
    
    @Id
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "client_id", nullable = false)
    private UUID clientId;
    
    @Column(name = "program_id", nullable = false)
    private UUID programId;
    
    @Column(name = "enrollment_date", nullable = false)
    private LocalDate enrollmentDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_to_head")
    private HmisRelationshipToHead relationshipToHead;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "residence_prior_to_entry")
    private HmisResidencePrior residencePriorToEntry;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "length_of_stay_prior_to_entry")
    private HmisLengthOfStay lengthOfStayPriorToEntry;
    
    @Column(name = "entry_from_street_outreach")
    private Boolean entryFromStreetOutreach;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private EnrollmentStatus status;
    
    @Column(name = "enrollment_period_start")
    private Instant enrollmentPeriodStart;
    
    @Column(name = "enrollment_period_end")
    private Instant enrollmentPeriodEnd;
    
    @OneToMany(mappedBy = "enrollment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<JpaServiceEpisodeEntity> serviceEpisodes = new ArrayList<>();
    
    @OneToOne(mappedBy = "enrollment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private JpaProjectExitEntity projectExit;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @Column(name = "created_by")
    private UUID createdBy;
    
    @Column(name = "updated_by")
    private UUID updatedBy;
    
    @Version
    @Column(name = "version")
    private Integer version;
    
    // Constructors
    protected JpaProgramEnrollmentEntity() {}
    
    public JpaProgramEnrollmentEntity(UUID id, UUID clientId, UUID programId, 
                                    LocalDate enrollmentDate) {
        this.id = id;
        this.clientId = clientId;
        this.programId = programId;
        this.enrollmentDate = enrollmentDate;
        this.status = EnrollmentStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    // Enums matching database types
    public enum HmisRelationshipToHead {
        SELF_HEAD_OF_HOUSEHOLD,
        HEAD_OF_HOUSEHOLD_SPOUSE_PARTNER,
        HEAD_OF_HOUSEHOLD_CHILD,
        HEAD_OF_HOUSEHOLD_STEP_CHILD,
        HEAD_OF_HOUSEHOLD_GRANDCHILD,
        HEAD_OF_HOUSEHOLD_PARENT,
        HEAD_OF_HOUSEHOLD_SIBLING,
        OTHER_RELATIVE,
        UNRELATED_HOUSEHOLD_MEMBER,
        CLIENT_DOESNT_KNOW,
        CLIENT_REFUSED,
        DATA_NOT_COLLECTED
    }
    
    public enum HmisResidencePrior {
        HOMELESS_SITUATION,
        INSTITUTIONAL_SETTING,
        HOUSED,
        CLIENT_DOESNT_KNOW,
        CLIENT_REFUSED,
        DATA_NOT_COLLECTED
    }
    
    public enum HmisLengthOfStay {
        ONE_WEEK_OR_LESS,
        MORE_THAN_ONE_WEEK_BUT_LESS_THAN_ONE_MONTH,
        ONE_TO_THREE_MONTHS,
        MORE_THAN_THREE_MONTHS_BUT_LESS_THAN_ONE_YEAR,
        ONE_YEAR_OR_LONGER,
        CLIENT_DOESNT_KNOW,
        CLIENT_REFUSED,
        DATA_NOT_COLLECTED
    }
    
    public enum EnrollmentStatus {
        PENDING, ACTIVE, SUSPENDED, EXITED, CANCELLED
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }
    
    public UUID getProgramId() { return programId; }
    public void setProgramId(UUID programId) { this.programId = programId; }
    
    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public void setEnrollmentDate(LocalDate enrollmentDate) { this.enrollmentDate = enrollmentDate; }
    
    public HmisRelationshipToHead getRelationshipToHead() { return relationshipToHead; }
    public void setRelationshipToHead(HmisRelationshipToHead relationshipToHead) { 
        this.relationshipToHead = relationshipToHead; 
    }
    
    public HmisResidencePrior getResidencePriorToEntry() { return residencePriorToEntry; }
    public void setResidencePriorToEntry(HmisResidencePrior residencePriorToEntry) { 
        this.residencePriorToEntry = residencePriorToEntry; 
    }
    
    public HmisLengthOfStay getLengthOfStayPriorToEntry() { return lengthOfStayPriorToEntry; }
    public void setLengthOfStayPriorToEntry(HmisLengthOfStay lengthOfStayPriorToEntry) { 
        this.lengthOfStayPriorToEntry = lengthOfStayPriorToEntry; 
    }
    
    public Boolean getEntryFromStreetOutreach() { return entryFromStreetOutreach; }
    public void setEntryFromStreetOutreach(Boolean entryFromStreetOutreach) { 
        this.entryFromStreetOutreach = entryFromStreetOutreach; 
    }
    
    public EnrollmentStatus getStatus() { return status; }
    public void setStatus(EnrollmentStatus status) { this.status = status; }
    
    public Instant getEnrollmentPeriodStart() { return enrollmentPeriodStart; }
    public void setEnrollmentPeriodStart(Instant enrollmentPeriodStart) { 
        this.enrollmentPeriodStart = enrollmentPeriodStart; 
    }
    
    public Instant getEnrollmentPeriodEnd() { return enrollmentPeriodEnd; }
    public void setEnrollmentPeriodEnd(Instant enrollmentPeriodEnd) { 
        this.enrollmentPeriodEnd = enrollmentPeriodEnd; 
    }
    
    public List<JpaServiceEpisodeEntity> getServiceEpisodes() { return serviceEpisodes; }
    public void setServiceEpisodes(List<JpaServiceEpisodeEntity> serviceEpisodes) { 
        this.serviceEpisodes = serviceEpisodes; 
    }
    
    public JpaProjectExitEntity getProjectExit() { return projectExit; }
    public void setProjectExit(JpaProjectExitEntity projectExit) { this.projectExit = projectExit; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    
    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }
    
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
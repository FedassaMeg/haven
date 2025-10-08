package org.haven.programenrollment.infrastructure.persistence;

import jakarta.persistence.*;
import org.haven.clientprofile.domain.ClientId;
import org.haven.programenrollment.domain.*;
import org.haven.shared.vo.CodeableConcept;
import org.haven.shared.vo.Period;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
    @Column(name = "relationship_to_head", columnDefinition = "hmis_relationship_to_head")
    private HmisRelationshipToHead relationshipToHead;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "residence_prior_to_entry", columnDefinition = "hmis_residence_prior")
    private HmisResidencePrior residencePriorToEntry;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "length_of_stay_prior_to_entry", columnDefinition = "hmis_length_of_stay")
    private HmisLengthOfStay lengthOfStayPriorToEntry;
    
    @Column(name = "entry_from_street_outreach")
    private Boolean entryFromStreetOutreach;
    
    @Column(name = "predecessor_enrollment_id")
    private UUID predecessorEnrollmentId;
    
    @Column(name = "residential_move_in_date")
    private LocalDate residentialMoveInDate;
    
    @Column(name = "household_id")
    private String householdId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "enrollment_status")
    private ProgramEnrollment.EnrollmentStatus status;
    
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
        this.status = ProgramEnrollment.EnrollmentStatus.ACTIVE;
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
    
    public UUID getPredecessorEnrollmentId() { return predecessorEnrollmentId; }
    public void setPredecessorEnrollmentId(UUID predecessorEnrollmentId) {
        this.predecessorEnrollmentId = predecessorEnrollmentId;
    }
    
    public LocalDate getResidentialMoveInDate() { return residentialMoveInDate; }
    public void setResidentialMoveInDate(LocalDate residentialMoveInDate) {
        this.residentialMoveInDate = residentialMoveInDate;
    }
    
    public String getHouseholdId() { return householdId; }
    public void setHouseholdId(String householdId) {
        this.householdId = householdId;
    }
    
    public ProgramEnrollment.EnrollmentStatus getStatus() { return status; }
    public void setStatus(ProgramEnrollment.EnrollmentStatus status) { this.status = status; }
    
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
    
    // Constructor from domain object
    public JpaProgramEnrollmentEntity(ProgramEnrollment enrollment) {
        this.id = enrollment.getId().value();
        this.clientId = enrollment.getClientId().value();
        this.programId = enrollment.getProgramId();
        this.enrollmentDate = enrollment.getEnrollmentDate();
        this.status = enrollment.getStatus();
        this.householdId = enrollment.getHouseholdId();
        this.residentialMoveInDate = enrollment.getResidentialMoveInDate();
        this.predecessorEnrollmentId = enrollment.getPredecessorEnrollmentId();
        
        if (enrollment.getEnrollmentPeriod() != null) {
            this.enrollmentPeriodStart = enrollment.getEnrollmentPeriod().getStart();
            this.enrollmentPeriodEnd = enrollment.getEnrollmentPeriod().getEnd();
        }
        
        this.createdAt = enrollment.getCreatedAt();
        this.updatedAt = Instant.now();
        
        // Handle service episodes
        if (enrollment.getServiceEpisodes() != null) {
            this.serviceEpisodes = enrollment.getServiceEpisodes().stream()
                .map(JpaServiceEpisodeEntity::fromDomainObject)
                .collect(Collectors.toList());
        }
        
        // Handle project exit
        if (enrollment.getProjectExit() != null) {
            this.projectExit = JpaProjectExitEntity.fromDomainObject(enrollment.getProjectExit());
        }
    }
    
    // Convert to domain object
    public ProgramEnrollment toDomainObject() {
        ProgramEnrollment enrollment = ProgramEnrollment.reconstitute(
            ProgramEnrollmentId.of(this.id),
            ClientId.of(this.clientId),
            this.programId,
            this.enrollmentDate,
            this.status,
            this.createdAt
        );
        
        enrollment.setHouseholdId(this.householdId);
        enrollment.setResidentialMoveInDate(this.residentialMoveInDate);
        enrollment.setPredecessorEnrollmentId(this.predecessorEnrollmentId);
        
        if (this.enrollmentPeriodStart != null || this.enrollmentPeriodEnd != null) {
            enrollment.setEnrollmentPeriod(new Period(this.enrollmentPeriodStart, this.enrollmentPeriodEnd));
        }
        
        // Convert service episodes
        if (this.serviceEpisodes != null && !this.serviceEpisodes.isEmpty()) {
            List<ServiceEpisode> episodes = this.serviceEpisodes.stream()
                .map(JpaServiceEpisodeEntity::toDomainObject)
                .collect(Collectors.toList());
            enrollment.setServiceEpisodes(episodes);
        }
        
        // Convert project exit
        if (this.projectExit != null) {
            enrollment.setProjectExit(this.projectExit.toDomainObject());
        }
        
        return enrollment;
    }
}
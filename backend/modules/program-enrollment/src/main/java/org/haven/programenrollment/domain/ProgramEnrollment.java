package org.haven.programenrollment.domain;

import org.haven.clientprofile.domain.ClientId;
import org.haven.shared.domain.AggregateRoot;
import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import org.haven.shared.vo.Period;
import org.haven.shared.vo.hmis.*;
import org.haven.programenrollment.domain.events.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * HMIS-aligned Program Enrollment aggregate root
 * Manages the complete enrollment lifecycle: enrollment → services → exit
 */
public class ProgramEnrollment extends AggregateRoot<ProgramEnrollmentId> {
    
    private ClientId clientId;
    private UUID programId;
    private LocalDate enrollmentDate;
    private Period enrollmentPeriod;
    private EnrollmentStatus status;
    
    // HMIS Universal Data Elements for enrollment
    private CodeableConcept relationshipToHead;
    private CodeableConcept residencePriorToEntry;
    private CodeableConcept lengthOfStay;
    private String entryFrom;
    
    // HMIS 2024 Comparable Database fields
    private String householdId;
    private RelationshipToHeadOfHousehold hmisRelationshipToHoH;
    private PriorLivingSituation hmisPriorLivingSituation;
    private LengthOfStay hmisLengthOfStay;
    private DisablingCondition hmisDisablingCondition;
    
    // Service episodes during enrollment
    private List<ServiceEpisode> serviceEpisodes = new ArrayList<>();
    
    // Exit information (nullable until exit occurs)
    private ProjectExit projectExit;
    
    private Instant createdAt;
    
    public static ProgramEnrollment create(ClientId clientId, UUID programId, 
                                         LocalDate enrollmentDate, 
                                         CodeableConcept relationshipToHead,
                                         CodeableConcept residencePriorToEntry,
                                         String entryFrom) {
        ProgramEnrollmentId enrollmentId = ProgramEnrollmentId.generate();
        ProgramEnrollment enrollment = new ProgramEnrollment();
        enrollment.apply(new ProgramEnrollmentCreated(
            enrollmentId.value(),
            clientId.value(),
            programId,
            enrollmentDate,
            relationshipToHead,
            residencePriorToEntry,
            null, // lengthOfStay calculated separately
            entryFrom,
            Instant.now()
        ));
        return enrollment;
    }
    
    public void addServiceEpisode(CodeableConcept serviceType, LocalDate serviceDate, 
                                String providedBy, String description) {
        if (status == EnrollmentStatus.EXITED) {
            throw new IllegalStateException("Cannot add services to exited enrollment");
        }
        
        ServiceEpisodeId episodeId = ServiceEpisodeId.generate();
        apply(new ServiceEpisodeAdded(
            id.value(),
            episodeId.value(),
            serviceType,
            serviceDate,
            providedBy,
            description,
            Instant.now()
        ));
    }
    
    /**
     * Link an external ServiceEpisode to this enrollment
     * Used when ServiceEpisode aggregate is created separately
     */
    public void linkServiceEpisode(ServiceEpisodeId episodeId,
                                 org.haven.shared.vo.services.ServiceType serviceType,
                                 LocalDate serviceDate,
                                 String providedBy) {
        if (status == EnrollmentStatus.EXITED) {
            throw new IllegalStateException("Cannot link services to exited enrollment");
        }
        
        apply(new ServiceEpisodeLinked(
            id.value(),
            episodeId.value(),
            serviceType.name(),
            serviceDate,
            providedBy,
            Instant.now()
        ));
    }
    
    /**
     * Get total service hours for this enrollment
     */
    public Double getTotalServiceHours() {
        // This would typically be calculated from linked ServiceEpisodes
        // For now, return estimated based on episode count
        return serviceEpisodes.size() * 0.75; // Assuming 45 minutes average
    }
    
    /**
     * Get service intensity (services per week)
     */
    public Double getServiceIntensity() {
        if (enrollmentPeriod == null || serviceEpisodes.isEmpty()) {
            return 0.0;
        }
        
        long enrollmentDays = java.time.Duration.between(
            enrollmentPeriod.start(), 
            enrollmentPeriod.end() != null ? enrollmentPeriod.end() : Instant.now()
        ).toDays();
        
        if (enrollmentDays == 0) return 0.0;
        
        double enrollmentWeeks = enrollmentDays / 7.0;
        return serviceEpisodes.size() / enrollmentWeeks;
    }
    
    /**
     * Check if enrollment meets minimum service requirements
     */
    public boolean meetsMinimumServiceRequirements() {
        // This would vary by program type and funder requirements
        // Example: at least 1 service per month for active enrollment
        if (serviceEpisodes.isEmpty()) return false;
        
        // Check if there's been a service in the last 30 days
        Instant thirtyDaysAgo = Instant.now().minusSeconds(30 * 24 * 60 * 60);
        return serviceEpisodes.stream()
            .anyMatch(episode -> episode.getRecordedAt().isAfter(thirtyDaysAgo));
    }
    
    public void exitProgram(LocalDate exitDate, CodeableConcept exitReason, 
                          CodeableConcept destination, String recordedBy) {
        if (status == EnrollmentStatus.EXITED) {
            throw new IllegalStateException("Enrollment is already exited");
        }
        
        apply(new ProgramExited(
            id.value(),
            exitDate,
            exitReason,
            destination,
            null, // housing outcome determined separately
            recordedBy,
            Instant.now()
        ));
    }
    
    @Override
    protected void when(DomainEvent event) {
        if (event instanceof ProgramEnrollmentCreated e) {
            this.id = ProgramEnrollmentId.of(e.enrollmentId());
            this.clientId = new ClientId(e.clientId());
            this.programId = e.programId();
            this.enrollmentDate = e.enrollmentDate();
            this.relationshipToHead = e.relationshipToHead();
            this.residencePriorToEntry = e.residencePriorToEntry();
            this.lengthOfStay = e.lengthOfStay();
            this.entryFrom = e.entryFrom();
            this.status = EnrollmentStatus.ACTIVE;
            this.enrollmentPeriod = new Period(e.occurredAt(), null);
            this.createdAt = e.occurredAt();
            
        } else if (event instanceof ServiceEpisodeAdded e) {
            ServiceEpisode episode = new ServiceEpisode(
                ServiceEpisodeId.of(e.serviceEpisodeId()),
                e.serviceType(),
                e.serviceDate(),
                e.providedBy()
            );
            this.serviceEpisodes.add(episode);
            
        } else if (event instanceof ProgramExited e) {
            this.projectExit = new ProjectExit(
                e.exitDate(),
                e.exitReason(),
                e.destination(),
                e.recordedBy()
            );
            this.status = EnrollmentStatus.EXITED;
            if (this.enrollmentPeriod != null) {
                this.enrollmentPeriod = new Period(
                    this.enrollmentPeriod.start(), 
                    e.occurredAt()
                );
            }
            
        } else {
            throw new IllegalArgumentException("Unhandled event: " + event.getClass());
        }
    }
    
    public enum EnrollmentStatus {
        PENDING,
        ACTIVE,
        SUSPENDED,
        EXITED,
        CANCELLED
    }
    
    // Getters
    public ClientId getClientId() { return clientId; }
    public UUID getProgramId() { return programId; }
    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public Period getEnrollmentPeriod() { return enrollmentPeriod; }
    public EnrollmentStatus getStatus() { return status; }
    public CodeableConcept getRelationshipToHead() { return relationshipToHead; }
    public CodeableConcept getResidencePriorToEntry() { return residencePriorToEntry; }
    public CodeableConcept getLengthOfStay() { return lengthOfStay; }
    public String getEntryFrom() { return entryFrom; }
    public List<ServiceEpisode> getServiceEpisodes() { return List.copyOf(serviceEpisodes); }
    public ProjectExit getProjectExit() { return projectExit; }
    public Instant getCreatedAt() { return createdAt; }
    
    public boolean isActive() {
        return status == EnrollmentStatus.ACTIVE;
    }
    
    public boolean hasExited() {
        return status == EnrollmentStatus.EXITED && projectExit != null;
    }
    
    public int getServiceEpisodeCount() {
        return serviceEpisodes.size();
    }
    
    // HMIS Comparable Database methods
    public void updateHouseholdId(String householdId) {
        this.householdId = householdId;
    }
    
    public void updateHmisRelationshipToHoH(RelationshipToHeadOfHousehold relationship) {
        this.hmisRelationshipToHoH = relationship;
    }
    
    public void updateHmisPriorLivingSituation(PriorLivingSituation priorLiving) {
        this.hmisPriorLivingSituation = priorLiving;
    }
    
    public void updateHmisLengthOfStay(LengthOfStay lengthOfStay) {
        this.hmisLengthOfStay = lengthOfStay;
    }
    
    public void updateHmisDisablingCondition(DisablingCondition disablingCondition) {
        this.hmisDisablingCondition = disablingCondition;
    }
    
    // HMIS getters
    public String getHouseholdId() { 
        return householdId != null ? householdId : id.value().toString(); 
    }
    public RelationshipToHeadOfHousehold getHmisRelationshipToHoH() { 
        return hmisRelationshipToHoH != null ? hmisRelationshipToHoH : RelationshipToHeadOfHousehold.DATA_NOT_COLLECTED; 
    }
    public PriorLivingSituation getHmisPriorLivingSituation() { 
        return hmisPriorLivingSituation != null ? hmisPriorLivingSituation : PriorLivingSituation.DATA_NOT_COLLECTED; 
    }
    public LengthOfStay getHmisLengthOfStay() { 
        return hmisLengthOfStay != null ? hmisLengthOfStay : LengthOfStay.DATA_NOT_COLLECTED; 
    }
    public DisablingCondition getHmisDisablingCondition() { 
        return hmisDisablingCondition != null ? hmisDisablingCondition : DisablingCondition.DATA_NOT_COLLECTED; 
    }
    
    /**
     * Check if this enrollment is likely chronic homelessness
     * Based on HMIS chronic homelessness criteria
     */
    public boolean isPotentiallyChronicallyHomeless() {
        return hmisDisablingCondition == DisablingCondition.YES &&
               hmisPriorLivingSituation != null && hmisPriorLivingSituation.isLiterallyHomeless() &&
               hmisLengthOfStay != null && hmisLengthOfStay.isLongTerm();
    }
    
    /**
     * Determine if this is a family enrollment
     */
    public boolean isFamilyEnrollment() {
        return hmisRelationshipToHoH != null && 
               (hmisRelationshipToHoH.isHeadOfHousehold() || hmisRelationshipToHoH.isFamilyMember());
    }
    
    /**
     * Check if enrollment meets HMIS data quality standards
     */
    public boolean meetsHmisDataQuality() {
        return householdId != null &&
               hmisRelationshipToHoH != null && hmisRelationshipToHoH.isKnownRelationship() &&
               hmisPriorLivingSituation != null &&
               hmisLengthOfStay != null && hmisLengthOfStay.isKnownLength() &&
               hmisDisablingCondition != null && hmisDisablingCondition.isKnownStatus();
    }
}
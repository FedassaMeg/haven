package org.haven.casemgmt.domain;

import org.haven.clientprofile.domain.ClientId;
import org.haven.shared.domain.AggregateRoot;
import org.haven.shared.vo.*;
import org.haven.casemgmt.domain.events.*;
import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Program Enrollment aggregate for case management
 * Tracks client enrollment in specific programs with service delivery
 */
public class ProgramEnrollmentAggregate extends AggregateRoot<ProgramEnrollmentId> {
    
    private ClientId clientId;
    private CaseId caseId;
    private UUID programId;
    private String programName;
    private LocalDate enrollmentDate;
    private LocalDate expectedExitDate;
    private LocalDate actualExitDate;
    private EnrollmentStatus status;
    private CodeableConcept exitReason;
    private String exitDestination;
    private String enrolledBy;
    private String exitedBy;
    private String entryPoint;
    private String exitNotes;
    private List<UUID> serviceEpisodeIds = new ArrayList<>();
    private Instant createdAt;
    private Instant lastModified;
    
    public static ProgramEnrollmentAggregate open(ClientId clientId, CaseId caseId, 
                                                 UUID programId, String programName,
                                                 LocalDate enrollmentDate, String enrolledBy,
                                                 String entryPoint) {
        ProgramEnrollmentId enrollmentId = ProgramEnrollmentId.generate();
        ProgramEnrollmentAggregate enrollment = new ProgramEnrollmentAggregate();
        enrollment.apply(new EnrollmentOpened(
            enrollmentId.value(),
            clientId.value(),
            caseId.value(),
            programId,
            programName,
            enrollmentDate,
            enrolledBy,
            entryPoint,
            Instant.now()
        ));
        return enrollment;
    }
    
    public void updateEnrollment(LocalDate expectedExitDate, String notes, String updatedBy) {
        if (status == EnrollmentStatus.EXITED) {
            throw new IllegalStateException("Cannot update exited enrollment");
        }
        
        apply(new EnrollmentUpdated(
            id.value(),
            clientId.value(),
            expectedExitDate,
            notes,
            updatedBy,
            Instant.now()
        ));
    }
    
    public void exit(LocalDate exitDate, CodeableConcept exitReason, String exitDestination,
                    String exitNotes, String exitedBy) {
        if (status == EnrollmentStatus.EXITED) {
            throw new IllegalStateException("Enrollment is already exited");
        }
        
        apply(new EnrollmentExited(
            id.value(),
            clientId.value(),
            exitDate,
            exitReason,
            exitDestination,
            exitNotes,
            exitedBy,
            Instant.now()
        ));
    }
    
    public void linkServiceEpisode(UUID serviceEpisodeId) {
        if (!serviceEpisodeIds.contains(serviceEpisodeId)) {
            serviceEpisodeIds.add(serviceEpisodeId);
        }
    }
    
    @Override
    protected void when(DomainEvent event) {
        if (event instanceof EnrollmentOpened e) {
            this.id = new ProgramEnrollmentId(e.enrollmentId());
            this.clientId = new ClientId(e.clientId());
            this.caseId = new CaseId(e.caseId());
            this.programId = e.programId();
            this.programName = e.programName();
            this.enrollmentDate = e.enrollmentDate();
            this.enrolledBy = e.enrolledBy();
            this.entryPoint = e.entryPoint();
            this.status = EnrollmentStatus.ACTIVE;
            this.createdAt = e.occurredAt();
            this.lastModified = e.occurredAt();
        } else if (event instanceof EnrollmentUpdated e) {
            this.expectedExitDate = e.expectedExitDate();
            this.lastModified = e.occurredAt();
        } else if (event instanceof EnrollmentExited e) {
            this.actualExitDate = e.exitDate();
            this.exitReason = e.exitReason();
            this.exitDestination = e.exitDestination();
            this.exitNotes = e.exitNotes();
            this.exitedBy = e.exitedBy();
            this.status = EnrollmentStatus.EXITED;
            this.lastModified = e.occurredAt();
        } else {
            throw new IllegalArgumentException("Unhandled event: " + event.getClass());
        }
    }
    
    public enum EnrollmentStatus {
        ACTIVE, ON_HOLD, EXITED, TRANSFERRED
    }
    
    // Getters
    public ClientId getClientId() { return clientId; }
    public CaseId getCaseId() { return caseId; }
    public UUID getProgramId() { return programId; }
    public String getProgramName() { return programName; }
    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public LocalDate getExpectedExitDate() { return expectedExitDate; }
    public LocalDate getActualExitDate() { return actualExitDate; }
    public EnrollmentStatus getStatus() { return status; }
    public CodeableConcept getExitReason() { return exitReason; }
    public String getExitDestination() { return exitDestination; }
    public String getEnrolledBy() { return enrolledBy; }
    public String getExitedBy() { return exitedBy; }
    public String getEntryPoint() { return entryPoint; }
    public String getExitNotes() { return exitNotes; }
    public List<UUID> getServiceEpisodeIds() { return List.copyOf(serviceEpisodeIds); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastModified() { return lastModified; }
    
    public boolean isActive() {
        return status == EnrollmentStatus.ACTIVE;
    }
    
    public boolean hasExited() {
        return status == EnrollmentStatus.EXITED;
    }
}
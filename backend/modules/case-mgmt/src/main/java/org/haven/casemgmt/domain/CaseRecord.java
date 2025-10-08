package org.haven.casemgmt.domain;

import org.haven.clientprofile.domain.ClientId;
import org.haven.shared.domain.AggregateRoot;
import org.haven.shared.vo.*;
import org.haven.casemgmt.domain.events.*;
import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Refactored case management aggregate
 * Now serves as coordination layer that references program enrollments
 * Aligns with HMIS model where case work = program enrollment + service episodes
 */
public class CaseRecord extends AggregateRoot<CaseId> {
    
    private ClientId clientId;
    private CodeableConcept caseType;
    private CodeableConcept priority;
    private CaseStatus status;
    private Period period;
    private String description;
    private List<CaseNote> notes = new ArrayList<>();
    private List<CaseParticipant> participants = new ArrayList<>();
    private List<CaseAssignment> assignmentHistory = new ArrayList<>();
    private List<ProgramEnrollmentId> linkedEnrollments = new ArrayList<>();
    private List<ServiceEpisodeId> linkedServiceEpisodes = new ArrayList<>();
    private List<SafetyPlanId> linkedSafetyPlans = new ArrayList<>();
    private List<LegalAdvocacyId> linkedLegalAdvocacies = new ArrayList<>();
    private List<FinancialAssistanceRequestId> linkedFinancialRequests = new ArrayList<>();
    private Instant createdAt;
    
    public static CaseRecord open(ClientId clientId, CodeableConcept caseType,
                                 CodeableConcept priority, String description) {
        CaseId caseId = CaseId.generate();
        CaseRecord caseRecord = new CaseRecord();
        caseRecord.apply(new CaseOpened(caseId.value(), clientId.value(),
                                       caseType, priority, description, Instant.now()));
        return caseRecord;
    }

    /**
     * Reconstruct aggregate from event history without creating new events
     * Used when loading from repository
     */
    public static CaseRecord reconstruct(UUID caseId, List<DomainEvent> events) {
        CaseRecord aggregate = new CaseRecord();
        aggregate.id = new CaseId(caseId);
        for (int i = 0; i < events.size(); i++) {
            aggregate.replay(events.get(i), i + 1);
        }
        return aggregate;
    }
    
    public void assignTo(String assigneeId, String assigneeName, CodeableConcept role, 
                      CaseAssignment.AssignmentType assignmentType, String reason, String assignedBy) {
        if (status == CaseStatus.CLOSED) {
            throw new IllegalStateException("Cannot assign closed case");
        }
        
        // End current primary assignment if assigning a new primary
        if (assignmentType == CaseAssignment.AssignmentType.PRIMARY) {
            getCurrentPrimaryAssignment().ifPresent(currentAssignment -> {
                endAssignment(currentAssignment.getAssignmentId(), 
                            "Replaced by new primary assignment", assignedBy);
            });
        }
        
        UUID assignmentId = UUID.randomUUID();
        boolean isPrimary = assignmentType == CaseAssignment.AssignmentType.PRIMARY;
        
        apply(new CaseAssigned(id.value(), assignmentId, assigneeId, assigneeName, role, 
                             assignmentType, reason, assignedBy, isPrimary, Instant.now()));
    }
    
    public void endAssignment(UUID assignmentId, String reason, String endedBy) {
        CaseAssignment assignment = findAssignmentById(assignmentId)
            .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + assignmentId));
        
        if (!assignment.isActive()) {
            throw new IllegalStateException("Assignment is already ended");
        }
        
        apply(new CaseAssignmentEnded(id.value(), assignmentId, assignment.getAssigneeId(), 
                                    reason, endedBy, Instant.now(), Instant.now()));
    }
    
    public void addNote(String content, String authorId) {
        apply(new CaseNoteAdded(id.value(), UUID.randomUUID(), content, authorId, Instant.now()));
    }
    
    public void updateStatus(CaseStatus newStatus) {
        if (this.status != newStatus) {
            apply(new CaseStatusChanged(id.value(), this.status, newStatus, Instant.now()));
        }
    }
    
    public void linkProgramEnrollment(ProgramEnrollmentId enrollmentId, String linkedBy, String reason) {
        if (linkedEnrollments.contains(enrollmentId)) {
            throw new IllegalStateException("Enrollment is already linked to this case");
        }
        apply(new ProgramEnrollmentLinked(id.value(), enrollmentId.value(), linkedBy, reason, Instant.now()));
    }
    
    public void linkServiceEpisode(ServiceEpisodeId episodeId, String linkedBy, String reason) {
        if (linkedServiceEpisodes.contains(episodeId)) {
            throw new IllegalStateException("Service episode is already linked to this case");
        }
        apply(new ServiceEpisodeLinked(id.value(), episodeId.value(), linkedBy, reason, Instant.now()));
    }
    
    public void linkSafetyPlan(SafetyPlanId safetyPlanId, String linkedBy, String reason) {
        if (linkedSafetyPlans.contains(safetyPlanId)) {
            throw new IllegalStateException("Safety plan is already linked to this case");
        }
        apply(new SafetyPlanLinked(id.value(), safetyPlanId.value(), linkedBy, reason, Instant.now()));
    }
    
    public void linkLegalAdvocacy(LegalAdvocacyId legalAdvocacyId, String linkedBy, String reason) {
        if (linkedLegalAdvocacies.contains(legalAdvocacyId)) {
            throw new IllegalStateException("Legal advocacy is already linked to this case");
        }
        apply(new LegalAdvocacyLinked(id.value(), legalAdvocacyId.value(), linkedBy, reason, Instant.now()));
    }
    
    public void linkFinancialRequest(FinancialAssistanceRequestId requestId, String linkedBy, String reason) {
        if (linkedFinancialRequests.contains(requestId)) {
            throw new IllegalStateException("Financial assistance request is already linked to this case");
        }
        apply(new FinancialRequestLinked(id.value(), requestId.value(), linkedBy, reason, Instant.now()));
    }
    
    public void close(String reason) {
        if (status == CaseStatus.CLOSED) {
            throw new IllegalStateException("Case is already closed");
        }
        apply(new CaseClosed(id.value(), reason, Instant.now()));
    }
    
    @Override
    protected void when(DomainEvent event) {
        if (event instanceof CaseOpened e) {
            this.id = new CaseId(e.caseId());
            this.clientId = new ClientId(e.clientId());
            this.caseType = e.caseType();
            this.priority = e.priority();
            this.description = e.description();
            this.status = CaseStatus.OPEN;
            this.period = new Period(e.occurredAt(), null);
            this.createdAt = e.occurredAt();
        } else if (event instanceof CaseAssigned e) {
            CaseAssignment newAssignment = new CaseAssignment(
                e.assignmentId(), e.assigneeId(), e.assigneeName(), e.role(),
                new Period(e.occurredAt(), null), e.assignmentType(), 
                e.reason(), e.assignedBy(), e.isPrimary()
            );
            this.assignmentHistory.add(newAssignment);
        } else if (event instanceof CaseAssignmentEnded e) {
            // Find and replace the assignment with ended version
            for (int i = 0; i < assignmentHistory.size(); i++) {
                CaseAssignment assignment = assignmentHistory.get(i);
                if (assignment.getAssignmentId().equals(e.assignmentId())) {
                    CaseAssignment endedAssignment = assignment.endAssignment(e.endedAt(), e.endReason());
                    assignmentHistory.set(i, endedAssignment);
                    break;
                }
            }
        } else if (event instanceof CaseNoteAdded e) {
            this.notes.add(new CaseNote(e.noteId(), e.content(), e.authorId(), e.occurredAt()));
        } else if (event instanceof CaseStatusChanged e) {
            this.status = e.newStatus();
        } else if (event instanceof ProgramEnrollmentLinked e) {
            this.linkedEnrollments.add(new ProgramEnrollmentId(e.enrollmentId()));
        } else if (event instanceof ServiceEpisodeLinked e) {
            this.linkedServiceEpisodes.add(new ServiceEpisodeId(e.episodeId()));
        } else if (event instanceof SafetyPlanLinked e) {
            this.linkedSafetyPlans.add(new SafetyPlanId(e.safetyPlanId()));
        } else if (event instanceof LegalAdvocacyLinked e) {
            this.linkedLegalAdvocacies.add(new LegalAdvocacyId(e.legalAdvocacyId()));
        } else if (event instanceof FinancialRequestLinked e) {
            this.linkedFinancialRequests.add(new FinancialAssistanceRequestId(e.requestId()));
        } else if (event instanceof CaseClosed e) {
            this.status = CaseStatus.CLOSED;
            if (this.period != null) {
                this.period = new Period(this.period.start(), e.occurredAt());
            }
        } else {
            throw new IllegalArgumentException("Unhandled event: " + event.getClass());
        }
    }
    
    public enum CaseStatus {
        OPEN, IN_PROGRESS, ON_HOLD, CLOSED, CANCELLED
    }
    
    // Getters
    public ClientId getClientId() { return clientId; }
    public CodeableConcept getCaseType() { return caseType; }
    public CodeableConcept getPriority() { return priority; }
    public CaseStatus getStatus() { return status; }
    public Period getPeriod() { return period; }
    public String getDescription() { return description; }
    public List<CaseNote> getNotes() { return List.copyOf(notes); }
    public List<CaseParticipant> getParticipants() { return List.copyOf(participants); }
    public List<CaseAssignment> getAssignmentHistory() { return List.copyOf(assignmentHistory); }
    public List<ProgramEnrollmentId> getLinkedEnrollments() { return List.copyOf(linkedEnrollments); }
    public List<ServiceEpisodeId> getLinkedServiceEpisodes() { return List.copyOf(linkedServiceEpisodes); }
    public List<SafetyPlanId> getLinkedSafetyPlans() { return List.copyOf(linkedSafetyPlans); }
    public List<LegalAdvocacyId> getLinkedLegalAdvocacies() { return List.copyOf(linkedLegalAdvocacies); }
    public List<FinancialAssistanceRequestId> getLinkedFinancialRequests() { return List.copyOf(linkedFinancialRequests); }
    public Instant getCreatedAt() { return createdAt; }
    
    public boolean isActive() {
        return status != CaseStatus.CLOSED && status != CaseStatus.CANCELLED;
    }
    
    public boolean hasLinkedEnrollments() {
        return !linkedEnrollments.isEmpty();
    }
    
    public int getLinkedEnrollmentCount() {
        return linkedEnrollments.size();
    }
    
    // Assignment helper methods
    public Optional<CaseAssignment> getCurrentPrimaryAssignment() {
        return assignmentHistory.stream()
            .filter(a -> a.isActive() && a.isPrimary())
            .findFirst();
    }
    
    public List<CaseAssignment> getActiveAssignments() {
        return assignmentHistory.stream()
            .filter(CaseAssignment::isActive)
            .toList();
    }
    
    public List<CaseAssignment> getActiveAssignments(CaseAssignment.AssignmentType type) {
        return assignmentHistory.stream()
            .filter(a -> a.isActive() && a.getAssignmentType() == type)
            .toList();
    }
    
    public Optional<CaseAssignment> findAssignmentById(UUID assignmentId) {
        return assignmentHistory.stream()
            .filter(a -> a.getAssignmentId().equals(assignmentId))
            .findFirst();
    }
    
    public boolean hasActiveAssignments() {
        return assignmentHistory.stream().anyMatch(CaseAssignment::isActive);
    }
    
    public boolean hasActivePrimaryAssignment() {
        return getCurrentPrimaryAssignment().isPresent();
    }
    
    public List<CaseAssignment> getAssignmentsForWorker(String workerId) {
        return assignmentHistory.stream()
            .filter(a -> a.getAssigneeId().equals(workerId))
            .toList();
    }
    
    public List<CaseAssignment> getAssignmentHistory(String workerId) {
        return getAssignmentsForWorker(workerId);
    }
    
    public Optional<CaseAssignment> getAssignmentOn(Instant dateTime) {
        return assignmentHistory.stream()
            .filter(a -> a.isActiveOn(dateTime) && a.isPrimary())
            .findFirst();
    }
}

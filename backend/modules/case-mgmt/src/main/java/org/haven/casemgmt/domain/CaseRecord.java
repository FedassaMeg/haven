package org.haven.casemgmt.domain;

import org.haven.clientprofile.domain.ClientId;
import org.haven.shared.domain.AggregateRoot;
import org.haven.shared.vo.*;
import org.haven.casemgmt.domain.events.*;
import org.haven.shared.events.DomainEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FHIR-inspired case management aggregate
 * Based on FHIR ServiceRequest and EpisodeOfCare resources
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
    private CaseAssignment assignment;
    private Instant createdAt;
    
    public static CaseRecord open(ClientId clientId, CodeableConcept caseType, 
                                 CodeableConcept priority, String description) {
        CaseId caseId = CaseId.generate();
        CaseRecord caseRecord = new CaseRecord();
        caseRecord.apply(new CaseOpened(caseId.value(), clientId.value(), 
                                       caseType, priority, description, Instant.now()));
        return caseRecord;
    }
    
    public void assignTo(String assigneeId, CodeableConcept role) {
        if (status == CaseStatus.CLOSED) {
            throw new IllegalStateException("Cannot assign closed case");
        }
        apply(new CaseAssigned(id.value(), assigneeId, role, Instant.now()));
    }
    
    public void addNote(String content, String authorId) {
        apply(new CaseNoteAdded(id.value(), UUID.randomUUID(), content, authorId, Instant.now()));
    }
    
    public void updateStatus(CaseStatus newStatus) {
        if (this.status != newStatus) {
            apply(new CaseStatusChanged(id.value(), this.status, newStatus, Instant.now()));
        }
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
            this.assignment = new CaseAssignment(e.assigneeId(), e.role(), e.occurredAt());
        } else if (event instanceof CaseNoteAdded e) {
            this.notes.add(new CaseNote(e.noteId(), e.content(), e.authorId(), e.occurredAt()));
        } else if (event instanceof CaseStatusChanged e) {
            this.status = e.newStatus();
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
    public CaseAssignment getAssignment() { return assignment; }
    public Instant getCreatedAt() { return createdAt; }
    
    public boolean isActive() {
        return status != CaseStatus.CLOSED && status != CaseStatus.CANCELLED;
    }
}
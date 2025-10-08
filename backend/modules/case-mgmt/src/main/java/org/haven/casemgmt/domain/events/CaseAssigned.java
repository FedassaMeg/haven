package org.haven.casemgmt.domain.events;

import org.haven.casemgmt.domain.CaseAssignment.AssignmentType;
import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import java.time.Instant;
import java.util.UUID;

public class CaseAssigned extends DomainEvent {
    private final UUID assignmentId;
    private final String assigneeId;
    private final String assigneeName;
    private final CodeableConcept role;
    private final AssignmentType assignmentType;
    private final String reason;
    private final String assignedBy;
    private final boolean isPrimary;

    public CaseAssigned(UUID caseId, UUID assignmentId, String assigneeId, String assigneeName, CodeableConcept role, AssignmentType assignmentType, String reason, String assignedBy, boolean isPrimary, Instant occurredAt) {
        super(caseId, occurredAt);
        this.assignmentId = assignmentId;
        this.assigneeId = assigneeId;
        this.assigneeName = assigneeName;
        this.role = role;
        this.assignmentType = assignmentType;
        this.reason = reason;
        this.assignedBy = assignedBy;
        this.isPrimary = isPrimary;
    }

    public UUID caseId() {
        return getAggregateId();
    }

    public UUID getCaseId() {
        return getAggregateId();
    }

    public UUID assignmentId() {
        return assignmentId;
    }

    public String assigneeId() {
        return assigneeId;
    }

    public String assigneeName() {
        return assigneeName;
    }

    public CodeableConcept role() {
        return role;
    }

    public AssignmentType assignmentType() {
        return assignmentType;
    }

    public String reason() {
        return reason;
    }

    public String assignedBy() {
        return assignedBy;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    // JavaBean-style getters
    public UUID getAssignmentId() { return assignmentId; }
    public String getAssigneeId() { return assigneeId; }
    public String getAssigneeName() { return assigneeName; }
    public CodeableConcept getRole() { return role; }
    public AssignmentType getAssignmentType() { return assignmentType; }
    public String getReason() { return reason; }
    public String getAssignedBy() { return assignedBy; }
    public boolean getIsPrimary() { return isPrimary; }

}
package org.haven.casemgmt.domain;

import org.haven.shared.vo.CodeableConcept;
import org.haven.shared.vo.Period;
import java.time.Instant;
import java.util.UUID;

public class CaseAssignment {
    private final UUID assignmentId;
    private final String assigneeId;
    private final String assigneeName;
    private final CodeableConcept role;
    private final Period assignmentPeriod;
    private final AssignmentType assignmentType;
    private final String reason;
    private final String assignedBy;
    private final boolean isPrimary;
    
    public enum AssignmentType {
        PRIMARY("Primary case manager"),
        BACKUP("Backup/coverage assignment"),
        TEMPORARY("Temporary assignment"),
        COLLABORATIVE("Collaborative assignment"),
        SUPERVISORY("Supervisory oversight");
        
        private final String description;
        
        AssignmentType(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    public CaseAssignment(String assigneeId, String assigneeName, CodeableConcept role, 
                         AssignmentType assignmentType, String reason, String assignedBy, 
                         boolean isPrimary) {
        this.assignmentId = UUID.randomUUID();
        this.assigneeId = assigneeId;
        this.assigneeName = assigneeName;
        this.role = role;
        this.assignmentPeriod = new Period(Instant.now(), null);
        this.assignmentType = assignmentType;
        this.reason = reason;
        this.assignedBy = assignedBy;
        this.isPrimary = isPrimary;
    }
    
    // Constructor for completed assignments
    public CaseAssignment(UUID assignmentId, String assigneeId, String assigneeName, 
                         CodeableConcept role, Period assignmentPeriod, AssignmentType assignmentType, 
                         String reason, String assignedBy, boolean isPrimary) {
        this.assignmentId = assignmentId;
        this.assigneeId = assigneeId;
        this.assigneeName = assigneeName;
        this.role = role;
        this.assignmentPeriod = assignmentPeriod;
        this.assignmentType = assignmentType;
        this.reason = reason;
        this.assignedBy = assignedBy;
        this.isPrimary = isPrimary;
    }
    
    public CaseAssignment endAssignment(Instant endTime, String reason) {
        if (!isActive()) {
            throw new IllegalStateException("Assignment is already ended");
        }
        return new CaseAssignment(
            this.assignmentId,
            this.assigneeId, 
            this.assigneeName,
            this.role,
            new Period(this.assignmentPeriod.start(), endTime),
            this.assignmentType,
            reason,
            this.assignedBy,
            this.isPrimary
        );
    }
    
    public boolean isActive() {
        return assignmentPeriod.end() == null;
    }
    
    public boolean isActiveOn(Instant dateTime) {
        return assignmentPeriod.contains(dateTime);
    }
    
    public boolean canCoverFor(CaseAssignment other) {
        return assignmentType == AssignmentType.BACKUP && 
               other.assignmentType == AssignmentType.PRIMARY;
    }
    
    // Getters
    public UUID getAssignmentId() { return assignmentId; }
    public String getAssigneeId() { return assigneeId; }
    public String getAssigneeName() { return assigneeName; }
    public CodeableConcept getRole() { return role; }
    public Period getAssignmentPeriod() { return assignmentPeriod; }
    public AssignmentType getAssignmentType() { return assignmentType; }
    public String getReason() { return reason; }
    public String getAssignedBy() { return assignedBy; }
    public boolean isPrimary() { return isPrimary; }
    public Instant getAssignedAt() { return assignmentPeriod.start(); }
    public Instant getEndedAt() { return assignmentPeriod.end(); }
}
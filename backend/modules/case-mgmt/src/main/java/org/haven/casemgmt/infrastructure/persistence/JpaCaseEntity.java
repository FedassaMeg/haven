package org.haven.casemgmt.infrastructure.persistence;

import org.haven.casemgmt.domain.CaseRecord;
import org.haven.casemgmt.domain.CaseId;
import org.haven.clientprofile.domain.ClientId;
import org.haven.shared.vo.CodeableConcept;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cases", schema = "haven")
public class JpaCaseEntity {
    
    @Id
    private UUID id;
    
    @Column(name = "client_id", nullable = false)
    private UUID clientId;
    
    @Column(name = "case_type_code", length = 50)
    private String caseTypeCode;
    
    @Column(name = "case_type_display", length = 255)
    private String caseTypeDisplay;
    
    @Column(name = "priority_code", length = 50)
    private String priorityCode;
    
    @Column(name = "priority_display", length = 255)
    private String priorityDisplay;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CaseRecord.CaseStatus status;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "opened_date", nullable = false)
    private Instant openedDate;
    
    @Column(name = "closed_date")
    private Instant closedDate;
    
    @Column(name = "assigned_to")
    private String assignedTo;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Version
    private Long version;
    
    // Constructors
    protected JpaCaseEntity() {
        // JPA requires default constructor
    }
    
    public JpaCaseEntity(UUID id, UUID clientId, String caseTypeCode, String caseTypeDisplay,
                        String priorityCode, String priorityDisplay, CaseRecord.CaseStatus status,
                        String description, Instant openedDate, String assignedTo, Instant createdAt) {
        this.id = id;
        this.clientId = clientId;
        this.caseTypeCode = caseTypeCode;
        this.caseTypeDisplay = caseTypeDisplay;
        this.priorityCode = priorityCode;
        this.priorityDisplay = priorityDisplay;
        this.status = status;
        this.description = description;
        this.openedDate = openedDate;
        this.assignedTo = assignedTo;
        this.createdAt = createdAt;
    }
    
    // Factory methods
    public static JpaCaseEntity fromDomain(CaseRecord caseRecord) {
        return new JpaCaseEntity(
            caseRecord.getId().value(),
            caseRecord.getClientId().value(),
            caseRecord.getCaseType() != null && !caseRecord.getCaseType().coding().isEmpty() ? caseRecord.getCaseType().coding().get(0).code() : null,
            caseRecord.getCaseType() != null && !caseRecord.getCaseType().coding().isEmpty() ? caseRecord.getCaseType().coding().get(0).display() : null,
            caseRecord.getPriority() != null && !caseRecord.getPriority().coding().isEmpty() ? caseRecord.getPriority().coding().get(0).code() : null,
            caseRecord.getPriority() != null && !caseRecord.getPriority().coding().isEmpty() ? caseRecord.getPriority().coding().get(0).display() : null,
            caseRecord.getStatus(),
            caseRecord.getDescription(),
            caseRecord.getPeriod() != null ? caseRecord.getPeriod().start() : null,
            caseRecord.getAssignment() != null ? caseRecord.getAssignment().assigneeId() : null,
            caseRecord.getCreatedAt()
        );
    }
    
    public CaseRecord toDomain() {
        // For now, return a simplified reconstruction
        // In a full implementation, you'd replay events from the event store
        CodeableConcept caseType = caseTypeCode != null ? 
            new CodeableConcept(List.of(new CodeableConcept.Coding(null, null, caseTypeCode, caseTypeDisplay, null)), caseTypeDisplay) :
            new CodeableConcept(List.of(), "Unknown");
        CodeableConcept priority = priorityCode != null ? 
            new CodeableConcept(List.of(new CodeableConcept.Coding(null, null, priorityCode, priorityDisplay, null)), priorityDisplay) :
            new CodeableConcept(List.of(), "Normal");
        
        CaseRecord caseRecord = CaseRecord.open(
            new ClientId(clientId),
            caseType,
            priority,
            description
        );
        
        return caseRecord;
    }
    
    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }
    
    public String getCaseTypeCode() { return caseTypeCode; }
    public void setCaseTypeCode(String caseTypeCode) { this.caseTypeCode = caseTypeCode; }
    
    public String getCaseTypeDisplay() { return caseTypeDisplay; }
    public void setCaseTypeDisplay(String caseTypeDisplay) { this.caseTypeDisplay = caseTypeDisplay; }
    
    public String getPriorityCode() { return priorityCode; }
    public void setPriorityCode(String priorityCode) { this.priorityCode = priorityCode; }
    
    public String getPriorityDisplay() { return priorityDisplay; }
    public void setPriorityDisplay(String priorityDisplay) { this.priorityDisplay = priorityDisplay; }
    
    public CaseRecord.CaseStatus getStatus() { return status; }
    public void setStatus(CaseRecord.CaseStatus status) { this.status = status; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Instant getOpenedDate() { return openedDate; }
    public void setOpenedDate(Instant openedDate) { this.openedDate = openedDate; }
    
    public Instant getClosedDate() { return closedDate; }
    public void setClosedDate(Instant closedDate) { this.closedDate = closedDate; }
    
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
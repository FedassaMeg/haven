package org.haven.casemgmt.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Read model for RestrictedNote queries
 * Optimized for fast lookups and filtering without event replay
 */
@Entity
@Table(name = "restricted_note_read_model", indexes = {
    @Index(name = "idx_restricted_note_client_id", columnList = "clientId"),
    @Index(name = "idx_restricted_note_case_id", columnList = "caseId"),
    @Index(name = "idx_restricted_note_author_id", columnList = "authorId"),
    @Index(name = "idx_restricted_note_note_type", columnList = "noteType"),
    @Index(name = "idx_restricted_note_visibility_scope", columnList = "visibilityScope"),
    @Index(name = "idx_restricted_note_is_sealed", columnList = "isSealed"),
    @Index(name = "idx_restricted_note_expires_at", columnList = "expiresAt")
})
public class RestrictedNoteReadModel {
    
    @Id
    private UUID noteId;
    
    @Column(nullable = false)
    private UUID clientId;
    
    @Column(nullable = false)
    private String clientName;
    
    @Column(nullable = false)
    private UUID caseId;
    
    @Column(nullable = false)
    private String caseNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NoteType noteType;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    private String title;
    
    @Column(nullable = false)
    private UUID authorId;
    
    @Column(nullable = false)
    private String authorName;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column(nullable = false)
    private Instant lastModified;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VisibilityScope visibilityScope;
    
    @Column(nullable = false)
    private boolean isSealed;
    
    private String sealReason;
    
    private Instant sealedAt;
    
    private UUID sealedBy;
    
    private String sealedByName;
    
    private boolean isTemporary;
    
    private Instant expiresAt;
    
    @Version
    private Long version;
    
    protected RestrictedNoteReadModel() {
        // JPA constructor
    }
    
    public RestrictedNoteReadModel(UUID noteId, UUID clientId, String clientName, 
                                 UUID caseId, String caseNumber, NoteType noteType, 
                                 String content, String title, UUID authorId, 
                                 String authorName, Instant createdAt, 
                                 VisibilityScope visibilityScope) {
        this.noteId = noteId;
        this.clientId = clientId;
        this.clientName = clientName;
        this.caseId = caseId;
        this.caseNumber = caseNumber;
        this.noteType = noteType;
        this.content = content;
        this.title = title;
        this.authorId = authorId;
        this.authorName = authorName;
        this.createdAt = createdAt;
        this.lastModified = createdAt;
        this.visibilityScope = visibilityScope;
        this.isSealed = false;
    }
    
    // Getters and setters
    public UUID getNoteId() { return noteId; }
    public void setNoteId(UUID noteId) { this.noteId = noteId; }
    
    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }
    
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    
    public UUID getCaseId() { return caseId; }
    public void setCaseId(UUID caseId) { this.caseId = caseId; }
    
    public String getCaseNumber() { return caseNumber; }
    public void setCaseNumber(String caseNumber) { this.caseNumber = caseNumber; }
    
    public NoteType getNoteType() { return noteType; }
    public void setNoteType(NoteType noteType) { this.noteType = noteType; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public UUID getAuthorId() { return authorId; }
    public void setAuthorId(UUID authorId) { this.authorId = authorId; }
    
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getLastModified() { return lastModified; }
    public void setLastModified(Instant lastModified) { this.lastModified = lastModified; }
    
    public VisibilityScope getVisibilityScope() { return visibilityScope; }
    public void setVisibilityScope(VisibilityScope visibilityScope) { this.visibilityScope = visibilityScope; }
    
    public boolean isSealed() { return isSealed; }
    public void setSealed(boolean sealed) { isSealed = sealed; }
    
    public String getSealReason() { return sealReason; }
    public void setSealReason(String sealReason) { this.sealReason = sealReason; }
    
    public Instant getSealedAt() { return sealedAt; }
    public void setSealedAt(Instant sealedAt) { this.sealedAt = sealedAt; }
    
    public UUID getSealedBy() { return sealedBy; }
    public void setSealedBy(UUID sealedBy) { this.sealedBy = sealedBy; }
    
    public String getSealedByName() { return sealedByName; }
    public void setSealedByName(String sealedByName) { this.sealedByName = sealedByName; }
    
    public boolean isTemporary() { return isTemporary; }
    public void setTemporary(boolean temporary) { isTemporary = temporary; }
    
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    
    // Enums to match domain model
    public enum NoteType {
        STANDARD, COUNSELING, PRIVILEGED_COUNSELING, LEGAL_ADVOCACY, 
        ATTORNEY_CLIENT, SAFETY_PLAN, MEDICAL, THERAPEUTIC, 
        INTERNAL_ADMIN, WORKFLOW_PROGRESS, COMPLIANCE_VERIFICATION, 
        ALERT, INVESTIGATION_UPDATE, MANDATED_REPORT
    }
    
    public enum VisibilityScope {
        PUBLIC, CASE_TEAM, CLINICAL_ONLY, LEGAL_TEAM, SAFETY_TEAM, 
        MEDICAL_TEAM, ADMIN_ONLY, AUTHOR_ONLY, ATTORNEY_CLIENT, CUSTOM
    }
}
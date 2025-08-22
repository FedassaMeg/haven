package org.haven.readmodels.infrastructure;

import org.haven.readmodels.domain.RestrictedNote;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "restricted_notes")
public class JpaRestrictedNoteEntity {
    
    @Id
    @Column(name = "note_id")
    private UUID noteId;
    
    @Column(name = "client_id", nullable = false)
    private UUID clientId;
    
    @Column(name = "client_name")
    private String clientName;
    
    @Column(name = "case_id")
    private UUID caseId;
    
    @Column(name = "case_number")
    private String caseNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "note_type", nullable = false)
    private RestrictedNote.NoteType noteType;
    
    @Lob
    @Column(name = "content")
    private String content;
    
    @Column(name = "author_id", nullable = false)
    private UUID authorId;
    
    @Column(name = "author_name")
    private String authorName;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "last_modified")
    private Instant lastModified;
    
    @ElementCollection
    @CollectionTable(name = "note_authorized_viewers", joinColumns = @JoinColumn(name = "note_id"))
    @Column(name = "user_id")
    private List<UUID> authorizedViewers;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_scope", nullable = false)
    private RestrictedNote.VisibilityScope visibilityScope;
    
    @Column(name = "is_sealed")
    private Boolean isSealed;
    
    @Column(name = "seal_reason")
    private String sealReason;
    
    @Column(name = "sealed_at")
    private Instant sealedAt;
    
    @Column(name = "sealed_by")
    private UUID sealedBy;
    
    // Default constructor for JPA
    public JpaRestrictedNoteEntity() {}
    
    // Constructor from domain object
    public JpaRestrictedNoteEntity(RestrictedNote note) {
        this.noteId = note.getNoteId();
        this.clientId = note.getClientId();
        this.clientName = note.getClientName();
        this.caseId = note.getCaseId();
        this.caseNumber = note.getCaseNumber();
        this.noteType = note.getNoteType();
        this.content = note.getContent();
        this.authorId = note.getAuthorId();
        this.authorName = note.getAuthorName();
        this.createdAt = note.getCreatedAt();
        this.lastModified = note.getLastModified();
        this.authorizedViewers = note.getAuthorizedViewers();
        this.visibilityScope = note.getVisibilityScope();
        this.isSealed = note.getIsSealed();
        this.sealReason = note.getSealReason();
        this.sealedAt = note.getSealedAt();
        this.sealedBy = note.getSealedBy();
    }
    
    // Convert to domain object
    public RestrictedNote toDomain() {
        RestrictedNote note = new RestrictedNote();
        note.setNoteId(this.noteId);
        note.setClientId(this.clientId);
        note.setClientName(this.clientName);
        note.setCaseId(this.caseId);
        note.setCaseNumber(this.caseNumber);
        note.setNoteType(this.noteType);
        note.setContent(this.content);
        note.setAuthorId(this.authorId);
        note.setAuthorName(this.authorName);
        note.setCreatedAt(this.createdAt);
        note.setLastModified(this.lastModified);
        note.setAuthorizedViewers(this.authorizedViewers);
        note.setVisibilityScope(this.visibilityScope);
        note.setIsSealed(this.isSealed);
        note.setSealReason(this.sealReason);
        note.setSealedAt(this.sealedAt);
        note.setSealedBy(this.sealedBy);
        return note;
    }
    
    // Update from domain object
    public void updateFrom(RestrictedNote note) {
        this.clientName = note.getClientName();
        this.caseNumber = note.getCaseNumber();
        this.noteType = note.getNoteType();
        this.content = note.getContent();
        this.authorName = note.getAuthorName();
        this.lastModified = note.getLastModified();
        this.authorizedViewers = note.getAuthorizedViewers();
        this.visibilityScope = note.getVisibilityScope();
        this.isSealed = note.getIsSealed();
        this.sealReason = note.getSealReason();
        this.sealedAt = note.getSealedAt();
        this.sealedBy = note.getSealedBy();
    }
    
    // Getters and Setters
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
    
    public RestrictedNote.NoteType getNoteType() { return noteType; }
    public void setNoteType(RestrictedNote.NoteType noteType) { this.noteType = noteType; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public UUID getAuthorId() { return authorId; }
    public void setAuthorId(UUID authorId) { this.authorId = authorId; }
    
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getLastModified() { return lastModified; }
    public void setLastModified(Instant lastModified) { this.lastModified = lastModified; }
    
    public List<UUID> getAuthorizedViewers() { return authorizedViewers; }
    public void setAuthorizedViewers(List<UUID> authorizedViewers) { this.authorizedViewers = authorizedViewers; }
    
    public RestrictedNote.VisibilityScope getVisibilityScope() { return visibilityScope; }
    public void setVisibilityScope(RestrictedNote.VisibilityScope visibilityScope) { this.visibilityScope = visibilityScope; }
    
    public Boolean getIsSealed() { return isSealed; }
    public void setIsSealed(Boolean isSealed) { this.isSealed = isSealed; }
    
    public String getSealReason() { return sealReason; }
    public void setSealReason(String sealReason) { this.sealReason = sealReason; }
    
    public Instant getSealedAt() { return sealedAt; }
    public void setSealedAt(Instant sealedAt) { this.sealedAt = sealedAt; }
    
    public UUID getSealedBy() { return sealedBy; }
    public void setSealedBy(UUID sealedBy) { this.sealedBy = sealedBy; }
}
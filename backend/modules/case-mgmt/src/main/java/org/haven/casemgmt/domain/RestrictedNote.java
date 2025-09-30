package org.haven.casemgmt.domain;

import org.haven.casemgmt.domain.events.*;
import org.haven.shared.events.DomainEvent;
import org.haven.shared.domain.AggregateRoot;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class RestrictedNote extends AggregateRoot<RestrictedNoteId> {
    
    private RestrictedNoteId noteId;
    private UUID clientId;
    private String clientName;
    private UUID caseId;
    private String caseNumber;
    private NoteType noteType;
    private String content;
    private String title;
    private UUID authorId;
    private String authorName;
    private Instant createdAt;
    private Instant lastModified;
    private List<UUID> authorizedViewers;
    private VisibilityScope visibilityScope;
    private boolean isSealed;
    private String sealReason;
    private Instant sealedAt;
    private UUID sealedBy;
    private String sealedByName;
    private boolean isTemporary;
    private Instant expiresAt;
    
    public enum NoteType {
        STANDARD("Standard case note", VisibilityScope.CASE_TEAM),
        COUNSELING("Counseling session note", VisibilityScope.CLINICAL_ONLY),
        PRIVILEGED_COUNSELING("Privileged counseling note", VisibilityScope.AUTHOR_ONLY),
        LEGAL_ADVOCACY("Legal advocacy note", VisibilityScope.LEGAL_TEAM),
        ATTORNEY_CLIENT("Attorney-client privileged", VisibilityScope.ATTORNEY_CLIENT),
        SAFETY_PLAN("Safety planning discussion", VisibilityScope.SAFETY_TEAM),
        MEDICAL("Medical information", VisibilityScope.MEDICAL_TEAM),
        THERAPEUTIC("Therapeutic session", VisibilityScope.CLINICAL_ONLY),
        INTERNAL_ADMIN("Internal administrative note", VisibilityScope.ADMIN_ONLY),
        WORKFLOW_PROGRESS("Workflow progress note", VisibilityScope.CASE_TEAM),
        COMPLIANCE_VERIFICATION("Compliance verification", VisibilityScope.ADMIN_ONLY),
        ALERT("Alert notification", VisibilityScope.ADMIN_ONLY),
        INVESTIGATION_UPDATE("Investigation update", VisibilityScope.LEGAL_TEAM),
        MANDATED_REPORT("Mandated report", VisibilityScope.LEGAL_TEAM);
        
        private final String description;
        private final VisibilityScope defaultScope;
        
        NoteType(String description, VisibilityScope defaultScope) {
            this.description = description;
            this.defaultScope = defaultScope;
        }
        
        public String getDescription() { return description; }
        public VisibilityScope getDefaultScope() { return defaultScope; }
    }
    
    public enum VisibilityScope {
        PUBLIC("Visible to all case team members"),
        CASE_TEAM("Visible to assigned case team"),
        CLINICAL_ONLY("Visible to clinical staff only"),
        LEGAL_TEAM("Visible to legal advocates only"),
        SAFETY_TEAM("Visible to safety planning team"),
        MEDICAL_TEAM("Visible to medical staff only"),
        ADMIN_ONLY("Visible to administrators only"),
        AUTHOR_ONLY("Visible to author only"),
        ATTORNEY_CLIENT("Attorney-client privileged"),
        CUSTOM("Custom access list");
        
        private final String description;
        
        VisibilityScope(String description) {
            this.description = description;
        }
        
        public String getDescription() { return description; }
    }
    
    protected RestrictedNote() {
        // For event sourcing reconstruction
    }
    
    public static RestrictedNote create(UUID clientId, String clientName, UUID caseId, String caseNumber,
                                      NoteType noteType, String content, String title, UUID authorId, 
                                      String authorName, List<UUID> authorizedViewers, VisibilityScope visibilityScope) {
        RestrictedNote note = new RestrictedNote();
        RestrictedNoteId noteId = RestrictedNoteId.newId();
        note.id = noteId;
        Instant now = Instant.now();
        
        VisibilityScope finalScope = visibilityScope != null ? visibilityScope : noteType.getDefaultScope();
        
        RestrictedNoteCreated event = new RestrictedNoteCreated(
            noteId.value(), clientId, clientName, caseId, caseNumber, 
            noteType.name(), content, authorId, authorName, now, 
            authorizedViewers, finalScope.name(), title
        );
        
        note.apply(event);
        return note;
    }
    
    public static RestrictedNote reconstruct() {
        return new RestrictedNote();
    }
    
    public void updateContent(String newContent, UUID updatedBy, String updatedByName, String updateReason) {
        if (isSealed) {
            throw new IllegalStateException("Cannot update sealed note");
        }
        
        RestrictedNoteUpdated event = new RestrictedNoteUpdated(
            noteId.value(), newContent, updatedBy, updatedByName, Instant.now(),
            authorizedViewers, visibilityScope.name(), updateReason
        );
        
        apply(event);
    }
    
    public void seal(UUID sealedBy, String sealedByName, String sealReason, String legalBasis, 
                    boolean isTemporary, Instant expiresAt) {
        if (isSealed) {
            throw new IllegalStateException("Note is already sealed");
        }
        
        RestrictedNoteSealed event = new RestrictedNoteSealed(
            noteId.value(), sealedBy, sealedByName, Instant.now(), sealReason, 
            legalBasis, isTemporary, expiresAt
        );
        
        apply(event);
    }
    
    public void unseal(UUID unsealedBy, String unsealedByName, String unsealReason, String legalBasis) {
        if (!isSealed) {
            throw new IllegalStateException("Note is not sealed");
        }
        
        RestrictedNoteUnsealed event = new RestrictedNoteUnsealed(
            noteId.value(), unsealedBy, unsealedByName, Instant.now(), unsealReason, 
            legalBasis, this.sealReason
        );
        
        apply(event);
    }
    
    public void recordAccess(UUID accessedBy, String accessedByName, List<String> userRoles, 
                           String accessMethod, String ipAddress, String userAgent, 
                           boolean wasContentViewed, String accessReason) {
        RestrictedNoteAccessed event = new RestrictedNoteAccessed(
            noteId.value(), accessedBy, accessedByName, Instant.now(), userRoles, 
            accessMethod, ipAddress, userAgent, wasContentViewed, accessReason
        );
        
        apply(event);
    }
    
    public boolean isVisibleTo(UUID userId, List<String> userRoles) {
        if (isSealed && !userId.equals(sealedBy)) {
            return false;
        }
        
        if (authorizedViewers != null && !authorizedViewers.isEmpty()) {
            return authorizedViewers.contains(userId);
        }
        
        if (noteType == NoteType.PRIVILEGED_COUNSELING) {
            return userRoles.contains("DV_COUNSELOR") || userRoles.contains("LICENSED_CLINICIAN") || 
                   userId.equals(authorId);
        }
        
        switch (visibilityScope) {
            case PUBLIC:
                return true;
            case CASE_TEAM:
                return userRoles.contains("CASE_MANAGER") || userRoles.contains("SUPERVISOR");
            case CLINICAL_ONLY:
                return userRoles.contains("CLINICIAN") || userRoles.contains("THERAPIST") || 
                       userRoles.contains("COUNSELOR") || userRoles.contains("DV_COUNSELOR");
            case LEGAL_TEAM:
                return userRoles.contains("LEGAL_ADVOCATE") || userRoles.contains("ATTORNEY");
            case SAFETY_TEAM:
                return userRoles.contains("SAFETY_SPECIALIST") || userRoles.contains("CRISIS_COUNSELOR");
            case MEDICAL_TEAM:
                return userRoles.contains("NURSE") || userRoles.contains("DOCTOR") || 
                       userRoles.contains("MEDICAL_ADVOCATE");
            case ADMIN_ONLY:
                return userRoles.contains("ADMINISTRATOR") || userRoles.contains("SUPERVISOR");
            case AUTHOR_ONLY:
                return userId.equals(authorId);
            case ATTORNEY_CLIENT:
                return userRoles.contains("ATTORNEY") || userId.equals(authorId);
            case CUSTOM:
                return authorizedViewers != null && authorizedViewers.contains(userId);
            default:
                return false;
        }
    }
    
    public boolean requiresSpecialHandling() {
        return visibilityScope == VisibilityScope.ATTORNEY_CLIENT || 
               visibilityScope == VisibilityScope.AUTHOR_ONLY ||
               noteType == NoteType.PRIVILEGED_COUNSELING ||
               isSealed;
    }
    
    @Override
    protected void when(DomainEvent event) {
        switch (event.eventType()) {
            case "RestrictedNoteCreated":
                on((RestrictedNoteCreated) event);
                break;
            case "RestrictedNoteUpdated":
                on((RestrictedNoteUpdated) event);
                break;
            case "RestrictedNoteSealed":
                on((RestrictedNoteSealed) event);
                break;
            case "RestrictedNoteUnsealed":
                on((RestrictedNoteUnsealed) event);
                break;
            case "RestrictedNoteAccessed":
                on((RestrictedNoteAccessed) event);
                break;
        }
    }
    
    private void on(RestrictedNoteCreated event) {
        this.noteId = RestrictedNoteId.of(event.getNoteId());
        this.clientId = event.getClientId();
        this.clientName = event.getClientName();
        this.caseId = event.getCaseId();
        this.caseNumber = event.getCaseNumber();
        this.noteType = NoteType.valueOf(event.getNoteType());
        this.content = event.getContent();
        this.title = event.getTitle();
        this.authorId = event.getAuthorId();
        this.authorName = event.getAuthorName();
        this.createdAt = event.getCreatedAt();
        this.lastModified = event.getCreatedAt();
        this.authorizedViewers = event.getAuthorizedViewers();
        this.visibilityScope = VisibilityScope.valueOf(event.getVisibilityScope());
        this.isSealed = false;
    }
    
    private void on(RestrictedNoteUpdated event) {
        this.content = event.getContent();
        this.lastModified = event.getUpdatedAt();
        this.authorizedViewers = event.getAuthorizedViewers();
        if (event.getVisibilityScope() != null) {
            this.visibilityScope = VisibilityScope.valueOf(event.getVisibilityScope());
        }
    }
    
    private void on(RestrictedNoteSealed event) {
        this.isSealed = true;
        this.sealReason = event.getSealReason();
        this.sealedAt = event.getSealedAt();
        this.sealedBy = event.getSealedBy();
        this.sealedByName = event.getSealedByName();
        this.isTemporary = event.isTemporary();
        this.expiresAt = event.getExpiresAt();
    }
    
    private void on(RestrictedNoteUnsealed event) {
        this.isSealed = false;
        this.sealReason = null;
        this.sealedAt = null;
        this.sealedBy = null;
        this.sealedByName = null;
        this.isTemporary = false;
        this.expiresAt = null;
    }
    
    private void on(RestrictedNoteAccessed event) {
        // Access events don't change the state, they're just for auditing
    }
    
    // Getters
    public UUID getNoteId() { return noteId.value(); }
    public UUID getClientId() { return clientId; }
    public String getClientName() { return clientName; }
    public UUID getCaseId() { return caseId; }
    public String getCaseNumber() { return caseNumber; }
    public NoteType getNoteType() { return noteType; }
    public String getContent() { return content; }
    public String getTitle() { return title; }
    public UUID getAuthorId() { return authorId; }
    public String getAuthorName() { return authorName; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastModified() { return lastModified; }
    public List<UUID> getAuthorizedViewers() { return authorizedViewers; }
    public VisibilityScope getVisibilityScope() { return visibilityScope; }
    public boolean isSealed() { return isSealed; }
    public String getSealReason() { return sealReason; }
    public Instant getSealedAt() { return sealedAt; }
    public UUID getSealedBy() { return sealedBy; }
    public String getSealedByName() { return sealedByName; }
    public boolean isTemporary() { return isTemporary; }
    public Instant getExpiresAt() { return expiresAt; }
}
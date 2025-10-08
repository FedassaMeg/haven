package org.haven.readmodels.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class RestrictedNote {
    private UUID noteId;
    private UUID clientId;
    private String clientName;
    private UUID caseId;
    private String caseNumber;
    private NoteType noteType;
    private String content;
    private UUID authorId;
    private String authorName;
    private Instant createdAt;
    private Instant lastModified;
    private List<UUID> authorizedViewers;
    private VisibilityScope visibilityScope;
    private Boolean isSealed;
    private String sealReason;
    private Instant sealedAt;
    private UUID sealedBy;
    
    public enum NoteType {
        STANDARD("Standard case note", VisibilityScope.CASE_TEAM),
        COUNSELING("Counseling session note", VisibilityScope.CLINICAL_ONLY),
        PRIVILEGED_COUNSELING("Privileged counseling note", VisibilityScope.AUTHOR_ONLY),
        LEGAL_ADVOCACY("Legal advocacy note", VisibilityScope.LEGAL_TEAM),
        ATTORNEY_CLIENT("Attorney-client privileged", VisibilityScope.ATTORNEY_CLIENT),
        SAFETY_PLAN("Safety planning discussion", VisibilityScope.SAFETY_TEAM),
        MEDICAL("Medical information", VisibilityScope.MEDICAL_TEAM),
        THERAPEUTIC("Therapeutic session", VisibilityScope.CLINICAL_ONLY),
        INTERNAL_ADMIN("Internal administrative note", VisibilityScope.ADMIN_ONLY);
        
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
    
    public RestrictedNote() {}
    
    public RestrictedNote(UUID clientId, String clientName, UUID caseId, String caseNumber, 
                         NoteType noteType, String content, UUID authorId, String authorName) {
        this.noteId = UUID.randomUUID();
        this.clientId = clientId;
        this.clientName = clientName;
        this.caseId = caseId;
        this.caseNumber = caseNumber;
        this.noteType = noteType;
        this.content = content;
        this.authorId = authorId;
        this.authorName = authorName;
        this.createdAt = Instant.now();
        this.lastModified = Instant.now();
        this.visibilityScope = noteType.getDefaultScope();
        this.isSealed = false;
    }
    
    public boolean isVisibleTo(UUID userId, List<String> userRoles) {
        return isVisibleTo(userId, userRoles, null);
    }
    
    public boolean isVisibleTo(UUID userId, List<String> userRoles, 
                              List<org.haven.clientprofile.domain.pii.QualifiedDVCounselorVerification.UserCredential> credentials) {
        if (isSealed && !userId.equals(sealedBy)) {
            return false;
        }
        
        if (authorizedViewers != null && !authorizedViewers.isEmpty()) {
            return authorizedViewers.contains(userId);
        }
        
        // Special handling for privileged counseling notes (EVID § 1037.1)
        if (noteType == NoteType.PRIVILEGED_COUNSELING && credentials != null) {
            return org.haven.clientprofile.domain.pii.QualifiedDVCounselorVerification
                .canAccessPrivilegedCounselingNotes(userId, userRoles, credentials, authorId);
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
                return userRoles.contains("NURSE") || userRoles.contains("DOCTOR") || userRoles.contains("MEDICAL_ADVOCATE");
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
    
    public String getVisibilityWarning() {
        if (requiresSpecialHandling()) {
            switch (visibilityScope) {
                case ATTORNEY_CLIENT:
                    return "⚖️ ATTORNEY-CLIENT PRIVILEGED - Confidential legal communication";
                case AUTHOR_ONLY:
                    return "🔒 RESTRICTED ACCESS - Author only";
                default:
                    if (noteType == NoteType.PRIVILEGED_COUNSELING) {
                        return "🔐 PRIVILEGED COUNSELING - Confidential therapeutic communication";
                    }
                    if (isSealed) {
                        return "🚫 SEALED NOTE - " + (sealReason != null ? sealReason : "Access restricted");
                    }
                    return "⚠️ RESTRICTED - Special handling required";
            }
        }
        return null;
    }
    
    public void seal(UUID userId, String reason) {
        this.isSealed = true;
        this.sealReason = reason;
        this.sealedAt = Instant.now();
        this.sealedBy = userId;
    }
    
    public void unseal() {
        this.isSealed = false;
        this.sealReason = null;
        this.sealedAt = null;
        this.sealedBy = null;
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
    
    public NoteType getNoteType() { return noteType; }
    public void setNoteType(NoteType noteType) { this.noteType = noteType; }
    
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
    
    public VisibilityScope getVisibilityScope() { return visibilityScope; }
    public void setVisibilityScope(VisibilityScope visibilityScope) { this.visibilityScope = visibilityScope; }
    
    public Boolean getIsSealed() { return isSealed; }
    public void setIsSealed(Boolean isSealed) { this.isSealed = isSealed; }
    
    public String getSealReason() { return sealReason; }
    public void setSealReason(String sealReason) { this.sealReason = sealReason; }
    
    public Instant getSealedAt() { return sealedAt; }
    public void setSealedAt(Instant sealedAt) { this.sealedAt = sealedAt; }
    
    public UUID getSealedBy() { return sealedBy; }
    public void setSealedBy(UUID sealedBy) { this.sealedBy = sealedBy; }
}
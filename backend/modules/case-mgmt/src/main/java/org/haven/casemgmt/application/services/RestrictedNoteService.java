package org.haven.casemgmt.application.services;

import org.haven.casemgmt.domain.RestrictedNote;
import org.haven.casemgmt.domain.RestrictedNoteId;
import org.haven.casemgmt.domain.RestrictedNoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for creating and managing restricted notes
 * Automatically creates secured notes for mandated report activities
 */
@Service
public class RestrictedNoteService {
    
    private final RestrictedNoteRepository restrictedNoteRepository;
    
    @Autowired
    public RestrictedNoteService(RestrictedNoteRepository restrictedNoteRepository) {
        this.restrictedNoteRepository = restrictedNoteRepository;
    }
    
    /**
     * Create a new restricted note with specified access controls
     */
    public UUID createRestrictedNote(UUID clientId, String clientName, UUID caseId, String caseNumber,
                                   String noteType, String content, String title, UUID createdByUserId, 
                                   String createdByName, List<UUID> authorizedViewers, String visibilityScope) {
        
        RestrictedNote.NoteType type = RestrictedNote.NoteType.valueOf(noteType);
        RestrictedNote.VisibilityScope scope = visibilityScope != null ? 
            RestrictedNote.VisibilityScope.valueOf(visibilityScope) : type.getDefaultScope();
        
        RestrictedNote note = RestrictedNote.create(
            clientId, clientName, caseId, caseNumber, type, content, title,
            createdByUserId, createdByName, authorizedViewers, scope
        );
        
        restrictedNoteRepository.save(note);
        return note.getId().value();
    }
    
    /**
     * Add update to existing note (for status changes, document attachments, etc.)
     */
    public void updateRestrictedNote(UUID noteId, String updateContent, UUID updatedByUserId, 
                                   String updatedByName, String updateReason) {
        RestrictedNote note = restrictedNoteRepository.findById(RestrictedNoteId.of(noteId))
            .orElseThrow(() -> new IllegalArgumentException("Note not found: " + noteId));
        
        note.updateContent(updateContent, updatedByUserId, updatedByName, updateReason);
        restrictedNoteRepository.save(note);
    }
    
    /**
     * Seal a restricted note
     */
    public void sealNote(UUID noteId, UUID sealedByUserId, String sealedByName, String sealReason, 
                        String legalBasis, boolean isTemporary, Instant expiresAt) {
        RestrictedNote note = restrictedNoteRepository.findById(RestrictedNoteId.of(noteId))
            .orElseThrow(() -> new IllegalArgumentException("Note not found: " + noteId));
        
        note.seal(sealedByUserId, sealedByName, sealReason, legalBasis, isTemporary, expiresAt);
        restrictedNoteRepository.save(note);
    }
    
    /**
     * Unseal a restricted note
     */
    public void unsealNote(UUID noteId, UUID unsealedByUserId, String unsealedByName, 
                          String unsealReason, String legalBasis) {
        RestrictedNote note = restrictedNoteRepository.findById(RestrictedNoteId.of(noteId))
            .orElseThrow(() -> new IllegalArgumentException("Note not found: " + noteId));
        
        note.unseal(unsealedByUserId, unsealedByName, unsealReason, legalBasis);
        restrictedNoteRepository.save(note);
    }
    
    /**
     * Record access to a restricted note for audit purposes
     */
    public void recordNoteAccess(UUID noteId, UUID accessedByUserId, String accessedByName, 
                               List<String> userRoles, String accessMethod, String ipAddress, 
                               String userAgent, boolean wasContentViewed, String accessReason) {
        RestrictedNote note = restrictedNoteRepository.findById(RestrictedNoteId.of(noteId))
            .orElseThrow(() -> new IllegalArgumentException("Note not found: " + noteId));
        
        note.recordAccess(accessedByUserId, accessedByName, userRoles, accessMethod, 
                         ipAddress, userAgent, wasContentViewed, accessReason);
        restrictedNoteRepository.save(note);
    }
    
    /**
     * Get accessible notes for a user
     */
    public List<RestrictedNote> getAccessibleNotesForUser(UUID userId, List<String> userRoles) {
        return restrictedNoteRepository.findAccessibleToUser(userId, userRoles);
    }
    
    /**
     * Get accessible notes for a specific client
     */
    public List<RestrictedNote> getAccessibleNotesForClient(UUID clientId, UUID userId, List<String> userRoles) {
        return restrictedNoteRepository.findByClientIdAccessibleToUser(clientId, userId, userRoles);
    }
    
    /**
     * Check if user has access to a specific note
     */
    public boolean hasAccess(UUID noteId, UUID userId, List<String> userRoles) {
        return restrictedNoteRepository.hasValidAccess(noteId, userId, userRoles);
    }
    
    /**
     * Create progress note for mandated report workflow
     */
    public UUID createWorkflowProgressNote(UUID clientId, String clientName, UUID reportId, 
                                         String workflowStep, String progressDescription, 
                                         UUID createdByUserId, String createdByName) {
        
        String content = String.format(
            "MANDATED REPORT WORKFLOW PROGRESS\n\n" +
            "Report ID: %s\n" +
            "Workflow Step: %s\n" +
            "Progress: %s\n" +
            "Timestamp: %s\n" +
            "Updated by: %s",
            reportId, workflowStep, progressDescription, Instant.now(), createdByUserId
        );
        
        return createRestrictedNote(
            clientId, clientName, reportId, "RPT-" + reportId.toString().substring(0, 8),
            "WORKFLOW_PROGRESS", content, "Mandated Report Workflow Update",
            createdByUserId, createdByName, null, "CASE_TEAM"
        );
    }
    
    /**
     * Create compliance note for audit trail
     */
    public UUID createComplianceNote(UUID clientId, String clientName, UUID reportId, 
                                    String complianceRequirement, String complianceStatus, 
                                    UUID createdByUserId, String createdByName) {
        
        String content = String.format(
            "COMPLIANCE VERIFICATION\n\n" +
            "Report ID: %s\n" +
            "Requirement: %s\n" +
            "Status: %s\n" +
            "Verified At: %s\n" +
            "Verified By: %s\n\n" +
            "This note documents compliance with legal reporting requirements.",
            reportId, complianceRequirement, complianceStatus, Instant.now(), createdByUserId
        );
        
        return createRestrictedNote(
            clientId, clientName, reportId, "RPT-" + reportId.toString().substring(0, 8),
            "COMPLIANCE_VERIFICATION", content, "Compliance Verification: " + complianceRequirement,
            createdByUserId, createdByName, null, "ADMIN_ONLY"
        );
    }
    
    /**
     * Create alert note for urgent mandated report issues
     */
    public UUID createAlertNote(UUID clientId, String clientName, UUID reportId, String alertType, 
                              String alertDescription, UUID createdByUserId, String createdByName,
                              boolean requiresImmediateAttention) {
        
        String urgencyLevel = requiresImmediateAttention ? "IMMEDIATE" : "HIGH";
        
        String content = String.format(
            "MANDATED REPORT ALERT - %s PRIORITY\n\n" +
            "Report ID: %s\n" +
            "Alert Type: %s\n" +
            "Description: %s\n" +
            "Created At: %s\n" +
            "Created By: %s\n\n" +
            "%s",
            urgencyLevel, reportId, alertType, alertDescription, 
            Instant.now(), createdByUserId,
            requiresImmediateAttention ? 
                "*** IMMEDIATE ATTENTION REQUIRED ***" : 
                "Review Required"
        );
        
        return createRestrictedNote(
            clientId, clientName, reportId, "RPT-" + reportId.toString().substring(0, 8),
            "ALERT", content, "ALERT: " + alertType,
            createdByUserId, createdByName, null, "ADMIN_ONLY"
        );
    }
    
    /**
     * Create investigation note for tracking agency response
     */
    public UUID createInvestigationNote(UUID clientId, String clientName, UUID reportId, 
                                      String investigationPhase, String findings, 
                                      UUID createdByUserId, String createdByName) {
        
        String content = String.format(
            "INVESTIGATION UPDATE\n\n" +
            "Report ID: %s\n" +
            "Investigation Phase: %s\n" +
            "Findings: %s\n" +
            "Updated At: %s\n" +
            "Updated By: %s\n\n" +
            "This note tracks the external agency investigation of the mandated report.",
            reportId, investigationPhase, findings, Instant.now(), createdByUserId
        );
        
        return createRestrictedNote(
            clientId, clientName, reportId, "RPT-" + reportId.toString().substring(0, 8),
            "INVESTIGATION_UPDATE", content, "Investigation Update: " + investigationPhase,
            createdByUserId, createdByName, null, "LEGAL_TEAM"
        );
    }
}
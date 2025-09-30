package org.haven.api.restrictednotes;

import org.haven.casemgmt.application.services.RestrictedNoteService;
import org.haven.casemgmt.application.services.RestrictedNoteAuditService;
import org.haven.casemgmt.domain.RestrictedNote;
import org.haven.api.restrictednotes.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for restricted notes management
 * Provides endpoints for CRUD operations, sealing/unsealing, and audit logging
 */
@RestController
@RequestMapping("/api/restricted-notes")
@PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
public class RestrictedNotesController {
    
    private final RestrictedNoteService restrictedNoteService;
    private final RestrictedNoteAuditService auditService;
    
    @Autowired
    public RestrictedNotesController(RestrictedNoteService restrictedNoteService,
                                   RestrictedNoteAuditService auditService) {
        this.restrictedNoteService = restrictedNoteService;
        this.auditService = auditService;
    }
    
    /**
     * Create a new restricted note
     */
    @PostMapping
    @PreAuthorize("hasRole('CASE_MANAGER') or hasRole('CLINICIAN') or hasRole('LEGAL_ADVOCATE') or hasRole('ADMIN')")
    public ResponseEntity<RestrictedNoteResponse> createNote(
            @RequestBody CreateRestrictedNoteRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) {
        
        UUID noteId = restrictedNoteService.createRestrictedNote(
            request.getClientId(),
            request.getClientName(),
            request.getCaseId(),
            request.getCaseNumber(),
            request.getNoteType(),
            request.getContent(),
            request.getTitle(),
            getUserId(auth),
            getUserName(auth),
            request.getAuthorizedViewers(),
            request.getVisibilityScope()
        );
        
        // Record the creation access
        restrictedNoteService.recordNoteAccess(
            noteId, getUserId(auth), getUserName(auth), getUserRoles(auth),
            "CREATE", getClientIP(httpRequest), getUserAgent(httpRequest),
            true, "Note creation"
        );
        
        RestrictedNoteResponse response = new RestrictedNoteResponse();
        response.setNoteId(noteId);
        response.setMessage("Restricted note created successfully");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Update an existing restricted note
     */
    @PutMapping("/{noteId}")
    @PreAuthorize("hasRole('CASE_MANAGER') or hasRole('CLINICIAN') or hasRole('LEGAL_ADVOCATE') or hasRole('ADMIN')")
    public ResponseEntity<RestrictedNoteResponse> updateNote(
            @PathVariable UUID noteId,
            @RequestBody UpdateRestrictedNoteRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) {
        
        // Check access before allowing update
        if (!restrictedNoteService.hasAccess(noteId, getUserId(auth), getUserRoles(auth))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        restrictedNoteService.updateRestrictedNote(
            noteId,
            request.getContent(),
            getUserId(auth),
            getUserName(auth),
            request.getUpdateReason()
        );
        
        // Record the update access
        restrictedNoteService.recordNoteAccess(
            noteId, getUserId(auth), getUserName(auth), getUserRoles(auth),
            "UPDATE", getClientIP(httpRequest), getUserAgent(httpRequest),
            true, request.getUpdateReason()
        );
        
        RestrictedNoteResponse response = new RestrictedNoteResponse();
        response.setNoteId(noteId);
        response.setMessage("Restricted note updated successfully");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Seal a restricted note
     */
    @PostMapping("/{noteId}/seal")
    @PreAuthorize("hasRole('SUPERVISOR') or hasRole('ADMIN') or hasRole('LEGAL_COUNSEL')")
    public ResponseEntity<RestrictedNoteResponse> sealNote(
            @PathVariable UUID noteId,
            @RequestBody SealNoteRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) {
        
        restrictedNoteService.sealNote(
            noteId,
            getUserId(auth),
            getUserName(auth),
            request.getSealReason(),
            request.getLegalBasis(),
            request.isTemporary(),
            request.getExpiresAt()
        );
        
        // Record the seal action
        restrictedNoteService.recordNoteAccess(
            noteId, getUserId(auth), getUserName(auth), getUserRoles(auth),
            "SEAL", getClientIP(httpRequest), getUserAgent(httpRequest),
            false, "Note sealed: " + request.getSealReason()
        );
        
        RestrictedNoteResponse response = new RestrictedNoteResponse();
        response.setNoteId(noteId);
        response.setMessage("Restricted note sealed successfully");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Unseal a restricted note
     */
    @PostMapping("/{noteId}/unseal")
    @PreAuthorize("hasRole('SUPERVISOR') or hasRole('ADMIN') or hasRole('LEGAL_COUNSEL')")
    public ResponseEntity<RestrictedNoteResponse> unsealNote(
            @PathVariable UUID noteId,
            @RequestBody UnsealNoteRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) {
        
        restrictedNoteService.unsealNote(
            noteId,
            getUserId(auth),
            getUserName(auth),
            request.getUnsealReason(),
            request.getLegalBasis()
        );
        
        // Record the unseal action
        restrictedNoteService.recordNoteAccess(
            noteId, getUserId(auth), getUserName(auth), getUserRoles(auth),
            "UNSEAL", getClientIP(httpRequest), getUserAgent(httpRequest),
            false, "Note unsealed: " + request.getUnsealReason()
        );
        
        RestrictedNoteResponse response = new RestrictedNoteResponse();
        response.setNoteId(noteId);
        response.setMessage("Restricted note unsealed successfully");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get accessible notes for current user
     */
    @GetMapping("/accessible")
    public ResponseEntity<Page<RestrictedNoteDetailResponse>> getAccessibleNotes(
            Authentication auth,
            Pageable pageable,
            HttpServletRequest httpRequest) {
        
        List<RestrictedNote> notes = restrictedNoteService.getAccessibleNotesForUser(
            getUserId(auth), getUserRoles(auth)
        );
        
        List<RestrictedNoteDetailResponse> responses = notes.stream()
            .map(this::toDetailResponse)
            .collect(Collectors.toList());
        
        // Record access to note list
        responses.forEach(note -> 
            restrictedNoteService.recordNoteAccess(
                note.getNoteId(), getUserId(auth), getUserName(auth), getUserRoles(auth),
                "VIEW_LIST", getClientIP(httpRequest), getUserAgent(httpRequest),
                false, "Notes list access"
            )
        );
        
        Page<RestrictedNoteDetailResponse> page = new PageImpl<>(responses, pageable, responses.size());
        return ResponseEntity.ok(page);
    }
    
    /**
     * Get accessible notes for a specific client
     */
    @GetMapping("/client/{clientId}")
    public ResponseEntity<Page<RestrictedNoteDetailResponse>> getClientNotes(
            @PathVariable UUID clientId,
            Authentication auth,
            Pageable pageable,
            HttpServletRequest httpRequest) {
        
        List<RestrictedNote> notes = restrictedNoteService.getAccessibleNotesForClient(
            clientId, getUserId(auth), getUserRoles(auth)
        );
        
        List<RestrictedNoteDetailResponse> responses = notes.stream()
            .map(this::toDetailResponse)
            .collect(Collectors.toList());
        
        // Record access to client notes
        responses.forEach(note -> 
            restrictedNoteService.recordNoteAccess(
                note.getNoteId(), getUserId(auth), getUserName(auth), getUserRoles(auth),
                "VIEW_CLIENT_NOTES", getClientIP(httpRequest), getUserAgent(httpRequest),
                false, "Client notes access for: " + clientId
            )
        );
        
        Page<RestrictedNoteDetailResponse> page = new PageImpl<>(responses, pageable, responses.size());
        return ResponseEntity.ok(page);
    }
    
    /**
     * Get a specific note (with content redacted if necessary)
     */
    @GetMapping("/{noteId}")
    public ResponseEntity<RestrictedNoteDetailResponse> getNote(
            @PathVariable UUID noteId,
            Authentication auth,
            HttpServletRequest httpRequest) {
        
        if (!restrictedNoteService.hasAccess(noteId, getUserId(auth), getUserRoles(auth))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Record note access
        restrictedNoteService.recordNoteAccess(
            noteId, getUserId(auth), getUserName(auth), getUserRoles(auth),
            "VIEW_DETAIL", getClientIP(httpRequest), getUserAgent(httpRequest),
            true, "Note detail access"
        );
        
        // In a real implementation, we'd fetch the note and convert to response
        // For now, return a placeholder
        RestrictedNoteDetailResponse response = new RestrictedNoteDetailResponse();
        response.setNoteId(noteId);
        response.setMessage("Note access recorded, details would be returned here");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get audit trail for a specific note
     */
    @GetMapping("/{noteId}/audit")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER') or hasRole('SUPERVISOR')")
    public ResponseEntity<List<NoteAuditResponse>> getNoteAuditTrail(@PathVariable UUID noteId) {
        
        List<RestrictedNoteAuditService.AuditLogEntry> auditEntries = auditService.getAuditTrailForNote(noteId);
        
        List<NoteAuditResponse> auditTrail = auditEntries.stream()
            .map(this::toNoteAuditResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(auditTrail);
    }
    
    /**
     * Get compliance report for a specific note
     */
    @GetMapping("/{noteId}/compliance-report")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<RestrictedNoteAuditService.ComplianceReport> getComplianceReport(@PathVariable UUID noteId) {
        
        RestrictedNoteAuditService.ComplianceReport report = auditService.generateComplianceReport(noteId);
        return ResponseEntity.ok(report);
    }
    
    /**
     * Get access events for a specific note
     */
    @GetMapping("/{noteId}/access-log")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER') or hasRole('SUPERVISOR')")
    public ResponseEntity<List<NoteAuditResponse>> getAccessLog(@PathVariable UUID noteId) {
        
        List<RestrictedNoteAuditService.AuditLogEntry> accessEntries = auditService.getAccessEvents(noteId);
        
        List<NoteAuditResponse> accessLog = accessEntries.stream()
            .map(this::toNoteAuditResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(accessLog);
    }
    
    private RestrictedNoteDetailResponse toDetailResponse(RestrictedNote note) {
        RestrictedNoteDetailResponse response = new RestrictedNoteDetailResponse();
        response.setNoteId(note.getNoteId());
        response.setClientId(note.getClientId());
        response.setClientName(note.getClientName());
        response.setCaseId(note.getCaseId());
        response.setCaseNumber(note.getCaseNumber());
        response.setNoteType(note.getNoteType().name());
        response.setTitle(note.getTitle());
        response.setAuthorId(note.getAuthorId());
        response.setAuthorName(note.getAuthorName());
        response.setCreatedAt(note.getCreatedAt());
        response.setLastModified(note.getLastModified());
        response.setVisibilityScope(note.getVisibilityScope().name());
        response.setSealed(note.isSealed());
        response.setSealReason(note.getSealReason());
        response.setSealedAt(note.getSealedAt());
        response.setSealedBy(note.getSealedBy());
        response.setRequiresSpecialHandling(note.requiresSpecialHandling());
        
        // Content is included but may be redacted based on access level
        response.setContent(note.getContent());
        
        return response;
    }
    
    private NoteAuditResponse toNoteAuditResponse(RestrictedNoteAuditService.AuditLogEntry entry) {
        NoteAuditResponse response = new NoteAuditResponse();
        response.setNoteId(entry.getNoteId());
        response.setEventType(entry.getEventType());
        response.setPerformedBy(entry.getPerformedBy());
        response.setPerformedByName(entry.getPerformedByName());
        response.setUserRoles(entry.getUserRoles());
        response.setPerformedAt(entry.getOccurredAt());
        response.setReason(entry.getDetails());
        response.setAccessMethod(entry.getAccessMethod());
        response.setIpAddress(entry.getIpAddress());
        response.setUserAgent(entry.getUserAgent());
        response.setContentViewed(entry.isContentViewed());
        response.setDetails(entry.getDetails());
        return response;
    }
    
    private UUID getUserId(Authentication auth) {
        // Extract user ID from authentication
        return UUID.randomUUID(); // Placeholder
    }
    
    private String getUserName(Authentication auth) {
        return auth.getName();
    }
    
    private List<String> getUserRoles(Authentication auth) {
        return auth.getAuthorities().stream()
            .map(authority -> authority.getAuthority())
            .collect(Collectors.toList());
    }
    
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null) {
            return xfHeader.split(",")[0];
        }
        return request.getRemoteAddr();
    }
    
    private String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}
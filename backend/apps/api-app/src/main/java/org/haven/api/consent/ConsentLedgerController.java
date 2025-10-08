package org.haven.api.consent;

import org.haven.clientprofile.application.dto.*;
import org.haven.clientprofile.application.services.ConsentLedgerQueryService;
import org.haven.clientprofile.domain.consent.ConsentStatus;
import org.haven.clientprofile.domain.consent.ConsentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for consent ledger queries and exports
 * Provides read-only access to consent records with comprehensive filtering and search
 */
@RestController
@RequestMapping("/api/consent-ledger")
@PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
public class ConsentLedgerController {
    
    private final ConsentLedgerQueryService queryService;
    
    public ConsentLedgerController(ConsentLedgerQueryService queryService) {
        this.queryService = queryService;
    }
    
    /**
     * Search consent ledger with comprehensive filters and pagination
     */
    @GetMapping("/search")
    public ResponseEntity<Page<ConsentLedgerResponse>> searchConsents(
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) ConsentType consentType,
            @RequestParam(required = false) ConsentStatus status,
            @RequestParam(required = false) String recipientOrganization,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX") Instant grantedAfter,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX") Instant grantedBefore,
            @RequestParam(defaultValue = "false") boolean includeVAWAProtected,
            Pageable pageable) {
        
        Page<ConsentLedgerResponse> results = queryService.searchConsents(
            clientId, consentType, status, recipientOrganization, 
            grantedAfter, grantedBefore, includeVAWAProtected, pageable
        );
        
        return ResponseEntity.ok(results);
    }
    
    /**
     * Get consent ledger for a specific client
     */
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<ConsentLedgerResponse>> getClientConsents(
            @PathVariable UUID clientId,
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        
        List<ConsentLedgerResponse> consents = activeOnly 
            ? queryService.getActiveConsentsForClient(clientId)
            : queryService.getAllConsentsForClient(clientId);
            
        return ResponseEntity.ok(consents);
    }
    
    /**
     * Get consents expiring soon (requiring review)
     */
    @GetMapping("/expiring-soon")
    public ResponseEntity<List<ConsentLedgerResponse>> getConsentsExpiringSoon(
            @RequestParam(defaultValue = "30") int daysAhead) {
        
        List<ConsentLedgerResponse> consents = queryService.getConsentsExpiringSoon(daysAhead);
        return ResponseEntity.ok(consents);
    }
    
    /**
     * Get expired consents
     */
    @GetMapping("/expired")
    public ResponseEntity<List<ConsentLedgerResponse>> getExpiredConsents() {
        List<ConsentLedgerResponse> consents = queryService.getExpiredConsents();
        return ResponseEntity.ok(consents);
    }
    
    /**
     * Get consents by recipient organization
     */
    @GetMapping("/recipient/{recipientOrganization}")
    public ResponseEntity<List<ConsentLedgerResponse>> getConsentsByRecipient(
            @PathVariable String recipientOrganization) {
        
        List<ConsentLedgerResponse> consents = queryService.getConsentsByRecipient(recipientOrganization);
        return ResponseEntity.ok(consents);
    }
    
    /**
     * Get VAWA protected consents (restricted access)
     */
    @GetMapping("/vawa-protected")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<List<ConsentLedgerResponse>> getVAWAProtectedConsents() {
        List<ConsentLedgerResponse> consents = queryService.getVAWAProtectedConsents();
        return ResponseEntity.ok(consents);
    }
    
    /**
     * Get audit trail for a specific consent
     */
    @GetMapping("/{consentId}/audit-trail")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<List<ConsentAuditResponse>> getConsentAuditTrail(@PathVariable UUID consentId) {
        List<ConsentAuditResponse> auditTrail = queryService.getConsentAuditTrail(consentId);
        return ResponseEntity.ok(auditTrail);
    }
    
    /**
     * Export consent ledger data (CSV format)
     */
    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<byte[]> exportConsentLedger(
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) ConsentType consentType,
            @RequestParam(required = false) ConsentStatus status,
            @RequestParam(required = false) String recipientOrganization,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX") Instant grantedAfter,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX") Instant grantedBefore,
            @RequestParam(defaultValue = "false") boolean includeVAWAProtected) {
        
        byte[] csvData = queryService.exportConsentLedger(
            clientId, consentType, status, recipientOrganization, 
            grantedAfter, grantedBefore, includeVAWAProtected
        );
        
        return ResponseEntity.ok()
            .header("Content-Type", "text/csv")
            .header("Content-Disposition", "attachment; filename=consent-ledger-export.csv")
            .body(csvData);
    }
    
    /**
     * Get consent statistics for dashboard
     */
    @GetMapping("/statistics")
    public ResponseEntity<ConsentStatisticsResponse> getConsentStatistics() {
        ConsentStatisticsResponse stats = queryService.getConsentStatistics();
        return ResponseEntity.ok(stats);
    }
}